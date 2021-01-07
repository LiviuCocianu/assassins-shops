package io.github.idoomful.assassinseconomy.gui;

import io.github.idoomful.assassinseconomy.configuration.SettingsYML;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public class BankGUI implements MyGUI, InventoryHolder {
    private final Inventory inventory;
    private final Player player;

    public BankGUI(Player player) {
        this.player = player;
        inventory = Bukkit.createInventory(this, SettingsYML.Bank.ROWS.getInt() * 9, SettingsYML.Bank.TITLE.getString(player));

        InventoryBuilder builder = new InventoryBuilder(inventory);
        builder.setConfigItemList(SettingsYML.Bank.ITEMS.getItemList(), player);
        builder.setConfigItemArrangement(SettingsYML.Bank.LAYOUT.getStringList(player));

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
