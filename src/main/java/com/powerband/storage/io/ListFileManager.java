package com.powerband.storage.io;

import com.powerband.common.constants.AppConstants;
import com.powerband.common.constants.BinaryConstants;
import com.powerband.common.exceptions.ArchivoCorruptoException;
import com.powerband.common.exceptions.DuplicateNameException;
import com.powerband.common.exceptions.StorageIOException;
import com.powerband.common.model.RecordingMetadata;
import com.powerband.common.model.ValidadorNombres;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Objects;

public final class ListFileManager implements AutoCloseable {
    private final RandomAccessFile archivo;
    private boolean cerrado;
    private boolean escrituraFallida;

    public ListFileManager() throws StorageIOException, ArchivoCorruptoException {
        this(AppConstants.RUTA_LISTA);
    }

    public ListFileManager(Path ruta) throws StorageIOException, ArchivoCorruptoException {
        Objects.requireNonNull(ruta, "ruta");
        try {
            Files.createDirectories(ruta.toAbsolutePath().getParent());
            RandomAccessFile abierto = new RandomAccessFile(ruta.toFile(), "rwd");
            try {
                validarTamano(abierto.length());
            } catch (IOException | ArchivoCorruptoException error) {
                try {
                    abierto.close();
                } catch (IOException cierre) {
                    error.addSuppressed(cierre);
                }
                throw error;
            }
            archivo = abierto;
        } catch (IOException error) {
            throw new StorageIOException("No se pudo abrir la lista de grabaciones: " + ruta, error);
        }
    }

    public synchronized long obtenerCantidad() throws StorageIOException, ArchivoCorruptoException {
        comprobarAbierto();
        try {
            long tamano = archivo.length();
            validarTamano(tamano);
            return tamano / BinaryConstants.BYTES_REGISTRO_LISTA;
        } catch (IOException error) {
            throw new StorageIOException("No se pudo consultar el tamano de la lista", error);
        }
    }

    public synchronized RecordingMetadata leerRegistro(long numero)
            throws StorageIOException, ArchivoCorruptoException {
        long cantidad = obtenerCantidad();
        if (numero < 0 || numero >= cantidad) {
            throw new IllegalArgumentException("Numero de registro fuera de la lista: " + numero);
        }
        byte[] datos = new byte[BinaryConstants.BYTES_REGISTRO_LISTA];
        try {
            archivo.seek(numero * BinaryConstants.BYTES_REGISTRO_LISTA);
            archivo.readFully(datos);
            try (DataInputStream entrada = new DataInputStream(new ByteArrayInputStream(datos))) {
                String nombre = NombreBinario.leer(entrada);
                return new RecordingMetadata(nombre, Instant.ofEpochMilli(entrada.readLong()));
            }
        } catch (EOFException error) {
            throw new ArchivoCorruptoException("Registro incompleto en la lista: " + numero, error);
        } catch (IllegalArgumentException error) {
            throw new ArchivoCorruptoException("Registro invalido en la lista: " + numero, error);
        } catch (IOException error) {
            throw new StorageIOException("No se pudo leer el registro de la lista: " + numero, error);
        }
    }

    public synchronized boolean existeNombre(String nombre)
            throws StorageIOException, ArchivoCorruptoException {
        ValidadorNombres.validar(nombre);
        long cantidad = obtenerCantidad();
        for (long numero = 0; numero < cantidad; numero++) {
            if (leerRegistro(numero).obtenerNombre().equals(nombre)) {
                return true;
            }
        }
        return false;
    }

    public synchronized RecordingMetadata registrar(String nombre)
            throws StorageIOException, ArchivoCorruptoException, DuplicateNameException {
        RecordingMetadata registro = new RecordingMetadata(nombre, Instant.now());
        registrar(registro);
        return registro;
    }

    public synchronized void registrar(RecordingMetadata registro)
            throws StorageIOException, ArchivoCorruptoException, DuplicateNameException {
        comprobarAbierto();
        if (escrituraFallida) {
            throw new IllegalStateException("No se puede continuar despues de un fallo de escritura");
        }
        Objects.requireNonNull(registro, "registro");
        if (existeNombre(registro.obtenerNombre())) {
            throw new DuplicateNameException("Ya existe una grabacion con el nombre: " + registro.obtenerNombre());
        }
        long cantidad = obtenerCantidad();
        if (cantidad >= Long.MAX_VALUE / BinaryConstants.BYTES_REGISTRO_LISTA) {
            throw new IllegalStateException("No hay espacio direccionable para otro registro");
        }
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream(BinaryConstants.BYTES_REGISTRO_LISTA);
            try (DataOutputStream salida = new DataOutputStream(bytes)) {
                NombreBinario.escribir(salida, registro.obtenerNombre());
                salida.writeLong(registro.obtenerFechaHora().toEpochMilli());
            }
            byte[] datos = bytes.toByteArray();
            if (datos.length != BinaryConstants.BYTES_REGISTRO_LISTA) {
                throw new IllegalStateException("La codificacion no produjo un registro de lista completo");
            }
            archivo.seek(cantidad * BinaryConstants.BYTES_REGISTRO_LISTA);
            archivo.write(datos);
        } catch (IOException error) {
            escrituraFallida = true;
            throw new StorageIOException("No se pudo registrar la grabacion en la lista", error);
        }
    }

    private static void validarTamano(long tamano) throws ArchivoCorruptoException {
        if (tamano % BinaryConstants.BYTES_REGISTRO_LISTA != 0) {
            throw new ArchivoCorruptoException("La lista contiene un registro incompleto");
        }
    }

    private void comprobarAbierto() {
        if (cerrado) {
            throw new IllegalStateException("La lista esta cerrada");
        }
    }

    @Override
    public synchronized void close() throws StorageIOException {
        if (!cerrado) {
            try {
                archivo.close();
                cerrado = true;
            } catch (IOException error) {
                throw new StorageIOException("No se pudo cerrar la lista de grabaciones", error);
            }
        }
    }
}