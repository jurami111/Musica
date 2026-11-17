package com.powerband.ui;

import com.powerband.bridge.abstraction.InstrumentoGamepad;
import com.powerband.bridge.abstraction.MobileSensorInstrument;
import com.powerband.bridge.abstraction.WebcamInstrument;
import com.powerband.bridge.implementor.BassSynthCommand;
import com.powerband.bridge.implementor.LeadSynthCommand;
import com.powerband.bridge.implementor.PadSynthCommand;
import com.powerband.common.constants.AppConstants;
import com.powerband.common.constants.SynthConstants;
import com.powerband.common.exceptions.PowerBandException;
import com.powerband.editor.MusicEditorController;
import com.powerband.editor.RecorderService;
import com.powerband.editor.SesionEditor;
import com.powerband.instrumentos.BusInstrumentos;
import com.powerband.instrumentos.FuenteCamara;
import com.powerband.instrumentos.FuenteGamepad;
import com.powerband.instrumentos.ReproductorBridge;
import com.powerband.network.BandClient;
import com.powerband.network.InstrumentSocketServer;
import com.powerband.storage.io.ListFileManager;
import com.powerband.storage.io.MusicFileManager;
import java.io.IOException;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

final class RecursosEditor implements AutoCloseable {
    private static final Logger REGISTRO = Logger.getLogger(RecursosEditor.class.getName());
    private final MusicFileManager musica;
    private final ListFileManager lista;
    private final BandClient cliente;
    private final ReproductorBridge vivo;
    private final SesionEditor sesion;
    private final FuentesReales fuentes;
    private final Handler avisos;
    private final Logger[] registros;
    final ColaMonitor monitor;
    final ControlEditor control;

    private RecursosEditor(MusicFileManager musica, ListFileManager lista, BandClient cliente,
            ReproductorBridge vivo, SesionEditor sesion, FuentesReales fuentes,
            ColaMonitor monitor, RecorderService grabador, Consumer<String> mostrarAviso) {
        this.musica = musica;
        this.lista = lista;
        this.cliente = cliente;
        this.vivo = vivo;
        this.sesion = sesion;
        this.fuentes = fuentes;
        this.monitor = monitor;
        control = new ControlEditor(sesion, grabador, lista, fuentes);
        avisos = new Handler() {
            @Override
            public void publish(LogRecord registro) {
                if (registro.getLevel().intValue() >= Level.WARNING.intValue()) {
                    String detalle = registro.getThrown() == null ? "" : ": " + registro.getThrown();
                    mostrarAviso.accept(registro.getMessage() + detalle);
                }
            }

            @Override
            public void flush() {
            }

            @Override
            public void close() {
            }
        };
        registros = new Logger[] {Logger.getLogger(FuenteCamara.class.getName()),
                Logger.getLogger(FuenteGamepad.class.getName()),
                Logger.getLogger(InstrumentSocketServer.class.getName()), REGISTRO};
        for (Logger registro : registros) {
            registro.addHandler(avisos);
        }
    }

    static RecursosEditor abrir(int puerto, Consumer<String> mostrarAviso)
            throws IOException, PowerBandException {
        Objects.requireNonNull(mostrarAviso, "mostrarAviso");
        MusicFileManager musica = null;
        ListFileManager lista = null;
        BandClient cliente = null;
        ReproductorBridge vivo = null;
        SesionEditor sesion = null;
        try {
            musica = new MusicFileManager();
            lista = new ListFileManager();
            cliente = new BandClient();
            vivo = crearReproductor(cliente);
            RecorderService grabador = new RecorderService(musica, lista);
            ColaMonitor monitor = new ColaMonitor(new MusicEditorController(grabador));
            BusInstrumentos bus = new BusInstrumentos(vivo, monitor, grabador, System::nanoTime);
            sesion = new SesionEditor(grabador, bus, lista, AppConstants.RUTA_INDICE,
                    AppConstants.RUTA_GRABACIONES, crearReproductor(cliente));
            FuentesReales fuentes = new FuentesReales(
                    new FuenteCamara(new WebcamInstrument(bus.sintetizador(SynthConstants.IDENTIFICADOR_ORGANO))),
                    new FuenteGamepad(new InstrumentoGamepad(bus.sintetizador(SynthConstants.IDENTIFICADOR_BAJO))),
                    new InstrumentSocketServer(puerto,
                            new MobileSensorInstrument(bus.sintetizador(SynthConstants.IDENTIFICADOR_PIANO))),
                    vivo);
            return new RecursosEditor(musica, lista, cliente, vivo, sesion, fuentes,
                    monitor, grabador, mostrarAviso);
        } catch (IOException | PowerBandException | RuntimeException | LinkageError error) {
            Throwable cierre = cerrarRecursos(null, sesion, vivo, cliente, lista, musica);
            if (cierre != null) {
                error.addSuppressed(cierre);
            }
            throw error;
        }
    }

    void iniciarFuentes() {
        fuentes.iniciar();
    }

    @Override
    public void close() throws IOException {
        Throwable fallo = cerrarRecursos(fuentes, sesion, vivo, cliente, lista, musica);
        for (Logger registro : registros) {
            registro.removeHandler(avisos);
        }
        if (fallo != null) {
            throw new IOException("No se pudieron cerrar todos los recursos del editor", fallo);
        }
    }

    private static ReproductorBridge crearReproductor(BandClient cliente) {
        return new ReproductorBridge(new LeadSynthCommand(cliente), new BassSynthCommand(cliente),
                new PadSynthCommand(cliente));
    }

    private static Throwable cerrarRecursos(FuentesReales fuentes, SesionEditor sesion,
            ReproductorBridge vivo, BandClient cliente, ListFileManager lista, MusicFileManager musica) {
        Throwable fallo = null;
        try {
            if (fuentes != null) {
                fuentes.detener();
            }
        } catch (IOException | RuntimeException error) {
            fallo = acumular(fallo, error);
        }
        try {
            if (sesion != null) {
                sesion.close();
            }
        } catch (IOException | RuntimeException error) {
            fallo = acumular(fallo, error);
        }
        try {
            if (vivo != null) {
                vivo.close();
            }
        } catch (IOException | RuntimeException error) {
            fallo = acumular(fallo, error);
        }
        if (cliente != null) {
            cliente.close();
        }
        try {
            if (lista != null) {
                lista.close();
            }
        } catch (PowerBandException | RuntimeException error) {
            fallo = acumular(fallo, error);
        }
        try {
            if (musica != null) {
                musica.close();
            }
        } catch (PowerBandException | RuntimeException error) {
            fallo = acumular(fallo, error);
        }
        return fallo;
    }

    private static Throwable acumular(Throwable anterior, Throwable nuevo) {
        if (anterior == null) {
            return nuevo;
        }
        anterior.addSuppressed(nuevo);
        return anterior;
    }

    private static final class FuentesReales implements ControlEditor.Fuentes {
        private final FuenteCamara camara;
        private final FuenteGamepad gamepad;
        private final InstrumentSocketServer movil;
        private final ReproductorBridge vivo;

        FuentesReales(FuenteCamara camara, FuenteGamepad gamepad,
                InstrumentSocketServer movil, ReproductorBridge vivo) {
            this.camara = camara;
            this.gamepad = gamepad;
            this.movil = movil;
            this.vivo = vivo;
        }

        @Override
        public void iniciar() {
            try {
                movil.iniciar();
                REGISTRO.info("Servidor movil en puerto " + movil.puertoLocal());
            } catch (IOException | RuntimeException error) {
                REGISTRO.log(Level.SEVERE, "No se pudo iniciar el servidor movil", error);
            }
            try {
                camara.iniciar();
            } catch (RuntimeException | LinkageError error) {
                REGISTRO.log(Level.SEVERE, "No se pudo iniciar la camara", error);
            }
            try {
                gamepad.iniciar();
            } catch (RuntimeException | LinkageError error) {
                REGISTRO.log(Level.SEVERE, "No se pudo iniciar el gamepad", error);
            }
        }

        @Override
        public void detener() throws IOException {
            Throwable fallo = null;
            try {
                camara.close();
            } catch (IOException | RuntimeException error) {
                fallo = acumular(fallo, error);
            }
            try {
                gamepad.close();
            } catch (IOException | RuntimeException error) {
                fallo = acumular(fallo, error);
            }
            try {
                movil.close();
            } catch (IOException | RuntimeException error) {
                fallo = acumular(fallo, error);
            }
            try {
                vivo.finalizarReproduccion();
            } catch (IOException | RuntimeException error) {
                fallo = acumular(fallo, error);
            }
            if (fallo != null) {
                throw new IOException("No se pudieron detener todas las fuentes", fallo);
            }
        }
    }
}
