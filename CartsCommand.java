package com.kodari.portalspawn.carts;

import com.kodari.portalspawn.PortalSpawnPlugin;
import com.kodari.portalspawn.portal.PortalPlacement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

/** /portalspawncentrecarts [<X> <Y> <Z> <Amount>] */
public final class CartsCommand implements CommandExecutor, TabCompleter {

    static final int MAX_PORTALS = 100;
    static final int DEFAULT_AMOUNT = 5;

    private final PortalSpawnPlugin plugin;
    private final NamespacedKey rodKey;
    private final NamespacedKey idKey;
    private final NamespacedKey dataKey;

    public CartsCommand(PortalSpawnPlugin plugin) {
        this.plugin = plugin;
        this.rodKey = new NamespacedKey(plugin, "cart_rod");
        this.idKey = new NamespacedKey(plugin, "cart_rod_id");
        this.dataKey = new NamespacedKey(plugin, "cart_rod_portals");
    }

    static String prefix() {
        return ChatColor.GREEN + "[PortalSpawn] " + ChatColor.GRAY;
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
        Player player = (Player) sender;

        Location loc = player.getLocation();
        int x = loc.getBlockX();
        int y = loc.getBlockY();
        int z = loc.getBlockZ();
        int amount = DEFAULT_AMOUNT;

        if (args.length == 4) {
            try {
                x = Integer.parseInt(args[0]);
                y = Integer.parseInt(args[1]);
                z = Integer.parseInt(args[2]);
                amount = Integer.parseInt(args[3]);
            } catch (NumberFormatException ex) {
                sender.sendMessage(ChatColor.RED + "Coordinates and amount must be whole numbers.");
                return true;
            }
        } else if (args.length != 0) {
            sender.sendMessage(ChatColor.RED + "Usage: /" + label + " [<X> <Y> <Z> <Amount>]");
            return true;
        }
        if (amount < 1 || amount > MAX_PORTALS) {
            sender.sendMessage(ChatColor.RED + "Amount must be between 1 and " + MAX_PORTALS + ".");
            return true;
        }

        World world = player.getWorld();
        List<int[]> placed = PortalPlacement.placeAround(world, x, y, z, PortalPlacement.DEFAULT_RADIUS, amount);
        if (placed.isEmpty()) {
            sender.sendMessage(ChatColor.RED + "[PortalSpawn] Could not place any portals. "
                    + "Check the coordinates, build height, world border and clear space.");
            return true;
        }

        ItemStack rod = createRod(world, placed);
        HashMap<Integer, ItemStack> leftover = player.getInventory().addItem(rod);
        for (ItemStack item : leftover.values()) {
            world.dropItemNaturally(player.getLocation(), item);
        }

        sender.sendMessage(prefix() + "Spawned " + placed.size() + " portal(s) within a "
                + PortalPlacement.DEFAULT_RADIUS + "-block radius of " + x + " " + y + " " + z + ".");
        if (placed.size() < amount) {
            sender.sendMessage(ChatColor.YELLOW + "[PortalSpawn] Only " + placed.size() + " of " + amount
                    + " fit (no clear ground found for the rest).");
        }
        sender.sendMessage(prefix() + "You received a Portal Cart Rod. Pull it in to launch the TNT carts!");
        return true;
    }

    private ItemStack createRod(World world, List<int[]> portals) {
        StringBuilder sb = new StringBuilder(world.getName()).append('|');
        for (int i = 0; i < portals.size(); i++) {
            int[] p = portals.get(i);
            if (i > 0) {
                sb.append(';');
            }
            sb.append(p[0]).append(',').append(p[1]).append(',').append(p[2]);
        }

        ItemStack rod = new ItemStack(Material.FISHING_ROD);
        ItemMeta meta = rod.getItemMeta();
        meta.setDisplayName(ChatColor.RED + "Portal Cart Rod");
        meta.setLore(Arrays.asList(ChatColor.GRAY + "Pull to launch TNT carts out of " + portals.size() + " portal(s)."));
        meta.getPersistentDataContainer().set(rodKey, PersistentDataType.BYTE, (byte) 1);
        meta.getPersistentDataContainer().set(idKey, PersistentDataType.STRING, UUID.randomUUID().toString());
        meta.getPersistentDataContainer().set(dataKey, PersistentDataType.STRING, sb.toString());
        rod.setItemMeta(meta);
        return rod;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 4) {
            return Arrays.asList("5", "10", "25", "50", "100");
        }
        return new ArrayList<String>();
    }
}
