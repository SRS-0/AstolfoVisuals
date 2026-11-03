package xyz.angames.astolfoclient.client.render.compat;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.platform.*;
import com.mojang.blaze3d.textures.GpuTextureView;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.util.Identifier;
import org.joml.Matrix4f;

/** Translates legacy effect state into immutable Blaze3D pipeline descriptions. */
public final class RenderSystem {
    static boolean blend, depth = true, depthWrite = true, cull = true;
    static DepthTestFunction depthFunction = DepthTestFunction.LEQUAL_DEPTH_TEST;
    static BlendFunction blendFunction = BlendFunction.TRANSLUCENT;
    static final float[] color = {1,1,1,1};
    record TextureBinding(GpuTextureView view, net.minecraft.client.gl.GpuSampler sampler) {}
    static final Map<Integer, TextureBinding> textures = new HashMap<>();
    static net.minecraft.client.gl.Framebuffer target;
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
    public static void blendFunc(SourceFactor src, DestFactor dst) { blendFunction=new BlendFunction(src,dst); }
    public static void blendFuncSeparate(SourceFactor src, DestFactor dst, SourceFactor srcAlpha, DestFactor dstAlpha) { blendFunction=new BlendFunction(src,dst,srcAlpha,dstAlpha); }
    public static void depthFunc(int value) { depthFunction=switch(value) {case 513 -> DepthTestFunction.LESS_DEPTH_TEST; case 514 -> DepthTestFunction.EQUAL_DEPTH_TEST; case 516 -> DepthTestFunction.GREATER_DEPTH_TEST; case 519 -> DepthTestFunction.NO_DEPTH_TEST; default -> DepthTestFunction.LEQUAL_DEPTH_TEST;}; }
    public static void setShaderColor(float r,float g,float b,float a) {color[0]=r;color[1]=g;color[2]=b;color[3]=a;}
    public static ShaderProgram setShader(ShaderProgramKey key) { return program=programs.computeIfAbsent(key,ShaderProgram::new); }
    public static void setShaderTexture(int slot,Identifier texture) { setShaderTexture(slot,MinecraftClient.getInstance().getTextureManager().getTexture(texture)); }
    public static void setShaderTexture(int slot,AbstractTexture texture) { textures.put(slot,new TextureBinding(texture.getGlTextureView(),texture.getSampler())); }
    public static void setShaderTexture(int slot,GpuTextureView texture,net.minecraft.client.gl.GpuSampler sampler) {textures.put(slot,new TextureBinding(texture,sampler));}
    public static void lineWidth(float value) {lineWidth=value;}
    public static boolean isOnRenderThread() {return com.mojang.blaze3d.systems.RenderSystem.isOnRenderThread();}
    public static void assertOnRenderThread() {com.mojang.blaze3d.systems.RenderSystem.assertOnRenderThread();}
    public static Matrix4f getModelViewMatrix() {return com.mojang.blaze3d.systems.RenderSystem.getModelViewMatrix();}
    public static Matrix4f getProjectionMatrix() {var client=MinecraftClient.getInstance();return client.gameRenderer.getBasicProjectionMatrix(client.options.getFov().getValue().floatValue());}
}
