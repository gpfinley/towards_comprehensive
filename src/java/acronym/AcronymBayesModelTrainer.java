package acronym;

import model.Document;
import model.Token;

import java.io.*;
import java.util.*;
import java.util.function.Function;

/**
 * Created by ANONYMOUS on 10/23/15.
 */
public class AcronymBayesModelTrainer implements AcronymModelTrainer {

    // These are loaded from text files
    // expansionMap works the same as in AcronymVectorModel
    // uniqueIdMap maps unique identifying strings of the acronym long forms (as appear in the preprocessed training
    // text file) to their English forms
    private final Map<String,List<String>> expansionMap;
    private final Map<String,String> uniqueIdMap;

    // Number of tokens to look back/ahead when calculating word vectors
    private final static double DEFAULT_MAX_DIST = 9;
    private double maxDist = DEFAULT_MAX_DIST;

    private VectorSpaceDouble vectorSpace = new VectorSpaceDouble(DEFAULT_MAX_DIST);
    private Map<String,WordVectorDouble> senseMapMean = new HashMap<>();
    private Map<String,WordVectorDouble> senseMapSd = new HashMap<>();

    private Map<String,List<WordVectorDouble>> senseMapAll = new HashMap<>();

    // How counts will be transformed (default is sqrt to flatten out the vectors a bit)
    private static Function<Double,Double> transformCounts = (x) -> x;

    /**
     * Initializes the acronym trainer. Needs paths to two text files:
     * @param expansionMapFile which maps short forms to long forms
     * @param uniqueIdMapFile which maps unique identifying strings to long forms
     * @return
     * @throws IOException
     */
    public AcronymBayesModelTrainer(String expansionMapFile, String uniqueIdMapFile) throws IOException {

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
     * This will finalize the vectors and put them all into a knew AcronymBayesModel, which can be used or serialized
     * @return
     */
    public AcronymBayesModel getModel() {
        vectorSpace.finishTraining();
        for(String sense : senseMapAll.keySet()) {
            Map<Integer,Double> meanVector = new HashMap<>();
            Map<Integer,Double> sdVector = new HashMap<>();

            int maxKey = vectorSpace.numWords();
            double n = senseMapAll.get(sense).size();

            for(int i=0; i<maxKey; i++) {
                double sum = 0;
                for(WordVectorDouble vec : senseMapAll.get(sense)) {
                    sum += vec.get(i);
                }
                if(sum != 0) {
                    double mean = sum / n;
                    meanVector.put(i, mean);
                    double sumDist = 0;
                    for (WordVectorDouble vec : senseMapAll.get(sense)) {
                        sumDist += Math.pow(mean - vec.get(i), 2);
                    }
                    sdVector.put(i, Math.sqrt(sumDist / n));
                }
            }
            senseMapMean.put(sense, new WordVectorDouble(meanVector));
            senseMapSd.put(sense, new WordVectorDouble(sdVector));
        }

        return new AcronymBayesModel(vectorSpace, senseMapMean, senseMapSd, expansionMap);
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

        // Now go through to every token of interest, calculate a vector for it, and add it to the vectors already
        // found for that sense.
        i = 0;
        for(Token tokenOfInterest : tokensOfInterest) {
            String senseEnglish = uniqueIdMap.get(tokenOfInterest.getText());
            int start = startPositions.get(i);
            int end = endPositions.get(i);
            if(start < 0) start = 0;
            if(end > allTokens.size()) end = allTokens.size() - 1;

            WordVectorDouble calculatedVector = vectorSpace.vectorize(allTokens.subList(start, end), tokenOfInterest);

            senseMapAll.putIfAbsent(senseEnglish, new ArrayList<>());
            senseMapAll.get(senseEnglish).add(calculatedVector);

            i++;
        }
    }

    /**
     * Counts will be transformed (after summing) by a function. The default is Math.sqrt
     * @param function a different function to use (takes and returns double)
     */
    public void setTransformFunction(Function<Double,Double> function) {
        transformCounts = function;
    }

}
