# Astolfo Visuals — Fabric 26.2 source port

Independent port of the recovered Astolfo Visuals client. The completed 1.21.11 source is preserved on the [1.21.11 branch](https://github.com/SRS-0/AstolfoVisuals/tree/1.21.11).

## Install

Use Minecraft 26.2, Java 25, Fabric Loader 0.20.1 or newer, and Fabric API for 26.2 (built against 0.161.0+26.2). Build the project and put the installable JAR from build/libs/ in your instance's mods folder. Install only one Astolfo Visuals version. The sources JAR is for developers.

Insert or right Shift opens the menu. The development profile uses K. Clear screen backgrounds are retained.

## 26.2 port.2 rendering fixes

World post effects now capture the current scene immediately, independently of GUI extraction. Post effects sample scene depth without binding the same texture as a depth attachment. HitGlow runs after opaque terrain is drawn. Sky shaders use reversed depth and the device clip-depth range. GUI effects carry explicit quad-local coordinates, and the watermark scales down to fit small windows.

## Migration

- Migrated Yarn names to Mojang names and moved to unobfuscated Fabric Loom with separate client/common source sets.
- Updated GUI extraction, HUD registration, Minecraft GUI ownership, level render events, render states, entity layers, inputs, clocks, item submission and mixin targets for 26.2.
- Updated GPU pipeline layouts, vertex formats, bind groups, texture formats and indexed drawing to the 26.2 graphics API.
- Updated effect depth comparisons, near-plane clipping and hand depth clears for reversed depth. Scene-depth shaders account for the device's clip-depth range.
- Moved direct shader-hat drawing to a registered feature renderer execution phase.
- Preserved deferred GUI panels, MSDF text, clipping, scene sampling, shader effects and triangle-based wide lines.
- Hand capture uses an explicit transparent RGBA texture, flushes queued geometry while that target is active, then restores the previous attachments before composition.

Effects run through Minecraft's GPU API. Java still prepares geometry, settings and simulation; this is not a GPU compute rewrite of the entire client. OpenGL is the tested backend. Native Vulkan and third-party renderer/shader-pack compatibility are not claimed without separate testing.

## Build

Select JDK 25 and run:

    .\gradlew.bat build verifyShaders
    .\gradlew.bat runClient

verifyShaders compiles 90 shader stages against the 26.2 include resources. Its validator is configured for Windows x64. tools/AuditMixins.java checks configured target classes, selectors, accessors, shadows and injection signatures. See TEST-RESULTS.md and migration/ for verification evidence.

## Provenance and limits

This is reconstructed decompiled source, not the original authoring project. Original authorship and GPL-3.0-only metadata are preserved, along with original resources and embedded dependencies. The original JAR remains at ../astolfovisuals-2.0(1).jar; SHA-256: 97e26ddf6188e50f39db872d40f4d5e4038b5c62ae423d093f7fd4a2e22d6413.

Not every module, custom player model, multiplayer interaction or setting combination has been visually verified. Original missing audio assets remain missing. Offline development users produce expected Realms/profile authorization errors. Local OS performance-counter warnings are unrelated to the mod renderer.
