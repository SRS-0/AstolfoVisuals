package xyz.angames.astolfoclient.client.render.compat;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.platform.*;
import com.mojang.blaze3d.textures.GpuTextureView;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.resources.Identifier;
import org.joml.Matrix4f;

/** Translates legacy effect state into immutable Blaze3D pipeline descriptions. */
public final class RenderSystem {
    static boolean blend, depth = true, depthWrite = true, cull = true;
    static CompareOp depthFunction = CompareOp.GREATER_THAN_OR_EQUAL;
    static BlendFunction blendFunction = BlendFunction.TRANSLUCENT;
    static final float[] color = {1,1,1,1};
    record TextureBinding(GpuTextureView view, com.mojang.blaze3d.textures.GpuSampler sampler) {}
    static final Map<Integer, TextureBinding> textures = new HashMap<>();
    static com.mojang.blaze3d.pipeline.RenderTarget target;
    static float lineWidth=1;
    private static final Map<ShaderProgramKey,ShaderProgram> programs = new HashMap<>();
    static ShaderProgram program;
    private RenderSystem() {}
    public static void enableBlend() { blend=true; }
    public static void disableBlend() { blend=false; }
    public static void enableDepthTest() { depth=true; }
    public static void disableDepthTest() { depth=false; }
    public static void depthMask(boolean value) { depthWrite=value; }
    public static void enableCull() { cull=true; }
    public static void disableCull() { cull=false; }
    public static void defaultBlendFunc() { blendFunction=BlendFunction.TRANSLUCENT; }
    public static void blendFunc(BlendFactor src, BlendFactor dst) { blendFunction=new BlendFunction(src,dst); }
    public static void blendFuncSeparate(BlendFactor src, BlendFactor dst, BlendFactor srcAlpha, BlendFactor dstAlpha) { blendFunction=new BlendFunction(src,dst,srcAlpha,dstAlpha); }
    // Minecraft 26.2 uses reversed depth: legacy comparisons must be inverted.
    public static void depthFunc(int value) { depthFunction=switch(value) {case 512 -> CompareOp.NEVER_PASS; case 513 -> CompareOp.GREATER_THAN; case 514 -> CompareOp.EQUAL; case 516 -> CompareOp.LESS_THAN; case 517 -> CompareOp.NOT_EQUAL; case 518 -> CompareOp.LESS_THAN_OR_EQUAL; case 519 -> CompareOp.ALWAYS_PASS; default -> CompareOp.GREATER_THAN_OR_EQUAL;}; }
    public static void setShaderColor(float r,float g,float b,float a) {color[0]=r;color[1]=g;color[2]=b;color[3]=a;}
    public static ShaderProgram setShader(ShaderProgramKey key) { return program=programs.computeIfAbsent(key,ShaderProgram::new); }
    public static void setShaderTexture(int slot,Identifier texture) { setShaderTexture(slot,Minecraft.getInstance().getTextureManager().getTexture(texture)); }
    public static void setShaderTexture(int slot,AbstractTexture texture) { textures.put(slot,new TextureBinding(texture.getTextureView(),texture.getSampler())); }
    public static void setShaderTexture(int slot,GpuTextureView texture,com.mojang.blaze3d.textures.GpuSampler sampler) {textures.put(slot,new TextureBinding(texture,sampler));}
    public static void lineWidth(float value) {lineWidth=value;}
    public static boolean isOnRenderThread() {return com.mojang.blaze3d.systems.RenderSystem.isOnRenderThread();}
    public static void assertOnRenderThread() {com.mojang.blaze3d.systems.RenderSystem.assertOnRenderThread();}
    public static Matrix4f getModelViewMatrixCopy() {return com.mojang.blaze3d.systems.RenderSystem.getModelViewMatrixCopy();}
    public static Matrix4f getProjectionMatrix() {var client=Minecraft.getInstance();return new Matrix4f(client.gameRenderer.gameRenderState().levelRenderState.cameraRenderState.projectionMatrix);}
}
