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
import org.bukkit.Color;
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

public class RadiatorWaterMinigame implements TaskMinigame {

  public static final String GUI_TITLE = HalloweenChat.color("<shadow:#000000FF><#5A4E7C>[</shadow><#FF7518>เช็คระดับน้ำในหม้อน้ำ<shadow:#000000FF><#5A4E7C>]</shadow>");

  private static final int MAX_LEVEL = 5;
  private static final Color COOLANT_COLOR = Color.fromRGB(0, 210, 230);

  private static final int BANNER_SLOT = 4;
  private static final int[] GAUGE_SLOTS = {11, 12, 13, 14, 15};
  private static final int CAP_SLOT = 20;
  private static final int FUNNEL_SLOT = 22;
  private static final int BOTTLE_SLOT = 24;
  private static final int SEAL_BUTTON_SLOT = 31;

  private static class RadiatorSession {
    boolean capOpen = false;
    int waterLevel = 1;
  }

  private final Arena arena;
  private final Map<UUID, RadiatorSession> sessions = new HashMap<>();
  private final Set<UUID> completedSessions = new HashSet<>();

  public RadiatorWaterMinigame(Arena arena) {
    this.arena = arena;
  }

  @Override
  public TaskType getTaskType() {
    return TaskType.CHECK_RADIATOR_WATER;
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
    return plain.contains("เช็คระดับน้ำในหม้อน้ำ") || plain.contains("Radiator");
  }

  @Override
  public void open(Player player) {
    completedSessions.remove(player.getUniqueId());
    RadiatorSession session = new RadiatorSession();
    sessions.put(player.getUniqueId(), session);

    Inventory inv = Bukkit.createInventory(null, 36, GUI_TITLE);
    buildGui(inv, session);
    player.openInventory(inv);

    TaskGuiHelper.playSound(player, XSound.BLOCK_CHEST_OPEN, 0.8F, 1.2F);
    TaskGuiHelper.sendTaskBar(player, "<#F4F1DE>ขั้นที่ 1 คลิก <#FFB703>ฝาหม้อน้ำเรืองแสงทางซ้าย <#F4F1DE>เพื่อเปิดฝา");
  }

  private void buildGui(Inventory inv, RadiatorSession session) {
    TaskGuiHelper.fillBackground(inv);

    boolean full = session.waterLevel >= MAX_LEVEL;

    if(!session.capOpen) {
      inv.setItem(BANNER_SLOT, TaskGuiHelper.createItem(
        Material.BEACON,
        1,
        false,
        TaskGuiHelper.tag("<#FF7518>", "ขั้นที่ 1/3 - เปิดฝาหม้อน้ำทางฝั่งซ้าย"),
        "",
        "<#F4F1DE>1. คลิก <#FFB703>ฝาหม้อน้ำเรืองแสงทางซ้าย <#F4F1DE>เพื่อระบายไอร้อน",
        "<#B8B0C8>2. เทน้ำยาหล่อเย็นทางขวาให้เต็ม 5/5",
        "<#B8B0C8>3. ปิดฝาหม้อน้ำด้านล่าง",
        "",
        TaskGuiHelper.hint("<#72EFDD>", "Visual Task - ผู้เล่นรอบข้างจะเห็นไอน้ำยืนยันตัวตน")
      ));
    } else if(!full) {
      inv.setItem(BANNER_SLOT, TaskGuiHelper.createItem(
        Material.BEACON,
        2,
        false,
        TaskGuiHelper.tag("<#72EFDD>", "ขั้นที่ 2/3 - เทน้ำยาหล่อเย็น (" + session.waterLevel + "/" + MAX_LEVEL + ")"),
        "",
        "<#6EEB83>1. เปิดฝาหม้อน้ำแล้ว",
        "<#F4F1DE>2. คลิก <#FFB703>ขวดน้ำยาเรืองแสงทางขวา <#F4F1DE>จนเต็ม 5/5",
        "<#B8B0C8>3. ปิดฝาหม้อน้ำด้านล่าง",
        "",
        TaskGuiHelper.hint("<#72EFDD>", "Visual Task - ผู้เล่นรอบข้างจะเห็นไอน้ำยืนยันตัวตน")
      ));
    } else {
      inv.setItem(BANNER_SLOT, TaskGuiHelper.createItem(
        Material.BEACON,
        3,
        false,
        TaskGuiHelper.tag("<#6EEB83>", "ขั้นที่ 3/3 - ปิดฝาหม้อน้ำด้านล่าง"),
        "",
        "<#6EEB83>1. เปิดฝาหม้อน้ำแล้ว",
        "<#6EEB83>2. เติมน้ำยาหล่อเย็นเต็ม 5/5 แล้ว",
        "<#F4F1DE>3. คลิก <#6EEB83>ปุ่มมรกตเรืองแสงด้านล่าง <#F4F1DE>เพื่อปิดฝาให้แน่น"
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
          TaskGuiHelper.tag("<#6EEB83>", "ระดับน้ำยาหล่อเย็น " + mark + "/5 - เต็มมาตรฐาน"),
          "",
          "<#6EEB83>ระบบระบายความร้อนพร้อมใช้งาน"
        );
      } else if(i < session.waterLevel) {
        XMaterial mat = (session.waterLevel == 1) ? XMaterial.RED_STAINED_GLASS_PANE : XMaterial.LIGHT_BLUE_STAINED_GLASS_PANE;
        String col = (session.waterLevel == 1) ? "<#FF3B47>" : "<#72EFDD>";
        bar = TaskGuiHelper.createItem(
          mat,
          1,
          false,
          TaskGuiHelper.tag(col, "ระดับน้ำยาหล่อเย็น " + mark + "/5"),
          "",
          session.waterLevel == 1 ? "<#FF3B47>น้ำแห้ง หม้อน้ำร้อนจัด" : "<#72EFDD>กำลังเติมน้ำยาหล่อเย็น..."
        );
      } else if(i == MAX_LEVEL - 1) {
        bar = TaskGuiHelper.createItem(
          XMaterial.CYAN_STAINED_GLASS_PANE,
          1,
          false,
          TaskGuiHelper.tag("<#72EFDD>", "ขีดเป้าหมายมาตรฐาน (5/5)"),
          "",
          TaskGuiHelper.hint("<#B8B0C8>", "เติมน้ำยาหล่อเย็นให้ถึงขีดนี้")
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

    if(session.capOpen) {
      inv.setItem(CAP_SLOT, TaskGuiHelper.createItem(
        XMaterial.BREWING_STAND,
        1,
        false,
        TaskGuiHelper.doneName("เปิดฝาหม้อน้ำแล้ว"),
        "",
        TaskGuiHelper.hint("<#B8B0C8>", full ? "กดปุ่มมรกตด้านล่างเพื่อปิดฝา" : "คลิกขวดน้ำยาเรืองแสงทางขวาต่อได้เลย")
      ));
    } else {
      inv.setItem(CAP_SLOT, TaskGuiHelper.createItem(
        XMaterial.CAULDRON,
        1,
        true,
        TaskGuiHelper.clickName("เปิดฝาหม้อน้ำ (ขั้นที่ 1)"),
        "",
        "<#F4F1DE>คลายฝาเพื่อระบายแรงดันไอร้อนก่อนเติมน้ำยา",
        "",
        TaskGuiHelper.hint("<#FFB703>", "กดคลิกที่นี่เป็นอันดับแรก")
      ));
    }

    Material funnelMat = full ? Material.WATER_BUCKET : (session.capOpen ? Material.BUCKET : Material.MAGMA_BLOCK);
    inv.setItem(FUNNEL_SLOT, TaskGuiHelper.createItem(
      funnelMat,
      1,
      false,
      TaskGuiHelper.tag(full ? "<#6EEB83>" : "<#72EFDD>", "ช่องพักน้ำหม้อน้ำรถบัส"),
      "",
      "<#F4F1DE>สถานะ " + (!session.capOpen ? "<#FF3B47>ฝายังปิดอยู่" : (full ? "<#6EEB83>น้ำยาเต็ม 5/5" : "<#FFB703>กำลังเติม (" + session.waterLevel + "/5)"))
    ));

    if(!session.capOpen) {
      inv.setItem(BOTTLE_SLOT, TaskGuiHelper.createColoredPotion(
        COOLANT_COLOR,
        false,
        TaskGuiHelper.lockedName("ขวดน้ำยาหล่อเย็น (ขั้นที่ 2)"),
        "",
        TaskGuiHelper.hint("<#FF3B47>", "เปิดฝาหม้อน้ำทางฝั่งซ้ายก่อน")
      ));
    } else if(!full) {
      inv.setItem(BOTTLE_SLOT, TaskGuiHelper.createColoredPotion(
        COOLANT_COLOR,
        true,
        TaskGuiHelper.clickName("เทน้ำยาหล่อเย็น +1 ระดับ (ขั้นที่ 2)"),
        "",
        "<#F4F1DE>ระดับน้ำยา <#72EFDD>" + session.waterLevel + "/5 <shadow:#000000FF><#5A4E7C>-></shadow> <#6EEB83>" + (session.waterLevel + 1) + "/5",
        "",
        TaskGuiHelper.hint("<#FFB703>", "กดคลิกที่นี่จนเต็ม 5/5")
      ));
    } else {
      inv.setItem(BOTTLE_SLOT, TaskGuiHelper.createItem(
        Material.GLASS_BOTTLE,
        1,
        false,
        TaskGuiHelper.doneName("เทน้ำยาหล่อเย็นครบ 5/5 แล้ว"),
        "",
        TaskGuiHelper.hint("<#6EEB83>", "กดปุ่มมรกตด้านล่างเพื่อปิดฝาหม้อน้ำ")
      ));
    }

    if(session.capOpen && full) {
      inv.setItem(SEAL_BUTTON_SLOT, TaskGuiHelper.createItem(
        XMaterial.EMERALD_BLOCK,
        1,
        true,
        TaskGuiHelper.confirmName("ปิดฝาหม้อน้ำให้แน่น (ขั้นที่ 3)"),
        "",
        "<#F4F1DE>น้ำยาหล่อเย็นเต็มขีดมาตรฐาน <#6EEB83>5/5",
        "",
        TaskGuiHelper.hint("<#6EEB83>", "กดคลิกที่นี่เพื่อเสร็จสิ้นภารกิจ")
      ));
    } else {
      inv.setItem(SEAL_BUTTON_SLOT, TaskGuiHelper.createItem(
        XMaterial.IRON_BARS,
        1,
        false,
        TaskGuiHelper.lockedName("ปิดฝาหม้อน้ำ (ขั้นที่ 3)"),
        "",
        !session.capOpen
          ? "<#FF3B47>ต้องเปิดฝาทางซ้ายและเติมน้ำยาก่อน"
          : "<#FF3B47>ต้องเทน้ำยาทางขวาให้ครบ 5/5 ก่อน"
      ));
    }
  }

  @Override
  public boolean handleClick(Player player, int rawSlot, ClickType clickType) {
    if(completedSessions.contains(player.getUniqueId())) {
      return true;
    }

    RadiatorSession session = sessions.get(player.getUniqueId());
    if(session == null) {
      return true;
    }

    Inventory topInv = player.getOpenInventory().getTopInventory();

    if(rawSlot == CAP_SLOT) {
      if(!session.capOpen) {
        session.capOpen = true;
        TaskGuiHelper.playSound(player, XSound.BLOCK_IRON_TRAPDOOR_OPEN, 1F, 1.2F);
        TaskGuiHelper.playSound(player, XSound.BLOCK_FIRE_EXTINGUISH, 0.8F, 1.5F);
        arena.getTaskManager().getVisualEffectManager().triggerStepEffect(player, TaskType.CHECK_RADIATOR_WATER, 1, MAX_LEVEL);
        buildGui(topInv, session);
        TaskGuiHelper.sendTaskBar(player, "<#6EEB83>เปิดฝาหม้อน้ำแล้ว <#FFB703>คลิกขวดน้ำยาเรืองแสงทางขวาเพื่อเติม");
      } else {
        TaskGuiHelper.playSound(player, XSound.UI_BUTTON_CLICK, 0.5F, 1.2F);
        TaskGuiHelper.sendTaskBar(player, session.waterLevel < MAX_LEVEL
          ? "<#FFB703>คลิกขวดน้ำยาเรืองแสงทางขวา <#F4F1DE>เพื่อเติมน้ำยา"
          : "<#6EEB83>คลิกปุ่มมรกตด้านล่าง <#F4F1DE>เพื่อปิดฝาหม้อน้ำ");
      }
      return true;
    }

    if(rawSlot == BOTTLE_SLOT) {
      if(!session.capOpen) {
        TaskGuiHelper.playSound(player, XSound.ENTITY_VILLAGER_NO, 0.85F, 1F);
        TaskGuiHelper.sendErrorBar(player, "คลิกเปิดฝาหม้อน้ำเรืองแสงทางซ้ายก่อน");
        return true;
      }

      if(session.waterLevel < MAX_LEVEL) {
        session.waterLevel++;
        TaskGuiHelper.playSound(player, XSound.ITEM_BUCKET_EMPTY, 0.9F, 1.2F);
        TaskGuiHelper.playSound(player, XSound.BLOCK_WATER_AMBIENT, 0.8F, 1.5F);
        arena.getTaskManager().getVisualEffectManager().triggerStepEffect(player, TaskType.CHECK_RADIATOR_WATER, session.waterLevel, MAX_LEVEL);
        buildGui(topInv, session);

        if(session.waterLevel >= MAX_LEVEL) {
          TaskGuiHelper.playSound(player, XSound.BLOCK_NOTE_BLOCK_PLING, 1F, 1.8F);
          TaskGuiHelper.sendTaskBar(player, "<#6EEB83>น้ำยาเต็ม 5/5 แล้ว <#FFB703>คลิกปุ่มมรกตด้านล่างเพื่อปิดฝาหม้อน้ำ");
        } else {
          TaskGuiHelper.sendTaskBar(player, "<#F4F1DE>เติมน้ำยาหล่อเย็นแล้ว <#72EFDD>" + session.waterLevel + "/" + MAX_LEVEL);
        }
      } else {
        TaskGuiHelper.playSound(player, XSound.UI_BUTTON_CLICK, 0.5F, 1.2F);
        TaskGuiHelper.sendTaskBar(player, "<#6EEB83>คลิกปุ่มมรกตด้านล่าง <#F4F1DE>เพื่อปิดฝาหม้อน้ำ");
      }
      return true;
    }

    if(rawSlot == SEAL_BUTTON_SLOT) {
      if(session.capOpen && session.waterLevel >= MAX_LEVEL) {
        completedSessions.add(player.getUniqueId());
        sessions.remove(player.getUniqueId());

        TaskGuiHelper.playSound(player, XSound.BLOCK_ANVIL_USE, 0.8F, 1.5F);
        TaskGuiHelper.playSound(player, XSound.ENTITY_PLAYER_LEVELUP, 1F, 1.5F);
        TaskGuiHelper.sendSuccessBar(player, "เช็คระดับน้ำในหม้อน้ำสำเร็จ");

        Bukkit.getScheduler().runTaskLater(arena.getPlugin(), () -> {
          if(player.isOnline() && arena.getPlayers().contains(player) && !arena.getVotingManager().isVotingActive()) {
            if(isGui(player.getOpenInventory().getTitle())) {
              player.closeInventory();
            }
            arena.getTaskManager().completeTaskFromMinigame(player, TaskType.CHECK_RADIATOR_WATER);
          }
        }, 8L);
      } else {
        TaskGuiHelper.playSound(player, XSound.ENTITY_VILLAGER_NO, 0.9F, 1F);
        TaskGuiHelper.sendErrorBar(player, !session.capOpen ? "คลิกเปิดฝาหม้อน้ำทางซ้ายก่อน" : "คลิกขวดน้ำยาทางขวาให้ครบ 5/5 ก่อน");
      }
      return true;
    }

    if(rawSlot >= 0 && rawSlot < 36) {
      TaskGuiHelper.playSound(player, XSound.UI_BUTTON_CLICK, 0.45F, 1.0F);
      if(!session.capOpen) {
        TaskGuiHelper.sendTaskBar(player, "<#FFB703>ขั้นที่ 1 คลิกฝาหม้อน้ำเรืองแสงทางซ้าย");
      } else if(session.waterLevel < MAX_LEVEL) {
        TaskGuiHelper.sendTaskBar(player, "<#FFB703>ขั้นที่ 2 คลิกขวดน้ำยาเรืองแสงทางขวา");
      } else {
        TaskGuiHelper.sendTaskBar(player, "<#6EEB83>ขั้นที่ 3 คลิกปุ่มมรกตด้านล่างเพื่อปิดฝา");
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
