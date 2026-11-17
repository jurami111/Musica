package com.powerband.instrumentos;

import com.powerband.bridge.abstraction.InstrumentoGamepad;
import com.powerband.bridge.abstraction.MobileSensorInstrument;
import com.powerband.bridge.abstraction.WebcamInstrument;
import com.powerband.bridge.implementor.BassSynthCommand;
import com.powerband.bridge.implementor.LeadSynthCommand;
import com.powerband.bridge.implementor.PadSynthCommand;
import com.powerband.common.constants.SynthConstants;
import com.powerband.common.constants.AppConstants;
import com.powerband.common.constants.BinaryConstants;
import com.powerband.common.constants.ConstantesInstrumentos;
import com.powerband.common.exceptions.PowerBandException;
import com.powerband.editor.MusicEditorController;
import com.powerband.editor.RecorderService;
import com.powerband.editor.SesionEditor;
import com.powerband.network.BandClient;
import com.powerband.network.InstrumentSocketServer;
import com.powerband.storage.io.ListFileManager;
import com.powerband.storage.io.MusicFileManager;
import java.io.IOException;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class ConsolaInstrumentos {
    private static final Logger REGISTRO = Logger.getLogger(ConsolaInstrumentos.class.getName());

    private ConsolaInstrumentos() {
    }

    public static void main(String[] args) throws IOException, PowerBandException {
        if (args.length > 1 || (args.length == 1
                && (!args[0].matches("[0-9]{1,5}")
                    || Integer.parseInt(args[0]) > ConstantesInstrumentos.PUERTO_MAXIMO))) {
            System.err.println("Uso: Main [puerto TCP entre 0 y 65535]");
            return;
        }
        int puerto = args.length == 0 ? InstrumentSocketServer.PUERTO_PREDETERMINADO
                : Integer.parseInt(args[0]);
        try (MusicFileManager musica = new MusicFileManager();
                ListFileManager lista = new ListFileManager();
                BandClient clienteOsc = new BandClient();
                ReproductorBridge vivo = crearReproductor(clienteOsc)) {
            RecorderService grabador = new RecorderService(musica, lista);
            MusicEditorController editor = new MusicEditorController(grabador);
            BusInstrumentos bus = new BusInstrumentos(vivo, editor, grabador, System::nanoTime);
            try (SesionEditor sesion = new SesionEditor(grabador, bus, lista,
                        AppConstants.RUTA_INDICE, AppConstants.RUTA_GRABACIONES,
                        crearReproductor(clienteOsc));
                    FuenteCamara camara = new FuenteCamara(new WebcamInstrument(
                        bus.sintetizador(SynthConstants.IDENTIFICADOR_ORGANO)));
                    FuenteGamepad gamepad = new FuenteGamepad(new InstrumentoGamepad(
                        bus.sintetizador(SynthConstants.IDENTIFICADOR_BAJO)));
                    InstrumentSocketServer movil = new InstrumentSocketServer(puerto,
                        new MobileSensorInstrument(bus.sintetizador(SynthConstants.IDENTIFICADOR_PIANO)))) {
                try {
                    movil.iniciar();
                    System.out.println("Servidor móvil en puerto " + movil.puertoLocal());
                } catch (IOException error) {
                    REGISTRO.log(Level.SEVERE, "No se pudo iniciar el servidor móvil", error);
                }
                camara.iniciar();
                gamepad.iniciar();
                System.out.println("OSC UDP hacia " + SynthConstants.DIRECCION_SUPERCOLLIDER + ":"
                        + SynthConstants.PUERTO_OSC + ". Cargue los synths en SuperCollider;"
                        + " UDP no confirma que el servidor este disponible.");
                System.out.println("Comandos: grabar, finalizar, listar, cargar NOMBRE, play, parar,"
                        + " camara, gamepad, movil, parar camara, parar gamepad, parar movil, salir");
                Scanner entrada = new Scanner(System.in);
                try {
                    while (entrada.hasNextLine()) {
                        String linea = entrada.nextLine();
                        if (linea.equals("salir")) {
                            if (grabador.estaActiva()) {
                                sesion.detenerGrabacion();
                            }
                            if (grabador.tienePendiente() && !grabador.estaFallida()) {
                                solicitarNombre(entrada, sesion);
                            }
                            break;
                        }
                        try {
                            if (grabador.estaFallida()) {
                                System.err.println("La sesion fallo. Salga y revise los archivos antes de reiniciar.");
                                continue;
                            }
                            if (grabador.tienePendiente()) {
                                solicitarNombre(entrada, sesion);
                                continue;
                            }
                            if (sesion.estaReproduciendo() && !linea.equals("parar")
                                    && !linea.equals("listar") && !linea.startsWith("parar ")) {
                                System.out.println("Hay una reproduccion en curso; use parar o espere.");
                                continue;
                            }
                            if ((linea.equals("grabar") || linea.startsWith("cargar ")
                                    || linea.equals("play")) && grabador.estaActiva()) {
                                System.out.println("Primero finalice la grabacion activa.");
                                continue;
                            }
                            if (linea.equals("grabar")) {
                                sesion.iniciarGrabacion();
                                System.out.println("Grabación iniciada; use finalizar para detener y nombrar.");
                            } else if (linea.equals("finalizar")) {
                                if (grabador.estaActiva()) {
                                    sesion.detenerGrabacion();
                                    solicitarNombre(entrada, sesion);
                                } else {
                                    System.out.println("No hay una grabacion activa.");
                                }
                            } else if (linea.equals("listar")) {
                                if (lista.obtenerCantidad() == 0) {
                                    System.out.println("No hay grabaciones guardadas.");
                                }
                                sesion.listar(registro -> System.out.println(
                                        registro.obtenerNombre() + " | " + registro.obtenerFechaHora()));
                            } else if (linea.startsWith("cargar ")) {
                                System.out.println(sesion.cargar(linea.substring(7))
                                        ? "Grabacion cargada; use play."
                                        : "Nombre invalido o grabacion no encontrada.");
                            } else if (linea.equals("play")) {
                                if (!sesion.tieneCargada()) {
                                    System.out.println("Primero use cargar NOMBRE.");
                                    continue;
                                }
                                camara.close();
                                gamepad.close();
                                movil.close();
                                vivo.finalizarReproduccion();
                                sesion.reproducir().whenComplete((ignorado, error) -> {
                                    if (error == null) {
                                        System.out.println("Reproduccion finalizada. Puede reiniciar las fuentes.");
                                    } else if (error instanceof InterruptedException
                                            && error.getSuppressed().length == 0) {
                                        System.out.println("Reproduccion detenida.");
                                    } else {
                                        REGISTRO.log(Level.SEVERE, "Fallo de reproduccion", error);
                                    }
                                });
                            } else if (linea.equals("parar")) {
                                sesion.parar();
                            } else if (linea.equals("camara")) {
                                camara.iniciar();
                            } else if (linea.equals("gamepad")) {
                                gamepad.iniciar();
                            } else if (linea.equals("movil")) {
                                movil.iniciar();
                                System.out.println("Servidor móvil en puerto " + movil.puertoLocal());
                            } else if (linea.equals("parar camara")) {
                                camara.close();
                                System.out.println("Cámara detenida");
                            } else if (linea.equals("parar gamepad")) {
                                gamepad.close();
                                System.out.println("Gamepad detenido");
                            } else if (linea.equals("parar movil")) {
                                movil.close();
                                System.out.println("Servidor móvil detenido");
                            } else {
                                System.out.println("Comando desconocido");
                            }
                        } catch (PowerBandException | IOException | IllegalStateException error) {
                            REGISTRO.log(Level.SEVERE, "No se pudo completar el comando", error);
                        }
                    }
                } finally {
                    if (grabador.estaActiva()) {
                        sesion.detenerGrabacion();
                    }
                    if (grabador.tienePendiente() && !grabador.estaFallida()) {
                        solicitarNombre(entrada, sesion);
                    }
                }
            }
        }
    }

    private static ReproductorBridge crearReproductor(BandClient cliente) {
        return new ReproductorBridge(new LeadSynthCommand(cliente), new BassSynthCommand(cliente),
                new PadSynthCommand(cliente));
    }

    private static void solicitarNombre(Scanner entrada, SesionEditor sesion) throws PowerBandException {
        System.out.println("Nombre de la grabacion (unico, hasta "
                + BinaryConstants.MAXIMO_UNIDADES_NOMBRE + " unidades UTF-16):");
        while (entrada.hasNextLine()) {
            if (sesion.guardar(entrada.nextLine())) {
                System.out.println("Grabacion guardada e indexada.");
                return;
            }
            System.out.println("Nombre invalido o ya existente; indique otro:");
        }
        REGISTRO.warning("Entrada cerrada sin asignar nombre: queda un encabezado pendiente"
                + " en recordings.music y la grabacion no aparece en el catalogo.");
    }
}
