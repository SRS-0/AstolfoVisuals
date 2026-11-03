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
import dev.sxmurxy.mre.renderers.impl.BuiltTexture;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import org.joml.Matrix4f;
import org.lwjgl.glfw.GLFW;
import xyz.angames.astolfoclient.client.AstolfoclientClient;
import xyz.angames.astolfoclient.client.config.ThemeManager;
import xyz.angames.astolfoclient.client.gui.HudEditorScreen;
import xyz.angames.astolfoclient.client.gui.clickgui.GuiUtils;
import xyz.angames.astolfoclient.client.hud.LogoRenderer;
import xyz.angames.astolfoclient.client.module.modules.render.InterfaceModule;

@Environment(value=EnvType.CLIENT)
public class EffectHudManager {
    public float x = 10.0f;
    public float y = 100.0f;
    public Position position = Position.LEFT;
    public boolean panelOpen = false;
    private boolean submenuOpen = false;
    private float mainPanelAnim = 0.0f;
    private float subPanelAnim = 0.0f;
    private boolean wasMouseDown = false;
    private final Map<MobEffect, PotionCardState> cardStateMap = new LinkedHashMap<MobEffect, PotionCardState>();
    private float totalHeight = 0.0f;
    private float lastFrameTotalMaxWidth = 90.0f;
    private float animatedX = 10.0f;
    private float masterAlpha = 0.0f;
    private long lastUpdateTimeNs = -1L;
    private static final Supplier<MsdfFont> BOLD_FONT = Suppliers.memoize(() -> MsdfFont.builder().atlas("bold").data("bold").build());
    private static final Supplier<MsdfFont> SEMIBOLD_FONT = Suppliers.memoize(() -> MsdfFont.builder().atlas("semibold").data("semibold").build());
    private static final Supplier<MsdfFont> MEDIUM_FONT = Suppliers.memoize(() -> MsdfFont.builder().atlas("medium").data("medium").build());
    private static final Supplier<MsdfFont> ICON_FONT = Suppliers.memoize(() -> MsdfFont.builder().atlas("icon").data("icon").build());

    public void render(GuiGraphicsExtractor context) {
        this.render(context, Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(false));
    }

    public void render(GuiGraphicsExtractor context, float tickDelta) {
        PotionCardState state;
        MobEffect effect;
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) {
            return;
        }
        Collection effects = client.player.getActiveEffects();
        boolean isEditing = client.gui.screen() instanceof HudEditorScreen;
        InterfaceModule interfaceMod = (InterfaceModule)(AstolfoclientClient.moduleManager != null ? AstolfoclientClient.moduleManager.getModuleByName("Interface") : null);
        boolean isSettingEnabled = interfaceMod == null || interfaceMod.effectHud.get();
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
        long themeTime = (long)((double)nowNs / 1000000.0);
        Color themeColor = new Color(ThemeManager.getThemedColor(themeTime / 10L));
        List<MobEffectInstance> list = isEditing ? this.getPlaceholders() : new ArrayList(effects);
        HashSet<MobEffect> currentActiveEffects = new HashSet<MobEffect>();
        if (isSettingEnabled && (interfaceMod == null || interfaceMod.isEnabled() || isEditing)) {
            for (MobEffectInstance inst : list) {
                effect = (MobEffect)inst.getEffect().value();
                currentActiveEffects.add(effect);
                state = this.cardStateMap.computeIfAbsent(effect, k -> new PotionCardState());
                state.cachedInstance = inst;
            }
        }
        Iterator<Map.Entry<MobEffect, PotionCardState>> iterator = this.cardStateMap.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<MobEffect, PotionCardState> entry = iterator.next();
            effect = entry.getKey();
            state = entry.getValue();
            boolean isActive = currentActiveEffects.contains(effect);
            float targetAnim = isActive ? 1.0f : 0.0f;
            float speed = 14.0f;
            state.animProgress += (targetAnim - state.animProgress) * (float)(1.0 - Math.exp((double)(-speed) * deltaSeconds));
            float targetHeightScale = isActive ? 1.0f : (state.animProgress < 0.35f ? 0.0f : 1.0f);
            state.heightScale += (targetHeightScale - state.heightScale) * (float)(1.0 - Math.exp((double)(-speed) * deltaSeconds));
            if (isActive || !(state.animProgress < 0.005f) || !(state.heightScale < 0.005f)) continue;
            iterator.remove();
        }
        boolean shouldShow = !this.cardStateMap.isEmpty() && isSettingEnabled && (interfaceMod == null || interfaceMod.isEnabled() || isEditing);
        float targetMasterAlpha = shouldShow ? 1.0f : 0.0f;
        this.masterAlpha += (targetMasterAlpha - this.masterAlpha) * (float)(1.0 - Math.exp(-12.0 * deltaSeconds));
        if (this.masterAlpha < 0.005f && !shouldShow) {
            this.panelOpen = false;
            this.submenuOpen = false;
            this.mainPanelAnim = 0.0f;
            this.subPanelAnim = 0.0f;
            return;
        }
        float cardHeight = 26.0f;
        float radius = 6.0f;
        float iconSize = 14.0f;
        float nameSize = 8.0f;
        float infoSize = 7.0f;
        float paddingX = 8.0f;
        float iconGap = 6.5f;
        float gap = 4.0f;
        MsdfFont bold = (MsdfFont)BOLD_FONT.get();
        MsdfFont semibold = (MsdfFont)SEMIBOLD_FONT.get();
        MsdfFont medium = (MsdfFont)MEDIUM_FONT.get();
        MsdfFont iconFont = (MsdfFont)ICON_FONT.get();
        float scaleModifier = this.getScaleModifier();
        float scaledWidth = (float)client.getWindow().getGuiScaledWidth() / scaleModifier;
        float scaledHeight = (float)client.getWindow().getGuiScaledHeight() / scaleModifier;
        float computedTotalHeight = 0.0f;
        for (PotionCardState state2 : this.cardStateMap.values()) {
            computedTotalHeight += (cardHeight + gap) * state2.heightScale;
        }
        if (computedTotalHeight > gap) {
            computedTotalHeight -= gap;
        }
        this.y = (scaledHeight - computedTotalHeight) / 2.0f;
        float targetBaseX = this.position == Position.RIGHT ? scaledWidth - this.lastFrameTotalMaxWidth - 10.0f : 10.0f;
        this.animatedX += (targetBaseX - this.animatedX) * (float)(1.0 - Math.exp(-14.0 * deltaSeconds));
        this.x = this.animatedX;
        context.pose().pushMatrix();
        context.pose().scale(scaleModifier, scaleModifier);
        Matrix4f matrix = xyz.angames.astolfoclient.client.render.GuiTransforms.matrix(context.pose());
        float currentY = this.y;
        float currentFrameMaxWidth = 0.0f;
        for (Map.Entry<MobEffect, PotionCardState> entry : this.cardStateMap.entrySet()) {
            float levelW;
            float nameW;
            float cardProgress;
            float effectiveAlpha;
            MobEffect effect2 = entry.getKey();
            PotionCardState state3 = entry.getValue();
            MobEffectInstance instance = state3.cachedInstance;
            if (instance == null || (effectiveAlpha = (cardProgress = state3.animProgress) * this.masterAlpha) <= 0.002f) continue;
            String name = Component.translatable((String)effect2.getDescriptionId()).getString();
            int level = instance.getAmplifier() + 1;
            boolean showLevel = level > 1;
            String levelNum = String.valueOf(level);
            String duration = this.formatDuration(instance);
            state3.durationAnimator.update(duration);
            float f = nameW = bold != null ? bold.getWidth(name, nameSize) : 35.0f;
            levelW = showLevel ? (bold != null ? bold.getWidth(levelNum, nameSize) : 8.0f) : 0.0f;
            float spaceW = showLevel ? (bold != null ? bold.getWidth(" ", nameSize) : 3.0f) : 0.0f;
            float durationW = medium != null ? medium.getWidth(duration, infoSize) : 20.0f;
            float topLineW = nameW + spaceW + levelW;
            float bottomLineW = durationW;
            float maxTextW = Math.max(topLineW, bottomLineW);
            float targetWidth = paddingX + iconSize + iconGap + maxTextW + paddingX;
            state3.width += (targetWidth - state3.width) * (float)(1.0 - Math.exp(-14.0 * deltaSeconds));
            float currentAnimatedWidth = state3.width;
            if (currentAnimatedWidth > currentFrameMaxWidth) {
                currentFrameMaxWidth = currentAnimatedWidth;
            }
            float cardX = this.position == Position.RIGHT ? this.animatedX + (this.lastFrameTotalMaxWidth - currentAnimatedWidth) : this.animatedX;
            float cardEase = 1.0f - (float)Math.pow(1.0f - cardProgress, 3.0);
            float cardScale = 0.88f + 0.12f * cardEase;
            context.pose().pushMatrix();
            if (cardScale < 0.999f) {
                float cx = cardX + (this.position == Position.RIGHT ? currentAnimatedWidth : 0.0f);
                float cy = currentY + cardHeight / 2.0f;
                context.pose().translate(cx, cy);
                context.pose().scale(cardScale, cardScale);
                context.pose().translate(-cx, -cy);
            }
            Matrix4f cardMat = xyz.angames.astolfoclient.client.render.GuiTransforms.matrix(context.pose());
            this.renderShadow(cardMat, cardX, currentY, currentAnimatedWidth, cardHeight, radius, effectiveAlpha);
            ((BuiltRectangle)Builder.rectangle().size(new SizeState(currentAnimatedWidth, cardHeight)).radius(new QuadRadiusState(radius)).color(new QuadColorState(new Color(0, 0, 0, (int)(255.0f * effectiveAlpha)))).build()).render(cardMat, cardX, currentY);
            float centerY = currentY + cardHeight / 2.0f;
            float currentX = cardX + paddingX;
            float iconCX = currentX + iconSize / 2.0f;
            this.drawIconGlowShadow(cardMat, iconCX, centerY, iconSize / 2.0f, themeColor, 0.12f * effectiveAlpha);
            Identifier icon = this.getEffectIconIdentifier((Holder<MobEffect>)instance.getEffect());
            ((BuiltTexture)Builder.texture().size(new SizeState(iconSize, iconSize)).radius(new QuadRadiusState(3.0f)).texture(0.0f, 0.0f, 1.0f, 1.0f, client.getTextureManager().getTexture(icon)).color(new QuadColorState(new Color(255, 255, 255, (int)(255.0f * effectiveAlpha)))).build()).render(cardMat, currentX, centerY - iconSize / 2.0f);
            currentX += iconSize + iconGap;
            if (bold != null) {
                Color whiteText = GuiUtils.withAlpha(Color.WHITE, effectiveAlpha);
                ((BuiltText)Builder.text().font(bold).text(name).color(whiteText).size(nameSize).build()).render(cardMat, currentX, currentY + 4.5f);
                if (showLevel) {
                    ((BuiltText)Builder.text().font(bold).text(levelNum).color(GuiUtils.withAlpha(themeColor, effectiveAlpha)).size(nameSize).build()).render(cardMat, currentX + nameW + spaceW, currentY + 4.5f);
                }
            }
            if (medium != null) {
                Color grayText = GuiUtils.withAlpha(new Color(160, 160, 165), effectiveAlpha);
                state3.durationAnimator.render(cardMat, currentX, currentY + 14.5f, grayText, medium, infoSize, effectiveAlpha);
            }
            context.pose().popMatrix();
            currentY += (cardHeight + gap) * state3.heightScale;
        }
        this.totalHeight = currentY - this.y;
        this.lastFrameTotalMaxWidth = currentFrameMaxWidth;
        if (isEditing) {
            double mx = this.getScaledMouseX();
            double my = this.getScaledMouseY();
            boolean isMouseDown = GLFW.glfwGetMouseButton((long)client.getWindow().handle(), (int)0) == 1;
            this.drawConnectedContextPanel(context, scaledWidth, scaledHeight, themeColor, mx, my, isMouseDown, this.wasMouseDown, (float)deltaSeconds);
            this.wasMouseDown = isMouseDown;
        } else {
            this.panelOpen = false;
            this.submenuOpen = false;
            this.mainPanelAnim = 0.0f;
            this.subPanelAnim = 0.0f;
            this.wasMouseDown = false;
        }
        context.pose().popMatrix();
    }

    private void drawConnectedContextPanel(GuiGraphicsExtractor context, float screenW, float screenH, Color themeColor, double mouseX, double mouseY, boolean isMouseDown, boolean wasMouseDown, float deltaTime) {
        Color closeColor;
        float mX;
        boolean opensToRight;
        float targetMain = this.panelOpen ? 1.0f : 0.0f;
        this.mainPanelAnim += (targetMain - this.mainPanelAnim) * (float)(1.0 - Math.exp(-14.0 * (double)deltaTime));
        if (!this.panelOpen && this.mainPanelAnim < 0.01f) {
            this.mainPanelAnim = 0.0f;
            this.subPanelAnim = 0.0f;
            return;
        }
        float targetSub = this.submenuOpen && this.panelOpen ? 1.0f : 0.0f;
        this.subPanelAnim += (targetSub - this.subPanelAnim) * (float)(1.0 - Math.exp(-14.0 * (double)deltaTime));
        float mainW = 110.0f;
        float mainH = 48.0f;
        float subW = 88.0f;
        float subH = 38.0f;
        float mY = this.y;
        boolean bl = opensToRight = this.position == Position.LEFT;
        if (opensToRight) {
            mX = this.animatedX + this.lastFrameTotalMaxWidth + 8.0f;
        } else {
            float totalW = mainW + (this.subPanelAnim > 0.01f ? subW + 5.0f : 0.0f);
            mX = this.animatedX - totalW - 8.0f;
        }
        float f = this.subPanelAnim > 0.01f ? subW + 5.0f : 0.0f;
        if (mX + mainW + f > screenW - 6.0f) {
            mX = screenW - mainW - (this.subPanelAnim > 0.01f ? subW + 5.0f : 0.0f) - 6.0f;
        }
        if (mX < 6.0f) {
            mX = 6.0f;
        }
        if (mY + mainH > screenH - 6.0f) {
            mY = screenH - mainH - 6.0f;
        }
        if (mY < 6.0f) {
            mY = 6.0f;
        }
        MsdfFont boldFont = (MsdfFont)BOLD_FONT.get();
        MsdfFont mediumFont = (MsdfFont)MEDIUM_FONT.get();
        MsdfFont spFont = (MsdfFont)LogoRenderer.SP_FONT.get();
        float mainEase = 1.0f - (float)Math.pow(1.0f - this.mainPanelAnim, 3.0);
        float mainScale = 0.88f + 0.12f * mainEase;
        context.pose().pushMatrix();
        if (mainScale < 0.999f) {
            float cx = mX + mainW / 2.0f;
            float cy = mY + mainH / 2.0f;
            context.pose().translate(cx, cy);
            context.pose().scale(mainScale, mainScale);
            context.pose().translate(-cx, -cy);
        }
        Matrix4f matrix = xyz.angames.astolfoclient.client.render.GuiTransforms.matrix(context.pose());
        for (int i = 6; i >= 0; --i) {
            float progress = (float)i / 6.0f;
            float spread = progress * 4.0f;
            int alpha = (int)(60.0f * (1.0f - progress) * (1.0f - progress) * mainEase);
            if (alpha <= 0) continue;
            ((BuiltRectangle)Builder.rectangle().size(new SizeState(mainW + spread * 2.0f, mainH + spread * 2.0f)).radius(new QuadRadiusState(8.0f + spread)).color(new QuadColorState(new Color(0, 0, 0, alpha))).build()).render(matrix, mX - spread, mY - spread);
        }
        Color bgMain = new Color(0, 0, 0, (int)(255.0f * mainEase));
        ((BuiltRectangle)Builder.rectangle().size(new SizeState(mainW, mainH)).radius(new QuadRadiusState(8.0f)).color(new QuadColorState(bgMain)).build()).render(matrix, mX, mY);
        if (boldFont != null) {
            ((BuiltText)Builder.text().font(boldFont).text("Potions").size(7.5f).color(new Color(255, 255, 255, (int)(255.0f * mainEase))).build()).render(matrix, mX + 8.0f, mY + 6.5f);
        }
        float closeX = mX + mainW - 14.0f;
        float closeY = mY + 6.5f;
        boolean closeHover = mouseX >= (double)(closeX - 2.0f) && mouseX <= (double)(closeX + 10.0f) && mouseY >= (double)(closeY - 2.0f) && mouseY <= (double)(closeY + 10.0f);
        Color color = closeColor = closeHover ? new Color(255, 80, 80, (int)(255.0f * mainEase)) : new Color(140, 140, 150, (int)(255.0f * mainEase));
        if (spFont != null) {
            ((BuiltText)Builder.text().font(spFont).text("A").size(7.0f).color(closeColor).build()).render(matrix, closeX, closeY);
        }
        if (closeHover && isMouseDown && !wasMouseDown) {
            this.panelOpen = false;
            this.submenuOpen = false;
            context.pose().popMatrix();
            return;
        }
        float c1X = mX + 4.0f;
        float c1Y = mY + 20.0f;
        float c1W = mainW - 8.0f;
        float c1H = 22.0f;
        ((BuiltRectangle)Builder.rectangle().size(new SizeState(c1W, c1H)).radius(new QuadRadiusState(6.0f)).color(new QuadColorState(new Color(10, 10, 15, (int)(220.0f * mainEase)))).build()).render(matrix, c1X, c1Y);
        float r1Y = c1Y + 3.0f;
        float r1H = 16.0f;
        boolean r1Hov = mouseX >= (double)c1X && mouseX <= (double)(c1X + c1W) && mouseY >= (double)r1Y && mouseY <= (double)(r1Y + r1H);
        boolean r1Active = this.submenuOpen;
        if (r1Hov) {
            ((BuiltRectangle)Builder.rectangle().size(new SizeState(c1W - 4.0f, r1H)).radius(new QuadRadiusState(4.0f)).color(new QuadColorState(new Color(22, 22, 32, (int)(180.0f * mainEase)))).build()).render(matrix, c1X + 2.0f, r1Y);
        }
        if (spFont != null) {
            ((BuiltText)Builder.text().font(spFont).text("C").size(7.5f).color(new Color(170, 170, 190, (int)(255.0f * mainEase))).build()).render(matrix, c1X + 5.0f, r1Y + 4.0f);
        }
        if (mediumFont != null) {
            ((BuiltText)Builder.text().font(mediumFont).text("Position").size(6.5f).color(new Color(230, 230, 245, (int)(255.0f * mainEase))).build()).render(matrix, c1X + 16.0f, r1Y + 5.5f);
            String currentPosName = (this.position == Position.LEFT ? "Left" : "Right") + " >";
            float posStrW = mediumFont.getWidth(currentPosName, 6.0f);
            Color posCol = r1Active ? GuiUtils.withAlpha(themeColor, mainEase) : new Color(140, 140, 160, (int)(255.0f * mainEase));
            ((BuiltText)Builder.text().font(mediumFont).text(currentPosName).size(6.0f).color(posCol).build()).render(matrix, c1X + c1W - posStrW - 5.0f, r1Y + 5.5f);
        }
        if (r1Hov && isMouseDown && !wasMouseDown) {
            this.submenuOpen = !this.submenuOpen;
        }
        context.pose().popMatrix();
        if (this.subPanelAnim > 0.005f) {
            float subEase = 1.0f - (float)Math.pow(1.0f - this.subPanelAnim, 3.0);
            float subScale = 0.88f + 0.12f * subEase;
            float sX = mX + mainW + 5.0f;
            if (sX + subW > screenW - 6.0f) {
                sX = mX - subW - 5.0f;
            }
            float sY = mY;
            context.pose().pushMatrix();
            if (subScale < 0.999f) {
                float scx = sX + subW / 2.0f;
                float scy = sY + subH / 2.0f;
                context.pose().translate(scx, scy);
                context.pose().scale(subScale, subScale);
                context.pose().translate(-scx, -scy);
            }
            matrix = xyz.angames.astolfoclient.client.render.GuiTransforms.matrix(context.pose());
            for (int i = 6; i >= 0; --i) {
                float progress = (float)i / 6.0f;
                float spread = progress * 4.0f;
                int alpha = (int)(60.0f * (1.0f - progress) * (1.0f - progress) * subEase);
                if (alpha <= 0) continue;
                ((BuiltRectangle)Builder.rectangle().size(new SizeState(subW + spread * 2.0f, subH + spread * 2.0f)).radius(new QuadRadiusState(8.0f + spread)).color(new QuadColorState(new Color(0, 0, 0, alpha))).build()).render(matrix, sX - spread, sY - spread);
            }
            Color bgSub = new Color(0, 0, 0, (int)(255.0f * subEase));
            ((BuiltRectangle)Builder.rectangle().size(new SizeState(subW, subH)).radius(new QuadRadiusState(8.0f)).color(new QuadColorState(bgSub)).build()).render(matrix, sX, sY);
            Position[] posOptions = new Position[]{Position.LEFT, Position.RIGHT};
            String[] posLabels = new String[]{"Left", "Right"};
            float posRowH = 15.0f;
            float listStartY = sY + 4.0f;
            for (int i = 0; i < posOptions.length; ++i) {
                boolean rHov;
                float rowY = listStartY + (float)i * posRowH;
                boolean isSel = this.position == posOptions[i];
                boolean bl2 = rHov = mouseX >= (double)(sX + 3.0f) && mouseX <= (double)(sX + subW - 3.0f) && mouseY >= (double)rowY && mouseY <= (double)(rowY + 14.0f);
                if (rHov) {
                    ((BuiltRectangle)Builder.rectangle().size(new SizeState(subW - 6.0f, 14.0f)).radius(new QuadRadiusState(4.0f)).color(new QuadColorState(new Color(25, 25, 36, (int)(220.0f * subEase)))).build()).render(matrix, sX + 3.0f, rowY);
                }
                if (isSel && spFont != null) {
                    ((BuiltText)Builder.text().font(spFont).text("D").size(6.5f).color(new Color(255, 255, 255, (int)(255.0f * subEase))).build()).render(matrix, sX + 6.0f, rowY + 3.5f);
                }
                if (mediumFont != null) {
                    Color textCol = isSel ? new Color(240, 240, 255, (int)(255.0f * subEase)) : (rHov ? new Color(180, 180, 200, (int)(255.0f * subEase)) : new Color(110, 110, 130, (int)(255.0f * subEase)));
                    ((BuiltText)Builder.text().font(mediumFont).text(posLabels[i]).size(6.5f).color(textCol).build()).render(matrix, sX + (isSel ? 18.0f : 8.0f), rowY + 4.5f);
                }
                if (!rHov || !isMouseDown || wasMouseDown) continue;
                this.position = posOptions[i];
            }
            context.pose().popMatrix();
        }
    }

    private Identifier getEffectIconIdentifier(Holder<MobEffect> effect) {
        return effect.unwrapKey().map(key -> Identifier.fromNamespaceAndPath((String)"minecraft", (String)("textures/mob_effect/" + key.identifier().getPath() + ".png"))).orElse(Identifier.fromNamespaceAndPath((String)"minecraft", (String)"textures/missing.png"));
    }

    private String formatDuration(MobEffectInstance effect) {
        if (effect.isInfiniteDuration()) {
            return "Infinite";
        }
        int totalSeconds = effect.getDuration() / 20;
        return String.format("%d:%02d", totalSeconds / 60, totalSeconds % 60);
    }

    private List<MobEffectInstance> getPlaceholders() {
        return List.of(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 16500, 0), new MobEffectInstance(MobEffects.STRENGTH, 740, 1), new MobEffectInstance(MobEffects.SPEED, 1640, 1), new MobEffectInstance(MobEffects.ABSORPTION, 2260, 3));
    }

    private void renderShadow(Matrix4f matrix, float x, float y, float w, float h, float radius, float masterAlpha) {
        int layers = 12;
        float maxSpread = 8.0f;
        for (int i = layers - 1; i >= 0; --i) {
            float progress = (float)i / (float)layers;
            float spread = progress * maxSpread;
            float alphaFactor = (1.0f - progress) * (1.0f - progress);
            int alpha = (int)(102.0f * alphaFactor * masterAlpha);
            if (alpha <= 0) continue;
            ((BuiltRectangle)Builder.rectangle().size(new SizeState(w + spread * 2.0f, h + spread * 2.0f)).radius(new QuadRadiusState(radius + spread)).color(new QuadColorState(new Color(0, 0, 0, alpha))).build()).render(matrix, x - spread, y - spread + 1.0f);
        }
    }

    private void drawIconGlowShadow(Matrix4f matrix, float cx, float cy, float radius, Color color, float masterAlpha) {
        if (masterAlpha <= 0.001f) {
            return;
        }
        int steps = 10;
        float maxSpread = 8.0f;
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

    private double getScaledMouseX() {
        Minecraft mc = Minecraft.getInstance();
        double scaleModifier = this.getScaleModifier();
        return mc.mouseHandler.xpos() * (double)mc.getWindow().getGuiScaledWidth() / (double)mc.getWindow().getScreenWidth() / scaleModifier;
    }

    private double getScaledMouseY() {
        Minecraft mc = Minecraft.getInstance();
        double scaleModifier = this.getScaleModifier();
        return mc.mouseHandler.ypos() * (double)mc.getWindow().getGuiScaledHeight() / (double)mc.getWindow().getScreenHeight() / scaleModifier;
    }

    public boolean onMouseClicked(double mouseX, double mouseY, int button) {
        boolean isOverEffectHud;
        float currentCardX;
        Minecraft mc = Minecraft.getInstance();
        boolean isEditing = mc.gui.screen() instanceof HudEditorScreen;
        if (!isEditing) {
            return false;
        }
        float scaleModifier = this.getScaleModifier();
        double mx = mouseX / (double)scaleModifier;
        double my = mouseY / (double)scaleModifier;
        float screenW = (float)mc.getWindow().getGuiScaledWidth() / scaleModifier;
        float f = currentCardX = this.position == Position.RIGHT ? screenW - this.lastFrameTotalMaxWidth - 10.0f : 10.0f;
        if (this.panelOpen) {
            boolean inSub;
            float mainW = 110.0f;
            float mainH = 48.0f;
            float subW = 88.0f;
            float subH = 38.0f;
            float mX = this.position == Position.LEFT ? this.animatedX + this.lastFrameTotalMaxWidth + 8.0f : this.animatedX - mainW - (this.submenuOpen ? subW + 5.0f : 0.0f) - 8.0f;
            float mY = this.y;
            boolean inMain = mx >= (double)mX && mx <= (double)(mX + mainW) && my >= (double)mY && my <= (double)(mY + mainH);
            float sX = mX + mainW + 5.0f;
            if (sX + subW > screenW - 6.0f) {
                sX = mX - subW - 5.0f;
            }
            boolean bl = inSub = this.submenuOpen && mx >= (double)sX && mx <= (double)(sX + subW) && my >= (double)mY && my <= (double)(mY + subH);
            if (inMain || inSub) {
                return true;
            }
            if (button == 0 || button == 1) {
                this.panelOpen = false;
                this.submenuOpen = false;
            }
        }
        boolean bl = isOverEffectHud = mx >= (double)(currentCardX - 2.0f) && mx <= (double)(currentCardX + this.lastFrameTotalMaxWidth + 2.0f) && my >= (double)(this.y - 2.0f) && my <= (double)(this.y + this.totalHeight + 2.0f);
        if (button == 1 && isOverEffectHud) {
            boolean bl2 = this.panelOpen = !this.panelOpen;
            if (this.panelOpen) {
                this.mainPanelAnim = 0.0f;
                this.subPanelAnim = 0.0f;
                this.submenuOpen = false;
            } else {
                this.submenuOpen = false;
            }
            return true;
        }
        return false;
    }

    public void onMouseDragged(double mouseX, double mouseY, int button) {
    }

    public void onMouseReleased(double mouseX, double mouseY, int button) {
    }

    public void onMouseReleased(int button) {
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

    @Environment(value=EnvType.CLIENT)
    public static enum Position {
        LEFT,
        RIGHT;

    }

    @Environment(value=EnvType.CLIENT)
    private static class PotionCardState {
        float animProgress = 0.0f;
        float heightScale = 0.0f;
        float width = 80.0f;
        MobEffectInstance cachedInstance;
        TextAnimator durationAnimator = new TextAnimator();

        private PotionCardState() {
        }
    }

    @Environment(value=EnvType.CLIENT)
    private static class TextAnimator {
        private String currentStr = "";
        private final char[] oldChars = new char[32];
        private final long[] animStartTimes = new long[32];
        private static final long DURATION = 180L;

        private TextAnimator() {
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

        public void render(Matrix4f matrix, float startX, float y, Color textColor, MsdfFont font, float fontSize, float masterAlpha) {
            float currX = startX;
            long now = System.currentTimeMillis();
            for (int i = 0; i < this.currentStr.length(); ++i) {
                char cNew = this.currentStr.charAt(i);
                String strNew = String.valueOf(cNew);
                float charW = font.getWidth(strNew, fontSize);
                long elapsed = now - this.animStartTimes[i];
                if (this.oldChars[i] != '\u0000' && elapsed >= 0L && elapsed < 180L) {
                    float progress = (float)elapsed / 180.0f;
                    float easeOut = 1.0f - (float)Math.pow(1.0f - progress, 3.0);
                    char cOld = this.oldChars[i];
                    String strOld = String.valueOf(cOld);
                    int oldAlpha = Math.max(0, Math.min(255, (int)(255.0f * (1.0f - easeOut) * masterAlpha)));
                    Color oldColor = new Color(textColor.getRed(), textColor.getGreen(), textColor.getBlue(), oldAlpha);
                    float oldOffsetY = -3.5f * easeOut;
                    ((BuiltText)Builder.text().font(font).text(strOld).size(fontSize).color(oldColor).build()).render(matrix, currX, y + oldOffsetY);
                    int newAlpha = Math.max(0, Math.min(255, (int)(255.0f * easeOut * masterAlpha)));
                    Color newColor = new Color(textColor.getRed(), textColor.getGreen(), textColor.getBlue(), newAlpha);
                    float newOffsetY = 3.5f * (1.0f - easeOut);
                    ((BuiltText)Builder.text().font(font).text(strNew).size(fontSize).color(newColor).build()).render(matrix, currX, y + newOffsetY);
                } else {
                    this.oldChars[i] = '\u0000';
                    ((BuiltText)Builder.text().font(font).text(strNew).size(fontSize).color(textColor).build()).render(matrix, currX, y);
                }
                currX += charW;
            }
        }
    }
}

