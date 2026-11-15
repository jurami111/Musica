package com.powerband.bridge.implementor;

import com.powerband.common.constants.SynthConstants;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class PadSynthCommand implements SynthImplementor {
    private final EmisorOsc emisor;
    private final Map<Integer, Integer> notasActivas = new HashMap<>();

    public PadSynthCommand(EmisorOsc emisor) {
        this.emisor = Objects.requireNonNull(emisor, "emisor");
    }

    @Override
    public synchronized void iniciarNota(int nota, int duracionMs) throws IOException {
        ValidadorNotas.validarNota(nota);
        ValidadorNotas.validarDuracion(duracionMs);
        terminarNota(nota);
        float frecuencia = (float) (440.0 * Math.pow(2.0, (nota - 69) / 12.0));
        int nodo = emisor.crearSintetizador(SynthConstants.NOMBRE_ORGANO,
                "freq", frecuencia, "r1", 1f, "r2", 2f, "r3", 3f,
                "a1", 1f, "a2", 0.5f, "a3", 0.25f);
        notasActivas.put(nota, nodo);
    }

    @Override
    public synchronized void terminarNota(int nota) throws IOException {
        ValidadorNotas.validarNota(nota);
        Integer nodo = notasActivas.get(nota);
        if (nodo != null) {
            emisor.cambiarParametros(nodo, "gate", 0);
            notasActivas.remove(nota);
        }
    }
}
