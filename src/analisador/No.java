/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package analisador;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author itsupport
 */
public class No {
     private String nome; // Ex: "declaracao", "tipo", "ID"
    private List<No> filhos;
    
    public No(String nome) {
        this.nome = nome;
        this.filhos = new ArrayList<>();
    }
    
    public void adicionarFilho(No filho) {
        filhos.add(filho);
    }

    public String getNome() {
        return nome;
    }

    public List<No> getFilhos() {
        return filhos;
    }

    // Para impressão da árvore
    public void imprimir(String prefixo) {
        System.out.println(prefixo + nome);
        for (No filho : filhos) {
            filho.imprimir(prefixo + "  ");
        }
    }
}
