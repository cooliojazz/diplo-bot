package com.up.diplobot;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.util.Date;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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

//    public static final long TURN_INTERVAL = 60 * 60 * 24 * 3;
    public static final long TURN_INTERVAL = 1000 * 60 * 1;
    public static Date nextt = new Date();
    public Game game = new Game();
    public static GraphBuilder gb;
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
        gb = new GraphBuilder(instance.game);
        jda = new JDABuilder(AccountType.BOT).setToken("MzU1MDk4NTIzMDg1MTc2ODMy.DJIxnw.amADv3v7Zeh7wCfnatUXGSs1VuU").buildBlocking();
        jda.addEventListener(instance);
        mapchan = Main.jda.getTextChannels().stream().filter(c -> c.getName().equals("game-map")).findAny().get();
        orderschan = Main.jda.getTextChannels().stream().filter(c -> c.getName().equals("game-orders")).findAny().get();
    }
    
    public void startGame() {
        game.startGame();
        instance.sendTurnNotices(nextt);
        exec.schedule(() -> {
            instance.processOrders();
        }, TURN_INTERVAL, TimeUnit.MILLISECONDS);
    }
    
    public void processOrders() {
        //Do orders
        sendChannelMessage(orderschan, "\nOrders for the " + game.getSeason() + " of " + game.getYear() + " are as follows: ");
        for (Player p : game.getPlayers()) {
            for (Order o : p.getOrders()) {
                sendChannelMessage(orderschan, p.getUser().getName() + " ordered that the " + o);
            }
        }
        //Proccess orders
        
        
        gb.win.getCanvas().repaint();
        try {
            sendCurrentBoard();
        } catch (IOException ex) {
            Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
        }
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
                Pattern joinp = Pattern.compile("I would (?:very )?(?:much )?(?:like to join|appreciate joining) (?:the game|Diplomacy) as (.+)\\.");
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
    
    public void sendCurrentBoard() throws IOException {
        File board = new File("currentboard.png");
        ImageIO.write(gb.win.buffer, "png", board);
        mapchan.sendFile(board, new MessageBuilder().append("The current state of affairs...").build()).queue();
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
    
    /**
     * Game order:
    ~* 0. Have everyone register as a country 
    =* 1. Send turn notification (You have until XX to send your orders)
    ~* 2. Gather orders
     * 3. Evaluate orders
     * 4. Resolve conflicts/displacements
     * 5. Display status
     * 5. Repeat for Fall
     * 6. Do end of year supply checks
     * 7. Repeat For Spring
     * 
     */
}
