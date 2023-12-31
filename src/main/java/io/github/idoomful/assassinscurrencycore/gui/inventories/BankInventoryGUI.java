package io.github.idoomful.assassinscurrencycore.gui.inventories;

import io.github.idoomful.assassinscurrencycore.DMain;
import io.github.idoomful.assassinscurrencycore.configuration.SettingsYML;
import io.github.idoomful.assassinscurrencycore.gui.ItemBuilder;
import io.github.idoomful.assassinscurrencycore.gui.MyGUI;
import io.github.idoomful.assassinscurrencycore.gui.Paginable;
import io.github.idoomful.assassinscurrencycore.utils.Economy;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

public class BankInventoryGUI extends Paginable implements MyGUI {
    private final Inventory inventory;
    private final Player player;
    private final OfflinePlayer target;

    public BankInventoryGUI(Player player) {
        this.player = player;
        this.target = player;
        inventory = Bukkit.createInventory(this,
                SettingsYML.BankInventoryOptions.ROWS.getInt() * 9,
                SettingsYML.BankInventoryOptions.TITLE.getString(player)
        );
        openInventory(player);
    }

    public BankInventoryGUI(Player player, OfflinePlayer target) {
        this.player = player;
        this.target = target;

        inventory = Bukkit.createInventory(this,
                SettingsYML.BankInventoryOptions.ROWS.getInt() * 9,
                SettingsYML.BankInventoryOptions.TITLE.getString(target)
        );
        openInventory(player);
    }

    public OfflinePlayer getTarget() {
        return target;
    }

    @Override
    protected List<ItemStack> bodyList() {
        AtomicReference<LinkedHashMap<String, Integer>> map = new AtomicReference<>();
        DMain.getInstance().getSQL().getBankInventory(target.getName(), map::set);

        List<ItemStack> output = new ArrayList<>();

        for(Map.Entry<String, Integer> entry : map.get().entrySet()) {
            String currency = entry.getKey();
            int amount = entry.getValue();

            while(amount >= 64) {
                output.add(Economy.Currency.getMarkedItem(currency, 64));
                amount -= 64;
            }

            if(amount > 0) output.add(Economy.Currency.getMarkedItem(currency, amount));
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
        return SettingsYML.BankInventoryOptions.SKIPPING_POINTS.getIntArray();
    }

    @Override
    protected int itemsPerPage() {
        return SettingsYML.BankInventoryOptions.ITEMS_PER_PAGE.getInt();
    }

    @Override
    protected ItemStack createNextButton() {
        return ItemBuilder.build(SettingsYML.BankInventoryOptions.NEXT_PAGE_ICON.getString(target)
                .replace("$page$", getPage() + ""));
    }

    @Override
    protected ItemStack createPreviousButton() {
        return ItemBuilder.build(SettingsYML.BankInventoryOptions.PREVIOUS_PAGE_ICON.getString(target)
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
