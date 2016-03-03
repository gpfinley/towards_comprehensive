package acronym;

import model.Token;

import java.io.*;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPOutputStream;

/**
 * Created by ANONYMOUS on 10/23/15.
 */
public class AcronymSvmModel implements Serializable, AcronymModel {

    // A vector space with a built dictionary to use at test time
    private final VectorSpaceDouble vectorSpaceDouble;

    // A map between acronyms and all their possible long forms
    private final Map<String,List<String>> expansionMap;

    private final Map<String,GradientDescentModel> models;

    public AcronymSvmModel(VectorSpaceDouble vectorSpaceDouble, Map<String, List<String>> expansionMap, Map<String,GradientDescentModel> models) {
        this.expansionMap = expansionMap;
        this.models = models;
        this.vectorSpaceDouble = vectorSpaceDouble;
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

        String acronym = AcronymModel.standardForm(token);
        if(!expansionMap.containsKey(acronym)) return "";

        if(expansionMap.get(acronym).size() == 1) {
            return (String) expansionMap.get(acronym).toArray()[0];
        }
        if(expansionMap.get(acronym).size() == 0) {
            return token.getText();
        }

        WordVectorDouble vector = vectorSpaceDouble.vectorize(context, token);
        vector.multiply(vectorSpaceDouble.getIdf());
        vector.normVector();

        int winnerInt = models.get(acronym).classify( vector );
        String winner = expansionMap.get(acronym).get(winnerInt);

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
