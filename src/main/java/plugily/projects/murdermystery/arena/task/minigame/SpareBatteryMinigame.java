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

public class SpareBatteryMinigame implements TaskMinigame {

  public static final String GUI_TITLE = HalloweenChat.color("<shadow:#000000FF><#5A4E7C>[</shadow><#FF7518>พ่วงชาร์จแบตเตอรี่สำรอง<shadow:#000000FF><#5A4E7C>]</shadow>");

  private static final int STATUS_SLOT = 4;
  private static final int GENERATOR_SLOT = 10;
  private static final int[] PROGRESS_SLOTS = {11, 12, 13, 14, 15};
  private static final int BATTERY_SLOT = 16;
  private static final int START_BUTTON_SLOT = 22;

  private static class BatterySession {
    boolean charging = false;
    int step = 0;
    BukkitTask task;
  }

  private final Arena arena;
  private final Map<UUID, BatterySession> sessions = new HashMap<>();
  private final Set<UUID> completedSessions = new HashSet<>();

  public SpareBatteryMinigame(Arena arena) {
    this.arena = arena;
  }

  @Override
  public TaskType getTaskType() {
    return TaskType.CHARGE_SPARE_BATTERY;
  }

  @Override
  public String getGuiTitle() {
    return GUI_TITLE;
  }

  @Override
  public boolean isGui(String title) {
    return title != null && HalloweenChat.strip(title).contains("พ่วงชาร์จแบตเตอรี่สำรอง");
  }

  @Override
  public void open(Player player) {
    cancelSession(player.getUniqueId());
    completedSessions.remove(player.getUniqueId());

    BatterySession session = new BatterySession();
    sessions.put(player.getUniqueId(), session);

    Inventory inv = Bukkit.createInventory(null, 27, GUI_TITLE);
    buildGui(inv, session);
    player.openInventory(inv);

    XSound.BLOCK_BEACON_AMBIENT.play(player.getLocation(), 0.7F, 1.4F);
    TaskGuiHelper.sendTaskBar(player, "<#F4F1DE>คลิกสวิตช์เรืองแสงด้านล่างเพื่อเริ่มพ่วงชาร์จแบตเตอรี่สำรอง");
  }

  private void buildGui(Inventory inv, BatterySession session) {
    TaskGuiHelper.fillBackground(inv);

    int percent = Math.min(100, session.step * 20);
    boolean finished = session.step >= PROGRESS_SLOTS.length;

    ItemStack status;
    if(finished) {
      status = TaskGuiHelper.createItem(
        Material.EMERALD,
        1,
        true,
        TaskGuiHelper.doneName("ชาร์จแบตเตอรี่สำรองเต็ม 100%"),
        "",
        "<#6EEB83>แบตเตอรี่สำรองพร้อมนำไปพ่วงสตาร์ทรถบัสแล้ว"
      );
    } else if(session.charging) {
      status = TaskGuiHelper.createItem(
        XMaterial.DAYLIGHT_DETECTOR,
        1,
        false,
        TaskGuiHelper.tag("<#FFB703>", "ขั้นที่ 2/2 - กำลังชาร์จไฟ " + percent + "%"),
        "",
        "<#F4F1DE>ความคืบหน้า " + TaskGuiHelper.buildPipeBar(session.step, PROGRESS_SLOTS.length, 10) + " <#FFB703>" + percent + "%",
        "",
        TaskGuiHelper.hint("<#B8B0C8>", "เปิดหน้าจอนี้ค้างไว้จนกว่าจะครบ 100%")
      );
    } else {
      status = TaskGuiHelper.createItem(
        XMaterial.DAYLIGHT_DETECTOR,
        1,
        false,
        TaskGuiHelper.tag("<#FF7518>", "ขั้นที่ 1/2 - เริ่มพ่วงสายแบตเตอรี่สำรอง"),
        "",
        "<#F4F1DE>คลิกปุ่ม <#FFB703>สวิตช์พ่วงไฟ <#F4F1DE>ที่เรืองแสงด้านล่าง",
        "<#F4F1DE>แล้วถือหน้าจอนี้รอจนประจุเต็ม <#6EEB83>100%"
      );
    }
    inv.setItem(STATUS_SLOT, status);

    inv.setItem(GENERATOR_SLOT, TaskGuiHelper.createItem(
      Material.REDSTONE_BLOCK,
      1,
      false,
      TaskGuiHelper.tag("<#FFB703>", "ตู้เครื่องปั่นไฟสำรอง 24V"),
      "",
      "<#F4F1DE>สถานะ " + (session.charging ? "<#6EEB83>กำลังจ่ายกระแสไฟ" : "<#B8B0C8>รอเปิดสวิตช์")
    ));

    for(int i = 0; i < PROGRESS_SLOTS.length; i++) {
      int segPercent = (i + 1) * 20;
      ItemStack pane;
      if(i < session.step) {
        pane = TaskGuiHelper.createItem(
          XMaterial.LIME_STAINED_GLASS_PANE,
          1,
          finished,
          TaskGuiHelper.tag("<#6EEB83>", "ประจุไฟสะสม " + segPercent + "%"),
          "",
          "<#6EEB83>กระแสไฟไหลผ่านเรียบร้อย"
        );
      } else if(session.charging && i == session.step) {
        pane = TaskGuiHelper.createItem(
          XMaterial.YELLOW_STAINED_GLASS_PANE,
          1,
          true,
          TaskGuiHelper.tag("<#FFB703>", "กำลังส่งกระแสไฟ..."),
          "",
          TaskGuiHelper.hint("<#FFB703>", "กำลังชาร์จเข้าสู่ช่องที่ " + (i + 1) + "/5")
        );
      } else {
        pane = TaskGuiHelper.createItem(
          XMaterial.GRAY_STAINED_GLASS_PANE,
          1,
          false,
          TaskGuiHelper.tag("<#B8B0C8>", "สายพ่วงแบตเตอรี่ ช่องที่ " + (i + 1) + "/5"),
          "",
          "<#B8B0C8>ยังไม่มีกระแสไฟไหลผ่าน"
        );
      }
      inv.setItem(PROGRESS_SLOTS[i], pane);
    }

    inv.setItem(BATTERY_SLOT, TaskGuiHelper.createItem(
      finished ? Material.EMERALD_BLOCK : Material.IRON_BLOCK,
      1,
      finished,
      TaskGuiHelper.tag(finished ? "<#6EEB83>" : "<#72EFDD>", "แบตเตอรี่สำรองรถบัส"),
      "",
      "<#F4F1DE>ประจุไฟสะสม " + (finished ? "<#6EEB83>" : "<#FFB703>") + percent + "%"
    ));

    ItemStack startBtn;
    if(finished) {
      startBtn = TaskGuiHelper.createItem(
        XMaterial.EMERALD_BLOCK,
        1,
        true,
        TaskGuiHelper.doneName("พ่วงชาร์จแบตเตอรี่สำรองสำเร็จ"),
        "",
        "<#6EEB83>ประจุไฟเต็ม 100% พร้อมนำไปสตาร์ทรถบัส"
      );
    } else if(session.charging) {
      startBtn = TaskGuiHelper.createItem(
        XMaterial.CLOCK,
        Math.max(1, session.step),
        false,
        TaskGuiHelper.tag("<#6EEB83>", "กำลังชาร์จแบตเตอรี่สำรอง " + percent + "%"),
        "",
        "<#F4F1DE>กำลังส่งกระแสไฟเข้าแบตเตอรี่สำรอง",
        "",
        TaskGuiHelper.hint("<#FFB703>", "ห้ามปิดหน้าจอนี้จนกว่าจะครบ 100%")
      );
    } else {
      startBtn = TaskGuiHelper.createItem(
        XMaterial.LEVER,
        1,
        true,
        TaskGuiHelper.clickName("หนีบสายพ่วงแบตเตอรี่สำรอง"),
        "",
        "<#F4F1DE>เชื่อมต่อตู้ปั่นไฟเข้ากับแบตเตอรี่สำรองรถบัส",
        "",
        TaskGuiHelper.hint("<#FFB703>", "กดคลิกที่นี่เพื่อเริ่มชาร์จไฟ")
      );
    }
    inv.setItem(START_BUTTON_SLOT, startBtn);
  }

  @Override
  public boolean handleClick(Player player, int rawSlot, ClickType clickType) {
    if(completedSessions.contains(player.getUniqueId())) {
      return true;
    }
    BatterySession session = sessions.get(player.getUniqueId());
    if(session == null) {
      return true;
    }

    if(session.charging) {
      if(rawSlot >= 0 && rawSlot < 27) {
        TaskGuiHelper.sendTaskBar(player, "<#F4F1DE>กำลังชาร์จไฟ <#FFB703>" + (session.step * 20) + "% <#B8B0C8>กรุณารอสักครู่");
      }
      return true;
    }

    if(rawSlot == START_BUTTON_SLOT) {
      session.charging = true;
      session.step = 0;
      Inventory topInv = player.getOpenInventory().getTopInventory();
      buildGui(topInv, session);
      XSound.BLOCK_LEVER_CLICK.play(player.getLocation(), 0.9F, 1.3F);
      TaskGuiHelper.sendTaskBar(player, "<#F4F1DE>เริ่มส่งกระแสไฟ " + TaskGuiHelper.buildPipeBar(0, PROGRESS_SLOTS.length, 10) + " <#FFB703>0%");

      session.task = Bukkit.getScheduler().runTaskTimer(arena.getPlugin(), () -> {
        if(!player.isOnline()
          || !arena.getPlayers().contains(player)
          || arena.getVotingManager().isVotingActive()
          || !sessions.containsKey(player.getUniqueId())
          || !isGui(player.getOpenInventory().getTitle())) {
          cancelSession(player.getUniqueId());
          return;
        }
        session.step++;
        int pct = Math.min(100, session.step * 20);
        XSound.BLOCK_NOTE_BLOCK_PLING.play(player.getLocation(), 0.65F, 0.9F + (session.step * 0.18F));
        buildGui(topInv, session);

        if(session.step >= PROGRESS_SLOTS.length) {
          if(session.task != null) {
            session.task.cancel();
            session.task = null;
          }
          completedSessions.add(player.getUniqueId());
          sessions.remove(player.getUniqueId());
          XSound.ENTITY_PLAYER_LEVELUP.play(player.getLocation(), 0.9F, 1.5F);
          TaskGuiHelper.sendSuccessBar(player, "พ่วงชาร์จแบตเตอรี่สำรองเต็ม 100% สำเร็จ");

          Bukkit.getScheduler().runTaskLater(arena.getPlugin(), () -> {
            if(player.isOnline() && arena.getPlayers().contains(player) && !arena.getVotingManager().isVotingActive()) {
              if(isGui(player.getOpenInventory().getTitle())) {
                player.closeInventory();
              }
              arena.getTaskManager().completeTaskFromMinigame(player, TaskType.CHARGE_SPARE_BATTERY);
            }
          }, 8L);
        } else {
          TaskGuiHelper.sendTaskBar(player, "<#F4F1DE>กำลังชาร์จไฟ " + TaskGuiHelper.buildPipeBar(session.step, PROGRESS_SLOTS.length, 10) + " <#FFB703>" + pct + "%");
        }
      }, 15L, 15L);
      return true;
    }

    if(rawSlot >= 0 && rawSlot < 27) {
      XSound.UI_BUTTON_CLICK.play(player.getLocation(), 0.5F, 0.9F);
      TaskGuiHelper.sendTaskBar(player, "<#FFB703>คลิกสวิตช์เรืองแสงช่องล่างสุด <#F4F1DE>เพื่อเริ่มพ่วงไฟ");
    }
    return true;
  }

  private void cancelSession(UUID uuid) {
    BatterySession old = sessions.remove(uuid);
    if(old != null && old.task != null) {
      old.task.cancel();
    }
  }

  @Override
  public void handleClose(Player player) {
    cancelSession(player.getUniqueId());
    completedSessions.remove(player.getUniqueId());
  }

  @Override
  public void cleanup() {
    for(UUID uuid : new HashSet<>(sessions.keySet())) {
      cancelSession(uuid);
    }
    sessions.clear();
    completedSessions.clear();
  }
}
