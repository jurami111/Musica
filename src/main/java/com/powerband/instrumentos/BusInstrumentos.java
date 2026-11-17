package com.powerband.instrumentos;

import com.powerband.bridge.implementor.SynthImplementor;
import com.powerband.common.constants.BinaryConstants;
import com.powerband.common.constants.SynthConstants;
import com.powerband.common.exceptions.PowerBandException;
import com.powerband.common.interfaces.AudioEnginePlayer;
import com.powerband.common.interfaces.CommandObserver;
import com.powerband.common.model.SynthCommand;
import com.powerband.editor.RecorderService;
import java.io.IOException;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.function.LongSupplier;

public final class BusInstrumentos {
    private final AudioEnginePlayer reproductor;
    private final CommandObserver editor;
    private final RecorderService grabador;
    private final LongSupplier reloj;
    private final long inicioSesion;
    private final Map<Integer, double[]> organoActivo = new TreeMap<>();
    private long inicioGrabacion;

    public BusInstrumentos(AudioEnginePlayer reproductor, CommandObserver editor,
            RecorderService grabador, LongSupplier reloj) {
        this.reproductor = Objects.requireNonNull(reproductor, "reproductor");
        this.editor = Objects.requireNonNull(editor, "editor");
        this.grabador = Objects.requireNonNull(grabador, "grabador");
        this.reloj = Objects.requireNonNull(reloj, "reloj");
        inicioSesion = reloj.getAsLong();
    }

    public SynthImplementor sintetizador(int identificador) {
        if (identificador != SynthConstants.IDENTIFICADOR_PIANO
                && identificador != SynthConstants.IDENTIFICADOR_BAJO
                && identificador != SynthConstants.IDENTIFICADOR_ORGANO) {
            throw new IllegalArgumentException("Sintetizador desconocido");
        }
        return new SynthImplementor() {
            @Override
            public void iniciarNota(int nota, int duracionMs) throws IOException {
                if (duracionMs <= 0) {
                    throw new IllegalArgumentException("La duración debe ser positiva");
                }
                if (identificador == SynthConstants.IDENTIFICADOR_ORGANO) {
                    publicar(identificador, BinaryConstants.ACCION_INICIAR_NOTA,
                            nota, SynthConstants.R1_ORGANO, SynthConstants.R2_ORGANO, SynthConstants.R3_ORGANO,
                            SynthConstants.A1_ORGANO, SynthConstants.A2_ORGANO, SynthConstants.A3_ORGANO);
                } else if (identificador == SynthConstants.IDENTIFICADOR_PIANO) {
                    publicar(identificador, BinaryConstants.ACCION_TOCAR, nota, duracionMs,
                            SynthConstants.TERCERA_MAYOR);
                } else {
                    publicar(identificador, BinaryConstants.ACCION_TOCAR, nota, duracionMs);
                }
            }

            @Override
            public void terminarNota(int nota) throws IOException {
                if (identificador == SynthConstants.IDENTIFICADOR_ORGANO) {
                    publicar(identificador, BinaryConstants.ACCION_TERMINAR_NOTA, nota);
                } else if (nota < SynthConstants.NOTA_MIDI_MINIMA || nota > SynthConstants.NOTA_MIDI_MAXIMA) {
                    throw new IllegalArgumentException("Nota MIDI inválida");
                }
            }
        };
    }

    private synchronized void publicar(int sintetizador, int accion, double... parametros)
            throws IOException {
        if (sintetizador == SynthConstants.IDENTIFICADOR_ORGANO
                && accion == BinaryConstants.ACCION_INICIAR_NOTA
                && organoActivo.containsKey((int) parametros[0])) {
            publicar(sintetizador, BinaryConstants.ACCION_TERMINAR_NOTA, parametros[0]);
        }
        if (sintetizador == SynthConstants.IDENTIFICADOR_ORGANO
                && accion == BinaryConstants.ACCION_TERMINAR_NOTA
                && !organoActivo.containsKey((int) parametros[0])) {
            new SynthCommand(0, sintetizador, accion, parametros);
            return;
        }
        long origen = grabador.estaActiva() ? inicioGrabacion : inicioSesion;
        SynthCommand comando = new SynthCommand(reloj.getAsLong() - origen, sintetizador, accion, parametros);
        reproductor.reproducirComando(comando);
        if (sintetizador == SynthConstants.IDENTIFICADOR_ORGANO) {
            int nota = (int) parametros[0];
            if (accion == BinaryConstants.ACCION_INICIAR_NOTA) {
                organoActivo.put(nota, parametros.clone());
            } else {
                organoActivo.remove(nota);
            }
        }
        try {
            editor.observarComando(comando);
        } catch (PowerBandException | IllegalStateException error) {
            throw new IOException("No se pudo notificar el comando al editor", error);
        }
    }

    public synchronized boolean iniciarGrabacion(String nombre) throws PowerBandException {
        if (!grabador.iniciar(nombre)) {
            return false;
        }
        registrarInicio();
        return true;
    }

    public synchronized void iniciarGrabacion() throws PowerBandException {
        grabador.iniciar();
        registrarInicio();
    }

    private void registrarInicio() throws PowerBandException {
        inicioGrabacion = reloj.getAsLong();
        for (double[] parametros : organoActivo.values()) {
            editor.observarComando(new SynthCommand(0, SynthConstants.IDENTIFICADOR_ORGANO,
                    BinaryConstants.ACCION_INICIAR_NOTA, parametros));
        }
    }

    public synchronized void finalizarGrabacion() throws PowerBandException {
        registrarFinal();
        grabador.finalizar();
    }

    public synchronized void detenerGrabacion() throws PowerBandException {
        registrarFinal();
        grabador.detener();
    }

    private void registrarFinal() throws PowerBandException {
        if (!grabador.estaActiva()) {
            throw new IllegalStateException("No hay una grabación activa");
        }
        long tiempo = reloj.getAsLong() - inicioGrabacion;
        for (int nota : organoActivo.keySet()) {
            editor.observarComando(new SynthCommand(tiempo, SynthConstants.IDENTIFICADOR_ORGANO,
                    BinaryConstants.ACCION_TERMINAR_NOTA, nota));
        }
    }
}
