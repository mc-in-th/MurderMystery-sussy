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

public class EngineOilMinigame implements TaskMinigame {

  public static final String GUI_TITLE = HalloweenChat.color("<#5A4E7C>[ <gradient:#FF5400:#FFB703><bold>เช็คน้ำมันเครื่องยนต์</bold></gradient> <#5A4E7C>]");

  private static class OilSession {
    int oilLevel = 1;
  }

  private final Arena arena;
  private final Map<UUID, OilSession> sessions = new HashMap<>();
  private final Set<UUID> completedSessions = new HashSet<>();

  private static final int INSTRUCTION_SLOT = 0;
  private static final int OIL_CAN_SLOT = 13;
  private static final int CAP_SLOT = 22;
  private static final int[] GAUGE_SLOTS = {2, 3, 4, 5, 6};

  public EngineOilMinigame(Arena arena) {
    this.arena = arena;
  }

  @Override
  public TaskType getTaskType() {
    return TaskType.CHECK_ENGINE_OIL;
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
    return plain.contains("น้ำมันเครื่อง") || plain.contains("Oil");
  }

  @Override
  public void open(Player player) {
    completedSessions.remove(player.getUniqueId());
    OilSession session = new OilSession();
    sessions.put(player.getUniqueId(), session);

    Inventory inv = Bukkit.createInventory(null, 27, GUI_TITLE);
    buildGui(inv, session);
    player.openInventory(inv);

    XSound.BLOCK_CHEST_OPEN.play(player.getLocation(), 0.8F, 1.2F);
    HalloweenChat.sendActionBar(player, HalloweenChat.color("<gradient:#FF5400:#FFB703><bold>[เช็คน้ำมันเครื่อง]</bold></gradient> <#FFB703>คลิกกระป๋องน้ำมันตรงกลางเพื่อเติมให้ถึงขีด MAX สีเขียว!"));
    player.sendMessage(HalloweenChat.color("<gradient:#FF5400:#FFB703><bold>[ภารกิจน้ำมันเครื่อง]</bold></gradient> <#FFB703>คลิกกระป๋องตรงกลางเพื่อเติมน้ำมันให้ถึงขีด MAX แล้วกดปิดฝาด้านล่าง"));
  }

  private void buildGui(Inventory inv, OilSession session) {
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
      iMeta.setDisplayName(HalloweenChat.color("<gradient:#FF5400:#FFB703><bold>วิธีทำภารกิจ: เช็คน้ำมันเครื่องยนต์ (Instructions)</bold></gradient>"));
      iMeta.setLore(HalloweenChat.colorLines(
        "<#F4F1DE>ขั้นตอนการทำภารกิจ:",
        "<#B8B0C8>1. คลิก <#FF7518>[กระป๋องน้ำมันเครื่อง] <#B8B0C8>ตรงกลาง เพื่อเติมน้ำมัน",
        "<#B8B0C8>2. เติมจนเกจวัดด้านบนถึงขีด <#6EEB83>MAX สีเขียว (ระดับ 3)",
        "<#B8B0C8>3. เมื่อถึงขีด MAX ให้คลิก <#6EEB83>[ปิดฝาน้ำมันเครื่อง] <#B8B0C8>ด้านล่างเพื่อจบภารกิจ!"
      ));
      instruction.setItemMeta(iMeta);
    }
    inv.setItem(INSTRUCTION_SLOT, instruction);

    for(int i = 0; i < 5; i++) {
      ItemStack bar;
      if(i == 0) {
        bar = XMaterial.RED_STAINED_GLASS_PANE.parseItem();
      } else if(i == 1) {
        bar = (session.oilLevel >= 1) ? XMaterial.ORANGE_STAINED_GLASS_PANE.parseItem() : XMaterial.BLACK_STAINED_GLASS_PANE.parseItem();
      } else if(i == 2) {
        bar = (session.oilLevel >= 2) ? XMaterial.YELLOW_STAINED_GLASS_PANE.parseItem() : XMaterial.BLACK_STAINED_GLASS_PANE.parseItem();
      } else if(i == 3) {
        bar = (session.oilLevel >= 3) ? XMaterial.LIME_STAINED_GLASS_PANE.parseItem() : XMaterial.BLACK_STAINED_GLASS_PANE.parseItem();
      } else {
        bar = XMaterial.PURPLE_STAINED_GLASS_PANE.parseItem();
      }

      ItemMeta bMeta = bar.getItemMeta();
      if(bMeta != null) {
        if(i == 0) {
          bMeta.setDisplayName(HalloweenChat.color("<#FF3B47>* ขีด MIN (ระดับต่ำสุด)"));
        } else if(i == 1) {
          bMeta.setDisplayName(HalloweenChat.color(session.oilLevel >= 1 ? "<#FF7518>* ระดับต่ำ (Low)" : "<#5A4E7C>o ยังไม่ถึงระดับ"));
        } else if(i == 2) {
          bMeta.setDisplayName(HalloweenChat.color(session.oilLevel >= 2 ? "<#FFB703>* ระดับปานกลาง (Medium)" : "<#5A4E7C>o ยังไม่ถึงระดับ"));
        } else if(i == 3) {
          bMeta.setDisplayName(HalloweenChat.color(session.oilLevel >= 3 ? "<#6EEB83>* ขีด MAX (ระดับมาตรฐานสมบูรณ์) [/]" : "<#5A4E7C>o ขีด MAX (เป้าหมาย)"));
        } else {
          bMeta.setDisplayName(HalloweenChat.color("<#5A4E7C> "));
        }
        bar.setItemMeta(bMeta);
      }
      inv.setItem(GAUGE_SLOTS[i], bar);
    }

    ItemStack oilCan;
    if(session.oilLevel >= 3) {
      oilCan = new ItemStack(Material.HONEY_BOTTLE);
    } else {
      oilCan = new ItemStack(Material.POTION);
    }
    ItemMeta oMeta = oilCan.getItemMeta();
    if(oMeta != null) {
      if(session.oilLevel >= 3) {
        oMeta.setDisplayName(HalloweenChat.color("<gradient:#38B000:#9EF01A><bold>[/] น้ำมันเครื่องถึงขีด MAX แล้ว!</bold></gradient>"));
        oMeta.setLore(HalloweenChat.colorLines(
          "<#B8B0C8>ระดับน้ำมันเครื่องเต็มตามมาตรฐานเรียบร้อย",
          "<#6EEB83>> คลิกปุ่ม [ปิดฝาน้ำมันเครื่อง] ด้านล่าง!"
        ));
      } else {
        oMeta.setDisplayName(HalloweenChat.color("<#5A4E7C>[ <gradient:#FF5400:#FFB703><bold>คลิกเติมน้ำมันเครื่อง (+1 ขีด)</bold></gradient> <#5A4E7C>]"));
        oMeta.setLore(HalloweenChat.colorLines(
          "<#B8B0C8>ระดับปัจจุบัน: <#FFB703>" + (session.oilLevel == 1 ? "ต่ำ (Low)" : "ปานกลาง (Medium)"),
          "<#B8B0C8>เป้าหมาย: <#6EEB83>ขีด MAX สีเขียว",
          "<#FFB703>> คลิกที่นี่เพื่อเทน้ำมันเพิ่ม"
        ));
      }
      oilCan.setItemMeta(oMeta);
    }
    inv.setItem(OIL_CAN_SLOT, oilCan);

    ItemStack cap;
    if(session.oilLevel >= 3) {
      cap = XMaterial.EMERALD_BLOCK.parseItem();
    } else {
      cap = XMaterial.IRON_BARS.parseItem();
    }
    ItemMeta cMeta = cap.getItemMeta();
    if(cMeta != null) {
      if(session.oilLevel >= 3) {
        cMeta.setDisplayName(HalloweenChat.color("<#5A4E7C>[ <gradient:#38B000:#9EF01A><bold>ปิดฝาน้ำมันเครื่อง (เสร็จสิ้น) [/]</bold></gradient> <#5A4E7C>]"));
        cMeta.setLore(HalloweenChat.colorLines(
          "<#B8B0C8>น้ำมันเครื่องได้ระดับมาตรฐานแล้ว",
          "<#6EEB83>> คลิกเพื่อจบภารกิจ!"
        ));
      } else {
        cMeta.setDisplayName(HalloweenChat.color("<#B8B0C8>[ ปิดฝาน้ำมันเครื่อง (ยังไม่เต็ม) ]"));
        cMeta.setLore(HalloweenChat.colorLines(
          "<#FF3B47>ต้องเติมน้ำมันให้ถึงขีด MAX สีเขียวก่อน!",
          "<#F4F1DE>ปัจจุบัน: <#FFB703>ระดับ " + session.oilLevel + " / 3"
        ));
      }
      cap.setItemMeta(cMeta);
    }
    inv.setItem(CAP_SLOT, cap);
  }

  @Override
  public boolean handleClick(Player player, int rawSlot, ClickType clickType) {
    if(completedSessions.contains(player.getUniqueId())) {
      return true;
    }

    OilSession session = sessions.get(player.getUniqueId());
    if(session == null) {
      return true;
    }

    if(rawSlot == INSTRUCTION_SLOT) {
      XSound.UI_BUTTON_CLICK.play(player.getLocation(), 0.5F, 1.2F);
      HalloweenChat.sendActionBar(player, HalloweenChat.color("<#FFB703>คลิกกระป๋องตรงกลางเพื่อเติมน้ำมันให้ถึงขีด MAX แล้วกดปิดฝาด้านล่าง!"));
      return true;
    }

    Inventory topInv = player.getOpenInventory().getTopInventory();

    if(rawSlot == OIL_CAN_SLOT) {
      if(session.oilLevel < 3) {
        session.oilLevel++;
        XSound.ITEM_BUCKET_EMPTY.play(player.getLocation(), 0.9F, 1.3F);
        XSound.ITEM_BOTTLE_FILL.play(player.getLocation(), 0.8F, 1.2F);

        if(session.oilLevel >= 3) {
          XSound.BLOCK_NOTE_BLOCK_PLING.play(player.getLocation(), 1F, 1.8F);
          HalloweenChat.sendActionBar(player, HalloweenChat.color("<#6EEB83>[/] น้ำมันเครื่องถึงขีด MAX แล้ว! -> คลิก [ปิดฝาน้ำมันเครื่อง] ด้านล่าง!"));
        } else {
          HalloweenChat.sendActionBar(player, HalloweenChat.color("<gradient:#FF5400:#FFB703>[เติมน้ำมันเครื่อง]</gradient> <#FFB703>ระดับปานกลาง (เติมอีก 1 ครั้งเพื่อถึงขีด MAX)"));
        }
      } else {
        XSound.UI_BUTTON_CLICK.play(player.getLocation(), 0.5F, 1.2F);
        HalloweenChat.sendActionBar(player, HalloweenChat.color("<#6EEB83>น้ำมันเครื่องเต็มขีด MAX แล้ว! ให้คลิกปิดฝาด้านล่าง"));
      }
      buildGui(topInv, session);
      return true;
    }

    if(rawSlot == CAP_SLOT) {
      if(session.oilLevel >= 3) {
        completedSessions.add(player.getUniqueId());
        sessions.remove(player.getUniqueId());

        XSound.BLOCK_ANVIL_USE.play(player.getLocation(), 0.8F, 1.5F);
        XSound.ENTITY_PLAYER_LEVELUP.play(player.getLocation(), 1F, 1.5F);
        HalloweenChat.sendActionBar(player, HalloweenChat.color("<gradient:#38B000:#9EF01A><bold>[/] ปิดฝาน้ำมันเครื่องเรียบร้อย! เครื่องยนต์พร้อมทำงาน!</bold></gradient>"));

        Bukkit.getScheduler().runTaskLater(arena.getPlugin(), () -> {
          if(player.isOnline()) {
            player.closeInventory();
            arena.getTaskManager().completeTaskFromMinigame(player, TaskType.CHECK_ENGINE_OIL);
          }
        }, 10L);
      } else {
        XSound.ENTITY_VILLAGER_NO.play(player.getLocation(), 1F, 1F);
        HalloweenChat.sendActionBar(player, HalloweenChat.color("<#FF3B47>[ยังไม่เสร็จ] ต้องคลิกเติมน้ำมันตรงกลางให้ถึงขีด MAX ก่อน!"));
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
