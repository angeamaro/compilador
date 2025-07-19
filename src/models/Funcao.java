/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package models;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author itsupport
 */
public class Funcao {
    String nome;
    String tipoRetorno;
    List<Parametro> parametros;
    int linhaDeclaracao;
    
    public Funcao(String nome, String tipoRetorno, int linha) {
        this.nome = nome;
        this.tipoRetorno = tipoRetorno;
        this.parametros = new ArrayList<>();
        this.linhaDeclaracao = linha;
    }

    public String getNome() {
        return nome;
    }

    public String getTipoRetorno() {
        return tipoRetorno;
    }

    public List<Parametro> getParametros() {
        return parametros;
    }

    public int getLinhaDeclaracao() {
        return linhaDeclaracao;
    }
    
    
}
