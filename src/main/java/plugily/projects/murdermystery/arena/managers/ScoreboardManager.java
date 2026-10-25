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
import plugily.projects.murdermystery.arena.cutscene.CutsceneManager;
import plugily.projects.murdermystery.arena.role.Role;
import plugily.projects.murdermystery.arena.task.GameTask;
import plugily.projects.murdermystery.utils.HalloweenChat;

import java.util.ArrayList;
import java.util.Collections;
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

  private boolean shouldHideScoreboard(Player player) {
    if(arena instanceof Arena && ((Arena) arena).getCutsceneManager().isPlaying()) {
      return true;
    }
    return player != null && CutsceneManager.isPreviewing(player);
  }

  @Override
  public void createScoreboard(IUser user) {
    if(user == null) {
      return;
    }
    removeScoreboard(user);
    if(shouldHideScoreboard(user.getPlayer())) {
      return;
    }
    super.createScoreboard(user);
  }

  @Override
  public void updateScoreboards() {
    if(arena instanceof Arena && ((Arena) arena).getCutsceneManager().isPlaying()) {
      stopAllScoreboards();
      return;
    }
    for(Player player : arena.getPlayers()) {
      if(CutsceneManager.isPreviewing(player)) {
        IUser user = arena.getPlugin().getUserManager().getUser(player);
        if(user != null) {
          removeScoreboard(user);
        }
      }
    }
    super.updateScoreboards();
  }

  @Override
  public List<String> getScoreboardLines(Player player) {
    if(shouldHideScoreboard(player)) {
      return Collections.emptyList();
    }
    if(!(arena instanceof Arena)) {
      return super.getScoreboardLines(player);
    }

    Arena gameArena = (Arena) arena;
    IArenaState state = gameArena.getArenaState();
    IUser user = gameArena.getPlugin().getUserManager().getUser(player);

    if(state == IArenaState.WAITING_FOR_PLAYERS) {
      List<String> lines = new ArrayList<>();
      lines.add(HalloweenChat.color("<#5A4E7C>----------------"));
      lines.add(HalloweenChat.color("<#F4F1DE>ด่าน: <#FF7518>" + gameArena.getMapName()));
      lines.add(HalloweenChat.color("<#F4F1DE>สถานะ: <#5A4E7C>[<#FFB703>รอผู้เล่น<#5A4E7C>]"));
      lines.add("");
      lines.add(HalloweenChat.color("<#F4F1DE>ผู้เล่น: <#72EFDD>%arena_players_size%<#5A4E7C>/<#72EFDD>%arena_max_players% คน"));
      lines.add(HalloweenChat.color("<#F4F1DE>ขั้นต่ำ: <#FFB703>%arena_min_players% คน"));
      if(!gameArena.isHideChances()) {
        lines.add("");
        lines.add(HalloweenChat.color("<#F4F1DE>โอกาสเป็นฆาตกร: <#FF3B47>%arena_murderer_chance%"));
      }
      lines.add(HalloweenChat.color("<#5A4E7C>----------------"));
      return lines;
    }

    if(state == IArenaState.STARTING || state == IArenaState.FULL_GAME) {
      List<String> lines = new ArrayList<>();
      lines.add(HalloweenChat.color("<#5A4E7C>----------------"));
      lines.add(HalloweenChat.color("<#F4F1DE>ด่าน: <#FF7518>" + gameArena.getMapName()));
      lines.add(HalloweenChat.color("<#F4F1DE>สถานะ: " + (state == IArenaState.FULL_GAME
        ? "<#5A4E7C>[<#6EEB83>ห้องเต็ม<#5A4E7C>]"
        : "<#5A4E7C>[<#6EEB83>กำลังจะเริ่ม<#5A4E7C>]")));
      lines.add("");
      lines.add(HalloweenChat.color("<#F4F1DE>เริ่มใน: <#FFB703>" + Math.max(0, gameArena.getTimer()) + " วิ"));
      lines.add(HalloweenChat.color("<#F4F1DE>ผู้เล่น: <#72EFDD>%arena_players_size%<#5A4E7C>/<#72EFDD>%arena_max_players% คน"));
      if(!gameArena.isHideChances()) {
        lines.add("");
        lines.add(HalloweenChat.color("<#F4F1DE>โอกาสเป็นฆาตกร: <#FF3B47>%arena_murderer_chance%"));
      }
      lines.add(HalloweenChat.color("<#5A4E7C>----------------"));
      return lines;
    }

    if(state == IArenaState.IN_GAME) {
      boolean isMurderer = Role.isRole(Role.MURDERER, user, gameArena);

      List<String> lines = new ArrayList<>();
      lines.add(HalloweenChat.color("<#5A4E7C>----------------"));
      lines.add(HalloweenChat.color("<#F4F1DE>บทบาท: " + (isMurderer
        ? "<#5A4E7C>[<#FF3B47>ฆาตกร<#5A4E7C>]"
        : "<#5A4E7C>[<#FFB703>ผู้โดยสาร<#5A4E7C>]")));
      lines.add(HalloweenChat.color("<#F4F1DE>เวลา: <#FFB703>%arena_time%"));
      lines.add(HalloweenChat.color("<#F4F1DE>ผู้รอดชีวิต: <#72EFDD>%arena_innocent_size% คน"));
      lines.add("");

      if(isMurderer) {
        lines.add(HalloweenChat.color("<#5A4E7C>[<#FF3B47>ภารกิจหลอก<#5A4E7C>]"));
        List<GameTask> sampleTasks = gameArena.getTaskManager().getTasksForMurderer();
        if(sampleTasks != null && !sampleTasks.isEmpty()) {
          for(GameTask task : sampleTasks) {
            lines.add(HalloweenChat.color("<#B8B0C8>- <#F4F1DE>" + task.getTaskType().getDisplayName()));
          }
        } else {
          lines.add(HalloweenChat.color("<#B8B0C8>ไม่มีภารกิจในห้องนี้"));
        }
      } else {
        List<GameTask> tasks = gameArena.getTaskManager().getPlayerTasks(player);
        int completed = gameArena.getTaskManager().getPlayerCompletedTasks(player);
        int total = gameArena.getTaskManager().getPlayerTotalTasks(player);
        lines.add(HalloweenChat.color("<#5A4E7C>[<#FF7518>ภารกิจของคุณ<#5A4E7C>] <#FFB703>" + completed + "<#5A4E7C>/<#FFB703>" + total));
        if(tasks != null && !tasks.isEmpty()) {
          for(GameTask task : tasks) {
            String visualTag = (task.getTaskType().isVisual() && gameArena.isVisualTasksEnabled())
              ? " <#72EFDD>[V]"
              : "";
            if(task.isCompleted()) {
              lines.add(HalloweenChat.color("<#6EEB83>[/] <#6EEB83>" + task.getTaskType().getDisplayName() + visualTag));
            } else {
              lines.add(HalloweenChat.color("<#FF3B47>[x] <#F4F1DE>" + task.getTaskType().getDisplayName() + visualTag));
            }
          }
        } else {
          lines.add(HalloweenChat.color("<#B8B0C8>ไม่มีภารกิจ"));
        }
      }
      lines.add(HalloweenChat.color("<#5A4E7C>----------------"));
      return lines;
    }

    if(state == IArenaState.ENDING) {
      boolean murderWon = gameArena.isMurdererVictory();
      Player mainMurderer = gameArena.getCharacter(Arena.CharacterType.MURDERER);
      List<String> lines = new ArrayList<>();
      lines.add(HalloweenChat.color("<#5A4E7C>----------------"));
      lines.add(HalloweenChat.color("<#F4F1DE>สถานะ: <#5A4E7C>[<#FFB703>จบเกมแล้ว<#5A4E7C>]"));
      lines.add(HalloweenChat.color("<#F4F1DE>ผู้ชนะ: " + (murderWon
        ? "<#5A4E7C>[<#FF3B47>ฆาตกร<#5A4E7C>]"
        : "<#5A4E7C>[<#6EEB83>ผู้โดยสาร<#5A4E7C>]")));
      lines.add("");
      if(user != null && !user.isPermanentSpectator()) {
        boolean isMurderer = gameArena.getMurdererList().contains(player)
          || Role.isRole(Role.MURDERER, user, gameArena)
          || player.equals(mainMurderer);
        boolean playerWon = (murderWon && isMurderer) || (!murderWon && !isMurderer);
        lines.add(HalloweenChat.color("<#F4F1DE>ผลลัพธ์: " + (playerWon ? "<#6EEB83>ชนะ" : "<#FF3B47>แพ้")));
      }
      String murdererNames = formatMurdererNames(gameArena, mainMurderer);
      if(!murdererNames.isEmpty()) {
        lines.add(HalloweenChat.color("<#F4F1DE>ฆาตกร: <#FF3B47>" + murdererNames));
      }
      lines.add(HalloweenChat.color("<#F4F1DE>กลับล็อบบี้ใน: <#FFB703>" + Math.max(0, gameArena.getTimer()) + " วิ"));
      lines.add(HalloweenChat.color("<#5A4E7C>----------------"));
      return lines;
    }

    if(state == IArenaState.RESTARTING) {
      List<String> lines = new ArrayList<>();
      lines.add(HalloweenChat.color("<#5A4E7C>----------------"));
      lines.add(HalloweenChat.color("<#F4F1DE>ด่าน: <#FF7518>" + gameArena.getMapName()));
      lines.add(HalloweenChat.color("<#F4F1DE>สถานะ: <#5A4E7C>[<#FF3B47>กำลังรีเซ็ต<#5A4E7C>]"));
      lines.add("");
      lines.add(HalloweenChat.color("<#B8B0C8>กำลังเตรียมสนามประลองใหม่"));
      lines.add(HalloweenChat.color("<#5A4E7C>----------------"));
      return lines;
    }

    return super.getScoreboardLines(player);
  }

  private String formatMurdererNames(Arena gameArena, Player fallbackMurderer) {
    List<Player> murderers = gameArena.getMurdererList();
    StringBuilder sb = new StringBuilder();
    if(murderers != null) {
      for(Player murderer : murderers) {
        if(murderer == null) {
          continue;
        }
        if(sb.length() > 0) {
          sb.append(", ");
        }
        sb.append(murderer.getName());
      }
    }
    if(sb.length() == 0 && fallbackMurderer != null) {
      return fallbackMurderer.getName();
    }
    return sb.toString();
  }
}
