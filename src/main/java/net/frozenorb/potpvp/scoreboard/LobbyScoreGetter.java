
package net.frozenorb.potpvp.scoreboard;

import java.util.Collection;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.BiConsumer;

import cc.fyre.clans.Clans;
import net.frozenorb.potpvp.profile.Profile;
import net.frozenorb.potpvp.util.CC;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import net.frozenorb.potpvp.PotPvPSI;
import net.frozenorb.potpvp.elo.EloHandler;
import net.frozenorb.potpvp.match.MatchHandler;
import net.frozenorb.potpvp.party.Party;
import net.frozenorb.potpvp.party.PartyHandler;
import net.frozenorb.potpvp.queue.MatchQueue;
import net.frozenorb.potpvp.queue.MatchQueueEntry;
import net.frozenorb.potpvp.queue.QueueHandler;
import net.frozenorb.potpvp.tournament.Tournament;
import net.frozenorb.potpvp.tournament.Tournament.TournamentStage;
import net.frozenorb.potpvp.tournament.TournamentHandler;
import net.frozenorb.qlib.autoreboot.AutoRebootHandler;
import net.frozenorb.qlib.util.LinkedList;
import net.frozenorb.qlib.util.TimeUtils;

final class LobbyScoreGetter implements BiConsumer<Player, LinkedList<String>> {

    private int LAST_ONLINE_COUNT = 0;
    private int LAST_IN_FIGHTS_COUNT = 0;
    private int LAST_IN_QUEUES_COUNT = 0;

    private long lastUpdated = System.currentTimeMillis();

    @Override
    public void accept(Player player, LinkedList<String> scores) {
        PartyHandler partyHandler = PotPvPSI.getInstance().getPartyHandler();
        Party playerParty = partyHandler.getParty(player);
        int stars = PotPvPSI.getInstance().getProfileManager().getProfile(player.getUniqueId()).getStars();
        scores.addAll(PotPvPSI.getInstance().getConfig().getStringList("Scoreboard.Lobby"));
        int index = 0;
        for (final String line : scores) {
            String _line = scores.get(index);
            _line = _line
                    .replace("%player_count%", String.valueOf(LAST_ONLINE_COUNT))
                    .replace("%fights_count%", String.valueOf(LAST_IN_FIGHTS_COUNT))
                    .replace("%queues_count%", String.valueOf(LAST_IN_QUEUES_COUNT))
                    .replace("%stars_count%", String.valueOf(stars));
            scores.set(index, ("&r" + _line));
            ++index;
        }
        Optional<UUID> followingOpt = PotPvPSI.getInstance().getFollowHandler().getFollowing(player);
        MatchHandler matchHandler = PotPvPSI.getInstance().getMatchHandler();
        QueueHandler queueHandler = PotPvPSI.getInstance().getQueueHandler();
        EloHandler eloHandler = PotPvPSI.getInstance().getEloHandler();

        if (playerParty != null) {
            int size = playerParty.getMembers().size();
            scores.add(PotPvPSI.getInstance().getConfig().getString("Scoreboard.Party").replace("%party_members_count%", String.valueOf(size)));
        }

        if (Clans.instance.getClanHandler().getByPlayer(player.getUniqueId()) != null) {
            if (Bukkit.getPluginManager().getPlugin("Clans") != null) {
                scores.add("");
                scores.add("&6&lClan&7: &f" + Objects.requireNonNull(Clans.Companion.getInstance().getClanHandler().getByPlayer(player.getUniqueId())).getName());
                scores.add("&6&lClan ELO&7: &f" + Objects.requireNonNull(Clans.Companion.getInstance().getClanHandler().getByPlayer(player.getUniqueId())).getCachedElo());
                //scores.add("&6&lClan Online" + Objects.requireNonNull(Clans.Companion.getInstance().getClanHandler().getByPlayer(player.getUniqueId())).getOnlinePlayers().size());
            }
        }

        if (2500 <= System.currentTimeMillis() - lastUpdated) {
            lastUpdated = System.currentTimeMillis();
            LAST_ONLINE_COUNT = Bukkit.getOnlinePlayers().size();
            LAST_IN_FIGHTS_COUNT = matchHandler.countPlayersPlayingInProgressMatches();
            LAST_IN_QUEUES_COUNT = queueHandler.getQueuedCount();
        }

        // this definitely can be a .ifPresent, however creating the new lambda that often
        // was causing some performance issues, so we do this less pretty (but more efficent)
        // check (we can't define the lambda up top and reference because we reference the
        // scores variable)
        if (player.hasMetadata("modmode")) {
            double tps = Bukkit.spigot().getTPS()[1];
            scores.add("&bStaff Mode");
            scores.add("&7┃ &fOnline&7: &b" + Bukkit.getOnlinePlayers().size() + "/" + Bukkit.getMaxPlayers());
            scores.add("&7┃ &fVanish&7: &b" + (player.hasMetadata("invisible") ? "&aOn" : "&cOff"));
            if (player.isOp()) {
                scores.add("&7┃ &fTPS&7: &b" + formatBasicTps(tps));
            }
        }
        if (followingOpt.isPresent()) {
            Player following = Bukkit.getPlayer(followingOpt.get());
            scores.add(" ");
            scores.add("&fFollowing: *&b" + following.getName());

            if (player.hasPermission("foxtrot.staff")) {
                MatchQueueEntry targetEntry = getQueueEntry(following);

                if (targetEntry != null) {
                    MatchQueue queue = targetEntry.getQueue();

                    scores.add("&7┃ &fQueued: &b" + (queue.isRanked() ? "(R)" : "(UR)") + " " + queue.getKitType().getDisplayName());
//                    scores.add("&7" + (queue.isRanked() ? "Ranked" : "Unranked") + " " + queue.getKitType().getDisplayName());
                }
            }
        }

        MatchQueueEntry entry = getQueueEntry(player);
        Tournament tournament = PotPvPSI.getInstance().getTournamentHandler().getTournament();
        if (entry != null) {
            String waitTimeFormatted = TimeUtils.formatIntoMMSS(entry.getWaitSeconds());
            MatchQueue queue = entry.getQueue();

            scores.add("&b&7&m--------------------");
            scores.add("&b" + (queue.isRanked() ? "Ranked" : "Unranked") + " " + queue.getKitType().getDisplayName());
            scores.add("&7┃ &fTime: *&b" + waitTimeFormatted);

            if (queue.isRanked()) {
                if (tournament != null) {
                    int elo = eloHandler.getElo(entry.getMembers(), queue.getKitType());
                    int window = entry.getWaitSeconds() * QueueHandler.RANKED_WINDOW_GROWTH_PER_SECOND;

                    scores.add("&7┃ &fSearch range: *&b" + Math.max(0, elo - window) + " - " + (elo + window));
                }
            }
        }

        if (AutoRebootHandler.isRebooting()) {
            String secondsStr = TimeUtils.formatIntoMMSS(AutoRebootHandler.getRebootSecondsRemaining());
            scores.add("&7┃ &fRebooting&7: &b" + secondsStr);
        }

        if (player.hasMetadata("ModMode")) {
            scores.add(ChatColor.GRAY.toString() + ChatColor.BOLD + "In Silent Mode");
        }

        if (tournament != null) {
            scores.add("&7&m--------------------");
            scores.add("&b&lTournament");
            if (tournament.getStage() == TournamentStage.WAITING_FOR_TEAMS) {
                int teamSize = tournament.getRequiredPartySize();
                scores.add("&7┃ &fKit&7: &b" + tournament.getType().getDisplayName());
                scores.add("&7┃ &fTeam Size&7: &b" + teamSize + "v" + teamSize);
                int multiplier = teamSize < 3 ? teamSize : 1;
                scores.add("&7┃ &f" + (teamSize < 3 ? "Players"  : "Teams") + "&7: &b" + (tournament.getActiveParties().size() * multiplier + "/" + tournament.getRequiredPartiesToStart() * multiplier));
            } else if (tournament.getStage() == TournamentStage.COUNTDOWN) {
                if (tournament.getCurrentRound() == 0) {
                    scores.add("&9");
                    scores.add("&7┃ &fBegins in &b" + tournament.getBeginNextRoundIn() + "&f second" + (tournament.getBeginNextRoundIn() == 1 ? "." : "s."));
                } else {
                    scores.add("&9");
                    scores.add("&b&lRound " + (tournament.getCurrentRound() + 1));
                    scores.add("&7┃ &fBegins in &b" + tournament.getBeginNextRoundIn() + "&f second" + (tournament.getBeginNextRoundIn() == 1 ? "." : "s."));
                }
            } else if (tournament.getStage() == TournamentStage.IN_PROGRESS) {
                scores.add("&7┃ &fRound&7: &b" + tournament.getCurrentRound());

                int teamSize = tournament.getRequiredPartySize();
                int multiplier = teamSize < 3 ? teamSize : 1;

                scores.add("&7┃ &f" + (teamSize < 3 ? "Players" : "Teams") + "&7: &b" + tournament.getActiveParties().size() * multiplier + "/" + tournament.getRequiredPartiesToStart() * multiplier);
                scores.add("&7┃ &fDuration&7: &b" + TimeUtils.formatIntoMMSS((int) (System.currentTimeMillis() - tournament.getRoundStartedAt()) / 1000));
            }
        }

        
    }

    private MatchQueueEntry getQueueEntry(Player player) {
        PartyHandler partyHandler = PotPvPSI.getInstance().getPartyHandler();
        QueueHandler queueHandler = PotPvPSI.getInstance().getQueueHandler();

        Party playerParty = partyHandler.getParty(player);
        if (playerParty != null) {
            return queueHandler.getQueueEntry(playerParty);
        } else {
            return queueHandler.getQueueEntry(player.getUniqueId());
        }
    }

    private String formatBasicTps(double tps) {
        return "" + Math.min(Math.round(tps * 10.0) / 10.0, 20.0);
    }
}