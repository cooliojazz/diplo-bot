package com.up.diplobot;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * @author Ricky
 */
public class Order {
    public final static Pattern holdp = Pattern.compile("(A|F) ([A-Z]{3}) H");
    public final static Pattern movep = Pattern.compile("(A|F) ([A-Z]{3})-([A-Z]{3})");
    public final static Pattern suppp = Pattern.compile("(A|F) ([A-Z]{3}) S (A|F) ([A-Z]{3})(?:-([A-Z]{3}))?");
    public final static Pattern convp = Pattern.compile("F ([A-Z]{3}) C A ([A-Z]{3})-([A-Z]{3})");
    
//    String order;
    OrderType type;
    Territory main;
    Territory dest;
    boolean valid = true;
    String vmsg = "";
    private Game g;

    public Order(Game g, String order) {
        this.g = g;
//        this.order = order;
        Matcher holdm = holdp.matcher(order);
        Matcher movem = movep.matcher(order);
        Matcher suppm = suppp.matcher(order);
        Matcher convm = convp.matcher(order);
        if (holdm.find()) {
            type = OrderType.HOLD;
            main = g.getTerritoryFromCode(holdm.group(2));
            if (main != null) {
                if (main.getOccupant() != null) {
                    Unit.UnitType ut = getUnitTypeFromCode(holdm.group(1));
                    if (ut != main.getOccupant().getType()) {
                        incorrectUnitCode(main.getInfo(), ut);
                    }
                } else {
                    missingUnitCode(main.getInfo());
                }
            } else {
                invalidTerritoyCode(holdm.group(2));
            }
        } else if (movem.find()) {
            type = OrderType.MOVE;
            main = g.getTerritoryFromCode(movem.group(2));
            if (main != null) {
                if (main.getOccupant() != null) {
                    Unit.UnitType ut = getUnitTypeFromCode(movem.group(1));
                    if (ut != main.getOccupant().getType()) {
                        incorrectUnitCode(main.getInfo(), ut);
                    }
                } else {
                    missingUnitCode(main.getInfo());
                }
                dest = g.getTerritoryFromCode(movem.group(3));
                if (dest != null) {
                    if (!g.graph.areConnected(main.getInfo(), dest.getInfo())) {
                        //Has convoy?
                        if (g.graph.areConnectedViaWater(main.getInfo(), dest.getInfo())) {
                            valid = false;
                            vmsg = "Convoys not supported atm, but this might be okay?";
                        } else {
                            notAdjacentTerritoy(main.getInfo(), dest.getInfo());
                        }
                    }
                } else {
                    invalidTerritoyCode(movem.group(3));
                }
            } else {
                invalidTerritoyCode(movem.group(2));
            }
        } else if (suppm.find()) {
            type = OrderType.SUPPORT;
        } else if (convm.find()) {
            type = OrderType.CONVOY;
        } else {
            invalidOrder();
        }
    }
    
    private Unit.UnitType getUnitTypeFromCode(String code) {
        if (code.equals("A")) return Unit.UnitType.ARMY;
        if (code.equals("F")) return Unit.UnitType.FLEET;
        return null;
    }
    
    private void incorrectUnitCode(TerritoryDescriptor td, Unit.UnitType ut) {
        valid = false;
        vmsg = "I do apologize, but I do not see a " + ut + " in " + td.getName() + ".";
    }
    
    private void missingUnitCode(TerritoryDescriptor td) {
        valid = false;
        vmsg = "I do apologize, but I do not see a unit in " + td.getName() + ".";
    }
    
    private void invalidTerritoyCode(String code) {
        valid = false;
        vmsg = "I do apologize, but I have not heard of the province called " + code + ".";
    }
    
    private void notAdjacentTerritoy(TerritoryDescriptor td1, TerritoryDescriptor td2) {
        valid = false;
        vmsg = "I do apologize, but " + td1.getName() + " and " + td2.getName() + " are not close enough that such a momevemnt would be possible in a single season!.";
    }
    
    private void invalidOrder() {
        valid = false;
        vmsg = "I do apologize, but I don't understand the order you are trying to give.";
    }

    public boolean isValid() {
        return valid;
    }

//    public String getOrder() {
//        return order;
//    }

    @Override
    public String toString() {
        switch (type) {
            case HOLD:
                return main.getOccupant().getType() + " in " + main.info.getName() + " should hold.";
            case MOVE:
                return main.getOccupant().getType() + " in " + main.info.getName() + " should move to " + dest.info.getName() + ".";
            case SUPPORT:
                return "";
            case CONVOY:
                return "";
        }
        return "";
    }
    
    public enum OrderType {
        HOLD, MOVE, SUPPORT, CONVOY;
    }
}
