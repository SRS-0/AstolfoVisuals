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
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.scores.DisplaySlot;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.ReadOnlyScoreInfo;
import net.minecraft.world.scores.ScoreHolder;
import net.minecraft.world.scores.Scoreboard;
import org.joml.Matrix4f;
import xyz.angames.astolfoclient.client.config.ThemeManager;
import xyz.angames.astolfoclient.client.gui.HudEditorScreen;
import xyz.angames.astolfoclient.client.gui.clickgui.GuiUtils;
import xyz.angames.astolfoclient.client.module.modules.misc.NameProtectModule;
import xyz.angames.astolfoclient.client.util.TargetUtils;

@Environment(value=EnvType.CLIENT)
public class TargetHudManager {
    private static final int DISPLAY_DURATION_MS = 250;
    private static final int DAMAGE_ANIM_MS = 300;
    private static final Supplier<MsdfFont> BOLD_FONT = Suppliers.memoize(() -> MsdfFont.builder().atlas("bold").data("bold").build());
    private static final Supplier<MsdfFont> SEMIBOLD_FONT = Suppliers.memoize(() -> MsdfFont.builder().atlas("semibold").data("semibold").build());
    private static final Supplier<MsdfFont> MEDIUM_FONT = Suppliers.memoize(() -> MsdfFont.builder().atlas("medium").data("medium").build());
    private static final Identifier DEFAULT_MOB_ICON = Identifier.fromNamespaceAndPath((String)"astolfoclient", (String)"textures/gui/mob_icon.png");
    private static final Pattern HEALTH_PATTERN = Pattern.compile("(\\d+(\\.\\d+)?)");
    private final float baseWidth = 122.0f;
    private final float baseHeight = 33.0f;
    public float x = 100.0f;
    public float y = 100.0f;
    private LivingEntity currentTarget;
    private long lastHitTime;
    private long lastDamageTime;
    private float animationProgress = 0.0f;
    private float visualHealth = 20.0f;
    private float damageHealth = 20.0f;
    private float visualAbsorption = 0.0f;
    private final TextAnimator hpAnimator = new TextAnimator();
    private boolean dragging = false;
    private float dragOffsetX;
    private float dragOffsetY;
    private long lastUpdateTimeNs = -1L;

    public void setTarget(LivingEntity target) {
        if (target != null && TargetUtils.isInvisible((Entity)target)) {
            return;
        }
        if (this.currentTarget != target) {
            this.currentTarget = target;
            if (target != null) {
                float h;
                this.visualHealth = h = this.getRealHealth(target);
                this.damageHealth = h;
                this.visualAbsorption = target.getAbsorptionAmount();
            }
        }
        this.lastHitTime = (long)((double)System.nanoTime() / 1000000.0);
        this.animationProgress = Math.max(this.animationProgress, 0.01f);
    }

    public void triggerDamage() {
        this.lastDamageTime = (long)((double)System.nanoTime() / 1000000.0);
    }

    public void render(GuiGraphicsExtractor context, float tickDelta) {
        LivingEntity entityToRender;
        Minecraft client = Minecraft.getInstance();
        if (client.level == null) {
            return;
        }
        boolean isEditing = client.gui.screen() instanceof HudEditorScreen;
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
        long now = (long)((double)nowNs / 1000000.0);
        boolean isTargetValid = this.currentTarget != null && this.currentTarget.isAlive() && !TargetUtils.isInvisible((Entity)this.currentTarget);
        boolean shouldShow = isTargetValid && now - this.lastHitTime <= 250L || isEditing;
        this.animationProgress += ((shouldShow ? 1.0f : 0.0f) - this.animationProgress) * (float)(1.0 - Math.exp(-14.0 * deltaSeconds));
        if (this.animationProgress < 0.005f && !shouldShow) {
            this.currentTarget = null;
            return;
        }
        entityToRender = isEditing ? client.player : this.currentTarget;
        if (entityToRender == null) {
            return;
        }
        float realHealth = this.getRealHealth((LivingEntity)entityToRender);
        float realAbsorption = entityToRender.getAbsorptionAmount();
        this.visualHealth += (realHealth - this.visualHealth) * (float)(1.0 - Math.exp(-12.0 * deltaSeconds));
        this.damageHealth += (realHealth - this.damageHealth) * (float)(1.0 - Math.exp(-6.0 * deltaSeconds));
        this.visualAbsorption += (realAbsorption - this.visualAbsorption) * (float)(1.0 - Math.exp(-12.0 * deltaSeconds));
        String name = NameProtectModule.getProtectedName(entityToRender.getName().getString());
        MsdfFont bold = (MsdfFont)BOLD_FONT.get();
        MsdfFont semibold = (MsdfFont)SEMIBOLD_FONT.get();
        MsdfFont medium = (MsdfFont)MEDIUM_FONT.get();
        float scaleModifier = this.getScaleModifier();
        context.pose().pushMatrix();
        context.pose().translate(this.x, this.y);
        context.pose().scale(scaleModifier, scaleModifier);
        context.pose().translate(-this.x, -this.y);
        float ease = 1.0f - (float)Math.pow(1.0f - this.animationProgress, 3.0);
        float popScale = 0.86f + 0.14f * ease;
        if (popScale < 0.999f) {
            float cx = this.x + 61.0f;
            float cy = this.y + 16.5f;
            context.pose().translate(cx, cy);
            context.pose().scale(popScale, popScale);
            context.pose().translate(-cx, -cy);
        }
        Matrix4f matrix = xyz.angames.astolfoclient.client.render.GuiTransforms.matrix(context.pose());
        Color themeColor = new Color(ThemeManager.getThemedColor(now / 10L));
        int shadowSteps = 12;
        float maxSpread = 7.5f;
        for (int i = shadowSteps - 1; i >= 0; --i) {
            float progress = (float)i / (float)shadowSteps;
            float spread = progress * maxSpread;
            float alphaFactor = (1.0f - progress) * (1.0f - progress);
            int alpha = (int)(102.0f * alphaFactor * this.animationProgress);
            if (alpha <= 0) continue;
            ((BuiltRectangle)Builder.rectangle().size(new SizeState(122.0f + spread * 2.0f, 33.0f + spread * 2.0f)).radius(new QuadRadiusState(6.5f + spread)).color(new QuadColorState(new Color(0, 0, 0, alpha))).build()).render(matrix, this.x - spread, this.y - spread + 0.8f);
        }
        ((BuiltRectangle)Builder.rectangle().size(new SizeState(122.0f, 33.0f)).radius(new QuadRadiusState(6.5f)).color(new QuadColorState(new Color(0, 0, 0, (int)(255.0f * this.animationProgress)))).build()).render(matrix, this.x, this.y);
        long damageElapsed = now - this.lastDamageTime;
        float sX = 0.0f;
        float sY = 0.0f;
        Color headTint = Color.WHITE;
        if (damageElapsed < 300L) {
            float f = 1.0f - (float)damageElapsed / 300.0f;
            sX = (float)Math.sin((double)damageElapsed * 0.6) * 2.5f * f;
            sY = (float)Math.cos((double)damageElapsed * 0.6) * 1.5f * f;
            headTint = new Color(255, (int)this.lerp(255.0f, 60.0f, f), (int)this.lerp(255.0f, 60.0f, f));
        }
        float headSize = 22.0f;
        float headX = this.x + 5.5f + sX;
        float headY = this.y + (33.0f - headSize) / 2.0f + sY;
        float headCenterX = headX + headSize / 2.0f;
        float headCenterY = headY + headSize / 2.0f;
        this.drawIconGlowShadow(matrix, headCenterX, headCenterY, headSize / 2.0f, themeColor, 0.12f * this.animationProgress);
        this.renderEntityHead(client, (LivingEntity)entityToRender, matrix, headX, headY, headSize, headTint, this.animationProgress);
        float textStartX = this.x + 32.5f;
        float textTopY = this.y + 4.5f;
        float maxNameW = 122.0f - (textStartX - this.x) - 6.0f;
        Color whiteText = GuiUtils.withAlpha(Color.WHITE, this.animationProgress);
        Color grayText = GuiUtils.withAlpha(new Color(160, 160, 165), this.animationProgress);
        Object displayName = name;
        if (bold != null && bold.getWidth((String)displayName, 8.5f) > maxNameW) {
            while (((String)displayName).length() > 2 && bold.getWidth((String)displayName + "...", 8.5f) > maxNameW) {
                displayName = ((String)displayName).substring(0, ((String)displayName).length() - 1);
            }
            displayName = (String)displayName + "...";
        }
        if (bold != null) {
            ((BuiltText)Builder.text().font(bold).text((String)displayName).color(whiteText).size(8.5f).build()).render(matrix, textStartX, textTopY);
        }
        String baseHpString = String.format("%.1f HP", Float.valueOf(this.visualHealth)).replace(',', '.');
        this.hpAnimator.update(baseHpString);
        float hpTopY = textTopY + 9.5f;
        if (medium != null) {
            this.hpAnimator.render(matrix, textStartX, hpTopY, grayText, medium, 7.0f, this.animationProgress);
            if (this.visualAbsorption > 0.5f) {
                String absString = String.format(" (+%.1f)", Float.valueOf(this.visualAbsorption)).replace(',', '.');
                Color goldColor = new Color(255, 215, 0);
                float baseTextWidth = medium.getWidth(baseHpString, 7.0f);
                ((BuiltText)Builder.text().font(medium).text(absString).color(GuiUtils.withAlpha(goldColor, this.animationProgress)).size(7.0f).build()).render(matrix, textStartX + baseTextWidth, hpTopY);
            }
        }
        float barWidth = 122.0f - (textStartX - this.x) - 6.0f;
        float barY = hpTopY + 9.0f;
        this.renderHpBar(matrix, textStartX, barY, barWidth, 3.2f, themeColor, this.animationProgress, entityToRender.getMaxHealth());
        context.pose().popMatrix();
    }

    public float getRealHealth(LivingEntity entity) {
        float extracted;
        ReadOnlyScoreInfo score;
        if (entity == null || Minecraft.getInstance().level == null) {
            return 0.0f;
        }
        Scoreboard scoreboard = Minecraft.getInstance().level.getScoreboard();
        Objective objective = scoreboard.getDisplayObjective(DisplaySlot.BELOW_NAME);
        if (objective != null && (score = scoreboard.getPlayerScoreInfo((ScoreHolder)entity, objective)) != null) {
            return score.value();
        }
        if (entity instanceof Player) {
            String suffix;
            Player player = (Player)entity;
            PlayerInfo entry = Minecraft.getInstance().getConnection().getPlayerInfo(player.getUUID());
            if (entry != null && entry.getTeam() != null && (extracted = this.extractHealthFromString(suffix = entry.getTeam().getPlayerSuffix().getString())) != -1.0f) {
                return extracted;
            }
        }
        List<Entity> nearby = Minecraft.getInstance().level.getEntities((Entity)entity, entity.getBoundingBox().inflate(4.0, 2.0, 4.0));
        for (Entity e : nearby) {
            if (!e.isCustomNameVisible() && !(e instanceof ArmorStand) || (extracted = this.extractHealthFromString(e.getDisplayName().getString())) == -1.0f) continue;
            return extracted;
        }
        return entity.getHealth();
    }

    private float extractHealthFromString(String text) {
        if (text == null || text.isEmpty()) {
            return -1.0f;
        }
        String clean = text.replaceAll("(?i)\u00a7[0-9A-FK-ORX]", "");
        Matcher matcher = HEALTH_PATTERN.matcher(clean);
        if (matcher.find()) {
            try {
                return Float.parseFloat(matcher.group(1));
            }
            catch (Exception exception) {
                // empty catch block
            }
        }
        return -1.0f;
    }

    private void renderHpBar(Matrix4f matrix, float bx, float by, float bw, float bh, Color theme, float alpha, float maxHp) {
        ((BuiltRectangle)Builder.rectangle().size(new SizeState(bw, bh)).radius(new QuadRadiusState(bh / 2.0f)).color(new QuadColorState(GuiUtils.withAlpha(new Color(18, 18, 22), alpha))).build()).render(matrix, bx, by);
        float safeMax = Math.max(maxHp, 20.0f);
        float hpPct = Mth.clamp((float)(this.visualHealth / safeMax), (float)0.0f, (float)1.0f);
        float hpWidth = bw * hpPct;
        float absPct = Mth.clamp((float)(this.visualAbsorption / safeMax), (float)0.0f, (float)1.0f);
        float absWidth = bw * absPct;
        float dmgPct = Mth.clamp((float)(this.damageHealth / safeMax), (float)0.0f, (float)1.0f);
        float dmgWidth = bw * dmgPct;
        if (hpWidth > 2.0f) {
            this.drawBarGlowShadow(matrix, bx, by, hpWidth, bh, bh / 2.0f, theme, 0.2f * alpha);
        }
        if (absWidth > 2.0f) {
            this.drawBarGlowShadow(matrix, bx, by, absWidth, bh, bh / 2.0f, new Color(255, 215, 0), 0.28f * alpha);
        }
        if (dmgWidth > hpWidth + 0.5f) {
            ((BuiltRectangle)Builder.rectangle().size(new SizeState(dmgWidth, bh)).radius(new QuadRadiusState(bh / 2.0f)).color(new QuadColorState(GuiUtils.withAlpha(new Color(235, 60, 60), 0.8f * alpha))).build()).render(matrix, bx, by);
        }
        if (hpWidth > 0.01f) {
            Color themeLeft = theme;
            Color themeRight = GuiUtils.interpolateColor(theme, new Color(180, 50, 255), 0.3f);
            float normalHpRadius = Math.min(bh / 2.0f, hpWidth / 2.0f);
            ((BuiltRectangle)Builder.rectangle().size(new SizeState(hpWidth, bh)).radius(new QuadRadiusState(normalHpRadius)).color(new QuadColorState(GuiUtils.withAlpha(themeLeft, alpha), GuiUtils.withAlpha(themeRight, alpha), GuiUtils.withAlpha(themeRight, alpha), GuiUtils.withAlpha(themeLeft, alpha))).build()).render(matrix, bx, by);
        }
        if (absWidth > 0.01f) {
            float absHpRadius = Math.min(bh / 2.0f, absWidth / 2.0f);
            Color goldLeft = new Color(255, 220, 40);
            Color goldRight = new Color(255, 160, 10);
            ((BuiltRectangle)Builder.rectangle().size(new SizeState(absWidth, bh)).radius(new QuadRadiusState(absHpRadius)).color(new QuadColorState(GuiUtils.withAlpha(goldLeft, alpha), GuiUtils.withAlpha(goldRight, alpha), GuiUtils.withAlpha(goldRight, alpha), GuiUtils.withAlpha(goldLeft, alpha))).build()).render(matrix, bx, by);
        }
    }

    private void renderEntityHead(Minecraft client, LivingEntity entity, Matrix4f matrix, float hX, float hY, float size, Color tint, float alpha) {
        if (entity instanceof AbstractClientPlayer) {
            AbstractClientPlayer player = (AbstractClientPlayer)entity;
            AbstractTexture skin = client.getTextureManager().getTexture(player.getSkin().body().texturePath());
            ((BuiltTexture)Builder.texture().size(new SizeState(size, size)).radius(new QuadRadiusState(4.0f)).texture(0.125f, 0.125f, 0.125f, 0.125f, skin).color(new QuadColorState(GuiUtils.withAlpha(tint, alpha))).build()).render(matrix, hX, hY);
            ((BuiltTexture)Builder.texture().size(new SizeState(size, size)).radius(new QuadRadiusState(4.0f)).texture(0.625f, 0.125f, 0.125f, 0.125f, skin).color(new QuadColorState(GuiUtils.withAlpha(tint, alpha * 0.95f))).build()).render(matrix, hX, hY);
        } else {
            AbstractTexture mob = client.getTextureManager().getTexture(DEFAULT_MOB_ICON);
            ((BuiltTexture)Builder.texture().size(new SizeState(size, size)).radius(new QuadRadiusState(4.0f)).texture(0.0f, 0.0f, 1.0f, 1.0f, mob).color(new QuadColorState(GuiUtils.withAlpha(tint, alpha))).build()).render(matrix, hX, hY);
        }
    }

    private void drawBarGlowShadow(Matrix4f matrix, float x, float y, float w, float h, float radius, Color color, float masterAlpha) {
        if (masterAlpha <= 0.001f) {
            return;
        }
        int steps = 6;
        float maxSpread = 4.5f;
        int cr = color.getRed();
        int cg = color.getGreen();
        int cb = color.getBlue();
        for (int i = steps - 1; i >= 0; --i) {
            float progress = (float)i / (float)steps;
            float spread = progress * maxSpread;
            float alphaFactor = (1.0f - progress) * (1.0f - progress);
            int alpha = (int)(255.0f * alphaFactor * masterAlpha);
            if (alpha <= 0) continue;
            ((BuiltRectangle)Builder.rectangle().size(new SizeState(w + spread * 2.0f, h + spread * 2.0f)).radius(new QuadRadiusState(radius + spread)).color(new QuadColorState(new Color(cr, cg, cb, alpha))).build()).render(matrix, x - spread, y - spread + 0.5f);
        }
    }

    private void drawIconGlowShadow(Matrix4f matrix, float cx, float cy, float radius, Color color, float masterAlpha) {
        if (masterAlpha <= 0.001f) {
            return;
        }
        int steps = 10;
        float maxSpread = 7.0f;
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
        Minecraft client = Minecraft.getInstance();
        double currentGuiScale = client.getWindow().getGuiScale();
        if (currentGuiScale <= 0.0) {
            currentGuiScale = 2.0;
        }
        return (float)(2.0 / currentGuiScale);
    }

    private float lerp(float start, float end, float delta) {
        return start + (end - start) * Mth.clamp((float)delta, (float)0.0f, (float)1.0f);
    }

    public boolean onMouseClicked(double mouseX, double mouseY, int button) {
        float scaleModifier = this.getScaleModifier();
        float effectiveW = 122.0f * scaleModifier;
        float effectiveH = 33.0f * scaleModifier;
        if (button == 0 && mouseX >= (double)this.x && mouseX <= (double)(this.x + effectiveW) && mouseY >= (double)this.y && mouseY <= (double)(this.y + effectiveH)) {
            this.dragging = true;
            this.dragOffsetX = (float)(mouseX - (double)this.x);
            this.dragOffsetY = (float)(mouseY - (double)this.y);
            return true;
        }
        return false;
    }

    public boolean onMouseDragged(double mouseX, double mouseY, int button) {
        if (this.dragging && button == 0) {
            Minecraft mc = Minecraft.getInstance();
            float scaleModifier = this.getScaleModifier();
            float screenW = mc.getWindow().getGuiScaledWidth();
            float screenH = mc.getWindow().getGuiScaledHeight();
            float effectiveW = 122.0f * scaleModifier;
            float effectiveH = 33.0f * scaleModifier;
            float targetX = (float)(mouseX - (double)this.dragOffsetX);
            float targetY = (float)(mouseY - (double)this.dragOffsetY);
            this.x = Math.max(0.0f, Math.min(Math.max(0.0f, screenW - effectiveW), targetX));
            this.y = Math.max(0.0f, Math.min(Math.max(0.0f, screenH - effectiveH), targetY));
            return true;
        }
        return false;
    }

    public boolean onMouseReleased(double mouseX, double mouseY, int button) {
        if (this.dragging && button == 0) {
            this.dragging = false;
            return true;
        }
        return false;
    }

    public boolean onMouseReleased(int button) {
        if (this.dragging && button == 0) {
            this.dragging = false;
            return true;
        }
        return false;
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
        return 122.0f;
    }

    public float getHeight() {
        return 33.0f;
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

