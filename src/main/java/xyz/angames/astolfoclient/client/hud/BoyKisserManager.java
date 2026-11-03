package xyz.angames.astolfoclient.client.hud;

import java.util.ArrayList;
import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.util.Identifier;
import org.joml.Matrix4f;

@Environment(value=EnvType.CLIENT)
public class BoyKisserManager {
    private final MinecraftClient client = MinecraftClient.getInstance();
    private final List<Identifier> frames = new ArrayList<Identifier>();
    private int currentFrame = 0;
    private long lastFrameTime = 0L;
    private final int frameDelay = 100;
    private double x = 10.0;
    private double y = 50.0;
    private final double width = 64.0;
    private final double height = 64.0;
    private boolean isDragging = false;
    private double dragX;
    private double dragY;

    public BoyKisserManager() {
        int frameCount = 52;
        for (int i = 0; i < frameCount; ++i) {
            this.frames.add(Identifier.of((String)"astolfoclient", (String)("textures/gui/boikiser/boykisser_" + i + ".png")));
        }
    }

    public void render(DrawContext context, float delta) {
        long now = (long)((double)System.nanoTime() / 1000000.0);
        if (now - this.lastFrameTime > 100L) {
            this.currentFrame = (this.currentFrame + 1) % this.frames.size();
            this.lastFrameTime = now;
        }
        Identifier currentTexture = this.frames.get(this.currentFrame);
        context.getMatrices().pushMatrix();
        float scaleModifier = this.getScaleModifier();
        context.getMatrices().translate((float)this.x, (float)this.y);
        context.getMatrices().scale(scaleModifier, scaleModifier);
        context.getMatrices().translate((float)(-this.x), (float)(-this.y));
        context.drawTexture(net.minecraft.client.gl.RenderPipelines.GUI_TEXTURED, currentTexture, (int)this.x, (int)this.y, 0, 0, 64, 64, 64, 64);
        context.getMatrices().popMatrix();
    }

    public float getScaleModifier() {
        MinecraftClient mc = MinecraftClient.getInstance();
        double currentGuiScale = mc.getWindow().getScaleFactor();
        if (currentGuiScale <= 0.0) {
            currentGuiScale = 2.0;
        }
        return (float)(2.0 / currentGuiScale);
    }

    public boolean onMouseClicked(double mouseX, double mouseY, int button) {
        float scaleModifier = this.getScaleModifier();
        double effectiveW = 64.0 * (double)scaleModifier;
        double effectiveH = 64.0 * (double)scaleModifier;
        if (button == 0 && mouseX >= this.x && mouseX <= this.x + effectiveW && mouseY >= this.y && mouseY <= this.y + effectiveH) {
            this.isDragging = true;
            this.dragX = mouseX - this.x;
            this.dragY = mouseY - this.y;
            return true;
        }
        return false;
    }

    public void onMouseDragged(double mouseX, double mouseY, int button) {
        if (button == 0 && this.isDragging) {
            float scaleModifier = this.getScaleModifier();
            float screenW = this.client.getWindow().getScaledWidth();
            float screenH = this.client.getWindow().getScaledHeight();
            float effectiveW = (float)(64.0 * (double)scaleModifier);
            float effectiveH = (float)(64.0 * (double)scaleModifier);
            this.x = Math.max(0.0, Math.min((double)Math.max(0.0f, screenW - effectiveW), mouseX - this.dragX));
            this.y = Math.max(0.0, Math.min((double)Math.max(0.0f, screenH - effectiveH), mouseY - this.dragY));
        }
    }

    public void onMouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0) {
            this.isDragging = false;
        }
    }

    public void onMouseReleased(int button) {
        if (button == 0) {
            this.isDragging = false;
        }
    }

    public double getX() {
        return this.x;
    }

    public double getY() {
        return this.y;
    }

    public void setX(double x) {
        this.x = x;
    }

    public void setY(double y) {
        this.y = y;
    }

    public double getWidth() {
        return 64.0;
    }

    public double getHeight() {
        return 64.0;
    }
}

