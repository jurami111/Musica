package com.powerband.common.model;

import com.powerband.common.constants.BinaryConstants;
import com.powerband.common.constants.SynthConstants;

public final class SynthCommand {
    private final long tiempo;
    private final int sintetizador;
    private final int accion;
    private final double[] parametros;

    public SynthCommand(long tiempo, int sintetizador, int accion, double... parametros) {
        if (tiempo < 0 || parametros == null
                || parametros.length == 0 || parametros.length > BinaryConstants.MAXIMO_PARAMETROS) {
            throw new IllegalArgumentException("Tiempo o cantidad de parametros invalidos");
        }
        double[] copia = parametros.clone();
        for (double parametro : copia) {
            if (!Double.isFinite(parametro)) {
                throw new IllegalArgumentException("Los parametros deben ser numeros finitos");
            }
        }
        int cantidad;
        if (sintetizador == SynthConstants.IDENTIFICADOR_PIANO
                && accion == BinaryConstants.ACCION_TOCAR) {
            cantidad = SynthConstants.PARAMETROS_PIANO;
        } else if (sintetizador == SynthConstants.IDENTIFICADOR_BAJO
                && accion == BinaryConstants.ACCION_TOCAR) {
            cantidad = SynthConstants.PARAMETROS_BAJO;
        } else if (sintetizador == SynthConstants.IDENTIFICADOR_ORGANO
                && accion == BinaryConstants.ACCION_INICIAR_NOTA) {
            cantidad = SynthConstants.PARAMETROS_INICIO_ORGANO;
        } else if (sintetizador == SynthConstants.IDENTIFICADOR_ORGANO
                && accion == BinaryConstants.ACCION_TERMINAR_NOTA) {
            cantidad = SynthConstants.PARAMETROS_FIN_ORGANO;
        } else {
            throw new IllegalArgumentException("Sintetizador y accion incompatibles o desconocidos");
        }
        if (copia.length != cantidad) {
            throw new IllegalArgumentException("Cantidad incorrecta de parametros para el comando");
        }
        if (copia[0] != Math.rint(copia[0])
                || copia[0] < SynthConstants.NOTA_MIDI_MINIMA
                || copia[0] > SynthConstants.NOTA_MIDI_MAXIMA) {
            throw new IllegalArgumentException("La nota MIDI debe ser un entero entre 0 y 127");
        }
        if (accion == BinaryConstants.ACCION_TOCAR) {
            if (copia[1] != Math.rint(copia[1]) || copia[1] <= 0 || copia[1] > Integer.MAX_VALUE) {
                throw new IllegalArgumentException("La duracion debe ser un entero positivo representable como int");
            }
            if (sintetizador == SynthConstants.IDENTIFICADOR_PIANO
                    && copia[2] != SynthConstants.TERCERA_MENOR
                    && copia[2] != SynthConstants.TERCERA_MAYOR) {
                throw new IllegalArgumentException("La tercera del piano debe ser 3 o 4");
            }
        }
        this.tiempo = tiempo;
        this.sintetizador = sintetizador;
        this.accion = accion;
        this.parametros = copia;
    }

    public long obtenerTiempo() {
        return tiempo;
    }

    public int obtenerSintetizador() {
        return sintetizador;
    }

    public int obtenerAccion() {
        return accion;
    }

    public double[] obtenerParametros() {
        return parametros.clone();
    }
}