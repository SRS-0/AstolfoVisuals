package xyz.angames.astolfoclient.client.hud;

import dev.sxmurxy.mre.builders.Builder;
import dev.sxmurxy.mre.builders.states.QuadColorState;
import dev.sxmurxy.mre.builders.states.QuadRadiusState;
import dev.sxmurxy.mre.builders.states.SizeState;
import dev.sxmurxy.mre.renderers.impl.BuiltBorder;
import dev.sxmurxy.mre.renderers.impl.BuiltRectangle;
import java.awt.Color;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4f;
import xyz.angames.astolfoclient.client.AstolfoclientClient;
import xyz.angames.astolfoclient.client.config.ThemeManager;
import xyz.angames.astolfoclient.client.gui.clickgui.GuiUtils;
import xyz.angames.astolfoclient.client.hud.LogoRenderer;
import xyz.angames.astolfoclient.client.module.Module;

@Environment(value=EnvType.CLIENT)
public class CustomHotbarManager {
    private final Minecraft client = Minecraft.getInstance();
    private Module cachedModule;
    private static final float SLOT_SIZE = 26.0f;
    private static final float SPACING = 4.0f;
    private static final int SLOTS = 9;
    private static final float PADDING = 0.0f;
    private static final float HOTBAR_WIDTH = 266.0f;
    private static final float HOTBAR_HEIGHT = 26.0f;
    private final SizeState mainSizeState = new SizeState(266.0f, 26.0f);
    private final SizeState singleSlotSizeState = new SizeState(26.0f, 26.0f);
    private final QuadRadiusState commonRadiusState = new QuadRadiusState(7.0f);
    private static final Color BG_COLOR_OBJ = new Color(10, 10, 12, 220);
    private static final Color BLUR_COLOR_OBJ = new Color(0, 0, 0, 120);
    private final QuadColorState bgSolidState = new QuadColorState(BG_COLOR_OBJ);
    private final QuadColorState bgBlurState = new QuadColorState(BLUR_COLOR_OBJ);
    private float animatedSlotPosition = 0.0f;
    private long lastUpdateTimeNs = -1L;

    public void render(GuiGraphicsExtractor context, float tickDelta) {
        if (this.cachedModule == null) {
            this.cachedModule = AstolfoclientClient.moduleManager.getModuleByName("CustomHotbar");
        }
        if (this.cachedModule == null || !this.cachedModule.isEnabled()) {
            return;
        }
        LocalPlayer player = this.client.player;
        if (player == null || this.client.gameRenderer.gameRenderState().guiRenderState.isHudHidden || player.isSpectator()) {
            return;
        }
        Matrix4f matrix = xyz.angames.astolfoclient.client.render.GuiTransforms.matrix(context.pose());
        int screenWidth = context.guiWidth();
        int screenHeight = context.guiHeight();
        context.pose().pushMatrix();
        float scaleModifier = this.getScaleModifier();
        context.pose().scale(scaleModifier, scaleModifier);
        float scaledWidth = (float)screenWidth / scaleModifier;
        float scaledHeight = (float)screenHeight / scaleModifier;
        float startX = (scaledWidth - 266.0f) / 2.0f;
        float startY = scaledHeight - 26.0f - 5.0f - LogoRenderer.getHotbarYOffset() / scaleModifier;
        long now = System.currentTimeMillis();
        Color themeColor = new Color(ThemeManager.getThemedColor(now / 10L));
        int darkR = (int)((float)themeColor.getRed() * 0.2f);
        int darkG = (int)((float)themeColor.getGreen() * 0.2f);
        int darkB = (int)((float)themeColor.getBlue() * 0.2f);
        Color topGradColor = new Color(darkR, darkG, darkB, 220);
        Color bottomGradColor = new Color(0, 0, 0, 220);
        float wave = (float)(Math.sin((double)now / 1000.0 * 2.0) * 0.5 + 0.5);
        Color topColor = GuiUtils.interpolateColor(bottomGradColor, topGradColor, wave);
        Color bottomColor = GuiUtils.interpolateColor(bottomGradColor, topGradColor, 1.0f - wave);
        this.renderShadow(matrix, startX, startY, 266.0f, 26.0f, 7.0f, 1.0f);
        ((BuiltRectangle)Builder.rectangle().size(this.mainSizeState).radius(this.commonRadiusState).color(new QuadColorState(topColor, bottomColor, bottomColor, topColor)).build()).render(matrix, startX, startY);
        int currentSlot = player.getInventory().getSelectedSlot();
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
        float interpolationSpeed = 15.0f;
        this.animatedSlotPosition += ((float)currentSlot - this.animatedSlotPosition) * (float)(1.0 - Math.exp((double)(-interpolationSpeed) * deltaSeconds));
        float selectionX = startX + 0.0f + this.animatedSlotPosition * 30.0f;
        float selectionY = startY + 0.0f;
        Color faintFill = new Color(themeColor.getRed(), themeColor.getGreen(), themeColor.getBlue(), 50);
        ((BuiltBorder)Builder.border().size(this.singleSlotSizeState).radius(this.commonRadiusState).color(new QuadColorState(themeColor)).thickness(2.0f).build()).render(matrix, selectionX, selectionY);
        ((BuiltRectangle)Builder.rectangle().size(this.singleSlotSizeState).radius(this.commonRadiusState).color(new QuadColorState(faintFill)).build()).render(matrix, selectionX, selectionY);
        float itemBaseY = startY + 0.0f + 5.0f;
        for (int i = 0; i < 9; ++i) {
            ItemStack stack = (ItemStack)player.getInventory().getItem(i);
            if (stack.isEmpty()) continue;
            float itemX = startX + 0.0f + (float)i * 30.0f + 5.0f;
            this.renderItem(context, stack, (int)itemX, (int)itemBaseY);
        }
        ItemStack offhandStack = player.getOffhandItem();
        if (!offhandStack.isEmpty()) {
            float offhandX = startX - 26.0f - 8.0f;
            float offhandY = startY;
            this.renderShadow(matrix, offhandX, offhandY, 26.0f, 26.0f, 7.0f, 1.0f);
            ((BuiltRectangle)Builder.rectangle().size(this.singleSlotSizeState).radius(this.commonRadiusState).color(new QuadColorState(topColor, bottomColor, bottomColor, topColor)).build()).render(matrix, offhandX, offhandY);
            ((BuiltBorder)Builder.border().size(this.singleSlotSizeState).radius(this.commonRadiusState).color(new QuadColorState(themeColor)).thickness(1.5f).build()).render(matrix, offhandX, offhandY);
            this.renderItem(context, offhandStack, (int)(offhandX + 0.0f + 5.0f), (int)(offhandY + 0.0f + 5.0f));
        }
        context.pose().popMatrix();
    }

    private void renderItem(GuiGraphicsExtractor context, ItemStack stack, int x, int y) {
        context.item(stack, x, y);
        context.itemDecorations(this.client.font, stack, x, y);
    }

    private void renderShadow(Matrix4f matrix, float x, float y, float w, float h, float radius, float masterAlpha) {
        int layers = 6;
        float maxSpread = 5.0f;
        for (int i = layers; i > 0; --i) {
            float progress = (float)i / (float)layers;
            float fade = 1.0f - progress;
            float alpha = fade * fade * 0.4f * masterAlpha;
            int alphaInt = Math.max(0, Math.min(255, (int)(255.0f * alpha)));
            if (alphaInt <= 0) continue;
            float expand = progress * maxSpread;
            Color shadowColor = new Color(0, 0, 0, alphaInt);
            ((BuiltRectangle)Builder.rectangle().size(new SizeState(w + expand * 2.0f, h + expand * 2.0f)).radius(new QuadRadiusState(radius + expand)).color(new QuadColorState(shadowColor)).build()).render(matrix, x - expand, y - expand);
        }
    }

    private float getScaleModifier() {
        Minecraft mc = Minecraft.getInstance();
        double currentGuiScale = mc.getWindow().getGuiScale();
        float baseScale = 0.8f;
        float referenceWidth = 2560.0f;
        float screenWidth = mc.getWindow().getWidth();
        float resolutionScale = screenWidth / referenceWidth;
        resolutionScale = Math.max(0.5f, Math.min(1.5f, resolutionScale));
        return (float)(2.0 * (double)baseScale * (double)resolutionScale / currentGuiScale);
    }
}

