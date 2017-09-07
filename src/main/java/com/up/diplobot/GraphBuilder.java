package com.up.diplobot;

import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JOptionPane;

/**
 *
 * @author Ricky
 */
public class GraphBuilder {

    public DrawableWindow win;
    public static MODE m = MODE.ADDING;
    Game g;
    public static TerritoryDescriptor selected = null;

    public GraphBuilder(Game g) {
        this.g = g;
        this.win = new DrawableWindow(500, 500, g::drawGame);
        win.getCanvas().repaint();
        win.getCanvas().addMouseListener(new MouseAdapter() {
            
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getButton() != MouseEvent.BUTTON2) {
                    if (m == MODE.ADDING) {
                        if (e.getButton() == MouseEvent.BUTTON1) {
                            TerritoryDescriptor t = new TerritoryDescriptor(TerritoryDescriptor.TerritoryType.LAND, (double)e.getX() / win.getCanvas().getWidth(), (double)e.getY() / win.getCanvas().getHeight());
                            g.territoryinfo.add(t);
                            g.graph.addTerritory(t);
                        } else if (e.getButton() == MouseEvent.BUTTON3) {
                            TerritoryDescriptor t = findTerritory((double)e.getX() / win.getCanvas().getWidth(), (double)e.getY() / win.getCanvas().getHeight());
                            g.territoryinfo.remove(t);
                            g.graph.removeTerritory(t);
                        }
                    } else if (m == MODE.CONNECTING) {
                        if (e.getButton() == MouseEvent.BUTTON1) {
                            if (selected == null) {
                                selected = findTerritory((double)e.getX() / win.getCanvas().getWidth(), (double)e.getY() / win.getCanvas().getHeight());
                            } else {
                                TerritoryDescriptor t = findTerritory((double)e.getX() / win.getCanvas().getWidth(), (double)e.getY() / win.getCanvas().getHeight());
                                if (t != null) {
                                    g.graph.addConnection(selected, t);
                                    selected = null;
                                }
                            }
                        } else if (e.getButton() == MouseEvent.BUTTON3) {
                            if (selected == null) {
                                selected = findTerritory((double)e.getX() / win.getCanvas().getWidth(), (double)e.getY() / win.getCanvas().getHeight());
                            } else {
                                TerritoryDescriptor t = findTerritory((double)e.getX() / win.getCanvas().getWidth(), (double)e.getY() / win.getCanvas().getHeight());
                                if (t != null) {
                                    g.graph.removeConnection(selected, t);
                                    selected = null;
                                }
                            }
                        }
                    } else if (m == MODE.NAMING) {
                        selected = findTerritory((double)e.getX() / win.getCanvas().getWidth(), (double)e.getY() / win.getCanvas().getHeight());
                        if (selected != null) {
                            win.getCanvas().repaint();
                            String name = JOptionPane.showInputDialog(win.getCanvas(), "Territory Name", selected.getName());
                            String abv = JOptionPane.showInputDialog(win.getCanvas(), "Territory Abreviation", selected.getAbbreviation());
                            if (name != null && abv != null) {
                                selected.setName(name);
                                selected.setAbbreviation(abv);
                            }
                        }
                    } else if (m == MODE.TYPE) {
                        selected = findTerritory((double)e.getX() / win.getCanvas().getWidth(), (double)e.getY() / win.getCanvas().getHeight());
                        if (selected != null) {
                            win.getCanvas().repaint();
                            TerritoryDescriptor.TerritoryType type = (TerritoryDescriptor.TerritoryType)JOptionPane.showInputDialog(win.getCanvas(), "Territory Type", "", JOptionPane.QUESTION_MESSAGE, null, TerritoryDescriptor.TerritoryType.values(), TerritoryDescriptor.TerritoryType.LAND);
                            if (type != null) {
                                selected.setType(type);
                            }
                        }
                        selected = null;
                    } else if (m == MODE.COUNTRY) {
                        selected = findTerritory((double)e.getX() / win.getCanvas().getWidth(), (double)e.getY() / win.getCanvas().getHeight());
                        if (selected != null) {
                            win.getCanvas().repaint();
                            Country country = (Country)JOptionPane.showInputDialog(win.getCanvas(), "Territory Starting Country", "", JOptionPane.QUESTION_MESSAGE, null, Country.values(), Country.AUSTRIA);
                            if (country != null) {
                                selected.setStartingCountry(country);
                            }
                        }
                        selected = null;
                    } else if (m == MODE.SUPPLY) {
                        selected = findTerritory((double)e.getX() / win.getCanvas().getWidth(), (double)e.getY() / win.getCanvas().getHeight());
                        if (selected != null) {
                            selected.setSupplyCenter(!selected.isSupplyCenter());
                        }
                    } else if (m == MODE.UNITS) {
                        selected = findTerritory((double)e.getX() / win.getCanvas().getWidth(), (double)e.getY() / win.getCanvas().getHeight());
                        if (selected != null) {
                            win.getCanvas().repaint();
                            Unit.UnitType type = (Unit.UnitType)JOptionPane.showInputDialog(win.getCanvas(), "Territory Starting Unit", "", JOptionPane.QUESTION_MESSAGE, null, Unit.UnitType.values(), Unit.UnitType.ARMY);
                            if (type != null) {
                                selected.setStartType(type);
                            }
                        }
                    }
                } else {
                    selected = null;
                }
                win.getCanvas().repaint();
            }
            
        });
        win.getCanvas().addKeyListener(new KeyAdapter() {
            @Override
            public void keyTyped(KeyEvent e) {
                switch (e.getKeyChar()) {
                    case 'a': {
                        m = MODE.ADDING;
                        break;
                    }
                    case 'c': {
                        m = MODE.CONNECTING;
                        break;
                    }
                    case 'd': {
                        m = MODE.COUNTRY;
                        break;
                    }
                    case 'n': {
                        m = MODE.NAMING;
                        break;
                    }
                    case 'o': {
                        //open();
                        g.loadMap();
                        break;
                    }
                    case 'p': {
                        m = MODE.SUPPLY;
                        break;
                    }
                    case 'u': {
                        m = MODE.UNITS;
                        break;
                    }
                    case 's': {
                        //save();
                        File f = new File("map.jsd");
                        try (ObjectOutputStream os = new ObjectOutputStream(new FileOutputStream(f))) {
                            os.writeObject(g.territoryinfo);
                            os.writeObject(g.graph);
                        } catch (FileNotFoundException ex) {
                            Logger.getLogger(GraphBuilder.class.getName()).log(Level.SEVERE, null, ex);
                        } catch (IOException ex) {
                            Logger.getLogger(GraphBuilder.class.getName()).log(Level.SEVERE, null, ex);
                        }
                        
                        break;
                    }
                    case 't': {
                        m = MODE.TYPE;
                        break;
                    }
                    case 'q': {
                        try {
                            Main.instance.sendCurrentBoard();
                        } catch (IOException ex) {
                            Logger.getLogger(GraphBuilder.class.getName()).log(Level.SEVERE, null, ex);
                        }
                        break;
                    }
                }
                win.getCanvas().repaint();
            }
            
        });
        win.getCanvas().requestFocus();
    }
    
    public TerritoryDescriptor findTerritory(double x, double y) {
        for (TerritoryDescriptor t : g.territoryinfo) {
            if (t.getPosition().distance(x, y) < 0.01) {
                return t;
            }
        }
        return null;
    }
    
    public enum MODE {
        ADDING("Adding Territories"), CONNECTING("Connecting Territories"), NAMING("Naming Territories"), TYPE("Setting Territory Types"), COUNTRY("Setting Territory Starting Country"), SUPPLY("Setting Supply Territories"), UNITS("Setting Territory Units");
        
        String text;

        private MODE(String text) {
            this.text = text;
        }

        @Override
        public String toString() {
            return text;
        }
        
    }
}
