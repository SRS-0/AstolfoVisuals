package xyz.angames.astolfoclient.client.compat;

import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import com.mojang.blaze3d.systems.RenderSystem;
import dev.sxmurxy.mre.utils.CRenderPipelines;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import net.minecraft.client.Minecraft;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xyz.angames.astolfoclient.client.AstolfoclientClient;

/**
 * Compiles every pipeline the mod owns once, on the first client tick, and logs the result. Shader/bind-layout
 * mistakes then show up at startup instead of the first time an effect happens to draw.
 */
public final class PipelineSelfTest {
   private static final Logger LOGGER = LoggerFactory.getLogger("AstolfoVisuals/PipelineSelfTest");
   private static boolean done;
   private static int ticks;

   private PipelineSelfTest() {
   }

   public static void runOnce(Minecraft client) {
      // Wait until the startup resource reload has finished so shader sources exist.
      if (done || ++ticks < 300) {
         return;
      }

      done = true;
      int ok = 0;
      int failed = 0;
      for (Class<?> holder : new Class<?>[]{CRenderPipelines.class, AstolfoclientClient.class}) {
         for (Field f : holder.getDeclaredFields()) {
            if (!Modifier.isStatic(f.getModifiers())) {
               continue;
            }

            try {
               f.setAccessible(true);
               Object v = f.get(null);
               RenderPipeline pipeline = null;
               if (v instanceof RenderPipeline p) {
                  pipeline = p;
               } else if (v instanceof ShaderProgram sp) {
                  pipeline = xyz.angames.astolfoclient.client.compat.RenderSystem.pipelineFor(sp);
               }

               if (pipeline == null) {
                  continue;
               }

               if (RenderSystem.getCompiledPipeline(pipeline) != null) {
                  ok++;
                  LOGGER.info("pipeline OK: {}", f.getName());
               } else {
                  failed++;
                  LOGGER.error("pipeline INVALID: {}", f.getName());
               }
            } catch (Throwable t) {
               failed++;
               LOGGER.error("pipeline FAILED: " + f.getName(), t);
            }
         }
      }

      LOGGER.info("pipeline self-test finished: {} ok, {} failed", ok, failed);
   }
}
