package xyz.angames.astolfoclient.client.module.modules.misc;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.MutableText;
import net.minecraft.text.PlainTextContent;
import net.minecraft.text.Text;
import net.minecraft.text.TextContent;
import net.minecraft.text.TranslatableTextContent;
import xyz.angames.astolfoclient.client.AstolfoclientClient;
import xyz.angames.astolfoclient.client.module.Module;
import xyz.angames.astolfoclient.client.module.setting.BooleanSetting;

@Environment(value=EnvType.CLIENT)
public class PasswordHiderModule
extends Module {
    public static PasswordHiderModule INSTANCE;
    public final BooleanSetting hideInChat = new BooleanSetting("Hide In Chat", true);
    private static final Pattern CHAT_COMMAND_PATTERN;

    public PasswordHiderModule() {
        super("PasswordHider", "Hides passwords typed in chat commands using asterisks", Module.Category.MISC);
        INSTANCE = this;
        this.addSetting(this.hideInChat);
    }

    public static boolean isPasswordCommand(String text) {
        if (text == null || text.isEmpty()) {
            return false;
        }
        String trimmed = text.trim();
        if (!trimmed.startsWith("/")) {
            return false;
        }
        int firstSpace = trimmed.indexOf(32);
        if (firstSpace == -1) {
            return false;
        }
        String command = trimmed.substring(1, firstSpace).toLowerCase(Locale.ROOT);
        return PasswordHiderModule.isTargetCommand(command);
    }

    private static boolean isTargetCommand(String cmd) {
        return cmd.equals("l") || cmd.equals("login") || cmd.equals("reg") || cmd.equals("register") || cmd.equals("changepassword") || cmd.equals("cp") || cmd.equals("auth") || cmd.equals("pin") || cmd.equals("2fa");
    }

    public static String getMaskedText(String text) {
        if (!PasswordHiderModule.isPasswordCommand(text)) {
            return text;
        }
        int firstSpace = text.indexOf(32);
        if (firstSpace == -1) {
            return text;
        }
        String prefix = text.substring(0, firstSpace + 1);
        String password = text.substring(firstSpace + 1);
        StringBuilder sb = new StringBuilder(prefix);
        for (int i = 0; i < password.length(); ++i) {
            char c = password.charAt(i);
            if (c == ' ') {
                sb.append(' ');
                continue;
            }
            sb.append('*');
        }
        return sb.toString();
    }

    public static void setupChatField(TextFieldWidget chatField) {
        if (chatField == null) {
            return;
        }
        chatField.addFormatter((originalStr, firstCharacterIndex) -> {
            if (AstolfoclientClient.moduleManager == null) {
                return Text.literal((String)originalStr).asOrderedText();
            }
            Module mod = AstolfoclientClient.moduleManager.getModuleByName("PasswordHider");
            if (mod == null || !mod.isEnabled()) {
                return Text.literal((String)originalStr).asOrderedText();
            }
            String fullText = chatField.getText();
            if (PasswordHiderModule.isPasswordCommand(fullText)) {
                int firstSpace = fullText.indexOf(32);
                int passStart = firstSpace + 1;
                StringBuilder sb = new StringBuilder(originalStr.length());
                for (int j = 0; j < originalStr.length(); ++j) {
                    int globalIndex = firstCharacterIndex + j;
                    char c = originalStr.charAt(j);
                    if (globalIndex >= passStart && c != ' ') {
                        sb.append('*');
                        continue;
                    }
                    sb.append(c);
                }
                return Text.literal((String)sb.toString()).asOrderedText();
            }
            return Text.literal((String)originalStr).asOrderedText();
        });
    }

    public static void renderChatFieldOverlay(DrawContext context, TextFieldWidget chatField) {
    }

    public static Text getProtectedChat(Text message) {
        if (message == null) {
            return null;
        }
        if (AstolfoclientClient.moduleManager == null) {
            return message;
        }
        Module mod = AstolfoclientClient.moduleManager.getModuleByName("PasswordHider");
        if (mod == null || !mod.isEnabled()) {
            return message;
        }
        PasswordHiderModule ph = (PasswordHiderModule)mod;
        if (!ph.hideInChat.get()) {
            return message;
        }
        try {
            return PasswordHiderModule.protectChatTree(message);
        }
        catch (Exception e) {
            return message;
        }
    }

    private static Text protectChatTree(Text text) {
        TextContent content;
        if (text == null) {
            return null;
        }
        TextContent newContent = content = text.getContent();
        boolean contentChanged = false;
        if (content instanceof PlainTextContent.Literal) {
            PlainTextContent.Literal literal = (PlainTextContent.Literal)content;
            String str = literal.string();
            String masked = PasswordHiderModule.maskChatString(str);
            if (!masked.equals(str)) {
                newContent = Text.literal((String)masked).getContent();
                contentChanged = true;
            }
        } else if (content instanceof TranslatableTextContent) {
            TranslatableTextContent trans = (TranslatableTextContent)content;
            Object[] args = trans.getArgs();
            Object[] newArgs = new Object[args.length];
            boolean argsChanged = false;
            for (int i = 0; i < args.length; ++i) {
                Object arg = args[i];
                if (arg instanceof Text) {
                    Text argText = (Text)arg;
                    Text protectedArg = PasswordHiderModule.protectChatTree(argText);
                    newArgs[i] = protectedArg;
                    if (protectedArg == argText) continue;
                    argsChanged = true;
                    continue;
                }
                if (arg instanceof String) {
                    String argStr = (String)arg;
                    String masked = PasswordHiderModule.maskChatString(argStr);
                    newArgs[i] = masked;
                    if (masked.equals(argStr)) continue;
                    argsChanged = true;
                    continue;
                }
                newArgs[i] = arg;
            }
            if (argsChanged) {
                newContent = new TranslatableTextContent(trans.getKey(), trans.getFallback(), newArgs);
                contentChanged = true;
            }
        }
        List<Text> siblings = text.getSiblings();
        ArrayList<Text> newSiblings = new ArrayList<Text>(siblings.size());
        boolean siblingsChanged = false;
        for (Text sibling : siblings) {
            Text protectedSibling = PasswordHiderModule.protectChatTree(sibling);
            newSiblings.add(protectedSibling);
            if (protectedSibling == sibling) continue;
            siblingsChanged = true;
        }
        if (!contentChanged && !siblingsChanged) {
            return text;
        }
        MutableText result = MutableText.of((TextContent)newContent).setStyle(text.getStyle());
        for (Text sibling : newSiblings) {
            result.append(sibling);
        }
        return result;
    }

    public static String maskChatString(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }
        Matcher matcher = CHAT_COMMAND_PATTERN.matcher(input);
        if (!matcher.find()) {
            return input;
        }
        StringBuilder sb = new StringBuilder();
        matcher.reset();
        while (matcher.find()) {
            String fullMatch = matcher.group(0);
            int spaceIdx = fullMatch.indexOf(32);
            if (spaceIdx == -1) continue;
            String cmdPart = fullMatch.substring(0, spaceIdx + 1);
            String passPart = fullMatch.substring(spaceIdx + 1);
            String[] passWords = passPart.split("\\s+");
            StringBuilder maskedPass = new StringBuilder();
            for (int i = 0; i < passWords.length; ++i) {
                if (i > 0) {
                    maskedPass.append(" ");
                }
                maskedPass.append("****");
            }
            matcher.appendReplacement(sb, Matcher.quoteReplacement(cmdPart + String.valueOf(maskedPass)));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    static {
        CHAT_COMMAND_PATTERN = Pattern.compile("(?i)/(?:login|register|reg|changepassword|cp|auth|pin|2fa|l)\\s+([^\\s]+(?:\\s+[^\\s]+)*)");
    }
}

