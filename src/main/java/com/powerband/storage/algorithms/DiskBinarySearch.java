package com.powerband.storage.algorithms;

import com.powerband.common.constants.AppConstants;
import com.powerband.common.constants.BinaryConstants;
import com.powerband.common.exceptions.ArchivoCorruptoException;
import com.powerband.common.exceptions.StorageIOException;
import com.powerband.common.model.IndexEntry;
import com.powerband.common.model.ValidadorNombres;
import com.powerband.storage.io.NombreBinario;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Optional;

public final class DiskBinarySearch {
    public Optional<IndexEntry> buscar(String nombre)
            throws StorageIOException, ArchivoCorruptoException {
        return buscar(AppConstants.RUTA_INDICE, nombre);
    }

    public Optional<IndexEntry> buscar(Path rutaIndice, String nombre)
            throws StorageIOException, ArchivoCorruptoException {
        Objects.requireNonNull(rutaIndice, "rutaIndice");
        ValidadorNombres.validar(nombre);
        Path ruta = rutaIndice.toAbsolutePath().normalize();
        if (!Files.isRegularFile(ruta, LinkOption.NOFOLLOW_LINKS)) {
            throw new StorageIOException("El indice no existe o no es un archivo regular: " + ruta);
        }

        try (RandomAccessFile archivo = new RandomAccessFile(ruta.toFile(), "r")) {
            long tamano = archivo.length();
            if (tamano % BinaryConstants.BYTES_REGISTRO != 0) {
                throw new ArchivoCorruptoException("El indice contiene un registro incompleto");
            }
            long cantidad = tamano / BinaryConstants.BYTES_REGISTRO;
            long inferior = 0;
            long superior = cantidad - 1;
            while (inferior <= superior) {
                long medio = inferior + (superior - inferior) / 2;
                long posicion = medio * BinaryConstants.BYTES_REGISTRO;
                IndexEntry entrada = leerEntrada(archivo, posicion);
                int comparacion = nombre.compareTo(entrada.obtenerNombre());
                if (comparacion == 0) {
                    return Optional.of(entrada);
                }
                if (comparacion < 0) {
                    superior = medio - 1;
                } else {
                    inferior = medio + 1;
                }
            }
            return Optional.empty();
        } catch (ArchivoCorruptoException error) {
            throw error;
        } catch (IOException error) {
            throw new StorageIOException("No se pudo buscar en el indice: " + ruta, error);
        }
    }

    private IndexEntry leerEntrada(RandomAccessFile archivo, long posicion)
            throws IOException, ArchivoCorruptoException {
        archivo.seek(posicion);
        String nombre;
        long posicionEncabezado;
        try {
            nombre = NombreBinario.leer(archivo);
            posicionEncabezado = archivo.readLong();
            for (int i = 0; i < BinaryConstants.BYTES_RELLENO_INDICE; i++) {
                if (archivo.readByte() != BinaryConstants.VALOR_RELLENO) {
                    throw new ArchivoCorruptoException(
                            "El relleno del indice contiene datos en " + posicion);
                }
            }
        } catch (java.io.EOFException error) {
            throw new ArchivoCorruptoException("Registro incompleto en el indice: " + posicion, error);
        }
        try {
            return new IndexEntry(nombre, posicionEncabezado);
        } catch (IllegalArgumentException error) {
            throw new ArchivoCorruptoException("Registro invalido en el indice: " + posicion, error);
        }
    }
}