package Tokens;

import java.util.HashMap;

/**
 *
 * @author itsupport
 */
public class TokenSimbolos extends Token {

    public static final HashMap<String, String> simbolos = new HashMap<>();

    static {
        // Delimitadores
        simbolos.put("(", "LPAREN");
        simbolos.put(")", "RPAREN");
        simbolos.put("{", "LBRACE");
        simbolos.put("}", "RBRACE");
        simbolos.put("[", "LBRACKET");
        simbolos.put("]", "RBRACKET");

        // Símbolos
        simbolos.put(";", "SEMICOLON");
        simbolos.put(",", "COMMA");
        simbolos.put(".", "DOT");
        simbolos.put("->", "ARROW");
        simbolos.put(":", "COLON");
        simbolos.put("?", "QUESTION");

        // Comentários e pré-processador
        simbolos.put("//", "COMMENT");
        simbolos.put("/*", "COMMENT");
        simbolos.put("#", "PREPROCESSOR");
    }

    public TokenSimbolos(String valor, int linha, int coluna) {
        super(simbolos.get(valor), valor, linha, coluna);
    }
}
