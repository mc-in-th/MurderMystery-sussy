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

public class DieselFuelMinigame implements TaskMinigame {

  public static final String GUI_TITLE = HalloweenChat.color("<shadow:#000000FF><#5A4E7C>[</shadow><#FF7518>สถานีเติมน้ำมันดีเซล<shadow:#000000FF><#5A4E7C>]</shadow>");

  private static class FuelSession {
    int liters = 0;
  }

  private final Arena arena;
  private final Map<UUID, FuelSession> sessions = new HashMap<>();
  private final Set<UUID> completedSessions = new HashSet<>();

  private static final int BANNER_SLOT = 4;
  private static final int[] GAUGE_SLOTS = {11, 12, 13, 14, 15};
  private static final int NOZZLE_SLOT = 20;
  private static final int PIPE_SLOT = 22;
  private static final int PUMP_LEVER_SLOT = 24;
  private static final int SEAL_CAP_SLOT = 31;

  public DieselFuelMinigame(Arena arena) {
    this.arena = arena;
  }

  @Override
  public TaskType getTaskType() {
    return TaskType.FIND_DIESEL_FUEL;
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
    return plain.contains("น้ำมันดีเซล") || plain.contains("Diesel");
  }

  @Override
  public void open(Player player) {
    completedSessions.remove(player.getUniqueId());
    FuelSession session = new FuelSession();
    sessions.put(player.getUniqueId(), session);

    Inventory inv = Bukkit.createInventory(null, 36, GUI_TITLE);
    buildGui(inv, session);
    player.openInventory(inv);

    TaskGuiHelper.playSound(player, XSound.BLOCK_CHEST_OPEN, 0.8F, 1.2F);
    TaskGuiHelper.sendTaskBar(player, "<#F4F1DE>คลิก <#FFB703>คันโยกเรืองแสงทางขวา <#F4F1DE>เพื่อปั๊มน้ำมันดีเซลให้เต็ม <#6EEB83>100 ลิตร");
  }

  private void buildGui(Inventory inv, FuelSession session) {
    TaskGuiHelper.fillBackground(inv);

    boolean full = session.liters >= 100;
    int filledBars = Math.min(5, session.liters / 20);

    if(full) {
      inv.setItem(BANNER_SLOT, TaskGuiHelper.createItem(
        Material.HOPPER,
        5,
        false,
        TaskGuiHelper.tag("<#6EEB83>", "ขั้นที่ 2/2 - น้ำมันเต็ม 100 ลิตรแล้ว"),
        "",
        "<#F4F1DE>คลิกปุ่ม <#6EEB83>มรกตเรืองแสงด้านล่าง <#F4F1DE>เพื่อปิดฝาถังน้ำมัน"
      ));
    } else {
      inv.setItem(BANNER_SLOT, TaskGuiHelper.createItem(
        Material.HOPPER,
        Math.max(1, filledBars),
        false,
        TaskGuiHelper.tag("<#FF7518>", "ขั้นที่ 1/2 - ปั๊มน้ำมันดีเซล (" + session.liters + "/100 ลิตร)"),
        "",
        "<#F4F1DE>คลิก <#FFB703>คันโยกเรืองแสงทางขวา <#F4F1DE>เพื่อสูบน้ำมันครั้งละ <#6EEB83>+20 ลิตร",
        "<#F4F1DE>เมื่อครบ <#6EEB83>100 ลิตร <#F4F1DE>จึงกดปิดฝาถังด้านล่าง"
      ));
    }

    for(int i = 0; i < GAUGE_SLOTS.length; i++) {
      int barLiters = (i + 1) * 20;
      ItemStack bar;
      if(i < filledBars) {
        bar = TaskGuiHelper.createItem(
          full ? XMaterial.LIME_STAINED_GLASS_PANE : XMaterial.ORANGE_STAINED_GLASS_PANE,
          1,
          false,
          TaskGuiHelper.tag(full ? "<#6EEB83>" : "<#FFB703>", "ระดับน้ำมัน " + barLiters + "/100 ลิตร"),
          "",
          "<#6EEB83>สูบน้ำมันเข้าถังแล้ว"
        );
      } else {
        bar = TaskGuiHelper.createItem(
          XMaterial.GRAY_STAINED_GLASS_PANE,
          1,
          false,
          TaskGuiHelper.tag("<#B8B0C8>", "ขีดวัดระดับ " + barLiters + " ลิตร"),
          "",
          "<#B8B0C8>ยังว่างอยู่"
        );
      }
      inv.setItem(GAUGE_SLOTS[i], bar);
    }

    inv.setItem(NOZZLE_SLOT, TaskGuiHelper.createItem(
      full ? Material.LAVA_BUCKET : Material.BUCKET,
      1,
      false,
      TaskGuiHelper.tag(full ? "<#6EEB83>" : "<#FF7518>", "ถังน้ำมันดีเซลรถบัส"),
      "",
      "<#F4F1DE>ปริมาณสะสม <#FFB703>" + session.liters + "<shadow:#000000FF><#5A4E7C>/</shadow><#6EEB83>100 ลิตร",
      "<#F4F1DE>เกจวัด " + TaskGuiHelper.buildPipeBar(session.liters, 100, 10)
    ));

    XMaterial pipeMat = full
      ? XMaterial.LIME_STAINED_GLASS_PANE
      : (session.liters > 0 ? XMaterial.ORANGE_STAINED_GLASS_PANE : XMaterial.GRAY_STAINED_GLASS_PANE);
    inv.setItem(PIPE_SLOT, TaskGuiHelper.createItem(
      pipeMat,
      1,
      false,
      TaskGuiHelper.tag("<#B8B0C8>", "ท่อส่งน้ำมันเชื้อเพลิง"),
      "",
      full ? "<#6EEB83>จ่ายน้ำมันครบจำนวนแล้ว" : "<#B8B0C8>ส่งน้ำมันจากหัวจ่ายเข้าสู่ถัง"
    ));

    if(full) {
      inv.setItem(PUMP_LEVER_SLOT, TaskGuiHelper.createItem(
        XMaterial.TRIPWIRE_HOOK,
        1,
        false,
        TaskGuiHelper.doneName("สูบน้ำมันครบ 100 ลิตรแล้ว"),
        "",
        TaskGuiHelper.hint("<#6EEB83>", "กดปุ่มมรกตด้านล่างเพื่อปิดฝาถังได้เลย")
      ));
    } else {
      inv.setItem(PUMP_LEVER_SLOT, TaskGuiHelper.createItem(
        XMaterial.TRIPWIRE_HOOK,
        1,
        true,
        TaskGuiHelper.clickName("ปั๊มน้ำมันดีเซล (+20 ลิตร)"),
        "",
        "<#F4F1DE>ปัจจุบัน <#FFB703>" + session.liters + " ลิตร <shadow:#000000FF><#5A4E7C>-></shadow> <#6EEB83>" + Math.min(100, session.liters + 20) + " ลิตร",
        "",
        TaskGuiHelper.hint("<#FFB703>", "กดคลิกที่นี่เพื่อสูบน้ำมัน")
      ));
    }

    if(full) {
      inv.setItem(SEAL_CAP_SLOT, TaskGuiHelper.createItem(
        XMaterial.EMERALD_BLOCK,
        1,
        true,
        TaskGuiHelper.confirmName("ปิดฝาถังน้ำมันและล็อก"),
        "",
        "<#F4F1DE>น้ำมันดีเซลเต็มถัง <#6EEB83>100 ลิตร",
        "",
        TaskGuiHelper.hint("<#6EEB83>", "กดคลิกที่นี่เพื่อเสร็จสิ้นภารกิจ")
      ));
    } else {
      inv.setItem(SEAL_CAP_SLOT, TaskGuiHelper.createItem(
        XMaterial.IRON_BARS,
        1,
        false,
        TaskGuiHelper.lockedName("ปิดฝาถังน้ำมัน"),
        "",
        "<#FF3B47>ต้องปั๊มน้ำมันให้ครบ 100 ลิตรก่อน",
        "",
        TaskGuiHelper.hint("<#B8B0C8>", "ยังขาดอีก " + (100 - session.liters) + " ลิตร")
      ));
    }
  }

  @Override
  public boolean handleClick(Player player, int rawSlot, ClickType clickType) {
    if(completedSessions.contains(player.getUniqueId())) {
      return true;
    }

    FuelSession session = sessions.get(player.getUniqueId());
    if(session == null) {
      return true;
    }

    Inventory topInv = player.getOpenInventory().getTopInventory();

    if(rawSlot == PUMP_LEVER_SLOT) {
      if(session.liters < 100) {
        session.liters = Math.min(100, session.liters + 20);
        TaskGuiHelper.playSound(player, XSound.BLOCK_PISTON_EXTEND, 0.8F, 1.3F);
        TaskGuiHelper.playSound(player, XSound.ITEM_BUCKET_FILL, 0.75F, 0.9F + (session.liters / 200.0F));
        buildGui(topInv, session);

        if(session.liters >= 100) {
          TaskGuiHelper.playSound(player, XSound.BLOCK_NOTE_BLOCK_PLING, 1F, 1.8F);
          TaskGuiHelper.sendTaskBar(player, "<#6EEB83>น้ำมันเต็ม 100 ลิตรแล้ว <#FFB703>คลิกปุ่มมรกตด้านล่างเพื่อปิดฝาถัง");
        } else {
          TaskGuiHelper.sendTaskBar(player, "<#F4F1DE>เติมน้ำมันแล้ว <#FFB703>" + session.liters + "/100 ลิตร");
        }
      } else {
        TaskGuiHelper.playSound(player, XSound.UI_BUTTON_CLICK, 0.5F, 1.2F);
        TaskGuiHelper.sendTaskBar(player, "<#FFB703>คลิกปุ่มมรกตด้านล่าง <#F4F1DE>เพื่อปิดฝาถังน้ำมัน");
      }
      return true;
    }

    if(rawSlot == SEAL_CAP_SLOT) {
      if(session.liters >= 100) {
        completedSessions.add(player.getUniqueId());
        sessions.remove(player.getUniqueId());

        TaskGuiHelper.playSound(player, XSound.BLOCK_ANVIL_USE, 0.8F, 1.6F);
        TaskGuiHelper.playSound(player, XSound.ENTITY_PLAYER_LEVELUP, 1F, 1.5F);
        TaskGuiHelper.sendSuccessBar(player, "เติมน้ำมันดีเซลครบ 100 ลิตรสำเร็จ");

        Bukkit.getScheduler().runTaskLater(arena.getPlugin(), () -> {
          if(player.isOnline() && arena.getPlayers().contains(player) && !arena.getVotingManager().isVotingActive()) {
            if(isGui(player.getOpenInventory().getTitle())) {
              player.closeInventory();
            }
            arena.getTaskManager().completeTaskFromMinigame(player, TaskType.FIND_DIESEL_FUEL);
          }
        }, 8L);
      } else {
        TaskGuiHelper.playSound(player, XSound.ENTITY_VILLAGER_NO, 0.9F, 1F);
        TaskGuiHelper.sendErrorBar(player, "คลิกคันโยกเรืองแสงทางขวาให้ครบ 100 ลิตรก่อน");
      }
      return true;
    }

    if(rawSlot >= 0 && rawSlot < 36) {
      TaskGuiHelper.playSound(player, XSound.UI_BUTTON_CLICK, 0.45F, 1.0F);
      if(session.liters < 100) {
        TaskGuiHelper.sendTaskBar(player, "<#FFB703>คลิกคันโยกเรืองแสงทางขวา <#F4F1DE>เพื่อสูบน้ำมัน (+20 ลิตร)");
      } else {
        TaskGuiHelper.sendTaskBar(player, "<#6EEB83>คลิกปุ่มมรกตด้านล่าง <#F4F1DE>เพื่อปิดฝาถังน้ำมัน");
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
