package xyz.angames.astolfoclient.client.util;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(EnvType.CLIENT)
public class RandomNameGenerator {
   private static final Random RNG = new Random();
   private static final Set<String> SESSION_USED_NAMES = Collections.newSetFromMap(new ConcurrentHashMap<>());
   private static final List<String> ONLINE_NAME_CACHE = Collections.synchronizedList(new ArrayList<>());
   private static final ExecutorService ASYNC_FETCHER = Executors.newSingleThreadExecutor(r -> {
      Thread t = new Thread(r, "Astolfo-NameMC-Fetcher");
      t.setDaemon(true);
      return t;
   });
   private static boolean fetchStarted = false;
   private static final String[] BASE_WORDS = new String[]{
      "Shadow",
      "Frost",
      "Viper",
      "Ghost",
      "Lunar",
      "Nova",
      "Blaze",
      "Storm",
      "Echo",
      "Aero",
      "Zenith",
      "Pulse",
      "Pixel",
      "Nexus",
      "Phantom",
      "Spark",
      "Drift",
      "Abyss",
      "Astral",
      "Cosmo",
      "Raven",
      "Onyx",
      "Cobalt",
      "Quartz",
      "Apex",
      "Helix",
      "Cipher",
      "Vortex",
      "Mystic",
      "Solar",
      "Glint",
      "Kitsune",
      "Sakura",
      "Velvet",
      "Rogue",
      "Prism",
      "Flux",
      "Aura",
      "Static",
      "Bloom",
      "Mirage",
      "Spectre",
      "Zephyr",
      "Scythe",
      "Shade",
      "Glitch",
      "Rift",
      "Enigma",
      "Karma",
      "Frenzy",
      "Havoc",
      "Chaos",
      "Venom",
      "Titan",
      "Hydra",
      "Breeze",
      "Clover",
      "Crimson",
      "Frosty",
      "Glow",
      "Hazard",
      "Hollow",
      "Inferno",
      "Legacy",
      "Lucid",
      "Matrix",
      "Nebula",
      "Orbit",
      "Poison",
      "Quasar",
      "Radiant",
      "Reaper",
      "Rust",
      "Sapphire",
      "Scorch",
      "Shimmer",
      "Silent",
      "Silver",
      "Siren",
      "Sonic",
      "Spectral",
      "Spirit",
      "Stealth",
      "Thunder",
      "Toxic",
      "Valor",
      "Vector",
      "Void",
      "Voltage",
      "Winter",
      "Wraith",
      "Zero",
      "Cinder",
      "Tempest",
      "Vertex",
      "Chrono",
      "Dusk",
      "Dawn",
      "Eclipse",
      "Flare",
      "Ignite",
      "Omen",
      "Pinnacle",
      "Rune",
      "Sunder",
      "Talon",
      "Valkyrie",
      "Whisper",
      "Zeal",
      "Zen",
      "Flint",
      "Blade",
      "Summit",
      "Aegis",
      "Solace",
      "Solitude",
      "Astrid",
      "Bliss",
      "Cosmic",
      "Dynamo",
      "Embers",
      "Fable",
      "Glimmer",
      "Halo",
      "Icon",
      "Lynx",
      "Monarch",
      "Nimbus",
      "Oasis",
      "Plasma",
      "Quantum",
      "Rapture",
      "Shine",
      "Tide",
      "Utopia",
      "Vivid",
      "Wave",
      "Xeno",
      "Yield",
      "Crag",
      "Dune",
      "Edge",
      "Fang",
      "Gale",
      "Haze",
      "Iris",
      "Jade",
      "Kite",
      "Loom",
      "Moss",
      "Nook",
      "Peak",
      "Quill",
      "Reef",
      "Silt",
      "Thorn",
      "Vale",
      "Wisp",
      "Yew",
      "Zinc",
      "Blizzard",
      "Cyclone",
      "Tsunami",
      "Avalanche",
      "Obsidian",
      "Diamond",
      "Emerald",
      "Nether",
      "Ender",
      "Bedrock",
      "Beacon",
      "Totem",
      "Elytra",
      "Trident",
      "Netherite",
      "Amethyst",
      "Copper",
      "Sculk",
      "Crafter",
      "Mace",
      "Gryphon",
      "Chimera",
      "Basilisk",
      "Kraken",
      "Leviathan",
      "Wyvern",
      "Siren"
   };
   private static final String[] ADJECTIVES = new String[]{
      "Dark",
      "Cold",
      "Wild",
      "Fast",
      "Mad",
      "Brave",
      "Lost",
      "Grim",
      "Deep",
      "Holy",
      "Pure",
      "Iron",
      "Neon",
      "Red",
      "Blue",
      "Gold",
      "Cyber",
      "Silent",
      "Swift",
      "Hyper",
      "Toxic",
      "Super",
      "Epic",
      "Ultra",
      "Mega",
      "Alpha",
      "Omega",
      "Prime",
      "Fierce",
      "Grand",
      "True",
      "Night",
      "Star",
      "Solar",
      "Lunar",
      "Ghost",
      "Frost",
      "Shadow",
      "Royal",
      "Savage",
      "Noble",
      "Quick",
      "Ancient",
      "Astral",
      "Atomic",
      "Bitter",
      "Blazing",
      "Blind",
      "Bold",
      "Bright",
      "Brutal",
      "Calm",
      "Chill",
      "Clear",
      "Clever",
      "Cloudy",
      "Crazy",
      "Cruel",
      "Crystal",
      "Cursed",
      "Daring",
      "Deadly",
      "Divine",
      "Dread",
      "Elite",
      "Faded",
      "Fatal",
      "Feral",
      "Final",
      "Flash",
      "Flawless",
      "Floral",
      "Frozen",
      "Furious",
      "Ghostly",
      "Giant",
      "Gloomy",
      "Glorious",
      "Golden",
      "Graceful",
      "Heavy",
      "Hidden",
      "High",
      "Hollow",
      "Immortal",
      "Imperial",
      "Infinite",
      "Lethal",
      "Light",
      "Liquid",
      "Lonely",
      "Loyal",
      "Lucky",
      "Magic",
      "Mighty",
      "Mythic",
      "Null",
      "Pastel",
      "Phantom",
      "Primal",
      "Proud",
      "Quiet",
      "Rapid",
      "Rare",
      "Reckless",
      "Retro",
      "Sacred",
      "Secret",
      "Sharp",
      "Shining",
      "Simple",
      "Sinister",
      "Sleek",
      "Smooth",
      "Solid",
      "Sour",
      "Steel",
      "Stern",
      "Stolen",
      "Subtle",
      "Sunlit",
      "Supreme",
      "Sweet",
      "Tragic",
      "Twin",
      "Ultima",
      "Undead",
      "Urban",
      "Vague",
      "Vain",
      "Valiant",
      "Velvet",
      "Venomous",
      "Vicious",
      "Violet",
      "Vital",
      "Vivid",
      "Wicked",
      "Wise",
      "Young"
   };
   private static final String[] NOUNS = new String[]{
      "Knight",
      "Wolf",
      "Bite",
      "Walker",
      "Rider",
      "Dragon",
      "Ninja",
      "Claw",
      "Blade",
      "Panda",
      "Fox",
      "Tiger",
      "Hawk",
      "Eagle",
      "Heart",
      "Echo",
      "Shade",
      "Gazer",
      "Dust",
      "Arrow",
      "Drive",
      "Hunter",
      "Keeper",
      "Reaper",
      "Flake",
      "Fly",
      "Fall",
      "Shot",
      "King",
      "Lord",
      "Soul",
      "Star",
      "Cloud",
      "Flame",
      "Shield",
      "Sword",
      "Striker",
      "Runner",
      "Seeker",
      "Master",
      "Guard",
      "Beast",
      "Fang",
      "Raven",
      "Demon",
      "Angel",
      "Warrior",
      "Samurai",
      "Mage",
      "Slayer",
      "Warden",
      "Titan",
      "Ranger",
      "Phoenix",
      "Sniper",
      "Assassin",
      "Bandit",
      "Berserker",
      "Bishop",
      "Captain",
      "Champion",
      "Commander",
      "Crusader",
      "Duelist",
      "Emperor",
      "General",
      "Gladiator",
      "Guardian",
      "Hero",
      "Monk",
      "Nomad",
      "Officer",
      "Paladin",
      "Pilot",
      "Pirate",
      "Prince",
      "Rebel",
      "Rogue",
      "Sage",
      "Scout",
      "Sentinel",
      "Shaman",
      "Soldier",
      "Specter",
      "Spy",
      "Stalker",
      "Strategist",
      "Summoner",
      "Templar",
      "Thief",
      "Vanguard",
      "Veteran",
      "Viking",
      "Villain",
      "Voyager",
      "Warlock",
      "Warlord",
      "Wizard",
      "Zealot",
      "Anvil",
      "Anchor",
      "Armor",
      "Axe",
      "Banner",
      "Barrier",
      "Bell",
      "Bolt",
      "Bomb",
      "Bow",
      "Brand",
      "Bridge",
      "Bullet",
      "Cage",
      "Cannon",
      "Cape",
      "Castle",
      "Chain",
      "Chalice",
      "Chest",
      "Circle",
      "Citadel",
      "Cloak",
      "Coil",
      "Compass",
      "Core",
      "Crown",
      "Crush",
      "Crypt",
      "Cube",
      "Dagger",
      "Dart",
      "Disc",
      "Dome",
      "Feather",
      "Flag",
      "Flash",
      "Forge",
      "Fortress",
      "Gauntlet",
      "Gear",
      "Gem",
      "Glaive",
      "Globe",
      "Hammer",
      "Helm",
      "Horn",
      "Javelin",
      "Key",
      "Lantern",
      "Lock",
      "Mask",
      "Mirror",
      "Orb",
      "Pillar",
      "Potion",
      "Quiver",
      "Ring",
      "Rod",
      "Rope",
      "Scale",
      "Scepter",
      "Scroll",
      "Shard",
      "Sigil",
      "Staff",
      "Statue",
      "Talisman",
      "Tome",
      "Torch",
      "Tower",
      "Trap",
      "Vessel",
      "Wand",
      "Wheel",
      "Whip",
      "Wing"
   };
   private static final String[] SHORT_NAMES = new String[]{
      "Kiro",
      "Zayd",
      "Raze",
      "Vex",
      "Jinx",
      "Kobe",
      "Taro",
      "Milo",
      "Axel",
      "Finn",
      "Zane",
      "Cole",
      "Luka",
      "Niko",
      "Enzo",
      "Theo",
      "Ezra",
      "Leon",
      "Mika",
      "Yuki",
      "Koa",
      "Zack",
      "Sora",
      "Levi",
      "Ryder",
      "Nash",
      "Jax",
      "Beau",
      "Tate",
      "Knox",
      "Cruz",
      "Kade",
      "Gage",
      "Crew",
      "Jett",
      "Dax",
      "Zeke",
      "Boone",
      "Colt",
      "Flynn",
      "Remi",
      "Onyx",
      "Cash",
      "Orion",
      "Silas",
      "Atlas",
      "Toby",
      "Liam",
      "Noah",
      "Kai",
      "Leo",
      "Eli",
      "Max",
      "Ian",
      "Sam",
      "Ben",
      "Dan",
      "Jay",
      "Ray",
      "Ace",
      "Rex",
      "Roy",
      "Guy",
      "Ash",
      "Sky",
      "Neo",
      "Rio",
      "Taj",
      "Kye",
      "Dov",
      "Dex",
      "Ren",
      "Ken",
      "Shin",
      "Jin",
      "Ryu",
      "Hao",
      "Zen",
      "Kael",
      "Bryn",
      "Zade",
      "Nox",
      "Voss",
      "Kyro",
      "Nyx",
      "Rift",
      "Vail",
      "Cove",
      "Faye",
      "Lumi",
      "Omi",
      "Kip",
      "Brix",
      "Bram",
      "Cade",
      "Dion",
      "Eros",
      "Huck",
      "Keon",
      "Lark",
      "Mael",
      "Oren",
      "Pike",
      "Quen",
      "Roan",
      "Seth",
      "Trev",
      "Vane",
      "Wren",
      "Yael",
      "Zev"
   };
   private static final String[] ANIME_GAMER = new String[]{
      "Kirito",
      "Tanjiro",
      "Gojo",
      "Sukuna",
      "Mikasa",
      "Killua",
      "Itachi",
      "Sasuke",
      "Kakashi",
      "Luffy",
      "Zoro",
      "Sanji",
      "Deku",
      "Bakugo",
      "Todoroki",
      "Kenma",
      "Kageyama",
      "Hinata",
      "Asta",
      "Yami",
      "Megumi",
      "Toji",
      "Yuji",
      "Choso",
      "Mahito",
      "Geto",
      "Inumaki",
      "Denji",
      "Makima",
      "Giyuu",
      "Rengoku",
      "Shinobu",
      "Zenitsu",
      "Inosuke",
      "Muichiro",
      "Obanai",
      "Sanemi",
      "Dio",
      "Jotaro",
      "Kira",
      "Giorno",
      "Eren",
      "Levi",
      "Armin",
      "Madara",
      "Minato",
      "Neji",
      "Gaara",
      "Alucard",
      "Kaneki",
      "Kurapika",
      "Hisoka",
      "Chrollo",
      "Feitan",
      "Illumi",
      "Rin",
      "Yukio",
      "Archer",
      "Gilgamesh",
      "Shirou",
      "Saber",
      "Lancer",
      "Kirei",
      "Sinbad",
      "Judal",
      "Akame",
      "Esdeath",
      "Mob",
      "Reigen",
      "Guts",
      "Griffith",
      "Spike",
      "Vash",
      "Edward",
      "Mustang"
   };
   private static final String[] REAL_MC_SEEDS = new String[]{
      "Technoblade",
      "Dream",
      "Sapnap",
      "GeorgeNotFound",
      "TommyInnit",
      "Tubbo",
      "WilburSoot",
      "Philza",
      "Ranboo",
      "BadBoyHalo",
      "Skeppy",
      "Purpled",
      "Bitzel",
      "Wisp",
      "TapL",
      "Fruitberries",
      "Illumina",
      "Feinberg",
      "ClownPierce",
      "Rekrap2",
      "Roier",
      "Quackity",
      "FoolishG",
      "Punz",
      "Hannahxxrose",
      "CaptainSparklez",
      "DanTDM",
      "Stampy",
      "PopularMMOs",
      "MumboJumbo",
      "Grian",
      "Scar",
      "EthosLab",
      "Docm77",
      "BdoubleO100",
      "Rendog",
      "Iskall85",
      "Stressmonster",
      "FalseSymmetry",
      "Zedaph",
      "Tango",
      "ImpulseSV",
      "VintageBeef",
      "xBCrafted",
      "Keralis",
      "Cubfan135",
      "Smallishbeans",
      "LDShadowLady",
      "Smajor1995",
      "SolidarityGaming",
      "Defiant",
      "Spectacle",
      "Veritas",
      "Zephyrus",
      "Challenger",
      "Vanguard",
      "Overlord",
      "Imperator",
      "Dominator",
      "Oblivion",
      "Cataclysm",
      "Resilience",
      "Eternity",
      "Immortal",
      "Ascendant",
      "Requiem",
      "Seraph",
      "Valiant",
      "Solitary",
      "Revenant",
      "Insurgent",
      "Nemesis",
      "Outlaw",
      "Renegade",
      "Vagrant",
      "Ronin",
      "Shinobi",
      "Spook",
      "Riddle",
      "Horizon",
      "Supernova",
      "Hypernova",
      "Pulsar",
      "Galaxy",
      "Comet",
      "Meteor",
      "Starlight",
      "Sunburst",
      "Moonbeam",
      "Twilight",
      "Midnight",
      "Aurora",
      "Borealis",
      "Australis",
      "Corona",
      "Luminescence",
      "Permafrost",
      "Infernal",
      "Tartarus"
   };
   private static final String[] PREFIXES = new String[]{
      "Not",
      "Its",
      "The",
      "Real",
      "Im",
      "Just",
      "Mr",
      "Sir",
      "x_",
      "i",
      "v_",
      "Lil",
      "Big",
      "Hey",
      "Oh",
      "OG_",
      "Official",
      "ii_",
      "z_",
      "q_",
      "el_",
      "Lord",
      "Saint",
      "Zen_",
      "Neo_",
      "Dr_",
      "Pro_",
      "Rx_",
      "Fx_",
      "Gx_"
   };
   private static final String[] SUFFIXES = new String[]{
      "y",
      "er",
      "ed",
      "ly",
      "ing",
      "MC",
      "PvP",
      "_",
      "x",
      "HD",
      "XD",
      "GG",
      "Plays",
      "YT",
      "God",
      "Lord",
      "Boy",
      "Girl",
      "King",
      "Guy",
      "Dev",
      "Pro",
      "Craft",
      "Zone",
      "v",
      "q",
      "z",
      "FN",
      "Live",
      "TTV",
      "VFX",
      "SFX",
      "Club",
      "Squad",
      "Gang",
      "Clan",
      "HQ",
      "Lab",
      "Hub",
      "Net",
      "Dot"
   };
   private static final String[] NUMBERS = new String[]{
      "0", "1", "7", "69", "99", "123", "777", "007", "404", "2024", "2025", "2026", "360", "420", "2k", "88", "11", "22", "33", "01", "07", "13", "17", "999"
   };
   private static final String[] SYLLABLE_ROOTS = new String[]{
      "Ash",
      "Kip",
      "Zan",
      "Rox",
      "Ven",
      "Lyn",
      "Tor",
      "Kael",
      "Drak",
      "Vex",
      "Syl",
      "Mor",
      "Fen",
      "Sol",
      "Lun",
      "Pyr",
      "Cry",
      "Aer",
      "Hyd",
      "Geo",
      "Nex",
      "Vor",
      "Zel",
      "Thal",
      "Rhys",
      "Krag",
      "Vane",
      "Bly",
      "Cael",
      "Dael"
   };
   private static final String[] SYLLABLE_ENDS = new String[]{
      "is", "ar", "en", "os", "um", "ix", "or", "an", "ex", "on", "us", "ia", "ae", "el", "yn", "ir", "ath", "oth", "eth", "ith", "ius", "ox", "yx", "orx"
   };

   public static synchronized void triggerAsyncNameMCFetch() {
      if (!fetchStarted) {
         fetchStarted = true;
         ASYNC_FETCHER.submit(
            () -> {
               try {
                  String[] sources = new String[]{
                     "https://namemc.com/minecraft-names", "https://raw.githubusercontent.com/jeanphorn/wordlist-miners/master/usernames/minecraft.txt"
                  };

                  for (String sourceUrl : sources) {
                     try {
                        URL url = URI.create(sourceUrl).toURL();
                        HttpURLConnection conn = (HttpURLConnection)url.openConnection();
                        conn.setRequestMethod("GET");
                        conn.setConnectTimeout(2000);
                        conn.setReadTimeout(2500);
                        conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AstolfoClient/1.0");
                        if (conn.getResponseCode() == 200) {
                           try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                              int count = 0;
                              Pattern pattern = Pattern.compile("^[a-zA-Z0-9_]{3,16}$");
                              Pattern linkPattern = Pattern.compile("/name/([a-zA-Z0-9_]{3,16})");

                              String line;
                              while ((line = reader.readLine()) != null && count < 600) {
                                 line = line.trim();
                                 Matcher lm = linkPattern.matcher(line);
                                 if (lm.find()) {
                                    String extracted = lm.group(1);
                                    if (extracted.length() >= 3 && extracted.length() <= 16) {
                                       ONLINE_NAME_CACHE.add(extracted);
                                       count++;
                                    }
                                 } else if (pattern.matcher(line).matches()) {
                                    ONLINE_NAME_CACHE.add(line);
                                    count++;
                                 }
                              }
                           }
                        }
                     } catch (Throwable var16) {
                     }

                     if (!ONLINE_NAME_CACHE.isEmpty()) {
                        break;
                     }
                  }
               } catch (Throwable var17) {
               }
            }
         );
      }
   }

   public static synchronized String generateUniqueName() {
      Set<String> existingNames = new HashSet<>(SESSION_USED_NAMES);

      try {
         Object accounts = Class.forName("ru.vidtu.ias.config.IASStorage").getField("ACCOUNTS").get(null);
         if (accounts instanceof Iterable<?> list) {
            for (Object acc : list) {
               if (acc != null) {
                  Object name = acc.getClass().getMethod("name").invoke(acc);
                  if (name != null) {
                     existingNames.add(name.toString().toLowerCase(Locale.ROOT));
                  }
               }
            }
         }
      } catch (Throwable var4) {
      }

      for (int attempts = 0; attempts < 1500; attempts++) {
         String candidate = generateCandidate();
         if (candidate != null && candidate.length() >= 3 && candidate.length() <= 16 && candidate.matches("^[a-zA-Z0-9_]+$")) {
            String lower = candidate.toLowerCase(Locale.ROOT);
            if (!existingNames.contains(lower)) {
               SESSION_USED_NAMES.add(lower);
               return candidate;
            }
         }
      }

      String fallback = "User_" + (1000 + RNG.nextInt(9000));

      while (existingNames.contains(fallback.toLowerCase(Locale.ROOT))) {
         fallback = "User_" + RNG.nextInt(100000);
      }

      SESSION_USED_NAMES.add(fallback.toLowerCase(Locale.ROOT));
      return fallback;
   }

   private static String generateCandidate() {
      if (!ONLINE_NAME_CACHE.isEmpty() && RNG.nextInt(100) < 45) {
         String onlineName = ONLINE_NAME_CACHE.get(RNG.nextInt(ONLINE_NAME_CACHE.size()));
         return sliceAndFuse(onlineName);
      }

      int style = RNG.nextInt(14);
      switch (style) {
         case 0: {
            String p = pick(PREFIXES);
            String b = pick(BASE_WORDS);
            return trimToMax(p + b);
         }
         case 1: {
            String b = pick(BASE_WORDS);
            String s = pick(SUFFIXES);
            return trimToMax(b + s);
         }
         case 2: {
            String a = pick(ADJECTIVES);
            String n = pick(NOUNS);
            return trimToMax(a + n);
         }
         case 3: {
            String a = pick(ADJECTIVES);
            String n = pick(NOUNS);
            return trimToMax(a + "_" + n);
         }
         case 4: {
            String b = pick(BASE_WORDS);
            String num = pick(NUMBERS);
            boolean under = RNG.nextBoolean();
            return trimToMax(under ? b + "_" + num : b + num);
         }
         case 5: {
            String s = pick(SHORT_NAMES);
            if (RNG.nextBoolean()) {
               return trimToMax(s + (RNG.nextBoolean() ? "_" : "") + pick(NUMBERS));
            }

            return trimToMax(s + (RNG.nextBoolean() ? "_" : "") + pick(SUFFIXES));
         }
         case 6: {
            String b = pick(BASE_WORDS);
            if (RNG.nextBoolean()) {
               return trimToMax("_" + b + "_");
            }

            return trimToMax("xX_" + b + "_Xx");
         }
         case 7: {
            String b = pick(BASE_WORDS);
            String leet = applySubtleLeet(b);
            if (RNG.nextBoolean()) {
               leet = (RNG.nextBoolean() ? "i_" : "x_") + leet;
            } else if (RNG.nextBoolean()) {
               leet = leet + "_";
            }

            return trimToMax(leet);
         }
         case 8:
            String an = pick(ANIME_GAMER);
            if (RNG.nextBoolean()) {
               return trimToMax(pick(PREFIXES) + an);
            }

            return trimToMax(an + (RNG.nextBoolean() ? "_" : "") + (RNG.nextBoolean() ? pick(NUMBERS) : pick(SUFFIXES)));
         case 9: {
            String p = pick(PREFIXES);
            if (!p.endsWith("_")) {
               p = p + "_";
            }

            String b = pick(BASE_WORDS);
            String s = pick(SUFFIXES);
            return trimToMax(p + b + s);
         }
         case 10:
            String seed = pick(REAL_MC_SEEDS);
            return sliceAndFuse(seed);
         case 11:
            String root = pick(SYLLABLE_ROOTS);
            String end = pick(SYLLABLE_ENDS);
            if (RNG.nextBoolean()) {
               return trimToMax(root + end + (RNG.nextBoolean() ? "_" : "") + (RNG.nextBoolean() ? pick(NUMBERS) : ""));
            }

            return trimToMax(pick(PREFIXES) + root + end);
         case 12: {
            String s = pick(SHORT_NAMES);
            if (RNG.nextBoolean()) {
               return s;
            }

            return trimToMax(s + (RNG.nextBoolean() ? "_" : (RNG.nextBoolean() ? "x" : "v")));
         }
         default: {
            String b = pick(BASE_WORDS);
            String tag = RNG.nextBoolean() ? "q" : (RNG.nextBoolean() ? "v" : (RNG.nextBoolean() ? "z" : "x"));
            return trimToMax(tag + b);
         }
      }
   }

   private static String sliceAndFuse(String sourceName) {
      if (sourceName != null && sourceName.length() >= 3) {
         String[] tokens = sourceName.split("(?<=[a-z])(?=[A-Z])|(?<=[A-Za-z])(?=[0-9])|(?<=[0-9])(?=[A-Za-z])|_");
         List<String> cleanTokens = new ArrayList<>();

         for (String t : tokens) {
            if (t != null && t.length() >= 2) {
               cleanTokens.add(t);
            }
         }

         int subMode = RNG.nextInt(5);
         if (!cleanTokens.isEmpty() && subMode == 0) {
            String token = cleanTokens.get(RNG.nextInt(cleanTokens.size()));
            return trimToMax(token + (RNG.nextBoolean() ? "_" : "") + pick(BASE_WORDS));
         } else if (!cleanTokens.isEmpty() && subMode == 1) {
            String token = cleanTokens.get(RNG.nextInt(cleanTokens.size()));
            return trimToMax(pick(ADJECTIVES) + (RNG.nextBoolean() ? "_" : "") + token);
         } else if (subMode == 2) {
            String p = pick(PREFIXES);
            return trimToMax(p + (p.endsWith("_") ? "" : (RNG.nextBoolean() ? "_" : "")) + sourceName);
         } else {
            return subMode == 3
               ? trimToMax(sourceName + (RNG.nextBoolean() ? "_" : "") + (RNG.nextBoolean() ? pick(SUFFIXES) : pick(NUMBERS)))
               : trimToMax(applySubtleLeet(sourceName));
         }
      } else {
         return pick(BASE_WORDS) + pick(SUFFIXES);
      }
   }

   private static String applySubtleLeet(String word) {
      char[] chars = word.toCharArray();
      int changed = 0;

      for (int i = 0; i < chars.length && changed < 2; i++) {
         char c = Character.toLowerCase(chars[i]);
         if (c == 'o') {
            chars[i] = '0';
            changed++;
         } else if (c == 'e') {
            chars[i] = '3';
            changed++;
         } else if (c == 'a') {
            chars[i] = '4';
            changed++;
         } else if (c == 'i') {
            chars[i] = '1';
            changed++;
         } else if (c == 's' && i > 0) {
            chars[i] = '5';
            changed++;
         } else if (c == 't' && i > 0) {
            chars[i] = '7';
            changed++;
         }
      }

      return new String(chars);
   }

   private static String pick(String[] array) {
      return array[RNG.nextInt(array.length)];
   }

   private static String trimToMax(String str) {
      return str.length() > 16 ? str.substring(0, 16) : str;
   }

   static {
      triggerAsyncNameMCFetch();
   }
}
