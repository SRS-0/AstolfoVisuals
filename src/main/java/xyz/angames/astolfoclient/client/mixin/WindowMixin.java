package xyz.angames.astolfoclient.client.mixin;

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.platform.Window;
import net.minecraft.server.packs.PackMetadataResources;
import com.mojang.blaze3d.platform.IconSet;
import xyz.angames.astolfoclient.client.compat.GLFW;
import org.lwjgl.system.MemoryUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(EnvType.CLIENT)
@Mixin(Window.class)
public class WindowMixin {
   private boolean iconsSet = false;

   @Inject(method = "<init>", at = @At("TAIL"))
   private void onInit(CallbackInfo ci) {
      this.setCustomIcons();
   }

   @Inject(method = "setIcon", at = @At("HEAD"), cancellable = true)
   private void onSetIcon(PackMetadataResources resourcePack, IconSet icons, CallbackInfo ci) {
      ci.cancel();
      if (!this.iconsSet) {
         this.setCustomIcons();
         this.iconsSet = true;
      }
   }

   private void setCustomIcons() {
      Window window = (Window)(Object)this;
      long handle = window.handle();
      String[] paths = new String[]{
         "/assets/astolfoclient/textures/gui/logo_black_big.png",
         "/assets/astolfoclient/textures/gui/logo_black_64.png",
         "/assets/astolfoclient/textures/gui/logo_black_32.png"
      };

      // SDL takes a single surface; alternate sizes are attached so the platform can pick the best match.
      org.lwjgl.sdl.SDL_Surface first = null;
      for (String path : paths) {
         try (InputStream is = WindowMixin.class.getResourceAsStream(path)) {
            if (is == null) {
               continue;
            }

            NativeImage image = NativeImage.read(is);
            int w = image.getWidth();
            int h = image.getHeight();
            ByteBuffer buf = MemoryUtil.memAlloc(w * h * 4);
            for (int y = 0; y < h; y++) {
               for (int x = 0; x < w; x++) {
                  int argb = image.getPixel(x, y);
                  buf.put((byte)(argb >> 16 & 0xFF));
                  buf.put((byte)(argb >> 8 & 0xFF));
                  buf.put((byte)(argb & 0xFF));
                  buf.put((byte)(argb >> 24 & 0xFF));
               }
            }

            buf.flip();
            image.close();
            org.lwjgl.sdl.SDL_Surface surface = org.lwjgl.sdl.SDLSurface.SDL_CreateSurfaceFrom(w, h, org.lwjgl.sdl.SDLPixels.SDL_PIXELFORMAT_RGBA32, buf, w * 4);
            if (surface == null) {
               continue;
            }

            if (first == null) {
               first = surface;
            } else {
               org.lwjgl.sdl.SDLSurface.SDL_AddSurfaceAlternateImage(first, surface);
            }
         } catch (Exception e) {
            e.printStackTrace();
         }
      }

      if (first != null) {
         org.lwjgl.sdl.SDLVideo.SDL_SetWindowIcon(handle, first);
      }
   }
}
