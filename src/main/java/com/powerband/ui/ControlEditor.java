package com.powerband.ui;

import com.powerband.common.exceptions.PowerBandException;
import com.powerband.common.model.RecordingMetadata;
import com.powerband.editor.RecorderService;
import com.powerband.editor.SesionEditor;
import com.powerband.storage.io.ListFileManager;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletionStage;

final class ControlEditor {
    static final int TAMANO_PAGINA = 50;

    interface Fuentes {
        void iniciar();
        void detener() throws IOException;
    }

    private final SesionEditor sesion;
    private final RecorderService grabador;
    private final ListFileManager lista;
    private final Fuentes fuentes;

    ControlEditor(SesionEditor sesion, RecorderService grabador, ListFileManager lista, Fuentes fuentes) {
        this.sesion = Objects.requireNonNull(sesion, "sesion");
        this.grabador = Objects.requireNonNull(grabador, "grabador");
        this.lista = Objects.requireNonNull(lista, "lista");
        this.fuentes = Objects.requireNonNull(fuentes, "fuentes");
    }

    void iniciarGrabacion() throws PowerBandException {
        sesion.iniciarGrabacion();
    }

    void detenerGrabacion() throws PowerBandException {
        sesion.detenerGrabacion();
    }

    boolean guardar(String nombre) throws PowerBandException {
        return sesion.guardar(nombre);
    }

    Pagina pagina(long inicio) throws PowerBandException {
        long cantidad = lista.obtenerCantidad();
        long ultimoInicio = cantidad == 0 ? 0 : ((cantidad - 1) / TAMANO_PAGINA) * TAMANO_PAGINA;
        long desde = Math.min(Math.max(0, inicio), ultimoInicio);
        List<RecordingMetadata> registros = new ArrayList<>();
        long fin = desde + Math.min(TAMANO_PAGINA, cantidad - desde);
        for (long numero = desde; numero < fin; numero++) {
            registros.add(lista.leerRegistro(numero));
        }
        return new Pagina(desde, cantidad, registros);
    }

    boolean cargar(String nombre) throws PowerBandException {
        return sesion.cargar(nombre);
    }

    CompletionStage<Void> reproducir() throws IOException {
        if (!sesion.tieneCargada() || grabador.estaActiva() || grabador.tienePendiente()
                || grabador.estaFallida() || sesion.estaReproduciendo()) {
            throw new IllegalStateException("No se puede iniciar la reproduccion en el estado actual");
        }
        fuentes.detener();
        return sesion.reproducir();
    }

    void parar() throws IOException {
        sesion.parar();
    }

    void reiniciarFuentes() throws IOException {
        if (sesion.estaReproduciendo()) {
            throw new IllegalStateException("No se pueden iniciar instrumentos durante la reproduccion");
        }
        fuentes.detener();
        fuentes.iniciar();
    }

    Cierre prepararCierre() throws PowerBandException {
        if (grabador.estaFallida()) {
            return Cierre.FALLIDA;
        }
        if (grabador.estaActiva()) {
            sesion.detenerGrabacion();
        }
        return grabador.tienePendiente() ? Cierre.PENDIENTE : Cierre.LISTO;
    }

    Estado estado() {
        return new Estado(grabador.estaActiva(), grabador.tienePendiente(), grabador.estaFallida(),
                sesion.tieneCargada());
    }

    record Pagina(long inicio, long cantidad, List<RecordingMetadata> registros) {
    }

    record Estado(boolean grabando, boolean pendiente, boolean fallida, boolean cargada) {
    }

    enum Cierre {
        LISTO, PENDIENTE, FALLIDA
    }
}
