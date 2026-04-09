package model;

/**
 * Represents a word and its associated vector representations.
 * This class acts as a Data Transfer Object (DTO) holding both the
 * high-dimensional
 * dense vector (used for math/logic) and the low-dimensional PCA vector
 * (used for visualization).
 */
public class WordEmbedding {
    /** The word string itself. */
    private String word;

    /** The full high-dimensional vector (e.g., 300 dimensions). */
    private Vector denseVector;

    /**
     * The reduced-dimensionality vector (e.g., 50 dimensions) for visualization.
     */
    private Vector pcaVector;

    /**
     * Constructs a new WordEmbedding.
     * 
     * @param word        The word string.
     * @param denseVector The original high-dimensional vector.
     * @param pcaVector   The PCA-reduced vector.
     */
    public WordEmbedding(String word, Vector denseVector, Vector pcaVector) {
        this.word = word;
        this.denseVector = denseVector;
        this.pcaVector = pcaVector;
    }

    /**
     * Gets the word.
     * 
     * @return The word string.
     */
    public String getWord() {
        return word;
    }

    /**
     * Gets the high-dimensional dense vector.
     * Used for distance calculations and arithmetic.
     * 
     * @return The dense Vector.
     */
    public Vector getDenseVector() {
        return denseVector;
    }

    /**
     * Gets the reduced-dimensional PCA vector.
     * Used principally for 2D/3D visualization and axis mapping.
     * 
     * @return The PCA Vector.
     */
    public Vector getPcaVector() {
        return pcaVector;
    }

    @Override
    public String toString() {
        return word;
    }
}
