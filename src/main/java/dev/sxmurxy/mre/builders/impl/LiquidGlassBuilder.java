package dev.sxmurxy.mre.builders.impl;

import java.awt.Color;

import dev.sxmurxy.mre.builders.AbstractBuilder;
import dev.sxmurxy.mre.builders.states.QuadColorState;
import dev.sxmurxy.mre.builders.states.QuadRadiusState;
import dev.sxmurxy.mre.builders.states.SizeState;
import dev.sxmurxy.mre.renderers.impl.BuiltLiquidGlass;

public final class LiquidGlassBuilder extends AbstractBuilder<BuiltLiquidGlass> {

    private SizeState size;
    private QuadRadiusState radius;
    private QuadColorState color;
    private float smoothness;
    private float cornerSmoothness;
    private float globalAlpha;
    private float baseAlpha;
    private float fresnelPower;
    private int fresnelColor;
    private float fresnelAlpha;
    private float fresnelMix;
    private boolean fresnelInvert;
    private float distortStrength;

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

    /** The backdrop is always captured; kept for source compatibility. */
    public LiquidGlassBuilder captureBackground() {
        return this;
    }

    @Override
    protected BuiltLiquidGlass _build() {
        return new BuiltLiquidGlass(
            this.size, this.radius, this.color, this.smoothness, this.cornerSmoothness, this.globalAlpha,
            this.baseAlpha, this.fresnelPower, this.fresnelColor, this.fresnelAlpha, this.fresnelMix,
            this.fresnelInvert, this.distortStrength
        );
    }

    @Override
    protected void reset() {
        this.size = SizeState.NONE;
        this.radius = QuadRadiusState.NO_ROUND;
        this.color = QuadColorState.WHITE;
        this.smoothness = 1.0f;
        this.cornerSmoothness = 2.0f;
        this.globalAlpha = 1.0f;
        this.baseAlpha = 0.2f;
        this.fresnelPower = 2.0f;
        this.fresnelColor = -1;
        this.fresnelAlpha = 0.8f;
        this.fresnelMix = 1.0f;
        this.fresnelInvert = false;
        this.distortStrength = 0.03f;
    }

}
