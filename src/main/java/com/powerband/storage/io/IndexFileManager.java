package com.powerband.storage.io;

import com.powerband.common.constants.AppConstants;
import com.powerband.common.constants.BinaryConstants;
import com.powerband.common.exceptions.ArchivoCorruptoException;
import com.powerband.common.exceptions.StorageIOException;
import com.powerband.common.model.IndexEntry;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

public final class IndexFileManager implements AutoCloseable {
    private final RandomAccessFile archivo;
    private boolean cerrado;
    private boolean escrituraFallida;

    public IndexFileManager() throws StorageIOException, ArchivoCorruptoException {
        this(AppConstants.RUTA_INDICE);
    }

    public IndexFileManager(Path ruta) throws StorageIOException, ArchivoCorruptoException {
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
            throw new StorageIOException("No se pudo abrir el indice: " + ruta, error);
        }
    }

    public synchronized long obtenerCantidad() throws StorageIOException, ArchivoCorruptoException {
        comprobarAbierto();
        try {
            long tamano = archivo.length();
            validarTamano(tamano);
            return tamano / BinaryConstants.BYTES_REGISTRO;
        } catch (IOException error) {
            throw new StorageIOException("No se pudo consultar el tamano del indice", error);
        }
    }

    public synchronized void agregarRegistro(IndexEntry entrada)
            throws StorageIOException, ArchivoCorruptoException {
        comprobarAbierto();
        if (escrituraFallida) {
            throw new IllegalStateException("No se puede continuar despues de un fallo de escritura");
        }
        Objects.requireNonNull(entrada, "entrada");
        long cantidad = obtenerCantidad();
        if (cantidad >= Long.MAX_VALUE / BinaryConstants.BYTES_REGISTRO) {
            throw new IllegalStateException("No hay espacio direccionable para otro registro");
        }
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream(BinaryConstants.BYTES_REGISTRO);
            try (DataOutputStream salida = new DataOutputStream(bytes)) {
                NombreBinario.escribir(salida, entrada.obtenerNombre());
                salida.writeLong(entrada.obtenerPosicionEncabezado());
                for (int i = 0; i < BinaryConstants.BYTES_RELLENO_INDICE; i++) {
                    salida.writeByte(BinaryConstants.VALOR_RELLENO);
                }
            }
            byte[] registro = bytes.toByteArray();
            if (registro.length != BinaryConstants.BYTES_REGISTRO) {
                throw new IllegalStateException("La codificacion no produjo un registro de indice completo");
            }
            archivo.seek(cantidad * BinaryConstants.BYTES_REGISTRO);
            archivo.write(registro);
        } catch (IOException error) {
            escrituraFallida = true;
            throw new StorageIOException("No se pudo escribir el registro del indice", error);
        }
    }

    public synchronized IndexEntry leerRegistro(long numero)
            throws StorageIOException, ArchivoCorruptoException {
        long cantidad = obtenerCantidad();
        if (numero < 0 || numero >= cantidad) {
            throw new IllegalArgumentException("Numero de registro fuera del indice: " + numero);
        }
        byte[] datos = new byte[BinaryConstants.BYTES_REGISTRO];
        try {
            archivo.seek(numero * BinaryConstants.BYTES_REGISTRO);
            archivo.readFully(datos);
            try (DataInputStream entrada = new DataInputStream(new ByteArrayInputStream(datos))) {
                String nombre = NombreBinario.leer(entrada);
                long posicion = entrada.readLong();
                for (int i = 0; i < BinaryConstants.BYTES_RELLENO_INDICE; i++) {
                    if (entrada.readByte() != BinaryConstants.VALOR_RELLENO) {
                        throw new ArchivoCorruptoException("El relleno del indice contiene datos");
                    }
                }
                return new IndexEntry(nombre, posicion);
            }
        } catch (EOFException error) {
            throw new ArchivoCorruptoException("Registro incompleto en el indice: " + numero, error);
        } catch (IllegalArgumentException error) {
            throw new ArchivoCorruptoException("Registro invalido en el indice: " + numero, error);
        } catch (IOException error) {
            throw new StorageIOException("No se pudo leer el registro del indice: " + numero, error);
        }
    }

    private static void validarTamano(long tamano) throws ArchivoCorruptoException {
        if (tamano % BinaryConstants.BYTES_REGISTRO != 0) {
            throw new ArchivoCorruptoException("El indice contiene un registro incompleto");
        }
    }

    private void comprobarAbierto() {
        if (cerrado) {
            throw new IllegalStateException("El indice esta cerrado");
        }
    }

    @Override
    public synchronized void close() throws StorageIOException {
        if (!cerrado) {
            try {
                archivo.close();
                cerrado = true;
            } catch (IOException error) {
                throw new StorageIOException("No se pudo cerrar el indice", error);
            }
        }
    }
}