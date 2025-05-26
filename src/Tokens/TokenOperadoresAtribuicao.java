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
public class TokenOperadoresAtribuicao extends Token {

    //Operadores de atribuição  
    public static final HashMap<String, String> operadores = new HashMap<>();

    static {
        operadores.put("=", "ASSIGN");
        operadores.put("+=", "ADD_ASSIGN");
        operadores.put("-=", "SUB_ASSIGN");
        operadores.put("*=", "MUL_ASSIGN");
        operadores.put("/=", "DIV_ASSIGN");
        operadores.put("<<=", "LEFT_SHIFT_ASSIGN");
        operadores.put(">>=", "RIGHT_SHIFT_ASSIGN");
        operadores.put("&=", "AND_ASSIGN");
        operadores.put("|=", "OR_ASSIGN");
        operadores.put("^=", "XOR_ASSIGN");
    }

    public TokenOperadoresAtribuicao(String valor, int linha, int coluna) {
        super(operadores.get(valor), valor, linha, coluna);
    }

}
