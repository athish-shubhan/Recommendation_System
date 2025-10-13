package restaurant.recommendation.analyzer;

import restaurant.recommendation.model.*;
import java.util.*;
import java.util.stream.Collectors;

/**
 * ContentBasedPreferenceAnalyzer - Concrete implementation
 * Analyzes user preferences based on item content and attributes
 */
public class ContentBasedPreferenceAnalyzer extends PreferenceAnalyzer {

    public ContentBasedPreferenceAnalyzer() {
        super("Content-Based Analyzer", 1.0);
    }

    @Override
    public double scoreItemsBasedOnPreferences(MenuItem menuItem, UserPreferences userPreferences) {
        double score = 0.0;

        // Dietary preference scoring
        if (userPreferences.isVegPreference() && menuItem.isVegetarian()) {
            score += 0.8;
        } else if (!userPreferences.isVegPreference() && !menuItem.isVegetarian()) {
            score += 0.6;
        }

        // Price range consideration
        if (userPreferences.isWithinPriceRange(menuItem.getPrice())) {
            score += 0.5;
        } else {
            score *= 0.3; // Penalty for out of range
        }

        // Spiciness matching
        if (menuItem.isSpicy()) {
            if (userPreferences.getSpicinessLevel() >= 3) {
                score += 0.4;
            } else if (userPreferences.getSpicinessLevel() <= 2) {
                score *= 0.5; // Penalty for spice mismatch
            }
        }

        // Category preferences
        String categoryName = menuItem.getCategory().getCategoryName();
        double categoryPref = userPreferences.getCategoryPreference(categoryName);
        score += categoryPref * 0.3;

        return Math.max(0.0, Math.min(1.0, score));
    }

    @Override
    public double calculateTasteSimilarity(UserPreferences userPreferences, MenuItem menuItem) {
        double similarity = 0.0;

        // Ingredient-based similarity
        List<String> ingredients = menuItem.getIngredientsList();
        for (String ingredient : ingredients) {
            double ingredientPref = userPreferences.getIngredientPreference(ingredient);
            similarity += ingredientPref;
        }

        if (!ingredients.isEmpty()) {
            similarity /= ingredients.size(); // Average similarity
        }

        return Math.max(0.0, Math.min(1.0, similarity));
    }

    @Override
    public List<MenuItem> getRecommendations(List<MenuItem> availableItems, 
                                           UserPreferences userPreferences, 
                                           int maxResults) {
        return availableItems.stream()
                .filter(MenuItem::isAvailable)
                .sorted((a, b) -> Double.compare(
                    scoreItemsBasedOnPreferences(b, userPreferences),
                    scoreItemsBasedOnPreferences(a, userPreferences)))
                .limit(maxResults)
                .collect(Collectors.toList());
    }

    @Override
    public void updateFromFeedback(String userId, String itemId, double rating, String feedback) {
        // Simple feedback processing for content-based approach
        if (rating >= 4.0) {
            System.out.println("Content-based: Positive feedback recorded for " + itemId);
        } else if (rating <= 2.0) {
            System.out.println("Content-based: Negative feedback recorded for " + itemId);
        }
    }

    @Override
    public String getExplanation(MenuItem menuItem, UserPreferences userPreferences) {
        StringBuilder explanation = new StringBuilder();
        explanation.append("Recommended based on your preferences: ");

        if (userPreferences.isVegPreference() && menuItem.isVegetarian()) {
            explanation.append("vegetarian choice, ");
        }

        if (userPreferences.isWithinPriceRange(menuItem.getPrice())) {
            explanation.append("within your budget, ");
        }

        String categoryName = menuItem.getCategory().getCategoryName();
        if (userPreferences.getCategoryPreference(categoryName) > 0.5) {
            explanation.append("matches your ").append(categoryName.toLowerCase()).append(" preference");
        }

        String result = explanation.toString();
        return result.endsWith(", ") ? result.substring(0, result.length() - 2) : result;
    }
}