package semanticindexing;

import java.io.Serializable;
import java.util.*;

/**
 * A single word/document context in a random indexing model. Immutable after calling constructor.
 * Takes two arguments: dimensionality and number of 1s/-1s (# of nonzero elements / 2)
 * Current implementation made to be super memory efficient (although shorts would make it even more efficient!)
 *
 * Created by ANONYMOUS on 10/30/15.
 */
public class RandomIndexingContext implements Serializable {

    private final int nonzero[];
    private final int n;

    public RandomIndexingContext(int d, int n) {

        Random random = new Random();

        this.n = n;
        nonzero = new int[n*2];

        Set<Integer> indices = new HashSet<>();
        int i = 0;
        while(indices.size() < n*2) {
            int nextInt = random.nextInt(d);
            if (indices.add(nextInt)) {
                nonzero[i] = nextInt;
                i++;
            }
        }
    }

    public int[] getOnes() {
        return Arrays.copyOfRange(nonzero, 0, n);
    }
    public int[] getNegOnes() {
        return Arrays.copyOfRange(nonzero, n, n*2);
    }

    public String toString() {
        String s = "";
        for(int i=0; i<n*2; i++) {
            s += nonzero[i] + " ";
        }
        return s;
    }
}
