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
import org.bukkit.scheduler.BukkitTask;
import plugily.projects.minigamesbox.classic.utils.version.VersionUtils;
import plugily.projects.minigamesbox.classic.utils.version.xseries.XMaterial;
import plugily.projects.minigamesbox.classic.utils.version.xseries.XSound;
import plugily.projects.murdermystery.arena.Arena;
import plugily.projects.murdermystery.arena.task.TaskType;
import plugily.projects.murdermystery.utils.HalloweenChat;

public class SubmitScanMinigame implements TaskMinigame {

  public static final String GUI_TITLE = HalloweenChat.color("<#5A4E7C>[ <gradient:#00B4D8:#72EFDD><bold>สแกนร่างกายยืนยันตัวตน (Scan)</bold></gradient> <#5A4E7C>]");
  private static final int TOTAL_STEPS = 9;
  private static final int[] PROGRESS_SLOTS = {18, 19, 20, 21, 22, 23, 24, 25, 26};

  private static class ScanSession {
    int step = 0;
    boolean scanning = false;
    BukkitTask scanTask;
  }

  private final Arena arena;
  private final Map<UUID, ScanSession> sessions = new HashMap<>();
  private final Set<UUID> completedSessions = new HashSet<>();

  public SubmitScanMinigame(Arena arena) {
    this.arena = arena;
  }

  @Override
  public TaskType getTaskType() {
    return TaskType.SUBMIT_SCAN;
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
    return plain.contains("สแกนร่างกายยืนยันตัวตน") || plain.contains("Scan");
  }

  @Override
  public void open(Player player) {
    stopSessionTask(player.getUniqueId());
    completedSessions.remove(player.getUniqueId());

    ScanSession session = new ScanSession();
    sessions.put(player.getUniqueId(), session);

    Inventory inv = Bukkit.createInventory(null, 27, GUI_TITLE);
    buildGui(inv, player, session);
    player.openInventory(inv);

    XSound.BLOCK_BEACON_ACTIVATE.play(player.getLocation(), 0.8F, 1.4F);
    HalloweenChat.sendActionBar(player, HalloweenChat.color("<gradient:#00B4D8:#72EFDD><bold>[สแกนร่างกาย]</bold></gradient> <#FFB703>คลิกปุ่มตรงกลางเพื่อเริ่มสแกนยืนยันตัวตน (คนอื่นจะเห็นแสงสแกนรอบตัวคุณ!)"));
  }

  private void buildGui(Inventory inv, Player player, ScanSession session) {
    ItemStack bg = XMaterial.PURPLE_STAINED_GLASS_PANE.parseItem();
    ItemMeta bgMeta = bg.getItemMeta();
    if(bgMeta != null) {
      bgMeta.setDisplayName(HalloweenChat.color("<#5A4E7C> "));
      bg.setItemMeta(bgMeta);
    }
    for(int i = 0; i < 27; i++) {
      inv.setItem(i, bg);
    }

    ItemStack instruction = new ItemStack(Material.BOOK);
    ItemMeta iMeta = instruction.getItemMeta();
    if(iMeta != null) {
      iMeta.setDisplayName(HalloweenChat.color("<gradient:#00B4D8:#72EFDD><bold>วิธีทำภารกิจ: สแกนร่างกาย (Visual Task)</bold></gradient>"));
      iMeta.setLore(HalloweenChat.colorLines(
        "<#72EFDD>[!] นี่คือภารกิจยืนยันตัวตน (Visual Task)!",
        "<#B8B0C8>1. คลิกที่ปุ่มเครื่องสแกนตรงกลาง (ช่อง 13)",
        "<#B8B0C8>2. ยืนรอให้เครื่องสแกนตรวจสอบข้อมูลครบ 100%",
        "<#6EEB83>3. ขณะสแกน ผู้เล่นคนอื่นที่อยู่ใกล้ๆ จะเห็นวงแหวนแสงสีเขียว",
        "<#6EEB83>   รอบตัวคุณ ซึ่งช่วยพิสูจน์ว่าคุณเป็นผู้บริสุทธิ์!"
      ));
      instruction.setItemMeta(iMeta);
    }
    inv.setItem(0, instruction);

    int percent = (int) Math.round(((double) session.step / TOTAL_STEPS) * 100.0);
    ItemStack scanner = session.step >= TOTAL_STEPS
      ? XMaterial.EMERALD_BLOCK.parseItem()
      : (session.scanning ? XMaterial.BEACON.parseItem() : XMaterial.SEA_LANTERN.parseItem());
    ItemMeta sMeta = scanner.getItemMeta();
    if(sMeta != null) {
      if(session.step >= TOTAL_STEPS) {
        sMeta.setDisplayName(HalloweenChat.color("<gradient:#38B000:#9EF01A><bold>[/] สแกนร่างกายสำเร็จ 100% (Verified Innocent)</bold></gradient>"));
      } else if(session.scanning) {
        sMeta.setDisplayName(HalloweenChat.color("<gradient:#00B4D8:#72EFDD><bold>กำลังสแกนร่างกาย... (" + percent + "%)</bold></gradient>"));
      } else {
        sMeta.setDisplayName(HalloweenChat.color("<#5A4E7C>[ <gradient:#00B4D8:#72EFDD><bold>คลิกเพื่อเริ่มสแกนร่างกาย (Start MedBay Scan)</bold></gradient> <#5A4E7C>]"));
      }
      sMeta.setLore(HalloweenChat.colorLines(
        "<#F4F1DE>ชื่อผู้โดยสาร: <#6EEB83>" + player.getName(),
        "<#F4F1DE>รหัสประจำตัว: <#FFB703>#AMOG-" + Math.abs(player.getUniqueId().hashCode() % 9000 + 1000),
        "<#F4F1DE>ส่วนสูง / น้ำหนัก: <#72EFDD>106 cm / 42 kg",
        "<#F4F1DE>สถานะชีวภาพ: " + (session.step >= TOTAL_STEPS ? "<#6EEB83>ยืนยันผู้บริสุทธิ์ [/]" : "<#FFB703>กำลังตรวจสอบข้อมูล..."),
        " ",
        session.scanning ? "<#72EFDD>* กรุณาเปิดหน้าต่างนี้ค้างไว้จนกว่าจะครบ 100%" : "<#6EEB83>> คลิกที่นี่เพื่อเริ่มเดินเครื่องสแกน!"
      ));
      scanner.setItemMeta(sMeta);
    }
    inv.setItem(13, scanner);

    for(int i = 0; i < PROGRESS_SLOTS.length; i++) {
      boolean done = i < session.step;
      ItemStack pane = done ? XMaterial.LIME_STAINED_GLASS_PANE.parseItem() : XMaterial.CYAN_STAINED_GLASS_PANE.parseItem();
      ItemMeta pMeta = pane.getItemMeta();
      if(pMeta != null) {
        pMeta.setDisplayName(HalloweenChat.color(done
          ? "<#6EEB83>■ สแกนส่วนที่ " + (i + 1) + "/" + TOTAL_STEPS + " สำเร็จ"
          : "<#72EFDD>□ รอการสแกนส่วนที่ " + (i + 1) + "/" + TOTAL_STEPS));
        pane.setItemMeta(pMeta);
      }
      inv.setItem(PROGRESS_SLOTS[i], pane);
    }
  }

  @Override
  public boolean handleClick(Player player, int rawSlot, ClickType clickType) {
    if(completedSessions.contains(player.getUniqueId())) {
      return true;
    }
    ScanSession session = sessions.get(player.getUniqueId());
    if(session == null) {
      return true;
    }

    if(rawSlot == 13 && !session.scanning) {
      session.scanning = true;
      XSound.BLOCK_BEACON_POWER_SELECT.play(player.getLocation(), 0.9F, 1.4F);
      buildGui(player.getOpenInventory().getTopInventory(), player, session);

      session.scanTask = Bukkit.getScheduler().runTaskTimer(arena.getPlugin(), () -> {
        if(!player.isOnline()) {
          stopSessionTask(player.getUniqueId());
          return;
        }
        session.step++;
        arena.getTaskManager().getVisualEffectManager().triggerStepEffect(player, TaskType.SUBMIT_SCAN, session.step, TOTAL_STEPS);
        buildGui(player.getOpenInventory().getTopInventory(), player, session);

        if(session.step >= TOTAL_STEPS) {
          stopSessionTask(player.getUniqueId());
          completedSessions.add(player.getUniqueId());
          sessions.remove(player.getUniqueId());
          XSound.ENTITY_PLAYER_LEVELUP.play(player.getLocation(), 1.0F, 1.5F);
          HalloweenChat.sendActionBar(player, HalloweenChat.color("<gradient:#38B000:#9EF01A><bold>[/] สแกนร่างกายยืนยันตัวตนสำเร็จ!</bold></gradient>"));

          Bukkit.getScheduler().runTaskLater(arena.getPlugin(), () -> {
            if(player.isOnline()) {
              player.closeInventory();
              arena.getTaskManager().completeTaskFromMinigame(player, TaskType.SUBMIT_SCAN);
            }
          }, 10L);
        }
      }, 10L, 10L);
    }
    return true;
  }

  private void stopSessionTask(UUID playerId) {
    ScanSession session = sessions.get(playerId);
    if(session != null && session.scanTask != null) {
      session.scanTask.cancel();
      session.scanTask = null;
    }
  }

  @Override
  public void handleClose(Player player) {
    stopSessionTask(player.getUniqueId());
    sessions.remove(player.getUniqueId());
    completedSessions.remove(player.getUniqueId());
  }

  @Override
  public void cleanup() {
    for(UUID id : new HashSet<>(sessions.keySet())) {
      stopSessionTask(id);
    }
    sessions.clear();
    completedSessions.clear();
  }
}
