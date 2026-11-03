package xyz.angames.astolfoclient.client.mixin;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.entity.player.SkinTextures;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xyz.angames.astolfoclient.client.util.FriendManager;

@Environment(value=EnvType.CLIENT)
@Mixin(value={AbstractClientPlayerEntity.class})
public class AbstractClientPlayerEntityMixin {
    private static final Identifier CUSTOM_CAPE = Identifier.of((String)"astolfoclient", (String)"textures/cape.png");

    @Inject(method={"getSkin"}, at={@At(value="RETURN")}, cancellable=true)
    private void onGetSkinTextures(CallbackInfoReturnable<SkinTextures> cir) {
        boolean isFriend;
        AbstractClientPlayerEntity player = (AbstractClientPlayerEntity)(Object)this;
        MinecraftClient mc = MinecraftClient.getInstance();
        boolean isSelf = mc.player != null && player.getUuid().equals(mc.player.getUuid());
        boolean bl = isFriend = player.getName() != null && FriendManager.isFriend(player.getName().getString()) || player.getGameProfile() != null && FriendManager.isFriend(player.getGameProfile().name());
        if (isSelf || isFriend) {
            SkinTextures original = (SkinTextures)cir.getReturnValue();
            if (original == null) {
                return;
            }
            SkinTextures customTextures = new SkinTextures(original.body(), new net.minecraft.util.AssetInfo.TextureAssetInfo(CUSTOM_CAPE, CUSTOM_CAPE), new net.minecraft.util.AssetInfo.TextureAssetInfo(CUSTOM_CAPE, CUSTOM_CAPE), original.model(), original.secure());
            cir.setReturnValue(customTextures);
        }
    }
}

