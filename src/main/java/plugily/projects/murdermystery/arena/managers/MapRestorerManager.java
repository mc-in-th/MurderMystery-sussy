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

import org.bukkit.entity.Item;
import org.golde.bukkit.corpsereborn.CorpseAPI.CorpseAPI;
import plugily.projects.minigamesbox.classic.arena.managers.PluginMapRestorerManager;
import plugily.projects.murdermystery.HookManager;
import plugily.projects.murdermystery.arena.Arena;
import plugily.projects.murdermystery.arena.corpse.Corpse;
import plugily.projects.murdermystery.arena.corpse.Stand;

import java.util.Objects;

public class MapRestorerManager extends PluginMapRestorerManager {

  public final Arena arena;

  public MapRestorerManager(Arena arena) {
    super(arena);
    this.arena = arena;
  }

  @Override
  public void fullyRestoreArena() {
    cleanUpArena();
    super.fullyRestoreArena();
  }


  public void cleanUpArena() {
    removeBowHolo();
    arena.setMurdererLocatorReceived(false);
    arena.getDetectiveContributions().clear();
    arena.getMurdererContributions().clear();
    arena.getGameCharacters().clear();
    arena.getMurdererList().clear();
    arena.getDetectiveList().clear();
    arena.getDeaths().clear();
    arena.getSpectators().clear();
    arena.setDetectiveDead(false);
    arena.setSabotageWin(false);
    arena.setTaskWin(false);
    arena.getTaskManager().cleanup();
    arena.getVotingManager().cleanup();
    arena.getVentManager().cleanup();
    arena.getMinimap().cleanup();
    arena.getSabotageManager().cleanup();
    arena.clearTextDisplays();
    arena.clearGlowEntries();
    arena.spawnCampfireHologram();
    clearCorpses();
    clearGold();
  }

  public void removeBowHolo() {
    if(arena.getBowHologram() != null) {
      arena.getPlugin().getHologramManager().getHolograms().remove(arena.getBowHologram());
      if(!arena.getBowHologram().isDeleted()) {
        arena.getBowHologram().delete();
      }
    }
    arena.setBowHologram(null);
  }

  public void clearGold() {
    arena.getGoldSpawned().stream().filter(Objects::nonNull).forEach(Item::remove);
    arena.getGoldSpawned().clear();
  }

  public void clearCorpses() {
    if(arena.getPlugin().getCorpseHandler() != null) {
      arena.getPlugin().getCorpseHandler().clearArenaCorpses(arena);
    }
    if(!arena.getPlugin().getHookManager().isFeatureEnabled(HookManager.HookFeature.CORPSES)) {
      for(Stand stand : arena.getStands()) {
        if(stand == null) {
          continue;
        }
        if(stand.getHologram() != null) {
          arena.getPlugin().getHologramManager().getHolograms().remove(stand.getHologram());
          if(!stand.getHologram().isDeleted()) {
            stand.getHologram().delete();
          }
        }
        if(stand.getStand() != null) {
          arena.getPlugin().getHologramManager().getArmorStands().remove(stand.getStand());
          stand.getStand().remove();
        }
      }
      arena.getStands().clear();
      return;
    }
    for(Corpse corpse : arena.getCorpses()) {
      if(corpse == null) {
        continue;
      }
      if(corpse.getHologram() != null) {
        arena.getPlugin().getHologramManager().getHolograms().remove(corpse.getHologram());
        if(!corpse.getHologram().isDeleted()) {
          corpse.getHologram().delete();
        }
      }
      if(corpse.getCorpseData() != null) {
        corpse.getCorpseData().destroyCorpseFromEveryone();
        CorpseAPI.removeCorpse(corpse.getCorpseData());
      }
    }
    arena.getCorpses().clear();
  }
}
