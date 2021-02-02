package io.github.idoomful.assassinscurrencycore.gui;

import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public interface MyGUI extends InventoryHolder {
    Inventory getInventory();
    void openInventory(Player player);
}
