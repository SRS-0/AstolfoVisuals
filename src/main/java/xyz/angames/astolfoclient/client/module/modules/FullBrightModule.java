package xyz.angames.astolfoclient.client.module.modules;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LightBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import xyz.angames.astolfoclient.client.module.Module;
import xyz.angames.astolfoclient.client.module.setting.ModeSetting;

@Environment(value=EnvType.CLIENT)
public class FullBrightModule
extends Module {
    public final ModeSetting mode = new ModeSetting("Mode", "Potion", "Potion", "Light");
    private final Minecraft client = Minecraft.getInstance();
    private final Map<BlockPos, BlockState> activeLights = new HashMap<BlockPos, BlockState>();
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
        BlockPos playerPos;
        if (this.client.player == null || this.client.level == null) {
            return;
        }
        BlockPos centerPos = playerPos = this.client.player.blockPosition();
        BlockState feetState = this.client.level.getBlockState(playerPos);
        if (!feetState.isAir() && !feetState.is(Blocks.LIGHT)) {
            centerPos = playerPos.above();
        }
        HashMap<BlockPos, Integer> targetLights = new HashMap<BlockPos, Integer>();
        int radius = 4;
        for (int dx = -radius; dx <= radius; ++dx) {
            for (int dy = -radius; dy <= radius; ++dy) {
                for (int dz = -radius; dz <= radius; ++dz) {
                    int level;
                    double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
                    if (!(dist <= (double)radius) || (level = 15 - (int)Math.round(dist * 3.2)) < 1) continue;
                    BlockPos pos = centerPos.offset(dx, dy, dz);
                    targetLights.put(pos, level);
                }
            }
        }
        Iterator<Map.Entry<BlockPos, BlockState>> iterator = this.activeLights.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<BlockPos, BlockState> entry = iterator.next();
            BlockPos pos = entry.getKey();
            if (targetLights.containsKey(pos)) continue;
            BlockState current = this.client.level.getBlockState(pos);
            if (current.is(Blocks.LIGHT)) {
                this.client.level.setBlock(pos, entry.getValue(), 2);
            }
            iterator.remove();
        }
        for (Map.Entry entry : targetLights.entrySet()) {
            BlockPos pos = (BlockPos)entry.getKey();
            int level = (Integer)entry.getValue();
            BlockState current = this.client.level.getBlockState(pos);
            if (this.activeLights.containsKey(pos)) {
                if (current.is(Blocks.LIGHT)) {
                    int currentLevel = (Integer)current.getValue((Property)LightBlock.LEVEL);
                    if (currentLevel == level) continue;
                    this.client.level.setBlock(pos, (BlockState)current.setValue((Property)LightBlock.LEVEL, (Comparable)Integer.valueOf(level)), 2);
                    continue;
                }
                this.activeLights.remove(pos);
                continue;
            }
            if (!current.isAir()) continue;
            this.activeLights.put(pos.immutable(), current);
            this.client.level.setBlock(pos, (BlockState)Blocks.LIGHT.defaultBlockState().setValue((Property)LightBlock.LEVEL, (Comparable)Integer.valueOf(level)), 2);
        }
    }

    private void clearLight() {
        for (Map.Entry<BlockPos, BlockState> entry : this.activeLights.entrySet()) {
            BlockPos pos = entry.getKey();
            BlockState current = this.client.level.getBlockState(pos);
            if (!current.is(Blocks.LIGHT)) continue;
            this.client.level.setBlock(pos, entry.getValue(), 2);
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

