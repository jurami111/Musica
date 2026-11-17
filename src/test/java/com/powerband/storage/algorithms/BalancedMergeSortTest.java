package com.powerband.storage.algorithms;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.powerband.common.exceptions.DiskSortException;
import com.powerband.common.model.IndexEntry;
import com.powerband.storage.io.IndexFileManager;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class BalancedMergeSortTest {
    @TempDir
    Path carpeta;

    @Test
    void ordenaVariasPasadasConDuplicadosYConservaCadaPosicion() throws Exception {
        Path ruta = carpeta.resolve("recordings.index");
        String[] nombres = {"Zeta", "Alfa", "ñandu", "Alfa", "Árbol", "Beta", "alfa",
                "Zeta", "Beta", "Casa", "Alfa", "Árbol", "Otro"};
        try (IndexFileManager indice = new IndexFileManager(ruta)) {
            for (int i = 0; i < nombres.length; i++) {
                indice.agregarRegistro(new IndexEntry(nombres[i], i * 256L));
            }
        }

        new BalancedMergeSort().ordenar(ruta);

        String[] esperado = {"Alfa", "Alfa", "Alfa", "Beta", "Beta", "Casa", "Otro",
                "Zeta", "Zeta", "alfa", "Árbol", "Árbol", "ñandu"};
        long[] posiciones = {256, 768, 2560, 1280, 2048, 2304, 3072,
                0, 1792, 1536, 1024, 2816, 512};
        try (IndexFileManager indice = new IndexFileManager(ruta)) {
            assertEquals(esperado.length, indice.obtenerCantidad());
            for (int i = 0; i < esperado.length; i++) {
                IndexEntry entrada = indice.leerRegistro(i);
                assertEquals(esperado[i], entrada.obtenerNombre());
                assertEquals(posiciones[i], entrada.obtenerPosicionEncabezado());
            }
        }
        assertEquals(13 * 256, Files.size(ruta));
        comprobarSinTemporales();
    }

    @Test
    void aceptaArchivoVacioYUnoConUnRegistroSinCambiarSusBytes() throws Exception {
        Path vacio = carpeta.resolve("vacio.index");
        Path uno = carpeta.resolve("uno.index");
        try (IndexFileManager indiceVacio = new IndexFileManager(vacio);
                IndexFileManager indiceUno = new IndexFileManager(uno)) {
            indiceUno.agregarRegistro(new IndexEntry("Único", 0));
        }
        byte[] bytesVacio = Files.readAllBytes(vacio);
        byte[] bytesUno = Files.readAllBytes(uno);
        BalancedMergeSort orden = new BalancedMergeSort();
        orden.ordenar(vacio);
        orden.ordenar(uno);
        assertArrayEquals(bytesVacio, Files.readAllBytes(vacio));
        assertArrayEquals(bytesUno, Files.readAllBytes(uno));
        comprobarSinTemporales();
    }

    @Test
    void ordenaInversoYNoAlteraIndiceYaOrdenado() throws Exception {
        Path ruta = carpeta.resolve("recordings.index");
        try (IndexFileManager indice = new IndexFileManager(ruta)) {
            for (int i = 16; i >= 0; i--) {
                indice.agregarRegistro(new IndexEntry(String.format("Nombre%02d", i), i * 256L));
            }
        }
        BalancedMergeSort orden = new BalancedMergeSort();
        orden.ordenar(ruta);
        try (IndexFileManager indice = new IndexFileManager(ruta)) {
            for (int i = 0; i <= 16; i++) {
                IndexEntry entrada = indice.leerRegistro(i);
                assertEquals(String.format("Nombre%02d", i), entrada.obtenerNombre());
                assertEquals(i * 256L, entrada.obtenerPosicionEncabezado());
            }
        }
        byte[] ordenado = Files.readAllBytes(ruta);
        orden.ordenar(ruta);
        assertArrayEquals(ordenado, Files.readAllBytes(ruta));
        comprobarSinTemporales();
    }

    @Test
    void reportaCorrupcionSinAlterarElOriginalNiDejarTemporales() throws Exception {
        Path ruta = carpeta.resolve("recordings.index");
        try (IndexFileManager indice = new IndexFileManager(ruta)) {
            indice.agregarRegistro(new IndexEntry("B", 0));
            indice.agregarRegistro(new IndexEntry("A", 256));
        }
        try (RandomAccessFile archivo = new RandomAccessFile(ruta.toFile(), "rw")) {
            archivo.seek(256 + 108);
            archivo.writeByte(1);
        }
        byte[] antes = Files.readAllBytes(ruta);
        DiskSortException error = assertThrows(DiskSortException.class,
                () -> new BalancedMergeSort().ordenar(ruta));
        assertTrue(error.getCause() instanceof com.powerband.common.exceptions.ArchivoCorruptoException);
        assertArrayEquals(antes, Files.readAllBytes(ruta));
        comprobarSinTemporales();
    }

    @Test
    void rechazaArchivoIncompletoYRegistroUnicoCorrupto() throws Exception {
        Path ruta = carpeta.resolve("recordings.index");
        try (IndexFileManager indice = new IndexFileManager(ruta)) {
            indice.agregarRegistro(new IndexEntry("A", 0));
        }
        try (RandomAccessFile archivo = new RandomAccessFile(ruta.toFile(), "rw")) {
            archivo.seek(108);
            archivo.writeByte(1);
        }
        byte[] corrupto = Files.readAllBytes(ruta);
        assertThrows(DiskSortException.class, () -> new BalancedMergeSort().ordenar(ruta));
        assertArrayEquals(corrupto, Files.readAllBytes(ruta));
        try (RandomAccessFile archivo = new RandomAccessFile(ruta.toFile(), "rw")) {
            archivo.setLength(255);
        }
        byte[] truncado = Files.readAllBytes(ruta);
        assertThrows(DiskSortException.class, () -> new BalancedMergeSort().ordenar(ruta));
        assertArrayEquals(truncado, Files.readAllBytes(ruta));
        comprobarSinTemporales();
    }

    @Test
    void noCreaArchivoCuandoLaRutaNoExisteYRechazaDirectorios() throws Exception {
        Path inexistente = carpeta.resolve("inexistente.index");
        assertThrows(DiskSortException.class, () -> new BalancedMergeSort().ordenar(inexistente));
        assertTrue(Files.notExists(inexistente));
        assertThrows(DiskSortException.class, () -> new BalancedMergeSort().ordenar(carpeta));
        comprobarSinTemporales();
    }

    private void comprobarSinTemporales() throws Exception {
        try (Stream<Path> contenido = Files.list(carpeta)) {
            assertEquals(0, contenido
                    .filter(ruta -> ruta.getFileName().toString().startsWith(".orden-indice-"))
                    .count());
        }
    }
}
