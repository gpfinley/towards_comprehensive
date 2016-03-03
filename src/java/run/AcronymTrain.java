package run;

import acronym.*;
import com.ibm.icu.text.BreakIterator;
import com.ibm.icu.text.RuleBasedBreakIterator;
import model.Document;

import java.io.*;
import java.util.Arrays;
import java.util.Scanner;

public class AcronymTrain {

    private static BreakIterator tokenizer;
    private static final String TOKENIZER_PARAMS_FILE = "/tokenizer_break_rules.txt";

    public static void main(String[] args) throws Exception {

        if(args[0].equals("test")) {
            AcronymTest.main(args);
            System.exit(0);
        }

        if (args.length < 5) {
            System.out.println("Must provide five arguments for training: model type, output file, training data file, acronym expansions file, sense IDs file");
            throw new Exception();
        }
        String dataFile = args[2];
        String modelType = args[0].toLowerCase();
        String modelPath = args[1];
        String expansionMap = args[3];
        String uniqueIds = args[4];

        initializeTokenizer();

        AcronymModelTrainer trainer;
        if (modelType.equals("cos")) {
            trainer = new AcronymVectorModelTrainer(expansionMap, uniqueIds);
        } else if (modelType.equals("svm")) {
            trainer = new AcronymSvmModelTrainer(expansionMap, uniqueIds);
        } else if (modelType.equals("lr")) {
            trainer = new AcronymLogisticModelTrainer(expansionMap, uniqueIds);
        } else if (modelType.equals("nb")) {
            trainer = new AcronymBayesModelTrainer(expansionMap, uniqueIds);
        } else if (modelType.equals("ri")) {
            trainer = new AcronymRandomIndexingModelTrainer(expansionMap, uniqueIds);
        } else if (modelType.equals("bsc")) {
            trainer = new AcronymSpatterModelTrainer(expansionMap, uniqueIds);
        } else {
            System.out.println("Invalid model type. Must use one of: nb, lr, svm, cos, ri, bsc");
            throw new Exception();
        }

        System.out.println("Training a model for classifier " + modelType + " for data at " + dataFile);
        trainModelOnFile(trainer, dataFile);

        trainer.getModel().serialize(modelPath);
    }

    public static void trainModelOnFile(AcronymModelTrainer trainer, String dataFile) throws IOException {
        BufferedReader fileReader = new BufferedReader(new FileReader(dataFile));
        String line;
        while((line = fileReader.readLine()) != null) {
            if(line.length() > 2) {
                Document doc = new Document(line, tokenizer);
                trainer.addDocumentToModel(doc);
            }
        }
    }

    public static void initializeTokenizer() throws IOException {
        String rules = new Scanner( AcronymTrain.class.getResourceAsStream(TOKENIZER_PARAMS_FILE) ).useDelimiter("\\A").next();
        tokenizer = new RuleBasedBreakIterator(rules);
    }
}
