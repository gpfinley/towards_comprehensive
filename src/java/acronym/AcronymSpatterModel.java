package acronym;

import model.Token;
import semanticindexing.BinarySpatterVectorBit;

import java.io.*;
import java.util.*;
import java.util.zip.GZIPOutputStream;

/**
 * Created by ANONYMOUS on 10/23/15.
 */
public class AcronymSpatterModel implements Serializable, AcronymModel {

    private final int DIMENSIONALITY = 10000;
    // May be different from the value in the trainer!
    private final int MAX_DIST = 9;

    private final Map<String,List<String>> expansionMap;
    private final Map<String,String> reverseExpansionMap;

    private final List<String> senses;

    private final Map<String,BinarySpatterVectorBit> acronymVectors;
    private final List<BinarySpatterVectorBit> senseVectors;
    private final List<BinarySpatterVectorBit> acrSenseVectors;

    private final List<int[]> acronymsWithTerm;

    private final Map<String,Integer> dictionary;

    private final double[] weights;

    private BinarySpatterVectorBit[] wordVectors = null;

    public AcronymSpatterModel(Map<String, List<String>> expansionMap, List<String> senses, List<int[]> acronymsWithTerm, Map<String,Integer> dictionary, double[] weights) {
        this.expansionMap = expansionMap;
        this.senses = senses;
        this.acronymsWithTerm = acronymsWithTerm;
        this.dictionary = dictionary;
        this.weights = weights;

        acronymVectors = new HashMap<>();
        senseVectors = new ArrayList<>();
        acrSenseVectors = new ArrayList<>();

        // Build a quick map to look up acronyms by sense, which we'll use a lot
        reverseExpansionMap = new HashMap<>();
        for(String acronym : expansionMap.keySet()) {
            for(String sense : expansionMap.get(acronym)) {
                reverseExpansionMap.put(sense, acronym);
            }
        }

        // Create a binary spatter vector for each acronym and put them into a map
        for(String acronym : expansionMap.keySet()) {
            acronymVectors.put(acronym, new BinarySpatterVectorBit(DIMENSIONALITY));
        }
        // Create binary spatter vectors for senses. These are in a list since senses have integer identifiers
        for(String sense : senses) {
            BinarySpatterVectorBit senseBsv = new BinarySpatterVectorBit(DIMENSIONALITY);
            senseVectors.add(senseBsv);
            acrSenseVectors.add(senseBsv.xor(acronymVectors.get(reverseExpansionMap.get(sense))));
        }

        // Build BSVs for every word in the corpus
        // If this code is removed, then redundant code will build these when attempting to classify acronyms
        wordVectors = new BinarySpatterVectorBit[dictionary.size()];
        for (String word : dictionary.keySet()) {
            BinarySpatterVectorBit wordVector = new BinarySpatterVectorBit(DIMENSIONALITY);

            int wordInt = dictionary.get(word);

            int[] cooccurringSenses = acronymsWithTerm.get(wordInt);
            for (int i = 0; i < cooccurringSenses.length; i++) {
                wordVector.add(acrSenseVectors.get(i), cooccurringSenses[i]);
            }
            wordVector.updateToVotingRecord();
            wordVector.forgetVotingRecord();
            wordVectors[wordInt] = wordVector;
        }

    }

    public List<String> getExpansions(Token token) {
        String acronym = AcronymModel.standardForm(token);
        return expansionMap.get(acronym);
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

    @Override
    public String findBestSense(List<Token> context, Token token) {

        if(wordVectors == null) {
            wordVectors = new BinarySpatterVectorBit[dictionary.size()];
            for (String word : dictionary.keySet()) {
                BinarySpatterVectorBit wordVector = new BinarySpatterVectorBit(DIMENSIONALITY);

                int wordInt = dictionary.get(word);

                int[] cooccurringSenses = acronymsWithTerm.get(wordInt);
                for (int i = 0; i < cooccurringSenses.length; i++) {
                    wordVector.add(acrSenseVectors.get(i), cooccurringSenses[i]);
                }
                wordVector.updateToVotingRecord();
                wordVector.forgetVotingRecord();
                wordVectors[wordInt] = wordVector;
            }
        }

        // Return an empty string if we don't know this acronym
        String acronym = AcronymModel.standardForm(token);
        if(!expansionMap.containsKey(acronym)) return "";

        // If there's only one sense, return that
        if(expansionMap.get(acronym).size() == 1) {
            return expansionMap.get(acronym).get(0);
        }

        BinarySpatterVectorBit contextVector = new BinarySpatterVectorBit(DIMENSIONALITY);

        int best = DIMENSIONALITY;
        String winner = "acronym_expander_not_working";

        assert context.contains(token);

        int start = -1;
        int end = -1;

        // To determine our position in the word list. Useful when calculating distance from center
        int i = 0;

        for(Token t : context) {
        // Determine if we've hit the token of interest yet
            if (t == token) {
                start = i - MAX_DIST;
                end = i + MAX_DIST;
                break;
            }
            i++;
        }

        if(start < 0) start = 0;
        if(end >= context.size()) end = context.size()-1;

        for(Token t : context.subList(start, end)) {

            String word = AcronymModel.standardForm(t);

            if(dictionary.containsKey(word) && t != token) {
                int wordInt = dictionary.get(word);
                BinarySpatterVectorBit wordVector = wordVectors[wordInt];

                contextVector.add(wordVector, weights[wordInt]);
            }
        }
        contextVector.updateToVotingRecord();
        contextVector.forgetVotingRecord();
        contextVector = contextVector.xor(acronymVectors.get(acronym));
        for(String sense : expansionMap.get(acronym)) {
            int dist = contextVector.hamming(senseVectors.get(senses.indexOf(sense)));
            if(dist < best) {
                best = dist;
                winner = sense;
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
