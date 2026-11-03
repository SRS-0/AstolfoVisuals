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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Identifier;
import org.joml.Matrix4f;
import xyz.angames.astolfoclient.client.AstolfoclientClient;
import xyz.angames.astolfoclient.client.config.ThemeManager;
import xyz.angames.astolfoclient.client.gui.HudEditorScreen;
import xyz.angames.astolfoclient.client.gui.clickgui.GuiUtils;
import xyz.angames.astolfoclient.client.module.Module;
import xyz.angames.astolfoclient.client.module.modules.render.InterfaceModule;
import xyz.angames.astolfoclient.client.util.KeyUtils;

@Environment(value=EnvType.CLIENT)
public class ActiveBindsManager {
    private static final Supplier<MsdfFont> BOLD_FONT = Suppliers.memoize(() -> MsdfFont.builder().atlas("bold").data("bold").build());
    private static final Supplier<MsdfFont> SEMIBOLD_FONT = Suppliers.memoize(() -> MsdfFont.builder().atlas("semibold").data("semibold").build());
    private static final Supplier<MsdfFont> MEDIUM_FONT = Suppliers.memoize(() -> MsdfFont.builder().atlas("medium").data("medium").build());
    private static final Supplier<MsdfFont> ICON_FONT = Suppliers.memoize(() -> MsdfFont.builder().name("interface_icons_activebinds").data(Identifier.of((String)"mre", (String)"icons/interface/interface.json")).atlas(Identifier.of((String)"mre", (String)"icons/interface/interface.png")).glyphMapper(g -> {
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
    public float x = 10.0f;
    public float y = 100.0f;
    private boolean dragging = false;
    private float dragOffsetX;
    private float dragOffsetY;
    private final Map<String, Float> animationMap = new HashMap<String, Float>();
    private float currentWidth = 90.0f;
    private float currentHeight = 22.0f;
    private float masterAlpha = 0.0f;
    private long lastUpdateTimeNs = -1L;

    public void render(DrawContext context, float tickDelta) {
        float headerContentW;
        List<Module> allModules;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.world == null) {
            return;
        }
        boolean isEditing = mc.currentScreen instanceof HudEditorScreen;
        InterfaceModule interfaceMod = (InterfaceModule)(AstolfoclientClient.moduleManager != null ? AstolfoclientClient.moduleManager.getModuleByName("Interface") : null);
        boolean isModuleOn = interfaceMod != null && interfaceMod.isEnabled() && interfaceMod.activeBinds.get();
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
        long now = (long)((double)nowNs / 1000000.0);
        Color themeColor = new Color(ThemeManager.getThemedColor(now / 10L));
        ArrayList<BindData> activeList = new ArrayList<BindData>();
        List<Module> list = allModules = AstolfoclientClient.moduleManager != null ? AstolfoclientClient.moduleManager.getModules() : List.of();
        if (isEditing && allModules.stream().noneMatch(m -> m.isEnabled() && m.getKeyCode() != -1)) {
            activeList.add(new BindData("KillAura", "R", 1.0f));
            activeList.add(new BindData("TargetStrafe", "V", 1.0f));
            activeList.add(new BindData("Velocity", "H", 1.0f));
        } else {
            for (Module module : allModules) {
                float interpolationSpeed;
                if (module.getKeyCode() == -1) continue;
                boolean isOn = module.isEnabled();
                float currentProgress = this.animationMap.getOrDefault(module.getName(), Float.valueOf(0.0f)).floatValue();
                if ((currentProgress += ((isOn ? 1.0f : 0.0f) - currentProgress) * (float)(1.0 - Math.exp((double)(-(interpolationSpeed = 16.0f)) * deltaSeconds))) > 0.005f) {
                    this.animationMap.put(module.getName(), Float.valueOf(currentProgress));
                    activeList.add(new BindData(module.getName(), KeyUtils.getKeyName(module.getKeyCode()), currentProgress));
                    continue;
                }
                this.animationMap.remove(module.getName());
            }
        }
        boolean shouldShow = !activeList.isEmpty() && isModuleOn || isEditing;
        float f = shouldShow ? 1.0f : 0.0f;
        float fadeSpeed = 12.0f;
        this.masterAlpha += (f - this.masterAlpha) * (float)(1.0 - Math.exp((double)(-fadeSpeed) * deltaSeconds));
        if (this.masterAlpha < 0.005f && !shouldShow) {
            return;
        }
        float headerHeight = 20.0f;
        float headerNameSize = 8.0f;
        float headerIconSize = 8.0f;
        float headerPaddingX = 8.0f;
        float headerIconGap = 4.5f;
        float moduleNameSize = 7.0f;
        float rowHeight = 14.0f;
        float paddingX = 8.0f;
        float paddingBottom = 5.0f;
        float minColumnGap = 16.0f;
        float radius = 6.0f;
        Color white = new Color(255, 255, 255, 255);
        Color lightGrayText = new Color(220, 220, 225, 255);
        MsdfFont semibold = (MsdfFont)SEMIBOLD_FONT.get();
        MsdfFont medium = (MsdfFont)MEDIUM_FONT.get();
        MsdfFont iconFont = (MsdfFont)ICON_FONT.get();
        String iconChar = "K";
        String headerText = "Keybinds";
        float headerIconW = iconFont != null ? iconFont.getWidth(iconChar, headerIconSize) : 8.0f;
        float headerTextW = semibold != null ? semibold.getWidth(headerText, headerNameSize) : 38.0f;
        float maxRowW = headerContentW = headerIconW + headerIconGap + headerTextW;
        for (BindData bind : activeList) {
            float nameW = medium != null ? medium.getWidth(bind.name, moduleNameSize) : 35.0f;
            float f2 = medium != null ? medium.getWidth(bind.key, moduleNameSize) : 10.0f;
            float keyW = f2;
            float rowW = nameW + minColumnGap + keyW;
            if (!(rowW > maxRowW)) continue;
            maxRowW = rowW;
        }
        float targetWidth = Math.max(88.0f, paddingX + maxRowW + paddingX);
        float targetHeight = activeList.isEmpty() ? headerHeight : headerHeight + 2.0f + (float)activeList.size() * rowHeight + paddingBottom;
        this.currentWidth += (targetWidth - this.currentWidth) * (float)(1.0 - Math.exp(-15.0 * deltaSeconds));
        this.currentHeight += (targetHeight - this.currentHeight) * (float)(1.0 - Math.exp(-15.0 * deltaSeconds));
        float scaleModifier = this.getScaleModifier();
        context.getMatrices().pushMatrix();
        context.getMatrices().translate(this.x, this.y);
        context.getMatrices().scale(scaleModifier, scaleModifier);
        context.getMatrices().translate(-this.x, -this.y);
        Matrix4f baseMatrix = xyz.angames.astolfoclient.client.render.GuiTransforms.matrix(context.getMatrices());
        this.renderShadow(baseMatrix, this.x, this.y, this.currentWidth, this.currentHeight, radius, this.masterAlpha);
        ((BuiltRectangle)Builder.rectangle().size(new SizeState(this.currentWidth, this.currentHeight)).radius(new QuadRadiusState(radius)).color(new QuadColorState(new Color(0, 0, 0, (int)(255.0f * this.masterAlpha)))).build()).render(baseMatrix, this.x, this.y);
        float headerCenterY = this.y + headerHeight / 2.0f;
        float hX = this.x + headerPaddingX;
        float iconCX = hX + headerIconW / 2.0f;
        this.drawIconGlowShadow(baseMatrix, iconCX, headerCenterY, headerIconSize / 2.0f, themeColor, 0.12f * this.masterAlpha);
        if (iconFont != null) {
            ((BuiltText)Builder.text().font(iconFont).text(iconChar).color(GuiUtils.withAlpha(themeColor, this.masterAlpha)).size(headerIconSize).build()).render(baseMatrix, hX, headerCenterY - 2.9f);
        }
        hX += headerIconW + headerIconGap;
        if (semibold != null) {
            ((BuiltText)Builder.text().font(semibold).text(headerText).color(GuiUtils.withAlpha(white, this.masterAlpha)).size(headerNameSize).build()).render(baseMatrix, hX, headerCenterY - 2.9f);
        }
        if (!activeList.isEmpty()) {
            float divY = this.y + headerHeight;
            ((BuiltRectangle)Builder.rectangle().size(new SizeState(this.currentWidth - paddingX * 2.0f, 1.0f)).radius(new QuadRadiusState(0.5f)).color(new QuadColorState(new Color(22, 22, 28, (int)(200.0f * this.masterAlpha)))).build()).render(baseMatrix, this.x + paddingX, divY);
        }
        float rowStartY = this.y + headerHeight + 3.5f;
        for (int i = 0; i < activeList.size(); ++i) {
            BindData bind = (BindData)activeList.get(i);
            float rowY = rowStartY + (float)i * rowHeight;
            float rowCenterY = rowY + rowHeight / 2.0f;
            float totalRowAlpha = bind.progress * this.masterAlpha;
            Color rowWhite = GuiUtils.withAlpha(lightGrayText, totalRowAlpha);
            Color rowDim = GuiUtils.withAlpha(themeColor, totalRowAlpha);
            if (medium != null) {
                ((BuiltText)Builder.text().font(medium).text(bind.name).color(rowWhite).size(moduleNameSize).build()).render(baseMatrix, this.x + paddingX, rowCenterY - 2.5f);
            }
            if (medium == null) continue;
            float keyW = medium.getWidth(bind.key, moduleNameSize);
            float rightX = this.x + this.currentWidth - paddingX - keyW;
            ((BuiltText)Builder.text().font(medium).text(bind.key).color(rowDim).size(moduleNameSize).build()).render(baseMatrix, rightX, rowCenterY - 2.5f);
        }
        context.getMatrices().popMatrix();
    }

    private void renderShadow(Matrix4f matrix, float x, float y, float w, float h, float radius, float masterAlpha) {
        int layers = 12;
        float maxSpread = 7.5f;
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
        float maxSpread = 7.0f;
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
        MinecraftClient mc = MinecraftClient.getInstance();
        double currentGuiScale = mc.getWindow().getScaleFactor();
        if (currentGuiScale <= 0.0) {
            currentGuiScale = 2.0;
        }
        return (float)(2.0 / currentGuiScale);
    }

    public boolean onMouseClicked(double mouseX, double mouseY, int button) {
        boolean isSettingEnabled;
        MinecraftClient mc = MinecraftClient.getInstance();
        boolean isEditing = mc.currentScreen instanceof HudEditorScreen;
        InterfaceModule interfaceMod = (InterfaceModule)(AstolfoclientClient.moduleManager != null ? AstolfoclientClient.moduleManager.getModuleByName("Interface") : null);
        boolean bl = isSettingEnabled = interfaceMod != null && interfaceMod.activeBinds.get();
        if (!(isEditing || interfaceMod != null && isSettingEnabled && interfaceMod.isEnabled())) {
            return false;
        }
        float scaleModifier = this.getScaleModifier();
        float effectiveW = this.currentWidth * scaleModifier;
        float effectiveH = this.currentHeight * scaleModifier;
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
            MinecraftClient mc = MinecraftClient.getInstance();
            float scaleModifier = this.getScaleModifier();
            float screenW = mc.getWindow().getScaledWidth();
            float screenH = mc.getWindow().getScaledHeight();
            float effectiveW = this.currentWidth * scaleModifier;
            float effectiveH = this.currentHeight * scaleModifier;
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
        return this.currentHeight;
    }

    @Environment(value=EnvType.CLIENT)
    private static class BindData {
        String name;
        String key;
        float progress;

        BindData(String name, String key, float progress) {
            this.name = name;
            this.key = key;
            this.progress = progress;
        }
    }
}

