package com.powerband.bridge.abstraction;

import com.powerband.bridge.implementor.SynthImplementor;
import java.io.IOException;
import java.util.Objects;

public abstract class Instrument {
    private final SynthImplementor sintetizador;

    protected Instrument(SynthImplementor sintetizador) {
        this.sintetizador = Objects.requireNonNull(sintetizador, "sintetizador");
    }

    public void iniciarNota(int nota, int duracionMs) throws IOException {
        sintetizador.iniciarNota(nota, duracionMs);
    }

    public void terminarNota(int nota) throws IOException {
        sintetizador.terminarNota(nota);
    }
}
