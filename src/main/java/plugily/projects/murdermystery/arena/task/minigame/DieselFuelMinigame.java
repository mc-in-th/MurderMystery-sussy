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

public class DieselFuelMinigame implements TaskMinigame {

  public static final String GUI_TITLE = HalloweenChat.color("<#5A4E7C>[<#FF7518>สถานีเติมน้ำมันดีเซล<#5A4E7C>]");

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
    HalloweenChat.sendActionBar(player, HalloweenChat.color("<#5A4E7C>[<#FF7518>Task<#5A4E7C>] <#F4F1DE>กดคันโยกทางขวาเพื่อปั๊มน้ำมันดีเซลให้เต็ม <#6EEB83>100 ลิตร"));
  }

  private void buildGui(Inventory inv, FuelSession session) {
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
      iMeta.setDisplayName(HalloweenChat.color("<#5A4E7C>[<#FF7518>Diesel Fuel<#5A4E7C>]"));
      iMeta.setLore(HalloweenChat.colorLines(
        "",
        "<#F4F1DE>1. คลิกคันโยกปั๊มน้ำมันทางขวา (+20 ลิตร)",
        "<#F4F1DE>2. ปั๊มจนเต็ม <#6EEB83>100 ลิตร <#F4F1DE>แล้วกดปิดฝาถังด้านล่าง",
        "",
        "<#5A4E7C>(<#B8B0C8>เติมเชื้อเพลิงสำรองสำหรับรถบัส<#5A4E7C>)"
      ));
      instruction.setItemMeta(iMeta);
    }
    inv.setItem(INSTRUCTION_SLOT, instruction);

    int filledBars = Math.min(7, (session.liters * 7) / 100);
    for(int i = 0; i < 7; i++) {
      ItemStack bar;
      if(i < filledBars) {
        bar = (session.liters >= 100 ? XMaterial.LIME_STAINED_GLASS_PANE : XMaterial.ORANGE_STAINED_GLASS_PANE).parseItem();
      } else {
        bar = XMaterial.GRAY_STAINED_GLASS_PANE.parseItem();
      }
      ItemMeta bMeta = bar.getItemMeta();
      if(bMeta != null) {
        bMeta.setDisplayName(HalloweenChat.color("<#5A4E7C>[<#FFB703>น้ำมันดีเซล " + session.liters + "/100 ลิตร<#5A4E7C>]"));
        bar.setItemMeta(bMeta);
      }
      inv.setItem(GAUGE_SLOTS[i], bar);
    }

    ItemStack canister = new ItemStack(session.liters >= 100 ? Material.LAVA_BUCKET : Material.BUCKET);
    ItemMeta cMeta = canister.getItemMeta();
    if(cMeta != null) {
      cMeta.setDisplayName(HalloweenChat.color(session.liters >= 100
        ? "<#5A4E7C>[<#6EEB83>แกลลอนดีเซลเต็ม 100 ลิตร<#5A4E7C>]"
        : "<#5A4E7C>[<#FF7518>ถังแกลลอนน้ำมันดีเซล<#5A4E7C>]"));
      cMeta.setLore(HalloweenChat.colorLines(
        "",
        "<#F4F1DE>ปริมาณสะสม <#FFB703>" + session.liters + "<#5A4E7C>/<#6EEB83>100 ลิตร",
        "",
        session.liters >= 100
          ? "<#5A4E7C>(<#6EEB83>กดปิดฝาถังด้านล่างเพื่อจบภารกิจ<#5A4E7C>)"
          : "<#5A4E7C>(<#B8B0C8>กดคันโยกทางขวาเพื่อปั๊มน้ำมัน<#5A4E7C>)"
      ));
      canister.setItemMeta(cMeta);
    }
    inv.setItem(NOZZLE_SLOT, canister);

    ItemStack divider = XMaterial.IRON_BARS.parseItem();
    ItemMeta dMeta = divider.getItemMeta();
    if(dMeta != null) {
      dMeta.setDisplayName(HalloweenChat.color("<#5A4E7C>[<#B8B0C8>ท่อจ่ายน้ำมัน<#5A4E7C>]"));
      divider.setItemMeta(dMeta);
    }
    inv.setItem(13, divider);

    ItemStack lever = XMaterial.TRIPWIRE_HOOK.parseItem();
    ItemMeta lMeta = lever.getItemMeta();
    if(lMeta != null) {
      if(session.liters >= 100) {
        lMeta.setDisplayName(HalloweenChat.color("<#5A4E7C>[<#6EEB83>น้ำมันเต็มถังแล้ว<#5A4E7C>]"));
        lMeta.setLore(HalloweenChat.colorLines("", "<#5A4E7C>(<#B8B0C8>คลิกปุ่มปิดฝาถังด้านล่าง<#5A4E7C>)"));
      } else {
        lMeta.setDisplayName(HalloweenChat.color("<#5A4E7C>[<#FFB703>คันโยกปั๊มน้ำมัน +20 ลิตร<#5A4E7C>]"));
        lMeta.setLore(HalloweenChat.colorLines(
          "",
          "<#F4F1DE>สูบน้ำมันดีเซลเข้าถังแกลลอน",
          "",
          "<#5A4E7C>(<#B8B0C8>กดคลิกเพื่อปั๊มน้ำมัน<#5A4E7C>)"
        ));
      }
      lever.setItemMeta(lMeta);
    }
    inv.setItem(PUMP_LEVER_SLOT, lever);

    ItemStack cap = (session.liters >= 100 ? XMaterial.EMERALD_BLOCK : XMaterial.IRON_BARS).parseItem();
    ItemMeta capMeta = cap.getItemMeta();
    if(capMeta != null) {
      if(session.liters >= 100) {
        capMeta.setDisplayName(HalloweenChat.color("<#5A4E7C>[<#6EEB83>ปิดฝาถังน้ำมันและล็อค<#5A4E7C>]"));
        capMeta.setLore(HalloweenChat.colorLines(
          "",
          "<#F4F1DE>น้ำมันดีเซลเต็ม <#6EEB83>100 ลิตร",
          "",
          "<#5A4E7C>(<#B8B0C8>กดคลิกเพื่อจบภารกิจ<#5A4E7C>)"
        ));
      } else {
        capMeta.setDisplayName(HalloweenChat.color("<#5A4E7C>[<#B8B0C8>ปิดฝาถังน้ำมัน<#5A4E7C>]"));
        capMeta.setLore(HalloweenChat.colorLines(
          "",
          "<#FF3B47>ต้องเติมน้ำมันให้ครบ 100 ลิตรก่อน",
          "",
          "<#5A4E7C>(<#B8B0C8>ปัจจุบัน " + session.liters + "/100 ลิตร<#5A4E7C>)"
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
          HalloweenChat.sendActionBar(player, HalloweenChat.color("<#5A4E7C>[<#6EEB83>Task<#5A4E7C>] <#6EEB83>น้ำมันเต็ม 100 ลิตรแล้ว กดปิดฝาถังด้านล่างได้เลย"));
        } else {
          HalloweenChat.sendActionBar(player, HalloweenChat.color("<#5A4E7C>[<#FF7518>Task<#5A4E7C>] <#F4F1DE>เติมน้ำมันแล้ว <#FFB703>" + session.liters + "/100 ลิตร"));
        }
      } else {
        XSound.UI_BUTTON_CLICK.play(player.getLocation(), 0.5F, 1.0F);
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
        HalloweenChat.sendActionBar(player, HalloweenChat.color("<#5A4E7C>[<#6EEB83>Task<#5A4E7C>] <#6EEB83>เติมน้ำมันดีเซลสำเร็จ"));

        Bukkit.getScheduler().runTaskLater(arena.getPlugin(), () -> {
          if(player.isOnline() && arena.getPlayers().contains(player) && !arena.getVotingManager().isVotingActive()) {
            if(isGui(player.getOpenInventory().getTitle())) {
              player.closeInventory();
            }
            arena.getTaskManager().completeTaskFromMinigame(player, TaskType.FIND_DIESEL_FUEL);
          }
        }, 8L);
      } else {
        XSound.ENTITY_VILLAGER_NO.play(player.getLocation(), 1F, 1F);
        HalloweenChat.sendActionBar(player, HalloweenChat.color("<#5A4E7C>[<#FF3B47>Task<#5A4E7C>] <#FF3B47>ต้องปั๊มน้ำมันให้ครบ 100 ลิตรก่อนปิดฝาถัง"));
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
