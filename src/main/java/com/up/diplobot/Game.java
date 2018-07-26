package com.up.diplobot;

import java.awt.Canvas;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.util.Pair;
import javax.imageio.ImageIO;

/**
 *
 * @author Ricky
 */
public class Game {
    BufferedImage map = null;
    ArrayList<TerritoryDescriptor> territoryinfo = new ArrayList<>();
    AdjacencyGraph graph = new AdjacencyGraph();
    
    private ArrayList<Player> players = new ArrayList<>();
    ArrayList<Territory> territories = new ArrayList<>();
    private int turn;
    private HashMap<TerritoryDescriptor, HashMap<TerritoryDescriptor, Integer>> influence = new HashMap<>();

    public Game() {
        try {
            map = ImageIO.read(getClass().getResourceAsStream("map.png"));
        } catch (IOException ex) {
            Logger.getLogger(GraphBuilder.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    public void addPlayer(Player p) {
        players.add(p);
    }
    
    public int getTurn() {
        return turn;
    }
    
    public void nextTurn() {
        turn++;
    }

    public ArrayList<Player> getPlayers() {
        return players;
    }
    
    public int getYear() {
        return turn / 2 + 1901;
    }
    
    public Season getSeason() {
        return turn % 2 == 0 ? Season.SPRING : Season.FALL;
    }
    
    public Territory getTerritoryFromCode(String code) {
        for (Territory t : territories) {
            if (t.getInfo().getAbbreviation() != null && t.getInfo().getAbbreviation().equals(code)) {
                return t;
            }
        }
        return null;
    }
    
    public HashMap<TerritoryDescriptor, Integer> getInfluencesOn(Territory t) {
        if (!influence.containsKey(t.getInfo())) {
            influence.put(t.getInfo(), new HashMap<>());
        }
        return influence.get(t.getInfo());
    }
    
    public void resetInfluences() {
        influence.clear();
        for (Territory t : territories) {
            influence.put(t.getInfo(), new HashMap<>());
        }
    }
    
    public HashMap<TerritoryDescriptor, Pair<TerritoryDescriptor, Integer>> calculateMaxInfluences() {
        HashMap<TerritoryDescriptor, Pair<TerritoryDescriptor, Integer>> maxis = new HashMap<>();
        for (Map.Entry<TerritoryDescriptor, HashMap<TerritoryDescriptor, Integer>> tis : influence.entrySet()) {
            maxis.put(tis.getKey(), calculateMaxInfluenceIn(tis.getValue()));
        }
        return maxis;
    }
    
    public Pair<TerritoryDescriptor, Integer> calculateMaxInfluenceIn(HashMap<TerritoryDescriptor, Integer> inf) {
        TerritoryDescriptor maxtd = null;
        int maxi = 0;
        for (Map.Entry<TerritoryDescriptor, Integer> si : inf.entrySet()) {
            if (si.getValue() == maxi) {
                maxtd = null;
            }
            if (si.getValue() > maxi) {
                maxtd = si.getKey();
                maxi = si.getValue();
            }
        }
        return new Pair<>(maxtd, maxi);
    }
    
    public void startGame() {
        loadMap();
        if (new File("game.jsd").exists()) {
            loadGame();
        } else {
            for (TerritoryDescriptor td : territoryinfo) {
                territories.add(new Territory(td));
            }
        }
    }
    
    public void drawGame(Rectangle r, Graphics g) {
        g.drawImage(map, 0, 0, (int)r.getWidth(), (int)r.getHeight(), null);
        for (Territory t : territories) {
            if (t.getOccupant() != null) {
                g.drawImage(t.getOccupant().getCountry().getCountryUnitImage(t.getOccupant().getType()), (int)(t.getInfo().getPosition().getX() * r.getWidth()) - 15, (int)(t.getInfo().getPosition().getY() * r.getHeight()) - 15, 30, 30, null);
            } else if (t.getOwner() != null) {
                g.setColor(t.getOwner().c);
                g.fillOval((int)(t.getInfo().getPosition().getX() * r.getWidth()) - 3, (int)(t.getInfo().getPosition().getY() * r.getHeight()) - 3, 7, 7);
            }
        }
    }
    
    public void drawEditor(Rectangle r, Graphics g) {
        g.drawImage(map, 0, 0, (int)r.getWidth(), (int)r.getHeight(), null);
        for (AdjacencyGraph.OrderlessPair<TerritoryDescriptor, TerritoryDescriptor> con : graph.getConnections()) {
            g.setColor(Color.DARK_GRAY);
            g.drawLine((int)(con.t.getPosition().getX() * r.getWidth()), (int)(con.t.getPosition().getY() * r.getHeight()), (int)(con.s.getPosition().getX() * r.getWidth()), (int)(con.s.getPosition().getY() * r.getHeight()));
        }
        for (TerritoryDescriptor t : territoryinfo) {
            g.setColor(t.getType().c);
            if (t.isSupplyCenter()) {
                g.fillRect((int)(t.getPosition().getX() * r.getWidth()) - 4, (int)(t.getPosition().getY() * r.getHeight()) - 4, 10, 10);
            } else {
                g.fillOval((int)(t.getPosition().getX() * r.getWidth()) - 4, (int)(t.getPosition().getY() * r.getHeight()) - 4, 9, 9);
            }
            g.setColor(getBGTextColor(t.getType().c));
            g.drawString(t.toString(), (int)(t.getPosition().getX() * r.getWidth()) - g.getFontMetrics().stringWidth(t.toString()) / 2 + 1, (int)(t.getPosition().getY() * r.getHeight()) + 16);
            g.drawString(t.toString(), (int)(t.getPosition().getX() * r.getWidth()) - g.getFontMetrics().stringWidth(t.toString()) / 2, (int)(t.getPosition().getY() * r.getHeight()) + 16);
            g.setColor(t.getType().c);
            g.drawString(t.toString(), (int)(t.getPosition().getX() * r.getWidth()) - g.getFontMetrics().stringWidth(t.toString()) / 2, (int)(t.getPosition().getY() * r.getHeight()) + 15);
            if (t.getStartingCountry() != null) {
                g.setColor(t.getStartingCountry().c);
                g.fillOval((int)(t.getPosition().getX() * r.getWidth()) - 3, (int)(t.getPosition().getY() * r.getHeight()) - 3, 7, 7);
            }
        }
        if (GraphBuilder.selected != null) {
            g.setColor(Color.WHITE);
            g.fillOval((int)(GraphBuilder.selected.getPosition().getX() * r.getWidth()) - 4, (int)(GraphBuilder.selected.getPosition().getY() * r.getHeight()) - 4, 9, 9);
        }
        g.setColor(Color.red);
        g.drawString(GraphBuilder.m.toString(), 10, 20);
    }
    
    public void loadMap() {
        File f = new File("map.jsd");
        try (ObjectInputStream os = new ObjectInputStream(new FileInputStream(f))) {
            territoryinfo = (ArrayList<TerritoryDescriptor>)os.readObject();
            graph = (AdjacencyGraph)os.readObject();
        } catch (FileNotFoundException ex) {
            Logger.getLogger(GraphBuilder.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(GraphBuilder.class.getName()).log(Level.SEVERE, null, ex);
        } catch (ClassNotFoundException ex) {
            Logger.getLogger(GraphBuilder.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    public void loadGame() {
        File f = new File("game.jsd");
        try (ObjectInputStream os = new ObjectInputStream(new FileInputStream(f))) {
            territories = (ArrayList<Territory>)os.readObject();
            players = (ArrayList<Player>)os.readObject();
            turn = os.readInt();
        } catch (FileNotFoundException ex) {
            Logger.getLogger(GraphBuilder.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(GraphBuilder.class.getName()).log(Level.SEVERE, null, ex);
        } catch (ClassNotFoundException ex) {
            Logger.getLogger(GraphBuilder.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    public void saveGame() {
        File f = new File("game.jsd");
        try (ObjectOutputStream os = new ObjectOutputStream(new FileOutputStream(f))) {
            os.writeObject(territories);
            os.writeObject(players);
            os.writeInt(turn);
        } catch (FileNotFoundException ex) {
            Logger.getLogger(GraphBuilder.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(GraphBuilder.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    public static Color getBGTextColor(Color c) {
        float[] hsb = Color.RGBtoHSB(c.getRed(), c.getGreen(), c.getBlue(), null);
        hsb[0] = hsb[0] + 0.5f;
        hsb[1] = hsb[1] - 0.4f;
        hsb[2] = hsb[2] - 0.15f;
        return Color.getHSBColor(hsb[0], hsb[1], hsb[2]);
    }
    
    public enum Season {
        FALL("Fall"), SPRING("Spring");
        String name;

        private Season(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
        
    }
}
