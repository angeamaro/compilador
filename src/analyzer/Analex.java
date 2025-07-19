package analyzer;

import Tokens.*;
import abstracts.AAnalex;
import java.io.*;
import java.util.*;

public class Analex extends AAnalex {

    protected BufferedReader reader;
    protected int currentChar;
    protected int linha = 1;
    protected int coluna = 0;
    protected TabelaDeTokens tabela;

    public Analex(String filePath) throws IOException {
        reader = new BufferedReader(new FileReader(filePath));
        tabela = new TabelaDeTokens();
        advance();
    }

    @Override
    protected void advance() throws IOException {
        currentChar = reader.read();
        if (currentChar == '\n') {
            //tabela.adicionarToken(new Token("NEWLINE", "\\n", linha, coluna));
            linha++;
            coluna = 0;
        } else {
            coluna++;
        }
    }

    @Override
    public TabelaDeTokens analisarCodigo() throws IOException {
        while (currentChar != -1) {
            char ch = (char) currentChar;

            // Ignorar espaços em branco
            if (Character.isWhitespace(ch)) {
                advance();
                continue;
            }

            // Diretivas de pré-processador (ex: #include <stdio.h>)
            if (ch == '#') {
                processarDiretiva();
                continue;
            }

            // Comentários
            if (ch == '/') {
                processarComentario();
                continue;
            }

            // Identificadores e palavras reservadas
            if (Character.isLetter(ch) || ch == '_') {
                processarIdentificador();
                continue;
            }

            // Números (inteiros ou floats)
            if (Character.isDigit(ch)) {
                processarNumero();
                continue;
            }

            // Strings
            if (ch == '"') {
                processarString();
                continue;
            }

            // Caracteres
            if (ch == '\'') {
                processarCaractere();
                continue;
            }

            // Operadores e símbolos
            if (processarOperadores()) {
                continue;
            }

            // Se nenhum token foi reconhecido, reportar erro
            System.err.printf("Erro léxico: Caractere '%c' não reconhecido (Linha %d, Coluna %d)\n", ch, linha, coluna);
            advance();
        }
        tabela.adicionarToken(new TokenPalavrasReservadas("EOF", linha, coluna));
        return tabela;
    }

    // Métodos auxiliares para cada categoria de token
    @Override
    protected void processarDiretiva() throws IOException {
        StringBuilder lex = new StringBuilder();
        int startCol = coluna;

        // Adiciona o '#'
        lex.append((char) currentChar);
        advance();

        // Ler o nome da diretiva (ex: "include")
        while (Character.isLetter(currentChar)) {
            lex.append((char) currentChar);
            advance();
        }

        // Ignorar espaços entre o nome da diretiva e o delimitador
        while (Character.isWhitespace(currentChar)) {
            advance();
        }

        // Ler o conteúdo entre '<' e '>' ou entre '"'
        if (currentChar == '<' || currentChar == '"') {
            char opening = (char) currentChar;
            char closing = (opening == '<') ? '>' : '"';

            lex.append(opening);
            advance();

            // Ler até o delimitador de fechamento
            while (currentChar != closing && currentChar != -1 && currentChar != '\n') {
                lex.append((char) currentChar);
                advance();
            }

            // Adicionar o delimitador de fechamento
            if (currentChar == closing) {
                lex.append((char) currentChar);
                advance();
            } else {
                System.err.println("Erro: Delimitador '" + closing + "' não fechado (Linha " + linha + ")");
            }
        }

        // Adicionar o token DIRECTIVE completo
        tabela.adicionarToken(new Token("DIRECTIVE", lex.toString(), linha, startCol));
    }

    @Override
    protected void processarComentario() throws IOException {
        int startCol = coluna;
        advance();

        if (currentChar == '/') {
            // Comentário de linha
            advance();
            while (currentChar != -1 && currentChar != '\n') {
                advance();
            }
        } else if (currentChar == '*') {
            // Comentário de bloco
            advance();
            while (currentChar != -1) {
                if (currentChar == '*') {
                    advance();
                    if (currentChar == '/') {
                        advance();
                        break;
                    }
                } else {
                    advance();
                }
            }
        } else {
            // Operador de divisão
            tabela.adicionarToken(new TokenOperadoresAritmeticos("/", linha, startCol));
        }
    }

    @Override
    protected void processarIdentificador() throws IOException {
        StringBuilder lex = new StringBuilder();
        int startCol = coluna;

        do {
            lex.append((char) currentChar);
            advance();
        } while (Character.isLetterOrDigit(currentChar) || currentChar == '_');

        String palavra = lex.toString();
        if (TokenPalavrasReservadas.palavrasReservadas.containsKey(palavra)) {
            tabela.adicionarToken(new TokenPalavrasReservadas(palavra, linha, startCol));
        } else {
            tabela.adicionarToken(new Token("IDENTIFIER", palavra, linha, startCol));
        }
    }

    @Override
    protected void processarNumero() throws IOException {
        StringBuilder lex = new StringBuilder();
        int startCol = coluna;
        boolean isFloat = false;

        // Parte inteira
        while (Character.isDigit(currentChar)) {
            lex.append((char) currentChar);
            advance();
        }

        // Parte decimal (se houver)
        if (currentChar == '.') {
            isFloat = true;
            lex.append('.');
            advance();

            if (!Character.isDigit(currentChar)) {
                System.err.printf("Erro: Número float inválido (Linha %d, Coluna %d)\n", linha, coluna);
                return;
            }

            while (Character.isDigit(currentChar)) {
                lex.append((char) currentChar);
                advance();
            }
        }

        // Verificar se há caracteres inválidos após o número (ex: 5a)
        if (Character.isLetter(currentChar)) {
            System.err.printf("Erro: Número seguido de letra (Linha %d, Coluna %d)\n", linha, coluna);
            return;
        }

        String tipo = isFloat ? "NUMBER_FLOAT" : "NUMBER";
        tabela.adicionarToken(new Token(tipo, lex.toString(), linha, startCol));
    }

    @Override
    protected boolean processarOperadores() throws IOException {
        int startCol = coluna;
        char ch = (char) currentChar;

        switch (ch) {
            // Operadores aritméticos e atribuição composta
            case '+':
                advance();
                if (currentChar == '=') {
                    tabela.adicionarToken(new TokenOperadoresAtribuicao("+=", linha, startCol));
                    advance();
                } else if (currentChar == '+') {
                    tabela.adicionarToken(new TokenOperadoresAritmeticos("++", linha, startCol));
                    advance();
                } else {
                    tabela.adicionarToken(new TokenOperadoresAritmeticos("+", linha, startCol));
                }
                return true;

            case '-':
                advance();
                if (currentChar == '=') {
                    tabela.adicionarToken(new TokenOperadoresAtribuicao("-=", linha, startCol));
                    advance();
                } else if (currentChar == '>') {
                    tabela.adicionarToken(new TokenSimbolos("->", linha, startCol));
                    advance();
                } else if (currentChar == '-') {
                    tabela.adicionarToken(new TokenOperadoresAritmeticos("--", linha, startCol));
                    advance();
                }else {
                    tabela.adicionarToken(new TokenOperadoresAritmeticos("-", linha, startCol));
                }
                    return true;

                
        case '*':
                advance();
                if (currentChar == '=') {
                    tabela.adicionarToken(new TokenOperadoresAtribuicao("*=", linha, startCol));
                    advance();
                } else {
                    tabela.adicionarToken(new TokenOperadoresAritmeticos("*", linha, startCol));
                }
                return true;

            case '/':
                advance();
                if (currentChar == '=') {
                    tabela.adicionarToken(new TokenOperadoresAtribuicao("/=", linha, startCol));
                    advance();
                } else {
                    tabela.adicionarToken(new TokenOperadoresAritmeticos("/", linha, startCol));
                }
                return true;

            case '%':
                advance();
                if (currentChar == '=') {
                    tabela.adicionarToken(new TokenOperadoresAtribuicao("%=", linha, startCol));
                    advance();
                } else {
                    tabela.adicionarToken(new TokenOperadoresAritmeticos("%", linha, startCol));
                }
                return true;

            // Operadores relacionais e lógicos
            case '=':
                advance();
                if (currentChar == '=') {
                    tabela.adicionarToken(new TokenOperadoresRelacionais("==", linha, startCol));
                    advance();
                } else {
                    tabela.adicionarToken(new TokenOperadoresAtribuicao("=", linha, startCol));
                }
                return true;

            case '!':
                advance();
                if (currentChar == '=') {
                    tabela.adicionarToken(new TokenOperadoresRelacionais("!=", linha, startCol));
                    advance();
                } else {
                    tabela.adicionarToken(new TokenOperadoresRelacionais("!", linha, startCol));
                }
                return true;

            case '<':
                advance();
                if (currentChar == '<') {
                    advance();
                    if (currentChar == '=') {
                        tabela.adicionarToken(new TokenOperadoresAtribuicao("<<=", linha, startCol));
                        advance();
                    } else {
                        tabela.adicionarToken(new TokenBitwise("<<", linha, startCol));
                    }
                } else if (currentChar == '=') {
                    tabela.adicionarToken(new TokenOperadoresRelacionais("<=", linha, startCol));
                    advance();
                } else {
                    tabela.adicionarToken(new TokenOperadoresRelacionais("<", linha, startCol));
                }
                return true;

            case '>':
                advance();
                if (currentChar == '>') {
                    advance();
                    if (currentChar == '=') {
                        tabela.adicionarToken(new TokenOperadoresAtribuicao(">>=", linha, startCol));
                        advance();
                    } else {
                        tabela.adicionarToken(new TokenBitwise(">>", linha, startCol));
                    }
                } else if (currentChar == '=') {
                    tabela.adicionarToken(new TokenOperadoresRelacionais(">=", linha, startCol));
                    advance();
                } else {
                    tabela.adicionarToken(new TokenOperadoresRelacionais(">", linha, startCol));
                }
                return true;

            // Operadores bitwise
            case '&':
                advance();
                if (currentChar == '&') {
                    tabela.adicionarToken(new TokenOperadoresRelacionais("&&", linha, startCol));
                    advance();
                } else if (currentChar == '=') {
                    tabela.adicionarToken(new TokenOperadoresAtribuicao("&=", linha, startCol));
                    advance();
                } else {
                    tabela.adicionarToken(new TokenBitwise("&", linha, startCol));
                }
                return true;

            case '|':
                advance();
                if (currentChar == '|') {
                    tabela.adicionarToken(new TokenOperadoresRelacionais("||", linha, startCol));
                    advance();
                } else if (currentChar == '=') {
                    tabela.adicionarToken(new TokenOperadoresAtribuicao("|=", linha, startCol));
                    advance();
                } else {
                    tabela.adicionarToken(new TokenBitwise("|", linha, startCol));
                }
                return true;

            case '^':
                advance();
                if (currentChar == '=') {
                    tabela.adicionarToken(new TokenOperadoresAtribuicao("^=", linha, startCol));
                    advance();
                } else {
                    tabela.adicionarToken(new TokenBitwise("^", linha, startCol));
                }
                return true;

            case '~':
                tabela.adicionarToken(new TokenBitwise("~", linha, startCol));
                advance();
                return true;

            // Símbolos diversos
            case ';':
                tabela.adicionarToken(new TokenSimbolos(";", linha, startCol));
                advance();
                return true;

            case ',':
                tabela.adicionarToken(new TokenSimbolos(",", linha, startCol));
                advance();
                return true;

            case '(':
                tabela.adicionarToken(new TokenSimbolos("(", linha, startCol));
                advance();
                return true;

            case ')':
                tabela.adicionarToken(new TokenSimbolos(")", linha, startCol));
                advance();
                return true;

            case '{':
                tabela.adicionarToken(new TokenSimbolos("{", linha, startCol));
                advance();
                return true;

            case '}':
                tabela.adicionarToken(new TokenSimbolos("}", linha, startCol));
                advance();
                return true;

            case '[':
                tabela.adicionarToken(new TokenSimbolos("[", linha, startCol));
                advance();
                return true;

            case ']':
                tabela.adicionarToken(new TokenSimbolos("]", linha, startCol));
                advance();
                return true;

            case '.':
                tabela.adicionarToken(new TokenSimbolos(".", linha, startCol));
                advance();
                return true;

            case '?':
                tabela.adicionarToken(new TokenSimbolos("?", linha, startCol));
                advance();
                return true;

            case ':':
                tabela.adicionarToken(new TokenSimbolos(":", linha, startCol));
                advance();
                return true;

            default:
                return false;
        }
    }

    @Override
    protected void processarString() throws IOException {
        StringBuilder lex = new StringBuilder();
        int startCol = coluna;

        lex.append((char) currentChar); // Adiciona a '"' inicial
        advance();

        while (currentChar != -1 && currentChar != '"' && currentChar != '\n') {
            lex.append((char) currentChar);
            advance();
        }

        if (currentChar == '"') {
            lex.append((char) currentChar);
            advance();
            tabela.adicionarToken(new Token("STRING", lex.toString(), linha, startCol));
        } else {
            System.err.printf("Erro: String não fechada (Linha %d)\n", linha);
        }
    }

    @Override
    protected void processarCaractere() throws IOException {
        StringBuilder lex = new StringBuilder();
        int startCol = coluna;

        lex.append((char) currentChar); // Adiciona a ''' inicial
        advance();

        if (currentChar != -1 && currentChar != '\'') {
            lex.append((char) currentChar);
            advance();
        }

        if (currentChar == '\'') {
            lex.append((char) currentChar);
            advance();
            tabela.adicionarToken(new Token("CHAR", lex.toString(), linha, startCol));
        } else {
            System.err.printf("Erro: Caractere não fechado (Linha %d)\n", linha);
        }
    }

}
