package io.github.idoomful.assassinscurrencycore;

import com.google.gson.Gson;
import io.github.idoomful.assassinscurrencycore.api.PAPI;
import io.github.idoomful.assassinscurrencycore.commands.CommandsClass;
import io.github.idoomful.assassinscurrencycore.configuration.ConfigManager;
import io.github.idoomful.assassinscurrencycore.configuration.SettingsYML;
import io.github.idoomful.assassinscurrencycore.configuration.ShopItem;
import io.github.idoomful.assassinscurrencycore.data.SQL.Lite;
import io.github.idoomful.assassinscurrencycore.events.EventsClass;
import io.github.idoomful.assassinscurrencycore.gui.inventories.BankInventoryGUI;
import io.github.idoomful.assassinscurrencycore.gui.inventories.ShopGUI;
import io.github.idoomful.assassinscurrencycore.utils.Economy;
import org.bukkit.Bukkit;
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
    private final HashMap<UUID, BankInventoryGUI> banks = new HashMap<>();
    private Lite sql;

    private Gson gson = new Gson();

    @Override
    public void onEnable() {
        plugin = this;
        conf = new ConfigManager<>(this);

        new Economy();

        sql = new Lite(this);
        new EventsClass(this);
        new CommandsClass(this);

        SettingsYML.Shops.OPTIONS.getCategoryIDs().forEach(id -> {
            List<ShopItem> items = new ArrayList<>();

            for(String itemID : SettingsYML.Shops.OPTIONS.getCategoryItems(id)) {
                items.add(new ShopItem(id, itemID));
            }

            shopCategories.put(id, items);
        });

        if(Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI") && (new PAPI(this).register())) {
            Bukkit.getLogger().info(getDescription().getName() + " hooked into PlaceholderAPI successfully..");
        } else {
            Bukkit.getLogger().info(getDescription().getName() + " couldn't hook into PlaceholderAPI, proceeding anyway..");
        }
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
    public HashMap<UUID, BankInventoryGUI> getOpenedBanks() {
        return banks;
    }
    public Lite getSQL() {
        return sql;
    }
    public Gson getGson() {
        return gson;
    }
}
