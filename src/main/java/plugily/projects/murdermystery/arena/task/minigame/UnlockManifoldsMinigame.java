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
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class UnlockManifoldsMinigame implements TaskMinigame {

  public static final String GUI_TITLE = HalloweenChat.color("<shadow:#000000FF><#5A4E7C>[</shadow><#FF7518>ตรวจสอบแผนที่เส้นทางสำรอง<shadow:#000000FF><#5A4E7C>]</shadow>");

  private static final int INFO_SLOT = 4;
  private static final int STATUS_SLOT = 31;
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
    if(title == null) {
      return false;
    }
    String plain = HalloweenChat.strip(title);
    return plain.contains("แผนที่เส้นทาง") || plain.contains("ปลดล็อกท่อแรงดัน");
  }

  @Override
  public void open(Player player) {
    completedSessions.remove(player.getUniqueId());
    ManifoldSession session = new ManifoldSession();
    sessions.put(player.getUniqueId(), session);

    Inventory inv = Bukkit.createInventory(null, 36, GUI_TITLE);
    buildGui(inv, session);
    player.openInventory(inv);

    XSound.BLOCK_CHEST_OPEN.play(player.getLocation(), 0.8F, 1.25F);
    TaskGuiHelper.sendTaskBar(player, "<#F4F1DE>คลิกจุดพิกัดบนแผนที่เรียงลำดับจาก <#FFB703>#1 <#F4F1DE>ไปถึง <#6EEB83>#10");
  }

  private void buildGui(Inventory inv, ManifoldSession session) {
    TaskGuiHelper.fillBackground(inv);

    boolean finished = session.nextExpected > 10;
    int unlocked = Math.min(10, session.nextExpected - 1);

    if(finished) {
      inv.setItem(INFO_SLOT, TaskGuiHelper.createItem(
        Material.EMERALD,
        10,
        true,
        TaskGuiHelper.doneName("บันทึกพิกัดเส้นทางสำรองครบ 10 จุดแล้ว"),
        "",
        "<#6EEB83>ได้เส้นทางลัดออกจากเมืองเรียบร้อย"
      ));
    } else {
      inv.setItem(INFO_SLOT, TaskGuiHelper.createItem(
        Material.MAP,
        session.nextExpected,
        false,
        TaskGuiHelper.tag("<#FFB703>", "คลิกพิกัดถัดไป -> จุดที่ #" + session.nextExpected),
        "",
        "<#F4F1DE>มองหาหมุดพิกัดหมายเลข <#FFB703>" + session.nextExpected + " <#F4F1DE>บนแผนที่ด้านล่าง",
        "<#F4F1DE>แล้วคลิกไล่เส้นทางตั้งแต่ <#FFB703>1 <#F4F1DE>จนถึง <#6EEB83>10"
      ));
    }

    for(int i = 0; i < MANIFOLD_SLOTS.length; i++) {
      int num = session.numbers.get(i);
      boolean pressed = num < session.nextExpected;
      boolean highlightStart = (num == 1 && session.nextExpected == 1);

      ItemStack btn;
      if(pressed) {
        btn = TaskGuiHelper.createItem(
          XMaterial.LIME_STAINED_GLASS_PANE,
          num,
          finished,
          TaskGuiHelper.doneName("พิกัดเส้นทาง #" + num),
          "",
          "<#6EEB83>บันทึกจุดผ่านทางแล้ว"
        );
      } else if(highlightStart) {
        btn = TaskGuiHelper.createItem(
          XMaterial.BLUE_STAINED_GLASS_PANE,
          num,
          true,
          TaskGuiHelper.clickName("เริ่มที่พิกัดต้นทาง #1"),
          "",
          "<#F4F1DE>คลิกที่นี่เพื่อเริ่มลากเส้นทางจุดที่ <#FFB703>1",
          "",
          TaskGuiHelper.hint("<#FFB703>", "จากนั้นไล่กด 2, 3 ... ไปจนถึง 10")
        );
      } else {
        btn = TaskGuiHelper.createItem(
          XMaterial.BLUE_STAINED_GLASS_PANE,
          num,
          false,
          TaskGuiHelper.tag("<#72EFDD>", "พิกัดเส้นทาง #" + num),
          "",
          TaskGuiHelper.hint("<#B8B0C8>", "กดเมื่อถึงลำดับจุดที่ " + num)
        );
      }
      inv.setItem(MANIFOLD_SLOTS[i], btn);
    }

    inv.setItem(STATUS_SLOT, TaskGuiHelper.createItem(
      finished ? Material.EMERALD : Material.PAPER,
      Math.max(1, unlocked),
      finished,
      TaskGuiHelper.tag(finished ? "<#6EEB83>" : "<#FF7518>", "บันทึกเส้นทางแล้ว " + unlocked + "/10 จุด"),
      "",
      "<#F4F1DE>ความคืบหน้า " + TaskGuiHelper.buildPipeBar(unlocked, 10, 10)
    ));
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
      if(rawSlot >= 0 && rawSlot < 36) {
        XSound.UI_BUTTON_CLICK.play(player.getLocation(), 0.45F, 1.0F);
        TaskGuiHelper.sendTaskBar(player, "<#F4F1DE>ค้นหาแล้วคลิกหมุดพิกัด <#FFB703>หมายเลข #" + session.nextExpected);
      }
      return true;
    }

    int clickedNum = session.numbers.get(idx);
    if(clickedNum < session.nextExpected) {
      XSound.UI_BUTTON_CLICK.play(player.getLocation(), 0.45F, 1.2F);
      TaskGuiHelper.sendTaskBar(player, "<#6EEB83>พิกัด #" + clickedNum + " บันทึกแล้ว <#FFB703>คลิกพิกัด #" + session.nextExpected + " ถัดไป");
      return true;
    }

    Inventory topInv = player.getOpenInventory().getTopInventory();
    if(clickedNum == session.nextExpected) {
      session.nextExpected++;
      XSound.BLOCK_NOTE_BLOCK_PLING.play(player.getLocation(), 0.85F, 0.85F + (clickedNum * 0.09F));
      buildGui(topInv, session);

      if(session.nextExpected > 10) {
        completedSessions.add(player.getUniqueId());
        sessions.remove(player.getUniqueId());
        XSound.ENTITY_PLAYER_LEVELUP.play(player.getLocation(), 0.9F, 1.5F);
        TaskGuiHelper.sendSuccessBar(player, "ตรวจสอบสมุดแผนที่เส้นทางสำรองสำเร็จ");

        Bukkit.getScheduler().runTaskLater(arena.getPlugin(), () -> {
          if(player.isOnline() && arena.getPlayers().contains(player) && !arena.getVotingManager().isVotingActive()) {
            if(isGui(player.getOpenInventory().getTitle())) {
              player.closeInventory();
            }
            arena.getTaskManager().completeTaskFromMinigame(player, TaskType.UNLOCK_MANIFOLDS);
          }
        }, 8L);
      } else {
        TaskGuiHelper.sendTaskBar(player, "<#6EEB83>บันทึกจุด #" + clickedNum + " แล้ว <#F4F1DE>ถัดไปคลิก <#FFB703>พิกัด #" + session.nextExpected);
      }
    } else {
      session.nextExpected = 1;
      XSound.BLOCK_NOTE_BLOCK_BASS.play(player.getLocation(), 0.9F, 0.6F);
      TaskGuiHelper.sendErrorBar(player, "ลากเส้นทางผิดลำดับ (#" + clickedNum + ") เริ่มไล่ใหม่ตั้งแต่จุดที่ #1");
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
