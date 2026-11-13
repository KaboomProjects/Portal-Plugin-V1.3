package com.kodari.portalspawn.carts;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

/** Shoots TNT minecarts out of portals and blows them up when they hit the ground. */
public final class CartLauncher {

    public static final String TAG = "portalspawn_cart";

    private CartLauncher() {
    }

    public static void launch(Plugin plugin, World world, List<int[]> portals,
            int perPortal, int perBurst, int intervalTicks, float power) {
        new Session(world, portals, perPortal, perBurst, Math.max(1, intervalTicks), power)
                .runTaskTimer(plugin, 0L, 1L);
    }

    /** Rains TNT carts down at random spots inside a radius around a centre. */
    public static void launchArea(Plugin plugin, World world, int cx, int cy, int cz, int radius,
            int amount, int perBurst, int intervalTicks, float power) {
        Session session = new Session(world, new ArrayList<int[]>(), amount, perBurst,
                Math.max(1, intervalTicks), power);
        session.area = new int[] {cx, cy, cz, radius};
        session.runTaskTimer(plugin, 0L, 1L);
    }

    private static final class Cart {
        final Entity entity;
        int age;

        Cart(Entity entity) {
            this.entity = entity;
        }
    }

    private static final class Session extends BukkitRunnable {
        private final World world;
        private final List<int[]> portals;
        private final int perPortal;
        private final int perBurst;
        private final int interval;
        private final float power;
        private final List<Cart> carts = new ArrayList<Cart>();
        private int tick;
        private int emitted;
        int[] area;

        Session(World world, List<int[]> portals, int perPortal, int perBurst, int interval, float power) {
            this.world = world;
            this.portals = portals;
            this.perPortal = perPortal;
            this.perBurst = Math.max(1, perBurst);
            this.interval = interval;
            this.power = power;
        }

        @Override
        public void run() {
            if (emitted < perPortal && tick % interval == 0) {
                int count = Math.min(perBurst, perPortal - emitted);
                if (area != null) {
                    for (int i = 0; i < count; i++) {
                        spawnAreaCart();
                    }
                } else {
                    for (int[] portal : portals) {
                        for (int i = 0; i < count; i++) {
                            spawnCart(portal);
                        }
                    }
                }
                emitted += count;
            }
            tick++;

            Iterator<Cart> it = carts.iterator();
            while (it.hasNext()) {
                Cart cart = it.next();
                if (!cart.entity.isValid()) {
                    it.remove();
                    continue;
                }
                cart.age++;
                if (cart.age >= 8 && (cart.entity.isOnGround() || cart.age >= 100)) {
                    Location at = cart.entity.getLocation();
                    cart.entity.remove();
                    world.createExplosion(at, power, false, true);
                    it.remove();
                }
            }

            if (emitted >= perPortal && carts.isEmpty()) {
                cancel();
            }
        }

        private void spawnAreaCart() {
            ThreadLocalRandom rnd = ThreadLocalRandom.current();
            double angle = rnd.nextDouble() * Math.PI * 2.0;
            double dist = Math.sqrt(rnd.nextDouble()) * area[3];
            int x = area[0] + (int) Math.round(Math.cos(angle) * dist);
            int z = area[2] + (int) Math.round(Math.sin(angle) * dist);
            int y;
            if (world.getEnvironment() == World.Environment.NORMAL) {
                y = world.getHighestBlockYAt(x, z) + 10;
            } else {
                y = area[1] + 8;
            }
            y = Math.min(y, world.getMaxHeight() - 2);
            Location at = new Location(world, x + 0.5, y, z + 0.5);
            Entity cart = world.spawnEntity(at, EntityType.TNT_MINECART);
            cart.addScoreboardTag(TAG);
            world.spawnParticle(Particle.PORTAL, at, 20, 0.4, 0.6, 0.4, 0.2);
            carts.add(new Cart(cart));
        }

        private void spawnCart(int[] portal) {
            ThreadLocalRandom rnd = ThreadLocalRandom.current();
            int side = rnd.nextBoolean() ? 1 : -1;
            // The portal plane runs along X, so carts leave through the Z sides.
            // Spawning slightly outside the plane stops them from being teleported.
            Location at = new Location(world,
                    portal[0] + 2.0 + (rnd.nextDouble() - 0.5) * 1.5,
                    portal[1] + 1.3,
                    portal[2] + 0.5 + side * 1.6);
            Entity cart = world.spawnEntity(at, EntityType.TNT_MINECART);
            cart.addScoreboardTag(TAG);
            cart.setVelocity(new Vector(
                    (rnd.nextDouble() - 0.5) * 0.4,
                    0.45 + rnd.nextDouble() * 0.35,
                    side * (0.25 + rnd.nextDouble() * 0.4)));
            world.spawnParticle(Particle.PORTAL, at, 20, 0.4, 0.6, 0.4, 0.2);
            carts.add(new Cart(cart));
        }
    }
}
