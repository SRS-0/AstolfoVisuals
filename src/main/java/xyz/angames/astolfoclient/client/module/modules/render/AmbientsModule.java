package xyz.angames.astolfoclient.client.module.modules.render;

import xyz.angames.astolfoclient.client.render.compat.RenderSystem;
import com.mojang.blaze3d.pipeline.RenderTarget;
import java.awt.Color;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.FloatBuffer;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Quaternionf;
import org.joml.Quaternionfc;
import org.lwjgl.BufferUtils;
import xyz.angames.astolfoclient.client.config.ThemeManager;
import xyz.angames.astolfoclient.client.module.Module;
import xyz.angames.astolfoclient.client.module.setting.BooleanSetting;
import xyz.angames.astolfoclient.client.module.setting.ModeSetting;
import xyz.angames.astolfoclient.client.module.setting.NumberSetting;

@Environment(value=EnvType.CLIENT)
public class AmbientsModule
extends Module {
    public final BooleanSetting sphereBlur = new BooleanSetting("Sphere Blur", false);
    public final NumberSetting sphereRadius = new NumberSetting("Sphere Radius", 30.0, 3.0, 150.0, 1.0){

        @Override
        public boolean isVisible() {
            return AmbientsModule.this.sphereBlur.get();
        }
    };
    public final NumberSetting blurSmoothness = new NumberSetting("Blur Smoothness", 12.0, 1.0, 60.0, 1.0){

        @Override
        public boolean isVisible() {
            return AmbientsModule.this.sphereBlur.get();
        }
    };
    public final NumberSetting blurStrength = new NumberSetting("Blur Strength", 12.0, 1.0, 50.0, 0.5){

        @Override
        public boolean isVisible() {
            return AmbientsModule.this.sphereBlur.get();
        }
    };
    public final ModeSetting blurQuality = new ModeSetting("Blur Quality", "High", new String[]{"Low", "Medium", "High", "Ultra"}){

        @Override
        public boolean isVisible() {
            return AmbientsModule.this.sphereBlur.get();
        }
    };
    public final BooleanSetting smoothFollow = new BooleanSetting("Smooth Follow", true){

        @Override
        public boolean isVisible() {
            return AmbientsModule.this.sphereBlur.get();
        }
    };
    public final NumberSetting blurFollowSpeed = new NumberSetting("Follow Speed", 8.0, 1.0, 25.0, 0.5){

        @Override
        public boolean isVisible() {
            return AmbientsModule.this.sphereBlur.get() && AmbientsModule.this.smoothFollow.get();
        }
    };
    public final BooleanSetting blurSky = new BooleanSetting("Blur Sky", true){

        @Override
        public boolean isVisible() {
            return AmbientsModule.this.sphereBlur.get();
        }
    };
    public final BooleanSetting blurThemeTint = new BooleanSetting("Theme Tint", false){

        @Override
        public boolean isVisible() {
            return AmbientsModule.this.sphereBlur.get();
        }
    };
    public final NumberSetting blurThemeStrength = new NumberSetting("Tint Strength", 0.15, 0.0, 1.0, 0.05){

        @Override
        public boolean isVisible() {
            return AmbientsModule.this.sphereBlur.get() && AmbientsModule.this.blurThemeTint.get();
        }
    };
    public final BooleanSetting customTime = new BooleanSetting("Custom Time", true);
    public final NumberSetting time = new NumberSetting("Time", 18000.0, 0.0, 24000.0, 100.0);
    public final BooleanSetting customSkybox = new BooleanSetting("Custom Skybox", true);
    public final ModeSetting skyboxMode = new ModeSetting("Mode", "Clouds", "Clouds", "Smoke", "Caustic", "Nebula", "Bright Clouds", "Cloud Space");
    public final NumberSetting skyboxStrength = new NumberSetting("Skybox Strength", 0.6, 0.0, 1.0, 0.05);
    public final BooleanSetting customFog = new BooleanSetting("Custom Fog", true);
    public final NumberSetting fogStart = new NumberSetting("Fog Start", 5.0, 0.0, 100.0, 1.0);
    public final NumberSetting fogEnd = new NumberSetting("Fog End", 50.0, 5.0, 200.0, 1.0);
    public final BooleanSetting fogColorEnabled = new BooleanSetting("Fog Color Enabled", true);
    public final BooleanSetting themeSync = new BooleanSetting("Theme Sync", true);
    public final NumberSetting fogStrength = new NumberSetting("Fog Color Strength", 0.6, 0.0, 1.0, 0.05);
    public final BooleanSetting fogShader = new BooleanSetting("Fog Shader", false);
    public final ModeSetting fogShaderMode = new ModeSetting("Fog Shader Mode", "Mist", "Mist", "Smoke", "Caustic", "Nebula", "Waves", "Abyss");
    public final NumberSetting fogShaderStrength = new NumberSetting("Fog Shader Strength", 0.6, 0.0, 1.0, 0.05);
    public final NumberSetting fogShaderSpeed = new NumberSetting("Fog Shader Speed", 1.0, 0.1, 5.0, 0.1);
    public final NumberSetting fogShaderDensity = new NumberSetting("Fog Shader Density", 1.0, 0.1, 3.0, 0.1);
    public final NumberSetting fogShaderHorizon = new NumberSetting("Fog Shader Horizon", 0.8, 0.0, 2.0, 0.05);
    public final BooleanSetting fogShaderThemeSync = new BooleanSetting("Fog Shader Theme", true);
    public final NumberSetting blockBrightness = new NumberSetting("Block Brightness", 1.0, 0.0, 1.0, 0.01);
    public final NumberSetting saturation = new NumberSetting("Saturation", 1.0, 0.0, 2.0, 0.05);
    private int cloudsProgram = -1;
    private int smokeProgram = -1;
    private int causticProgram = -1;
    private int nebulaProgram = -1;
    private int radiantProgram = -1;
    private int sunsetProgram = -1;
    private int texturedProgram = -1;
    private int fogMistProgram = -1;
    private int fogSmokeProgram = -1;
    private int fogCausticProgram = -1;
    private int fogNebulaProgram = -1;
    private int fogWavesProgram = -1;
    private int fogAbyssProgram = -1;
    private int skyboxVao = -1;
    private int skyboxVbo = -1;
    private int texturedSkyboxVao = -1;
    private int texturedSkyboxVbo = -1;
    private int blackProgram = -1;
    private int sphereBlurProgram = -1;
    private int sphereBlurVao = -1;
    private int sphereBlurVbo = -1;
    private RenderTarget sphereBlurFbo = null;
    private Vec3 smoothSphereCenter = null;
    private long lastSphereUpdateTime = 0L;
    private RenderTarget saturationFbo = null;
    private int saturationProgram = -1;
    private int saturationVao = -1;
    private int saturationVbo = -1;

    public AmbientsModule() {
        super("Ambients", Module.Category.RENDER);
        this.addSetting(this.customTime);
        this.addSetting(this.time);
        this.addSetting(this.customSkybox);
        this.addSetting(this.skyboxMode);
        this.addSetting(this.skyboxStrength);
        this.addSetting(this.customFog);
        this.addSetting(this.fogStart);
        this.addSetting(this.fogEnd);
        this.addSetting(this.fogColorEnabled);
        this.addSetting(this.themeSync);
        this.addSetting(this.fogStrength);
        this.addSetting(this.fogShader);
        this.addSetting(this.fogShaderMode);
        this.addSetting(this.fogShaderStrength);
        this.addSetting(this.fogShaderSpeed);
        this.addSetting(this.fogShaderDensity);
        this.addSetting(this.fogShaderHorizon);
        this.addSetting(this.fogShaderThemeSync);
        this.addSetting(this.blockBrightness);
        this.addSetting(this.saturation);
        this.addSetting(this.sphereBlur);
        this.addSetting(this.sphereRadius);
        this.addSetting(this.blurSmoothness);
        this.addSetting(this.blurStrength);
        this.addSetting(this.blurQuality);
        this.addSetting(this.smoothFollow);
        this.addSetting(this.blurFollowSpeed);
        this.addSetting(this.blurSky);
        this.addSetting(this.blurThemeTint);
        this.addSetting(this.blurThemeStrength);
        LevelRenderEvents.AFTER_SOLID_FEATURES.register(context -> {
            if (this.isEnabled() && this.customSkybox.get()) {
                this.renderShaderSkybox(context);
            }
        });
        LevelRenderEvents.AFTER_SOLID_FEATURES.register(context -> {
            if (this.isEnabled() && this.fogShader.get()) {
                this.renderShaderFog(context);
            }
        });
        LevelRenderEvents.AFTER_SOLID_FEATURES.register(context -> {
            if (this.isEnabled() && this.blockBrightness.get() < 1.0) {
                this.renderBlackShader(context);
            }
        });
        LevelRenderEvents.END_MAIN.register(context -> {
            if (this.isEnabled() && this.sphereBlur.get()) {
                this.renderSphereBlur(context);
            }
        });
        LevelRenderEvents.END_MAIN.register(context -> {
            if (this.isEnabled() && Math.abs(this.saturation.get() - 1.0) > 0.001) {
                this.renderSaturation(context);
            }
        });
    }

    

    

    /*
     * Enabled aggressive exception aggregation
     */
    private String loadShaderSource(String path) {
        try (InputStream is = Minecraft.getInstance().getResourceManager().open(Identifier.fromNamespaceAndPath((String)"astolfoclient", (String)path));){
            String string;
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));){
                string = reader.lines().collect(Collectors.joining("\n"));
            }
            return string;
        }
        catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }

    

    

    

    

    

    

    

    

    private void renderShaderSkybox(LevelRenderContext context) {
 if(Minecraft.getInstance().player==null)return;
 String name=switch(skyboxMode.get()) {case "Smoke" -> "ambient_smoke";case "Clouds" -> "ambient_clouds";case "Caustic" -> "ambient_caustic";case "Nebula" -> "ambient_nebula";case "Radiant" -> "ambient_radiant";case "Sunset" -> "ambient_sunset";default -> "ambient_textured";};
 var pass=skyPass(name,context);
 float t=(System.currentTimeMillis()%100000L)/1000f;var color=new Color(ThemeManager.getThemedColor(0));float r=color.getRed()/255f,g=color.getGreen()/255f,b=color.getBlue()/255f,a=skyboxStrength.getFloat();
 pass.uniform("time",t).uniform("Time",t).uniform("themeColor",r,g,b).uniform("Accent",r,g,b).uniform("alpha",a).uniform("ColorModulator",1,1,1,a).uniform("themeSync",themeSync.get()?1:0);
 boolean textured=name.equals("ambient_textured");
 if(textured) {int index=switch(skyboxMode.get()){case "Lake"->2;case "Cloud Space"->3;case "Clear Evening"->4;case "Underwater"->5;default->1;};var texture=xyz.angames.astolfoclient.client.render.compat.WorldPass.outside(()->Minecraft.getInstance().getTextureManager().getTexture(Identifier.fromNamespaceAndPath("astolfoclient","sky/"+index+".png")));pass.texture(0,texture.getTextureView(),true);}
 drawSky(pass,textured,false);
}

    

    private void renderBlackShader(LevelRenderContext context) {var pass=skyPass("ambient_black",context).uniform("alpha",1-blockBrightness.getFloat());drawSky(pass,false,true);}

    

    

    

    

    

    

    private void renderShaderFog(LevelRenderContext context) {
 if(Minecraft.getInstance().player==null)return;
 String name=switch(fogShaderMode.get()){case "Smoke"->"ambient_fogsmoke";case "Caustic"->"ambient_fogcaustic";case "Nebula"->"ambient_fognebula";case "Waves"->"ambient_fogwaves";case "Abyss"->"ambient_fogabyss";default->"ambient_fogmist";};
 var color=new Color(ThemeManager.getThemedColor(0));float r=fogShaderThemeSync.get()?color.getRed()/255f:1,g=fogShaderThemeSync.get()?color.getGreen()/255f:1,b=fogShaderThemeSync.get()?color.getBlue()/255f:1;
 var pass=skyPass(name,context).uniform("time",(System.currentTimeMillis()%100000L)/1000f).uniform("themeColor",r,g,b).uniform("alpha",fogShaderStrength.getFloat()).uniform("speed",fogShaderSpeed.getFloat()).uniform("density",fogShaderDensity.getFloat()).uniform("horizon",fogShaderHorizon.getFloat());drawSky(pass,false,false);
}

    

    

    

    private Vec3 getSmoothSphereCenter(float tickDelta) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return Vec3.ZERO;
        }
        double targetX = Mth.lerp((double)tickDelta, (double)mc.player.xOld, (double)mc.player.getX());
        double targetY = Mth.lerp((double)tickDelta, (double)mc.player.yOld, (double)mc.player.getY()) + (double)mc.player.getBbHeight() * 0.5;
        double targetZ = Mth.lerp((double)tickDelta, (double)mc.player.zOld, (double)mc.player.getZ());
        Vec3 targetPos = new Vec3(targetX, targetY, targetZ);
        if (this.smoothSphereCenter == null || !this.smoothFollow.get()) {
            this.smoothSphereCenter = targetPos;
            return targetPos;
        }
        if (this.smoothSphereCenter.distanceToSqr(targetPos) > 40000.0) {
            this.smoothSphereCenter = targetPos;
            return targetPos;
        }
        long now = System.currentTimeMillis();
        float dt = this.lastSphereUpdateTime == 0L ? 0.016f : (float)(now - this.lastSphereUpdateTime) / 1000.0f;
        dt = Mth.clamp((float)dt, (float)0.001f, (float)0.1f);
        this.lastSphereUpdateTime = now;
        float speed = (float)this.blurFollowSpeed.get();
        float t = 1.0f - (float)Math.exp(-speed * dt);
        this.smoothSphereCenter = new Vec3(Mth.lerp((double)t, (double)this.smoothSphereCenter.x, (double)targetPos.x), Mth.lerp((double)t, (double)this.smoothSphereCenter.y, (double)targetPos.y), Mth.lerp((double)t, (double)this.smoothSphereCenter.z, (double)targetPos.z));
        return this.smoothSphereCenter;
    }

    private void renderSphereBlur(LevelRenderContext context) {
 var mc=Minecraft.getInstance();if(mc.player==null)return;var mainFbo=mc.gameRenderer.mainRenderTarget();if(mainFbo.getDepthTextureView()==null)return;int width=mainFbo.width,height=mainFbo.height;
         Matrix4f viewRotMat = new Matrix4f().rotation((Quaternionfc)new Quaternionf((Quaternionfc)context.gameRenderer().mainCamera().rotation()).conjugate());
        Matrix4f projMat = new Matrix4f((Matrix4fc)RenderSystem.getProjectionMatrix());
        Matrix4f invViewProjMat = new Matrix4f((Matrix4fc)projMat).mul((Matrix4fc)viewRotMat).invert();
        float[] invViewProjArr = new float[16];
        invViewProjMat.get(invViewProjArr);
        float tickDelta = Minecraft.getInstance().getDeltaTracker() != null ? Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(true) : 1.0f;
        Vec3 sphereCenter = this.getSmoothSphereCenter(tickDelta);
        Vec3 camPos = context.gameRenderer().mainCamera().position();
        Vec3 sphereCenterRelCam = sphereCenter.subtract(camPos);
        int sampleCount = 32;
        String quality = this.blurQuality.get();
        if (quality.equals("Low")) {
            sampleCount = 12;
        } else if (quality.equals("Medium")) {
            sampleCount = 20;
        } else if (quality.equals("High")) {
            sampleCount = 32;
        } else if (quality.equals("Ultra")) {
            sampleCount = 48;
        }
        Color themeColor = new Color(ThemeManager.getThemedColor(0L));
        float r = (float)themeColor.getRed() / 255.0f;
        float g = (float)themeColor.getGreen() / 255.0f;
        float b = (float)themeColor.getBlue() / 255.0f;
        float tintStrength = this.blurThemeTint.get() ? (float)this.blurThemeStrength.get() : 0.0f;

 var copy=dev.sxmurxy.mre.renderers.GpuSceneCopy.capture();
 var pass=new xyz.angames.astolfoclient.client.render.compat.ScreenPass("ambient_sphereblur").texture(0,copy.getColorTextureView(),true).texture(1,mainFbo.getDepthTextureView(),false);
 pass.uniform("uInvViewProjMat",invViewProjArr).uniform("uSphereCenterRelCam",(float)sphereCenterRelCam.x,(float)sphereCenterRelCam.y,(float)sphereCenterRelCam.z).uniform("uSphereRadius",sphereRadius.getFloat()).uniform("uSmoothness",blurSmoothness.getFloat()).uniform("uBlurStrength",blurStrength.getFloat()).uniform("uResolution",width,height).uniform("uSampleCount",sampleCount).uniform("uBlurSky",blurSky.get()?1:0).uniform("uThemeColor",r,g,b).uniform("uThemeTint",tintStrength).draw(mainFbo);
}

    

    

    

    private void renderSaturation(LevelRenderContext context) {if(Math.abs(saturation.get()-1)<0.001)return;var copy=dev.sxmurxy.mre.renderers.GpuSceneCopy.capture();new xyz.angames.astolfoclient.client.render.compat.ScreenPass("ambient_saturation").texture(0,copy.getColorTextureView(),true).uniform("uSaturation",saturation.getFloat()).draw(Minecraft.getInstance().gameRenderer.mainRenderTarget());}

    @Override
    public void onEnable() {
    }

    @Override
    public void onDisable() {this.smoothSphereCenter=null;}

    @Override
    public void onTick() {
    }
 private xyz.angames.astolfoclient.client.render.compat.ScreenPass skyPass(String name,LevelRenderContext context) {float[] view=new float[16],projection=new float[16];new Matrix4f().rotation(new Quaternionf(context.gameRenderer().mainCamera().rotation()).conjugate()).get(view);RenderSystem.getProjectionMatrix().get(projection);return new xyz.angames.astolfoclient.client.render.compat.ScreenPass(name).uniform("uModelViewMat",view).uniform("uProjMat",projection);}
 private float[] texturedVertices() {        float size = 100.0f;
        float[] vertices = new float[180];
        int index = 0;
        int[] faces = new int[]{1, 0, 2, 4, 3, 5};
        float[][] coords = new float[][]{{-size, -size, size, -size, -size, -size, size, -size, -size, size, -size, size}, {-size, size, -size, -size, size, size, size, size, size, size, size, -size}, {size, size, -size, size, -size, -size, -size, -size, -size, -size, size, -size}, {-size, size, -size, -size, -size, -size, -size, -size, size, -size, size, size}, {-size, size, size, -size, -size, size, size, -size, size, size, size, size}, {size, size, size, size, -size, size, size, -size, -size, size, size, -size}};
        for (int face : faces) {
            float[] c = coords[face];
            int col = face % 3;
            int row = face / 3;
            float u1 = (float)col / 3.0f;
            float u2 = (float)(col + 1) / 3.0f;
            float v1 = (float)row / 2.0f;
            float v2 = (float)(row + 1) / 2.0f;
            float x1 = c[0];
            float y1 = c[1];
            float z1 = c[2];
            float x2 = c[3];
            float y2 = c[4];
            float z2 = c[5];
            float x3 = c[6];
            float y3 = c[7];
            float z3 = c[8];
            float x4 = c[9];
            float y4 = c[10];
            float z4 = c[11];
            vertices[index++] = x1;
            vertices[index++] = y1;
            vertices[index++] = z1;
            vertices[index++] = u1;
            vertices[index++] = v1;
            vertices[index++] = x2;
            vertices[index++] = y2;
            vertices[index++] = z2;
            vertices[index++] = u1;
            vertices[index++] = v2;
            vertices[index++] = x3;
            vertices[index++] = y3;
            vertices[index++] = z3;
            vertices[index++] = u2;
            vertices[index++] = v2;
            vertices[index++] = x1;
            vertices[index++] = y1;
            vertices[index++] = z1;
            vertices[index++] = u1;
            vertices[index++] = v1;
            vertices[index++] = x3;
            vertices[index++] = y3;
            vertices[index++] = z3;
            vertices[index++] = u2;
            vertices[index++] = v2;
            vertices[index++] = x4;
            vertices[index++] = y4;
            vertices[index++] = z4;
            vertices[index++] = u2;
            vertices[index++] = v1;
        }

 return vertices;}
 private void drawSky(xyz.angames.astolfoclient.client.render.compat.ScreenPass pass,boolean textured,boolean greater) {
 float[] vertices=textured?texturedVertices():new float[]{-1.0f, -1.0f, -1.0f, 1.0f, -1.0f, -1.0f, 1.0f, 1.0f, -1.0f, -1.0f, -1.0f, -1.0f, 1.0f, 1.0f, -1.0f, -1.0f, 1.0f, -1.0f, -1.0f, -1.0f, 1.0f, 1.0f, -1.0f, 1.0f, 1.0f, 1.0f, 1.0f, -1.0f, -1.0f, 1.0f, 1.0f, 1.0f, 1.0f, -1.0f, 1.0f, 1.0f, -1.0f, -1.0f, -1.0f, -1.0f, -1.0f, 1.0f, -1.0f, 1.0f, 1.0f, -1.0f, -1.0f, -1.0f, -1.0f, 1.0f, 1.0f, -1.0f, 1.0f, -1.0f, 1.0f, -1.0f, -1.0f, 1.0f, -1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, -1.0f, -1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, -1.0f, -1.0f, 1.0f, -1.0f, 1.0f, 1.0f, -1.0f, 1.0f, 1.0f, 1.0f, -1.0f, 1.0f, -1.0f, 1.0f, 1.0f, 1.0f, -1.0f, 1.0f, 1.0f, -1.0f, -1.0f, -1.0f, 1.0f, -1.0f, -1.0f, 1.0f, -1.0f, 1.0f, -1.0f, -1.0f, -1.0f, 1.0f, -1.0f, 1.0f, -1.0f, -1.0f, 1.0f};int stride=textured?5:3;
 var builder=xyz.angames.astolfoclient.client.render.compat.Tesselator.getInstance().begin(com.mojang.renderpearl.api.pipeline.PrimitiveTopology.TRIANGLES,textured?com.mojang.blaze3d.vertex.DefaultVertexFormat.POSITION_TEX:com.mojang.blaze3d.vertex.DefaultVertexFormat.POSITION);
 for(int i=0;i<vertices.length;i+=stride){var vertex=builder.addVertex(vertices[i],vertices[i+1],vertices[i+2]);if(textured)vertex.setUv(vertices[i+3],vertices[i+4]);}
 pass.drawMesh(builder.buildOrThrow(),Minecraft.getInstance().gameRenderer.mainRenderTarget(),true,greater?com.mojang.renderpearl.api.pipeline.CompareOp.LESS_THAN:com.mojang.renderpearl.api.pipeline.CompareOp.GREATER_THAN_OR_EQUAL,true);
 }
}
