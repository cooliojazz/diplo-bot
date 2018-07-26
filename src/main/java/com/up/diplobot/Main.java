package com.up.diplobot;

import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javafx.util.Pair;
import javax.imageio.ImageIO;
import javax.security.auth.login.LoginException;
import net.dv8tion.jda.core.AccountType;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.JDABuilder;
import net.dv8tion.jda.core.MessageBuilder;
import net.dv8tion.jda.core.entities.ChannelType;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.dv8tion.jda.core.exceptions.RateLimitedException;
import net.dv8tion.jda.core.hooks.ListenerAdapter;

/**
 *
 * @author Ricky
 */
public class Main extends ListenerAdapter {
    
    /*
    __Formal Guide to Diplo-Bot__
    **1.** Send it a request to play, something along the lines of *I would like to play the game.*, with politeness to suit.
    **2.** Wait for a turn notification
    **3.** Send it all orders for that turn in standard diplomacy format (eg. *A MUN-BER*), and it will inform you of any mistakes you may have made and ask you to correct them (eg. trying to order a unit in a territory you don't own)
    **4.** Check the #game-map and #game-orders channels to see all the submitted orders, along with the state of the board after they were executed.
    **5.** Some units may have been displaced, and if so, need to retreat. Diplo-Bot will inform you of any such units, and you may then use a standard move order to specify where it should retreat to. Any units left without valid orders in this phase will subsequently be disbanded.
    **6.** If this is a spring turn, your supply will be updated, and so you may have units to recruit or units to disband. Diplo-Bot will inform you of which it is, and you specify the units for either the same way, with a simple order that is just what and where (eg. *A KIE*)
    **7.** Repeat ad naseum.
    */
    
    
    
    
    
    /**
     * Game order:
    =* 0. Have everyone register as a country 
    =* 1. Send turn notification (You have until XX to send your orders)
    =* 2. Gather orders
    =* 3. Evaluate orders
    =* 4. Resolve conflicts/displacements
    =* 5. Display status
    ~* 5. Repeat for Fall
    ~* 6. Do end of year supply checks
    ~* 7. Repeat For Spring
     * 
     */

//    public static final long TURN_INTERVAL = 1000 * 60 * 60 * 24 * 3;
    public static final long TURN_INTERVAL = 1000 * 60;
    public static final long RETREATS_INTERVAL = 1000 * 30;
    public static final long SUPPLY_INTERVAL = 1000 * 30;
    public static Date nextt = new Date();
    public Game game = new Game();
//    public static GraphBuilder gb;
    public static Main instance;
    public static JDA jda;
    
    public static TextChannel mapchan;
    public static TextChannel orderschan;
    public static ScheduledExecutorService exec = Executors.newScheduledThreadPool(1);
    public TurnPhase phase = TurnPhase.JOINING;
    public HashMap<Territory, Unit> displaced = new HashMap<>();
    
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws LoginException, RateLimitedException, InterruptedException {
        instance = new Main();
//        gb = new GraphBuilder(instance.game);
        jda = new JDABuilder(AccountType.BOT).setToken("MzU1MDk4NTIzMDg1MTc2ODMy.DJIxnw.amADv3v7Zeh7wCfnatUXGSs1VuU").buildBlocking();
        jda.addEventListener(instance);
        mapchan = Main.jda.getTextChannels().stream().filter(c -> c.getName().equals("game-map")).findAny().get();
        orderschan = Main.jda.getTextChannels().stream().filter(c -> c.getName().equals("game-orders")).findAny().get();
    }
    
    public void startGame() {
        phase = TurnPhase.ORDERS;
        game.startGame();
        sendCurrentBoard();
        sendTurnNotices(nextt);
        exec.schedule(this::processOrders, TURN_INTERVAL, TimeUnit.MILLISECONDS);
    }
    
    public void processOrders() {
        try {
            //Pre-grab order info
            HashMap<Order, String> orderis = new HashMap<>();
            for (Player p : game.getPlayers()) {
                for (Order o : p.getOrders()) {
                    orderis.put(o, "**" + p.getUser().getName() + "** ordered that the " + o);
                }
            }

            //Proccess orders
            //Group orders
            HashMap<Order.OrderType, ArrayList<Order>> gorders = new HashMap<>();
            for (Order.OrderType o : Order.OrderType.values()) {
                gorders.put(o, new ArrayList<>());
            }
            for (Player p : game.getPlayers()) {
                for (Order o : p.getOrders()) {
                    gorders.get(o.getType()).add(o);
                }
            }
            //Add any units without orders
            for (Territory t : game.territories) {
                if (t.getOccupant() != null) {
                    if (!gorders.entrySet().stream().map(e -> e.getValue()).anyMatch(e -> e.stream().anyMatch(o -> o.main.equals(t)))) {
                        gorders.get(Order.OrderType.HOLD).add(new Order(Order.OrderType.HOLD, t.getOwner(), t));
                    }
                }
            }
            //Calculate influence
            game.resetInfluences();
            for (Order o : gorders.get(Order.OrderType.HOLD)) {
                HashMap<TerritoryDescriptor, Integer> si = game.getInfluencesOn(o.main);
                si.put(o.main.getInfo(), 1);
            }
            for (Order o : gorders.get(Order.OrderType.MOVE)) {
                int inf = 1;
                if (!game.graph.areConnected(o.main.getInfo(), o.dest.getInfo())) {
                    if (!game.graph.areConnectedViaConoys(o.main.getInfo(), o.dest.getInfo(), gorders.get(Order.OrderType.CONVOY))) {
                        inf = 0;
                    }
                }
                HashMap<TerritoryDescriptor, Integer> si = game.getInfluencesOn(o.dest);
                if (!si.containsKey(o.main.getInfo())) {
                    si.put(o.main.getInfo(), inf);
                } else {
                    si.put(o.main.getInfo(), si.get(o.main.getInfo()) + inf);
                }
            }
            for (Order o : gorders.get(Order.OrderType.SUPPORT)) {
                Territory dest;
                if (o.dest != null) {
                    dest = o.dest;
                } else {
                    dest = o.smain;
                }
                HashMap<TerritoryDescriptor, Integer> si = game.getInfluencesOn(dest);
                if (!si.containsKey(o.smain.getInfo())) {
                    si.put(o.smain.getInfo(), 1);
                } else {
                    si.put(o.smain.getInfo(), si.get(o.smain.getInfo()) + 1);
                }
            }
            //Get maxes
            HashMap<TerritoryDescriptor, Pair<TerritoryDescriptor, Integer>> maxis = game.calculateMaxInfluences();

            HashMap<Unit, Territory> olddisplaced = new HashMap<>();
            displaced = new HashMap<>();
            //Do moves
            for (Order o : gorders.get(Order.OrderType.MOVE)) {
                if (maxis.get(o.dest.getInfo()).getKey() == o.main.getInfo()) {
                    //Do move! Displacements might have to be dealt with when getting unit to move
                    olddisplaced.put(o.main.getOccupant(), o.main);
                    if (o.dest.getOccupant() != null) displaced.put(o.dest, o.dest.getOccupant());
                    if (displaced.containsKey(o.main)) {
                        o.dest.setOccupant(displaced.get(o.main));
                        displaced.remove(o.main);
                    } else {
                        o.dest.setOccupant(o.main.getOccupant());
                        o.main.setOccupant(null);
                    }
                } else {
                    //Set failed for this and supporting orders
                    o.setFailed(true);
                    for (Order so : gorders.get(Order.OrderType.SUPPORT)) {
                        if (so.smain == o.main && so.dest == o.dest) {
                            so.setFailed(true);
                        }
                    }
                    //Set this unit to re-influence it's territory
                    Pair<TerritoryDescriptor, Integer> mt = maxis.get(o.main.getInfo());
                    if (mt != null && mt.getValue() == 1) {
                        maxis.put(o.main.getInfo(), new Pair<>(null, 1));
                        //Deal with this unit having been displaced and work backwards if necessary
                        Unit du = displaced.get(o.main);
                        if (du != null) {
                            displaced.remove(o.main);
                            olddisplaced.put(du, o.main);
                            while (du != null) {
                                Territory t = olddisplaced.get(du);
                                Unit tu = t.getOccupant();
                                t.setOccupant(du);
                                du = tu;
                            }
                        }
                    }
                }
            }

            //Display orders
            ArrayList<String> ordersm = new ArrayList<>();
            ordersm.add("\n╓┘\n╠═══════════════════════════════════════\n║\n" + "║ Orders for the " + game.getSeason() + " of " + game.getYear() + " are as follows:\n║\n");
            for (Map.Entry<Order, String> os : orderis.entrySet()) {
                if (ordersm.get(ordersm.size() - 1).length() > 1500) ordersm.add("");
                ordersm.set(ordersm.size() - 1, ordersm.get(ordersm.size() - 1) + "║ **•** " + (os.getKey().isFailed() ? "~~" : "") + os.getValue() + (os.getKey().isFailed() ? "~~" : "")  + "\n");
            }
            ordersm.set(ordersm.size() - 1, ordersm.get(ordersm.size() - 1) + "║\n╠═══════════════════════════════════════\n╙┐\n");
            for (String msg : ordersm) sendChannelMessage(orderschan, msg);
            //Display last turn
            sendCurrentBoard();

            //Send displacement notices
            sendRetreatNotices(displaced, nextt);
            
            phase = TurnPhase.RETREATS;
            exec.schedule(this::processDisplacements, RETREATS_INTERVAL, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    public void processDisplacements() {
        //Handle displacements
        ArrayList<Order> orders = new ArrayList<>();
        for (Player p : game.getPlayers()) {
            for (Order o : p.getOrders()) {
                orders.add(o);
            }
        }
        //Calculate influence
        for (Order o : orders) {
            HashMap<TerritoryDescriptor, Integer> si = game.getInfluencesOn(o.dest);
            if (!si.containsKey(o.main.getInfo())) {
                si.put(o.main.getInfo(), 1);
            } else {
                si.put(o.main.getInfo(), si.get(o.main.getInfo()) + 1);
            }
        }
        
        HashMap<TerritoryDescriptor, Pair<TerritoryDescriptor, Integer>> maxis = game.calculateMaxInfluences();

        //Do moves
        for (Order o : orders) {
            if (maxis.get(o.dest.getInfo()).getKey() == o.main.getInfo()) {
                //Do move!
                if (displaced.containsKey(o.main)) {
                    o.dest.setOccupant(displaced.get(o.main));
                    displaced.remove(o.main);
                }
            } else {
                //Set failed for this
                o.setFailed(true);
            }
        }
        
        sendCurrentBoard();

        //If fall, update land ownership, check supply, disband or grant
        if (game.getSeason() == Game.Season.FALL) {
            for (Territory t : game.territories) {
                if (t.getOccupant() != null) t.setOwner(t.getOccupant().getCountry());
            }
            //Check supply
            for (Player p : game.getPlayers()) {
                Country c = p.getCountry();
                ////
                int asupply = (int)(long)game.territories.stream().filter(t -> t.getInfo().isSupplyCenter() && c.equals(t.getOwner())).collect(Collectors.counting());
                int usupply = (int)(long)game.territories.stream().filter(t -> t.getOccupant() != null && c.equals(t.getOwner())).collect(Collectors.counting());
                if (usupply < asupply) {
                    sendUserMessage(p.getUser(), "Good news, after evaluating your various supply stations, I have determined that you can now sustain " + (asupply - usupply) + " extra troops!");
                }
                if (asupply < usupply) {
                    sendUserMessage(p.getUser(), "I do apologize, but after evaluating your various supply stations, I have determined that you can no longer sustain " + (usupply - asupply) + " of your troops!");
                }
                p.supplyoff = asupply - usupply;
            }
            //Send unit update requests
            phase = TurnPhase.SUPPLY;
            exec.schedule(this::processSupply, SUPPLY_INTERVAL, TimeUnit.MILLISECONDS);
        } else {
            nextTurn();
        }
    }
    
    public void processSupply() {
        for (Player p : game.getPlayers()) {
            for (Order o : p.getOrders()) {
                if (p.supplyoff < 0) {
                    o.main.setOccupant(null);
                } else {
                    o.main.setOccupant(new Unit(p.getCountry(), o.ut));
                }
            }
            if (p.supplyoff < 0) {
                //Forced random disband
                sendUserMessage(p.getUser(), "I do apologize, but since you did not specify which troop to disband, a random one was chosen for you!");
                Territory ter = game.territories.stream().filter(t -> t.getOwner() == p.getCountry()).findAny().orElse(null);
                if (ter != null) ter.setOccupant(null);
            }
        }
        
        nextTurn();
    }
    
    public void nextTurn() {
        game.saveGame();
        //Next turn
        for (Player p : game.getPlayers()) {
            p.clearOrders();
        }
        game.nextTurn();
        nextt = new Date(nextt.getTime() + TURN_INTERVAL);
        sendTurnNotices(nextt);
        
        phase = TurnPhase.ORDERS;
        exec.schedule(this::processOrders, TURN_INTERVAL, TimeUnit.MILLISECONDS);
    }
    
    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if (event.isFromType(ChannelType.PRIVATE) && !event.getAuthor().equals(jda.getSelfUser())) {
            Player ply = getPlayerFromUser(event.getAuthor());
            switch (phase) {
                case JOINING: {
                    if (ply == null) {
                        Pattern joinp = Pattern.compile("[Ii] would (?:very )?(?:much )?(?:like to (?:join|play)|appreciate (?:join|play)ing) (?:the game |[Dd]iplomacy )?as (.+)\\.");
//                        Pattern joinp = Pattern.compile("[Ii] would (?:very )?(?:much )?(?:like to (?:join|play)|appreciate (?:join|play)ing) (?:the game|[Dd]iplomacy)?\\.");
                        Matcher joinm = joinp.matcher(event.getMessage().getContent());
                        if (joinm.find()) {
                            Country c = Country.valueOf(joinm.group(1).toUpperCase());
//                            List<Country> freec = Arrays.asList(Country.values()).stream().filter(c -> !game.getPlayers().stream().anyMatch(p -> p.getCountry() == c)).collect(Collectors.toList());
//                            Country c = freec.get((int)(Math.random() * freec.size()));
                            if (game.getPlayers().stream().anyMatch(p -> p.getCountry() == c)) {
                                sendUserMessage(event.getAuthor(), "I do apologize, but the esteemed " + game.getPlayers().stream().filter(p -> p.getCountry() == c).findFirst().get().getUser().getName() + " has already stepped up to play as " + c + ". Do please choose someone else and try again though!");
                                return;
                            }
                            Player p = new Player(c, event.getAuthor());
                            game.addPlayer(p);
                            sendUserMessage(event.getAuthor(), "Then the wonderful country of " + c + " you shall be!");
                            sendChannelMessage(orderschan, "Our good friend " + p.getUser().getName() + " has decided to play the role of " + c + " in this gathering.");
//                            if (game.getPlayers().size() == Country.values().length - 6) startGame();
                            return;
                        }
                    }
                    if (event.getMessage().getContent().equals("Please start the game, gamemaster Luadon.")) {
                        startGame();
                        return;
                    }
                    break;
                }
                case ORDERS: {
                    Order o = new Order(game, event.getMessage().getContent(), ply);
                    o.setSender(ply.getCountry());
                    if (o.isValid()) {
                        ply.addOrder(o);
                    } else {
                        sendUserMessage(event.getAuthor(), o.vmsg);
                    }
                    return;
                }
                case RETREATS: {
                    Order o = new Order(game, event.getMessage().getContent(), ply);
                    o.setSender(ply.getCountry());
                    if (o.getType() != null && o.getType() == Order.OrderType.MOVE) {
                        if (o.isValid()) {
                            ply.addOrder(o);
                        } else {
                            sendUserMessage(event.getAuthor(), o.vmsg);
                        }
                    } else {
                        sendUserMessage(event.getAuthor(), "I do apologize, but only move orders are available to describe where to retreat your units to!");
                    }
                    return;
                }
                case SUPPLY: {
                    Order o = new Order(game, event.getMessage().getContent(), ply);
                    o.setSender(ply.getCountry());
                    if (o.getType() != null && o.getType() == Order.OrderType.SELECT) {
                        if (o.isValid()) {
                            ply.addOrder(o);
                        } else {
                            sendUserMessage(event.getAuthor(), o.vmsg);
                        }
                    } else {
                        sendUserMessage(event.getAuthor(), "I do apologize, but only unit specifying orders are available to describe units to recruit or disband!");
                    }
                    return;
                }
            }
            sendUserMessage(event.getAuthor(), "I do apologize, but I just cannot understand what it is that you are requesting of me.");
        }
    }
    
    public Player getPlayerFromUser(User u) {
        return game.getPlayers().stream().filter(p -> p.getUserId() == u.getIdLong()).findFirst().orElse(null);
    }
    
    public void sendCurrentBoard() {
        try {
            File board = new File("currentboard.png");
            BufferedImage cbi = new BufferedImage(game.map.getWidth(), game.map.getHeight(), BufferedImage.TYPE_INT_ARGB);
            game.drawGame(new Rectangle(0, 0, cbi.getWidth(), cbi.getHeight()), cbi.createGraphics());
            ImageIO.write(cbi, "png", board);
            mapchan.sendFile(board, new MessageBuilder().append("The current state of affairs in " + game.getSeason() + " of " + game.getYear() + "...").build()).queue();
        } catch (IOException ex) {
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    public void sendUserMessage(User u, String m) {
        u.openPrivateChannel().complete().sendMessage(m).queue();
    }
    
    public void sendChannelMessage(TextChannel c, String m) {
        c.sendMessage(m).queue();
    }
    
    public void sendRetreatNotices(HashMap<Territory, Unit> displaced, Date d) {
        for (Player p : game.getPlayers()) {
            int total = 0;
            for (Map.Entry<Territory, Unit> dis : displaced.entrySet()) {
                if (p.getCountry() == dis.getValue().getCountry()) {
                    sendUserMessage(p.getUser(), "The " + dis.getValue().getType() + " in " + dis.getKey().getInfo().getName() + " has been forced to retreat!");
                    total++;
                }
            }
            if (total > 0) {
                sendUserMessage(p.getUser(), "You have until " + DateFormat.getInstance().format(d) + " to send me your " + total + " retreat order(s), or else your units witout valid orders will be forced to disband from your millitary!");
            } else {
                sendUserMessage(p.getUser(), "None of your units were forced back from their current positions this season! Good on you!");
            }
        }
    }
    
    public void sendTurnNotices(Date d) {
        for (Player p : game.getPlayers()) {
            sendUserMessage(p.getUser(), "You have until " + DateFormat.getInstance().format(d) + " to send me all your orders for the " + game.getSeason() + " of " + game.getYear() + ".");
        }
    }
    
    enum TurnPhase {
        JOINING, ORDERS, RETREATS, SUPPLY
    }
}
