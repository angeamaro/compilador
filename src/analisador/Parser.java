package analisador;

import Tokens.*;
import java.io.IOException;
import java.util.List;
import abstracts.AParser;

public class Parser extends AParser {

    protected Token tokenAtual;
    protected int pos = 0;
    protected TabelaDeTokens tabela;
    protected int countErros = 0;
    protected List<Token> tokens;

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
        System.err.println("Erro: " + msg + " na linha " + tokenAtual.getLinha() + " encontrado " + tokenAtual.getValor());
        countErros++;
        sincronizar();
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
        programa();
        if (!tokenAtual.getTipo().equals("EOF")) {
            erro("Tokens restantes inesperados");
        }
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
        especificador_tipo();
        ponteiro(); // <--- Adicionado ponteiro
        if (!consumir("IDENTIFIER") && !consumir("MAIN")) {
            erro("Esperado nome da função");
        }
        if (!consumir("LPAREN")) {
            erro("Esperado '(' após nome da função");
        }
        if (!tokenAtual.getTipo().equals("RPAREN")) {
            parametros();
        }
        if (!consumir("RPAREN")) {
            erro("Esperado ')' após parâmetros");
        }
        bloco();
    }

    // <parametros> ::= <especificador_tipo> IDENTIFIER (',' <especificador_tipo> IDENTIFIER)*
    @Override
    protected void parametros() throws IOException {
        do {
            especificador_tipo();
            ponteiro(); // <--- Adicionado ponteiro
            if (!consumir("IDENTIFIER")) {
                erro("Esperado identificador do parâmetro");
            }
        } while (consumir("COMMA"));
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
        especificador_tipo();
        ponteiro(); // <--- Adicionado ponteiro
        lista_identificadores();
        if (!consumir("SEMICOLON")) {
            erro("Esperado ';' após declaração");
        }
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
        expressao();
        if (!consumir("RPAREN")) {
            erro("Esperado ')' após expressão");
        }
        bloco();

        // Verifica else opcional
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
        expressao();
        if (!consumir("RPAREN")) {
            erro("Esperado ')' após expressão");
        }
        if (!consumir("LBRACE")) {
            erro("Esperado '{' após condição do 'while'");
        }
        while (!tokenAtual.getTipo().equals("RBRACE") && !tokenAtual.getTipo().equals("EOF")) {
            comando();
        }
        if (!consumir("RBRACE")) {
            erro("Esperado '}' após bloco do 'while'");
        }
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
        expressao();
        if (!consumir("SEMICOLON")) {
            erro("Esperado ';' após return");
        }
    }

    // <atribuicao> ::= IDENTIFIER ('[' <expressao> ']')? '=' <expressao> ';'
    @Override
    protected void atribuicao() throws IOException {
        // Verifica se é incremento/decremento pré-fixo (++x ou --x)
        boolean isPrefix = false;
        if (tokenAtual.getTipo().equals("INCREMENT") || tokenAtual.getTipo().equals("DECREMENT")) {
            isPrefix = true;
            avancar(); // Consome o operador (++ ou --)
        }

        // Identificador (variável ou array)
        if (!consumir("IDENTIFIER")) {
            erro("Esperado identificador");
        }

        // Verifica se é acesso a array (ex: v[0] = ...)
        if (consumir("LBRACKET")) {
            expressao();
            if (!consumir("RBRACKET")) {
                erro("Esperado ']' após índice do array");
            }
        }

        // Verifica operadores de atribuição
        if (isPrefix) {
            // Já consumimos o operador pré-fixo, só precisa do ponto e vírgula
            if (!consumir("SEMICOLON")) {
                erro("Esperado ';' após incremento/decremento");
            }
        } else if (tokenAtual.getTipo().equals("ASSIGN")
                || tokenAtual.getTipo().equals("ADD_ASSIGN")
                || tokenAtual.getTipo().equals("SUB_ASSIGN")
                || tokenAtual.getTipo().equals("MUL_ASSIGN")
                || tokenAtual.getTipo().equals("DIV_ASSIGN")) {

            avancar(); // Consome o operador de atribuição

            // Para operadores compostos, precisa de expressão
            if (!tokenAtual.getTipo().equals("INCREMENT")
                    && !tokenAtual.getTipo().equals("DECREMENT")) {
                expressao();
            }

            if (!consumir("SEMICOLON")) {
                erro("Esperado ';' após expressão");
            }
        } // Verifica incremento/decremento pós-fixo (x++ ou x--)
        else if (tokenAtual.getTipo().equals("INCREMENT") || tokenAtual.getTipo().equals("DECREMENT")) {
            avancar(); // Consome o operador (++ ou --)
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
        consumir("IDENTIFIER");
        if (!consumir("LPAREN")) {
            erro("Esperado '(' após nome da função");
        }
        if (!tokenAtual.getTipo().equals("RPAREN")) {
            argumentos();
        }
        if (!consumir("RPAREN")) {
            erro("Esperado ')' após argumentos");
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
    protected void especificador_tipo() throws IOException {
        boolean hasType = false;
        while (tokenAtual.getTipo().matches("VOID|CHAR|SHORT|INT|LONG|FLOAT|DOUBLE|SIGNED|UNSIGNED|STRUCT")) {
            hasType = true;
            if (tokenAtual.getTipo().equals("STRUCT")) {
                consumir("STRUCT");
                if (!consumir("IDENTIFIER")) {
                    erro("Esperado nome da struct após 'struct'");
                }
                break;
            } else {
                avancar();
            }
        }
        if (!hasType) {
            erro("Tipo inválido");
        }
    }

    // <bloco> ::= '{' { <comando> }* '}'
    @Override
    protected void bloco() throws IOException {
        if (!consumir("LBRACE")) {
            erro("Esperado '{' para iniciar bloco");
        }
        while (!tokenAtual.getTipo().equals("RBRACE") && !tokenAtual.getTipo().equals("EOF")) {
            comando();
        }
        if (!consumir("RBRACE")) {
            erro("Esperado '}' para fechar bloco");
        }
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
        // Adicionar tratamento para operadores unários: *, &, !, -
        if (consumir("MULTIPLY") || consumir("BITWISE_AND") || consumir("NOT") || consumir("MINUS")) {
            elemento(); // Operador unário
        } else if (tokenAtual.getTipo().equals("IDENTIFIER")
                && tokens.size() > pos + 1
                && tokens.get(pos + 1).getTipo().equals("LPAREN")) {
            chamada_funcao();
        } else if (tokenAtual.getTipo().equals("IDENTIFIER")) {
            consumir("IDENTIFIER");
            // Verificar acessos (. ou ->) após identificador
            while (tokenAtual.getTipo().equals("DOT") || tokenAtual.getTipo().equals("ARROW")) {
                avancar(); // Consome . ou ->
                if (!consumir("IDENTIFIER")) {
                    erro("Esperado identificador após acesso");
                }
            }
        } // Aceitar strings como elementos válidos
        else if (tokenAtual.getTipo().equals("STRING")) {
            avancar();
        } else if (tokenAtual.getTipo().equals("NUMBER")
                || tokenAtual.getTipo().equals("CHAR")
                || tokenAtual.getTipo().equals("NUMBER_FLOAT")) {
            avancar();
        } else if (consumir("LPAREN")) {
            expressao();
            if (!consumir("RPAREN")) {
                erro("Esperado ')' após expressão");
            }
        } else if (consumir("INCREMENT") || consumir("DECREMENT")) {
            // Operadores de incremento/decremento
        } else {
            erro("Elemento inválido: " + tokenAtual.getValor());
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

}
