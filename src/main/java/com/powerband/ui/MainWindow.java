package com.powerband.ui;

import com.powerband.common.constants.ConstantesInstrumentos;
import com.powerband.common.exceptions.PowerBandException;
import com.powerband.network.InstrumentSocketServer;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

public class MainWindow extends JFrame {
    private static final Logger REGISTRO = Logger.getLogger(MainWindow.class.getName());
    private final ExecutorService trabajador = Executors.newSingleThreadExecutor();
    private final LiveMonitorPanel monitor = new LiveMonitorPanel();
    private final RecordingsListPanel lista = new RecordingsListPanel();
    private final JButton grabar = new JButton("Grabar");
    private final JButton detenerGrabacion = new JButton("Parar grabacion");
    private final JButton nombrar = new JButton("Nombrar y guardar");
    private final JButton reproducir = new JButton("Reproducir");
    private final JButton parar = new JButton("Parar reproduccion");
    private final JButton reiniciar = new JButton("Reiniciar instrumentos");
    private final JLabel estadoTexto = new JLabel("Abriendo editor...");
    private final JTextArea avisos = new JTextArea(3, 60);
    private final ArrayDeque<String> mensajes = new ArrayDeque<>();
    private final Timer actualizacionMonitor;
    private volatile RecursosEditor recursos;
    private ControlEditor.Estado estado;
    private boolean ocupado;
    private boolean reproduciendo;
    private boolean cerrando;
    private boolean cerrarAlGuardar;

    public MainWindow(int puerto) {
        super("Power Band - Editor musical");
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        setMinimumSize(new Dimension(760, 500));
        setSize(1000, 650);
        setLocationRelativeTo(null);

        JSplitPane paneles = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, monitor, lista);
        paneles.setResizeWeight(0.55);
        add(paneles, BorderLayout.CENTER);

        JPanel botones = new JPanel(new GridLayout(2, 3, 5, 5));
        botones.add(grabar);
        botones.add(detenerGrabacion);
        botones.add(nombrar);
        botones.add(reproducir);
        botones.add(parar);
        botones.add(reiniciar);
        add(botones, BorderLayout.NORTH);

        JPanel inferior = new JPanel(new BorderLayout());
        avisos.setEditable(false);
        avisos.setLineWrap(true);
        avisos.setWrapStyleWord(true);
        inferior.add(estadoTexto, BorderLayout.NORTH);
        inferior.add(new JScrollPane(avisos), BorderLayout.CENTER);
        add(inferior, BorderLayout.SOUTH);

        grabar.addActionListener(evento -> ejecutar(() -> {
            recursos.control.iniciarGrabacion();
            return null;
        }, resultado -> informar("Grabacion iniciada.")));
        detenerGrabacion.addActionListener(evento -> detenerYNombrar());
        nombrar.addActionListener(evento -> solicitarNombre());
        lista.anterior.addActionListener(evento -> mostrarPagina(lista.inicio() - ControlEditor.TAMANO_PAGINA));
        lista.siguiente.addActionListener(evento -> mostrarPagina(lista.inicio() + ControlEditor.TAMANO_PAGINA));
        lista.cargar.addActionListener(evento -> cargarSeleccion());
        reproducir.addActionListener(evento -> iniciarReproduccion());
        parar.addActionListener(evento -> ejecutar(() -> {
            recursos.control.parar();
            return null;
        }, resultado -> informar("Deteniendo reproduccion...")));
        reiniciar.addActionListener(evento -> ejecutar(() -> {
            recursos.control.reiniciarFuentes();
            return null;
        }, resultado -> informar("Se solicito el inicio de los instrumentos; revise los avisos.")));
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent evento) {
                solicitarCierre();
            }
        });

        actualizacionMonitor = new Timer(150, evento -> {
            if (recursos != null) {
                monitor.mostrar(recursos.monitor.retirar());
            }
        });
        actualizacionMonitor.start();
        actualizarControles();
        ejecutar(() -> {
            RecursosEditor abiertos = RecursosEditor.abrir(puerto,
                    aviso -> SwingUtilities.invokeLater(() -> recibirAviso(aviso)));
            try {
                abiertos.iniciarFuentes();
                ControlEditor.Pagina pagina = abiertos.control.pagina(0);
                recursos = abiertos;
                return pagina;
            } catch (PowerBandException | RuntimeException | LinkageError error) {
                try {
                    abiertos.close();
                } catch (IOException cierre) {
                    error.addSuppressed(cierre);
                }
                throw error;
            }
        }, pagina -> {
            lista.mostrar(pagina);
            informar("Se solicito el inicio de los instrumentos. OSC por UDP no confirma sonido.");
        });
    }

    public static void main(String[] args) {
        if (args.length > 1 || (args.length == 1 && (!args[0].matches("[0-9]{1,5}")
                || Integer.parseInt(args[0]) > ConstantesInstrumentos.PUERTO_MAXIMO))) {
            System.err.println("Uso: MainWindow [puerto TCP entre 0 y 65535]");
            return;
        }
        int puerto = args.length == 0 ? InstrumentSocketServer.PUERTO_PREDETERMINADO
                : Integer.parseInt(args[0]);
        SwingUtilities.invokeLater(() -> new MainWindow(puerto).setVisible(true));
    }

    private void detenerYNombrar() {
        ejecutar(() -> {
            recursos.control.detenerGrabacion();
            return null;
        }, resultado -> solicitarNombre());
    }

    private void solicitarNombre() {
        String nombre = JOptionPane.showInputDialog(this,
                "Nombre unico de la grabacion (hasta 50 unidades UTF-16):");
        if (nombre == null) {
            cerrarAlGuardar = false;
            informar("La grabacion sigue pendiente de nombre.");
            return;
        }
        ejecutar(() -> recursos.control.guardar(nombre), guardada -> {
            if (!guardada) {
                JOptionPane.showMessageDialog(this, "Nombre invalido o ya existente. Intente otro.",
                        "Nombre no valido", JOptionPane.WARNING_MESSAGE);
                solicitarNombre();
            } else {
                informar("Grabacion guardada e indexada.");
                if (cerrarAlGuardar) {
                    cerrarAlGuardar = false;
                    cerrarRecursos();
                } else {
                    mostrarPagina(lista.inicio());
                }
            }
        });
    }

    private void mostrarPagina(long inicio) {
        ejecutar(() -> recursos.control.pagina(inicio), lista::mostrar);
    }

    private void cargarSeleccion() {
        String nombre = lista.nombreSeleccionado();
        if (nombre == null) {
            JOptionPane.showMessageDialog(this, "Seleccione una grabacion.");
            return;
        }
        ejecutar(() -> recursos.control.cargar(nombre), cargada -> {
            if (cargada) {
                informar("Grabacion cargada: " + nombre);
            } else {
                informar("No se encontro la grabacion seleccionada.");
            }
        });
    }

    private void iniciarReproduccion() {
        ejecutar(() -> recursos.control.reproducir(), resultado -> {
            reproduciendo = true;
            actualizarControles();
            informar("Reproduccion iniciada; las fuentes en vivo estan detenidas.");
            resultado.whenComplete((ignorado, error) -> SwingUtilities.invokeLater(() -> {
                if (!cerrando) {
                    finalizarReproduccion(error);
                }
            }));
        });
    }

    private void finalizarReproduccion(Throwable errorReproduccion) {
        trabajador.execute(() -> {
            Throwable fallo = errorReproduccion instanceof CompletionException
                    && errorReproduccion.getCause() != null
                    ? errorReproduccion.getCause() : errorReproduccion;
            try {
                recursos.control.parar();
            } catch (IOException | RuntimeException error) {
                if (fallo == null) {
                    fallo = error;
                } else {
                    fallo.addSuppressed(error);
                }
            }
            ControlEditor.Estado nuevoEstado = recursos.control.estado();
            Throwable resultado = fallo;
            SwingUtilities.invokeLater(() -> {
                if (cerrando) {
                    return;
                }
                estado = nuevoEstado;
                reproduciendo = false;
                if (resultado == null) {
                    informar("Reproduccion finalizada. Puede reiniciar los instrumentos.");
                } else if (resultado instanceof InterruptedException && resultado.getSuppressed().length == 0) {
                    informar("Reproduccion detenida. Puede reiniciar los instrumentos.");
                } else {
                    mostrarError("Fallo de reproduccion", resultado);
                }
                actualizarControles();
            });
        });
    }

    private void solicitarCierre() {
        if (ocupado) {
            informar("Espere a que termine la operacion actual antes de cerrar.");
            return;
        }
        if (recursos == null) {
            cerrarRecursos();
            return;
        }
        ejecutar(() -> recursos.control.prepararCierre(), resultado -> {
            if (resultado == ControlEditor.Cierre.PENDIENTE) {
                cerrarAlGuardar = true;
                solicitarNombre();
            } else {
                if (resultado == ControlEditor.Cierre.FALLIDA) {
                    JOptionPane.showMessageDialog(this,
                            "La sesion fallo. No se pudo garantizar el guardado; revise los archivos.",
                            "Sesion fallida", JOptionPane.ERROR_MESSAGE);
                }
                cerrarRecursos();
            }
        });
    }

    private void cerrarRecursos() {
        cerrando = true;
        ocupado = true;
        actualizarControles();
        actualizacionMonitor.stop();
        trabajador.execute(() -> {
            Throwable fallo = null;
            try {
                if (recursos != null) {
                    recursos.close();
                }
            } catch (IOException | RuntimeException error) {
                fallo = error;
            }
            Throwable resultado = fallo;
            SwingUtilities.invokeLater(() -> {
                if (resultado != null) {
                    mostrarError("Fallo al cerrar el editor", resultado);
                }
                trabajador.shutdown();
                dispose();
            });
        });
    }

    private <T> void ejecutar(Operacion<T> operacion, Consumer<T> alTerminar) {
        if (ocupado || cerrando) {
            return;
        }
        ocupado = true;
        actualizarControles();
        trabajador.execute(() -> {
            T resultado = null;
            Throwable fallo = null;
            try {
                resultado = operacion.ejecutar();
            } catch (IOException | PowerBandException | RuntimeException | LinkageError error) {
                fallo = error;
            }
            ControlEditor.Estado nuevoEstado = recursos == null ? null : recursos.control.estado();
            T valor = resultado;
            Throwable error = fallo;
            SwingUtilities.invokeLater(() -> {
                ocupado = false;
                estado = nuevoEstado;
                actualizarControles();
                if (error == null) {
                    alTerminar.accept(valor);
                    actualizarControles();
                } else {
                    cerrarAlGuardar = false;
                    mostrarError("No se pudo completar la operacion", error);
                }
            });
        });
    }

    private void actualizarControles() {
        boolean disponible = estado != null && !ocupado && !cerrando;
        boolean sana = disponible && !estado.fallida();
        boolean libre = sana && !estado.grabando() && !estado.pendiente() && !reproduciendo;
        grabar.setEnabled(libre);
        detenerGrabacion.setEnabled(sana && estado.grabando() && !reproduciendo);
        nombrar.setEnabled(sana && estado.pendiente() && !reproduciendo);
        reproducir.setEnabled(libre && estado.cargada());
        parar.setEnabled(disponible && reproduciendo);
        reiniciar.setEnabled(sana && !reproduciendo);
        lista.habilitar(libre);
        if (estado != null) {
            estadoTexto.setText(estado.fallida() ? "Sesion fallida: revise los archivos."
                    : ocupado ? "Operacion en curso..."
                    : estado.grabando() ? "Grabando"
                    : estado.pendiente() ? "Grabacion pendiente de nombre"
                    : reproduciendo ? "Reproduciendo"
                    : estado.cargada() ? "Grabacion cargada" : "Editor listo");
        }
    }

    private void informar(String mensaje) {
        if (mensajes.size() == 20) {
            mensajes.removeFirst();
        }
        mensajes.addLast(mensaje);
        avisos.setText(String.join("\n", mensajes));
        avisos.setCaretPosition(avisos.getDocument().getLength());
    }

    private void recibirAviso(String mensaje) {
        if (cerrando) {
            return;
        }
        informar(mensaje);
        if (recursos != null) {
            trabajador.execute(() -> {
                ControlEditor.Estado nuevoEstado = recursos.control.estado();
                SwingUtilities.invokeLater(() -> {
                    if (!cerrando) {
                        estado = nuevoEstado;
                        actualizarControles();
                    }
                });
            });
        }
    }

    private void mostrarError(String mensaje, Throwable error) {
        REGISTRO.log(Level.SEVERE, mensaje, error);
        informar(mensaje + ": " + error);
        JOptionPane.showMessageDialog(this, mensaje + ": " + error.getMessage(),
                "Error", JOptionPane.ERROR_MESSAGE);
    }

    @FunctionalInterface
    private interface Operacion<T> {
        T ejecutar() throws IOException, PowerBandException;
    }
}
