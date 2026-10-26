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

public class TirePressureMinigame implements TaskMinigame {

  public static final String GUI_TITLE = HalloweenChat.color("<shadow:#000000FF><#5A4E7C>[</shadow><#FF7518>เช็คลมยางรถบัส<shadow:#000000FF><#5A4E7C>]</shadow>");

  private final Arena arena;
  private final Map<UUID, Integer> playerPsi = new HashMap<>();
  private final Set<UUID> completedSessions = new HashSet<>();

  private static final int MIN_TARGET_PSI = 30;
  private static final int MAX_TARGET_PSI = 32;
  private static final int MAX_SAFE_PSI = 34;

  private static final int[] GAUGE_SLOTS = {1, 2, 3, 4, 5, 6, 7};
  private static final int METER_SLOT = 13;
  private static final int HEAVY_PUMP_SLOT = 20;
  private static final int FINE_PUMP_SLOT = 22;
  private static final int BLEED_VALVE_SLOT = 24;
  private static final int LOCK_VALVE_SLOT = 31;

  public TirePressureMinigame(Arena arena) {
    this.arena = arena;
  }

  @Override
  public TaskType getTaskType() {
    return TaskType.CHECK_TIRE_PRESSURE;
  }

  @Override
  public String getGuiTitle() {
    return GUI_TITLE;
  }

  @Override
  public boolean isGui(String title) {
    return title != null && HalloweenChat.strip(title).contains("เช็คลมยางรถบัส");
  }

  @Override
  public void open(Player player) {
    completedSessions.remove(player.getUniqueId());
    int initialPsi = 10 + (int) (Math.random() * 5);
    playerPsi.put(player.getUniqueId(), initialPsi);

    Inventory inv = Bukkit.createInventory(null, 36, GUI_TITLE);
    buildGui(inv, initialPsi);
    player.openInventory(inv);

    XSound.BLOCK_CHEST_OPEN.play(player.getLocation(), 0.8F, 1.2F);
    TaskGuiHelper.sendTaskBar(player, "<#F4F1DE>สูบลมให้ได้ <#6EEB83>30 - 32 PSI <#B8B0C8>(คลิกปุ่มที่เรืองแสงได้เลย)");
  }

  private void buildGui(Inventory inv, int psi) {
    TaskGuiHelper.fillBackground(inv);

    boolean inSweetSpot = (psi >= MIN_TARGET_PSI && psi <= MAX_TARGET_PSI);

    int activeZone;
    if(psi < 16) {
      activeZone = 0;
    } else if(psi <= 21) {
      activeZone = 1;
    } else if(psi <= 25) {
      activeZone = 2;
    } else if(psi <= 29) {
      activeZone = 3;
    } else if(psi <= 32) {
      activeZone = 4;
    } else if(psi == 33) {
      activeZone = 5;
    } else {
      activeZone = 6;
    }

    for(int i = 0; i < GAUGE_SLOTS.length; i++) {
      ItemStack led;
      if(i == 4) {
        if(inSweetSpot) {
          led = TaskGuiHelper.createItem(
            XMaterial.EMERALD_BLOCK,
            1,
            false,
            TaskGuiHelper.tag("<#6EEB83>", "โซนมาตรฐาน 30-32 PSI - พอดีแล้ว"),
            "",
            "<#6EEB83>แรงดันอยู่ในเกณฑ์ปลอดภัย พร้อมล็อกวาล์ว"
          );
        } else if(psi > MAX_TARGET_PSI) {
          led = TaskGuiHelper.createItem(
            XMaterial.YELLOW_STAINED_GLASS_PANE,
            1,
            false,
            TaskGuiHelper.tag("<#FFB703>", "เลยโซน 30-32 PSI ไปแล้ว"),
            "",
            TaskGuiHelper.hint("<#FF3B47>", "คลิกวาล์วระบายลมทางขวา (-3 PSI)")
          );
        } else {
          led = TaskGuiHelper.createItem(
            XMaterial.GREEN_STAINED_GLASS_PANE,
            1,
            false,
            TaskGuiHelper.tag("<#6EEB83>", "เป้าหมายมาตรฐาน 30-32 PSI"),
            "",
            TaskGuiHelper.hint("<#B8B0C8>", "สูบลมให้แถบวัดขึ้นมาถึงช่องสีเขียวนี้")
          );
        }
      } else if(i < 4) {
        if(i <= activeZone) {
          led = TaskGuiHelper.createItem(
            inSweetSpot ? XMaterial.LIME_STAINED_GLASS_PANE : XMaterial.ORANGE_STAINED_GLASS_PANE,
            1,
            false,
            TaskGuiHelper.tag(inSweetSpot ? "<#6EEB83>" : "<#FFB703>", "แรงดันลมสะสม " + psi + " PSI"),
            "",
            inSweetSpot ? "<#6EEB83>แรงดันได้ระดับมาตรฐาน" : "<#FFB703>แรงดันยังต่ำกว่า 30 PSI"
          );
        } else {
          led = TaskGuiHelper.createItem(
            XMaterial.GRAY_STAINED_GLASS_PANE,
            1,
            false,
            TaskGuiHelper.tag("<#B8B0C8>", "แถบวัดแรงดันต่ำ"),
            "",
            "<#B8B0C8>ยังไม่มีแรงดันถึงระดับนี้"
          );
        }
      } else {
        if(i <= activeZone) {
          led = TaskGuiHelper.createItem(
            XMaterial.RED_STAINED_GLASS_PANE,
            1,
            false,
            TaskGuiHelper.tag("<#FF3B47>", "แรงดันเกินพิกัด " + psi + " PSI"),
            "",
            "<#FF3B47>อันตราย! หากเกิน 34 PSI วาล์วนิรภัยจะดีดกลับ"
          );
        } else {
          led = TaskGuiHelper.createItem(
            XMaterial.GRAY_STAINED_GLASS_PANE,
            1,
            false,
            TaskGuiHelper.tag("<#B8B0C8>", "โซนอันตราย (เกิน 32 PSI)"),
            "",
            "<#B8B0C8>ระวังอย่าสูบลมจนล้ำเข้ามาโซนนี้"
          );
        }
      }
      inv.setItem(GAUGE_SLOTS[i], led);
    }

    String diffStatus;
    if(inSweetSpot) {
      diffStatus = TaskGuiHelper.hint("<#6EEB83>", "แรงดันพอดีแล้ว คลิกปุ่มมรกตด้านล่างเพื่อล็อกวาล์ว");
    } else if(psi < MIN_TARGET_PSI) {
      diffStatus = TaskGuiHelper.hint("<#FFB703>", "ยังขาดอีก " + (MIN_TARGET_PSI - psi) + " PSI จะถึงเกณฑ์มาตรฐาน");
    } else {
      diffStatus = TaskGuiHelper.hint("<#FF3B47>", "ลมเกินอยู่ " + (psi - MAX_TARGET_PSI) + " PSI คลิกวาล์วระบายลม -3 PSI");
    }

    inv.setItem(METER_SLOT, TaskGuiHelper.createItem(
      Material.CLOCK,
      Math.max(1, psi),
      false,
      TaskGuiHelper.tag(inSweetSpot ? "<#6EEB83>" : (psi > MAX_TARGET_PSI ? "<#FF3B47>" : "<#FFB703>"), "แรงดันปัจจุบัน " + psi + " PSI"),
      "",
      "<#F4F1DE>ค่าเป้าหมาย <#6EEB83>30 - 32 PSI <#B8B0C8>(ห้ามเกิน 34 PSI)",
      diffStatus,
      "",
      TaskGuiHelper.hint("<#72EFDD>", "Visual Task - ผู้เล่นรอบข้างจะเห็นคลื่นลมยืนยันตัวตน")
    ));

    boolean recommendHeavy = psi <= 25;
    int heavyPreview = psi + 5;
    inv.setItem(HEAVY_PUMP_SLOT, TaskGuiHelper.createItem(
      XMaterial.PISTON,
      5,
      recommendHeavy,
      recommendHeavy ? TaskGuiHelper.clickName("ปั๊มลมแรง +5 PSI") : TaskGuiHelper.tag("<#FF7518>", "ปั๊มลมแรง +5 PSI"),
      "",
      heavyPreview > MAX_SAFE_PSI
        ? "<#FF3B47>ระวัง! กดแล้วจะทะลุ " + heavyPreview + " PSI (วาล์วจะดีดกลับ)"
        : "<#F4F1DE>หลังกดจะเป็น <#FFB703>" + heavyPreview + " PSI",
      "",
      TaskGuiHelper.hint(recommendHeavy ? "<#FFB703>" : "<#B8B0C8>", recommendHeavy ? "แนะนำให้กดปุ่มนี้เพื่อเติมลมเร็ว" : "กดเพื่อเพิ่มแรงดัน +5 PSI")
    ));

    boolean recommendFine = psi >= 26 && psi < MIN_TARGET_PSI;
    int finePreview = psi + 2;
    inv.setItem(FINE_PUMP_SLOT, TaskGuiHelper.createItem(
      XMaterial.TRIPWIRE_HOOK,
      2,
      recommendFine,
      recommendFine ? TaskGuiHelper.clickName("ปั๊มลมละเอียด +2 PSI") : TaskGuiHelper.tag("<#FFB703>", "ปั๊มลมละเอียด +2 PSI"),
      "",
      finePreview > MAX_SAFE_PSI
        ? "<#FF3B47>ระวัง! กดแล้วจะทะลุ " + finePreview + " PSI (วาล์วจะดีดกลับ)"
        : "<#F4F1DE>หลังกดจะเป็น " + (finePreview >= MIN_TARGET_PSI && finePreview <= MAX_TARGET_PSI ? "<#6EEB83>" : "<#FFB703>") + finePreview + " PSI",
      "",
      TaskGuiHelper.hint(recommendFine ? "<#FFB703>" : "<#B8B0C8>", recommendFine ? "แนะนำให้กดปุ่มนี้เพื่อเทียบเข้าโซน 30-32" : "กดเพื่อเพิ่มแรงดันทีละ +2 PSI")
    ));

    boolean recommendBleed = psi > MAX_TARGET_PSI;
    int bleedPreview = Math.max(0, psi - 3);
    inv.setItem(BLEED_VALVE_SLOT, TaskGuiHelper.createItem(
      XMaterial.HOPPER,
      3,
      recommendBleed,
      recommendBleed ? TaskGuiHelper.clickName("วาล์วระบายลม -3 PSI") : TaskGuiHelper.tag("<#FF3B47>", "วาล์วระบายลม -3 PSI"),
      "",
      "<#F4F1DE>หลังกดจะลดเหลือ " + (bleedPreview >= MIN_TARGET_PSI && bleedPreview <= MAX_TARGET_PSI ? "<#6EEB83>" : "<#FFB703>") + bleedPreview + " PSI",
      "",
      TaskGuiHelper.hint(recommendBleed ? "<#FFB703>" : "<#B8B0C8>", recommendBleed ? "แนะนำให้กดเพื่อลดลมส่วนเกิน" : "กดเพื่อปล่อยลมออก -3 PSI")
    ));

    if(inSweetSpot) {
      inv.setItem(LOCK_VALVE_SLOT, TaskGuiHelper.createItem(
        XMaterial.EMERALD_BLOCK,
        1,
        true,
        TaskGuiHelper.confirmName("ปิดล็อกจุกลมยาง (" + psi + " PSI)"),
        "",
        "<#F4F1DE>แรงดันลมยางได้มาตรฐาน <#6EEB83>" + psi + " PSI",
        "",
        TaskGuiHelper.hint("<#6EEB83>", "กดคลิกที่นี่เพื่อเสร็จสิ้นภารกิจ")
      ));
    } else {
      inv.setItem(LOCK_VALVE_SLOT, TaskGuiHelper.createItem(
        XMaterial.IRON_BARS,
        1,
        false,
        TaskGuiHelper.lockedName("ปิดล็อกจุกลมยาง"),
        "",
        "<#FF3B47>ต้องปรับแรงดันให้อยู่ในช่วง 30 - 32 PSI ก่อน",
        "",
        TaskGuiHelper.hint("<#B8B0C8>", "ปัจจุบัน " + psi + " PSI")
      ));
    }
  }

  @Override
  public boolean handleClick(Player player, int rawSlot, ClickType clickType) {
    if(completedSessions.contains(player.getUniqueId())) {
      return true;
    }

    int currentPsi = playerPsi.getOrDefault(player.getUniqueId(), 12);

    if(rawSlot == HEAVY_PUMP_SLOT) {
      currentPsi += 5;
      XSound.BLOCK_PISTON_EXTEND.play(player.getLocation(), 0.9F, 1.2F);
      XSound.ENTITY_HORSE_BREATHE.play(player.getLocation(), 0.8F, 0.9F);
      checkAndApplyPsi(player, currentPsi);
      return true;
    }

    if(rawSlot == FINE_PUMP_SLOT) {
      currentPsi += 2;
      XSound.BLOCK_PISTON_EXTEND.play(player.getLocation(), 0.65F, 1.6F);
      XSound.ENTITY_CHICKEN_EGG.play(player.getLocation(), 0.7F, 0.85F);
      checkAndApplyPsi(player, currentPsi);
      return true;
    }

    if(rawSlot == BLEED_VALVE_SLOT) {
      currentPsi = Math.max(0, currentPsi - 3);
      XSound.BLOCK_FIRE_EXTINGUISH.play(player.getLocation(), 0.8F, 1.5F);
      checkAndApplyPsi(player, currentPsi);
      return true;
    }

    if(rawSlot == LOCK_VALVE_SLOT) {
      if(currentPsi >= MIN_TARGET_PSI && currentPsi <= MAX_TARGET_PSI) {
        completedSessions.add(player.getUniqueId());
        playerPsi.remove(player.getUniqueId());

        XSound.BLOCK_IRON_DOOR_CLOSE.play(player.getLocation(), 1F, 1.2F);
        XSound.ENTITY_PLAYER_LEVELUP.play(player.getLocation(), 1F, 1.5F);
        TaskGuiHelper.sendSuccessBar(player, "ล็อกแรงดันลมยางที่ " + currentPsi + " PSI สำเร็จ");

        Bukkit.getScheduler().runTaskLater(arena.getPlugin(), () -> {
          if(player.isOnline() && arena.getPlayers().contains(player) && !arena.getVotingManager().isVotingActive()) {
            if(isGui(player.getOpenInventory().getTitle())) {
              player.closeInventory();
            }
            arena.getTaskManager().completeTaskFromMinigame(player, TaskType.CHECK_TIRE_PRESSURE);
          }
        }, 8L);
      } else {
        XSound.ENTITY_VILLAGER_NO.play(player.getLocation(), 0.9F, 1F);
        TaskGuiHelper.sendErrorBar(player, "ปรับแรงดันให้ได้ 30-32 PSI ก่อน (ปัจจุบัน " + currentPsi + " PSI)");
      }
      return true;
    }

    if(rawSlot >= 0 && rawSlot < 36) {
      XSound.UI_BUTTON_CLICK.play(player.getLocation(), 0.45F, 1.0F);
      if(currentPsi >= MIN_TARGET_PSI && currentPsi <= MAX_TARGET_PSI) {
        TaskGuiHelper.sendTaskBar(player, "<#6EEB83>คลิกปุ่มมรกตด้านล่าง <#F4F1DE>เพื่อล็อกวาล์วลมยาง");
      } else {
        TaskGuiHelper.sendTaskBar(player, "<#FFB703>คลิกปุ่มปั๊มลมหรือวาล์วที่เรืองแสง <#F4F1DE>ให้ได้ 30-32 PSI");
      }
    }
    return true;
  }

  private void checkAndApplyPsi(Player player, int newPsi) {
    if(newPsi > MAX_SAFE_PSI) {
      XSound.BLOCK_FIRE_EXTINGUISH.play(player.getLocation(), 0.95F, 0.7F);
      XSound.BLOCK_NOTE_BLOCK_BASS.play(player.getLocation(), 0.9F, 0.65F);
      newPsi = 16;
      TaskGuiHelper.sendErrorBar(player, "แรงดันทะลุ 34 PSI วาล์วนิรภัยปล่อยลมลดเหลือ 16 PSI");
    } else if(newPsi >= MIN_TARGET_PSI && newPsi <= MAX_TARGET_PSI) {
      XSound.BLOCK_NOTE_BLOCK_PLING.play(player.getLocation(), 0.9F, 1.8F);
      TaskGuiHelper.sendTaskBar(player, "<#6EEB83>แรงดัน " + newPsi + " PSI พอดีแล้ว <#FFB703>คลิกปุ่มมรกตด้านล่างเพื่อล็อกวาล์ว");
    } else if(newPsi > MAX_TARGET_PSI) {
      TaskGuiHelper.sendTaskBar(player, "<#FF3B47>แรงดัน " + newPsi + " PSI เกินเกณฑ์ <#FFB703>คลิกวาล์วระบายลม -3 PSI ทางขวา");
    } else {
      TaskGuiHelper.sendTaskBar(player, "<#F4F1DE>แรงดันปัจจุบัน <#FFB703>" + newPsi + " PSI <#B8B0C8>(เป้าหมาย 30-32 PSI)");
    }

    playerPsi.put(player.getUniqueId(), newPsi);
    int progressStep = Math.max(0, Math.min(10, (int) Math.round(((double) newPsi / MIN_TARGET_PSI) * 10.0)));
    arena.getTaskManager().getVisualEffectManager().triggerStepEffect(player, TaskType.CHECK_TIRE_PRESSURE, progressStep, 10);
    buildGui(player.getOpenInventory().getTopInventory(), newPsi);
  }

  @Override
  public void handleClose(Player player) {
    playerPsi.remove(player.getUniqueId());
    completedSessions.remove(player.getUniqueId());
  }

  @Override
  public void cleanup() {
    playerPsi.clear();
    completedSessions.clear();
  }
}
