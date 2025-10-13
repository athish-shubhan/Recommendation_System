package restaurant.recommendation.model;

import java.time.LocalDateTime;
import java.util.List;
import java.util.ArrayList;

/**
 * Offer class as per UML diagram
 * Represents promotional deals applicable to MenuItems
 */
public class Offer {
    // UML specified attributes
    private String offerId;
    private double discountPercent;
    private LocalDateTime validity;

    // Additional attributes for complete functionality
    private String offerName;
    private String description;
    private List<String> applicableItemIds;
    private List<String> applicableCategories;
    private double minimumOrderValue;
    private double maximumDiscount;
    private int usageLimit;
    private int currentUsage;
    private boolean isActive;
    private LocalDateTime createdAt;

    // Constructors
    public Offer() {
        this.applicableItemIds = new ArrayList<>();
        this.applicableCategories = new ArrayList<>();
        this.createdAt = LocalDateTime.now();
        this.validity = LocalDateTime.now().plusDays(30); // Default 30 days
        this.isActive = true;
        this.currentUsage = 0;
        this.usageLimit = Integer.MAX_VALUE; // No limit by default
    }

    public Offer(String offerId, String offerName, double discountPercent) {
        this();
        this.offerId = offerId;
        this.offerName = offerName;
        this.discountPercent = Math.max(0.0, Math.min(100.0, discountPercent));
    }

    public Offer(String offerId, String offerName, double discountPercent, LocalDateTime validity) {
        this(offerId, offerName, discountPercent);
        this.validity = validity;
    }

    // UML specified methods
    public boolean isValidForUser(String userId) {
        if (!isActive || isExpired()) return false;

        // Check usage limit
        if (currentUsage >= usageLimit) return false;

        // Additional user-specific validation could be added here
        // For now, assume all active, non-expired offers are valid for all users
        return true;
    }

    public MenuItem applyOffer(MenuItem menuItem) {
        if (menuItem == null || !isApplicableToItem(menuItem)) {
            return menuItem; // Return unchanged if not applicable
        }

        double originalPrice = menuItem.getPrice();
        double discountAmount = calculateDiscountAmount(originalPrice);
        double newPrice = Math.max(0.0, originalPrice - discountAmount);

        // Create a copy of the menu item with discounted price
        MenuItem discountedItem = new MenuItem(
            menuItem.getItemId() + "_DISCOUNTED",
            menuItem.getName() + " (Discounted)",
            menuItem.getIngredientsList(),
            menuItem.getCategory(),
            newPrice
        );

        // Copy other properties
        discountedItem.setAvailabilityStatus(menuItem.isAvailabilityStatus());
        discountedItem.setAverageRating(menuItem.getAverageRating());
        discountedItem.setTotalRatings(menuItem.getTotalRatings());
        discountedItem.setTags(menuItem.getTags());
        discountedItem.setPopularityScore(menuItem.getPopularityScore());

        // Add discount tag
        discountedItem.addTag("discounted");
        discountedItem.addTag("offer:" + offerId);

        return discountedItem;
    }

    // Offer management methods
    public boolean isApplicableToItem(MenuItem item) {
        if (item == null || !isActive || isExpired()) return false;

        // Check if specific item is included
        if (applicableItemIds.contains(item.getItemId())) {
            return true;
        }

        // Check if item's category is included
        if (item.getCategory() != null) {
            String categoryName = item.getCategory().getCategoryName();
            if (applicableCategories.contains(categoryName)) {
                return true;
            }
        }

        // If no specific items or categories defined, offer applies to all items
        return applicableItemIds.isEmpty() && applicableCategories.isEmpty();
    }

    public double calculateDiscountAmount(double originalPrice) {
        if (originalPrice <= 0) return 0.0;

        double discountAmount = originalPrice * (discountPercent / 100.0);

        // Apply maximum discount limit if set
        if (maximumDiscount > 0) {
            discountAmount = Math.min(discountAmount, maximumDiscount);
        }

        return discountAmount;
    }

    public double calculateFinalPrice(double originalPrice) {
        return Math.max(0.0, originalPrice - calculateDiscountAmount(originalPrice));
    }

    public boolean isExpired() {
        return validity != null && LocalDateTime.now().isAfter(validity);
    }

    public boolean canBeUsed() {
        return isActive && !isExpired() && currentUsage < usageLimit;
    }

    public boolean useOffer() {
        if (!canBeUsed()) return false;

        currentUsage++;
        return true;
    }

    public boolean isEligibleForOrder(double orderValue) {
        return orderValue >= minimumOrderValue;
    }

    // Item and category management
    public void addApplicableItem(String itemId) {
        if (itemId != null && !applicableItemIds.contains(itemId)) {
            applicableItemIds.add(itemId);
        }
    }

    public void removeApplicableItem(String itemId) {
        applicableItemIds.remove(itemId);
    }

    public void addApplicableCategory(String categoryName) {
        if (categoryName != null && !applicableCategories.contains(categoryName)) {
            applicableCategories.add(categoryName);
        }
    }

    public void removeApplicableCategory(String categoryName) {
        applicableCategories.remove(categoryName);
    }

    public void clearApplicableItems() {
        applicableItemIds.clear();
    }

    public void clearApplicableCategories() {
        applicableCategories.clear();
    }

    // Status management
    public void activate() {
        this.isActive = true;
    }

    public void deactivate() {
        this.isActive = false;
    }

    public void extendValidity(int days) {
        if (validity != null) {
            this.validity = this.validity.plusDays(days);
        }
    }

    public void resetUsage() {
        this.currentUsage = 0;
    }

    // Analytics methods
    public double getUsageRate() {
        if (usageLimit == 0 || usageLimit == Integer.MAX_VALUE) return 0.0;
        return (double) currentUsage / usageLimit;
    }

    public long getDaysUntilExpiry() {
        if (validity == null) return Long.MAX_VALUE;

        LocalDateTime now = LocalDateTime.now();
        if (now.isAfter(validity)) return 0;

        return java.time.Duration.between(now, validity).toDays();
    }

    public boolean isPopular() {
        return currentUsage > 50; // Arbitrary threshold
    }

    // Getters and Setters
    public String getOfferId() { return offerId; }
    public void setOfferId(String offerId) { this.offerId = offerId; }

    public String getOfferName() { return offerName; }
    public void setOfferName(String offerName) { this.offerName = offerName; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public double getDiscountPercent() { return discountPercent; }
    public void setDiscountPercent(double discountPercent) { 
        this.discountPercent = Math.max(0.0, Math.min(100.0, discountPercent)); 
    }

    public LocalDateTime getValidity() { return validity; }
    public void setValidity(LocalDateTime validity) { this.validity = validity; }

    public List<String> getApplicableItemIds() { 
        return new ArrayList<>(applicableItemIds); 
    }
    public void setApplicableItemIds(List<String> applicableItemIds) { 
        this.applicableItemIds = applicableItemIds != null ? new ArrayList<>(applicableItemIds) : new ArrayList<>();
    }

    public List<String> getApplicableCategories() { 
        return new ArrayList<>(applicableCategories); 
    }
    public void setApplicableCategories(List<String> applicableCategories) { 
        this.applicableCategories = applicableCategories != null ? new ArrayList<>(applicableCategories) : new ArrayList<>();
    }

    public double getMinimumOrderValue() { return minimumOrderValue; }
    public void setMinimumOrderValue(double minimumOrderValue) { 
        this.minimumOrderValue = Math.max(0.0, minimumOrderValue); 
    }

    public double getMaximumDiscount() { return maximumDiscount; }
    public void setMaximumDiscount(double maximumDiscount) { 
        this.maximumDiscount = Math.max(0.0, maximumDiscount); 
    }

    public int getUsageLimit() { return usageLimit; }
    public void setUsageLimit(int usageLimit) { 
        this.usageLimit = Math.max(0, usageLimit); 
    }

    public int getCurrentUsage() { return currentUsage; }
    public void setCurrentUsage(int currentUsage) { 
        this.currentUsage = Math.max(0, currentUsage); 
    }

    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    @Override
    public String toString() {
        return String.format("Offer{id='%s', name='%s', discount=%.1f%%, usage=%d/%d, expires=%s, active=%b}", 
                           offerId, offerName, discountPercent, currentUsage, 
                           usageLimit == Integer.MAX_VALUE ? "∞" : String.valueOf(usageLimit),
                           validity != null ? validity.toLocalDate() : "never", isActive);
    }
}