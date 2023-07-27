package net.frozenorb.potpvp.command;

import net.frozenorb.potpvp.PotPvPSI;
import net.frozenorb.potpvp.command.task.StuckTask;
import net.frozenorb.potpvp.match.Match;
import net.frozenorb.potpvp.util.CC;
import net.frozenorb.potpvp.util.cooldown.Cooldowns;
import net.frozenorb.qlib.command.Command;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

/**
 * @author LBuddyBoy (lbuddyboy.me)
 * 26/06/2021 / 9:27 AM
 * potpvp-si / net.frozenorb.potpvp.command
 */
public class StuckCommand {

	public static Cooldowns stuckTime = new Cooldowns();

	@Command(names = "stuck", permission = "")
	public static void stuck(Player sender) {
		Match match = PotPvPSI.getInstance().getMatchHandler().getMatchPlaying(sender);
		if (match == null) {
			sender.sendMessage(CC.chat("&cYou cannot do this whilst you're not in a match."));
			return;
		}
		if (!match.getKitType().getId().equalsIgnoreCase("baseraiding")) {
			sender.sendMessage(CC.chat("&cYou can only do this if you're playing the Base Raiding simulation."));
			return;
		}
		sender.sendMessage(CC.chat("&cYou will be teleported to your spawn point in 15 seconds, don't get hit or move a block!"));
		stuckTime.applyCooldown(sender, 15);
		BukkitTask task = new BukkitRunnable() {
			@Override
			public void run() {
				sender.teleport(match.getArena().getSpectatorSpawn().add(0, 2, 0));
			}
		}.runTaskLater(PotPvPSI.getInstance(), 20 * 15);
		BukkitRunnable runnable = new StuckTask(sender, sender.getLocation(), task, sender.getHealth());
		runnable.runTaskTimer(PotPvPSI.getInstance(), 2, 2);


	}

}
