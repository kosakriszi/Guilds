package me.glaremasters.guilds.commands;

import com.sk89q.worldedit.BlockVector;
import com.sk89q.worldguard.bukkit.RegionContainer;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.domains.DefaultDomain;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.flags.DefaultFlag;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedCuboidRegion;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import me.glaremasters.guilds.Main;
import me.glaremasters.guilds.commands.base.CommandBase;
import me.glaremasters.guilds.guild.Guild;
import me.glaremasters.guilds.guild.GuildRole;
import me.glaremasters.guilds.message.Message;
import me.glaremasters.guilds.util.ConfirmAction;
import me.glaremasters.guilds.util.WorldGuardHandler;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

/**
 * Created by GlareMasters on 11/11/2017.
 */
public class CommandClaim extends CommandBase {

    WorldGuardHandler wg = new WorldGuardHandler();

    public CommandClaim() {
        super("claim", Main.getInstance().getConfig().getString("commands.description.claim"),
                "guilds.command.claim", false,
                null, null, 0, 1);
    }

    @Override
    public void execute(Player player, String[] args) {

        Guild guild = Guild.getGuild(player.getUniqueId());
        if (guild == null) {
            Message.sendMessage(player, Message.COMMAND_ERROR_NO_GUILD);
            return;
        }

        GuildRole role = GuildRole.getRole(guild.getMember(player.getUniqueId()).getRole());
        if (!role.canClaimLand()) {
            Message.sendMessage(player, Message.COMMAND_ERROR_ROLE_NO_PERMISSION);
            return;
        }

        final FileConfiguration config = Main.getInstance().getConfig();
        if (!config.getBoolean("hooks.worldguard")) {
            Message.sendMessage(player, Message.COMMAND_CLAIM_WORLDGUARD_REQUIRED);
            return;
        }
        double claimCost;

        if (config.getBoolean("custom-claim-size")) {
            if (args.length != 1) {
                Message.sendMessage(player, Message.COMMAND_CLAIM_SIZE_REQUIRED);
                return;
            }
            if (Integer.valueOf(args[0]) > config.getInt("custom-max-claim-size")) {
                Message.sendMessage(player, Message.COMMAND_CLAIM_TOO_BIG
                        .replace("{max}", String.valueOf(config.getInt("custom-max-claim-size"))));
                return;
            }

            claimCost = (config.getDouble("custom-claim-price") * Integer.valueOf(args[0]));
            if (Main.vault && claimCost != -1) {
                if (Main.getInstance().getEconomy().getBalance(player) < claimCost) {
                    Message.sendMessage(player, Message.COMMAND_ERROR_NOT_ENOUGH_MONEY);
                    return;
                }

                Message.sendMessage(player, Message.COMMAND_CREATE_MONEY_WARNING_SETHOME
                        .replace("{amount}", String.valueOf(claimCost)));


            }
        } else {
            claimCost = config.getDouble("regular-claim-price");
            if (Main.vault && claimCost != -1) {
                if (Main.getInstance().getEconomy().getBalance(player) < claimCost) {
                    Message.sendMessage(player, Message.COMMAND_ERROR_NOT_ENOUGH_MONEY);
                    return;
                }
                Message.sendMessage(player, Message.COMMAND_CREATE_MONEY_WARNING_SETHOME
                        .replace("{amount}", String.valueOf(claimCost)));

            }
        }

        Main.getInstance().getCommandHandler().addAction(player, new ConfirmAction() {
            @Override
            public void accept() {
                if (config.getBoolean("custom-claim-size")) {
                    double claimCost = (config.getDouble("custom-claim-price") * Double
                            .valueOf(args[0]));
                    EconomyResponse response =
                            Main.getInstance().getEconomy().withdrawPlayer(player, claimCost);
                    if (!response.transactionSuccess()) {
                        Message.sendMessage(player, Message.COMMAND_ERROR_NOT_ENOUGH_MONEY);
                        return;
                    }
                } else {
                    double claimCost2 = config.getDouble("regular-claim-price");
                    EconomyResponse response2 =
                            Main.getInstance().getEconomy().withdrawPlayer(player, claimCost2);
                    if (!response2.transactionSuccess()) {
                        Message.sendMessage(player, Message.COMMAND_ERROR_NOT_ENOUGH_MONEY);
                        return;
                    }
                }
                if (!config.getBoolean("custom-claim-size")) {
                    BlockVector min = new BlockVector((player.getLocation().getX() - (
                            config.getInt("regular-claim-size") / 2)), 0,
                            (player.getLocation().getZ() - (config.getInt("regular-claim-size")
                                    / 2)));
                    BlockVector max = new BlockVector((player.getLocation().getX() + (
                            config.getInt("regular-claim-size") / 2)), 255,
                            (player.getLocation().getZ() + (config.getInt("regular-claim-size")
                                    / 2)));
                    ProtectedRegion region = new ProtectedCuboidRegion(guild.getName(), min, max);
                    RegionContainer container = wg.getWorldGuard().getRegionContainer();
                    RegionManager regions = container.get(player.getWorld());

                    if (region != null) {
                        regions.removeRegion(guild.getName());
                    }

                    ApplicableRegionSet set = regions.getApplicableRegions(region);
                    if (set.size() > 0) {
                        Message.sendMessage(player, Message.COMMAND_CLAIM_TOO_CLOSE);
                        return;
                    }

                    regions.addRegion(region);
                    Message.sendMessage(player, Message.COMMAND_CLAIM_COORDINATES);
                    player.sendMessage(ChatColor.BLUE + "" + Math
                            .ceil((player.getLocation().getX() - (
                                    config.getInt("regular-claim-size")
                                            / 2))) + ", " + "0.0" + ", " +
                            Math.ceil((player.getLocation().getZ() - (
                                    config.getInt("regular-claim-size")
                                            / 2))) + ChatColor.GREEN + " to " +
                            ChatColor.BLUE + (Math.ceil((player.getLocation().getX() + (
                            config.getInt("regular-claim-size") / 2))) + ", " + "255.0, "
                            + (Math
                            .ceil((player.getLocation().getZ() + (
                                    config.getInt("regular-claim-size")
                                            / 2))))));
                    region.setFlag(DefaultFlag.GREET_MESSAGE,
                            "Entering " + guild.getName() + "'s base");
                    region.setFlag(DefaultFlag.FAREWELL_MESSAGE,
                            "Leaving " + guild.getName() + "'s base");

                    ProtectedRegion regionTest = regions.getRegion(guild.getName());
                    Location outlineMin = new Location(player.getWorld(), 0, 0, 0);
                    outlineMin.setX(regionTest.getMinimumPoint().getX());
                    outlineMin.setY(player.getLocation().getY());
                    outlineMin.setZ(regionTest.getMinimumPoint().getZ());

                    Location outlineMax = new Location(player.getWorld(), 0, 0, 0);
                    outlineMax.setX(regionTest.getMaximumPoint().getX());
                    outlineMax.setY(player.getLocation().getY());
                    outlineMax.setZ(regionTest.getMaximumPoint().getZ());

                    for (double x1 = 0; x1 <= outlineMax.getX() - outlineMin.getX(); x1++) {
                        player.sendBlockChange(outlineMin.clone().add(x1, 0, 0), Material.DIRT,
                                (byte) 0);
                    }

                    for (double z = 0; z <= outlineMax.getZ() - outlineMin.getZ(); z++) {
                        player.sendBlockChange(outlineMin.clone().add(0, 0, z), Material.DIRT,
                                (byte) 0);
                    }

                    DefaultDomain members = region.getMembers();
                    DefaultDomain owners = region.getOwners();
                    owners.addPlayer(player.getName());
                    guild.getMembers().stream()
                            .map(member -> Bukkit.getOfflinePlayer(member.getUniqueId()))
                            .forEach(member -> {
                                members.addPlayer(member.getName());
                            });
                } else {
                    BlockVector min = new BlockVector(
                            (player.getLocation().getX() - (Integer.valueOf(args[0]) / 2)), 0,
                            (player.getLocation().getZ() - (Integer.valueOf(args[0]) / 2)));
                    BlockVector max = new BlockVector((player.getLocation().getX() + (
                            Integer.valueOf(args[0]) / 2)), 255,
                            (player.getLocation().getZ() + (Integer.valueOf(args[0]) / 2)));
                    ProtectedRegion region = new ProtectedCuboidRegion(guild.getName(), min, max);
                    RegionContainer container = wg.getWorldGuard().getRegionContainer();
                    RegionManager regions = container.get(player.getWorld());

                    if (region != null) {
                        regions.removeRegion(guild.getName());
                    }

                    ApplicableRegionSet set = regions.getApplicableRegions(region);
                    if (set.size() > 0) {
                        Message.sendMessage(player, Message.COMMAND_CLAIM_TOO_CLOSE);
                        return;
                    }

                    regions.addRegion(region);
                    Message.sendMessage(player, Message.COMMAND_CLAIM_COORDINATES);
                    player.sendMessage(ChatColor.BLUE + "" + Math
                            .ceil((player.getLocation().getX() - (Integer.valueOf(args[0])
                                    / 2))) + ", " + "0.0" + ", " +
                            Math.ceil((player.getLocation().getZ() - (Integer.valueOf(args[0])
                                    / 2))) + ChatColor.GREEN + " to " +
                            ChatColor.BLUE + (Math.ceil((player.getLocation().getX() + (
                            Integer.valueOf(args[0]) / 2))) + ", " + "255.0, "
                            + (Math
                            .ceil((player.getLocation().getZ() + (Integer.valueOf(args[0])
                                    / 2))))));
                    region.setFlag(DefaultFlag.GREET_MESSAGE,
                            "Entering " + guild.getName() + "'s base");
                    region.setFlag(DefaultFlag.FAREWELL_MESSAGE,
                            "Leaving " + guild.getName() + "'s base");

                    ProtectedRegion regionTest = regions.getRegion(guild.getName());
                    Location outlineMin = new Location(player.getWorld(), 0, 0, 0);
                    outlineMin.setX(regionTest.getMinimumPoint().getX());
                    outlineMin.setY(player.getLocation().getY());
                    outlineMin.setZ(regionTest.getMinimumPoint().getZ());

                    Location outlineMax = new Location(player.getWorld(), 0, 0, 0);
                    outlineMax.setX(regionTest.getMaximumPoint().getX());
                    outlineMax.setY(player.getLocation().getY());
                    outlineMax.setZ(regionTest.getMaximumPoint().getZ());

                    for (double x1 = 0; x1 <= outlineMax.getX() - outlineMin.getX(); x1++) {
                        player.sendBlockChange(outlineMin.clone().add(x1, 0, 0), Material.DIRT,
                                (byte) 0);
                    }

                    for (double z = 0; z <= outlineMax.getZ() - outlineMin.getZ(); z++) {
                        player.sendBlockChange(outlineMin.clone().add(0, 0, z), Material.DIRT,
                                (byte) 0);
                    }

                    DefaultDomain members = region.getMembers();
                    DefaultDomain owners = region.getOwners();
                    owners.addPlayer(player.getName());
                    guild.getMembers().stream()
                            .map(member -> Bukkit.getOfflinePlayer(member.getUniqueId()))
                            .forEach(member -> {
                                members.addPlayer(member.getName());
                            });
                }
            }

            @Override
            public void decline() {
                Main.getInstance().getCommandHandler().removeAction(player);
            }
        });
    }

}



