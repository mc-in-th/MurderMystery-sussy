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

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.util.Vector;
import plugily.projects.minigamesbox.api.arena.IArenaState;
import plugily.projects.minigamesbox.api.arena.IPluginArena;
import plugily.projects.minigamesbox.api.user.IUser;
import plugily.projects.minigamesbox.classic.arena.PluginArenaUtils;
import plugily.projects.minigamesbox.classic.handlers.items.SpecialItem;
import plugily.projects.minigamesbox.classic.handlers.language.MessageBuilder;
import plugily.projects.minigamesbox.classic.utils.hologram.ArmorStandHologram;
import plugily.projects.minigamesbox.classic.utils.misc.complement.ComplementAccessor;
import plugily.projects.minigamesbox.classic.utils.version.VersionUtils;
import plugily.projects.murdermystery.arena.cutscene.CutsceneManager;
import plugily.projects.murdermystery.arena.managers.MapRestorerManager;
import plugily.projects.murdermystery.arena.role.Role;
import plugily.projects.murdermystery.utils.HalloweenChat;
import plugily.projects.murdermystery.utils.ItemPosition;

import java.util.List;

/**
 * @author Plajer
 * <p>Created at 13.03.2018
 */
public class ArenaUtils extends PluginArenaUtils {

  private ArenaUtils() {
    super();
  }

  public static void playRoleRevealSound(Player player, boolean murderer) {
    if(player == null || !player.isOnline()) {
      return;
    }
    if(murderer) {
      player.playSound(player.getLocation(), Sound.ENTITY_WITHER_SPAWN, 0.45f, 0.75f);
      player.playSound(player.getLocation(), Sound.BLOCK_RESPAWN_ANCHOR_DEPLETE, 0.85f, 0.6f);
      player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASEDRUM, 1.0f, 0.6f);
      Bukkit.getScheduler().runTaskLater(getPlugin(), () -> {
        if(player.isOnline()) {
          player.playSound(player.getLocation(), Sound.BLOCK_BELL_RESONATE, 0.8f, 0.55f);
          player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_DIDGERIDOO, 0.9f, 0.55f);
        }
      }, 4L);
      Bukkit.getScheduler().runTaskLater(getPlugin(), () -> {
        if(player.isOnline()) {
          player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 0.7f, 0.75f);
        }
      }, 8L);
    } else {
      player.playSound(player.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 0.85f, 1.25f);
      player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 1.0f, 0.9f);
      Bukkit.getScheduler().runTaskLater(getPlugin(), () -> {
        if(player.isOnline()) {
          player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 1.0f, 1.2f);
          player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_BELL, 0.85f, 1.1f);
        }
      }, 4L);
      Bukkit.getScheduler().runTaskLater(getPlugin(), () -> {
        if(player.isOnline()) {
          player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_CHIME, 1.0f, 1.5f);
          player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.65f, 1.35f);
        }
      }, 8L);
    }
  }

  public static void onMurdererDeath(Arena arena) {
    String winTitle = "<shadow:#000000FF><#5A4E7C>[</shadow><#6EEB83>ผู้โดยสารเป็นฝ่ายชนะ<shadow:#000000FF><#5A4E7C>]</shadow>";
    String winSub = new MessageBuilder("IN_GAME_MESSAGES_GAME_END_PLACEHOLDERS_MURDERER_STOPPED").asKey().build();
    for(Player player : arena.getPlayers()) {
      HalloweenChat.sendTitles(player, winTitle, winSub, 10, 60, 10);
    }
    //we must call it ticks later due to instant respawn bug
    Bukkit.getScheduler().runTask(getPlugin(), () -> getPlugin().getArenaManager().stopGame(false, arena));
  }

  public static void updateInnocentLocator(Arena arena) {
    java.util.List<Player> list = arena.getPlayersLeft();

    if(!arena.isMurdererLocatorReceived()) {
      ItemStack innocentLocator = new ItemStack(Material.COMPASS, 1);
      ItemMeta innocentMeta = innocentLocator.getItemMeta();
      ComplementAccessor.getComplement()
        .setDisplayName(
          innocentMeta,
          new MessageBuilder("IN_GAME_MESSAGES_ARENA_LOCATOR_INNOCENT").asKey().build());
      innocentLocator.setItemMeta(innocentMeta);
      for(Player p : list) {
        if(arena.isMurderAlive(p)) {
          if(arena.getVentManager() != null && arena.getVentManager().isInsideVent(p)) {
            arena.getVentManager().updateSavedItem(p, ItemPosition.INNOCENTS_LOCATOR.getMurdererItemPosition(), innocentLocator);
          } else {
            ItemPosition.setItem(getPlugin().getUserManager().getUser(p), ItemPosition.INNOCENTS_LOCATOR, innocentLocator);
          }
        }
      }
      arena.setMurdererLocatorReceived(true);

      for(Player p : list) {
        if(Role.isRole(Role.MURDERER, getPlugin().getUserManager().getUser(p), arena)) {
          continue;
        }
        HalloweenChat.sendTitleEntry(p, new MessageBuilder("IN_GAME_MESSAGES_ARENA_LOCATOR_WATCH_OUT")
          .asKey()
          .player(p)
          .arena(arena)
          .build());
      }
    }

    for(Player p : list) {
      if(Role.isRole(Role.MURDERER, getPlugin().getUserManager().getUser(p), arena)) {
        continue;
      }
      for(Player murder : arena.getMurdererList()) {
        if(arena.isMurderAlive(murder)) {
          murder.setCompassTarget(p.getLocation());
        }
      }
      break;
    }
  }

  public static void dropBowAndAnnounce(Arena arena, Player victim) {
    if(arena.getBowHologram() != null) {
      return;
    }

    for(Player player : arena.getPlayers()) {
      IUser user = arena.getPlugin().getUserManager().getUser(player);
      if(Role.isRole(Role.MURDERER, user, arena)) {
        continue;
      }
      HalloweenChat.sendTitleEntry(player, new MessageBuilder("IN_GAME_MESSAGES_ARENA_PLAYING_BOW_DROPPED").asKey().arena(arena).build());
    }

    ArmorStandHologram hologram =
      new ArmorStandHologram(victim.getLocation()).appendItem(new ItemStack(Material.BOW, 1));
    hologram.getArmorStands().forEach(stand -> stand.setPersistent(false));
    if(hologram.getEntityItem() != null) {
      hologram.getEntityItem().setPersistent(false);
    }
    hologram.setPickupHandler(picker -> {
      if(arena.getArenaState() != IArenaState.IN_GAME || isSpectator(picker, arena) || arena.getVotingManager().isVotingActive()) {
        return;
      }
      IUser pickerUser = arena.getPlugin().getUserManager().getUser(picker);
      if(pickerUser == null || Role.isRole(Role.MURDERER, pickerUser, arena)) {
        return;
      }
      ((MapRestorerManager) arena.getMapRestorerManager()).removeBowHolo();
      arena.setCharacter(Arena.CharacterType.FAKE_DETECTIVE, picker);
      ItemPosition.setItem(pickerUser, ItemPosition.BOW, new ItemStack(Material.BOW, 1));
      ItemPosition.setItem(pickerUser, ItemPosition.INFINITE_ARROWS, new ItemStack(Material.ARROW, arena.getPlugin().getConfig().getInt("Bow.Amount.Arrows.Detective", 3)));
      for(Player p : arena.getPlayersLeft()) {
        IUser u = getPlugin().getUserManager().getUser(p);
        if(Role.isRole(Role.INNOCENT, u, arena)) {
          ItemPosition.setItem(u, ItemPosition.BOW_LOCATOR, null);
        }
      }
    });
    getPlugin().getHologramManager().getHolograms().add(hologram);

    arena.setBowHologram(hologram);
    addBowLocator(arena, hologram.getLocation());
  }

  private static void addBowLocator(Arena arena, Location loc) {
    ItemStack bowLocator = new ItemStack(Material.COMPASS, 1);
    ItemMeta bowMeta = bowLocator.getItemMeta();
    ComplementAccessor.getComplement()
      .setDisplayName(
        bowMeta, new MessageBuilder("IN_GAME_MESSAGES_ARENA_LOCATOR_BOW").asKey().build());
    bowLocator.setItemMeta(bowMeta);
    for(Player p : arena.getPlayersLeft()) {
      IUser user = getPlugin().getUserManager().getUser(p);
      if(Role.isRole(Role.INNOCENT, user, arena)) {
        ItemPosition.setItem(user, ItemPosition.BOW_LOCATOR, bowLocator);
        p.setCompassTarget(loc);
      }
    }
  }

  public static void giveSpectatorLoadout(Player player, Arena arena) {
    if(player == null || !player.isOnline() || arena == null) {
      return;
    }
    arena.getPlugin().getSpecialItemManager().addSpecialItemsOfStage(player, SpecialItem.DisplayStage.SPECTATOR);
    if(arena.getTaskManager().hasAssignedTasks(player) && arena.getPlayersLeft().size() > arena.aliveMurderer()) {
      arena.getTaskManager().giveTaskPointer(player);
      if(arena.getMinimap() != null) {
        player.getInventory().setItemInOffHand(arena.getMinimap().createMapItem(player));
      }
      arena.getTaskManager().updatePlayerStationVisibility(player);
    }
    player.updateInventory();
  }

  public static void forceEliminatePlayer(Player player, Arena arena) {
    if(player == null || arena == null || arena.isDeathPlayer(player)) {
      return;
    }
    if(arena.getVentManager() != null && arena.getVentManager().isInsideVent(player)) {
      arena.getVentManager().forceExitForMeeting(player);
    }
    for(PotionEffect effect : player.getActivePotionEffects()) {
      player.removePotionEffect(effect.getType());
    }
    IUser user = getPlugin().getUserManager().getUser(player);
    if(user != null) {
      user.adjustStatistic("DEATHS", 1);
      user.setSpectator(true);
    }
    arena.addDeathPlayer(player);
    arena.addSpectatorPlayer(player);

    player.closeInventory();
    player.getInventory().clear();
    player.getInventory().setArmorContents(null);
    player.getInventory().setItemInOffHand(null);
    arena.getTaskManager().handlePlayerDeath(player);

    hidePlayer(player, arena);
    applySpectatorState(player, arena, true);
    arena.getPlugin().getRewardsHandler().performReward(player, arena.getPlugin().getRewardsHandler().getRewardType("PLAYER_DEATH"));
    giveSpectatorLoadout(player, arena);
    arena.getScoreboardManager().updateScoreboards();
  }

  public static boolean isSpectator(Player player, Arena arena) {
    if(player == null || arena == null) {
      return false;
    }
    if(arena.isDeathPlayer(player) || arena.isSpectatorPlayer(player)) {
      return true;
    }
    IUser user = getPlugin().getUserManager().getUser(player);
    return user != null && (user.isSpectator() || user.isPermanentSpectator());
  }

  public static void applySpectatorState(Player player, Arena arena, boolean startFlying) {
    if(player == null || !player.isOnline() || arena == null || !arena.getPlayers().contains(player)) {
      return;
    }
    IArenaState state = arena.getArenaState();
    if(state == IArenaState.WAITING_FOR_PLAYERS || state == IArenaState.ENDING || state == IArenaState.RESTARTING) {
      return;
    }
    IUser user = getPlugin().getUserManager().getUser(player);
    if((state == IArenaState.STARTING || state == IArenaState.FULL_GAME)
      && (user == null || (!user.isSpectator() && !user.isPermanentSpectator() && !arena.isSpectatorPlayer(player)))) {
      return;
    }

    if(user != null) {
      user.setSpectator(true);
    }
    arena.addSpectatorPlayer(player);

    if(CutsceneManager.isPreviewing(player)
      || (arena.getCutsceneManager().isPlaying() && player.getGameMode() == GameMode.SPECTATOR)) {
      return;
    }

    List<Player> firstPersonList = getPlugin().getSpectatorItemsManager().getSpectatorSettingsMenu().firstPersonMode;
    boolean inFirstPerson = (player.getGameMode() == GameMode.SPECTATOR && player.getSpectatorTarget() != null)
      || (firstPersonList != null && firstPersonList.contains(player));

    if(inFirstPerson && player.getSpectatorTarget() instanceof Player) {
      Player specTarget = (Player) player.getSpectatorTarget();
      if(arena.getVentManager() != null && arena.getVentManager().isInsideVent(specTarget)) {
        player.setSpectatorTarget(null);
        if(firstPersonList != null) {
          firstPersonList.remove(player);
        }
        inFirstPerson = false;
      }
    }

    if(inFirstPerson) {
      if(player.getGameMode() != GameMode.SPECTATOR) {
        player.setGameMode(GameMode.SPECTATOR);
      }
      for(Player other : arena.getPlayers()) {
        if(other != null && other.isOnline() && !other.equals(player) && !isSpectator(other, arena)) {
          VersionUtils.hidePlayer(getPlugin(), other, player);
        }
      }
      return;
    }

    if(player.getGameMode() != GameMode.ADVENTURE) {
      player.setGameMode(GameMode.ADVENTURE);
    }
    VersionUtils.setCollidable(player, false);
    player.setFallDistance(0.0f);
    player.setFireTicks(0);
    if(player.getFlySpeed() < 0.15f) {
      player.setFlySpeed(0.15f);
    }
    if(player.getWalkSpeed() <= 0.05f) {
      player.setWalkSpeed(0.2f);
    }
    if(!player.getAllowFlight()) {
      player.setAllowFlight(true);
    }
    if(startFlying) {
      player.setAllowFlight(true);
      if(player.isOnGround()) {
        player.setVelocity(new Vector(0.0, 0.18, 0.0));
      }
      player.setFlying(true);
    }

    List<Player> invisibleSpecs = getPlugin().getSpectatorItemsManager().getSpectatorSettingsMenu().invisibleSpectators;
    for(Player other : arena.getPlayers()) {
      if(other == null || !other.isOnline() || other.equals(player)) {
        continue;
      }
      if(!isSpectator(other, arena)) {
        VersionUtils.hidePlayer(getPlugin(), other, player);
        if(arena.getVentManager() == null || !arena.getVentManager().isInsideVent(other)) {
          VersionUtils.showPlayer(getPlugin(), player, other);
        } else {
          VersionUtils.hidePlayer(getPlugin(), player, other);
        }
      } else {
        if(invisibleSpecs != null && invisibleSpecs.contains(other)) {
          VersionUtils.hidePlayer(getPlugin(), other, player);
        } else {
          VersionUtils.showPlayer(getPlugin(), other, player);
        }
        if(invisibleSpecs != null && invisibleSpecs.contains(player)) {
          VersionUtils.hidePlayer(getPlugin(), player, other);
        } else {
          VersionUtils.showPlayer(getPlugin(), player, other);
        }
      }
    }
  }

  public static void updateNameTagsVisibility(final Player p) {
    if(!getPlugin().getConfigPreferences().getOption("HIDE_NAMETAGS")) {
      return;
    }
    for(Player players : getPlugin().getServer().getOnlinePlayers()) {
      IPluginArena arena = getPlugin().getArenaRegistry().getArena(players);
      if(arena == null) {
        continue;
      }
      VersionUtils.updateNameTagsVisibility(
        p, players, "MMHide", arena.getArenaState() != IArenaState.IN_GAME);
    }
  }
}
