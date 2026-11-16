package com.powerband.instrumentos;

import com.powerband.bridge.abstraction.InstrumentoGamepad;
import com.powerband.bridge.abstraction.MobileSensorInstrument;
import com.powerband.bridge.abstraction.WebcamInstrument;
import com.powerband.bridge.implementor.BassSynthCommand;
import com.powerband.bridge.implementor.LeadSynthCommand;
import com.powerband.bridge.implementor.PadSynthCommand;
import com.powerband.common.constants.SynthConstants;
import com.powerband.common.exceptions.PowerBandException;
import com.powerband.editor.MusicEditorController;
import com.powerband.editor.RecorderService;
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
        int puerto = args.length == 0 ? InstrumentSocketServer.PUERTO_PREDETERMINADO
                : Integer.parseInt(args[0]);
        try (MusicFileManager musica = new MusicFileManager();
                ListFileManager lista = new ListFileManager();
                BandClient clienteOsc = new BandClient()) {
            RecorderService grabador = new RecorderService(musica, lista);
            MusicEditorController editor = new MusicEditorController(grabador);
            BusInstrumentos bus = new BusInstrumentos(new ReproductorBridge(
                    new LeadSynthCommand(clienteOsc), new BassSynthCommand(clienteOsc),
                    new PadSynthCommand(clienteOsc)), editor, grabador, System::nanoTime);
            try (FuenteCamara camara = new FuenteCamara(new WebcamInstrument(
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
                System.out.println("Comandos: grabar NOMBRE, finalizar, camara, gamepad, movil,"
                        + " parar camara, parar gamepad, parar movil, salir");
                Scanner entrada = new Scanner(System.in);
                try {
                    while (entrada.hasNextLine()) {
                        String linea = entrada.nextLine();
                        if (linea.equals("salir")) {
                            break;
                        }
                        try {
                            if (linea.startsWith("grabar ")) {
                                if (bus.iniciarGrabacion(linea.substring(7))) {
                                    System.out.println("Grabación iniciada");
                                } else {
                                    System.out.println("Nombre inválido o ya existente");
                                }
                            } else if (linea.equals("finalizar")) {
                                bus.finalizarGrabacion();
                                System.out.println("Grabación guardada");
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
                    camara.close();
                    gamepad.close();
                    movil.close();
                    if (grabador.estaActiva()) {
                        try {
                            bus.finalizarGrabacion();
                            System.out.println("Grabación guardada");
                        } catch (PowerBandException | IllegalStateException error) {
                            REGISTRO.log(Level.SEVERE, "No se pudo finalizar la grabación", error);
                        }
                    }
                }
            }
        }
    }
}
