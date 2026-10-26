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

package plugily.projects.murdermystery.handlers;

import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;
import de.oliver.fancynpcs.api.AttributeManager;
import de.oliver.fancynpcs.api.FancyNpcsPlugin;
import de.oliver.fancynpcs.api.Npc;
import de.oliver.fancynpcs.api.NpcAttribute;
import de.oliver.fancynpcs.api.NpcData;
import de.oliver.fancynpcs.api.actions.ActionTrigger;
import de.oliver.fancynpcs.api.events.NpcInteractEvent;
import de.oliver.fancynpcs.api.skins.SkinData;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Nullable;
import plugily.projects.minigamesbox.api.arena.IArenaState;
import plugily.projects.minigamesbox.api.user.IUser;
import plugily.projects.murdermystery.Main;
import plugily.projects.murdermystery.arena.Arena;
import plugily.projects.murdermystery.arena.ArenaUtils;
import plugily.projects.murdermystery.arena.role.Role;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class FancyNpcsCorpseHandler implements Listener, CorpseHandler.ExternalCorpseProvider {

  private static final long BODY_SPAWN_GRACE_MS = 1500L;
  private static final double SLEEPING_FLOOR_OFFSET_Y = 0.125D;

  private static final class CorpseNpcEntry {
    private final String npcId;
    private final Npc npc;
    private final Arena arena;
    private final String victimName;
    private final long spawnTimeMs;

    private CorpseNpcEntry(String npcId, Npc npc, Arena arena, String victimName) {
      this.npcId = npcId;
      this.npc = npc;
      this.arena = arena;
      this.victimName = victimName;
      this.spawnTimeMs = System.currentTimeMillis();
    }
  }

  private final Main plugin;
  private final Map<String, CorpseNpcEntry> activeCorpseNpcs = new ConcurrentHashMap<>();

  public FancyNpcsCorpseHandler(Main plugin) {
    this.plugin = plugin;
    plugin.getServer().getPluginManager().registerEvents(this, plugin);
  }

  @Nullable
  public Runnable spawnCorpse(Player victim, Arena arena, Location groundLoc) {
    if(victim == null || arena == null || groundLoc == null || groundLoc.getWorld() == null) {
      return null;
    }
    try {
      FancyNpcsPlugin api = FancyNpcsPlugin.get();
      if(api == null || api.getNpcAdapter() == null || api.getNpcManager() == null) {
        return null;
      }

      Location corpseLoc = groundLoc.clone().add(0.0D, SLEEPING_FLOOR_OFFSET_Y, 0.0D);
      corpseLoc.setYaw(victim.getLocation().getYaw());
      corpseLoc.setPitch(0.0F);

      String npcName = "mm_corpse_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
      NpcData data = new NpcData(npcName, victim.getUniqueId(), corpseLoc);
      data.setType(EntityType.PLAYER);
      data.setDisplayName("<empty>");
      data.setShowInTab(false);
      data.setCollidable(false);
      data.setTurnToPlayer(false);
      data.setInteractionCooldown(0.25F);
      data.setVisibilityDistance(64);

      SkinData skinData = extractPlayerSkin(victim);
      if(skinData != null) {
        data.setSkinData(skinData);
      } else {
        data.setSkin(victim.getName(), SkinData.SkinVariant.AUTO);
      }

      AttributeManager attributeManager = api.getAttributeManager();
      if(attributeManager != null) {
        NpcAttribute poseAttr = attributeManager.getAttributeByName(EntityType.PLAYER, "pose");
        if(poseAttr != null) {
          data.addAttribute(poseAttr, "sleeping");
        }
        NpcAttribute onGroundAttr = attributeManager.getAttributeByName(EntityType.PLAYER, "on_ground");
        if(onGroundAttr != null) {
          data.addAttribute(onGroundAttr, "true");
        }
      }

      Npc npc = api.getNpcAdapter().apply(data);
      if(npc == null) {
        return null;
      }
      npc.setSaveToFile(false);
      npc.create();
      api.getNpcManager().registerNpc(npc);
      npc.spawnForAll();

      String npcId = data.getId();
      activeCorpseNpcs.put(npcId, new CorpseNpcEntry(npcId, npc, arena, victim.getName()));
      return () -> removeCorpseNpc(npcId);
    } catch(Throwable t) {
      plugin.getDebugger().debug("[FancyNpcsCorpseHandler] Failed to spawn FancyNpcs corpse: {0}", t.getMessage());
      return null;
    }
  }

  @Nullable
  private SkinData extractPlayerSkin(Player player) {
    try {
      PlayerProfile profile = player.getPlayerProfile();
      for(ProfileProperty prop : profile.getProperties()) {
        if("textures".equalsIgnoreCase(prop.getName())) {
          String value = prop.getValue();
          String signature = prop.getSignature();
          if(value != null && !value.isEmpty() && signature != null && !signature.isEmpty()) {
            return new SkinData(player.getUniqueId().toString(), SkinData.SkinVariant.AUTO, value, signature);
          }
        }
      }
    } catch(Throwable ignored) {
    }
    return null;
  }

  public void removeCorpseNpc(String npcId) {
    if(npcId == null) {
      return;
    }
    CorpseNpcEntry entry = activeCorpseNpcs.remove(npcId);
    if(entry == null || entry.npc == null) {
      return;
    }
    try {
      FancyNpcsPlugin api = FancyNpcsPlugin.get();
      if(api != null && api.getNpcManager() != null) {
        api.getNpcManager().removeNpc(entry.npc);
      }
    } catch(Throwable ignored) {
    }
    try {
      entry.npc.removeForAll();
    } catch(Throwable ignored) {
    }
  }

  public void clearArenaCorpses(Arena arena) {
    if(arena == null) {
      return;
    }
    List<String> toRemove = new ArrayList<>();
    for(CorpseNpcEntry entry : activeCorpseNpcs.values()) {
      if(arena.equals(entry.arena)) {
        toRemove.add(entry.npcId);
      }
    }
    for(String npcId : toRemove) {
      removeCorpseNpc(npcId);
    }
  }

  public void shutdown() {
    List<String> allIds = new ArrayList<>(activeCorpseNpcs.keySet());
    for(String npcId : allIds) {
      removeCorpseNpc(npcId);
    }
  }

  @EventHandler
  public void onNpcInteract(NpcInteractEvent event) {
    Npc npc = event.getNpc();
    if(npc == null || npc.getData() == null) {
      return;
    }
    CorpseNpcEntry entry = activeCorpseNpcs.get(npc.getData().getId());
    if(entry == null) {
      return;
    }
    event.setCancelled(true);

    Player clicker = event.getPlayer();
    ActionTrigger trigger = event.getInteractionType();
    if(Bukkit.isPrimaryThread()) {
      handleCorpseClick(clicker, entry, trigger);
    } else {
      Bukkit.getScheduler().runTask(plugin, () -> handleCorpseClick(clicker, entry, trigger));
    }
  }

  private void handleCorpseClick(Player clicker, CorpseNpcEntry entry, ActionTrigger trigger) {
    if(clicker == null || !clicker.isOnline()) {
      return;
    }
    if(System.currentTimeMillis() - entry.spawnTimeMs < BODY_SPAWN_GRACE_MS) {
      return;
    }
    Arena arena = plugin.getArenaRegistry().getArena(clicker);
    if(arena == null || !arena.equals(entry.arena) || arena.getArenaState() != IArenaState.IN_GAME) {
      return;
    }
    if(ArenaUtils.isSpectator(clicker, arena)) {
      return;
    }
    if(arena.getVotingManager().isVotingActive()
      || arena.getCutsceneManager().isPlaying()
      || (arena.getVentManager() != null && arena.getVentManager().isInsideVent(clicker))) {
      return;
    }
    if(arena.getVotingManager().isOnKillReportCooldown(clicker)) {
      return;
    }
    if(trigger == ActionTrigger.LEFT_CLICK) {
      IUser user = plugin.getUserManager().getUser(clicker);
      ItemStack inHand = clicker.getInventory().getItemInMainHand();
      ItemStack murdererSword = plugin.getSwordSkinManager().getMurdererSword(clicker);
      if(Role.isRole(Role.MURDERER, user, arena)
        && inHand != null
        && murdererSword != null
        && inHand.getType() == murdererSword.getType()) {
        return;
      }
    }
    arena.getVotingManager().startVoting(clicker, entry.victimName);
  }
}
