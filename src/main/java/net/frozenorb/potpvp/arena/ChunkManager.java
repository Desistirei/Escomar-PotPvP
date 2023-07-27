package net.frozenorb.potpvp.arena;

import net.frozenorb.potpvp.PotPvPSI;
import org.bukkit.Chunk;

public class ChunkManager {
    private final PotPvPSI plugin;
    private boolean chunksLoaded;

    public ChunkManager() {
        this.plugin = PotPvPSI.getInstance();
        this.plugin.getServer().getScheduler().runTaskLater(this.plugin, this::loadChunks, 1L);
    }

    private void loadChunks() {
        this.plugin.getLogger().info("Started loading all the chunks...");

        for (ArenaSchematic schematic : this.plugin.getArenaHandler().getSchematics()) {

            if (!schematic.isEnabled())
                continue;

            for (Arena arena : this.plugin.getArenaHandler().getArenas(schematic)) {

                int arenaMinX = arena.getBounds().getLowerX() >> 4;
                int arenaMinZ = arena.getBounds().getLowerZ() >> 4;
                int arenaMaxX = arena.getBounds().getUpperX() >> 4;
                int arenaMaxZ = arena.getBounds().getUpperZ() >> 4;

                if (arenaMinX > arenaMaxX) {
                    final int lastArenaMinX2 = arenaMinX;
                    arenaMinX = arenaMaxX;
                    arenaMaxX = lastArenaMinX2;
                }
                if (arenaMinZ > arenaMaxZ) {
                    final int lastArenaMinZ2 = arenaMinZ;
                    arenaMinZ = arenaMaxZ;
                    arenaMaxZ = lastArenaMinZ2;
                }
                for (int x7 = arenaMinX; x7 <= arenaMaxX; ++x7) {
                    for (int z7 = arenaMinZ; z7 <= arenaMaxZ; ++z7) {
                        final Chunk chunk7 = arena.getBounds().getLowerNE().getWorld().getChunkAt(x7, z7);
                        if (!chunk7.isLoaded()) {
                            chunk7.load();
                        }
                    }
                }
            }
        }
        this.plugin.getLogger().info("Finished loading all the chunks!");
        this.chunksLoaded = true;
    }

    public boolean isChunksLoaded() {
        return this.chunksLoaded;
    }
}
