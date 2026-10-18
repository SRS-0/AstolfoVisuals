package xyz.angames.astolfoclient.client.hud;

import xyz.angames.astolfoclient.client.compat.GuiCompat;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.sun.management.OperatingSystemMXBean;
import dev.sxmurxy.mre.builders.Builder;
import dev.sxmurxy.mre.builders.states.QuadColorState;
import dev.sxmurxy.mre.builders.states.QuadRadiusState;
import dev.sxmurxy.mre.builders.states.SizeState;
import dev.sxmurxy.mre.msdf.MsdfFont;
import java.awt.Color;
import java.lang.management.ManagementFactory;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.multiplayer.PlayerInfo;
import org.joml.Matrix4f;
import xyz.angames.astolfoclient.client.compat.GLFW;
import xyz.angames.astolfoclient.client.AstolfoclientClient;
import xyz.angames.astolfoclient.client.DiscordRpcManager;
import xyz.angames.astolfoclient.client.config.ThemeManager;
import xyz.angames.astolfoclient.client.gui.HudEditorScreen;
import xyz.angames.astolfoclient.client.gui.clickgui.GuiUtils;
import xyz.angames.astolfoclient.client.module.modules.render.InterfaceModule;
import xyz.angames.astolfoclient.client.protection.ClientProtectionManager;
import xyz.angames.astolfoclient.client.util.DiscordAvatarManager;

@Environment(EnvType.CLIENT)
public class LogoRenderer {
   private final Minecraft client = Minecraft.getInstance();
   public static final Supplier<MsdfFont> ASTOLFO_FONT = Suppliers.memoize(
      () -> MsdfFont.builder()
         .name("astolfo_logo")
         .data(Identifier.fromNamespaceAndPath("mre", "fonts/astolfo.json"))
         .atlas(Identifier.fromNamespaceAndPath("mre", "fonts/astolfo.png"))
         .build()
   );
   public static final Supplier<MsdfFont> WATERMARK_FONT = Suppliers.memoize(
      () -> MsdfFont.builder()
         .name("watermark_icons")
         .data(Identifier.fromNamespaceAndPath("mre", "icons/watermark/watermark.json"))
         .atlas(Identifier.fromNamespaceAndPath("mre", "icons/watermark/watermark.png"))
         .glyphMapper(g -> {
            int idx = g.index();
            if (idx == 4) {
               return 65;
            } else if (idx == 5) {
               return 66;
            } else if (idx == 6) {
               return 67;
            } else if (idx == 7) {
               return 68;
            } else if (idx == 9) {
               return 69;
            } else if (idx == 10) {
               return 70;
            } else if (idx == 8) {
               return 71;
            } else if (idx == 3) {
               return 72;
            } else if (idx == 11) {
               return 73;
            } else if (idx == 12) {
               return 74;
            } else {
               return idx == 0 ? 75 : g.unicode() != 0 ? g.unicode() : idx;
            }
         })
         .build()
   );
   public static final Supplier<MsdfFont> SP_FONT = Suppliers.memoize(
      () -> MsdfFont.builder()
         .name("watermark_sp_icons")
         .data(Identifier.fromNamespaceAndPath("mre", "icons/watermark/seting-panel/watermark-sp.json"))
         .atlas(Identifier.fromNamespaceAndPath("mre", "icons/watermark/seting-panel/watermark-sp.png"))
         .glyphMapper(g -> {
            int idx = g.index();
            if (idx == 4) {
               return 65;
            } else if (idx == 5) {
               return 66;
            } else if (idx == 6) {
               return 67;
            } else if (idx == 3) {
               return 68;
            } else {
               return idx == 0 ? 69 : g.unicode() != 0 ? g.unicode() : idx;
            }
         })
         .build()
   );
   public static final Supplier<MsdfFont> SEMIBOLD_FONT = Suppliers.memoize(() -> MsdfFont.builder().atlas("semibold").data("semibold").build());
   public static final Supplier<MsdfFont> BOLD_FONT = Suppliers.memoize(() -> MsdfFont.builder().atlas("bold").data("bold").build());
   public static final Supplier<MsdfFont> MEDIUM_FONT = Suppliers.memoize(() -> MsdfFont.builder().atlas("medium").data("medium").build());
   public static String watermarkPosition = "TOP_LEFT";
   public static boolean showAvatar = true;
   public static final Set<LogoRenderer.SectionType> enabledSections = EnumSet.of(
      LogoRenderer.SectionType.BRAND,
      LogoRenderer.SectionType.SERVER,
      LogoRenderer.SectionType.TIME,
      LogoRenderer.SectionType.PING,
      LogoRenderer.SectionType.FPS,
      LogoRenderer.SectionType.CPU,
      LogoRenderer.SectionType.GPU,
      LogoRenderer.SectionType.RAM,
      LogoRenderer.SectionType.USER
   );
   public static final List<LogoRenderer.SectionType> sectionOrder = new ArrayList<>(
      Arrays.asList(
         LogoRenderer.SectionType.BRAND,
         LogoRenderer.SectionType.SERVER,
         LogoRenderer.SectionType.TIME,
         LogoRenderer.SectionType.PING,
         LogoRenderer.SectionType.FPS,
         LogoRenderer.SectionType.CPU,
         LogoRenderer.SectionType.GPU,
         LogoRenderer.SectionType.RAM,
         LogoRenderer.SectionType.USER
      )
   );
   private static float hotbarYOffsetAnim = 0.0F;
   private static long lastHotbarAnimTime = System.currentTimeMillis();
   private final Map<LogoRenderer.SectionType, Float> renderXMap = new HashMap<>();
   private final Map<LogoRenderer.SectionType, Float> scaleMap = new HashMap<>();
   private final Map<LogoRenderer.SectionType, Float> glowAlphaMap = new HashMap<>();
   private LogoRenderer.SectionType draggedSection = null;
   private float dragMouseOffsetX = 0.0F;
   private boolean wasMouseDown = false;
   private boolean wasRightMouseDown = false;
   private Object lastScreen = null;
   private boolean panelOpen = false;
   private LogoRenderer.SubmenuType activeSubmenu = LogoRenderer.SubmenuType.NONE;
   private float mainPanelAnim = 0.0F;
   private float subPanelAnim = 0.0F;
   private float avatarSwitchAnim = 1.0F;
   private float renderContainerX = 10.0F;
   private float renderContainerY = 10.0F;
   private long lastFrameTime = System.currentTimeMillis();
   private final LogoRenderer.AnimatedString serverAnim = new LogoRenderer.AnimatedString();
   private final LogoRenderer.AnimatedString timeAnim = new LogoRenderer.AnimatedString();
   private final LogoRenderer.AnimatedString pingAnim = new LogoRenderer.AnimatedString();
   private final LogoRenderer.AnimatedString fpsAnim = new LogoRenderer.AnimatedString();
   private final LogoRenderer.AnimatedString cpuAnim = new LogoRenderer.AnimatedString();
   private final LogoRenderer.AnimatedString gpuAnim = new LogoRenderer.AnimatedString();
   private final LogoRenderer.AnimatedString ramAnim = new LogoRenderer.AnimatedString();
   private final LogoRenderer.AnimatedString userAnim = new LogoRenderer.AnimatedString();

   public static List<LogoRenderer.SectionType> getSectionOrder() {
      return sectionOrder;
   }

   public static float getHotbarYOffset() {
      long now = System.currentTimeMillis();
      float delta = Math.min((float)(now - lastHotbarAnimTime) / 1000.0F, 0.05F);
      lastHotbarAnimTime = now;
      boolean isBotMid = isWatermarkAtBottomCenter();
      float target = isBotMid ? 34.0F : 0.0F;
      if (Math.abs(hotbarYOffsetAnim - target) > 0.05F) {
         hotbarYOffsetAnim = GuiUtils.animate(hotbarYOffsetAnim, target, 16.0F, delta);
      } else {
         hotbarYOffsetAnim = target;
      }

      return hotbarYOffsetAnim;
   }

   public static boolean isWatermarkAtBottomCenter() {
      if (AstolfoclientClient.moduleManager == null) {
         return false;
      } else if (!(AstolfoclientClient.moduleManager.getModuleByName("Interface") instanceof InterfaceModule ifaceMod)) {
         return false;
      } else if (ifaceMod.isEnabled() && ifaceMod.logo.get()) {
         String pos = watermarkPosition != null ? watermarkPosition : "TOP_LEFT";
         return "BOTTOM_CENTER".equalsIgnoreCase(pos) || "BOT_MID".equalsIgnoreCase(pos) || "BOTTOM_MID".equalsIgnoreCase(pos);
      } else {
         return false;
      }
   }

   public static void setSectionOrder(List<LogoRenderer.SectionType> newOrder) {
      if (newOrder != null && !newOrder.isEmpty()) {
         sectionOrder.clear();
         sectionOrder.addAll(newOrder);
      }
   }

   public static boolean isSectionEnabled(LogoRenderer.SectionType type) {
      return type == LogoRenderer.SectionType.BRAND ? true : enabledSections.contains(type);
   }

   public static void toggleSection(LogoRenderer.SectionType type) {
      if (type != LogoRenderer.SectionType.BRAND) {
         if (enabledSections.contains(type)) {
            enabledSections.remove(type);
         } else {
            enabledSections.add(type);
         }
      }
   }

   public static void setSectionEnabled(LogoRenderer.SectionType type, boolean enabled) {
      if (type != LogoRenderer.SectionType.BRAND) {
         if (enabled) {
            enabledSections.add(type);
         } else {
            enabledSections.remove(type);
         }
      }
   }

   private static float animate(float current, float target, float factor60, float deltaTime) {
      if (Math.abs(target - current) < 1.0E-4F) {
         return target;
      }

      float factor = 1.0F - (float)Math.pow(1.0 - factor60, deltaTime * 60.0F);
      return current + (target - current) * factor;
   }

   private InterfaceModule getInterfaceModule() {
      return AstolfoclientClient.moduleManager == null ? null : (InterfaceModule)AstolfoclientClient.moduleManager.getModuleByName("Interface");
   }

   public void render(GuiGraphicsExtractor context) {
      if (this.client.level != null && this.client.player != null) {
         InterfaceModule interfaceMod = this.getInterfaceModule();
         if (interfaceMod == null || interfaceMod.logo.get() || this.client.gui.screen() instanceof HudEditorScreen) {
            long now = System.currentTimeMillis();
            float deltaTime = (float)(now - this.lastFrameTime) / 1000.0F;
            this.lastFrameTime = now;
            if (deltaTime < 0.001F) {
               deltaTime = 0.001F;
            }

            if (deltaTime > 0.1F) {
               deltaTime = 0.1F;
            }

            MsdfFont astolfoFont = (MsdfFont)ASTOLFO_FONT.get();
            MsdfFont watermarkFont = (MsdfFont)WATERMARK_FONT.get();
            MsdfFont semiboldFont = (MsdfFont)SEMIBOLD_FONT.get();
            MsdfFont boldFont = (MsdfFont)BOLD_FONT.get();
            MsdfFont mediumFont = (MsdfFont)MEDIUM_FONT.get();
            MsdfFont spFont = (MsdfFont)SP_FONT.get();
            double currentGuiScale = this.client.getWindow().getGuiScale();
            if (currentGuiScale <= 0.0) {
               currentGuiScale = 2.0;
            }

            float scaleFactor = (float)(2.0 / currentGuiScale);
            float guiWidth = this.client.getWindow().getGuiScaledWidth();
            float guiHeight = this.client.getWindow().getGuiScaledHeight();

            try {
               float fontSize = 8.5F;
               float iconFontSize = 8.5F;
               float logoSize = 11.5F;
               float avatarSize = 13.0F;
               float paddingLeft = 7.0F;
               float paddingRight = 7.0F;
               float gap = 4.0F;
               float sectionGap = 7.0F;
               String brandText = "Astolfo Visuals";
               String serverStr = this.getServerStr();
               String timeStr = LocalTime.now().format(DateTimeFormatter.ofPattern("H:mm"));
               String pingStr = this.getPing() + " MS";
               String fpsStr = this.getFps() + " FPS";
               String cpuStr = this.getCpuUsage() + "% CPU";
               String gpuStr = this.getGpuUsage() + "% GPU";
               String ramStr = this.getRamUsage() + "% RAM";
               String userStr = DiscordRpcManager.discordUsername != null
                     && !DiscordRpcManager.discordUsername.trim().isEmpty()
                     && !DiscordRpcManager.discordUsername.equals("Connecting...")
                  ? DiscordRpcManager.discordUsername
                  : this.getUserName();
               this.serverAnim.update(serverStr);
               this.timeAnim.update(timeStr);
               this.pingAnim.update(pingStr);
               this.fpsAnim.update(fpsStr);
               this.cpuAnim.update(cpuStr);
               this.gpuAnim.update(gpuStr);
               this.ramAnim.update(ramStr);
               this.userAnim.update(userStr);
               Object activeScreen = this.client.gui.screen();
               if (activeScreen != this.lastScreen) {
                  this.draggedSection = null;
                  this.wasMouseDown = false;
                  this.wasRightMouseDown = false;
                  this.lastScreen = activeScreen;
               }

               boolean inChat = this.isAnyScreenOpen();
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
                  if (screenWidth > 0.0 && screenHeight > 0.0) {
                     mouseScaledX = mx[0] / screenWidth * guiWidth;
                     mouseScaledY = my[0] / screenHeight * guiHeight;
                  }

                  isMouseDown = GLFW.glfwGetMouseButton(win, 0) == 1;
                  isRightMouseDown = GLFW.glfwGetMouseButton(win, 1) == 1;
               }

               List<LogoRenderer.SectionType> activeSections = new ArrayList<>();

               for (LogoRenderer.SectionType type : sectionOrder) {
                  if (isSectionEnabled(type)) {
                     activeSections.add(type);
                  }
               }

               if (activeSections.isEmpty()) {
                  activeSections.add(LogoRenderer.SectionType.BRAND);
               }

               Map<LogoRenderer.SectionType, Float> widthMap = new HashMap<>();
               Map<LogoRenderer.SectionType, Float> targetXMap = new HashMap<>();
               float dotSectionSpacing = sectionGap * 2.0F;
               float accumX = paddingLeft;

               for (LogoRenderer.SectionType type : activeSections) {
                  float iconSize = type == LogoRenderer.SectionType.BRAND ? logoSize : iconFontSize;
                  MsdfFont secFont = type == LogoRenderer.SectionType.BRAND ? astolfoFont : watermarkFont;
                  float wIcon = secFont != null ? secFont.getWidth(type.icon, iconSize) : iconSize;
                  String text = this.getSectionText(type, brandText, serverStr, timeStr, pingStr, fpsStr, cpuStr, gpuStr, ramStr, userStr);
                  String templateText = text.replaceAll("[0-9]", "0");
                  float wText = semiboldFont != null ? semiboldFont.getWidth(templateText, fontSize) : 20.0F;
                  float wSection = wIcon + gap + wText;
                  widthMap.put(type, wSection);
                  targetXMap.put(type, accumX);
                  accumX += wSection + dotSectionSpacing;
               }

               LogoRenderer.SectionType lastSection = activeSections.get(activeSections.size() - 1);
               float lastSectionTargetX = targetXMap.getOrDefault(lastSection, paddingLeft);
               float lastSectionWidth = widthMap.getOrDefault(lastSection, 0.0F);
               boolean hasAvatar = showAvatar;
               float currX_Avatar = lastSectionTargetX + lastSectionWidth + sectionGap + 1.0F;
               float totalW = hasAvatar ? currX_Avatar + avatarSize + paddingRight : lastSectionTargetX + lastSectionWidth + paddingRight;
               float totalH = 22.0F;
               float pillRadius = 6.5F;
               float margin = 10.0F * scaleFactor;
               String posMode = watermarkPosition != null ? watermarkPosition : "TOP_LEFT";
               float targetContainerX;
               float targetContainerY;
               switch (posMode) {
                  case "TOP_CENTER":
                     targetContainerX = (guiWidth - totalW * scaleFactor) / 2.0F;
                     targetContainerY = margin;
                     break;
                  case "TOP_RIGHT":
                     targetContainerX = guiWidth - totalW * scaleFactor - margin;
                     targetContainerY = margin;
                     break;
                  case "BOTTOM_LEFT":
                     targetContainerX = margin;
                     targetContainerY = guiHeight - totalH * scaleFactor - margin;
                     break;
                  case "BOTTOM_CENTER":
                     targetContainerX = (guiWidth - totalW * scaleFactor) / 2.0F;
                     targetContainerY = guiHeight - totalH * scaleFactor - margin;
                     break;
                  case "BOTTOM_RIGHT":
                     targetContainerX = guiWidth - totalW * scaleFactor - margin;
                     targetContainerY = guiHeight - totalH * scaleFactor - margin;
                     break;
                  default:
                     targetContainerX = margin;
                     targetContainerY = margin;
               }

               if (Math.abs(this.renderContainerX - targetContainerX) > 0.1F) {
                  this.renderContainerX = animate(this.renderContainerX, targetContainerX, 0.25F, deltaTime);
               } else {
                  this.renderContainerX = targetContainerX;
               }

               if (Math.abs(this.renderContainerY - targetContainerY) > 0.1F) {
                  this.renderContainerY = animate(this.renderContainerY, targetContainerY, 0.25F, deltaTime);
               } else {
                  this.renderContainerY = targetContainerY;
               }

               float x = this.renderContainerX;
               float y = this.renderContainerY;
               float mouseInternalX = (float)((mouseScaledX - x) / scaleFactor);
               float mouseInternalY = (float)((mouseScaledY - y) / scaleFactor);
               if (inChat) {
                  if (isRightMouseDown && !this.wasRightMouseDown) {
                     float visContMinX = x;
                     float visContMaxX = x + totalW * scaleFactor;
                     float visContMinY = y;
                     float visContMaxY = y + totalH * scaleFactor;
                     if (mouseScaledX >= visContMinX - 6.0F
                        && mouseScaledX <= visContMaxX + 6.0F
                        && mouseScaledY >= visContMinY - 6.0F
                        && mouseScaledY <= visContMaxY + 6.0F) {
                        this.panelOpen = !this.panelOpen;
                        if (this.panelOpen) {
                           this.activeSubmenu = LogoRenderer.SubmenuType.NONE;
                        }
                     }
                  }
               } else {
                  this.panelOpen = false;
               }

               this.wasRightMouseDown = isRightMouseDown;
               if (!inChat) {
                  this.draggedSection = null;
                  this.wasMouseDown = false;
               } else if (!isMouseDown) {
                  this.draggedSection = null;
               } else if (this.draggedSection == null && !this.wasMouseDown && !this.panelOpen) {
                  for (int i = 1; i < activeSections.size(); i++) {
                     LogoRenderer.SectionType type = activeSections.get(i);
                     float sX = targetXMap.getOrDefault(type, paddingLeft);
                     float sW = widthMap.getOrDefault(type, 0.0F);
                     float visMinX = sX * scaleFactor + x;
                     float visMaxX = (sX + sW) * scaleFactor + x;
                     float visMinY = y - 5.0F;
                     float visMaxY = y + totalH * scaleFactor + 15.0F;
                     if (mouseScaledX >= visMinX - 6.0F && mouseScaledX <= visMaxX + 6.0F && mouseScaledY >= visMinY && mouseScaledY <= visMaxY) {
                        this.draggedSection = type;
                        this.dragMouseOffsetX = mouseInternalX - sX;
                        break;
                     }
                  }
               } else if (this.draggedSection != null && activeSections.contains(this.draggedSection)) {
                  int currIdx = sectionOrder.indexOf(this.draggedSection);
                  int targetIdx = currIdx;

                  for (int i = 1; i < activeSections.size(); i++) {
                     LogoRenderer.SectionType other = activeSections.get(i);
                     float oX = targetXMap.getOrDefault(other, 0.0F);
                     float oW = widthMap.getOrDefault(other, 0.0F);
                     float center = oX + oW / 2.0F;
                     if (mouseInternalX < center) {
                        int oIdx = sectionOrder.indexOf(other);
                        if (oIdx < targetIdx) {
                           targetIdx = oIdx;
                           break;
                        }
                     } else if (mouseInternalX > center) {
                        int oIdx = sectionOrder.indexOf(other);
                        if (oIdx > targetIdx) {
                           targetIdx = oIdx;
                        }
                     }
                  }

                  if (targetIdx != currIdx && targetIdx >= 1 && targetIdx < sectionOrder.size()) {
                     sectionOrder.remove(currIdx);
                     sectionOrder.add(targetIdx, this.draggedSection);
                  }
               }

               for (LogoRenderer.SectionType type : activeSections) {
                  float targetX = targetXMap.getOrDefault(type, 0.0F);
                  float currX = this.renderXMap.getOrDefault(type, targetX);
                  float sW = widthMap.getOrDefault(type, 0.0F);
                  if (type == this.draggedSection) {
                     float mouseTargetX = mouseInternalX - this.dragMouseOffsetX;
                     float wBrand = widthMap.getOrDefault(LogoRenderer.SectionType.BRAND, 0.0F);
                     float minAllowedX = paddingLeft + wBrand + dotSectionSpacing;
                     float maxAllowedX = totalW - paddingRight - (hasAvatar ? avatarSize + sectionGap : 0.0F) - sW;
                     if (mouseTargetX < minAllowedX) {
                        mouseTargetX = minAllowedX;
                     }

                     if (mouseTargetX > maxAllowedX) {
                        mouseTargetX = maxAllowedX;
                     }

                     currX = animate(currX, mouseTargetX, 0.45F, deltaTime);
                  } else {
                     currX = animate(currX, targetX, 0.25F, deltaTime);
                  }

                  this.renderXMap.put(type, currX);
                  float visMinX = currX * scaleFactor + x;
                  float visMaxX = (currX + sW) * scaleFactor + x;
                  float visMinY = y - 2.0F;
                  float visMaxY = y + totalH * scaleFactor + 2.0F;
                  boolean isHovered = inChat
                     && type != LogoRenderer.SectionType.BRAND
                     && mouseScaledX >= visMinX - 2.0F
                     && mouseScaledX <= visMaxX + 2.0F
                     && mouseScaledY >= visMinY
                     && mouseScaledY <= visMaxY;
                  float targetScale = type == this.draggedSection ? 1.14F : (isHovered ? 1.06F : 1.0F);
                  float targetGlow = type == this.draggedSection ? 0.3F : (isHovered ? 0.15F : 0.055F);
                  float currScale = this.scaleMap.getOrDefault(type, 1.0F);
                  float currGlow = this.glowAlphaMap.getOrDefault(type, 0.055F);
                  currScale = animate(currScale, targetScale, 0.25F, deltaTime);
                  currGlow = animate(currGlow, targetGlow, 0.25F, deltaTime);
                  this.scaleMap.put(type, currScale);
                  this.glowAlphaMap.put(type, currGlow);
               }

               Color themeColor = new Color(ThemeManager.getThemedColor(now / 10L));
               context.pose().pushMatrix();
               context.pose().translate(x, y);
               context.pose().scale(scaleFactor, scaleFactor);
               context.pose().translate(-x, -y);
               Matrix4f matrix = GuiCompat.matrix4f(context);
               int shadowSteps = 12;
               float maxSpread = 8.0F;

               for (int i = shadowSteps - 1; i >= 0; i--) {
                  float progress = (float)i / shadowSteps;
                  float spread = progress * maxSpread;
                  float alphaFactor = (1.0F - progress) * (1.0F - progress);
                  int alpha = (int)(102.0F * alphaFactor);
                  if (alpha > 0) {
                     Builder.rectangle()
                        .size(new SizeState(totalW + spread * 2.0F, totalH + spread * 2.0F))
                        .radius(new QuadRadiusState(pillRadius + spread))
                        .color(new QuadColorState(new Color(0, 0, 0, alpha)))
                        .build()
                        .render(matrix, x - spread, y - spread + 1.0F);
                  }
               }

               Builder.rectangle()
                  .size(new SizeState(totalW, totalH))
                  .radius(new QuadRadiusState(pillRadius))
                  .color(new QuadColorState(new Color(0, 0, 0, 255)))
                  .build()
                  .render(matrix, x, y);
               Color textColor = Color.WHITE;
               Color dotColor = new Color(130, 130, 135, 220);
               float centerY = y + totalH / 2.0F;
               float textTop = centerY - 3.1F;
               float iconTop = centerY - 4.93F;
               float logoTop = centerY - 6.9F;
               float dotTop = centerY - 1.1F;
               float avatarTop = centerY - 6.5F;
               float fullAvatarX = x + currX_Avatar;
               float avatarRadius = avatarSize / 2.0F;
               float avatarCenterX = fullAvatarX + avatarRadius;

               for (LogoRenderer.SectionType type : activeSections) {
                  float sX = x + this.renderXMap.getOrDefault(type, 0.0F);
                  float iconSize = type == LogoRenderer.SectionType.BRAND ? logoSize : iconFontSize;
                  MsdfFont secFont = type == LogoRenderer.SectionType.BRAND ? astolfoFont : watermarkFont;
                  float wIcon = secFont != null ? secFont.getWidth(type.icon, iconSize) : iconSize;
                  float iconCX = sX + wIcon / 2.0F;
                  float gAlpha = this.glowAlphaMap.getOrDefault(type, 0.055F);
                  this.drawIconGlowShadow(matrix, iconCX, centerY, iconSize / 2.0F, themeColor, gAlpha);
               }

               if (hasAvatar) {
                  this.drawIconGlowShadow(matrix, avatarCenterX, centerY, avatarRadius, themeColor, 0.055F);
               }

               for (int i = 0; i < activeSections.size(); i++) {
                  LogoRenderer.SectionType type = activeSections.get(i);
                  float sX = x + this.renderXMap.getOrDefault(type, 0.0F);
                  float sW = widthMap.getOrDefault(type, 0.0F);
                  if (type == LogoRenderer.SectionType.BRAND) {
                     if (astolfoFont != null) {
                        Builder.text().font(astolfoFont).text("A").size(logoSize).color(themeColor).build().render(matrix, sX, logoTop);
                     }

                     float textX = sX + (astolfoFont != null ? astolfoFont.getWidth("A", logoSize) : logoSize) + gap;
                     Builder.text().font(semiboldFont).text(brandText).size(fontSize).color(textColor).build().render(matrix, textX, textTop);
                  } else {
                     if (watermarkFont != null) {
                        Builder.text().font(watermarkFont).text(type.icon).size(iconFontSize).color(themeColor).build().render(matrix, sX, iconTop);
                     }

                     float textX = sX + (watermarkFont != null ? watermarkFont.getWidth(type.icon, iconFontSize) : iconFontSize) + gap;
                     this.renderSectionAnim(type, matrix, textX, textTop, textColor, semiboldFont, fontSize, brandText, userStr);
                  }

                  if (i < activeSections.size() - 1) {
                     float slotTargetX = x + targetXMap.getOrDefault(type, 0.0F);
                     float dotX = slotTargetX + sW + sectionGap;
                     Builder.rectangle()
                        .size(new SizeState(2.2F, 2.2F))
                        .radius(new QuadRadiusState(1.1F))
                        .color(new QuadColorState(dotColor))
                        .build()
                        .render(matrix, dotX - 1.1F, dotTop);
                  }
               }

               if (hasAvatar) {
                  boolean drawnAvatar = false;
                  Identifier avatarTex = DiscordAvatarManager.getAvatarTexture();
                  if (avatarTex != null) {
                     try {
                        AbstractTexture tex = this.client.getTextureManager().getTexture(avatarTex);
                        if (tex != null) {
                           Builder.texture()
                              .size(new SizeState(avatarSize, avatarSize))
                              .radius(new QuadRadiusState(avatarRadius))
                              .texture(0.0F, 0.0F, 1.0F, 1.0F, tex)
                              .color(new QuadColorState(Color.WHITE))
                              .build()
                              .render(matrix, fullAvatarX, avatarTop);
                           drawnAvatar = true;
                        }
                     } catch (Exception var89) {
                     }
                  }

                  if (!drawnAvatar && this.client.player != null) {
                     try {
                        Identifier skinTex = this.client.player.getSkin().body().texturePath();
                        if (skinTex != null) {
                           AbstractTexture tex = this.client.getTextureManager().getTexture(skinTex);
                           if (tex != null) {
                              Builder.texture()
                                 .size(new SizeState(avatarSize, avatarSize))
                                 .radius(new QuadRadiusState(avatarRadius))
                                 .texture(0.125F, 0.125F, 0.125F, 0.125F, tex)
                                 .color(new QuadColorState(Color.WHITE))
                                 .build()
                                 .render(matrix, fullAvatarX, avatarTop);
                              Builder.texture()
                                 .size(new SizeState(avatarSize, avatarSize))
                                 .radius(new QuadRadiusState(avatarRadius))
                                 .texture(0.625F, 0.125F, 0.125F, 0.125F, tex)
                                 .color(new QuadColorState(Color.WHITE))
                                 .build()
                                 .render(matrix, fullAvatarX, avatarTop);
                              drawnAvatar = true;
                           }
                        }
                     } catch (Exception var88) {
                     }
                  }

                  if (!drawnAvatar && watermarkFont != null) {
                     Builder.text()
                        .font(watermarkFont)
                        .text("A")
                        .size(iconFontSize)
                        .color(Color.WHITE)
                        .build()
                        .render(matrix, fullAvatarX + (avatarSize - iconFontSize) / 2.0F, iconTop);
                  }
               }

               context.pose().popMatrix();
               this.drawCrashAlerts(context, x, y, totalW, totalH, scaleFactor, guiWidth, guiHeight, themeColor, deltaTime);
               if (inChat) {
                  this.drawAnimatedContextPanel(
                     context,
                     x,
                     y,
                     totalW,
                     totalH,
                     scaleFactor,
                     guiWidth,
                     guiHeight,
                     themeColor,
                     mouseScaledX,
                     mouseScaledY,
                     isMouseDown,
                     this.wasMouseDown,
                     deltaTime
                  );
               } else {
                  this.panelOpen = false;
                  this.activeSubmenu = LogoRenderer.SubmenuType.NONE;
                  this.mainPanelAnim = 0.0F;
                  this.subPanelAnim = 0.0F;
               }

               this.wasMouseDown = isMouseDown;
            } catch (Exception e) {
               e.printStackTrace();
            }
         }
      }
   }

   private void drawCrashAlerts(
      GuiGraphicsExtractor context,
      float wX,
      float wY,
      float wTotalW,
      float wTotalH,
      float scaleFactor,
      float guiWidth,
      float guiHeight,
      Color themeColor,
      float deltaTime
   ) {
      List<ClientProtectionManager.CrashAlert> alerts = ClientProtectionManager.getInstance().getActiveAlerts();
      if (alerts != null && !alerts.isEmpty()) {
         MsdfFont boldFont = (MsdfFont)BOLD_FONT.get();
         MsdfFont semiboldFont = (MsdfFont)SEMIBOLD_FONT.get();
         MsdfFont astolfoFont = (MsdfFont)ASTOLFO_FONT.get();
         String posMode = watermarkPosition != null ? watermarkPosition : "TOP_LEFT";
         boolean isBottom = posMode.startsWith("BOTTOM");
         float currY = isBottom ? wY - 4.0F : wY + wTotalH * scaleFactor + 4.0F;
         long now = System.currentTimeMillis();
         float logoSize = 11.5F;
         float logoW = astolfoFont != null ? astolfoFont.getWidth("A", logoSize) : 10.0F;

         for (int i = 0; i < alerts.size(); i++) {
            ClientProtectionManager.CrashAlert alert = alerts.get(i);
            long age = now - alert.timestamp;
            float targetAnim = age < 4000L ? 1.0F : 0.0F;
            alert.anim = animate(alert.anim, targetAnim, 0.22F, deltaTime);
            if (!(alert.anim < 0.01F) || age < 4000L) {
               float ease = 1.0F - (float)Math.pow(1.0F - alert.anim, 3.0);
               int alpha = (int)(255.0F * ease);
               if (alpha > 0) {
                  String titleText = "CRASH BLOCKED: " + alert.title.toUpperCase();
                  String detailText = alert.details;
                  float titleW = boldFont != null ? boldFont.getWidth(titleText, 7.5F) : 70.0F;
                  float detailW = semiboldFont != null ? semiboldFont.getWidth(detailText, 7.0F) : 60.0F;
                  float textLeftOffset = 7.0F + logoW + 6.0F;
                  float cardW = Math.max(titleW, detailW) + textLeftOffset + 10.0F;
                  float cardH = 22.0F;
                  float cardX = wX;
                  if (posMode.contains("RIGHT")) {
                     cardX = wX + wTotalW * scaleFactor - cardW;
                  } else if (posMode.contains("CENTER")) {
                     cardX = wX + (wTotalW * scaleFactor - cardW) / 2.0F;
                  }

                  if (cardX < 6.0F) {
                     cardX = 6.0F;
                  }

                  if (cardX + cardW > guiWidth - 6.0F) {
                     cardX = guiWidth - cardW - 6.0F;
                  }

                  float cardY = isBottom ? currY - cardH : currY;
                  context.pose().pushMatrix();
                  float scale = 0.9F + 0.1F * ease;
                  if (scale < 0.999F) {
                     float cx = cardX + cardW / 2.0F;
                     float cy = cardY + cardH / 2.0F;
                     context.pose().translate(cx, cy);
                     context.pose().scale(scale, scale);
                     context.pose().translate(-cx, -cy);
                  }

                  Matrix4f matrix = GuiCompat.matrix4f(context);

                  for (int s = 6; s >= 0; s--) {
                     float progress = s / 6.0F;
                     float spread = progress * 4.0F;
                     int sAlpha = (int)(60.0F * (1.0F - progress) * (1.0F - progress) * ease);
                     if (sAlpha > 0) {
                        Builder.rectangle()
                           .size(new SizeState(cardW + spread * 2.0F, cardH + spread * 2.0F))
                           .radius(new QuadRadiusState(6.5F + spread))
                           .color(new QuadColorState(new Color(0, 0, 0, sAlpha)))
                           .build()
                           .render(matrix, cardX - spread, cardY - spread + 1.0F);
                     }
                  }

                  Builder.rectangle()
                     .size(new SizeState(cardW, cardH))
                     .radius(new QuadRadiusState(6.5F))
                     .color(new QuadColorState(new Color(0, 0, 0, (int)(245.0F * ease))))
                     .build()
                     .render(matrix, cardX, cardY);
                  if (astolfoFont != null) {
                     Color logoCol = new Color(themeColor.getRed(), themeColor.getGreen(), themeColor.getBlue(), alpha);
                     Builder.text()
                        .font(astolfoFont)
                        .text("A")
                        .size(logoSize)
                        .color(logoCol)
                        .build()
                        .render(matrix, cardX + 7.0F, cardY + (cardH - logoSize) / 2.0F - 1.0F);
                  }

                  if (boldFont != null) {
                     Color titleCol = new Color(255, 90, 90, alpha);
                     Builder.text().font(boldFont).text(titleText).size(7.5F).color(titleCol).build().render(matrix, cardX + textLeftOffset, cardY + 3.5F);
                  }

                  if (semiboldFont != null) {
                     Builder.text()
                        .font(semiboldFont)
                        .text(detailText)
                        .size(7.0F)
                        .color(new Color(230, 230, 235, alpha))
                        .build()
                        .render(matrix, cardX + textLeftOffset, cardY + 12.0F);
                  }

                  float progress = Math.max(0.0F, Math.min(1.0F, 1.0F - (float)age / 4500.0F));
                  float barW = (cardW - 14.0F) * progress;
                  if (barW > 1.0F) {
                     Builder.rectangle()
                        .size(new SizeState(barW, 1.2F))
                        .radius(new QuadRadiusState(0.6F))
                        .color(new QuadColorState(new Color(themeColor.getRed(), themeColor.getGreen(), themeColor.getBlue(), (int)(180.0F * ease))))
                        .build()
                        .render(matrix, cardX + 7.0F, cardY + cardH - 2.5F);
                  }

                  context.pose().popMatrix();
                  if (isBottom) {
                     currY -= cardH + 4.0F;
                  } else {
                     currY += cardH + 4.0F;
                  }
               }
            }
         }
      }
   }

   private void drawAnimatedContextPanel(
      GuiGraphicsExtractor context,
      float wX,
      float wY,
      float wTotalW,
      float wTotalH,
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
      this.mainPanelAnim = animate(this.mainPanelAnim, targetMain, 0.22F, deltaTime);
      if (!this.panelOpen && this.mainPanelAnim < 0.01F) {
         this.mainPanelAnim = 0.0F;
      } else {
         boolean hasSub = this.activeSubmenu != LogoRenderer.SubmenuType.NONE;
         float targetSub = hasSub && this.panelOpen ? 1.0F : 0.0F;
         this.subPanelAnim = animate(this.subPanelAnim, targetSub, 0.22F, deltaTime);
         float mainW = 110.0F;
         float mainH = 86.0F;
         float mX = wX;
         float mY = wY + wTotalH * scaleFactor + 6.0F;
         float subW = 115.0F;
         LogoRenderer.SectionType[] toggleableTypes = new LogoRenderer.SectionType[]{
            LogoRenderer.SectionType.SERVER,
            LogoRenderer.SectionType.TIME,
            LogoRenderer.SectionType.PING,
            LogoRenderer.SectionType.FPS,
            LogoRenderer.SectionType.CPU,
            LogoRenderer.SectionType.GPU,
            LogoRenderer.SectionType.RAM,
            LogoRenderer.SectionType.USER
         };
         float subH = this.activeSubmenu == LogoRenderer.SubmenuType.ITEMS
            ? toggleableTypes.length * 15.0F + 8.0F
            : (this.activeSubmenu == LogoRenderer.SubmenuType.POSITION ? 104.0F : 0.0F);
         if (mX + mainW + (hasSub ? subW + 5.0F : 0.0F) > guiWidth - 6.0F) {
            mX = guiWidth - mainW - (hasSub ? subW + 5.0F : 0.0F) - 6.0F;
         }

         if (mX < 6.0F) {
            mX = 6.0F;
         }

         float maxPanelH = hasSub ? Math.max(mainH, subH) : mainH;
         if (mY + maxPanelH > guiHeight - 6.0F) {
            mY = wY - maxPanelH - 6.0F;
         }

         if (mY < 6.0F) {
            mY = 6.0F;
         }

         MsdfFont boldFont = (MsdfFont)BOLD_FONT.get();
         MsdfFont mediumFont = (MsdfFont)MEDIUM_FONT.get();
         MsdfFont spFont = (MsdfFont)SP_FONT.get();
         MsdfFont watermarkFont = (MsdfFont)WATERMARK_FONT.get();
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
               .text("Watermark")
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
            this.activeSubmenu = LogoRenderer.SubmenuType.NONE;
            context.pose().popMatrix();
         } else {
            float c1X = mX + 4.0F;
            float c1Y = mY + 20.0F;
            float c1W = mainW - 8.0F;
            float c1H = 36.0F;
            Builder.rectangle()
               .size(new SizeState(c1W, c1H))
               .radius(new QuadRadiusState(6.0F))
               .color(new QuadColorState(new Color(10, 10, 15, (int)(220.0F * mainEase))))
               .build()
               .render(matrix, c1X, c1Y);
            float r1Y = c1Y + 2.0F;
            float r1H = 16.0F;
            boolean r1Hov = mouseX >= c1X && mouseX <= c1X + c1W && mouseY >= r1Y && mouseY <= r1Y + r1H;
            boolean r1Active = this.activeSubmenu == LogoRenderer.SubmenuType.ITEMS;
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
                  .text("Items")
                  .size(6.5F)
                  .color(new Color(230, 230, 245, (int)(255.0F * mainEase)))
                  .build()
                  .render(matrix, c1X + 16.0F, r1Y + 5.5F);
               String selStr = "Select >";
               float selW = mediumFont.getWidth(selStr, 6.0F);
               Color selCol = r1Active ? GuiUtils.withAlpha(themeColor, mainEase) : new Color(140, 140, 160, (int)(255.0F * mainEase));
               Builder.text().font(mediumFont).text(selStr).size(6.0F).color(selCol).build().render(matrix, c1X + c1W - selW - 5.0F, r1Y + 5.5F);
            }

            if (r1Hov && isMouseDown && !wasMouseDown) {
               this.activeSubmenu = this.activeSubmenu == LogoRenderer.SubmenuType.ITEMS ? LogoRenderer.SubmenuType.NONE : LogoRenderer.SubmenuType.ITEMS;
            }

            Builder.rectangle()
               .size(new SizeState(c1W - 12.0F, 1.0F))
               .radius(new QuadRadiusState(0.5F))
               .color(new QuadColorState(new Color(22, 22, 30, (int)(180.0F * mainEase))))
               .build()
               .render(matrix, c1X + 6.0F, c1Y + 18.0F);
            float r2Y = c1Y + 18.0F;
            float r2H = 16.0F;
            boolean r2Hov = mouseX >= c1X && mouseX <= c1X + c1W && mouseY >= r2Y && mouseY <= r2Y + r2H;
            boolean r2Active = this.activeSubmenu == LogoRenderer.SubmenuType.POSITION;
            if (r2Hov) {
               Builder.rectangle()
                  .size(new SizeState(c1W - 4.0F, r2H))
                  .radius(new QuadRadiusState(4.0F))
                  .color(new QuadColorState(new Color(22, 22, 32, (int)(180.0F * mainEase))))
                  .build()
                  .render(matrix, c1X + 2.0F, r2Y);
            }

            if (spFont != null) {
               Builder.text()
                  .font(spFont)
                  .text("C")
                  .size(7.5F)
                  .color(new Color(170, 170, 190, (int)(255.0F * mainEase)))
                  .build()
                  .render(matrix, c1X + 5.0F, r2Y + 4.0F);
            }

            if (mediumFont != null) {
               Builder.text()
                  .font(mediumFont)
                  .text("Position")
                  .size(6.5F)
                  .color(new Color(230, 230, 245, (int)(255.0F * mainEase)))
                  .build()
                  .render(matrix, c1X + 16.0F, r2Y + 5.5F);
               String currentPosName = this.getPosShortLabel(watermarkPosition != null ? watermarkPosition : "TOP_LEFT") + " >";
               float posStrW = mediumFont.getWidth(currentPosName, 6.0F);
               Color posCol = r2Active ? GuiUtils.withAlpha(themeColor, mainEase) : new Color(140, 140, 160, (int)(255.0F * mainEase));
               Builder.text().font(mediumFont).text(currentPosName).size(6.0F).color(posCol).build().render(matrix, c1X + c1W - posStrW - 5.0F, r2Y + 5.5F);
            }

            if (r2Hov && isMouseDown && !wasMouseDown) {
               this.activeSubmenu = this.activeSubmenu == LogoRenderer.SubmenuType.POSITION ? LogoRenderer.SubmenuType.NONE : LogoRenderer.SubmenuType.POSITION;
            }

            float c2X = mX + 4.0F;
            float c2Y = mY + 60.0F;
            float c2W = mainW - 8.0F;
            float c2H = 22.0F;
            Builder.rectangle()
               .size(new SizeState(c2W, c2H))
               .radius(new QuadRadiusState(6.0F))
               .color(new QuadColorState(new Color(10, 10, 15, (int)(220.0F * mainEase))))
               .build()
               .render(matrix, c2X, c2Y);
            boolean avVal = showAvatar;
            float targetSwitch = avVal ? 1.0F : 0.0F;
            this.avatarSwitchAnim = animate(this.avatarSwitchAnim, targetSwitch, 0.28F, deltaTime);
            float avY = c2Y + 3.0F;
            float avH = 16.0F;
            boolean avHov = mouseX >= c2X && mouseX <= c2X + c2W && mouseY >= avY && mouseY <= avY + avH;
            if (avHov) {
               Builder.rectangle()
                  .size(new SizeState(c2W - 4.0F, avH))
                  .radius(new QuadRadiusState(4.0F))
                  .color(new QuadColorState(new Color(22, 22, 32, (int)(180.0F * mainEase))))
                  .build()
                  .render(matrix, c2X + 2.0F, avY);
            }

            if (watermarkFont != null) {
               Builder.text()
                  .font(watermarkFont)
                  .text("A")
                  .size(7.5F)
                  .color(new Color(180, 180, 200, (int)(255.0F * mainEase)))
                  .build()
                  .render(matrix, c2X + 5.0F, avY + 4.0F);
            }

            if (mediumFont != null) {
               Builder.text()
                  .font(mediumFont)
                  .text("Avatar")
                  .size(6.5F)
                  .color(new Color(230, 230, 245, (int)(255.0F * mainEase)))
                  .build()
                  .render(matrix, c2X + 16.0F, avY + 5.5F);
            }

            float swW = 20.0F;
            float swH = 11.0F;
            float swX = c2X + c2W - swW - 5.0F;
            float swY = avY + 2.5F;
            Color activeThemeCol = GuiUtils.withAlpha(themeColor, mainEase);
            Color offThemeCol = new Color(0, 0, 0, (int)(255.0F * mainEase));
            Color swBg = GuiUtils.interpolateColor(offThemeCol, activeThemeCol, this.avatarSwitchAnim);
            Builder.rectangle()
               .size(new SizeState(swW, swH))
               .radius(new QuadRadiusState(swH / 2.0F))
               .color(new QuadColorState(swBg))
               .build()
               .render(matrix, swX, swY);
            float baseKnobRadius = 3.8F;
            float offKnobX = swX + baseKnobRadius + 1.8F;
            float onKnobX = swX + swW - baseKnobRadius - 1.8F;
            float currentKnobCX = offKnobX + (onKnobX - offKnobX) * this.avatarSwitchAnim;
            float knobCY = swY + swH / 2.0F;
            float stretch = (float)Math.sin(this.avatarSwitchAnim * Math.PI) * 3.2F;
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
            if (avHov && isMouseDown && !wasMouseDown) {
               showAvatar = !showAvatar;
            }

            context.pose().popMatrix();
            if (this.subPanelAnim > 0.005F) {
               float subEase = 1.0F - (float)Math.pow(1.0F - this.subPanelAnim, 3.0);
               float subScale = 0.88F + 0.12F * subEase;
               float sX = mX + mainW + 5.0F;
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
               if (this.activeSubmenu == LogoRenderer.SubmenuType.ITEMS) {
                  float itemRowH = 15.0F;
                  float listStartY = sY + 4.0F;

                  for (int i = 0; i < toggleableTypes.length; i++) {
                     LogoRenderer.SectionType type = toggleableTypes[i];
                     float rY = listStartY + i * itemRowH;
                     boolean isEnabled = isSectionEnabled(type);
                     boolean rHov = mouseX >= sX + 3.0F && mouseX <= sX + subW - 3.0F && mouseY >= rY && mouseY <= rY + itemRowH - 1.0F;
                     if (rHov) {
                        Builder.rectangle()
                           .size(new SizeState(subW - 6.0F, itemRowH - 1.0F))
                           .radius(new QuadRadiusState(4.0F))
                           .color(new QuadColorState(new Color(25, 25, 36, (int)(220.0F * subEase))))
                           .build()
                           .render(matrix, sX + 3.0F, rY);
                     }

                     if (isEnabled && spFont != null) {
                        Builder.text()
                           .font(spFont)
                           .text("D")
                           .size(6.5F)
                           .color(new Color(255, 255, 255, (int)(255.0F * subEase)))
                           .build()
                           .render(matrix, sX + 6.0F, rY + 4.0F);
                     }

                     if (watermarkFont != null) {
                        Builder.text()
                           .font(watermarkFont)
                           .text(type.icon)
                           .size(7.5F)
                           .color(GuiUtils.withAlpha(themeColor, subEase))
                           .build()
                           .render(matrix, sX + 21.0F, rY + 3.5F);
                     }

                     if (mediumFont != null) {
                        Color textCol = isEnabled
                           ? new Color(240, 240, 255, (int)(255.0F * subEase))
                           : (rHov ? new Color(180, 180, 200, (int)(255.0F * subEase)) : new Color(110, 110, 130, (int)(255.0F * subEase)));
                        Builder.text().font(mediumFont).text(type.title).size(6.5F).color(textCol).build().render(matrix, sX + 33.0F, rY + 5.0F);
                     }

                     if (rHov && isMouseDown && !wasMouseDown) {
                        toggleSection(type);
                     }
                  }
               } else if (this.activeSubmenu == LogoRenderer.SubmenuType.POSITION) {
                  String[] posKeys = new String[]{"TOP_LEFT", "TOP_CENTER", "TOP_RIGHT", "BOTTOM_LEFT", "BOTTOM_CENTER", "BOTTOM_RIGHT"};
                  String[] posLabels = new String[]{"Top Left", "Top Mid", "Top Right", "Bot Left", "Bot Mid", "Bot Right"};
                  float posRowH = 16.0F;
                  float listStartY = sY + 4.0F;
                  String currentPosKey = watermarkPosition != null ? watermarkPosition : "TOP_LEFT";

                  for (int i = 0; i < posKeys.length; i++) {
                     float rY = listStartY + i * posRowH;
                     boolean isSel = currentPosKey.equals(posKeys[i]);
                     boolean rHov = mouseX >= sX + 3.0F && mouseX <= sX + subW - 3.0F && mouseY >= rY && mouseY <= rY + posRowH - 1.0F;
                     if (rHov) {
                        Builder.rectangle()
                           .size(new SizeState(subW - 6.0F, posRowH - 1.0F))
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
                        Builder.text().font(mediumFont).text(posLabels[i]).size(6.5F).color(textCol).build().render(matrix, sX + 20.0F, rY + 5.5F);
                     }

                     if (rHov && isMouseDown && !wasMouseDown) {
                        watermarkPosition = posKeys[i];
                     }
                  }
               }

               context.pose().popMatrix();
            }
         }
      }
   }

   private String getPosShortLabel(String pos) {
      return switch (pos) {
         case "TOP_CENTER" -> "Top-Mid";
         case "TOP_RIGHT" -> "Top-Right";
         case "BOTTOM_LEFT" -> "Bot-Left";
         case "BOTTOM_CENTER" -> "Bot-Mid";
         case "BOTTOM_RIGHT" -> "Bot-Right";
         default -> "Top-Left";
      };
   }

   private String getSectionText(
      LogoRenderer.SectionType type,
      String brandText,
      String serverStr,
      String timeStr,
      String pingStr,
      String fpsStr,
      String cpuStr,
      String gpuStr,
      String ramStr,
      String userStr
   ) {
      return switch (type) {
         case BRAND -> brandText;
         case SERVER -> serverStr;
         case TIME -> timeStr;
         case PING -> pingStr;
         case FPS -> fpsStr;
         case CPU -> cpuStr;
         case GPU -> gpuStr;
         case RAM -> ramStr;
         case USER -> userStr;
      };
   }

   private void renderSectionAnim(
      LogoRenderer.SectionType type, Matrix4f matrix, float x, float y, Color textColor, MsdfFont font, float fontSize, String brandText, String userStr
   ) {
      if (font != null) {
         switch (type) {
            case BRAND:
               Builder.text().font(font).text(brandText).size(fontSize).color(textColor).build().render(matrix, x, y);
               break;
            case SERVER:
               this.serverAnim.render(matrix, x, y, textColor, font, fontSize);
               break;
            case TIME:
               this.timeAnim.render(matrix, x, y, textColor, font, fontSize);
               break;
            case PING:
               this.pingAnim.render(matrix, x, y, textColor, font, fontSize);
               break;
            case FPS:
               this.fpsAnim.render(matrix, x, y, textColor, font, fontSize);
               break;
            case CPU:
               this.cpuAnim.render(matrix, x, y, textColor, font, fontSize);
               break;
            case GPU:
               this.gpuAnim.render(matrix, x, y, textColor, font, fontSize);
               break;
            case RAM:
               this.ramAnim.render(matrix, x, y, textColor, font, fontSize);
               break;
            case USER:
               this.userAnim.render(matrix, x, y, textColor, font, fontSize);
         }
      }
   }

   private void drawIconGlowShadow(Matrix4f matrix, float cx, float cy, float baseRadius, Color themeColor, float alphaMultiplier) {
      int glowSteps = 6;
      float maxSpread = 8.0F;
      int r = themeColor.getRed();
      int g = themeColor.getGreen();
      int b = themeColor.getBlue();
      float baseAlpha = themeColor.getAlpha() / 255.0F;
      float glowCy = cy - 2.0F;

      for (int i = glowSteps - 1; i >= 0; i--) {
         float progress = (float)i / glowSteps;
         float spread = progress * maxSpread;
         float alphaFactor = (1.0F - progress) * (1.0F - progress);
         float alpha = baseAlpha * alphaMultiplier * alphaFactor;
         int alphaInt = (int)(255.0F * alpha);
         if (alphaInt > 0) {
            Color glowColor = new Color(r, g, b, alphaInt);
            float radius = baseRadius + spread;
            Builder.rectangle()
               .size(new SizeState(radius * 2.0F, radius * 2.0F))
               .radius(new QuadRadiusState(radius))
               .color(new QuadColorState(glowColor))
               .build()
               .render(matrix, cx - radius, glowCy - radius);
         }
      }
   }

   private String getServerStr() {
      return this.client.getCurrentServer() != null && this.client.getCurrentServer().ip != null && !this.client.getCurrentServer().ip.trim().isEmpty()
         ? this.client.getCurrentServer().ip
         : "Local";
   }

   private int getCpuUsage() {
      try {
         if (ManagementFactory.getOperatingSystemMXBean() instanceof OperatingSystemMXBean sunBean) {
            double load = sunBean.getCpuLoad();
            if (load >= 0.0) {
               return (int)Math.round(load * 100.0);
            }
         }
      } catch (Throwable var5) {
      }

      return 18;
   }

   private int getGpuUsage() {
      if (this.client.getFps() > 0) {
         int fps = this.client.getFps();
         return Math.min(99, Math.max(10, 100 - fps / 12));
      } else {
         return 24;
      }
   }

   private int getRamUsage() {
      long max = Runtime.getRuntime().maxMemory();
      long total = Runtime.getRuntime().totalMemory();
      long free = Runtime.getRuntime().freeMemory();
      long used = total - free;
      return max > 0L ? (int)Math.round((double)used / max * 100.0) : 35;
   }

   private int getPing() {
      if (this.client.getConnection() != null && this.client.player != null) {
         PlayerInfo info = this.client.getConnection().getPlayerInfo(this.client.player.getUUID());
         if (info != null) {
            return info.getLatency();
         }
      }

      return 0;
   }

   private int getFps() {
      return this.client.getFps();
   }

   private String getUserName() {
      return this.client.getUser() != null && this.client.getUser().getName() != null ? this.client.getUser().getName() : "User";
   }

   private boolean isAnyScreenOpen() {
      return this.client.gui.screen() != null;
   }

   @Environment(EnvType.CLIENT)
   private static class AnimatedString {
      private String currentStr = "";
      private final long[] animStartTimes = new long[32];
      private final char[] oldChars = new char[32];
      private static final long DURATION = 200L;

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

      public void render(Matrix4f matrix, float startX, float y, Color textColor, MsdfFont font, float fontSize) {
         float currX = startX;
         long now = System.currentTimeMillis();

         for (int i = 0; i < this.currentStr.length(); i++) {
            char cNew = this.currentStr.charAt(i);
            String strNew = String.valueOf(cNew);
            float charW = font.getWidth(strNew, fontSize);
            long elapsed = now - this.animStartTimes[i];
            if (this.oldChars[i] != 0 && elapsed >= 0L && elapsed < 200L) {
               float progress = (float)elapsed / 200.0F;
               float easeOut = 1.0F - (float)Math.pow(1.0F - progress, 3.0);
               char cOld = this.oldChars[i];
               String strOld = String.valueOf(cOld);
               int oldAlpha = Math.max(0, Math.min(255, (int)(255.0F * (1.0F - easeOut))));
               Color oldColor = new Color(255, 255, 255, oldAlpha);
               float oldOffsetY = -4.5F * easeOut;
               Builder.text().font(font).text(strOld).size(fontSize).color(oldColor).build().render(matrix, currX, y + oldOffsetY);
               int newAlpha = Math.max(0, Math.min(255, (int)(255.0F * easeOut)));
               Color newColor = new Color(255, 255, 255, newAlpha);
               float newOffsetY = 4.5F * (1.0F - easeOut);
               Builder.text().font(font).text(strNew).size(fontSize).color(newColor).build().render(matrix, currX, y + newOffsetY);
            } else {
               this.oldChars[i] = 0;
               Builder.text().font(font).text(strNew).size(fontSize).color(textColor).build().render(matrix, currX, y);
            }

            currX += charW;
         }
      }
   }

   @Environment(EnvType.CLIENT)
   public enum SectionType {
      BRAND("Logo", "A"),
      SERVER("Server Address", "E"),
      TIME("Time", "B"),
      PING("Latency", "D"),
      FPS("Framerate", "C"),
      CPU("CPU Load", "G"),
      GPU("GPU Load", "I"),
      RAM("Memory Load", "F"),
      USER("Username", "A");

      public final String title;
      public final String icon;

      SectionType(String title, String icon) {
         this.title = title;
         this.icon = icon;
      }
   }

   @Environment(EnvType.CLIENT)
   private enum SubmenuType {
      NONE,
      ITEMS,
      POSITION;
   }
}
