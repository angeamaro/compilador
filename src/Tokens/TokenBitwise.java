/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package Tokens;

import java.util.HashMap;

/**
 *
 * @author itsupport
 */
public class TokenBitwise extends Token {

    //Operadores Bitwise
    public static final HashMap<String, String> operadores = new HashMap<>();

    static {
        operadores.put("&", "BITWISE_AND");
        operadores.put("|", "BITWISE_OR");
        operadores.put("^", "BITWISE_XOR");
        operadores.put("~", "BITWISE_NOT");
        operadores.put("<<", "LEFT_SHIFT");
        operadores.put(">>", "RIGHT_SHIFT");
    }

    public TokenBitwise(String valor, int linha, int coluna) {
        super(operadores.get(valor), valor, linha, coluna);
    }

}
