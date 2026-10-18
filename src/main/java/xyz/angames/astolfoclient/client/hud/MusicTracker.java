package xyz.angames.astolfoclient.client.hud;

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.win32.StdCallLibrary.StdCallCallback;
import dev.redstones.mediaplayerinfo.IMediaSession;
import dev.redstones.mediaplayerinfo.MediaInfo;
import dev.redstones.mediaplayerinfo.MediaPlayerInfo;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.client.Minecraft;

@Environment(EnvType.CLIENT)
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
   private static DynamicTexture artworkTexture = null;
   private static byte[] lastArtworkBytes = null;
   private static MusicTracker.User32 user32Instance = null;

   private static synchronized void pollMediaSession() {
      try {
         List<IMediaSession> sessions = MediaPlayerInfo.Instance.getMediaSessions();
         if (sessions != null && !sessions.isEmpty()) {
            IMediaSession activeSession = null;

            for (IMediaSession s : sessions) {
               if (s != null && s.getMedia() != null && s.getMedia().getPlaying()) {
                  activeSession = s;
                  break;
               }
            }

            if (activeSession == null) {
               for (IMediaSession s : sessions) {
                  if (s != null && s.getMedia() != null) {
                     activeSession = s;
                     break;
                  }
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
                  double currentEstimate = lastPolledPosition + (System.currentTimeMillis() - lastPollSystemTime) / 1000.0;
                  boolean driftDetected = Math.abs(newPosition - currentEstimate) > 2.0;
                  title = newTitle;
                  artist = newArtist;
                  duration = newDuration;
                  playing = newPlaying;
                  artworkBytes = newArtwork;
                  String cleanBrand = "System";
                  if (newOwner.contains("spotify")) {
                     cleanBrand = "Spotify";
                  } else if (newOwner.contains("chrome")
                     || newOwner.contains("msedge")
                     || newOwner.contains("firefox")
                     || newOwner.contains("opera")
                     || newOwner.contains("brave")) {
                     String browserBrand = detectBrowserBrand(newTitle);
                     if (browserBrand != null) {
                        cleanBrand = browserBrand;
                     } else {
                        cleanBrand = newOwner.contains("chrome")
                           ? "Chrome"
                           : (
                              newOwner.contains("msedge")
                                 ? "Edge"
                                 : (newOwner.contains("firefox") ? "Firefox" : (newOwner.contains("opera") ? "Opera" : "Browser"))
                           );
                     }
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
      } catch (Throwable ignored) {
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
         return Math.max(0.0, lastPolledPosition);
      }

      double elapsed = (System.currentTimeMillis() - lastPollSystemTime) / 1000.0;
      return Math.max(0.0, Math.min(duration, lastPolledPosition + elapsed));
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
      if (artist != null && title != null) {
         String fileName = (artist + " - " + title).replaceAll("[\\\\/:*?\"<>|]", "_") + ".txt";
         File file = new File(Minecraft.getInstance().gameDirectory, "astolfoclient/lyrics/" + fileName);
         if (!file.exists()) {
            fileName = (artist + " - " + title).replaceAll("[\\\\/:*?\"<>|]", "_") + ".lrc";
            file = new File(Minecraft.getInstance().gameDirectory, "astolfoclient/lyrics/" + fileName);
         }

         if (file.exists()) {
            try {
               return Files.readString(file.toPath(), StandardCharsets.UTF_8);
            } catch (Exception e) {
               e.printStackTrace();
            }
         }

         return "No lyrics found.\nCreate: astolfoclient/lyrics/" + (artist + " - " + title).replaceAll("[\\\\/:*?\"<>|]", "_") + ".txt\n\n\n\n\n";
      } else {
         return "";
      }
   }

   public static Identifier getArtworkTexture(byte[] artworkPng) {
      if (artworkPng == null || artworkPng.length == 0) {
         return null;
      }

      if (lastArtworkBytes != null && Arrays.equals(lastArtworkBytes, artworkPng)) {
         return artworkIdentifier;
      }

      cleanupArtwork();

      try {
         NativeImage nativeImage = NativeImage.read(new ByteArrayInputStream(artworkPng));
         if (nativeImage == null) {
            return null;
         }

         artworkTexture = new DynamicTexture(() -> "astolfo artwork", nativeImage);
         artworkIdentifier = Identifier.fromNamespaceAndPath("astolfoclient", "music_artwork_" + System.currentTimeMillis());
         Minecraft.getInstance().getTextureManager().register(artworkIdentifier, artworkTexture);
         lastArtworkBytes = artworkPng;
         return artworkIdentifier;
      } catch (Exception e) {
         e.printStackTrace();
         return null;
      }
   }

   public static void cleanupArtwork() {
      if (artworkTexture != null) {
         try {
            Minecraft.getInstance().getTextureManager().release(artworkIdentifier);
            artworkTexture.close();
         } catch (Throwable var1) {
         }

         artworkTexture = null;
         artworkIdentifier = null;
         lastArtworkBytes = null;
      }
   }

   public static void playPause() {
      sendMediaKey((byte)-77);
   }

   public static void next() {
      sendMediaKey((byte)-75);
   }

   public static void previous() {
      sendMediaKey((byte)-76);
   }

   private static String _d(int[] data, int seed) {
      char[] out = new char[data.length];

      for (int i = 0; i < data.length; i++) {
         int k = seed + (i + 1) * -1640531527;
         k = (k ^ k >>> 16) * -2048144789;
         k = (k ^ k >>> 13) * -1028477387;
         k ^= k >>> 16;
         int b = (k ^ k >>> 8) & 65535;
         int a = (k >>> 16 | 1) & 65535;
         int r = (k >>> 5 & 7) + 1;
         int x = k >>> 11 & 65535;
         int inv = a;
         inv = inv * (2 - a * inv) & 65535;
         inv = inv * (2 - a * inv) & 65535;
         inv = inv * (2 - a * inv) & 65535;
         inv = inv * (2 - a * inv) & 65535;
         int v3 = data[i] ^ x;
         int v2 = (v3 >>> r | v3 << 16 - r) & 65535;
         int v1 = v2 * inv & 65535;
         int c = v1 - b & 65535;
         out[i] = (char)c;
      }

      return new String(out);
   }

   private static synchronized MusicTracker.User32 getUser32() {
      if (user32Instance == null) {
         user32Instance = (MusicTracker.User32)Native.load(_d(new int[]{9200, 14199, 56005, 51538, 58337, 10139}, 525700510), MusicTracker.User32.class);
      }

      return user32Instance;
   }

   private static void sendMediaKey(byte vk) {
      try {
         getUser32().keybd_event(vk, (byte)0, 0, 0);
         getUser32().keybd_event(vk, (byte)0, 2, 0);
      } catch (Throwable t) {
         t.printStackTrace();
      }
   }

   private static String detectBrowserBrand(String songTitle) {
      if (songTitle != null && !songTitle.isEmpty() && !songTitle.equals("No Music Playing")) {
         final String[] detected = new String[1];
         final String lowerTitle = songTitle.toLowerCase();

         try {
            getUser32().EnumWindows(new MusicTracker.User32.WNDENUMPROC() {
               @Override
               public boolean callback(Pointer hWnd, Pointer arg) {
                  char[] windowText = new char[512];
                  int len = MusicTracker.getUser32().GetWindowTextW(hWnd, windowText, 512);
                  if (len > 0) {
                     String titleStr = new String(windowText, 0, len).toLowerCase();
                     if (titleStr.contains(lowerTitle)) {
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
                  }

                  return true;
               }
            }, null);
         } catch (Throwable var4) {
         }

         return detected[0];
      } else {
         return null;
      }
   }

   static {
      Thread pollingThread = new Thread(() -> {
         while (true) {
            try {
               pollMediaSession();
               Thread.sleep(400L);
            } catch (InterruptedException e) {
               return;
            } catch (Throwable t) {
               t.printStackTrace();
            }
         }
      });
      pollingThread.setName("AstolfoClient-MusicTracker-Poll");
      pollingThread.setDaemon(true);
      pollingThread.start();
   }

   @Environment(EnvType.CLIENT)
   private interface User32 extends Library {
      void keybd_event(byte var1, byte var2, int var3, int var4);

      boolean EnumWindows(MusicTracker.User32.WNDENUMPROC var1, Pointer var2);

      int GetWindowTextW(Pointer var1, char[] var2, int var3);

      @Environment(EnvType.CLIENT)
      interface WNDENUMPROC extends StdCallCallback {
         boolean callback(Pointer var1, Pointer var2);
      }
   }
}
