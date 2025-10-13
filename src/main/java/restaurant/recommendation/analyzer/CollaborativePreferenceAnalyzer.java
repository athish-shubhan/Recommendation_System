package restaurant.recommendation.analyzer;

import restaurant.recommendation.model.*;
import java.util.*;
import java.util.stream.Collectors;

/**
 * CollaborativePreferenceAnalyzer - Concrete implementation
 * Analyzes preferences based on similar users' behaviors
 */
public class CollaborativePreferenceAnalyzer extends PreferenceAnalyzer {
    private Map<String, List<String>> userSimilarities; // userId -> similar user IDs
    private Map<String, Double> itemPopularity; // itemId -> popularity score

    public CollaborativePreferenceAnalyzer() {
        super("Collaborative Filtering Analyzer", 0.9);
        this.userSimilarities = new HashMap<>();
        this.itemPopularity = new HashMap<>();
    }

    @Override
    public double scoreItemsBasedOnPreferences(MenuItem menuItem, UserPreferences userPreferences) {
        double score = 0.0;
        String userId = userPreferences.getUserId();

        // Get popularity-based score
        double popularity = itemPopularity.getOrDefault(menuItem.getItemId(), 0.5);
        score += popularity * 0.4;

        // Get collaborative score from similar users
        List<String> similarUsers = userSimilarities.getOrDefault(userId, new ArrayList<>());
        if (!similarUsers.isEmpty()) {
            double collaborativeScore = 0.0;
            for (String similarUserId : similarUsers) {
                // Simulate similarity scoring (in real implementation, would use actual user data)
                collaborativeScore += 0.7; // Placeholder
            }
            collaborativeScore /= similarUsers.size();
            score += collaborativeScore * 0.6;
        } else {
            // Fallback to item rating if no similar users
            score += menuItem.getAverageRating() / 5.0 * 0.6;
        }

        return Math.max(0.0, Math.min(1.0, score));
    }

    @Override
    public double calculateTasteSimilarity(UserPreferences userPreferences, MenuItem menuItem) {
        // Collaborative filtering calculates user-user similarity
        String userId = userPreferences.getUserId();
        List<String> similarUsers = userSimilarities.getOrDefault(userId, new ArrayList<>());

        if (similarUsers.isEmpty()) {
            return 0.5; // Neutral similarity
        }

        // Average similarity with users who liked this item
        return 0.75; // Placeholder - would use actual collaborative filtering
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
        // Update item popularity
        double currentPopularity = itemPopularity.getOrDefault(itemId, 0.5);
        double newPopularity = (currentPopularity + (rating / 5.0)) / 2.0;
        itemPopularity.put(itemId, newPopularity);

        // Update user similarities (simplified)
        if (rating >= 4.0) {
            userSimilarities.computeIfAbsent(userId, k -> new ArrayList<>());
            System.out.println("Collaborative: Updated user similarity for " + userId);
        }
    }

    @Override
    public String getExplanation(MenuItem menuItem, UserPreferences userPreferences) {
        String userId = userPreferences.getUserId();
        List<String> similarUsers = userSimilarities.getOrDefault(userId, new ArrayList<>());

        if (!similarUsers.isEmpty()) {
            return "Users with similar tastes also enjoyed this item. Highly rated by customers with preferences like yours.";
        } else {
            return "Popular choice among customers. Rating: " + String.format("%.1f", menuItem.getAverageRating()) + "/5.0";
        }
    }

    // Utility methods for collaborative filtering
    public void addUserSimilarity(String userId, String similarUserId) {
        userSimilarities.computeIfAbsent(userId, k -> new ArrayList<>()).add(similarUserId);
    }

    public void setItemPopularity(String itemId, double popularity) {
        itemPopularity.put(itemId, popularity);
    }
}