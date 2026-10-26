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

import org.bukkit.Location;

public class GameTask {

  private final TaskType taskType;
  private final Location location;
  private boolean completed;

  public GameTask(TaskType taskType, Location location) {
    this.taskType = taskType;
    this.location = location;
    this.completed = false;
  }

  public TaskType getTaskType() {
    return taskType;
  }

  public Location getLocation() {
    return location;
  }

  public boolean isCompleted() {
    return completed;
  }

  public void setCompleted(boolean completed) {
    this.completed = completed;
  }

  public boolean isInRange(Location playerLocation, double range) {
    if(playerLocation == null || location == null) {
      return false;
    }
    if(!playerLocation.getWorld().equals(location.getWorld())) {
      return false;
    }
    return playerLocation.distanceSquared(location) <= (range * range);
  }
}
