package net.frozenorb.potpvp.command;

import com.google.common.collect.ImmutableList;

import net.frozenorb.potpvp.PotPvPLang;
import net.frozenorb.potpvp.PotPvPSI;
import net.frozenorb.potpvp.match.MatchHandler;
import net.frozenorb.potpvp.util.C;
import net.frozenorb.qlib.command.Command;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * Generic /help command, changes message sent based on if sender is playing in
 * or spectating a match.
 */
public final class HelpCommand {

    private static final List<String> HELP_MESSAGE_HEADER = ImmutableList.of(
            C.MAIN + PotPvPLang.LONG_LINE,
        "&b§lPractice Help",
        C.MAIN + PotPvPLang.LONG_LINE,
        "§7§lNOTE: §eMost things are clickable!",
        ""
    );

    private static final List<String> HELP_MESSAGE_LOBBY = ImmutableList.of(
        "&bHelpful Commands:",
        "§f/duel <player> §7- Challenge a player to a duel",
        "§f/party invite <player> §7- Invite a player to a party",
        "",
        "&bOther Commands:",
        "§f/party help §7- Information on party commands",
        "§f/report <player> <reason> §7- Report a player for violating the rules",
        "§f/request <message> §7- Request assistance from a staff member"
    );

    private static final List<String> HELP_MESSAGE_MATCH = ImmutableList.of(
        "&bCommon Commands:",
        "§f/spectate <player> §7- Spectate a player in a match",
        "§f/report <player> <reason> §7- Report a player for violating the rules",
        "§f/request <message> §7- Request assistance from a staff member"
    );

    private static final List<String> HELP_MESSAGE_FOOTER = ImmutableList.of(
        "",
        "&bServer Information:",
        "§fOfficial Teamspeak §7- &bts.prime.gg",
        //"§fOfficial Rules §7- &bforums.orbit.rip/rules",
        "§fStore §7- &bstore.prime.gg",
     // "§ePractice Leaderboards §7- §cwww.minehq.com/stats/potpvp",
        ChatColor.WHITE + PotPvPLang.LONG_LINE
    );

    @Command(names = {"help", "?", "halp", "helpme"}, permission = "")
    public static void help(Player sender) {
        MatchHandler matchHandler = PotPvPSI.getInstance().getMatchHandler();

        HELP_MESSAGE_HEADER.forEach(sender::sendMessage);

        if (matchHandler.isPlayingOrSpectatingMatch(sender)) {
            HELP_MESSAGE_MATCH.forEach(sender::sendMessage);
        } else {
            HELP_MESSAGE_LOBBY.forEach(sender::sendMessage);
        }

        HELP_MESSAGE_FOOTER.forEach(sender::sendMessage);
    }

}
