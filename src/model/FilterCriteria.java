package restaurant.recommendation.model;

import java.util.function.Predicate;


public class FilterCriteria {
    private String name;
    private String rule;

    private Predicate<MenuItem> filterPredicate;
    private double priority;
    private boolean isActive;
    private String description;

    // Constructors
    public FilterCriteria() {
        this.priority = 1.0;
        this.isActive = true;
    }

    public FilterCriteria(String name, String rule) {
        this();
        this.name = name;
        this.rule = rule;
        this.filterPredicate = createPredicateFromRule(rule);
    }

    public FilterCriteria(String name, String rule, String description) {
        this(name, rule);
        this.description = description;
    }

    public FilterCriteria(String name, Predicate<MenuItem> filterPredicate) {
        this();
        this.name = name;
        this.filterPredicate = filterPredicate;
        this.rule = "Custom predicate";
    }

    // UML specified method
    public boolean appliesTo(MenuItem menuItem) {
        if (!isActive || menuItem == null) return false;

        try {
            return filterPredicate != null ? filterPredicate.test(menuItem) : true;
        } catch (Exception e) {
            System.err.println("Error applying filter '" + name + "': " + e.getMessage());
            return false; // Safe fallback
        }
    }

    // Rule-based predicate creation
    private Predicate<MenuItem> createPredicateFromRule(String rule) {
        if (rule == null || rule.trim().isEmpty()) {
            return item -> true; // No filter
        }

        String lowerRule = rule.toLowerCase().trim();

        // Dietary filters
        if (lowerRule.equals("vegetarian")) {
            return MenuItem::isVegetarian;
        } else if (lowerRule.equals("vegan")) {
            return MenuItem::isVegan;
        } else if (lowerRule.equals("non-vegetarian")) {
            return item -> !item.isVegetarian();
        }

        // Spiciness filters
        else if (lowerRule.equals("spicy")) {
            return MenuItem::isSpicy;
        } else if (lowerRule.equals("mild")) {
            return item -> !item.isSpicy();
        }

        // Temperature filters
        else if (lowerRule.equals("hot")) {
            return MenuItem::isHot;
        } else if (lowerRule.equals("cold")) {
            return MenuItem::isCold;
        }

        // Health filters
        else if (lowerRule.equals("healthy")) {
            return MenuItem::isHealthy;
        }

        // Availability filters
        else if (lowerRule.equals("available")) {
            return MenuItem::isAvailable;
        } else if (lowerRule.equals("unavailable")) {
            return item -> !item.isAvailable();
        }

        // Price range filters
        else if (lowerRule.startsWith("price_under_")) {
            try {
                double maxPrice = Double.parseDouble(lowerRule.substring("price_under_".length()));
                return item -> item.getPrice() <= maxPrice;
            } catch (NumberFormatException e) {
                return item -> true;
            }
        } else if (lowerRule.startsWith("price_over_")) {
            try {
                double minPrice = Double.parseDouble(lowerRule.substring("price_over_".length()));
                return item -> item.getPrice() >= minPrice;
            } catch (NumberFormatException e) {
                return item -> true;
            }
        }

        // Rating filters
        else if (lowerRule.startsWith("rating_above_")) {
            try {
                double minRating = Double.parseDouble(lowerRule.substring("rating_above_".length()));
                return item -> item.getAverageRating() >= minRating;
            } catch (NumberFormatException e) {
                return item -> true;
            }
        }

        // Category filters
        else if (lowerRule.startsWith("category_")) {
            String categoryName = lowerRule.substring("category_".length());
            return item -> item.getCategory() != null && 
                          item.getCategory().getCategoryName().toLowerCase().contains(categoryName);
        }

        // Tag filters
        else if (lowerRule.startsWith("tag_")) {
            String tagName = lowerRule.substring("tag_".length());
            return item -> item.hasTag(tagName);
        }

        // Ingredient filters
        else if (lowerRule.startsWith("contains_")) {
            String ingredient = lowerRule.substring("contains_".length());
            return item -> item.getIngredientsList().stream()
                               .anyMatch(ing -> ing.toLowerCase().contains(ingredient));
        } else if (lowerRule.startsWith("excludes_")) {
            String ingredient = lowerRule.substring("excludes_".length());
            return item -> item.getIngredientsList().stream()
                               .noneMatch(ing -> ing.toLowerCase().contains(ingredient));
        }

        // Default: no filter
        else {
            return item -> true;
        }
    }

    // Filter management methods
    public void activate() {
        this.isActive = true;
    }

    public void deactivate() {
        this.isActive = false;
    }

    public void updateRule(String newRule) {
        this.rule = newRule;
        this.filterPredicate = createPredicateFromRule(newRule);
    }

    public void setCustomPredicate(Predicate<MenuItem> predicate) {
        this.filterPredicate = predicate;
        this.rule = "Custom predicate";
    }

    public long countMatchingItems(java.util.List<MenuItem> items) {
        if (items == null || !isActive) return 0;

        return items.stream()
                .filter(this::appliesTo)
                .count();
    }

    public java.util.List<MenuItem> filterItems(java.util.List<MenuItem> items) {
        if (items == null || !isActive) {
            return items != null ? new java.util.ArrayList<>(items) : new java.util.ArrayList<>();
        }

        return items.stream()
                .filter(this::appliesTo)
                .collect(java.util.stream.Collectors.toList());
    }

    // Predicate combination methods
    public FilterCriteria and(FilterCriteria other) {
        if (other == null) return this;

        String combinedName = this.name + " AND " + other.name;
        Predicate<MenuItem> combinedPredicate = this.filterPredicate.and(other.filterPredicate);

        return new FilterCriteria(combinedName, combinedPredicate);
    }

    public FilterCriteria or(FilterCriteria other) {
        if (other == null) return this;

        String combinedName = this.name + " OR " + other.name;
        Predicate<MenuItem> combinedPredicate = this.filterPredicate.or(other.filterPredicate);

        return new FilterCriteria(combinedName, combinedPredicate);
    }

    public FilterCriteria negate() {
        String negatedName = "NOT " + this.name;
        Predicate<MenuItem> negatedPredicate = this.filterPredicate.negate();

        return new FilterCriteria(negatedName, negatedPredicate);
    }

    // Common filter factories
    public static FilterCriteria vegetarianOnly() {
        return new FilterCriteria("Vegetarian Only", "vegetarian", 
                                "Shows only vegetarian items");
    }

    public static FilterCriteria priceRange(double minPrice, double maxPrice) {
        String name = String.format("Price Range %.0f-%.0f", minPrice, maxPrice);
        Predicate<MenuItem> predicate = item -> {
            double price = item.getPrice();
            return price >= minPrice && price <= maxPrice;
        };
        return new FilterCriteria(name, predicate);
    }

    public static FilterCriteria highRated(double minRating) {
        String name = String.format("Rating %.1f+", minRating);
        return new FilterCriteria(name, "rating_above_" + minRating, 
                                "Shows items with rating " + minRating + " or higher");
    }

    public static FilterCriteria availableOnly() {
        return new FilterCriteria("Available Only", "available", 
                                "Shows only available items");
    }

    public static FilterCriteria categoryFilter(String categoryName) {
        return new FilterCriteria("Category: " + categoryName, 
                                "category_" + categoryName.toLowerCase(),
                                "Shows items from " + categoryName + " category");
    }

    public static FilterCriteria allergenFree(String allergen) {
        String name = "No " + allergen;
        return new FilterCriteria(name, "excludes_" + allergen.toLowerCase(),
                                "Excludes items containing " + allergen);
    }

    // Getters and Setters
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getRule() { return rule; }
    public void setRule(String rule) { 
        this.rule = rule;
        this.filterPredicate = createPredicateFromRule(rule);
    }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public double getPriority() { return priority; }
    public void setPriority(double priority) { this.priority = priority; }

    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }

    @Override
    public String toString() {
        return String.format("FilterCriteria{name='%s', rule='%s', active=%b, priority=%.1f}", 
                           name, rule, isActive, priority);
    }
}