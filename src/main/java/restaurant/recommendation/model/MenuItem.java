package restaurant.recommendation.model;

import java.util.List;
import java.util.ArrayList;
import java.util.Objects;

/**
 * MenuItem class implementing Comparable interface as per UML diagram
 * Represents individual menu items with all attributes and methods specified
 */
public class MenuItem implements Comparable<MenuItem> {
    // UML specified attributes
    private String itemId;
    private List<String> ingredientsList;
    private double price;
    private double popularityScore;
    private boolean availabilityStatus;

    // Additional attributes for complete functionality
    private String name;
    private MenuCategory category;
    private double averageRating;
    private int totalRatings;
    private List<String> tags;

    // Constructors
    public MenuItem() {
        this.ingredientsList = new ArrayList<>();
        this.tags = new ArrayList<>();
        this.availabilityStatus = true;
        this.popularityScore = 0.0;
        this.averageRating = 0.0;
        this.totalRatings = 0;
    }

    public MenuItem(String itemId, String name, List<String> ingredientsList, 
                   MenuCategory category, double price) {
        this();
        this.itemId = itemId;
        this.name = name;
        this.ingredientsList = new ArrayList<>(ingredientsList);
        this.category = category;
        this.price = price;
    }

    // UML specified methods
    public String getDetails() {
        return String.format("MenuItem{id='%s', name='%s', price=%.2f, rating=%.1f, available=%b}", 
                           itemId, name, price, averageRating, availabilityStatus);
    }

    public boolean isAvailable() {
        return availabilityStatus;
    }

    // Additional methods for complete functionality
    public void updateRating(double newRating) {
        if (totalRatings == 0) {
            this.averageRating = newRating;
            this.totalRatings = 1;
        } else {
            double totalScore = this.averageRating * this.totalRatings;
            this.totalRatings++;
            this.averageRating = (totalScore + newRating) / this.totalRatings;
        }

        // Update popularity based on rating
        updatePopularityFromRating();
    }

    public boolean isVegetarian() {
        return tags != null && (tags.contains("vegetarian") || tags.contains("vegan"));
    }

    public boolean isVegan() {
        return tags != null && tags.contains("vegan");
    }

    public boolean isSpicy() {
        return tags != null && tags.contains("spicy");
    }

    public boolean isSweet() {
        return tags != null && tags.contains("sweet");
    }

    public boolean hasAllergen(String allergen) {
        if (ingredientsList == null) return false;

        return ingredientsList.stream()
                .anyMatch(ingredient -> ingredient.toLowerCase().contains(allergen.toLowerCase()));
    }

    public boolean isHealthy() {
        return tags != null && (tags.contains("healthy") || tags.contains("low-fat") || tags.contains("organic"));
    }

    public boolean isCold() {
        return tags != null && tags.contains("cold");
    }

    public boolean isHot() {
        return tags != null && tags.contains("hot");
    }

    // Comparable implementation - sorts by popularity score (descending)
    @Override
    public int compareTo(MenuItem other) {
        if (other == null) return 1;

        // Primary sort: popularity score (descending)
        int popularityComparison = Double.compare(other.popularityScore, this.popularityScore);
        if (popularityComparison != 0) {
            return popularityComparison;
        }

        // Secondary sort: average rating (descending)
        int ratingComparison = Double.compare(other.averageRating, this.averageRating);
        if (ratingComparison != 0) {
            return ratingComparison;
        }

        // Tertiary sort: price (ascending - cheaper items preferred if equal popularity/rating)
        int priceComparison = Double.compare(this.price, other.price);
        if (priceComparison != 0) {
            return priceComparison;
        }

        // Final sort: item name for consistency
        return this.name != null ? this.name.compareTo(other.name) : 0;
    }

    // Helper methods
    private void updatePopularityFromRating() {
        // Popularity combines rating and number of ratings
        if (totalRatings > 0) {
            double ratingFactor = averageRating / 5.0; // Normalize to 0-1
            double volumeFactor = Math.min(1.0, totalRatings / 100.0); // Volume boost up to 100 ratings
            this.popularityScore = (ratingFactor * 0.7) + (volumeFactor * 0.3);
        }
    }

    public void incrementPopularity(double increment) {
        this.popularityScore = Math.min(1.0, this.popularityScore + increment);
    }

    public void addTag(String tag) {
        if (tags == null) {
            tags = new ArrayList<>();
        }
        if (!tags.contains(tag)) {
            tags.add(tag);
        }
    }

    public void removeTag(String tag) {
        if (tags != null) {
            tags.remove(tag);
        }
    }

    public boolean hasTag(String tag) {
        return tags != null && tags.contains(tag);
    }

    public void addIngredient(String ingredient) {
        if (ingredientsList == null) {
            ingredientsList = new ArrayList<>();
        }
        if (!ingredientsList.contains(ingredient)) {
            ingredientsList.add(ingredient);
        }
    }

    public void removeIngredient(String ingredient) {
        if (ingredientsList != null) {
            ingredientsList.remove(ingredient);
        }
    }

    // Getters and Setters
    public String getItemId() { return itemId; }
    public void setItemId(String itemId) { this.itemId = itemId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public List<String> getIngredientsList() { 
        return ingredientsList != null ? new ArrayList<>(ingredientsList) : new ArrayList<>(); 
    }
    public void setIngredientsList(List<String> ingredientsList) { 
        this.ingredientsList = ingredientsList != null ? new ArrayList<>(ingredientsList) : new ArrayList<>();
    }

    public MenuCategory getCategory() { return category; }
    public void setCategory(MenuCategory category) { this.category = category; }

    public double getPrice() { return price; }
    public void setPrice(double price) { 
        this.price = Math.max(0.0, price); // Ensure non-negative price
    }

    public double getPopularityScore() { return popularityScore; }
    public void setPopularityScore(double popularityScore) { 
        this.popularityScore = Math.max(0.0, Math.min(1.0, popularityScore)); 
    }

    public boolean isAvailabilityStatus() { return availabilityStatus; }
    public void setAvailabilityStatus(boolean availabilityStatus) { 
        this.availabilityStatus = availabilityStatus; 
    }

    public double getAverageRating() { return averageRating; }
    public void setAverageRating(double averageRating) { 
        this.averageRating = Math.max(0.0, Math.min(5.0, averageRating)); 
    }

    public int getTotalRatings() { return totalRatings; }
    public void setTotalRatings(int totalRatings) { 
        this.totalRatings = Math.max(0, totalRatings); 
    }

    public List<String> getTags() { 
        return tags != null ? new ArrayList<>(tags) : new ArrayList<>(); 
    }
    public void setTags(List<String> tags) { 
        this.tags = tags != null ? new ArrayList<>(tags) : new ArrayList<>(); 
    }

    // Object methods
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MenuItem menuItem = (MenuItem) o;
        return Objects.equals(itemId, menuItem.itemId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(itemId);
    }

    @Override
    public String toString() {
        return getDetails();
    }
}