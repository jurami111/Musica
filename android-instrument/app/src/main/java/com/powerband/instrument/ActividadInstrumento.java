package com.powerband.instrument;

import android.app.Activity;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.text.InputType;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class ActividadInstrumento extends Activity implements SensorEventListener {
    private SensorManager sensores;
    private Sensor acelerometro;
    private Sensor giroscopio;
    private EditText direccion;
    private EditText puerto;
    private TextView estado;
    private Button boton;
    private volatile Socket conexion;
    private volatile float[] aceleracion;
    private volatile float[] giro;

    @Override
    public void onCreate(Bundle estadoGuardado) {
        super.onCreate(estadoGuardado);
        sensores = (SensorManager) getSystemService(SENSOR_SERVICE);
        acelerometro = sensores.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        giroscopio = sensores.getDefaultSensor(Sensor.TYPE_GYROSCOPE);

        LinearLayout pantalla = new LinearLayout(this);
        pantalla.setOrientation(LinearLayout.VERTICAL);
        int margen = (int) (16 * getResources().getDisplayMetrics().density);
        pantalla.setPadding(margen, margen, margen, margen);
        direccion = new EditText(this);
        direccion.setHint("Dirección del servidor");
        direccion.setSingleLine(true);
        pantalla.addView(direccion);
        puerto = new EditText(this);
        puerto.setHint("Puerto");
        puerto.setInputType(InputType.TYPE_CLASS_NUMBER);
        puerto.setText("5000");
        pantalla.addView(puerto);
        TextView instrucciones = new TextView(this);
        instrucciones.setText("Gira el móvil para cambiar la nota. Muévelo para tocar.");
        pantalla.addView(instrucciones);
        estado = new TextView(this);
        estado.setText("Desconectado");
        pantalla.addView(estado);
        boton = new Button(this);
        boton.setText("Conectar");
        boton.setOnClickListener(v -> {
            if (conexion == null) {
                conectar();
            } else {
                desconectar("Desconectado");
            }
        });
        pantalla.addView(boton, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        setContentView(pantalla);
    }

    private void conectar() {
        if (acelerometro == null || giroscopio == null) {
            estado.setText("El dispositivo necesita acelerómetro y giroscopio.");
            return;
        }
        String anfitrion = direccion.getText().toString().trim();
        String textoPuerto = puerto.getText().toString().trim();
        if (anfitrion.isEmpty() || !textoPuerto.matches("[0-9]{1,5}")) {
            estado.setText("Indica una dirección y un puerto válidos.");
            return;
        }
        int numeroPuerto = Integer.parseInt(textoPuerto);
        if (numeroPuerto < 1 || numeroPuerto > 65535) {
            estado.setText("El puerto debe estar entre 1 y 65535.");
            return;
        }

        Socket socket = new Socket();
        conexion = socket;
        aceleracion = null;
        giro = null;
        boton.setText("Desconectar");
        estado.setText("Conectando...");
        new Thread(() -> transmitir(socket, anfitrion, numeroPuerto), "sensores-tcp").start();
    }

    private void transmitir(Socket socket, String anfitrion, int numeroPuerto) {
        try {
            socket.connect(new InetSocketAddress(anfitrion, numeroPuerto), 5000);
            runOnUiThread(() -> {
                if (conexion != socket) {
                    return;
                }
                boolean leeAcelerometro = sensores.registerListener(
                        this, acelerometro, SensorManager.SENSOR_DELAY_GAME);
                boolean leeGiroscopio = sensores.registerListener(
                        this, giroscopio, SensorManager.SENSOR_DELAY_GAME);
                if (!leeAcelerometro || !leeGiroscopio) {
                    desconectar("No se pudieron activar ambos sensores.");
                } else {
                    estado.setText("Conectado");
                }
            });
            try (BufferedWriter salida = new BufferedWriter(new OutputStreamWriter(
                    socket.getOutputStream(), StandardCharsets.US_ASCII))) {
                while (conexion == socket && !socket.isClosed()) {
                    float[] a = aceleracion;
                    float[] g = giro;
                    if (a != null && g != null) {
                        salida.write(Float.toString(a[0]) + "," + Float.toString(a[1]) + ","
                                + Float.toString(a[2]) + "," + Float.toString(g[0]) + ","
                                + Float.toString(g[1]) + "," + Float.toString(g[2]) + "\n");
                        salida.flush();
                    }
                    Thread.sleep(50);
                }
            }
        } catch (IOException error) {
            runOnUiThread(() -> {
                if (conexion == socket) {
                    desconectar("Error de conexión: " + error.getMessage());
                }
            });
        } catch (InterruptedException error) {
            Thread.currentThread().interrupt();
            runOnUiThread(() -> {
                if (conexion == socket) {
                    desconectar("El envío de sensores fue interrumpido.");
                }
            });
        }
    }

    private void desconectar(String mensaje) {
        Socket socket = conexion;
        conexion = null;
        sensores.unregisterListener(this);
        aceleracion = null;
        giro = null;
        boton.setText("Conectar");
        estado.setText(mensaje);
        if (socket != null) {
            try {
                socket.close();
            } catch (IOException error) {
                estado.setText("Error al cerrar la conexión: " + error.getMessage());
            }
        }
    }

    @Override
    public void onSensorChanged(SensorEvent evento) {
        float[] valores = evento.values;
        if (!Float.isFinite(valores[0]) || !Float.isFinite(valores[1])
                || !Float.isFinite(valores[2])) {
            desconectar("El sensor produjo una lectura inválida.");
            return;
        }
        float[] muestra = {valores[0], valores[1], valores[2]};
        if (evento.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            aceleracion = muestra;
        } else if (evento.sensor.getType() == Sensor.TYPE_GYROSCOPE) {
            giro = muestra;
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int precision) {
    }

    @Override
    protected void onStop() {
        desconectar("Desconectado");
        super.onStop();
    }
}
