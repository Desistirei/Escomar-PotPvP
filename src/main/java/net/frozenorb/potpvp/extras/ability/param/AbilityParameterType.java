package net.frozenorb.potpvp.extras.ability.param;

import net.frozenorb.potpvp.PotPvPSI;
import net.frozenorb.potpvp.extras.ability.Ability;
import net.frozenorb.qlib.command.ParameterType;
import org.apache.commons.lang.StringUtils;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * @author LBuddyBoy (lbuddyboy.me)
 * 01/07/2021 / 11:36 AM
 * HCTeams / net.frozenorb.potpvp.ability.param
 */
public class AbilityParameterType implements ParameterType<Ability> {

	@Override
	public Ability transform(CommandSender sender, String source) {
		for (Ability abi : PotPvPSI.getInstance().getAbilityHandler().getAbilities()) {
			if (source.equalsIgnoreCase(abi.name())) {
				return abi;
			}
		}

		sender.sendMessage(ChatColor.RED + "No ability with the name " + source + " found.");
		return (null);
	}

	@Override
	public List<String> tabComplete(Player sender, Set<String> flags, String source) {
		List<String> completions = new ArrayList<>();

		for (Ability ability : PotPvPSI.getInstance().getAbilityHandler().getAbilities()) {
			if (StringUtils.startsWith(ability.name(), source)) {
				completions.add(ability.name());
			}
		}

		return (completions);
	}

}