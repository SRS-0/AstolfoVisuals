# OpenGL verification — port.2, 2026-11-03

Target: Minecraft 1.21.11, Fabric Loader 0.19.3, Fabric API 0.141.6+1.21.11, Windows, normal Minecraft OpenGL renderer. Tests ran with JDK 25 and Java 21 output bytecode.

## Build and static checks

- Complete Gradle build/remap succeeds. Final artifact: `../dist/astolfovisuals-2.0-port.2+1.21.11.jar`.
- All 90 OpenGL shader-stage compilations pass; zero failures. Screen-pass vertex attribute names are checked against the supplied mesh attributes.
- Configured mixin audit: zero unresolved selectors. Packaged hand-render mixin selectors were also inspected and correctly remapped to intermediary names.
- The final client log contains no rendering ERROR entries or shader compilation failures during the checks below.

## In-game checks

| Check | Observed result |
| --- | --- |
| Startup and local creative world | Client loads and enters the world. |
| Mod menu | Panels, MSDF text, icons, clipped content, search, toggles and mode popup render; the menu fits the 856×512 test window. |
| Hand glass/blur and outer bloom | Effect is confined to the hand. Toggling it off restores the ordinary hand while the scene stays unchanged. |
| Hand animated Fill1 | Animated fill is visible on the hand without covering the scene. |
| Hand Fill3, motion and inner glow | Reflection fill and glow remain on the hand; motion history follows hand/item movement and settles. |
| Resource reload | F3+T completes and the hand effect continues rendering. |
| ChinaHat | Shader hat is visible above the player from both rear and front third-person views; switching back to first person resumes the hand effect. |
| Animated HUD image | Animation is visible and updates in the upper-left HUD. |
| Projectile trajectory and timer | Holding an ender pearl shows the pink predicted path and a legible circular landing timer (observed at 0.9 s). |
| Block fill | Before the coordinate fix the fill was mostly black. After the fix, the animated pink/white pulse renders on the selected block. |
| Multiple effects together | Hand effect, block fill, hat, projectile overlay and HUD image were exercised in the same local test session. |

## Fixes verified or reviewed

The hand bloom pass previously used ONE/ONE additive blending while its RGB output was not premultiplied by the mask. This added color across the full-screen quad even where mask alpha was zero. It now uses SRC_ALPHA/ONE for color and preserves destination alpha. All six hand vertex shaders now bind `UV0`, matching the actual mesh, instead of the nonexistent `UV` attribute. Hand blur preserves the mask alpha, and capture restores the previous render targets in a finally block after queued hand geometry is flushed.

Block and ragdoll fill centers now pass through the same position transform as their vertices. The block fix was visually checked. The ragdoll correction shares the same mathematical issue but was not exercised with an entity death in this session.

Projectile timer text and the animated HUD image use the native deferred GUI renderer. ChinaHat drawing is submitted through the ordered entity command queue. Thick lines are expanded into quads, including near-plane clipping, rather than relying on legacy line width state.

## Limits

This is a development-client test, not an exhaustive test of every module and setting combination. Fill2, every other effect mode, entity-death effects, all custom player models, multiplayer behavior, Iris and third-party shader packs have not been fully verified. Jump-circle distortion compiled, but a distinct ring was not confirmed in the final screenshot. Some original sound-event entries refer to audio absent from the supplied original JAR.

VulkanMod compatibility is outside the current scope at the user's request. The Vulkan-specific compatibility mixin and development launch dependency were removed.

The test settings and creative world are confined to `port/run`; they are not included in the installable JAR.

## Logs

`build-test.log`: successful full build plus OpenGL shader validation.
`mixin-audit.log`: static mixin check.
`opengl-hand-test.log`: hand mask/fill and resource reload session.
`opengl-combined-test.log`: combined hat/HUD/hand/projectile session.
`game-test.log`: final port.2 build and corrected block-fill session.

## port.3 clear screen backgrounds

Removed Screen.applyBlur, both renderDarkening overloads, and renderInGameBackground through explicit cancellable mixin targets. This removes shared full-screen blur and dimming while retaining menu controls and the underlying scene/panorama.

The complete port.3 build succeeds. Static mixin audit reports zero unresolved selectors. All four packaged background method targets were inspected and correctly remapped to intermediary names. This small screen-background change was not separately checked in a live client; the earlier port.2 in-game results above describe the unchanged renderer/effects.
