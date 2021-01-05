package io.github.idoomful.assassinseconomy.configuration;

import io.github.idoomful.assassinseconomy.DMain;
import io.github.idoomful.assassinseconomy.gui.ItemBuilder;
import io.github.idoomful.assassinseconomy.utils.Utils;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public enum SettingsYML {
    _OPTIONS(""),
    SHOP_ROWS("shop-rows"),
    PRICE_FORMAT("price-format"),
    PRICE_LORE("price-lore"),
    SKIPPING_POINTS("skipping-points"),
    ITEMS_PER_PAGE("items-per-page"),
    NEXT_PAGE_ICON("next-page-icon"),
    NEXT_BUTTON_SLOTS("next-button-slots"),
    PREVIOUS_PAGE_ICON("previous-page-icon"),
    PREVIOUS_BUTTON_SLOTS("previous-button-slots");

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
    public String getString(Player player) {
        return Utils.placeholder(player, settings.getString(path));
    }
    public List<String> getStringList(Player player) {
        return Utils.placeholder(player, settings.getStringList(path));
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

    public enum Currencies {
        OPTIONS;

        public String path;
        public FileConfiguration settings;

        Currencies() {
            this.path = "currencies";
            this.settings = SettingsYML._OPTIONS.settings;
        }

        public List<String> getIDs() {
            return new ArrayList<>(settings.getConfigurationSection(path).getKeys(false));
        }

        public boolean hasID(String id) {
            return getIDs().contains(id);
        }

        public ItemStack getItem(String id, int amount) {
            if(hasID(id)) {
                ItemStack output = ItemBuilder.build(settings.getString(path + "." + id));
                output.setAmount(amount);
                return output;
            }

            ItemStack def = new ItemStack(Material.STONE);
            ItemMeta im = def.getItemMeta();
            im.setDisplayName(MessagesYML.Errors.ITEM_INVALID_CURRENCY.color(null));
            def.setItemMeta(im);
            return def;
        }
    }

    public enum CurrencyWorths {
        OPTIONS;

        public String path;
        public FileConfiguration settings;

        CurrencyWorths() {
            this.path = "currency-worths";
            this.settings = SettingsYML._OPTIONS.settings;
        }

        public List<String> getIDs() {
            return new ArrayList<>(settings.getConfigurationSection(path).getKeys(false));
        }

        public boolean hasID(String id) {
            return getIDs().contains(id);
        }

        public ConfigPair<Integer, String> getWorth(String id) {
            if(hasID(id)) {
                return new ConfigPair<>(
                        Integer.parseInt(settings.getString(path + "." + id).split(" ")[0]),
                        settings.getString(path + "." + id).split(" ")[1]
                );
            }

            return null;
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
