package com.powerband.network;

import java.util.regex.Pattern;

record MuestraSensores(double ax, double ay, double az, double gx, double gy, double gz) {
    private static final Pattern DECIMAL = Pattern.compile(
            "[+-]?(?:[0-9]+(?:\\.[0-9]*)?|\\.[0-9]+)(?:[eE][+-]?[0-9]+)?");

    static MuestraSensores interpretar(String linea) {
        String[] campos = linea.split(",", -1);
        if (campos.length != 6) {
            throw new IllegalArgumentException("La muestra debe tener seis valores");
        }
        double[] valores = new double[6];
        for (int i = 0; i < campos.length; i++) {
            if (!DECIMAL.matcher(campos[i]).matches()) {
                throw new IllegalArgumentException("Número de sensor inválido");
            }
            valores[i] = Double.parseDouble(campos[i]);
            if (!Double.isFinite(valores[i])) {
                throw new IllegalArgumentException("Número de sensor fuera de rango");
            }
        }
        return new MuestraSensores(valores[0], valores[1], valores[2],
                valores[3], valores[4], valores[5]);
    }
}
