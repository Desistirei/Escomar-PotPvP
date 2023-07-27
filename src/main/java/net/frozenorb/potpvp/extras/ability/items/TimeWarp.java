package net.frozenorb.potpvp.extras.ability.items;

import net.frozenorb.potpvp.PotPvPSI;
import net.frozenorb.potpvp.extras.ability.Ability;
import net.frozenorb.potpvp.extras.ability.tasks.TimewarpTask;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.EnderPearl;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.scheduler.BukkitRunnable;
import net.frozenorb.potpvp.util.CC;
import net.frozenorb.potpvp.util.cooldown.Cooldowns;

import java.util.*;

/**
 * Copyright LBuddyBoy
 * Made by LBuddyBoy
 * on 2/17/2021, 4:37 PM
 */
public class TimeWarp extends Ability {

    public static HashMap<Player, Location> timewarp = new HashMap<>();
    public static HashMap<UUID, Integer> bukkitStore = new HashMap<>();
    public static List<UUID> justClicked = new ArrayList<>();
    public Cooldowns cd = new Cooldowns();

    @Override
    public Cooldowns cooldown() {
        return cd;
    }

    @Override
    public List<String> lore() {
        return CC.translate(Arrays.asList(
                " ",
                "&7After clicking this if you have pearled",
                "&7within the last 15 seconds you will be teleported",
                "&7to where you pearled.",
                " "
        ));
    }

    @Override
    public List<String> foundInfo() {
        return CC.translate(Arrays.asList(
                "Ability Packages",
                "Partner Crates"
        ));
    }

    @Override
    public String displayName() {
        return CC.chat("&b&lTimeWarp");
    }

    @Override
    public String name() {
        return "timewarp";
    }

    @Override
    public int data() {
        return 0;
    }

    @Override
    public Material mat() {
        return Material.FEATHER;
    }

    @Override
    public boolean glow() {
        return true;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (event.getItem() == null)
            return;
        if (event.getAction().name().contains("RIGHT")) {
            if (isSimilar(event.getItem())) {
                if (!canUse(player)) {
                    return;
                }
                if (timewarp.isEmpty() || !timewarp.containsKey(player)) {
                    player.sendMessage(CC.chat("&cYou have not pearled in the last 20 seconds!"));
                    return;
                }
                Location l = timewarp.get(player);
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        player.teleport(l);
                        timewarp.remove(player);
                    }
                }.runTaskLater(PotPvPSI.getInstance(), 20 * 3);
                addCooldown(player, 60);
                event.setCancelled(true);
                takeItem(player);

                List<String> hitMsg = Arrays.asList(
                        "",
                        "&bYou &fhave just activated a " + displayName() + "&f.",
                        " ",
                        "&7┃ &fYou are being teleport to your last pearl.",
                        "");

                hitMsg.forEach(s -> player.sendMessage(CC.chat(s)));

            }
        }
    }

    @EventHandler
    public void onPearl(ProjectileLaunchEvent event) {
        if (event.getEntity() instanceof EnderPearl) {
            if (event.getEntity().getShooter() instanceof Player) {
                Player shooter = (Player) event.getEntity().getShooter();
                TimewarpTask task = new TimewarpTask(shooter);
                if (timewarp.containsKey(shooter)) {
                    Bukkit.getScheduler().cancelTask(bukkitStore.get(shooter.getUniqueId()));
                    timewarp.remove(shooter);
                }

                timewarp.put(shooter, shooter.getLocation());
                task.runTaskLater(PotPvPSI.getInstance(), 20 * 20);
                bukkitStore.put(shooter.getUniqueId(), task.getTaskId());
            }
        }
    }

}
