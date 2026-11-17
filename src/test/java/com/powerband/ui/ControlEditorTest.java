package com.powerband.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.powerband.common.constants.SynthConstants;
import com.powerband.common.exceptions.PowerBandException;
import com.powerband.common.interfaces.AudioEnginePlayer;
import com.powerband.common.model.RecordingMetadata;
import com.powerband.editor.MusicEditorController;
import com.powerband.editor.RecorderService;
import com.powerband.editor.SesionEditor;
import com.powerband.instrumentos.BusInstrumentos;
import com.powerband.storage.io.ListFileManager;
import com.powerband.storage.io.MusicFileManager;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ControlEditorTest {
    @TempDir
    Path carpeta;

    @Test
    void grabacionPendienteAdmiteCorreccionYReproduccionDetieneFuentes() throws Exception {
        try (Entorno entorno = new Entorno()) {
            entorno.control.iniciarGrabacion();
            entorno.bus.sintetizador(SynthConstants.IDENTIFICADOR_PIANO).iniciarNota(60, 200);
            entorno.control.detenerGrabacion();
            assertTrue(entorno.control.estado().pendiente());
            assertFalse(entorno.control.guardar(" "));
            assertTrue(entorno.control.estado().pendiente());
            assertTrue(entorno.control.guardar("Ensayo"));
            assertEquals("Ensayo", entorno.control.pagina(0).registros().get(0).obtenerNombre());

            entorno.control.iniciarGrabacion();
            entorno.control.detenerGrabacion();
            assertFalse(entorno.control.guardar("Ensayo"));
            assertTrue(entorno.control.guardar("Otro"));

            assertTrue(entorno.control.cargar("Ensayo"));
            entorno.control.reproducir().toCompletableFuture().get(2, TimeUnit.SECONDS);
            entorno.control.parar();
            assertEquals(1, entorno.fuentes.detenciones);
            assertFalse(entorno.control.estado().cargada());
            entorno.control.reiniciarFuentes();
            assertEquals(2, entorno.fuentes.detenciones);
            assertEquals(1, entorno.fuentes.inicios);
        }
    }

    @Test
    void noReproduceSiNoSePuedenDetenerLasFuentes() throws Exception {
        try (Entorno entorno = new Entorno()) {
            entorno.control.iniciarGrabacion();
            entorno.control.detenerGrabacion();
            assertTrue(entorno.control.guardar("Ensayo"));
            assertTrue(entorno.control.cargar("Ensayo"));
            entorno.fuentes.fallar = true;
            assertThrows(IOException.class, entorno.control::reproducir);
            assertTrue(entorno.control.estado().cargada());
            entorno.fuentes.fallar = false;
            entorno.control.reproducir().toCompletableFuture().get(2, TimeUnit.SECONDS);
            entorno.control.parar();
        }
    }

    @Test
    void listaPorPaginasSinCargarElCatalogoCompleto() throws Exception {
        try (Entorno entorno = new Entorno()) {
            for (int numero = 0; numero < 105; numero++) {
                entorno.lista.registrar(new RecordingMetadata("Grabacion " + numero, Instant.ofEpochMilli(numero)));
            }
            ControlEditor.Pagina primera = entorno.control.pagina(0);
            ControlEditor.Pagina segunda = entorno.control.pagina(50);
            ControlEditor.Pagina ultima = entorno.control.pagina(100);
            assertEquals(105, primera.cantidad());
            assertEquals(50, primera.registros().size());
            assertEquals("Grabacion 50", segunda.registros().get(0).obtenerNombre());
            assertEquals(5, ultima.registros().size());
            assertEquals("Grabacion 104", ultima.registros().get(4).obtenerNombre());
            assertEquals(100, entorno.control.pagina(1000).inicio());
        }
    }

    @Test
    void cierreDetieneGrabacionYPideNombreHastaQueSeGuarde() throws Exception {
        try (Entorno entorno = new Entorno()) {
            entorno.control.iniciarGrabacion();
            entorno.bus.sintetizador(SynthConstants.IDENTIFICADOR_BAJO).iniciarNota(36, 200);
            assertEquals(ControlEditor.Cierre.PENDIENTE, entorno.control.prepararCierre());
            assertFalse(entorno.control.estado().grabando());
            assertEquals(ControlEditor.Cierre.PENDIENTE, entorno.control.prepararCierre());
            assertFalse(entorno.control.guardar(" "));
            assertEquals(ControlEditor.Cierre.PENDIENTE, entorno.control.prepararCierre());
            assertTrue(entorno.control.guardar("Cierre"));
            assertEquals(ControlEditor.Cierre.LISTO, entorno.control.prepararCierre());
        }
    }

    @Test
    void cierreInformaSesionFallidaCuandoElGuardadoFalla() throws Exception {
        try (Entorno entorno = new Entorno()) {
            Files.createDirectory(carpeta.resolve("recordings.index"));
            entorno.control.iniciarGrabacion();
            entorno.control.detenerGrabacion();
            assertThrows(PowerBandException.class, () -> entorno.control.guardar("Ensayo"));
            assertEquals(ControlEditor.Cierre.FALLIDA, entorno.control.prepararCierre());
        }
    }

    private final class Entorno implements AutoCloseable {
        private final MusicFileManager musica;
        private final ListFileManager lista;
        private final SesionEditor sesion;
        private final BusInstrumentos bus;
        private final FuentesSimuladas fuentes = new FuentesSimuladas();
        private final ControlEditor control;

        Entorno() throws Exception {
            Path rutaMusica = carpeta.resolve("recordings.music");
            musica = new MusicFileManager(rutaMusica);
            lista = new ListFileManager(carpeta.resolve("recordings.list"));
            RecorderService grabador = new RecorderService(musica, lista);
            AudioEnginePlayer reproductor = comando -> { };
            bus = new BusInstrumentos(reproductor, new MusicEditorController(grabador),
                    grabador, () -> 0L);
            sesion = new SesionEditor(grabador, bus, lista, carpeta.resolve("recordings.index"),
                    rutaMusica, reproductor);
            control = new ControlEditor(sesion, grabador, lista, fuentes);
        }

        @Override
        public void close() throws Exception {
            try {
                sesion.close();
            } finally {
                try {
                    lista.close();
                } finally {
                    musica.close();
                }
            }
        }
    }

    private static final class FuentesSimuladas implements ControlEditor.Fuentes {
        private int inicios;
        private int detenciones;
        private boolean fallar;

        @Override
        public void iniciar() {
            inicios++;
        }

        @Override
        public void detener() throws IOException {
            detenciones++;
            if (fallar) {
                throw new IOException("Fuente no detenida");
            }
        }
    }
}
