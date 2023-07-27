package net.frozenorb.potpvp.party.command;

import com.google.common.collect.ImmutableList;

import net.frozenorb.potpvp.PotPvPLang;
import net.frozenorb.qlib.command.Command;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.List;

public final class PartyHelpCommand {

    private static final List<String> HELP_MESSAGE = ImmutableList.of(
        ChatColor.WHITE + PotPvPLang.LONG_LINE,
        "&b§lParty Help §7- §fInformation on how to use party commands",
        ChatColor.WHITE + PotPvPLang.LONG_LINE,
        "&bParty Commands:",
        "§f/party invite §7- Invite a player to join your party",
        "§f/party leave §7- Leave your current party",
        "§f/party accept [player] §7- Accept party invitation",
        "§f/party info [player] §7- View the roster of the party",
        "",
        "&bLeader Commands:",
        "§f/party kick <player> §7- Kick a player from your party",
        "§f/party leader <player> §7- Transfer party leadership",
        "§f/party disband §7 - Disbands party",
        "§f/party lock §7 - Lock party from others joining",
        "§f/party open §7 - Open party to others joining",
        "§f/party password <password> §7 - Sets party password",
        "",
        "&bOther Help:",
        "§fTo use &bparty chat§f, prefix your message with the §7'&b@§7' §fsign.",
        ChatColor.WHITE + PotPvPLang.LONG_LINE
    );

    @Command(names = {"party", "p", "t", "team", "f", "party help", "p help", "t help", "team help", "f help"}, permission = "")
    public static void party(Player sender) {
        HELP_MESSAGE.forEach(sender::sendMessage);
    }

}