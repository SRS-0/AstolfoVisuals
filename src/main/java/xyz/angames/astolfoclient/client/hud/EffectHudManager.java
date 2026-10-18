package xyz.angames.astolfoclient.client.hud;

import xyz.angames.astolfoclient.client.compat.GuiCompat;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import dev.sxmurxy.mre.builders.Builder;
import dev.sxmurxy.mre.builders.states.QuadColorState;
import dev.sxmurxy.mre.builders.states.QuadRadiusState;
import dev.sxmurxy.mre.builders.states.SizeState;
import dev.sxmurxy.mre.msdf.MsdfFont;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.Holder;
import org.joml.Matrix4f;
import xyz.angames.astolfoclient.client.compat.GLFW;
import xyz.angames.astolfoclient.client.AstolfoclientClient;
import xyz.angames.astolfoclient.client.config.ThemeManager;
import xyz.angames.astolfoclient.client.gui.HudEditorScreen;
import xyz.angames.astolfoclient.client.gui.clickgui.GuiUtils;
import xyz.angames.astolfoclient.client.module.modules.render.InterfaceModule;

@Environment(EnvType.CLIENT)
public class EffectHudManager {
   public float x = 10.0F;
   public float y = 100.0F;
   public EffectHudManager.Position position = EffectHudManager.Position.LEFT;
   public boolean panelOpen = false;
   private boolean submenuOpen = false;
   private float mainPanelAnim = 0.0F;
   private float subPanelAnim = 0.0F;
   private boolean wasMouseDown = false;
   private final Map<MobEffect, EffectHudManager.PotionCardState> cardStateMap = new LinkedHashMap<>();
   private float totalHeight = 0.0F;
   private float lastFrameTotalMaxWidth = 90.0F;
   private float animatedX = 10.0F;
   private float masterAlpha = 0.0F;
   private long lastUpdateTimeNs = -1L;
   private static final Supplier<MsdfFont> BOLD_FONT = Suppliers.memoize(() -> MsdfFont.builder().atlas("bold").data("bold").build());
   private static final Supplier<MsdfFont> SEMIBOLD_FONT = Suppliers.memoize(() -> MsdfFont.builder().atlas("semibold").data("semibold").build());
   private static final Supplier<MsdfFont> MEDIUM_FONT = Suppliers.memoize(() -> MsdfFont.builder().atlas("medium").data("medium").build());
   private static final Supplier<MsdfFont> ICON_FONT = Suppliers.memoize(() -> MsdfFont.builder().atlas("icon").data("icon").build());

   public void render(GuiGraphicsExtractor context) {
      this.render(context, Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(false));
   }

   public void render(GuiGraphicsExtractor context, float tickDelta) {
      Minecraft client = Minecraft.getInstance();
      if (client.player != null) {
         Collection<MobEffectInstance> effects = client.player.getActiveEffects();
         boolean isEditing = client.gui.screen() instanceof HudEditorScreen;
         InterfaceModule interfaceMod = (InterfaceModule)(
            AstolfoclientClient.moduleManager != null ? AstolfoclientClient.moduleManager.getModuleByName("Interface") : null
         );
         boolean isSettingEnabled = interfaceMod == null || interfaceMod.effectHud.get();
         long nowNs = System.nanoTime();
         if (this.lastUpdateTimeNs == -1L) {
            this.lastUpdateTimeNs = nowNs;
         }

         long elapsedNs = nowNs - this.lastUpdateTimeNs;
         this.lastUpdateTimeNs = nowNs;
         double deltaSeconds = elapsedNs / 1.0E9;
         if (deltaSeconds > 0.1) {
            deltaSeconds = 0.1;
         }

         long themeTime = (long)(nowNs / 1000000.0);
         Color themeColor = new Color(ThemeManager.getThemedColor(themeTime / 10L));
         List<MobEffectInstance> list = isEditing ? this.getPlaceholders() : new ArrayList<>(effects);
         Set<MobEffect> currentActiveEffects = new HashSet<>();
         if (isSettingEnabled && (interfaceMod == null || interfaceMod.isEnabled() || isEditing)) {
            for (MobEffectInstance inst : list) {
               MobEffect effect = (MobEffect)inst.getEffect().value();
               currentActiveEffects.add(effect);
               EffectHudManager.PotionCardState state = this.cardStateMap.computeIfAbsent(effect, k -> new EffectHudManager.PotionCardState());
               state.cachedInstance = inst;
            }
         }

         Iterator<Entry<MobEffect, EffectHudManager.PotionCardState>> iterator = this.cardStateMap.entrySet().iterator();

         while (iterator.hasNext()) {
            Entry<MobEffect, EffectHudManager.PotionCardState> entry = iterator.next();
            MobEffect effect = entry.getKey();
            EffectHudManager.PotionCardState state = entry.getValue();
            boolean isActive = currentActiveEffects.contains(effect);
            float targetAnim = isActive ? 1.0F : 0.0F;
            float speed = 14.0F;
            state.animProgress = state.animProgress + (targetAnim - state.animProgress) * (float)(1.0 - Math.exp(-speed * deltaSeconds));
            float targetHeightScale = isActive ? 1.0F : (state.animProgress < 0.35F ? 0.0F : 1.0F);
            state.heightScale = state.heightScale + (targetHeightScale - state.heightScale) * (float)(1.0 - Math.exp(-speed * deltaSeconds));
            if (!isActive && state.animProgress < 0.005F && state.heightScale < 0.005F) {
               iterator.remove();
            }
         }

         boolean shouldShow = !this.cardStateMap.isEmpty() && isSettingEnabled && (interfaceMod == null || interfaceMod.isEnabled() || isEditing);
         float targetMasterAlpha = shouldShow ? 1.0F : 0.0F;
         this.masterAlpha = this.masterAlpha + (targetMasterAlpha - this.masterAlpha) * (float)(1.0 - Math.exp(-12.0 * deltaSeconds));
         if (this.masterAlpha < 0.005F && !shouldShow) {
            this.panelOpen = false;
            this.submenuOpen = false;
            this.mainPanelAnim = 0.0F;
            this.subPanelAnim = 0.0F;
         } else {
            float cardHeight = 26.0F;
            float radius = 6.0F;
            float iconSize = 14.0F;
            float nameSize = 8.0F;
            float infoSize = 7.0F;
            float paddingX = 8.0F;
            float iconGap = 6.5F;
            float gap = 4.0F;
            MsdfFont bold = (MsdfFont)BOLD_FONT.get();
            MsdfFont semibold = (MsdfFont)SEMIBOLD_FONT.get();
            MsdfFont medium = (MsdfFont)MEDIUM_FONT.get();
            MsdfFont iconFont = (MsdfFont)ICON_FONT.get();
            float scaleModifier = this.getScaleModifier();
            float scaledWidth = client.getWindow().getGuiScaledWidth() / scaleModifier;
            float scaledHeight = client.getWindow().getGuiScaledHeight() / scaleModifier;
            float computedTotalHeight = 0.0F;

            for (EffectHudManager.PotionCardState state : this.cardStateMap.values()) {
               computedTotalHeight += (cardHeight + gap) * state.heightScale;
            }

            if (computedTotalHeight > gap) {
               computedTotalHeight -= gap;
            }

            this.y = (scaledHeight - computedTotalHeight) / 2.0F;
            float targetBaseX = this.position == EffectHudManager.Position.RIGHT ? scaledWidth - this.lastFrameTotalMaxWidth - 10.0F : 10.0F;
            this.animatedX = this.animatedX + (targetBaseX - this.animatedX) * (float)(1.0 - Math.exp(-14.0 * deltaSeconds));
            this.x = this.animatedX;
            context.pose().pushMatrix();
            context.pose().scale(scaleModifier, scaleModifier);
            Matrix4f matrix = GuiCompat.matrix4f(context);
            float currentY = this.y;
            float currentFrameMaxWidth = 0.0F;

            for (Entry<MobEffect, EffectHudManager.PotionCardState> entry : this.cardStateMap.entrySet()) {
               MobEffect effect = entry.getKey();
               EffectHudManager.PotionCardState state = entry.getValue();
               MobEffectInstance instance = state.cachedInstance;
               if (instance != null) {
                  float cardProgress = state.animProgress;
                  float effectiveAlpha = cardProgress * this.masterAlpha;
                  if (!(effectiveAlpha <= 0.002F)) {
                     String name = Component.translatable(effect.getDescriptionId()).getString();
                     int level = instance.getAmplifier() + 1;
                     boolean showLevel = level > 1;
                     String levelNum = String.valueOf(level);
                     String duration = this.formatDuration(instance);
                     state.durationAnimator.update(duration);
                     float nameW = bold != null ? bold.getWidth(name, nameSize) : 35.0F;
                     float levelW = showLevel ? (bold != null ? bold.getWidth(levelNum, nameSize) : 8.0F) : 0.0F;
                     float spaceW = showLevel ? (bold != null ? bold.getWidth(" ", nameSize) : 3.0F) : 0.0F;
                     float durationW = medium != null ? medium.getWidth(duration, infoSize) : 20.0F;
                     float topLineW = nameW + spaceW + levelW;
                     float bottomLineW = durationW;
                     float maxTextW = Math.max(topLineW, bottomLineW);
                     float targetWidth = paddingX + iconSize + iconGap + maxTextW + paddingX;
                     state.width = state.width + (targetWidth - state.width) * (float)(1.0 - Math.exp(-14.0 * deltaSeconds));
                     float currentAnimatedWidth = state.width;
                     if (currentAnimatedWidth > currentFrameMaxWidth) {
                        currentFrameMaxWidth = currentAnimatedWidth;
                     }

                     float cardX = this.position == EffectHudManager.Position.RIGHT
                        ? this.animatedX + (this.lastFrameTotalMaxWidth - currentAnimatedWidth)
                        : this.animatedX;
                     float cardEase = 1.0F - (float)Math.pow(1.0F - cardProgress, 3.0);
                     float cardScale = 0.88F + 0.12F * cardEase;
                     context.pose().pushMatrix();
                     if (cardScale < 0.999F) {
                        float cx = cardX + (this.position == EffectHudManager.Position.RIGHT ? currentAnimatedWidth : 0.0F);
                        float cy = currentY + cardHeight / 2.0F;
                        context.pose().translate(cx, cy);
                        context.pose().scale(cardScale, cardScale);
                        context.pose().translate(-cx, -cy);
                     }

                     Matrix4f cardMat = GuiCompat.matrix4f(context);
                     this.renderShadow(cardMat, cardX, currentY, currentAnimatedWidth, cardHeight, radius, effectiveAlpha);
                     Builder.rectangle()
                        .size(new SizeState(currentAnimatedWidth, cardHeight))
                        .radius(new QuadRadiusState(radius))
                        .color(new QuadColorState(new Color(0, 0, 0, (int)(255.0F * effectiveAlpha))))
                        .build()
                        .render(cardMat, cardX, currentY);
                     float centerY = currentY + cardHeight / 2.0F;
                     float currentX = cardX + paddingX;
                     float iconCX = currentX + iconSize / 2.0F;
                     this.drawIconGlowShadow(cardMat, iconCX, centerY, iconSize / 2.0F, themeColor, 0.12F * effectiveAlpha);
                     Identifier icon = this.getEffectIconIdentifier(instance.getEffect());
                     Builder.texture()
                        .size(new SizeState(iconSize, iconSize))
                        .radius(new QuadRadiusState(3.0F))
                        .texture(0.0F, 0.0F, 1.0F, 1.0F, client.getTextureManager().getTexture(icon))
                        .color(new QuadColorState(new Color(255, 255, 255, (int)(255.0F * effectiveAlpha))))
                        .build()
                        .render(cardMat, currentX, centerY - iconSize / 2.0F);
                     currentX += iconSize + iconGap;
                     if (bold != null) {
                        Color whiteText = GuiUtils.withAlpha(Color.WHITE, effectiveAlpha);
                        Builder.text().font(bold).text(name).color(whiteText).size(nameSize).build().render(cardMat, currentX, currentY + 4.5F);
                        if (showLevel) {
                           Builder.text()
                              .font(bold)
                              .text(levelNum)
                              .color(GuiUtils.withAlpha(themeColor, effectiveAlpha))
                              .size(nameSize)
                              .build()
                              .render(cardMat, currentX + nameW + spaceW, currentY + 4.5F);
                        }
                     }

                     if (medium != null) {
                        Color grayText = GuiUtils.withAlpha(new Color(160, 160, 165), effectiveAlpha);
                        state.durationAnimator.render(cardMat, currentX, currentY + 14.5F, grayText, medium, infoSize, effectiveAlpha);
                     }

                     context.pose().popMatrix();
                     currentY += (cardHeight + gap) * state.heightScale;
                  }
               }
            }

            this.totalHeight = currentY - this.y;
            this.lastFrameTotalMaxWidth = currentFrameMaxWidth;
            if (isEditing) {
               double mx = this.getScaledMouseX();
               double my = this.getScaledMouseY();
               boolean isMouseDown = GLFW.glfwGetMouseButton(client.getWindow().handle(), 0) == 1;
               this.drawConnectedContextPanel(context, scaledWidth, scaledHeight, themeColor, mx, my, isMouseDown, this.wasMouseDown, (float)deltaSeconds);
               this.wasMouseDown = isMouseDown;
            } else {
               this.panelOpen = false;
               this.submenuOpen = false;
               this.mainPanelAnim = 0.0F;
               this.subPanelAnim = 0.0F;
               this.wasMouseDown = false;
            }

            context.pose().popMatrix();
         }
      }
   }

   private void drawConnectedContextPanel(
      GuiGraphicsExtractor context,
      float screenW,
      float screenH,
      Color themeColor,
      double mouseX,
      double mouseY,
      boolean isMouseDown,
      boolean wasMouseDown,
      float deltaTime
   ) {
      float targetMain = this.panelOpen ? 1.0F : 0.0F;
      this.mainPanelAnim = this.mainPanelAnim + (targetMain - this.mainPanelAnim) * (float)(1.0 - Math.exp(-14.0 * deltaTime));
      if (!this.panelOpen && this.mainPanelAnim < 0.01F) {
         this.mainPanelAnim = 0.0F;
         this.subPanelAnim = 0.0F;
      } else {
         float targetSub = this.submenuOpen && this.panelOpen ? 1.0F : 0.0F;
         this.subPanelAnim = this.subPanelAnim + (targetSub - this.subPanelAnim) * (float)(1.0 - Math.exp(-14.0 * deltaTime));
         float mainW = 110.0F;
         float mainH = 48.0F;
         float subW = 88.0F;
         float subH = 38.0F;
         float mY = this.y;
         boolean opensToRight = this.position == EffectHudManager.Position.LEFT;
         float mX;
         if (opensToRight) {
            mX = this.animatedX + this.lastFrameTotalMaxWidth + 8.0F;
         } else {
            float totalW = mainW + (this.subPanelAnim > 0.01F ? subW + 5.0F : 0.0F);
            mX = this.animatedX - totalW - 8.0F;
         }

         if (mX + mainW + (this.subPanelAnim > 0.01F ? subW + 5.0F : 0.0F) > screenW - 6.0F) {
            mX = screenW - mainW - (this.subPanelAnim > 0.01F ? subW + 5.0F : 0.0F) - 6.0F;
         }

         if (mX < 6.0F) {
            mX = 6.0F;
         }

         if (mY + mainH > screenH - 6.0F) {
            mY = screenH - mainH - 6.0F;
         }

         if (mY < 6.0F) {
            mY = 6.0F;
         }

         MsdfFont boldFont = (MsdfFont)BOLD_FONT.get();
         MsdfFont mediumFont = (MsdfFont)MEDIUM_FONT.get();
         MsdfFont spFont = (MsdfFont)LogoRenderer.SP_FONT.get();
         float mainEase = 1.0F - (float)Math.pow(1.0F - this.mainPanelAnim, 3.0);
         float mainScale = 0.88F + 0.12F * mainEase;
         context.pose().pushMatrix();
         if (mainScale < 0.999F) {
            float cx = mX + mainW / 2.0F;
            float cy = mY + mainH / 2.0F;
            context.pose().translate(cx, cy);
            context.pose().scale(mainScale, mainScale);
            context.pose().translate(-cx, -cy);
         }

         Matrix4f matrix = GuiCompat.matrix4f(context);

         for (int i = 6; i >= 0; i--) {
            float progress = i / 6.0F;
            float spread = progress * 4.0F;
            int alpha = (int)(60.0F * (1.0F - progress) * (1.0F - progress) * mainEase);
            if (alpha > 0) {
               Builder.rectangle()
                  .size(new SizeState(mainW + spread * 2.0F, mainH + spread * 2.0F))
                  .radius(new QuadRadiusState(8.0F + spread))
                  .color(new QuadColorState(new Color(0, 0, 0, alpha)))
                  .build()
                  .render(matrix, mX - spread, mY - spread);
            }
         }

         Color bgMain = new Color(0, 0, 0, (int)(255.0F * mainEase));
         Builder.rectangle()
            .size(new SizeState(mainW, mainH))
            .radius(new QuadRadiusState(8.0F))
            .color(new QuadColorState(bgMain))
            .build()
            .render(matrix, mX, mY);
         if (boldFont != null) {
            Builder.text()
               .font(boldFont)
               .text("Potions")
               .size(7.5F)
               .color(new Color(255, 255, 255, (int)(255.0F * mainEase)))
               .build()
               .render(matrix, mX + 8.0F, mY + 6.5F);
         }

         float closeX = mX + mainW - 14.0F;
         float closeY = mY + 6.5F;
         boolean closeHover = mouseX >= closeX - 2.0F && mouseX <= closeX + 10.0F && mouseY >= closeY - 2.0F && mouseY <= closeY + 10.0F;
         Color closeColor = closeHover ? new Color(255, 80, 80, (int)(255.0F * mainEase)) : new Color(140, 140, 150, (int)(255.0F * mainEase));
         if (spFont != null) {
            Builder.text().font(spFont).text("A").size(7.0F).color(closeColor).build().render(matrix, closeX, closeY);
         }

         if (closeHover && isMouseDown && !wasMouseDown) {
            this.panelOpen = false;
            this.submenuOpen = false;
            context.pose().popMatrix();
         } else {
            float c1X = mX + 4.0F;
            float c1Y = mY + 20.0F;
            float c1W = mainW - 8.0F;
            float c1H = 22.0F;
            Builder.rectangle()
               .size(new SizeState(c1W, c1H))
               .radius(new QuadRadiusState(6.0F))
               .color(new QuadColorState(new Color(10, 10, 15, (int)(220.0F * mainEase))))
               .build()
               .render(matrix, c1X, c1Y);
            float r1Y = c1Y + 3.0F;
            float r1H = 16.0F;
            boolean r1Hov = mouseX >= c1X && mouseX <= c1X + c1W && mouseY >= r1Y && mouseY <= r1Y + r1H;
            boolean r1Active = this.submenuOpen;
            if (r1Hov) {
               Builder.rectangle()
                  .size(new SizeState(c1W - 4.0F, r1H))
                  .radius(new QuadRadiusState(4.0F))
                  .color(new QuadColorState(new Color(22, 22, 32, (int)(180.0F * mainEase))))
                  .build()
                  .render(matrix, c1X + 2.0F, r1Y);
            }

            if (spFont != null) {
               Builder.text()
                  .font(spFont)
                  .text("C")
                  .size(7.5F)
                  .color(new Color(170, 170, 190, (int)(255.0F * mainEase)))
                  .build()
                  .render(matrix, c1X + 5.0F, r1Y + 4.0F);
            }

            if (mediumFont != null) {
               Builder.text()
                  .font(mediumFont)
                  .text("Position")
                  .size(6.5F)
                  .color(new Color(230, 230, 245, (int)(255.0F * mainEase)))
                  .build()
                  .render(matrix, c1X + 16.0F, r1Y + 5.5F);
               String currentPosName = (this.position == EffectHudManager.Position.LEFT ? "Left" : "Right") + " >";
               float posStrW = mediumFont.getWidth(currentPosName, 6.0F);
               Color posCol = r1Active ? GuiUtils.withAlpha(themeColor, mainEase) : new Color(140, 140, 160, (int)(255.0F * mainEase));
               Builder.text().font(mediumFont).text(currentPosName).size(6.0F).color(posCol).build().render(matrix, c1X + c1W - posStrW - 5.0F, r1Y + 5.5F);
            }

            if (r1Hov && isMouseDown && !wasMouseDown) {
               this.submenuOpen = !this.submenuOpen;
            }

            context.pose().popMatrix();
            if (this.subPanelAnim > 0.005F) {
               float subEase = 1.0F - (float)Math.pow(1.0F - this.subPanelAnim, 3.0);
               float subScale = 0.88F + 0.12F * subEase;
               float sX = mX + mainW + 5.0F;
               if (sX + subW > screenW - 6.0F) {
                  sX = mX - subW - 5.0F;
               }

               float sY = mY;
               context.pose().pushMatrix();
               if (subScale < 0.999F) {
                  float scx = sX + subW / 2.0F;
                  float scy = sY + subH / 2.0F;
                  context.pose().translate(scx, scy);
                  context.pose().scale(subScale, subScale);
                  context.pose().translate(-scx, -scy);
               }

               matrix = GuiCompat.matrix4f(context);

               for (int i = 6; i >= 0; i--) {
                  float progress = i / 6.0F;
                  float spread = progress * 4.0F;
                  int alpha = (int)(60.0F * (1.0F - progress) * (1.0F - progress) * subEase);
                  if (alpha > 0) {
                     Builder.rectangle()
                        .size(new SizeState(subW + spread * 2.0F, subH + spread * 2.0F))
                        .radius(new QuadRadiusState(8.0F + spread))
                        .color(new QuadColorState(new Color(0, 0, 0, alpha)))
                        .build()
                        .render(matrix, sX - spread, sY - spread);
                  }
               }

               Color bgSub = new Color(0, 0, 0, (int)(255.0F * subEase));
               Builder.rectangle()
                  .size(new SizeState(subW, subH))
                  .radius(new QuadRadiusState(8.0F))
                  .color(new QuadColorState(bgSub))
                  .build()
                  .render(matrix, sX, sY);
               EffectHudManager.Position[] posOptions = new EffectHudManager.Position[]{EffectHudManager.Position.LEFT, EffectHudManager.Position.RIGHT};
               String[] posLabels = new String[]{"Left", "Right"};
               float posRowH = 15.0F;
               float listStartY = sY + 4.0F;

               for (int i = 0; i < posOptions.length; i++) {
                  float rowY = listStartY + i * posRowH;
                  boolean isSel = this.position == posOptions[i];
                  boolean rHov = mouseX >= sX + 3.0F && mouseX <= sX + subW - 3.0F && mouseY >= rowY && mouseY <= rowY + 14.0F;
                  if (rHov) {
                     Builder.rectangle()
                        .size(new SizeState(subW - 6.0F, 14.0F))
                        .radius(new QuadRadiusState(4.0F))
                        .color(new QuadColorState(new Color(25, 25, 36, (int)(220.0F * subEase))))
                        .build()
                        .render(matrix, sX + 3.0F, rowY);
                  }

                  if (isSel && spFont != null) {
                     Builder.text()
                        .font(spFont)
                        .text("D")
                        .size(6.5F)
                        .color(new Color(255, 255, 255, (int)(255.0F * subEase)))
                        .build()
                        .render(matrix, sX + 6.0F, rowY + 3.5F);
                  }

                  if (mediumFont != null) {
                     Color textCol = isSel
                        ? new Color(240, 240, 255, (int)(255.0F * subEase))
                        : (rHov ? new Color(180, 180, 200, (int)(255.0F * subEase)) : new Color(110, 110, 130, (int)(255.0F * subEase)));
                     Builder.text()
                        .font(mediumFont)
                        .text(posLabels[i])
                        .size(6.5F)
                        .color(textCol)
                        .build()
                        .render(matrix, sX + (isSel ? 18.0F : 8.0F), rowY + 4.5F);
                  }

                  if (rHov && isMouseDown && !wasMouseDown) {
                     this.position = posOptions[i];
                  }
               }

               context.pose().popMatrix();
            }
         }
      }
   }

   private Identifier getEffectIconIdentifier(Holder<MobEffect> effect) {
      return effect.unwrapKey()
         .map(key -> Identifier.fromNamespaceAndPath("minecraft", "textures/mob_effect/" + key.identifier().getPath() + ".png"))
         .orElse(Identifier.fromNamespaceAndPath("minecraft", "textures/missing.png"));
   }

   private String formatDuration(MobEffectInstance effect) {
      if (effect.isInfiniteDuration()) {
         return "Infinite";
      }

      int totalSeconds = effect.getDuration() / 20;
      return String.format("%d:%02d", totalSeconds / 60, totalSeconds % 60);
   }

   private List<MobEffectInstance> getPlaceholders() {
      return List.of(
         new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 16500, 0),
         new MobEffectInstance(MobEffects.STRENGTH, 740, 1),
         new MobEffectInstance(MobEffects.SPEED, 1640, 1),
         new MobEffectInstance(MobEffects.ABSORPTION, 2260, 3)
      );
   }

   private void renderShadow(Matrix4f matrix, float x, float y, float w, float h, float radius, float masterAlpha) {
      int layers = 12;
      float maxSpread = 8.0F;

      for (int i = layers - 1; i >= 0; i--) {
         float progress = (float)i / layers;
         float spread = progress * maxSpread;
         float alphaFactor = (1.0F - progress) * (1.0F - progress);
         int alpha = (int)(102.0F * alphaFactor * masterAlpha);
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

   private void drawIconGlowShadow(Matrix4f matrix, float cx, float cy, float radius, Color color, float masterAlpha) {
      if (!(masterAlpha <= 0.001F)) {
         int steps = 10;
         float maxSpread = 8.0F;
         int cr = color.getRed();
         int cg = color.getGreen();
         int cb = color.getBlue();

         for (int i = steps - 1; i >= 0; i--) {
            float progress = (float)i / steps;
            float spread = progress * maxSpread;
            float alphaFactor = (1.0F - progress) * (1.0F - progress);
            int alpha = (int)(255.0F * alphaFactor * masterAlpha);
            if (alpha > 0) {
               float curR = radius + spread;
               Builder.rectangle()
                  .size(new SizeState(curR * 2.0F, curR * 2.0F))
                  .radius(new QuadRadiusState(curR))
                  .color(new QuadColorState(new Color(cr, cg, cb, alpha)))
                  .build()
                  .render(matrix, cx - curR, cy - curR);
            }
         }
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
      return mc.mouseHandler.xpos() * mc.getWindow().getGuiScaledWidth() / mc.getWindow().getScreenWidth() / scaleModifier;
   }

   private double getScaledMouseY() {
      Minecraft mc = Minecraft.getInstance();
      double scaleModifier = this.getScaleModifier();
      return mc.mouseHandler.ypos() * mc.getWindow().getGuiScaledHeight() / mc.getWindow().getScreenHeight() / scaleModifier;
   }

   public boolean onMouseClicked(double mouseX, double mouseY, int button) {
      Minecraft mc = Minecraft.getInstance();
      boolean isEditing = mc.gui.screen() instanceof HudEditorScreen;
      if (!isEditing) {
         return false;
      }

      float scaleModifier = this.getScaleModifier();
      double mx = mouseX / scaleModifier;
      double my = mouseY / scaleModifier;
      float screenW = mc.getWindow().getGuiScaledWidth() / scaleModifier;
      float currentCardX = this.position == EffectHudManager.Position.RIGHT ? screenW - this.lastFrameTotalMaxWidth - 10.0F : 10.0F;
      if (this.panelOpen) {
         float mainW = 110.0F;
         float mainH = 48.0F;
         float subW = 88.0F;
         float subH = 38.0F;
         float mX = this.position == EffectHudManager.Position.LEFT
            ? this.animatedX + this.lastFrameTotalMaxWidth + 8.0F
            : this.animatedX - mainW - (this.submenuOpen ? subW + 5.0F : 0.0F) - 8.0F;
         float mY = this.y;
         boolean inMain = mx >= mX && mx <= mX + mainW && my >= mY && my <= mY + mainH;
         float sX = mX + mainW + 5.0F;
         if (sX + subW > screenW - 6.0F) {
            sX = mX - subW - 5.0F;
         }

         boolean inSub = this.submenuOpen && mx >= sX && mx <= sX + subW && my >= mY && my <= mY + subH;
         if (inMain || inSub) {
            return true;
         }

         if (button == 0 || button == 1) {
            this.panelOpen = false;
            this.submenuOpen = false;
         }
      }

      boolean isOverEffectHud = mx >= currentCardX - 2.0F
         && mx <= currentCardX + this.lastFrameTotalMaxWidth + 2.0F
         && my >= this.y - 2.0F
         && my <= this.y + this.totalHeight + 2.0F;
      if (button == 1 && isOverEffectHud) {
         this.panelOpen = !this.panelOpen;
         if (this.panelOpen) {
            this.mainPanelAnim = 0.0F;
            this.subPanelAnim = 0.0F;
            this.submenuOpen = false;
         } else {
            this.submenuOpen = false;
         }

         return true;
      } else {
         return false;
      }
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

   @Environment(EnvType.CLIENT)
   public enum Position {
      LEFT,
      RIGHT;
   }

   @Environment(EnvType.CLIENT)
   private static class PotionCardState {
      float animProgress = 0.0F;
      float heightScale = 0.0F;
      float width = 80.0F;
      MobEffectInstance cachedInstance;
      EffectHudManager.TextAnimator durationAnimator = new EffectHudManager.TextAnimator();
   }

   @Environment(EnvType.CLIENT)
   private static class TextAnimator {
      private String currentStr = "";
      private final char[] oldChars = new char[32];
      private final long[] animStartTimes = new long[32];
      private static final long DURATION = 180L;

      public void update(String newStr) {
         if (newStr != null) {
            if (this.currentStr.isEmpty()) {
               this.currentStr = newStr;
            } else if (!newStr.equals(this.currentStr)) {
               long now = System.currentTimeMillis();
               int maxLen = Math.max(newStr.length(), this.currentStr.length());

               for (int i = 0; i < maxLen && i < 32; i++) {
                  char cOld = i < this.currentStr.length() ? this.currentStr.charAt(i) : '\u0000';
                  char cNew = i < newStr.length() ? newStr.charAt(i) : '\u0000';
                  if (cOld != cNew && cOld != 0 && cNew != 0) {
                     this.oldChars[i] = cOld;
                     this.animStartTimes[i] = now;
                  } else if (cOld == 0 || cNew == 0) {
                     this.oldChars[i] = 0;
                  }
               }

               this.currentStr = newStr;
            }
         }
      }

      public void render(Matrix4f matrix, float startX, float y, Color textColor, MsdfFont font, float fontSize, float masterAlpha) {
         float currX = startX;
         long now = System.currentTimeMillis();

         for (int i = 0; i < this.currentStr.length(); i++) {
            char cNew = this.currentStr.charAt(i);
            String strNew = String.valueOf(cNew);
            float charW = font.getWidth(strNew, fontSize);
            long elapsed = now - this.animStartTimes[i];
            if (this.oldChars[i] != 0 && elapsed >= 0L && elapsed < 180L) {
               float progress = (float)elapsed / 180.0F;
               float easeOut = 1.0F - (float)Math.pow(1.0F - progress, 3.0);
               char cOld = this.oldChars[i];
               String strOld = String.valueOf(cOld);
               int oldAlpha = Math.max(0, Math.min(255, (int)(255.0F * (1.0F - easeOut) * masterAlpha)));
               Color oldColor = new Color(textColor.getRed(), textColor.getGreen(), textColor.getBlue(), oldAlpha);
               float oldOffsetY = -3.5F * easeOut;
               Builder.text().font(font).text(strOld).size(fontSize).color(oldColor).build().render(matrix, currX, y + oldOffsetY);
               int newAlpha = Math.max(0, Math.min(255, (int)(255.0F * easeOut * masterAlpha)));
               Color newColor = new Color(textColor.getRed(), textColor.getGreen(), textColor.getBlue(), newAlpha);
               float newOffsetY = 3.5F * (1.0F - easeOut);
               Builder.text().font(font).text(strNew).size(fontSize).color(newColor).build().render(matrix, currX, y + newOffsetY);
            } else {
               this.oldChars[i] = 0;
               Builder.text().font(font).text(strNew).size(fontSize).color(textColor).build().render(matrix, currX, y);
            }

            currX += charW;
         }
      }
   }
}
