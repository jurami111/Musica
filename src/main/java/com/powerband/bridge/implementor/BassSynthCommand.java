package com.powerband.bridge.implementor;

import com.powerband.common.constants.SynthConstants;
import java.io.IOException;
import java.util.Objects;

public class BassSynthCommand implements SynthImplementor {
    private final EmisorOsc emisor;

    public BassSynthCommand(EmisorOsc emisor) {
        this.emisor = Objects.requireNonNull(emisor, "emisor");
    }

    @Override
    public void iniciarNota(int nota, int duracionMs) throws IOException {
        ValidadorNotas.validarNota(nota);
        ValidadorNotas.validarDuracion(duracionMs);
        emisor.crearSintetizador(SynthConstants.NOMBRE_BAJO, "note", nota, "durMs", duracionMs);
    }

    @Override
    public void terminarNota(int nota) {
        ValidadorNotas.validarNota(nota);
    }
}
