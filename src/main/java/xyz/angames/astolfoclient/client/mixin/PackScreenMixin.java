package xyz.angames.astolfoclient.client.mixin;

import java.nio.file.Path;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.screen.pack.PackScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(value=EnvType.CLIENT)
@Mixin(value={PackScreen.class})
public class PackScreenMixin {
    @Shadow
    @Mutable
    private Path file;

    @Inject(method={"<init>"}, at={@At(value="RETURN")})
    private void onInit(CallbackInfo ci) {
    }
}

