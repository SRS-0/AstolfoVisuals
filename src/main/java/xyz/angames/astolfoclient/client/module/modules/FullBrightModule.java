package xyz.angames.astolfoclient.client.module.modules;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.world.level.block.LightBlock;
import xyz.angames.astolfoclient.client.module.Module;
import xyz.angames.astolfoclient.client.module.setting.ModeSetting;

@Environment(EnvType.CLIENT)
public class FullBrightModule extends Module {
   public final ModeSetting mode = new ModeSetting("Mode", "Potion", "Potion", "Light");
   private final Minecraft client = Minecraft.getInstance();
   private final Map<BlockPos, BlockState> activeLights = new HashMap<>();
   private String lastMode = "";

   public FullBrightModule() {
      super("FullBright", Module.Category.RENDER);
      this.addSetting(this.mode);
   }

   @Override
   public void onEnable() {
      this.lastMode = this.mode.get();
      if (this.client.player != null && this.client.level != null && this.lastMode.equalsIgnoreCase("Potion")) {
         this.applyEffect();
      }
   }

   @Override
   public void onDisable() {
      this.removePotionEffect();
      this.clearLight();
      this.lastMode = "";
   }

   @Override
   public void onTick() {
      if (this.isEnabled() && this.client.player != null && this.client.level != null) {
         String currentMode = this.mode.get();
         if (!currentMode.equalsIgnoreCase(this.lastMode)) {
            if (this.lastMode.equalsIgnoreCase("Potion")) {
               this.removePotionEffect();
            } else if (this.lastMode.equalsIgnoreCase("Light")) {
               this.clearLight();
            }

            this.lastMode = currentMode;
         }

         if (currentMode.equalsIgnoreCase("Potion")) {
            if (!this.client.player.hasEffect(MobEffects.NIGHT_VISION)) {
               this.applyEffect();
            }
         } else if (currentMode.equalsIgnoreCase("Light")) {
            this.removePotionEffect();
            this.updateDynamicLight();
         }
      }
   }

   private void updateDynamicLight() {
      if (this.client.player != null && this.client.level != null) {
         BlockPos playerPos = this.client.player.blockPosition();
         BlockPos centerPos = playerPos;
         BlockState feetState = this.client.level.getBlockState(playerPos);
         if (!feetState.isAir() && !feetState.is(Blocks.LIGHT)) {
            centerPos = playerPos.above();
         }

         Map<BlockPos, Integer> targetLights = new HashMap<>();
         int radius = 4;

         for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -radius; dy <= radius; dy++) {
               for (int dz = -radius; dz <= radius; dz++) {
                  double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
                  if (dist <= radius) {
                     int level = 15 - (int)Math.round(dist * 3.2);
                     if (level >= 1) {
                        BlockPos pos = centerPos.offset(dx, dy, dz);
                        targetLights.put(pos, level);
                     }
                  }
               }
            }
         }

         Iterator<Entry<BlockPos, BlockState>> iterator = this.activeLights.entrySet().iterator();

         while (iterator.hasNext()) {
            Entry<BlockPos, BlockState> entry = iterator.next();
            BlockPos pos = entry.getKey();
            if (!targetLights.containsKey(pos)) {
               BlockState current = this.client.level.getBlockState(pos);
               if (current.is(Blocks.LIGHT)) {
                  this.client.level.setBlock(pos, entry.getValue(), 2);
               }

               iterator.remove();
            }
         }

         for (Entry<BlockPos, Integer> entry : targetLights.entrySet()) {
            BlockPos pos = entry.getKey();
            int level = entry.getValue();
            BlockState current = this.client.level.getBlockState(pos);
            if (this.activeLights.containsKey(pos)) {
               if (current.is(Blocks.LIGHT)) {
                  int currentLevel = (Integer)current.getValue(LightBlock.LEVEL);
                  if (currentLevel != level) {
                     this.client.level.setBlock(pos, (BlockState)current.setValue(LightBlock.LEVEL, level), 2);
                  }
               } else {
                  this.activeLights.remove(pos);
               }
            } else if (current.isAir()) {
               this.activeLights.put(pos.immutable(), current);
               this.client.level.setBlock(pos, (BlockState)Blocks.LIGHT.defaultBlockState().setValue(LightBlock.LEVEL, level), 2);
            }
         }
      }
   }

   private void clearLight() {
      for (Entry<BlockPos, BlockState> entry : this.activeLights.entrySet()) {
         BlockPos pos = entry.getKey();
         BlockState current = this.client.level.getBlockState(pos);
         if (current.is(Blocks.LIGHT)) {
            this.client.level.setBlock(pos, entry.getValue(), 2);
         }
      }

      this.activeLights.clear();
   }

   private void applyEffect() {
      int longDuration = -1;
      MobEffectInstance nightVisionEffect = new MobEffectInstance(MobEffects.NIGHT_VISION, longDuration, 0, false, false, true);
      this.client.player.addEffect(nightVisionEffect);
   }

   private void removePotionEffect() {
      if (this.client.player != null && this.client.player.hasEffect(MobEffects.NIGHT_VISION)) {
         this.client.player.removeEffect(MobEffects.NIGHT_VISION);
      }
   }
}
