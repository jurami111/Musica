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
        ValidadorNotas.validarDuracion(duracionMs);
        iniciarNota(nota, SynthConstants.R1_ORGANO, SynthConstants.R2_ORGANO, SynthConstants.R3_ORGANO,
                SynthConstants.A1_ORGANO, SynthConstants.A2_ORGANO, SynthConstants.A3_ORGANO);
    }

    public synchronized void iniciarNota(int nota, double r1, double r2, double r3,
            double a1, double a2, double a3) throws IOException {
        ValidadorNotas.validarNota(nota);
        float[] controles = {convertirParametro(r1), convertirParametro(r2), convertirParametro(r3),
                convertirParametro(a1), convertirParametro(a2), convertirParametro(a3)};
        terminarNota(nota);
        float frecuencia = (float) (SynthConstants.FRECUENCIA_REFERENCIA * Math.pow(
                SynthConstants.RAZON_OCTAVA,
                (nota - SynthConstants.NOTA_REFERENCIA) / SynthConstants.SEMITONOS_OCTAVA));
        int nodo = emisor.crearSintetizador(SynthConstants.NOMBRE_ORGANO,
                "freq", frecuencia, "r1", controles[0], "r2", controles[1], "r3", controles[2],
                "a1", controles[3], "a2", controles[4], "a3", controles[5]);
        notasActivas.put(nota, nodo);
    }

    private static float convertirParametro(double parametro) throws IOException {
        float convertido = (float) parametro;
        if (!Double.isFinite(parametro) || !Float.isFinite(convertido)
                || (parametro != 0 && convertido == 0)) {
            throw new IOException("El parámetro del órgano no es representable como float OSC: " + parametro);
        }
        return convertido;
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

    public synchronized void terminarTodas() throws IOException {
        IOException fallo = null;
        var pendientes = notasActivas.entrySet().iterator();
        while (pendientes.hasNext()) {
            var entrada = pendientes.next();
            try {
                emisor.cambiarParametros(entrada.getValue(), "gate", 0);
                pendientes.remove();
            } catch (IOException error) {
                if (fallo == null) {
                    fallo = error;
                } else if (fallo != error) {
                    fallo.addSuppressed(error);
                }
            }
        }
        if (fallo != null) {
            throw fallo;
        }
    }
}
