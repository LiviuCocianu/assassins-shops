package io.github.idoomful.assassinscurrencycore.configuration;

import io.github.idoomful.assassinscurrencycore.utils.ConfigPair;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

// ConfigManager class built by iDoomful

public class ConfigManager<T extends JavaPlugin> {
    private final T plugin;
    private final HashMap<String, Map.Entry<File, FileConfiguration>> files = new HashMap<>();

    public ConfigManager(T plugin) {
        this.plugin = plugin;

        addConfigurationFile("messages");
        addConfigurationFile("settings");
        addConfigurationFile("shops");
    }

    public ConfigManager<T> addConfigurationFile(String name) {
        // Add required files
        Map.Entry<File, FileConfiguration> filePair = new ConfigPair<>(new File(plugin.getDataFolder(), name + ".yml"), new YamlConfiguration());
        files.put(name, filePair);

        // Create <name>.yml
        final File configFile = files.get(name).getKey();
        final FileConfiguration configYAML = files.get(name).getValue();

        if(!configFile.exists()) {
            boolean ignored = configFile.getParentFile().mkdirs();
            plugin.saveResource(name + ".yml", false);
        }
        try {
            configYAML.load(configFile);
        } catch(IOException | InvalidConfigurationException ie) {
            ie.printStackTrace();
        }

        return this;
    }

    public void reloadConfigs() {
        files.forEach((name, pair) -> {
            final File configFile = pair.getKey();
            final FileConfiguration configYAML = pair.getValue();

            if (!configFile.exists()) {
                addConfigurationFile(name);
                plugin.getLogger().info(name + "'.yml' nu a fost gasit, se recreeza versiunea implicita...");
            } else {
                try {
                    configYAML.load(configFile);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        plugin.getLogger().info("Configuratia a fost reincarcata.");
    }

    public FileConfiguration getFile(String name) {
        return files.get(name).getValue();
    }
}