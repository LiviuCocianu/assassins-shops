package io.github.idoomful.assassinscurrencycore.utils;

import io.github.bananapuncher714.nbteditor.NBTEditor;
import io.github.idoomful.assassinscurrencycore.DMain;
import io.github.idoomful.assassinscurrencycore.configuration.MessagesYML;
import io.github.idoomful.assassinscurrencycore.data.SQL.TransactionLog;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class CurrencyUtils {
    public enum ConvertWay {
        UP, DOWN
    }

    /**
     * Gets all the currency quantities from the bank of the player
     *
     * @param player The name of the targeted player
     * @return a map that contains all currencies and how much of any the player has in their bank
     */
    public static LinkedHashMap<String, Integer> getBank(String player) {
        AtomicReference<LinkedHashMap<String, Integer>> output = new AtomicReference<>();
        DMain.getInstance().getSQL().getBankInventory(player, output::set);
        return output.get();
    }

    /**
     * Overrides the amount of currecy the target has in their bank with this amount
     *
     * @param player The name of the targeted player
     * @param currency The currency ID
     * @param amount The amount to set
     */
    public static void setToBank(String player, String currency, int amount) {
        DMain.getInstance().getSQL().setToBank(player, currency, amount);
    }

    /**
     * Adds this amount of the specified currency to the target's bank
     *
     * @param player The name of the targeted player
     * @param currency The currency ID
     * @param amount The amount to add
     */
    public static void addToBank(String player, String currency, int amount) {
        DMain.getInstance().getSQL().addToBank(player, currency, amount);
    }

    /**
     * Subtracts this amount of the specified currency from the target's bank
     *
     * @param player The name of the targeted player
     * @param currency The currency ID
     * @param amount The amount to add
     */
    public static void subtractFromBank(String player, String currency, int amount) {
        DMain.getInstance().getSQL().subtractFromBank(player, currency, amount);
    }

    /**
     *
     * @param currency The currency to search for
     * @param amount The amount to withdraw
     * @param player The name of the target to withdraw from
     * @return whether the withdrawal was successful or not
     */
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

    /**
     * Searches for currency to convert in the specified sense in the inventory of the target.
     *
     * @param player The name of the target
     * @param way The sense of conversion. UP for conversions that take the least valuable currency
     *            in the inventory of the player and converts it to the next most valuable one, and
     *            DOWN for the opposite.
     */
    public static void convert(Player player, ConvertWay way) {
        AtomicBoolean reached = new AtomicBoolean(false);

        String currency = getFirstCurrencyType(player, way);

        LinkedHashMap<String, ConfigPair<Integer, String>> map = Economy.Worth.getWorthMap();
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

        space = getItemSpace(player.getInventory(), filter);

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

        // Rearrange currency leftovers scattered around in the inventory
        if(way == ConvertWay.UP) {
            List<ItemStack> arranged = new ArrayList<>();

            for(int i = 0; i < player.getInventory().getContents().length; i++) {
                ItemStack item = player.getInventory().getContents()[i];

                if(item == null) continue;
                if(NBTEditor.contains(item, "CurrencyId")) {
                    if(item.getAmount() < 64) {
                        arranged.add(item);
                        player.getInventory().setItem(i, null);
                    }
                }
            }

            arranged.forEach(player.getInventory()::addItem);
        }

        if(reached.get()) {
            if (way == ConvertWay.UP) player.sendMessage(MessagesYML.CONVERTED_UP.withPrefix(player));
            else player.sendMessage(MessagesYML.CONVERTED_DOWN.withPrefix(player));
        } else {
            player.sendMessage(MessagesYML.Errors.NO_CONVERTIBLES.withPrefix(player));
        }
    }

    /**
     * Withdraws every currency in the provided list from the target
     *
     * @param player The affected target
     * @param costs A list of ConfigPair that take an amount and a currency ID
     * @param message A toggle for whether the method should announce the target the withdrawal failed or not
     * @return a map that contains the withdrawn currency that is only empty when the withdrawal is successful
     */
    public static HashMap<String, Integer> withdrawCosts(Player player, List<ConfigPair<Integer, String>> costs, boolean message) {
        HashMap<String, Integer> withdrawnBackup = new HashMap<>();

        for(ConfigPair<Integer, String> pair : costs) {
            boolean result = CurrencyUtils.withdrawCurrency(pair.getValue(), pair.getKey(), player);

            if(result) {
                withdrawnBackup.put(pair.getValue(), pair.getKey());
            } else {
                withdrawnBackup.forEach((curr, amount) -> {
                    player.getInventory().addItem(Economy.Currency.getMarkedItem(curr, amount));
                });

                if(message) player.sendMessage(MessagesYML.Errors.TRANSACTION_ERROR.withPrefix(player));
                return new HashMap<>();
            }
        }

        return withdrawnBackup;
    }

    /**
     * Withdraws every currency in the provided list from the target and multiply each amount to the provided multiplier
     *
     * @param player The affected target
     * @param costs A list of ConfigPair that take an amount and a currency ID
     * @param mult A number that will be multiplied by the amount of each provided currency
     * @return whether the withdrawal was successful or not
     */
    public static boolean withdrawMultipliedCosts(Player player, List<ConfigPair<Integer, String>> costs, int mult) {
        HashMap<String, Integer> withdrawnBackup = new HashMap<>();

        for(int i = 0; i < mult; i++) {
            HashMap<String, Integer> withdrawn = CurrencyUtils.withdrawCosts(player, costs, false);

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

    /**
     * Searches through the inventory of the targeted player and checks if they have at least all the
     * provided currencies in the inventory.
     *
     * @param player The targeted player
     * @param currencies The currencies with their amounts to search for
     * @return whether all the provided currencies are found in the inventory of the target
     */
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

    /**
     * Returns the the amount of the provided ItemStack that can fit in the provided inventory
     *
     * @param inventory The inventory to search through
     * @param item An ItemStack that is used as a filter
     * @return the amount of the provided filter that can fit in the provided inventory
     */
    public static int getItemSpace(Inventory inventory, ItemStack item) {
        int count = 0;

        for(ItemStack is : inventory) {
            if(is == null) count += 64;
            else if(is.isSimilar(item)) count += 64 - is.getAmount();
        }

        return count;
    }

    /**
     * Returns the currency ID of the first most valuable or least valuable (depending on direction) currency
     * that can be found in the inventory of the target.
     *
     * @param player The target whose inventory will be checked
     * @param direction The value direction
     * @return the currency ID of the first most valuable or least valuable (depending on direction) currency
     * that can be found in the inventory of the target
     */
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

    /**
     * Returns whether there is enough of the provided currency to convert to the next most valuable currency
     *
     * @param player The target whose inventory is checked
     * @param currency The currency to search for
     * @return whether there is enough of the provided currency to convert to the next most valuable currency
     */
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

    /**
     * Returns the amount of currency found in the provided inventory
     *
     * @param currency The currency to search for
     * @param inventory The inventory to search in
     * @return the amount of currency found in the provided inventory
     */
    public static int getCurrencyAmount(String currency, Inventory inventory) {
        int amount = 0;

        for(int i = 0; i < inventory.getContents().length; i++) {
            ItemStack item = inventory.getContents()[i];

            if(item == null) continue;
            if(NBTEditor.contains(item, "CurrencyId")) {
                if(NBTEditor.getString(item, "CurrencyId").equalsIgnoreCase(currency))
                amount += item.getAmount();
            }
        }

        return amount;
    }

    /**
     * Sets the provided currencies in the provided location
     *
     * @param inv The inventory where the wallet is
     * @param slot The slot where the wallet is
     * @param map The map of currency IDs and their amounts to be set in the wallet
     */
    public static void setWalletCurrencies(Inventory inv, int slot, HashMap<String, Integer> map) {
        ItemStack wallet = inv.getItem(slot);
        int size = 0;

        for(Map.Entry<String, Integer> pair : map.entrySet()) {
            wallet = NBTEditor.set(wallet, Math.max(0, pair.getValue()), pair.getKey());
        }

        for (String id : Economy.Currency.getIDs()) if (NBTEditor.contains(wallet, id)) size++;

        if (Economy.Currency.getIDs().size() > size) {
            for (String id : Economy.Currency.getIDs()) {
                if (!NBTEditor.contains(wallet, id)) wallet = NBTEditor.set(wallet, 0, id);
            }
        }

        inv.setItem(slot, wallet);
        Utils.updateWalletLore(inv);
    }

    /**
     * Returns the currencies inside the provided wallet ItemStack
     *
     * @param item The wallet ItemStack
     * @return the currencies inside the provided wallet ItemStack
     */
    public static HashMap<String, Integer> getWalletCurrency(ItemStack item) {
        HashMap<String, Integer> output = new HashMap<>();

        Economy.Currency.getIDs().forEach(id -> output.put(id, 0));
        if(isWallet(item)) Economy.Currency.getIDs().forEach(id -> output.put(id, NBTEditor.getInt(item, id)));

        return output;
    }

    /**
     * Returns whether the provided item is a wallet or not
     *
     * @param item The wallet ItemStack
     * @return whether the provided item is a wallet or not
     */
    public static boolean isWallet(ItemStack item) {
        return NBTEditor.contains(item, "WalletId");
    }

    /**
     * Returns whether the provided wallet has any currency inside of it or not
     *
     * @param item The wallet ItemStack
     * @return whether the provided wallet has any currency inside of it or not
     */
    public static boolean isWalletEmpty(ItemStack item) {
        return getWalletCurrency(item).values().stream().noneMatch(amount -> amount > 0);
    }

    public static void createLogsFile() {
        String logsFileName = new SimpleDateFormat("dd-MM-yyyy").format(new Date());
        File logsFile = new File(
                DMain.getInstance().getDataFolder() + File.separator + "logs",
                logsFileName + ".txt"
        );

        if(!logsFile.exists()) {
            try {
                boolean result = logsFile.createNewFile();

                if(result) DMain.getInstance().getLogger().info("Created transaction log file for " + logsFileName);
                else DMain.getInstance().getLogger().warning("Couldn't create transaction log file for " + logsFileName);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void logTransaction(TransactionLog log) {
        String logsFileName = new SimpleDateFormat("dd-MM-yyyy").format(new Date());
        File logsFile = new File(
                DMain.getInstance().getDataFolder() + File.separator + "logs",
                logsFileName + ".txt"
        );

        createLogsFile();

        if(logsFile.exists()) {
            try {
                BufferedWriter bw = new BufferedWriter(new FileWriter(logsFile, true));

                String prefix = new SimpleDateFormat("[HH:mm:ss]: ").format(new Date());

                bw.write(prefix + log.logMessage());
                bw.newLine();
                bw.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
