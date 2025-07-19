/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package models;

import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author itsupport
 */
public class DefinicaoStruct {
    private String nome;
    private Map<String, String> campos = new HashMap<>(); // Nome do campo -> Tipo

    public DefinicaoStruct(String nome) {
        this.nome = nome;
    }

    public String getNome() {
        return nome;
    }

    public Map<String, String> getCampos() {
        return campos;
    }
    
    

    
}
