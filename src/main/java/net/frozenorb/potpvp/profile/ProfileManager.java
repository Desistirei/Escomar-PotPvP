package net.frozenorb.potpvp.profile;

import lombok.Getter;
import net.frozenorb.potpvp.PotPvPSI;
import org.bson.Document;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class ProfileManager implements Listener {

    public ProfileManager() {
        Bukkit.getPluginManager().registerEvents(this, PotPvPSI.getInstance());
    }

    @Getter
    private final Map<UUID, Profile> profiles = new ConcurrentHashMap<>();

    public Profile getProfile(UUID uniqueId) {
        for (Profile profile : profiles.values()) {
            if (profile.getUniqueId() == uniqueId)
                return profile;
        }
        return new Profile(uniqueId);
    }

    public Profile getProfile(Player player) {
        for (Profile profile : profiles.values()) {
            if (profile.getUniqueId() == player.getUniqueId())
                return profile;
        }
        return new Profile(player.getUniqueId());
    }

    @EventHandler
    public void onJoin(AsyncPlayerPreLoginEvent event) {
        Profile profile = new Profile(event.getUniqueId());

        PotPvPSI.getInstance().getProfileManager().getProfiles().put(event.getUniqueId(), profile);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        CompletableFuture.runAsync(() -> getProfile(event.getPlayer().getUniqueId()).save());
    }
}
