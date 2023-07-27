package net.frozenorb.potpvp.command;

import net.frozenorb.potpvp.PotPvPSI;
import net.frozenorb.potpvp.match.Match;
import net.frozenorb.potpvp.match.command.LeaveCommand;
import net.frozenorb.qlib.command.Command;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * @author LBuddyBoy (lbuddyboy.me)
 * 05/09/2021 / 8:00 PM
 * potpvp-si / net.frozenorb.potpvp.command
 */
public class ForceEndAllMatches {

	@Command(names = "forceendmatches", permission = "op")
	public static void forceendallmatches(CommandSender sender) {
		for (Match match : PotPvPSI.getInstance().getMatchHandler().getHostedMatches()) {
			match.terminateMatch();
			match.getAllPlayers().forEach(uuid -> {
				Player player = Bukkit.getPlayer(uuid);
				if (player != null) {
					LeaveCommand.leave(player);
				}
			});
		}
	}

}
