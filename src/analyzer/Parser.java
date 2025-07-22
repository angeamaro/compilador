package analyzer;

import Tokens.*;
import java.io.IOException;
import java.util.List;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import models.*;

public class Parser {

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

    
    protected boolean consumir(String esperado) throws IOException {
        if (tokenAtual.getTipo().equals(esperado)) {
            avancar();
            return true;
        }
        return false;
    }

    
    protected void avancar() {
        pos++;
        if (pos < tokens.size()) {
            tokenAtual = tokens.get(pos);
        } else {
            tokenAtual = new Token("EOF", "Fim de Arquivo", -1, -1);
        }
    }

    
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

    
    protected void sincronizar() {
        while (!tokenAtual.getTipo().equals("SEMICOLON")
                && !tokenAtual.getTipo().equals("RBRACE")
                && !tokenAtual.getTipo().equals("EOF")) {
            avancar();
        }
        if (!tokenAtual.getTipo().equals("EOF")) {
            avancar();
        }
    }

    
    public void parse() throws IOException {
        escopos.abrirEscopo(); // Escopo global
        programa();
        if (!tokenAtual.getTipo().equals("EOF")) {
            erro("Tokens restantes inesperados");
        }
        escopos.fecharEscopo();
    }

    
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

    
    protected void declaracao_struct() throws IOException {
        consumir("STRUCT");
        if (!tokenAtual.getTipo().equals("IDENTIFIER")) {
            erro("Esperado nome da struct");
            sincronizar();
            return;
        }
        String nomeStruct = tokenAtual.getValor();
        avancar();

        if (escopos.buscarStruct(nomeStruct) != null) {
            erroSemantico("Struct '" + nomeStruct + "' já declarada");
            sincronizar();
            return;
        }

        Struct struct = new Struct(nomeStruct);

        if (!consumir("LBRACE")) {
            erro("Esperado '{' após nome da struct");
            sincronizar();
            return;
        }

        while (tokenAtual.getTipo().matches("VOID|CHAR|SHORT|INT|LONG|FLOAT|DOUBLE|SIGNED|UNSIGNED|STRUCT")) {
            String tipoCampo = especificador_tipo();
            StringBuilder tipoCompleto = new StringBuilder(tipoCampo);

            while (consumir("MULTIPLY")) {
                tipoCompleto.append("*");
            }

            if (!tokenAtual.getTipo().equals("IDENTIFIER")) {
                erro("Esperado identificador do campo");
                sincronizar();
                return;
            }
            String nomeCampo = tokenAtual.getValor();
            avancar();

            boolean isArray = false;
            if (consumir("LBRACKET")) {
                isArray = true;
                if (!tokenAtual.getTipo().equals("RBRACKET")) {
                    if (tokenAtual.getTipo().equals("NUMBER")) {
                        String valor = tokenAtual.getValor();
                        try {
                            int tamanho = Integer.parseInt(valor);
                            if (tamanho <= 0) {
                                erroSemantico("Tamanho do array deve ser positivo: " + valor);
                            }
                        } catch (NumberFormatException e) {
                            erroSemantico("Tamanho do array inválido: " + valor);
                        }
                        avancar();
                    } else if (tokenAtual.getTipo().equals("IDENTIFIER")) {
                        String nomeConstante = tokenAtual.getValor();
                        Variavel var = escopos.buscarVariavel(nomeConstante);
                        if (var == null || !var.getTipo().equals("int")) {
                            erroSemantico("Constante '" + nomeConstante + "' não é um inteiro válido");
                        }
                        avancar();
                    } else {
                        erro("Esperado tamanho do array (número ou identificador)");
                    }
                    if (!consumir("RBRACKET")) {
                        erro("Esperado ']' após tamanho do array");
                        sincronizar();
                        return;
                    }
                } else {
                    avancar();
                }
                tipoCompleto.append("[]");
            }

            if (struct.getCampos().containsKey(nomeCampo)) {
                erroSemantico("Campo '" + nomeCampo + "' já definido na struct '" + nomeStruct + "'");
            } else {
                if (!ehTipoValido(tipoCompleto.toString())) {
                    erroSemantico("Tipo inválido para campo '" + nomeCampo + "': " + tipoCompleto);
                } else {
                    struct.getCampos().put(nomeCampo, tipoCompleto.toString());
                }
            }

            if (!consumir("SEMICOLON")) {
                erro("Esperado ';' após campo da struct");
                sincronizar();
                return;
            }
        }

        if (!consumir("RBRACE")) {
            erro("Esperado '}' após campos da struct");
            sincronizar();
            return;
        }
        if (!consumir("SEMICOLON")) {
            erro("Esperado ';' após declaração da struct");
            sincronizar();
        }

        escopos.adicionarStruct(nomeStruct, struct);
    }

    
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

        Funcao funcao = new Funcao(nomeFuncao, tipoRetorno, tokenAtual.getLinha());

        funcaoAtual = nomeFuncao;
        tipoRetornoAtual = tipoRetorno;

        if (!consumir("LPAREN")) {
            erro("Esperado '(' após nome da função");
            return;
        }

        escopos.abrirEscopo();

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

        funcao.getParametros().addAll(parametros);
        escopos.adicionarFuncao(nomeFuncao, funcao);

        bloco();

        escopos.fecharEscopo();
        funcaoAtual = null;
        tipoRetornoAtual = null;
    }

    
    protected List<Parametro> parametros() throws IOException {
        List<Parametro> parametros = new ArrayList<>();
        do {
            String tipoParam = especificador_tipo();
            StringBuilder tipoCompleto = new StringBuilder(tipoParam);
            boolean isPonteiro = false;

            while (consumir("MULTIPLY")) {
                tipoCompleto.append("*");
                isPonteiro = true;
            }

            if (!tokenAtual.getTipo().equals("IDENTIFIER")) {
                erro("Esperado identificador do parâmetro");
                return parametros;
            }
            String nomeParam = tokenAtual.getValor();
            avancar();

            Parametro param = new Parametro(nomeParam, tipoCompleto.toString(), isPonteiro);
            parametros.add(param);

            Variavel var = new Variavel(nomeParam, tipoCompleto.toString(), false, false, tokenAtual.getLinha());
            escopos.adicionarVariavel(nomeParam, var);

        } while (consumir("COMMA"));
        return parametros;
    }

    
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

            Variavel var = new Variavel(nome, tipo, false, isArray, tokenAtual.getLinha());
            escopos.adicionarVariavel(nome, var);

            if (consumir("ASSIGN")) {
                String tipoExpressao = verificarExpressao();
                verificarCompatibilidadeTipos(tipo, tipoExpressao, "atribuição");
            }
        } while (consumir("COMMA"));
    }

    
    protected void expressao() throws IOException {
        expressao_ternaria();
    }

    
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

    
    protected void expressao_logica() throws IOException {
        expressao_relacional();
        while (tokenAtual.getTipo().equals("AND") || tokenAtual.getTipo().equals("OR")) {
            avancar();
            expressao_relacional();
        }
    }

    
    protected void expressao_relacional() throws IOException {
        expressao_aritmetica();
        while (eOperadorRelacional()) {
            avancar();
            expressao_aritmetica();
        }
    }

    
    protected void expressao_aritmetica() throws IOException {
        termo();
        while (tokenAtual.getTipo().equals("PLUS") || tokenAtual.getTipo().equals("MINUS")) {
            avancar();
            termo();
        }
    }

    
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

    
    protected void for_cmd() throws IOException {
        consumir("FOR");
        if (!consumir("LPAREN")) {
            erro("Esperado '(' após for");
        }
        if (tokenAtual.getTipo().matches("INT|FLOAT|DOUBLE|CHAR|VOID|STRUCT")) {
            especificador_tipo();
            ponteiro();
        }
        atribuicao();
        expressao();
        if (!consumir("SEMICOLON")) {
            erro("Esperado ';' após condição do for");
        }
        atribuicao_sem_ponto_e_virgula();
        if (!consumir("RPAREN")) {
            erro("Esperado ')' após for");
        }
        bloco();
    }

    
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
                if (!tokenAtual.getTipo().equals("IDENTIFIER")) {
                    erro("Esperado nome da struct após 'struct'");
                    return "desconhecido";
                }
                tipo.append(tokenAtual.getValor());
                avancar();
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

    
    protected void printf_cmd() throws IOException {
        consumir("IDENTIFIER"); // "printf"
        if (!consumir("LPAREN")) {
            erro("Esperado '(' após printf");
            sincronizar();
            return;
        }

        // Expect a string literal as the first argument
        if (!tokenAtual.getTipo().equals("STRING")) {
            erro("Esperado string de formato como primeiro argumento de printf");
            sincronizar();
            return;
        }

        // Parse format string and extract specifiers
        String formatString = tokenAtual.getValor();
        List<String> formatSpecifiers = new ArrayList<>();
        Pattern pattern = Pattern.compile("%[a-z]");
        Matcher matcher = pattern.matcher(formatString);
        while (matcher.find()) {
            formatSpecifiers.add(matcher.group());
        }
        avancar(); // Consume STRING

        // Parse and validate arguments
        List<String> argTypes = new ArrayList<>();
        if (tokenAtual.getTipo().equals("COMMA")) {
            while (consumir("COMMA")) {
                String tipoArg = verificarExpressao();
                argTypes.add(tipoArg);
            }
        }

        // Validate argument types against format specifiers
        for (int i = 0; i < Math.min(formatSpecifiers.size(), argTypes.size()); i++) {
            String specifier = formatSpecifiers.get(i);
            String argType = argTypes.get(i);
            String expectedType = getExpectedTypeForSpecifier(specifier);

            if (!isCompatibleType(expectedType, argType)) {
                erroSemantico("Tipo incompatível para formatador '" + specifier +
                        "': esperado " + expectedType + ", recebido " + argType);
            }
        }

        if (!consumir("RPAREN")) {
            erro("Esperado ')' após argumentos de printf");
            sincronizar();
            return;
        }
        if (!consumir("SEMICOLON")) {
            erro("Esperado ';' após printf");
            sincronizar();
            return;
        }
    }

    
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

    
    protected void do_while_cmd() throws IOException {
        consumir("DO");
        bloco();
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
            if (!tokenAtual.getTipo().equals("NUMBER") && !tokenAtual.getTipo().equals("CHAR")) {
                erro("Esperado constante no case");
            }
            avancar();
            if (!consumir("COLON")) {
                erro("Esperado ':' após constante");
            }
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

    
    protected void atribuicao() throws IOException {
        boolean isPrefix = false;
        if (tokenAtual.getTipo().equals("INCREMENT") || tokenAtual.getTipo().equals("DECREMENT")) {
            isPrefix = true;
            avancar();
        }

        String tipoLValue = parseLValuePath();

        if (tipoLValue == null || tipoLValue.equals("desconhecido")) {
            erro("Caminho inválido para atribuição");
            return;
        }

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
            verificarCompatibilidadeTipos(tipoLValue, tipoExpressao, "atribuição com " + operador);

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

    private String parseLValuePath() throws IOException {
        if (!tokenAtual.getTipo().equals("IDENTIFIER")) {
            erro("Esperado identificador para atribuição");
            return "desconhecido";
        }

        String nomeBase = tokenAtual.getValor();
        Variavel var = escopos.buscarVariavel(nomeBase);
        if (var == null) {
            erroSemantico("Variável '" + nomeBase + "' não declarada");
        }
        String tipoAtual = var != null ? var.getTipo() : "desconhecido";
        avancar();

        while (tokenAtual != null) {
            if (tokenAtual.getTipo().equals("LBRACKET")) {
                avancar();
                String tipoIndice = verificarExpressao();
                if (!tipoIndice.equals("int") && !tipoIndice.equals("number")) {
                    erroSemantico("Índice de array deve ser inteiro");
                }
                if (!consumir("RBRACKET")) {
                    erro("Esperado ']' após índice do array");
                }
            } else if (tokenAtual.getTipo().equals("DOT") || tokenAtual.getTipo().equals("ARROW")) {
                String operador = tokenAtual.getTipo().equals("ARROW") ? "->" : ".";
                boolean isPointerAccess = tokenAtual.getTipo().equals("ARROW");
                avancar();

                if (!tokenAtual.getTipo().equals("IDENTIFIER")) {
                    erro("Esperado identificador após '" + operador + "'");
                    return "desconhecido";
                }

                String campo = tokenAtual.getValor();
                avancar();

                if (isPointerAccess) {
                    if (!tipoAtual.endsWith("*")) {
                        erroSemantico("Acesso '->' em não-ponteiro");
                    }
                    tipoAtual = tipoAtual.substring(0, tipoAtual.length() - 1).trim();
                }

                if (!tipoAtual.startsWith("struct ")) {
                    erroSemantico("Acesso a campo em não-estrutura: " + tipoAtual);
                    return "desconhecido";
                }

                String nomeStruct = tipoAtual.substring(7);
                Struct struct = escopos.buscarStruct(nomeStruct);
                if (struct == null) {
                    erroSemantico("Struct não definida: " + nomeStruct);
                    return "desconhecido";
                }

                String tipoCampo = struct.getCampos().get(campo);
                if (tipoCampo == null) {
                    erroSemantico("Campo '" + campo + "' não existe em " + nomeStruct);
                    return "desconhecido";
                }

                tipoAtual = tipoCampo;
            } else {
                break;
            }
        }

        return tipoAtual;
    }

    
    protected void chamada_funcao() throws IOException {
        String nomeFuncao = tokenAtual.getValor();
        avancar();

        Funcao funcao = escopos.buscarFuncao(nomeFuncao);
        if (funcao == null) {
            erroSemantico("Função '" + nomeFuncao + "' não declarada");
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

    
    protected void argumentos() throws IOException {
        do {
            expressao();
        } while (consumir("COMMA"));
    }

    
    protected void bloco() throws IOException {
        if (!consumir("LBRACE")) {
            erro("Esperado '{' para iniciar bloco");
        }

        escopos.abrirEscopo();

        while (!tokenAtual.getTipo().equals("RBRACE") && !tokenAtual.getTipo().equals("EOF")) {
            comando();
        }

        if (!consumir("RBRACE")) {
            erro("Esperado '}' para fechar bloco");
        }

        escopos.fecharEscopo();
    }

    
    protected boolean eOperadorRelacional() {
        return tokenAtual.getTipo().matches("EQUAL|NOT_EQUAL|LESS|GREATER|LESS_EQUAL|GREATER_EQUAL");
    }

    
    protected void termo() throws IOException {
        fator();
        while (tokenAtual.getTipo().equals("MULTIPLY") || tokenAtual.getTipo().equals("DIVIDE")) {
            avancar();
            fator();
        }
    }

    
    protected void atribuicao_sem_ponto_e_virgula() throws IOException {
        consumir("IDENTIFIER");

        if (consumir("ASSIGN")) {
            expressao();
        } else if (consumir("INCREMENT") || consumir("DECREMENT")) {
        } else if (consumir("ADD_ASSIGN") || consumir("SUB_ASSIGN")
                || consumir("MUL_ASSIGN") || consumir("DIV_ASSIGN")) {
            expressao();
        } else {
            erro("Esperado operador de atribuição após identificador");
        }
    }

    
    protected void fator() throws IOException {
        elemento();

        if (tokenAtual.getTipo().equals("LBRACKET")) {
            consumir("LBRACKET");
            expressao();
            if (!consumir("RBRACKET")) {
                erro("Esperado ']' após índice do array");
            }
        }
    }

    
    protected void elemento() throws IOException {
        if (consumir("MULTIPLY") || consumir("BITWISE_AND") || consumir("NOT") || consumir("MINUS")
                || consumir("INCREMENT") || consumir("DECREMENT")) {
            elemento();
        } else if (tokenAtual.getTipo().equals("IDENTIFIER")
                && pos + 1 < tokens.size()
                && tokens.get(pos + 1).getTipo().equals("LPAREN")) {
            chamada_funcao();
        } else if (tokenAtual.getTipo().equals("IDENTIFIER")) {
            String nome = tokenAtual.getValor();
            avancar();

            while (tokenAtual != null && (tokenAtual.getTipo().equals("DOT")
                    || tokenAtual.getTipo().equals("ARROW")
                    || tokenAtual.getTipo().equals("LBRACKET")
                    || tokenAtual.getTipo().equals("INCREMENT")
                    || tokenAtual.getTipo().equals("DECREMENT"))) {
                if (tokenAtual.getTipo().equals("LBRACKET")) {
                    avancar();
                    expressao();
                    if (!consumir("RBRACKET")) {
                        erro("Esperado ']' após índice do array");
                        sincronizar();
                        return;
                    }
                } else if (tokenAtual.getTipo().equals("DOT") || tokenAtual.getTipo().equals("ARROW")) {
                    String operador = tokenAtual.getTipo().equals("ARROW") ? "->" : ".";
                    avancar();
                    if (!tokenAtual.getTipo().equals("IDENTIFIER")) {
                        erro("Esperado identificador após '" + operador + "' em '" + nome + "'");
                        sincronizar();
                        return;
                    }
                    avancar();
                } else {
                    avancar();
                }
            }
        } else if (tokenAtual.getTipo().equals("STRING")
                || tokenAtual.getTipo().equals("NUMBER")
                || tokenAtual.getTipo().equals("CHAR")
                || tokenAtual.getTipo().equals("NUMBER_FLOAT")) {
            avancar();
        } else if (consumir("LPAREN")) {
            expressao();
            if (!consumir("RPAREN")) {
                erro("Esperado ')' após expressão");
                sincronizar();
                return;
            }
        } else {
            erro("Elemento inválido: " + (tokenAtual != null ? tokenAtual.getValor() : "null"));
            if (tokenAtual != null) {
                avancar();
            }
            sincronizar();
            return;
        }
    }

    
    protected void ponteiro() throws IOException {
        while (consumir("MULTIPLY")) {
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

            if (!tiposCompativeis(tipoVerdadeiro, tipoFalso)) {
                erroSemantico("Tipos incompatíveis no operador ternário: " + tipoVerdadeiro + " e " + tipoFalso);
            }
            return tipoVerdadeiro;
        }
        return tipo;
    }

    private String verificarExpressaoLogica() throws IOException {
        String tipo = verificarExpressaoRelacional();

        while (tokenAtual.getTipo().equals("AND") || tokenAtual.getTipo().equals("OR")) {
            avancar();
            String tipoDir = verificarExpressaoRelacional();

            if (!ehTipoBooleano(tipo) || !ehTipoBooleano(tipoDir)) {
                erroSemantico("Operandos lógicos devem ser booleanos");
            }
            tipo = "int";
        }
        return tipo;
    }

    private String verificarExpressaoRelacional() throws IOException {
        String tipo = verificarExpressaoAritmetica();

        if (eOperadorRelacional()) {
            avancar();
            String tipoDir = verificarExpressaoAritmetica();

            if (!ehTipoNumerico(tipo) || !ehTipoNumerico(tipoDir)) {
                erroSemantico("Operandos relacionais devem ser numéricos");
            }
            tipo = "int";
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

            if (!ehTipoNumerico(tipo) || !ehTipoNumerico(tipoDir)) {
                erroSemantico("Operandos aritméticos devem ser numéricos");
            }

            tipo = determinarTipoResultante(tipo, tipoDir);
        }
        return tipo;
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

        if (tokenAtual.getTipo().equals("LBRACKET")) {
            consumir("LBRACKET");
            String tipoIndice = verificarExpressao();

            if (!tipoIndice.equals("int")) {
                erroSemantico("Índice de array deve ser inteiro");
            }

            if (!consumir("RBRACKET")) {
                erro("Esperado ']' após índice do array");
            }

            tipo = obterTipoBaseArray(tipo);
        }
        return tipo;
    }

    private String verificarElemento() throws IOException {
        if (consumir("MULTIPLY") || consumir("BITWISE_AND") || consumir("NOT") || consumir("MINUS")
                || consumir("INCREMENT") || consumir("DECREMENT")) {
            String operador = tokens.get(pos - 1).getTipo();
            String tipoOperando = verificarElemento();

            switch (operador) {
                case "MULTIPLY":
                    if (!tipoOperando.endsWith("*")) {
                        erroSemantico("Operador de desreferência '*' requer um ponteiro, recebido: " + tipoOperando);
                        return "desconhecido";
                    }
                    return obterTipoBasePonteiro(tipoOperando);
                case "BITWISE_AND":
                    return tipoOperando + "*";
                case "NOT":
                case "MINUS":
                    if (!ehTipoNumerico(tipoOperando)) {
                        erroSemantico("Operando unário inválido para " + operador + ": " + tipoOperando);
                    }
                    return tipoOperando;
                case "INCREMENT":
                case "DECREMENT":
                    if (!ehTipoNumerico(tipoOperando)) {
                        erroSemantico("Operando de incremento/decremento deve ser numérico, recebido: " + tipoOperando);
                    }
                    return tipoOperando;
                default:
                    return tipoOperando;
            }
        } else if (tokenAtual.getTipo().equals("IDENTIFIER")
                && pos + 1 < tokens.size()
                && tokens.get(pos + 1).getTipo().equals("LPAREN")) {
            String nomeFuncao = tokenAtual.getValor();
            Funcao funcao = escopos.buscarFuncao(nomeFuncao);
            chamada_funcao();
            if (funcao == null) {
                erroSemantico("Função '" + nomeFuncao + "' não declarada");
                return "desconhecido";
            }
            return funcao.getTipoRetorno();
        } else if (tokenAtual.getTipo().equals("IDENTIFIER")) {
            String nome = tokenAtual.getValor();
            Variavel var = escopos.buscarVariavel(nome);
            if (var == null) {
                erroSemantico("Variável '" + nome + "' não declarada");
                avancar();
                return "desconhecido";
            }
            String tipoAtual = var.getTipo();
            avancar();

            while (tokenAtual != null
                    && (tokenAtual.getTipo().equals("DOT")
                    || tokenAtual.getTipo().equals("ARROW")
                    || tokenAtual.getTipo().equals("LBRACKET")
                    || tokenAtual.getTipo().equals("INCREMENT")
                    || tokenAtual.getTipo().equals("DECREMENT"))) {
                if (tokenAtual.getTipo().equals("LBRACKET")) {
                    avancar();
                    String tipoIndice = verificarExpressao();
                    if (!tipoIndice.equals("int")) {
                        erroSemantico("Índice de array deve ser inteiro, recebido: " + tipoIndice);
                    }
                    if (!consumir("RBRACKET")) {
                        erro("Esperado ']' após índice do array");
                        sincronizar();
                        return "desconhecido";
                    }
                    if (tipoAtual.endsWith("[]")) {
                        tipoAtual = tipoAtual.substring(0, tipoAtual.length() - 2).trim();
                    } else if (tipoAtual.endsWith("*")) {
                        tipoAtual = tipoAtual.substring(0, tipoAtual.length() - 1).trim();
                    } else {
                        erroSemantico("Tentativa de indexação em não-array: " + tipoAtual);
                        tipoAtual = "desconhecido";
                    }
                } else if (tokenAtual.getTipo().equals("DOT") || tokenAtual.getTipo().equals("ARROW")) {
                    boolean isPointerAccess = tokenAtual.getTipo().equals("ARROW");
                    String operador = isPointerAccess ? "->" : ".";
                    avancar();

                    if (!tokenAtual.getTipo().equals("IDENTIFIER")) {
                        erroSemantico("Esperado identificador após '" + operador + "' em '" + nome + "'");
                        if (tokenAtual != null) {
                            avancar();
                        }
                        return "desconhecido";
                    }
                    String campo = tokenAtual.getValor();
                    avancar();

                    if (isPointerAccess && !tipoAtual.endsWith("*")) {
                        erroSemantico("Acesso '->' em não-ponteiro '" + nome + "' (tipo: " + tipoAtual + "). Use '.' para acessar campos.");
                        tipoAtual = "desconhecido";
                        continue;
                    }
                    if (!isPointerAccess && tipoAtual.endsWith("*")) {
                        erroSemantico("Acesso '.' em ponteiro '" + nome + "' (tipo: " + tipoAtual + "). Use '->' para acessar campos.");
                        tipoAtual = "desconhecido";
                        continue;
                    }

                    if (isPointerAccess) {
                        tipoAtual = tipoAtual.substring(0, tipoAtual.length() - 1).trim();
                    }

                    if (!tipoAtual.startsWith("struct ")) {
                        erroSemantico("Acesso a campo em não-estrutura: '" + nome + "' (tipo: " + tipoAtual + ")");
                        tipoAtual = "desconhecido";
                        continue;
                    }
                    String nomeStruct = tipoAtual.substring(7).replaceAll("\\*|\\[\\]", "").trim();

                    Struct struct = escopos.buscarStruct(nomeStruct);
                    if (struct == null) {
                        erroSemantico("Struct não definida: '" + nomeStruct + "' para '" + nome + operador + campo + "'");
                        tipoAtual = "desconhecido";
                        continue;
                    }

                    String tipoCampo = struct.getCampos().get(campo);
                    if (tipoCampo == null) {
                        erroSemantico("Campo '" + campo + "' não existe em struct '" + nomeStruct + "' para '" + nome + operador + campo + "'");
                        tipoAtual = "desconhecido";
                    } else {
                        tipoAtual = tipoCampo;
                    }
                } else {
                    if (!ehTipoNumerico(tipoAtual)) {
                        erroSemantico("Operando de incremento/decremento deve ser numérico, recebido: " + tipoAtual);
                    }
                    avancar();
                }
            }
            return tipoAtual;
        } else if (tokenAtual.getTipo().equals("STRING")) {
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
        } else if (consumir("LPAREN")) {
            String tipo = verificarExpressao();
            if (!consumir("RPAREN")) {
                erro("Esperado ')' após expressão");
                sincronizar();
                return "desconhecido";
            }
            return tipo;
        } else {
            erro("Elemento inválido: " + (tokenAtual != null ? tokenAtual.getValor() : "null"));
            if (tokenAtual != null) {
                avancar();
            }
            sincronizar();
            return "desconhecido";
        }
    }

    private boolean ehTipoNumerico(String tipo) {
        return tipo.matches("char|short|int|long|float|double");
    }

    private boolean ehTipoValido(String tipo) {
        if (tipo.matches("void|char|short|int|long|float|double|signed|unsigned"
                + "|unsigned int|unsigned char|unsigned short|unsigned long"
                + "|signed int|signed char|signed short int|signed long|number")) {
            return true;
        }
        if (tipo.startsWith("struct ")) {
            String nomeStruct = tipo.substring(7).replaceAll("\\*|\\[\\]", "").trim();
            return escopos.buscarStruct(nomeStruct) != null;
        }
        if (tipo.endsWith("*") || tipo.endsWith("[]")) {
            String baseTipo = tipo.replaceAll("\\*|\\[\\]", "").trim();
            return ehTipoValido(baseTipo);
        }
        return false;
    }

    private boolean tiposCompativeis(String tipo1, String tipo2) {
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
        if (tipo.contains("[")) {
            return tipo.substring(0, tipo.indexOf('['));
        }
        return tipo;
    }

    private String obterTipoBasePonteiro(String tipo) {
        if (tipo.endsWith("*")) {
            return tipo.substring(0, tipo.length() - 1);
        }
        return "desconhecido";
    }

    // Helper method to map format specifiers to expected types
    private String getExpectedTypeForSpecifier(String specifier) {
        switch (specifier) {
            case "%s":
                return "char*"; // Also compatible with char[]
            case "%d":
            case "%i":
                return "int";
            case "%u":
                return "unsigned int";
            case "%f":
                return "float";
            case "%lf":
                return "double";
            case "%c":
                return "char";
            default:
                return "desconhecido";
        }
    }

    // Helper method to check type compatibility for printf format specifiers
    private boolean isCompatibleType(String expectedType, String actualType) {
        if (expectedType.equals(actualType)) {
            return true;
        }
        // Allow char[] for %s
        if (expectedType.equals("char*") && actualType.equals("char[]")) {
            return true;
        }
        // Allow numeric promotions
        if (expectedType.equals("double") && actualType.matches("float|int|char")) {
            return true;
        }
        if (expectedType.equals("float") && actualType.matches("int|char")) {
            return true;
        }
        if (expectedType.equals("int") && actualType.matches("char|short|unsigned int")) {
            return true;
        }
        if (expectedType.equals("unsigned int") && actualType.matches("int|char|short")) {
            return true;
        }
        return false;
    }
}