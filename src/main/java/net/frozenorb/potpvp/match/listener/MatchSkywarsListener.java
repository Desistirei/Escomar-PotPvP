package net.frozenorb.potpvp.match.listener;

import net.frozenorb.potpvp.PotPvPSI;
import net.frozenorb.potpvp.arena.Arena;
import net.frozenorb.potpvp.match.Match;
import net.frozenorb.potpvp.match.MatchHandler;
import net.frozenorb.potpvp.match.chest.ChestTier;
import net.frozenorb.potpvp.match.event.MatchStartEvent;
import net.frozenorb.qlib.util.Callback;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Chest;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

/**
 * @author LBuddyBoy (lbuddyboy.me)
 * 24/06/2021 / 4:51 PM
 * potpvp-si / net.frozenorb.potpvp.match.listener
 */
public class MatchSkywarsListener implements Listener {

	@EventHandler
	public void onStart(MatchStartEvent event) {
		if (event.getMatch().getKitType().getId().equalsIgnoreCase("skywars")) {

			List<ChestTier> tiers = Arrays.asList(
					ChestTier.ONE,
					ChestTier.THREE,
					ChestTier.FOUR);

			int e = 0;

			List<Chest> chests = new ArrayList<>();
			forEachBlock(event.getMatch(), block -> {
				if (block.getType() == Material.CHEST) {
					chests.add((Chest)block.getState());
				}
			});

			for (Chest chest : chests) {
				if (e == 3) {
					e = 0;
				}
				ChestTier randomTier = tiers.get(e);
				randomTier.getItems().forEach(stack -> {
					chest.getBlockInventory().setItem(new Random().nextInt(27), stack);
				});
				++e;
			}
		}
	}

	@EventHandler
	public void onMove(PlayerMoveEvent event) {

		Player player = event.getPlayer();
		Location from = event.getFrom();
		Location to = event.getTo();

		if (from.getBlockX() == to.getBlockX() &&
						from.getBlockY() == to.getBlockY() &&
						from.getBlockZ() == to.getBlockZ()) {
			return;
		}

		MatchHandler matchHandler = PotPvPSI.getInstance().getMatchHandler();
		Match match = matchHandler.getMatchPlayingOrSpectating(player);

		if (match == null) {
			return;
		}
		if (match.getKitType().getId().equalsIgnoreCase("skywars")) {
			if (event.getTo().getY() < 0) {
				match.markDead(event.getPlayer());
				match.addSpectator(event.getPlayer(), null, true);
				event.getPlayer().teleport(match.getArena().getSpectatorSpawn());
			}
		}
	}

	private void forEachBlock(Match match, Callback<Block> callback) {
		Arena arena = match.getArena();
		Location start = arena.getBounds().getLowerNE();
		Location end = arena.getBounds().getUpperSW();
		World world = arena.getBounds().getWorld();

		for (int x = start.getBlockX(); x < end.getBlockX(); x++) {
			for (int y = start.getBlockY(); y < end.getBlockY(); y++) {
				for (int z = start.getBlockZ(); z < end.getBlockZ(); z++) {
					callback.callback(world.getBlockAt(x, y, z));
				}
			}
		}
	}

}
