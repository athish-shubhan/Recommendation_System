package restaurant.recommendation.engine;

import restaurant.recommendation.model.*;
import restaurant.recommendation.analyzer.*;
import restaurant.recommendation.filter.*;
import restaurant.recommendation.tracker.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;


public class RecommendationEngine {
    // Core attributes
    private String algorithmType;
    private LocalDateTime lastUpdateTimestamp;
    private Map<String, Double> performanceMetrics;

    private PreferenceAnalyzer preferenceAnalyzer;
    private FeedbackAnalyzer feedbackAnalyzer;
    private RecommendationFilter recommendationFilter;

    private UserProfileManager userProfileManager;
    private PopularityTracker popularityTracker;
    private Inventory inventory;

    private List<MenuItem> menuItems;
    private Map<String, MenuItem> menuItemMap;
    private List<Offer> activeOffers;

    public RecommendationEngine() {
        this.algorithmType = "Hybrid";
        this.lastUpdateTimestamp = LocalDateTime.now();
        this.performanceMetrics = new HashMap<>();
        this.menuItems = new ArrayList<>();
        this.menuItemMap = new HashMap<>();
        this.activeOffers = new ArrayList<>();

        this.preferenceAnalyzer = new ContentBasedPreferenceAnalyzer();
        this.feedbackAnalyzer = new AdvancedFeedbackAnalyzer();
        this.recommendationFilter = new RecommendationFilter();

        this.userProfileManager = new UserProfileManager();
        this.popularityTracker = new PopularityTracker();
        this.inventory = new Inventory();

        initializePerformanceMetrics();
    }

    public RecommendationEngine(PreferenceAnalyzer preferenceAnalyzer,
                               FeedbackAnalyzer feedbackAnalyzer,
                               UserProfileManager userProfileManager) {
        this();
        this.preferenceAnalyzer = preferenceAnalyzer;
        this.feedbackAnalyzer = feedbackAnalyzer;
        this.userProfileManager = userProfileManager;
    }

    private void initializePerformanceMetrics() {
        performanceMetrics.put("total_recommendations", 0.0);
        performanceMetrics.put("avg_confidence", 0.0);
        performanceMetrics.put("success_rate", 0.0);
    }

    public UserPreferences retrieveUserProfile(String userId) {
        return userProfileManager.loadUserPreferences(userId);
    }

    public OrderHistory fetchOrderHistory(String userId, String timeRange) {
        return userProfileManager.getUserOrderHistory(userId, timeRange);
    }

    public List<MenuItem> analyzeMenuItems(List<MenuItem> menuList, ContextualFactor userContext) {
        return menuList.stream()
                .filter(MenuItem::isAvailable)
                .filter(item -> inventory.isInStock(item.getItemId()))
                .collect(Collectors.toList());
    }

    public double calculateSimilarity(UserPreferences userPreferences, MenuItem menuItem) {
        return preferenceAnalyzer.calculateTasteSimilarity(userPreferences, menuItem);
    }

    public List<MenuItem> applyContextualFilters(List<MenuItem> menuList, ContextualFactor contextualFactors) {
        return recommendationFilter.filterByContext(menuList, contextualFactors);
    }

    public List<MenuItem> scoreAndRankItems(List<MenuItem> filteredItems, UserPreferences userPreferences) {
        return filteredItems.stream()
                .sorted((a, b) -> Double.compare(
                    preferenceAnalyzer.scoreItemsBasedOnPreferences(b, userPreferences),
                    preferenceAnalyzer.scoreItemsBasedOnPreferences(a, userPreferences)))
                .collect(Collectors.toList());
    }

    public RecommendationResult presentRecommendations(String userId, List<MenuItem> recommendationList) {
        RecommendationResult result = new RecommendationResult(userId, recommendationList);
        result.setAlgorithmUsed(algorithmType);

        UserPreferences userPrefs = retrieveUserProfile(userId);

        // Add explanations and confidence scores
        for (MenuItem item : recommendationList) {
            String explanation = preferenceAnalyzer.getExplanation(item, userPrefs);
            double confidence = preferenceAnalyzer.scoreItemsBasedOnPreferences(item, userPrefs);

            result.addExplanation(item.getItemId(), explanation);
            result.addConfidenceScore(item.getItemId(), confidence);
        }

        return result;
    }

    public void refineAlgorithms(Map<String, Object> feedbackData) {
        lastUpdateTimestamp = LocalDateTime.now();

        String userId = (String) feedbackData.get("user_id");
        String itemId = (String) feedbackData.get("item_id");
        Double rating = (Double) feedbackData.get("rating");
        String comment = (String) feedbackData.getOrDefault("comment", "");

        if (userId != null && itemId != null && rating != null) {
            // Process feedback through analyzer
            feedbackAnalyzer.processCustomerReviews(itemId, userId, rating, comment);
            feedbackAnalyzer.updatePreferencesBasedOnFeedback(userId, feedbackData);

            // Update preference analyzer
            preferenceAnalyzer.updateFromFeedback(userId, itemId, rating, comment);

            // Update performance metrics
            updatePerformanceMetrics(rating);
        }
    }

    public RecommendationResult handleColdStart(String userId, ContextualFactor context, int maxResults) {
        // Use popularity-based recommendations for new users
        List<MenuItem> popularItems = popularityTracker.getTrendingItems("24h");

        // Apply basic contextual filters
        List<MenuItem> filteredItems = applyContextualFilters(popularItems, context);

        // Limit results
        List<MenuItem> recommendations = filteredItems.stream()
                .limit(maxResults)
                .collect(Collectors.toList());

        RecommendationResult result = presentRecommendations(userId, recommendations);
        result.setAlgorithmUsed("Cold Start - Popular Items");

        return result;
    }

    public void synchronizeWithInventory() {
        // Update menu item availability based on inventory
        for (MenuItem item : menuItems) {
            boolean inStock = inventory.isInStock(item.getItemId());
            item.setAvailabilityStatus(inStock);
        }

        System.out.println("✓ Synchronized with inventory: " + menuItems.size() + " items updated");
    }

    public void coordinateWithOffers(List<Offer> offerList) {
        this.activeOffers = new ArrayList<>(offerList);

        System.out.println("✓ Coordinated with offers: " + offerList.size() + " active offers");
    }

    public RecommendationResult generateRecommendations(String userId, 
                                                       ContextualFactor context, 
                                                       int maxResults) {
        long startTime = System.currentTimeMillis();

        try {
            // 1. Retrieve user profile
            UserPreferences userPreferences = retrieveUserProfile(userId);

            // 2. Handle cold start scenario
            if (isNewUser(userId)) {
                return handleColdStart(userId, context, maxResults);
            }

            List<MenuItem> availableItems = analyzeMenuItems(menuItems, context);

            if (availableItems.isEmpty()) {
                return new RecommendationResult(userId, new ArrayList<>());
            }

            List<MenuItem> contextFiltered = applyContextualFilters(availableItems, context);

            List<MenuItem> dietaryFiltered = recommendationFilter.filterByDietaryRestrictions(
                contextFiltered, userPreferences.getAllergyList());

            List<MenuItem> rankedItems = scoreAndRankItems(dietaryFiltered, userPreferences);

            List<MenuItem> finalRecommendations = rankedItems.stream()
                    .limit(maxResults)
                    .collect(Collectors.toList());

            RecommendationResult result = presentRecommendations(userId, finalRecommendations);

            long processingTime = System.currentTimeMillis() - startTime;
            result.addMetadata("processing_time_ms", processingTime);

            updatePerformanceMetrics(4.0); // Default positive for successful recommendation

            return result;

        } catch (Exception e) {
            System.err.println("Error generating recommendations: " + e.getMessage());
            return handleColdStart(userId, context, maxResults);
        }
    }

    // Utility methods
    private boolean isNewUser(String userId) {
        OrderHistory history = fetchOrderHistory(userId, "all");
        return history == null || history.getOrderList().isEmpty();
    }

    private void updatePerformanceMetrics(double rating) {
        double currentTotal = performanceMetrics.get("total_recommendations");
        double currentAvg = performanceMetrics.get("avg_confidence");

        performanceMetrics.put("total_recommendations", currentTotal + 1);

        if (currentTotal > 0) {
            double newAvg = ((currentAvg * currentTotal) + rating) / (currentTotal + 1);
            performanceMetrics.put("avg_confidence", newAvg);
        } else {
            performanceMetrics.put("avg_confidence", rating);
        }

        double successCount = rating >= 3.5 ? 1.0 : 0.0;
        double currentSuccessRate = performanceMetrics.getOrDefault("success_rate", 0.0);
        double newSuccessRate = ((currentSuccessRate * currentTotal) + successCount) / (currentTotal + 1);
        performanceMetrics.put("success_rate", newSuccessRate);
    }

    public void addMenuItem(MenuItem menuItem) {
        menuItems.add(menuItem);
        menuItemMap.put(menuItem.getItemId(), menuItem);

        // Update popularity tracker
        popularityTracker.addMenuItem(menuItem);
    }

    public MenuItem getMenuItem(String itemId) {
        return menuItemMap.get(itemId);
    }

    public List<MenuItem> getMenuItems() {
        return new ArrayList<>(menuItems);
    }

    public void removeMenuItem(String itemId) {
        MenuItem item = menuItemMap.remove(itemId);
        if (item != null) {
            menuItems.remove(item);
        }
    }

    public UserProfileManager getUserProfileManager() {
        return userProfileManager;
    }

    public void setUserProfileManager(UserProfileManager userProfileManager) {
        this.userProfileManager = userProfileManager;
    }

    public PopularityTracker getPopularityTracker() {
        return popularityTracker;
    }

    public void setPopularityTracker(PopularityTracker popularityTracker) {
        this.popularityTracker = popularityTracker;
    }

    public Inventory getInventory() {
        return inventory;
    }

    public void setInventory(Inventory inventory) {
        this.inventory = inventory;
    }

    // Getters for composition relationships (no setters - lifecycle bound)
    public PreferenceAnalyzer getPreferenceAnalyzer() {
        return preferenceAnalyzer;
    }

    public FeedbackAnalyzer getFeedbackAnalyzer() {
        return feedbackAnalyzer;
    }

    public RecommendationFilter getRecommendationFilter() {
        return recommendationFilter;
    }

    // Other getters and setters
    public String getAlgorithmType() {
        return algorithmType;
    }

    public void setAlgorithmType(String algorithmType) {
        this.algorithmType = algorithmType;
    }

    public LocalDateTime getLastUpdateTimestamp() {
        return lastUpdateTimestamp;
    }

    public Map<String, Double> getPerformanceMetrics() {
        return new HashMap<>(performanceMetrics);
    }

    public List<Offer> getActiveOffers() {
        return new ArrayList<>(activeOffers);
    }

    @Override
    public String toString() {
        return String.format("RecommendationEngine{algorithm='%s', menuItems=%d, offers=%d}", 
                           algorithmType, menuItems.size(), activeOffers.size());
    }
}