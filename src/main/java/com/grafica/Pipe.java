package com.grafica;

public class Pipe {
    public float x;
    public float gapCentroY;
    // Ahora llevamos el control por separado
    public boolean puntuadaJ1;
    public boolean puntuadaJ2;

    public static final float ANCHO = 0.18f;
    public static final float GAP_ALTO = 0.48f;

    public Pipe(float x, float gapCentroY) {
        this.x = x;
        this.gapCentroY = gapCentroY;
        this.puntuadaJ1 = false;
        this.puntuadaJ2 = false;
    }

    public void actualizar(float dt, float velocidad) {
        this.x -= velocidad * dt;
    }
}
