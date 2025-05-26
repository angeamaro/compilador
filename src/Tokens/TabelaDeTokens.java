/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package Tokens;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 *
 * @author itsupport
 */
public class TabelaDeTokens {

    private List<Token> tokensEncontrados;
    private int indiceAtual;

    public TabelaDeTokens() {
        this.tokensEncontrados = new ArrayList<>();
        this.indiceAtual = 0;
    }

    public void adicionarToken(Token token) {
        tokensEncontrados.add(token);
    }

    public Token proximoToken() {
        if (indiceAtual < tokensEncontrados.size()) {
            return tokensEncontrados.get(indiceAtual++);
        }
        return new Token("EOF", "FIM_DO_ARQUIVO", -1, -1); // Melhor que null
    }

    public void imprimirTokens() {
        System.out.println("Tokens encontrados:");
        tokensEncontrados.forEach(System.out::println);
    }

    public List<Token> getTokens() {
        return Collections.unmodifiableList(tokensEncontrados);
    }

    public int getPosicao() {
        return indiceAtual;
    }

    public void setPosicao(int posicao) {
        if (posicao < 0 || posicao > tokensEncontrados.size()) {
            throw new IndexOutOfBoundsException("Posição inválida: " + posicao);
        }
        indiceAtual = posicao;
    }

    // Novo método útil para look ahead
    public Token verProximoToken() {
        if (indiceAtual < tokensEncontrados.size()) {
            return tokensEncontrados.get(indiceAtual);
        }
        return new Token("EOF", "FIM_DO_ARQUIVO", -1, -1);
    }

    public Token lookAhead(int k) {
        int pos = indiceAtual + k - 1;
        if (pos < tokensEncontrados.size()) {
            return tokensEncontrados.get(pos);
        }
        return null;
    }

}
