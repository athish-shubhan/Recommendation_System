package restaurant.recommendation.model;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;
import java.util.List;
import java.util.ArrayList;

/**
 * Inventory class as per UML diagram
 * Tracks stock and availability status of MenuItems
 */
public class Inventory {
    private Map<String, Integer> stockLevels; // itemId -> stock count
    private Map<String, Integer> reservedStock; // itemId -> reserved count
    private Map<String, LocalDateTime> lastRestocked; // itemId -> last restock time
    private int lowStockThreshold;
    private LocalDateTime lastUpdated;

    // Constructor
    public Inventory() {
        this.stockLevels = new HashMap<>();
        this.reservedStock = new HashMap<>();
        this.lastRestocked = new HashMap<>();
        this.lowStockThreshold = 5; // Default low stock threshold
        this.lastUpdated = LocalDateTime.now();
    }

    public Inventory(int lowStockThreshold) {
        this();
        this.lowStockThreshold = Math.max(1, lowStockThreshold);
    }

    // UML specified methods
    public boolean checkStock(String menuItemId) {
        if (menuItemId == null) return false;

        int available = getAvailableStock(menuItemId);
        return available > 0;
    }

    public void updateStock(String menuItemId, int quantityChange) {
        if (menuItemId == null) return;

        int currentStock = stockLevels.getOrDefault(menuItemId, 0);
        int newStock = Math.max(0, currentStock + quantityChange);

        stockLevels.put(menuItemId, newStock);
        this.lastUpdated = LocalDateTime.now();

        if (quantityChange > 0) {
            lastRestocked.put(menuItemId, LocalDateTime.now());
        }

        System.out.println(String.format("Stock updated: %s %s %d (Total: %d)", 
                                        menuItemId, 
                                        quantityChange > 0 ? "+" : "", 
                                        quantityChange, 
                                        newStock));

        // Check for low stock warning
        if (newStock <= lowStockThreshold && newStock > 0) {
            System.out.println("⚠️ Low stock warning: " + menuItemId + " (" + newStock + " remaining)");
        } else if (newStock == 0) {
            System.out.println("❌ Out of stock: " + menuItemId);
        }
    }

    // Extended inventory management methods
    public boolean isInStock(String itemId) {
        return checkStock(itemId);
    }

    public int getStockLevel(String itemId) {
        return stockLevels.getOrDefault(itemId, 0);
    }

    public int getAvailableStock(String itemId) {
        int total = stockLevels.getOrDefault(itemId, 0);
        int reserved = reservedStock.getOrDefault(itemId, 0);
        return Math.max(0, total - reserved);
    }

    public void setStock(String itemId, int quantity) {
        if (itemId != null && quantity >= 0) {
            stockLevels.put(itemId, quantity);
            lastRestocked.put(itemId, LocalDateTime.now());
            this.lastUpdated = LocalDateTime.now();
        }
    }

    public boolean reserveStock(String itemId, int quantity) {
        if (itemId == null || quantity <= 0) return false;

        int available = getAvailableStock(itemId);
        if (available >= quantity) {
            reservedStock.put(itemId, reservedStock.getOrDefault(itemId, 0) + quantity);
            this.lastUpdated = LocalDateTime.now();
            return true;
        }

        return false;
    }

    public void releaseReservedStock(String itemId, int quantity) {
        if (itemId != null && quantity > 0) {
            int currentReserved = reservedStock.getOrDefault(itemId, 0);
            int newReserved = Math.max(0, currentReserved - quantity);

            if (newReserved == 0) {
                reservedStock.remove(itemId);
            } else {
                reservedStock.put(itemId, newReserved);
            }

            this.lastUpdated = LocalDateTime.now();
        }
    }

    public void consumeReservedStock(String itemId, int quantity) {
        if (itemId != null && quantity > 0) {
            // Release reservation
            releaseReservedStock(itemId, quantity);

            // Reduce actual stock
            updateStock(itemId, -quantity);
        }
    }

    // Stock analysis methods
    public boolean isLowStock(String itemId) {
        return getStockLevel(itemId) <= lowStockThreshold && getStockLevel(itemId) > 0;
    }

    public boolean isOutOfStock(String itemId) {
        return getStockLevel(itemId) == 0;
    }

    public Set<String> getLowStockItems() {
        Set<String> lowStockItems = new HashSet<>();

        for (Map.Entry<String, Integer> entry : stockLevels.entrySet()) {
            if (entry.getValue() <= lowStockThreshold && entry.getValue() > 0) {
                lowStockItems.add(entry.getKey());
            }
        }

        return lowStockItems;
    }

    public Set<String> getOutOfStockItems() {
        Set<String> outOfStockItems = new HashSet<>();

        for (Map.Entry<String, Integer> entry : stockLevels.entrySet()) {
            if (entry.getValue() == 0) {
                outOfStockItems.add(entry.getKey());
            }
        }

        return outOfStockItems;
    }

    public Set<String> getAvailableItems() {
        Set<String> availableItems = new HashSet<>();

        for (Map.Entry<String, Integer> entry : stockLevels.entrySet()) {
            if (entry.getValue() > 0) {
                availableItems.add(entry.getKey());
            }
        }

        return availableItems;
    }

    public Map<String, Integer> getStockReport() {
        return new HashMap<>(stockLevels);
    }

    public int getTotalItems() {
        return stockLevels.size();
    }

    public int getTotalStock() {
        return stockLevels.values().stream().mapToInt(Integer::intValue).sum();
    }

    public int getTotalReservedStock() {
        return reservedStock.values().stream().mapToInt(Integer::intValue).sum();
    }

    public int getTotalAvailableStock() {
        return getTotalStock() - getTotalReservedStock();
    }

    // Bulk operations
    public void restockAll(int quantity) {
        LocalDateTime now = LocalDateTime.now();

        for (String itemId : stockLevels.keySet()) {
            updateStock(itemId, quantity);
            lastRestocked.put(itemId, now);
        }

        System.out.println("✓ Restocked all items with " + quantity + " units");
    }

    public void restockLowStockItems(int targetQuantity) {
        Set<String> lowStockItems = getLowStockItems();

        for (String itemId : lowStockItems) {
            int currentStock = getStockLevel(itemId);
            int restockQuantity = Math.max(0, targetQuantity - currentStock);

            if (restockQuantity > 0) {
                updateStock(itemId, restockQuantity);
            }
        }

        System.out.println("✓ Restocked " + lowStockItems.size() + " low-stock items");
    }

    public void clearExpiredItems(List<String> expiredItemIds) {
        if (expiredItemIds != null) {
            for (String itemId : expiredItemIds) {
                stockLevels.put(itemId, 0);
                reservedStock.remove(itemId);
            }

            this.lastUpdated = LocalDateTime.now();
            System.out.println("✓ Cleared " + expiredItemIds.size() + " expired items");
        }
    }

    // Reporting methods
    public Map<String, Object> getInventoryStatus() {
        Map<String, Object> status = new HashMap<>();

        status.put("totalItems", getTotalItems());
        status.put("totalStock", getTotalStock());
        status.put("totalReservedStock", getTotalReservedStock());
        status.put("totalAvailableStock", getTotalAvailableStock());
        status.put("lowStockCount", getLowStockItems().size());
        status.put("outOfStockCount", getOutOfStockItems().size());
        status.put("availableItemsCount", getAvailableItems().size());
        status.put("lowStockThreshold", lowStockThreshold);
        status.put("lastUpdated", lastUpdated);

        return status;
    }

    public List<Map<String, Object>> getDetailedStockReport() {
        List<Map<String, Object>> report = new ArrayList<>();

        for (Map.Entry<String, Integer> entry : stockLevels.entrySet()) {
            String itemId = entry.getKey();
            int stock = entry.getValue();
            int reserved = reservedStock.getOrDefault(itemId, 0);
            int available = stock - reserved;

            Map<String, Object> itemReport = new HashMap<>();
            itemReport.put("itemId", itemId);
            itemReport.put("totalStock", stock);
            itemReport.put("reservedStock", reserved);
            itemReport.put("availableStock", available);
            itemReport.put("status", stock == 0 ? "OUT_OF_STOCK" : 
                                   stock <= lowStockThreshold ? "LOW_STOCK" : "IN_STOCK");
            itemReport.put("lastRestocked", lastRestocked.get(itemId));

            report.add(itemReport);
        }

        return report;
    }

    // Getters and Setters
    public int getLowStockThreshold() { return lowStockThreshold; }
    public void setLowStockThreshold(int lowStockThreshold) { 
        this.lowStockThreshold = Math.max(1, lowStockThreshold); 
    }

    public LocalDateTime getLastUpdated() { return lastUpdated; }

    @Override
    public String toString() {
        return String.format("Inventory{items=%d, totalStock=%d, available=%d, lowStock=%d, outOfStock=%d}", 
                           getTotalItems(), getTotalStock(), getTotalAvailableStock(), 
                           getLowStockItems().size(), getOutOfStockItems().size());
    }
}