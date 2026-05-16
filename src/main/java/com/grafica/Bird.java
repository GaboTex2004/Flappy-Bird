package com.grafica;

public class Bird {
    public float x;
    public float y;
    public float velY;
    
    // Nuevas propiedades para el modo multijugador
    public boolean isDead;
    public int puntaje;
    public float r, g, b; // Color del cuerpo del pájaro
    
    public final float ancho = 0.10f;
    public final float alto = 0.10f;

    private static final float GRAVEDAD = -1.9f;
    private static final float IMPULSO_SALTO = 0.85f;
    private static final float VELOCIDAD_MAX_CAIDA = -1.8f;

    public Bird(float startX, float startY, float r, float g, float b) {
        this.x = startX;
        this.y = startY;
        this.velY = 0.0f;
        this.isDead = false;
        this.puntaje = 0;
        this.r = r;
        this.g = g;
        this.b = b;
    }

    public void saltar() {
        if (!isDead) {
            velY = IMPULSO_SALTO;
        }
    }

    public void actualizar(float dt) {
        if (isDead && y <= -0.75f) {
            y = -0.75f;
            velY = 0;
            return;
        }
        velY += GRAVEDAD * dt;
        if (velY < VELOCIDAD_MAX_CAIDA) {
            velY = VELOCIDAD_MAX_CAIDA;
        }
        y += velY * dt;

        if (isDead && y <= -0.75f) {
            y = -0.75f;
            velY = 0;
        }
    }

    public boolean fueraDeLimites() {
        return (y + alto * 0.5f >= 1.0f) || (y - alto * 0.5f <= -1.0f);
    }
}
