package net.frozenorb.potpvp.match;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import com.lunarclient.bukkitapi.LunarClientAPI;
import com.lunarclient.bukkitapi.object.TitleType;
import lombok.Data;
import net.frozenorb.potpvp.extras.ability.profile.Profile;
import net.frozenorb.potpvp.kit.Kit;
import net.frozenorb.potpvp.kit.KitHandler;
import net.frozenorb.potpvp.util.*;
import org.bson.Document;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.craftbukkit.libs.com.google.gson.JsonObject;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BlockVector;
import org.bukkit.util.Vector;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import lombok.Getter;
import lombok.Setter;
import net.frozenorb.potpvp.PotPvPSI;
import net.frozenorb.potpvp.arena.Arena;
import net.frozenorb.potpvp.elo.EloCalculator;
import net.frozenorb.potpvp.kittype.KitType;
import net.frozenorb.potpvp.lobby.LobbyHandler;
import net.frozenorb.potpvp.match.event.MatchCountdownStartEvent;
import net.frozenorb.potpvp.match.event.MatchEndEvent;
import net.frozenorb.potpvp.match.event.MatchSpectatorJoinEvent;
import net.frozenorb.potpvp.match.event.MatchSpectatorLeaveEvent;
import net.frozenorb.potpvp.match.event.MatchStartEvent;
import net.frozenorb.potpvp.match.event.MatchTerminateEvent;
import net.frozenorb.potpvp.match.replay.ReplayableAction;
import net.frozenorb.potpvp.postmatchinv.PostMatchPlayer;
import net.frozenorb.potpvp.setting.Setting;
import net.frozenorb.potpvp.setting.SettingHandler;
import net.frozenorb.qlib.nametag.FrozenNametagHandler;
import net.frozenorb.potpvp.util.uuid.UniqueIDCache;

public final class Match {

	@Getter private static final int MATCH_END_DELAY_SECONDS = 3;


	@Getter private final String _id = UUID.randomUUID().toString().substring(0, 7);


	@Getter private final KitType kitType;

	@Getter private final Arena arena;

	@Getter private final List<MatchTeam> teams; // immutable so  is ok
	@Getter private final Map<UUID, PostMatchPlayer> postMatchPlayers = new HashMap<>();
	@Getter private final Set<UUID> spectators = new HashSet<>();

	@Getter
	private MatchTeam winner;
	@Getter
	private MatchEndReason endReason;
	@Getter
	private MatchState state;
	@Getter
	private Date startedAt;
	@Getter
	private Date endedAt;
	@Getter
	private boolean ranked;

	// we track if matches should give a rematch diamond manually. previouly
	// we just checked if both teams had 1 player on them, but this wasn't
	// always accurate. Scenarios like a team split of a 3 man team (with one
	// sitting out) would get treated as a 1v1 when calculating rematches.
	// https://github.com/FrozenOrb/PotPvP-SI/issues/19
	// this will also be set to false for ranked matches (which don't allow
	// rematches)
	@Getter
	private boolean allowRematches;

	@Getter @Setter
	private EloCalculator.Result eloChange;

	// this will keep track of blocks placed by players during this match.
	// it'll only be populated if the KitType allows building in the first place.
	@Getter private final Set<BlockVector> placedBlocks = new HashSet<>();

	// we only spectators generate one message (either a join or a leave)
	// per match, to prevent spam. This tracks who has used their one message
	@Getter private final transient Set<UUID> spectatorMessagesUsed = new HashSet<>();

	@Getter
	private Map<UUID, UUID> lastHit = Maps.newHashMap();
	@Getter
	private Map<UUID, Integer> combos = Maps.newHashMap();
	@Getter
	private Map<UUID, Integer> totalHits = Maps.newHashMap();
	@Getter
	private Map<UUID, Integer> longestCombo = Maps.newHashMap();
	@Getter
	private Map<UUID, Integer> missedPots = Maps.newHashMap();

	@Getter
	private Map<UUID, Integer> kills = Maps.newHashMap();

	@Getter
	private Map<MatchTeam, Integer> wins = Maps.newHashMap();

	@Getter
	private Map<MatchTeam, Integer> hits = Maps.newHashMap();
	@Getter
	private Map<MatchTeam, Integer> boxingCombos = Maps.newHashMap();

	@Getter
	private Map<UUID, Integer> lives = Maps.newHashMap();

	@Getter
	private Map<UUID, Kit> usedKit = Maps.newHashMap();

	@Getter
	private List<ReplayableAction> replayableActions = Lists.newArrayList();

	@Getter
	private Set<UUID> allPlayers = Sets.newHashSet();

	@Getter
	private Set<UUID> winningPlayers;

	@Getter
	private Set<UUID> losingPlayers;

	public Match(KitType kitType, Arena arena, List<MatchTeam> teams, boolean ranked, boolean allowRematches) {
		this.kitType = Preconditions.checkNotNull(kitType, "kitType");
		this.arena = Preconditions.checkNotNull(arena, "arena");
		this.teams = ImmutableList.copyOf(teams);
		this.ranked = ranked;
		this.allowRematches = allowRematches;
		this.winningPlayers = new HashSet<>();
		this.losingPlayers = new HashSet<>();
		this.allPlayers = new HashSet<>();

		saveState();
	}

	private void saveState() {
		if (kitType.isBuildingAllowed())
			this.arena.takeSnapshot();
	}

	void startCountdown() {
		state = MatchState.COUNTDOWN;

		Map<UUID, Match> playingCache = PotPvPSI.getInstance().getMatchHandler().getPlayingMatchCache();
		Set<Player> updateVisiblity = new HashSet<>();

		boolean choseTrapper = false;

		for (MatchTeam team : this.getTeams()) {
			int i = 0;
			int actualSize = team.getAllMembers().size();
			for (UUID playerUuid : team.getAllMembers()) {
				++i;
				if (!team.isAlive(playerUuid))
					continue;

				Player player = Bukkit.getPlayer(playerUuid);

				playingCache.put(player.getUniqueId(), this);


				Location spawn;
				if (getKitType().getId().equalsIgnoreCase("baseraiding")) {
					spawn = (team == teams.get(0) ? arena.getTeam2Spawn() : arena.getTeam1Spawn()).clone();
					Vector oldDirection = spawn.getDirection();

					Block block = spawn.getBlock();
					while (block.getRelative(BlockFace.DOWN).getType() == Material.AIR) {
						block = block.getRelative(BlockFace.DOWN);
						if (block.getY() <= 0) {
							block = spawn.getBlock();
							break;
						}
					}

					spawn = block.getLocation();
					spawn.setDirection(oldDirection);
				} else {
					spawn = (team == teams.get(0) ? arena.getTeam1Spawn() : arena.getTeam2Spawn()).clone();
					Vector oldDirection = spawn.getDirection();

					Block block = spawn.getBlock();
					while (block.getRelative(BlockFace.DOWN).getType() == Material.AIR) {
						block = block.getRelative(BlockFace.DOWN);
						if (block.getY() <= 0) {
							block = spawn.getBlock();
							break;
						}
					}

					spawn = block.getLocation();
					spawn.setDirection(oldDirection);
				}

				if (getKitType().getId().equalsIgnoreCase("pearlfight")) {
					lives.put(player.getUniqueId(), 3);
				} else {
					lives.put(player.getUniqueId(), 1);
				}

				kills.put(player.getUniqueId(), 0);
				wins.put(team, 0);
				spawn.add(0.5, 0, 0.5);
				player.teleport(spawn);

				player.getInventory().setHeldItemSlot(0);
				if (getKitType().getId().equalsIgnoreCase("baseraiding")) {
					if (!choseTrapper) {
						LunarClientAPI.getInstance().sendTitle(player, TitleType.TITLE, ChatColor.GREEN + "YOU ARE TRAPPING", Duration.ofSeconds(5L));
						player.setMetadata("trapper", new FixedMetadataValue(PotPvPSI.getInstance(), true));
						player.sendMessage(" ");
						player.sendMessage(CC.translate("&aYou have been chosen as a trapper!"));
						player.sendMessage(" ");
					} else {
						LunarClientAPI.getInstance().sendTitle(player, TitleType.TITLE, ChatColor.GREEN + "YOU ARE BASE RAIDING", Duration.ofSeconds(5L));
					}
				}

				hits.putIfAbsent(team, 0);
				player.setMetadata("waiting", new FixedMetadataValue(PotPvPSI.getInstance(), true));
				new BukkitRunnable() {

					int countdownTimeRemaining = kitType.getId().equals("SUMO") ? 5 : 5;

					public void run() {
						if (state != MatchState.COUNTDOWN) {
							cancel();
							player.removeMetadata("waiting", PotPvPSI.getInstance());
							VisibilityUtils.updateVisibility(player);
							return;
						}

						if (countdownTimeRemaining == 0) {
							player.removeMetadata("waiting", PotPvPSI.getInstance());
							VisibilityUtils.updateVisibility(player);
							cancel();
							return; // so we don't send '0...' message
						}
						countdownTimeRemaining--;
					}

				}.runTaskTimer(PotPvPSI.getInstance(), 0L, 20L);

				if (i == actualSize)
					choseTrapper = true;

				FrozenNametagHandler.reloadPlayer(player);
				FrozenNametagHandler.reloadOthersFor(player);

				updateVisiblity.add(player);
				PatchedPlayerUtils.resetInventory(player, GameMode.SURVIVAL);
			}
		}

		// we wait to update visibility until everyone's been put in the player cache
		// then we update vis, otherwise the update code will see 'partial' views of the
		// match
		updateVisiblity.forEach(VisibilityUtils::updateVisibilityFlicker);

		Bukkit.getPluginManager().callEvent(new MatchCountdownStartEvent(this));


		new BukkitRunnable() {

			int countdownTimeRemaining = kitType.getId().equals("SUMO") ? 5 : 5;

			public void run() {
				if (state != MatchState.COUNTDOWN) {
					cancel();
					return;
				}

				if (countdownTimeRemaining == 0) {
					playSoundAll(Sound.NOTE_PLING, 2F);
					startMatch();
					return; // so we don't send '0...' message
				} else if (countdownTimeRemaining <= 3) {
					playSoundAll(Sound.NOTE_PLING, 1F);
				}


				messageAll(ChatColor.YELLOW.toString() + countdownTimeRemaining + "...");
				countdownTimeRemaining--;
			}

		}.runTaskTimer(PotPvPSI.getInstance(), 0L, 20L);
	}

	private void startMatch() {
		state = MatchState.IN_PROGRESS;
		startedAt = new Date();

		messageAll(ChatColor.GREEN + "The match has begun.");
		Bukkit.getPluginManager().callEvent(new MatchStartEvent(this));

		for (UUID uuid : getAllPlayers()) {
			Player player = Bukkit.getPlayer(uuid);
			player.removeMetadata("waiting", PotPvPSI.getInstance());
		}
	}

	public void endMatch(MatchEndReason reason) {
		// prevent duplicate endings
		if (state == MatchState.ENDING || state == MatchState.TERMINATED) {
			return;
		}

		state = MatchState.ENDING;
		endedAt = new Date();
		endReason = reason;
		try {
			for (MatchTeam matchTeam : this.getTeams()) {
				for (UUID playerUuid : matchTeam.getAllMembers()) {
					allPlayers.add(playerUuid);
					Player player = Bukkit.getPlayer(playerUuid);

					if (player != null) {
						player.removeMetadata("trapper", PotPvPSI.getInstance());

						postMatchPlayers.computeIfAbsent(playerUuid, v -> new PostMatchPlayer(player, kitType.getHealingMethod(), totalHits.getOrDefault(player.getUniqueId(), 0), longestCombo.getOrDefault(player.getUniqueId(), 0), missedPots.getOrDefault(player.getUniqueId(), 0)));

					}
				}
			}

			if (getKitType().getId().equalsIgnoreCase("spleef") ||getKitType().getId().equalsIgnoreCase("bridge") || getKitType().getId().equalsIgnoreCase("bridges") || getKitType().getId().equalsIgnoreCase("battlerush") || getKitType().getId().equalsIgnoreCase("skywars") || getKitType().getId().equalsIgnoreCase("builduhc") || getKitType().getId().equalsIgnoreCase("baseraiding"))
				arena.restore();
			messageAll(ChatColor.RED + "Match ended.");
			Bukkit.getPluginManager().callEvent(new MatchEndEvent(this));
		} catch (Exception ex) {
			ex.printStackTrace();
		}

		int delayTicks = MATCH_END_DELAY_SECONDS * 20;
		if (JavaPlugin.getProvidingPlugin(this.getClass()).isEnabled()) {
			Bukkit.getScheduler().runTaskLater(PotPvPSI.getInstance(), this::terminateMatch, delayTicks);
		} else {
			this.terminateMatch();
		}
	}

	public void terminateMatch() {
		// prevent double terminations
		if (state == MatchState.TERMINATED) {
			return;
		}

		state = MatchState.TERMINATED;

		// if the match ends before the countdown ends
		// we have to set this to avoid a NPE in Date#from
		if (startedAt == null) {
			startedAt = new Date();
		}

		// if endedAt wasn't set before (if terminateMatch was called directly)
		// we want to make sure we set an ending time. Otherwise we keep the
		// technically more accurate time set in endMatch
		if (endedAt == null) {
			endedAt = new Date();
		}

		this.winningPlayers = winner.getAllMembers();
		this.losingPlayers = teams.stream().filter(team -> team != winner).flatMap(team -> team.getAllMembers().stream()).collect(Collectors.toSet());

		if (getKitType().getId().equalsIgnoreCase("builduhc") || getKitType().getId().equalsIgnoreCase("baseraiding") || getKitType().getId().equalsIgnoreCase("pearlfight") || getKitType().getId().equalsIgnoreCase("bridges") || getKitType().getId().equalsIgnoreCase("battlerush")) {
			this.getArena().restore();
		}

		Bukkit.getPluginManager().callEvent(new MatchTerminateEvent(this));

//		// we have to make a few edits to the document so we use Gson (which has
//		// adapters
//		// for things like Locations) and then edit it
//		JsonObject document = PotPvPSI.getGson().toJsonTree(this).getAsJsonObject();
//
//		document.addProperty("winner", teams.indexOf(winner)); // replace the full team with their index in the full list
//		document.addProperty("arena", arena.getSchematic()); // replace the full arena with its schematic (website doesn't care which copy we
//		// used)
//g
//		Bukkit.getScheduler().runTaskAsynchronously(PotPvPSI.getInstance(), () -> {
//			// The Document#parse call really sucks. It generates literally thousands of
//			// objects per call.
//			// Hopefully we'll be moving to just posting to a web service soon enough (and
//			// then we don't have to run
//			// Mongo's stupid JSON parser)
//			Document parsedDocument = Document.parse(document.toString());
//			parsedDocument.put("startedAt", startedAt);
//			parsedDocument.put("endedAt", endedAt);
//			MongoUtils.getCollection(MatchHandler.MONGO_COLLECTION_NAME).insertOne(parsedDocument);
//		});

		MatchHandler matchHandler = PotPvPSI.getInstance().getMatchHandler();
		LobbyHandler lobbyHandler = PotPvPSI.getInstance().getLobbyHandler();

		Map<UUID, Match> playingCache = matchHandler.getPlayingMatchCache();
		Map<UUID, Match> spectateCache = matchHandler.getSpectatingMatchCache();

		PotPvPSI.getInstance().getArenaHandler().releaseArena(arena);
		matchHandler.removeMatch(this);

		getTeams().forEach(team -> {
			team.getAllMembers().forEach(player -> {
				if (team.isAlive(player)) {
					playingCache.remove(player);
					spectateCache.remove(player);
					lobbyHandler.returnToLobby(Bukkit.getPlayer(player));
				}
			});
		});

		spectators.forEach(player -> {
			if (Bukkit.getPlayer(player) != null) {
				playingCache.remove(player);
				spectateCache.remove(player);
				lobbyHandler.returnToLobby(Bukkit.getPlayer(player));
			}
		});
	}

	public void terminateMatch(boolean leftMatch, Player left) {
		// prevent double terminations
		if (state == MatchState.TERMINATED) {
			return;
		}

		if (leftMatch) {
			for (MatchTeam team : getTeams()) {
				if (!team.getAllMembers().contains(left.getUniqueId())) {
					winner = team;
					for (UUID allMember : team.getAllMembers()) {
						getWinningPlayers().add(allMember);
					}
				} else {
					try {
						team.getAllMembers().remove(left.getUniqueId());
					} catch (Exception ignored) {

					}
				}
			}
		}

		state = MatchState.TERMINATED;

		// if the match ends before the countdown ends
		// we have to set this to avoid a NPE in Date#from
		if (startedAt == null) {
			startedAt = new Date();
		}

		// if endedAt wasn't set before (if terminateMatch was called directly)
		// we want to make sure we set an ending time. Otherwise we keep the
		// technically more accurate time set in endMatch
		if (endedAt == null) {
			endedAt = new Date();
		}

		try {
			for (MatchTeam matchTeam : this.getTeams()) {
				for (UUID playerUuid : matchTeam.getAllMembers()) {
					allPlayers.add(playerUuid);
					Player player = Bukkit.getPlayer(playerUuid);

					if (player != null) {
						player.removeMetadata("trapper", PotPvPSI.getInstance());

						postMatchPlayers.computeIfAbsent(playerUuid, v -> new PostMatchPlayer(player, kitType.getHealingMethod(), totalHits.getOrDefault(player.getUniqueId(), 0), longestCombo.getOrDefault(player.getUniqueId(), 0), missedPots.getOrDefault(player.getUniqueId(), 0)));

					}
				}
			}

			if (getKitType().getId().equalsIgnoreCase("spleef") ||getKitType().getId().equalsIgnoreCase("bridge") || getKitType().getId().equalsIgnoreCase("battlerush") || getKitType().getId().equalsIgnoreCase("skywars") || getKitType().getId().equalsIgnoreCase("builduhc") || getKitType().getId().equalsIgnoreCase("baseraiding"))
				arena.restore();
			messageAll(ChatColor.RED + "Match ended.");
			Bukkit.getPluginManager().callEvent(new MatchEndEvent(this));
		} catch (Exception ex) {
			ex.printStackTrace();
		}

		this.winningPlayers = winner.getAllMembers();
		this.losingPlayers = teams.stream().filter(team -> team != winner).flatMap(team -> team.getAllMembers().stream()).collect(Collectors.toSet());

		Bukkit.getPluginManager().callEvent(new MatchTerminateEvent(this));

//		// we have to make a few edits to the document so we use Gson (which has
//		// adapters
//		// for things like Locations) and then edit it
//		JsonObject document = PotPvPSI.getGson().toJsonTree(this).getAsJsonObject();
//
//		document.addProperty("winner", teams.indexOf(winner)); // replace the full team with their index in the full list
//		document.addProperty("arena", arena.getSchematic()); // replace the full arena with its schematic (website doesn't care which copy we
//		// used)
//g
//		Bukkit.getScheduler().runTaskAsynchronously(PotPvPSI.getInstance(), () -> {
//			// The Document#parse call really sucks. It generates literally thousands of
//			// objects per call.
//			// Hopefully we'll be moving to just posting to a web service soon enough (and
//			// then we don't have to run
//			// Mongo's stupid JSON parser)
//			Document parsedDocument = Document.parse(document.toString());
//			parsedDocument.put("startedAt", startedAt);
//			parsedDocument.put("endedAt", endedAt);
//			MongoUtils.getCollection(MatchHandler.MONGO_COLLECTION_NAME).insertOne(parsedDocument);
//		});

		MatchHandler matchHandler = PotPvPSI.getInstance().getMatchHandler();
		LobbyHandler lobbyHandler = PotPvPSI.getInstance().getLobbyHandler();

		Map<UUID, Match> playingCache = matchHandler.getPlayingMatchCache();
		Map<UUID, Match> spectateCache = matchHandler.getSpectatingMatchCache();

		PotPvPSI.getInstance().getArenaHandler().releaseArena(arena);
		matchHandler.removeMatch(this);

		getTeams().forEach(team -> {
			team.getAllMembers().forEach(player -> {
				if (team.isAlive(player)) {
					playingCache.remove(player);
					spectateCache.remove(player);
					lobbyHandler.returnToLobby(Bukkit.getPlayer(player));
				}
			});
		});

		spectators.forEach(player -> {
			if (Bukkit.getPlayer(player) != null) {
				playingCache.remove(player);
				spectateCache.remove(player);
				lobbyHandler.returnToLobby(Bukkit.getPlayer(player));
			}
		});
	}

	public Set<UUID> getSpectators() {
		return ImmutableSet.copyOf(spectators);
	}

	public Map<UUID, PostMatchPlayer> getPostMatchPlayers() {
		return ImmutableMap.copyOf(postMatchPlayers);
	}

	public void checkEnded() {
		if (state == MatchState.ENDING || state == MatchState.TERMINATED) {
			return;
		}

		if (!getKitType().getId().equalsIgnoreCase("boxing")) {
			if (getKitType().getId().equalsIgnoreCase("bridges") || getKitType().getId().equalsIgnoreCase("battlerush")) {
				for (MatchTeam team : teams) {
					if (wins.get(team) >= 3) {
						this.winner = team;
						endMatch(MatchEndReason.ENEMIES_ELIMINATED);
					}
				}
			} else {
				List<MatchTeam> teamsAlive = new ArrayList<>();
				for (MatchTeam team : teams) {
					if (!team.getAliveMembers().isEmpty()) {
						teamsAlive.add(team);
					}
				}

				if (teamsAlive.size() == 1) {
					this.winner = teamsAlive.get(0);
					endMatch(MatchEndReason.ENEMIES_ELIMINATED);
				}
			}
		} else {
			if (getKitType().getId().equalsIgnoreCase("boxing")) {

				for (MatchTeam team : teams) {
					if (hits.getOrDefault(team, 0) >= 100) {
						this.winner = team;
					}
				}
				endMatch(MatchEndReason.ENEMIES_ELIMINATED);
			}
		}
	}

	public boolean isSpectator(UUID uuid) {
		return spectators.contains(uuid);
	}

	public void addSpectator(Player player, Player target) {
		addSpectator(player, target, false);
	}

	// fromMatch indicates if they were a player immediately before spectating.
	// we use this for things like teleporting and messages
	public void addSpectator(Player player, Player target, boolean fromMatch) {
		if (getLives().containsKey(player.getUniqueId()) && getLives().get(player.getUniqueId()) > 0)
			return;
		if (!fromMatch && state == MatchState.ENDING) {
			player.sendMessage(ChatColor.RED + "This match is no longer available for spectating.");
			return;
		}

		Map<UUID, Match> spectateCache = PotPvPSI.getInstance().getMatchHandler().getSpectatingMatchCache();

		spectateCache.put(player.getUniqueId(), this);
		spectators.add(player.getUniqueId());

		if (!fromMatch) {
			Location tpTo = arena.getSpectatorSpawn();

			if (target != null) {
				// we tp them a bit up so they're not inside of their target
				tpTo = target.getLocation().clone().add(0, 1.5, 0);
			}

			player.teleport(tpTo);
			player.sendMessage(ChatColor.YELLOW + "Now spectating " + ChatColor.AQUA + getSimpleDescription(true) + ChatColor.YELLOW + "...");
			sendSpectatorMessage(player, ChatColor.AQUA + player.getName() + ChatColor.YELLOW + " is now spectating.");
		} else {
			// so players don't accidentally click the item to stop spectating
			player.getInventory().setHeldItemSlot(0);
		}

		FrozenNametagHandler.reloadPlayer(player);
		FrozenNametagHandler.reloadOthersFor(player);

		VisibilityUtils.updateVisibility(player);
		PatchedPlayerUtils.resetInventory(player, GameMode.CREATIVE, true); // because we're about to reset their inv on a timer
		InventoryUtils.resetInventoryDelayed(player);
		player.setAllowFlight(true);
		player.setFlying(true); // called after PlayerUtils reset, make sure they don't fall out of the sky
		ItemListener.addButtonCooldown(player, 1_500);

		Bukkit.getPluginManager().callEvent(new MatchSpectatorJoinEvent(player, this));
	}

	public void removeSpectator(Player player) {
		removeSpectator(player, true);
	}

	public void removeSpectator(Player player, boolean returnToLobby) {
		Map<UUID, Match> spectateCache = PotPvPSI.getInstance().getMatchHandler().getSpectatingMatchCache();

		spectateCache.remove(player.getUniqueId());
		spectators.remove(player.getUniqueId());
		ItemListener.addButtonCooldown(player, 1_500);

		sendSpectatorMessage(player, ChatColor.AQUA + player.getName() + ChatColor.YELLOW + " is no longer spectating.");

		if (returnToLobby) {
			PotPvPSI.getInstance().getLobbyHandler().returnToLobby(player);
		}

		Bukkit.getPluginManager().callEvent(new MatchSpectatorLeaveEvent(player, this));
	}

	private void sendSpectatorMessage(Player spectator, String message) {
		// see comment on spectatorMessagesUsed field for more
		if (spectator.hasMetadata("ModMode") || !spectatorMessagesUsed.add(spectator.getUniqueId())) {
			return;
		}

		SettingHandler settingHandler = PotPvPSI.getInstance().getSettingHandler();

		for (Player online : Bukkit.getOnlinePlayers()) {
			if (online == spectator) {
				continue;
			}

			boolean sameMatch = isSpectator(online.getUniqueId()) || getTeam(online.getUniqueId()) != null;
			boolean spectatorMessagesEnabled = settingHandler.getSetting(online, Setting.SHOW_SPECTATOR_JOIN_MESSAGES);

			if (sameMatch && spectatorMessagesEnabled) {
				online.sendMessage(message);
			}
		}
	}

	public void markDead(Player player) {
		MatchTeam team = getTeam(player.getUniqueId());
		Profile profile = Profile.byUUID(player.getUniqueId());

		if (team == null) {
			return;
		}

		Player killer = Bukkit.getPlayer(profile.getLastDamagerName());
		net.frozenorb.potpvp.profile.Profile deadProfile = PotPvPSI.getInstance().getProfileManager().getProfile(player.getUniqueId());
		deadProfile.setDeaths(deadProfile.getDeaths() + 1);
		deadProfile.save();

		if (killer != null) {

			net.frozenorb.potpvp.profile.Profile killerProfile = PotPvPSI.getInstance().getProfileManager().getProfile(killer);
			killerProfile.setKills(killerProfile.getKills() + 1);
			killerProfile.save();

			if (getTeam(killer.getUniqueId()) != null) {
				if (getTeams().contains(getTeam(killer.getUniqueId()))) {
					kills.put(killer.getUniqueId(), kills.get(killer.getUniqueId()) + 1);
				}
			}
		}

//		for (MatchTeam matchTeam : getTeams()) {
//			if (matchTeam != team) {
//				int wins = this.wins.get(matchTeam);
//				if (this.wins.containsKey(matchTeam) && wins <= 0) {
//					return;
//				}
//			}
//		}

		if (getKitType().getId().equalsIgnoreCase("bridges") || getKitType().getId().equalsIgnoreCase("battlerush")) {
			Location spawnLoc = (teams.get(0) != null && teams.get(0) == team ? getArena().getTeam1Spawn() : getArena().getTeam2Spawn());
			player.teleport(spawnLoc);
			player.setNoDamageTicks(20);
			getUsedKit().getOrDefault(player.getUniqueId(), Kit.ofDefaultKit(getKitType())).apply(player);
			if (killer != null) {
				if (getTeam(killer.getUniqueId()) != null) {
					if (getTeams().contains(getTeam(killer.getUniqueId()))) {
						messageAll(CC.chat("&b" + UniqueIDCache.name(killer.getUniqueId()) + "&f threw &b" + UniqueIDCache.name(player.getUniqueId()) + " &fin to the void."));
					} else {
						messageAll(CC.chat("&b" + UniqueIDCache.name(player.getUniqueId()) + " &fdied."));
					}
				}
			} else {
				messageAll(CC.chat("&b" + UniqueIDCache.name(player.getUniqueId()) + " &fdied."));
			}
		} else {
			lives.put(player.getUniqueId(), lives.get(player.getUniqueId()) - 1);
			if (lives.get(player.getUniqueId()) <= 0) {

				Map<UUID, Match> playingCache = PotPvPSI.getInstance().getMatchHandler().getPlayingMatchCache();

				team.markDead(player.getUniqueId());
				playingCache.remove(player.getUniqueId());

				postMatchPlayers.put(player.getUniqueId(), new PostMatchPlayer(player, kitType.getHealingMethod(), totalHits.getOrDefault(player.getUniqueId(), 0), longestCombo.getOrDefault(player.getUniqueId(), 0), missedPots.getOrDefault(player.getUniqueId(), 0)));
				checkEnded();

			} else {

				player.getInventory().clear();

				if (getKitType().getId().equalsIgnoreCase("pearlfight")) {
					getUsedKit().getOrDefault(player.getUniqueId(), Kit.ofDefaultKit(getKitType())).apply(player);
				}

				getTeams().forEach(team1 -> {
					for (UUID member : team1.getAllMembers()) {
						Player target = Bukkit.getPlayer(member);
						if (target != null) {
							target.hidePlayer(player);
						}
					}
				});

				player.setMetadata("waiting", new FixedMetadataValue(PotPvPSI.getInstance(), true));

				player.setGameMode(GameMode.CREATIVE);

				LunarClientAPI.getInstance().sendTitle(player, TitleType.TITLE, CC.translate("&bRespawning"), Duration.ofSeconds(3L));

				new BukkitRunnable() {
					@Override
					public void run() {
						if (getState() == MatchState.TERMINATED || getState() == MatchState.ENDING) {
							cancel();
							player.removeMetadata("waiting", PotPvPSI.getInstance());
							player.setGameMode(GameMode.SURVIVAL);

							getTeams().forEach(team1 -> {
								for (UUID member : team1.getAllMembers()) {
									Player target = Bukkit.getPlayer(member);
									if (target != null) {
										target.showPlayer(player);
									}
								}
							});
							VisibilityUtils.updateVisibility(player);
							return;
						}
						VisibilityUtils.updateVisibility(player);
						player.setGameMode(GameMode.SURVIVAL);
						player.removeMetadata("waiting", PotPvPSI.getInstance());
						Location spawnLoc = (teams.get(0) != null && teams.get(0) == team ? getArena().getTeam1Spawn() : getArena().getTeam2Spawn());
						player.setNoDamageTicks(140);
						player.teleport(spawnLoc);

						getTeams().forEach(team1 -> {
							for (UUID member : team1.getAllMembers()) {
								Player target = Bukkit.getPlayer(member);
								if (target != null) {
									target.showPlayer(player);
								}
							}
						});

						new BukkitRunnable() {
							@Override
							public void run() {
								player.setNoDamageTicks(20);
							}
						}.runTaskLater(PotPvPSI.getInstance(), 30);

					}
				}.runTaskLater(PotPvPSI.getInstance(), 20 * 3);
			}
		}
	}

	public MatchTeam getTeam(UUID playerUuid) {
		for (MatchTeam team : teams) {
			if (team.isAlive(playerUuid)) {
				return team;
			}
		}

		return null;
	}

	public MatchTeam getPreviousTeam(UUID playerUuid) {
		for (MatchTeam team : teams) {
			if (team.getAllMembers().contains(playerUuid)) {
				return team;
			}
		}

		return null;
	}

	/**
	 * Creates a simple, one line description of this match This will include two
	 * players (if a 1v1) or player counts and the kit type
	 *
	 * @return A simple description of this match
	 */
	public String getSimpleDescription(boolean includeRankedUnranked) {
		String players;

		if (teams.size() == 2) {
			MatchTeam teamA = teams.get(0);
			MatchTeam teamB = teams.get(1);

			if (teamA.getAliveMembers().size() == 1 && teamB.getAliveMembers().size() == 1) {
				String nameA = UniqueIDCache.name(teamA.getFirstAliveMember());
				String nameB = UniqueIDCache.name(teamB.getFirstAliveMember());

				players = nameA + " vs " + nameB;
			} else {
				players = teamA.getAliveMembers().size() + " vs " + teamB.getAliveMembers().size();
			}
		} else {
			int numTotalPlayers = 0;

			for (MatchTeam team : teams) {
				numTotalPlayers += team.getAliveMembers().size();
			}

			players = numTotalPlayers + " player fight";
		}

		if (includeRankedUnranked) {
			String rankedStr = ranked ? "Ranked" : "Unranked";
			return players + " (" + rankedStr + " " + kitType.getDisplayName() + ")";
		} else {
			return players;
		}
	}

	/**
	 * Sends a basic chat message to all alive participants and spectators
	 *
	 * @param message the message to send
	 */
	public void messageAll(String message) {
		messageAlive(message);
		messageSpectators(message);
	}

	/**
	 * Plays a sound for all alive participants and spectators
	 *
	 * @param sound the Sound to play
	 * @param pitch the pitch to play the provided sound at
	 */
	public void playSoundAll(Sound sound, float pitch) {
		playSoundAlive(sound, pitch);
		playSoundSpectators(sound, pitch);
	}

	/**
	 * Sends a basic chat message to all spectators
	 *
	 * @param message the message to send
	 */
	public void messageSpectators(String message) {
		for (UUID spectator : spectators) {
			Player spectatorBukkit = Bukkit.getPlayer(spectator);

			if (spectatorBukkit != null) {
				spectatorBukkit.sendMessage(message);
			}
		}
	}

	/**
	 * Plays a sound for all spectators
	 *
	 * @param sound the Sound to play
	 * @param pitch the pitch to play the provided sound at
	 */
	public void playSoundSpectators(Sound sound, float pitch) {
		for (UUID spectator : spectators) {
			Player spectatorBukkit = Bukkit.getPlayer(spectator);

			if (spectatorBukkit != null) {
				spectatorBukkit.playSound(spectatorBukkit.getEyeLocation(), sound, 10F, pitch);
			}
		}
	}

	/**
	 * Sends a basic chat message to all alive participants
	 *
	 * @param message the message to send
	 * @see MatchTeam#messageAlive(String)
	 */
	public void messageAlive(String message) {
		for (MatchTeam team : teams) {
			team.messageAlive(message);
		}
	}

	/**
	 * Plays a sound for all alive participants
	 *
	 * @param sound the Sound to play
	 * @param pitch the pitch to play the provided sound at
	 */
	public void playSoundAlive(Sound sound, float pitch) {
		for (MatchTeam team : teams) {
			team.playSoundAlive(sound, pitch);
		}
	}

	/**
	 * Records a placed block during this match. Used to keep track of which blocks
	 * can be broken.
	 */
	public void recordPlacedBlock(Block block) {
		placedBlocks.add(block.getLocation().toVector().toBlockVector());
	}

	/**
	 * Checks if a block can be broken in this match. Only used if the KitType
	 * allows building.
	 */
	public boolean canBeBroken(Block block) {
		if (kitType.getId().equalsIgnoreCase("skywars"))
			return true;
		return (kitType.getId().equalsIgnoreCase("SPLEEF") && (block.getType() == Material.SNOW_BLOCK || block.getType() == Material.GRASS || block.getType() == Material.DIRT)) || placedBlocks.contains(block.getLocation().toVector().toBlockVector());
	}

}