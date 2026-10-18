package xyz.angames.astolfoclient.client.hud;

import xyz.angames.astolfoclient.client.compat.GuiCompat;
import com.google.common.base.Supplier;
import dev.sxmurxy.mre.builders.Builder;
import dev.sxmurxy.mre.builders.states.QuadColorState;
import dev.sxmurxy.mre.builders.states.QuadRadiusState;
import dev.sxmurxy.mre.builders.states.SizeState;
import dev.sxmurxy.mre.msdf.MsdfFont;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import org.joml.Matrix4f;
import xyz.angames.astolfoclient.client.compat.GLFW;
import xyz.angames.astolfoclient.client.AstolfoclientClient;
import xyz.angames.astolfoclient.client.config.ThemeManager;
import xyz.angames.astolfoclient.client.gui.HudEditorScreen;
import xyz.angames.astolfoclient.client.gui.clickgui.GuiUtils;
import xyz.angames.astolfoclient.client.module.modules.render.InterfaceModule;

@Environment(EnvType.CLIENT)
public class ArmorHudManager {
   private static final net.minecraft.world.entity.EquipmentSlot[] ARMOR_SLOTS = {
      net.minecraft.world.entity.EquipmentSlot.FEET, net.minecraft.world.entity.EquipmentSlot.LEGS,
      net.minecraft.world.entity.EquipmentSlot.CHEST, net.minecraft.world.entity.EquipmentSlot.HEAD
   };

   private static final Supplier<MsdfFont> BOLD_FONT = LogoRenderer.BOLD_FONT;
   private static final Supplier<MsdfFont> MEDIUM_FONT = LogoRenderer.MEDIUM_FONT;
   private static final Supplier<MsdfFont> SP_FONT = LogoRenderer.SP_FONT;
   private static final Supplier<MsdfFont> WATERMARK_FONT = LogoRenderer.WATERMARK_FONT;
   public float x = 10.0F;
   public float y = 200.0F;
   private static final int SLOT_SIZE = 18;
   private static final int ITEM_GAP = 2;
   public static final float HORIZ_WIDTH = 78.0F;
   public static final float HORIZ_HEIGHT = 18.0F;
   public static final float VERT_WIDTH = 18.0F;
   public static final float VERT_HEIGHT = 78.0F;
   public static String layout = "VERTICAL";
   public static boolean warningGlow = true;
   private boolean dragging = false;
   private float dragOffsetX;
   private float dragOffsetY;
   private boolean panelOpen = false;
   private ArmorHudManager.SubmenuType activeSubmenu = ArmorHudManager.SubmenuType.NONE;
   private float mainPanelAnim = 0.0F;
   private float subPanelAnim = 0.0F;
   private float warningGlowSwitchAnim = 1.0F;
   private boolean wasMouseDown = false;
   private boolean wasRightMouseDown = false;
   private Object lastScreen = null;
   private long lastFrameTime = System.currentTimeMillis();
   private final Minecraft client = Minecraft.getInstance();

   public static boolean isVertical() {
      return layout != null && layout.equalsIgnoreCase("VERTICAL");
   }

   public float getWidth() {
      return isVertical() ? 18.0F : 78.0F;
   }

   public float getHeight() {
      return isVertical() ? 78.0F : 18.0F;
   }

   public void render(GuiGraphicsExtractor context) {
      InterfaceModule interfaceMod = getInterfaceModule();
      boolean isEditing = this.client.gui.screen() instanceof HudEditorScreen;
      boolean inScreen = this.client.gui.screen() != null;
      if (interfaceMod != null) {
         if (isEditing || interfaceMod.isEnabled() && interfaceMod.armorHud.get()) {
            if (this.client.player != null) {
               long now = System.currentTimeMillis();
               float deltaTime = (float)(now - this.lastFrameTime) / 1000.0F;
               this.lastFrameTime = now;
               if (deltaTime < 0.001F) {
                  deltaTime = 0.001F;
               }

               if (deltaTime > 0.1F) {
                  deltaTime = 0.1F;
               }

               boolean vertical = isVertical();
               float curW = vertical ? 18.0F : 78.0F;
               float curH = vertical ? 78.0F : 18.0F;
               double currentGuiScale = this.client.getWindow().getGuiScale();
               if (currentGuiScale <= 0.0) {
                  currentGuiScale = 2.0;
               }

               float scaleModifier = (float)(2.0 / currentGuiScale);
               context.pose().pushMatrix();
               context.pose().translate(this.x, this.y);
               context.pose().scale(scaleModifier, scaleModifier);
               context.pose().translate(-this.x, -this.y);
               List<ItemStack> armorList = new ArrayList<>();
               boolean hasArmor = false;

               for (int i = 0; i < 4; i++) {
                  ItemStack stack = (ItemStack)this.client.player.getItemBySlot(ARMOR_SLOTS[i]);
                  armorList.add(stack);
                  if (!stack.isEmpty()) {
                     hasArmor = true;
                  }
               }

               if (isEditing && !hasArmor) {
                  armorList.set(0, new ItemStack(Items.DIAMOND_BOOTS));
                  armorList.set(1, new ItemStack(Items.DIAMOND_LEGGINGS));
                  ItemStack damagedChest = new ItemStack(Items.DIAMOND_CHESTPLATE);
                  damagedChest.setDamageValue(damagedChest.getMaxDamage() - 10);
                  armorList.set(2, damagedChest);
                  armorList.set(3, new ItemStack(Items.DIAMOND_HELMET));
               }

               float startX = this.x;
               float startY = this.y;
               context.pose().pushMatrix();
               context.pose().translate(0.0F, 0.0F);

               for (int i = 3; i >= 0; i--) {
                  ItemStack stack = armorList.get(i);
                  if (!stack.isEmpty()) {
                     int index = 3 - i;
                     float drawX = vertical ? startX : startX + index * 20;
                     float drawY = vertical ? startY + index * 20 : startY;
                     boolean almostBroken = false;
                     if (warningGlow && stack.isDamageableItem() && stack.getMaxDamage() > 0) {
                        float durabilityPercent = (float)(stack.getMaxDamage() - stack.getDamageValue()) / stack.getMaxDamage();
                        if (durabilityPercent <= 0.15F) {
                           almostBroken = true;
                        }
                     }

                     context.pose().pushMatrix();
                     if (almostBroken) {
                        float pulseAlpha = (float)Math.abs(Math.sin(System.nanoTime() / 2.0E8));
                        float bounceOffset = pulseAlpha * -3.0F;
                        context.pose().translate(0.0F, bounceOffset);
                        Matrix4f glowMat = GuiCompat.matrix4f(context);

                        for (int j = 0; j < 4; j++) {
                           float expand = (j + 1) * 2.0F;
                           int alpha = (int)(255.0F * (0.3F - j * 0.07F) * (0.4F + 0.6F * pulseAlpha));
                           if (alpha > 0) {
                              Builder.rectangle()
                                 .size(new SizeState(16.0F + expand, 16.0F + expand))
                                 .radius(new QuadRadiusState(3.5F + expand / 2.0F))
                                 .color(new QuadColorState(new Color(255, 40, 40, alpha)))
                                 .build()
                                 .render(glowMat, drawX - expand / 2.0F, drawY - expand / 2.0F);
                           }
                        }
                     }

                     context.item(stack, (int)drawX, (int)drawY);
                     context.itemDecorations(this.client.font, stack, (int)drawX, (int)drawY);
                     context.pose().popMatrix();
                  }
               }

               context.pose().popMatrix();
               context.pose().popMatrix();
               Object activeScreen = this.client.gui.screen();
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
                     GLFW.glfwGetCursorPos(win, mx, my);
                     double screenWidth = this.client.getWindow().getWidth();
                     double screenHeight = this.client.getWindow().getHeight();
                     float guiWidth = this.client.getWindow().getGuiScaledWidth();
                     float guiHeight = this.client.getWindow().getGuiScaledHeight();
                     if (screenWidth > 0.0 && screenHeight > 0.0) {
                        mouseScaledX = mx[0] / screenWidth * guiWidth;
                        mouseScaledY = my[0] / screenHeight * guiHeight;
                     }

                     isMouseDown = GLFW.glfwGetMouseButton(win, 0) == 1;
                     isRightMouseDown = GLFW.glfwGetMouseButton(win, 1) == 1;
                  }

                  if (isRightMouseDown && !this.wasRightMouseDown && !(this.client.gui.screen() instanceof HudEditorScreen)) {
                     float visMinX = this.x;
                     float visMinY = this.y;
                     float visMaxX = this.x + curW * scaleModifier;
                     float visMaxY = this.y + curH * scaleModifier;
                     if (mouseScaledX >= visMinX - 4.0F && mouseScaledX <= visMaxX + 4.0F && mouseScaledY >= visMinY - 4.0F && mouseScaledY <= visMaxY + 4.0F) {
                        this.panelOpen = !this.panelOpen;
                        if (this.panelOpen) {
                           this.activeSubmenu = ArmorHudManager.SubmenuType.NONE;
                        }
                     }
                  }

                  Color themeColor = new Color(ThemeManager.getThemedColor(now / 10L));
                  this.drawAnimatedContextPanel(
                     context,
                     this.x,
                     this.y,
                     curW,
                     curH,
                     scaleModifier,
                     this.client.getWindow().getGuiScaledWidth(),
                     this.client.getWindow().getGuiScaledHeight(),
                     themeColor,
                     mouseScaledX,
                     mouseScaledY,
                     isMouseDown,
                     this.wasMouseDown,
                     deltaTime
                  );
                  this.wasMouseDown = isMouseDown;
                  this.wasRightMouseDown = isRightMouseDown;
               } else {
                  this.panelOpen = false;
                  this.activeSubmenu = ArmorHudManager.SubmenuType.NONE;
                  this.mainPanelAnim = 0.0F;
                  this.subPanelAnim = 0.0F;
               }
            }
         } else {
            this.panelOpen = false;
            this.activeSubmenu = ArmorHudManager.SubmenuType.NONE;
            this.mainPanelAnim = 0.0F;
            this.subPanelAnim = 0.0F;
         }
      }
   }

   private void drawAnimatedContextPanel(
      GuiGraphicsExtractor context,
      float aX,
      float aY,
      float aW,
      float aH,
      float scaleFactor,
      float guiWidth,
      float guiHeight,
      Color themeColor,
      double mouseX,
      double mouseY,
      boolean isMouseDown,
      boolean wasMouseDown,
      float deltaTime
   ) {
      float targetMain = this.panelOpen ? 1.0F : 0.0F;
      this.mainPanelAnim = GuiUtils.animate(this.mainPanelAnim, targetMain, 16.0F, deltaTime);
      if (!this.panelOpen && this.mainPanelAnim < 0.01F) {
         this.mainPanelAnim = 0.0F;
      } else {
         boolean hasSub = this.activeSubmenu != ArmorHudManager.SubmenuType.NONE;
         float targetSub = hasSub && this.panelOpen ? 1.0F : 0.0F;
         this.subPanelAnim = GuiUtils.animate(this.subPanelAnim, targetSub, 16.0F, deltaTime);
         float mainW = 110.0F;
         float mainH = 70.0F;
         float pX = aX;
         float pY = aY + aH * scaleFactor + 6.0F;
         float subW = 95.0F;
         String[] modes = new String[]{"VERTICAL", "HORIZONTAL"};
         String[] labels = new String[]{"Vertical", "Horizontal"};
         float subH = modes.length * 16.0F + 8.0F;
         if (pX + mainW + (hasSub ? subW + 5.0F : 0.0F) > guiWidth - 6.0F) {
            pX = guiWidth - mainW - (hasSub ? subW + 5.0F : 0.0F) - 6.0F;
         }

         if (pX < 6.0F) {
            pX = 6.0F;
         }

         float maxPanelH = hasSub ? Math.max(mainH, subH) : mainH;
         if (pY + maxPanelH > guiHeight - 6.0F) {
            pY = aY - maxPanelH - 6.0F;
         }

         if (pY < 6.0F) {
            pY = 6.0F;
         }

         MsdfFont boldFont = (MsdfFont)BOLD_FONT.get();
         MsdfFont mediumFont = (MsdfFont)MEDIUM_FONT.get();
         MsdfFont spFont = (MsdfFont)SP_FONT.get();
         MsdfFont watermarkFont = (MsdfFont)WATERMARK_FONT.get();
         float mainEase = 1.0F - (float)Math.pow(1.0F - this.mainPanelAnim, 3.0);
         float mainScale = 0.88F + 0.12F * mainEase;
         context.pose().pushMatrix();
         if (mainScale < 0.999F) {
            float cx = pX + mainW / 2.0F;
            float cy = pY + mainH / 2.0F;
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
                  .render(matrix, pX - spread, pY - spread);
            }
         }

         Color bgMain = new Color(0, 0, 0, (int)(255.0F * mainEase));
         Builder.rectangle()
            .size(new SizeState(mainW, mainH))
            .radius(new QuadRadiusState(8.0F))
            .color(new QuadColorState(bgMain))
            .build()
            .render(matrix, pX, pY);
         if (boldFont != null) {
            Builder.text()
               .font(boldFont)
               .text("Armor HUD")
               .size(7.5F)
               .color(new Color(255, 255, 255, (int)(255.0F * mainEase)))
               .build()
               .render(matrix, pX + 8.0F, pY + 6.5F);
         }

         float closeX = pX + mainW - 14.0F;
         float closeY = pY + 6.5F;
         boolean closeHover = mouseX >= closeX - 2.0F && mouseX <= closeX + 10.0F && mouseY >= closeY - 2.0F && mouseY <= closeY + 10.0F;
         Color closeColor = closeHover ? new Color(255, 80, 80, (int)(255.0F * mainEase)) : new Color(140, 140, 150, (int)(255.0F * mainEase));
         if (spFont != null) {
            Builder.text().font(spFont).text("A").size(7.0F).color(closeColor).build().render(matrix, closeX, closeY);
         }

         if (closeHover && isMouseDown && !wasMouseDown) {
            this.panelOpen = false;
            this.activeSubmenu = ArmorHudManager.SubmenuType.NONE;
            context.pose().popMatrix();
         } else {
            float c1X = pX + 4.0F;
            float c1Y = pY + 20.0F;
            float c1W = mainW - 8.0F;
            float c1H = 20.0F;
            Builder.rectangle()
               .size(new SizeState(c1W, c1H))
               .radius(new QuadRadiusState(6.0F))
               .color(new QuadColorState(new Color(10, 10, 15, (int)(220.0F * mainEase))))
               .build()
               .render(matrix, c1X, c1Y);
            float r1Y = c1Y + 2.0F;
            float r1H = 16.0F;
            boolean r1Hov = mouseX >= c1X && mouseX <= c1X + c1W && mouseY >= r1Y && mouseY <= r1Y + r1H;
            boolean r1Active = this.activeSubmenu == ArmorHudManager.SubmenuType.LAYOUT;
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
                  .text("B")
                  .size(7.5F)
                  .color(new Color(170, 170, 190, (int)(255.0F * mainEase)))
                  .build()
                  .render(matrix, c1X + 5.0F, r1Y + 4.0F);
            }

            if (mediumFont != null) {
               Builder.text()
                  .font(mediumFont)
                  .text("Layout")
                  .size(6.5F)
                  .color(new Color(230, 230, 245, (int)(255.0F * mainEase)))
                  .build()
                  .render(matrix, c1X + 16.0F, r1Y + 5.5F);
               String currentLayoutName = (isVertical() ? "Vertical" : "Horizontal") + " >";
               float layW = mediumFont.getWidth(currentLayoutName, 6.0F);
               Color layCol = r1Active ? GuiUtils.withAlpha(themeColor, mainEase) : new Color(140, 140, 160, (int)(255.0F * mainEase));
               Builder.text().font(mediumFont).text(currentLayoutName).size(6.0F).color(layCol).build().render(matrix, c1X + c1W - layW - 5.0F, r1Y + 5.5F);
            }

            if (r1Hov && isMouseDown && !wasMouseDown) {
               this.activeSubmenu = this.activeSubmenu == ArmorHudManager.SubmenuType.LAYOUT
                  ? ArmorHudManager.SubmenuType.NONE
                  : ArmorHudManager.SubmenuType.LAYOUT;
            }

            float c2X = pX + 4.0F;
            float c2Y = pY + 44.0F;
            float c2W = mainW - 8.0F;
            float c2H = 22.0F;
            Builder.rectangle()
               .size(new SizeState(c2W, c2H))
               .radius(new QuadRadiusState(6.0F))
               .color(new QuadColorState(new Color(10, 10, 15, (int)(220.0F * mainEase))))
               .build()
               .render(matrix, c2X, c2Y);
            boolean wgVal = warningGlow;
            float targetSwitch = wgVal ? 1.0F : 0.0F;
            this.warningGlowSwitchAnim = GuiUtils.animate(this.warningGlowSwitchAnim, targetSwitch, 16.0F, deltaTime);
            float wgY = c2Y + 3.0F;
            float wgH = 16.0F;
            boolean wgHov = mouseX >= c2X && mouseX <= c2X + c2W && mouseY >= wgY && mouseY <= wgY + wgH;
            if (wgHov) {
               Builder.rectangle()
                  .size(new SizeState(c2W - 4.0F, wgH))
                  .radius(new QuadRadiusState(4.0F))
                  .color(new QuadColorState(new Color(22, 22, 32, (int)(180.0F * mainEase))))
                  .build()
                  .render(matrix, c2X + 2.0F, wgY);
            }

            if (watermarkFont != null) {
               Builder.text()
                  .font(watermarkFont)
                  .text("G")
                  .size(7.5F)
                  .color(new Color(180, 180, 200, (int)(255.0F * mainEase)))
                  .build()
                  .render(matrix, c2X + 5.0F, wgY + 4.0F);
            }

            if (mediumFont != null) {
               Builder.text()
                  .font(mediumFont)
                  .text("Warning Glow")
                  .size(6.5F)
                  .color(new Color(230, 230, 245, (int)(255.0F * mainEase)))
                  .build()
                  .render(matrix, c2X + 16.0F, wgY + 5.5F);
            }

            float swW = 20.0F;
            float swH = 11.0F;
            float swX = c2X + c2W - swW - 5.0F;
            float swY = wgY + 2.5F;
            Color activeThemeCol = GuiUtils.withAlpha(themeColor, mainEase);
            Color offThemeCol = new Color(0, 0, 0, (int)(255.0F * mainEase));
            Color swBg = GuiUtils.interpolateColor(offThemeCol, activeThemeCol, this.warningGlowSwitchAnim);
            Builder.rectangle()
               .size(new SizeState(swW, swH))
               .radius(new QuadRadiusState(swH / 2.0F))
               .color(new QuadColorState(swBg))
               .build()
               .render(matrix, swX, swY);
            float baseKnobRadius = 3.8F;
            float offKnobX = swX + baseKnobRadius + 1.8F;
            float onKnobX = swX + swW - baseKnobRadius - 1.8F;
            float currentKnobCX = offKnobX + (onKnobX - offKnobX) * this.warningGlowSwitchAnim;
            float knobCY = swY + swH / 2.0F;
            float stretch = (float)Math.sin(this.warningGlowSwitchAnim * Math.PI) * 3.2F;
            float knobW = baseKnobRadius * 2.0F + stretch;
            float knobH = baseKnobRadius * 2.0F - stretch * 0.35F;
            float knobX0 = currentKnobCX - knobW / 2.0F;
            float knobY0 = knobCY - knobH / 2.0F;
            Builder.rectangle()
               .size(new SizeState(knobW, knobH))
               .radius(new QuadRadiusState(knobH / 2.0F))
               .color(new QuadColorState(new Color(255, 255, 255, (int)(255.0F * mainEase))))
               .build()
               .render(matrix, knobX0, knobY0);
            if (wgHov && isMouseDown && !wasMouseDown) {
               warningGlow = !warningGlow;
            }

            context.pose().popMatrix();
            if (this.subPanelAnim > 0.005F) {
               float subEase = 1.0F - (float)Math.pow(1.0F - this.subPanelAnim, 3.0);
               float subScale = 0.88F + 0.12F * subEase;
               float sX = pX + mainW + 5.0F;
               float sY = pY;
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
               if (this.activeSubmenu == ArmorHudManager.SubmenuType.LAYOUT) {
                  float rowH = 16.0F;
                  float listStartY = sY + 4.0F;

                  for (int i = 0; i < modes.length; i++) {
                     float rY = listStartY + i * rowH;
                     boolean isSel = layout.equalsIgnoreCase(modes[i]);
                     boolean rHov = mouseX >= sX + 3.0F && mouseX <= sX + subW - 3.0F && mouseY >= rY && mouseY <= rY + rowH - 1.0F;
                     if (rHov) {
                        Builder.rectangle()
                           .size(new SizeState(subW - 6.0F, rowH - 1.0F))
                           .radius(new QuadRadiusState(4.0F))
                           .color(new QuadColorState(new Color(25, 25, 36, (int)(220.0F * subEase))))
                           .build()
                           .render(matrix, sX + 3.0F, rY);
                     }

                     if (isSel && spFont != null) {
                        Builder.text()
                           .font(spFont)
                           .text("D")
                           .size(6.5F)
                           .color(new Color(255, 255, 255, (int)(255.0F * subEase)))
                           .build()
                           .render(matrix, sX + 6.0F, rY + 4.5F);
                     }

                     if (mediumFont != null) {
                        Color textCol = isSel
                           ? new Color(255, 255, 255, (int)(255.0F * subEase))
                           : (rHov ? new Color(200, 200, 220, (int)(255.0F * subEase)) : new Color(130, 130, 150, (int)(255.0F * subEase)));
                        Builder.text().font(mediumFont).text(labels[i]).size(6.5F).color(textCol).build().render(matrix, sX + 20.0F, rY + 5.5F);
                     }

                     if (rHov && isMouseDown && !wasMouseDown) {
                        layout = modes[i];
                     }
                  }
               }

               context.pose().popMatrix();
            }
         }
      }
   }

   public boolean onMouseClicked(double mouseX, double mouseY, int button) {
      boolean isEditing = this.client.gui.screen() instanceof HudEditorScreen;
      InterfaceModule interfaceMod = getInterfaceModule();
      if (interfaceMod == null) {
         return false;
      }

      if (isEditing || interfaceMod.isEnabled() && interfaceMod.armorHud.get()) {
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
            float mainW = 110.0F;
            float mainH = 70.0F;
            float subW = 95.0F;
            float subH = 40.0F;
            boolean hasSub = this.activeSubmenu != ArmorHudManager.SubmenuType.NONE;
            float pX = this.x;
            float pY = this.y + h * scaleModifier + 6.0F;
            float guiWidth = this.client.getWindow().getGuiScaledWidth();
            float guiHeight = this.client.getWindow().getGuiScaledHeight();
            if (pX + mainW + (hasSub ? subW + 5.0F : 0.0F) > guiWidth - 6.0F) {
               pX = guiWidth - mainW - (hasSub ? subW + 5.0F : 0.0F) - 6.0F;
            }

            if (pX < 6.0F) {
               pX = 6.0F;
            }

            float maxPanelH = hasSub ? Math.max(mainH, subH) : mainH;
            if (pY + maxPanelH > guiHeight - 6.0F) {
               pY = this.y - maxPanelH - 6.0F;
            }

            if (pY < 6.0F) {
               pY = 6.0F;
            }

            boolean inMain = mouseX >= pX && mouseX <= pX + mainW && mouseY >= pY && mouseY <= pY + mainH;
            boolean inSub = hasSub && mouseX >= pX + mainW + 5.0F && mouseX <= pX + mainW + 5.0F + subW && mouseY >= pY && mouseY <= pY + subH;
            if (inMain || inSub) {
               return true;
            }
         }

         if (button == 1 && mouseX >= visMinX - 4.0F && mouseX <= visMaxX + 4.0F && mouseY >= visMinY - 4.0F && mouseY <= visMaxY + 4.0F) {
            this.panelOpen = !this.panelOpen;
            if (this.panelOpen) {
               this.activeSubmenu = ArmorHudManager.SubmenuType.NONE;
            }

            this.wasRightMouseDown = true;
            return true;
         } else if (button == 0 && mouseX >= visMinX - 4.0F && mouseX <= visMaxX + 4.0F && mouseY >= visMinY - 4.0F && mouseY <= visMaxY + 4.0F) {
            this.dragging = true;
            this.dragOffsetX = (float)(mouseX - this.x);
            this.dragOffsetY = (float)(mouseY - this.y);
            return true;
         } else {
            return false;
         }
      } else {
         return false;
      }
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
         float targetX = (float)(mouseX - this.dragOffsetX);
         float targetY = (float)(mouseY - this.dragOffsetY);
         this.x = Math.max(0.0F, Math.min(Math.max(0.0F, screenW - w * scaleModifier), targetX));
         this.y = Math.max(0.0F, Math.min(Math.max(0.0F, screenH - h * scaleModifier), targetY));
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
      return AstolfoclientClient.moduleManager == null ? null : (InterfaceModule)AstolfoclientClient.moduleManager.getModuleByName("Interface");
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
   private enum SubmenuType {
      NONE,
      LAYOUT;
   }
}
