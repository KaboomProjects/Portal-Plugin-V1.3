package com.kodari.portalspawn.command;

import com.kodari.portalspawn.PortalSpawnPlugin;
import com.kodari.portalspawn.portal.PortalPlacement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

/**
 * /portalspawn        <X> <Y> <Z> <Amount>  -> gives a one-use Portal Rod
 * /portalspawncentre  <X> <Y> <Z> <Amount>  -> spawns portals within 100 blocks right away
 */
public final class PortalSpawnCommand implements CommandExecutor, TabCompleter {

    public static final int MAX_PORTALS = 100;

    private final PortalSpawnPlugin plugin;
    private final boolean spawnAroundCoordinates;

    public PortalSpawnCommand(PortalSpawnPlugin plugin, boolean spawnAroundCoordinates) {
        this.plugin = plugin;
        this.spawnAroundCoordinates = spawnAroundCoordinates;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String permission = spawnAroundCoordinates ? "portalspawn.admin" : "portalspawn.use";
        if (!sender.hasPermission(permission)) {
            sender.sendMessage(ChatColor.RED + "You do not have permission to use this command.");
            return true;
        }
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + (spawnAroundCoordinates
                    ? "Only a player can spawn portals around coordinates in their current world."
                    : "Only a player can receive a Portal Rod."));
            return true;
        }
        Player player = (Player) sender;

        if (args.length != 4) {
            sender.sendMessage(ChatColor.RED + "Usage: /" + command.getName() + " <X> <Y> <Z> <Amount>");
            return true;
        }
        int x;
        int y;
        int z;
        int amount;
        try {
            x = Integer.parseInt(args[0]);
            y = Integer.parseInt(args[1]);
            z = Integer.parseInt(args[2]);
            amount = Integer.parseInt(args[3]);
        } catch (NumberFormatException ex) {
            sender.sendMessage(ChatColor.RED + "Coordinates and amount must be whole numbers.");
            return true;
        }
        if (amount < 1 || amount > MAX_PORTALS) {
            sender.sendMessage(ChatColor.RED + "Amount must be between 1 and " + MAX_PORTALS + ".");
            return true;
        }

        if (spawnAroundCoordinates) {
            World world = player.getWorld();
            List<int[]> placed = PortalPlacement.placeAround(world, x, y, z, PortalPlacement.DEFAULT_RADIUS, amount);
            if (placed.size() < amount) {
                sender.sendMessage(ChatColor.RED + "[PortalSpawn] Could not place all portals. Check the coordinates, "
                        + "build height, world border, and clear space.");
                return true;
            }
            sender.sendMessage(ChatColor.GREEN + "[PortalSpawn] " + ChatColor.GRAY + "Successfully spawned " + amount
                    + " portals within a " + PortalPlacement.DEFAULT_RADIUS + "-block radius of "
                    + x + " " + y + " " + z + " in " + world.getName() + ".");
            return true;
        }

        World nether = findNetherWorld(player.getWorld());
        if (nether == null) {
            sender.sendMessage(ChatColor.RED + "[PortalSpawn] The target Nether world is not available.");
            return true;
        }

        ItemStack rod = new ItemStack(Material.FISHING_ROD);
        ItemMeta meta = rod.getItemMeta();
        if (meta == null) {
            sender.sendMessage(ChatColor.RED + "Could not prepare the Portal Rod.");
            return true;
        }
        meta.setDisplayName(ChatColor.GOLD + "Portal Rod");
        meta.setLore(Arrays.asList(ChatColor.GRAY + "Pull to spawn portals at the specified coordinates."));
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        pdc.set(plugin.getRodKey(), PersistentDataType.BYTE, (byte) 1);
        pdc.set(plugin.getRodIdKey(), PersistentDataType.STRING, UUID.randomUUID().toString());
        pdc.set(plugin.getWorldKey(), PersistentDataType.STRING, nether.getName());
        pdc.set(plugin.getXKey(), PersistentDataType.INTEGER, x);
        pdc.set(plugin.getYKey(), PersistentDataType.INTEGER, y);
        pdc.set(plugin.getZKey(), PersistentDataType.INTEGER, z);
        pdc.set(plugin.getAmountKey(), PersistentDataType.INTEGER, amount);
        rod.setItemMeta(meta);

        HashMap<Integer, ItemStack> leftover = player.getInventory().addItem(rod);
        for (ItemStack item : leftover.values()) {
            player.getWorld().dropItemNaturally(player.getLocation(), item);
        }
        sender.sendMessage(ChatColor.GREEN + "[PortalSpawn] " + ChatColor.GRAY + "Portal Rod created for " + amount
                + " portal(s) at " + x + " " + y + " " + z + ".");
        return true;
    }

    /** The player's own world if it is a Nether, else "<world>_nether", else the first Nether world. */
    private static World findNetherWorld(World current) {
        if (current.getEnvironment() == World.Environment.NETHER) {
            return current;
        }
        World byName = Bukkit.getWorld(current.getName() + "_nether");
        if (byName != null && byName.getEnvironment() == World.Environment.NETHER) {
            return byName;
        }
        for (World w : Bukkit.getWorlds()) {
            if (w.getEnvironment() == World.Environment.NETHER) {
                return w;
            }
        }
        return null;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 4) {
            return Arrays.asList("1", "5", "10", "100");
        }
        return new ArrayList<String>();
    }
}
