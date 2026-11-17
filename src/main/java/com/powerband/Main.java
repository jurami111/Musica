package com.powerband;

import com.powerband.common.exceptions.PowerBandException;
import com.powerband.instrumentos.ConsolaInstrumentos;
import java.io.IOException;

public final class Main {
    private Main() {
    }

    public static void main(String[] args) throws IOException, PowerBandException {
        ConsolaInstrumentos.main(args);
    }
}