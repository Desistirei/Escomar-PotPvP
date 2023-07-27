package net.frozenorb.potpvp.extras.ability.tasks;

import net.frozenorb.potpvp.extras.ability.items.TimeWarp;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

public class TimewarpTask extends BukkitRunnable {

	private Player player;

	public TimewarpTask(Player player) {
		this.player = player;
	}

	@Override
	public void run() {
		if (TimeWarp.justClicked.contains(player.getUniqueId()))
			return;
		TimeWarp.timewarp.remove(player);
		TimeWarp.bukkitStore.remove(player.getUniqueId());
//		player.sendMessage(CC.chat("&cYour previous location that you used has just perished. You can no longer teleport over there."));
	}
}
