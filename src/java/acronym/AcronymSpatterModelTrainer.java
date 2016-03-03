package acronym;

import model.Document;
import model.Token;

import java.io.*;
import java.util.*;

/**
 * Created by ANONYMOUS on 10/23/15.
 */
public class AcronymSpatterModelTrainer implements AcronymModelTrainer {

    private final int MAX_DIST = 9;

    // These are loaded from text files
    // expansionMap works the same as in AcronymVectorModel
    // uniqueIdMap maps unique identifying strings of the acronym long forms (as appear in the preprocessed training
    // text file) to their English forms
    private final Map<String,List<String>> expansionMap;
    private final Map<String,String> uniqueIdMap;
    private final Map<String,String> reverseExpansionMap;

    private List<String> senses = new ArrayList<>();

    private int nDocs = 0;

    private List<int[]> acronymsWithTerm = new ArrayList<>();

    private Map<String,Integer> dictionary = new HashMap<>();

    // Will count up how many of each word occurs in each document so that doc freqs can be calculated
    private List<Map<Integer,Integer>> wordsByDocument = new ArrayList<>();
    // Total counts of every word
    private List<Integer> globalFreqs = new ArrayList<>();

    /**
     * Initializes the acronym trainer. Needs paths to two text files:
     * @param expansionMapFile which maps short forms to long forms
     * @param uniqueIdMapFile which maps unique identifying strings to long forms
     * @return
     * @throws IOException
     */
    public AcronymSpatterModelTrainer(String expansionMapFile, String uniqueIdMapFile) throws IOException {
        Map<String,List<String>> expansionMapBuilder = new HashMap<>();
        Map<String,String> uniqueIdMapBuilder = new HashMap<>();

        InputStream inputStream = new FileInputStream(expansionMapFile);
        BufferedReader fileReader = new BufferedReader(new InputStreamReader(inputStream));
        String nextLine;
        while ((nextLine = fileReader.readLine()) != null) {
            List<String> fields = Arrays.asList(nextLine.split("\\|"));
            if(fields.size() >= 1) {
                String acronym = AcronymModel.standardFormString(fields.get(0));
                List<String> senses = new ArrayList<>(fields.subList(1,fields.size()));
                expansionMapBuilder.put(acronym, senses);
            }
        }

        InputStream inputStreamB = new FileInputStream(uniqueIdMapFile);
        BufferedReader fileReaderB = new BufferedReader(new InputStreamReader(inputStreamB));
        while ((nextLine = fileReaderB.readLine()) != null) {
            String[] fields = nextLine.split("\\|");
            uniqueIdMapBuilder.put(fields[0], fields[1]);
        }

        expansionMap = expansionMapBuilder;
        uniqueIdMap = uniqueIdMapBuilder;
        reverseExpansionMap = new HashMap<>();

        // Build up the maps between acrs/senses and integer indices, and build up the spatter vectors for all acrs, senses, and combos
        for(String acronym : expansionMap.keySet()) {
            for(String sense : expansionMap.get(acronym)) {
                if(!senses.contains(sense))
                    senses.add(sense);
                reverseExpansionMap.put(sense,acronym);
            }
        }
    }

    /**
     * Call this after all documents have been added using addDocumentToModel(Document)
     * This will finalize the vectors and put them all into a knew AcronymVectorModel, which can be used or serialized
     * @return
     */
    public AcronymSpatterModel getModel() {

        double[] entropyWeight = new double[dictionary.size()];

        for(Map<Integer,Integer> wordsThisDocument : wordsByDocument) {
            for(Map.Entry<Integer,Integer> e : wordsThisDocument.entrySet()) {
                double p = ((double)e.getValue()) / globalFreqs.get(e.getKey());
                if(p != 0) {
                    entropyWeight[e.getKey()] += p * Math.log(p);
                }
            }
        }

        for(int i=0; i<entropyWeight.length; i++) {
            entropyWeight[i] /= Math.log(nDocs);
            entropyWeight[i] += 1.0;
        }

        return new AcronymSpatterModel(expansionMap, senses, acronymsWithTerm, dictionary, entropyWeight);

    }

    /**
     * Adds a tokenized document to the model, which should have been initialized already
     * @param document
     */
    public void addDocumentToModel(Document document) {

        // These lists will all be the same length
        // Tokens that are unique IDs, which will be used for training
        List<Token> tokensOfInterest = new ArrayList<>();
        // Chunks where our tokens of interest will be
        List<Integer> startPositions = new ArrayList<>();
        List<Integer> endPositions = new ArrayList<>();

        List<Token> allTokens = new ArrayList<>();

        // Our position in the document, for building the lists
        int i = 0;

        // Find tokens of interest in the document and their context; populate the above lists
        for(Token token : document.getTokens()) {
            allTokens.add(token);
            if(uniqueIdMap.containsKey(token.getText())) {
                tokensOfInterest.add(token);
                startPositions.add(i - MAX_DIST);
                endPositions.add(i + MAX_DIST + 1);
            }
            i++;
        }

        i = 0;
        for(Token tokenOfInterest : tokensOfInterest) {
            String senseEnglish = uniqueIdMap.get(tokenOfInterest.getText());
            int start = startPositions.get(i);
            int end = endPositions.get(i);
            if(start < 0) start = 0;
            if(end > allTokens.size()) end = allTokens.size() - 1;

            wordsByDocument.add(new HashMap<>());
            nDocs++;

            for(Token contextToken : allTokens.subList(start, end)) {
                String word = AcronymModel.standardForm(contextToken);
                if (!dictionary.containsKey(word)) {
                    dictionary.put(word, dictionary.size());
                    globalFreqs.add(0);
                    acronymsWithTerm.add(new int[senses.size()]);
                }
                int wordInt = dictionary.get(word);
                globalFreqs.set(wordInt, globalFreqs.get(wordInt)+1);
                wordsByDocument.get(wordsByDocument.size() - 1).putIfAbsent(wordInt, 0);
                wordsByDocument.get(wordsByDocument.size() - 1).compute(wordInt, (x,y) -> y+1);

                // Increment the counter for this specific term appearing with this specific sense
                // might be slow (use hashmap instead of ArrayList.indexOf?)
                int senseIndex = senses.indexOf(senseEnglish);
                if(senseIndex < senses.size() && senseIndex >= 0) {
                    acronymsWithTerm.get(wordInt)[senseIndex]++;
                }

            }

            i++;
        }
    }
}
