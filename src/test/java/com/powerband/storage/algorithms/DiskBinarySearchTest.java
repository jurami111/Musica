package com.powerband.storage.algorithms;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.powerband.common.exceptions.ArchivoCorruptoException;
import com.powerband.common.exceptions.StorageIOException;
import com.powerband.common.model.IndexEntry;
import com.powerband.storage.io.IndexFileManager;
import com.powerband.storage.io.MusicFileManager;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class DiskBinarySearchTest {
    @TempDir
    Path carpeta;

    @Test
    void buscaEnVacioYUnRegistro() throws Exception {
        Path vacio = carpeta.resolve("vacio.index");
        Path uno = carpeta.resolve("uno.index");
        try (IndexFileManager indice = new IndexFileManager(vacio);
                IndexFileManager indiceUno = new IndexFileManager(uno)) {
            indiceUno.agregarRegistro(new IndexEntry("Único", 0));
        }
        DiskBinarySearch busqueda = new DiskBinarySearch();
        assertTrue(busqueda.buscar(vacio, "Cualquier").isEmpty());
        assertFalse(busqueda.buscar(uno, "Cualquier").isPresent());
        assertEquals(new IndexEntry("Único", 0).obtenerNombre(),
                busqueda.buscar(uno, "Único").orElseThrow().obtenerNombre());
        assertEquals(0, busqueda.buscar(uno, "Único").orElseThrow().obtenerPosicionEncabezado());
    }

    @Test
    void buscaPrimeroMedioYUltimoConCantidadImpar() throws Exception {
        Path ruta = crearIndice(new String[] {"Alfa", "Bravo", "Charlie", "Delta", "Echo"});
        DiskBinarySearch busqueda = new DiskBinarySearch();
        comprobar(busqueda.buscar(ruta, "Alfa").orElseThrow(), "Alfa", 0);
        comprobar(busqueda.buscar(ruta, "Charlie").orElseThrow(), "Charlie", 512);
        comprobar(busqueda.buscar(ruta, "Echo").orElseThrow(), "Echo", 1024);
    }

    @Test
    void buscaConCantidadParYNoConfundeLosLimites() throws Exception {
        Path ruta = crearIndice(new String[] {"Bravo", "Delta", "Foxtrot", "Hotel"});
        DiskBinarySearch busqueda = new DiskBinarySearch();
        comprobar(busqueda.buscar(ruta, "Bravo").orElseThrow(), "Bravo", 0);
        comprobar(busqueda.buscar(ruta, "Delta").orElseThrow(), "Delta", 256);
        comprobar(busqueda.buscar(ruta, "Foxtrot").orElseThrow(), "Foxtrot", 512);
        comprobar(busqueda.buscar(ruta, "Hotel").orElseThrow(), "Hotel", 768);
        assertTrue(busqueda.buscar(ruta, "Alfa").isEmpty());
        assertTrue(busqueda.buscar(ruta, "Zulu").isEmpty());
        assertTrue(busqueda.buscar(ruta, "Echo").isEmpty());
    }

    @Test
    void respetaComparacionExactaDeMayusculasEspaciosYTildes() throws Exception {
        Path ruta = crearIndice(new String[] {" Canción", "Canción", "canción"});
        DiskBinarySearch busqueda = new DiskBinarySearch();
        comprobar(busqueda.buscar(ruta, " Canción").orElseThrow(), " Canción", 0);
        comprobar(busqueda.buscar(ruta, "Canción").orElseThrow(), "Canción", 256);
        comprobar(busqueda.buscar(ruta, "canción").orElseThrow(), "canción", 512);
        assertTrue(busqueda.buscar(ruta, "CANCION").isEmpty());
        assertTrue(busqueda.buscar(ruta, "Canción ").isEmpty());
    }

    @Test
    void offsetEncontradoAbreElEncabezadoCorrespondiente() throws Exception {
        Path musica = carpeta.resolve("recordings.music");
        Path indice = carpeta.resolve("recordings.index");
        long posicion;
        try (MusicFileManager gestor = new MusicFileManager(musica);
                IndexFileManager entradas = new IndexFileManager(indice)) {
            gestor.reservarEncabezado();
            gestor.finalizarGrabacion("Primera");
            posicion = gestor.reservarEncabezado();
            gestor.finalizarGrabacion("Canción elegida");
            entradas.agregarRegistro(new IndexEntry("Canción elegida", posicion));
        }
        IndexEntry resultado = new DiskBinarySearch().buscar(indice, "Canción elegida").orElseThrow();
        try (MusicFileManager gestor = new MusicFileManager(musica)) {
            assertEquals("Canción elegida",
                    gestor.leerEncabezado(resultado.obtenerPosicionEncabezado()).obtenerNombre());
        }
    }

    @Test
    void consultaSoloLeeElCaminoBinarioYNoModificaElArchivo() throws Exception {
        Path ruta = carpeta.resolve("muchos.index");
        try (IndexFileManager indice = new IndexFileManager(ruta)) {
            for (int i = 0; i < 1023; i++) {
                indice.agregarRegistro(new IndexEntry(String.format("Clave%04d", i), i * 256L));
            }
        }
        byte[] antes = Files.readAllBytes(ruta);
        DiskBinarySearch busqueda = new DiskBinarySearch();
        comprobar(busqueda.buscar(ruta, "Clave0999").orElseThrow(), "Clave0999", 999L * 256);
        assertTrue(busqueda.buscar(ruta, "Clave2000").isEmpty());
        assertArrayEquals(antes, Files.readAllBytes(ruta));
    }

    @Test
    void validaArgumentosYRutasSinConvertirErroresEnAusencia() throws Exception {
        Path ruta = crearIndice(new String[] {"A", "B"});
        DiskBinarySearch busqueda = new DiskBinarySearch();
        for (String nombre : new String[] {null, "", " ", "a\0b", "a".repeat(51)}) {
            assertThrows(IllegalArgumentException.class, () -> busqueda.buscar(ruta, nombre));
        }
        Path inexistente = carpeta.resolve("inexistente.index");
        assertThrows(StorageIOException.class, () -> busqueda.buscar(inexistente, "A"));
        assertThrows(StorageIOException.class, () -> busqueda.buscar(carpeta, "A"));
    }

    @Test
    void detectaTamanoIncompletoYRegistroCorruptoSinModificarElIndice() throws Exception {
        Path ruta = crearIndice(new String[] {"Alfa", "Bravo", "Charlie", "Delta", "Echo"});
        DiskBinarySearch busqueda = new DiskBinarySearch();
        try (RandomAccessFile archivo = new RandomAccessFile(ruta.toFile(), "rw")) {
            archivo.setLength(5 * 256 - 1);
        }
        assertThrows(ArchivoCorruptoException.class, () -> busqueda.buscar(ruta, "Charlie"));
        try (RandomAccessFile archivo = new RandomAccessFile(ruta.toFile(), "rw")) {
            archivo.setLength(5 * 256);
            archivo.seek(2 * 256 + 108);
            archivo.writeByte(1);
        }
        byte[] antes = Files.readAllBytes(ruta);
        assertThrows(ArchivoCorruptoException.class, () -> busqueda.buscar(ruta, "Charlie"));
        assertArrayEquals(antes, Files.readAllBytes(ruta));
    }

    private Path crearIndice(String[] nombres) throws Exception {
        Path ruta = carpeta.resolve("prueba-" + System.nanoTime() + ".index");
        try (IndexFileManager indice = new IndexFileManager(ruta)) {
            for (int i = 0; i < nombres.length; i++) {
                indice.agregarRegistro(new IndexEntry(nombres[i], i * 256L));
            }
        }
        new BalancedMergeSort().ordenar(ruta);
        return ruta;
    }

    private static void comprobar(IndexEntry entrada, String nombre, long posicion) {
        assertEquals(nombre, entrada.obtenerNombre());
        assertEquals(posicion, entrada.obtenerPosicionEncabezado());
    }
}
