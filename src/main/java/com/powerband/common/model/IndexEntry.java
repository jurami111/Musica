package com.powerband.common.model;

import com.powerband.common.constants.BinaryConstants;

public final class IndexEntry {
    private final String nombre;
    private final long posicionEncabezado;

    public IndexEntry(String nombre, long posicionEncabezado) {
        ValidadorNombres.validar(nombre);
        if (posicionEncabezado < 0
                || posicionEncabezado % BinaryConstants.BYTES_REGISTRO != 0) {
            throw new IllegalArgumentException("La posicion debe ser no negativa y estar alineada");
        }
        this.nombre = nombre;
        this.posicionEncabezado = posicionEncabezado;
    }

    public String obtenerNombre() {
        return nombre;
    }

    public long obtenerPosicionEncabezado() {
        return posicionEncabezado;
    }
}