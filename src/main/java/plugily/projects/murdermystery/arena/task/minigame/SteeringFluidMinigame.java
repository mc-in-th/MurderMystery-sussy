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

  public static final String GUI_TITLE = HalloweenChat.color("<#5A4E7C>[<#FF7518>เช็คน้ำมันพวงมาลัยเพาเวอร์<#5A4E7C>]");

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
    HalloweenChat.sendActionBar(player, HalloweenChat.color("<#5A4E7C>[<#FF7518>Task<#5A4E7C>] <#F4F1DE>กดหมุนซ้ายหรือขวาเพื่อตั้งศูนย์พวงมาลัยให้ตรง <#6EEB83>0 <#F4F1DE>แล้วกดล็อกด้านล่าง"));
  }

  private void buildGui(Inventory inv, SteeringSession session) {
    ItemStack bg = XMaterial.BLACK_STAINED_GLASS_PANE.parseItem();
    ItemMeta bgMeta = bg.getItemMeta();
    if(bgMeta != null) {
      bgMeta.setDisplayName(" ");
      try {
        bgMeta.setHideTooltip(true);
      } catch(Throwable ignored) {
      }
      bg.setItemMeta(bgMeta);
    }
    for(int i = 0; i < 27; i++) {
      inv.setItem(i, bg);
    }

    ItemStack instruction = new ItemStack(Material.BOOK);
    ItemMeta iMeta = instruction.getItemMeta();
    if(iMeta != null) {
      iMeta.setDisplayName(HalloweenChat.color("<#5A4E7C>[<#FF7518>Steering Alignment<#5A4E7C>]"));
      iMeta.setLore(HalloweenChat.colorLines(
        "",
        "<#F4F1DE>1. คลิกปุ่มลูกศรหมุนซ้ายหรือหมุนขวา",
        "<#F4F1DE>2. ปรับให้เข็มชี้ตรงศูนย์กลางที่ <#6EEB83>0",
        "<#F4F1DE>3. กดล็อกระบบพวงมาลัยด้านล่างเพื่อจบภารกิจ"
      ));
      instruction.setItemMeta(iMeta);
    }
    inv.setItem(INSTRUCTION_SLOT, instruction);

    for(int i = 0; i < SCALE_SLOTS.length; i++) {
      int slotAngle = i - 2;
      ItemStack led;
      if(slotAngle == session.angle) {
        led = (session.angle == 0 ? XMaterial.LIME_STAINED_GLASS_PANE : XMaterial.RED_STAINED_GLASS_PANE).parseItem();
      } else {
        led = XMaterial.GRAY_STAINED_GLASS_PANE.parseItem();
      }

      ItemMeta lMeta = led.getItemMeta();
      if(lMeta != null) {
        if(slotAngle == 0) {
          lMeta.setDisplayName(HalloweenChat.color(session.angle == 0
            ? "<#5A4E7C>[<#6EEB83>0 ศูนย์ตรงกลาง<#5A4E7C>]"
            : "<#5A4E7C>[<#B8B0C8>0 ศูนย์เป้าหมาย<#5A4E7C>]"));
        } else if(slotAngle < 0) {
          lMeta.setDisplayName(HalloweenChat.color(slotAngle == session.angle
            ? "<#5A4E7C>[<#FF3B47>" + slotAngle + " เอียงซ้าย<#5A4E7C>]"
            : "<#5A4E7C>[<#B8B0C8>" + slotAngle + "<#5A4E7C>]"));
        } else {
          lMeta.setDisplayName(HalloweenChat.color(slotAngle == session.angle
            ? "<#5A4E7C>[<#FF3B47>+" + slotAngle + " เอียงขวา<#5A4E7C>]"
            : "<#5A4E7C>[<#B8B0C8>+" + slotAngle + "<#5A4E7C>]"));
        }
        led.setItemMeta(lMeta);
      }
      inv.setItem(SCALE_SLOTS[i], led);
    }

    ItemStack turnLeft = XMaterial.ARROW.parseItem();
    ItemMeta tlMeta = turnLeft.getItemMeta();
    if(tlMeta != null) {
      tlMeta.setDisplayName(HalloweenChat.color("<#5A4E7C>[<#FFB703>หมุนพวงมาลัยไปทางซ้าย<#5A4E7C>]"));
      tlMeta.setLore(HalloweenChat.colorLines(
        "",
        "<#F4F1DE>ปรับองศาพวงมาลัยไปทางซ้าย",
        "",
        "<#5A4E7C>(<#B8B0C8>กดคลิกเพื่อหมุนซ้าย<#5A4E7C>)"
      ));
      turnLeft.setItemMeta(tlMeta);
    }
    inv.setItem(TURN_LEFT_SLOT, turnLeft);

    ItemStack wheel = new ItemStack(session.angle == 0 ? Material.EMERALD : Material.MINECART);
    ItemMeta rMeta = wheel.getItemMeta();
    if(rMeta != null) {
      if(session.angle == 0) {
        rMeta.setDisplayName(HalloweenChat.color("<#5A4E7C>[<#6EEB83>พวงมาลัยตรงศูนย์แล้ว<#5A4E7C>]"));
        rMeta.setLore(HalloweenChat.colorLines(
          "",
          "<#5A4E7C>(<#6EEB83>กดปุ่มล็อกด้านล่างได้เลย<#5A4E7C>)"
        ));
      } else {
        rMeta.setDisplayName(HalloweenChat.color("<#5A4E7C>[<#FF3B47>พวงมาลัยยังไม่ตรงศูนย์<#5A4E7C>]"));
        rMeta.setLore(HalloweenChat.colorLines(
          "",
          "<#F4F1DE>ตำแหน่งปัจจุบัน " + (session.angle < 0 ? "<#FF3B47>เอียงซ้าย " + session.angle : "<#FF3B47>เอียงขวา +" + session.angle),
          "",
          "<#5A4E7C>(<#B8B0C8>คลิกปุ่มลูกศรให้ตรงศูนย์ 0<#5A4E7C>)"
        ));
      }
      wheel.setItemMeta(rMeta);
    }
    inv.setItem(RESERVOIR_SLOT, wheel);

    ItemStack turnRight = XMaterial.ARROW.parseItem();
    ItemMeta trMeta = turnRight.getItemMeta();
    if(trMeta != null) {
      trMeta.setDisplayName(HalloweenChat.color("<#5A4E7C>[<#FFB703>หมุนพวงมาลัยไปทางขวา<#5A4E7C>]"));
      trMeta.setLore(HalloweenChat.colorLines(
        "",
        "<#F4F1DE>ปรับองศาพวงมาลัยไปทางขวา",
        "",
        "<#5A4E7C>(<#B8B0C8>กดคลิกเพื่อหมุนขวา<#5A4E7C>)"
      ));
      turnRight.setItemMeta(trMeta);
    }
    inv.setItem(TURN_RIGHT_SLOT, turnRight);

    ItemStack actionBtn = (session.angle == 0 ? XMaterial.EMERALD_BLOCK : XMaterial.IRON_BARS).parseItem();
    ItemMeta aMeta = actionBtn.getItemMeta();
    if(aMeta != null) {
      if(session.angle == 0) {
        aMeta.setDisplayName(HalloweenChat.color("<#5A4E7C>[<#6EEB83>ล็อกระบบพวงมาลัยเพาเวอร์<#5A4E7C>]"));
        aMeta.setLore(HalloweenChat.colorLines(
          "",
          "<#F4F1DE>ตั้งศูนย์พวงมาลัยและตรวจเช็คสมบูรณ์",
          "",
          "<#5A4E7C>(<#B8B0C8>กดคลิกเพื่อจบภารกิจ<#5A4E7C>)"
        ));
      } else {
        aMeta.setDisplayName(HalloweenChat.color("<#5A4E7C>[<#B8B0C8>ล็อกระบบพวงมาลัย<#5A4E7C>]"));
        aMeta.setLore(HalloweenChat.colorLines(
          "",
          "<#FF3B47>ต้องตั้งศูนย์พวงมาลัยให้ตรง 0 ก่อน"
        ));
      }
      actionBtn.setItemMeta(aMeta);
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
        HalloweenChat.sendActionBar(player, HalloweenChat.color("<#5A4E7C>[<#FF3B47>Task<#5A4E7C>] <#FF3B47>สุดระยะการหมุนซ้ายแล้ว"));
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
        HalloweenChat.sendActionBar(player, HalloweenChat.color("<#5A4E7C>[<#FF3B47>Task<#5A4E7C>] <#FF3B47>สุดระยะการหมุนขวาแล้ว"));
      }
      return true;
    }

    if(rawSlot == ACTION_BUTTON_SLOT) {
      if(session.angle == 0) {
        completedSessions.add(player.getUniqueId());
        sessions.remove(player.getUniqueId());

        XSound.BLOCK_ANVIL_USE.play(player.getLocation(), 0.8F, 1.5F);
        XSound.ENTITY_PLAYER_LEVELUP.play(player.getLocation(), 1F, 1.5F);
        HalloweenChat.sendActionBar(player, HalloweenChat.color("<#5A4E7C>[<#6EEB83>Task<#5A4E7C>] <#6EEB83>เช็คน้ำมันพวงมาลัยเพาเวอร์สำเร็จ"));

        Bukkit.getScheduler().runTaskLater(arena.getPlugin(), () -> {
          if(player.isOnline() && arena.getPlayers().contains(player) && !arena.getVotingManager().isVotingActive()) {
            if(isGui(player.getOpenInventory().getTitle())) {
              player.closeInventory();
            }
            arena.getTaskManager().completeTaskFromMinigame(player, TaskType.CHECK_STEERING_FLUID);
          }
        }, 8L);
      } else {
        XSound.ENTITY_VILLAGER_NO.play(player.getLocation(), 0.8F, 1F);
        HalloweenChat.sendActionBar(player, HalloweenChat.color("<#5A4E7C>[<#FF3B47>Task<#5A4E7C>] <#FF3B47>กรุณาตั้งศูนย์พวงมาลัยให้ตรงที่ 0 ก่อน"));
      }
      return true;
    }

    return true;
  }

  private void checkAngleFeedback(Player player, SteeringSession session) {
    if(session.angle == 0) {
      XSound.BLOCK_NOTE_BLOCK_PLING.play(player.getLocation(), 1F, 1.8F);
      HalloweenChat.sendActionBar(player, HalloweenChat.color("<#5A4E7C>[<#6EEB83>Task<#5A4E7C>] <#6EEB83>พวงมาลัยตรงศูนย์ 0 แล้ว กดล็อกด้านล่างได้เลย"));
    } else if(session.angle < 0) {
      HalloweenChat.sendActionBar(player, HalloweenChat.color("<#5A4E7C>[<#FF7518>Task<#5A4E7C>] <#F4F1DE>พวงมาลัยเอียงซ้าย <#FFB703>" + session.angle));
    } else {
      HalloweenChat.sendActionBar(player, HalloweenChat.color("<#5A4E7C>[<#FF7518>Task<#5A4E7C>] <#F4F1DE>พวงมาลัยเอียงขวา <#FFB703>+" + session.angle));
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
