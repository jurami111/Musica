package com.powerband.instrumentos;

import com.powerband.common.constants.ConstantesInstrumentos;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class DetectorBotonesGamepad {
    private final boolean[] anteriores = new boolean[ConstantesInstrumentos.cantidadBotonesGamepad()];

    public List<Integer> actualizar(boolean[] pulsados) {
        Objects.requireNonNull(pulsados, "pulsados");
        if (pulsados.length != anteriores.length) {
            throw new IllegalArgumentException("Se requieren cuatro botones del gamepad");
        }
        List<Integer> notas = new ArrayList<>();
        for (int i = 0; i < anteriores.length; i++) {
            if (pulsados[i] && !anteriores[i]) {
                notas.add(ConstantesInstrumentos.notaGamepad(i));
            }
            anteriores[i] = pulsados[i];
        }
        return notas;
    }
}
