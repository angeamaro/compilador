/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package models;

/**
 *
 * @author itsupport
 */
public class Parametro {
    String nome;
    String tipo;
    boolean isPonteiro;
    
    public Parametro(String nome, String tipo, boolean isPonteiro) {
        this.nome = nome;
        this.tipo = tipo;
        this.isPonteiro = isPonteiro;
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
    
    
}
