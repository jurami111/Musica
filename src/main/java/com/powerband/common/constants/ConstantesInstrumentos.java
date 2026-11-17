package com.powerband.common.constants;

public final class ConstantesInstrumentos {
    public static final int PUERTO_MOVIL = 5000;
    public static final int PUERTO_MAXIMO = 65535;
    public static final int MAXIMO_LINEA_SENSORES = 256;
    public static final int DURACION_NOTA_MS = 300;
    public static final int DISPOSITIVO_PREDETERMINADO = 0;
    public static final int NOTA_INACTIVA = -1;

    public static final int ANCHO_CAMARA = 80;
    public static final int ALTO_CAMARA = 60;
    public static final int CANALES_BGR = 3;
    public static final long INTERVALO_CAMARA_NS = 100_000_000L;
    public static final int ROJO_MINIMO = 100;
    public static final int PESO_ROJO = 2;
    public static final int PESO_OTRO_COLOR = 3;
    public static final int PIXELES_ROJOS_MINIMOS = 24;
    public static final int CUADROS_ESTABLES = 2;
    public static final long INTERVALO_GAMEPAD_MS = 20;

    public static final int NOTA_MOVIL_INICIAL = 60;
    public static final int NOTA_MOVIL_MINIMA = 48;
    public static final int NOTA_MOVIL_MAXIMA = 72;
    public static final double UMBRAL_GIRO_REARME = 0.5;
    public static final double UMBRAL_GIRO = 1.5;
    public static final double GRAVEDAD = 9.80665;
    public static final double UMBRAL_MOVIMIENTO_REARME = 2.0;
    public static final double UMBRAL_MOVIMIENTO = 4.0;
    public static final long INTERVALO_NOTAS_MOVIL_NS = 400_000_000L;

    private static final int[] NOTAS_CAMARA = {60, 62, 64, 65, 67, 69, 71, 72};
    private static final int[] NOTAS_GAMEPAD = {36, 38, 40, 43};

    private ConstantesInstrumentos() {
    }

    public static int cantidadNotasCamara() {
        return NOTAS_CAMARA.length;
    }

    public static int notaCamara(int posicion) {
        return NOTAS_CAMARA[posicion];
    }

    public static int cantidadBotonesGamepad() {
        return NOTAS_GAMEPAD.length;
    }

    public static int notaGamepad(int posicion) {
        return NOTAS_GAMEPAD[posicion];
    }
}
