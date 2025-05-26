package Tokens;

import java.util.HashMap;

/**
 *
 * @author itsupport
 */
public class TokenTiposDeDados extends Token {

    public static final HashMap<String, String> tiposDeDados = new HashMap<>();

    static {
        // Tipos de dados primitivos
        tiposDeDados.put("int", "INT");
        tiposDeDados.put("float", "FLOAT");
        tiposDeDados.put("char", "CHAR");
        tiposDeDados.put("double", "DOUBLE");
        tiposDeDados.put("void", "VOID");

        // Identificadores e literais
        tiposDeDados.put("IDENTIFIER", "IDENTIFIER"); // Nome de variável ou função
        tiposDeDados.put("NUMBER", "NUMBER");         // Número inteiro
        tiposDeDados.put("NUMBER_FLOAT", "NUMBER_FLOAT"); // Número decimal
        tiposDeDados.put("STRING", "STRING");         // Literal entre aspas duplas
        tiposDeDados.put("CHARACTER", "CHARACTER");   // Literal entre aspas simples
    }

    public TokenTiposDeDados(String valor, int linha, int coluna) {
        super(tiposDeDados.get(valor), valor, linha, coluna);
    }
}
