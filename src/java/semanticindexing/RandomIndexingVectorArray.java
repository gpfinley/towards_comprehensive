package semanticindexing;

import java.io.Serializable;
import java.util.*;

/**
 * An array (not HashMap) flavor of RandomIndexingVector
 *
 * Created by ANONYMOUS on 10/30/15.
 */
public class RandomIndexingVectorArray implements Serializable {

    private final int SIZE = 1800;

    // Using Double in case we want normalization (not simple thresholding)
    private float[] vector;

    public RandomIndexingVectorArray() {
        vector = new float[SIZE];
    }

    public RandomIndexingVectorArray(Map<Integer, Float> map) {
        for(Map.Entry<Integer,Float> e : map.entrySet()) {
            vector[e.getKey()] = e.getValue();
        }
    }

    public RandomIndexingVectorArray(float[] vector) {
        this.vector = vector;
    }

    public double sum() {
        double sum = 0;
        for(int i=0; i<SIZE; i++) {
            sum += vector[i];
        }
        return sum;
    }

    public String toString() {
        return Arrays.toString(vector);
    }

    public double length() {
        double sqsum = 0;
        for(int i=0; i<SIZE; i++) {
            sqsum += Math.pow(vector[i], 2);
        }
        return Math.pow(sqsum, 0.5);
    }

    // Normalize this vector
    public void normVector() {
        double mag = length();
        if(mag == 0) return;
        for(int i=0; i<SIZE; i++) {
            vector[i] /= mag;
        }
    }

    /**
     * Return a new vector, with all positive numbers set to 1, all negative numbers to -1
     */
    public RandomIndexingVectorArray thresholdAtOne() {
        float[] thresholded = Arrays.copyOf(vector, SIZE);
        for(int i=0; i<SIZE; i++) {
            if(vector[i] < 0) vector[i] = (float)-1;
            if(vector[i] > 0) vector[i] = (float)1;
        }
        return new RandomIndexingVectorArray(thresholded);
    }

    /**
     * Add a random indexing document/word context
     * @param context
     */
    public void addContext(RandomIndexingContext context) {
        addContext(context, 1.0);
    }

    public void addContext(RandomIndexingContext context, double weight) {
        int[] ones = context.getOnes();
        int[] negOnes = context.getNegOnes();
        for(int i = 0; i<ones.length; i++) {
            vector[ones[i]] += weight;
            vector[negOnes[i]] -= weight;
        }
    }

    // will add another vector onto this one
    public void add(RandomIndexingVectorArray v) {
        for(int i=0; i<SIZE; i++) {
            vector[i] += v.get(i);
        }
    }

    public void multiply(RandomIndexingVectorArray v) {
        for(int i=0; i<SIZE; i++) {
            vector[i] *= v.get(i);
        }
    }

    /**
     * Return the dot product of this vector with another
     * @param v another vector
     * @return their dot product
     */
    public double dot(RandomIndexingVectorArray v) {
        double sum = 0;
        for(int i=0; i<SIZE; i++) {
            sum += vector[i] * v.get(i);
        }
        return sum;
    }

    public double cosine(RandomIndexingVectorArray v) {
        return dot(v) / (length() * v.length());
    }

    /**
     * Getters: get the key set, or get an element
     */

    public double get(int i) {
        return vector[i];
    }

}
