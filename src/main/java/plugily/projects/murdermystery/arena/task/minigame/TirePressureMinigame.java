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

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class TirePressureMinigame implements TaskMinigame {

  public static final String GUI_TITLE = "§8[ §6 เช็คลมยางรถบัส §8]";

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
    return title != null && title.contains("เช็คลมยางรถบัส");
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
    VersionUtils.sendActionBar(player, "§e[เช็คลมยาง] ปั๊มและปรับแรงดันให้อยู่ในช่วง 30-32 PSI แล้วกดล็อควาล์ว!");
    player.sendMessage("§6[ภารกิจเช็คลมยาง] §eปรับแรงดันลมยางให้อยู่ในช่วง 30-32 PSI แล้วกดล็อควาล์ว (ดูวิธีทำที่หนังสือช่อง 1)");
  }

  private void buildGui(Inventory inv, int psi) {
    ItemStack bg = XMaterial.GRAY_STAINED_GLASS_PANE.parseItem();
    ItemMeta bgMeta = bg.getItemMeta();
    if(bgMeta != null) {
      bgMeta.setDisplayName("§7 ");
      bg.setItemMeta(bgMeta);
    }
    for(int i = 0; i < 27; i++) {
      inv.setItem(i, bg);
    }

    ItemStack instruction = new ItemStack(Material.BOOK);
    ItemMeta iMeta = instruction.getItemMeta();
    if(iMeta != null) {
      iMeta.setDisplayName("§e§l วิธีทำภารกิจ: เช็คลมยางรถบัส (Instructions)");
      iMeta.setLore(Arrays.asList(
        "§fขั้นตอนการทำภารกิจ:",
        "§71. ดูกล่องแรงดันปัจจุบันด้านบน",
        "§72. ใช้ปั๊มหนัก (+5 PSI) หรือปั๊มละเอียด (+2 PSI) เพื่อเพิ่มลม",
        "§73. หากเผลอปั๊มเกิน ให้ใช้วาล์วระบายลม (-3 PSI)",
        "§74. ปรับให้อยู่ในช่วง §a30 - 32 PSI §7(แถบสีเขียวพอดีเป๊ะ)",
        "§75. เมื่อได้ 30-32 PSI ให้คลิก §a[ ปิดล็อควาล์ว] §7ด้านล่าง",
        "§c[!] ระวัง: หากเกิน 34 PSI วาล์วจะระเบิดฉุกเฉินและลดเหลือ 16 PSI!"
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
          ledMeta.setDisplayName(isActive ? "§a§l [ เกณฑ์มาตรฐาน 30-32 PSI ] << คุณอยู่ตรงนี้!" : "§a[ ค่าเป้าหมายมาตรฐาน 30-32 PSI ]");
        } else if(i < 4) {
          ledMeta.setDisplayName(isActive ? "§e* ลมยางอ่อน (" + psi + " PSI) << คุณอยู่ตรงนี้" : "§8ระดับแรงดันต่ำ");
        } else {
          ledMeta.setDisplayName(isActive ? "§c* ลมยางเกิน (" + psi + " PSI) << คุณอยู่ตรงนี้!" : "§8ระดับแรงดันเกิน");
        }
        led.setItemMeta(ledMeta);
      }
      inv.setItem(GAUGE_SLOTS[i], led);
    }

    boolean inSweetSpot = (psi >= MIN_TARGET_PSI && psi <= MAX_TARGET_PSI);
    ItemStack meter = new ItemStack(Material.CLOCK);
    ItemMeta meterMeta = meter.getItemMeta();
    if(meterMeta != null) {
      meterMeta.setDisplayName(inSweetSpot ? "§a§lเกจวัด: " + psi + " PSI (พอดีแล้ว!) [/]" : "§6§lเกจวัด: §e" + psi + " PSI");
      meterMeta.setLore(Arrays.asList(
        "§7แรงดันปัจจุบัน: §e" + psi + " PSI",
        "§aค่าเป้าหมายมาตรฐาน: §a30 - 32 PSI",
        inSweetSpot ? "§a[/] แรงดันลมยางพอดีแล้ว! กดล็อควาล์วได้เลย" : (psi < MIN_TARGET_PSI ? "§c^ ขาดอีก " + (MIN_TARGET_PSI - psi) + " PSI" : "§cv ลมเกินอยู่ " + (psi - MAX_TARGET_PSI) + " PSI (ปล่อยลมออก!)")
      ));
      meter.setItemMeta(meterMeta);
    }
    inv.setItem(0, meter);
    inv.setItem(8, meter);

    ItemStack heavy = XMaterial.PISTON.parseItem();
    ItemMeta hMeta = heavy.getItemMeta();
    if(hMeta != null) {
      hMeta.setDisplayName("§6§l[ ปั๊มลมแรง (+5 PSI) ]");
      hMeta.setLore(Arrays.asList(
        "§7สูบลมแรงดันสูงเข้าไปในล้อ",
        "§a> เพิ่มแรงดัน +5 PSI",
        "§7(ใช้เมื่อลมยางยังอ่อนมาก)"
      ));
      heavy.setItemMeta(hMeta);
    }
    inv.setItem(HEAVY_PUMP_SLOT, heavy);

    ItemStack fine = XMaterial.TRIPWIRE_HOOK.parseItem();
    ItemMeta fMeta = fine.getItemMeta();
    if(fMeta != null) {
      fMeta.setDisplayName("§e§l[ ปั๊มลมละเอียด (+2 PSI) ]");
      fMeta.setLore(Arrays.asList(
        "§7สูบลมแบบปรับละเอียดเพื่อเข้าสู่เป้าหมาย",
        "§a> เพิ่มแรงดัน +2 PSI",
        "§7(ใช้เมื่อใกล้ถึง 30-32 PSI)"
      ));
      fine.setItemMeta(fMeta);
    }
    inv.setItem(FINE_PUMP_SLOT, fine);

    ItemStack bleed = XMaterial.HOPPER.parseItem();
    ItemMeta bMeta = bleed.getItemMeta();
    if(bMeta != null) {
      bMeta.setDisplayName("§c§l[ วาล์วระบายลม (-3 PSI) ]");
      bMeta.setLore(Arrays.asList(
        "§7เปิดวาล์วระบายลมส่วนเกินออก",
        "§c> ลดแรงดัน -3 PSI",
        "§7(ใช้เมื่อเผลอปั๊มลมเกิน 32 PSI)"
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
        lMeta.setDisplayName("§a§l[ ปิดล็อควาล์วเสร็จสิ้น (Confirm) ] [/]");
        lMeta.setLore(Arrays.asList(
          "§7แรงดันอยู่ที่ §a" + psi + " PSI §7(มาตรฐาน 30-32 PSI)",
          "§a> คลิกเพื่อบันทึกและจบภารกิจ!"
        ));
      } else {
        lMeta.setDisplayName("§7[ ปิดล็อควาล์ว (ยังไม่พร้อม) ]");
        lMeta.setLore(Arrays.asList(
          "§cแรงดันต้องอยู่ระหว่าง 30 - 32 PSI เท่านั้น!",
          "§fปัจจุบัน: §e" + psi + " PSI",
          "§7(ปรับแรงดันให้พอดีก่อนจึงจะล็อคได้)"
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
      VersionUtils.sendActionBar(player, "§e ปรับแรงดันให้อยู่ในช่วง 30-32 PSI แล้วกดล็อควาล์วด้านล่าง!");
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
      VersionUtils.sendActionBar(player, "§7[วาล์วระบาย] ปล่อยลมออก -3 PSI -> เหลือ " + currentPsi + " PSI");
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
        VersionUtils.sendActionBar(player, "§a§l[/] เช็คลมยางสำเร็จ! แรงดันล็อคที่ " + currentPsi + " PSI มาตรฐาน!");

        Bukkit.getScheduler().runTaskLater(arena.getPlugin(), () -> {
          if(player.isOnline()) {
            player.closeInventory();
            arena.getTaskManager().completeTaskFromMinigame(player, TaskType.CHECK_TIRE_PRESSURE);
          }
        }, 10L);
      } else {
        XSound.ENTITY_VILLAGER_NO.play(player.getLocation(), 1F, 1F);
        if(currentPsi < MIN_TARGET_PSI) {
          VersionUtils.sendActionBar(player, "§c[ล็อคไม่ได้] ลมยางยังอ่อนเกินไป! (ปัจจุบัน " + currentPsi + " PSI / เป้าหมาย 30-32 PSI)");
        } else {
          VersionUtils.sendActionBar(player, "§c[ล็อคไม่ได้] ลมยางเกินเกณฑ์! ต้องระบายลมออก (ปัจจุบัน " + currentPsi + " PSI / เป้าหมาย 30-32 PSI)");
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

      VersionUtils.sendActionBar(player, "§c แรงดันเกิน 34 PSI! วาล์วนิรภัยระบายลมทิ้งฉุกเฉิน! (ลมยางลดเหลือ 16 PSI)");
      newPsi = 16;
    } else {
      if(newPsi >= MIN_TARGET_PSI && newPsi <= MAX_TARGET_PSI) {
        VersionUtils.sendActionBar(player, "§a[/] แรงดันอยู่ที่ " + newPsi + " PSI (พอดีแล้ว!) -> รีบกด [ ล็อควาล์ว] ด้านล่าง!");
        XSound.BLOCK_NOTE_BLOCK_PLING.play(player.getLocation(), 0.8F, 1.8F);
      } else {
        VersionUtils.sendActionBar(player, "§e[เกจวัด] แรงดันปัจจุบัน: §6" + newPsi + " PSI §7(เป้าหมาย: §a30-32 PSI§7)");
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
