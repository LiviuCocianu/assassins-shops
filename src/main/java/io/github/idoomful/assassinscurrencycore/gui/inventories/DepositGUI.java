package io.github.idoomful.assassinscurrencycore.gui.inventories;

import io.github.idoomful.assassinscurrencycore.configuration.SettingsYML;
import io.github.idoomful.assassinscurrencycore.gui.InventoryBuilder;
import io.github.idoomful.assassinscurrencycore.gui.MyGUI;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

public class DepositGUI implements MyGUI {
    private final Inventory inventory;

    public DepositGUI(Player player) {
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
