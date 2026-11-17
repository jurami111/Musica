package com.powerband.storage.io;

import com.powerband.common.constants.BinaryConstants;
import com.powerband.common.exceptions.ArchivoCorruptoException;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

public final class NombreBinario {
    private NombreBinario() {
    }

    public static void escribir(DataOutput salida, String nombre) throws IOException {
        for (int i = 0; i < BinaryConstants.MAXIMO_UNIDADES_NOMBRE; i++) {
            salida.writeChar(i < nombre.length() ? nombre.charAt(i) : BinaryConstants.VALOR_RELLENO);
        }
    }

    public static String leer(DataInput entrada) throws IOException, ArchivoCorruptoException {
        StringBuilder nombre = new StringBuilder();
        boolean relleno = false;
        for (int i = 0; i < BinaryConstants.MAXIMO_UNIDADES_NOMBRE; i++) {
            char caracter = entrada.readChar();
            if (caracter == BinaryConstants.VALOR_RELLENO) {
                relleno = true;
            } else if (relleno) {
                throw new ArchivoCorruptoException("El relleno del nombre contiene caracteres");
            } else {
                nombre.append(caracter);
            }
        }
        return nombre.toString();
    }
}
