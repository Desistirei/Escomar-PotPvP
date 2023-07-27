package net.frozenorb.potpvp.match.listener;

import java.util.UUID;

import net.frozenorb.potpvp.listener.PearlCooldownListener;
import net.frozenorb.potpvp.match.*;
import net.frozenorb.potpvp.match.event.BridgeEnterLavaPortalEvent;
import net.frozenorb.potpvp.match.event.BridgeEnterWaterPortalEvent;
import net.frozenorb.potpvp.match.event.MatchEndEvent;
import net.frozenorb.potpvp.util.BridgeUtil;
import net.frozenorb.potpvp.util.CC;
import org.bukkit.*;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.EnderPearl;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Snowball;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.ItemStack;

import net.frozenorb.potpvp.PotPvPSI;
import net.frozenorb.potpvp.arena.Arena;
import net.frozenorb.potpvp.nametag.PotPvPNametagProvider;
import net.frozenorb.qlib.cuboid.Cuboid;
import net.frozenorb.qlib.util.PlayerUtils;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;

public final class MatchGeneralListener implements Listener {

	@EventHandler
	public void onPlayerDeath(PlayerDeathEvent event) {
		MatchHandler matchHandler = PotPvPSI.getInstance().getMatchHandler();
		Player player = event.getEntity();
		Match match = matchHandler.getMatchPlaying(player);

		if (match == null) {
			return;
		}

		if (match.getKitType().getId().equalsIgnoreCase("bridges") || match.getKitType().getId().equalsIgnoreCase("battlerush"))
			return;

		match.markDead(player);
		match.addSpectator(player, null, true);
		player.teleport(player.getLocation().add(0, 2, 0));

		// if we're ending the match we don't drop pots/bowls
		if (match.getState() == MatchState.ENDING) {
			event.getDrops().removeIf(i -> i.getType() == Material.POTION || i.getType() == Material.GLASS_BOTTLE || i.getType() == Material.MUSHROOM_SOUP || i.getType() == Material.BOWL);
		}
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void onPlayerQuit(PlayerQuitEvent event) {
		MatchHandler matchHandler = PotPvPSI.getInstance().getMatchHandler();
		Player player = event.getPlayer();
		Match match = matchHandler.getMatchPlaying(player);

		player.removeMetadata("trapper", PotPvPSI.getInstance());

		if (match == null) {
			return;
		}

		MatchState state = match.getState();

		if (state == MatchState.COUNTDOWN || state == MatchState.IN_PROGRESS) {
			for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
				UUID onlinePlayerUuid = onlinePlayer.getUniqueId();

				// if this player has no relation to the match skip
				if (match.getTeam(onlinePlayerUuid) == null && !match.isSpectator(onlinePlayerUuid)) {
					continue;
				}

				ChatColor playerColor = PotPvPNametagProvider.getNameColor(player, onlinePlayer);
				String playerFormatted = playerColor + player.getName();

				onlinePlayer.sendMessage(playerFormatted + ChatColor.GRAY + " disconnected.");
			}
		}

		// run this regardless of match state
		match.markDead(player);
		if (player.getKiller() != null) {
			for (int i = 0; i < 6; ++i) {
				PotPvPSI.getInstance().getKillEffectHandler().playEffect(player.getKiller(), player);
			}
		}
	}

	@EventHandler
	public void onTeleport(PlayerTeleportEvent event) {
		if (event.getCause() == PlayerTeleportEvent.TeleportCause.ENDER_PEARL) {
			if (event.getPlayer().hasMetadata("waiting")) {
				event.setCancelled(true);
			}
		}
	}

	@EventHandler
	public void onPlayerMove(PlayerMoveEvent event) {
		Player player = event.getPlayer();
		Location from = event.getFrom();
		Location to = event.getTo();

		if (
				from.getBlockX() == to.getBlockX() &&
						from.getBlockY() == to.getBlockY() &&
						from.getBlockZ() == to.getBlockZ()
		) {
			return;
		}

		MatchHandler matchHandler = PotPvPSI.getInstance().getMatchHandler();
		Match match = matchHandler.getMatchPlayingOrSpectating(player);

		if (match == null) {
			return;
		}

		if (match.getKitType().getId().equalsIgnoreCase("spleef")) {
			if (event.getTo().getY() < (match.getArena().getTeam1Spawn().getY() - 8)) {
				match.markDead(player);
				match.addSpectator(player, null, true);
				return;
			}
		}

		if (event.getTo().getY() < (match.getArena().getTeam1Spawn().getY() - 18)) {
			if (match.getKitType().getId().equalsIgnoreCase("bridges") || match.getKitType().getId().equalsIgnoreCase("battlerush")) {
				match.markDead(player);
				return;
			}
		}

		if (event.getTo().getBlock().getType() == Material.LAVA || event.getTo().getBlock().getType() == Material.STATIONARY_LAVA) {
			if (match.getKitType().getId().equalsIgnoreCase("bridges") || match.getKitType().getId().equalsIgnoreCase("battlerush")) {
				BridgeEnterWaterPortalEvent bridgeEnterWaterPortalEvent = new BridgeEnterWaterPortalEvent(player, match);
				Bukkit.getPluginManager().callEvent(bridgeEnterWaterPortalEvent);
				return;
			}
		}

		if (event.getTo().getBlock().getType() == Material.WATER || event.getTo().getBlock().getType() == Material.STATIONARY_WATER) {
			if (match.getKitType().getId().equalsIgnoreCase("bridges") || match.getKitType().getId().equalsIgnoreCase("battlerush")) {
				BridgeEnterLavaPortalEvent bridgeEnterLavaPortalEvent = new BridgeEnterLavaPortalEvent(player, match);
				Bukkit.getPluginManager().callEvent(bridgeEnterLavaPortalEvent);
				return;
			}
		}

		if (match.getKitType().getId().equalsIgnoreCase("PEARLFIGHT")) {
			if ((player.getLocation().getY() < match.getArena().getTeam1Spawn().getY() - 10)) {
				player.teleport(match.getArena().getSpectatorSpawn());
				new BukkitRunnable() {
					@Override
					public void run() {
						match.markDead(player);
					}
				}.runTaskLater(PotPvPSI.getInstance(), 10);
			}
			return;
		}

		Arena arena = match.getArena();
		Cuboid bounds = arena.getBounds();

		// pretend the vertical bounds of the arena are 2 blocks lower than they
		// are to avoid issues with players hitting their heads on the glass (Jon said to do this)
		// looks kind of funny but in a high frequency event this is by far the fastest

		if (!bounds.contains(to) || !bounds.contains(to.getBlockX(), to.getBlockY() + 2, to.getBlockZ())) {
			// spectators get a nice message, players just get cancelled
			if (match.isSpectator(player.getUniqueId())) {
				player.teleport(arena.getSpectatorSpawn());
			} else if (to.getBlockY() >= bounds.getUpperY() || to.getBlockY() <= bounds.getLowerY()) { // if left vertically
				if (to.getBlockY() >= bounds.getUpperY() && match.getKitType().getId().equalsIgnoreCase("SKYWARS"))
					return;
				if ((match.getKitType().getId().equalsIgnoreCase("SKYWARS") || match.getKitType().getId().equalsIgnoreCase("SUMO") || match.getKitType().getId().equalsIgnoreCase("SPLEEF"))) {
					if (to.getBlockY() <= bounds.getLowerY() && bounds.getLowerY() - to.getBlockY() <= 20)
						return; // let the player fall 10 blocks
					match.markDead(player);
					match.addSpectator(player, null, true);
					player.teleport(arena.getSpectatorSpawn());
				}
			} else {
				if (match.getKitType().getId().equalsIgnoreCase("SKYWARS") || match.getKitType().getId().equalsIgnoreCase("SUMO") || match.getKitType().getId().equalsIgnoreCase("SPLEEF")) { // if they left horizontally
					match.markDead(player);
					match.addSpectator(player, null, true);
					player.teleport(arena.getSpectatorSpawn());
					event.setTo(event.getFrom());
				}
			}
		} else if (to.getBlockY() + 5 < arena.getSpectatorSpawn().getBlockY()) { // if the player is still in the arena bounds but fell down from the spawn point
			if (match.getKitType().getId().equalsIgnoreCase("SUMO")) {
				match.markDead(player);
				match.addSpectator(player, null, true);
				player.teleport(arena.getSpectatorSpawn());
			}
		}
	}

	@EventHandler
	public void onMove(PlayerMoveEvent event) {
		Location from = event.getFrom();
		Location to = event.getTo();

		if (from.getBlockX() == to.getBlockX() &&
				from.getBlockY() == to.getBlockY() &&
				from.getBlockZ() == to.getBlockZ()) {
			return;
		}

		if (event.getPlayer().hasMetadata("waiting")) {
			event.setTo(from);
		}
	}

	@EventHandler
	public void onLeave(PlayerQuitEvent event) {
		if (event.getPlayer().hasMetadata("waiting")) {
			event.getPlayer().removeMetadata("waiting", PotPvPSI.getInstance());
		}
		MatchHandler matchHandler = PotPvPSI.getInstance().getMatchHandler();
		Match match = matchHandler.getMatchPlayingOrSpectating(event.getPlayer());

		if (match == null) {
			return;
		}

		MatchTeam matchTeam = match.getTeam(event.getPlayer().getUniqueId());

		if (matchTeam == null)
			return;

		if (matchTeam.getAllMembers().size() > 1)
			return;

		match.terminateMatch(true, event.getPlayer());

	}

	/**
	 * Prevents (non-fall) damage between ANY two players not on opposing {@link MatchTeam}s.
	 * This includes cancelling damage from a player not in a match attacking a player in a match.
	 */
	@EventHandler
	public void onEntityDamageByEntity(EntityDamageByEntityEvent event) {
		if (event.getDamager() instanceof EnderPearl)
			return;
		if (event.getEntityType() != EntityType.PLAYER) {
			return;
		}

		// in the context of an EntityDamageByEntityEvent, DamageCause.FALL
		// is the 0 hearts of damage and knockback applied when hitting
		// another player with a thrown enderpearl. We allow this damage
		// in order to be consistent with HCTeams

		if (event.getCause() == EntityDamageEvent.DamageCause.FALL) {
			return;
		}

		MatchHandler matchHandler = PotPvPSI.getInstance().getMatchHandler();
		Player victim = (Player) event.getEntity();
		Player damager = PlayerUtils.getDamageSource(event.getDamager());

		if (damager == null) {
			return;
		}

		Match match = matchHandler.getMatchPlaying(damager);
		boolean isSpleef = match != null && match.getKitType().getId().equalsIgnoreCase("SPLEEF");
		boolean isSumo = match != null && match.getKitType().getId().equalsIgnoreCase("SUMO");
		boolean isPearlFight = match != null && match.getKitType().getId().equalsIgnoreCase("PEARLFIGHT");
		boolean isBoxing = match != null && match.getKitType().getId().equalsIgnoreCase("BOXING");

		// we only specifically allow damage where both players are in a match together
		// and not on the same team, everything else is cancelled.
		if (match != null) {
			MatchTeam victimTeam = match.getTeam(victim.getUniqueId());
			MatchTeam damagerTeam = match.getTeam(damager.getUniqueId());

			if (isSpleef && event.getDamager() instanceof Snowball) return;

			if (isSumo || isBoxing || isPearlFight && victimTeam != null && victimTeam != damagerTeam) {
				// Ugly hack because people actually lose health & hunger in sumo somehow
				event.setDamage(0);
				return;
			}

			if (victimTeam != null && victimTeam != damagerTeam && !isSpleef) {
				return;
			}
		}


		event.setCancelled(true);
	}

	@EventHandler
	public void onPlayerDropItem(PlayerDropItemEvent event) {
		MatchHandler matchHandler = PotPvPSI.getInstance().getMatchHandler();
		Player player = event.getPlayer();

		if (!matchHandler.isPlayingMatch(player)) {
			return;
		}

		ItemStack itemStack = event.getItemDrop().getItemStack();
		Material itemType = itemStack.getType();
		String itemTypeName = itemType.name().toLowerCase();
		int heldSlot = player.getInventory().getHeldItemSlot();

		// don't let players drop swords, axes, and bows in the first slot
		if (!PlayerUtils.hasOwnInventoryOpen(player) && heldSlot == 0 && (itemTypeName.contains("sword") || itemTypeName.contains("axe") || itemType == Material.BOW)) {
			player.sendMessage(ChatColor.RED + "You can't drop that while you're holding it in slot 1.");
			event.setCancelled(true);
		}

		// glass bottles and bowls are removed from inventories but
		// don't spawn items on the ground
		if (itemType == Material.GLASS_BOTTLE || itemType == Material.BOWL) {
			event.getItemDrop().remove();
		}
	}

	@EventHandler
	public void onPickup(PlayerPickupItemEvent event) {
		MatchHandler matchHandler = PotPvPSI.getInstance().getMatchHandler();
		Player player = event.getPlayer();

		if (!matchHandler.isPlayingMatch(player)) {
			return;
		}

		Match match = matchHandler.getMatchPlaying(event.getPlayer());
		if (match == null) return;

		if (match.getState() == MatchState.ENDING || match.getState() == MatchState.TERMINATED) {
			event.setCancelled(true);
		}
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onConsume(PlayerItemConsumeEvent event) {
		ItemStack stack = event.getItem();
		if (stack == null || stack.getType() != Material.POTION) return;

		Bukkit.getScheduler().runTaskLater(PotPvPSI.getInstance(), () -> {
			//event.getPlayer().setItemInHand(null);
		}, 1L);
	}

	private BlockFace getDirection(Player player) {
		float yaw = player.getLocation().getYaw();
		if (yaw < 0) {
			yaw += 360;
		}
		if (yaw >= 315 || yaw < 45) {
			return BlockFace.SOUTH;
		} else if (yaw < 135) {
			return BlockFace.WEST;
		} else if (yaw < 225) {
			return BlockFace.NORTH;
		} else if (yaw < 315) {
			return BlockFace.EAST;
		}
		return BlockFace.NORTH;
	}




}