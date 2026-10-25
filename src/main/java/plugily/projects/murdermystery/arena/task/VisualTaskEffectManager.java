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

package plugily.projects.murdermystery.arena.task;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.scheduler.BukkitTask;
import plugily.projects.minigamesbox.classic.utils.version.xseries.XSound;
import plugily.projects.murdermystery.arena.Arena;
import plugily.projects.murdermystery.utils.HalloweenChat;

public class VisualTaskEffectManager {

  private static class ActiveVisualSession {
    final UUID playerId;
    final String playerName;
    final TaskType taskType;
    final Location stationLocation;
    TextDisplay statusDisplay;
    int tickCount = 0;
    int currentStep = 0;
    int totalSteps = 5;

    ActiveVisualSession(Player player, TaskType taskType, Location stationLocation) {
      this.playerId = player.getUniqueId();
      this.playerName = player.getName();
      this.taskType = taskType;
      this.stationLocation = stationLocation != null ? stationLocation.clone() : player.getLocation().getBlock().getLocation();
    }
  }

  private final Arena arena;
  private final Map<UUID, ActiveVisualSession> activeSessions = new HashMap<>();
  private final List<TextDisplay> completionDisplays = new ArrayList<>();
  private BukkitTask animationTask;

  public VisualTaskEffectManager(Arena arena) {
    this.arena = arena;
  }

  private boolean isSpectator(Player player) {
    if(player == null) {
      return true;
    }
    try {
      return arena.getPlugin().getUserManager().getUser(player).isSpectator();
    } catch(Throwable ignored) {
      return false;
    }
  }

  public void startVisualSession(Player player, TaskType taskType, Location stationLoc) {
    if(player == null || taskType == null || !taskType.isVisual() || !arena.isVisualTasksEnabled() || isSpectator(player)) {
      return;
    }
    stopVisualSession(player);

    ActiveVisualSession session = new ActiveVisualSession(player, taskType, stationLoc);
    Location holoLoc = player.getLocation().clone().add(0.0, 2.35, 0.0);
    String initialText = buildStatusText(session);
    session.statusDisplay = arena.spawnTextDisplay(holoLoc, initialText, true);

    activeSessions.put(player.getUniqueId(), session);
    ensureAnimationLoopRunning();
  }

  public void triggerStepEffect(Player player, TaskType taskType, int currentStep, int totalSteps) {
    if(player == null || taskType == null || !taskType.isVisual() || !arena.isVisualTasksEnabled() || isSpectator(player)) {
      return;
    }
    ActiveVisualSession session = activeSessions.get(player.getUniqueId());
    if(session != null) {
      session.currentStep = Math.max(0, currentStep);
      session.totalSteps = Math.max(1, totalSteps);
      if(session.statusDisplay != null && session.statusDisplay.isValid()) {
        session.statusDisplay.setText(HalloweenChat.color(buildStatusText(session)));
      }
    }

    Location origin = (session != null && session.stationLocation != null)
      ? session.stationLocation.clone().add(0.5, 1.0, 0.5)
      : player.getLocation().clone().add(0.0, 1.0, 0.0);
    World world = origin.getWorld();
    if(world == null) {
      return;
    }

    Particle dustParticle = resolveDustParticle();

    if(taskType == TaskType.CHECK_RADIATOR_WATER) {
      spawnRadiatorSteamBurst(origin, dustParticle);
    } else if(taskType == TaskType.CHECK_TIRE_PRESSURE) {
      spawnTirePumpWave(origin, dustParticle);
    }
  }

  public void completeVisualSession(Player player, TaskType taskType) {
    if(player == null || taskType == null) {
      return;
    }
    ActiveVisualSession session = activeSessions.remove(player.getUniqueId());
    Location baseLoc = (session != null && session.stationLocation != null)
      ? session.stationLocation.clone().add(0.5, 1.0, 0.5)
      : player.getLocation().clone().add(0.0, 1.0, 0.0);

    if(session != null) {
      cleanupSessionEntities(session);
    }

    if(!taskType.isVisual() || !arena.isVisualTasksEnabled() || isSpectator(player)) {
      return;
    }

    World world = player.getWorld();
    Particle dustParticle = resolveDustParticle();
    Location playerCenter = player.getLocation().clone().add(0.0, 1.0, 0.0);

    if(dustParticle != null) {
      try {
        Particle.DustOptions lime = new Particle.DustOptions(Color.fromRGB(110, 235, 131), 1.6F);
        Particle.DustOptions cyan = new Particle.DustOptions(Color.fromRGB(114, 239, 221), 1.4F);
        for(int angle = 0; angle < 360; angle += 15) {
          double rad = Math.toRadians(angle);
          double dx = Math.cos(rad) * 1.1;
          double dz = Math.sin(rad) * 1.1;
          world.spawnParticle(dustParticle, playerCenter.clone().add(dx, 0.2, dz), 1, 0, 0, 0, 0, lime);
          world.spawnParticle(dustParticle, playerCenter.clone().add(dx * 0.8, 0.9, dz * 0.8), 1, 0, 0, 0, 0, cyan);
        }
      } catch(Exception ignored) {
      }
    }

    spawnSafeParticle(world, "HAPPY_VILLAGER", "VILLAGER_HAPPY", playerCenter, 25, 0.5, 0.6, 0.5, 0.05);
    spawnSafeParticle(world, "END_ROD", "END_ROD", baseLoc, 18, 0.4, 0.5, 0.4, 0.04);
    XSound.BLOCK_BEACON_ACTIVATE.play(playerCenter, 0.9F, 1.6F);

    Location verifyLoc = player.getLocation().clone().add(0.0, 2.35, 0.0);
    String verifyText = "<#5A4E7C>[<#6EEB83>Visual Task<#5A4E7C>]\n\n"
      + "<#FFB703>" + player.getName() + " <#F4F1DE>ยืนยันตัวตนแล้ว\n\n"
      + "<#5A4E7C>(<#B8B0C8>ทำภารกิจ " + taskType.getDisplayName() + " สำเร็จ<#5A4E7C>)";
    TextDisplay verifyDisplay = arena.spawnTextDisplay(verifyLoc, verifyText, true);
    if(verifyDisplay != null) {
      completionDisplays.add(verifyDisplay);
      Bukkit.getScheduler().runTaskLater(arena.getPlugin(), () -> {
        if(verifyDisplay.isValid()) {
          verifyDisplay.remove();
        }
        completionDisplays.remove(verifyDisplay);
      }, 70L);
    }
  }

  public void stopVisualSession(Player player) {
    if(player == null) {
      return;
    }
    ActiveVisualSession session = activeSessions.remove(player.getUniqueId());
    if(session != null) {
      cleanupSessionEntities(session);
    }
  }

  private void cleanupSessionEntities(ActiveVisualSession session) {
    if(session.statusDisplay != null && session.statusDisplay.isValid()) {
      try {
        session.statusDisplay.remove();
      } catch(Throwable ignored) {
      }
      session.statusDisplay = null;
    }
  }

  private void ensureAnimationLoopRunning() {
    if(animationTask != null) {
      return;
    }
    animationTask = Bukkit.getScheduler().runTaskTimer(arena.getPlugin(), () -> {
      if(activeSessions.isEmpty()) {
        stopAnimationLoop();
        return;
      }
      Particle dustParticle = resolveDustParticle();
      Iterator<Map.Entry<UUID, ActiveVisualSession>> it = activeSessions.entrySet().iterator();
      while(it.hasNext()) {
        Map.Entry<UUID, ActiveVisualSession> entry = it.next();
        Player player = Bukkit.getPlayer(entry.getKey());
        ActiveVisualSession session = entry.getValue();
        if(player == null || !player.isOnline() || !arena.getPlayers().contains(player)) {
          cleanupSessionEntities(session);
          it.remove();
          continue;
        }

        session.tickCount++;
        if(session.statusDisplay != null && session.statusDisplay.isValid()) {
          try {
            session.statusDisplay.teleport(player.getLocation().clone().add(0.0, 2.35, 0.0));
          } catch(Throwable ignored) {
          }
        }

        renderContinuousEffect(player, session, dustParticle);
      }
    }, 2L, 3L);
  }

  private void renderContinuousEffect(Player player, ActiveVisualSession session, Particle dustParticle) {
    World world = player.getWorld();
    Location pLoc = player.getLocation();
    Location sLoc = session.stationLocation != null && session.stationLocation.getWorld() != null
      ? session.stationLocation.clone().add(0.5, 0.8, 0.5)
      : pLoc.clone().add(0.0, 0.8, 0.0);

    if(session.taskType == TaskType.CHECK_RADIATOR_WATER) {
      spawnSafeParticle(world, "CLOUD", "CLOUD", sLoc.clone().add(0, 0.4, 0), 2, 0.2, 0.25, 0.2, 0.01);
      spawnSafeParticle(world, "SPLASH", "WATER_SPLASH", sLoc.clone().add(0, 0.3, 0), 4, 0.25, 0.2, 0.25, 0.05);
    } else if(session.taskType == TaskType.CHECK_TIRE_PRESSURE) {
      if(dustParticle != null) {
        try {
          Particle.DustOptions amber = new Particle.DustOptions(Color.fromRGB(255, 183, 3), 1.1F);
          double angle = Math.toRadians((session.tickCount * 36) % 360);
          world.spawnParticle(dustParticle, pLoc.clone().add(Math.cos(angle) * 0.7, 0.2, Math.sin(angle) * 0.7), 1, 0, 0, 0, 0, amber);
        } catch(Exception ignored) {
        }
      }
    }
  }

  private void spawnRadiatorSteamBurst(Location origin, Particle dustParticle) {
    World world = origin.getWorld();
    if(world == null) {
      return;
    }
    spawnSafeParticle(world, "CLOUD", "CLOUD", origin.clone().add(0, 0.5, 0), 12, 0.3, 0.45, 0.3, 0.03);
    spawnSafeParticle(world, "SPLASH", "WATER_SPLASH", origin.clone().add(0, 0.4, 0), 15, 0.35, 0.35, 0.35, 0.08);
    if(dustParticle != null) {
      try {
        Particle.DustOptions coolantCyan = new Particle.DustOptions(Color.fromRGB(0, 180, 216), 1.35F);
        world.spawnParticle(dustParticle, origin.clone().add(0, 0.4, 0), 10, 0.3, 0.3, 0.3, 0, coolantCyan);
      } catch(Exception ignored) {
      }
    }
    XSound.BLOCK_FIRE_EXTINGUISH.play(origin, 0.75F, 1.4F);
  }

  private void spawnTirePumpWave(Location origin, Particle dustParticle) {
    World world = origin.getWorld();
    if(world == null) {
      return;
    }
    spawnSafeParticle(world, "CLOUD", "CLOUD", origin, 8, 0.35, 0.15, 0.35, 0.03);
    if(dustParticle != null) {
      try {
        Particle.DustOptions airWhite = new Particle.DustOptions(Color.fromRGB(244, 241, 222), 1.3F);
        for(int a = 0; a < 360; a += 30) {
          double rad = Math.toRadians(a);
          world.spawnParticle(dustParticle, origin.clone().add(Math.cos(rad) * 0.85, 0.15, Math.sin(rad) * 0.85), 1, 0, 0, 0, 0, airWhite);
        }
      } catch(Exception ignored) {
      }
    }
  }

  private String buildStatusText(ActiveVisualSession session) {
    int percent = session.totalSteps <= 0 ? 0 : Math.min(100, (int) Math.round(((double) session.currentStep / session.totalSteps) * 100.0));
    return "<#5A4E7C>[<#72EFDD>Visual Task<#5A4E7C>]\n\n"
      + "<#F4F1DE>" + session.taskType.getDisplayName() + "\n"
      + "<#FFB703>" + percent + "%\n\n"
      + "<#5A4E7C>(<#B8B0C8>กำลังยืนยันตัวตน<#5A4E7C>)";
  }

  private Particle resolveDustParticle() {
    try {
      return Particle.valueOf("DUST");
    } catch(Exception e) {
      try {
        return Particle.valueOf("REDSTONE");
      } catch(Exception ignored) {
        return null;
      }
    }
  }

  private void spawnSafeParticle(World world, String modernName, String legacyName, Location loc, int count, double dx, double dy, double dz, double extra) {
    if(world == null || loc == null) {
      return;
    }
    Particle p = null;
    try {
      p = Particle.valueOf(modernName);
    } catch(Exception e) {
      try {
        p = Particle.valueOf(legacyName);
      } catch(Exception ignored) {
      }
    }
    if(p != null) {
      try {
        world.spawnParticle(p, loc, count, dx, dy, dz, extra);
      } catch(Exception ignored) {
      }
    }
  }

  private void stopAnimationLoop() {
    if(animationTask != null) {
      animationTask.cancel();
      animationTask = null;
    }
  }

  public void cleanup() {
    stopAnimationLoop();
    for(ActiveVisualSession session : activeSessions.values()) {
      cleanupSessionEntities(session);
    }
    activeSessions.clear();
    for(TextDisplay td : completionDisplays) {
      if(td != null && td.isValid()) {
        try {
          td.remove();
        } catch(Throwable ignored) {
        }
      }
    }
    completionDisplays.clear();
  }
}
