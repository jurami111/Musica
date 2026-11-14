package com.powerband.common.exceptions;

public class DiskSortException extends PowerBandException {

    public DiskSortException(String mensaje) {
        super(mensaje);
    }

    public DiskSortException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}