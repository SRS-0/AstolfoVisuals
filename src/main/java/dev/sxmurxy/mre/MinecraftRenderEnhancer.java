package dev.sxmurxy.mre;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Environment(value=EnvType.CLIENT)
public final class MinecraftRenderEnhancer
implements ModInitializer {
    public static final String MOD_ID = "mre";
    public static final Logger LOGGER = LoggerFactory.getLogger((String)"mre");

    public void onInitialize() {
    }
}

