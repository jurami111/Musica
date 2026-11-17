package com.powerband.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.powerband.common.model.RecordingMetadata;
import java.time.Instant;
import java.util.List;
import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import org.junit.jupiter.api.Test;

class RecordingsListPanelTest {
    @Test
    void seleccionaElNombreYHabilitaNavegacionSegunLaPagina() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            RecordingsListPanel panel = new RecordingsListPanel();
            panel.mostrar(new ControlEditor.Pagina(0, 51, List.of(
                    new RecordingMetadata("Primera", Instant.EPOCH))));
            panel.habilitar(true);
            JScrollPane desplazamiento = (JScrollPane) panel.getComponent(0);
            JList<?> lista = (JList<?>) desplazamiento.getViewport().getView();
            lista.setSelectedIndex(0);
            assertEquals("Primera", panel.nombreSeleccionado());
            assertFalse(panel.anterior.isEnabled());
            assertTrue(panel.siguiente.isEnabled());
            assertTrue(panel.cargar.isEnabled());

            panel.mostrar(new ControlEditor.Pagina(50, 51, List.of(
                    new RecordingMetadata("Ultima", Instant.EPOCH))));
            panel.habilitar(true);
            assertTrue(panel.anterior.isEnabled());
            assertFalse(panel.siguiente.isEnabled());
            panel.habilitar(false);
            assertFalse(panel.cargar.isEnabled());
        });
    }
}
