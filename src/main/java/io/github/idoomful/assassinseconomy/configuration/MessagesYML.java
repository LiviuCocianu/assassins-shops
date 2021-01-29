package io.github.idoomful.assassinseconomy.configuration;

import io.github.idoomful.assassinseconomy.DMain;
import io.github.idoomful.assassinseconomy.gui.ItemBuilder;
import io.github.idoomful.assassinseconomy.utils.Utils;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public enum MessagesYML {
    PREFIX("prefix"),
    RELOAD("reload"),
    CATEGORIES("categories"),
    CURRENCIES("currencies"),
    SPECIFY_AMOUNT("specify-amount"),
    CANCEL_AMOUNT("cancel-amount"),
    CONVERTED_UP("converted-up"),
    CONVERTED_DOWN("converted-down"),
    CREATING_BANK("creating-bank"),
    CURRENCY_FORMAT("currency-format"),
    CURRENCY_SEPARATOR("currency-separator"),
    CURRENCY_SAVED("currency-saved"),
    REPAIRED("repaired"),
    CURRENCY_SET("currency-set");

    String output;
    FileConfiguration messages;

    MessagesYML(String output) {
        messages = DMain.getInstance().getConfigs().getFile("messages");
        this.output = "messages." + output;
    }

    public String withPrefix(Player player) {
        String text = MessagesYML.PREFIX.color(player) + messages.getString(output);
        return Utils.placeholder(player, text);
    }

    public String color(Player player) {
        String text = messages.getString(output);
        return Utils.placeholder(player, text);
    }

    public void reload() {
        messages = DMain.getInstance().getConfigs().getFile("messages");
    }

    public enum Currencies {
        OPTIONS;

        public String path;
        public FileConfiguration messages;

        Currencies() {
            this.path = "currencies";
            this.messages = MessagesYML.RELOAD.messages;
        }

        public List<String> getIDs() {
            return new ArrayList<>(messages.getConfigurationSection(path).getKeys(false));
        }

        public boolean hasID(String id) {
            return getIDs().contains(id);
        }

        public String getString(String id) {
            if(hasID(id)) return Utils.color(messages.getString(path + "." + id));
            return "";
        }
    }

    public enum Scales {
        THOUSAND("thousand"),
        MILLION("million"),
        BILLION("billion"),
        TRILLION("trillion"),
        QUADRILLION("quadrillion"),
        QUINTILLION("quintillion"),
        SEXTILLION("sextillion"),
        SEPTILLION("septillion"),
        OCTILLION("octillion"),
        NONILLION("nonillion"),
        DECILLION("decillion");

        String output;
        FileConfiguration messages;

        Scales(String output) {
            messages = MessagesYML.PREFIX.messages;
            this.output = "scales." + output;
        }

        public String color() {
            return messages.getString(output);
        }
    }

    public enum Errors {
        NO_PERMISSION("no-permission"),
        NOT_ONLINE("not-online"),
        ITEM_INVALID_CURRENCY("item-invalid-currency"),
        INVALID_CURRENCY("invalid-currency"),
        INVALID_SHOP("invalid-shop"),
        NO_NUMBER("no-number"),
        NO_NUMBER_SPECIFIED("no-number-specified"),
        TRANSACTION_ERROR("transaction-error"),
        WRONG_ARGUMENT_COUNT("wrong-argument-count"),
        NO_NEGATIVE("no-negative"),
        UNSTACKABLE_ITEM("unstackable-item"),
        NO_CONVERTIBLES("no-convertibles"),
        NOT_ENOUGH_SPACE("not-enough-space"),
        NO_BANK("no-bank"),
        NO_HAND_ITEM("no-hand-item"),
        MAX_USES("max-uses"),
        MAX_DURABILITY("max-durability"),
        NOT_BROKEN_ENOUGH("not-broken-enough"),
        TOOK_TOO_LONG("took-too-long");

        String output;
        FileConfiguration messages;

        Errors(String output) {
            messages = MessagesYML.PREFIX.messages;
            this.output = "errors." + output;
        }

        public String withPrefix(Player player) {
            String text = MessagesYML.PREFIX.color(player) + messages.getString(output);
            return Utils.placeholder(player, text);
        }

        public String color(Player player) {
            String text = messages.getString(output);
            return Utils.placeholder(player, text);
        }
    }

    public enum Lists {
        HELP("help");

        String output;
        FileConfiguration messages;

        Lists(String output) {
            messages = MessagesYML.PREFIX.messages;
            this.output = "lists." + output;
        }

        public List<String> getStringList(Player player) {
            return Utils.placeholder(player, messages.getStringList(output));
        }
    }
}
