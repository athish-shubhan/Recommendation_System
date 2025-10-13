package restaurant.recommendation.model;

import java.time.LocalDateTime;
import java.util.List;
import java.util.ArrayList;
import java.util.stream.Collectors;
import java.util.Map;
import java.util.HashMap;

public class OrderHistory {
    private List<Order> orderList;

    private String userId;
    private LocalDateTime lastUpdated;

    // Constructor
    public OrderHistory() {
        this.orderList = new ArrayList<>();
        this.lastUpdated = LocalDateTime.now();
    }

    public OrderHistory(String userId) {
        this();
        this.userId = userId;
    }

    public void addOrder(Order order) {
        if (order != null) {
            orderList.add(order);
            this.lastUpdated = LocalDateTime.now();
        }
    }

    public void removeOrder(Order order) {
        if (orderList.remove(order)) {
            this.lastUpdated = LocalDateTime.now();
        }
    }

    public void removeOrderById(String orderId) {
        boolean removed = orderList.removeIf(order -> 
            order.getOrderId() != null && order.getOrderId().equals(orderId));

        if (removed) {
            this.lastUpdated = LocalDateTime.now();
        }
    }

    public Order getOrderById(String orderId) {
        return orderList.stream()
                .filter(order -> order.getOrderId() != null && order.getOrderId().equals(orderId))
                .findFirst()
                .orElse(null);
    }

    public List<Order> getOrdersByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        return orderList.stream()
                .filter(order -> {
                    LocalDateTime orderDate = order.getTimestamp();
                    return orderDate != null && 
                           !orderDate.isBefore(startDate) && 
                           !orderDate.isAfter(endDate);
                })
                .collect(Collectors.toList());
    }

    public List<Order> getRecentOrders(int count) {
        return orderList.stream()
                .sorted((a, b) -> b.getTimestamp().compareTo(a.getTimestamp())) // Most recent first
                .limit(count)
                .collect(Collectors.toList());
    }

    public List<Order> getOrdersWithItem(String itemId) {
        return orderList.stream()
                .filter(order -> order.getItemIds().contains(itemId))
                .collect(Collectors.toList());
    }

    public List<Order> getOrdersWithRating(double minRating, double maxRating) {
        return orderList.stream()
                .filter(order -> {
                    double rating = order.getRating();
                    return rating >= minRating && rating <= maxRating;
                })
                .collect(Collectors.toList());
    }

    // Analytics methods
    public double getAverageOrderValue() {
        if (orderList.isEmpty()) return 0.0;

        return orderList.stream()
                .mapToDouble(Order::getTotalAmount)
                .average()
                .orElse(0.0);
    }

    public double getAverageRating() {
        return orderList.stream()
                .filter(order -> order.getRating() > 0) // Only rated orders
                .mapToDouble(Order::getRating)
                .average()
                .orElse(0.0);
    }

    public int getTotalOrders() {
        return orderList.size();
    }

    public double getTotalSpent() {
        return orderList.stream()
                .mapToDouble(Order::getTotalAmount)
                .sum();
    }

    public Map<String, Integer> getMostOrderedItems() {
        Map<String, Integer> itemFrequency = new HashMap<>();

        for (Order order : orderList) {
            for (String itemId : order.getItemIds()) {
                itemFrequency.merge(itemId, 1, Integer::sum);
            }
        }

        return itemFrequency;
    }

    public Map<String, Integer> getOrderFrequencyByTimeOfDay() {
        Map<String, Integer> timeFrequency = new HashMap<>();

        for (Order order : orderList) {
            LocalDateTime timestamp = order.getTimestamp();
            if (timestamp != null) {
                int hour = timestamp.getHour();
                String timeSlot;

                if (hour >= 6 && hour < 12) {
                    timeSlot = "Morning";
                } else if (hour >= 12 && hour < 17) {
                    timeSlot = "Afternoon";
                } else if (hour >= 17 && hour < 22) {
                    timeSlot = "Evening";
                } else {
                    timeSlot = "Night";
                }

                timeFrequency.merge(timeSlot, 1, Integer::sum);
            }
        }

        return timeFrequency;
    }

    public String getFavoriteOrderingTime() {
        Map<String, Integer> timeFreq = getOrderFrequencyByTimeOfDay();

        return timeFreq.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("Unknown");
    }

    public double getOrderFrequency() {
        if (orderList.isEmpty()) return 0.0;

        LocalDateTime firstOrder = orderList.stream()
                .map(Order::getTimestamp)
                .min(LocalDateTime::compareTo)
                .orElse(LocalDateTime.now());

        LocalDateTime lastOrder = orderList.stream()
                .map(Order::getTimestamp)
                .max(LocalDateTime::compareTo)
                .orElse(LocalDateTime.now());

        long daysBetween = java.time.Duration.between(firstOrder, lastOrder).toDays();

        if (daysBetween == 0) return orderList.size(); // All orders on same day

        return (double) orderList.size() / daysBetween; // Orders per day
    }

    public List<String> getPreferredItems(int topCount) {
        return getMostOrderedItems().entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(topCount)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    // Quality methods
    public boolean isEmpty() {
        return orderList.isEmpty();
    }

    public void clear() {
        orderList.clear();
        this.lastUpdated = LocalDateTime.now();
    }

    public OrderHistory getFilteredCopy(LocalDateTime startDate, LocalDateTime endDate) {
        OrderHistory filtered = new OrderHistory(this.userId);
        filtered.orderList = getOrdersByDateRange(startDate, endDate);
        return filtered;
    }

    // Statistics summary
    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new HashMap<>();

        stats.put("totalOrders", getTotalOrders());
        stats.put("totalSpent", getTotalSpent());
        stats.put("averageOrderValue", getAverageOrderValue());
        stats.put("averageRating", getAverageRating());
        stats.put("favoriteOrderingTime", getFavoriteOrderingTime());
        stats.put("orderFrequency", getOrderFrequency());
        stats.put("preferredItems", getPreferredItems(5));

        if (!orderList.isEmpty()) {
            LocalDateTime firstOrder = orderList.stream()
                    .map(Order::getTimestamp)
                    .min(LocalDateTime::compareTo)
                    .orElse(null);

            LocalDateTime lastOrder = orderList.stream()
                    .map(Order::getTimestamp)
                    .max(LocalDateTime::compareTo)
                    .orElse(null);

            stats.put("firstOrderDate", firstOrder);
            stats.put("lastOrderDate", lastOrder);
        }

        return stats;
    }

    // Getters and Setters
    public List<Order> getOrderList() {
        return new ArrayList<>(orderList);
    }

    public void setOrderList(List<Order> orderList) {
        this.orderList = orderList != null ? new ArrayList<>(orderList) : new ArrayList<>();
        this.lastUpdated = LocalDateTime.now();
    }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public LocalDateTime getLastUpdated() { return lastUpdated; }

    @Override
    public String toString() {
        return String.format("OrderHistory{userId='%s', orders=%d, totalSpent=%.2f, avgRating=%.1f}", 
                           userId, orderList.size(), getTotalSpent(), getAverageRating());
    }
}