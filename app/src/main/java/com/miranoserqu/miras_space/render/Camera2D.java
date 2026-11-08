package com.miranoserqu.miras_space.render;

import com.miranoserqu.miras_space.Option;
import org.joml.Matrix4f;

public class Camera2D {

    // Relación de conversión: 1 unidad del mundo = 100 píxeles base
    public static final float PIXELS_PER_UNIT = 50.0f;

    private float x = 0.0f; // Posición de la cámara en el mundo (en unidades)
    private float y = 0.0f;

    private final Matrix4f projectionMatrix = new Matrix4f();
    private final Matrix4f viewMatrix = new Matrix4f();
    private final Matrix4f combinedMatrix = new Matrix4f();

    /**
     * Actualiza la matriz ortográfica basada en el tamaño real del Viewport
     * @param windowWidth Ancho en píxeles de la ventana
     * @param windowHeight Alto en píxeles de la ventana
     */
    public void updateMatrices(int windowWidth, int windowHeight) {
        // 1. Calcular cuántas unidades entran en pantalla según la altura y el zoom
        float unitsInHeight = (float) ((windowHeight / PIXELS_PER_UNIT) / Option.ZOOM.get().getValue());
        float aspectRatio = (float) windowWidth / (float) windowHeight;
        float unitsInWidth = unitsInHeight * aspectRatio;

        // 2. Definir los límites ortográficos centrados en (0,0) de la cámara
        float halfWidth = unitsInWidth / 2.0f;
        float halfHeight = unitsInHeight / 2.0f;

        // Proyección ortográfica: (left, right, bottom, top, zNear, zFar)
        projectionMatrix.identity().ortho(
                -halfWidth, halfWidth,
                -halfHeight, halfHeight,
                -1.0f, 1.0f
        );

        // 3. Matriz de vista (traslada la cámara a la posición x, y)
        viewMatrix.identity().translate(-x, -y, 0.0f);

        // 4. Combinar matrices: Proyección * Vista
        projectionMatrix.mul(viewMatrix, combinedMatrix);
    }

    public void setPosition(float x, float y) {
        this.x = x;
        this.y = y;
    }

    public Matrix4f getCombinedMatrix() {
        return combinedMatrix;
    }
}
