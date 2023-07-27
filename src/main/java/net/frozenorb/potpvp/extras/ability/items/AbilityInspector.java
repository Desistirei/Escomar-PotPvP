package net.frozenorb.potpvp.extras.ability.items;

import net.frozenorb.potpvp.PotPvPSI;
import net.frozenorb.potpvp.extras.ability.Ability;
import net.frozenorb.potpvp.extras.ability.profile.task.TaskType;
import org.apache.commons.lang.StringUtils;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;
import net.frozenorb.potpvp.profile.Profile;
import net.frozenorb.potpvp.util.CC;
import net.frozenorb.potpvp.util.cooldown.Cooldowns;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author LBuddyBoy (lbuddyboy.me)
 * 19/07/2021 / 3:25 PM
 * HCTeams / net.frozenorb.potpvp.ability.items
 */
public class AbilityInspector extends Ability {

	public Cooldowns cd = new Cooldowns();
	public ConcurrentHashMap<UUID, Integer> hits = new ConcurrentHashMap<>();

	@Override
	public Cooldowns cooldown() {
		return cd;
	}

	@Override
	public String name() {
		return "abilityinspector";
	}

	@Override
	public String displayName() {
		return CC.chat("&5&lAbility Inspector");
	}

	@Override
	public int data() {
		return 0;
	}

	@Override
	public Material mat() {
		return Material.BOOK;
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
						"&7Hit a player 3 times to display all ability items",
						"&7within the player that was hit.",
						" ",
						"&c&lNOTE&7: You will get a list of their ability items",
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
			List<String> abilityItems = new ArrayList<>();
			for (ItemStack stack : damaged.getInventory().getContents()) {
				if (stack != null && !stack.getType().equals(Material.AIR)) {
					for (Ability ability : PotPvPSI.getInstance().getAbilityHandler().getAbilities()) {
						if (ability.getStack().isSimilar(stack)) {
							abilityItems.add(ability.name());
						}
					}
				}
			}
			List<String> hitMsg = Arrays.asList(
					"",
					"&5&lYOU HAVE HIT SOMEONE!",
					" ",
					"&5You" + " &fhave just hit &5" + damaged.getName(),
					"&fwith an &5Ability Inspector&f.",
					" ",
					"&7┃ &fTheir inventory contains the following ability items:",
					"&7┃ &5" + (abilityItems.isEmpty() ? "None" : StringUtils.join(abilityItems, ", ")) + "&f.",
					"");

			hitMsg.forEach(s -> damager.sendMessage(CC.chat(s)));

			addCooldown(damager, 90);
			takeItem(damager);

			CompletableFuture.runAsync(() -> hits.remove(damager.getUniqueId()));
			return;
		}

		activateRunnable(TaskType.INSPECTORHIT, damager, hits);
	}
}
