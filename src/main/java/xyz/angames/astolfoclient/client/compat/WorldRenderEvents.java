package xyz.angames.astolfoclient.client.compat;

import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;

/**
 * Re-creation of the removed {@code WorldRenderEvents}. 26.x has no "entities"/"last" split that is safe to draw
 * immediately in, so every phase fires at {@link LevelRenderEvents#END_MAIN}, after the main level pass is closed.
 */
public final class WorldRenderEvents {
   public static final Event<BeforeEntities> BEFORE_ENTITIES = phase(BeforeEntities.class, (cb, c) -> cb.beforeEntities(c));
   public static final Event<AfterEntities> AFTER_ENTITIES = phase(AfterEntities.class, (cb, c) -> cb.afterEntities(c));
   public static final Event<AfterTranslucent> AFTER_TRANSLUCENT = phase(AfterTranslucent.class, (cb, c) -> cb.afterTranslucent(c));
   public static final Event<Last> LAST = phase(Last.class, (cb, c) -> cb.onLast(c));

   static {
      LevelRenderEvents.END_MAIN.register(level -> {
         WorldRenderContext ctx = new WorldRenderContext(level);
         RenderSystem.setLastProjection(ctx.projectionMatrix());
         RenderSystem.flushDeferred(new org.joml.Matrix4f(ctx.cameraState().viewRotationMatrix));
         dev.sxmurxy.mre.utils.MreDraw.setImmediate(true);
         // Raw-GL effects draw into whatever framebuffer is bound; make that the main target for the duration.
         int prevFbo = org.lwjgl.opengl.GL11.glGetInteger(org.lwjgl.opengl.GL30.GL_FRAMEBUFFER_BINDING);
         int[] prevViewport = new int[4];
         org.lwjgl.opengl.GL11.glGetIntegerv(org.lwjgl.opengl.GL11.GL_VIEWPORT, prevViewport);
         GlCompat.bindWrite(net.minecraft.client.Minecraft.getInstance().gameRenderer.mainRenderTarget(), true);
         try {
            BEFORE_ENTITIES.invoker().beforeEntities(ctx);
            AFTER_ENTITIES.invoker().afterEntities(ctx);
            AFTER_TRANSLUCENT.invoker().afterTranslucent(ctx);
            LAST.invoker().onLast(ctx);
         } finally {
            dev.sxmurxy.mre.utils.MreDraw.setImmediate(false);
            org.lwjgl.opengl.GL30.glBindFramebuffer(org.lwjgl.opengl.GL30.GL_FRAMEBUFFER, prevFbo);
            org.lwjgl.opengl.GL11.glViewport(prevViewport[0], prevViewport[1], prevViewport[2], prevViewport[3]);
         }
      });
   }

   private WorldRenderEvents() {
   }

   /** Forces class initialisation so the END_MAIN hook is registered. */
   public static void init() {
   }

   private interface Invoker<T> {
      void call(T callback, WorldRenderContext context);
   }

   @SuppressWarnings("unchecked")
   private static <T> Event<T> phase(Class<T> type, Invoker<T> invoker) {
      return EventFactory.createArrayBacked(type, callbacks -> (T)java.lang.reflect.Proxy.newProxyInstance(
         type.getClassLoader(), new Class<?>[]{type}, (proxy, method, args) -> {
            for (T cb : callbacks) {
               invoker.call(cb, (WorldRenderContext)args[0]);
            }

            return null;
         }
      ));
   }

   @FunctionalInterface
   public interface BeforeEntities {
      void beforeEntities(WorldRenderContext context);
   }

   @FunctionalInterface
   public interface AfterEntities {
      void afterEntities(WorldRenderContext context);
   }

   @FunctionalInterface
   public interface AfterTranslucent {
      void afterTranslucent(WorldRenderContext context);
   }

   @FunctionalInterface
   public interface Last {
      void onLast(WorldRenderContext context);
   }
}
