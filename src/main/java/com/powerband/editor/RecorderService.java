package com.powerband.editor;

import com.powerband.common.exceptions.PowerBandException;
import com.powerband.common.constants.AppConstants;
import com.powerband.common.interfaces.CommandObserver;
import com.powerband.common.model.IndexEntry;
import com.powerband.common.model.RecordingMetadata;
import com.powerband.common.model.SynthCommand;
import com.powerband.common.model.ValidadorNombres;
import com.powerband.storage.io.ListFileManager;
import com.powerband.storage.io.MusicFileManager;
import com.powerband.storage.io.IndexFileManager;
import com.powerband.storage.algorithms.BalancedMergeSort;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Objects;

public class RecorderService implements CommandObserver {
    private final MusicFileManager musica;
    private final ListFileManager lista;
    private final Path rutaIndice;
    private Instant fechaInicio;
    private String nombre;
    private boolean activa;
    private boolean fallida;
    private boolean pendiente;

    public RecorderService(MusicFileManager musica, ListFileManager lista) {
        this(musica, lista, musica.obtenerRuta().resolveSibling(AppConstants.NOMBRE_ARCHIVO_INDICE));
    }

    public RecorderService(MusicFileManager musica, ListFileManager lista, Path rutaIndice) {
        this.musica = Objects.requireNonNull(musica, "musica");
        this.lista = Objects.requireNonNull(lista, "lista");
        this.rutaIndice = Objects.requireNonNull(rutaIndice, "rutaIndice");
    }

    public synchronized boolean iniciar(String nombre) throws PowerBandException {
        comprobarDisponible();
        if (!nombreDisponible(nombre)) {
            return false;
        }
        iniciar();
        this.nombre = nombre;
        return true;
    }

    public synchronized void iniciar() throws PowerBandException {
        comprobarDisponible();
        try {
            musica.reservarEncabezado();
            fechaInicio = Instant.now();
            nombre = null;
            activa = true;
        } catch (PowerBandException | RuntimeException error) {
            fallida = true;
            throw error;
        }
    }

    public synchronized boolean nombreDisponible(String candidato) throws PowerBandException {
        return ValidadorNombres.esValido(candidato) && !lista.existeNombre(candidato);
    }

    @Override
    public synchronized void observarComando(SynthCommand comando) throws PowerBandException {
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

    public synchronized void detener() {
        if (!activa) {
            throw new IllegalStateException("No hay una grabación activa");
        }
        activa = false;
        pendiente = true;
    }

    public synchronized void finalizar() throws PowerBandException {
        if (nombre == null) {
            throw new IllegalStateException("Debe asignar un nombre al finalizar");
        }
        detener();
        if (!guardar(nombre)) {
            throw new IllegalStateException("El nombre ya no esta disponible");
        }
    }

    public synchronized boolean guardar(String candidato) throws PowerBandException {
        if (!pendiente || fallida) {
            throw new IllegalStateException("No hay una grabacion pendiente de nombre o la sesion fallo");
        }
        String etapa = "validar nombre en recordings.list";
        try {
            if (!nombreDisponible(candidato)) {
                return false;
            }
            etapa = "completar recordings.music";
            long posicion = musica.finalizarGrabacion(candidato);
            etapa = "actualizar recordings.index";
            try (IndexFileManager indice = new IndexFileManager(rutaIndice)) {
                indice.agregarRegistro(new IndexEntry(candidato, posicion));
            }
            etapa = "ordenar recordings.index";
            new BalancedMergeSort().ordenar(rutaIndice);
            etapa = "actualizar recordings.list";
            lista.registrar(new RecordingMetadata(candidato, fechaInicio));
            pendiente = false;
            nombre = null;
            return true;
        } catch (PowerBandException | RuntimeException error) {
            fallida = true;
            throw new PowerBandException("Fallo al " + etapa
                    + "; los archivos pueden estar inconsistentes. Reinicie solo despues de revisarlos.", error);
        }
    }

    private void comprobarDisponible() {
        if (activa || pendiente || fallida) {
            throw new IllegalStateException("Ya hay una grabacion activa, pendiente o fallida");
        }
    }

    public synchronized boolean estaActiva() {
        return activa;
    }

    public synchronized boolean tienePendiente() {
        return pendiente;
    }

    public synchronized boolean estaFallida() {
        return fallida;
    }
}
