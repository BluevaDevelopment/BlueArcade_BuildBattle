package net.blueva.arcade.modules.build_battle.support.heads;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.lang.reflect.Field;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class SkullUtil {

    private static final Pattern URL_PATTERN = Pattern.compile("\"url\"\\s*:\\s*\"([^\"]+)\"");

    private SkullUtil() {
    }

    public static ItemStack createSkull(String textureOrName, String displayName) {
        ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) skull.getItemMeta();
        if (meta == null) {
            return skull;
        }

        if (textureOrName != null && textureOrName.startsWith("eyJ")) {
            ItemStack result = applyBase64(skull, meta, textureOrName, displayName);
            if (result != null) return result;




            if (displayName != null) meta.setDisplayName(displayName);
            skull.setItemMeta(meta);
            return skull;
        }


        try {
            meta.setOwningPlayer(Bukkit.getOfflinePlayer(textureOrName));
        } catch (Exception ignored) {
        }
        if (displayName != null) {
            meta.setDisplayName(displayName);
        }
        skull.setItemMeta(meta);
        return skull;
    }

    private static ItemStack applyBase64(ItemStack skull, SkullMeta meta, String base64, String displayName) {

        ItemStack result = applyPaperProfile(skull, meta, base64, displayName);
        if (result != null) return result;


        result = applySpigotProfile(skull, meta, base64, displayName);
        if (result != null) return result;


        return applyLegacyProfile(skull, meta, base64, displayName);
    }





    private static ItemStack applyPaperProfile(ItemStack skull, SkullMeta meta, String base64, String displayName) {
        try {
            java.lang.reflect.Method createProfile = Bukkit.class.getMethod("createProfile", UUID.class, String.class);
            Object profile = createProfile.invoke(null, UUID.randomUUID(), "Head");


            Class<?> propertyClass = Class.forName("com.destroystokyo.paper.profile.ProfileProperty");
            Object property = propertyClass.getConstructor(String.class, String.class)
                    .newInstance("textures", base64);

            profile.getClass().getMethod("setProperty", propertyClass).invoke(profile, property);

            java.lang.reflect.Method setPlayerProfile = meta.getClass().getMethod("setPlayerProfile",
                    Class.forName("com.destroystokyo.paper.profile.PlayerProfile"));
            setPlayerProfile.invoke(meta, profile);

            if (displayName != null) meta.setDisplayName(displayName);
            skull.setItemMeta(meta);
            return skull;
        } catch (ClassNotFoundException | NoSuchMethodException e) {

            return null;
        } catch (Exception e) {
            return null;
        }
    }




    private static ItemStack applySpigotProfile(ItemStack skull, SkullMeta meta, String base64, String displayName) {
        String url = extractUrlFromBase64(base64);
        if (url == null) return null;

        try {
            URL skinUrl = new URL(url);
            java.lang.reflect.Method createProfile = Bukkit.class.getMethod("createProfile", UUID.class, String.class);
            Object profile = createProfile.invoke(null, UUID.randomUUID(), "Head");

            Object textures = profile.getClass().getMethod("getTextures").invoke(profile);
            textures.getClass().getMethod("setSkin", URL.class).invoke(textures, skinUrl);


            profile.getClass().getMethod("setTextures",
                    Class.forName("org.bukkit.profile.PlayerTextures")).invoke(profile, textures);

            meta.setOwnerProfile((org.bukkit.profile.PlayerProfile) profile);

            if (displayName != null) meta.setDisplayName(displayName);
            skull.setItemMeta(meta);
            return skull;
        } catch (ClassNotFoundException | NoSuchMethodException e) {
            return null;
        } catch (MalformedURLException e) {
            return null;
        } catch (Exception e) {
            return null;
        }
    }




    private static ItemStack applyLegacyProfile(ItemStack skull, SkullMeta meta, String base64, String displayName) {
        try {
            Object profile = createGameProfile(base64);
            if (profile == null) return null;

            Field profileField = meta.getClass().getDeclaredField("profile");
            profileField.setAccessible(true);
            profileField.set(meta, profile);

            if (displayName != null) meta.setDisplayName(displayName);
            skull.setItemMeta(meta);
            return skull;
        } catch (NoSuchFieldException e) {
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    private static String extractUrlFromBase64(String base64) {
        try {
            String json = new String(Base64.getDecoder().decode(base64), StandardCharsets.UTF_8);
            Matcher matcher = URL_PATTERN.matcher(json);
            if (matcher.find()) {
                return matcher.group(1);
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private static Object createGameProfile(String base64Texture) {
        try {
            Class<?> gameProfileClass = Class.forName("com.mojang.authlib.GameProfile");
            Class<?> propertyClass = Class.forName("com.mojang.authlib.properties.Property");
            UUID uuid = new UUID(base64Texture.hashCode(), base64Texture.hashCode());
            Object profile = gameProfileClass.getConstructor(UUID.class, String.class).newInstance(uuid, "Head");
            Object propertyMap = gameProfileClass.getMethod("getProperties").invoke(profile);
            Object property = propertyClass.getConstructor(String.class, String.class).newInstance("textures", base64Texture);
            propertyMap.getClass().getMethod("put", Object.class, Object.class).invoke(propertyMap, "textures", property);
            return profile;
        } catch (Exception e) {
            return null;
        }
    }
}
