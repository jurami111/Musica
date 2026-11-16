package com.powerband.instrumentos;

import com.powerband.bridge.abstraction.Instrument;
import com.studiohartman.jamepad.ControllerManager;
import com.studiohartman.jamepad.ControllerState;
import java.io.IOException;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class FuenteGamepad implements AutoCloseable {
    private static final Logger REGISTRO = Logger.getLogger(FuenteGamepad.class.getName());
    private final Instrument instrumento;
    private volatile boolean activa;
    private Thread hilo;

    public FuenteGamepad(Instrument instrumento) {
        this.instrumento = Objects.requireNonNull(instrumento, "instrumento");
    }

    public synchronized void iniciar() {
        if (hilo != null && hilo.isAlive()) {
            throw new IllegalStateException("El gamepad ya está iniciado");
        }
        activa = true;
        hilo = new Thread(this::capturar, "captura-gamepad");
        hilo.start();
    }

    private void capturar() {
        ControllerManager gestor = null;
        boolean inicializado = false;
        try {
            gestor = new ControllerManager();
            gestor.initSDLGamepad();
            inicializado = true;
            ProcesadorGamepad procesador = new ProcesadorGamepad(instrumento);
            ControllerState estado = gestor.getState(0);
            if (!estado.isConnected) {
                throw new IOException("No hay un gamepad compatible conectado");
            }
            REGISTRO.info("Gamepad conectado: " + estado.controllerType);
            while (activa) {
                estado = gestor.getState(0);
                if (!estado.isConnected) {
                    throw new IOException("El gamepad se ha desconectado");
                }
                procesador.procesar(new boolean[] {estado.a, estado.b, estado.x, estado.y});
                Thread.sleep(20);
            }
        } catch (InterruptedException error) {
            Thread.currentThread().interrupt();
        } catch (IOException | RuntimeException | LinkageError error) {
            if (activa) {
                REGISTRO.log(Level.SEVERE, "Gamepad desconectado o no disponible", error);
            }
        } finally {
            activa = false;
            if (inicializado) {
                gestor.quitSDLGamepad();
            }
        }
    }

    @Override
    public void close() throws IOException {
        Thread actual;
        synchronized (this) {
            activa = false;
            actual = hilo;
            if (actual != null) {
                actual.interrupt();
            }
        }
        if (actual != null && actual != Thread.currentThread()) {
            try {
                actual.join();
            } catch (InterruptedException error) {
                Thread.currentThread().interrupt();
                throw new IOException("No se pudo esperar el cierre del gamepad", error);
            }
        }
    }
}
