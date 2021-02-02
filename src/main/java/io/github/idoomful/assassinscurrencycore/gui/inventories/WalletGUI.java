package io.github.idoomful.assassinscurrencycore.gui.inventories;

import io.github.bananapuncher714.nbteditor.NBTEditor;
import io.github.idoomful.assassinscurrencycore.configuration.SettingsYML;
import io.github.idoomful.assassinscurrencycore.gui.MyGUI;
import io.github.idoomful.assassinscurrencycore.utils.Economy;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.LinkedHashMap;
import java.util.Map;

public class WalletGUI implements MyGUI {
    private final Inventory inventory;

    public WalletGUI(Player player, ItemStack wallet) {
        int rows = NBTEditor.getInt(wallet, "WalletRows");

        inventory = Bukkit.createInventory(this, rows * 9, SettingsYML.WalletOptions.TITLE.getString(player));

        LinkedHashMap<String, Integer> walletContent = new LinkedHashMap<>();
        Economy.Currency.getIDs().forEach(curr -> walletContent.put(curr, NBTEditor.getInt(wallet, curr)));

        for(Map.Entry<String, Integer> pair : walletContent.entrySet()) {
            int amount = pair.getValue();

            while(amount > 64) {
                inventory.addItem(Economy.Currency.getMarkedItem(pair.getKey(), 64));
                amount -= 64;
            }

            if(amount > 0) inventory.addItem(Economy.Currency.getMarkedItem(pair.getKey(), amount));
        }

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
