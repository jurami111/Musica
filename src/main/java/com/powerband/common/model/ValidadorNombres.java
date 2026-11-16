package com.powerband.common.model;

import com.powerband.common.constants.BinaryConstants;

public final class ValidadorNombres {
    private ValidadorNombres() {
    }

    public static boolean esValido(String nombre) {
        return nombre != null && !nombre.isBlank()
                && nombre.length() <= BinaryConstants.MAXIMO_UNIDADES_NOMBRE
                && nombre.indexOf('\0') < 0;
    }

    public static void validar(String nombre) {
        if (!esValido(nombre)) {
            throw new IllegalArgumentException("El nombre debe tener entre 1 y "
                    + BinaryConstants.MAXIMO_UNIDADES_NOMBRE + " unidades UTF-16, sin nulos");
        }
    }
}
