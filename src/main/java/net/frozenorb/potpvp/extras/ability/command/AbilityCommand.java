package net.frozenorb.potpvp.extras.ability.command;

import net.frozenorb.potpvp.PotPvPSI;
import net.frozenorb.potpvp.extras.ability.Ability;
import net.frozenorb.qlib.command.Command;
import net.frozenorb.qlib.command.Param;
import net.frozenorb.qlib.menu.Button;
import net.frozenorb.qlib.menu.Menu;
import org.apache.commons.lang.StringUtils;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import net.frozenorb.potpvp.util.CC;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author LBuddyBoy (lbuddyboy.me)
 * 01/07/2021 / 11:35 AM
 * HCTeams / net.frozenorb.potpvp.ability.command
 */
public class AbilityCommand {

	@Command(names = "ability give", permission = "foxtrot.ability")
	public static void give(CommandSender sender, @Param(name = "player") Player target, @Param(name = "ability") Ability ability, @Param(name = "amount") int amount) {
		ItemStack item = ability.getStack();
		item.setAmount(amount);

		target.getInventory().addItem(item);
	}

	@Command(names = "ability list", permission = "foxtrot.ability")
	public static void list(Player sender) {
		List<String> names = new ArrayList<>();
		PotPvPSI.getInstance().getAbilityHandler().getAbilities().forEach(ability -> names.add(ability.name()));
		sender.sendMessage(CC.chat("&b&lAbility List&f: " + StringUtils.join(names, ", ")));
	}

	@Command(names = {"ability preview"}, permission = "")
	public static void showcase(Player sender) {

		Menu menu = new Menu() {

			@Override
			public boolean isPlaceholder() {
				return true;
			}

			@Override
			public String getTitle(Player player) {
				return "Ability Items";
			}

			@Override
			public Map<Integer, Button> getButtons(Player player) {
				Map<Integer, Button> buttons = new HashMap<>();
				int i = 0;
				for (Ability currentAbility : PotPvPSI.getInstance().getAbilityHandler().getAbilities()) {
					buttons.put(i, new Button() {
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
						public ItemStack getButtonItem(Player player) {
							ItemStack stack = currentAbility.getStack().clone();
							ItemMeta meta = stack.getItemMeta();

							List<String> toLore = meta.getLore();
//							toLore.add(CC.chat("&7&lFound In:"));
//							for (String s : currentAbility.foundInfo()) {
//								toLore.add(CC.chat("&7┃ &7" + s));
//							}
//							meta.setLore(toLore);
							stack.setItemMeta(meta);
							stack.setAmount(1);

							return stack;
						}
					});
					++i;
				}

				return buttons;
			}
		};
		menu.openMenu(sender);

	}

}
