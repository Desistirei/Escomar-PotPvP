package net.frozenorb.potpvp.match.chest;

import lombok.Getter;
import net.frozenorb.potpvp.util.ItemBuilder;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.Arrays;
import java.util.List;

/**
 * @author LBuddyBoy (lbuddyboy.me)
 * 30/07/2021 / 2:49 PM
 * potpvp-si / net.frozenorb.potpvp.match.chest
 */

@Getter
public enum ChestTier {

	ONE(1, Arrays.asList(
			new ItemBuilder(Material.ARROW).amount(16).build(),
			new ItemBuilder(Material.COBBLESTONE).amount(16).build(),
			new ItemBuilder(Material.STONE).amount(24).build(),
			new ItemBuilder(Material.GOLDEN_APPLE).amount(6).build()
	)),
	THREE(2, Arrays.asList(
			new ItemBuilder(Material.DIAMOND_HELMET).build(),
			new ItemBuilder(Material.DIAMOND_CHESTPLATE).build(),
			new ItemBuilder(Material.DIAMOND_LEGGINGS).build(),
			new ItemBuilder(Material.DIAMOND_BOOTS).build(),
			new ItemBuilder(Material.DIAMOND_SWORD).build()
	)),
	FOUR(3, Arrays.asList(
			new ItemBuilder(Material.WOOD).amount(16).build(),
			new ItemBuilder(Material.LOG).amount(24).build(),
			new ItemBuilder(Material.COOKED_BEEF).amount(16).build(),
			new ItemBuilder(Material.BOW).build()
	));

	private final int tier;
	private final List<ItemStack> items;

	ChestTier(int tier, List<ItemStack> items) {
		this.tier = tier;
		this.items = items;
	}

}
