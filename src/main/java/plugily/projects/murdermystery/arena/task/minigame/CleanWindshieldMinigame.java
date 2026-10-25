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

public class CleanWindshieldMinigame implements TaskMinigame {

  public static final String GUI_TITLE = HalloweenChat.color("<shadow:#000000FF><#5A4E7C>[</shadow><#FF7518>เช็ดคราบโคลนกระจกหน้ารถ<shadow:#000000FF><#5A4E7C>]</shadow>");
  private static final int[] TRACK_SLOTS = {10, 11, 12, 13, 14, 15, 16};
  private static final int BUCKET_SLOT = 22;

  private static class WipeSession {
    boolean spongeReady = false;
    int progress = 0;
    long wipeStartMs = 0L;
    String statusLine = "<#B8B0C8>ชุบน้ำฟองน้ำจากถังด้านล่างเพื่อเริ่มเช็ดกระจก";
  }

  private final Arena arena;
  private final Map<UUID, WipeSession> sessions = new HashMap<>();
  private final Set<UUID> completedSessions = new HashSet<>();

  public CleanWindshieldMinigame(Arena arena) {
    this.arena = arena;
  }

  @Override
  public TaskType getTaskType() {
    return TaskType.CLEAN_WINDSHIELD;
  }

  @Override
  public String getGuiTitle() {
    return GUI_TITLE;
  }

  @Override
  public boolean isGui(String title) {
    return title != null && HalloweenChat.strip(title).contains("เช็ดคราบโคลนกระจกหน้ารถ");
  }

  @Override
  public void open(Player player) {
    completedSessions.remove(player.getUniqueId());
    WipeSession session = new WipeSession();
    sessions.put(player.getUniqueId(), session);

    Inventory inv = Bukkit.createInventory(null, 27, GUI_TITLE);
    buildGui(inv, session);
    player.openInventory(inv);

    XSound.ITEM_BUCKET_FILL.play(player.getLocation(), 0.8F, 1.2F);
    HalloweenChat.sendActionBar(player, "<shadow:#000000FF><#5A4E7C>[</shadow><#FF7518>Task<shadow:#000000FF><#5A4E7C>]</shadow> <#F4F1DE>หยิบฟองน้ำด้านล่าง แล้วคลิกเช็ดกระจกจากซ้ายไปขวาด้วยความเร็วพอดี");
  }

  private void buildGui(Inventory inv, WipeSession session) {
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

    ItemStack statusItem = new ItemStack(Material.GLASS);
    ItemMeta sMeta = statusItem.getItemMeta();
    if(sMeta != null) {
      HalloweenChat.setItemName(sMeta, "<shadow:#000000FF><#5A4E7C>[</shadow><#FF7518>Bus Windshield<shadow:#000000FF><#5A4E7C>]</shadow>");
      HalloweenChat.setItemLore(
        sMeta,
        "",
        session.statusLine,
        "",
        "<shadow:#000000FF><#5A4E7C>(</shadow><#B8B0C8>คลิกเช็ดกระจกจากซ้ายไปขวาตามลำดับ<shadow:#000000FF><#5A4E7C>)</shadow>"
      );
      statusItem.setItemMeta(sMeta);
    }
    inv.setItem(4, statusItem);

    for(int i = 0; i < TRACK_SLOTS.length; i++) {
      ItemStack slotItem;
      if(!session.spongeReady) {
        slotItem = XMaterial.BROWN_STAINED_GLASS_PANE.parseItem();
        ItemMeta m = slotItem.getItemMeta();
        if(m != null) {
          HalloweenChat.setItemName(m, "<shadow:#000000FF><#5A4E7C>[</shadow><#B8B0C8>คราบโคลนกระจก #" + (i + 1) + "<shadow:#000000FF><#5A4E7C>]</shadow>");
          HalloweenChat.setItemLore(m, "", "<shadow:#000000FF><#5A4E7C>(</shadow><#B8B0C8>กรุณาหยิบฟองน้ำด้านล่างก่อน<shadow:#000000FF><#5A4E7C>)</shadow>");
          slotItem.setItemMeta(m);
        }
      } else if(i < session.progress) {
        slotItem = XMaterial.LIGHT_BLUE_STAINED_GLASS_PANE.parseItem();
        ItemMeta m = slotItem.getItemMeta();
        if(m != null) {
          HalloweenChat.setItemName(m, "<shadow:#000000FF><#5A4E7C>[</shadow><#6EEB83>กระจกสะอาดแล้ว #" + (i + 1) + "<shadow:#000000FF><#5A4E7C>]</shadow>");
          slotItem.setItemMeta(m);
        }
      } else if(i == session.progress) {
        slotItem = XMaterial.WET_SPONGE.parseItem();
        ItemMeta m = slotItem.getItemMeta();
        if(m != null) {
          HalloweenChat.setItemName(m, "<shadow:#000000FF><#5A4E7C>[</shadow><#FFB703>เช็ดคราบโคลนช่องที่ " + (i + 1) + "/7<shadow:#000000FF><#5A4E7C>]</shadow>");
          HalloweenChat.setItemLore(
            m,
            "",
            "<#F4F1DE>คลิกเพื่อปัดคราบโคลนไปทางขวา",
            "",
            "<shadow:#000000FF><#5A4E7C>(</shadow><#B8B0C8>อย่าเช็ดเร็วหรือช้าเกินไป<shadow:#000000FF><#5A4E7C>)</shadow>"
          );
          slotItem.setItemMeta(m);
        }
      } else {
        slotItem = XMaterial.BROWN_STAINED_GLASS_PANE.parseItem();
        ItemMeta m = slotItem.getItemMeta();
        if(m != null) {
          HalloweenChat.setItemName(m, "<shadow:#000000FF><#5A4E7C>[</shadow><#FF7518>คราบโคลนกระจก #" + (i + 1) + "<shadow:#000000FF><#5A4E7C>]</shadow>");
          HalloweenChat.setItemLore(m, "", "<shadow:#000000FF><#5A4E7C>(</shadow><#B8B0C8>คลิกช่องที่มีฟองน้ำก่อนหน้าตามลำดับ<shadow:#000000FF><#5A4E7C>)</shadow>");
          slotItem.setItemMeta(m);
        }
      }
      inv.setItem(TRACK_SLOTS[i], slotItem);
    }

    ItemStack bucket = new ItemStack(session.spongeReady ? Material.BUCKET : Material.WATER_BUCKET);
    ItemMeta wMeta = bucket.getItemMeta();
    if(wMeta != null) {
      if(session.spongeReady) {
        HalloweenChat.setItemName(wMeta, "<shadow:#000000FF><#5A4E7C>[</shadow><#6EEB83>ชุบน้ำฟองน้ำแล้ว<shadow:#000000FF><#5A4E7C>]</shadow>");
        HalloweenChat.setItemLore(wMeta, "", "<shadow:#000000FF><#5A4E7C>(</shadow><#B8B0C8>คลิกแถวกลางจากซ้ายไปขวาเพื่อเช็ดกระจก<shadow:#000000FF><#5A4E7C>)</shadow>");
      } else {
        HalloweenChat.setItemName(wMeta, "<shadow:#000000FF><#5A4E7C>[</shadow><#FFB703>ถังน้ำและฟองน้ำเช็ดกระจก<shadow:#000000FF><#5A4E7C>]</shadow>");
        HalloweenChat.setItemLore(
          wMeta,
          "",
          "<#F4F1DE>หยิบฟองน้ำชุบน้ำขึ้นมาเช็ดโคลนหน้ากระจกรถบัส",
          "",
          "<shadow:#000000FF><#5A4E7C>(</shadow><#B8B0C8>กดคลิกเพื่อหยิบฟองน้ำ<shadow:#000000FF><#5A4E7C>)</shadow>"
        );
      }
      bucket.setItemMeta(wMeta);
    }
    inv.setItem(BUCKET_SLOT, bucket);
  }

  @Override
  public boolean handleClick(Player player, int rawSlot, ClickType clickType) {
    if(completedSessions.contains(player.getUniqueId())) {
      return true;
    }
    WipeSession session = sessions.get(player.getUniqueId());
    if(session == null) {
      return true;
    }

    Inventory topInv = player.getOpenInventory().getTopInventory();

    if(rawSlot == BUCKET_SLOT && !session.spongeReady) {
      session.spongeReady = true;
      session.progress = 0;
      session.wipeStartMs = 0L;
      session.statusLine = "<#FFB703>พร้อมเช็ดกระจก คลิกช่องที่ 1 ถึง 7 ตามลำดับ";
      XSound.ITEM_BUCKET_EMPTY.play(player.getLocation(), 0.9F, 1.2F);
      buildGui(topInv, session);
      return true;
    }

    if(!session.spongeReady) {
      if(rawSlot >= 10 && rawSlot <= 16) {
        XSound.ENTITY_VILLAGER_NO.play(player.getLocation(), 0.7F, 1.0F);
      }
      return true;
    }

    int expectedSlot = TRACK_SLOTS[session.progress];
    if(rawSlot == expectedSlot) {
      if(session.progress == 0) {
        session.wipeStartMs = System.currentTimeMillis();
      }
      session.progress++;
      XSound.BLOCK_SLIME_BLOCK_STEP.play(player.getLocation(), 0.8F, 1.0F + (session.progress * 0.12F));

      if(session.progress >= TRACK_SLOTS.length) {
        long elapsed = System.currentTimeMillis() - session.wipeStartMs;
        if(elapsed < 700L) {
          session.progress = 0;
          session.wipeStartMs = 0L;
          session.statusLine = "<#FF3B47>เช็ดเร็วเกินไป คราบโคลนยังออกไม่หมด กรุณาลองใหม่";
          XSound.BLOCK_NOTE_BLOCK_BASS.play(player.getLocation(), 0.9F, 0.7F);
          buildGui(topInv, session);
          return true;
        }
        if(elapsed > 5500L) {
          session.progress = 0;
          session.wipeStartMs = 0L;
          session.statusLine = "<#FF3B47>เช็ดช้าเกินไป คราบโคลนแห้งติดกระจก กรุณาลองใหม่";
          XSound.BLOCK_NOTE_BLOCK_BASS.play(player.getLocation(), 0.9F, 0.7F);
          buildGui(topInv, session);
          return true;
        }

        completedSessions.add(player.getUniqueId());
        sessions.remove(player.getUniqueId());
        session.statusLine = "<#6EEB83>กระจกหน้ารถบัสสะอาดใสเรียบร้อยแล้ว";
        buildGui(topInv, session);
        XSound.ENTITY_PLAYER_LEVELUP.play(player.getLocation(), 0.9F, 1.5F);

        Bukkit.getScheduler().runTaskLater(arena.getPlugin(), () -> {
          if(player.isOnline() && arena.getPlayers().contains(player) && !arena.getVotingManager().isVotingActive()) {
            if(isGui(player.getOpenInventory().getTitle())) {
              player.closeInventory();
            }
            arena.getTaskManager().completeTaskFromMinigame(player, TaskType.CLEAN_WINDSHIELD);
          }
        }, 8L);
        return true;
      }

      session.statusLine = "<#F4F1DE>กำลังเช็ดคราบโคลน <#FFB703>" + session.progress + "/7";
      buildGui(topInv, session);
      return true;
    }

    if(rawSlot >= 10 && rawSlot <= 16) {
      session.progress = 0;
      session.wipeStartMs = 0L;
      session.statusLine = "<#FF3B47>เช็ดข้ามช่อง กรุณาเริ่มเช็ดจากซ้ายสุดใหม่";
      XSound.BLOCK_NOTE_BLOCK_BASS.play(player.getLocation(), 0.8F, 0.8F);
      buildGui(topInv, session);
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
