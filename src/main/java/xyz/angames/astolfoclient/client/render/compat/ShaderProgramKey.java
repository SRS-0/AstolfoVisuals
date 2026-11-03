package xyz.angames.astolfoclient.client.render.compat;

import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.ShaderDefines;
import net.minecraft.resources.Identifier;

/** Describes an effect; draw-state variants are materialized as immutable pipelines. */
public record ShaderProgramKey(Identifier configId, VertexFormat vertexFormat, ShaderDefines defines) {}
