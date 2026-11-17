package com.powerband.editor;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.powerband.common.constants.BinaryConstants;
import com.powerband.common.constants.SynthConstants;
import com.powerband.common.exceptions.PowerBandException;
import com.powerband.common.interfaces.AudioEnginePlayer;
import com.powerband.common.model.SynthCommand;
import com.powerband.instrumentos.BusInstrumentos;
import com.powerband.storage.io.IndexFileManager;
import com.powerband.storage.io.ListFileManager;
import com.powerband.storage.io.MusicFileManager;
import java.nio.file.Files;
import java.nio.file.Path;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.CountDownLatch;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SesionEditorTest {
    @TempDir
    Path carpeta;

    @Test
    void guardaOrdenaListaYReproduceDespuesDeReabrirLosArchivos() throws Exception {
        List<SynthCommand> enviados = new ArrayList<>();
        try (MusicFileManager musica = new MusicFileManager(musica());
                ListFileManager lista = new ListFileManager(lista())) {
            RecorderService grabador = new RecorderService(musica, lista);
            BusInstrumentos bus = crearBus(grabador);
            try (SesionEditor sesion = crearSesion(grabador, bus, lista, enviados::add)) {
                assertFalse(sesion.cargar("Ausente"));
                sesion.iniciarGrabacion();
                bus.sintetizador(SynthConstants.IDENTIFICADOR_PIANO).iniciarNota(60, 300);
                sesion.detenerGrabacion();
                long tamanoDetenido = musica.obtenerTamano();
                assertFalse(sesion.guardar(" "));
                bus.sintetizador(SynthConstants.IDENTIFICADOR_PIANO).iniciarNota(61, 300);
                assertEquals(tamanoDetenido, musica.obtenerTamano());
                assertTrue(sesion.guardar("Zarza"));

                sesion.iniciarGrabacion();
                bus.sintetizador(SynthConstants.IDENTIFICADOR_BAJO).iniciarNota(36, 300);
                sesion.detenerGrabacion();
                assertFalse(sesion.guardar("Zarza"));
                assertTrue(grabador.tienePendiente());
                assertTrue(sesion.guardar("Alba"));

                List<String> nombres = new ArrayList<>();
                sesion.listar(registro -> nombres.add(registro.obtenerNombre()));
                assertEquals(List.of("Zarza", "Alba"), nombres);
                assertFalse(lista.leerRegistro(1).obtenerFechaHora()
                        .isBefore(lista.leerRegistro(0).obtenerFechaHora()));
                try (IndexFileManager indice = new IndexFileManager(indice())) {
                    assertEquals(2, indice.obtenerCantidad());
                    assertEquals("Alba", indice.leerRegistro(0).obtenerNombre());
                    assertEquals(2L * BinaryConstants.BYTES_REGISTRO,
                            indice.leerRegistro(0).obtenerPosicionEncabezado());
                    assertEquals("Zarza", indice.leerRegistro(1).obtenerNombre());
                    assertEquals(0, indice.leerRegistro(1).obtenerPosicionEncabezado());
                }
            }
        }
        try (MusicFileManager musica = new MusicFileManager(musica());
                ListFileManager lista = new ListFileManager(lista())) {
            RecorderService grabador = new RecorderService(musica, lista);
            try (SesionEditor sesion = crearSesion(grabador, crearBus(grabador), lista, enviados::add)) {
                assertTrue(sesion.cargar("Zarza"));
                assertTrue(enviados.isEmpty());
                sesion.reproducir().toCompletableFuture().get(2, TimeUnit.SECONDS);
                assertEquals(1, enviados.size());
                assertEquals(SynthConstants.IDENTIFICADOR_PIANO, enviados.get(0).obtenerSintetizador());
                assertArrayEquals(new double[] {60, 300, 4}, enviados.get(0).obtenerParametros());
                assertFalse(sesion.tieneCargada());
                assertFalse(sesion.cargar("Ausente"));
                assertFalse(sesion.tieneCargada());
            }
        }
    }

    @Test
    void bloqueaNuevasOperacionesSiFallaLaActualizacionDelIndice() throws Exception {
        Files.createDirectory(indice());
        try (MusicFileManager musica = new MusicFileManager(musica());
                ListFileManager lista = new ListFileManager(lista())) {
            RecorderService grabador = new RecorderService(musica, lista);
            try (SesionEditor sesion = crearSesion(grabador, crearBus(grabador), lista, comando -> { })) {
                sesion.iniciarGrabacion();
                sesion.detenerGrabacion();
                PowerBandException error = assertThrows(PowerBandException.class,
                        () -> sesion.guardar("Ensayo"));
                assertTrue(error.getMessage().contains("actualizar recordings.index"));
                assertNotNull(error.getCause());
                assertTrue(grabador.estaFallida());
                assertEquals(0, lista.obtenerCantidad());
                assertThrows(IllegalStateException.class, sesion::iniciarGrabacion);
                assertThrows(IllegalStateException.class, () -> sesion.cargar("Ensayo"));
            }
        }
    }

    @Test
    void noPermiteCargarMientrasSeGrabaONombra() throws Exception {
        try (MusicFileManager musica = new MusicFileManager(musica());
                ListFileManager lista = new ListFileManager(lista())) {
            RecorderService grabador = new RecorderService(musica, lista);
            try (SesionEditor sesion = crearSesion(grabador, crearBus(grabador), lista, comando -> { })) {
                sesion.iniciarGrabacion();
                assertThrows(IllegalStateException.class, () -> sesion.cargar("Ensayo"));
                sesion.detenerGrabacion();
                assertThrows(IllegalStateException.class, sesion::iniciarGrabacion);
                assertThrows(IllegalStateException.class, () -> sesion.cargar("Ensayo"));
                assertTrue(sesion.guardar("Ensayo"));
                assertTrue(sesion.cargar("Ensayo"));
                sesion.iniciarGrabacion();
                assertFalse(sesion.tieneCargada());
                sesion.detenerGrabacion();
                assertTrue(sesion.guardar("Otra"));
            }
        }
    }

    @Test
    void noModificaArchivosNiIniciaOtroPlayMientrasReproduce() throws Exception {
        CountDownLatch enviando = new CountDownLatch(1);
        CountDownLatch continuar = new CountDownLatch(1);
        AudioEnginePlayer receptor = comando -> {
            enviando.countDown();
            try {
                continuar.await();
            } catch (InterruptedException error) {
                Thread.currentThread().interrupt();
                throw new IOException("Envio interrumpido", error);
            }
        };
        try (MusicFileManager musica = new MusicFileManager(musica());
                ListFileManager lista = new ListFileManager(lista())) {
            RecorderService grabador = new RecorderService(musica, lista);
            BusInstrumentos bus = crearBus(grabador);
            try (SesionEditor sesion = crearSesion(grabador, bus, lista, receptor)) {
                sesion.iniciarGrabacion();
                bus.sintetizador(SynthConstants.IDENTIFICADOR_BAJO).iniciarNota(36, 300);
                sesion.detenerGrabacion();
                assertTrue(sesion.guardar("Ensayo"));
                assertTrue(sesion.cargar("Ensayo"));
                var resultado = sesion.reproducir();
                try {
                    assertTrue(enviando.await(2, TimeUnit.SECONDS));
                    long tamano = musica.obtenerTamano();
                    assertThrows(IllegalStateException.class, sesion::iniciarGrabacion);
                    assertThrows(IllegalStateException.class, () -> sesion.cargar("Ensayo"));
                    assertThrows(IllegalStateException.class, sesion::reproducir);
                    assertEquals(tamano, musica.obtenerTamano());
                } finally {
                    continuar.countDown();
                }
                resultado.toCompletableFuture().get(2, TimeUnit.SECONDS);
                sesion.parar();
                assertTrue(sesion.cargar("Ensayo"));
            }
        } finally {
            continuar.countDown();
        }
    }

    private BusInstrumentos crearBus(RecorderService grabador) {
        return new BusInstrumentos(comando -> { }, new MusicEditorController(grabador),
                grabador, () -> 0L);
    }

    private SesionEditor crearSesion(RecorderService grabador, BusInstrumentos bus,
            ListFileManager lista, AudioEnginePlayer reproductor) {
        return new SesionEditor(grabador, bus, lista, indice(), musica(), reproductor);
    }

    private Path musica() {
        return carpeta.resolve("recordings.music");
    }

    private Path lista() {
        return carpeta.resolve("recordings.list");
    }

    private Path indice() {
        return carpeta.resolve("recordings.index");
    }
}
