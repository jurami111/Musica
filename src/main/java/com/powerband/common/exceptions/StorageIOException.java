package com.powerband.common.exceptions;

public class StorageIOException extends PowerBandException {

    public StorageIOException(String mensaje) {
        super(mensaje);
    }

    public StorageIOException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}