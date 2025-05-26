/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package utils;


import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 *
 * @author itsupport
 */
public class Ficheiro {

    public static String ler(String caminho) throws IOException {
        return new String(Files.readAllBytes(Paths.get(caminho)));
    }

}
