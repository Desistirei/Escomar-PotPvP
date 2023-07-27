package net.frozenorb.potpvp.lobby.menu.statistics;

import com.google.common.collect.Lists;
import net.frozenorb.potpvp.PotPvPSI;
import net.frozenorb.potpvp.profile.Profile;
import net.frozenorb.potpvp.util.CC;
import net.frozenorb.qlib.menu.Button;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.List;

public class RecordsButton extends Button {

    @Override
    public String getName(Player player) {
        return player.getDisplayName() + ChatColor.GRAY + " ┃ "  + ChatColor.WHITE + "Records";
    }

    @Override
    public List<String> getDescription(Player player) {
        List<String> description = Lists.newArrayList();

        Profile p = PotPvPSI.getInstance().getProfileManager().getProfile(player);

        description.add(ChatColor.GRAY.toString() + ChatColor.STRIKETHROUGH + "----------------");
        description.add(CC.chat("&7┃ &fGames Played&7: &b" + p.getGamesPlayed()));
        description.add(CC.chat("&7┃ &fWins&7: &b" + p.getGamesWon()));
        description.add(CC.chat("&7┃ &fLoses&7: &b" + p.getLoses()));
        description.add(CC.chat("&7┃ &fKills&7: &b" + p.getKills()));
        description.add(CC.chat("&7┃ &fDeaths&7: &b" + p.getDeaths()));
        description.add(CC.chat("&7┃ &fLongest Combo&7: &b" + p.getHighestCombo()));
        description.add(ChatColor.GRAY.toString() + ChatColor.STRIKETHROUGH + "----------------");

        return description;
    }

    @Override
    public Material getMaterial(Player player) {
        return Material.BOOK;
    }

    @Override
    public byte getDamageValue(Player player) {
        return (byte) 0;
    }
}
