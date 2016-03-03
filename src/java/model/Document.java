package model;

import java.util.ArrayList;
import java.util.List;

import com.ibm.icu.text.BreakIterator;
import model.Token;


/**
 * Simple class to hold a string of Tokens
 *
 * Created by ANONYMOUS on 1/4/16.
 */
public class Document {
    List<Token> tokens;

    public Document(String documentText, BreakIterator tokenizer) {
        tokens = new ArrayList<>();

        tokenizer.setText(documentText);

        int begin = tokenizer.first();
        int end = tokenizer.next();
        while (end != BreakIterator.DONE) {
            tokens.add(new Token(documentText, begin, end));
            begin = end;
            end = tokenizer.next();
        }
    }

    public List<Token> getTokens() {
        return tokens;
    }
}
