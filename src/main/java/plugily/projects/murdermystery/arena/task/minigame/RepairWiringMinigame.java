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

  public static final String GUI_TITLE = HalloweenChat.color("<#5A4E7C>[ <gradient:#FF5400:#FFB703><bold>ซ่อมสายไฟเครื่องยนต์</bold></gradient> <#5A4E7C>]");

  public enum WireColor {
    RED("สีแดง (Red)", XMaterial.RED_WOOL, XMaterial.RED_STAINED_GLASS_PANE, "<#FF3B47>"),
    BLUE("สีน้ำเงิน (Blue)", XMaterial.BLUE_WOOL, XMaterial.BLUE_STAINED_GLASS_PANE, "<#72EFDD>"),
    YELLOW("สีเหลือง (Yellow)", XMaterial.YELLOW_WOOL, XMaterial.YELLOW_STAINED_GLASS_PANE, "<#FFB703>"),
    MAGENTA("สีชมพู (Magenta)", XMaterial.MAGENTA_WOOL, XMaterial.MAGENTA_STAINED_GLASS_PANE, "<#C77DFF>");

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

    XSound.BLOCK_CHEST_OPEN.play(player.getLocation(), 0.8F, 1.2F);
    HalloweenChat.sendActionBar(player, HalloweenChat.color("<gradient:#FF5400:#FFB703><bold>[ซ่อมสายไฟ]</bold></gradient> <#FFB703>คลิกเลือกสายไฟฝั่งซ้าย แล้วคลิกต่อกับสีเดียวกันฝั่งขวา!"));
    player.sendMessage(HalloweenChat.color("<gradient:#FF5400:#FFB703><bold>[ภารกิจซ่อมสายไฟ]</bold></gradient> <#FFB703>คลิกเลือกสายไฟฝั่งซ้าย แล้วต่อกับสีเดียวกันฝั่งขวาให้ครบ 4 เส้น (ดูวิธีทำที่หนังสือช่องบน)"));
  }

  private void buildGui(Inventory inv, WiringSession session) {
    ItemStack bg = XMaterial.PURPLE_STAINED_GLASS_PANE.parseItem();
    ItemMeta bgMeta = bg.getItemMeta();
    if(bgMeta != null) {
      bgMeta.setDisplayName(HalloweenChat.color("<#5A4E7C> "));
      bg.setItemMeta(bgMeta);
    }
    for(int i = 0; i < 54; i++) {
      inv.setItem(i, bg);
    }

    ItemStack banner = new ItemStack(Material.BOOK);
    ItemMeta bannerMeta = banner.getItemMeta();
    if(bannerMeta != null) {
      bannerMeta.setDisplayName(HalloweenChat.color("<gradient:#FF5400:#FFB703><bold>วิธีทำภารกิจ: ซ่อมสายไฟเครื่องยนต์ (Instructions)</bold></gradient>"));
      bannerMeta.setLore(HalloweenChat.colorLines(
        "<#F4F1DE>ขั้นตอนการทำภารกิจ:",
        "<#B8B0C8>1. คลิกเลือกสายไฟสีใดก็ได้ทาง <#F4F1DE>ฝั่งซ้าย (Left)",
        "<#B8B0C8>2. คลิกปลายสายที่มีสีเดียวกันทาง <#F4F1DE>ฝั่งขวา (Right)",
        "<#B8B0C8>3. เชื่อมต่อให้ตรงสีครบทั้ง 4 เส้นเพื่อเปิดเครื่องยนต์",
        "<#FF3B47>[!] ระวัง: หากต่อผิดสี จะเกิดประกายไฟช็อตและต้องต่อใหม่!"
      ));
      banner.setItemMeta(bannerMeta);
    }
    inv.setItem(4, banner);

    for(int i = 0; i < 4; i++) {
      WireColor leftColor = session.leftWires[i];
      boolean isConnected = session.connected[i];
      boolean isSelected = (session.selectedLeft == i);

      ItemStack leftItem = isConnected ? leftColor.wool.parseItem() : (isSelected ? XMaterial.SEA_LANTERN.parseItem() : leftColor.wool.parseItem());
      ItemMeta leftMeta = leftItem.getItemMeta();
      if(leftMeta != null) {
        if(isConnected) {
          leftMeta.setDisplayName(HalloweenChat.color(leftColor.colorCode + "* สายไฟ" + leftColor.name + " <#6EEB83>[เชื่อมแล้ว [/]]"));
          leftMeta.setLore(HalloweenChat.colorLines("<#6EEB83>สายไฟเส้นนี้เชื่อมต่อเรียบร้อยแล้ว"));
        } else if(isSelected) {
          leftMeta.setDisplayName(HalloweenChat.color(leftColor.colorCode + ">> [ กำลังเลือก: " + leftColor.name + " ]"));
          leftMeta.setLore(HalloweenChat.colorLines("<#FFB703>> คลิกที่ปลายสายสีเดียวกันทางฝั่งขวาเพื่อเชื่อมต่อ!"));
        } else {
          leftMeta.setDisplayName(HalloweenChat.color(leftColor.colorCode + "* ปลายสาย" + leftColor.name));
          leftMeta.setLore(HalloweenChat.colorLines("<#FFB703>> คลิกเพื่อเลือกสายไฟเส้นนี้"));
        }
        leftItem.setItemMeta(leftMeta);
      }
      inv.setItem(LEFT_SLOTS[i], leftItem);

      WireColor rightColor = session.rightWires[i];
      boolean rightConnected = false;
      for(int k = 0; k < 4; k++) {
        if(session.connected[k] && session.leftWires[k] == rightColor) {
          rightConnected = true;
          break;
        }
      }

      ItemStack rightItem = rightColor.wool.parseItem();
      ItemMeta rightMeta = rightItem.getItemMeta();
      if(rightMeta != null) {
        if(rightConnected) {
          rightMeta.setDisplayName(HalloweenChat.color(rightColor.colorCode + "* ปลายสาย" + rightColor.name + " <#6EEB83>[เชื่อมแล้ว [/]]"));
        } else {
          rightMeta.setDisplayName(HalloweenChat.color(rightColor.colorCode + "* ปลายสาย" + rightColor.name));
          rightMeta.setLore(HalloweenChat.colorLines("<#FFB703>> คลิกเพื่อเชื่อมต่อ"));
        }
        rightItem.setItemMeta(rightMeta);
      }
      inv.setItem(RIGHT_SLOTS[i], rightItem);

      int startSlot = LEFT_SLOTS[i] + 1;
      int endSlot = RIGHT_SLOTS[i] - 1;

      ItemStack wireLine;
      if(isConnected) {
        wireLine = leftColor.glass.parseItem();
        ItemMeta wlMeta = wireLine.getItemMeta();
        if(wlMeta != null) {
          wlMeta.setDisplayName(HalloweenChat.color(leftColor.colorCode + "=== กระแสไฟวิ่งปกติ ==="));
          wireLine.setItemMeta(wlMeta);
        }
      } else {
        wireLine = XMaterial.BLACK_STAINED_GLASS_PANE.parseItem();
        ItemMeta wlMeta = wireLine.getItemMeta();
        if(wlMeta != null) {
          wlMeta.setDisplayName(HalloweenChat.color("<#5A4E7C>--- สายไฟขาด ---"));
          wireLine.setItemMeta(wlMeta);
        }
      }
      for(int s = startSlot; s <= endSlot; s++) {
        inv.setItem(s, wireLine);
      }
    }

    int doneCount = 0;
    for(boolean b : session.connected) {
      if(b) {
        doneCount++;
      }
    }

    ItemStack status = new ItemStack(doneCount == 4 ? Material.EMERALD : Material.REDSTONE);
    ItemMeta stMeta = status.getItemMeta();
    if(stMeta != null) {
      stMeta.setDisplayName(HalloweenChat.color(doneCount == 4
        ? "<gradient:#38B000:#9EF01A><bold>[/] เชื่อมต่อสายไฟครบทั้งหมดแล้ว!</bold></gradient>"
        : "<#FF7518>ความคืบหน้า: <#FFB703>" + doneCount + " <#5A4E7C>/ <#FFB703>4 เส้น"));
      stMeta.setLore(HalloweenChat.colorLines("<#B8B0C8>ต่อสายไฟให้ตรงสีกันทุกเส้น"));
      status.setItemMeta(stMeta);
    }
    inv.setItem(49, status);
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

    if(rawSlot == 4) {
      XSound.UI_BUTTON_CLICK.play(player.getLocation(), 0.5F, 1.2F);
      HalloweenChat.sendActionBar(player, HalloweenChat.color("<#FFB703>คลิกเลือกสายไฟฝั่งซ้าย แล้วคลิกต่อกับสีเดียวกันฝั่งขวา!"));
      return true;
    }

    for(int i = 0; i < 4; i++) {
      if(LEFT_SLOTS[i] == rawSlot) {
        if(session.connected[i]) {
          XSound.UI_BUTTON_CLICK.play(player.getLocation(), 0.5F, 1F);
          HalloweenChat.sendActionBar(player, HalloweenChat.color("<#6EEB83>สายไฟเส้นนี้เชื่อมต่อเรียบร้อยแล้ว!"));
          return true;
        }

        session.selectedLeft = i;
        WireColor color = session.leftWires[i];
        XSound.BLOCK_LEVER_CLICK.play(player.getLocation(), 1F, 1.2F);
        HalloweenChat.sendActionBar(player, HalloweenChat.color("<#FFB703>เลือกสายไฟ" + color.name + " แล้ว! -> คลิกปลายสายสีเดียวกันทางขวา!"));
        buildGui(player.getOpenInventory().getTopInventory(), session);
        return true;
      }
    }

    for(int j = 0; j < 4; j++) {
      if(RIGHT_SLOTS[j] == rawSlot) {
        if(session.selectedLeft == -1) {
          XSound.ENTITY_VILLAGER_NO.play(player.getLocation(), 0.7F, 1.2F);
          HalloweenChat.sendActionBar(player, HalloweenChat.color("<#FF3B47>กรุณาคลิกเลือกสายไฟทางฝั่งซ้ายก่อน!"));
          return true;
        }

        WireColor selectedColor = session.leftWires[session.selectedLeft];
        WireColor clickedRightColor = session.rightWires[j];

        if(selectedColor == clickedRightColor) {
          session.connected[session.selectedLeft] = true;
          session.selectedLeft = -1;

          XSound.BLOCK_ANVIL_USE.play(player.getLocation(), 0.6F, 1.8F);
          XSound.BLOCK_REDSTONE_TORCH_BURNOUT.play(player.getLocation(), 0.8F, 1.5F);

          buildGui(player.getOpenInventory().getTopInventory(), session);

          boolean allDone = true;
          for(boolean b : session.connected) {
            if(!b) {
              allDone = false;
              break;
            }
          }

          if(allDone) {
            completedSessions.add(player.getUniqueId());
            sessions.remove(player.getUniqueId());

            XSound.ENTITY_PLAYER_LEVELUP.play(player.getLocation(), 1F, 1.5F);
            HalloweenChat.sendActionBar(player, HalloweenChat.color("<gradient:#38B000:#9EF01A><bold>[/] ซ่อมสายไฟเครื่องยนต์สำเร็จ! ระบบไฟเชื่อมต่อสมบูรณ์!</bold></gradient>"));

            Bukkit.getScheduler().runTaskLater(arena.getPlugin(), () -> {
              if(player.isOnline()) {
                player.closeInventory();
                arena.getTaskManager().completeTaskFromMinigame(player, TaskType.REPAIR_ENGINE_WIRING);
              }
            }, 10L);
          } else {
            HalloweenChat.sendActionBar(player, HalloweenChat.color("<#6EEB83>[/] เชื่อมต่อสายไฟ" + selectedColor.name + " สำเร็จ!"));
          }
        } else {
          XSound.ENTITY_VILLAGER_NO.play(player.getLocation(), 1F, 0.8F);
          XSound.ENTITY_CREEPER_PRIMED.play(player.getLocation(), 0.7F, 1.8F);

          HalloweenChat.sendActionBar(player, HalloweenChat.color("<#FF3B47>[x] สีไม่ตรงกัน! (" + selectedColor.name + " != " + clickedRightColor.name + ") ลองใหม่อีกครั้ง!"));
          session.selectedLeft = -1;
          buildGui(player.getOpenInventory().getTopInventory(), session);
        }
        return true;
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
