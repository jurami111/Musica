package com.powerband.common.model;

import com.powerband.common.constants.BinaryConstants;

public final class RecordingHeader {
    private final String nombre;
    private final long inicio;
    private final long fin;

    public RecordingHeader(String nombre, long inicio, long fin) {
        ValidadorNombres.validar(nombre);
        if (inicio < BinaryConstants.BYTES_REGISTRO || fin < inicio
                || inicio % BinaryConstants.BYTES_REGISTRO != 0
                || fin % BinaryConstants.BYTES_REGISTRO != 0) {
            throw new IllegalArgumentException("Los limites deben estar alineados y el final no puede preceder al inicio");
        }
        this.nombre = nombre;
        this.inicio = inicio;
        this.fin = fin;
    }

    public String obtenerNombre() {
        return nombre;
    }

    public long obtenerInicio() {
        return inicio;
    }

    public long obtenerFin() {
        return fin;
    }
}