package com.powerband.editor;

import com.powerband.common.exceptions.PowerBandException;
import com.powerband.common.interfaces.AudioEnginePlayer;
import com.powerband.common.model.RecordingMetadata;
import com.powerband.common.model.ValidadorNombres;
import com.powerband.instrumentos.BusInstrumentos;
import com.powerband.storage.io.ListFileManager;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.concurrent.CompletionStage;
import java.util.function.Consumer;

public final class SesionEditor implements AutoCloseable {
    private final RecorderService grabador;
    private final BusInstrumentos bus;
    private final ListFileManager lista;
    private final Path rutaIndice;
    private final Path rutaMusica;
    private final AudioEnginePlayer reproductor;
    private PlaybackBufferThread cargada;
    private PlaybackBufferThread reproduccion;

    public SesionEditor(RecorderService grabador, BusInstrumentos bus, ListFileManager lista,
            Path rutaIndice, Path rutaMusica, AudioEnginePlayer reproductor) {
        this.grabador = Objects.requireNonNull(grabador, "grabador");
        this.bus = Objects.requireNonNull(bus, "bus");
        this.lista = Objects.requireNonNull(lista, "lista");
        this.rutaIndice = Objects.requireNonNull(rutaIndice, "rutaIndice");
        this.rutaMusica = Objects.requireNonNull(rutaMusica, "rutaMusica");
        this.reproductor = Objects.requireNonNull(reproductor, "reproductor");
    }

    public boolean estaReproduciendo() {
        return reproduccion != null && reproduccion.isAlive();
    }

    public boolean tieneCargada() {
        return cargada != null;
    }

    public void iniciarGrabacion() throws PowerBandException {
        comprobarLibre();
        bus.iniciarGrabacion();
        cargada = null;
    }

    public void detenerGrabacion() throws PowerBandException {
        bus.detenerGrabacion();
    }

    public boolean guardar(String nombre) throws PowerBandException {
        return grabador.guardar(nombre);
    }

    public void listar(Consumer<RecordingMetadata> mostrar) throws PowerBandException {
        comprobarSana();
        long cantidad = lista.obtenerCantidad();
        for (long numero = 0; numero < cantidad; numero++) {
            mostrar.accept(lista.leerRegistro(numero));
        }
    }

    public boolean cargar(String nombre) throws PowerBandException {
        comprobarLibre();
        cargada = null;
        if (!ValidadorNombres.esValido(nombre)) {
            return false;
        }
        if (lista.obtenerCantidad() == 0 && Files.notExists(rutaIndice)) {
            return false;
        }
        cargada = PlaybackBufferThread.cargar(rutaIndice, rutaMusica, nombre, reproductor)
                .orElse(null);
        return cargada != null;
    }

    public CompletionStage<Void> reproducir() {
        comprobarLibre();
        if (cargada == null) {
            throw new IllegalStateException("Primero cargue una grabacion");
        }
        reproduccion = cargada;
        cargada = null;
        reproduccion.start();
        return reproduccion.obtenerResultado();
    }

    private void comprobarLibre() {
        comprobarSana();
        if (estaReproduciendo() || grabador.estaActiva() || grabador.tienePendiente()) {
            throw new IllegalStateException("Hay una grabacion pendiente o una reproduccion en curso");
        }
    }

    private void comprobarSana() {
        if (grabador.estaFallida()) {
            throw new IllegalStateException("La sesion fallo; revise los archivos antes de reiniciar");
        }
    }

    public void parar() throws IOException {
        if (reproduccion != null) {
            reproduccion.interrupt();
            try {
                reproduccion.join();
            } catch (InterruptedException error) {
                Thread.currentThread().interrupt();
                throw new IOException("No se pudo esperar el cierre de reproduccion", error);
            }
        }
    }

    @Override
    public void close() throws IOException {
        parar();
        reproductor.finalizarReproduccion();
    }
}
