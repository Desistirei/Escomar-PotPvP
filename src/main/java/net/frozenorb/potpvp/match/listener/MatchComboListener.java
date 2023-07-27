package net.frozenorb.potpvp.match.listener;

import java.util.Objects;

import net.frozenorb.potpvp.match.kbprofile.ComboProfile;
import net.frozenorb.potpvp.match.event.MatchEndEvent;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import net.frozenorb.potpvp.match.Match;
import net.frozenorb.potpvp.match.event.MatchStartEvent;
import org.bukkit.event.player.PlayerItemDamageEvent;
import org.spigotmc.SpigotConfig;

public class MatchComboListener implements Listener {

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onStart(MatchStartEvent event) {
		Match match = event.getMatch();

		int noDamageTicks = match.getKitType().getId().contains("combo") ? 3 : 19;
		match.getTeams().forEach(team -> {
			team.getAliveMembers().stream().map(Bukkit::getPlayer).filter(Objects::nonNull).forEach(p -> {
				p.setMaximumNoDamageTicks(noDamageTicks);
				if (match.getKitType().getId().contains("combo")) {
					p.setKbProfile(SpigotConfig.getKbProfileByName("Combo"));
				} else {
					p.setKbProfile(SpigotConfig.getKbProfileByName("Default"));

				}
			});
		});
	}

	@EventHandler
	public void onEnd(MatchEndEvent event) {

	}

	@EventHandler
	public void itemDmg(PlayerItemDamageEvent event) {
		if (!event.getItem().getType().name().contains("SWORD")) {
			if (event.getPlayer().getMaximumNoDamageTicks() == 3) {
				event.setDamage(event.getDamage() + 1);
			}
		}
	}
	}
