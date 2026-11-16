package com.powerband.instrumentos;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.studiohartman.jamepad.ControllerManager;
import org.bytedeco.javacpp.Loader;
import org.bytedeco.opencv.global.opencv_videoio;
import org.junit.jupiter.api.Test;

class BibliotecasNativasTest {
    @Test
    void cargaBibliotecasEnMacArm64SinAbrirDispositivos() {
        assumeTrue(System.getProperty("os.name").startsWith("Mac")
                && System.getProperty("os.arch").equals("aarch64"));
        assertNotNull(Loader.load(opencv_videoio.class));
        assertNotNull(new ControllerManager());
    }
}
