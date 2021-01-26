package io.github.idoomful.assassinseconomy.utils;

import io.github.bananapuncher714.nbteditor.NBTEditor;
import io.github.idoomful.assassinseconomy.DMain;
import io.github.idoomful.assassinseconomy.configuration.ConfigPair;
import io.github.idoomful.assassinseconomy.configuration.MessagesYML;
import io.github.idoomful.assassinseconomy.configuration.SettingsYML;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class CurrencyUtils {
    public enum ConvertWay {
        UP, DOWN
    }

    public static boolean withdrawCurrency(String currency, int amount, Player player) {
        ItemStack[] items = player.getInventory().getContents();

        for(int i = 0; i < items.length; i++) {
            if(items[i] == null) continue;

            ItemStack item = items[i];

            if(NBTEditor.contains(item, "CurrencyId")) {
                if(NBTEditor.getString(item,  "CurrencyId").equals(currency)) {
                    if(item.getAmount() >= amount) {
                        item.setAmount(item.getAmount() - amount);
                        player.getInventory().setItem(i, item);
                        return true;
                    }
                }
            }
        }

        // Handle change
        List<String> ids = Economy.Currency.getIDs();

        // Resume loop from the next currency to check if the player has any superior currency
        for(int j = ids.indexOf(currency) + 1; j < ids.size(); j++) {
            String resumedCurrency = ids.get(j);

            // Go through inventory
            for(int i = 0; i < items.length; i++) {
                if(items[i] == null) continue;

                ItemStack item = items[i];

                if(NBTEditor.contains(item, "CurrencyId")) {
                    // If the player has it in the inventory
                    if (NBTEditor.getString(item, "CurrencyId").equals(resumedCurrency)) {

                        item.setAmount(item.getAmount() - 1);
                        player.getInventory().setItem(i, item);

                        // Get worth of that currency
                        ConfigPair<Integer, String> worth = Economy.Worth.getWorth(resumedCurrency);

                        // Index of the currency that will end up being used for thw withdrawal
                        // Starting from the biggest currency
                        int currIndex = ids.indexOf(worth.getValue());

                        // The currency that needs to be reached
                        String currIndexed = ids.get(currIndex);
                        int currWorthIndexed = Economy.Worth.getWorth(ids.get(currIndex + 1)).getKey();

                        int previousIndex = 0;

                        // Keep track of the change that needs to be given in the process
                        List<ItemStack> changeList = new ArrayList<>();
                        changeList.add(Economy.Currency.getMarkedItem(currIndexed, currWorthIndexed));

                        while (!currIndexed.equals(currency)) {
                            // Go down to the inferior of this currIndexed
                            currIndex -= 1;
                            currIndexed = ids.get(currIndex);
                            currWorthIndexed = Economy.Worth.getWorth(ids.get(currIndex + 1)).getKey();

                            // Subtract one unit so change can be broken down
                            changeList.get(previousIndex).setAmount(changeList.get(previousIndex).getAmount() - 1);

                            // Add a stack of inferior currency
                            changeList.add(Economy.Currency.getMarkedItem(currIndexed, currWorthIndexed));

                            previousIndex += 1;
                        }

                        // If the demanded currency is a full stack, don't give any amount of it
                        // This statement prevents the method from giving 1 of 'currency'
                        if (amount < currWorthIndexed) {
                            // Finally subtract the needed amount from the change breakdown
                            ItemStack equalCurrency = changeList.get(previousIndex);
                            equalCurrency.setAmount(equalCurrency.getAmount() - amount);
                            changeList.set(previousIndex, equalCurrency);
                        } else if (amount == currWorthIndexed) {
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
        }

        return false;
    }

    public static void convert(Player player, ConvertWay way) {
        AtomicBoolean reached = new AtomicBoolean(false);

        String currency = getFirstCurrencyType(player, way);

        HashMap<String, ConfigPair<Integer, String>> map = Economy.Worth.getWorthMap();
        int space;

        while(
                (map.get(currency) == null && way == ConvertWay.DOWN)
                || (!hasEnoughToConvert(player, currency) && way == ConvertWay.UP)
        ) {
            int index = 1;
            List<String> ids = Economy.Currency.getIDs();
            if((ids.indexOf(currency) + index) >= ids.size()) break;

            String clone = ids.get(ids.indexOf(currency) + index);

            while (clone == null) {
                index += 1;
                clone = ids.get(ids.indexOf(currency) + index);
            }

            currency = clone;
        }

        ItemStack filter;

        if(!map.containsKey(currency)) {
            filter = Economy.Currency.getMarkedItem(currency, 1);
        } else {
            filter = Economy.Currency.getMarkedItem(map.get(currency).getValue(), 1);
        }

        space = getItemSpace(player, filter);

        int index = 0;
        int amountAcumulated = 0;
        List<Integer> currencyStacks = new ArrayList<>();
        String inferiorDown = null;

        for(ItemStack item : player.getInventory().getContents()) {
            if (item == null) {
                index++;
                continue;
            }

            if (NBTEditor.contains(item, "CurrencyId")) {
                if (NBTEditor.getString(item, "CurrencyId").equals(currency)) {
                    for (Map.Entry<String, ConfigPair<Integer, String>> entry : map.entrySet()) {
                        String inferior = entry.getValue().getValue();
                        int amountToSearch = entry.getValue().getKey();

                        if (way == ConvertWay.UP) {
                            // Check if there is currency to convert into a superior currency
                            if (inferior.equals(currency)) {
                                while (item.getAmount() >= amountToSearch) {
                                    item.setAmount(item.getAmount() - amountToSearch);
                                    player.getInventory().setItem(index, item);
                                    player.getInventory().addItem(Economy.Currency.getMarkedItem(entry.getKey(), 1));
                                    reached.set(true);
                                }
                            }
                        } else if (way == ConvertWay.DOWN) {
                            // Check if there is currency to convert into a inferior currency
                            if (entry.getKey().equals(currency)) {
                                inferiorDown = inferior;
                                amountAcumulated += item.getAmount() * amountToSearch;
                                currencyStacks.add(index);
                            }
                        }
                    }
                }
            }

            index++;
        }

        if(way == ConvertWay.DOWN && amountAcumulated > 0) {
            space += currencyStacks.size() * 64;

            for (int itemIndex : currencyStacks) {
                ItemStack item = player.getInventory().getItem(itemIndex);

                if (amountAcumulated > space) {
                    player.sendMessage(MessagesYML.Errors.NOT_ENOUGH_SPACE.withPrefix(player));
                    return;
                }

                item.setAmount(0);
                player.getInventory().setItem(itemIndex, item);
            }

            player.getInventory().addItem(Economy.Currency.getMarkedItem(inferiorDown, amountAcumulated));
            reached.set(true);
        }


        if(reached.get()) {
            if (way == ConvertWay.UP) player.sendMessage(MessagesYML.CONVERTED_UP.withPrefix(player));
            else player.sendMessage(MessagesYML.CONVERTED_DOWN.withPrefix(player));
        } else {
            player.sendMessage(MessagesYML.Errors.NO_CONVERTIBLES.withPrefix(player));
        }
    }

    public static HashMap<String, Integer> withdrawCosts(Player player, List<ConfigPair<Integer, String>> costs) {
        HashMap<String, Integer> withdrawnBackup = new HashMap<>();

        for(ConfigPair<Integer, String> pair : costs) {
            boolean result = CurrencyUtils.withdrawCurrency(pair.getValue(), pair.getKey(), player);

            if(result) {
                withdrawnBackup.put(pair.getValue(), pair.getKey());
            } else {
                withdrawnBackup.forEach((curr, amount) -> {
                    player.getInventory().addItem(Economy.Currency.getMarkedItem(curr, amount));
                });

                player.sendMessage(MessagesYML.Errors.TRANSACTION_ERROR.withPrefix(player));
                return new HashMap<>();
            }
        }

        return withdrawnBackup;
    }

    public static boolean withdrawMultipliedCosts(Player player, List<ConfigPair<Integer, String>> costs, int mult) {
        HashMap<String, Integer> withdrawnBackup = new HashMap<>();

        for(int i = 0; i < mult; i++) {
            HashMap<String, Integer> withdrawn = CurrencyUtils.withdrawCosts(player, costs);

            if(withdrawn.isEmpty()) {
                for(Map.Entry<String, Integer> pair : withdrawnBackup.entrySet()) {
                    player.getInventory().addItem(Economy.Currency.getMarkedItem(pair.getKey(), pair.getValue()));
                }

                player.sendMessage(MessagesYML.Errors.TRANSACTION_ERROR.withPrefix(player));
                return false;
            }

            withdrawn.forEach((curr, amount) -> {
                if(withdrawnBackup.containsKey(curr))
                    withdrawnBackup.put(curr, withdrawnBackup.get(curr) + amount);
                else withdrawnBackup.put(curr, amount);
            });
        }

        return true;
    }

    public static boolean hasAllCurrencies(Player player, List<ConfigPair<Integer, String>> currencies) {
        HashMap<String, Integer> playerCurr = new HashMap<>();

        for(ItemStack item : player.getInventory().getContents()) {
            if (item == null) continue;

            if(NBTEditor.contains(item, "CurrencyId")) {
                String curr = NBTEditor.getString(item, "CurrencyId");

                if(!playerCurr.containsKey(curr)) playerCurr.put(curr, 0);
                playerCurr.put(curr, playerCurr.get(curr) + item.getAmount());
            }
        }

        for(ConfigPair<Integer, String> pair : currencies) {
            if(!playerCurr.containsKey(pair.getValue())) return false;
            if(playerCurr.get(pair.getValue()) < pair.getKey()) return false;
        }

        return true;
    }

    private static int getItemSpace(Player player, ItemStack item) {
        int count = 0;

        for(ItemStack is : player.getInventory().getContents()) {
            if(is == null) count += 64;
            else if(is.isSimilar(item)) {
                count += (64 - is.getAmount()) == 0 ? 64 : 64 - is.getAmount();
            }
        }

        return count;
    }

    private static String getFirstCurrencyType(Player player, ConvertWay direction) {
        String output = "";

        List<String> currencyList =  Economy.Currency.getIDs();
        if(direction == ConvertWay.DOWN) Collections.reverse(currencyList);

        for(String curr : currencyList) {
            for(ItemStack item : player.getInventory().getContents()) {
                if(item == null) continue;
                if(NBTEditor.contains(item, "CurrencyId")) {
                    if(NBTEditor.getString(item, "CurrencyId").equals(curr))
                        return curr;
                }
            }
        }

        return output;
    }

    private static boolean hasEnoughToConvert(Player player, String currency) {
        int total = 0;

        for(ItemStack item : player.getInventory().getContents()) {
            if (item == null) continue;
            if(NBTEditor.contains(item, "CurrencyId")) {
                if(NBTEditor.getString(item, "CurrencyId").equals(currency))
                    total += item.getAmount();
            }
        }

        int currIndex = Economy.Worth.getIDs().indexOf(currency) + 1;

        if(currIndex >= Economy.Worth.getIDs().size()) return false;

        String above = Economy.Worth.getIDs().get(currIndex);

        return total >= Objects.requireNonNull(Economy.Worth.getWorth(above)).getKey();
    }
}
