package restaurant.recommendation.ml;

import java.io.*;
import java.util.*;
import com.fasterxml.jackson.databind.ObjectMapper;


public class MLPredictionService {
    private static final String PYTHON_SCRIPT_PATH = "python/java_python_bridge.py";
    private final ObjectMapper objectMapper;
    private final String pythonExecutable;

    public MLPredictionService() {
        this.objectMapper = new ObjectMapper();
        this.pythonExecutable = "python3"; // or "python" depending on system
    }

    public MLPredictionService(String pythonPath) {
        this.objectMapper = new ObjectMapper();
        this.pythonExecutable = pythonPath;
    }


    public MLPrediction predictRating(String userId, String itemId, String method) {
        Map<String, Object> request = new HashMap<>();
        request.put("command", "predict_rating");
        request.put("user_id", userId);
        request.put("item_id", itemId);
        request.put("method", method != null ? method : "hybrid");

        try {
            Map<String, Object> response = callPythonService(request);

            if (response.containsKey("error")) {
                System.err.println("ML Prediction Error: " + response.get("error"));
                return new MLPrediction(3.5, 0.3, "fallback", "Error in ML prediction");
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> prediction = (Map<String, Object>) response.get("prediction");

            double rating = ((Number) prediction.get("rating")).doubleValue();
            double confidence = ((Number) prediction.get("confidence")).doubleValue();
            String usedMethod = (String) prediction.get("method");

            return new MLPrediction(rating, confidence, usedMethod, "ML prediction successful");

        } catch (Exception e) {
            System.err.println("Error calling ML service: " + e.getMessage());
            return new MLPrediction(3.5, 0.3, "fallback", "Exception: " + e.getMessage());
        }
    }


    public List<MLRecommendation> getMLRecommendations(String userId, List<String> itemIds, int topK) {
        Map<String, Object> request = new HashMap<>();
        request.put("command", "get_recommendations");
        request.put("user_id", userId);
        request.put("item_ids", itemIds);
        request.put("top_k", topK);

        try {
            Map<String, Object> response = callPythonService(request);

            if (response.containsKey("error")) {
                System.err.println("ML Recommendations Error: " + response.get("error"));
                return new ArrayList<>();
            }

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> recommendations = 
                (List<Map<String, Object>>) response.get("recommendations");

            List<MLRecommendation> result = new ArrayList<>();

            for (Map<String, Object> rec : recommendations) {
                String itemId = (String) rec.get("item_id");
                double predictedRating = ((Number) rec.get("predicted_rating")).doubleValue();
                double confidence = ((Number) rec.get("confidence")).doubleValue();
                String method = (String) rec.get("method");

                result.add(new MLRecommendation(itemId, predictedRating, confidence, method));
            }

            return result;

        } catch (Exception e) {
            System.err.println("Error getting ML recommendations: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    public boolean updateModelWithFeedback(String userId, String itemId, double rating, String context) {
        Map<String, Object> request = new HashMap<>();
        request.put("command", "update_feedback");
        request.put("user_id", userId);
        request.put("item_id", itemId);
        request.put("rating", rating);
        if (context != null) {
            request.put("context", context);
        }

        try {
            Map<String, Object> response = callPythonService(request);

            if (response.containsKey("error")) {
                System.err.println("ML Feedback Update Error: " + response.get("error"));
                return false;
            }

            return "success".equals(response.get("status"));

        } catch (Exception e) {
            System.err.println("Error updating ML model with feedback: " + e.getMessage());
            return false;
        }
    }


    public Map<String, Object> getModelPerformance() {
        Map<String, Object> request = new HashMap<>();
        request.put("command", "get_performance");

        try {
            Map<String, Object> response = callPythonService(request);

            if (response.containsKey("error")) {
                System.err.println("ML Performance Error: " + response.get("error"));
                return new HashMap<>();
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> metrics = (Map<String, Object>) response.get("metrics");
            return metrics != null ? metrics : new HashMap<>();

        } catch (Exception e) {
            System.err.println("Error getting ML performance: " + e.getMessage());
            return new HashMap<>();
        }
    }

    private Map<String, Object> callPythonService(Map<String, Object> request) throws Exception {
        // Convert request to JSON
        String jsonRequest = objectMapper.writeValueAsString(request);

        // Create process to call Python script
        ProcessBuilder pb = new ProcessBuilder(pythonExecutable, PYTHON_SCRIPT_PATH);
        pb.redirectErrorStream(true);
        Process process = pb.start();

        // Send request to Python script
        try (OutputStreamWriter writer = new OutputStreamWriter(process.getOutputStream())) {
            writer.write(jsonRequest);
            writer.flush();
        }

        // Read response from Python script
        StringBuilder response = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
        }

        // Wait for process to complete
        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new RuntimeException("Python script exited with code: " + exitCode);
        }

        // Parse JSON response
        @SuppressWarnings("unchecked")
        Map<String, Object> result = objectMapper.readValue(response.toString(), Map.class);
        return result;
    }


    public static class MLPrediction {
        private final double rating;
        private final double confidence;
        private final String method;
        private final String message;

        public MLPrediction(double rating, double confidence, String method, String message) {
            this.rating = rating;
            this.confidence = confidence;
            this.method = method;
            this.message = message;
        }

        // Getters
        public double getRating() { return rating; }
        public double getConfidence() { return confidence; }
        public String getMethod() { return method; }
        public String getMessage() { return message; }

        @Override
        public String toString() {
            return String.format("MLPrediction{rating=%.2f, confidence=%.2f, method='%s'}", 
                               rating, confidence, method);
        }
    }

 
    public static class MLRecommendation {
        private final String itemId;
        private final double predictedRating;
        private final double confidence;
        private final String method;

        public MLRecommendation(String itemId, double predictedRating, double confidence, String method) {
            this.itemId = itemId;
            this.predictedRating = predictedRating;
            this.confidence = confidence;
            this.method = method;
        }

        // Getters
        public String getItemId() { return itemId; }
        public double getPredictedRating() { return predictedRating; }
        public double getConfidence() { return confidence; }
        public String getMethod() { return method; }

        @Override
        public String toString() {
            return String.format("MLRecommendation{itemId='%s', rating=%.2f, confidence=%.2f}", 
                               itemId, predictedRating, confidence);
        }
    }
}