package net.blueva.arcade.modules.build_battle.support.banner;

import org.bukkit.DyeColor;
import org.bukkit.block.banner.Pattern;
import org.bukkit.block.banner.PatternType;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;

public final class BannerPatternUtil {

    private static final Class<?> PATTERN_TYPE_CLASS;
    private static final boolean IS_REGISTRY_API;




    private static final List<String> KNOWN_PATTERN_NAMES = List.of(
            "BASE", "BORDER", "BRICKS", "CIRCLE", "CIRCLE_MIDDLE", "CREEPER", "CROSS",
            "CURLY_BORDER", "DIAGONAL_LEFT", "DIAGONAL_RIGHT", "DIAGONAL_LEFT_MIRROR",
            "DIAGONAL_RIGHT_MIRROR", "DIAGONAL_UP_LEFT", "DIAGONAL_UP_RIGHT", "FLOW",
            "FLOWER", "GLOBE", "GRADIENT", "GRADIENT_UP", "GUSTER", "HALF_HORIZONTAL",
            "HALF_HORIZONTAL_BOTTOM", "HALF_HORIZONTAL_MIRROR", "HALF_VERTICAL",
            "HALF_VERTICAL_MIRROR", "HALF_VERTICAL_RIGHT", "MOJANG", "PIGLIN", "RHOMBUS",
            "RHOMBUS_MIDDLE", "SKULL", "SMALL_STRIPES", "SQUARE_BOTTOM_LEFT",
            "SQUARE_BOTTOM_RIGHT", "SQUARE_TOP_LEFT", "SQUARE_TOP_RIGHT", "STRAIGHT_CROSS",
            "STRIPE_BOTTOM", "STRIPE_CENTER", "STRIPE_DOWNLEFT", "STRIPE_DOWNRIGHT",
            "STRIPE_LEFT", "STRIPE_MIDDLE", "STRIPE_RIGHT", "STRIPE_SMALL", "STRIPE_TOP",
            "TRIANGLE_BOTTOM", "TRIANGLE_TOP", "TRIANGLES_BOTTOM", "TRIANGLES_TOP"
    );

    static {
        Class<?> ptClass = null;
        boolean isRegistry = false;
        try {
            ptClass = Class.forName("org.bukkit.block.banner.PatternType");

            ptClass.getMethod("valueOf", String.class);

            ptClass.getField("BASE");
        } catch (NoSuchFieldException e) {

            isRegistry = true;
        } catch (Exception e) {

            isRegistry = true;
        }
        PATTERN_TYPE_CLASS = ptClass;
        IS_REGISTRY_API = isRegistry;
    }

    private BannerPatternUtil() {
    }

    public static Object resolvePatternType(String name) {
        if (PATTERN_TYPE_CLASS == null) {
            return null;
        }
        String normalized = name.toUpperCase(java.util.Locale.ROOT);
        try {
            if (!IS_REGISTRY_API) {

                return PATTERN_TYPE_CLASS.getMethod("valueOf", String.class).invoke(null, normalized);
            } else {

                Class<?> registryClass = Class.forName("org.bukkit.Registry");
                Object bannerRegistry = registryClass.getField("BANNER_PATTERN").get(null);
                Class<?> keyClass = Class.forName("org.bukkit.NamespacedKey");
                Object key = keyClass.getMethod("minecraft", String.class).invoke(null, normalized.toLowerCase(java.util.Locale.ROOT));
                return bannerRegistry.getClass().getMethod("get", keyClass).invoke(bannerRegistry, key);
            }
        } catch (Exception e) {
            return null;
        }
    }

    public static Pattern createPattern(DyeColor color, String patternTypeName) {
        Object patternType = resolvePatternType(patternTypeName);
        if (patternType == null) {
            return null;
        }
        try {
            Constructor<?> constructor = Pattern.class.getConstructor(DyeColor.class, PatternType.class);
            return (Pattern) constructor.newInstance(color, patternType);
        } catch (Exception e) {
            return null;
        }
    }

    public static List<String> getAvailablePatternNames() {
        List<String> names = new ArrayList<>();




        for (String name : KNOWN_PATTERN_NAMES) {
            if (resolvePatternType(name) != null) {
                names.add(name);
            }
        }


        if (names.isEmpty()) {
            tryNativeApiNames(names);
        }

        return names;
    }


    public static List<String> getAvailableLayerPatternNames() {
        List<String> names = new ArrayList<>();
        for (String name : getAvailablePatternNames()) {
            if (!"BASE".equalsIgnoreCase(name)) {
                names.add(name);
            }
        }
        return names;
    }

    private static void tryNativeApiNames(List<String> names) {
        if (PATTERN_TYPE_CLASS == null) {
            return;
        }
        try {
            if (!IS_REGISTRY_API) {

                Object[] values = (Object[]) PATTERN_TYPE_CLASS.getMethod("values").invoke(null);
                for (Object v : values) {
                    names.add(((Enum<?>) v).name());
                }
            } else {

                Class<?> registryClass = Class.forName("org.bukkit.Registry");
                Object bannerRegistry = registryClass.getField("BANNER_PATTERN").get(null);
                if (bannerRegistry instanceof Iterable<?> iterable) {
                    for (Object patternType : iterable) {
                        try {
                            Object namespacedKey = patternType.getClass().getMethod("getKey").invoke(patternType);
                            String keyStr = (String) namespacedKey.getClass().getMethod("getKey").invoke(namespacedKey);
                            names.add(keyStr.toUpperCase(java.util.Locale.ROOT));
                        } catch (Exception ignored) {
                        }
                    }
                }
            }
        } catch (Exception ignored) {
        }
    }
}
