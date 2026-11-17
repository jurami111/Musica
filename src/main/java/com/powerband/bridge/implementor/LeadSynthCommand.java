package com.powerband.bridge.implementor;

import com.powerband.common.constants.SynthConstants;
import java.io.IOException;
import java.util.Objects;

public class LeadSynthCommand implements SynthImplementor {
    private final EmisorOsc emisor;

    public LeadSynthCommand(EmisorOsc emisor) {
        this.emisor = Objects.requireNonNull(emisor, "emisor");
    }

    @Override
    public void iniciarNota(int nota, int duracionMs) throws IOException {
        iniciarNota(nota, duracionMs, SynthConstants.TERCERA_MAYOR);
    }

    public void iniciarNota(int nota, int duracionMs, int tercera) throws IOException {
        ValidadorNotas.validarNota(nota);
        ValidadorNotas.validarDuracion(duracionMs);
        if (tercera != SynthConstants.TERCERA_MAYOR && tercera != SynthConstants.TERCERA_MENOR) {
            throw new IllegalArgumentException("La tercera del piano debe ser 3 o 4");
        }
        emisor.crearSintetizador(SynthConstants.NOMBRE_PIANO,
                "note", nota, "durMs", duracionMs, "third", tercera);
    }

    @Override
    public void terminarNota(int nota) {
        ValidadorNotas.validarNota(nota);
    }
}
