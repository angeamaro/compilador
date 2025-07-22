/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package models;

import java.util.HashMap;
import java.util.Stack;

/**
 *
 * @author itsupport
 */
public class Escopo {

    private Stack<HashMap<String, Variavel>> pilhaEscopos = new Stack<>();
    private HashMap<String, Funcao> funcoes = new HashMap<>();
    private HashMap<String, String> tiposStruct = new HashMap<>();
    private HashMap<String, Struct> definicoesStruct = new HashMap<>();

    public void abrirEscopo() {
        pilhaEscopos.push(new HashMap<>());
    }

    public void fecharEscopo() {
        if (!pilhaEscopos.isEmpty()) {
            pilhaEscopos.pop();  // Descarta o escopo mais recente
        }
    }

    public void adicionarVariavel(String nome, Variavel var) {
        if (pilhaEscopos.peek().containsKey(nome)) {
            // Variável já declarada neste escopo
            System.err.println("Erro Semântico: Variável '" + nome + "' já declarada neste escopo");
        } else {
            pilhaEscopos.peek().put(nome, var);
        }
    }

    public Variavel buscarVariavel(String nome) {
        for (int i = pilhaEscopos.size() - 1; i >= 0; i--) {
            if (pilhaEscopos.get(i).containsKey(nome)) {
                return pilhaEscopos.get(i).get(nome);
            }
        }
        return null; // Variável não encontrada
    }
    
     public void adicionarStruct(String nome, Struct struct) {
        if (definicoesStruct.containsKey(nome)) {
            System.err.println("Erro Semântico: Struct '" + nome + "' já declarada");
        } else {
            definicoesStruct.put(nome, struct);
            tiposStruct.put(nome, "struct " + nome); // Register struct type
        }
    }

    public Struct buscarStruct(String nome) {
        return definicoesStruct.get(nome);
    }


    public void adicionarFuncao(String nome, Funcao func) {
        if (funcoes.containsKey(nome)) {
            System.err.println("Erro Semântico: Função '" + nome + "' já declarada");
        } else {
            funcoes.put(nome, func);
        }
    }

    public void adicionarDefinicaoStruct(Struct def) {
        definicoesStruct.put(def.getNome(), def);
    }

    public String buscarTipoCampoStruct(String nomeStruct, String nomeCampo) {
        Struct def = definicoesStruct.get(nomeStruct);
        if (def == null) {
            return null;
        }
        return def.getCampos().get(nomeCampo);
    }

    public Funcao buscarFuncao(String nome) {
        return funcoes.get(nome);
    }

    public void adicionarTipoStruct(String nome, String tipo) {
        tiposStruct.put(nome, tipo);
    }

    public String buscarTipoStruct(String nome) {
        return tiposStruct.get(nome);
    }

    public Stack<HashMap<String, Variavel>> getPilhaEscopos() {
        return pilhaEscopos;
    }

    public HashMap<String, Funcao> getFuncoes() {
        return funcoes;
    }

    public HashMap<String, String> getTiposStruct() {
        return tiposStruct;
    }

    public HashMap<String, Struct> getDefinicoesStruct() {
        return definicoesStruct;
    }
    
    

}
