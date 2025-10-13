package restaurant.recommendation.model;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;


public class RecommendationResult {
    private List<MenuItem> recommendationList;
    private Map<String, String> explanations;

    private String userId;
    private Map<String, Double> confidenceScores;
    private LocalDateTime generatedAt;
    private String algorithmUsed;
    private Map<String, Object> metadata;
    private double averageConfidence;
    private long processingTimeMs;

    public RecommendationResult() {
        this.recommendationList = new ArrayList<>();
        this.explanations = new HashMap<>();
        this.confidenceScores = new HashMap<>();
        this.metadata = new HashMap<>();
        this.generatedAt = LocalDateTime.now();
    }

    public RecommendationResult(String userId, List<MenuItem> recommendationList) {
        this();
        this.userId = userId;
        this.recommendationList = recommendationList != null ? 
                                  new ArrayList<>(recommendationList) : new ArrayList<>();
    }

    public RecommendationResult(String userId, List<MenuItem> recommendationList, 
                               String algorithmUsed) {
        this(userId, recommendationList);
        this.algorithmUsed = algorithmUsed;
    }

    public void addRecommendation(MenuItem item, String explanation, double confidence) {
        if (item != null) {
            recommendationList.add(item);

            if (explanation != null) {
                explanations.put(item.getItemId(), explanation);
            }

            confidenceScores.put(item.getItemId(), 
                               Math.max(0.0, Math.min(1.0, confidence)));

            updateAverageConfidence();
        }
    }

    public void addExplanation(String itemId, String explanation) {
        if (itemId != null && explanation != null) {
            explanations.put(itemId, explanation);
        }
    }

    public void addConfidenceScore(String itemId, double confidence) {
        if (itemId != null) {
            confidenceScores.put(itemId, Math.max(0.0, Math.min(1.0, confidence)));
            updateAverageConfidence();
        }
    }

    public String getExplanation(String itemId) {
        return explanations.getOrDefault(itemId, "No explanation available");
    }

    public double getConfidenceScore(String itemId) {
        return confidenceScores.getOrDefault(itemId, 0.0);
    }

    public void addMetadata(String key, Object value) {
        if (key != null && value != null) {
            metadata.put(key, value);
        }
    }

    // Analysis methods
    public double getAverageConfidence() {
        return averageConfidence;
    }

    private void updateAverageConfidence() {
        if (confidenceScores.isEmpty()) {
            this.averageConfidence = 0.0;
            return;
        }

        this.averageConfidence = confidenceScores.values().stream()
                .mapToDouble(Double::doubleValue)
                .average()
                .orElse(0.0);
    }

    public MenuItem getTopRecommendation() {
        return recommendationList.isEmpty() ? null : recommendationList.get(0);
    }

    public List<MenuItem> getHighConfidenceRecommendations(double threshold) {
        return recommendationList.stream()
                .filter(item -> getConfidenceScore(item.getItemId()) >= threshold)
                .collect(java.util.stream.Collectors.toList());
    }

    public List<MenuItem> getTopRecommendations(int count) {
        return recommendationList.stream()
                .limit(Math.max(0, count))
                .collect(java.util.stream.Collectors.toList());
    }

    public boolean isEmpty() {
        return recommendationList.isEmpty();
    }

    public int size() {
        return recommendationList.size();
    }

    public boolean hasExplanations() {
        return !explanations.isEmpty();
    }

    public boolean hasConfidenceScores() {
        return !confidenceScores.isEmpty();
    }

    // Quality assessment methods
    public double getQualityScore() {
        if (recommendationList.isEmpty()) return 0.0;

        double avgRating = recommendationList.stream()
                .mapToDouble(MenuItem::getAverageRating)
                .average()
                .orElse(0.0);

        double avgConfidence = getAverageConfidence();
        double diversityScore = calculateDiversityScore();

        // Weighted quality score
        return (avgRating / 5.0 * 0.4) + (avgConfidence * 0.3) + (diversityScore * 0.3);
    }

    private double calculateDiversityScore() {
        if (recommendationList.size() <= 1) return 0.0;

        // Calculate category diversity
        java.util.Set<String> categories = recommendationList.stream()
                .filter(item -> item.getCategory() != null)
                .map(item -> item.getCategory().getCategoryName())
                .collect(java.util.stream.Collectors.toSet());

        return Math.min(1.0, (double) categories.size() / recommendationList.size());
    }

    public Map<String, Integer> getCategoryDistribution() {
        Map<String, Integer> distribution = new HashMap<>();

        for (MenuItem item : recommendationList) {
            if (item.getCategory() != null) {
                String categoryName = item.getCategory().getCategoryName();
                distribution.merge(categoryName, 1, Integer::sum);
            }
        }

        return distribution;
    }

    public Map<String, Double> getPriceRangeAnalysis() {
        if (recommendationList.isEmpty()) return new HashMap<>();

        double[] prices = recommendationList.stream()
                .mapToDouble(MenuItem::getPrice)
                .toArray();

        java.util.Arrays.sort(prices);

        Map<String, Double> analysis = new HashMap<>();
        analysis.put("minPrice", prices[0]);
        analysis.put("maxPrice", prices[prices.length - 1]);
        analysis.put("avgPrice", java.util.Arrays.stream(prices).average().orElse(0.0));

        if (prices.length > 1) {
            analysis.put("medianPrice", prices[prices.length / 2]);
            analysis.put("priceRange", prices[prices.length - 1] - prices[0]);
        }

        return analysis;
    }

    // Filtering and manipulation
    public RecommendationResult filterByCategory(String categoryName) {
        List<MenuItem> filtered = recommendationList.stream()
                .filter(item -> item.getCategory() != null && 
                               item.getCategory().getCategoryName().equals(categoryName))
                .collect(java.util.stream.Collectors.toList());

        RecommendationResult filteredResult = new RecommendationResult(userId, filtered);
        filteredResult.algorithmUsed = this.algorithmUsed + " (filtered by " + categoryName + ")";

        // Copy explanations and confidence scores for filtered items
        for (MenuItem item : filtered) {
            String itemId = item.getItemId();
            if (explanations.containsKey(itemId)) {
                filteredResult.explanations.put(itemId, explanations.get(itemId));
            }
            if (confidenceScores.containsKey(itemId)) {
                filteredResult.confidenceScores.put(itemId, confidenceScores.get(itemId));
            }
        }

        filteredResult.updateAverageConfidence();
        return filteredResult;
    }

    public RecommendationResult filterByPriceRange(double minPrice, double maxPrice) {
        List<MenuItem> filtered = recommendationList.stream()
                .filter(item -> {
                    double price = item.getPrice();
                    return price >= minPrice && price <= maxPrice;
                })
                .collect(java.util.stream.Collectors.toList());

        RecommendationResult filteredResult = new RecommendationResult(userId, filtered);
        filteredResult.algorithmUsed = this.algorithmUsed + 
                                     String.format(" (price %.0f-%.0f)", minPrice, maxPrice);

        // Copy relevant data
        for (MenuItem item : filtered) {
            String itemId = item.getItemId();
            if (explanations.containsKey(itemId)) {
                filteredResult.explanations.put(itemId, explanations.get(itemId));
            }
            if (confidenceScores.containsKey(itemId)) {
                filteredResult.confidenceScores.put(itemId, confidenceScores.get(itemId));
            }
        }

        filteredResult.updateAverageConfidence();
        return filteredResult;
    }

    // Sorting methods
    public void sortByConfidence() {
        recommendationList.sort((a, b) -> Double.compare(
            getConfidenceScore(b.getItemId()), 
            getConfidenceScore(a.getItemId())
        ));
    }

    public void sortByRating() {
        recommendationList.sort((a, b) -> Double.compare(
            b.getAverageRating(), 
            a.getAverageRating()
        ));
    }

    public void sortByPrice() {
        recommendationList.sort((a, b) -> Double.compare(a.getPrice(), b.getPrice()));
    }

    // Export methods
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();

        map.put("userId", userId);
        map.put("algorithmUsed", algorithmUsed);
        map.put("generatedAt", generatedAt);
        map.put("size", recommendationList.size());
        map.put("averageConfidence", averageConfidence);
        map.put("qualityScore", getQualityScore());
        map.put("processingTimeMs", processingTimeMs);

        // Add recommendation details
        List<Map<String, Object>> items = new ArrayList<>();
        for (MenuItem item : recommendationList) {
            Map<String, Object> itemMap = new HashMap<>();
            itemMap.put("itemId", item.getItemId());
            itemMap.put("name", item.getName());
            itemMap.put("price", item.getPrice());
            itemMap.put("rating", item.getAverageRating());
            itemMap.put("confidence", getConfidenceScore(item.getItemId()));
            itemMap.put("explanation", getExplanation(item.getItemId()));
            items.add(itemMap);
        }
        map.put("recommendations", items);

        // Add metadata
        map.put("metadata", new HashMap<>(metadata));

        return map;
    }

    // Getters and Setters
    public List<MenuItem> getRecommendationList() { 
        return new ArrayList<>(recommendationList); 
    }

    public void setRecommendationList(List<MenuItem> recommendationList) { 
        this.recommendationList = recommendationList != null ? 
                                  new ArrayList<>(recommendationList) : new ArrayList<>();
        updateAverageConfidence();
    }

    public Map<String, String> getExplanations() { 
        return new HashMap<>(explanations); 
    }

    public void setExplanations(Map<String, String> explanations) { 
        this.explanations = explanations != null ? new HashMap<>(explanations) : new HashMap<>();
    }

    public Map<String, Double> getConfidenceScores() { 
        return new HashMap<>(confidenceScores); 
    }

    public void setConfidenceScores(Map<String, Double> confidenceScores) { 
        this.confidenceScores = confidenceScores != null ? new HashMap<>(confidenceScores) : new HashMap<>();
        updateAverageConfidence();
    }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public LocalDateTime getGeneratedAt() { return generatedAt; }
    public void setGeneratedAt(LocalDateTime generatedAt) { this.generatedAt = generatedAt; }

    public String getAlgorithmUsed() { return algorithmUsed; }
    public void setAlgorithmUsed(String algorithmUsed) { this.algorithmUsed = algorithmUsed; }

    public Map<String, Object> getMetadata() { return new HashMap<>(metadata); }
    public void setMetadata(Map<String, Object> metadata) { 
        this.metadata = metadata != null ? new HashMap<>(metadata) : new HashMap<>();
    }

    public long getProcessingTimeMs() { return processingTimeMs; }
    public void setProcessingTimeMs(long processingTimeMs) { 
        this.processingTimeMs = Math.max(0, processingTimeMs); 
    }

    @Override
    public String toString() {
        return String.format("RecommendationResult{userId='%s', items=%d, algorithm='%s', confidence=%.2f, quality=%.2f}", 
                           userId, recommendationList.size(), algorithmUsed, averageConfidence, getQualityScore());
    }
}