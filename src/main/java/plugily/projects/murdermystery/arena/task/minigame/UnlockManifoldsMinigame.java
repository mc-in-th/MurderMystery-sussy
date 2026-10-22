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

public class UnlockManifoldsMinigame implements TaskMinigame {

  public static final String GUI_TITLE = HalloweenChat.color("<#5A4E7C>[<#FF7518>ปลดล็อกท่อแรงดัน<#5A4E7C>]");
  private static final int[] MANIFOLD_SLOTS = {
    11, 12, 13, 14, 15,
    20, 21, 22, 23, 24
  };

  private static class ManifoldSession {
    final List<Integer> numbers = new ArrayList<>();
    int nextExpected = 1;

    ManifoldSession() {
      for(int i = 1; i <= 10; i++) {
        numbers.add(i);
      }
      Collections.shuffle(numbers);
    }
  }

  private final Arena arena;
  private final Map<UUID, ManifoldSession> sessions = new HashMap<>();
  private final Set<UUID> completedSessions = new HashSet<>();

  public UnlockManifoldsMinigame(Arena arena) {
    this.arena = arena;
  }

  @Override
  public TaskType getTaskType() {
    return TaskType.UNLOCK_MANIFOLDS;
  }

  @Override
  public String getGuiTitle() {
    return GUI_TITLE;
  }

  @Override
  public boolean isGui(String title) {
    return title != null && HalloweenChat.strip(title).contains("ปลดล็อกท่อแรงดัน");
  }

  @Override
  public void open(Player player) {
    completedSessions.remove(player.getUniqueId());
    ManifoldSession session = new ManifoldSession();
    sessions.put(player.getUniqueId(), session);

    Inventory inv = Bukkit.createInventory(null, 36, GUI_TITLE);
    buildGui(inv, session);
    player.openInventory(inv);

    XSound.BLOCK_IRON_TRAPDOOR_OPEN.play(player.getLocation(), 0.8F, 1.2F);
    HalloweenChat.sendActionBar(player, HalloweenChat.color("<#5A4E7C>[<#FF7518>Task<#5A4E7C>] <#F4F1DE>กดตัวเลขเรียงลำดับจาก <#FFB703>1 <#F4F1DE>ถึง <#FFB703>10"));
  }

  private void buildGui(Inventory inv, ManifoldSession session) {
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
    for(int i = 0; i < 36; i++) {
      inv.setItem(i, bg);
    }

    ItemStack info = XMaterial.COMPARATOR.parseItem();
    ItemMeta iMeta = info.getItemMeta();
    if(iMeta != null) {
      iMeta.setDisplayName(HalloweenChat.color("<#5A4E7C>[<#FF7518>Unlock Manifolds<#5A4E7C>]"));
      iMeta.setLore(HalloweenChat.colorLines(
        "",
        "<#F4F1DE>หมายเลขถัดไป <#FFB703>" + Math.min(10, session.nextExpected),
        "",
        "<#5A4E7C>(<#B8B0C8>กดเรียงลำดับจาก 1 ถึง 10<#5A4E7C>)"
      ));
      info.setItemMeta(iMeta);
    }
    inv.setItem(4, info);

    for(int i = 0; i < MANIFOLD_SLOTS.length; i++) {
      int num = session.numbers.get(i);
      boolean pressed = num < session.nextExpected;
      ItemStack btn = (pressed ? XMaterial.LIME_STAINED_GLASS_PANE : XMaterial.LIGHT_BLUE_STAINED_GLASS_PANE).parseItem();
      btn.setAmount(num);
      ItemMeta bMeta = btn.getItemMeta();
      if(bMeta != null) {
        if(pressed) {
          bMeta.setDisplayName(HalloweenChat.color("<#5A4E7C>[<#6EEB83>หมายเลข " + num + "<#5A4E7C>]"));
          bMeta.setLore(HalloweenChat.colorLines("", "<#6EEB83>ปลดล็อกแล้ว"));
        } else {
          bMeta.setDisplayName(HalloweenChat.color("<#5A4E7C>[<#FFB703>หมายเลข " + num + "<#5A4E7C>]"));
          bMeta.setLore(HalloweenChat.colorLines("", "<#5A4E7C>(<#B8B0C8>กดคลิกเพื่อปลดล็อก<#5A4E7C>)"));
        }
        btn.setItemMeta(bMeta);
      }
      inv.setItem(MANIFOLD_SLOTS[i], btn);
    }
  }

  @Override
  public boolean handleClick(Player player, int rawSlot, ClickType clickType) {
    if(completedSessions.contains(player.getUniqueId())) {
      return true;
    }
    ManifoldSession session = sessions.get(player.getUniqueId());
    if(session == null) {
      return true;
    }

    int idx = -1;
    for(int i = 0; i < MANIFOLD_SLOTS.length; i++) {
      if(MANIFOLD_SLOTS[i] == rawSlot) {
        idx = i;
        break;
      }
    }
    if(idx == -1) {
      return true;
    }

    int clickedNum = session.numbers.get(idx);
    if(clickedNum < session.nextExpected) {
      return true;
    }

    Inventory topInv = player.getOpenInventory().getTopInventory();
    if(clickedNum == session.nextExpected) {
      session.nextExpected++;
      XSound.BLOCK_NOTE_BLOCK_PLING.play(player.getLocation(), 0.8F, 0.9F + (clickedNum * 0.08F));
      buildGui(topInv, session);

      if(session.nextExpected > 10) {
        completedSessions.add(player.getUniqueId());
        sessions.remove(player.getUniqueId());
        XSound.ENTITY_PLAYER_LEVELUP.play(player.getLocation(), 0.9F, 1.5F);
        Bukkit.getScheduler().runTaskLater(arena.getPlugin(), () -> {
          if(player.isOnline()) {
            player.closeInventory();
            arena.getTaskManager().completeTaskFromMinigame(player, TaskType.UNLOCK_MANIFOLDS);
          }
        }, 8L);
      }
    } else {
      session.nextExpected = 1;
      XSound.BLOCK_NOTE_BLOCK_BASS.play(player.getLocation(), 0.9F, 0.6F);
      HalloweenChat.sendActionBar(player, HalloweenChat.color("<#5A4E7C>[<#FF3B47>Task<#5A4E7C>] <#FF3B47>กดผิดลำดับ เริ่มต้นใหม่ที่หมายเลข 1"));
      buildGui(topInv, session);
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

