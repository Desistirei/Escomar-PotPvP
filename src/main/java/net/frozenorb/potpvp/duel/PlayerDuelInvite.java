package net.frozenorb.potpvp.duel;

import java.util.Set;
import java.util.UUID;

import lombok.Getter;
import net.frozenorb.potpvp.arena.ArenaSchematic;
import org.bukkit.entity.Player;

import net.frozenorb.potpvp.kittype.KitType;

@Getter
public final class PlayerDuelInvite extends DuelInvite<UUID> {

    private ArenaSchematic schematic;

    public PlayerDuelInvite(Player sender, Player target, KitType kitType) {
        super(sender.getUniqueId(), target.getUniqueId(), kitType);
    }

    public PlayerDuelInvite(Player sender, Player target, KitType kitType, ArenaSchematic schematic) {
        super(sender.getUniqueId(), target.getUniqueId(), kitType);
        this.schematic = schematic;
    }
}