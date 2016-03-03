package model;

import com.ibm.icu.text.BreakIterator;

/**
 * Used when evaluating acronym models
 *
 * Created by ANONYMOUS on 1/4/16.
 */
public class AcronymDatum {

    public Token tokenOfInterest;
    public Document allTokens;
    // correct sense:
    public String gold;

    /**
     * Create a new datum for evaluation
     * @param context the text the test item appears in
     * @param startPos the starting position of the acronym
     * @param gold the correct sense
     * @param tokenizer an initialized BreakIterator tokenizer
     */
    public AcronymDatum(String context, int startPos, String gold, BreakIterator tokenizer) {
        this.gold = gold;
        allTokens = new Document(context, tokenizer);
        for(Token t : allTokens.getTokens()) {
            if(t.getBegin() == startPos)
                tokenOfInterest = t;
        }
        if(tokenOfInterest == null) {
            System.out.println("Token not found at promised position in this document: " + context);
        }
    }

}
