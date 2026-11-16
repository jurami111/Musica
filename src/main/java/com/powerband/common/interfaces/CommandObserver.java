package com.powerband.common.interfaces;

import com.powerband.common.exceptions.PowerBandException;
import com.powerband.common.model.SynthCommand;

public interface CommandObserver {
    void observarComando(SynthCommand comando) throws PowerBandException;
}