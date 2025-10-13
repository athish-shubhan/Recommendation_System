package restaurant.recommendation.analyzer;

import restaurant.recommendation.model.*;
import java.util.Map;

/**
 * Abstract FeedbackAnalyzer class as per UML diagram
 * Implements Comparable interface for analyzer ranking
 */
public abstract class FeedbackAnalyzer implements Comparable<FeedbackAnalyzer> {
    protected String analyzerName;
    protected double influence;

    public FeedbackAnalyzer(String analyzerName, double influence) {
        this.analyzerName = analyzerName;
        this.influence = influence;
    }

    // Abstract methods as per UML diagram
    public abstract void processCustomerReviews(String itemId, String userId, 
                                              double rating, String comment);

    public abstract void updatePreferencesBasedOnFeedback(String userId, Map<String, Object> feedback);

    public abstract double analyzeSentiment(String feedbackText);

    @Override
    public int compareTo(FeedbackAnalyzer other) {
        return Double.compare(other.influence, this.influence);
    }

    // Getters  
    public String getAnalyzerName() { return analyzerName; }
    public double getInfluence() { return influence; }
    public void setInfluence(double influence) { this.influence = influence; }

    @Override
    public String toString() {
        return String.format("%s{influence=%.2f}", analyzerName, influence);
    }
}