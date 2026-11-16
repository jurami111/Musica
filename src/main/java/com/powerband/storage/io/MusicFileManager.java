package com.powerband.storage.io;

import com.powerband.common.constants.AppConstants;
import com.powerband.common.constants.BinaryConstants;
import com.powerband.common.exceptions.ArchivoCorruptoException;
import com.powerband.common.exceptions.StorageIOException;
import com.powerband.common.model.RecordingHeader;
import com.powerband.common.model.SynthCommand;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

public final class MusicFileManager implements AutoCloseable {
    private final RandomAccessFile archivo;
    private long reserva = BinaryConstants.SIN_RESERVA;
    private long ultimoTiempo;
    private boolean cerrado;
    private boolean escrituraFallida;

    public MusicFileManager() throws StorageIOException, ArchivoCorruptoException {
        this(AppConstants.RUTA_GRABACIONES);
    }

    public MusicFileManager(Path ruta) throws StorageIOException, ArchivoCorruptoException {
        Objects.requireNonNull(ruta, "ruta");
        try {
            Path carpeta = ruta.toAbsolutePath().getParent();
            Files.createDirectories(carpeta);
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
            throw new StorageIOException("No se pudo abrir el archivo de grabaciones: " + ruta, error);
        }
    }

    public synchronized long reservarEncabezado() throws StorageIOException, ArchivoCorruptoException {
        comprobarEscritura();
        if (reserva != BinaryConstants.SIN_RESERVA) {
            throw new IllegalStateException("Ya hay una grabacion pendiente de finalizar");
        }
        long posicion = obtenerTamano();
        if (posicion > Long.MAX_VALUE - BinaryConstants.BYTES_REGISTRO) {
            throw new IllegalStateException("No hay espacio direccionable para otro registro");
        }
        long inicio = posicion + BinaryConstants.BYTES_REGISTRO;
        escribirEncabezado(posicion, BinaryConstants.TIPO_ENCABEZADO_PENDIENTE, "", inicio, inicio);
        reserva = posicion;
        ultimoTiempo = 0;
        return posicion;
    }

    public synchronized long escribirComando(SynthCommand comando)
            throws StorageIOException, ArchivoCorruptoException {
        comprobarReserva();
        Objects.requireNonNull(comando, "comando");
        if (comando.obtenerTiempo() < ultimoTiempo) {
            throw new IllegalArgumentException("Los tiempos de la grabacion no pueden retroceder");
        }
        long posicion = obtenerTamano();
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream(BinaryConstants.BYTES_REGISTRO);
            try (DataOutputStream salida = new DataOutputStream(bytes)) {
                salida.writeInt(BinaryConstants.TIPO_COMANDO);
                salida.writeLong(comando.obtenerTiempo());
                salida.writeInt(comando.obtenerSintetizador());
                salida.writeInt(comando.obtenerAccion());
                double[] parametros = comando.obtenerParametros();
                salida.writeInt(parametros.length);
                for (int i = 0; i < BinaryConstants.MAXIMO_PARAMETROS; i++) {
                    salida.writeDouble(i < parametros.length ? parametros[i] : 0.0);
                }
                escribirCeros(salida, BinaryConstants.BYTES_RELLENO_COMANDO);
            }
            escribirRegistro(posicion, bytes.toByteArray());
            ultimoTiempo = comando.obtenerTiempo();
            return posicion;
        } catch (IOException error) {
            escrituraFallida = true;
            throw new StorageIOException("No se pudo escribir el comando en " + posicion, error);
        }
    }

    public synchronized long finalizarGrabacion(String nombre)
            throws StorageIOException, ArchivoCorruptoException {
        comprobarReserva();
        RecordingHeader encabezado = new RecordingHeader(nombre,
                reserva + BinaryConstants.BYTES_REGISTRO, obtenerTamano());
        long posicion = reserva;
        escribirEncabezado(posicion, BinaryConstants.TIPO_ENCABEZADO, encabezado.obtenerNombre(),
                encabezado.obtenerInicio(), encabezado.obtenerFin());
        reserva = BinaryConstants.SIN_RESERVA;
        return posicion;
    }

    public synchronized RecordingHeader leerEncabezado(long posicion)
            throws StorageIOException, ArchivoCorruptoException {
        byte[] registro = leerRegistro(posicion);
        try (DataInputStream entrada = new DataInputStream(new ByteArrayInputStream(registro))) {
            if (entrada.readInt() != BinaryConstants.TIPO_ENCABEZADO) {
                throw new ArchivoCorruptoException("No hay un encabezado definitivo en " + posicion);
            }
            String nombre = NombreBinario.leer(entrada);
            RecordingHeader encabezado = new RecordingHeader(nombre,
                    entrada.readLong(), entrada.readLong());
            comprobarCeros(entrada, BinaryConstants.BYTES_RELLENO_ENCABEZADO);
            if (encabezado.obtenerInicio() != posicion + BinaryConstants.BYTES_REGISTRO
                    || encabezado.obtenerFin() > obtenerTamano()) {
                throw new ArchivoCorruptoException("Los limites del encabezado no corresponden al archivo");
            }
            return encabezado;
        } catch (IllegalArgumentException error) {
            throw new ArchivoCorruptoException("Encabezado invalido en " + posicion, error);
        } catch (IOException error) {
            throw new StorageIOException("No se pudo interpretar el encabezado en " + posicion, error);
        }
    }

    public synchronized SynthCommand leerComando(long posicion)
            throws StorageIOException, ArchivoCorruptoException {
        byte[] registro = leerRegistro(posicion);
        try (DataInputStream entrada = new DataInputStream(new ByteArrayInputStream(registro))) {
            if (entrada.readInt() != BinaryConstants.TIPO_COMANDO) {
                throw new ArchivoCorruptoException("No hay un comando en " + posicion);
            }
            long tiempo = entrada.readLong();
            int sintetizador = entrada.readInt();
            int accion = entrada.readInt();
            int cantidad = entrada.readInt();
            if (cantidad < 0 || cantidad > BinaryConstants.MAXIMO_PARAMETROS) {
                throw new ArchivoCorruptoException("Cantidad de parametros fuera del registro");
            }
            double[] parametros = new double[cantidad];
            for (int i = 0; i < BinaryConstants.MAXIMO_PARAMETROS; i++) {
                long bits = entrada.readLong();
                if (i < cantidad) {
                    parametros[i] = Double.longBitsToDouble(bits);
                } else if (bits != BinaryConstants.VALOR_RELLENO) {
                    throw new ArchivoCorruptoException("Un parametro no utilizado contiene datos");
                }
            }
            comprobarCeros(entrada, BinaryConstants.BYTES_RELLENO_COMANDO);
            return new SynthCommand(tiempo, sintetizador, accion, parametros);
        } catch (IllegalArgumentException error) {
            throw new ArchivoCorruptoException("Comando invalido en " + posicion, error);
        } catch (IOException error) {
            throw new StorageIOException("No se pudo interpretar el comando en " + posicion, error);
        }
    }

    public synchronized long obtenerTamano() throws StorageIOException, ArchivoCorruptoException {
        comprobarAbierto();
        try {
            long tamano = archivo.length();
            validarTamano(tamano);
            return tamano;
        } catch (IOException error) {
            throw new StorageIOException("No se pudo consultar el tamano de las grabaciones", error);
        }
    }

    private void escribirEncabezado(long posicion, int tipo, String nombre, long inicio, long fin)
            throws StorageIOException {
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream(BinaryConstants.BYTES_REGISTRO);
            try (DataOutputStream salida = new DataOutputStream(bytes)) {
                salida.writeInt(tipo);
                NombreBinario.escribir(salida, nombre);
                salida.writeLong(inicio);
                salida.writeLong(fin);
                escribirCeros(salida, BinaryConstants.BYTES_RELLENO_ENCABEZADO);
            }
            escribirRegistro(posicion, bytes.toByteArray());
        } catch (IOException error) {
            escrituraFallida = true;
            throw new StorageIOException("No se pudo escribir el encabezado en " + posicion, error);
        }
    }

    private void escribirRegistro(long posicion, byte[] datos) throws IOException {
        if (datos.length != BinaryConstants.BYTES_REGISTRO) {
            throw new IllegalStateException("La codificacion no produjo un registro de 256 bytes");
        }
        archivo.seek(posicion);
        archivo.write(datos);
    }

    private byte[] leerRegistro(long posicion) throws StorageIOException, ArchivoCorruptoException {
        long tamano = obtenerTamano();
        if (posicion < 0 || posicion % BinaryConstants.BYTES_REGISTRO != 0
                || posicion > tamano - BinaryConstants.BYTES_REGISTRO) {
            throw new ArchivoCorruptoException("Posicion sin un registro completo: " + posicion);
        }
        byte[] datos = new byte[BinaryConstants.BYTES_REGISTRO];
        try {
            archivo.seek(posicion);
            archivo.readFully(datos);
            return datos;
        } catch (java.io.EOFException error) {
            throw new ArchivoCorruptoException("Registro truncado en " + posicion, error);
        } catch (IOException error) {
            throw new StorageIOException("No se pudo leer el registro en " + posicion, error);
        }
    }

    private static void validarTamano(long tamano) throws ArchivoCorruptoException {
        if (tamano % BinaryConstants.BYTES_REGISTRO != 0) {
            throw new ArchivoCorruptoException("El archivo contiene un registro incompleto");
        }
    }

    private static void escribirCeros(DataOutputStream salida, int cantidad) throws IOException {
        for (int i = 0; i < cantidad; i++) {
            salida.writeByte(BinaryConstants.VALOR_RELLENO);
        }
    }

    private static void comprobarCeros(DataInputStream entrada, int cantidad)
            throws IOException, ArchivoCorruptoException {
        for (int i = 0; i < cantidad; i++) {
            if (entrada.readByte() != BinaryConstants.VALOR_RELLENO) {
                throw new ArchivoCorruptoException("El relleno del registro contiene datos");
            }
        }
    }

    private void comprobarAbierto() {
        if (cerrado) {
            throw new IllegalStateException("El archivo esta cerrado");
        }
    }

    private void comprobarEscritura() {
        comprobarAbierto();
        if (escrituraFallida) {
            throw new IllegalStateException("No se puede continuar despues de un fallo de escritura");
        }
    }

    private void comprobarReserva() {
        comprobarEscritura();
        if (reserva == BinaryConstants.SIN_RESERVA) {
            throw new IllegalStateException("Primero debe reservarse un encabezado");
        }
    }

    @Override
    public synchronized void close() throws StorageIOException {
        if (!cerrado) {
            try {
                archivo.close();
                cerrado = true;
            } catch (IOException error) {
                throw new StorageIOException("No se pudo cerrar el archivo de grabaciones", error);
            }
        }
    }
}