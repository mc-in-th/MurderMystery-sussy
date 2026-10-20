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
import plugily.projects.murdermystery.arena.Arena;
import plugily.projects.murdermystery.arena.task.TaskType;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class TaskMinigameManager {

  private final Arena arena;
  private final Map<TaskType, TaskMinigame> minigames = new EnumMap<>(TaskType.class);
  private final Map<UUID, TaskType> activeMinigames = new HashMap<>();

  public TaskMinigameManager(Arena arena) {
    this.arena = arena;
    registerMinigame(new ControlPanelMinigame(arena));
    registerMinigame(new TirePressureMinigame(arena));
    registerMinigame(new RepairWiringMinigame(arena));
    registerMinigame(new DieselFuelMinigame(arena));
    registerMinigame(new RadiatorWaterMinigame(arena));
    registerMinigame(new EngineOilMinigame(arena));
    registerMinigame(new SteeringFluidMinigame(arena));
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

  public boolean openMinigame(Player player, TaskType taskType) {
    TaskMinigame minigame = minigames.get(taskType);
    if(minigame != null) {
      activeMinigames.put(player.getUniqueId(), taskType);
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
    TaskType activeType = activeMinigames.get(player.getUniqueId());
    if(activeType != null) {
      TaskMinigame m = minigames.get(activeType);
      if(m != null) {
        return m.handleClick(player, rawSlot, clickType);
      }
    }

    for(TaskMinigame m : minigames.values()) {
      if(m.isGui(title)) {
        return m.handleClick(player, rawSlot, clickType);
      }
    }
    return false;
  }

  public void handleClose(Player player, String title) {
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
    for(TaskMinigame m : minigames.values()) {
      m.cleanup();
    }
  }
}
