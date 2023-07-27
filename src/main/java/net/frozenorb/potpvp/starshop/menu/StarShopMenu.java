package net.frozenorb.potpvp.starshop.menu;

import lombok.AllArgsConstructor;
import net.frozenorb.potpvp.PotPvPSI;
import net.frozenorb.potpvp.profile.Profile;
import net.frozenorb.potpvp.starshop.menu.type.StarShopItem;
import net.frozenorb.potpvp.util.CC;
import net.frozenorb.potpvp.util.ItemBuilder;
import net.frozenorb.qlib.menu.Button;
import net.frozenorb.qlib.menu.Menu;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;

import java.util.*;

/**
 * @author LBuddyBoy (lbuddyboy.me)
 * 14/08/2021 / 3:17 PM
 * HCTeams / rip.orbit.hcteams.stars.menu
 */
public class StarShopMenu extends Menu {

	@Override
	public String getTitle(Player player) {
		return CC.translate("&bStar Shop");
	}

	@Override
	public Map<Integer, Button> getButtons(Player player) {
		Map<Integer, Button> buttons = new HashMap<>();

		buttons.put(13, new StarsButton());

		int i = 27;
		for (StarShopItem value : StarShopItem.values()) {
			buttons.put(i, new ItemButton(value));
			++i;
		}

		return buttons;
	}

	@Override
	public boolean isPlaceholder() {
		return true;
	}

	public static class StarsButton extends Button {

		@Override
		public String getName(Player player) {
			return CC.translate("&fYour Stars&7: &b" + PotPvPSI.getInstance().getProfileManager().getProfile(player.getUniqueId()).getStars() + "✧");
		}

		@Override
		public List<String> getDescription(Player player) {
			return Collections.emptyList();
		}

		@Override
		public Material getMaterial(Player player) {
			return Material.NETHER_STAR;
		}
	}

	@AllArgsConstructor
	public static class ItemButton extends Button {

		private final StarShopItem item;

		@Override
		public ItemStack getButtonItem(Player player) {
			return new ItemBuilder(item.getMaterial())
					.amount(item.getAmount())
					.name(CC.translate(item.getDisplayName()))
					.data(item.getData())
					.lore(CC.translate(Arrays.asList(
							"&7┃ &fPrice&7: &b" + item.getPrice() + "✧",
							"&7┃ &fClick to purchase this item"
					)))
					.build();
		}

		@Override
		public String getName(Player player) {
			return null;
		}

		@Override
		public List<String> getDescription(Player player) {
			return null;
		}

		@Override
		public Material getMaterial(Player player) {
			return null;
		}

		@Override
		public void clicked(Player player, int slot, ClickType clickType) {
			int stars = PotPvPSI.getInstance().getProfileManager().getProfile(player.getUniqueId()).getStars();
			if (stars < item.getPrice()) {
				player.sendMessage(CC.translate("&cInsufficient Funds."));
				return;
			}
			Bukkit.dispatchCommand(Bukkit.getConsoleSender(), item.getCommand().replaceAll("%player%", player.getName()));

			player.sendMessage(CC.translate("&fYou have just purchased " + item.getDisplayName() + " &ffor &b" + item.getPrice() + "✧"));

			PotPvPSI.getInstance().getProfileManager().getProfile(player.getUniqueId()).setStars(stars - item.getPrice());
			PotPvPSI.getInstance().getProfileManager().getProfile(player.getUniqueId()).save();
		}
	}
}
