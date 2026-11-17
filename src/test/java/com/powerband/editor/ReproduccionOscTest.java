package com.powerband.editor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.powerband.bridge.implementor.BassSynthCommand;
import com.powerband.bridge.implementor.LeadSynthCommand;
import com.powerband.bridge.implementor.PadSynthCommand;
import com.powerband.common.constants.BinaryConstants;
import com.powerband.common.constants.SynthConstants;
import com.powerband.common.model.IndexEntry;
import com.powerband.common.model.SynthCommand;
import com.powerband.instrumentos.ReproductorBridge;
import com.powerband.network.BandClient;
import com.powerband.storage.io.IndexFileManager;
import com.powerband.storage.io.MusicFileManager;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ReproduccionOscTest {
    @TempDir
    Path carpeta;

    @Test
    void bufferEnviaParametrosGrabadosPorOscSinDuplicarlos() throws Exception {
        crearGrabacion(
                new SynthCommand(0, SynthConstants.IDENTIFICADOR_PIANO,
                        BinaryConstants.ACCION_TOCAR, 48, 1500, 4),
                new SynthCommand(0, SynthConstants.IDENTIFICADOR_PIANO,
                        BinaryConstants.ACCION_TOCAR, 57, 1200, 3),
                new SynthCommand(0, SynthConstants.IDENTIFICADOR_BAJO,
                        BinaryConstants.ACCION_TOCAR, 36, 450),
                new SynthCommand(0, SynthConstants.IDENTIFICADOR_ORGANO,
                        BinaryConstants.ACCION_INICIAR_NOTA, 69, 1, 2.01, 4, 1, 0.6, 0.3),
                new SynthCommand(0, SynthConstants.IDENTIFICADOR_ORGANO,
                        BinaryConstants.ACCION_TERMINAR_NOTA, 69));
        try (DatagramSocket receptor = new DatagramSocket(0, InetAddress.getLoopbackAddress());
                BandClient cliente = new BandClient(
                        receptor.getLocalAddress().getHostAddress(), receptor.getLocalPort())) {
            receptor.setSoTimeout(2000);
            PlaybackBufferThread hilo = cargar(cliente);
            hilo.start();
            hilo.obtenerResultado().toCompletableFuture().get(2, TimeUnit.SECONDS);
            assertEquals(List.of("/s_new", ",siiisisisi", "classicPiano", 10000, 0, 1,
                    "note", 48, "durMs", 1500, "third", 4), recibir(receptor));
            assertEquals(List.of("/s_new", ",siiisisisi", "classicPiano", 10001, 0, 1,
                    "note", 57, "durMs", 1200, "third", 3), recibir(receptor));
            assertEquals(List.of("/s_new", ",siiisisi", "stringBass", 10002, 0, 1,
                    "note", 36, "durMs", 450), recibir(receptor));
            assertEquals(List.of("/s_new", ",siiisfsfsfsfsfsfsf", "organ70s", 10003, 0, 1,
                    "freq", 440f, "r1", 1f, "r2", 2.01f, "r3", 4f,
                    "a1", 1f, "a2", 0.6f, "a3", 0.3f), recibir(receptor));
            assertEquals(List.of("/n_set", ",isi", 10003, "gate", 0), recibir(receptor));
            receptor.setSoTimeout(100);
            assertThrows(java.net.SocketTimeoutException.class, () -> recibir(receptor));
        }
    }

    @Test
    void falloDeEnvioLlegaAlResultadoDelBuffer() throws Exception {
        crearGrabacion(new SynthCommand(0, SynthConstants.IDENTIFICADOR_BAJO,
                BinaryConstants.ACCION_TOCAR, 36, 450));
        try (BandClient cliente = new BandClient()) {
            PlaybackBufferThread hilo = cargar(cliente);
            cliente.close();
            hilo.start();
            ExecutionException error = assertThrows(ExecutionException.class,
                    () -> hilo.obtenerResultado().toCompletableFuture().get(2, TimeUnit.SECONDS));
            assertInstanceOf(IOException.class, error.getCause());
        }
    }

    @Test
    void liberaNotasDeOrganoSiFaltaElComandoFinal() throws Exception {
        crearGrabacion(new SynthCommand(0, SynthConstants.IDENTIFICADOR_ORGANO,
                BinaryConstants.ACCION_INICIAR_NOTA, 69, 1, 2, 3, 1, 0.5, 0.25));
        try (DatagramSocket receptor = new DatagramSocket(0, InetAddress.getLoopbackAddress());
                BandClient cliente = new BandClient(
                        receptor.getLocalAddress().getHostAddress(), receptor.getLocalPort())) {
            receptor.setSoTimeout(2000);
            PlaybackBufferThread hilo = cargar(cliente);
            hilo.start();
            hilo.obtenerResultado().toCompletableFuture().get(2, TimeUnit.SECONDS);
            assertEquals("/s_new", recibir(receptor).get(0));
            assertEquals(List.of("/n_set", ",isi", 10000, "gate", 0), recibir(receptor));
        }
    }

    private PlaybackBufferThread cargar(BandClient cliente) throws Exception {
        ReproductorBridge reproductor = new ReproductorBridge(new LeadSynthCommand(cliente),
                new BassSynthCommand(cliente), new PadSynthCommand(cliente));
        return PlaybackBufferThread.cargar(carpeta.resolve("index"), carpeta.resolve("music"),
                "Ensayo", reproductor).orElseThrow();
    }

    private void crearGrabacion(SynthCommand... comandos) throws Exception {
        try (MusicFileManager musica = new MusicFileManager(carpeta.resolve("music"));
                IndexFileManager indice = new IndexFileManager(carpeta.resolve("index"))) {
            long posicion = musica.reservarEncabezado();
            for (SynthCommand comando : comandos) {
                musica.escribirComando(comando);
            }
            musica.finalizarGrabacion("Ensayo");
            indice.agregarRegistro(new IndexEntry("Ensayo", posicion));
        }
    }

    private List<Object> recibir(DatagramSocket receptor) throws IOException {
        byte[] datos = new byte[1024];
        DatagramPacket paquete = new DatagramPacket(datos, datos.length);
        receptor.receive(paquete);
        ByteBuffer buffer = ByteBuffer.wrap(datos, 0, paquete.getLength());
        String ruta = leerCadena(buffer);
        String tipos = leerCadena(buffer);
        List<Object> valores = new ArrayList<>(List.of(ruta, tipos));
        for (int i = 1; i < tipos.length(); i++) {
            switch (tipos.charAt(i)) {
                case 's' -> valores.add(leerCadena(buffer));
                case 'i' -> valores.add(buffer.getInt());
                case 'f' -> valores.add(buffer.getFloat());
                default -> throw new AssertionError("Tipo OSC inesperado");
            }
        }
        assertEquals(0, buffer.remaining());
        return valores;
    }

    private String leerCadena(ByteBuffer buffer) {
        int inicio = buffer.position();
        while (buffer.get() != 0) {
        }
        int longitud = buffer.position() - inicio - 1;
        while (buffer.position() % 4 != 0) {
            assertEquals(0, buffer.get());
        }
        return new String(buffer.array(), inicio, longitud, StandardCharsets.US_ASCII);
    }
}
