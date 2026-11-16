package com.powerband.storage.io;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.powerband.common.exceptions.ArchivoCorruptoException;
import com.powerband.common.exceptions.DuplicateNameException;
import com.powerband.common.exceptions.StorageIOException;
import com.powerband.common.model.RecordingHeader;
import com.powerband.common.model.RecordingMetadata;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class ListFileManagerTest {
    @TempDir
    Path carpeta;

    @Test
    void guardaSoloNombreYFechaEn108Bytes() throws Exception {
        Path ruta = carpeta.resolve("datos").resolve("recordings.list");
        Instant fecha = Instant.parse("2026-11-15T21:03:04.123456789Z");
        String nombre = "Canción ñ";
        try (ListFileManager gestor = new ListFileManager(ruta)) {
            gestor.registrar(new RecordingMetadata(nombre, fecha));
            assertEquals(108, Files.size(ruta));
            assertEquals(Instant.parse("2026-11-15T21:03:04.123Z"),
                    gestor.leerRegistro(0).obtenerFechaHora());
        }
        try (DataInputStream entrada = new DataInputStream(Files.newInputStream(ruta))) {
            for (int i = 0; i < 50; i++) {
                assertEquals(i < nombre.length() ? nombre.charAt(i) : 0, entrada.readChar());
            }
            assertEquals(fecha.toEpochMilli(), entrada.readLong());
            assertEquals(-1, entrada.read());
        }
    }

    @Test
    void mantieneOrdenDeRegistroAlReabrirAunqueNombreYFechaNoEstenOrdenados() throws Exception {
        Path ruta = carpeta.resolve("recordings.list");
        Instant primera = Instant.parse("2026-11-15T21:00:00Z");
        Instant segunda = primera.minusSeconds(60);
        try (ListFileManager gestor = new ListFileManager(ruta)) {
            assertEquals(0, gestor.obtenerCantidad());
            assertFalse(gestor.existeNombre("Zeta"));
            gestor.registrar(new RecordingMetadata("Zeta", primera));
            gestor.registrar(new RecordingMetadata("Alfa", segunda));
        }
        try (ListFileManager gestor = new ListFileManager(ruta)) {
            gestor.registrar(new RecordingMetadata("Media", primera.plusSeconds(60)));
            assertEquals(3, gestor.obtenerCantidad());
            assertEquals("Zeta", gestor.leerRegistro(0).obtenerNombre());
            assertEquals(primera, gestor.leerRegistro(0).obtenerFechaHora());
            assertEquals("Alfa", gestor.leerRegistro(1).obtenerNombre());
            assertEquals(segunda, gestor.leerRegistro(1).obtenerFechaHora());
            assertEquals("Media", gestor.leerRegistro(2).obtenerNombre());
            assertEquals(324, Files.size(ruta));
        }
    }

    @Test
    void rechazaDuplicadoPersistidoSinCambiarLaListaYPermiteOtroNombre() throws Exception {
        Path ruta = carpeta.resolve("recordings.list");
        try (ListFileManager gestor = new ListFileManager(ruta)) {
            gestor.registrar("Original");
        }
        byte[] antes = Files.readAllBytes(ruta);
        try (ListFileManager gestor = new ListFileManager(ruta)) {
            assertTrue(gestor.existeNombre("Original"));
            assertThrows(DuplicateNameException.class, () -> gestor.registrar("Original"));
            assertArrayEquals(antes, Files.readAllBytes(ruta));
            assertFalse(gestor.existeNombre("Otra"));
            gestor.registrar("Otra");
            assertEquals(2, gestor.obtenerCantidad());
        }
    }

    @Test
    void comparaExactamenteMayusculasEspaciosYAcentosSinNormalizar() throws Exception {
        String[] nombres = {"Canción", "canción", " Canción", "Canción ", "Cancion"};
        try (ListFileManager gestor = new ListFileManager(carpeta.resolve("recordings.list"))) {
            for (String nombre : nombres) {
                assertFalse(gestor.existeNombre(nombre));
                gestor.registrar(nombre);
            }
            for (int i = 0; i < nombres.length; i++) {
                String nombre = nombres[i];
                assertEquals(nombre, gestor.leerRegistro(i).obtenerNombre());
                assertTrue(gestor.existeNombre(nombre));
                assertThrows(DuplicateNameException.class, () -> gestor.registrar(nombre));
            }
        }
    }

    @Test
    void validaLosMismosNombresQueMusicSinEscribirYAdmiteElLimite() throws Exception {
        Path ruta = carpeta.resolve("recordings.list");
        Instant fecha = Instant.parse("2026-11-15T21:00:00Z");
        try (ListFileManager gestor = new ListFileManager(ruta)) {
            for (String nombre : new String[] {null, "", "  ", "a\0b", "a".repeat(51)}) {
                assertThrows(IllegalArgumentException.class, () -> gestor.registrar(nombre));
                assertThrows(IllegalArgumentException.class, () -> new RecordingHeader(nombre, 256, 256));
                assertEquals(0, Files.size(ruta));
            }
            String nombre = "ñ".repeat(50);
            gestor.registrar(new RecordingMetadata(nombre, fecha));
            assertEquals(nombre, gestor.leerRegistro(0).obtenerNombre());
            assertTrue(gestor.existeNombre(nombre));
        }
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 4})
    void detectaNombreVacioORellenoCorruptoSinAnexarRegistros(int posicion) throws Exception {
        Path ruta = crearLista();
        try (RandomAccessFile archivo = new RandomAccessFile(ruta.toFile(), "rw")) {
            archivo.seek(posicion);
            archivo.writeChar(posicion == 0 ? '\0' : 'X');
        }
        try (ListFileManager gestor = new ListFileManager(ruta)) {
            assertThrows(ArchivoCorruptoException.class, () -> gestor.leerRegistro(0));
            assertThrows(ArchivoCorruptoException.class, () -> gestor.existeNombre("Otra"));
            assertThrows(ArchivoCorruptoException.class, () -> gestor.registrar("Otra"));
            assertEquals(108, Files.size(ruta));
        }
    }

    @Test
    void detectaTruncamientoDuranteLecturaYAlAbrir() throws Exception {
        Path ruta = crearLista();
        try (ListFileManager gestor = new ListFileManager(ruta)) {
            try (RandomAccessFile archivo = new RandomAccessFile(ruta.toFile(), "rw")) {
                archivo.setLength(107);
            }
            assertThrows(ArchivoCorruptoException.class, gestor::obtenerCantidad);
            assertThrows(ArchivoCorruptoException.class, () -> gestor.registrar("Otra"));
        }
        assertThrows(ArchivoCorruptoException.class, () -> new ListFileManager(ruta));
    }

    @Test
    void fallaExplicitamenteSiLaRutaNoPermiteCrearElArchivo() throws Exception {
        Path archivo = carpeta.resolve("no-es-carpeta");
        Files.writeString(archivo, "contenido");
        StorageIOException error = assertThrows(StorageIOException.class,
                () -> new ListFileManager(archivo.resolve("recordings.list")));
        assertInstanceOf(IOException.class, error.getCause());
    }

    private Path crearLista() throws Exception {
        Path ruta = carpeta.resolve("recordings.list");
        try (ListFileManager gestor = new ListFileManager(ruta)) {
            gestor.registrar("A");
        }
        return ruta;
    }
}
