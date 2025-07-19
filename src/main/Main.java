/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Main.java to edit this template
 */
package main;

import Tokens.TabelaDeTokens;
import analisador.*;
import Tokens.Token;
import java.io.IOException;

/**
 *
 * @author itsupport
 */
public class Main {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws Exception {
        // TODO code application logic here

        String caminhoFicheiro = "codigo.c";
        try {
            Analex analex = new Analex(caminhoFicheiro);
            TabelaDeTokens tabela = analex.analisarCodigo();

            /* Verifique se os tokens foram gerados
            System.out.println("Total de tokens: " + tabela.getTokens().size());
            tabela.imprimirTokens();   Se você tiver esse método */

            Parser parser = new Parser(tabela);
            parser.parse();
            System.out.println("Análise concluída. Total de erros: " + parser.getCountErros());
        } catch (Exception e) {
            System.err.println("Erro fatal: " + e.getMessage());
            e.printStackTrace();
        }

    }

}
