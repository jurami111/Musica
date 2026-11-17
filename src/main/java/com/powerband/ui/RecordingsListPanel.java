package com.powerband.ui;

import com.powerband.common.model.RecordingMetadata;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.border.TitledBorder;

public class RecordingsListPanel extends JPanel {
    private final DefaultListModel<RecordingMetadata> registros = new DefaultListModel<>();
    private final JList<RecordingMetadata> lista = new JList<>(registros);
    private final JLabel pagina = new JLabel();
    final JButton anterior = new JButton("Anterior");
    final JButton siguiente = new JButton("Siguiente");
    final JButton cargar = new JButton("Cargar");
    private ControlEditor.Pagina actual = new ControlEditor.Pagina(0, 0, java.util.List.of());

    public RecordingsListPanel() {
        super(new BorderLayout());
        setBorder(new TitledBorder("Grabaciones"));
        lista.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        lista.setCellRenderer((list, valor, indice, seleccionado, foco) -> {
            JLabel texto = new JLabel(valor.obtenerNombre() + " | " + valor.obtenerFechaHora());
            texto.setOpaque(true);
            texto.setBackground(seleccionado ? list.getSelectionBackground() : list.getBackground());
            texto.setForeground(seleccionado ? list.getSelectionForeground() : list.getForeground());
            return texto;
        });
        add(new JScrollPane(lista), BorderLayout.CENTER);
        JPanel controles = new JPanel(new FlowLayout(FlowLayout.LEFT));
        controles.add(anterior);
        controles.add(siguiente);
        controles.add(pagina);
        controles.add(cargar);
        add(controles, BorderLayout.SOUTH);
    }

    void mostrar(ControlEditor.Pagina nueva) {
        actual = nueva;
        registros.clear();
        for (RecordingMetadata registro : nueva.registros()) {
            registros.addElement(registro);
        }
        long primero = nueva.cantidad() == 0 ? 0 : nueva.inicio() + 1;
        long ultimo = nueva.inicio() + nueva.registros().size();
        pagina.setText(primero + "-" + ultimo + " de " + nueva.cantidad());
    }

    String nombreSeleccionado() {
        RecordingMetadata registro = lista.getSelectedValue();
        return registro == null ? null : registro.obtenerNombre();
    }

    long inicio() {
        return actual.inicio();
    }

    void habilitar(boolean disponible) {
        anterior.setEnabled(disponible && actual.inicio() > 0);
        siguiente.setEnabled(disponible && actual.inicio() + actual.registros().size() < actual.cantidad());
        cargar.setEnabled(disponible && !actual.registros().isEmpty());
    }
}
