# 26.3 verification

## HUD editor correction — port.2

Release `2.0-port.2+26.3` adds screen mouse input independent of Minecraft's gameplay button flags, retained quick clicks, direct watermark drag events, foreground settings panels and click capture. The effects HUD now shows editor placeholders even when disabled for gameplay.

Verified in the local OpenGL development client:

- Watermark: right-click panel opening, avatar toggle, individual section toggle, bottom-right/top-left positioning, closing panels and quick section reordering. A quick drag moved Server Address after Framerate.
- HUD element dragging: moved target HUD and keybinds to new positions.
- Armor: opened settings and changed Vertical to Horizontal; the preview updated.
- Effects: placeholders visible with the gameplay setting disabled; opened the settings/submenu and changed Left to Right; previews moved to the right edge.
- Settings panels and submenus rendered above overlapping HUD previews. Clicks in their bounds were captured before background element dragging.
- Final client session (`migration/hud-game-test-4.log`) closed normally with no shader/pipeline errors or render exceptions found.

Automated checks: `VerifyHudMouseInput.java` passed quick-click retention, shared frame state, held buttons, release, screen changes and focus-loss cases. Mixin audit reported zero unresolved selectors. Final `build verifyShaders --offline` succeeded with all 90 shader compilations passing (`migration/build-hud-final.log`). Packaged JAR version verified: port.2; 436 Java 25 classes, three embedded dependencies, 257 Java files in the sources JAR.

## Base renderer verification — port.1

Verified on Windows using Java 25, Minecraft 26.3, Fabric Loader 0.20.1 and Fabric API 0.165.2+26.3. Test backend: OpenGL. Release version: 2.0-port.1+26.3.

## Build and automated checks

- `build verifyShaders --offline`: BUILD SUCCESSFUL. All 90 shader stage compilations passed with explicit locations; shared parameter interfaces and GUI quad coordinates passed validation.
- Configured mixin target/selector audit: zero unresolved selectors, including the new world-pass hook.
- `tools/VerifyWorldPass.java`: pass closure, pipeline/uniform/scissor/debug-group restoration, exception recovery and SDL key/mouse/modifier translation passed.
- Packaged JAR: 433 class files, Java class version 69 (Java 25), three embedded dependencies and the new world-pass implementation. Sources JAR contains 255 Java files.
- Preserved 26.2 snapshot: all 665 files in its source checksum manifest matched; no changes.

## Game checks

Launched a development client and created a fresh local creative world. Final session log: `migration/game-test-8.log`.

- First-person hand shader remained confined to the hand, with glow/glass/blur settings enabled. Returned to first person after third-person testing and resource reload.
- Sky shader, sphere blur, rain, HUD and hat rendered together. Rain and ambient effects could be toggled back off, returning to the normal world view.
- Hat rendered in rear and front third person. Scene texture uploads and custom effects no longer attempted to open nested passes inside the native world pass.
- Resource reload (F3+T) completed with the combined effects enabled; rendering continued afterward.
- Spawned a cow on a stone test platform and attacked it twice. TargetESP animated with distortion, target HUD updated health, HitGlow particles appeared and the nearby platform received pink glow. The scene continued rendering without a frozen frame.
- ClickGUI opened through the translated K test bind. Search accepted text, the hand fill selector worked, and Escape handled search/menu input. Clear menu backgrounds were retained.
- Final session contained no shader/pipeline errors or IllegalStateException. Closed normally and saved the local world before the release build.

## Limits

This was a local OpenGL smoke test, not exhaustive coverage of every visual style, resolution, swing animation or multiplayer interaction. JumpCircle's individual jump animation was not visually confirmed. Native Vulkan, Iris and third-party shader packs were not tested. Existing missing audio assets and offline profile/Realms or operating-system performance-counter warnings remain unrelated to these renderer checks.

The development run configuration and world are test data, not part of the release JAR. Use the default menu binds (Insert or right Shift) in a fresh installation.
