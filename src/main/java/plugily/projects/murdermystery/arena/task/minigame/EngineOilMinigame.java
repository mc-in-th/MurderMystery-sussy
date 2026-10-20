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

public class EngineOilMinigame implements TaskMinigame {

  public static final String GUI_TITLE = "§8[ §6 เช็คน้ำมันเครื่องยนต์ §8]";

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
    return title != null && (title.contains("น้ำมันเครื่อง") || title.contains("Oil"));
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
    VersionUtils.sendActionBar(player, "§e[เช็คน้ำมันเครื่อง] คลิกกระป๋องน้ำมันตรงกลางเพื่อเติมให้ถึงขีด MAX สีเขียว!");
    player.sendMessage("§6[ภารกิจน้ำมันเครื่อง] §eคลิกกระป๋องตรงกลางเพื่อเติมน้ำมันให้ถึงขีด MAX แล้วกดปิดฝาด้านล่าง");
  }

  private void buildGui(Inventory inv, OilSession session) {
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
      iMeta.setDisplayName("§e§l วิธีทำภารกิจ: เช็คน้ำมันเครื่องยนต์ (Instructions)");
      iMeta.setLore(Arrays.asList(
        "§fขั้นตอนการทำภารกิจ:",
        "§71. คลิก §6[กระป๋องน้ำมันเครื่อง] §7ตรงกลาง เพื่อเติมน้ำมัน",
        "§72. เติมจนเกจวัดด้านบนถึงขีด §aMAX สีเขียว (ระดับ 3)",
        "§73. เมื่อถึงขีด MAX ให้คลิก §a[ ปิดฝาน้ำมันเครื่อง] §7ด้านล่างเพื่อจบภารกิจ!"
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
        bar = XMaterial.GRAY_STAINED_GLASS_PANE.parseItem();
      }

      ItemMeta bMeta = bar.getItemMeta();
      if(bMeta != null) {
        if(i == 0) {
          bMeta.setDisplayName("§c* ขีด MIN (ระดับต่ำสุด)");
        } else if(i == 1) {
          bMeta.setDisplayName(session.oilLevel >= 1 ? "§6* ระดับต่ำ (Low)" : "§8o ยังไม่ถึงระดับ");
        } else if(i == 2) {
          bMeta.setDisplayName(session.oilLevel >= 2 ? "§e* ระดับปานกลาง (Medium)" : "§8o ยังไม่ถึงระดับ");
        } else if(i == 3) {
          bMeta.setDisplayName(session.oilLevel >= 3 ? "§a* ขีด MAX (ระดับมาตรฐานสมบูรณ์) [/]" : "§8o ขีด MAX (เป้าหมาย)");
        } else {
          bMeta.setDisplayName("§7 ");
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
        oMeta.setDisplayName("§a§l[/] น้ำมันเครื่องถึงขีด MAX แล้ว!");
        oMeta.setLore(Arrays.asList(
          "§7ระดับน้ำมันเครื่องเต็มตามมาตรฐานเรียบร้อย",
          "§a> คลิกปุ่ม [ ปิดฝาน้ำมันเครื่อง] ด้านล่าง!"
        ));
      } else {
        oMeta.setDisplayName("§6§l[ คลิกเติมน้ำมันเครื่อง (+1 ขีด) ]");
        oMeta.setLore(Arrays.asList(
          "§7ระดับปัจจุบัน: §e" + (session.oilLevel == 1 ? "ต่ำ (Low)" : "ปานกลาง (Medium)"),
          "§7เป้าหมาย: §aขีด MAX สีเขียว",
          "§e> คลิกที่นี่เพื่อเทน้ำมันเพิ่ม"
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
        cMeta.setDisplayName("§a§l[ ปิดฝาน้ำมันเครื่อง (เสร็จสิ้น) ] [/]");
        cMeta.setLore(Arrays.asList(
          "§7น้ำมันเครื่องได้ระดับมาตรฐานแล้ว",
          "§a> คลิกเพื่อจบภารกิจ!"
        ));
      } else {
        cMeta.setDisplayName("§7[ ปิดฝาน้ำมันเครื่อง (ยังไม่เต็ม) ]");
        cMeta.setLore(Arrays.asList(
          "§cต้องเติมน้ำมันให้ถึงขีด MAX สีเขียวก่อน!",
          "§fปัจจุบัน: §eระดับ " + session.oilLevel + " / 3"
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
      VersionUtils.sendActionBar(player, "§e คลิกกระป๋องตรงกลางเพื่อเติมน้ำมันให้ถึงขีด MAX แล้วกดปิดฝาด้านล่าง!");
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
          VersionUtils.sendActionBar(player, "§a[/] น้ำมันเครื่องถึงขีด MAX แล้ว! -> คลิก [ ปิดฝาน้ำมันเครื่อง] ด้านล่าง!");
        } else {
          VersionUtils.sendActionBar(player, "§6[เติมน้ำมันเครื่อง] ระดับปานกลาง (เติมอีก 1 ครั้งเพื่อถึงขีด MAX)");
        }
      } else {
        XSound.UI_BUTTON_CLICK.play(player.getLocation(), 0.5F, 1.2F);
        VersionUtils.sendActionBar(player, "§aน้ำมันเครื่องเต็มขีด MAX แล้ว! ให้คลิกปิดฝาด้านล่าง");
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
        VersionUtils.sendActionBar(player, "§a§l[/] ปิดฝาน้ำมันเครื่องเรียบร้อย! เครื่องยนต์พร้อมทำงาน!");

        Bukkit.getScheduler().runTaskLater(arena.getPlugin(), () -> {
          if(player.isOnline()) {
            player.closeInventory();
            arena.getTaskManager().completeTaskFromMinigame(player, TaskType.CHECK_ENGINE_OIL);
          }
        }, 10L);
      } else {
        XSound.ENTITY_VILLAGER_NO.play(player.getLocation(), 1F, 1F);
        VersionUtils.sendActionBar(player, "§c[ยังไม่เสร็จ] ต้องคลิกเติมน้ำมันตรงกลางให้ถึงขีด MAX ก่อน!");
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
