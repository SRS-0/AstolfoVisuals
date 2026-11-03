package xyz.angames.astolfoclient.client.mixin;
import net.minecraft.client.renderer.Projection;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import xyz.angames.astolfoclient.client.module.modules.render.AspectRatioModule;
@Mixin(Projection.class)
public abstract class AspectProjectionMixin {
 @ModifyVariable(method="setupPerspective",at=@At("HEAD"),argsOnly=true,ordinal=3)
 private float astolfo$aspect(float value,float near,float far,float fov,float width,float height) {
  var module=AspectRatioModule.getInstance();
  return module!=null&&module.isEnabled()&&height>0?height*module.getAspectRatio(width/height):value;
 }
}
