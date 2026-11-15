package com.powerband.bridge.implementor;

import com.powerband.common.constants.SynthConstants;
import java.io.IOException;
import java.util.Objects;

public class LeadSynthCommand implements SynthImplementor {
    private final EmisorOsc emisor;

    public LeadSynthCommand(EmisorOsc emisor) {
        this.emisor = Objects.requireNonNull(emisor, "emisor");
    }

    @Override
    public void iniciarNota(int nota, int duracionMs) throws IOException {
        ValidadorNotas.validarNota(nota);
        ValidadorNotas.validarDuracion(duracionMs);
        emisor.crearSintetizador(SynthConstants.NOMBRE_PIANO,
                "note", nota, "durMs", duracionMs, "third", 4);
    }

    @Override
    public void terminarNota(int nota) {
        ValidadorNotas.validarNota(nota);
    }
}
