package com.powerband.common.interfaces;

import com.powerband.common.model.SynthCommand;
import java.io.IOException;

public interface AudioEnginePlayer {
    void reproducirComando(SynthCommand comando) throws IOException;

    default void playCommand(SynthCommand comando) throws IOException {
        reproducirComando(comando);
    }

    default void finalizarReproduccion() throws IOException {
    }
}