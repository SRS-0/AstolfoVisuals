package dev.sxmurxy.mre.providers;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.stream.Collectors;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;

@Environment(value=EnvType.CLIENT)
public final class ResourceProvider {
    private static final Gson GSON = new Gson();

    public static Identifier getShaderIdentifier(String name) {
        return Identifier.fromNamespaceAndPath((String)"mre", (String)("core/" + name));
    }

    public static JsonObject toJson(Identifier identifier) {
        return JsonParser.parseString((String)ResourceProvider.toString(identifier)).getAsJsonObject();
    }

    public static <T> T fromJsonToInstance(Identifier identifier, Class<T> clazz) {
        return (T)GSON.fromJson(ResourceProvider.toString(identifier), clazz);
    }

    public static String toString(Identifier identifier) {
        return ResourceProvider.toString(identifier, "\n");
    }

    /*
     * Enabled aggressive exception aggregation
     */
    public static String toString(Identifier identifier, String delimiter) {
        block27: {
            Minecraft client = Minecraft.getInstance();
            if (client != null && client.getResourceManager() != null) {
                try (InputStream inputStream2 = client.getResourceManager().open(identifier);){
                    String string;
                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream2));){
                        string = reader.lines().collect(Collectors.joining(delimiter));
                    }
                    return string;
                }
                catch (Throwable inputStream2) {
                    // empty catch block
                }
            }
            String path = "/assets/" + identifier.getNamespace() + "/" + identifier.getPath();
            try (InputStream inputStream = ResourceProvider.class.getResourceAsStream(path);){
                String string;
                if (inputStream == null) break block27;
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));){
                    string = reader.lines().collect(Collectors.joining(delimiter));
                }
                return string;
            }
            catch (Throwable throwable) {
                // empty catch block
            }
        }
        throw new RuntimeException("Failed to load resource: " + String.valueOf(identifier));
    }
}

