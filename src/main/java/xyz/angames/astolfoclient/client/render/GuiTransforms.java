package xyz.angames.astolfoclient.client.render;

import org.joml.Matrix3x2fc;
import org.joml.Matrix4f;

public final class GuiTransforms {
    private GuiTransforms() {}
    public static Matrix4f matrix(Matrix3x2fc source) {
        return new Matrix4f().m00(source.m00()).m01(source.m01()).m10(source.m10()).m11(source.m11()).m30(source.m20()).m31(source.m21());
    }
}
