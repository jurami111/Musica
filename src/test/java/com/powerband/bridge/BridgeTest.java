package com.powerband.bridge;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.powerband.bridge.abstraction.AudioMicInstrument;
import com.powerband.bridge.abstraction.Instrument;
import com.powerband.bridge.abstraction.MobileSensorInstrument;
import com.powerband.bridge.abstraction.WebcamInstrument;
import com.powerband.bridge.implementor.BassSynthCommand;
import com.powerband.bridge.implementor.EmisorOsc;
import com.powerband.bridge.implementor.LeadSynthCommand;
import com.powerband.bridge.implementor.PadSynthCommand;
import com.powerband.bridge.implementor.SynthImplementor;
import com.powerband.network.BandClient;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

class BridgeTest {
    @Test
    void cadaInstrumentoDelegaEnCualquierSintetizador() throws IOException {
        List<String> llamadas = new ArrayList<>();
        SynthImplementor sintetizador = new SynthImplementor() {
            public void iniciarNota(int nota, int duracionMs) {
                llamadas.add("iniciar " + nota + " " + duracionMs);
            }

            public void terminarNota(int nota) {
                llamadas.add("terminar " + nota);
            }
        };
        List<Instrument> instrumentos = List.of(
                new AudioMicInstrument(sintetizador),
                new MobileSensorInstrument(sintetizador),
                new WebcamInstrument(sintetizador));

        for (Instrument instrumento : instrumentos) {
            instrumento.iniciarNota(60, 500);
            instrumento.terminarNota(60);
        }

        assertEquals(List.of("iniciar 60 500", "terminar 60",
                "iniciar 60 500", "terminar 60",
                "iniciar 60 500", "terminar 60"), llamadas);
    }

    @Test
    void pianoYBajoEnvíanSusControlesYTerminanAutomaticamente() throws IOException {
        EmisorDePrueba emisor = new EmisorDePrueba();
        SynthImplementor piano = new LeadSynthCommand(emisor);
        SynthImplementor bajo = new BassSynthCommand(emisor);

        piano.iniciarNota(60, 1500);
        bajo.iniciarNota(36, 450);
        piano.terminarNota(60);
        bajo.terminarNota(36);

        assertEquals(List.of(
                List.of("classicPiano", "note", 60, "durMs", 1500, "third", 4),
                List.of("stringBass", "note", 36, "durMs", 450)), emisor.mensajes);
    }

    @Test
    void organoLiberaElNodoAnteriorAlRepetirYConservaElNodoSiFalla() throws IOException {
        EmisorDePrueba emisor = new EmisorDePrueba();
        PadSynthCommand organo = new PadSynthCommand(emisor);

        organo.iniciarNota(69, 1000);
        organo.iniciarNota(69, 1000);
        emisor.fallarCambio = true;
        assertThrows(IOException.class, () -> organo.terminarNota(69));
        emisor.fallarCambio = false;
        organo.terminarNota(69);
        organo.terminarNota(69);

        assertEquals(List.of(
                List.of("organ70s", "freq", 440f, "r1", 1f, "r2", 2f, "r3", 3f,
                        "a1", 1f, "a2", 0.5f, "a3", 0.25f),
                List.of(10000, "gate", 0),
                List.of("organ70s", "freq", 440f, "r1", 1f, "r2", 2f, "r3", 3f,
                        "a1", 1f, "a2", 0.5f, "a3", 0.25f),
                List.of(10001, "gate", 0)), emisor.mensajes);
    }

    @Test
    void validaLasNotasYDuracionesAntesDeEnviar() {
        EmisorDePrueba emisor = new EmisorDePrueba();
        List<SynthImplementor> sintetizadores = List.of(
                new LeadSynthCommand(emisor), new BassSynthCommand(emisor),
                new PadSynthCommand(emisor));
        for (SynthImplementor sintetizador : sintetizadores) {
            assertThrows(IllegalArgumentException.class, () -> sintetizador.iniciarNota(-1, 100));
            assertThrows(IllegalArgumentException.class, () -> sintetizador.iniciarNota(128, 100));
            assertThrows(IllegalArgumentException.class, () -> sintetizador.iniciarNota(60, 0));
            assertThrows(IllegalArgumentException.class, () -> sintetizador.terminarNota(128));
        }
        assertEquals(List.of(), emisor.mensajes);
    }

    @Test
    void propagaElFalloDelEmisorHastaElInstrumento() {
        EmisorDePrueba emisor = new EmisorDePrueba();
        emisor.fallarCreacion = true;
        Instrument instrumento = new WebcamInstrument(new LeadSynthCommand(emisor));
        IOException error = assertThrows(IOException.class, () -> instrumento.iniciarNota(60, 500));
        assertEquals("fallo de envío", error.getMessage());
    }

    @Test
    void enviaOscCodificadoPorUdpConNodosCompartidos() throws IOException {
        try (DatagramSocket receptor = new DatagramSocket(0, InetAddress.getLoopbackAddress());
                BandClient cliente = new BandClient(
                        receptor.getLocalAddress().getHostAddress(), receptor.getLocalPort())) {
            receptor.setSoTimeout(2000);
            new BassSynthCommand(cliente).iniciarNota(36, 450);
            assertEquals(List.of("/s_new", ",siiisisi", "stringBass", 10000, 0, 1,
                    "note", 36, "durMs", 450), recibir(receptor));

            new LeadSynthCommand(cliente).iniciarNota(60, 1500);
            assertEquals(List.of("/s_new", ",siiisisisi", "classicPiano", 10001, 0, 1,
                    "note", 60, "durMs", 1500, "third", 4), recibir(receptor));

            PadSynthCommand organo = new PadSynthCommand(cliente);
            organo.iniciarNota(69, 1000);
            assertEquals(List.of("/s_new", ",siiisfsfsfsfsfsfsf", "organ70s", 10002, 0, 1,
                    "freq", 440f, "r1", 1f, "r2", 2f, "r3", 3f,
                    "a1", 1f, "a2", 0.5f, "a3", 0.25f), recibir(receptor));
            organo.terminarNota(69);
            assertEquals(List.of("/n_set", ",isi", 10002, "gate", 0), recibir(receptor));
        }
    }

    private static List<Object> recibir(DatagramSocket receptor) throws IOException {
        byte[] datos = new byte[1024];
        DatagramPacket paquete = new DatagramPacket(datos, datos.length);
        receptor.receive(paquete);
        ByteBuffer buffer = ByteBuffer.wrap(datos, 0, paquete.getLength());
        String ruta = leerCadena(buffer);
        String tipos = leerCadena(buffer);
        List<Object> resultado = new ArrayList<>(List.of(ruta, tipos));
        for (int i = 1; i < tipos.length(); i++) {
            switch (tipos.charAt(i)) {
                case 's' -> resultado.add(leerCadena(buffer));
                case 'i' -> resultado.add(buffer.getInt());
                case 'f' -> resultado.add(buffer.getFloat());
                default -> throw new AssertionError("Tipo OSC inesperado: " + tipos.charAt(i));
            }
        }
        assertEquals(0, buffer.remaining());
        return resultado;
    }

    private static String leerCadena(ByteBuffer buffer) {
        int inicio = buffer.position();
        while (buffer.get() != 0) {
        }
        int fin = buffer.position() - 1;
        String texto = new String(buffer.array(), inicio, fin - inicio, StandardCharsets.US_ASCII);
        while (buffer.position() % 4 != 0) {
            assertEquals(0, buffer.get());
        }
        return texto;
    }

    private static class EmisorDePrueba implements EmisorOsc {
        private final List<List<Object>> mensajes = new ArrayList<>();
        private int siguienteNodo = 10000;
        private boolean fallarCreacion;
        private boolean fallarCambio;

        @Override
        public int crearSintetizador(String nombre, Object... controles) throws IOException {
            if (fallarCreacion) {
                throw new IOException("fallo de envío");
            }
            List<Object> mensaje = new ArrayList<>();
            mensaje.add(nombre);
            mensaje.addAll(Arrays.asList(controles));
            mensajes.add(mensaje);
            return siguienteNodo++;
        }

        @Override
        public void cambiarParametros(int nodo, Object... controles) throws IOException {
            if (fallarCambio) {
                throw new IOException("fallo de envío");
            }
            List<Object> mensaje = new ArrayList<>();
            mensaje.add(nodo);
            mensaje.addAll(Arrays.asList(controles));
            mensajes.add(mensaje);
        }
    }
}
