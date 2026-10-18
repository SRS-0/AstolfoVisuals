package xyz.angames.astolfoclient.client.gui.clickgui;

import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import dev.sxmurxy.mre.msdf.MsdfFont;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.resources.Identifier;

@Environment(EnvType.CLIENT)
public class ClickGuiIcons {
   public static final Supplier<MsdfFont> CLICKGUI_ICONS = Suppliers.memoize(
      () -> MsdfFont.builder()
         .name("clickgui_icons")
         .data(Identifier.fromNamespaceAndPath("mre", "icons/clickgui/clickgui.json"))
         .atlas(Identifier.fromNamespaceAndPath("mre", "icons/clickgui/clickgui.png"))
         .glyphMapper(g -> 65 + g.index())
         .build()
   );
   public static final Supplier<MsdfFont> SP_FONT = Suppliers.memoize(
      () -> MsdfFont.builder()
         .name("settings_panel")
         .data(Identifier.fromNamespaceAndPath("mre", "icons/watermark/seting-panel/watermark-sp.json"))
         .atlas(Identifier.fromNamespaceAndPath("mre", "icons/watermark/seting-panel/watermark-sp.png"))
         .glyphMapper(g -> 65 + g.index())
         .build()
   );
   public static final Supplier<MsdfFont> ASTOLFO_LOGO = Suppliers.memoize(
      () -> MsdfFont.builder()
         .name("astolfo_logo")
         .data(Identifier.fromNamespaceAndPath("mre", "fonts/astolfo.json"))
         .atlas(Identifier.fromNamespaceAndPath("mre", "fonts/astolfo.png"))
         .build()
   );
   public static final Supplier<MsdfFont> BOLD_FONT = Suppliers.memoize(() -> MsdfFont.builder().atlas("bold").data("bold").build());
   public static final Supplier<MsdfFont> SEMIBOLD_FONT = Suppliers.memoize(() -> MsdfFont.builder().atlas("semibold").data("semibold").build());
   public static final Supplier<MsdfFont> MEDIUM_FONT = Suppliers.memoize(() -> MsdfFont.builder().atlas("medium").data("medium").build());
   public static final Supplier<MsdfFont> REGULAR_FONT = Suppliers.memoize(() -> MsdfFont.builder().atlas("regular").data("regular").build());
   public static final String ICON_SCRIPTS = "D";
   public static final String ICON_CONFIGS = "E";
   public static final String ICON_RENDER = "F";
   public static final String ICON_MISC = "G";
   public static final String ICON_MOVEMENT = "H";
   public static final String ICON_SEARCH = "I";
   public static final String ICON_SETTINGS = "J";
   public static final String ICON_COMBAT = "K";
   public static final String ICON_PLAYER = "L";
   public static final String ICON_FOX_LOGO = "A";
}
