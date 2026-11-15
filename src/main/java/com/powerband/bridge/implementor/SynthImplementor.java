package com.powerband.bridge.implementor;

import java.io.IOException;

public interface SynthImplementor {
    void iniciarNota(int nota, int duracionMs) throws IOException;

    void terminarNota(int nota) throws IOException;
}
