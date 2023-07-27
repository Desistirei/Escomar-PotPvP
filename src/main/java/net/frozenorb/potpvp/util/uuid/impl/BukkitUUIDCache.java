package net.frozenorb.potpvp.util.uuid.impl;

import net.frozenorb.potpvp.PotPvPSI;
import net.frozenorb.potpvp.util.uuid.UUIDCache;

import java.util.UUID;

public final class BukkitUUIDCache
        implements UUIDCache {
    @Override
    public UUID uuid(String name) {
        return PotPvPSI.getInstance().getServer().getOfflinePlayer(name).getUniqueId();
    }

    @Override
    public String name(UUID uuid) {
        return PotPvPSI.getInstance().getServer().getOfflinePlayer(uuid).getName();
    }

    @Override
    public void ensure(UUID uuid) {
    }

    @Override
    public void update(UUID uuid, String name) {
    }
}

