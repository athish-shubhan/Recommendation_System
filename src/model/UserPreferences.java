package restaurant.recommendation.model;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;


public class UserPreferences {
    private int spicinessLevel;
    private boolean vegPreference;
    private List<String> allergyList;
    private List<String> favouriteCuisines;

    private String userId;
    private boolean veganPreference;
    private Map<String, Double> ingredientPreferences;
    private Map<String, Double> categoryPreferences;
    private double priceRangeLower;
    private double priceRangeUpper;
    private Set<String> dislikedIngredients;
    private Set<String> preferredTags;

    public UserPreferences() {
        this.allergyList = new ArrayList<>();
        this.favouriteCuisines = new ArrayList<>();
        this.ingredientPreferences = new HashMap<>();
        this.categoryPreferences = new HashMap<>();
        this.dislikedIngredients = new HashSet<>();
        this.preferredTags = new HashSet<>();
        this.spicinessLevel = 2; // Default mild
        this.priceRangeLower = 0.0;
        this.priceRangeUpper = Double.MAX_VALUE;
    }

    public UserPreferences(String userId) {
        this();
        this.userId = userId;
    }

    public void updateIngredientPreference(String ingredient, double preferenceScore) {
        if (ingredient != null) {
            preferenceScore = Math.max(-1.0, Math.min(1.0, preferenceScore)); // Clamp to [-1, 1]
            ingredientPreferences.put(ingredient.toLowerCase(), preferenceScore);
        }
    }

    public void updateCategoryPreference(String category, double preferenceScore) {
        if (category != null) {
            preferenceScore = Math.max(-1.0, Math.min(1.0, preferenceScore)); // Clamp to [-1, 1]
            categoryPreferences.put(category.toLowerCase(), preferenceScore);
        }
    }

    public double getIngredientPreference(String ingredient) {
        if (ingredient == null) return 0.0;
        return ingredientPreferences.getOrDefault(ingredient.toLowerCase(), 0.0);
    }

    public double getCategoryPreference(String category) {
        if (category == null) return 0.0;
        return categoryPreferences.getOrDefault(category.toLowerCase(), 0.0);
    }

    public boolean hasAllergen(String allergen) {
        if (allergen == null) return false;
        return allergyList.stream()
                .anyMatch(allergy -> allergy.toLowerCase().contains(allergen.toLowerCase()));
    }

    public boolean isWithinPriceRange(double price) {
        return price >= priceRangeLower && price <= priceRangeUpper;
    }

    public void setPriceRange(double lower, double upper) {
        if (lower >= 0 && upper >= lower) {
            this.priceRangeLower = lower;
            this.priceRangeUpper = upper;
        }
    }

    public void addAllergen(String allergen) {
        if (allergen != null && !allergen.trim().isEmpty()) {
            allergyList.add(allergen.trim().toLowerCase());
        }
    }

    public void removeAllergen(String allergen) {
        if (allergen != null) {
            allergyList.remove(allergen.trim().toLowerCase());
        }
    }

    public void clearAllergies() {
        allergyList.clear();
    }

    public void addFavouriteCuisine(String cuisine) {
        if (cuisine != null && !cuisine.trim().isEmpty()) {
            String cleanCuisine = cuisine.trim();
            if (!favouriteCuisines.contains(cleanCuisine)) {
                favouriteCuisines.add(cleanCuisine);
            }
        }
    }

    public void removeFavouriteCuisine(String cuisine) {
        if (cuisine != null) {
            favouriteCuisines.remove(cuisine.trim());
        }
    }

    public boolean isFavouriteCuisine(String cuisine) {
        if (cuisine == null) return false;
        return favouriteCuisines.contains(cuisine.trim());
    }

    // Disliked ingredients management
    public void addDislikedIngredient(String ingredient) {
        if (ingredient != null && !ingredient.trim().isEmpty()) {
            dislikedIngredients.add(ingredient.trim().toLowerCase());
        }
    }

    public void removeDislikedIngredient(String ingredient) {
        if (ingredient != null) {
            dislikedIngredients.remove(ingredient.trim().toLowerCase());
        }
    }

    public boolean isDislikedIngredient(String ingredient) {
        if (ingredient == null) return false;
        return dislikedIngredients.contains(ingredient.toLowerCase());
    }

    public void addPreferredTag(String tag) {
        if (tag != null && !tag.trim().isEmpty()) {
            preferredTags.add(tag.trim().toLowerCase());
        }
    }

    public void removePreferredTag(String tag) {
        if (tag != null) {
            preferredTags.remove(tag.trim().toLowerCase());
        }
    }

    public boolean hasPreferredTag(String tag) {
        if (tag == null) return false;
        return preferredTags.contains(tag.toLowerCase());
    }

    public double calculateItemCompatibility(MenuItem item) {
        if (item == null) return 0.0;

        double score = 0.0;

        // Dietary compatibility
        if (vegPreference && !item.isVegetarian()) {
            return -1.0; // Strong incompatibility
        }
        if (veganPreference && !item.isVegan()) {
            return -1.0; // Strong incompatibility
        }

        // Allergy check
        for (String allergen : allergyList) {
            if (item.hasAllergen(allergen)) {
                return -1.0; // Safety first
            }
        }

        // Price compatibility
        if (!isWithinPriceRange(item.getPrice())) {
            score -= 0.3;
        }

        // Spiciness compatibility
        if (item.isSpicy()) {
            if (spicinessLevel >= 3) {
                score += 0.2;
            } else {
                score -= 0.3;
            }
        }

        // Ingredient preferences
        for (String ingredient : item.getIngredientsList()) {
            if (isDislikedIngredient(ingredient)) {
                score -= 0.4;
            } else {
                score += getIngredientPreference(ingredient) * 0.1;
            }
        }

        // Category preferences
        if (item.getCategory() != null) {
            score += getCategoryPreference(item.getCategory().getCategoryName()) * 0.2;
        }

        // Tag preferences
        for (String tag : item.getTags()) {
            if (hasPreferredTag(tag)) {
                score += 0.15;
            }
        }

        return Math.max(-1.0, Math.min(1.0, score));
    }

    // Profile analysis methods
    public Map<String, Object> getPreferenceSummary() {
        Map<String, Object> summary = new HashMap<>();

        summary.put("userId", userId);
        summary.put("dietary", vegPreference ? (veganPreference ? "Vegan" : "Vegetarian") : "Non-Vegetarian");
        summary.put("spicinessLevel", spicinessLevel);
        summary.put("allergyCount", allergyList.size());
        summary.put("favouriteCuisines", new ArrayList<>(favouriteCuisines));
        summary.put("priceRange", String.format("%.0f - %.0f", priceRangeLower, priceRangeUpper));
        summary.put("ingredientPreferencesCount", ingredientPreferences.size());
        summary.put("categoryPreferencesCount", categoryPreferences.size());

        return summary;
    }

    public boolean isHealthConscious() {
        return hasPreferredTag("healthy") || hasPreferredTag("organic") || 
               hasPreferredTag("low-fat") || vegPreference || veganPreference;
    }

    public boolean isBudgetConscious() {
        return priceRangeUpper <= 100.0; // Assuming 100 is a budget threshold
    }

    // Getters and Setters
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public int getSpicinessLevel() { return spicinessLevel; }
    public void setSpicinessLevel(int spicinessLevel) { 
        this.spicinessLevel = Math.max(1, Math.min(5, spicinessLevel)); 
    }

    public boolean isVegPreference() { return vegPreference; }
    public void setVegPreference(boolean vegPreference) { 
        this.vegPreference = vegPreference;
        if (!vegPreference) {
            this.veganPreference = false; // Can't be vegan if not vegetarian
        }
    }

    public boolean isVeganPreference() { return veganPreference; }
    public void setVeganPreference(boolean veganPreference) { 
        this.veganPreference = veganPreference;
        if (veganPreference) {
            this.vegPreference = true; // Vegan implies vegetarian
        }
    }

    public List<String> getAllergyList() { 
        return new ArrayList<>(allergyList); 
    }
    public void setAllergyList(List<String> allergyList) { 
        this.allergyList = allergyList != null ? new ArrayList<>(allergyList) : new ArrayList<>();
    }

    public List<String> getFavouriteCuisines() { 
        return new ArrayList<>(favouriteCuisines); 
    }
    public void setFavouriteCuisines(List<String> favouriteCuisines) { 
        this.favouriteCuisines = favouriteCuisines != null ? new ArrayList<>(favouriteCuisines) : new ArrayList<>();
    }

    public Map<String, Double> getIngredientPreferences() { 
        return new HashMap<>(ingredientPreferences); 
    }

    public Map<String, Double> getCategoryPreferences() { 
        return new HashMap<>(categoryPreferences); 
    }

    public double getPriceRangeLower() { return priceRangeLower; }
    public double getPriceRangeUpper() { return priceRangeUpper; }

    public Set<String> getDislikedIngredients() { 
        return new HashSet<>(dislikedIngredients); 
    }

    public Set<String> getPreferredTags() { 
        return new HashSet<>(preferredTags); 
    }

    @Override
    public String toString() {
        return String.format("UserPreferences{userId='%s', veg=%b, spice=%d, allergies=%d, cuisines=%d}", 
                           userId, vegPreference, spicinessLevel, allergyList.size(), favouriteCuisines.size());
    }
}