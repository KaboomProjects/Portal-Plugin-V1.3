package com.kodari.portalspawn;

import com.kodari.portalspawn.carts.CartsCommand;
import com.kodari.portalspawn.carts.CartsListener;
import com.kodari.portalspawn.carts.TntCommand;
import com.kodari.portalspawn.command.DimensionCalculatorCommand;
import com.kodari.portalspawn.command.PortalSpawnCommand;
import com.kodari.portalspawn.listener.PortalSpawnListener;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Set;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public final class PortalSpawnPlugin extends JavaPlugin {

    private static final int MAX_STORED_ROD_IDS = 5000;

    private final Set<String> consumedRodIds = new LinkedHashSet<String>();
    private NamespacedKey rodKey;
    private NamespacedKey rodIdKey;
    private NamespacedKey worldKey;
    private NamespacedKey xKey;
    private NamespacedKey yKey;
    private NamespacedKey zKey;
    private NamespacedKey amountKey;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        rodKey = new NamespacedKey(this, "portal_rod");
        rodIdKey = new NamespacedKey(this, "portal_rod_id");
        worldKey = new NamespacedKey(this, "portal_world");
        xKey = new NamespacedKey(this, "portal_x");
        yKey = new NamespacedKey(this, "portal_y");
        zKey = new NamespacedKey(this, "portal_z");
        amountKey = new NamespacedKey(this, "portal_amount");

        consumedRodIds.addAll(getConfig().getStringList("consumed-rod-ids"));

        register("portalspawn", new PortalSpawnCommand(this, false));
        register("portalspawncentre", new PortalSpawnCommand(this, true));
        register("calculate", new DimensionCalculatorCommand());

        CartsCommand carts = new CartsCommand(this);
        register("portalspawncentrecarts", carts, carts);
        TntCommand tnt = new TntCommand(this);
        register("portalspawntnt", tnt, tnt);

        Bukkit.getPluginManager().registerEvents(new PortalSpawnListener(this), this);
        Bukkit.getPluginManager().registerEvents(new CartsListener(this), this);
    }

    private void register(String name, org.bukkit.command.CommandExecutor executor) {
        register(name, executor, executor instanceof org.bukkit.command.TabCompleter
                ? (org.bukkit.command.TabCompleter) executor : null);
    }

    private void register(String name, org.bukkit.command.CommandExecutor executor,
            org.bukkit.command.TabCompleter completer) {
        PluginCommand command = getCommand(name);
        if (command == null) {
            return;
        }
        command.setExecutor(executor);
        if (completer != null) {
            command.setTabCompleter(completer);
        }
    }

    public NamespacedKey getRodKey() { return rodKey; }
    public NamespacedKey getRodIdKey() { return rodIdKey; }
    public NamespacedKey getWorldKey() { return worldKey; }
    public NamespacedKey getXKey() { return xKey; }
    public NamespacedKey getYKey() { return yKey; }
    public NamespacedKey getZKey() { return zKey; }
    public NamespacedKey getAmountKey() { return amountKey; }

    public boolean isRodConsumed(String rodId) {
        return consumedRodIds.contains(rodId);
    }

    public void markRodConsumed(String rodId) {
        consumedRodIds.add(rodId);
        ArrayList<String> ids = new ArrayList<String>(consumedRodIds);
        if (ids.size() > MAX_STORED_ROD_IDS) {
            ids = new ArrayList<String>(ids.subList(ids.size() - MAX_STORED_ROD_IDS, ids.size()));
            consumedRodIds.clear();
            consumedRodIds.addAll(ids);
        }
        getConfig().set("consumed-rod-ids", ids);
        saveConfig();
    }
}
