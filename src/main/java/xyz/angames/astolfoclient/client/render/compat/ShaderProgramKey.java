package xyz.angames.astolfoclient.client.render.compat;

import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.gl.Defines;
import net.minecraft.util.Identifier;

/** Describes an effect; draw-state variants are materialized as immutable pipelines. */
public record ShaderProgramKey(Identifier configId, VertexFormat vertexFormat, Defines defines) {}
