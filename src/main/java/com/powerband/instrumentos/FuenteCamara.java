package com.powerband.instrumentos;

import com.powerband.bridge.abstraction.Instrument;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bytedeco.javacpp.indexer.UByteIndexer;
import org.bytedeco.opencv.opencv_core.Mat;
import org.bytedeco.opencv.opencv_core.Size;
import org.bytedeco.opencv.opencv_videoio.VideoCapture;
import static org.bytedeco.opencv.global.opencv_imgproc.INTER_NEAREST;
import static org.bytedeco.opencv.global.opencv_imgproc.resize;

public final class FuenteCamara implements AutoCloseable {
    private static final Logger REGISTRO = Logger.getLogger(FuenteCamara.class.getName());
    private final Instrument instrumento;
    private volatile boolean activa;
    private Thread hilo;

    public FuenteCamara(Instrument instrumento) {
        this.instrumento = java.util.Objects.requireNonNull(instrumento, "instrumento");
    }

    public synchronized void iniciar() {
        if (hilo != null && hilo.isAlive()) {
            throw new IllegalStateException("La cámara ya está iniciada");
        }
        activa = true;
        hilo = new Thread(this::capturar, "captura-camara");
        hilo.start();
    }

    private void capturar() {
        ProcesadorCamara procesador = new ProcesadorCamara(instrumento);
        try (VideoCapture camara = new VideoCapture(0);
                Mat cuadro = new Mat(); Mat reducido = new Mat(); Size tamano = new Size(80, 60)) {
            if (!camara.isOpened()) {
                throw new IOException("No se pudo abrir la cámara predeterminada");
            }
            REGISTRO.info("Cámara conectada");
            while (activa) {
                long inicio = System.nanoTime();
                if (!camara.read(cuadro) || cuadro.empty()) {
                    throw new IOException("No se pudo leer un cuadro de la cámara");
                }
                if (cuadro.channels() != 3) {
                    throw new IOException("La cámara no produjo imágenes BGR");
                }
                resize(cuadro, reducido, tamano, 0, 0, INTER_NEAREST);
                BufferedImage imagen = new BufferedImage(80, 60, BufferedImage.TYPE_INT_RGB);
                try (UByteIndexer pixeles = reducido.createIndexer(false)) {
                    for (int y = 0; y < 60; y++) {
                        for (int x = 0; x < 80; x++) {
                            int azul = pixeles.get(y, x, 0);
                            int verde = pixeles.get(y, x, 1);
                            int rojo = pixeles.get(y, x, 2);
                            imagen.setRGB(x, y, (rojo << 16) | (verde << 8) | azul);
                        }
                    }
                }
                procesador.procesar(imagen);
                long espera = 100_000_000L - (System.nanoTime() - inicio);
                if (espera > 0) {
                    Thread.sleep(espera / 1_000_000L, (int) (espera % 1_000_000L));
                }
            }
        } catch (InterruptedException error) {
            Thread.currentThread().interrupt();
        } catch (IOException | RuntimeException | LinkageError error) {
            if (activa) {
                REGISTRO.log(Level.SEVERE, "Cámara desconectada o no disponible", error);
            }
        } finally {
            activa = false;
            try {
                procesador.detenerNota();
            } catch (IOException error) {
                REGISTRO.log(Level.SEVERE, "No se pudo terminar la nota de la cámara", error);
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
                throw new IOException("No se pudo esperar el cierre de la cámara", error);
            }
        }
    }
}
