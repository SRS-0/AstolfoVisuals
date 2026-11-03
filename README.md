# Astolfo Visuals — Fabric 1.21.11 source port

The recovered client compiles and an installable remapped JAR is produced by `build`. This is a reconstructed source port, with ongoing in-game verification; it is not a claim that every visual mode is identical to the original.

## Install

Use Minecraft **1.21.11**, Fabric Loader **0.19.3 or newer**, Fabric API for **1.21.11**, and Java **21 or newer**. Install `build/libs/astolfovisuals-2.0-port.3+1.21.11.jar` in the instance's `mods` folder. Do not also install the original 1.21.4 JAR. The `-sources.jar` is for source inspection, not installation.

Insert or right Shift opens the mod menu. The local command `$bind add clickgui K` changes the shortcut. This port targets the normal OpenGL renderer. No additional rendering mod is required. VulkanMod compatibility is outside the current scope.

## What changed

- Removed the shared screen backdrop blur, darkening texture, and in-game gradient. Menus and the HUD editor keep a clear view behind their controls.

- Remapped and decompiled the original 1.21.4 classes, repaired decompiler errors, and migrated Minecraft/Fabric APIs, inputs, render events, entity states, models, inventory, skins, and mixins to 1.21.11.
- Replaced active direct OpenGL effect paths with Minecraft GPU pipelines, explicit vertex/index buffers, texture views, samplers, render targets, and uniform buffers.
- Migrated rounded panels, borders, textures, MSDF text, blur, and liquid glass to the deferred GUI renderer. Per-element parameters are vertex attributes so Minecraft can batch the elements while preserving order and clipping. The menu fits smaller windows.
- Corrected block and ragdoll fill centers to use the same coordinate space as the uploaded vertices.
- Fixed hand bloom blending to respect mask opacity, matched texture-coordinate attributes to the mesh, preserved antialiased hand-mask edges, and restored the previous render target even when capture fails.
- Migrated the projectile timer text and animated HUD image to native deferred GUI drawing.
- Migrated world-effect shaders, ambient sky/fog effects, hit/target/jump distortion, wet surfaces, and hand glass/blur/motion/bloom to backend-neutral render passes. Hand command queues are executed before their capture target is composited.
- Replaced legacy line widths with triangle geometry so wide outlines and trails do not require backend-specific line-width support.
- Added reusable scene copies for effects that sample the scene, with resize/shutdown cleanup. Converted the depth-blur post-effect resource format to 1.21.11.
- Preserved original resources, embedded dependencies, GPL-3.0-only metadata, module configuration, and optional integrations. IAS is optional and detected reflectively.

Fragment shading and effects execute on the GPU. Java still prepares geometry, settings, and particle state. This port does not move the whole Minecraft client or particle simulation into GPU compute shaders.

## Build and checks

Select a JDK 21+ and run from this directory:

```powershell
.\gradlew.bat build verifyShaders
.\gradlew.bat runClient
```

`verifyShaders` performs 90 OpenGL shader-stage compilation checks, including the deferred GUI variants and depth-blur fragment shader. Minecraft includes are expanded from the mapped game resources. OpenGL SPIR-V validation raises GLSL 150 to 330 for the validator; shipped sources retain GLSL 150. The validation runtime is configured for Windows x64.

`tools/AuditMixins.java` checks configured target classes, shadows, accessors, invokers, injection argument signatures, and invoked method selectors against the mapped game JAR. `mixin-audit.log` records the latest check. This static audit supplements real startup checks.


## Recovered source and provenance

The original `../astolfovisuals-2.0(1).jar`, extracted `../src`, and initial `../recovered` decompilation are preserved. CFR 0.152 recovered 230 top-level Java source files from 402 class files; selected classes were recovered again with Vineflower 1.11.1. Original intermediary names were remapped using Yarn 1.21.4 build 8 before the port to Yarn 1.21.11 build 6. Decompiled source is not the original authoring project.

Original JAR SHA-256: `97e26ddf6188e50f39db872d40f4d5e4038b5c62ae423d093f7fd4a2e22d6413`.

## Verification scope and limitations

The complete build, shader compiler checks, and static mixin audit pass. OpenGL startup, entry into a local creative world, and visible mod menu rendering have been exercised. Further effect testing is recorded in `TEST-RESULTS.md` as it completes.

Not every module, setting combination, custom player model, multiplayer interaction, or third-party shader pack has been visually verified. This does not establish Iris/shader-pack compatibility. Some sound events in the original resource manifest reference audio files absent from the supplied original JAR; those sounds remain unavailable. Development sessions use Fabric's offline test user, so Realms authorization messages are expected.
