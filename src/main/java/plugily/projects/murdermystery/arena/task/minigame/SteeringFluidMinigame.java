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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class SteeringFluidMinigame implements TaskMinigame {

  public static final String GUI_TITLE = HalloweenChat.color("<#5A4E7C>[ <gradient:#FF5400:#FFB703><bold>เช็คน้ำมันพวงมาลัยเพาเวอร์</bold></gradient> <#5A4E7C>]");

  private static class SteeringSession {
    int angle;

    SteeringSession() {
      this.angle = Math.random() < 0.5 ? -2 : 2;
    }
  }

  private final Arena arena;
  private final Map<UUID, SteeringSession> sessions = new HashMap<>();
  private final Set<UUID> completedSessions = new HashSet<>();

  private static final int INSTRUCTION_SLOT = 0;
  private static final int TURN_LEFT_SLOT = 11;
  private static final int RESERVOIR_SLOT = 13;
  private static final int TURN_RIGHT_SLOT = 15;
  private static final int ACTION_BUTTON_SLOT = 22;
  private static final int[] SCALE_SLOTS = {2, 3, 4, 5, 6};

  public SteeringFluidMinigame(Arena arena) {
    this.arena = arena;
  }

  @Override
  public TaskType getTaskType() {
    return TaskType.CHECK_STEERING_FLUID;
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
    return plain.contains("พวงมาลัย") || plain.contains("Steering");
  }

  @Override
  public void open(Player player) {
    completedSessions.remove(player.getUniqueId());
    SteeringSession session = new SteeringSession();
    sessions.put(player.getUniqueId(), session);

    Inventory inv = Bukkit.createInventory(null, 27, GUI_TITLE);
    buildGui(inv, session);
    player.openInventory(inv);

    XSound.BLOCK_CHEST_OPEN.play(player.getLocation(), 0.8F, 1.2F);
    VersionUtils.sendActionBar(player, HalloweenChat.color("<gradient:#FF5400:#FFB703><bold>[น้ำมันพวงมาลัย]</bold></gradient> <#FFB703>กดหมุนซ้าย/ขวา เพื่อตั้งศูนย์พวงมาลัยให้ตรง [ 0 ]!"));
    player.sendMessage(HalloweenChat.color("<gradient:#FF5400:#FFB703><bold>[ภารกิจน้ำมันพวงมาลัย]</bold></gradient> <#FFB703>กดปุ่มหมุนซ้าย/ขวาให้ตรงศูนย์ [ 0 ] สีเขียว -> กดล็อคด้านล่าง"));
  }

  private void buildGui(Inventory inv, SteeringSession session) {
    ItemStack bg = XMaterial.PURPLE_STAINED_GLASS_PANE.parseItem();
    ItemMeta bgMeta = bg.getItemMeta();
    if(bgMeta != null) {
      bgMeta.setDisplayName(HalloweenChat.color("<#5A4E7C> "));
      bg.setItemMeta(bgMeta);
    }
    for(int i = 0; i < 27; i++) {
      inv.setItem(i, bg);
    }

    ItemStack instruction = new ItemStack(Material.BOOK);
    ItemMeta iMeta = instruction.getItemMeta();
    if(iMeta != null) {
      iMeta.setDisplayName(HalloweenChat.color("<gradient:#FF5400:#FFB703><bold>วิธีทำภารกิจ: น้ำมันพวงมาลัยเพาเวอร์ (Instructions)</bold></gradient>"));
      iMeta.setLore(HalloweenChat.colorLines(
        "<#F4F1DE>ขั้นตอนการทำภารกิจ:",
        "<#B8B0C8>1. คลิกปุ่ม <#FFB703>[< หมุนซ้าย] <#B8B0C8>หรือ <#FFB703>[หมุนขวา >]",
        "<#B8B0C8>2. ปรับให้เข็มชี้ตรงที่ <#6EEB83>[ 0 ] ศูนย์ตรงกลาง (สีเขียว)",
        "<#B8B0C8>3. เมื่อตรงศูนย์แล้ว ให้คลิก <#6EEB83>[ล็อคระบบพวงมาลัย] <#B8B0C8>ด้านล่างเพื่อจบภารกิจ!"
      ));
      instruction.setItemMeta(iMeta);
    }
    inv.setItem(INSTRUCTION_SLOT, instruction);

    for(int i = 0; i < 5; i++) {
      int slotAngle = i - 2;
      ItemStack led;
      if(slotAngle == session.angle) {
        if(session.angle == 0) {
          led = XMaterial.LIME_STAINED_GLASS_PANE.parseItem();
        } else {
          led = XMaterial.RED_STAINED_GLASS_PANE.parseItem();
        }
      } else {
        led = XMaterial.BLACK_STAINED_GLASS_PANE.parseItem();
      }

      ItemMeta lMeta = led.getItemMeta();
      if(lMeta != null) {
        if(slotAngle == 0) {
          lMeta.setDisplayName(HalloweenChat.color("<#6EEB83>* [ 0 ] ศูนย์ตรงกลาง (CENTER) [/]"));
        } else if(slotAngle < 0) {
          lMeta.setDisplayName(HalloweenChat.color("<#FF3B47>* [ " + slotAngle + " ] เอียงซ้าย"));
        } else {
          lMeta.setDisplayName(HalloweenChat.color("<#FF3B47>* [ +" + slotAngle + " ] เอียงขวา"));
        }
        led.setItemMeta(lMeta);
      }
      inv.setItem(SCALE_SLOTS[i], led);
    }

    ItemStack turnLeft = XMaterial.ARROW.parseItem();
    ItemMeta tlMeta = turnLeft.getItemMeta();
    if(tlMeta != null) {
      tlMeta.setDisplayName(HalloweenChat.color("<#5A4E7C>[ <gradient:#FF5400:#FFB703><bold>< หมุนพวงมาลัยไปทางซ้าย</bold></gradient> <#5A4E7C>]"));
      tlMeta.setLore(HalloweenChat.colorLines(
        "<#B8B0C8>ปรับองศาพวงมาลัยไปทางซ้าย",
        "<#FFB703>> คลิกเพื่อหมุนซ้าย"
      ));
      turnLeft.setItemMeta(tlMeta);
    }
    inv.setItem(TURN_LEFT_SLOT, turnLeft);

    ItemStack wheel = new ItemStack(session.angle == 0 ? Material.EMERALD : Material.MINECART);
    ItemMeta rMeta = wheel.getItemMeta();
    if(rMeta != null) {
      if(session.angle == 0) {
        rMeta.setDisplayName(HalloweenChat.color("<gradient:#38B000:#9EF01A><bold>[/] พวงมาลัยตรงศูนย์แล้ว!</bold></gradient>"));
        rMeta.setLore(HalloweenChat.colorLines(
          "<#B8B0C8>ตำแหน่งศูนย์ตรง [ 0 ] ถูกต้องแล้ว",
          "<#6EEB83>> คลิกปุ่มล็อคด้านล่างเพื่อจบภารกิจ!"
        ));
      } else {
        rMeta.setDisplayName(HalloweenChat.color("<#5A4E7C>[ <gradient:#D90429:#FF5400><bold>พวงมาลัยยังไม่ตรงศูนย์</bold></gradient> <#5A4E7C>]"));
        rMeta.setLore(HalloweenChat.colorLines(
          "<#B8B0C8>ปัจจุบัน: " + (session.angle < 0 ? "<#FF3B47>เอียงซ้าย " + session.angle : "<#FF3B47>เอียงขวา +" + session.angle),
          "<#FFB703>> คลิกปุ่มลูกศรเพื่อปรับให้ตรง [ 0 ] ตรงกลาง"
        ));
      }
      wheel.setItemMeta(rMeta);
    }
    inv.setItem(RESERVOIR_SLOT, wheel);

    ItemStack turnRight = XMaterial.ARROW.parseItem();
    ItemMeta trMeta = turnRight.getItemMeta();
    if(trMeta != null) {
      trMeta.setDisplayName(HalloweenChat.color("<#5A4E7C>[ <gradient:#FF5400:#FFB703><bold>หมุนพวงมาลัยไปทางขวา ></bold></gradient> <#5A4E7C>]"));
      trMeta.setLore(HalloweenChat.colorLines(
        "<#B8B0C8>ปรับองศาพวงมาลัยไปทางขวา",
        "<#FFB703>> คลิกเพื่อหมุนขวา"
      ));
      turnRight.setItemMeta(trMeta);
    }
    inv.setItem(TURN_RIGHT_SLOT, turnRight);

    ItemStack actionBtn;
    if(session.angle == 0) {
      actionBtn = XMaterial.EMERALD_BLOCK.parseItem();
      ItemMeta aMeta = actionBtn.getItemMeta();
      if(aMeta != null) {
        aMeta.setDisplayName(HalloweenChat.color("<#5A4E7C>[ <gradient:#38B000:#9EF01A><bold>ล็อคระบบพวงมาลัยเพาเวอร์ (เสร็จสิ้น) [/]</bold></gradient> <#5A4E7C>]"));
        aMeta.setLore(HalloweenChat.colorLines(
          "<#B8B0C8>ตั้งศูนย์พวงมาลัยและตรวจเช็คสมบูรณ์",
          "<#6EEB83>> คลิกเพื่อจบภารกิจ!"
        ));
        actionBtn.setItemMeta(aMeta);
      }
    } else {
      actionBtn = XMaterial.IRON_BARS.parseItem();
      ItemMeta aMeta = actionBtn.getItemMeta();
      if(aMeta != null) {
        aMeta.setDisplayName(HalloweenChat.color("<#B8B0C8>[ ล็อคระบบพวงมาลัย (ยังไม่พร้อม) ]"));
        aMeta.setLore(HalloweenChat.colorLines("<#FF3B47>ต้องตั้งศูนย์พวงมาลัยให้ตรง [ 0 ] ก่อน!"));
        actionBtn.setItemMeta(aMeta);
      }
    }
    inv.setItem(ACTION_BUTTON_SLOT, actionBtn);
  }

  @Override
  public boolean handleClick(Player player, int rawSlot, ClickType clickType) {
    if(completedSessions.contains(player.getUniqueId())) {
      return true;
    }

    SteeringSession session = sessions.get(player.getUniqueId());
    if(session == null) {
      return true;
    }

    if(rawSlot == INSTRUCTION_SLOT) {
      XSound.UI_BUTTON_CLICK.play(player.getLocation(), 0.5F, 1.2F);
      VersionUtils.sendActionBar(player, HalloweenChat.color("<#FFB703>กดปุ่มหมุนซ้าย/ขวาให้ตรงศูนย์ [ 0 ] -> กดล็อคด้านล่าง!"));
      return true;
    }

    Inventory topInv = player.getOpenInventory().getTopInventory();

    if(rawSlot == TURN_LEFT_SLOT) {
      if(session.angle > -2) {
        session.angle--;
        XSound.BLOCK_DISPENSER_DISPENSE.play(player.getLocation(), 0.9F, 1.2F);
        checkAngleFeedback(player, session);
        buildGui(topInv, session);
      } else {
        XSound.BLOCK_ANVIL_HIT.play(player.getLocation(), 0.6F, 1.8F);
        VersionUtils.sendActionBar(player, HalloweenChat.color("<#FF3B47>สุดระยะการหมุนซ้ายแล้ว!"));
      }
      return true;
    }

    if(rawSlot == TURN_RIGHT_SLOT) {
      if(session.angle < 2) {
        session.angle++;
        XSound.BLOCK_DISPENSER_DISPENSE.play(player.getLocation(), 0.9F, 1.2F);
        checkAngleFeedback(player, session);
        buildGui(topInv, session);
      } else {
        XSound.BLOCK_ANVIL_HIT.play(player.getLocation(), 0.6F, 1.8F);
        VersionUtils.sendActionBar(player, HalloweenChat.color("<#FF3B47>สุดระยะการหมุนขวาแล้ว!"));
      }
      return true;
    }

    if(rawSlot == ACTION_BUTTON_SLOT) {
      if(session.angle == 0) {
        completedSessions.add(player.getUniqueId());
        sessions.remove(player.getUniqueId());

        XSound.BLOCK_ANVIL_USE.play(player.getLocation(), 0.8F, 1.5F);
        XSound.ENTITY_PLAYER_LEVELUP.play(player.getLocation(), 1F, 1.5F);
        VersionUtils.sendActionBar(player, HalloweenChat.color("<gradient:#38B000:#9EF01A><bold>[/] ตั้งศูนย์และเช็คน้ำมันพวงมาลัยเพาเวอร์เสร็จสิ้น!</bold></gradient>"));

        Bukkit.getScheduler().runTaskLater(arena.getPlugin(), () -> {
          if(player.isOnline()) {
            player.closeInventory();
            arena.getTaskManager().completeTaskFromMinigame(player, TaskType.CHECK_STEERING_FLUID);
          }
        }, 10L);
      } else {
        XSound.ENTITY_VILLAGER_NO.play(player.getLocation(), 0.8F, 1F);
        VersionUtils.sendActionBar(player, HalloweenChat.color("<#FF3B47>[ยังไม่พร้อม] กรุณาตั้งศูนย์พวงมาลัยให้ตรงที่ [ 0 ] ก่อน!"));
      }
      return true;
    }

    return true;
  }

  private void checkAngleFeedback(Player player, SteeringSession session) {
    if(session.angle == 0) {
      XSound.BLOCK_NOTE_BLOCK_PLING.play(player.getLocation(), 1F, 1.8F);
      VersionUtils.sendActionBar(player, HalloweenChat.color("<#6EEB83>[/] พวงมาลัยตรงศูนย์ [ 0 ] แล้ว! -> คลิก [ล็อคระบบพวงมาลัย] ด้านล่าง!"));
    } else if(session.angle < 0) {
      VersionUtils.sendActionBar(player, HalloweenChat.color("<#FFB703>พวงมาลัยเอียงซ้าย [ " + session.angle + " ] -> คลิกหมุนขวา >"));
    } else {
      VersionUtils.sendActionBar(player, HalloweenChat.color("<#FFB703>พวงมาลัยเอียงขวา [ +" + session.angle + " ] -> คลิกหมุนซ้าย <"));
    }
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
