package com.up.diplobot;

import java.awt.Color;
import java.awt.geom.Point2D;
import java.io.Serializable;

/**
 *
 * @author Ricky
 */
public class TerritoryDescriptor implements Serializable {
    public static final long serialVersionUID = -3383384884390892925l;
    private TerritoryType type;
    private Point2D.Double pos;
    private String name = "";
    private String abbreviation = "";
    private Country startingc;
    private boolean supply = false;
    private Unit.UnitType stype = null;

    public TerritoryDescriptor(TerritoryType type, double x, double y) {
        this.type = type;
        setPosition(x, y);
    }
    
    public TerritoryDescriptor(TerritoryType type, Point2D.Double pos) {
        this.type = type;
        this.pos = pos;
    }

    public TerritoryDescriptor() {
        this(TerritoryType.LAND, new Point2D.Double(0, 0));
    }
    
    public boolean isLand() {
        return type == TerritoryType.LAND || type == TerritoryType.COAST;
    }
    
    public boolean hasWater() {
        return type == TerritoryType.WATER || type == TerritoryType.COAST;
    }

    public void setType(TerritoryType type) {
        this.type = type;
    }
    
    public TerritoryType getType() {
        return type;
    }
    
    public void setPosition(double x, double y) {
        this.pos = new Point2D.Double(x, y);
    }
    
    public Point2D.Double getPosition() {
        return pos;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getName() {
        return name;
    }

    public void setAbbreviation(String abbreviation) {
        this.abbreviation = abbreviation;
    }

    public String getAbbreviation() {
        return abbreviation;
    }

    public void setStartingCountry(Country startingc) {
        this.startingc = startingc;
    }

    public Country getStartingCountry() {
        return startingc;
    }

    public boolean isSupplyCenter() {
        return supply;
    }

    public void setSupplyCenter(boolean supply) {
        this.supply = supply;
    }

    public void setStartType(Unit.UnitType stype) {
        this.stype = stype;
    }

    public Unit.UnitType getStartType() {
        return stype;
    }

    @Override
    public String toString() {
        return stype + " - " + name + " (" + abbreviation + ")";
    }
    
    public enum TerritoryType {
        WATER("Ocean", Color.BLUE.darker()), COAST("Coast", Color.CYAN.darker()), LAND("Land", Color.GREEN.darker());
        
        private String text;
        public Color c;

        private TerritoryType(String text, Color c) {
            this.text = text;
            this.c = c;
        }

        @Override
        public String toString() {
            return text;
        }
        
    }
}
