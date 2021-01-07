package io.github.idoomful.assassinseconomy.gui;

import io.github.idoomful.assassinseconomy.DMain;
import io.github.idoomful.assassinseconomy.configuration.ConfigPair;
import io.github.idoomful.assassinseconomy.configuration.MessagesYML;
import io.github.idoomful.assassinseconomy.configuration.SettingsYML;
import io.github.idoomful.assassinseconomy.utils.Utils;
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
        inventory = Bukkit.createInventory(this, SettingsYML.SHOP_ROWS.getInt() * 9, SettingsYML.Shops.OPTIONS.getCategoryTitle(category));
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

            for(String line : SettingsYML.PRICE_LORE.getStringList(player)) {
                if(line.contains("$prices$")) {
                    for(ConfigPair<Integer, String> price : DMain.getInstance().getShopCategories().get(category).get(i).getPrices()) {
                        lore.add(Utils.color(line.replace("$prices$", SettingsYML.PRICE_FORMAT.getString(player)
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
    }

    @Override
    public void previousPage() {
        inventory.clear();
        refreshPage();
        super.previousPage();
    }

    @Override
    protected int[] skippingPoints() {
        return SettingsYML.SKIPPING_POINTS.getIntArray();
    }

    @Override
    protected int itemsPerPage() {
        return SettingsYML.ITEMS_PER_PAGE.getInt();
    }

    @Override
    protected ItemStack createNextButton() {
        return SettingsYML.NEXT_PAGE_ICON.getItem();
    }

    @Override
    protected ItemStack createPreviousButton() {
        return SettingsYML.PREVIOUS_PAGE_ICON.getItem();
    }

    @Override
    protected ItemStack createItemBeforeReplacement() {
        return new ItemStack(Material.AIR);
    }

    @Override
    protected int[] nextButtonSlots() {
        return SettingsYML.NEXT_BUTTON_SLOTS.getIntArray();
    }

    @Override
    protected int[] previousButtonSlots() {
        return SettingsYML.PREVIOUS_BUTTON_SLOTS.getIntArray();
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
