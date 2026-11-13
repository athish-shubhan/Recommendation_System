package restaurant.recommendation.analyzer;

import restaurant.recommendation.model.*;
import java.util.*;
import java.util.stream.Collectors;


public class ContentBasedPreferenceAnalyzer extends PreferenceAnalyzer {

    public ContentBasedPreferenceAnalyzer() {
        super("Content-Based Analyzer", 1.0);
    }

    // Weights of the score components; they sum to 1 so the score stays in [0, 1].
    static final double W_PRICE = 0.30;
    static final double W_SPICE = 0.25;
    static final double W_CATEGORY = 0.20;
    static final double W_TAGS = 0.15;
    static final double W_DIET = 0.10;

    /**
     * How well an item fits a user's stated preferences, in [0, 1].
     *
     * A weighted average of five components, each itself in [0, 1]. Where the
     * user has said nothing, a component is neutral (0.5), so missing
     * information neither rewards nor punishes an item. Hard dietary rules
     * (allergens, vegetarian, vegan) are not scored here; the engine filters
     * those out before ranking.
     */
    @Override
    public double scoreItemsBasedOnPreferences(MenuItem menuItem, UserPreferences userPreferences) {
        double price = userPreferences.isWithinPriceRange(menuItem.getPrice()) ? 1.0 : 0.0;

        // Spiciness level runs 1 (mild) to 5 (very hot). A spicy dish suits
        // the user in proportion to the level; a mild dish suits everyone, a
        // little less so for people who like heat.
        double heat = (clamp(userPreferences.getSpicinessLevel(), 1, 5) - 1) / 4.0;
        double spice = menuItem.isSpicy() ? heat : 1.0 - heat / 2.0;

        // Category preference is stored in [-1, 1]; 0 means "no opinion".
        String categoryName = menuItem.getCategory() != null ? menuItem.getCategory().getCategoryName() : null;
        double category = (userPreferences.getCategoryPreference(categoryName) + 1.0) / 2.0;

        double tags = 0.5;
        if (!userPreferences.getPreferredTags().isEmpty()) {
            List<String> itemTags = menuItem.getTags();
            tags = itemTags != null && itemTags.stream().anyMatch(userPreferences::hasPreferredTag) ? 1.0 : 0.0;
        }

        // Eating meat is not a preference against vegetarian food, so only a
        // vegetarian's match earns the bonus.
        double diet = userPreferences.isVegPreference() && menuItem.isVegetarian() ? 1.0 : 0.5;

        double score = W_PRICE * price + W_SPICE * spice + W_CATEGORY * category + W_TAGS * tags + W_DIET * diet;
        return clamp(score, 0.0, 1.0);
    }

    private static double clamp(double v, double lo, double hi) {
        return Math.max(lo, Math.min(hi, v));
    }

    @Override
    public double calculateTasteSimilarity(UserPreferences userPreferences, MenuItem menuItem) {
        double similarity = 0.0;

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
            explanation.append("matches your ").append(categoryName.toLowerCase()).append(" preference, ");
        }
        List<String> itemTags = menuItem.getTags();
        if (itemTags != null) {
            for (String tag : itemTags) {
                if (userPreferences.hasPreferredTag(tag)) {
                    explanation.append("tagged ").append(tag).append(", ");
                }
            }
        }

        String result = explanation.toString();
        return result.endsWith(", ") ? result.substring(0, result.length() - 2) : result;
    }
}