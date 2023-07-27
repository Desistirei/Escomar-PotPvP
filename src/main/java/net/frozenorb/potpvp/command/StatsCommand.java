package net.frozenorb.potpvp.command;

import net.frozenorb.potpvp.lobby.menu.StatisticsMenu;
import net.frozenorb.qlib.command.Command;
import org.bukkit.entity.Player;

/**
 * @author LBuddyBoy (lbuddyboy.me)
 * 24/06/2021 / 10:06 PM
 * potpvp-si / net.frozenorb.potpvp.command
 */
public class StatsCommand {

	@Command(names = "stats", permission = "")
	public static void stats(Player sender) {
		new StatisticsMenu().openMenu(sender);
	}

}
