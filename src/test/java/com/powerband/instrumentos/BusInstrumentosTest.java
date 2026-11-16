package com.powerband.instrumentos;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.powerband.bridge.implementor.SynthImplementor;
import com.powerband.bridge.abstraction.InstrumentoGamepad;
import com.powerband.bridge.abstraction.MobileSensorInstrument;
import com.powerband.bridge.abstraction.WebcamInstrument;
import com.powerband.common.constants.BinaryConstants;
import com.powerband.common.constants.SynthConstants;
import com.powerband.common.exceptions.PowerBandException;
import com.powerband.common.model.RecordingHeader;
import com.powerband.common.model.SynthCommand;
import com.powerband.editor.MusicEditorController;
import com.powerband.editor.RecorderService;
import com.powerband.storage.io.ListFileManager;
import com.powerband.storage.io.MusicFileManager;
import com.powerband.network.InstrumentSocketServer;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class BusInstrumentosTest {
    @TempDir
    Path carpeta;

    @Test
    void grabaConcurrentementeYEquilibraElOrganoEnLosLimites() throws Exception {
        List<SynthCommand> enviados = new ArrayList<>();
        AtomicLong reloj = new AtomicLong();
        try (MusicFileManager musica = new MusicFileManager(carpeta.resolve("music"));
                ListFileManager lista = new ListFileManager(carpeta.resolve("list"))) {
            RecorderService grabador = new RecorderService(musica, lista);
            BusInstrumentos bus = new BusInstrumentos(enviados::add,
                    new MusicEditorController(grabador), grabador, reloj::incrementAndGet);
            SynthImplementor piano = bus.sintetizador(SynthConstants.IDENTIFICADOR_PIANO);
            SynthImplementor bajo = bus.sintetizador(SynthConstants.IDENTIFICADOR_BAJO);
            SynthImplementor organo = bus.sintetizador(SynthConstants.IDENTIFICADOR_ORGANO);
            organo.iniciarNota(60, 300);
            assertTrue(bus.iniciarGrabacion("Ensayo"));
            ExecutorService hilos = Executors.newFixedThreadPool(3);
            try {
                List<Future<?>> tareas = new ArrayList<>();
                for (SynthImplementor sintetizador : List.of(piano, bajo, organo)) {
                    tareas.add(hilos.submit(() -> {
                        for (int i = 0; i < 10; i++) {
                            try {
                                sintetizador.iniciarNota(64 + i, 300);
                            } catch (IOException error) {
                                throw new RuntimeException(error);
                            }
                        }
                    }));
                }
                for (Future<?> tarea : tareas) {
                    tarea.get();
                }
            } finally {
                hilos.shutdownNow();
            }
            bus.finalizarGrabacion();
            assertEquals(31, enviados.size());
            assertEquals("Ensayo", lista.leerRegistro(0).obtenerNombre());
            RecordingHeader encabezado = musica.leerEncabezado(0);
            long tiempoAnterior = -1;
            int inicios = 0;
            int finales = 0;
            int total = 0;
            for (long posicion = encabezado.obtenerInicio(); posicion < encabezado.obtenerFin();
                    posicion += BinaryConstants.BYTES_REGISTRO) {
                SynthCommand comando = musica.leerComando(posicion);
                assertTrue(comando.obtenerTiempo() >= tiempoAnterior);
                tiempoAnterior = comando.obtenerTiempo();
                if (comando.obtenerSintetizador() == SynthConstants.IDENTIFICADOR_ORGANO) {
                    if (comando.obtenerAccion() == BinaryConstants.ACCION_INICIAR_NOTA) {
                        inicios++;
                    } else {
                        finales++;
                    }
                }
                total++;
            }
            assertEquals(11, inicios);
            assertEquals(11, finales);
            assertEquals(42, total);
            assertFalse(bus.iniciarGrabacion("Ensayo"));
            assertFalse(bus.iniciarGrabacion(" "));
        }
    }

    @Test
    void comunicaFalloDelObservadorSinOcultarLaCausa() throws Exception {
        try (MusicFileManager musica = new MusicFileManager(carpeta.resolve("music"));
                ListFileManager lista = new ListFileManager(carpeta.resolve("list"))) {
            RecorderService grabador = new RecorderService(musica, lista);
            PowerBandException fallo = new PowerBandException("fallo");
            BusInstrumentos bus = new BusInstrumentos(comando -> { },
                    comando -> { throw fallo; }, grabador, System::nanoTime);
            IOException error = assertThrows(IOException.class,
                    () -> bus.sintetizador(SynthConstants.IDENTIFICADOR_PIANO).iniciarNota(60, 300));
            assertEquals(fallo, error.getCause());
        }
    }

    @Test
    void interrumpeGrabacionSiFallaLaEscritura() throws Exception {
        try (MusicFileManager musica = new MusicFileManager(carpeta.resolve("music"));
                ListFileManager lista = new ListFileManager(carpeta.resolve("list"))) {
            RecorderService grabador = new RecorderService(musica, lista);
            BusInstrumentos bus = new BusInstrumentos(comando -> { },
                    new MusicEditorController(grabador), grabador, System::nanoTime);
            assertTrue(bus.iniciarGrabacion("Ensayo"));
            musica.close();
            IOException error = assertThrows(IOException.class, () -> bus
                    .sintetizador(SynthConstants.IDENTIFICADOR_PIANO).iniciarNota(60, 300));
            assertTrue(error.getCause() instanceof IllegalStateException);
            assertFalse(grabador.estaActiva());
            assertThrows(IllegalStateException.class, bus::finalizarGrabacion);
            assertThrows(IllegalStateException.class, () -> bus.iniciarGrabacion("Otro"));
            assertEquals(0, lista.obtenerCantidad());
        }
    }

    @Test
    void reiniciarNotaDeOrganoTerminaElNodoAntesDeVolverATocarlo() throws Exception {
        List<SynthCommand> enviados = new ArrayList<>();
        try (MusicFileManager musica = new MusicFileManager(carpeta.resolve("music"));
                ListFileManager lista = new ListFileManager(carpeta.resolve("list"))) {
            RecorderService grabador = new RecorderService(musica, lista);
            BusInstrumentos bus = new BusInstrumentos(enviados::add,
                    new MusicEditorController(grabador), grabador, System::nanoTime);
            SynthImplementor organo = bus.sintetizador(SynthConstants.IDENTIFICADOR_ORGANO);
            organo.iniciarNota(60, 300);
            organo.iniciarNota(60, 300);
            organo.terminarNota(60);
            organo.terminarNota(60);
            assertEquals(List.of(BinaryConstants.ACCION_INICIAR_NOTA,
                    BinaryConstants.ACCION_TERMINAR_NOTA,
                    BinaryConstants.ACCION_INICIAR_NOTA,
                    BinaryConstants.ACCION_TERMINAR_NOTA),
                    enviados.stream().map(SynthCommand::obtenerAccion).toList());
        }
    }

    @Test
    void tresFuentesEntreganYGrabanPorElMismoBus() throws Exception {
            List<SynthCommand> enviados = new CopyOnWriteArrayList<>();
            try (MusicFileManager musica = new MusicFileManager(carpeta.resolve("music"));
                    ListFileManager lista = new ListFileManager(carpeta.resolve("list"))) {
                RecorderService grabador = new RecorderService(musica, lista);
                BusInstrumentos bus = new BusInstrumentos(enviados::add,
                        new MusicEditorController(grabador), grabador, System::nanoTime);
                ProcesadorCamara camara = new ProcesadorCamara(new WebcamInstrument(
                        bus.sintetizador(SynthConstants.IDENTIFICADOR_ORGANO)));
                ProcesadorGamepad gamepad = new ProcesadorGamepad(new InstrumentoGamepad(
                        bus.sintetizador(SynthConstants.IDENTIFICADOR_BAJO)));
                try (InstrumentSocketServer servidor = new InstrumentSocketServer(0,
                        new MobileSensorInstrument(bus.sintetizador(SynthConstants.IDENTIFICADOR_PIANO)))) {
                    servidor.iniciar();
                    assertTrue(bus.iniciarGrabacion("Banda"));
                    try (Socket movil = new Socket(InetAddress.getLoopbackAddress(), servidor.puertoLocal())) {
                        movil.getOutputStream().write(
                                "0,0,14,0,0,0\n".getBytes(StandardCharsets.US_ASCII));
                        long limite = System.nanoTime() + TimeUnit.SECONDS.toNanos(2);
                        while (enviados.isEmpty() && System.nanoTime() < limite) {
                            Thread.sleep(5);
                        }
                        assertEquals(1, enviados.size());
                        gamepad.procesar(new boolean[] {true, false, false, false});
                        BufferedImage rojo = new BufferedImage(80, 60, BufferedImage.TYPE_INT_RGB);
                        for (int y = 0; y < 24; y++) {
                            rojo.setRGB(1, y, 0xff0000);
                        }
                        camara.procesar(rojo);
                        camara.procesar(rojo);
                        camara.detenerNota();
                        bus.finalizarGrabacion();
                    }
                }
                RecordingHeader cabecera = musica.leerEncabezado(0);
                List<Integer> synths = new ArrayList<>();
                for (long p = cabecera.obtenerInicio(); p < cabecera.obtenerFin();
                        p += BinaryConstants.BYTES_REGISTRO) {
                    synths.add(musica.leerComando(p).obtenerSintetizador());
                }
                assertEquals(List.of(SynthConstants.IDENTIFICADOR_PIANO,
                        SynthConstants.IDENTIFICADOR_BAJO, SynthConstants.IDENTIFICADOR_ORGANO,
                        SynthConstants.IDENTIFICADOR_ORGANO), synths);
                assertEquals(4, enviados.size());
        }
    }
}
