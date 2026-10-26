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

import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;
import plugily.projects.minigamesbox.api.arena.IArenaState;
import plugily.projects.minigamesbox.api.user.IUser;
import plugily.projects.minigamesbox.classic.handlers.language.MessageBuilder;
import plugily.projects.minigamesbox.classic.utils.version.VersionUtils;
import plugily.projects.minigamesbox.classic.utils.version.xseries.XSound;
import plugily.projects.murdermystery.arena.Arena;
import plugily.projects.murdermystery.arena.ArenaUtils;
import plugily.projects.murdermystery.arena.role.Role;
import plugily.projects.murdermystery.arena.task.minigame.TaskMinigameManager;
import plugily.projects.murdermystery.utils.HalloweenChat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

public class TaskManager {

  public static class TaskStationDisplay {
    private final Location location;
    private final Set<TaskType> taskTypes;
    private final TextDisplay activeHologram;
    private final TextDisplay completedHologram;
    private final BlockDisplay glowingBlock;

    public TaskStationDisplay(Location location, Set<TaskType> taskTypes,
                              TextDisplay activeHologram, TextDisplay completedHologram,
                              BlockDisplay glowingBlock) {
      this.location = location;
      this.taskTypes = taskTypes;
      this.activeHologram = activeHologram;
      this.completedHologram = completedHologram;
      this.glowingBlock = glowingBlock;
    }

    public Location getLocation() {
      return location;
    }

    public Set<TaskType> getTaskTypes() {
      return taskTypes;
    }

    public TextDisplay getActiveHologram() {
      return activeHologram;
    }

    public TextDisplay getCompletedHologram() {
      return completedHologram;
    }

    public BlockDisplay getGlowingBlock() {
      return glowingBlock;
    }

    public void remove() {
      if(activeHologram != null && activeHologram.isValid()) {
        try {
          activeHologram.remove();
        } catch(Throwable ignored) {
        }
      }
      if(completedHologram != null && completedHologram.isValid()) {
        try {
          completedHologram.remove();
        } catch(Throwable ignored) {
        }
      }
      if(glowingBlock != null && glowingBlock.isValid()) {
        try {
          glowingBlock.remove();
        } catch(Throwable ignored) {
        }
      }
    }
  }

  private final Arena arena;
  private final Map<Player, List<GameTask>> playerTasks = new HashMap<>();
  private final List<TaskStationDisplay> stationDisplays = new ArrayList<>();
  private final TaskMinigameManager minigameManager;
  private final VisualTaskEffectManager visualEffectManager;
  private BossBar taskBossBar;
  private final Set<Player> bossBarViewers = new HashSet<>();
  private BukkitTask arrowTrackerTask;
  private int totalTasks = 0;
  private int completedTasks = 0;
  private boolean allTasksCompleted = false;
  private boolean taskWinTriggered = false;
  private int taskRoundId = 0;

  public TaskManager(Arena arena) {
    this.arena = arena;
    this.minigameManager = new TaskMinigameManager(arena);
    this.visualEffectManager = new VisualTaskEffectManager(arena);
  }

  public int getTaskRoundId() {
    return taskRoundId;
  }

  public boolean isNearAssignedTask(Player player, TaskType taskType, double maxDistSq) {
    if(player == null || taskType == null) {
      return false;
    }
    List<GameTask> tasks = playerTasks.get(player);
    if(tasks == null || tasks.isEmpty()) {
      return false;
    }
    for(GameTask task : tasks) {
      if(!task.isCompleted() && task.getTaskType() == taskType) {
        Location loc = task.getLocation();
        if(loc != null && loc.getWorld() != null && loc.getWorld().equals(player.getWorld())) {
          if(player.getLocation().distanceSquared(loc.clone().add(0.5, 0.5, 0.5)) <= maxDistSq) {
            return true;
          }
        }
      }
    }
    return false;
  }

  public VisualTaskEffectManager getVisualEffectManager() {
    return visualEffectManager;
  }

  private static Location toBlockLocation(Location loc) {
    if(loc == null || loc.getWorld() == null) {
      return loc;
    }
    return new Location(loc.getWorld(), loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
  }

  private static boolean isSameBlock(Location a, Location b) {
    if(a == null || b == null || a.getWorld() == null || b.getWorld() == null) {
      return false;
    }
    return a.getWorld().equals(b.getWorld())
      && a.getBlockX() == b.getBlockX()
      && a.getBlockY() == b.getBlockY()
      && a.getBlockZ() == b.getBlockZ();
  }

  public void assignTasks(List<Player> innocents, List<Location> taskLocations, int tasksPerPlayer) {
    taskRoundId++;
    playerTasks.clear();
    visualEffectManager.cleanup();
    for(TaskStationDisplay station : stationDisplays) {
      if(station.getGlowingBlock() != null) {
        arena.removeEntityGlowColor(station.getGlowingBlock());
      }
      station.remove();
    }
    stationDisplays.clear();
    completedTasks = 0;
    allTasksCompleted = false;
    taskWinTriggered = false;

    List<Location> genericLocations = arena.getTaskLocations();
    boolean hasGenericLocations = !genericLocations.isEmpty();

    List<TaskType> assignableTypes = new ArrayList<>();
    for(TaskType type : TaskType.getValues()) {
      if(hasGenericLocations || !arena.getTaskLocations(type).isEmpty()) {
        assignableTypes.add(type);
      }
    }

    if(assignableTypes.isEmpty()) {
      arena.getPlugin().getDebugger().debug("No task locations configured for arena {0}, skipping task assignment", arena.getId());
      return;
    }

    Random random = new Random();
    List<Set<TaskType>> assignedSets = new ArrayList<>();

    for(Player player : innocents) {
      List<TaskType> pool = new ArrayList<>(assignableTypes);
      Collections.shuffle(pool, random);

      int count = Math.max(1, Math.min(tasksPerPlayer, pool.size()));
      Set<TaskType> playerTypes = new LinkedHashSet<>(pool.subList(0, count));

      int attempts = 0;
      while(attempts < 15 && assignedSets.contains(playerTypes)) {
        Collections.shuffle(pool, random);
        playerTypes = new LinkedHashSet<>(pool.subList(0, count));
        attempts++;
      }
      assignedSets.add(playerTypes);

      List<Location> shuffledGeneric = new ArrayList<>(genericLocations);
      Collections.shuffle(shuffledGeneric, random);

      List<GameTask> tasks = new ArrayList<>();
      int genericIdx = 0;
      for(TaskType type : playerTypes) {
        Location taskLoc = null;
        List<Location> specificLocs = arena.getTaskLocations(type);
        if(!specificLocs.isEmpty()) {
          taskLoc = toBlockLocation(specificLocs.get(random.nextInt(specificLocs.size())));
        } else if(!shuffledGeneric.isEmpty()) {
          for(int attempt = 0; attempt < shuffledGeneric.size(); attempt++) {
            Location candidate = toBlockLocation(shuffledGeneric.get((genericIdx + attempt) % shuffledGeneric.size()));
            boolean alreadyUsed = false;
            for(GameTask existing : tasks) {
              if(isSameBlock(existing.getLocation(), candidate)) {
                alreadyUsed = true;
                break;
              }
            }
            if(!alreadyUsed) {
              taskLoc = candidate;
              genericIdx += attempt + 1;
              break;
            }
          }
          if(taskLoc == null) {
            taskLoc = toBlockLocation(shuffledGeneric.get(genericIdx % shuffledGeneric.size()));
            genericIdx++;
          }
        }
        if(taskLoc != null) {
          tasks.add(new GameTask(type, taskLoc));
        }
      }
      playerTasks.put(player, tasks);

      sendPlayerTaskList(player);
      giveTaskPointer(player);
    }

    totalTasks = playerTasks.values().stream().mapToInt(List::size).sum();
    createBossBar();
    startArrowTracker();

    Map<Location, Set<TaskType>> locationTasks = new LinkedHashMap<>();

    for(Map.Entry<TaskType, List<Location>> entry : arena.getSpecificTaskLocations().entrySet()) {
      for(Location loc : entry.getValue()) {
        if(loc != null && loc.getWorld() != null) {
          locationTasks.computeIfAbsent(toBlockLocation(loc), k -> new LinkedHashSet<>()).add(entry.getKey());
        }
      }
    }

    for(List<GameTask> pTasks : playerTasks.values()) {
      for(GameTask gt : pTasks) {
        Location loc = gt.getLocation();
        if(loc != null && loc.getWorld() != null) {
          locationTasks.computeIfAbsent(toBlockLocation(loc), k -> new LinkedHashSet<>()).add(gt.getTaskType());
        }
      }
    }

    for(Location loc : genericLocations) {
      if(loc != null && loc.getWorld() != null) {
        locationTasks.computeIfAbsent(toBlockLocation(loc), k -> new LinkedHashSet<>());
      }
    }

    for(Map.Entry<Location, Set<TaskType>> entry : locationTasks.entrySet()) {
      Location loc = entry.getKey();
      Set<TaskType> types = entry.getValue();
      Location displayLoc = loc.clone().add(0.5, 1.4, 0.5);

      String activeText;
      String completedText;
      if(types.isEmpty()) {
        activeText = "<shadow:#000000FF><#5A4E7C>[</shadow><#FF7518>Task<shadow:#000000FF><#5A4E7C>]</shadow>\n\n<#F4F1DE>จุดทำภารกิจ\n\n<shadow:#000000FF><#5A4E7C>(</shadow><#B8B0C8>กดคลิกขวาเพื่อทำภารกิจ<shadow:#000000FF><#5A4E7C>)</shadow>";
        completedText = "<shadow:#000000FF><#5A4E7C>[</shadow><#6EEB83>Task<shadow:#000000FF><#5A4E7C>]</shadow>\n\n<#6EEB83>ทำภารกิจเสร็จแล้ว\n\n<shadow:#000000FF><#5A4E7C>(</shadow><#B8B0C8>จุดทำภารกิจนี้เสร็จสิ้นแล้ว<shadow:#000000FF><#5A4E7C>)</shadow>";
      } else if(types.size() == 1) {
        TaskType t = types.iterator().next();
        activeText = "<shadow:#000000FF><#5A4E7C>[</shadow><#FF7518>Task<shadow:#000000FF><#5A4E7C>]</shadow>\n\n" + t.getHologramTitle() + "\n\n<shadow:#000000FF><#5A4E7C>(</shadow><#B8B0C8>กดคลิกขวาเพื่อทำภารกิจ<shadow:#000000FF><#5A4E7C>)</shadow>";
        completedText = "<shadow:#000000FF><#5A4E7C>[</shadow><#6EEB83>Task<shadow:#000000FF><#5A4E7C>]</shadow>\n\n<#B8B0C8>" + t.getDisplayName() + "\n<#6EEB83>ทำภารกิจเสร็จแล้ว";
      } else {
        StringBuilder sbActive = new StringBuilder("<shadow:#000000FF><#5A4E7C>[</shadow><#FF7518>Task<shadow:#000000FF><#5A4E7C>]</shadow>\n\n");
        StringBuilder sbDone = new StringBuilder("<shadow:#000000FF><#5A4E7C>[</shadow><#6EEB83>Task<shadow:#000000FF><#5A4E7C>]</shadow>\n\n");
        int count = 0;
        for(TaskType t : types) {
          if(count > 0) {
            sbActive.append("\n");
            sbDone.append("\n");
          }
          sbActive.append("<#F4F1DE>").append(t.getDisplayName());
          sbDone.append("<#B8B0C8>").append(t.getDisplayName());
          count++;
        }
        sbActive.append("\n\n<shadow:#000000FF><#5A4E7C>(</shadow><#B8B0C8>กดคลิกขวาเพื่อทำภารกิจ<shadow:#000000FF><#5A4E7C>)</shadow>");
        sbDone.append("\n<#6EEB83>ทำภารกิจเสร็จแล้ว");
        activeText = sbActive.toString();
        completedText = sbDone.toString();
      }

      arena.removeNearbyDisplays(displayLoc, 1.2, 1.8, 1.2, false);
      TextDisplay activeHologram = arena.spawnTextDisplay(displayLoc, activeText, false);
      TextDisplay completedHologram = arena.spawnTextDisplay(displayLoc, completedText, false);
      BlockDisplay glowingBlock = arena.spawnBlockDisplay(loc, null, ChatColor.GOLD);

      stationDisplays.add(new TaskStationDisplay(loc, types, activeHologram, completedHologram, glowingBlock));
    }

    updateAllPlayersStationVisibility();

    arena.getPlugin().getDebugger().debug("Assigned {0} total tasks across {1} players in arena {2}",
      totalTasks, innocents.size(), arena.getId());
  }

  public void giveTaskPointer(Player player) {
    try {
      ItemStack compass = new ItemStack(Material.COMPASS);
      ItemMeta meta = compass.getItemMeta();
      if(meta != null) {
        HalloweenChat.setItemName(meta, "<shadow:#000000FF><#5A4E7C>[</shadow><#FF7518>Task Compass<shadow:#000000FF><#5A4E7C>]</shadow>");
        HalloweenChat.setItemLore(meta,
          "<#F4F1DE>ชี้ไปยังจุดทำภารกิจถัดไปของคุณ",
          "<shadow:#000000FF><#5A4E7C>(</shadow><#B8B0C8>เดินตามเข็มทิศแล้วกดคลิกขวาที่จุดภารกิจ<shadow:#000000FF><#5A4E7C>)</shadow>"
        );
        compass.setItemMeta(meta);
      }
      int targetSlot = 8;
      ItemStack existing = player.getInventory().getItem(8);
      if(existing != null && existing.getType() != Material.AIR && !isTaskCompass(existing)) {
        targetSlot = 7;
      }
      player.getInventory().setItem(targetSlot, compass);
    } catch(Exception ignored) {
    }
  }

  public boolean isTaskCompass(ItemStack item) {
    if(item == null || item.getType() != Material.COMPASS || !item.hasItemMeta() || !item.getItemMeta().hasDisplayName()) {
      return false;
    }
    return HalloweenChat.strip(item.getItemMeta().getDisplayName()).contains("Task Compass");
  }

  public boolean hasAssignedTasks(Player player) {
    if(player == null) {
      return false;
    }
    List<GameTask> tasks = playerTasks.get(player);
    return tasks != null && !tasks.isEmpty();
  }

  public void sendPlayerTaskList(Player player) {
    List<GameTask> tasks = playerTasks.get(player);
    if(tasks == null || tasks.isEmpty()) {
      return;
    }

    player.sendMessage(" ");
    HalloweenChat.send(player, "<shadow:#000000FF><#5A4E7C>[</shadow><#FF7518>Tasks<shadow:#000000FF><#5A4E7C>]</shadow>");
    for(GameTask task : tasks) {
      String taskName = task.getTaskType().getDisplayName();
      String visualTag = (task.getTaskType().isVisual() && arena.isVisualTasksEnabled())
        ? " <shadow:#000000FF><#5A4E7C>(</shadow><#72EFDD>ยืนยันตัวตน<shadow:#000000FF><#5A4E7C>)</shadow>"
        : "";
      if(task.isCompleted()) {
        HalloweenChat.send(player, "<shadow:#000000FF><#5A4E7C>- </shadow><#6EEB83>" + taskName + visualTag + " <shadow:#000000FF><#5A4E7C>(</shadow><#6EEB83>เสร็จแล้ว<shadow:#000000FF><#5A4E7C>)</shadow>");
      } else {
        HalloweenChat.send(player, "<shadow:#000000FF><#5A4E7C>- </shadow><#F4F1DE>" + taskName + visualTag);
      }
    }
    HalloweenChat.send(player, "<shadow:#000000FF><#5A4E7C>(</shadow><#B8B0C8>เดินตามเข็มทิศหรือแผนที่ไปกดคลิกขวาที่จุดภารกิจ<shadow:#000000FF><#5A4E7C>)</shadow>");
    player.sendMessage(" ");
  }

  public boolean tryCompleteTask(Player player) {
    List<GameTask> tasks = playerTasks.get(player);
    if(tasks == null || tasks.isEmpty()) {
      return false;
    }

    double range = arena.getPlugin().getConfig().getDouble("Tasks.Complete-Range", 3.0);

    for(GameTask task : tasks) {
      if(!task.isCompleted() && task.isInRange(player.getLocation(), range)) {
        task.setCompleted(true);
        completedTasks++;
        updateBossBar();
        visualEffectManager.completeVisualSession(player, task.getTaskType());

        String taskName = task.getTaskType().getDisplayName();

        HalloweenChat.send(player, new MessageBuilder("IN_GAME_MESSAGES_ARENA_TASK_COMPLETED")
          .asKey().player(player).arena(arena)
          .value(taskName)
          .build());

        int remaining = getPlayerTotalTasks(player) - getPlayerCompletedTasks(player);
        if(remaining > 0) {
          HalloweenChat.send(player, "<shadow:#000000FF><#5A4E7C>[</shadow><#FF7518>Task<shadow:#000000FF><#5A4E7C>]</shadow> <#F4F1DE>เหลืออีก <#FFB703>" + remaining + " <#F4F1DE>ภารกิจ");
          GameTask next = getNextTask(player);
          if(next != null) {
            String nextName = next.getTaskType().getDisplayName();
            HalloweenChat.sendActionBar(player, "<shadow:#000000FF><#5A4E7C>[</shadow><#FF7518>Task<shadow:#000000FF><#5A4E7C>]</shadow> <#B8B0C8>ภารกิจถัดไป <#FFB703>" + nextName);
          }
        } else {
          HalloweenChat.send(player, "<shadow:#000000FF><#5A4E7C>[</shadow><#6EEB83>Task<shadow:#000000FF><#5A4E7C>]</shadow> <#6EEB83>ทำภารกิจส่วนตัวครบแล้ว <shadow:#000000FF><#5A4E7C>(</shadow><#B8B0C8>รอเพื่อนร่วมทีมทำภารกิจให้ครบ<shadow:#000000FF><#5A4E7C>)</shadow>");
          HalloweenChat.sendActionBar(player, "<shadow:#000000FF><#5A4E7C>[</shadow><#6EEB83>Task<shadow:#000000FF><#5A4E7C>]</shadow> <#6EEB83>ทำภารกิจส่วนตัวครบแล้ว");
        }

        updatePlayerStationVisibility(player);

        if(completedTasks >= totalTasks) {
          allTasksCompleted = true;
        }

        return true;
      }
    }

    for(Location loc : arena.getAllTaskLocations()) {
      if(player.getWorld().equals(loc.getWorld()) && player.getLocation().distanceSquared(loc) <= (range * range)) {
        HalloweenChat.sendActionBar(player, "<shadow:#000000FF><#5A4E7C>[</shadow><#FF7518>Task<shadow:#000000FF><#5A4E7C>]</shadow> <#B8B0C8>ไม่มีภารกิจที่จุดนี้");
        break;
      }
    }

    return false;
  }

  public TaskMinigameManager getMinigameManager() {
    return minigameManager;
  }

  public boolean isStationDisplayEntity(Entity entity) {
    return findStationByEntity(entity) != null;
  }

  private TaskStationDisplay findStationByEntity(Entity entity) {
    if(entity == null) {
      return null;
    }
    for(TaskStationDisplay station : stationDisplays) {
      if(entity.equals(station.getActiveHologram())
        || entity.equals(station.getCompletedHologram())
        || entity.equals(station.getGlowingBlock())) {
        return station;
      }
    }
    return null;
  }

  public boolean interactTaskEntity(Player player, Entity entity) {
    TaskStationDisplay station = findStationByEntity(entity);
    if(station == null) {
      return false;
    }
    return interactAtStation(player, station.getLocation());
  }

  public boolean interactTask(Player player) {
    return interactTask(player, null);
  }

  public boolean interactTask(Player player, Location targetLoc) {
    List<GameTask> tasks = playerTasks.get(player);
    if(tasks == null || tasks.isEmpty()) {
      return false;
    }

    if(targetLoc != null && targetLoc.getWorld() != null) {
      Location matchedStation = findStationForClickedBlock(targetLoc);
      if(matchedStation == null) {
        return false;
      }
      return interactAtStation(player, matchedStation);
    }

    double maxDistSq = 7.5625;
    GameTask closestTask = null;
    double closestDistSq = maxDistSq;

    for(GameTask task : tasks) {
      if(task.isCompleted() || task.getLocation() == null || task.getLocation().getWorld() == null) {
        continue;
      }
      if(!task.getLocation().getWorld().equals(player.getWorld())) {
        continue;
      }
      double distSq = task.getLocation().clone().add(0.5, 0.5, 0.5).distanceSquared(player.getLocation());
      if(distSq <= closestDistSq) {
        closestDistSq = distSq;
        closestTask = task;
      }
    }

    if(closestTask != null) {
      return openOrCompleteTask(player, closestTask);
    }

    Location nearestStation = null;
    double nearestStationDistSq = maxDistSq;
    for(Location loc : arena.getAllTaskLocations()) {
      if(loc == null || loc.getWorld() == null || !loc.getWorld().equals(player.getWorld())) {
        continue;
      }
      double distSq = loc.clone().add(0.5, 0.5, 0.5).distanceSquared(player.getLocation());
      if(distSq <= nearestStationDistSq) {
        nearestStationDistSq = distSq;
        nearestStation = loc;
      }
    }

    if(nearestStation != null) {
      return interactAtStation(player, nearestStation);
    }

    return false;
  }

  private Location findStationForClickedBlock(Location targetLoc) {
    List<Location> allStations = arena.getAllTaskLocations();
    for(Location loc : allStations) {
      if(isSameBlock(loc, targetLoc)) {
        return loc;
      }
    }
    for(Location loc : allStations) {
      if(loc != null && loc.getWorld() != null && loc.getWorld().equals(targetLoc.getWorld())) {
        if(loc.getBlockX() == targetLoc.getBlockX()
          && loc.getBlockZ() == targetLoc.getBlockZ()
          && targetLoc.getBlockY() - loc.getBlockY() == 1) {
          return loc;
        }
      }
    }
    return null;
  }

  private boolean interactAtStation(Player player, Location stationLoc) {
    if(stationLoc == null || stationLoc.getWorld() == null || !stationLoc.getWorld().equals(player.getWorld())) {
      return false;
    }
    if(player.getLocation().distanceSquared(stationLoc.clone().add(0.5, 0.5, 0.5)) > 36.0) {
      return false;
    }

    List<GameTask> tasks = playerTasks.get(player);
    if(tasks == null || tasks.isEmpty()) {
      return false;
    }

    for(GameTask task : tasks) {
      if(!task.isCompleted() && isSameBlock(task.getLocation(), stationLoc)) {
        return openOrCompleteTask(player, task);
      }
    }

    for(GameTask task : tasks) {
      if(task.isCompleted() && isSameBlock(task.getLocation(), stationLoc)) {
        HalloweenChat.sendActionBar(player, "<shadow:#000000FF><#5A4E7C>[</shadow><#6EEB83>Task<shadow:#000000FF><#5A4E7C>]</shadow> <#6EEB83>ทำภารกิจจุดนี้เสร็จแล้ว");
        return true;
      }
    }

    HalloweenChat.sendActionBar(player, "<shadow:#000000FF><#5A4E7C>[</shadow><#FF7518>Task<shadow:#000000FF><#5A4E7C>]</shadow> <#B8B0C8>ไม่มีภารกิจที่จุดนี้");
    return true;
  }

  private boolean openOrCompleteTask(Player player, GameTask task) {
    if(minigameManager.hasMinigame(task.getTaskType())) {
      boolean opened = minigameManager.openMinigame(player, task.getTaskType());
      if(opened && task.getTaskType().isVisual() && !ArenaUtils.isSpectator(player, arena)) {
        visualEffectManager.startVisualSession(player, task.getTaskType(), task.getLocation());
      }
      return opened;
    }
    return completeTaskFromMinigame(player, task.getTaskType());
  }

  public boolean completeTaskFromMinigame(Player player, TaskType taskType) {
    if(arena.getArenaState() != IArenaState.IN_GAME
      || arena.getVotingManager().isVotingActive()
      || arena.getCutsceneManager().isPlaying()) {
      return false;
    }
    IUser user = arena.getPlugin().getUserManager().getUser(player);
    if(Role.isRole(Role.MURDERER, user, arena)) {
      return false;
    }
    if(minigameManager.hasMinigame(taskType) && !minigameManager.isValidCompletionRound(player)) {
      return false;
    }
    List<GameTask> tasks = playerTasks.get(player);
    if(tasks == null || tasks.isEmpty()) {
      return false;
    }

    for(GameTask task : tasks) {
      if(!task.isCompleted() && task.getTaskType() == taskType) {
        task.setCompleted(true);
        completedTasks++;
        updateBossBar();
        if(!ArenaUtils.isSpectator(player, arena)) {
          visualEffectManager.completeVisualSession(player, task.getTaskType());
        } else {
          visualEffectManager.stopVisualSession(player);
        }

        String taskName = task.getTaskType().getDisplayName();
        HalloweenChat.send(player, new MessageBuilder("IN_GAME_MESSAGES_ARENA_TASK_COMPLETED")
          .asKey().player(player).arena(arena)
          .value(taskName)
          .build());

        if(ArenaUtils.isSpectator(player, arena)) {
          player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
        } else {
          XSound.ENTITY_EXPERIENCE_ORB_PICKUP.play(player.getLocation(), 1F, 1F);
          VersionUtils.sendParticles("VILLAGER_HAPPY", player, player.getLocation().add(0, 1, 0), 15);
        }

        int remaining = getPlayerTotalTasks(player) - getPlayerCompletedTasks(player);
        if(remaining > 0) {
          HalloweenChat.send(player, "<shadow:#000000FF><#5A4E7C>[</shadow><#FF7518>Task<shadow:#000000FF><#5A4E7C>]</shadow> <#F4F1DE>เหลืออีก <#FFB703>" + remaining + " <#F4F1DE>ภารกิจ");
          GameTask next = getNextTask(player);
          if(next != null) {
            String nextName = next.getTaskType().getDisplayName();
            HalloweenChat.sendActionBar(player, "<shadow:#000000FF><#5A4E7C>[</shadow><#FF7518>Task<shadow:#000000FF><#5A4E7C>]</shadow> <#B8B0C8>ภารกิจถัดไป <#FFB703>" + nextName);
          }
        } else {
          HalloweenChat.send(player, "<shadow:#000000FF><#5A4E7C>[</shadow><#6EEB83>Task<shadow:#000000FF><#5A4E7C>]</shadow> <#6EEB83>ทำภารกิจส่วนตัวครบแล้ว <shadow:#000000FF><#5A4E7C>(</shadow><#B8B0C8>รอเพื่อนร่วมทีมทำภารกิจให้ครบ<shadow:#000000FF><#5A4E7C>)</shadow>");
          HalloweenChat.sendActionBar(player, "<shadow:#000000FF><#5A4E7C>[</shadow><#6EEB83>Task<shadow:#000000FF><#5A4E7C>]</shadow> <#6EEB83>ทำภารกิจส่วนตัวครบแล้ว");
        }

        updatePlayerStationVisibility(player);

        if(completedTasks >= totalTasks) {
          allTasksCompleted = true;
          triggerTaskVictory();
        }

        return true;
      }
    }
    return false;
  }

  public void triggerTaskVictory() {
    if(taskWinTriggered || arena.getArenaState() != IArenaState.IN_GAME) {
      return;
    }
    taskWinTriggered = true;
    arena.setTaskWin(true);
    String allDoneMsg = new MessageBuilder("IN_GAME_MESSAGES_ARENA_TASK_ALL_COMPLETED")
      .asKey().arena(arena).build();
    for(Player p : arena.getPlayers()) {
      HalloweenChat.send(p, allDoneMsg);
      HalloweenChat.sendTitles(p,
        new MessageBuilder("IN_GAME_MESSAGES_ARENA_TASK_WIN_TITLE").asKey().build(),
        new MessageBuilder("IN_GAME_MESSAGES_ARENA_TASK_WIN_SUBTITLE").asKey().build(),
        10, 60, 10);
    }
    Bukkit.getScheduler().runTask(arena.getPlugin(), () -> arena.getPlugin().getArenaManager().stopGame(false, arena));
  }

  public GameTask getNextTask(Player player) {
    List<GameTask> tasks = playerTasks.get(player);
    if(tasks == null) {
      return null;
    }
    for(GameTask task : tasks) {
      if(!task.isCompleted()) {
        return task;
      }
    }
    return null;
  }

  public int getPlayerCompletedTasks(Player player) {
    List<GameTask> tasks = playerTasks.get(player);
    if(tasks == null) {
      return 0;
    }
    return (int) tasks.stream().filter(GameTask::isCompleted).count();
  }

  public int getPlayerTotalTasks(Player player) {
    List<GameTask> tasks = playerTasks.get(player);
    if(tasks == null) {
      return 0;
    }
    return tasks.size();
  }

  public int getTotalTasks() {
    return totalTasks;
  }

  public int getCompletedTasks() {
    return completedTasks;
  }

  public boolean areAllTasksCompleted() {
    return allTasksCompleted && totalTasks > 0;
  }

  public Map<Player, List<GameTask>> getPlayerTasks() {
    return playerTasks;
  }

  private Component formatBossBarTitle() {
    return HalloweenChat.component(
      "<shadow:#000000FF><#5A4E7C>[</shadow><#FF7518>Tasks<shadow:#000000FF><#5A4E7C>]</shadow> <#F4F1DE>ซ่อมรถบัส <#FFB703>"
        + completedTasks
        + "<shadow:#000000FF><#5A4E7C>/</shadow><#FFB703>"
        + totalTasks
    );
  }

  private void hideTaskBossBarAll() {
    if(taskBossBar == null) {
      bossBarViewers.clear();
      return;
    }
    for(Player viewer : new ArrayList<>(bossBarViewers)) {
      if(viewer != null) {
        try {
          viewer.hideBossBar(taskBossBar);
        } catch(Throwable ignored) {
        }
      }
    }
    for(Player player : arena.getPlayers()) {
      if(player != null) {
        try {
          player.hideBossBar(taskBossBar);
        } catch(Throwable ignored) {
        }
      }
    }
    bossBarViewers.clear();
  }

  private void createBossBar() {
    hideTaskBossBarAll();
    taskBossBar = BossBar.bossBar(formatBossBarTitle(), 0.0f, BossBar.Color.PURPLE, BossBar.Overlay.NOTCHED_10);

    for(Player player : arena.getPlayers()) {
      addPlayerToBossBar(player);
    }
    updateBossBar();
  }

  public void updateBossBar() {
    if(taskBossBar == null) {
      return;
    }
    taskBossBar.name(formatBossBarTitle());
    float progress = totalTasks <= 0 ? 0.0f : (float) Math.max(0.0, Math.min(1.0, (double) completedTasks / totalTasks));
    taskBossBar.progress(progress);

    if(progress >= 0.75f) {
      taskBossBar.color(BossBar.Color.GREEN);
    } else if(progress >= 0.5f) {
      taskBossBar.color(BossBar.Color.YELLOW);
    } else {
      taskBossBar.color(BossBar.Color.PURPLE);
    }
  }

  public void addPlayerToBossBar(Player player) {
    if(taskBossBar != null && player != null) {
      try {
        player.showBossBar(taskBossBar);
        bossBarViewers.add(player);
      } catch(Throwable ignored) {
      }
    }
  }

  public void removePlayerFromBossBar(Player player) {
    if(taskBossBar != null && player != null) {
      try {
        player.hideBossBar(taskBossBar);
      } catch(Throwable ignored) {
      }
      bossBarViewers.remove(player);
    }
  }

  public void cleanup() {
    taskRoundId++;
    stopArrowTracker();
    visualEffectManager.cleanup();
    hideTaskBossBarAll();
    taskBossBar = null;
    for(TaskStationDisplay station : stationDisplays) {
      if(station.getGlowingBlock() != null) {
        arena.removeEntityGlowColor(station.getGlowingBlock());
      }
      station.remove();
    }
    stationDisplays.clear();
    minigameManager.cleanup();
    playerTasks.clear();
    totalTasks = 0;
    completedTasks = 0;
    allTasksCompleted = false;
    taskWinTriggered = false;
  }

  public void startArrowTracker() {
    stopArrowTracker();
    arrowTrackerTask = Bukkit.getScheduler().runTaskTimer(arena.getPlugin(), () -> {
      if(arena.getSabotageManager() != null && arena.getSabotageManager().isCommsJammed()) {
        return;
      }
      for(Map.Entry<Player, List<GameTask>> entry : playerTasks.entrySet()) {
        Player player = entry.getKey();
        if(!player.isOnline()) {
          continue;
        }

        GameTask next = getNextTask(player);
        if(next == null) {
          continue;
        }

        Location target = next.getLocation();
        if(target == null || target.getWorld() == null || !target.getWorld().equals(player.getWorld())) {
          continue;
        }

        player.setCompassTarget(target);

        Location eye = player.getEyeLocation();
        Vector toTask = target.clone().add(0.5, 0.5, 0.5).toVector().subtract(eye.toVector());
        double dist = toTask.length();

        Particle dustParticle = null;
        try {
          dustParticle = Particle.valueOf("DUST");
        } catch(Exception e) {
          try {
            dustParticle = Particle.valueOf("REDSTONE");
          } catch(Exception ignored) {
          }
        }

        if(dist > 1.5 && dustParticle != null) {
          Vector dir = toTask.normalize();
          Location p1 = eye.clone().add(dir.clone().multiply(1.2));
          Location p2 = eye.clone().add(dir.clone().multiply(1.8));
          try {
            Particle.DustOptions pumpkin = new Particle.DustOptions(Color.fromRGB(255, 117, 24), 1.2F);
            player.spawnParticle(dustParticle, p1, 1, 0, 0, 0, 0, pumpkin);
            player.spawnParticle(dustParticle, p2, 1, 0, 0, 0, 0, pumpkin);
          } catch(Exception ignored) {
          }
        }

        if(dustParticle != null) {
          try {
            Location blockTop = target.clone().add(0.5, 1.2, 0.5);
            Particle.DustOptions amber = new Particle.DustOptions(Color.fromRGB(255, 183, 3), 1.4F);
            player.spawnParticle(dustParticle, blockTop, 2, 0.1, 0.2, 0.1, 0, amber);
          } catch(Exception ignored) {
          }
        }
      }
    }, 20L, 10L);
  }

  public void stopArrowTracker() {
    if(arrowTrackerTask != null) {
      arrowTrackerTask.cancel();
      arrowTrackerTask = null;
    }
  }

  public List<GameTask> getPlayerTasks(Player player) {
    return playerTasks.get(player);
  }

  public List<GameTask> getTasksForMurderer() {
    List<GameTask> sample = new ArrayList<>();
    for(List<GameTask> list : playerTasks.values()) {
      for(GameTask task : list) {
        boolean exists = sample.stream().anyMatch(t -> t.getTaskType() == task.getTaskType());
        if(!exists) {
          sample.add(task);
        }
        if(sample.size() >= 3) {
          break;
        }
      }
      if(sample.size() >= 3) {
        break;
      }
    }
    return sample;
  }

  public void handlePlayerDeath(Player player) {
    visualEffectManager.stopVisualSession(player);
    minigameManager.handleClose(player, null);
    updateBossBar();
  }

  public void removePlayer(Player player) {
    visualEffectManager.stopVisualSession(player);
    boolean hadActiveMinigame = minigameManager.hasActiveMinigame(player);
    minigameManager.invalidatePlayerRound(player);
    minigameManager.handleClose(player, null);
    if(hadActiveMinigame && player != null && player.isOnline()) {
      player.closeInventory();
    }
    if(player != null && player.isOnline()) {
      for(int i = 0; i < player.getInventory().getSize(); i++) {
        if(isTaskCompass(player.getInventory().getItem(i))) {
          player.getInventory().setItem(i, null);
        }
      }
    }
    List<GameTask> tasks = playerTasks.remove(player);
    if(tasks != null) {
      for(GameTask task : tasks) {
        if(!task.isCompleted() && totalTasks > completedTasks) {
          totalTasks--;
        }
      }
      boolean hasLivingInnocents = !playerTasks.isEmpty() && arena.getPlayersLeft().size() > arena.aliveMurderer();
      if(hasLivingInnocents && totalTasks > 0 && completedTasks >= totalTasks) {
        allTasksCompleted = true;
        if(!arena.getVotingManager().isVotingActive() && !arena.getCutsceneManager().isPlaying()) {
          triggerTaskVictory();
        }
      }
      updateBossBar();
    }
    removePlayerFromBossBar(player);
  }

  public List<TaskStationDisplay> getStationDisplays() {
    return stationDisplays;
  }

  public void updatePlayerStationVisibility(Player player) {
    if(player == null || !player.isOnline()) {
      return;
    }

    boolean commsJammed = arena.getSabotageManager() != null && arena.getSabotageManager().isCommsJammed();
    IUser user = arena.getPlugin().getUserManager().getUser(player);
    boolean isMurderer = user != null && Role.isRole(Role.MURDERER, user, arena);

    if(commsJammed && !isMurderer) {
      for(TaskStationDisplay station : stationDisplays) {
        if(station.getActiveHologram() != null && station.getActiveHologram().isValid()) {
          try {
            player.hideEntity(arena.getPlugin(), station.getActiveHologram());
          } catch(Throwable ignored) {
          }
        }
        if(station.getCompletedHologram() != null && station.getCompletedHologram().isValid()) {
          try {
            player.hideEntity(arena.getPlugin(), station.getCompletedHologram());
          } catch(Throwable ignored) {
          }
        }
        if(station.getGlowingBlock() != null && station.getGlowingBlock().isValid()) {
          try {
            player.hideEntity(arena.getPlugin(), station.getGlowingBlock());
          } catch(Throwable ignored) {
          }
        }
      }
      return;
    }

    List<GameTask> pTasks = playerTasks.get(player);
    for(TaskStationDisplay station : stationDisplays) {
      boolean hasTaskHere = false;
      boolean hasUnfinishedTaskHere = false;

      if(pTasks != null) {
        for(GameTask gt : pTasks) {
          Location gLoc = gt.getLocation();
          if(gLoc != null && gLoc.getWorld() != null && gLoc.getWorld().equals(station.getLocation().getWorld())) {
            if(gLoc.getBlockX() == station.getLocation().getBlockX()
              && gLoc.getBlockY() == station.getLocation().getBlockY()
              && gLoc.getBlockZ() == station.getLocation().getBlockZ()) {
              hasTaskHere = true;
              if(!gt.isCompleted()) {
                hasUnfinishedTaskHere = true;
              }
            }
          }
        }
      }

      if(hasTaskHere) {
        if(hasUnfinishedTaskHere) {
          if(station.getActiveHologram() != null && station.getActiveHologram().isValid()) {
            try {
              player.showEntity(arena.getPlugin(), station.getActiveHologram());
            } catch(Throwable ignored) {
            }
          }
          if(station.getCompletedHologram() != null && station.getCompletedHologram().isValid()) {
            try {
              player.hideEntity(arena.getPlugin(), station.getCompletedHologram());
            } catch(Throwable ignored) {
            }
          }
          if(station.getGlowingBlock() != null && station.getGlowingBlock().isValid()) {
            try {
              arena.setEntityGlowColor(station.getGlowingBlock(), ChatColor.GOLD);
              player.showEntity(arena.getPlugin(), station.getGlowingBlock());
            } catch(Throwable ignored) {
            }
          }
        } else {
          if(station.getActiveHologram() != null && station.getActiveHologram().isValid()) {
            try {
              player.hideEntity(arena.getPlugin(), station.getActiveHologram());
            } catch(Throwable ignored) {
            }
          }
          if(station.getCompletedHologram() != null && station.getCompletedHologram().isValid()) {
            try {
              player.showEntity(arena.getPlugin(), station.getCompletedHologram());
            } catch(Throwable ignored) {
            }
          }
          if(station.getGlowingBlock() != null && station.getGlowingBlock().isValid()) {
            try {
              player.hideEntity(arena.getPlugin(), station.getGlowingBlock());
            } catch(Throwable ignored) {
            }
          }
        }
      } else {
        if(station.getActiveHologram() != null && station.getActiveHologram().isValid()) {
          try {
            player.showEntity(arena.getPlugin(), station.getActiveHologram());
          } catch(Throwable ignored) {
          }
        }
        if(station.getCompletedHologram() != null && station.getCompletedHologram().isValid()) {
          try {
            player.hideEntity(arena.getPlugin(), station.getCompletedHologram());
          } catch(Throwable ignored) {
          }
        }
        if(station.getGlowingBlock() != null && station.getGlowingBlock().isValid()) {
          try {
            player.hideEntity(arena.getPlugin(), station.getGlowingBlock());
          } catch(Throwable ignored) {
          }
        }
      }
    }
  }

  public void updateAllPlayersStationVisibility() {
    for(Player player : arena.getPlayers()) {
      updatePlayerStationVisibility(player);
    }
  }
}
