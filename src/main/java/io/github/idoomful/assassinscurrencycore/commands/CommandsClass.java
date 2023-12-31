package io.github.idoomful.assassinscurrencycore.commands;

import io.github.bananapuncher714.nbteditor.NBTEditor;
import io.github.idoomful.assassinscurrencycore.DMain;
import io.github.idoomful.assassinscurrencycore.configuration.MessagesYML;
import io.github.idoomful.assassinscurrencycore.configuration.SettingsYML;
import io.github.idoomful.assassinscurrencycore.configuration.ShopItem;
import io.github.idoomful.assassinscurrencycore.data.SQL.TransactionLog;
import io.github.idoomful.assassinscurrencycore.gui.ItemBuilder;
import io.github.idoomful.assassinscurrencycore.gui.inventories.DepositGUI;
import io.github.idoomful.assassinscurrencycore.gui.inventories.BankInventoryGUI;
import io.github.idoomful.assassinscurrencycore.gui.inventories.ShopGUI;
import io.github.idoomful.assassinscurrencycore.utils.ConfigPair;
import io.github.idoomful.assassinscurrencycore.utils.CurrencyUtils;
import io.github.idoomful.assassinscurrencycore.utils.Economy;
import io.github.idoomful.assassinscurrencycore.utils.Utils;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

public class CommandsClass {
    private final DMain plugin;

    public CommandsClass(DMain plugin) {
        this.plugin = plugin;

        final String pluginName = plugin.getDescription().getName();
        final String pluginNameLower = pluginName.toLowerCase();

        // Command initialization
        final CommandSettings settings = new CommandSettings(pluginNameLower)
                .setPermissionMessage(MessagesYML.Errors.NO_PERMISSION.withPrefix(null))
                .setAliases(Utils.Array.of("assassinscurrencycore", "ascurr"));

        new ModularCommand(settings, (player, args) -> {
            Player arg = player instanceof Player ? (Player) player : null;

            if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
                MessagesYML.Lists.HELP.getStringList(arg).forEach(player::sendMessage);
                return;
            }

            if(!player.hasPermission(pluginNameLower + ".command")) {
                player.sendMessage(MessagesYML.Errors.NO_PERMISSION.withPrefix(arg));
                return;
            }

            switch(args[0]) {
                case "removeentry":
                case "deleteentry":
                    if(player.hasPermission(pluginNameLower + ".command.removeentry")
                    || player.hasPermission(pluginNameLower + ".command.deleteentry")) {
                        if(args.length == 2) {
                            AtomicReference<List<String>> playersInDB = new AtomicReference<>();
                            plugin.getSQL().getPlayers(playersInDB::set);

                            if(!playersInDB.get().contains(args[1])) {
                                player.sendMessage(MessagesYML.Errors.NO_BANK.withPrefix(arg));
                                return;
                            }

                            plugin.getSQL().removeEntry(args[1]);
                            player.sendMessage(MessagesYML.DELETED_BANK.withPrefix(arg).replace("$player$", args[1]));
                        } else {
                            player.sendMessage(MessagesYML.Errors.WRONG_ARGUMENT_COUNT.withPrefix(arg));
                        }
                    } else {
                        player.sendMessage(MessagesYML.Errors.NO_PERMISSION.withPrefix(arg));
                    }
                    break;
                case "item":
                    if(player.hasPermission(pluginNameLower + ".command.item")) {
                        if(args.length == 3 || args.length == 4) {
                            if(Bukkit.getPlayer(args[1]) == null) {
                                player.sendMessage(MessagesYML.Errors.NOT_ONLINE.withPrefix(arg));
                                return;
                            }

                            try {
                                Player target = Bukkit.getPlayer(args[1]);
                                String id = args[2];
                                int amount = args.length == 4 ? Integer.parseInt(args[3]) : 1;
                                amount = Math.min(amount, 64);

                                if(id.equalsIgnoreCase("wallet")) {
                                    int cooldown = SettingsYML.WalletOptions.COOLDOWN.getInt();

                                    ItemStack item = ItemBuilder.build(SettingsYML.WalletOptions.ITEM.getString(target)
                                            .replace("$cooldown$", cooldown + "")
                                    );

                                    item.setAmount(amount);

                                    item = NBTEditor.set(item, SettingsYML.WalletOptions.ROWS.getInt(), "WalletRows");
                                    item = NBTEditor.set(item, UUID.randomUUID().toString(), "WalletId");

                                    for(String curr : Economy.Currency.getIDs()) {
                                        item = NBTEditor.set(item, 0, curr);
                                    }

                                    target.getInventory().addItem(item);

                                    Utils.updateWalletLore(target.getInventory());

                                    player.sendMessage(MessagesYML.GIVEN_ITEM.withPrefix(arg)
                                            .replace("$amount$", amount + "")
                                            .replace("$item$", SettingsYML.WalletOptions.ITEM_NAME.getString(arg))
                                            .replace("$player$", args[1])
                                    );
                                }
                            } catch(NumberFormatException ne) {
                                player.sendMessage(MessagesYML.Errors.NO_NUMBER.withPrefix(arg));
                            }
                        }
                    } else {
                        player.sendMessage(MessagesYML.Errors.NO_PERMISSION.withPrefix(arg));
                    }
                    break;
                case "currencies":
                    if(player.hasPermission(pluginNameLower + ".command.currencies")) {
                        List<String> results = Economy.Currency.getIDs();
                        String str = results.toString().replaceAll("[\\[\\]]", "");
                        player.sendMessage(MessagesYML.CURRENCIES.withPrefix(arg).replace("$currencies$", str));
                    } else {
                        player.sendMessage(MessagesYML.Errors.NO_PERMISSION.withPrefix(arg));
                    }
                    break;
                case "categories":
                    if(player.hasPermission(pluginNameLower + ".command.categories")) {
                        List<String> results = SettingsYML.Shops.OPTIONS.getCategoryIDs();
                        String str = results.toString().replaceAll("[\\[\\]]", "");
                        player.sendMessage(MessagesYML.CATEGORIES.withPrefix(arg).replace("$categories$", str));
                    } else {
                        player.sendMessage(MessagesYML.Errors.NO_PERMISSION.withPrefix(arg));
                    }
                    break;
                case "open":
                    if(player.hasPermission(pluginNameLower + ".command.open")) {
                        if(args.length == 3) {
                            if(SettingsYML.Shops.OPTIONS.hasCategory(args[1])) {
                                if(!Bukkit.getPlayer(args[2]).isOnline()) {
                                    player.sendMessage(MessagesYML.Errors.NOT_ONLINE.withPrefix(arg));
                                    return;
                                }

                                Player target = Bukkit.getPlayer(args[2]);

                                plugin.getOpenedShops().put(target.getUniqueId(), new ShopGUI(args[1], target));
                            } else {
                                player.sendMessage(MessagesYML.Errors.INVALID_SHOP.withPrefix(arg));
                            }
                        } else {
                            player.sendMessage(MessagesYML.Errors.WRONG_ARGUMENT_COUNT.withPrefix(arg));
                        }
                    } else {
                        player.sendMessage(MessagesYML.Errors.NO_PERMISSION.withPrefix(arg));
                    }
                    break;
                case "reload":
                    if(player.hasPermission(pluginNameLower + ".command.reload")) {
                        plugin.getConfigs().reloadConfigs();

                        MessagesYML.RELOAD.reload();
                        SettingsYML._OPTIONS.reload();

                        plugin.getShopCategories().clear();

                        SettingsYML.Shops.OPTIONS.getCategoryIDs().forEach(id -> {
                            List<ShopItem> items = new ArrayList<>();

                            for(String itemID : SettingsYML.Shops.OPTIONS.getCategoryItems(id)) {
                                items.add(new ShopItem(id, itemID));
                            }

                            plugin.getShopCategories().put(id, items);
                        });

                        player.sendMessage(MessagesYML.RELOAD.withPrefix(arg));
                    } else {
                        player.sendMessage(MessagesYML.Errors.NO_PERMISSION.withPrefix(arg));
                    }
                    break;
                case "version":
                case "ver":
                case "v":
                    if(player.hasPermission(pluginNameLower + ".command.version")) {
                        player.sendMessage(pluginName + " version: " + plugin.getVersion());
                    } else {
                        player.sendMessage(MessagesYML.Errors.NO_PERMISSION.withPrefix(arg));
                    }
                    break;
                default:
                    MessagesYML.Lists.HELP.getStringList(arg).forEach(player::sendMessage);
            }
        });

        final CommandSettings settings2 = new CommandSettings("currency")
                .setPermissionMessage(MessagesYML.Errors.NO_PERMISSION.withPrefix(null))
                .setAliases(Utils.Array.of("curr", "money"));

        new ModularCommand(settings2, (player, args) -> {
            Player arg = player instanceof Player ? (Player) player : null;

            if(!player.hasPermission(pluginNameLower + ".command")) {
                player.sendMessage(MessagesYML.Errors.NO_PERMISSION.withPrefix(arg));
                return;
            }

            if(args.length == 0) {
                MessagesYML.Lists.HELP.getStringList(arg).forEach(player::sendMessage);
                return;
            }

            switch(args[0]) {
                case "help":
                    MessagesYML.Lists.HELP.getStringList(arg).forEach(player::sendMessage);
                    break;
                case "give":
                    if (player.hasPermission(pluginNameLower + ".command.give")) {
                        if(args.length >= 3) {
                            if (!Bukkit.getPlayer(args[1]).isOnline()) {
                                player.sendMessage(MessagesYML.Errors.NOT_ONLINE.withPrefix(arg));
                                return;
                            }

                            Player target = Bukkit.getPlayer(args[1]);

                            if (Economy.Currency.hasID(args[2])) {
                                try {
                                    int amount = args.length == 4 ? Integer.parseInt(args[3]) : 1;

                                    List<ConfigPair<Integer, String>> currs = new ArrayList<>();
                                    currs.add(new ConfigPair<>(amount, args[2]));

                                    CurrencyUtils.logTransaction(new TransactionLog(
                                            args[1],
                                            arg != null ? arg.getName() : "CONSOLE",
                                            "given by currency core",
                                            currs
                                    ).currenciesAdded(true));

                                    target.getInventory().addItem(
                                            Economy.Currency.getMarkedItem(args[2], amount)
                                    );
                                } catch (NumberFormatException ne) {
                                    player.sendMessage(MessagesYML.Errors.NO_NUMBER.withPrefix(arg));
                                }
                            } else {
                                player.sendMessage(MessagesYML.Errors.INVALID_CURRENCY.withPrefix(arg));
                            }
                        } else {
                            player.sendMessage(MessagesYML.Errors.WRONG_ARGUMENT_COUNT.withPrefix(arg));
                        }
                    } else {
                        player.sendMessage(MessagesYML.Errors.NO_PERMISSION.withPrefix(arg));
                    }
                    break;
                case "convert":
                    if (player.hasPermission(pluginNameLower + ".command.convert")) {
                        if (args.length == 3) {
                            if(!Bukkit.getPlayer(args[1]).isOnline()) {
                                player.sendMessage(MessagesYML.Errors.NOT_ONLINE.withPrefix(arg));
                                return;
                            }

                            Player target = Bukkit.getPlayer(args[1]);

                            List<ConfigPair<Integer, String>> currs = new ArrayList<>();
                            currs.add(new ConfigPair<>(0, "unknown"));

                            String comment = "";

                            if (args[2].equalsIgnoreCase("up")) {
                                comment = "converted up";
                                CurrencyUtils.convert(target, CurrencyUtils.ConvertWay.UP);
                            } else if (args[2].equalsIgnoreCase("down")) {
                                comment = "converted down";
                                CurrencyUtils.convert(target, CurrencyUtils.ConvertWay.DOWN);
                            }

                            CurrencyUtils.logTransaction(new TransactionLog(
                                    target.getName(),
                                    arg != null ? arg.getName() : "CONSOLE",
                                    comment,
                                    currs
                            ).convert(true));
                        } else {
                            player.sendMessage(MessagesYML.Errors.WRONG_ARGUMENT_COUNT.withPrefix(arg));
                        }
                    } else {
                        player.sendMessage(MessagesYML.Errors.NO_PERMISSION.withPrefix(arg));
                    }
                    break;
                case "store":
                case "deposit":
                    if (player.hasPermission(pluginNameLower + ".command.deposit")) {
                        if(args.length == 2) {
                            plugin.getSQL().exists(args[1], result -> {
                                if (!Bukkit.getOfflinePlayer(args[1]).isOnline()) {
                                    if(player instanceof Player) {
                                        player.sendMessage(MessagesYML.Errors.NOT_ONLINE.withPrefix(arg));
                                    }
                                    return;
                                }

                                Player target = Bukkit.getPlayer(args[1]);

                                if(!result) {
                                    target.sendMessage(MessagesYML.CREATING_BANK.withPrefix(target));
                                    createEntry(target.getName());
                                }

                                new DepositGUI(target);
                            });
                        } else if(args.length == 1 && player instanceof Player) {
                            Player pl = (Player) player;

                            plugin.getSQL().exists(pl.getName(), result -> {
                                if(!result) {
                                    player.sendMessage(MessagesYML.CREATING_BANK.withPrefix(pl));
                                    createEntry(player.getName());
                                }

                                new DepositGUI(pl);
                            });
                        }
                    } else {
                        player.sendMessage(MessagesYML.Errors.NO_PERMISSION.withPrefix(arg));
                    }
                    break;
                case "bank":
                    if (player.hasPermission(pluginNameLower + ".command.bank")) {
                        if(player instanceof Player) {
                            if(args.length == 2) {
                                plugin.getSQL().exists(args[1], result -> {
                                    OfflinePlayer target = Bukkit.getOfflinePlayer(args[1]);

                                    if(!target.hasPlayedBefore()) {
                                        player.sendMessage(MessagesYML.Errors.NEVER_PLAYED.withPrefix(arg));
                                        return;
                                    }

                                    if(!result && target.hasPlayedBefore()) {
                                        player.sendMessage(MessagesYML.CREATING_BANK.withPrefix(arg));
                                        createEntry(target.getName());
                                    }

                                    plugin.getOpenedBanks().put(((Player) player).getUniqueId(), new BankInventoryGUI((Player) player, target));
                                });
                            } else {
                                player.sendMessage(MessagesYML.Errors.WRONG_ARGUMENT_COUNT.withPrefix(arg));
                            }
                        }
                    } else {
                        player.sendMessage(MessagesYML.Errors.NO_PERMISSION.withPrefix(arg));
                    }
                    break;
                case "inv":
                case "inventory":
                    if (player.hasPermission(pluginNameLower + ".command.inventory")) {
                        if(args.length == 2) {
                            plugin.getSQL().exists(args[1], result -> {
                                if (!Bukkit.getOfflinePlayer(args[1]).isOnline()) {
                                    if(player instanceof Player) {
                                        player.sendMessage(MessagesYML.Errors.NOT_ONLINE.withPrefix(arg));
                                    }
                                    return;
                                }

                                Player target = Bukkit.getPlayer(args[1]);

                                if(!result) {
                                    player.sendMessage(MessagesYML.CREATING_BANK.withPrefix(arg));
                                    createEntry(target.getName());
                                }

                                plugin.getOpenedBanks().put(target.getUniqueId(), new BankInventoryGUI(target));
                            });
                        } else if(args.length == 1 && player instanceof Player) {
                            Player pl = (Player) player;

                            plugin.getSQL().exists(pl.getName(), result -> {
                                if(!result) {
                                    player.sendMessage(MessagesYML.CREATING_BANK.withPrefix(pl));
                                    createEntry(player.getName());
                                }

                                plugin.getOpenedBanks().put(pl.getUniqueId(), new BankInventoryGUI(pl));
                            });
                        }
                    } else {
                        player.sendMessage(MessagesYML.Errors.NO_PERMISSION.withPrefix(arg));
                    }
                    break;
                case "set":
                    if (player.hasPermission(pluginNameLower + ".command.set")) {
                        if(args.length >= 3) {
                            String target = args[1];

                            if (Economy.Currency.hasID(args[2])) {
                                try {
                                    String currency = args[2];
                                    int amount = args.length == 4 ? Integer.parseInt(args[3]) : -69420;

                                    if(amount == -69420) {
                                        player.sendMessage(MessagesYML.Errors.NO_NUMBER_SPECIFIED.withPrefix(arg));
                                        return;
                                    }

                                    if(amount < 0) {
                                        player.sendMessage(MessagesYML.Errors.NO_NEGATIVE.withPrefix(arg));
                                        return;
                                    }

                                    AtomicReference<LinkedHashMap<String, Integer>> map = new AtomicReference<>();
                                    plugin.getSQL().getBankInventory(target, map::set);

                                    map.get().put(currency, amount);

                                    List<ConfigPair<Integer, String>> currs = new ArrayList<>();
                                    currs.add(new ConfigPair<>(amount, currency));

                                    CurrencyUtils.logTransaction(new TransactionLog(
                                            target,
                                            arg != null ? arg.getName() : "CONSOLE",
                                            "",
                                            currs
                                    ).currencySet(true));

                                    plugin.getSQL().setBankInventory(target, map.get());

                                    player.sendMessage(MessagesYML.CURRENCY_SET.withPrefix(arg)
                                            .replace("$currency$", currency)
                                            .replace("$player$", target)
                                            .replace("$amount$", Utils.formatNumber(amount, false))
                                    );
                                } catch (NumberFormatException ne) {
                                    player.sendMessage(MessagesYML.Errors.NO_NUMBER.withPrefix(arg));
                                }
                            } else {
                                player.sendMessage(MessagesYML.Errors.INVALID_CURRENCY.withPrefix(arg));
                            }
                        } else {
                            player.sendMessage(MessagesYML.Errors.WRONG_ARGUMENT_COUNT.withPrefix(arg));
                        }
                    } else {
                        player.sendMessage(MessagesYML.Errors.NO_PERMISSION.withPrefix(arg));
                    }
                    break;
                case "take":
                    if (player.hasPermission(pluginNameLower + ".command.take")) {
                        if(args.length >= 3) {
                            String targetName = args[1];

                            if (Economy.Currency.hasID(args[2])) {
                                try {
                                    String currency = args[2];
                                    int amount = args.length == 4 ? Integer.parseInt(args[3]) : -69420;

                                    if(amount == -69420) {
                                        player.sendMessage(MessagesYML.Errors.NO_NUMBER_SPECIFIED.withPrefix(arg));
                                        return;
                                    }

                                    if(amount <= 0) {
                                        player.sendMessage(MessagesYML.Errors.NO_NEGATIVE.withPrefix(arg));
                                        return;
                                    }

                                    if (!Bukkit.getOfflinePlayer(targetName).isOnline()) {
                                        if(player instanceof Player) player.sendMessage(MessagesYML.Errors.NOT_ONLINE.withPrefix(arg));
                                        return;
                                    }

                                    Player target = Bukkit.getPlayer(args[1]);
                                    final int inInvConst = CurrencyUtils.getCurrencyAmount(currency, target.getInventory());
                                    int amountVar = amount;

                                    if(inInvConst >= amount) {
                                        List<ConfigPair<Integer, String>> currs = new ArrayList<>();
                                        currs.add(new ConfigPair<>(amount, currency));

                                        CurrencyUtils.logTransaction(new TransactionLog(
                                                targetName,
                                                arg != null ? arg.getName() : "CONSOLE",
                                                "",
                                                currs
                                        ).currenciesWithdraw(true));

                                        for(int i = 0; i < target.getInventory().getContents().length; i++) {
                                            ItemStack item = target.getInventory().getContents()[i];

                                            if(item == null) continue;
                                            if(NBTEditor.contains(item, "CurrencyId")) {
                                                if (NBTEditor.getString(item, "CurrencyId").equalsIgnoreCase(currency)) {
                                                    // Subtract until the desired amount is reached
                                                    if(amountVar > 0) {
                                                        int aux = amountVar;
                                                        amountVar -= item.getAmount();

                                                        if (aux >= item.getAmount()) item.setAmount(0);
                                                        else item.setAmount(item.getAmount() - aux);

                                                        target.getInventory().setItem(i, item);
                                                    }
                                                }
                                            }
                                        }

                                        player.sendMessage(MessagesYML.CURRENCY_TAKEN.withPrefix(arg)
                                                .replace("$currency$", currency)
                                                .replace("$player$", targetName)
                                                .replace("$amount$", Utils.formatNumber(amount, false))
                                        );
                                    }
                                } catch (NumberFormatException ne) {
                                    player.sendMessage(MessagesYML.Errors.NO_NUMBER.withPrefix(arg));
                                }
                            } else {
                                player.sendMessage(MessagesYML.Errors.INVALID_CURRENCY.withPrefix(arg));
                            }
                        } else {
                            player.sendMessage(MessagesYML.Errors.WRONG_ARGUMENT_COUNT.withPrefix(arg));
                        }
                    } else {
                        player.sendMessage(MessagesYML.Errors.NO_PERMISSION.withPrefix(arg));
                    }
                    break;
                case "top":
                    if (player.hasPermission(pluginNameLower + ".command.top")) {
                        String currency;
                        int page = 1;

                        if(args.length >= 2) currency = args[1];
                        else currency = SettingsYML.TopOptions.DEFAULT_CURRENCY.getString(arg);

                        if(args.length >= 3) page = Integer.parseInt(args[2]);
                        page = Math.min(page, 1000);
                        page = Math.max(1, page);

                        AtomicReference<List<String>> playersInDB = new AtomicReference<>();
                        plugin.getSQL().getPlayers(playersInDB::set);

                        int databaseSize = playersInDB.get().size();

                        TreeMap<Float, String> top = new TreeMap<>(Collections.reverseOrder());

                        for(int i = 0; i < databaseSize; i++) {
                            String name = playersInDB.get().get(i);
                            float value = Float.parseFloat(PlaceholderAPI.setPlaceholders(Bukkit.getOfflinePlayer(name), "%acurrency_md_" + currency + "%"));
                            top.put(value, name);
                        }

                        final int pageLength = SettingsYML.TopOptions.PER_PAGE.getInt();

                        int start = (pageLength * page) - pageLength;

                        if(start > databaseSize) {
                            while(((pageLength * page) - pageLength) > databaseSize) page--;
                            start = (pageLength * page) - pageLength;
                        }

                        int end = (pageLength * page) - 1;

                        for(String headerLine : SettingsYML.TopOptions.LIST_HEADER.getStringList(arg)) {
                            player.sendMessage(headerLine
                                    .replace("$page$", page + "")
                                    .replace("$currency$", SettingsYML.TopOptions.CURRENCY_LANG.getCurrencyLang(currency))
                            );
                        }

                        Float[] values = top.keySet().toArray(new Float[0]);
                        String[] players = top.values().toArray(new String[0]);

                        for(int i = start; i < end; i++) {
                            List<Integer> individuals = SettingsYML.TopOptions.INDIVIDUAL_ENTRY_FORMATS.getIndividualFormats();
                            int number = i + 1;

                            if(individuals.contains(number) && players.length > i) {
                                OfflinePlayer target = Bukkit.getOfflinePlayer(players[i]);
                                player.sendMessage(SettingsYML.TopOptions.INDIVIDUAL_ENTRY_FORMATS.getIndividualFormat(target, number)
                                        .replace("$number$", number + "")
                                        .replace("$player$", players[i])
                                        .replace("$value$", values[i] + "")
                                        .replace("$currency$", SettingsYML.TopOptions.CURRENCY_LANG.getCurrencyLang(currency))
                                );
                            } else {
                                if(players.length <= i) {
                                    for(String line : SettingsYML.TopOptions.EMPTY_ENTRY_FORMAT.getStringList(arg)) {
                                        player.sendMessage(line
                                                .replace("$number$", number + "")
                                                .replace("$currency$", SettingsYML.TopOptions.CURRENCY_LANG.getCurrencyLang(currency))
                                        );
                                    }
                                } else {
                                    for(String line : SettingsYML.TopOptions.ENTRY_FORMAT.getStringList(arg)) {
                                        player.sendMessage(line
                                                .replace("$number$", number + "")
                                                .replace("$player$", players[i])
                                                .replace("$value$", values[i] + "")
                                                .replace("$currency$", SettingsYML.TopOptions.CURRENCY_LANG.getCurrencyLang(currency))
                                        );
                                    }
                                }
                            }
                        }

                        for(String headerLine : SettingsYML.TopOptions.LIST_FOOTER.getStringList(arg)) {
                            player.sendMessage(headerLine
                                    .replace("$page$", page + "")
                                    .replace("$currency$", SettingsYML.TopOptions.CURRENCY_LANG.getCurrencyLang(currency))
                            );
                        }
                    } else {
                        player.sendMessage(MessagesYML.Errors.NO_PERMISSION.withPrefix(arg));
                    }
            }
        });
    }

    private void createEntry(String player) {
        LinkedHashMap<String, Integer> bank = new LinkedHashMap<>();
        Economy.Currency.getIDs().forEach(curr -> bank.put(curr, 0));
        plugin.getSQL().addEntry(player, bank);
    }
}