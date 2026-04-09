package math;

import model.Vector;

/**
 * Implements the Cosine Similarity metric.
 * Cosine similarity measures the cosine of the angle between two non-zero
 * vectors.
 * It is a judgment of orientation and not magnitude: two vectors with the same
 * orientation
 * have a cosine similarity of 1, two vectors at 90 degrees have a similarity of
 * 0,
 * and two vectors diametrically opposed have a similarity of -1.
 * 
 * <p>
 * Formula: (A . B) / (||A|| * ||B||)
 * </p>
 */
public class CosineSimilarity implements DistanceMetric {

    /**
     * Calculates the cosine similarity between two vectors.
     * 
     * @param v1 The first vector.
     * @param v2 The second vector.
     * @return The cosine of the angle between the vectors (range -1 to 1).
     * @throws IllegalArgumentException if vector dimensions do not match.
     */
    @Override
    public double calculate(Vector v1, Vector v2) {
        if (v1.getDimension() != v2.getDimension()) {
            throw new IllegalArgumentException("Vector dimensions must match");
        }

        double dotProduct = 0.0;
        double normA = 0.0;
        double normB = 0.0;

        for (int i = 0; i < v1.getDimension(); i++) {
            dotProduct += v1.get(i) * v2.get(i);
            normA += Math.pow(v1.get(i), 2);
            normB += Math.pow(v2.get(i), 2);
        }

        if (normA == 0 || normB == 0) {
            return 0.0; // Avoid division by zero, though 0-vectors are edge cases in embeddings
        }

        return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
    }

    /**
     * Returns the name of this strategy.
     * 
     * @return "Cosine Similarity"
     */
    @Override
    public String getName() {
        return "Cosine Similarity";
    }
}
