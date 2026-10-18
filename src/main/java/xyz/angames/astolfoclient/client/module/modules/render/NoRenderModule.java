package xyz.angames.astolfoclient.client.module.modules.render;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.client.Minecraft;
import xyz.angames.astolfoclient.client.module.Module;
import xyz.angames.astolfoclient.client.module.setting.BooleanSetting;

@Environment(EnvType.CLIENT)
public class NoRenderModule extends Module {
   private static NoRenderModule instance;
   public final BooleanSetting fire = new BooleanSetting("Fire Overlay", true);
   public final BooleanSetting totem = new BooleanSetting("Totem Pop", true);
   public final BooleanSetting scoreboard = new BooleanSetting("Scoreboard", false);
   public final BooleanSetting bossbar = new BooleanSetting("Bossbar", false);
   public final BooleanSetting hurtCam = new BooleanSetting("Hurt Camera", true);
   public final BooleanSetting pumpkin = new BooleanSetting("Pumpkin Blur", true);
   public final BooleanSetting portal = new BooleanSetting("Portal Nausea", true);
   public final BooleanSetting blindness = new BooleanSetting("Blindness", true);
   public final BooleanSetting blockOverlay = new BooleanSetting("Block Overlay", true);
   public final BooleanSetting grass = new BooleanSetting("Grass", false);
   private boolean lastGrassState = false;

   public NoRenderModule() {
      super("NoRender", Module.Category.RENDER);
      this.addSetting(this.fire);
      this.addSetting(this.totem);
      this.addSetting(this.scoreboard);
      this.addSetting(this.bossbar);
      this.addSetting(this.hurtCam);
      this.addSetting(this.pumpkin);
      this.addSetting(this.portal);
      this.addSetting(this.blindness);
      this.addSetting(this.blockOverlay);
      this.addSetting(this.grass);
      instance = this;
   }

   public static NoRenderModule getInstance() {
      return instance;
   }

   @Override
   public void onEnable() {
      if (this.grass.get()) {
         this.reloadWorldRenderer();
      }

      this.lastGrassState = this.grass.get();
   }

   @Override
   public void onDisable() {
      if (this.lastGrassState) {
         this.reloadWorldRenderer();
      }

      this.lastGrassState = false;
   }

   @Override
   public void onTick() {
      if (this.grass.get() != this.lastGrassState) {
         this.lastGrassState = this.grass.get();
         this.reloadWorldRenderer();
      }
   }

   private void reloadWorldRenderer() {
      Minecraft mc = Minecraft.getInstance();
      if (mc.levelRenderer != null) {
         mc.levelRenderer.invalidateCompiledGeometry(mc.level, mc.options, mc.gameRenderer.mainCamera(), mc.getBlockColors());
      }
   }

   public static boolean isGrass(BlockState state) {
      if (state == null) {
         return false;
      }

      Block block = state.getBlock();
      return block == Blocks.SHORT_GRASS
         || block == Blocks.TALL_GRASS
         || block == Blocks.FERN
         || block == Blocks.LARGE_FERN
         || block == Blocks.SEAGRASS
         || block == Blocks.TALL_SEAGRASS
         || block == Blocks.DEAD_BUSH
         || block == Blocks.HANGING_ROOTS
         || block == Blocks.NETHER_SPROUTS
         || block == Blocks.CRIMSON_ROOTS
         || block == Blocks.WARPED_ROOTS;
   }
}
