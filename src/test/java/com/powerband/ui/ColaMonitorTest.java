package com.powerband.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.powerband.common.constants.BinaryConstants;
import com.powerband.common.constants.SynthConstants;
import com.powerband.common.exceptions.PowerBandException;
import com.powerband.common.model.SynthCommand;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import org.junit.jupiter.api.Test;

class ColaMonitorTest {
    @Test
    void limitaPendientesSinDescartarComandosDelEditor() throws Exception {
        List<SynthCommand> grabados = new ArrayList<>();
        ColaMonitor cola = new ColaMonitor(grabados::add);
        for (int numero = 0; numero < 250; numero++) {
            cola.observarComando(comando(numero));
        }
        ColaMonitor.Lote lote = cola.retirar();
        assertEquals(250, grabados.size());
        assertEquals(200, lote.comandos().size());
        assertEquals(50, lote.omitidos());
        assertEquals(50, lote.comandos().get(0).obtenerTiempo());
        assertEquals(249, lote.comandos().get(199).obtenerTiempo());
        assertTrue(cola.retirar().comandos().isEmpty());
    }

    @Test
    void informaErrorDelEditorSinPublicarUnComandoNoGrabado() {
        ColaMonitor cola = new ColaMonitor(comando -> {
            throw new PowerBandException("Fallo de grabacion");
        });
        assertThrows(PowerBandException.class, () -> cola.observarComando(comando(0)));
        assertTrue(cola.retirar().comandos().isEmpty());
    }

    @Test
    void vistaConservaSoloLosDoscientosMasRecientesEntreLotes() throws Exception {
        ColaMonitor cola = new ColaMonitor(comando -> { });
        for (int numero = 0; numero < 150; numero++) {
            cola.observarComando(comando(numero));
        }
        ColaMonitor.Lote primerLote = cola.retirar();
        for (int numero = 150; numero < 300; numero++) {
            cola.observarComando(comando(numero));
        }
        ColaMonitor.Lote segundoLote = cola.retirar();
        SwingUtilities.invokeAndWait(() -> {
            LiveMonitorPanel panel = new LiveMonitorPanel();
            panel.mostrar(primerLote);
            panel.mostrar(segundoLote);
            JScrollPane desplazamiento = (JScrollPane) panel.getComponent(0);
            JList<?> lista = (JList<?>) desplazamiento.getViewport().getView();
            assertEquals(200, lista.getModel().getSize());
            assertTrue(lista.getModel().getElementAt(0).toString().contains("Tiempo: 100 ns"));
            assertTrue(lista.getModel().getElementAt(199).toString().contains("Tiempo: 299 ns"));
        });
    }

    private static SynthCommand comando(long tiempo) {
        return new SynthCommand(tiempo, SynthConstants.IDENTIFICADOR_BAJO,
                BinaryConstants.ACCION_TOCAR, 36, 200);
    }
}
