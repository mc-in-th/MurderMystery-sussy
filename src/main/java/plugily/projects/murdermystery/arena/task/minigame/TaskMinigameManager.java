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

package plugily.projects.murdermystery.arena.task.minigame;

import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import plugily.projects.minigamesbox.api.arena.IArenaState;
import plugily.projects.minigamesbox.api.user.IUser;
import plugily.projects.murdermystery.arena.Arena;
import plugily.projects.murdermystery.arena.role.Role;
import plugily.projects.murdermystery.arena.task.TaskType;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class TaskMinigameManager {

  private final Arena arena;
  private final Map<TaskType, TaskMinigame> minigames = new EnumMap<>(TaskType.class);
  private final Map<UUID, TaskType> activeMinigames = new HashMap<>();
  private final Map<UUID, Integer> openedRoundIds = new HashMap<>();

  public TaskMinigameManager(Arena arena) {
    this.arena = arena;
    registerMinigame(new ControlPanelMinigame(arena));
    registerMinigame(new TirePressureMinigame(arena));
    registerMinigame(new RepairWiringMinigame(arena));
    registerMinigame(new DieselFuelMinigame(arena));
    registerMinigame(new RadiatorWaterMinigame(arena));
    registerMinigame(new EngineOilMinigame(arena));
    registerMinigame(new SteeringFluidMinigame(arena));
    registerMinigame(new CleanWindshieldMinigame(arena));
    registerMinigame(new UnlockManifoldsMinigame(arena));
    registerMinigame(new ChargeBatteryMinigame(arena));
    registerMinigame(new CleanAirFilterMinigame(arena));
  }

  public void registerMinigame(TaskMinigame minigame) {
    minigames.put(minigame.getTaskType(), minigame);
  }

  public boolean hasMinigame(TaskType taskType) {
    return minigames.containsKey(taskType);
  }

  public TaskMinigame getMinigame(TaskType taskType) {
    return minigames.get(taskType);
  }

  public boolean hasActiveMinigame(Player player) {
    return player != null && activeMinigames.containsKey(player.getUniqueId());
  }

  public boolean isValidCompletionRound(Player player) {
    if(player == null) {
      return false;
    }
    Integer roundId = openedRoundIds.get(player.getUniqueId());
    return roundId != null && roundId == arena.getTaskManager().getTaskRoundId();
  }

  public void invalidatePlayerRound(Player player) {
    if(player != null) {
      openedRoundIds.remove(player.getUniqueId());
    }
  }

  public boolean openMinigame(Player player, TaskType taskType) {
    TaskMinigame minigame = minigames.get(taskType);
    if(minigame != null) {
      activeMinigames.put(player.getUniqueId(), taskType);
      openedRoundIds.put(player.getUniqueId(), arena.getTaskManager().getTaskRoundId());
      minigame.open(player);
      return true;
    }
    return false;
  }

  public boolean isMinigameGUI(Player player, String title) {
    if(player != null && activeMinigames.containsKey(player.getUniqueId())) {
      return true;
    }
    return isMinigameGUI(title);
  }

  public boolean isMinigameGUI(String title) {
    if(title == null) {
      return false;
    }
    for(TaskMinigame m : minigames.values()) {
      if(m.isGui(title)) {
        return true;
      }
    }
    return false;
  }

  public boolean handleClick(Player player, String title, int rawSlot, ClickType clickType) {
    if(arena.getArenaState() != IArenaState.IN_GAME
      || arena.getVotingManager().isVotingActive()
      || arena.getCutsceneManager().isPlaying()) {
      player.closeInventory();
      return true;
    }
    IUser user = arena.getPlugin().getUserManager().getUser(player);
    if(Role.isRole(Role.MURDERER, user, arena)
      || (arena.getVentManager() != null && arena.getVentManager().isInsideVent(player))) {
      player.closeInventory();
      return true;
    }

    TaskType activeType = activeMinigames.get(player.getUniqueId());
    if(activeType != null) {
      if(!arena.getTaskManager().isNearAssignedTask(player, activeType, 49.0)) {
        player.closeInventory();
        return true;
      }
      TaskMinigame m = minigames.get(activeType);
      if(m != null) {
        return m.handleClick(player, rawSlot, clickType);
      }
    }

    for(TaskMinigame m : minigames.values()) {
      if(m.isGui(title)) {
        if(!arena.getTaskManager().isNearAssignedTask(player, m.getTaskType(), 49.0)) {
          player.closeInventory();
          return true;
        }
        return m.handleClick(player, rawSlot, clickType);
      }
    }
    return false;
  }

  public void handleClose(Player player, String title) {
    arena.getTaskManager().getVisualEffectManager().stopVisualSession(player);
    TaskType activeType = activeMinigames.remove(player.getUniqueId());
    if(activeType != null) {
      TaskMinigame m = minigames.get(activeType);
      if(m != null) {
        m.handleClose(player);
        return;
      }
    }

    for(TaskMinigame m : minigames.values()) {
      if(m.isGui(title)) {
        m.handleClose(player);
        return;
      }
    }
  }

  public void cleanup() {
    activeMinigames.clear();
    openedRoundIds.clear();
    for(TaskMinigame m : minigames.values()) {
      m.cleanup();
    }
  }
}
