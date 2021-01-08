package io.github.idoomful.assassinseconomy.gui.inventories;

import io.github.idoomful.assassinseconomy.configuration.SettingsYML;
import io.github.idoomful.assassinseconomy.gui.InventoryBuilder;
import io.github.idoomful.assassinseconomy.gui.MyGUI;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public class BankGUI implements MyGUI, InventoryHolder {
    private final Inventory inventory;
    private final Player player;

    public BankGUI(Player player) {
        this.player = player;
        inventory = Bukkit.createInventory(this, SettingsYML.BankOptions.ROWS.getInt() * 9, SettingsYML.BankOptions.TITLE.getString(player));

        InventoryBuilder builder = new InventoryBuilder(inventory);
        builder.setConfigItemList(SettingsYML.BankOptions.ITEMS.getItemList(), player);
        builder.setConfigItemArrangement(SettingsYML.BankOptions.LAYOUT.getStringList(player));

        openInventory(player);
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    @Override
    public void openInventory(Player player) {
        player.openInventory(inventory);
    }
}
