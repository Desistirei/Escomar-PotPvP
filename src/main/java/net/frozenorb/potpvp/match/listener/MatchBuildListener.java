package net.frozenorb.potpvp.match.listener;

import net.frozenorb.potpvp.PotPvPSI;
import net.frozenorb.potpvp.arena.Arena;
import net.frozenorb.potpvp.extras.ability.Ability;
import net.frozenorb.potpvp.extras.ability.items.Dome;
import net.frozenorb.potpvp.match.Match;
import net.frozenorb.potpvp.match.MatchHandler;
import net.frozenorb.potpvp.match.MatchState;
import net.frozenorb.potpvp.match.MatchTeam;
import net.frozenorb.potpvp.match.event.MatchEndEvent;
import net.frozenorb.potpvp.util.CC;
import net.frozenorb.qlib.cuboid.Cuboid;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockFormEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class MatchBuildListener implements Listener {

	private static final int SEARCH_RADIUS = 3;

	@EventHandler
	public void onBlockBreak(BlockBreakEvent event) {
		Player player = event.getPlayer();
		MatchHandler matchHandler = PotPvPSI.getInstance().getMatchHandler();
		if (!matchHandler.isPlayingMatch(player)) {
			// BasicPreventionListener handles this
			return;
		}
		if (player.hasMetadata("Build"))
			return;

		Match match = matchHandler.getMatchPlaying(player);
		if (match.getKitType().getId().equalsIgnoreCase("baseraiding")) {
			if (player.hasMetadata("trapper")) {
				return;
			}
			event.setCancelled(true);
			return;
		} else if (match.getKitType().getId().equalsIgnoreCase("spleef")) {
			if (event.getBlock().getType() == Material.SNOW_BLOCK || event.getBlock().getType() == Material.DIRT || event.getBlock().getType() == Material.GRASS) {
				return;
			}
			event.setCancelled(true);
			return;
		}
		if (!match.getKitType().isBuildingAllowed() || match.getState() != MatchState.IN_PROGRESS) {
			event.setCancelled(true);
		} else {
			if (!match.canBeBroken(event.getBlock())) {
				event.setCancelled(true);
			}
		}
	}

	@EventHandler
	public void onEnd(MatchEndEvent event) {
		event.getMatch().getTeams().forEach(t -> {
			t.getAllMembers().forEach(m -> {
				Player player = Bukkit.getPlayer(m);
				player.removeMetadata("trapper", PotPvPSI.getInstance());
				PotPvPSI.getInstance().getAbilityHandler().getAbilities().forEach(ability -> {
					ability.cooldown().removeCooldown(player);
				});
				PotPvPSI.getInstance().getAbilityHandler().getPocketbards().forEach(ability -> {
					ability.cooldown().removeCooldown(player);
				});
				PotPvPSI.getInstance().getLogger().info("[PotPvP] Successfully reset the ability cooldowns for " + player.getName());
			});
		});
	}

	@EventHandler
	public void interact(PlayerInteractEvent event) {
		Player player = event.getPlayer();
		Ability dome = PotPvPSI.getInstance().getAbilityHandler().byName("dome");
		if (dome.isSimilar(event.getItem()))
			return;
		MatchHandler matchHandler = PotPvPSI.getInstance().getMatchHandler();
		Match match = matchHandler.getMatchPlayingOrSpectating(player);

		if (match != null) {

			if (match.getSpectators().contains(player.getUniqueId())) {
				event.setUseInteractedBlock(Event.Result.DENY);
				return;
			}

			if (matchHandler.isPlayingMatch(player)) {

				if (match.getKitType().getId().equalsIgnoreCase("baseraiding")) {

					if (!player.hasMetadata("trapper")) {
						if (player.getItemInHand().getType() == Material.POTION || player.getItemInHand().getType() == Material.ENDER_PEARL || player.getItemInHand().getType() == Material.BOW || player.getItemInHand().getType() == Material.COOKED_BEEF || player.getItemInHand().getType() == Material.GOLDEN_APPLE) {
							if (event.getClickedBlock() == null)
								return;
							if (event.getClickedBlock().getType() == Material.FENCE_GATE || event.getClickedBlock().getType().name().contains("CHEST")) {
								if (player.isSneaking())
									return;
								event.setCancelled(true);
								return;
							}
							return;
						}
						event.setCancelled(true);
					}
				}
			}
		}
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void onBlockPlace(BlockPlaceEvent event) {
		Player player = event.getPlayer();
		MatchHandler matchHandler = PotPvPSI.getInstance().getMatchHandler();
		if (!matchHandler.isPlayingMatch(player)) {
			// BasicPreventionListener handles this
			if (player.hasMetadata("Build"))
				return;
			return;
		}

		Match match = matchHandler.getMatchPlaying(player);

		if (match == null) {
			return;
		}

		if (match.getKitType().getId().equalsIgnoreCase("baseraiding")) {
			Ability dome = PotPvPSI.getInstance().getAbilityHandler().byName("dome");
			if (dome.isSimilar(event.getItemInHand()))
				return;
			if (player.hasMetadata("trapper")) {
				Arena arena = match.getArena();
				Cuboid bounds = arena.getBounds();

				if (!bounds.contains(event.getBlockPlaced())) {
					event.setCancelled(true);
					return;
				}

				match.recordPlacedBlock(event.getBlock());

				event.getItemInHand().setAmount(64);
				return;
			}
		}

		if (match.getKitType().getId().equalsIgnoreCase("pearlfight")) {
			new BukkitRunnable() {
				@Override
				public void run() {
					event.getBlockPlaced().setType(Material.AIR);
				}
			}.runTaskLater(PotPvPSI.getInstance(), 20 * 5);
			return;
		}
		if (match.getKitType().getId().equalsIgnoreCase("bridges") || match.getKitType().getId().equalsIgnoreCase("battlerush")) {
			if (event.getBlock().getType() == Material.STATIONARY_LAVA || event.getBlock().getType() == Material.STATIONARY_LAVA) {
				event.setCancelled(true);
				return;
			}
			if (event.getBlock().getType() == Material.WATER || event.getBlock().getType() == Material.STATIONARY_WATER) {
				event.setCancelled(true);
				return;
			}
			if (event.getBlockPlaced().getLocation().getBlockY() >= (match.getArena().getSpectatorSpawn().getY() + 10)) {
				event.setCancelled(true);
				return;
			}
			if (match.getArena().getTeam1Spawn().distance(event.getBlockPlaced().getLocation()) < 10) {
				event.setCancelled(true);
				return;
			}
			if (match.getArena().getTeam2Spawn().distance(event.getBlockPlaced().getLocation()) < 10) {
				event.setCancelled(true);
				return;
			}
			for (int i = 0; i < 5; i++) {
				if (event.getBlockPlaced().getLocation().subtract(0, 1 + i, 0).getBlock().getType().name().contains("LAVA")) {
					event.setCancelled(true);
					player.sendMessage(CC.translate("&cYou cannot place blocks near portals."));
					break;
				}
			}
			for (int i = 0; i < 5; i++) {
				if (event.getBlockPlaced().getLocation().subtract(0, 1 + i, 0).getBlock().getType().name().contains("WATER")) {
					event.setCancelled(true);
					player.sendMessage(CC.translate("&cYou cannot place blocks near portals."));
					break;
				}
			}
		}

		if (!match.getKitType().isBuildingAllowed()) {
			event.setCancelled(true);
			return;
		}

		if (match.getState() != MatchState.IN_PROGRESS) {
			event.setCancelled(true);
			return;
		}

		if (!canBePlaced(event.getBlock(), match)) {
			player.sendMessage(ChatColor.RED + "You can't build here.");
			event.setCancelled(true);
			player.teleport(player.getLocation()); // teleport them back so they can't block-glitch
			return;
		}

		// apparently this is a problem
		if (event.getPlayer().getItemInHand().getType() == Material.FLINT_AND_STEEL && event.getBlockAgainst().getType() == Material.GLASS) {
			event.setCancelled(true);
			return;
		}

		match.recordPlacedBlock(event.getBlock());
	}

	@EventHandler
	public void onBucketEmpty(PlayerBucketEmptyEvent event) {
		Player player = event.getPlayer();
		MatchHandler matchHandler = PotPvPSI.getInstance().getMatchHandler();

		if (!matchHandler.isPlayingMatch(player)) {
			return;
		}

		Match match = matchHandler.getMatchPlaying(player);
		if (player.hasMetadata("trapper")) {
			return;
		}
		if (!match.getKitType().isBuildingAllowed() || match.getState() != MatchState.IN_PROGRESS) {
			event.setCancelled(true);
			return;
		}

		if (!canBePlaced(event.getBlockClicked(), match)) {
			player.sendMessage(ChatColor.RED + "You can't build here.");
			event.setCancelled(true);
		}
	}

	@EventHandler
	public void onBlockForm(BlockFormEvent event) {
		MatchHandler matchHandler = PotPvPSI.getInstance().getMatchHandler();

		for (Match match : matchHandler.getHostedMatches()) {
			if (!match.getArena().getBounds().contains(event.getBlock()) || !match.getKitType().isBuildingAllowed()) {
				continue;
			}

			match.recordPlacedBlock(event.getBlock());
			break;
		}
	}

	private boolean canBePlaced(Block placedBlock, Match match) {
		if (match.getKitType().getId().equalsIgnoreCase("bridges") || match.getKitType().getId().equalsIgnoreCase("battlerush")) {
			return true;
		}
		if (match.getKitType().getId().equalsIgnoreCase("skywars"))
			return true;
		for (int x = -SEARCH_RADIUS; x <= SEARCH_RADIUS; x++) {
			for (int y = -SEARCH_RADIUS; y <= SEARCH_RADIUS; y++) {
				for (int z = -SEARCH_RADIUS; z <= SEARCH_RADIUS; z++) {
					if (x == 0 && y == 0 && z == 0) {
						continue;
					}

					Block current = placedBlock.getRelative(x, y, z);

					if (current.isEmpty()) {
						continue;
					}

					if (isBlacklistedBlock(current)) {
						continue;
					}

					if (isBorderGlass(current, match)) {
						continue;
					}

					if (!match.canBeBroken(current)) {
						return true;
					}
				}
			}
		}

		return false;
	}

	private boolean isBlacklistedBlock(Block block) {
		return block.isLiquid() || block.getType().name().contains("LOG") || block.getType().name().contains("LEAVES");
	}

	private boolean isBorderGlass(Block block, Match match) {
		if (block.getType() != Material.GLASS) {
			return false;
		}

		Cuboid cuboid = match.getArena().getBounds();

		// the reason we do a buffer of 3 blocks here is because sometimes
		// schematics aren't perfectly copied and the glass isn't exactly on the
		// limit of the arena.
		return (getDistanceBetween(block.getX(), cuboid.getLowerX()) <= 3 || getDistanceBetween(block.getX(), cuboid.getUpperX()) <= 3) || (getDistanceBetween(block.getZ(), cuboid.getLowerZ()) <= 3 || getDistanceBetween(block.getZ(), cuboid.getUpperZ()) <= 3);
	}

	private int getDistanceBetween(int x, int z) {
		return Math.abs(x - z);
	}

}