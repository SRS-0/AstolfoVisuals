package xyz.angames.astolfoclient.client.mixin;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import xyz.angames.astolfoclient.client.util.FriendManager;

@Environment(value=EnvType.CLIENT)
@Mixin(value={PlayerListEntry.class})
public class MixinPlayerListEntry {
    @Inject(method={"getDisplayName"}, at={@At(value="RETURN")}, cancellable=true)
    private void astolfo$formatFriendName(CallbackInfoReturnable<Text> cir) {
        PlayerListEntry entry = (PlayerListEntry)(Object)this;
        if (entry.getProfile() == null || entry.getProfile().name() == null) {
            return;
        }
        String playerName = entry.getProfile().name();
        if (FriendManager.isFriend(playerName)) {
            MutableText friendName = Text.literal((String)playerName).formatted(new Formatting[]{Formatting.GREEN, Formatting.BOLD});
            cir.setReturnValue(friendName);
        }
    }
}

