package com.powerband.common.exceptions;

public class ArchivoCorruptoException extends PowerBandException {

    public ArchivoCorruptoException(String mensaje) {
        super(mensaje);
    }

    public ArchivoCorruptoException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
