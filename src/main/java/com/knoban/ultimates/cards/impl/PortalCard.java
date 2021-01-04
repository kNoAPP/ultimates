package com.knoban.ultimates.cards.impl;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.knoban.atlas.commandsII.ACAPI;
import com.knoban.atlas.commandsII.annotations.AtlasCommand;
import com.knoban.atlas.world.Coordinate;
import com.knoban.ultimates.Ultimates;
import com.knoban.ultimates.aspects.Message;
import com.knoban.ultimates.cards.Card;
import com.knoban.ultimates.cards.CardInfo;
import com.knoban.ultimates.primal.PrimalSource;
import com.knoban.ultimates.primal.Tier;
import org.bukkit.*;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@CardInfo(
        material = Material.END_PORTAL_FRAME,
        name = "graviton-portal",
        display = "§7Graviton Portal", // Typically we want the color to match the Primal
        description = {"§7You can open a §dportal", "§7between locations that", "§7anyone can use."},
        source = PrimalSource.MOON,
        tier = Tier.EPIC
)
public class PortalCard extends Card {

    private BukkitTask portalLoop;
    private ConcurrentHashMap<UUID, Portal> playerPortals = new ConcurrentHashMap<>();

    public PortalCard(Ultimates plugin) {
        super(plugin);
        ACAPI.getApi().registerCommandsFromClass(plugin, PortalCard.class, this);
    }

    @Override
    protected void register() {
        super.register();
        portalLoop = new BukkitRunnable() {

            private Cache<Entity, Boolean> recentTeleports = CacheBuilder.newBuilder()
                    .expireAfterWrite(8, TimeUnit.SECONDS).build();
            private int i = 0;

            @Override
            public void run() {
                for(Portal portal : playerPortals.values()) {
                    if(portal.isOpen()) {
                        Location a = portal.getA().getLocation();
                        Location b = portal.getB().getLocation();
                        a.getWorld().playSound(a, Sound.BLOCK_PORTAL_AMBIENT, 0.15F, 1.2F);
                        b.getWorld().playSound(b, Sound.BLOCK_PORTAL_AMBIENT, 0.15F, 1.2F);

                        a.getWorld().spawnParticle(Particle.PORTAL, a, 15, 1F, 1F, 1F, 0.5);
                        b.getWorld().spawnParticle(Particle.PORTAL, b, 15, 1F, 1F, 1F, 0.5);

                        if(i % 2 == 0) {
                            Firework aFw = a.getWorld().spawn(a, Firework.class);
                            List<Entity> aEntities = aFw.getNearbyEntities(2, 2, 2);
                            aFw.remove();
                            a.getWorld().playSound(a, Sound.ENTITY_FIREWORK_ROCKET_BLAST, 0.1F, 0.8F);
                            for(Entity en : aEntities) {
                                if(recentTeleports.getIfPresent(en) != null)
                                    continue;

                                en.getWorld().playSound(en.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 0.6F, 0.8F);
                                recentTeleports.put(en, true);
                                en.teleport(b);
                                en.sendMessage("§5You've been teleported by a portal!");
                                en.getWorld().playSound(en.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 0.6F, 0.8F);
                            }

                            Firework bFw = b.getWorld().spawn(b, Firework.class);
                            List<Entity> bEntities = bFw.getNearbyEntities(2, 2, 2);
                            bFw.remove();
                            b.getWorld().playSound(b, Sound.ENTITY_FIREWORK_ROCKET_BLAST, 0.2F, 0.8F);
                            for(Entity en : bEntities) {
                                if(recentTeleports.getIfPresent(en) != null)
                                    continue;

                                en.getWorld().playSound(en.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 0.6F, 0.8F);
                                recentTeleports.put(en, true);
                                en.teleport(a);
                                en.sendMessage("§5You've been teleported by a portal!");
                                en.getWorld().playSound(en.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 0.6F, 0.8F);
                            }
                        }
                        for(double x = 0; x < 2 * Math.PI; x += Math.PI / 24) {
                            a.getWorld().spawnParticle(Particle.CLOUD, a, 0, 0.3 * Math.cos(x), 0F, 0.3 * Math.sin(x), 0.4);
                            b.getWorld().spawnParticle(Particle.CLOUD, b, 0, 0.3 * Math.cos(x), 0F, 0.3 * Math.sin(x), 0.4);
                        }

                        i++;
                    }
                }
            }
        }.runTaskTimer(plugin, 10L, 10L);
    }

    @Override
    protected void unregister() {
        super.unregister();
        portalLoop.cancel();
        portalLoop = null;
    }

    @Override
    public boolean draw(Player p) {
        boolean toRet = super.draw(p);
        if(toRet) {
            cmdPortalBase(p);
        }
        return toRet;
    }

    @Override
    public boolean discard(Player p) {
        boolean toRet = super.discard(p);
        if(toRet) {
            Portal portal = playerPortals.remove(p.getUniqueId());
            writeData(p.getUniqueId(), "portal", portal.serialize(), null);
        }
        return toRet;
    }

    @Override
    public void onDefaultPlayerData(Player p) {
        Portal portal = new Portal();
        writeData(p.getUniqueId(), "portal", portal.serialize(), null);
    }

    @Override
    public void onPlayerData(Player p, Map<String, Object> data) {
        Portal portal = new Portal((Map<String, Object>) data.get("portal"));
        playerPortals.put(p.getUniqueId(), portal);
    }

    @AtlasCommand(paths = {"portal", "portal help", "portal help 1"})
    public void cmdPortalBase(CommandSender sender) {
        if(sender instanceof Player && !drawn.contains(sender)) {
            sender.sendMessage("§cYou must draw the " + info.display() + " §ccard to use this command!");
            return;
        }

        sender.sendMessage(Message.INFO.getMessage("Portal Help"));
        sender.sendMessage(ChatColor.DARK_GREEN + "------------------");
        sender.sendMessage(Message.HELP.getMessage("/portal help - Show all commands for portals"));
        sender.sendMessage(Message.HELP.getMessage("/portal set 1 - Set the 1st portal location"));
        sender.sendMessage(Message.HELP.getMessage("/portal set 2 - Set the 2nd portal location"));
        sender.sendMessage(Message.HELP.getMessage("/portal open - Open the portal"));
        sender.sendMessage(Message.HELP.getMessage("/portal close - Close the portal"));
    }

    @AtlasCommand(paths = {"portal set 1"})
    public void cmdPortalSet1(Player sender) {
        if(!drawn.contains(sender)) {
            sender.sendMessage("§cYou must draw the " + info.display() + " §ccard to use this command!");
            return;
        }

        Portal portal = playerPortals.get(sender.getUniqueId());
        portal.setA(new Coordinate(sender.getLocation().clone().add(0, 0.5, 0)));
        portal.setOpen(false);

        sender.sendMessage("§aPortal 1 §7has been set to your location!");
        sender.sendMessage(portal.getB() != null
                ? "§7Open your portal with §d/portal open§7."
                : "§7Set §aPortal 2's §7location with §d/portal set 2§7.");
        sender.playSound(sender.getLocation(), Sound.BLOCK_END_PORTAL_FRAME_FILL, 0.3F, 0.8F);
    }

    @AtlasCommand(paths = {"portal set 2"})
    public void cmdPortalSet2(Player sender) {
        if(!drawn.contains(sender)) {
            sender.sendMessage("§cYou must draw the " + info.display() + " §ccard to use this command!");
            return;
        }

        Portal portal = playerPortals.get(sender.getUniqueId());
        portal.setB(new Coordinate(sender.getLocation().clone().add(0, 0.5, 0)));
        portal.setOpen(false);

        sender.sendMessage("§aPortal 2 §7has been set to your location!");
        sender.sendMessage(portal.getA() != null
                ? "§7Open your portal with §d/portal open§7."
                : "§7Set §aPortal 1's §7location with §d/portal set 2§7.");
        sender.playSound(sender.getLocation(), Sound.BLOCK_END_PORTAL_FRAME_FILL, 0.3F, 0.8F);
    }

    @AtlasCommand(paths = {"portal open"})
    public void cmdPortalOpen(Player sender) {
        if(!drawn.contains(sender)) {
            sender.sendMessage("§cYou must draw the " + info.display() + " §ccard to use this command!");
            return;
        }

        Portal portal = playerPortals.get(sender.getUniqueId());
        if(portal.getA() == null || portal.getB() == null) {
            sender.sendMessage("§cYou need to set some portal locations first!");
            sender.sendMessage("§7Try §d/portal set 1 §7and §d/portal set 2§7.");
            return;
        }

        if(portal.isOpen()) {
            sender.sendMessage("§cYour portal is already open.");
            return;
        }

        portal.setOpen(true);

        sender.sendMessage("§dThe portal's been opened!");

        Location a = portal.getA().getLocation();
        Location b = portal.getB().getLocation();
        a.getWorld().playSound(a, Sound.BLOCK_END_PORTAL_SPAWN, 0.5F, 0.8F);
        b.getWorld().playSound(b, Sound.BLOCK_END_PORTAL_SPAWN, 0.5F, 0.8F);
}

    @AtlasCommand(paths = {"portal close"})
    public void cmdPortalClose(Player sender) {
        if(!drawn.contains(sender)) {
            sender.sendMessage("§cYou must draw the " + info.display() + " §ccard to use this command!");
            return;
        }

        Portal portal = playerPortals.get(sender.getUniqueId());
        if(!portal.isOpen()) {
            sender.sendMessage("§cYour portal isn't open.");
            return;
        }

        portal.setOpen(false);

        sender.sendMessage("§dThe portal's been closed!");

        Location a = portal.getA().getLocation();
        Location b = portal.getB().getLocation();
        a.getWorld().playSound(a, Sound.ENTITY_GENERIC_EXTINGUISH_FIRE, 0.5F, 0.8F);
        b.getWorld().playSound(b, Sound.ENTITY_GENERIC_EXTINGUISH_FIRE, 0.5F, 0.8F);
    }


    private static class Portal {

        private Coordinate a, b;
        private boolean open;

        public Portal() {}

        public Portal(Map<String, Object> data) {
            String aData = (String) data.get("a");
            if(aData != null)
                this.a = Coordinate.deserialize(aData);

            String bData = (String) data.get("b");
            if(bData != null)
                this.b = Coordinate.deserialize(bData);

            this.open = (boolean) data.getOrDefault("open", false);
        }

        public Coordinate getA() {
            return a;
        }

        public void setA(Coordinate a) {
            this.a = a;
        }

        public Coordinate getB() {
            return b;
        }

        public void setB(Coordinate b) {
            this.b = b;
        }

        public boolean isOpen() {
            return open;
        }

        public void setOpen(boolean open) {
            this.open = open;
        }

        public Map<String, Object> serialize() {
            TreeMap<String, Object> data = new TreeMap<>();
            data.put("a", a != null ? a.serialize() : null);
            data.put("b", a != null ? b.serialize() : null);
            data.put("open", open);

            return data;
        }
    }
}