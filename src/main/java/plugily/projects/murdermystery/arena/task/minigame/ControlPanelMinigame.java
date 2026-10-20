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
import plugily.projects.minigamesbox.classic.utils.version.VersionUtils;
import plugily.projects.minigamesbox.classic.utils.version.xseries.XMaterial;
import plugily.projects.minigamesbox.classic.utils.version.xseries.XSound;
import plugily.projects.murdermystery.arena.Arena;
import plugily.projects.murdermystery.arena.task.TaskType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class ControlPanelMinigame implements TaskMinigame {

  public static final String GUI_TITLE = "§8[ §6 แผงควบคุมระบบไฟ §8]";

  private static class PanelSession {
    final List<Integer> requiredSequence;
    int currentStep = 0;
    final boolean[] switchState = new boolean[5];

    PanelSession() {
      List<Integer> seq = new ArrayList<>(Arrays.asList(0, 1, 2, 3, 4));
      Collections.shuffle(seq);
      this.requiredSequence = seq;
    }
  }

  private final Arena arena;
  private final Map<UUID, PanelSession> sessions = new HashMap<>();
  private final Set<UUID> completedSessions = new HashSet<>();

  private static final int[] SWITCH_SLOTS = {11, 12, 13, 14, 15};
  private static final int[] LIGHT_SLOTS = {2, 3, 4, 5, 6};
  private static final String[] SWITCH_LABELS = {
    "§e[1] ไฟหน้ารถ (Headlights)",
    "§e[2] ปั๊มน้ำมัน (Fuel Pump)",
    "§e[3] พัดลมหม้อน้ำ (Radiator Fan)",
    "§e[4] ระบบสตาร์ท (Ignition)",
    "§e[5] คัตเอาท์แอร์ (AC Unit)"
  };

  public ControlPanelMinigame(Arena arena) {
    this.arena = arena;
  }

  @Override
  public TaskType getTaskType() {
    return TaskType.CHECK_CONTROL_PANEL;
  }

  @Override
  public String getGuiTitle() {
    return GUI_TITLE;
  }

  @Override
  public boolean isGui(String title) {
    return title != null && title.contains("แผงควบคุมระบบไฟ");
  }

  @Override
  public void open(Player player) {
    completedSessions.remove(player.getUniqueId());
    PanelSession session = new PanelSession();
    sessions.put(player.getUniqueId(), session);

    Inventory inv = Bukkit.createInventory(null, 27, GUI_TITLE);
    buildGui(inv, session);
    player.openInventory(inv);

    XSound.BLOCK_CHEST_OPEN.play(player.getLocation(), 0.8F, 1.2F);
    VersionUtils.sendActionBar(player, "§e[แผงควบคุม] อ่านลำดับขั้นตอนด้านบน แล้วสับสวิตช์ตามลำดับที่ถูกต้อง!");
    player.sendMessage("§6[ภารกิจแผงควบคุม] §eสับสวิตช์ 5 ตัวตามลำดับไฟสีเหลือง (ดูวิธีทำได้ที่ไอคอนหนังสือช่อง 1)");
  }

  private void buildGui(Inventory inv, PanelSession session) {
    ItemStack bg = XMaterial.GRAY_STAINED_GLASS_PANE.parseItem();
    ItemMeta bgMeta = bg.getItemMeta();
    if(bgMeta != null) {
      bgMeta.setDisplayName("§7 ");
      bg.setItemMeta(bgMeta);
    }
    for(int i = 0; i < 27; i++) {
      inv.setItem(i, bg);
    }

    ItemStack instruction = new ItemStack(Material.BOOK);
    ItemMeta iMeta = instruction.getItemMeta();
    if(iMeta != null) {
      iMeta.setDisplayName("§e§l วิธีทำภารกิจ: แผงควบคุมระบบไฟ (Instructions)");
      iMeta.setLore(Arrays.asList(
        "§fขั้นตอนการทำภารกิจ:",
        "§71. สังเกตไฟสัญญาณด้านบน (ไฟสีเหลืองคือสวิตช์ตัวถัดไป)",
        "§72. คลิกสับสวิตช์แถวกลางตามลำดับให้ถูกต้อง",
        "§73. สับสวิตช์ให้ครบทั้ง 5 ตัวเพื่อเปิดระบบไฟสำเร็จ",
        "§c[!] ระวัง: หากสับผิดตัว เบรกเกอร์จะระเบิดและรีเซ็ตใหม่ทั้งหมด!"
      ));
      instruction.setItemMeta(iMeta);
    }
    inv.setItem(0, instruction);

    ItemStack monitor = new ItemStack(Material.KNOWLEDGE_BOOK);
    ItemMeta monMeta = monitor.getItemMeta();
    if(monMeta != null) {
      monMeta.setDisplayName("§6§l[ ลำดับขั้นตอนเปิดระบบความปลอดภัย ]");
      List<String> lore = new ArrayList<>();
      lore.add("§fต้องสับสวิตช์ตามลำดับนี้เท่านั้น:");
      StringBuilder sb = new StringBuilder("§e");
      for(int i = 0; i < session.requiredSequence.size(); i++) {
        int swIdx = session.requiredSequence.get(i);
        if(i > 0) {
          sb.append(" §7-> §e");
        }
        if(i < session.currentStep) {
          sb.append("§a§m[").append(swIdx + 1).append("]§r");
        } else if(i == session.currentStep) {
          sb.append("§6§l[").append(swIdx + 1).append("]");
        } else {
          sb.append("§e[").append(swIdx + 1).append("]");
        }
      }
      lore.add(sb.toString());
      lore.add(" ");
      lore.add("§7ขั้นตอนปัจจุบัน: §aขั้นตอนที่ " + (session.currentStep + 1) + " / 5");
      monMeta.setLore(lore);
      monitor.setItemMeta(monMeta);
    }
    inv.setItem(8, monitor);

    int targetSwitch = (session.currentStep < 5) ? session.requiredSequence.get(session.currentStep) : -1;
    for(int i = 0; i < 5; i++) {
      boolean isOn = session.switchState[i];
      boolean isNext = (i == targetSwitch);

      ItemStack light;
      if(isOn) {
        light = XMaterial.LIME_STAINED_GLASS_PANE.parseItem();
      } else if(isNext) {
        light = XMaterial.YELLOW_STAINED_GLASS_PANE.parseItem();
      } else {
        light = XMaterial.RED_STAINED_GLASS_PANE.parseItem();
      }

      ItemMeta lMeta = light.getItemMeta();
      if(lMeta != null) {
        if(isOn) {
          lMeta.setDisplayName("§a* สวิตช์ " + (i + 1) + " [เปิดแล้ว [/]]");
        } else if(isNext) {
          lMeta.setDisplayName("§e* สวิตช์ " + (i + 1) + " [เปิดตัวนี้เป็นลำดับถัดไป!]");
        } else {
          lMeta.setDisplayName("§c* สวิตช์ " + (i + 1) + " [ปิดอยู่]");
        }
        light.setItemMeta(lMeta);
      }
      inv.setItem(LIGHT_SLOTS[i], light);
    }

    for(int i = 0; i < 5; i++) {
      boolean isOn = session.switchState[i];
      boolean isNext = (!isOn && session.currentStep < session.requiredSequence.size() && session.requiredSequence.get(session.currentStep) == i);

      ItemStack sw;
      if(isOn) {
        sw = XMaterial.TRIPWIRE_HOOK.parseItem();
      } else if(isNext) {
        sw = XMaterial.REDSTONE_TORCH.parseItem();
      } else {
        sw = XMaterial.LEVER.parseItem();
      }
      ItemMeta swMeta = sw.getItemMeta();
      if(swMeta != null) {
        swMeta.setDisplayName(SWITCH_LABELS[i]);
        if(isOn) {
          swMeta.setLore(Arrays.asList(
            "§a[ สถานะ: ON เปิดใช้งานแล้ว [/] ]",
            "§7(เปิดตามลำดับถูกต้องแล้ว)"
          ));
        } else if(isNext) {
          swMeta.setLore(Arrays.asList(
            "§e§l[ * สับสวิตช์ตัวนี้เป็นลำดับถัดไป! ]",
            "§e> คลิกที่นี่เพื่อสับสวิตช์นี้!"
          ));
        } else {
          swMeta.setLore(Arrays.asList(
            "§c[ สถานะ: OFF ปิดอยู่ ]",
            "§7(ยังไม่ถึงคิวสับตัวนี้)"
          ));
        }
        sw.setItemMeta(swMeta);
      }
      inv.setItem(SWITCH_SLOTS[i], sw);
    }

    ItemStack status = new ItemStack(session.currentStep == 5 ? Material.EMERALD : Material.COMPASS);
    ItemMeta stMeta = status.getItemMeta();
    if(stMeta != null) {
      stMeta.setDisplayName(session.currentStep == 5 ? "§a§l[/] ทุกระบบเชื่อมต่อสมบูรณ์!" : "§6ความคืบหน้า: §e" + session.currentStep + " §7/ §e5 สวิตช์");
      stMeta.setLore(Arrays.asList(
        "§7สับสวิตช์ตามลำดับที่มอนิเตอร์กำหนดให้ครบ",
        "§7เพื่อจ่ายกระแสไฟเข้าแผงควบคุมรถบัส"
      ));
      status.setItemMeta(stMeta);
    }
    inv.setItem(22, status);
  }

  @Override
  public boolean handleClick(Player player, int rawSlot, ClickType clickType) {
    if(completedSessions.contains(player.getUniqueId())) {
      return true;
    }

    PanelSession session = sessions.get(player.getUniqueId());
    if(session == null) {
      return true;
    }

    if(rawSlot == 0 || rawSlot == 8) {
      XSound.UI_BUTTON_CLICK.play(player.getLocation(), 0.5F, 1.2F);
      VersionUtils.sendActionBar(player, "§e สับสวิตช์ 5 ตัวตามลำดับไฟสีเหลืองด้านบน!");
      return true;
    }

    int clickedSwitch = -1;
    for(int i = 0; i < SWITCH_SLOTS.length; i++) {
      if(SWITCH_SLOTS[i] == rawSlot) {
        clickedSwitch = i;
        break;
      }
    }

    if(clickedSwitch != -1) {
      if(session.currentStep >= 5) {
        return true;
      }

      int expectedSwitch = session.requiredSequence.get(session.currentStep);

      if(clickedSwitch == expectedSwitch) {
        session.switchState[clickedSwitch] = true;
        session.currentStep++;

        XSound.BLOCK_LEVER_CLICK.play(player.getLocation(), 1F, 1.4F);
        XSound.BLOCK_NOTE_BLOCK_PLING.play(player.getLocation(), 0.8F, 0.8F + (session.currentStep * 0.25F));

        buildGui(player.getOpenInventory().getTopInventory(), session);

        if(session.currentStep >= 5) {
          completedSessions.add(player.getUniqueId());
          sessions.remove(player.getUniqueId());

          XSound.ENTITY_PLAYER_LEVELUP.play(player.getLocation(), 1F, 1.5F);
          VersionUtils.sendActionBar(player, "§a§l[/] แผงควบคุมระบบไฟสตาร์ทสำเร็จ! รถบัสพร้อมทำงาน!");

          Bukkit.getScheduler().runTaskLater(arena.getPlugin(), () -> {
            if(player.isOnline()) {
              player.closeInventory();
              arena.getTaskManager().completeTaskFromMinigame(player, TaskType.CHECK_CONTROL_PANEL);
            }
          }, 10L);
        } else {
          VersionUtils.sendActionBar(player, "§a[/] ถูกต้อง! ขั้นตอนที่ " + session.currentStep + "/5 สำเร็จแล้ว");
        }
      } else {
        XSound.ENTITY_GENERIC_EXPLODE.play(player.getLocation(), 0.7F, 1.8F);
        XSound.BLOCK_ANVIL_LAND.play(player.getLocation(), 0.6F, 0.6F);

        VersionUtils.sendActionBar(player, "§c ไฟฟ้าลัดวงจร! สับสวิตช์ผิดลำดับ เบรกเกอร์ตัดวงจรทั้งหมด!");

        PanelSession fresh = new PanelSession();
        sessions.put(player.getUniqueId(), fresh);
        buildGui(player.getOpenInventory().getTopInventory(), fresh);
      }
    }

    return true;
  }

  @Override
  public void handleClose(Player player) {
    if(completedSessions.contains(player.getUniqueId())) {
      completedSessions.remove(player.getUniqueId());
    }
  }

  @Override
  public void cleanup() {
    sessions.clear();
    completedSessions.clear();
  }
}
