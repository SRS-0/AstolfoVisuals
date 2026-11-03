package xyz.angames.astolfoclient.client.mixin;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.angames.astolfoclient.client.AstolfoclientClient;
import xyz.angames.astolfoclient.client.module.Module;

@Environment(value=EnvType.CLIENT)
@Mixin(value={PlayerModel.class})
public abstract class PlayerEntityModelMixin {
    @Inject(method={"setupAnim(Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;)V"}, at={@At(value="TAIL")})
    private void swellBabyHead(AvatarRenderState state, CallbackInfo ci) {
        if (Minecraft.getInstance().player != null && state.id == Minecraft.getInstance().player.getId()) {
            Module babyMod = AstolfoclientClient.moduleManager.getModuleByName("BabyPlayer");
            PlayerModel model = (PlayerModel)(Object)this;
            if (babyMod != null && babyMod.isEnabled()) {
                model.head.xScale = 1.75f;
                model.head.yScale = 1.75f;
                model.head.zScale = 1.75f;
            } else {
                model.head.xScale = 1.0f;
                model.head.yScale = 1.0f;
                model.head.zScale = 1.0f;
            }
        }
    }
}

