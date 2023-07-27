package net.frozenorb.potpvp.command;

import net.frozenorb.potpvp.PotPvPSI;
import net.frozenorb.potpvp.kit.menu.kits.KitsMenu;
import net.frozenorb.potpvp.kittype.menu.select.SelectKitTypeMenu;
import net.frozenorb.potpvp.match.Match;
import net.frozenorb.potpvp.match.MatchHandler;
import net.frozenorb.potpvp.util.CC;
import net.frozenorb.qlib.command.Command;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

/**
 * @author LBuddyBoy (lbuddyboy.me)
 * 24/06/2021 / 10:26 PM
 * potpvp-si / net.frozenorb.potpvp.command
 */
public class EditKitsCommand {

	@Command(names = "editkits", permission = "")
	public static void edit(Player player) {
		MatchHandler matchHandler = PotPvPSI.getInstance().getMatchHandler();
		Match match = matchHandler.getMatchPlayingOrSpectating(player);

		if (match != null) {
			player.sendMessage(CC.translate("&cYou may only use this whilst in the lobby."));
			return;
		}
		new SelectKitTypeMenu(kitType -> {
			new KitsMenu(kitType).openMenu(player);
		}, "Select a kit to edit...").openMenu(player);
	}

}
