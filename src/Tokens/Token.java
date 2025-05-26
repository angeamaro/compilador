/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package Tokens;

/**
 *
 * @author itsupport
 */
public class Token {

    protected String tipo;
    protected String valor;
    protected int linha;
    protected int coluna;

    public Token(String tipo, String valor, int linha, int coluna) {
        this.tipo = tipo;
        this.valor = valor;
        this.linha = linha;
        this.coluna = coluna;
    }

    public String toString() {
        return String.format("Token(tipo=%s, valor=%s, linha=%d, coluna=%d)", tipo, valor, linha, coluna);
    }

    public String getTipo() {
        return tipo;
    }

    public String getValor() {
        return valor;
    }

    public int getLinha() {
        return linha;
    }

    public int getColuna() {
        return coluna;
    }
    

}
