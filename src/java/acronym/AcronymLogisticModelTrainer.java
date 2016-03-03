package acronym;

import model.Document;
import model.Token;

import java.io.*;
import java.util.*;

/**
 * Created by ANONYMOUS on 10/23/15.
 */
public class AcronymLogisticModelTrainer implements AcronymModelTrainer {

    // These are loaded from text files
    // expansionMap works the same as in AcronymVectorModel
    // uniqueIdMap maps unique identifying strings of the acronym long forms (as appear in the preprocessed training
    // text file) to their English forms
    private final Map<String,List<String>> expansionMap;
    private final Map<String,String> uniqueIdMap;

    // Number of tokens to look back/ahead when calculating word vectors
    private final static double DEFAULT_MAX_DIST = 9;
    private double maxDist = DEFAULT_MAX_DIST;

    // logistic, svm, etc.
    private final String MODEL_TYPE = "logistic";

    // How many iterations of gradient descent to go through and the learning rate to use
    // Because these are very high-dimensional (tens of thousands) and low-valued (<1) vectors, learning rate is large
    private int numReps = 100;
    private int learningRate = 10;

    private VectorSpaceDouble vectorSpace = new VectorSpaceDouble(maxDist);
    private Map<String,List<WordVectorDouble>> senseMap = new HashMap<>();

    private Map<String,GradientDescentModel> models = new HashMap<>();

    public AcronymLogisticModelTrainer(String expansionMapFile, String uniqueIdMapFile) throws IOException {
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
    }

    /**
     * Call this after all documents have been added using addDocumentToModel(Document)
     * This will finalize the vectors and put them all into a new AcronymModel, which can be used or serialized
     * @return
     */
    public AcronymSvmModel getModel() {
        vectorSpace.finishTraining();

        System.out.println(expansionMap.size() + " acronyms total");

        for (List<WordVectorDouble> vectorList : senseMap.values()) {
            for (WordVectorDouble vector : vectorList) {
                vector.normVector();
            }
        }

        for (String acronym : expansionMap.keySet()) {

            // determine which senses we actually saw, and count the total number of training examples
            List<String> presentSenses = new ArrayList<>();
            for(String sense : expansionMap.get(acronym)) {
                if(senseMap.containsKey(sense)) {
                    presentSenses.add(sense);
                }
            }
            expansionMap.put(acronym, presentSenses);

            if(expansionMap.get(acronym).size() > 1) {

                models.put(acronym, new GradientDescentModel(vectorSpace.numWords(), expansionMap.get(acronym).size(), MODEL_TYPE));
                models.get(acronym).setLearningRate(learningRate);

                // Collect all examples of this sense into a list of lists and have the model train them all
                List<List<DoubleVector>> allVectors = new ArrayList<>();
                for(String sense : expansionMap.get(acronym)) {
                    List<DoubleVector> thisSense = new ArrayList<>();
                    for(WordVectorDouble v : senseMap.get(sense)) {
                        thisSense.add(v);
                    }
                    allVectors.add(thisSense);
                }
                models.get(acronym).trainAll(allVectors, numReps);

            }
        }
        return new AcronymSvmModel(vectorSpace, expansionMap, models);
    }

    /**
     * Adds a tokenized document to the model, which should have been initialized already
     * @param document
     */
    public void addDocumentToModel(Document document) {

        // Maximum number of words to look at (needn't look much farther than maxDist, probably)
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

}
