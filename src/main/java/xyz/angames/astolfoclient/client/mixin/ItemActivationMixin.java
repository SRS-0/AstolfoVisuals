package xyz.angames.astolfoclient.client.mixin;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.angames.astolfoclient.client.module.modules.render.NoRenderModule;
@Mixin(LocalPlayer.class)
public class ItemActivationMixin {    @Inject(method={"displayItemActivation"}, at={@At(value="HEAD")}, cancellable=true)
    private void onShowFloatingItem(ItemStack floatingItem, CallbackInfo ci) {
        NoRenderModule noRender = NoRenderModule.getInstance();
        if (noRender != null && noRender.isEnabled() && noRender.totem.get()) {
            ci.cancel();
        }
    }

}
