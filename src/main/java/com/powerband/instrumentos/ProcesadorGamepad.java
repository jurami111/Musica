package com.powerband.instrumentos;

import com.powerband.bridge.abstraction.Instrument;
import com.powerband.common.constants.ConstantesInstrumentos;
import java.io.IOException;
import java.util.Objects;

public final class ProcesadorGamepad {
    private final Instrument instrumento;
    private final DetectorBotonesGamepad detector = new DetectorBotonesGamepad();

    public ProcesadorGamepad(Instrument instrumento) {
        this.instrumento = Objects.requireNonNull(instrumento, "instrumento");
    }

    public void procesar(boolean[] pulsados) throws IOException {
        for (int nota : detector.actualizar(pulsados)) {
            instrumento.iniciarNota(nota, ConstantesInstrumentos.DURACION_NOTA_MS);
        }
    }
}
