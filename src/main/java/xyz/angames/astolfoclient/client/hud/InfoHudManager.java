package xyz.angames.astolfoclient.client.hud;

import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import dev.sxmurxy.mre.builders.Builder;
import dev.sxmurxy.mre.builders.states.QuadColorState;
import dev.sxmurxy.mre.builders.states.QuadRadiusState;
import dev.sxmurxy.mre.builders.states.SizeState;
import dev.sxmurxy.mre.msdf.MsdfFont;
import dev.sxmurxy.mre.renderers.impl.BuiltRectangle;
import dev.sxmurxy.mre.renderers.impl.BuiltText;
import java.awt.Color;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;
import org.joml.Matrix4f;
import xyz.angames.astolfoclient.client.AstolfoclientClient;
import xyz.angames.astolfoclient.client.config.ThemeManager;
import xyz.angames.astolfoclient.client.gui.HudEditorScreen;
import xyz.angames.astolfoclient.client.gui.clickgui.GuiUtils;
import xyz.angames.astolfoclient.client.module.modules.misc.CoordsHiderModule;
import xyz.angames.astolfoclient.client.module.modules.render.InterfaceModule;

@Environment(value=EnvType.CLIENT)
public class InfoHudManager {
    private static final Supplier<MsdfFont> SEMIBOLD_FONT = Suppliers.memoize(() -> MsdfFont.builder().atlas("semibold").data("semibold").build());
    private static final Supplier<MsdfFont> MEDIUM_FONT = Suppliers.memoize(() -> MsdfFont.builder().atlas("medium").data("medium").build());
    private static final Supplier<MsdfFont> ICON_FONT = Suppliers.memoize(() -> MsdfFont.builder().name("interface_icons_infohud").data(Identifier.fromNamespaceAndPath((String)"mre", (String)"icons/interface/interface.json")).atlas(Identifier.fromNamespaceAndPath((String)"mre", (String)"icons/interface/interface.png")).glyphMapper(g -> {
        int idx = g.index();
        if (idx == 3) {
            return 75;
        }
        if (idx == 4) {
            return 83;
        }
        if (idx == 5) {
            return 67;
        }
        if (idx == 6) {
            return 66;
        }
        return idx;
    }).build());
    public static float height = 18.0f;
    public static float radius = 5.5f;
    public static float infoSize = 7.0f;
    public static float iconSize = 7.0f;
    public static float paddingX = 6.5f;
    public static float iconGap = 4.2f;
    public static float elementGap = 5.0f;
    public float x = 10.0f;
    public float y = 45.0f;
    private boolean dragging = false;
    private float dragOffsetX;
    private float dragOffsetY;
    private float currentWidth = 120.0f;
    private double animatedBps = 0.0;
    private float animationProgress = 0.0f;
    private long lastUpdateTimeNs = -1L;
    private final AnimatedString coordXAnim = new AnimatedString();
    private final AnimatedString coordYAnim = new AnimatedString();
    private final AnimatedString coordZAnim = new AnimatedString();
    private final AnimatedString bpsAnim = new AnimatedString();

    public void render(GuiGraphicsExtractor context, float tickDelta) {
        Minecraft client = Minecraft.getInstance();
        if (client.level == null || client.player == null || AstolfoclientClient.moduleManager == null) {
            return;
        }
        boolean isEditing = client.gui.screen() instanceof HudEditorScreen;
        InterfaceModule interfaceMod = (InterfaceModule)AstolfoclientClient.moduleManager.getModuleByName("Interface");
        boolean isSettingEnabled = interfaceMod != null && interfaceMod.infoHud.get();
        boolean shouldShow = interfaceMod != null && interfaceMod.isEnabled() && isSettingEnabled || isEditing;
        long nowNs = System.nanoTime();
        if (this.lastUpdateTimeNs == -1L) {
            this.lastUpdateTimeNs = nowNs;
        }
        long elapsedNs = nowNs - this.lastUpdateTimeNs;
        this.lastUpdateTimeNs = nowNs;
        double deltaSeconds = (double)elapsedNs / 1.0E9;
        if (deltaSeconds > 0.1) {
            deltaSeconds = 0.1;
        }
        float interpolationSpeed = 10.0f;
        this.animationProgress += ((shouldShow ? 1.0f : 0.0f) - this.animationProgress) * (float)(1.0 - Math.exp((double)(-interpolationSpeed) * deltaSeconds));
        if (this.animationProgress < 0.01f) {
            return;
        }
        double px = client.player.getX();
        double py = client.player.getY();
        double pz = client.player.getZ();
        int ix = (int)px;
        int iy = (int)py;
        int iz = (int)pz;
        double deltaX = client.player.getX() - client.player.xo;
        double deltaZ = client.player.getZ() - client.player.zo;
        double targetBps = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ) * 20.0;
        this.animatedBps += (targetBps - this.animatedBps) * (1.0 - Math.exp(-8.0 * deltaSeconds));
        if (this.animatedBps < 0.01) {
            this.animatedBps = 0.0;
        }
        MsdfFont semibold = (MsdfFont)SEMIBOLD_FONT.get();
        MsdfFont iconFont = (MsdfFont)ICON_FONT.get();
        String iconCoords = "C";
        String iconBps = "B";
        boolean hideCoords = CoordsHiderModule.isInfoHudHidden();
        String mask = CoordsHiderModule.getMaskString();
        String valX = hideCoords ? mask : String.valueOf(ix);
        String valY = hideCoords ? mask : String.valueOf(iy);
        String valZ = hideCoords ? mask : String.valueOf(iz);
        this.coordXAnim.update(valX);
        this.coordYAnim.update(valY);
        this.coordZAnim.update(valZ);
        float coordsIconW = iconFont != null ? iconFont.getWidth(iconCoords, iconSize) : 7.0f;
        float coordsTextW = semibold != null ? semibold.getWidth(valX, infoSize) + semibold.getWidth("X ", infoSize) + semibold.getWidth(valY, infoSize) + semibold.getWidth("Y ", infoSize) + semibold.getWidth(valZ, infoSize) + semibold.getWidth("Z", infoSize) : 50.0f;
        float dotSize = 1.8f;
        float bpsIconW = iconFont != null ? iconFont.getWidth(iconBps, iconSize) : 7.0f;
        String bpsVal = String.format("%.2f", this.animatedBps).replace(',', '.');
        this.bpsAnim.update(bpsVal);
        float bpsTextW = semibold != null ? semibold.getWidth(bpsVal, infoSize) + semibold.getWidth(" BPS", infoSize) : 25.0f;
        float targetWidth = paddingX + coordsIconW + iconGap + coordsTextW + elementGap + dotSize + elementGap + bpsIconW + iconGap + bpsTextW + paddingX;
        this.currentWidth += (targetWidth - this.currentWidth) * (float)(1.0 - Math.exp(-15.0 * deltaSeconds));
        float scaleModifier = this.getScaleModifier();
        context.pose().pushMatrix();
        context.pose().translate(this.x, this.y);
        context.pose().scale(scaleModifier, scaleModifier);
        context.pose().translate(-this.x, -this.y);
        Matrix4f matrix = xyz.angames.astolfoclient.client.render.GuiTransforms.matrix(context.pose());
        long themeTime = (long)((double)nowNs / 1000000.0);
        Color themeColor = new Color(ThemeManager.getThemedColor(themeTime / 10L));
        Color whiteText = GuiUtils.withAlpha(Color.WHITE, this.animationProgress);
        Color dotColor = GuiUtils.withAlpha(new Color(130, 130, 135, 220), this.animationProgress);
        Color dynamicIconColor = GuiUtils.withAlpha(themeColor, this.animationProgress);
        this.renderShadow(matrix, this.x, this.y, this.currentWidth, height, radius, this.animationProgress);
        ((BuiltRectangle)Builder.rectangle().size(new SizeState(this.currentWidth, height)).radius(new QuadRadiusState(radius)).color(new QuadColorState(new Color(0, 0, 0, (int)(255.0f * this.animationProgress)))).build()).render(matrix, this.x, this.y);
        float centerY = this.y + height / 2.0f;
        float currentX = this.x + paddingX;
        float iconCoordsCX = currentX + coordsIconW / 2.0f;
        this.drawIconGlowShadow(matrix, iconCoordsCX, centerY, iconSize / 2.0f, themeColor, 0.12f * this.animationProgress);
        if (iconFont != null) {
            ((BuiltText)Builder.text().font(iconFont).text(iconCoords).color(dynamicIconColor).size(iconSize).build()).render(matrix, currentX, centerY - 2.5f);
        }
        currentX += coordsIconW + iconGap;
        if (semibold != null) {
            float wX = this.coordXAnim.render(matrix, currentX, centerY - 2.5f, whiteText, semibold, infoSize);
            ((BuiltText)Builder.text().font(semibold).text("X ").color(whiteText).size(infoSize).build()).render(matrix, currentX += wX, centerY - 2.5f);
            float wY = this.coordYAnim.render(matrix, currentX += semibold.getWidth("X ", infoSize), centerY - 2.5f, whiteText, semibold, infoSize);
            ((BuiltText)Builder.text().font(semibold).text("Y ").color(whiteText).size(infoSize).build()).render(matrix, currentX += wY, centerY - 2.5f);
            float wZ = this.coordZAnim.render(matrix, currentX += semibold.getWidth("Y ", infoSize), centerY - 2.5f, whiteText, semibold, infoSize);
            ((BuiltText)Builder.text().font(semibold).text("Z").color(whiteText).size(infoSize).build()).render(matrix, currentX += wZ, centerY - 2.5f);
            currentX += semibold.getWidth("Z", infoSize) + elementGap;
        }
        ((BuiltRectangle)Builder.rectangle().size(new SizeState(dotSize, dotSize)).radius(new QuadRadiusState(dotSize / 2.0f)).color(new QuadColorState(dotColor)).build()).render(matrix, currentX, centerY - dotSize / 2.0f);
        float iconBpsCX = (currentX += dotSize + elementGap) + bpsIconW / 2.0f;
        this.drawIconGlowShadow(matrix, iconBpsCX, centerY, iconSize / 2.0f, themeColor, 0.12f * this.animationProgress);
        if (iconFont != null) {
            ((BuiltText)Builder.text().font(iconFont).text(iconBps).color(dynamicIconColor).size(iconSize).build()).render(matrix, currentX, centerY - 2.5f);
        }
        currentX += bpsIconW + iconGap;
        if (semibold != null) {
            float wBps = this.bpsAnim.render(matrix, currentX, centerY - 2.5f, whiteText, semibold, infoSize);
            ((BuiltText)Builder.text().font(semibold).text(" BPS").color(whiteText).size(infoSize).build()).render(matrix, currentX += wBps, centerY - 2.5f);
        }
        context.pose().popMatrix();
    }

    private void renderShadow(Matrix4f matrix, float x, float y, float w, float h, float radius, float masterAlpha) {
        int layers = 12;
        float maxSpread = 6.5f;
        for (int i = layers - 1; i >= 0; --i) {
            float progress = (float)i / (float)layers;
            float spread = progress * maxSpread;
            float alphaFactor = (1.0f - progress) * (1.0f - progress);
            int alpha = (int)(102.0f * alphaFactor * masterAlpha);
            if (alpha <= 0) continue;
            ((BuiltRectangle)Builder.rectangle().size(new SizeState(w + spread * 2.0f, h + spread * 2.0f)).radius(new QuadRadiusState(radius + spread)).color(new QuadColorState(new Color(0, 0, 0, alpha))).build()).render(matrix, x - spread, y - spread + 0.8f);
        }
    }

    private void drawIconGlowShadow(Matrix4f matrix, float cx, float cy, float radius, Color color, float masterAlpha) {
        if (masterAlpha <= 0.001f) {
            return;
        }
        int steps = 10;
        float maxSpread = 6.5f;
        int cr = color.getRed();
        int cg = color.getGreen();
        int cb = color.getBlue();
        for (int i = steps - 1; i >= 0; --i) {
            float progress = (float)i / (float)steps;
            float spread = progress * maxSpread;
            float alphaFactor = (1.0f - progress) * (1.0f - progress);
            int alpha = (int)(255.0f * alphaFactor * masterAlpha);
            if (alpha <= 0) continue;
            float curR = radius + spread;
            ((BuiltRectangle)Builder.rectangle().size(new SizeState(curR * 2.0f, curR * 2.0f)).radius(new QuadRadiusState(curR)).color(new QuadColorState(new Color(cr, cg, cb, alpha))).build()).render(matrix, cx - curR, cy - curR);
        }
    }

    private float getScaleModifier() {
        Minecraft mc = Minecraft.getInstance();
        double currentGuiScale = mc.getWindow().getGuiScale();
        if (currentGuiScale <= 0.0) {
            currentGuiScale = 2.0;
        }
        return (float)(2.0 / currentGuiScale);
    }

    public boolean onMouseClicked(double mouseX, double mouseY, int button) {
        boolean isSettingEnabled;
        Minecraft mc = Minecraft.getInstance();
        boolean isEditing = mc.gui.screen() instanceof HudEditorScreen;
        InterfaceModule interfaceMod = (InterfaceModule)AstolfoclientClient.moduleManager.getModuleByName("Interface");
        boolean bl = isSettingEnabled = interfaceMod != null && interfaceMod.infoHud.get();
        if (!(isEditing || interfaceMod != null && isSettingEnabled && interfaceMod.isEnabled())) {
            return false;
        }
        float scaleModifier = this.getScaleModifier();
        float effectiveW = this.currentWidth * scaleModifier;
        float effectiveH = height * scaleModifier;
        if (button == 0 && mouseX >= (double)this.x && mouseX <= (double)(this.x + effectiveW) && mouseY >= (double)this.y && mouseY <= (double)(this.y + effectiveH)) {
            this.dragging = true;
            this.dragOffsetX = (float)(mouseX - (double)this.x);
            this.dragOffsetY = (float)(mouseY - (double)this.y);
            return true;
        }
        return false;
    }

    public boolean onMouseDragged(double mouseX, double mouseY, int button) {
        if (this.dragging && button == 0) {
            Minecraft mc = Minecraft.getInstance();
            float scaleModifier = this.getScaleModifier();
            float screenW = mc.getWindow().getGuiScaledWidth();
            float screenH = mc.getWindow().getGuiScaledHeight();
            float effectiveW = this.currentWidth * scaleModifier;
            float effectiveH = height * scaleModifier;
            float targetX = (float)(mouseX - (double)this.dragOffsetX);
            float targetY = (float)(mouseY - (double)this.dragOffsetY);
            this.x = Math.max(0.0f, Math.min(Math.max(0.0f, screenW - effectiveW), targetX));
            this.y = Math.max(0.0f, Math.min(Math.max(0.0f, screenH - effectiveH), targetY));
            return true;
        }
        return false;
    }

    public boolean onMouseReleased(double mouseX, double mouseY, int button) {
        if (this.dragging && button == 0) {
            this.dragging = false;
            return true;
        }
        return false;
    }

    public boolean onMouseReleased(int button) {
        if (this.dragging && button == 0) {
            this.dragging = false;
            return true;
        }
        return false;
    }

    public float getX() {
        return this.x;
    }

    public float getY() {
        return this.y;
    }

    public void setX(float x) {
        this.x = x;
    }

    public void setY(float y) {
        this.y = y;
    }

    public float getWidth() {
        return this.currentWidth;
    }

    public float getHeight() {
        return height;
    }

    @Environment(value=EnvType.CLIENT)
    private static class AnimatedString {
        private String currentStr = "";
        private final long[] animStartTimes = new long[32];
        private final char[] oldChars = new char[32];
        private static final long DURATION = 200L;

        private AnimatedString() {
        }

        public void update(String newStr) {
            if (newStr == null) {
                return;
            }
            if (this.currentStr.isEmpty()) {
                this.currentStr = newStr;
                return;
            }
            if (newStr.equals(this.currentStr)) {
                return;
            }
            long now = System.currentTimeMillis();
            int maxLen = Math.max(newStr.length(), this.currentStr.length());
            for (int i = 0; i < maxLen && i < 32; ++i) {
                char cNew;
                char cOld = i < this.currentStr.length() ? this.currentStr.charAt(i) : (char)'\u0000';
                char c = cNew = i < newStr.length() ? newStr.charAt(i) : (char)'\u0000';
                if (cOld != cNew && cOld != '\u0000' && cNew != '\u0000') {
                    this.oldChars[i] = cOld;
                    this.animStartTimes[i] = now;
                    continue;
                }
                if (cOld != '\u0000' && cNew != '\u0000') continue;
                this.oldChars[i] = '\u0000';
            }
            this.currentStr = newStr;
        }

        public float render(Matrix4f matrix, float startX, float y, Color textColor, MsdfFont font, float fontSize) {
            float currX = startX;
            long now = System.currentTimeMillis();
            for (int i = 0; i < this.currentStr.length(); ++i) {
                char cNew = this.currentStr.charAt(i);
                String strNew = String.valueOf(cNew);
                float charW = font.getWidth(strNew, fontSize);
                long elapsed = now - this.animStartTimes[i];
                if (this.oldChars[i] != '\u0000' && elapsed >= 0L && elapsed < 200L) {
                    float progress = (float)elapsed / 200.0f;
                    float easeOut = 1.0f - (float)Math.pow(1.0f - progress, 3.0);
                    char cOld = this.oldChars[i];
                    String strOld = String.valueOf(cOld);
                    int oldAlpha = Math.max(0, Math.min(textColor.getAlpha(), (int)((float)textColor.getAlpha() * (1.0f - easeOut))));
                    Color oldColor = new Color(textColor.getRed(), textColor.getGreen(), textColor.getBlue(), oldAlpha);
                    float oldOffsetY = -3.5f * easeOut;
                    ((BuiltText)Builder.text().font(font).text(strOld).size(fontSize).color(oldColor).build()).render(matrix, currX, y + oldOffsetY);
                    int newAlpha = Math.max(0, Math.min(textColor.getAlpha(), (int)((float)textColor.getAlpha() * easeOut)));
                    Color newColor = new Color(textColor.getRed(), textColor.getGreen(), textColor.getBlue(), newAlpha);
                    float newOffsetY = 3.5f * (1.0f - easeOut);
                    ((BuiltText)Builder.text().font(font).text(strNew).size(fontSize).color(newColor).build()).render(matrix, currX, y + newOffsetY);
                } else {
                    this.oldChars[i] = '\u0000';
                    ((BuiltText)Builder.text().font(font).text(strNew).size(fontSize).color(textColor).build()).render(matrix, currX, y);
                }
                currX += charW;
            }
            return currX - startX;
        }
    }
}

