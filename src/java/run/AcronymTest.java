package run;

import java.io.*;
import java.util.*;

import com.ibm.icu.text.BreakIterator;
import com.ibm.icu.text.RuleBasedBreakIterator;
import model.AcronymDatum;
import acronym.AcronymModel;

/**
 * Created by ANONYMOUS on 1/4/16.
 */
public class AcronymTest {

    private static BreakIterator tokenizer;
    private static final String TOKENIZER_PARAMS_FILE = "/tokenizer_break_rules.txt";

    public static void main(String[] args) throws Exception {

        if(args.length < 3) {
            System.out.println("Need two arguments after 'test': model file and test data file");
            throw new Exception();
        }

        String modelFile = args[1];
        String dataFile = args[2];

        System.out.println("Testing classifier at " + modelFile + " on data at " + dataFile);

        initializeTokenizer();

        List<AcronymDatum> acronymData = generateAcronymData(dataFile);
        AcronymModel model = AcronymModel.loadFromSerialized(modelFile);

        System.out.println(test(acronymData, model));
    }

    public static void initializeTokenizer() throws IOException {
        String rules = new Scanner( AcronymTrain.class.getResourceAsStream(TOKENIZER_PARAMS_FILE) ).useDelimiter("\\A").next();
        tokenizer = new RuleBasedBreakIterator(rules);
    }

    private static List<AcronymDatum> generateAcronymData(String filename) throws IOException {
        List<AcronymDatum> acronymData = new ArrayList<>();
        BufferedReader fileReader = new BufferedReader(new FileReader(filename));

        String line;
        while((line = fileReader.readLine()) != null) {
            String[] splitLine = line.split("\\|");

            int startPos = Integer.parseInt(splitLine[3]);
            int endPos = Integer.parseInt(splitLine[4]) + 1;
            String sense = splitLine[1];
            if (sense.equals("GENERAL ENGLISH")) {
                sense = splitLine[0].toLowerCase();
            }

            // sub out the non-canonical forms, which get tokenized funny
            String subbedString = splitLine[6];
            subbedString = subbedString.substring(0, startPos) + splitLine[0] + subbedString.substring(endPos);
            AcronymDatum acronymDatum = new AcronymDatum(subbedString, startPos, sense, tokenizer);
            acronymData.add(acronymDatum);

        }

        return acronymData;
    }

    private static double test(List<AcronymDatum> acronymData, AcronymModel model) {

        int total = 0;
        int correct = 0;

        for(AcronymDatum acronymDatum : acronymData) {
            total++;
            String hypothesis = model.findBestSense(acronymDatum.allTokens.getTokens(), acronymDatum.tokenOfInterest);

            if(hypothesis.equals(acronymDatum.gold)) {
                correct++;
            }
            else if(hypothesis.equals(acronymDatum.tokenOfInterest.getText().toLowerCase()) && acronymDatum.gold.equals("GENERAL ENGLISH"))
                correct++;
        }

        System.out.println(correct + " out of " + total);
        return (double)correct / total;
    }

}
