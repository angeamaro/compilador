package Tokens;

import java.util.HashMap;

/**
 *
 * @author itsupport
 */
public class TokenOperadoresAritmeticos extends Token {

    public static final HashMap<String, String> operadores = new HashMap<>();

    static {
        // Operadores aritméticos
        operadores.put("+", "PLUS");
        operadores.put("-", "MINUS");
        operadores.put("*", "MULTIPLY");
        operadores.put("/", "DIVIDE");
        operadores.put("%", "MODULO");

        // Incrementos/decrementos
        operadores.put("++", "INCREMENT");
        operadores.put("--", "DECREMENT");
    }

    public TokenOperadoresAritmeticos(String valor, int linha, int coluna) {
        super(operadores.get(valor), valor, linha, coluna);
    }
}
