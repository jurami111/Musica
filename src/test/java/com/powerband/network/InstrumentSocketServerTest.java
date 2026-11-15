package com.powerband.network;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.powerband.bridge.abstraction.MobileSensorInstrument;
import com.powerband.bridge.implementor.LeadSynthCommand;
import com.powerband.bridge.implementor.SynthImplementor;
import java.io.IOException;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;

class InstrumentSocketServerTest {
    @Test
    void interpretaSeisNumerosFinitosYRechazaDatosInvalidos() {
        assertEquals(new MuestraSensores(1, -2.5, 9.81, 0, 0, 1.5),
                MuestraSensores.interpretar("1,-2.5,9.81,0,0,1.5"));
        for (String linea : new String[] {
                "0,0,0,0,0", "0,0,0,0,0,0,0", "NaN,0,0,0,0,0",
                "1e999,0,0,0,0,0", "0,0,0,0,0, ", "0x1p0,0,0,0,0,0"
        }) {
            assertThrows(IllegalArgumentException.class, () -> MuestraSensores.interpretar(linea));
        }
    }

    @Test
    void gestosAplicanGiroAntesDeTocarYRequierenRearmeYEspera() throws IOException {
        SintetizadorPrueba sintetizador = new SintetizadorPrueba();
        AtomicLong reloj = new AtomicLong();
        GestorGestos gestos = new GestorGestos(new MobileSensorInstrument(sintetizador), reloj::get);
        MuestraSensores giroYMovimiento = new MuestraSensores(0, 0, 14, 0, 0, 1.5);
        MuestraSensores reposo = new MuestraSensores(0, 0, 9.80665, 0, 0, 0);

        gestos.procesar(giroYMovimiento);
        gestos.procesar(giroYMovimiento);
        gestos.procesar(reposo);
        reloj.set(399_000_000L);
        gestos.procesar(giroYMovimiento);
        assertEquals(61, sintetizador.notas.poll());
        assertNull(sintetizador.notas.poll());

        reloj.set(400_000_000L);
        gestos.procesar(giroYMovimiento);
        assertEquals(62, sintetizador.notas.poll());
        assertNull(sintetizador.notas.poll());
    }

    @Test
    void limitaLasNotasYElGiroNecesitaVolverAlCentro() throws IOException {
        SintetizadorPrueba sintetizador = new SintetizadorPrueba();
        GestorGestos gestos = new GestorGestos(new MobileSensorInstrument(sintetizador), () -> 0);
        for (int i = 0; i < 20; i++) {
            gestos.procesar(new MuestraSensores(0, 0, 9.8, 0, 0, 2));
            gestos.procesar(new MuestraSensores(0, 0, 9.8, 0, 0, 0));
        }
        gestos.procesar(new MuestraSensores(0, 0, 14, 0, 0, 2));
        assertEquals(72, sintetizador.notas.poll());

        SintetizadorPrueba segundo = new SintetizadorPrueba();
        GestorGestos descendente = new GestorGestos(new MobileSensorInstrument(segundo), () -> 0);
        for (int i = 0; i < 20; i++) {
            descendente.procesar(new MuestraSensores(0, 0, 9.8, 0, 0, -2));
            descendente.procesar(new MuestraSensores(0, 0, 9.8, 0, 0, 0));
        }
        descendente.procesar(new MuestraSensores(0, 0, 14, 0, 0, -2));
        assertEquals(48, segundo.notas.poll());
    }

    @Test
    void recibeLineasFragmentadasYReiniciaEstadoAlReconectar() throws Exception {
        SintetizadorPrueba sintetizador = new SintetizadorPrueba();
        try (InstrumentSocketServer servidor = new InstrumentSocketServer(0,
                new MobileSensorInstrument(sintetizador))) {
            servidor.iniciar();
            try (Socket cliente = conectar(servidor)) {
                OutputStream salida = cliente.getOutputStream();
                salida.write("0,0,9.8,0,0,2\n0,0,".getBytes(StandardCharsets.US_ASCII));
                salida.flush();
                assertNull(sintetizador.notas.poll(100, TimeUnit.MILLISECONDS));
                salida.write("14,0,0,0\n".getBytes(StandardCharsets.US_ASCII));
                salida.flush();
                assertEquals(61, sintetizador.notas.poll(2, TimeUnit.SECONDS));
                salida.write("0,0,14,0,0".getBytes(StandardCharsets.US_ASCII));
            }
            try (Socket cliente = conectar(servidor)) {
                cliente.getOutputStream().write("0,0,14,0,0,0\n".getBytes(StandardCharsets.US_ASCII));
                assertEquals(60, sintetizador.notas.poll(2, TimeUnit.SECONDS));
            }
        }
    }

    @Test
    void rechazaLineaInvalidaYLargaYAceptaConexionPosterior() throws Exception {
        SintetizadorPrueba sintetizador = new SintetizadorPrueba();
        try (InstrumentSocketServer servidor = new InstrumentSocketServer(0,
                new MobileSensorInstrument(sintetizador))) {
            servidor.iniciar();
            for (String linea : new String[] {"no-es-una-muestra\n", "1".repeat(257)}) {
                try (Socket cliente = conectar(servidor)) {
                    cliente.setSoTimeout(2000);
                    cliente.getOutputStream().write(linea.getBytes(StandardCharsets.US_ASCII));
                    assertEquals(-1, cliente.getInputStream().read());
                }
            }
            try (Socket cliente = conectar(servidor)) {
                cliente.getOutputStream().write("0,0,14,0,0,0\n".getBytes(StandardCharsets.US_ASCII));
                assertEquals(60, sintetizador.notas.poll(2, TimeUnit.SECONDS));
            }
        }
    }

    @Test
    void cerrarDetieneConexionActivaYLiberaPuerto() throws Exception {
        SintetizadorPrueba sintetizador = new SintetizadorPrueba();
        InstrumentSocketServer servidor = new InstrumentSocketServer(0,
                new MobileSensorInstrument(sintetizador));
        servidor.iniciar();
        int puerto = servidor.puertoLocal();
        try (Socket cliente = conectar(servidor)) {
            cliente.setSoTimeout(2000);
            servidor.close();
            assertEquals(-1, cliente.getInputStream().read());
        } finally {
            servidor.close();
        }
        try (java.net.ServerSocket comprobacion = new java.net.ServerSocket(puerto)) {
            assertTrue(comprobacion.isBound());
        }
    }

    @Test
    void transmiteUnGestoTcpComoMensajeOscDePiano() throws Exception {
        InetAddress local = InetAddress.getLoopbackAddress();
        try (DatagramSocket receptor = new DatagramSocket(0, local);
                BandClient clienteOsc = new BandClient(local.getHostAddress(), receptor.getLocalPort());
                InstrumentSocketServer servidor = new InstrumentSocketServer(0,
                        new MobileSensorInstrument(new LeadSynthCommand(clienteOsc)))) {
            receptor.setSoTimeout(2000);
            servidor.iniciar();
            try (Socket cliente = conectar(servidor)) {
                cliente.getOutputStream().write(
                        "0,0,14,0,0,2\n".getBytes(StandardCharsets.US_ASCII));
                byte[] datos = new byte[256];
                DatagramPacket paquete = new DatagramPacket(datos, datos.length);
                receptor.receive(paquete);
                ByteBuffer mensaje = ByteBuffer.wrap(datos, 0, paquete.getLength());
                assertEquals("/s_new", leerCadena(mensaje));
                assertEquals(",siiisisisi", leerCadena(mensaje));
                assertEquals("classicPiano", leerCadena(mensaje));
                assertEquals(10000, mensaje.getInt());
                assertEquals(0, mensaje.getInt());
                assertEquals(1, mensaje.getInt());
                assertEquals("note", leerCadena(mensaje));
                assertEquals(61, mensaje.getInt());
                assertEquals("durMs", leerCadena(mensaje));
                assertEquals(300, mensaje.getInt());
                assertEquals("third", leerCadena(mensaje));
                assertEquals(4, mensaje.getInt());
                assertEquals(0, mensaje.remaining());
            }
        }
    }

    private String leerCadena(ByteBuffer buffer) {
        int inicio = buffer.position();
        while (buffer.get() != 0) {
        }
        int fin = buffer.position() - 1;
        while (buffer.position() % 4 != 0) {
            assertEquals(0, buffer.get());
        }
        return new String(buffer.array(), inicio, fin - inicio, StandardCharsets.US_ASCII);
    }

    private Socket conectar(InstrumentSocketServer servidor) throws IOException {
        return new Socket(InetAddress.getLoopbackAddress(), servidor.puertoLocal());
    }

    private static class SintetizadorPrueba implements SynthImplementor {
        private final BlockingQueue<Integer> notas = new LinkedBlockingQueue<>();

        @Override
        public void iniciarNota(int nota, int duracionMs) {
            assertEquals(300, duracionMs);
            notas.add(nota);
        }

        @Override
        public void terminarNota(int nota) {
        }
    }
}
