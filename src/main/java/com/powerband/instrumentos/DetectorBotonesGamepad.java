package com.powerband.instrumentos;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class DetectorBotonesGamepad {
    private static final int[] NOTAS = {36, 38, 40, 43};
    private final boolean[] anteriores = new boolean[NOTAS.length];

    public List<Integer> actualizar(boolean[] pulsados) {
        Objects.requireNonNull(pulsados, "pulsados");
        if (pulsados.length != NOTAS.length) {
            throw new IllegalArgumentException("Se requieren cuatro botones del gamepad");
        }
        List<Integer> notas = new ArrayList<>();
        for (int i = 0; i < NOTAS.length; i++) {
            if (pulsados[i] && !anteriores[i]) {
                notas.add(NOTAS[i]);
            }
            anteriores[i] = pulsados[i];
        }
        return notas;
    }
}
