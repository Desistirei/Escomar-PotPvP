package net.frozenorb.potpvp.scoreboard;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.BiConsumer;

import net.frozenorb.potpvp.command.StuckCommand;
import net.frozenorb.potpvp.kittype.HealingMethod;
import net.frozenorb.potpvp.listener.PearlCooldownListener;
import net.frozenorb.potpvp.pvpclasses.pvpclasses.ArcherClass;
import net.frozenorb.potpvp.pvpclasses.pvpclasses.BardClass;
import net.frozenorb.potpvp.util.BridgeUtil;
import net.frozenorb.potpvp.util.CC;
import net.frozenorb.potpvp.util.uuid.UniqueIDCache;
import net.frozenorb.qlib.scoreboard.ScoreFunction;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import com.google.common.collect.ImmutableMap;

import net.frozenorb.potpvp.PotPvPSI;
import net.frozenorb.potpvp.match.Match;
import net.frozenorb.potpvp.match.MatchHandler;
import net.frozenorb.potpvp.match.MatchState;
import net.frozenorb.potpvp.match.MatchTeam;
import net.frozenorb.qlib.autoreboot.AutoRebootHandler;
import net.frozenorb.qlib.util.LinkedList;
import net.frozenorb.qlib.util.PlayerUtils;
import net.frozenorb.qlib.util.TimeUtils;

// the list here must be viewed as rendered javadoc to make sense. In IntelliJ, click on
// 'MatchScoreGetter' and press Control+Q

/**
 * Implements the scoreboard as defined in {@link net.frozenorb.potpvp.scoreboard}<br />
 * This class is divided up into multiple prodcedures to reduce overall complexity<br /><br />
 * <p>
 * Although there are many possible outcomes, for a 4v4 match this code would take the
 * following path:<br /><br />
 *
 * <ul>
 *   <li>accept()</li>
 *   <ul>
 *     <li>renderParticipantLines()</li>
 *     <ul>
 *       <li>render4v4MatchLines()</li>
 *       <ul>
 *         <li>renderTeamMemberOverviewLines()</li>
 *         <li>renderTeamMemberOverviewLines()</li>
 *       </ul>
 *     </ul>
 *     <li>renderMetaLines()</li>
 *   </ul>
 * </ul>
 */
final class MatchScoreGetter implements BiConsumer<Player, LinkedList<String>> {

	// we can't count heals on an async thread so we use
	// a task to count and then report values (via this map) to
	// the scoreboard thread
	private Map<UUID, Integer> healsLeft = ImmutableMap.of();

	MatchScoreGetter() {
		Bukkit.getScheduler().runTaskTimerAsynchronously(PotPvPSI.getInstance(), () -> {
			MatchHandler matchHandler = PotPvPSI.getInstance().getMatchHandler();
			Map<UUID, Integer> newHealsLeft = new HashMap<>();

			for (Player player : Bukkit.getOnlinePlayers()) {
				Match playing = matchHandler.getMatchPlaying(player);

				if (playing == null) {
					continue;
				}

				HealingMethod healingMethod = playing.getKitType().getHealingMethod();

				if (healingMethod == null) {
					continue;
				}

				int count = healingMethod.count(player.getInventory().getContents());
				newHealsLeft.put(player.getUniqueId(), count);
			}

			this.healsLeft = newHealsLeft;
		}, 10L, 10L);
	}

	@Override
	public void accept(Player player, LinkedList<String> scores) {
		Optional<UUID> followingOpt = PotPvPSI.getInstance().getFollowHandler().getFollowing(player);
		MatchHandler matchHandler = PotPvPSI.getInstance().getMatchHandler();
		Match match = matchHandler.getMatchPlayingOrSpectating(player);

		// this method shouldn't have even been called if
		// they're not in a match
		if (match == null) {

			if (AutoRebootHandler.isRebooting()) {
				String secondsStr = TimeUtils.formatIntoMMSS(AutoRebootHandler.getRebootSecondsRemaining());
				scores.add("&7┃ &fRebooting&7: &b" + secondsStr);
			}

			if (player.hasMetadata("ModMode")) {
				scores.add(ChatColor.YELLOW.toString() + ChatColor.BOLD + "In Silent Mode");
			}
			return;
		}

		boolean participant = match.getTeam(player.getUniqueId()) != null;
		boolean renderPing = false;

		if (participant) {
			renderPing = renderParticipantLines(scores, match, player);
		} else {
			MatchTeam previousTeam = match.getPreviousTeam(player.getUniqueId());
			renderSpectatorLines(scores, match, previousTeam);
		}

		// this definitely can be a .ifPresent, however creating the new lambda that often
		// was causing some performance issues, so we do this less pretty (but more efficent)
		// check (we can't define the lambda up top and reference because we reference the
		// scores variable)
		followingOpt.ifPresent(uuid -> scores.add("&7┃ &fFollowing: *&b" + UniqueIDCache.name(uuid)));

		if (AutoRebootHandler.isRebooting()) {
			String secondsStr = TimeUtils.formatIntoMMSS(AutoRebootHandler.getRebootSecondsRemaining());
			scores.add("&7┃ &4&lReboot&7: &f" + secondsStr);
		}

		if (player.hasMetadata("ModMode")) {
			scores.add(ChatColor.YELLOW.toString() + ChatColor.BOLD + "In Silent Mode");
		}
	}

	private boolean renderParticipantLines(List<String> scores, Match match, Player player) {
		List<MatchTeam> teams = match.getTeams();

		// only render scoreboard if we have two teams
		if (teams.size() != 2) {
			return false;
		}

		// this method won't be called if the player isn't a participant
		MatchTeam ourTeam = match.getTeam(player.getUniqueId());
		MatchTeam otherTeam = teams.get(0) == ourTeam ? teams.get(1) : teams.get(0);

		// we use getAllMembers instead of getAliveMembers to avoid
		// mid-match scoreboard changes as players die / disconnect
		int ourTeamSize = ourTeam.getAllMembers().size();
		int otherTeamSize = otherTeam.getAllMembers().size();

		if (ourTeamSize == 1 && otherTeamSize == 1) {
			Player other = Bukkit.getPlayer(otherTeam.getFirstMember());
			scores.add("&aYour Ping: &f" + PlayerUtils.getPing(player));
			scores.add(("&cYour Ping: &f") + PlayerUtils.getPing(other));
		} else if (ourTeamSize <= 2 && otherTeamSize <= 2) {
			render2v2MatchLines(scores, ourTeam, otherTeam, player, match.getKitType().getHealingMethod());
		} else if (ourTeamSize <= 4 && otherTeamSize <= 4) {
			render4v4MatchLines(scores, ourTeam, otherTeam);
		} else if (ourTeam.getAllMembers().size() <= 9) {
			renderLargeMatchLines(scores, ourTeam, otherTeam);
		} else {
			renderJumboMatchLines(scores, ourTeam, otherTeam);
		}

		String archerMarkScore = getArcherMarkScore(player);
		String bardEffectScore = getBardEffectScore(player);
		String bardEnergyScore = getBardEnergyScore(player);

		renderMetaLines(scores, match, player);

		if (archerMarkScore != null) {
			scores.add("&7┃ &fArcher Mark&7: &b" + archerMarkScore);
		}

		if (bardEffectScore != null) {
			scores.add("&7┃ &fBard Effect&7: &b" + bardEffectScore);
		}

		if (bardEnergyScore != null) {
			scores.add("&7┃ &fBard Energy&7: &b" + bardEnergyScore);
		}

		long cooldownExpires = PearlCooldownListener.pearlCooldown.getOrDefault(player.getUniqueId(), 0L);

		int millisLeft = (int) (cooldownExpires - System.currentTimeMillis());
		double secondsLeft = millisLeft / 1000D;
		// round to 1 digit past decimal
		secondsLeft = Math.round(10D * secondsLeft) / 10D;

		if (!(cooldownExpires < System.currentTimeMillis())) {
			scores.add("&7┃ &fEnderpearl&7: &b" + secondsLeft);
		}

		if (PotPvPSI.getInstance().getAbilityHandler().getAbilityCD().onCooldown(player)) {
			scores.add("&7┃ &fAbility Item&7: &b" + PotPvPSI.getInstance().getAbilityHandler().getAbilityCD().getRemaining(player));
		}

		if (StuckCommand.stuckTime.onCooldown(player)) {
			scores.add("&7┃ &fStuck&7: &b" + StuckCommand.stuckTime.getRemaining(player));
		}

		if (match.getKitType().getId().equalsIgnoreCase("PEARLFIGHT")) {
			scores.add("");
			scores.add("&b&lLives");
			for (Map.Entry<UUID, Integer> entry : match.getLives().entrySet()) {
				scores.add("&7┃ &f" + UniqueIDCache.name(entry.getKey()) + "&7: &b" + entry.getValue());
			}
		}

		if (match.getKitType().getId().equalsIgnoreCase("bridges") || match.getKitType().getId().equalsIgnoreCase("battlerush") || match.getKitType().getId().equalsIgnoreCase("battlerush")) {
			scores.add("");
			scores.add("&b&lPlayer Kills");
			for (Map.Entry<UUID, Integer> entry : match.getKills().entrySet()) {
				scores.add("&7┃ &f" + UniqueIDCache.name(entry.getKey()) + "&7: &b" + entry.getValue());
			}
			scores.add("");
			for (MatchTeam team : match.getTeams()) {
				if (team.getAllMembers().contains(player.getUniqueId())) {
					scores.add("&7┃ &fYour Team&7: " + BridgeUtil.barBuilder(match.getWins().get(team), "&b"));
				} else {
					scores.add("&7┃ &fEnemy Team&7: " + BridgeUtil.barBuilder(match.getWins().get(team), "&b"));
				}
			}
		}

		if (match.getKitType().getId().equalsIgnoreCase("boxing")) {

			MatchTeam them = null;
			MatchTeam you = match.getTeam(player.getUniqueId());
			for (MatchTeam team : match.getTeams()) {
				if (!team.getAllMembers().contains(player.getUniqueId())) {
					them = team;
				}
			}

			int youHits = match.getHits().getOrDefault(you, 0);
			int themHits = 0;
			if (them != null) {
				themHits = match.getHits().getOrDefault(them, 0);
			}

			int difference = youHits - themHits;
			String differenceText = (difference != 0 ? CC.translate((difference < 0 ?
					CC.translate(" &c(" + difference + ")")
					: CC.translate(" &a(+" + difference + ")"))) : "");

			scores.add(" ");
			scores.add("&bHits" + differenceText);

			int youCombos = match.getBoxingCombos().getOrDefault(you, 0);
			int themCombos = 0;

			if (them != null) {
				themCombos = match.getBoxingCombos().getOrDefault(them, 0);
			}

			String youAdd = (youCombos > 1 ? CC.translate(" &f(" + youCombos + " Combo)") : "");
			String themAdd = (themCombos > 1 ? CC.translate(" &f(" + themCombos + " Combo)") : "");

			scores.add("&7┃ &fYou&7: &b" + youHits + youAdd);
			scores.add("&7┃ &fThem&7: &b" + themHits + themAdd);
		}


		return false;
	}

	private void render2v2MatchLines(List<String> scores, MatchTeam ourTeam, MatchTeam otherTeam, Player player, HealingMethod healingMethod) {
		// 2v2, but potentially 1v2 / 1v1 if players have died
		UUID partnerUuid = null;

		for (UUID teamMember : ourTeam.getAllMembers()) {
			if (teamMember != player.getUniqueId()) {
				partnerUuid = teamMember;
				break;
			}
		}

		if (partnerUuid != null) {
			String healthStr;
			String healsStr;
			String namePrefix;

			if (ourTeam.isAlive(partnerUuid)) {
				Player partnerPlayer = Bukkit.getPlayer(partnerUuid); // will never be null (or isAlive would've returned false)
				double health = Math.round(partnerPlayer.getHealth()) / 2D;
				int heals = healsLeft.getOrDefault(partnerUuid, 0);

				ChatColor healthColor;
				ChatColor healsColor;

				if (health > 8) {
					healthColor = ChatColor.GREEN;
				} else if (health > 6) {
					healthColor = ChatColor.YELLOW;
				} else if (health > 4) {
					healthColor = ChatColor.AQUA;
				} else if (health > 1) {
					healthColor = ChatColor.RED;
				} else {
					healthColor = ChatColor.DARK_RED;
				}

				if (heals > 20) {
					healsColor = ChatColor.GREEN;
				} else if (heals > 12) {
					healsColor = ChatColor.YELLOW;
				} else if (heals > 8) {
					healsColor = ChatColor.AQUA;
				} else if (heals > 3) {
					healsColor = ChatColor.RED;
				} else {
					healsColor = ChatColor.DARK_RED;
				}

				namePrefix = "&a";
				healthStr = healthColor.toString() + health + " *❤*" + ChatColor.GRAY;

				if (healingMethod != null) {
					healsStr = " &l⏐ " + healsColor.toString() + heals + " " + (heals == 1 ? healingMethod.getShortSingular() : healingMethod.getShortPlural());
				} else {
					healsStr = "";
				}
			} else {
				namePrefix = "&7&m";
				healthStr = "&4RIP";
				healsStr = "";
			}

			scores.add(namePrefix + UniqueIDCache.name(partnerUuid));
			scores.add(healthStr + healsStr);
			scores.add("&b");
		}

		scores.add("&bOpponents");
		scores.addAll(renderTeamMemberOverviewLines(otherTeam));

		// Removes the space
		if (PotPvPSI.getInstance().getMatchHandler().getMatchPlaying(player).getState() == MatchState.IN_PROGRESS) {
			scores.add("&c");
		}
	}

	private void render4v4MatchLines(List<String> scores, MatchTeam ourTeam, MatchTeam otherTeam) {
		// Above a 2v2, but up to a 4v4.
		scores.add("&bOpponents &f(" + otherTeam.getAliveMembers().size() + "/" + otherTeam.getAllMembers().size() + ")");
		scores.addAll(renderTeamMemberOverviewLines(otherTeam));
		scores.add("&b");
		scores.add("&bTeam &f(" + ourTeam.getAliveMembers().size() + "/" + ourTeam.getAllMembers().size() + ")");
		scores.addAll(renderTeamMemberOverviewLinesWithHearts(ourTeam));
		if (PotPvPSI.getInstance().getMatchHandler().getMatchPlaying(Bukkit.getPlayer(ourTeam.getFirstAliveMember())).getState() == MatchState.IN_PROGRESS) {
			scores.add("&c");
		}
	}

	private void renderLargeMatchLines(List<String> scores, MatchTeam ourTeam, MatchTeam otherTeam) {
		// We just display THEIR team's names, and the other team is a number.
		scores.add("&7┃ &fOpponents: &b" + otherTeam.getAliveMembers().size() + "/" + otherTeam.getAllMembers().size());
		scores.add("&b");
		scores.add("&7┃ &fTeam &b(" + ourTeam.getAliveMembers().size() + "/" + ourTeam.getAllMembers().size() + ")");
		scores.addAll(renderTeamMemberOverviewLinesWithHearts(ourTeam));
	}

	private void renderJumboMatchLines(List<String> scores, MatchTeam ourTeam, MatchTeam otherTeam) {
		// We just display numbers.
		scores.add("&7┃ &fOpponents: &b" + otherTeam.getAliveMembers().size() + "/" + otherTeam.getAllMembers().size());
		scores.add("&7┃ &fTeam: &b" + ourTeam.getAliveMembers().size() + "/" + ourTeam.getAllMembers().size());
	}

	private void renderSpectatorLines(List<String> scores, Match match, MatchTeam oldTeam) {
		String rankedStr = match.isRanked() ? " (R)" : "";
		scores.add("&7┃ &fKit: &b" + match.getKitType().getColoredDisplayName() + rankedStr);

		List<MatchTeam> teams = match.getTeams();

		if (match.getKitType().getId().equalsIgnoreCase("PEARLFIGHT")) {
			scores.add("");
			scores.add("&b&lLives");
			for (Map.Entry<UUID, Integer> entry : match.getLives().entrySet()) {
				scores.add("&7┃ &f" + UniqueIDCache.name(entry.getKey()) + "&7: &b" + entry.getValue());
			}
		}

		if (match.getKitType().getId().equalsIgnoreCase("bridges") || match.getKitType().getId().equalsIgnoreCase("battlerush")) {
			scores.add("");
			scores.add("&b&lPlayer Kills");
			for (Map.Entry<UUID, Integer> entry : match.getKills().entrySet()) {
				scores.add("&7┃ &f" + UniqueIDCache.name(entry.getKey()) + "&7: &b" + entry.getValue());
			}
			scores.add("");

			int i = 1;
			for (MatchTeam team : match.getTeams()) {
				scores.add("&7┃ &fTeam # " + i + "&7: " + BridgeUtil.barBuilder(match.getWins().get(team), "&b"));
			}
		}
		if (match.getKitType().getId().equalsIgnoreCase("boxing")) {
			int i = 1;
			scores.add(" ");
			scores.add("&bHits");
			for (MatchTeam team : match.getTeams()) {
				int hits = 0;
				for (UUID member : team.getAllMembers()) {
					hits = hits + match.getTotalHits().get(member);
				}

				scores.add("&7┃ &fTeam #" + i + "&7: &b" + hits);
				++i;
			}
		}

		// only render team overview if we have two teams
		if (teams.size() == 2) {
			MatchTeam teamOne = teams.get(0);
			MatchTeam teamTwo = teams.get(1);

			if (teamOne.getAllMembers().size() != 1 && teamTwo.getAllMembers().size() != 1) {
				// spectators who were on a team see teams as they releate
				// to them, not just one/two.
				if (oldTeam == null) {
					scores.add("&7┃ &fTeam One: &b" + teamOne.getAliveMembers().size() + "/" + teamOne.getAllMembers().size());
					scores.add("&7┃ &fTeam Two: &b" + teamTwo.getAliveMembers().size() + "/" + teamTwo.getAllMembers().size());
				} else {
					MatchTeam otherTeam = oldTeam == teamOne ? teamTwo : teamOne;

					scores.add("&fOpponents: &b" + otherTeam.getAliveMembers().size() + "/" + otherTeam.getAllMembers().size());
					scores.add("&7┃ &fTeam: &b" + oldTeam.getAliveMembers().size() + "/" + oldTeam.getAllMembers().size());
				}
			}
		}
	}

	private void renderMetaLines(List<String> scores, Match match, Player player) {
		Date startedAt = match.getStartedAt();
		Date endedAt = match.getEndedAt();
		String formattedDuration;

		// short circuit for matches which are still counting down
		// or which ended before they started (if a player disconnects
		// during countdown)
		if (startedAt == null) {
			return;
		} else {
			// we go from when it started to either now (if it's in progress)
			// or the timestamp at which the match actually ended
			formattedDuration = TimeUtils.formatLongIntoMMSS(ChronoUnit.SECONDS.between(
					startedAt.toInstant(),
					endedAt == null ? Instant.now() : endedAt.toInstant()
			));
		}

		// spectators don't have any bold entries on their scoreboard
		scores.add(PotPvPSI.getInstance().getDominantColor() + "&7┃ &fDuration: &b" + formattedDuration);
	}

    /* Returns the names of all alive players, colored + indented, followed
       by the names of all dead players, colored + indented. */

	private List<String> renderTeamMemberOverviewLinesWithHearts(MatchTeam team) {
		List<String> aliveLines = new ArrayList<>();
		List<String> deadLines = new ArrayList<>();

		// seperate lists to sort alive players before dead
		// + color differently
		for (UUID teamMember : team.getAllMembers()) {
			if (team.isAlive(teamMember)) {
				aliveLines.add("&7┃ &f" + UniqueIDCache.name(teamMember) + " " + getHeartString(team, teamMember));
			} else {
				deadLines.add("&7┃ &7&m" + UniqueIDCache.name(teamMember));
			}
		}

		List<String> result = new ArrayList<>();

		result.addAll(aliveLines);
		result.addAll(deadLines);

		return result;
	}

	private List<String> renderTeamMemberOverviewLines(MatchTeam team) {
		List<String> aliveLines = new ArrayList<>();
		List<String> deadLines = new ArrayList<>();

		// seperate lists to sort alive players before dead
		// + color differently
		for (UUID teamMember : team.getAllMembers()) {
			if (team.isAlive(teamMember)) {
				aliveLines.add("&7┃ &f" + UniqueIDCache.name(teamMember));
			} else {
				deadLines.add("&7┃ &7&m" + UniqueIDCache.name(teamMember));
			}
		}

		List<String> result = new ArrayList<>();

		result.addAll(aliveLines);
		result.addAll(deadLines);

		return result;
	}

	private String getHeartString(MatchTeam ourTeam, UUID partnerUuid) {
		if (partnerUuid != null) {
			String healthStr;

			if (ourTeam.isAlive(partnerUuid)) {
				Player partnerPlayer = Bukkit.getPlayer(partnerUuid); // will never be null (or isAlive would've returned false)
				double health = Math.round(partnerPlayer.getHealth()) / 2D;

				ChatColor healthColor;

				if (health > 8) {
					healthColor = ChatColor.GREEN;
				} else if (health > 6) {
					healthColor = ChatColor.YELLOW;
				} else if (health > 4) {
					healthColor = ChatColor.AQUA;
				} else if (health > 1) {
					healthColor = ChatColor.RED;
				} else {
					healthColor = ChatColor.DARK_RED;
				}

				healthStr = healthColor.toString() + "(" + health + " ❤)";
			} else {
				healthStr = "&4(RIP)";
			}

			return healthStr;
		} else {
			return "&4(RIP)";
		}
	}

	public String getArcherMarkScore(Player player) {
		if (ArcherClass.isMarked(player)) {
			long diff = ArcherClass.getMarkedPlayers().get(player.getName()) - System.currentTimeMillis();

			if (diff > 0) {
				return (ScoreFunction.TIME_FANCY.apply(diff / 1000F));
			}
		}

		return (null);
	}

	public String getBardEffectScore(Player player) {
		if (BardClass.getLastEffectUsage().containsKey(player.getName()) && BardClass.getLastEffectUsage().get(player.getName()) >= System.currentTimeMillis()) {
			float diff = BardClass.getLastEffectUsage().get(player.getName()) - System.currentTimeMillis();

			if (diff > 0) {
				return (ScoreFunction.TIME_SIMPLE.apply(diff / 1000F));
			}
		}

		return (null);
	}

	public String getBardEnergyScore(Player player) {
		if (BardClass.getEnergy().containsKey(player.getName())) {
			float energy = BardClass.getEnergy().get(player.getName());

			if (energy > 0) {
				// No function here, as it's a "raw" value.
				return (String.valueOf(BardClass.getEnergy().get(player.getName())));
			}
		}

		return (null);
	}
}
