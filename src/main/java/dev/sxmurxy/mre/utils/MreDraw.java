package dev.sxmurxy.mre.utils;

import com.mojang.renderpearl.api.pipeline.IndexType;
import com.mojang.renderpearl.api.buffers.GpuBuffer;
import com.mojang.renderpearl.api.buffers.GpuBufferSlice;
import com.mojang.blaze3d.buffers.Std140Builder;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.renderpearl.api.device.GpuDevice;
import com.mojang.renderpearl.api.commands.RenderPass;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.renderpearl.api.textures.FilterMode;
import com.mojang.renderpearl.api.textures.GpuSampler;
import com.mojang.renderpearl.api.textures.GpuTexture;
import com.mojang.renderpearl.api.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.MeshData;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalDouble;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.renderer.Projection;
import net.minecraft.client.renderer.ProjectionMatrixBuffer;
import net.minecraft.client.renderer.state.WindowRenderState;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.lwjgl.system.MemoryStack;

/**
 * Draw submission for MRE. Calls made while the GUI is being extracted are queued (the 26.x GUI is deferred, so an
 * immediate draw would land underneath it) and flushed once the GUI has been rendered. Calls made in the level
 * render ({@link #setImmediate(boolean)}) are drawn straight away.
 */
public final class MreDraw {

    public interface UniformWriter {
        void write(Std140Builder builder);
    }

    public record Texture(String name, GpuTextureView view, GpuSampler sampler) {
    }

    private record Pending(RenderPipeline pipeline, GpuBuffer vbo, GpuBuffer ubo, IndexType indexType, int indexCount,
                           List<Texture> textures, boolean backdrop, ScreenRectangle scissor) {
    }

    private static final List<Pending> QUEUE = new ArrayList<>();
    private static boolean immediate;
    private static ScreenRectangle currentScissor;
    private static final Projection GUI_PROJECTION = new Projection();
    private static final ProjectionMatrixBuffer GUI_PROJECTION_BUFFER = new ProjectionMatrixBuffer("mre gui");
    private static boolean guiPass;
    private static GpuTexture backdropTexture;
    private static GpuTextureView backdropView;

    private MreDraw() {
    }

    /** Current GUI scissor (GUI-scaled coordinates), maintained by GuiGraphicsExtractorMixin. */
    public static void setScissor(ScreenRectangle rectangle) {
        currentScissor = rectangle;
    }

    public static void setImmediate(boolean value) {
        immediate = value;
    }

    public static GpuSampler linear() {
        return RenderSystem.getSamplerCache().getClampToEdge(FilterMode.LINEAR);
    }

    /** Submits a mesh. {@code uniformSize} is the std140 size of the MreParams block (0 for none). */
    public static void submit(MeshData mesh, RenderPipeline pipeline, int uniformSize, UniformWriter uniforms,
                              List<Texture> textures, boolean backdrop) {
        try (MeshData m = mesh) {
            MeshData.DrawState ds = m.drawState();
            if (ds.indexCount() <= 0) {
                return;
            }

            GpuDevice device = RenderSystem.getDevice();
            GpuBuffer vbo = device.createBuffer(() -> "MRE vbo", GpuBuffer.USAGE_VERTEX, m.vertexBuffer());
            GpuBuffer ubo = null;
            if (uniformSize > 0) {
                try (MemoryStack stack = MemoryStack.stackPush()) {
                    Std140Builder b = Std140Builder.onStack(stack, uniformSize);
                    uniforms.write(b);
                    ubo = device.createBuffer(() -> "MRE params", GpuBuffer.USAGE_UNIFORM | GpuBuffer.USAGE_COPY_DST, b.get());
                }
            }

            Pending p = new Pending(pipeline, vbo, ubo, ds.indexType(), ds.indexCount(), textures, backdrop,
                immediate ? null : currentScissor);
            if (immediate) {
                draw(p);
            } else {
                QUEUE.add(p);
            }
        }
    }

    /** Draws everything queued during GUI extraction. Must be called with the GUI projection bound. */
    public static void flush() {
        if (QUEUE.isEmpty()) {
            return;
        }

        List<Pending> copy = new ArrayList<>(QUEUE);
        QUEUE.clear();
        WindowRenderState w = Minecraft.getInstance().gameRenderer.gameRenderState().windowRenderState;
        GUI_PROJECTION.setupOrtho(1000.0F, 11000.0F, (float) w.width / w.guiScale, (float) w.height / w.guiScale, true);
        guiPass = true;
        try {
            for (Pending p : copy) {
                draw(p);
            }
        } finally {
            guiPass = false;
            currentScissor = null;
        }
    }

    private static void draw(Pending p) {
        GpuDevice device = RenderSystem.getDevice();
        try {
            RenderTarget target = Minecraft.getInstance().gameRenderer.mainRenderTarget();
            GpuTextureView color = target.getColorTextureView();
            List<Texture> textures = p.textures;
            if (p.backdrop) {
                captureBackdrop(target);
                textures = new ArrayList<>(textures);
                textures.add(new Texture("Sampler0", backdropView, linear()));
            }

            var seq = RenderSystem.getSequentialBuffer(p.pipeline.getPrimitiveTopology());
            GpuBuffer ibo = seq.getBuffer(p.indexCount);
            // GUI geometry is submitted at z=0 and the GUI ortho projection spans [1000, 11000]; vanilla shifts it by -11000.
            GpuBufferSlice transforms = RenderSystem.getDynamicUniforms().writeTransform(
                guiPass ? new Matrix4f().setTranslation(0.0F, 0.0F, -11000.0F) : RenderSystem.getModelViewMatrixCopy(), new Vector4f(1.0F, 1.0F, 1.0F, 1.0F));
            // Writing the projection buffer is itself a command, so it has to happen before the pass is opened.
            GpuBufferSlice guiProjectionSlice = guiPass ? GUI_PROJECTION_BUFFER.getBuffer(GUI_PROJECTION) : null;
            try (RenderPass pass = device.createCommandEncoder()
                    .createRenderPass(() -> "MRE draw", color, Optional.empty(), null, OptionalDouble.empty())) {
                pass.setPipeline(RenderSystem.getCompiledPipeline(p.pipeline));
                if (p.scissor != null) {
                    WindowRenderState w = Minecraft.getInstance().gameRenderer.gameRenderState().windowRenderState;
                    int scale = w.guiScale;
                    double left = p.scissor.left() * scale;
                    double top = p.scissor.top() * scale;
                    double right = Math.min(p.scissor.right() * scale, w.width);
                    double bottom = Math.min(p.scissor.bottom() * scale, w.height);
                    pass.enableScissor((int) left, w.height - (int) bottom, Math.max(0, (int) (right - left)), Math.max(0, (int) (bottom - top)));
                }

                RenderSystem.bindDefaultUniforms(pass);
                if (guiPass) {
                    pass.setUniform("Projection", guiProjectionSlice);
                }

                pass.setUniform("DynamicTransforms", transforms);
                if (p.ubo != null) {
                    pass.setUniform("MreParams", p.ubo);
                }

                pass.setVertexBuffer(0, p.vbo.slice());
                for (Texture t : textures) {
                    pass.setUniform(t.name, t.view, t.sampler);
                }

                pass.setIndexBuffer(ibo, seq.type());
                pass.drawIndexed(p.indexCount, 1, 0, 0, 0);
            }
        } finally {
            p.vbo.close();
            if (p.ubo != null) {
                p.ubo.close();
            }
        }
    }

    private static void captureBackdrop(RenderTarget target) {
        GpuDevice device = RenderSystem.getDevice();
        GpuTexture src = target.getColorTexture();
        if (backdropTexture == null || backdropTexture.getWidth(0) != src.getWidth(0) || backdropTexture.getHeight(0) != src.getHeight(0)) {
            if (backdropView != null) {
                backdropView.close();
            }

            if (backdropTexture != null) {
                backdropTexture.close();
            }

            backdropTexture = device.createTexture(() -> "MRE backdrop", 15, src.getFormat(), src.getWidth(0), src.getHeight(0), 1, 1);
            backdropView = device.createTextureView(backdropTexture);
        }

        device.createCommandEncoder().copyTextureToTexture(src, backdropTexture, 0, 0, 0, 0, 0, src.getWidth(0), src.getHeight(0));
    }

}
