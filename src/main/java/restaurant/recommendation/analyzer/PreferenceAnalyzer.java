package restaurant.recommendation.analyzer;

import restaurant.recommendation.model.*;
import java.util.List;

/**
 * Abstract PreferenceAnalyzer class as per UML diagram
 * Implements Comparable interface for analyzer ranking
 */
public abstract class PreferenceAnalyzer implements Comparable<PreferenceAnalyzer> {
    protected String analyzerName;
    protected double weight;

    public PreferenceAnalyzer(String analyzerName, double weight) {
        this.analyzerName = analyzerName;
        this.weight = weight;
    }

    // Abstract methods as per UML diagram
    public abstract double scoreItemsBasedOnPreferences(MenuItem menuItem, UserPreferences userPreferences);

    public abstract double calculateTasteSimilarity(UserPreferences userPreferences, MenuItem menuItem);

    public abstract List<MenuItem> getRecommendations(List<MenuItem> availableItems, 
                                                     UserPreferences userPreferences, 
                                                     int maxResults);

    public abstract void updateFromFeedback(String userId, String itemId, double rating, String feedback);

    public abstract String getExplanation(MenuItem menuItem, UserPreferences userPreferences);

    @Override
    public int compareTo(PreferenceAnalyzer other) {
        return Double.compare(other.weight, this.weight);
    }

    // Getters
    public String getAnalyzerName() { return analyzerName; }
    public double getWeight() { return weight; }
    public void setWeight(double weight) { this.weight = weight; }

    @Override
    public String toString() {
        return String.format("%s{weight=%.2f}", analyzerName, weight);
    }
}