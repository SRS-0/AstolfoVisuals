package xyz.angames.astolfoclient.client.gui;

import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.CharacterEvent;

import xyz.angames.astolfoclient.client.compat.GuiCompat;
import com.google.common.base.Supplier;
import dev.sxmurxy.mre.builders.Builder;
import dev.sxmurxy.mre.builders.states.QuadColorState;
import dev.sxmurxy.mre.builders.states.QuadRadiusState;
import dev.sxmurxy.mre.builders.states.SizeState;
import dev.sxmurxy.mre.msdf.MsdfFont;
import java.awt.Color;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.util.Mth;
import net.minecraft.client.Camera;
import net.minecraft.client.gui.screens.Screen;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.GameRenderer;
import org.joml.Matrix4f;
import xyz.angames.astolfoclient.client.AstolfoclientClient;
import xyz.angames.astolfoclient.client.DiscordRpcManager;
import xyz.angames.astolfoclient.client.config.GuiScaleSettings;
import xyz.angames.astolfoclient.client.config.SoundSettings;
import xyz.angames.astolfoclient.client.config.ThemeManager;
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

@Environment(EnvType.CLIENT)
public class ClickGuiScreen extends Screen {
   private final List<ModuleButton> allModuleButtons;
   private static Method renderHandMethod = null;
   private ClickGuiScreen.NavTab activeTab = ClickGuiScreen.NavTab.RENDER;
   private ClickGuiScreen.RenderSubcategory activeRenderSubcategory = ClickGuiScreen.RenderSubcategory.ALL;
   private float renderExpandAnim = 1.0F;
   private ClickGuiScreen.RenderSubcategory lastHoveredRenderSubcategory = null;
   private float animatedTabPillY = -1.0F;
   public boolean isSearching = false;
   private String searchText = "";
   private float searchFocusAnim = 0.0F;
   private long lastTypingTime = 0L;
   public static final float GUI_WIDTH = 580.0F;
   public static final float GUI_HEIGHT = 380.0F;
   public static final float SIDEBAR_WIDTH = 138.0F;
   private final Map<ClickGuiScreen.NavTab, Float> scrollOffsets = new HashMap<>();
   private final Map<ClickGuiScreen.NavTab, Float> targetScrollOffsets = new HashMap<>();
   private final ColorPickerComponent themePicker = new ColorPickerComponent("Theme Color");
   private String selectedConfig = "default";
   private String newConfigInput = "";
   private boolean isTypingNewConfig = false;
   private List<String> localConfigs = new ArrayList<>();
   private final List<String> cloudConfigs = new ArrayList<>();
   private String configStatusMessage = "";
   private long configStatusTime = 0L;
   private float configScroll = 0.0F;
   private boolean settingsModalOpen = false;
   private float settingsModalAnim = 0.0F;
   private boolean colorPickerOpen = false;
   private float colorPickerAnim = 0.0F;
   private boolean soundSettingsOpen = false;
   private float soundSettingsAnim = 0.0F;
   private boolean scaleModalOpen = false;
   private float scaleModalAnim = 0.0F;
   public static final float SETTINGS_MODAL_W = 106.0F;
   public static final float SETTINGS_MODAL_H = 106.0F;
   public static final float COLOR_PICKER_W = 106.0F;
   public static final float COLOR_PICKER_H = 94.0F;
   public static final float SOUND_SETTINGS_W = 138.0F;
   public static final float SOUND_SETTINGS_H = 190.0F;
   public static final float SCALE_SETTINGS_W = 94.0F;
   public static final float SCALE_SETTINGS_H = 162.0F;
   private ClickGuiScreen.NavTab lastHoveredTab = null;
   private ModuleButton lastHoveredModuleButton = null;
   private static volatile String discordUsername = "";
   private static volatile String discordAvatarUrl = "";
   private final Map<String, Float> hoverAnimations = new HashMap<>();
   private long initTime;
   private long lastFrameTime = 0L;
   private float openAnimProgress = 0.0F;
   private static final Supplier<MsdfFont> SEMIBOLD_FONT = ClickGuiIcons.SEMIBOLD_FONT;
   private static final Supplier<MsdfFont> MEDIUM_FONT = ClickGuiIcons.MEDIUM_FONT;

   public float getSettingsModalX(float winX) {
      return winX + 138.0F + 6.0F;
   }

   public float getSettingsModalY(float winY) {
      return winY + 380.0F - 106.0F - 12.0F;
   }

   public float getSettingsRowY(float popY, int row) {
      return popY + 5.0F + row * 24.0F;
   }

   public float getColorPickerX(float winX) {
      return this.getSettingsModalX(winX) + 106.0F + 6.0F;
   }

   public float getColorPickerY(float winY) {
      float popY = this.getSettingsModalY(winY);
      return Mth.clamp(popY - 10.0F, winY + 38.0F, winY + 380.0F - 94.0F - 8.0F);
   }

   public float getSoundSettingsX(float winX) {
      return this.getSettingsModalX(winX) + 106.0F + 6.0F;
   }

   public float getSoundSettingsY(float winY) {
      float popY = this.getSettingsModalY(winY);
      return Mth.clamp(popY - 16.0F, winY + 38.0F, winY + 380.0F - 190.0F - 8.0F);
   }

   public float getScaleSettingsX(float winX) {
      return this.getSettingsModalX(winX) + 106.0F + 6.0F;
   }

   public float getScaleSettingsY(float winY) {
      float popY = this.getSettingsModalY(winY);
      return Mth.clamp(popY - 10.0F, winY + 38.0F, winY + 380.0F - 162.0F - 8.0F);
   }

   public boolean isAnyModalOpen() {
      return this.settingsModalOpen
         || this.colorPickerOpen
         || this.soundSettingsOpen
         || this.scaleModalOpen
         || ModePopupState.isOpen()
         || SubSettingsPopupState.isOpen()
         || MultiSelectPopupState.isOpen();
   }

   public boolean isAnyTextInputFocused() {
      return this.isSearching || this.isTypingNewConfig;
   }

   public ClickGuiScreen() {
      super(Component.literal("AstolfoClient"));
      this.allModuleButtons = AstolfoclientClient.moduleManager.getModules().stream().map(m -> new ModuleButton(m, 205.0F)).collect(Collectors.toList());

      for (ClickGuiScreen.NavTab tab : ClickGuiScreen.NavTab.values()) {
         this.scrollOffsets.put(tab, 0.0F);
         this.targetScrollOffsets.put(tab, 0.0F);
      }
   }

   public static void setDiscordUser(String username, String userId, String avatarHash) {
      discordUsername = username != null ? username : "";
      if (avatarHash != null && !avatarHash.isEmpty() && userId != null && !userId.isEmpty()) {
         discordAvatarUrl = "https://cdn.discordapp.com/avatars/" + userId + "/" + avatarHash + ".png?size=256";
      }

      DiscordAvatarManager.update(username, userId, avatarHash);
   }

   @Override
   protected void init() {
      this.initTime = System.currentTimeMillis();
      this.lastFrameTime = System.currentTimeMillis();
      this.openAnimProgress = 0.0F;
      this.animatedTabPillY = -1.0F;
      this.isSearching = false;
      this.searchText = "";
      this.searchFocusAnim = 0.0F;
      this.lastHoveredTab = null;
      this.lastHoveredRenderSubcategory = null;
      this.lastHoveredModuleButton = null;
      this.isTypingNewConfig = false;
      this.newConfigInput = "";
      this.renderExpandAnim = this.activeTab == ClickGuiScreen.NavTab.RENDER ? 1.0F : 0.0F;
      this.reloadConfigs();
      if (DiscordRpcManager.discordUsername != null && !DiscordRpcManager.discordUsername.isEmpty()) {
         discordUsername = DiscordRpcManager.discordUsername;
      }

      this.refreshModuleButtons();
      this.themePicker.loadColor();
      ModSounds.playGuiOpen();
   }

   public static ClickGuiScreen.RenderSubcategory getRenderSubcategory(Module module) {
      if (module == null) {
         return ClickGuiScreen.RenderSubcategory.ALL;
      }

      String name = module.getName().toLowerCase();
      switch (name) {
         case "targetesp":
         case "hitesp":
         case "hitglow":
         case "damageindicators":
         case "killeffect":
         case "trajectories":
            return ClickGuiScreen.RenderSubcategory.PVP;
         case "ambients":
         case "blockoutline":
         case "cubeparticles":
         case "fireflies":
         case "rain":
         case "fullbright":
         case "itemphysics":
         case "lineglyphs":
            return ClickGuiScreen.RenderSubcategory.WORLD;
         case "interface":
         case "norender":
         case "crosshair":
            return ClickGuiScreen.RenderSubcategory.INTERFACE;
         default:
            return ClickGuiScreen.RenderSubcategory.PLAYER;
      }
   }

   private int getModuleCount(ClickGuiScreen.RenderSubcategory sub) {
      if (AstolfoclientClient.moduleManager == null) {
         return 0;
      } else {
         return sub == ClickGuiScreen.RenderSubcategory.ALL
            ? (int)AstolfoclientClient.moduleManager.getModules().stream().filter(m -> m.getCategory() == Module.Category.RENDER).count()
            : (int)AstolfoclientClient.moduleManager
               .getModules()
               .stream()
               .filter(m -> m.getCategory() == Module.Category.RENDER && getRenderSubcategory(m) == sub)
               .count();
      }
   }

   public void refreshModuleButtons() {
      if (AstolfoclientClient.moduleManager != null) {
         List<Module> currentMods = AstolfoclientClient.moduleManager.getModules();
         this.allModuleButtons.removeIf(btn -> !currentMods.contains(btn.module));

         for (Module m : currentMods) {
            boolean exists = this.allModuleButtons.stream().anyMatch(btn -> btn.module == m || btn.module.getName().equalsIgnoreCase(m.getName()));
            if (!exists) {
               this.allModuleButtons.add(new ModuleButton(m, 205.0F));
            }
         }
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

   @Override
   public boolean isPauseScreen() {
      return false;
   }

   @Override
   public boolean shouldCloseOnEsc() {
      return false;
   }

   @Override
   public void onClose() {
      this.settingsModalOpen = false;
      this.colorPickerOpen = false;
      this.soundSettingsOpen = false;
      this.scaleModalOpen = false;
      ModePopupState.close();
      SubSettingsPopupState.close();
      MultiSelectPopupState.close();
      super.onClose();
   }

   @Override
   public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
      this.renderHandLogic(context, delta);
      long now = System.currentTimeMillis();
      if (this.lastFrameTime == 0L) {
         this.lastFrameTime = now;
      }

      float deltaTime = (float)(now - this.lastFrameTime) / 1000.0F;
      this.lastFrameTime = now;
      deltaTime = Mth.clamp(deltaTime, 5.0E-4F, 0.1F);

      for (ModuleButton mb : this.allModuleButtons) {
         mb.isVisible = false;
      }

      float sm = GuiUtils.getScaleModifier(this.minecraft) * GuiScaleSettings.getScale();
      int adjMouseX = (int)(mouseX / sm);
      int adjMouseY = (int)(mouseY / sm);
      context.pose().pushMatrix();
      context.pose().scale(sm, sm);
      Matrix4f mx = GuiCompat.matrix4f(context);
      float scaledWidth = this.width / sm;
      float scaledHeight = this.height / sm;
      this.openAnimProgress = GuiUtils.animate(this.openAnimProgress, 1.0F, 14.0F, deltaTime);
      float alpha = this.openAnimProgress;
      float winX = (scaledWidth - 580.0F) / 2.0F;
      float winY = (scaledHeight - 380.0F) / 2.0F;
      if (this.openAnimProgress < 0.999F) {
         float cx = winX + 290.0F;
         float cy = winY + 190.0F;
         float scale = 0.92F + 0.08F * this.openAnimProgress;
         context.pose().translate(cx, cy);
         context.pose().scale(scale, scale);
         context.pose().translate(-cx, -cy);
         mx = GuiCompat.matrix4f(context);
      }

      Color themeColor = new Color(ThemeManager.getThemedColor(System.currentTimeMillis() / 10L));
      this.renderShadow(mx, winX, winY, 580.0F, 380.0F, 10.0F, alpha);
      Builder.rectangle()
         .size(new SizeState(580.0F, 380.0F))
         .radius(new QuadRadiusState(10.0F))
         .color(new QuadColorState(new Color(0, 0, 0, 255)))
         .build()
         .render(mx, winX, winY);
      this.drawSidebar(context, mx, winX, winY, adjMouseX, adjMouseY, deltaTime, alpha, themeColor);
      this.drawTopBar(context, mx, winX, winY, adjMouseX, adjMouseY, deltaTime, alpha, themeColor);
      this.drawMainContent(context, mx, winX, winY, adjMouseX, adjMouseY, deltaTime, alpha, themeColor);
      this.settingsModalAnim = GuiUtils.animate(this.settingsModalAnim, this.settingsModalOpen ? 1.0F : 0.0F, 18.0F, deltaTime);
      if (this.settingsModalAnim > 0.005F) {
         this.drawSettingsModal(context, mx, winX, winY, adjMouseX, adjMouseY, deltaTime, alpha, themeColor);
      }

      this.colorPickerAnim = GuiUtils.animate(this.colorPickerAnim, this.settingsModalOpen && this.colorPickerOpen ? 1.0F : 0.0F, 18.0F, deltaTime);
      if (this.colorPickerAnim > 0.005F) {
         this.drawColorPickerModal(context, mx, winX, winY, adjMouseX, adjMouseY, deltaTime, alpha, themeColor);
      }

      this.soundSettingsAnim = GuiUtils.animate(this.soundSettingsAnim, this.settingsModalOpen && this.soundSettingsOpen ? 1.0F : 0.0F, 18.0F, deltaTime);
      if (this.soundSettingsAnim > 0.005F) {
         this.drawSoundsModal(context, mx, winX, winY, adjMouseX, adjMouseY, deltaTime, alpha, themeColor);
      }

      this.scaleModalAnim = GuiUtils.animate(this.scaleModalAnim, this.settingsModalOpen && this.scaleModalOpen ? 1.0F : 0.0F, 18.0F, deltaTime);
      if (this.scaleModalAnim > 0.005F) {
         this.drawScaleModal(context, mx, winX, winY, adjMouseX, adjMouseY, deltaTime, alpha, themeColor);
      }

      ModePopupState.renderActive(context, winX, winY, 580.0F, 380.0F, adjMouseX, adjMouseY, deltaTime, alpha, themeColor);
      SubSettingsPopupState.renderActive(context, winX, winY, 580.0F, 380.0F, adjMouseX, adjMouseY, deltaTime, alpha, themeColor);
      MultiSelectPopupState.renderActive(context, winX, winY, 580.0F, 380.0F, adjMouseX, adjMouseY, deltaTime, alpha, themeColor);
      context.pose().popMatrix();
   }

   private void drawSidebar(GuiGraphicsExtractor context, Matrix4f mx, float winX, float winY, int mouseX, int mouseY, float deltaTime, float alpha, Color themeColor) {
      Builder.rectangle()
         .size(new SizeState(1.0F, 380.0F))
         .radius(new QuadRadiusState(0.0F))
         .color(new QuadColorState(new Color(16, 16, 22, 255)))
         .build()
         .render(mx, winX + 138.0F, winY);
      float logoSize = 33.0F;
      float logoX = winX + (138.0F - logoSize) / 2.0F;
      float logoY = winY + 4.0F;

      try {
         MsdfFont logoFont = (MsdfFont)ClickGuiIcons.ASTOLFO_LOGO.get();
         if (logoFont != null) {
            float logoCX = logoX + logoSize / 2.0F;
            float logoCY = logoY + logoSize / 2.0F - 2.0F;
            GuiUtils.drawIconGlowShadow(mx, logoCX, logoCY, 6.0F, themeColor, 0.08F * alpha, 4.0F);
            Builder.text().font(logoFont).text("A").size(logoSize).color(GuiUtils.withAlpha(themeColor, alpha)).build().render(mx, logoX, logoY);
         }
      } catch (Exception var55) {
      }

      Builder.rectangle()
         .size(new SizeState(114.0F, 1.0F))
         .radius(new QuadRadiusState(0.0F))
         .color(new QuadColorState(new Color(18, 18, 24, 255)))
         .build()
         .render(mx, winX + 12.0F, winY + 48.0F);
      MsdfFont iconFont = (MsdfFont)ClickGuiIcons.CLICKGUI_ICONS.get();
      GuiUtils.renderTextSafely(mx, "Functions", winX + 14.0F, winY + 58.0F, GuiUtils.withAlpha(new Color(110, 110, 125), alpha), 8.0F);
      float targetExpand = this.activeTab == ClickGuiScreen.NavTab.RENDER ? 1.0F : 0.0F;
      this.renderExpandAnim = GuiUtils.animate(this.renderExpandAnim, targetExpand, 16.0F, deltaTime);
      ClickGuiScreen.RenderSubcategory[] subcategories = ClickGuiScreen.RenderSubcategory.values();
      float subH = 17.0F;
      float subGap = 2.5F;
      float subTotalH = subcategories.length * subH + (subcategories.length - 1) * subGap + 4.0F;
      float currentSubHeight = subTotalH * this.renderExpandAnim;
      float tabW = 122.0F;
      float tabH = 22.0F;
      float tabX = winX + (138.0F - tabW) / 2.0F;
      float renderTabY = winY + 70.0F;
      float miscTabY = renderTabY + tabH + 3.5F + currentSubHeight;
      float otherHeaderY = miscTabY + tabH + 6.0F;
      float configsTabY = otherHeaderY + 14.0F;

      float targetActiveY = switch (this.activeTab) {
         case RENDER -> renderTabY;
         case MISC -> miscTabY;
         case CONFIGS -> configsTabY;
         default -> renderTabY;
      };
      GuiUtils.renderTextSafely(mx, "Other", winX + 14.0F, otherHeaderY, GuiUtils.withAlpha(new Color(110, 110, 125), alpha), 8.0F);
      if (this.animatedTabPillY < 0.0F) {
         this.animatedTabPillY = targetActiveY;
      } else {
         this.animatedTabPillY = GuiUtils.animate(this.animatedTabPillY, targetActiveY, 18.0F, deltaTime);
      }

      Color activePillBg = new Color(
         (int)(themeColor.getRed() * 0.22F), (int)(themeColor.getGreen() * 0.22F), (int)(themeColor.getBlue() * 0.22F), (int)(255.0F * alpha)
      );
      Builder.rectangle()
         .size(new SizeState(tabW, tabH))
         .radius(new QuadRadiusState(5.0F))
         .color(new QuadColorState(activePillBg))
         .build()
         .render(mx, tabX, this.animatedTabPillY);
      ClickGuiScreen.NavTab currentlyHoveredTab = null;
      ClickGuiScreen.RenderSubcategory currentlyHoveredSub = null;
      boolean popupsActive = this.isAnyModalOpen();
      this.drawNavTabButton(
         mx, ClickGuiScreen.NavTab.RENDER, tabX, renderTabY, tabW, tabH, mouseX, mouseY, popupsActive, deltaTime, alpha, themeColor, iconFont
      );
      if (!popupsActive && GuiUtils.isMouseOver(mouseX, mouseY, tabX, renderTabY, tabW, tabH)) {
         currentlyHoveredTab = ClickGuiScreen.NavTab.RENDER;
      }

      if (this.renderExpandAnim > 0.01F) {
         context.enableScissor((int)winX, (int)(renderTabY + tabH), (int)(winX + 138.0F), (int)(renderTabY + tabH + currentSubHeight + 3.0F));
         float trunkX = tabX + 14.0F;
         float subStartY = renderTabY + tabH + 3.0F;
         float subItemX = tabX + 26.0F;
         float subItemW = tabW - 27.0F;
         float lastItemCenterY = subStartY + (subcategories.length - 1) * (subH + subGap) + subH / 2.0F;
         float trunkH = lastItemCenterY - subStartY;
         Builder.rectangle()
            .size(new SizeState(1.5F, trunkH))
            .radius(new QuadRadiusState(0.75F))
            .color(new QuadColorState(GuiUtils.withAlpha(new Color(85, 85, 105), alpha * this.renderExpandAnim * 0.7F)))
            .build()
            .render(mx, trunkX - 0.75F, subStartY);

         for (int i = 0; i < subcategories.length; i++) {
            ClickGuiScreen.RenderSubcategory sub = subcategories[i];
            float itemY = subStartY + i * (subH + subGap);
            float itemCenterY = itemY + subH / 2.0F;
            float branchW = subItemX - 3.0F - trunkX;
            Builder.rectangle()
               .size(new SizeState(branchW, 1.5F))
               .radius(new QuadRadiusState(0.75F))
               .color(new QuadColorState(GuiUtils.withAlpha(new Color(85, 85, 105), alpha * this.renderExpandAnim * 0.7F)))
               .build()
               .render(mx, trunkX, itemCenterY - 0.75F);
            boolean isSubHov = !popupsActive && GuiUtils.isMouseOver(mouseX, mouseY, subItemX, itemY, subItemW, subH);
            if (isSubHov && this.activeTab == ClickGuiScreen.NavTab.RENDER) {
               currentlyHoveredSub = sub;
            }

            boolean isSubActive = this.activeTab == ClickGuiScreen.NavTab.RENDER && this.activeRenderSubcategory == sub;
            float subHovA = this.hoverAnimations.getOrDefault("sub_" + sub.name(), 0.0F);
            subHovA = GuiUtils.animate(subHovA, isSubHov && this.activeTab == ClickGuiScreen.NavTab.RENDER ? 1.0F : 0.0F, 15.0F, deltaTime);
            this.hoverAnimations.put("sub_" + sub.name(), subHovA);
            if (isSubActive) {
               Color subActiveBg = new Color(
                  (int)(themeColor.getRed() * 0.18F),
                  (int)(themeColor.getGreen() * 0.18F),
                  (int)(themeColor.getBlue() * 0.18F),
                  (int)(230.0F * alpha * this.renderExpandAnim)
               );
               Builder.rectangle()
                  .size(new SizeState(subItemW, subH))
                  .radius(new QuadRadiusState(4.0F))
                  .color(new QuadColorState(subActiveBg))
                  .build()
                  .render(mx, subItemX, itemY);
            } else if (subHovA > 0.01F) {
               Builder.rectangle()
                  .size(new SizeState(subItemW, subH))
                  .radius(new QuadRadiusState(4.0F))
                  .color(new QuadColorState(GuiUtils.withAlpha(new Color(24, 24, 32), alpha * this.renderExpandAnim * subHovA)))
                  .build()
                  .render(mx, subItemX, itemY);
            }

            Color subTextCol = isSubActive
               ? GuiUtils.withAlpha(Color.WHITE, alpha * this.renderExpandAnim)
               : (
                  isSubHov
                     ? GuiUtils.withAlpha(new Color(215, 215, 230), alpha * this.renderExpandAnim)
                     : GuiUtils.withAlpha(new Color(130, 130, 145), alpha * this.renderExpandAnim)
               );
            GuiUtils.renderTextSafely(mx, sub.title, subItemX + 7.0F, itemCenterY - 3.8F, subTextCol, 7.8F);
            int count = this.getModuleCount(sub);
            String countStr = String.valueOf(count);
            MsdfFont mediumFont = (MsdfFont)ClickGuiIcons.MEDIUM_FONT.get();
            float countW = mediumFont != null ? mediumFont.getWidth(countStr, 6.5F) : 8.0F;
            Color countCol = isSubActive
               ? GuiUtils.withAlpha(themeColor, alpha * this.renderExpandAnim * 0.9F)
               : GuiUtils.withAlpha(new Color(90, 90, 105), alpha * this.renderExpandAnim * 0.8F);
            GuiUtils.renderTextSafely(mx, countStr, subItemX + subItemW - countW - 6.0F, itemCenterY - 3.3F, countCol, 6.5F);
         }

         context.disableScissor();
      }

      this.drawNavTabButton(mx, ClickGuiScreen.NavTab.MISC, tabX, miscTabY, tabW, tabH, mouseX, mouseY, popupsActive, deltaTime, alpha, themeColor, iconFont);
      if (!popupsActive && GuiUtils.isMouseOver(mouseX, mouseY, tabX, miscTabY, tabW, tabH)) {
         currentlyHoveredTab = ClickGuiScreen.NavTab.MISC;
      }

      this.drawNavTabButton(
         mx, ClickGuiScreen.NavTab.CONFIGS, tabX, configsTabY, tabW, tabH, mouseX, mouseY, popupsActive, deltaTime, alpha, themeColor, iconFont
      );
      if (!popupsActive && GuiUtils.isMouseOver(mouseX, mouseY, tabX, configsTabY, tabW, tabH)) {
         currentlyHoveredTab = ClickGuiScreen.NavTab.CONFIGS;
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

      float profY = winY + 380.0F - 44.0F;
      Builder.rectangle()
         .size(new SizeState(138.0F, 1.0F))
         .radius(new QuadRadiusState(0.0F))
         .color(new QuadColorState(new Color(16, 16, 22, 255)))
         .build()
         .render(mx, winX, profY);
      float profCenterY = profY + 22.0F;
      float avSize = 28.0F;
      float avX = winX + 10.0F;
      float avY = profCenterY - avSize / 2.0F;
      float avRadius = avSize / 2.0F;
      boolean drawnAvatar = false;
      Identifier avTex = DiscordAvatarManager.getAvatarTexture();
      if (avTex != null) {
         try {
            AbstractTexture tex = Minecraft.getInstance().getTextureManager().getTexture(avTex);
            if (tex != null) {
               Builder.texture()
                  .size(new SizeState(avSize, avSize))
                  .radius(new QuadRadiusState(avRadius))
                  .texture(0.0F, 0.0F, 1.0F, 1.0F, tex)
                  .color(new QuadColorState(GuiUtils.withAlpha(Color.WHITE, alpha)))
                  .build()
                  .render(mx, avX, avY);
               drawnAvatar = true;
            }
         } catch (Exception var54) {
         }
      }

      if (!drawnAvatar && this.minecraft.player != null) {
         try {
            Identifier skinTex = this.minecraft.player.getSkin().body().texturePath();
            if (skinTex != null) {
               AbstractTexture tex = this.minecraft.getTextureManager().getTexture(skinTex);
               if (tex != null) {
                  Builder.texture()
                     .size(new SizeState(avSize, avSize))
                     .radius(new QuadRadiusState(avRadius))
                     .texture(0.125F, 0.125F, 0.125F, 0.125F, tex)
                     .color(new QuadColorState(GuiUtils.withAlpha(Color.WHITE, alpha)))
                     .build()
                     .render(mx, avX, avY);
                  Builder.texture()
                     .size(new SizeState(avSize, avSize))
                     .radius(new QuadRadiusState(avRadius))
                     .texture(0.625F, 0.125F, 0.125F, 0.125F, tex)
                     .color(new QuadColorState(GuiUtils.withAlpha(Color.WHITE, alpha)))
                     .build()
                     .render(mx, avX, avY);
                  drawnAvatar = true;
               }
            }
         } catch (Exception var53) {
         }
      }

      if (!drawnAvatar) {
         Builder.rectangle()
            .size(new SizeState(avSize, avSize))
            .radius(new QuadRadiusState(avRadius))
            .color(new QuadColorState(GuiUtils.withAlpha(new Color(28, 28, 36), alpha)))
            .build()
            .render(mx, avX, avY);
      }

      String name = discordUsername != null && !discordUsername.isEmpty()
         ? discordUsername
         : (this.minecraft.player != null ? this.minecraft.player.getName().getString() : "User");
      float userTextX = avX + avSize + 7.0F;
      GuiUtils.renderTextSafely(mx, "User: " + name, userTextX, profCenterY - 4.0F, GuiUtils.withAlpha(Color.WHITE, alpha), 8.5F);
      float gearX = winX + 138.0F - 22.0F;
      float gearY = profCenterY - 4.8F;
      boolean gearHov = !this.isAnyModalOpen() && GuiUtils.isMouseOver(mouseX, mouseY, gearX - 4.0F, gearY - 4.0F, 18.0F, 18.0F);
      float gearHovA = this.hoverAnimations.getOrDefault("gear_hover", 0.0F);
      gearHovA = GuiUtils.animate(gearHovA, gearHov ? 1.0F : 0.0F, 15.0F, deltaTime);
      this.hoverAnimations.put("gear_hover", gearHovA);
      if (iconFont != null) {
         try {
            Color gearColor = GuiUtils.interpolateColor(new Color(210, 210, 225), Color.WHITE, gearHovA);
            Builder.text().font(iconFont).text("J").size(10.5F).color(GuiUtils.withAlpha(gearColor, alpha)).build().render(mx, gearX, gearY);
         } catch (Exception var52) {
         }
      }
   }

   private void drawNavTabButton(
      Matrix4f mx,
      ClickGuiScreen.NavTab tab,
      float tabX,
      float tabY,
      float tabW,
      float tabH,
      int mouseX,
      int mouseY,
      boolean popupsActive,
      float deltaTime,
      float alpha,
      Color themeColor,
      MsdfFont iconFont
   ) {
      boolean isCur = this.activeTab == tab;
      boolean hov = GuiUtils.isMouseOver(mouseX, mouseY, tabX, tabY, tabW, tabH);
      float hovA = this.hoverAnimations.getOrDefault("tab_" + tab.name(), 0.0F);
      hovA = GuiUtils.animate(hovA, hov && !popupsActive ? 1.0F : 0.0F, 15.0F, deltaTime);
      this.hoverAnimations.put("tab_" + tab.name(), hovA);
      if (!isCur && hovA > 0.01F) {
         Builder.rectangle()
            .size(new SizeState(tabW, tabH))
            .radius(new QuadRadiusState(5.0F))
            .color(new QuadColorState(GuiUtils.withAlpha(new Color(20, 20, 26), alpha * hovA)))
            .build()
            .render(mx, tabX, tabY);
      }

      float tabCenterY = tabY + tabH / 2.0F;
      float iconSize = 9.5F;
      float iconX = tabX + 9.0F;
      float iconY = tabCenterY - 4.8F;
      float textX = tabX + 23.0F;
      float textY = tabCenterY - 4.6F;
      if (iconFont != null && tab.icon != null) {
         try {
            float iconCX = iconX + iconSize / 2.0F;
            float iconCY = tabCenterY;
            GuiUtils.drawIconGlowShadow(mx, iconCX, iconCY, iconSize / 2.0F, themeColor, 0.08F * alpha);
            Builder.text().font(iconFont).text(tab.icon).size(iconSize).color(GuiUtils.withAlpha(themeColor, alpha)).build().render(mx, iconX, iconY);
         } catch (Exception var25) {
         }
      }

      GuiUtils.renderTextSafely(mx, tab.title, textX, textY, GuiUtils.withAlpha(Color.WHITE, alpha), 8.8F);
   }

   private void drawTopBar(GuiGraphicsExtractor context, Matrix4f mx, float winX, float winY, int mouseX, int mouseY, float deltaTime, float alpha, Color themeColor) {
      float topBarX = winX + 138.0F;
      float topBarW = 442.0F;
      float topBarH = 36.0F;
      Builder.rectangle()
         .size(new SizeState(topBarW, 1.0F))
         .radius(new QuadRadiusState(0.0F))
         .color(new QuadColorState(new Color(16, 16, 22, 255)))
         .build()
         .render(mx, topBarX, winY + topBarH);
      String displayTitle = this.activeTab.title;
      if (this.activeTab == ClickGuiScreen.NavTab.RENDER && this.activeRenderSubcategory != ClickGuiScreen.RenderSubcategory.ALL) {
         displayTitle = "Render > " + this.activeRenderSubcategory.title;
      }

      if (this.isSearching && !this.searchText.isEmpty()) {
         String truncSearch = this.searchText.length() > 14 ? this.searchText.substring(0, 14) + "..." : this.searchText;
         displayTitle = "Search: \"" + truncSearch + "\"";
      }

      GuiUtils.renderTextSafely(mx, displayTitle, topBarX + 16.0F, winY + topBarH / 2.0F - 3.5F, GuiUtils.withAlpha(Color.WHITE, alpha), 11.0F);
      float searchW = 125.0F;
      float searchH = 20.0F;
      float searchX = winX + 580.0F - searchW - 14.0F;
      float searchY = winY + (topBarH - searchH) / 2.0F;
      this.searchFocusAnim = GuiUtils.animate(this.searchFocusAnim, this.isSearching ? 1.0F : 0.0F, 16.0F, deltaTime);
      Color searchBg = GuiUtils.interpolateColor(new Color(14, 14, 18, 255), new Color(20, 20, 26, 255), this.searchFocusAnim);
      Builder.rectangle()
         .size(new SizeState(searchW, searchH))
         .radius(new QuadRadiusState(4.0F))
         .color(new QuadColorState(GuiUtils.withAlpha(searchBg, alpha)))
         .build()
         .render(mx, searchX, searchY);
      context.enableScissor((int)(searchX + 6.0F), (int)searchY, (int)(searchX + searchW - 18.0F), (int)(searchY + searchH));
      float maxTextW = searchW - 24.0F;
      float textStartX = searchX + 8.0F;
      if (this.isSearching) {
         long timeSinceTyping = System.currentTimeMillis() - this.lastTypingTime;
         float cursorAlpha = timeSinceTyping < 500L ? 1.0F : (System.currentTimeMillis() % 1000L < 500L ? 1.0F : 0.0F);
         float tw = ((MsdfFont)SEMIBOLD_FONT.get()).getWidth(this.searchText, 8.5F);
         float textOffset = tw > maxTextW ? tw - maxTextW : 0.0F;
         float textDrawX = textStartX - textOffset;
         GuiUtils.renderTextSafely(mx, this.searchText, textDrawX, searchY + 5.5F, GuiUtils.withAlpha(Color.WHITE, alpha), 8.5F);
         GuiUtils.renderTextSafely(mx, "_", textDrawX + tw, searchY + 5.5F, GuiUtils.withAlpha(Color.WHITE, alpha * cursorAlpha), 8.5F);
      } else {
         GuiUtils.renderTextSafely(mx, "Search...", textStartX, searchY + 5.5F, GuiUtils.withAlpha(new Color(110, 110, 125), alpha), 8.5F);
      }

      context.disableScissor();
      MsdfFont iconFont = (MsdfFont)ClickGuiIcons.CLICKGUI_ICONS.get();
      if (iconFont != null) {
         try {
            Builder.text()
               .font(iconFont)
               .text("I")
               .size(8.5F)
               .color(GuiUtils.withAlpha(themeColor, alpha))
               .build()
               .render(mx, searchX + searchW - 15.0F, searchY + 5.5F);
         } catch (Exception var27) {
         }
      }
   }

   private void drawMainContent(GuiGraphicsExtractor context, Matrix4f mx, float winX, float winY, int mouseX, int mouseY, float deltaTime, float alpha, Color themeColor) {
      float contentX = winX + 138.0F;
      float contentY = winY + 36.0F;
      float contentW = 442.0F;
      float contentH = 344.0F;
      context.enableScissor((int)contentX, (int)contentY, (int)(contentX + contentW), (int)(contentY + contentH));
      if (this.activeTab == ClickGuiScreen.NavTab.CONFIGS && !this.isSearching) {
         this.drawConfigsView(context, mx, contentX, contentY, contentW, contentH, mouseX, mouseY, deltaTime, alpha, themeColor);
      } else {
         this.drawModulesView(context, mx, contentX, contentY, contentW, contentH, mouseX, mouseY, deltaTime, alpha, themeColor);
      }

      context.disableScissor();
   }

   private void drawModulesView(
      GuiGraphicsExtractor context,
      Matrix4f mx,
      float contentX,
      float contentY,
      float contentW,
      float contentH,
      int mouseX,
      int mouseY,
      float deltaTime,
      float alpha,
      Color themeColor
   ) {
      float scroll = this.scrollOffsets.get(this.activeTab);
      float targetScroll = this.targetScrollOffsets.get(this.activeTab);
      scroll = GuiUtils.animate(scroll, targetScroll, 15.0F, deltaTime);
      this.scrollOffsets.put(this.activeTab, scroll);
      boolean searching = this.isSearching && !this.searchText.trim().isEmpty();
      Set<ModuleButton> matchingButtons = new HashSet<>();
      float margin = 11.0F;
      float colGap = 10.0F;
      float colW = (contentW - margin * 2.0F - colGap) / 2.0F;
      float col1X = contentX + margin;
      float col2X = col1X + colW + colGap;
      float col1Y = contentY + 10.0F + scroll;
      float col2Y = contentY + 10.0F + scroll;

      for (ModuleButton mb : this.allModuleButtons) {
         boolean matches;
         if (searching) {
            matches = mb.module.getName().toLowerCase().contains(this.searchText.toLowerCase());
         } else if (this.activeTab == ClickGuiScreen.NavTab.RENDER) {
            if (mb.module.getCategory() == Module.Category.RENDER) {
               if (this.activeRenderSubcategory == ClickGuiScreen.RenderSubcategory.ALL) {
                  matches = true;
               } else {
                  matches = getRenderSubcategory(mb.module) == this.activeRenderSubcategory;
               }
            } else {
               matches = false;
            }
         } else {
            matches = mb.module.getCategory() == this.activeTab.category;
         }

         mb.width = colW;
         float mbH = mb.calculateHeight();
         if (matches) {
            matchingButtons.add(mb);
            float targetX;
            float targetY;
            if (col1Y <= col2Y) {
               targetX = col1X;
               targetY = col1Y;
               col1Y += mbH + 9.0F;
            } else {
               targetX = col2X;
               targetY = col2Y;
               col2Y += mbH + 9.0F;
            }

            if (mb.renderX <= -9000.0F) {
               mb.renderX = targetX;
               mb.renderY = targetY + 10.0F;
            }

            mb.renderAlpha = GuiUtils.animate(mb.renderAlpha, 1.0F, 18.0F, deltaTime);
            mb.renderScale = GuiUtils.animate(mb.renderScale, 1.0F, 18.0F, deltaTime);
            mb.renderX = GuiUtils.animate(mb.renderX, targetX, 20.0F, deltaTime);
            mb.renderY = GuiUtils.animate(mb.renderY, targetY, 20.0F, deltaTime);
         } else {
            mb.renderAlpha = GuiUtils.animate(mb.renderAlpha, 0.0F, 22.0F, deltaTime);
            mb.renderScale = GuiUtils.animate(mb.renderScale, 0.94F, 22.0F, deltaTime);
            if (mb.renderX > -9000.0F) {
               mb.renderY = GuiUtils.animate(mb.renderY, mb.renderY + 6.0F, 16.0F, deltaTime);
            }
         }

         mb.x = mb.renderX;
         mb.y = mb.renderY;
      }

      boolean popupsActive = this.isAnyModalOpen();
      ModuleButton currentlyHoveredBtn = null;

      for (ModuleButton mb : this.allModuleButtons) {
         if (mb.renderAlpha > 0.01F) {
            float mbH = mb.calculateHeight();
            boolean inView = mb.y + mbH > contentY && mb.y < contentY + contentH;
            mb.isVisible = mb.renderAlpha > 0.4F && inView && matchingButtons.contains(mb);
            if (inView) {
               int passMouseX = popupsActive ? -1 : mouseX;
               int passMouseY = popupsActive ? -1 : mouseY;
               mb.render(context, this, passMouseX, passMouseY, alpha, deltaTime);
               if (!popupsActive
                  && mb.isVisible
                  && GuiUtils.isMouseOver(mouseX, mouseY, mb.x, mb.y, mb.width, mbH)
                  && mouseX >= contentX
                  && mouseY >= contentY
                  && mouseY <= contentY + contentH) {
                  currentlyHoveredBtn = mb;
               }
            }
         } else {
            mb.isVisible = false;
         }
      }

      if (currentlyHoveredBtn != this.lastHoveredModuleButton) {
         if (currentlyHoveredBtn != null && System.currentTimeMillis() - this.initTime > 250L) {
            ModSounds.playModuleSelect();
         }

         this.lastHoveredModuleButton = currentlyHoveredBtn;
      }

      float maxColumnY = Math.max(col1Y, col2Y) - scroll;
      float totalContentH = maxColumnY - (contentY + 10.0F);
      float maxScroll = Math.max(0.0F, totalContentH - (contentH - 20.0F));
      this.targetScrollOffsets.put(this.activeTab, Mth.clamp(targetScroll, -maxScroll, 0.0F));
      if (totalContentH > contentH - 20.0F) {
         float scrollbarW = 3.0F;
         float scrollbarX = contentX + contentW - 5.0F;
         float scrollbarTrackH = contentH - 16.0F;
         float scrollbarTrackY = contentY + 8.0F;
         float thumbRatio = (contentH - 20.0F) / totalContentH;
         float thumbH = Math.max(20.0F, scrollbarTrackH * thumbRatio);
         float scrollProgress = -scroll / maxScroll;
         float thumbY = scrollbarTrackY + (scrollbarTrackH - thumbH) * Mth.clamp(scrollProgress, 0.0F, 1.0F);
         Builder.rectangle()
            .size(new SizeState(scrollbarW, thumbH))
            .radius(new QuadRadiusState(1.5F))
            .color(new QuadColorState(GuiUtils.withAlpha(new Color(50, 50, 65), alpha * 0.6F)))
            .build()
            .render(mx, scrollbarX, thumbY);
      }
   }

   private void drawConfigsView(
      GuiGraphicsExtractor context,
      Matrix4f mx,
      float contentX,
      float contentY,
      float contentW,
      float contentH,
      int mouseX,
      int mouseY,
      float deltaTime,
      float alpha,
      Color themeColor
   ) {
      float margin = 11.0F;
      float colGap = 10.0F;
      float cardW = (contentW - margin * 2.0F - colGap) / 2.0F;
      float card1X = contentX + margin;
      float card2X = card1X + cardW + colGap;
      float cardY = contentY + 10.0F;
      float cardH = contentH - 20.0F;
      Builder.rectangle()
         .size(new SizeState(cardW, cardH))
         .radius(new QuadRadiusState(8.0F))
         .color(new QuadColorState(GuiUtils.withAlpha(new Color(6, 6, 8), alpha)))
         .build()
         .render(mx, card1X, cardY);
      GuiUtils.renderTextSafely(mx, "Configuration Manager", card1X + 12.0F, cardY + 10.0F, GuiUtils.withAlpha(Color.WHITE, alpha), 10.0F);
      float inputY = cardY + 30.0F;
      float inputW = cardW - 65.0F;
      float inputH = 18.0F;
      Builder.rectangle()
         .size(new SizeState(inputW, inputH))
         .radius(new QuadRadiusState(4.0F))
         .color(new QuadColorState(GuiUtils.withAlpha(new Color(14, 14, 18), alpha)))
         .build()
         .render(mx, card1X + 10.0F, inputY);
      String dispConfigName = this.isTypingNewConfig ? this.newConfigInput : (this.newConfigInput.isEmpty() ? "New config name..." : this.newConfigInput);
      Color inputCol = this.isTypingNewConfig ? Color.WHITE : (this.newConfigInput.isEmpty() ? new Color(100, 100, 120) : Color.WHITE);
      GuiUtils.renderTextSafely(mx, dispConfigName, card1X + 14.0F, inputY + 5.0F, GuiUtils.withAlpha(inputCol, alpha), 8.5F);
      float createBtnX = card1X + 10.0F + inputW + 4.0F;
      float createBtnW = cardW - inputW - 24.0F;
      boolean createHov = GuiUtils.isMouseOver(mouseX, mouseY, createBtnX, inputY, createBtnW, inputH);
      float createHovA = this.hoverAnimations.getOrDefault("btn_create", 0.0F);
      createHovA = GuiUtils.animate(createHovA, createHov ? 1.0F : 0.0F, 15.0F, deltaTime);
      this.hoverAnimations.put("btn_create", createHovA);
      Color createBg = GuiUtils.interpolateColor(new Color(24, 24, 32), themeColor, createHovA);
      Builder.rectangle()
         .size(new SizeState(createBtnW, inputH))
         .radius(new QuadRadiusState(4.0F))
         .color(new QuadColorState(GuiUtils.withAlpha(createBg, alpha)))
         .build()
         .render(mx, createBtnX, inputY);
      Color createTextCol = GuiUtils.interpolateColor(Color.WHITE, Color.BLACK, createHovA);
      GuiUtils.renderTextSafely(mx, "Create", createBtnX + 7.0F, inputY + 5.0F, GuiUtils.withAlpha(createTextCol, alpha), 8.5F);
      float listY = inputY + 24.0F;
      float listH = cardH - 88.0F;
      Builder.rectangle()
         .size(new SizeState(cardW - 20.0F, listH))
         .radius(new QuadRadiusState(4.0F))
         .color(new QuadColorState(GuiUtils.withAlpha(new Color(12, 12, 16), alpha)))
         .build()
         .render(mx, card1X + 10.0F, listY);
      float itemY = listY + 4.0F;

      for (String cfg : this.localConfigs) {
         boolean isSel = cfg.equalsIgnoreCase(this.selectedConfig);
         boolean itemHov = GuiUtils.isMouseOver(mouseX, mouseY, card1X + 12.0F, itemY, cardW - 24.0F, 15.0F);
         float cfgHovA = this.hoverAnimations.getOrDefault("cfg_" + cfg, 0.0F);
         cfgHovA = GuiUtils.animate(cfgHovA, itemHov ? 1.0F : 0.0F, 15.0F, deltaTime);
         this.hoverAnimations.put("cfg_" + cfg, cfgHovA);
         if (isSel) {
            Builder.rectangle()
               .size(new SizeState(cardW - 24.0F, 15.0F))
               .radius(new QuadRadiusState(3.0F))
               .color(new QuadColorState(GuiUtils.withAlpha(themeColor, alpha * 0.35F)))
               .build()
               .render(mx, card1X + 12.0F, itemY);
         } else if (cfgHovA > 0.01F) {
            Builder.rectangle()
               .size(new SizeState(cardW - 24.0F, 15.0F))
               .radius(new QuadRadiusState(3.0F))
               .color(new QuadColorState(GuiUtils.withAlpha(new Color(22, 22, 28), alpha * cfgHovA)))
               .build()
               .render(mx, card1X + 12.0F, itemY);
         }

         Color itemCol = isSel ? themeColor : GuiUtils.interpolateColor(new Color(180, 180, 195), Color.WHITE, cfgHovA);
         GuiUtils.renderTextSafely(mx, cfg, card1X + 18.0F, itemY + 3.5F, GuiUtils.withAlpha(itemCol, alpha), 8.5F);
         itemY += 17.0F;
      }

      float btnRowY = cardY + cardH - 24.0F;
      float btnW = (cardW - 28.0F) / 3.0F;
      boolean loadHov = GuiUtils.isMouseOver(mouseX, mouseY, card1X + 10.0F, btnRowY, btnW, 16.0F);
      float loadHovA = this.hoverAnimations.getOrDefault("btn_load", 0.0F);
      loadHovA = GuiUtils.animate(loadHovA, loadHov ? 1.0F : 0.0F, 15.0F, deltaTime);
      this.hoverAnimations.put("btn_load", loadHovA);
      Color loadBg = GuiUtils.interpolateColor(new Color(24, 24, 34), themeColor, loadHovA);
      Builder.rectangle()
         .size(new SizeState(btnW, 16.0F))
         .radius(new QuadRadiusState(4.0F))
         .color(new QuadColorState(GuiUtils.withAlpha(loadBg, alpha)))
         .build()
         .render(mx, card1X + 10.0F, btnRowY);
      Color loadTextCol = GuiUtils.interpolateColor(Color.WHITE, Color.BLACK, loadHovA);
      GuiUtils.renderTextSafely(
         mx,
         "Load",
         card1X + 10.0F + (btnW - ((MsdfFont)SEMIBOLD_FONT.get()).getWidth("Load", 8.5F)) / 2.0F,
         btnRowY + 4.0F,
         GuiUtils.withAlpha(loadTextCol, alpha),
         8.5F
      );
      float saveBtnX = card1X + 10.0F + btnW + 4.0F;
      boolean saveHov = GuiUtils.isMouseOver(mouseX, mouseY, saveBtnX, btnRowY, btnW, 16.0F);
      float saveHovA = this.hoverAnimations.getOrDefault("btn_save", 0.0F);
      saveHovA = GuiUtils.animate(saveHovA, saveHov ? 1.0F : 0.0F, 15.0F, deltaTime);
      this.hoverAnimations.put("btn_save", saveHovA);
      Color saveBg = GuiUtils.interpolateColor(new Color(24, 24, 34), themeColor, saveHovA);
      Builder.rectangle()
         .size(new SizeState(btnW, 16.0F))
         .radius(new QuadRadiusState(4.0F))
         .color(new QuadColorState(GuiUtils.withAlpha(saveBg, alpha)))
         .build()
         .render(mx, saveBtnX, btnRowY);
      Color saveTextCol = GuiUtils.interpolateColor(Color.WHITE, Color.BLACK, saveHovA);
      GuiUtils.renderTextSafely(
         mx,
         "Save",
         saveBtnX + (btnW - ((MsdfFont)SEMIBOLD_FONT.get()).getWidth("Save", 8.5F)) / 2.0F,
         btnRowY + 4.0F,
         GuiUtils.withAlpha(saveTextCol, alpha),
         8.5F
      );
      float delBtnX = saveBtnX + btnW + 4.0F;
      boolean delHov = GuiUtils.isMouseOver(mouseX, mouseY, delBtnX, btnRowY, btnW, 16.0F);
      float delHovA = this.hoverAnimations.getOrDefault("btn_del", 0.0F);
      delHovA = GuiUtils.animate(delHovA, delHov ? 1.0F : 0.0F, 15.0F, deltaTime);
      this.hoverAnimations.put("btn_del", delHovA);
      Color delBg = GuiUtils.interpolateColor(new Color(34, 20, 20), new Color(220, 50, 50), delHovA);
      Builder.rectangle()
         .size(new SizeState(btnW, 16.0F))
         .radius(new QuadRadiusState(4.0F))
         .color(new QuadColorState(GuiUtils.withAlpha(delBg, alpha)))
         .build()
         .render(mx, delBtnX, btnRowY);
      Color delTextCol = GuiUtils.interpolateColor(new Color(220, 80, 80), Color.WHITE, delHovA);
      GuiUtils.renderTextSafely(
         mx,
         "Delete",
         delBtnX + (btnW - ((MsdfFont)SEMIBOLD_FONT.get()).getWidth("Delete", 8.5F)) / 2.0F,
         btnRowY + 4.0F,
         GuiUtils.withAlpha(delTextCol, alpha),
         9.0F
      );
      Builder.rectangle()
         .size(new SizeState(cardW, cardH))
         .radius(new QuadRadiusState(8.0F))
         .color(new QuadColorState(GuiUtils.withAlpha(new Color(6, 6, 8), alpha)))
         .build()
         .render(mx, card2X, cardY);
      GuiUtils.renderTextSafely(mx, "Config Details", card2X + 12.0F, cardY + 10.0F, GuiUtils.withAlpha(Color.WHITE, alpha), 10.0F);
      float infoY = cardY + 32.0F;
      GuiUtils.renderTextSafely(mx, "Selected Config: " + this.selectedConfig, card2X + 14.0F, infoY, GuiUtils.withAlpha(themeColor, alpha), 9.0F);
      infoY += 16.0F;
      GuiUtils.renderTextSafely(mx, "Total Configs: " + this.localConfigs.size(), card2X + 14.0F, infoY, GuiUtils.withAlpha(Color.WHITE, alpha), 9.0F);
      infoY += 16.0F;
      GuiUtils.renderTextSafely(
         mx, "Total Modules: " + this.allModuleButtons.size(), card2X + 14.0F, infoY, GuiUtils.withAlpha(new Color(170, 170, 185), alpha), 9.0F
      );
      infoY += 16.0F;
      GuiUtils.renderTextSafely(mx, "Storage: Cloud Profile (API)", card2X + 14.0F, infoY, GuiUtils.withAlpha(new Color(140, 140, 155), alpha), 8.5F);
      if (System.currentTimeMillis() - this.configStatusTime < 4000L && !this.configStatusMessage.isEmpty()) {
         GuiUtils.renderTextSafely(mx, this.configStatusMessage, card2X + 14.0F, cardY + cardH - 24.0F, GuiUtils.withAlpha(themeColor, alpha), 9.0F);
      }
   }

   private void drawSettingsModal(
      GuiGraphicsExtractor context, Matrix4f mx, float winX, float winY, int mouseX, int mouseY, float deltaTime, float alpha, Color themeColor
   ) {
      float sm = this.settingsModalAnim;
      float ease = 1.0F - (float)Math.pow(1.0F - sm, 3.0);
      float scale = 0.9F + 0.1F * ease;
      float modalAlpha = alpha * ease;
      float popW = 106.0F;
      float popH = 106.0F;
      float popX = this.getSettingsModalX(winX);
      float popY = this.getSettingsModalY(winY);
      context.pose().pushMatrix();
      if (scale < 0.999F) {
         float scx = popX + popW / 2.0F;
         float scy = popY + popH / 2.0F;
         context.pose().translate(scx, scy);
         context.pose().scale(scale, scale);
         context.pose().translate(-scx, -scy);
      }

      Matrix4f matrix = GuiCompat.matrix4f(context);

      for (int i = 6; i >= 0; i--) {
         float progress = i / 6.0F;
         float spread = progress * 4.0F;
         int sAlpha = (int)(60.0F * (1.0F - progress) * (1.0F - progress) * modalAlpha);
         if (sAlpha > 0) {
            Builder.rectangle()
               .size(new SizeState(popW + spread * 2.0F, popH + spread * 2.0F))
               .radius(new QuadRadiusState(8.0F + spread))
               .color(new QuadColorState(new Color(0, 0, 0, sAlpha)))
               .build()
               .render(matrix, popX - spread, popY - spread);
         }
      }

      Builder.rectangle()
         .size(new SizeState(popW, popH))
         .radius(new QuadRadiusState(8.0F))
         .color(new QuadColorState(new Color(0, 0, 0, (int)(255.0F * modalAlpha))))
         .build()
         .render(matrix, popX, popY);
      MsdfFont medFont = null;

      try {
         medFont = (MsdfFont)ClickGuiIcons.MEDIUM_FONT.get();
      } catch (Exception var41) {
      }

      float contentW = popW - 10.0F;
      float rX = popX + 5.0F;
      float rowH = 21.0F;
      float r1Y = this.getSettingsRowY(popY, 0);
      boolean colHov = GuiUtils.isMouseOver(mouseX, mouseY, rX, r1Y, contentW, rowH);
      if (colHov || this.colorPickerOpen) {
         Builder.rectangle()
            .size(new SizeState(contentW, rowH))
            .radius(new QuadRadiusState(5.0F))
            .color(new QuadColorState(new Color(25, 25, 36, (int)(200.0F * modalAlpha))))
            .build()
            .render(matrix, rX, r1Y);
      }

      GuiUtils.renderTextSafely(matrix, "Client color", rX + 5.0F, r1Y + 6.0F, GuiUtils.withAlpha(Color.WHITE, modalAlpha), 7.5F);
      float dotSize = 10.0F;
      float dotX = popX + popW - 5.0F - dotSize - 4.0F;
      float dotY = r1Y + (rowH - dotSize) / 2.0F;
      Builder.rectangle()
         .size(new SizeState(dotSize, dotSize))
         .radius(new QuadRadiusState(dotSize / 2.0F))
         .color(new QuadColorState(GuiUtils.withAlpha(themeColor, modalAlpha)))
         .build()
         .render(matrix, dotX, dotY);
      float r2Y = this.getSettingsRowY(popY, 1);
      boolean sndHov = GuiUtils.isMouseOver(mouseX, mouseY, rX, r2Y, contentW, rowH);
      if (sndHov || this.soundSettingsOpen) {
         Builder.rectangle()
            .size(new SizeState(contentW, rowH))
            .radius(new QuadRadiusState(5.0F))
            .color(new QuadColorState(new Color(25, 25, 36, (int)(200.0F * modalAlpha))))
            .build()
            .render(matrix, rX, r2Y);
      }

      GuiUtils.renderTextSafely(matrix, "Sounds", rX + 5.0F, r2Y + 6.0F, GuiUtils.withAlpha(Color.WHITE, modalAlpha), 7.5F);
      String masterVolStr = Math.round(SoundSettings.getMasterVolume()) + "%";
      float volStrW = medFont != null ? medFont.getWidth(masterVolStr, 7.0F) : 18.0F;
      GuiUtils.renderTextSafely(matrix, masterVolStr, popX + popW - 9.0F - volStrW, r2Y + 6.0F, GuiUtils.withAlpha(themeColor, modalAlpha), 7.0F);
      float r3Y = this.getSettingsRowY(popY, 2);
      boolean scaleHov = GuiUtils.isMouseOver(mouseX, mouseY, rX, r3Y, contentW, rowH);
      if (scaleHov || this.scaleModalOpen) {
         Builder.rectangle()
            .size(new SizeState(contentW, rowH))
            .radius(new QuadRadiusState(5.0F))
            .color(new QuadColorState(new Color(25, 25, 36, (int)(200.0F * modalAlpha))))
            .build()
            .render(matrix, rX, r3Y);
      }

      GuiUtils.renderTextSafely(matrix, "GUI scale", rX + 5.0F, r3Y + 6.0F, GuiUtils.withAlpha(Color.WHITE, modalAlpha), 7.5F);
      String scaleStr = GuiScaleSettings.getScaleLabel() + " >";
      float scaleStrW = medFont != null ? medFont.getWidth(scaleStr, 7.0F) : 22.0F;
      GuiUtils.renderTextSafely(matrix, scaleStr, popX + popW - 9.0F - scaleStrW, r3Y + 6.0F, GuiUtils.withAlpha(themeColor, modalAlpha), 7.0F);
      float r4Y = this.getSettingsRowY(popY, 3);
      boolean hudHov = GuiUtils.isMouseOver(mouseX, mouseY, rX, r4Y, contentW, rowH);
      Color hudBg = hudHov ? new Color(38, 38, 48) : new Color(22, 22, 28);
      Builder.rectangle()
         .size(new SizeState(contentW, rowH))
         .radius(new QuadRadiusState(5.0F))
         .color(new QuadColorState(GuiUtils.withAlpha(hudBg, modalAlpha)))
         .build()
         .render(matrix, rX, r4Y);
      float hudTw = medFont != null ? medFont.getWidth("Hud editor", 7.5F) : 38.0F;
      float hudTx = rX + (contentW - hudTw) / 2.0F;
      GuiUtils.renderTextSafely(matrix, "Hud editor", hudTx, r4Y + 6.0F, GuiUtils.withAlpha(Color.WHITE, modalAlpha), 7.5F);
      context.pose().popMatrix();
   }

   private void drawSoundsModal(GuiGraphicsExtractor context, Matrix4f mx, float winX, float winY, int mouseX, int mouseY, float deltaTime, float alpha, Color themeColor) {
      float sndEase = 1.0F - (float)Math.pow(1.0F - this.soundSettingsAnim, 3.0);
      float sndScale = 0.9F + 0.1F * sndEase;
      float sndAlpha = alpha * sndEase;
      float sndW = 138.0F;
      float sndH = 190.0F;
      float sndX = this.getSoundSettingsX(winX);
      float sndY = this.getSoundSettingsY(winY);
      context.pose().pushMatrix();
      if (sndScale < 0.999F) {
         float scx = sndX + sndW / 2.0F;
         float scy = sndY + sndH / 2.0F;
         context.pose().translate(scx, scy);
         context.pose().scale(sndScale, sndScale);
         context.pose().translate(-scx, -scy);
      }

      Matrix4f matrix = GuiCompat.matrix4f(context);

      for (int i = 6; i >= 0; i--) {
         float progress = i / 6.0F;
         float spread = progress * 4.0F;
         int sAlpha = (int)(60.0F * (1.0F - progress) * (1.0F - progress) * sndAlpha);
         if (sAlpha > 0) {
            Builder.rectangle()
               .size(new SizeState(sndW + spread * 2.0F, sndH + spread * 2.0F))
               .radius(new QuadRadiusState(8.0F + spread))
               .color(new QuadColorState(new Color(0, 0, 0, sAlpha)))
               .build()
               .render(matrix, sndX - spread, sndY - spread);
         }
      }

      Builder.rectangle()
         .size(new SizeState(sndW, sndH))
         .radius(new QuadRadiusState(8.0F))
         .color(new QuadColorState(new Color(0, 0, 0, (int)(255.0F * sndAlpha))))
         .build()
         .render(matrix, sndX, sndY);
      SoundSettingsPopup.render(context, sndX, sndY, sndW, sndH, mouseX, mouseY, deltaTime, sndAlpha, themeColor);
      context.pose().popMatrix();
   }

   private void drawScaleModal(GuiGraphicsExtractor context, Matrix4f mx, float winX, float winY, int mouseX, int mouseY, float deltaTime, float alpha, Color themeColor) {
      float scaleEase = 1.0F - (float)Math.pow(1.0F - this.scaleModalAnim, 3.0);
      float scaleScale = 0.9F + 0.1F * scaleEase;
      float scaleAlpha = alpha * scaleEase;
      float scW = 94.0F;
      float scH = 162.0F;
      float scX = this.getScaleSettingsX(winX);
      float scY = this.getScaleSettingsY(winY);
      context.pose().pushMatrix();
      if (scaleScale < 0.999F) {
         float scx = scX + scW / 2.0F;
         float scy = scY + scH / 2.0F;
         context.pose().translate(scx, scy);
         context.pose().scale(scaleScale, scaleScale);
         context.pose().translate(-scx, -scy);
      }

      Matrix4f matrix = GuiCompat.matrix4f(context);

      for (int i = 6; i >= 0; i--) {
         float progress = i / 6.0F;
         float spread = progress * 4.0F;
         int sAlpha = (int)(60.0F * (1.0F - progress) * (1.0F - progress) * scaleAlpha);
         if (sAlpha > 0) {
            Builder.rectangle()
               .size(new SizeState(scW + spread * 2.0F, scH + spread * 2.0F))
               .radius(new QuadRadiusState(8.0F + spread))
               .color(new QuadColorState(new Color(0, 0, 0, sAlpha)))
               .build()
               .render(matrix, scX - spread, scY - spread);
         }
      }

      Builder.rectangle()
         .size(new SizeState(scW, scH))
         .radius(new QuadRadiusState(8.0F))
         .color(new QuadColorState(new Color(0, 0, 0, (int)(255.0F * scaleAlpha))))
         .build()
         .render(matrix, scX, scY);
      ScaleSettingsPopup.render(context, scX, scY, scW, scH, mouseX, mouseY, deltaTime, scaleAlpha, themeColor);
      context.pose().popMatrix();
   }

   private void drawColorPickerModal(
      GuiGraphicsExtractor context, Matrix4f mx, float winX, float winY, int mouseX, int mouseY, float deltaTime, float alpha, Color themeColor
   ) {
      float cpEase = 1.0F - (float)Math.pow(1.0F - this.colorPickerAnim, 3.0);
      float cpScale = 0.9F + 0.1F * cpEase;
      float cpAlpha = alpha * cpEase;
      float cpW = 106.0F;
      float cpH = 94.0F;
      float cpX = this.getColorPickerX(winX);
      float cpY = this.getColorPickerY(winY);
      context.pose().pushMatrix();
      if (cpScale < 0.999F) {
         float scx = cpX + cpW / 2.0F;
         float scy = cpY + cpH / 2.0F;
         context.pose().translate(scx, scy);
         context.pose().scale(cpScale, cpScale);
         context.pose().translate(-scx, -scy);
      }

      Matrix4f matrix = GuiCompat.matrix4f(context);

      for (int i = 6; i >= 0; i--) {
         float progress = i / 6.0F;
         float spread = progress * 4.0F;
         int sAlpha = (int)(60.0F * (1.0F - progress) * (1.0F - progress) * cpAlpha);
         if (sAlpha > 0) {
            Builder.rectangle()
               .size(new SizeState(cpW + spread * 2.0F, cpH + spread * 2.0F))
               .radius(new QuadRadiusState(8.0F + spread))
               .color(new QuadColorState(new Color(0, 0, 0, sAlpha)))
               .build()
               .render(matrix, cpX - spread, cpY - spread);
         }
      }

      Builder.rectangle()
         .size(new SizeState(cpW, cpH))
         .radius(new QuadRadiusState(8.0F))
         .color(new QuadColorState(new Color(0, 0, 0, (int)(255.0F * cpAlpha))))
         .build()
         .render(matrix, cpX, cpY);
      this.themePicker.x = cpX;
      this.themePicker.y = cpY;
      this.themePicker.width = cpW;
      this.themePicker.height = cpH;
      this.themePicker.animate(deltaTime);
      this.themePicker.render(matrix, mouseX, mouseY, cpAlpha);
      context.pose().popMatrix();
   }

   private void renderShadow(Matrix4f matrix, float x, float y, float w, float h, float radius, float masterAlpha) {
      int shadowSteps = 12;
      float maxSpread = 10.0F;

      for (int i = shadowSteps - 1; i >= 0; i--) {
         float progress = (float)i / shadowSteps;
         float spread = progress * maxSpread;
         float alphaFactor = (1.0F - progress) * (1.0F - progress);
         int alpha = (int)(120.0F * alphaFactor * masterAlpha);
         if (alpha > 0) {
            Builder.rectangle()
               .size(new SizeState(w + spread * 2.0F, h + spread * 2.0F))
               .radius(new QuadRadiusState(radius + spread))
               .color(new QuadColorState(new Color(0, 0, 0, alpha)))
               .build()
               .render(matrix, x - spread, y - spread + 1.0F);
         }
      }
   }

   @Override
   public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
      return this.mouseClicked(event.x(), event.y(), xyz.angames.astolfoclient.client.compat.KeyMap.sdlButtonToGlfw(event.button()));
   }

   @Override
   public boolean mouseReleased(MouseButtonEvent event) {
      return this.mouseReleased(event.x(), event.y(), xyz.angames.astolfoclient.client.compat.KeyMap.sdlButtonToGlfw(event.button()));
   }

   @Override
   public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
      return this.mouseDragged(event.x(), event.y(), xyz.angames.astolfoclient.client.compat.KeyMap.sdlButtonToGlfw(event.button()), dx, dy);
   }

   @Override
   public boolean keyPressed(KeyEvent event) {
      return this.keyPressed(xyz.angames.astolfoclient.client.compat.KeyMap.sdlToGlfw(event.key()), event.keycode(), xyz.angames.astolfoclient.client.compat.KeyMap.glfwMods(event));
   }

   @Override
   public boolean charTyped(CharacterEvent event) {
      return this.charTyped((char)event.codepoint(), 0);
   }

   public boolean mouseClicked(double mouseX, double mouseY, int button) {
      float sm = GuiUtils.getScaleModifier(this.minecraft) * GuiScaleSettings.getScale();
      float adjMouseX = (float)(mouseX / sm);
      float adjMouseY = (float)(mouseY / sm);
      if (System.currentTimeMillis() - this.initTime < 150L) {
         return true;
      }

      float scaledWidth = this.width / sm;
      float scaledHeight = this.height / sm;
      float winX = (scaledWidth - 580.0F) / 2.0F;
      float winY = (scaledHeight - 380.0F) / 2.0F;
      if (ModePopupState.mouseClicked(adjMouseX, adjMouseY, button, winX, winY, 580.0F, 380.0F)) {
         return true;
      }

      if (SubSettingsPopupState.mouseClicked(adjMouseX, adjMouseY, button, winX, winY, 580.0F, 380.0F)) {
         return true;
      }

      if (MultiSelectPopupState.mouseClicked(adjMouseX, adjMouseY, button, winX, winY, 580.0F, 380.0F)) {
         return true;
      }

      float popW = 106.0F;
      float popH = 106.0F;
      float popX = this.getSettingsModalX(winX);
      float popY = this.getSettingsModalY(winY);
      float cpW = 106.0F;
      float cpH = 94.0F;
      float cpX = this.getColorPickerX(winX);
      float cpY = this.getColorPickerY(winY);
      float sndW = 138.0F;
      float sndH = 190.0F;
      float sndX = this.getSoundSettingsX(winX);
      float sndY = this.getSoundSettingsY(winY);
      float scW = 94.0F;
      float scH = 162.0F;
      float scX = this.getScaleSettingsX(winX);
      float scY = this.getScaleSettingsY(winY);
      if (this.colorPickerOpen && this.colorPickerAnim > 0.05F) {
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

      if (this.soundSettingsOpen && this.soundSettingsAnim > 0.05F && GuiUtils.isMouseOver(adjMouseX, adjMouseY, sndX, sndY, sndW, sndH)) {
         return SoundSettingsPopup.mouseClicked(adjMouseX, adjMouseY, button, sndX, sndY, sndW, sndH) ? true : true;
      }

      if (this.scaleModalOpen && this.scaleModalAnim > 0.05F && GuiUtils.isMouseOver(adjMouseX, adjMouseY, scX, scY, scW, scH)) {
         return ScaleSettingsPopup.mouseClicked(adjMouseX, adjMouseY, button, scX, scY, scW, scH) ? true : true;
      }

      if (this.settingsModalOpen && this.settingsModalAnim > 0.05F) {
         float contentW = popW - 10.0F;
         float rX = popX + 5.0F;
         float rowH = 21.0F;
         float r1Y = this.getSettingsRowY(popY, 0);
         float r2Y = this.getSettingsRowY(popY, 1);
         float r3Y = this.getSettingsRowY(popY, 2);
         float r4Y = this.getSettingsRowY(popY, 3);
         if (GuiUtils.isMouseOver(adjMouseX, adjMouseY, rX, r1Y, contentW, rowH)) {
            this.colorPickerOpen = !this.colorPickerOpen;
            if (this.colorPickerOpen) {
               this.themePicker.loadColor();
               this.soundSettingsOpen = false;
               this.scaleModalOpen = false;
            }

            ModSounds.playModeOpen();
            return true;
         }

         if (GuiUtils.isMouseOver(adjMouseX, adjMouseY, rX, r2Y, contentW, rowH)) {
            this.soundSettingsOpen = !this.soundSettingsOpen;
            if (this.soundSettingsOpen) {
               this.colorPickerOpen = false;
               this.scaleModalOpen = false;
            }

            ModSounds.playModeOpen();
            return true;
         }

         if (GuiUtils.isMouseOver(adjMouseX, adjMouseY, rX, r3Y, contentW, rowH)) {
            this.scaleModalOpen = !this.scaleModalOpen;
            if (this.scaleModalOpen) {
               this.colorPickerOpen = false;
               this.soundSettingsOpen = false;
            }

            ModSounds.playModeOpen();
            return true;
         }

         if (GuiUtils.isMouseOver(adjMouseX, adjMouseY, rX, r4Y, contentW, rowH)) {
            ModSounds.playModeOpen();
            this.minecraft.gui.setScreen(new HudEditorScreen());
            return true;
         }

         if (GuiUtils.isMouseOver(adjMouseX, adjMouseY, popX, popY, popW, popH)) {
            return true;
         }

         boolean inColor = this.colorPickerOpen && GuiUtils.isMouseOver(adjMouseX, adjMouseY, cpX, cpY, cpW, cpH);
         boolean inSounds = this.soundSettingsOpen && GuiUtils.isMouseOver(adjMouseX, adjMouseY, sndX, sndY, sndW, sndH);
         boolean inScale = this.scaleModalOpen && GuiUtils.isMouseOver(adjMouseX, adjMouseY, scX, scY, scW, scH);
         if (!inColor && !inSounds && !inScale) {
            this.settingsModalOpen = false;
            this.colorPickerOpen = false;
            this.soundSettingsOpen = false;
            this.scaleModalOpen = false;
            ModSounds.playModeOpen();
            return true;
         }
      }

      float searchW = 125.0F;
      float searchH = 20.0F;
      float searchX = winX + 580.0F - searchW - 14.0F;
      float searchY = winY + (36.0F - searchH) / 2.0F;
      if (GuiUtils.isMouseOver(adjMouseX, adjMouseY, searchX, searchY, searchW, searchH)) {
         this.isSearching = !this.isSearching;
         if (!this.isSearching) {
            this.searchText = "";
         }

         ModSounds.playSearchClick();
         return true;
      } else {
         ClickGuiScreen.RenderSubcategory[] subcategories = ClickGuiScreen.RenderSubcategory.values();
         float subH = 17.0F;
         float subGap = 2.5F;
         float subTotalH = subcategories.length * subH + (subcategories.length - 1) * subGap + 4.0F;
         float currentSubHeight = subTotalH * this.renderExpandAnim;
         float tabW = 122.0F;
         float tabH = 22.0F;
         float tabX = winX + (138.0F - tabW) / 2.0F;
         float renderTabY = winY + 70.0F;
         float miscTabY = renderTabY + tabH + 3.5F + currentSubHeight;
         float otherHeaderY = miscTabY + tabH + 6.0F;
         float configsTabY = otherHeaderY + 14.0F;
         if (this.activeTab == ClickGuiScreen.NavTab.RENDER && this.renderExpandAnim > 0.4F) {
            float subStartY = renderTabY + tabH + 3.0F;
            float subItemX = tabX + 26.0F;
            float subItemW = tabW - 27.0F;

            for (int i = 0; i < subcategories.length; i++) {
               ClickGuiScreen.RenderSubcategory sub = subcategories[i];
               float itemY = subStartY + i * (subH + subGap);
               if (GuiUtils.isMouseOver(adjMouseX, adjMouseY, subItemX, itemY, subItemW, subH)) {
                  if (this.activeRenderSubcategory != sub) {
                     this.activeRenderSubcategory = sub;
                     this.targetScrollOffsets.put(ClickGuiScreen.NavTab.RENDER, 0.0F);
                     ModSounds.playModuleSelect();
                  }

                  this.isSearching = false;
                  this.searchText = "";
                  return true;
               }
            }
         }

         if (GuiUtils.isMouseOver(adjMouseX, adjMouseY, tabX, renderTabY, tabW, tabH)) {
            if (this.activeTab != ClickGuiScreen.NavTab.RENDER) {
               this.activeTab = ClickGuiScreen.NavTab.RENDER;
               ModSounds.playCategoryChange();
            } else {
               this.activeRenderSubcategory = ClickGuiScreen.RenderSubcategory.ALL;
               this.targetScrollOffsets.put(ClickGuiScreen.NavTab.RENDER, 0.0F);
               ModSounds.playModuleSelect();
            }

            this.isSearching = false;
            this.searchText = "";
            return true;
         } else if (GuiUtils.isMouseOver(adjMouseX, adjMouseY, tabX, miscTabY, tabW, tabH)) {
            if (this.activeTab != ClickGuiScreen.NavTab.MISC) {
               this.activeTab = ClickGuiScreen.NavTab.MISC;
               ModSounds.playCategoryChange();
            }

            this.isSearching = false;
            this.searchText = "";
            return true;
         } else if (GuiUtils.isMouseOver(adjMouseX, adjMouseY, tabX, configsTabY, tabW, tabH)) {
            if (this.activeTab != ClickGuiScreen.NavTab.CONFIGS) {
               this.activeTab = ClickGuiScreen.NavTab.CONFIGS;
               ModSounds.playCategoryChange();
            }

            this.isSearching = false;
            this.searchText = "";
            return true;
         } else {
            float profY = winY + 380.0F - 44.0F;
            float profCenterY = profY + 22.0F;
            float gearX = winX + 138.0F - 22.0F;
            float gearY = profCenterY - 4.8F;
            if (GuiUtils.isMouseOver(adjMouseX, adjMouseY, gearX - 4.0F, gearY - 4.0F, 18.0F, 18.0F)) {
               this.settingsModalOpen = !this.settingsModalOpen;
               if (!this.settingsModalOpen) {
                  this.colorPickerOpen = false;
                  this.soundSettingsOpen = false;
                  this.scaleModalOpen = false;
               } else {
                  this.themePicker.loadColor();
               }

               ModSounds.playModeOpen();
               return true;
            } else {
               if (this.activeTab == ClickGuiScreen.NavTab.CONFIGS && !this.isSearching) {
                  float contentX = winX + 138.0F;
                  float contentY = winY + 36.0F;
                  float contentW = 442.0F;
                  float contentH = 344.0F;
                  float margin = 11.0F;
                  float colGap = 10.0F;
                  float cardW = (contentW - margin * 2.0F - colGap) / 2.0F;
                  float card1X = contentX + margin;
                  float cardY = contentY + 10.0F;
                  float cardH = contentH - 20.0F;
                  float inputY = cardY + 30.0F;
                  float inputW = cardW - 65.0F;
                  float inputH = 18.0F;
                  if (GuiUtils.isMouseOver(adjMouseX, adjMouseY, card1X + 10.0F, inputY, inputW, inputH)) {
                     this.isTypingNewConfig = true;
                     return true;
                  }

                  this.isTypingNewConfig = false;
                  float createBtnX = card1X + 10.0F + inputW + 4.0F;
                  float createBtnW = cardW - inputW - 24.0F;
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

                  float listY = inputY + 24.0F;
                  float itemY = listY + 4.0F;

                  for (String cfg : this.localConfigs) {
                     if (GuiUtils.isMouseOver(adjMouseX, adjMouseY, card1X + 12.0F, itemY, cardW - 24.0F, 15.0F)) {
                        this.selectedConfig = cfg;
                        return true;
                     }

                     itemY += 17.0F;
                  }

                  float btnRowY = cardY + cardH - 24.0F;
                  float btnW = (cardW - 28.0F) / 3.0F;
                  if (GuiUtils.isMouseOver(adjMouseX, adjMouseY, card1X + 10.0F, btnRowY, btnW, 16.0F)) {
                     new Thread(() -> {
                        boolean ok = AstolfoclientClient.configManager.loadConfig(this.selectedConfig);
                        this.configStatusMessage = ok ? "Cloud config loaded!" : "Failed to load from cloud!";
                        this.configStatusTime = System.currentTimeMillis();
                     }).start();
                     return true;
                  }

                  float saveBtnX = card1X + 10.0F + btnW + 4.0F;
                  if (GuiUtils.isMouseOver(adjMouseX, adjMouseY, saveBtnX, btnRowY, btnW, 16.0F)) {
                     new Thread(() -> {
                        String err = AstolfoclientClient.configManager.saveConfig(this.selectedConfig);
                        this.configStatusMessage = err == null ? "Cloud config saved!" : "Save error: " + err;
                        this.configStatusTime = System.currentTimeMillis();
                        this.reloadConfigs();
                     }).start();
                     return true;
                  }

                  float delBtnX = saveBtnX + btnW + 4.0F;
                  if (GuiUtils.isMouseOver(adjMouseX, adjMouseY, delBtnX, btnRowY, btnW, 16.0F)) {
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
                     if (mb.isVisible && GuiUtils.isMouseOver(adjMouseX, adjMouseY, mb.x, mb.y, mb.width, mb.height)) {
                        if (button == 1 || button == 2) {
                           for (ModuleButton other : this.allModuleButtons) {
                              if (other != mb) {
                                 other.isBinding = false;
                              }
                           }
                        }

                        if (mb.mouseClicked(adjMouseX, adjMouseY, button)) {
                           return true;
                        }
                     }
                  }
               }

               return super.mouseClicked(new MouseButtonEvent(mouseX, mouseY, new MouseButtonInfo(button + 1, 0)), false);
            }
         }
      }
   }

   public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
      float sm = GuiUtils.getScaleModifier(this.minecraft) * GuiScaleSettings.getScale();
      float adjMouseX = (float)(mouseX / sm);
      float adjMouseY = (float)(mouseY / sm);
      float winX = (this.width / sm - 580.0F) / 2.0F;
      float winY = (this.height / sm - 380.0F) / 2.0F;
      if (SubSettingsPopupState.mouseDragged(adjMouseX, adjMouseY, button, winX, winY, 580.0F, 380.0F)) {
         return true;
      }

      if (this.colorPickerOpen && this.themePicker.isDragging()) {
         this.themePicker.update(adjMouseX, adjMouseY);
         return true;
      }

      if (this.soundSettingsOpen && SoundSettingsPopup.isDragging()) {
         float sndW = 138.0F;
         float sndX = this.getSoundSettingsX(winX);
         SoundSettingsPopup.mouseDragged(adjMouseX, sndX, sndW);
         return true;
      }

      if (!this.isAnyModalOpen()) {
         for (ModuleButton mb : this.allModuleButtons) {
            if (mb.isVisible && mb.mouseDragged(adjMouseX, adjMouseY, button, deltaX, deltaY)) {
               return true;
            }
         }
      }

      return super.mouseDragged(new MouseButtonEvent(mouseX, mouseY, new MouseButtonInfo(button + 1, 0)), deltaX, deltaY);
   }

   public boolean mouseReleased(double mouseX, double mouseY, int button) {
      float sm = GuiUtils.getScaleModifier(this.minecraft) * GuiScaleSettings.getScale();
      float adjX = (float)(mouseX / sm);
      float adjY = (float)(mouseY / sm);
      this.themePicker.mouseReleased();
      SoundSettingsPopup.mouseReleased();
      SubSettingsPopupState.mouseReleased(mouseX, mouseY, button);

      for (ModuleButton mb : this.allModuleButtons) {
         mb.mouseReleased();
      }

      return super.mouseReleased(new MouseButtonEvent(mouseX, mouseY, new MouseButtonInfo(button + 1, 0)));
   }

   @Override
   public boolean mouseScrolled(double mouseX, double mouseY, double hAm, double vAm) {
      float sm = GuiUtils.getScaleModifier(this.minecraft) * GuiScaleSettings.getScale();
      float adjX = (float)(mouseX / sm);
      float adjY = (float)(mouseY / sm);
      float winX = (this.width / sm - 580.0F) / 2.0F;
      float winY = (this.height / sm - 380.0F) / 2.0F;
      if (ModePopupState.mouseScrolled(adjX, adjY, vAm, winX, winY, 580.0F, 380.0F)) {
         return true;
      } else if (MultiSelectPopupState.mouseScrolled(adjX, adjY, vAm, winX, winY, 580.0F, 380.0F)) {
         return true;
      } else if (SubSettingsPopupState.mouseScrolled(adjX, adjY, vAm, winX, winY, 580.0F, 380.0F)) {
         return true;
      } else if (this.isAnyModalOpen()) {
         return true;
      } else if (GuiUtils.isMouseOver(adjX, adjY, winX + 138.0F, winY + 36.0F, 442.0F, 344.0F)) {
         float target = this.targetScrollOffsets.getOrDefault(this.activeTab, 0.0F);
         this.targetScrollOffsets.put(this.activeTab, target + (float)vAm * 32.0F);
         return true;
      } else {
         return super.mouseScrolled(mouseX, mouseY, hAm, vAm);
      }
   }

   public boolean charTyped(char chr, int mod) {
      if (this.isSearching) {
         if (this.searchText.length() < 30 && chr >= ' ' && chr != 127) {
            this.searchText = this.searchText + chr;
            this.lastTypingTime = System.currentTimeMillis();
         }

         return true;
      } else if (this.isTypingNewConfig) {
         if (this.newConfigInput.length() < 24 && chr >= ' ' && chr != 127) {
            this.newConfigInput = this.newConfigInput + chr;
         }

         return true;
      } else {
         return super.charTyped(new CharacterEvent(chr));
      }
   }

   public boolean keyPressed(int key, int scan, int mod) {
      if (MultiSelectPopupState.isOpen() && key == 256) {
         MultiSelectPopupState.close();
         return true;
      }

      if (SubSettingsPopupState.isOpen() && key == 256) {
         SubSettingsPopupState.close();
         return true;
      }

      if (ModePopupState.isOpen() && key == 256) {
         ModePopupState.close();
         return true;
      }

      if ((this.settingsModalOpen || this.colorPickerOpen || this.soundSettingsOpen || this.scaleModalOpen) && key == 256) {
         this.settingsModalOpen = false;
         this.colorPickerOpen = false;
         this.soundSettingsOpen = false;
         this.scaleModalOpen = false;
         return true;
      }

      for (ModuleButton mb : this.allModuleButtons) {
         if (mb.isBinding) {
            if (key != 256 && key != 261) {
               mb.module.setKeyCode(key);
            } else {
               mb.module.setKeyCode(-1);
            }

            mb.isBinding = false;
            return true;
         }
      }

      for (ModuleButton mb : this.allModuleButtons) {
         mb.keyPressed(key);
      }

      if (this.isSearching) {
         this.lastTypingTime = System.currentTimeMillis();
         if (key == 259 && !this.searchText.isEmpty()) {
            this.searchText = this.searchText.substring(0, this.searchText.length() - 1);
            return true;
         }

         if (key != 256 && key != 257) {
            return true;
         }

         this.isSearching = false;
         return true;
      } else if (this.isTypingNewConfig) {
         if (key == 259 && !this.newConfigInput.isEmpty()) {
            this.newConfigInput = this.newConfigInput.substring(0, this.newConfigInput.length() - 1);
            return true;
         }

         if (key != 256 && key != 257) {
            return true;
         }

         this.isTypingNewConfig = false;
         return true;
      } else {
         boolean isCloseKey = key == 256 || key == 260 || key == 344 || key == AstolfoclientClient.clickGuiKeyCode;
         if (!isCloseKey) {
            return super.keyPressed(new KeyEvent(xyz.angames.astolfoclient.client.compat.KeyMap.glfwToSdl(key), scan, 0));
         }

         if (key == 256) {
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
            this.onClose();
         }

         return true;
      }
   }

   private void renderHandLogic(GuiGraphicsExtractor ctx, float delta) {
      // The hand preview used GameRenderer#renderHand, which no longer exists in the deferred 26.x renderer.
   }

   @Environment(EnvType.CLIENT)
   public enum NavTab {
      RENDER(Module.Category.RENDER, "F", "Render", true),
      MISC(Module.Category.MISC, "G", "Miscellaneous", true),
      CONFIGS(null, "E", "Configs", false);

      public final Module.Category category;
      public final String icon;
      public final String title;
      public final boolean isFunction;

      NavTab(Module.Category category, String icon, String title, boolean isFunction) {
         this.category = category;
         this.icon = icon;
         this.title = title;
         this.isFunction = isFunction;
      }
   }

   @Environment(EnvType.CLIENT)
   public enum RenderSubcategory {
      ALL("All"),
      PVP("PVP"),
      WORLD("World"),
      PLAYER("Player"),
      INTERFACE("Interface");

      public final String title;

      RenderSubcategory(String title) {
         this.title = title;
      }
   }
}
