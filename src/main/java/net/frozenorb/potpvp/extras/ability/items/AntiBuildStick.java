package net.frozenorb.potpvp.extras.ability.items;

import net.frozenorb.potpvp.PotPvPSI;
import net.frozenorb.potpvp.extras.ability.Ability;
import net.frozenorb.potpvp.extras.ability.profile.task.TaskType;
import org.apache.commons.lang.StringUtils;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import net.frozenorb.potpvp.util.CC;
import net.frozenorb.potpvp.util.cooldown.Cooldowns;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author LBuddyBoy (lbuddyboy.me)
 * 18/07/2021 / 1:46 AM
 * HCTeams / net.frozenorb.potpvp.ability.items
 */
public class AntiBuildStick extends Ability {

	public Cooldowns cd = new Cooldowns();
	public static Cooldowns buildTime = new Cooldowns();
	public ConcurrentHashMap<UUID, Integer> hits = new ConcurrentHashMap<>();

	public static List<String> blockedTypes = Arrays.asList(
			"DOOR",
			"PLATE",
			"CHEST",
			"GATE"
	);

	public static List<String> blockedTypesPretty = Arrays.asList(
			"Fence Gates",
			"Pressure Plates",
			"Trap Doors",
			"Chest",
			"Doors",
			"Buttons & Levers"
	);

	@Override
	public Cooldowns cooldown() {
		return cd;
	}

	@Override
	public String name() {
		return "antibuildstick";
	}

	@Override
	public String displayName() {
		return CC.chat("&b&lAntiBuildStick");
	}

	@Override
	public int data() {
		return 0;
	}

	@Override
	public Material mat() {
		return Material.STICK;
	}

	@Override
	public boolean glow() {
		return true;
	}

	@Override
	public List<String> lore() {
		return CC.translate(
				Arrays.asList(
						" ",
						"&7Hit a player 3 times to commence a sequence where they",
						"&7cannot build/interact with a select few materials within",
						"&7the game.",
						" ",
						"&c&lNOTE&7: They will not be able to use/interact with any of the following:",
						"&7" + StringUtils.join(blockedTypesPretty, ", "),
						" "
				)
		);
	}

	@Override
	public List<String> foundInfo() {
		return CC.translate(Arrays.asList(
				"Ability Packages",
				"Partner Crates"
		));
	}

	@EventHandler
	public void onHit(EntityDamageByEntityEvent event) {
		if (!checkInstancePlayer(event.getEntity()))
			return;
		if (!checkInstancePlayer(event.getDamager()))
			return;
		Player damager = (Player) event.getDamager();

		if (!isSimilar(damager.getItemInHand()))
			return;

		Player damaged = (Player) event.getEntity();

		if (!canUse(damager)) {
			return;
		}
		if (!canAttack(damager, damaged))
			return;

		if (!hits.isEmpty() && hits.get(damager.getUniqueId()) != null && hits.get(damager.getUniqueId()) >= 3) {

			List<String> beenHitMsg = Arrays.asList(
					"",
					"&b&lYOU HAVE BEEN HIT!",
					" ",
					"&b" + damager.getName() + " &fhas just hit you with",
					"&fan &bAntiBuildStick&f.",
					" ",
					"&7┃ &fYou cannot use/interact with the following",
					"&7┃ &ffor &b15 seconds&f.",
					"&7┃ &f" + StringUtils.join(blockedTypesPretty, ", "),
					"");

			List<String> hitMsg = Arrays.asList(
					"",
					"&b&lYOU HAVE HIT SOMEONE!",
					" ",
					"&bYou" + " &fhave just hit &5" + damaged.getName(),
					"&fwith an &bAntiBuildStick&f.",
					" ",
					"&7┃ &fThey cannot use/interact with the following",
					"&7┃ &ffor &b15 seconds&f.",
					"&7┃ &f" + StringUtils.join(blockedTypesPretty, ", "),
					"");

			hitMsg.forEach(s -> damager.sendMessage(CC.chat(s)));

			beenHitMsg.forEach(s -> damaged.sendMessage(CC.chat(s)));

			PotPvPSI.getInstance().getAbilityHandler().getAbilityEffect().applyCooldown(damaged, 15);
			buildTime.applyCooldown(damaged, 15);
			addCooldown(damager, 90);
			takeItem(damager);

			CompletableFuture.runAsync(() -> hits.remove(damager.getUniqueId()));
			return;
		}

		activateRunnable(TaskType.ANTIBUILDHIT, damager, hits);
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void onBlockPlace(BlockPlaceEvent event) {
		Ability dome = PotPvPSI.getInstance().getAbilityHandler().byName("dome");
		if (dome.isSimilar(event.getItemInHand()))
			return;
		if (buildTime.onCooldown(event.getPlayer())) {
			event.setCancelled(true);
			event.getPlayer().sendMessage(CC.chat("&cYou cannot do this for &l" + buildTime.getRemaining(event.getPlayer())));
		}
	}

	@EventHandler
	public void onBlockBreak(BlockBreakEvent event) {
		if (buildTime.onCooldown(event.getPlayer())) {
			event.setCancelled(true);
			event.getPlayer().sendMessage(CC.chat("&cYou cannot do this for &l" + buildTime.getRemaining(event.getPlayer())));
		}
	}

	@EventHandler
	public void onInteract(PlayerInteractEvent event) {
		if (buildTime.onCooldown(event.getPlayer())) {
			if (event.getClickedBlock() == null)
				return;
			if (event.getAction() == Action.PHYSICAL) {
				event.setUseInteractedBlock(Event.Result.DENY);
				event.getPlayer().sendMessage(CC.chat("&cYou cannot do this for &l" + buildTime.getRemaining(event.getPlayer())));
				return;
			}
			for (String blockedType : blockedTypes) {
				if (event.getClickedBlock().getType().name().contains(blockedType)) {
					event.setUseInteractedBlock(Event.Result.DENY);
					event.getPlayer().sendMessage(CC.chat("&cYou cannot do this for &l" + buildTime.getRemaining(event.getPlayer())));
				}
			}
		}
	}

}
