package xyz.angames.astolfoclient.client.hud;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.win32.StdCallLibrary;
import dev.redstones.mediaplayerinfo.IMediaSession;
import dev.redstones.mediaplayerinfo.MediaInfo;
import dev.redstones.mediaplayerinfo.MediaPlayerInfo;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;

@Environment(value=EnvType.CLIENT)
public class MusicTracker {
    private static int cycle = 0;
    private static volatile boolean active = false;
    private static volatile String title = "No Music Playing";
    private static volatile String artist = "Unknown Artist";
    private static volatile long position = 0L;
    private static volatile long duration = 100L;
    private static volatile boolean playing = false;
    private static volatile byte[] artworkBytes = null;
    private static volatile String owner = "system";
    private static volatile long lastPollSystemTime = 0L;
    private static volatile long lastPolledPosition = 0L;
    private static Identifier artworkIdentifier = null;
    private static NativeImageBackedTexture artworkTexture = null;
    private static byte[] lastArtworkBytes = null;
    private static User32 user32Instance;

    private static synchronized void pollMediaSession() {
        try {
            List<IMediaSession> sessions = MediaPlayerInfo.Instance.getMediaSessions();
            if (sessions != null && !sessions.isEmpty()) {
                IMediaSession activeSession = null;
                for (IMediaSession s : sessions) {
                    if (s == null || s.getMedia() == null || !s.getMedia().getPlaying()) continue;
                    activeSession = s;
                    break;
                }
                if (activeSession == null) {
                    for (IMediaSession s : sessions) {
                        if (s == null || s.getMedia() == null) continue;
                        activeSession = s;
                        break;
                    }
                }
                if (activeSession != null) {
                    MediaInfo media = activeSession.getMedia();
                    if (media != null) {
                        boolean sessionGained = !active;
                        active = true;
                        String newTitle = media.getTitle() != null ? media.getTitle() : "";
                        String newArtist = media.getArtist() != null ? media.getArtist() : "";
                        boolean newPlaying = media.getPlaying();
                        long newDuration = media.getDuration();
                        long newPosition = media.getPosition();
                        byte[] newArtwork = media.getArtworkPng();
                        String newOwner = activeSession.getOwner().toLowerCase();
                        boolean trackChanged = !newTitle.equals(title) || !newArtist.equals(artist);
                        boolean playingChanged = newPlaying != playing;
                        boolean durationChanged = newDuration != duration;
                        double currentEstimate = (double)lastPolledPosition + (double)(System.currentTimeMillis() - lastPollSystemTime) / 1000.0;
                        boolean driftDetected = Math.abs((double)newPosition - currentEstimate) > 2.0;
                        title = newTitle;
                        artist = newArtist;
                        duration = newDuration;
                        playing = newPlaying;
                        artworkBytes = newArtwork;
                        String cleanBrand = "System";
                        if (newOwner.contains("spotify")) {
                            cleanBrand = "Spotify";
                        } else if (newOwner.contains("chrome") || newOwner.contains("msedge") || newOwner.contains("firefox") || newOwner.contains("opera") || newOwner.contains("brave")) {
                            String browserBrand = MusicTracker.detectBrowserBrand(newTitle);
                            cleanBrand = browserBrand != null ? browserBrand : (newOwner.contains("chrome") ? "Chrome" : (newOwner.contains("msedge") ? "Edge" : (newOwner.contains("firefox") ? "Firefox" : (newOwner.contains("opera") ? "Opera" : "Browser"))));
                        } else if (newOwner.contains("yandex")) {
                            cleanBrand = "Yandex";
                        }
                        owner = cleanBrand;
                        if (trackChanged || playingChanged || durationChanged || driftDetected || sessionGained) {
                            lastPolledPosition = newPosition;
                            lastPollSystemTime = System.currentTimeMillis();
                        }
                    } else {
                        active = false;
                    }
                    return;
                }
            }
            active = false;
        }
        catch (Throwable ignored) {
            active = false;
        }
    }

    public static boolean haveActiveSession() {
        return active;
    }

    public static String getTitle() {
        return title;
    }

    public static String getArtist() {
        return artist;
    }

    public static long getDuration() {
        return duration;
    }

    public static boolean isPlaying() {
        return playing;
    }

    public static byte[] getArtworkBytes() {
        return artworkBytes;
    }

    public static String getOwner() {
        return owner;
    }

    public static double getPosition() {
        if (!playing) {
            return Math.max(0.0, (double)lastPolledPosition);
        }
        double elapsed = (double)(System.currentTimeMillis() - lastPollSystemTime) / 1000.0;
        return Math.max(0.0, Math.min((double)duration, (double)lastPolledPosition + elapsed));
    }

    public static int getCycle() {
        return cycle;
    }

    public static void setCycle(int c) {
        cycle = c;
    }

    public static void swapCycle() {
        cycle = (cycle + 1) % 3;
    }

    public static String getLyrics() {
        return "";
    }

    public static String getLyrics(String artist, String title) {
        if (artist == null || title == null) {
            return "";
        }
        String fileName = (artist + " - " + title).replaceAll("[\\\\/:*?\"<>|]", "_") + ".txt";
        File file = new File(MinecraftClient.getInstance().runDirectory, "astolfoclient/lyrics/" + fileName);
        if (!file.exists()) {
            fileName = (artist + " - " + title).replaceAll("[\\\\/:*?\"<>|]", "_") + ".lrc";
            file = new File(MinecraftClient.getInstance().runDirectory, "astolfoclient/lyrics/" + fileName);
        }
        if (file.exists()) {
            try {
                return Files.readString(file.toPath(), StandardCharsets.UTF_8);
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
        return "No lyrics found.\nCreate: astolfoclient/lyrics/" + (artist + " - " + title).replaceAll("[\\\\/:*?\"<>|]", "_") + ".txt\n\n\n\n\n";
    }

    public static Identifier getArtworkTexture(byte[] artworkPng) {
        if (artworkPng == null || artworkPng.length == 0) {
            return null;
        }
        if (lastArtworkBytes != null && Arrays.equals(lastArtworkBytes, artworkPng)) {
            return artworkIdentifier;
        }
        MusicTracker.cleanupArtwork();
        try {
            NativeImage nativeImage = NativeImage.read((InputStream)new ByteArrayInputStream(artworkPng));
            if (nativeImage == null) {
                return null;
            }
            artworkTexture = new NativeImageBackedTexture(() -> "Astolfo artwork", nativeImage);
            artworkIdentifier = Identifier.of((String)"astolfoclient", (String)("music_artwork_" + System.currentTimeMillis()));
            MinecraftClient.getInstance().getTextureManager().registerTexture(artworkIdentifier, (AbstractTexture)artworkTexture);
            lastArtworkBytes = artworkPng;
            return artworkIdentifier;
        }
        catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static void cleanupArtwork() {
        if (artworkTexture != null) {
            try {
                MinecraftClient.getInstance().getTextureManager().destroyTexture(artworkIdentifier);
                artworkTexture.close();
            }
            catch (Throwable throwable) {
                // empty catch block
            }
            artworkTexture = null;
            artworkIdentifier = null;
            lastArtworkBytes = null;
        }
    }

    public static void playPause() {
        MusicTracker.sendMediaKey((byte)-77);
    }

    public static void next() {
        MusicTracker.sendMediaKey((byte)-75);
    }

    public static void previous() {
        MusicTracker.sendMediaKey((byte)-76);
    }

    private static String _d(int[] data, int seed) {
        char[] out = new char[data.length];
        for (int i = 0; i < data.length; ++i) {
            int k = seed + (i + 1) * -1640531527;
            k = (k ^ k >>> 16) * -2048144789;
            k = (k ^ k >>> 13) * -1028477387;
            k ^= k >>> 16;
            int b = (k ^ k >>> 8) & 0xFFFF;
            int a = (k >>> 16 | 1) & 0xFFFF;
            int r = (k >>> 5 & 7) + 1;
            int x = k >>> 11 & 0xFFFF;
            int inv = a;
            inv = inv * (2 - a * inv) & 0xFFFF;
            inv = inv * (2 - a * inv) & 0xFFFF;
            inv = inv * (2 - a * inv) & 0xFFFF;
            inv = inv * (2 - a * inv) & 0xFFFF;
            int v3 = data[i] ^ x;
            int v2 = (v3 >>> r | v3 << 16 - r) & 0xFFFF;
            int v1 = v2 * inv & 0xFFFF;
            int c = v1 - b & 0xFFFF;
            out[i] = (char)c;
        }
        return new String(out);
    }

    private static synchronized User32 getUser32() {
        if (user32Instance == null) {
            user32Instance = (User32)Native.load((String)MusicTracker._d(new int[]{9200, 14199, 56005, 51538, 58337, 10139}, 525700510), User32.class);
        }
        return user32Instance;
    }

    private static void sendMediaKey(byte vk) {
        try {
            MusicTracker.getUser32().keybd_event(vk, (byte)0, 0, 0);
            MusicTracker.getUser32().keybd_event(vk, (byte)0, 2, 0);
        }
        catch (Throwable t) {
            t.printStackTrace();
        }
    }

    private static String detectBrowserBrand(String songTitle) {
        if (songTitle == null || songTitle.isEmpty() || songTitle.equals("No Music Playing")) {
            return null;
        }
        final String[] detected = new String[1];
        final String lowerTitle = songTitle.toLowerCase();
        try {
            MusicTracker.getUser32().EnumWindows(new User32.WNDENUMPROC(){

                @Override
                public boolean callback(Pointer hWnd, Pointer arg) {
                    String titleStr;
                    char[] windowText = new char[512];
                    int len = MusicTracker.getUser32().GetWindowTextW(hWnd, windowText, 512);
                    if (len > 0 && (titleStr = new String(windowText, 0, len).toLowerCase()).contains(lowerTitle)) {
                        if (titleStr.contains("youtube music")) {
                            detected[0] = "YT Music";
                            return false;
                        }
                        if (titleStr.contains("youtube")) {
                            detected[0] = "YouTube";
                            return false;
                        }
                        if (titleStr.contains("soundcloud")) {
                            detected[0] = "SoundCloud";
                            return false;
                        }
                    }
                    return true;
                }
            }, null);
        }
        catch (Throwable throwable) {
            // empty catch block
        }
        return detected[0];
    }

    static {
        Thread pollingThread = new Thread(() -> {
            while (true) {
                try {
                    while (true) {
                        MusicTracker.pollMediaSession();
                        Thread.sleep(400L);
                    }
                }
                catch (InterruptedException e) {
                }
                catch (Throwable t) {
                    t.printStackTrace();
                    continue;
                }
                break;
            }
        });
        pollingThread.setName("AstolfoClient-MusicTracker-Poll");
        pollingThread.setDaemon(true);
        pollingThread.start();
        user32Instance = null;
    }

    @Environment(value=EnvType.CLIENT)
    private static interface User32
    extends Library {
        public void keybd_event(byte var1, byte var2, int var3, int var4);

        public boolean EnumWindows(WNDENUMPROC var1, Pointer var2);

        public int GetWindowTextW(Pointer var1, char[] var2, int var3);

        @Environment(value=EnvType.CLIENT)
        public static interface WNDENUMPROC
        extends StdCallLibrary.StdCallCallback {
            public boolean callback(Pointer var1, Pointer var2);
        }
    }
}

