package model;

import java.util.Arrays;

/**
 * Represents a mathematical vector in N-dimensional space.
 * This class provides immutable operations for vector arithmetic such as
 * addition,
 * subtraction, scaling, and magnitude calculation.
 * 
 * <p>
 * It serves as the fundamental data structure for calculations in the
 * LatentSpace Explorer application.
 * </p>
 */
public class Vector {
    /** The components of the vector. */
    private double[] values;

    /**
     * Constructs a new Vector with the specified components.
     * 
     * @param values An array of doubles representing the vector's components.
     */
    public Vector(double[] values) {
        this.values = values;
    }

    /**
     * Retrieves the raw component values of the vector.
     * 
     * @return The double array backing this vector.
     */
    public double[] getValues() {
        return values;
    }

    /**
     * Returns the dimensionality of the vector.
     * 
     * @return The number of components in the vector.
     */
    public int getDimension() {
        return values.length;
    }

    /**
     * Gets the value at a specific dimension index.
     * 
     * @param index The 0-based index of the component to retrieve.
     * @return The value at the specified index.
     */
    public double get(int index) {
        return values[index];
    }

    /**
     * Adds another vector to this vector.
     * This operation is component-wise: result[i] = this[i] + other[i].
     * 
     * @param other The vector to add. Must have the same dimension as this vector.
     * @return A new Vector representing the sum.
     * @throws IllegalArgumentException if dimensions do not match.
     */
    public Vector add(Vector other) {
        if (this.getDimension() != other.getDimension()) {
            throw new IllegalArgumentException("Vector dimensions must match");
        }
        double[] result = new double[this.getDimension()];
        for (int i = 0; i < result.length; i++) {
            result[i] = this.values[i] + other.values[i];
        }
        return new Vector(result);
    }

    /**
     * Subtracts another vector from this vector.
     * This operation is component-wise: result[i] = this[i] - other[i].
     * 
     * @param other The vector to subtract. Must have the same dimension as this
     *              vector.
     * @return A new Vector representing the difference.
     * @throws IllegalArgumentException if dimensions do not match.
     */
    public Vector subtract(Vector other) {
        if (this.getDimension() != other.getDimension()) {
            throw new IllegalArgumentException("Vector dimensions must match");
        }
        double[] result = new double[this.getDimension()];
        for (int i = 0; i < result.length; i++) {
            result[i] = this.values[i] - other.values[i];
        }
        return new Vector(result);
    }

    /**
     * Scales this vector by a scalar value.
     * Each component is multiplied by the scalar: result[i] = this[i] * scalar.
     * 
     * @param scalar The factor to scale by.
     * @return A new Vector representing the scaled vector.
     */
    public Vector scale(double scalar) {
        double[] result = new double[this.getDimension()];
        for (int i = 0; i < result.length; i++) {
            result[i] = this.values[i] * scalar;
        }
        return new Vector(result);
    }

    /**
     * Calculates the Euclidean magnitude (length) of the vector.
     * Formula: sqrt(sum(v[i]^2))
     * 
     * @return The magnitude of the vector.
     */
    public double magnitude() {
        double sum = 0;
        for (double val : values) {
            sum += val * val;
        }
        return Math.sqrt(sum);
    }

    /**
     * Returns a string representation of the vector.
     * 
     * @return A string representation of the vector values.
     */
    @Override
    public String toString() {
        return Arrays.toString(values);
    }
}
