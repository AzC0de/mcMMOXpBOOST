package us.azcode.AzmcMMOxp;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import us.azcode.AzmcMMOxp.commands.BoostCommands;
import us.azcode.AzmcMMOxp.util.BoostManager;

public class AzXPBoost extends JavaPlugin {
    private Economy economy;
    private BoostManager boostManager;

    @Override
    public void onEnable() {
        if (!setupEconomy()) {
            getLogger().severe(String.format("[%s] Disabled due to no Vault dependency found!", getDescription().getName()));
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        saveDefaultConfig();
        saveResource("messages.yml", false);
        boostManager = new BoostManager(this, economy);
        getServer().getPluginManager().registerEvents(boostManager, this);
        getCommand("mcm").setExecutor(new BoostCommands(this, boostManager, economy));
    }

    private boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            return false;
        }
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return false;
        }
        economy = rsp.getProvider();
        return economy != null;
    }
}