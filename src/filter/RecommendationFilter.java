package restaurant.recommendation.filter;

import restaurant.recommendation.model.ContextualFactor;
import restaurant.recommendation.model.FilterCriteria;
import restaurant.recommendation.model.MenuItem;
import restaurant.recommendation.model.UserPreferences;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


public class RecommendationFilter {

    // Active FilterCriteria to be applied as a pipeline (UML composition-style use)
    private List<FilterCriteria> activeFilters;

    // AND (strict) vs OR (lenient) evaluation for the activeFilters pipeline
    private boolean strictMode;

    public RecommendationFilter() {
        this.activeFilters = new ArrayList<>();
        this.strictMode = true; // default to strict (AND) composition
    }

    // ========== UML methods ==========

    // Inventory status filter: "available" | "unavailable" | anything -> pass-through
    public List<MenuItem> filterByInventory(List<MenuItem> menuList, String inventoryStatus) {
        if (menuList == null || menuList.isEmpty()) return new ArrayList<>();
        if (inventoryStatus == null || inventoryStatus.isBlank()) return new ArrayList<>(menuList);

        String s = inventoryStatus.trim().toLowerCase();
        return menuList.stream()
                .filter(item -> {
                    if ("available".equals(s)) return item.isAvailable();
                    if ("unavailable".equals(s)) return !item.isAvailable();
                    return true; // unknown status -> do not filter
                })
                .collect(Collectors.toList());
    }

    // Dietary restrictions filter using allergy list
    public List<MenuItem> filterByDietaryRestrictions(List<MenuItem> menuList, List<String> allergyList) {
        if (menuList == null || menuList.isEmpty()) return new ArrayList<>();
        if (allergyList == null || allergyList.isEmpty()) return new ArrayList<>(menuList);

        return menuList.stream()
                .filter(item -> {
                    for (String allergen : allergyList) {
                        if (allergen != null && item.hasAllergen(allergen)) {
                            return false;
                        }
                    }
                    return true;
                })
                .collect(Collectors.toList());
    }

    // Contextual filter using ContextualFactor suitability predicate
    public List<MenuItem> filterByContext(List<MenuItem> menuList, ContextualFactor contextualFactor) {
        if (menuList == null || menuList.isEmpty()) return new ArrayList<>();
        if (contextualFactor == null) return new ArrayList<>(menuList);

        return menuList.stream()
                .filter(contextualFactor::isAppropriateForContext)
                .collect(Collectors.toList());
    }

    // ========== Helpful filters referenced by the engine and criteria ==========

    public List<MenuItem> filterByPriceRange(List<MenuItem> menuList, double minPrice, double maxPrice) {
        if (menuList == null || menuList.isEmpty()) return new ArrayList<>();
        double lo = Math.min(minPrice, maxPrice);
        double hi = Math.max(minPrice, maxPrice);

        return menuList.stream()
                .filter(i -> {
                    double p = i.getPrice();
                    return p >= lo && p <= hi;
                })
                .collect(Collectors.toList());
    }

    public List<MenuItem> filterByCategory(List<MenuItem> menuList, String categoryName) {
        if (menuList == null || menuList.isEmpty()) return new ArrayList<>();
        if (categoryName == null || categoryName.isBlank()) return new ArrayList<>(menuList);

        String c = categoryName.trim().toLowerCase();
        return menuList.stream()
                .filter(i -> i.getCategory() != null &&
                        i.getCategory().getCategoryName() != null &&
                        i.getCategory().getCategoryName().toLowerCase().contains(c))
                .collect(Collectors.toList());
    }

    public List<MenuItem> filterByRating(List<MenuItem> menuList, double minRating) {
        if (menuList == null || menuList.isEmpty()) return new ArrayList<>();

        return menuList.stream()
                .filter(i -> i.getAverageRating() >= minRating)
                .collect(Collectors.toList());
    }

    public List<MenuItem> filterByTags(List<MenuItem> menuList, List<String> requiredTags) {
        if (menuList == null || menuList.isEmpty()) return new ArrayList<>();
        if (requiredTags == null || requiredTags.isEmpty()) return new ArrayList<>(menuList);

        return menuList.stream()
                .filter(i -> i.getTags() != null && i.getTags().containsAll(requiredTags))
                .collect(Collectors.toList());
    }

    public List<MenuItem> filterVegetarianOnly(List<MenuItem> menuList) {
        if (menuList == null || menuList.isEmpty()) return new ArrayList<>();
        return menuList.stream().filter(MenuItem::isVegetarian).collect(Collectors.toList());
    }

    public List<MenuItem> filterVeganOnly(List<MenuItem> menuList) {
        if (menuList == null || menuList.isEmpty()) return new ArrayList<>();
        return menuList.stream().filter(MenuItem::isVegan).collect(Collectors.toList());
    }

    public List<MenuItem> filterSpicyOnly(List<MenuItem> menuList) {
        if (menuList == null || menuList.isEmpty()) return new ArrayList<>();
        return menuList.stream().filter(MenuItem::isSpicy).collect(Collectors.toList());
    }

    public List<MenuItem> filterMildOnly(List<MenuItem> menuList) {
        if (menuList == null || menuList.isEmpty()) return new ArrayList<>();
        return menuList.stream().filter(i -> !i.isSpicy()).collect(Collectors.toList());
    }

    // ========== FilterCriteria pipeline (UML: appliesTo with AND/OR composition) ==========

    public void addFilter(FilterCriteria criteria) {
        if (criteria != null && !activeFilters.contains(criteria)) {
            activeFilters.add(criteria);
        }
    }

    public void removeFilter(FilterCriteria criteria) {
        activeFilters.remove(criteria);
    }

    public void clearFilters() {
        activeFilters.clear();
    }

    public List<MenuItem> applyFilter(List<MenuItem> menuList, FilterCriteria criteria) {
        if (menuList == null || menuList.isEmpty() || criteria == null) {
            return menuList != null ? new ArrayList<>(menuList) : new ArrayList<>();
        }
        return criteria.filterItems(menuList);
    }

    public List<MenuItem> applyAllFilters(List<MenuItem> menuList) {
        if (menuList == null || menuList.isEmpty()) return new ArrayList<>();
        if (activeFilters.isEmpty()) return new ArrayList<>(menuList);

        return menuList.stream()
                .filter(item -> {
                    if (strictMode) {
                        // AND logic
                        return activeFilters.stream()
                                .filter(FilterCriteria::isActive)
                                .allMatch(f -> f.appliesTo(item));
                    } else {
                        // OR logic
                        return activeFilters.stream()
                                .filter(FilterCriteria::isActive)
                                .anyMatch(f -> f.appliesTo(item));
                    }
                })
                .collect(Collectors.toList());
    }

    /**
     * Full pipeline used by the engine:
     * 1) Inventory (available)
     * 2) Dietary (allergies, veg/vegan, price)
     * 3) Contextual
     * 4) Custom FilterCriteria (AND/OR based on strictMode)
     */
    public List<MenuItem> applyFilteringPipeline(
            List<MenuItem> menuList,
            UserPreferences userPreferences,
            ContextualFactor context
    ) {
        if (menuList == null || menuList.isEmpty()) return new ArrayList<>();

        List<MenuItem> filtered = new ArrayList<>(menuList);

        // 1) Availability first
        filtered = filterByInventory(filtered, "available");

        // 2) Dietary: allergies + veg/vegan + price range
        if (userPreferences != null) {
            filtered = filterByDietaryRestrictions(filtered, userPreferences.getAllergyList());
            if (userPreferences.isVeganPreference()) {
                filtered = filterVeganOnly(filtered);
            } else if (userPreferences.isVegPreference()) {
                filtered = filterVegetarianOnly(filtered);
            }
            filtered = filterByPriceRange(filtered,
                    userPreferences.getPriceRangeLower(),
                    userPreferences.getPriceRangeUpper());
        }

        // 3) Context awareness
        if (context != null) {
            filtered = filterByContext(filtered, context);
        }

        // 4) Custom criteria pipeline
        filtered = applyAllFilters(filtered);

        return filtered;
    }

    // ========== Analytics helpers ==========

    public Map<String, Integer> getFilteringStats(List<MenuItem> originalList, List<MenuItem> filteredList) {
        Map<String, Integer> stats = new HashMap<>();
        int original = originalList != null ? originalList.size() : 0;
        int filtered = filteredList != null ? filteredList.size() : 0;

        stats.put("originalCount", original);
        stats.put("filteredCount", filtered);
        stats.put("removedCount", Math.max(0, original - filtered));
        stats.put("retentionPercentage", original == 0 ? 0 : (int) Math.round((filtered * 100.0) / original));
        return stats;
    }

    public int countActiveFilters() {
        return (int) activeFilters.stream().filter(FilterCriteria::isActive).count();
    }

    public List<String> getActiveFilterNames() {
        return activeFilters.stream()
                .filter(FilterCriteria::isActive)
                .map(FilterCriteria::getName)
                .collect(Collectors.toList());
    }

    public boolean hasActiveFilters() {
        return activeFilters.stream().anyMatch(FilterCriteria::isActive);
    }

    // ========== Getters / Setters / toString ==========

    public List<FilterCriteria> getActiveFilters() {
        return new ArrayList<>(activeFilters);
    }

    public void setActiveFilters(List<FilterCriteria> activeFilters) {
        this.activeFilters = activeFilters != null ? new ArrayList<>(activeFilters) : new ArrayList<>();
    }

    public boolean isStrictMode() {
        return strictMode;
    }

    public void setStrictMode(boolean strictMode) {
        this.strictMode = strictMode;
    }

    @Override
    public String toString() {
        return String.format("RecommendationFilter{activeFilters=%d, strictMode=%b}", countActiveFilters(), strictMode);
    }
}
