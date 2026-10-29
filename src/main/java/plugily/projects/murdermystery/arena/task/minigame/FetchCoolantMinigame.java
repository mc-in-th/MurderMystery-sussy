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

public class FetchCoolantMinigame implements TaskMinigame {

  public static final String GUI_TITLE = HalloweenChat.color("<shadow:#000000FF><#5A4E7C>[</shadow><#FF7518>ตักน้ำหล่อเย็นหม้อน้ำ<shadow:#000000FF><#5A4E7C>]</shadow>");

  private static final int MAX_LEVEL = 5;
  private static final Color COOLANT_COLOR = Color.fromRGB(0, 210, 230);

  private static final int BANNER_SLOT = 4;
  private static final int[] GAUGE_SLOTS = {11, 12, 13, 14, 15};
  private static final int CAP_SLOT = 20;
  private static final int FUNNEL_SLOT = 22;
  private static final int BOTTLE_SLOT = 24;
  private static final int SEAL_BUTTON_SLOT = 31;

  private static class CoolantSession {
    boolean capOpen = false;
    int waterLevel = 1;
  }

  private final Arena arena;
  private final Map<UUID, CoolantSession> sessions = new HashMap<>();
  private final Set<UUID> completedSessions = new HashSet<>();

  public FetchCoolantMinigame(Arena arena) {
    this.arena = arena;
  }

  @Override
  public TaskType getTaskType() {
    return TaskType.FETCH_COOLANT_WATER;
  }

  @Override
  public String getGuiTitle() {
    return GUI_TITLE;
  }

  @Override
  public boolean isGui(String title) {
    return title != null && HalloweenChat.strip(title).contains("ตักน้ำหล่อเย็นหม้อน้ำ");
  }

  @Override
  public void open(Player player) {
    completedSessions.remove(player.getUniqueId());
    CoolantSession session = new CoolantSession();
    sessions.put(player.getUniqueId(), session);

    Inventory inv = Bukkit.createInventory(null, 36, GUI_TITLE);
    buildGui(inv, session);
    player.openInventory(inv);

    XSound.BLOCK_CHEST_OPEN.play(player.getLocation(), 0.8F, 1.2F);
    TaskGuiHelper.sendTaskBar(player, "<#F4F1DE>ขั้นที่ 1 คลิก <#FFB703>ฝาถังสำรองเรืองแสงทางซ้าย <#F4F1DE>เพื่อเปิดฝาถัง");
  }

  private void buildGui(Inventory inv, CoolantSession session) {
    TaskGuiHelper.fillBackground(inv);

    boolean full = session.waterLevel >= MAX_LEVEL;

    if(!session.capOpen) {
      inv.setItem(BANNER_SLOT, TaskGuiHelper.createItem(
        Material.BEACON,
        1,
        false,
        TaskGuiHelper.tag("<#FF7518>", "ขั้นที่ 1/3 - เปิดฝาถังน้ำสำรองทางฝั่งซ้าย"),
        "",
        "<#F4F1DE>1. คลิก <#FFB703>ฝาถังสำรองเรืองแสงทางซ้าย <#F4F1DE>เพื่อเปิดช่องรับน้ำ",
        "<#B8B0C8>2. ตักน้ำหล่อเย็นทางขวาเข้าถังให้เต็ม 5/5",
        "<#B8B0C8>3. ปิดฝาถังสำรองด้านล่างให้แน่น",
        "",
        TaskGuiHelper.hint("<#72EFDD>", "Visual Task - ผู้เล่นรอบข้างจะเห็นละอองน้ำยืนยันตัวตน")
      ));
    } else if(!full) {
      inv.setItem(BANNER_SLOT, TaskGuiHelper.createItem(
        Material.BEACON,
        2,
        false,
        TaskGuiHelper.tag("<#72EFDD>", "ขั้นที่ 2/3 - ตักน้ำหล่อเย็นเข้าถัง (" + session.waterLevel + "/" + MAX_LEVEL + ")"),
        "",
        "<#6EEB83>1. เปิดฝาถังสำรองแล้ว",
        "<#F4F1DE>2. คลิก <#FFB703>ขวดตักน้ำเรืองแสงทางขวา <#F4F1DE>จนเต็ม 5/5",
        "<#B8B0C8>3. ปิดฝาถังสำรองด้านล่างให้แน่น",
        "",
        TaskGuiHelper.hint("<#72EFDD>", "Visual Task - ผู้เล่นรอบข้างจะเห็นละอองน้ำยืนยันตัวตน")
      ));
    } else {
      inv.setItem(BANNER_SLOT, TaskGuiHelper.createItem(
        Material.BEACON,
        3,
        false,
        TaskGuiHelper.tag("<#6EEB83>", "ขั้นที่ 3/3 - ปิดฝาถังสำรองด้านล่าง"),
        "",
        "<#6EEB83>1. เปิดฝาถังสำรองแล้ว",
        "<#6EEB83>2. ตักน้ำหล่อเย็นเต็ม 5/5 แล้ว",
        "<#F4F1DE>3. คลิก <#6EEB83>ปุ่มมรกตเรืองแสงด้านล่าง <#F4F1DE>เพื่อปิดฝาถังให้แน่น"
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
          TaskGuiHelper.tag("<#6EEB83>", "ระดับน้ำหล่อเย็น " + mark + "/5 - เต็มถังสำรอง"),
          "",
          "<#6EEB83>พร้อมนำไปเติมหม้อน้ำรถบัส"
        );
      } else if(i < session.waterLevel) {
        XMaterial mat = (session.waterLevel == 1) ? XMaterial.RED_STAINED_GLASS_PANE : XMaterial.LIGHT_BLUE_STAINED_GLASS_PANE;
        String col = (session.waterLevel == 1) ? "<#FF3B47>" : "<#72EFDD>";
        bar = TaskGuiHelper.createItem(
          mat,
          1,
          false,
          TaskGuiHelper.tag(col, "ระดับน้ำหล่อเย็น " + mark + "/5"),
          "",
          session.waterLevel == 1 ? "<#FF3B47>น้ำในถังสำรองเกือบแห้ง" : "<#72EFDD>กำลังตักน้ำหล่อเย็นเข้าถัง..."
        );
      } else if(i == MAX_LEVEL - 1) {
        bar = TaskGuiHelper.createItem(
          XMaterial.CYAN_STAINED_GLASS_PANE,
          1,
          false,
          TaskGuiHelper.tag("<#72EFDD>", "ขีดความจุถังสำรอง (5/5)"),
          "",
          TaskGuiHelper.hint("<#B8B0C8>", "ตักน้ำหล่อเย็นให้ถึงขีดนี้")
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
        TaskGuiHelper.doneName("เปิดฝาถังสำรองแล้ว"),
        "",
        TaskGuiHelper.hint("<#B8B0C8>", full ? "กดปุ่มมรกตด้านล่างเพื่อปิดฝาถัง" : "คลิกขวดตักน้ำเรืองแสงทางขวาต่อได้เลย")
      ));
    } else {
      inv.setItem(CAP_SLOT, TaskGuiHelper.createItem(
        XMaterial.CAULDRON,
        1,
        true,
        TaskGuiHelper.clickName("เปิดฝาถังสำรอง (ขั้นที่ 1)"),
        "",
        "<#F4F1DE>คลายฝาถังพักน้ำก่อนตักน้ำหล่อเย็นเข้าถัง",
        "",
        TaskGuiHelper.hint("<#FFB703>", "กดคลิกที่นี่เป็นอันดับแรก")
      ));
    }

    Material funnelMat = full ? Material.WATER_BUCKET : (session.capOpen ? Material.BUCKET : Material.CAULDRON);
    inv.setItem(FUNNEL_SLOT, TaskGuiHelper.createItem(
      funnelMat,
      1,
      false,
      TaskGuiHelper.tag(full ? "<#6EEB83>" : "<#72EFDD>", "ถังน้ำหล่อเย็นสำรองรถบัส"),
      "",
      "<#F4F1DE>สถานะ " + (!session.capOpen ? "<#FF3B47>ฝาถังยังปิดอยู่" : (full ? "<#6EEB83>น้ำเต็มถัง 5/5" : "<#FFB703>กำลังตักเติม (" + session.waterLevel + "/5)"))
    ));

    if(!session.capOpen) {
      inv.setItem(BOTTLE_SLOT, TaskGuiHelper.createColoredPotion(
        COOLANT_COLOR,
        false,
        TaskGuiHelper.lockedName("ขวดตักน้ำหล่อเย็น (ขั้นที่ 2)"),
        "",
        TaskGuiHelper.hint("<#FF3B47>", "เปิดฝาถังสำรองทางฝั่งซ้ายก่อน")
      ));
    } else if(!full) {
      inv.setItem(BOTTLE_SLOT, TaskGuiHelper.createColoredPotion(
        COOLANT_COLOR,
        true,
        TaskGuiHelper.clickName("ตักน้ำหล่อเย็น +1 ระดับ (ขั้นที่ 2)"),
        "",
        "<#F4F1DE>ระดับน้ำ <#72EFDD>" + session.waterLevel + "/5 <shadow:#000000FF><#5A4E7C>-></shadow> <#6EEB83>" + (session.waterLevel + 1) + "/5",
        "",
        TaskGuiHelper.hint("<#FFB703>", "กดคลิกที่นี่จนเต็ม 5/5")
      ));
    } else {
      inv.setItem(BOTTLE_SLOT, TaskGuiHelper.createItem(
        Material.GLASS_BOTTLE,
        1,
        false,
        TaskGuiHelper.doneName("ตักน้ำหล่อเย็นครบ 5/5 แล้ว"),
        "",
        TaskGuiHelper.hint("<#6EEB83>", "กดปุ่มมรกตด้านล่างเพื่อปิดฝาถังสำรอง")
      ));
    }

    if(session.capOpen && full) {
      inv.setItem(SEAL_BUTTON_SLOT, TaskGuiHelper.createItem(
        XMaterial.EMERALD_BLOCK,
        1,
        true,
        TaskGuiHelper.confirmName("ปิดฝาถังสำรองให้แน่น (ขั้นที่ 3)"),
        "",
        "<#F4F1DE>น้ำหล่อเย็นเต็มความจุถัง <#6EEB83>5/5",
        "",
        TaskGuiHelper.hint("<#6EEB83>", "กดคลิกที่นี่เพื่อเสร็จสิ้นภารกิจ")
      ));
    } else {
      inv.setItem(SEAL_BUTTON_SLOT, TaskGuiHelper.createItem(
        XMaterial.IRON_BARS,
        1,
        false,
        TaskGuiHelper.lockedName("ปิดฝาถังสำรอง (ขั้นที่ 3)"),
        "",
        !session.capOpen
          ? "<#FF3B47>ต้องเปิดฝาถังทางซ้ายและตักน้ำก่อน"
          : "<#FF3B47>ต้องตักน้ำหล่อเย็นทางขวาให้ครบ 5/5 ก่อน"
      ));
    }
  }

  @Override
  public boolean handleClick(Player player, int rawSlot, ClickType clickType) {
    if(completedSessions.contains(player.getUniqueId())) {
      return true;
    }

    CoolantSession session = sessions.get(player.getUniqueId());
    if(session == null) {
      return true;
    }

    Inventory topInv = player.getOpenInventory().getTopInventory();

    if(rawSlot == CAP_SLOT) {
      if(!session.capOpen) {
        session.capOpen = true;
        XSound.BLOCK_IRON_TRAPDOOR_OPEN.play(player.getLocation(), 1F, 1.2F);
        XSound.BLOCK_FIRE_EXTINGUISH.play(player.getLocation(), 0.8F, 1.5F);
        arena.getTaskManager().getVisualEffectManager().triggerStepEffect(player, TaskType.FETCH_COOLANT_WATER, 1, MAX_LEVEL);
        buildGui(topInv, session);
        TaskGuiHelper.sendTaskBar(player, "<#6EEB83>เปิดฝาถังสำรองแล้ว <#FFB703>คลิกขวดตักน้ำเรืองแสงทางขวาเพื่อตักเติม");
      } else {
        XSound.UI_BUTTON_CLICK.play(player.getLocation(), 0.5F, 1.2F);
        TaskGuiHelper.sendTaskBar(player, session.waterLevel < MAX_LEVEL
          ? "<#FFB703>คลิกขวดตักน้ำเรืองแสงทางขวา <#F4F1DE>เพื่อตักน้ำหล่อเย็น"
          : "<#6EEB83>คลิกปุ่มมรกตด้านล่าง <#F4F1DE>เพื่อปิดฝาถังสำรอง");
      }
      return true;
    }

    if(rawSlot == BOTTLE_SLOT) {
      if(!session.capOpen) {
        XSound.ENTITY_VILLAGER_NO.play(player.getLocation(), 0.85F, 1F);
        TaskGuiHelper.sendErrorBar(player, "คลิกเปิดฝาถังสำรองเรืองแสงทางซ้ายก่อน");
        return true;
      }

      if(session.waterLevel < MAX_LEVEL) {
        session.waterLevel++;
        XSound.ITEM_BUCKET_EMPTY.play(player.getLocation(), 0.9F, 1.2F);
        XSound.BLOCK_WATER_AMBIENT.play(player.getLocation(), 0.8F, 1.5F);
        arena.getTaskManager().getVisualEffectManager().triggerStepEffect(player, TaskType.FETCH_COOLANT_WATER, session.waterLevel, MAX_LEVEL);
        buildGui(topInv, session);

        if(session.waterLevel >= MAX_LEVEL) {
          XSound.BLOCK_NOTE_BLOCK_PLING.play(player.getLocation(), 1F, 1.8F);
          TaskGuiHelper.sendTaskBar(player, "<#6EEB83>น้ำเต็มถัง 5/5 แล้ว <#FFB703>คลิกปุ่มมรกตด้านล่างเพื่อปิดฝาถังสำรอง");
        } else {
          TaskGuiHelper.sendTaskBar(player, "<#F4F1DE>ตักน้ำหล่อเย็นแล้ว <#72EFDD>" + session.waterLevel + "/" + MAX_LEVEL);
        }
      } else {
        XSound.UI_BUTTON_CLICK.play(player.getLocation(), 0.5F, 1.2F);
        TaskGuiHelper.sendTaskBar(player, "<#6EEB83>คลิกปุ่มมรกตด้านล่าง <#F4F1DE>เพื่อปิดฝาถังสำรอง");
      }
      return true;
    }

    if(rawSlot == SEAL_BUTTON_SLOT) {
      if(session.capOpen && session.waterLevel >= MAX_LEVEL) {
        completedSessions.add(player.getUniqueId());
        sessions.remove(player.getUniqueId());

        XSound.BLOCK_ANVIL_USE.play(player.getLocation(), 0.8F, 1.5F);
        XSound.ENTITY_PLAYER_LEVELUP.play(player.getLocation(), 1F, 1.5F);
        TaskGuiHelper.sendSuccessBar(player, "ตักน้ำหล่อเย็นหม้อน้ำสำเร็จ");

        Bukkit.getScheduler().runTaskLater(arena.getPlugin(), () -> {
          if(player.isOnline() && arena.getPlayers().contains(player) && !arena.getVotingManager().isVotingActive()) {
            if(isGui(player.getOpenInventory().getTitle())) {
              player.closeInventory();
            }
            arena.getTaskManager().completeTaskFromMinigame(player, TaskType.FETCH_COOLANT_WATER);
          }
        }, 8L);
      } else {
        XSound.ENTITY_VILLAGER_NO.play(player.getLocation(), 0.9F, 1F);
        TaskGuiHelper.sendErrorBar(player, !session.capOpen ? "คลิกเปิดฝาถังสำรองทางซ้ายก่อน" : "คลิกขวดตักน้ำทางขวาให้ครบ 5/5 ก่อน");
      }
      return true;
    }

    if(rawSlot >= 0 && rawSlot < 36) {
      XSound.UI_BUTTON_CLICK.play(player.getLocation(), 0.45F, 1.0F);
      if(!session.capOpen) {
        TaskGuiHelper.sendTaskBar(player, "<#FFB703>ขั้นที่ 1 คลิกฝาถังสำรองเรืองแสงทางซ้าย");
      } else if(session.waterLevel < MAX_LEVEL) {
        TaskGuiHelper.sendTaskBar(player, "<#FFB703>ขั้นที่ 2 คลิกขวดตักน้ำเรืองแสงทางขวา");
      } else {
        TaskGuiHelper.sendTaskBar(player, "<#6EEB83>ขั้นที่ 3 คลิกปุ่มมรกตด้านล่างเพื่อปิดฝาถัง");
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
