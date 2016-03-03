package semanticindexing;

import java.io.Serializable;
import java.util.*;

/**
 * Vector as used by random indexing
 * Has the capabilities to add RandomIndexingContexts
 *
 * Created by ANONYMOUS on 10/30/15.
 */
public class RandomIndexingVector implements Serializable {

    // Using Double in case we want normalization (not simple thresholding)
    private Map<Integer,Float> vector;

    public RandomIndexingVector() {
        vector = new HashMap<>();
    }

    public RandomIndexingVector(Map<Integer,Float> map) {
        vector = map;
    }

    public String toString() {
        return vector.toString();
    }

    public double length() {
        double sqsum = 0;
        for (double x : vector.values()) {
            sqsum += Math.pow(x, 2);
        }
        return Math.pow(sqsum, 0.5);
    }

    // Normalize this vector
    public void normVector() {
        double mag = length();
        for (Map.Entry<Integer,Float> e : vector.entrySet()) {
            e.setValue((float)(e.getValue() / mag));
        }
    }

    /**
     * Return a new vector, with all positive numbers set to 1, all negative numbers to -1
     */
    public RandomIndexingVector thresholdAtOne() {
        Map<Integer,Float> thresholded = new HashMap<>(vector);
        for(Map.Entry<Integer,Float> e : vector.entrySet()) {
            if(e.getValue() > 0) {
                thresholded.put(e.getKey(), (float)1);
            }
            else if(e.getValue() < 0) {
                thresholded.put(e.getKey(), (float)-1);
            }
        }
        return new RandomIndexingVector(thresholded);
    }

    /**
     * Add a random indexing document/word context
     * @param context
     */
    public void addContext(RandomIndexingContext context) {
        addContext(context, 1);
    }

    /**
     * Add a random indexing document/word context
     * @param context
     * @param weight
     */
    public void addContext(RandomIndexingContext context, double weight) {
        float w = (float) weight;
        int[] ones = context.getOnes();
        int[] negOnes = context.getNegOnes();
        for(int i = 0; i<ones.length; i++) {
            vector.putIfAbsent(ones[i], (float)0);
            vector.putIfAbsent(negOnes[i], (float)0);
            vector.compute(ones[i], (index, val) -> val+w);
            vector.compute(negOnes[i], (index, val) -> val-w);
        }
    }

    // will add another vector onto this one
    public void add(RandomIndexingVector v) {
        for (int i : v.getKeySet()) {
            vector.put(i, (float) (this.get(i) + v.get(i)));
        }
    }

    /**
     * Return the dot product of this vector with another
     * @param v another vector
     * @return their dot product
     */
    public double dot(RandomIndexingVector v) {
        double sum = 0;
        for (int i : vector.keySet()) {
            sum += vector.get(i) * v.get(i);
        }
        return sum;
    }

    public double cosine(RandomIndexingVector v) {
        return dot(v) / (length() * v.length());
    }

    /**
     * Getters: get the key set, or get an element
     */

    public Set<Integer> getKeySet() {
        return new HashSet<>(vector.keySet());
    }

    public double get(int i) {
        if (vector.containsKey(i))
            return (double) vector.get(i);
        else
            return 0;
    }

}
