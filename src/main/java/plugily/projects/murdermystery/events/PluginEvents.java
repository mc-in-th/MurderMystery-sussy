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

package plugily.projects.murdermystery.events;

import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.EulerAngle;
import org.bukkit.util.Vector;
import plugily.projects.minigamesbox.api.arena.IArenaState;
import plugily.projects.minigamesbox.api.user.IUser;
import plugily.projects.minigamesbox.classic.handlers.language.MessageBuilder;
import plugily.projects.minigamesbox.classic.utils.version.ServerVersion;
import plugily.projects.minigamesbox.classic.utils.version.VersionUtils;
import plugily.projects.minigamesbox.classic.utils.version.xseries.XMaterial;
import plugily.projects.minigamesbox.classic.utils.version.xseries.XSound;
import plugily.projects.murdermystery.Main;
import plugily.projects.murdermystery.arena.Arena;
import plugily.projects.murdermystery.arena.ArenaUtils;
import plugily.projects.murdermystery.arena.cutscene.CutsceneManager;
import plugily.projects.murdermystery.arena.managers.NameTagPacketManager;
import plugily.projects.murdermystery.arena.role.Role;
import plugily.projects.murdermystery.utils.HalloweenChat;

/**
 * @author Plajer
 * <p>Created at 05.08.2018
 */
public class PluginEvents implements Listener {

  private final Main plugin;

  public PluginEvents(Main plugin) {
    this.plugin = plugin;
    plugin.getServer().getPluginManager().registerEvents(this, plugin);
  }

  @EventHandler(priority = EventPriority.LOWEST)
  public void onPlayerJoin(PlayerJoinEvent event) {
    NameTagPacketManager.handlePlayerJoin(plugin, event.getPlayer());
  }

  @EventHandler(priority = EventPriority.LOWEST)
  public void onPlayerQuit(PlayerQuitEvent event) {
    CutsceneManager.handlePlayerQuit(plugin, event.getPlayer());
    NameTagPacketManager.handlePlayerQuit(plugin, event.getPlayer());
  }

  @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
  public void onPlayerChangedWorld(PlayerChangedWorldEvent event) {
    plugin.getServer().getScheduler().runTaskLater(plugin, ArenaUtils::syncAllNameTags, 1L);
  }

  @EventHandler(priority = EventPriority.HIGHEST)
  public void onSwordThrow(PlayerInteractEvent event) {
    if(event.getHand() == EquipmentSlot.OFF_HAND) {
      return;
    }
    if(event.getAction() == Action.LEFT_CLICK_AIR || event.getAction() == Action.LEFT_CLICK_BLOCK || event.getAction() == Action.PHYSICAL) {
      return;
    }
    if(event.getAction() == Action.RIGHT_CLICK_BLOCK && event.isCancelled()) {
      return;
    }
    Player attacker = event.getPlayer();
    Arena arena = plugin.getArenaRegistry().getArena(attacker);
    if(arena == null || arena.getArenaState() != IArenaState.IN_GAME) {
      return;
    }
    if(arena.getVotingManager().isVotingActive()
      || arena.getCutsceneManager().isPlaying()
      || (arena.getVentManager() != null && arena.getVentManager().isInsideVent(attacker))) {
      return;
    }

    IUser attackerUser = plugin.getUserManager().getUser(attacker);
    if(attackerUser == null || attackerUser.isSpectator() || arena.isDeathPlayer(attacker) || !Role.isRole(Role.MURDERER, attackerUser, arena)) {
      return;
    }

    ItemStack murdererSword = plugin.getSwordSkinManager().getMurdererSword(attacker);

    if(murdererSword == null) {
      return;
    }

    if(VersionUtils.getItemInHand(attacker).getType() != murdererSword.getType()) {
      return;
    }
    if(attackerUser.getCooldown("sword_shoot") > 0) {
      return;
    }
    createFlyingSword(arena, attacker, attackerUser);

    int swordFlyCooldown = plugin.getConfig().getInt("Sword.Cooldown.Fly", 5);
    if(swordFlyCooldown <= 0) {
      return;
    }
    attackerUser.setCooldown("sword_shoot", swordFlyCooldown);
    if(ServerVersion.Version.isCurrentLower(ServerVersion.Version.v1_11)) {
      attackerUser.setCooldown("sword_attack", (plugin.getConfig().getInt("Sword.Cooldown.Attack", 1)));
    } else {
      VersionUtils.setMaterialCooldown(attacker, plugin.getSwordSkinManager().getMurdererSword(attacker).getType(), 20 * (plugin.getConfig().getInt("Sword.Cooldown.Attack", 1)));
    }
    plugin.getBukkitHelper().applyActionBarCooldown(attacker, swordFlyCooldown);
  }

  private void createFlyingSword(Arena sourceArena, Player attacker, IUser attackerUser) {
    Location loc = attacker.getLocation();
    Vector vec = loc.getDirection();
    vec.normalize().multiply(plugin.getConfig().getDouble("Sword.Speed", 0.65));
    Location standStart = plugin.getBukkitHelper().rotateAroundAxisY(new Vector(1.0D, 0.0D, 0.0D), loc.getYaw()).toLocation(attacker.getWorld()).add(loc);
    standStart.setYaw(loc.getYaw());
    ArmorStand stand = (ArmorStand) attacker.getWorld().spawnEntity(standStart, EntityType.ARMOR_STAND);
    stand.setPersistent(false);
    stand.setVisible(false);
    if(ServerVersion.Version.isCurrentHigher(ServerVersion.Version.v1_8_8)) {
      stand.setInvulnerable(true);
      stand.setSilent(true);
    }

    VersionUtils.setItemInHand(stand, plugin.getSwordSkinManager().getMurdererSword(attacker));

    stand.setRightArmPose(new EulerAngle(Math.toRadians(350.0), Math.toRadians(loc.getPitch() * -1.0), Math.toRadians(90.0)));
    VersionUtils.setCollidable(stand, false);

    stand.setGravity(false);
    stand.setRemoveWhenFarAway(true);

    if(ServerVersion.Version.isCurrentEqualOrHigher(ServerVersion.Version.v1_8_8)) {
      stand.setMarker(true);
    }

    Location initialise = plugin.getBukkitHelper().rotateAroundAxisY(new Vector(-0.8D, 1.45D, 0.0D), loc.getYaw()).toLocation(attacker.getWorld()).add(standStart)
      .add(plugin.getBukkitHelper().rotateAroundAxisY(plugin.getBukkitHelper().rotateAroundAxisX(new Vector(0.0D, 0.0D, 1.0D), loc.getPitch()), loc.getYaw()));
    int maxRange = plugin.getConfig().getInt("Sword.Fly.Range", 20);
    double maxHitRange = plugin.getConfig().getDouble("Sword.Fly.Radius", 0.5);
    new BukkitRunnable() {
      @Override
      public void run() {
        if(!stand.isValid() || sourceArena.getArenaState() != IArenaState.IN_GAME
          || sourceArena.getVotingManager().isVotingActive() || sourceArena.getCutsceneManager().isPlaying()) {
          cancel();
          stand.remove();
          return;
        }
        VersionUtils.teleport(stand, standStart.add(vec));
        initialise.add(vec);
        for(Entity entity : initialise.getWorld().getNearbyEntities(initialise, maxHitRange, maxHitRange, maxHitRange)) {
          if(entity instanceof Player) {
            Player victim = (Player) entity;
            Arena arena = plugin.getArenaRegistry().getArena(victim);
            if(arena == null || arena != sourceArena || victim.equals(attacker)) {
              continue;
            }
            IUser victimUser = plugin.getUserManager().getUser(victim);
            if(ArenaUtils.isSpectator(victim, arena)
              || Role.isRole(Role.MURDERER, victimUser, arena)
              || (arena.getVentManager() != null && arena.getVentManager().isInsideVent(victim))) {
              continue;
            }
            killBySword(arena, attackerUser, victim);
            cancel();
            stand.remove();
            return;
          }
        }
        if(loc.distance(initialise) > maxRange || initialise.getBlock().getType().isSolid()) {
          cancel();
          stand.remove();
        }
      }
    }.runTaskTimer(plugin, 0, 1);
  }

  private void killBySword(Arena arena, IUser attackerUser, Player victim) {
    if(arena == null) {
      return;
    }
    IUser user = plugin.getUserManager().getUser(victim);
    if(Role.isRole(Role.MURDERER, user, arena) || ArenaUtils.isSpectator(victim, arena)) {
      return;
    }
    arena.getVotingManager().recordKill(attackerUser.getPlayer());
    XSound.ENTITY_PLAYER_DEATH.play(victim.getLocation());
    victim.damage(100.0);
    if(!arena.getVotingManager().isVotingActive() && arena.getArenaState() == IArenaState.IN_GAME) {
      HalloweenChat.sendTitles(
        victim,
        new MessageBuilder("IN_GAME_DEATH_SCREEN").asKey().player(victim).arena(arena).build(),
        new MessageBuilder("IN_GAME_MESSAGES_GAME_END_PLACEHOLDERS_MURDERER_KILLED_YOU").asKey().build(),
        5, 50, 15
      );
    }
    attackerUser.adjustStatistic("LOCAL_KILLS", 1);
    attackerUser.adjustStatistic("KILLS", 1);
  }

  @EventHandler(priority = EventPriority.HIGH)
  // highest priority to fully protect our game
  public void onBlockBreak(BlockBreakEvent event) {
    Arena arena = plugin.getArenaRegistry().getArena(event.getPlayer());
    if(arena == null) {
      return;
    }
    event.setCancelled(true);
    if(event.getBlock().getType() != XMaterial.ARMOR_STAND.parseMaterial()) {
      return;
    }
    plugin.getHologramManager().getArmorStands().removeIf(armorStand -> {
      boolean isSameType = armorStand.getLocation().getBlock().getType() == event.getBlock().getType();
      if(isSameType) {
        armorStand.remove();
        armorStand.setCustomNameVisible(false);
      }
      return isSameType;
    });
  }

  @EventHandler(priority = EventPriority.HIGH)
  // highest priority to fully protect our game
  public void onBuild(BlockPlaceEvent event) {
    Arena arena = plugin.getArenaRegistry().getArena(event.getPlayer());
    if(arena == null) {
      return;
    }
    event.setCancelled(true);
  }
}
