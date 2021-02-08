package io.github.idoomful.assassinscurrencycore.events;

import io.github.bananapuncher714.nbteditor.NBTEditor;
import io.github.idoomful.assassinscurrencycore.DMain;
import io.github.idoomful.assassinscurrencycore.data.SQL.TransactionLog;
import io.github.idoomful.assassinscurrencycore.utils.ConfigPair;
import io.github.idoomful.assassinscurrencycore.configuration.MessagesYML;
import io.github.idoomful.assassinscurrencycore.configuration.SettingsYML;
import io.github.idoomful.assassinscurrencycore.configuration.ShopItem;
import io.github.idoomful.assassinscurrencycore.gui.ItemBuilder;
import io.github.idoomful.assassinscurrencycore.gui.inventories.DepositGUI;
import io.github.idoomful.assassinscurrencycore.gui.inventories.BankInventoryGUI;
import io.github.idoomful.assassinscurrencycore.gui.inventories.ShopGUI;
import io.github.idoomful.assassinscurrencycore.gui.inventories.WalletGUI;
import io.github.idoomful.assassinscurrencycore.utils.CurrencyUtils;
import io.github.idoomful.assassinscurrencycore.utils.Economy;
import io.github.idoomful.assassinscurrencycore.utils.Events;
import io.github.idoomful.assassinscurrencycore.utils.Utils;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;

import static org.bukkit.event.EventPriority.MONITOR;

public class EventsClass implements Listener {
    private final DMain main;

    private final ArrayList<UUID> walletMovementCooldown = new ArrayList<>();
    private final ArrayList<UUID> walletCooldown = new ArrayList<>();
    private final ArrayList<UUID> chatEvent = new ArrayList<>();
    private final HashMap<UUID, ShopItem> targetedItem = new HashMap<>();
    public static final HashMap<UUID, LinkedHashMap<String, Integer>> wallets = new HashMap<>();

    public EventsClass(DMain main) {
        this.main = main;

        Bukkit.getPluginManager().registerEvents(this, main);

        Events.listen(main, InventoryCloseEvent.class, e -> {
            main.getOpenedShops().remove(e.getPlayer().getUniqueId());
            main.getOpenedBanks().remove(e.getPlayer().getUniqueId());

            if(e.getInventory().getHolder() instanceof DepositGUI) {
                for(int i = 0; i < 27; i++) {
                    ItemStack item = e.getInventory().getItem(i);
                    if(item == null) continue;
                    e.getPlayer().getInventory().addItem(item);
                }
            }
        });

        Events.listen(main, PlayerQuitEvent.class, e -> {
           chatEvent.remove(e.getPlayer().getUniqueId());
           targetedItem.remove(e.getPlayer().getUniqueId());
        });
    }

    @EventHandler
    public void onOpen(InventoryOpenEvent e) {
        Player player = (Player) e.getPlayer();
        Inventory inv = e.getInventory();

        if(inv.getHolder() instanceof WalletGUI) {
            ItemStack wallet = Utils.usesVersionBetween("1.4.x", "1.8.x")
                    ? player.getItemInHand()
                    : player.getInventory().getItemInMainHand();

            LinkedHashMap<String, Integer> walletContent = new LinkedHashMap<>();
            Economy.Currency.getIDs().forEach(curr -> walletContent.put(curr, NBTEditor.getInt(wallet, curr)));
            wallets.put(player.getUniqueId(), walletContent);
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent e) {
        Player player = (Player) e.getPlayer();
        Inventory inv = e.getInventory();
        ItemStack wallet = Utils.usesVersionBetween("1.4.x", "1.8.x")
                ? player.getItemInHand()
                : player.getInventory().getItemInMainHand();

        if(inv.getHolder() instanceof WalletGUI) {
            for(String curr : wallets.get(player.getUniqueId()).keySet()) {
                wallet = NBTEditor.set(wallet, wallets.get(player.getUniqueId()).get(curr), curr);
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

            Utils.updateWalletLore(player.getInventory());

            walletCooldown.add(player.getUniqueId());
            Bukkit.getScheduler().scheduleSyncDelayedTask(main, () -> {
                walletCooldown.remove(player.getUniqueId());
            }, SettingsYML.WalletOptions.COOLDOWN.getInt() * 20);

            wallets.remove(player.getUniqueId());
        }
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent e) {
        Player player = e.getPlayer();
        ItemStack inHand = Utils.usesVersionBetween("1.4.x", "1.8.x")
                ? player.getItemInHand()
                : player.getInventory().getItemInMainHand();

        if(inHand == null || inHand.getType() == Material.AIR) return;

        if(e.getAction() == Action.RIGHT_CLICK_AIR || e.getAction() == Action.RIGHT_CLICK_BLOCK) {
            if(NBTEditor.contains(inHand, "WalletRows")) {
                e.setCancelled(true);

                if(!walletCooldown.contains(player.getUniqueId())) {
                    new WalletGUI(player, inHand);
                    SettingsYML.SFX.WALLET_OPEN.playSoundFor(player);
                } else {
                    player.sendMessage(MessagesYML.Errors.COOLDOWN.withPrefix(player)
                            .replace("$cooldown$", SettingsYML.WalletOptions.COOLDOWN.getInt() + "")
                    );
                }
            }
        }
    }

    // Mark unregistered currency with its appropriate ID
    // Useful for when other plugins try to give this currency to players
    @EventHandler
    public void onClick(InventoryClickEvent e) {
        ItemStack[] items = e.getInventory().getContents();

        if(e.getCursor() != null) {
            ItemStack item = e.getCursor();

            for(String id : Economy.Currency.getIDs()) {
                if(item.isSimilar(Economy.Currency.getItem(id, item.getAmount()))) {
                    if(!NBTEditor.contains(item, "CurrencyId")) {
                        List<ConfigPair<Integer, String>> currs = new ArrayList<>();
                        currs.add(new ConfigPair<>(item.getAmount(), id));

                        CurrencyUtils.logTransaction(new TransactionLog(
                                e.getWhoClicked().getName(),
                                e.getWhoClicked().getName(),
                                "found in cursor",
                                currs
                        ).pseudoCurrency(true));

                        e.setCursor(NBTEditor.set(item, id, "CurrencyId"));
                        return;
                    }
                }
            }
        }

        for(int i = 0; i < items.length; i++) {
            if(items[i] == null) continue;

            ItemStack item = items[i];

            for(String id : Economy.Currency.getIDs()) {
                if(item.isSimilar(Economy.Currency.getItem(id, item.getAmount()))) {
                    if(!NBTEditor.contains(item, "CurrencyId")) {
                        List<ConfigPair<Integer, String>> currs = new ArrayList<>();
                        currs.add(new ConfigPair<>(item.getAmount(), id));

                        CurrencyUtils.logTransaction(new TransactionLog(
                                e.getWhoClicked().getName(),
                                e.getWhoClicked().getName(),
                                "found somewhere in inventory",
                                currs
                        ).pseudoCurrency(true));

                        e.getInventory().setItem(i, NBTEditor.set(item, id, "CurrencyId"));
                    }
                }
            }
        }
    }

    @EventHandler
    public void onShopClick(InventoryClickEvent e) {
        if(e.getClickedInventory() == null) return;

        Player player = (Player) e.getWhoClicked();
        ItemStack clicked = e.getCurrentItem();

        if(e.getClickedInventory().getHolder() instanceof ShopGUI) {
            if(e.getCurrentItem().getType() == Material.AIR) return;
            e.setCancelled(true);

            if(clicked.isSimilar(SettingsYML.ShopOptions.NEXT_PAGE_ICON.getItem())) {
                main.getOpenedShops().get(player.getUniqueId()).nextPage();
                return;
            }

            if(clicked.isSimilar(SettingsYML.ShopOptions.PREVIOUS_PAGE_ICON.getItem())) {
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

                    for(String line : SettingsYML.ShopOptions.PRICE_LORE.getStringList(player)) {
                        if(line.contains("$prices$")) {
                            for(ConfigPair<Integer, String> price : shopItem.getPrices()) {
                                lore.add(Utils.color(line.replace("$prices$", SettingsYML.ShopOptions.PRICE_FORMAT.getString(player)
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

                        SettingsYML.SFX.REQUIRE_INPUT.playSoundFor(player);

                        Bukkit.getScheduler().scheduleSyncDelayedTask(main, () -> {
                            if(chatEvent.contains(player.getUniqueId())) {
                                chatEvent.remove(player.getUniqueId());
                                player.sendMessage(MessagesYML.Errors.TOOK_TOO_LONG.withPrefix(player));
                            }
                        }, 20 * SettingsYML.ShopOptions.CHOICE_TIMER.getInt());
                    }
                }
            }
        }
    }

    @EventHandler
    public void onBankInventoryClick(InventoryClickEvent e) {
        if(e.getClickedInventory() == null) return;

        Player player = (Player) e.getWhoClicked();
        ItemStack clicked = e.getCurrentItem();

        if(e.getClickedInventory().getHolder() instanceof BankInventoryGUI) {
            BankInventoryGUI gui = main.getOpenedBanks().get(player.getUniqueId());

            if (clicked.isSimilar(ItemBuilder.build(SettingsYML.BankInventoryOptions.NEXT_PAGE_ICON.getString(player)
                    .replace("$page$", gui.getPage() + "")))) {
                e.setCancelled(true);
                gui.nextPage();
                return;
            }

            if (clicked.isSimilar(ItemBuilder.build(SettingsYML.BankInventoryOptions.PREVIOUS_PAGE_ICON.getString(player)
                    .replace("$page$", gui.getPage() + "")))) {
                e.setCancelled(true);
                gui.previousPage();
                return;
            }

            if(e.getClick() == ClickType.MIDDLE || e.isShiftClick()) {
                e.setCancelled(true);
                return;
            }

            // If they have an item in their cursor
            if(e.getCursor().getType() != Material.AIR) {
                e.setCancelled(true);
                return;
            }

            if(clicked.getType() != Material.AIR && NBTEditor.contains(clicked, "CurrencyId")) {
                String currency = NBTEditor.getString(clicked, "CurrencyId");

                List<ConfigPair<Integer, String>> currs = new ArrayList<>();
                currs.add(new ConfigPair<>(clicked.getAmount(), currency));

                CurrencyUtils.logTransaction(new TransactionLog(
                        player.getName(),
                        player.getName(),
                        "",
                        currs
                ).bankWithdraw(true));

                main.getSQL().subtractFromBank(player.getName(), currency, clicked.getAmount());
            }
        }
    }

    @EventHandler
    public void onDepositClick(InventoryClickEvent e) {
        if(e.getClickedInventory() == null) return;

        Player player = (Player) e.getWhoClicked();
        ItemStack clicked = e.getCurrentItem();

        if(e.getClickedInventory().getHolder() instanceof DepositGUI
                || (e.getView().getBottomInventory().equals(e.getClickedInventory())
                && e.getView().getTopInventory().getHolder() instanceof DepositGUI)
        ) {
            HashMap<String, Integer> toDeposit = new HashMap<>();

            if(e.getCurrentItem().getType() == Material.AIR) return;

            // Check if they clicked on the deposit icon
            if(clicked.isSimilar(SettingsYML.BankOptions.ITEMS.getItem("d"))) {
                e.setCancelled(true);
                boolean isEmpty = true;

                for(int i = 0; i < 27; i++) {
                    if(e.getClickedInventory().getContents()[i] != null) {
                        isEmpty = false;
                        break;
                    }
                }

                if(isEmpty) {
                    player.sendMessage(MessagesYML.Errors.NO_STORED_CURRENCY.withPrefix(player));
                    return;
                }

                for(int i = 0; i < e.getClickedInventory().getContents().length; i++) {
                    ItemStack item = e.getClickedInventory().getContents()[i];

                    if(item == null) continue;

                    if(NBTEditor.contains(item, "CurrencyId")) {
                        String currency = NBTEditor.getString(item, "CurrencyId");

                        if(!toDeposit.containsKey(currency)) toDeposit.put(currency, 0);
                        toDeposit.put(currency, toDeposit.get(currency) + item.getAmount());
                    } else if(NBTEditor.contains(item, "WalletRows")) {
                        // Empty wallet into the bank
                        boolean walletEmpty = true;

                        for(String currency : Economy.Currency.getIDs()) {
                            int currWallet = NBTEditor.getInt(item, currency);

                            if(currWallet > 0) {
                                walletEmpty = false;

                                if(!toDeposit.containsKey(currency)) toDeposit.put(currency, currWallet);
                                else toDeposit.put(currency, toDeposit.get(currency) + NBTEditor.getInt(item, currency));
                            }
                        }

                        if(walletEmpty) {
                            player.getInventory().addItem(item);
                            e.getClickedInventory().setItem(i, null);
                        } else {
                            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "ascurr item " + player.getName() + " wallet 1");
                        }
                    }
                }

                StringBuilder currencies = new StringBuilder();

                int index = 0;
                for(Map.Entry<String, Integer> entry : toDeposit.entrySet()) {
                    index++;
                    String currency = entry.getKey();
                    int amount = entry.getValue();

                    main.getSQL().addToBank(player.getName(), currency, amount);

                    currencies.append(MessagesYML.CURRENCY_FORMAT.color(player)
                            .replace("$amount$", amount + "")
                            .replace("$currency$", MessagesYML.Currencies.OPTIONS.getString(currency))
                    );

                    if(index != toDeposit.size()) currencies.append(MessagesYML.CURRENCY_SEPARATOR.color(player));
                }

                if(!toDeposit.isEmpty()) {
                    List<ConfigPair<Integer, String>> currs = new ArrayList<>();
                    toDeposit.forEach((curr, amount) -> currs.add(new ConfigPair<>(amount, curr)));

                    CurrencyUtils.logTransaction(new TransactionLog(
                            player.getName(),
                            player.getName(),
                            "",
                            currs
                    ).bankDeposit(true));
                }

                /*

                When a player closes the bank with money inside, without sending the money
                they can lose it, so there is a system that gives them the money back on close.

                Though, in this case they sent the money, so we'll remove it from the bank GUI
                to prevent duping.

                */
                for(int i = 0; i < 27; i++) e.getClickedInventory().setItem(i, null);

                e.setCurrentItem(null);

                Bukkit.getScheduler().scheduleSyncDelayedTask(main, player::closeInventory, 3);

                if(!toDeposit.isEmpty()) {
                    player.sendMessage(MessagesYML.CURRENCY_SAVED.withPrefix(player)
                            .replace("$currencies$", Utils.color(currencies.toString())));

                    SettingsYML.SFX.SUCCESSFUL_ACTION.playSoundFor(player);
                }
            } else if(!NBTEditor.contains(clicked, "CurrencyId") && !NBTEditor.contains(clicked, "WalletRows")) {
                e.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onWalletClick(InventoryClickEvent e) {
        if(e.getClickedInventory() == null) return;

        Player player = (Player) e.getWhoClicked();
        ItemStack cursor = e.getCursor();
        ItemStack clicked = e.getCurrentItem();

        if(e.getClickedInventory().getHolder() instanceof WalletGUI
                || (e.getView().getTopInventory().getHolder() instanceof WalletGUI
                && e.getClickedInventory().equals(e.getView().getBottomInventory()))
        ) {
            if(e.getClick() == ClickType.MIDDLE || e.getClick() == ClickType.RIGHT) {
                e.setCancelled(true);
                return;
            }

            String clickedCurr = clicked.getType() != Material.AIR && NBTEditor.contains(clicked, "CurrencyId")
                    ? NBTEditor.getString(clicked, "CurrencyId") : "";
            String cursorCurr = cursor.getType() != Material.AIR && NBTEditor.contains(cursor, "CurrencyId")
                    ? NBTEditor.getString(cursor, "CurrencyId") : "";

            boolean isAdding = cursor.getType() != Material.AIR
                    && (clicked.getType() == Material.AIR ||
                    (clicked.getType() != Material.AIR && clickedCurr.equals(cursorCurr)));

            boolean isSubtracting = cursor.getType() == Material.AIR && clicked.getType() != Material.AIR;

            boolean clickedInWallet = e.getView().getTopInventory().equals(e.getClickedInventory());

            boolean isShiftClicking = clicked.getType() != Material.AIR && e.isShiftClick();

            if (((isSubtracting || isShiftClicking) && clickedInWallet)) {
                if (!clickedCurr.isEmpty()) {
                    int subtract = Math.max(wallets.get(player.getUniqueId()).get(clickedCurr) - clicked.getAmount(), 0);

                    if (!walletMovementCooldown.contains(player.getUniqueId())) {
                        List<ConfigPair<Integer, String>> currs = new ArrayList<>();
                        currs.add(new ConfigPair<>(clicked.getAmount(), clickedCurr));

                        CurrencyUtils.logTransaction(new TransactionLog(
                                player.getName(),
                                player.getName(),
                                "",
                                currs
                        ).walletOut(true));

                        wallets.get(player.getUniqueId()).put(clickedCurr, subtract);

                        walletMovementCooldown.add(player.getUniqueId());
                        Bukkit.getScheduler().scheduleSyncDelayedTask(main, () -> {
                            walletMovementCooldown.remove(player.getUniqueId());
                        }, 10);
                    } else {
                        player.sendMessage(MessagesYML.Errors.TOO_FAST.withPrefix(player));
                        e.setCancelled(true);
                    }
                }
            } else if ((isAdding && clickedInWallet) || isShiftClicking) {
                String targetedCurr = (isAdding && clickedInWallet) ? cursorCurr : clickedCurr;

                if(targetedCurr.isEmpty()) {
                    e.setCancelled(true);
                    return;
                }

                int amount = wallets.get(player.getUniqueId()).get(targetedCurr);
                int addend = (isAdding && clickedInWallet) ? cursor.getAmount() : clicked.getAmount();

                if(e.isShiftClick() && CurrencyUtils.getItemSpace(e.getView().getTopInventory(), clicked) == 0) {
                    e.setCancelled(true);
                    return;
                }

                if(e.isShiftClick() && CurrencyUtils.getItemSpace(e.getView().getTopInventory(), clicked) < clicked.getAmount()) {
                    addend = CurrencyUtils.getItemSpace(e.getView().getTopInventory(), clicked);
                }

                if (!walletMovementCooldown.contains(player.getUniqueId())) {
                    List<ConfigPair<Integer, String>> currs = new ArrayList<>();
                    currs.add(new ConfigPair<>(addend, targetedCurr));

                    CurrencyUtils.logTransaction(new TransactionLog(
                            player.getName(),
                            player.getName(),
                            "",
                            currs
                    ).walletIn(true));

                    wallets.get(player.getUniqueId()).put(targetedCurr, amount + addend);

                    walletMovementCooldown.add(player.getUniqueId());
                    Bukkit.getScheduler().scheduleSyncDelayedTask(main, () -> {
                        walletMovementCooldown.remove(player.getUniqueId());
                    }, 10);
                } else {
                    player.sendMessage(MessagesYML.Errors.TOO_FAST.withPrefix(player));
                    e.setCancelled(true);
                }
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
                SettingsYML.SFX.CANCEL_INPUT.playSoundFor(player);
                return;
            }

            ShopItem shopItem = targetedItem.get(player.getUniqueId());

            try {
                int amount = Integer.parseInt(message);

                if(amount < 0) {
                    player.sendMessage(MessagesYML.Errors.NO_NEGATIVE.withPrefix(player));
                    return;
                }

                if((shopItem.getIcon().getMaxStackSize() == 1 || !shopItem.isStackable()) && amount > 1) {
                    player.sendMessage(MessagesYML.Errors.UNSTACKABLE_ITEM.withPrefix(player));
                    return;
                }

                int space = CurrencyUtils.getItemSpace(player.getInventory(), shopItem.getIcon());

                if(amount > space) {
                    player.sendMessage(MessagesYML.Errors.NO_SPACE.withPrefix(player));
                    return;
                }

                boolean oneEmpty = false;

                for(ItemStack is : player.getInventory().getContents()) {
                    if(is == null) {
                        oneEmpty = true;
                        break;
                    }
                }

                if(!oneEmpty) {
                    player.sendMessage(MessagesYML.Errors.REQUIRE_ONE_SLOT.withPrefix(player));
                    return;
                }

                if(!CurrencyUtils.withdrawMultipliedCosts(player, shopItem.getPrices(), amount)) {
                    chatEvent.remove(player.getUniqueId());
                    targetedItem.remove(player.getUniqueId());
                    return;
                } else {
                    List<ConfigPair<Integer, String>> currs = new ArrayList<>();
                    shopItem.getPrices().forEach(pair -> {
                        currs.add(new ConfigPair<>(pair.getKey() * amount, pair.getValue()));
                    });

                    CurrencyUtils.logTransaction(new TransactionLog(
                            player.getName(),
                            "",
                            "transaction in shop '" + shopItem.getCategory() + "'",
                            currs
                    ).currenciesWithdraw(true));
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

                SettingsYML.SFX.SUCCESSFUL_ACTION.playSoundFor(player);
            } catch(NumberFormatException ne) {
                player.sendMessage(MessagesYML.Errors.NO_NUMBER.withPrefix(player));
            }
        }
    }

    @EventHandler(priority = MONITOR)
    public void onCommand(PlayerCommandPreprocessEvent e) {
        String command = e.getMessage();
        Player player = e.getPlayer();

        // Override Essentials command
        if(command.equalsIgnoreCase("/repair")) {
            e.setCancelled(true);

            ItemStack inHand = Utils.usesVersionBetween("1.4.x", "1.8.x")
                    ? player.getInventory().getItemInHand()
                    : player.getInventory().getItemInMainHand();

            if(inHand == null || inHand.getType() == Material.AIR) {
                player.sendMessage(MessagesYML.Errors.NO_HAND_ITEM.withPrefix(player));
                return;
            }

            if(NBTEditor.contains(inHand, "GadgetName")) {
                String name = NBTEditor.getString(inHand, "GadgetName");

                if(Bukkit.getPluginManager().isPluginEnabled("AtomGadgets")) {
                    if(SettingsYML.RepairCosts.OPTIONS.isGadgetReplenishable(name)) {
                        if(!inHand.hasItemMeta()) return;
                        if(!inHand.getItemMeta().hasLore()) return;

                        FileConfiguration gadgetMsg = Utils.getConfigOf("AtomGadgets", "messages");
                        FileConfiguration gadgetSett = Utils.getConfigOf("AtomGadgets", "settings");

                        int uses = NBTEditor.getInt(inHand, "uses");
                        int given = SettingsYML.RepairCosts.OPTIONS.getGivenUses(name);
                        int maxUses = gadgetSett.getInt(name + ".max-uses");

                        if(uses + given == maxUses + given) {
                            player.sendMessage(MessagesYML.Errors.MAX_USES.withPrefix(player));
                            return;
                        }

                        int newUses = Math.min(uses + given, maxUses);

                        if(CurrencyUtils.withdrawCosts(player, SettingsYML.RepairCosts.OPTIONS.getGadgetCosts(name), true).isEmpty()) return;

                        ItemMeta im = inHand.getItemMeta();
                        List<String> newLore = im.getLore();
                        String usesLine = gadgetMsg.getString("lore-additions.uses");

                        for(String line : inHand.getItemMeta().getLore()) {
                            if(line.startsWith(Utils.color(usesLine.replace("$uses$", "")))) {
                                int index = newLore.indexOf(line);

                                newLore.set(index, Utils.color(usesLine.replace("$uses$", newUses + "")));
                                break;
                            }
                        }

                        im.setLore(newLore);
                        inHand.setItemMeta(im);

                        if(Utils.usesVersionBetween("1.4.x", "1.8.x")) {
                            player.getInventory().setItemInHand(NBTEditor.set(inHand, newUses, "uses"));
                        } else {
                            player.getInventory().setItemInMainHand(NBTEditor.set(inHand, newUses, "uses"));
                        }

                        player.sendMessage(MessagesYML.REPAIRED.withPrefix(player));
                        return;
                    }
                }
            }

            short newDur = (short) (inHand.getDurability() - SettingsYML.RepairCosts.OPTIONS.getGivenDurability());

            if(inHand.getDurability() == 0) {
                player.sendMessage(MessagesYML.Errors.MAX_DURABILITY.withPrefix(player));
                return;
            }

            if(newDur < 0) {
                player.sendMessage(MessagesYML.Errors.NOT_BROKEN_ENOUGH.withPrefix(player)
                        .replace("$durability$", SettingsYML.RepairCosts.OPTIONS.getGivenDurability() + ""));
                return;
            }

            if(CurrencyUtils.withdrawCosts(player, SettingsYML.RepairCosts.OPTIONS.getDurabilityCosts(), true).isEmpty()) return;

            inHand.setDurability(newDur);
            player.sendMessage(MessagesYML.REPAIRED.withPrefix(player));
        }
    }
}
