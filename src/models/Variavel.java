/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package models;

/**
 *
 * @author itsupport
 */
public class Variavel {
    String nome;
    String tipo;
    boolean isPonteiro;
    boolean isArray;
    int linhaDeclaracao;
    
    public Variavel(String nome, String tipo, boolean isPonteiro, boolean isArray, int linha) {
        this.nome = nome;
        this.tipo = tipo;
        this.isPonteiro = isPonteiro;
        this.isArray = isArray;
        this.linhaDeclaracao = linha;
    }

    public String getNome() {
        return nome;
    }

    public String getTipo() {
        return tipo;
    }

    public boolean isIsPonteiro() {
        return isPonteiro;
    }

    public boolean isIsArray() {
        return isArray;
    }

    public int getLinhaDeclaracao() {
        return linhaDeclaracao;
    }
    
    
}
