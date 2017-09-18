package me.glaremasters.guilds.commands;

import me.glaremasters.guilds.Main;
import me.glaremasters.guilds.commands.base.CommandBase;
import me.glaremasters.guilds.updater.SpigotUpdater;
import org.bukkit.command.CommandSender;

public class CommandUpdate extends CommandBase {

    public CommandUpdate() {
        super("update", Main.getInstance().getConfig().getString("commands.description.update"),
                "guilds.command.update",
                true, null, null, 0, 0);
    }

    public void execute(CommandSender sender, String[] args) {
        SpigotUpdater updater = new SpigotUpdater(Main.getInstance(), 46962);
        try {
            if (updater.checkForUpdates()) {
                sender.sendMessage("An update was found! New version: " + updater.getLatestVersion()
                        + " download: " + updater.getResourceURL());
            }
        } catch (Exception e) {
            sender.sendMessage("Could not check for updates!");
            e.printStackTrace();
        }
    }
}

