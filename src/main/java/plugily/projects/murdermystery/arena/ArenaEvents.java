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

import com.destroystokyo.paper.event.player.PlayerStopSpectatingEntityEvent;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Display;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.player.PlayerToggleFlightEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import plugily.projects.minigamesbox.api.arena.IArenaState;
import plugily.projects.minigamesbox.api.arena.IPluginArena;
import plugily.projects.minigamesbox.api.user.IUser;
import plugily.projects.minigamesbox.classic.arena.PluginArenaEvents;
import plugily.projects.minigamesbox.classic.handlers.items.SpecialItem;
import plugily.projects.minigamesbox.classic.handlers.language.MessageBuilder;
import plugily.projects.minigamesbox.classic.utils.misc.complement.ComplementAccessor;
import plugily.projects.minigamesbox.classic.utils.version.ServerVersion;
import plugily.projects.minigamesbox.classic.utils.version.VersionUtils;
import plugily.projects.minigamesbox.classic.utils.version.events.api.PlugilyEntityPickupItemEvent;
import plugily.projects.minigamesbox.classic.utils.version.events.api.PlugilyPlayerPickupArrow;
import plugily.projects.minigamesbox.classic.utils.version.xseries.XPotion;
import plugily.projects.minigamesbox.classic.utils.version.xseries.XSound;
import plugily.projects.minigamesbox.classic.utils.version.xseries.inventory.XInventoryView;
import plugily.projects.murdermystery.Main;
import plugily.projects.murdermystery.arena.corpse.Stand;
import plugily.projects.murdermystery.arena.cutscene.CutsceneManager;
import plugily.projects.murdermystery.arena.role.Role;
import plugily.projects.murdermystery.arena.sabotage.SabotageType;
import plugily.projects.murdermystery.arena.special.SpecialBlock;
import plugily.projects.murdermystery.arena.special.pray.PrayerRegistry;
import plugily.projects.murdermystery.arena.voting.VotingManager;
import plugily.projects.murdermystery.utils.HalloweenChat;
import plugily.projects.murdermystery.utils.ItemPosition;

/**
 * @author Plajer
 * <p>Created at 13.03.2018
 */
public class ArenaEvents extends PluginArenaEvents {

  private final Main plugin;

  public ArenaEvents(Main plugin) {
    super(plugin);
    this.plugin = plugin;
    plugin.getServer().getPluginManager().registerEvents(this, plugin);
  }

  @Override
  public void handleIngameVoidDeath(Player victim, IPluginArena arena) {
    Arena pluginArena = plugin.getArenaRegistry().getArena(arena.getId());
    if(pluginArena == null) {
      return;
    }
    Location safeSpawn = !pluginArena.getPlayerSpawnPoints().isEmpty()
      ? pluginArena.getPlayerSpawnPoints().get(0)
      : pluginArena.getStartLocation();
    if(ArenaUtils.isSpectator(victim, pluginArena)) {
      if(safeSpawn != null) {
        VersionUtils.teleport(victim, safeSpawn).thenAccept(ignored ->
          Bukkit.getScheduler().runTaskLater(plugin, () -> ArenaUtils.applySpectatorState(victim, pluginArena, true), 1L));
      }
      return;
    }
    victim.damage(1000.0);
    if(arena.getArenaState() == IArenaState.IN_GAME && safeSpawn != null) {
      VersionUtils.teleport(victim, safeSpawn).thenAccept(ignored ->
        Bukkit.getScheduler().runTaskLater(plugin, () -> ArenaUtils.applySpectatorState(victim, pluginArena, true), 1L));
    }
  }

  @EventHandler
  public void onBowShot(EntityShootBowEvent event) {
    if(event.getEntityType() != EntityType.PLAYER) {
      return;
    }
    Player player = (Player) event.getEntity();
    IUser user = plugin.getUserManager().getUser(player);
    if(!Role.isRole(Role.ANY_DETECTIVE, user)) {
      return;
    }
    if(user.getCooldown("bow_shot") > 0) {
      event.setCancelled(true);
      return;
    }
    int bowCooldown = plugin.getConfig().getInt("Bow.Cooldown", 5);
    if(bowCooldown <= 0) {
      return;
    }
    user.setCooldown("bow_shot", bowCooldown);
    plugin.getBukkitHelper().applyActionBarCooldown(player, bowCooldown);
    VersionUtils.setMaterialCooldown(player, event.getBow().getType(), 20 * (plugin.getConfig().getInt("Bow.Cooldown", 5)));
  }

  @EventHandler
  public void onArrowPickup(PlugilyPlayerPickupArrow e) {
    if(plugin.getArenaRegistry().isInArena(e.getPlayer())) {
      e.getItem().remove();
      e.setCancelled(true);
    }
  }

  @EventHandler
  public void onItemPickup(PlugilyEntityPickupItemEvent e) {
    if(!(e.getEntity() instanceof Player)) {
      return;
    }
    Player player = (Player) e.getEntity();
    Arena arena = plugin.getArenaRegistry().getArena(player);
    if(arena == null) {
      return;
    }
    e.setCancelled(true);
  }


  @EventHandler
  public void onMurdererDamage(EntityDamageByEntityEvent e) {
    if(e.getDamager() instanceof Player && e.getEntity() instanceof ArmorStand) {
      Player clicker = (Player) e.getDamager();
      Arena arena = plugin.getArenaRegistry().getArena(clicker);
      if(arena != null && arena.getArenaState() == IArenaState.IN_GAME) {
        e.setCancelled(true);
        IUser clickerUser = plugin.getUserManager().getUser(clicker);
        if(!clickerUser.isSpectator()) {
          arena.getVotingManager().tryReportNearbyBody(clicker, e.getEntity().getLocation());
        }
        return;
      }
    }
    if(!(e.getDamager() instanceof Player) || e.getEntityType() != EntityType.PLAYER) {
      return;
    }
    Player attacker = (Player) e.getDamager();
    IUser userAttacker = plugin.getUserManager().getUser(attacker);
    Player victim = (Player) e.getEntity();
    IUser userVictim = plugin.getUserManager().getUser(victim);
    if(!ArenaUtils.areInSameArena(attacker, victim)) {
      return;
    }
    //we are killing player via damage() method so event can be cancelled safely, will work for detective damage murderer and others
    e.setCancelled(true);

    Arena checkArena = plugin.getArenaRegistry().getArena(attacker);
    if(checkArena == null || checkArena.getArenaState() != IArenaState.IN_GAME
      || checkArena.getVotingManager().isVotingActive() || checkArena.getCutsceneManager().isPlaying()
      || (checkArena.getVentManager() != null && checkArena.getVentManager().isInsideVent(attacker))) {
      return;
    }

    if(userAttacker.isSpectator() || userVictim.isSpectator() || checkArena.isDeathPlayer(attacker) || checkArena.isDeathPlayer(victim)) {
      return;
    }

    //better check this for future even if anyone else cannot use sword
    if(!Role.isRole(Role.MURDERER, userAttacker)) {
      return;
    }

    //check if victim is murderer
    if(Role.isRole(Role.MURDERER, userVictim)) {
      return;
    }
    if(VersionUtils.getItemInHand(attacker) == null || plugin.getSwordSkinManager().getMurdererSword(attacker) == null) {
      return;
    }
    //just don't kill user if item isn't murderer sword
    if(VersionUtils.getItemInHand(attacker).getType() != plugin.getSwordSkinManager().getMurdererSword(attacker).getType()) {
      return;
    }

    //check if sword has cooldown
    if(ServerVersion.Version.isCurrentLower(ServerVersion.Version.v1_11)) {
      if(plugin.getUserManager().getUser(attacker).getCooldown("sword_attack") > 0) {
        return;
      }
    } else if(attacker.hasCooldown(plugin.getSwordSkinManager().getMurdererSword(attacker).getType())) {
      return;
    }

    if(Role.isRole(Role.MURDERER, userVictim)) {
      plugin.getRewardsHandler().performReward(attacker, plugin.getRewardsHandler().getRewardType("KILL_MURDERER"));
    }

    if(checkArena != null) {
      checkArena.getVotingManager().recordKill(attacker);
    }

    XSound.ENTITY_PLAYER_DEATH.play(victim.getLocation());
    victim.damage(100.0);

    if(!checkArena.getVotingManager().isVotingActive() && checkArena.getArenaState() == IArenaState.IN_GAME) {
      HalloweenChat.sendTitles(
        victim,
        new MessageBuilder("IN_GAME_DEATH_SCREEN").asKey().player(victim).arena(checkArena).build(),
        new MessageBuilder("IN_GAME_MESSAGES_GAME_END_PLACEHOLDERS_MURDERER_KILLED_YOU").asKey().build(),
        5, 50, 15
      );
    }

    IUser user = plugin.getUserManager().getUser(attacker);

    user.adjustStatistic("KILLS", 1);
    user.adjustStatistic("LOCAL_KILLS", 1);
  }


  @EventHandler
  public void onArrowDamage(EntityDamageByEntityEvent e) {
    if(!(e.getDamager() instanceof Arrow)) {
      return;
    }
    if(!(((Arrow) e.getDamager()).getShooter() instanceof Player)) {
      return;
    }
    Player attacker = (Player) ((Arrow) e.getDamager()).getShooter();
    IUser userAttacker = plugin.getUserManager().getUser(attacker);
    if(plugin.getArenaRegistry().isInArena(attacker)) {
      e.setCancelled(true);
      e.getDamager().remove();
    }
    if(e.getEntityType() != EntityType.PLAYER) {
      return;
    }
    Player victim = (Player) e.getEntity();
    IUser userVictim = plugin.getUserManager().getUser(victim);
    if(!ArenaUtils.areInSameArena(attacker, victim)) {
      return;
    }
    //we won't allow to suicide
    if(attacker.equals(victim)) {
      e.setCancelled(true);
      return;
    }
    //dont kill murderer on bow damage if attacker is murderer
    if(Role.isRole(Role.MURDERER, userAttacker) && Role.isRole(Role.MURDERER, userVictim)) {
      e.setCancelled(true);
      return;
    }
    Arena arena = plugin.getArenaRegistry().getArena(attacker);
    if(arena == null || arena.getArenaState() != IArenaState.IN_GAME || arena.getVotingManager().isVotingActive() || arena.getCutsceneManager().isPlaying()) {
      return;
    }
    if(userAttacker.isSpectator() || userVictim.isSpectator() || arena.isDeathPlayer(attacker) || arena.isDeathPlayer(victim)) {
      return;
    }
    //we need to set it before the victim die, because of hero character
    if(Role.isRole(Role.MURDERER, userVictim)) {
      arena.setCharacter(Arena.CharacterType.HERO, attacker);
    }
    XSound.ENTITY_PLAYER_DEATH.play(victim.getLocation());
    victim.damage(100.0);


    userAttacker.adjustStatistic("KILLS", 1);
    if(Role.isRole(Role.MURDERER, userAttacker)) {
      userAttacker.adjustStatistic("LOCAL_KILLS", 1);
    }

    if(Role.isRole(Role.MURDERER, userVictim)) {
      arena.adjustContributorValue(Role.MURDERER, userAttacker, plugin.getRandom().nextInt(2));
    } else if(plugin.getConfigPreferences().getOption("BOW_KILL_DETECTIVE") && Role.isRole(Role.INNOCENT, userVictim)) {
      String deathTitle = new MessageBuilder("IN_GAME_DEATH_SCREEN").asKey().player(victim).arena(arena).build();
      String victimSubtitle = Role.isRole(Role.MURDERER, userAttacker)
        ? new MessageBuilder("IN_GAME_MESSAGES_GAME_END_PLACEHOLDERS_MURDERER_KILLED_YOU").asKey().build()
        : new MessageBuilder("IN_GAME_MESSAGES_GAME_END_PLACEHOLDERS_INNOCENT_KILLED_YOU").asKey().build();
      HalloweenChat.sendTitles(victim, deathTitle, victimSubtitle, 5, 50, 15);

      if(Role.isRole(Role.INNOCENT, userAttacker)) {
        attacker.damage(100.0);
        HalloweenChat.sendTitles(
          attacker,
          new MessageBuilder("IN_GAME_DEATH_SCREEN").asKey().player(attacker).arena(arena).build(),
          new MessageBuilder("IN_GAME_MESSAGES_GAME_END_PLACEHOLDERS_INNOCENT_KILLED_WRONGLY").asKey().build(),
          5, 55, 15
        );
      }
    }
  }

  private void giveSpectatorLoadout(Player player, Arena arena) {
    plugin.getSpecialItemManager().addSpecialItemsOfStage(player, SpecialItem.DisplayStage.SPECTATOR);
    if(arena.getTaskManager().hasAssignedTasks(player) && arena.getPlayersLeft().size() > arena.aliveMurderer()) {
      arena.getTaskManager().giveTaskPointer(player);
      if(arena.getMinimap() != null) {
        player.getInventory().setItemInOffHand(arena.getMinimap().createMapItem(player));
      }
      arena.getTaskManager().updatePlayerStationVisibility(player);
    }
    player.updateInventory();
  }

  @EventHandler(priority = EventPriority.HIGH)
  public void onPlayerDie(PlayerDeathEvent e) {
    Player player = e.getEntity();
    Arena arena = plugin.getArenaRegistry().getArena(player);
    if(arena == null) {
      return;
    }
    IUser user = plugin.getUserManager().getUser(player);
    ComplementAccessor.getComplement().setDeathMessage(e, "");
    e.getDrops().clear();
    e.setDroppedExp(0);
    e.setCancelled(true);
    e.setReviveHealth(VersionUtils.getMaxHealth(player));
    player.setFireTicks(0);
    player.setFallDistance(0.0f);
    player.setFoodLevel(20);

    if(arena.getArenaState() == IArenaState.WAITING_FOR_PLAYERS
      || arena.getArenaState() == IArenaState.STARTING
      || arena.getArenaState() == IArenaState.FULL_GAME) {
      if(arena.getLobbyLocation() != null) {
        VersionUtils.teleport(player, arena.getLobbyLocation());
      }
      return;
    }
    if(arena.getArenaState() == IArenaState.ENDING || arena.getArenaState() == IArenaState.RESTARTING) {
      player.getInventory().clear();
      player.getInventory().setArmorContents(null);
      player.setFlying(false);
      player.setAllowFlight(false);
      return;
    }
    if(ArenaUtils.isSpectator(player, arena)) {
      ArenaUtils.applySpectatorState(player, arena, true);
      return;
    }

    if(arena.getVentManager() != null && arena.getVentManager().isInsideVent(player)) {
      arena.getVentManager().exitVent(player);
    }

    Player killer = player.getKiller();
    plugin.getCorpseHandler().spawnCorpse(player, arena, killer);
    for(PotionEffect effect : player.getActivePotionEffects()) {
      player.removePotionEffect(effect.getType());
    }
    XPotion.BLINDNESS.buildPotionEffect(2 * 20, 1).apply(player);

    boolean gameStopping = false;
    if(Role.isRole(Role.MURDERER, user, arena) && arena.lastAliveMurderer()) {
      gameStopping = true;
      if(arena.getVotingManager().isVotingActive()) {
        Bukkit.getScheduler().runTask(plugin, () -> plugin.getArenaManager().stopGame(false, arena));
      } else {
        ArenaUtils.onMurdererDeath(arena);
      }
    }

    user.adjustStatistic("DEATHS", 1);
    arena.addDeathPlayer(player);
    arena.addSpectatorPlayer(player);
    user.setSpectator(true);

    player.closeInventory();
    player.getInventory().clear();
    player.getInventory().setArmorContents(null);
    player.getInventory().setItemInOffHand(null);
    arena.getTaskManager().handlePlayerDeath(player);

    ArenaUtils.hidePlayer(player, arena);
    ArenaUtils.applySpectatorState(player, arena, true);
    plugin.getRewardsHandler().performReward(player, plugin.getRewardsHandler().getRewardType("PLAYER_DEATH"));

    if(plugin.getConfigPreferences().getOption("HIDE_DEATH")) {
      String deathMsg = new MessageBuilder(MessageBuilder.ActionType.DEATH).player(player).arena(arena).build();
      for(Player p : arena.getPlayers()) {
        HalloweenChat.send(p, deathMsg);
      }
    }

    HalloweenChat.send(player, new MessageBuilder("IN_GAME_SPECTATOR_YOU_ARE_SPECTATOR").asKey().player(player).arena(arena).build());
    if(!arena.getVotingManager().isVotingActive() && !gameStopping) {
      HalloweenChat.sendTitles(player,
        new MessageBuilder("IN_GAME_DEATH_SCREEN").asKey().player(player).arena(arena).build(),
        "<#B8B0C8>คุณอยู่ในโหมดวิญญาณผู้ชม สามารถบินได้อย่างอิสระ",
        5, 45, 15);
    }

    giveSpectatorLoadout(player, arena);
    arena.getScoreboardManager().updateScoreboards();

    Bukkit.getScheduler().runTaskLater(plugin, () -> {
      if(!player.isOnline() || plugin.getArenaRegistry().getArena(player) != arena || arena.getArenaState() != IArenaState.IN_GAME) {
        return;
      }
      ArenaUtils.applySpectatorState(player, arena, true);
      giveSpectatorLoadout(player, arena);
    }, 2L);

    Bukkit.getScheduler().runTaskLater(plugin, () -> {
      if(!player.isOnline() || plugin.getArenaRegistry().getArena(player) != arena || arena.getArenaState() != IArenaState.IN_GAME) {
        return;
      }
      ArenaUtils.applySpectatorState(player, arena, true);
      if(arena.getTaskManager().hasAssignedTasks(player) && arena.getPlayersLeft().size() > arena.aliveMurderer()) {
        int remaining = arena.getTaskManager().getPlayerTotalTasks(player) - arena.getTaskManager().getPlayerCompletedTasks(player);
        if(remaining > 0) {
          HalloweenChat.send(player, "<shadow:#000000FF><#5A4E7C>[</shadow><#FF7518>Tasks<shadow:#000000FF><#5A4E7C>]</shadow> <#F4F1DE>คุณเสียชีวิตแล้ว แต่ยังต้องทำภารกิจที่เหลืออีก <#FFB703>" + remaining + " <#F4F1DE>ภารกิจเพื่อช่วยทีม");
          HalloweenChat.sendActionBar(player, "<shadow:#000000FF><#5A4E7C>[</shadow><#FF7518>Tasks<shadow:#000000FF><#5A4E7C>]</shadow> <#F4F1DE>บินไปทำภารกิจที่เหลืออีก <#FFB703>" + remaining + " <#F4F1DE>ภารกิจ");
        }
      }
    }, 10L);
  }

  @EventHandler(priority = EventPriority.HIGHEST)
  public void onRespawn(PlayerRespawnEvent event) {
    Player player = event.getPlayer();
    Arena arena = plugin.getArenaRegistry().getArena(player);
    if(arena == null) {
      return;
    }
    if(arena.getArenaState() == IArenaState.STARTING || arena.getArenaState() == IArenaState.WAITING_FOR_PLAYERS || arena.getArenaState() == IArenaState.FULL_GAME) {
      event.setRespawnLocation(arena.getLobbyLocation());
      return;
    }
    if(arena.getArenaState() == IArenaState.RESTARTING) {
      event.setRespawnLocation(arena.getEndLocation());
      return;
    }
    if(arena.getPlayers().contains(player)) {
      IUser user = plugin.getUserManager().getUser(player);
      Location firstSpawn = !arena.getPlayerSpawnPoints().isEmpty() ? arena.getPlayerSpawnPoints().get(0) : arena.getStartLocation();

      if(firstSpawn != null && player.getLocation().getWorld() != null && player.getLocation().getWorld().equals(firstSpawn.getWorld())) {
        event.setRespawnLocation(player.getLocation());
      } else if(firstSpawn != null) {
        event.setRespawnLocation(firstSpawn);
      }
      user.setSpectator(true);
      arena.addSpectatorPlayer(player);
      player.setGameMode(GameMode.ADVENTURE);
      VersionUtils.setCollidable(player, false);
      player.setFlySpeed(0.15f);
      player.setAllowFlight(true);
      player.setFlying(true);
      ArenaUtils.hidePlayer(player, arena);
      player.removePotionEffect(PotionEffectType.NIGHT_VISION);
      plugin.getRewardsHandler().performReward(player, plugin.getRewardsHandler().getRewardType("PLAYER_DEATH"));

      Bukkit.getScheduler().runTaskLater(plugin, () -> {
        if(player.isOnline() && plugin.getArenaRegistry().getArena(player) == arena) {
          ArenaUtils.applySpectatorState(player, arena, true);
          if(arena.getArenaState() == IArenaState.IN_GAME) {
            giveSpectatorLoadout(player, arena);
          }
        }
      }, 1L);
    }
  }

  @EventHandler(priority = EventPriority.HIGHEST)
  public void onSpectatorToggleFlight(PlayerToggleFlightEvent event) {
    Player player = event.getPlayer();
    Arena arena = plugin.getArenaRegistry().getArena(player);
    if(arena == null) {
      return;
    }
    if(arena.getCutsceneManager().isPlaying() || CutsceneManager.isPreviewing(player)) {
      return;
    }
    if(ArenaUtils.isSpectator(player, arena)) {
      event.setCancelled(false);
      player.setAllowFlight(true);
      if(player.getFlySpeed() < 0.15f) {
        player.setFlySpeed(0.15f);
      }
      Bukkit.getScheduler().runTaskLater(plugin, () -> {
        if(player.isOnline() && plugin.getArenaRegistry().getArena(player) == arena && ArenaUtils.isSpectator(player, arena)) {
          if(!player.getAllowFlight()) {
            player.setAllowFlight(true);
          }
        }
      }, 1L);
    }
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onSpectatorWorldChange(PlayerChangedWorldEvent event) {
    Player player = event.getPlayer();
    Arena arena = plugin.getArenaRegistry().getArena(player);
    if(arena == null || !ArenaUtils.isSpectator(player, arena)) {
      return;
    }
    Bukkit.getScheduler().runTaskLater(plugin, () -> {
      if(player.isOnline() && plugin.getArenaRegistry().getArena(player) == arena && ArenaUtils.isSpectator(player, arena)) {
        ArenaUtils.applySpectatorState(player, arena, true);
      }
    }, 1L);
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onSpectatorTeleport(PlayerTeleportEvent event) {
    Player player = event.getPlayer();
    Arena arena = plugin.getArenaRegistry().getArena(player);
    if(arena == null || !ArenaUtils.isSpectator(player, arena)) {
      return;
    }
    Bukkit.getScheduler().runTaskLater(plugin, () -> {
      if(player.isOnline() && plugin.getArenaRegistry().getArena(player) == arena && ArenaUtils.isSpectator(player, arena)) {
        ArenaUtils.applySpectatorState(player, arena, !player.isFlying() && player.isOnGround());
      }
    }, 1L);
  }

  @EventHandler(priority = EventPriority.MONITOR)
  public void onSpectatorExitFirstPerson(PlayerToggleSneakEvent event) {
    if(!event.isSneaking()) {
      return;
    }
    Player player = event.getPlayer();
    Arena arena = plugin.getArenaRegistry().getArena(player);
    if(arena == null || !ArenaUtils.isSpectator(player, arena)) {
      return;
    }
    Bukkit.getScheduler().runTaskLater(plugin, () -> {
      if(player.isOnline() && plugin.getArenaRegistry().getArena(player) == arena && ArenaUtils.isSpectator(player, arena)) {
        ArenaUtils.applySpectatorState(player, arena, true);
      }
    }, 1L);
  }


  @EventHandler
  public void locatorDistanceUpdate(PlayerMoveEvent event) {
    Player player = event.getPlayer();
    if(CutsceneManager.isRecording(player) && event.getTo() != null) {
      CutsceneManager.updateRecordedPlayerPosition(player, event.getTo());
    }
    Arena arena = plugin.getArenaRegistry().getArena(player);
    if(arena == null) {
      return;
    }
    IUser user = plugin.getUserManager().getUser(player);
    //skip spectators
    if(ArenaUtils.isSpectator(player, arena)) {
      return;
    }
    if(arena.getVotingManager().isVotingActive()) {
      Location to = event.getTo();
      if(to != null && (event.getFrom().getX() != to.getX() || event.getFrom().getZ() != to.getZ())) {
        Location spot = arena.getVotingManager().getMeetingSpot(player);
        if(spot != null) {
          Location fixed = spot.clone();
          fixed.setYaw(to.getYaw());
          fixed.setPitch(to.getPitch());
          event.setTo(fixed);
        } else {
          Location fixed = event.getFrom().clone();
          fixed.setYaw(to.getYaw());
          fixed.setPitch(to.getPitch());
          event.setTo(fixed);
        }
      }
      return;
    }
    if(arena.getArenaState() == IArenaState.IN_GAME) {
      VotingManager.DeadBody nearbyBody = arena.getVotingManager().findNearbyBody(player.getLocation(), 3.8);
      if(nearbyBody != null) {
        HalloweenChat.sendActionBar(player, "<shadow:#000000FF><#5A4E7C>[</shadow><#FF3B47>Dead Body<shadow:#000000FF><#5A4E7C>]</shadow> <#F4F1DE>ร่างของ <#FFB703>" + nearbyBody.getVictimName() + " <shadow:#000000FF><#5A4E7C>(</shadow><#B8B0C8>กดคลิกขวาเพื่อรายงานศพ<shadow:#000000FF><#5A4E7C>)</shadow>");
      }

      if(arena.isMurdererLocatorReceived() && Role.isRole(Role.MURDERER, user, arena) && arena.isMurderAlive(player)) {
        ItemStack innocentLocator = new ItemStack(Material.COMPASS, 1);
        ItemMeta innocentMeta = innocentLocator.getItemMeta();
        for(Player p : arena.getPlayersLeft()) {
          Arena playerArena = plugin.getArenaRegistry().getArena(p);
          IUser playerUser = plugin.getUserManager().getUser(p);

          if(Role.isRole(Role.INNOCENT, playerUser, playerArena)) {
            ComplementAccessor.getComplement().setDisplayName(innocentMeta, new MessageBuilder("IN_GAME_MESSAGES_ARENA_LOCATOR_INNOCENT").asKey().player(player).arena(arena).build() + " §7| §a" + (int) Math.round(player.getLocation().distance(p.getLocation())));
            innocentLocator.setItemMeta(innocentMeta);
            ItemPosition.setItem(user, ItemPosition.INNOCENTS_LOCATOR, innocentLocator);
          }
        }
      }
    }
  }

  @EventHandler
  public void onDrop(PlayerDropItemEvent event) {
    Arena arena = plugin.getArenaRegistry().getArena(event.getPlayer());
    if(arena != null && (arena.getArenaState() == IArenaState.IN_GAME || arena.getVotingManager().isVotingActive())) {
      event.setCancelled(true);
    }
  }

  @EventHandler
  public void onItemMove(InventoryClickEvent event) {
    if(event.getWhoClicked() instanceof Player && plugin.getArenaRegistry().isInArena((Player) event.getWhoClicked())) {
      Player player = (Player) event.getWhoClicked();
      Arena arena = plugin.getArenaRegistry().getArena(player);
      if(arena != null) {
        String title = XInventoryView.of(event.getView()).getTitle();
        if(arena.getVotingManager().isVotingGUI(title)) {
          event.setCancelled(true);
          arena.getVotingManager().handleVoteClick(player, event.getRawSlot());
          return;
        }
        if(arena.getSabotageManager().isSabotageGUI(title)) {
          event.setCancelled(true);
          arena.getSabotageManager().handleSabotageClick(player, event.getRawSlot());
          return;
        }
        if(arena.getSabotageManager().isFixGUI(title)) {
          event.setCancelled(true);
          arena.getSabotageManager().handleFixClick(player, event.getRawSlot());
          return;
        }
        if(arena.getTaskManager().getMinigameManager().isMinigameGUI(player, title)) {
          event.setCancelled(true);
          if(arena.getVotingManager().isVotingActive()) {
            arena.getTaskManager().getMinigameManager().handleClose(player, title);
            player.closeInventory();
            return;
          }
          arena.getTaskManager().getMinigameManager().handleClick(player, title, event.getRawSlot(), event.getClick());
          return;
        }
      }
      if(XInventoryView.of(event.getView()).getType() == InventoryType.CRAFTING || XInventoryView.of(event.getView()).getType() == InventoryType.PLAYER) {
        event.setResult(Event.Result.DENY);
      }
    }
  }

  @EventHandler
  public void onInventoryClose(InventoryCloseEvent event) {
    if(event.getPlayer() instanceof Player && plugin.getArenaRegistry().isInArena((Player) event.getPlayer())) {
      Player player = (Player) event.getPlayer();
      Arena arena = plugin.getArenaRegistry().getArena(player);
      if(arena != null && arena.getArenaState() == IArenaState.IN_GAME) {
        String title = XInventoryView.of(event.getView()).getTitle();
        if(arena.getSabotageManager().isFixGUI(title) || arena.getSabotageManager().isSabotageGUI(title)) {
          arena.getSabotageManager().handleClose(player);
        }
        arena.getTaskManager().getMinigameManager().handleClose(player, title);
      }
    }
  }

  @EventHandler(priority = EventPriority.HIGH)
  public void onCorpseStandInteract(PlayerInteractAtEntityEvent event) {
    if(event.getHand() == EquipmentSlot.OFF_HAND) {
      return;
    }
    Player player = event.getPlayer();
    Arena arena = plugin.getArenaRegistry().getArena(player);
    if(arena == null || arena.getArenaState() != IArenaState.IN_GAME) {
      return;
    }
    if(arena.getCutsceneManager().isPlaying()) {
      event.setCancelled(true);
      return;
    }
    IUser user = plugin.getUserManager().getUser(player);
    if(ArenaUtils.isSpectator(player, arena)) {
      if(!arena.getVotingManager().isVotingActive()
        && event.getRightClicked() instanceof Display
        && arena.getTaskManager().hasAssignedTasks(player)) {
        if(arena.getTaskManager().interactTaskEntity(player, event.getRightClicked())) {
          event.setCancelled(true);
        }
      }
      return;
    }
    if(arena.getVotingManager().isVotingActive()) {
      event.setCancelled(true);
      arena.getVotingManager().openVotingGUI(player);
      return;
    }
    if(arena.getVotingManager().tryReportNearbyBody(player, event.getRightClicked().getLocation())) {
      event.setCancelled(true);
      return;
    }
    if(Role.isRole(Role.MURDERER, user, arena) && arena.getSabotageManager().isSabotageRemote(player.getInventory().getItemInMainHand())) {
      event.setCancelled(true);
      arena.getSabotageManager().openSabotageGUI(player);
      return;
    }
    if(event.getRightClicked() instanceof ArmorStand) {
      if(arena.getVotingManager().isOnKillReportCooldown(player)) {
        event.setCancelled(true);
        return;
      }
      ArmorStand stand = (ArmorStand) event.getRightClicked();
      for(Stand corpseStand : arena.getStands()) {
        if(stand.equals(corpseStand.getStand())) {
          event.setCancelled(true);
          arena.getVotingManager().tryReportNearbyBody(player, stand.getLocation());
          return;
        }
      }
    }
    if(event.getRightClicked() instanceof Display) {
      if(event.getRightClicked().equals(arena.getCampfireDisplay())) {
        event.setCancelled(true);
        arena.getVotingManager().startVoting(player, null);
        return;
      }
      if(arena.getSabotageManager().hasActiveSabotage() && arena.getSabotageManager().isAtFixStation(player, event.getRightClicked().getLocation())) {
        event.setCancelled(true);
        arena.getSabotageManager().openFixGUI(player);
        return;
      }
      if(arena.getVentDisplays().contains(event.getRightClicked()) || arena.getVentBlockDisplays().contains(event.getRightClicked())) {
        if(Role.isRole(Role.MURDERER, user, arena) && arena.getVentManager() != null) {
          event.setCancelled(true);
          SpecialBlock closestVent = null;
          double closestDistSq = 16.0;
          Location clickLoc = event.getRightClicked().getLocation();
          for(SpecialBlock sb : arena.getSpecialBlocks()) {
            if(sb.getSpecialBlockType() == SpecialBlock.SpecialBlockType.RAPID_TELEPORTATION
              && sb.getLocation() != null && sb.getLocation().getWorld() != null
              && sb.getLocation().getWorld().equals(clickLoc.getWorld())) {
              double dSq = sb.getLocation().clone().add(0.5, 0.5, 0.5).distanceSquared(clickLoc);
              if(dSq <= closestDistSq) {
                closestDistSq = dSq;
                closestVent = sb;
              }
            }
          }
          if(closestVent != null) {
            if(arena.getVentManager().isInsideVent(player)) {
              if(!arena.getVentManager().hasJustEntered(player)) {
                arena.getVentManager().exitVent(player);
              }
            } else {
              arena.getVentManager().enterVent(player, closestVent);
            }
          }
        }
        return;
      }

      if(!Role.isRole(Role.MURDERER, user, arena)) {
        if(arena.getTaskManager().interactTaskEntity(player, event.getRightClicked())) {
          event.setCancelled(true);
        }
      } else {
        if(arena.getTaskManager().isStationDisplayEntity(event.getRightClicked())) {
          event.setCancelled(true);
          HalloweenChat.sendActionBar(player, "<shadow:#000000FF><#5A4E7C>[</shadow><#FF3B47>ฆาตกร<shadow:#000000FF><#5A4E7C>]</shadow> <#FF3B47>คุณเป็นฆาตกร ไม่สามารถทำภารกิจได้");
        }
      }
    }
  }

  @EventHandler(priority = EventPriority.HIGH)
  public void onTaskInteract(PlayerInteractEvent event) {
    if(event.getHand() == EquipmentSlot.OFF_HAND) {
      return;
    }
    Player player = event.getPlayer();
    Arena arena = plugin.getArenaRegistry().getArena(player);
    if(arena == null || arena.getArenaState() != IArenaState.IN_GAME) {
      return;
    }
    if(arena.getCutsceneManager().isPlaying()) {
      event.setCancelled(true);
      return;
    }
    IUser user = plugin.getUserManager().getUser(player);
    if(ArenaUtils.isSpectator(player, arena)) {
      if(!arena.getVotingManager().isVotingActive() && arena.getTaskManager().hasAssignedTasks(player)) {
        if(event.getAction() == Action.RIGHT_CLICK_BLOCK) {
          Block specBlock = event.getClickedBlock();
          Location specTargetLoc = specBlock != null ? specBlock.getLocation() : null;
          if(arena.getTaskManager().interactTask(player, specTargetLoc)) {
            event.setCancelled(true);
          }
        } else if(event.getAction() == Action.RIGHT_CLICK_AIR) {
          ItemStack specHand = player.getInventory().getItemInMainHand();
          if(specHand == null || specHand.getType() == Material.AIR || arena.getTaskManager().isTaskCompass(specHand)) {
            if(arena.getTaskManager().interactTask(player, null)) {
              event.setCancelled(true);
            }
          }
        }
      }
      return;
    }

    if(arena.getVotingManager().isVotingActive()) {
      event.setCancelled(true);
      if(event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
        arena.getVotingManager().openVotingGUI(player);
      }
      return;
    }

    if(event.getAction() == Action.LEFT_CLICK_BLOCK) {
      Block leftBlock = event.getClickedBlock();
      Location leftLoc = leftBlock != null ? leftBlock.getLocation() : null;
      ItemStack leftHand = player.getInventory().getItemInMainHand();
      ItemStack murdererSword = plugin.getSwordSkinManager().getMurdererSword(player);
      boolean holdingSword = Role.isRole(Role.MURDERER, user, arena)
        && leftHand != null
        && murdererSword != null
        && leftHand.getType() == murdererSword.getType();
      Location busHornLoc = arena.getCampfireLocation();
      if(!holdingSword && busHornLoc != null && leftLoc != null && leftLoc.getWorld() != null && leftLoc.getWorld().equals(busHornLoc.getWorld())) {
        if(leftLoc.getBlockX() == busHornLoc.getBlockX() && leftLoc.getBlockY() == busHornLoc.getBlockY() && leftLoc.getBlockZ() == busHornLoc.getBlockZ()) {
          event.setCancelled(true);
          arena.getVotingManager().startVoting(player, null);
        }
      }
      return;
    }

    if(event.getAction() != Action.RIGHT_CLICK_BLOCK && event.getAction() != Action.RIGHT_CLICK_AIR) {
      return;
    }

    Block clickedBlock = event.getClickedBlock();
    Location targetLoc = clickedBlock != null ? clickedBlock.getLocation() : null;

    ItemStack inHand = player.getInventory().getItemInMainHand();
    if(Role.isRole(Role.MURDERER, user, arena) && arena.getSabotageManager().isSabotageRemote(inHand)) {
      event.setCancelled(true);
      arena.getSabotageManager().openSabotageGUI(player);
      return;
    }

    ItemStack murdererSword = plugin.getSwordSkinManager().getMurdererSword(player);
    boolean holdingMurdererSword = Role.isRole(Role.MURDERER, user, arena)
      && inHand != null
      && murdererSword != null
      && inHand.getType() == murdererSword.getType();

    if(!holdingMurdererSword && arena.getVotingManager().tryReportNearbyBody(player, targetLoc)) {
      event.setCancelled(true);
      return;
    }

    Location campfireLoc = arena.getCampfireLocation();
    if(campfireLoc != null && targetLoc != null && targetLoc.getWorld() != null && targetLoc.getWorld().equals(campfireLoc.getWorld())) {
      if((targetLoc.getBlockX() == campfireLoc.getBlockX() && targetLoc.getBlockY() == campfireLoc.getBlockY() && targetLoc.getBlockZ() == campfireLoc.getBlockZ())
        || targetLoc.distanceSquared(campfireLoc) <= 2.25) {
        event.setCancelled(true);
        arena.getVotingManager().startVoting(player, null);
        return;
      }
    }

    if(arena.getSabotageManager().hasActiveSabotage() && arena.getSabotageManager().isAtFixStation(player, targetLoc)) {
      event.setCancelled(true);
      arena.getSabotageManager().openFixGUI(player);
      return;
    }

    SabotageType stationType = arena.getSabotageManager().getSabotageStationAt(targetLoc);
    if(stationType != null) {
      event.setCancelled(true);
      HalloweenChat.sendActionBar(player, "<shadow:#000000FF><#5A4E7C>[</shadow><#6EEB83>Sabotage<shadow:#000000FF><#5A4E7C>]</shadow> <#F4F1DE>" + stationType.getDisplayName() + " <#6EEB83>ทำงานปกติ");
      return;
    }

    if(Role.isRole(Role.MURDERER, user, arena)) {
      if(targetLoc != null) {
        for(Location loc : arena.getAllTaskLocations()) {
          if(loc != null && loc.getWorld() != null && loc.getWorld().equals(targetLoc.getWorld())) {
            if(targetLoc.getBlockX() == loc.getBlockX() && targetLoc.getBlockY() == loc.getBlockY() && targetLoc.getBlockZ() == loc.getBlockZ()) {
              event.setCancelled(true);
              HalloweenChat.sendActionBar(player, "<shadow:#000000FF><#5A4E7C>[</shadow><#FF3B47>ฆาตกร<shadow:#000000FF><#5A4E7C>]</shadow> <#FF3B47>คุณเป็นฆาตกร ไม่สามารถทำภารกิจได้");
              return;
            }
          }
        }
      }
      return;
    }

    if(arena.getTaskManager().interactTask(player, targetLoc)) {
      event.setCancelled(true);
    }
  }

  @EventHandler
  public void onSpectateTeleport(PlayerTeleportEvent event) {
    if(event.getCause() != PlayerTeleportEvent.TeleportCause.SPECTATE) {
      return;
    }
    Player player = event.getPlayer();
    if(CutsceneManager.isPreviewing(player)) {
      event.setCancelled(true);
      return;
    }
    Arena arena = plugin.getArenaRegistry().getArena(player);
    if(arena != null && arena.getCutsceneManager().isPlaying()) {
      event.setCancelled(true);
    }
  }

  @EventHandler
  public void onStopSpectatingEntity(PlayerStopSpectatingEntityEvent event) {
    Player player = event.getPlayer();
    if(CutsceneManager.isPreviewing(player)) {
      event.setCancelled(true);
      return;
    }
    Arena arena = plugin.getArenaRegistry().getArena(player);
    if(arena != null && arena.getCutsceneManager().isPlaying()) {
      event.setCancelled(true);
    }
  }
}
