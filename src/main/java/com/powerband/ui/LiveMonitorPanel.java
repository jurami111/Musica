package com.powerband.ui;

import com.powerband.common.model.SynthCommand;
import java.awt.BorderLayout;
import java.util.Arrays;
import javax.swing.DefaultListModel;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.border.TitledBorder;

public class LiveMonitorPanel extends JPanel {
    private final DefaultListModel<String> lineas = new DefaultListModel<>();
    private final JLabel avisos = new JLabel("Sin comandos omitidos");
    private long omitidos;

    public LiveMonitorPanel() {
        super(new BorderLayout());
        setBorder(new TitledBorder("Comandos en vivo"));
        add(new JScrollPane(new JList<>(lineas)), BorderLayout.CENTER);
        add(avisos, BorderLayout.SOUTH);
    }

    void mostrar(ColaMonitor.Lote lote) {
        omitidos += lote.omitidos();
        if (omitidos > 0) {
            avisos.setText("Comandos no mostrados por sobrecarga: " + omitidos);
        }
        for (SynthCommand comando : lote.comandos()) {
            if (lineas.size() == ColaMonitor.LIMITE) {
                lineas.remove(0);
            }
            lineas.addElement("Tiempo: " + comando.obtenerTiempo() + " ns | Sintetizador: "
                    + comando.obtenerSintetizador() + " | Accion: " + comando.obtenerAccion()
                    + " | Parametros: " + Arrays.toString(comando.obtenerParametros()));
        }
    }
}
