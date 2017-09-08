package com.up.diplobot;

/**
 *
 * @author Ricky
 */
public class Territory {
    private TerritoryDescriptor info;
    private Country owner;
    private Unit occupant = null;

    public Territory(TerritoryDescriptor info) {
        this.info = info;
        this.owner = info.getStartingCountry();
        if (info.getStartType() != null) occupant = new Unit(owner, info.getStartType());
    }

    public Unit getOccupant() {
        return occupant;
    }

    public void setOccupant(Unit occupant) {
        this.occupant = occupant;
    }

    public Country getOwner() {
        return owner;
    }

    public void setOwner(Country owner) {
        this.owner = owner;
    }

    public TerritoryDescriptor getInfo() {
        return info;
    }
    
}
