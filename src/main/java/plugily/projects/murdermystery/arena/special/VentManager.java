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
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import plugily.projects.minigamesbox.api.user.IUser;
import plugily.projects.minigamesbox.classic.utils.version.VersionUtils;
import plugily.projects.minigamesbox.classic.utils.version.xseries.XSound;
import plugily.projects.murdermystery.Main;
import plugily.projects.murdermystery.arena.Arena;
import plugily.projects.murdermystery.arena.role.Role;
import plugily.projects.murdermystery.arena.sabotage.SabotageManager;
import plugily.projects.murdermystery.utils.HalloweenChat;
import plugily.projects.murdermystery.utils.ItemPosition;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class VentManager {

  private final Main plugin;
  private final Arena arena;

  private final Map<UUID, VentSession> activeSessions = new HashMap<>();
  private final Map<UUID, Long> ventCooldowns = new HashMap<>();
  private final Map<UUID, Long> lastSwitchTime = new HashMap<>();

  public static class VentSession {
    final ItemStack[] savedInventory;
    final ItemStack[] savedArmor;
    final ItemStack savedOffHand;
    int currentVentIndex;
    final List<SpecialBlock> sewerBlocks;
    final long enteredAtMs;

    VentSession(ItemStack[] savedInventory, ItemStack[] savedArmor, ItemStack savedOffHand, int currentVentIndex, List<SpecialBlock> sewerBlocks) {
      this.savedInventory = savedInventory;
      this.savedArmor = savedArmor;
      this.savedOffHand = savedOffHand;
      this.currentVentIndex = currentVentIndex;
      this.sewerBlocks = sewerBlocks;
      this.enteredAtMs = System.currentTimeMillis();
    }
  }

  public VentManager(Main plugin, Arena arena) {
    this.plugin = plugin;
    this.arena = arena;
  }

  public boolean isInsideVent(Player player) {
    return activeSessions.containsKey(player.getUniqueId());
  }

  public boolean hasJustEntered(Player player) {
    VentSession session = activeSessions.get(player.getUniqueId());
    return session != null && (System.currentTimeMillis() - session.enteredAtMs) < 300L;
  }

  public Location getCurrentVentLocation(Player player) {
    VentSession session = activeSessions.get(player.getUniqueId());
    if(session == null) {
      return null;
    }
    SpecialBlock vent = session.sewerBlocks.get(session.currentVentIndex);
    return vent.getLocation().clone().add(0.5, 1.0, 0.5);
  }

  public int getCurrentVentIndex(Player player) {
    VentSession session = activeSessions.get(player.getUniqueId());
    return session == null ? -1 : session.currentVentIndex;
  }

  public int getTotalVents(Player player) {
    VentSession session = activeSessions.get(player.getUniqueId());
    return session == null ? 0 : session.sewerBlocks.size();
  }

  public void updateSavedItem(Player player, int slot, ItemStack item) {
    VentSession session = activeSessions.get(player.getUniqueId());
    if(session != null && session.savedInventory != null && slot >= 0 && slot < session.savedInventory.length) {
      session.savedInventory[slot] = item;
    }
  }

  public boolean enterVent(Player player, SpecialBlock currentBlock) {
    IUser checkUser = plugin.getUserManager().getUser(player);
    if(checkUser == null || checkUser.isSpectator() || arena.isDeathPlayer(player)
      || arena.getVotingManager().isVotingActive() || arena.getCutsceneManager().isPlaying()) {
      return false;
    }
    long now = System.currentTimeMillis();
    long nextAllowed = ventCooldowns.getOrDefault(player.getUniqueId(), 0L);
    if(now < nextAllowed) {
      long remaining = (nextAllowed - now + 999) / 1000;
      HalloweenChat.sendActionBar(player, HalloweenChat.color("<#5A4E7C>[<#FF3B47>Sewer Vent<#5A4E7C>] <#FF3B47>คูลดาวน์ <#FFB703>" + remaining + " วิ"));
      return false;
    }

    List<SpecialBlock> sewerBlocks = new ArrayList<>();
    for(SpecialBlock sb : arena.getSpecialBlocks()) {
      if(sb.getSpecialBlockType() == SpecialBlock.SpecialBlockType.RAPID_TELEPORTATION) {
        sewerBlocks.add(sb);
      }
    }

    if(sewerBlocks.size() < 2) {
      player.sendMessage(HalloweenChat.color("<#5A4E7C>[<#FF3B47>Sewer Vent<#5A4E7C>] <#FF3B47>ไม่มีท่อระบายน้ำจุดอื่นให้เชื่อมต่อในแผนที่นี้"));
      return false;
    }

    int startIndex = -1;
    if(currentBlock != null) {
      for(int i = 0; i < sewerBlocks.size(); i++) {
        if(sewerBlocks.get(i).getLocation().getBlock().equals(currentBlock.getLocation().getBlock())) {
          startIndex = i;
          break;
        }
      }
    }
    if(startIndex == -1) {
      startIndex = 0;
    }

    ItemStack[] saved = player.getInventory().getContents().clone();
    ItemStack[] savedArmor = player.getInventory().getArmorContents().clone();
    ItemStack savedOffHand = player.getInventory().getItemInOffHand() != null ? player.getInventory().getItemInOffHand().clone() : null;
    player.getInventory().clear();
    player.getInventory().setArmorContents(null);
    player.getInventory().setItemInOffHand(null);

    ItemStack prev = new ItemStack(Material.SPECTRAL_ARROW);
    ItemMeta metaPrev = prev.getItemMeta();
    if(metaPrev != null) {
      metaPrev.setDisplayName(HalloweenChat.color("<#5A4E7C>[<#FF7518>ห้องก่อนหน้า<#5A4E7C>]"));
      metaPrev.setLore(HalloweenChat.colorLines(
        "<#B8B0C8>คลิกเพื่อสลับไปท่อห้องก่อนหน้า",
        "<#5A4E7C>(<#B8B0C8>คลิกเพื่อเปลี่ยนห้อง<#5A4E7C>)"
      ));
      prev.setItemMeta(metaPrev);
    }
    player.getInventory().setItem(0, prev);
    player.getInventory().setItem(3, prev);

    ItemStack exit = new ItemStack(Material.IRON_TRAPDOOR);
    ItemMeta metaExit = exit.getItemMeta();
    if(metaExit != null) {
      metaExit.setDisplayName(HalloweenChat.color("<#5A4E7C>[<#FF3B47>ออกจากท่อ<#5A4E7C>]"));
      metaExit.setLore(HalloweenChat.colorLines(
        "<#B8B0C8>คลิกไอเทมนี้หรือกด <#FFB703>SHIFT <#B8B0C8>เพื่อออกจากท่อ",
        "<#5A4E7C>(<#B8B0C8>คลิกเพื่อโผล่ขึ้นมา<#5A4E7C>)"
      ));
      exit.setItemMeta(metaExit);
    }
    player.getInventory().setItem(4, exit);

    ItemStack next = new ItemStack(Material.SPECTRAL_ARROW);
    ItemMeta metaNext = next.getItemMeta();
    if(metaNext != null) {
      metaNext.setDisplayName(HalloweenChat.color("<#5A4E7C>[<#FF7518>ห้องถัดไป<#5A4E7C>]"));
      metaNext.setLore(HalloweenChat.colorLines(
        "<#B8B0C8>คลิกเพื่อสลับไปท่อห้องถัดไป",
        "<#5A4E7C>(<#B8B0C8>คลิกเพื่อเปลี่ยนห้อง<#5A4E7C>)"
      ));
      next.setItemMeta(metaNext);
    }
    player.getInventory().setItem(5, next);
    player.getInventory().setItem(8, next);
    player.getInventory().setHeldItemSlot(4);

    if(arena.getSabotageManager() != null) {
      player.getInventory().setItem(SabotageManager.SABOTAGE_REMOTE_SLOT, arena.getSabotageManager().createSabotageRemoteItem());
    }

    if(arena.getMinimap() != null) {
      player.getInventory().setItemInOffHand(arena.getMinimap().createMapItem(player));
    }

    activeSessions.put(player.getUniqueId(), new VentSession(saved, savedArmor, savedOffHand, startIndex, sewerBlocks));

    try {
      PotionEffectType darkness = PotionEffectType.DARKNESS;
      if(darkness != null) {
        player.addPotionEffect(new PotionEffect(darkness, 999999, 0, false, false, false));
      }
    } catch(Throwable e) {
      try {
        PotionEffectType darkness = PotionEffectType.getByName("DARKNESS");
        if(darkness != null) {
          player.addPotionEffect(new PotionEffect(darkness, 999999, 0, false, false, false));
        } else {
          player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 999999, 0, false, false, false));
        }
      } catch(Throwable ignored) {
      }
    }
    player.setRemainingAir(player.getMaximumAir());

    player.setInvisible(true);
    player.setAllowFlight(true);
    player.setFlying(true);
    for(Player p : arena.getPlayers()) {
      if(!p.equals(player)) {
        p.hidePlayer(plugin, player);
      }
    }

    Location insideLoc = sewerBlocks.get(startIndex).getLocation().clone().add(0.5, 1.0, 0.5);
    insideLoc.setYaw(player.getLocation().getYaw());
    insideLoc.setPitch(player.getLocation().getPitch());
    VersionUtils.teleport(player, insideLoc);

    XSound.BLOCK_IRON_TRAPDOOR_CLOSE.play(player.getLocation(), 1F, 0.8F);
    XSound.ENTITY_BAT_TAKEOFF.play(player.getLocation(), 1F, 0.6F);
    VersionUtils.sendParticles("SMOKE_LARGE", player, insideLoc.clone().add(0, 0.2, 0), 15);

    HalloweenChat.sendActionBar(player, HalloweenChat.color("<#5A4E7C>[<#FF3B47>Sewer Vent<#5A4E7C>] <#F4F1DE>จุดที่ <#FFB703>" + (startIndex + 1) + "/" + sewerBlocks.size()
      + " <#5A4E7C>(<#B8B0C8>คลิกขวาเพื่อเปลี่ยนห้อง หรือกด <#FFB703>SHIFT <#B8B0C8>เพื่อออก<#5A4E7C>)"));
    return true;
  }

  public void previousVent(Player player) {
    long now = System.currentTimeMillis();
    if(now - lastSwitchTime.getOrDefault(player.getUniqueId(), 0L) < 200L) {
      return;
    }
    lastSwitchTime.put(player.getUniqueId(), now);

    VentSession session = activeSessions.get(player.getUniqueId());
    if(session == null) {
      return;
    }

    session.currentVentIndex = (session.currentVentIndex - 1 + session.sewerBlocks.size()) % session.sewerBlocks.size();
    moveToCurrentVent(player, session);
  }

  public void nextVent(Player player) {
    long now = System.currentTimeMillis();
    if(now - lastSwitchTime.getOrDefault(player.getUniqueId(), 0L) < 200L) {
      return;
    }
    lastSwitchTime.put(player.getUniqueId(), now);

    VentSession session = activeSessions.get(player.getUniqueId());
    if(session == null) {
      return;
    }

    session.currentVentIndex = (session.currentVentIndex + 1) % session.sewerBlocks.size();
    moveToCurrentVent(player, session);
  }

  private void moveToCurrentVent(Player player, VentSession session) {
    SpecialBlock vent = session.sewerBlocks.get(session.currentVentIndex);
    Location loc = vent.getLocation().clone().add(0.5, 1.0, 0.5);
    loc.setYaw(player.getLocation().getYaw());
    loc.setPitch(player.getLocation().getPitch());
    VersionUtils.teleport(player, loc);

    XSound.BLOCK_IRON_TRAPDOOR_CLOSE.play(loc, 1F, 1.2F);
    XSound.ENTITY_BAT_TAKEOFF.play(loc, 1F, 0.8F);

    HalloweenChat.sendActionBar(player, HalloweenChat.color("<#5A4E7C>[<#FF3B47>Sewer Vent<#5A4E7C>] <#F4F1DE>จุดที่ <#FFB703>" + (session.currentVentIndex + 1) + "/" + session.sewerBlocks.size()
      + " <#5A4E7C>(<#B8B0C8>คลิกขวาเพื่อเปลี่ยนห้อง หรือกด <#FFB703>SHIFT <#B8B0C8>เพื่อออก<#5A4E7C>)"));
  }

  public void exitVent(Player player) {
    VentSession session = activeSessions.remove(player.getUniqueId());
    if(session == null) {
      return;
    }
    lastSwitchTime.remove(player.getUniqueId());

    player.removePotionEffect(PotionEffectType.BLINDNESS);
    try {
      player.removePotionEffect(PotionEffectType.DARKNESS);
    } catch(Throwable ignored) {
      try {
        PotionEffectType darkness = PotionEffectType.getByName("DARKNESS");
        if(darkness != null) {
          player.removePotionEffect(darkness);
        }
      } catch(Throwable ignored2) {
      }
    }
    player.setRemainingAir(player.getMaximumAir());
    player.setInvisible(false);
    player.setFlying(false);
    player.setAllowFlight(false);
    for(Player p : arena.getPlayers()) {
      p.showPlayer(plugin, player);
    }

    if(plugin.getArenaRegistry().getArena(player) != arena) {
      return;
    }

    SpecialBlock currentVent = session.sewerBlocks.get(session.currentVentIndex);
    Location exitLoc = currentVent.getLocation().clone().add(0.5, 1.0, 0.5);
    exitLoc.setYaw(player.getLocation().getYaw());
    exitLoc.setPitch(player.getLocation().getPitch());
    VersionUtils.teleport(player, exitLoc);

    player.getInventory().clear();
    if(session.savedInventory != null) {
      player.getInventory().setContents(session.savedInventory);
    }
    if(session.savedArmor != null) {
      player.getInventory().setArmorContents(session.savedArmor);
    }
    if(session.savedOffHand != null) {
      player.getInventory().setItemInOffHand(session.savedOffHand);
    }

    int inGameLength = plugin.getConfig().getInt("Time-Manager.In-Game", 270);
    if(arena.getTimer() <= (inGameLength - 14)) {
      IUser user = plugin.getUserManager().getUser(player);
      if(user != null && !user.isSpectator() && Role.isRole(Role.MURDERER, user, arena)) {
        int swordSlot = ItemPosition.MURDERER_SWORD.getMurdererItemPosition();
        ItemStack currentSword = player.getInventory().getItem(swordSlot);
        if(currentSword == null || currentSword.getType() == Material.AIR) {
          ItemStack sword = plugin.getSwordSkinManager().getMurdererSword(player);
          if(sword == null) {
            sword = plugin.getSwordSkinManager().getRandomSwordSkin(player);
          }
          ItemPosition.setItem(user, ItemPosition.MURDERER_SWORD, sword);
        }
      }
    }
    player.updateInventory();

    XSound.BLOCK_IRON_TRAPDOOR_OPEN.play(exitLoc, 1F, 1F);
    VersionUtils.sendParticles("PORTAL", player, exitLoc, 25);

    int cooldownSeconds = plugin.getConfig().getInt("Vent.Cooldown", 10);
    ventCooldowns.put(player.getUniqueId(), System.currentTimeMillis() + (cooldownSeconds * 1000L));

    HalloweenChat.sendActionBar(player, HalloweenChat.color("<#5A4E7C>[<#FF3B47>Sewer Vent<#5A4E7C>] <#6EEB83>ออกจากท่อแล้ว <#5A4E7C>(<#B8B0C8>คูลดาวน์ " + cooldownSeconds + " วิ<#5A4E7C>)"));
  }

  public void cleanup() {
    for(UUID uuid : new ArrayList<>(activeSessions.keySet())) {
      Player player = Bukkit.getPlayer(uuid);
      if(player != null && player.isOnline()) {
        exitVent(player);
      }
    }
    activeSessions.clear();
    ventCooldowns.clear();
    lastSwitchTime.clear();
  }
}
