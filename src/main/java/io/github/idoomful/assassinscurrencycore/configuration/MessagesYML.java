package io.github.idoomful.assassinscurrencycore.configuration;

import io.github.idoomful.assassinscurrencycore.DMain;
import io.github.idoomful.assassinscurrencycore.utils.Utils;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

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
    DELETED_BANK("deleted-bank"),
    CURRENCY_FORMAT("currency-format"),
    CURRENCY_SEPARATOR("currency-separator"),
    CURRENCY_SAVED("currency-saved"),
    REPAIRED("repaired"),
    CURRENCY_SET("currency-set"),
    CURRENCY_TAKEN("currency-taken"),
    GIVEN_ITEM("given-item");

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
        NEVER_PLAYED("never-played"),
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
        TOOK_TOO_LONG("took-too-long"),
        NO_SPACE("no-space"),
        REQUIRE_ONE_SLOT("require-one-slot"),
        NO_STORED_CURRENCY("no-stored-currency"),
        COOLDOWN("cooldown"),
        TOO_FAST("too-fast"),
        UNREPAIRABLE("unrepairable"),
        CURRENCY_CANNOT_USE_ENDERCHEST("currency-cannot-use-enderchest"),
        PREVENT_ITEM_LOSS("prevent-item-loss");

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
