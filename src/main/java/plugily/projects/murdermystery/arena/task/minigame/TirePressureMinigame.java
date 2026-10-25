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

public class TirePressureMinigame implements TaskMinigame {

  public static final String GUI_TITLE = HalloweenChat.color("<shadow:#000000FF><#5A4E7C>[</shadow><#FF7518>เช็คลมยางรถบัส<shadow:#000000FF><#5A4E7C>]</shadow>");

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
    HalloweenChat.sendActionBar(player, "<shadow:#000000FF><#5A4E7C>[</shadow><#FF7518>Task<shadow:#000000FF><#5A4E7C>]</shadow> <#F4F1DE>ปรับแรงดันลมยางให้อยู่ในช่วง <#6EEB83>30-32 PSI <#F4F1DE>แล้วกดล็อควาล์ว");
  }

  private void buildGui(Inventory inv, int psi) {
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
      HalloweenChat.setItemName(iMeta, "<shadow:#000000FF><#5A4E7C>[</shadow><#FF7518>Tire Pressure<shadow:#000000FF><#5A4E7C>]</shadow>");
      HalloweenChat.setItemLore(
        iMeta,
        "",
        "<#F4F1DE>1. ใช้ปั๊มลมแรง (+5) หรือปั๊มละเอียด (+2)",
        "<#F4F1DE>2. หากลมเกิน ให้ใช้วาล์วระบายลม (-3)",
        "<#F4F1DE>3. ปรับให้อยู่ในช่วง <#6EEB83>30 - 32 PSI <#F4F1DE>แล้วล็อควาล์ว",
        "",
        "<shadow:#000000FF><#5A4E7C>(</shadow><#FF3B47>หากเกิน 34 PSI วาล์วนิรภัยจะปล่อยลมออก<shadow:#000000FF><#5A4E7C>)</shadow>"
      );
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
          led = isActive ? XMaterial.RED_CONCRETE.parseItem() : XMaterial.GRAY_STAINED_GLASS_PANE.parseItem();
        } else if(i <= 2) {
          led = isActive ? XMaterial.ORANGE_CONCRETE.parseItem() : XMaterial.GRAY_STAINED_GLASS_PANE.parseItem();
        } else {
          led = isActive ? XMaterial.YELLOW_CONCRETE.parseItem() : XMaterial.GRAY_STAINED_GLASS_PANE.parseItem();
        }
      } else {
        led = isActive ? XMaterial.RED_CONCRETE.parseItem() : XMaterial.GRAY_STAINED_GLASS_PANE.parseItem();
      }

      ItemMeta ledMeta = led.getItemMeta();
      if(ledMeta != null) {
        if(i == 4) {
          HalloweenChat.setItemName(ledMeta, isActive
            ? "<shadow:#000000FF><#5A4E7C>[</shadow><#6EEB83>มาตรฐาน 30-32 PSI<shadow:#000000FF><#5A4E7C>]</shadow>"
            : "<shadow:#000000FF><#5A4E7C>[</shadow><#B8B0C8>เป้าหมาย 30-32 PSI<shadow:#000000FF><#5A4E7C>]</shadow>");
        } else if(i < 4) {
          HalloweenChat.setItemName(ledMeta, isActive
            ? "<shadow:#000000FF><#5A4E7C>[</shadow><#FFB703>ลมยางอ่อน " + psi + " PSI<shadow:#000000FF><#5A4E7C>]</shadow>"
            : "<shadow:#000000FF><#5A4E7C>[</shadow><#B8B0C8>ระดับแรงดันต่ำ<shadow:#000000FF><#5A4E7C>]</shadow>");
        } else {
          HalloweenChat.setItemName(ledMeta, isActive
            ? "<shadow:#000000FF><#5A4E7C>[</shadow><#FF3B47>ลมยางเกิน " + psi + " PSI<shadow:#000000FF><#5A4E7C>]</shadow>"
            : "<shadow:#000000FF><#5A4E7C>[</shadow><#B8B0C8>ระดับแรงดันเกิน<shadow:#000000FF><#5A4E7C>]</shadow>");
        }
        led.setItemMeta(ledMeta);
      }
      inv.setItem(GAUGE_SLOTS[i], led);
    }

    boolean inSweetSpot = (psi >= MIN_TARGET_PSI && psi <= MAX_TARGET_PSI);
    ItemStack meter = new ItemStack(Material.CLOCK);
    ItemMeta meterMeta = meter.getItemMeta();
    if(meterMeta != null) {
      HalloweenChat.setItemName(meterMeta, inSweetSpot
        ? "<shadow:#000000FF><#5A4E7C>[</shadow><#6EEB83>เกจวัด " + psi + " PSI<shadow:#000000FF><#5A4E7C>]</shadow>"
        : "<shadow:#000000FF><#5A4E7C>[</shadow><#FFB703>เกจวัด " + psi + " PSI<shadow:#000000FF><#5A4E7C>]</shadow>");
      HalloweenChat.setItemLore(
        meterMeta,
        "",
        "<#F4F1DE>แรงดันปัจจุบัน <#FFB703>" + psi + " PSI",
        "<#F4F1DE>ค่ามาตรฐาน <#6EEB83>30 - 32 PSI",
        "",
        inSweetSpot
          ? "<shadow:#000000FF><#5A4E7C>(</shadow><#6EEB83>แรงดันพอดีแล้ว กดล็อควาล์วด้านล่าง<shadow:#000000FF><#5A4E7C>)</shadow>"
          : (psi < MIN_TARGET_PSI
            ? "<shadow:#000000FF><#5A4E7C>(</shadow><#FFB703>ขาดอีก " + (MIN_TARGET_PSI - psi) + " PSI<shadow:#000000FF><#5A4E7C>)</shadow>"
            : "<shadow:#000000FF><#5A4E7C>(</shadow><#FF3B47>เกินอยู่ " + (psi - MAX_TARGET_PSI) + " PSI<shadow:#000000FF><#5A4E7C>)</shadow>")
      );
      meter.setItemMeta(meterMeta);
    }
    inv.setItem(8, meter);

    ItemStack heavy = XMaterial.PISTON.parseItem();
    ItemMeta hMeta = heavy.getItemMeta();
    if(hMeta != null) {
      HalloweenChat.setItemName(hMeta, "<shadow:#000000FF><#5A4E7C>[</shadow><#FF7518>ปั๊มลมแรง +5 PSI<shadow:#000000FF><#5A4E7C>]</shadow>");
      HalloweenChat.setItemLore(
        hMeta,
        "",
        "<#F4F1DE>สูบลมแรงดันสูงเข้ายางรถบัส",
        "",
        "<shadow:#000000FF><#5A4E7C>(</shadow><#B8B0C8>กดคลิกเพื่อเพิ่ม +5 PSI<shadow:#000000FF><#5A4E7C>)</shadow>"
      );
      heavy.setItemMeta(hMeta);
    }
    inv.setItem(HEAVY_PUMP_SLOT, heavy);

    ItemStack fine = XMaterial.TRIPWIRE_HOOK.parseItem();
    ItemMeta fMeta = fine.getItemMeta();
    if(fMeta != null) {
      HalloweenChat.setItemName(fMeta, "<shadow:#000000FF><#5A4E7C>[</shadow><#FFB703>ปั๊มลมละเอียด +2 PSI<shadow:#000000FF><#5A4E7C>]</shadow>");
      HalloweenChat.setItemLore(
        fMeta,
        "",
        "<#F4F1DE>สูบลมแบบปรับละเอียด",
        "",
        "<shadow:#000000FF><#5A4E7C>(</shadow><#B8B0C8>กดคลิกเพื่อเพิ่ม +2 PSI<shadow:#000000FF><#5A4E7C>)</shadow>"
      );
      fine.setItemMeta(fMeta);
    }
    inv.setItem(FINE_PUMP_SLOT, fine);

    ItemStack bleed = XMaterial.HOPPER.parseItem();
    ItemMeta bMeta = bleed.getItemMeta();
    if(bMeta != null) {
      HalloweenChat.setItemName(bMeta, "<shadow:#000000FF><#5A4E7C>[</shadow><#FF3B47>วาล์วระบายลม -3 PSI<shadow:#000000FF><#5A4E7C>]</shadow>");
      HalloweenChat.setItemLore(
        bMeta,
        "",
        "<#F4F1DE>ระบายลมส่วนเกินออกจากล้อ",
        "",
        "<shadow:#000000FF><#5A4E7C>(</shadow><#B8B0C8>กดคลิกเพื่อลด -3 PSI<shadow:#000000FF><#5A4E7C>)</shadow>"
      );
      bleed.setItemMeta(bMeta);
    }
    inv.setItem(BLEED_VALVE_SLOT, bleed);

    ItemStack lockBtn = (inSweetSpot ? XMaterial.EMERALD_BLOCK : XMaterial.IRON_BARS).parseItem();
    ItemMeta lMeta = lockBtn.getItemMeta();
    if(lMeta != null) {
      if(inSweetSpot) {
        HalloweenChat.setItemName(lMeta, "<shadow:#000000FF><#5A4E7C>[</shadow><#6EEB83>ปิดล็อควาล์วลมยาง<shadow:#000000FF><#5A4E7C>]</shadow>");
        HalloweenChat.setItemLore(
          lMeta,
          "",
          "<#F4F1DE>แรงดันมาตรฐาน <#6EEB83>" + psi + " PSI",
          "",
          "<shadow:#000000FF><#5A4E7C>(</shadow><#B8B0C8>กดคลิกเพื่อบันทึกและจบภารกิจ<shadow:#000000FF><#5A4E7C>)</shadow>"
        );
      } else {
        HalloweenChat.setItemName(lMeta, "<shadow:#000000FF><#5A4E7C>[</shadow><#B8B0C8>ปิดล็อควาล์ว<shadow:#000000FF><#5A4E7C>]</shadow>");
        HalloweenChat.setItemLore(
          lMeta,
          "",
          "<#FF3B47>แรงดันต้องอยู่ระหว่าง 30 - 32 PSI",
          "",
          "<shadow:#000000FF><#5A4E7C>(</shadow><#B8B0C8>ปัจจุบัน " + psi + " PSI<shadow:#000000FF><#5A4E7C>)</shadow>"
        );
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

    if(rawSlot == 0 || rawSlot == 8) {
      XSound.UI_BUTTON_CLICK.play(player.getLocation(), 0.5F, 1.2F);
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
      HalloweenChat.sendActionBar(player, "<shadow:#000000FF><#5A4E7C>[</shadow><#FF7518>Task<shadow:#000000FF><#5A4E7C>]</shadow> <#F4F1DE>ระบายลมออก เหลือ <#FFB703>" + currentPsi + " PSI");
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
        HalloweenChat.sendActionBar(player, "<shadow:#000000FF><#5A4E7C>[</shadow><#6EEB83>Task<shadow:#000000FF><#5A4E7C>]</shadow> <#6EEB83>ล็อคแรงดันลมยางที่ " + currentPsi + " PSI สำเร็จ");

        Bukkit.getScheduler().runTaskLater(arena.getPlugin(), () -> {
          if(player.isOnline() && arena.getPlayers().contains(player) && !arena.getVotingManager().isVotingActive()) {
            if(isGui(player.getOpenInventory().getTitle())) {
              player.closeInventory();
            }
            arena.getTaskManager().completeTaskFromMinigame(player, TaskType.CHECK_TIRE_PRESSURE);
          }
        }, 8L);
      } else {
        XSound.ENTITY_VILLAGER_NO.play(player.getLocation(), 1F, 1F);
        HalloweenChat.sendActionBar(player, "<shadow:#000000FF><#5A4E7C>[</shadow><#FF3B47>Task<shadow:#000000FF><#5A4E7C>]</shadow> <#FF3B47>แรงดันต้องอยู่ระหว่าง 30-32 PSI ก่อนปิดล็อควาล์ว");
      }
      return true;
    }

    return true;
  }

  private void checkAndApplyPsi(Player player, int newPsi) {
    if(newPsi > MAX_SAFE_PSI) {
      XSound.ENTITY_GENERIC_EXPLODE.play(player.getLocation(), 0.7F, 1.6F);
      XSound.BLOCK_FIRE_EXTINGUISH.play(player.getLocation(), 0.9F, 0.6F);
      HalloweenChat.sendActionBar(player, "<shadow:#000000FF><#5A4E7C>[</shadow><#FF3B47>Task<shadow:#000000FF><#5A4E7C>]</shadow> <#FF3B47>แรงดันเกิน 34 PSI วาล์วนิรภัยปล่อยลมเหลือ 16 PSI");
      newPsi = 16;
    } else if(newPsi >= MIN_TARGET_PSI && newPsi <= MAX_TARGET_PSI) {
      HalloweenChat.sendActionBar(player, "<shadow:#000000FF><#5A4E7C>[</shadow><#6EEB83>Task<shadow:#000000FF><#5A4E7C>]</shadow> <#6EEB83>แรงดัน " + newPsi + " PSI พอดีแล้ว กดล็อควาล์วด้านล่างได้เลย");
      XSound.BLOCK_NOTE_BLOCK_PLING.play(player.getLocation(), 0.8F, 1.8F);
    } else {
      HalloweenChat.sendActionBar(player, "<shadow:#000000FF><#5A4E7C>[</shadow><#FF7518>Task<shadow:#000000FF><#5A4E7C>]</shadow> <#F4F1DE>แรงดันปัจจุบัน <#FFB703>" + newPsi + " PSI <shadow:#000000FF><#5A4E7C>(</shadow><#B8B0C8>เป้าหมาย 30-32 PSI<shadow:#000000FF><#5A4E7C>)</shadow>");
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
