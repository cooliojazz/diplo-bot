package com.up.diplobot;

import java.io.Serializable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * @author Ricky
 */
public class Order implements Serializable {
    public final static Pattern selp = Pattern.compile("^(A|F) ([A-Z]{3})");
    public final static Pattern holdp = Pattern.compile("^(A|F) ([A-Z]{3}) H");
    public final static Pattern movep = Pattern.compile("^(A|F) ([A-Z]{3})-([A-Z]{3})");
    public final static Pattern suppp = Pattern.compile("^(A|F) ([A-Z]{3}) S (A|F) ([A-Z]{3})(?:-([A-Z]{3}))?");
    public final static Pattern convp = Pattern.compile("^(A|F) ([A-Z]{3}) C (A|F) ([A-Z]{3})-([A-Z]{3})");
    
    private OrderType type;
    Territory main = null;
    Territory dest = null;
    Territory smain = null;
    private boolean valid = true;
    String vmsg = "";
    private boolean failed = false;
    private Country sender;
    public Unit.UnitType ut;

    public Order(Game g, String order, Player p) {
        order = order.toUpperCase();
        Matcher selm = selp.matcher(order);
        Matcher holdm = holdp.matcher(order);
        Matcher movem = movep.matcher(order);
        Matcher suppm = suppp.matcher(order);
        Matcher convm = convp.matcher(order);
        if (holdm.find()) {
            type = OrderType.HOLD;
            main = g.getTerritoryFromCode(holdm.group(2));
            if (main != null) {
                if (main.getOwner() == p.getCountry()) {
                    if (main.getOccupant() != null) {
                        Unit.UnitType ut = getUnitTypeFromCode(holdm.group(1));
                        if (ut != main.getOccupant().getType()) {
                            incorrectUnitCode(main.getInfo(), ut);
                        }
                    } else {
                        missingUnitCode(main.getInfo());
                    }
                } else {
                    invalidOwner(main);
                }
            } else {
                invalidTerritoyCode(holdm.group(2));
            }
        } else if (movem.find()) {
            type = OrderType.MOVE;
            main = g.getTerritoryFromCode(movem.group(2));
            if (main != null) {
                if (main.getOccupant() != null) {
                    if (main.getOccupant().getCountry() == p.getCountry()) {
                        Unit.UnitType ut = getUnitTypeFromCode(movem.group(1));
                        if (ut != main.getOccupant().getType()) {
                            incorrectUnitCode(main.getInfo(), ut);
                        }
                    } else {
//                        invalidOwner(main);
                    }
                } else {
                    missingUnitCode(main.getInfo());
                }
                dest = g.getTerritoryFromCode(movem.group(3));
                if (dest != null) {
                    if (main.getOccupant() != null) {
                        if (main.getOccupant().canMove(dest.getInfo())) {
                            if (!g.graph.areConnected(main.getInfo(), dest.getInfo())) {
                                //Has convoy?
                                if (!g.graph.areConnectedViaWater(main.getInfo(), dest.getInfo())) {
                                    notAdjacentTerritoy(main.getInfo(), dest.getInfo());
                                }
                            }
                        } else {
                            invalidTerritoryTypeForUnit(main.getOccupant(), dest.getInfo());
                        }
                    } else {
                        missingUnitCode(main.getInfo());
                    }
                } else {
                    invalidTerritoyCode(movem.group(3));
                }
            } else {
                invalidTerritoyCode(movem.group(2));
            }
        } else if (suppm.find()) {
            type = OrderType.SUPPORT;
            main = g.getTerritoryFromCode(suppm.group(2));
            if (main != null) {
                if (main.getOccupant() != null) {
                    if (main.getOccupant().getCountry() == p.getCountry()) {
                        Unit.UnitType ut = getUnitTypeFromCode(suppm.group(1));
                        if (ut != main.getOccupant().getType()) {
                            incorrectUnitCode(main.getInfo(), ut);
                        }
                    } else {
                        invalidOwner(main);
                    }
                } else {
                    missingUnitCode(main.getInfo());
                }
                smain = g.getTerritoryFromCode(suppm.group(4));
                if (smain != null) {
                    if (smain.getOccupant() != null) {
                        Unit.UnitType ut = getUnitTypeFromCode(suppm.group(3));
                        if (ut != smain.getOccupant().getType()) {
                            incorrectUnitCode(smain.getInfo(), ut);
                        }
                    } else {
                        missingUnitCode(smain.getInfo());
                    }
                    if (suppm.groupCount() == 5) {
                        dest = g.getTerritoryFromCode(suppm.group(5));
                        if (dest != null) {
                            if (main.getInfo() != dest.getInfo()) {
                                if (!g.graph.areConnected(main.getInfo(), dest.getInfo())) {
                                    notAdjacentTerritoy(main.getInfo(), dest.getInfo());
                                }
                            } else {
                                valid = false;
                                vmsg = "I do apologize, but you cannot support an attack to the territory you are supporting from!";
                            }
                        }
                    }
                }
            } else {
                invalidTerritoyCode(suppm.group(2));
            }
        } else if (convm.find()) {
            type = OrderType.CONVOY;
            main = g.getTerritoryFromCode(convm.group(2));
            if (main != null) {
                    if (main.getInfo().getType() == TerritoryDescriptor.TerritoryType.WATER) {
                    if (main.getOccupant() != null) {
                        if (main.getOccupant().getCountry()== p.getCountry()) {
                            Unit.UnitType ut = getUnitTypeFromCode(convm.group(1));
                            if (ut == Unit.UnitType.FLEET) {
                                if (ut != main.getOccupant().getType()) {
                                    incorrectUnitCode(main.getInfo(), ut);
                                }
                            } else {
                                invalidConvoyType();
                            }
                        } else {
                            invalidOwner(main);
                        }
                    } else {
                        missingUnitCode(main.getInfo());
                    }
                    smain = g.getTerritoryFromCode(convm.group(4));
                    if (smain != null) {
                        if (smain.getOccupant() != null) {
                            Unit.UnitType ut = getUnitTypeFromCode(convm.group(3));
                            if (ut == Unit.UnitType.ARMY) {
                                if (ut != smain.getOccupant().getType()) {
                                    incorrectUnitCode(smain.getInfo(), ut);
                                }
                            } else {
                                invalidConvoyType();
                            }
                        } else {
                            missingUnitCode(smain.getInfo());
                        }
                        dest = g.getTerritoryFromCode(convm.group(5));
                        if (dest != null) {
                            if (!g.graph.areConnectedViaWater(smain.getInfo(), dest.getInfo())) {
                                notAdjacentTerritoy(main.getInfo(), dest.getInfo());
                            }
                        } else {
                            invalidTerritoyCode(convm.group(5));
                        }
                    }
                } else {
                    valid = false;
                    vmsg = "I do apologize, but you cannot convoy units across land!";
                }
            } else {
                invalidTerritoyCode(convm.group(2));
            }
        } else if (selm.find()) {
            type = OrderType.SELECT;
            main = g.getTerritoryFromCode(selm.group(2));
            if (main != null) {
                if (main.getOccupant().getCountry() == p.getCountry()) {
//                    if (main.getOccupant() != null) {
                        ut = getUnitTypeFromCode(selm.group(1));
//                        if (ut != main.getOccupant().getType()) {
//                            incorrectUnitCode(main.getInfo(), ut);
//                        }
//                    } else {
//                        missingUnitCode(main.getInfo());
//                    }
                } else {
                    invalidOwner(main);
                }
            } else {
                invalidTerritoyCode(selm.group(2));
            }
        } else {
            invalidOrder();
        }
    }

    public Order(OrderType type, Country sender, Territory main) {
        this.type = type;
        this.sender = sender;
        this.main = main;
    }

    public OrderType getType() {
        return type;
    }
    
    private Unit.UnitType getUnitTypeFromCode(String code) {
        if (code.equals("A")) return Unit.UnitType.ARMY;
        if (code.equals("F")) return Unit.UnitType.FLEET;
        return null;
    }
    
    private void incorrectUnitCode(TerritoryDescriptor td, Unit.UnitType ut) {
        valid = false;
        vmsg = "I do apologize, but I don't see a(n) " + ut + " in " + td.getName() + ".";
    }
    
    private void invalidConvoyType() {
        valid = false;
        vmsg = "I do apologize, but only a Fleet can convoy units, and they only know how to carry an Army.";
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
    
    private void invalidTerritoryTypeForUnit(Unit u, TerritoryDescriptor td) {
        valid = false;
        vmsg = "I do apologize, but a(n) " + u.getType() + " cannot move into " + td.getType();
    }
    
    private void invalidOwner(Territory t) {
        valid = false;
        vmsg = "I do apologize, but " + t.getInfo().getName() + " does not belog to you, and therefore the unit stationed there will not listen to your orders!";
    }

    public boolean isValid() {
        return valid;
    }
    
    public boolean isFailed() {
        return failed;
    }

    public void setFailed(boolean failed) {
        this.failed = failed;
    }

    public void setSender(Country sender) {
        this.sender = sender;
    }

    public Country getSender() {
        return sender;
    }

    @Override
    public String toString() {
        switch (type) {
            case SELECT:
                return main.getOccupant().getType() + " in *" + main.getInfo().getName() + "*";
            case HOLD:
                return main.getOccupant().getType() + " in *" + main.getInfo().getName() + "* should hold.";
            case MOVE:
                return main.getOccupant().getType() + " in *" + main.getInfo().getName() + "* should move to *" + dest.getInfo().getName() + "*.";
            case SUPPORT:
                return main.getOccupant().getType() + " in *" + main.getInfo().getName() + "* should support the " + smain.getOccupant().getType() + " in *" + smain.getInfo().getName() + "*" + (dest == null ? "" : " moving to *" + dest.getInfo().getName()) + "*.";
            case CONVOY:
                return main.getOccupant().getType() + " in *" + main.getInfo().getName() + "* should help convoy the " + smain.getOccupant().getType() + " in *" + smain.getInfo().getName() + "* to *" + dest.getInfo().getName() + "*.";
        }
        return "";
    }
    
    public enum OrderType {
        HOLD, MOVE, SUPPORT, CONVOY, SELECT;
    }
}
