package xyz.angames.astolfoclient.client.render.models;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;
import org.joml.Matrix4f;

@Environment(value=EnvType.CLIENT)
public class CowModel {
    private static final Identifier MODEL_LOCATION = Identifier.of((String)"astolfoclient", (String)"models/cow_mesh.json");
    private static final Identifier TEXTURE_LOCATION = Identifier.of((String)"astolfoclient", (String)"textures/models/cow.png");
    private static final float MODEL_SCALE = 0.48f;
    private final List<MeshTriangle> triangles = new ArrayList<MeshTriangle>();
    private boolean loaded;
    private boolean failed;
    private float centerX;
    private float centerZ;
    private float minY;
    private float bodyPivotY;

    public void render(MatrixStack matrixStack, VertexConsumerProvider vertexConsumers, PlayerEntityRenderState state, int light) {
        this.ensureLoaded();
        if (this.failed || this.triangles.isEmpty()) {
            return;
        }
        float ageInTicks = state.age;
        float walkAmount = Math.min(state.limbSwingAmplitude, 1.0f);
        float bob = (float)Math.sin(state.limbSwingAnimationProgress * 0.6662f) * walkAmount * 0.035f + (float)Math.sin(ageInTicks * 0.08f) * 0.01f;
        float sway = (float)Math.sin(ageInTicks * 0.05f) * 0.9f;
        float pitch = -2.0f + walkAmount * 5.5f + (float)Math.cos(ageInTicks * 0.07f) * 0.6f;
        VertexConsumer buffer = vertexConsumers.getBuffer(RenderLayers.entityTranslucent((Identifier)TEXTURE_LOCATION));
        matrixStack.push();
        matrixStack.translate(0.0, (double)bob, 0.0);
        matrixStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(state.bodyYaw));
        matrixStack.scale(0.48f, 0.48f, 0.48f);
        matrixStack.translate(this.centerX, this.bodyPivotY, this.centerZ);
        matrixStack.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(sway));
        matrixStack.multiply(RotationAxis.POSITIVE_X.rotationDegrees(pitch));
        matrixStack.translate(-this.centerX, -this.bodyPivotY, -this.centerZ);
        matrixStack.translate(-this.centerX, -this.minY, -this.centerZ);
        this.renderTriangles(matrixStack, buffer, light);
        matrixStack.pop();
    }

    private void ensureLoaded() {
        if (this.loaded || this.failed) {
            return;
        }
        try (InputStream stream = MinecraftClient.getInstance().getResourceManager().getResourceOrThrow(MODEL_LOCATION).getInputStream();
             InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8);){
            this.parse(JsonParser.parseReader((Reader)reader).getAsJsonObject());
            this.loaded = true;
        }
        catch (Exception e) {
            this.failed = true;
            System.err.println("Failed to load cow pet model from " + String.valueOf(MODEL_LOCATION));
            e.printStackTrace();
        }
    }

    private void parse(JsonObject root) {
        this.triangles.clear();
        JsonObject bounds = root.getAsJsonObject("bounds");
        JsonArray min = bounds.getAsJsonArray("min");
        JsonArray max = bounds.getAsJsonArray("max");
        float minX = min.get(0).getAsFloat();
        this.minY = min.get(1).getAsFloat();
        float minZ = min.get(2).getAsFloat();
        float maxX = max.get(0).getAsFloat();
        float maxY = max.get(1).getAsFloat();
        float maxZ = max.get(2).getAsFloat();
        this.centerX = (minX + maxX) * 0.5f;
        this.centerZ = (minZ + maxZ) * 0.5f;
        this.bodyPivotY = this.minY + (maxY - this.minY) * 0.52f;
        JsonArray trianglesArray = root.getAsJsonArray("triangles");
        for (int i = 0; i < trianglesArray.size(); ++i) {
            JsonArray triangleArray = trianglesArray.get(i).getAsJsonArray();
            if (triangleArray.size() < 3) continue;
            this.triangles.add(new MeshTriangle(this.readVertex(triangleArray.get(0).getAsJsonArray()), this.readVertex(triangleArray.get(1).getAsJsonArray()), this.readVertex(triangleArray.get(2).getAsJsonArray())));
        }
    }

    private MeshVertex readVertex(JsonArray vertexArray) {
        return new MeshVertex(vertexArray.get(0).getAsFloat(), vertexArray.get(1).getAsFloat(), vertexArray.get(2).getAsFloat(), vertexArray.get(3).getAsFloat(), vertexArray.get(4).getAsFloat());
    }

    private void renderTriangles(MatrixStack matrixStack, VertexConsumer buffer, int light) {
        Matrix4f matrix = matrixStack.peek().getPositionMatrix();
        for (MeshTriangle triangle : this.triangles) {
            this.putVertex(buffer, matrix, triangle.a, light);
            this.putVertex(buffer, matrix, triangle.b, light);
            this.putVertex(buffer, matrix, triangle.c, light);
        }
    }

    private void putVertex(VertexConsumer buffer, Matrix4f matrix, MeshVertex vertex, int light) {
        buffer.vertex(matrix, vertex.x, vertex.y, vertex.z).color(255, 255, 255, 255).texture(vertex.u, vertex.v).overlay(OverlayTexture.DEFAULT_UV).light(light).normal(0.0f, 1.0f, 0.0f);
    }

    @Environment(value=EnvType.CLIENT)
    private static final class MeshTriangle {
        private final MeshVertex a;
        private final MeshVertex b;
        private final MeshVertex c;

        private MeshTriangle(MeshVertex a, MeshVertex b, MeshVertex c) {
            this.a = a;
            this.b = b;
            this.c = c;
        }
    }

    @Environment(value=EnvType.CLIENT)
    private static final class MeshVertex {
        private final float x;
        private final float y;
        private final float z;
        private final float u;
        private final float v;

        private MeshVertex(float x, float y, float z, float u, float v) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.u = u;
            this.v = v;
        }
    }
}

