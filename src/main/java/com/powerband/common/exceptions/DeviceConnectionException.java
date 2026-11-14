package com.powerband.common.exceptions;

public class DeviceConnectionException extends PowerBandException {

    public DeviceConnectionException(String mensaje) {
        super(mensaje);
    }

    public DeviceConnectionException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}