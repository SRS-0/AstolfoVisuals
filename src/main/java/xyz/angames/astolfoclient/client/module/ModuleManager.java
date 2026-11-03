package xyz.angames.astolfoclient.client.module;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import xyz.angames.astolfoclient.client.gui.ClickGuiScreen;
import xyz.angames.astolfoclient.client.module.Module;
import xyz.angames.astolfoclient.client.module.modules.BoyKisserModule;
import xyz.angames.astolfoclient.client.module.modules.FullBrightModule;
import xyz.angames.astolfoclient.client.module.modules.HitEspModule;
import xyz.angames.astolfoclient.client.module.modules.JumpCircleModule;
import xyz.angames.astolfoclient.client.module.modules.TargetEspModule;
import xyz.angames.astolfoclient.client.module.modules.misc.AutoRespawnModule;
import xyz.angames.astolfoclient.client.module.modules.misc.CoordsHiderModule;
import xyz.angames.astolfoclient.client.module.modules.misc.NameProtectModule;
import xyz.angames.astolfoclient.client.module.modules.misc.PasswordHiderModule;
import xyz.angames.astolfoclient.client.module.modules.render.AmbientsModule;
import xyz.angames.astolfoclient.client.module.modules.render.AspectRatioModule;
import xyz.angames.astolfoclient.client.module.modules.render.BabyPlayerModule;
import xyz.angames.astolfoclient.client.module.modules.render.BlockOutlineModule;
import xyz.angames.astolfoclient.client.module.modules.render.CameraUtilsModule;
import xyz.angames.astolfoclient.client.module.modules.render.ChinaHatModule;
import xyz.angames.astolfoclient.client.module.modules.render.CrosshairModule;
import xyz.angames.astolfoclient.client.module.modules.render.CubeParticlesModule;
import xyz.angames.astolfoclient.client.module.modules.render.DamageIndicatorModule;
import xyz.angames.astolfoclient.client.module.modules.render.DashTrailModule;
import xyz.angames.astolfoclient.client.module.modules.render.FireFliesModule;
import xyz.angames.astolfoclient.client.module.modules.render.HandPositionModule;
import xyz.angames.astolfoclient.client.module.modules.render.HitGlowModule;
import xyz.angames.astolfoclient.client.module.modules.render.InterfaceModule;
import xyz.angames.astolfoclient.client.module.modules.render.ItemChamsModule;
import xyz.angames.astolfoclient.client.module.modules.render.ItemPhysicsModule;
import xyz.angames.astolfoclient.client.module.modules.render.KillEffectModule;
import xyz.angames.astolfoclient.client.module.modules.render.LineGlyphsModule;
import xyz.angames.astolfoclient.client.module.modules.render.ModelsModule;
import xyz.angames.astolfoclient.client.module.modules.render.NoRenderModule;
import xyz.angames.astolfoclient.client.module.modules.render.ParticlesModule;
import xyz.angames.astolfoclient.client.module.modules.render.RagdollModule;
import xyz.angames.astolfoclient.client.module.modules.render.RainModule;
import xyz.angames.astolfoclient.client.module.modules.render.ShaderHand;
import xyz.angames.astolfoclient.client.module.modules.render.SwingAnimationModule;
import xyz.angames.astolfoclient.client.module.modules.render.TrailsModule;
import xyz.angames.astolfoclient.client.module.modules.render.TrajectoriesModule;
import xyz.angames.astolfoclient.client.module.modules.render.WingsModule;

@Environment(value=EnvType.CLIENT)
public class ModuleManager {
    public static ModuleManager INSTANCE;
    public static List<Module> modules;
    public static final List<Class<? extends Module>> betaModules;

    public static boolean isBeta(Module module) {
        if (module == null) {
            return false;
        }
        return betaModules.contains(module.getClass());
    }

    public ModuleManager() {
        INSTANCE = this;
        this.addModule(new AmbientsModule());
        this.addModule(new AspectRatioModule());
        this.addModule(new BabyPlayerModule());
        this.addModule(new BlockOutlineModule());
        this.addModule(new CameraUtilsModule());
        this.addModule(new ChinaHatModule());
        this.addModule(new CrosshairModule());
        this.addModule(new CubeParticlesModule());
        this.addModule(new DamageIndicatorModule());
        this.addModule(new DashTrailModule());
        this.addModule(new FireFliesModule());
        this.addModule(new HandPositionModule());
        this.addModule(new HitGlowModule());
        this.addModule(new InterfaceModule());
        this.addModule(new ItemChamsModule());
        this.addModule(new ItemPhysicsModule());
        this.addModule(new KillEffectModule());
        this.addModule(new LineGlyphsModule());
        this.addModule(new ModelsModule());
        this.addModule(new NoRenderModule());
        this.addModule(new ParticlesModule());
        this.addModule(new RagdollModule());
        this.addModule(new RainModule());
        this.addModule(new ShaderHand());
        this.addModule(new SwingAnimationModule());
        this.addModule(new TrailsModule());
        this.addModule(new TrajectoriesModule());
        this.addModule(new WingsModule());
        this.addModule(new CoordsHiderModule());
        this.addModule(new NameProtectModule());
        this.addModule(new PasswordHiderModule());
        this.addModule(new AutoRespawnModule());
        this.addModule(new BoyKisserModule());
        this.addModule(new FullBrightModule());
        this.addModule(new HitEspModule());
        this.addModule(new JumpCircleModule());
        this.addModule(new TargetEspModule());
    }

    private void addModule(Module module) {
        modules.add(module);
    }

    public synchronized void registerDynamicModule(Module module) {
        Screen screen;
        if (module == null) {
            return;
        }
        modules.removeIf(m -> m.getName().equalsIgnoreCase(module.getName()));
        modules.add(module);
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc != null && (screen = mc.currentScreen) instanceof ClickGuiScreen) {
            ClickGuiScreen clickGui = (ClickGuiScreen)screen;
            clickGui.refreshModuleButtons();
        }
    }

    public synchronized void unregisterDynamicModule(Module module) {
        Screen screen;
        if (module == null) {
            return;
        }
        module.setEnabled(false);
        modules.remove(module);
        modules.removeIf(m -> m.getName().equalsIgnoreCase(module.getName()));
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc != null && (screen = mc.currentScreen) instanceof ClickGuiScreen) {
            ClickGuiScreen clickGui = (ClickGuiScreen)screen;
            clickGui.refreshModuleButtons();
        }
    }

    public List<Module> getModules() {
        return modules != null ? modules : Collections.emptyList();
    }

    public Module getModuleByName(String name) {
        if (modules == null || name == null) {
            return null;
        }
        for (Module module : modules) {
            if (module == null || module.getName() == null || !module.getName().equalsIgnoreCase(name)) continue;
            return module;
        }
        return null;
    }

    public static Module getModule(Class<? extends Module> clazz) {
        if (modules == null || clazz == null) {
            return null;
        }
        for (Module module : modules) {
            if (module == null || module.getClass() != clazz) continue;
            return module;
        }
        return null;
    }

    static {
        modules = new ArrayList<Module>();
        betaModules = new ArrayList<Class<? extends Module>>();
    }
}

