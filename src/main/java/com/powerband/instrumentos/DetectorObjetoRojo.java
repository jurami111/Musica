package com.powerband.instrumentos;

import com.powerband.common.constants.ConstantesInstrumentos;
import java.awt.image.BufferedImage;
import java.util.Objects;
import java.util.OptionalInt;

public final class DetectorObjetoRojo {
    private int candidato = ConstantesInstrumentos.NOTA_INACTIVA;
    private int repeticiones;
    private int notaActiva = ConstantesInstrumentos.NOTA_INACTIVA;

    public OptionalInt actualizar(BufferedImage imagen) {
        Objects.requireNonNull(imagen, "imagen");
        long sumaX = 0;
        int cantidad = 0;
        for (int y = 0; y < ConstantesInstrumentos.ALTO_CAMARA; y++) {
            int origenY = (int) ((long) y * imagen.getHeight() / ConstantesInstrumentos.ALTO_CAMARA);
            for (int x = 0; x < ConstantesInstrumentos.ANCHO_CAMARA; x++) {
                int origenX = (int) ((long) x * imagen.getWidth() / ConstantesInstrumentos.ANCHO_CAMARA);
                int color = imagen.getRGB(origenX, origenY);
                int rojo = (color >>> 16) & 255;
                int verde = (color >>> 8) & 255;
                int azul = color & 255;
                if (rojo >= ConstantesInstrumentos.ROJO_MINIMO
                        && ConstantesInstrumentos.PESO_ROJO * rojo >= ConstantesInstrumentos.PESO_OTRO_COLOR * verde
                        && ConstantesInstrumentos.PESO_ROJO * rojo >= ConstantesInstrumentos.PESO_OTRO_COLOR * azul) {
                    cantidad++;
                    sumaX += x;
                }
            }
        }
        int nota = cantidad < ConstantesInstrumentos.PIXELES_ROJOS_MINIMOS
                ? ConstantesInstrumentos.NOTA_INACTIVA
                : ConstantesInstrumentos.notaCamara((int) (sumaX * ConstantesInstrumentos.cantidadNotasCamara()
                        / (cantidad * ConstantesInstrumentos.ANCHO_CAMARA)));
        if (nota == candidato) {
            repeticiones = Math.min(ConstantesInstrumentos.CUADROS_ESTABLES, repeticiones + 1);
        } else {
            candidato = nota;
            repeticiones = 1;
        }
        if (repeticiones >= ConstantesInstrumentos.CUADROS_ESTABLES) {
            notaActiva = candidato;
        }
        return notaActiva < 0 ? OptionalInt.empty() : OptionalInt.of(notaActiva);
    }
}
