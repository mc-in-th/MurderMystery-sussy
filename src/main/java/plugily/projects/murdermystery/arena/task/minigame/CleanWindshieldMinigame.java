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

  private static final int STATUS_SLOT = 4;
  private static final int[] TRACK_SLOTS = {10, 11, 12, 13, 14, 15, 16};
  private static final int BUCKET_SLOT = 22;

  private static class WipeSession {
    boolean spongeReady = false;
    int progress = 0;
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

    TaskGuiHelper.playSound(player, XSound.ITEM_BUCKET_FILL, 0.8F, 1.2F);
    TaskGuiHelper.sendTaskBar(player, "<#F4F1DE>ขั้นที่ 1 คลิกถังน้ำเรืองแสงด้านล่างเพื่อชุบน้ำฟองน้ำ");
  }

  private void buildGui(Inventory inv, WipeSession session) {
    TaskGuiHelper.fillBackground(inv);

    boolean finished = session.progress >= TRACK_SLOTS.length;

    ItemStack statusItem;
    if(finished) {
      statusItem = TaskGuiHelper.createItem(
        Material.EMERALD,
        1,
        true,
        TaskGuiHelper.doneName("กระจกหน้ารถบัสใสสะอาดแล้ว"),
        "",
        "<#6EEB83>ทัศนวิสัยด้านหน้ารถบัสชัดเจนสมบูรณ์"
      );
    } else if(!session.spongeReady) {
      statusItem = TaskGuiHelper.createItem(
        Material.GLASS,
        1,
        false,
        TaskGuiHelper.tag("<#FF7518>", "ขั้นที่ 1/2 - ชุบน้ำฟองน้ำด้านล่าง"),
        "",
        "<#F4F1DE>คลิกที่ <#FFB703>ถังน้ำเรืองแสงด้านล่าง <#F4F1DE>เพื่อเตรียมฟองน้ำ",
        "<#F4F1DE>จากนั้นคลิกเช็ดกระจกแถวกลางจากซ้ายไปขวา"
      );
    } else {
      statusItem = TaskGuiHelper.createItem(
        Material.GLASS,
        Math.min(TRACK_SLOTS.length, session.progress + 1),
        false,
        TaskGuiHelper.tag("<#FFB703>", "ขั้นที่ 2/2 - เช็ดกระจกช่องที่ " + (session.progress + 1) + "/7"),
        "",
        "<#F4F1DE>ความสะอาด " + TaskGuiHelper.buildPipeBar(session.progress, TRACK_SLOTS.length, 7) + " <#FFB703>" + session.progress + "/7",
        "",
        TaskGuiHelper.hint("<#FFB703>", "คลิกที่ฟองน้ำเรืองแสงไล่จากซ้ายไปขวา")
      );
    }
    inv.setItem(STATUS_SLOT, statusItem);

    for(int i = 0; i < TRACK_SLOTS.length; i++) {
      int stepNum = i + 1;
      ItemStack slotItem;
      if(!session.spongeReady) {
        slotItem = TaskGuiHelper.createItem(
          XMaterial.BROWN_STAINED_GLASS_PANE,
          stepNum,
          false,
          TaskGuiHelper.tag("<#B8B0C8>", "คราบโคลนเกาะกระจก #" + stepNum),
          "",
          TaskGuiHelper.hint("<#FF3B47>", "คลิกถังน้ำเรืองแสงด้านล่างก่อน")
        );
      } else if(i < session.progress) {
        slotItem = TaskGuiHelper.createItem(
          XMaterial.LIGHT_BLUE_STAINED_GLASS_PANE,
          1,
          finished,
          TaskGuiHelper.tag("<#6EEB83>", "กระจกใสสะอาดแล้ว #" + stepNum),
          "",
          "<#6EEB83>ขจัดคราบโคลนเรียบร้อย"
        );
      } else if(i == session.progress) {
        slotItem = TaskGuiHelper.createItem(
          XMaterial.WET_SPONGE,
          stepNum,
          true,
          TaskGuiHelper.clickName("เช็ดคราบโคลนช่องที่ " + stepNum + "/7"),
          "",
          "<#F4F1DE>ปัดฟองน้ำชุบน้ำล้างโคลนออกจากกระจก",
          "",
          TaskGuiHelper.hint("<#FFB703>", "กดคลิกที่นี่เพื่อเช็ดช่องนี้")
        );
      } else {
        slotItem = TaskGuiHelper.createItem(
          XMaterial.BROWN_STAINED_GLASS_PANE,
          stepNum,
          false,
          TaskGuiHelper.tag("<#FF7518>", "คราบโคลนช่องถัดไป #" + stepNum),
          "",
          TaskGuiHelper.hint("<#B8B0C8>", "เช็ดช่องที่ " + (session.progress + 1) + " ก่อน")
        );
      }
      inv.setItem(TRACK_SLOTS[i], slotItem);
    }

    ItemStack bucket;
    if(!session.spongeReady) {
      bucket = TaskGuiHelper.createItem(
        Material.WATER_BUCKET,
        1,
        true,
        TaskGuiHelper.clickName("ชุบน้ำฟองน้ำเช็ดกระจก"),
        "",
        "<#F4F1DE>จุ่มฟองน้ำลงถังน้ำสะอาดเพื่อเริ่มเช็ดโคลน",
        "",
        TaskGuiHelper.hint("<#FFB703>", "กดคลิกที่นี่เป็นอันดับแรก")
      );
    } else {
      bucket = TaskGuiHelper.createItem(
        Material.BUCKET,
        1,
        false,
        TaskGuiHelper.doneName("ชุบน้ำฟองน้ำพร้อมแล้ว"),
        "",
        TaskGuiHelper.hint("<#B8B0C8>", "คลิกฟองน้ำเรืองแสงแถวกลางได้เลย")
      );
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
      TaskGuiHelper.playSound(player, XSound.ITEM_BUCKET_EMPTY, 0.9F, 1.2F);
      buildGui(topInv, session);
      TaskGuiHelper.sendTaskBar(player, "<#F4F1DE>ขั้นที่ 2 คลิกที่ <#FFB703>ฟองน้ำเรืองแสง <#F4F1DE>ไล่เช็ดจากซ้ายไปขวา (1-7)");
      return true;
    }

    if(!session.spongeReady) {
      if(rawSlot >= 0 && rawSlot < 27) {
        TaskGuiHelper.playSound(player, XSound.ENTITY_VILLAGER_NO, 0.7F, 1.1F);
        TaskGuiHelper.sendErrorBar(player, "คลิกถังน้ำเรืองแสงด้านล่างเพื่อชุบน้ำฟองน้ำก่อน");
      }
      return true;
    }

    int expectedSlot = TRACK_SLOTS[session.progress];
    if(rawSlot == expectedSlot) {
      session.progress++;
      TaskGuiHelper.playSound(player, XSound.BLOCK_SLIME_BLOCK_STEP, 0.85F, 1.0F + (session.progress * 0.12F));
      TaskGuiHelper.playSound(player, XSound.BLOCK_NOTE_BLOCK_CHIME, 0.5F, 0.9F + (session.progress * 0.1F));
      buildGui(topInv, session);

      if(session.progress >= TRACK_SLOTS.length) {
        completedSessions.add(player.getUniqueId());
        sessions.remove(player.getUniqueId());
        TaskGuiHelper.playSound(player, XSound.ENTITY_PLAYER_LEVELUP, 0.9F, 1.5F);
        TaskGuiHelper.sendSuccessBar(player, "เช็ดคราบโคลนกระจกหน้ารถบัสสะอาดเรียบร้อย");

        Bukkit.getScheduler().runTaskLater(arena.getPlugin(), () -> {
          if(player.isOnline() && arena.getPlayers().contains(player) && !arena.getVotingManager().isVotingActive()) {
            if(isGui(player.getOpenInventory().getTitle())) {
              player.closeInventory();
            }
            arena.getTaskManager().completeTaskFromMinigame(player, TaskType.CLEAN_WINDSHIELD);
          }
        }, 8L);
      } else {
        TaskGuiHelper.sendTaskBar(player, "<#F4F1DE>เช็ดคราบโคลนแล้ว <#6EEB83>" + session.progress + "/7 <#B8B0C8>(คลิกฟองน้ำช่องถัดไป)");
      }
      return true;
    }

    if(rawSlot >= 10 && rawSlot <= 16) {
      TaskGuiHelper.playSound(player, XSound.UI_BUTTON_CLICK, 0.6F, 0.8F);
      TaskGuiHelper.sendTaskBar(player, "<#FFB703>คลิกที่ฟองน้ำเรืองแสงช่องที่ " + (session.progress + 1) + " <#F4F1DE>เพื่อเช็ดต่อ");
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
