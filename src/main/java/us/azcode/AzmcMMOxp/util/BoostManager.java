package us.azcode.AzmcMMOxp.util;

import com.gmail.nossr50.events.experience.McMMOPlayerXpGainEvent;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import us.azcode.AzmcMMOxp.AzXPBoost;
import us.azcode.AzmcMMOxp.model.BoostType;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class BoostManager implements Listener {
    private final AzXPBoost plugin;
    private final Economy economy;
    private final Map<String, BoostType> boostTypes;
    private final Map<UUID, List<BoostType>> playerBoosts;
    private final Map<UUID, BoostType> activeBoosts;
    private final Map<UUID, Long> activeBoostsEndTime;
    private final DBManager dbManager;
    private final FileConfiguration messages;

    public BoostManager(AzXPBoost plugin, Economy economy) {
        this.plugin = plugin;
        this.economy = economy;
        this.boostTypes = new HashMap<>();
        this.playerBoosts = new HashMap<>();
        this.activeBoosts = new HashMap<>();
        this.activeBoostsEndTime = new HashMap<>();
        this.dbManager = new DBManager(this.plugin, this);
        this.messages = YamlConfiguration.loadConfiguration(new File(plugin.getDataFolder(), "messages.yml"));
        loadBoosts();
    }

    private void loadBoosts() {
        ConfigurationSection boostsSection = plugin.getConfig().getConfigurationSection("boosts");
        if (boostsSection != null) {
            for (String key : boostsSection.getKeys(false)) {
                String name = boostsSection.getString(key + ".name");
                double multiplier = boostsSection.getDouble(key + ".multiplier");
                int duration = boostsSection.getInt(key + ".duration");
                double price = boostsSection.getDouble(key + ".price");
                BoostType boostType = new BoostType(name, multiplier, duration, price);
                boostTypes.put(name.toLowerCase(), boostType);
            }
        }
    }

    public BoostType getBoostType(String name) {
        return boostTypes.get(name.toLowerCase());
    }

    private String getMessage(String key) {
        return ChatColor.translateAlternateColorCodes('&', messages.getString("messages." + key, "Message not found: " + key));
    }

    public void handleBuyCommand(Player player, String boostName) {
        BoostType boostType = getBoostType(boostName);
        if (boostType == null) {
            player.sendMessage(getMessage("boost_not_found"));
            return;
        }
        if (economy.getBalance(player) >= boostType.getPrice()) {
            economy.withdrawPlayer(player, boostType.getPrice());
            playerBoosts.computeIfAbsent(player.getUniqueId(), k -> new ArrayList<>()).add(boostType);
            dbManager.saveBoost(player.getUniqueId(), boostType);
            player.sendMessage(getMessage("purchase_successful"));
        } else {
            player.sendMessage(getMessage("insufficient_funds"));
        }
    }

    public void handleListCommand(Player player) {
        UUID uuid = player.getUniqueId();
        List<BoostType> boosts = dbManager.loadBoosts(uuid);
        if (boosts.isEmpty()) {
            player.sendMessage(getMessage("no_purchased_boosts"));
            return;
        }

        StringBuilder boostList = new StringBuilder(getMessage("available_boost"));
        Map<String, Integer> boostCounts = new HashMap<>();
        for (BoostType boost : boosts) {
            boostCounts.put(boost.getName(), boostCounts.getOrDefault(boost.getName(), 0) + 1);
        }
        for (Map.Entry<String, Integer> entry : boostCounts.entrySet()) {
            boostList.append("\n").append(getMessage("boost_format")
                .replace("%boost_name%", entry.getKey())
                .replace("%quantity%", String.valueOf(entry.getValue())));
        }
        player.sendMessage(boostList.toString());
    }

    public void handleClaimCommand(Player player, String boostName) {
        if (activeBoosts.containsKey(player.getUniqueId())) {
            player.sendMessage(getMessage("already_active_boost"));
            return;
        }

        List<BoostType> boosts = dbManager.loadBoosts(player.getUniqueId());
        if (boosts.isEmpty()) {
            player.sendMessage(getMessage("no_boosts_to_claim"));
            return;
        }
        BoostType boostToClaim = boosts.stream()
                .filter(boost -> boost.getName().equalsIgnoreCase(boostName))
                .findFirst()
                .orElse(null);
        if (boostToClaim == null) {
            player.sendMessage(getMessage("not_purchased_boost"));
            return;
        }
        activeBoosts.put(player.getUniqueId(), boostToClaim);
        long endTime = System.currentTimeMillis() + (boostToClaim.getDuration() * 1000L);
        activeBoostsEndTime.put(player.getUniqueId(), endTime);
        dbManager.setBoostUnavailable(player.getUniqueId(), boostToClaim);
        player.sendMessage(getMessage("boost_activated").replace("%boost_name%", boostToClaim.getName()));

        long delay = boostToClaim.getDuration() * 20L; 
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (activeBoosts.get(player.getUniqueId()) == boostToClaim) {
                player.sendMessage(getMessage("boost_expired"));
                activeBoosts.remove(player.getUniqueId());
                activeBoostsEndTime.remove(player.getUniqueId());
            }
        }, delay);
    }

    public void handleTimeCommand(Player player) {
        BoostType activeBoost = activeBoosts.get(player.getUniqueId());
        Long endTime = activeBoostsEndTime.get(player.getUniqueId());
        if (activeBoost == null || endTime == null || endTime < System.currentTimeMillis()) {
            player.sendMessage(getMessage("no_active_boost"));
            return;
        }
        long timeRemaining = (endTime - System.currentTimeMillis()) / 1000L;
        String boostInfo = getMessage("active_boost_and_time_remaining")
            .replace("%boost_name%", activeBoost.getName())
            .replace("%multiplier%", String.valueOf(activeBoost.getMultiplier()))
            .replace("%time_remaining%", String.valueOf(timeRemaining));
        for (String line : boostInfo.split("\n")) {
            player.sendMessage(line);
        }
    }
    public void giveBoost(UUID uuid, String boostName, int quantity) {
        BoostType boostType = getBoostType(boostName);
        if (boostType == null) {
            return;
        }
        for (int i = 0; i < quantity; i++) {
            dbManager.saveBoost(uuid, boostType);
        }
    }

    public void viewBoosts(CommandSender sender, UUID uuid) {
        List<BoostType> boosts = dbManager.loadBoosts(uuid);
        StringBuilder boostList = new StringBuilder(getMessage("available_boost"));
        Map<String, Integer> boostCounts = new HashMap<>();
        for (BoostType boost : boosts) {
            boostCounts.put(boost.getName(), boostCounts.getOrDefault(boost.getName(), 0) + 1);
        }
        for (Map.Entry<String, Integer> entry : boostCounts.entrySet()) {
            boostList.append("\n").append(getMessage("boost_format")
                .replace("%boost_name%", entry.getKey())
                .replace("%quantity%", String.valueOf(entry.getValue())));
        }
        sender.sendMessage(boostList.toString());

        BoostType activeBoost = activeBoosts.get(uuid);
        if (activeBoost != null) {
            sender.sendMessage(getMessage("active_boost")
                .replace("%boost_name%", activeBoost.getName())
                .replace("%multiplier%", String.valueOf(activeBoost.getMultiplier())));
        } else {
            sender.sendMessage(getMessage("no_active_boost"));
        }
    }
    @EventHandler
    public void onXpGain(McMMOPlayerXpGainEvent event) {
        Player player = event.getPlayer();
        BoostType activeBoost = activeBoosts.get(player.getUniqueId());
        if (activeBoost != null) {
            event.setRawXpGained((int) (event.getRawXpGained() * activeBoost.getMultiplier()));
        }
    }
}
