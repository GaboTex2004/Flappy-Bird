package com.grafica;

import org.lwjgl.glfw.GLFW;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

public class Game {
    private Renderer renderer;
    private InputManager input;
    private long window;

    private Bird jugador1;
    private Bird jugador2;
    private Bird jugador3;
    private List<Pipe> tuberias;
    private Random random;

    public static final int ESTADO_MENU = 0;
    public static final int ESTADO_JUGANDO = 1;
    public static final int ESTADO_PAUSA = 2;
    public static final int ESTADO_GAMEOVER = 3;
    private int estadoActual = ESTADO_MENU;
    private int opcionMenuSeleccionada = 0; // 0 = Jugar, 1 = Salir

    private float timerSpawn;

    private float velocidadActual;
    private static final float VELOCIDAD_BASE = 0.62f;
    private static final float TIEMPO_ENTRE_TUBERIAS = 1.5f;

    private Texture texFondoMenu;
    private Texture texNivelActual;
    private Texture juego;
    private int nivelActual;

    public Game() {
        renderer = new Renderer();
        window = renderer.initWindow(900, 700, "Flappy Bird Geometrico");
        input = new InputManager(window);
        tuberias = new ArrayList<>();
        random = new Random();

        // Cargar texturas
        texFondoMenu = new Texture("Fondo menu.jfif");
        nivelActual = 1;
        texNivelActual = Texture.generarTexto("NIVEL 1", 32, java.awt.Color.WHITE, true);

        reset();
        estadoActual = ESTADO_MENU; // Iniciamos en menu
    }
    
    private void reset() {
        // J1 Amarillo
        jugador1 = new Bird(-0.45f, 0.4f, 0.98f, 0.85f, 0.20f); 
        // J2 Azul/Celeste, un poco más atrás para que no se superpongan exactamente al inicio
        jugador2 = new Bird(-0.55f, 0.3f, 0.20f, 0.60f, 0.98f);
        jugador3 = new Bird(-0.35f, 0.5f, 0.80f, 0.50f, 0.98f);

        velocidadActual = VELOCIDAD_BASE;
        tuberias.clear();
        timerSpawn = 0.0f;
        actualizarTitulo();
    }

    public void run() {
        float ultimoTiempo = (float) GLFW.glfwGetTime();
        while (!GLFW.glfwWindowShouldClose(window)) {
            float ahora = (float) GLFW.glfwGetTime();
            float dt = ahora - ultimoTiempo; // dt = Delta Time (tiempo que tardó en renderizarse el frame anterior)
            ultimoTiempo = ahora;
            
            // GUARDA DE SEGURIDAD: Si el juego se traba (lag), el dt crece mucho. 
            // Limitarlo a 0.033f evita que las físicas "atraviesen" las paredes por un pico de lag.
            if (dt > 0.033f) dt = 0.033f; 

            // Los 3 pilares de cualquier ciclo de juego:
            procesarEntrada();   // 1. Escuchar al jugador
            actualizarLogica(dt); // 2. Mover objetos, calcular físicas y colisiones
            renderizar();        // 3. Dibujar todo en pantalla

            GLFW.glfwSwapBuffers(window); // Intercambia el buffer de dibujo con el de pantalla (evita parpadeos)
            GLFW.glfwPollEvents();        // Procesa eventos del sistema (teclado, mouse, ventana)
        }
        // GESTIÓN DE MEMORIA: Al salir del loop, hay que liberar la memoria de la GPU (texturas y ventana)
        if (texFondoMenu != null) texFondoMenu.cleanup();
        if (texNivelActual != null) texNivelActual.cleanup();
        renderer.cleanup();
    }

    private void procesarEntrada() {
        if (input.isEscapePressed()) GLFW.glfwSetWindowShouldClose(window, true);

        if (estadoActual == ESTADO_MENU) {
            if (input.isWOrUpPressed()) {
                opcionMenuSeleccionada = 0;
            }
            if (input.isDownPressed()) {
                opcionMenuSeleccionada = 1;
            }
            if (input.isSpacePressed() || input.isEnterPressed()) {
                if (opcionMenuSeleccionada == 0) {
                    reset();
                    estadoActual = ESTADO_JUGANDO;
                } else {
                    GLFW.glfwSetWindowShouldClose(window, true);
                }
            }
        } else if (estadoActual == ESTADO_JUGANDO) {
            if (input.isEnterPressed()) {
                estadoActual = ESTADO_PAUSA;
                return;
            }
            if (input.isSpacePressed() && !jugador1.isDead) jugador1.saltar();
            if (input.isWOrUpPressed() && !jugador2.isDead) jugador2.saltar();
            if (input.isTPressed() && !jugador3.isDead) jugador3.saltar();
        } else if (estadoActual == ESTADO_PAUSA) {
            if (input.isSpacePressed() || input.isEnterPressed()) {
                estadoActual = ESTADO_JUGANDO;
            }
        } else if (estadoActual == ESTADO_GAMEOVER) {
            if (input.isRPressed() || input.isSpacePressed() || input.isEnterPressed()) {
                estadoActual = ESTADO_MENU;
            }
        }
    }

    private void actualizarLogica(float dt) {
        if (estadoActual == ESTADO_MENU || estadoActual == ESTADO_PAUSA) {
            return;
        }

        // Físicas (Incluso si están muertos, deben caer hasta el suelo)
        jugador1.actualizar(dt);
        if (!jugador1.isDead && jugador1.fueraDeLimites()) jugador1.isDead = true;
        
        jugador2.actualizar(dt);
        if (!jugador2.isDead && jugador2.fueraDeLimites()) {
            jugador2.isDead = true;
        }
        jugador3.actualizar(dt);
        if (!jugador3.isDead && jugador3.fueraDeLimites()) {
            jugador3.isDead = true;
        }
        if(jugador2.isDead&& jugador2.x>-1.75f){
            jugador2.x-=0.004f;
        }
        if(jugador1.isDead&& jugador1.x>-1.75f){
            jugador1.x-=0.004f;
        }
        
        if (jugador1.isDead && jugador2.isDead &&jugador3.isDead && estadoActual != ESTADO_GAMEOVER) {
            estadoActual = ESTADO_GAMEOVER;
            actualizarTitulo();
        }

        if (estadoActual == ESTADO_GAMEOVER) {
            return;
        }

        timerSpawn += dt;
        if (timerSpawn >= TIEMPO_ENTRE_TUBERIAS) {
            timerSpawn = 0.0f;
            float gapCentro = -0.45f + random.nextFloat() * 0.9f;
            tuberias.add(new Pipe(1.2f, gapCentro));
        }

        Iterator<Pipe> it = tuberias.iterator();
        while (it.hasNext()) {
            Pipe t = it.next();
            t.actualizar(dt, velocidadActual);

            if (!jugador1.isDead && t.x + (Pipe.ANCHO / 2) < jugador1.x && !t.puntuadaJ1) {
                jugador1.puntaje++;
                t.puntuadaJ1 = true;
                actualizarTitulo(); 
            }
            
            if (!jugador2.isDead && t.x + (Pipe.ANCHO / 2) < jugador2.x && !t.puntuadaJ2) {
                jugador2.puntaje++;
                t.puntuadaJ2 = true;
                actualizarTitulo(); 
            }
            if (!jugador3.isDead && t.x + (Pipe.ANCHO / 2) < jugador3.x && !t.puntuadaJ2) {
                jugador3.puntaje++;
                t.puntuadaJ2 = true;
                actualizarTitulo(); 
            }

            if (!jugador1.isDead && colisiona(jugador1, t)) jugador1.isDead = true;
            if (!jugador2.isDead && colisiona(jugador2, t)) jugador2.isDead = true;
            if (!jugador3.isDead && colisiona(jugador3, t)) jugador3.isDead = true;
            if (t.x + (Pipe.ANCHO / 2) < -1.3f) it.remove();
        }

        int maxPuntuacion = Math.max(jugador1.puntaje, jugador2.puntaje);
        int nuevoNivel = 1 + (maxPuntuacion / 5);
        
        if (nuevoNivel != nivelActual) {
            nivelActual = nuevoNivel;
            if (texNivelActual != null) texNivelActual.cleanup();
            texNivelActual = Texture.generarTexto("NIVEL " + nivelActual, 32, java.awt.Color.WHITE, true);
        }
        if(nivelActual%3!=0){
            velocidadActual = VELOCIDAD_BASE + (nivelActual - 1) * 0.15f;
        }else{
            velocidadActual = VELOCIDAD_BASE;
        }
    }

    private boolean colisiona(Bird b, Pipe t) {
        // 1. Calcular los límites (bordes) de la caja del pájaro
        float birdLeft = b.x - (b.ancho / 2);
        float birdRight = b.x + (b.ancho / 2);
        float birdBottom = b.y - (b.alto / 2);
        float birdTop = b.y + (b.alto / 2);

        // 2. Calcular los límites horizontales del tubo
        float pipeLeft = t.x - (Pipe.ANCHO / 2);
        float pipeRight = t.x + (Pipe.ANCHO / 2);
        
        // 3. Verificar colisión en el eje X (¿El pájaro está cruzando el tubo horizontalmente?)
        boolean overlapX = birdRight > pipeLeft && birdLeft < pipeRight;
        if (!overlapX) return false; // Si ni siquiera coincide en X, no hay colisión.

        // 4. Calcular los límites del "Gap" (el hueco por donde pasa el pájaro)
        float gapTop = t.gapCentroY + (Pipe.GAP_ALTO / 2);
        float gapBottom = t.gapCentroY - (Pipe.GAP_ALTO / 2);
        
        // 5. Retorna TRUE si el pájaro toca el tubo de arriba O el tubo de abajo
        return birdTop > gapTop || birdBottom < gapBottom;
    }

    private void renderizar() {
        renderer.limpiarPantalla();
        renderer.dibujarFondo();
        
        if (estadoActual == ESTADO_MENU) {
            // Dibujar fondo de menú usando la imagen
            renderer.dibujarTextura(texFondoMenu, 0.0f, 0.0f, 2.0f, 2.0f);
            
            // Dibujar la interfaz geométrica del menú encima
            renderer.dibujarMenuInicio(opcionMenuSeleccionada);
        } else {
            renderer.dibujarTuberias(tuberias);
            renderer.dibujarPajaro(jugador1);
            renderer.dibujarPajaro(jugador2);
            renderer.dibujarPajaro(jugador3);
            renderer.dibujarPuntajes(jugador1.puntaje, jugador2.puntaje, jugador3.puntaje);
            
            // Dibujar nivel en el centro superior
            renderer.dibujarTextura(texNivelActual, 0.0f, 0.8f, 0.35f, 0.15f);
            
            if (estadoActual == ESTADO_PAUSA) {
                renderer.dibujarMenuPausa();
            } else if (estadoActual == ESTADO_GAMEOVER) {
                renderer.dibujarOverlayMuerte();
                if(jugador1.puntaje > jugador2.puntaje&&jugador1.puntaje > jugador3.puntaje)
                    juego = Texture.generarTexto("Gano jugador 1 gano con "+jugador1.puntaje+" puntos", 32, java.awt.Color.WHITE, true);
                else if(jugador2.puntaje > jugador1.puntaje && jugador2.puntaje > jugador3.puntaje)
                    juego = Texture.generarTexto("Gano jugador 2 gano con "+jugador2.puntaje+" puntos", 32, java.awt.Color.WHITE, true);
                else if(jugador3.puntaje > jugador2.puntaje && jugador3.puntaje > jugador1.puntaje)
                    juego = Texture.generarTexto("Gano jugador 3 gano con "+jugador3.puntaje+" puntos", 32, java.awt.Color.WHITE, true);
                else 
                    juego = Texture.generarTexto("Empate", 32, java.awt.Color.WHITE, true);
                renderer.dibujarTextura(juego, 0.0f, 0.0f, 2f, 0.32f);
            }else if(jugador1.puntaje == 5 ||jugador2.puntaje == 5 || jugador3.puntaje == 5 ){
                estadoActual = ESTADO_GAMEOVER;
            }
        }
    }

    private void actualizarTitulo() {
        if (jugador1 == null || jugador2 == null || jugador3==null) return;
        String titulo = "J1 (Amarillo): " + jugador1.puntaje + " | J2 (Azul): " + jugador2.puntaje + "| j3(violeta): "+ jugador3.puntaje;
        if (estadoActual == ESTADO_MENU) GLFW.glfwSetWindowTitle(window, "Flappy Bird Geométrico - Menú");
        else if (estadoActual == ESTADO_GAMEOVER) GLFW.glfwSetWindowTitle(window, titulo + " | GAME OVER");
        else if (estadoActual == ESTADO_PAUSA) GLFW.glfwSetWindowTitle(window, titulo + " | PAUSA");
        else GLFW.glfwSetWindowTitle(window, titulo);
    }
}