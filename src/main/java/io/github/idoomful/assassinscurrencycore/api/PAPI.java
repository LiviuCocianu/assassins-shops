package io.github.idoomful.assassinscurrencycore.api;

import io.github.idoomful.assassinscurrencycore.DMain;
import io.github.idoomful.assassinscurrencycore.utils.Economy;
import io.github.idoomful.assassinscurrencycore.utils.Utils;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedHashMap;
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
                if(res) plugin.getSQL().getCurrencies(finalPl, inv::set);
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

        return null;
    }

    private String mixCurrencies(String player, String phCurr, boolean includeDecimals) {
        AtomicReference<LinkedHashMap<String, Integer>> bank = new AtomicReference<>(null);

        plugin.getSQL().exists(player, res -> {
            if(res) plugin.getSQL().getCurrencies(player, bank::set);
        });

        if(bank.get() == null) return "0";

        float mixed = 0;
        float inferior = 1, superior = 0, superiorMult = 1;
        boolean isSuperior = false;

        int index = 0;
        for (String currency : Economy.Currency.getIDs()) {
            int inBank = bank.get().get(currency);

            if (phCurr.equals(currency)) {
                mixed += inBank;
                isSuperior = true;
                index += 100;
                continue;
            }

            if (index == 0) inferior = inBank;
            if (index >= 100) {
                superiorMult = inBank == 0 ? 1 : inBank;
                index -= 99;
            }

            int worth = Economy.Worth.getWorthAbove(currency, 1);
            worth = worth == 0 ? 1 : worth;

            // Get actual worth for the biggest currency
            if((index == Economy.Currency.getIDs().size() - 1) && inBank > 0) {
                worth = Economy.Worth.getWorth(currency).getKey();
            }

            if (!isSuperior) inferior = inferior / worth;
            else {
                superiorMult = superiorMult * worth;
                superior += inBank * superiorMult;
            }

            index++;
        }

        mixed += inferior + superior;

        return Utils.formatNumber(mixed, includeDecimals);
    }
}
