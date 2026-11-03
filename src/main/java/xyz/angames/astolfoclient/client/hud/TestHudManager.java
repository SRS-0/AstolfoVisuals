package xyz.angames.astolfoclient.client.hud;

import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import dev.sxmurxy.mre.builders.Builder;
import dev.sxmurxy.mre.builders.states.QuadColorState;
import dev.sxmurxy.mre.builders.states.QuadRadiusState;
import dev.sxmurxy.mre.builders.states.SizeState;
import dev.sxmurxy.mre.msdf.MsdfFont;
import dev.sxmurxy.mre.renderers.impl.BuiltBlur;
import dev.sxmurxy.mre.renderers.impl.BuiltLiquidGlass;
import dev.sxmurxy.mre.renderers.impl.BuiltRectangle;
import dev.sxmurxy.mre.renderers.impl.BuiltText;
import java.awt.Color;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import org.joml.Matrix4f;
import xyz.angames.astolfoclient.client.AstolfoclientClient;
import xyz.angames.astolfoclient.client.module.Module;

@Environment(value=EnvType.CLIENT)
public class TestHudManager {
    private final MinecraftClient client = MinecraftClient.getInstance();
    private static final Supplier<MsdfFont> BIKO_FONT = Suppliers.memoize(() -> MsdfFont.builder().atlas("biko").data("biko").build());
    public double x = 100.0;
    public double y = 100.0;
    public double width = 150.0;
    public double height = 100.0;
    private boolean isDragging = false;
    private double dragX;
    private double dragY;

    public void render(DrawContext context, float delta) {
        boolean isModuleOn;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.world == null) {
            return;
        }
        Module testModule = AstolfoclientClient.moduleManager.getModuleByName("Test");
        boolean bl = isModuleOn = testModule != null && testModule.isEnabled();
        if (!isModuleOn) {
            return;
        }
        float blurRad = 15.0f;
        float squirtVal = 7.0f;
        float distortVal = 0.08f;
        float fresnelPowerVal = 2.0f;
        float baseAlphaVal = 0.2f;
        float roundingVal = 7.0f;
        context.getMatrices().pushMatrix();
        float scaleModifier = this.getScaleModifier();
        context.getMatrices().translate((float)this.x, (float)this.y);
        context.getMatrices().scale(scaleModifier, scaleModifier);
        context.getMatrices().translate((float)(-this.x), (float)(-this.y));
        Matrix4f matrix = xyz.angames.astolfoclient.client.render.GuiTransforms.matrix(context.getMatrices());
        float x1 = (float)this.x;
        float y1 = (float)this.y;
        float w = (float)this.width;
        float h = (float)this.height;
        ((BuiltBlur)Builder.blur().size(new SizeState(w, h)).radius(new QuadRadiusState(roundingVal)).blurRadius(blurRad).color(new QuadColorState(Color.WHITE)).build()).render(matrix, x1, y1);
        float liquidGlassRadius = roundingVal * squirtVal / 2.0f;
        ((BuiltLiquidGlass)Builder.liquidGlass().size(new SizeState(w + 2.0f, h + 2.0f)).radius(new QuadRadiusState(liquidGlassRadius)).color(new QuadColorState(Color.WHITE)).smoothness(0.5f, squirtVal).alpha(1.0f, baseAlphaVal).fresnel(fresnelPowerVal, Color.WHITE.getRGB(), 1.0f, 0.0f, true).distortStrength(distortVal).captureBackground().build()).render(matrix, x1 - 1.0f, y1 - 1.0f);
        ((BuiltRectangle)Builder.rectangle().size(new SizeState(w, h)).radius(new QuadRadiusState(roundingVal)).color(new QuadColorState(new Color(30, 30, 30, 180))).build()).render(matrix, x1, y1);
        String text = "67";
        float textWidth = ((MsdfFont)BIKO_FONT.get()).getWidth(text, 12.0f);
        ((BuiltText)Builder.text().font((MsdfFont)BIKO_FONT.get()).text(text).color(Color.WHITE).size(12.0f).build()).render(matrix, x1 + (w - textWidth) / 2.0f, y1 + (h - 12.0f) / 2.0f);
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
        boolean isModuleOn;
        MinecraftClient mc = MinecraftClient.getInstance();
        Module testModule = AstolfoclientClient.moduleManager.getModuleByName("Test");
        boolean bl = isModuleOn = testModule != null && testModule.isEnabled();
        if (!isModuleOn) {
            return false;
        }
        float scaleModifier = this.getScaleModifier();
        double effectiveW = this.width * (double)scaleModifier;
        double effectiveH = this.height * (double)scaleModifier;
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
            MinecraftClient mc = MinecraftClient.getInstance();
            float scaleModifier = this.getScaleModifier();
            float screenW = mc.getWindow().getScaledWidth();
            float screenH = mc.getWindow().getScaledHeight();
            float effectiveW = (float)(this.width * (double)scaleModifier);
            float effectiveH = (float)(this.height * (double)scaleModifier);
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
        return this.width;
    }

    public double getHeight() {
        return this.height;
    }
}

