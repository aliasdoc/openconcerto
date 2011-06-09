/*
 * DynamicJava - Copyright (C) 1999 Dyade
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and
 * associated documentation files (the "Software"), to deal in the Software without restriction,
 * including without limitation the rights to use, copy, modify, merge, publish, distribute,
 * sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions: The above copyright notice and this
 * permission notice shall be included in all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT
 * NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL DYADE BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH
 * THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 * 
 * Except as contained in this notice, the name of Dyade shall not be used in advertising or
 * otherwise to promote the sale, use or other dealings in this Software without prior written
 * authorization from Dyade.
 */

/* Generated By:JavaCC: Do not edit this line. ParseException.java Version 0.7pre6 */
package koala.dynamicjava.parser;

/**
 * This exception is thrown when parse errors are encountered. You can explicitly create objects of
 * this exception type by calling the method generateParseException in the generated parser.
 * 
 * You can modify this class to customize your error reporting mechanisms so long as you retain the
 * public fields.
 */
public class ParseException extends Exception {
    /**
     * The end of line string for this machine.
     */
    protected String eol = System.getProperty("line.separator", "\n");

    private int line;

    private int col;

    private String message = "?";

    /**
     * This constructor is used by the method "generateParseException" in the generated parser.
     * Calling this constructor generates a new object of this type with the fields "currentToken",
     * "expectedTokenSequences", and "tokenImage" set. The boolean flag "specialConstructor" is also
     * set to true to indicate that this constructor was used to create this object. This
     * constructor calls its super class with the empty string to force the "toString" method of
     * parent class "Throwable" to print the error message in the form: ParseException: <result of
     * getMessage>
     */
    public ParseException(final Token currentTokenVal, final int[][] expectedTokenSequencesVal, final String[] tokenImageVal) {
        super("");
        this.specialConstructor = true;
        this.currentToken = currentTokenVal;
        this.expectedTokenSequences = expectedTokenSequencesVal;
        this.tokenImage = tokenImageVal;
        computeMessage();
    }

    private void computeMessage() {
        String expected = "";
        int maxSize = 0;
        for (int i = 0; i < this.expectedTokenSequences.length; i++) {
            if (maxSize < this.expectedTokenSequences[i].length) {
                maxSize = this.expectedTokenSequences[i].length;
            }
            for (int j = 0; j < this.expectedTokenSequences[i].length; j++) {
                expected += this.tokenImage[this.expectedTokenSequences[i][j]] + " ";
            }
            if (this.expectedTokenSequences[i][this.expectedTokenSequences[i].length - 1] != 0) {
                expected += "...";
            }
            expected += this.eol + "    ";
        }
        String retval = "Encountered \"";
        Token tok = this.currentToken.next;
        for (int i = 0; i < maxSize; i++) {
            if (i != 0) {
                retval += " ";
            }
            if (tok.kind == 0) {
                retval += this.tokenImage[0];
                break;
            }
            retval += add_escapes(tok.image);
            tok = tok.next;
        }
        this.line = this.currentToken.next.beginLine;
        this.col = this.currentToken.next.beginColumn;
        System.out.println("*******************");
        System.out.println(this.line + "," + this.col);
        System.out.println("*******************");

        retval += this.eol;
        if (this.expectedTokenSequences.length == 1) {
            retval += "Was expecting:" + this.eol + "    ";
        } else {
            retval += "Was expecting one of:" + this.eol + "    ";
        }
        retval += expected;
        this.message = retval;
    }

    /**
     * The following constructors are for use by you for whatever purpose you can think of.
     * Constructing the exception in this manner makes the exception behave in the normal way -
     * i.e., as documented in the class "Throwable". The fields "errorToken",
     * "expectedTokenSequences", and "tokenImage" do not contain relevant information. The JavaCC
     * generated code does not use these constructors.
     */

    public ParseException() {
        super();
        this.specialConstructor = false;
    }

    public ParseException(final String message) {
        super(message);
        this.specialConstructor = false;
    }

    /**
     * This variable determines which constructor was used to create this object and thereby affects
     * the semantics of the "getMessage" method (see below).
     */
    protected boolean specialConstructor;

    /**
     * This is the last token that has been consumed successfully. If this object has been created
     * due to a parse error, the token followng this token will (therefore) be the first error
     * token.
     */
    public Token currentToken;

    /**
     * Each entry in this array is an array of integers. Each array of integers represents a
     * sequence of tokens (by their ordinal values) that is expected at this point of the parse.
     */
    public int[][] expectedTokenSequences;

    /**
     * This is a reference to the "tokenImage" array of the generated parser within which the parse
     * error occurred. This array is defined in the generated ...Constants interface.
     */
    public String[] tokenImage;

    /**
     * This method has the standard behavior when this object has been created using the standard
     * constructors. Otherwise, it uses "currentToken" and "expectedTokenSequences" to generate a
     * parse error message and returns it. If this object has been created due to a parse error, and
     * you do not catch it (it gets thrown from the parser), then this method is called during the
     * printing of the final stack trace, and hence the correct error message gets displayed.
     */
    @Override
    public String getMessage() {
        if (!this.specialConstructor) {
            return super.getMessage();
        }
        return this.message;
    }

    /**
     * Used to convert raw characters to their escaped version when these raw version cannot be used
     * as part of an ASCII string literal.
     */
    protected String add_escapes(final String str) {
        final StringBuffer retval = new StringBuffer();
        char ch;
        for (int i = 0; i < str.length(); i++) {
            switch (str.charAt(i)) {
            case 0:
                continue;
            case '\b':
                retval.append("\\b");
                continue;
            case '\t':
                retval.append("\\t");
                continue;
            case '\n':
                retval.append("\\n");
                continue;
            case '\f':
                retval.append("\\f");
                continue;
            case '\r':
                retval.append("\\r");
                continue;
            case '\"':
                retval.append("\\\"");
                continue;
            case '\'':
                retval.append("\\\'");
                continue;
            case '\\':
                retval.append("\\\\");
                continue;
            default:
                if ((ch = str.charAt(i)) < 0x20 || ch > 0x7e) {
                    final String s = "0000" + Integer.toString(ch, 16);
                    retval.append("\\u" + s.substring(s.length() - 4, s.length()));
                } else {
                    retval.append(ch);
                }
                continue;
            }
        }
        return retval.toString();
    }

    public int getLine() {
        return this.line;

    }

    public int getColumn() {
        return this.col;

    }

    @Override
    public String toString() {
        // TODO Auto-generated method stub
        return "ParseException" + this.message + " line " + getLine() + " column:" + getColumn();
    }

}
