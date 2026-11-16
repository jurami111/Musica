package com.powerband.editor;

import com.powerband.common.exceptions.PowerBandException;
import com.powerband.common.interfaces.CommandObserver;
import com.powerband.common.model.SynthCommand;
import java.util.Objects;

public class MusicEditorController implements CommandObserver {
    private final RecorderService grabador;

    public MusicEditorController(RecorderService grabador) {
        this.grabador = Objects.requireNonNull(grabador, "grabador");
    }

    @Override
    public void observarComando(SynthCommand comando) throws PowerBandException {
        grabador.observarComando(comando);
    }
}
