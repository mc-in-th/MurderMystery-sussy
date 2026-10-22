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

public class CleanAirFilterMinigame implements TaskMinigame {

  public static final String GUI_TITLE = HalloweenChat.color("<#5A4E7C>[<#FF7518>ทำความสะอาดไส้กรองอากาศ<#5A4E7C>]");
  private static final int TOTAL_LEAVES = 6;
  private static final int[] FILTER_SLOTS = {
    11, 12, 13, 14, 15, 16,
    20, 21, 22, 23, 24, 25,
    29, 30, 31, 32, 33, 34
  };
  private static final int CHUTE_SLOT = 19;

  private static class FilterSession {
    final Set<Integer> leafSlots = new HashSet<>();

    FilterSession() {
      List<Integer> pool = new ArrayList<>();
      for(int slot : FILTER_SLOTS) {
        pool.add(slot);
      }
      Collections.shuffle(pool);
      for(int i = 0; i < TOTAL_LEAVES && i < pool.size(); i++) {
        leafSlots.add(pool.get(i));
      }
    }
  }

  private final Arena arena;
  private final Map<UUID, FilterSession> sessions = new HashMap<>();
  private final Set<UUID> completedSessions = new HashSet<>();

  public CleanAirFilterMinigame(Arena arena) {
    this.arena = arena;
  }

  @Override
  public TaskType getTaskType() {
    return TaskType.CLEAN_AIR_FILTER;
  }

  @Override
  public String getGuiTitle() {
    return GUI_TITLE;
  }

  @Override
  public boolean isGui(String title) {
    return title != null && HalloweenChat.strip(title).contains("ทำความสะอาดไส้กรองอากาศ");
  }

  @Override
  public void open(Player player) {
    completedSessions.remove(player.getUniqueId());
    FilterSession session = new FilterSession();
    sessions.put(player.getUniqueId(), session);

    Inventory inv = Bukkit.createInventory(null, 45, GUI_TITLE);
    buildGui(inv, session);
    player.openInventory(inv);

    XSound.BLOCK_CHEST_OPEN.play(player.getLocation(), 0.8F, 1.2F);
    HalloweenChat.sendActionBar(player, HalloweenChat.color("<#5A4E7C>[<#FF7518>Task<#5A4E7C>] <#F4F1DE>คลิกหยิบใบไม้ที่อุดตันไส้กรองออกให้หมด"));
  }

  private void buildGui(Inventory inv, FilterSession session) {
    inv.clear();

    ItemStack border = XMaterial.BLACK_STAINED_GLASS_PANE.parseItem();
    ItemMeta bMeta = border.getItemMeta();
    if(bMeta != null) {
      bMeta.setDisplayName(" ");
      try {
        bMeta.setHideTooltip(true);
      } catch(Throwable ignored) {
      }
      border.setItemMeta(bMeta);
    }

    for(int i = 0; i < 45; i++) {
      if(i < 9 || i >= 36 || i % 9 == 0 || i % 9 == 8 || i % 9 == 1) {
        inv.setItem(i, border);
      }
    }

    int remaining = session.leafSlots.size();
    ItemStack chute = new ItemStack(Material.HOPPER);
    ItemMeta cMeta = chute.getItemMeta();
    if(cMeta != null) {
      cMeta.setDisplayName(HalloweenChat.color("<#5A4E7C>[<#FF7518>Air Filter<#5A4E7C>]"));
      cMeta.setLore(HalloweenChat.colorLines(
        "",
        "<#F4F1DE>สิ่งอุดตันที่เหลือ <#FFB703>" + remaining + "<#5A4E7C>/<#F4F1DE>" + TOTAL_LEAVES,
        "",
        "<#5A4E7C>(<#B8B0C8>คลิกใบไม้ในช่องกรองเพื่อดูดทิ้ง<#5A4E7C>)"
      ));
      chute.setItemMeta(cMeta);
    }
    inv.setItem(CHUTE_SLOT, chute);

    XMaterial[] leafMats = {XMaterial.OAK_LEAVES, XMaterial.BIRCH_LEAVES, XMaterial.VINE};
    for(int slot : session.leafSlots) {
      ItemStack leaf = leafMats[slot % leafMats.length].parseItem();
      ItemMeta lMeta = leaf.getItemMeta();
      if(lMeta != null) {
        lMeta.setDisplayName(HalloweenChat.color("<#5A4E7C>[<#FFB703>เศษใบไม้อุดตัน<#5A4E7C>]"));
        lMeta.setLore(HalloweenChat.colorLines("", "<#5A4E7C>(<#B8B0C8>กดคลิกเพื่อกำจัดออก<#5A4E7C>)"));
        leaf.setItemMeta(lMeta);
      }
      inv.setItem(slot, leaf);
    }
  }

  @Override
  public boolean handleClick(Player player, int rawSlot, ClickType clickType) {
    if(completedSessions.contains(player.getUniqueId())) {
      return true;
    }
    FilterSession session = sessions.get(player.getUniqueId());
    if(session == null || !session.leafSlots.contains(rawSlot)) {
      return true;
    }

    session.leafSlots.remove(rawSlot);
    XSound.BLOCK_COMPOSTER_FILL.play(player.getLocation(), 0.9F, 1.3F);

    Inventory topInv = player.getOpenInventory().getTopInventory();
    buildGui(topInv, session);

    if(session.leafSlots.isEmpty()) {
      completedSessions.add(player.getUniqueId());
      sessions.remove(player.getUniqueId());
      XSound.ENTITY_PLAYER_LEVELUP.play(player.getLocation(), 0.9F, 1.5F);
      Bukkit.getScheduler().runTaskLater(arena.getPlugin(), () -> {
        if(player.isOnline()) {
          player.closeInventory();
          arena.getTaskManager().completeTaskFromMinigame(player, TaskType.CLEAN_AIR_FILTER);
        }
      }, 8L);
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

