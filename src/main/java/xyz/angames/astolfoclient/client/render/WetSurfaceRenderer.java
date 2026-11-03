package xyz.angames.astolfoclient.client.render;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
public final class WetSurfaceRenderer {
 private static final WetSurfaceRenderer INSTANCE=new WetSurfaceRenderer();
 private WetSurfaceRenderer(){} public static WetSurfaceRenderer getInstance(){return INSTANCE;}
 public void apply(Minecraft mc,Camera camera,Matrix4f viewMatrix,Matrix4f projectionMatrix,Parameters parameters){
 if(mc==null||mc.level==null||mc.player==null||camera==null||parameters==null||parameters.wetness<=0.0001f||parameters.reflectionStrength<=0.0001f)return;
 var target=mc.gameRenderer.mainRenderTarget();if(target.getDepthTextureView()==null)return;int width=target.width,height=target.height;
 var inverseProjection=new Matrix4f(projectionMatrix).invert();var inverseView=new Matrix4f(viewMatrix).invert();Vec3 cameraPos=camera.position();inverseView.m30((float)cameraPos.x).m31((float)cameraPos.y).m32((float)cameraPos.z);parameters.cameraX=(float)cameraPos.x;parameters.cameraY=(float)cameraPos.y;parameters.cameraZ=(float)cameraPos.z;
 var scene=dev.sxmurxy.mre.renderers.GpuSceneCopy.capture();var pass=new xyz.angames.astolfoclient.client.render.compat.ScreenPass("wet_surface_gpu").texture(0,scene.getColorTextureView(),true).texture(1,target.getDepthTextureView(),false);
         pass.uniform("u_Resolution", width, height);
        pass.uniform("u_ViewMatrix", viewMatrix.get(new float[16]));
        pass.uniform("u_ProjectionMatrix", projectionMatrix.get(new float[16]));
        pass.uniform("u_InverseProjectionMatrix", inverseProjection.get(new float[16]));
        pass.uniform("u_InverseViewMatrix", inverseView.get(new float[16]));
        pass.uniform("u_SunDir", parameters.sunDirX, parameters.sunDirY, parameters.sunDirZ);
        pass.uniform("u_CameraPos", parameters.cameraX, parameters.cameraY, parameters.cameraZ);
        pass.uniform("u_Time", (float)(System.currentTimeMillis() % 1000000L) / 1000.0f);
        pass.uniform("u_Wetness", WetSurfaceRenderer.clamp(parameters.wetness, 0.0f, 2.0f));
        pass.uniform("u_PuddleCoverage", WetSurfaceRenderer.clamp(parameters.puddleCoverage, 0.0f, 1.0f));
        pass.uniform("u_ReflectionStrength", WetSurfaceRenderer.clamp(parameters.reflectionStrength, 0.0f, 2.5f));
        pass.uniform("u_MaxDistance", WetSurfaceRenderer.clamp(parameters.maxDistance, 4.0f, 128.0f));
        pass.uniform("u_RippleStrength", WetSurfaceRenderer.clamp(parameters.rippleStrength, 0.0f, 2.0f));
        pass.uniform("u_RainAmount", WetSurfaceRenderer.clamp(parameters.rainAmount, 0.0f, 2.0f));
        pass.uniform("u_AmbientLight", WetSurfaceRenderer.clamp(parameters.ambientLight, 0.1f, 1.5f));
        pass.uniform("u_LightningFlash", WetSurfaceRenderer.clamp(parameters.lightningFlash, 0.0f, 1.5f));
        pass.uniform("u_ReflectionSteps", Math.max(4, Math.min(16, parameters.reflectionSteps)));
        pass.uniform("u_Ripples", parameters.ripples ? 1 : 0);
        pass.uniform("u_ThemeColor", parameters.themeR, parameters.themeG, parameters.themeB);
        pass.uniform("u_UseThemeColor", parameters.useThemeColor ? 1 : 0);
 pass.draw(target);
 }
 public void reset(){ /* All per-frame inputs are refreshed by apply. */ }
 public void close(){ /* The shared scene-copy target is released at client shutdown. */ }
 private static float clamp(float value,float min,float max){return Math.max(min,Math.min(max,value));}
 public static final class Parameters {
        public float wetness = 1.0f;
        public float puddleCoverage = 0.82f;
        public float reflectionStrength = 1.25f;
        public float maxDistance = 56.0f;
        public float rippleStrength = 0.8f;
        public float rainAmount = 1.0f;
        public int reflectionSteps = 10;
        public boolean ripples = true;
        public float themeR = 1.0f;
        public float themeG = 1.0f;
        public float themeB = 1.0f;
        public boolean useThemeColor = false;
        public float sunDirX = 0.0f;
        public float sunDirY = 1.0f;
        public float sunDirZ = 0.0f;
        public float ambientLight = 1.0f;
        public float lightningFlash = 0.0f;
        public float cameraX = 0.0f;
        public float cameraY = 0.0f;
        public float cameraZ = 0.0f;
    }
}
