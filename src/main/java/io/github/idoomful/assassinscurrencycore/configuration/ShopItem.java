package io.github.idoomful.assassinscurrencycore.configuration;

import io.github.idoomful.assassinscurrencycore.DMain;
import io.github.idoomful.assassinscurrencycore.gui.ItemBuilder;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class ShopItem {
    private ItemStack icon;
    private List<ConfigPair<Integer, String>> prices = new ArrayList<>();
    private boolean giveItself;
    private List<String> commands = new ArrayList<>();

    public ShopItem(String category, String itemID) {
        FileConfiguration sett = DMain.getInstance().getConfigs().getFile("settings");
        icon = ItemBuilder.build(sett.getString("shops." + category + ".items." + itemID + ".icon"));

        for(String pricePair : sett.getStringList("shops." + category + ".items." + itemID + ".prices")) {
            int amount;
            String currency = pricePair.split(" ")[1];

            try {
                amount = Integer.parseInt(pricePair.split(" ")[0]);
            } catch(NumberFormatException ne) {
                amount = 0;
            }

            prices.add(new ConfigPair<>(amount, currency));
        }

        giveItself = sett.getBoolean("shops." + category + ".items." + itemID + ".give-itself", true);

        commands.addAll(sett.getStringList("shops." + category + ".items." + itemID + ".commands"));
    }

    public ItemStack getIcon() {
        return icon;
    }

    public List<ConfigPair<Integer, String>> getPrices() {
        return prices;
    }

    public boolean isGivingItself() {
        return giveItself;
    }

    public List<String> getCommands() {
        return commands;
    }

    public void setIcon(ItemStack icon) {
        this.icon = icon;
    }

    public void setPrices(List<ConfigPair<Integer, String>> prices) {
        this.prices = prices;
    }

    public void giveItself(boolean giveItself) {
        this.giveItself = giveItself;
    }

    public void setCommands(List<String> commands) {
        this.commands = commands;
    }
}
