package restaurant.recommendation.model;

import java.time.LocalDateTime;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

public class Order {
    private String orderId;
    private String userId;
    private List<String> itemIds;
    private Map<String, Integer> itemQuantities;
    private double totalAmount;
    private LocalDateTime timestamp;
    private double rating;
    private String feedback;
    private String status;
    private String paymentMethod;

    // Constructors
    public Order() {
        this.itemIds = new ArrayList<>();
        this.itemQuantities = new HashMap<>();
        this.timestamp = LocalDateTime.now();
        this.rating = 0.0;
        this.status = "PENDING";
    }

    public Order(String orderId, String userId) {
        this();
        this.orderId = orderId;
        this.userId = userId;
    }

    public Order(String orderId, String userId, List<String> itemIds, double totalAmount) {
        this(orderId, userId);
        this.itemIds = new ArrayList<>(itemIds);
        this.totalAmount = totalAmount;
    }

    // Order management methods
    public void addItem(String itemId, int quantity) {
        if (itemId != null && quantity > 0) {
            if (!itemIds.contains(itemId)) {
                itemIds.add(itemId);
            }
            itemQuantities.put(itemId, itemQuantities.getOrDefault(itemId, 0) + quantity);
        }
    }

    public void removeItem(String itemId) {
        if (itemId != null) {
            itemIds.remove(itemId);
            itemQuantities.remove(itemId);
        }
    }

    public int getItemQuantity(String itemId) {
        return itemQuantities.getOrDefault(itemId, 0);
    }

    public int getTotalItemCount() {
        return itemQuantities.values().stream().mapToInt(Integer::intValue).sum();
    }

    public boolean containsItem(String itemId) {
        return itemIds.contains(itemId);
    }

    // Rating and feedback
    public void setRatingAndFeedback(double rating, String feedback) {
        this.rating = Math.max(1.0, Math.min(5.0, rating));
        this.feedback = feedback;
    }

    public boolean isRated() {
        return rating > 0;
    }

    // Order status management
    public void updateStatus(String status) {
        if (status != null) {
            this.status = status.toUpperCase();
        }
    }

    public boolean isCompleted() {
        return "COMPLETED".equals(status) || "DELIVERED".equals(status);
    }

    public boolean isPending() {
        return "PENDING".equals(status);
    }

    public boolean isCancelled() {
        return "CANCELLED".equals(status);
    }

    // Analytics methods
    public double getAverageItemPrice() {
        if (getTotalItemCount() == 0) return 0.0;
        return totalAmount / getTotalItemCount();
    }

    public LocalDateTime getOrderDate() {
        return timestamp != null ? timestamp.toLocalDate().atStartOfDay() : null;
    }

    public String getTimeOfDay() {
        if (timestamp == null) return "Unknown";

        int hour = timestamp.getHour();
        if (hour >= 6 && hour < 12) return "Morning";
        else if (hour >= 12 && hour < 17) return "Afternoon";
        else if (hour >= 17 && hour < 22) return "Evening";
        else return "Night";
    }

    public Map<String, Object> getOrderSummary() {
        Map<String, Object> summary = new HashMap<>();
        summary.put("orderId", orderId);
        summary.put("userId", userId);
        summary.put("itemCount", getTotalItemCount());
        summary.put("totalAmount", totalAmount);
        summary.put("averageItemPrice", getAverageItemPrice());
        summary.put("timestamp", timestamp);
        summary.put("timeOfDay", getTimeOfDay());
        summary.put("rating", rating);
        summary.put("status", status);
        summary.put("isRated", isRated());
        return summary;
    }

    // Getters and Setters
    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public List<String> getItemIds() { return new ArrayList<>(itemIds); }
    public void setItemIds(List<String> itemIds) { 
        this.itemIds = itemIds != null ? new ArrayList<>(itemIds) : new ArrayList<>();
    }

    public Map<String, Integer> getItemQuantities() { return new HashMap<>(itemQuantities); }
    public void setItemQuantities(Map<String, Integer> itemQuantities) {
        this.itemQuantities = itemQuantities != null ? new HashMap<>(itemQuantities) : new HashMap<>();
    }

    public double getTotalAmount() { return totalAmount; }
    public void setTotalAmount(double totalAmount) { this.totalAmount = Math.max(0.0, totalAmount); }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }

    public double getRating() { return rating; }
    public void setRating(double rating) { this.rating = Math.max(0.0, Math.min(5.0, rating)); }

    public String getFeedback() { return feedback; }
    public void setFeedback(String feedback) { this.feedback = feedback; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }

    @Override
    public String toString() {
        return String.format("Order{id='%s', user='%s', items=%d, amount=%.2f, rating=%.1f, status='%s'}", 
                           orderId, userId, getTotalItemCount(), totalAmount, rating, status);
    }
}