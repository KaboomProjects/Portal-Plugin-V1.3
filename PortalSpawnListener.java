package com.kodari.portalspawn.listener;

import com.kodari.portalspawn.PortalSpawnPlugin;
import com.kodari.portalspawn.portal.PortalPlacement;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

/** Pulling in a Portal Rod spawns its portals (one use). */
public final class PortalSpawnListener implements Listener {

    private final PortalSpawnPlugin plugin;

    public PortalSpawnListener(PortalSpawnPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerFish(PlayerFishEvent event) {
        PlayerFishEvent.State state = event.getState();
        if (state != PlayerFishEvent.State.REEL_IN
                && state != PlayerFishEvent.State.CAUGHT_FISH
                && state != PlayerFishEvent.State.CAUGHT_ENTITY
                && state != PlayerFishEvent.State.IN_GROUND) {
            return;
        }
        Player player = event.getPlayer();
        PlayerInventory inv = player.getInventory();
        ItemStack hand = inv.getItemInMainHand();
        if (hand == null || hand.getType() != Material.FISHING_ROD || !hand.hasItemMeta()) {
            return;
        }
        ItemMeta meta = hand.getItemMeta();
        if (meta == null) {
            return;
        }
        PersistentDataContainer pdc = meta.getPersistentDataContainer();
        if (!pdc.has(plugin.getRodKey(), PersistentDataType.BYTE)) {
            return;
        }
        event.setCancelled(true);

        String rodId = pdc.get(plugin.getRodIdKey(), PersistentDataType.STRING);
        Integer x = pdc.get(plugin.getXKey(), PersistentDataType.INTEGER);
        Integer y = pdc.get(plugin.getYKey(), PersistentDataType.INTEGER);
        Integer z = pdc.get(plugin.getZKey(), PersistentDataType.INTEGER);
        Integer amount = pdc.get(plugin.getAmountKey(), PersistentDataType.INTEGER);
        String worldName = pdc.get(plugin.getWorldKey(), PersistentDataType.STRING);
        if (rodId == null || x == null || y == null || z == null || amount == null || worldName == null) {
            player.sendMessage(ChatColor.RED + "[PortalSpawn] This Portal Rod has invalid data.");
            return;
        }
        if (plugin.isRodConsumed(rodId)) {
            player.sendMessage(ChatColor.RED + "[PortalSpawn] This Portal Rod has already been used.");
            removeRodCopies(inv, rodId);
            return;
        }
        if (amount < 1 || amount > 100) {
            player.sendMessage(ChatColor.RED + "[PortalSpawn] This Portal Rod has invalid or out-of-range coordinates.");
            return;
        }
        World world = Bukkit.getWorld(worldName);
        if (world == null || world.getEnvironment() != World.Environment.NETHER) {
            player.sendMessage(ChatColor.RED + "[PortalSpawn] The target Nether world is not available.");
            return;
        }

        List<int[]> placed = PortalPlacement.place(world, x, y, z, amount);
        if (placed.size() < amount) {
            player.sendMessage(ChatColor.RED + "[PortalSpawn] Could not place all portals. Check the coordinates, "
                    + "build height, world border, and clear space.");
            return;
        }
        plugin.markRodConsumed(rodId);
        removeRodCopies(inv, rodId);
        player.sendMessage(ChatColor.GREEN + "[PortalSpawn] " + ChatColor.GRAY + "Successfully spawned " + amount
                + " Nether portals!");
    }

    private void removeRodCopies(PlayerInventory inv, String rodId) {
        for (int i = 0; i < inv.getSize(); i++) {
            ItemStack item = inv.getItem(i);
            if (item == null || item.getType() != Material.FISHING_ROD || !item.hasItemMeta()) {
                continue;
            }
            String id = item.getItemMeta().getPersistentDataContainer()
                    .get(plugin.getRodIdKey(), PersistentDataType.STRING);
            if (rodId.equals(id)) {
                inv.setItem(i, new ItemStack(Material.AIR));
            }
        }
    }
}
