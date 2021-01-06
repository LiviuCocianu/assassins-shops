package io.github.idoomful.assassinseconomy.commands;

import io.github.bananapuncher714.nbteditor.NBTEditor;
import io.github.idoomful.assassinseconomy.DMain;
import io.github.idoomful.assassinseconomy.configuration.MessagesYML;
import io.github.idoomful.assassinseconomy.configuration.SettingsYML;
import io.github.idoomful.assassinseconomy.configuration.ShopItem;
import io.github.idoomful.assassinseconomy.gui.ShopGUI;
import io.github.idoomful.assassinseconomy.utils.CurrencyUtils;
import io.github.idoomful.assassinseconomy.utils.Utils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class CommandsClass {
    public CommandsClass(DMain plugin) {
        final String pluginName = plugin.getDescription().getName();
        final String pluginNameLower = pluginName.toLowerCase();

        // Command initialization
        final CommandSettings settings = new CommandSettings(pluginNameLower)
                .setPermissionMessage(MessagesYML.Errors.NO_PERMISSION.withPrefix(null))
                .setAliases(Utils.Array.of("shop", "asshop"));

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
                case "currencies":
                    if(player.hasPermission(pluginNameLower + ".command.currencies")) {
                        List<String> results = SettingsYML.Currencies.OPTIONS.getIDs();
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

            if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
                MessagesYML.Lists.HELP.getStringList(arg).forEach(player::sendMessage);
                return;
            }

            if(!player.hasPermission(pluginNameLower + ".command")) {
                player.sendMessage(MessagesYML.Errors.NO_PERMISSION.withPrefix(arg));
                return;
            }

            switch(args[0]) {
                case "give":
                    if (player.hasPermission(pluginNameLower + ".command.give")) {
                        if(args.length >= 3) {
                            if (!Bukkit.getPlayer(args[1]).isOnline()) {
                                player.sendMessage(MessagesYML.Errors.NOT_ONLINE.withPrefix(arg));
                                return;
                            }

                            Player target = Bukkit.getPlayer(args[1]);

                            if (SettingsYML.Currencies.OPTIONS.hasID(args[2])) {
                                try {
                                    int amount = args.length == 4 ? Integer.parseInt(args[3]) : 1;
                                    target.getInventory().addItem(
                                            SettingsYML.Currencies.OPTIONS.getMarkedItem(args[2], amount)
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
                    if(player instanceof Player) {
                        if (player.hasPermission(pluginNameLower + ".command.convert")) {
                            if(args.length == 2) {
                                if(args[1].equalsIgnoreCase("up")) {
                                    CurrencyUtils.convert((Player) player, CurrencyUtils.ConvertWay.UP);
                                } else if(args[1].equalsIgnoreCase("down")) {
                                    CurrencyUtils.convert((Player) player, CurrencyUtils.ConvertWay.DOWN);
                                }
                            } else {
                                player.sendMessage(MessagesYML.Errors.WRONG_ARGUMENT_COUNT.withPrefix(arg));
                            }
                        } else {
                            player.sendMessage(MessagesYML.Errors.NO_PERMISSION.withPrefix(arg));
                        }
                    }
                    break;
            }
        });
    }
}