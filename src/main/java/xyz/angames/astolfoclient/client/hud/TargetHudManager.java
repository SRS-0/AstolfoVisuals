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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.resources.Identifier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.util.Mth;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.scores.DisplaySlot;
import net.minecraft.world.scores.ReadOnlyScoreInfo;
import org.joml.Matrix4f;
import xyz.angames.astolfoclient.client.config.ThemeManager;
import xyz.angames.astolfoclient.client.gui.HudEditorScreen;
import xyz.angames.astolfoclient.client.gui.clickgui.GuiUtils;
import xyz.angames.astolfoclient.client.module.modules.misc.NameProtectModule;
import xyz.angames.astolfoclient.client.util.TargetUtils;

@Environment(EnvType.CLIENT)
public class TargetHudManager {
   private static final int DISPLAY_DURATION_MS = 250;
   private static final int DAMAGE_ANIM_MS = 300;
   private static final Supplier<MsdfFont> BOLD_FONT = Suppliers.memoize(() -> MsdfFont.builder().atlas("bold").data("bold").build());
   private static final Supplier<MsdfFont> SEMIBOLD_FONT = Suppliers.memoize(() -> MsdfFont.builder().atlas("semibold").data("semibold").build());
   private static final Supplier<MsdfFont> MEDIUM_FONT = Suppliers.memoize(() -> MsdfFont.builder().atlas("medium").data("medium").build());
   private static final Identifier DEFAULT_MOB_ICON = Identifier.fromNamespaceAndPath("astolfoclient", "textures/gui/mob_icon.png");
   private static final Pattern HEALTH_PATTERN = Pattern.compile("(\\d+(\\.\\d+)?)");
   private final float baseWidth = 122.0F;
   private final float baseHeight = 33.0F;
   public float x = 100.0F;
   public float y = 100.0F;
   private LivingEntity currentTarget;
   private long lastHitTime;
   private long lastDamageTime;
   private float animationProgress = 0.0F;
   private float visualHealth = 20.0F;
   private float damageHealth = 20.0F;
   private float visualAbsorption = 0.0F;
   private final TargetHudManager.TextAnimator hpAnimator = new TargetHudManager.TextAnimator();
   private boolean dragging = false;
   private float dragOffsetX;
   private float dragOffsetY;
   private long lastUpdateTimeNs = -1L;

   public void setTarget(LivingEntity target) {
      if (target == null || !TargetUtils.isInvisible(target)) {
         if (this.currentTarget != target) {
            this.currentTarget = target;
            if (target != null) {
               float h = this.getRealHealth(target);
               this.visualHealth = h;
               this.damageHealth = h;
               this.visualAbsorption = target.getAbsorptionAmount();
            }
         }

         this.lastHitTime = (long)(System.nanoTime() / 1000000.0);
         this.animationProgress = Math.max(this.animationProgress, 0.01F);
      }
   }

   public void triggerDamage() {
      this.lastDamageTime = (long)(System.nanoTime() / 1000000.0);
   }

   public void render(GuiGraphicsExtractor context, float tickDelta) {
      Minecraft client = Minecraft.getInstance();
      if (client.level != null) {
         boolean isEditing = client.gui.screen() instanceof HudEditorScreen;
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

         long now = (long)(nowNs / 1000000.0);
         boolean isTargetValid = this.currentTarget != null && this.currentTarget.isAlive() && !TargetUtils.isInvisible(this.currentTarget);
         boolean shouldShow = isTargetValid && now - this.lastHitTime <= 250L || isEditing;
         this.animationProgress = this.animationProgress
            + ((shouldShow ? 1.0F : 0.0F) - this.animationProgress) * (float)(1.0 - Math.exp(-14.0 * deltaSeconds));
         if (this.animationProgress < 0.005F && !shouldShow) {
            this.currentTarget = null;
         } else {
            LivingEntity entityToRender = (LivingEntity)(isEditing ? client.player : this.currentTarget);
            if (entityToRender != null) {
               float realHealth = this.getRealHealth(entityToRender);
               float realAbsorption = entityToRender.getAbsorptionAmount();
               this.visualHealth = this.visualHealth + (realHealth - this.visualHealth) * (float)(1.0 - Math.exp(-12.0 * deltaSeconds));
               this.damageHealth = this.damageHealth + (realHealth - this.damageHealth) * (float)(1.0 - Math.exp(-6.0 * deltaSeconds));
               this.visualAbsorption = this.visualAbsorption + (realAbsorption - this.visualAbsorption) * (float)(1.0 - Math.exp(-12.0 * deltaSeconds));
               String name = NameProtectModule.getProtectedName(entityToRender.getName().getString());
               MsdfFont bold = (MsdfFont)BOLD_FONT.get();
               MsdfFont semibold = (MsdfFont)SEMIBOLD_FONT.get();
               MsdfFont medium = (MsdfFont)MEDIUM_FONT.get();
               float scaleModifier = this.getScaleModifier();
               context.pose().pushMatrix();
               context.pose().translate(this.x, this.y);
               context.pose().scale(scaleModifier, scaleModifier);
               context.pose().translate(-this.x, -this.y);
               float ease = 1.0F - (float)Math.pow(1.0F - this.animationProgress, 3.0);
               float popScale = 0.86F + 0.14F * ease;
               if (popScale < 0.999F) {
                  float cx = this.x + 61.0F;
                  float cy = this.y + 16.5F;
                  context.pose().translate(cx, cy);
                  context.pose().scale(popScale, popScale);
                  context.pose().translate(-cx, -cy);
               }

               Matrix4f matrix = GuiCompat.matrix4f(context);
               Color themeColor = new Color(ThemeManager.getThemedColor(now / 10L));
               int shadowSteps = 12;
               float maxSpread = 7.5F;

               for (int i = shadowSteps - 1; i >= 0; i--) {
                  float progress = (float)i / shadowSteps;
                  float spread = progress * maxSpread;
                  float alphaFactor = (1.0F - progress) * (1.0F - progress);
                  int alpha = (int)(102.0F * alphaFactor * this.animationProgress);
                  if (alpha > 0) {
                     Builder.rectangle()
                        .size(new SizeState(122.0F + spread * 2.0F, 33.0F + spread * 2.0F))
                        .radius(new QuadRadiusState(6.5F + spread))
                        .color(new QuadColorState(new Color(0, 0, 0, alpha)))
                        .build()
                        .render(matrix, this.x - spread, this.y - spread + 0.8F);
                  }
               }

               Builder.rectangle()
                  .size(new SizeState(122.0F, 33.0F))
                  .radius(new QuadRadiusState(6.5F))
                  .color(new QuadColorState(new Color(0, 0, 0, (int)(255.0F * this.animationProgress))))
                  .build()
                  .render(matrix, this.x, this.y);
               long damageElapsed = now - this.lastDamageTime;
               float sX = 0.0F;
               float sY = 0.0F;
               Color headTint = Color.WHITE;
               if (damageElapsed < 300L) {
                  float f = 1.0F - (float)damageElapsed / 300.0F;
                  sX = (float)Math.sin(damageElapsed * 0.6) * 2.5F * f;
                  sY = (float)Math.cos(damageElapsed * 0.6) * 1.5F * f;
                  headTint = new Color(255, (int)this.lerp(255.0F, 60.0F, f), (int)this.lerp(255.0F, 60.0F, f));
               }

               float headSize = 22.0F;
               float headX = this.x + 5.5F + sX;
               float headY = this.y + (33.0F - headSize) / 2.0F + sY;
               float headCenterX = headX + headSize / 2.0F;
               float headCenterY = headY + headSize / 2.0F;
               this.drawIconGlowShadow(matrix, headCenterX, headCenterY, headSize / 2.0F, themeColor, 0.12F * this.animationProgress);
               this.renderEntityHead(client, entityToRender, matrix, headX, headY, headSize, headTint, this.animationProgress);
               float textStartX = this.x + 32.5F;
               float textTopY = this.y + 4.5F;
               float maxNameW = 122.0F - (textStartX - this.x) - 6.0F;
               Color whiteText = GuiUtils.withAlpha(Color.WHITE, this.animationProgress);
               Color grayText = GuiUtils.withAlpha(new Color(160, 160, 165), this.animationProgress);
               String displayName = name;
               if (bold != null && bold.getWidth(displayName, 8.5F) > maxNameW) {
                  while (displayName.length() > 2 && bold.getWidth(displayName + "...", 8.5F) > maxNameW) {
                     displayName = displayName.substring(0, displayName.length() - 1);
                  }

                  displayName = displayName + "...";
               }

               if (bold != null) {
                  Builder.text().font(bold).text(displayName).color(whiteText).size(8.5F).build().render(matrix, textStartX, textTopY);
               }

               String baseHpString = String.format("%.1f HP", this.visualHealth).replace(',', '.');
               this.hpAnimator.update(baseHpString);
               float hpTopY = textTopY + 9.5F;
               if (medium != null) {
                  this.hpAnimator.render(matrix, textStartX, hpTopY, grayText, medium, 7.0F, this.animationProgress);
                  if (this.visualAbsorption > 0.5F) {
                     String absString = String.format(" (+%.1f)", this.visualAbsorption).replace(',', '.');
                     Color goldColor = new Color(255, 215, 0);
                     float baseTextWidth = medium.getWidth(baseHpString, 7.0F);
                     Builder.text()
                        .font(medium)
                        .text(absString)
                        .color(GuiUtils.withAlpha(goldColor, this.animationProgress))
                        .size(7.0F)
                        .build()
                        .render(matrix, textStartX + baseTextWidth, hpTopY);
                  }
               }

               float barWidth = 122.0F - (textStartX - this.x) - 6.0F;
               float barY = hpTopY + 9.0F;
               this.renderHpBar(matrix, textStartX, barY, barWidth, 3.2F, themeColor, this.animationProgress, entityToRender.getMaxHealth());
               context.pose().popMatrix();
            }
         }
      }
   }

   public float getRealHealth(LivingEntity entity) {
      if (entity != null && Minecraft.getInstance().level != null) {
         Scoreboard scoreboard = Minecraft.getInstance().level.getScoreboard();
         Objective objective = scoreboard.getDisplayObjective(DisplaySlot.BELOW_NAME);
         if (objective != null) {
            ReadOnlyScoreInfo score = scoreboard.getPlayerScoreInfo(entity, objective);
            if (score != null) {
               return score.value();
            }
         }

         if (entity instanceof Player player) {
            PlayerInfo entry = Minecraft.getInstance().getConnection().getPlayerInfo(player.getUUID());
            if (entry != null && entry.getTeam() != null) {
               String suffix = entry.getTeam().getPlayerSuffix().getString();
               float extracted = this.extractHealthFromString(suffix);
               if (extracted != -1.0F) {
                  return extracted;
               }
            }
         }

         for (Entity e : Minecraft.getInstance().level.getEntities(entity, entity.getBoundingBox().inflate(4.0, 2.0, 4.0))) {
            if (e.isCustomNameVisible() || e instanceof ArmorStand) {
               float extracted = this.extractHealthFromString(e.getDisplayName().getString());
               if (extracted != -1.0F) {
                  return extracted;
               }
            }
         }

         return entity.getHealth();
      } else {
         return 0.0F;
      }
   }

   private float extractHealthFromString(String text) {
      if (text != null && !text.isEmpty()) {
         String clean = text.replaceAll("(?i)§[0-9A-FK-ORX]", "");
         Matcher matcher = HEALTH_PATTERN.matcher(clean);
         if (matcher.find()) {
            try {
               return Float.parseFloat(matcher.group(1));
            } catch (Exception var5) {
            }
         }

         return -1.0F;
      } else {
         return -1.0F;
      }
   }

   private void renderHpBar(Matrix4f matrix, float bx, float by, float bw, float bh, Color theme, float alpha, float maxHp) {
      Builder.rectangle()
         .size(new SizeState(bw, bh))
         .radius(new QuadRadiusState(bh / 2.0F))
         .color(new QuadColorState(GuiUtils.withAlpha(new Color(18, 18, 22), alpha)))
         .build()
         .render(matrix, bx, by);
      float safeMax = Math.max(maxHp, 20.0F);
      float hpPct = Mth.clamp(this.visualHealth / safeMax, 0.0F, 1.0F);
      float hpWidth = bw * hpPct;
      float absPct = Mth.clamp(this.visualAbsorption / safeMax, 0.0F, 1.0F);
      float absWidth = bw * absPct;
      float dmgPct = Mth.clamp(this.damageHealth / safeMax, 0.0F, 1.0F);
      float dmgWidth = bw * dmgPct;
      if (hpWidth > 2.0F) {
         this.drawBarGlowShadow(matrix, bx, by, hpWidth, bh, bh / 2.0F, theme, 0.2F * alpha);
      }

      if (absWidth > 2.0F) {
         this.drawBarGlowShadow(matrix, bx, by, absWidth, bh, bh / 2.0F, new Color(255, 215, 0), 0.28F * alpha);
      }

      if (dmgWidth > hpWidth + 0.5F) {
         Builder.rectangle()
            .size(new SizeState(dmgWidth, bh))
            .radius(new QuadRadiusState(bh / 2.0F))
            .color(new QuadColorState(GuiUtils.withAlpha(new Color(235, 60, 60), 0.8F * alpha)))
            .build()
            .render(matrix, bx, by);
      }

      if (hpWidth > 0.01F) {
         Color themeLeft = theme;
         Color themeRight = GuiUtils.interpolateColor(theme, new Color(180, 50, 255), 0.3F);
         float normalHpRadius = Math.min(bh / 2.0F, hpWidth / 2.0F);
         Builder.rectangle()
            .size(new SizeState(hpWidth, bh))
            .radius(new QuadRadiusState(normalHpRadius))
            .color(
               new QuadColorState(
                  GuiUtils.withAlpha(themeLeft, alpha),
                  GuiUtils.withAlpha(themeRight, alpha),
                  GuiUtils.withAlpha(themeRight, alpha),
                  GuiUtils.withAlpha(themeLeft, alpha)
               )
            )
            .build()
            .render(matrix, bx, by);
      }

      if (absWidth > 0.01F) {
         float absHpRadius = Math.min(bh / 2.0F, absWidth / 2.0F);
         Color goldLeft = new Color(255, 220, 40);
         Color goldRight = new Color(255, 160, 10);
         Builder.rectangle()
            .size(new SizeState(absWidth, bh))
            .radius(new QuadRadiusState(absHpRadius))
            .color(
               new QuadColorState(
                  GuiUtils.withAlpha(goldLeft, alpha),
                  GuiUtils.withAlpha(goldRight, alpha),
                  GuiUtils.withAlpha(goldRight, alpha),
                  GuiUtils.withAlpha(goldLeft, alpha)
               )
            )
            .build()
            .render(matrix, bx, by);
      }
   }

   private void renderEntityHead(Minecraft client, LivingEntity entity, Matrix4f matrix, float hX, float hY, float size, Color tint, float alpha) {
      if (entity instanceof AbstractClientPlayer player) {
         AbstractTexture skin = client.getTextureManager().getTexture(player.getSkin().body().texturePath());
         Builder.texture()
            .size(new SizeState(size, size))
            .radius(new QuadRadiusState(4.0F))
            .texture(0.125F, 0.125F, 0.125F, 0.125F, skin)
            .color(new QuadColorState(GuiUtils.withAlpha(tint, alpha)))
            .build()
            .render(matrix, hX, hY);
         Builder.texture()
            .size(new SizeState(size, size))
            .radius(new QuadRadiusState(4.0F))
            .texture(0.625F, 0.125F, 0.125F, 0.125F, skin)
            .color(new QuadColorState(GuiUtils.withAlpha(tint, alpha * 0.95F)))
            .build()
            .render(matrix, hX, hY);
      } else {
         AbstractTexture mob = client.getTextureManager().getTexture(DEFAULT_MOB_ICON);
         Builder.texture()
            .size(new SizeState(size, size))
            .radius(new QuadRadiusState(4.0F))
            .texture(0.0F, 0.0F, 1.0F, 1.0F, mob)
            .color(new QuadColorState(GuiUtils.withAlpha(tint, alpha)))
            .build()
            .render(matrix, hX, hY);
      }
   }

   private void drawBarGlowShadow(Matrix4f matrix, float x, float y, float w, float h, float radius, Color color, float masterAlpha) {
      if (!(masterAlpha <= 0.001F)) {
         int steps = 6;
         float maxSpread = 4.5F;
         int cr = color.getRed();
         int cg = color.getGreen();
         int cb = color.getBlue();

         for (int i = steps - 1; i >= 0; i--) {
            float progress = (float)i / steps;
            float spread = progress * maxSpread;
            float alphaFactor = (1.0F - progress) * (1.0F - progress);
            int alpha = (int)(255.0F * alphaFactor * masterAlpha);
            if (alpha > 0) {
               Builder.rectangle()
                  .size(new SizeState(w + spread * 2.0F, h + spread * 2.0F))
                  .radius(new QuadRadiusState(radius + spread))
                  .color(new QuadColorState(new Color(cr, cg, cb, alpha)))
                  .build()
                  .render(matrix, x - spread, y - spread + 0.5F);
            }
         }
      }
   }

   private void drawIconGlowShadow(Matrix4f matrix, float cx, float cy, float radius, Color color, float masterAlpha) {
      if (!(masterAlpha <= 0.001F)) {
         int steps = 10;
         float maxSpread = 7.0F;
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
      Minecraft client = Minecraft.getInstance();
      double currentGuiScale = client.getWindow().getGuiScale();
      if (currentGuiScale <= 0.0) {
         currentGuiScale = 2.0;
      }

      return (float)(2.0 / currentGuiScale);
   }

   private float lerp(float start, float end, float delta) {
      return start + (end - start) * Mth.clamp(delta, 0.0F, 1.0F);
   }

   public boolean onMouseClicked(double mouseX, double mouseY, int button) {
      float scaleModifier = this.getScaleModifier();
      float effectiveW = 122.0F * scaleModifier;
      float effectiveH = 33.0F * scaleModifier;
      if (button == 0 && mouseX >= this.x && mouseX <= this.x + effectiveW && mouseY >= this.y && mouseY <= this.y + effectiveH) {
         this.dragging = true;
         this.dragOffsetX = (float)(mouseX - this.x);
         this.dragOffsetY = (float)(mouseY - this.y);
         return true;
      } else {
         return false;
      }
   }

   public boolean onMouseDragged(double mouseX, double mouseY, int button) {
      if (this.dragging && button == 0) {
         Minecraft mc = Minecraft.getInstance();
         float scaleModifier = this.getScaleModifier();
         float screenW = mc.getWindow().getGuiScaledWidth();
         float screenH = mc.getWindow().getGuiScaledHeight();
         float effectiveW = 122.0F * scaleModifier;
         float effectiveH = 33.0F * scaleModifier;
         float targetX = (float)(mouseX - this.dragOffsetX);
         float targetY = (float)(mouseY - this.dragOffsetY);
         this.x = Math.max(0.0F, Math.min(Math.max(0.0F, screenW - effectiveW), targetX));
         this.y = Math.max(0.0F, Math.min(Math.max(0.0F, screenH - effectiveH), targetY));
         return true;
      } else {
         return false;
      }
   }

   public boolean onMouseReleased(double mouseX, double mouseY, int button) {
      if (this.dragging && button == 0) {
         this.dragging = false;
         return true;
      } else {
         return false;
      }
   }

   public boolean onMouseReleased(int button) {
      if (this.dragging && button == 0) {
         this.dragging = false;
         return true;
      } else {
         return false;
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
      return 122.0F;
   }

   public float getHeight() {
      return 33.0F;
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
