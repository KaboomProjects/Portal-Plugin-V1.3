package com.kodari.portalspawn.portal;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import org.bukkit.Axis;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Orientable;

/** Places standard 4x5 nether portals around a centre and returns their frame corners. */
public final class PortalPlacement {

    public static final int PORTAL_WIDTH = 4;
    public static final int PORTAL_HEIGHT = 5;
    public static final int PORTAL_SPACING = 6;
    public static final int DEFAULT_RADIUS = 100;

    private PortalPlacement() {
    }

    /** Places portals close to the given coordinates (radius grows with the amount). */
    public static List<int[]> place(World world, int x, int y, int z, int amount) {
        int radius = Math.max(PORTAL_SPACING, (int) Math.ceil(Math.sqrt(amount) * PORTAL_SPACING));
        return placeAround(world, x, y, z, radius, amount);
    }

    /** Returns one {x, y, z} per placed portal (lowest corner of the frame). */
    public static List<int[]> placeAround(World world, int cx, int cy, int cz, int radius, int amount) {
        List<int[]> placed = new ArrayList<int[]>();
        ThreadLocalRandom rnd = ThreadLocalRandom.current();
        int attempts = amount * 40 + 40;
        for (int a = 0; a < attempts && placed.size() < amount; a++) {
            double angle = rnd.nextDouble() * Math.PI * 2.0;
            double dist = Math.sqrt(rnd.nextDouble()) * radius;
            int x = cx + (int) Math.round(Math.cos(angle) * dist);
            int z = cz + (int) Math.round(Math.sin(angle) * dist);
            if (tooClose(placed, x, z)) {
                continue;
            }
            int baseY = findBaseY(world, x, z, cy);
            if (baseY == Integer.MIN_VALUE) {
                continue;
            }
            if (baseY < world.getMinHeight() || baseY + PORTAL_HEIGHT >= world.getMaxHeight()) {
                continue;
            }
            if (!world.getWorldBorder().isInside(new Location(world, x, baseY, z))
                    || !world.getWorldBorder().isInside(new Location(world, x + PORTAL_WIDTH, baseY, z))) {
                continue;
            }
            build(world, x, baseY, z);
            placed.add(new int[] {x, baseY, z});
        }
        return placed;
    }

    private static boolean tooClose(List<int[]> placed, int x, int z) {
        for (int[] p : placed) {
            if (Math.abs(p[0] - x) < PORTAL_SPACING && Math.abs(p[2] - z) < PORTAL_SPACING) {
                return true;
            }
        }
        return false;
    }

    private static int findBaseY(World world, int x, int z, int cy) {
        int min = world.getMinHeight();
        int max = world.getMaxHeight();
        int start;
        if (world.getEnvironment() == World.Environment.NORMAL) {
            start = world.getHighestBlockYAt(x, z);
        } else {
            start = Math.min(Math.max(cy, min + 2), max - PORTAL_HEIGHT - 3);
        }
        for (int y = start; y > min + 1 && y > start - 64; y--) {
            Block ground = world.getBlockAt(x, y, z);
            if (!ground.getType().isSolid()) {
                continue;
            }
            boolean clear = true;
            for (int h = 1; h <= PORTAL_HEIGHT; h++) {
                Block b = world.getBlockAt(x, y + h, z);
                if (!b.isPassable() || b.isLiquid()) {
                    clear = false;
                    break;
                }
            }
            if (clear) {
                return y + 1;
            }
        }
        return Integer.MIN_VALUE;
    }

    private static void build(World world, int x, int y, int z) {
        BlockData portalData = Bukkit.createBlockData(Material.NETHER_PORTAL);
        if (portalData instanceof Orientable) {
            ((Orientable) portalData).setAxis(Axis.X);
        }
        for (int dx = 0; dx < PORTAL_WIDTH; dx++) {
            for (int dy = 0; dy < PORTAL_HEIGHT; dy++) {
                for (int dz = -2; dz <= 2; dz++) {
                    Block b = world.getBlockAt(x + dx, y + dy, z + dz);
                    boolean frame = dz == 0 && (dy == 0 || dy == PORTAL_HEIGHT - 1 || dx == 0 || dx == PORTAL_WIDTH - 1);
                    boolean inside = dz == 0 && !frame;
                    if (frame) {
                        b.setType(Material.OBSIDIAN, false);
                    } else if (inside) {
                        b.setBlockData(portalData, false);
                    } else {
                        b.setType(Material.AIR, false);
                    }
                }
            }
        }
    }
}
