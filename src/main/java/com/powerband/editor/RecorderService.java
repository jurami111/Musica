package com.powerband.editor;

import com.powerband.common.exceptions.PowerBandException;
import com.powerband.common.interfaces.CommandObserver;
import com.powerband.common.model.SynthCommand;
import com.powerband.common.model.ValidadorNombres;
import com.powerband.storage.io.ListFileManager;
import com.powerband.storage.io.MusicFileManager;
import java.util.Objects;

public class RecorderService implements CommandObserver {
    private final MusicFileManager musica;
    private final ListFileManager lista;
    private String nombre;
    private boolean activa;
    private boolean fallida;

    public RecorderService(MusicFileManager musica, ListFileManager lista) {
        this.musica = Objects.requireNonNull(musica, "musica");
        this.lista = Objects.requireNonNull(lista, "lista");
    }

    public boolean iniciar(String nombre) throws PowerBandException {
        if (activa || fallida) {
            throw new IllegalStateException("Ya hay una grabación activa o fallida");
        }
        if (!ValidadorNombres.esValido(nombre) || lista.existeNombre(nombre)) {
            return false;
        }
        musica.reservarEncabezado();
        this.nombre = nombre;
        activa = true;
        return true;
    }

    @Override
    public void observarComando(SynthCommand comando) throws PowerBandException {
        Objects.requireNonNull(comando, "comando");
        if (activa) {
            try {
                musica.escribirComando(comando);
            } catch (PowerBandException | RuntimeException error) {
                activa = false;
                fallida = true;
                throw error;
            }
        }
    }

    public void finalizar() throws PowerBandException {
        if (!activa) {
            throw new IllegalStateException("No hay una grabación activa");
        }
        activa = false;
        try {
            musica.finalizarGrabacion(nombre);
            lista.registrar(nombre);
        } catch (PowerBandException | RuntimeException error) {
            fallida = true;
            throw error;
        }
    }

    public boolean estaActiva() {
        return activa;
    }
}
