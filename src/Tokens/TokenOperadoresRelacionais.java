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
public class TokenOperadoresRelacionais extends Token {

    public static final HashMap<String, String> operadores = new HashMap<>();

    static {
        //Operadores relacionais
        operadores.put("==", "EQUAL");
        operadores.put("!=", "NOT_EQUAL");
        operadores.put(">", "GREATER");
        operadores.put("<", "LESS");
        operadores.put(">=", "GREATER_EQUAL");
        operadores.put("<=", "LESS_EQUAL");
        // Operadores lógicos
        operadores.put("&&", "AND");
        operadores.put("||", "OR");
        operadores.put("!", "NOT");
    }

    public TokenOperadoresRelacionais(String valor, int linha, int coluna) {
        super(operadores.get(valor), valor, linha, coluna);
    }

}
