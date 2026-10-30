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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class EngineOilMinigame implements TaskMinigame {

  public static final String GUI_TITLE = HalloweenChat.color("<shadow:#000000FF><#5A4E7C>[</shadow><#FF7518>เช็คน้ำมันเครื่องยนต์<shadow:#000000FF><#5A4E7C>]</shadow>");

  private static final int MAX_LEVEL = 5;
  private static final int BANNER_SLOT = 4;
  private static final int[] GAUGE_SLOTS = {11, 12, 13, 14, 15};
  private static final int OIL_CAN_SLOT = 22;
  private static final int CAP_SLOT = 31;

  private static class OilSession {
    int oilLevel = 1;
  }

  private final Arena arena;
  private final Map<UUID, OilSession> sessions = new HashMap<>();
  private final Set<UUID> completedSessions = new HashSet<>();

  public EngineOilMinigame(Arena arena) {
    this.arena = arena;
  }

  @Override
  public TaskType getTaskType() {
    return TaskType.CHECK_ENGINE_OIL;
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
    return plain.contains("น้ำมันเครื่อง") || plain.contains("Oil");
  }

  @Override
  public void open(Player player) {
    completedSessions.remove(player.getUniqueId());
    OilSession session = new OilSession();
    sessions.put(player.getUniqueId(), session);

    Inventory inv = Bukkit.createInventory(null, 36, GUI_TITLE);
    buildGui(inv, session);
    player.openInventory(inv);

    TaskGuiHelper.playSound(player, XSound.BLOCK_CHEST_OPEN, 0.8F, 1.2F);
    TaskGuiHelper.sendTaskBar(player, "<#F4F1DE>คลิก <#FFB703>ขวดน้ำมันเครื่องเรืองแสงตรงกลาง <#F4F1DE>จนเต็มขีดสีเขียว (5/5)");
  }

  private void buildGui(Inventory inv, OilSession session) {
    TaskGuiHelper.fillBackground(inv);

    boolean full = session.oilLevel >= MAX_LEVEL;

    if(full) {
      inv.setItem(BANNER_SLOT, TaskGuiHelper.createItem(
        Material.COMPASS,
        MAX_LEVEL,
        false,
        TaskGuiHelper.tag("<#6EEB83>", "ขั้นที่ 2/2 - น้ำมันเครื่องเต็มขีดมาตรฐาน (5/5)"),
        "",
        "<#F4F1DE>คลิกปุ่ม <#6EEB83>มรกตเรืองแสงด้านล่าง <#F4F1DE>เพื่อปิดฝาน้ำมันเครื่อง"
      ));
    } else {
      inv.setItem(BANNER_SLOT, TaskGuiHelper.createItem(
        Material.COMPASS,
        session.oilLevel,
        false,
        TaskGuiHelper.tag("<#FF7518>", "ขั้นที่ 1/2 - เติมน้ำมันเครื่อง (" + session.oilLevel + "/" + MAX_LEVEL + ")"),
        "",
        "<#F4F1DE>คลิกที่ <#FFB703>ขวดน้ำมันเครื่องเรืองแสงตรงกลาง <#F4F1DE>เพื่อเติมทีละขีด",
        "<#F4F1DE>เมื่อครบ <#6EEB83>5/5 ขีด <#F4F1DE>จึงกดปิดฝาด้านล่าง"
      ));
    }

    for(int i = 0; i < GAUGE_SLOTS.length; i++) {
      int mark = i + 1;
      ItemStack bar;
      if(full) {
        bar = TaskGuiHelper.createItem(
          XMaterial.LIME_STAINED_GLASS_PANE,
          1,
          false,
          TaskGuiHelper.tag("<#6EEB83>", "ขีดวัดน้ำมันเครื่อง " + mark + "/5 - เต็มมาตรฐาน"),
          "",
          "<#6EEB83>ระดับน้ำมันเครื่องหล่อลื่นสมบูรณ์"
        );
      } else if(i < session.oilLevel) {
        XMaterial activeMat = (session.oilLevel == 1) ? XMaterial.RED_STAINED_GLASS_PANE : XMaterial.YELLOW_STAINED_GLASS_PANE;
        String activeColor = (session.oilLevel == 1) ? "<#FF3B47>" : "<#FFB703>";
        bar = TaskGuiHelper.createItem(
          activeMat,
          1,
          false,
          TaskGuiHelper.tag(activeColor, "ระดับน้ำมันเครื่อง " + mark + "/5"),
          "",
          session.oilLevel == 1 ? "<#FF3B47>น้ำมันเครื่องแห้ง ต้องเติมเพิ่ม" : "<#FFB703>กำลังเติมน้ำมันเครื่อง..."
        );
      } else if(i == MAX_LEVEL - 1) {
        bar = TaskGuiHelper.createItem(
          XMaterial.GREEN_STAINED_GLASS_PANE,
          1,
          false,
          TaskGuiHelper.tag("<#6EEB83>", "ขีดเป้าหมายมาตรฐาน (5/5)"),
          "",
          TaskGuiHelper.hint("<#B8B0C8>", "เติมน้ำมันให้ถึงขีดนี้")
        );
      } else {
        bar = TaskGuiHelper.createItem(
          XMaterial.GRAY_STAINED_GLASS_PANE,
          1,
          false,
          TaskGuiHelper.tag("<#B8B0C8>", "ขีดวัดระดับ " + mark + "/5"),
          "",
          "<#B8B0C8>ยังว่างอยู่"
        );
      }
      inv.setItem(GAUGE_SLOTS[i], bar);
    }

    if(full) {
      inv.setItem(OIL_CAN_SLOT, TaskGuiHelper.createItem(
        Material.GLASS_BOTTLE,
        1,
        false,
        TaskGuiHelper.doneName("เทน้ำมันเครื่องครบ 5/5 ขีดแล้ว"),
        "",
        TaskGuiHelper.hint("<#6EEB83>", "กดปุ่มมรกตด้านล่างเพื่อปิดฝาน้ำมันเครื่อง")
      ));
    } else {
      inv.setItem(OIL_CAN_SLOT, TaskGuiHelper.createItem(
        Material.HONEY_BOTTLE,
        1,
        true,
        TaskGuiHelper.clickName("เทน้ำมันเครื่อง (+1 ขีด)"),
        "",
        "<#F4F1DE>ระดับปัจจุบัน <#FFB703>" + session.oilLevel + "/5 <shadow:#000000FF><#5A4E7C>-></shadow> <#6EEB83>" + (session.oilLevel + 1) + "/5",
        "",
        TaskGuiHelper.hint("<#FFB703>", "กดคลิกที่นี่เพื่อเติมน้ำมันเครื่อง")
      ));
    }

    if(full) {
      inv.setItem(CAP_SLOT, TaskGuiHelper.createItem(
        XMaterial.EMERALD_BLOCK,
        1,
        true,
        TaskGuiHelper.confirmName("ปิดฝาน้ำมันเครื่อง"),
        "",
        "<#F4F1DE>น้ำมันเครื่องได้ระดับมาตรฐาน <#6EEB83>5/5",
        "",
        TaskGuiHelper.hint("<#6EEB83>", "กดคลิกที่นี่เพื่อเสร็จสิ้นภารกิจ")
      ));
    } else {
      inv.setItem(CAP_SLOT, TaskGuiHelper.createItem(
        XMaterial.IRON_BARS,
        1,
        false,
        TaskGuiHelper.lockedName("ปิดฝาน้ำมันเครื่อง"),
        "",
        "<#FF3B47>ต้องเติมน้ำมันให้ครบ 5/5 ขีดก่อน",
        "",
        TaskGuiHelper.hint("<#B8B0C8>", "ยังขาดอีก " + (MAX_LEVEL - session.oilLevel) + " ขีด")
      ));
    }
  }

  @Override
  public boolean handleClick(Player player, int rawSlot, ClickType clickType) {
    if(completedSessions.contains(player.getUniqueId())) {
      return true;
    }

    OilSession session = sessions.get(player.getUniqueId());
    if(session == null) {
      return true;
    }

    Inventory topInv = player.getOpenInventory().getTopInventory();

    if(rawSlot == OIL_CAN_SLOT) {
      if(session.oilLevel < MAX_LEVEL) {
        session.oilLevel++;
        TaskGuiHelper.playSound(player, XSound.ITEM_BUCKET_EMPTY, 0.9F, 1.25F);
        TaskGuiHelper.playSound(player, XSound.ITEM_BOTTLE_FILL, 0.8F, 0.95F + (session.oilLevel * 0.1F));
        buildGui(topInv, session);

        if(session.oilLevel >= MAX_LEVEL) {
          TaskGuiHelper.playSound(player, XSound.BLOCK_NOTE_BLOCK_PLING, 1F, 1.8F);
          TaskGuiHelper.sendTaskBar(player, "<#6EEB83>น้ำมันเครื่องเต็มขีด 5/5 แล้ว <#FFB703>คลิกปุ่มมรกตด้านล่างเพื่อปิดฝา");
        } else {
          TaskGuiHelper.sendTaskBar(player, "<#F4F1DE>เติมน้ำมันเครื่องแล้ว <#FFB703>" + session.oilLevel + "/" + MAX_LEVEL);
        }
      } else {
        TaskGuiHelper.playSound(player, XSound.UI_BUTTON_CLICK, 0.5F, 1.2F);
        TaskGuiHelper.sendTaskBar(player, "<#6EEB83>คลิกปุ่มมรกตด้านล่าง <#F4F1DE>เพื่อปิดฝาน้ำมันเครื่อง");
      }
      return true;
    }

    if(rawSlot == CAP_SLOT) {
      if(session.oilLevel >= MAX_LEVEL) {
        completedSessions.add(player.getUniqueId());
        sessions.remove(player.getUniqueId());

        TaskGuiHelper.playSound(player, XSound.BLOCK_ANVIL_USE, 0.8F, 1.5F);
        TaskGuiHelper.playSound(player, XSound.ENTITY_PLAYER_LEVELUP, 1F, 1.5F);
        TaskGuiHelper.sendSuccessBar(player, "เช็คน้ำมันเครื่องยนต์สำเร็จ");

        Bukkit.getScheduler().runTaskLater(arena.getPlugin(), () -> {
          if(player.isOnline() && arena.getPlayers().contains(player) && !arena.getVotingManager().isVotingActive()) {
            if(isGui(player.getOpenInventory().getTitle())) {
              player.closeInventory();
            }
            arena.getTaskManager().completeTaskFromMinigame(player, TaskType.CHECK_ENGINE_OIL);
          }
        }, 8L);
      } else {
        TaskGuiHelper.playSound(player, XSound.ENTITY_VILLAGER_NO, 0.9F, 1F);
        TaskGuiHelper.sendErrorBar(player, "คลิกขวดน้ำมันเรืองแสงตรงกลางให้ครบ 5/5 ขีดก่อน");
      }
      return true;
    }

    if(rawSlot >= 0 && rawSlot < 36) {
      TaskGuiHelper.playSound(player, XSound.UI_BUTTON_CLICK, 0.45F, 1.0F);
      if(session.oilLevel < MAX_LEVEL) {
        TaskGuiHelper.sendTaskBar(player, "<#FFB703>คลิกขวดน้ำมันเครื่องเรืองแสงตรงกลาง <#F4F1DE>เพื่อเติมน้ำมัน");
      } else {
        TaskGuiHelper.sendTaskBar(player, "<#6EEB83>คลิกปุ่มมรกตด้านล่าง <#F4F1DE>เพื่อปิดฝาน้ำมันเครื่อง");
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
