package com.powerband.instrumentos;

import java.awt.image.BufferedImage;
import java.util.Objects;
import java.util.OptionalInt;

public final class DetectorObjetoRojo {
    private static final int ANCHO = 80;
    private static final int ALTO = 60;
    private static final int[] NOTAS = {60, 62, 64, 65, 67, 69, 71, 72};
    private int candidato = -1;
    private int repeticiones;
    private int notaActiva = -1;

    public OptionalInt actualizar(BufferedImage imagen) {
        Objects.requireNonNull(imagen, "imagen");
        long sumaX = 0;
        int cantidad = 0;
        for (int y = 0; y < ALTO; y++) {
            int origenY = (int) ((long) y * imagen.getHeight() / ALTO);
            for (int x = 0; x < ANCHO; x++) {
                int origenX = (int) ((long) x * imagen.getWidth() / ANCHO);
                int color = imagen.getRGB(origenX, origenY);
                int rojo = (color >>> 16) & 255;
                int verde = (color >>> 8) & 255;
                int azul = color & 255;
                if (rojo >= 100 && 2 * rojo >= 3 * verde && 2 * rojo >= 3 * azul) {
                    cantidad++;
                    sumaX += x;
                }
            }
        }
        int nota = cantidad < 24 ? -1 : NOTAS[(int) (sumaX * NOTAS.length / (cantidad * ANCHO))];
        if (nota == candidato) {
            repeticiones = Math.min(2, repeticiones + 1);
        } else {
            candidato = nota;
            repeticiones = 1;
        }
        if (repeticiones >= 2) {
            notaActiva = candidato;
        }
        return notaActiva < 0 ? OptionalInt.empty() : OptionalInt.of(notaActiva);
    }
}
