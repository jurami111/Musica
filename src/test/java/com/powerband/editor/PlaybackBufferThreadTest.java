package com.powerband.editor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.powerband.common.constants.BinaryConstants;
import com.powerband.common.constants.SynthConstants;
import com.powerband.common.exceptions.ArchivoCorruptoException;
import com.powerband.common.exceptions.StorageIOException;
import com.powerband.common.model.IndexEntry;
import com.powerband.common.model.SynthCommand;
import com.powerband.storage.io.IndexFileManager;
import com.powerband.storage.io.MusicFileManager;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class PlaybackBufferThreadTest {
    @TempDir
    Path carpeta;

    @Test
    void cargaSinReproducirYUsaTiemposRelativosAlInicioDelPlay() throws Exception {
        crearGrabacion(100, 200, 200, 400);
        AtomicLong reloj = new AtomicLong(1000);
        List<Long> momentos = new ArrayList<>();
        List<Long> esperas = new ArrayList<>();
        PlaybackBufferThread hilo = PlaybackBufferThread.cargar(indice(), musica(), "Ensayo",
                comando -> {
                    momentos.add(reloj.get());
                    reloj.addAndGet(30);
                }, reloj::get, nanos -> {
                    esperas.add(nanos);
                    reloj.addAndGet(nanos);
                }).orElseThrow();

        assertTrue(momentos.isEmpty());
        reloj.addAndGet(9000);
        hilo.start();
        hilo.obtenerResultado().toCompletableFuture().get(2, TimeUnit.SECONDS);

        assertEquals(List.of(10100L, 10200L, 10230L, 10400L), momentos);
        assertEquals(List.of(100L, 70L, 140L), esperas);
    }

    @Test
    void vuelveAEsperarSiElRelojDespiertaAntes() throws Exception {
        crearGrabacion(100);
        AtomicLong reloj = new AtomicLong();
        List<Long> momentos = new ArrayList<>();
        PlaybackBufferThread hilo = PlaybackBufferThread.cargar(indice(), musica(), "Ensayo",
                comando -> momentos.add(reloj.get()), reloj::get,
                nanos -> reloj.addAndGet(Math.max(1, nanos / 2))).orElseThrow();
        hilo.start();
        hilo.obtenerResultado().toCompletableFuture().get(2, TimeUnit.SECONDS);
        assertEquals(List.of(100L), momentos);
    }

    @Test
    void noEncuentraNombreYNoCreaArchivoMusica() throws Exception {
        try (IndexFileManager indice = new IndexFileManager(indice())) {
            indice.agregarRegistro(new IndexEntry("Otra", 0));
        }
        assertTrue(PlaybackBufferThread.cargar(indice(), musica(), "Ausente",
                comando -> { }).isEmpty());
        assertFalse(Files.exists(musica()));
        assertThrows(StorageIOException.class, () -> PlaybackBufferThread.cargar(
                carpeta.resolve("no-existe"), musica(), "Ausente", comando -> { }));
    }

    @Test
    void detectaReferenciaIncorrectaYArchivoMusicaAusente() throws Exception {
        crearGrabacion(0);
        try (RandomAccessFile archivo = new RandomAccessFile(indice().toFile(), "rw")) {
            archivo.seek(0);
            archivo.writeChar('X');
        }
        assertThrows(ArchivoCorruptoException.class,
                () -> PlaybackBufferThread.cargar(indice(), musica(), "Xnsayo", comando -> { }));
        Files.delete(musica());
        assertThrows(StorageIOException.class,
                () -> PlaybackBufferThread.cargar(indice(), musica(), "Xnsayo", comando -> { }));
        assertFalse(Files.exists(musica()));
    }

    @Test
    void reproduceGrabacionVaciaYSinComandosDeLaSiguiente() throws Exception {
        crearGrabacion();
        try (MusicFileManager musica = new MusicFileManager(musica())) {
            musica.reservarEncabezado();
            musica.escribirComando(comando(0));
            musica.finalizarGrabacion("Siguiente");
        }
        List<SynthCommand> enviados = new ArrayList<>();
        PlaybackBufferThread hilo = PlaybackBufferThread.cargar(
                indice(), musica(), "Ensayo", enviados::add).orElseThrow();
        hilo.start();
        hilo.obtenerResultado().toCompletableFuture().get(2, TimeUnit.SECONDS);
        assertTrue(enviados.isEmpty());
    }

    @Test
    void detectaTiemposDecrecientesSinEnviarElComandoInvalido() throws Exception {
        crearGrabacion(100, 200);
        try (RandomAccessFile archivo = new RandomAccessFile(musica().toFile(), "rw")) {
            archivo.seek(512 + Integer.BYTES);
            archivo.writeLong(50);
        }
        AtomicLong reloj = new AtomicLong();
        List<Long> enviados = new ArrayList<>();
        PlaybackBufferThread hilo = PlaybackBufferThread.cargar(indice(), musica(), "Ensayo",
                comando -> enviados.add(comando.obtenerTiempo()), reloj::get, reloj::addAndGet)
                .orElseThrow();
        hilo.start();
        ExecutionException error = assertThrows(ExecutionException.class,
                () -> hilo.obtenerResultado().toCompletableFuture().get(2, TimeUnit.SECONDS));
        assertInstanceOf(ArchivoCorruptoException.class, error.getCause());
        assertEquals(List.of(100L), enviados);
    }

    @Test
    void informaFalloDelReproductorYNoContinua() throws Exception {
        crearGrabacion(0, 0);
        IOException fallo = new IOException("Fallo de envio");
        AtomicLong enviados = new AtomicLong();
        PlaybackBufferThread hilo = PlaybackBufferThread.cargar(indice(), musica(), "Ensayo",
                comando -> {
                    enviados.incrementAndGet();
                    throw fallo;
                }).orElseThrow();
        hilo.start();
        ExecutionException error = assertThrows(ExecutionException.class,
                () -> hilo.obtenerResultado().toCompletableFuture().get(2, TimeUnit.SECONDS));
        assertEquals(fallo, error.getCause());
        assertEquals(1, enviados.get());
    }

    @Test
    void interrumpirLaEsperaInformaElErrorYNoEnviaElComando() throws Exception {
        crearGrabacion(100);
        CountDownLatch esperando = new CountDownLatch(1);
        List<SynthCommand> enviados = new ArrayList<>();
        PlaybackBufferThread hilo = PlaybackBufferThread.cargar(indice(), musica(), "Ensayo",
                enviados::add, () -> 0L, nanos -> {
                    esperando.countDown();
                    new CountDownLatch(1).await();
                }).orElseThrow();
        hilo.start();
        assertTrue(esperando.await(2, TimeUnit.SECONDS));
        hilo.interrupt();
        ExecutionException error = assertThrows(ExecutionException.class,
                () -> hilo.obtenerResultado().toCompletableFuture().get(2, TimeUnit.SECONDS));
        assertInstanceOf(InterruptedException.class, error.getCause());
        hilo.join();
        assertTrue(hilo.isInterrupted());
        assertTrue(enviados.isEmpty());
    }

    @Test
    void detectaSiElEncabezadoCambiaEntreCargaYPlay() throws Exception {
        crearGrabacion(0);
        PlaybackBufferThread hilo = PlaybackBufferThread.cargar(
                indice(), musica(), "Ensayo", comando -> { }).orElseThrow();
        try (RandomAccessFile archivo = new RandomAccessFile(musica().toFile(), "rw")) {
            archivo.seek(Integer.BYTES);
            archivo.writeChar('X');
        }
        hilo.start();
        ExecutionException error = assertThrows(ExecutionException.class,
                () -> hilo.obtenerResultado().toCompletableFuture().get(2, TimeUnit.SECONDS));
        assertInstanceOf(ArchivoCorruptoException.class, error.getCause());
    }

    private void crearGrabacion(long... tiempos) throws Exception {
        try (MusicFileManager gestor = new MusicFileManager(musica());
                IndexFileManager indice = new IndexFileManager(indice())) {
            long posicion = gestor.reservarEncabezado();
            for (long tiempo : tiempos) {
                gestor.escribirComando(comando(tiempo));
            }
            gestor.finalizarGrabacion("Ensayo");
            indice.agregarRegistro(new IndexEntry("Ensayo", posicion));
        }
    }

    private static SynthCommand comando(long tiempo) {
        return new SynthCommand(tiempo, SynthConstants.IDENTIFICADOR_BAJO,
                BinaryConstants.ACCION_TOCAR, 36, 450);
    }

    private Path indice() {
        return carpeta.resolve("recordings.index");
    }

    private Path musica() {
        return carpeta.resolve("recordings.music");
    }
}
