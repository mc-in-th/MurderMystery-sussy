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

public class TirePressureMinigame implements TaskMinigame {

  public static final String GUI_TITLE = HalloweenChat.color("<#5A4E7C>[ <gradient:#FF5400:#FFB703><bold>เช็คลมยางรถบัส</bold></gradient> <#5A4E7C>]");

  private final Arena arena;
  private final Map<UUID, Integer> playerPsi = new HashMap<>();
  private final Set<UUID> completedSessions = new HashSet<>();

  private static final int MIN_TARGET_PSI = 30;
  private static final int MAX_TARGET_PSI = 32;
  private static final int MAX_SAFE_PSI = 34;

  private static final int HEAVY_PUMP_SLOT = 11;
  private static final int FINE_PUMP_SLOT = 13;
  private static final int BLEED_VALVE_SLOT = 15;
  private static final int LOCK_VALVE_SLOT = 22;

  private static final int[] GAUGE_SLOTS = {1, 2, 3, 4, 5, 6, 7};

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

    Inventory inv = Bukkit.createInventory(null, 27, GUI_TITLE);
    buildGui(inv, initialPsi);
    player.openInventory(inv);

    XSound.BLOCK_CHEST_OPEN.play(player.getLocation(), 0.8F, 1.2F);
    VersionUtils.sendActionBar(player, HalloweenChat.color("<gradient:#FF5400:#FFB703><bold>[เช็คลมยาง]</bold></gradient> <#FFB703>ปั๊มและปรับแรงดันให้อยู่ในช่วง 30-32 PSI แล้วกดล็อควาล์ว!"));
    player.sendMessage(HalloweenChat.color("<gradient:#FF5400:#FFB703><bold>[ภารกิจเช็คลมยาง]</bold></gradient> <#FFB703>ปรับแรงดันลมยางให้อยู่ในช่วง 30-32 PSI แล้วกดล็อควาล์ว (ดูวิธีทำที่หนังสือช่อง 1)"));
  }

  private void buildGui(Inventory inv, int psi) {
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
      iMeta.setDisplayName(HalloweenChat.color("<gradient:#FF5400:#FFB703><bold>วิธีทำภารกิจ: เช็คลมยางรถบัส (Instructions)</bold></gradient>"));
      iMeta.setLore(HalloweenChat.colorLines(
        "<#F4F1DE>ขั้นตอนการทำภารกิจ:",
        "<#B8B0C8>1. ดูกล่องแรงดันปัจจุบันด้านบน",
        "<#B8B0C8>2. ใช้ปั๊มหนัก (+5 PSI) หรือปั๊มละเอียด (+2 PSI) เพื่อเพิ่มลม",
        "<#B8B0C8>3. หากเผลอปั๊มเกิน ให้ใช้วาล์วระบายลม (-3 PSI)",
        "<#B8B0C8>4. ปรับให้อยู่ในช่วง <#6EEB83>30 - 32 PSI <#B8B0C8>(แถบสีเขียวพอดีเป๊ะ)",
        "<#B8B0C8>5. เมื่อได้ 30-32 PSI ให้คลิก <#6EEB83>[ปิดล็อควาล์ว] <#B8B0C8>ด้านล่าง",
        "<#FF3B47>[!] ระวัง: หากเกิน 34 PSI วาล์วจะระเบิดฉุกเฉินและลดเหลือ 16 PSI!"
      ));
      instruction.setItemMeta(iMeta);
    }
    inv.setItem(0, instruction);

    int activeZone;
    if(psi < 15) {
      activeZone = 0;
    } else if(psi <= 22) {
      activeZone = 1;
    } else if(psi <= 27) {
      activeZone = 2;
    } else if(psi <= 29) {
      activeZone = 3;
    } else if(psi <= 32) {
      activeZone = 4;
    } else if(psi <= 34) {
      activeZone = 5;
    } else {
      activeZone = 6;
    }

    for(int i = 0; i < GAUGE_SLOTS.length; i++) {
      boolean isActive = (i == activeZone);
      ItemStack led;

      if(i == 4) {
        led = isActive ? XMaterial.EMERALD_BLOCK.parseItem() : XMaterial.LIME_STAINED_GLASS_PANE.parseItem();
      } else if(i < 4) {
        if(i == 0) {
          led = isActive ? XMaterial.RED_CONCRETE.parseItem() : XMaterial.BLACK_STAINED_GLASS_PANE.parseItem();
        } else if(i <= 2) {
          led = isActive ? XMaterial.ORANGE_CONCRETE.parseItem() : XMaterial.BLACK_STAINED_GLASS_PANE.parseItem();
        } else {
          led = isActive ? XMaterial.YELLOW_CONCRETE.parseItem() : XMaterial.BLACK_STAINED_GLASS_PANE.parseItem();
        }
      } else {
        led = isActive ? XMaterial.RED_CONCRETE.parseItem() : XMaterial.BLACK_STAINED_GLASS_PANE.parseItem();
      }

      ItemMeta ledMeta = led.getItemMeta();
      if(ledMeta != null) {
        if(i == 4) {
          ledMeta.setDisplayName(HalloweenChat.color(isActive
            ? "<gradient:#38B000:#9EF01A><bold>[ เกณฑ์มาตรฐาน 30-32 PSI ] << คุณอยู่ตรงนี้!</bold></gradient>"
            : "<#6EEB83>[ ค่าเป้าหมายมาตรฐาน 30-32 PSI ]"));
        } else if(i < 4) {
          ledMeta.setDisplayName(HalloweenChat.color(isActive
            ? "<#FFB703>* ลมยางอ่อน (" + psi + " PSI) << คุณอยู่ตรงนี้"
            : "<#5A4E7C>ระดับแรงดันต่ำ"));
        } else {
          ledMeta.setDisplayName(HalloweenChat.color(isActive
            ? "<#FF3B47>* ลมยางเกิน (" + psi + " PSI) << คุณอยู่ตรงนี้!"
            : "<#5A4E7C>ระดับแรงดันเกิน"));
        }
        led.setItemMeta(ledMeta);
      }
      inv.setItem(GAUGE_SLOTS[i], led);
    }

    boolean inSweetSpot = (psi >= MIN_TARGET_PSI && psi <= MAX_TARGET_PSI);
    ItemStack meter = new ItemStack(Material.CLOCK);
    ItemMeta meterMeta = meter.getItemMeta();
    if(meterMeta != null) {
      meterMeta.setDisplayName(HalloweenChat.color(inSweetSpot
        ? "<gradient:#38B000:#9EF01A><bold>เกจวัด: " + psi + " PSI (พอดีแล้ว!) [/]</bold></gradient>"
        : "<#FF7518><bold>เกจวัด: <#FFB703>" + psi + " PSI</bold>"));
      meterMeta.setLore(HalloweenChat.colorLines(
        "<#B8B0C8>แรงดันปัจจุบัน: <#FFB703>" + psi + " PSI",
        "<#6EEB83>ค่าเป้าหมายมาตรฐาน: 30 - 32 PSI",
        inSweetSpot
          ? "<#6EEB83>[/] แรงดันลมยางพอดีแล้ว! กดล็อควาล์วได้เลย"
          : (psi < MIN_TARGET_PSI ? "<#FF3B47>^ ขาดอีก " + (MIN_TARGET_PSI - psi) + " PSI" : "<#FF3B47>v ลมเกินอยู่ " + (psi - MAX_TARGET_PSI) + " PSI (ปล่อยลมออก!)")
      ));
      meter.setItemMeta(meterMeta);
    }
    inv.setItem(0, meter);
    inv.setItem(8, meter);

    ItemStack heavy = XMaterial.PISTON.parseItem();
    ItemMeta hMeta = heavy.getItemMeta();
    if(hMeta != null) {
      hMeta.setDisplayName(HalloweenChat.color("<#5A4E7C>[ <gradient:#FF5400:#FFB703><bold>ปั๊มลมแรง (+5 PSI)</bold></gradient> <#5A4E7C>]"));
      hMeta.setLore(HalloweenChat.colorLines(
        "<#B8B0C8>สูบลมแรงดันสูงเข้าไปในล้อ",
        "<#6EEB83>> เพิ่มแรงดัน +5 PSI",
        "<#B8B0C8>(ใช้เมื่อลมยางยังอ่อนมาก)"
      ));
      heavy.setItemMeta(hMeta);
    }
    inv.setItem(HEAVY_PUMP_SLOT, heavy);

    ItemStack fine = XMaterial.TRIPWIRE_HOOK.parseItem();
    ItemMeta fMeta = fine.getItemMeta();
    if(fMeta != null) {
      fMeta.setDisplayName(HalloweenChat.color("<#5A4E7C>[ <gradient:#FF7518:#FFB703><bold>ปั๊มลมละเอียด (+2 PSI)</bold></gradient> <#5A4E7C>]"));
      fMeta.setLore(HalloweenChat.colorLines(
        "<#B8B0C8>สูบลมแบบปรับละเอียดเพื่อเข้าสู่เป้าหมาย",
        "<#6EEB83>> เพิ่มแรงดัน +2 PSI",
        "<#B8B0C8>(ใช้เมื่อใกล้ถึง 30-32 PSI)"
      ));
      fine.setItemMeta(fMeta);
    }
    inv.setItem(FINE_PUMP_SLOT, fine);

    ItemStack bleed = XMaterial.HOPPER.parseItem();
    ItemMeta bMeta = bleed.getItemMeta();
    if(bMeta != null) {
      bMeta.setDisplayName(HalloweenChat.color("<#5A4E7C>[ <gradient:#D90429:#FF5400><bold>วาล์วระบายลม (-3 PSI)</bold></gradient> <#5A4E7C>]"));
      bMeta.setLore(HalloweenChat.colorLines(
        "<#B8B0C8>เปิดวาล์วระบายลมส่วนเกินออก",
        "<#FF3B47>> ลดแรงดัน -3 PSI",
        "<#B8B0C8>(ใช้เมื่อเผลอปั๊มลมเกิน 32 PSI)"
      ));
      bleed.setItemMeta(bMeta);
    }
    inv.setItem(BLEED_VALVE_SLOT, bleed);

    ItemStack lockBtn;
    if(inSweetSpot) {
      lockBtn = XMaterial.EMERALD_BLOCK.parseItem();
    } else {
      lockBtn = XMaterial.IRON_BARS.parseItem();
    }
    ItemMeta lMeta = lockBtn.getItemMeta();
    if(lMeta != null) {
      if(inSweetSpot) {
        lMeta.setDisplayName(HalloweenChat.color("<#5A4E7C>[ <gradient:#38B000:#9EF01A><bold>ปิดล็อควาล์วเสร็จสิ้น (Confirm) [/]</bold></gradient> <#5A4E7C>]"));
        lMeta.setLore(HalloweenChat.colorLines(
          "<#B8B0C8>แรงดันอยู่ที่ <#6EEB83>" + psi + " PSI <#B8B0C8>(มาตรฐาน 30-32 PSI)",
          "<#6EEB83>> คลิกเพื่อบันทึกและจบภารกิจ!"
        ));
      } else {
        lMeta.setDisplayName(HalloweenChat.color("<#B8B0C8>[ ปิดล็อควาล์ว (ยังไม่พร้อม) ]"));
        lMeta.setLore(HalloweenChat.colorLines(
          "<#FF3B47>แรงดันต้องอยู่ระหว่าง 30 - 32 PSI เท่านั้น!",
          "<#F4F1DE>ปัจจุบัน: <#FFB703>" + psi + " PSI",
          "<#B8B0C8>(ปรับแรงดันให้พอดีก่อนจึงจะล็อคได้)"
        ));
      }
      lockBtn.setItemMeta(lMeta);
    }
    inv.setItem(LOCK_VALVE_SLOT, lockBtn);
  }

  @Override
  public boolean handleClick(Player player, int rawSlot, ClickType clickType) {
    if(completedSessions.contains(player.getUniqueId())) {
      return true;
    }

    int currentPsi = playerPsi.getOrDefault(player.getUniqueId(), 12);

    if(rawSlot == 0) {
      XSound.UI_BUTTON_CLICK.play(player.getLocation(), 0.5F, 1.2F);
      VersionUtils.sendActionBar(player, HalloweenChat.color("<#FFB703>ปรับแรงดันให้อยู่ในช่วง 30-32 PSI แล้วกดล็อควาล์วด้านล่าง!"));
      return true;
    }

    if(rawSlot == HEAVY_PUMP_SLOT) {
      currentPsi += 5;
      XSound.BLOCK_PISTON_EXTEND.play(player.getLocation(), 0.9F, 1.2F);
      XSound.ENTITY_HORSE_BREATHE.play(player.getLocation(), 0.8F, 0.9F);
      checkAndApplyPsi(player, currentPsi);
      return true;
    }

    if(rawSlot == FINE_PUMP_SLOT) {
      currentPsi += 2;
      XSound.BLOCK_PISTON_EXTEND.play(player.getLocation(), 0.6F, 1.6F);
      XSound.ENTITY_CHICKEN_EGG.play(player.getLocation(), 0.7F, 0.8F);
      checkAndApplyPsi(player, currentPsi);
      return true;
    }

    if(rawSlot == BLEED_VALVE_SLOT) {
      currentPsi = Math.max(0, currentPsi - 3);
      XSound.BLOCK_FIRE_EXTINGUISH.play(player.getLocation(), 0.8F, 1.5F);
      VersionUtils.sendActionBar(player, HalloweenChat.color("<#B8B0C8>[วาล์วระบาย] ปล่อยลมออก -3 PSI -> เหลือ <#FFB703>" + currentPsi + " PSI"));
      playerPsi.put(player.getUniqueId(), currentPsi);
      buildGui(player.getOpenInventory().getTopInventory(), currentPsi);
      return true;
    }

    if(rawSlot == LOCK_VALVE_SLOT) {
      if(currentPsi >= MIN_TARGET_PSI && currentPsi <= MAX_TARGET_PSI) {
        completedSessions.add(player.getUniqueId());
        playerPsi.remove(player.getUniqueId());

        XSound.BLOCK_IRON_DOOR_CLOSE.play(player.getLocation(), 1F, 1.2F);
        XSound.ENTITY_PLAYER_LEVELUP.play(player.getLocation(), 1F, 1.5F);
        VersionUtils.sendActionBar(player, HalloweenChat.color("<gradient:#38B000:#9EF01A><bold>[/] เช็คลมยางสำเร็จ! แรงดันล็อคที่ " + currentPsi + " PSI มาตรฐาน!</bold></gradient>"));

        Bukkit.getScheduler().runTaskLater(arena.getPlugin(), () -> {
          if(player.isOnline()) {
            player.closeInventory();
            arena.getTaskManager().completeTaskFromMinigame(player, TaskType.CHECK_TIRE_PRESSURE);
          }
        }, 10L);
      } else {
        XSound.ENTITY_VILLAGER_NO.play(player.getLocation(), 1F, 1F);
        if(currentPsi < MIN_TARGET_PSI) {
          VersionUtils.sendActionBar(player, HalloweenChat.color("<#FF3B47>[ล็อคไม่ได้] ลมยางยังอ่อนเกินไป! (ปัจจุบัน " + currentPsi + " PSI / เป้าหมาย 30-32 PSI)"));
        } else {
          VersionUtils.sendActionBar(player, HalloweenChat.color("<#FF3B47>[ล็อคไม่ได้] ลมยางเกินเกณฑ์! ต้องระบายลมออก (ปัจจุบัน " + currentPsi + " PSI / เป้าหมาย 30-32 PSI)"));
        }
      }
      return true;
    }

    return true;
  }

  private void checkAndApplyPsi(Player player, int newPsi) {
    if(newPsi > MAX_SAFE_PSI) {
      XSound.ENTITY_GENERIC_EXPLODE.play(player.getLocation(), 0.8F, 1.6F);
      XSound.BLOCK_FIRE_EXTINGUISH.play(player.getLocation(), 1F, 0.5F);

      VersionUtils.sendActionBar(player, HalloweenChat.color("<gradient:#D90429:#FF5400><bold>แรงดันเกิน 34 PSI! วาล์วนิรภัยระบายลมทิ้งฉุกเฉิน! (ลมยางลดเหลือ 16 PSI)</bold></gradient>"));
      newPsi = 16;
    } else {
      if(newPsi >= MIN_TARGET_PSI && newPsi <= MAX_TARGET_PSI) {
        VersionUtils.sendActionBar(player, HalloweenChat.color("<#6EEB83>[/] แรงดันอยู่ที่ " + newPsi + " PSI (พอดีแล้ว!) -> รีบกด [ล็อควาล์ว] ด้านล่าง!"));
        XSound.BLOCK_NOTE_BLOCK_PLING.play(player.getLocation(), 0.8F, 1.8F);
      } else {
        VersionUtils.sendActionBar(player, HalloweenChat.color("<gradient:#FF5400:#FFB703>[เกจวัด]</gradient> <#F4F1DE>แรงดันปัจจุบัน: <#FF7518>" + newPsi + " PSI <#B8B0C8>(เป้าหมาย: <#6EEB83>30-32 PSI<#B8B0C8>)"));
      }
    }

    playerPsi.put(player.getUniqueId(), newPsi);
    buildGui(player.getOpenInventory().getTopInventory(), newPsi);
  }

  @Override
  public void handleClose(Player player) {
    if(completedSessions.contains(player.getUniqueId())) {
      completedSessions.remove(player.getUniqueId());
    }
  }

  @Override
  public void cleanup() {
    playerPsi.clear();
    completedSessions.clear();
  }
}
