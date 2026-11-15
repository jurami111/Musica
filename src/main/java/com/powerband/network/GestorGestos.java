package com.powerband.network;

import com.powerband.bridge.abstraction.Instrument;
import java.io.IOException;
import java.util.function.LongSupplier;

final class GestorGestos {
    private final Instrument instrumento;
    private final LongSupplier reloj;
    private int nota = 60;
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
        if (Math.abs(giro) < 0.5) {
            giroArmado = true;
        } else if (giroArmado && giro >= 1.5) {
            nota = Math.min(72, nota + 1);
            giroArmado = false;
        } else if (giroArmado && giro <= -1.5) {
            nota = Math.max(48, nota - 1);
            giroArmado = false;
        }

        double magnitud = Math.hypot(Math.hypot(muestra.ax(), muestra.ay()), muestra.az());
        double movimiento = Math.abs(magnitud - 9.80665);
        if (movimiento < 2.0) {
            movimientoArmado = true;
        } else if (movimiento >= 4.0 && movimientoArmado) {
            long ahora = reloj.getAsLong();
            if (!notaTocada || ahora - ultimaNota >= 400_000_000L) {
                instrumento.iniciarNota(nota, 300);
                ultimaNota = ahora;
                notaTocada = true;
                movimientoArmado = false;
            }
        }
    }
}
