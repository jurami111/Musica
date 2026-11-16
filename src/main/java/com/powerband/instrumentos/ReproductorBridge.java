package com.powerband.instrumentos;

import com.powerband.bridge.implementor.SynthImplementor;
import com.powerband.common.constants.BinaryConstants;
import com.powerband.common.constants.SynthConstants;
import com.powerband.common.interfaces.AudioEnginePlayer;
import com.powerband.common.model.SynthCommand;
import java.io.IOException;
import java.util.Objects;

public final class ReproductorBridge implements AudioEnginePlayer {
    private final SynthImplementor piano;
    private final SynthImplementor bajo;
    private final SynthImplementor organo;

    public ReproductorBridge(SynthImplementor piano, SynthImplementor bajo, SynthImplementor organo) {
        this.piano = Objects.requireNonNull(piano, "piano");
        this.bajo = Objects.requireNonNull(bajo, "bajo");
        this.organo = Objects.requireNonNull(organo, "organo");
    }

    @Override
    public void reproducirComando(SynthCommand comando) throws IOException {
        double[] parametros = comando.obtenerParametros();
        int nota = (int) parametros[0];
        if (comando.obtenerSintetizador() == SynthConstants.IDENTIFICADOR_PIANO) {
            if (parametros[2] != SynthConstants.TERCERA_MAYOR) {
                throw new IOException("El Bridge no admite tercera menor");
            }
            piano.iniciarNota(nota, (int) parametros[1]);
        } else if (comando.obtenerSintetizador() == SynthConstants.IDENTIFICADOR_BAJO) {
            bajo.iniciarNota(nota, (int) parametros[1]);
        } else if (comando.obtenerAccion() == BinaryConstants.ACCION_TERMINAR_NOTA) {
            organo.terminarNota(nota);
        } else {
            double[] valores = {1, 2, 3, 1, 0.5, 0.25};
            for (int i = 0; i < valores.length; i++) {
                if (parametros[i + 1] != valores[i]) {
                    throw new IOException("El Bridge no admite parciales personalizados");
                }
            }
            organo.iniciarNota(nota, 1);
        }
    }
}
