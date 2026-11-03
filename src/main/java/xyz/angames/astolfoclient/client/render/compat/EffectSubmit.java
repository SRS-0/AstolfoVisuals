package xyz.angames.astolfoclient.client.render.compat;

import java.util.List;
import net.fabricmc.fabric.api.client.rendering.v1.FeatureRendererRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.SubmitRenderPhases;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.feature.*;
import net.minecraft.client.renderer.feature.submit.SubmitNode;

/** Executes GPU effects during drawing, rather than the geometry preparation phase. */
public record EffectSubmit(Runnable draw) implements SubmitNode {
    private static final FeatureRendererType<EffectSubmit> TYPE = FeatureRendererType.create("astolfo_effect");
    public static void register() {
        FeatureRendererRegistry.register(TYPE, () -> new FeatureRenderer<EffectSubmit>() {
            public void prepareGroup(FeatureFrameContext context, List<EffectSubmit> submits, boolean outline) {}
            public void executeGroup(FeatureFrameContext context, int group, List<EffectSubmit> submits, boolean outline) {
                if (!outline) submits.forEach(submit -> submit.draw.run());
            }
        });
    }
    public static void submit(SubmitNodeCollector queue, Runnable draw) {
        queue.submitCustom(SubmitRenderPhases.TRANSLUCENT_CUSTOM_GEOMETRY, new EffectSubmit(draw));
    }
    @Override public FeatureRendererType<EffectSubmit> featureType() { return TYPE; }
}
