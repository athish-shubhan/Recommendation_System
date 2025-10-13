package restaurant.recommendation.engine;

import restaurant.recommendation.model.*;
import restaurant.recommendation.analyzer.FeedbackAnalyzer;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

public class UserProfileManager {
    private Map<String, UserPreferences> userPreferences;
    private Map<String, OrderHistory> userOrderHistories;
    private FeedbackAnalyzer feedbackAnalyzer;

    public UserProfileManager() {
        this.userPreferences = new HashMap<>();
        this.userOrderHistories = new HashMap<>();
    }

    public UserProfileManager(FeedbackAnalyzer feedbackAnalyzer) {
        this();
        this.feedbackAnalyzer = feedbackAnalyzer;
    }

    public UserPreferences loadUserPreferences(String userId) {
        return userPreferences.computeIfAbsent(userId, this::createDefaultUserPreferences);
    }

    public void updateUserPreferences(String userId, UserPreferences newPreferences) {
        userPreferences.put(userId, newPreferences);
        System.out.println("✓ Updated preferences for user: " + userId);
    }

    public OrderHistory getUserOrderHistory(String userId, String timeRange) {
        OrderHistory history = userOrderHistories.get(userId);
        if (history == null) {
            history = new OrderHistory(userId);
            userOrderHistories.put(userId, history);
        }

        if ("recent".equals(timeRange)) {
            return filterRecentOrders(history, 30); // Last 30 days
        } else if ("week".equals(timeRange)) {
            return filterRecentOrders(history, 7); // Last week
        }

        return history;
    }

    public void addOrderToHistory(String userId, Order order) {
        OrderHistory history = userOrderHistories.computeIfAbsent(userId, OrderHistory::new);
        history.addOrder(order);

        updatePreferencesFromOrder(userId, order);
    }

    public void createUserProfile(String userId, UserPreferences initialPreferences) {
        userPreferences.put(userId, initialPreferences);
        userOrderHistories.put(userId, new OrderHistory(userId));
        System.out.println("✓ Created new user profile: " + userId);
    }

    public boolean userExists(String userId) {
        return userPreferences.containsKey(userId);
    }

    public Set<String> getAllUserIds() {
        return new HashSet<>(userPreferences.keySet());
    }

    public void updatePreferencesFromFeedback(String userId, String itemId, 
                                             double rating, String comment) {
        UserPreferences prefs = loadUserPreferences(userId);

        if (feedbackAnalyzer != null) {
            Map<String, Object> feedbackData = new HashMap<>();
            feedbackData.put("user_id", userId);
            feedbackData.put("item_id", itemId);
            feedbackData.put("rating", rating);
            feedbackData.put("comment", comment);

            feedbackAnalyzer.updatePreferencesBasedOnFeedback(userId, feedbackData);
        }

        updatePreferencesFromRating(prefs, itemId, rating);

        System.out.println(String.format("✓ Updated preferences from feedback: %s -> %s (%.1f)", 
                                        userId, itemId, rating));
    }

    public Map<String, Object> getUserAnalytics(String userId) {
        Map<String, Object> analytics = new HashMap<>();

        UserPreferences prefs = loadUserPreferences(userId);
        OrderHistory history = getUserOrderHistory(userId, "all");

        analytics.put("total_orders", history.getOrderList().size());
        analytics.put("spiciness_level", prefs.getSpicinessLevel());
        analytics.put("is_vegetarian", prefs.isVegPreference());
        analytics.put("allergen_count", prefs.getAllergyList().size());
        analytics.put("favorite_cuisines", prefs.getFavouriteCuisines());
        analytics.put("price_range", Arrays.asList(prefs.getPriceRangeLower(), prefs.getPriceRangeUpper()));

        Map<String, Integer> categoryFrequency = calculateCategoryFrequency(history);
        analytics.put("category_preferences", categoryFrequency);

        double avgRating = calculateAverageRatingGiven(history);
        analytics.put("average_rating_given", avgRating);

        return analytics;
    }

    public List<String> getSimilarUsers(String userId, int limit) {
        UserPreferences targetUser = loadUserPreferences(userId);

        return userPreferences.entrySet().stream()
                .filter(entry -> !entry.getKey().equals(userId))
                .sorted((e1, e2) -> Double.compare(
                    calculateUserSimilarity(targetUser, e2.getValue()),
                    calculateUserSimilarity(targetUser, e1.getValue())))
                .limit(limit)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    // Helper methods
    private UserPreferences createDefaultUserPreferences(String userId) {
        UserPreferences prefs = new UserPreferences(userId);
        prefs.setSpicinessLevel(2); // Mild
        prefs.setPriceRange(0.0, 200.0); // Wide range
        return prefs;
    }

    private OrderHistory filterRecentOrders(OrderHistory history, int days) {
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(days);

        List<Order> recentOrders = history.getOrderList().stream()
                .filter(order -> order.getTimestamp().isAfter(cutoffDate))
                .collect(Collectors.toList());

        OrderHistory filteredHistory = new OrderHistory(history.getUserId());
        recentOrders.forEach(filteredHistory::addOrder);

        return filteredHistory;
    }

    private void updatePreferencesFromOrder(String userId, Order order) {
        UserPreferences prefs = loadUserPreferences(userId);

        // Update category preferences based on ordered items
        for (String itemId : order.getItemIds()) {
        ///need from other grp members
        }

        double orderTotal = order.getTotalAmount();
        if (orderTotal > prefs.getPriceRangeUpper()) {
            prefs.setPriceRange(prefs.getPriceRangeLower(), orderTotal * 1.2);
        }
    }

    private void updatePreferencesFromRating(UserPreferences prefs, String itemId, double rating) {
        if (rating >= 4.0) {
            System.out.println("Positive feedback recorded, updating preferences");
        } else if (rating <= 2.0) {
            System.out.println("Negative feedback recorded, updating avoid preferences");
        }
    }

    private Map<String, Integer> calculateCategoryFrequency(OrderHistory history) {
        Map<String, Integer> frequency = new HashMap<>();

        for (Order order : history.getOrderList()) {
            ///need from other grp members
            frequency.merge("Food", 1, Integer::sum);
        }

        return frequency;
    }

    private double calculateAverageRatingGiven(OrderHistory history) {
        return history.getOrderList().stream()
                .filter(order -> order.getRating() > 0)
                .mapToDouble(Order::getRating)
                .average()
                .orElse(3.0);
    }

    private double calculateUserSimilarity(UserPreferences user1, UserPreferences user2) {
        double similarity = 0.0;

        // Compare dietary preferences
        if (user1.isVegPreference() == user2.isVegPreference()) {
            similarity += 0.3;
        }

        // Compare spiciness levels
        double spiceDiff = Math.abs(user1.getSpicinessLevel() - user2.getSpicinessLevel());
        similarity += (5.0 - spiceDiff) / 5.0 * 0.2;

        // Compare favorite cuisines
        Set<String> common = new HashSet<>(user1.getFavouriteCuisines());
        common.retainAll(user2.getFavouriteCuisines());

        int totalCuisines = user1.getFavouriteCuisines().size() + user2.getFavouriteCuisines().size();
        if (totalCuisines > 0) {
            similarity += (double) common.size() * 2 / totalCuisines * 0.3;
        }

        // Compare price ranges
        double priceOverlap = calculatePriceRangeOverlap(user1, user2);
        similarity += priceOverlap * 0.2;

        return Math.max(0.0, Math.min(1.0, similarity));
    }

    private double calculatePriceRangeOverlap(UserPreferences user1, UserPreferences user2) {
        double min1 = user1.getPriceRangeLower();
        double max1 = user1.getPriceRangeUpper();
        double min2 = user2.getPriceRangeLower();
        double max2 = user2.getPriceRangeUpper();

        double overlapStart = Math.max(min1, min2);
        double overlapEnd = Math.min(max1, max2);

        if (overlapStart >= overlapEnd) {
            return 0.0; // No overlap
        }

        double overlapLength = overlapEnd - overlapStart;
        double range1Length = max1 - min1;
        double range2Length = max2 - min2;
        double averageRange = (range1Length + range2Length) / 2.0;

        return Math.min(1.0, overlapLength / averageRange);
    }

    public void importUserProfiles(Map<String, UserPreferences> profiles) {
        userPreferences.putAll(profiles);
        System.out.println("✓ Imported " + profiles.size() + " user profiles");
    }

    public Map<String, UserPreferences> exportUserProfiles() {
        return new HashMap<>(userPreferences);
    }

    public void clearUserData(String userId) {
        userPreferences.remove(userId);
        userOrderHistories.remove(userId);
        System.out.println("✓ Cleared data for user: " + userId);
    }

    public FeedbackAnalyzer getFeedbackAnalyzer() {
        return feedbackAnalyzer;
    }

    public void setFeedbackAnalyzer(FeedbackAnalyzer feedbackAnalyzer) {
        this.feedbackAnalyzer = feedbackAnalyzer;
    }

    public int getUserCount() {
        return userPreferences.size();
    }

    public int getTotalOrders() {
        return userOrderHistories.values().stream()
                .mapToInt(history -> history.getOrderList().size())
                .sum();
    }

    @Override
    public String toString() {
        return String.format("UserProfileManager{users=%d, totalOrders=%d}", 
                           getUserCount(), getTotalOrders());
    }
}