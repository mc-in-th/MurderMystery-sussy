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

package plugily.projects.murdermystery.arena.states;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import plugily.projects.minigamesbox.api.user.IUser;
import plugily.projects.minigamesbox.classic.arena.PluginArena;
import plugily.projects.minigamesbox.classic.arena.states.PluginStartingState;
import plugily.projects.minigamesbox.classic.handlers.language.MessageBuilder;
import plugily.projects.minigamesbox.classic.utils.version.VersionUtils;
import plugily.projects.murdermystery.arena.Arena;
import plugily.projects.murdermystery.arena.ArenaUtils;
import plugily.projects.murdermystery.arena.role.Role;
import plugily.projects.murdermystery.arena.special.pray.PrayerRegistry;
import plugily.projects.murdermystery.utils.HalloweenChat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Plajer
 * <p>Created at 03.06.2019
 */
public class StartingState extends PluginStartingState {

  int maxmurderer = 1;

  @Override
  public void handleCall(PluginArena arena) {
    super.handleCall(arena);
    Arena pluginArena = (Arena) getPlugin().getArenaRegistry().getArena(arena.getId());
    if(pluginArena == null) {
      return;
    }

    if(!pluginArena.isHideChances()) {
      for(Player player : arena.getPlayersLeft()) {
        HalloweenChat.sendActionBar(player, new MessageBuilder("IN_GAME_MESSAGES_ARENA_ROLE_CHANCES_ACTION_BAR").asKey().player(player).arena(pluginArena).build());
      }
    }

    if(arena.getTimer() == 0 || arena.isForceStart()) {
      for(Player player : arena.getPlayersLeft()) {
        IUser user = arena.getPlugin().getUserManager().getUser(player);
        user.resetNonePersistentStatistics();
        PrayerRegistry.getRush().remove(player);
        PrayerRegistry.getBan().remove(player);
        getPlugin().getActionBarManager().clearActionBarsFromPlayer(player);
      }

      Set<Player> playersToSet = new HashSet<>(arena.getPlayersLeft());

      pluginArena.setSabotageWin(false);
      getMaxRolesToSet(pluginArena);

      addRole(pluginArena, Role.MURDERER, playersToSet);

      arena.getPlugin().getDebugger().debug("After: Arena: {0} | Murders = {1}, Players = {2} | Players: Murders = {3}", arena.getId(), maxmurderer, arena.getPlayersLeft().size(), pluginArena.getMurdererList());

      pluginArena.getSpecialBlocks().forEach(pluginArena::loadSpecialBlock);
      pluginArena.spawnCampfireHologram();
      pluginArena.clearPluginBossBars();

      Runnable finishStart = () -> {
        int size = pluginArena.getPlayerSpawnPoints().size();
        List<Player> innocentPlayers = new ArrayList<>();
        for(Player player : arena.getPlayersLeft()) {
          if(size > 0) {
            VersionUtils.teleport(player, pluginArena.getPlayerSpawnPoints().get(getPlugin().getRandom().nextInt(size)));
          }
          ArenaUtils.updateNameTagsVisibility(player);
          player.setGameMode(GameMode.ADVENTURE);
          IUser user = arena.getPlugin().getUserManager().getUser(player);
          if(Role.isRole(Role.MURDERER, user, arena)) {
            HalloweenChat.sendTitleEntry(player, new MessageBuilder("IN_GAME_MESSAGES_ARENA_PLAYING_ROLE_MURDERER").asKey().arena(pluginArena).player(player).build());
            ArenaUtils.playRoleRevealSound(player, true);
            pluginArena.getSabotageManager().giveSabotageRemote(player);
          } else {
            HalloweenChat.sendTitleEntry(player, new MessageBuilder("IN_GAME_MESSAGES_ARENA_PLAYING_ROLE_INNOCENT").asKey().player(player).arena(pluginArena).build());
            ArenaUtils.playRoleRevealSound(player, false);
            innocentPlayers.add(player);
          }
        }

        pluginArena.updateVentDisplaysVisibility();
        List<Location> taskLocs = pluginArena.getAllTaskLocations();
        int tasksPerPlayer = pluginArena.getEffectiveTasksPerPlayer();
        pluginArena.clearPluginBossBars();
        pluginArena.getTaskManager().assignTasks(innocentPlayers, taskLocs, tasksPerPlayer);
        pluginArena.getMinimap().distributeMaps();
        pluginArena.getSabotageManager().start();
        pluginArena.getVotingManager().initForGame();

        for(Player p : pluginArena.getPlayers()) {
          if(ArenaUtils.isSpectator(p, pluginArena)) {
            ArenaUtils.applySpectatorState(p, pluginArena, true);
            pluginArena.getTaskManager().addPlayerToBossBar(p);
            pluginArena.getSabotageManager().addPlayerToBossBar(p);
          }
        }
      };

      if(pluginArena.getCutsceneManager().hasCutscene()) {
        pluginArena.getCutsceneManager().startCutscene(finishStart);
      } else {
        finishStart.run();
      }
    }
  }

  private void addRole(Arena arena, Role role, Set<Player> playersToSet) {
    String roleName = role.toString();

    List<IUser> chancesRanking = getPlugin().getUserManager().getUsers(arena).stream().filter(user -> playersToSet.contains(user.getPlayer())).sorted(Comparator.comparingInt(user -> arena.getContributorValue(role, user))).collect(Collectors.toList());
    Collections.reverse(chancesRanking);
    List<Player> chancesPlayer = new ArrayList<>();
    for(IUser user : chancesRanking) {
      chancesPlayer.add(user.getPlayer());
    }
    getPlugin().getDebugger().debug("Arena {0} | Role add {1} | List {2}", arena.getId(), roleName, chancesPlayer);

    int amount = maxmurderer;
    for(int i = 0; i < amount && i < chancesRanking.size(); i++) {
      IUser user = chancesRanking.get(i);
      Player userPlayer = user.getPlayer();
      arena.setCharacter(role, userPlayer);
      arena.resetContributorValue(role, user);
      playersToSet.remove(userPlayer);
      if(role == Role.MURDERER) {
        arena.getMurdererList().add(userPlayer);
      }
    }
  }

  private void getMaxRolesToSet(Arena arena) {
    maxmurderer = 1;
    int playersSize = arena.getPlayersLeft().size();
    arena.getPlugin().getDebugger().debug("Before: Arena: {0} | Murders = {1}, Players = {2} | Configured: Murders = {3}", arena.getId(), maxmurderer, playersSize, arena.getArenaOption("MURDERER_DIVIDER"));
    if(arena.getArenaOption("MURDERER_DIVIDER") > 1 && playersSize > arena.getArenaOption("MURDERER_DIVIDER")) {
      maxmurderer = (playersSize / arena.getArenaOption("MURDERER_DIVIDER"));
    }
    if(playersSize - maxmurderer < 1) {
      arena.getPlugin().getDebugger().debug("{0} Murderers amount was reduced because there are not enough players", arena.getId());
      if(maxmurderer > 1) {
        maxmurderer--;
      }
    }
    arena.getPlugin().getDebugger().debug("After: Arena: {0} | Murders = {1}, Players = {2} | Configured: Murders = {3}", arena.getId(), maxmurderer, playersSize, arena.getArenaOption("MURDERER_DIVIDER"));
  }
}
