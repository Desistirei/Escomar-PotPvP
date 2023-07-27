package net.frozenorb.potpvp.match.kbprofile;


import net.angel.spigot.knockback.KnockbackProfile;

public class ComboProfile implements KnockbackProfile {
	@Override
	public String getName() {
		return "RealCombo";
	}

	@Override
	public double getFriction() {
		return 2.5;
	}

	@Override
	public void setFriction(double v) {

	}

	@Override
	public double getHorizontal() {
		return 0.05;
	}

	@Override
	public void setHorizontal(double v) {

	}

	@Override
	public double getVertical() {
		return 0.15;
	}

	@Override
	public void setVertical(double v) {

	}

	@Override
	public double getVerticalLimit() {
		return 0.35;
	}

	@Override
	public void setVerticalLimit(double v) {

	}

	@Override
	public double getExtraHorizontal() {
		return 0.425;
	}

	@Override
	public void setExtraHorizontal(double v) {

	}

	@Override
	public double getExtraVertical() {
		return 0.05;
	}

	@Override
	public void setExtraVertical(double v) {

	}
}
