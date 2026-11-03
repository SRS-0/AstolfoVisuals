# Fabric 26.2 verification — 2026-11-03

Target: Minecraft 26.2, Fabric Loader 0.20.1, Fabric API 0.161.0+26.2, JDK 25, Windows, OpenGL on NVIDIA GeForce RTX 3080 Laptop GPU.

## Final checks

- Gradle build succeeds. Installable artifact: release/astolfovisuals-2.0-port.2+26.2.jar.
- 90 shader-stage compilation checks pass; zero failures.
- Static audit of configured mixins reports zero unresolved selectors.
- Release metadata declares Minecraft 26.2 and Java 25. Packaged classes use bytecode major 69.
- Release includes 427 class files and all three embedded dependency JARs. The sources JAR contains all 250 Java source files.

## Observed in the development client

| Check | Result |
| --- | --- |
| Startup and new creative world | Loads and enters a fresh 26.2 world; the earlier 1.21.11 save was not upgraded. |
| Screen backgrounds | World list, world creation and mod menu retain a clear background. Shared screen blur/darkening cancellations load correctly. |
| Mod menu | Panels, text, icons, scrolling content, toggles, search and mode popup render and respond. |
| Hand Normal/glass, blur and outer glow | Effect stays on the hand. Toggling the module restores the ordinary hand. |
| Hand Fill1 | Animated pink fill stays on the hand and held ender pearl; scene remains clear. |
| Shader hat | Pink shader hat renders above the player in rear and front third-person views. |
| Animated HUD image | Visible and animated in the upper-left corner. |
| Block pulse | Pink/white pulse renders on the selected leaf block. |
| Projectile overlay | Holding an ender pearl displays its pink trajectory, landing ring and legible timer (observed 0.1 s). |
| Resource reload | F3+T finishes; hand effect, block effect and HUD continue rendering. |
| Combined effects | Hand/held-item shader, block pulse, trajectory and animated HUD render in the same session. |

## Bugs found and corrected during 26.2 testing

The new clear-color API originally received a default Vector4f, whose W value is one. That made the entire hand capture opaque and spread the effect across the screen. It now clears explicitly to (0,0,0,0) in an RGBA texture. Temporary GPU readback measured 14,280 nonzero-alpha pixels out of 409,920, with alpha zero at the center and corner; before the correction all 409,920 pixels were opaque. Diagnostic code was removed before the final build.

Hand depth clears now use zero, and legacy effect comparisons are inverted for 26.2 reversed depth. Near-plane clipping uses the reversed near plane. Active scene-depth shaders recognize zero-depth sky pixels and use the device's clip-depth range when reconstructing positions.

26.2 custom-geometry callbacks run during preparation. Direct shader-hat drawing there was erased by subsequent rendering. The hat now uses a registered feature renderer and draws during execution. Both third-person views were checked after this correction.

## Scope

These are development-client OpenGL checks. Native Vulkan, Iris, shader packs, multiplayer, every mode/setting combination, entity-death effects and every custom player model have not been visually verified. Fill2, Fill3, motion and inner glow are compiled but were not separately exercised in the final 26.2 session. Earlier 1.21.11 results do not establish 26.2 behavior.

Logs contain local OS performance-counter warnings and offline-profile/Realms authorization errors. No render-thread shader compilation or mixin failure occurred in the final world/reload test.

## Evidence

- migration/build-final.log — full build and 90 shader-stage checks.
- migration/mixin-audit.log — zero unresolved selectors.
- migration/game-final-test.log — final world, effects and resource-reload session; clean shutdown.
- migration/game-transparent-mask.log — corrected mask diagnostic.
- migration/game-mask-diagnostic.log — pre-fix opaque mask diagnostic.

The local test world and configuration in run/ are not included in the release JAR.

## port.2 regression fixes

- World scene copies are immediate. GUI blur/glass explicitly request their deferred scene copy; GUI extraction ends before world rendering, and pending scene requests survive additional extractors. This removes stale-frame feedback in rain, sphere blur, jump and target distortion.
- Full-screen effects no longer attach the scene depth texture while sampling it.
- HitGlow moved from COLLECT_SUBMITS (preparation) to AFTER_OPAQUE_TERRAIN (drawing). A local cow hit visibly illuminated nearby block faces.
- Procedural sky depth and comparisons follow reversed depth and the device clip-depth range. The animated pink cloud sky and sphere blur rendered together with a live world scene.
- Rain was toggled independently of Ambients. Terrain kept its original colors; wet reflections and ripples appeared on upward surfaces.
- TargetESP Circle distortion animated around the local target while scene and HUD continued updating.
- GUI quads now carry local coordinates explicitly rather than deriving them from the batch vertex index. Menu controls, text, toggles, category filtering and HUD editor previews were exercised with world effects enabled.
- All nine Interface options were enabled together. HUD panels and text rendered; independent widget positions remain user-configurable in the HUD editor. The long watermark now fits the viewport.
- Shader validation also checks matching EffectData interfaces between stages and explicit local coordinates in GUI vertex shaders. All 90 stage compilations pass. Static mixin audit reports zero unresolved selectors.

Evidence: migration/effect-fixes-build.log, migration/effect-fixes-mixins.log, migration/effect-fixes-game.log and migration/effect-fixes-final-game.log.

Final port.2 viewport check: the complete watermark, including RAM, username and avatar, fits the 854-pixel client area. HUD panels continued rendering with TargetESP active. JumpCircle uses the corrected scene/depth path and its shaders pass validation; a distinct jump-circle ring was not independently confirmed visually in this session. This is not an exhaustive check of every HUD style, setting combination or native Vulkan.
