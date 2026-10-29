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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class SparePartsMinigame implements TaskMinigame {

  public static final String GUI_TITLE = HalloweenChat.color("<shadow:#000000FF><#5A4E7C>[</shadow><#FF7518>ค้นหากล่องฟิวส์และอะไหล่<shadow:#000000FF><#5A4E7C>]</shadow>");

  private static final int TOTAL_CLOGS = 6;
  private static final int STATUS_SLOT = 4;
  private static final int BIN_SLOT = 40;
  private static final int[] SHELF_SLOTS = {
    11, 12, 13, 14, 15,
    20, 21, 22, 23, 24,
    29, 30, 31, 32, 33
  };
  private static final XMaterial[] CLOG_MATERIALS = {
    XMaterial.OAK_LEAVES,
    XMaterial.VINE,
    XMaterial.COBWEB
  };

  private static class PartsSession {
    final Set<Integer> clogSlots = new HashSet<>();

    PartsSession() {
      List<Integer> pool = new ArrayList<>();
      for(int slot : SHELF_SLOTS) {
        pool.add(slot);
      }
      Collections.shuffle(pool);
      for(int i = 0; i < TOTAL_CLOGS && i < pool.size(); i++) {
        clogSlots.add(pool.get(i));
      }
    }
  }

  private final Arena arena;
  private final Map<UUID, PartsSession> sessions = new HashMap<>();
  private final Set<UUID> completedSessions = new HashSet<>();

  public SparePartsMinigame(Arena arena) {
    this.arena = arena;
  }

  @Override
  public TaskType getTaskType() {
    return TaskType.SEARCH_SPARE_PARTS;
  }

  @Override
  public String getGuiTitle() {
    return GUI_TITLE;
  }

  @Override
  public boolean isGui(String title) {
    return title != null && HalloweenChat.strip(title).contains("ค้นหากล่องฟิวส์และอะไหล่");
  }

  @Override
  public void open(Player player) {
    completedSessions.remove(player.getUniqueId());
    PartsSession session = new PartsSession();
    sessions.put(player.getUniqueId(), session);

    Inventory inv = Bukkit.createInventory(null, 45, GUI_TITLE);
    buildGui(inv, session);
    player.openInventory(inv);

    XSound.BLOCK_CHEST_OPEN.play(player.getLocation(), 0.8F, 1.2F);
    TaskGuiHelper.sendTaskBar(player, "<#F4F1DE>คลิกหยากไย่หรือเศษใบไม้ที่เรืองแสงบนชั้นวางเพื่อค้นหาอะไหล่ให้ครบ <#FFB703>6 จุด");
  }

  private void buildGui(Inventory inv, PartsSession session) {
    TaskGuiHelper.fillBackground(inv);

    int remaining = session.clogSlots.size();
    int cleared = TOTAL_CLOGS - remaining;

    ItemStack statusItem;
    if(remaining == 0) {
      statusItem = TaskGuiHelper.createItem(
        Material.EMERALD,
        1,
        true,
        TaskGuiHelper.doneName("พบกล่องฟิวส์และอะไหล่สำรองครบแล้ว"),
        "",
        "<#6EEB83>รวบรวมฟิวส์และอะไหล่สำหรับซ่อมรถบัสเรียบร้อย"
      );
    } else {
      statusItem = TaskGuiHelper.createItem(
        Material.CHEST,
        remaining,
        false,
        TaskGuiHelper.tag("<#FF7518>", "จุดที่ต้องรื้อค้นอีก " + remaining + "/" + TOTAL_CLOGS + " จุด"),
        "",
        "<#F4F1DE>คลิกที่ <#FFB703>หยากไย่และเศษใบไม้เรืองแสง <#F4F1DE>บนชั้นวางของ",
        "<#F4F1DE>เพื่อรื้อหาฟิวส์และอะไหล่สำรองทีละช่อง"
      );
    }
    inv.setItem(STATUS_SLOT, statusItem);

    for(int slot : SHELF_SLOTS) {
      if(session.clogSlots.contains(slot)) {
        XMaterial clogMat = CLOG_MATERIALS[slot % CLOG_MATERIALS.length];
        String clogType = (clogMat == XMaterial.COBWEB) ? "หยากไย่บังชั้นวาง" : "เศษใบไม้ทับกล่องอะไหล่";
        ItemStack clog = TaskGuiHelper.createItem(
          clogMat,
          1,
          true,
          TaskGuiHelper.clickName("รื้อ" + clogType + "ออก"),
          "",
          "<#F4F1DE>บดบังกล่องฟิวส์และอะไหล่สำรองบนชั้น",
          "",
          TaskGuiHelper.hint("<#FFB703>", "กดคลิกเพื่อรื้อค้นทันที")
        );
        inv.setItem(slot, clog);
      } else {
        ItemStack grate = TaskGuiHelper.createItem(
          XMaterial.IRON_BARS,
          1,
          false,
          TaskGuiHelper.tag("<#6EEB83>", "ตรวจค้นช่องเก็บของแล้ว"),
          "",
          "<#B8B0C8>เก็บฟิวส์และอะไหล่สำรองจากช่องนี้แล้ว"
        );
        inv.setItem(slot, grate);
      }
    }

    inv.setItem(BIN_SLOT, TaskGuiHelper.createItem(
      Material.CHEST,
      Math.max(1, cleared),
      remaining == 0,
      TaskGuiHelper.tag(remaining == 0 ? "<#6EEB83>" : "<#FFB703>", "ค้นพบอะไหล่แล้ว " + cleared + "/" + TOTAL_CLOGS + " จุด"),
      "",
      "<#F4F1DE>ความคืบหน้า " + TaskGuiHelper.buildPipeBar(cleared, TOTAL_CLOGS, 12)
    ));
  }

  @Override
  public boolean handleClick(Player player, int rawSlot, ClickType clickType) {
    if(completedSessions.contains(player.getUniqueId())) {
      return true;
    }
    PartsSession session = sessions.get(player.getUniqueId());
    if(session == null) {
      return true;
    }

    if(!session.clogSlots.contains(rawSlot)) {
      if(rawSlot >= 0 && rawSlot < 45) {
        XSound.UI_BUTTON_CLICK.play(player.getLocation(), 0.45F, 1.0F);
        TaskGuiHelper.sendTaskBar(player, "<#FFB703>คลิกที่หยากไย่หรือเศษใบไม้ที่เรืองแสง <#F4F1DE>เพื่อรื้อหาอะไหล่");
      }
      return true;
    }

    session.clogSlots.remove(rawSlot);
    int cleared = TOTAL_CLOGS - session.clogSlots.size();
    XSound.BLOCK_COMPOSTER_FILL.play(player.getLocation(), 0.9F, 1.1F + (cleared * 0.08F));

    Inventory topInv = player.getOpenInventory().getTopInventory();
    buildGui(topInv, session);

    if(session.clogSlots.isEmpty()) {
      completedSessions.add(player.getUniqueId());
      sessions.remove(player.getUniqueId());
      XSound.ENTITY_PLAYER_LEVELUP.play(player.getLocation(), 0.9F, 1.5F);
      TaskGuiHelper.sendSuccessBar(player, "ค้นหากล่องฟิวส์และอะไหล่สำเร็จ");

      Bukkit.getScheduler().runTaskLater(arena.getPlugin(), () -> {
        if(player.isOnline() && arena.getPlayers().contains(player) && !arena.getVotingManager().isVotingActive()) {
          if(isGui(player.getOpenInventory().getTitle())) {
            player.closeInventory();
          }
          arena.getTaskManager().completeTaskFromMinigame(player, TaskType.SEARCH_SPARE_PARTS);
        }
      }, 8L);
    } else {
      TaskGuiHelper.sendTaskBar(player, "<#F4F1DE>ค้นพบอะไหล่แล้ว <#6EEB83>" + cleared + "/" + TOTAL_CLOGS + " จุด");
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
