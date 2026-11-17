package com.powerband.network;

import com.powerband.bridge.abstraction.Instrument;
import com.powerband.common.constants.ConstantesInstrumentos;
import java.io.IOException;
import java.util.function.LongSupplier;

final class GestorGestos {
    private final Instrument instrumento;
    private final LongSupplier reloj;
    private int nota = ConstantesInstrumentos.NOTA_MOVIL_INICIAL;
    private boolean giroArmado = true;
    private boolean movimientoArmado = true;
    private boolean notaTocada;
    private long ultimaNota;

    GestorGestos(Instrument instrumento, LongSupplier reloj) {
        this.instrumento = instrumento;
        this.reloj = reloj;
    }

    void procesar(MuestraSensores muestra) throws IOException {
        double giro = muestra.gz();
        if (Math.abs(giro) < ConstantesInstrumentos.UMBRAL_GIRO_REARME) {
            giroArmado = true;
        } else if (giroArmado && giro >= ConstantesInstrumentos.UMBRAL_GIRO) {
            nota = Math.min(ConstantesInstrumentos.NOTA_MOVIL_MAXIMA, nota + 1);
            giroArmado = false;
        } else if (giroArmado && giro <= -ConstantesInstrumentos.UMBRAL_GIRO) {
            nota = Math.max(ConstantesInstrumentos.NOTA_MOVIL_MINIMA, nota - 1);
            giroArmado = false;
        }

        double magnitud = Math.hypot(Math.hypot(muestra.ax(), muestra.ay()), muestra.az());
        double movimiento = Math.abs(magnitud - ConstantesInstrumentos.GRAVEDAD);
        if (movimiento < ConstantesInstrumentos.UMBRAL_MOVIMIENTO_REARME) {
            movimientoArmado = true;
        } else if (movimiento >= ConstantesInstrumentos.UMBRAL_MOVIMIENTO && movimientoArmado) {
            long ahora = reloj.getAsLong();
            if (!notaTocada || ahora - ultimaNota >= ConstantesInstrumentos.INTERVALO_NOTAS_MOVIL_NS) {
                instrumento.iniciarNota(nota, ConstantesInstrumentos.DURACION_NOTA_MS);
                ultimaNota = ahora;
                notaTocada = true;
                movimientoArmado = false;
            }
        }
    }
}
