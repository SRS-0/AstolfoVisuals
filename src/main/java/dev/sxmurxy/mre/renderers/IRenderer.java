package dev.sxmurxy.mre.renderers;

import org.joml.Matrix4f;

public interface IRenderer {

    Matrix4f DEFAULT_MATRIX = new Matrix4f();

    default void render(double x, double y) {
        this.render((float) x, (float) y);
    }

    default void render(float x, float y) {
        this.render(DEFAULT_MATRIX, x, y);
    }

    default void render(Matrix4f matrix, double x, double y) {
        this.render(matrix, (float) x, (float) y);
    }

    default void render(Matrix4f matrix, float x, float y) {
        this.render(matrix, x, y, 0.0f);
    }

    default void render(double x, double y, double z) {
        this.render((float) x, (float) y, (float) z);
    }

    default void render(float x, float y, float z) {
        this.render(DEFAULT_MATRIX, x, y, z);
    }

    default void render(Matrix4f matrix, double x, double y, double z) {
        this.render(matrix, (float) x, (float) y, (float) z);
    }

    public void render(Matrix4f matrix, float x, float y, float z);

}