package com.powerband.common.constants;

public final class SynthConstants {

    public static final String DIRECCION_SUPERCOLLIDER = "127.0.0.1";
    public static final int PUERTO_OSC = 57110;
    public static final String MENSAJE_CREAR_SINTETIZADOR = "/s_new";
    public static final String MENSAJE_CAMBIAR_PARAMETROS = "/n_set";

    public static final String NOMBRE_PIANO = "classicPiano";
    public static final String NOMBRE_BAJO = "stringBass";
    public static final String NOMBRE_ORGANO = "organ70s";

    public static final int IDENTIFICADOR_PIANO = 1;
    public static final int IDENTIFICADOR_BAJO = 2;
    public static final int IDENTIFICADOR_ORGANO = 3;

    public static final int NOTA_MIDI_MINIMA = 0;
    public static final int NOTA_MIDI_MAXIMA = 127;
    public static final int PARAMETROS_PIANO = 3;
    public static final int PARAMETROS_BAJO = 2;
    public static final int PARAMETROS_INICIO_ORGANO = 7;
    public static final int PARAMETROS_FIN_ORGANO = 1;
    public static final int TERCERA_MENOR = 3;
    public static final int TERCERA_MAYOR = 4;

    private SynthConstants() {
    }
}