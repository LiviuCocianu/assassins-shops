package io.github.idoomful.assassinscurrencycore.gui.inventories;

import io.github.idoomful.assassinscurrencycore.DMain;
import io.github.idoomful.assassinscurrencycore.configuration.ConfigPair;
import io.github.idoomful.assassinscurrencycore.configuration.MessagesYML;
import io.github.idoomful.assassinscurrencycore.configuration.SettingsYML;
import io.github.idoomful.assassinscurrencycore.gui.MyGUI;
import io.github.idoomful.assassinscurrencycore.gui.Paginable;
import io.github.idoomful.assassinscurrencycore.utils.Utils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class ShopGUI extends Paginable implements MyGUI, InventoryHolder {
    private final Inventory inventory;
    private final String category;
    private final Player player;

    public ShopGUI(String category, Player player) {
        inventory = Bukkit.createInventory(this, SettingsYML.ShopOptions.ROWS.getInt() * 9, SettingsYML.Shops.OPTIONS.getCategoryTitle(category));
        this.category = category;
        this.player = player;
        openInventory(player);
    }

    @Override
    protected List<ItemStack> bodyList() {
        List<ItemStack> output = new ArrayList<>();
        DMain.getInstance().getShopCategories().get(category).forEach(si -> output.add(si.getIcon().clone()));

        for(int i = 0; i < output.size(); i++) {
            ItemStack item = output.get(i);
            ItemMeta im = item.getItemMeta();

            List<String> lore = new ArrayList<>();

            if(item.hasItemMeta() && item.getItemMeta().hasLore()) {
                lore.addAll(im.getLore());
            }

            for(String line : SettingsYML.ShopOptions.PRICE_LORE.getStringList(player)) {
                if(line.contains("$prices$")) {
                    for(ConfigPair<Integer, String> price : DMain.getInstance().getShopCategories().get(category).get(i).getPrices()) {
                        lore.add(Utils.color(line.replace("$prices$", SettingsYML.ShopOptions.PRICE_FORMAT.getString(player)
                                .replace("$amount$", price.getKey() + ""))
                                .replace("$currency$", MessagesYML.Currencies.OPTIONS.getString(price.getValue())))
                        );
                    }

                    continue;
                }

                lore.add(Utils.color(line));
            }

            im.setLore(lore);
            item.setItemMeta(im);
            output.set(i, item);
        }

        return output;
    }

    @Override
    public void nextPage() {
        inventory.clear();
        refreshPage();
        super.nextPage();

        SettingsYML.SFX.PAGE_TURN.playSoundFor(player);
    }

    @Override
    public void previousPage() {
        inventory.clear();
        refreshPage();
        super.previousPage();

        SettingsYML.SFX.PAGE_TURN.playSoundFor(player);
    }

    @Override
    protected int[] skippingPoints() {
        return SettingsYML.ShopOptions.SKIPPING_POINTS.getIntArray();
    }

    @Override
    protected int itemsPerPage() {
        return SettingsYML.ShopOptions.ITEMS_PER_PAGE.getInt();
    }

    @Override
    protected ItemStack createNextButton() {
        return SettingsYML.ShopOptions.NEXT_PAGE_ICON.getItem();
    }

    @Override
    protected ItemStack createPreviousButton() {
        return SettingsYML.ShopOptions.PREVIOUS_PAGE_ICON.getItem();
    }

    @Override
    protected ItemStack createItemBeforeReplacement() {
        return new ItemStack(Material.AIR);
    }

    @Override
    protected int[] nextButtonSlots() {
        return SettingsYML.ShopOptions.NEXT_BUTTON_SLOTS.getIntArray();
    }

    @Override
    protected int[] previousButtonSlots() {
        return SettingsYML.ShopOptions.PREVIOUS_BUTTON_SLOTS.getIntArray();
    }

    @Override
    public Inventory getInventory() {
        return inventory;
    }

    @Override
    public void openInventory(Player player) {
        createFirstPage();
        player.openInventory(inventory);
    }
}
