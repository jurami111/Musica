package com.powerband.common.constants;

import java.nio.file.Path;

public final class AppConstants {

    public static final String NOMBRE_ARCHIVO_GRABACIONES = "recordings.music";
    public static final String NOMBRE_ARCHIVO_INDICE = "recordings.index";
    public static final String NOMBRE_ARCHIVO_LISTA = "recordings.list";

    public static final Path DIRECTORIO_DATOS = Path.of("data");
    public static final Path DIRECTORIO_TEMPORAL = DIRECTORIO_DATOS.resolve("temp");
    public static final Path RUTA_GRABACIONES =
            DIRECTORIO_DATOS.resolve(NOMBRE_ARCHIVO_GRABACIONES);
    public static final Path RUTA_INDICE =
            DIRECTORIO_DATOS.resolve(NOMBRE_ARCHIVO_INDICE);
    public static final Path RUTA_LISTA =
            DIRECTORIO_DATOS.resolve(NOMBRE_ARCHIVO_LISTA);

    private AppConstants() {
    }
}