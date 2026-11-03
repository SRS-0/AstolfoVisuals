package xyz.angames.astolfoclient.client.module.modules.render;

import xyz.angames.astolfoclient.client.render.compat.RenderSystem;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Framebuffer;
import net.minecraft.client.gl.SimpleFramebuffer;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import xyz.angames.astolfoclient.client.AstolfoclientClient;
import xyz.angames.astolfoclient.client.config.ThemeManager;
import xyz.angames.astolfoclient.client.module.Module;
import xyz.angames.astolfoclient.client.module.setting.BooleanSetting;
import xyz.angames.astolfoclient.client.module.setting.ModeSetting;
import xyz.angames.astolfoclient.client.module.setting.NumberSetting;

@Environment(value=EnvType.CLIENT)
public class ShaderHand
extends Module {
    public static boolean rendering = false;
    public final ModeSetting mode = new ModeSetting("Mode", "Normal", "Normal", "Fill1", "Fill2", "Fill3");
    public final BooleanSetting glass = new BooleanSetting("Glass", true);
    public final NumberSetting glassAlpha = new NumberSetting("Glass Alpha", 0.5, 0.0, 1.0, 0.05);
    public final BooleanSetting blur = new BooleanSetting("Blur Background", true);
    public final NumberSetting blurStrength = new NumberSetting("Blur Strength", 3.0, 0.0, 100.0, 0.5);
    public final BooleanSetting glow = new BooleanSetting("Glow", true);
    public final NumberSetting glowRadius = new NumberSetting("Glow Radius", 3.0, 1.0, 5.0, 1.0);
    public final BooleanSetting outerGlow = new BooleanSetting("Outer Glow", true);
    public final NumberSetting outerExposure = new NumberSetting("Outer Exposure", 2.0, 0.5, 5.0, 0.1);
    public final BooleanSetting innerGlow = new BooleanSetting("Inner Glow", false);
    public final NumberSetting innerExposure = new NumberSetting("Inner Exposure", 2.0, 0.5, 5.0, 0.1);
    public final BooleanSetting handColor = new BooleanSetting("Hand Color", false);
    public final BooleanSetting motion = new BooleanSetting("Motion", false);
    public final NumberSetting motionStrength = new NumberSetting("Motion Strength", 0.8, 0.0, 0.99, 0.05);

 private Framebuffer handsBuffer,motionBuffer,tempMotionBuffer;
 private final List<Framebuffer> bloomBuffers=new ArrayList<>();
 private final MinecraftClient mc=MinecraftClient.getInstance();
 private long lastDrawTime;
 private com.mojang.blaze3d.textures.GpuTextureView previousColor,previousDepth;
 private boolean capturing;
 private static final com.mojang.blaze3d.pipeline.BlendFunction MASKED_GLOW = new com.mojang.blaze3d.pipeline.BlendFunction(com.mojang.blaze3d.platform.SourceFactor.SRC_ALPHA,com.mojang.blaze3d.platform.DestFactor.ONE,com.mojang.blaze3d.platform.SourceFactor.ZERO,com.mojang.blaze3d.platform.DestFactor.ONE);
 public ShaderHand() {super("ShaderHand", Category.RENDER);this.addSettings(mode,glass,glassAlpha,blur,blurStrength,glow,glowRadius,outerGlow,outerExposure,innerGlow,innerExposure,handColor,motion,motionStrength);net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents.CLIENT_STOPPING.register(client->clearBuffers());}
 public static ShaderHand getInstance(){return AstolfoclientClient.moduleManager==null?null:(ShaderHand)AstolfoclientClient.moduleManager.getModuleByName("ShaderHand");}
 public void onEnable(){clearBuffers();} public void onDisable(){clearBuffers();}
 private static void clear(Framebuffer buffer){if(buffer==null)return;var encoder=com.mojang.blaze3d.systems.RenderSystem.getDevice().createCommandEncoder();if(buffer.getDepthAttachment()!=null)encoder.clearColorAndDepthTextures(buffer.getColorAttachment(),0,buffer.getDepthAttachment(),1);else encoder.clearColorTexture(buffer.getColorAttachment(),0);}
 private Framebuffer size(Framebuffer buffer,int width,int height,boolean depth){if(buffer==null)buffer=new SimpleFramebuffer("Astolfo hands",width,height,depth);else if(buffer.textureWidth!=width||buffer.textureHeight!=height){buffer.resize(width,height);clear(buffer);}return buffer;}
 private void clearBuffers(){rendering=false;restoreTarget();if(handsBuffer!=null)handsBuffer.delete();if(motionBuffer!=null)motionBuffer.delete();if(tempMotionBuffer!=null)tempMotionBuffer.delete();bloomBuffers.stream().filter(java.util.Objects::nonNull).forEach(Framebuffer::delete);bloomBuffers.clear();handsBuffer=motionBuffer=tempMotionBuffer=null;lastDrawTime=0;}
 public Framebuffer getHandsBuffer(){handsBuffer=size(handsBuffer,mc.getWindow().getFramebufferWidth(),mc.getWindow().getFramebufferHeight(),true);return handsBuffer;}
 public void beginRender(){var target=getHandsBuffer();clear(target);previousColor=com.mojang.blaze3d.systems.RenderSystem.outputColorTextureOverride;previousDepth=com.mojang.blaze3d.systems.RenderSystem.outputDepthTextureOverride;capturing=true;rendering=true;com.mojang.blaze3d.systems.RenderSystem.outputColorTextureOverride=target.getColorAttachmentView();com.mojang.blaze3d.systems.RenderSystem.outputDepthTextureOverride=target.getDepthAttachmentView();}
 public void restoreTarget(){if(!capturing)return;com.mojang.blaze3d.systems.RenderSystem.outputColorTextureOverride=previousColor;com.mojang.blaze3d.systems.RenderSystem.outputDepthTextureOverride=previousDepth;previousColor=previousDepth=null;capturing=false;}
 private xyz.angames.astolfoclient.client.render.compat.ScreenPass pass(String name){return new xyz.angames.astolfoclient.client.render.compat.ScreenPass("hand_"+name);}
 public void draw(){
 rendering=false;restoreTarget();
 if(handsBuffer==null)return;var main=mc.getFramebuffer();Framebuffer input=handsBuffer;int width=main.textureWidth,height=main.textureHeight;
 if(motion.get()&&motionStrength.getFloat()>0){long now=System.currentTimeMillis();double dt=Math.max(0.001,Math.min(0.1,lastDrawTime==0?0.016:(now-lastDrawTime)/1000.0));boolean reset=lastDrawTime==0||now-lastDrawTime>100;lastDrawTime=now;motionBuffer=size(motionBuffer,width,height,false);tempMotionBuffer=size(tempMotionBuffer,width,height,false);if(reset){clear(motionBuffer);clear(tempMotionBuffer);}float strength=(float)Math.pow(motionStrength.getFloat(),dt*60);
 pass("motion").texture(0,handsBuffer.getColorAttachmentView(),true).texture(1,motionBuffer.getColorAttachmentView(),true).uniform("motionStrength",strength).draw(tempMotionBuffer);var swap=motionBuffer;motionBuffer=tempMotionBuffer;tempMotionBuffer=swap;input=motionBuffer;
 }else {if(motionBuffer!=null)motionBuffer.delete();if(tempMotionBuffer!=null)tempMotionBuffer.delete();motionBuffer=tempMotionBuffer=null;lastDrawTime=0;}
 var background=dev.sxmurxy.mre.renderers.GpuSceneCopy.capture();int color=ThemeManager.getThemedColor(0);float r=((color>>16)&255)/255f,g=((color>>8)&255)/255f,b=(color&255)/255f;
 var blend=(glass.get()||blur.get())?com.mojang.blaze3d.pipeline.BlendFunction.TRANSLUCENT:new com.mojang.blaze3d.pipeline.BlendFunction(com.mojang.blaze3d.platform.SourceFactor.ONE,com.mojang.blaze3d.platform.DestFactor.ONE_MINUS_SRC_ALPHA);
 int fill=mode.is("Fill1")?1:mode.is("Fill2")?2:mode.is("Fill3")?3:0;
 pass("base").texture(0,input.getColorAttachmentView(),true).texture(1,background.getColorAttachmentView(),true).uniform("alpha",glass.get()?glassAlpha.getFloat():1).uniform("doBlur",blur.get()?1:0).uniform("blurStrength",blurStrength.getFloat()).uniform("resolution",width,height).uniform("time",(float)(System.nanoTime()/1e9%100000)).uniform("themeColor",r,g,b).uniform("useHandColor",handColor.get()?1:0).uniform("mode",fill).blend(blend).draw(main,true);
 if(glow.get())renderKawaseBloom(input);
 }
 private void renderKawaseBloom(Framebuffer input){
 int iterations=Math.max(1,Math.min(5,glowRadius.getInt()));int width=mc.getWindow().getFramebufferWidth(),height=mc.getWindow().getFramebufferHeight();
 while(bloomBuffers.size()<iterations)bloomBuffers.add(null);
 Framebuffer source=input;
 for(int i=0;i<iterations;i++){var target=size(bloomBuffers.get(i),Math.max(1,width>>(i+1)),Math.max(1,height>>(i+1)),false);bloomBuffers.set(i,target);pass("kawase_down").texture(0,source.getColorAttachmentView(),true).uniform("uHalfPixel",0.5f/target.textureWidth,0.5f/target.textureHeight).uniform("uOffset",1+i,1+i).draw(target);source=target;}
 for(int i=iterations-2;i>=0;i--){var target=bloomBuffers.get(i);pass("kawase_up").texture(0,source.getColorAttachmentView(),true).uniform("uHalfPixel",0.5f/target.textureWidth,0.5f/target.textureHeight).uniform("uOffset",2+i,2+i).uniform("color",1,1,1).draw(target);source=target;}
 int color=ThemeManager.getThemedColor(0);float r=((color>>16)&255)/255f,g=((color>>8)&255)/255f,b=(color&255)/255f;
 for(boolean inner:new boolean[]{false,true}){if(inner?!innerGlow.get():!outerGlow.get())continue;pass(inner?"inner_glow":"outer_glow").texture(0,source.getColorAttachmentView(),true).texture(1,input.getColorAttachmentView(),true).uniform("glowColor1",r,g,b).uniform("glowColor2",r,g,b).uniform("alpha",inner?innerExposure.getFloat():outerExposure.getFloat()).uniform("useHandColor",handColor.get()?1:0).blend(MASKED_GLOW).draw(mc.getFramebuffer(),true);}
 }
public boolean shouldRender() {
        if (!this.isEnabled()) {
            return false;
        }
        if (this.mc.player == null) {
            return false;
        }
        ItemStack mainHand = this.mc.player.getMainHandStack();
        ItemStack offHand = this.mc.player.getOffHandStack();
        return !this.isMap(mainHand) && !this.isMap(offHand);
    }
private boolean isMap(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        String path = Registries.ITEM.getId(stack.getItem()).getPath();
        return "map".equals(path) || "filled_map".equals(path);
    }
}
