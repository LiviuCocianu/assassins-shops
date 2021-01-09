package io.github.idoomful.assassinseconomy.gui.inventories;

import io.github.idoomful.assassinseconomy.DMain;
import io.github.idoomful.assassinseconomy.configuration.SettingsYML;
import io.github.idoomful.assassinseconomy.gui.ItemBuilder;
import io.github.idoomful.assassinseconomy.gui.MyGUI;
import io.github.idoomful.assassinseconomy.gui.Paginable;
import io.github.idoomful.assassinseconomy.utils.Economy;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

public class BankInventoryGUI extends Paginable implements MyGUI, InventoryHolder {
    private final Inventory inventory;
    private final Player player;

    public BankInventoryGUI(Player player) {
        this.player = player;
        inventory = Bukkit.createInventory(this,
                SettingsYML.BankInventoryOptions.ROWS.getInt() * 9,
                SettingsYML.BankInventoryOptions.TITLE.getString(player)
        );
        openInventory(player);
    }

    @Override
    protected List<ItemStack> bodyList() {
        AtomicReference<HashMap<String, Integer>> map = new AtomicReference<>();
        DMain.getInstance().getSQL().getCurrencies(player.getName(), map::set);

        List<ItemStack> output = new ArrayList<>();

        for(Map.Entry<String, Integer> entry : map.get().entrySet()) {
            String currency = entry.getKey();
            int amount = entry.getValue();

            while(amount >= 64) {
                output.add(Economy.Currency.getItem(currency, 64));
                amount -= 64;
            }

            if(amount > 0) output.add(Economy.Currency.getItem(currency, amount));
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
        return SettingsYML.BankInventoryOptions.SKIPPING_POINTS.getIntArray();
    }

    @Override
    protected int itemsPerPage() {
        return SettingsYML.BankInventoryOptions.ITEMS_PER_PAGE.getInt();
    }

    @Override
    protected ItemStack createNextButton() {
        return ItemBuilder.build(SettingsYML.BankInventoryOptions.NEXT_PAGE_ICON.getString(player)
                .replace("$page$", getPage() + ""));
    }

    @Override
    protected ItemStack createPreviousButton() {
        return ItemBuilder.build(SettingsYML.BankInventoryOptions.PREVIOUS_PAGE_ICON.getString(player)
                .replace("$page$", getPage() + ""));
    }

    @Override
    protected ItemStack createItemBeforeReplacement() {
        return new ItemStack(Material.AIR);
    }

    @Override
    protected int[] nextButtonSlots() {
        return SettingsYML.BankInventoryOptions.NEXT_BUTTON_SLOTS.getIntArray();
    }

    @Override
    protected int[] previousButtonSlots() {
        return SettingsYML.BankInventoryOptions.PREVIOUS_BUTTON_SLOTS.getIntArray();
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
