package acronym;

import model.Token;

import java.io.*;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPOutputStream;

/**
 * Created by ANONYMOUS on 10/23/15.
 */
public class AcronymBayesModel implements Serializable, AcronymModel {

    // A vector space with a built dictionary to use at test time
    private final VectorSpaceDouble vectorSpaceDouble;

    // A map between acronyms and all their possible long forms
    private final Map<String,List<String>> expansionMap;

    // Maps long forms to their trained word vectors
    private final Map<String,WordVectorDouble> senseMapMean;
    private final Map<String,WordVectorDouble> senseMapSd;

    private double gauss(double mean, double sd, double x) {
        return 1.0 / (Math.sqrt( 2 * Math.PI * Math.pow(sd,2) )) * Math.exp( - Math.pow(x - mean, 2) / (2*Math.pow(sd, 2)) );
    }

    public AcronymBayesModel(VectorSpaceDouble vectorSpaceDouble, Map<String, WordVectorDouble> senseMapMean, Map<String, WordVectorDouble> senseMapSd, Map<String, List<String>> expansionMap) {
        this.expansionMap = expansionMap;
        this.senseMapMean = senseMapMean;
        this.senseMapSd = senseMapSd;
        this.vectorSpaceDouble = vectorSpaceDouble;
    }

    public List<String> getExpansions(Token token) {
        String acronym = AcronymModel.standardForm(token);
        return expansionMap.get(acronym);
    }

    public String standardForm(Token token) {
        return AcronymModel.standardForm(token);
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

        String acronym = AcronymModel.standardForm(token);
        if(!expansionMap.containsKey(acronym)) return "";

        if(expansionMap.get(acronym).size() == 1) {
            return expansionMap.get(acronym).get(0);
        }

        double best = -Double.MAX_VALUE;
        String winner = "acronym_expander_not_working";

        WordVectorDouble vector = vectorSpaceDouble.vectorize(context, token);

        // Loop through all possible senses for this acronym
        for(String sense : expansionMap.get(acronym)) {
            if(senseMapMean.containsKey(sense)) {
                WordVectorDouble mean = senseMapMean.get(sense);
                WordVectorDouble sd = senseMapSd.get(sense);

                double score = 0;

                for(int key : vector.getKeySet()) {
                    double minLogLik = -100;
                    double addScore = gauss(mean.get(key), sd.get(key), vector.get(key));
                    addScore = Math.log(addScore);
                    if(!(addScore >= minLogLik)) addScore = minLogLik;
                    score += addScore;
                }

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
