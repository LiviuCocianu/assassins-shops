package io.github.idoomful.assassinseconomy.utils;

import io.github.bananapuncher714.nbteditor.NBTEditor;
import io.github.idoomful.assassinseconomy.DMain;
import io.github.idoomful.assassinseconomy.configuration.ConfigPair;
import io.github.idoomful.assassinseconomy.configuration.SettingsYML;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class CurrencyUtils {
    public static boolean withdrawCurrency(String currency, int amount, Player player) {
        ItemStack[] items = player.getInventory().getContents();

        for(int i = 0; i < items.length; i++) {
            if(items[i] == null) continue;

            ItemStack item = items[i];
            ItemStack configItem = NBTEditor.set(SettingsYML.Currencies.OPTIONS.getItem(currency, item.getAmount()), currency, "CurrencyId");

            if(item.isSimilar(configItem)) {
                if(item.getAmount() >= amount) {
                    item.setAmount(item.getAmount() - amount);
                    player.getInventory().setItem(i, item);
                    return true;
                }
            }
        }

        // Handle change
        List<String> ids = SettingsYML.Currencies.OPTIONS.getIDs();

        // Resume loop from the next currency to check if the player has any superior currency
        for(int j = ids.indexOf(currency) + 1; j < ids.size(); j++) {
            String resumedCurrency = ids.get(j);

            // Go through inventory
            for(int i = 0; i < items.length; i++) {
                if(items[i] == null) continue;

                ItemStack item = items[i];
                ItemStack configItem = NBTEditor.set(SettingsYML.Currencies.OPTIONS.getItem(resumedCurrency, item.getAmount()), resumedCurrency, "CurrencyId");

                // If the player has it in the inventory
                if(item.isSimilar(configItem)) {
                    item.setAmount(item.getAmount() - 1);
                    player.getInventory().setItem(i, item);

                    // Get worth of that currency
                    ConfigPair<Integer, String> worth = SettingsYML.CurrencyWorths.OPTIONS.getWorth(resumedCurrency);

                    // Index of the currency that will end up being used for thw withdrawal
                    // Starting from the biggest currency
                    int currIndex = ids.indexOf(worth.getValue());

                    // The currency that needs to be reached
                    String currIndexed = ids.get(currIndex);
                    int currWorthIndexed = SettingsYML.CurrencyWorths.OPTIONS.getWorth(ids.get(currIndex + 1)).getKey();

                    int previousIndex = 0;

                    // Keep track of the change that needs to be given in the process
                    List<ItemStack> changeList = new ArrayList<>();
                    changeList.add(NBTEditor.set(SettingsYML.Currencies.OPTIONS.getItem(currIndexed, currWorthIndexed), currIndexed, "CurrencyId"));

                    while(!currIndexed.equals(currency)) {
                        // Go down to the inferior of this currIndexed
                        currIndex -= 1;
                        currIndexed = ids.get(currIndex);
                        currWorthIndexed = SettingsYML.CurrencyWorths.OPTIONS.getWorth(ids.get(currIndex + 1)).getKey();

                        // Subtract one unit so change can be broken down
                        changeList.get(previousIndex).setAmount(changeList.get(previousIndex).getAmount() - 1);

                        // Add a stack of inferior currency
                        changeList.add(NBTEditor.set(SettingsYML.Currencies.OPTIONS.getItem(currIndexed, currWorthIndexed), currIndexed, "CurrencyId"));

                        previousIndex += 1;
                    }

                    // If the demanded currency is a full stack, don't give any amount of it
                    // This statement prevents the method from giving 1 of 'currency'
                    if(amount < currWorthIndexed) {
                        // Finally subtract the needed amount from the change breakdown
                        ItemStack equalCurrency = changeList.get(previousIndex);
                        equalCurrency.setAmount(equalCurrency.getAmount() - amount);
                        changeList.set(previousIndex, equalCurrency);
                    } else if(amount == currWorthIndexed) {
                        changeList.remove(previousIndex);
                    } else {
                        final int clone = currWorthIndexed;
                        Bukkit.getScheduler().scheduleSyncDelayedTask(DMain.getInstance(), () -> {
                            withdrawCurrency(currency, (amount - clone) + 1, player);
                        }, 2);
                    }

                    // Give the player the change
                    changeList.forEach(change -> player.getInventory().addItem(change));

                    return true;
                }
            }
        }

        return false;
    }
}
