package xyz.angames.astolfoclient.client;

import com.google.common.eventbus.EventBus;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents.ClientStopping;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents.EndTick;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents.Disconnect;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents.Join;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.LivingEntityRenderLayerRegistrationCallback;
import xyz.angames.astolfoclient.client.compat.WorldRenderEvents;
import xyz.angames.astolfoclient.client.compat.WorldRenderEvents.AfterTranslucent;
import xyz.angames.astolfoclient.client.compat.WorldRenderEvents.Last;
import xyz.angames.astolfoclient.client.compat.ShaderProgram;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import net.minecraft.resources.Identifier;
import net.minecraft.client.Minecraft;
import xyz.angames.astolfoclient.client.command.CommandManager;
import xyz.angames.astolfoclient.client.config.ConfigManager;
import xyz.angames.astolfoclient.client.effects.BlockOutlineRenderer;
import xyz.angames.astolfoclient.client.effects.ChinaHatFeatureRenderer;
import xyz.angames.astolfoclient.client.effects.CircleEspManager;
import xyz.angames.astolfoclient.client.effects.CircleEspRenderer;
import xyz.angames.astolfoclient.client.effects.DamageIndicatorManager;
import xyz.angames.astolfoclient.client.effects.DamageIndicatorRenderer;
import xyz.angames.astolfoclient.client.effects.DiamondEspManager;
import xyz.angames.astolfoclient.client.effects.DiamondEspRenderer;
import xyz.angames.astolfoclient.client.effects.GhostEspManager;
import xyz.angames.astolfoclient.client.effects.GhostEspRenderer;
import xyz.angames.astolfoclient.client.effects.HitEspManager;
import xyz.angames.astolfoclient.client.effects.HitEspRenderer;
import xyz.angames.astolfoclient.client.effects.JumpCircleManager;
import xyz.angames.astolfoclient.client.effects.JumpCircleRenderer;
import xyz.angames.astolfoclient.client.effects.KillEffectManager;
import xyz.angames.astolfoclient.client.effects.KillEffectRenderer;
import xyz.angames.astolfoclient.client.effects.LineGlyphsRenderer;
import xyz.angames.astolfoclient.client.effects.ParticleManager;
import xyz.angames.astolfoclient.client.effects.ParticleRenderer;
import xyz.angames.astolfoclient.client.effects.RagdollRenderer;
import xyz.angames.astolfoclient.client.effects.TargetEspManager;
import xyz.angames.astolfoclient.client.effects.TargetEspRenderer;
import xyz.angames.astolfoclient.client.effects.TrailsRenderer;
import xyz.angames.astolfoclient.client.gui.HudEditorScreen;
import xyz.angames.astolfoclient.client.hud.ActiveBindsManager;
import xyz.angames.astolfoclient.client.hud.ArmorHudManager;
import xyz.angames.astolfoclient.client.hud.ArrayListManager;
import xyz.angames.astolfoclient.client.hud.BoyKisserManager;
import xyz.angames.astolfoclient.client.hud.CrosshairManager;
import xyz.angames.astolfoclient.client.hud.CustomHotbarManager;
import xyz.angames.astolfoclient.client.hud.EffectHudManager;
import xyz.angames.astolfoclient.client.hud.HudRenderer;
import xyz.angames.astolfoclient.client.hud.InfoHudManager;
import xyz.angames.astolfoclient.client.hud.InventoryHudManager;
import xyz.angames.astolfoclient.client.hud.MusicHudManager;
import xyz.angames.astolfoclient.client.hud.ScoreboardManager;
import xyz.angames.astolfoclient.client.hud.TargetHudManager;
import xyz.angames.astolfoclient.client.hud.TestHudManager;
import xyz.angames.astolfoclient.client.module.Module;
import xyz.angames.astolfoclient.client.module.ModuleManager;
import xyz.angames.astolfoclient.client.module.modules.TargetEspModule;
import xyz.angames.astolfoclient.client.module.modules.render.CubeParticlesModule;
import xyz.angames.astolfoclient.client.module.modules.render.InterfaceModule;
import xyz.angames.astolfoclient.client.protection.ClientProtectionManager;
import xyz.angames.astolfoclient.client.render.GpsRenderer;
import xyz.angames.astolfoclient.client.render.TrajectoriesRenderer;
import xyz.angames.astolfoclient.client.util.FakePlayerEntity;
import xyz.angames.astolfoclient.client.util.ModSounds;
import xyz.angames.astolfoclient.client.util.Render3DUtil;
import xyz.angames.astolfoclient.client.util.TargetUtils;
import xyz.angames.astolfoclient.client.util.TimerManager;

@Environment(EnvType.CLIENT)
public class AstolfoclientClient implements ClientModInitializer {
   private static AstolfoclientClient instance;
   private final String clientName = "Astolfo Visuals";
   private final String version = "2.0";
   private final String bio = "https://fakecrime.bio/SRS";
   public static final EventBus EVENT_BUS = new EventBus();
   public static final boolean isUnloaded = false;
   public static Path originalResourcePackDir = null;
   public static Path originalModsDir = null;
   public static TimerManager timerManager;
   public static ModuleManager moduleManager;
   public static ConfigManager configManager;
   public static CommandManager commandManager;
   public static BlockOutlineRenderer blockOutlineRenderer;
   public static HudRenderer hudRenderer;
   public static TargetHudManager targetHudManager;
   public static InventoryHudManager inventoryHudManager;
   public static ArmorHudManager armorHudManager;
   public static CustomHotbarManager customHotbarManager;
   public static ScoreboardManager scoreboardManager;
   public static EffectHudManager effectHudManager;
   public static ArrayListManager arrayListManager;
   public static CrosshairManager crosshairManager;
   public static BoyKisserManager boyKisserManager;
   public static TestHudManager testHudManager;
   public static InfoHudManager infoHudManager;
   public static LineGlyphsRenderer lineGlyphsRenderer;
   public static DamageIndicatorManager damageIndicatorManager;
   public static DamageIndicatorRenderer damageIndicatorRenderer;
   public static JumpCircleManager jumpCircleManager;
   public static TargetEspManager targetEspManager;
   public static HitEspManager hitEspManager;
   public static ParticleManager particleManager;
   public static GhostEspManager ghostEspManager;
   public static KillEffectManager killEffectManager;
   public static CircleEspManager circleEspManager;
   public static DiamondEspManager diamondEspManager;
   public static MusicHudManager musicHudManager;
   public static GpsRenderer gpsRenderer;
   public static RagdollRenderer ragdollRenderer;
   public static JumpCircleRenderer jumpCircleRenderer;
   public static TargetEspRenderer targetEspRenderer;
   public static HitEspRenderer hitEspRenderer;
   public static ParticleRenderer particleRenderer;
   public static GhostEspRenderer ghostEspRenderer;
   public static KillEffectRenderer killEffectRenderer;
   public static TrailsRenderer trailsRenderer;
   public static CircleEspRenderer circleEspRenderer;
   public static DiamondEspRenderer diamondEspRenderer;
   public static TrajectoriesRenderer trajectoriesRenderer;
   public static ActiveBindsManager activeBindsManager;
   public static final ShaderProgram CHAMS_OUTLINE_SHADER = new ShaderProgram(Identifier.fromNamespaceAndPath("astolfoclient", "core/chams_outline"), DefaultVertexFormat.POSITION_TEX);
   public static final ShaderProgram LIQUID_GLASS_SHADER = new ShaderProgram(Identifier.fromNamespaceAndPath("astolfoclient", "core/liquid_glass"), DefaultVertexFormat.ENTITY);
   public static final ShaderProgram COSMOS_FILL_SHADER = new ShaderProgram(Identifier.fromNamespaceAndPath("astolfoclient", "core/cosmos_fill"), DefaultVertexFormat.POSITION_TEX_COLOR);
   public static final ShaderProgram CHINA_HAT_SHADER = new ShaderProgram(Identifier.fromNamespaceAndPath("astolfoclient", "core/china_hat"), DefaultVertexFormat.POSITION_TEX_COLOR);
   public static final ShaderProgram BLOCK_OUTLINE_SHADER = new ShaderProgram(Identifier.fromNamespaceAndPath("astolfoclient", "core/block_outline"), DefaultVertexFormat.POSITION_TEX_COLOR);
   private DiscordRpcManager discordRpcManager;
   public static int clickGuiKeyCode = 260;
   public static int hudEditorKeyCode = 79;

   public static Path getUnloadedResourcePackDir() {
      try {
         String appData = System.getenv("APPDATA");
         Path customPath;
         if (appData != null && !appData.isEmpty()) {
            customPath = Path.of(appData, ".minecraft", "resourcepacks");
         } else {
            customPath = Path.of(System.getProperty("user.home"), "AppData", "Roaming", ".minecraft", "resourcepacks");
         }

         if (!Files.exists(customPath)) {
            Files.createDirectories(customPath);
         }

         return customPath;
      } catch (Exception ignored) {
         return Path.of("resourcepacks");
      }
   }

   public static Path getUnloadedModsDir() {
      String appData = System.getenv("APPDATA");
      Path customPath;
      if (appData != null && !appData.isEmpty()) {
         customPath = Path.of(appData, ".minecraft", "mods");
      } else {
         customPath = Path.of(System.getProperty("user.home"), "AppData", "Roaming", ".minecraft", "mods");
      }

      try {
         Files.createDirectories(customPath);
      } catch (Exception var3) {
      }

      return customPath;
   }

   public static AstolfoclientClient getInstance() {
      return instance;
   }

   public String getClientName() {
      return "Astolfo Visuals";
   }

   public String getVersion() {
      return "2.0";
   }

   public void onInitializeClient() {
      dev.sxmurxy.mre.utils.CRenderPipelines.init();
      instance = this;
      ModSounds.register();
      moduleManager = new ModuleManager();
      commandManager = new CommandManager();
      configManager = new ConfigManager();
      lineGlyphsRenderer = new LineGlyphsRenderer();
      timerManager = new TimerManager();
      activeBindsManager = new ActiveBindsManager();
      blockOutlineRenderer = new BlockOutlineRenderer();
      jumpCircleManager = new JumpCircleManager();
      targetEspManager = new TargetEspManager();
      hitEspManager = new HitEspManager();
      particleManager = new ParticleManager();
      ghostEspManager = new GhostEspManager();
      killEffectManager = new KillEffectManager();
      circleEspManager = new CircleEspManager();
      diamondEspManager = new DiamondEspManager();
      musicHudManager = new MusicHudManager();
      trajectoriesRenderer = new TrajectoriesRenderer();
      testHudManager = new TestHudManager();
      infoHudManager = new InfoHudManager();
      hudRenderer = new HudRenderer();
      targetHudManager = new TargetHudManager();
      damageIndicatorManager = new DamageIndicatorManager();
      damageIndicatorRenderer = new DamageIndicatorRenderer(damageIndicatorManager);
      inventoryHudManager = new InventoryHudManager();
      armorHudManager = new ArmorHudManager();
      customHotbarManager = new CustomHotbarManager();
      scoreboardManager = new ScoreboardManager();
      effectHudManager = new EffectHudManager();
      arrayListManager = new ArrayListManager();
      crosshairManager = new CrosshairManager();
      boyKisserManager = new BoyKisserManager();
      jumpCircleRenderer = new JumpCircleRenderer(jumpCircleManager);
      targetEspRenderer = new TargetEspRenderer(targetEspManager);
      hitEspRenderer = new HitEspRenderer(hitEspManager);
      particleRenderer = new ParticleRenderer(particleManager);
      ghostEspRenderer = new GhostEspRenderer(ghostEspManager);
      killEffectRenderer = new KillEffectRenderer(killEffectManager);
      circleEspRenderer = new CircleEspRenderer(circleEspManager);
      diamondEspRenderer = new DiamondEspRenderer(diamondEspManager);
      trailsRenderer = new TrailsRenderer();
      this.discordRpcManager = new DiscordRpcManager();
      gpsRenderer = new GpsRenderer();
      ragdollRenderer = new RagdollRenderer();
      CompletableFuture.runAsync(() -> {
         try {
            if (configManager != null) {
               configManager.loadFriends();
               configManager.loadConfig("default");
            }
         } catch (Throwable t) {
            System.err.println("[Config] Config load error: " + t.getMessage());
         }

         try {
            if (this.discordRpcManager != null) {
               this.discordRpcManager.start();
            }
         } catch (Throwable t) {
            System.err.println("[DiscordRPC] Start error: " + t.getMessage());
         }
      });
      ClientLifecycleEvents.CLIENT_STOPPING.register((ClientStopping)client -> {
         if (configManager != null) {
            configManager.saveFriends();
         }

         if (this.discordRpcManager != null) {
            this.discordRpcManager.stop();
         }
      });
      ClientPlayConnectionEvents.JOIN.register((Join)(handler, sender, client) -> {
         if (this.discordRpcManager != null) {
            this.discordRpcManager.update();
         }
      });
      ClientPlayConnectionEvents.DISCONNECT.register((Disconnect)(handler, client) -> {
         if (this.discordRpcManager != null) {
            this.discordRpcManager.update();
         }
      });
      ClientTickEvents.END_CLIENT_TICK.register((EndTick)client -> {
         xyz.angames.astolfoclient.client.compat.PipelineSelfTest.runOnce(client);
         try {
            ClientProtectionManager.getInstance().tick();
         } catch (Exception var16) {
         }

         if (FakePlayerEntity.instance != null && (client.level == null || FakePlayerEntity.instance.level() != client.level)) {
            try {
               FakePlayerEntity.instance.discard();
            } catch (Exception var15) {
            }

            FakePlayerEntity.instance = null;
         }

         if (client.getWindow() != null) {
            client.getWindow().setTitle("Astolfo Visuals 2.0 | https://fakecrime.bio/SRS");
         }

         for (Module module : moduleManager.getModules()) {
            try {
               module.onTick();
            } catch (Exception var14) {
            }
         }

         if (client.player != null && client.level != null) {
            if (!(client.gui.screen() instanceof HudEditorScreen)) {
               try {
                  LivingEntity lookedTarget = TargetUtils.getLookedAtTarget(client, 40.0);
                  if (lookedTarget != null) {
                     if (targetHudManager != null && isModuleEnabled("TargetHUD")) {
                        targetHudManager.setTarget(lookedTarget);
                     }

                     if (isModuleEnabled("TargetESP")) {
                        TargetEspModule.addTargetEffect(lookedTarget);
                     }
                  }
               } catch (Exception var13) {
               }
            }

            try {
               jumpCircleManager.tick();
            } catch (Exception var12) {
            }

            try {
               targetEspManager.tick();
            } catch (Exception var11) {
            }

            try {
               hitEspManager.tick();
            } catch (Exception var10) {
            }

            try {
               particleManager.tick();
            } catch (Exception var9) {
            }

            try {
               ghostEspManager.tick();
            } catch (Exception var8) {
            }

            try {
               killEffectManager.tick();
            } catch (Exception var7) {
            }

            try {
               circleEspManager.tick();
            } catch (Exception var6) {
            }

            try {
               diamondEspManager.tick();
            } catch (Exception var5) {
            }
         }
      });
      LivingEntityRenderLayerRegistrationCallback.EVENT
         .register((LivingEntityRenderLayerRegistrationCallback)(entityType, entityRenderer, registrationHelper, context) -> {
            if (entityType == EntityTypes.PLAYER) {
               registrationHelper.register(new ChinaHatFeatureRenderer((RenderLayerParent<AvatarRenderState, PlayerModel>)entityRenderer));
            }
         });
      WorldRenderEvents.AFTER_TRANSLUCENT.register((AfterTranslucent)context -> gpsRenderer.render(context));
      WorldRenderEvents.LAST.register((Last)context -> {
         try {
            lineGlyphsRenderer.render(context);
         } catch (Exception var17) {
         }

         try {
            blockOutlineRenderer.render(context);
         } catch (Exception var16) {
         }

         try {
            damageIndicatorRenderer.render(context);
         } catch (Exception var15) {
         }

         try {
            jumpCircleRenderer.render(context);
         } catch (Exception var14) {
         }

         try {
            targetEspRenderer.render(context);
         } catch (Exception var13) {
         }

         try {
            hitEspRenderer.render(context);
         } catch (Exception var12) {
         }

         try {
            ragdollRenderer.render(context);
         } catch (Exception var11) {
         }

         try {
            particleRenderer.render(context);
         } catch (Exception var10) {
         }

         try {
            Module cubeParticles = moduleManager != null ? moduleManager.getModuleByName("CubeParticles") : null;
            if (cubeParticles != null && cubeParticles.isEnabled()) {
               ((CubeParticlesModule)cubeParticles).onRender3D(context);
            }
         } catch (Exception var9) {
         }

         try {
            ghostEspRenderer.render(context);
         } catch (Exception var8) {
         }

         try {
            killEffectRenderer.render(context);
         } catch (Exception var7) {
         }

         try {
            circleEspRenderer.render(context);
         } catch (Exception var6) {
         }

         try {
            diamondEspRenderer.render(context);
         } catch (Exception var5) {
         }

         try {
            trailsRenderer.render(context);
         } catch (Exception var4) {
         }

         try {
            trajectoriesRenderer.render(context);
         } catch (Exception var3) {
         }

         try {
            Render3DUtil.onRenderWorld(context.matrixStack());
         } catch (Exception var2) {
         }
      });
      HudElementRegistry.addLast(Identifier.fromNamespaceAndPath("astolfoclient", "hud"), (HudElement)(drawContext, tickDelta) -> {
         Minecraft client = Minecraft.getInstance();

         for (Module module : moduleManager.getModules()) {
            if (module.isEnabled()) {
               try {
                  module.onRender();
               } catch (Exception var20) {
               }
            }
         }

         if (client.player != null && client.gui.screen() == null) {
            if (isModuleEnabled("TargetHUD")) {
               try {
                  targetHudManager.render(drawContext, tickDelta.getGameTimeDeltaPartialTick(true));
               } catch (Exception var19) {
               }
            }

            if (isModuleEnabled("EffectHud")) {
               try {
                  effectHudManager.render(drawContext);
               } catch (Exception var18) {
               }
            }

            if (isModuleEnabled("Crosshair")) {
               try {
                  crosshairManager.render(drawContext);
               } catch (Exception var17) {
               }
            }

            if (isModuleEnabled("ActiveBinds")) {
               try {
                  activeBindsManager.render(drawContext, tickDelta.getGameTimeDeltaPartialTick(true));
               } catch (Exception var16) {
               }
            }

            if (isModuleEnabled("Test")) {
               try {
                  testHudManager.render(drawContext, tickDelta.getGameTimeDeltaPartialTick(true));
               } catch (Exception var15) {
               }
            }

            if (isModuleEnabled("InfoHud")) {
               try {
                  infoHudManager.render(drawContext, tickDelta.getGameTimeDeltaPartialTick(true));
               } catch (Exception var14) {
               }
            }

            if (isModuleEnabled("ArrayList")) {
               try {
                  arrayListManager.render(drawContext);
               } catch (Exception var13) {
               }
            }

            if (isModuleEnabled("InventoryHUD")) {
               try {
                  inventoryHudManager.render(drawContext);
               } catch (Exception var12) {
               }
            }

            if (isModuleEnabled("ArmorHUD")) {
               try {
                  armorHudManager.render(drawContext);
               } catch (Exception var11) {
               }
            }

            if (isModuleEnabled("CustomHotbar")) {
               try {
                  customHotbarManager.render(drawContext, tickDelta.getGameTimeDeltaPartialTick(true));
               } catch (Exception var10) {
               }
            }

            if (isModuleEnabled("Scoreboard")) {
               try {
                  scoreboardManager.render(drawContext);
               } catch (Exception var9) {
               }
            }

            if (isModuleEnabled("MusicHUD")) {
               try {
                  musicHudManager.render(drawContext, tickDelta.getGameTimeDeltaPartialTick(true));
               } catch (Exception var8) {
               }
            }

            try {
               hudRenderer.render(drawContext, tickDelta.getGameTimeDeltaPartialTick(true));
            } catch (Exception var7) {
            }

            try {
               trajectoriesRenderer.renderHUD(drawContext);
            } catch (Exception var6) {
            }
         }
      });
   }

   public static boolean isModuleEnabled(String name) {
      if (moduleManager == null) {
         return false;
      }

      if (name.equalsIgnoreCase("TargetHUD")
         || name.equalsIgnoreCase("EffectHud")
         || name.equalsIgnoreCase("InventoryHUD")
         || name.equalsIgnoreCase("ArrayList")
         || name.equalsIgnoreCase("ArmorHUD")
         || name.equalsIgnoreCase("MusicHUD")
         || name.equalsIgnoreCase("InfoHud")
         || name.equalsIgnoreCase("WaterMark")
         || name.equalsIgnoreCase("ActiveBinds")) {
         InterfaceModule interfaceMod = (InterfaceModule)moduleManager.getModuleByName("Interface");
         if (interfaceMod == null || !interfaceMod.isEnabled()) {
            return false;
         }

         switch (name.toLowerCase()) {
            case "targethud":
               return interfaceMod.targetHud.get();
            case "effecthud":
               return interfaceMod.effectHud.get();
            case "inventoryhud":
               return interfaceMod.inventoryHud.get();
            case "arraylist":
               return interfaceMod.arrayList.get();
            case "armorhud":
               return interfaceMod.armorHud.get();
            case "musichud":
               return interfaceMod.musicHud.get();
            case "infohud":
               return interfaceMod.infoHud.get();
            case "watermark":
               return interfaceMod.logo.get();
            case "activebinds":
               return interfaceMod.activeBinds.get();
         }
      }

      Module m = moduleManager.getModuleByName(name);
      return m != null && m.isEnabled();
   }

   public String getBio() {
      return "https://fakecrime.bio/SRS";
   }

   public DiscordRpcManager getDiscordRpcManager() {
      return this.discordRpcManager;
   }
}
