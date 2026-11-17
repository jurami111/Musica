package com.powerband.instrumentos;

import com.powerband.bridge.abstraction.Instrument;
import com.powerband.common.constants.ConstantesInstrumentos;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Objects;
import java.util.OptionalInt;

public final class ProcesadorCamara {
    private final Instrument instrumento;
    private final DetectorObjetoRojo detector = new DetectorObjetoRojo();
    private Integer notaActiva;

    public ProcesadorCamara(Instrument instrumento) {
        this.instrumento = Objects.requireNonNull(instrumento, "instrumento");
    }

    public void procesar(BufferedImage imagen) throws IOException {
        OptionalInt detectada = detector.actualizar(imagen);
        Integer nota = detectada.isPresent() ? detectada.getAsInt() : null;
        if (Objects.equals(notaActiva, nota)) {
            return;
        }
        detenerNota();
        if (nota != null) {
            notaActiva = nota;
            instrumento.iniciarNota(nota, ConstantesInstrumentos.DURACION_NOTA_MS);
        }
    }

    public void detenerNota() throws IOException {
        if (notaActiva != null) {
            instrumento.terminarNota(notaActiva);
            notaActiva = null;
        }
    }
}
