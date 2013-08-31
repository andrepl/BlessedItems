package com.norcode.bukkit.blesseditems;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

public class DataStore {

    final String fileName;
    final JavaPlugin plugin;

    File configFile;
    FileConfiguration fileConfiguration;

    public DataStore(JavaPlugin plugin, String fileName) {
        if (plugin == null)
            throw new IllegalArgumentException("plugin cannot be null");
        if (!plugin.isInitialized())
            throw new IllegalArgumentException("plugin must be initialized");
        this.plugin = plugin;
        this.fileName = fileName;
    }

    public void reloadConfig() {
        if (configFile == null) {
            File dataFolder = plugin.getDataFolder();
            if (dataFolder == null)
                throw new IllegalStateException();
            configFile = new File(dataFolder, fileName);
        }
        fileConfiguration = YamlConfiguration.loadConfiguration(configFile);

        // Look for defaults in the jar
        InputStream defConfigStream = plugin.getResource(fileName);
        if (defConfigStream != null) {
            YamlConfiguration defConfig = YamlConfiguration.loadConfiguration(defConfigStream);
            fileConfiguration.setDefaults(defConfig);
        }
    }

    public FileConfiguration getConfig() {
        if (fileConfiguration == null) {
            this.reloadConfig();
        }
        return fileConfiguration;
    }

    public void saveConfig() {
        if (fileConfiguration == null || configFile == null) {
            return;
        } else {
            try {
                getConfig().save(configFile);
            } catch (IOException ex) {
                plugin.getLogger().log(Level.SEVERE, "Could not save config to " + configFile, ex);
            }
        }
    }

    public void saveDefaultConfig() {
        if (!configFile.exists()) {
            this.plugin.saveResource(fileName, false);
        }
    }

    public void saveItems(Player player, HashMap<Integer, ItemStack> items) {
        ConfigurationSection sect = null;
        if (getConfig().contains(player.getName())) {
            sect = getConfig().getConfigurationSection(player.getName());
        } else {
            sect = getConfig().createSection(player.getName());
        }
        for (Map.Entry<Integer, ItemStack> e: items.entrySet()) {
            sect.set(e.getKey().toString(), e.getValue());
        }
    }

    public HashMap<Integer, ItemStack> getItems(Player player) {
        HashMap<Integer, ItemStack> items = new HashMap<Integer, ItemStack>();
        if (getConfig().contains(player.getName())) {
            ConfigurationSection sect = getConfig().getConfigurationSection(player.getName());
            for (String key: sect.getKeys(false)) {
                Integer slot = Integer.parseInt(key);
                items.put(slot, sect.getItemStack(key));
            }
        }
        return items;
    }

    public void clearItems(Player p) {
        getConfig().set(p.getName(), null);
    }
}