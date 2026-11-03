package xyz.angames.astolfoclient.client.module.modules.misc;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.MutableText;
import net.minecraft.text.PlainTextContent;
import net.minecraft.text.Text;
import net.minecraft.text.TextContent;
import net.minecraft.text.TranslatableTextContent;
import xyz.angames.astolfoclient.client.AstolfoclientClient;
import xyz.angames.astolfoclient.client.module.Module;
import xyz.angames.astolfoclient.client.util.FriendManager;

@Environment(value=EnvType.CLIENT)
public class NameProtectModule
extends Module {
    public static final String REPLACEMENT = "astolfoclient.top";
    public static final String FRIEND_REPLACEMENT = "astolfoclient.top";

    public NameProtectModule() {
        super("NameProtect", "Replaces your name and friends' names", Module.Category.MISC);
    }

    public static String getProtectedName(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        if (AstolfoclientClient.moduleManager == null) {
            return text;
        }
        Module module = AstolfoclientClient.moduleManager.getModuleByName("NameProtect");
        if (module == null || !module.isEnabled()) {
            return text;
        }
        try {
            return NameProtectModule.replaceTargets(text);
        }
        catch (Exception e) {
            return text;
        }
    }

    public static Text getProtectedText(Text original) {
        if (original == null) {
            return null;
        }
        if (AstolfoclientClient.moduleManager == null) {
            return original;
        }
        Module module = AstolfoclientClient.moduleManager.getModuleByName("NameProtect");
        if (module == null || !module.isEnabled()) {
            return original;
        }
        try {
            return NameProtectModule.protectTree(original);
        }
        catch (Exception e) {
            return original;
        }
    }

    private static Text protectTree(Text text) {
        TextContent content;
        if (text == null) {
            return null;
        }
        TextContent newContent = content = text.getContent();
        boolean contentChanged = false;
        if (content instanceof PlainTextContent.Literal) {
            PlainTextContent.Literal literal = (PlainTextContent.Literal)content;
            String str = literal.string();
            String replaced = NameProtectModule.replaceTargets(str);
            if (!replaced.equals(str)) {
                newContent = Text.literal((String)replaced).getContent();
                contentChanged = true;
            }
        } else if (content instanceof TranslatableTextContent) {
            TranslatableTextContent translatableContent = (TranslatableTextContent)content;
            Object[] args = translatableContent.getArgs();
            Object[] newArgs = new Object[args.length];
            boolean argsChanged = false;
            for (int i = 0; i < args.length; ++i) {
                Object protectedArg;
                Object arg = args[i];
                if (arg instanceof Text) {
                    Text argText = (Text)arg;
                    newArgs[i] = protectedArg = NameProtectModule.protectTree(argText);
                    if (protectedArg == argText) continue;
                    argsChanged = true;
                    continue;
                }
                if (arg instanceof String) {
                    String argStr = (String)arg;
                    newArgs[i] = protectedArg = NameProtectModule.replaceTargets(argStr);
                    if (((String)protectedArg).equals(argStr)) continue;
                    argsChanged = true;
                    continue;
                }
                newArgs[i] = arg;
            }
            if (argsChanged) {
                newContent = new TranslatableTextContent(translatableContent.getKey(), translatableContent.getFallback(), newArgs);
                contentChanged = true;
            }
        }
        List<Text> siblings = text.getSiblings();
        ArrayList<Text> newSiblings = new ArrayList<Text>(siblings.size());
        boolean siblingsChanged = false;
        for (Text sibling : siblings) {
            Text protectedSibling = NameProtectModule.protectTree(sibling);
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

    private static List<TargetEntry> getActiveTargets() {
        String myName;
        ArrayList<TargetEntry> targets = new ArrayList<TargetEntry>();
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.getSession() != null && client.getSession().getUsername() != null && !(myName = client.getSession().getUsername().trim()).isEmpty() && !myName.equalsIgnoreCase("astolfoclient.top")) {
            targets.add(new TargetEntry(myName, "astolfoclient.top"));
        }
        for (String friend : FriendManager.getFriends()) {
            String f;
            if (friend == null || (f = friend.trim()).isEmpty() || f.equalsIgnoreCase("astolfoclient.top")) continue;
            boolean duplicate = false;
            for (TargetEntry existing : targets) {
                if (!existing.target.equalsIgnoreCase(f)) continue;
                duplicate = true;
                break;
            }
            if (duplicate) continue;
            targets.add(new TargetEntry(f, "astolfoclient.top"));
        }
        targets.sort((a, b) -> Integer.compare(b.target.length(), a.target.length()));
        return targets;
    }

    private static void findProtectedRanges(String text, String token, List<int[]> ranges) {
        if (token == null || token.isEmpty()) {
            return;
        }
        String lowerText = text.toLowerCase(Locale.ROOT);
        String lowerToken = token.toLowerCase(Locale.ROOT);
        int pos = 0;
        while ((pos = lowerText.indexOf(lowerToken, pos)) != -1) {
            ranges.add(new int[]{pos, pos + token.length()});
            pos += token.length();
        }
    }

    private static boolean overlapsProtected(int start, int end, List<int[]> protectedRanges) {
        for (int[] r : protectedRanges) {
            if (start >= r[1] || end <= r[0]) continue;
            return true;
        }
        return false;
    }

    private static boolean isWordChar(char c) {
        return c >= 'a' && c <= 'z' || c >= 'A' && c <= 'Z' || c >= '0' && c <= '9' || c == '_';
    }

    private static boolean isBoundaryBefore(String s, int index) {
        if (index <= 0) {
            return true;
        }
        char prev = s.charAt(index - 1);
        if (!NameProtectModule.isWordChar(prev)) {
            return true;
        }
        return index >= 2 && s.charAt(index - 2) == '\u00a7';
    }

    private static boolean isBoundaryAfter(String s, int index) {
        if (index >= s.length()) {
            return true;
        }
        char next = s.charAt(index);
        if (next == '\u00a7') {
            return true;
        }
        return !NameProtectModule.isWordChar(next);
    }

    public static String replaceTargets(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        List<TargetEntry> targets = NameProtectModule.getActiveTargets();
        if (targets.isEmpty()) {
            return text;
        }
        ArrayList<int[]> protectedRanges = new ArrayList<int[]>();
        NameProtectModule.findProtectedRanges(text, "astolfoclient.top", protectedRanges);
        if (!"astolfoclient.top".equalsIgnoreCase("astolfoclient.top")) {
            NameProtectModule.findProtectedRanges(text, "astolfoclient.top", protectedRanges);
        }
        String lowerText = text.toLowerCase(Locale.ROOT);
        ArrayList<FoundMatch> matches = new ArrayList<FoundMatch>();
        int searchPos = 0;
        while (searchPos < text.length()) {
            int earliestStart = -1;
            int earliestEnd = -1;
            TargetEntry bestTarget = null;
            for (TargetEntry entry : targets) {
                boolean validAfter;
                int idx = lowerText.indexOf(entry.lowerTarget, searchPos);
                if (idx == -1) continue;
                int end = idx + entry.target.length();
                boolean validBefore = !NameProtectModule.isWordChar(entry.target.charAt(0)) || NameProtectModule.isBoundaryBefore(text, idx);
                boolean bl = validAfter = !NameProtectModule.isWordChar(entry.target.charAt(entry.target.length() - 1)) || NameProtectModule.isBoundaryAfter(text, end);
                if (!validBefore || !validAfter || NameProtectModule.overlapsProtected(idx, end, protectedRanges) || earliestStart != -1 && idx >= earliestStart) continue;
                earliestStart = idx;
                earliestEnd = end;
                bestTarget = entry;
            }
            if (earliestStart == -1 || bestTarget == null) break;
            matches.add(new FoundMatch(earliestStart, earliestEnd, bestTarget.replacement));
            searchPos = earliestEnd;
        }
        if (matches.isEmpty()) {
            return text;
        }
        StringBuilder sb = new StringBuilder(text.length() + matches.size() * 16);
        int lastPos = 0;
        for (FoundMatch m : matches) {
            sb.append(text, lastPos, m.start);
            sb.append(m.replacement);
            lastPos = m.end;
        }
        sb.append(text, lastPos, text.length());
        return sb.toString();
    }

    @Environment(value=EnvType.CLIENT)
    private static class TargetEntry {
        final String target;
        final String replacement;
        final String lowerTarget;

        TargetEntry(String target, String replacement) {
            this.target = target;
            this.replacement = replacement;
            this.lowerTarget = target.toLowerCase(Locale.ROOT);
        }
    }

    @Environment(value=EnvType.CLIENT)
    private static class FoundMatch {
        final int start;
        final int end;
        final String replacement;

        FoundMatch(int start, int end, String replacement) {
            this.start = start;
            this.end = end;
            this.replacement = replacement;
        }
    }
}

