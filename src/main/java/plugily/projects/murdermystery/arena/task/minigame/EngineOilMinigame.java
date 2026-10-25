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

public class EngineOilMinigame implements TaskMinigame {

  public static final String GUI_TITLE = HalloweenChat.color("<shadow:#000000FF><#5A4E7C>[</shadow><#FF7518>เช็คน้ำมันเครื่องยนต์<shadow:#000000FF><#5A4E7C>]</shadow>");

  private static class OilSession {
    int oilLevel = 1;
  }

  private final Arena arena;
  private final Map<UUID, OilSession> sessions = new HashMap<>();
  private final Set<UUID> completedSessions = new HashSet<>();

  private static final int INSTRUCTION_SLOT = 0;
  private static final int OIL_CAN_SLOT = 13;
  private static final int CAP_SLOT = 22;
  private static final int[] GAUGE_SLOTS = {2, 3, 4, 5};

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
    HalloweenChat.sendActionBar(player, "<shadow:#000000FF><#5A4E7C>[</shadow><#FF7518>Task<shadow:#000000FF><#5A4E7C>]</shadow> <#F4F1DE>คลิกกระป๋องน้ำมันตรงกลางเพื่อเติมให้ถึงขีดสีเขียว แล้วกดปิดฝาด้านล่าง");
  }

  private void buildGui(Inventory inv, OilSession session) {
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
      HalloweenChat.setItemName(iMeta, "<shadow:#000000FF><#5A4E7C>[</shadow><#FF7518>Engine Oil<shadow:#000000FF><#5A4E7C>]</shadow>");
      HalloweenChat.setItemLore(
        iMeta,
        "",
        "<#F4F1DE>1. คลิกกระป๋องน้ำมันเครื่องตรงกลางเพื่อเติมน้ำมัน",
        "<#F4F1DE>2. เติมจนเกจวัดด้านบนถึงขีดมาตรฐานสีเขียว",
        "<#F4F1DE>3. กดปิดฝาน้ำมันเครื่องด้านล่างเพื่อจบภารกิจ"
      );
      instruction.setItemMeta(iMeta);
    }
    inv.setItem(INSTRUCTION_SLOT, instruction);

    for(int i = 0; i < GAUGE_SLOTS.length; i++) {
      ItemStack bar;
      if(i == 0) {
        bar = XMaterial.RED_STAINED_GLASS_PANE.parseItem();
      } else if(i == 1) {
        bar = (session.oilLevel >= 1 ? XMaterial.ORANGE_STAINED_GLASS_PANE : XMaterial.GRAY_STAINED_GLASS_PANE).parseItem();
      } else if(i == 2) {
        bar = (session.oilLevel >= 2 ? XMaterial.YELLOW_STAINED_GLASS_PANE : XMaterial.GRAY_STAINED_GLASS_PANE).parseItem();
      } else {
        bar = (session.oilLevel >= 3 ? XMaterial.LIME_STAINED_GLASS_PANE : XMaterial.GRAY_STAINED_GLASS_PANE).parseItem();
      }

      ItemMeta bMeta = bar.getItemMeta();
      if(bMeta != null) {
        if(i == 0) {
          HalloweenChat.setItemName(bMeta, "<shadow:#000000FF><#5A4E7C>[</shadow><#FF3B47>ขีดต่ำสุด<shadow:#000000FF><#5A4E7C>]</shadow>");
        } else if(i == 1) {
          HalloweenChat.setItemName(bMeta, session.oilLevel >= 1
            ? "<shadow:#000000FF><#5A4E7C>[</shadow><#FF7518>ระดับต่ำ<shadow:#000000FF><#5A4E7C>]</shadow>"
            : "<shadow:#000000FF><#5A4E7C>[</shadow><#B8B0C8>ว่าง<shadow:#000000FF><#5A4E7C>]</shadow>");
        } else if(i == 2) {
          HalloweenChat.setItemName(bMeta, session.oilLevel >= 2
            ? "<shadow:#000000FF><#5A4E7C>[</shadow><#FFB703>ระดับปานกลาง<shadow:#000000FF><#5A4E7C>]</shadow>"
            : "<shadow:#000000FF><#5A4E7C>[</shadow><#B8B0C8>ว่าง<shadow:#000000FF><#5A4E7C>]</shadow>");
        } else {
          HalloweenChat.setItemName(bMeta, session.oilLevel >= 3
            ? "<shadow:#000000FF><#5A4E7C>[</shadow><#6EEB83>ขีดมาตรฐานเต็ม<shadow:#000000FF><#5A4E7C>]</shadow>"
            : "<shadow:#000000FF><#5A4E7C>[</shadow><#B8B0C8>ขีดเป้าหมาย<shadow:#000000FF><#5A4E7C>]</shadow>");
        }
        bar.setItemMeta(bMeta);
      }
      inv.setItem(GAUGE_SLOTS[i], bar);
    }

    ItemStack oilCan = new ItemStack(session.oilLevel >= 3 ? Material.HONEY_BOTTLE : Material.POTION);
    ItemMeta oMeta = oilCan.getItemMeta();
    if(oMeta != null) {
      if(session.oilLevel >= 3) {
        HalloweenChat.setItemName(oMeta, "<shadow:#000000FF><#5A4E7C>[</shadow><#6EEB83>น้ำมันเครื่องเต็มขีดมาตรฐานแล้ว<shadow:#000000FF><#5A4E7C>]</shadow>");
        HalloweenChat.setItemLore(
          oMeta,
          "",
          "<shadow:#000000FF><#5A4E7C>(</shadow><#6EEB83>กดปิดฝาน้ำมันเครื่องด้านล่างได้เลย<shadow:#000000FF><#5A4E7C>)</shadow>"
        );
      } else {
        HalloweenChat.setItemName(oMeta, "<shadow:#000000FF><#5A4E7C>[</shadow><#FFB703>กระป๋องน้ำมันเครื่อง<shadow:#000000FF><#5A4E7C>]</shadow>");
        HalloweenChat.setItemLore(
          oMeta,
          "",
          "<#F4F1DE>ระดับปัจจุบัน <#FFB703>" + session.oilLevel + "<shadow:#000000FF><#5A4E7C>/</shadow><#F4F1DE>3",
          "",
          "<shadow:#000000FF><#5A4E7C>(</shadow><#B8B0C8>กดคลิกเพื่อเติมน้ำมัน +1 ขีด<shadow:#000000FF><#5A4E7C>)</shadow>"
        );
      }
      oilCan.setItemMeta(oMeta);
    }
    inv.setItem(OIL_CAN_SLOT, oilCan);

    ItemStack cap = (session.oilLevel >= 3 ? XMaterial.EMERALD_BLOCK : XMaterial.IRON_BARS).parseItem();
    ItemMeta cMeta = cap.getItemMeta();
    if(cMeta != null) {
      if(session.oilLevel >= 3) {
        HalloweenChat.setItemName(cMeta, "<shadow:#000000FF><#5A4E7C>[</shadow><#6EEB83>ปิดฝาน้ำมันเครื่อง<shadow:#000000FF><#5A4E7C>]</shadow>");
        HalloweenChat.setItemLore(
          cMeta,
          "",
          "<#F4F1DE>น้ำมันเครื่องได้ระดับมาตรฐานแล้ว",
          "",
          "<shadow:#000000FF><#5A4E7C>(</shadow><#B8B0C8>กดคลิกเพื่อจบภารกิจ<shadow:#000000FF><#5A4E7C>)</shadow>"
        );
      } else {
        HalloweenChat.setItemName(cMeta, "<shadow:#000000FF><#5A4E7C>[</shadow><#B8B0C8>ปิดฝาน้ำมันเครื่อง<shadow:#000000FF><#5A4E7C>]</shadow>");
        HalloweenChat.setItemLore(
          cMeta,
          "",
          "<#FF3B47>ต้องเติมน้ำมันให้ถึงขีดสีเขียวก่อน",
          "",
          "<shadow:#000000FF><#5A4E7C>(</shadow><#B8B0C8>ระดับปัจจุบัน " + session.oilLevel + "/3<shadow:#000000FF><#5A4E7C>)</shadow>"
        );
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
          HalloweenChat.sendActionBar(player, "<shadow:#000000FF><#5A4E7C>[</shadow><#6EEB83>Task<shadow:#000000FF><#5A4E7C>]</shadow> <#6EEB83>น้ำมันเครื่องเต็มแล้ว กดปิดฝาด้านล่างได้เลย");
        } else {
          HalloweenChat.sendActionBar(player, "<shadow:#000000FF><#5A4E7C>[</shadow><#FF7518>Task<shadow:#000000FF><#5A4E7C>]</shadow> <#F4F1DE>เติมน้ำมันเครื่อง <#FFB703>" + session.oilLevel + "/3");
        }
      } else {
        XSound.UI_BUTTON_CLICK.play(player.getLocation(), 0.5F, 1.2F);
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
        HalloweenChat.sendActionBar(player, "<shadow:#000000FF><#5A4E7C>[</shadow><#6EEB83>Task<shadow:#000000FF><#5A4E7C>]</shadow> <#6EEB83>เช็คน้ำมันเครื่องยนต์สำเร็จ");

        Bukkit.getScheduler().runTaskLater(arena.getPlugin(), () -> {
          if(player.isOnline() && arena.getPlayers().contains(player) && !arena.getVotingManager().isVotingActive()) {
            if(isGui(player.getOpenInventory().getTitle())) {
              player.closeInventory();
            }
            arena.getTaskManager().completeTaskFromMinigame(player, TaskType.CHECK_ENGINE_OIL);
          }
        }, 8L);
      } else {
        XSound.ENTITY_VILLAGER_NO.play(player.getLocation(), 1F, 1F);
        HalloweenChat.sendActionBar(player, "<shadow:#000000FF><#5A4E7C>[</shadow><#FF3B47>Task<shadow:#000000FF><#5A4E7C>]</shadow> <#FF3B47>ต้องคลิกเติมน้ำมันตรงกลางให้ครบ 3 ระดับก่อน");
      }
      return true;
    }

    return true;
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
