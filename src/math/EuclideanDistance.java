package math;

import model.Vector;

/**
 * Implements the Euclidean Distance metric.
 * This is the "ordinary" straight-line distance between two points in Euclidean
 * space.
 * 
 * <p>
 * Formula: sqrt(sum((a[i] - b[i])^2))
 * </p>
 */
public class EuclideanDistance implements DistanceMetric {

    /**
     * Calculates the Euclidean distance between two vectors.
     * 
     * @param v1 The first vector.
     * @param v2 The second vector.
     * @return The Euclidean distance (>= 0).
     * @throws IllegalArgumentException if vector dimensions do not match.
     */
    @Override
    public double calculate(Vector v1, Vector v2) {
        if (v1.getDimension() != v2.getDimension()) {
            throw new IllegalArgumentException("Vector dimensions must match");
        }

        double sumSquaredDiff = 0.0;
        for (int i = 0; i < v1.getDimension(); i++) {
            double diff = v1.get(i) - v2.get(i);
            sumSquaredDiff += diff * diff;
        }

        return Math.sqrt(sumSquaredDiff);
    }

    /**
     * Returns the name of this strategy.
     * 
     * @return "Euclidean Distance"
     */
    @Override
    public String getName() {
        return "Euclidean Distance";
    }
}
