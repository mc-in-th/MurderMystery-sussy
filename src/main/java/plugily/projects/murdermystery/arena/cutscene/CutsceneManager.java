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
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Display;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Transformation;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;
import plugily.projects.minigamesbox.api.arena.IArenaState;
import plugily.projects.minigamesbox.classic.PluginMain;
import plugily.projects.minigamesbox.classic.utils.configuration.ConfigUtils;
import plugily.projects.minigamesbox.classic.utils.version.VersionUtils;
import plugily.projects.murdermystery.Main;
import plugily.projects.murdermystery.arena.Arena;
import plugily.projects.murdermystery.utils.HalloweenChat;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
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

public class CutsceneManager {

  public static final int FPS = 60;
  private static final long FRAME_INTERVAL_NANOS = 1_000_000_000L / FPS;
  private static final int MAX_RECORDING_FRAMES = FPS * 60;
  private static final double PLAYER_EYE_OFFSET = 1.62;
  private static final double OVERLAY_DISTANCE = 0.15;
  private static final int FADE_IN_FRAMES = 60;
  private static final int FADE_OUT_FRAMES = 45;

  private static final ScheduledExecutorService EXECUTOR = Executors.newScheduledThreadPool(2, r -> {
    Thread t = new Thread(r, "MurderMystery-Cutscene-60FPS");
    t.setDaemon(true);
    return t;
  });

  private static final Map<UUID, RecordingSession> activeRecordings = new ConcurrentHashMap<>();
  private static final Map<UUID, PreviewSession> activePreviews = new ConcurrentHashMap<>();

  private final Arena arena;
  private final List<CutsceneFrame> frames = new ArrayList<>();
  private ScheduledFuture<?> playbackFuture;
  private BukkitTask syncTask;
  private TextDisplay cameraRig;
  private TextDisplay fadeOverlay;
  private volatile boolean playing = false;

  public CutsceneManager(Arena arena) {
    this.arena = arena;
  }

  public boolean hasCutscene() {
    return !frames.isEmpty();
  }

  public boolean isPlaying() {
    return playing;
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
    return (double) frames.size() / FPS;
  }

  public void startCutscene(Runnable onComplete) {
    cleanup();
    if(frames.isEmpty()) {
      if(onComplete != null) {
        onComplete.run();
      }
      return;
    }

    playing = true;
    Main plugin = arena.getPlugin();
    World fallbackWorld = getFallbackWorld();
    List<CutsceneFrame> snapshotFrames = new ArrayList<>(frames);

    List<Player> participants = new ArrayList<>(arena.getPlayersLeft());
    CutsceneFrame firstFrame = snapshotFrames.get(0);
    Location initialEyeLoc = firstFrame.toEyeLocation(fallbackWorld);
    Location initialOverlayLoc = firstFrame.toOverlayLocation(fallbackWorld);
    cameraRig = spawnCameraRig(initialEyeLoc);
    fadeOverlay = spawnFadeOverlay(initialOverlayLoc);

    for(Player player : participants) {
      if(player == null || !player.isOnline()) {
        continue;
      }
      preparePlayerForCutscene(player, participants, initialEyeLoc, cameraRig);
      HalloweenChat.sendTitles(
        player,
        HalloweenChat.color("<gradient:#9D4EDD:#FF7518><bold>" + arena.getMapName() + "</bold></gradient>"),
        HalloweenChat.color("<#B8B0C8>โปรดระวังคนร้ายที่แฝงตัวอยู่ในกลุ่มของคุณ..."),
        15,
        45,
        15
      );
      player.playSound(player.getLocation(), Sound.BLOCK_PORTAL_TRIGGER, 0.5f, 1.2f);
    }

    final long startNanos = System.nanoTime();
    final int totalFrames = snapshotFrames.size();

    syncTask = new BukkitRunnable() {
      @Override
      public void run() {
        if(!playing || arena.getArenaState() != IArenaState.IN_GAME) {
          cancel();
          return;
        }
        int frameIdx = (int) ((System.nanoTime() - startNanos) / FRAME_INTERVAL_NANOS);
        if(frameIdx >= totalFrames) {
          finishCutscene(onComplete);
          cancel();
          return;
        }
        CutsceneFrame currentFrame = snapshotFrames.get(Math.min(frameIdx, totalFrames - 1));
        Location eyeLoc = currentFrame.toEyeLocation(fallbackWorld);
        if(eyeLoc != null && eyeLoc.getWorld() != null) {
          if(cameraRig != null && cameraRig.isValid()) {
            cameraRig.teleport(eyeLoc);
          }
          updateFadeOverlayTick(fadeOverlay, currentFrame, fallbackWorld, frameIdx, totalFrames);
          int remainingSec = Math.max(1, (int) Math.ceil((double) (totalFrames - frameIdx) / FPS));
          for(Player player : arena.getPlayersLeft()) {
            if(player == null || !player.isOnline()) {
              continue;
            }
            if(player.getGameMode() != GameMode.SPECTATOR) {
              player.setGameMode(GameMode.SPECTATOR);
            }
            player.setFlySpeed(0.0f);
            if(cameraRig != null && cameraRig.isValid()) {
              if(player.getSpectatorTarget() != cameraRig) {
                player.setSpectatorTarget(cameraRig);
              }
            } else {
              player.teleport(currentFrame.toLocation(fallbackWorld));
            }
            HalloweenChat.sendActionBar(
              player,
              HalloweenChat.color("<#B8B0C8>เกมกำลังจะเริ่มใน <#FFB703>" + remainingSec + "s")
            );
          }
        }
      }
    }.runTaskTimer(plugin, 0L, 1L);

    playbackFuture = EXECUTOR.scheduleAtFixedRate(() -> {
      if(!playing) {
        return;
      }
      int frameIdx = (int) ((System.nanoTime() - startNanos) / FRAME_INTERVAL_NANOS);
      if(frameIdx >= totalFrames) {
        Bukkit.getScheduler().runTask(plugin, () -> finishCutscene(onComplete));
        return;
      }
      CutsceneFrame frame = snapshotFrames.get(frameIdx);
      List<Player> viewers = arena.getPlayersLeft();
      TextDisplay rig = cameraRig;
      if(rig != null) {
        PacketCameraSender.sendEntityPosition60Fps(rig, frame, viewers);
      }
      TextDisplay overlay = fadeOverlay;
      if(overlay != null && computeFadeAlpha(frameIdx, totalFrames) > 0) {
        PacketCameraSender.sendOverlayPosition60Fps(overlay, frame, viewers);
      }
    }, 0L, FRAME_INTERVAL_NANOS, TimeUnit.NANOSECONDS);
  }

  private void finishCutscene(Runnable onComplete) {
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

    if(wasPlaying) {
      for(Player player : arena.getPlayers()) {
        if(player != null && player.isOnline()) {
          restorePlayerFromCutscene(player);
          applyPostCutsceneFadeIn(player);
        }
      }
    }
    removeCameraRig();

    if(onComplete != null && arena.getArenaState() == IArenaState.IN_GAME) {
      onComplete.run();
    }
  }

  public void handlePlayerLeave(Player player) {
    if(player == null) {
      return;
    }
    restorePlayerFromCutscene(player);
  }

  public void cleanup() {
    if(playbackFuture != null) {
      playbackFuture.cancel(false);
      playbackFuture = null;
    }
    if(syncTask != null) {
      syncTask.cancel();
      syncTask = null;
    }
    if(playing) {
      playing = false;
      for(Player player : arena.getPlayers()) {
        if(player != null && player.isOnline()) {
          restorePlayerFromCutscene(player);
        }
      }
    }
    removeCameraRig();
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

  private void removeCameraRig() {
    if(cameraRig != null) {
      try {
        if(cameraRig.isValid()) {
          cameraRig.remove();
        }
      } catch(Throwable ignored) {
      }
      cameraRig = null;
    }
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

  private void preparePlayerForCutscene(Player player, List<Player> allPlayers, Location startEyeLoc, TextDisplay rig) {
    Main plugin = arena.getPlugin();
    for(Player other : allPlayers) {
      if(other != null && !other.equals(player)) {
        VersionUtils.hidePlayer(plugin, player, other);
      }
    }
    if(startEyeLoc != null && startEyeLoc.getWorld() != null) {
      VersionUtils.teleport(player, startEyeLoc);
    }
    player.setGameMode(GameMode.SPECTATOR);
    player.setFlySpeed(0.0f);
    player.setWalkSpeed(0.0f);
    if(rig != null && rig.isValid()) {
      player.setSpectatorTarget(rig);
    }
  }

  private void restorePlayerFromCutscene(Player player) {
    Main plugin = arena.getPlugin();
    if(player.getGameMode() == GameMode.SPECTATOR && player.getSpectatorTarget() != null) {
      player.setSpectatorTarget(null);
    }
    player.setFlySpeed(0.1f);
    player.setWalkSpeed(0.2f);
    player.setGameMode(GameMode.ADVENTURE);
    player.setFlying(false);
    player.setAllowFlight(false);
    for(Player other : arena.getPlayers()) {
      if(other != null && !other.equals(player)) {
        VersionUtils.showPlayer(plugin, player, other);
      }
    }
  }

  private World getFallbackWorld() {
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

    BukkitTask tickTracker = new BukkitRunnable() {
      @Override
      public void run() {
        if(!player.isOnline()) {
          stopRecording(plugin, player, false);
          cancel();
          return;
        }
        session.latestFrame = CutsceneFrame.fromLocation(player.getLocation());
        int currentFrames = recorded.size();
        if(currentFrames >= MAX_RECORDING_FRAMES) {
          player.sendMessage(HalloweenChat.color("<#FFB703>[!] บันทึกครบเวลาสูงสุด (60 วินาที @ 60FPS) ระบบทำการบันทึกให้อัตโนมัติ!"));
          stopRecording(plugin, player, true);
          cancel();
          return;
        }
        if(currentFrames % 12 == 0) {
          double seconds = (double) currentFrames / FPS;
          HalloweenChat.sendActionBar(
            player,
            HalloweenChat.color(
              "<gradient:#D90429:#FF5400><bold>[REC 60FPS]</bold></gradient> <#F4F1DE>ห้อง <#FFB703>"
                + arena.getId()
                + " <#5A4E7C>| <#6EEB83>"
                + String.format(Locale.US, "%.1fs", seconds)
                + " <#B8B0C8>(" + currentFrames + " เฟรม @ 60FPS) <#5A4E7C>| <#FFB703>/mma stopcutscene"
            )
          );
        }
      }
    }.runTaskTimer(plugin, 0L, 1L);

    ScheduledFuture<?> highFreqTask = EXECUTOR.scheduleAtFixedRate(() -> {
      if(recorded.size() < MAX_RECORDING_FRAMES) {
        recorded.add(session.latestFrame);
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
          HalloweenChat.color("<#6EEB83><bold>[SAVED 60FPS]</bold> บันทึก Cutscene ห้อง <#FFB703>" + arena.getId() + " <#6EEB83>สำเร็จ!")
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

    List<CutsceneFrame> cutsceneFrames = new ArrayList<>(arena.getCutsceneManager().getFrames());
    if(cutsceneFrames.isEmpty()) {
      return false;
    }

    Location originalLoc = player.getLocation().clone();
    GameMode originalGameMode = player.getGameMode();
    float originalFlySpeed = player.getFlySpeed();
    boolean originalAllowFlight = player.getAllowFlight();
    boolean originalFlying = player.isFlying();
    World fallbackWorld = arena.getCutsceneManager().getFallbackWorld();

    CutsceneFrame firstFrame = cutsceneFrames.get(0);
    Location startEyeLoc = firstFrame.toEyeLocation(fallbackWorld);
    Location startOverlayLoc = firstFrame.toOverlayLocation(fallbackWorld);
    TextDisplay rig = spawnCameraRig(startEyeLoc);
    TextDisplay overlay = spawnFadeOverlay(startOverlayLoc);

    if(startEyeLoc != null && startEyeLoc.getWorld() != null) {
      player.teleport(startEyeLoc);
    }
    player.setGameMode(GameMode.SPECTATOR);
    player.setFlySpeed(0.0f);
    if(rig != null && rig.isValid()) {
      player.setSpectatorTarget(rig);
    }

    final long startNanos = System.nanoTime();
    final int totalFrames = cutsceneFrames.size();
    final List<Player> viewerList = Collections.singletonList(player);

    BukkitTask syncTask = new BukkitRunnable() {
      @Override
      public void run() {
        if(!player.isOnline()) {
          stopPreview(player);
          cancel();
          return;
        }
        int idx = (int) ((System.nanoTime() - startNanos) / FRAME_INTERVAL_NANOS);
        if(idx >= totalFrames) {
          stopPreview(player);
          cancel();
          return;
        }
        CutsceneFrame frame = cutsceneFrames.get(Math.min(idx, totalFrames - 1));
        Location eyeLoc = frame.toEyeLocation(fallbackWorld);
        if(eyeLoc != null && eyeLoc.getWorld() != null) {
          if(rig != null && rig.isValid()) {
            rig.teleport(eyeLoc);
            if(player.getGameMode() != GameMode.SPECTATOR) {
              player.setGameMode(GameMode.SPECTATOR);
            }
            if(player.getSpectatorTarget() != rig) {
              player.setSpectatorTarget(rig);
            }
          } else {
            player.teleport(frame.toLocation(fallbackWorld));
          }
          updateFadeOverlayTick(overlay, frame, fallbackWorld, idx, totalFrames);
        }
        double remaining = (double) (totalFrames - idx) / FPS;
        HalloweenChat.sendActionBar(
          player,
          HalloweenChat.color(
            "<gradient:#9D4EDD:#FF7518><bold>[PREVIEW 60FPS]</bold></gradient> <#F4F1DE>ห้อง <#FFB703>"
              + arena.getId()
              + " <#5A4E7C>| <#6EEB83>เหลือ "
              + String.format(Locale.US, "%.1fs", remaining)
          )
        );
      }
    }.runTaskTimer(plugin, 0L, 1L);

    ScheduledFuture<?> highFreqFuture = EXECUTOR.scheduleAtFixedRate(() -> {
      if(!player.isOnline()) {
        return;
      }
      int idx = (int) ((System.nanoTime() - startNanos) / FRAME_INTERVAL_NANOS);
      if(idx >= totalFrames) {
        Bukkit.getScheduler().runTask(plugin, () -> stopPreview(player));
        return;
      }
      CutsceneFrame frame = cutsceneFrames.get(idx);
      if(rig != null) {
        PacketCameraSender.sendEntityPosition60Fps(rig, frame, viewerList);
      }
      if(overlay != null && computeFadeAlpha(idx, totalFrames) > 0) {
        PacketCameraSender.sendOverlayPosition60Fps(overlay, frame, viewerList);
      }
    }, 0L, FRAME_INTERVAL_NANOS, TimeUnit.NANOSECONDS);

    activePreviews.put(
      player.getUniqueId(),
      new PreviewSession(originalLoc, originalGameMode, originalFlySpeed, originalAllowFlight, originalFlying, syncTask, highFreqFuture, rig, overlay)
    );
    return true;
  }

  public static boolean stopPreview(Player player) {
    if(player == null) {
      return false;
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
    if(player.isOnline()) {
      if(player.getGameMode() == GameMode.SPECTATOR && player.getSpectatorTarget() != null) {
        player.setSpectatorTarget(null);
      }
      player.setFlySpeed(session.originalFlySpeed);
      player.setGameMode(session.originalGameMode);
      player.setAllowFlight(session.originalAllowFlight);
      if(session.originalAllowFlight) {
        player.setFlying(session.originalFlying);
      }
      if(session.originalLoc != null && session.originalLoc.getWorld() != null) {
        player.teleport(session.originalLoc);
      }
      applyPostCutsceneFadeIn(player);
      HalloweenChat.sendActionBar(player, HalloweenChat.color("<#6EEB83>จบการเล่นตัวอย่าง Cutscene แล้ว"));
    }
    if(session.rig != null) {
      try {
        if(session.rig.isValid()) {
          session.rig.remove();
        }
      } catch(Throwable ignored) {
      }
    }
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

  public static class RecordingSession {
    private final Arena arena;
    private final List<CutsceneFrame> frames;
    private volatile CutsceneFrame latestFrame;
    private BukkitTask tickTask;
    private ScheduledFuture<?> captureFuture;

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

  private static class PreviewSession {
    private final Location originalLoc;
    private final GameMode originalGameMode;
    private final float originalFlySpeed;
    private final boolean originalAllowFlight;
    private final boolean originalFlying;
    private final BukkitTask syncTask;
    private final ScheduledFuture<?> highFreqFuture;
    private final TextDisplay rig;
    private final TextDisplay overlay;

    private PreviewSession(Location originalLoc, GameMode originalGameMode, float originalFlySpeed, boolean originalAllowFlight, boolean originalFlying, BukkitTask syncTask, ScheduledFuture<?> highFreqFuture, TextDisplay rig, TextDisplay overlay) {
      this.originalLoc = originalLoc;
      this.originalGameMode = originalGameMode;
      this.originalFlySpeed = originalFlySpeed;
      this.originalAllowFlight = originalAllowFlight;
      this.originalFlying = originalFlying;
      this.syncTask = syncTask;
      this.highFreqFuture = highFreqFuture;
      this.rig = rig;
      this.overlay = overlay;
    }
  }

  private static final class PacketCameraSender {
    private static boolean initialized = false;
    private static boolean supported = false;
    private static Method craftPlayerGetHandle;
    private static Field serverPlayerConnectionField;
    private static Method packetSendMethod;
    private static Constructor<?> vec3Constructor;
    private static Object zeroVec3;
    private static Constructor<?> posMoveRotConstructor;
    private static Constructor<?> syncPacketConstructor;
    private static Constructor<?> rotateHeadConstructor;
    private static Method craftEntityGetHandle;

    private static synchronized void init() {
      if(initialized) {
        return;
      }
      initialized = true;
      try {
        Class<?> vec3Class = Class.forName("net.minecraft.world.phys.Vec3");
        vec3Constructor = vec3Class.getConstructor(double.class, double.class, double.class);
        zeroVec3 = vec3Constructor.newInstance(0.0, 0.0, 0.0);

        Class<?> posMoveRotClass = Class.forName("net.minecraft.world.entity.PositionMoveRotation");
        posMoveRotConstructor = posMoveRotClass.getConstructor(vec3Class, vec3Class, float.class, float.class);

        Class<?> syncPacketClass = Class.forName("net.minecraft.network.protocol.game.ClientboundEntityPositionSyncPacket");
        syncPacketConstructor = syncPacketClass.getConstructor(int.class, posMoveRotClass, boolean.class);

        Class<?> entityClass = Class.forName("net.minecraft.world.entity.Entity");
        Class<?> rotateHeadClass = Class.forName("net.minecraft.network.protocol.game.ClientboundRotateHeadPacket");
        rotateHeadConstructor = rotateHeadClass.getConstructor(entityClass, byte.class);

        Class<?> packetClass = Class.forName("net.minecraft.network.protocol.Packet");
        Class<?> commonListenerClass = Class.forName("net.minecraft.server.network.ServerCommonPacketListenerImpl");
        packetSendMethod = commonListenerClass.getMethod("send", packetClass);

        Class<?> serverPlayerClass = Class.forName("net.minecraft.server.level.ServerPlayer");
        serverPlayerConnectionField = serverPlayerClass.getField("connection");

        supported = true;
      } catch(Throwable ignored) {
        supported = false;
      }
    }

    static void sendEntityPosition60Fps(TextDisplay rig, CutsceneFrame frame, List<Player> viewers) {
      if(rig == null || frame == null || viewers == null || viewers.isEmpty()) {
        return;
      }
      sendPositionPacket(rig, frame.x, frame.y + PLAYER_EYE_OFFSET, frame.z, frame.yaw, frame.pitch, viewers);
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
        viewers
      );
    }

    private static void sendPositionPacket(TextDisplay entity, double x, double y, double z, float yaw, float pitch, List<Player> viewers) {
      init();
      if(!supported) {
        return;
      }
      try {
        int entityId = entity.getEntityId();
        Object posVec = vec3Constructor.newInstance(x, y, z);
        Object posMoveRot = posMoveRotConstructor.newInstance(posVec, zeroVec3, yaw, pitch);
        Object syncPacket = syncPacketConstructor.newInstance(entityId, posMoveRot, false);

        if(craftEntityGetHandle == null) {
          craftEntityGetHandle = entity.getClass().getMethod("getHandle");
        }
        Object nmsEntity = craftEntityGetHandle.invoke(entity);
        byte headYawByte = (byte) ((int) (yaw * 256.0f / 360.0f));
        Object headPacket = rotateHeadConstructor.newInstance(nmsEntity, headYawByte);

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
            packetSendMethod.invoke(connection, headPacket);
          }
        }
      } catch(Throwable ignored) {
      }
    }
  }
}
