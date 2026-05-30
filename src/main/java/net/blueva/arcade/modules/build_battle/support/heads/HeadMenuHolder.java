package net.blueva.arcade.modules.build_battle.support.heads;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public class HeadMenuHolder implements InventoryHolder {

    public enum Type {
        CATEGORIES, HEADS, SEARCH
    }

    private final Type type;
    private final String categoryId;

    public HeadMenuHolder(Type type) {
        this(type, null);
    }

    public HeadMenuHolder(Type type, String categoryId) {
        this.type = type;
        this.categoryId = categoryId;
    }

    public Type getType() {
        return type;
    }

    public String getCategoryId() {
        return categoryId;
    }

    @Override
    public Inventory getInventory() {
        return null;
    }
}
