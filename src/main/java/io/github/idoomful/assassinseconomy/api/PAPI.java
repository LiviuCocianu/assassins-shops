package io.github.idoomful.assassinseconomy.api;

import io.github.idoomful.assassinseconomy.DMain;
import io.github.idoomful.assassinseconomy.utils.Economy;
import io.github.idoomful.assassinseconomy.utils.Utils;
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
        if(identifier.startsWith("currency_") || identifier.startsWith("curr_") || identifier.startsWith("c_")) {
            String phCurrency = identifier.replaceFirst("_", "*").split("\\*")[1];
            AtomicReference<LinkedHashMap<String, Integer>> inv = new AtomicReference<>(null);

            plugin.getSQL().exists(player.getName(), res -> {
                if(res) plugin.getSQL().getCurrencies(player.getName(), inv::set);
            });

            if(inv.get() == null || !inv.get().containsKey(phCurrency)) return "0";

            return Utils.formatNumber(inv.get().get(phCurrency), false);
        }

        if(identifier.startsWith("currencymix_") || identifier.startsWith("currmix_") || identifier.startsWith("mix_") || identifier.startsWith("m_")) {
            return mixCurrencies(identifier, player.getName(), false);
        }

        if(identifier.startsWith("currencymixd_") || identifier.startsWith("currmixd_") || identifier.startsWith("mixd_") || identifier.startsWith("md_")) {
            return mixCurrencies(identifier, player.getName(), true);
        }

        return null;
    }

    private String mixCurrencies(String identifier, String player, boolean includeDecimals) {
        String phCurrency = identifier.replaceFirst("_", "*").split("\\*")[1];
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

            if (phCurrency.equals(currency)) {
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
