/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Main.java to edit this template
 */
package main;

import analyzer.Parser;
import analyzer.Analex;
import Tokens.TabelaDeTokens;
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
        String caminhoFicheiro = "codigo.c";
        String ficheiroSaida = "codigo.exe"; 

        try {
            long inicio = System.nanoTime();

            Analex analex = new Analex(caminhoFicheiro);
            TabelaDeTokens tabela = analex.analisarCodigo();

            Parser parser = new Parser(tabela);
            parser.parse();

            long fim = System.nanoTime();
            double tempoCompilacao = (fim - inicio) / 1_000_000_000.0;

            if (parser.getCountErros() == 0) {
                System.out.printf(
                        """
                Compilation results...
                --------
                - Errors: %d
                - Warnings: 0
                - Output Filename: %s
                - Output Size: %.2f KiB
                - Compilation Time: %.2fs
                """,
                        parser.getCountErros(),
                        ficheiroSaida,
                        calcularTamanhoFicheiroKB(ficheiroSaida),
                        tempoCompilacao
                );
            } else {
                System.out.println("Compilação falhou. Total de erros: " + parser.getCountErros());
            }

        } catch (Exception e) {
            System.err.println("Erro fatal: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static double calcularTamanhoFicheiroKB(String caminho) {
        java.io.File file = new java.io.File(caminho);
        if (file.exists()) {
            return file.length() / 1024.0;
        }
        return 0.0;
    }

}
