/*
 * MurderMystery - Find the murderer, kill him and survive!
 * Copyright (c) 2022  Plugily Projects - maintained by Tigerpanzer_02 and contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package plugily.projects.murdermystery.handlers;

import org.bukkit.Bukkit;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.util.EulerAngle;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;
import org.golde.bukkit.corpsereborn.CorpseAPI.CorpseAPI;
import org.golde.bukkit.corpsereborn.CorpseAPI.events.CorpseClickEvent;
import org.golde.bukkit.corpsereborn.CorpseAPI.events.CorpseSpawnEvent;
import org.golde.bukkit.corpsereborn.nms.Corpses;

import plugily.projects.minigamesbox.api.arena.IArenaState;
import plugily.projects.minigamesbox.api.user.IUser;
import plugily.projects.minigamesbox.classic.utils.version.ServerVersion;
import plugily.projects.minigamesbox.classic.utils.version.VersionUtils;
import plugily.projects.minigamesbox.classic.utils.version.xseries.XMaterial;
import plugily.projects.murdermystery.Main;
import plugily.projects.murdermystery.api.events.game.MurderGameCorpseSpawnEvent;
import plugily.projects.murdermystery.arena.Arena;
import plugily.projects.murdermystery.HookManager;
import plugily.projects.murdermystery.arena.corpse.Corpse;
import plugily.projects.murdermystery.arena.corpse.Stand;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Plajer
 * <p>
 * Created at 07.10.2018
 */
public class CorpseHandler implements Listener {

  interface ExternalCorpseProvider {
    @Nullable
    Runnable spawnCorpse(Player victim, Arena arena, Location groundLoc);

    void clearArenaCorpses(Arena arena);

    void shutdown();
  }

  private final Main plugin;
  private Corpses.CorpseData lastSpawnedCorpse;
  @Nullable
  private ExternalCorpseProvider fancyNpcsProvider;

  private final Map<String, String> registeredLastWords = new HashMap<>();
  private final ItemStack head = XMaterial.PLAYER_HEAD.parseItem();

  public CorpseHandler(Main plugin) {
    this.plugin = plugin;
    if(plugin.getHookManager() != null && plugin.getHookManager().isFeatureEnabled(HookManager.HookFeature.FANCY_NPCS)) {
      initFancyNpcsProvider();
    }
    //run bit later than hook manager to ensure it's not null
    Bukkit.getScheduler().runTaskLater(plugin, () -> {
      if(this.fancyNpcsProvider == null && plugin.getHookManager().isFeatureEnabled(HookManager.HookFeature.FANCY_NPCS)) {
        initFancyNpcsProvider();
      }
      if(plugin.getHookManager().isFeatureEnabled(HookManager.HookFeature.CORPSES)) {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
      }
    }, 20 * 7);
  }

  private void initFancyNpcsProvider() {
    try {
      this.fancyNpcsProvider = new FancyNpcsCorpseHandler(plugin);
      plugin.getDebugger().debug("[CorpseHandler] Hooked into FancyNpcs for player corpses!");
    } catch(Throwable t) {
      this.fancyNpcsProvider = null;
      plugin.getDebugger().debug("[CorpseHandler] Could not initialize FancyNpcs corpse provider: {0}", t.getMessage());
    }
  }

  public void registerLastWord(String permission, String lastWord) {
    registeredLastWords.put(permission, lastWord);
  }

  public void clearArenaCorpses(Arena arena) {
    if(fancyNpcsProvider != null) {
      fancyNpcsProvider.clearArenaCorpses(arena);
    }
  }

  public void shutdown() {
    if(fancyNpcsProvider != null) {
      fancyNpcsProvider.shutdown();
    }
  }

  @SuppressWarnings("deprecation")
  public void spawnCorpse(Player player, Arena arena) {
    spawnCorpse(player, arena, null);
  }

  @SuppressWarnings("deprecation")
  public void spawnCorpse(Player player, Arena arena, @Nullable Player killer) {
    if(arena.getVotingManager().isVotingActive()) {
      return;
    }
    MurderGameCorpseSpawnEvent murderGameCorpseSpawnEvent = new MurderGameCorpseSpawnEvent(arena, player.getPlayer(), player.getLocation(), killer);
    Bukkit.getPluginManager().callEvent(murderGameCorpseSpawnEvent);
    if(murderGameCorpseSpawnEvent.isCancelled()) {
      return;
    }

    Location groundLoc = snapToGround(player.getLocation());
    String displayText = buildCorpseDisplayText(player);

    if(fancyNpcsProvider != null) {
      Runnable cleanupCallback = fancyNpcsProvider.spawnCorpse(player, arena, groundLoc);
      if(cleanupCallback != null) {
        TextDisplay td = arena.spawnTextDisplay(groundLoc.clone().add(0.0D, 0.85D, 0.0D), displayText);
        arena.getVotingManager().registerDeadBody(player.getName(), groundLoc, new ArrayList<>(), td, null, cleanupCallback);
        return;
      }
    }

    if(!plugin.getHookManager().isFeatureEnabled(HookManager.HookFeature.CORPSES)) {
      ArmorStand stand = groundLoc.getWorld().spawn(groundLoc.clone().add(0.0D, -1.25D, 0.0D), ArmorStand.class);
      SkullMeta meta = (SkullMeta) head.getItemMeta();
      meta = VersionUtils.setPlayerHead(player, meta);
      head.setItemMeta(meta);

      stand.setVisible(false);
      stand.setPersistent(false);
      if(ServerVersion.Version.isCurrentEqualOrHigher(ServerVersion.Version.v1_16)) {
        stand.getEquipment().setHelmet(head);
      } else {
        stand.setHelmet(head);
      }
      stand.setGravity(false);
      stand.setCustomNameVisible(false);
      stand.setHeadPose(new EulerAngle(0.0D, Math.toRadians(groundLoc.getYaw()), 0.0D));

      ArmorStand hitboxStand = groundLoc.getWorld().spawn(groundLoc.clone().add(0.0D, -0.2D, 0.0D), ArmorStand.class);
      hitboxStand.setVisible(false);
      hitboxStand.setPersistent(false);
      hitboxStand.setGravity(false);
      hitboxStand.setCustomNameVisible(false);

      plugin.getHologramManager().getArmorStands().add(stand);
      plugin.getHologramManager().getArmorStands().add(hitboxStand);

      arena.addHead(new Stand(null, stand));
      arena.addHead(new Stand(null, hitboxStand));

      TextDisplay td = arena.spawnTextDisplay(groundLoc.clone().add(0.0D, 1.05D, 0.0D), displayText);

      List<ArmorStand> corpseStands = new ArrayList<>();
      corpseStands.add(stand);
      corpseStands.add(hitboxStand);
      arena.getVotingManager().registerDeadBody(player.getName(), groundLoc, corpseStands, td, null);
      return;
    }

    Corpses.CorpseData corpse = CorpseAPI.spawnCorpse(player, groundLoc);
    lastSpawnedCorpse = corpse;
    arena.addCorpse(new Corpse(null, corpse));
    TextDisplay td = arena.spawnTextDisplay(groundLoc.clone().add(0.0D, 0.95D, 0.0D), displayText);
    arena.getVotingManager().registerDeadBody(player.getName(), groundLoc, new ArrayList<>(), td, null);
  }

  private Location snapToGround(Location loc) {
    if(loc == null || loc.getWorld() == null) {
      return loc;
    }
    Location snapped = loc.clone();
    try {
      RayTraceResult hit = loc.getWorld().rayTraceBlocks(
        loc.clone().add(0.0D, 0.5D, 0.0D),
        new Vector(0.0D, -1.0D, 0.0D),
        4.5D,
        FluidCollisionMode.NEVER,
        true
      );
      if(hit != null && hit.getHitPosition() != null) {
        double hitY = hit.getHitPosition().getY();
        if(hitY <= loc.getY() + 0.6D && loc.getY() - hitY <= 4.0D) {
          snapped.setY(hitY);
        }
      }
    } catch(Throwable ignored) {
    }
    return snapped;
  }

  private String buildCorpseDisplayText(Player player) {
    StringBuilder builder = new StringBuilder();
    builder.append("<shadow:#000000FF><#5A4E7C>[</shadow><#FF3B47>Dead Body<shadow:#000000FF><#5A4E7C>]</shadow>\n");
    builder.append("<#F4F1DE>ร่างของ <#FFB703>").append(player.getName());
    if(plugin.getLastWordsManager() != null) {
      String lastWord = plugin.getLastWordsManager().getRandomLastWord(player);
      if(lastWord != null && !lastWord.trim().isEmpty()) {
        builder.append("\n<shadow:#000000FF><#5A4E7C>\"</shadow>").append(lastWord).append("<shadow:#000000FF><#5A4E7C>\"</shadow>");
      }
    }
    builder.append("\n\n<shadow:#000000FF><#5A4E7C>(</shadow><#B8B0C8>กดคลิกขวาเพื่อรายงานศพ<shadow:#000000FF><#5A4E7C>)</shadow>");
    return builder.toString();
  }

  @EventHandler
  public void onCorpseSpawn(CorpseSpawnEvent e) {
    if(lastSpawnedCorpse == null) {
      return;
    }
    if(plugin.getConfigPreferences().getOption("CORPSES_INTEGRATION_OVERWRITE") && !lastSpawnedCorpse.equals(e.getCorpse())) {
      e.setCancelled(true);
    }
  }

  @EventHandler
  public void onCorpseClick(CorpseClickEvent e) {
    if(plugin.getArenaRegistry().isInArena(e.getClicker())) {
      e.setCancelled(true);
      e.getClicker().closeInventory();

      Arena arena = plugin.getArenaRegistry().getArena(e.getClicker());
      if(arena != null && arena.getArenaState() == IArenaState.IN_GAME) {
        IUser user = plugin.getUserManager().getUser(e.getClicker());
        if(!user.isSpectator() && !arena.getVotingManager().isOnKillReportCooldown(e.getClicker())) {
          arena.getVotingManager().startVoting(e.getClicker(), e.getCorpse().getCorpseName());
        }
      }
    }
  }

}
