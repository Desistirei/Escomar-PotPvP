package net.frozenorb.potpvp.starshop.command;

import net.frozenorb.potpvp.PotPvPSI;
import net.frozenorb.potpvp.starshop.menu.StarShopMenu;
import net.frozenorb.potpvp.util.CC;
import net.frozenorb.qlib.command.Command;
import net.frozenorb.qlib.command.Param;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;

/**
 * @author LBuddyBoy (lbuddyboy.me)
 * 14/08/2021 / 6:04 AM
 * HCTeams / rip.orbit.hcteams.stars.command
 */
public class StarsCommand {

	@Command(names = "starshop", permission = "")
	public static void starshop(Player sender) {
		new StarShopMenu().openMenu(sender);
	}

	@Command(names = {"stars set"}, permission = "op")
	public static void setStars(CommandSender sender, @Param(name = "target") UUID target, @Param(name = "amount") int amount) {
		PotPvPSI.getInstance().getProfileManager().getProfile(target).setStars(amount);
		PotPvPSI.getInstance().getProfileManager().getProfile(target).save();
		sender.sendMessage(CC.translate("&aSuccess, the players stars is now " + amount));
	}

	@Command(names = {"stars add"}, permission = "op")
	public static void addStars(CommandSender sender, @Param(name = "target") UUID target, @Param(name = "amount") int amount) {
		int toAdd = PotPvPSI.getInstance().getProfileManager().getProfile(target).getStars() + amount;
		PotPvPSI.getInstance().getProfileManager().getProfile(target).setStars(toAdd);
		PotPvPSI.getInstance().getProfileManager().getProfile(target).save();
		sender.sendMessage(CC.translate("&aSuccess, the players stars is now " + toAdd));
	}

}
