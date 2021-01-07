package io.github.idoomful.assassinseconomy.gui;

import io.github.idoomful.assassinseconomy.DMain;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.*;
import java.util.logging.Logger;

public class InventoryBuilder {
	private final Map<String, ItemStack> symbolMatching = new HashMap<>();
	private Inventory inventory;
	private PlayerInventory playerInventory;
	private Inventory playerInv;
	private final boolean isPlayerInv;

    private final Logger log = DMain.getInstance().getLogger();
	
	public InventoryBuilder(Inventory inventory) {
		this.inventory = inventory;
		isPlayerInv = false;
	}

	public InventoryBuilder(PlayerInventory playerInventory) {
	    this.playerInventory = playerInventory;
	    playerInv = Bukkit.createInventory(null, 9 * 4, "");
	    isPlayerInv = true;
    }
	
	public void setItems(String ... symbols) {
		if((isPlayerInv ? playerInv.getSize() : inventory.getSize()) < symbols.length) {
			return;
		}
		
		for (int i = 0; i < symbols.length; i++) {
			if (symbolMatching.containsKey(symbols[i])) {
				ItemStack item = symbolMatching.get(symbols[i]);
				if (item.getType().equals(Material.AIR)) continue;
				if(isPlayerInv) playerInv.setItem(i, item);
				else inventory.setItem(i, item);
			}
		}
		symbolMatching.clear();
	}

	// 2
    public void setConfigItemArrangement(List<String> list) {
        int index = 0;
        for(String row : list) {
            if(index >= (isPlayerInv ? playerInv.getSize() : inventory.getSize())) {
                return;
            }

            for(String ch : row.split(" ")) {
                if(ch.equals("-")) {
                    index += 1;
                    continue;
                }
                if (symbolMatching.containsKey(ch)) {
                    if(isPlayerInv) playerInv.setItem(index++, symbolMatching.get(ch));
                    else inventory.setItem(index++, symbolMatching.get(ch));
                }
            }
        }
        symbolMatching.clear();
    }

    public void setItemStack(String symbol, ItemStack item) {
        symbolMatching.put(symbol, item);
    }

    // 1
    public void setConfigItemList(List<String> list, Player player) {
        for(String item : list) {
            final String symbol = item.split(" ")[0];
            String itemString = item.substring(item.indexOf(item.split(" ")[1]));

            if(player != null) itemString = itemString.replace("$player$", player.getName());
            final ItemStack is = ItemBuilder.build(itemString);

            symbolMatching.put(symbol, is);
        }
    }

    public void setRow(int row, String ... symbols) {
	    if(symbols.length > 9 || row > 6) return;

	    final int beginning = (9 * row) - 9;
	    int symbolIndex = 0;

        for (int i = beginning; i < beginning + 9; i++) {
            String symbol = symbols[symbolIndex];
            symbolIndex += 1;

            if (symbol.equals("-")) {
                if(isPlayerInv) playerInv.setItem(i, null);
                else inventory.setItem(i, null);
                continue;
            }
            if (symbolMatching.containsKey(symbol)) {
                final ItemStack item = symbolMatching.get(symbol);
                if(isPlayerInv) playerInv.setItem(i, item);
                else inventory.setItem(i, item);
            }
        }
    }

    public void setConfigRow(int rowNum, String row) {
	    int beginning = (9 * rowNum) - 9;

        for (String ch : row.split(" ")) {
            if (ch.equals("-")) {
                beginning += 1;
                continue;
            }

            if (symbolMatching.containsKey(ch)) {
                if (isPlayerInv) playerInv.setItem(beginning++, symbolMatching.get(ch));
                else inventory.setItem(beginning++, symbolMatching.get(ch));
            }
        }

        symbolMatching.clear();
    }

	public Inventory getInventory() {
	    return isPlayerInv ? playerInv : inventory;
    }

    public List<Object> getAddedItems() {
	    return new ArrayList<>(symbolMatching.values());
    }

    public void overrideAddedItems(List<ItemStack> list) {
	    int index = 0;
	    for(Map.Entry<String, ItemStack> set : symbolMatching.entrySet()) {
	        symbolMatching.put(set.getKey(), list.get(index));
	        index++;
        }
    }

    public void setHotbar() {
	    if(!isPlayerInv) return;

	    List<ItemStack> hotbar = Arrays.asList(playerInv.getContents()).subList(27, 36);
	    ItemStack[] modifiedContents = playerInventory.getContents();

	    for(int i = 0; i < 9; i++) {
	        modifiedContents[i] = hotbar.get(i);
        }

	    playerInventory.setContents(modifiedContents);
    }

    public void setStorage() {
        if(!isPlayerInv) return;

        List<ItemStack> hotbar = Arrays.asList(playerInv.getContents()).subList(0, 27);
        ItemStack[] modifiedContents = playerInventory.getContents();

        for(int i = 9; i < 36; i++) {
            modifiedContents[i] = hotbar.get(i - 9);
        }

        playerInventory.setContents(modifiedContents);
    }

    public void setPlayerInventory() {
	    setStorage();
	    setHotbar();
    }
}
