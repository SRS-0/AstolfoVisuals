package xyz.angames.astolfoclient.client.mixin;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.core.ClientAsset;
import net.minecraft.resources.Identifier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.world.entity.player.PlayerSkin;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xyz.angames.astolfoclient.client.util.FriendManager;

@Environment(EnvType.CLIENT)
@Mixin(AbstractClientPlayer.class)
public class AbstractClientPlayerEntityMixin {
   private static final Identifier CUSTOM_CAPE = Identifier.fromNamespaceAndPath("astolfoclient", "cape");

   @Inject(method = "getSkin", at = @At("RETURN"), cancellable = true)
   private void onGetSkinTextures(CallbackInfoReturnable<PlayerSkin> cir) {
      AbstractClientPlayer player = (AbstractClientPlayer)(Object)this;
      Minecraft mc = Minecraft.getInstance();
      boolean isSelf = mc.player != null && player.getUUID().equals(mc.player.getUUID());
      boolean isFriend = player.getName() != null && FriendManager.isFriend(player.getName().getString())
         || player.getGameProfile() != null && FriendManager.isFriend(player.getGameProfile().name());
      if (isSelf || isFriend) {
         PlayerSkin original = (PlayerSkin)cir.getReturnValue();
         if (original == null) {
            return;
         }

         PlayerSkin customTextures = new PlayerSkin(
            original.body(), new ClientAsset.ResourceTexture(CUSTOM_CAPE), new ClientAsset.ResourceTexture(CUSTOM_CAPE), original.model(), original.secure()
         );
         cir.setReturnValue(customTextures);
      }
   }
}
