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

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;
import plugily.projects.minigamesbox.classic.handlers.language.MessageBuilder;
import plugily.projects.minigamesbox.classic.utils.version.VersionUtils;
import plugily.projects.minigamesbox.classic.utils.version.xseries.XSound;
import plugily.projects.murdermystery.arena.Arena;
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
  private BukkitTask arrowTrackerTask;
  private int totalTasks = 0;
  private int completedTasks = 0;
  private boolean allTasksCompleted = false;

  public TaskManager(Arena arena) {
    this.arena = arena;
    this.minigameManager = new TaskMinigameManager(arena);
    this.visualEffectManager = new VisualTaskEffectManager(arena);
  }

  public VisualTaskEffectManager getVisualEffectManager() {
    return visualEffectManager;
  }

  public void assignTasks(List<Player> innocents, List<Location> taskLocations, int tasksPerPlayer) {
    playerTasks.clear();
    visualEffectManager.cleanup();
    for(TaskStationDisplay station : stationDisplays) {
      station.remove();
    }
    stationDisplays.clear();
    completedTasks = 0;
    allTasksCompleted = false;

    if(taskLocations.isEmpty()) {
      arena.getPlugin().getDebugger().debug("No task locations configured for arena {0}, skipping task assignment", arena.getId());
      return;
    }

    TaskType[] allTaskTypes = TaskType.getValues();

    Random random = new Random();
    List<Set<TaskType>> assignedSets = new ArrayList<>();

    for(Player player : innocents) {
      List<TaskType> pool = new ArrayList<>(Arrays.asList(allTaskTypes));
      Collections.shuffle(pool, random);

      int count = Math.max(1, Math.min(tasksPerPlayer, pool.size()));
      Set<TaskType> playerTypes = new HashSet<>(pool.subList(0, count));

      int attempts = 0;
      while(attempts < 15 && assignedSets.contains(playerTypes)) {
        Collections.shuffle(pool, random);
        playerTypes = new HashSet<>(pool.subList(0, count));
        attempts++;
      }
      assignedSets.add(playerTypes);

      List<Location> shuffledLocations = new ArrayList<>(taskLocations);
      Collections.shuffle(shuffledLocations, random);

      List<GameTask> tasks = new ArrayList<>();
      int i = 0;
      for(TaskType type : playerTypes) {
        Location taskLoc = null;
        List<Location> specificLocs = arena.getTaskLocations(type);
        if(!specificLocs.isEmpty()) {
          taskLoc = specificLocs.get(random.nextInt(specificLocs.size()));
        } else if(!arena.getTaskLocations().isEmpty()) {
          List<Location> genericLocs = arena.getTaskLocations();
          taskLoc = genericLocs.get(random.nextInt(genericLocs.size()));
        } else if(!shuffledLocations.isEmpty()) {
          for(int attempt = 0; attempt < shuffledLocations.size(); attempt++) {
            Location candidate = shuffledLocations.get((i + attempt) % shuffledLocations.size());
            boolean alreadyUsed = false;
            for(GameTask existing : tasks) {
              if(existing.getLocation() != null && existing.getLocation().equals(candidate)) {
                alreadyUsed = true;
                break;
              }
            }
            if(!alreadyUsed) {
              taskLoc = candidate;
              i += attempt + 1;
              break;
            }
          }
          if(taskLoc == null) {
            taskLoc = shuffledLocations.get(i % shuffledLocations.size());
            i++;
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
          locationTasks.computeIfAbsent(loc, k -> new LinkedHashSet<>()).add(entry.getKey());
        }
      }
    }

    for(List<GameTask> pTasks : playerTasks.values()) {
      for(GameTask gt : pTasks) {
        Location loc = gt.getLocation();
        if(loc != null && loc.getWorld() != null) {
          locationTasks.computeIfAbsent(loc, k -> new LinkedHashSet<>()).add(gt.getTaskType());
        }
      }
    }

    for(Location loc : taskLocations) {
      if(loc != null && loc.getWorld() != null) {
        locationTasks.computeIfAbsent(loc, k -> new LinkedHashSet<>());
      }
    }

    for(Map.Entry<Location, Set<TaskType>> entry : locationTasks.entrySet()) {
      Location loc = entry.getKey();
      Set<TaskType> types = entry.getValue();
      Location displayLoc = loc.clone().add(0.5, 1.4, 0.5);

      String activeText;
      String completedText;
      if(types.isEmpty()) {
        activeText = "<#5A4E7C>[<#FF7518>Task<#5A4E7C>]\n\n<#F4F1DE>จุดทำภารกิจ\n\n<#5A4E7C>(<#B8B0C8>กดคลิกขวาเพื่อทำภารกิจ<#5A4E7C>)";
        completedText = "<#5A4E7C>[<#6EEB83>Task<#5A4E7C>]\n\n<#6EEB83>ทำภารกิจเสร็จแล้ว\n\n<#5A4E7C>(<#B8B0C8>จุดทำภารกิจนี้เสร็จสิ้นแล้ว<#5A4E7C>)";
      } else if(types.size() == 1) {
        TaskType t = types.iterator().next();
        activeText = "<#5A4E7C>[<#FF7518>Task<#5A4E7C>]\n\n" + t.getHologramTitle() + "\n\n<#5A4E7C>(<#B8B0C8>กดคลิกขวาเพื่อทำภารกิจ<#5A4E7C>)";
        completedText = "<#5A4E7C>[<#6EEB83>Task<#5A4E7C>]\n\n<#B8B0C8>" + t.getDisplayName() + "\n<#6EEB83>ทำภารกิจเสร็จแล้ว";
      } else {
        StringBuilder sbActive = new StringBuilder("<#5A4E7C>[<#FF7518>Task<#5A4E7C>]\n\n");
        StringBuilder sbDone = new StringBuilder("<#5A4E7C>[<#6EEB83>Task<#5A4E7C>]\n\n");
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
        sbActive.append("\n\n<#5A4E7C>(<#B8B0C8>กดคลิกขวาเพื่อทำภารกิจ<#5A4E7C>)");
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
        meta.setDisplayName(HalloweenChat.color("<#5A4E7C>[<#FF7518>Task Compass<#5A4E7C>]"));
        meta.setLore(Arrays.asList(
          HalloweenChat.color("<#F4F1DE>ชี้ไปยังจุดทำภารกิจถัดไปของคุณ"),
          HalloweenChat.color("<#5A4E7C>(<#B8B0C8>เดินตามเข็มทิศแล้วกดคลิกขวาที่จุดภารกิจ<#5A4E7C>)")
        ));
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
    player.sendMessage(HalloweenChat.color("<#5A4E7C>[<#FF7518>Tasks<#5A4E7C>]"));
    for(GameTask task : tasks) {
      String taskName = task.getTaskType().getDisplayName();
      String visualTag = (task.getTaskType().isVisual() && arena.isVisualTasksEnabled())
        ? " <#5A4E7C>(<#72EFDD>ยืนยันตัวตน<#5A4E7C>)"
        : "";
      if(task.isCompleted()) {
        player.sendMessage(HalloweenChat.color("<#5A4E7C>- <#6EEB83>" + taskName + visualTag + " <#5A4E7C>(<#6EEB83>เสร็จแล้ว<#5A4E7C>)"));
      } else {
        player.sendMessage(HalloweenChat.color("<#5A4E7C>- <#F4F1DE>" + taskName + visualTag));
      }
    }
    player.sendMessage(HalloweenChat.color("<#5A4E7C>(<#B8B0C8>เดินตามเข็มทิศหรือแผนที่ไปกดคลิกขวาที่จุดภารกิจ<#5A4E7C>)"));
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

        new MessageBuilder("IN_GAME_MESSAGES_ARENA_TASK_COMPLETED")
          .asKey().player(player).arena(arena)
          .value(taskName)
          .sendPlayer();

        int remaining = getPlayerTotalTasks(player) - getPlayerCompletedTasks(player);
        if(remaining > 0) {
          player.sendMessage(HalloweenChat.color("<#5A4E7C>[<#FF7518>Task<#5A4E7C>] <#F4F1DE>เหลืออีก <#FFB703>" + remaining + " <#F4F1DE>ภารกิจ"));
          GameTask next = getNextTask(player);
          if(next != null) {
            String nextName = next.getTaskType().getDisplayName();
            HalloweenChat.sendActionBar(player, HalloweenChat.color("<#5A4E7C>[<#FF7518>Task<#5A4E7C>] <#B8B0C8>ภารกิจถัดไป <#FFB703>" + nextName));
          }
        } else {
          player.sendMessage(HalloweenChat.color("<#5A4E7C>[<#6EEB83>Task<#5A4E7C>] <#6EEB83>ทำภารกิจส่วนตัวครบแล้ว <#5A4E7C>(<#B8B0C8>รอเพื่อนร่วมทีมทำภารกิจให้ครบ<#5A4E7C>)"));
          HalloweenChat.sendActionBar(player, HalloweenChat.color("<#5A4E7C>[<#6EEB83>Task<#5A4E7C>] <#6EEB83>ทำภารกิจส่วนตัวครบแล้ว"));
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
        HalloweenChat.sendActionBar(player, HalloweenChat.color("<#5A4E7C>[<#FF7518>Task<#5A4E7C>] <#B8B0C8>ไม่มีภารกิจที่จุดนี้"));
        break;
      }
    }

    return false;
  }

  public TaskMinigameManager getMinigameManager() {
    return minigameManager;
  }

  public boolean interactTask(Player player) {
    return interactTask(player, null);
  }

  public boolean interactTask(Player player, Location targetLoc) {
    List<GameTask> tasks = playerTasks.get(player);
    if(tasks == null || tasks.isEmpty()) {
      return false;
    }

    double range = arena.getPlugin().getConfig().getDouble("Tasks.Complete-Range", 4.5);
    if(range < 4.5) {
      range = 4.5;
    }

    for(GameTask task : tasks) {
      if(task.isCompleted()) {
        continue;
      }

      Location taskLoc = task.getLocation();
      boolean match = false;

      if(targetLoc != null && taskLoc != null && targetLoc.getWorld() != null && targetLoc.getWorld().equals(taskLoc.getWorld())) {
        if(targetLoc.getBlockX() == taskLoc.getBlockX() && targetLoc.getBlockY() == taskLoc.getBlockY() && targetLoc.getBlockZ() == taskLoc.getBlockZ()) {
          match = true;
        } else if(targetLoc.distanceSquared(taskLoc) <= 4.0) {
          match = true;
        }
      }

      if(!match && task.isInRange(player.getLocation(), range)) {
        match = true;
      }

      if(match) {
        if(minigameManager.hasMinigame(task.getTaskType())) {
          boolean opened = minigameManager.openMinigame(player, task.getTaskType());
          if(opened && task.getTaskType().isVisual()) {
            visualEffectManager.startVisualSession(player, task.getTaskType(), task.getLocation());
          }
          return opened;
        } else {
          return completeTaskFromMinigame(player, task.getTaskType());
        }
      }
    }

    for(Location loc : arena.getAllTaskLocations()) {
      if(loc == null || loc.getWorld() == null || !loc.getWorld().equals(player.getWorld())) {
        continue;
      }

      boolean nearStation = false;
      if(targetLoc != null && (targetLoc.getBlockX() == loc.getBlockX() && targetLoc.getBlockY() == loc.getBlockY() && targetLoc.getBlockZ() == loc.getBlockZ())) {
        nearStation = true;
      } else if(targetLoc != null && targetLoc.distanceSquared(loc) <= 4.0) {
        nearStation = true;
      } else if(player.getLocation().distanceSquared(loc) <= (range * range)) {
        nearStation = true;
      }

      if(nearStation) {
        boolean wasCompletedHere = false;
        for(GameTask gt : tasks) {
          if(gt.isCompleted() && gt.getLocation() != null && gt.getLocation().getBlockX() == loc.getBlockX() && gt.getLocation().getBlockY() == loc.getBlockY() && gt.getLocation().getBlockZ() == loc.getBlockZ()) {
            wasCompletedHere = true;
            break;
          }
        }
        if(wasCompletedHere) {
          HalloweenChat.sendActionBar(player, HalloweenChat.color("<#5A4E7C>[<#6EEB83>Task<#5A4E7C>] <#6EEB83>ทำภารกิจจุดนี้เสร็จแล้ว"));
        } else {
          HalloweenChat.sendActionBar(player, HalloweenChat.color("<#5A4E7C>[<#FF7518>Task<#5A4E7C>] <#B8B0C8>ไม่มีภารกิจที่จุดนี้"));
        }
        return true;
      }
    }

    return false;
  }

  public boolean completeTaskFromMinigame(Player player, TaskType taskType) {
    List<GameTask> tasks = playerTasks.get(player);
    if(tasks == null || tasks.isEmpty()) {
      return false;
    }

    for(GameTask task : tasks) {
      if(!task.isCompleted() && task.getTaskType() == taskType) {
        task.setCompleted(true);
        completedTasks++;
        updateBossBar();
        visualEffectManager.completeVisualSession(player, task.getTaskType());

        String taskName = task.getTaskType().getDisplayName();
        new MessageBuilder("IN_GAME_MESSAGES_ARENA_TASK_COMPLETED")
          .asKey().player(player).arena(arena)
          .value(taskName)
          .sendPlayer();

        XSound.ENTITY_EXPERIENCE_ORB_PICKUP.play(player.getLocation(), 1F, 1F);
        VersionUtils.sendParticles("VILLAGER_HAPPY", player, player.getLocation().add(0, 1, 0), 15);

        int remaining = getPlayerTotalTasks(player) - getPlayerCompletedTasks(player);
        if(remaining > 0) {
          player.sendMessage(HalloweenChat.color("<#5A4E7C>[<#FF7518>Task<#5A4E7C>] <#F4F1DE>เหลืออีก <#FFB703>" + remaining + " <#F4F1DE>ภารกิจ"));
          GameTask next = getNextTask(player);
          if(next != null) {
            String nextName = next.getTaskType().getDisplayName();
            HalloweenChat.sendActionBar(player, HalloweenChat.color("<#5A4E7C>[<#FF7518>Task<#5A4E7C>] <#B8B0C8>ภารกิจถัดไป <#FFB703>" + nextName));
          }
        } else {
          player.sendMessage(HalloweenChat.color("<#5A4E7C>[<#6EEB83>Task<#5A4E7C>] <#6EEB83>ทำภารกิจส่วนตัวครบแล้ว <#5A4E7C>(<#B8B0C8>รอเพื่อนร่วมทีมทำภารกิจให้ครบ<#5A4E7C>)"));
          HalloweenChat.sendActionBar(player, HalloweenChat.color("<#5A4E7C>[<#6EEB83>Task<#5A4E7C>] <#6EEB83>ทำภารกิจส่วนตัวครบแล้ว"));
        }

        updatePlayerStationVisibility(player);

        if(completedTasks >= totalTasks) {
          allTasksCompleted = true;
        }

        return true;
      }
    }
    return false;
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

  private String formatBossBarTitle() {
    return HalloweenChat.color(
      "<#5A4E7C>[<#FF7518>Tasks<#5A4E7C>] <#F4F1DE>ซ่อมรถบัส <#FFB703>"
        + completedTasks
        + "<#5A4E7C>/<#FFB703>"
        + totalTasks
    );
  }

  private void createBossBar() {
    if(taskBossBar != null) {
      taskBossBar.setVisible(false);
      taskBossBar.removeAll();
    }
    taskBossBar = Bukkit.createBossBar(formatBossBarTitle(), BarColor.PURPLE, BarStyle.SEGMENTED_10);
    taskBossBar.setProgress(0.0);
    taskBossBar.setVisible(true);

    for(Player player : arena.getPlayers()) {
      taskBossBar.addPlayer(player);
    }
    updateBossBar();
  }

  public void updateBossBar() {
    if(taskBossBar == null) {
      return;
    }
    taskBossBar.setTitle(formatBossBarTitle());
    double progress = totalTasks <= 0 ? 0.0 : Math.max(0.0, Math.min(1.0, (double) completedTasks / totalTasks));
    taskBossBar.setProgress(progress);

    if(progress >= 0.75) {
      taskBossBar.setColor(BarColor.GREEN);
    } else if(progress >= 0.5) {
      taskBossBar.setColor(BarColor.YELLOW);
    } else {
      taskBossBar.setColor(BarColor.PURPLE);
    }
  }

  public void addPlayerToBossBar(Player player) {
    if(taskBossBar != null && player != null) {
      taskBossBar.addPlayer(player);
    }
  }

  public void removePlayerFromBossBar(Player player) {
    if(taskBossBar != null && player != null) {
      taskBossBar.removePlayer(player);
    }
  }

  public void cleanup() {
    stopArrowTracker();
    visualEffectManager.cleanup();
    if(taskBossBar != null) {
      taskBossBar.setVisible(false);
      taskBossBar.removeAll();
      taskBossBar = null;
    }
    for(TaskStationDisplay station : stationDisplays) {
      station.remove();
    }
    stationDisplays.clear();
    minigameManager.cleanup();
    playerTasks.clear();
    totalTasks = 0;
    completedTasks = 0;
    allTasksCompleted = false;
  }

  public void startArrowTracker() {
    stopArrowTracker();
    arrowTrackerTask = Bukkit.getScheduler().runTaskTimer(arena.getPlugin(), () -> {
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
