package net.frozenorb.potpvp.killeffects.menu;

import net.frozenorb.potpvp.PotPvPSI;
import net.frozenorb.potpvp.killeffects.effects.KillEffect;
import net.frozenorb.potpvp.profile.Profile;
import net.frozenorb.potpvp.util.CC;
import net.frozenorb.qlib.menu.Button;
import net.frozenorb.qlib.menu.Menu;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;

import java.util.*;

/**
 * @author LBuddyBoy (lbuddyboy.me)
 * 25/06/2021 / 9:32 AM
 * potpvp-si / net.frozenorb.potpvp.killeffects.menu
 */
public class KillEffectMenu extends Menu {

	@Override
	public String getTitle(Player player) {
		return "Kill Effects";
	}

	@Override
	public Map<Integer, Button> getButtons(Player player) {
		Map<Integer, Button> buttons = new HashMap<>();

		int i = 0;
		Profile p = PotPvPSI.getInstance().getProfileManager().getProfile(player);
		for (KillEffect killEffect : PotPvPSI.getInstance().getKillEffectHandler().getEffects()) {
			if (i <= 7) {
				buttons.put(getSlot(1 + i, 1), new Button() {
					@Override
					public String getName(Player player) {
						return CC.chat(killEffect.getDisplayName());
					}

					@Override
					public List<String> getDescription(Player player) {
						KillEffect effect = p.getKillEffect();
						if (!player.hasPermission("potpvp.killeffect." + killEffect.getName())) {
							return CC.list(Arrays.asList("&cYou lack permission to use this kill effect.", "&b" + CC.ARROW_RIGHT + " &fYou can purchase access to this on &bstore.orbit.rip&f."));
						}
						if (effect == null) {
							return CC.list(Arrays.asList("&fClick to &aactivate&7 the " + killEffect.getDisplayName() + " &fKill Effect"));
						}

						return CC.list(Arrays.asList("&7Click to " + (effect == killEffect ? "&cdeactivate" : "&aactivated") + " &fthe " + killEffect.getDisplayName() + " &fKill Effect."));
					}

					@Override
					public Material getMaterial(Player player) {
						if (!player.hasPermission("potpvp.killeffect." + killEffect.getName())) {
							return Material.REDSTONE_BLOCK;
						}
						return killEffect.getIcon();
					}

					@Override
					public void clicked(Player player, int slot, ClickType clickType) {
						if (!player.hasPermission("potpvp.killeffect." + killEffect.getName())) {
							player.sendMessage(CC.chat("&cYou lack permission for this kill effect."));
							player.sendMessage(CC.chat("&b" + CC.ARROW_RIGHT + " &fYou can purchase access to this on &bstore.orbit.rip&f."));
							return;
						}
						if (p.getKillEffect() == killEffect) {
							player.sendMessage(CC.chat("&fYou have just &cde-activated &fthe " + killEffect.getDisplayName() + "&f."));
							p.setKillEffect(KillEffect.none);
							p.save();
							return;
						}
						player.sendMessage(CC.chat("&fYou have just &aactivated &fthe " + killEffect.getDisplayName() + "&f."));
						p.setKillEffect(killEffect);
						p.save();
					}
				});
			} else if (i <= 14) {
				buttons.put(getSlot(1 + i - 7, 2), new Button() {
					@Override
					public String getName(Player player) {
						return CC.chat(killEffect.getDisplayName());
					}

					@Override
					public List<String> getDescription(Player player) {
						KillEffect effect = p.getKillEffect();
						if (!player.hasPermission("potpvp.killeffect." + killEffect.getName())) {
							return CC.list(Arrays.asList("&cYou lack permission to use this kill effect.", "&b" + CC.ARROW_RIGHT + " &fYou can purchase access to this on &bstore.orbit.rip&f."));
						}
						if (effect == null) {
							return CC.list(Arrays.asList("&fClick to &aactivate&7 the " + killEffect.getDisplayName() + " &fKill Effect"));
						}

						return CC.list(Arrays.asList("&7Click to " + (effect == killEffect ? "&cdeactivate" : "&aactivated") + " &fthe " + killEffect.getDisplayName() + " &fKill Effect."));
					}

					@Override
					public Material getMaterial(Player player) {
						if (!player.hasPermission("potpvp.killeffect." + killEffect.getName())) {
							return Material.REDSTONE_BLOCK;
						}
						return killEffect.getIcon();
					}

					@Override
					public void clicked(Player player, int slot, ClickType clickType) {
						if (!player.hasPermission("potpvp.killeffect." + killEffect.getName())) {
							player.sendMessage(CC.chat("&cYou lack permission for this kill effect."));
							player.sendMessage(CC.chat("&b" + CC.ARROW_RIGHT + " &fYou can purchase access to this on &bstore.orbit.rip&f."));
							return;
						}
						if (p.getKillEffect() == killEffect) {
							p.setKillEffect(null);
							p.save();
							return;
						}
						player.sendMessage(CC.chat("&fYou have just &aactivated &fthe " + killEffect.getDisplayName() + "&f."));
						p.setKillEffect(killEffect);
						p.save();
					}
				});
			}
			++i;
		}
		return buttons;
	}

	@Override
	public int size(Map<Integer, Button> buttons) {
		return actualSize(buttons) + 9;
	}

	public int actualSize(Map<Integer, Button> buttons) {
		int highest = 0;
		Iterator var3 = buttons.keySet().iterator();

		while(var3.hasNext()) {
			int buttonValue = (Integer)var3.next();
			if (buttonValue > highest) {
				highest = buttonValue;
			}
		}

		return (int)(Math.ceil((double)(highest + 1) / 9.0D) * 9.0D);
	}
}
