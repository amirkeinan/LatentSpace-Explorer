package math;

import model.Vector;

/**
 * Defines a strategy for calculating the distance or similarity between two
 * vectors.
 * This interface follows the Strategy Design Pattern, allowing the application
 * to
 * switch between different metrics (e.g., Cosine Similarity, Euclidean
 * Distance) at runtime.
 */
public interface DistanceMetric {
    /**
     * Calculates the distance/similarity between two vectors.
     * 
     * @param v1 The first vector.
     * @param v2 The second vector.
     * @return The calculated distance or similarity score.
     */
    double calculate(Vector v1, Vector v2);

    /**
     * Retrieves the display name of this metric.
     * 
     * @return The name of the metric.
     */
    String getName();
}
