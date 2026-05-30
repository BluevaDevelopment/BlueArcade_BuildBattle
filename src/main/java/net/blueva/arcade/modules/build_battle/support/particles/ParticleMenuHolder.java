package net.blueva.arcade.modules.build_battle.support.particles;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public class ParticleMenuHolder implements InventoryHolder {

    public enum Type {
        SELECT, REMOVE
    }

    private final Type type;

    public ParticleMenuHolder(Type type) {
        this.type = type;
    }

    public Type getType() {
        return type;
    }

    @Override
    public Inventory getInventory() {
        return null;
    }
}
