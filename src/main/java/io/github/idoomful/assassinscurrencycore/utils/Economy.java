package io.github.idoomful.assassinscurrencycore.utils;

import io.github.bananapuncher714.nbteditor.NBTEditor;
import io.github.idoomful.assassinscurrencycore.DMain;
import io.github.idoomful.assassinscurrencycore.configuration.MessagesYML;
import io.github.idoomful.assassinscurrencycore.gui.ItemBuilder;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

public class Economy {
    private static final FileConfiguration settings = DMain.getInstance().getConfigs().getFile("settings");

    public static class Currency {
        /**
         * Returns a list of all currency IDs.
         *
         * @return a list of all the available currency IDs in the config file
         */
        public static List<String> getIDs() {
            return new ArrayList<>(settings.getConfigurationSection("currencies").getKeys(false));
        }

        /**
         * Checks if the given ID is a valid currency ID.
         *
         * @param id The currency ID to check against
         * @return true, if this currency ID exists in the config file, false otherwise
         */
        public static boolean hasID(String id) {
            return getIDs().contains(id);
        }

        /**
         * Creates an ItemStack that represents the currency with the given ID.
         * This method doesn't return a valid currency item since it's not marked.
         * Use Economy#Currency#getMarkedItem(String id, int amount) for a valid item.
         *
         * @param id The ID of the currency
         * @param amount The amount that will be in the returned ItemStack
         * @return an ItemStack that will represent the currency with this ID
         */
        public static ItemStack getItem(String id, int amount) {
            if(hasID(id)) {
                ItemStack output = ItemBuilder.build(settings.getString(  "currencies." + id));
                output.setAmount(amount);
                return output;
            }

            ItemStack def = new ItemStack(Material.STONE);
            ItemMeta im = def.getItemMeta();
            im.setDisplayName(MessagesYML.Errors.ITEM_INVALID_CURRENCY.color(null));
            def.setItemMeta(im);
            return def;
        }

        /**
         * Creates an ItemStack that represents the currency with the given ID.
         * This item marks the ItemStack object with a NBT tag that will validate
         * it as a fully fledged currency.
         *
         * @param id The ID of the currency
         * @param amount The amount that will be in the returned ItemStack
         * @return an ItemStack that will represent the currency with this ID
         */
        public static ItemStack getMarkedItem(String id, int amount) {
            return NBTEditor.set(getItem(id, amount), id, "CurrencyId");
        }

        /**
         * Takes an ItemStack object and checks if it is considered a currency.
         *
         * @param item The item that will be checked
         * @return either an empty string, if the item is not a currency, or the ID of the currency
         */
        public static boolean isCurrency(ItemStack item) {
            return NBTEditor.contains(item, "CurrencyId");
        }

        /**
         * Takes an ItemStack object and gets the currency ID associated with it
         *
         * @param item The item that will be checked
         * @return the currency ID associated to this ItemStack, or an empty string if the item is not a currency-marked item
         */
        public static String getCurrencyID(ItemStack item) {
            if(isCurrency(item)) return NBTEditor.getString(item, "CurrencyId");
            else return "";
        }
    }

    public static class Worth {
        public static List<String> getIDs() {
            return new ArrayList<>(settings.getConfigurationSection("currency-worths").getKeys(false));
        }

        public static boolean hasID(String id) {
            return getIDs().contains(id);
        }

        public static LinkedHashMap<String, ConfigPair<Integer, String>> getWorthMap() {
            LinkedHashMap<String, ConfigPair<Integer, String>> output = new LinkedHashMap<>();
            getIDs().forEach(id -> output.put(id, getWorth(id)));
            return output;
        }

        public static ConfigPair<Integer, String> getWorth(String id) {
            if(hasID(id)) {
                return new ConfigPair<>(
                        Integer.parseInt(settings.getString("currency-worths." + id).split(" ")[0]),
                        settings.getString("currency-worths." + id).split(" ")[1]
                );
            }

            return new ConfigPair<>(-1, "null");
        }

        public static int getWorthAbove(String id, int steps) {
            List<String> ids = Economy.Currency.getIDs();

            if ((ids.indexOf(id) + steps) >= ids.size()) return 0;

            String above = ids.get(ids.indexOf(id) + steps);

            return Integer.parseInt(settings.getString("currency-worths." + above, "-1 null").split(" ")[0]);
        }

        public static int getWorthBelow(String id, int steps) {

            List<String> ids = Economy.Currency.getIDs();

            if ((ids.indexOf(id) - steps) < 0) return 0;

            String below = ids.get(ids.indexOf(id) - steps);

            if (!getWorthMap().containsKey(below)) return 0;

            return getWorth(below).getKey();
        }
    }
}
