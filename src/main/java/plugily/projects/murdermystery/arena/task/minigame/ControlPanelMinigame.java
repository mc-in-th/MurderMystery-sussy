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
import plugily.projects.minigamesbox.classic.utils.version.xseries.XMaterial;
import plugily.projects.minigamesbox.classic.utils.version.xseries.XSound;
import plugily.projects.murdermystery.arena.Arena;
import plugily.projects.murdermystery.arena.task.TaskType;
import plugily.projects.murdermystery.utils.HalloweenChat;

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

  public static final String GUI_TITLE = HalloweenChat.color("<shadow:#000000FF><#5A4E7C>[</shadow><#FF7518>แผงควบคุมระบบไฟ<shadow:#000000FF><#5A4E7C>]</shadow>");

  private static final int MONITOR_SLOT = 4;
  private static final int[] LIGHT_SLOTS = {11, 12, 13, 14, 15};
  private static final int[] SWITCH_SLOTS = {20, 21, 22, 23, 24};
  private static final int STATUS_SLOT = 31;

  private static final String[] SWITCH_NAMES = {
    "ไฟหน้ารถ",
    "ปั๊มน้ำมัน",
    "พัดลมหม้อน้ำ",
    "ระบบสตาร์ท",
    "ระบบปรับอากาศ"
  };

  private static class PanelSession {
    final List<Integer> requiredSequence;
    int currentStep = 0;
    final boolean[] switchState = new boolean[5];

    PanelSession() {
      List<Integer> seq = new ArrayList<>(Arrays.asList(0, 1, 2, 3, 4));
      Collections.shuffle(seq);
      this.requiredSequence = seq;
    }

    void resetProgressKeepSequence() {
      this.currentStep = 0;
      Arrays.fill(this.switchState, false);
    }
  }

  private final Arena arena;
  private final Map<UUID, PanelSession> sessions = new HashMap<>();
  private final Set<UUID> completedSessions = new HashSet<>();

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
    return title != null && HalloweenChat.strip(title).contains("แผงควบคุมระบบไฟ");
  }

  @Override
  public void open(Player player) {
    completedSessions.remove(player.getUniqueId());
    PanelSession session = new PanelSession();
    sessions.put(player.getUniqueId(), session);

    Inventory inv = Bukkit.createInventory(null, 36, GUI_TITLE);
    buildGui(inv, session);
    player.openInventory(inv);

    TaskGuiHelper.playSound(player, XSound.BLOCK_CHEST_OPEN, 0.8F, 1.2F);
    int firstSwitch = session.requiredSequence.get(0) + 1;
    TaskGuiHelper.sendTaskBar(player, "<#F4F1DE>คลิกสวิตช์ที่เรืองแสงใต้ไฟสีเหลือง <shadow:#000000FF><#5A4E7C>(</shadow><#FFB703>เริ่มที่สวิตช์ #" + firstSwitch + "<shadow:#000000FF><#5A4E7C>)</shadow>");
  }

  private void buildGui(Inventory inv, PanelSession session) {
    TaskGuiHelper.fillBackground(inv);

    boolean finished = session.currentStep >= 5;
    int targetSwitch = finished ? -1 : session.requiredSequence.get(session.currentStep);

    StringBuilder seqBuilder = new StringBuilder();
    for(int i = 0; i < session.requiredSequence.size(); i++) {
      int swNum = session.requiredSequence.get(i) + 1;
      if(i > 0) {
        seqBuilder.append(" <shadow:#000000FF><#5A4E7C>-></shadow> ");
      }
      if(i < session.currentStep) {
        seqBuilder.append("<#6EEB83>#").append(swNum);
      } else if(i == session.currentStep) {
        seqBuilder.append("<#FFB703>[#").append(swNum).append("]");
      } else {
        seqBuilder.append("<#B8B0C8>#").append(swNum);
      }
    }

    if(finished) {
      inv.setItem(MONITOR_SLOT, TaskGuiHelper.createItem(
        Material.EMERALD,
        1,
        true,
        TaskGuiHelper.doneName("แผงควบคุมระบบไฟพร้อมทำงานแล้ว"),
        "",
        seqBuilder.toString()
      ));
    } else {
      inv.setItem(MONITOR_SLOT, TaskGuiHelper.createItem(
        XMaterial.DAYLIGHT_DETECTOR,
        targetSwitch + 1,
        false,
        TaskGuiHelper.tag("<#FFB703>", "สวิตช์ถัดไป - หมายเลข #" + (targetSwitch + 1)),
        "",
        "<#F4F1DE>ลำดับการเปิดไฟ " + seqBuilder,
        "",
        TaskGuiHelper.hint("<#FFB703>", "คลิกสวิตช์ที่เรืองแสงใต้ไฟสีเหลือง")
      ));
    }

    for(int i = 0; i < 5; i++) {
      int num = i + 1;
      boolean isOn = session.switchState[i];
      boolean isNext = (i == targetSwitch);

      ItemStack light;
      if(isOn) {
        light = TaskGuiHelper.createItem(
          XMaterial.LIME_STAINED_GLASS_PANE,
          1,
          finished,
          TaskGuiHelper.tag("<#6EEB83>", "ไฟสัญญาณ #" + num + " - เปิดแล้ว"),
          "",
          "<#6EEB83>ระบบ " + SWITCH_NAMES[i] + " ทำงานปกติ"
        );
      } else if(isNext) {
        light = TaskGuiHelper.createItem(
          XMaterial.YELLOW_STAINED_GLASS_PANE,
          1,
          true,
          TaskGuiHelper.tag("<#FFB703>", "ไฟสัญญาณ #" + num + " - ลำดับถัดไป"),
          "",
          TaskGuiHelper.hint("<#FFB703>", "คลิกสวิตช์ #" + num + " ที่อยู่ด้านล่างนี้")
        );
      } else {
        light = TaskGuiHelper.createItem(
          XMaterial.GRAY_STAINED_GLASS_PANE,
          1,
          false,
          TaskGuiHelper.tag("<#B8B0C8>", "ไฟสัญญาณ #" + num + " - รอคิว"),
          "",
          "<#B8B0C8>ยังไม่ถึงลำดับการสับสวิตช์นี้"
        );
      }
      inv.setItem(LIGHT_SLOTS[i], light);

      ItemStack sw;
      if(isOn) {
        sw = TaskGuiHelper.createItem(
          XMaterial.TRIPWIRE_HOOK,
          num,
          false,
          TaskGuiHelper.doneName("สวิตช์ #" + num + " (" + SWITCH_NAMES[i] + ")"),
          "",
          "<#6EEB83>จ่ายกระแสไฟเรียบร้อยแล้ว"
        );
      } else if(isNext) {
        sw = TaskGuiHelper.createItem(
          XMaterial.REDSTONE_TORCH,
          num,
          true,
          TaskGuiHelper.clickName("สับสวิตช์ #" + num + " (" + SWITCH_NAMES[i] + ")"),
          "",
          "<#F4F1DE>เปิดวงจร <#FFB703>" + SWITCH_NAMES[i],
          "",
          TaskGuiHelper.hint("<#FFB703>", "กดคลิกสวิตช์นี้เป็นลำดับถัดไป")
        );
      } else {
        sw = TaskGuiHelper.createItem(
          XMaterial.LEVER,
          num,
          false,
          TaskGuiHelper.tag("<#B8B0C8>", "รอคิว สวิตช์ #" + num) + " <#B8B0C8>(" + SWITCH_NAMES[i] + ")",
          "",
          TaskGuiHelper.hint("<#FF3B47>", "อย่าเพิ่งกด คลิกเฉพาะตัวที่มีไฟสีเหลือง")
        );
      }
      inv.setItem(SWITCH_SLOTS[i], sw);
    }

    inv.setItem(STATUS_SLOT, TaskGuiHelper.createItem(
      finished ? Material.EMERALD : Material.REDSTONE,
      Math.max(1, session.currentStep),
      finished,
      TaskGuiHelper.tag(finished ? "<#6EEB83>" : "<#FF7518>", "เปิดสวิตช์แล้ว " + session.currentStep + "/5 ตัว"),
      "",
      "<#F4F1DE>ความคืบหน้า " + TaskGuiHelper.buildPipeBar(session.currentStep, 5, 10)
    ));
  }

  @Override
  public boolean handleClick(Player player, int rawSlot, ClickType clickType) {
    if(completedSessions.contains(player.getUniqueId())) {
      return true;
    }

    PanelSession session = sessions.get(player.getUniqueId());
    if(session == null || session.currentStep >= 5) {
      return true;
    }

    int expectedSwitch = session.requiredSequence.get(session.currentStep);

    int clickedSwitch = -1;
    for(int i = 0; i < SWITCH_SLOTS.length; i++) {
      if(SWITCH_SLOTS[i] == rawSlot) {
        clickedSwitch = i;
        break;
      }
      if(LIGHT_SLOTS[i] == rawSlot && i == expectedSwitch) {
        clickedSwitch = i;
        break;
      }
    }

    if(clickedSwitch == -1) {
      if(rawSlot >= 0 && rawSlot < 36) {
        TaskGuiHelper.playSound(player, XSound.UI_BUTTON_CLICK, 0.45F, 1.0F);
        TaskGuiHelper.sendTaskBar(player, "<#FFB703>คลิกที่สวิตช์ #" + (expectedSwitch + 1) + " <#F4F1DE>ที่เรืองแสงใต้ไฟสีเหลือง");
      }
      return true;
    }

    if(session.switchState[clickedSwitch]) {
      TaskGuiHelper.playSound(player, XSound.UI_BUTTON_CLICK, 0.5F, 1.2F);
      return true;
    }

    Inventory topInv = player.getOpenInventory().getTopInventory();

    if(clickedSwitch == expectedSwitch) {
      session.switchState[clickedSwitch] = true;
      session.currentStep++;

      TaskGuiHelper.playSound(player, XSound.BLOCK_LEVER_CLICK, 1F, 1.4F);
      TaskGuiHelper.playSound(player, XSound.BLOCK_NOTE_BLOCK_PLING, 0.8F, 0.85F + (session.currentStep * 0.22F));

      buildGui(topInv, session);

      if(session.currentStep >= 5) {
        completedSessions.add(player.getUniqueId());
        sessions.remove(player.getUniqueId());

        TaskGuiHelper.playSound(player, XSound.ENTITY_PLAYER_LEVELUP, 1F, 1.5F);
        TaskGuiHelper.sendSuccessBar(player, "เชื่อมต่อแผงควบคุมระบบไฟครบทุกจุดแล้ว");

        Bukkit.getScheduler().runTaskLater(arena.getPlugin(), () -> {
          if(player.isOnline() && arena.getPlayers().contains(player) && !arena.getVotingManager().isVotingActive()) {
            if(isGui(player.getOpenInventory().getTitle())) {
              player.closeInventory();
            }
            arena.getTaskManager().completeTaskFromMinigame(player, TaskType.CHECK_CONTROL_PANEL);
          }
        }, 8L);
      } else {
        int nextSw = session.requiredSequence.get(session.currentStep) + 1;
        TaskGuiHelper.sendTaskBar(player, "<#6EEB83>เปิดสวิตช์สำเร็จ <#F4F1DE>ลำดับถัดไปคือ <#FFB703>สวิตช์ #" + nextSw);
      }
    } else {
      session.resetProgressKeepSequence();
      TaskGuiHelper.playSound(player, XSound.BLOCK_REDSTONE_TORCH_BURNOUT, 0.8F, 0.9F);
      TaskGuiHelper.playSound(player, XSound.BLOCK_NOTE_BLOCK_BASS, 0.9F, 0.65F);

      int firstSw = session.requiredSequence.get(0) + 1;
      TaskGuiHelper.sendErrorBar(player, "สับผิดตัว เบรกเกอร์ตัดไฟ เริ่มสับใหม่ที่สวิตช์ #" + firstSw);
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
