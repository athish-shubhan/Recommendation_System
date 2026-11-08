package restaurant.recommendation.model;

import java.util.HashMap;
import java.util.Map;

/**
 * Tracks stock levels per menu item. Items without an explicit level fall back
 * to a default stock (0 unless given to the constructor).
 */
public class Inventory {
    private final Map<String, Integer> stock = new HashMap<>();
    private final int defaultStock;

    public Inventory() {
        this(0);
    }

    public Inventory(int defaultStock) {
        this.defaultStock = Math.max(0, defaultStock);
    }

    public void setStock(String itemId, int quantity) {
        if (itemId == null) return;
        stock.put(itemId, Math.max(0, quantity));
    }

    public int getStock(String itemId) {
        return stock.getOrDefault(itemId, defaultStock);
    }

    public boolean isInStock(String itemId) {
        return getStock(itemId) > 0;
    }
}
