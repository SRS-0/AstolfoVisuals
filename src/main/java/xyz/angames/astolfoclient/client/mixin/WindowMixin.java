package xyz.angames.astolfoclient.client.mixin;

import java.io.InputStream;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.util.Icons;
import net.minecraft.client.util.Window;
import net.minecraft.resource.ResourcePack;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.glfw.GLFWImage;
import org.lwjgl.system.MemoryUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Environment(value=EnvType.CLIENT)
@Mixin(value={Window.class})
public class WindowMixin {
    private boolean iconsSet = false;

    @Inject(method={"<init>"}, at={@At(value="TAIL")})
    private void onInit(CallbackInfo ci) {
        this.setCustomIcons();
    }

    @Inject(method={"setIcon"}, at={@At(value="HEAD")}, cancellable=true)
    private void onSetIcon(ResourcePack resourcePack, Icons icons, CallbackInfo ci) {
        ci.cancel();
        if (!this.iconsSet) {
            this.setCustomIcons();
            this.iconsSet = true;
        }
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private void setCustomIcons() {
        block19: {
            Window window = (Window)(Object)this;
            long handle = window.getHandle();
            String[] paths = new String[]{"/assets/astolfoclient/textures/gui/logo_black_32.png", "/assets/astolfoclient/textures/gui/logo_black_64.png", "/assets/astolfoclient/textures/gui/logo_black_big.png"};
            ArrayList<ByteBuffer> buffers = new ArrayList<ByteBuffer>();
            GLFWImage.Buffer iconBuffer = null;
            try {
                for (String path : paths) {
                    try (InputStream is = WindowMixin.class.getResourceAsStream(path);){
                        NativeImage image;
                        if (is == null || (image = NativeImage.read((InputStream)is)) == null) continue;
                        int w = image.getWidth();
                        int h = image.getHeight();
                        ByteBuffer buf = MemoryUtil.memAlloc((int)(w * h * 4));
                        for (int y = 0; y < h; ++y) {
                            for (int x = 0; x < w; ++x) {
                                int argb = image.getColorArgb(x, y);
                                buf.put((byte)(argb >> 16 & 0xFF));
                                buf.put((byte)(argb >> 8 & 0xFF));
                                buf.put((byte)(argb & 0xFF));
                                buf.put((byte)(argb >> 24 & 0xFF));
                            }
                        }
                        buf.flip();
                        image.close();
                        if (iconBuffer == null) {
                            iconBuffer = GLFWImage.malloc((int)paths.length);
                        }
                        iconBuffer.position(buffers.size());
                        iconBuffer.width(w);
                        iconBuffer.height(h);
                        iconBuffer.pixels(buf);
                        buffers.add(buf);
                    }
                }
                if (iconBuffer == null) break block19;
                iconBuffer.position(0);
                iconBuffer.limit(buffers.size());
                GLFW.glfwSetWindowIcon((long)handle, iconBuffer);
            }
            catch (Exception e) {
                e.printStackTrace();
            }
            finally {
                if (iconBuffer != null) {
                    iconBuffer.free();
                }
                for (ByteBuffer buf : buffers) {
                    MemoryUtil.memFree((Buffer)buf);
                }
            }
        }
    }
}

