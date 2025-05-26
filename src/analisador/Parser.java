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
        this.tabela = tabela;
        this.tokens = tabela.getTokens();
        if (tokens != null && !tokens.isEmpty()) {
            this.tokenAtual = tokens.get(0);
        } else {
            this.tokenAtual = new Token("EOF", "Fim de Arquivo", -1, -1);
        }
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
    protected void sincronizar() throws IOException {
        while (!tokenAtual.getTipo().equals("SEMICOLON")
                && !tokenAtual.getTipo().equals("RBRACE")
                && !tokenAtual.getTipo().equals("EOF")) {
            avancar();
        }
        avancar(); // Consome o token de sincronização
    }

    // Entrada do programa
    @Override
    public void parse() throws IOException {
        programa();
        if (!tokenAtual.getTipo().equals("EOF")) {
            erro("Tokens restantes inesperados");
        }
        System.out.println("Análise concluída. Total de erros: " + countErros);
    }

    // -------------------REGRAS DA GRAMÁTICA ---------------------//
    
    // <programa> ::= { ( declaracao_funcao | declaracao ) }*
    @Override
    public void programa() throws IOException {
        // Nova regra: { declaracao_funcao } funcao_main
        while (tokenAtual.getTipo().equals("INT")
                || tokenAtual.getTipo().equals("FLOAT")
                || tokenAtual.getTipo().equals("CHAR")
                || tokenAtual.getTipo().equals("VOID")) {

            if ((tokens.get(pos + 1).getTipo().equals("IDENTIFIER") || (tokens.get(pos + 1).getTipo().equals("MAIN")))
                    && tokens.get(pos + 2).getTipo().equals("LPAREN")) {
                declaracao_funcao();
            } else {
                declaracao();
            }
        }
        if (!tokenAtual.getTipo().equals("EOF")) {
            erro("Código após a função main não permitido");
        }
    }

    // <declaracao_funcao> ::= <tipo> (IDENTIFIER | MAIN) '(' [ <parametros> ] ')' <bloco>
    @Override
    protected void declaracao_funcao() throws IOException {
        tipo();
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

    // <parametros> ::= <tipo> IDENTIFIER (',' <tipo> IDENTIFIER)*
    @Override
    protected void parametros() throws IOException {
        do {
            tipo();
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
                || tokenAtual.getTipo().equals("INT")
                || tokenAtual.getTipo().equals("FLOAT")
                || tokenAtual.getTipo().equals("CHAR")) {
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

    // <declaracao> ::= [CONST] <tipo> <lista_identificadores> ';'
    @Override
    protected void declaracao() throws IOException {
        boolean isConst = consumir("CONST");
        if (isConst) {
            tipo();
        } else {
            tipo();
        }
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
        if (eOperadorRelacional()) {
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
        if (consumir("INT") || consumir("DOUBLE") || consumir("CHAR")) {
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
        consumir("IDENTIFIER");
        if (!consumir("LPAREN")) {
            erro("Esperado '(' após printf");
        }
        // Consome a string de formato
        if (!tokenAtual.getTipo().equals("STRING") && !tokenAtual.getTipo().equals("IDENTIFIER")) {
            erro("Esperado string ou identificador em printf");
        }
        avancar();
        // Consome variáveis se houver
        while (consumir("COMMA")) {
            expressao(); // Pode ser identificador ou expressão mais complexa
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
        // Aceita identificador ou acesso a array
        consumir("IDENTIFIER");

        // Verifica se é acesso a array (ex: v[0])
        if (consumir("LBRACKET")) {
            expressao(); // Índice do array
            if (!consumir("RBRACKET")) {
                erro("Esperado ']' após índice do array");
            }
        }

        if (!consumir("ASSIGN")) {
            erro("Esperado '=' após identificador");
        }
        expressao();
        if (!consumir("SEMICOLON")) {
            erro("Esperado ';' após expressão");
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

    // <tipo> ::= INT | FLOAT | CHAR
    @Override
    protected void tipo() throws IOException {
        if (!consumir("INT") && !consumir("FLOAT") && !consumir("CHAR")) {
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
            expressao(); // para casos como: a = b + 2
            return;
        }

        if (consumir("INCREMENT") || consumir("DECREMENT")) {
            return; // para casos como: a++ ou a--
        }

        if (consumir("ADD_ASSIGN") || consumir("SUB_ASSIGN")
                || consumir("MUL_ASSIGN") || consumir("DIV_ASSIGN")) {

            if (!(consumir("NUMBER") || consumir("NUMBER_FLOAT") || consumir("IDENTIFIER"))) {
                erro("Esperado número ou identificador após operador composto de atribuição");
            }
            return;
        }

        erro("Esperado operador de atribuição após identificador");
    }

    /* <fator> ::= NUMBER | IDENTIFIER | CHAR 
                | NUMBER_FLOAT | '(' <expressao> ')' 
                | '!' <fator> | <chamada_funcao>*/
    @Override
    protected void fator() throws IOException {
        if (consumir("NOT")) {
            fator();
        } else if (tokenAtual.getTipo().equals("IDENTIFIER")
                && tokens.get(pos + 1).getTipo().equals("LPAREN")) {
            chamada_funcao();
        } else if (tokenAtual.getTipo().equals("NUMBER")
                || tokenAtual.getTipo().equals("IDENTIFIER")
                || tokenAtual.getTipo().equals("CHAR")
                || tokenAtual.getTipo().equals("NUMBER_FLOAT")) {
            avancar();
        } else if (consumir("LPAREN")) {
            expressao();
            if (!consumir("RPAREN")) {
                erro("Esperado ')' após expressão");
            }
        } else {
            erro("Fator inválido");
        }
    }

}
