package com.up.diplobot;

import java.awt.Canvas;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
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
    
    public String getSeason() {
        return turn % 2 == 0 ? "Spring" : "Fall";
    }
    
    public Territory getTerritoryFromCode(String code) {
        for (Territory t : territories) {
            if (t.getInfo().getAbbreviation() != null && t.getInfo().getAbbreviation().equals(code)) {
                return t;
            }
        }
        return null;
    }
    
    public void startGame() {
        loadMap();
        for (TerritoryDescriptor td : territoryinfo) {
            territories.add(new Territory(td));
        }
    }
    
    public void drawGame(Canvas c, Graphics g) {
        g.drawImage(map, 0, 0, c.getWidth(), c.getHeight(), null);
        for (Territory t : territories) {
            if (t.getOccupant() != null) {
                g.drawImage(t.getOwner().getCountryUnitImage(t.getOccupant().getType()), (int)(t.getInfo().getPosition().getX() * c.getWidth()) - 15, (int)(t.getInfo().getPosition().getY() * c.getHeight()) - 15, 30, 30, null);
            } else if (t.getOwner() != null) {
                g.setColor(t.getOwner().c);
                g.fillOval((int)(t.getInfo().getPosition().getX() * c.getWidth()) - 3, (int)(t.getInfo().getPosition().getY() * c.getHeight()) - 3, 7, 7);
            }
        }
    }
    
    public void drawEditor(Canvas c, Graphics g) {
        g.drawImage(map, 0, 0, c.getWidth(), c.getHeight(), null);
        for (AdjacencyGraph.GraphPair<TerritoryDescriptor, TerritoryDescriptor> con : graph.getConnections()) {
            g.setColor(Color.DARK_GRAY);
            g.drawLine((int)(con.t.getPosition().getX() * c.getWidth()), (int)(con.t.getPosition().getY() * c.getHeight()), (int)(con.s.getPosition().getX() * c.getWidth()), (int)(con.s.getPosition().getY() * c.getHeight()));
        }
        for (TerritoryDescriptor t : territoryinfo) {
            g.setColor(t.getType().c);
            if (t.isSupplyCenter()) {
                g.fillRect((int)(t.getPosition().getX() * c.getWidth()) - 4, (int)(t.getPosition().getY() * c.getHeight()) - 4, 10, 10);
            } else {
                g.fillOval((int)(t.getPosition().getX() * c.getWidth()) - 4, (int)(t.getPosition().getY() * c.getHeight()) - 4, 9, 9);
            }
            g.setColor(getBGTextColor(t.getType().c));
            g.drawString(t.toString(), (int)(t.getPosition().getX() * c.getWidth()) - g.getFontMetrics().stringWidth(t.toString()) / 2 + 1, (int)(t.getPosition().getY() * c.getHeight()) + 16);
            g.drawString(t.toString(), (int)(t.getPosition().getX() * c.getWidth()) - g.getFontMetrics().stringWidth(t.toString()) / 2, (int)(t.getPosition().getY() * c.getHeight()) + 16);
            g.setColor(t.getType().c);
            g.drawString(t.toString(), (int)(t.getPosition().getX() * c.getWidth()) - g.getFontMetrics().stringWidth(t.toString()) / 2, (int)(t.getPosition().getY() * c.getHeight()) + 15);
            if (t.getStartingCountry() != null) {
                g.setColor(t.getStartingCountry().c);
                g.fillOval((int)(t.getPosition().getX() * c.getWidth()) - 3, (int)(t.getPosition().getY() * c.getHeight()) - 3, 7, 7);
            }
        }
        if (GraphBuilder.selected != null) {
            g.setColor(Color.WHITE);
            g.fillOval((int)(GraphBuilder.selected.getPosition().getX() * c.getWidth()) - 4, (int)(GraphBuilder.selected.getPosition().getY() * c.getHeight()) - 4, 9, 9);
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
    
    public static Color getBGTextColor(Color c) {
        float[] hsb = Color.RGBtoHSB(c.getRed(), c.getGreen(), c.getBlue(), null);
        hsb[0] = hsb[0] + 0.5f;
        hsb[1] = hsb[1] - 0.4f;
        hsb[2] = hsb[2] - 0.15f;
        return Color.getHSBColor(hsb[0], hsb[1], hsb[2]);
    }
}
