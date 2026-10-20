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
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffectType;
import plugily.projects.minigamesbox.api.arena.IArenaState;
import plugily.projects.minigamesbox.api.arena.IPluginArena;
import plugily.projects.minigamesbox.api.user.IUser;
import plugily.projects.minigamesbox.classic.arena.PluginArenaEvents;
import plugily.projects.minigamesbox.classic.handlers.items.SpecialItem;
import plugily.projects.minigamesbox.classic.handlers.language.MessageBuilder;
import plugily.projects.minigamesbox.classic.handlers.language.TitleBuilder;
import plugily.projects.minigamesbox.classic.utils.misc.complement.ComplementAccessor;
import plugily.projects.minigamesbox.classic.utils.version.ServerVersion;
import plugily.projects.minigamesbox.classic.utils.version.VersionUtils;
import plugily.projects.minigamesbox.classic.utils.version.events.api.PlugilyEntityPickupItemEvent;
import plugily.projects.minigamesbox.classic.utils.version.events.api.PlugilyPlayerPickupArrow;
import plugily.projects.minigamesbox.classic.utils.version.xseries.XPotion;
import plugily.projects.minigamesbox.classic.utils.version.xseries.XSound;
import plugily.projects.minigamesbox.classic.utils.version.xseries.inventory.XInventoryView;
import plugily.projects.murdermystery.Main;
import plugily.projects.murdermystery.api.events.game.MurderGameGetBowEvent;
import plugily.projects.murdermystery.api.events.game.MurderGameGoldPickupEvent;
import plugily.projects.murdermystery.arena.corpse.Stand;
import plugily.projects.murdermystery.arena.managers.MapRestorerManager;
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
    victim.damage(1000.0);
    if(arena.getArenaState() == IArenaState.IN_GAME) {
      VersionUtils.teleport(victim, pluginArena.getPlayerSpawnPoints().get(0));
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
    IUser user = plugin.getUserManager().getUser(player);
    e.setCancelled(true);
    if(arena.getBowHologram() != null
      && e.getItem().equals(arena.getBowHologram().getEntityItem())) {
      if(!user.isSpectator() && Role.isRole(Role.INNOCENT, user, arena)) {
        MurderGameGetBowEvent murderGameGetBowEvent = new MurderGameGetBowEvent(arena, player);
        Bukkit.getPluginManager().callEvent(murderGameGetBowEvent);
        if(murderGameGetBowEvent.isCancelled()) {
          return;
        }

        XSound.BLOCK_LAVA_POP.play(player.getLocation(), 1F, 2F);

        ((MapRestorerManager) arena.getMapRestorerManager()).removeBowHolo();
        e.getItem().remove();

        for(Player loopPlayer : arena.getPlayersLeft()) {
          IUser loopUser = plugin.getUserManager().getUser(loopPlayer);
          if(Role.isRole(Role.INNOCENT, loopUser)) {
            ItemPosition.setItem(loopUser, ItemPosition.BOW_LOCATOR, new ItemStack(Material.AIR, 1));
          }
        }

        arena.setCharacter(Arena.CharacterType.FAKE_DETECTIVE, player);
        ItemPosition.setItem(user, ItemPosition.BOW, new ItemStack(Material.BOW, 1));
        ItemPosition.setItem(user, ItemPosition.INFINITE_ARROWS, new ItemStack(Material.ARROW, plugin.getConfig().getInt("Bow.Amount.Arrows.Fake", 3)));
        new MessageBuilder("IN_GAME_MESSAGES_ARENA_PLAYING_BOW_PICKUP").asKey().player(player).arena(arena).sendArena();
      }

      return;
    }

    if(e.getItem().getItemStack().getType() != Material.GOLD_INGOT) {
      return;
    }

    if(user.isSpectator() || arena.getArenaState() != IArenaState.IN_GAME) {
      return;
    }

    if(PrayerRegistry.getBan().contains(player)) {
      e.setCancelled(true);
      return;
    }

    int pickedUpAmount = e.getItem().getItemStack().getAmount();
    if(PrayerRegistry.getRush().contains(player)) {
      pickedUpAmount = 3 * pickedUpAmount;
    }
    MurderGameGoldPickupEvent murderGameGoldPickupEvent = new MurderGameGoldPickupEvent(arena, player, pickedUpAmount);
    Bukkit.getPluginManager().callEvent(murderGameGoldPickupEvent);
    if(murderGameGoldPickupEvent.isCancelled()) {
      return;
    }

    e.getItem().remove();

    XSound.BLOCK_LAVA_POP.play(player.getLocation(), 1, 1);
    arena.getGoldSpawned().remove(e.getItem());

    ItemStack stack = new ItemStack(Material.GOLD_INGOT, murderGameGoldPickupEvent.getAmount());
    ItemPosition.addItem(user, ItemPosition.GOLD_INGOTS, stack);
    user.adjustStatistic("LOCAL_GOLD", stack.getAmount());
    ArenaUtils.addScore(user, ArenaUtils.ScoreAction.GOLD_PICKUP, stack.getAmount());

    new MessageBuilder("IN_GAME_MESSAGES_ARENA_PLAYING_SCORE_GOLD").asKey().player(player).arena(arena).sendPlayer();
    plugin.getRewardsHandler().performReward(player, plugin.getRewardsHandler().getRewardType("GOLD_PICKUP"));

    if(Role.isRole(Role.ANY_DETECTIVE, user, arena)) {
      ItemPosition.addItem(user, ItemPosition.ARROWS, new ItemStack(Material.ARROW, e.getItem().getItemStack().getAmount() * plugin.getConfig().getInt("Bow.Amount.Arrows.Detective", 3)));
      return;
    }

    if(user.getStatistic("LOCAL_GOLD") >= plugin.getConfig().getInt("Gold.Amount.Bow", 10)) {
      MurderGameGetBowEvent murderGameGetBowEvent = new MurderGameGetBowEvent(arena, player);
      Bukkit.getPluginManager().callEvent(murderGameGetBowEvent);
      if(murderGameGetBowEvent.isCancelled()) {
        return;
      }

      user.setStatistic("LOCAL_GOLD", 0);
      new TitleBuilder("IN_GAME_MESSAGES_ARENA_PLAYING_BOW_SHOT_TITLE")
        .asKey()
        .player(player)
        .arena(arena)
        .sendPlayer();
      ItemPosition.setItem(user, ItemPosition.BOW, new ItemStack(Material.BOW, 1));
      ItemPosition.addItem(
        user,
        ItemPosition.ARROWS,
        new ItemStack(Material.ARROW, plugin.getConfig().getInt("Bow.Amount.Arrows.Gold", 3)));
      player
        .getInventory()
        .setItem(
          /* same for all roles */ ItemPosition.GOLD_INGOTS.getOtherRolesItemPosition(),
          null);
    }
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
    if(checkArena != null && checkArena.getVotingManager().isVotingActive()) {
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
    } else if(Role.isRole(Role.ANY_DETECTIVE, userVictim)) {
      plugin.getRewardsHandler().performReward(attacker, plugin.getRewardsHandler().getRewardType("KILL_DETECTIVE"));
    }

    XSound.ENTITY_PLAYER_DEATH.play(victim.getLocation());
    victim.damage(100.0);

    IUser user = plugin.getUserManager().getUser(attacker);

    user.adjustStatistic("KILLS", 1);
    user.adjustStatistic("LOCAL_KILLS", 1);
    ArenaUtils.addScore(user, ArenaUtils.ScoreAction.KILL_PLAYER, 0);

    Arena arena = plugin.getArenaRegistry().getArena(attacker);
    if(Role.isRole(Role.ANY_DETECTIVE, userVictim) && arena.lastAliveDetective()) {
      //if already true, no effect is done :)
      arena.setDetectiveDead(true);
      if(Role.isRole(Role.FAKE_DETECTIVE, userVictim)) {
        arena.setCharacter(Arena.CharacterType.FAKE_DETECTIVE, null);
      }
      ArenaUtils.dropBowAndAnnounce(arena, victim);
    }
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
    if(arena == null || arena.getVotingManager().isVotingActive()) {
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
      arena.adjustContributorValue(Role.DETECTIVE, userAttacker, plugin.getRandom().nextInt(2));
      ArenaUtils.addScore(userAttacker, ArenaUtils.ScoreAction.KILL_PLAYER, 0);
    }

    VersionUtils.sendTitles(victim, new MessageBuilder("IN_GAME_DEATH_SCREEN").asKey().build(), null, 5, 40, 50);

    if(Role.isRole(Role.MURDERER, userVictim)) {
      ArenaUtils.addScore(userAttacker, ArenaUtils.ScoreAction.KILL_MURDERER, 0);
      arena.adjustContributorValue(Role.MURDERER, userAttacker, plugin.getRandom().nextInt(2));
    } else if(plugin.getConfigPreferences().getOption("BOW_KILL_DETECTIVE") && (Role.isRole(Role.ANY_DETECTIVE, userVictim) || Role.isRole(Role.INNOCENT, userVictim))) {
      if(Role.isRole(Role.MURDERER, userAttacker)) {
        VersionUtils.sendTitles(victim, null, new MessageBuilder("IN_GAME_MESSAGES_GAME_END_PLACEHOLDERS_MURDERER_KILLED_YOU").asKey().build(), 5, 40, 5);
      } else {
        VersionUtils.sendTitles(victim, null, new MessageBuilder("IN_GAME_MESSAGES_GAME_END_PLACEHOLDERS_INNOCENT_KILLED_YOU").asKey().build(), 5, 40, 5);
      }

      //if else, murderer killed, so don't kill him :)
      if(Role.isRole(Role.ANY_DETECTIVE, userAttacker) || Role.isRole(Role.INNOCENT, userAttacker)) {
        VersionUtils.sendSubTitle(attacker, new MessageBuilder("IN_GAME_MESSAGES_GAME_END_PLACEHOLDERS_INNOCENT_KILLED_WRONGLY").asKey().build(), 5, 40, 5);

        attacker.damage(100.0);
        ArenaUtils.addScore(userAttacker, ArenaUtils.ScoreAction.INNOCENT_KILL, 0);
        plugin.getRewardsHandler().performReward(attacker, plugin.getRewardsHandler().getRewardType("KILL_DETECTIVE"));
        if(Role.isRole(Role.ANY_DETECTIVE, userAttacker) && arena.lastAliveDetective()) {
          arena.setDetectiveDead(true);
          if(Role.isRole(Role.FAKE_DETECTIVE, userAttacker)) {
            arena.setCharacter(Arena.CharacterType.FAKE_DETECTIVE, null);
          }
          ArenaUtils.dropBowAndAnnounce(arena, victim);
        }
      }
    }
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
    plugin.getCorpseHandler().spawnCorpse(player, arena, player.getKiller());
    XPotion.BLINDNESS.buildPotionEffect(3 * 20, 1).apply(player);
    if(arena.getArenaState() == IArenaState.STARTING) {
      return;
    } else if(arena.getArenaState() == IArenaState.ENDING || arena.getArenaState() == IArenaState.RESTARTING) {
      player.getInventory().clear();
      player.setFlying(false);
      player.setAllowFlight(false);
      user.setStatistic("LOCAL_GOLD", 0);
      return;
    }
    if(Role.isRole(Role.MURDERER, user, arena) && arena.lastAliveMurderer()) {
      ArenaUtils.onMurdererDeath(arena);
    }
    if(Role.isRole(Role.ANY_DETECTIVE, user) && arena.lastAliveDetective()) {
      arena.setDetectiveDead(true);
      if(Role.isRole(Role.FAKE_DETECTIVE, user)) {
        arena.setCharacter(Arena.CharacterType.FAKE_DETECTIVE, null);
      }
      ArenaUtils.dropBowAndAnnounce(arena, player);
    }
    user.adjustStatistic("DEATHS", 1);
    user.setSpectator(true);
    VersionUtils.setCollidable(player, false);
    player.setGameMode(GameMode.SURVIVAL);
    user.setStatistic("LOCAL_GOLD", 0);
    ArenaUtils.hidePlayer(player, arena);
    player.setAllowFlight(true);
    player.setFlying(true);
    player.getInventory().clear();
    arena.getTaskManager().removePlayer(player);
    if(plugin.getConfigPreferences().getOption("HIDE_DEATH")) {
      new MessageBuilder(MessageBuilder.ActionType.DEATH).player(player).arena(arena).sendArena();
    }

    if(arena.getArenaState() != IArenaState.ENDING && arena.getArenaState() != IArenaState.RESTARTING) {
      arena.addDeathPlayer(player);
    }
    //we must call it ticks later due to instant respawn bug
    Bukkit.getScheduler().runTaskLater(plugin, () -> {
      player.spigot().respawn();
      plugin.getSpecialItemManager().addSpecialItemsOfStage(player, SpecialItem.DisplayStage.SPECTATOR);
    }, 5);
  }

  @EventHandler(priority = EventPriority.HIGHEST)
  public void onRespawn(PlayerRespawnEvent event) {
    Player player = event.getPlayer();
    Arena arena = plugin.getArenaRegistry().getArena(player);
    if(arena == null) {
      return;
    }
    if(arena.getArenaState() == IArenaState.STARTING || arena.getArenaState() == IArenaState.WAITING_FOR_PLAYERS) {
      event.setRespawnLocation(arena.getLobbyLocation());
      return;
    }
    if(arena.getArenaState() == IArenaState.RESTARTING) {
      event.setRespawnLocation(arena.getEndLocation());
      return;
    }
    if(arena.getPlayers().contains(player)) {
      IUser user = plugin.getUserManager().getUser(player);
      org.bukkit.Location firstSpawn = arena.getPlayerSpawnPoints().get(0);

      if(player.getLocation().getWorld().equals(firstSpawn.getWorld())) {
        event.setRespawnLocation(player.getLocation());
      } else {
        event.setRespawnLocation(firstSpawn);
      }
      player.setAllowFlight(true);
      player.setFlying(true);
      user.setSpectator(true);
      ArenaUtils.hidePlayer(player, arena);
      VersionUtils.setCollidable(player, false);
      player.setGameMode(GameMode.SURVIVAL);
      player.removePotionEffect(PotionEffectType.NIGHT_VISION);
      user.setStatistic("LOCAL_GOLD", 0);
      plugin.getRewardsHandler().performReward(player, plugin.getRewardsHandler().getRewardType("PLAYER_DEATH"));
    }
  }


  @EventHandler
  public void locatorDistanceUpdate(PlayerMoveEvent event) {
    Player player = event.getPlayer();
    Arena arena = plugin.getArenaRegistry().getArena(player);
    if(arena == null) {
      return;
    }
    IUser user = plugin.getUserManager().getUser(player);
    //skip spectators
    if(user.isSpectator()) {
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
        VersionUtils.sendActionBar(player, HalloweenChat.color("<gradient:#D90429:#FF5400><bold>พบศพของ " + nearbyBody.getVictimName() + "!</bold></gradient> <#FFB703><bold>[คลิกขวาหรือโจมตีเพื่อรายงานศพ / Report]</bold>"));
      }

      if(Role.isRole(Role.INNOCENT, user, arena)) {
        if(player.getInventory().getItem(ItemPosition.BOW_LOCATOR.getOtherRolesItemPosition()) != null) {
          ItemStack bowLocator = new ItemStack(Material.COMPASS, 1);
          ItemMeta bowMeta = bowLocator.getItemMeta();
          ComplementAccessor.getComplement().setDisplayName(bowMeta, new MessageBuilder("IN_GAME_MESSAGES_ARENA_LOCATOR_BOW").asKey().player(player).arena(arena).build() + " §7| §a" + (int) Math.round(player.getLocation().distance(player.getCompassTarget())));
          bowLocator.setItemMeta(bowMeta);
          ItemPosition.setItem(user, ItemPosition.BOW_LOCATOR, bowLocator);
          return;
        }
      }
      if(arena.isMurdererLocatorReceived() && Role.isRole(Role.MURDERER, user, arena) && arena.isMurderAlive(player)) {
        ItemStack innocentLocator = new ItemStack(Material.COMPASS, 1);
        ItemMeta innocentMeta = innocentLocator.getItemMeta();
        for(Player p : arena.getPlayersLeft()) {
          Arena playerArena = plugin.getArenaRegistry().getArena(p);
          IUser playerUser = plugin.getUserManager().getUser(p);

          if(Role.isRole(Role.INNOCENT, playerUser, playerArena) || Role.isRole(Role.ANY_DETECTIVE, playerUser, playerArena)) {
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

  @EventHandler
  public void onCorpseStandInteract(PlayerInteractAtEntityEvent event) {
    if(event.getHand() == EquipmentSlot.OFF_HAND) {
      return;
    }
    Player player = event.getPlayer();
    Arena arena = plugin.getArenaRegistry().getArena(player);
    if(arena == null || arena.getArenaState() != IArenaState.IN_GAME) {
      return;
    }
    IUser user = plugin.getUserManager().getUser(player);
    if(user.isSpectator()) {
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
      ArmorStand stand = (ArmorStand) event.getRightClicked();
      for(Stand corpseStand : arena.getStands()) {
        if(stand.equals(corpseStand.getStand())) {
          event.setCancelled(true);
          arena.getVotingManager().startVoting(player);
          return;
        }
      }
    }
    if(event.getRightClicked() instanceof Display) {
      if(arena.getSabotageManager().hasActiveSabotage() && arena.getSabotageManager().isAtFixStation(player, event.getRightClicked().getLocation())) {
        event.setCancelled(true);
        arena.getSabotageManager().openFixGUI(player);
        return;
      }
      if(arena.getVentDisplays().contains(event.getRightClicked()) || arena.getVentBlockDisplays().contains(event.getRightClicked())) {
        if(Role.isRole(Role.MURDERER, user, arena) && arena.getVentManager() != null) {
          event.setCancelled(true);
          for(SpecialBlock sb : arena.getSpecialBlocks()) {
            if(sb.getSpecialBlockType() == SpecialBlock.SpecialBlockType.RAPID_TELEPORTATION) {
              if(sb.getLocation().getWorld() != null && sb.getLocation().getWorld().equals(player.getWorld()) && sb.getLocation().distanceSquared(player.getLocation()) <= 16.0) {
                if(arena.getVentManager().isInsideVent(player)) {
                  arena.getVentManager().exitVent(player);
                } else {
                  arena.getVentManager().enterVent(player, sb);
                }
                return;
              }
            }
          }
        }
        return;
      }

      if(!Role.isRole(Role.MURDERER, user, arena)) {
        if(arena.getTaskManager().interactTask(player, event.getRightClicked().getLocation())) {
          event.setCancelled(true);
        }
      } else {
        event.setCancelled(true);
        VersionUtils.sendActionBar(player, HalloweenChat.color("<gradient:#D90429:#FF5400><bold>[คนร้าย]</bold></gradient> <#FF3B47>คุณเป็นคนร้าย ไม่สามารถทำภารกิจได้!"));
      }
    }
  }

  @EventHandler
  public void onTaskInteract(PlayerInteractEvent event) {
    if(event.getHand() == EquipmentSlot.OFF_HAND) {
      return;
    }
    Player player = event.getPlayer();
    Arena arena = plugin.getArenaRegistry().getArena(player);
    if(arena == null || arena.getArenaState() != IArenaState.IN_GAME) {
      return;
    }
    IUser user = plugin.getUserManager().getUser(player);
    if(user.isSpectator()) {
      return;
    }

    if(arena.getVotingManager().isVotingActive()) {
      event.setCancelled(true);
      if(event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {
        arena.getVotingManager().openVotingGUI(player);
      }
      return;
    }

    Block clickedBlock = event.getClickedBlock();
    Location targetLoc = clickedBlock != null ? clickedBlock.getLocation() : null;

    if(arena.getVotingManager().tryReportNearbyBody(player, targetLoc)) {
      event.setCancelled(true);
      return;
    }

    if(event.getAction() != Action.RIGHT_CLICK_BLOCK && event.getAction() != Action.RIGHT_CLICK_AIR) {
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

    ItemStack inHand = player.getInventory().getItemInMainHand();
    if(Role.isRole(Role.MURDERER, user, arena) && arena.getSabotageManager().isSabotageRemote(inHand)) {
      event.setCancelled(true);
      arena.getSabotageManager().openSabotageGUI(player);
      return;
    }

    if(arena.getSabotageManager().hasActiveSabotage() && arena.getSabotageManager().isAtFixStation(player, targetLoc)) {
      event.setCancelled(true);
      arena.getSabotageManager().openFixGUI(player);
      return;
    }

    SabotageType stationType = arena.getSabotageManager().getSabotageStationAt(targetLoc);
    if(stationType != null) {
      event.setCancelled(true);
      VersionUtils.sendActionBar(player, HalloweenChat.color("<#6EEB83>[[/]] จุด " + stationType.getIcon() + " " + stationType.getDisplayName() + " <#B8B0C8>ยังทำงานปกติ ไม่มีการก่อวินาศกรรม"));
      return;
    }

    if(Role.isRole(Role.MURDERER, user, arena)) {
      if(targetLoc != null) {
        for(Location loc : arena.getAllTaskLocations()) {
          if(loc != null && loc.getWorld() != null && loc.getWorld().equals(targetLoc.getWorld())) {
            if((targetLoc.getBlockX() == loc.getBlockX() && targetLoc.getBlockY() == loc.getBlockY() && targetLoc.getBlockZ() == loc.getBlockZ())
              || targetLoc.distanceSquared(loc) <= 4.0) {
              event.setCancelled(true);
              VersionUtils.sendActionBar(player, HalloweenChat.color("<gradient:#D90429:#FF5400><bold>[คนร้าย]</bold></gradient> <#FF3B47>คุณเป็นคนร้าย ไม่สามารถทำภารกิจได้! <#B8B0C8>(แอบลงท่อหรือกำจัดผู้โดยสาร)"));
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
}
