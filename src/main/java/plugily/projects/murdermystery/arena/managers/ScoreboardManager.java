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

package plugily.projects.murdermystery.arena.managers;

import org.bukkit.entity.Player;
import plugily.projects.minigamesbox.api.arena.IArenaState;
import plugily.projects.minigamesbox.api.user.IUser;
import plugily.projects.minigamesbox.classic.arena.PluginArena;
import plugily.projects.minigamesbox.classic.arena.managers.PluginScoreboardManager;
import plugily.projects.murdermystery.arena.Arena;
import plugily.projects.murdermystery.arena.role.Role;
import plugily.projects.murdermystery.arena.task.GameTask;
import plugily.projects.murdermystery.utils.HalloweenChat;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Tigerpanzer_02
 * <p>Created at 19.12.2021
 */
public class ScoreboardManager extends PluginScoreboardManager {

  private final PluginArena arena;

  public ScoreboardManager(PluginArena arena) {
    super(arena);
    this.arena = arena;
  }

  @Override
  public List<String> getScoreboardLines(Player player) {
    if(arena.getArenaState() == IArenaState.IN_GAME && arena instanceof Arena) {
      Arena gameArena = (Arena) arena;
      IUser user = arena.getPlugin().getUserManager().getUser(player);
      boolean isMurderer = Role.isRole(Role.MURDERER, user, gameArena);

      List<String> lines = new ArrayList<>();
      lines.add(HalloweenChat.color("<gradient:#9D4EDD:#FF7518>----------------</gradient>"));
      lines.add(HalloweenChat.color("<#F4F1DE>บทบาท: " + (isMurderer
        ? "<gradient:#D90429:#FF5400><bold>ฆาตกร (Impostor)</bold></gradient>"
        : "<gradient:#FF7518:#FFB703><bold>ผู้โดยสาร (Passenger)</bold></gradient>")));
      lines.add(HalloweenChat.color("<#F4F1DE>เวลา: <#FFB703>%arena_time%"));
      lines.add(HalloweenChat.color("<#F4F1DE>ผู้รอดชีวิต: <#72EFDD>%arena_innocent_size% คน"));
      lines.add("");

      if(isMurderer) {
        lines.add(HalloweenChat.color("<gradient:#D90429:#FF5400><bold>ภารกิจหลอก (Fake Tasks):</bold></gradient>"));
        List<GameTask> sampleTasks = gameArena.getTaskManager().getTasksForMurderer();
        if(sampleTasks != null && !sampleTasks.isEmpty()) {
          for(GameTask task : sampleTasks) {
            lines.add(HalloweenChat.color("<#B8B0C8>- <#C77DFF>" + task.getTaskType().getDisplayName()));
          }
        } else {
          lines.add(HalloweenChat.color("<#B8B0C8>ไม่มีภารกิจในห้องนี้"));
        }
      } else {
        List<GameTask> tasks = gameArena.getTaskManager().getPlayerTasks(player);
        int completed = gameArena.getTaskManager().getPlayerCompletedTasks(player);
        int total = gameArena.getTaskManager().getPlayerTotalTasks(player);
        lines.add(HalloweenChat.color("<gradient:#FF5400:#FFB703><bold>ภารกิจของคุณ</bold></gradient> <#B8B0C8>(" + completed + "/" + total + "):"));
        if(tasks != null && !tasks.isEmpty()) {
          for(GameTask task : tasks) {
            if(task.isCompleted()) {
              lines.add(HalloweenChat.color("<#6EEB83>[/] <#6EEB83>" + task.getTaskType().getDisplayName()));
            } else {
              lines.add(HalloweenChat.color("<#FF3B47>[x] <#F4F1DE>" + task.getTaskType().getDisplayName()));
            }
          }
        } else {
          lines.add(HalloweenChat.color("<#B8B0C8>ไม่มีภารกิจ"));
        }
      }
      lines.add(HalloweenChat.color("<gradient:#FF7518:#9D4EDD>----------------</gradient>"));
      return lines;
    }
    return super.getScoreboardLines(player);
  }
}
