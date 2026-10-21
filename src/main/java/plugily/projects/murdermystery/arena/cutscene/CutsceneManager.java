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
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import plugily.projects.minigamesbox.api.arena.IArenaState;
import plugily.projects.minigamesbox.classic.PluginMain;
import plugily.projects.minigamesbox.classic.utils.configuration.ConfigUtils;
import plugily.projects.minigamesbox.classic.utils.version.VersionUtils;
import plugily.projects.murdermystery.Main;
import plugily.projects.murdermystery.arena.Arena;
import plugily.projects.murdermystery.utils.HalloweenChat;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public class CutsceneManager {

  private static final int MAX_RECORDING_TICKS = 20 * 60;
  private static final Map<UUID, RecordingSession> activeRecordings = new HashMap<>();
  private static final Map<UUID, PreviewSession> activePreviews = new HashMap<>();

  private final Arena arena;
  private final List<CutsceneFrame> frames = new ArrayList<>();
  private BukkitTask playbackTask;
  private boolean playing = false;

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
    return frames.size() / 20.0;
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

    List<Player> participants = new ArrayList<>(arena.getPlayersLeft());
    Location initialLoc = frames.get(0).toLocation(fallbackWorld);

    for(Player player : participants) {
      if(player == null || !player.isOnline()) {
        continue;
      }
      preparePlayerForCutscene(player, participants, initialLoc);
      VersionUtils.sendTitles(
        player,
        HalloweenChat.color("<gradient:#9D4EDD:#FF7518><bold>" + arena.getMapName() + "</bold></gradient>"),
        HalloweenChat.color("<#B8B0C8>โปรดระวังคนร้ายที่แฝงตัวอยู่ในกลุ่มของคุณ..."),
        10,
        50,
        15
      );
      player.playSound(player.getLocation(), Sound.BLOCK_PORTAL_TRIGGER, 0.5f, 1.2f);
    }

    playbackTask = new BukkitRunnable() {
      private int tickIndex = 0;

      @Override
      public void run() {
        if(arena.getArenaState() != IArenaState.IN_GAME || tickIndex >= frames.size()) {
          finishCutscene(onComplete);
          cancel();
          return;
        }

        CutsceneFrame frame = frames.get(tickIndex);
        Location targetLoc = frame.toLocation(fallbackWorld);
        if(targetLoc != null && targetLoc.getWorld() != null) {
          for(Player player : arena.getPlayersLeft()) {
            if(player == null || !player.isOnline()) {
              continue;
            }
            if(player.getGameMode() != GameMode.SPECTATOR) {
              player.setGameMode(GameMode.SPECTATOR);
            }
            player.setFlySpeed(0.0f);
            player.teleport(targetLoc);
            if(tickIndex % 10 == 0) {
              int remainingSec = Math.max(1, (int) Math.ceil((frames.size() - tickIndex) / 20.0));
              VersionUtils.sendActionBar(
                player,
                HalloweenChat.color("<gradient:#9D4EDD:#FF7518><bold>[ฉากเปิดตัว / Cutscene]</bold></gradient> <#B8B0C8>เกมกำลังจะเริ่มใน <#FFB703>" + remainingSec + "s")
              );
            }
          }
        }
        tickIndex++;
      }
    }.runTaskTimer(plugin, 0L, 1L);
  }

  private void finishCutscene(Runnable onComplete) {
    if(playbackTask != null) {
      playbackTask.cancel();
      playbackTask = null;
    }
    boolean wasPlaying = playing;
    playing = false;

    if(wasPlaying) {
      for(Player player : arena.getPlayers()) {
        if(player != null && player.isOnline()) {
          restorePlayerFromCutscene(player);
        }
      }
    }

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
    if(playbackTask != null) {
      playbackTask.cancel();
      playbackTask = null;
    }
    if(playing) {
      playing = false;
      for(Player player : arena.getPlayers()) {
        if(player != null && player.isOnline()) {
          restorePlayerFromCutscene(player);
        }
      }
    }
  }

  private void preparePlayerForCutscene(Player player, List<Player> allPlayers, Location startLoc) {
    Main plugin = arena.getPlugin();
    for(Player other : allPlayers) {
      if(other != null && !other.equals(player)) {
        VersionUtils.hidePlayer(plugin, player, other);
      }
    }
    if(startLoc != null && startLoc.getWorld() != null) {
      VersionUtils.teleport(player, startLoc);
    }
    player.setGameMode(GameMode.SPECTATOR);
    player.setFlySpeed(0.0f);
    player.setWalkSpeed(0.0f);
  }

  private void restorePlayerFromCutscene(Player player) {
    Main plugin = arena.getPlugin();
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

  public static void startRecording(PluginMain plugin, Player player, Arena arena) {
    stopRecording(plugin, player, false);
    stopPreview(player);

    List<CutsceneFrame> recorded = new ArrayList<>();
    BukkitTask task = new BukkitRunnable() {
      @Override
      public void run() {
        if(!player.isOnline()) {
          stopRecording(plugin, player, false);
          cancel();
          return;
        }
        if(recorded.size() >= MAX_RECORDING_TICKS) {
          player.sendMessage(HalloweenChat.color("<#FFB703>[!] บันทึกครบเวลาสูงสุด (60 วินาที) ระบบทำการบันทึกให้อัตโนมัติ!"));
          stopRecording(plugin, player, true);
          cancel();
          return;
        }

        Location loc = player.getLocation();
        recorded.add(CutsceneFrame.fromLocation(loc));

        if(recorded.size() % 4 == 0) {
          double seconds = recorded.size() / 20.0;
          VersionUtils.sendActionBar(
            player,
            HalloweenChat.color(
              "<gradient:#D90429:#FF5400><bold>[REC]</bold></gradient> <#F4F1DE>กำลังบันทึก Cutscene ห้อง <#FFB703>"
                + arena.getId()
                + " <#5A4E7C>| <#6EEB83>"
                + String.format(Locale.US, "%.1fs", seconds)
                + " <#B8B0C8>(" + recorded.size() + " เฟรม) <#5A4E7C>| <#FFB703>/mma stopcutscene"
            )
          );
        }
      }
    }.runTaskTimer(plugin, 0L, 1L);

    activeRecordings.put(player.getUniqueId(), new RecordingSession(arena, recorded, task));
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
    if(session.task != null) {
      session.task.cancel();
    }

    if(save && !session.frames.isEmpty()) {
      Arena arena = session.arena;
      arena.getCutsceneManager().setFrames(session.frames);

      FileConfiguration config = ConfigUtils.getConfig(plugin, "arenas");
      config.set("instances." + arena.getId() + ".cutscene", serializeFrames(session.frames));
      ConfigUtils.saveConfig(plugin, config, "arenas");

      if(player.isOnline()) {
        player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.2f);
        VersionUtils.sendActionBar(
          player,
          HalloweenChat.color("<#6EEB83><bold>[SAVED]</bold> บันทึก Cutscene ห้อง <#FFB703>" + arena.getId() + " <#6EEB83>สำเร็จ!")
        );
      }
    }
    return new RecordingResult(session.arena, session.frames.size());
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

    player.setGameMode(GameMode.SPECTATOR);
    player.setFlySpeed(0.0f);

    BukkitTask task = new BukkitRunnable() {
      private int idx = 0;

      @Override
      public void run() {
        if(!player.isOnline() || idx >= cutsceneFrames.size()) {
          stopPreview(player);
          cancel();
          return;
        }
        Location loc = cutsceneFrames.get(idx).toLocation(fallbackWorld);
        if(loc != null && loc.getWorld() != null) {
          player.teleport(loc);
        }
        if(idx % 5 == 0) {
          double remaining = (cutsceneFrames.size() - idx) / 20.0;
          VersionUtils.sendActionBar(
            player,
            HalloweenChat.color(
              "<gradient:#9D4EDD:#FF7518><bold>[PREVIEW]</bold></gradient> <#F4F1DE>กำลังเล่นตัวอย่าง Cutscene ห้อง <#FFB703>"
                + arena.getId()
                + " <#5A4E7C>| <#6EEB83>เหลือ "
                + String.format(Locale.US, "%.1fs", remaining)
            )
          );
        }
        idx++;
      }
    }.runTaskTimer(plugin, 0L, 1L);

    activePreviews.put(
      player.getUniqueId(),
      new PreviewSession(originalLoc, originalGameMode, originalFlySpeed, originalAllowFlight, originalFlying, task)
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
    if(session.task != null) {
      session.task.cancel();
    }
    if(player.isOnline()) {
      player.setFlySpeed(session.originalFlySpeed);
      player.setGameMode(session.originalGameMode);
      player.setAllowFlight(session.originalAllowFlight);
      if(session.originalAllowFlight) {
        player.setFlying(session.originalFlying);
      }
      if(session.originalLoc != null && session.originalLoc.getWorld() != null) {
        player.teleport(session.originalLoc);
      }
      VersionUtils.sendActionBar(player, HalloweenChat.color("<#6EEB83>จบการเล่นตัวอย่าง Cutscene แล้ว"));
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
    private final BukkitTask task;

    public RecordingSession(Arena arena, List<CutsceneFrame> frames, BukkitTask task) {
      this.arena = arena;
      this.frames = frames;
      this.task = task;
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
      return frameCount / 20.0;
    }
  }

  private static class PreviewSession {
    private final Location originalLoc;
    private final GameMode originalGameMode;
    private final float originalFlySpeed;
    private final boolean originalAllowFlight;
    private final boolean originalFlying;
    private final BukkitTask task;

    private PreviewSession(Location originalLoc, GameMode originalGameMode, float originalFlySpeed, boolean originalAllowFlight, boolean originalFlying, BukkitTask task) {
      this.originalLoc = originalLoc;
      this.originalGameMode = originalGameMode;
      this.originalFlySpeed = originalFlySpeed;
      this.originalAllowFlight = originalAllowFlight;
      this.originalFlying = originalFlying;
      this.task = task;
    }
  }
}

