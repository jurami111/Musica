package com.powerband.network;

import com.powerband.bridge.implementor.EmisorOsc;
import com.powerband.common.constants.SynthConstants;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;

public class BandClient implements EmisorOsc, AutoCloseable {
    private final DatagramSocket socket;
    private final InetAddress direccion;
    private final int puerto;
    private int siguienteNodo = 10000;

    public BandClient() throws IOException {
        this(SynthConstants.DIRECCION_SUPERCOLLIDER, SynthConstants.PUERTO_OSC);
    }

    public BandClient(String direccion, int puerto) throws IOException {
        this.direccion = InetAddress.getByName(direccion);
        this.puerto = puerto;
        this.socket = new DatagramSocket();
    }

    @Override
    public synchronized int crearSintetizador(String nombre, Object... controles) throws IOException {
        int nodo = siguienteNodo++;
        Object[] argumentos = new Object[4 + controles.length];
        argumentos[0] = nombre;
        argumentos[1] = nodo;
        argumentos[2] = 0;
        argumentos[3] = 1;
        System.arraycopy(controles, 0, argumentos, 4, controles.length);
        enviar(SynthConstants.MENSAJE_CREAR_SINTETIZADOR, argumentos);
        return nodo;
    }

    @Override
    public void cambiarParametros(int nodo, Object... controles) throws IOException {
        Object[] argumentos = new Object[1 + controles.length];
        argumentos[0] = nodo;
        System.arraycopy(controles, 0, argumentos, 1, controles.length);
        enviar(SynthConstants.MENSAJE_CAMBIAR_PARAMETROS, argumentos);
    }

    private void enviar(String ruta, Object... argumentos) throws IOException {
        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        escribirCadena(salida, ruta);
        StringBuilder tipos = new StringBuilder(",");
        for (Object argumento : argumentos) {
            if (argumento instanceof Integer) {
                tipos.append('i');
            } else if (argumento instanceof Float) {
                tipos.append('f');
            } else if (argumento instanceof String) {
                tipos.append('s');
            } else {
                throw new IllegalArgumentException("Tipo OSC no soportado: " + argumento);
            }
        }
        escribirCadena(salida, tipos.toString());
        for (Object argumento : argumentos) {
            if (argumento instanceof Integer valor) {
                escribirEntero(salida, valor);
            } else if (argumento instanceof Float valor) {
                escribirEntero(salida, Float.floatToIntBits(valor));
            } else {
                escribirCadena(salida, (String) argumento);
            }
        }
        byte[] datos = salida.toByteArray();
        socket.send(new DatagramPacket(datos, datos.length, direccion, puerto));
    }

    private static void escribirEntero(ByteArrayOutputStream salida, int valor) {
        salida.write(valor >>> 24);
        salida.write(valor >>> 16);
        salida.write(valor >>> 8);
        salida.write(valor);
    }

    private static void escribirCadena(ByteArrayOutputStream salida, String texto) {
        if (!StandardCharsets.US_ASCII.newEncoder().canEncode(texto)) {
            throw new IllegalArgumentException("La cadena OSC debe ser ASCII");
        }
        byte[] datos = texto.getBytes(StandardCharsets.US_ASCII);
        salida.writeBytes(datos);
        int relleno = 4 - datos.length % 4;
        for (int i = 0; i < relleno; i++) {
            salida.write(0);
        }
    }

    @Override
    public void close() {
        socket.close();
    }
}
