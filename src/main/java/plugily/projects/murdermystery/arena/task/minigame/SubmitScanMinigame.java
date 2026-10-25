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
import org.bukkit.scheduler.BukkitTask;
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

public class SubmitScanMinigame implements TaskMinigame {

  public static final String GUI_TITLE = HalloweenChat.color("<#5A4E7C>[<#FF7518>สแกนร่างกายยืนยันตัวตน<#5A4E7C>]");
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
    HalloweenChat.sendActionBar(player, HalloweenChat.color("<#5A4E7C>[<#FF7518>Task<#5A4E7C>] <#F4F1DE>คลิกปุ่มตรงกลางเพื่อเริ่มสแกนร่างกายยืนยันตัวตน"));
  }

  private void buildGui(Inventory inv, Player player, ScanSession session) {
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
    for(int i = 0; i < 27; i++) {
      inv.setItem(i, bg);
    }

    ItemStack instruction = new ItemStack(Material.BOOK);
    ItemMeta iMeta = instruction.getItemMeta();
    if(iMeta != null) {
      iMeta.setDisplayName(HalloweenChat.color("<#5A4E7C>[<#72EFDD>MedBay Scan<#5A4E7C>]"));
      iMeta.setLore(HalloweenChat.colorLines(
        "",
        "<#F4F1DE>1. คลิกปุ่มเครื่องสแกนตรงกลางเพื่อเดินเครื่อง",
        "<#F4F1DE>2. ยืนรอให้เครื่องสแกนตรวจสอบข้อมูลครบ 100%",
        "",
        "<#5A4E7C>(<#72EFDD>Visual Task ยืนยันตัวตนต่อผู้เล่นรอบข้าง<#5A4E7C>)"
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
        sMeta.setDisplayName(HalloweenChat.color("<#5A4E7C>[<#6EEB83>สแกนร่างกายสำเร็จ 100%<#5A4E7C>]"));
      } else if(session.scanning) {
        sMeta.setDisplayName(HalloweenChat.color("<#5A4E7C>[<#72EFDD>กำลังสแกนร่างกาย " + percent + "%<#5A4E7C>]"));
      } else {
        sMeta.setDisplayName(HalloweenChat.color("<#5A4E7C>[<#FF7518>เริ่มสแกนร่างกายยืนยันตัวตน<#5A4E7C>]"));
      }
      sMeta.setLore(HalloweenChat.colorLines(
        "",
        "<#F4F1DE>ผู้โดยสาร <#6EEB83>" + player.getName(),
        "<#F4F1DE>รหัสประจำตัว <#FFB703>#BUS-" + Math.abs(player.getUniqueId().hashCode() % 9000 + 1000),
        "<#F4F1DE>สถานะชีวภาพ " + (session.step >= TOTAL_STEPS ? "<#6EEB83>ยืนยันผู้บริสุทธิ์" : "<#FFB703>กำลังตรวจสอบข้อมูล"),
        "",
        session.scanning
          ? "<#5A4E7C>(<#B8B0C8>กรุณาเปิดหน้าต่างนี้ค้างไว้จนครบ 100%<#5A4E7C>)"
          : "<#5A4E7C>(<#B8B0C8>กดคลิกเพื่อเริ่มเดินเครื่องสแกน<#5A4E7C>)"
      ));
      scanner.setItemMeta(sMeta);
    }
    inv.setItem(13, scanner);

    for(int i = 0; i < PROGRESS_SLOTS.length; i++) {
      boolean done = i < session.step;
      ItemStack pane = (done ? XMaterial.LIME_STAINED_GLASS_PANE : XMaterial.GRAY_STAINED_GLASS_PANE).parseItem();
      ItemMeta pMeta = pane.getItemMeta();
      if(pMeta != null) {
        pMeta.setDisplayName(HalloweenChat.color(done
          ? "<#5A4E7C>[<#6EEB83>สแกนส่วนที่ " + (i + 1) + "/" + TOTAL_STEPS + "<#5A4E7C>]"
          : "<#5A4E7C>[<#B8B0C8>รอสแกนส่วนที่ " + (i + 1) + "/" + TOTAL_STEPS + "<#5A4E7C>]"));
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

    if(rawSlot == 0) {
      XSound.UI_BUTTON_CLICK.play(player.getLocation(), 0.5F, 1.2F);
      return true;
    }

    if(rawSlot == 13 && !session.scanning) {
      session.scanning = true;
      Inventory topInv = player.getOpenInventory().getTopInventory();
      XSound.BLOCK_BEACON_POWER_SELECT.play(player.getLocation(), 0.9F, 1.4F);
      buildGui(topInv, player, session);

      session.scanTask = Bukkit.getScheduler().runTaskTimer(arena.getPlugin(), () -> {
        if(!player.isOnline()
          || !arena.getPlayers().contains(player)
          || arena.getVotingManager().isVotingActive()
          || !sessions.containsKey(player.getUniqueId())
          || !isGui(player.getOpenInventory().getTitle())) {
          stopSessionTask(player.getUniqueId());
          sessions.remove(player.getUniqueId());
          return;
        }
        session.step++;
        arena.getTaskManager().getVisualEffectManager().triggerStepEffect(player, TaskType.SUBMIT_SCAN, session.step, TOTAL_STEPS);
        buildGui(topInv, player, session);

        if(session.step >= TOTAL_STEPS) {
          stopSessionTask(player.getUniqueId());
          completedSessions.add(player.getUniqueId());
          sessions.remove(player.getUniqueId());
          XSound.ENTITY_PLAYER_LEVELUP.play(player.getLocation(), 1.0F, 1.5F);
          HalloweenChat.sendActionBar(player, HalloweenChat.color("<#5A4E7C>[<#6EEB83>Task<#5A4E7C>] <#6EEB83>สแกนร่างกายยืนยันตัวตนสำเร็จ"));

          Bukkit.getScheduler().runTaskLater(arena.getPlugin(), () -> {
            if(player.isOnline() && arena.getPlayers().contains(player) && !arena.getVotingManager().isVotingActive()) {
              if(isGui(player.getOpenInventory().getTitle())) {
                player.closeInventory();
              }
              arena.getTaskManager().completeTaskFromMinigame(player, TaskType.SUBMIT_SCAN);
            }
          }, 8L);
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
