package acronym;

import model.Token;

import java.io.*;
import java.util.*;
import java.util.zip.GZIPOutputStream;

/**
 * An implementation of an acronym model that uses word vectors and a cosine distance metric
 *
 * Created by ANONYMOUS on 10/23/15.
 */
public class AcronymVectorModel implements Serializable, AcronymModel {

    // A vector space with a built dictionary to use at test time
    private final VectorSpaceDouble vectorSpaceDouble;

    // A map between acronyms and all their possible long forms
    private final Map<String,List<String>> expansionMap;

    // Maps long forms to their trained word vectors
    private final Map<String,DoubleVector> senseMap;

    /**
     * Constructor. Needs several things already made:
     * @param vectorSpaceDouble the vector space (most importantly dictionary) used to build context vectors
     * @param senseMap which maps between senses and their context vectors
     * @param expansionMap which maps between acronym Strings and Lists of their possible senses
     */
    public AcronymVectorModel(VectorSpaceDouble vectorSpaceDouble, Map<String, DoubleVector> senseMap, Map<String, List<String>> expansionMap) {
        this.expansionMap = expansionMap;
        this.senseMap = senseMap;
        this.vectorSpaceDouble = vectorSpaceDouble;
    }

    /**
     * Will return a list of the possible senses for this acronym
     * @param token a Token
     * @return a List of Strings of all possible senses
     */
    public List<String> getExpansions(Token token) {
        String acronym = AcronymModel.standardForm(token);
        if(expansionMap.containsKey(acronym))
            return new ArrayList<>(expansionMap.get(acronym));
        return new ArrayList<>();
    }

    /**
     * Does the model know about this acronym?
     * @param token
     * @return
     */
    public boolean hasAcronym(Token token) {
        String acronym = AcronymModel.standardForm(token);
        if(expansionMap.containsKey(acronym)) {
            return true;
        }
        return false;
    }

    /**
     * Will return the model's best guess for the sense of this acronym
     * @param context a list of tokens providing context for this acronym
     * @param token the token of the acronym itself
     * @return
     */
    @Override
    public String findBestSense(List<Token> context, Token token) {

        // String to assign to unknown acronyms
        final String UNK = "(unknown)";

        String acronym = AcronymModel.standardForm(token);

        // If the model doesn't contain this acronym, make sure it doesn't contain an upper-case version of it
        if(!expansionMap.containsKey(acronym))
            return UNK;

        double best = -Double.MAX_VALUE;
        String winner = UNK;

        DoubleVector vector = vectorSpaceDouble.vectorize(context, token);
        vector.multiply(vectorSpaceDouble.getIdf());

        // Loop through all possible senses for this acronym
        for(String sense : expansionMap.get(acronym)) {
            if(senseMap.containsKey(sense)) {
                DoubleVector compVec = senseMap.get(sense);
                double score = vector.dot(compVec);
                if (score > best) {
                    best = score;
                    winner = sense;
                }
            }
        }
        return winner;
    }

    /**
     * Write this object to a file
     * @param filename the name of the output file (*.ser)
     * @throws IOException
     */
    public void serialize(String filename) throws IOException {
        FileOutputStream fileOutputStream = new FileOutputStream(filename);
        GZIPOutputStream gzipOutputStream = new GZIPOutputStream(fileOutputStream);
        ObjectOutputStream objectOutputStream = new ObjectOutputStream(gzipOutputStream);
        objectOutputStream.writeObject(this);
        gzipOutputStream.flush();
        gzipOutputStream.close();
    }

}