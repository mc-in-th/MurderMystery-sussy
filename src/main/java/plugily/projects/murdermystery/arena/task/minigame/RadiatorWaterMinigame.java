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

public class RadiatorWaterMinigame implements TaskMinigame {

  public static final String GUI_TITLE = "§8[ §6 เช็คระดับน้ำในหม้อน้ำ §8]";

  private static class RadiatorSession {
    boolean capOpen = false;
    int waterLevel = 1;
  }

  private final Arena arena;
  private final Map<UUID, RadiatorSession> sessions = new HashMap<>();
  private final Set<UUID> completedSessions = new HashSet<>();

  private static final int INSTRUCTION_SLOT = 0;
  private static final int CAP_SLOT = 11;
  private static final int FUNNEL_SLOT = 13;
  private static final int BOTTLE_SLOT = 15;
  private static final int SEAL_BUTTON_SLOT = 22;
  private static final int[] GAUGE_SLOTS = {2, 3, 4, 5, 6};

  public RadiatorWaterMinigame(Arena arena) {
    this.arena = arena;
  }

  @Override
  public TaskType getTaskType() {
    return TaskType.CHECK_RADIATOR_WATER;
  }

  @Override
  public String getGuiTitle() {
    return GUI_TITLE;
  }

  @Override
  public boolean isGui(String title) {
    return title != null && (title.contains("หม้อน้ำ") || title.contains("Radiator"));
  }

  @Override
  public void open(Player player) {
    completedSessions.remove(player.getUniqueId());
    RadiatorSession session = new RadiatorSession();
    sessions.put(player.getUniqueId(), session);

    Inventory inv = Bukkit.createInventory(null, 27, GUI_TITLE);
    buildGui(inv, session);
    player.openInventory(inv);

    XSound.BLOCK_CHEST_OPEN.play(player.getLocation(), 0.8F, 1.2F);
    VersionUtils.sendActionBar(player, "§e[เช็คหม้อน้ำ] เปิดฝาหม้อน้ำทางซ้าย แล้วเทน้ำยาหล่อเย็นทางขวา!");
    player.sendMessage("§6[ภารกิจหม้อน้ำ] §eเปิดฝาหม้อน้ำทางซ้าย -> เทน้ำยาทางขวาให้เต็ม -> ปิดฝาด้านล่าง");
  }

  private void buildGui(Inventory inv, RadiatorSession session) {
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
      iMeta.setDisplayName("§e§l วิธีทำภารกิจ: เช็คระดับน้ำในหม้อน้ำ (Instructions)");
      iMeta.setLore(Arrays.asList(
        "§fขั้นตอนการทำภารกิจ:",
        "§71. คลิก §e[ฝาหม้อน้ำ] §7ที่ช่อง 11 ทางซ้ายเพื่อเปิดฝา",
        "§72. คลิก §b[ขวดน้ำยาหล่อเย็น] §7ที่ช่อง 15 ทางขวาเพื่อเทน้ำยา",
        "§73. เติมจนเกจวัดด้านบนถึง §aขีดมาตรฐานสีเขียว (Optimal)",
        "§74. เมื่อเต็มแล้ว ให้คลิก §a[ ปิดฝาหม้อน้ำ] §7ด้านล่างเพื่อจบภารกิจ!"
      ));
      instruction.setItemMeta(iMeta);
    }
    inv.setItem(INSTRUCTION_SLOT, instruction);

    for(int i = 0; i < 5; i++) {
      ItemStack bar;
      if(i == 0) {
        bar = XMaterial.RED_STAINED_GLASS_PANE.parseItem();
      } else if(i == 1) {
        bar = (session.waterLevel >= 1) ? XMaterial.LIGHT_BLUE_STAINED_GLASS_PANE.parseItem() : XMaterial.BLACK_STAINED_GLASS_PANE.parseItem();
      } else if(i == 2) {
        bar = (session.waterLevel >= 2) ? XMaterial.LIGHT_BLUE_STAINED_GLASS_PANE.parseItem() : XMaterial.BLACK_STAINED_GLASS_PANE.parseItem();
      } else if(i == 3) {
        bar = (session.waterLevel >= 3) ? XMaterial.LIME_STAINED_GLASS_PANE.parseItem() : XMaterial.BLACK_STAINED_GLASS_PANE.parseItem();
      } else {
        bar = XMaterial.GRAY_STAINED_GLASS_PANE.parseItem();
      }

      ItemMeta bMeta = bar.getItemMeta();
      if(bMeta != null) {
        if(i == 0) {
          bMeta.setDisplayName("§c* ระดับต่ำ (Low)");
        } else if(i == 3) {
          bMeta.setDisplayName(session.waterLevel >= 3 ? "§a* ขีดมาตรฐานสีเขียว (Optimal) [/]" : "§8o ขีดมาตรฐาน (เป้าหมาย)");
        } else {
          bMeta.setDisplayName(session.waterLevel >= i ? "§b* ระดับน้ำหล่อเย็น" : "§7 ");
        }
        bar.setItemMeta(bMeta);
      }
      inv.setItem(GAUGE_SLOTS[i], bar);
    }

    ItemStack cap;
    if(session.capOpen) {
      cap = XMaterial.BREWING_STAND.parseItem();
      ItemMeta cMeta = cap.getItemMeta();
      if(cMeta != null) {
        cMeta.setDisplayName("§a§l[/] เปิดฝาหม้อน้ำแล้ว");
        cMeta.setLore(Arrays.asList("§7พร้อมเติมน้ำยาหล่อเย็นทางขวา"));
        cap.setItemMeta(cMeta);
      }
    } else {
      cap = XMaterial.CAULDRON.parseItem();
      ItemMeta cMeta = cap.getItemMeta();
      if(cMeta != null) {
        cMeta.setDisplayName("§e§l[ คลิกเปิดฝาหม้อน้ำ ]");
        cMeta.setLore(Arrays.asList(
          "§7ฝาหม้อน้ำปิดอยู่",
          "§e> คลิกเพื่อเปิดฝาหม้อน้ำ"
        ));
        cap.setItemMeta(cMeta);
      }
    }
    inv.setItem(CAP_SLOT, cap);

    ItemStack funnel = new ItemStack(session.waterLevel >= 3 ? Material.WATER_BUCKET : Material.BUCKET);
    ItemMeta fMeta = funnel.getItemMeta();
    if(fMeta != null) {
      fMeta.setDisplayName(session.waterLevel >= 3 ? "§a§l[/] น้ำยาหล่อเย็นเต็มมาตรฐาน!" : "§b§lช่องเติมน้ำยาหม้อน้ำ");
      fMeta.setLore(Arrays.asList(
        "§7สถานะ: " + (!session.capOpen ? "§cยังไม่ได้เปิดฝา" : (session.waterLevel >= 3 ? "§aเต็มเรียบร้อย" : "§eเปิดฝาแล้ว พร้อมเติมน้ำยา")),
        session.waterLevel >= 3 ? "§a> ปิดฝาหม้อน้ำด้านล่างเพื่อจบภารกิจ!" : " "
      ));
      funnel.setItemMeta(fMeta);
    }
    inv.setItem(FUNNEL_SLOT, funnel);

    ItemStack bottle = new ItemStack(Material.POTION);
    ItemMeta bMeta = bottle.getItemMeta();
    if(bMeta != null) {
      if(!session.capOpen) {
        bMeta.setDisplayName("§7[ ขวดน้ำยาหล่อเย็น (เปิดฝาก่อน) ]");
        bMeta.setLore(Arrays.asList("§cต้องคลิกเปิดฝาหม้อน้ำทางซ้ายก่อน!"));
      } else if(session.waterLevel >= 3) {
        bMeta.setDisplayName("§a§l[/] น้ำยาเต็มถึงขีดมาตรฐานแล้ว");
        bMeta.setLore(Arrays.asList("§a> คลิกปุ่มปิดฝาด้านล่างได้เลย!"));
      } else {
        bMeta.setDisplayName("§b§l[ คลิกเทน้ำยาหล่อเย็น (+1 ระดับ) ]");
        bMeta.setLore(Arrays.asList(
          "§7เทน้ำยาลงในช่องหม้อน้ำ",
          "§e> คลิกเพื่อเทน้ำยา"
        ));
      }
      bottle.setItemMeta(bMeta);
    }
    inv.setItem(BOTTLE_SLOT, bottle);

    ItemStack seal;
    if(session.capOpen && session.waterLevel >= 3) {
      seal = XMaterial.EMERALD_BLOCK.parseItem();
    } else {
      seal = XMaterial.IRON_BARS.parseItem();
    }
    ItemMeta sMeta = seal.getItemMeta();
    if(sMeta != null) {
      if(session.capOpen && session.waterLevel >= 3) {
        sMeta.setDisplayName("§a§l[ ปิดฝาหม้อน้ำให้แน่น (เสร็จสิ้น) ] [/]");
        sMeta.setLore(Arrays.asList(
          "§7น้ำยาหล่อเย็นได้ระดับมาตรฐานแล้ว",
          "§a> คลิกเพื่อจบภารกิจ!"
        ));
      } else {
        sMeta.setDisplayName("§7[ ปิดฝาหม้อน้ำ (ยังไม่พร้อม) ]");
        sMeta.setLore(Arrays.asList(
          "§cต้องเปิดฝาและเติมน้ำยาให้ถึงขีดสีเขียวก่อน!",
          "§fระดับปัจจุบัน: §e" + session.waterLevel + " / 3"
        ));
      }
      seal.setItemMeta(sMeta);
    }
    inv.setItem(SEAL_BUTTON_SLOT, seal);
  }

  @Override
  public boolean handleClick(Player player, int rawSlot, ClickType clickType) {
    if(completedSessions.contains(player.getUniqueId())) {
      return true;
    }

    RadiatorSession session = sessions.get(player.getUniqueId());
    if(session == null) {
      return true;
    }

    if(rawSlot == INSTRUCTION_SLOT) {
      XSound.UI_BUTTON_CLICK.play(player.getLocation(), 0.5F, 1.2F);
      VersionUtils.sendActionBar(player, "§e เปิดฝาทางซ้าย -> เทน้ำยาทางขวาให้เต็ม -> ปิดฝาด้านล่าง!");
      return true;
    }

    Inventory topInv = player.getOpenInventory().getTopInventory();

    if(rawSlot == CAP_SLOT) {
      if(!session.capOpen) {
        session.capOpen = true;
        XSound.BLOCK_IRON_TRAPDOOR_OPEN.play(player.getLocation(), 1F, 1.2F);
        XSound.BLOCK_FIRE_EXTINGUISH.play(player.getLocation(), 0.8F, 1.5F);
        VersionUtils.sendActionBar(player, "§a[/] เปิดฝาหม้อน้ำแล้ว! -> คลิกขวดน้ำยาทางขวา (ช่อง 15) เพื่อเทน้ำยา");
      } else {
        XSound.UI_BUTTON_CLICK.play(player.getLocation(), 0.5F, 1.2F);
      }
      buildGui(topInv, session);
      return true;
    }

    if(rawSlot == BOTTLE_SLOT) {
      if(!session.capOpen) {
        XSound.ENTITY_VILLAGER_NO.play(player.getLocation(), 0.8F, 1F);
        VersionUtils.sendActionBar(player, "§c[เปิดฝาก่อน] กรุณาคลิกเปิดฝาหม้อน้ำทางซ้าย (ช่อง 11) ก่อน!");
        return true;
      }

      if(session.waterLevel < 3) {
        session.waterLevel++;
        XSound.ITEM_BUCKET_EMPTY.play(player.getLocation(), 0.9F, 1.2F);
        XSound.BLOCK_WATER_AMBIENT.play(player.getLocation(), 0.8F, 1.5F);

        if(session.waterLevel >= 3) {
          XSound.BLOCK_NOTE_BLOCK_PLING.play(player.getLocation(), 1F, 1.8F);
          VersionUtils.sendActionBar(player, "§a[/] น้ำยาหล่อเย็นเต็มขีดสีเขียวแล้ว! -> คลิก [ ปิดฝาหม้อน้ำ] ด้านล่าง!");
        } else {
          VersionUtils.sendActionBar(player, "§b[เติมน้ำยา] เทน้ำยาเพิ่มแล้ว (ระดับ " + session.waterLevel + " / 3)");
        }
      } else {
        XSound.UI_BUTTON_CLICK.play(player.getLocation(), 0.5F, 1.2F);
        VersionUtils.sendActionBar(player, "§aน้ำยาหล่อเย็นเต็มขีดเขียวแล้ว! ให้กดปิดฝาด้านล่าง");
      }

      buildGui(topInv, session);
      return true;
    }

    if(rawSlot == SEAL_BUTTON_SLOT) {
      if(session.capOpen && session.waterLevel >= 3) {
        completedSessions.add(player.getUniqueId());
        sessions.remove(player.getUniqueId());

        XSound.BLOCK_ANVIL_USE.play(player.getLocation(), 0.8F, 1.5F);
        XSound.ENTITY_PLAYER_LEVELUP.play(player.getLocation(), 1F, 1.5F);
        VersionUtils.sendActionBar(player, "§a§l[/] ปิดฝาหม้อน้ำเรียบร้อย! ระบบระบายความร้อนพร้อมทำงาน!");

        Bukkit.getScheduler().runTaskLater(arena.getPlugin(), () -> {
          if(player.isOnline()) {
            player.closeInventory();
            arena.getTaskManager().completeTaskFromMinigame(player, TaskType.CHECK_RADIATOR_WATER);
          }
        }, 10L);
      } else {
        XSound.ENTITY_VILLAGER_NO.play(player.getLocation(), 1F, 1F);
        VersionUtils.sendActionBar(player, "§c[ยังไม่พร้อม] ต้องเปิดฝาและเติมน้ำยาให้ถึงขีดสีเขียวก่อน!");
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
