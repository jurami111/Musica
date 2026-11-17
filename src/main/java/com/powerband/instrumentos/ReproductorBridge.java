package com.powerband.instrumentos;

import com.powerband.bridge.implementor.LeadSynthCommand;
import com.powerband.bridge.implementor.BassSynthCommand;
import com.powerband.bridge.implementor.PadSynthCommand;
import com.powerband.common.constants.BinaryConstants;
import com.powerband.common.constants.SynthConstants;
import com.powerband.common.interfaces.AudioEnginePlayer;
import com.powerband.common.model.SynthCommand;
import java.io.IOException;
import java.util.Objects;

public final class ReproductorBridge implements AudioEnginePlayer, AutoCloseable {
    private final LeadSynthCommand piano;
    private final BassSynthCommand bajo;
    private final PadSynthCommand organo;

    public ReproductorBridge(LeadSynthCommand piano, BassSynthCommand bajo, PadSynthCommand organo) {
        this.piano = Objects.requireNonNull(piano, "piano");
        this.bajo = Objects.requireNonNull(bajo, "bajo");
        this.organo = Objects.requireNonNull(organo, "organo");
    }

    @Override
    public void reproducirComando(SynthCommand comando) throws IOException {
        double[] parametros = comando.obtenerParametros();
        int nota = (int) parametros[0];
        if (comando.obtenerSintetizador() == SynthConstants.IDENTIFICADOR_PIANO) {
            piano.iniciarNota(nota, (int) parametros[1], (int) parametros[2]);
        } else if (comando.obtenerSintetizador() == SynthConstants.IDENTIFICADOR_BAJO) {
            bajo.iniciarNota(nota, (int) parametros[1]);
        } else if (comando.obtenerAccion() == BinaryConstants.ACCION_TERMINAR_NOTA) {
            organo.terminarNota(nota);
        } else {
            organo.iniciarNota(nota, parametros[1], parametros[2], parametros[3],
                    parametros[4], parametros[5], parametros[6]);
        }
    }

    @Override
    public void finalizarReproduccion() throws IOException {
        organo.terminarTodas();
    }

    @Override
    public void close() throws IOException {
        finalizarReproduccion();
    }
}
