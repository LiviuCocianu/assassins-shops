package io.github.idoomful.assassinseconomy.configuration;

import io.github.idoomful.assassinseconomy.DMain;
import io.github.idoomful.assassinseconomy.gui.ItemBuilder;
import io.github.idoomful.assassinseconomy.utils.Utils;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public enum SettingsYML {
    _OPTIONS("");

    String path;
    FileConfiguration settings;

    SettingsYML(String output) {
        settings = DMain.getInstance().getConfigs().getFile("settings");
        this.path = output;
    }

    public void reload() {
        settings = DMain.getInstance().getConfigs().getFile("settings");
    }
    public int getInt() {
        return settings.getInt(path);
    }
    public ItemStack getItem() {
        return ItemBuilder.build(settings.getString(path));
    }

    public enum ShopOptions {
        ROWS("rows"),
        PRICE_FORMAT("price-format"),
        PRICE_LORE("price-lore"),

        SKIPPING_POINTS("skipping-points"),
        ITEMS_PER_PAGE("items-per-page"),
        NEXT_PAGE_ICON("next-page-icon"),
        NEXT_BUTTON_SLOTS("next-button-slots"),
        PREVIOUS_PAGE_ICON("previous-page-icon"),
        PREVIOUS_BUTTON_SLOTS("previous-button-slots");

        public String path;
        public FileConfiguration settings;

        ShopOptions(String string) {
            this.path = "shop-options." + string;
            this.settings = SettingsYML._OPTIONS.settings;
        }

        public String getString(Player player) {
            return Utils.placeholder(player, settings.getString(path));
        }
        public int getInt() {
            return settings.getInt(path);
        }
        public int[] getIntArray() {
            List<Integer> configList = settings.getIntegerList(path);
            int[] output = new int[configList.size()];

            for(int i = 0; i < configList.size(); i++) {
                output[i] = configList.get(i);
            }

            return output;
        }
        public ItemStack getItem() {
            return ItemBuilder.build(settings.getString(path));
        }
        public List<String> getStringList(Player player) {
            return Utils.placeholder(player, settings.getStringList(path));
        }
    }

    public enum BankOptions {
        TITLE("title"),
        ROWS("rows"),
        ITEMS("items"),
        LAYOUT("layout");

        public String path;
        public FileConfiguration settings;

        BankOptions(String string) {
            this.path = "bank-options." + string;
            this.settings = SettingsYML._OPTIONS.settings;
        }

        public int getInt() {
            return settings.getInt(path);
        }
        public String getString(Player player) {
            return Utils.placeholder(player, settings.getString(path));
        }
        public List<String> getStringList(Player player) {
            return Utils.placeholder(player, settings.getStringList(path));
        }
        public List<String> getSymbols() {
            return new ArrayList<>(settings.getConfigurationSection(path).getKeys(false));
        }

        public List<String> getItemList() {
            List<String> output = new ArrayList<>();
            getSymbols().forEach(s -> output.add(s + " " + settings.getString(path + "." + s)));
            return output;
        }

        public ItemStack getItem(String symbol) {
            return ItemBuilder.build(settings.getString(path + "." + symbol));
        }
    }

    public enum BankInventoryOptions {
        TITLE("title"),
        ROWS("rows"),
        SKIPPING_POINTS("skipping-points"),
        ITEMS_PER_PAGE("items-per-page"),
        NEXT_PAGE_ICON("next-page-icon"),
        NEXT_BUTTON_SLOTS("next-button-slots"),
        PREVIOUS_PAGE_ICON("previous-page-icon"),
        PREVIOUS_BUTTON_SLOTS("previous-button-slots");

        public String path;
        public FileConfiguration settings;

        BankInventoryOptions(String string) {
            this.path = "bank-inventory-options." + string;
            this.settings = SettingsYML._OPTIONS.settings;
        }

        public String getString(Player player) {
            return Utils.placeholder(player, settings.getString(path));
        }
        public int getInt() {
            return settings.getInt(path);
        }
        public int[] getIntArray() {
            List<Integer> configList = settings.getIntegerList(path);
            int[] output = new int[configList.size()];

            for(int i = 0; i < configList.size(); i++) {
                output[i] = configList.get(i);
            }

            return output;
        }
        public ItemStack getItem() {
            return ItemBuilder.build(settings.getString(path));
        }
    }

    public enum RepairCosts {
        OPTIONS;

        public String path;
        public FileConfiguration settings;

        RepairCosts() {
            this.path = "repair-costs.";
            this.settings = SettingsYML._OPTIONS.settings;
        }

        public boolean isGadgetReplenishable(String id) {
            return settings.getConfigurationSection(path + "gadgets").getKeys(false).contains(id);
        }

        public int getGivenUses(String id) {
            return settings.getInt(path + "gadgets." + id + ".uses-given");
        }

        public List<ConfigPair<Integer, String>> getGadgetCosts(String id) {
            List<ConfigPair<Integer, String>> output = new ArrayList<>();
            settings.getStringList(path + "gadgets." + id + ".costs").forEach(line -> {
                output.add(new ConfigPair<>(Integer.parseInt(line.split(" ")[0]), line.split(" ")[1]));
            });
            return output;
        }

        public int getGivenDurability() {
            return settings.getInt(path + "durability.durability-given");
        }

        public List<ConfigPair<Integer, String>> getDurabilityCosts() {
            List<ConfigPair<Integer, String>> output = new ArrayList<>();
            settings.getStringList(path + "durability.costs").forEach(line -> {
                output.add(new ConfigPair<>(Integer.parseInt(line.split(" ")[0]), line.split(" ")[1]));
            });
            return output;
        }
    }

    public enum Shops {
        OPTIONS;

        public String path;
        public FileConfiguration settings;

        Shops() {
            this.path = "shops";
            this.settings = SettingsYML._OPTIONS.settings;
        }

        public List<String> getCategoryIDs() {
            return new ArrayList<>(settings.getConfigurationSection(path).getKeys(false));
        }

        public String getCategoryTitle(String id) {
            return Utils.color(settings.getString(path + "." + id + ".title"));
        }

        public List<String> getCategoryItems(String id) {
            return new ArrayList<>(settings.getConfigurationSection(path + "." + id + ".items").getKeys(false));
        }

        public boolean hasCategory(String id) {
            return getCategoryIDs().contains(id);
        }
    }
}
