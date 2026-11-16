package com.powerband.common.model;

import java.time.Instant;
import java.util.Objects;

public final class RecordingMetadata {
    private final String nombre;
    private final Instant fechaHora;

    public RecordingMetadata(String nombre, Instant fechaHora) {
        ValidadorNombres.validar(nombre);
        Objects.requireNonNull(fechaHora, "fechaHora");
        try {
            this.fechaHora = Instant.ofEpochMilli(fechaHora.toEpochMilli());
        } catch (ArithmeticException error) {
            throw new IllegalArgumentException("La fecha no se puede representar en milisegundos como long", error);
        }
        this.nombre = nombre;
    }

    public String obtenerNombre() {
        return nombre;
    }

    public Instant obtenerFechaHora() {
        return fechaHora;
    }
}