package xyz.angames.astolfoclient.client.mixin;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.ImmutableStringReader;
import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.context.CommandContextBuilder;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.mojang.brigadier.tree.CommandNode;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ChatInputSuggestor;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.command.CommandSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import xyz.angames.astolfoclient.client.AstolfoclientClient;

@Environment(value=EnvType.CLIENT)
@Mixin(value={ChatInputSuggestor.class})
public abstract class ChatInputSuggestorMixin {
    @Shadow
    private TextFieldWidget textField;
    @Shadow
    private CompletableFuture<Suggestions> pendingSuggestions;
    @Shadow
    private ParseResults<CommandSource> parse;

    @Shadow
    public abstract void showCommandSuggestions();

    @Inject(method={"refresh"}, at={@At(value="HEAD")}, cancellable=true)
    public void onRefresh(CallbackInfo ci) {
        String text = this.textField.getText();
        if (text.startsWith("$")) {
            int cursor = this.textField.getCursor();
            int lastSpace = text.lastIndexOf(32, cursor - 1);
            int start = lastSpace == -1 ? 1 : lastSpace + 1;
            List<String> suggestionsList = AstolfoclientClient.commandManager.getSuggestions(text);
            SuggestionsBuilder builder = new SuggestionsBuilder(text, start);
            for (String s : suggestionsList) {
                builder.suggest(s);
            }
            this.pendingSuggestions = builder.buildFuture();
            MinecraftClient client = MinecraftClient.getInstance();
            if (client.player != null && client.player.networkHandler != null) {
                CommandDispatcher dummyDispatcher = new CommandDispatcher();
                StringReader reader = new StringReader(text);
                reader.setCursor(text.length());
                CommandContextBuilder contextBuilder = new CommandContextBuilder(dummyDispatcher, (Object)client.player.networkHandler.getCommandSource(), (CommandNode)dummyDispatcher.getRoot(), 0);
                this.parse = new ParseResults(contextBuilder, (ImmutableStringReader)reader, Collections.emptyMap());
            }
            this.showCommandSuggestions();
            ci.cancel();
        }
    }
}

