package xyz.angames.astolfoclient.client.command.commands;

import com.mojang.authlib.GameProfile;
import java.util.UUID;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.ChatFormatting;
import net.minecraft.world.entity.Entity;
import xyz.angames.astolfoclient.client.command.Command;
import xyz.angames.astolfoclient.client.util.FakePlayerEntity;

@Environment(value=EnvType.CLIENT)
public class FakePlayerCommand
extends Command {
    public FakePlayerCommand() {
        super("fakeplayer", "Spawns/Despawns a client-side fake player for module testing", "$fakeplayer [spawn <name> | despawn]", new String[0]);
    }

    @Override
    public void execute(String[] args) {
        if (this.mc.player == null || this.mc.level == null) {
            this.sendError("You must be in-game to use this command.");
            return;
        }
        if (args.length == 0) {
            if (FakePlayerEntity.instance != null) {
                this.despawn();
            } else {
                this.spawn("FakePlayer");
            }
            return;
        }
        String action = args[0].toLowerCase();
        if (action.equals("spawn")) {
            String name = "FakePlayer";
            if (args.length > 1 && !args[1].trim().isEmpty()) {
                name = args[1];
            }
            this.spawn(name);
        } else if (action.equals("despawn") || action.equals("remove") || action.equals("clear") || action.equals("off")) {
            this.despawn();
        } else {
            this.sendError("Unknown action. Usage: " + this.getSyntax());
        }
    }

    private void spawn(String name) {
        if (FakePlayerEntity.instance != null) {
            this.despawn();
        }
        GameProfile profile = new GameProfile(UUID.randomUUID(), name);
        FakePlayerEntity fakePlayer = new FakePlayerEntity(this.mc.level, profile);
        fakePlayer.copyPosition((Entity)this.mc.player);
        fakePlayer.yHeadRot = this.mc.player.yHeadRot;
        fakePlayer.yBodyRot = this.mc.player.yBodyRot;
        fakePlayer.getInventory().replaceWith(this.mc.player.getInventory());
        int fakeId = -987654;
        fakePlayer.setId(fakeId);
        this.mc.level.addEntity((Entity)fakePlayer);
        FakePlayerEntity.instance = fakePlayer;
        FakePlayerCommand.sendMessage("Spawned fake player: " + String.valueOf(ChatFormatting.AQUA) + name + String.valueOf(ChatFormatting.GRAY) + " (ID: " + fakeId + ")");
    }

    private void despawn() {
        if (FakePlayerEntity.instance != null) {
            int id = FakePlayerEntity.instance.getId();
            FakePlayerEntity.instance.discard();
            this.mc.level.removeEntity(id, Entity.RemovalReason.DISCARDED);
            FakePlayerEntity.instance = null;
            FakePlayerCommand.sendMessage("Despawned fake player.");
        } else {
            this.sendError("No fake player currently spawned.");
        }
    }
}

