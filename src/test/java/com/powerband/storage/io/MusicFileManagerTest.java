package com.powerband.storage.io;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.powerband.common.constants.BinaryConstants;
import com.powerband.common.constants.SynthConstants;
import com.powerband.common.exceptions.ArchivoCorruptoException;
import com.powerband.common.exceptions.StorageIOException;
import com.powerband.common.model.RecordingHeader;
import com.powerband.common.model.SynthCommand;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class MusicFileManagerTest {
    @TempDir
    Path carpeta;

    @Test
    void escribeBytesExactosYFinalizaSinModificarComandos() throws Exception {
        Path ruta = carpeta.resolve("datos").resolve("recordings.music");
        try (MusicFileManager gestor = new MusicFileManager(ruta)) {
            assertEquals(0, gestor.reservarEncabezado());
            assertEquals(256, Files.size(ruta));
            assertEquals(256, gestor.escribirComando(bajo(1234567890123L)));
            byte[] antes = Files.readAllBytes(ruta);
            assertEquals(0, gestor.finalizarGrabacion("Cancion con ñ"));
            byte[] despues = Files.readAllBytes(ruta);
            assertEquals(512, despues.length);
            assertArrayEquals(Arrays.copyOfRange(antes, 256, 512),
                    Arrays.copyOfRange(despues, 256, 512));
            assertArrayEquals(new byte[136], Arrays.copyOfRange(despues, 120, 256));
            assertArrayEquals(new byte[104], Arrays.copyOfRange(despues, 408, 512));
            assertArrayEquals(new byte[112], Arrays.copyOfRange(despues, 296, 408));
        }
        try (DataInputStream entrada = new DataInputStream(Files.newInputStream(ruta))) {
            assertEquals(BinaryConstants.TIPO_ENCABEZADO, entrada.readInt());
            String nombre = "Cancion con ñ";
            for (int i = 0; i < 50; i++) {
                assertEquals(i < nombre.length() ? nombre.charAt(i) : 0, entrada.readChar());
            }
            assertEquals(256, entrada.readLong());
            assertEquals(512, entrada.readLong());
            entrada.skipNBytes(136);
            assertEquals(BinaryConstants.TIPO_COMANDO, entrada.readInt());
            assertEquals(1234567890123L, entrada.readLong());
            assertEquals(SynthConstants.IDENTIFICADOR_BAJO, entrada.readInt());
            assertEquals(BinaryConstants.ACCION_TOCAR, entrada.readInt());
            assertEquals(2, entrada.readInt());
            assertEquals(36.0, entrada.readDouble());
            assertEquals(450.0, entrada.readDouble());
        }
    }

    @Test
    void recuperaTodosLosComandosYVariasGrabacionesAlReabrir() throws Exception {
        Path ruta = carpeta.resolve("recordings.music");
        List<SynthCommand> comandos = List.of(
                new SynthCommand(0, SynthConstants.IDENTIFICADOR_PIANO,
                        BinaryConstants.ACCION_TOCAR, 60, 1500, 3),
                bajo(0),
                new SynthCommand(10, SynthConstants.IDENTIFICADOR_ORGANO,
                        BinaryConstants.ACCION_INICIAR_NOTA, 64, 1, 2.01, 4, 1, 0.6, 0.3),
                new SynthCommand(Long.MAX_VALUE, SynthConstants.IDENTIFICADOR_ORGANO,
                        BinaryConstants.ACCION_TERMINAR_NOTA, 64));
        String nombre = "Á".repeat(50);
        try (MusicFileManager gestor = new MusicFileManager(ruta)) {
            gestor.reservarEncabezado();
            for (SynthCommand comando : comandos) {
                gestor.escribirComando(comando);
            }
            gestor.finalizarGrabacion(nombre);
            assertEquals(1280, gestor.reservarEncabezado());
            assertEquals(1280, gestor.finalizarGrabacion("Vacia"));
        }
        try (MusicFileManager gestor = new MusicFileManager(ruta)) {
            RecordingHeader primero = gestor.leerEncabezado(0);
            assertEquals(nombre, primero.obtenerNombre());
            assertEquals(256, primero.obtenerInicio());
            assertEquals(1280, primero.obtenerFin());
            for (int i = 0; i < comandos.size(); i++) {
                SynthCommand esperado = comandos.get(i);
                SynthCommand leido = gestor.leerComando((i + 1L) * 256);
                assertEquals(esperado.obtenerTiempo(), leido.obtenerTiempo());
                assertEquals(esperado.obtenerSintetizador(), leido.obtenerSintetizador());
                assertEquals(esperado.obtenerAccion(), leido.obtenerAccion());
                assertArrayEquals(esperado.obtenerParametros(), leido.obtenerParametros());
            }
            RecordingHeader segundo = gestor.leerEncabezado(1280);
            assertEquals("Vacia", segundo.obtenerNombre());
            assertEquals(1536, segundo.obtenerInicio());
            assertEquals(1536, segundo.obtenerFin());
            assertEquals(1536, gestor.obtenerTamano());
        }
    }

    @Test
    void entradasInvalidasNoEscribenNiPierdenLaReserva() throws Exception {
        try (MusicFileManager gestor = new MusicFileManager(carpeta.resolve("recordings.music"))) {
            assertThrows(IllegalStateException.class, () -> gestor.escribirComando(bajo(0)));
            gestor.reservarEncabezado();
            gestor.escribirComando(bajo(20));
            assertThrows(IllegalArgumentException.class, () -> gestor.escribirComando(bajo(19)));
            for (String nombre : List.of("", "   ", "a\0b", "a".repeat(51))) {
                assertThrows(IllegalArgumentException.class, () -> gestor.finalizarGrabacion(nombre));
                assertEquals(512, gestor.obtenerTamano());
            }
            gestor.finalizarGrabacion("Valida");
            assertEquals("Valida", gestor.leerEncabezado(0).obtenerNombre());
        }
    }

    @Test
    void validaContratoDeParametrosYProtegeSusDatos() {
        double[] parametros = {36, 450};
        SynthCommand comando = new SynthCommand(0, SynthConstants.IDENTIFICADOR_BAJO,
                BinaryConstants.ACCION_TOCAR, parametros);
        parametros[0] = 90;
        comando.obtenerParametros()[0] = 80;
        assertArrayEquals(new double[] {36, 450}, comando.obtenerParametros());
        for (double[] invalidos : List.of(new double[17], new double[] {36},
                new double[] {128, 450}, new double[] {36.5, 450},
                new double[] {36, 0}, new double[] {36, 1.5},
                new double[] {36, Double.NaN}, new double[] {36, Double.POSITIVE_INFINITY})) {
            assertThrows(IllegalArgumentException.class, () -> new SynthCommand(0,
                    SynthConstants.IDENTIFICADOR_BAJO, BinaryConstants.ACCION_TOCAR, invalidos));
        }
        assertThrows(IllegalArgumentException.class, () -> bajo(-1));
        assertThrows(IllegalArgumentException.class, () -> new SynthCommand(0,
                SynthConstants.IDENTIFICADOR_PIANO, BinaryConstants.ACCION_TOCAR, 60, 500, 5));
        assertThrows(IllegalArgumentException.class, () -> new SynthCommand(0,
                SynthConstants.IDENTIFICADOR_BAJO, BinaryConstants.ACCION_TERMINAR_NOTA, 36));
    }

    @Test
    void detectaReservaInconclusaTiposYPosicionesIncorrectas() throws Exception {
        Path ruta = carpeta.resolve("recordings.music");
        try (MusicFileManager gestor = new MusicFileManager(ruta)) {
            gestor.reservarEncabezado();
            gestor.escribirComando(bajo(0));
            assertThrows(ArchivoCorruptoException.class, () -> gestor.leerEncabezado(0));
            assertThrows(ArchivoCorruptoException.class, () -> gestor.leerComando(0));
            assertThrows(ArchivoCorruptoException.class, () -> gestor.leerEncabezado(256));
            for (long posicion : new long[] {-256, 1, 512, Long.MAX_VALUE}) {
                assertThrows(ArchivoCorruptoException.class, () -> gestor.leerComando(posicion));
            }
        }
        try (MusicFileManager gestor = new MusicFileManager(ruta)) {
            assertThrows(ArchivoCorruptoException.class, () -> gestor.leerEncabezado(0));
        }
    }

    @ParameterizedTest
    @ValueSource(longs = {0, 8, 104, 112, 120})
    void detectaEncabezadosCorruptos(long posicion) throws Exception {
        Path ruta = crearGrabacion();
        try (RandomAccessFile archivo = new RandomAccessFile(ruta.toFile(), "rw")) {
            archivo.seek(posicion);
            // Modifica tipo, nombre tras su relleno, limites o relleno del encabezado.
            archivo.writeByte(127);
        }
        try (MusicFileManager gestor = new MusicFileManager(ruta)) {
            assertThrows(ArchivoCorruptoException.class, () -> gestor.leerEncabezado(0));
        }
    }

    @ParameterizedTest
    @ValueSource(longs = {256, 260, 268, 272, 276, 280, 296, 408})
    void detectaComandosCorruptos(long posicion) throws Exception {
        Path ruta = crearGrabacion();
        try (RandomAccessFile archivo = new RandomAccessFile(ruta.toFile(), "rw")) {
            archivo.seek(posicion);
            // Incluye campos, parametros no utilizados y relleno.
            archivo.writeLong(-1);
        }
        try (MusicFileManager gestor = new MusicFileManager(ruta)) {
            assertThrows(ArchivoCorruptoException.class, () -> gestor.leerComando(256));
        }
    }

    @Test
    void detectaArchivoTruncadoAlAbrirYDuranteLectura() throws Exception {
        Path ruta = crearGrabacion();
        try (MusicFileManager gestor = new MusicFileManager(ruta)) {
            try (RandomAccessFile archivo = new RandomAccessFile(ruta.toFile(), "rw")) {
                archivo.setLength(511);
            }
            assertThrows(ArchivoCorruptoException.class, () -> gestor.leerComando(256));
        }
        assertThrows(ArchivoCorruptoException.class, () -> new MusicFileManager(ruta));
    }

    @Test
    void falloDeAperturaConservaLaCausa() throws Exception {
        Path archivo = carpeta.resolve("no-es-carpeta");
        Files.writeString(archivo, "contenido");
        StorageIOException error = assertThrows(StorageIOException.class,
                () -> new MusicFileManager(archivo.resolve("recordings.music")));
        assertInstanceOf(IOException.class, error.getCause());
    }

    private Path crearGrabacion() throws Exception {
        Path ruta = carpeta.resolve("recordings.music");
        try (MusicFileManager gestor = new MusicFileManager(ruta)) {
            gestor.reservarEncabezado();
            gestor.escribirComando(bajo(0));
            gestor.finalizarGrabacion("A");
        }
        return ruta;
    }

    private static SynthCommand bajo(long tiempo) {
        return new SynthCommand(tiempo, SynthConstants.IDENTIFICADOR_BAJO,
                BinaryConstants.ACCION_TOCAR, 36, 450);
    }
}
