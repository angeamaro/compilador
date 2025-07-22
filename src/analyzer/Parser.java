package analyzer;

import Tokens.*;
import java.io.IOException;
import java.util.List;
import abstracts.AParser;
import java.util.ArrayList;
import models.*;

public class Parser extends AParser {

    protected Token tokenAtual;
    protected int pos = 0;
    protected TabelaDeTokens tabela;
    protected int countErros = 0;
    protected List<Token> tokens;
    private Escopo escopos = new Escopo();
    private String funcaoAtual = null; // Para verificação de return
    private String tipoRetornoAtual = null; // Para verificação de return

    public Parser(TabelaDeTokens tabela) {
        if (tabela == null) {
            throw new IllegalArgumentException("TabelaDeTokens não pode ser nula");
        }

        this.tabela = tabela;
        this.tokens = tabela.getTokens();

        if (tokens == null) {
            throw new IllegalArgumentException("Lista de tokens não pode ser nula");
        }

        if (!tokens.isEmpty()) {
            this.tokenAtual = tokens.get(0);
        } else {
            this.tokenAtual = new Token("EOF", "Fim de Arquivo", -1, -1);
        }
    }

    public int getCountErros() {
        return countErros;
    }

    @Override
    protected boolean consumir(String esperado) throws IOException {
        if (tokenAtual.getTipo().equals(esperado)) {
            avancar();
            return true;
        }
        return false;
    }

    @Override
    protected void avancar() {
        pos++;
        if (pos < tokens.size()) {
            tokenAtual = tokens.get(pos);
        } else {
            tokenAtual = new Token("EOF", "Fim de Arquivo", -1, -1);
        }
    }

    @Override
    protected void erro(String msg) throws IOException {
        System.err.println("Erro: " + msg + " encontrado " + tokenAtual.getValor()
                + " [Linha: " + tokenAtual.getLinha()
                + ", Coluna: " + tokenAtual.getColuna() + "]");
        countErros++;
        sincronizar();
    }

    private void erroSemantico(String mensagem) {
        int linha = tokenAtual != null ? tokenAtual.getLinha() : -1;
        int coluna = tokenAtual != null ? tokenAtual.getColuna() : -1;
        System.err.println("Erro Semântico: " + mensagem + " [Linha: " + linha + ", Coluna: " + coluna + "]");
        countErros++; // Incrementa também o contador geral de erros
    }

    @Override
    protected void sincronizar() {
        while (!tokenAtual.getTipo().equals("SEMICOLON")
                && !tokenAtual.getTipo().equals("RBRACE")
                && !tokenAtual.getTipo().equals("EOF")) {
            avancar();
        }
        if (!tokenAtual.getTipo().equals("EOF")) {
            avancar();
        }
    }    // Entrada do programa

    @Override
    public void parse() throws IOException {
        escopos.abrirEscopo(); // Escopo global
        programa();
        if (!tokenAtual.getTipo().equals("EOF")) {
            erro("Tokens restantes inesperados");
        }
        escopos.fecharEscopo();
    }

    // -------------------REGRAS DA GRAMÁTICA ---------------------//
    // <programa> ::= { ( declaracao_funcao | declaracao | declaracao_struct | diretiva ) }*
    @Override
    public void programa() throws IOException {
        while (!tokenAtual.getTipo().equals("EOF")) {
            if (tokenAtual.getTipo().equals("STRUCT")) {
                declaracao_struct();
            } else if (tokenAtual.getTipo().equals("DIRECTIVE")) {
                consumir("DIRECTIVE");
            } else if (tokenAtual.getTipo().equals("CONST")
                    || tokenAtual.getTipo().equals("VOID")
                    || tokenAtual.getTipo().equals("CHAR")
                    || tokenAtual.getTipo().equals("SHORT")
                    || tokenAtual.getTipo().equals("INT")
                    || tokenAtual.getTipo().equals("LONG")
                    || tokenAtual.getTipo().equals("FLOAT")
                    || tokenAtual.getTipo().equals("DOUBLE")
                    || tokenAtual.getTipo().equals("SIGNED")
                    || tokenAtual.getTipo().equals("UNSIGNED")) {

                // Verifica se é função ou declaração
                if (pos + 2 < tokens.size()
                        && (tokens.get(pos + 1).getTipo().equals("IDENTIFIER") || tokens.get(pos + 1).getTipo().equals("MAIN"))
                        && tokens.get(pos + 2).getTipo().equals("LPAREN")) {

                    declaracao_funcao();
                } else {
                    declaracao();
                }
            } else {
                erro("Declaração inválida no escopo global");
                avancar();
            }
        }
    }

    //<declaracao_struct> ::= STRUCT IDENTIFIER '{' { <tipo> IDENTIFIER ';' }* '}' ';'
    @Override
    protected void declaracao_struct() throws IOException {
        consumir("STRUCT");
        if (!consumir("IDENTIFIER")) {
            erro("Esperado nome da struct");
        }
        if (!consumir("LBRACE")) {
            erro("Esperado '{' após nome da struct");
        }

        while (tokenAtual.getTipo().matches("VOID|CHAR|SHORT|INT|LONG|FLOAT|DOUBLE|SIGNED|UNSIGNED|STRUCT")) {
            especificador_tipo();
            ponteiro(); // <--- Adicionado ponteiro
            if (!consumir("IDENTIFIER")) {
                erro("Esperado identificador do campo");
            }

            if (consumir("LBRACKET")) {
                if (!tokenAtual.getTipo().equals("NUMBER") && !tokenAtual.getTipo().equals("IDENTIFIER")) {
                    erro("Esperado tamanho do array");
                }
                avancar();
                if (!consumir("RBRACKET")) {
                    erro("Esperado ']' após tamanho do array");
                }
            }

            if (!consumir("SEMICOLON")) {
                erro("Esperado ';' após campo da struct");
            }
        }

        if (!consumir("RBRACE")) {
            erro("Esperado '}' após campos da struct");
        }
        consumir("SEMICOLON");
    }

    // <declaracao_funcao> ::= <especificador_tipo> (IDENTIFIER | MAIN) '(' [ <parametros> ] ')' <bloco>
    @Override
    protected void declaracao_funcao() throws IOException {
        String tipoRetorno = especificador_tipo();
        ponteiro();

        String nomeFuncao;
        if (tokenAtual.getTipo().equals("IDENTIFIER")) {
            nomeFuncao = tokenAtual.getValor();
            avancar();
        } else if (consumir("MAIN")) {
            nomeFuncao = "main";
        } else {
            erro("Esperado nome da função");
            return;
        }

        // Create Funcao object (will populate parameters later)
        Funcao funcao = new Funcao(nomeFuncao, tipoRetorno, tokenAtual.getLinha());

        // Prepare for return verification
        funcaoAtual = nomeFuncao;
        tipoRetornoAtual = tipoRetorno;

        if (!consumir("LPAREN")) {
            erro("Esperado '(' após nome da função");
            return;
        }

        // Open scope for parameters
        escopos.abrirEscopo();

        // Parse parameters and collect them
        List<Parametro> parametros = new ArrayList<>();
        if (!tokenAtual.getTipo().equals("RPAREN")) {
            parametros = parametros();
        }
        if (!consumir("RPAREN")) {
            erro("Esperado ')' após parâmetros");
            escopos.fecharEscopo();
            funcaoAtual = null;
            tipoRetornoAtual = null;
            return;
        }

        // Store parameters in Funcao object
        funcao.getParametros().addAll(parametros);

        // Register function with collected parameters
        escopos.adicionarFuncao(nomeFuncao, funcao);

        // Parse function body
        bloco();

        // Close parameter scope
        escopos.fecharEscopo();

        // Reset return verification
        funcaoAtual = null;
        tipoRetornoAtual = null;
    }

    @Override
    protected List<Parametro> parametros() throws IOException {
        List<Parametro> parametros = new ArrayList<>();
        do {
            String tipoParam = especificador_tipo();
            StringBuilder tipoCompleto = new StringBuilder(tipoParam);
            boolean isPonteiro = false;

            // Handle pointers
            while (consumir("MULTIPLY")) {
                tipoCompleto.append("*");
                isPonteiro = true; // Set flag if any pointer is detected
            }

            if (!tokenAtual.getTipo().equals("IDENTIFIER")) {
                erro("Esperado identificador do parâmetro");
                return parametros;
            }
            String nomeParam = tokenAtual.getValor();
            avancar();

            // Create and store parameter
            Parametro param = new Parametro(nomeParam, tipoCompleto.toString(), isPonteiro);
            parametros.add(param);

            // Register parameter as variable in scope
            Variavel var = new Variavel(nomeParam, tipoCompleto.toString(), false, false, tokenAtual.getLinha());
            escopos.adicionarVariavel(nomeParam, var);

        } while (consumir("COMMA"));
        return parametros;
    }

    /*<comando> ::= <declaracao> | <atribuicao> | <if_cmd> | <while_cmd> 
                 | <for_cmd> | <printf_cmd> | <scanf_cmd> | <chamada_funcao> 
                 | <return_cmd> | <do_while_cmd> | <switch_cmd> | BREAK ';' | CONTINUE ';'*/
    @Override
    protected void comando() throws IOException {
        if (tokenAtual.getTipo().equals("CONST")
                || tokenAtual.getTipo().equals("VOID")
                || tokenAtual.getTipo().equals("CHAR")
                || tokenAtual.getTipo().equals("SHORT")
                || tokenAtual.getTipo().equals("INT")
                || tokenAtual.getTipo().equals("LONG")
                || tokenAtual.getTipo().equals("FLOAT")
                || tokenAtual.getTipo().equals("DOUBLE")
                || tokenAtual.getTipo().equals("SIGNED")
                || tokenAtual.getTipo().equals("UNSIGNED")
                || tokenAtual.getTipo().equals("STRUCT")) {
            declaracao();
        } else if (tokenAtual.getTipo().equals("IF")) {
            if_cmd();
        } else if (tokenAtual.getTipo().equals("WHILE")) {
            while_cmd();
        } else if (tokenAtual.getTipo().equals("FOR")) {
            for_cmd();
        } else if (tokenAtual.getTipo().equals("IDENTIFIER") && tokenAtual.getValor().equals("printf")) {
            printf_cmd();
        } else if (tokenAtual.getTipo().equals("IDENTIFIER") && tokenAtual.getValor().equals("scanf")) {
            scanf_cmd();
        } else if (tokenAtual.getTipo().equals("DO")) {
            do_while_cmd();
        } else if (tokenAtual.getTipo().equals("SWITCH")) {
            switch_cmd();
        } else if (tokenAtual.getTipo().equals("RETURN")) {
            return_cmd();
        } else if (tokenAtual.getTipo().equals("BREAK")) {
            consumir("BREAK");
            if (!consumir("SEMICOLON")) {
                erro("Esperado ';' após break");
            }
        } else if (tokenAtual.getTipo().equals("CONTINUE")) {
            consumir("CONTINUE");
            if (!consumir("SEMICOLON")) {
                erro("Esperado ';' após continue");
            }
        } else if (tokenAtual.getTipo().equals("IDENTIFIER")
                && tokens.get(pos + 1).getTipo().equals("LPAREN")) {
            chamada_funcao();
            if (!consumir("SEMICOLON")) {
                erro("Esperado ';' após chamada de função");
            }
        } else if (tokenAtual.getTipo().equals("IDENTIFIER")) {
            atribuicao();
        } else {
            erro("Comando inválido");
        }
    }

    // <declaracao> ::= [CONST] <especificador_tipo> <lista_identificadores> ';'
    @Override
    protected void declaracao() throws IOException {
        boolean isConst = consumir("CONST");
        String tipo = especificador_tipo();
        ponteiro();

        lista_identificadores(tipo);
        if (!consumir("SEMICOLON")) {
            erro("Esperado ';' após declaração");
        }
    }

    protected void lista_identificadores(String tipo) throws IOException {
        do {
            if (!tokenAtual.getTipo().equals("IDENTIFIER")) {
                erro("Esperado identificador");
                return;
            }

            String nome = tokenAtual.getValor();
            avancar();

            boolean isArray = false;
            if (consumir("LBRACKET")) {
                isArray = true;
                if (!tokenAtual.getTipo().equals("NUMBER") && !tokenAtual.getTipo().equals("IDENTIFIER")) {
                    erro("Esperado tamanho do array");
                }
                avancar();
                if (!consumir("RBRACKET")) {
                    erro("Esperado ']' após tamanho do array");
                }
            }

            // Registra variável
            Variavel var = new Variavel(nome, tipo, false, isArray, tokenAtual.getLinha());
            escopos.adicionarVariavel(nome, var);

            if (consumir("ASSIGN")) {
                String tipoExpressao = verificarExpressao();
                verificarCompatibilidadeTipos(tipo, tipoExpressao, "atribuição");
            }
        } while (consumir("COMMA"));
    }

    // <lista_identificadores> ::= IDENTIFIER ('[' (NUMBER | IDENTIFIER) ']')? [ '=' <expressao> ] (',' IDENTIFIER ('[' (NUMBER | IDENTIFIER) ']')? [ '=' <expressao> ])*
    @Override
    protected void lista_identificadores() throws IOException {
        do {
            if (!consumir("IDENTIFIER")) {
                erro("Esperado identificador");
            }
            // Para arrays
            if (consumir("LBRACKET")) {
                if (!tokenAtual.getTipo().equals("NUMBER") && !tokenAtual.getTipo().equals("IDENTIFIER")) {
                    erro("Esperado tamanho do array");
                }
                avancar();
                if (!consumir("RBRACKET")) {
                    erro("Esperado ']' após tamanho do array");
                }
            }
            if (consumir("ASSIGN")) {
                expressao();
            }
        } while (consumir("COMMA"));
    }

    // <expressao> ::= <expressao_ternaria>
    @Override
    protected void expressao() throws IOException {
        expressao_ternaria();
    }

    // <expressao_ternaria> ::= <expressao_logica> [ '?' <expressao> ':' <expressao> ]
    @Override
    protected void expressao_ternaria() throws IOException {
        expressao_logica();
        if (consumir("QUESTION")) {
            expressao();
            if (!consumir("COLON")) {
                erro("Esperado ':' no operador ternário");
            }
            expressao();
        }
    }

    // <expressao_logica> ::= <expressao_relacional> ( ('&&' | '||') <expressao_relacional> )*
    @Override
    protected void expressao_logica() throws IOException {
        expressao_relacional();
        while (tokenAtual.getTipo().equals("AND") || tokenAtual.getTipo().equals("OR")) {
            avancar();
            expressao_relacional();
        }
    }

    // <expressao_relacional> ::= <expressao_aritmetica> [ ('==' | '!=' | '<' | '>' | '<=' | '>=') <expressao_aritmetica> ]
    @Override
    protected void expressao_relacional() throws IOException {
        expressao_aritmetica();
        while (eOperadorRelacional()) {
            avancar();
            expressao_aritmetica();
        }
    }

    // <expressao_aritmetica> ::= <termo> ( ('+' | '-') <termo> )*
    @Override
    protected void expressao_aritmetica() throws IOException {
        termo();
        while (tokenAtual.getTipo().equals("PLUS") || tokenAtual.getTipo().equals("MINUS")) {
            avancar();
            termo();
        }
    }

    // <if_cmd> ::= IF '(' <expressao> ')' <bloco> [ ELSE <bloco> ]
    @Override
    protected void if_cmd() throws IOException {
        consumir("IF");
        if (!consumir("LPAREN")) {
            erro("Esperado '(' após 'if'");
        }
        String tipoCondicao = verificarExpressao();
        verificarCondicao(tipoCondicao);

        if (!consumir("RPAREN")) {
            erro("Esperado ')' após expressão");
        }
        bloco();

        if (consumir("ELSE")) {
            bloco();
        }
    }

    // <while_cmd> ::= WHILE '(' <expressao> ')' <bloco>
    @Override
    protected void while_cmd() throws IOException {
        consumir("WHILE");
        if (!consumir("LPAREN")) {
            erro("Esperado '(' após 'while'");
        }

        String tipoCondicao = verificarExpressao();
        verificarCondicao(tipoCondicao);

        if (!consumir("RPAREN")) {
            erro("Esperado ')' após expressão");
        }
        bloco();
    }

    // <for_cmd> ::= FOR '(' <atribuicao> <expressao> ';' <atribuicao_sem_ponto> ')' <bloco>
    @Override
    protected void for_cmd() throws IOException {
        consumir("FOR");
        if (!consumir("LPAREN")) {
            erro("Esperado '(' após for");
        }
        // Adicionado suporte para tipo com ponteiro
        if (tokenAtual.getTipo().matches("INT|FLOAT|DOUBLE|CHAR|VOID|STRUCT")) {
            especificador_tipo();
            ponteiro();
        }
        atribuicao();  // Inicialização
        expressao();    // Condição
        if (!consumir("SEMICOLON")) {
            erro("Esperado ';' após condição do for");
        }
        atribuicao_sem_ponto_e_virgula();  // Incremento
        if (!consumir("RPAREN")) {
            erro("Esperado ')' após for");
        }
        bloco();
    }

    // <printf_cmd> ::= PRINTF '(' (STRING | IDENTIFIER) (',' <expressao>)* ')' ';'
    @Override
    protected void printf_cmd() throws IOException {
        consumir("IDENTIFIER"); // "printf"
        if (!consumir("LPAREN")) {
            erro("Esperado '(' após printf");
        }

        // Primeiro argumento pode ser qualquer expressão
        expressao();

        // Argumentos adicionais
        while (consumir("COMMA")) {
            expressao();
        }

        if (!consumir("RPAREN")) {
            erro("Esperado ')' após printf");
        }
        if (!consumir("SEMICOLON")) {
            erro("Esperado ';' após printf");
        }
    }

    // <scanf_cmd> ::= SCANF '(' STRING (',' '&' IDENTIFIER)* ')' ';'
    @Override
    protected void scanf_cmd() throws IOException {
        consumir("IDENTIFIER");
        if (!consumir("LPAREN")) {
            erro("Esperado '(' após scanf");
        }
        if (!tokenAtual.getTipo().equals("STRING")) {
            erro("Esperado string de formato em scanf");
        }
        avancar();

        while (consumir("COMMA")) {
            if (!consumir("BITWISE_AND")) {
                erro("Esperado '&' antes do identificador em scanf");
            }
            if (!consumir("IDENTIFIER")) {
                erro("Esperado identificador após &");
            }
        }

        if (!consumir("RPAREN")) {
            erro("Esperado ')' após scanf");
        }
        if (!consumir("SEMICOLON")) {
            erro("Esperado ';' após scanf");
        }
    }

    // <do_while_cmd> ::= DO <bloco> WHILE '(' <expressao> ')' ';'
    @Override
    protected void do_while_cmd() throws IOException {
        consumir("DO");
        bloco(); // Ou comando() se permitir comando único
        if (!consumir("WHILE")) {
            erro("Esperado 'while' após bloco do do");
        }
        if (!consumir("LPAREN")) {
            erro("Esperado '(' após while");
        }
        expressao();
        if (!consumir("RPAREN")) {
            erro("Esperado ')' após expressão");
        }
        if (!consumir("SEMICOLON")) {
            erro("Esperado ';' após do-while");
        }
    }

    // <switch_cmd> ::= SWITCH '(' <expressao> ')' '{' (CASE (NUMBER | CHAR) ':' { <comando> }* )+ [ DEFAULT ':' { <comando> }* ] '}'
    @Override
    protected void switch_cmd() throws IOException {
        consumir("SWITCH");
        if (!consumir("LPAREN")) {
            erro("Esperado '(' após switch");
        }
        expressao();
        if (!consumir("RPAREN")) {
            erro("Esperado ')' após expressão");
        }
        if (!consumir("LBRACE")) {
            erro("Esperado '{' após switch");
        }

        while (consumir("CASE")) {
            // Constante pode ser número ou char
            if (!tokenAtual.getTipo().equals("NUMBER") && !tokenAtual.getTipo().equals("CHAR")) {
                erro("Esperado constante no case");
            }
            avancar();
            if (!consumir("COLON")) {
                erro("Esperado ':' após constante");
            }
            // Comandos do case
            while (!tokenAtual.getTipo().equals("CASE")
                    && !tokenAtual.getTipo().equals("DEFAULT")
                    && !tokenAtual.getTipo().equals("RBRACE")) {
                comando();
            }
        }

        if (consumir("DEFAULT")) {
            if (!consumir("COLON")) {
                erro("Esperado ':' após default");
            }
            while (!tokenAtual.getTipo().equals("RBRACE")) {
                comando();
            }
        }

        if (!consumir("RBRACE")) {
            erro("Esperado '}' após switch");
        }
    }

    // <return_cmd> ::= RETURN <expressao> ';'
    @Override
    protected void return_cmd() throws IOException {
        consumir("RETURN");

        String tipoExpressao = verificarExpressao();
        if (tipoRetornoAtual != null) {
            verificarCompatibilidadeTipos(tipoRetornoAtual, tipoExpressao, "retorno");
        }

        if (!consumir("SEMICOLON")) {
            erro("Esperado ';' após return");
        }
    }

    // <atribuicao> ::= IDENTIFIER ('[' <expressao> ']')? '=' <expressao> ';'
    @Override
    protected void atribuicao() throws IOException {
        // Verifica se é incremento/decremento pré-fixo
        boolean isPrefix = false;
        if (tokenAtual.getTipo().equals("INCREMENT") || tokenAtual.getTipo().equals("DECREMENT")) {
            isPrefix = true;
            avancar();
        }

        // Identificador
        if (!tokenAtual.getTipo().equals("IDENTIFIER")) {
            erro("Esperado identificador");
            return;
        }

        String nomeVar = tokenAtual.getValor();
        Variavel var = escopos.buscarVariavel(nomeVar);
        if (var == null) {
            erroSemantico("Variável '" + nomeVar + "' não declarada");
        }
        avancar();

        // Verifica acesso a array
        if (consumir("LBRACKET")) {
            expressao();
            if (!consumir("RBRACKET")) {
                erro("Esperado ']' após índice do array");
            }
        }

        // Verifica operadores de atribuição
        if (isPrefix) {
            if (!consumir("SEMICOLON")) {
                erro("Esperado ';' após incremento/decremento");
            }
        } else if (tokenAtual.getTipo().equals("ASSIGN")
                || tokenAtual.getTipo().equals("ADD_ASSIGN")
                || tokenAtual.getTipo().equals("SUB_ASSIGN")
                || tokenAtual.getTipo().equals("MUL_ASSIGN")
                || tokenAtual.getTipo().equals("DIV_ASSIGN")) {

            String operador = tokenAtual.getTipo();
            avancar();

            String tipoExpressao = verificarExpressao();
            if (var != null) {
                verificarCompatibilidadeTipos(var.getTipo(), tipoExpressao, "atribuição com " + operador);
            }

            if (!consumir("SEMICOLON")) {
                erro("Esperado ';' após expressão");
            }
        } else if (tokenAtual.getTipo().equals("INCREMENT") || tokenAtual.getTipo().equals("DECREMENT")) {
            avancar();
            if (!consumir("SEMICOLON")) {
                erro("Esperado ';' após incremento/decremento");
            }
        } else {
            erro("Esperado operador de atribuição após identificador");
        }
    }

    // <chamada_funcao> ::= IDENTIFIER '(' [ <argumentos> ] ')'
    @Override
    protected void chamada_funcao() throws IOException {
        String nomeFuncao = tokenAtual.getValor();
        avancar();

        Funcao funcao = escopos.buscarFuncao(nomeFuncao);
        if (funcao == null) {
            erroSemantico("Função" + nomeFuncao + "' não declarada");
        }

        if (!consumir("LPAREN")) {
            erro("Esperado '(' após nome da função");
        }

        int contadorArgs = 0;
        if (!tokenAtual.getTipo().equals("RPAREN")) {
            do {
                String tipoArg = verificarExpressao();
                contadorArgs++;

                if (funcao != null && contadorArgs <= funcao.getParametros().size()) {
                    Parametro param = funcao.getParametros().get(contadorArgs - 1);
                    verificarCompatibilidadeTipos(param.getTipo(), tipoArg, "argumento");
                }
            } while (consumir("COMMA"));
        }

        if (funcao != null && contadorArgs != funcao.getParametros().size()) {
            erroSemantico("Número incorreto de argumentos para '" + nomeFuncao
                    + "'. Esperados: " + funcao.getParametros().size()
                    + ", fornecidos: " + contadorArgs);
        }

        if (!consumir("RPAREN")) {
            erro("Esperado ')' após argumentos");
        }
    }

    private void verificarCompatibilidadeTipos(String tipoEsperado, String tipoRecebido, String contexto) {
        // Lógica simplificada de verificação de tipos
        if (!tipoEsperado.equals(tipoRecebido)) {
            erroSemantico("Incompatibilidade de tipos em " + contexto
                    + ". Esperado: " + tipoEsperado + ", Recebido: " + tipoRecebido);
        }
    }

    private void verificarCondicao(String tipo) {
        if (!tipo.equals("bool") && !tipo.equals("int")) {
            erroSemantico("Condição deve ser booleana ou numérica. Tipo recebido: " + tipo);
        }
    }

    // <argumentos> ::= <expressao> (',' <expressao>)*
    @Override
    protected void argumentos() throws IOException {
        do {
            expressao();
        } while (consumir("COMMA"));
    }

    // Nova regra: <especificador_tipo> ::= VOID | CHAR | SHORT | INT | LONG | FLOAT | DOUBLE | SIGNED | UNSIGNED | STRUCT IDENTIFIER
    @Override
    protected String especificador_tipo() throws IOException {
        StringBuilder tipo = new StringBuilder();
        boolean hasType = false;
        boolean isStruct = false;

        while (tokenAtual.getTipo().matches("VOID|CHAR|SHORT|INT|LONG|FLOAT|DOUBLE|SIGNED|UNSIGNED|STRUCT")) {
            hasType = true;

            if (tokenAtual.getTipo().equals("STRUCT")) {
                isStruct = true;
                tipo.append("struct ");
                consumir("STRUCT");
                if (!consumir("IDENTIFIER")) {
                    erro("Esperado nome da struct após 'struct'");
                } else {
                    tipo.append(tokenAtual.getValor());
                }
                break;
            } else {
                tipo.append(tokenAtual.getValor()).append(" ");
                avancar();
            }
        }

        if (!hasType) {
            erro("Tipo inválido");
            return "desconhecido";
        }

        return isStruct ? tipo.toString() : tipo.toString().trim();
    }

    // <bloco> ::= '{' { <comando> }* '}'
    @Override
    protected void bloco() throws IOException {
        if (!consumir("LBRACE")) {
            erro("Esperado '{' para iniciar bloco");
        }

        // Abre novo escopo para o bloco
        escopos.abrirEscopo();

        while (!tokenAtual.getTipo().equals("RBRACE") && !tokenAtual.getTipo().equals("EOF")) {
            comando();
        }

        if (!consumir("RBRACE")) {
            erro("Esperado '}' para fechar bloco");
        }

        // Fecha escopo do bloco
        escopos.fecharEscopo();
    }

    // <operador_relacional> ::= '==' | '!=' | '<' | '>' | '<=' | '>='
    @Override
    protected boolean eOperadorRelacional() {
        return tokenAtual.getTipo().matches("EQUAL|NOT_EQUAL|LESS|GREATER|LESS_EQUAL|GREATER_EQUAL");
    }

    // <termo> ::= <fator> ( ('*' | '/') <fator> )*
    @Override
    protected void termo() throws IOException {
        fator();
        while (tokenAtual.getTipo().equals("MULTIPLY") || tokenAtual.getTipo().equals("DIVIDE")) {
            avancar();
            fator();
        }
    }

    // <atribuicao_sem_ponto> ::= IDENTIFIER ('=' | '+=' | '-=' | '*=' | '/=' | '++' | '--') <expressao>
    @Override
    protected void atribuicao_sem_ponto_e_virgula() throws IOException {
        consumir("IDENTIFIER");

        if (consumir("ASSIGN")) {
            expressao(); // aceita qualquer expressão
        } else if (consumir("INCREMENT") || consumir("DECREMENT")) {
            // Não precisa de expressão (a++ ou a--)
        } else if (consumir("ADD_ASSIGN") || consumir("SUB_ASSIGN")
                || consumir("MUL_ASSIGN") || consumir("DIV_ASSIGN")) {
            expressao(); // aceita qualquer expressão após operador composto
        } else {
            erro("Esperado operador de atribuição após identificador");
        }
    }

    /* <fator> ::= NUMBER | IDENTIFIER | CHAR 
                | NUMBER_FLOAT | '(' <expressao> ')' 
                | '!' <fator> | <chamada_funcao>*/
    @Override
    protected void fator() throws IOException {
        elemento();

        // Tratamento de arrays após o elemento básico
        if (tokenAtual.getTipo().equals("LBRACKET")) {
            consumir("LBRACKET");
            expressao();
            if (!consumir("RBRACKET")) {
                erro("Esperado ']' após índice do array");
            }
        }
    }

    // Adicione este novo método à classe
    @Override
    protected void elemento() throws IOException {
        // Handle unary operators: *, &, !, -, ++ (pre), -- (pre)
        if (consumir("MULTIPLY") || consumir("BITWISE_AND") || consumir("NOT") || consumir("MINUS")
                || consumir("INCREMENT") || consumir("DECREMENT")) {
            elemento(); // Recursive call for unary operator
        } // Handle function calls
        else if (tokenAtual.getTipo().equals("IDENTIFIER")
                && pos + 1 < tokens.size()
                && tokens.get(pos + 1).getTipo().equals("LPAREN")) {
            chamada_funcao();
        } // Handle identifiers
        else if (tokenAtual.getTipo().equals("IDENTIFIER")) {
            consumir("IDENTIFIER");
        } // Handle literals: string, number, char, float
        else if (tokenAtual.getTipo().equals("STRING")
                || tokenAtual.getTipo().equals("NUMBER")
                || tokenAtual.getTipo().equals("CHAR")
                || tokenAtual.getTipo().equals("NUMBER_FLOAT")) {
            avancar();
        } // Handle parenthesized expressions
        else if (consumir("LPAREN")) {
            expressao();
            if (!consumir("RPAREN")) {
                erro("Esperado ')' após expressão");
                return; // Exit to prevent further processing
            }
        } else {
            erro("Elemento inválido: " + (tokenAtual != null ? tokenAtual.getValor() : "null"));
            if (tokenAtual != null) {
                avancar(); // Advance to avoid infinite loop
            }
            return; // Exit to prevent further processing
        }

        // Handle post-expression accesses: ., ->, [], ++ (post), -- (post)
        while (tokenAtual != null && (tokenAtual.getTipo().equals("DOT")
                || tokenAtual.getTipo().equals("ARROW")
                || tokenAtual.getTipo().equals("LBRACKET")
                || tokenAtual.getTipo().equals("INCREMENT")
                || tokenAtual.getTipo().equals("DECREMENT"))) {
            if (tokenAtual.getTipo().equals("LBRACKET")) {
                avancar(); // Consume '['
                expressao(); // Parse array index
                if (!consumir("RBRACKET")) {
                    erro("Esperado ']' após índice do array");
                    return; // Exit to prevent further processing
                }
            } else if (tokenAtual.getTipo().equals("DOT") || tokenAtual.getTipo().equals("ARROW")) {
                avancar(); // Consume '.' or '->'
                if (!consumir("IDENTIFIER")) {
                    erro("Esperado identificador após acesso");
                    return; // Exit to prevent further processing
                }
            } else {
                // Post-increment/decrement
                avancar(); // Consume ++ or --
            }
        }
    }

// Novo método para acesso a array
    protected void acesso_array() throws IOException {
        consumir("IDENTIFIER");
        consumir("LBRACKET");
        expressao();
        if (!consumir("RBRACKET")) {
            erro("Esperado ']' após índice do array");
        }
    }

    @Override
    protected void ponteiro() throws IOException {
        while (consumir("MULTIPLY")) {
            // Consome múltiplos '*' (ex: int **ptr)
        }
    }

    private String verificarExpressao() throws IOException {
        return verificarExpressaoTernaria();
    }

    private String verificarExpressaoTernaria() throws IOException {
        String tipo = verificarExpressaoLogica();

        if (consumir("QUESTION")) {
            String tipoVerdadeiro = verificarExpressao();
            if (!consumir("COLON")) {
                erro("Esperado ':' no operador ternário");
            }
            String tipoFalso = verificarExpressao();

            // Verificar compatibilidade dos tipos
            if (!tiposCompativeis(tipoVerdadeiro, tipoFalso)) {
                erroSemantico("Tipos incompatíveis no operador ternário: " + tipoVerdadeiro + " e " + tipoFalso);
            }
            return tipoVerdadeiro; // Retorna o tipo do primeiro ramo
        }
        return tipo;
    }

    private String verificarExpressaoLogica() throws IOException {
        String tipo = verificarExpressaoRelacional();

        while (tokenAtual.getTipo().equals("AND") || tokenAtual.getTipo().equals("OR")) {
            avancar();
            String tipoDir = verificarExpressaoRelacional();

            // Verificar se são tipos lógicos válidos
            if (!ehTipoBooleano(tipo) || !ehTipoBooleano(tipoDir)) {
                erroSemantico("Operandos lógicos devem ser booleanos");
            }
            tipo = "int"; // Em C, operações lógicas retornam int (0 ou 1)
        }
        return tipo;
    }

    private String verificarExpressaoRelacional() throws IOException {
        String tipo = verificarExpressaoAritmetica();

        if (eOperadorRelacional()) {
            avancar();
            String tipoDir = verificarExpressaoAritmetica();

            // Verificar compatibilidade numérica
            if (!ehTipoNumerico(tipo) || !ehTipoNumerico(tipoDir)) {
                erroSemantico("Operandos relacionais devem ser numéricos");
            }
            tipo = "int"; // Resultado é inteiro (0 ou 1)
        }
        return tipo;
    }

    private boolean ehTipoBooleano(String tipo) {
        return tipo.equals("int");
    }

    private String verificarExpressaoAritmetica() throws IOException {
        String tipo = verificarTermo();

        while (tokenAtual.getTipo().equals("PLUS") || tokenAtual.getTipo().equals("MINUS")) {
            avancar();
            String tipoDir = verificarTermo();

            // Verificar tipos numéricos
            if (!ehTipoNumerico(tipo) || !ehTipoNumerico(tipoDir)) {
                erroSemantico("Operandos aritméticos devem ser numéricos");
            }

            // Determinar tipo resultante (promoção de tipos)
            tipo = determinarTipoResultante(tipo, tipoDir);
        }
        return tipo;
    }

    private boolean ehTipoCondicionalValido(String tipo) {
        return ehTipoNumerico(tipo);
    }

    private String verificarTermo() throws IOException {
        String tipo = verificarFator();

        while (tokenAtual.getTipo().equals("MULTIPLY") || tokenAtual.getTipo().equals("DIVIDE")) {
            avancar();
            String tipoDir = verificarFator();

            if (!ehTipoNumerico(tipo) || !ehTipoNumerico(tipoDir)) {
                erroSemantico("Operandos devem ser numéricos");
            }
            tipo = determinarTipoResultante(tipo, tipoDir);
        }
        return tipo;
    }

    private String verificarFator() throws IOException {
        String tipo = verificarElemento();

        // Verificar acesso a array
        if (tokenAtual.getTipo().equals("LBRACKET")) {
            consumir("LBRACKET");
            String tipoIndice = verificarExpressao();

            if (!tipoIndice.equals("int")) {
                erroSemantico("Índice de array deve ser inteiro");
            }

            if (!consumir("RBRACKET")) {
                erro("Esperado ']' após índice do array");
            }

            // Se é array, retorna o tipo base
            tipo = obterTipoBaseArray(tipo);
        }
        return tipo;
    }

    private String verificarElemento() throws IOException {
        // Handle unary operators: *, &, !, -, ++ (pre), -- (pre)
        if (consumir("MULTIPLY") || consumir("BITWISE_AND") || consumir("NOT") || consumir("MINUS")
                || consumir("INCREMENT") || consumir("DECREMENT")) {
            String operador = tokens.get(pos - 1).getTipo(); // Get the consumed operator
            String tipoOperando = verificarElemento(); // Recursive call for operand

            switch (operador) {
                case "MULTIPLY":
                    return obterTipoBasePonteiro(tipoOperando); // Dereference
                case "BITWISE_AND":
                    return tipoOperando + "*"; // Address-of
                case "NOT":
                case "MINUS":
                    if (!ehTipoNumerico(tipoOperando)) {
                        erroSemantico("Operando unário inválido para " + operador);
                    }
                    return tipoOperando;
                case "INCREMENT":
                case "DECREMENT":
                    if (!ehTipoNumerico(tipoOperando)) {
                        erroSemantico("Operando de incremento/decremento deve ser numérico");
                    }
                    return tipoOperando;
                default:
                    return tipoOperando;
            }
        } // Handle function calls
        else if (tokenAtual.getTipo().equals("IDENTIFIER")
                && pos + 1 < tokens.size()
                && tokens.get(pos + 1).getTipo().equals("LPAREN")) {
            String nomeFuncao = tokenAtual.getValor();
            Funcao funcao = escopos.buscarFuncao(nomeFuncao);
            chamada_funcao(); // Parse function call
            if (funcao == null) {
                erroSemantico("Função '" + nomeFuncao + "' não declarada");
                return "desconhecido";
            }
            return funcao.getTipoRetorno(); // Return function's return type
        } // Handle identifiers
        else if (tokenAtual.getTipo().equals("IDENTIFIER")) {
            String nome = tokenAtual.getValor();
            Variavel var = escopos.buscarVariavel(nome);
            if (var == null) {
                erroSemantico("Variável '" + nome + "' não declarada");
                avancar();
                return "desconhecido";
            }
            String tipo = var.getTipo();
            avancar(); // Consume identifier

            // Process struct accesses (. or ->)
            while (tokenAtual != null
                    && (tokenAtual.getTipo().equals("DOT") || tokenAtual.getTipo().equals("ARROW"))) {
                boolean isPointerAccess = tokenAtual.getTipo().equals("ARROW");
                avancar(); // Consume . or ->
                if (tokenAtual == null || !tokenAtual.getTipo().equals("IDENTIFIER")) {
                    erro("Esperado identificador após acesso");
                    return "desconhecido";
                }
                String campo = tokenAtual.getValor();
                avancar(); // Consume field identifier
                if (isPointerAccess) {
                    if (!tipo.endsWith("*")) {
                        erroSemantico("Acesso '->' em não-ponteiro");
                        tipo = "desconhecido";
                        continue;
                    }
                    tipo = tipo.substring(0, tipo.length() - 1).trim(); // Remove *
                }
                if (!tipo.startsWith("struct ")) {
                    erroSemantico("Acesso a campo em tipo não estruturado: " + tipo);
                    tipo = "desconhecido";
                    continue;
                }
                String nomeStruct = tipo.substring(7); // Remove "struct "
                String tipoCampo = escopos.buscarTipoCampoStruct(nomeStruct, campo);
                if (tipoCampo == null) {
                    erroSemantico("Struct '" + nomeStruct + "' não possui campo '" + campo + "'");
                    tipo = "desconhecido";
                } else {
                    tipo = tipoCampo; // Update type to field type
                }
            }
            return tipo;
        } // Handle literals
        else if (tokenAtual.getTipo().equals("STRING")) {
            avancar();
            return "char*";
        } else if (tokenAtual.getTipo().equals("NUMBER")) {
            avancar();
            return "int";
        } else if (tokenAtual.getTipo().equals("NUMBER_FLOAT")) {
            avancar();
            return "float";
        } else if (tokenAtual.getTipo().equals("CHAR")) {
            avancar();
            return "char";
        } // Handle parenthesized expressions
        else if (consumir("LPAREN")) {
            String tipo = verificarExpressao();
            if (!consumir("RPAREN")) {
                erro("Esperado ')' após expressão");
                return "desconhecido";
            }
            return tipo;
        } else {
            erro("Elemento inválido: " + (tokenAtual != null ? tokenAtual.getValor() : "null"));
            if (tokenAtual != null) {
                avancar();
            }
            return "desconhecido";
        }

        // Handle post-expression accesses: [], ++, --
    }

// Métodos auxiliares
    private boolean ehTipoNumerico(String tipo) {
        return tipo.matches("char|short|int|long|float|double");
    }

    private boolean tiposCompativeis(String tipo1, String tipo2) {
        // Lógica simplificada de compatibilidade
        if (tipo1.equals(tipo2)) {
            return true;
        }
        if (tipo1.equals("double") && tipo2.matches("float|int|char")) {
            return true;
        }
        if (tipo1.equals("float") && tipo2.matches("int|char")) {
            return true;
        }
        if (tipo1.equals("long") && tipo2.matches("int|char")) {
            return true;
        }
        if (tipo1.equals("int") && tipo2.equals("char")) {
            return true;
        }
        return false;
    }

    private String determinarTipoResultante(String tipo1, String tipo2) {
        // Promoção de tipos para operações aritméticas
        if (tipo1.equals("double") || tipo2.equals("double")) {
            return "double";
        }
        if (tipo1.equals("float") || tipo2.equals("float")) {
            return "float";
        }
        if (tipo1.equals("long") || tipo2.equals("long")) {
            return "long";
        }
        return "int";
    }

    private String obterTipoBaseArray(String tipo) {
        // Remove dimensões de arrays (ex: int[10] -> int)
        if (tipo.contains("[")) {
            return tipo.substring(0, tipo.indexOf('['));
        }
        return tipo;
    }

    private String obterTipoBasePonteiro(String tipo) {
        // Remove um nível de ponteiro (ex: int** -> int*)
        if (tipo.endsWith("*")) {
            return tipo.substring(0, tipo.length() - 1);
        }
        return "desconhecido";
    }

}
