package xyz.angames.astolfoclient.client.mixin;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.render.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Environment(value=EnvType.CLIENT)
@Mixin(value={GameRenderer.class})
public interface GameRendererAccessor {
    @org.spongepowered.asm.mixin.gen.Accessor("entityRenderDispatcher")
    net.minecraft.client.render.command.RenderDispatcher astolfo$getRenderDispatcher();

    @Invoker(value="updateCrosshairTarget")
    public void invokeUpdateCrosshairTarget(float var1);
}

