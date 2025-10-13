package restaurant.recommendation.utils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * SimilarityUtils - Fully implemented per UML intent:
 *  - Cosine similarity for vectors and sparse maps
 *  - Jaccard similarity for sets
 *  - Pearson correlation for co-rated vectors
 *  - Utility normalization and distance helpers
 *
 * All methods are null/empty safe and numerically stable.
 */
public final class SimilarityUtils {

    private SimilarityUtils() {
        // Utility class: prevent instantiation
    }

    // ========== Cosine similarity ==========

    // Dense vectors
    public static double cosine(double[] a, double[] b) {
        if (!validSameLength(a, b)) return 0.0;
        double dot = 0.0, na = 0.0, nb = 0.0;
        for (int i = 0; i < a.length; i++) {
            dot += a[i] * b[i];
            na  += a[i] * a[i];
            nb  += b[i] * b[i];
        }
        if (na == 0.0 || nb == 0.0) return 0.0;
        return dot / (Math.sqrt(na) * Math.sqrt(nb));
    }

    // Sparse keyed vectors (e.g., item ratings per user)
    public static double cosine(Map<String, Double> a, Map<String, Double> b) {
        if (a == null || b == null || a.isEmpty() || b.isEmpty()) return 0.0;

        // Iterate over the smaller map for efficiency
        Map<String, Double> small = a.size() <= b.size() ? a : b;
        Map<String, Double> large = small == a ? b : a;

        double dot = 0.0, na = 0.0, nb = 0.0;

        for (Map.Entry<String, Double> e : small.entrySet()) {
            String key = e.getKey();
            Double va = safe(e.getValue());
            Double vb = safe(large.get(key));
            if (vb != null) {
                dot += va * vb;
            }
        }
        for (double va : a.values()) na += va * va;
        for (double vb : b.values()) nb += vb * vb;

        if (na == 0.0 || nb == 0.0) return 0.0;
        return dot / (Math.sqrt(na) * Math.sqrt(nb));
    }

    // ========== Jaccard similarity ==========

    public static <T> double jaccard(Set<T> a, Set<T> b) {
        if (a == null || b == null || a.isEmpty() || b.isEmpty()) return 0.0;
        Set<T> inter = new HashSet<>(a);
        inter.retainAll(b);
        Set<T> union = new HashSet<>(a);
        union.addAll(b);
        if (union.isEmpty()) return 0.0;
        return (double) inter.size() / (double) union.size();
    }

    // ========== Pearson correlation (user-user or item-item) ==========

    // Using common keys only
    public static double pearson(Map<String, Double> a, Map<String, Double> b) {
        if (a == null || b == null || a.isEmpty() || b.isEmpty()) return 0.0;

        // Find co-rated keys
        Set<String> common = new HashSet<>(a.keySet());
        common.retainAll(b.keySet());
        if (common.size() < 2) return 0.0;

        double meanA = common.stream().mapToDouble(k -> safe(a.get(k))).average().orElse(0.0);
        double meanB = common.stream().mapToDouble(k -> safe(b.get(k))).average().orElse(0.0);

        double num = 0.0, da = 0.0, db = 0.0;
        for (String k : common) {
            double va = safe(a.get(k)) - meanA;
            double vb = safe(b.get(k)) - meanB;
            num += va * vb;
            da  += va * va;
            db  += vb * vb;
        }

        double denom = Math.sqrt(da) * Math.sqrt(db);
        if (denom == 0.0) return 0.0;
        return num / denom;
    }

    // ========== Euclidean distance and similarity ==========

    public static double euclideanDistance(double[] a, double[] b) {
        if (!validSameLength(a, b)) return Double.POSITIVE_INFINITY;
        double sum = 0.0;
        for (int i = 0; i < a.length; i++) {
            double d = a[i] - b[i];
            sum += d * d;
        }
        return Math.sqrt(sum);
    }

    // Convert distance to similarity in (0,1]; larger distance -> smaller similarity
    public static double distanceToSimilarity(double distance) {
        if (Double.isInfinite(distance) || Double.isNaN(distance)) return 0.0;
        return 1.0 / (1.0 + Math.max(0.0, distance));
    }

    // ========== Normalization helpers ==========

    // Min-max normalize to [0,1]
    public static double[] minMaxNormalize(double[] v) {
        if (v == null || v.length == 0) return new double[0];
        double min = Arrays.stream(v).min().orElse(0.0);
        double max = Arrays.stream(v).max().orElse(0.0);
        double range = max - min;
        if (range == 0.0) {
            double[] z = new double[v.length];
            Arrays.fill(z, 0.0);
            return z;
        }
        double[] out = new double[v.length];
        for (int i = 0; i < v.length; i++) out[i] = (v[i] - min) / range;
        return out;
    }

    // Z-score normalize (mean 0, std 1)
    public static double[] zScoreNormalize(double[] v) {
        if (v == null || v.length == 0) return new double[0];
        double mean = Arrays.stream(v).average().orElse(0.0);
        double variance = 0.0;
        for (double x : v) variance += (x - mean) * (x - mean);
        variance /= v.length;
        double std = Math.sqrt(variance);
        if (std == 0.0) {
            double[] z = new double[v.length];
            Arrays.fill(z, 0.0);
            return z;
        }
        double[] out = new double[v.length];
        for (int i = 0; i < v.length; i++) out[i] = (v[i] - mean) / std;
        return out;
    }

    // ========== Utility methods ==========

    private static boolean validSameLength(double[] a, double[] b) {
        return a != null && b != null && a.length == b.length && a.length > 0;
    }

    private static Double safe(Double x) {
        return x == null ? 0.0 : x;
    }

    public static Map<String, Double> toUnitVector(Map<String, Double> v) {
        if (v == null || v.isEmpty()) return Collections.emptyMap();
        double norm = Math.sqrt(v.values().stream().mapToDouble(d -> d * d).sum());
        if (norm == 0.0) return v;
        Map<String, Double> out = new HashMap<>();
        for (Map.Entry<String, Double> e : v.entrySet()) {
            out.put(e.getKey(), e.getValue() / norm);
        }
        return out;
    }

    public static double dot(Map<String, Double> a, Map<String, Double> b) {
        if (a == null || b == null || a.isEmpty() || b.isEmpty()) return 0.0;
        Map<String, Double> small = a.size() <= b.size() ? a : b;
        Map<String, Double> large = small == a ? b : a;
        double s = 0.0;
        for (Map.Entry<String, Double> e : small.entrySet()) {
            Double vb = large.get(e.getKey());
            if (vb != null) s += safe(e.getValue()) * vb;
        }
        return s;
    }

    public static Set<String> intersectionKeys(Map<String, ?> a, Map<String, ?> b) {
        if (a == null || b == null) return Collections.emptySet();
        Set<String> s = new HashSet<>(a.keySet());
        s.retainAll(b.keySet());
        return s;
    }

    public static String topKToString(Map<String, Double> scores, int k) {
        if (scores == null || scores.isEmpty()) return "[]";
        return scores.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .limit(Math.max(0, k))
                .map(e -> e.getKey() + ":" + String.format(Locale.US, "%.3f", e.getValue()))
                .collect(Collectors.joining(", ", "[", "]"));
    }

    @Override
    public String toString() {
        return "SimilarityUtils{cosine,jaccard,pearson,euclidean,normalize}";
    }
}
