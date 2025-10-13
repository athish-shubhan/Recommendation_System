package restaurant.recommendation.tracker;

import restaurant.recommendation.model.MenuItem;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;


public class PopularityTracker {

    private final Map<String, Double> trendingScore;

    private final Map<String, Integer> orderCounts;          // itemId -> total orders
    private final Map<String, Double> viewCounts;            // itemId -> total views
    private final Map<String, List<Double>> ratingHistory;   // itemId -> ratings
    private final Map<String, LocalDateTime> lastOrdered;    // itemId -> last order time
    private final Map<String, MenuItem> itemRegistry;        // itemId -> MenuItem (for return objects)

    private LocalDateTime lastUpdated;

    public PopularityTracker() {
        this.trendingScore   = new HashMap<>();
        this.orderCounts     = new HashMap<>();
        this.viewCounts      = new HashMap<>();
        this.ratingHistory   = new HashMap<>();
        this.lastOrdered     = new HashMap<>();
        this.itemRegistry    = new HashMap<>();
        this.lastUpdated     = LocalDateTime.now();
    }

    // ========== Registration ==========

    public void addMenuItem(MenuItem item) {
        if (item == null) return;
        String id = item.getItemId();
        if (id == null) return;

        itemRegistry.put(id, item);
        trendingScore.putIfAbsent(id, 0.0);
        orderCounts.putIfAbsent(id, 0);
        viewCounts.putIfAbsent(id, 0.0);
        ratingHistory.putIfAbsent(id, new ArrayList<>());
    }

    public void removeItem(String itemId) {
        if (itemId == null) return;
        itemRegistry.remove(itemId);
        trendingScore.remove(itemId);
        orderCounts.remove(itemId);
        viewCounts.remove(itemId);
        ratingHistory.remove(itemId);
        lastOrdered.remove(itemId);
    }


    public void recordOrder(String itemId) {
        if (!itemRegistry.containsKey(itemId)) return;
        orderCounts.merge(itemId, 1, Integer::sum);
        lastOrdered.put(itemId, LocalDateTime.now());
        updateTrendingScore(itemId);
    }

    public void recordView(String itemId) {
        if (!itemRegistry.containsKey(itemId)) return;
        viewCounts.merge(itemId, 1.0, Double::sum);
        updateTrendingScore(itemId);
    }

    public void recordRating(String itemId, double rating) {
        if (!itemRegistry.containsKey(itemId)) return;
        double r = Math.max(1.0, Math.min(5.0, rating));
        ratingHistory.computeIfAbsent(itemId, k -> new ArrayList<>()).add(r);
        updateTrendingScore(itemId);
    }

    public void recordMultipleOrders(Map<String, Integer> batch) {
        if (batch == null) return;
        batch.forEach((id, count) -> {
            if (count == null || count <= 0) return;
            for (int i = 0; i < count; i++) recordOrder(id);
        });
    }

    // ========== Trending computation (with recency decay) ==========

    private void updateTrendingScore(String itemId) {
        // Weights: orders 0.40, recency 0.30, ratings 0.20, views 0.10
        double score = 0.0;

        // Orders factor (log-scaled)
        int orders = orderCounts.getOrDefault(itemId, 0);
        score += Math.log1p(orders) * 0.40;

        // Recency factor (decay over 7 days from last order)
        LocalDateTime last = lastOrdered.get(itemId);
        if (last != null) {
            long hours = Math.max(0, ChronoUnit.HOURS.between(last, LocalDateTime.now()));
            double recency = Math.max(0.0, 1.0 - (hours / 168.0)); // 168h = 7 days
            score += recency * 0.30;
        }

        // Rating factor
        List<Double> ratings = ratingHistory.getOrDefault(itemId, Collections.emptyList());
        if (!ratings.isEmpty()) {
            double avg = ratings.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
            score += (avg / 5.0) * 0.20;
        }

        // Views factor (log-scaled)
        double views = viewCounts.getOrDefault(itemId, 0.0);
        score += Math.log1p(views) * 0.10;

        trendingScore.put(itemId, Math.max(0.0, score));
        lastUpdated = LocalDateTime.now();
    }

    private LocalDateTime cutoffFor(String timeWindow) {
        LocalDateTime now = LocalDateTime.now();
        if (timeWindow == null) return now.minusDays(1);

        switch (timeWindow.toLowerCase()) {
            case "1h":
            case "hour":
                return now.minusHours(1);
            case "24h":
            case "day":
                return now.minusDays(1);
            case "7d":
            case "week":
                return now.minusWeeks(1);
            case "30d":
            case "month":
                return now.minusDays(30);
            default:
                return now.minusDays(1);
        }
    }

    private void refreshWithinWindow(String timeWindow) {
        
        for (String id : itemRegistry.keySet()) {
            updateTrendingScore(id);
        }
    }


    public List<MenuItem> getTrendingItems(String timeWindow) {
        refreshWithinWindow(timeWindow);
        return trendingScore.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .map(e -> itemRegistry.get(e.getKey()))
                .filter(Objects::nonNull)
                .limit(10) // top 10
                .collect(Collectors.toList());
    }


    public List<MenuItem> getMostOrderedItems(int limit) {
        return orderCounts.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(Math.max(0, limit))
                .map(e -> itemRegistry.get(e.getKey()))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    public List<MenuItem> getHighestRatedItems(int limit) {
        return ratingHistory.entrySet().stream()
                .filter(e -> !e.getValue().isEmpty())
                .sorted((a, b) -> {
                    double avgA = a.getValue().stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
                    double avgB = b.getValue().stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
                    return Double.compare(avgB, avgA);
                })
                .limit(Math.max(0, limit))
                .map(e -> itemRegistry.get(e.getKey()))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    public List<MenuItem> getMostViewedItems(int limit) {
        return viewCounts.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .limit(Math.max(0, limit))
                .map(e -> itemRegistry.get(e.getKey()))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    public double getTrendingScore(String itemId) {
        return trendingScore.getOrDefault(itemId, 0.0);
    }

    public int getOrderCount(String itemId) {
        return orderCounts.getOrDefault(itemId, 0);
    }

    public double getAverageRating(String itemId) {
        List<Double> r = ratingHistory.getOrDefault(itemId, Collections.emptyList());
        return r.isEmpty() ? 0.0 : r.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
    }

    public double getViewCount(String itemId) {
        return viewCounts.getOrDefault(itemId, 0.0);
    }

    public LocalDateTime getLastOrdered(String itemId) {
        return lastOrdered.get(itemId);
    }

    public Map<String, Object> getItemStatistics(String itemId) {
        Map<String, Object> stats = new HashMap<>();
        stats.put("itemId", itemId);
        stats.put("trendingScore", getTrendingScore(itemId));
        stats.put("orderCount", getOrderCount(itemId));
        stats.put("viewCount", getViewCount(itemId));
        stats.put("averageRating", getAverageRating(itemId));
        stats.put("lastOrdered", getLastOrdered(itemId));
        stats.put("ratingsCount", ratingHistory.getOrDefault(itemId, Collections.emptyList()).size());
        return stats;
    }

    public Map<String, Object> getOverallStatistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("trackedItems", itemRegistry.size());
        stats.put("totalOrders", orderCounts.values().stream().mapToInt(Integer::intValue).sum());
        stats.put("totalViews", viewCounts.values().stream().mapToDouble(Double::doubleValue).sum());
        stats.put("totalRatings", ratingHistory.values().stream().mapToInt(List::size).sum());
        stats.put("lastUpdated", lastUpdated);

        // Top performers
        MenuItem topTrending = getTrendingItems("24h").stream().findFirst().orElse(null);
        MenuItem topOrdered  = getMostOrderedItems(1).stream().findFirst().orElse(null);
        MenuItem topRated    = getHighestRatedItems(1).stream().findFirst().orElse(null);

        stats.put("topTrending", topTrending);
        stats.put("topOrdered", topOrdered);
        stats.put("topRated", topRated);
        return stats;
    }


    public void resetItemStatistics(String itemId) {
        if (itemId == null) return;
        trendingScore.put(itemId, 0.0);
        orderCounts.put(itemId, 0);
        viewCounts.put(itemId, 0.0);
        ratingHistory.put(itemId, new ArrayList<>());
        lastOrdered.remove(itemId);
        lastUpdated = LocalDateTime.now();
    }

    public void resetAllStatistics() {
        trendingScore.clear();
        orderCounts.clear();
        viewCounts.clear();
        ratingHistory.clear();
        lastOrdered.clear();
        itemRegistry.keySet().forEach(this::addMenuItem); // reinitialize
        lastUpdated = LocalDateTime.now();
    }


    public int getTrackedItemCount() {
        return itemRegistry.size();
    }

    public LocalDateTime getLastUpdated() {
        return lastUpdated;
    }

    @Override
    public String toString() {
        return String.format(
                "PopularityTracker{items=%d, totalOrders=%d, totalViews=%.0f, lastUpdated=%s}",
                itemRegistry.size(),
                orderCounts.values().stream().mapToInt(Integer::intValue).sum(),
                viewCounts.values().stream().mapToDouble(Double::doubleValue).sum(),
                lastUpdated != null ? lastUpdated.toLocalDate() : "never"
        );
    }
}
