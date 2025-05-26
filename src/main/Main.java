/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Main.java to edit this template
 */
package main;

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
        /*Analisador parser = new Analisador(analex);
        parser.parseProgram();*/

        System.out.println("\n\n\n");

        try {
            Analex analex = new Analex(caminhoFicheiro);
            //analex.analisarCodigo().imprimirTokens();
            Parser parser = new Parser(analex.analisarCodigo());
            parser.parse();
        } catch (IOException e) {
            System.out.println("Erro ao ler o ficheiro: " + e.getMessage());
        }
    }

}
