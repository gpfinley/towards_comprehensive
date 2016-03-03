package semanticindexing;

import java.io.Serializable;
import java.util.BitSet;
import java.util.Random;

/**
 * Binary spatter vector, using BitSet for memory efficiency (rather than boolean, which takes one byte per entry)
 *
 * Created by ANONYMOUS on 11/20/15.
 */
public class BinarySpatterVectorBit implements Serializable {

    private BitSet vector;

    // Use for sums. Will stay unset for non-sum vectors
    private double[] votingRecord = null;

    /**
     * Constructor for creating a new randomized vector.
     * @param d dimensionality of the vector (defaults to 10000)
     */
    public BinarySpatterVectorBit(int d) {
        Random random = new Random();
        vector = new BitSet(d);
        for(int i=0; i<d; i++) {
            if(random.nextBoolean())
                vector.flip(i);
        }
    }
    public BinarySpatterVectorBit() {
        this(10000);
    }

    public BinarySpatterVectorBit(BitSet b) {
        vector = (BitSet) b.clone();
    }

    // Generate a vector from an array of double, with negative -> 0 and positive -> 1
    public BinarySpatterVectorBit(double[] v) {
        Random random = new Random();
        vector = new BitSet(v.length);
        for(int i=0; i<v.length; i++) {
            if(v[i] == 0 && random.nextBoolean())
                    vector.flip(i);
            else if(v[i] > 0)
                vector.flip(i);
        }
    }

    public boolean get(int i) {
        return vector.get(i);
    }

    public int length() {
        return vector.size();
    }

    // Will add another vector to this one's voting record
    // WARNING: will not update the voting record
    public void add(BinarySpatterVectorBit v, double weight) {
        if(votingRecord == null) {
            votingRecord = new double[vector.size()];
        }

        for(int i=0; i<vector.size(); i++) {
            if(v.get(i)) {
                votingRecord[i] += weight;
            }
            else {
                votingRecord[i] -= weight;
            }
        }
    }

    public void updateToVotingRecord() {
        vector = new BitSet(vector.size());
        for(int i=0; i<vector.size(); i++) {
            vector.set(i, votingRecord[i] > 0);
        }
    }

    // To save memory if the voting record is no longer important
    public void forgetVotingRecord() {
        votingRecord = null;
    }

    // Returns the hamming distance between this vector and another
    public int hamming(BinarySpatterVectorBit v) {
        BitSet differentBits = (BitSet) vector.clone();
        differentBits.xor(v.getBitSet());
        return differentBits.cardinality();
    }

    public BitSet getBitSet() {
        return vector;
    }

    // Normalized hamming dist
    public double hammingNormal(BinarySpatterVectorBit v) {
        return (double) hamming(v) / v.length();
    }

    public BinarySpatterVectorBit xor(BinarySpatterVectorBit v) {
        assert v.length() == vector.size();

        BitSet xored = (BitSet) vector.clone();
        xored.xor(v.getBitSet());

        return new BinarySpatterVectorBit(xored);
    }

}
