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

public class DieselFuelMinigame implements TaskMinigame {

  public static final String GUI_TITLE = HalloweenChat.color("<#5A4E7C>[ <gradient:#FF5400:#FFB703><bold>สถานีเติมน้ำมันดีเซล</bold></gradient> <#5A4E7C>]");

  private static class FuelSession {
    int liters = 0;
  }

  private final Arena arena;
  private final Map<UUID, FuelSession> sessions = new HashMap<>();
  private final Set<UUID> completedSessions = new HashSet<>();

  private static final int INSTRUCTION_SLOT = 0;
  private static final int NOZZLE_SLOT = 11;
  private static final int PUMP_LEVER_SLOT = 15;
  private static final int SEAL_CAP_SLOT = 22;
  private static final int[] GAUGE_SLOTS = {1, 2, 3, 4, 5, 6, 7};

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

    Inventory inv = Bukkit.createInventory(null, 27, GUI_TITLE);
    buildGui(inv, session);
    player.openInventory(inv);

    XSound.BLOCK_CHEST_OPEN.play(player.getLocation(), 0.8F, 1.2F);
    VersionUtils.sendActionBar(player, HalloweenChat.color("<gradient:#FF5400:#FFB703><bold>[เติมน้ำมันดีเซล]</bold></gradient> <#FFB703>กดคันโยกทางขวาเพื่อปั๊มน้ำมันให้เต็ม 100 ลิตร!"));
    player.sendMessage(HalloweenChat.color("<gradient:#FF5400:#FFB703><bold>[ภารกิจเติมน้ำมันดีเซล]</bold></gradient> <#FFB703>กดคันโยกทางขวาเพื่อเติมน้ำมันให้เต็ม 100 ลิตร -> ปิดฝาด้านล่าง"));
  }

  private void buildGui(Inventory inv, FuelSession session) {
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
      iMeta.setDisplayName(HalloweenChat.color("<gradient:#FF5400:#FFB703><bold>วิธีทำภารกิจ: เติมน้ำมันดีเซล (Instructions)</bold></gradient>"));
      iMeta.setLore(HalloweenChat.colorLines(
        "<#F4F1DE>ขั้นตอนการทำภารกิจ:",
        "<#B8B0C8>1. คลิก <#FFB703>[คันโยกปั๊มน้ำมัน] <#B8B0C8>ที่ช่อง 15 ทางขวา เพื่อเติมน้ำมัน (+20 ลิตร)",
        "<#B8B0C8>2. ปั๊มจนเกจวัดด้านบนเต็ม <#6EEB83>100 ลิตร",
        "<#B8B0C8>3. เมื่อเต็ม 100 ลิตรแล้ว ให้คลิก <#6EEB83>[ปิดฝาถังน้ำมัน] <#B8B0C8>ด้านล่างเพื่อจบภารกิจ!"
      ));
      instruction.setItemMeta(iMeta);
    }
    inv.setItem(INSTRUCTION_SLOT, instruction);

    int filledBars = Math.min(7, (session.liters * 7) / 100);
    for(int i = 0; i < 7; i++) {
      ItemStack bar;
      if(i < filledBars) {
        if(session.liters >= 100) {
          bar = XMaterial.LIME_STAINED_GLASS_PANE.parseItem();
        } else {
          bar = XMaterial.ORANGE_STAINED_GLASS_PANE.parseItem();
        }
      } else {
        bar = XMaterial.BLACK_STAINED_GLASS_PANE.parseItem();
      }
      ItemMeta bMeta = bar.getItemMeta();
      if(bMeta != null) {
        bMeta.setDisplayName(HalloweenChat.color("<#FF7518>ระดับน้ำมัน: <#FFB703>" + session.liters + " <#5A4E7C>/ <#6EEB83>100 ลิตร"));
        bar.setItemMeta(bMeta);
      }
      inv.setItem(GAUGE_SLOTS[i], bar);
    }

    ItemStack canister = new ItemStack(session.liters >= 100 ? Material.LAVA_BUCKET : Material.BUCKET);
    ItemMeta cMeta = canister.getItemMeta();
    if(cMeta != null) {
      cMeta.setDisplayName(HalloweenChat.color(session.liters >= 100
        ? "<gradient:#38B000:#9EF01A><bold>[/] ถังน้ำมันดีเซลเต็ม 100 ลิตร!</bold></gradient>"
        : "<gradient:#FF5400:#FFB703><bold>ถังแกลลอนน้ำมันดีเซล</bold></gradient>"));
      cMeta.setLore(HalloweenChat.colorLines(
        "<#B8B0C8>ปริมาณปัจจุบัน: <#FFB703>" + session.liters + " <#5A4E7C>/ <#6EEB83>100 ลิตร",
        session.liters >= 100 ? "<#6EEB83>> ปิดฝาถังด้านล่างเพื่อจบภารกิจ!" : "<#B8B0C8>กดคันโยกทางขวาเพื่อเติมน้ำมัน"
      ));
      canister.setItemMeta(cMeta);
    }
    inv.setItem(NOZZLE_SLOT, canister);

    ItemStack divider = XMaterial.IRON_BARS.parseItem();
    ItemMeta dMeta = divider.getItemMeta();
    if(dMeta != null) {
      dMeta.setDisplayName(HalloweenChat.color("<#B8B0C8>ท่อจ่ายน้ำมัน"));
      divider.setItemMeta(dMeta);
    }
    inv.setItem(13, divider);

    ItemStack lever = XMaterial.TRIPWIRE_HOOK.parseItem();
    ItemMeta lMeta = lever.getItemMeta();
    if(lMeta != null) {
      if(session.liters >= 100) {
        lMeta.setDisplayName(HalloweenChat.color("<gradient:#38B000:#9EF01A><bold>[/] น้ำมันเต็มแล้ว</bold></gradient>"));
        lMeta.setLore(HalloweenChat.colorLines("<#B8B0C8>คลิกปุ่มปิดฝาด้านล่างได้เลย!"));
      } else {
        lMeta.setDisplayName(HalloweenChat.color("<#5A4E7C>[ <gradient:#FF5400:#FFB703><bold>กดคันโยกปั๊มน้ำมัน (+20 ลิตร)</bold></gradient> <#5A4E7C>]"));
        lMeta.setLore(HalloweenChat.colorLines(
          "<#B8B0C8>กดเพื่อสูบน้ำมันเข้าถังแกลลอน",
          "<#FFB703>> คลิกเพื่อปั๊มน้ำมัน"
        ));
      }
      lever.setItemMeta(lMeta);
    }
    inv.setItem(PUMP_LEVER_SLOT, lever);

    ItemStack cap;
    if(session.liters >= 100) {
      cap = XMaterial.EMERALD_BLOCK.parseItem();
    } else {
      cap = XMaterial.IRON_BARS.parseItem();
    }
    ItemMeta capMeta = cap.getItemMeta();
    if(capMeta != null) {
      if(session.liters >= 100) {
        capMeta.setDisplayName(HalloweenChat.color("<#5A4E7C>[ <gradient:#38B000:#9EF01A><bold>ปิดฝาถังน้ำมันและล็อค (เสร็จสิ้น) [/]</bold></gradient> <#5A4E7C>]"));
        capMeta.setLore(HalloweenChat.colorLines(
          "<#B8B0C8>น้ำมันเต็ม 100 ลิตรเรียบร้อยแล้ว",
          "<#6EEB83>> คลิกเพื่อจบภารกิจ!"
        ));
      } else {
        capMeta.setDisplayName(HalloweenChat.color("<#B8B0C8>[ ปิดฝาถังน้ำมัน (ยังไม่เต็ม) ]"));
        capMeta.setLore(HalloweenChat.colorLines(
          "<#FF3B47>ต้องเติมน้ำมันให้ครบ 100 ลิตรก่อน!",
          "<#F4F1DE>ปัจจุบัน: <#FFB703>" + session.liters + " <#5A4E7C>/ <#6EEB83>100 ลิตร"
        ));
      }
      cap.setItemMeta(capMeta);
    }
    inv.setItem(SEAL_CAP_SLOT, cap);
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

    if(rawSlot == INSTRUCTION_SLOT) {
      XSound.UI_BUTTON_CLICK.play(player.getLocation(), 0.5F, 1.2F);
      VersionUtils.sendActionBar(player, HalloweenChat.color("<#FFB703>กดคันโยกทางขวาเพื่อเติมน้ำมันให้เต็ม 100 ลิตร -> ปิดฝาด้านล่าง!"));
      return true;
    }

    Inventory topInv = player.getOpenInventory().getTopInventory();

    if(rawSlot == PUMP_LEVER_SLOT) {
      if(session.liters < 100) {
        session.liters = Math.min(100, session.liters + 20);
        XSound.BLOCK_PISTON_EXTEND.play(player.getLocation(), 0.8F, 1.3F);
        XSound.ITEM_BUCKET_FILL.play(player.getLocation(), 0.7F, 1.1F);

        if(session.liters >= 100) {
          XSound.BLOCK_NOTE_BLOCK_PLING.play(player.getLocation(), 1F, 1.8F);
          VersionUtils.sendActionBar(player, HalloweenChat.color("<#6EEB83>[/] น้ำมันเต็ม 100 ลิตรแล้ว! -> คลิก [ปิดฝาถังน้ำมัน] ด้านล่าง!"));
        } else {
          VersionUtils.sendActionBar(player, HalloweenChat.color("<gradient:#FF5400:#FFB703>[เติมน้ำมัน]</gradient> <#F4F1DE>ปริมาณ: <#FF7518>" + session.liters + " <#FFB703>/ 100 ลิตร"));
        }
      } else {
        XSound.UI_BUTTON_CLICK.play(player.getLocation(), 0.5F, 1.0F);
        VersionUtils.sendActionBar(player, HalloweenChat.color("<#6EEB83>น้ำมันเต็มแล้ว! รีบกดปิดฝาถังด้านล่าง"));
      }
      buildGui(topInv, session);
      return true;
    }

    if(rawSlot == SEAL_CAP_SLOT) {
      if(session.liters >= 100) {
        completedSessions.add(player.getUniqueId());
        sessions.remove(player.getUniqueId());

        XSound.BLOCK_ANVIL_USE.play(player.getLocation(), 0.8F, 1.6F);
        XSound.ENTITY_PLAYER_LEVELUP.play(player.getLocation(), 1F, 1.5F);
        VersionUtils.sendActionBar(player, HalloweenChat.color("<gradient:#38B000:#9EF01A><bold>[/] ปิดฝาถังน้ำมันดีเซลเรียบร้อย! พร้อมนำไปเติมรถบัส!</bold></gradient>"));

        Bukkit.getScheduler().runTaskLater(arena.getPlugin(), () -> {
          if(player.isOnline()) {
            player.closeInventory();
            arena.getTaskManager().completeTaskFromMinigame(player, TaskType.FIND_DIESEL_FUEL);
          }
        }, 10L);
      } else {
        XSound.ENTITY_VILLAGER_NO.play(player.getLocation(), 1F, 1F);
        VersionUtils.sendActionBar(player, HalloweenChat.color("<#FF3B47>[ยังไม่เสร็จ] ต้องปั๊มน้ำมันให้ครบ 100 ลิตรก่อนปิดฝาถัง!"));
      }
      return true;
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
