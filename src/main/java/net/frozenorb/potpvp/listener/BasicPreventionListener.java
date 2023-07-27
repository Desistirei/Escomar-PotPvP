package net.frozenorb.potpvp.listener;

import java.util.Map;
import java.util.Optional;

import net.frozenorb.potpvp.extras.ability.Ability;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockFadeEvent;
import org.bukkit.event.block.BlockFormEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.BlockSpreadEvent;
import org.bukkit.event.block.LeavesDecayEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.weather.ThunderChangeEvent;
import org.bukkit.event.weather.WeatherChangeEvent;

import net.frozenorb.potpvp.PotPvPSI;

public final class BasicPreventionListener implements Listener {

    @EventHandler
    public void onPlayerKick(PlayerKickEvent event) {
        event.setLeaveMessage(null);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        event.setQuitMessage(null);
    }

    @EventHandler
    public void onProjectileHit(ProjectileHitEvent event) {
        if (event.getEntityType() == EntityType.ARROW) {
            event.getEntity().remove();
        }
    }

    @EventHandler
    public void onThunderChange(ThunderChangeEvent event) {
        event.setCancelled(true);
    }

    @EventHandler
    public void onWeatherChange(WeatherChangeEvent event) {
        event.setCancelled(true);
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        event.setDeathMessage(null);
        event.setDroppedExp(0);
    }

    @EventHandler
    public void onBlockSpread(BlockSpreadEvent event) {
        event.setCancelled(true);
    }

    @EventHandler
    public void onLeavesDecay(LeavesDecayEvent event) {
        event.setCancelled(true);
    }

    @EventHandler
    public void onBlockFade(BlockFadeEvent event) {
        event.setCancelled(true);
    }

    @EventHandler
    public void onBlockForm(BlockFormEvent event) {
        event.setCancelled(true);
    }

    @EventHandler
    public void onPlayerPortal(PlayerPortalEvent event) {
        event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onBlockPlace(BlockPlaceEvent event) {
        Ability dome = PotPvPSI.getInstance().getAbilityHandler().byName("dome");
        if (dome.isSimilar(event.getItemInHand()))
            return;
        if (!canInteractWithBlocks(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        if (!canInteractWithBlocks(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    private boolean canInteractWithBlocks(Player player) {
        if (PotPvPSI.getInstance().getMatchHandler().isPlayingMatch(player)) {
            // completely ignore players in matches, MatchBuildListener handles this.
            return true;
        }

        boolean inLobby = PotPvPSI.getInstance().getLobbyHandler().isInLobby(player);
        boolean isCreative = player.getGameMode() == GameMode.CREATIVE;
        boolean isOp = player.isOp();
        boolean buildMeta = player.hasMetadata("Build");

        return inLobby && isCreative && isOp && buildMeta;
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Ability dome = PotPvPSI.getInstance().getAbilityHandler().byName("dome");
        if (dome.isSimilar(event.getItem()))
            return;
        if (event.getAction() == Action.PHYSICAL && event.getClickedBlock().getType() == Material.SOIL) {
            event.setCancelled(true);
        }
    }

//    @EventHandler
//    public void onPrepareCraft(PrepareItemCraftEvent event) {
//        if (event.get().hasMetadata("trapper"))
//            return;
//        event.getInventory().setResult(null);
//    }

}