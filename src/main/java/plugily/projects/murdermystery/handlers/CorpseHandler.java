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

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.util.EulerAngle;
import org.jetbrains.annotations.Nullable;
import org.golde.bukkit.corpsereborn.CorpseAPI.CorpseAPI;
import org.golde.bukkit.corpsereborn.CorpseAPI.events.CorpseClickEvent;
import org.golde.bukkit.corpsereborn.CorpseAPI.events.CorpseSpawnEvent;
import org.golde.bukkit.corpsereborn.nms.Corpses;

import plugily.projects.minigamesbox.api.arena.IArenaState;
import plugily.projects.minigamesbox.api.user.IUser;
import plugily.projects.minigamesbox.classic.handlers.language.MessageBuilder;
import plugily.projects.minigamesbox.classic.utils.hologram.ArmorStandHologram;
import plugily.projects.minigamesbox.classic.utils.version.ServerVersion;
import plugily.projects.minigamesbox.classic.utils.version.VersionUtils;
import plugily.projects.minigamesbox.classic.utils.version.xseries.XMaterial;
import plugily.projects.murdermystery.Main;
import plugily.projects.murdermystery.api.events.game.MurderGameCorpseSpawnEvent;
import plugily.projects.murdermystery.arena.Arena;
import plugily.projects.murdermystery.HookManager;
import plugily.projects.murdermystery.arena.corpse.Corpse;
import plugily.projects.murdermystery.arena.corpse.Stand;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Plajer
 * <p>
 * Created at 07.10.2018
 */
public class CorpseHandler implements Listener {

  private final Main plugin;
  private Corpses.CorpseData lastSpawnedCorpse;

  private final Map<String, String> registeredLastWords = new HashMap<>();
  private final ItemStack head = XMaterial.PLAYER_HEAD.parseItem();

  public CorpseHandler(Main plugin) {
    this.plugin = plugin;
    //run bit later than hook manager to ensure it's not null
    Bukkit.getScheduler().runTaskLater(plugin, () -> {
      if(plugin.getHookManager().isFeatureEnabled(HookManager.HookFeature.CORPSES)) {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
      }
    }, 20 * 7);
  }

  public void registerLastWord(String permission, String lastWord) {
    registeredLastWords.put(permission, lastWord);
  }

  @SuppressWarnings("deprecation")
  public void spawnCorpse(Player player, Arena arena) {
    spawnCorpse(player, arena, null);
  }

  @SuppressWarnings("deprecation")
  public void spawnCorpse(Player player, Arena arena, @Nullable Player killer) {
    if(arena.getVotingManager().isVotingActive()) {
      return;
    }
    MurderGameCorpseSpawnEvent murderGameCorpseSpawnEvent = new MurderGameCorpseSpawnEvent(arena, player.getPlayer(), player.getLocation(), killer);
    Bukkit.getPluginManager().callEvent(murderGameCorpseSpawnEvent);
    if(murderGameCorpseSpawnEvent.isCancelled()) {
      return;
    }

    Location deathLoc = player.getLocation().clone();

    if(!plugin.getHookManager().isFeatureEnabled(HookManager.HookFeature.CORPSES)) {
      ArmorStand stand = deathLoc.getWorld().spawn(deathLoc.clone().add(0.0D, -1.25D, 0.0D), ArmorStand.class);
      SkullMeta meta = (SkullMeta) head.getItemMeta();
      meta = VersionUtils.setPlayerHead(player, meta);
      head.setItemMeta(meta);

      stand.setVisible(false);
      if(ServerVersion.Version.isCurrentEqualOrHigher(ServerVersion.Version.v1_16)) {
        stand.getEquipment().setHelmet(head);
      } else {
        stand.setHelmet(head);
      }
      stand.setGravity(false);
      stand.setCustomNameVisible(false);
      stand.setHeadPose(new EulerAngle(Math.toRadians(deathLoc.getX()), Math.toRadians(deathLoc.getPitch()), Math.toRadians(deathLoc.getZ())));

      ArmorStand hitboxStand = deathLoc.getWorld().spawn(deathLoc.clone().add(0.0D, -0.2D, 0.0D), ArmorStand.class);
      hitboxStand.setVisible(false);
      hitboxStand.setGravity(false);
      hitboxStand.setCustomNameVisible(false);

      plugin.getHologramManager().getArmorStands().add(stand);
      plugin.getHologramManager().getArmorStands().add(hitboxStand);

      ArmorStandHologram hologram = getLastWordsHologram(player);
      arena.addHead(new Stand(hologram, stand));
      arena.addHead(new Stand(null, hitboxStand));

      TextDisplay td = arena.spawnTextDisplay(deathLoc.clone().add(0, 1.15, 0),
        "&c&l ร่างไร้วิญญาณของ &f" + player.getName() + " &c&l\n&e[คลิกขวาหรือโจมตีเพื่อรายงานศพ / Report]");

      List<ArmorStand> corpseStands = new ArrayList<>();
      corpseStands.add(stand);
      corpseStands.add(hitboxStand);
      arena.getVotingManager().registerDeadBody(player.getName(), deathLoc, corpseStands, td, hologram);
      return;
    }
    ArmorStandHologram hologram = getLastWordsHologram(player);
    Corpses.CorpseData corpse = CorpseAPI.spawnCorpse(player, deathLoc);
    lastSpawnedCorpse = corpse;
    //spawns 2 corpses - Corpses.CorpseData corpse = lastSpawnedCorpse = CorpseAPI.spawnCorpse(player, player.getLocation());
    arena.addCorpse(new Corpse(hologram, corpse));
    TextDisplay td = arena.spawnTextDisplay(deathLoc.clone().add(0, 1.15, 0),
      "&c&l ร่างไร้วิญญาณของ &f" + player.getName() + " &c&l\n&e[คลิกขวาหรือโจมตีเพื่อรายงานศพ / Report]");
    arena.getVotingManager().registerDeadBody(player.getName(), deathLoc, new ArrayList<>(), td, hologram);
  }

  private ArmorStandHologram getLastWordsHologram(Player player) {
    ArmorStandHologram hologram = new ArmorStandHologram(player.getLocation());
    hologram.appendLine(new MessageBuilder(plugin.getLastWordsManager().getHologramTitle()).player(player).build());
    hologram.appendLine(plugin.getLastWordsManager().getRandomLastWord(player));
    return hologram;
  }

  @EventHandler
  public void onCorpseSpawn(CorpseSpawnEvent e) {
    if(lastSpawnedCorpse == null) {
      return;
    }
    if(plugin.getConfigPreferences().getOption("CORPSES_INTEGRATION_OVERWRITE") && !lastSpawnedCorpse.equals(e.getCorpse())) {
      e.setCancelled(true);
    }
  }

  @EventHandler
  public void onCorpseClick(CorpseClickEvent e) {
    if(plugin.getArenaRegistry().isInArena(e.getClicker())) {
      e.setCancelled(true);
      e.getClicker().closeInventory();

      Arena arena = plugin.getArenaRegistry().getArena(e.getClicker());
      if(arena != null && arena.getArenaState() == IArenaState.IN_GAME) {
        IUser user = plugin.getUserManager().getUser(e.getClicker());
        if(!user.isSpectator()) {
          arena.getVotingManager().startVoting(e.getClicker(), e.getCorpse().getCorpseName());
        }
      }
    }
  }

}
