package xyz.angames.astolfoclient.client.gui;

import com.google.common.base.Supplier;
import dev.sxmurxy.mre.builders.Builder;
import dev.sxmurxy.mre.builders.states.QuadColorState;
import dev.sxmurxy.mre.builders.states.QuadRadiusState;
import dev.sxmurxy.mre.builders.states.SizeState;
import dev.sxmurxy.mre.msdf.MsdfFont;
import dev.sxmurxy.mre.renderers.impl.BuiltRectangle;
import dev.sxmurxy.mre.renderers.impl.BuiltText;
import dev.sxmurxy.mre.renderers.impl.BuiltTexture;
import java.awt.Color;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import org.joml.Matrix4f;
import xyz.angames.astolfoclient.client.AstolfoclientClient;
import xyz.angames.astolfoclient.client.DiscordRpcManager;
import xyz.angames.astolfoclient.client.config.GuiScaleSettings;
import xyz.angames.astolfoclient.client.config.SoundSettings;
import xyz.angames.astolfoclient.client.config.ThemeManager;
import xyz.angames.astolfoclient.client.gui.HudEditorScreen;
import xyz.angames.astolfoclient.client.gui.clickgui.ClickGuiIcons;
import xyz.angames.astolfoclient.client.gui.clickgui.ColorPickerComponent;
import xyz.angames.astolfoclient.client.gui.clickgui.GuiUtils;
import xyz.angames.astolfoclient.client.gui.clickgui.ModePopupState;
import xyz.angames.astolfoclient.client.gui.clickgui.ModuleButton;
import xyz.angames.astolfoclient.client.gui.clickgui.MultiSelectPopupState;
import xyz.angames.astolfoclient.client.gui.clickgui.ScaleSettingsPopup;
import xyz.angames.astolfoclient.client.gui.clickgui.SoundSettingsPopup;
import xyz.angames.astolfoclient.client.gui.clickgui.SubSettingsPopupState;
import xyz.angames.astolfoclient.client.module.Module;
import xyz.angames.astolfoclient.client.module.modules.render.HandPositionModule;
import xyz.angames.astolfoclient.client.util.DiscordAvatarManager;
import xyz.angames.astolfoclient.client.util.ModSounds;

@Environment(value=EnvType.CLIENT)
public class ClickGuiScreen
extends Screen {
    private final List<ModuleButton> allModuleButtons;
    private static Method renderHandMethod = null;
    private NavTab activeTab = NavTab.RENDER;
    private RenderSubcategory activeRenderSubcategory = RenderSubcategory.ALL;
    private float renderExpandAnim = 1.0f;
    private RenderSubcategory lastHoveredRenderSubcategory = null;
    private float animatedTabPillY = -1.0f;
    public boolean isSearching = false;
    private String searchText = "";
    private float searchFocusAnim = 0.0f;
    private long lastTypingTime = 0L;
    public static final float GUI_WIDTH = 580.0f;
    public static final float GUI_HEIGHT = 380.0f;
    public static final float SIDEBAR_WIDTH = 138.0f;
    private final Map<NavTab, Float> scrollOffsets = new HashMap<NavTab, Float>();
    private final Map<NavTab, Float> targetScrollOffsets = new HashMap<NavTab, Float>();
    private final ColorPickerComponent themePicker = new ColorPickerComponent("Theme Color");
    private String selectedConfig = "default";
    private String newConfigInput = "";
    private boolean isTypingNewConfig = false;
    private List<String> localConfigs = new ArrayList<String>();
    private final List<String> cloudConfigs = new ArrayList<String>();
    private String configStatusMessage = "";
    private long configStatusTime = 0L;
    private float configScroll = 0.0f;
    private boolean settingsModalOpen = false;
    private float settingsModalAnim = 0.0f;
    private boolean colorPickerOpen = false;
    private float colorPickerAnim = 0.0f;
    private boolean soundSettingsOpen = false;
    private float soundSettingsAnim = 0.0f;
    private boolean scaleModalOpen = false;
    private float scaleModalAnim = 0.0f;
    public static final float SETTINGS_MODAL_W = 106.0f;
    public static final float SETTINGS_MODAL_H = 106.0f;
    public static final float COLOR_PICKER_W = 106.0f;
    public static final float COLOR_PICKER_H = 94.0f;
    public static final float SOUND_SETTINGS_W = 138.0f;
    public static final float SOUND_SETTINGS_H = 190.0f;
    public static final float SCALE_SETTINGS_W = 94.0f;
    public static final float SCALE_SETTINGS_H = 162.0f;
    private NavTab lastHoveredTab = null;
    private ModuleButton lastHoveredModuleButton = null;
    private static volatile String discordUsername = "";
    private static volatile String discordAvatarUrl = "";
    private final Map<String, Float> hoverAnimations = new HashMap<String, Float>();
    private long initTime;
    private long lastFrameTime = 0L;
    private float openAnimProgress = 0.0f;
    private static final Supplier<MsdfFont> SEMIBOLD_FONT = ClickGuiIcons.SEMIBOLD_FONT;
    private static final Supplier<MsdfFont> MEDIUM_FONT = ClickGuiIcons.MEDIUM_FONT;

    public float getSettingsModalX(float winX) {
        return winX + 138.0f + 6.0f;
    }

    public float getSettingsModalY(float winY) {
        return winY + 380.0f - 106.0f - 12.0f;
    }

    public float getSettingsRowY(float popY, int row) {
        return popY + 5.0f + (float)row * 24.0f;
    }

    public float getColorPickerX(float winX) {
        return this.getSettingsModalX(winX) + 106.0f + 6.0f;
    }

    public float getColorPickerY(float winY) {
        float popY = this.getSettingsModalY(winY);
        return MathHelper.clamp((float)(popY - 10.0f), (float)(winY + 38.0f), (float)(winY + 380.0f - 94.0f - 8.0f));
    }

    public float getSoundSettingsX(float winX) {
        return this.getSettingsModalX(winX) + 106.0f + 6.0f;
    }

    public float getSoundSettingsY(float winY) {
        float popY = this.getSettingsModalY(winY);
        return MathHelper.clamp((float)(popY - 16.0f), (float)(winY + 38.0f), (float)(winY + 380.0f - 190.0f - 8.0f));
    }

    public float getScaleSettingsX(float winX) {
        return this.getSettingsModalX(winX) + 106.0f + 6.0f;
    }

    public float getScaleSettingsY(float winY) {
        float popY = this.getSettingsModalY(winY);
        return MathHelper.clamp((float)(popY - 10.0f), (float)(winY + 38.0f), (float)(winY + 380.0f - 162.0f - 8.0f));
    }

    public boolean isAnyModalOpen() {
        return this.settingsModalOpen || this.colorPickerOpen || this.soundSettingsOpen || this.scaleModalOpen || ModePopupState.isOpen() || SubSettingsPopupState.isOpen() || MultiSelectPopupState.isOpen();
    }

    public boolean isAnyTextInputFocused() {
        return this.isSearching || this.isTypingNewConfig;
    }

    public ClickGuiScreen() {
        super((Text)Text.literal((String)"AstolfoClient"));
        this.allModuleButtons = AstolfoclientClient.moduleManager.getModules().stream().map(m -> new ModuleButton((Module)m, 205.0f)).collect(Collectors.toList());
        for (NavTab tab : NavTab.values()) {
            this.scrollOffsets.put(tab, Float.valueOf(0.0f));
            this.targetScrollOffsets.put(tab, Float.valueOf(0.0f));
        }
    }

    public static void setDiscordUser(String username, String userId, String avatarHash) {
        String string = discordUsername = username != null ? username : "";
        if (avatarHash != null && !avatarHash.isEmpty() && userId != null && !userId.isEmpty()) {
            discordAvatarUrl = "https://cdn.discordapp.com/avatars/" + userId + "/" + avatarHash + ".png?size=256";
        }
        DiscordAvatarManager.update(username, userId, avatarHash);
    }

    private float effectiveScale() {
        float requested = GuiUtils.getScaleModifier(this.client) * GuiScaleSettings.getScale();
        return Math.min(requested, Math.min(this.width / 600.0f, this.height / 400.0f));
    }

    protected void init() {
        this.initTime = System.currentTimeMillis();
        this.lastFrameTime = System.currentTimeMillis();
        this.openAnimProgress = 0.0f;
        this.animatedTabPillY = -1.0f;
        this.isSearching = false;
        this.searchText = "";
        this.searchFocusAnim = 0.0f;
        this.lastHoveredTab = null;
        this.lastHoveredRenderSubcategory = null;
        this.lastHoveredModuleButton = null;
        this.isTypingNewConfig = false;
        this.newConfigInput = "";
        this.renderExpandAnim = this.activeTab == NavTab.RENDER ? 1.0f : 0.0f;
        this.reloadConfigs();
        if (DiscordRpcManager.discordUsername != null && !DiscordRpcManager.discordUsername.isEmpty()) {
            discordUsername = DiscordRpcManager.discordUsername;
        }
        this.refreshModuleButtons();
        this.themePicker.loadColor();
        ModSounds.playGuiOpen();
    }

    public static RenderSubcategory getRenderSubcategory(Module module) {
        String name;
        if (module == null) {
            return RenderSubcategory.ALL;
        }
        switch (name = module.getName().toLowerCase()) {
            case "targetesp": 
            case "hitesp": 
            case "hitglow": 
            case "damageindicators": 
            case "killeffect": 
            case "trajectories": {
                return RenderSubcategory.PVP;
            }
            case "ambients": 
            case "blockoutline": 
            case "cubeparticles": 
            case "fireflies": 
            case "rain": 
            case "fullbright": 
            case "itemphysics": 
            case "lineglyphs": {
                return RenderSubcategory.WORLD;
            }
            case "interface": 
            case "norender": 
            case "crosshair": {
                return RenderSubcategory.INTERFACE;
            }
        }
        return RenderSubcategory.PLAYER;
    }

    private int getModuleCount(RenderSubcategory sub) {
        if (AstolfoclientClient.moduleManager == null) {
            return 0;
        }
        if (sub == RenderSubcategory.ALL) {
            return (int)AstolfoclientClient.moduleManager.getModules().stream().filter(m -> m.getCategory() == Module.Category.RENDER).count();
        }
        return (int)AstolfoclientClient.moduleManager.getModules().stream().filter(m -> m.getCategory() == Module.Category.RENDER && ClickGuiScreen.getRenderSubcategory(m) == sub).count();
    }

    public void refreshModuleButtons() {
        if (AstolfoclientClient.moduleManager == null) {
            return;
        }
        List<Module> currentMods = AstolfoclientClient.moduleManager.getModules();
        this.allModuleButtons.removeIf(btn -> !currentMods.contains(btn.module));
        for (Module m : currentMods) {
            boolean exists = this.allModuleButtons.stream().anyMatch(btn -> btn.module == m || btn.module.getName().equalsIgnoreCase(m.getName()));
            if (exists) continue;
            this.allModuleButtons.add(new ModuleButton(m, 205.0f));
        }
    }

    private void reloadConfigs() {
        if (AstolfoclientClient.configManager != null) {
            this.localConfigs = AstolfoclientClient.configManager.getLocalConfigs();
            if (!this.localConfigs.contains("default")) {
                this.localConfigs.add(0, "default");
            }
            if (!this.localConfigs.contains(this.selectedConfig) && !this.localConfigs.isEmpty()) {
                this.selectedConfig = this.localConfigs.get(0);
            }
        }
    }

    public boolean shouldPause() {
        return false;
    }

    public boolean shouldCloseOnEsc() {
        return false;
    }

    public void close() {
        this.settingsModalOpen = false;
        this.colorPickerOpen = false;
        this.soundSettingsOpen = false;
        this.scaleModalOpen = false;
        ModePopupState.close();
        SubSettingsPopupState.close();
        MultiSelectPopupState.close();
        super.close();
    }

    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderHandLogic(context, delta);
        long now = System.currentTimeMillis();
        if (this.lastFrameTime == 0L) {
            this.lastFrameTime = now;
        }
        float deltaTime = (float)(now - this.lastFrameTime) / 1000.0f;
        this.lastFrameTime = now;
        deltaTime = MathHelper.clamp((float)deltaTime, (float)5.0E-4f, (float)0.1f);
        for (ModuleButton mb : this.allModuleButtons) {
            mb.isVisible = false;
        }
        float sm = this.effectiveScale();
        int adjMouseX = (int)((float)mouseX / sm);
        int adjMouseY = (int)((float)mouseY / sm);
        context.getMatrices().pushMatrix();
        context.getMatrices().scale(sm, sm);
        Matrix4f mx = xyz.angames.astolfoclient.client.render.GuiTransforms.matrix(context.getMatrices());
        float scaledWidth = (float)this.width / sm;
        float scaledHeight = (float)this.height / sm;
        float alpha = this.openAnimProgress = GuiUtils.animate(this.openAnimProgress, 1.0f, 14.0f, deltaTime);
        float winX = (scaledWidth - 580.0f) / 2.0f;
        float winY = (scaledHeight - 380.0f) / 2.0f;
        if (this.openAnimProgress < 0.999f) {
            float cx = winX + 290.0f;
            float cy = winY + 190.0f;
            float scale = 0.92f + 0.08f * this.openAnimProgress;
            context.getMatrices().translate(cx, cy);
            context.getMatrices().scale(scale, scale);
            context.getMatrices().translate(-cx, -cy);
            mx = xyz.angames.astolfoclient.client.render.GuiTransforms.matrix(context.getMatrices());
        }
        Color themeColor = new Color(ThemeManager.getThemedColor(System.currentTimeMillis() / 10L));
        this.renderShadow(mx, winX, winY, 580.0f, 380.0f, 10.0f, alpha);
        ((BuiltRectangle)Builder.rectangle().size(new SizeState(580.0f, 380.0f)).radius(new QuadRadiusState(10.0f)).color(new QuadColorState(new Color(0, 0, 0, 255))).build()).render(mx, winX, winY);
        this.drawSidebar(context, mx, winX, winY, adjMouseX, adjMouseY, deltaTime, alpha, themeColor);
        this.drawTopBar(context, mx, winX, winY, adjMouseX, adjMouseY, deltaTime, alpha, themeColor);
        this.drawMainContent(context, mx, winX, winY, adjMouseX, adjMouseY, deltaTime, alpha, themeColor);
        this.settingsModalAnim = GuiUtils.animate(this.settingsModalAnim, this.settingsModalOpen ? 1.0f : 0.0f, 18.0f, deltaTime);
        if (this.settingsModalAnim > 0.005f) {
            this.drawSettingsModal(context, mx, winX, winY, adjMouseX, adjMouseY, deltaTime, alpha, themeColor);
        }
        this.colorPickerAnim = GuiUtils.animate(this.colorPickerAnim, this.settingsModalOpen && this.colorPickerOpen ? 1.0f : 0.0f, 18.0f, deltaTime);
        if (this.colorPickerAnim > 0.005f) {
            this.drawColorPickerModal(context, mx, winX, winY, adjMouseX, adjMouseY, deltaTime, alpha, themeColor);
        }
        this.soundSettingsAnim = GuiUtils.animate(this.soundSettingsAnim, this.settingsModalOpen && this.soundSettingsOpen ? 1.0f : 0.0f, 18.0f, deltaTime);
        if (this.soundSettingsAnim > 0.005f) {
            this.drawSoundsModal(context, mx, winX, winY, adjMouseX, adjMouseY, deltaTime, alpha, themeColor);
        }
        this.scaleModalAnim = GuiUtils.animate(this.scaleModalAnim, this.settingsModalOpen && this.scaleModalOpen ? 1.0f : 0.0f, 18.0f, deltaTime);
        if (this.scaleModalAnim > 0.005f) {
            this.drawScaleModal(context, mx, winX, winY, adjMouseX, adjMouseY, deltaTime, alpha, themeColor);
        }
        ModePopupState.renderActive(context, winX, winY, 580.0f, 380.0f, adjMouseX, adjMouseY, deltaTime, alpha, themeColor);
        SubSettingsPopupState.renderActive(context, winX, winY, 580.0f, 380.0f, adjMouseX, adjMouseY, deltaTime, alpha, themeColor);
        MultiSelectPopupState.renderActive(context, winX, winY, 580.0f, 380.0f, adjMouseX, adjMouseY, deltaTime, alpha, themeColor);
        context.getMatrices().popMatrix();
    }

    private void drawSidebar(DrawContext context, Matrix4f mx, float winX, float winY, int mouseX, int mouseY, float deltaTime, float alpha, Color themeColor) {
        ((BuiltRectangle)Builder.rectangle().size(new SizeState(1.0f, 380.0f)).radius(new QuadRadiusState(0.0f)).color(new QuadColorState(new Color(16, 16, 22, 255))).build()).render(mx, winX + 138.0f, winY);
        float logoSize = 33.0f;
        float logoX = winX + (138.0f - logoSize) / 2.0f;
        float logoY = winY + 4.0f;
        try {
            MsdfFont logoFont = (MsdfFont)ClickGuiIcons.ASTOLFO_LOGO.get();
            if (logoFont != null) {
                float logoCX = logoX + logoSize / 2.0f;
                float logoCY = logoY + logoSize / 2.0f - 2.0f;
                GuiUtils.drawIconGlowShadow(mx, logoCX, logoCY, 6.0f, themeColor, 0.08f * alpha, 4.0f);
                ((BuiltText)Builder.text().font(logoFont).text("A").size(logoSize).color(GuiUtils.withAlpha(themeColor, alpha)).build()).render(mx, logoX, logoY);
            }
        }
        catch (Exception logoFont) {
            // empty catch block
        }
        ((BuiltRectangle)Builder.rectangle().size(new SizeState(114.0f, 1.0f)).radius(new QuadRadiusState(0.0f)).color(new QuadColorState(new Color(18, 18, 24, 255))).build()).render(mx, winX + 12.0f, winY + 48.0f);
        MsdfFont iconFont = (MsdfFont)ClickGuiIcons.CLICKGUI_ICONS.get();
        GuiUtils.renderTextSafely(mx, "Functions", winX + 14.0f, winY + 58.0f, GuiUtils.withAlpha(new Color(110, 110, 125), alpha), 8.0f);
        float targetExpand = this.activeTab == NavTab.RENDER ? 1.0f : 0.0f;
        this.renderExpandAnim = GuiUtils.animate(this.renderExpandAnim, targetExpand, 16.0f, deltaTime);
        RenderSubcategory[] subcategories = RenderSubcategory.values();
        float subH = 17.0f;
        float subGap = 2.5f;
        float subTotalH = (float)subcategories.length * subH + (float)(subcategories.length - 1) * subGap + 4.0f;
        float currentSubHeight = subTotalH * this.renderExpandAnim;
        float tabW = 122.0f;
        float tabH = 22.0f;
        float tabX = winX + (138.0f - tabW) / 2.0f;
        float renderTabY = winY + 70.0f;
        float miscTabY = renderTabY + tabH + 3.5f + currentSubHeight;
        float otherHeaderY = miscTabY + tabH + 6.0f;
        float configsTabY = otherHeaderY + 14.0f;
        float targetActiveY = switch (this.activeTab.ordinal()) {
            case 0 -> renderTabY;
            case 1 -> miscTabY;
            case 2 -> configsTabY;
            default -> renderTabY;
        };
        GuiUtils.renderTextSafely(mx, "Other", winX + 14.0f, otherHeaderY, GuiUtils.withAlpha(new Color(110, 110, 125), alpha), 8.0f);
        this.animatedTabPillY = this.animatedTabPillY < 0.0f ? targetActiveY : GuiUtils.animate(this.animatedTabPillY, targetActiveY, 18.0f, deltaTime);
        Color activePillBg = new Color((int)((float)themeColor.getRed() * 0.22f), (int)((float)themeColor.getGreen() * 0.22f), (int)((float)themeColor.getBlue() * 0.22f), (int)(255.0f * alpha));
        ((BuiltRectangle)Builder.rectangle().size(new SizeState(tabW, tabH)).radius(new QuadRadiusState(5.0f)).color(new QuadColorState(activePillBg)).build()).render(mx, tabX, this.animatedTabPillY);
        NavTab currentlyHoveredTab = null;
        RenderSubcategory currentlyHoveredSub = null;
        boolean popupsActive = this.isAnyModalOpen();
        this.drawNavTabButton(mx, NavTab.RENDER, tabX, renderTabY, tabW, tabH, mouseX, mouseY, popupsActive, deltaTime, alpha, themeColor, iconFont);
        if (!popupsActive && GuiUtils.isMouseOver(mouseX, mouseY, tabX, renderTabY, tabW, tabH)) {
            currentlyHoveredTab = NavTab.RENDER;
        }
        if (this.renderExpandAnim > 0.01f) {
            context.enableScissor((int)winX, (int)(renderTabY + tabH), (int)(winX + 138.0f), (int)(renderTabY + tabH + currentSubHeight + 3.0f));
            float trunkX = tabX + 14.0f;
            float subStartY = renderTabY + tabH + 3.0f;
            float subItemX = tabX + 26.0f;
            float subItemW = tabW - 27.0f;
            float lastItemCenterY = subStartY + (float)(subcategories.length - 1) * (subH + subGap) + subH / 2.0f;
            float trunkH = lastItemCenterY - subStartY;
            ((BuiltRectangle)Builder.rectangle().size(new SizeState(1.5f, trunkH)).radius(new QuadRadiusState(0.75f)).color(new QuadColorState(GuiUtils.withAlpha(new Color(85, 85, 105), alpha * this.renderExpandAnim * 0.7f))).build()).render(mx, trunkX - 0.75f, subStartY);
            for (int i = 0; i < subcategories.length; ++i) {
                boolean isSubHov;
                RenderSubcategory sub = subcategories[i];
                float itemY = subStartY + (float)i * (subH + subGap);
                float itemCenterY = itemY + subH / 2.0f;
                float branchW = subItemX - 3.0f - trunkX;
                ((BuiltRectangle)Builder.rectangle().size(new SizeState(branchW, 1.5f)).radius(new QuadRadiusState(0.75f)).color(new QuadColorState(GuiUtils.withAlpha(new Color(85, 85, 105), alpha * this.renderExpandAnim * 0.7f))).build()).render(mx, trunkX, itemCenterY - 0.75f);
                boolean bl = isSubHov = !popupsActive && GuiUtils.isMouseOver(mouseX, mouseY, subItemX, itemY, subItemW, subH);
                if (isSubHov && this.activeTab == NavTab.RENDER) {
                    currentlyHoveredSub = sub;
                }
                boolean isSubActive = this.activeTab == NavTab.RENDER && this.activeRenderSubcategory == sub;
                float subHovA = this.hoverAnimations.getOrDefault("sub_" + sub.name(), Float.valueOf(0.0f)).floatValue();
                subHovA = GuiUtils.animate(subHovA, isSubHov && this.activeTab == NavTab.RENDER ? 1.0f : 0.0f, 15.0f, deltaTime);
                this.hoverAnimations.put("sub_" + sub.name(), Float.valueOf(subHovA));
                if (isSubActive) {
                    Color subActiveBg = new Color((int)((float)themeColor.getRed() * 0.18f), (int)((float)themeColor.getGreen() * 0.18f), (int)((float)themeColor.getBlue() * 0.18f), (int)(230.0f * alpha * this.renderExpandAnim));
                    ((BuiltRectangle)Builder.rectangle().size(new SizeState(subItemW, subH)).radius(new QuadRadiusState(4.0f)).color(new QuadColorState(subActiveBg)).build()).render(mx, subItemX, itemY);
                } else if (subHovA > 0.01f) {
                    ((BuiltRectangle)Builder.rectangle().size(new SizeState(subItemW, subH)).radius(new QuadRadiusState(4.0f)).color(new QuadColorState(GuiUtils.withAlpha(new Color(24, 24, 32), alpha * this.renderExpandAnim * subHovA))).build()).render(mx, subItemX, itemY);
                }
                Color subTextCol = isSubActive ? GuiUtils.withAlpha(Color.WHITE, alpha * this.renderExpandAnim) : (isSubHov ? GuiUtils.withAlpha(new Color(215, 215, 230), alpha * this.renderExpandAnim) : GuiUtils.withAlpha(new Color(130, 130, 145), alpha * this.renderExpandAnim));
                GuiUtils.renderTextSafely(mx, sub.title, subItemX + 7.0f, itemCenterY - 3.8f, subTextCol, 7.8f);
                int count = this.getModuleCount(sub);
                String countStr = String.valueOf(count);
                MsdfFont mediumFont = (MsdfFont)ClickGuiIcons.MEDIUM_FONT.get();
                float countW = mediumFont != null ? mediumFont.getWidth(countStr, 6.5f) : 8.0f;
                Color countCol = isSubActive ? GuiUtils.withAlpha(themeColor, alpha * this.renderExpandAnim * 0.9f) : GuiUtils.withAlpha(new Color(90, 90, 105), alpha * this.renderExpandAnim * 0.8f);
                GuiUtils.renderTextSafely(mx, countStr, subItemX + subItemW - countW - 6.0f, itemCenterY - 3.3f, countCol, 6.5f);
            }
            context.disableScissor();
        }
        this.drawNavTabButton(mx, NavTab.MISC, tabX, miscTabY, tabW, tabH, mouseX, mouseY, popupsActive, deltaTime, alpha, themeColor, iconFont);
        if (!popupsActive && GuiUtils.isMouseOver(mouseX, mouseY, tabX, miscTabY, tabW, tabH)) {
            currentlyHoveredTab = NavTab.MISC;
        }
        this.drawNavTabButton(mx, NavTab.CONFIGS, tabX, configsTabY, tabW, tabH, mouseX, mouseY, popupsActive, deltaTime, alpha, themeColor, iconFont);
        if (!popupsActive && GuiUtils.isMouseOver(mouseX, mouseY, tabX, configsTabY, tabW, tabH)) {
            currentlyHoveredTab = NavTab.CONFIGS;
        }
        if (currentlyHoveredSub != this.lastHoveredRenderSubcategory) {
            if (currentlyHoveredSub != null && System.currentTimeMillis() - this.initTime > 250L) {
                ModSounds.playModuleSelect();
            }
            this.lastHoveredRenderSubcategory = currentlyHoveredSub;
        }
        if (currentlyHoveredTab != this.lastHoveredTab) {
            if (currentlyHoveredTab != null && System.currentTimeMillis() - this.initTime > 250L) {
                ModSounds.playModuleSelect();
            }
            this.lastHoveredTab = currentlyHoveredTab;
        }
        float profY = winY + 380.0f - 44.0f;
        ((BuiltRectangle)Builder.rectangle().size(new SizeState(138.0f, 1.0f)).radius(new QuadRadiusState(0.0f)).color(new QuadColorState(new Color(16, 16, 22, 255))).build()).render(mx, winX, profY);
        float profCenterY = profY + 22.0f;
        float avSize = 28.0f;
        float avX = winX + 10.0f;
        float avY = profCenterY - avSize / 2.0f;
        float avRadius = avSize / 2.0f;
        boolean drawnAvatar = false;
        Identifier avTex = DiscordAvatarManager.getAvatarTexture();
        if (avTex != null) {
            try {
                AbstractTexture tex = MinecraftClient.getInstance().getTextureManager().getTexture(avTex);
                if (tex != null) {
                    ((BuiltTexture)Builder.texture().size(new SizeState(avSize, avSize)).radius(new QuadRadiusState(avRadius)).texture(0.0f, 0.0f, 1.0f, 1.0f, tex).color(new QuadColorState(GuiUtils.withAlpha(Color.WHITE, alpha))).build()).render(mx, avX, avY);
                    drawnAvatar = true;
                }
            }
            catch (Exception tex) {
                // empty catch block
            }
        }
        if (!drawnAvatar && this.client.player != null) {
            try {
                AbstractTexture tex;
                Identifier skinTex = this.client.player.getSkin().body().texturePath();
                if (skinTex != null && (tex = this.client.getTextureManager().getTexture(skinTex)) != null) {
                    ((BuiltTexture)Builder.texture().size(new SizeState(avSize, avSize)).radius(new QuadRadiusState(avRadius)).texture(0.125f, 0.125f, 0.125f, 0.125f, tex).color(new QuadColorState(GuiUtils.withAlpha(Color.WHITE, alpha))).build()).render(mx, avX, avY);
                    ((BuiltTexture)Builder.texture().size(new SizeState(avSize, avSize)).radius(new QuadRadiusState(avRadius)).texture(0.625f, 0.125f, 0.125f, 0.125f, tex).color(new QuadColorState(GuiUtils.withAlpha(Color.WHITE, alpha))).build()).render(mx, avX, avY);
                    drawnAvatar = true;
                }
            }
            catch (Exception skinTex) {
                // empty catch block
            }
        }
        if (!drawnAvatar) {
            ((BuiltRectangle)Builder.rectangle().size(new SizeState(avSize, avSize)).radius(new QuadRadiusState(avRadius)).color(new QuadColorState(GuiUtils.withAlpha(new Color(28, 28, 36), alpha))).build()).render(mx, avX, avY);
        }
        String name = discordUsername != null && !discordUsername.isEmpty() ? discordUsername : (this.client.player != null ? this.client.player.getName().getString() : "User");
        float userTextX = avX + avSize + 7.0f;
        GuiUtils.renderTextSafely(mx, "User: " + name, userTextX, profCenterY - 4.0f, GuiUtils.withAlpha(Color.WHITE, alpha), 8.5f);
        float gearX = winX + 138.0f - 22.0f;
        float gearY = profCenterY - 4.8f;
        boolean gearHov = !this.isAnyModalOpen() && GuiUtils.isMouseOver(mouseX, mouseY, gearX - 4.0f, gearY - 4.0f, 18.0f, 18.0f);
        float gearHovA = this.hoverAnimations.getOrDefault("gear_hover", Float.valueOf(0.0f)).floatValue();
        gearHovA = GuiUtils.animate(gearHovA, gearHov ? 1.0f : 0.0f, 15.0f, deltaTime);
        this.hoverAnimations.put("gear_hover", Float.valueOf(gearHovA));
        if (iconFont != null) {
            try {
                Color gearColor = GuiUtils.interpolateColor(new Color(210, 210, 225), Color.WHITE, gearHovA);
                ((BuiltText)Builder.text().font(iconFont).text("J").size(10.5f).color(GuiUtils.withAlpha(gearColor, alpha)).build()).render(mx, gearX, gearY);
            }
            catch (Exception exception) {
                // empty catch block
            }
        }
    }

    private void drawNavTabButton(Matrix4f mx, NavTab tab, float tabX, float tabY, float tabW, float tabH, int mouseX, int mouseY, boolean popupsActive, float deltaTime, float alpha, Color themeColor, MsdfFont iconFont) {
        boolean isCur = this.activeTab == tab;
        boolean hov = GuiUtils.isMouseOver(mouseX, mouseY, tabX, tabY, tabW, tabH);
        float hovA = this.hoverAnimations.getOrDefault("tab_" + tab.name(), Float.valueOf(0.0f)).floatValue();
        hovA = GuiUtils.animate(hovA, hov && !popupsActive ? 1.0f : 0.0f, 15.0f, deltaTime);
        this.hoverAnimations.put("tab_" + tab.name(), Float.valueOf(hovA));
        if (!isCur && hovA > 0.01f) {
            ((BuiltRectangle)Builder.rectangle().size(new SizeState(tabW, tabH)).radius(new QuadRadiusState(5.0f)).color(new QuadColorState(GuiUtils.withAlpha(new Color(20, 20, 26), alpha * hovA))).build()).render(mx, tabX, tabY);
        }
        float tabCenterY = tabY + tabH / 2.0f;
        float iconSize = 9.5f;
        float iconX = tabX + 9.0f;
        float iconY = tabCenterY - 4.8f;
        float textX = tabX + 23.0f;
        float textY = tabCenterY - 4.6f;
        if (iconFont != null && tab.icon != null) {
            try {
                float iconCX = iconX + iconSize / 2.0f;
                float iconCY = tabCenterY;
                GuiUtils.drawIconGlowShadow(mx, iconCX, iconCY, iconSize / 2.0f, themeColor, 0.08f * alpha);
                ((BuiltText)Builder.text().font(iconFont).text(tab.icon).size(iconSize).color(GuiUtils.withAlpha(themeColor, alpha)).build()).render(mx, iconX, iconY);
            }
            catch (Exception exception) {
                // empty catch block
            }
        }
        GuiUtils.renderTextSafely(mx, tab.title, textX, textY, GuiUtils.withAlpha(Color.WHITE, alpha), 8.8f);
    }

    private void drawTopBar(DrawContext context, Matrix4f mx, float winX, float winY, int mouseX, int mouseY, float deltaTime, float alpha, Color themeColor) {
        float topBarX = winX + 138.0f;
        float topBarW = 442.0f;
        float topBarH = 36.0f;
        ((BuiltRectangle)Builder.rectangle().size(new SizeState(topBarW, 1.0f)).radius(new QuadRadiusState(0.0f)).color(new QuadColorState(new Color(16, 16, 22, 255))).build()).render(mx, topBarX, winY + topBarH);
        Object displayTitle = this.activeTab.title;
        if (this.activeTab == NavTab.RENDER && this.activeRenderSubcategory != RenderSubcategory.ALL) {
            displayTitle = "Render > " + this.activeRenderSubcategory.title;
        }
        if (this.isSearching && !this.searchText.isEmpty()) {
            String truncSearch = this.searchText.length() > 14 ? this.searchText.substring(0, 14) + "..." : this.searchText;
            displayTitle = "Search: \"" + truncSearch + "\"";
        }
        GuiUtils.renderTextSafely(mx, (String)displayTitle, topBarX + 16.0f, winY + topBarH / 2.0f - 3.5f, GuiUtils.withAlpha(Color.WHITE, alpha), 11.0f);
        float searchW = 125.0f;
        float searchH = 20.0f;
        float searchX = winX + 580.0f - searchW - 14.0f;
        float searchY = winY + (topBarH - searchH) / 2.0f;
        this.searchFocusAnim = GuiUtils.animate(this.searchFocusAnim, this.isSearching ? 1.0f : 0.0f, 16.0f, deltaTime);
        Color searchBg = GuiUtils.interpolateColor(new Color(14, 14, 18, 255), new Color(20, 20, 26, 255), this.searchFocusAnim);
        ((BuiltRectangle)Builder.rectangle().size(new SizeState(searchW, searchH)).radius(new QuadRadiusState(4.0f)).color(new QuadColorState(GuiUtils.withAlpha(searchBg, alpha))).build()).render(mx, searchX, searchY);
        context.enableScissor((int)(searchX + 6.0f), (int)searchY, (int)(searchX + searchW - 18.0f), (int)(searchY + searchH));
        float maxTextW = searchW - 24.0f;
        float textStartX = searchX + 8.0f;
        if (this.isSearching) {
            long timeSinceTyping = System.currentTimeMillis() - this.lastTypingTime;
            float cursorAlpha = timeSinceTyping < 500L ? 1.0f : (System.currentTimeMillis() % 1000L < 500L ? 1.0f : 0.0f);
            float tw = ((MsdfFont)SEMIBOLD_FONT.get()).getWidth(this.searchText, 8.5f);
            float textOffset = tw > maxTextW ? tw - maxTextW : 0.0f;
            float textDrawX = textStartX - textOffset;
            GuiUtils.renderTextSafely(mx, this.searchText, textDrawX, searchY + 5.5f, GuiUtils.withAlpha(Color.WHITE, alpha), 8.5f);
            GuiUtils.renderTextSafely(mx, "_", textDrawX + tw, searchY + 5.5f, GuiUtils.withAlpha(Color.WHITE, alpha * cursorAlpha), 8.5f);
        } else {
            GuiUtils.renderTextSafely(mx, "Search...", textStartX, searchY + 5.5f, GuiUtils.withAlpha(new Color(110, 110, 125), alpha), 8.5f);
        }
        context.disableScissor();
        MsdfFont iconFont = (MsdfFont)ClickGuiIcons.CLICKGUI_ICONS.get();
        if (iconFont != null) {
            try {
                ((BuiltText)Builder.text().font(iconFont).text("I").size(8.5f).color(GuiUtils.withAlpha(themeColor, alpha)).build()).render(mx, searchX + searchW - 15.0f, searchY + 5.5f);
            }
            catch (Exception exception) {
                // empty catch block
            }
        }
    }

    private void drawMainContent(DrawContext context, Matrix4f mx, float winX, float winY, int mouseX, int mouseY, float deltaTime, float alpha, Color themeColor) {
        float contentX = winX + 138.0f;
        float contentY = winY + 36.0f;
        float contentW = 442.0f;
        float contentH = 344.0f;
        context.enableScissor((int)contentX, (int)contentY, (int)(contentX + contentW), (int)(contentY + contentH));
        if (this.activeTab == NavTab.CONFIGS && !this.isSearching) {
            this.drawConfigsView(context, mx, contentX, contentY, contentW, contentH, mouseX, mouseY, deltaTime, alpha, themeColor);
        } else {
            this.drawModulesView(context, mx, contentX, contentY, contentW, contentH, mouseX, mouseY, deltaTime, alpha, themeColor);
        }
        context.disableScissor();
    }

    private void drawModulesView(DrawContext context, Matrix4f mx, float contentX, float contentY, float contentW, float contentH, int mouseX, int mouseY, float deltaTime, float alpha, Color themeColor) {
        float scroll = this.scrollOffsets.get((Object)this.activeTab).floatValue();
        float targetScroll = this.targetScrollOffsets.get((Object)this.activeTab).floatValue();
        scroll = GuiUtils.animate(scroll, targetScroll, 15.0f, deltaTime);
        this.scrollOffsets.put(this.activeTab, Float.valueOf(scroll));
        boolean searching = this.isSearching && !this.searchText.trim().isEmpty();
        HashSet<ModuleButton> matchingButtons = new HashSet<ModuleButton>();
        float margin = 11.0f;
        float colGap = 10.0f;
        float colW = (contentW - margin * 2.0f - colGap) / 2.0f;
        float col1X = contentX + margin;
        float col2X = col1X + colW + colGap;
        float col1Y = contentY + 10.0f + scroll;
        float col2Y = contentY + 10.0f + scroll;
        for (ModuleButton mb : this.allModuleButtons) {
            boolean matches = searching ? mb.module.getName().toLowerCase().contains(this.searchText.toLowerCase()) : (this.activeTab == NavTab.RENDER ? (mb.module.getCategory() == Module.Category.RENDER ? (this.activeRenderSubcategory == RenderSubcategory.ALL ? true : ClickGuiScreen.getRenderSubcategory(mb.module) == this.activeRenderSubcategory) : false) : mb.module.getCategory() == this.activeTab.category);
            mb.width = colW;
            float mbH = mb.calculateHeight();
            if (matches) {
                float targetY;
                float targetX;
                matchingButtons.add(mb);
                if (col1Y <= col2Y) {
                    targetX = col1X;
                    targetY = col1Y;
                    col1Y += mbH + 9.0f;
                } else {
                    targetX = col2X;
                    targetY = col2Y;
                    col2Y += mbH + 9.0f;
                }
                if (mb.renderX <= -9000.0f) {
                    mb.renderX = targetX;
                    mb.renderY = targetY + 10.0f;
                }
                mb.renderAlpha = GuiUtils.animate(mb.renderAlpha, 1.0f, 18.0f, deltaTime);
                mb.renderScale = GuiUtils.animate(mb.renderScale, 1.0f, 18.0f, deltaTime);
                mb.renderX = GuiUtils.animate(mb.renderX, targetX, 20.0f, deltaTime);
                mb.renderY = GuiUtils.animate(mb.renderY, targetY, 20.0f, deltaTime);
            } else {
                mb.renderAlpha = GuiUtils.animate(mb.renderAlpha, 0.0f, 22.0f, deltaTime);
                mb.renderScale = GuiUtils.animate(mb.renderScale, 0.94f, 22.0f, deltaTime);
                if (mb.renderX > -9000.0f) {
                    mb.renderY = GuiUtils.animate(mb.renderY, mb.renderY + 6.0f, 16.0f, deltaTime);
                }
            }
            mb.x = mb.renderX;
            mb.y = mb.renderY;
        }
        boolean popupsActive = this.isAnyModalOpen();
        ModuleButton currentlyHoveredBtn = null;
        for (ModuleButton mb : this.allModuleButtons) {
            if (mb.renderAlpha > 0.01f) {
                float mbH = mb.calculateHeight();
                boolean inView = mb.y + mbH > contentY && mb.y < contentY + contentH;
                boolean bl = mb.isVisible = mb.renderAlpha > 0.4f && inView && matchingButtons.contains(mb);
                if (!inView) continue;
                int passMouseX = popupsActive ? -1 : mouseX;
                int passMouseY = popupsActive ? -1 : mouseY;
                mb.render(context, this, passMouseX, passMouseY, alpha, deltaTime);
                if (popupsActive || !mb.isVisible || !GuiUtils.isMouseOver(mouseX, mouseY, mb.x, mb.y, mb.width, mbH) || !((float)mouseX >= contentX) || !((float)mouseY >= contentY) || !((float)mouseY <= contentY + contentH)) continue;
                currentlyHoveredBtn = mb;
                continue;
            }
            mb.isVisible = false;
        }
        if (currentlyHoveredBtn != this.lastHoveredModuleButton) {
            if (currentlyHoveredBtn != null && System.currentTimeMillis() - this.initTime > 250L) {
                ModSounds.playModuleSelect();
            }
            this.lastHoveredModuleButton = currentlyHoveredBtn;
        }
        float maxColumnY = Math.max(col1Y, col2Y) - scroll;
        float totalContentH = maxColumnY - (contentY + 10.0f);
        float maxScroll = Math.max(0.0f, totalContentH - (contentH - 20.0f));
        this.targetScrollOffsets.put(this.activeTab, Float.valueOf(MathHelper.clamp((float)targetScroll, (float)(-maxScroll), (float)0.0f)));
        if (totalContentH > contentH - 20.0f) {
            float scrollbarW = 3.0f;
            float scrollbarX = contentX + contentW - 5.0f;
            float scrollbarTrackH = contentH - 16.0f;
            float scrollbarTrackY = contentY + 8.0f;
            float thumbRatio = (contentH - 20.0f) / totalContentH;
            float thumbH = Math.max(20.0f, scrollbarTrackH * thumbRatio);
            float scrollProgress = -scroll / maxScroll;
            float thumbY = scrollbarTrackY + (scrollbarTrackH - thumbH) * MathHelper.clamp((float)scrollProgress, (float)0.0f, (float)1.0f);
            ((BuiltRectangle)Builder.rectangle().size(new SizeState(scrollbarW, thumbH)).radius(new QuadRadiusState(1.5f)).color(new QuadColorState(GuiUtils.withAlpha(new Color(50, 50, 65), alpha * 0.6f))).build()).render(mx, scrollbarX, thumbY);
        }
    }

    private void drawConfigsView(DrawContext context, Matrix4f mx, float contentX, float contentY, float contentW, float contentH, int mouseX, int mouseY, float deltaTime, float alpha, Color themeColor) {
        String dispConfigName;
        float margin = 11.0f;
        float colGap = 10.0f;
        float cardW = (contentW - margin * 2.0f - colGap) / 2.0f;
        float card1X = contentX + margin;
        float card2X = card1X + cardW + colGap;
        float cardY = contentY + 10.0f;
        float cardH = contentH - 20.0f;
        ((BuiltRectangle)Builder.rectangle().size(new SizeState(cardW, cardH)).radius(new QuadRadiusState(8.0f)).color(new QuadColorState(GuiUtils.withAlpha(new Color(6, 6, 8), alpha))).build()).render(mx, card1X, cardY);
        GuiUtils.renderTextSafely(mx, "Configuration Manager", card1X + 12.0f, cardY + 10.0f, GuiUtils.withAlpha(Color.WHITE, alpha), 10.0f);
        float inputY = cardY + 30.0f;
        float inputW = cardW - 65.0f;
        float inputH = 18.0f;
        ((BuiltRectangle)Builder.rectangle().size(new SizeState(inputW, inputH)).radius(new QuadRadiusState(4.0f)).color(new QuadColorState(GuiUtils.withAlpha(new Color(14, 14, 18), alpha))).build()).render(mx, card1X + 10.0f, inputY);
        dispConfigName = this.isTypingNewConfig ? this.newConfigInput : (this.newConfigInput.isEmpty() ? "New config name..." : this.newConfigInput);
        Color inputCol = this.isTypingNewConfig ? Color.WHITE : (this.newConfigInput.isEmpty() ? new Color(100, 100, 120) : Color.WHITE);
        GuiUtils.renderTextSafely(mx, dispConfigName, card1X + 14.0f, inputY + 5.0f, GuiUtils.withAlpha(inputCol, alpha), 8.5f);
        float createBtnX = card1X + 10.0f + inputW + 4.0f;
        float createBtnW = cardW - inputW - 24.0f;
        boolean createHov = GuiUtils.isMouseOver(mouseX, mouseY, createBtnX, inputY, createBtnW, inputH);
        float createHovA = this.hoverAnimations.getOrDefault("btn_create", Float.valueOf(0.0f)).floatValue();
        createHovA = GuiUtils.animate(createHovA, createHov ? 1.0f : 0.0f, 15.0f, deltaTime);
        this.hoverAnimations.put("btn_create", Float.valueOf(createHovA));
        Color createBg = GuiUtils.interpolateColor(new Color(24, 24, 32), themeColor, createHovA);
        ((BuiltRectangle)Builder.rectangle().size(new SizeState(createBtnW, inputH)).radius(new QuadRadiusState(4.0f)).color(new QuadColorState(GuiUtils.withAlpha(createBg, alpha))).build()).render(mx, createBtnX, inputY);
        Color createTextCol = GuiUtils.interpolateColor(Color.WHITE, Color.BLACK, createHovA);
        GuiUtils.renderTextSafely(mx, "Create", createBtnX + 7.0f, inputY + 5.0f, GuiUtils.withAlpha(createTextCol, alpha), 8.5f);
        float listY = inputY + 24.0f;
        float listH = cardH - 88.0f;
        ((BuiltRectangle)Builder.rectangle().size(new SizeState(cardW - 20.0f, listH)).radius(new QuadRadiusState(4.0f)).color(new QuadColorState(GuiUtils.withAlpha(new Color(12, 12, 16), alpha))).build()).render(mx, card1X + 10.0f, listY);
        float itemY = listY + 4.0f;
        for (String cfg : this.localConfigs) {
            boolean isSel = cfg.equalsIgnoreCase(this.selectedConfig);
            boolean itemHov = GuiUtils.isMouseOver(mouseX, mouseY, card1X + 12.0f, itemY, cardW - 24.0f, 15.0f);
            float cfgHovA = this.hoverAnimations.getOrDefault("cfg_" + cfg, Float.valueOf(0.0f)).floatValue();
            cfgHovA = GuiUtils.animate(cfgHovA, itemHov ? 1.0f : 0.0f, 15.0f, deltaTime);
            this.hoverAnimations.put("cfg_" + cfg, Float.valueOf(cfgHovA));
            if (isSel) {
                ((BuiltRectangle)Builder.rectangle().size(new SizeState(cardW - 24.0f, 15.0f)).radius(new QuadRadiusState(3.0f)).color(new QuadColorState(GuiUtils.withAlpha(themeColor, alpha * 0.35f))).build()).render(mx, card1X + 12.0f, itemY);
            } else if (cfgHovA > 0.01f) {
                ((BuiltRectangle)Builder.rectangle().size(new SizeState(cardW - 24.0f, 15.0f)).radius(new QuadRadiusState(3.0f)).color(new QuadColorState(GuiUtils.withAlpha(new Color(22, 22, 28), alpha * cfgHovA))).build()).render(mx, card1X + 12.0f, itemY);
            }
            Color itemCol = isSel ? themeColor : GuiUtils.interpolateColor(new Color(180, 180, 195), Color.WHITE, cfgHovA);
            GuiUtils.renderTextSafely(mx, cfg, card1X + 18.0f, itemY + 3.5f, GuiUtils.withAlpha(itemCol, alpha), 8.5f);
            itemY += 17.0f;
        }
        float btnRowY = cardY + cardH - 24.0f;
        float btnW = (cardW - 28.0f) / 3.0f;
        boolean loadHov = GuiUtils.isMouseOver(mouseX, mouseY, card1X + 10.0f, btnRowY, btnW, 16.0f);
        float loadHovA = this.hoverAnimations.getOrDefault("btn_load", Float.valueOf(0.0f)).floatValue();
        loadHovA = GuiUtils.animate(loadHovA, loadHov ? 1.0f : 0.0f, 15.0f, deltaTime);
        this.hoverAnimations.put("btn_load", Float.valueOf(loadHovA));
        Color loadBg = GuiUtils.interpolateColor(new Color(24, 24, 34), themeColor, loadHovA);
        ((BuiltRectangle)Builder.rectangle().size(new SizeState(btnW, 16.0f)).radius(new QuadRadiusState(4.0f)).color(new QuadColorState(GuiUtils.withAlpha(loadBg, alpha))).build()).render(mx, card1X + 10.0f, btnRowY);
        Color loadTextCol = GuiUtils.interpolateColor(Color.WHITE, Color.BLACK, loadHovA);
        GuiUtils.renderTextSafely(mx, "Load", card1X + 10.0f + (btnW - ((MsdfFont)SEMIBOLD_FONT.get()).getWidth("Load", 8.5f)) / 2.0f, btnRowY + 4.0f, GuiUtils.withAlpha(loadTextCol, alpha), 8.5f);
        float saveBtnX = card1X + 10.0f + btnW + 4.0f;
        boolean saveHov = GuiUtils.isMouseOver(mouseX, mouseY, saveBtnX, btnRowY, btnW, 16.0f);
        float saveHovA = this.hoverAnimations.getOrDefault("btn_save", Float.valueOf(0.0f)).floatValue();
        saveHovA = GuiUtils.animate(saveHovA, saveHov ? 1.0f : 0.0f, 15.0f, deltaTime);
        this.hoverAnimations.put("btn_save", Float.valueOf(saveHovA));
        Color saveBg = GuiUtils.interpolateColor(new Color(24, 24, 34), themeColor, saveHovA);
        ((BuiltRectangle)Builder.rectangle().size(new SizeState(btnW, 16.0f)).radius(new QuadRadiusState(4.0f)).color(new QuadColorState(GuiUtils.withAlpha(saveBg, alpha))).build()).render(mx, saveBtnX, btnRowY);
        Color saveTextCol = GuiUtils.interpolateColor(Color.WHITE, Color.BLACK, saveHovA);
        GuiUtils.renderTextSafely(mx, "Save", saveBtnX + (btnW - ((MsdfFont)SEMIBOLD_FONT.get()).getWidth("Save", 8.5f)) / 2.0f, btnRowY + 4.0f, GuiUtils.withAlpha(saveTextCol, alpha), 8.5f);
        float delBtnX = saveBtnX + btnW + 4.0f;
        boolean delHov = GuiUtils.isMouseOver(mouseX, mouseY, delBtnX, btnRowY, btnW, 16.0f);
        float delHovA = this.hoverAnimations.getOrDefault("btn_del", Float.valueOf(0.0f)).floatValue();
        delHovA = GuiUtils.animate(delHovA, delHov ? 1.0f : 0.0f, 15.0f, deltaTime);
        this.hoverAnimations.put("btn_del", Float.valueOf(delHovA));
        Color delBg = GuiUtils.interpolateColor(new Color(34, 20, 20), new Color(220, 50, 50), delHovA);
        ((BuiltRectangle)Builder.rectangle().size(new SizeState(btnW, 16.0f)).radius(new QuadRadiusState(4.0f)).color(new QuadColorState(GuiUtils.withAlpha(delBg, alpha))).build()).render(mx, delBtnX, btnRowY);
        Color delTextCol = GuiUtils.interpolateColor(new Color(220, 80, 80), Color.WHITE, delHovA);
        GuiUtils.renderTextSafely(mx, "Delete", delBtnX + (btnW - ((MsdfFont)SEMIBOLD_FONT.get()).getWidth("Delete", 8.5f)) / 2.0f, btnRowY + 4.0f, GuiUtils.withAlpha(delTextCol, alpha), 9.0f);
        ((BuiltRectangle)Builder.rectangle().size(new SizeState(cardW, cardH)).radius(new QuadRadiusState(8.0f)).color(new QuadColorState(GuiUtils.withAlpha(new Color(6, 6, 8), alpha))).build()).render(mx, card2X, cardY);
        GuiUtils.renderTextSafely(mx, "Config Details", card2X + 12.0f, cardY + 10.0f, GuiUtils.withAlpha(Color.WHITE, alpha), 10.0f);
        float infoY = cardY + 32.0f;
        GuiUtils.renderTextSafely(mx, "Selected Config: " + this.selectedConfig, card2X + 14.0f, infoY, GuiUtils.withAlpha(themeColor, alpha), 9.0f);
        GuiUtils.renderTextSafely(mx, "Total Configs: " + this.localConfigs.size(), card2X + 14.0f, infoY += 16.0f, GuiUtils.withAlpha(Color.WHITE, alpha), 9.0f);
        GuiUtils.renderTextSafely(mx, "Total Modules: " + this.allModuleButtons.size(), card2X + 14.0f, infoY += 16.0f, GuiUtils.withAlpha(new Color(170, 170, 185), alpha), 9.0f);
        GuiUtils.renderTextSafely(mx, "Storage: Cloud Profile (API)", card2X + 14.0f, infoY += 16.0f, GuiUtils.withAlpha(new Color(140, 140, 155), alpha), 8.5f);
        if (System.currentTimeMillis() - this.configStatusTime < 4000L && !this.configStatusMessage.isEmpty()) {
            GuiUtils.renderTextSafely(mx, this.configStatusMessage, card2X + 14.0f, cardY + cardH - 24.0f, GuiUtils.withAlpha(themeColor, alpha), 9.0f);
        }
    }

    private void drawSettingsModal(DrawContext context, Matrix4f mx, float winX, float winY, int mouseX, int mouseY, float deltaTime, float alpha, Color themeColor) {
        float sm = this.settingsModalAnim;
        float ease = 1.0f - (float)Math.pow(1.0f - sm, 3.0);
        float scale = 0.9f + 0.1f * ease;
        float modalAlpha = alpha * ease;
        float popW = 106.0f;
        float popH = 106.0f;
        float popX = this.getSettingsModalX(winX);
        float popY = this.getSettingsModalY(winY);
        context.getMatrices().pushMatrix();
        if (scale < 0.999f) {
            float scx = popX + popW / 2.0f;
            float scy = popY + popH / 2.0f;
            context.getMatrices().translate(scx, scy);
            context.getMatrices().scale(scale, scale);
            context.getMatrices().translate(-scx, -scy);
        }
        Matrix4f matrix = xyz.angames.astolfoclient.client.render.GuiTransforms.matrix(context.getMatrices());
        for (int i = 6; i >= 0; --i) {
            float progress = (float)i / 6.0f;
            float spread = progress * 4.0f;
            int sAlpha = (int)(60.0f * (1.0f - progress) * (1.0f - progress) * modalAlpha);
            if (sAlpha <= 0) continue;
            ((BuiltRectangle)Builder.rectangle().size(new SizeState(popW + spread * 2.0f, popH + spread * 2.0f)).radius(new QuadRadiusState(8.0f + spread)).color(new QuadColorState(new Color(0, 0, 0, sAlpha))).build()).render(matrix, popX - spread, popY - spread);
        }
        ((BuiltRectangle)Builder.rectangle().size(new SizeState(popW, popH)).radius(new QuadRadiusState(8.0f)).color(new QuadColorState(new Color(0, 0, 0, (int)(255.0f * modalAlpha)))).build()).render(matrix, popX, popY);
        MsdfFont medFont = null;
        try {
            medFont = (MsdfFont)ClickGuiIcons.MEDIUM_FONT.get();
        }
        catch (Exception progress) {
            // empty catch block
        }
        float contentW = popW - 10.0f;
        float rX = popX + 5.0f;
        float rowH = 21.0f;
        float r1Y = this.getSettingsRowY(popY, 0);
        boolean colHov = GuiUtils.isMouseOver(mouseX, mouseY, rX, r1Y, contentW, rowH);
        if (colHov || this.colorPickerOpen) {
            ((BuiltRectangle)Builder.rectangle().size(new SizeState(contentW, rowH)).radius(new QuadRadiusState(5.0f)).color(new QuadColorState(new Color(25, 25, 36, (int)(200.0f * modalAlpha)))).build()).render(matrix, rX, r1Y);
        }
        GuiUtils.renderTextSafely(matrix, "Client color", rX + 5.0f, r1Y + 6.0f, GuiUtils.withAlpha(Color.WHITE, modalAlpha), 7.5f);
        float dotSize = 10.0f;
        float dotX = popX + popW - 5.0f - dotSize - 4.0f;
        float dotY = r1Y + (rowH - dotSize) / 2.0f;
        ((BuiltRectangle)Builder.rectangle().size(new SizeState(dotSize, dotSize)).radius(new QuadRadiusState(dotSize / 2.0f)).color(new QuadColorState(GuiUtils.withAlpha(themeColor, modalAlpha))).build()).render(matrix, dotX, dotY);
        float r2Y = this.getSettingsRowY(popY, 1);
        boolean sndHov = GuiUtils.isMouseOver(mouseX, mouseY, rX, r2Y, contentW, rowH);
        if (sndHov || this.soundSettingsOpen) {
            ((BuiltRectangle)Builder.rectangle().size(new SizeState(contentW, rowH)).radius(new QuadRadiusState(5.0f)).color(new QuadColorState(new Color(25, 25, 36, (int)(200.0f * modalAlpha)))).build()).render(matrix, rX, r2Y);
        }
        GuiUtils.renderTextSafely(matrix, "Sounds", rX + 5.0f, r2Y + 6.0f, GuiUtils.withAlpha(Color.WHITE, modalAlpha), 7.5f);
        String masterVolStr = Math.round(SoundSettings.getMasterVolume()) + "%";
        float volStrW = medFont != null ? medFont.getWidth(masterVolStr, 7.0f) : 18.0f;
        GuiUtils.renderTextSafely(matrix, masterVolStr, popX + popW - 9.0f - volStrW, r2Y + 6.0f, GuiUtils.withAlpha(themeColor, modalAlpha), 7.0f);
        float r3Y = this.getSettingsRowY(popY, 2);
        boolean scaleHov = GuiUtils.isMouseOver(mouseX, mouseY, rX, r3Y, contentW, rowH);
        if (scaleHov || this.scaleModalOpen) {
            ((BuiltRectangle)Builder.rectangle().size(new SizeState(contentW, rowH)).radius(new QuadRadiusState(5.0f)).color(new QuadColorState(new Color(25, 25, 36, (int)(200.0f * modalAlpha)))).build()).render(matrix, rX, r3Y);
        }
        GuiUtils.renderTextSafely(matrix, "GUI scale", rX + 5.0f, r3Y + 6.0f, GuiUtils.withAlpha(Color.WHITE, modalAlpha), 7.5f);
        String scaleStr = GuiScaleSettings.getScaleLabel() + " >";
        float scaleStrW = medFont != null ? medFont.getWidth(scaleStr, 7.0f) : 22.0f;
        GuiUtils.renderTextSafely(matrix, scaleStr, popX + popW - 9.0f - scaleStrW, r3Y + 6.0f, GuiUtils.withAlpha(themeColor, modalAlpha), 7.0f);
        float r4Y = this.getSettingsRowY(popY, 3);
        boolean hudHov = GuiUtils.isMouseOver(mouseX, mouseY, rX, r4Y, contentW, rowH);
        Color hudBg = hudHov ? new Color(38, 38, 48) : new Color(22, 22, 28);
        ((BuiltRectangle)Builder.rectangle().size(new SizeState(contentW, rowH)).radius(new QuadRadiusState(5.0f)).color(new QuadColorState(GuiUtils.withAlpha(hudBg, modalAlpha))).build()).render(matrix, rX, r4Y);
        float hudTw = medFont != null ? medFont.getWidth("Hud editor", 7.5f) : 38.0f;
        float hudTx = rX + (contentW - hudTw) / 2.0f;
        GuiUtils.renderTextSafely(matrix, "Hud editor", hudTx, r4Y + 6.0f, GuiUtils.withAlpha(Color.WHITE, modalAlpha), 7.5f);
        context.getMatrices().popMatrix();
    }

    private void drawSoundsModal(DrawContext context, Matrix4f mx, float winX, float winY, int mouseX, int mouseY, float deltaTime, float alpha, Color themeColor) {
        float sndEase = 1.0f - (float)Math.pow(1.0f - this.soundSettingsAnim, 3.0);
        float sndScale = 0.9f + 0.1f * sndEase;
        float sndAlpha = alpha * sndEase;
        float sndW = 138.0f;
        float sndH = 190.0f;
        float sndX = this.getSoundSettingsX(winX);
        float sndY = this.getSoundSettingsY(winY);
        context.getMatrices().pushMatrix();
        if (sndScale < 0.999f) {
            float scx = sndX + sndW / 2.0f;
            float scy = sndY + sndH / 2.0f;
            context.getMatrices().translate(scx, scy);
            context.getMatrices().scale(sndScale, sndScale);
            context.getMatrices().translate(-scx, -scy);
        }
        Matrix4f matrix = xyz.angames.astolfoclient.client.render.GuiTransforms.matrix(context.getMatrices());
        for (int i = 6; i >= 0; --i) {
            float progress = (float)i / 6.0f;
            float spread = progress * 4.0f;
            int sAlpha = (int)(60.0f * (1.0f - progress) * (1.0f - progress) * sndAlpha);
            if (sAlpha <= 0) continue;
            ((BuiltRectangle)Builder.rectangle().size(new SizeState(sndW + spread * 2.0f, sndH + spread * 2.0f)).radius(new QuadRadiusState(8.0f + spread)).color(new QuadColorState(new Color(0, 0, 0, sAlpha))).build()).render(matrix, sndX - spread, sndY - spread);
        }
        ((BuiltRectangle)Builder.rectangle().size(new SizeState(sndW, sndH)).radius(new QuadRadiusState(8.0f)).color(new QuadColorState(new Color(0, 0, 0, (int)(255.0f * sndAlpha)))).build()).render(matrix, sndX, sndY);
        SoundSettingsPopup.render(context, sndX, sndY, sndW, sndH, mouseX, mouseY, deltaTime, sndAlpha, themeColor);
        context.getMatrices().popMatrix();
    }

    private void drawScaleModal(DrawContext context, Matrix4f mx, float winX, float winY, int mouseX, int mouseY, float deltaTime, float alpha, Color themeColor) {
        float scaleEase = 1.0f - (float)Math.pow(1.0f - this.scaleModalAnim, 3.0);
        float scaleScale = 0.9f + 0.1f * scaleEase;
        float scaleAlpha = alpha * scaleEase;
        float scW = 94.0f;
        float scH = 162.0f;
        float scX = this.getScaleSettingsX(winX);
        float scY = this.getScaleSettingsY(winY);
        context.getMatrices().pushMatrix();
        if (scaleScale < 0.999f) {
            float scx = scX + scW / 2.0f;
            float scy = scY + scH / 2.0f;
            context.getMatrices().translate(scx, scy);
            context.getMatrices().scale(scaleScale, scaleScale);
            context.getMatrices().translate(-scx, -scy);
        }
        Matrix4f matrix = xyz.angames.astolfoclient.client.render.GuiTransforms.matrix(context.getMatrices());
        for (int i = 6; i >= 0; --i) {
            float progress = (float)i / 6.0f;
            float spread = progress * 4.0f;
            int sAlpha = (int)(60.0f * (1.0f - progress) * (1.0f - progress) * scaleAlpha);
            if (sAlpha <= 0) continue;
            ((BuiltRectangle)Builder.rectangle().size(new SizeState(scW + spread * 2.0f, scH + spread * 2.0f)).radius(new QuadRadiusState(8.0f + spread)).color(new QuadColorState(new Color(0, 0, 0, sAlpha))).build()).render(matrix, scX - spread, scY - spread);
        }
        ((BuiltRectangle)Builder.rectangle().size(new SizeState(scW, scH)).radius(new QuadRadiusState(8.0f)).color(new QuadColorState(new Color(0, 0, 0, (int)(255.0f * scaleAlpha)))).build()).render(matrix, scX, scY);
        ScaleSettingsPopup.render(context, scX, scY, scW, scH, mouseX, mouseY, deltaTime, scaleAlpha, themeColor);
        context.getMatrices().popMatrix();
    }

    private void drawColorPickerModal(DrawContext context, Matrix4f mx, float winX, float winY, int mouseX, int mouseY, float deltaTime, float alpha, Color themeColor) {
        float cpEase = 1.0f - (float)Math.pow(1.0f - this.colorPickerAnim, 3.0);
        float cpScale = 0.9f + 0.1f * cpEase;
        float cpAlpha = alpha * cpEase;
        float cpW = 106.0f;
        float cpH = 94.0f;
        float cpX = this.getColorPickerX(winX);
        float cpY = this.getColorPickerY(winY);
        context.getMatrices().pushMatrix();
        if (cpScale < 0.999f) {
            float scx = cpX + cpW / 2.0f;
            float scy = cpY + cpH / 2.0f;
            context.getMatrices().translate(scx, scy);
            context.getMatrices().scale(cpScale, cpScale);
            context.getMatrices().translate(-scx, -scy);
        }
        Matrix4f matrix = xyz.angames.astolfoclient.client.render.GuiTransforms.matrix(context.getMatrices());
        for (int i = 6; i >= 0; --i) {
            float progress = (float)i / 6.0f;
            float spread = progress * 4.0f;
            int sAlpha = (int)(60.0f * (1.0f - progress) * (1.0f - progress) * cpAlpha);
            if (sAlpha <= 0) continue;
            ((BuiltRectangle)Builder.rectangle().size(new SizeState(cpW + spread * 2.0f, cpH + spread * 2.0f)).radius(new QuadRadiusState(8.0f + spread)).color(new QuadColorState(new Color(0, 0, 0, sAlpha))).build()).render(matrix, cpX - spread, cpY - spread);
        }
        ((BuiltRectangle)Builder.rectangle().size(new SizeState(cpW, cpH)).radius(new QuadRadiusState(8.0f)).color(new QuadColorState(new Color(0, 0, 0, (int)(255.0f * cpAlpha)))).build()).render(matrix, cpX, cpY);
        this.themePicker.x = cpX;
        this.themePicker.y = cpY;
        this.themePicker.width = cpW;
        this.themePicker.height = cpH;
        this.themePicker.animate(deltaTime);
        this.themePicker.render(matrix, mouseX, mouseY, cpAlpha);
        context.getMatrices().popMatrix();
    }

    private void renderShadow(Matrix4f matrix, float x, float y, float w, float h, float radius, float masterAlpha) {
        int shadowSteps = 12;
        float maxSpread = 10.0f;
        for (int i = shadowSteps - 1; i >= 0; --i) {
            float progress = (float)i / (float)shadowSteps;
            float spread = progress * maxSpread;
            float alphaFactor = (1.0f - progress) * (1.0f - progress);
            int alpha = (int)(120.0f * alphaFactor * masterAlpha);
            if (alpha <= 0) continue;
            ((BuiltRectangle)Builder.rectangle().size(new SizeState(w + spread * 2.0f, h + spread * 2.0f)).radius(new QuadRadiusState(radius + spread)).color(new QuadColorState(new Color(0, 0, 0, alpha))).build()).render(matrix, x - spread, y - spread + 1.0f);
        }
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        float searchH;
        float searchY;
        float searchW;
        float searchX;
        float sm = this.effectiveScale();
        float adjMouseX = (float)(mouseX / (double)sm);
        float adjMouseY = (float)(mouseY / (double)sm);
        if (System.currentTimeMillis() - this.initTime < 150L) {
            return true;
        }
        float scaledWidth = (float)this.width / sm;
        float winX = (scaledWidth - 580.0f) / 2.0f;
        float scaledHeight = (float)this.height / sm;
        float winY = (scaledHeight - 380.0f) / 2.0f;
        if (ModePopupState.mouseClicked(adjMouseX, adjMouseY, button, winX, winY, 580.0f, 380.0f)) {
            return true;
        }
        if (SubSettingsPopupState.mouseClicked(adjMouseX, adjMouseY, button, winX, winY, 580.0f, 380.0f)) {
            return true;
        }
        if (MultiSelectPopupState.mouseClicked(adjMouseX, adjMouseY, button, winX, winY, 580.0f, 380.0f)) {
            return true;
        }
        float popW = 106.0f;
        float popH = 106.0f;
        float popX = this.getSettingsModalX(winX);
        float popY = this.getSettingsModalY(winY);
        float cpW = 106.0f;
        float cpH = 94.0f;
        float cpX = this.getColorPickerX(winX);
        float cpY = this.getColorPickerY(winY);
        float sndW = 138.0f;
        float sndH = 190.0f;
        float sndX = this.getSoundSettingsX(winX);
        float sndY = this.getSoundSettingsY(winY);
        float scW = 94.0f;
        float scH = 162.0f;
        float scX = this.getScaleSettingsX(winX);
        float scY = this.getScaleSettingsY(winY);
        if (this.colorPickerOpen && this.colorPickerAnim > 0.05f) {
            this.themePicker.x = cpX;
            this.themePicker.y = cpY;
            this.themePicker.width = cpW;
            this.themePicker.height = cpH;
            if (GuiUtils.isMouseOver(adjMouseX, adjMouseY, cpX, cpY, cpW, cpH)) {
                if (this.themePicker.mouseClicked(adjMouseX, adjMouseY, button)) {
                    return true;
                }
                return true;
            }
        }
        if (this.soundSettingsOpen && this.soundSettingsAnim > 0.05f && GuiUtils.isMouseOver(adjMouseX, adjMouseY, sndX, sndY, sndW, sndH)) {
            if (SoundSettingsPopup.mouseClicked(adjMouseX, adjMouseY, button, sndX, sndY, sndW, sndH)) {
                return true;
            }
            return true;
        }
        if (this.scaleModalOpen && this.scaleModalAnim > 0.05f && GuiUtils.isMouseOver(adjMouseX, adjMouseY, scX, scY, scW, scH)) {
            if (ScaleSettingsPopup.mouseClicked(adjMouseX, adjMouseY, button, scX, scY, scW, scH)) {
                return true;
            }
            return true;
        }
        if (this.settingsModalOpen && this.settingsModalAnim > 0.05f) {
            boolean inScale;
            float contentW = popW - 10.0f;
            float rX = popX + 5.0f;
            float rowH = 21.0f;
            float r1Y = this.getSettingsRowY(popY, 0);
            float r2Y = this.getSettingsRowY(popY, 1);
            float r3Y = this.getSettingsRowY(popY, 2);
            float r4Y = this.getSettingsRowY(popY, 3);
            if (GuiUtils.isMouseOver(adjMouseX, adjMouseY, rX, r1Y, contentW, rowH)) {
                boolean bl = this.colorPickerOpen = !this.colorPickerOpen;
                if (this.colorPickerOpen) {
                    this.themePicker.loadColor();
                    this.soundSettingsOpen = false;
                    this.scaleModalOpen = false;
                }
                ModSounds.playModeOpen();
                return true;
            }
            if (GuiUtils.isMouseOver(adjMouseX, adjMouseY, rX, r2Y, contentW, rowH)) {
                boolean bl = this.soundSettingsOpen = !this.soundSettingsOpen;
                if (this.soundSettingsOpen) {
                    this.colorPickerOpen = false;
                    this.scaleModalOpen = false;
                }
                ModSounds.playModeOpen();
                return true;
            }
            if (GuiUtils.isMouseOver(adjMouseX, adjMouseY, rX, r3Y, contentW, rowH)) {
                boolean bl = this.scaleModalOpen = !this.scaleModalOpen;
                if (this.scaleModalOpen) {
                    this.colorPickerOpen = false;
                    this.soundSettingsOpen = false;
                }
                ModSounds.playModeOpen();
                return true;
            }
            if (GuiUtils.isMouseOver(adjMouseX, adjMouseY, rX, r4Y, contentW, rowH)) {
                ModSounds.playModeOpen();
                this.client.setScreen((Screen)new HudEditorScreen());
                return true;
            }
            if (GuiUtils.isMouseOver(adjMouseX, adjMouseY, popX, popY, popW, popH)) {
                return true;
            }
            boolean inColor = this.colorPickerOpen && GuiUtils.isMouseOver(adjMouseX, adjMouseY, cpX, cpY, cpW, cpH);
            boolean inSounds = this.soundSettingsOpen && GuiUtils.isMouseOver(adjMouseX, adjMouseY, sndX, sndY, sndW, sndH);
            boolean bl = inScale = this.scaleModalOpen && GuiUtils.isMouseOver(adjMouseX, adjMouseY, scX, scY, scW, scH);
            if (!(inColor || inSounds || inScale)) {
                this.settingsModalOpen = false;
                this.colorPickerOpen = false;
                this.soundSettingsOpen = false;
                this.scaleModalOpen = false;
                ModSounds.playModeOpen();
                return true;
            }
        }
        if (GuiUtils.isMouseOver(adjMouseX, adjMouseY, searchX = winX + 580.0f - (searchW = 125.0f) - 14.0f, searchY = winY + (36.0f - (searchH = 20.0f)) / 2.0f, searchW, searchH)) {
            boolean bl = this.isSearching = !this.isSearching;
            if (!this.isSearching) {
                this.searchText = "";
            }
            ModSounds.playSearchClick();
            return true;
        }
        RenderSubcategory[] subcategories = RenderSubcategory.values();
        float subH = 17.0f;
        float subGap = 2.5f;
        float subTotalH = (float)subcategories.length * subH + (float)(subcategories.length - 1) * subGap + 4.0f;
        float currentSubHeight = subTotalH * this.renderExpandAnim;
        float tabW = 122.0f;
        float tabH = 22.0f;
        float tabX = winX + (138.0f - tabW) / 2.0f;
        float renderTabY = winY + 70.0f;
        float miscTabY = renderTabY + tabH + 3.5f + currentSubHeight;
        float otherHeaderY = miscTabY + tabH + 6.0f;
        float configsTabY = otherHeaderY + 14.0f;
        if (this.activeTab == NavTab.RENDER && this.renderExpandAnim > 0.4f) {
            float subStartY = renderTabY + tabH + 3.0f;
            float subItemX = tabX + 26.0f;
            float subItemW = tabW - 27.0f;
            for (int i = 0; i < subcategories.length; ++i) {
                RenderSubcategory sub = subcategories[i];
                float itemY = subStartY + (float)i * (subH + subGap);
                if (!GuiUtils.isMouseOver(adjMouseX, adjMouseY, subItemX, itemY, subItemW, subH)) continue;
                if (this.activeRenderSubcategory != sub) {
                    this.activeRenderSubcategory = sub;
                    this.targetScrollOffsets.put(NavTab.RENDER, Float.valueOf(0.0f));
                    ModSounds.playModuleSelect();
                }
                this.isSearching = false;
                this.searchText = "";
                return true;
            }
        }
        if (GuiUtils.isMouseOver(adjMouseX, adjMouseY, tabX, renderTabY, tabW, tabH)) {
            if (this.activeTab != NavTab.RENDER) {
                this.activeTab = NavTab.RENDER;
                ModSounds.playCategoryChange();
            } else {
                this.activeRenderSubcategory = RenderSubcategory.ALL;
                this.targetScrollOffsets.put(NavTab.RENDER, Float.valueOf(0.0f));
                ModSounds.playModuleSelect();
            }
            this.isSearching = false;
            this.searchText = "";
            return true;
        }
        if (GuiUtils.isMouseOver(adjMouseX, adjMouseY, tabX, miscTabY, tabW, tabH)) {
            if (this.activeTab != NavTab.MISC) {
                this.activeTab = NavTab.MISC;
                ModSounds.playCategoryChange();
            }
            this.isSearching = false;
            this.searchText = "";
            return true;
        }
        if (GuiUtils.isMouseOver(adjMouseX, adjMouseY, tabX, configsTabY, tabW, tabH)) {
            if (this.activeTab != NavTab.CONFIGS) {
                this.activeTab = NavTab.CONFIGS;
                ModSounds.playCategoryChange();
            }
            this.isSearching = false;
            this.searchText = "";
            return true;
        }
        float gearX = winX + 138.0f - 22.0f;
        float profY = winY + 380.0f - 44.0f;
        float profCenterY = profY + 22.0f;
        float gearY = profCenterY - 4.8f;
        if (GuiUtils.isMouseOver(adjMouseX, adjMouseY, gearX - 4.0f, gearY - 4.0f, 18.0f, 18.0f)) {
            boolean bl = this.settingsModalOpen = !this.settingsModalOpen;
            if (!this.settingsModalOpen) {
                this.colorPickerOpen = false;
                this.soundSettingsOpen = false;
                this.scaleModalOpen = false;
            } else {
                this.themePicker.loadColor();
            }
            ModSounds.playModeOpen();
            return true;
        }
        if (this.activeTab == NavTab.CONFIGS && !this.isSearching) {
            float contentX = winX + 138.0f;
            float contentY = winY + 36.0f;
            float contentW = 442.0f;
            float contentH = 344.0f;
            float margin = 11.0f;
            float colGap = 10.0f;
            float cardW = (contentW - margin * 2.0f - colGap) / 2.0f;
            float card1X = contentX + margin;
            float cardY = contentY + 10.0f;
            float cardH = contentH - 20.0f;
            float inputY = cardY + 30.0f;
            float inputW = cardW - 65.0f;
            float inputH = 18.0f;
            if (GuiUtils.isMouseOver(adjMouseX, adjMouseY, card1X + 10.0f, inputY, inputW, inputH)) {
                this.isTypingNewConfig = true;
                return true;
            }
            this.isTypingNewConfig = false;
            float createBtnX = card1X + 10.0f + inputW + 4.0f;
            float createBtnW = cardW - inputW - 24.0f;
            if (GuiUtils.isMouseOver(adjMouseX, adjMouseY, createBtnX, inputY, createBtnW, inputH)) {
                if (!this.newConfigInput.trim().isEmpty()) {
                    String cfgName = this.newConfigInput.trim();
                    new Thread(() -> {
                        String err = AstolfoclientClient.configManager.saveConfig(cfgName);
                        if (err == null) {
                            this.selectedConfig = cfgName;
                            this.newConfigInput = "";
                            this.isTypingNewConfig = false;
                            this.reloadConfigs();
                            this.configStatusMessage = "Config created & saved to cloud!";
                            this.configStatusTime = System.currentTimeMillis();
                        } else {
                            this.configStatusMessage = "Error: " + err;
                            this.configStatusTime = System.currentTimeMillis();
                        }
                    }).start();
                }
                return true;
            }
            float listY = inputY + 24.0f;
            float itemY = listY + 4.0f;
            for (String cfg : this.localConfigs) {
                if (GuiUtils.isMouseOver(adjMouseX, adjMouseY, card1X + 12.0f, itemY, cardW - 24.0f, 15.0f)) {
                    this.selectedConfig = cfg;
                    return true;
                }
                itemY += 17.0f;
            }
            float btnRowY = cardY + cardH - 24.0f;
            float btnW = (cardW - 28.0f) / 3.0f;
            if (GuiUtils.isMouseOver(adjMouseX, adjMouseY, card1X + 10.0f, btnRowY, btnW, 16.0f)) {
                new Thread(() -> {
                    boolean ok = AstolfoclientClient.configManager.loadConfig(this.selectedConfig);
                    this.configStatusMessage = ok ? "Cloud config loaded!" : "Failed to load from cloud!";
                    this.configStatusTime = System.currentTimeMillis();
                }).start();
                return true;
            }
            float saveBtnX = card1X + 10.0f + btnW + 4.0f;
            if (GuiUtils.isMouseOver(adjMouseX, adjMouseY, saveBtnX, btnRowY, btnW, 16.0f)) {
                new Thread(() -> {
                    String err = AstolfoclientClient.configManager.saveConfig(this.selectedConfig);
                    this.configStatusMessage = err == null ? "Cloud config saved!" : "Save error: " + err;
                    this.configStatusTime = System.currentTimeMillis();
                    this.reloadConfigs();
                }).start();
                return true;
            }
            float delBtnX = saveBtnX + btnW + 4.0f;
            if (GuiUtils.isMouseOver(adjMouseX, adjMouseY, delBtnX, btnRowY, btnW, 16.0f)) {
                if (!this.selectedConfig.equalsIgnoreCase("default")) {
                    new Thread(() -> {
                        boolean ok = AstolfoclientClient.configManager.deleteConfig(this.selectedConfig);
                        this.reloadConfigs();
                        this.configStatusMessage = ok ? "Config deleted from cloud!" : "Failed to delete!";
                        this.configStatusTime = System.currentTimeMillis();
                    }).start();
                }
                return true;
            }
        }
        if (!this.isAnyModalOpen()) {
            for (ModuleButton mb : this.allModuleButtons) {
                if (!mb.isVisible || !GuiUtils.isMouseOver(adjMouseX, adjMouseY, mb.x, mb.y, mb.width, mb.height)) continue;
                if (button == 1 || button == 2) {
                    for (ModuleButton other : this.allModuleButtons) {
                        if (other == mb) continue;
                        other.isBinding = false;
                    }
                }
                if (!mb.mouseClicked(adjMouseX, adjMouseY, button)) continue;
                return true;
            }
        }
        return super.mouseClicked(new net.minecraft.client.gui.Click(mouseX, mouseY, new net.minecraft.client.input.MouseInput(button, 0)), false);
    }

    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        float winY;
        float winX;
        float adjMouseY;
        float sm = this.effectiveScale();
        float adjMouseX = (float)(mouseX / (double)sm);
        if (SubSettingsPopupState.mouseDragged(adjMouseX, adjMouseY = (float)(mouseY / (double)sm), button, winX = ((float)this.width / sm - 580.0f) / 2.0f, winY = ((float)this.height / sm - 380.0f) / 2.0f, 580.0f, 380.0f)) {
            return true;
        }
        if (this.colorPickerOpen && this.themePicker.isDragging()) {
            this.themePicker.update(adjMouseX, adjMouseY);
            return true;
        }
        if (this.soundSettingsOpen && SoundSettingsPopup.isDragging()) {
            float sndW = 138.0f;
            float sndX = this.getSoundSettingsX(winX);
            SoundSettingsPopup.mouseDragged(adjMouseX, sndX, sndW);
            return true;
        }
        if (!this.isAnyModalOpen()) {
            for (ModuleButton mb : this.allModuleButtons) {
                if (!mb.isVisible || !mb.mouseDragged(adjMouseX, adjMouseY, button, deltaX, deltaY)) continue;
                return true;
            }
        }
        return super.mouseDragged(new net.minecraft.client.gui.Click(mouseX, mouseY, new net.minecraft.client.input.MouseInput(button, 0)), deltaX, deltaY);
    }

    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        float sm = this.effectiveScale();
        float adjX = (float)(mouseX / (double)sm);
        float adjY = (float)(mouseY / (double)sm);
        this.themePicker.mouseReleased();
        SoundSettingsPopup.mouseReleased();
        SubSettingsPopupState.mouseReleased(mouseX, mouseY, button);
        for (ModuleButton mb : this.allModuleButtons) {
            mb.mouseReleased();
        }
        return super.mouseReleased(new net.minecraft.client.gui.Click(mouseX, mouseY, new net.minecraft.client.input.MouseInput(button, 0)));
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        float winY;
        float winX;
        float adjY;
        float sm = this.effectiveScale();
        float adjX = (float)(mouseX / (double)sm);
        if (ModePopupState.mouseScrolled(adjX, adjY = (float)(mouseY / (double)sm), verticalAmount, winX = ((float)this.width / sm - 580.0f) / 2.0f, winY = ((float)this.height / sm - 380.0f) / 2.0f, 580.0f, 380.0f)) {
            return true;
        }
        if (MultiSelectPopupState.mouseScrolled(adjX, adjY, verticalAmount, winX, winY, 580.0f, 380.0f)) {
            return true;
        }
        if (SubSettingsPopupState.mouseScrolled(adjX, adjY, verticalAmount, winX, winY, 580.0f, 380.0f)) {
            return true;
        }
        if (this.isAnyModalOpen()) {
            return true;
        }
        if (GuiUtils.isMouseOver(adjX, adjY, winX + 138.0f, winY + 36.0f, 442.0f, 344.0f)) {
            float target = this.targetScrollOffsets.getOrDefault((Object)this.activeTab, Float.valueOf(0.0f)).floatValue();
            this.targetScrollOffsets.put(this.activeTab, Float.valueOf(target + (float)verticalAmount * 32.0f));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    public boolean charTyped(char chr, int modifiers) {
        if (this.isSearching) {
            if (this.searchText.length() < 30 && chr >= ' ' && chr != '\u007f') {
                this.searchText = this.searchText + chr;
                this.lastTypingTime = System.currentTimeMillis();
            }
            return true;
        }
        if (this.isTypingNewConfig) {
            if (this.newConfigInput.length() < 24 && chr >= ' ' && chr != '\u007f') {
                this.newConfigInput = this.newConfigInput + chr;
            }
            return true;
        }
        return super.charTyped(new net.minecraft.client.input.CharInput(chr, modifiers));
    }

    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        boolean isCloseKey;
        if (MultiSelectPopupState.isOpen() && keyCode == 256) {
            MultiSelectPopupState.close();
            return true;
        }
        if (SubSettingsPopupState.isOpen() && keyCode == 256) {
            SubSettingsPopupState.close();
            return true;
        }
        if (ModePopupState.isOpen() && keyCode == 256) {
            ModePopupState.close();
            return true;
        }
        if ((this.settingsModalOpen || this.colorPickerOpen || this.soundSettingsOpen || this.scaleModalOpen) && keyCode == 256) {
            this.settingsModalOpen = false;
            this.colorPickerOpen = false;
            this.soundSettingsOpen = false;
            this.scaleModalOpen = false;
            return true;
        }
        for (ModuleButton mb : this.allModuleButtons) {
            if (!mb.isBinding) continue;
            if (keyCode == 256 || keyCode == 261) {
                mb.module.setKeyCode(-1);
            } else {
                mb.module.setKeyCode(keyCode);
            }
            mb.isBinding = false;
            return true;
        }
        for (ModuleButton mb : this.allModuleButtons) {
            mb.keyPressed(keyCode);
        }
        if (this.isSearching) {
            this.lastTypingTime = System.currentTimeMillis();
            if (keyCode == 259 && !this.searchText.isEmpty()) {
                this.searchText = this.searchText.substring(0, this.searchText.length() - 1);
                return true;
            }
            if (keyCode == 256 || keyCode == 257) {
                this.isSearching = false;
                return true;
            }
            return true;
        }
        if (this.isTypingNewConfig) {
            if (keyCode == 259 && !this.newConfigInput.isEmpty()) {
                this.newConfigInput = this.newConfigInput.substring(0, this.newConfigInput.length() - 1);
                return true;
            }
            if (keyCode == 256 || keyCode == 257) {
                this.isTypingNewConfig = false;
                return true;
            }
            return true;
        }
        boolean bl = isCloseKey = keyCode == 256 || keyCode == 260 || keyCode == 344 || keyCode == AstolfoclientClient.clickGuiKeyCode;
        if (isCloseKey) {
            if (keyCode == 256) {
                if (this.colorPickerOpen || this.soundSettingsOpen || this.scaleModalOpen) {
                    this.colorPickerOpen = false;
                    this.soundSettingsOpen = false;
                    this.scaleModalOpen = false;
                    ModSounds.playModeOpen();
                    return true;
                }
                if (this.settingsModalOpen) {
                    this.settingsModalOpen = false;
                    ModSounds.playModeOpen();
                    return true;
                }
                if (ModePopupState.isOpen()) {
                    ModePopupState.close();
                    return true;
                }
                if (SubSettingsPopupState.isOpen()) {
                    SubSettingsPopupState.close();
                    return true;
                }
                if (MultiSelectPopupState.isOpen()) {
                    MultiSelectPopupState.close();
                    return true;
                }
            }
            if (System.currentTimeMillis() - this.initTime >= 150L) {
                this.close();
            }
            return true;
        }
        return super.keyPressed(new net.minecraft.client.input.KeyInput(keyCode, scanCode, modifiers));
    }

    private void renderHandLogic(DrawContext ctx, float delta) {
        boolean renderingHand = this.allModuleButtons.stream().anyMatch(mb -> mb.isVisible && mb.module instanceof HandPositionModule);
        if (!renderingHand || this.client.gameRenderer == null) {
            return;
        }
        try {
            if (renderHandMethod == null) {
                renderHandMethod = GameRenderer.class.getDeclaredMethod("renderHand", MatrixStack.class, Camera.class, Float.TYPE);
                renderHandMethod.setAccessible(true);
            }
            renderHandMethod.invoke((Object)this.client.gameRenderer, ctx.getMatrices(), this.client.gameRenderer.getCamera(), Float.valueOf(delta));
        }
        catch (Exception exception) {
            // empty catch block
        }
    }

    @Environment(value=EnvType.CLIENT)
    public static enum NavTab {
        RENDER(Module.Category.RENDER, "F", "Render", true),
        MISC(Module.Category.MISC, "G", "Miscellaneous", true),
        CONFIGS(null, "E", "Configs", false);

        public final Module.Category category;
        public final String icon;
        public final String title;
        public final boolean isFunction;

        private NavTab(Module.Category category, String icon, String title, boolean isFunction) {
            this.category = category;
            this.icon = icon;
            this.title = title;
            this.isFunction = isFunction;
        }
    }

    @Environment(value=EnvType.CLIENT)
    public static enum RenderSubcategory {
        ALL("All"),
        PVP("PVP"),
        WORLD("World"),
        PLAYER("Player"),
        INTERFACE("Interface");

        public final String title;

        private RenderSubcategory(String title) {
            this.title = title;
        }
    }
    @Override public boolean mouseClicked(net.minecraft.client.gui.Click click, boolean doubled) { return mouseClicked(click.x(), click.y(), click.button()); }
    @Override public boolean mouseDragged(net.minecraft.client.gui.Click click, double dx, double dy) { return mouseDragged(click.x(), click.y(), click.button(), dx, dy); }
    @Override public boolean mouseReleased(net.minecraft.client.gui.Click click) { return mouseReleased(click.x(), click.y(), click.button()); }
    @Override public boolean keyPressed(net.minecraft.client.input.KeyInput input) { return keyPressed(input.key(), input.scancode(), input.modifiers()); }
    @Override public boolean charTyped(net.minecraft.client.input.CharInput input) { return charTyped((char)input.codepoint(), input.modifiers()); }

}
