package com.powerband.common.exceptions;

public class PowerBandException extends Exception {

    public PowerBandException(String mensaje) {
        super(mensaje);
    }

    public PowerBandException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
