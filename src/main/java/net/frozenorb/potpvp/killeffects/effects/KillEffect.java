package net.frozenorb.potpvp.killeffects.effects;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.bukkit.Effect;
import org.bukkit.Material;

@AllArgsConstructor
public enum KillEffect {

	none(Material.AIR, "none", "&7None", Effect.EXTINGUISH, 0, false, Material.AIR),
	splash(Material.WATER_BUCKET, "splash", "&bSplash", Effect.SPLASH, 0, false, Material.AIR),
	flame(Material.FLINT_AND_STEEL, "splash", "&bFlame", Effect.FLAME, 0, false, Material.AIR),
	explosion(Material.TNT, "explosion", "&bExplosion", Effect.EXPLOSION, 0, false, Material.AIR),
	ender(Material.ENDER_PEARL, "ender", "&bEnder", Effect.ENDER_SIGNAL, 0, false, Material.AIR),
	blood(Material.REDSTONE, "blood", "&bBlood", Effect.STEP_SOUND, 0, true, Material.REDSTONE_BLOCK),
	firework(Material.FIREWORK, "firework", "&bFirework Spark", Effect.FIREWORKS_SPARK, 0, false, Material.AIR);


	@Getter private final Material icon;
	@Getter private final String name;
	@Getter private final String displayName;
	@Getter private final Effect effect;
	@Getter private final int effectData;
	@Getter private final boolean useMaterial;
	@Getter private final Material material;

}
