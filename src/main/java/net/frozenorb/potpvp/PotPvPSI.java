package net.frozenorb.potpvp;

import com.mongodb.MongoClient;
import com.mongodb.MongoClientSettings;
import com.mongodb.MongoCredential;
import com.mongodb.ServerAddress;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import lombok.Getter;
import lombok.SneakyThrows;
import net.frozenorb.potpvp.arena.ArenaHandler;
import net.frozenorb.potpvp.arena.ChunkManager;
import net.frozenorb.potpvp.duel.DuelHandler;
import net.frozenorb.potpvp.elo.EloHandler;
import net.frozenorb.potpvp.extras.ability.AbilityHandler;
import net.frozenorb.potpvp.follow.FollowHandler;
import net.frozenorb.potpvp.killeffects.KillEffectHandler;
import net.frozenorb.potpvp.kit.KitHandler;
import net.frozenorb.potpvp.kittype.KitType;
import net.frozenorb.potpvp.kittype.KitTypeJsonAdapter;
import net.frozenorb.potpvp.kittype.KitTypeParameterType;
import net.frozenorb.potpvp.license.License;
import net.frozenorb.potpvp.listener.*;
import net.frozenorb.potpvp.lobby.LobbyHandler;
import net.frozenorb.potpvp.lobby.listener.LobbyParkourListener;
import net.frozenorb.potpvp.match.Match;
import net.frozenorb.potpvp.match.MatchHandler;
import net.frozenorb.potpvp.nametag.PotPvPNametagProvider;
import net.frozenorb.potpvp.party.PartyHandler;
import net.frozenorb.potpvp.postmatchinv.PostMatchInvHandler;
import net.frozenorb.potpvp.profile.ProfileManager;
import net.frozenorb.potpvp.pvpclasses.PvPClassHandler;
import net.frozenorb.potpvp.queue.QueueHandler;
import net.frozenorb.potpvp.rematch.RematchHandler;
import net.frozenorb.potpvp.scoreboard.PotPvPScoreboardConfiguration;
import net.frozenorb.potpvp.setting.SettingHandler;
import net.frozenorb.potpvp.statistics.StatisticsHandler;
import net.frozenorb.potpvp.tab.PotPvPLayoutProvider;
import net.frozenorb.potpvp.tournament.TournamentHandler;
import net.frozenorb.potpvp.util.YamlDoc;
import net.frozenorb.potpvp.util.uuid.UniqueIDCache;
import net.frozenorb.qlib.command.FrozenCommandHandler;
import net.frozenorb.qlib.nametag.FrozenNametagHandler;
import net.frozenorb.qlib.scoreboard.FrozenScoreboardHandler;
import net.frozenorb.qlib.serialization.*;
import net.frozenorb.qlib.tab.FrozenTabHandler;
import org.bukkit.*;
import org.bukkit.craftbukkit.libs.com.google.gson.Gson;
import org.bukkit.craftbukkit.libs.com.google.gson.GsonBuilder;
import org.bukkit.craftbukkit.libs.com.google.gson.TypeAdapter;
import org.bukkit.craftbukkit.libs.com.google.gson.stream.JsonReader;
import org.bukkit.craftbukkit.libs.com.google.gson.stream.JsonWriter;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.util.BlockVector;
import org.bukkit.util.Vector;
import org.spigotmc.SpigotConfig;

import java.io.IOException;
import java.util.Collections;

public final class PotPvPSI extends JavaPlugin {

    private static PotPvPSI instance;
    @Getter private YamlDoc abilityYML;
    @Getter private
    AbilityHandler abilityHandler;
    @Getter private static Gson gson = new GsonBuilder()
        .registerTypeHierarchyAdapter(PotionEffect.class, new PotionEffectAdapter())
        .registerTypeHierarchyAdapter(ItemStack.class, new ItemStackAdapter())
        .registerTypeHierarchyAdapter(Location.class, new LocationAdapter())
        .registerTypeHierarchyAdapter(Vector.class, new VectorAdapter())
        .registerTypeAdapter(BlockVector.class, new BlockVectorAdapter())
        .registerTypeHierarchyAdapter(KitType.class, new KitTypeJsonAdapter()) // custom KitType serializer
        .registerTypeAdapter(ChunkSnapshot.class, new ChunkSnapshotAdapter())
        .serializeNulls()
        .create();

    private MongoClient mongoClient;
    @Getter private MongoDatabase mongoDatabase;

    @Getter private SettingHandler settingHandler;
    @Getter private DuelHandler duelHandler;
    @Getter private KitHandler kitHandler;
    @Getter private LobbyHandler lobbyHandler;
    private ArenaHandler arenaHandler;
    @Getter private MatchHandler matchHandler;
    @Getter private PartyHandler partyHandler;
    @Getter private QueueHandler queueHandler;
    @Getter private RematchHandler rematchHandler;
    @Getter private PostMatchInvHandler postMatchInvHandler;
    @Getter private FollowHandler followHandler;
    @Getter private EloHandler eloHandler;
    @Getter private TournamentHandler tournamentHandler;
    @Getter private KillEffectHandler killEffectHandler;
    @Getter private PvPClassHandler pvpClassHandler;
    @Getter private MongoCollection profilesCollection;
    @Getter private ProfileManager profileManager;

    @Getter private ChatColor dominantColor = ChatColor.RED;

    @SneakyThrows
    @Override
    public void onEnable() {
        SpigotConfig.onlyCustomTab = false;
        this.dominantColor = ChatColor.AQUA;
        instance = this;
        saveDefaultConfig();
        abilityYML = new YamlDoc(getDataFolder(), "ability.yml");
        abilityYML.init();
        abilityHandler = new AbilityHandler();
        setupMongo();
        profilesCollection = mongoDatabase.getCollection("Profiles");
        profileManager = new ProfileManager();
        for (World world : Bukkit.getWorlds()) {
            world.setGameRuleValue("doDaylightCycle", "false");
            world.setGameRuleValue("doMobSpawning", "false");
            world.setTime(6_000L);
        }

        if (!new License(PotPvPSI.getInstance().getConfig().getString("LICENSE"), "http://blessedschif.000webhostapp.com/verify.php", this).register()) return;

        UniqueIDCache.init();
        tournamentHandler = new TournamentHandler();
        settingHandler = new SettingHandler();
        duelHandler = new DuelHandler();
        kitHandler = new KitHandler();
        lobbyHandler = new LobbyHandler();
        arenaHandler = new ArenaHandler();
        matchHandler = new MatchHandler();
        partyHandler = new PartyHandler();
        queueHandler = new QueueHandler();
        rematchHandler = new RematchHandler();
        postMatchInvHandler = new PostMatchInvHandler();
        followHandler = new FollowHandler();
        eloHandler = new EloHandler();
        pvpClassHandler = new PvPClassHandler();
        killEffectHandler = new KillEffectHandler();

        getServer().getPluginManager().registerEvents(new BasicPreventionListener(), this);
        getServer().getPluginManager().registerEvents(new BowHealthListener(), this);
        getServer().getPluginManager().registerEvents(new ChatFormatListener(), this);
        getServer().getPluginManager().registerEvents(new ChatToggleListener(), this);
        getServer().getPluginManager().registerEvents(new LobbyParkourListener(), this);
        getServer().getPluginManager().registerEvents(new ElevatorListener(), this);
        getServer().getPluginManager().registerEvents(new NightModeListener(), this);
        getServer().getPluginManager().registerEvents(new PearlCooldownListener(), this);
        getServer().getPluginManager().registerEvents(new RankedMatchQualificationListener(), this);
        getServer().getPluginManager().registerEvents(new TabCompleteListener(), this);
        getServer().getPluginManager().registerEvents(new StatisticsHandler(), this);

        FrozenCommandHandler.registerAll(this);
        FrozenCommandHandler.registerParameterType(KitType.class, new KitTypeParameterType());

        //FrozenNametagHandler.init();
        FrozenNametagHandler.registerProvider(new PotPvPNametagProvider());
        FrozenScoreboardHandler.setConfiguration(PotPvPScoreboardConfiguration.create());
        FrozenTabHandler.setLayoutProvider(new PotPvPLayoutProvider());

        new ChunkManager();
    }

    @Override
    public void onDisable() {
        for (Match match : this.matchHandler.getHostedMatches()) {
            if (match.getKitType().isBuildingAllowed()) match.getArena().restore();
            if (match.getKitType().getId().equalsIgnoreCase("baseraiding")) match.getArena().restore();
            if (match.getKitType().getId().equalsIgnoreCase("bridges") || match.getKitType().getId().equalsIgnoreCase("battlerush")) match.getArena().restore();
            if (match.getKitType().getId().equalsIgnoreCase("pearlfight")) match.getArena().restore();
        }

        try {
            arenaHandler.saveSchematics();
        } catch (IOException e) {
            e.printStackTrace();
        }

        for (String playerName : PvPClassHandler.getEquippedKits().keySet()) {
            PvPClassHandler.getEquippedKits().get(playerName).remove(getServer().getPlayerExact(playerName));
        }

        instance = null;
    }

    private void setupMongo() {
        if(!getConfig().getBoolean("Mongo.Auth.Enabled")) {
            mongoClient = new com.mongodb.MongoClient(
                    getConfig().getString("Mongo.Host"),
                    getConfig().getInt("Mongo.Port")
            );
        }else {
            mongoClient = new com.mongodb.MongoClient(
                    new ServerAddress(getConfig().getString("Mongo.Host"), getConfig().getInt("Mongo.Port")),
                    Collections.singletonList(MongoCredential.createCredential(getConfig().getString("Mongo.Auth.Username"),
                            getConfig().getString("Mongo.Auth.Database"), getConfig().getString("Mongo.Auth.Password").toCharArray())));
        }

        String databaseId = getConfig().getString("Mongo.Database");
        mongoDatabase = mongoClient.getDatabase(databaseId);
    }

    // This is here because chunk snapshots are (still) being deserialized, and serialized sometimes.
    private static class ChunkSnapshotAdapter extends TypeAdapter<ChunkSnapshot> {

        @Override
        public ChunkSnapshot read(JsonReader arg0) throws IOException {
            return null;
        }

        @Override
        public void write(JsonWriter arg0, ChunkSnapshot arg1) throws IOException {
            
        }
        
    }

    public ArenaHandler getArenaHandler() {
        return arenaHandler;
    }

    public static PotPvPSI getInstance() {
        return instance;
    }


}