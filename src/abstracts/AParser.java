package abstracts;

import Tokens.*;
import java.io.IOException;
import java.util.List;
import models.Parametro;

/**
 * Interface abstrata para o analisador sintático/semântico
 */
public abstract class AParser {
    // === Métodos Principais ===
    public abstract void parse() throws IOException;
    public abstract int getCountErros();

    // === Controle de Fluxo ===
    protected abstract void avancar();
    protected abstract boolean consumir(String esperado) throws IOException;
    protected abstract void sincronizar();
    protected abstract void erro(String msg) throws IOException;
    protected abstract void erroSemantico(String mensagem);

    // === Regras Gramaticais ===
    protected abstract void programa() throws IOException;
    protected abstract void declaracao_struct() throws IOException;
    protected abstract void declaracao_funcao() throws IOException;
    protected abstract List<Parametro> parametros() throws IOException;
    protected abstract void comando() throws IOException;
    protected abstract void declaracao() throws IOException;
    protected abstract void lista_identificadores(String tipo) throws IOException;
    protected abstract void expressao() throws IOException;
    protected abstract void expressao_ternaria() throws IOException;
    protected abstract void expressao_logica() throws IOException;
    protected abstract void expressao_relacional() throws IOException;
    protected abstract void expressao_aritmetica() throws IOException;
    protected abstract void if_cmd() throws IOException;
    protected abstract void while_cmd() throws IOException;
    protected abstract void for_cmd() throws IOException;
    protected abstract String especificador_tipo() throws IOException;
    protected abstract void printf_cmd() throws IOException;
    protected abstract void scanf_cmd() throws IOException;
    protected abstract void do_while_cmd() throws IOException;
    protected abstract void switch_cmd() throws IOException;
    protected abstract void return_cmd() throws IOException;
    protected abstract void atribuicao() throws IOException;
    protected abstract void chamada_funcao() throws IOException;
    protected abstract void bloco() throws IOException;
    protected abstract void termo() throws IOException;
    protected abstract void atribuicao_sem_ponto_e_virgula() throws IOException;
    protected abstract void fator() throws IOException;
    protected abstract void elemento() throws IOException;
    protected abstract void ponteiro() throws IOException;
    protected abstract void argumentos() throws IOException;

    // === Métodos de Verificação Semântica ===
    protected abstract void verificarCompatibilidadeTipos(String tipoEsperado, String tipoRecebido, String contexto);
    protected abstract void verificarCondicao(String tipo);
    protected abstract String verificarExpressao() throws IOException;
    protected abstract String verificarExpressaoTernaria() throws IOException;
    protected abstract String verificarExpressaoLogica() throws IOException;
    protected abstract String verificarExpressaoRelacional() throws IOException;
    protected abstract String verificarExpressaoAritmetica() throws IOException;
    protected abstract String verificarTermo() throws IOException;
    protected abstract String verificarFator() throws IOException;
    protected abstract String verificarElemento() throws IOException;
    
    // === Utilitários ===
    protected abstract boolean eOperadorRelacional();
    protected abstract boolean ehTipoNumerico(String tipo);
    protected abstract boolean ehTipoValido(String tipo);
    protected abstract boolean tiposCompativeis(String tipo1, String tipo2);
    protected abstract String determinarTipoResultante(String tipo1, String tipo2);
    protected abstract String obterTipoBaseArray(String tipo);
    protected abstract String obterTipoBasePonteiro(String tipo);
}