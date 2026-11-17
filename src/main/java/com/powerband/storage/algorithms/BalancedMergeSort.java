package com.powerband.storage.algorithms;

import com.powerband.common.constants.AppConstants;
import com.powerband.common.exceptions.DiskSortException;
import com.powerband.common.exceptions.PowerBandException;
import com.powerband.common.model.IndexEntry;
import com.powerband.storage.io.IndexFileManager;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Objects;

public final class BalancedMergeSort {
    public void ordenar() throws DiskSortException {
        ordenar(AppConstants.RUTA_INDICE);
    }

    public void ordenar(Path ruta) throws DiskSortException {
        Objects.requireNonNull(ruta, "ruta");
        Path indice = ruta.toAbsolutePath().normalize();
        try {
            BasicFileAttributes atributos = Files.readAttributes(
                    indice, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
            if (!atributos.isRegularFile()) {
                throw new IOException("El indice debe ser un archivo regular: " + indice);
            }

            long cantidad;
            try (IndexFileManager entrada = new IndexFileManager(indice)) {
                cantidad = entrada.obtenerCantidad();
                if (cantidad == 1) {
                    entrada.leerRegistro(0);
                }
            }
            if (cantidad < 2) {
                return;
            }

            try (Temporales temporales = new Temporales(indice.getParent())) {
                Path entrada = indice;
                Path salida = temporales.resultadoUno;
                long bloque = 1;
                while (true) {
                    vaciar(temporales.izquierdo);
                    vaciar(temporales.derecho);
                    distribuir(entrada, temporales.izquierdo, temporales.derecho, bloque);
                    vaciar(salida);
                    mezclar(temporales.izquierdo, temporales.derecho, salida, bloque);
                    if (bloque >= cantidad - bloque) {
                        break;
                    }
                    bloque *= 2;
                    entrada = salida;
                    salida = salida.equals(temporales.resultadoUno)
                            ? temporales.resultadoDos : temporales.resultadoUno;
                }
                Files.move(salida, indice, StandardCopyOption.ATOMIC_MOVE,
                        StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException | PowerBandException error) {
            throw new DiskSortException(
                    "No se pudo completar el ordenamiento del indice; revise tambien la limpieza de temporales",
                    error);
        }
    }

    private void distribuir(Path origen, Path izquierdo, Path derecho, long bloque)
            throws PowerBandException {
        try (IndexFileManager entrada = new IndexFileManager(origen);
                IndexFileManager salidaIzquierda = new IndexFileManager(izquierdo);
                IndexFileManager salidaDerecha = new IndexFileManager(derecho)) {
            long cantidad = entrada.obtenerCantidad();
            long restantesEnBloque = bloque;
            boolean escribirIzquierda = true;
            for (long numero = 0; numero < cantidad; numero++) {
                IndexEntry registro = entrada.leerRegistro(numero);
                if (escribirIzquierda) {
                    salidaIzquierda.agregarRegistro(registro);
                } else {
                    salidaDerecha.agregarRegistro(registro);
                }
                restantesEnBloque--;
                if (restantesEnBloque == 0) {
                    escribirIzquierda = !escribirIzquierda;
                    restantesEnBloque = bloque;
                }
            }
        }
    }

    private void mezclar(Path izquierdo, Path derecho, Path resultado, long bloque)
            throws PowerBandException {
        try (IndexFileManager entradaIzquierda = new IndexFileManager(izquierdo);
                IndexFileManager entradaDerecha = new IndexFileManager(derecho);
                IndexFileManager salida = new IndexFileManager(resultado)) {
            long cantidadIzquierda = entradaIzquierda.obtenerCantidad();
            long cantidadDerecha = entradaDerecha.obtenerCantidad();
            long posicionIzquierda = 0;
            long posicionDerecha = 0;
            while (posicionIzquierda < cantidadIzquierda || posicionDerecha < cantidadDerecha) {
                long finIzquierdo = posicionIzquierda
                        + Math.min(bloque, cantidadIzquierda - posicionIzquierda);
                long finDerecho = posicionDerecha
                        + Math.min(bloque, cantidadDerecha - posicionDerecha);
                IndexEntry actualIzquierdo = null;
                IndexEntry actualDerecho = null;
                while (posicionIzquierda < finIzquierdo || posicionDerecha < finDerecho
                        || actualIzquierdo != null || actualDerecho != null) {
                    if (actualIzquierdo == null && posicionIzquierda < finIzquierdo) {
                        actualIzquierdo = entradaIzquierda.leerRegistro(posicionIzquierda++);
                    }
                    if (actualDerecho == null && posicionDerecha < finDerecho) {
                        actualDerecho = entradaDerecha.leerRegistro(posicionDerecha++);
                    }
                    if (actualDerecho == null || (actualIzquierdo != null
                            && actualIzquierdo.obtenerNombre().compareTo(actualDerecho.obtenerNombre()) <= 0)) {
                        salida.agregarRegistro(actualIzquierdo);
                        actualIzquierdo = null;
                    } else {
                        salida.agregarRegistro(actualDerecho);
                        actualDerecho = null;
                    }
                }
            }
        }
    }

    private static void vaciar(Path ruta) throws IOException {
        try (java.io.OutputStream salida = Files.newOutputStream(ruta)) {
            // Se vacian exclusivamente archivos de la carpeta temporal propia.
        }
    }

    private static final class Temporales implements AutoCloseable {
        private final Path carpeta;
        private final Path izquierdo;
        private final Path derecho;
        private final Path resultadoUno;
        private final Path resultadoDos;

        private Temporales(Path directorio) throws IOException {
            carpeta = Files.createTempDirectory(directorio, ".orden-indice-");
            izquierdo = carpeta.resolve("izquierdo");
            derecho = carpeta.resolve("derecho");
            resultadoUno = carpeta.resolve("resultado-uno");
            resultadoDos = carpeta.resolve("resultado-dos");
        }

        @Override
        public void close() throws IOException {
            IOException error = null;
            error = eliminar(izquierdo, error);
            error = eliminar(derecho, error);
            error = eliminar(resultadoUno, error);
            error = eliminar(resultadoDos, error);
            error = eliminar(carpeta, error);
            if (error != null) {
                throw error;
            }
        }

        private static IOException eliminar(Path ruta, IOException anterior) {
            try {
                Files.deleteIfExists(ruta);
            } catch (IOException error) {
                if (anterior == null) {
                    return error;
                }
                anterior.addSuppressed(error);
            }
            return anterior;
        }
    }
}