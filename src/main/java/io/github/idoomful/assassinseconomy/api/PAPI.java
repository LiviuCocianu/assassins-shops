package io.github.idoomful.assassinseconomy.api;

import io.github.idoomful.assassinseconomy.DMain;
import io.github.idoomful.assassinseconomy.utils.Economy;
import io.github.idoomful.assassinseconomy.utils.Utils;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
        return "assassinscurrency";
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
        if(identifier.startsWith("currency_")) {
            String phCurrency = identifier.replaceFirst("_", "*").split("\\*")[1];
            AtomicReference<HashMap<String, Integer>> inv = new AtomicReference<>(null);

            plugin.getSQL().exists(player.getName(), res -> {
                if(res) plugin.getSQL().getCurrencies(player.getName(), inv::set);
            });

            if(inv.get() == null || !inv.get().containsKey(phCurrency)) return "0";

            return Utils.formatNumber(inv.get().get(phCurrency));
        }

        if(identifier.startsWith("currencymix_")) {
            String phCurrency = identifier.replaceFirst("_", "*").split("\\*")[1];
            AtomicReference<HashMap<String, Integer>> bank = new AtomicReference<>(null);

            plugin.getSQL().exists(player.getName(), res -> {
                if(res) plugin.getSQL().getCurrencies(player.getName(), bank::set);
            });

            if(bank.get() == null) return "0";

            int mixed = 0;
            int inferior = 1, superior = 1;
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
                    superior = inBank == 0 ? 1 : inBank;
                    index -= 99;
                }

                int worth = Economy.Worth.getWorthAbove(currency, 1);
                worth = worth == 0 ? 1 : worth;

                Bukkit.broadcastMessage(""); // TODO debug
                Bukkit.broadcastMessage("currency: " + currency); // TODO debug
                Bukkit.broadcastMessage("worth: " + worth); // TODO debug
                Bukkit.broadcastMessage("inferior 0: " + inferior); // TODO debug
                Bukkit.broadcastMessage("superior 0: " + superior); // TODO debug

                if((index == Economy.Currency.getIDs().size() - 1) && inBank > 0) {
                    worth = Economy.Worth.getWorth(currency).getKey();
                }

                if (!isSuperior) inferior = inferior / worth;
                else superior = superior * worth;

                Bukkit.broadcastMessage("inferior: " + inferior); // TODO debug
                Bukkit.broadcastMessage("superior: " + superior); // TODO debug

                index++;
            }

            mixed += inferior + superior;

            return Utils.formatNumber(mixed);
        }

        return null;
    }
}
