package xyz.angames.astolfoclient.client.command.commands;

import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.ChatFormatting;
import xyz.angames.astolfoclient.client.command.Command;
import xyz.angames.astolfoclient.client.manager.GpsManager;

@Environment(EnvType.CLIENT)
public class GpsCommand extends Command {
   public GpsCommand() {
      super("gps", "Sets a navigation waypoint", "$gps <clear | x z>");
   }

   @Override
   public void execute(String[] args) {
      if (args.length < 1) {
         this.sendSyntax();
      } else if (args[0].equalsIgnoreCase("clear")) {
         GpsManager.getInstance().clear();
         sendMessage(ChatFormatting.GREEN + "GPS Waypoint cleared.");
      } else if (args.length < 2) {
         this.sendError("Usage: $gps <x> <z> OR $gps clear");
      } else {
         try {
            double x = Double.parseDouble(args[0]);
            double z = Double.parseDouble(args[1]);
            GpsManager.getInstance().setWaypoint(x, z);
            sendMessage(ChatFormatting.GREEN + String.format("GPS set to X: %.1f, Z: %.1f", x, z));
         } catch (NumberFormatException e) {
            this.sendError("Invalid coordinates. Please enter numbers.");
         }
      }
   }

   @Override
   public List<String> suggest(String[] args) {
      return args.length == 1 ? List.of("clear") : super.suggest(args);
   }
}
