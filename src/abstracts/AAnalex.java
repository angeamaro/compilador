/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package abstracts;

import java.io.IOException;
import Tokens.TabelaDeTokens;

/**
 *
 * @author itsupport
 */
public abstract class AAnalex {
    
    public abstract TabelaDeTokens analisarCodigo() throws IOException;

    // Métodos auxiliares que podem ser úteis para extensão
    protected abstract void advance() throws IOException;
    protected abstract void processarDiretiva() throws IOException;
    protected abstract void processarComentario() throws IOException;
    protected abstract void processarIdentificador() throws IOException;
    protected abstract void processarNumero() throws IOException;
    protected abstract boolean processarOperadores() throws IOException;
    protected abstract void processarString() throws IOException;
    protected abstract void processarCaractere() throws IOException;
    
}
