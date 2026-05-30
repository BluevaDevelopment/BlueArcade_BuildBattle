package net.blueva.arcade.modules.build_battle.support.banner;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
public class BannerMenuHolder implements InventoryHolder {

    public enum Stage {
        BASE, LAYER, LAYER_COLOR
    }

    private final BannerBuilderState state;
    private final Stage stage;

    public BannerMenuHolder(BannerBuilderState state, Stage stage) {
        this.state = state;
        this.stage = stage;
    }

    public BannerBuilderState getState() {
        return state;
    }

    public Stage getStage() {
        return stage;
    }

    @Override
    public Inventory getInventory() {
        return null;
    }
}
