package net.frozenorb.potpvp.statistics;

import java.util.Map;
import java.util.UUID;

import net.frozenorb.potpvp.profile.Profile;
import net.frozenorb.potpvp.util.CC;
import org.bson.Document;
import org.bukkit.Bukkit;
import org.bukkit.Statistic;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import com.google.common.collect.Maps;
import com.mongodb.client.MongoCollection;

import net.frozenorb.potpvp.PotPvPSI;
import net.frozenorb.potpvp.kittype.KitType;
import net.frozenorb.potpvp.match.Match;
import net.frozenorb.potpvp.match.event.MatchTerminateEvent;
import net.frozenorb.potpvp.util.MongoUtils;
import net.frozenorb.potpvp.util.uuid.UniqueIDCache;
import net.minecraft.util.com.google.common.base.Objects;
import net.minecraft.util.com.google.common.collect.ImmutableMap;

public class StatisticsHandler implements Listener {

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onMatchEnd(MatchTerminateEvent event) {
        Match match = event.getMatch();

        if (match.getKitType().equals(KitType.teamFight)) return;

        match.getWinningPlayers().forEach(uuid -> {

            Profile p = PotPvPSI.getInstance().getProfileManager().getProfile(uuid);
            p.setGamesWon(p.getGamesWon() + 1);
            p.setGamesPlayed(p.getGamesPlayed() + 1);
            p.setWinStreak(p.getWinStreak() + 1);
            p.setStars(p.getStars() + 10);
            if (p.getWinStreak() > p.getHighestWinStreak()) {
                p.setHighestWinStreak(p.getWinStreak());
            }

            p.save();

            Player player = Bukkit.getPlayer(uuid);
            if (player != null) {
                player.sendMessage(CC.translate("&fStars&7: &b+10✧"));
            }

//            rip.orbit.gravity.profile.Profile profile = rip.orbit.gravity.profile.Profile.getByUuid(uuid);
//            profile.getGlobalInfo().setPracticeWins(profile.getGlobalInfo().getPracticeWins() + 1);
//            profile.getGlobalInfo().setPracticeGamesPlayed(profile.getGlobalInfo().getPracticeGamesPlayed() + 1);
//            profile.getGlobalInfo().setPracticeCurrentWinstreak(p.getWinStreak());
//            profile.getGlobalInfo().setPracticeHighestWinstreak(p.getHighestWinStreak());
//            profile.save();

        });
        
        match.getLosingPlayers().forEach(uuid -> {

            Profile p = PotPvPSI.getInstance().getProfileManager().getProfile(uuid);
            p.setLoses(p.getLoses() + 1);
            p.setGamesPlayed(p.getGamesPlayed() + 1);
            p.setWinStreak(0);
            p.save();

//            rip.orbit.gravity.profile.Profile profile = rip.orbit.gravity.profile.Profile.getByUuid(uuid);
//            profile.getGlobalInfo().setPracticeLoses(profile.getGlobalInfo().getPracticeLoses() + 1);
//            profile.getGlobalInfo().setPracticeGamesPlayed(profile.getGlobalInfo().getPracticeGamesPlayed() + 1);
//            profile.save();

        });
    }
}
