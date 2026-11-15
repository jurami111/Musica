package com.powerband.bridge.implementor;

import java.io.IOException;

public interface EmisorOsc {
    int crearSintetizador(String nombre, Object... controles) throws IOException;

    void cambiarParametros(int nodo, Object... controles) throws IOException;
}
