package io.github.idoomful.assassinseconomy;

import io.github.idoomful.assassinseconomy.commands.CommandsClass;
import io.github.idoomful.assassinseconomy.configuration.ConfigManager;
import io.github.idoomful.assassinseconomy.configuration.SettingsYML;
import io.github.idoomful.assassinseconomy.configuration.ShopItem;
import io.github.idoomful.assassinseconomy.events.EventsClass;
import io.github.idoomful.assassinseconomy.gui.ShopGUI;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public class DMain extends JavaPlugin {
    private final String version = getDescription().getVersion();

    private static DMain plugin;
    private ConfigManager<DMain> conf;

    private final HashMap<String, List<ShopItem>> shopCategories = new HashMap<>();
    private final HashMap<UUID, ShopGUI> shops = new HashMap<>();
    //private ManagerSQL<DMain> sql;

    @Override
    public void onEnable() {
        plugin = this;
        conf = new ConfigManager<>(this);
        // sql = new ManagerSQL<>(this);
        new EventsClass(this);
        new CommandsClass(this);

        SettingsYML.Shops.OPTIONS.getCategoryIDs().forEach(id -> {
            List<ShopItem> items = new ArrayList<>();

            for(String itemID : SettingsYML.Shops.OPTIONS.getCategoryItems(id)) {
                items.add(new ShopItem(id, itemID));
            }

            shopCategories.put(id, items);
        });
    }

    @Override
    public void onDisable() {

    }

    public static DMain getInstance() {
        return plugin;
    }
    public String getVersion() {
        return version;
    }
    public ConfigManager<DMain> getConfigs() {
        return conf;
    }
    public HashMap<String, List<ShopItem>> getShopCategories() {
        return shopCategories;
    }
    public HashMap<UUID, ShopGUI> getOpenedShops() {
        return shops;
    }
    /*public ManagerSQL<DMain> getSQL() {
        return sql;
    }*/
}
