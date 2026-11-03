package xyz.angames.astolfoclient.client.mixin;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.platform.IconSet;
import net.minecraft.server.packs.PackMetadataResources;
import net.minecraft.server.packs.resources.IoSupplier;
import java.io.*;
import java.util.*;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(Window.class)
public abstract class WindowMixin {
 @Shadow private void setIcon(List<IoSupplier<InputStream>> icons) throws IOException {throw new AssertionError();}
 @Inject(method="setIcon(Lnet/minecraft/server/packs/PackMetadataResources;Lcom/mojang/blaze3d/platform/IconSet;)V",at=@At("HEAD"),cancellable=true)
 private void astolfo$icon(CallbackInfo ci) {
  try {setIcon(List.of(()->Objects.requireNonNull(WindowMixin.class.getResourceAsStream("/assets/astolfoclient/textures/gui/logo_black_64.png"))));ci.cancel();}catch(IOException ignored){}
 }
}
