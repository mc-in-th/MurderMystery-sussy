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

package plugily.projects.murdermystery.arena.cutscene;

import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;
import plugily.projects.minigamesbox.api.arena.IArenaState;
import plugily.projects.minigamesbox.api.user.IUser;
import plugily.projects.minigamesbox.classic.PluginMain;
import plugily.projects.minigamesbox.classic.utils.configuration.ConfigUtils;
import plugily.projects.minigamesbox.classic.utils.version.VersionUtils;
import plugily.projects.murdermystery.Main;
import plugily.projects.murdermystery.arena.Arena;
import plugily.projects.murdermystery.arena.ArenaUtils;
import plugily.projects.murdermystery.utils.HalloweenChat;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

public class CutsceneManager {

  public static final int FPS = 60;
  private static final long FRAME_INTERVAL_NANOS = 1_000_000_000L / FPS;
  private static final int MAX_RECORDING_FRAMES = FPS * 60;
  private static final double PLAYER_EYE_OFFSET = 1.62;
  private static final double OVERLAY_DISTANCE = 0.15;
  private static final int FADE_IN_FRAMES = 60;
  private static final int FADE_OUT_FRAMES = 45;
  private static final double AUTO_POST_STOP_HOLD_SECONDS = 2.4;
  private static final double AUTO_CUT_PROGRESS = 0.56;

  private static volatile ScheduledExecutorService executor;

  private static synchronized ScheduledExecutorService getExecutor() {
    if(executor == null || executor.isShutdown() || executor.isTerminated()) {
      executor = Executors.newScheduledThreadPool(2, r -> {
        Thread t = new Thread(r, "MurderMystery-Cutscene-60FPS");
        t.setDaemon(true);
        return t;
      });
    }
    return executor;
  }

  private static final Map<UUID, RecordingSession> activeRecordings = new ConcurrentHashMap<>();
  private static final Map<UUID, PreviewSession> activePreviews = new ConcurrentHashMap<>();
  private static final Set<UUID> internalBypass = ConcurrentHashMap.newKeySet();

  public static boolean isInternalBypass(Player player) {
    return player != null && internalBypass.contains(player.getUniqueId());
  }

  private static void runWithInternalBypass(Player player, Runnable action) {
    if(player == null || action == null) {
      return;
    }
    UUID uuid = player.getUniqueId();
    internalBypass.add(uuid);
    try {
      action.run();
    } finally {
      internalBypass.remove(uuid);
    }
  }

  private static boolean isCutsceneAllowedState(IArenaState state) {
    return state == IArenaState.IN_GAME || state == IArenaState.STARTING || state == IArenaState.FULL_GAME;
  }

  private static void teleportToCutsceneCamera(Player player, Location targetEyeLoc, TextDisplay rig) {
    if(player == null || !player.isOnline()) {
      return;
    }
    runWithInternalBypass(player, () -> {
      if(targetEyeLoc != null && targetEyeLoc.getWorld() != null) {
        targetEyeLoc.getChunk().load(true);
        if(player.getGameMode() == GameMode.SPECTATOR && player.getSpectatorTarget() != null) {
          player.setSpectatorTarget(null);
        }
        if(!player.getWorld().equals(targetEyeLoc.getWorld()) || player.getLocation().distanceSquared(targetEyeLoc) > 1.0) {
          player.teleport(targetEyeLoc, PlayerTeleportEvent.TeleportCause.PLUGIN);
        }
      }
      if(player.getGameMode() != GameMode.SPECTATOR) {
        player.setGameMode(GameMode.SPECTATOR);
      }
      player.setFlySpeed(0.0f);
      player.setWalkSpeed(0.0f);
      if(rig != null && rig.isValid() && player.getWorld().equals(rig.getWorld())) {
        player.setSpectatorTarget(rig);
        PacketCameraSender.sendSetCamera(rig, Collections.singletonList(player));
      }
    });
  }

  private final Arena arena;
  private final List<CutsceneFrame> frames = new ArrayList<>();
  private Location busStartLocation;
  private Location busEndLocation;
  private double busDriveDurationSeconds = 8.0;
  private String busSchematicName = BusSchematicLoader.DEFAULT_SCHEMATIC_NAME;
  private Location busHidePos1;
  private Location busHidePos2;
  private volatile List<Player> activeViewers = Collections.emptyList();
  private final AtomicLong sessionCounter = new AtomicLong(0);
  private ScheduledFuture<?> playbackFuture;
  private BukkitTask syncTask;
  private volatile List<CameraShotRig> shotRigs = Collections.emptyList();
  private TextDisplay fadeOverlay;
  private volatile BusCutsceneRig activeBusRig;
  private volatile boolean playing = false;

  public CutsceneManager(Arena arena) {
    this.arena = arena;
  }

  public boolean hasBusCutscene() {
    return busStartLocation != null
      && busEndLocation != null
      && busStartLocation.getWorld() != null
      && (busEndLocation.getWorld() == null || busStartLocation.getWorld().equals(busEndLocation.getWorld()));
  }

  public boolean hasCutscene() {
    return !frames.isEmpty() || hasBusCutscene();
  }

  public Location getBusStartLocation() {
    return busStartLocation;
  }

  public void setBusStartLocation(Location busStartLocation) {
    this.busStartLocation = busStartLocation;
  }

  public Location getBusEndLocation() {
    return busEndLocation;
  }

  public void setBusEndLocation(Location busEndLocation) {
    this.busEndLocation = busEndLocation;
  }

  public double getBusDriveDurationSeconds() {
    return busDriveDurationSeconds;
  }

  public void setBusDriveDurationSeconds(double busDriveDurationSeconds) {
    this.busDriveDurationSeconds = Math.max(1.5, Math.min(60.0, busDriveDurationSeconds));
  }

  public String getBusSchematicName() {
    return busSchematicName;
  }

  public void setBusSchematicName(String busSchematicName) {
    this.busSchematicName = BusSchematicLoader.sanitizeFileName(busSchematicName);
  }

  public Location getBusHidePos1() {
    return busHidePos1;
  }

  public void setBusHidePos1(Location busHidePos1) {
    this.busHidePos1 = busHidePos1;
  }

  public Location getBusHidePos2() {
    return busHidePos2;
  }

  public void setBusHidePos2(Location busHidePos2) {
    this.busHidePos2 = busHidePos2;
  }

  public void clearBusCutscene() {
    this.busStartLocation = null;
    this.busEndLocation = null;
    this.busDriveDurationSeconds = 8.0;
    this.busSchematicName = BusSchematicLoader.DEFAULT_SCHEMATIC_NAME;
    this.busHidePos1 = null;
    this.busHidePos2 = null;
  }

  public boolean isPlaying() {
    return playing;
  }

  public boolean isActiveViewer(Player player) {
    return playing && player != null && activeViewers.contains(player);
  }

  public List<CutsceneFrame> getEffectiveFrames() {
    if(!frames.isEmpty()) {
      return new ArrayList<>(frames);
    }
    if(hasBusCutscene()) {
      return generateAutoBusCameraFrames();
    }
    return new ArrayList<>();
  }

  private List<CutsceneFrame> generateAutoBusCameraFrames() {
    List<CutsceneFrame> generated = new ArrayList<>();
    if(!hasBusCutscene()) {
      return generated;
    }
    World world = busStartLocation.getWorld();
    String worldName = world != null ? world.getName() : "world";
    double sx = busStartLocation.getX();
    double sy = busStartLocation.getY();
    double sz = busStartLocation.getZ();
    double ex = busEndLocation.getX();
    double ey = busEndLocation.getY();
    double ez = busEndLocation.getZ();
    double dx = ex - sx;
    double dy = ey - sy;
    double dz = ez - sz;
    double horizLen = Math.sqrt(dx * dx + dz * dz);
    double fx = horizLen > 1e-4 ? (dx / horizLen) : 1.0;
    double fz = horizLen > 1e-4 ? (dz / horizLen) : 0.0;
    double rx = -fz;
    double rz = fx;

    double driveSec = Math.max(1.5, busDriveDurationSeconds);
    double totalSec = driveSec + AUTO_POST_STOP_HOLD_SECONDS;
    int totalFrames = Math.max(FPS * 4, (int) Math.round(totalSec * FPS));
    int cutFrameIdx = Math.max(
      FADE_IN_FRAMES + 20,
      Math.min(totalFrames - FADE_OUT_FRAMES - 30, (int) Math.round(driveSec * AUTO_CUT_PROGRESS * FPS))
    );

    double cruiseFraction = 0.65;
    double cruiseSpeed = 2.0 / (1.0 + cruiseFraction);
    double cutRoadFraction = Math.min(1.0, cruiseSpeed * AUTO_CUT_PROGRESS);

    double clearRight = 9.6;
    double clearLeft = 9.6;
    if(world != null && Bukkit.isPrimaryThread()) {
      clearRight = measureSideClearance(world, sx, sy, sz, dx, dy, dz, rx, rz, 1.0, cutRoadFraction);
      clearLeft = measureSideClearance(world, sx, sy, sz, dx, dy, dz, rx, rz, -1.0, cutRoadFraction);
    }
    double sideSign = (clearLeft > clearRight + 1.0) ? -1.0 : 1.0;
    double chosenSideClear = sideSign > 0.0 ? clearRight : clearLeft;
    double sideStartDist = Math.max(5.8, Math.min(8.8, chosenSideClear - 0.8));
    double sideEndDist = Math.max(5.4, sideStartDist - 0.9);

    double frontClear = 17.2;
    if(world != null && Bukkit.isPrimaryThread()) {
      frontClear = measureFrontClearance(world, ex, ey, ez, fx, fz, rx, rz, sideSign * 1.9);
    }
    double frontStartDist = Math.max(11.8, Math.min(16.2, frontClear - 0.8));
    double frontEndDist = Math.max(11.4, Math.min(13.8, frontStartDist - 2.2));
    double blastStartSec = driveSec * 0.96;

    for(int i = 0; i < totalFrames; i++) {
      double elapsed = (double) i / FPS;
      double u = Math.max(0.0, Math.min(1.0, elapsed / driveSec));
      double s;
      if(u <= cruiseFraction) {
        s = cruiseSpeed * u;
      } else {
        double span = 1.0 - cruiseFraction;
        double w = (u - cruiseFraction) / span;
        s = (cruiseSpeed * cruiseFraction) + cruiseSpeed * span * (w - 0.5 * w * w);
      }

      double bx = sx + dx * s;
      double by = sy + dy * s;
      double bz = sz + dz * s;

      double camX;
      double camY;
      double camZ;
      double targetX;
      double targetY;
      double targetZ;
      float shakeYaw = 0.0f;
      float shakePitch = 0.0f;

      if(i < cutFrameIdx) {
        double p1 = (double) i / Math.max(1, cutFrameIdx - 1);
        double sp1 = p1 * p1 * (3.0 - 2.0 * p1);
        double forwardOffset = 3.2 - 4.8 * sp1;
        double lateralOffset = sideSign * (sideStartDist + (sideEndDist - sideStartDist) * sp1);
        double eyeY = by + 2.65 - 0.30 * sp1;

        camX = bx + fx * forwardOffset + rx * lateralOffset;
        camY = eyeY - PLAYER_EYE_OFFSET;
        camZ = bz + fz * forwardOffset + rz * lateralOffset;

        double lookForward = 1.4 - 0.6 * sp1;
        targetX = bx + fx * lookForward;
        targetY = by + 3.15;
        targetZ = bz + fz * lookForward;
      } else {
        double p2 = (double) (i - cutFrameIdx) / Math.max(1, totalFrames - 1 - cutFrameIdx);
        double sp2 = p2 * p2 * (3.0 - 2.0 * p2);
        double frontDist = frontStartDist + (frontEndDist - frontStartDist) * sp2;
        double lateralOffset = sideSign * (2.2 - 0.8 * sp2);
        double eyeY = ey + 1.68 - 0.22 * sp2;

        double blastDt = elapsed - blastStartSec;
        if(blastDt >= 0.0 && blastDt <= 0.50) {
          double decay = 1.0 - (blastDt / 0.50);
          decay = decay * decay;
          eyeY += Math.sin(blastDt * 58.0) * 0.065 * decay;
          shakePitch = (float) (Math.cos(blastDt * 64.0) * 1.35 * decay);
          shakeYaw = (float) (Math.sin(blastDt * 47.0) * 0.75 * decay);
        }

        camX = ex + fx * frontDist + rx * lateralOffset;
        camY = eyeY - PLAYER_EYE_OFFSET;
        camZ = ez + fz * frontDist + rz * lateralOffset;

        double settle = u >= 1.0 ? Math.min(1.0, (elapsed - driveSec) / Math.max(0.4, AUTO_POST_STOP_HOLD_SECONDS * 0.7)) : 0.0;
        double smoothSettle = settle * settle * (3.0 - 2.0 * settle);
        double lookNoseOffset = 6.5 + 0.8 * smoothSettle;
        targetX = bx + fx * lookNoseOffset;
        targetY = by + 2.85 - 0.40 * smoothSettle;
        targetZ = bz + fz * lookNoseOffset;
      }

      double lookDx = targetX - camX;
      double lookDy = targetY - (camY + PLAYER_EYE_OFFSET);
      double lookDz = targetZ - camZ;
      double lookHoriz = Math.sqrt(lookDx * lookDx + lookDz * lookDz);
      float yaw = (float) Math.toDegrees(Math.atan2(-lookDx, lookDz)) + shakeYaw;
      float pitch = (float) Math.toDegrees(-Math.atan2(lookDy, Math.max(1e-4, lookHoriz))) + shakePitch;

      generated.add(new CutsceneFrame(worldName, camX, camY, camZ, yaw, pitch));
    }
    return generated;
  }

  private static double measureSideClearance(
    World world,
    double sx,
    double sy,
    double sz,
    double dx,
    double dy,
    double dz,
    double rx,
    double rz,
    double sign,
    double maxRoadFraction
  ) {
    double minClear = 9.6;
    int samples = 8;
    for(int sIdx = 0; sIdx <= samples; sIdx++) {
      double frac = maxRoadFraction * ((double) sIdx / (double) samples);
      double bx = sx + dx * frac;
      double by = sy + dy * frac;
      double bz = sz + dz * frac;
      for(double dist = 4.2; dist <= 9.5; dist += 0.5) {
        double tx = bx + rx * sign * dist;
        double tz = bz + rz * sign * dist;
        if(isOccludedAt(world, tx, by + 2.4, tz) || isOccludedAt(world, tx, by + 1.5, tz)) {
          if(dist < minClear) {
            minClear = dist;
          }
          break;
        }
      }
    }
    return minClear;
  }

  private static double measureFrontClearance(
    World world,
    double ex,
    double ey,
    double ez,
    double fx,
    double fz,
    double rx,
    double rz,
    double lateralOffset
  ) {
    for(double dist = 10.2; dist <= 17.0; dist += 0.5) {
      double tx = ex + fx * dist + rx * lateralOffset;
      double tz = ez + fz * dist + rz * lateralOffset;
      if(isOccludedAt(world, tx, ey + 1.6, tz) || isOccludedAt(world, tx, ey + 2.4, tz)) {
        return dist;
      }
    }
    return 17.2;
  }

  private static boolean isOccludedAt(World world, double x, double y, double z) {
    int bx = (int) Math.floor(x);
    int by = (int) Math.floor(y);
    int bz = (int) Math.floor(z);
    if(!world.isChunkLoaded(bx >> 4, bz >> 4)) {
      return false;
    }
    try {
      Block b = world.getBlockAt(bx, by, bz);
      return b.getType().isOccluding();
    } catch(Throwable ignored) {
      return false;
    }
  }

  public Location getInitialEyeLocation() {
    List<CutsceneFrame> effective = getEffectiveFrames();
    if(effective.isEmpty()) {
      return null;
    }
    return effective.get(0).toEyeLocation(getFallbackWorld());
  }

  public List<CutsceneFrame> getFrames() {
    return frames;
  }

  public void setFrames(List<CutsceneFrame> newFrames) {
    frames.clear();
    if(newFrames != null) {
      frames.addAll(newFrames);
    }
  }

  public void clearFrames() {
    frames.clear();
  }

  public double getDurationSeconds() {
    if(!frames.isEmpty()) {
      return (double) frames.size() / FPS;
    }
    if(hasBusCutscene()) {
      return Math.max(1.5, busDriveDurationSeconds) + AUTO_POST_STOP_HOLD_SECONDS;
    }
    return 0.0;
  }

  public boolean isUsingAutoCamera() {
    return frames.isEmpty() && hasBusCutscene();
  }

  public void startCutscene(Runnable onComplete) {
    cleanup();
    List<CutsceneFrame> snapshotFrames = getEffectiveFrames();
    if(snapshotFrames.isEmpty()) {
      if(onComplete != null) {
        onComplete.run();
      }
      return;
    }

    final long currentSession = sessionCounter.incrementAndGet();
    playing = true;
    arena.getScoreboardManager().stopAllScoreboards();
    Main plugin = arena.getPlugin();
    World fallbackWorld = getFallbackWorld();
    final boolean customCamera = !frames.isEmpty();

    List<Player> participants = new ArrayList<>(arena.getPlayers());
    List<Player> initialViewers = new ArrayList<>(participants.size());
    for(Player participant : participants) {
      if(participant != null && participant.isOnline()) {
        initialViewers.add(participant);
      }
    }
    activeViewers = Collections.unmodifiableList(initialViewers);
    CutsceneFrame firstFrame = snapshotFrames.get(0);
    Location initialEyeLoc = firstFrame.toEyeLocation(fallbackWorld);
    Location initialOverlayLoc = firstFrame.toOverlayLocation(fallbackWorld);
    if(initialEyeLoc != null && initialEyeLoc.getWorld() != null) {
      initialEyeLoc.getChunk().load(true);
    }

    for(Player player : initialViewers) {
      preparePlayerForCutscene(player, arena.getPlayers(), initialEyeLoc, null);
    }

    List<CameraShotRig> spawnedShots = spawnShotRigs(snapshotFrames, fallbackWorld);
    shotRigs = spawnedShots;
    TextDisplay initialRig = resolveActiveRig(spawnedShots, 0);
    fadeOverlay = spawnFadeOverlay(initialOverlayLoc);
    if(hasBusCutscene()) {
      activeBusRig = BusCutsceneRig.spawn(
        plugin,
        arena,
        busStartLocation,
        busEndLocation,
        busSchematicName,
        busHidePos1,
        busHidePos2,
        initialViewers
      );
    }

    for(Player player : initialViewers) {
      if(!player.isOnline()) {
        continue;
      }
      if(initialRig != null && initialRig.isValid() && player.getWorld().equals(initialRig.getWorld())) {
        runWithInternalBypass(player, () -> player.setSpectatorTarget(initialRig));
      }
      HalloweenChat.sendTitles(
        player,
        "<shadow:#000000FF><#5A4E7C>[</shadow><#FF7518>" + arena.getMapName() + "<shadow:#000000FF><#5A4E7C>]</shadow>",
        "<#B8B0C8>ระวังคนร้ายที่แฝงตัวอยู่ในกลุ่มของคุณ",
        15,
        45,
        15
      );
      player.playSound(player.getLocation(), Sound.BLOCK_PORTAL_TRIGGER, 0.5f, 1.2f);
    }
    if(initialRig != null && initialRig.isValid()) {
      PacketCameraSender.sendSetCamera(initialRig, activeViewers);
    }

    final long startNanos = System.nanoTime();
    final int totalFrames = snapshotFrames.size();
    final double totalCutsceneSec = (double) totalFrames / FPS;
    final double effectiveDriveSec = BusCutsceneRig.computeEffectiveDriveSeconds(
      busDriveDurationSeconds,
      totalCutsceneSec,
      customCamera
    );
    final AtomicReference<TextDisplay> lastSentHighFreqRig = new AtomicReference<>(initialRig);

    syncTask = new BukkitRunnable() {
      private int tickCount = 0;
      private TextDisplay lastSyncedRig = initialRig;

      @Override
      public void run() {
        if(!playing || sessionCounter.get() != currentSession || !isCutsceneAllowedState(arena.getArenaState())) {
          cancel();
          return;
        }
        int frameIdx = (int) ((System.nanoTime() - startNanos) / FRAME_INTERVAL_NANOS);
        if(frameIdx >= totalFrames) {
          finishCutscene(currentSession, onComplete);
          cancel();
          return;
        }
        tickCount++;
        int clampedIdx = Math.min(frameIdx, totalFrames - 1);
        CutsceneFrame currentFrame = snapshotFrames.get(clampedIdx);
        Location eyeLoc = currentFrame.toEyeLocation(fallbackWorld);
        TextDisplay activeRig = resolveActiveRig(spawnedShots, clampedIdx);
        boolean shotSwitched = activeRig != null && activeRig != lastSyncedRig;
        if(shotSwitched) {
          lastSyncedRig = activeRig;
        }
        if(eyeLoc != null && eyeLoc.getWorld() != null) {
          if(activeRig != null && activeRig.isValid()) {
            activeRig.teleport(eyeLoc);
          }
          updateFadeOverlayTick(fadeOverlay, currentFrame, fallbackWorld, frameIdx, totalFrames);
          int remainingSec = Math.max(1, (int) Math.ceil((double) (totalFrames - frameIdx) / FPS));
          Collection<Player> currentArenaPlayers = arena.getPlayers();
          List<Player> updatedViewers = new ArrayList<>(currentArenaPlayers.size());
          boolean shouldReconfirmCamera = shotSwitched || tickCount <= 6 || tickCount == 10 || tickCount == 15 || tickCount % 10 == 0;
          for(Player player : currentArenaPlayers) {
            if(player == null || !player.isOnline()) {
              continue;
            }
            updatedViewers.add(player);
            if(activeRig != null && activeRig.isValid()) {
              if(!player.getWorld().equals(activeRig.getWorld()) || player.getLocation().distanceSquared(eyeLoc) > 2304.0) {
                teleportToCutsceneCamera(player, eyeLoc, activeRig);
              } else {
                runWithInternalBypass(player, () -> {
                  if(player.getGameMode() != GameMode.SPECTATOR) {
                    player.setGameMode(GameMode.SPECTATOR);
                  }
                  player.setFlySpeed(0.0f);
                  if(player.getSpectatorTarget() != activeRig) {
                    player.setSpectatorTarget(activeRig);
                  } else if(!PacketCameraSender.isSetCameraSupported() && (tickCount == 2 || tickCount == 6)) {
                    player.setSpectatorTarget(null);
                    player.setSpectatorTarget(activeRig);
                  }
                });
              }
            } else {
              Location fallbackLoc = currentFrame.toLocation(fallbackWorld);
              if(fallbackLoc != null) {
                runWithInternalBypass(player, () -> {
                  if(player.getGameMode() != GameMode.SPECTATOR) {
                    player.setGameMode(GameMode.SPECTATOR);
                  }
                  player.setFlySpeed(0.0f);
                  player.teleport(fallbackLoc, PlayerTeleportEvent.TeleportCause.PLUGIN);
                });
              }
            }
            HalloweenChat.sendActionBar(
              player,
              HalloweenChat.color("<#B8B0C8>เกมกำลังจะเริ่มใน <#FFB703>" + remainingSec + "s")
            );
          }
          activeViewers = Collections.unmodifiableList(updatedViewers);
          if(activeBusRig != null) {
            double elapsedSec = (double) (System.nanoTime() - startNanos) / 1_000_000_000.0;
            activeBusRig.tickMainThread(elapsedSec, effectiveDriveSec, updatedViewers, PacketCameraSender::updateServerEntityPosition);
          }
          if(shouldReconfirmCamera && activeRig != null && activeRig.isValid()) {
            PacketCameraSender.sendSetCamera(activeRig, updatedViewers);
          }
        }
      }
    }.runTaskTimer(plugin, 0L, 1L);

    playbackFuture = getExecutor().scheduleAtFixedRate(() -> {
      if(!playing || sessionCounter.get() != currentSession) {
        return;
      }
      long elapsedNanos = System.nanoTime() - startNanos;
      int frameIdx = (int) (elapsedNanos / FRAME_INTERVAL_NANOS);
      if(frameIdx >= totalFrames) {
        ScheduledFuture<?> currentFuture = playbackFuture;
        if(currentFuture != null) {
          currentFuture.cancel(false);
        }
        Bukkit.getScheduler().runTask(plugin, () -> finishCutscene(currentSession, onComplete));
        return;
      }
      CutsceneFrame frame = snapshotFrames.get(frameIdx);
      List<Player> viewersSnapshot = activeViewers;
      TextDisplay rig = resolveActiveRig(spawnedShots, frameIdx);
      if(rig != null) {
        if(lastSentHighFreqRig.getAndSet(rig) != rig) {
          PacketCameraSender.sendSetCamera(rig, viewersSnapshot);
        }
        PacketCameraSender.sendEntityPosition60Fps(rig, frame, viewersSnapshot);
      }
      TextDisplay overlay = fadeOverlay;
      if(overlay != null && computeFadeAlpha(frameIdx, totalFrames) > 0) {
        PacketCameraSender.sendOverlayPosition60Fps(overlay, frame, viewersSnapshot);
      }
      BusCutsceneRig busRig = activeBusRig;
      if(busRig != null && busRig.getAnchor() != null) {
        double elapsedSec = (double) elapsedNanos / 1_000_000_000.0;
        BusCutsceneRig.BusPose pose = busRig.computePose(elapsedSec, effectiveDriveSec);
        PacketCameraSender.sendEntityPositionRaw60Fps(busRig.getAnchor(), pose.x, pose.y, pose.z, 0.0f, 0.0f, viewersSnapshot);
      }
    }, 0L, FRAME_INTERVAL_NANOS, TimeUnit.NANOSECONDS);
  }

  private void finishCutscene(long expectedSession, Runnable onComplete) {
    if(sessionCounter.get() != expectedSession) {
      return;
    }
    sessionCounter.incrementAndGet();
    if(playbackFuture != null) {
      playbackFuture.cancel(false);
      playbackFuture = null;
    }
    if(syncTask != null) {
      syncTask.cancel();
      syncTask = null;
    }
    boolean wasPlaying = playing;
    playing = false;
    activeViewers = Collections.emptyList();
    if(!wasPlaying) {
      removeCameraRig();
      return;
    }

    for(Player player : arena.getPlayers()) {
      if(player != null && player.isOnline()) {
        detachPlayerCameraOnly(player);
      }
    }
    removeCameraRig();

    if(onComplete != null && isCutsceneAllowedState(arena.getArenaState())) {
      onComplete.run();
    }

    for(Player player : arena.getPlayers()) {
      if(player != null && player.isOnline()) {
        restorePlayerVisibilityAfterCutscene(player);
        showHornHologramToPlayer(arena.getPlugin(), arena, player);
        if(!isArenaSpectator(player)) {
          applyPostCutsceneFadeIn(player);
        }
      }
    }
    restoreScoreboards();
  }

  public void handlePlayerLeave(Player player) {
    if(player == null) {
      return;
    }
    List<Player> current = activeViewers;
    if(current.contains(player)) {
      List<Player> copy = new ArrayList<>(current);
      copy.remove(player);
      activeViewers = Collections.unmodifiableList(copy);
    }
    BusCutsceneRig busRig = activeBusRig;
    if(busRig != null) {
      busRig.restoreViewer(player);
    }
    showHornHologramToPlayer(arena.getPlugin(), arena, player);
    if(!playing) {
      return;
    }
    runWithInternalBypass(player, () -> {
      if(player.getGameMode() == GameMode.SPECTATOR && player.getSpectatorTarget() != null) {
        player.setSpectatorTarget(null);
      }
      player.setFlySpeed(0.1f);
      player.setWalkSpeed(0.2f);
    });
  }

  public void cleanup() {
    sessionCounter.incrementAndGet();
    if(playbackFuture != null) {
      playbackFuture.cancel(false);
      playbackFuture = null;
    }
    if(syncTask != null) {
      syncTask.cancel();
      syncTask = null;
    }
    activeViewers = Collections.emptyList();
    if(playing) {
      playing = false;
      for(Player player : arena.getPlayers()) {
        if(player != null && player.isOnline()) {
          restorePlayerFromCutscene(player);
        }
      }
      restoreScoreboards();
    }
    removeCameraRig();
  }

  private void restoreScoreboards() {
    if(arena.getArenaState() == IArenaState.RESTARTING) {
      return;
    }
    for(Player player : arena.getPlayers()) {
      if(player != null && player.isOnline() && !isPreviewing(player)) {
        IUser user = arena.getPlugin().getUserManager().getUser(player);
        if(user != null) {
          arena.getScoreboardManager().createScoreboard(user);
        }
      }
    }
  }

  private static int computeFadeAlpha(int frameIdx, int totalFrames) {
    int fadeInAlpha = 0;
    if(frameIdx < FADE_IN_FRAMES) {
      double progress = Math.max(0.0, Math.min(1.0, (double) frameIdx / FADE_IN_FRAMES));
      fadeInAlpha = (int) Math.round(255.0 * (1.0 - progress));
    }
    int fadeOutAlpha = 0;
    int framesRemaining = totalFrames - frameIdx;
    if(framesRemaining <= FADE_OUT_FRAMES) {
      double progress = Math.max(0.0, Math.min(1.0, 1.0 - ((double) framesRemaining / FADE_OUT_FRAMES)));
      fadeOutAlpha = (int) Math.round(255.0 * progress);
    }
    return Math.max(fadeInAlpha, fadeOutAlpha);
  }

  private static void updateFadeOverlayTick(TextDisplay overlay, CutsceneFrame frame, World fallbackWorld, int frameIdx, int totalFrames) {
    if(overlay == null || !overlay.isValid()) {
      return;
    }
    int alpha = computeFadeAlpha(frameIdx, totalFrames);
    if(alpha <= 0) {
      overlay.setBackgroundColor(Color.fromARGB(0, 0, 0, 0));
      overlay.setViewRange(0.0f);
    } else {
      Location overlayLoc = frame.toOverlayLocation(fallbackWorld);
      if(overlayLoc != null) {
        overlay.teleport(overlayLoc);
      }
      overlay.setViewRange(1.0f);
      overlay.setBackgroundColor(Color.fromARGB(Math.min(255, alpha), 0, 0, 0));
    }
  }

  private static void applyPostCutsceneFadeIn(Player player) {
    if(player == null || !player.isOnline()) {
      return;
    }
    try {
      player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 25, 0, false, false, false));
      player.addPotionEffect(new PotionEffect(PotionEffectType.DARKNESS, 32, 0, false, false, false));
    } catch(Throwable ignored) {
    }
  }

  private static TextDisplay spawnCameraRig(Location eyeLoc) {
    if(eyeLoc == null || eyeLoc.getWorld() == null) {
      return null;
    }
    try {
      TextDisplay display = (TextDisplay) eyeLoc.getWorld().spawnEntity(eyeLoc, EntityType.TEXT_DISPLAY);
      display.setGravity(false);
      display.setPersistent(false);
      display.setTeleportDuration(1);
      display.setInterpolationDuration(1);
      display.setViewRange(0.0f);
      return display;
    } catch(Throwable t) {
      return null;
    }
  }

  private static TextDisplay spawnFadeOverlay(Location overlayLoc) {
    if(overlayLoc == null || overlayLoc.getWorld() == null) {
      return null;
    }
    try {
      return overlayLoc.getWorld().spawn(overlayLoc, TextDisplay.class, entity -> {
        entity.setText("   \n   \n   ");
        entity.setBillboard(Display.Billboard.CENTER);
        entity.setShadowed(false);
        entity.setDefaultBackground(false);
        entity.setBackgroundColor(Color.fromARGB(255, 0, 0, 0));
        entity.setSeeThrough(false);
        entity.setGravity(false);
        entity.setPersistent(false);
        entity.setTeleportDuration(1);
        entity.setInterpolationDuration(1);
        entity.setBrightness(new Display.Brightness(0, 0));
        try {
          Transformation transformation = new Transformation(
            new Vector3f(0.0f, -22.5f, 0.0f),
            new AxisAngle4f(),
            new Vector3f(60.0f, 60.0f, 1.0f),
            new AxisAngle4f()
          );
          entity.setTransformation(transformation);
        } catch(Throwable ignored) {
        }
      });
    } catch(Throwable t) {
      return null;
    }
  }

  private static void hideHornHologramFromPlayer(PluginMain plugin, Arena arena, Player player) {
    if(plugin == null || arena == null || player == null || !player.isOnline()) {
      return;
    }
    TextDisplay hornDisplay = arena.getCampfireDisplay();
    if(hornDisplay != null && hornDisplay.isValid()) {
      try {
        player.hideEntity(plugin, hornDisplay);
      } catch(Throwable ignored) {
      }
    }
  }

  private static void showHornHologramToPlayer(PluginMain plugin, Arena arena, Player player) {
    if(plugin == null || arena == null || player == null || !player.isOnline()) {
      return;
    }
    TextDisplay hornDisplay = arena.getCampfireDisplay();
    if(hornDisplay != null && hornDisplay.isValid()) {
      try {
        player.showEntity(plugin, hornDisplay);
      } catch(Throwable ignored) {
      }
    }
  }

  private void removeCameraRig() {
    if(activeBusRig != null) {
      try {
        activeBusRig.cleanup(arena.getPlayers());
      } catch(Throwable ignored) {
      }
      activeBusRig = null;
    }
    destroyShotRigs(shotRigs);
    shotRigs = Collections.emptyList();
    if(fadeOverlay != null) {
      try {
        if(fadeOverlay.isValid()) {
          fadeOverlay.remove();
        }
      } catch(Throwable ignored) {
      }
      fadeOverlay = null;
    }
  }

  private static List<CameraShotRig> spawnShotRigs(List<CutsceneFrame> frames, World fallbackWorld) {
    List<CameraShotRig> list = new ArrayList<>(4);
    if(frames == null || frames.isEmpty()) {
      return list;
    }
    for(int i = 0; i < frames.size(); i++) {
      if(i == 0 || isHardCutBetween(frames.get(i - 1), frames.get(i))) {
        Location shotEyeLoc = frames.get(i).toEyeLocation(fallbackWorld);
        if(shotEyeLoc != null && shotEyeLoc.getWorld() != null) {
          shotEyeLoc.getChunk().load(true);
          TextDisplay rig = spawnCameraRig(shotEyeLoc);
          if(rig != null) {
            list.add(new CameraShotRig(i, rig));
          }
        }
      }
    }
    return list;
  }

  private static boolean isHardCutBetween(CutsceneFrame a, CutsceneFrame b) {
    if(a == null || b == null) {
      return false;
    }
    if(a.worldName != null && b.worldName != null && !a.worldName.equals(b.worldName)) {
      return true;
    }
    double dx = a.x - b.x;
    double dy = a.y - b.y;
    double dz = a.z - b.z;
    if(dx * dx + dy * dy + dz * dz > 4.0) {
      return true;
    }
    float dYaw = Math.abs(CutsceneFrame.wrapDegrees(a.yaw - b.yaw));
    return dYaw > 55.0f;
  }

  private static TextDisplay resolveActiveRig(List<CameraShotRig> rigs, int frameIdx) {
    if(rigs == null || rigs.isEmpty()) {
      return null;
    }
    for(int i = rigs.size() - 1; i >= 0; i--) {
      CameraShotRig shot = rigs.get(i);
      if(frameIdx >= shot.startFrame && shot.rig != null && shot.rig.isValid()) {
        return shot.rig;
      }
    }
    CameraShotRig first = rigs.get(0);
    return (first.rig != null && first.rig.isValid()) ? first.rig : null;
  }

  private static void destroyShotRigs(List<CameraShotRig> rigs) {
    if(rigs == null || rigs.isEmpty()) {
      return;
    }
    for(CameraShotRig shot : rigs) {
      if(shot != null && shot.rig != null) {
        try {
          if(shot.rig.isValid()) {
            shot.rig.remove();
          }
        } catch(Throwable ignored) {
        }
      }
    }
  }

  private void preparePlayerForCutscene(Player player, Collection<Player> allPlayers, Location startEyeLoc, TextDisplay rig) {
    Main plugin = arena.getPlugin();
    for(Player other : allPlayers) {
      if(other != null && !other.equals(player)) {
        VersionUtils.hidePlayer(plugin, player, other);
      }
    }
    hideHornHologramFromPlayer(plugin, arena, player);
    teleportToCutsceneCamera(player, startEyeLoc, rig);
  }

  private boolean isArenaSpectator(Player player) {
    if(player == null) {
      return false;
    }
    if(arena.isSpectatorPlayer(player) || arena.isDeathPlayer(player)) {
      return true;
    }
    IUser user = arena.getPlugin().getUserManager().getUser(player);
    return user != null && (user.isSpectator() || user.isPermanentSpectator());
  }

  private void detachPlayerCameraOnly(Player player) {
    runWithInternalBypass(player, () -> {
      if(player.getGameMode() == GameMode.SPECTATOR && player.getSpectatorTarget() != null) {
        player.setSpectatorTarget(null);
      }
      player.setFlySpeed(0.1f);
      player.setWalkSpeed(0.2f);
      if(isArenaSpectator(player)) {
        ArenaUtils.applySpectatorState(player, arena, true);
        return;
      }
      player.setGameMode(GameMode.ADVENTURE);
      player.setFlying(false);
      player.setAllowFlight(false);
    });
  }

  private void restorePlayerVisibilityAfterCutscene(Player player) {
    if(isArenaSpectator(player)) {
      return;
    }
    Main plugin = arena.getPlugin();
    for(Player other : arena.getPlayers()) {
      if(other != null && !other.equals(player)) {
        if(isArenaSpectator(other)) {
          VersionUtils.hidePlayer(plugin, player, other);
        } else {
          VersionUtils.showPlayer(plugin, player, other);
        }
      }
    }
  }

  private void restorePlayerFromCutscene(Player player) {
    detachPlayerCameraOnly(player);
    restorePlayerVisibilityAfterCutscene(player);
    showHornHologramToPlayer(arena.getPlugin(), arena, player);
  }

  private World getFallbackWorld() {
    if(busStartLocation != null && busStartLocation.getWorld() != null) {
      return busStartLocation.getWorld();
    }
    if(!arena.getPlayerSpawnPoints().isEmpty() && arena.getPlayerSpawnPoints().get(0) != null) {
      return arena.getPlayerSpawnPoints().get(0).getWorld();
    }
    if(arena.getStartLocation() != null) {
      return arena.getStartLocation().getWorld();
    }
    if(!Bukkit.getWorlds().isEmpty()) {
      return Bukkit.getWorlds().get(0);
    }
    return null;
  }

  public static boolean isRecording(Player player) {
    return player != null && activeRecordings.containsKey(player.getUniqueId());
  }

  public static RecordingSession getRecordingSession(Player player) {
    return player == null ? null : activeRecordings.get(player.getUniqueId());
  }

  public static boolean isPreviewing(Player player) {
    return player != null && activePreviews.containsKey(player.getUniqueId());
  }

  public static void updateRecordedPlayerPosition(Player player, Location to) {
    if(player == null || to == null) {
      return;
    }
    RecordingSession session = activeRecordings.get(player.getUniqueId());
    if(session != null) {
      session.latestFrame = CutsceneFrame.fromLocation(to);
    }
  }

  public static void startRecording(PluginMain plugin, Player player, Arena arena) {
    stopRecording(plugin, player, false);
    stopPreview(player);

    List<CutsceneFrame> recorded = Collections.synchronizedList(new ArrayList<>());
    CutsceneFrame initialFrame = CutsceneFrame.fromLocation(player.getLocation());
    RecordingSession session = new RecordingSession(arena, recorded, initialFrame);
    final List<Player> recorderList = Collections.singletonList(player);
    hideHornHologramFromPlayer(plugin, arena, player);

    CutsceneManager cm = arena.getCutsceneManager();
    final double effectiveDriveSec = BusCutsceneRig.computeEffectiveDriveSeconds(
      cm.getBusDriveDurationSeconds(),
      0.0,
      false
    );
    if(cm.hasBusCutscene()) {
      session.busRig = BusCutsceneRig.spawn(
        plugin,
        arena,
        cm.getBusStartLocation(),
        cm.getBusEndLocation(),
        cm.getBusSchematicName(),
        cm.getBusHidePos1(),
        cm.getBusHidePos2(),
        recorderList
      );
    }
    final long recordStartNanos = System.nanoTime();

    BukkitTask tickTracker = new BukkitRunnable() {
      @Override
      public void run() {
        if(!player.isOnline()) {
          stopRecording(plugin, player, false);
          cancel();
          return;
        }
        session.latestFrame = CutsceneFrame.fromLocation(player.getLocation());
        if(session.busRig != null) {
          double elapsedSec = (double) (System.nanoTime() - recordStartNanos) / 1_000_000_000.0;
          session.busRig.tickMainThread(elapsedSec, effectiveDriveSec, recorderList, PacketCameraSender::updateServerEntityPosition);
        }
        int currentFrames = recorded.size();
        if(currentFrames >= MAX_RECORDING_FRAMES) {
          HalloweenChat.send(player, "<shadow:#000000FF><#5A4E7C>[</shadow><#FF7518>Cutscene<shadow:#000000FF><#5A4E7C>]</shadow> <#FFB703>บันทึกครบเวลาสูงสุด ระบบทำการบันทึกให้อัตโนมัติ");
          stopRecording(plugin, player, true);
          cancel();
          return;
        }
        if(currentFrames % 12 == 0) {
          double seconds = (double) currentFrames / FPS;
          HalloweenChat.sendActionBar(
            player,
            "<shadow:#000000FF><#5A4E7C>[</shadow><#FF3B47>Recording<shadow:#000000FF><#5A4E7C>]</shadow> <#F4F1DE>ห้อง <#FFB703>"
              + arena.getId()
              + " <shadow:#000000FF><#5A4E7C>|</shadow> <#6EEB83>"
              + String.format(Locale.US, "%.1fs", seconds)
              + " <shadow:#000000FF><#5A4E7C>(</shadow><#B8B0C8>/mma stopcutscene<shadow:#000000FF><#5A4E7C>)</shadow>"
          );
        }
      }
    }.runTaskTimer(plugin, 0L, 1L);

    ScheduledFuture<?> highFreqTask = getExecutor().scheduleAtFixedRate(() -> {
      if(recorded.size() < MAX_RECORDING_FRAMES) {
        recorded.add(session.latestFrame);
      }
      BusCutsceneRig busRig = session.busRig;
      if(busRig != null && busRig.getAnchor() != null) {
        double elapsedSec = (double) (System.nanoTime() - recordStartNanos) / 1_000_000_000.0;
        BusCutsceneRig.BusPose pose = busRig.computePose(elapsedSec, effectiveDriveSec);
        PacketCameraSender.sendEntityPositionRaw60Fps(busRig.getAnchor(), pose.x, pose.y, pose.z, 0.0f, 0.0f, recorderList);
      }
    }, 0L, FRAME_INTERVAL_NANOS, TimeUnit.NANOSECONDS);

    session.tickTask = tickTracker;
    session.captureFuture = highFreqTask;
    activeRecordings.put(player.getUniqueId(), session);
    player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.6f);
  }

  public static RecordingResult stopRecording(PluginMain plugin, Player player, boolean save) {
    if(player == null) {
      return null;
    }
    RecordingSession session = activeRecordings.remove(player.getUniqueId());
    if(session == null) {
      return null;
    }
    if(session.captureFuture != null) {
      session.captureFuture.cancel(false);
    }
    if(session.tickTask != null) {
      session.tickTask.cancel();
    }
    if(session.busRig != null) {
      try {
        session.busRig.cleanup(Collections.singletonList(player));
      } catch(Throwable ignored) {
      }
      session.busRig = null;
    }
    showHornHologramToPlayer(plugin, session.arena, player);

    List<CutsceneFrame> rawFrames;
    synchronized(session.frames) {
      rawFrames = new ArrayList<>(session.frames);
    }
    List<CutsceneFrame> smoothedFrames = smoothSubTickFrames(rawFrames);

    if(save && !smoothedFrames.isEmpty()) {
      Arena arena = session.arena;
      arena.getCutsceneManager().setFrames(smoothedFrames);

      FileConfiguration config = ConfigUtils.getConfig(plugin, "arenas");
      config.set("instances." + arena.getId() + ".cutscene", serializeFrames(smoothedFrames));
      ConfigUtils.saveConfig(plugin, config, "arenas");

      if(player.isOnline()) {
        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.2f);
        HalloweenChat.sendActionBar(
          player,
          "<shadow:#000000FF><#5A4E7C>[</shadow><#6EEB83>Cutscene<shadow:#000000FF><#5A4E7C>]</shadow> <#F4F1DE>บันทึกฉากเปิดตัวห้อง <#FFB703>" + arena.getId() + " <#6EEB83>สำเร็จ"
        );
      }
    }
    return new RecordingResult(session.arena, smoothedFrames.size());
  }

  private static List<CutsceneFrame> smoothSubTickFrames(List<CutsceneFrame> raw) {
    if(raw == null || raw.size() < 2) {
      return raw == null ? new ArrayList<>() : new ArrayList<>(raw);
    }
    List<CutsceneFrame> result = new ArrayList<>(raw);
    int n = result.size();
    int i = 0;
    while(i < n - 1) {
      CutsceneFrame start = result.get(i);
      int j = i + 1;
      while(j < n && result.get(j).isSamePositionAndRotation(start)) {
        j++;
      }
      int gap = j - i;
      if(gap > 1 && gap <= 5 && j < n) {
        CutsceneFrame end = result.get(j);
        if(start.worldName.equals(end.worldName)) {
          for(int k = 1; k < gap; k++) {
            double t = (double) k / (double) gap;
            result.set(i + k, start.interpolate(end, t));
          }
        }
      }
      i = j;
    }
    return result;
  }

  public static boolean startPreview(PluginMain plugin, Player player, Arena arena) {
    stopRecording(plugin, player, false);
    stopPreview(player);

    CutsceneManager cm = arena.getCutsceneManager();
    List<CutsceneFrame> cutsceneFrames = cm.getEffectiveFrames();
    if(cutsceneFrames.isEmpty()) {
      return false;
    }
    final boolean customCamera = !cm.getFrames().isEmpty();

    Location originalLoc = player.getLocation().clone();
    GameMode originalGameMode = player.getGameMode();
    float originalFlySpeed = player.getFlySpeed();
    boolean originalAllowFlight = player.getAllowFlight();
    boolean originalFlying = player.isFlying();
    World fallbackWorld = cm.getFallbackWorld();

    CutsceneFrame firstFrame = cutsceneFrames.get(0);
    Location startEyeLoc = firstFrame.toEyeLocation(fallbackWorld);
    Location startOverlayLoc = firstFrame.toOverlayLocation(fallbackWorld);
    if(startEyeLoc != null && startEyeLoc.getWorld() != null) {
      startEyeLoc.getChunk().load(true);
    }

    final UUID previewToken = UUID.randomUUID();
    final long startNanos = System.nanoTime();
    final int totalFrames = cutsceneFrames.size();
    final double totalCutsceneSec = (double) totalFrames / FPS;
    final double effectiveDriveSec = BusCutsceneRig.computeEffectiveDriveSeconds(
      cm.getBusDriveDurationSeconds(),
      totalCutsceneSec,
      customCamera
    );
    final List<Player> viewerList = Collections.singletonList(player);

    PreviewSession previewSession = new PreviewSession(
      previewToken,
      plugin,
      arena,
      originalLoc,
      originalGameMode,
      originalFlySpeed,
      originalAllowFlight,
      originalFlying
    );
    activePreviews.put(player.getUniqueId(), previewSession);
    hideHornHologramFromPlayer(plugin, arena, player);

    teleportToCutsceneCamera(player, startEyeLoc, null);

    List<CameraShotRig> spawnedShots = spawnShotRigs(cutsceneFrames, fallbackWorld);
    TextDisplay initialRig = resolveActiveRig(spawnedShots, 0);
    TextDisplay overlay = spawnFadeOverlay(startOverlayLoc);
    previewSession.shotRigs = spawnedShots;
    previewSession.overlay = overlay;
    if(cm.hasBusCutscene()) {
      previewSession.busRig = BusCutsceneRig.spawn(
        plugin,
        arena,
        cm.getBusStartLocation(),
        cm.getBusEndLocation(),
        cm.getBusSchematicName(),
        cm.getBusHidePos1(),
        cm.getBusHidePos2(),
        viewerList
      );
    }

    if(initialRig != null && initialRig.isValid() && player.getWorld().equals(initialRig.getWorld())) {
      runWithInternalBypass(player, () -> player.setSpectatorTarget(initialRig));
      PacketCameraSender.sendSetCamera(initialRig, viewerList);
    }

    final AtomicReference<TextDisplay> lastSentHighFreqRig = new AtomicReference<>(initialRig);

    BukkitTask syncTask = new BukkitRunnable() {
      private int tickCount = 0;
      private TextDisplay lastSyncedRig = initialRig;

      @Override
      public void run() {
        if(!player.isOnline()) {
          stopPreview(player, previewToken);
          cancel();
          return;
        }
        long elapsedNanos = System.nanoTime() - startNanos;
        int idx = (int) (elapsedNanos / FRAME_INTERVAL_NANOS);
        if(idx >= totalFrames) {
          stopPreview(player, previewToken);
          cancel();
          return;
        }
        tickCount++;
        int clampedIdx = Math.min(idx, totalFrames - 1);
        CutsceneFrame frame = cutsceneFrames.get(clampedIdx);
        Location eyeLoc = frame.toEyeLocation(fallbackWorld);
        TextDisplay activeRig = resolveActiveRig(spawnedShots, clampedIdx);
        boolean shotSwitched = activeRig != null && activeRig != lastSyncedRig;
        if(shotSwitched) {
          lastSyncedRig = activeRig;
        }
        boolean shouldReconfirmCamera = shotSwitched || tickCount <= 6 || tickCount == 10 || tickCount == 15 || tickCount % 10 == 0;
        if(eyeLoc != null && eyeLoc.getWorld() != null) {
          if(activeRig != null && activeRig.isValid()) {
            activeRig.teleport(eyeLoc);
            if(!player.getWorld().equals(activeRig.getWorld()) || player.getLocation().distanceSquared(eyeLoc) > 2304.0) {
              teleportToCutsceneCamera(player, eyeLoc, activeRig);
            } else {
              runWithInternalBypass(player, () -> {
                if(player.getGameMode() != GameMode.SPECTATOR) {
                  player.setGameMode(GameMode.SPECTATOR);
                }
                player.setFlySpeed(0.0f);
                if(player.getSpectatorTarget() != activeRig) {
                  player.setSpectatorTarget(activeRig);
                } else if(!PacketCameraSender.isSetCameraSupported() && (tickCount == 2 || tickCount == 6)) {
                  player.setSpectatorTarget(null);
                  player.setSpectatorTarget(activeRig);
                }
              });
            }
            if(shouldReconfirmCamera) {
              PacketCameraSender.sendSetCamera(activeRig, viewerList);
            }
          } else {
            Location fallbackLoc = frame.toLocation(fallbackWorld);
            if(fallbackLoc != null) {
              runWithInternalBypass(player, () -> {
                if(player.getGameMode() != GameMode.SPECTATOR) {
                  player.setGameMode(GameMode.SPECTATOR);
                }
                player.setFlySpeed(0.0f);
                player.teleport(fallbackLoc, PlayerTeleportEvent.TeleportCause.PLUGIN);
              });
            }
          }
          updateFadeOverlayTick(overlay, frame, fallbackWorld, idx, totalFrames);
          if(previewSession.busRig != null) {
            double elapsedSec = (double) elapsedNanos / 1_000_000_000.0;
            previewSession.busRig.tickMainThread(elapsedSec, effectiveDriveSec, viewerList, PacketCameraSender::updateServerEntityPosition);
          }
        }
        double remaining = (double) (totalFrames - idx) / FPS;
        HalloweenChat.sendActionBar(
          player,
          "<shadow:#000000FF><#5A4E7C>[</shadow><#FF7518>Preview<shadow:#000000FF><#5A4E7C>]</shadow> <#F4F1DE>ห้อง <#FFB703>"
            + arena.getId()
            + " <shadow:#000000FF><#5A4E7C>|</shadow> <#6EEB83>เหลือ "
            + String.format(Locale.US, "%.1fs", remaining)
        );
      }
    }.runTaskTimer(plugin, 0L, 1L);

    ScheduledFuture<?> highFreqFuture = getExecutor().scheduleAtFixedRate(() -> {
      if(!player.isOnline()) {
        return;
      }
      long elapsedNanos = System.nanoTime() - startNanos;
      int idx = (int) (elapsedNanos / FRAME_INTERVAL_NANOS);
      if(idx >= totalFrames) {
        PreviewSession active = activePreviews.get(player.getUniqueId());
        if(active != null && active.sessionToken.equals(previewToken) && active.highFreqFuture != null) {
          active.highFreqFuture.cancel(false);
        }
        Bukkit.getScheduler().runTask(plugin, () -> stopPreview(player, previewToken));
        return;
      }
      CutsceneFrame frame = cutsceneFrames.get(idx);
      TextDisplay rig = resolveActiveRig(spawnedShots, idx);
      if(rig != null) {
        if(lastSentHighFreqRig.getAndSet(rig) != rig) {
          PacketCameraSender.sendSetCamera(rig, viewerList);
        }
        PacketCameraSender.sendEntityPosition60Fps(rig, frame, viewerList);
      }
      if(overlay != null && computeFadeAlpha(idx, totalFrames) > 0) {
        PacketCameraSender.sendOverlayPosition60Fps(overlay, frame, viewerList);
      }
      BusCutsceneRig busRig = previewSession.busRig;
      if(busRig != null && busRig.getAnchor() != null) {
        double elapsedSec = (double) elapsedNanos / 1_000_000_000.0;
        BusCutsceneRig.BusPose pose = busRig.computePose(elapsedSec, effectiveDriveSec);
        PacketCameraSender.sendEntityPositionRaw60Fps(busRig.getAnchor(), pose.x, pose.y, pose.z, 0.0f, 0.0f, viewerList);
      }
    }, 0L, FRAME_INTERVAL_NANOS, TimeUnit.NANOSECONDS);

    previewSession.syncTask = syncTask;
    previewSession.highFreqFuture = highFreqFuture;
    if(plugin instanceof Main) {
      Arena playerArena = ((Main) plugin).getArenaRegistry().getArena(player);
      if(playerArena != null) {
        IUser user = plugin.getUserManager().getUser(player);
        if(user != null) {
          playerArena.getScoreboardManager().removeScoreboard(user);
        }
      }
    }
    return true;
  }

  public static void shutdownAll(PluginMain plugin) {
    for(UUID uuid : new ArrayList<>(activeRecordings.keySet())) {
      Player player = Bukkit.getPlayer(uuid);
      if(player != null) {
        stopRecording(plugin, player, false);
      } else {
        RecordingSession session = activeRecordings.remove(uuid);
        if(session != null) {
          if(session.captureFuture != null) {
            session.captureFuture.cancel(false);
          }
          if(session.tickTask != null) {
            session.tickTask.cancel();
          }
          if(session.busRig != null) {
            try {
              session.busRig.cleanup(Collections.emptyList());
            } catch(Throwable ignored) {
            }
          }
        }
      }
    }
    for(UUID uuid : new ArrayList<>(activePreviews.keySet())) {
      Player player = Bukkit.getPlayer(uuid);
      if(player != null) {
        stopPreview(player);
      } else {
        PreviewSession session = activePreviews.remove(uuid);
        if(session != null) {
          if(session.highFreqFuture != null) {
            session.highFreqFuture.cancel(false);
          }
          if(session.syncTask != null) {
            session.syncTask.cancel();
          }
          if(session.busRig != null) {
            try {
              session.busRig.cleanup(Collections.emptyList());
            } catch(Throwable ignored) {
            }
          }
          destroyShotRigs(session.shotRigs);
          session.shotRigs = Collections.emptyList();
          if(session.overlay != null) {
            try {
              if(session.overlay.isValid()) {
                session.overlay.remove();
              }
            } catch(Throwable ignored) {
            }
          }
        }
      }
    }
    synchronized(CutsceneManager.class) {
      if(executor != null) {
        executor.shutdownNow();
        executor = null;
      }
    }
  }

  public static void handlePlayerQuit(PluginMain plugin, Player player) {
    if(player == null) {
      return;
    }
    stopRecording(plugin, player, false);
    stopPreview(player, null);
  }

  public static boolean stopPreview(Player player) {
    return stopPreview(player, null);
  }

  private static boolean stopPreview(Player player, UUID expectedToken) {
    if(player == null) {
      return false;
    }
    if(expectedToken != null) {
      PreviewSession current = activePreviews.get(player.getUniqueId());
      if(current == null || !current.sessionToken.equals(expectedToken)) {
        return false;
      }
    }
    PreviewSession session = activePreviews.remove(player.getUniqueId());
    if(session == null) {
      return false;
    }
    if(session.highFreqFuture != null) {
      session.highFreqFuture.cancel(false);
    }
    if(session.syncTask != null) {
      session.syncTask.cancel();
    }
    if(session.busRig != null) {
      try {
        session.busRig.cleanup(Collections.singletonList(player));
      } catch(Throwable ignored) {
      }
      session.busRig = null;
    }
    showHornHologramToPlayer(session.plugin, session.arena, player);
    try {
      runWithInternalBypass(player, () -> {
        if(player.getGameMode() == GameMode.SPECTATOR && player.getSpectatorTarget() != null) {
          player.setSpectatorTarget(null);
        }
        if(session.originalLoc != null && session.originalLoc.getWorld() != null) {
          session.originalLoc.getChunk().load(true);
          player.teleport(session.originalLoc, PlayerTeleportEvent.TeleportCause.PLUGIN);
        }
        player.setGameMode(session.originalGameMode);
        player.setFlySpeed(session.originalFlySpeed);
        player.setWalkSpeed(0.2f);
        player.setAllowFlight(session.originalAllowFlight);
        if(session.originalAllowFlight) {
          player.setFlying(session.originalFlying);
        }
      });
    } catch(Throwable ignored) {
    }
    if(player.isOnline()) {
      applyPostCutsceneFadeIn(player);
      HalloweenChat.sendActionBar(player, HalloweenChat.color("<#6EEB83>จบการเล่นตัวอย่าง Cutscene แล้ว"));
      if(session.plugin instanceof Main) {
        Arena playerArena = ((Main) session.plugin).getArenaRegistry().getArena(player);
        if(playerArena != null && !playerArena.getCutsceneManager().isPlaying() && playerArena.getArenaState() != IArenaState.RESTARTING) {
          IUser user = session.plugin.getUserManager().getUser(player);
          if(user != null) {
            playerArena.getScoreboardManager().createScoreboard(user);
          }
        }
      }
    }
    destroyShotRigs(session.shotRigs);
    session.shotRigs = Collections.emptyList();
    if(session.overlay != null) {
      try {
        if(session.overlay.isValid()) {
          session.overlay.remove();
        }
      } catch(Throwable ignored) {
      }
    }
    return true;
  }

  public static List<String> serializeFrames(List<CutsceneFrame> frameList) {
    List<String> out = new ArrayList<>(frameList.size());
    for(CutsceneFrame f : frameList) {
      out.add(f.serialize());
    }
    return out;
  }

  public static List<CutsceneFrame> deserializeFrames(List<String> serializedList) {
    List<CutsceneFrame> out = new ArrayList<>();
    if(serializedList == null) {
      return out;
    }
    for(String line : serializedList) {
      CutsceneFrame frame = CutsceneFrame.deserialize(line);
      if(frame != null) {
        out.add(frame);
      }
    }
    return out;
  }

  public static class CutsceneFrame {
    private final String worldName;
    private final double x;
    private final double y;
    private final double z;
    private final float yaw;
    private final float pitch;

    public CutsceneFrame(String worldName, double x, double y, double z, float yaw, float pitch) {
      this.worldName = worldName;
      this.x = x;
      this.y = y;
      this.z = z;
      this.yaw = yaw;
      this.pitch = pitch;
    }

    public static CutsceneFrame fromLocation(Location loc) {
      String w = loc.getWorld() != null ? loc.getWorld().getName() : "world";
      return new CutsceneFrame(w, loc.getX(), loc.getY(), loc.getZ(), loc.getYaw(), loc.getPitch());
    }

    public boolean isSamePositionAndRotation(CutsceneFrame other) {
      if(other == null || !this.worldName.equals(other.worldName)) {
        return false;
      }
      return Math.abs(this.x - other.x) < 1e-4
        && Math.abs(this.y - other.y) < 1e-4
        && Math.abs(this.z - other.z) < 1e-4
        && Math.abs(this.yaw - other.yaw) < 1e-3f
        && Math.abs(this.pitch - other.pitch) < 1e-3f;
    }

    public CutsceneFrame interpolate(CutsceneFrame target, double t) {
      double nx = this.x + (target.x - this.x) * t;
      double ny = this.y + (target.y - this.y) * t;
      double nz = this.z + (target.z - this.z) * t;
      float deltaYaw = wrapDegrees(target.yaw - this.yaw);
      float nyaw = wrapDegrees(this.yaw + (float) (deltaYaw * t));
      float npitch = (float) (this.pitch + (target.pitch - this.pitch) * t);
      return new CutsceneFrame(this.worldName, nx, ny, nz, nyaw, npitch);
    }

    private static float wrapDegrees(float degrees) {
      float d = degrees % 360.0f;
      if(d >= 180.0f) {
        d -= 360.0f;
      }
      if(d < -180.0f) {
        d += 360.0f;
      }
      return d;
    }

    public Location toLocation(World fallbackWorld) {
      World w = worldName != null ? Bukkit.getWorld(worldName) : null;
      if(w == null) {
        w = fallbackWorld;
      }
      if(w == null) {
        return null;
      }
      return new Location(w, x, y, z, yaw, pitch);
    }

    public Location toEyeLocation(World fallbackWorld) {
      World w = worldName != null ? Bukkit.getWorld(worldName) : null;
      if(w == null) {
        w = fallbackWorld;
      }
      if(w == null) {
        return null;
      }
      return new Location(w, x, y + PLAYER_EYE_OFFSET, z, yaw, pitch);
    }

    public Location toOverlayLocation(World fallbackWorld) {
      World w = worldName != null ? Bukkit.getWorld(worldName) : null;
      if(w == null) {
        w = fallbackWorld;
      }
      if(w == null) {
        return null;
      }
      double yawRad = Math.toRadians(yaw);
      double pitchRad = Math.toRadians(pitch);
      double cosPitch = Math.cos(pitchRad);
      double dx = -Math.sin(yawRad) * cosPitch;
      double dy = -Math.sin(pitchRad);
      double dz = Math.cos(yawRad) * cosPitch;
      return new Location(
        w,
        x + dx * OVERLAY_DISTANCE,
        y + PLAYER_EYE_OFFSET + dy * OVERLAY_DISTANCE,
        z + dz * OVERLAY_DISTANCE,
        yaw,
        pitch
      );
    }

    public String serialize() {
      return String.format(Locale.US, "%s,%.3f,%.3f,%.3f,%.2f,%.2f", worldName, x, y, z, yaw, pitch);
    }

    public static CutsceneFrame deserialize(String raw) {
      if(raw == null || raw.isEmpty()) {
        return null;
      }
      String[] parts = raw.split(",");
      if(parts.length < 6) {
        return null;
      }
      try {
        String worldName = parts[0].trim();
        double x = Double.parseDouble(parts[1].trim());
        double y = Double.parseDouble(parts[2].trim());
        double z = Double.parseDouble(parts[3].trim());
        float yaw = Float.parseFloat(parts[4].trim());
        float pitch = Float.parseFloat(parts[5].trim());
        return new CutsceneFrame(worldName, x, y, z, yaw, pitch);
      } catch(NumberFormatException ex) {
        return null;
      }
    }
  }

  public static String serializePreciseLocation(Location loc) {
    if(loc == null) {
      return null;
    }
    return CutsceneFrame.fromLocation(loc).serialize();
  }

  public static Location deserializePreciseLocation(String raw) {
    if(raw == null || raw.isEmpty()) {
      return null;
    }
    CutsceneFrame frame = CutsceneFrame.deserialize(raw);
    if(frame != null) {
      World fallback = !Bukkit.getWorlds().isEmpty() ? Bukkit.getWorlds().get(0) : null;
      return frame.toLocation(fallback);
    }
    return plugily.projects.minigamesbox.classic.utils.serialization.LocationSerializer.getLocation(raw);
  }

  public static class RecordingSession {
    private final Arena arena;
    private final List<CutsceneFrame> frames;
    private volatile CutsceneFrame latestFrame;
    private BukkitTask tickTask;
    private ScheduledFuture<?> captureFuture;
    private volatile BusCutsceneRig busRig;

    public RecordingSession(Arena arena, List<CutsceneFrame> frames, CutsceneFrame initialFrame) {
      this.arena = arena;
      this.frames = frames;
      this.latestFrame = initialFrame;
    }

    public Arena getArena() {
      return arena;
    }

    public int getFrameCount() {
      return frames.size();
    }
  }

  public static class RecordingResult {
    private final Arena arena;
    private final int frameCount;

    public RecordingResult(Arena arena, int frameCount) {
      this.arena = arena;
      this.frameCount = frameCount;
    }

    public Arena getArena() {
      return arena;
    }

    public int getFrameCount() {
      return frameCount;
    }

    public double getDurationSeconds() {
      return (double) frameCount / FPS;
    }
  }

  private static final class CameraShotRig {
    private final int startFrame;
    private final TextDisplay rig;

    private CameraShotRig(int startFrame, TextDisplay rig) {
      this.startFrame = startFrame;
      this.rig = rig;
    }
  }

  private static class PreviewSession {
    private final UUID sessionToken;
    private final PluginMain plugin;
    private final Arena arena;
    private final Location originalLoc;
    private final GameMode originalGameMode;
    private final float originalFlySpeed;
    private final boolean originalAllowFlight;
    private final boolean originalFlying;
    private volatile BukkitTask syncTask;
    private volatile ScheduledFuture<?> highFreqFuture;
    private volatile List<CameraShotRig> shotRigs = Collections.emptyList();
    private volatile TextDisplay overlay;
    private volatile BusCutsceneRig busRig;

    private PreviewSession(
      UUID sessionToken,
      PluginMain plugin,
      Arena arena,
      Location originalLoc,
      GameMode originalGameMode,
      float originalFlySpeed,
      boolean originalAllowFlight,
      boolean originalFlying
    ) {
      this.sessionToken = sessionToken;
      this.plugin = plugin;
      this.arena = arena;
      this.originalLoc = originalLoc;
      this.originalGameMode = originalGameMode;
      this.originalFlySpeed = originalFlySpeed;
      this.originalAllowFlight = originalAllowFlight;
      this.originalFlying = originalFlying;
    }
  }

  static void syncVehiclePassengers(Entity vehicle, Collection<Player> viewers) {
    PacketCameraSender.sendPassengersPacket(vehicle, viewers);
  }

  private static final class PacketCameraSender {
    private static volatile boolean initialized = false;
    private static volatile boolean supported = false;
    private static volatile boolean setCameraSupported = false;
    private static volatile Method craftPlayerGetHandle;
    private static Field serverPlayerConnectionField;
    private static Method packetSendMethod;
    private static Constructor<?> vec3Constructor;
    private static Object zeroVec3;
    private static Constructor<?> posMoveRotConstructor;
    private static Constructor<?> syncPacketConstructor;
    private static Constructor<?> rotateHeadConstructor;
    private static Constructor<?> setCameraConstructor;
    private static Constructor<?> setPassengersConstructor;
    private static Method entitySetPosMethod;
    private static volatile Method craftEntityGetHandle;

    private static synchronized void init() {
      if(initialized) {
        return;
      }
      initialized = true;
      try {
        Class<?> entityClass = Class.forName("net.minecraft.world.entity.Entity");
        try {
          entitySetPosMethod = entityClass.getMethod("setPos", double.class, double.class, double.class);
        } catch(Throwable ignored) {
          entitySetPosMethod = null;
        }
        Class<?> packetClass = Class.forName("net.minecraft.network.protocol.Packet");
        Class<?> commonListenerClass = Class.forName("net.minecraft.server.network.ServerCommonPacketListenerImpl");
        packetSendMethod = commonListenerClass.getMethod("send", packetClass);

        Class<?> serverPlayerClass = Class.forName("net.minecraft.server.level.ServerPlayer");
        serverPlayerConnectionField = serverPlayerClass.getField("connection");

        try {
          Class<?> setCameraClass = Class.forName("net.minecraft.network.protocol.game.ClientboundSetCameraPacket");
          setCameraConstructor = setCameraClass.getConstructor(entityClass);
          setCameraSupported = true;
        } catch(Throwable ignored) {
          setCameraSupported = false;
        }

        try {
          Class<?> setPassengersClass = Class.forName("net.minecraft.network.protocol.game.ClientboundSetPassengersPacket");
          setPassengersConstructor = setPassengersClass.getConstructor(entityClass);
        } catch(Throwable ignored) {
          setPassengersConstructor = null;
        }

        Class<?> vec3Class = Class.forName("net.minecraft.world.phys.Vec3");
        vec3Constructor = vec3Class.getConstructor(double.class, double.class, double.class);
        zeroVec3 = vec3Constructor.newInstance(0.0, 0.0, 0.0);

        Class<?> posMoveRotClass = Class.forName("net.minecraft.world.entity.PositionMoveRotation");
        posMoveRotConstructor = posMoveRotClass.getConstructor(vec3Class, vec3Class, float.class, float.class);

        Class<?> syncPacketClass = Class.forName("net.minecraft.network.protocol.game.ClientboundEntityPositionSyncPacket");
        syncPacketConstructor = syncPacketClass.getConstructor(int.class, posMoveRotClass, boolean.class);

        Class<?> rotateHeadClass = Class.forName("net.minecraft.network.protocol.game.ClientboundRotateHeadPacket");
        rotateHeadConstructor = rotateHeadClass.getConstructor(entityClass, byte.class);

        supported = true;
      } catch(Throwable ignored) {
        supported = false;
      }
    }

    private static Object resolveNmsEntity(Entity entity) throws Exception {
      Method getter = craftEntityGetHandle;
      if(getter == null || !getter.getDeclaringClass().isInstance(entity)) {
        Class<?> curr = entity.getClass();
        Method baseMethod = curr.getMethod("getHandle");
        while(curr.getSuperclass() != null) {
          try {
            baseMethod = curr.getSuperclass().getMethod("getHandle");
            curr = curr.getSuperclass();
          } catch(NoSuchMethodException stop) {
            break;
          }
        }
        getter = baseMethod;
        craftEntityGetHandle = getter;
      }
      return getter.invoke(entity);
    }

    static boolean isSetCameraSupported() {
      init();
      return setCameraSupported;
    }

    static void updateServerEntityPosition(Entity entity, double x, double y, double z) {
      if(entity == null || !entity.isValid()) {
        return;
      }
      init();
      if(entitySetPosMethod != null) {
        try {
          Object nmsEntity = resolveNmsEntity(entity);
          entitySetPosMethod.invoke(nmsEntity, x, y, z);
          List<Entity> passengers = entity.getPassengers();
          if(passengers != null && !passengers.isEmpty()) {
            for(Entity passenger : passengers) {
              if(passenger != null && passenger.isValid()) {
                try {
                  Object nmsPassenger = resolveNmsEntity(passenger);
                  entitySetPosMethod.invoke(nmsPassenger, x, y, z);
                } catch(Throwable ignored) {
                }
              }
            }
          }
          return;
        } catch(Throwable ignored) {
        }
      }
      try {
        Location cur = entity.getLocation();
        double dx = cur.getX() - x;
        double dy = cur.getY() - y;
        double dz = cur.getZ() - z;
        if(dx * dx + dy * dy + dz * dz > 64.0 && cur.getWorld() != null) {
          entity.teleport(
            new Location(cur.getWorld(), x, y, z, 0.0f, 0.0f),
            PlayerTeleportEvent.TeleportCause.PLUGIN,
            io.papermc.paper.entity.TeleportFlag.EntityState.RETAIN_PASSENGERS
          );
        }
      } catch(Throwable ignored) {
      }
    }

    static void sendPassengersPacket(Entity vehicle, Collection<Player> viewers) {
      init();
      if(setPassengersConstructor == null || vehicle == null || !vehicle.isValid() || viewers == null || viewers.isEmpty()) {
        return;
      }
      try {
        Object nmsVehicle = resolveNmsEntity(vehicle);
        Object passengersPacket = setPassengersConstructor.newInstance(nmsVehicle);
        for(Player viewer : viewers) {
          if(viewer == null || !viewer.isOnline()) {
            continue;
          }
          if(craftPlayerGetHandle == null) {
            craftPlayerGetHandle = viewer.getClass().getMethod("getHandle");
          }
          Object handle = craftPlayerGetHandle.invoke(viewer);
          Object connection = serverPlayerConnectionField.get(handle);
          if(connection != null) {
            packetSendMethod.invoke(connection, passengersPacket);
          }
        }
      } catch(Throwable ignored) {
      }
    }

    static void sendSetCamera(Entity entity, List<Player> viewers) {
      init();
      if(!setCameraSupported || entity == null || viewers == null || viewers.isEmpty()) {
        return;
      }
      try {
        Object nmsEntity = resolveNmsEntity(entity);
        Object cameraPacket = setCameraConstructor.newInstance(nmsEntity);
        for(Player viewer : viewers) {
          if(viewer == null || !viewer.isOnline()) {
            continue;
          }
          if(craftPlayerGetHandle == null) {
            craftPlayerGetHandle = viewer.getClass().getMethod("getHandle");
          }
          Object handle = craftPlayerGetHandle.invoke(viewer);
          Object connection = serverPlayerConnectionField.get(handle);
          if(connection != null) {
            packetSendMethod.invoke(connection, cameraPacket);
          }
        }
      } catch(Throwable ignored) {
      }
    }

    static void sendEntityPosition60Fps(TextDisplay rig, CutsceneFrame frame, List<Player> viewers) {
      if(rig == null || frame == null || viewers == null || viewers.isEmpty()) {
        return;
      }
      sendPositionPacket(rig, frame.x, frame.y + PLAYER_EYE_OFFSET, frame.z, frame.yaw, frame.pitch, true, viewers);
    }

    static void sendOverlayPosition60Fps(TextDisplay overlay, CutsceneFrame frame, List<Player> viewers) {
      if(overlay == null || frame == null || viewers == null || viewers.isEmpty()) {
        return;
      }
      double yawRad = Math.toRadians(frame.yaw);
      double pitchRad = Math.toRadians(frame.pitch);
      double cosPitch = Math.cos(pitchRad);
      double dx = -Math.sin(yawRad) * cosPitch;
      double dy = -Math.sin(pitchRad);
      double dz = Math.cos(yawRad) * cosPitch;
      sendPositionPacket(
        overlay,
        frame.x + dx * OVERLAY_DISTANCE,
        frame.y + PLAYER_EYE_OFFSET + dy * OVERLAY_DISTANCE,
        frame.z + dz * OVERLAY_DISTANCE,
        frame.yaw,
        frame.pitch,
        true,
        viewers
      );
    }

    static void sendEntityPositionRaw60Fps(Entity entity, double x, double y, double z, float yaw, float pitch, List<Player> viewers) {
      if(entity == null || viewers == null || viewers.isEmpty()) {
        return;
      }
      sendPositionPacket(entity, x, y, z, yaw, pitch, false, viewers);
    }

    private static void sendPositionPacket(Entity entity, double x, double y, double z, float yaw, float pitch, boolean includeHeadRot, List<Player> viewers) {
      init();
      if(!supported) {
        return;
      }
      try {
        int entityId = entity.getEntityId();
        Object posVec = vec3Constructor.newInstance(x, y, z);
        Object posMoveRot = posMoveRotConstructor.newInstance(posVec, zeroVec3, yaw, pitch);
        Object syncPacket = syncPacketConstructor.newInstance(entityId, posMoveRot, false);

        Object headPacket = null;
        if(includeHeadRot) {
          Object nmsEntity = resolveNmsEntity(entity);
          byte headYawByte = (byte) ((int) (yaw * 256.0f / 360.0f));
          headPacket = rotateHeadConstructor.newInstance(nmsEntity, headYawByte);
        }

        for(Player viewer : viewers) {
          if(viewer == null || !viewer.isOnline()) {
            continue;
          }
          if(craftPlayerGetHandle == null) {
            craftPlayerGetHandle = viewer.getClass().getMethod("getHandle");
          }
          Object handle = craftPlayerGetHandle.invoke(viewer);
          Object connection = serverPlayerConnectionField.get(handle);
          if(connection != null) {
            packetSendMethod.invoke(connection, syncPacket);
            if(headPacket != null) {
              packetSendMethod.invoke(connection, headPacket);
            }
          }
        }
      } catch(Throwable ignored) {
      }
    }
  }
}
