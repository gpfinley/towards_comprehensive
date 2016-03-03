package model;

/**
 * Simple class to hold a token
 * Contains the text of the token as well as its begin and end indices in its string of origin
 *
 * Created by ANONYMOUS on 1/4/16.
 */
public class Token {
    String text;
    int begin;
    int end;

    public Token(String body, int begin, int end) {
        text = body.substring(begin, end);
        this.begin = begin;
        this.end = end;
    }
    public String getText() {
        return text;
    }

    public int getBegin() {
        return begin;
    }
    public int getEnd() {
        return end;
    }
}
