package com.powerband.network;

import com.powerband.bridge.abstraction.Instrument;
import com.powerband.bridge.abstraction.MobileSensorInstrument;
import com.powerband.bridge.implementor.LeadSynthCommand;
import com.powerband.common.constants.ConstantesInstrumentos;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

public class InstrumentSocketServer implements AutoCloseable {
    public static final int PUERTO_PREDETERMINADO = ConstantesInstrumentos.PUERTO_MOVIL;
    private static final int MAXIMO_LINEA = ConstantesInstrumentos.MAXIMO_LINEA_SENSORES;
    private static final Logger REGISTRO = Logger.getLogger(InstrumentSocketServer.class.getName());

    private final int puerto;
    private final Instrument instrumento;
    private volatile boolean ejecutando;
    private ServerSocket servidor;
    private Socket cliente;
    private Thread hilo;
    private boolean deteniendo;

    public InstrumentSocketServer(int puerto, Instrument instrumento) {
        this.puerto = puerto;
        this.instrumento = Objects.requireNonNull(instrumento, "instrumento");
    }

    public synchronized void iniciar() throws IOException {
        if (ejecutando || hilo != null || deteniendo) {
            throw new IllegalStateException("El servidor ya fue iniciado");
        }
        servidor = new ServerSocket(puerto);
        ejecutando = true;
        hilo = new Thread(this::escuchar, "servidor-instrumento");
        hilo.start();
    }

    public synchronized int puertoLocal() {
        if (servidor == null) {
            throw new IllegalStateException("El servidor no ha iniciado");
        }
        return servidor.getLocalPort();
    }

    private void escuchar() {
        try {
            while (ejecutando) {
                Socket conexion = servidor.accept();
                synchronized (this) {
                    if (!ejecutando) {
                        conexion.close();
                        break;
                    }
                    cliente = conexion;
                }
                REGISTRO.info("Móvil conectado");
                try (conexion) {
                    recibir(conexion);
                } catch (IOException | IllegalArgumentException error) {
                    if (ejecutando) {
                        REGISTRO.log(Level.WARNING, "Conexión del móvil finalizada con error", error);
                    }
                } finally {
                    synchronized (this) {
                        cliente = null;
                    }
                    REGISTRO.info("Móvil desconectado");
                }
            }
        } catch (IOException error) {
            if (ejecutando) {
                REGISTRO.log(Level.SEVERE, "Error del servidor de instrumentos", error);
            }
        } finally {
            ejecutando = false;
            try {
                servidor.close();
            } catch (IOException error) {
                REGISTRO.log(Level.WARNING, "No se pudo cerrar el servidor", error);
            }
        }
    }

    private void recibir(Socket conexion) throws IOException {
        GestorGestos gestos = new GestorGestos(instrumento, System::nanoTime);
        InputStream entrada = conexion.getInputStream();
        ByteArrayOutputStream linea = new ByteArrayOutputStream();
        int valor;
        while (ejecutando && (valor = entrada.read()) != -1) {
            if (valor == '\n') {
                gestos.procesar(MuestraSensores.interpretar(linea.toString(StandardCharsets.US_ASCII)));
                linea.reset();
            } else {
                if (valor > 127 || linea.size() >= MAXIMO_LINEA) {
                    throw new IllegalArgumentException("Línea de sensor inválida o demasiado larga");
                }
                linea.write(valor);
            }
        }
    }

    @Override
    public void close() throws IOException {
        Thread hiloActual;
        IOException fallo = null;
        synchronized (this) {
            if (deteniendo) {
                throw new IllegalStateException("El servidor ya se está deteniendo");
            }
            deteniendo = true;
            ejecutando = false;
            try {
                if (servidor != null) {
                    servidor.close();
                }
            } catch (IOException error) {
                fallo = error;
            }
            try {
                if (cliente != null) {
                    cliente.close();
                }
            } catch (IOException error) {
                if (fallo == null) {
                    fallo = error;
                } else {
                    fallo.addSuppressed(error);
                }
            }
            hiloActual = hilo;
        }
        if (hiloActual != null && hiloActual != Thread.currentThread()) {
            try {
                hiloActual.join();
            } catch (InterruptedException error) {
                Thread.currentThread().interrupt();
                synchronized (this) {
                    deteniendo = false;
                }
                IOException interrupcion = new IOException("Interrumpido al detener el servidor", error);
                if (fallo != null) {
                    interrupcion.addSuppressed(fallo);
                }
                throw interrupcion;
            }
        }
        synchronized (this) {
            hilo = null;
            servidor = null;
            deteniendo = false;
        }
        if (fallo != null) {
            throw fallo;
        }
    }

    public static void main(String[] args) throws IOException {
        int puerto = args.length == 0 ? PUERTO_PREDETERMINADO : Integer.parseInt(args[0]);
        BandClient clienteOsc = new BandClient();
        InstrumentSocketServer servidor = new InstrumentSocketServer(puerto,
                new MobileSensorInstrument(new LeadSynthCommand(clienteOsc)));
        try {
            servidor.iniciar();
        } catch (IOException | RuntimeException error) {
            clienteOsc.close();
            throw error;
        }
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                servidor.close();
            } catch (IOException error) {
                REGISTRO.log(Level.SEVERE, "No se pudo detener el servidor", error);
            } finally {
                clienteOsc.close();
            }
        }, "cerrar-instrumento"));
        REGISTRO.info("Servidor de instrumentos escuchando en el puerto " + servidor.puertoLocal());
    }
}
