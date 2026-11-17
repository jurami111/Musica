package com.powerband.editor;

import com.powerband.common.constants.BinaryConstants;
import com.powerband.common.constants.AppConstants;
import com.powerband.common.exceptions.ArchivoCorruptoException;
import com.powerband.common.exceptions.PowerBandException;
import com.powerband.common.exceptions.StorageIOException;
import com.powerband.common.interfaces.AudioEnginePlayer;
import com.powerband.common.model.IndexEntry;
import com.powerband.common.model.RecordingHeader;
import com.powerband.common.model.SynthCommand;
import com.powerband.storage.algorithms.DiskBinarySearch;
import com.powerband.storage.io.MusicFileManager;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.LongSupplier;

public final class PlaybackBufferThread extends Thread {
    private final Path rutaMusica;
    private final long posicionEncabezado;
    private final RecordingHeader encabezado;
    private final AudioEnginePlayer reproductor;
    private final LongSupplier reloj;
    private final Espera espera;
    private final CompletableFuture<Void> resultado = new CompletableFuture<>();

    @FunctionalInterface
    interface Espera {
        void esperar(long nanosegundos) throws InterruptedException;
    }

    private PlaybackBufferThread(Path rutaMusica, long posicionEncabezado,
            RecordingHeader encabezado, AudioEnginePlayer reproductor,
            LongSupplier reloj, Espera espera) {
        super("reproduccion-grabacion");
        this.rutaMusica = rutaMusica;
        this.posicionEncabezado = posicionEncabezado;
        this.encabezado = encabezado;
        this.reproductor = reproductor;
        this.reloj = reloj;
        this.espera = espera;
    }

    public static Optional<PlaybackBufferThread> cargar(Path rutaIndice, Path rutaMusica,
            String nombre, AudioEnginePlayer reproductor)
            throws StorageIOException, ArchivoCorruptoException {
        return cargar(rutaIndice, rutaMusica, nombre, reproductor, System::nanoTime,
                PlaybackBufferThread::dormir);
    }

    static Optional<PlaybackBufferThread> cargar(Path rutaIndice, Path rutaMusica,
            String nombre, AudioEnginePlayer reproductor, LongSupplier reloj, Espera espera)
            throws StorageIOException, ArchivoCorruptoException {
        Objects.requireNonNull(rutaMusica, "rutaMusica");
        Objects.requireNonNull(reproductor, "reproductor");
        Objects.requireNonNull(reloj, "reloj");
        Objects.requireNonNull(espera, "espera");

        Optional<IndexEntry> entrada = new DiskBinarySearch().buscar(rutaIndice, nombre);
        if (entrada.isEmpty()) {
            return Optional.empty();
        }
        Path ruta = rutaMusica.toAbsolutePath().normalize();
        validarArchivo(ruta);
        long posicion = entrada.get().obtenerPosicionEncabezado();
        try (MusicFileManager musica = new MusicFileManager(ruta)) {
            RecordingHeader encabezado = musica.leerEncabezado(posicion);
            if (!nombre.equals(encabezado.obtenerNombre())) {
                throw new ArchivoCorruptoException("El nombre del indice no coincide con el encabezado");
            }
            return Optional.of(new PlaybackBufferThread(ruta, posicion, encabezado,
                    reproductor, reloj, espera));
        }
    }

    public CompletionStage<Void> obtenerResultado() {
        return resultado.minimalCompletionStage();
    }

    @Override
    public void run() {
        Exception fallo = null;
        try {
            long inicio = reloj.getAsLong();
            validarArchivo(rutaMusica);
            try (MusicFileManager musica = new MusicFileManager(rutaMusica)) {
                RecordingHeader actual = musica.leerEncabezado(posicionEncabezado);
                if (!actual.obtenerNombre().equals(encabezado.obtenerNombre())
                        || actual.obtenerInicio() != encabezado.obtenerInicio()
                        || actual.obtenerFin() != encabezado.obtenerFin()) {
                    throw new ArchivoCorruptoException("El encabezado cambio despues de cargar");
                }
                long tiempoAnterior = 0;
                for (long posicion = actual.obtenerInicio(); posicion < actual.obtenerFin();
                        posicion += BinaryConstants.BYTES_REGISTRO) {
                    if (Thread.currentThread().isInterrupted()) {
                        throw new InterruptedException("Reproduccion interrumpida");
                    }
                    SynthCommand comando = musica.leerComando(posicion);
                    if (comando.obtenerTiempo() < tiempoAnterior) {
                        throw new ArchivoCorruptoException("Los tiempos de la grabacion retroceden");
                    }
                    esperarHasta(inicio, comando.obtenerTiempo());
                    reproductor.playCommand(comando);
                    tiempoAnterior = comando.obtenerTiempo();
                }
            }
        } catch (InterruptedException error) {
            Thread.currentThread().interrupt();
            fallo = error;
        } catch (PowerBandException | IOException | RuntimeException error) {
            fallo = error;
        } finally {
            try {
                reproductor.finalizarReproduccion();
            } catch (IOException | RuntimeException error) {
                if (fallo == null) {
                    fallo = error;
                } else if (fallo != error) {
                    fallo.addSuppressed(error);
                }
            }
        }
        if (fallo == null) {
            resultado.complete(null);
        } else {
            resultado.completeExceptionally(fallo);
        }
    }

    private void esperarHasta(long inicio, long tiempo) throws InterruptedException {
        long transcurrido = reloj.getAsLong() - inicio;
        while (transcurrido < tiempo) {
            espera.esperar(tiempo - transcurrido);
            transcurrido = reloj.getAsLong() - inicio;
        }
        if (Thread.currentThread().isInterrupted()) {
            throw new InterruptedException("Reproduccion interrumpida");
        }
    }

    private static void dormir(long nanosegundos) throws InterruptedException {
        long milisegundos = nanosegundos / AppConstants.NANOS_POR_MILISEGUNDO;
        int restantes = (int) (nanosegundos % AppConstants.NANOS_POR_MILISEGUNDO);
        Thread.sleep(milisegundos, restantes);
    }

    private static void validarArchivo(Path ruta) throws StorageIOException {
        try {
            BasicFileAttributes atributos = Files.readAttributes(ruta, BasicFileAttributes.class);
            if (!atributos.isRegularFile()) {
                throw new IOException("La ruta no es un archivo regular: " + ruta);
            }
        } catch (IOException error) {
            throw new StorageIOException("No se pudo acceder al archivo de grabaciones: " + ruta, error);
        }
    }
}