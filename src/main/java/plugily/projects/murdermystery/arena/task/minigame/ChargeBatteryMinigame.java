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
import org.bukkit.scheduler.BukkitTask;
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

public class ChargeBatteryMinigame implements TaskMinigame {

  public static final String GUI_TITLE = HalloweenChat.color("<shadow:#000000FF><#5A4E7C>[</shadow><#FF7518>พ่วงชาร์จแบตเตอรี่รถบัส<shadow:#000000FF><#5A4E7C>]</shadow>");
  private static final int[] PROGRESS_SLOTS = {11, 12, 13, 14, 15};
  private static final int START_BUTTON_SLOT = 22;

  private static class BatterySession {
    boolean charging = false;
    int step = 0;
    BukkitTask task;
  }

  private final Arena arena;
  private final Map<UUID, BatterySession> sessions = new HashMap<>();
  private final Set<UUID> completedSessions = new HashSet<>();

  public ChargeBatteryMinigame(Arena arena) {
    this.arena = arena;
  }

  @Override
  public TaskType getTaskType() {
    return TaskType.CHARGE_BATTERY;
  }

  @Override
  public String getGuiTitle() {
    return GUI_TITLE;
  }

  @Override
  public boolean isGui(String title) {
    return title != null && HalloweenChat.strip(title).contains("พ่วงชาร์จแบตเตอรี่รถบัส");
  }

  @Override
  public void open(Player player) {
    cancelSession(player.getUniqueId());
    completedSessions.remove(player.getUniqueId());

    BatterySession session = new BatterySession();
    sessions.put(player.getUniqueId(), session);

    Inventory inv = Bukkit.createInventory(null, 27, GUI_TITLE);
    buildGui(inv, session);
    player.openInventory(inv);

    XSound.BLOCK_BEACON_AMBIENT.play(player.getLocation(), 0.7F, 1.4F);
    HalloweenChat.sendActionBar(player, "<shadow:#000000FF><#5A4E7C>[</shadow><#FF7518>Task<shadow:#000000FF><#5A4E7C>]</shadow> <#F4F1DE>กดสวิตช์พ่วงแบตเตอรี่ด้านล่างแล้วรอชาร์จไฟจนครบ 100%");
  }

  private void buildGui(Inventory inv, BatterySession session) {
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

    int percent = Math.min(100, session.step * 20);

    ItemStack generator = new ItemStack(Material.REDSTONE_BLOCK);
    ItemMeta srcMeta = generator.getItemMeta();
    if(srcMeta != null) {
      HalloweenChat.setItemName(srcMeta, "<shadow:#000000FF><#5A4E7C>[</shadow><#FFB703>เครื่องปั่นไฟฉุกเฉิน<shadow:#000000FF><#5A4E7C>]</shadow>");
      HalloweenChat.setItemLore(srcMeta, "", "<#F4F1DE>แรงดันไฟพ่วง <#FFB703>24V DC");
      generator.setItemMeta(srcMeta);
    }
    inv.setItem(10, generator);

    ItemStack busBattery = new ItemStack(Material.IRON_BLOCK);
    ItemMeta dstMeta = busBattery.getItemMeta();
    if(dstMeta != null) {
      HalloweenChat.setItemName(dstMeta, "<shadow:#000000FF><#5A4E7C>[</shadow><#72EFDD>แบตเตอรี่หลักรถบัส<shadow:#000000FF><#5A4E7C>]</shadow>");
      HalloweenChat.setItemLore(dstMeta, "", "<#F4F1DE>ประจุไฟสะสม <#6EEB83>" + percent + "%");
      busBattery.setItemMeta(dstMeta);
    }
    inv.setItem(16, busBattery);

    for(int i = 0; i < PROGRESS_SLOTS.length; i++) {
      boolean filled = i < session.step;
      ItemStack pane = (filled ? XMaterial.LIME_STAINED_GLASS_PANE : XMaterial.GRAY_STAINED_GLASS_PANE).parseItem();
      ItemMeta pMeta = pane.getItemMeta();
      if(pMeta != null) {
        HalloweenChat.setItemName(
          pMeta,
          filled
            ? "<shadow:#000000FF><#5A4E7C>[</shadow><#6EEB83>กำลังส่งกระแสไฟ " + percent + "%<shadow:#000000FF><#5A4E7C>]</shadow>"
            : "<shadow:#000000FF><#5A4E7C>[</shadow><#B8B0C8>สายพ่วงแบตเตอรี่<shadow:#000000FF><#5A4E7C>]</shadow>"
        );
        pane.setItemMeta(pMeta);
      }
      inv.setItem(PROGRESS_SLOTS[i], pane);
    }

    ItemStack startBtn = (session.charging ? XMaterial.CLOCK : XMaterial.LEVER).parseItem();
    ItemMeta bMeta = startBtn.getItemMeta();
    if(bMeta != null) {
      if(session.charging) {
        HalloweenChat.setItemName(bMeta, "<shadow:#000000FF><#5A4E7C>[</shadow><#6EEB83>กำลังพ่วงไฟ " + percent + "%<shadow:#000000FF><#5A4E7C>]</shadow>");
        HalloweenChat.setItemLore(
          bMeta,
          "",
          "<#F4F1DE>กรุณาถือสายพ่วงค้างไว้จนกว่าจะเต็ม",
          "",
          "<shadow:#000000FF><#5A4E7C>(</shadow><#B8B0C8>รอให้ชาร์จไฟครบ 100%<shadow:#000000FF><#5A4E7C>)</shadow>"
        );
      } else {
        HalloweenChat.setItemName(bMeta, "<shadow:#000000FF><#5A4E7C>[</shadow><#FF7518>หนีบสายพ่วงแบตเตอรี่<shadow:#000000FF><#5A4E7C>]</shadow>");
        HalloweenChat.setItemLore(
          bMeta,
          "",
          "<#F4F1DE>ส่งกระแสไฟจากเครื่องปั่นไฟเข้าสู่แบตเตอรี่รถบัส",
          "",
          "<shadow:#000000FF><#5A4E7C>(</shadow><#B8B0C8>กดคลิกเพื่อเริ่มชาร์จแบตเตอรี่<shadow:#000000FF><#5A4E7C>)</shadow>"
        );
      }
      startBtn.setItemMeta(bMeta);
    }
    inv.setItem(START_BUTTON_SLOT, startBtn);
  }

  @Override
  public boolean handleClick(Player player, int rawSlot, ClickType clickType) {
    if(completedSessions.contains(player.getUniqueId())) {
      return true;
    }
    BatterySession session = sessions.get(player.getUniqueId());
    if(session == null || session.charging) {
      return true;
    }

    if(rawSlot == START_BUTTON_SLOT) {
      session.charging = true;
      session.step = 0;
      Inventory topInv = player.getOpenInventory().getTopInventory();
      buildGui(topInv, session);
      XSound.BLOCK_LEVER_CLICK.play(player.getLocation(), 0.8F, 1.2F);

      session.task = Bukkit.getScheduler().runTaskTimer(arena.getPlugin(), () -> {
        if(!player.isOnline()
          || !arena.getPlayers().contains(player)
          || arena.getVotingManager().isVotingActive()
          || !sessions.containsKey(player.getUniqueId())
          || !isGui(player.getOpenInventory().getTitle())) {
          cancelSession(player.getUniqueId());
          return;
        }
        session.step++;
        XSound.BLOCK_NOTE_BLOCK_HAT.play(player.getLocation(), 0.6F, 1.0F + (session.step * 0.15F));
        buildGui(topInv, session);

        if(session.step >= PROGRESS_SLOTS.length) {
          if(session.task != null) {
            session.task.cancel();
            session.task = null;
          }
          completedSessions.add(player.getUniqueId());
          sessions.remove(player.getUniqueId());
          XSound.ENTITY_PLAYER_LEVELUP.play(player.getLocation(), 0.9F, 1.5F);
          Bukkit.getScheduler().runTaskLater(arena.getPlugin(), () -> {
            if(player.isOnline() && arena.getPlayers().contains(player) && !arena.getVotingManager().isVotingActive()) {
              if(isGui(player.getOpenInventory().getTitle())) {
                player.closeInventory();
              }
              arena.getTaskManager().completeTaskFromMinigame(player, TaskType.CHARGE_BATTERY);
            }
          }, 8L);
        }
      }, 15L, 15L);
    }
    return true;
  }

  private void cancelSession(UUID uuid) {
    BatterySession old = sessions.remove(uuid);
    if(old != null && old.task != null) {
      old.task.cancel();
    }
  }

  @Override
  public void handleClose(Player player) {
    cancelSession(player.getUniqueId());
    completedSessions.remove(player.getUniqueId());
  }

  @Override
  public void cleanup() {
    for(UUID uuid : new HashSet<>(sessions.keySet())) {
      cancelSession(uuid);
    }
    sessions.clear();
    completedSessions.clear();
  }
}
