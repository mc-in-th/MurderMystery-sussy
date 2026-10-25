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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class EmptyGarbageMinigame implements TaskMinigame {

  public static final String GUI_TITLE = HalloweenChat.color("<#5A4E7C>[<#FF7518>ทิ้งขยะออกนอกรถบัส<#5A4E7C>]");
  private static final int TOTAL_PULLS = 5;
  private static final int LEVER_SLOT = 16;
  private static final int[][] TRASH_ROWS = {
    {10, 11, 12, 13},
    {19, 20, 21, 22},
    {28, 29, 30, 31},
    {37, 38, 39, 40}
  };

  private final Arena arena;
  private final Map<UUID, Integer> pullCounts = new HashMap<>();
  private final Set<UUID> completedSessions = new HashSet<>();

  public EmptyGarbageMinigame(Arena arena) {
    this.arena = arena;
  }

  @Override
  public TaskType getTaskType() {
    return TaskType.EMPTY_GARBAGE;
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
    return plain.contains("ทิ้งขยะออกนอกรถบัส") || plain.contains("Trash");
  }

  @Override
  public void open(Player player) {
    completedSessions.remove(player.getUniqueId());
    pullCounts.put(player.getUniqueId(), 0);

    Inventory inv = Bukkit.createInventory(null, 45, GUI_TITLE);
    buildGui(inv, 0);
    player.openInventory(inv);

    XSound.BLOCK_CHEST_OPEN.play(player.getLocation(), 0.8F, 1.2F);
    HalloweenChat.sendActionBar(player, HalloweenChat.color("<#5A4E7C>[<#FF7518>Task<#5A4E7C>] <#F4F1DE>คลิกดึงคันโยกทางขวา 5 ครั้งเพื่อระบายขยะออกนอกตัวรถ"));
  }

  private void buildGui(Inventory inv, int pulls) {
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
      iMeta.setDisplayName(HalloweenChat.color("<#5A4E7C>[<#72EFDD>Empty Garbage<#5A4E7C>]"));
      iMeta.setLore(HalloweenChat.colorLines(
        "",
        "<#F4F1DE>1. คลิกดึงคันโยกปล่องทิ้งขยะทางขวาทั้งหมด 5 ครั้ง",
        "<#F4F1DE>2. ขยะในปล่องจะถูกระบายออกนอกตัวรถทีละชั้น",
        "",
        "<#5A4E7C>(<#72EFDD>Visual Task ยืนยันตัวตนต่อผู้เล่นรอบข้าง<#5A4E7C>)"
      ));
      instruction.setItemMeta(iMeta);
    }
    inv.setItem(0, instruction);

    XMaterial[] trashMaterials = {
      XMaterial.ROTTEN_FLESH,
      XMaterial.BONE,
      XMaterial.COBWEB,
      XMaterial.DRIED_KELP
    };

    for(int r = 0; r < TRASH_ROWS.length; r++) {
      boolean flushed = r < pulls;
      for(int c = 0; c < TRASH_ROWS[r].length; c++) {
        ItemStack cell;
        if(flushed) {
          cell = XMaterial.LIME_STAINED_GLASS_PANE.parseItem();
          ItemMeta cMeta = cell.getItemMeta();
          if(cMeta != null) {
            cMeta.setDisplayName(HalloweenChat.color("<#5A4E7C>[<#6EEB83>ระบายขยะชั้นนี้แล้ว<#5A4E7C>]"));
            cell.setItemMeta(cMeta);
          }
        } else {
          cell = trashMaterials[(r + c) % trashMaterials.length].parseItem();
          ItemMeta cMeta = cell.getItemMeta();
          if(cMeta != null) {
            cMeta.setDisplayName(HalloweenChat.color("<#5A4E7C>[<#FFB703>ขยะค้างท่อ<#5A4E7C>]"));
            cMeta.setLore(HalloweenChat.colorLines("", "<#5A4E7C>(<#B8B0C8>ดึงคันโยกทางขวาเพื่อระบายออก<#5A4E7C>)"));
            cell.setItemMeta(cMeta);
          }
        }
        inv.setItem(TRASH_ROWS[r][c], cell);
      }
    }

    ItemStack lever = (pulls >= TOTAL_PULLS ? XMaterial.EMERALD_BLOCK : XMaterial.LEVER).parseItem();
    ItemMeta lMeta = lever.getItemMeta();
    if(lMeta != null) {
      if(pulls >= TOTAL_PULLS) {
        lMeta.setDisplayName(HalloweenChat.color("<#5A4E7C>[<#6EEB83>ระบายขยะออกหมดแล้ว<#5A4E7C>]"));
        lMeta.setLore(HalloweenChat.colorLines("", "<#6EEB83>ปล่องขยะสะอาดเรียบร้อย"));
      } else {
        lMeta.setDisplayName(HalloweenChat.color("<#5A4E7C>[<#FF7518>คันโยกระบายขยะ<#5A4E7C>]"));
        lMeta.setLore(HalloweenChat.colorLines(
          "",
          "<#F4F1DE>ความคืบหน้า <#FFB703>" + pulls + "<#5A4E7C>/<#F4F1DE>" + TOTAL_PULLS,
          "",
          "<#5A4E7C>(<#B8B0C8>กดคลิกเพื่อเปิดประตูระบายขยะ<#5A4E7C>)"
        ));
      }
      lever.setItemMeta(lMeta);
    }
    inv.setItem(LEVER_SLOT, lever);
  }

  @Override
  public boolean handleClick(Player player, int rawSlot, ClickType clickType) {
    if(completedSessions.contains(player.getUniqueId())) {
      return true;
    }
    if(!pullCounts.containsKey(player.getUniqueId())) {
      return true;
    }
    if(rawSlot == 0) {
      XSound.UI_BUTTON_CLICK.play(player.getLocation(), 0.5F, 1.2F);
      return true;
    }
    if(rawSlot != LEVER_SLOT) {
      return true;
    }

    int pulls = pullCounts.getOrDefault(player.getUniqueId(), 0) + 1;
    pullCounts.put(player.getUniqueId(), pulls);

    arena.getTaskManager().getVisualEffectManager().triggerStepEffect(player, TaskType.EMPTY_GARBAGE, pulls, TOTAL_PULLS);
    buildGui(player.getOpenInventory().getTopInventory(), pulls);

    if(pulls >= TOTAL_PULLS) {
      completedSessions.add(player.getUniqueId());
      pullCounts.remove(player.getUniqueId());
      XSound.ENTITY_PLAYER_LEVELUP.play(player.getLocation(), 1.0F, 1.5F);
      HalloweenChat.sendActionBar(player, HalloweenChat.color("<#5A4E7C>[<#6EEB83>Task<#5A4E7C>] <#6EEB83>ทิ้งขยะออกนอกรถบัสสำเร็จ"));

      Bukkit.getScheduler().runTaskLater(arena.getPlugin(), () -> {
        if(player.isOnline() && arena.getPlayers().contains(player) && !arena.getVotingManager().isVotingActive()) {
          if(isGui(player.getOpenInventory().getTitle())) {
            player.closeInventory();
          }
          arena.getTaskManager().completeTaskFromMinigame(player, TaskType.EMPTY_GARBAGE);
        }
      }, 8L);
    } else {
      HalloweenChat.sendActionBar(player, HalloweenChat.color("<#5A4E7C>[<#FF7518>Task<#5A4E7C>] <#F4F1DE>ดึงคันโยกระบายขยะ <#FFB703>" + pulls + "/" + TOTAL_PULLS));
    }

    return true;
  }

  @Override
  public void handleClose(Player player) {
    pullCounts.remove(player.getUniqueId());
    completedSessions.remove(player.getUniqueId());
  }

  @Override
  public void cleanup() {
    pullCounts.clear();
    completedSessions.clear();
  }
}
