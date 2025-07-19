/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package abstracts;

import java.io.IOException;

/**
 *
 * @author itsupport
 */
public abstract class AParser {
     // ===== Métodos principais =====
    public abstract void parse() throws IOException;

    // ===== Métodos de controle do fluxo de análise =====
    protected abstract void avancar();

    protected abstract boolean consumir(String esperado) throws IOException;

    protected abstract void sincronizar() throws IOException;

    protected abstract void erro(String msg) throws IOException;

    // ===== Regras gramaticais =====
    public abstract void programa() throws IOException;
    
    protected abstract void declaracao_struct() throws IOException;

    protected abstract void declaracao_funcao() throws IOException;

    protected abstract void parametros() throws IOException;

    protected abstract void comando() throws IOException;

    protected abstract void chamada_funcao() throws IOException;

    protected abstract void argumentos() throws IOException;

    protected abstract void declaracao() throws IOException;

    protected abstract String especificador_tipo() throws IOException;

    protected abstract void atribuicao() throws IOException;

    protected abstract void if_cmd() throws IOException;

    protected abstract void do_while_cmd() throws IOException;

    protected abstract void bloco() throws IOException;

    protected abstract void expressao() throws IOException;

    protected abstract void expressao_ternaria() throws IOException;

    protected abstract void switch_cmd() throws IOException;

    protected abstract void lista_identificadores() throws IOException;

    protected abstract void expressao_aritmetica() throws IOException;

    protected abstract boolean eOperadorRelacional();

    protected abstract void while_cmd() throws IOException;

    protected abstract void printf_cmd() throws IOException;

    protected abstract void return_cmd() throws IOException;

    protected abstract void termo() throws IOException;

    protected abstract void for_cmd() throws IOException;

    protected abstract void atribuicao_sem_ponto_e_virgula() throws IOException;

    protected abstract void scanf_cmd() throws IOException;

    protected abstract void fator() throws IOException;
    
    protected abstract void elemento() throws IOException;

    protected abstract void expressao_logica() throws IOException;

    protected abstract void expressao_relacional() throws IOException;
    
    protected abstract void ponteiro () throws IOException;
    
    protected abstract void acesso_array() throws IOException; 
}
