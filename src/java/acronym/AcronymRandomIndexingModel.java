package acronym;

import model.Token;
import semanticindexing.RandomIndexingContext;
import semanticindexing.RandomIndexingVector;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.*;
import java.util.zip.GZIPOutputStream;

/**
 * Created by ANONYMOUS on 10/30/15.
 */
public class AcronymRandomIndexingModel implements AcronymModel, Serializable {

    private final Map<String,List<String>> acronymExpansions;

    // Maps senses to vectors
    private final Map<String,RandomIndexingVector> senseMap;

    // Maps words to RI contexts (large set, small data)
    private final List<RandomIndexingContext> dictionary;

    private final VectorSpaceDouble vectorSpace;

    public AcronymRandomIndexingModel(VectorSpaceDouble vectorSpace, List<RandomIndexingContext> dictionary, Map<String,RandomIndexingVector> senseMap, Map<String, List<String>> acronymExpansions) {
        this.acronymExpansions = acronymExpansions;
        this.senseMap = senseMap;
        this.vectorSpace = vectorSpace;
        this.dictionary = dictionary;
    }

    public String standardForm(Token token) {
        String form = token.getText();
        return form.toLowerCase();
    }

    @Override
    public boolean hasAcronym(Token token) {
        if(acronymExpansions.containsKey(standardForm(token)))
            return true;
        return false;
    }

    @Override
    public String findBestSense(List<Token> context, Token token) {

        String acronym = AcronymModel.standardForm(token);
        if(!acronymExpansions.containsKey(acronym)) return "";

        if(acronymExpansions.get(acronym).size() == 1) {
            return acronymExpansions.get(acronym).get(0);
        }

        double best = -Double.MAX_VALUE;
        String winner = "acronym_expander_not_working";

//        RandomIndexingVector vector = vectorize(context, token).thresholdAtOne();
        RandomIndexingVector vector = vectorize(context, token);

        // Loop through all possible senses for this acronym
        for(String sense : acronymExpansions.get(acronym)) {
            if(senseMap.containsKey(sense)) {
//                RandomIndexingVector compVec = senseMap.get(sense).thresholdAtOne();
                RandomIndexingVector compVec = senseMap.get(sense);
                double score = vector.dot(compVec);

                if (score > best) {
                    best = score;
                    winner = sense;
                }
            }
        }
        return winner;
    }

    private RandomIndexingVector vectorize(List<Token> context, Token tokenOfInterest) {

        assert context.contains(tokenOfInterest);

        DoubleVector wordVec = vectorSpace.vectorize(context, tokenOfInterest);

        return wordToRiVec(wordVec);
    }

    private RandomIndexingVector wordToRiVec(DoubleVector wordVec) {

        RandomIndexingVector riv = new RandomIndexingVector();

        for(int key : wordVec.getKeySet()) {
            double weight = wordVec.get(key) * vectorSpace.getIdf().get(key);
            if(key >= 0 && key < dictionary.size()) {
                riv.addContext(dictionary.get(key), weight);
            }
        }

        return riv;
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
