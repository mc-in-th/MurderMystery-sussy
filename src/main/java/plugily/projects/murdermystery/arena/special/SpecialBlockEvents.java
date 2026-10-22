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

package plugily.projects.murdermystery.arena.special;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import plugily.projects.minigamesbox.api.arena.IArenaState;
import plugily.projects.minigamesbox.api.user.IUser;
import plugily.projects.minigamesbox.classic.handlers.language.MessageBuilder;
import plugily.projects.minigamesbox.classic.utils.helper.ItemBuilder;
import plugily.projects.minigamesbox.classic.utils.helper.ItemUtils;
import plugily.projects.minigamesbox.classic.utils.misc.complement.ComplementAccessor;
import plugily.projects.minigamesbox.classic.utils.version.ServerVersion;
import plugily.projects.minigamesbox.classic.utils.version.VersionUtils;
import plugily.projects.minigamesbox.classic.utils.version.xseries.XMaterial;
import plugily.projects.murdermystery.Main;
import plugily.projects.murdermystery.api.events.game.MurderGameBuyEvent;
import plugily.projects.murdermystery.arena.Arena;
import plugily.projects.murdermystery.arena.role.Role;
import plugily.projects.murdermystery.arena.sabotage.SabotageManager;
import plugily.projects.murdermystery.arena.special.mysterypotion.MysteryPotion;
import plugily.projects.murdermystery.arena.special.mysterypotion.MysteryPotionRegistry;
import plugily.projects.murdermystery.arena.special.pray.PrayerRegistry;
import plugily.projects.murdermystery.utils.HalloweenChat;
import plugily.projects.murdermystery.utils.ItemPosition;


/**
 * @author Plajer
 * <p>
 * Created at 16.10.2018
 */
public class SpecialBlockEvents implements Listener {

  private final Main plugin;

  public SpecialBlockEvents(Main plugin) {
    this.plugin = plugin;
    plugin.getServer().getPluginManager().registerEvents(this, plugin);
  }

  @EventHandler
  public void onSpecialBlockClick(PlayerInteractEvent event) {
    if(event.getClickedBlock() == null)
      return;

    if(ServerVersion.Version.isCurrentEqualOrHigher(ServerVersion.Version.v1_11) && event.getHand() == EquipmentSlot.OFF_HAND) {
      return;
    }

    Arena arena = plugin.getArenaRegistry().getArena(event.getPlayer());
    if(arena == null) {
      return;
    }

    if(arena.getArenaState() != IArenaState.IN_GAME || plugin.getUserManager().getUser(event.getPlayer()).isSpectator()) {
      return;
    }

    if(arena.getVentManager() != null && arena.getVentManager().isInsideVent(event.getPlayer())) {
      return;
    }

    for(SpecialBlock specialBlock : arena.getSpecialBlocks()) {
      if(specialBlock.getLocation().getBlock().equals(event.getClickedBlock())) {
        if(specialBlock.getSpecialBlockType() == SpecialBlock.SpecialBlockType.RAPID_TELEPORTATION) {
          onSewerVentClick(event, specialBlock);
          return;
        }
      }
    }
  }

  @EventHandler
  public void onMysteryPotionDrink(PlayerItemConsumeEvent event) {
    ItemStack item = event.getItem();
    if(item.getType() != XMaterial.POTION.parseMaterial() || !ItemUtils.isItemStackNamed(item)) {
      return;
    }

    if(plugin.getArenaRegistry().getArena(event.getPlayer()) == null) {
      return;
    }

    String itemDisplayName = ComplementAccessor.getComplement().getDisplayName(item.getItemMeta());
    IUser user = plugin.getUserManager().getUser(event.getPlayer());
    for(MysteryPotion potion : MysteryPotionRegistry.getMysteryPotions()) {
      if(itemDisplayName.equals(potion.getName())) {
        event.setCancelled(true);
        event.getPlayer().sendMessage(potion.getSubtitle());
        HalloweenChat.sendTitles(event.getPlayer(), "", potion.getSubtitle(), 5, 40, 5);
        ItemPosition.setItem(user, ItemPosition.POTION, null);
        event.getPlayer().addPotionEffect(potion.getPotionEffect());
        return;
      }
    }
  }

  @EventHandler
  public void onPlayerToggleSneak(PlayerToggleSneakEvent event) {
    if(!event.isSneaking()) {
      return;
    }
    Player player = event.getPlayer();
    Arena arena = plugin.getArenaRegistry().getArena(player);
    if(arena == null || arena.getArenaState() != IArenaState.IN_GAME || arena.getVentManager() == null) {
      return;
    }

    if(arena.getVentManager().isInsideVent(player)) {
      arena.getVentManager().exitVent(player);
      return;
    }

    IUser user = plugin.getUserManager().getUser(player);
    if(!Role.isRole(Role.MURDERER, user, arena)) {
      return;
    }

    for(SpecialBlock sb : arena.getSpecialBlocks()) {
      if(sb.getSpecialBlockType() == SpecialBlock.SpecialBlockType.RAPID_TELEPORTATION) {
        Location ventLoc = sb.getLocation().clone().add(0.5, 0.5, 0.5);
        if(player.getWorld().equals(ventLoc.getWorld()) && player.getLocation().distanceSquared(ventLoc) <= 3.5) {
          arena.getVentManager().enterVent(player, sb);
          return;
        }
      }
    }
  }

  private void onSewerVentClick(PlayerInteractEvent event, SpecialBlock currentBlock) {
    event.setCancelled(true);
    Player player = event.getPlayer();
    Arena arena = plugin.getArenaRegistry().getArena(player);
    if(arena == null || arena.getVentManager() == null) {
      return;
    }
    IUser user = plugin.getUserManager().getUser(player);

    if(!Role.isRole(Role.MURDERER, user, arena)) {
      player.sendMessage(HalloweenChat.color("<#5A4E7C>[<#FF3B47>Sewer Vent<#5A4E7C>] <#B8B0C8>ท่อระบายน้ำถูกปิดสนิท มีเพียงฆาตกรเท่านั้นที่มุดผ่านได้"));
      return;
    }

    if(arena.getVentManager().isInsideVent(player)) {
      arena.getVentManager().exitVent(player);
    } else {
      arena.getVentManager().enterVent(player, currentBlock);
    }
  }

  @EventHandler
  public void onVentInteract(PlayerInteractEvent event) {
    if(ServerVersion.Version.isCurrentEqualOrHigher(ServerVersion.Version.v1_11) && event.getHand() == EquipmentSlot.OFF_HAND) {
      return;
    }
    Player player = event.getPlayer();
    Arena arena = plugin.getArenaRegistry().getArena(player);
    if(arena == null || arena.getArenaState() != IArenaState.IN_GAME || arena.getVentManager() == null) {
      return;
    }
    if(!arena.getVentManager().isInsideVent(player)) {
      return;
    }

    event.setCancelled(true);

    ItemStack item = event.getItem();
    int slot = player.getInventory().getHeldItemSlot();

    if(slot == SabotageManager.SABOTAGE_REMOTE_SLOT || (item != null && arena.getSabotageManager() != null && arena.getSabotageManager().isSabotageRemote(item))) {
      arena.getSabotageManager().openSabotageGUI(player);
    } else if(slot == 0 || slot == 3 || (item != null && item.getType() == Material.SPECTRAL_ARROW && item.hasItemMeta() && HalloweenChat.strip(item.getItemMeta().getDisplayName()).contains("ห้องก่อนหน้า"))) {
      arena.getVentManager().previousVent(player);
    } else if(slot == 8 || slot == 5 || (item != null && item.getType() == Material.SPECTRAL_ARROW && item.hasItemMeta() && HalloweenChat.strip(item.getItemMeta().getDisplayName()).contains("ห้องถัดไป"))) {
      arena.getVentManager().nextVent(player);
    } else if(slot == 4 || (item != null && item.getType() == Material.IRON_TRAPDOOR)) {
      arena.getVentManager().exitVent(player);
    }
  }

  @EventHandler
  public void onVentInventoryClick(InventoryClickEvent event) {
    if(!(event.getWhoClicked() instanceof Player)) {
      return;
    }
    Player player = (Player) event.getWhoClicked();
    Arena arena = plugin.getArenaRegistry().getArena(player);
    if(arena == null || arena.getVentManager() == null) {
      return;
    }
    if(arena.getVentManager().isInsideVent(player)) {
      event.setCancelled(true);
      int slot = event.getSlot();
      if(slot == SabotageManager.SABOTAGE_REMOTE_SLOT || (event.getCurrentItem() != null && arena.getSabotageManager() != null && arena.getSabotageManager().isSabotageRemote(event.getCurrentItem()))) {
        arena.getSabotageManager().openSabotageGUI(player);
      } else if(slot == 0 || slot == 3) {
        arena.getVentManager().previousVent(player);
      } else if(slot == 8 || slot == 5) {
        arena.getVentManager().nextVent(player);
      } else if(slot == 4) {
        arena.getVentManager().exitVent(player);
      }
    }
  }

  @EventHandler
  public void onVentDropItem(PlayerDropItemEvent event) {
    Player player = event.getPlayer();
    Arena arena = plugin.getArenaRegistry().getArena(player);
    if(arena != null && arena.getVentManager() != null && arena.getVentManager().isInsideVent(player)) {
      event.setCancelled(true);
    }
  }

  @EventHandler
  public void onVentEntityDamage(EntityDamageEvent event) {
    if(event.getEntity() instanceof Player) {
      Player player = (Player) event.getEntity();
      Arena arena = plugin.getArenaRegistry().getArena(player);
      if(arena != null && arena.getVentManager() != null && arena.getVentManager().isInsideVent(player)) {
        event.setCancelled(true);
      }
    }
  }

  @EventHandler
  public void onVentEntityDamageByEntity(EntityDamageByEntityEvent event) {
    if(event.getDamager() instanceof Player) {
      Player player = (Player) event.getDamager();
      Arena arena = plugin.getArenaRegistry().getArena(player);
      if(arena != null && arena.getVentManager() != null && arena.getVentManager().isInsideVent(player)) {
        event.setCancelled(true);
      }
    }
  }

  @EventHandler
  public void onVentPlayerMove(PlayerMoveEvent event) {
    Player player = event.getPlayer();
    Arena arena = plugin.getArenaRegistry().getArena(player);
    if(arena == null || arena.getArenaState() != IArenaState.IN_GAME || arena.getVentManager() == null) {
      return;
    }
    if(!arena.getVentManager().isInsideVent(player)) {
      return;
    }

    Location from = event.getFrom();
    Location to = event.getTo();
    if(to == null) {
      return;
    }

    if(from.getX() != to.getX() || from.getY() != to.getY() || from.getZ() != to.getZ()) {
      Location ventLoc = arena.getVentManager().getCurrentVentLocation(player);
      if(ventLoc != null) {
        Location fixed = ventLoc.clone();
        fixed.setYaw(to.getYaw());
        fixed.setPitch(to.getPitch());
        event.setTo(fixed);
      }
    }
  }

  @EventHandler
  public void onVentPlayerQuit(PlayerQuitEvent event) {
    Player player = event.getPlayer();
    Arena arena = plugin.getArenaRegistry().getArena(player);
    if(arena != null && arena.getVentManager() != null && arena.getVentManager().isInsideVent(player)) {
      arena.getVentManager().exitVent(player);
    }
  }

  @EventHandler
  public void onPlayerJoin(PlayerJoinEvent event) {
    Player player = event.getPlayer();
    Arena arena = plugin.getArenaRegistry().getArena(player);
    if(arena != null && arena.getArenaState() == IArenaState.IN_GAME) {
      IUser user = plugin.getUserManager().getUser(player);
      if(Role.isRole(Role.MURDERER, user, arena)) {
        arena.showVentDisplays(player);
      } else {
        arena.hideVentDisplays(player);
      }
    }
  }

}
