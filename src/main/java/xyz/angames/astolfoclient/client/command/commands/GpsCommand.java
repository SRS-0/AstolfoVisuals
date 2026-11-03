package xyz.angames.astolfoclient.client.command.commands;

import java.util.List;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.Formatting;
import xyz.angames.astolfoclient.client.command.Command;
import xyz.angames.astolfoclient.client.manager.GpsManager;

@Environment(value=EnvType.CLIENT)
public class GpsCommand
extends Command {
    public GpsCommand() {
        super("gps", "Sets a navigation waypoint", "$gps <clear | x z>", new String[0]);
    }

    @Override
    public void execute(String[] args) {
        if (args.length < 1) {
            this.sendSyntax();
            return;
        }
        if (args[0].equalsIgnoreCase("clear")) {
            GpsManager.getInstance().clear();
            GpsCommand.sendMessage(String.valueOf(Formatting.GREEN) + "GPS Waypoint cleared.");
            return;
        }
        if (args.length < 2) {
            this.sendError("Usage: $gps <x> <z> OR $gps clear");
            return;
        }
        try {
            double x = Double.parseDouble(args[0]);
            double z = Double.parseDouble(args[1]);
            GpsManager.getInstance().setWaypoint(x, z);
            GpsCommand.sendMessage(String.valueOf(Formatting.GREEN) + String.format("GPS set to X: %.1f, Z: %.1f", x, z));
        }
        catch (NumberFormatException e) {
            this.sendError("Invalid coordinates. Please enter numbers.");
        }
    }

    @Override
    public List<String> suggest(String[] args) {
        if (args.length == 1) {
            return List.of("clear");
        }
        return super.suggest(args);
    }
}

