package xyz.angames.astolfoclient.client.gui.clickgui;

import dev.sxmurxy.mre.builders.Builder;
import dev.sxmurxy.mre.builders.states.QuadColorState;
import dev.sxmurxy.mre.builders.states.QuadRadiusState;
import dev.sxmurxy.mre.builders.states.SizeState;
import dev.sxmurxy.mre.renderers.impl.BuiltBorder;
import dev.sxmurxy.mre.renderers.impl.BuiltRectangle;
import java.awt.Color;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;
import xyz.angames.astolfoclient.client.config.ThemeManager;
import xyz.angames.astolfoclient.client.gui.clickgui.GuiUtils;
import xyz.angames.astolfoclient.client.util.ModSounds;

@Environment(value=EnvType.CLIENT)
public class ColorPickerComponent {
    private final String title;
    public float x;
    public float y;
    public float width;
    public float height;
    private float hue = 0.0f;
    private float sat = 1.0f;
    private float bri = 1.0f;
    private float animHue = 0.0f;
    private float animSat = 1.0f;
    private float animBri = 1.0f;
    private boolean draggingPad = false;
    private boolean draggingHue = false;

    public ColorPickerComponent(String title) {
        this.title = title;
    }

    public void loadColor() {
        Color current = ThemeManager.getThemeColor();
        if (current == null) {
            current = new Color(0xFF0000);
        }
        float[] hsb = Color.RGBtoHSB(current.getRed(), current.getGreen(), current.getBlue(), null);
        this.hue = hsb[0];
        this.sat = hsb[1];
        this.bri = hsb[2];
        this.animHue = this.hue;
        this.animSat = this.sat;
        this.animBri = this.bri;
    }

    public void animate(float deltaTime) {
        this.animHue = GuiUtils.animate(this.animHue, this.hue, 20.0f, deltaTime);
        this.animSat = GuiUtils.animate(this.animSat, this.sat, 20.0f, deltaTime);
        this.animBri = GuiUtils.animate(this.animBri, this.bri, 20.0f, deltaTime);
    }

    public void render(Matrix4f matrix, int mouseX, int mouseY, float alpha) {
        float padX = this.x + 6.0f;
        float padY = this.y + 6.0f;
        float padW = this.width - 12.0f;
        float padH = this.height - 25.0f;
        float hueBarX = padX;
        float hueBarY = padY + padH + 5.0f;
        float hueBarW = padW;
        float hueBarH = 8.0f;
        Color pureHue = Color.getHSBColor(this.animHue, 1.0f, 1.0f);
        Color cPureHue = GuiUtils.withAlpha(pureHue, alpha);
        Color cWhite = GuiUtils.withAlpha(Color.WHITE, alpha);
        ((BuiltRectangle)Builder.rectangle().size(new SizeState(padW, padH)).radius(new QuadRadiusState(5.0f)).color(new QuadColorState(cWhite, cWhite, cPureHue, cPureHue)).build()).render(matrix, padX, padY);
        Color cBlack = GuiUtils.withAlpha(Color.BLACK, alpha);
        Color cBlackTrans = new Color(0, 0, 0, 0);
        ((BuiltRectangle)Builder.rectangle().size(new SizeState(padW, padH)).radius(new QuadRadiusState(5.0f)).color(new QuadColorState(cBlackTrans, cBlack, cBlack, cBlackTrans)).build()).render(matrix, padX, padY);
        float curX = padX + Mth.clamp((float)this.animSat, (float)0.0f, (float)1.0f) * padW;
        float curY = padY + (1.0f - Mth.clamp((float)this.animBri, (float)0.0f, (float)1.0f)) * padH;
        ((BuiltBorder)Builder.border().size(new SizeState(8.5f, 8.5f)).radius(new QuadRadiusState(4.25f)).thickness(1.6f).color(new QuadColorState(GuiUtils.withAlpha(new Color(0, 0, 0, 160), alpha))).build()).render(matrix, curX - 4.25f, curY - 4.25f);
        ((BuiltBorder)Builder.border().size(new SizeState(7.0f, 7.0f)).radius(new QuadRadiusState(3.5f)).thickness(1.4f).color(new QuadColorState(GuiUtils.withAlpha(Color.WHITE, alpha))).build()).render(matrix, curX - 3.5f, curY - 3.5f);
        int slices = 6;
        float sliceW = hueBarW / (float)slices;
        for (int i = 0; i < slices; ++i) {
            float h1 = (float)i / (float)slices;
            float h2 = (float)(i + 1) / (float)slices;
            Color c1 = GuiUtils.withAlpha(Color.getHSBColor(h1, 1.0f, 1.0f), alpha);
            Color c2 = GuiUtils.withAlpha(Color.getHSBColor(h2, 1.0f, 1.0f), alpha);
            QuadRadiusState rad = QuadRadiusState.NO_ROUND;
            if (i == 0) {
                rad = new QuadRadiusState(4.0f, 4.0f, 0.0f, 0.0f);
            } else if (i == slices - 1) {
                rad = new QuadRadiusState(0.0f, 0.0f, 4.0f, 4.0f);
            }
            ((BuiltRectangle)Builder.rectangle().size(new SizeState(sliceW + 0.5f, hueBarH)).radius(rad).smoothness(0.0f).color(new QuadColorState(c1, c1, c2, c2)).build()).render(matrix, hueBarX + (float)i * sliceW, hueBarY);
        }
        float hueHandleX = hueBarX + Mth.clamp((float)this.animHue, (float)0.0f, (float)1.0f) * hueBarW;
        float hueHandleY = hueBarY + hueBarH / 2.0f;
        ((BuiltBorder)Builder.border().size(new SizeState(8.5f, 8.5f)).radius(new QuadRadiusState(4.25f)).thickness(1.6f).color(new QuadColorState(GuiUtils.withAlpha(new Color(0, 0, 0, 160), alpha))).build()).render(matrix, hueHandleX - 4.25f, hueHandleY - 4.25f);
        ((BuiltBorder)Builder.border().size(new SizeState(7.0f, 7.0f)).radius(new QuadRadiusState(3.5f)).thickness(1.4f).color(new QuadColorState(GuiUtils.withAlpha(Color.WHITE, alpha))).build()).render(matrix, hueHandleX - 3.5f, hueHandleY - 3.5f);
    }

    public boolean mouseClicked(double mx, double my, int btn) {
        float padX = this.x + 6.0f;
        float padY = this.y + 6.0f;
        float padH = this.height - 25.0f;
        float hueBarY = padY + padH + 5.0f;
        if (GuiUtils.isMouseOver((float)mx, (float)my, this.x, this.y, this.width, this.height)) {
            if (my >= (double)(hueBarY - 2.5f)) {
                this.draggingHue = true;
                this.draggingPad = false;
            } else {
                this.draggingPad = true;
                this.draggingHue = false;
            }
            ModSounds.playSliderMove();
            this.update(mx, my);
            return true;
        }
        return false;
    }

    public boolean isDragging() {
        return this.draggingPad || this.draggingHue;
    }

    public void update(double mx, double my) {
        float padX = this.x + 6.0f;
        float padY = this.y + 6.0f;
        float padW = this.width - 12.0f;
        float padH = this.height - 25.0f;
        float hueBarX = padX;
        float hueBarW = padW;
        if (this.draggingPad) {
            this.sat = Mth.clamp((float)((float)(mx - (double)padX) / padW), (float)0.0f, (float)1.0f);
            this.bri = 1.0f - Mth.clamp((float)((float)(my - (double)padY) / padH), (float)0.0f, (float)1.0f);
            this.updateTheme();
        }
        if (this.draggingHue) {
            this.hue = Mth.clamp((float)((float)(mx - (double)hueBarX) / hueBarW), (float)0.0f, (float)1.0f);
            this.updateTheme();
        }
    }

    public void mouseReleased() {
        this.draggingPad = false;
        this.draggingHue = false;
    }

    private void updateTheme() {
        Color c = Color.getHSBColor(this.hue, this.sat, this.bri);
        ThemeManager.setThemeColor(c);
    }
}

