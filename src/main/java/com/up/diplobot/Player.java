package com.up.diplobot;

import java.io.Serializable;
import java.util.ArrayList;
import net.dv8tion.jda.core.entities.User;

/**
 *
 * @author Ricky
 */
public class Player implements Serializable {
    private Country c;
    private long uid;
    private ArrayList<Order> orders = new ArrayList<>();

    public Player(Country c, User u) {
        this.c = c;
        setUser(u);
    }

    public Country getCountry() {
        return c;
    }

    public void setCountry(Country c) {
        this.c = c;
    }

    public User getUser() {
        return Main.jda.getUserById(uid);
    }

    public long getUserId() {
        return uid;
    }

    public void setUser(User u) {
        this.uid = u.getIdLong();
    }
    
    public void addOrder(Order o) {
        orders.add(o);
    }
    
    public void clearOrders() {
        orders.clear();
    }

    public ArrayList<Order> getOrders() {
        return orders;
    }
    
}
