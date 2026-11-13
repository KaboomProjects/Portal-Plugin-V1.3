package com.kodari.portalspawn.carts;

import com.kodari.portalspawn.PortalSpawnPlugin;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.entity.FishHook;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPortalEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

public final class CartsListener implements Listener {

    private final PortalSpawnPlugin plugin;
    private final NamespacedKey rodKey;
    private final NamespacedKey idKey;
    private final NamespacedKey dataKey;

    public CartsListener(PortalSpawnPlugin plugin) {
        this.plugin = plugin;
        this.rodKey = new NamespacedKey(plugin, "cart_rod");
        this.idKey = new NamespacedKey(plugin, "cart_rod_id");
        this.dataKey = new NamespacedKey(plugin, "cart_rod_portals");
    }

    private boolean isCartRod(ItemStack item) {
        if (item == null || item.getType() != Material.FISHING_ROD || !item.hasItemMeta()) {
            return false;
        }
        ItemMeta meta = item.getItemMeta();
        return meta != null && meta.getPersistentDataContainer().has(rodKey, PersistentDataType.BYTE);
    }

    @EventHandler(ignoreCancelled = true)
    public void onFish(PlayerFishEvent event) {
        PlayerFishEvent.State state = event.getState();
        if (state != PlayerFishEvent.State.REEL_IN
                && state != PlayerFishEvent.State.CAUGHT_FISH
                && state != PlayerFishEvent.State.CAUGHT_ENTITY
                && state != PlayerFishEvent.State.IN_GROUND) {
            return;
        }

        Player player = event.getPlayer();
        PlayerInventory inv = player.getInventory();
        boolean mainHand = isCartRod(inv.getItemInMainHand());
        ItemStack rod = mainHand ? inv.getItemInMainHand() : inv.getItemInOffHand();
        if (!mainHand && !isCartRod(rod)) {
            return;
        }

        event.setCancelled(true);
        FishHook hook = event.getHook();
        if (hook != null) {
            hook.remove();
        }

        ItemMeta meta = rod.getItemMeta();
        String id = meta.getPersistentDataContainer().get(idKey, PersistentDataType.STRING);
        String data = meta.getPersistentDataContainer().get(dataKey, PersistentDataType.STRING);
        if (id == null || data == null) {
            player.sendMessage(ChatColor.RED + "[PortalSpawn] This Portal Cart Rod has invalid data.");
            return;
        }
        if (plugin.isRodConsumed(id)) {
            player.sendMessage(ChatColor.RED + "[PortalSpawn] This Portal Cart Rod has already been used.");
            removeRod(inv, mainHand);
            return;
        }

        int sep = data.lastIndexOf('|');
        World world = sep > 0 ? Bukkit.getWorld(data.substring(0, sep)) : null;
        List<int[]> portals = new ArrayList<int[]>();
        if (world != null) {
            try {
                for (String part : data.substring(sep + 1).split(";")) {
                    String[] c = part.split(",");
                    portals.add(new int[] {Integer.parseInt(c[0]), Integer.parseInt(c[1]), Integer.parseInt(c[2])});
                }
            } catch (RuntimeException ex) {
                portals.clear();
            }
        }
        if (world == null || portals.isEmpty()) {
            player.sendMessage(ChatColor.RED + "[PortalSpawn] The portals of this rod are not available.");
            return;
        }

        plugin.markRodConsumed(id);
        removeRod(inv, mainHand);

        int perPortal = Math.max(1, plugin.getConfig().getInt("carts.per-portal", 50));
        int perBurst = Math.max(1, plugin.getConfig().getInt("carts.per-burst", 1));
        int intervalTicks = Math.max(1, (int) Math.round(plugin.getConfig().getDouble("carts.interval-seconds", 1.0) * 20.0));
        float power = (float) plugin.getConfig().getDouble("carts.explosion-power", 4.0);

        CartLauncher.launch(plugin, world, portals, perPortal, perBurst, intervalTicks, power);
        player.sendMessage(CartsCommand.prefix() + "TNT carts are coming out of " + portals.size() + " portal(s)!");
    }

    private void removeRod(PlayerInventory inv, boolean mainHand) {
        if (mainHand) {
            inv.setItemInMainHand(new ItemStack(Material.AIR));
        } else {
            inv.setItemInOffHand(new ItemStack(Material.AIR));
        }
    }

    /** Stops the carts from being sent through the portals into the Nether. */
    @EventHandler(ignoreCancelled = true)
    public void onPortal(EntityPortalEvent event) {
        if (event.getEntity().getScoreboardTags().contains(CartLauncher.TAG)) {
            event.setCancelled(true);
        }
    }
}
