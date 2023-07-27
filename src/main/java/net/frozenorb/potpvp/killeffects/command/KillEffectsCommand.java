package net.frozenorb.potpvp.killeffects.command;

import net.frozenorb.potpvp.killeffects.menu.KillEffectMenu;
import net.frozenorb.qlib.command.Command;
import org.bukkit.entity.Player;

/**
 * @author LBuddyBoy (lbuddyboy.me)
 * 25/06/2021 / 10:15 AM
 * potpvp-si / net.frozenorb.potpvp.killeffects.command
 */
public class KillEffectsCommand {

	@Command(names = "killeffects", permission = "")
	public static void killeffects(Player sender) {
		new KillEffectMenu().openMenu(sender);
	}

}
