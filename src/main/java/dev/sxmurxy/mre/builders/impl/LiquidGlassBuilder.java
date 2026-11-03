package dev.sxmurxy.mre.builders.impl;

import dev.sxmurxy.mre.builders.AbstractBuilder;
import dev.sxmurxy.mre.builders.states.QuadColorState;
import dev.sxmurxy.mre.builders.states.QuadRadiusState;
import dev.sxmurxy.mre.builders.states.SizeState;
import dev.sxmurxy.mre.renderers.impl.BuiltLiquidGlass;
import java.awt.Color;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.texture.AbstractTexture;

@Environment(value=EnvType.CLIENT)
public final class LiquidGlassBuilder
extends AbstractBuilder<BuiltLiquidGlass> {
    private SizeState size;
    private QuadRadiusState radius;
    private QuadColorState color;
    private float smoothness;
    private float cornerSmoothness;
    private float globalAlpha;
    private float fresnelPower;
    private int fresnelColor;
    private float fresnelAlpha;
    private float baseAlpha;
    private boolean fresnelInvert;
    private float fresnelMix;
    private float distortStrength;
    private boolean useBackground;
    private float u;
    private float v;
    private float texWidth;
    private float texHeight;
    private AbstractTexture textureId;

    public LiquidGlassBuilder size(SizeState size) {
        this.size = size;
        return this;
    }

    public LiquidGlassBuilder radius(QuadRadiusState radius) {
        this.radius = radius;
        return this;
    }

    public LiquidGlassBuilder color(QuadColorState color) {
        this.color = color;
        return this;
    }

    public LiquidGlassBuilder smoothness(float smoothness, float cornerSmoothness) {
        this.smoothness = smoothness;
        this.cornerSmoothness = cornerSmoothness;
        return this;
    }

    public LiquidGlassBuilder alpha(float globalAlpha, float baseAlpha) {
        this.globalAlpha = globalAlpha;
        this.baseAlpha = baseAlpha;
        return this;
    }

    public LiquidGlassBuilder fresnel(float power, Color color, float alpha, float mix, boolean invert) {
        return this.fresnel(power, color.getRGB(), alpha, mix, invert);
    }

    public LiquidGlassBuilder fresnel(float power, int color, float alpha, float mix, boolean invert) {
        this.fresnelPower = power;
        this.fresnelColor = color;
        this.fresnelAlpha = alpha;
        this.fresnelMix = mix;
        this.fresnelInvert = invert;
        return this;
    }

    public LiquidGlassBuilder distortStrength(float distortStrength) {
        this.distortStrength = distortStrength;
        return this;
    }

    public LiquidGlassBuilder captureBackground() {
        this.useBackground = true;
        return this;
    }

public LiquidGlassBuilder texture(float u, float v, float texWidth, float texHeight, AbstractTexture textureId) {
        this.useBackground = false;
        this.u = u;
        this.v = v;
        this.texWidth = texWidth;
        this.texHeight = texHeight;
        this.textureId = textureId;
        return this;
    }

    @Override
    protected BuiltLiquidGlass _build() {
        return new BuiltLiquidGlass(this.size, this.radius, this.color, this.smoothness, this.cornerSmoothness, this.globalAlpha, this.fresnelPower, this.fresnelColor, this.fresnelAlpha, this.baseAlpha, this.fresnelInvert, this.fresnelMix, this.distortStrength, this.useBackground, this.u, this.v, this.texWidth, this.texHeight, this.textureId);
    }

    @Override
    protected void reset() {
        this.size = SizeState.NONE;
        this.radius = QuadRadiusState.NO_ROUND;
        this.color = QuadColorState.WHITE;
        this.smoothness = 1.0f;
        this.cornerSmoothness = 2.0f;
        this.globalAlpha = 1.0f;
        this.fresnelPower = 2.0f;
        this.fresnelColor = -1;
        this.fresnelAlpha = 0.8f;
        this.baseAlpha = 0.2f;
        this.fresnelInvert = false;
        this.fresnelMix = 1.0f;
        this.distortStrength = 0.03f;
        this.useBackground = true;
        this.u = 0.0f;
        this.v = 0.0f;
        this.texWidth = 0.0f;
        this.texHeight = 0.0f;
        this.textureId = null;
    }
}

