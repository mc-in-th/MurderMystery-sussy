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

public class RepairWiringMinigame implements TaskMinigame {

  public static final String GUI_TITLE = HalloweenChat.color("<shadow:#000000FF><#5A4E7C>[</shadow><#FF7518>ซ่อมสายไฟเครื่องยนต์<shadow:#000000FF><#5A4E7C>]</shadow>");

  public enum WireColor {
    RED("สีแดง", XMaterial.RED_WOOL, XMaterial.RED_STAINED_GLASS_PANE, "<#FF3B47>"),
    BLUE("สีฟ้า", XMaterial.LIGHT_BLUE_WOOL, XMaterial.LIGHT_BLUE_STAINED_GLASS_PANE, "<#72EFDD>"),
    YELLOW("สีเหลือง", XMaterial.YELLOW_WOOL, XMaterial.YELLOW_STAINED_GLASS_PANE, "<#FFB703>"),
    MAGENTA("สีม่วง", XMaterial.MAGENTA_WOOL, XMaterial.MAGENTA_STAINED_GLASS_PANE, "<#C77DFF>");

    final String name;
    final XMaterial wool;
    final XMaterial glass;
    final String colorCode;

    WireColor(String name, XMaterial wool, XMaterial glass, String colorCode) {
      this.name = name;
      this.wool = wool;
      this.glass = glass;
      this.colorCode = colorCode;
    }
  }

  private static class WiringSession {
    final WireColor[] leftWires = {WireColor.RED, WireColor.BLUE, WireColor.YELLOW, WireColor.MAGENTA};
    final WireColor[] rightWires;
    final boolean[] connected = new boolean[4];
    int selectedLeft = -1;

    WiringSession() {
      List<WireColor> pool = new ArrayList<>(Arrays.asList(leftWires));
      Collections.shuffle(pool);
      if(pool.equals(Arrays.asList(leftWires))) {
        Collections.swap(pool, 0, 1);
      }
      this.rightWires = pool.toArray(new WireColor[0]);
    }
  }

  private final Arena arena;
  private final Map<UUID, WiringSession> sessions = new HashMap<>();
  private final Set<UUID> completedSessions = new HashSet<>();

  private static final int BANNER_SLOT = 4;
  private static final int STATUS_SLOT = 49;
  private static final int[] LEFT_SLOTS = {10, 19, 28, 37};
  private static final int[] RIGHT_SLOTS = {16, 25, 34, 43};

  public RepairWiringMinigame(Arena arena) {
    this.arena = arena;
  }

  @Override
  public TaskType getTaskType() {
    return TaskType.REPAIR_ENGINE_WIRING;
  }

  @Override
  public String getGuiTitle() {
    return GUI_TITLE;
  }

  @Override
  public boolean isGui(String title) {
    return title != null && HalloweenChat.strip(title).contains("ซ่อมสายไฟเครื่องยนต์");
  }

  @Override
  public void open(Player player) {
    completedSessions.remove(player.getUniqueId());
    WiringSession session = new WiringSession();
    sessions.put(player.getUniqueId(), session);

    Inventory inv = Bukkit.createInventory(null, 54, GUI_TITLE);
    buildGui(inv, session);
    player.openInventory(inv);

    TaskGuiHelper.playSound(player, XSound.BLOCK_CHEST_OPEN, 0.8F, 1.2F);
    TaskGuiHelper.sendTaskBar(player, "<#F4F1DE>คลิก <#FFB703>สายไฟเรืองแสงฝั่งซ้าย <#F4F1DE>แล้วคลิกต่อกับ <#6EEB83>สีเดียวกันฝั่งขวา");
  }

  private void buildGui(Inventory inv, WiringSession session) {
    TaskGuiHelper.fillBackground(inv);

    int doneCount = 0;
    for(boolean b : session.connected) {
      if(b) {
        doneCount++;
      }
    }
    boolean allDone = doneCount == 4;

    if(allDone) {
      inv.setItem(BANNER_SLOT, TaskGuiHelper.createItem(
        Material.EMERALD,
        1,
        true,
        TaskGuiHelper.doneName("เชื่อมต่อสายไฟเครื่องยนต์ครบ 4 เส้นแล้ว"),
        "",
        "<#6EEB83>กระแสไฟเครื่องยนต์เดินครบทุกวงจร"
      ));
    } else if(session.selectedLeft != -1) {
      WireColor sel = session.leftWires[session.selectedLeft];
      inv.setItem(BANNER_SLOT, TaskGuiHelper.createItem(
        XMaterial.REDSTONE_LAMP,
        1,
        true,
        TaskGuiHelper.tag("<#FFB703>", "ขั้นที่ 2 - คลิกขั้วสายไฟ" + sel.name + "ทางฝั่งขวา"),
        "",
        "<#F4F1DE>กำลังถือสายไฟ " + sel.colorCode + sel.name,
        "<#F4F1DE>มองหาขั้ว " + sel.colorCode + sel.name + " <#F4F1DE>ในคอลัมน์ขวาสุดแล้วคลิกเชื่อมต่อ"
      ));
    } else {
      inv.setItem(BANNER_SLOT, TaskGuiHelper.createItem(
        XMaterial.REDSTONE_LAMP,
        4 - doneCount,
        false,
        TaskGuiHelper.tag("<#FF7518>", "ขั้นที่ 1 - คลิกเลือกสายไฟเรืองแสงทางฝั่งซ้าย"),
        "",
        "<#F4F1DE>1. คลิกขั้วสายไฟที่เรืองแสงในคอลัมน์ซ้าย",
        "<#F4F1DE>2. คลิกต่อเข้ากับขั้วสีเดียวกันในคอลัมน์ขวา"
      ));
    }

    for(int i = 0; i < 4; i++) {
      WireColor leftColor = session.leftWires[i];
      boolean isConnected = session.connected[i];
      boolean isSelected = (session.selectedLeft == i);

      ItemStack leftItem;
      if(isConnected) {
        leftItem = TaskGuiHelper.createItem(
          leftColor.wool,
          1,
          false,
          TaskGuiHelper.doneName("สายไฟ" + leftColor.colorCode + leftColor.name),
          "",
          "<#6EEB83>เชื่อมต่อกระแสไฟเรียบร้อยแล้ว"
        );
      } else if(isSelected) {
        leftItem = TaskGuiHelper.createItem(
          leftColor.wool,
          1,
          true,
          TaskGuiHelper.tag("<#FFB703>", "กำลังถือสาย") + " " + leftColor.colorCode + "สายไฟ" + leftColor.name,
          "",
          TaskGuiHelper.hint("<#FFB703>", "คลิกขั้ว" + leftColor.name + "ทางฝั่งขวาเพื่อเชื่อมต่อ")
        );
      } else if(session.selectedLeft == -1) {
        leftItem = TaskGuiHelper.createItem(
          leftColor.wool,
          1,
          true,
          TaskGuiHelper.clickName("เลือกสายไฟ" + leftColor.colorCode + leftColor.name),
          "",
          TaskGuiHelper.hint("<#FFB703>", "กดคลิกเพื่อหยิบสายไฟเส้นนี้")
        );
      } else {
        leftItem = TaskGuiHelper.createItem(
          leftColor.wool,
          1,
          false,
          TaskGuiHelper.tag(leftColor.colorCode, "ขั้วสายไฟ" + leftColor.name),
          "",
          TaskGuiHelper.hint("<#B8B0C8>", "คลิกเพื่อเปลี่ยนมาจับสายเส้นนี้แทน")
        );
      }
      inv.setItem(LEFT_SLOTS[i], leftItem);

      int startSlot = LEFT_SLOTS[i] + 1;
      int endSlot = RIGHT_SLOTS[i] - 1;
      for(int s = startSlot; s <= endSlot; s++) {
        int offset = s - startSlot;
        ItemStack wirePane;
        if(isConnected) {
          wirePane = TaskGuiHelper.createItem(
            leftColor.glass,
            1,
            false,
            TaskGuiHelper.tag(leftColor.colorCode, "สายไฟ" + leftColor.name + " - เชื่อมต่อแล้ว"),
            "",
            "<#6EEB83>กระแสไฟไหลปกติ"
          );
        } else if(isSelected && offset < 2) {
          wirePane = TaskGuiHelper.createItem(
            leftColor.glass,
            1,
            offset == 1,
            TaskGuiHelper.tag(leftColor.colorCode, "กำลังลากสายไฟ" + leftColor.name + " ->"),
            "",
            TaskGuiHelper.hint("<#FFB703>", "คลิกขั้ว" + leftColor.name + "ทางฝั่งขวา")
          );
        } else {
          wirePane = TaskGuiHelper.createItem(
            XMaterial.GRAY_STAINED_GLASS_PANE,
            1,
            false,
            TaskGuiHelper.tag("<#B8B0C8>", "วงจรขาด"),
            "",
            "<#B8B0C8>รอเชื่อมต่อสายไฟ"
          );
        }
        inv.setItem(s, wirePane);
      }

      WireColor rightColor = session.rightWires[i];
      boolean rightConnected = false;
      for(int k = 0; k < 4; k++) {
        if(session.connected[k] && session.leftWires[k] == rightColor) {
          rightConnected = true;
          break;
        }
      }

      ItemStack rightItem;
      if(rightConnected) {
        rightItem = TaskGuiHelper.createItem(
          rightColor.wool,
          1,
          false,
          TaskGuiHelper.doneName("ปลายสาย" + rightColor.colorCode + rightColor.name),
          "",
          "<#6EEB83>เชื่อมต่อครบวงจรแล้ว"
        );
      } else if(session.selectedLeft != -1) {
        rightItem = TaskGuiHelper.createItem(
          rightColor.wool,
          1,
          true,
          TaskGuiHelper.clickName("ต่อเข้ากับปลายสาย" + rightColor.colorCode + rightColor.name),
          "",
          TaskGuiHelper.hint("<#FFB703>", "คลิกหากตรงกับสีสายไฟที่ถืออยู่")
        );
      } else {
        rightItem = TaskGuiHelper.createItem(
          rightColor.wool,
          1,
          false,
          TaskGuiHelper.tag(rightColor.colorCode, "ปลายสาย" + rightColor.name),
          "",
          TaskGuiHelper.hint("<#B8B0C8>", "คลิกเลือกสายไฟทางฝั่งซ้ายก่อน")
        );
      }
      inv.setItem(RIGHT_SLOTS[i], rightItem);
    }

    inv.setItem(STATUS_SLOT, TaskGuiHelper.createItem(
      allDone ? Material.EMERALD : Material.REDSTONE,
      Math.max(1, doneCount),
      allDone,
      TaskGuiHelper.tag(allDone ? "<#6EEB83>" : "<#FF7518>", "เชื่อมต่อสายไฟแล้ว " + doneCount + "/4 เส้น"),
      "",
      "<#F4F1DE>ความคืบหน้า " + TaskGuiHelper.buildPipeBar(doneCount, 4, 8)
    ));
  }

  @Override
  public boolean handleClick(Player player, int rawSlot, ClickType clickType) {
    if(completedSessions.contains(player.getUniqueId())) {
      return true;
    }

    WiringSession session = sessions.get(player.getUniqueId());
    if(session == null) {
      return true;
    }

    Inventory topInv = player.getOpenInventory().getTopInventory();

    for(int i = 0; i < 4; i++) {
      if(LEFT_SLOTS[i] == rawSlot) {
        if(session.connected[i]) {
          TaskGuiHelper.playSound(player, XSound.UI_BUTTON_CLICK, 0.5F, 1.1F);
          TaskGuiHelper.sendTaskBar(player, "<#6EEB83>สายเส้นนี้เชื่อมแล้ว <#FFB703>คลิกสายเส้นอื่นที่เรืองแสง");
          return true;
        }

        session.selectedLeft = i;
        WireColor color = session.leftWires[i];
        TaskGuiHelper.playSound(player, XSound.BLOCK_LEVER_CLICK, 1F, 1.25F);
        TaskGuiHelper.sendTaskBar(player, "<#F4F1DE>จับสายไฟ" + color.colorCode + color.name + " <#F4F1DE>แล้ว คลิกขั้ว" + color.colorCode + color.name + " <#F4F1DE>ทางฝั่งขวา");
        buildGui(topInv, session);
        return true;
      }
    }

    for(int j = 0; j < 4; j++) {
      if(RIGHT_SLOTS[j] == rawSlot) {
        if(session.selectedLeft == -1) {
          TaskGuiHelper.playSound(player, XSound.ENTITY_VILLAGER_NO, 0.75F, 1.1F);
          TaskGuiHelper.sendErrorBar(player, "คลิกเลือกสายไฟที่เรืองแสงทางฝั่งซ้ายก่อน");
          return true;
        }

        WireColor selectedColor = session.leftWires[session.selectedLeft];
        WireColor clickedRightColor = session.rightWires[j];

        if(selectedColor == clickedRightColor) {
          session.connected[session.selectedLeft] = true;
          session.selectedLeft = -1;

          TaskGuiHelper.playSound(player, XSound.BLOCK_ANVIL_USE, 0.55F, 1.8F);
          TaskGuiHelper.playSound(player, XSound.BLOCK_NOTE_BLOCK_PLING, 0.85F, 1.6F);

          buildGui(topInv, session);

          boolean allDone = true;
          int doneCount = 0;
          for(boolean b : session.connected) {
            if(b) {
              doneCount++;
            } else {
              allDone = false;
            }
          }

          if(allDone) {
            completedSessions.add(player.getUniqueId());
            sessions.remove(player.getUniqueId());

            TaskGuiHelper.playSound(player, XSound.ENTITY_PLAYER_LEVELUP, 1F, 1.5F);
            TaskGuiHelper.sendSuccessBar(player, "ซ่อมสายไฟเครื่องยนต์ครบทั้ง 4 เส้นสำเร็จ");

            Bukkit.getScheduler().runTaskLater(arena.getPlugin(), () -> {
              if(player.isOnline() && arena.getPlayers().contains(player) && !arena.getVotingManager().isVotingActive()) {
                if(isGui(player.getOpenInventory().getTitle())) {
                  player.closeInventory();
                }
                arena.getTaskManager().completeTaskFromMinigame(player, TaskType.REPAIR_ENGINE_WIRING);
              }
            }, 8L);
          } else {
            TaskGuiHelper.sendTaskBar(player, "<#6EEB83>ต่อสายไฟ" + selectedColor.name + "สำเร็จ (" + doneCount + "/4) <#FFB703>เลือกสายถัดไปฝั่งซ้าย");
          }
        } else {
          TaskGuiHelper.playSound(player, XSound.ENTITY_VILLAGER_NO, 0.9F, 0.85F);
          TaskGuiHelper.playSound(player, XSound.BLOCK_REDSTONE_TORCH_BURNOUT, 0.65F, 1.4F);

          TaskGuiHelper.sendErrorBar(player, "ขั้วคนละสี กรุณาคลิกเลือกสายไฟฝั่งซ้ายใหม่อีกครั้ง");
          session.selectedLeft = -1;
          buildGui(topInv, session);
        }
        return true;
      }
    }

    if(rawSlot >= 0 && rawSlot < 54) {
      TaskGuiHelper.playSound(player, XSound.UI_BUTTON_CLICK, 0.45F, 1.0F);
      if(session.selectedLeft == -1) {
        TaskGuiHelper.sendTaskBar(player, "<#FFB703>คลิกสายไฟที่เรืองแสงทางคอลัมน์ซ้ายก่อน");
      } else {
        WireColor sel = session.leftWires[session.selectedLeft];
        TaskGuiHelper.sendTaskBar(player, "<#FFB703>คลิกขั้ว" + sel.colorCode + sel.name + " <#FFB703>ทางคอลัมน์ขวาสุด");
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
