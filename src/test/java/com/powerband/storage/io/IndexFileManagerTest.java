package com.powerband.storage.io;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.powerband.common.exceptions.ArchivoCorruptoException;
import com.powerband.common.exceptions.StorageIOException;
import com.powerband.common.model.IndexEntry;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class IndexFileManagerTest {
    @TempDir
    Path carpeta;

    @Test
    void escribeNombrePosicionYRellenoEn256Bytes() throws Exception {
        Path ruta = carpeta.resolve("datos").resolve("recordings.index");
        String nombre = "Canción ñ";
        long posicion = 256L * 12_000_000;
        try (IndexFileManager gestor = new IndexFileManager(ruta)) {
            gestor.agregarRegistro(new IndexEntry(nombre, posicion));
            assertEquals(256, Files.size(ruta));
        }
        byte[] datos = Files.readAllBytes(ruta);
        assertArrayEquals(new byte[148], Arrays.copyOfRange(datos, 108, 256));
        try (DataInputStream entrada = new DataInputStream(Files.newInputStream(ruta))) {
            for (int i = 0; i < 50; i++) {
                assertEquals(i < nombre.length() ? nombre.charAt(i) : 0, entrada.readChar());
            }
            assertEquals(posicion, entrada.readLong());
            assertArrayEquals(new byte[148], entrada.readNBytes(148));
            assertEquals(-1, entrada.read());
        }
    }

    @Test
    void agregaYRecuperaRegistrosAlReabrirSinOrdenarlos() throws Exception {
        Path ruta = carpeta.resolve("recordings.index");
        String nombreLargo = "ñ".repeat(50);
        try (IndexFileManager gestor = new IndexFileManager(ruta)) {
            assertEquals(0, gestor.obtenerCantidad());
            gestor.agregarRegistro(new IndexEntry("Zeta", 0));
            gestor.agregarRegistro(new IndexEntry(nombreLargo, 512));
        }
        try (IndexFileManager gestor = new IndexFileManager(ruta)) {
            gestor.agregarRegistro(new IndexEntry("Alfa", 256));
            assertEquals(3, gestor.obtenerCantidad());
            assertEquals("Zeta", gestor.leerRegistro(0).obtenerNombre());
            assertEquals(0, gestor.leerRegistro(0).obtenerPosicionEncabezado());
            assertEquals(nombreLargo, gestor.leerRegistro(1).obtenerNombre());
            assertEquals(512, gestor.leerRegistro(1).obtenerPosicionEncabezado());
            assertEquals("Alfa", gestor.leerRegistro(2).obtenerNombre());
            assertEquals(256, gestor.leerRegistro(2).obtenerPosicionEncabezado());
            assertEquals(768, Files.size(ruta));
        }
    }

    @Test
    void recuperaUnEncabezadoRealUsandoLaPosicionGuardada() throws Exception {
        Path musica = carpeta.resolve("recordings.music");
        Path indice = carpeta.resolve("recordings.index");
        try (MusicFileManager gestorMusica = new MusicFileManager(musica);
                IndexFileManager gestorIndice = new IndexFileManager(indice)) {
            gestorMusica.reservarEncabezado();
            gestorMusica.finalizarGrabacion("Primera");
            long posicion = gestorMusica.reservarEncabezado();
            gestorMusica.finalizarGrabacion("Segunda");
            gestorIndice.agregarRegistro(new IndexEntry("Segunda", posicion));
            IndexEntry entrada = gestorIndice.leerRegistro(0);
            assertEquals(256, entrada.obtenerPosicionEncabezado());
            assertEquals(entrada.obtenerNombre(),
                    gestorMusica.leerEncabezado(entrada.obtenerPosicionEncabezado()).obtenerNombre());
        }
    }

    @Test
    void rechazaDatosInvalidosYLimitesFueraDelIndice() throws Exception {
        for (String nombre : new String[] {null, "", " ", "a\0b", "a".repeat(51)}) {
            assertThrows(IllegalArgumentException.class, () -> new IndexEntry(nombre, 0));
        }
        assertThrows(IllegalArgumentException.class, () -> new IndexEntry("A", -256));
        assertThrows(IllegalArgumentException.class, () -> new IndexEntry("A", 257));
        Path ruta = carpeta.resolve("recordings.index");
        try (IndexFileManager gestor = new IndexFileManager(ruta)) {
            assertThrows(IllegalArgumentException.class, () -> gestor.leerRegistro(0));
            gestor.agregarRegistro(new IndexEntry("A", 0));
            assertThrows(IllegalArgumentException.class, () -> gestor.leerRegistro(-1));
            assertThrows(IllegalArgumentException.class, () -> gestor.leerRegistro(1));
            assertThrows(IllegalArgumentException.class, () -> gestor.leerRegistro(Long.MAX_VALUE));
            assertEquals(256, Files.size(ruta));
        }
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 4, 100, 107, 108, 255})
    void detectaNombresPosicionesYRellenosCorruptos(int posicion) throws Exception {
        Path ruta = crearIndice();
        try (RandomAccessFile archivo = new RandomAccessFile(ruta.toFile(), "rw")) {
            archivo.seek(posicion);
            archivo.writeByte(posicion == 1 ? 0 : posicion == 100 ? 255 : 127);
        }
        try (IndexFileManager gestor = new IndexFileManager(ruta)) {
            assertThrows(ArchivoCorruptoException.class, () -> gestor.leerRegistro(0));
        }
    }

    @Test
    void detectaRegistroTruncadoDuranteLecturaYAlAbrir() throws Exception {
        Path ruta = crearIndice();
        try (IndexFileManager gestor = new IndexFileManager(ruta)) {
            try (RandomAccessFile archivo = new RandomAccessFile(ruta.toFile(), "rw")) {
                archivo.setLength(255);
            }
            assertThrows(ArchivoCorruptoException.class, () -> gestor.leerRegistro(0));
            assertThrows(ArchivoCorruptoException.class,
                    () -> gestor.agregarRegistro(new IndexEntry("Otra", 0)));
        }
        assertThrows(ArchivoCorruptoException.class, () -> new IndexFileManager(ruta));
    }

    @Test
    void informaErrorDeAperturaConSuCausa() throws Exception {
        Path archivo = carpeta.resolve("no-es-carpeta");
        Files.writeString(archivo, "contenido");
        StorageIOException error = assertThrows(StorageIOException.class,
                () -> new IndexFileManager(archivo.resolve("recordings.index")));
        assertInstanceOf(IOException.class, error.getCause());
    }

    private Path crearIndice() throws Exception {
        Path ruta = carpeta.resolve("recordings.index");
        try (IndexFileManager gestor = new IndexFileManager(ruta)) {
            gestor.agregarRegistro(new IndexEntry("A", 0));
        }
        return ruta;
    }
}
