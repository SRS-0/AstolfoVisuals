package xyz.angames.astolfoclient.client.hud;

import com.google.common.base.Supplier;
import dev.sxmurxy.mre.builders.Builder;
import dev.sxmurxy.mre.builders.states.QuadColorState;
import dev.sxmurxy.mre.builders.states.QuadRadiusState;
import dev.sxmurxy.mre.builders.states.SizeState;
import dev.sxmurxy.mre.msdf.MsdfFont;
import dev.sxmurxy.mre.renderers.impl.BuiltRectangle;
import dev.sxmurxy.mre.renderers.impl.BuiltText;
import java.awt.Color;
import java.util.ArrayList;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;
import org.joml.Matrix4f;
import org.lwjgl.glfw.GLFW;
import xyz.angames.astolfoclient.client.AstolfoclientClient;
import xyz.angames.astolfoclient.client.config.ThemeManager;
import xyz.angames.astolfoclient.client.gui.HudEditorScreen;
import xyz.angames.astolfoclient.client.gui.clickgui.GuiUtils;
import xyz.angames.astolfoclient.client.hud.LogoRenderer;
import xyz.angames.astolfoclient.client.module.modules.render.InterfaceModule;

@Environment(value=EnvType.CLIENT)
public class ArmorHudManager {
    private static final Supplier<MsdfFont> BOLD_FONT = LogoRenderer.BOLD_FONT;
    private static final Supplier<MsdfFont> MEDIUM_FONT = LogoRenderer.MEDIUM_FONT;
    private static final Supplier<MsdfFont> SP_FONT = LogoRenderer.SP_FONT;
    private static final Supplier<MsdfFont> WATERMARK_FONT = LogoRenderer.WATERMARK_FONT;
    public float x = 10.0f;
    public float y = 200.0f;
    private static final int SLOT_SIZE = 18;
    private static final int ITEM_GAP = 2;
    public static final float HORIZ_WIDTH = 78.0f;
    public static final float HORIZ_HEIGHT = 18.0f;
    public static final float VERT_WIDTH = 18.0f;
    public static final float VERT_HEIGHT = 78.0f;
    public static String layout = "VERTICAL";
    public static boolean warningGlow = true;
    private boolean dragging = false;
    private float dragOffsetX;
    private float dragOffsetY;
    private boolean panelOpen = false;
    private SubmenuType activeSubmenu = SubmenuType.NONE;
    private float mainPanelAnim = 0.0f;
    private float subPanelAnim = 0.0f;
    private float warningGlowSwitchAnim = 1.0f;
    private boolean wasMouseDown = false;
    private boolean wasRightMouseDown = false;
    private Object lastScreen = null;
    private long lastFrameTime = System.currentTimeMillis();
    private final Minecraft client = Minecraft.getInstance();

    public static boolean isVertical() {
        return layout != null && layout.equalsIgnoreCase("VERTICAL");
    }

    public float getWidth() {
        return ArmorHudManager.isVertical() ? 18.0f : 78.0f;
    }

    public float getHeight() {
        return ArmorHudManager.isVertical() ? 78.0f : 18.0f;
    }

    public void render(GuiGraphicsExtractor context) {
        boolean vertical;
        boolean inScreen;
        InterfaceModule interfaceMod = ArmorHudManager.getInterfaceModule();
        boolean isEditing = this.client.gui.screen() instanceof HudEditorScreen;
        boolean bl = inScreen = this.client.gui.screen() != null;
        if (interfaceMod == null) {
            return;
        }
        if (!(isEditing || interfaceMod.isEnabled() && interfaceMod.armorHud.get())) {
            this.panelOpen = false;
            this.activeSubmenu = SubmenuType.NONE;
            this.mainPanelAnim = 0.0f;
            this.subPanelAnim = 0.0f;
            return;
        }
        if (this.client.player == null) {
            return;
        }
        long now = System.currentTimeMillis();
        float deltaTime = (float)(now - this.lastFrameTime) / 1000.0f;
        this.lastFrameTime = now;
        if (deltaTime < 0.001f) {
            deltaTime = 0.001f;
        }
        if (deltaTime > 0.1f) {
            deltaTime = 0.1f;
        }
        float curW = (vertical = ArmorHudManager.isVertical()) ? 18.0f : 78.0f;
        float curH = vertical ? 78.0f : 18.0f;
        double currentGuiScale = this.client.getWindow().getGuiScale();
        if (currentGuiScale <= 0.0) {
            currentGuiScale = 2.0;
        }
        float scaleModifier = (float)(2.0 / currentGuiScale);
        context.pose().pushMatrix();
        context.pose().translate(this.x, this.y);
        context.pose().scale(scaleModifier, scaleModifier);
        context.pose().translate(-this.x, -this.y);
        ArrayList<ItemStack> armorList = new ArrayList<ItemStack>();
        boolean hasArmor = false;
        for (int i = 0; i < 4; ++i) {
            ItemStack stack = (ItemStack)this.client.player.getInventory().getItem(36 + i);
            armorList.add(stack);
            if (stack.isEmpty()) continue;
            hasArmor = true;
        }
        if (isEditing && !hasArmor) {
            armorList.set(0, new ItemStack((ItemLike)Items.DIAMOND_BOOTS));
            armorList.set(1, new ItemStack((ItemLike)Items.DIAMOND_LEGGINGS));
            ItemStack damagedChest = new ItemStack((ItemLike)Items.DIAMOND_CHESTPLATE);
            damagedChest.setDamageValue(damagedChest.getMaxDamage() - 10);
            armorList.set(2, damagedChest);
            armorList.set(3, new ItemStack((ItemLike)Items.DIAMOND_HELMET));
        }
        float startX = this.x;
        float startY = this.y;
        context.pose().pushMatrix();
        context.pose().translate(0.0f, 0.0f);
        for (int i = 3; i >= 0; --i) {
            float durabilityPercent;
            ItemStack stack = (ItemStack)armorList.get(i);
            if (stack.isEmpty()) continue;
            int index = 3 - i;
            float drawX = vertical ? startX : startX + (float)(index * 20);
            float drawY = vertical ? startY + (float)(index * 20) : startY;
            boolean almostBroken = false;
            if (warningGlow && stack.isDamageableItem() && stack.getMaxDamage() > 0 && (durabilityPercent = (float)(stack.getMaxDamage() - stack.getDamageValue()) / (float)stack.getMaxDamage()) <= 0.15f) {
                almostBroken = true;
            }
            context.pose().pushMatrix();
            if (almostBroken) {
                float pulseAlpha = (float)Math.abs(Math.sin((double)System.nanoTime() / 2.0E8));
                float bounceOffset = pulseAlpha * -3.0f;
                context.pose().translate(0.0f, bounceOffset);
                Matrix4f glowMat = xyz.angames.astolfoclient.client.render.GuiTransforms.matrix(context.pose());
                for (int j = 0; j < 4; ++j) {
                    float expand = (float)(j + 1) * 2.0f;
                    int alpha = (int)(255.0f * (0.3f - (float)j * 0.07f) * (0.4f + 0.6f * pulseAlpha));
                    if (alpha <= 0) continue;
                    ((BuiltRectangle)Builder.rectangle().size(new SizeState(16.0f + expand, 16.0f + expand)).radius(new QuadRadiusState(3.5f + expand / 2.0f)).color(new QuadColorState(new Color(255, 40, 40, alpha))).build()).render(glowMat, drawX - expand / 2.0f, drawY - expand / 2.0f);
                }
            }
            context.item(stack, (int)drawX, (int)drawY);
            context.itemDecorations(this.client.font, stack, (int)drawX, (int)drawY);
            context.pose().popMatrix();
        }
        context.pose().popMatrix();
        context.pose().popMatrix();
        Screen activeScreen = this.client.gui.screen();
        if (activeScreen != this.lastScreen) {
            this.wasMouseDown = false;
            this.wasRightMouseDown = false;
            this.lastScreen = activeScreen;
        }
        if (inScreen) {
            double mouseScaledX = -9999.0;
            double mouseScaledY = -9999.0;
            boolean isMouseDown = false;
            boolean isRightMouseDown = false;
            long win = this.client.getWindow().handle();
            if (win != 0L) {
                double[] mx = new double[1];
                double[] my = new double[1];
                GLFW.glfwGetCursorPos((long)win, (double[])mx, (double[])my);
                double screenWidth = this.client.getWindow().getWidth();
                double screenHeight = this.client.getWindow().getHeight();
                float guiWidth = this.client.getWindow().getGuiScaledWidth();
                float guiHeight = this.client.getWindow().getGuiScaledHeight();
                if (screenWidth > 0.0 && screenHeight > 0.0) {
                    mouseScaledX = mx[0] / screenWidth * (double)guiWidth;
                    mouseScaledY = my[0] / screenHeight * (double)guiHeight;
                }
                isMouseDown = GLFW.glfwGetMouseButton((long)win, (int)0) == 1;
                boolean bl2 = isRightMouseDown = GLFW.glfwGetMouseButton((long)win, (int)1) == 1;
            }
            if (isRightMouseDown && !this.wasRightMouseDown && !(this.client.gui.screen() instanceof HudEditorScreen)) {
                float visMinX = this.x;
                float visMinY = this.y;
                float visMaxX = this.x + curW * scaleModifier;
                float visMaxY = this.y + curH * scaleModifier;
                if (mouseScaledX >= (double)(visMinX - 4.0f) && mouseScaledX <= (double)(visMaxX + 4.0f) && mouseScaledY >= (double)(visMinY - 4.0f) && mouseScaledY <= (double)(visMaxY + 4.0f)) {
                    boolean bl3 = this.panelOpen = !this.panelOpen;
                    if (this.panelOpen) {
                        this.activeSubmenu = SubmenuType.NONE;
                    }
                }
            }
            Color themeColor = new Color(ThemeManager.getThemedColor(now / 10L));
            this.drawAnimatedContextPanel(context, this.x, this.y, curW, curH, scaleModifier, this.client.getWindow().getGuiScaledWidth(), this.client.getWindow().getGuiScaledHeight(), themeColor, mouseScaledX, mouseScaledY, isMouseDown, this.wasMouseDown, deltaTime);
            this.wasMouseDown = isMouseDown;
            this.wasRightMouseDown = isRightMouseDown;
        } else {
            this.panelOpen = false;
            this.activeSubmenu = SubmenuType.NONE;
            this.mainPanelAnim = 0.0f;
            this.subPanelAnim = 0.0f;
        }
    }

    private void drawAnimatedContextPanel(GuiGraphicsExtractor context, float aX, float aY, float aW, float aH, float scaleFactor, float guiWidth, float guiHeight, Color themeColor, double mouseX, double mouseY, boolean isMouseDown, boolean wasMouseDown, float deltaTime) {
        boolean wgHov;
        boolean r1Active;
        Color closeColor;
        float maxPanelH;
        float targetMain = this.panelOpen ? 1.0f : 0.0f;
        this.mainPanelAnim = GuiUtils.animate(this.mainPanelAnim, targetMain, 16.0f, deltaTime);
        if (!this.panelOpen && this.mainPanelAnim < 0.01f) {
            this.mainPanelAnim = 0.0f;
            return;
        }
        boolean hasSub = this.activeSubmenu != SubmenuType.NONE;
        float targetSub = hasSub && this.panelOpen ? 1.0f : 0.0f;
        this.subPanelAnim = GuiUtils.animate(this.subPanelAnim, targetSub, 16.0f, deltaTime);
        float mainW = 110.0f;
        float mainH = 70.0f;
        float pX = aX;
        float pY = aY + aH * scaleFactor + 6.0f;
        float subW = 95.0f;
        String[] modes = new String[]{"VERTICAL", "HORIZONTAL"};
        String[] labels = new String[]{"Vertical", "Horizontal"};
        float subH = (float)modes.length * 16.0f + 8.0f;
        float f = hasSub ? subW + 5.0f : 0.0f;
        if (pX + mainW + f > guiWidth - 6.0f) {
            pX = guiWidth - mainW - (hasSub ? subW + 5.0f : 0.0f) - 6.0f;
        }
        if (pX < 6.0f) {
            pX = 6.0f;
        }
        float f2 = maxPanelH = hasSub ? Math.max(mainH, subH) : mainH;
        if (pY + maxPanelH > guiHeight - 6.0f) {
            pY = aY - maxPanelH - 6.0f;
        }
        if (pY < 6.0f) {
            pY = 6.0f;
        }
        MsdfFont boldFont = (MsdfFont)BOLD_FONT.get();
        MsdfFont mediumFont = (MsdfFont)MEDIUM_FONT.get();
        MsdfFont spFont = (MsdfFont)SP_FONT.get();
        MsdfFont watermarkFont = (MsdfFont)WATERMARK_FONT.get();
        float mainEase = 1.0f - (float)Math.pow(1.0f - this.mainPanelAnim, 3.0);
        float mainScale = 0.88f + 0.12f * mainEase;
        context.pose().pushMatrix();
        if (mainScale < 0.999f) {
            float cx = pX + mainW / 2.0f;
            float cy = pY + mainH / 2.0f;
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
            ((BuiltRectangle)Builder.rectangle().size(new SizeState(mainW + spread * 2.0f, mainH + spread * 2.0f)).radius(new QuadRadiusState(8.0f + spread)).color(new QuadColorState(new Color(0, 0, 0, alpha))).build()).render(matrix, pX - spread, pY - spread);
        }
        Color bgMain = new Color(0, 0, 0, (int)(255.0f * mainEase));
        ((BuiltRectangle)Builder.rectangle().size(new SizeState(mainW, mainH)).radius(new QuadRadiusState(8.0f)).color(new QuadColorState(bgMain)).build()).render(matrix, pX, pY);
        if (boldFont != null) {
            ((BuiltText)Builder.text().font(boldFont).text("Armor HUD").size(7.5f).color(new Color(255, 255, 255, (int)(255.0f * mainEase))).build()).render(matrix, pX + 8.0f, pY + 6.5f);
        }
        float closeX = pX + mainW - 14.0f;
        float closeY = pY + 6.5f;
        boolean closeHover = mouseX >= (double)(closeX - 2.0f) && mouseX <= (double)(closeX + 10.0f) && mouseY >= (double)(closeY - 2.0f) && mouseY <= (double)(closeY + 10.0f);
        Color color = closeColor = closeHover ? new Color(255, 80, 80, (int)(255.0f * mainEase)) : new Color(140, 140, 150, (int)(255.0f * mainEase));
        if (spFont != null) {
            ((BuiltText)Builder.text().font(spFont).text("A").size(7.0f).color(closeColor).build()).render(matrix, closeX, closeY);
        }
        if (closeHover && isMouseDown && !wasMouseDown) {
            this.panelOpen = false;
            this.activeSubmenu = SubmenuType.NONE;
            context.pose().popMatrix();
            return;
        }
        float c1X = pX + 4.0f;
        float c1Y = pY + 20.0f;
        float c1W = mainW - 8.0f;
        float c1H = 20.0f;
        ((BuiltRectangle)Builder.rectangle().size(new SizeState(c1W, c1H)).radius(new QuadRadiusState(6.0f)).color(new QuadColorState(new Color(10, 10, 15, (int)(220.0f * mainEase)))).build()).render(matrix, c1X, c1Y);
        float r1Y = c1Y + 2.0f;
        float r1H = 16.0f;
        boolean r1Hov = mouseX >= (double)c1X && mouseX <= (double)(c1X + c1W) && mouseY >= (double)r1Y && mouseY <= (double)(r1Y + r1H);
        boolean bl = r1Active = this.activeSubmenu == SubmenuType.LAYOUT;
        if (r1Hov) {
            ((BuiltRectangle)Builder.rectangle().size(new SizeState(c1W - 4.0f, r1H)).radius(new QuadRadiusState(4.0f)).color(new QuadColorState(new Color(22, 22, 32, (int)(180.0f * mainEase)))).build()).render(matrix, c1X + 2.0f, r1Y);
        }
        if (spFont != null) {
            ((BuiltText)Builder.text().font(spFont).text("B").size(7.5f).color(new Color(170, 170, 190, (int)(255.0f * mainEase))).build()).render(matrix, c1X + 5.0f, r1Y + 4.0f);
        }
        if (mediumFont != null) {
            ((BuiltText)Builder.text().font(mediumFont).text("Layout").size(6.5f).color(new Color(230, 230, 245, (int)(255.0f * mainEase))).build()).render(matrix, c1X + 16.0f, r1Y + 5.5f);
            String currentLayoutName = (ArmorHudManager.isVertical() ? "Vertical" : "Horizontal") + " >";
            float layW = mediumFont.getWidth(currentLayoutName, 6.0f);
            Color layCol = r1Active ? GuiUtils.withAlpha(themeColor, mainEase) : new Color(140, 140, 160, (int)(255.0f * mainEase));
            ((BuiltText)Builder.text().font(mediumFont).text(currentLayoutName).size(6.0f).color(layCol).build()).render(matrix, c1X + c1W - layW - 5.0f, r1Y + 5.5f);
        }
        if (r1Hov && isMouseDown && !wasMouseDown) {
            this.activeSubmenu = this.activeSubmenu == SubmenuType.LAYOUT ? SubmenuType.NONE : SubmenuType.LAYOUT;
        }
        float c2X = pX + 4.0f;
        float c2Y = pY + 44.0f;
        float c2W = mainW - 8.0f;
        float c2H = 22.0f;
        ((BuiltRectangle)Builder.rectangle().size(new SizeState(c2W, c2H)).radius(new QuadRadiusState(6.0f)).color(new QuadColorState(new Color(10, 10, 15, (int)(220.0f * mainEase)))).build()).render(matrix, c2X, c2Y);
        boolean wgVal = warningGlow;
        float targetSwitch = wgVal ? 1.0f : 0.0f;
        this.warningGlowSwitchAnim = GuiUtils.animate(this.warningGlowSwitchAnim, targetSwitch, 16.0f, deltaTime);
        float wgY = c2Y + 3.0f;
        float wgH = 16.0f;
        boolean bl2 = wgHov = mouseX >= (double)c2X && mouseX <= (double)(c2X + c2W) && mouseY >= (double)wgY && mouseY <= (double)(wgY + wgH);
        if (wgHov) {
            ((BuiltRectangle)Builder.rectangle().size(new SizeState(c2W - 4.0f, wgH)).radius(new QuadRadiusState(4.0f)).color(new QuadColorState(new Color(22, 22, 32, (int)(180.0f * mainEase)))).build()).render(matrix, c2X + 2.0f, wgY);
        }
        if (watermarkFont != null) {
            ((BuiltText)Builder.text().font(watermarkFont).text("G").size(7.5f).color(new Color(180, 180, 200, (int)(255.0f * mainEase))).build()).render(matrix, c2X + 5.0f, wgY + 4.0f);
        }
        if (mediumFont != null) {
            ((BuiltText)Builder.text().font(mediumFont).text("Warning Glow").size(6.5f).color(new Color(230, 230, 245, (int)(255.0f * mainEase))).build()).render(matrix, c2X + 16.0f, wgY + 5.5f);
        }
        float swW = 20.0f;
        float swH = 11.0f;
        float swX = c2X + c2W - swW - 5.0f;
        float swY = wgY + 2.5f;
        Color activeThemeCol = GuiUtils.withAlpha(themeColor, mainEase);
        Color offThemeCol = new Color(0, 0, 0, (int)(255.0f * mainEase));
        Color swBg = GuiUtils.interpolateColor(offThemeCol, activeThemeCol, this.warningGlowSwitchAnim);
        ((BuiltRectangle)Builder.rectangle().size(new SizeState(swW, swH)).radius(new QuadRadiusState(swH / 2.0f)).color(new QuadColorState(swBg)).build()).render(matrix, swX, swY);
        float baseKnobRadius = 3.8f;
        float offKnobX = swX + baseKnobRadius + 1.8f;
        float onKnobX = swX + swW - baseKnobRadius - 1.8f;
        float currentKnobCX = offKnobX + (onKnobX - offKnobX) * this.warningGlowSwitchAnim;
        float knobCY = swY + swH / 2.0f;
        float stretch = (float)Math.sin((double)this.warningGlowSwitchAnim * Math.PI) * 3.2f;
        float knobW = baseKnobRadius * 2.0f + stretch;
        float knobH = baseKnobRadius * 2.0f - stretch * 0.35f;
        float knobX0 = currentKnobCX - knobW / 2.0f;
        float knobY0 = knobCY - knobH / 2.0f;
        ((BuiltRectangle)Builder.rectangle().size(new SizeState(knobW, knobH)).radius(new QuadRadiusState(knobH / 2.0f)).color(new QuadColorState(new Color(255, 255, 255, (int)(255.0f * mainEase)))).build()).render(matrix, knobX0, knobY0);
        if (wgHov && isMouseDown && !wasMouseDown) {
            warningGlow = !warningGlow;
        }
        context.pose().popMatrix();
        if (this.subPanelAnim > 0.005f) {
            float subEase = 1.0f - (float)Math.pow(1.0f - this.subPanelAnim, 3.0);
            float subScale = 0.88f + 0.12f * subEase;
            float sX = pX + mainW + 5.0f;
            float sY = pY;
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
            if (this.activeSubmenu == SubmenuType.LAYOUT) {
                float rowH = 16.0f;
                float listStartY = sY + 4.0f;
                for (int i = 0; i < modes.length; ++i) {
                    boolean rHov;
                    float rY = listStartY + (float)i * rowH;
                    boolean isSel = layout.equalsIgnoreCase(modes[i]);
                    boolean bl3 = rHov = mouseX >= (double)(sX + 3.0f) && mouseX <= (double)(sX + subW - 3.0f) && mouseY >= (double)rY && mouseY <= (double)(rY + rowH - 1.0f);
                    if (rHov) {
                        ((BuiltRectangle)Builder.rectangle().size(new SizeState(subW - 6.0f, rowH - 1.0f)).radius(new QuadRadiusState(4.0f)).color(new QuadColorState(new Color(25, 25, 36, (int)(220.0f * subEase)))).build()).render(matrix, sX + 3.0f, rY);
                    }
                    if (isSel && spFont != null) {
                        ((BuiltText)Builder.text().font(spFont).text("D").size(6.5f).color(new Color(255, 255, 255, (int)(255.0f * subEase))).build()).render(matrix, sX + 6.0f, rY + 4.5f);
                    }
                    if (mediumFont != null) {
                        Color textCol = isSel ? new Color(255, 255, 255, (int)(255.0f * subEase)) : (rHov ? new Color(200, 200, 220, (int)(255.0f * subEase)) : new Color(130, 130, 150, (int)(255.0f * subEase)));
                        ((BuiltText)Builder.text().font(mediumFont).text(labels[i]).size(6.5f).color(textCol).build()).render(matrix, sX + 20.0f, rY + 5.5f);
                    }
                    if (!rHov || !isMouseDown || wasMouseDown) continue;
                    layout = modes[i];
                }
            }
            context.pose().popMatrix();
        }
    }

    public boolean onMouseClicked(double mouseX, double mouseY, int button) {
        boolean isEditing = this.client.gui.screen() instanceof HudEditorScreen;
        InterfaceModule interfaceMod = ArmorHudManager.getInterfaceModule();
        if (interfaceMod == null) {
            return false;
        }
        if (!(isEditing || interfaceMod.isEnabled() && interfaceMod.armorHud.get())) {
            return false;
        }
        float w = this.getWidth();
        float h = this.getHeight();
        double currentGuiScale = this.client.getWindow().getGuiScale();
        if (currentGuiScale <= 0.0) {
            currentGuiScale = 2.0;
        }
        float scaleModifier = (float)(2.0 / currentGuiScale);
        float visMinX = this.x;
        float visMinY = this.y;
        float visMaxX = this.x + w * scaleModifier;
        float visMaxY = this.y + h * scaleModifier;
        if (this.panelOpen) {
            boolean inSub;
            float maxPanelH;
            float mainW = 110.0f;
            float mainH = 70.0f;
            float subW = 95.0f;
            float subH = 40.0f;
            boolean hasSub = this.activeSubmenu != SubmenuType.NONE;
            float pX = this.x;
            float pY = this.y + h * scaleModifier + 6.0f;
            float guiWidth = this.client.getWindow().getGuiScaledWidth();
            float guiHeight = this.client.getWindow().getGuiScaledHeight();
            float f = hasSub ? subW + 5.0f : 0.0f;
            if (pX + mainW + f > guiWidth - 6.0f) {
                pX = guiWidth - mainW - (hasSub ? subW + 5.0f : 0.0f) - 6.0f;
            }
            if (pX < 6.0f) {
                pX = 6.0f;
            }
            float f2 = maxPanelH = hasSub ? Math.max(mainH, subH) : mainH;
            if (pY + maxPanelH > guiHeight - 6.0f) {
                pY = this.y - maxPanelH - 6.0f;
            }
            if (pY < 6.0f) {
                pY = 6.0f;
            }
            boolean inMain = mouseX >= (double)pX && mouseX <= (double)(pX + mainW) && mouseY >= (double)pY && mouseY <= (double)(pY + mainH);
            boolean bl = inSub = hasSub && mouseX >= (double)(pX + mainW + 5.0f) && mouseX <= (double)(pX + mainW + 5.0f + subW) && mouseY >= (double)pY && mouseY <= (double)(pY + subH);
            if (inMain || inSub) {
                return true;
            }
        }
        if (button == 1 && mouseX >= (double)(visMinX - 4.0f) && mouseX <= (double)(visMaxX + 4.0f) && mouseY >= (double)(visMinY - 4.0f) && mouseY <= (double)(visMaxY + 4.0f)) {
            boolean bl = this.panelOpen = !this.panelOpen;
            if (this.panelOpen) {
                this.activeSubmenu = SubmenuType.NONE;
            }
            this.wasRightMouseDown = true;
            return true;
        }
        if (button == 0 && mouseX >= (double)(visMinX - 4.0f) && mouseX <= (double)(visMaxX + 4.0f) && mouseY >= (double)(visMinY - 4.0f) && mouseY <= (double)(visMaxY + 4.0f)) {
            this.dragging = true;
            this.dragOffsetX = (float)(mouseX - (double)this.x);
            this.dragOffsetY = (float)(mouseY - (double)this.y);
            return true;
        }
        return false;
    }

    public void onMouseDragged(double mouseX, double mouseY, int button) {
        if (this.dragging && button == 0) {
            double currentGuiScale = this.client.getWindow().getGuiScale();
            if (currentGuiScale <= 0.0) {
                currentGuiScale = 2.0;
            }
            float scaleModifier = (float)(2.0 / currentGuiScale);
            float screenW = this.client.getWindow().getGuiScaledWidth();
            float screenH = this.client.getWindow().getGuiScaledHeight();
            float w = this.getWidth();
            float h = this.getHeight();
            float targetX = (float)(mouseX - (double)this.dragOffsetX);
            float targetY = (float)(mouseY - (double)this.dragOffsetY);
            this.x = Math.max(0.0f, Math.min(Math.max(0.0f, screenW - w * scaleModifier), targetX));
            this.y = Math.max(0.0f, Math.min(Math.max(0.0f, screenH - h * scaleModifier), targetY));
        }
    }

    public void onMouseReleased(int button) {
        if (this.dragging && button == 0) {
            this.dragging = false;
        }
    }

    public void onMouseReleased(double mouseX, double mouseY, int button) {
        if (this.dragging && button == 0) {
            this.dragging = false;
        }
    }

    private static InterfaceModule getInterfaceModule() {
        if (AstolfoclientClient.moduleManager == null) {
            return null;
        }
        return (InterfaceModule)AstolfoclientClient.moduleManager.getModuleByName("Interface");
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
    private static enum SubmenuType {
        NONE,
        LAYOUT;

    }
}

