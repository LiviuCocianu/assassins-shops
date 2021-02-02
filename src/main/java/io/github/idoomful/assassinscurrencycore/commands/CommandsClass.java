package io.github.idoomful.assassinscurrencycore.commands;

import io.github.bananapuncher714.nbteditor.NBTEditor;
import io.github.idoomful.assassinscurrencycore.DMain;
import io.github.idoomful.assassinscurrencycore.configuration.MessagesYML;
import io.github.idoomful.assassinscurrencycore.configuration.SettingsYML;
import io.github.idoomful.assassinscurrencycore.configuration.ShopItem;
import io.github.idoomful.assassinscurrencycore.gui.ItemBuilder;
import io.github.idoomful.assassinscurrencycore.gui.inventories.BankGUI;
import io.github.idoomful.assassinscurrencycore.gui.inventories.BankInventoryGUI;
import io.github.idoomful.assassinscurrencycore.gui.inventories.ShopGUI;
import io.github.idoomful.assassinscurrencycore.utils.CurrencyUtils;
import io.github.idoomful.assassinscurrencycore.utils.Economy;
import io.github.idoomful.assassinscurrencycore.utils.Utils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
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
                                    ItemStack item = ItemBuilder.build(SettingsYML.WalletOptions.ITEM.getString(target));
                                    item.setAmount(amount);

                                    item = NBTEditor.set(item, SettingsYML.WalletOptions.DEFAULT_ROWS.getInt(), "WalletRows");
                                    for(String curr : Economy.Currency.getIDs()) {
                                        item = NBTEditor.set(item, 0, curr);
                                    }

                                    target.getInventory().addItem(item);

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

                            if (args[2].equalsIgnoreCase("up")) {
                                CurrencyUtils.convert(target, CurrencyUtils.ConvertWay.UP);
                            } else if (args[2].equalsIgnoreCase("down")) {
                                CurrencyUtils.convert(target, CurrencyUtils.ConvertWay.DOWN);
                            }
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

                                if(result) new BankGUI(target);
                                else {
                                    if(player instanceof Player && player.getName().equalsIgnoreCase(args[1])) {
                                        player.sendMessage(MessagesYML.CREATING_BANK.withPrefix((Player) player));
                                        createEntry(player.getName());

                                        new BankGUI(target);
                                        return;
                                    }

                                    player.sendMessage(MessagesYML.Errors.NO_BANK.withPrefix(null));
                                }
                            });
                        } else if(args.length == 1 && player instanceof Player) {
                            Player pl = (Player) player;

                            plugin.getSQL().exists(pl.getName(), result -> {
                                if(!result) {
                                    player.sendMessage(MessagesYML.CREATING_BANK.withPrefix(pl));
                                    createEntry(player.getName());
                                }

                                new BankGUI(pl);
                            });
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

                                if(result) plugin.getOpenedBanks().put(target.getUniqueId(), new BankInventoryGUI(target));
                                else {
                                    if(player instanceof Player && player.getName().equalsIgnoreCase(args[1])) {
                                        player.sendMessage(MessagesYML.CREATING_BANK.withPrefix((Player) player));
                                        createEntry(player.getName());

                                        plugin.getOpenedBanks().put(target.getUniqueId(), new BankInventoryGUI(target));
                                        return;
                                    }

                                    player.sendMessage(MessagesYML.Errors.NO_BANK.withPrefix(null));
                                }
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
            }
        });
    }

    private void createEntry(String player) {
        LinkedHashMap<String, Integer> bank = new LinkedHashMap<>();
        Economy.Currency.getIDs().forEach(curr -> bank.put(curr, 0));
        plugin.getSQL().addEntry(player, bank);
    }
}