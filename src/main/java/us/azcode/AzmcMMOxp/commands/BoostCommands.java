package us.azcode.AzmcMMOxp.commands;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import us.azcode.AzmcMMOxp.AzXPBoost;
import us.azcode.AzmcMMOxp.util.BoostManager;

import java.io.File;

public class BoostCommands implements CommandExecutor {
    private final AzXPBoost plugin;
    private final BoostManager boostManager;
    private FileConfiguration messages;

    public BoostCommands(AzXPBoost plugin, BoostManager boostManager, Economy economy) {
        this.plugin = plugin;
        this.boostManager = boostManager;
        this.messages = YamlConfiguration.loadConfiguration(new File(plugin.getDataFolder(), "messages.yml"));
    }

    private String getMessage(String key) {
        return ChatColor.translateAlternateColorCodes('&', messages.getString("messages." + key, "Message not found: " + key));
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("mcba")) {
            if (!sender.hasPermission("mcb.admin")) {
                sender.sendMessage(getMessage("no_permission"));
                return true;
            }
            if (args.length > 0) {
                switch (args[0].toLowerCase()) {
                    case "give":
                        if (args.length < 4) {
                            sender.sendMessage(getMessage("incorrect_command_usage"));
                            return true;
                        }
                        Player target = Bukkit.getPlayer(args[1]);
                        if (target == null) {
                            sender.sendMessage(getMessage("player_not_found"));
                            return true;
                        }
                        boostManager.giveBoost(target.getUniqueId(), args[2], Integer.parseInt(args[3]));
                        sender.sendMessage(getMessage("boost_given"));
                        break;
                    case "reload":
                        plugin.reloadConfig();
                        messages = YamlConfiguration.loadConfiguration(new File(plugin.getDataFolder(), "messages.yml"));
                        sender.sendMessage(getMessage("config_reloaded"));
                        break;
                    case "view":
                        if (args.length < 2) {
                            sender.sendMessage(getMessage("incorrect_command_usage"));
                            return true;
                        }
                        target = Bukkit.getPlayer(args[1]);
                        if (target == null) {
                            sender.sendMessage(getMessage("player_not_found"));
                            return true;
                        }
                        boostManager.viewBoosts(sender, target.getUniqueId());
                        break;
                    default:
                        sender.sendMessage(getMessage("unknown_command"));
                        break;
                }
            } else {
                sender.sendMessage(getMessage("incorrect_command_usage"));
            }
            return true;
        }

        // Existing mcm command handling...
        if (!(sender instanceof Player)) {
            sender.sendMessage(getMessage("players_only"));
            return true;
        }
        Player player = (Player) sender;

        if (args.length > 0) {
            switch (args[0].toLowerCase()) {
                case "buy":
                    if (args.length < 2) {
                        sender.sendMessage(getMessage("incorrect_command_usage"));
                        return true;
                    }
                    boostManager.handleBuyCommand(player, args[1]);
                    break;
                case "list":
                    boostManager.handleListCommand(player);
                    break;
                case "claim":
                    if (args.length < 2) {
                        sender.sendMessage(getMessage("incorrect_command_usage"));
                        return true;
                    }
                    boostManager.handleClaimCommand(player, args[1]);
                    break;
                case "time":
                    boostManager.handleTimeCommand(player);
                    break;
                default:
                    sender.sendMessage(getMessage("unknown_command"));
                    break;
            }
        } else {
            sender.sendMessage(getMessage("incorrect_command_usage"));
        }
        return true;
    }
}
