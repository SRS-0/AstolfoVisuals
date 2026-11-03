package xyz.angames.astolfoclient.client.util;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(value=EnvType.CLIENT)
public class TimerManager {
    private static float timerSpeed = 1.0f;

    public static void setTimer(float speed) {
        timerSpeed = speed;
    }

    public static float getTimer() {
        return timerSpeed;
    }

    public static void reset() {
        TimerManager.setTimer(1.0f);
    }

    public boolean hasReached(long l) {
        return false;
    }
}

