package com.up.diplobot;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

/**
 *
 * @author Ricky
 */
public class AdjacencyGraph implements Serializable {
    
    public static final long serialVersionUID = -3682709334443021234l;

    private ArrayList<TerritoryDescriptor> keys = new ArrayList<>();
    private HashMap<OrderlessPair<TerritoryDescriptor, TerritoryDescriptor>, Boolean> graph = new HashMap<>();
    
    public void addConnection(TerritoryDescriptor t1, TerritoryDescriptor t2) {
        graph.put(new OrderlessPair<>(t1, t2), true);
    }
    
    public void removeConnection(TerritoryDescriptor t1, TerritoryDescriptor t2) {
        graph.put(new OrderlessPair<>(t1, t2), false);
    }
    
    public boolean areConnected(TerritoryDescriptor t1, TerritoryDescriptor t2) {
        Boolean b = graph.get(new OrderlessPair<>(t1, t2));
        return b != null ? b : false;
    }
    
    public boolean areConnectedViaWater(TerritoryDescriptor t1, TerritoryDescriptor t2) {
        boolean[] visited = new boolean[keys.size()];
        ArrayList<TerritoryDescriptor> queue = new ArrayList<>();
        visited[keys.indexOf(t1)] = true;
        queue.add(t1);
        while (queue.size() > 0) {
            TerritoryDescriptor curt = queue.get(0);
            for (TerritoryDescriptor td : getConnections(curt)) {
                if (td == t2) {
                    return true;
                }
                if (td.getType() == TerritoryDescriptor.TerritoryType.WATER && !visited[keys.indexOf(td)]) {
                    queue.add(td);
                    visited[keys.indexOf(td)] = true;
                }
            }
            queue.remove(curt);
        }
        return false;
    }
    
    public boolean areConnectedViaConoys(TerritoryDescriptor t1, TerritoryDescriptor t2, ArrayList<Order> convoys) {
        boolean[] visited = new boolean[keys.size()];
        ArrayList<TerritoryDescriptor> queue = new ArrayList<>();
        visited[keys.indexOf(t1)] = true;
        queue.add(t1);
        while (queue.size() > 0) {
            TerritoryDescriptor curt = queue.get(0);
            for (TerritoryDescriptor td : getConnections(curt)) {
                if (td == t2) {
                    return true;
                }
                if (convoys.stream().anyMatch(o -> o.main.getInfo() == td) && !visited[keys.indexOf(td)]) {
                    queue.add(td);
                    visited[keys.indexOf(td)] = true;
                }
            }
            queue.remove(curt);
        }
        return false;
    }
    
    public void addTerritory(TerritoryDescriptor t) {
        for (TerritoryDescriptor t2 : keys) {
            graph.put(new OrderlessPair<>(t, t2), false);
        }
        keys.add(t);
    }
    
    public void removeTerritory(TerritoryDescriptor t) {
        for (TerritoryDescriptor t2 : keys) {
            graph.remove(new OrderlessPair<>(t, t2));
        }
        keys.remove(t);
    }
    
    public List<TerritoryDescriptor> getConnections(TerritoryDescriptor t1) {
        return graph.entrySet().stream().filter(e -> (e.getKey().s == t1 || e.getKey().t == t1) && e.getValue()).map(e -> e.getKey().s == t1 ? e.getKey().t : e.getKey().s).collect(Collectors.toList());
    }
    
    public List<OrderlessPair<TerritoryDescriptor, TerritoryDescriptor>> getConnections() {
        return graph.entrySet().stream().filter(e -> e.getValue()).map(e -> e.getKey()).collect(Collectors.toList());
    }
    
    public static class OrderlessPair<S, T> implements Serializable {
        public S s;
        public T t;

        public OrderlessPair(S s, T t) {
            this.s = s;
            this.t = t;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof OrderlessPair) {
                OrderlessPair p = (OrderlessPair)obj;
                return s == p.s && t == p.t || s == p.t && t == p.s;
            }
            return false;
        }

        @Override
        public int hashCode() {
            return s.hashCode() + t.hashCode();
        }
            
    }
}
