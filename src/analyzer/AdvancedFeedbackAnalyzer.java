package restaurant.recommendation.analyzer;

import restaurant.recommendation.model.*;
import java.util.*;
import java.util.regex.Pattern;

public class AdvancedFeedbackAnalyzer extends FeedbackAnalyzer {
    private Map<String, Map<String, Double>> userItemPreferences; // userId -> itemId -> preference
    private Map<String, Integer> feedbackCounts; // userId -> feedback count
    private Pattern positivePattern;
    private Pattern negativePattern;

    public AdvancedFeedbackAnalyzer() {
        super("Advanced Feedback Analyzer", 1.0);
        this.userItemPreferences = new HashMap<>();
        this.feedbackCounts = new HashMap<>();

        // Advanced regex patterns for sentiment analysis
        this.positivePattern = Pattern.compile(
            "\\b(excellent|amazing|outstanding|fantastic|wonderful|delicious|perfect|great|good|love|enjoy|tasty|fresh|quality)\\b",
            Pattern.CASE_INSENSITIVE
        );
        this.negativePattern = Pattern.compile(
            "\\b(terrible|awful|horrible|disgusting|bad|worst|hate|nasty|stale|bland|overpriced|disappointing)\\b",
            Pattern.CASE_INSENSITIVE
        );
    }

    @Override
    public void processCustomerReviews(String itemId, String userId, double rating, String comment) {
        double sentimentScore = analyzeSentiment(comment);

        userItemPreferences.computeIfAbsent(userId, k -> new HashMap<>())
                           .put(itemId, (rating / 5.0 + sentimentScore) / 2.0);

        feedbackCounts.put(userId, feedbackCounts.getOrDefault(userId, 0) + 1);

        System.out.println(String.format("Advanced feedback: User %s, Item %s, Rating %.1f, Sentiment %.2f", 
                                        userId, itemId, rating, sentimentScore));

        if (rating >= 4.5 && sentimentScore >= 0.7) {
            System.out.println("  Highly positive experience detected");
        } else if (rating <= 2.0 && sentimentScore <= 0.3) {
            System.out.println("  Negative experience requiring attention");
        } else if (Math.abs(rating/5.0 - sentimentScore) > 0.3) {
            System.out.println("  Inconsistency between rating and comment detected");
        }
    }

    @Override
    public void updatePreferencesBasedOnFeedback(String userId, Map<String, Object> feedback) {
        String itemId = (String) feedback.get("item_id");
        Double rating = (Double) feedback.get("rating");
        String comment = (String) feedback.get("comment");

        if (itemId != null && rating != null) {
            double sentimentScore = comment != null ? analyzeSentiment(comment) : rating / 5.0;
            double preferenceScore = (rating / 5.0 + sentimentScore) / 2.0;

            Map<String, Double> userPrefs = userItemPreferences.computeIfAbsent(userId, k -> new HashMap<>());
            double currentPref = userPrefs.getOrDefault(itemId, 0.5);
            double newPref = (currentPref * 0.7) + (preferenceScore * 0.3); // Weighted update
            userPrefs.put(itemId, newPref);

            System.out.println(String.format("Advanced preference update: %s -> %s = %.3f", 
                                            userId, itemId, newPref));
        }
    }

    @Override
    public double analyzeSentiment(String feedbackText) {
        if (feedbackText == null || feedbackText.trim().isEmpty()) {
            return 0.5; // Neutral
        }

        String text = feedbackText.toLowerCase();

        long positiveMatches = positivePattern.matcher(text).results().count();
        long negativeMatches = negativePattern.matcher(text).results().count();

        double sentiment = 0.5; // Start neutral

        if (positiveMatches > 0 || negativeMatches > 0) {
            double positiveWeight = positiveMatches * 0.15;
            double negativeWeight = negativeMatches * 0.15;
            sentiment = 0.5 + positiveWeight - negativeWeight;
        }

        if (text.contains("but ") || text.contains("however ")) {
            sentiment *= 0.8; // Reduce confidence for mixed sentiments
        }

        if (text.contains("!")) {
            if (sentiment > 0.5) sentiment += 0.1;
            else sentiment -= 0.1;
        }

        return Math.max(0.0, Math.min(1.0, sentiment));
    }

    public double getUserItemPreference(String userId, String itemId) {
        return userItemPreferences.getOrDefault(userId, new HashMap<>())
                                  .getOrDefault(itemId, 0.5);
    }

    public int getFeedbackCount(String userId) {
        return feedbackCounts.getOrDefault(userId, 0);
    }

    public boolean isExperiencedUser(String userId) {
        return getFeedbackCount(userId) >= 10;
    }
}