package xyz.angames.astolfoclient.client.util;

import java.util.HashSet;
import java.util.Set;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@Environment(value=EnvType.CLIENT)
public class FriendManager {
    private static final Set<String> friends = new HashSet<String>();

    public static void addFriend(String name) {
        friends.add(name.toLowerCase());
    }

    public static void removeFriend(String name) {
        friends.remove(name.toLowerCase());
    }

    public static boolean isFriend(String name) {
        if (name == null) {
            return false;
        }
        return friends.contains(name.toLowerCase());
    }

    public static void clearFriends() {
        friends.clear();
    }

    public static Set<String> getFriends() {
        return friends;
    }
}

