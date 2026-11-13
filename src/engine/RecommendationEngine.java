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
        performanceMetrics.put("feedback_count", 0.0);
        performanceMetrics.put("avg_rating", 0.0);
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
        Map<String, Double> scores = new HashMap<>();
        for (MenuItem item : filteredItems) {
            scores.put(item.getItemId(), preferenceAnalyzer.scoreItemsBasedOnPreferences(item, userPreferences));
        }
        // Best match first; equal scores fall back to rating, then id, so the
        // result does not depend on the order the menu was loaded in.
        return filteredItems.stream()
                .sorted(Comparator.comparingDouble((MenuItem item) -> scores.get(item.getItemId())).reversed()
                        .thenComparing(Comparator.comparingDouble(MenuItem::getAverageRating).reversed())
                        .thenComparing(MenuItem::getItemId))
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
            recordFeedback(rating);
        }
    }

    /**
     * Recommendations for a user with no order history.
     *
     * The hard constraints (stock, context, allergens, vegetarian/vegan) are
     * applied exactly as for a returning user: having no history is never a
     * reason to show someone a dish they cannot eat. If the user has a stored
     * profile the remaining items are ranked against it; otherwise they are
     * ranked by rating.
     */
    public RecommendationResult handleColdStart(String userId, ContextualFactor context, int maxResults) {
        boolean hasProfile = userProfileManager.userExists(userId);
        UserPreferences userPreferences = retrieveUserProfile(userId);
        List<MenuItem> eligible = eligibleItems(userPreferences, context);

        List<MenuItem> ranked = hasProfile
                ? scoreAndRankItems(eligible, userPreferences)
                : rankByPopularity(eligible);

        RecommendationResult result = presentRecommendations(userId, limit(ranked, maxResults));
        result.setAlgorithmUsed(hasProfile ? "Cold Start - Profile Match" : "Cold Start - Popular Items");
        return result;
    }

    /**
     * Items the user may be shown at all: available, in stock, appropriate
     * for the context, free of the user's allergens, and vegetarian or vegan
     * when the user is. These are constraints, not preferences, so they are
     * filters and never part of the score.
     */
    List<MenuItem> eligibleItems(UserPreferences userPreferences, ContextualFactor context) {
        List<MenuItem> items = analyzeMenuItems(menuItems, context);
        items = applyContextualFilters(items, context);
        items = recommendationFilter.filterByDietaryRestrictions(items, userPreferences.getAllergyList());
        if (userPreferences.isVeganPreference()) {
            items = recommendationFilter.filterVeganOnly(items);
        } else if (userPreferences.isVegPreference()) {
            items = recommendationFilter.filterVegetarianOnly(items);
        }
        return items;
    }

    /** Highest rated first; ties broken by item id so the order is repeatable. */
    private List<MenuItem> rankByPopularity(List<MenuItem> items) {
        return items.stream()
                .sorted(Comparator.comparingDouble(MenuItem::getAverageRating).reversed()
                        .thenComparing(MenuItem::getItemId))
                .collect(Collectors.toList());
    }

    private static List<MenuItem> limit(List<MenuItem> items, int maxResults) {
        return items.stream().limit(Math.max(0, maxResults)).collect(Collectors.toList());
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

    /**
     * Top {@code maxResults} items for a user in a context.
     *
     * Dietary constraints are enforced for every user. Ranking depends on what
     * is known: order history gives the full preference ranking, a stored
     * profile alone gives a profile match, and an unknown user gets the
     * highest-rated items.
     */
    public RecommendationResult generateRecommendations(String userId,
                                                       ContextualFactor context,
                                                       int maxResults) {
        long startTime = System.currentTimeMillis();

        RecommendationResult result;
        if (isNewUser(userId)) {
            result = handleColdStart(userId, context, maxResults);
        } else {
            UserPreferences userPreferences = retrieveUserProfile(userId);
            List<MenuItem> ranked = scoreAndRankItems(eligibleItems(userPreferences, context), userPreferences);
            result = presentRecommendations(userId, limit(ranked, maxResults));
        }

        result.addMetadata("processing_time_ms", System.currentTimeMillis() - startTime);
        recordRecommendation(result);
        return result;
    }

    private boolean isNewUser(String userId) {
        OrderHistory history = fetchOrderHistory(userId, "all");
        return history == null || history.getOrderList().isEmpty();
    }

    /**
     * Counts a served recommendation and folds its confidence (0..1) into the
     * running average. A recommendation being produced says nothing about
     * whether the user liked it, so it does not touch the rating metrics.
     */
    private void recordRecommendation(RecommendationResult result) {
        double served = performanceMetrics.get("total_recommendations");
        double avgConfidence = performanceMetrics.get("avg_confidence");
        performanceMetrics.put("avg_confidence",
                (avgConfidence * served + result.getAverageConfidence()) / (served + 1));
        performanceMetrics.put("total_recommendations", served + 1);
    }

    /** Rating metrics come only from real feedback; 3.5 and above counts as a success. */
    private void recordFeedback(double rating) {
        double count = performanceMetrics.get("feedback_count");
        double avgRating = performanceMetrics.get("avg_rating");
        double successRate = performanceMetrics.get("success_rate");
        double success = rating >= 3.5 ? 1.0 : 0.0;
        performanceMetrics.put("avg_rating", (avgRating * count + rating) / (count + 1));
        performanceMetrics.put("success_rate", (successRate * count + success) / (count + 1));
        performanceMetrics.put("feedback_count", count + 1);
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