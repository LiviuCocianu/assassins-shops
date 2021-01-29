package io.github.idoomful.assassinseconomy.utils;

import io.github.bananapuncher714.nbteditor.NBTEditor;
import io.github.idoomful.assassinseconomy.DMain;
import io.github.idoomful.assassinseconomy.configuration.ConfigPair;
import io.github.idoomful.assassinseconomy.configuration.MessagesYML;
import io.github.idoomful.assassinseconomy.gui.ItemBuilder;
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
        public static List<String> getIDs() {
            return new ArrayList<>(settings.getConfigurationSection("currencies").getKeys(false));
        }

        public static boolean hasID(String id) {
            return getIDs().contains(id);
        }

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

        public static ItemStack getMarkedItem(String id, int amount) {
            return NBTEditor.set(getItem(id, amount), id, "CurrencyId");
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

            return null;
        }

        public static int getWorthAbove(String id, int steps) {
            List<String> ids = Economy.Currency.getIDs();

            if ((ids.indexOf(id) + steps) >= ids.size()) return 0;

            String above = ids.get(ids.indexOf(id) + steps);

            return Integer.parseInt(settings.getString("currency-worths." + above).split(" ")[0]);
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
