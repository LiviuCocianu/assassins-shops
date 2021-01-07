package io.github.idoomful.assassinseconomy.events;

import io.github.bananapuncher714.nbteditor.NBTEditor;
import io.github.idoomful.assassinseconomy.DMain;
import io.github.idoomful.assassinseconomy.configuration.ConfigPair;
import io.github.idoomful.assassinseconomy.configuration.MessagesYML;
import io.github.idoomful.assassinseconomy.configuration.SettingsYML;
import io.github.idoomful.assassinseconomy.configuration.ShopItem;
import io.github.idoomful.assassinseconomy.gui.BankGUI;
import io.github.idoomful.assassinseconomy.gui.ItemBuilder;
import io.github.idoomful.assassinseconomy.gui.ShopGUI;
import io.github.idoomful.assassinseconomy.utils.CurrencyUtils;
import io.github.idoomful.assassinseconomy.utils.Events;
import io.github.idoomful.assassinseconomy.utils.Utils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

public class EventsClass implements Listener {
    private final DMain main;

    private final ArrayList<UUID> chatEvent = new ArrayList<>();
    private final HashMap<UUID, ShopItem> targetedItem = new HashMap<>();

    public EventsClass(DMain main) {
        this.main = main;

        Bukkit.getPluginManager().registerEvents(this, main);

        Events.listen(main, InventoryCloseEvent.class, e -> {
            main.getOpenedShops().remove(e.getPlayer().getUniqueId());
        });

        Events.listen(main, PlayerQuitEvent.class, e -> {
           chatEvent.remove(e.getPlayer().getUniqueId());
           targetedItem.remove(e.getPlayer().getUniqueId());
        });
    }

    // Mark unregistered currency with its appropriate ID
    // Useful for when other plugins try to give this currency to players
    @EventHandler
    public void onClick(InventoryClickEvent e) {
        ItemStack[] items = e.getInventory().getContents();

        if(e.getCursor() != null) {
            ItemStack item = e.getCursor();

            for(String id : SettingsYML.Currencies.OPTIONS.getIDs()) {
                if(item.isSimilar(SettingsYML.Currencies.OPTIONS.getItem(id, item.getAmount()))) {
                    if(!NBTEditor.contains(item, "CurrencyId")) {
                        e.setCursor(NBTEditor.set(item, id, "CurrencyId"));
                        return;
                    }
                }
            }
        }

        for(int i = 0; i < items.length; i++) {
            if(items[i] == null) continue;

            ItemStack item = items[i];

            for(String id : SettingsYML.Currencies.OPTIONS.getIDs()) {
                if(item.isSimilar(SettingsYML.Currencies.OPTIONS.getItem(id, item.getAmount()))) {
                    if(!NBTEditor.contains(item, "CurrencyId"))
                    e.getInventory().setItem(i, NBTEditor.set(item, id, "CurrencyId"));
                }
            }
        }
    }

    @EventHandler
    public void onGUIClick(InventoryClickEvent e) {
        if(e.getClickedInventory() == null) return;
        if(e.getCurrentItem() == null) return;
        if(e.getCurrentItem().getType() == Material.AIR) return;

        Player player = (Player) e.getWhoClicked();
        ItemStack clicked = e.getCurrentItem();

        if(e.getClickedInventory().getHolder() instanceof ShopGUI) {
            e.setCancelled(true);

            if(clicked.isSimilar(SettingsYML.NEXT_PAGE_ICON.getItem())) {
                main.getOpenedShops().get(player.getUniqueId()).nextPage();
                return;
            }

            if(clicked.isSimilar(SettingsYML.PREVIOUS_PAGE_ICON.getItem())) {
                main.getOpenedShops().get(player.getUniqueId()).previousPage();
                return;
            }

            for(String category : SettingsYML.Shops.OPTIONS.getCategoryIDs()) {
                for(int i = 0; i < DMain.getInstance().getShopCategories().get(category).size(); i++) {
                    ShopItem shopItem = DMain.getInstance().getShopCategories().get(category).get(i);

                    ItemStack bareItem = clicked.clone();

                    if(!bareItem.hasItemMeta()) return;

                    ItemMeta im = bareItem.getItemMeta();

                    if(!im.hasLore()) return;

                    List<String> lore = new ArrayList<>();

                    for(String line : SettingsYML.PRICE_LORE.getStringList(player)) {
                        if(line.contains("$prices$")) {
                            for(ConfigPair<Integer, String> price : shopItem.getPrices()) {
                                lore.add(Utils.color(line.replace("$prices$", SettingsYML.PRICE_FORMAT.getString(player)
                                        .replace("$amount$", price.getKey() + ""))
                                        .replace("$currency$", MessagesYML.Currencies.OPTIONS.getString(price.getValue())))
                                );
                            }

                            continue;
                        }

                        lore.add(Utils.color(line));
                    }

                    List<String> changedLore = im.getLore();
                    changedLore.removeAll(lore);

                    im.setLore(changedLore);
                    bareItem.setItemMeta(im);

                    if(bareItem.isSimilar(shopItem.getIcon())) {
                        chatEvent.add(player.getUniqueId());
                        targetedItem.put(player.getUniqueId(), shopItem);
                        player.closeInventory();
                        player.sendMessage(MessagesYML.SPECIFY_AMOUNT.withPrefix(player));
                    }
                }
            }
        } else if(e.getView().getTopInventory().getHolder() instanceof BankGUI
                && (e.getView().getBottomInventory().equals(e.getClickedInventory())
                || e.getView().getTopInventory().equals(e.getClickedInventory()))) {

            HashMap<String, Integer> toDeposit = new HashMap<>();

            // Check if they clicked on the deposit icon
            if(clicked.isSimilar(SettingsYML.Bank.ITEMS.getItem("d"))) {
                for(ItemStack item : e.getClickedInventory().getContents()) {
                    if(item == null) continue;

                    if(NBTEditor.contains(item, "CurrencyId")) {
                        String currency = NBTEditor.getString(item, "CurrencyId");
                        if(!toDeposit.containsKey(currency)) toDeposit.put(currency, 0);
                        toDeposit.put(currency, toDeposit.get(currency) + item.getAmount());
                    }
                }

                StringBuilder currencies = new StringBuilder();

                int index = 0;
                for(Map.Entry<String, Integer> entry : toDeposit.entrySet()) {
                    index++;
                    String currency = entry.getKey();
                    int amount = entry.getValue();

                    main.getSQL().addCurrency(player.getName(), currency, amount);

                    currencies.append(MessagesYML.CURRENCY_FORMAT.color(player)
                            .replace("$amount$", amount + "")
                            .replace("$currency$", MessagesYML.Currencies.OPTIONS.getString(currency))
                    );

                    if(index != toDeposit.size()) currencies.append(MessagesYML.CURRENCY_SEPARATOR.color(player));
                }

                player.closeInventory();
                player.sendMessage(MessagesYML.CURRENCY_SAVED.withPrefix(player)
                        .replace("$currencies$", Utils.color(currencies.toString())));
            } else if(!NBTEditor.contains(clicked, "CurrencyId")) {
                e.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent e) {
        Player player = e.getPlayer();
        String message = e.getMessage();

        if(chatEvent.contains(player.getUniqueId())) {
            e.setCancelled(true);

            if(message.equalsIgnoreCase("cancel") || message.equalsIgnoreCase("anulare")) {
                chatEvent.remove(player.getUniqueId());
                targetedItem.remove(player.getUniqueId());
                player.sendMessage(MessagesYML.CANCEL_AMOUNT.withPrefix(player));
                return;
            }

            ShopItem shopItem = targetedItem.get(player.getUniqueId());

            try {
                int amount = Integer.parseInt(message);

                if(amount < 0) {
                    player.sendMessage(MessagesYML.Errors.NO_NEGATIVE.withPrefix(player));
                    return;
                }

                if(shopItem.getIcon().getMaxStackSize() == 1 && amount > 1) {
                    player.sendMessage(MessagesYML.Errors.UNSTACKABLE_ITEM.withPrefix(player));
                    return;
                }

                boolean oneIsFaulty = false;

                for(ConfigPair<Integer, String> pair : shopItem.getPrices()) {
                    boolean result = CurrencyUtils.withdrawCurrency(pair.getValue(), pair.getKey() * amount, player);
                    if(!result) oneIsFaulty = true;
                }

                if(oneIsFaulty) {
                    player.sendMessage(MessagesYML.Errors.TRANSACTION_ERROR.withPrefix(player));
                    chatEvent.remove(player.getUniqueId());
                    targetedItem.remove(player.getUniqueId());
                    return;
                }

                if(shopItem.isGivingItself()) {
                    ItemStack toGive = shopItem.getIcon().clone();
                    toGive.setAmount(amount * toGive.getAmount());
                    player.getInventory().addItem(toGive);
                }

                for(String cmd : shopItem.getCommands()) {
                    if(cmd.startsWith("[message] ")) {
                        String msg = cmd.replace("[message] ", "")
                                .replace("$prefix$", MessagesYML.PREFIX.color(player))
                                .replace("$quantity$", amount + "")
                                .replace("$player$", player.getName());
                        player.sendMessage(Utils.placeholder(player, msg));
                        continue;
                    }

                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(),
                            Utils.placeholder(player,
                                    cmd.replace("$prefix$", MessagesYML.PREFIX.color(player))
                                            .replace("$quantity$", amount + "")
                                            .replace("$player$", player.getName())
                            )
                    );
                }

                chatEvent.remove(player.getUniqueId());
                targetedItem.remove(player.getUniqueId());
            } catch(NumberFormatException ne) {
                player.sendMessage(MessagesYML.Errors.NO_NUMBER.withPrefix(player));
            }
        }
    }
}
