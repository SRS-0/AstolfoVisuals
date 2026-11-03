package xyz.angames.astolfoclient.client.render.compat;

import com.google.gson.JsonParser;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.VertexFormat;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.*;
import net.minecraft.client.gl.UniformType;
import net.minecraft.util.Identifier;
import org.lwjgl.system.MemoryUtil;

public final class ShaderProgram {
    private final ShaderProgramKey key;
    boolean screen;
    private final Map<String, GlUniform> uniforms = new LinkedHashMap<>();
    private final List<String> samplers = new ArrayList<>();
    private final Map<Variant, RenderPipeline> variants = new HashMap<>();
    private record Layout(String name,int offset,int components,int length,int stride) {}
    private final List<Layout> layouts=new ArrayList<>();
    private int blockSize;

    ShaderProgram(ShaderProgramKey key) {
        this.key = key;
        String asset = "assets/" + key.configId().getNamespace() + "/shaders/" + key.configId().getPath() + ".json";
        try (var stream = ShaderProgram.class.getClassLoader().getResourceAsStream(asset)) {
            if(stream == null) throw new IllegalArgumentException("Missing shader description: " + asset);
            var config = JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
            screen=config.has("screen") && config.get("screen").getAsBoolean();
            blockSize=config.has("blockSize")?config.get("blockSize").getAsInt():0;
            for (var element : config.getAsJsonArray("uniforms")) {
                var spec = element.getAsJsonObject();
                String name = spec.get("name").getAsString();
                if(name.equals("ModelViewMat") || name.equals("ProjMat")) continue;
                var value = new GlUniform(spec.get("count").getAsInt(), spec.get("type").getAsString().equals("int"));
                var defaults = spec.getAsJsonArray("values");
                float[] data = new float[defaults.size()];
                for(int i=0;i<data.length;i++) data[i] = defaults.get(i).getAsFloat();
                value.set(data);
                uniforms.put(name, value);
                if(spec.has("offset"))layouts.add(new Layout(name,spec.get("offset").getAsInt(),spec.get("components").getAsInt(),spec.get("length").getAsInt(),spec.get("stride").getAsInt()));
            }
            for(var element : config.getAsJsonArray("samplers")) samplers.add(element.getAsJsonObject().get("name").getAsString());
        } catch (java.io.IOException e) { throw new IllegalStateException("Loading " + asset, e); }
    }
    public GlUniform getUniform(String name) {
        int start=name.indexOf('[');
        if(start>=0 && name.endsWith("]")) {
            var array=uniforms.get(name.substring(0,start));
            return array==null?null:array.element(Integer.parseInt(name.substring(start+1,name.length()-1)));
        }
        return uniforms.get(name);
    }
    public Map<String,GlUniform> getUniformsByName() { return Collections.unmodifiableMap(uniforms); }
    List<String> samplers() { return samplers; }
    ByteBuffer parameters() {
        var color = getUniform("ColorModulator");
        if(color != null && !screen) color.set(RenderSystem.color);
        if(!layouts.isEmpty()) {
            ByteBuffer data=MemoryUtil.memCalloc(blockSize);
            for(var layout:layouts)uniforms.get(layout.name).writeAt(data,layout.offset,layout.components,layout.length,layout.stride);
            return data;
        }
        ByteBuffer data = MemoryUtil.memAlloc(uniforms.values().stream().mapToInt(GlUniform::size).sum());
        uniforms.values().forEach(v -> v.write(data));
        return data.flip();
    }
    private record Variant(VertexFormat format, VertexFormat.DrawMode mode, boolean depth, boolean depthWrite, boolean cull, boolean blend,
            com.mojang.blaze3d.platform.DepthTestFunction depthFunction, com.mojang.blaze3d.pipeline.BlendFunction blendFunction) {}
    RenderPipeline pipeline(VertexFormat format, VertexFormat.DrawMode mode) {
        var variant = new Variant(format,mode,RenderSystem.depth,RenderSystem.depthWrite,RenderSystem.cull,RenderSystem.blend,RenderSystem.depthFunction,RenderSystem.blendFunction);
        return variants.computeIfAbsent(variant, v -> {
            var builder = RenderPipeline.builder().withLocation(Identifier.of("astolfoclient", "pipeline/" + key.configId().getPath().replace('/','_') + "_" + variants.size()))
                    .withVertexShader(key.configId()).withFragmentShader(key.configId())
                    .withVertexFormat(format,mode)
                    .withDepthTestFunction(v.depth ? v.depthFunction : com.mojang.blaze3d.platform.DepthTestFunction.NO_DEPTH_TEST)
                    .withDepthWrite(v.depthWrite).withCull(v.cull);
            if(v.blend) builder.withBlend(v.blendFunction); else builder.withoutBlend();
            if(!screen) builder.withUniform("Projection",UniformType.UNIFORM_BUFFER).withUniform("DynamicTransforms",UniformType.UNIFORM_BUFFER);
            if(!uniforms.isEmpty()) builder.withUniform("EffectData",UniformType.UNIFORM_BUFFER);
            samplers.forEach(builder::withSampler);
            return builder.build();
        });
    }
}
