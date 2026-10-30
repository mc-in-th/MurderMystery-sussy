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
import org.bukkit.event.player.PlayerTeleportEvent;
import plugily.projects.minigamesbox.api.arena.IArenaState;
import plugily.projects.minigamesbox.api.user.IUser;
import plugily.projects.minigamesbox.classic.arena.PluginArena;
import plugily.projects.minigamesbox.classic.arena.states.PluginStartingState;
import plugily.projects.minigamesbox.classic.handlers.language.MessageBuilder;
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
    Arena pluginArena = (Arena) getPlugin().getArenaRegistry().getArena(arena.getId());
    boolean startingNow = pluginArena != null
      && (arena.getTimer() == 0 || arena.isForceStart())
      && arena.getPlayers().size() >= arena.getMinimumPlayers();
    Location savedStartLocation = arena.getStartLocation();
    if(startingNow) {
      if((savedStartLocation == null || savedStartLocation.getWorld() == null) && !pluginArena.getPlayerSpawnPoints().isEmpty()) {
        savedStartLocation = pluginArena.getPlayerSpawnPoints().get(0);
        arena.setStartLocation(savedStartLocation);
      }
      if(pluginArena.getCutsceneManager().hasCutscene()) {
        Location cutsceneStart = pluginArena.getCutsceneManager().getInitialEyeLocation();
        if(cutsceneStart != null && cutsceneStart.getWorld() != null) {
          cutsceneStart.getChunk().load(true);
          arena.setStartLocation(cutsceneStart);
        }
      } else if(arena.getStartLocation() != null && arena.getStartLocation().getWorld() != null) {
        arena.getStartLocation().getChunk().load(true);
      }
    }

    try {
      super.handleCall(arena);
    } finally {
      if(startingNow && savedStartLocation != null) {
        arena.setStartLocation(savedStartLocation);
      }
    }

    if(pluginArena == null) {
      return;
    }

    if(startingNow && getArenaStateChange() == IArenaState.FULL_GAME) {
      setArenaState(IArenaState.IN_GAME);
    }

    if(!pluginArena.isHideChances() && getArenaStateChange() != IArenaState.IN_GAME) {
      for(Player player : arena.getPlayersLeft()) {
        HalloweenChat.sendActionBar(player, new MessageBuilder("IN_GAME_MESSAGES_ARENA_ROLE_CHANCES_ACTION_BAR").asKey().player(player).arena(pluginArena).build());
      }
    }

    if(startingNow && getArenaStateChange() == IArenaState.IN_GAME) {
      for(Player player : arena.getPlayersLeft()) {
        IUser user = arena.getPlugin().getUserManager().getUser(player);
        user.resetNonePersistentStatistics();
        PrayerRegistry.getRush().remove(player);
        PrayerRegistry.getBan().remove(player);
        getPlugin().getActionBarManager().clearActionBarsFromPlayer(player);
      }

      Set<Player> playersToSet = new HashSet<>(arena.getPlayersLeft());

      pluginArena.setSabotageWin(false);
      pluginArena.setTaskWin(false);
      getMaxRolesToSet(pluginArena);

      addRole(pluginArena, Role.MURDERER, playersToSet);

      arena.getPlugin().getDebugger().debug("After: Arena: {0} | Murders = {1}, Players = {2} | Players: Murders = {3}", arena.getId(), maxmurderer, arena.getPlayersLeft().size(), pluginArena.getMurdererList());

      pluginArena.clearPluginBossBars();

      Runnable finishStart = () -> {
        pluginArena.getSpecialBlocks().forEach(pluginArena::loadSpecialBlock);
        pluginArena.spawnCampfireHologram();
        int size = pluginArena.getPlayerSpawnPoints().size();
        List<Player> innocentPlayers = new ArrayList<>();
        for(Player player : arena.getPlayersLeft()) {
          if(player == null || !player.isOnline()) {
            continue;
          }
          if(size > 0) {
            Location spawnPoint = pluginArena.getPlayerSpawnPoints().get(getPlugin().getRandom().nextInt(size));
            if(spawnPoint != null && spawnPoint.getWorld() != null) {
              spawnPoint.getChunk().load(true);
              player.teleport(spawnPoint, PlayerTeleportEvent.TeleportCause.PLUGIN);
            }
          }
          player.setFallDistance(0.0f);
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
          if(p != null && p.isOnline() && ArenaUtils.isSpectator(p, pluginArena)) {
            if(size > 0) {
              Location specSpawn = pluginArena.getPlayerSpawnPoints().get(0);
              if(specSpawn != null && specSpawn.getWorld() != null) {
                specSpawn.getChunk().load(true);
                p.teleport(specSpawn, PlayerTeleportEvent.TeleportCause.PLUGIN);
              }
            }
            ArenaUtils.applySpectatorState(p, pluginArena, true);
            pluginArena.getTaskManager().addPlayerToBossBar(p);
            pluginArena.getSabotageManager().addPlayerToBossBar(p);
          }
        }
        ArenaUtils.syncArenaNameTags(pluginArena);
      };

      ArenaUtils.syncArenaNameTags(pluginArena);
      if(pluginArena.getCutsceneManager().hasCutscene()) {
        pluginArena.getCutsceneManager().startCutscene(finishStart);
      } else {
        finishStart.run();
      }
      getPlugin().getServer().getScheduler().runTask(getPlugin(), () -> ArenaUtils.syncArenaNameTags(pluginArena));
      getPlugin().getServer().getScheduler().runTaskLater(getPlugin(), () -> ArenaUtils.syncArenaNameTags(pluginArena), 3L);
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
