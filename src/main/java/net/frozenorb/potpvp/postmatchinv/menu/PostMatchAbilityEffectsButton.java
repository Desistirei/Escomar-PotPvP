package net.frozenorb.potpvp.postmatchinv.menu;

import net.frozenorb.potpvp.extras.ability.items.Thorns;
import net.frozenorb.potpvp.extras.ability.items.Warrior;
import net.frozenorb.potpvp.util.CC;
import net.frozenorb.potpvp.util.uuid.UniqueIDCache;
import net.frozenorb.qlib.menu.Button;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

final class PostMatchAbilityEffectsButton extends Button {

    private final UUID player;

    PostMatchAbilityEffectsButton(UUID player) {
        this.player = player;
    }

    @Override
    public String getName(Player player) {
        return ChatColor.AQUA + UniqueIDCache.name(this.player) + "'s Ability Effects";
    }

    @Override
    public List<String> getDescription(Player player) {
        List<String> abilityEffects = new ArrayList<>();
        if (Warrior.warrior.onCooldown(player)) {
            abilityEffects.add(CC.chat("&b" + UniqueIDCache.name(this.player) + " &fhad a &cWarrior Ability&f activated."));
        }
        if (Thorns.thorned.contains(player.getUniqueId())) {
            abilityEffects.add(CC.chat("&b" + UniqueIDCache.name(this.player) + " &fhad a &eThorns Ability&f activated."));
        }

        if (abilityEffects.isEmpty()) {
            abilityEffects.add(CC.chat("&cNone"));
        }

        return abilityEffects;
    }

    @Override
    public Material getMaterial(Player player) {
        return Material.EMERALD;
    }

    @Override
    public ItemStack getButtonItem(Player player) {
        return super.getButtonItem(player);
    }

    @Override
    public int getAmount(Player player) {
        return 1;
    }

}