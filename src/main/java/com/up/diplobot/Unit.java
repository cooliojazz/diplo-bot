package com.up.diplobot;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.Serializable;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;

/**
 *
 * @author Ricky
 */
public class Unit implements Serializable {
    
    private Country c;
    private UnitType type;

    public Unit(Country c, UnitType type) {
        this.c = c;
        this.type = type;
    }

    public UnitType getType() {
        return type;
    }
    
    public boolean canMove(TerritoryDescriptor td) {
        if (type == UnitType.ARMY) return td.isLand();
        if (type == UnitType.FLEET) return td.hasWater();
        return false;
    }

    public Country getCountry() {
        return c;
    }
    
    public enum UnitType {
        ARMY("Army", "army.png"), FLEET("Fleet", "fleet.png");
        
        String name;
        BufferedImage img;

        private UnitType(String name, String img) {
            this.name = name;
            try {
                this.img = ImageIO.read(getClass().getResourceAsStream(img));
            } catch (IOException ex) {
                Logger.getLogger(GraphBuilder.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        public BufferedImage getImage() {
            return img;
        }

        @Override
        public String toString() {
            return name;
        }
        
    }
}
