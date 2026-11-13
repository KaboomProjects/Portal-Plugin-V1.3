package com.kodari.portalspawn.carts;

import com.kodari.portalspawn.PortalSpawnPlugin;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

/** /portalspawntnt <radius> [amount] */
public final class TntCommand implements CommandExecutor, TabCompleter {

    static final int MAX_RADIUS = 150;
    static final int MAX_AMOUNT = 500;

    private final PortalSpawnPlugin plugin;

    public TntCommand(PortalSpawnPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("portalspawn.admin")) {
            sender.sendMessage(ChatColor.RED + "You do not have permission to use this command.");
            return true;
        }
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Only a player can use this command.");
            return true;
        }
        if (args.length < 1 || args.length > 2) {
            sender.sendMessage(ChatColor.RED + "Usage: /" + label + " <radius> [amount]");
            return true;
        }
        int radius;
        int amount = 50;
        try {
            radius = Integer.parseInt(args[0]);
            if (args.length == 2) {
                amount = Integer.parseInt(args[1]);
            }
        } catch (NumberFormatException ex) {
            sender.sendMessage(ChatColor.RED + "Radius and amount must be whole numbers.");
            return true;
        }
        if (radius < 1 || radius > MAX_RADIUS) {
            sender.sendMessage(ChatColor.RED + "Radius must be between 1 and " + MAX_RADIUS + ".");
            return true;
        }
        if (amount < 1 || amount > MAX_AMOUNT) {
            sender.sendMessage(ChatColor.RED + "Amount must be between 1 and " + MAX_AMOUNT + ".");
            return true;
        }

        Player player = (Player) sender;
        Location loc = player.getLocation();
        int perBurst = Math.max(1, plugin.getConfig().getInt("tnt.per-burst", 10));
        int intervalTicks = Math.max(1, (int) Math.round(plugin.getConfig().getDouble("tnt.interval-seconds", 1.0) * 20.0));
        float power = (float) plugin.getConfig().getDouble("carts.explosion-power", 4.0);

        CartLauncher.launchArea(plugin, player.getWorld(), loc.getBlockX(), loc.getBlockY(), loc.getBlockZ(),
                radius, amount, perBurst, intervalTicks, power);
        sender.sendMessage(CartsCommand.prefix() + "Spawning " + amount + " TNT carts within a "
                + radius + "-block radius around you.");
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return Arrays.asList("10", "25", "50", "100");
        }
        if (args.length == 2) {
            return Arrays.asList("25", "50", "100", "250");
        }
        return new ArrayList<String>();
    }
}
