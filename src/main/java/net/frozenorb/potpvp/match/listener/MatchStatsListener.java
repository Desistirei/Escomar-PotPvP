package net.frozenorb.potpvp.match.listener;

import java.util.Map;
import java.util.UUID;

import net.frozenorb.potpvp.match.MatchEndReason;
import net.frozenorb.potpvp.match.MatchTeam;
import net.frozenorb.potpvp.match.event.MatchEndEvent;
import net.frozenorb.potpvp.match.event.MatchStartEvent;
import net.frozenorb.potpvp.profile.Profile;
import org.bukkit.Bukkit;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PotionSplashEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.projectiles.ProjectileSource;

import net.frozenorb.potpvp.PotPvPSI;
import net.frozenorb.potpvp.match.Match;

public class MatchStatsListener implements Listener {

	@EventHandler(priority = EventPriority.MONITOR)
	public void onDamage(EntityDamageByEntityEvent event) {
		if (!(event.getEntity() instanceof Player) || !(event.getDamager() instanceof Player)) return;
		if (event.getDamager() instanceof EnderPearl)
			return;

		Player damager = (Player) event.getDamager();
		Player damaged = (Player) event.getEntity();

		Match damagerMatch = PotPvPSI.getInstance().getMatchHandler().getMatchPlaying(damager);

		if (damagerMatch == null) return;

		MatchTeam damagerTeam = damagerMatch.getTeam(damager.getUniqueId());
		MatchTeam damagedTeam = damagerMatch.getTeam(damaged.getUniqueId());

		Map<UUID, UUID> lastHitMap = damagerMatch.getLastHit();
		Map<UUID, Integer> combos = damagerMatch.getCombos();
		Map<UUID, Integer> totalHits = damagerMatch.getTotalHits();
		Map<UUID, Integer> longestCombo = damagerMatch.getLongestCombo();

		if (damagerMatch.getBoxingCombos().getOrDefault(damagedTeam, 0) > 0) {
			damagerMatch.getBoxingCombos().put(damagedTeam, 0);
		}

		UUID lastHit = lastHitMap.put(damager.getUniqueId(), damaged.getUniqueId());
		if (lastHit != null) {
			if (lastHit.equals(damaged.getUniqueId())) {
				combos.put(damager.getUniqueId(), combos.getOrDefault(damager.getUniqueId(), 0) + 1);
				damagerMatch.getBoxingCombos().put(damagerTeam, damagerMatch.getBoxingCombos().getOrDefault(damagerTeam, 0) + 1);
			} else {
				combos.put(damager.getUniqueId(), 1);
				damagerMatch.getBoxingCombos().put(damagerTeam, 1);
			}

			longestCombo.put(damager.getUniqueId(), Math.max(combos.get(damager.getUniqueId()), longestCombo.getOrDefault(damager.getUniqueId(), 1)));
		} else {
			combos.put(damager.getUniqueId(), 0);
			damagerMatch.getBoxingCombos().put(damagerTeam, 0);
		}


		totalHits.put(damager.getUniqueId(), totalHits.getOrDefault(damager.getUniqueId(), 0) + 1);
		damagerMatch.getHits().put(damagerTeam, damagerMatch.getHits().getOrDefault(damagerTeam, 0) + 1);

		Profile profile = PotPvPSI.getInstance().getProfileManager().getProfile(damager);

		if (damagerMatch.getKitType().getId().equalsIgnoreCase("boxing")) {
			if (damagerMatch.getHits().getOrDefault(damagerTeam, 0) >= 100) {
				damagerMatch.markDead(damaged);
				damagerMatch.addSpectator(damaged, null, true);
				damaged.teleport(damagerMatch.getArena().getSpectatorSpawn());
				return;
			}
		}

		if (profile.getHighestCombo() < longestCombo.getOrDefault(damager.getUniqueId(), 0)) {
			profile.setHighestCombo(longestCombo.get(damager.getUniqueId()));
			profile.save();
			return;
		}

		while (lastHitMap.values().remove(damager.getUniqueId())) ;
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onPotionLaunch(ProjectileLaunchEvent event) {
		Projectile thrownEntity = event.getEntity();
		if (!(thrownEntity instanceof ThrownPotion)) return;

		ThrownPotion thrownPotion = (ThrownPotion) thrownEntity;

		ProjectileSource projectileSource = thrownPotion.getShooter();
		if (!(projectileSource instanceof Player)) return;

		Player player = (Player) projectileSource;
		Match match = PotPvPSI.getInstance().getMatchHandler().getMatchPlaying(player);

		if (match == null) return;
		match.getMissedPots().put(player.getUniqueId(), match.getMissedPots().getOrDefault(player.getUniqueId(), 0) + 1);
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onSplash(PotionSplashEvent event) {
		ThrownPotion thrownPotion = event.getEntity();

		if (thrownPotion.getItem().getDurability() != 16421) return; // now we know it's a health pot!

		ProjectileSource projectileSource = thrownPotion.getShooter();
		if (!(projectileSource instanceof Player)) return;

		Player player = (Player) projectileSource;
		Match match = PotPvPSI.getInstance().getMatchHandler().getMatchPlaying(player);

		if (match == null) return;

		for (LivingEntity affectedEntity : event.getAffectedEntities()) {
			if (!affectedEntity.getUniqueId().equals(player.getUniqueId())) continue;

			if (event.getIntensity(affectedEntity) == 1.0D) {
				match.getMissedPots().put(player.getUniqueId(), Math.max(match.getMissedPots().getOrDefault(player.getUniqueId(), 1) - 1, 0));
			}
		}
	}
/*
    @EventHandler(priority = EventPriority.MONITOR)
    public void onMatchEnd(MatchEndEvent event) {
        Match match = event.getMatch();
        match.getTeams().forEach(team -> {
            if (match.getWinner() == team) {
                team.getAllMembers().forEach(PotPvPSI.getInstance().getWinsMap()::incrementWins);
            } else {
                team.getAllMembers().forEach(PotPvPSI.getInstance().getLossMap()::incrementLosses);
            }
        });
    }
    */
}
