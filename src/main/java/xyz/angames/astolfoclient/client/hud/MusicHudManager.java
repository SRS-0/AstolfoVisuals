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
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;
import xyz.angames.astolfoclient.client.AstolfoclientClient;
import xyz.angames.astolfoclient.client.config.ThemeManager;
import xyz.angames.astolfoclient.client.gui.HudEditorScreen;
import xyz.angames.astolfoclient.client.gui.clickgui.GuiUtils;
import xyz.angames.astolfoclient.client.module.modules.render.InterfaceModule;

@Environment(EnvType.CLIENT)
public class MusicHudManager {
   private static final Supplier<MsdfFont> BOLD_FONT = Suppliers.memoize(() -> MsdfFont.builder().atlas("bold").data("bold").build());
   private static final Supplier<MsdfFont> SEMIBOLD_FONT = Suppliers.memoize(() -> MsdfFont.builder().atlas("semibold").data("semibold").build());
   private static final Supplier<MsdfFont> MEDIUM_FONT = Suppliers.memoize(() -> MsdfFont.builder().atlas("medium").data("medium").build());
   private static final Identifier LOGO_TEXTURE = Identifier.fromNamespaceAndPath("astolfoclient", "textures/gui/logo.png");
   public float x = 10.0F;
   public float y = 150.0F;
   private boolean dragging = false;
   private float dragOffsetX = 0.0F;
   private float dragOffsetY = 0.0F;
   private final float collapsedWidth = 69.0F;
   private final float expWidth = 91.0F;
   private final float collapsedHeight = 14.0F;
   private float currentWidth = 69.0F;
   private float currentHeight = 14.0F;
   private float totalHeight = 14.0F;
   private float lastFrameTotalMaxWidth = 69.0F;
   private float animationProgress = 0.0F;
   private float extendingProgress = 0.0F;
   private float hoverPause = 0.0F;
   private final float[] waveHeights = new float[4];
   private boolean isExtended = false;
   private boolean showLyrics = false;
   private int lyricsOffset = 0;
   private long lastUpdateTimeNs = -1L;

   public void render(GuiGraphicsExtractor context, float tickDelta) {
      Minecraft client = Minecraft.getInstance();
      if (client.level != null && AstolfoclientClient.moduleManager != null) {
         InterfaceModule interfaceMod = (InterfaceModule)AstolfoclientClient.moduleManager.getModuleByName("Interface");
         if (interfaceMod != null) {
            boolean isSettingEnabled = interfaceMod.musicHud.get();
            boolean isEditing = client.gui.screen() instanceof HudEditorScreen;
            boolean shouldShow = interfaceMod.isEnabled() && isSettingEnabled || isEditing;
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

            float interpolationSpeed = 6.5F;
            this.animationProgress = this.animationProgress
               + ((shouldShow ? 1.0F : 0.0F) - this.animationProgress) * (float)(1.0 - Math.exp(-interpolationSpeed * deltaSeconds));
            if (this.animationProgress < 0.01F && !shouldShow) {
               MusicTracker.cleanupArtwork();
            } else {
               boolean hasSession = MusicTracker.haveActiveSession();
               if (!hasSession && !isEditing) {
                  this.animationProgress = this.animationProgress
                     + (0.0F - this.animationProgress) * (float)(1.0 - Math.exp(-interpolationSpeed * deltaSeconds));
                  if (this.animationProgress < 0.01F) {
                     MusicTracker.cleanupArtwork();
                     return;
                  }
               }

               String title = MusicTracker.getTitle();
               String artist = MusicTracker.getArtist();
               double position = MusicTracker.getPosition();
               long duration = MusicTracker.getDuration();
               boolean isPlaying = MusicTracker.isPlaying();
               byte[] artworkBytes = MusicTracker.getArtworkBytes();
               String owner = MusicTracker.getOwner();
               if (isEditing && !hasSession) {
                  title = "Astolfo Reborn Theme";
                  artist = "Astolfo Client";
                  position = 35.0;
                  duration = 180L;
                  isPlaying = true;
                  owner = "spotify";
               }

               if (duration <= 0L) {
                  duration = 1L;
               }

               this.extendingProgress = this.extendingProgress
                  + ((this.isExtended ? 1.0F : 0.0F) - this.extendingProgress) * (float)(1.0 - Math.exp(-interpolationSpeed * deltaSeconds));
               float expHeight = this.showLyrics ? 82.0F : 54.0F;
               float targetWidth = this.lerp(69.0F, 91.0F, this.extendingProgress);
               float targetHeight = this.lerp(14.0F, expHeight, this.extendingProgress);
               this.currentWidth = this.currentWidth + (targetWidth - this.currentWidth) * (float)(1.0 - Math.exp(-interpolationSpeed * deltaSeconds));
               this.currentHeight = this.currentHeight + (targetHeight - this.currentHeight) * (float)(1.0 - Math.exp(-interpolationSpeed * deltaSeconds));
               float extendingRatio = 0.0F;
               if (this.currentWidth > 69.0F) {
                  extendingRatio = (this.currentWidth - 69.0F) / 22.0F;
               }

               extendingRatio = Mth.clamp(extendingRatio, 0.0F, 1.0F);
               float scaleModifier = this.getScaleModifier();
               context.pose().pushMatrix();
               context.pose().translate(this.x, this.y);
               context.pose().scale(scaleModifier, scaleModifier);
               context.pose().translate(-this.x, -this.y);
               Matrix4f matrix = GuiCompat.matrix4f(context);
               long themeTime = (long)(nowNs / 1000000.0);
               Color themeColor = new Color(ThemeManager.getThemedColor(themeTime / 10L));
               Color whiteText = GuiUtils.withAlpha(Color.WHITE, this.animationProgress);
               Color grayText = GuiUtils.withAlpha(new Color(160, 160, 165), this.animationProgress);
               Color barBgColor = GuiUtils.withAlpha(new Color(24, 24, 28), this.animationProgress);
               float radius = 4.2F;
               this.renderShadow(matrix, this.x, this.y, this.currentWidth, this.currentHeight, radius, this.animationProgress);
               Builder.rectangle()
                  .size(new SizeState(this.currentWidth, this.currentHeight))
                  .radius(new QuadRadiusState(radius))
                  .color(new QuadColorState(new Color(0, 0, 0, (int)(255.0F * this.animationProgress))))
                  .build()
                  .render(matrix, this.x, this.y);
               float artworkSize = this.lerp(9.0F, 18.0F, extendingRatio);
               float artworkX = this.lerp(this.x + 3.0F, this.x + this.currentWidth / 2.0F - artworkSize / 2.0F, extendingRatio);
               float artworkY = this.lerp(this.y + 7.0F - artworkSize / 2.0F, this.y + 4.5F, extendingRatio);
               float artCenterX = artworkX + artworkSize / 2.0F;
               float artCenterY = artworkY + artworkSize / 2.0F;
               this.drawIconGlowShadow(matrix, artCenterX, artCenterY, artworkSize / 2.0F, themeColor, 0.12F * this.animationProgress);
               Identifier artIdent = MusicTracker.getArtworkTexture(artworkBytes);
               if (artIdent == null) {
                  artIdent = LOGO_TEXTURE;
               }

               AbstractTexture artTexture = client.getTextureManager().getTexture(artIdent);
               if (artTexture != null) {
                  Builder.texture()
                     .size(new SizeState(artworkSize, artworkSize))
                     .radius(new QuadRadiusState(this.lerp(2.2F, 3.2F, extendingRatio)))
                     .texture(0.0F, 0.0F, 1.0F, 1.0F, artTexture)
                     .color(new QuadColorState(GuiUtils.withAlpha(Color.WHITE, this.animationProgress)))
                     .build()
                     .render(matrix, artworkX, artworkY);
               }

               MsdfFont semibold = (MsdfFont)SEMIBOLD_FONT.get();
               MsdfFont bold = (MsdfFont)BOLD_FONT.get();
               MsdfFont medium = (MsdfFont)MEDIUM_FONT.get();
               float titleSize = this.lerp(5.5F, 6.2F, extendingRatio);
               float artistSize = 4.8F;
               float maxTextW = this.lerp(43.0F, this.currentWidth - 10.0F, extendingRatio);
               float textX = this.lerp(this.x + 14.5F, this.x + 5.0F, extendingRatio);
               float titleY = this.lerp(this.y + 7.0F - 2.0F, this.y + 24.5F, extendingRatio);
               if (title == null) {
                  title = "";
               }

               if (artist == null) {
                  artist = "";
               }

               this.drawClippedText(bold != null ? bold : semibold, matrix, title, textX, titleY, whiteText, titleSize, maxTextW);
               if (extendingRatio > 0.4F) {
                  float artistAlpha = Mth.clamp((extendingRatio - 0.4F) / 0.6F, 0.0F, 1.0F) * this.animationProgress;
                  Color artistColor = GuiUtils.withAlpha(grayText, artistAlpha);
                  this.drawClippedText(medium != null ? medium : semibold, matrix, artist, textX, this.y + 31.5F, artistColor, artistSize, maxTextW);
               }

               float waveStartX = this.lerp(this.x + 69.0F - 10.0F, this.x + this.currentWidth - 12.0F, extendingRatio);

               for (int i = 0; i < 4; i++) {
                  float phase = (float)themeTime * 0.008F + i * 0.7F;
                  float targetH = isPlaying ? (float)(1.5 + Math.abs(Math.sin(phase)) * 5.0) : 2.0F;
                  this.waveHeights[i] = this.waveHeights[i] + (targetH - this.waveHeights[i]) * (float)(1.0 - Math.exp(-15.0 * deltaSeconds));
                  float barX = waveStartX + i * this.lerp(1.4F, 2.0F, extendingRatio);
                  float barY = this.lerp(this.y + 7.0F - this.waveHeights[i] / 2.0F, this.y + 4.5F, extendingRatio);
                  float barW = this.lerp(1.0F, 1.2F, extendingRatio);
                  Builder.rectangle()
                     .size(new SizeState(barW, this.waveHeights[i]))
                     .radius(new QuadRadiusState(0.5F))
                     .color(new QuadColorState(GuiUtils.withAlpha(themeColor, this.animationProgress)))
                     .build()
                     .render(matrix, barX, barY);
               }

               if (extendingRatio > 0.6F) {
                  float expAlpha = Mth.clamp((extendingRatio - 0.6F) / 0.4F, 0.0F, 1.0F) * this.animationProgress;
                  float barWidth = this.lerp(0.0F, 78.0F, extendingRatio);
                  float barX = this.x + (this.currentWidth - barWidth) / 2.0F;
                  float barY = this.y + this.currentHeight - 15.0F;
                  float barH = 1.6F;
                  Builder.rectangle()
                     .size(new SizeState(barWidth, barH))
                     .radius(new QuadRadiusState(0.8F))
                     .color(new QuadColorState(GuiUtils.withAlpha(barBgColor, expAlpha)))
                     .build()
                     .render(matrix, barX, barY);
                  float fillRatio = (float)(position / duration);
                  if (fillRatio > 0.01F) {
                     float fillW = barWidth * Math.min(1.0F, fillRatio);
                     Color fillThemeColor = GuiUtils.withAlpha(themeColor, expAlpha);
                     Color fillThemeColor2 = GuiUtils.withAlpha(new Color(ThemeManager.getThemedColor(themeTime / 10L + 120L)), expAlpha);
                     Builder.rectangle()
                        .size(new SizeState(fillW, barH))
                        .radius(new QuadRadiusState(0.8F))
                        .color(new QuadColorState(fillThemeColor, fillThemeColor2, fillThemeColor2, fillThemeColor))
                        .build()
                        .render(matrix, barX, barY);
                  }

                  String elapsedStr = this.formatTime((long)position);
                  String durationStr = this.formatTime(duration);
                  float timeSize = 4.0F;
                  float timeY = this.y + this.currentHeight - 12.0F;
                  if (medium != null) {
                     Builder.text()
                        .font(medium)
                        .text(elapsedStr)
                        .color(GuiUtils.withAlpha(grayText, expAlpha))
                        .size(timeSize)
                        .build()
                        .render(matrix, barX, timeY);
                     float totalTimeW = medium.getWidth(durationStr, timeSize);
                     Builder.text()
                        .font(medium)
                        .text(durationStr)
                        .color(GuiUtils.withAlpha(grayText, expAlpha))
                        .size(timeSize)
                        .build()
                        .render(matrix, barX + barWidth - totalTimeW, timeY);
                  }
               }

               if (extendingRatio > 0.7F) {
                  float controlAlpha = (extendingRatio - 0.7F) / 0.3F * this.animationProgress;
                  float screenMouseX = (float)(client.mouseHandler.xpos() * client.getWindow().getGuiScaledWidth() / client.getWindow().getScreenWidth());
                  float screenMouseY = (float)(client.mouseHandler.ypos() * client.getWindow().getGuiScaledHeight() / client.getWindow().getScreenHeight());
                  float mouseScaledX = (screenMouseX - this.x) / scaleModifier + this.x;
                  float mouseScaledY = (screenMouseY - this.y) / scaleModifier + this.y;
                  float buttonY = this.y + this.currentHeight - 9.5F;
                  float playX = this.x + this.currentWidth / 2.0F - 5.0F;
                  boolean isPlayHovered = this.isHovered(playX, buttonY, 10.0, 9.0, mouseScaledX, mouseScaledY);
                  this.hoverPause = this.hoverPause + ((isPlayHovered ? 1.0F : 0.0F) - this.hoverPause) * (float)(1.0 - Math.exp(-15.0 * deltaSeconds));
                  Color playColor = GuiUtils.withAlpha(whiteText, (0.6F + 0.4F * this.hoverPause) * controlAlpha);
                  String playSymbol = isPlaying ? "⏸" : "▶";
                  context.pose().pushMatrix();
                  context.pose().translate(playX + 5.0F, buttonY + 4.5F);
                  context.pose().scale(0.85F, 0.85F);
                  context.pose().translate(-(playX + 5.0F), -(buttonY + 4.5F));
                  float symW = client.font.width(playSymbol);
                  context.text(client.font, playSymbol, (int)(playX + 5.0F - symW / 2.0F), (int)(buttonY + 1.5F), playColor.getRGB(), false);
                  context.pose().popMatrix();
                  float repX = this.x + 10.0F;
                  boolean isRepHovered = this.isHovered(repX, buttonY, 8.0, 8.0, mouseScaledX, mouseScaledY);
                  Color repColor = MusicTracker.getCycle() > 0 ? themeColor : grayText;
                  repColor = GuiUtils.withAlpha(repColor, (0.7F + 0.3F * (isRepHovered ? 1.0F : 0.0F)) * controlAlpha);
                  String repText = MusicTracker.getCycle() == 2 ? "⟳¹" : "⟳";
                  context.pose().pushMatrix();
                  context.pose().translate(repX + 4.0F, buttonY + 4.5F);
                  context.pose().scale(0.8F, 0.8F);
                  context.pose().translate(-(repX + 4.0F), -(buttonY + 4.5F));
                  float repW = client.font.width(repText);
                  context.text(client.font, repText, (int)(repX + 4.0F - repW / 2.0F), (int)(buttonY + 1.5F), repColor.getRGB(), false);
                  context.pose().popMatrix();
                  float lyrX = this.x + this.currentWidth - 18.0F;
                  boolean isLyrHovered = this.isHovered(lyrX, buttonY, 8.0, 8.0, mouseScaledX, mouseScaledY);
                  Color lyrColor = this.showLyrics ? themeColor : grayText;
                  lyrColor = GuiUtils.withAlpha(lyrColor, (0.7F + 0.3F * (isLyrHovered ? 1.0F : 0.0F)) * controlAlpha);
                  String lyrText = "\ud83d\udcdd";
                  context.pose().pushMatrix();
                  context.pose().translate(lyrX + 4.0F, buttonY + 4.5F);
                  context.pose().scale(0.8F, 0.8F);
                  context.pose().translate(-(lyrX + 4.0F), -(buttonY + 4.5F));
                  float lyrW = client.font.width(lyrText);
                  context.text(client.font, lyrText, (int)(lyrX + 4.0F - lyrW / 2.0F), (int)(buttonY + 1.5F), lyrColor.getRGB(), false);
                  context.pose().popMatrix();
                  if (medium != null) {
                     Builder.text()
                        .font(medium)
                        .text(owner)
                        .color(GuiUtils.withAlpha(grayText, 0.6F * controlAlpha))
                        .size(4.0F)
                        .build()
                        .render(matrix, this.x + 5.0F, this.y + 5.0F);
                  }
               }

               if (this.showLyrics && this.extendingProgress > 0.8F) {
                  float lyricsAlpha = (this.extendingProgress - 0.8F) / 0.2F * this.animationProgress;
                  String lyrics = MusicTracker.getLyrics(artist, title);
                  if (lyrics != null && !lyrics.isEmpty() && medium != null) {
                     String[] lines = lyrics.split("\\n");
                     int maxLines = Math.min(6, lines.length);
                     if (this.lyricsOffset > lines.length - maxLines) {
                        this.lyricsOffset = Math.max(lines.length - maxLines, 0);
                     }

                     for (int i = 0; i < maxLines && i + this.lyricsOffset < lines.length; i++) {
                        String line = lines[i + this.lyricsOffset].trim();
                        float lineW = medium.getWidth(line, 4.2F);
                        float lineX = this.x + this.currentWidth / 2.0F - lineW / 2.0F;
                        float lineY = this.y + 36.0F + i * 5.5F;
                        if (lineY + 4.2F < this.y + this.currentHeight - 16.0F) {
                           Builder.text()
                              .font(medium)
                              .text(line)
                              .color(GuiUtils.withAlpha(whiteText, lyricsAlpha))
                              .size(4.2F)
                              .build()
                              .render(matrix, lineX, lineY);
                        }
                     }
                  }
               }

               this.totalHeight = this.currentHeight;
               this.lastFrameTotalMaxWidth = this.currentWidth;
               context.pose().popMatrix();
            }
         }
      }
   }

   private void drawClippedText(MsdfFont font, Matrix4f matrix, String text, float tx, float ty, Color color, float size, float maxW) {
      if (text == null) {
         text = "";
      }

      if (font != null) {
         String truncatedText = this.truncateToFit(font, text, maxW, size);
         float textW = font.getWidth(truncatedText, size);
         float drawX = tx + (maxW - textW) / 2.0F;
         Builder.text().font(font).text(truncatedText).color(color).size(size).build().render(matrix, drawX, ty);
      }
   }

   private String truncateToFit(MsdfFont font, String text, float maxW, float size) {
      if (text != null && !text.isEmpty()) {
         float textW = font.getWidth(text, size);
         if (textW <= maxW) {
            return text;
         }

         String dots = "...";
         float dotsW = font.getWidth(dots, size);
         if (dotsW >= maxW) {
            return ".";
         }

         int low = 0;
         int high = text.length();
         int bestLength = 0;

         while (low <= high) {
            int mid = (low + high) / 2;
            String sub = text.substring(0, mid) + dots;
            float subW = font.getWidth(sub, size);
            if (subW <= maxW) {
               bestLength = mid;
               low = mid + 1;
            } else {
               high = mid - 1;
            }
         }

         return text.substring(0, bestLength) + dots;
      } else {
         return "";
      }
   }

   private boolean isHovered(double rx, double ry, double rw, double rh, double mx, double my) {
      return mx >= rx && mx <= rx + rw && my >= ry && my <= ry + rh;
   }

   private float lerp(float start, float end, float delta) {
      return start + (end - start) * Mth.clamp(delta, 0.0F, 1.0F);
   }

   private String formatTime(long totalSeconds) {
      long clamped = Math.max(0L, totalSeconds);
      long minutes = clamped / 60L;
      long seconds = clamped % 60L;
      return String.format("%d:%02d", minutes, seconds);
   }

   private void renderShadow(Matrix4f matrix, float x, float y, float w, float h, float radius, float masterAlpha) {
      int layers = 12;
      float maxSpread = 5.0F;

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
               .render(matrix, x - spread, y - spread + 0.6F);
         }
      }
   }

   private void drawIconGlowShadow(Matrix4f matrix, float cx, float cy, float radius, Color color, float masterAlpha) {
      if (!(masterAlpha <= 0.001F)) {
         int steps = 10;
         float maxSpread = 5.0F;
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

   public boolean onMouseClicked(double mouseX, double mouseY, int button) {
      Minecraft mc = Minecraft.getInstance();
      boolean isEditing = mc.gui.screen() instanceof HudEditorScreen;
      InterfaceModule interfaceMod = (InterfaceModule)AstolfoclientClient.moduleManager.getModuleByName("Interface");
      boolean isSettingEnabled = interfaceMod != null && interfaceMod.musicHud.get();
      if (isEditing || interfaceMod != null && isSettingEnabled && interfaceMod.isEnabled()) {
         float scaleModifier = this.getScaleModifier();
         float effectiveW = this.lastFrameTotalMaxWidth * scaleModifier;
         float effectiveH = this.totalHeight * scaleModifier;
         if (mouseX >= this.x && mouseX <= this.x + effectiveW && mouseY >= this.y && mouseY <= this.y + effectiveH) {
            double adjX = (mouseX - this.x) / scaleModifier + this.x;
            double adjY = (mouseY - this.y) / scaleModifier + this.y;
            if (button == 0) {
               if (this.extendingProgress > 0.7F) {
                  float buttonY = this.y + this.currentHeight - 9.5F;
                  float playX = this.x + this.currentWidth / 2.0F - 5.0F;
                  if (this.isHovered(playX, buttonY, 10.0, 9.0, adjX, adjY)) {
                     MusicTracker.playPause();
                     return true;
                  }

                  float repX = this.x + 10.0F;
                  if (this.isHovered(repX, buttonY, 8.0, 8.0, adjX, adjY)) {
                     MusicTracker.swapCycle();
                     return true;
                  }

                  float lyrX = this.x + this.currentWidth - 18.0F;
                  if (this.isHovered(lyrX, buttonY, 8.0, 8.0, adjX, adjY)) {
                     this.showLyrics = !this.showLyrics;
                     if (this.showLyrics) {
                        this.lyricsOffset = 0;
                     }

                     return true;
                  }
               }

               this.dragging = true;
               this.dragOffsetX = (float)(mouseX - this.x);
               this.dragOffsetY = (float)(mouseY - this.y);
               return true;
            }

            if (button == 1) {
               this.isExtended = !this.isExtended;
               return true;
            }
         }

         return false;
      } else {
         return false;
      }
   }

   public void onMouseDragged(double mouseX, double mouseY, int button) {
      if (this.dragging && button == 0) {
         Minecraft mc = Minecraft.getInstance();
         float scaleModifier = this.getScaleModifier();
         float screenW = mc.getWindow().getGuiScaledWidth();
         float screenH = mc.getWindow().getGuiScaledHeight();
         float effectiveW = this.lastFrameTotalMaxWidth * scaleModifier;
         float effectiveH = this.totalHeight * scaleModifier;
         float targetX = (float)(mouseX - this.dragOffsetX);
         float targetY = (float)(mouseY - this.dragOffsetY);
         this.x = Math.max(0.0F, Math.min(Math.max(0.0F, screenW - effectiveW), targetX));
         this.y = Math.max(0.0F, Math.min(Math.max(0.0F, screenH - effectiveH), targetY));
      }
   }

   public void onMouseReleased(double mouseX, double mouseY, int button) {
      if (this.dragging && button == 0) {
         this.dragging = false;
      }
   }

   public void onMouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
      if (this.showLyrics && this.isExtended) {
         if (verticalAmount > 0.0) {
            this.lyricsOffset = Math.max(0, this.lyricsOffset - 1);
         } else if (verticalAmount < 0.0) {
            this.lyricsOffset++;
         }
      }
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
      return this.lastFrameTotalMaxWidth;
   }

   public float getHeight() {
      return this.totalHeight;
   }
}
