package net.frozenorb.potpvp.match.listener;

import javafx.scene.transform.MatrixType;
import net.frozenorb.potpvp.PotPvPSI;
import net.frozenorb.potpvp.kit.Kit;
import net.frozenorb.potpvp.match.Match;
import net.frozenorb.potpvp.match.MatchHandler;
import net.frozenorb.potpvp.match.MatchTeam;
import net.frozenorb.potpvp.match.event.BridgeEnterLavaPortalEvent;
import net.frozenorb.potpvp.match.event.BridgeEnterWaterPortalEvent;
import net.frozenorb.potpvp.util.BridgeUtil;
import net.frozenorb.potpvp.util.CC;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.entity.*;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerItemDamageEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * @author LBuddyBoy (lbuddyboy.me)
 * 02/08/2021 / 8:09 AM
 * potpvp-si / net.frozenorb.potpvp.match.listener
 */
public class MatchBridgeListener implements Listener {

	@EventHandler
	public void onEat(PlayerItemConsumeEvent event) {
		Player player = event.getPlayer();
		MatchHandler matchHandler = PotPvPSI.getInstance().getMatchHandler();
		Match match = matchHandler.getMatchPlaying(player);

		if (match.getKitType().getId().equalsIgnoreCase("bridges") || match.getKitType().getId().equalsIgnoreCase("battlerush")) {
			if (event.getItem() != null) {
				if (event.getItem().getType() == Material.GOLDEN_APPLE) {
					event.getPlayer().setHealth(event.getPlayer().getMaxHealth());
				}
			}
		}
	}

	@EventHandler
	public void onItemDmg(PlayerItemDamageEvent event) {
		Player player = event.getPlayer();
		MatchHandler matchHandler = PotPvPSI.getInstance().getMatchHandler();
		Match match = matchHandler.getMatchPlaying(player);

		if (match.getKitType().getId().equalsIgnoreCase("bridges") || match.getKitType().getId().equalsIgnoreCase("battlerush")) {
			event.setCancelled(true);
		}
	}

	@EventHandler
	public void onDeath(PlayerDeathEvent event) {
		Player player = event.getEntity();
		if (event.getEntity().getKiller() != null) {
			MatchHandler matchHandler = PotPvPSI.getInstance().getMatchHandler();
			Match match = matchHandler.getMatchPlaying(player);

			if (match == null) {
				return;
			}

			if (!match.getKitType().getId().equalsIgnoreCase("bridges") || match.getKitType().getId().equalsIgnoreCase("battlerush"))
				return;

			match.getKills().put(player.getKiller().getUniqueId(), match.getKills().get(player.getKiller().getUniqueId()) + 1);
			player.spigot().respawn();

			event.setKeepInventory(true);
		}
	}

	@EventHandler
	public void onExplode(ExplosionPrimeEvent event) {
		event.setRadius(0);
	}

	@EventHandler
	public void onBurn(BlockBurnEvent event) {
		event.setCancelled(true);
	}

	@EventHandler
	public void onPortalEnter(BridgeEnterLavaPortalEvent event) {
		Match match = event.getMatch();
		Player player = event.getPlayer();

		if (match.getWinner() != null)
			return;
		MatchTeam team = match.getTeams().get(1);
		if (team != null) {
			if (team.getAllMembers().contains(player.getUniqueId())) {
				player.sendMessage(CC.chat("&cYou cannot enter your own portal."));
				player.teleport(match.getArena().getTeam2Spawn());
				return;
			}
			match.getUsedKit().getOrDefault(player.getUniqueId(), Kit.ofDefaultKit(match.getKitType())).apply(player);

			player.teleport(match.getArena().getTeam1Spawn());

			new BukkitRunnable() {
				@Override
				public void run() {
					match.getWins().put(match.getTeam(player.getUniqueId()), match.getWins().get(match.getTeam(player.getUniqueId())) + 1);

					if (match.getWins().get(match.getTeam(player.getUniqueId())) < 5) {
						for (MatchTeam matchTeam : match.getTeams()) {

							for (UUID allMember : matchTeam.getAllMembers()) {
								Player p = Bukkit.getPlayer(allMember);

								Location location;
								if (match.getTeams().get(0).getAllMembers().contains(p.getUniqueId())) {
									location = match.getArena().getTeam1Spawn();
								} else {
									location = match.getArena().getTeam2Spawn();
								}
								p.teleport(location);
								p.sendMessage(CC.chat("&7&m------------------"));
								p.sendMessage(CC.chat("&b" + player.getName() + " &fhas just scored."));
								p.sendMessage(CC.chat(" "));
								p.sendMessage(CC.chat(BridgeUtil.barBuilder(match.getWins().get(match.getTeams().get(0)), "&b")));
								p.sendMessage(CC.chat(BridgeUtil.barBuilder(match.getWins().get(match.getTeams().get(1)), "&b")));
								p.sendMessage(CC.chat("&7&m------------------"));
								p.setMetadata("waiting", new FixedMetadataValue(PotPvPSI.getInstance(), true));

								match.getUsedKit().get(p.getUniqueId()).apply(p);

								p.setHealth(p.getMaxHealth());
								p.updateInventory();

								new BukkitRunnable() {
									@Override
									public void run() {
										match.playSoundAll(Sound.NOTE_PLING, 1.5f);
										p.removeMetadata("waiting", PotPvPSI.getInstance());

									}
								}.runTaskLater(PotPvPSI.getInstance(), 20 * 3);
							}
						}
					}
					match.checkEnded();
				}
			}.runTaskLater(PotPvPSI.getInstance(), 10);
		}
	}

	@EventHandler
	public void onFall(EntityDamageEvent event) {
		if (event.getEntity() instanceof Player) {
			Player player = (Player) event.getEntity();
			MatchHandler matchHandler = PotPvPSI.getInstance().getMatchHandler();
			Match match = matchHandler.getMatchPlayingOrSpectating(player);

			if (match == null) {
				return;
			}

			if (!match.getKitType().getId().equalsIgnoreCase("bridges") || match.getKitType().getId().equalsIgnoreCase("battlerush"))
				return;

			if (event.getCause() == EntityDamageEvent.DamageCause.FALL) {
				event.setCancelled(true);
			}
		}
	}

	@EventHandler
	public void onSaturationLose(FoodLevelChangeEvent event) {
		if (event.getEntity() instanceof Player) {
			Player player = (Player) event.getEntity();
			Match match = PotPvPSI.getInstance().getMatchHandler().getMatchPlaying(player);
			if (match != null) {
				List<String> modes = Arrays.asList("boxing", "sumo", "wizard", "pearlfight", "bridges", "archer", "skywars", "combo");
				if (modes.contains(match.getKitType().getId())) {
					player.setFoodLevel(20);
				}
 			}
		}
	}

	@EventHandler
	public void onLeave(PlayerQuitEvent event) {
		Match match = PotPvPSI.getInstance().getMatchHandler().getMatchPlaying(event.getPlayer());
		if (match != null) {
			if (match.getKitType().getId().equalsIgnoreCase("bridges") || match.getKitType().getId().equalsIgnoreCase("battlerush")) {
				for (MatchTeam team : match.getTeams()) {
					for (UUID member : team.getAllMembers()) {
						if (member != event.getPlayer().getUniqueId()) {
							match.getWins().put(team, 5);
							match.checkEnded();
						}
					}
				}
			}
		}
	}

	@EventHandler
	public void onPortalEnter(BridgeEnterWaterPortalEvent event) {
		Match match = event.getMatch();
		Player player = event.getPlayer();
		if (match.getWinner() != null)
			return;
		MatchTeam team = match.getTeams().get(0);
		if (team != null) {
			if (team.getAllMembers().contains(player.getUniqueId())) {
				player.sendMessage(CC.chat("&cYou cannot enter your own portal."));
				player.teleport(match.getArena().getTeam1Spawn());
				return;
			}
			match.getUsedKit().getOrDefault(player.getUniqueId(), Kit.ofDefaultKit(match.getKitType())).apply(player);

			player.teleport(match.getArena().getTeam2Spawn());

			new BukkitRunnable() {
				@Override
				public void run() {
					match.getWins().put(match.getTeam(player.getUniqueId()), match.getWins().get(match.getTeam(player.getUniqueId())) + 1);

					if (match.getWins().get(match.getTeam(player.getUniqueId())) < 5) {
						for (MatchTeam matchTeam : match.getTeams()) {
							for (UUID allMember : matchTeam.getAllMembers()) {
								Player p = Bukkit.getPlayer(allMember);

								Location location;
								if (match.getTeams().get(0).getAllMembers().contains(p.getUniqueId())) {
									location = match.getArena().getTeam1Spawn();
								} else {
									location = match.getArena().getTeam2Spawn();
								}
								p.teleport(location);
								p.sendMessage(CC.chat("&7&m------------------"));
								p.sendMessage(CC.chat("&b" + player.getName() + " &fhas just scored."));
								p.sendMessage(CC.chat(" "));
								p.sendMessage(CC.chat(BridgeUtil.barBuilder(match.getWins().get(match.getTeams().get(0)), "&b")));
								p.sendMessage(CC.chat(BridgeUtil.barBuilder(match.getWins().get(match.getTeams().get(1)), "&b")));
								p.sendMessage(CC.chat("&7&m------------------"));
								p.setMetadata("waiting", new FixedMetadataValue(PotPvPSI.getInstance(), true));

								match.getUsedKit().getOrDefault(p.getUniqueId(), Kit.ofDefaultKit(match.getKitType())).apply(p);

								p.setHealth(p.getMaxHealth());
								p.updateInventory();

								new BukkitRunnable() {
									@Override
									public void run() {
										match.playSoundAll(Sound.NOTE_PLING, 1.5f);
										p.removeMetadata("waiting", PotPvPSI.getInstance());

									}
								}.runTaskLater(PotPvPSI.getInstance(), 20 * 3);
							}
						}
					}
					match.checkEnded();
				}
			}.runTaskLater(PotPvPSI.getInstance(), 10);
		}
	}

	@EventHandler
	public void onInventoryClick(InventoryClickEvent event) {
		if (event.getWhoClicked().hasMetadata("waiting")) {
			event.setCancelled(true);
		}
	}

	@EventHandler
	public void onShoot(EntityShootBowEvent event) {
		if (event.getEntity() instanceof Player) {
			Player player = (Player) event.getEntity();
			MatchHandler matchHandler = PotPvPSI.getInstance().getMatchHandler();
			Match match = matchHandler.getMatchPlaying(player);

			if (match.getKitType().getId().equalsIgnoreCase("bridges") || match.getKitType().getId().equalsIgnoreCase("battlerush")) {
				new BukkitRunnable() {
					@Override
					public void run() {
						if (!PotPvPSI.getInstance().getMatchHandler().isPlayingMatch(player))
							return;
						player.getInventory().addItem(new ItemStack(Material.ARROW));
						player.sendMessage(CC.chat("&aYou can now use your bow again."));
					}
				}.runTaskLater(PotPvPSI.getInstance(), 20 * 5);
			}
		}
	}
}
