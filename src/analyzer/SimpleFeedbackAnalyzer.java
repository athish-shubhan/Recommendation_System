package restaurant.recommendation.analyzer;

import restaurant.recommendation.model.*;
import java.util.*;


public class SimpleFeedbackAnalyzer extends FeedbackAnalyzer {
    private Map<String, List<Double>> userRatings; // userId -> ratings history

    public SimpleFeedbackAnalyzer() {
        super("Simple Feedback Analyzer", 0.7);
        this.userRatings = new HashMap<>();
    }

    @Override
    public void processCustomerReviews(String itemId, String userId, double rating, String comment) {
        userRatings.computeIfAbsent(userId, k -> new ArrayList<>()).add(rating);

        System.out.println(String.format("Simple feedback: User %s rated item %s as %.1f", 
                                        userId, itemId, rating));

        if (rating >= 4.0) {
            System.out.println("  Positive feedback recorded");
        } else if (rating <= 2.0) {
            System.out.println("  Negative feedback recorded");  
        }
    }

    @Override
    public void updatePreferencesBasedOnFeedback(String userId, Map<String, Object> feedback) {
        Double rating = (Double) feedback.get("rating");
        String itemId = (String) feedback.get("item_id");

        if (rating != null && itemId != null) {
            // Simple preference update
            if (rating >= 4.0) {
                System.out.println("Updating positive preferences for user " + userId);
            } else if (rating <= 2.0) {
                System.out.println("Updating negative preferences for user " + userId);
            }
        }
    }

    @Override
    public double analyzeSentiment(String feedbackText) {
        if (feedbackText == null || feedbackText.trim().isEmpty()) {
            return 0.5; // Neutral
        }

        String text = feedbackText.toLowerCase();
        double sentiment = 0.5; // Start neutral

        if (text.contains("good") || text.contains("great") || text.contains("excellent") ||
            text.contains("amazing") || text.contains("delicious") || text.contains("love")) {
            sentiment += 0.3;
        }

        if (text.contains("bad") || text.contains("terrible") || text.contains("awful") ||
            text.contains("hate") || text.contains("disgusting") || text.contains("worst")) {
            sentiment -= 0.3;
        }

        return Math.max(0.0, Math.min(1.0, sentiment));
    }

    public double getAverageRating(String userId) {
        List<Double> ratings = userRatings.get(userId);
        if (ratings == null || ratings.isEmpty()) {
            return 3.0; // Default neutral rating
        }

        return ratings.stream().mapToDouble(Double::doubleValue).average().orElse(3.0);
    }
}
