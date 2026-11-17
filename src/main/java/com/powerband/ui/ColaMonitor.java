package com.powerband.ui;

import com.powerband.common.exceptions.PowerBandException;
import com.powerband.common.interfaces.CommandObserver;
import com.powerband.common.model.SynthCommand;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

final class ColaMonitor implements CommandObserver {
    static final int LIMITE = 200;
    private final CommandObserver editor;
    private final ArrayDeque<SynthCommand> pendientes = new ArrayDeque<>();
    private long omitidos;

    ColaMonitor(CommandObserver editor) {
        this.editor = Objects.requireNonNull(editor, "editor");
    }

    @Override
    public void observarComando(SynthCommand comando) throws PowerBandException {
        editor.observarComando(comando);
        synchronized (this) {
            if (pendientes.size() == LIMITE) {
                pendientes.removeFirst();
                omitidos++;
            }
            pendientes.addLast(comando);
        }
    }

    synchronized Lote retirar() {
        Lote lote = new Lote(new ArrayList<>(pendientes), omitidos);
        pendientes.clear();
        omitidos = 0;
        return lote;
    }

    record Lote(List<SynthCommand> comandos, long omitidos) {
    }
}
