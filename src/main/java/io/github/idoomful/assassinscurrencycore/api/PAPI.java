package io.github.idoomful.assassinscurrencycore.api;

import io.github.idoomful.assassinscurrencycore.DMain;
import io.github.idoomful.assassinscurrencycore.utils.CurrencyUtils;
import io.github.idoomful.assassinscurrencycore.utils.Economy;
import io.github.idoomful.assassinscurrencycore.utils.Utils;
import jdk.nashorn.internal.runtime.regexp.joni.Regex;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedHashMap;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

public class PAPI extends PlaceholderExpansion {
    private final DMain plugin;

    public PAPI(DMain main) {
        plugin = main;
    }

    @Override
    public boolean persist(){
        return true;
    }

    @Override
    public boolean canRegister(){
        return true;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "acurrency";
    }

    @Override
    public @NotNull String getAuthor() {
        return "iDoomful";
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getVersion();
    }

    @Override
    public String onRequest(OfflinePlayer player, @NotNull String identifier) {
        String pl = player.getName();
        String phCurrency = "";

        String[] subs = {
                "currency", "curr", "c", "currencymix", "currmix", "mix", "m",
                "currencymixd", "currmixd", "mixd", "md"
        };

        for(String sub : subs) {
            if(identifier.startsWith(sub + "_")) {
                phCurrency = identifier.replaceFirst("_", "*").split("\\*")[1];

                // Handle placeholders of this format: acurrency_c_gold@iDoomful
                if(identifier.contains("@")) {
                    pl = phCurrency.split("@")[1];
                    phCurrency = phCurrency.split("@")[0];
                }
                break;
            }
        }

        if(identifier.startsWith("currency_") || identifier.startsWith("curr_") || identifier.startsWith("c_")) {
            AtomicReference<LinkedHashMap<String, Integer>> inv = new AtomicReference<>(null);

            String finalPl = pl;
            plugin.getSQL().exists(pl, res -> {
                if(res) plugin.getSQL().getBankInventory(finalPl, inv::set);
            });

            if(inv.get() == null || !inv.get().containsKey(phCurrency)) return "0";

            return Utils.formatNumber(inv.get().get(phCurrency), false);
        }

        if(identifier.startsWith("currencymix_") || identifier.startsWith("currmix_") || identifier.startsWith("mix_") || identifier.startsWith("m_")) {
            return mixCurrencies(pl, phCurrency, false);
        }

        if(identifier.startsWith("currencymixd_") || identifier.startsWith("currmixd_") || identifier.startsWith("mixd_") || identifier.startsWith("md_")) {
            return mixCurrencies(pl, phCurrency, true);
        }

        if(identifier.startsWith("playercurrency_")) {
            phCurrency = identifier.replaceFirst("_", "*").split("\\*")[1];
            if(player.isOnline()) return CurrencyUtils.getCurrencyAmount(phCurrency, player.getPlayer().getInventory()) + "";
            else return "0";
        }

        return null;
    }

    private String mixCurrencies(String player, String phCurr, boolean includeDecimals) {
        AtomicReference<LinkedHashMap<String, Integer>> bank = new AtomicReference<>(null);

        plugin.getSQL().exists(player, res -> {
            if(res) plugin.getSQL().getBankInventory(player, bank::set);
        });

        if(bank.get() == null) return "0";

        boolean isSuperior = false;

        double mix = 0;
        int worth = Economy.Worth.getWorthAbove(Economy.Currency.getIDs().get(0), 1);
        double worthAcc = worth;

        if (!phCurr.equals(Economy.Currency.getIDs().get(0))) {
            for (String currency : Economy.Currency.getIDs()) {
                if (phCurr.equals(currency)) break;
                if (currency.equals(Economy.Currency.getIDs().get(0))) continue;

                int above = Economy.Worth.getWorthAbove(Economy.Currency.getIDs().get(0), 2);
                above = above == -1 ? 1 : above;

                worthAcc *= above;
            }
        } else {
            worthAcc = 1;
        }

        for (String currency : Economy.Currency.getIDs()) {
            double bankAmount = bank.get().get(currency);

            if (!isSuperior && !phCurr.equals(currency)) mix += bankAmount / worthAcc;
            else mix += bankAmount * worthAcc;

            worth = Economy.Worth.getWorthAbove(currency, 1) != -1
                    ? Economy.Worth.getWorthAbove(currency, 1)
                    : 1;

            if(phCurr.equals(currency)) {
                int fetch = Objects.requireNonNull(Economy.Worth.getWorth(currency)).getKey();

                if(fetch == -1) worthAcc = Economy.Worth.getWorthAbove(currency, 1);
                else worthAcc = Objects.requireNonNull(Economy.Worth.getWorth(currency)).getKey();

                isSuperior = true;
            }

            else if(!isSuperior) worthAcc /= worth;
            else worthAcc *= worth;
        }

        String format = Utils.formatNumber((float) mix, includeDecimals);

        float decimal = (float) (mix - ((int) mix));

        return decimal > 0 && !includeDecimals ? ("~" + format) : format;
    }
}
