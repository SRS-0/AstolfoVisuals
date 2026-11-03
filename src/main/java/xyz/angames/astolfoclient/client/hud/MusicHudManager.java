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
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;
import xyz.angames.astolfoclient.client.AstolfoclientClient;
import xyz.angames.astolfoclient.client.config.ThemeManager;
import xyz.angames.astolfoclient.client.gui.HudEditorScreen;
import xyz.angames.astolfoclient.client.gui.clickgui.GuiUtils;
import xyz.angames.astolfoclient.client.hud.MusicTracker;
import xyz.angames.astolfoclient.client.module.modules.render.InterfaceModule;

@Environment(value=EnvType.CLIENT)
public class MusicHudManager {
    private static final Supplier<MsdfFont> BOLD_FONT = Suppliers.memoize(() -> MsdfFont.builder().atlas("bold").data("bold").build());
    private static final Supplier<MsdfFont> SEMIBOLD_FONT = Suppliers.memoize(() -> MsdfFont.builder().atlas("semibold").data("semibold").build());
    private static final Supplier<MsdfFont> MEDIUM_FONT = Suppliers.memoize(() -> MsdfFont.builder().atlas("medium").data("medium").build());
    private static final Identifier LOGO_TEXTURE = Identifier.fromNamespaceAndPath((String)"astolfoclient", (String)"textures/gui/logo.png");
    public float x = 10.0f;
    public float y = 150.0f;
    private boolean dragging = false;
    private float dragOffsetX = 0.0f;
    private float dragOffsetY = 0.0f;
    private final float collapsedWidth = 69.0f;
    private final float expWidth = 91.0f;
    private final float collapsedHeight = 14.0f;
    private float currentWidth = 69.0f;
    private float currentHeight = 14.0f;
    private float totalHeight = 14.0f;
    private float lastFrameTotalMaxWidth = 69.0f;
    private float animationProgress = 0.0f;
    private float extendingProgress = 0.0f;
    private float hoverPause = 0.0f;
    private final float[] waveHeights = new float[4];
    private boolean isExtended = false;
    private boolean showLyrics = false;
    private int lyricsOffset = 0;
    private long lastUpdateTimeNs = -1L;

    public void render(GuiGraphicsExtractor context, float tickDelta) {
        AbstractTexture artTexture;
        Minecraft client = Minecraft.getInstance();
        if (client.level == null || AstolfoclientClient.moduleManager == null) {
            return;
        }
        InterfaceModule interfaceMod = (InterfaceModule)AstolfoclientClient.moduleManager.getModuleByName("Interface");
        if (interfaceMod == null) {
            return;
        }
        boolean isSettingEnabled = interfaceMod.musicHud.get();
        boolean isEditing = client.gui.screen() instanceof HudEditorScreen;
        boolean shouldShow = interfaceMod.isEnabled() && isSettingEnabled || isEditing;
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
        float interpolationSpeed = 6.5f;
        this.animationProgress += ((shouldShow ? 1.0f : 0.0f) - this.animationProgress) * (float)(1.0 - Math.exp((double)(-interpolationSpeed) * deltaSeconds));
        if (this.animationProgress < 0.01f && !shouldShow) {
            MusicTracker.cleanupArtwork();
            return;
        }
        boolean hasSession = MusicTracker.haveActiveSession();
        if (!hasSession && !isEditing) {
            this.animationProgress += (0.0f - this.animationProgress) * (float)(1.0 - Math.exp((double)(-interpolationSpeed) * deltaSeconds));
            if (this.animationProgress < 0.01f) {
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
        this.extendingProgress += ((this.isExtended ? 1.0f : 0.0f) - this.extendingProgress) * (float)(1.0 - Math.exp((double)(-interpolationSpeed) * deltaSeconds));
        float expHeight = this.showLyrics ? 82.0f : 54.0f;
        float targetWidth = this.lerp(69.0f, 91.0f, this.extendingProgress);
        float targetHeight = this.lerp(14.0f, expHeight, this.extendingProgress);
        this.currentWidth += (targetWidth - this.currentWidth) * (float)(1.0 - Math.exp((double)(-interpolationSpeed) * deltaSeconds));
        this.currentHeight += (targetHeight - this.currentHeight) * (float)(1.0 - Math.exp((double)(-interpolationSpeed) * deltaSeconds));
        float extendingRatio = 0.0f;
        if (this.currentWidth > 69.0f) {
            extendingRatio = (this.currentWidth - 69.0f) / 22.0f;
        }
        extendingRatio = Mth.clamp((float)extendingRatio, (float)0.0f, (float)1.0f);
        float scaleModifier = this.getScaleModifier();
        context.pose().pushMatrix();
        context.pose().translate(this.x, this.y);
        context.pose().scale(scaleModifier, scaleModifier);
        context.pose().translate(-this.x, -this.y);
        Matrix4f matrix = xyz.angames.astolfoclient.client.render.GuiTransforms.matrix(context.pose());
        long themeTime = (long)((double)nowNs / 1000000.0);
        Color themeColor = new Color(ThemeManager.getThemedColor(themeTime / 10L));
        Color whiteText = GuiUtils.withAlpha(Color.WHITE, this.animationProgress);
        Color grayText = GuiUtils.withAlpha(new Color(160, 160, 165), this.animationProgress);
        Color barBgColor = GuiUtils.withAlpha(new Color(24, 24, 28), this.animationProgress);
        float radius = 4.2f;
        this.renderShadow(matrix, this.x, this.y, this.currentWidth, this.currentHeight, radius, this.animationProgress);
        ((BuiltRectangle)Builder.rectangle().size(new SizeState(this.currentWidth, this.currentHeight)).radius(new QuadRadiusState(radius)).color(new QuadColorState(new Color(0, 0, 0, (int)(255.0f * this.animationProgress)))).build()).render(matrix, this.x, this.y);
        float artworkSize = this.lerp(9.0f, 18.0f, extendingRatio);
        float artworkX = this.lerp(this.x + 3.0f, this.x + this.currentWidth / 2.0f - artworkSize / 2.0f, extendingRatio);
        float artworkY = this.lerp(this.y + 7.0f - artworkSize / 2.0f, this.y + 4.5f, extendingRatio);
        float artCenterX = artworkX + artworkSize / 2.0f;
        float artCenterY = artworkY + artworkSize / 2.0f;
        this.drawIconGlowShadow(matrix, artCenterX, artCenterY, artworkSize / 2.0f, themeColor, 0.12f * this.animationProgress);
        Identifier artIdent = MusicTracker.getArtworkTexture(artworkBytes);
        if (artIdent == null) {
            artIdent = LOGO_TEXTURE;
        }
        if ((artTexture = client.getTextureManager().getTexture(artIdent)) != null) {
            ((BuiltTexture)Builder.texture().size(new SizeState(artworkSize, artworkSize)).radius(new QuadRadiusState(this.lerp(2.2f, 3.2f, extendingRatio))).texture(0.0f, 0.0f, 1.0f, 1.0f, artTexture).color(new QuadColorState(GuiUtils.withAlpha(Color.WHITE, this.animationProgress))).build()).render(matrix, artworkX, artworkY);
        }
        MsdfFont semibold = (MsdfFont)SEMIBOLD_FONT.get();
        MsdfFont bold = (MsdfFont)BOLD_FONT.get();
        MsdfFont medium = (MsdfFont)MEDIUM_FONT.get();
        float titleSize = this.lerp(5.5f, 6.2f, extendingRatio);
        float artistSize = 4.8f;
        float maxTextW = this.lerp(43.0f, this.currentWidth - 10.0f, extendingRatio);
        float textX = this.lerp(this.x + 14.5f, this.x + 5.0f, extendingRatio);
        float titleY = this.lerp(this.y + 7.0f - 2.0f, this.y + 24.5f, extendingRatio);
        if (title == null) {
            title = "";
        }
        if (artist == null) {
            artist = "";
        }
        this.drawClippedText(bold != null ? bold : semibold, matrix, title, textX, titleY, whiteText, titleSize, maxTextW);
        if (extendingRatio > 0.4f) {
            float artistAlpha = Mth.clamp((float)((extendingRatio - 0.4f) / 0.6f), (float)0.0f, (float)1.0f) * this.animationProgress;
            Color artistColor = GuiUtils.withAlpha(grayText, artistAlpha);
            this.drawClippedText(medium != null ? medium : semibold, matrix, artist, textX, this.y + 31.5f, artistColor, artistSize, maxTextW);
        }
        float waveStartX = this.lerp(this.x + 69.0f - 10.0f, this.x + this.currentWidth - 12.0f, extendingRatio);
        for (int i = 0; i < 4; ++i) {
            float phase = (float)themeTime * 0.008f + (float)i * 0.7f;
            float targetH = isPlaying ? (float)(1.5 + Math.abs(Math.sin(phase)) * 5.0) : 2.0f;
            this.waveHeights[i] = this.waveHeights[i] + (targetH - this.waveHeights[i]) * (float)(1.0 - Math.exp(-15.0 * deltaSeconds));
            float barX = waveStartX + (float)i * this.lerp(1.4f, 2.0f, extendingRatio);
            float barY = this.lerp(this.y + 7.0f - this.waveHeights[i] / 2.0f, this.y + 4.5f, extendingRatio);
            float barW = this.lerp(1.0f, 1.2f, extendingRatio);
            ((BuiltRectangle)Builder.rectangle().size(new SizeState(barW, this.waveHeights[i])).radius(new QuadRadiusState(0.5f)).color(new QuadColorState(GuiUtils.withAlpha(themeColor, this.animationProgress))).build()).render(matrix, barX, barY);
        }
        if (extendingRatio > 0.6f) {
            float expAlpha = Mth.clamp((float)((extendingRatio - 0.6f) / 0.4f), (float)0.0f, (float)1.0f) * this.animationProgress;
            float barWidth = this.lerp(0.0f, 78.0f, extendingRatio);
            float barX = this.x + (this.currentWidth - barWidth) / 2.0f;
            float barY = this.y + this.currentHeight - 15.0f;
            float barH = 1.6f;
            ((BuiltRectangle)Builder.rectangle().size(new SizeState(barWidth, barH)).radius(new QuadRadiusState(0.8f)).color(new QuadColorState(GuiUtils.withAlpha(barBgColor, expAlpha))).build()).render(matrix, barX, barY);
            float fillRatio = (float)(position / (double)duration);
            if (fillRatio > 0.01f) {
                float fillW = barWidth * Math.min(1.0f, fillRatio);
                Color fillThemeColor = GuiUtils.withAlpha(themeColor, expAlpha);
                Color fillThemeColor2 = GuiUtils.withAlpha(new Color(ThemeManager.getThemedColor(themeTime / 10L + 120L)), expAlpha);
                ((BuiltRectangle)Builder.rectangle().size(new SizeState(fillW, barH)).radius(new QuadRadiusState(0.8f)).color(new QuadColorState(fillThemeColor, fillThemeColor2, fillThemeColor2, fillThemeColor)).build()).render(matrix, barX, barY);
            }
            String elapsedStr = this.formatTime((long)position);
            String durationStr = this.formatTime(duration);
            float timeSize = 4.0f;
            float timeY = this.y + this.currentHeight - 12.0f;
            if (medium != null) {
                ((BuiltText)Builder.text().font(medium).text(elapsedStr).color(GuiUtils.withAlpha(grayText, expAlpha)).size(timeSize).build()).render(matrix, barX, timeY);
                float totalTimeW = medium.getWidth(durationStr, timeSize);
                ((BuiltText)Builder.text().font(medium).text(durationStr).color(GuiUtils.withAlpha(grayText, expAlpha)).size(timeSize).build()).render(matrix, barX + barWidth - totalTimeW, timeY);
            }
        }
        if (extendingRatio > 0.7f) {
            float screenMouseY;
            float mouseScaledY;
            float controlAlpha = (extendingRatio - 0.7f) / 0.3f * this.animationProgress;
            float playX = this.x + this.currentWidth / 2.0f - 5.0f;
            float buttonY = this.y + this.currentHeight - 9.5f;
            float screenMouseX = (float)(client.mouseHandler.xpos() * (double)client.getWindow().getGuiScaledWidth() / (double)client.getWindow().getScreenWidth());
            float mouseScaledX = (screenMouseX - this.x) / scaleModifier + this.x;
            boolean isPlayHovered = this.isHovered(playX, buttonY, 10.0, 9.0, mouseScaledX, mouseScaledY = ((screenMouseY = (float)(client.mouseHandler.ypos() * (double)client.getWindow().getGuiScaledHeight() / (double)client.getWindow().getScreenHeight())) - this.y) / scaleModifier + this.y);
            this.hoverPause += ((isPlayHovered ? 1.0f : 0.0f) - this.hoverPause) * (float)(1.0 - Math.exp(-15.0 * deltaSeconds));
            Color playColor = GuiUtils.withAlpha(whiteText, (0.6f + 0.4f * this.hoverPause) * controlAlpha);
            String playSymbol = isPlaying ? "\u23f8" : "\u25b6";
            context.pose().pushMatrix();
            context.pose().translate(playX + 5.0f, buttonY + 4.5f);
            context.pose().scale(0.85f, 0.85f);
            context.pose().translate(-(playX + 5.0f), -(buttonY + 4.5f));
            float symW = client.font.width(playSymbol);
            context.text(client.font, playSymbol, (int)(playX + 5.0f - symW / 2.0f), (int)(buttonY + 1.5f), playColor.getRGB(), false);
            context.pose().popMatrix();
            float repX = this.x + 10.0f;
            boolean isRepHovered = this.isHovered(repX, buttonY, 8.0, 8.0, mouseScaledX, mouseScaledY);
            Color repColor = MusicTracker.getCycle() > 0 ? themeColor : grayText;
            repColor = GuiUtils.withAlpha(repColor, (0.7f + 0.3f * (isRepHovered ? 1.0f : 0.0f)) * controlAlpha);
            String repText = MusicTracker.getCycle() == 2 ? "\u27f3\u00b9" : "\u27f3";
            context.pose().pushMatrix();
            context.pose().translate(repX + 4.0f, buttonY + 4.5f);
            context.pose().scale(0.8f, 0.8f);
            context.pose().translate(-(repX + 4.0f), -(buttonY + 4.5f));
            float repW = client.font.width(repText);
            context.text(client.font, repText, (int)(repX + 4.0f - repW / 2.0f), (int)(buttonY + 1.5f), repColor.getRGB(), false);
            context.pose().popMatrix();
            float lyrX = this.x + this.currentWidth - 18.0f;
            boolean isLyrHovered = this.isHovered(lyrX, buttonY, 8.0, 8.0, mouseScaledX, mouseScaledY);
            Color lyrColor = this.showLyrics ? themeColor : grayText;
            lyrColor = GuiUtils.withAlpha(lyrColor, (0.7f + 0.3f * (isLyrHovered ? 1.0f : 0.0f)) * controlAlpha);
            String lyrText = "\ud83d\udcdd";
            context.pose().pushMatrix();
            context.pose().translate(lyrX + 4.0f, buttonY + 4.5f);
            context.pose().scale(0.8f, 0.8f);
            context.pose().translate(-(lyrX + 4.0f), -(buttonY + 4.5f));
            float lyrW = client.font.width(lyrText);
            context.text(client.font, lyrText, (int)(lyrX + 4.0f - lyrW / 2.0f), (int)(buttonY + 1.5f), lyrColor.getRGB(), false);
            context.pose().popMatrix();
            if (medium != null) {
                ((BuiltText)Builder.text().font(medium).text(owner).color(GuiUtils.withAlpha(grayText, 0.6f * controlAlpha)).size(4.0f).build()).render(matrix, this.x + 5.0f, this.y + 5.0f);
            }
        }
        if (this.showLyrics && this.extendingProgress > 0.8f) {
            float lyricsAlpha = (this.extendingProgress - 0.8f) / 0.2f * this.animationProgress;
            String lyrics = MusicTracker.getLyrics(artist, title);
            if (lyrics != null && !lyrics.isEmpty() && medium != null) {
                int maxLines;
                String[] lines = lyrics.split("\\n");
                if (this.lyricsOffset > lines.length - (maxLines = Math.min(6, lines.length))) {
                    this.lyricsOffset = Math.max(lines.length - maxLines, 0);
                }
                for (int i = 0; i < maxLines && i + this.lyricsOffset < lines.length; ++i) {
                    String line = lines[i + this.lyricsOffset].trim();
                    float lineW = medium.getWidth(line, 4.2f);
                    float lineX = this.x + this.currentWidth / 2.0f - lineW / 2.0f;
                    float lineY = this.y + 36.0f + (float)i * 5.5f;
                    if (!(lineY + 4.2f < this.y + this.currentHeight - 16.0f)) continue;
                    ((BuiltText)Builder.text().font(medium).text(line).color(GuiUtils.withAlpha(whiteText, lyricsAlpha)).size(4.2f).build()).render(matrix, lineX, lineY);
                }
            }
        }
        this.totalHeight = this.currentHeight;
        this.lastFrameTotalMaxWidth = this.currentWidth;
        context.pose().popMatrix();
    }

    private void drawClippedText(MsdfFont font, Matrix4f matrix, String text, float tx, float ty, Color color, float size, float maxW) {
        if (text == null) {
            text = "";
        }
        if (font == null) {
            return;
        }
        String truncatedText = this.truncateToFit(font, text, maxW, size);
        float textW = font.getWidth(truncatedText, size);
        float drawX = tx + (maxW - textW) / 2.0f;
        ((BuiltText)Builder.text().font(font).text(truncatedText).color(color).size(size).build()).render(matrix, drawX, ty);
    }

    private String truncateToFit(MsdfFont font, String text, float maxW, float size) {
        if (text == null || text.isEmpty()) {
            return "";
        }
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
                continue;
            }
            high = mid - 1;
        }
        return text.substring(0, bestLength) + dots;
    }

    private boolean isHovered(double rx, double ry, double rw, double rh, double mx, double my) {
        return mx >= rx && mx <= rx + rw && my >= ry && my <= ry + rh;
    }

    private float lerp(float start, float end, float delta) {
        return start + (end - start) * Mth.clamp((float)delta, (float)0.0f, (float)1.0f);
    }

    private String formatTime(long totalSeconds) {
        long clamped = Math.max(0L, totalSeconds);
        long minutes = clamped / 60L;
        long seconds = clamped % 60L;
        return String.format("%d:%02d", minutes, seconds);
    }

    private void renderShadow(Matrix4f matrix, float x, float y, float w, float h, float radius, float masterAlpha) {
        int layers = 12;
        float maxSpread = 5.0f;
        for (int i = layers - 1; i >= 0; --i) {
            float progress = (float)i / (float)layers;
            float spread = progress * maxSpread;
            float alphaFactor = (1.0f - progress) * (1.0f - progress);
            int alpha = (int)(102.0f * alphaFactor * masterAlpha);
            if (alpha <= 0) continue;
            ((BuiltRectangle)Builder.rectangle().size(new SizeState(w + spread * 2.0f, h + spread * 2.0f)).radius(new QuadRadiusState(radius + spread)).color(new QuadColorState(new Color(0, 0, 0, alpha))).build()).render(matrix, x - spread, y - spread + 0.6f);
        }
    }

    private void drawIconGlowShadow(Matrix4f matrix, float cx, float cy, float radius, Color color, float masterAlpha) {
        if (masterAlpha <= 0.001f) {
            return;
        }
        int steps = 10;
        float maxSpread = 5.0f;
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

    public boolean onMouseClicked(double mouseX, double mouseY, int button) {
        boolean isSettingEnabled;
        Minecraft mc = Minecraft.getInstance();
        boolean isEditing = mc.gui.screen() instanceof HudEditorScreen;
        InterfaceModule interfaceMod = (InterfaceModule)AstolfoclientClient.moduleManager.getModuleByName("Interface");
        boolean bl = isSettingEnabled = interfaceMod != null && interfaceMod.musicHud.get();
        if (!(isEditing || interfaceMod != null && isSettingEnabled && interfaceMod.isEnabled())) {
            return false;
        }
        float scaleModifier = this.getScaleModifier();
        float effectiveW = this.lastFrameTotalMaxWidth * scaleModifier;
        float effectiveH = this.totalHeight * scaleModifier;
        if (mouseX >= (double)this.x && mouseX <= (double)(this.x + effectiveW) && mouseY >= (double)this.y && mouseY <= (double)(this.y + effectiveH)) {
            double adjX = (mouseX - (double)this.x) / (double)scaleModifier + (double)this.x;
            double adjY = (mouseY - (double)this.y) / (double)scaleModifier + (double)this.y;
            if (button == 0) {
                if (this.extendingProgress > 0.7f) {
                    float playX = this.x + this.currentWidth / 2.0f - 5.0f;
                    float buttonY = this.y + this.currentHeight - 9.5f;
                    if (this.isHovered(playX, buttonY, 10.0, 9.0, adjX, adjY)) {
                        MusicTracker.playPause();
                        return true;
                    }
                    float repX = this.x + 10.0f;
                    if (this.isHovered(repX, buttonY, 8.0, 8.0, adjX, adjY)) {
                        MusicTracker.swapCycle();
                        return true;
                    }
                    float lyrX = this.x + this.currentWidth - 18.0f;
                    if (this.isHovered(lyrX, buttonY, 8.0, 8.0, adjX, adjY)) {
                        boolean bl2 = this.showLyrics = !this.showLyrics;
                        if (this.showLyrics) {
                            this.lyricsOffset = 0;
                        }
                        return true;
                    }
                }
                this.dragging = true;
                this.dragOffsetX = (float)(mouseX - (double)this.x);
                this.dragOffsetY = (float)(mouseY - (double)this.y);
                return true;
            }
            if (button == 1) {
                this.isExtended = !this.isExtended;
                return true;
            }
        }
        return false;
    }

    public void onMouseDragged(double mouseX, double mouseY, int button) {
        if (this.dragging && button == 0) {
            Minecraft mc = Minecraft.getInstance();
            float scaleModifier = this.getScaleModifier();
            float screenW = mc.getWindow().getGuiScaledWidth();
            float screenH = mc.getWindow().getGuiScaledHeight();
            float effectiveW = this.lastFrameTotalMaxWidth * scaleModifier;
            float effectiveH = this.totalHeight * scaleModifier;
            float targetX = (float)(mouseX - (double)this.dragOffsetX);
            float targetY = (float)(mouseY - (double)this.dragOffsetY);
            this.x = Math.max(0.0f, Math.min(Math.max(0.0f, screenW - effectiveW), targetX));
            this.y = Math.max(0.0f, Math.min(Math.max(0.0f, screenH - effectiveH), targetY));
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
                ++this.lyricsOffset;
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

