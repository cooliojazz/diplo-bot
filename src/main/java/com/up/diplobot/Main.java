package com.up.diplobot;

import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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
    
    /**
     * Game order:
    ~* 0. Have everyone register as a country 
    =* 1. Send turn notification (You have until XX to send your orders)
    ~* 2. Gather orders
    ~* 3. Evaluate orders
     * 4. Resolve conflicts/displacements
    ~* 5. Display status
     * 5. Repeat for Fall
     * 6. Do end of year supply checks
    ~* 7. Repeat For Spring
     * 
     */

//    public static final long TURN_INTERVAL = 60 * 60 * 24 * 3;
    public static final long TURN_INTERVAL = 1000 * 60;
    public static Date nextt = new Date();
    public Game game = new Game();
//    public static GraphBuilder gb;
    public static Main instance;
    public static JDA jda;
    
    public static TextChannel mapchan;
    public static TextChannel orderschan;
    public static ScheduledExecutorService exec = Executors.newScheduledThreadPool(1);
    
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
        game.startGame();
        sendCurrentBoard();
        instance.sendTurnNotices(nextt);
        exec.schedule(() -> {
            instance.processOrders();
        }, TURN_INTERVAL, TimeUnit.MILLISECONDS);
    }
    
    public void processOrders() {
        //Pre-grab order info
        HashMap<Order, String> orderis = new HashMap<>();
        for (Player p : game.getPlayers()) {
            for (Order o : p.getOrders()) {
                orderis.put(o, p.getUser().getName() + " ordered that the " + o);
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
        //Calculate influence
        HashMap<TerritoryDescriptor, HashMap<TerritoryDescriptor, Integer>> influence = new HashMap<>();
        for (Order o : gorders.get(Order.OrderType.HOLD)) {
            if (!influence.containsKey(o.main.getInfo())) {
                influence.put(o.main.getInfo(), new HashMap<>());
            }
            HashMap<TerritoryDescriptor, Integer> si = influence.get(o.main.getInfo());
//            if (!si.containsKey(o.main.getInfo())) {
            si.put(o.main.getInfo(), 1);
//            } else {
//                si.put(o.main.getInfo(), si.get(o.main.getInfo()) + 1);
//            }
        }
        for (Order o : gorders.get(Order.OrderType.MOVE)) {
            if (!influence.containsKey(o.dest.getInfo())) {
                influence.put(o.dest.getInfo(), new HashMap<>());
            }
            HashMap<TerritoryDescriptor, Integer> si = influence.get(o.dest.getInfo());
            if (!si.containsKey(o.main.getInfo())) {
                si.put(o.main.getInfo(), 1);
            } else {
                si.put(o.main.getInfo(), si.get(o.main.getInfo()) + 1);
            }
        }
        for (Order o : gorders.get(Order.OrderType.SUPPORT)) {
            if (!influence.containsKey(o.dest.getInfo())) {
                influence.put(o.dest.getInfo(), new HashMap<>());
            }
            HashMap<TerritoryDescriptor, Integer> si = influence.get(o.dest.getInfo());
            if (!si.containsKey(o.smain.getInfo())) {
                si.put(o.smain.getInfo(), 1);
            } else {
                si.put(o.smain.getInfo(), si.get(o.smain.getInfo()) + 1);
            }
        }
        /*
        
        Figure out convoying!!
        -Moves that aren't adjacent need to check that there are valid convoy orders to carry it the entire way
        
        
        */
        //Get maxes
        HashMap<TerritoryDescriptor, TerritoryDescriptor> maxis = new HashMap<>();
        for (Map.Entry<TerritoryDescriptor, HashMap<TerritoryDescriptor, Integer>> tis : influence.entrySet()) {
            TerritoryDescriptor maxtd = null;
            int maxi = 0;
            for (Map.Entry<TerritoryDescriptor, Integer> si : tis.getValue().entrySet()) {
                if (si.getValue() == maxi) {
                    maxtd = null;
                }
                if (si.getValue() > maxi) {
                    maxtd = si.getKey();
                    maxi = si.getValue();
                }
            }
            if (maxtd != null) maxis.put(tis.getKey(), maxtd);
        }
        
        HashMap<TerritoryDescriptor, Unit> displaced = new HashMap<>();
        
        //Do moves
        for (Order o : gorders.get(Order.OrderType.MOVE)) {
            if (maxis.get(o.dest.getInfo()) == o.main.getInfo()) {
                //Do move! Displacements might have to be dealt with when getting unit to move
                if (o.dest.getOccupant() != null) displaced.put(o.dest.getInfo(), o.dest.getOccupant());
                if (displaced.containsKey(o.main.getInfo())) {
                    o.dest.setOccupant(displaced.get(o.main.getInfo()));
                    displaced.remove(o.main.getInfo());
                } else {
                    o.dest.setOccupant(o.main.getOccupant());
                    o.main.setOccupant(null);
                }
                if (o.dest.getInfo().getType() != TerritoryDescriptor.TerritoryType.WATER) 
                    o.dest.setOwner(o.getSender());
            } else {
                //Set failed for this and supporting orders
                o.setFailed(true);
                for (Order so : gorders.get(Order.OrderType.SUPPORT)) {
                    if (so.smain == o.main && so.dest == o.dest) {
                        so.setFailed(true);
                    }
                }
                //Deal with this unit having been displaced and work backwards if necessary
            }
        }
        
        //Display orders
        ArrayList<String> ordersm = new ArrayList<String>();
        ordersm.add("===========================================================\n" + "Orders for the " + game.getSeason() + " of " + game.getYear() + " are as follows:\n \n");
        for (Map.Entry<Order, String> os : orderis.entrySet()) {
            if (ordersm.get(ordersm.size() - 1).length() > 1000) ordersm.add("");
            ordersm.set(ordersm.size() - 1, ordersm.get(ordersm.size() - 1) + "(" + (os.getKey().isFailed() ? "*" : "-") + ") " + os.getValue() + "\n");
        }
        for (String msg : ordersm) sendChannelMessage(orderschan, msg);
        //Display last turn
        sendCurrentBoard();
        
        //Handle displacements
        
        
        //Next turn
        for (Player p : game.getPlayers()) {
            p.clearOrders();
        }
        game.nextTurn();
        nextt = new Date(nextt.getTime() + TURN_INTERVAL);
        instance.sendTurnNotices(nextt);
        exec.schedule(() -> {
            instance.processOrders();
        }, TURN_INTERVAL, TimeUnit.MILLISECONDS);
    }
    
    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if (event.isFromType(ChannelType.PRIVATE) && !event.getAuthor().equals(jda.getSelfUser())) {
            Player ply = getPlayerFromUser(event.getAuthor());
            if (ply == null) {
                Pattern joinp = Pattern.compile("I would (?:very )?(?:much )?(?:like to (?:join|play)|appreciate (?:join|play)ing) (?:the game|Diplomacy)? ?as (.+)\\.");
                Matcher joinm = joinp.matcher(event.getMessage().getContent());
                if (joinm.find()) {
                    Country c = Country.valueOf(joinm.group(1).toUpperCase());
                    if (game.getPlayers().stream().anyMatch(p -> p.getCountry() == c)) {
                        sendUserMessage(event.getAuthor(), "I do apologize, but the esteemed " + game.getPlayers().stream().filter(p -> p.getCountry() == c).findFirst().get().getUser().getName() + " has already stepped up to play as " + c + ". Do please choose someone else and try again though!");
                        return;
                    }
                    Player p = new Player(c, event.getAuthor());
                    game.addPlayer(p);
                    sendUserMessage(event.getAuthor(), "Then the wonderful country of " + c + " you shall be!");
                    sendChannelMessage(orderschan, "Our good friend " + p.getUser().getName() + " has decided to play the role of " + c + " in this gathering.");
                    startGame();
                    return;
                }
            } else {
                Order o = new Order(game, event.getMessage().getContent());
                o.setSender(ply.getCountry());
                if (o.isValid()) {
                    ply.addOrder(o);
                } else {
                    sendUserMessage(event.getAuthor(), o.vmsg);
                }
                return;
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
    
    public void sendTurnNotices(Date d) {
        for (Player p : game.getPlayers()) {
            sendUserMessage(p.getUser(), "You have until " + DateFormat.getInstance().format(d) + " to send me all your orders for the " + game.getSeason() + " of " + game.getYear() + ".");
        }
    }
}
