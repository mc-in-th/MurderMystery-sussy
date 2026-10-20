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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import plugily.projects.minigamesbox.classic.utils.version.VersionUtils;
import plugily.projects.minigamesbox.classic.utils.version.xseries.XMaterial;
import plugily.projects.minigamesbox.classic.utils.version.xseries.XSound;
import plugily.projects.murdermystery.arena.Arena;
import plugily.projects.murdermystery.arena.task.TaskType;
import plugily.projects.murdermystery.utils.HalloweenChat;

public class EmptyGarbageMinigame implements TaskMinigame {

  public static final String GUI_TITLE = HalloweenChat.color("<#5A4E7C>[ <gradient:#38B000:#9EF01A><bold>ทิ้งขยะออกนอกรถบัส (Trash)</bold></gradient> <#5A4E7C>]");
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
    VersionUtils.sendActionBar(player, HalloweenChat.color("<gradient:#38B000:#9EF01A><bold>[ทิ้งขยะ]</bold></gradient> <#FFB703>คลิกดึงคันโยกทางขวา 5 ครั้งเพื่อระบายขยะออกนอกตัวรถ!"));
  }

  private void buildGui(Inventory inv, int pulls) {
    ItemStack bg = XMaterial.PURPLE_STAINED_GLASS_PANE.parseItem();
    ItemMeta bgMeta = bg.getItemMeta();
    if(bgMeta != null) {
      bgMeta.setDisplayName(HalloweenChat.color("<#5A4E7C> "));
      bg.setItemMeta(bgMeta);
    }
    for(int i = 0; i < 45; i++) {
      inv.setItem(i, bg);
    }

    ItemStack instruction = new ItemStack(Material.BOOK);
    ItemMeta iMeta = instruction.getItemMeta();
    if(iMeta != null) {
      iMeta.setDisplayName(HalloweenChat.color("<gradient:#38B000:#9EF01A><bold>วิธีทำภารกิจ: ทิ้งขยะออกนอกรถบัส (Visual Task)</bold></gradient>"));
      iMeta.setLore(HalloweenChat.colorLines(
        "<#72EFDD>[!] นี่คือภารกิจยืนยันตัวตน (Visual Task)!",
        "<#B8B0C8>1. คลิกดึงคันโยกปล่องทิ้งขยะทางขวา (ช่อง 16) ทั้งหมด 5 ครั้ง",
        "<#B8B0C8>2. ขยะในปล่องจะถูกระบายออกนอกตัวรถทีละชั้น",
        "<#6EEB83>3. ผู้เล่นคนอื่นที่ยืนดูอยู่จะเห็นขยะถูกปล่อยออกมาจริงๆ!"
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
            cMeta.setDisplayName(HalloweenChat.color("<#6EEB83>[/] ระบายขยะชั้นนี้แล้ว"));
            cell.setItemMeta(cMeta);
          }
        } else {
          cell = trashMaterials[(r + c) % trashMaterials.length].parseItem();
          ItemMeta cMeta = cell.getItemMeta();
          if(cMeta != null) {
            cMeta.setDisplayName(HalloweenChat.color("<#FF7518>ขยะค้างท่อ (ดึงคันโยกทางขวาเพื่อทิ้ง)"));
            cell.setItemMeta(cMeta);
          }
        }
        inv.setItem(TRASH_ROWS[r][c], cell);
      }
    }

    ItemStack lever = pulls >= TOTAL_PULLS ? XMaterial.EMERALD_BLOCK.parseItem() : XMaterial.LEVER.parseItem();
    ItemMeta lMeta = lever.getItemMeta();
    if(lMeta != null) {
      lMeta.setDisplayName(HalloweenChat.color(pulls >= TOTAL_PULLS
        ? "<gradient:#38B000:#9EF01A><bold>[/] ระบายขยะออกหมดแล้ว!</bold></gradient>"
        : "<#5A4E7C>[ <gradient:#FF5400:#FFB703><bold>คลิกดึงคันโยกทิ้งขยะ (" + pulls + "/" + TOTAL_PULLS + ")</bold></gradient> <#5A4E7C>]"));
      lMeta.setLore(HalloweenChat.colorLines(
        "<#B8B0C8>สถานะปล่องขยะ: <#6EEB83>" + pulls + " / " + TOTAL_PULLS,
        pulls >= TOTAL_PULLS ? "<#6EEB83>[/] สะอาดเรียบร้อย!" : "<#FFB703>> คลิกย้ำๆ เพื่อเปิดประตูระบายขยะ!"
      ));
      lever.setItemMeta(lMeta);
    }
    inv.setItem(LEVER_SLOT, lever);
  }

  @Override
  public boolean handleClick(Player player, int rawSlot, ClickType clickType) {
    if(completedSessions.contains(player.getUniqueId())) {
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
      VersionUtils.sendActionBar(player, HalloweenChat.color("<gradient:#38B000:#9EF01A><bold>[/] ทิ้งขยะออกนอกรถบัสเรียบร้อยแล้ว!</bold></gradient>"));

      Bukkit.getScheduler().runTaskLater(arena.getPlugin(), () -> {
        if(player.isOnline()) {
          player.closeInventory();
          arena.getTaskManager().completeTaskFromMinigame(player, TaskType.EMPTY_GARBAGE);
        }
      }, 10L);
    } else {
      VersionUtils.sendActionBar(player, HalloweenChat.color("<#6EEB83>[กำลังระบายขยะ] <#FFB703>ดึงคันโยกแล้ว " + pulls + "/" + TOTAL_PULLS + " ครั้ง!"));
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
