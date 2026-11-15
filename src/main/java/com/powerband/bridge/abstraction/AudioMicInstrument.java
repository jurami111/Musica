package com.powerband.bridge.abstraction;

import com.powerband.bridge.implementor.SynthImplementor;

public class AudioMicInstrument extends Instrument {
    public AudioMicInstrument(SynthImplementor sintetizador) {
        super(sintetizador);
    }
}
