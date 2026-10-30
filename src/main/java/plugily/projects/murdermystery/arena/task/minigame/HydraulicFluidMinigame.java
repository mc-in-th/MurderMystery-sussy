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

public class HydraulicFluidMinigame implements TaskMinigame {

  public static final String GUI_TITLE = HalloweenChat.color("<shadow:#000000FF><#5A4E7C>[</shadow><#FF7518>ถ่ายน้ำมันไฮดรอลิกสำรอง<shadow:#000000FF><#5A4E7C>]</shadow>");

  private static class HydraulicSession {
    int angle;

    HydraulicSession() {
      this.angle = Math.random() < 0.5 ? -2 : 2;
    }
  }

  private final Arena arena;
  private final Map<UUID, HydraulicSession> sessions = new HashMap<>();
  private final Set<UUID> completedSessions = new HashSet<>();

  private static final int BANNER_SLOT = 4;
  private static final int[] SCALE_SLOTS = {11, 12, 13, 14, 15};
  private static final int TURN_LEFT_SLOT = 20;
  private static final int RESERVOIR_SLOT = 22;
  private static final int TURN_RIGHT_SLOT = 24;
  private static final int ACTION_BUTTON_SLOT = 31;

  public HydraulicFluidMinigame(Arena arena) {
    this.arena = arena;
  }

  @Override
  public TaskType getTaskType() {
    return TaskType.TRANSFER_HYDRAULIC_FLUID;
  }

  @Override
  public String getGuiTitle() {
    return GUI_TITLE;
  }

  @Override
  public boolean isGui(String title) {
    return title != null && HalloweenChat.strip(title).contains("ถ่ายน้ำมันไฮดรอลิกสำรอง");
  }

  @Override
  public void open(Player player) {
    completedSessions.remove(player.getUniqueId());
    HydraulicSession session = new HydraulicSession();
    sessions.put(player.getUniqueId(), session);

    Inventory inv = Bukkit.createInventory(null, 36, GUI_TITLE);
    buildGui(inv, session);
    player.openInventory(inv);

    TaskGuiHelper.playSound(player, XSound.BLOCK_CHEST_OPEN, 0.8F, 1.2F);
    String dirHint = session.angle < 0 ? "หมุนขวา" : "หมุนซ้าย";
    TaskGuiHelper.sendTaskBar(player, "<#F4F1DE>คลิกปุ่มลูกศร <#FFB703>" + dirHint + "ที่เรืองแสง <#F4F1DE>เพื่อตั้งเข็มแรงดันวาล์วให้ตรง <#6EEB83>0");
  }

  private void buildGui(Inventory inv, HydraulicSession session) {
    TaskGuiHelper.fillBackground(inv);

    boolean centered = session.angle == 0;

    if(centered) {
      inv.setItem(BANNER_SLOT, TaskGuiHelper.createItem(
        Material.COMPASS,
        1,
        false,
        TaskGuiHelper.tag("<#6EEB83>", "ขั้นที่ 2/2 - แรงดันวาล์วสมดุลที่ 0 แล้ว"),
        "",
        "<#F4F1DE>คลิกปุ่ม <#6EEB83>มรกตเรืองแสงด้านล่าง <#F4F1DE>เพื่อล็อกวาล์วถ่ายน้ำมันไฮดรอลิก"
      ));
    } else if(session.angle < 0) {
      inv.setItem(BANNER_SLOT, TaskGuiHelper.createItem(
        Material.COMPASS,
        Math.abs(session.angle),
        false,
        TaskGuiHelper.tag("<#FF7518>", "ขั้นที่ 1/2 - เข็มแรงดันตกซ้าย (" + session.angle + ")"),
        "",
        "<#F4F1DE>คลิกปุ่มลูกศร <#FFB703>หมุนขวาที่เรืองแสง <#F4F1DE>อีก <#6EEB83>" + Math.abs(session.angle) + " ครั้ง",
        "<#F4F1DE>เพื่อให้เข็มแรงดันเลื่อนมาตรงช่องกลาง <#6EEB83>0"
      ));
    } else {
      inv.setItem(BANNER_SLOT, TaskGuiHelper.createItem(
        Material.COMPASS,
        session.angle,
        false,
        TaskGuiHelper.tag("<#FF7518>", "ขั้นที่ 1/2 - เข็มแรงดันเกินขวา (+" + session.angle + ")"),
        "",
        "<#F4F1DE>คลิกปุ่มลูกศร <#FFB703>หมุนซ้ายที่เรืองแสง <#F4F1DE>อีก <#6EEB83>" + session.angle + " ครั้ง",
        "<#F4F1DE>เพื่อให้เข็มแรงดันเลื่อนมาตรงช่องกลาง <#6EEB83>0"
      ));
    }

    for(int i = 0; i < SCALE_SLOTS.length; i++) {
      int slotAngle = i - 2;
      String label = slotAngle > 0 ? "+" + slotAngle : String.valueOf(slotAngle);
      ItemStack led;

      if(slotAngle == 0 && centered) {
        led = TaskGuiHelper.createItem(
          XMaterial.LIME_STAINED_GLASS_PANE,
          1,
          false,
          TaskGuiHelper.tag("<#6EEB83>", "0 - แรงดันสมดุลพอดี"),
          "",
          "<#6EEB83>ถ่ายน้ำมันไฮดรอลิกเข้าถังสำรองพร้อมใช้งานแล้ว"
        );
      } else if(slotAngle == 0) {
        led = TaskGuiHelper.createItem(
          XMaterial.YELLOW_STAINED_GLASS_PANE,
          1,
          false,
          TaskGuiHelper.tag("<#FFB703>", "0 - จุดสมดุลแรงดัน"),
          "",
          TaskGuiHelper.hint("<#FFB703>", "หมุนวาล์วให้เข็มมาตรงช่องนี้")
        );
      } else if(slotAngle == session.angle) {
        led = TaskGuiHelper.createItem(
          XMaterial.RED_STAINED_GLASS_PANE,
          1,
          false,
          TaskGuiHelper.tag("<#FF3B47>", "เข็มแรงดัน " + label + (slotAngle < 0 ? " (แรงดันต่ำ)" : " (แรงดันเกิน)")),
          "",
          TaskGuiHelper.hint("<#FFB703>", slotAngle < 0 ? "กดปุ่มหมุนขวาเพื่อเพิ่มแรงดัน" : "กดปุ่มหมุนซ้ายเพื่อลดแรงดัน")
        );
      } else {
        led = TaskGuiHelper.createItem(
          XMaterial.GRAY_STAINED_GLASS_PANE,
          1,
          false,
          TaskGuiHelper.tag("<#B8B0C8>", "ขีดวัดแรงดัน " + label),
          "",
          "<#B8B0C8>มาตรวัดวาล์วถังน้ำมันไฮดรอลิก"
        );
      }
      inv.setItem(SCALE_SLOTS[i], led);
    }

    boolean needLeft = session.angle > 0;
    inv.setItem(TURN_LEFT_SLOT, TaskGuiHelper.createItem(
      XMaterial.ARROW,
      needLeft ? session.angle : 1,
      needLeft,
      needLeft ? TaskGuiHelper.clickName("<- หมุนซ้ายลดแรงดันเข้าศูนย์") : TaskGuiHelper.tag("<#B8B0C8>", "<- หมุนวาล์วลดแรงดัน"),
      "",
      needLeft
        ? "<#F4F1DE>กดอีก <#6EEB83>" + session.angle + " ครั้ง <#F4F1DE>เพื่อเข้าจุดสมดุล 0"
        : "<#B8B0C8>ปรับแรงดันวาล์วไปทางซ้าย (-1)"
    ));

    inv.setItem(RESERVOIR_SLOT, TaskGuiHelper.createItem(
      centered ? Material.EMERALD : Material.HOPPER,
      1,
      false,
      TaskGuiHelper.tag(centered ? "<#6EEB83>" : "<#FF3B47>", centered ? "แรงดันวาล์วสมดุล 0 แล้ว" : "ถังจ่ายน้ำมันไฮดรอลิกสำรอง"),
      "",
      centered
        ? TaskGuiHelper.hint("<#6EEB83>", "กดปุ่มมรกตด้านล่างเพื่อล็อกวาล์วได้เลย")
        : "<#F4F1DE>ค่าปัจจุบัน " + (session.angle < 0 ? "<#FF3B47>แรงดันต่ำ " + session.angle : "<#FF3B47>แรงดันเกิน +" + session.angle)
    ));

    boolean needRight = session.angle < 0;
    inv.setItem(TURN_RIGHT_SLOT, TaskGuiHelper.createItem(
      XMaterial.ARROW,
      needRight ? Math.abs(session.angle) : 1,
      needRight,
      needRight ? TaskGuiHelper.clickName("หมุนขวาเพิ่มแรงดันเข้าศูนย์ ->") : TaskGuiHelper.tag("<#B8B0C8>", "หมุนวาล์วเพิ่มแรงดัน ->"),
      "",
      needRight
        ? "<#F4F1DE>กดอีก <#6EEB83>" + Math.abs(session.angle) + " ครั้ง <#F4F1DE>เพื่อเข้าจุดสมดุล 0"
        : "<#B8B0C8>ปรับแรงดันวาล์วไปทางขวา (+1)"
    ));

    if(centered) {
      inv.setItem(ACTION_BUTTON_SLOT, TaskGuiHelper.createItem(
        XMaterial.EMERALD_BLOCK,
        1,
        true,
        TaskGuiHelper.confirmName("ล็อกวาล์วถ่ายน้ำมันไฮดรอลิก"),
        "",
        "<#F4F1DE>แรงดันวาล์วสมดุลตรง <#6EEB83>0 <#F4F1DE>สมบูรณ์",
        "",
        TaskGuiHelper.hint("<#6EEB83>", "กดคลิกที่นี่เพื่อเสร็จสิ้นภารกิจ")
      ));
    } else {
      inv.setItem(ACTION_BUTTON_SLOT, TaskGuiHelper.createItem(
        XMaterial.IRON_BARS,
        1,
        false,
        TaskGuiHelper.lockedName("ล็อกวาล์วถ่ายน้ำมัน"),
        "",
        "<#FF3B47>ต้องปรับแรงดันวาล์วให้ตรงศูนย์ 0 ก่อน"
      ));
    }
  }

  @Override
  public boolean handleClick(Player player, int rawSlot, ClickType clickType) {
    if(completedSessions.contains(player.getUniqueId())) {
      return true;
    }

    HydraulicSession session = sessions.get(player.getUniqueId());
    if(session == null) {
      return true;
    }

    Inventory topInv = player.getOpenInventory().getTopInventory();

    if(rawSlot == TURN_LEFT_SLOT) {
      if(session.angle > -2) {
        session.angle--;
        TaskGuiHelper.playSound(player, XSound.BLOCK_DISPENSER_DISPENSE, 0.9F, 1.2F);
        buildGui(topInv, session);
        checkAngleFeedback(player, session);
      } else {
        TaskGuiHelper.playSound(player, XSound.BLOCK_ANVIL_HIT, 0.6F, 1.8F);
        TaskGuiHelper.sendErrorBar(player, "สุดระยะซ้ายแล้ว คลิกปุ่มหมุนขวาที่เรืองแสงแทน");
      }
      return true;
    }

    if(rawSlot == TURN_RIGHT_SLOT) {
      if(session.angle < 2) {
        session.angle++;
        TaskGuiHelper.playSound(player, XSound.BLOCK_DISPENSER_DISPENSE, 0.9F, 1.2F);
        buildGui(topInv, session);
        checkAngleFeedback(player, session);
      } else {
        TaskGuiHelper.playSound(player, XSound.BLOCK_ANVIL_HIT, 0.6F, 1.8F);
        TaskGuiHelper.sendErrorBar(player, "สุดระยะขวาแล้ว คลิกปุ่มหมุนซ้ายที่เรืองแสงแทน");
      }
      return true;
    }

    if(rawSlot == ACTION_BUTTON_SLOT) {
      if(session.angle == 0) {
        completedSessions.add(player.getUniqueId());
        sessions.remove(player.getUniqueId());

        TaskGuiHelper.playSound(player, XSound.BLOCK_ANVIL_USE, 0.8F, 1.5F);
        TaskGuiHelper.playSound(player, XSound.ENTITY_PLAYER_LEVELUP, 1F, 1.5F);
        TaskGuiHelper.sendSuccessBar(player, "ถ่ายน้ำมันไฮดรอลิกสำรองสำเร็จ");

        Bukkit.getScheduler().runTaskLater(arena.getPlugin(), () -> {
          if(player.isOnline() && arena.getPlayers().contains(player) && !arena.getVotingManager().isVotingActive()) {
            if(isGui(player.getOpenInventory().getTitle())) {
              player.closeInventory();
            }
            arena.getTaskManager().completeTaskFromMinigame(player, TaskType.TRANSFER_HYDRAULIC_FLUID);
          }
        }, 8L);
      } else {
        TaskGuiHelper.playSound(player, XSound.ENTITY_VILLAGER_NO, 0.85F, 1F);
        TaskGuiHelper.sendErrorBar(player, "คลิกลูกศรที่เรืองแสงให้เข็มแรงดันตรงศูนย์ 0 ก่อน");
      }
      return true;
    }

    if(rawSlot >= 0 && rawSlot < 36) {
      TaskGuiHelper.playSound(player, XSound.UI_BUTTON_CLICK, 0.45F, 1.0F);
      if(session.angle == 0) {
        TaskGuiHelper.sendTaskBar(player, "<#6EEB83>คลิกปุ่มมรกตด้านล่าง <#F4F1DE>เพื่อล็อกวาล์วถ่ายน้ำมัน");
      } else {
        TaskGuiHelper.sendTaskBar(player, "<#FFB703>คลิกปุ่มลูกศรที่เรืองแสง <#F4F1DE>เพื่อปรับแรงดันเข้าศูนย์ 0");
      }
    }
    return true;
  }

  private void checkAngleFeedback(Player player, HydraulicSession session) {
    if(session.angle == 0) {
      TaskGuiHelper.playSound(player, XSound.BLOCK_NOTE_BLOCK_PLING, 1F, 1.8F);
      TaskGuiHelper.sendTaskBar(player, "<#6EEB83>แรงดันสมดุลที่ 0 พอดีแล้ว <#FFB703>คลิกปุ่มมรกตด้านล่างเพื่อล็อกวาล์ว");
    } else if(session.angle < 0) {
      TaskGuiHelper.sendTaskBar(player, "<#F4F1DE>แรงดันยังต่ำ <#FFB703>" + session.angle + " <#B8B0C8>(คลิกหมุนขวาต่อ)");
    } else {
      TaskGuiHelper.sendTaskBar(player, "<#F4F1DE>แรงดันยังเกิน <#FFB703>+" + session.angle + " <#B8B0C8>(คลิกหมุนซ้ายต่อ)");
    }
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

