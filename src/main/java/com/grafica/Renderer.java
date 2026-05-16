package com.grafica;

import java.nio.FloatBuffer;
import java.util.List;

import org.lwjgl.BufferUtils;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;

public class Renderer {
    private long window;
    private int programa;
    
    private int vaoQuad, vboQuad;
    private int vaoTrianguloDer, vboTrianguloDer;
    private int vaoTrianguloUp, vboTrianguloUp;
    private int vaoTrianguloIzq, vboTrianguloIzq; // ARREGLO: Agregado vboTrianguloIzq
    
    // Geometría para el Sol (Círculo aproximado)
    private int vaoCirculo, vboCirculo;
    private final int SEGMENTOS_CIRCULO = 32; 
    private int uOffsetLocation, uScaleLocation, uColorLocation;

    private int programaTextura;
    private int uOffsetLocTex, uScaleLocTex;
    private int vaoTextura, vboTextura;

    public long initWindow(int ancho, int alto, String titulo) {
        if (!GLFW.glfwInit()) throw new IllegalStateException("Fallo al iniciar GLFW");

        GLFW.glfwDefaultWindowHints();
        // Configura OpenGL 3.3 en su versión "Core Profile"
        // Esto deshabilita todas las funciones obsoletas de versiones viejas de OpenGL
        GLFW.glfwWindowHint(GLFW.GLFW_VISIBLE, GLFW.GLFW_FALSE);
        GLFW.glfwWindowHint(GLFW.GLFW_RESIZABLE, GLFW.GLFW_FALSE);
        GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MAJOR, 3);
        GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MINOR, 3);
        GLFW.glfwWindowHint(GLFW.GLFW_OPENGL_PROFILE, GLFW.GLFW_OPENGL_CORE_PROFILE);

        window = GLFW.glfwCreateWindow(ancho, alto, titulo, 0, 0);
        GLFW.glfwMakeContextCurrent(window);
        GLFW.glfwSwapInterval(1);
        GLFW.glfwShowWindow(window);
        // IMPORTANTE: Inicializa las capacidades de OpenGL para que LWJGL pueda usar los comandos de la GPU
        GL.createCapabilities();

        crearShaders();
        crearGeometria();
        return window;
    }

    private void crearShaders() {
        String vertexSrc = """
            #version 330 core
            layout (location = 0) in vec3 aPos;
            uniform vec2 uOffset;
            uniform vec2 uScale;
            void main() {
                vec2 finalPos = aPos.xy * uScale + uOffset;
                gl_Position = vec4(finalPos, aPos.z, 1.0);
            }
            """;

        String fragmentSrc = """
            #version 330 core
            uniform vec3 uColor;
            out vec4 fragColor;
            void main() {
                fragColor = vec4(uColor, 1.0);
            }
            """;

        int vertexShader = GL20.glCreateShader(GL20.GL_VERTEX_SHADER);
        GL20.glShaderSource(vertexShader, vertexSrc);
        GL20.glCompileShader(vertexShader);

        int fragmentShader = GL20.glCreateShader(GL20.GL_FRAGMENT_SHADER);
        GL20.glShaderSource(fragmentShader, fragmentSrc);
        GL20.glCompileShader(fragmentShader);

        programa = GL20.glCreateProgram();
        GL20.glAttachShader(programa, vertexShader);
        GL20.glAttachShader(programa, fragmentShader);
        GL20.glLinkProgram(programa);

        uOffsetLocation = GL20.glGetUniformLocation(programa, "uOffset");
        uScaleLocation = GL20.glGetUniformLocation(programa, "uScale");
        uColorLocation = GL20.glGetUniformLocation(programa, "uColor");

        String vertexSrcTex = """
            #version 330 core
            layout (location = 0) in vec3 aPos;
            layout (location = 1) in vec2 aTexCoords;
            uniform vec2 uOffset;
            uniform vec2 uScale;
            out vec2 TexCoords;
            void main() {
                vec2 finalPos = aPos.xy * uScale + uOffset;
                gl_Position = vec4(finalPos, aPos.z, 1.0);
                // Invertimos Y para las texturas de AWT
                TexCoords = vec2(aTexCoords.x, 1.0 - aTexCoords.y);
            }
            """;

        String fragmentSrcTex = """
            #version 330 core
            in vec2 TexCoords;
            uniform sampler2D uTexture;
            out vec4 fragColor;
            void main() {
                vec4 texColor = texture(uTexture, TexCoords);
                if (texColor.a < 0.1) discard;
                fragColor = texColor;
            }
            """;

        int vShaderTex = GL20.glCreateShader(GL20.GL_VERTEX_SHADER);
        GL20.glShaderSource(vShaderTex, vertexSrcTex);
        GL20.glCompileShader(vShaderTex);

        int fShaderTex = GL20.glCreateShader(GL20.GL_FRAGMENT_SHADER);
        GL20.glShaderSource(fShaderTex, fragmentSrcTex);
        GL20.glCompileShader(fShaderTex);

        programaTextura = GL20.glCreateProgram();
        GL20.glAttachShader(programaTextura, vShaderTex);
        GL20.glAttachShader(programaTextura, fShaderTex);
        GL20.glLinkProgram(programaTextura);

        uOffsetLocTex = GL20.glGetUniformLocation(programaTextura, "uOffset");
        uScaleLocTex = GL20.glGetUniformLocation(programaTextura, "uScale");
    }

    private void crearGeometria() {
        // --- ARREGLO: Recibir arreglos [VAO, VBO] y asignarlos ---
        
        // Quad (Cuerpo)
        float[] quadVertices = {
            -0.5f, -0.5f, 0.0f,  
            0.5f, -0.5f, 0.0f,  
            0.5f,  0.5f, 0.0f,
            -0.5f, -0.5f, 0.0f,  
            0.5f,  0.5f, 0.0f, 
            -0.5f,  0.5f, 0.0f
        };
        int[] quadData = crearVao(quadVertices);
        vaoQuad = quadData[0];
        vboQuad = quadData[1];

        // Triángulo Derecha (Pico)
        float[] triDerVertices = {
            -0.5f,  0.5f, 0.0f, 
            -0.5f, -0.5f, 0.0f,  
             0.5f,  0.0f, 0.0f
        };
        int[] triDerData = crearVao(triDerVertices);
        vaoTrianguloDer = triDerData[0];
        vboTrianguloDer = triDerData[1];

        // Triángulo Arriba (Ala)
        float[] triUpVertices = {
            -0.5f, -0.5f, 0.0f,  
             0.5f, -0.5f, 0.0f,  
             0.0f,  0.5f, 0.0f
        };
        int[] triUpData = crearVao(triUpVertices);
        vaoTrianguloUp = triUpData[0];
        vboTrianguloUp = triUpData[1];

        // Triangulo Izquierda (Cola)
        float[] triBackVertices = {
            -0.5f,  0.5f, 0.0f,
             0.5f,  0.5f, 0.0f,
             0.5f, -0.5f, 0.0f,
        };
        int[] triIzqData = crearVao(triBackVertices);
        vaoTrianguloIzq = triIzqData[0];
        vboTrianguloIzq = triIzqData[1];

        // Círculo aproximado (Sol)
        int totalVertices = SEGMENTOS_CIRCULO + 2; 
        float[] circuloVertices = new float[totalVertices * 3]; 

        circuloVertices[0] = 0.0f;
        circuloVertices[1] = 0.0f;
        circuloVertices[2] = 0.0f;

        for (int i = 0; i <= SEGMENTOS_CIRCULO; i++) {
            double angulo = Math.toRadians((i * 360.0) / SEGMENTOS_CIRCULO);
            int index = (i + 1) * 3;
            circuloVertices[index] = (float) (0.5f * Math.cos(angulo)); 
            circuloVertices[index + 1] = (float) (0.5f * Math.sin(angulo)); 
            circuloVertices[index + 2] = 0.0f; 
        }
        int[] circuloData = crearVao(circuloVertices);
        vaoCirculo = circuloData[0];
        vboCirculo = circuloData[1];

        // Geometría Texturizada
        float[] texturedQuadVertices = {
            -0.5f, -0.5f, 0.0f, 0.0f, 0.0f,
             0.5f, -0.5f, 0.0f, 1.0f, 0.0f,
             0.5f,  0.5f, 0.0f, 1.0f, 1.0f,
            -0.5f, -0.5f, 0.0f, 0.0f, 0.0f,
             0.5f,  0.5f, 0.0f, 1.0f, 1.0f,
            -0.5f,  0.5f, 0.0f, 0.0f, 1.0f
        };
        vaoTextura = GL30.glGenVertexArrays();
        GL30.glBindVertexArray(vaoTextura);
        vboTextura = GL15.glGenBuffers();
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vboTextura);
        java.nio.FloatBuffer buffer = org.lwjgl.BufferUtils.createFloatBuffer(texturedQuadVertices.length);
        buffer.put(texturedQuadVertices).flip();
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, buffer, GL15.GL_STATIC_DRAW);
        GL20.glVertexAttribPointer(0, 3, GL11.GL_FLOAT, false, 5 * Float.BYTES, 0);
        GL20.glEnableVertexAttribArray(0);
        GL20.glVertexAttribPointer(1, 2, GL11.GL_FLOAT, false, 5 * Float.BYTES, 3 * Float.BYTES);
        GL20.glEnableVertexAttribArray(1);
    }

    // --- ARREGLO: Cambiar la firma a int[] para retornar ambos IDs ---
    private int[] crearVao(float[] vertices) {
        // 1. Generar y activar el VAO (Contenedor de configuración)
        int vao = GL30.glGenVertexArrays();
        GL30.glBindVertexArray(vao);
        
        // 2. Generar y activar el VBO (Cajón de memoria pura para los datos de los vértices)
        int vbo = GL15.glGenBuffers();
        GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vbo);
        
        // 3. Pasar los datos de Java a un Buffer nativo de memoria C (FloatBuffer) que la GPU entienda
        FloatBuffer buffer = BufferUtils.createFloatBuffer(vertices.length);
        buffer.put(vertices).flip();
        GL15.glBufferData(GL15.GL_ARRAY_BUFFER, buffer, GL15.GL_STATIC_DRAW); // Enviar datos a la GPU
        
        // 4. Explicar a OpenGL el formato de los datos (Puntero de Atributos)
        // Atributo 0: Posición de 3 floats (X, Y, Z). Cada vértice ocupa 3 * Float.BYTES en total.
        GL20.glVertexAttribPointer(0, 3, GL11.GL_FLOAT, false, 3 * Float.BYTES, 0);
        GL20.glEnableVertexAttribArray(0); // Habilitar el atributo de posición
        
        return new int[]{vao, vbo}; // Se retornan ambos IDs para poder limpiarlos al cerrar el juego
    }

    public void limpiarPantalla() {
        GL11.glClearColor(0.52f, 0.80f, 0.92f, 1.0f);
        GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);
        GL20.glUseProgram(programa);
    }

    private void dibujar(int vaoObjetivo, float x, float y, float ancho, float alto, float r, float g, float b, int tipoPrimitiva, int numVertices) {
        GL20.glUniform2f(uOffsetLocation, x, y);
        GL20.glUniform2f(uScaleLocation, ancho, alto);
        GL20.glUniform3f(uColorLocation, r, g, b);
        
        GL30.glBindVertexArray(vaoObjetivo);
        GL11.glDrawArrays(tipoPrimitiva, 0, numVertices);
    }

    public void dibujarTextura(Texture textura, float x, float y, float ancho, float alto) {
        GL20.glUseProgram(programaTextura);
        GL20.glUniform2f(uOffsetLocTex, x, y);
        GL20.glUniform2f(uScaleLocTex, ancho, alto);
        
        GL30.glActiveTexture(GL30.GL_TEXTURE0);
        textura.bind();
        
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        
        GL30.glBindVertexArray(vaoTextura);
        GL11.glDrawArrays(GL11.GL_TRIANGLES, 0, 6);
        
        GL11.glDisable(GL11.GL_BLEND);
        GL20.glUseProgram(programa); 
    }

    public void dibujarPajaro(Bird b) {
        //Cuerpo de Ave
        //si quiero hacerlo circular el cuerpo
        //int verticesSol = SEGMENTOS_CIRCULO + 2;
        dibujar(vaoQuad, b.x, b.y, b.ancho, b.alto, b.r, b.g, b.b, GL11.GL_TRIANGLE_FAN, 6);
        
        float picoAncho = b.ancho * 0.4f;
        dibujar(vaoTrianguloDer, b.x + (b.ancho/2) + (picoAncho/2), b.y, picoAncho, b.alto * 0.4f, 0.95f, 0.5f, 0.1f, GL11.GL_TRIANGLES, 3);
        
        float alaAlto = b.alto * 0.5f;
        if (b.velY > 0.1f) alaAlto = -alaAlto;
        dibujar(vaoTrianguloUp, b.x - (b.ancho * 0.1f), b.y, b.ancho * 0.6f, alaAlto, 0.95f, 0.95f, 0.95f, GL11.GL_TRIANGLES, 3);
        
        dibujar(vaoQuad, b.x + (b.ancho * 0.25f), b.y + (b.alto * 0.25f), b.ancho * 0.15f, b.ancho * 0.15f, 0.95f, 0.95f, 0.95f, GL11.GL_TRIANGLES, 6);
        
        dibujar(vaoQuad, b.x + (b.ancho * 0.27f), b.y + (b.alto * 0.2f), b.ancho * 0.05f, b.ancho * 0.05f, 0.1f, 0.1f, 0.1f, GL11.GL_TRIANGLES, 6 );
        
        float colaAncho = b.ancho * 0.6f;
        dibujar(vaoTrianguloIzq, b.x - (b.ancho/2) - (colaAncho/2) + 0.01f, b.y, colaAncho, b.alto * 0.6f, b.r * 0.8f, b.g * 0.8f, b.b * 0.8f, GL11.GL_TRIANGLES, 3);
    }

    public void dibujarTuberias(List<Pipe> tuberias) {
        float lipAlto = 0.08f; 
        float lipAncho = Pipe.ANCHO * 1.2f;

        for (Pipe t : tuberias) {
            float gapTop = t.gapCentroY + (Pipe.GAP_ALTO * 0.5f);
            float gapBottom = t.gapCentroY - (Pipe.GAP_ALTO * 0.5f);

            float altoSup = 1.0f - gapTop;
            if (altoSup > 0.0f) {
                dibujar(vaoQuad, t.x, gapTop + (altoSup * 0.5f), Pipe.ANCHO, altoSup, 0.18f, 0.70f, 0.25f, GL11.GL_TRIANGLES, 6);
                dibujar(vaoQuad, t.x - Pipe.ANCHO * 0.2f, gapTop + (altoSup * 0.5f), Pipe.ANCHO * 0.4f, altoSup, 0.10f, 0.55f, 0.20f, GL11.GL_TRIANGLES, 6);
                dibujar(vaoQuad, t.x, gapTop + (lipAlto * 0.5f), lipAncho, lipAlto, 0.25f, 0.80f, 0.35f, GL11.GL_TRIANGLES, 6);
            }

            float altoInf = gapBottom + 1.0f;
            if (altoInf > 0.0f){
                dibujar(vaoQuad, t.x, -1.0f + (altoInf * 0.5f), Pipe.ANCHO, altoInf, 0.18f, 0.70f, 0.25f, GL11.GL_TRIANGLES, 6); 
                dibujar(vaoQuad, t.x - Pipe.ANCHO * 0.2f, -1.0f + (altoInf * 0.5f), Pipe.ANCHO * 0.4f, altoInf, 0.10f, 0.55f, 0.20f, GL11.GL_TRIANGLES, 6); 
                dibujar(vaoQuad, t.x, gapBottom - (lipAlto * 0.5f), lipAncho, lipAlto, 0.25f, 0.80f, 0.35f, GL11.GL_TRIANGLES, 6);
            }
        }
    }

    public void dibujarFondo(){
        //sol
        int verticesSol = SEGMENTOS_CIRCULO + 2;
        dibujar(vaoCirculo, 0.7f, 0.7f, 0.2f, 0.2f, 0.99f, 0.90f, 0.20f, GL11.GL_TRIANGLE_FAN, verticesSol);
        //nube
        dibujar(vaoCirculo, -0.67f, 0.7f, 0.2f, 0.2f, 0.70f, 0.70f, 0.70f, GL11.GL_TRIANGLE_FAN, verticesSol);
        dibujar(vaoCirculo, -0.5f, 0.7f, 0.2f, 0.2f, 0.70f, 0.70f, 0.70f, GL11.GL_TRIANGLE_FAN, verticesSol);
        dibujar(vaoCirculo, -0.6f, 0.8f, 0.2f, 0.2f, 0.70f, 0.70f, 0.70f, GL11.GL_TRIANGLE_FAN, verticesSol);
        dibujar(vaoCirculo, -0.4f, 0.7f, 0.2f, 0.2f, 0.70f, 0.70f, 0.70f, GL11.GL_TRIANGLE_FAN, verticesSol);
        dibujar(vaoCirculo, -0.53f, 0.6f, 0.2f, 0.2f, 0.70f, 0.70f, 0.70f, GL11.GL_TRIANGLE_FAN, verticesSol);
        //montañas
        dibujar(vaoTrianguloUp, -0.6f, -0.4f, 1.0f, 0.8f, 0.5f, 0.5f, 0.55f, GL11.GL_TRIANGLES, 3);
        dibujar(vaoTrianguloUp, 0.0f, -0.5f, 1.2f, 1.0f, 0.45f, 0.45f, 0.5f, GL11.GL_TRIANGLES, 3);
        dibujar(vaoTrianguloUp, 0.6f, -0.45f, 1.0f, 0.9f, 0.5f, 0.5f, 0.55f, GL11.GL_TRIANGLES, 3);
        //direccion de suelo
        float sueloY = -0.9f; 
        //arbol1
        dibujar(vaoQuad, -0.7f, sueloY + 0.1f, 0.05f, 0.2f, 0.4f, 0.25f, 0.1f, GL11.GL_TRIANGLES, 6); 
        dibujar(vaoTrianguloUp, -0.7f, sueloY + 0.25f, 0.2f, 0.3f, 0.1f, 0.5f, 0.1f, GL11.GL_TRIANGLES, 3); 
        //arbol2
        dibujar(vaoQuad, 0.2f, sueloY + 0.15f, 0.06f, 0.3f, 0.4f, 0.25f, 0.1f, GL11.GL_TRIANGLES, 6); 
        dibujar(vaoTrianguloUp, 0.2f, sueloY + 0.35f, 0.25f, 0.4f, 0.1f, 0.5f, 0.1f, GL11.GL_TRIANGLES, 3); 
        //suelo
        dibujar(vaoQuad, 0.0f, -0.9f, 2.0f, 0.2f, 0.5f, 0.35f, 0.1f, GL11.GL_TRIANGLES, 6);
        dibujar(vaoQuad, 0.0f, -0.81f, 2.0f, 0.02f, 0.1f, 0.6f, 0.1f, GL11.GL_TRIANGLES, 6);
    }

    public void dibujarOverlayMuerte() {
        dibujar(vaoQuad, 0.0f, 0.0f, 2.0f, 0.40f, 0.15f, 0.18f, 0.22f, GL11.GL_TRIANGLES, 6);
    }

    private void dibujarSegmento(int seg, float x, float y, float w, float h, float r, float g, float b) {
        float thickness = Math.min(w, h) * 0.2f;
        float halfW = w / 2;
        float halfH = h / 2;
        switch(seg) {
            case 0: dibujar(vaoQuad, x, y + halfH, w, thickness, r, g, b, GL11.GL_TRIANGLES, 6); break;
            case 1: dibujar(vaoQuad, x - halfW, y + halfH/2, thickness, halfH, r, g, b, GL11.GL_TRIANGLES, 6); break;
            case 2: dibujar(vaoQuad, x + halfW, y + halfH/2, thickness, halfH, r, g, b, GL11.GL_TRIANGLES, 6); break;
            case 3: dibujar(vaoQuad, x, y, w, thickness, r, g, b, GL11.GL_TRIANGLES, 6); break;
            case 4: dibujar(vaoQuad, x - halfW, y - halfH/2, thickness, halfH, r, g, b, GL11.GL_TRIANGLES, 6); break;
            case 5: dibujar(vaoQuad, x + halfW, y - halfH/2, thickness, halfH, r, g, b, GL11.GL_TRIANGLES, 6); break;
            case 6: dibujar(vaoQuad, x, y - halfH, w, thickness, r, g, b, GL11.GL_TRIANGLES, 6); break;
        }
    }

    private void dibujarNumeroDigito(int digito, float x, float y, float w, float h, float r, float g, float b) {
        boolean[] segs = new boolean[7];
        switch(digito) {
            case 0: segs=new boolean[]{true,true,true,false,true,true,true}; break;
            case 1: segs=new boolean[]{false,false,true,false,false,true,false}; break;
            case 2: segs=new boolean[]{true,false,true,true,true,false,true}; break;
            case 3: segs=new boolean[]{true,false,true,true,false,true,true}; break;
            case 4: segs=new boolean[]{false,true,true,true,false,true,false}; break;
            case 5: segs=new boolean[]{true,true,false,true,false,true,true}; break;
            case 6: segs=new boolean[]{true,true,false,true,true,true,true}; break;
            case 7: segs=new boolean[]{true,false,true,false,false,true,false}; break;
            case 8: segs=new boolean[]{true,true,true,true,true,true,true}; break;
            case 9: segs=new boolean[]{true,true,true,true,false,true,true}; break;
        }
        for(int i=0; i<7; i++) {
            if(segs[i]) dibujarSegmento(i, x, y, w, h, r, g, b);
        }
    }

    private void dibujarNumero(int num, float x, float y, float w, float h, float r, float g, float b) {
        if (num == 0) {
            dibujarNumeroDigito(0, x, y, w, h, r, g, b);
            return;
        }
        int temp = num;
        int digits = 0;
        while(temp > 0) { digits++; temp /= 10; }

        float startX = x + (digits - 1) * (w * 0.7f);
        temp = num;
        for(int i=0; i<digits; i++) {
            dibujarNumeroDigito(temp % 10, startX - i*(w*1.4f), y, w, h, r, g, b);
            temp /= 10;
        }
    }

    public void dibujarPuntajes(int p1, int p2) {
        dibujarNumero(p1, -0.8f, 0.8f, 0.1f, 0.15f, 0.98f, 0.85f, 0.20f);
        dibujarNumero(p2, 0.8f, 0.8f, 0.1f, 0.15f, 0.20f, 0.60f, 0.98f);
    }

    public void dibujarMenuInicio(int opcion) {
        dibujar(vaoTrianguloDer, 0.0f, 0.2f, 0.3f, 0.3f, 0.2f, 0.8f, 0.2f, GL11.GL_TRIANGLES, 3);
        dibujar(vaoQuad, 0.0f, -0.3f, 0.25f, 0.25f, 0.8f, 0.2f, 0.2f, GL11.GL_TRIANGLES, 6);
        if (opcion == 0) {
            dibujar(vaoTrianguloDer, -0.3f, 0.2f, 0.1f, 0.1f, 1.0f, 1.0f, 1.0f, GL11.GL_TRIANGLES, 3);
        } else {
            dibujar(vaoTrianguloDer, -0.3f, -0.3f, 0.1f, 0.1f, 1.0f, 1.0f, 1.0f, GL11.GL_TRIANGLES, 3);
        }
    }

    public void dibujarMenuPausa() {
        dibujar(vaoQuad, 0.0f, 0.0f, 0.4f, 0.4f, 0.2f, 0.2f, 0.2f, GL11.GL_TRIANGLES, 6);
        dibujar(vaoQuad, -0.05f, 0.0f, 0.05f, 0.2f, 1.0f, 1.0f, 1.0f, GL11.GL_TRIANGLES, 6);
        dibujar(vaoQuad, 0.05f, 0.0f, 0.05f, 0.2f, 1.0f, 1.0f, 1.0f, GL11.GL_TRIANGLES, 6);
    }

    public void cleanup() {
        // --- ARREGLO: Limpieza correcta de VAOs y VBOs ---
        GL30.glDeleteVertexArrays(vaoQuad); GL15.glDeleteBuffers(vboQuad);
        GL30.glDeleteVertexArrays(vaoTrianguloUp); GL15.glDeleteBuffers(vboTrianguloUp);
        GL30.glDeleteVertexArrays(vaoTrianguloIzq); GL15.glDeleteBuffers(vboTrianguloIzq); // Faltaba su VBO
        GL30.glDeleteVertexArrays(vaoTrianguloDer); GL15.glDeleteBuffers(vboTrianguloDer);
        GL30.glDeleteVertexArrays(vaoCirculo); GL15.glDeleteBuffers(vboCirculo);

        // Limpiar los búferes de texturas también
        GL30.glDeleteVertexArrays(vaoTextura);
        GL15.glDeleteBuffers(vboTextura);

        GL20.glDeleteProgram(programa);
        GL20.glDeleteProgram(programaTextura);
        
        GLFW.glfwDestroyWindow(window);
        GLFW.glfwTerminate();
    }
}