package io.github.idoomful.assassinscurrencycore.data.SQL;

import io.github.idoomful.assassinscurrencycore.utils.ConfigPair;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.stream.Collectors;

public class TransactionLog {
    private final String affectedPlayerName;
    private final String executorName;
    private final String comment;
    private final List<ConfigPair<Integer, String>> registeredCurrencies;

    private boolean currencySet, currencyWithdraw, currenciesAdded, walletIn, walletOut, bankDeposit, bankWithdraw, pseudoCurrency, convert;

    public TransactionLog(Player affected, Player executor, String comment, List<ConfigPair<Integer, String>> registeredCurrencies, int multiplier) {
        this.affectedPlayerName = affected.getName();
        this.executorName = executor.getName();
        this.comment = comment;
        this.registeredCurrencies = registeredCurrencies.stream()
                .peek(pair -> pair.setKey(pair.getKey() * multiplier))
                .collect(Collectors.toList());
    }

    public TransactionLog(Player affected, Player executor, String comment, List<ConfigPair<Integer, String>> registeredCurrencies) {
        this.affectedPlayerName = affected.getName();
        this.executorName = executor.getName();
        this.comment = comment;
        this.registeredCurrencies = registeredCurrencies;
    }

    public TransactionLog(String affected, String executor, String comment, List<ConfigPair<Integer, String>> registeredCurrencies, int multiplier) {
        this.affectedPlayerName = affected;
        this.executorName = executor;
        this.comment = comment;
        this.registeredCurrencies = registeredCurrencies.stream()
                .peek(pair -> pair.setKey(pair.getKey() * multiplier))
                .collect(Collectors.toList());
    }

    public TransactionLog(String affected, String executor, String comment, List<ConfigPair<Integer, String>> registeredCurrencies) {
        this.affectedPlayerName = affected;
        this.executorName = executor;
        this.comment = comment;
        this.registeredCurrencies = registeredCurrencies;
    }

    public TransactionLog currencySet(boolean input) {
        currencySet = input;
        return this;
    }

    public TransactionLog currenciesWithdraw(boolean input) {
        currencyWithdraw = input;
        return this;
    }

    public TransactionLog currenciesAdded(boolean input) {
        currenciesAdded = input;
        return this;
    }

    public TransactionLog walletIn(boolean input) {
        walletIn = input;
        return this;
    }

    public TransactionLog walletOut(boolean input) {
        walletOut = input;
        return this;
    }

    public TransactionLog bankDeposit(boolean input) {
        bankDeposit = input;
        return this;
    }

    public TransactionLog bankWithdraw(boolean input) {
        bankWithdraw = input;
        return this;
    }

    public TransactionLog pseudoCurrency(boolean input) {
        pseudoCurrency = input;
        return this;
    }

    public TransactionLog convert(boolean input) {
        convert = input;
        return this;
    }

    public String logMessage() {
        String executor = executorName.isEmpty() ? "unknown" : executorName;
        String comment = this.comment.isEmpty() ? "none" : this.comment;
        String comp1 = registeredCurrencies.size() == 1 ? "One currency was" : "Multiple currencies were";

        StringBuilder currencies = new StringBuilder();

        for(int i = 0; i < registeredCurrencies.size(); i++) {
            ConfigPair<Integer, String> pair = registeredCurrencies.get(i);
            currencies.append("x").append(pair.getKey()).append(" ").append(pair.getValue());

            if(i == registeredCurrencies.size() - 1) break;

            currencies.append(", ");
        }

        if(currencySet) {
            return comp1 + " set for " + affectedPlayerName +
                    " (executor: " + executor + "; "
                    + "set: " + currencies.toString() + "; "
                    + "comment: " + comment
                    + ")";
        } else if(currencyWithdraw) {
            return comp1 + " subtracted from " + affectedPlayerName +
                    " (executor: " + executor + "; "
                    + "subtracted: " + currencies.toString() + "; "
                    + "comment: " + comment
                    + ")";
        } else if(currenciesAdded) {
            return comp1 + " given to " + affectedPlayerName +
                    " (executor: " + executor + "; "
                    + "added: " + currencies.toString() + "; "
                    + "comment: " + comment
                    + ")";
        } else if(walletIn) {
            return comp1 + " added to " + affectedPlayerName + "'s wallet" +
                    " (executor: " + executor + "; "
                    + "added: " + currencies.toString() + "; "
                    + "comment: " + comment
                    + ")";
        } else if(walletOut) {
            return comp1 + " taken from " + affectedPlayerName + "'s wallet" +
                    " (executor: " + executor + "; "
                    + "taken: " + currencies.toString() + "; "
                    + "comment: " + comment
                    + ")";
        } else if(bankDeposit) {
            return comp1 + " deposited into " + affectedPlayerName + "'s bank" +
                    " (executor: " + executor + "; "
                    + "deposited: " + currencies.toString() + "; "
                    + "comment: " + comment
                    + ")";
        } else if(bankWithdraw) {
            return comp1 + " withdrawn from " + affectedPlayerName + "'s bank" +
                    " (executor: " + executor + "; "
                    + "withdrawn: " + currencies.toString() + "; "
                    + "comment: " + comment
                    + ")";
        } else if(pseudoCurrency) {
            return "An item that can be pseudo currency was found and converted in " + affectedPlayerName + "'s inventory" +
                    " (found currency: " + currencies.toString() + "; "
                    + "comment: " + comment
                    + ")";
        } else if(convert) {
            return comp1 + " converted in " + affectedPlayerName + "'s inventory" +
                    " (executor: " + executor + "; "
                    + "converted: " + currencies.toString() + "; "
                    + "comment: " + comment
                    + ")";
        }

        return "";
    }
}
