package Tokens;

import java.util.HashMap;

/**
 *
 * @author itsupport
 */
public class TokenPalavrasReservadas extends Token {

    public static final HashMap<String, String> palavrasReservadas = new HashMap<>();

    static {
        // Palavras-chave de controle de fluxo
        palavrasReservadas.put("if", "IF");
        palavrasReservadas.put("else", "ELSE");
        palavrasReservadas.put("while", "WHILE");
        palavrasReservadas.put("for", "FOR");
        palavrasReservadas.put("do", "DO");
        palavrasReservadas.put("switch", "SWITCH");
        palavrasReservadas.put("case", "CASE");
        palavrasReservadas.put("default", "DEFAULT");
        palavrasReservadas.put("break", "BREAK");
        palavrasReservadas.put("continue", "CONTINUE");
        palavrasReservadas.put("return", "RETURN");
        palavrasReservadas.put("goto", "GOTO");

        // Definições de tipos e estruturas
        palavrasReservadas.put("struct", "STRUCT");
        palavrasReservadas.put("typedef", "TYPEDEF");
        palavrasReservadas.put("enum", "ENUM");
        palavrasReservadas.put("union", "UNION");

        // Qualificadores
        palavrasReservadas.put("const", "CONST");
        palavrasReservadas.put("static", "STATIC");
        palavrasReservadas.put("extern", "EXTERN");
        palavrasReservadas.put("volatile", "VOLATILE");
        palavrasReservadas.put("register", "REGISTER");
        palavrasReservadas.put("inline", "INLINE");

        // Outros
        palavrasReservadas.put("main", "MAIN");
        palavrasReservadas.put("sizeof", "SIZEOF");

        // Token especial
        palavrasReservadas.put("EOF", "EOF");
        palavrasReservadas.put("int", "INT");
        palavrasReservadas.put("float", "FLOAT");
        palavrasReservadas.put("char", "CHAR");
        palavrasReservadas.put("double", "DOUBLE");
        palavrasReservadas.put("void", "VOID");
    }

    public TokenPalavrasReservadas(String valor, int linha, int coluna) {
        super(palavrasReservadas.get(valor), valor, linha, coluna);
    }
}
