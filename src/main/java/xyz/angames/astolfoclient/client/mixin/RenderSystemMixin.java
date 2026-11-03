package xyz.angames.astolfoclient.client.mixin;
import java.awt.Color;
import net.minecraft.client.render.fog.FogRenderer;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import xyz.angames.astolfoclient.client.AstolfoclientClient;
import xyz.angames.astolfoclient.client.config.ThemeManager;
import xyz.angames.astolfoclient.client.module.modules.render.AmbientsModule;
@Mixin(FogRenderer.class)
public class RenderSystemMixin {
 private static AmbientsModule astolfo$ambient() {
  if(AstolfoclientClient.moduleManager==null)return null;
  var module=AstolfoclientClient.moduleManager.getModuleByName("Ambients");
  return module instanceof AmbientsModule ambient && ambient.isEnabled() && ambient.customFog.get()?ambient:null;
 }
 @ModifyVariable(method="applyFog(Ljava/nio/ByteBuffer;ILorg/joml/Vector4f;FFFFFF)V",at=@At("HEAD"),argsOnly=true,ordinal=0)
 private float astolfo$start(float value) {var ambient=astolfo$ambient();return ambient==null?value:ambient.fogStart.getFloat();}
 @ModifyVariable(method="applyFog(Ljava/nio/ByteBuffer;ILorg/joml/Vector4f;FFFFFF)V",at=@At("HEAD"),argsOnly=true,ordinal=1)
 private float astolfo$end(float value) {var ambient=astolfo$ambient();return ambient==null?value:ambient.fogEnd.getFloat();}
 @ModifyVariable(method="applyFog(Ljava/nio/ByteBuffer;ILorg/joml/Vector4f;FFFFFF)V",at=@At("HEAD"),argsOnly=true)
 private Vector4f astolfo$color(Vector4f original) {
  var ambient=astolfo$ambient();
  if(ambient==null || !ambient.fogColorEnabled.get() || !ambient.themeSync.get())return original;
  var color=new Color(ThemeManager.getThemedColor(0));float strength=ambient.fogStrength.getFloat();
  return new Vector4f(original).lerp(new Vector4f(color.getRed()/255f,color.getGreen()/255f,color.getBlue()/255f,original.w),strength);
 }
}
