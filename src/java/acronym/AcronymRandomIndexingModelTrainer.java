package acronym;

import model.Document;
import model.Token;
import semanticindexing.RandomIndexingContext;
import semanticindexing.RandomIndexingVector;

import java.io.*;
import java.util.*;

/**
 * Created by ANONYMOUS on 10/30/15.
 */
public class AcronymRandomIndexingModelTrainer implements AcronymModelTrainer {


    // These are loaded from text files
    // expansionMap works the same as in AcronymVectorModel
    // uniqueIdMap maps unique identifying strings of the acronym long forms (as appear in the preprocessed training
    // text file) to their English forms
    private final Map<String,List<String>> expansionMap;
    private final Map<String,String> uniqueIdMap;

    // Number of tokens to look back/ahead when calculating word vectors
    private final static int DEFAULT_MAX_DIST = 9;
    private int maxDist;

    private final int dimensionality = 1800;
    private final int nonZeroValues = 4;

    private VectorSpaceDouble vectorSpace;
    private Map<String,List<WordVectorDouble>> senseMap = new HashMap<>();

    /**
     * Initializes the acronym trainer. Needs paths to two text files:
     * @param expansionMapFile which maps short forms to long forms
     * @param uniqueIdMapFile which maps unique identifying strings to long forms
     * @return
     * @throws IOException
     */
    public AcronymRandomIndexingModelTrainer(String expansionMapFile, String uniqueIdMapFile) throws IOException {

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

        uniqueIdMap = uniqueIdMapBuilder;
        expansionMap = expansionMapBuilder;
        maxDist = DEFAULT_MAX_DIST;
        vectorSpace = new VectorSpaceDouble(maxDist);
//        model = new AcronymRandomIndexingModel(expansionMapBuilder);
    }

    /**
     * Adds a tokenized document to the model, which should have been initialized already
     * @param document
     */
    public void addDocumentToModel(Document document) {

        // Maximum number of words to look at (needn't look much farther than maxDist)
        int maxSize = (int) (maxDist * 1.5);

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
                startPositions.add(i - maxSize);
                endPositions.add(i + maxSize + 1);
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

            WordVectorDouble calculatedVector = vectorSpace.vectorize(allTokens.subList(start, end), tokenOfInterest);

            senseMap.putIfAbsent(senseEnglish, new ArrayList<>());
            senseMap.get(senseEnglish).add(calculatedVector);

            i++;
        }
    }

    public AcronymRandomIndexingModel getModel() {
        vectorSpace.finishTraining();

        List<RandomIndexingContext> dictionary = new ArrayList<>();

        for(int i=0; i<vectorSpace.numWords(); i++) {
            dictionary.add(new RandomIndexingContext(dimensionality, nonZeroValues));
        }

        Map<String,RandomIndexingVector> senseVecs = new HashMap<>();

        for (String acronym : expansionMap.keySet()) {

            // determine which senses we actually saw, and count the total number of training examples
            List<String> presentSenses = new ArrayList<>();
            int totalExamples = 0;
            for(String sense : expansionMap.get(acronym)) {
                if(senseMap.containsKey(sense)) {
                    presentSenses.add(sense);
                    totalExamples += senseMap.get(sense).size();
                }
            }
            expansionMap.put(acronym, presentSenses);

            if(expansionMap.get(acronym).size() > 1) {

                for(String sense : expansionMap.get(acronym)) {

                    RandomIndexingVector riv = new RandomIndexingVector();

                    for (DoubleVector wordVec : senseMap.get(sense)) {

                        for (int key : wordVec.getKeySet()) {
                            double weight = wordVec.get(key) * vectorSpace.getIdf().get(key);
                            riv.addContext(dictionary.get(key), weight);
                        }

                    }

                    senseVecs.put(sense, riv);
                }
            }
        }

        // norm the summed vectors or heavy weight will be given to more common terms
        for(RandomIndexingVector v : senseVecs.values()) {
            v.normVector();
        }

        return new AcronymRandomIndexingModel(vectorSpace, dictionary, senseVecs, expansionMap);
    }


}
