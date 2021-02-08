package io.github.idoomful.assassinscurrencycore;

import com.google.gson.Gson;
import io.github.bananapuncher714.nbteditor.NBTEditor;
import io.github.idoomful.assassinscurrencycore.api.PAPI;
import io.github.idoomful.assassinscurrencycore.commands.CommandsClass;
import io.github.idoomful.assassinscurrencycore.configuration.ConfigManager;
import io.github.idoomful.assassinscurrencycore.configuration.SettingsYML;
import io.github.idoomful.assassinscurrencycore.configuration.ShopItem;
import io.github.idoomful.assassinscurrencycore.data.SQL.Lite;
import io.github.idoomful.assassinscurrencycore.events.EventsClass;
import io.github.idoomful.assassinscurrencycore.gui.inventories.BankInventoryGUI;
import io.github.idoomful.assassinscurrencycore.gui.inventories.ShopGUI;
import io.github.idoomful.assassinscurrencycore.gui.inventories.WalletGUI;
import io.github.idoomful.assassinscurrencycore.utils.CurrencyUtils;
import io.github.idoomful.assassinscurrencycore.utils.Economy;
import io.github.idoomful.assassinscurrencycore.utils.Utils;
import org.bukkit.Bukkit;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.*;

public class DMain extends JavaPlugin {
    private final String version = getDescription().getVersion();

    private static DMain plugin;
    private ConfigManager<DMain> conf;

    private final HashMap<String, List<ShopItem>> shopCategories = new HashMap<>();
    private final HashMap<UUID, ShopGUI> shops = new HashMap<>();
    private final HashMap<UUID, BankInventoryGUI> banks = new HashMap<>();
    private Lite sql;

    private final Gson gson = new Gson();

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

        File logsFolder = new File(getDataFolder(), "logs");

        if(!logsFolder.exists()) {
            if(logsFolder.mkdirs()) getLogger().info("Created logs folder");
            else getLogger().warning("Couldn't create logs folder..");
        }

        CurrencyUtils.createLogsFile();
    }

    @Override
    public void onDisable() {
        Bukkit.getOnlinePlayers().forEach(player -> {
            if(player.getOpenInventory().getTopInventory().getHolder() instanceof WalletGUI) {
                Inventory inv = player.getOpenInventory().getTopInventory();
                ItemStack wallet = Utils.usesVersionBetween("1.4.x", "1.8.x")
                        ? player.getItemInHand()
                        : player.getInventory().getItemInMainHand();

                if(inv.getHolder() instanceof WalletGUI) {
                    for(String curr : EventsClass.wallets.get(player.getUniqueId()).keySet()) {
                        wallet = NBTEditor.set(wallet, EventsClass.wallets.get(player.getUniqueId()).get(curr), curr);
                    }

                    int size = 0;

                    for(String id : Economy.Currency.getIDs()) {
                        if(NBTEditor.contains(wallet, id)) size++;
                    }

                    if(Economy.Currency.getIDs().size() > size) {
                        for(String id : Economy.Currency.getIDs()) {
                            if(!NBTEditor.contains(wallet, id)) wallet = NBTEditor.set(wallet, 0, id);
                        }
                    }

                    if(Utils.usesVersionBetween("1.4.x", "1.8.x")) player.setItemInHand(wallet);
                    else player.getInventory().setItemInMainHand(wallet);

                    EventsClass.wallets.remove(player.getUniqueId());
                }
                
                player.closeInventory();
            }
        });
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
