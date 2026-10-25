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

package plugily.projects.murdermystery.arena.task.minigame;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import plugily.projects.minigamesbox.classic.utils.version.xseries.XMaterial;
import plugily.projects.minigamesbox.classic.utils.version.xseries.XSound;
import plugily.projects.murdermystery.arena.Arena;
import plugily.projects.murdermystery.arena.task.TaskType;
import plugily.projects.murdermystery.utils.HalloweenChat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class ClearAsteroidsMinigame implements TaskMinigame {

  public static final String GUI_TITLE = HalloweenChat.color("<#5A4E7C>[<#FF7518>ป้อมปืนยิงสกัดสิ่งกีดขวาง<#5A4E7C>]");
  private static final int TARGET_HITS = 8;
  private static final int[] GRID_SLOTS = {
    10, 11, 12, 13, 14, 15, 16,
    19, 20, 21, 22, 23, 24, 25,
    28, 29, 30, 31, 32, 33, 34
  };

  private static class AsteroidSession {
    int hits = 0;
    final Set<Integer> activeSlots = new HashSet<>();

    AsteroidSession() {
      spawnTargets(3);
    }

    void spawnTargets(int count) {
      List<Integer> available = new ArrayList<>();
      for(int slot : GRID_SLOTS) {
        if(!activeSlots.contains(slot)) {
          available.add(slot);
        }
      }
      Collections.shuffle(available);
      for(int i = 0; i < Math.min(count, available.size()); i++) {
        activeSlots.add(available.get(i));
      }
    }
  }

  private final Arena arena;
  private final Map<UUID, AsteroidSession> sessions = new HashMap<>();
  private final Set<UUID> completedSessions = new HashSet<>();

  public ClearAsteroidsMinigame(Arena arena) {
    this.arena = arena;
  }

  @Override
  public TaskType getTaskType() {
    return TaskType.CLEAR_ASTEROIDS;
  }

  @Override
  public String getGuiTitle() {
    return GUI_TITLE;
  }

  @Override
  public boolean isGui(String title) {
    if(title == null) {
      return false;
    }
    String plain = HalloweenChat.strip(title);
    return plain.contains("ป้อมปืนยิงสกัดสิ่งกีดขวาง") || plain.contains("Weapons");
  }

  @Override
  public void open(Player player) {
    completedSessions.remove(player.getUniqueId());
    AsteroidSession session = new AsteroidSession();
    sessions.put(player.getUniqueId(), session);

    Inventory inv = Bukkit.createInventory(null, 45, GUI_TITLE);
    buildGui(inv, session);
    player.openInventory(inv);

    XSound.BLOCK_BEACON_ACTIVATE.play(player.getLocation(), 0.8F, 1.5F);
    HalloweenChat.sendActionBar(player, HalloweenChat.color("<#5A4E7C>[<#FF7518>Task<#5A4E7C>] <#F4F1DE>คลิกยิงทำลายสิ่งกีดขวางในเรดาร์ให้ครบ 8 เป้าหมาย"));
  }

  private void buildGui(Inventory inv, AsteroidSession session) {
    ItemStack bg = XMaterial.BLACK_STAINED_GLASS_PANE.parseItem();
    ItemMeta bgMeta = bg.getItemMeta();
    if(bgMeta != null) {
      bgMeta.setDisplayName(" ");
      try {
        bgMeta.setHideTooltip(true);
      } catch(Throwable ignored) {
      }
      bg.setItemMeta(bgMeta);
    }
    for(int i = 0; i < 45; i++) {
      inv.setItem(i, bg);
    }

    ItemStack instruction = new ItemStack(Material.BOOK);
    ItemMeta iMeta = instruction.getItemMeta();
    if(iMeta != null) {
      iMeta.setDisplayName(HalloweenChat.color("<#5A4E7C>[<#72EFDD>Turret Defense<#5A4E7C>]"));
      iMeta.setLore(HalloweenChat.colorLines(
        "",
        "<#F4F1DE>1. คลิกเป้าหมายลูกไฟในจอเรดาร์เพื่อยิงทำลาย",
        "<#F4F1DE>2. ยิงสกัดสิ่งกีดขวางให้ครบทั้งหมด 8 เป้าหมาย",
        "",
        "<#5A4E7C>(<#72EFDD>Visual Task ยืนยันตัวตนต่อผู้เล่นรอบข้าง<#5A4E7C>)"
      ));
      instruction.setItemMeta(iMeta);
    }
    inv.setItem(0, instruction);

    ItemStack status = new ItemStack(Material.CROSSBOW);
    ItemMeta sMeta = status.getItemMeta();
    if(sMeta != null) {
      sMeta.setDisplayName(HalloweenChat.color("<#5A4E7C>[<#FF7518>ระบบควบคุมป้อมปืน<#5A4E7C>]"));
      sMeta.setLore(HalloweenChat.colorLines(
        "",
        "<#F4F1DE>เป้าหมายที่ทำลายแล้ว <#6EEB83>" + session.hits + "<#5A4E7C>/<#F4F1DE>" + TARGET_HITS,
        "",
        "<#5A4E7C>(<#B8B0C8>คลิกเป้าหมายในจอเรดาร์เพื่อยิง<#5A4E7C>)"
      ));
      status.setItemMeta(sMeta);
    }
    inv.setItem(4, status);

    for(int slot : GRID_SLOTS) {
      if(session.activeSlots.contains(slot)) {
        ItemStack target = XMaterial.FIRE_CHARGE.parseItem();
        ItemMeta tMeta = target.getItemMeta();
        if(tMeta != null) {
          tMeta.setDisplayName(HalloweenChat.color("<#5A4E7C>[<#FF3B47>เป้าหมายสิ่งกีดขวาง<#5A4E7C>]"));
          tMeta.setLore(HalloweenChat.colorLines("", "<#5A4E7C>(<#B8B0C8>กดคลิกเพื่อยิงสกัด<#5A4E7C>)"));
          target.setItemMeta(tMeta);
        }
        inv.setItem(slot, target);
      } else {
        ItemStack empty = XMaterial.GREEN_STAINED_GLASS_PANE.parseItem();
        ItemMeta eMeta = empty.getItemMeta();
        if(eMeta != null) {
          eMeta.setDisplayName(HalloweenChat.color("<#5A4E7C>[<#6EEB83>จอเรดาร์ปลอดโปร่ง<#5A4E7C>]"));
          empty.setItemMeta(eMeta);
        }
        inv.setItem(slot, empty);
      }
    }
  }

  @Override
  public boolean handleClick(Player player, int rawSlot, ClickType clickType) {
    if(completedSessions.contains(player.getUniqueId())) {
      return true;
    }
    AsteroidSession session = sessions.get(player.getUniqueId());
    if(session == null) {
      return true;
    }

    if(rawSlot == 0 || rawSlot == 4) {
      XSound.UI_BUTTON_CLICK.play(player.getLocation(), 0.5F, 1.2F);
      return true;
    }

    if(session.activeSlots.remove(rawSlot)) {
      session.hits++;
      if(session.hits < TARGET_HITS) {
        session.spawnTargets(1);
      }

      arena.getTaskManager().getVisualEffectManager().triggerStepEffect(player, TaskType.CLEAR_ASTEROIDS, session.hits, TARGET_HITS);
      buildGui(player.getOpenInventory().getTopInventory(), session);

      if(session.hits >= TARGET_HITS) {
        completedSessions.add(player.getUniqueId());
        sessions.remove(player.getUniqueId());
        XSound.ENTITY_PLAYER_LEVELUP.play(player.getLocation(), 1.0F, 1.5F);
        HalloweenChat.sendActionBar(player, HalloweenChat.color("<#5A4E7C>[<#6EEB83>Task<#5A4E7C>] <#6EEB83>ยิงสกัดสิ่งกีดขวางครบ 8 เป้าหมายสำเร็จ"));

        Bukkit.getScheduler().runTaskLater(arena.getPlugin(), () -> {
          if(player.isOnline() && arena.getPlayers().contains(player) && !arena.getVotingManager().isVotingActive()) {
            if(isGui(player.getOpenInventory().getTitle())) {
              player.closeInventory();
            }
            arena.getTaskManager().completeTaskFromMinigame(player, TaskType.CLEAR_ASTEROIDS);
          }
        }, 8L);
      } else {
        HalloweenChat.sendActionBar(player, HalloweenChat.color("<#5A4E7C>[<#FF7518>Task<#5A4E7C>] <#F4F1DE>ทำลายเป้าหมาย <#6EEB83>" + session.hits + "/" + TARGET_HITS));
      }
    }
    return true;
  }

  @Override
  public void handleClose(Player player) {
    sessions.remove(player.getUniqueId());
    completedSessions.remove(player.getUniqueId());
  }

  @Override
  public void cleanup() {
    sessions.clear();
    completedSessions.clear();
  }
}
