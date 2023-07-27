package net.frozenorb.potpvp.killeffects;

import lombok.Getter;
import net.frozenorb.potpvp.PotPvPSI;
import net.frozenorb.potpvp.killeffects.effects.KillEffect;
import net.frozenorb.potpvp.profile.Profile;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * @author LBuddyBoy (lbuddyboy.me)
 * 25/06/2021 / 9:11 AM
 * potpvp-si / net.frozenorb.potpvp.killeffects
 */
public class KillEffectHandler {

	@Getter
	private List<KillEffect> effects;

	public KillEffectHandler() {
		effects = new ArrayList<>();


		getEffects().add(KillEffect.firework);
		getEffects().add(KillEffect.flame);
		getEffects().add(KillEffect.splash);
		getEffects().add(KillEffect.explosion);
		getEffects().add(KillEffect.ender);
		getEffects().add(KillEffect.blood);
	}

	public void playEffect(Player killer, Player dead) {
		Profile p = PotPvPSI.getInstance().getProfileManager().getProfile(killer);

		for (int i = 0; i < 5; i++) {
			if (p.getKillEffect().isUseMaterial()) {
				dead.getLocation().getWorld().playEffect(dead.getLocation().add(0, i, 0), p.getKillEffect().getEffect(), p.getKillEffect().getMaterial());
			} else {
				dead.getLocation().getWorld().playEffect(dead.getLocation().add(0, i, 0), p.getKillEffect().getEffect(), p.getKillEffect().getEffectData());
			}
		}
	}
}
