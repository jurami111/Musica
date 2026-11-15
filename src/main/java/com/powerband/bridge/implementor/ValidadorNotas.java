package com.powerband.bridge.implementor;

import com.powerband.common.constants.SynthConstants;

final class ValidadorNotas {
    private ValidadorNotas() {
    }

    static void validarNota(int nota) {
        if (nota < SynthConstants.NOTA_MIDI_MINIMA || nota > SynthConstants.NOTA_MIDI_MAXIMA) {
            throw new IllegalArgumentException("La nota MIDI debe estar entre 0 y 127");
        }
    }

    static void validarDuracion(int duracionMs) {
        if (duracionMs <= 0) {
            throw new IllegalArgumentException("La duración debe ser positiva");
        }
    }
}
