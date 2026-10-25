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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class ControlPanelMinigame implements TaskMinigame {

  public static final String GUI_TITLE = HalloweenChat.color("<shadow:#000000FF><#5A4E7C>[</shadow><#FF7518>แผงควบคุมระบบไฟ<shadow:#000000FF><#5A4E7C>]</shadow>");

  private static class PanelSession {
    final List<Integer> requiredSequence;
    int currentStep = 0;
    final boolean[] switchState = new boolean[5];

    PanelSession() {
      List<Integer> seq = new ArrayList<>(Arrays.asList(0, 1, 2, 3, 4));
      Collections.shuffle(seq);
      this.requiredSequence = seq;
    }
  }

  private final Arena arena;
  private final Map<UUID, PanelSession> sessions = new HashMap<>();
  private final Set<UUID> completedSessions = new HashSet<>();

  private static final int[] SWITCH_SLOTS = {11, 12, 13, 14, 15};
  private static final int[] LIGHT_SLOTS = {2, 3, 4, 5, 6};
  private static final String[] SWITCH_LABELS = {
    "<shadow:#000000FF><#5A4E7C>[</shadow><#FFB703>สวิตช์ 1<shadow:#000000FF><#5A4E7C>]</shadow> <#F4F1DE>ไฟหน้ารถ",
    "<shadow:#000000FF><#5A4E7C>[</shadow><#FFB703>สวิตช์ 2<shadow:#000000FF><#5A4E7C>]</shadow> <#F4F1DE>ปั๊มน้ำมัน",
    "<shadow:#000000FF><#5A4E7C>[</shadow><#FFB703>สวิตช์ 3<shadow:#000000FF><#5A4E7C>]</shadow> <#F4F1DE>พัดลมหม้อน้ำ",
    "<shadow:#000000FF><#5A4E7C>[</shadow><#FFB703>สวิตช์ 4<shadow:#000000FF><#5A4E7C>]</shadow> <#F4F1DE>ระบบสตาร์ท",
    "<shadow:#000000FF><#5A4E7C>[</shadow><#FFB703>สวิตช์ 5<shadow:#000000FF><#5A4E7C>]</shadow> <#F4F1DE>ระบบปรับอากาศ"
  };

  public ControlPanelMinigame(Arena arena) {
    this.arena = arena;
  }

  @Override
  public TaskType getTaskType() {
    return TaskType.CHECK_CONTROL_PANEL;
  }

  @Override
  public String getGuiTitle() {
    return GUI_TITLE;
  }

  @Override
  public boolean isGui(String title) {
    return title != null && HalloweenChat.strip(title).contains("แผงควบคุมระบบไฟ");
  }

  @Override
  public void open(Player player) {
    completedSessions.remove(player.getUniqueId());
    PanelSession session = new PanelSession();
    sessions.put(player.getUniqueId(), session);

    Inventory inv = Bukkit.createInventory(null, 27, GUI_TITLE);
    buildGui(inv, session);
    player.openInventory(inv);

    XSound.BLOCK_CHEST_OPEN.play(player.getLocation(), 0.8F, 1.2F);
    HalloweenChat.sendActionBar(player, "<shadow:#000000FF><#5A4E7C>[</shadow><#FF7518>Task<shadow:#000000FF><#5A4E7C>]</shadow> <#F4F1DE>สับสวิตช์ทั้ง 5 ตัวตามลำดับสัญญาณไฟสีเหลือง");
  }

  private void buildGui(Inventory inv, PanelSession session) {
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
      HalloweenChat.setItemName(iMeta, "<shadow:#000000FF><#5A4E7C>[</shadow><#FF7518>Control Panel<shadow:#000000FF><#5A4E7C>]</shadow>");
      HalloweenChat.setItemLore(
        iMeta,
        "",
        "<#F4F1DE>1. สังเกตไฟสัญญาณสีเหลืองแถวบน",
        "<#F4F1DE>2. คลิกสับสวิตช์แถวกลางตามลำดับให้ครบ 5 ตัว",
        "",
        "<shadow:#000000FF><#5A4E7C>(</shadow><#FF3B47>หากสับผิดลำดับ เบรกเกอร์จะตัดและรีเซ็ตใหม่<shadow:#000000FF><#5A4E7C>)</shadow>"
      );
      instruction.setItemMeta(iMeta);
    }
    inv.setItem(0, instruction);

    ItemStack monitor = new ItemStack(Material.KNOWLEDGE_BOOK);
    ItemMeta monMeta = monitor.getItemMeta();
    if(monMeta != null) {
      HalloweenChat.setItemName(monMeta, "<shadow:#000000FF><#5A4E7C>[</shadow><#FFB703>ลำดับการเปิดสวิตช์<shadow:#000000FF><#5A4E7C>]</shadow>");
      List<String> lore = new ArrayList<>();
      lore.add("");
      StringBuilder sb = new StringBuilder();
      for(int i = 0; i < session.requiredSequence.size(); i++) {
        int swIdx = session.requiredSequence.get(i);
        if(i > 0) {
          sb.append(" <shadow:#000000FF><#5A4E7C>-></shadow> ");
        }
        if(i < session.currentStep) {
          sb.append("<#6EEB83>[").append(swIdx + 1).append("]");
        } else if(i == session.currentStep) {
          sb.append("<#FF7518>[").append(swIdx + 1).append("]");
        } else {
          sb.append("<#B8B0C8>[").append(swIdx + 1).append("]");
        }
      }
      lore.add(sb.toString());
      lore.add("");
      lore.add("<#F4F1DE>ขั้นตอน <#FFB703>" + Math.min(5, session.currentStep + 1) + "<shadow:#000000FF><#5A4E7C>/</shadow><#F4F1DE>5");
      HalloweenChat.setItemLore(monMeta, lore);
      monitor.setItemMeta(monMeta);
    }
    inv.setItem(8, monitor);

    int targetSwitch = (session.currentStep < 5) ? session.requiredSequence.get(session.currentStep) : -1;
    for(int i = 0; i < 5; i++) {
      boolean isOn = session.switchState[i];
      boolean isNext = (i == targetSwitch);

      ItemStack light;
      if(isOn) {
        light = XMaterial.LIME_STAINED_GLASS_PANE.parseItem();
      } else if(isNext) {
        light = XMaterial.YELLOW_STAINED_GLASS_PANE.parseItem();
      } else {
        light = XMaterial.RED_STAINED_GLASS_PANE.parseItem();
      }

      ItemMeta lMeta = light.getItemMeta();
      if(lMeta != null) {
        if(isOn) {
          HalloweenChat.setItemName(lMeta, "<shadow:#000000FF><#5A4E7C>[</shadow><#6EEB83>สวิตช์ " + (i + 1) + " เปิดแล้ว<shadow:#000000FF><#5A4E7C>]</shadow>");
        } else if(isNext) {
          HalloweenChat.setItemName(lMeta, "<shadow:#000000FF><#5A4E7C>[</shadow><#FFB703>สวิตช์ " + (i + 1) + " ลำดับถัดไป<shadow:#000000FF><#5A4E7C>]</shadow>");
        } else {
          HalloweenChat.setItemName(lMeta, "<shadow:#000000FF><#5A4E7C>[</shadow><#FF3B47>สวิตช์ " + (i + 1) + " ปิดอยู่<shadow:#000000FF><#5A4E7C>]</shadow>");
        }
        light.setItemMeta(lMeta);
      }
      inv.setItem(LIGHT_SLOTS[i], light);
    }

    for(int i = 0; i < 5; i++) {
      boolean isOn = session.switchState[i];
      boolean isNext = (!isOn && session.currentStep < session.requiredSequence.size() && session.requiredSequence.get(session.currentStep) == i);

      ItemStack sw;
      if(isOn) {
        sw = XMaterial.TRIPWIRE_HOOK.parseItem();
      } else if(isNext) {
        sw = XMaterial.REDSTONE_TORCH.parseItem();
      } else {
        sw = XMaterial.LEVER.parseItem();
      }
      ItemMeta swMeta = sw.getItemMeta();
      if(swMeta != null) {
        HalloweenChat.setItemName(swMeta, SWITCH_LABELS[i]);
        if(isOn) {
          HalloweenChat.setItemLore(
            swMeta,
            "",
            "<#6EEB83>เปิดใช้งานแล้ว"
          );
        } else if(isNext) {
          HalloweenChat.setItemLore(
            swMeta,
            "",
            "<#FFB703>สวิตช์ลำดับถัดไป",
            "",
            "<shadow:#000000FF><#5A4E7C>(</shadow><#B8B0C8>กดคลิกเพื่อสับสวิตช์<shadow:#000000FF><#5A4E7C>)</shadow>"
          );
        } else {
          HalloweenChat.setItemLore(
            swMeta,
            "",
            "<#FF3B47>ปิดอยู่",
            "",
            "<shadow:#000000FF><#5A4E7C>(</shadow><#B8B0C8>รอให้ถึงลำดับก่อน<shadow:#000000FF><#5A4E7C>)</shadow>"
          );
        }
        sw.setItemMeta(swMeta);
      }
      inv.setItem(SWITCH_SLOTS[i], sw);
    }

    ItemStack status = new ItemStack(session.currentStep == 5 ? Material.EMERALD : Material.COMPASS);
    ItemMeta stMeta = status.getItemMeta();
    if(stMeta != null) {
      HalloweenChat.setItemName(stMeta, session.currentStep == 5
        ? "<shadow:#000000FF><#5A4E7C>[</shadow><#6EEB83>ระบบไฟเชื่อมต่อสมบูรณ์<shadow:#000000FF><#5A4E7C>]</shadow>"
        : "<shadow:#000000FF><#5A4E7C>[</shadow><#FF7518>ความคืบหน้า " + session.currentStep + "/5<shadow:#000000FF><#5A4E7C>]</shadow>");
      HalloweenChat.setItemLore(
        stMeta,
        "",
        "<shadow:#000000FF><#5A4E7C>(</shadow><#B8B0C8>สับสวิตช์ตามลำดับไฟสีเหลือง<shadow:#000000FF><#5A4E7C>)</shadow>"
      );
      status.setItemMeta(stMeta);
    }
    inv.setItem(22, status);
  }

  @Override
  public boolean handleClick(Player player, int rawSlot, ClickType clickType) {
    if(completedSessions.contains(player.getUniqueId())) {
      return true;
    }

    PanelSession session = sessions.get(player.getUniqueId());
    if(session == null) {
      return true;
    }

    if(rawSlot == 0 || rawSlot == 8) {
      XSound.UI_BUTTON_CLICK.play(player.getLocation(), 0.5F, 1.2F);
      return true;
    }

    int clickedSwitch = -1;
    for(int i = 0; i < SWITCH_SLOTS.length; i++) {
      if(SWITCH_SLOTS[i] == rawSlot) {
        clickedSwitch = i;
        break;
      }
    }

    if(clickedSwitch != -1) {
      if(session.currentStep >= 5) {
        return true;
      }

      int expectedSwitch = session.requiredSequence.get(session.currentStep);

      if(clickedSwitch == expectedSwitch) {
        session.switchState[clickedSwitch] = true;
        session.currentStep++;

        XSound.BLOCK_LEVER_CLICK.play(player.getLocation(), 1F, 1.4F);
        XSound.BLOCK_NOTE_BLOCK_PLING.play(player.getLocation(), 0.8F, 0.8F + (session.currentStep * 0.25F));

        buildGui(player.getOpenInventory().getTopInventory(), session);

        if(session.currentStep >= 5) {
          completedSessions.add(player.getUniqueId());
          sessions.remove(player.getUniqueId());

          XSound.ENTITY_PLAYER_LEVELUP.play(player.getLocation(), 1F, 1.5F);
          HalloweenChat.sendActionBar(player, "<shadow:#000000FF><#5A4E7C>[</shadow><#6EEB83>Task<shadow:#000000FF><#5A4E7C>]</shadow> <#6EEB83>แผงควบคุมระบบไฟพร้อมทำงานแล้ว");

          Bukkit.getScheduler().runTaskLater(arena.getPlugin(), () -> {
            if(player.isOnline() && arena.getPlayers().contains(player) && !arena.getVotingManager().isVotingActive()) {
              if(isGui(player.getOpenInventory().getTitle())) {
                player.closeInventory();
              }
              arena.getTaskManager().completeTaskFromMinigame(player, TaskType.CHECK_CONTROL_PANEL);
            }
          }, 8L);
        }
      } else {
        XSound.ENTITY_GENERIC_EXPLODE.play(player.getLocation(), 0.6F, 1.8F);
        XSound.BLOCK_ANVIL_LAND.play(player.getLocation(), 0.5F, 0.7F);

        HalloweenChat.sendActionBar(player, "<shadow:#000000FF><#5A4E7C>[</shadow><#FF3B47>Task<shadow:#000000FF><#5A4E7C>]</shadow> <#FF3B47>สับสวิตช์ผิดลำดับ เบรกเกอร์รีเซ็ตใหม่ทั้งหมด");

        PanelSession fresh = new PanelSession();
        sessions.put(player.getUniqueId(), fresh);
        buildGui(player.getOpenInventory().getTopInventory(), fresh);
      }
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
