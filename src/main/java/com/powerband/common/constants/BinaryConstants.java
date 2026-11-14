package com.powerband.common.constants;

public final class BinaryConstants {

    public static final int BYTES_ENTERO = Integer.BYTES;
    public static final int BYTES_ENTERO_LARGO = Long.BYTES;
    public static final int BYTES_DECIMAL = Double.BYTES;
    public static final int BYTES_CARACTER = Character.BYTES;

    public static final int BYTES_REGISTRO = 256;
    public static final int MAXIMO_UNIDADES_NOMBRE = 50;
    public static final int BYTES_NOMBRE = MAXIMO_UNIDADES_NOMBRE * BYTES_CARACTER;
    public static final int MAXIMO_PARAMETROS = 16;
    public static final int BYTES_PARAMETROS = MAXIMO_PARAMETROS * BYTES_DECIMAL;
    public static final byte VALOR_RELLENO = 0;

    public static final int TIPO_ENCABEZADO = 1;
    public static final int TIPO_COMANDO = 2;

    public static final int ACCION_TOCAR = 1;
    public static final int ACCION_INICIAR_NOTA = 2;
    public static final int ACCION_TERMINAR_NOTA = 3;

    public static final int BYTES_CAMPOS_ENCABEZADO =
            BYTES_ENTERO + BYTES_NOMBRE + BYTES_ENTERO_LARGO + BYTES_ENTERO_LARGO;
    public static final int BYTES_RELLENO_ENCABEZADO =
            BYTES_REGISTRO - BYTES_CAMPOS_ENCABEZADO;

    public static final int BYTES_CAMPOS_INDICE = BYTES_NOMBRE + BYTES_ENTERO_LARGO;
    public static final int BYTES_RELLENO_INDICE =
            BYTES_REGISTRO - BYTES_CAMPOS_INDICE;

    public static final int BYTES_CAMPOS_COMANDO =
            BYTES_ENTERO + BYTES_ENTERO_LARGO + BYTES_ENTERO + BYTES_ENTERO
                    + BYTES_ENTERO + BYTES_PARAMETROS;
    public static final int BYTES_RELLENO_COMANDO =
            BYTES_REGISTRO - BYTES_CAMPOS_COMANDO;

    private BinaryConstants() {
    }
}