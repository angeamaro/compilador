package analyzer;

import Tokens.*;
import java.io.IOException;
import java.util.List;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import models.*;
import abstracts.AParser;

public class Parser extends AParser {

    protected Token tokenAtual;
    protected int pos = 0;
    protected TabelaDeTokens tabela;
    protected int countErros = 0;
    protected List<Token> tokens;
    private Escopo escopos = new Escopo();
    private String funcaoAtual = null; // For return verification
    private String tipoRetornoAtual = null; // For return verification

    public Parser(TabelaDeTokens tabela) {
        if (tabela == null) {
            throw new IllegalArgumentException("Token table cannot be null");
        }

        this.tabela = tabela;
        this.tokens = tabela.getTokens();

        if (tokens == null) {
            throw new IllegalArgumentException("Token list cannot be null");
        }

        if (!tokens.isEmpty()) {
            this.tokenAtual = tokens.get(0);
        } else {
            this.tokenAtual = new Token("EOF", "End of File", -1, -1);
        }
    }

    @Override
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
            tokenAtual = new Token("EOF", "End of File", -1, -1);
        }
    }

    @Override
    protected void erro(String msg) throws IOException {
        System.err.println("[Error] " + msg + " found '" + tokenAtual.getValor()
                + "' at line " + tokenAtual.getLinha()
                + ", column " + tokenAtual.getColuna());
        countErros++;
        sincronizar();
    }

    @Override
    protected void erroSemantico(String mensagem) {
        int linha = tokenAtual != null ? tokenAtual.getLinha() : -1;
        int coluna = tokenAtual != null ? tokenAtual.getColuna() : -1;
        System.err.println("[Semantic Error] " + mensagem + " at line " + linha + ", column " + coluna);
        countErros++;
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
    }

    // <programa> ::= { <declaracao_global> }*
    @Override
    public void parse() throws IOException {
        escopos.abrirEscopo(); // Global scope
        programa();
        if (!tokenAtual.getTipo().equals("EOF")) {
            erro("Unexpected tokens remaining");
        }
        escopos.fecharEscopo();
    }

    // <programa> ::= { <declaracao_global> }*
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

                if (pos + 2 < tokens.size()
                        && (tokens.get(pos + 1).getTipo().equals("IDENTIFIER") || tokens.get(pos + 1).getTipo().equals("MAIN"))
                        && tokens.get(pos + 2).getTipo().equals("LPAREN")) {

                    declaracao_funcao();
                } else {
                    declaracao();
                }
            } else {
                erro("Invalid declaration in global scope");
                avancar();
            }
        }
    }

    // <declaracao_struct> ::= STRUCT IDENTIFIER LBRACE { <campo_struct> }* RBRACE [SEMICOLON]
    @Override
    protected void declaracao_struct() throws IOException {
        consumir("STRUCT");
        if (!tokenAtual.getTipo().equals("IDENTIFIER")) {
            erro("Expected struct name");
            sincronizar();
            return;
        }
        String nomeStruct = tokenAtual.getValor();
        avancar();

        if (escopos.buscarStruct(nomeStruct) != null) {
            erroSemantico("Struct '" + nomeStruct + "' already declared");
            sincronizar();
            return;
        }

        Struct struct = new Struct(nomeStruct);

        if (!consumir("LBRACE")) {
            erro("Expected '{' after struct name");
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
                erro("Expected field identifier");
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
                                erroSemantico("Array size must be positive: " + valor);
                            }
                        } catch (NumberFormatException e) {
                            erroSemantico("Invalid array size: " + valor);
                        }
                        avancar();
                    } else if (tokenAtual.getTipo().equals("IDENTIFIER")) {
                        String nomeConstante = tokenAtual.getValor();
                        Variavel var = escopos.buscarVariavel(nomeConstante);
                        if (var == null || !var.getTipo().equals("int")) {
                            erroSemantico("Constant '" + nomeConstante + "' is not a valid integer");
                        }
                        avancar();
                    } else {
                        erro("Expected array size (number or identifier)");
                    }
                    if (!consumir("RBRACKET")) {
                        erro("Expected ']' after array size");
                        sincronizar();
                        return;
                    }
                } else {
                    avancar();
                }
                tipoCompleto.append("[]");
            }

            if (struct.getCampos().containsKey(nomeCampo)) {
                erroSemantico("Field '" + nomeCampo + "' already defined in struct '" + nomeStruct + "'");
            } else {
                if (!ehTipoValido(tipoCompleto.toString())) {
                    erroSemantico("Invalid type for field '" + nomeCampo + "': " + tipoCompleto);
                } else {
                    struct.getCampos().put(nomeCampo, tipoCompleto.toString());
                }
            }

            if (!consumir("SEMICOLON")) {
                erro("Expected ';' after struct field");
                sincronizar();
                return;
            }
        }

        if (!consumir("RBRACE")) {
            erro("Expected '}' after struct fields");
            sincronizar();
            return;
        }
        if (!consumir("SEMICOLON")) {
            erro("Expected ';' after struct declaration");
            sincronizar();
        }

        escopos.adicionarStruct(nomeStruct, struct);
    }

    // <declaracao_funcao> ::= <especificador_tipo> <ponteiro> (IDENTIFIER | MAIN) LPAREN [ <parametros> ] RPAREN <bloco>
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
            erro("Expected function name");
            return;
        }

        Funcao funcao = new Funcao(nomeFuncao, tipoRetorno, tokenAtual.getLinha());

        funcaoAtual = nomeFuncao;
        tipoRetornoAtual = tipoRetorno;

        if (!consumir("LPAREN")) {
            erro("Expected '(' after function name");
            return;
        }

        escopos.abrirEscopo();

        List<Parametro> parametros = new ArrayList<>();
        if (!tokenAtual.getTipo().equals("RPAREN")) {
            parametros = parametros();
        }
        if (!consumir("RPAREN")) {
            erro("Expected ')' after parameters");
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

    // <parametros> ::= <parametro> { COMMA <parametro> }*
    @Override
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
                erro("Expected parameter identifier");
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

    // <comando> ::= <declaracao> | <if_cmd> | <while_cmd> | <for_cmd> | <do_while_cmd> | <switch_cmd> | <printf_cmd> | <scanf_cmd> | <return_cmd> | <atribuicao> | <chamada_funcao_cmd> | BREAK SEMICOLON | CONTINUE SEMICOLON
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
                erro("Expected ';' after 'break'");
            }
        } else if (tokenAtual.getTipo().equals("CONTINUE")) {
            consumir("CONTINUE");
            if (!consumir("SEMICOLON")) {
                erro("Expected ';' after 'continue'");
            }
        } else if (tokenAtual.getTipo().equals("IDENTIFIER")
                && tokens.get(pos + 1).getTipo().equals("LPAREN")) {
            chamada_funcao();
            if (!consumir("SEMICOLON")) {
                erro("Expected ';' after function call");
            }
        } else if (tokenAtual.getTipo().equals("IDENTIFIER")) {
            atribuicao();
        } else {
            erro("Invalid statement");
        }
    }

    // <declaracao> ::= [ CONST ] <especificador_tipo> <ponteiro> <lista_identificadores> SEMICOLON
    @Override
    protected void declaracao() throws IOException {
        boolean isConst = consumir("CONST");
        String tipo = especificador_tipo();
        ponteiro();
        lista_identificadores(tipo);
        if (!consumir("SEMICOLON")) {
            erro("Expected ';' after declaration");
        }
    }

    // <lista_identificadores> ::= <identificador_decl> { COMMA <identificador_decl> }*
    @Override
    protected void lista_identificadores(String tipo) throws IOException {
        do {
            if (!tokenAtual.getTipo().equals("IDENTIFIER")) {
                erro("Expected identifier");
                return;
            }

            String nome = tokenAtual.getValor();
            avancar();

            boolean isArray = false;
            if (consumir("LBRACKET")) {
                isArray = true;
                if (!tokenAtual.getTipo().equals("NUMBER") && !tokenAtual.getTipo().equals("IDENTIFIER")) {
                    erro("Expected array size");
                }
                avancar();
                if (!consumir("RBRACKET")) {
                    erro("Expected ']' after array size");
                }
            }

            Variavel var = new Variavel(nome, tipo, false, isArray, tokenAtual.getLinha());
            escopos.adicionarVariavel(nome, var);

            if (consumir("ASSIGN")) {
                String tipoExpressao = verificarExpressao();
                verificarCompatibilidadeTipos(tipo, tipoExpressao, "assignment");
            }
        } while (consumir("COMMA"));
    }

    // <expressao> ::= <expressao_ternaria>
    @Override
    protected void expressao() throws IOException {
        expressao_ternaria();
    }

    // <expressao_ternaria> ::= <expressao_logica> [ QUESTION <expressao> COLON <expressao> ]

    /**
     *
     * @throws IOException
     */
    @Override
    protected void expressao_ternaria() throws IOException {
        expressao_logica();
        if (consumir("QUESTION")) {
            expressao();
            if (!consumir("COLON")) {
                erro("Expected ':' in ternary operator");
            }
            expressao();
        }
    }

    // <expressao_logica> ::= <expressao_relacional> { (AND | OR) <expressao_relacional> }*
    @Override
    protected void expressao_logica() throws IOException {
        expressao_relacional();
        while (tokenAtual.getTipo().equals("AND") || tokenAtual.getTipo().equals("OR")) {
            avancar();
            expressao_relacional();
        }
    }

    // <expressao_relacional> ::= <expressao_aritmetica> [ <op_relacional> <expressao_aritmetica> ]
    @Override
    protected void expressao_relacional() throws IOException {
        expressao_aritmetica();
        while (eOperadorRelacional()) {
            avancar();
            expressao_aritmetica();
        }
    }

    // <expressao_aritmetica> ::= <termo> { (PLUS | MINUS) <termo> }*
    @Override
    protected void expressao_aritmetica() throws IOException {
        termo();
        while (tokenAtual.getTipo().equals("PLUS") || tokenAtual.getTipo().equals("MINUS")) {
            avancar();
            termo();
        }
    }

    // <if_cmd> ::= IF LPAREN <expressao> RPAREN <bloco> [ ELSE <bloco> ]
    @Override
    protected void if_cmd() throws IOException {
        consumir("IF");
        if (!consumir("LPAREN")) {
            erro("Expected '(' after 'if'");
        }
        String tipoCondicao = verificarExpressao();
        verificarCondicao(tipoCondicao);

        if (!consumir("RPAREN")) {
            erro("Expected ')' after expression");
        }
        bloco();

        if (consumir("ELSE")) {
            bloco();
        }
    }

    // <while_cmd> ::= WHILE LPAREN <expressao> RPAREN <bloco>
    @Override
    protected void while_cmd() throws IOException {
        consumir("WHILE");
        if (!consumir("LPAREN")) {
            erro("Expected '(' after 'while'");
        }

        String tipoCondicao = verificarExpressao();
        verificarCondicao(tipoCondicao);

        if (!consumir("RPAREN")) {
            erro("Expected ')' after expression");
        }
        bloco();
    }

    // <for_cmd> ::= FOR LPAREN <inicializacao_for> <expressao> SEMICOLON <atribuicao_sem_ponto_e_virgula> RPAREN <bloco>
    @Override
    protected void for_cmd() throws IOException {
        consumir("FOR");
        if (!consumir("LPAREN")) {
            erro("Expected '(' after 'for'");
        }
        if (tokenAtual.getTipo().matches("INT|FLOAT|DOUBLE|CHAR|VOID|STRUCT")) {
            especificador_tipo();
            ponteiro();
        }
        atribuicao();
        expressao();
        if (!consumir("SEMICOLON")) {
            erro("Expected ';' after 'for' condition");
        }
        atribuicao_sem_ponto_e_virgula();
        if (!consumir("RPAREN")) {
            erro("Expected ')' after 'for'");
        }
        bloco();
    }

    // <especificador_tipo> ::= {<tipo_simples>}+ | STRUCT IDENTIFIER
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
                if (!tokenAtual.getTipo().equals("IDENTIFIER")) {
                    erro("Expected struct name after 'struct'");
                    return "unknown";
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
            erro("Invalid type specifier");
            return "unknown";
        }

        return isStruct ? tipo.toString() : tipo.toString().trim();
    }

    // <printf_cmd> ::= IDENTIFIER LPAREN <expressao> { COMMA <expressao> }* RPAREN SEMICOLON
    @Override
    protected void printf_cmd() throws IOException {
        consumir("IDENTIFIER"); // "printf"
        if (!consumir("LPAREN")) {
            erro("Expected '(' after 'printf'");
            sincronizar();
            return;
        }

        // Expect a string literal as the first argument
        if (!tokenAtual.getTipo().equals("STRING")) {
            erro("Expected format string as first argument of 'printf'");
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
                erroSemantico("Incompatible type for format specifier '" + specifier +
                        "': expected " + expectedType + ", got " + argType);
            }
        }

        if (!consumir("RPAREN")) {
            erro("Expected ')' after 'printf' arguments");
            sincronizar();
            return;
        }
        if (!consumir("SEMICOLON")) {
            erro("Expected ';' after 'printf'");
            sincronizar();
            return;
        }
    }

    // <scanf_cmd> ::= IDENTIFIER LPAREN STRING { COMMA BITWISE_AND IDENTIFIER }* RPAREN SEMICOLON
    @Override
    protected void scanf_cmd() throws IOException {
        consumir("IDENTIFIER");
        if (!consumir("LPAREN")) {
            erro("Expected '(' after 'scanf'");
        }
        if (!tokenAtual.getTipo().equals("STRING")) {
            erro("Expected format string in 'scanf'");
        }
        avancar();

        while (consumir("COMMA")) {
            if (!consumir("BITWISE_AND")) {
                erro("Expected '&' before identifier in 'scanf'");
            }
            if (!consumir("IDENTIFIER")) {
                erro("Expected identifier after '&'");
            }
        }

        if (!consumir("RPAREN")) {
            erro("Expected ')' after 'scanf'");
        }
        if (!consumir("SEMICOLON")) {
            erro("Expected ';' after 'scanf'");
        }
    }

    // <do_while_cmd> ::= DO <bloco> WHILE LPAREN <expressao> RPAREN SEMICOLON
    @Override
    protected void do_while_cmd() throws IOException {
        consumir("DO");
        bloco();
        if (!consumir("WHILE")) {
            erro("Expected 'while' after 'do' block");
        }
        if (!consumir("LPAREN")) {
            erro("Expected '(' after 'while'");
        }
        expressao();
        if (!consumir("RPAREN")) {
            erro("Expected ')' after expression");
        }
        if (!consumir("SEMICOLON")) {
            erro("Expected ';' after 'do-while'");
        }
    }

    // <switch_cmd> ::= SWITCH LPAREN <expressao> RPAREN LBRACE { <case_bloco> }* [ <default_bloco> ] RBRACE
    @Override
    protected void switch_cmd() throws IOException {
        consumir("SWITCH");
        if (!consumir("LPAREN")) {
            erro("Expected '(' after 'switch'");
        }
        expressao();
        if (!consumir("RPAREN")) {
            erro("Expected ')' after expression");
        }
        if (!consumir("LBRACE")) {
            erro("Expected '{' after 'switch'");
        }

        while (consumir("CASE")) {
            if (!tokenAtual.getTipo().equals("NUMBER") && !tokenAtual.getTipo().equals("CHAR")) {
                erro("Expected constant in 'case'");
            }
            avancar();
            if (!consumir("COLON")) {
                erro("Expected ':' after constant");
            }
            while (!tokenAtual.getTipo().equals("CASE")
                    && !tokenAtual.getTipo().equals("DEFAULT")
                    && !tokenAtual.getTipo().equals("RBRACE")) {
                comando();
            }
        }

        if (consumir("DEFAULT")) {
            if (!consumir("COLON")) {
                erro("Expected ':' after 'default'");
            }
            while (!tokenAtual.getTipo().equals("RBRACE")) {
                comando();
            }
        }

        if (!consumir("RBRACE")) {
            erro("Expected '}' after 'switch'");
        }
    }

    // <return_cmd> ::= RETURN <expressao> SEMICOLON
    @Override
    protected void return_cmd() throws IOException {
        consumir("RETURN");

        String tipoExpressao = verificarExpressao();
        if (tipoRetornoAtual != null) {
            verificarCompatibilidadeTipos(tipoRetornoAtual, tipoExpressao, "return");
        }

        if (!consumir("SEMICOLON")) {
            erro("Expected ';' after 'return'");
        }
    }

    // <atribuicao> ::= ( (INCREMENT | DECREMENT) <lvalue> | <lvalue> ( <op_atribuicao> <expressao> | INCREMENT | DECREMENT ) ) SEMICOLON
    @Override
    protected void atribuicao() throws IOException {
        boolean isPrefix = false;
        if (tokenAtual.getTipo().equals("INCREMENT") || tokenAtual.getTipo().equals("DECREMENT")) {
            isPrefix = true;
            avancar();
        }

        String tipoLValue = parseLValuePath();

        if (tipoLValue == null || tipoLValue.equals("unknown")) {
            erro("Invalid lvalue for assignment");
            return;
        }

        if (isPrefix) {
            if (!consumir("SEMICOLON")) {
                erro("Expected ';' after increment/decrement");
            }
        } else if (tokenAtual.getTipo().equals("ASSIGN")
                || tokenAtual.getTipo().equals("ADD_ASSIGN")
                || tokenAtual.getTipo().equals("SUB_ASSIGN")
                || tokenAtual.getTipo().equals("MUL_ASSIGN")
                || tokenAtual.getTipo().equals("DIV_ASSIGN")) {

            String operador = tokenAtual.getTipo();
            avancar();

            String tipoExpressao = verificarExpressao();
            verificarCompatibilidadeTipos(tipoLValue, tipoExpressao, "assignment with " + operador);

            if (!consumir("SEMICOLON")) {
                erro("Expected ';' after expression");
            }
        } else if (tokenAtual.getTipo().equals("INCREMENT") || tokenAtual.getTipo().equals("DECREMENT")) {
            avancar();
            if (!consumir("SEMICOLON")) {
                erro("Expected ';' after increment/decrement");
            }
        } else {
            erro("Expected assignment operator after identifier");
        }
    }

    // <lvalue> ::= IDENTIFIER [ LBRACKET <expressao> RBRACKET ]
    private String parseLValuePath() throws IOException {
        if (!tokenAtual.getTipo().equals("IDENTIFIER")) {
            erro("Expected identifier for assignment");
            return "unknown";
        }

        String nomeBase = tokenAtual.getValor();
        Variavel var = escopos.buscarVariavel(nomeBase);
        if (var == null) {
            erroSemantico("Variable '" + nomeBase + "' undeclared");
        }
        String tipoAtual = var != null ? var.getTipo() : "unknown";
        avancar();

        OUTER:
        while (tokenAtual != null) {
            switch (tokenAtual.getTipo()) {
                case "LBRACKET":
                    avancar();
                    String tipoIndice = verificarExpressao();
                    if (!tipoIndice.equals("int") && !tipoIndice.equals("number")) {
                        erroSemantico("Array index must be an integer");
                    }   if (!consumir("RBRACKET")) {
                        erro("Expected ']' after array index");
                    }   break;
                case "DOT":
                case "ARROW":
                    String operador = tokenAtual.getTipo().equals("ARROW") ? "->" : ".";
                    boolean isPointerAccess = tokenAtual.getTipo().equals("ARROW");
                    avancar();
                    if (!tokenAtual.getTipo().equals("IDENTIFIER")) {
                        erro("Expected identifier after '" + operador + "'");
                        return "unknown";
                    }   String campo = tokenAtual.getValor();
                    avancar();
                    if (isPointerAccess) {
                        if (!tipoAtual.endsWith("*")) {
                            erroSemantico("Invalid use of '->' on non-pointer '" + nomeBase + "' (type: " + tipoAtual + "). Use '.' for struct access");
                        }
                        tipoAtual = tipoAtual.substring(0, tipoAtual.length() - 1).trim();
                    }   if (!tipoAtual.startsWith("struct ")) {
                        erroSemantico("Field access on non-struct type: '" + nomeBase + "' (type: " + tipoAtual + ")");
                        return "unknown";
                    }   String nomeStruct = tipoAtual.substring(7);
                    Struct struct = escopos.buscarStruct(nomeStruct);
                    if (struct == null) {
                        erroSemantico("Struct '" + nomeStruct + "' not defined");
                        return "unknown";
                    }   String tipoCampo = struct.getCampos().get(campo);
                    if (tipoCampo == null) {
                        erroSemantico("Field '" + campo + "' not defined in struct '" + nomeStruct + "' for '" + nomeBase + operador + campo + "'");
                        return "unknown";
                    }   tipoAtual = tipoCampo;
                    break;
                default:
                    break OUTER;
            }
        }

        return tipoAtual;
    }

    // <chamada_funcao> ::= IDENTIFIER LPAREN [ <argumentos> ] RPAREN
    @Override
    protected void chamada_funcao() throws IOException {
        String nomeFuncao = tokenAtual.getValor();
        avancar();

        Funcao funcao = escopos.buscarFuncao(nomeFuncao);
        if (funcao == null) {
            erroSemantico("Function '" + nomeFuncao + "' undeclared");
        }

        if (!consumir("LPAREN")) {
            erro("Expected '(' after function name");
        }

        int contadorArgs = 0;
        if (!tokenAtual.getTipo().equals("RPAREN")) {
            do {
                String tipoArg = verificarExpressao();
                contadorArgs++;

                if (funcao != null && contadorArgs <= funcao.getParametros().size()) {
                    Parametro param = funcao.getParametros().get(contadorArgs - 1);
                    verificarCompatibilidadeTipos(param.getTipo(), tipoArg, "argument");
                }
            } while (consumir("COMMA"));
        }

        if (funcao != null && contadorArgs != funcao.getParametros().size()) {
            erroSemantico("Incorrect number of arguments for '" + nomeFuncao
                    + "'. Expected: " + funcao.getParametros().size()
                    + ", provided: " + contadorArgs);
        }

        if (!consumir("RPAREN")) {
            erro("Expected ')' after arguments");
        }
    }

    @Override
    protected void verificarCompatibilidadeTipos(String tipoEsperado, String tipoRecebido, String contexto) {
        if (!tipoEsperado.equals(tipoRecebido)) {
            erroSemantico("Type mismatch in " + contexto
                    + ". Expected: " + tipoEsperado + ", got: " + tipoRecebido);
        }
    }

    @Override
    protected void verificarCondicao(String tipo) {
        if (!tipo.equals("bool") && !tipo.equals("int")) {
            erroSemantico("Condition must be boolean or numeric. Got: " + tipo);
        }
    }

    // <argumentos> ::= <expressao> { COMMA <expressao> }*
    @Override
    protected void argumentos() throws IOException {
        do {
            expressao();
        } while (consumir("COMMA"));
    }

    // <bloco> ::= LBRACE { <comando> }* RBRACE
    @Override
    protected void bloco() throws IOException {
        if (!consumir("LBRACE")) {
            erro("Expected '{' to start block");
        }

        escopos.abrirEscopo();

        while (!tokenAtual.getTipo().equals("RBRACE") && !tokenAtual.getTipo().equals("EOF")) {
            comando();
        }

        if (!consumir("RBRACE")) {
            erro("Expected '}' to close block");
        }

        escopos.fecharEscopo();
    }

    // <op_relacional> ::= EQUAL | NOT_EQUAL | LESS | GREATER | LESS_EQUAL | GREATER_EQUAL
    @Override
    protected boolean eOperadorRelacional() {
        return tokenAtual.getTipo().matches("EQUAL|NOT_EQUAL|LESS|GREATER|LESS_EQUAL|GREATER_EQUAL");
    }

    // <termo> ::= <fator> { (MULTIPLY | DIVIDE) <fator> }*
    @Override
    protected void termo() throws IOException {
        fator();
        while (tokenAtual.getTipo().equals("MULTIPLY") || tokenAtual.getTipo().equals("DIVIDE")) {
            avancar();
            fator();
        }
    }

    // <atribuicao_sem_ponto_e_virgula> ::= IDENTIFIER ( <op_atribuicao> <expressao> | INCREMENT | DECREMENT )
    @Override
    protected void atribuicao_sem_ponto_e_virgula() throws IOException {
        consumir("IDENTIFIER");

        if (consumir("ASSIGN")) {
            expressao();
        } else if (consumir("INCREMENT") || consumir("DECREMENT")) {
        } else if (consumir("ADD_ASSIGN") || consumir("SUB_ASSIGN")
                || consumir("MUL_ASSIGN") || consumir("DIV_ASSIGN")) {
            expressao();
        } else {
            erro("Expected assignment operator after identifier");
        }
    }

    // <fator> ::= <elemento> [ LBRACKET <expressao> RBRACKET ]
    @Override
    protected void fator() throws IOException {
        elemento();

        if (tokenAtual.getTipo().equals("LBRACKET")) {
            consumir("LBRACKET");
            expressao();
            if (!consumir("RBRACKET")) {
                erro("Expected ']' after array index");
            }
        }
    }

    // <elemento> ::= (MULTIPLY | BITWISE_AND | NOT | MINUS) <elemento> | <chamada_funcao> | IDENTIFIER { (DOT | ARROW) IDENTIFIER }* | NUMBER | NUMBER_FLOAT | CHAR | STRING | LPAREN <expressao> RPAREN | (INCREMENT | DECREMENT) IDENTIFIER
    @Override
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
                        erro("Expected ']' after array index");
                        sincronizar();
                        return;
                    }
                } else if (tokenAtual.getTipo().equals("DOT") || tokenAtual.getTipo().equals("ARROW")) {
                    String operador = tokenAtual.getTipo().equals("ARROW") ? "->" : ".";
                    avancar();
                    if (!tokenAtual.getTipo().equals("IDENTIFIER")) {
                        erro("Expected identifier after '" + operador + "' in '" + nome + "'");
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
                erro("Expected ')' after expression");
                sincronizar();
                return;
            }
        } else {
            erro("Invalid element: " + (tokenAtual != null ? tokenAtual.getValor() : "null"));
            if (tokenAtual != null) {
                avancar();
            }
            sincronizar();
            return;
        }
    }

    // <ponteiro> ::= { MULTIPLY }*
    @Override
    protected void ponteiro() throws IOException {
        while (consumir("MULTIPLY")) {
        }
    }

    @Override
    protected String verificarExpressao() throws IOException {
        return verificarExpressaoTernaria();
    }

    @Override
    protected String verificarExpressaoTernaria() throws IOException {
        String tipo = verificarExpressaoLogica();

        if (consumir("QUESTION")) {
            String tipoVerdadeiro = verificarExpressao();
            if (!consumir("COLON")) {
                erro("Expected ':' in ternary operator");
            }
            String tipoFalso = verificarExpressao();

            if (!tiposCompativeis(tipoVerdadeiro, tipoFalso)) {
                erroSemantico("Incompatible types in ternary operator: " + tipoVerdadeiro + " and " + tipoFalso);
            }
            return tipoVerdadeiro;
        }
        return tipo;
    }

    @Override
    protected String verificarExpressaoLogica() throws IOException {
        String tipo = verificarExpressaoRelacional();

        while (tokenAtual.getTipo().equals("AND") || tokenAtual.getTipo().equals("OR")) {
            avancar();
            String tipoDir = verificarExpressaoRelacional();

            if (!ehTipoBooleano(tipo) || !ehTipoBooleano(tipoDir)) {
                erroSemantico("Logical operands must be boolean");
            }
            tipo = "int";
        }
        return tipo;
    }

    @Override
    protected String verificarExpressaoRelacional() throws IOException {
        String tipo = verificarExpressaoAritmetica();

        if (eOperadorRelacional()) {
            avancar();
            String tipoDir = verificarExpressaoAritmetica();

            if (!ehTipoNumerico(tipo) || !ehTipoNumerico(tipoDir)) {
                erroSemantico("Relational operands must be numeric");
            }
            tipo = "int";
        }
        return tipo;
    }

    private boolean ehTipoBooleano(String tipo) {
        return tipo.equals("int");
    }

    @Override
    protected String verificarExpressaoAritmetica() throws IOException {
        String tipo = verificarTermo();

        while (tokenAtual.getTipo().equals("PLUS") || tokenAtual.getTipo().equals("MINUS")) {
            avancar();
            String tipoDir = verificarTermo();

            if (!ehTipoNumerico(tipo) || !ehTipoNumerico(tipoDir)) {
                erroSemantico("Arithmetic operands must be numeric");
            }

            tipo = determinarTipoResultante(tipo, tipoDir);
        }
        return tipo;
    }

    @Override
    protected String verificarTermo() throws IOException {
        String tipo = verificarFator();

        while (tokenAtual.getTipo().equals("MULTIPLY") || tokenAtual.getTipo().equals("DIVIDE")) {
            avancar();
            String tipoDir = verificarFator();

            if (!ehTipoNumerico(tipo) || !ehTipoNumerico(tipoDir)) {
                erroSemantico("Arithmetic operands must be numeric");
            }
            tipo = determinarTipoResultante(tipo, tipoDir);
        }
        return tipo;
    }

    @Override
    protected String verificarFator() throws IOException {
        String tipo = verificarElemento();

        if (tokenAtual.getTipo().equals("LBRACKET")) {
            consumir("LBRACKET");
            String tipoIndice = verificarExpressao();

            if (!tipoIndice.equals("int")) {
                erroSemantico("Array index must be an integer");
            }

            if (!consumir("RBRACKET")) {
                erro("Expected ']' after array index");
            }

            tipo = obterTipoBaseArray(tipo);
        }
        return tipo;
    }

    @Override
    protected String verificarElemento() throws IOException {
        if (consumir("MULTIPLY") || consumir("BITWISE_AND") || consumir("NOT") || consumir("MINUS")
                || consumir("INCREMENT") || consumir("DECREMENT")) {
            String operador = tokens.get(pos - 1).getTipo();
            String tipoOperando = verificarElemento();

            switch (operador) {
                case "MULTIPLY":
                    if (!tipoOperando.endsWith("*")) {
                        erroSemantico("Dereference operator '*' requires a pointer, got: " + tipoOperando);
                        return "unknown";
                    }
                    return obterTipoBasePonteiro(tipoOperando);
                case "BITWISE_AND":
                    return tipoOperando + "*";
                case "NOT":
                case "MINUS":
                    if (!ehTipoNumerico(tipoOperando)) {
                        erroSemantico("Invalid unary operand for '" + operador + "': " + tipoOperando);
                    }
                    return tipoOperando;
                case "INCREMENT":
                case "DECREMENT":
                    if (!ehTipoNumerico(tipoOperando)) {
                        erroSemantico("Increment/decrement operand must be numeric, got: " + tipoOperando);
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
                erroSemantico("Function '" + nomeFuncao + "' undeclared");
                return "unknown";
            }
            return funcao.getTipoRetorno();
        } else if (tokenAtual.getTipo().equals("IDENTIFIER")) {
            String nome = tokenAtual.getValor();
            Variavel var = escopos.buscarVariavel(nome);
            if (var == null) {
                erroSemantico("Variable '" + nome + "' undeclared");
                avancar();
                return "unknown";
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
                        erroSemantico("Array index must be an integer, got: " + tipoIndice);
                    }
                    if (!consumir("RBRACKET")) {
                        erro("Expected ']' after array index");
                        sincronizar();
                        return "unknown";
                    }
                    if (tipoAtual.endsWith("[]")) {
                        tipoAtual = tipoAtual.substring(0, tipoAtual.length() - 2).trim();
                    } else if (tipoAtual.endsWith("*")) {
                        tipoAtual = tipoAtual.substring(0, tipoAtual.length() - 1).trim();
                    } else {
                        erroSemantico("Invalid array indexing on non-array: " + tipoAtual);
                        tipoAtual = "unknown";
                    }
                } else if (tokenAtual.getTipo().equals("DOT") || tokenAtual.getTipo().equals("ARROW")) {
                    boolean isPointerAccess = tokenAtual.getTipo().equals("ARROW");
                    String operador = isPointerAccess ? "->" : ".";
                    avancar();

                    if (!tokenAtual.getTipo().equals("IDENTIFIER")) {
                        erroSemantico("Expected identifier after '" + operador + "' in '" + nome + "'");
                        if (tokenAtual != null) {
                            avancar();
                        }
                        return "unknown";
                    }
                    String campo = tokenAtual.getValor();
                    avancar();

                    if (isPointerAccess && !tipoAtual.endsWith("*")) {
                        erroSemantico("Invalid use of '->' on non-pointer '" + nome + "' (type: " + tipoAtual + "). Use '.' for struct access");
                        tipoAtual = "unknown";
                        continue;
                    }
                    if (!isPointerAccess && tipoAtual.endsWith("*")) {
                        erroSemantico("Invalid use of '.' on pointer '" + nome + "' (type: " + tipoAtual + "). Use '->' for struct access");
                        tipoAtual = "unknown";
                        continue;
                    }

                    if (isPointerAccess) {
                        tipoAtual = tipoAtual.substring(0, tipoAtual.length() - 1).trim();
                    }

                    if (!tipoAtual.startsWith("struct ")) {
                        erroSemantico("Field access on non-struct type: '" + nome + "' (type: " + tipoAtual + ")");
                        tipoAtual = "unknown";
                        continue;
                    }
                    String nomeStruct = tipoAtual.substring(7).replaceAll("\\*|\\[\\]", "").trim();

                    Struct struct = escopos.buscarStruct(nomeStruct);
                    if (struct == null) {
                        erroSemantico("Struct '" + nomeStruct + "' not defined for '" + nome + operador + campo + "'");
                        tipoAtual = "unknown";
                        continue;
                    }

                    String tipoCampo = struct.getCampos().get(campo);
                    if (tipoCampo == null) {
                        erroSemantico("Field '" + campo + "' not defined in struct '" + nomeStruct + "' for '" + nome + operador + campo + "'");
                        tipoAtual = "unknown";
                    } else {
                        tipoAtual = tipoCampo;
                    }
                } else {
                    if (!ehTipoNumerico(tipoAtual)) {
                        erroSemantico("Increment/decrement operand must be numeric, got: " + tipoAtual);
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
                erro("Expected ')' after expression");
                sincronizar();
                return "unknown";
            }
            return tipo;
        } else {
            erro("Invalid element: " + (tokenAtual != null ? tokenAtual.getValor() : "null"));
            if (tokenAtual != null) {
                avancar();
            }
            sincronizar();
            return "unknown";
        }
    }

    @Override
    protected boolean ehTipoNumerico(String tipo) {
        return tipo.matches("char|short|int|long|float|double");
    }

    @Override
    protected boolean ehTipoValido(String tipo) {
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

    @Override
    protected boolean tiposCompativeis(String tipo1, String tipo2) {
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

    @Override
    protected String determinarTipoResultante(String tipo1, String tipo2) {
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

    @Override
    protected String obterTipoBaseArray(String tipo) {
        if (tipo.contains("[")) {
            return tipo.substring(0, tipo.indexOf('['));
        }
        return tipo;
    }

    @Override
    protected String obterTipoBasePonteiro(String tipo) {
        if (tipo.endsWith("*")) {
            return tipo.substring(0, tipo.length() - 1);
        }
        return "unknown";
    }

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
                return "unknown";
        }
    }

    private boolean isCompatibleType(String expectedType, String actualType) {
        if (expectedType.equals(actualType)) {
            return true;
        }
        if (expectedType.equals("char*") && actualType.equals("char[]")) {
            return true;
        }
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