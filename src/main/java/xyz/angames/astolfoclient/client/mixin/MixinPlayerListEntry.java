package xyz.angames.astolfoclient.client.mixin;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.client.multiplayer.PlayerInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xyz.angames.astolfoclient.client.util.FriendManager;

@Environment(EnvType.CLIENT)
@Mixin(PlayerInfo.class)
public class MixinPlayerListEntry {
   @Inject(method = "getTabListDisplayName", at = @At("RETURN"), cancellable = true)
   private void astolfo$formatFriendName(CallbackInfoReturnable<Component> cir) {
      PlayerInfo entry = (PlayerInfo)(Object)this;
      if (entry.getProfile() != null && entry.getProfile().name() != null) {
         String playerName = entry.getProfile().name();
         if (FriendManager.isFriend(playerName)) {
            MutableComponent friendName = Component.literal(playerName).withStyle(new ChatFormatting[]{ChatFormatting.GREEN, ChatFormatting.BOLD});
            cir.setReturnValue(friendName);
         }
      }
   }
}
