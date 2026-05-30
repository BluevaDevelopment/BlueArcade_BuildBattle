package net.blueva.arcade.modules.build_battle.support.banner;

import org.bukkit.DyeColor;
import org.bukkit.Material;
import org.bukkit.block.banner.Pattern;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BannerMeta;

import java.util.ArrayList;
import java.util.List;

public class BannerBuilderState {

    private DyeColor baseColor;
    private final List<PatternLayer> layers = new ArrayList<>();

    public void setBaseColor(DyeColor color) {
        this.baseColor = color;
        this.layers.clear();
    }

    public void addPattern(Pattern pattern, String patternName) {
        this.layers.add(new PatternLayer(pattern, patternName));
    }

    public void replaceLastPattern(Pattern pattern, String patternName) {
        if (!layers.isEmpty()) {
            layers.set(layers.size() - 1, new PatternLayer(pattern, patternName));
        }
    }

    public Pattern getLastPattern() {
        if (layers.isEmpty()) {
            return null;
        }
        return layers.get(layers.size() - 1).pattern();
    }

    public String getLastPatternName() {
        if (layers.isEmpty()) {
            return "BASE";
        }
        return layers.get(layers.size() - 1).patternName();
    }

    public DyeColor getBaseColor() {
        return baseColor;
    }

    public List<Pattern> getPatterns() {
        List<Pattern> patterns = new ArrayList<>();
        for (PatternLayer layer : layers) {
            patterns.add(layer.pattern());
        }
        return patterns;
    }

    public ItemStack buildBanner() {
        Material material = baseColor != null ? materialForColor(baseColor) : Material.WHITE_BANNER;
        ItemStack banner = new ItemStack(material);
        BannerMeta meta = (BannerMeta) banner.getItemMeta();
        if (meta != null) {
            meta.setPatterns(getPatterns());
            banner.setItemMeta(meta);
        }
        return banner;
    }

    private static Material materialForColor(DyeColor color) {
        try {
            return Material.valueOf(color.name() + "_BANNER");
        } catch (IllegalArgumentException e) {
            return Material.WHITE_BANNER;
        }
    }

    private record PatternLayer(Pattern pattern, String patternName) {
    }
}
