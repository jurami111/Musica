package com.powerband.instrumentos;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.awt.image.BufferedImage;
import java.util.List;
import java.util.OptionalInt;
import org.junit.jupiter.api.Test;

class DetectoresInstrumentosTest {
    @Test
    void camaraRequiereDosCuadrosYTerminaAlPerderObjeto() {
        DetectorObjetoRojo detector = new DetectorObjetoRojo();
        BufferedImage izquierda = cuadro(0, 3, 0xff0000);
        BufferedImage derecha = cuadro(77, 80, 0xff0000);
        BufferedImage vacio = cuadro(0, 3, 0x205000);
        assertEquals(OptionalInt.empty(), detector.actualizar(izquierda));
        assertEquals(OptionalInt.of(60), detector.actualizar(izquierda));
        assertEquals(OptionalInt.of(60), detector.actualizar(derecha));
        assertEquals(OptionalInt.of(72), detector.actualizar(derecha));
        assertEquals(OptionalInt.of(72), detector.actualizar(vacio));
        assertEquals(OptionalInt.empty(), detector.actualizar(vacio));
    }

    @Test
    void camaraRechazaPocosPixelesYColoresQueNoCumplenUmbral() {
        DetectorObjetoRojo detector = new DetectorObjetoRojo();
        BufferedImage pocos = new BufferedImage(80, 60, BufferedImage.TYPE_INT_RGB);
        for (int x = 0; x < 23; x++) {
            pocos.setRGB(x, 0, 0xff0000);
        }
        detector.actualizar(pocos);
        assertEquals(OptionalInt.empty(), detector.actualizar(pocos));
        BufferedImage naranja = cuadro(0, 3, 0xffb000);
        detector.actualizar(naranja);
        assertEquals(OptionalInt.empty(), detector.actualizar(naranja));
    }

    @Test
    void camaraDistingueLasOchoFranjasYElMinimoDePixeles() {
        int[] notas = {60, 62, 64, 65, 67, 69, 71, 72};
        for (int franja = 0; franja < notas.length; franja++) {
            BufferedImage imagen = new BufferedImage(80, 60, BufferedImage.TYPE_INT_RGB);
            for (int y = 0; y < 24; y++) {
                imagen.setRGB(franja * 10, y, 0x645000);
            }
            DetectorObjetoRojo detector = new DetectorObjetoRojo();
            detector.actualizar(imagen);
            assertEquals(OptionalInt.empty(), detector.actualizar(imagen));
            for (int y = 0; y < 24; y++) {
                imagen.setRGB(franja * 10, y, 0x640000);
            }
            detector.actualizar(imagen);
            assertEquals(OptionalInt.of(notas[franja]), detector.actualizar(imagen));
        }
    }

    @Test
    void botonesSoloProducenNotasEnCadaNuevaPulsacion() {
        DetectorBotonesGamepad detector = new DetectorBotonesGamepad();
        boolean[] ambos = {true, false, false, true};
        assertEquals(List.of(36, 43), detector.actualizar(ambos));
        assertEquals(List.of(), detector.actualizar(ambos));
        assertEquals(List.of(38, 40), detector.actualizar(new boolean[] {false, true, true, false}));
        assertEquals(List.of(36), detector.actualizar(new boolean[] {true, false, false, false}));
        assertThrows(IllegalArgumentException.class, () -> detector.actualizar(new boolean[3]));
    }

    private BufferedImage cuadro(int inicio, int fin, int color) {
        BufferedImage imagen = new BufferedImage(80, 60, BufferedImage.TYPE_INT_RGB);
        for (int x = inicio; x < fin; x++) {
            for (int y = 0; y < 60; y++) {
                imagen.setRGB(x, y, color);
            }
        }
        return imagen;
    }
}
