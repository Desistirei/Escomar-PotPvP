package net.frozenorb.potpvp.extras.ability;

import lombok.Getter;
import net.frozenorb.potpvp.PotPvPSI;
import net.frozenorb.potpvp.extras.ability.items.*;
import net.frozenorb.potpvp.extras.ability.items.pocketbards.Regeneration;
import net.frozenorb.potpvp.extras.ability.items.pocketbards.Resistance;
import net.frozenorb.potpvp.extras.ability.items.pocketbards.Speed;
import net.frozenorb.potpvp.extras.ability.items.pocketbards.Strength;
import net.frozenorb.potpvp.extras.ability.param.AbilityParameterType;
import net.frozenorb.potpvp.extras.ability.profile.ProfileListener;
import net.frozenorb.qlib.command.FrozenCommandHandler;
import net.frozenorb.potpvp.util.cooldown.Cooldowns;
import org.bukkit.Bukkit;

import java.util.ArrayList;
import java.util.List;

/**
 * @author LBuddyBoy (lbuddyboy.me)
 * 01/07/2021 / 12:53 AM
 * HCTeams / net.frozenorb.potpvp.ability
 */
public class AbilityHandler {

	@Getter private final List<Ability> abilities;
	@Getter private final List<Ability> pocketbards;
	@Getter private final Cooldowns abilityCD;
	@Getter private final Cooldowns abilityEffect;

	public AbilityHandler() {
		abilities = new ArrayList<>();
		pocketbards = new ArrayList<>();
		abilityEffect = new Cooldowns();
		abilityCD = new Cooldowns();

		FrozenCommandHandler.registerParameterType(Ability.class, new AbilityParameterType());

		abilities.add(new Switcher());
//		abilities.add(new Turret());
		abilities.add(new Recon());
		abilities.add(new AntiBuildStick());
		abilities.add(new AbilityInspector());
		abilities.add(new Curse());
		abilities.add(new Warrior());
		abilities.add(new TimeWarp());
		abilities.add(new Thorns());
		abilities.add(new GuardianAngel());
		abilities.add(new GhostMode());
		abilities.add(new PocketBard());
		abilities.add(new NinjaStar());
//		abilities.add(new Voider());
		abilities.add(new Dome());

		pocketbards.add(new Strength());
		pocketbards.add(new Resistance());
		pocketbards.add(new Regeneration());
		pocketbards.add(new Speed());

		Bukkit.getPluginManager().registerEvents(new ProfileListener(), PotPvPSI.getInstance());

	}

	public Ability byName(String name) {
		for (Ability ability : abilities) {
			if (ability.name().equals(name)) {
				return ability;
			}
		}
		return null;
	}

}
