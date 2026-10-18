package dev.sxmurxy.mre.builders.impl;

import com.mojang.renderpearl.api.textures.GpuSampler;
import com.mojang.renderpearl.api.textures.GpuTextureView;

import dev.sxmurxy.mre.builders.AbstractBuilder;
import dev.sxmurxy.mre.builders.states.QuadColorState;
import dev.sxmurxy.mre.builders.states.QuadRadiusState;
import dev.sxmurxy.mre.builders.states.SizeState;
import dev.sxmurxy.mre.renderers.impl.BuiltTexture;
import net.minecraft.client.renderer.texture.AbstractTexture;

public final class TextureBuilder extends AbstractBuilder<BuiltTexture> {

    private SizeState size;
    private QuadRadiusState radius;
    private QuadColorState color;
    private float smoothness;
    private float u, v;
    private float texWidth, texHeight;
    private GpuTextureView textureView;
    private GpuSampler sampler;

    public TextureBuilder size(SizeState size) {
        this.size = size;
        return this;
    }

    public TextureBuilder radius(QuadRadiusState radius) {
        this.radius = radius;
        return this;
    }

    public TextureBuilder color(QuadColorState color) {
        this.color = color;
        return this;
    }

    public TextureBuilder smoothness(float smoothness) {
        this.smoothness = smoothness;
        return this;
    }

    public TextureBuilder texture(float u, float v, float texWidth, float texHeight, AbstractTexture texture) {
        return texture(u, v, texWidth, texHeight, texture.getTextureView(), texture.getSampler());
    }

    public TextureBuilder texture(float u, float v, float texWidth, float texHeight, GpuTextureView textureView, GpuSampler sampler) {
        this.u = u;
        this.v = v;
        this.texWidth = texWidth;
        this.texHeight = texHeight;
        this.textureView = textureView;
        this.sampler = sampler;
        return this;
    }

    @Override
    protected BuiltTexture _build() {
        return new BuiltTexture(
            this.size,
            this.radius,
            this.color,
            this.smoothness,
            this.u, this.v,
            this.texWidth, this.texHeight,
            this.textureView,
            this.sampler
        );
    }

    @Override
    protected void reset() {
        this.size = SizeState.NONE;
        this.radius = QuadRadiusState.NO_ROUND;
        this.color = QuadColorState.WHITE;
        this.smoothness = 1.0f;
        this.u = 0.0f;
        this.v = 0.0f;
        this.texWidth = 0.0f;
        this.texHeight = 0.0f;
        this.textureView = null;
        this.sampler = null;
    }

}