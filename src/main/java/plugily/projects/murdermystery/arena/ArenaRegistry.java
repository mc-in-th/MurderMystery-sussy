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

package plugily.projects.murdermystery.arena;

import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import plugily.projects.minigamesbox.api.arena.IPluginArena;
import plugily.projects.minigamesbox.classic.arena.PluginArena;
import plugily.projects.minigamesbox.classic.arena.PluginArenaRegistry;
import plugily.projects.minigamesbox.classic.handlers.language.MessageBuilder;
import plugily.projects.minigamesbox.classic.utils.serialization.LocationSerializer;
import plugily.projects.murdermystery.Main;
import plugily.projects.murdermystery.arena.cutscene.CutsceneManager;
import plugily.projects.murdermystery.arena.sabotage.SabotageType;
import plugily.projects.murdermystery.arena.special.SpecialBlock;
import plugily.projects.murdermystery.arena.task.TaskType;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Tom on 27/07/2014.
 */
public class ArenaRegistry extends PluginArenaRegistry {

  private final Main plugin;

  public ArenaRegistry(Main plugin) {
    super(plugin);
    this.plugin = plugin;
  }


  @Override
  public void registerArenas() {
    for(Arena arena : getPluginArenas()) {
      arena.clearTextDisplays();
    }
    plugily.projects.murdermystery.utils.HalloweenChat.applyToLanguageConfig(plugin);
    super.registerArenas();
  }

  @Override
  public PluginArena getNewArena(String id) {
    return new Arena(id);
  }

  @Override
  public boolean additionalValidatorChecks(ConfigurationSection section, PluginArena arena, String id) {
    boolean checks = super.additionalValidatorChecks(section, arena, id);
    if(!checks) return false;

    if(!section.getBoolean(id + ".isdone")) {
      plugin.getDebugger().sendConsoleMsg(new MessageBuilder("VALIDATOR_INVALID_ARENA_CONFIGURATION").asKey().value("NOT VALIDATED").arena(arena).build());
      return false;
    }

    List<Location> playerSpawnPoints = new ArrayList<>();
    for(String loc : section.getStringList(id + ".playerspawnpoints")) {
      org.bukkit.Location serialized = LocationSerializer.getLocation(loc);

      // Ignore the arena if world is not exist at least in spawn points
      if(serialized == null || serialized.getWorld() == null) {
        section.set(id + ".isdone", false);
      } else {
        playerSpawnPoints.add(serialized);
      }
    }

    ((Arena) arena).setHideChances(section.getBoolean(id + ".hidechances"));
    arena.setArenaOption("MURDERER_DIVIDER",section.getInt(id + ".playerpermurderer", 5));
    ((Arena) arena).setPlayerSpawnPoints(playerSpawnPoints);
    if(!playerSpawnPoints.isEmpty()) {
      arena.setStartLocation(playerSpawnPoints.get(0));
    }

    ((Arena) arena).clearSpecificTaskLocations(null);
    ConfigurationSection tasksSec = section.getConfigurationSection(id + ".tasks");
    if(tasksSec != null) {
      for(String key : tasksSec.getKeys(false)) {
        TaskType type = TaskType.fromInput(key);
        if(type != null) {
          for(String locStr : tasksSec.getStringList(key)) {
            Location l = LocationSerializer.getLocation(locStr);
            if(l != null && l.getWorld() != null) {
              ((Arena) arena).removeTaskLocationAtBlock(l);
              ((Arena) arena).addSpecificTaskLocation(type, l);
            }
          }
        }
      }
    }

    List<Location> specificLoaded = ((Arena) arena).getAllTaskLocations();
    List<Location> taskLocations = new ArrayList<>();
    for(String loc : section.getStringList(id + ".tasklocations")) {
      Location l = LocationSerializer.getLocation(loc);
      if(l != null && l.getWorld() != null) {
        boolean alreadySpecific = specificLoaded.stream().anyMatch(s ->
          s.getWorld() != null && s.getWorld().equals(l.getWorld())
            && s.getBlockX() == l.getBlockX()
            && s.getBlockY() == l.getBlockY()
            && s.getBlockZ() == l.getBlockZ());
        if(!alreadySpecific) {
          taskLocations.add(l);
        }
      }
    }
    ((Arena) arena).setTaskLocations(taskLocations);

    ConfigurationSection sabotageSec = section.getConfigurationSection(id + ".sabotages");
    if(sabotageSec != null) {
      for(String key : sabotageSec.getKeys(false)) {
        SabotageType type = SabotageType.fromInput(key);
        if(type != null) {
          String locStr = sabotageSec.getString(key);
          if(locStr == null || locStr.isEmpty()) {
            List<String> legacyList = sabotageSec.getStringList(key);
            if(!legacyList.isEmpty()) {
              locStr = legacyList.get(0);
            }
          }
          if(locStr != null && !locStr.isEmpty()) {
            Location l = LocationSerializer.getLocation(locStr);
            if(l != null) {
              ((Arena) arena).setSabotageLocation(type, l);
            }
          }
        }
      }
    }

    String busHornStr = section.getString(id + ".bushorn");
    if(busHornStr == null || busHornStr.isEmpty()) {
      busHornStr = section.getString(id + ".campfire");
    }
    if(busHornStr != null && !busHornStr.isEmpty()) {
      Location busHornLoc = LocationSerializer.getLocation(busHornStr);
      if(busHornLoc != null) {
        ((Arena) arena).setCampfireLocation(busHornLoc);
      }
    }

    String meetingPointStr = section.getString(id + ".meetingpoint");
    if(meetingPointStr != null && !meetingPointStr.isEmpty()) {
      Location meetingLoc = LocationSerializer.getLocation(meetingPointStr);
      if(meetingLoc != null) {
        ((Arena) arena).setMeetingLocation(meetingLoc);
      }
    }

    List<Location> customBusSeats = new ArrayList<>();
    for(String seatStr : section.getStringList(id + ".busseats")) {
      if(seatStr != null && !seatStr.isEmpty()) {
        Location seatLoc = LocationSerializer.getLocation(seatStr);
        if(seatLoc != null) {
          customBusSeats.add(seatLoc);
        }
      }
    }
    if(!customBusSeats.isEmpty()) {
      ((Arena) arena).setBusSeatLocations(customBusSeats);
    }

    int customTasksPerPlayer = section.getInt(id + ".tasksperplayer", -1);
    if(customTasksPerPlayer > 0) {
      ((Arena) arena).setTasksPerPlayer(customTasksPerPlayer);
    }
    ((Arena) arena).setVisualTasksEnabled(
      section.getBoolean(id + ".visualtasks", plugin.getConfig().getBoolean("Tasks.Visual-Effects", true))
    );
    CutsceneManager cm = ((Arena) arena).getCutsceneManager();
    cm.setFrames(CutsceneManager.deserializeFrames(section.getStringList(id + ".cutscene")));

    String busStartStr = section.getString(id + ".buscutscene.start");
    if(busStartStr != null && !busStartStr.isEmpty()) {
      cm.setBusStartLocation(CutsceneManager.deserializePreciseLocation(busStartStr));
    }
    String busEndStr = section.getString(id + ".buscutscene.end");
    if(busEndStr != null && !busEndStr.isEmpty()) {
      cm.setBusEndLocation(CutsceneManager.deserializePreciseLocation(busEndStr));
    }
    if(section.contains(id + ".buscutscene.duration")) {
      cm.setBusDriveDurationSeconds(section.getDouble(id + ".buscutscene.duration", 8.0));
    }
    String busSchem = section.getString(id + ".buscutscene.schematic");
    if(busSchem != null && !busSchem.isEmpty()) {
      cm.setBusSchematicName(busSchem);
    }
    String hidePos1Str = section.getString(id + ".buscutscene.hidepos1");
    if(hidePos1Str != null && !hidePos1Str.isEmpty()) {
      cm.setBusHidePos1(LocationSerializer.getLocation(hidePos1Str));
    }
    String hidePos2Str = section.getString(id + ".buscutscene.hidepos2");
    if(hidePos2Str != null && !hidePos2Str.isEmpty()) {
      cm.setBusHidePos2(LocationSerializer.getLocation(hidePos2Str));
    }

    List<SpecialBlock> specialBlocks = new ArrayList<>();
    for(String loc : section.getStringList(id + ".mystery-cauldrons")) {
      specialBlocks.add(new SpecialBlock(LocationSerializer.getLocation(loc), SpecialBlock.SpecialBlockType.MYSTERY_CAULDRON));
    }
    for(String loc : section.getStringList(id + ".confessionals")) {
      specialBlocks.add(new SpecialBlock(LocationSerializer.getLocation(loc), SpecialBlock.SpecialBlockType.PRAISE_DEVELOPER));
    }
    for(String loc : section.getStringList(id + ".sewers")) {
      specialBlocks.add(new SpecialBlock(LocationSerializer.getLocation(loc), SpecialBlock.SpecialBlockType.RAPID_TELEPORTATION));
    }
    for(String loc : section.getStringList(id + ".rapid-teleportation")) {
      specialBlocks.add(new SpecialBlock(LocationSerializer.getLocation(loc), SpecialBlock.SpecialBlockType.RAPID_TELEPORTATION));
    }

    specialBlocks.forEach(((Arena) arena)::loadSpecialBlock);
    return true;
  }

  @Override
  public @Nullable Arena getArena(Player player) {
    IPluginArena pluginArena = super.getArena(player);
    if(pluginArena instanceof Arena) {
      return (Arena) pluginArena;
    }
    return null;
  }

  @Override
  public @Nullable Arena getArena(String id) {
    IPluginArena pluginArena = super.getArena(id);
    if(pluginArena instanceof Arena) {
      return (Arena) pluginArena;
    }
    return null;
  }

  public @NotNull List<Arena> getPluginArenas() {
    List<Arena> arenas = new ArrayList<>();
    for(IPluginArena pluginArena : super.getArenas()) {
      if(pluginArena instanceof Arena) {
        arenas.add((Arena) pluginArena);
      }
    }
    return arenas;
  }
}
