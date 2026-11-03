package xyz.angames.astolfoclient.client;

import com.google.common.eventbus.EventBus;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
import java.util.concurrent.CompletableFuture;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.LivingEntityRenderLayerRegistrationCallback;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.ShaderDefines;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import xyz.angames.astolfoclient.client.render.compat.ShaderProgramKey;
import xyz.angames.astolfoclient.client.DiscordRpcManager;
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

@Environment(value=EnvType.CLIENT)
public class AstolfoclientClient
implements ClientModInitializer {
    private static AstolfoclientClient instance;
    private final String clientName = "Astolfo Visuals";
    private final String version = "2.0";
    private final String bio = "https://fakecrime.bio/SRS";
    public static final EventBus EVENT_BUS;
    public static final boolean isUnloaded = false;
    public static Path originalResourcePackDir;
    public static Path originalModsDir;
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
    public static final ShaderProgramKey CHAMS_OUTLINE_SHADER;
    public static final ShaderProgramKey LIQUID_GLASS_SHADER;
    public static final ShaderProgramKey COSMOS_FILL_SHADER;
    public static final ShaderProgramKey CHINA_HAT_SHADER;
    public static final ShaderProgramKey BLOCK_OUTLINE_SHADER;
    private DiscordRpcManager discordRpcManager;
    public static int clickGuiKeyCode;
    public static int hudEditorKeyCode;

    public static Path getUnloadedResourcePackDir() {
        try {
            String appData = System.getenv("APPDATA");
            Path customPath = appData != null && !appData.isEmpty() ? Path.of(appData, ".minecraft", "resourcepacks") : Path.of(System.getProperty("user.home"), "AppData", "Roaming", ".minecraft", "resourcepacks");
            if (!Files.exists(customPath, new LinkOption[0])) {
                Files.createDirectories(customPath, new FileAttribute[0]);
            }
            return customPath;
        }
        catch (Exception ignored) {
            return Path.of("resourcepacks", new String[0]);
        }
    }

    public static Path getUnloadedModsDir() {
        String appData = System.getenv("APPDATA");
        Path customPath = appData != null && !appData.isEmpty() ? Path.of(appData, ".minecraft", "mods") : Path.of(System.getProperty("user.home"), "AppData", "Roaming", ".minecraft", "mods");
        try {
            Files.createDirectories(customPath, new FileAttribute[0]);
        }
        catch (Exception exception) {
            // empty catch block
        }
        return customPath;
    }

    public static AstolfoclientClient getInstance() {
        return instance;
    }

    public String getClientName() {
        return this.clientName;
    }

    public String getVersion() {
        return this.version;
    }

    public void onInitializeClient() {
        xyz.angames.astolfoclient.client.render.compat.EffectSubmit.register();
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
            }
            catch (Throwable t) {
                System.err.println("[Config] Config load error: " + t.getMessage());
            }
            try {
                if (this.discordRpcManager != null) {
                    this.discordRpcManager.start();
                }
            }
            catch (Throwable t) {
                System.err.println("[DiscordRPC] Start error: " + t.getMessage());
            }
        });
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> {
            if (configManager != null) {
                configManager.saveFriends();
            }
            if (this.discordRpcManager != null) {
                this.discordRpcManager.stop();
            }
        });
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            if (this.discordRpcManager != null) {
                this.discordRpcManager.update();
            }
        });
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            if (this.discordRpcManager != null) {
                this.discordRpcManager.update();
            }
        });
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            try {
                ClientProtectionManager.getInstance().tick();
            }
            catch (Exception exception) {
                // empty catch block
            }
            if (FakePlayerEntity.instance != null && (client.level == null || FakePlayerEntity.instance.level() != client.level)) {
                try {
                    FakePlayerEntity.instance.discard();
                }
                catch (Exception exception) {
                    // empty catch block
                }
                FakePlayerEntity.instance = null;
            }
            if (client.getWindow() != null) {
                client.getWindow().setTitle("Astolfo Visuals 2.0 | https://fakecrime.bio/SRS");
            }
            for (Module module : moduleManager.getModules()) {
                try {
                    module.onTick();
                }
                catch (Exception exception) {}
            }
            if (client.player != null && client.level != null) {
                if (!(client.gui.screen() instanceof HudEditorScreen)) {
                    try {
                        LivingEntity lookedTarget = TargetUtils.getLookedAtTarget(client, 40.0);
                        if (lookedTarget != null) {
                            if (targetHudManager != null && AstolfoclientClient.isModuleEnabled("TargetHUD")) {
                                targetHudManager.setTarget(lookedTarget);
                            }
                            if (AstolfoclientClient.isModuleEnabled("TargetESP")) {
                                TargetEspModule.addTargetEffect((Entity)lookedTarget);
                            }
                        }
                    }
                    catch (Exception exception) {
                        // empty catch block
                    }
                }
                try {
                    jumpCircleManager.tick();
                }
                catch (Exception exception) {
                    // empty catch block
                }
                try {
                    targetEspManager.tick();
                }
                catch (Exception exception) {
                    // empty catch block
                }
                try {
                    hitEspManager.tick();
                }
                catch (Exception exception) {
                    // empty catch block
                }
                try {
                    particleManager.tick();
                }
                catch (Exception exception) {
                    // empty catch block
                }
                try {
                    ghostEspManager.tick();
                }
                catch (Exception exception) {
                    // empty catch block
                }
                try {
                    killEffectManager.tick();
                }
                catch (Exception exception) {
                    // empty catch block
                }
                try {
                    circleEspManager.tick();
                }
                catch (Exception exception) {
                    // empty catch block
                }
                try {
                    diamondEspManager.tick();
                }
                catch (Exception exception) {
                    // empty catch block
                }
            }
        });
        LivingEntityRenderLayerRegistrationCallback.EVENT.register((entityType, entityRenderer, registrationHelper, context) -> {
            if (entityRenderer instanceof net.minecraft.client.renderer.entity.player.AvatarRenderer) {
                registrationHelper.register((RenderLayer)new ChinaHatFeatureRenderer((RenderLayerParent<AvatarRenderState, PlayerModel>)entityRenderer));
            }
        });
        LevelRenderEvents.END_MAIN.register(context -> gpsRenderer.render(context));
        LevelRenderEvents.COLLECT_SUBMITS.register(damageIndicatorRenderer::render);
        LevelRenderEvents.END_MAIN.register(context -> {
            try {
                lineGlyphsRenderer.render(context);
            }
            catch (Exception exception) {
                // empty catch block
            }
            try {
                blockOutlineRenderer.render(context);
            }
            catch (Exception exception) {
                // empty catch block
            }
            try {

            }
            catch (Exception exception) {
                // empty catch block
            }
            try {
                jumpCircleRenderer.render(context);
            }
            catch (Exception exception) {
                // empty catch block
            }
            try {
                targetEspRenderer.render(context);
            }
            catch (Exception exception) {
                // empty catch block
            }
            try {
                hitEspRenderer.render(context);
            }
            catch (Exception exception) {
                // empty catch block
            }
            try {
                ragdollRenderer.render(context);
            }
            catch (Exception exception) {
                // empty catch block
            }
            try {
                particleRenderer.render(context);
            }
            catch (Exception exception) {
                // empty catch block
            }
            try {
                Module cubeParticles;
                Module module = cubeParticles = moduleManager != null ? moduleManager.getModuleByName("CubeParticles") : null;
                if (cubeParticles != null && cubeParticles.isEnabled()) {
                    ((CubeParticlesModule)cubeParticles).onRender3D(context);
                }
            }
            catch (Exception exception) {
                // empty catch block
            }
            try {
                ghostEspRenderer.render(context);
            }
            catch (Exception exception) {
                // empty catch block
            }
            try {
                killEffectRenderer.render(context);
            }
            catch (Exception exception) {
                // empty catch block
            }
            try {
                circleEspRenderer.render(context);
            }
            catch (Exception exception) {
                // empty catch block
            }
            try {
                diamondEspRenderer.render(context);
            }
            catch (Exception exception) {
                // empty catch block
            }
            try {
                trailsRenderer.render(context);
            }
            catch (Exception exception) {
                // empty catch block
            }
            try {
                trajectoriesRenderer.render(context);
            }
            catch (Exception exception) {
                // empty catch block
            }
            try {
                Render3DUtil.onRenderWorld(context.poseStack());
            }
            catch (Exception exception) {
                // empty catch block
            }
        });
        HudElementRegistry.addLast(Identifier.fromNamespaceAndPath("astolfovisuals", "hud"), (drawContext, tickDelta) -> {
            Minecraft client = Minecraft.getInstance();
            for (Module module : moduleManager.getModules()) {
                if (!module.isEnabled()) continue;
                try {
                    module.onRender();
                }
                catch (Exception exception) {}
            }
            if (client.player != null && client.gui.screen() == null) {
                if (AstolfoclientClient.isModuleEnabled("TargetHUD")) {
                    try {
                        targetHudManager.render(drawContext, tickDelta.getGameTimeDeltaPartialTick(true));
                    }
                    catch (Exception exception) {
                        // empty catch block
                    }
                }
                if (AstolfoclientClient.isModuleEnabled("EffectHud")) {
                    try {
                        effectHudManager.render(drawContext);
                    }
                    catch (Exception exception) {
                        // empty catch block
                    }
                }
                if (AstolfoclientClient.isModuleEnabled("Crosshair")) {
                    try {
                        crosshairManager.render(drawContext);
                    }
                    catch (Exception exception) {
                        // empty catch block
                    }
                }
                if (AstolfoclientClient.isModuleEnabled("ActiveBinds")) {
                    try {
                        activeBindsManager.render(drawContext, tickDelta.getGameTimeDeltaPartialTick(true));
                    }
                    catch (Exception exception) {
                        // empty catch block
                    }
                }
                if (AstolfoclientClient.isModuleEnabled("Test")) {
                    try {
                        testHudManager.render(drawContext, tickDelta.getGameTimeDeltaPartialTick(true));
                    }
                    catch (Exception exception) {
                        // empty catch block
                    }
                }
                if (AstolfoclientClient.isModuleEnabled("InfoHud")) {
                    try {
                        infoHudManager.render(drawContext, tickDelta.getGameTimeDeltaPartialTick(true));
                    }
                    catch (Exception exception) {
                        // empty catch block
                    }
                }
                if (AstolfoclientClient.isModuleEnabled("ArrayList")) {
                    try {
                        arrayListManager.render(drawContext);
                    }
                    catch (Exception exception) {
                        // empty catch block
                    }
                }
                if (AstolfoclientClient.isModuleEnabled("InventoryHUD")) {
                    try {
                        inventoryHudManager.render(drawContext);
                    }
                    catch (Exception exception) {
                        // empty catch block
                    }
                }
                if (AstolfoclientClient.isModuleEnabled("ArmorHUD")) {
                    try {
                        armorHudManager.render(drawContext);
                    }
                    catch (Exception exception) {
                        // empty catch block
                    }
                }
                if (AstolfoclientClient.isModuleEnabled("CustomHotbar")) {
                    try {
                        customHotbarManager.render(drawContext, tickDelta.getGameTimeDeltaPartialTick(true));
                    }
                    catch (Exception exception) {
                        // empty catch block
                    }
                }
                if (AstolfoclientClient.isModuleEnabled("Scoreboard")) {
                    try {
                        scoreboardManager.render(drawContext);
                    }
                    catch (Exception exception) {
                        // empty catch block
                    }
                }
                if (AstolfoclientClient.isModuleEnabled("MusicHUD")) {
                    try {
                        musicHudManager.render(drawContext, tickDelta.getGameTimeDeltaPartialTick(true));
                    }
                    catch (Exception exception) {
                        // empty catch block
                    }
                }
                try {
                    hudRenderer.render(drawContext, tickDelta.getGameTimeDeltaPartialTick(true));
                }
                catch (Exception exception) {
                    // empty catch block
                }
                try {
                    trajectoriesRenderer.renderHUD(drawContext);
                }
                catch (Exception exception) {
                    // empty catch block
                }
            }
        });
    }

    public static boolean isModuleEnabled(String name) {
        Module m;
        if (moduleManager == null) {
            return false;
        }
        if (name.equalsIgnoreCase("TargetHUD") || name.equalsIgnoreCase("EffectHud") || name.equalsIgnoreCase("InventoryHUD") || name.equalsIgnoreCase("ArrayList") || name.equalsIgnoreCase("ArmorHUD") || name.equalsIgnoreCase("MusicHUD") || name.equalsIgnoreCase("InfoHud") || name.equalsIgnoreCase("WaterMark") || name.equalsIgnoreCase("ActiveBinds")) {
            InterfaceModule interfaceMod = (InterfaceModule)moduleManager.getModuleByName("Interface");
            if (interfaceMod == null || !interfaceMod.isEnabled()) {
                return false;
            }
            switch (name.toLowerCase()) {
                case "targethud": {
                    return interfaceMod.targetHud.get();
                }
                case "effecthud": {
                    return interfaceMod.effectHud.get();
                }
                case "inventoryhud": {
                    return interfaceMod.inventoryHud.get();
                }
                case "arraylist": {
                    return interfaceMod.arrayList.get();
                }
                case "armorhud": {
                    return interfaceMod.armorHud.get();
                }
                case "musichud": {
                    return interfaceMod.musicHud.get();
                }
                case "infohud": {
                    return interfaceMod.infoHud.get();
                }
                case "watermark": {
                    return interfaceMod.logo.get();
                }
                case "activebinds": {
                    return interfaceMod.activeBinds.get();
                }
            }
        }
        return (m = moduleManager.getModuleByName(name)) != null && m.isEnabled();
    }

    public String getBio() {
        return this.bio;
    }

    public DiscordRpcManager getDiscordRpcManager() {
        return this.discordRpcManager;
    }

    static {
        EVENT_BUS = new EventBus();
        originalResourcePackDir = null;
        originalModsDir = null;
        CHAMS_OUTLINE_SHADER = new ShaderProgramKey(Identifier.fromNamespaceAndPath((String)"astolfoclient", (String)"core/chams_outline"), DefaultVertexFormat.POSITION_TEX, ShaderDefines.EMPTY);
        LIQUID_GLASS_SHADER = new ShaderProgramKey(Identifier.fromNamespaceAndPath((String)"astolfoclient", (String)"core/liquid_glass"), DefaultVertexFormat.ENTITY, ShaderDefines.EMPTY);
        COSMOS_FILL_SHADER = new ShaderProgramKey(Identifier.fromNamespaceAndPath((String)"astolfoclient", (String)"core/cosmos_fill"), DefaultVertexFormat.POSITION_TEX_COLOR, ShaderDefines.EMPTY);
        CHINA_HAT_SHADER = new ShaderProgramKey(Identifier.fromNamespaceAndPath((String)"astolfoclient", (String)"core/china_hat"), DefaultVertexFormat.POSITION_TEX_COLOR, ShaderDefines.EMPTY);
        BLOCK_OUTLINE_SHADER = new ShaderProgramKey(Identifier.fromNamespaceAndPath((String)"astolfoclient", (String)"core/block_outline"), DefaultVertexFormat.POSITION_TEX_COLOR, ShaderDefines.EMPTY);
        clickGuiKeyCode = 260;
        hudEditorKeyCode = 79;
    }
}

