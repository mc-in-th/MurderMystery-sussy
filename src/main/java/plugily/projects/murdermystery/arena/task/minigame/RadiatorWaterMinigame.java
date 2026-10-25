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

public class RadiatorWaterMinigame implements TaskMinigame {

  public static final String GUI_TITLE = HalloweenChat.color("<shadow:#000000FF><#5A4E7C>[</shadow><#FF7518>เช็คระดับน้ำในหม้อน้ำ<shadow:#000000FF><#5A4E7C>]</shadow>");

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
    if(title == null) {
      return false;
    }
    String plain = HalloweenChat.strip(title);
    return plain.contains("หม้อน้ำ") || plain.contains("Radiator");
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
    HalloweenChat.sendActionBar(player, "<shadow:#000000FF><#5A4E7C>[</shadow><#FF7518>Task<shadow:#000000FF><#5A4E7C>]</shadow> <#F4F1DE>เปิดฝาหม้อน้ำทางซ้าย แล้วเทน้ำยาหล่อเย็นทางขวาให้เต็ม");
  }

  private void buildGui(Inventory inv, RadiatorSession session) {
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
      HalloweenChat.setItemName(iMeta, "<shadow:#000000FF><#5A4E7C>[</shadow><#72EFDD>Radiator Coolant<shadow:#000000FF><#5A4E7C>]</shadow>");
      HalloweenChat.setItemLore(
        iMeta,
        "",
        "<#F4F1DE>1. คลิกเปิดฝาหม้อน้ำทางซ้าย",
        "<#F4F1DE>2. คลิกขวดน้ำยาหล่อเย็นทางขวาจนถึงขีดสีเขียว",
        "<#F4F1DE>3. กดปิดฝาหม้อน้ำด้านล่างเพื่อจบภารกิจ",
        "",
        "<shadow:#000000FF><#5A4E7C>(</shadow><#72EFDD>Visual Task ยืนยันตัวตนต่อผู้เล่นรอบข้าง<shadow:#000000FF><#5A4E7C>)</shadow>"
      );
      instruction.setItemMeta(iMeta);
    }
    inv.setItem(INSTRUCTION_SLOT, instruction);

    for(int i = 0; i < 4; i++) {
      ItemStack bar;
      if(i == 0) {
        bar = XMaterial.RED_STAINED_GLASS_PANE.parseItem();
      } else if(i == 1) {
        bar = (session.waterLevel >= 1 ? XMaterial.LIGHT_BLUE_STAINED_GLASS_PANE : XMaterial.GRAY_STAINED_GLASS_PANE).parseItem();
      } else if(i == 2) {
        bar = (session.waterLevel >= 2 ? XMaterial.LIGHT_BLUE_STAINED_GLASS_PANE : XMaterial.GRAY_STAINED_GLASS_PANE).parseItem();
      } else {
        bar = (session.waterLevel >= 3 ? XMaterial.LIME_STAINED_GLASS_PANE : XMaterial.GRAY_STAINED_GLASS_PANE).parseItem();
      }

      ItemMeta bMeta = bar.getItemMeta();
      if(bMeta != null) {
        if(i == 0) {
          HalloweenChat.setItemName(bMeta, "<shadow:#000000FF><#5A4E7C>[</shadow><#FF3B47>ระดับต่ำ<shadow:#000000FF><#5A4E7C>]</shadow>");
        } else if(i == 3) {
          HalloweenChat.setItemName(bMeta, session.waterLevel >= 3
            ? "<shadow:#000000FF><#5A4E7C>[</shadow><#6EEB83>ขีดมาตรฐานเต็ม<shadow:#000000FF><#5A4E7C>]</shadow>"
            : "<shadow:#000000FF><#5A4E7C>[</shadow><#B8B0C8>ขีดเป้าหมาย<shadow:#000000FF><#5A4E7C>]</shadow>");
        } else {
          HalloweenChat.setItemName(bMeta, session.waterLevel >= i
            ? "<shadow:#000000FF><#5A4E7C>[</shadow><#72EFDD>ระดับน้ำยาหล่อเย็น<shadow:#000000FF><#5A4E7C>]</shadow>"
            : "<shadow:#000000FF><#5A4E7C>[</shadow><#B8B0C8>ว่าง<shadow:#000000FF><#5A4E7C>]</shadow>");
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
        HalloweenChat.setItemName(cMeta, "<shadow:#000000FF><#5A4E7C>[</shadow><#6EEB83>เปิดฝาหม้อน้ำแล้ว<shadow:#000000FF><#5A4E7C>]</shadow>");
        HalloweenChat.setItemLore(cMeta, "", "<shadow:#000000FF><#5A4E7C>(</shadow><#B8B0C8>คลิกขวดน้ำยาทางขวาเพื่อเติม<shadow:#000000FF><#5A4E7C>)</shadow>");
        cap.setItemMeta(cMeta);
      }
    } else {
      cap = XMaterial.CAULDRON.parseItem();
      ItemMeta cMeta = cap.getItemMeta();
      if(cMeta != null) {
        HalloweenChat.setItemName(cMeta, "<shadow:#000000FF><#5A4E7C>[</shadow><#FFB703>ฝาหม้อน้ำ<shadow:#000000FF><#5A4E7C>]</shadow>");
        HalloweenChat.setItemLore(
          cMeta,
          "",
          "<#F4F1DE>เปิดฝาระบายแรงดันก่อนเติมน้ำยา",
          "",
          "<shadow:#000000FF><#5A4E7C>(</shadow><#B8B0C8>กดคลิกเพื่อเปิดฝาหม้อน้ำ<shadow:#000000FF><#5A4E7C>)</shadow>"
        );
        cap.setItemMeta(cMeta);
      }
    }
    inv.setItem(CAP_SLOT, cap);

    ItemStack funnel = new ItemStack(session.waterLevel >= 3 ? Material.WATER_BUCKET : Material.BUCKET);
    ItemMeta fMeta = funnel.getItemMeta();
    if(fMeta != null) {
      HalloweenChat.setItemName(fMeta, session.waterLevel >= 3
        ? "<shadow:#000000FF><#5A4E7C>[</shadow><#6EEB83>น้ำยาหล่อเย็นเต็มแล้ว<shadow:#000000FF><#5A4E7C>]</shadow>"
        : "<shadow:#000000FF><#5A4E7C>[</shadow><#72EFDD>ช่องเติมน้ำยาหม้อน้ำ<shadow:#000000FF><#5A4E7C>]</shadow>");
      HalloweenChat.setItemLore(
        fMeta,
        "",
        "<#F4F1DE>สถานะ " + (!session.capOpen ? "<#FF3B47>ฝาปิดอยู่" : (session.waterLevel >= 3 ? "<#6EEB83>เต็มมาตรฐาน" : "<#FFB703>พร้อมเติมน้ำยา"))
      );
      funnel.setItemMeta(fMeta);
    }
    inv.setItem(FUNNEL_SLOT, funnel);

    ItemStack bottle = new ItemStack(Material.POTION);
    ItemMeta bMeta = bottle.getItemMeta();
    if(bMeta != null) {
      if(!session.capOpen) {
        HalloweenChat.setItemName(bMeta, "<shadow:#000000FF><#5A4E7C>[</shadow><#B8B0C8>ขวดน้ำยาหล่อเย็น<shadow:#000000FF><#5A4E7C>]</shadow>");
        HalloweenChat.setItemLore(bMeta, "", "<shadow:#000000FF><#5A4E7C>(</shadow><#FF3B47>กรุณาเปิดฝาหม้อน้ำทางซ้ายก่อน<shadow:#000000FF><#5A4E7C>)</shadow>");
      } else if(session.waterLevel >= 3) {
        HalloweenChat.setItemName(bMeta, "<shadow:#000000FF><#5A4E7C>[</shadow><#6EEB83>เติมถึงขีดมาตรฐานแล้ว<shadow:#000000FF><#5A4E7C>]</shadow>");
        HalloweenChat.setItemLore(bMeta, "", "<shadow:#000000FF><#5A4E7C>(</shadow><#6EEB83>กดปิดฝาหม้อน้ำด้านล่างได้เลย<shadow:#000000FF><#5A4E7C>)</shadow>");
      } else {
        HalloweenChat.setItemName(bMeta, "<shadow:#000000FF><#5A4E7C>[</shadow><#72EFDD>ขวดน้ำยาหล่อเย็น<shadow:#000000FF><#5A4E7C>]</shadow>");
        HalloweenChat.setItemLore(
          bMeta,
          "",
          "<#F4F1DE>เทน้ำยาหล่อเย็นลงในหม้อน้ำรถบัส",
          "",
          "<shadow:#000000FF><#5A4E7C>(</shadow><#B8B0C8>กดคลิกเพื่อเทน้ำยา +1 ระดับ<shadow:#000000FF><#5A4E7C>)</shadow>"
        );
      }
      bottle.setItemMeta(bMeta);
    }
    inv.setItem(BOTTLE_SLOT, bottle);

    ItemStack seal = (session.capOpen && session.waterLevel >= 3 ? XMaterial.EMERALD_BLOCK : XMaterial.IRON_BARS).parseItem();
    ItemMeta sMeta = seal.getItemMeta();
    if(sMeta != null) {
      if(session.capOpen && session.waterLevel >= 3) {
        HalloweenChat.setItemName(sMeta, "<shadow:#000000FF><#5A4E7C>[</shadow><#6EEB83>ปิดฝาหม้อน้ำให้แน่น<shadow:#000000FF><#5A4E7C>]</shadow>");
        HalloweenChat.setItemLore(
          sMeta,
          "",
          "<#F4F1DE>ระดับน้ำยาหล่อเย็นพร้อมใช้งาน",
          "",
          "<shadow:#000000FF><#5A4E7C>(</shadow><#B8B0C8>กดคลิกเพื่อจบภารกิจ<shadow:#000000FF><#5A4E7C>)</shadow>"
        );
      } else {
        HalloweenChat.setItemName(sMeta, "<shadow:#000000FF><#5A4E7C>[</shadow><#B8B0C8>ปิดฝาหม้อน้ำ<shadow:#000000FF><#5A4E7C>]</shadow>");
        HalloweenChat.setItemLore(
          sMeta,
          "",
          "<#FF3B47>ต้องเติมน้ำยาให้ถึงขีดสีเขียวก่อน",
          "",
          "<shadow:#000000FF><#5A4E7C>(</shadow><#B8B0C8>ระดับปัจจุบัน " + session.waterLevel + "/3<shadow:#000000FF><#5A4E7C>)</shadow>"
        );
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
      return true;
    }

    Inventory topInv = player.getOpenInventory().getTopInventory();

    if(rawSlot == CAP_SLOT) {
      if(!session.capOpen) {
        session.capOpen = true;
        XSound.BLOCK_IRON_TRAPDOOR_OPEN.play(player.getLocation(), 1F, 1.2F);
        XSound.BLOCK_FIRE_EXTINGUISH.play(player.getLocation(), 0.8F, 1.5F);
        arena.getTaskManager().getVisualEffectManager().triggerStepEffect(player, TaskType.CHECK_RADIATOR_WATER, 1, 4);
        HalloweenChat.sendActionBar(player, "<shadow:#000000FF><#5A4E7C>[</shadow><#FF7518>Task<shadow:#000000FF><#5A4E7C>]</shadow> <#6EEB83>เปิดฝาหม้อน้ำแล้ว คลิกขวดน้ำยาทางขวาเพื่อเติม");
      } else {
        XSound.UI_BUTTON_CLICK.play(player.getLocation(), 0.5F, 1.2F);
      }
      buildGui(topInv, session);
      return true;
    }

    if(rawSlot == BOTTLE_SLOT) {
      if(!session.capOpen) {
        XSound.ENTITY_VILLAGER_NO.play(player.getLocation(), 0.8F, 1F);
        HalloweenChat.sendActionBar(player, "<shadow:#000000FF><#5A4E7C>[</shadow><#FF3B47>Task<shadow:#000000FF><#5A4E7C>]</shadow> <#FF3B47>กรุณาเปิดฝาหม้อน้ำทางซ้ายก่อน");
        return true;
      }

      if(session.waterLevel < 3) {
        session.waterLevel++;
        XSound.ITEM_BUCKET_EMPTY.play(player.getLocation(), 0.9F, 1.2F);
        XSound.BLOCK_WATER_AMBIENT.play(player.getLocation(), 0.8F, 1.5F);
        arena.getTaskManager().getVisualEffectManager().triggerStepEffect(player, TaskType.CHECK_RADIATOR_WATER, session.waterLevel, 4);

        if(session.waterLevel >= 3) {
          XSound.BLOCK_NOTE_BLOCK_PLING.play(player.getLocation(), 1F, 1.8F);
          HalloweenChat.sendActionBar(player, "<shadow:#000000FF><#5A4E7C>[</shadow><#6EEB83>Task<shadow:#000000FF><#5A4E7C>]</shadow> <#6EEB83>น้ำยาหล่อเย็นเต็มแล้ว กดปิดฝาหม้อน้ำด้านล่างได้เลย");
        } else {
          HalloweenChat.sendActionBar(player, "<shadow:#000000FF><#5A4E7C>[</shadow><#FF7518>Task<shadow:#000000FF><#5A4E7C>]</shadow> <#F4F1DE>เติมน้ำยาหล่อเย็น <#72EFDD>" + session.waterLevel + "/3");
        }
      } else {
        XSound.UI_BUTTON_CLICK.play(player.getLocation(), 0.5F, 1.2F);
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
        HalloweenChat.sendActionBar(player, "<shadow:#000000FF><#5A4E7C>[</shadow><#6EEB83>Task<shadow:#000000FF><#5A4E7C>]</shadow> <#6EEB83>เช็คระดับน้ำในหม้อน้ำสำเร็จ");

        Bukkit.getScheduler().runTaskLater(arena.getPlugin(), () -> {
          if(player.isOnline() && arena.getPlayers().contains(player) && !arena.getVotingManager().isVotingActive()) {
            if(isGui(player.getOpenInventory().getTitle())) {
              player.closeInventory();
            }
            arena.getTaskManager().completeTaskFromMinigame(player, TaskType.CHECK_RADIATOR_WATER);
          }
        }, 8L);
      } else {
        XSound.ENTITY_VILLAGER_NO.play(player.getLocation(), 1F, 1F);
        HalloweenChat.sendActionBar(player, "<shadow:#000000FF><#5A4E7C>[</shadow><#FF3B47>Task<shadow:#000000FF><#5A4E7C>]</shadow> <#FF3B47>ต้องเปิดฝาและเติมน้ำยาให้ครบ 3 ระดับก่อน");
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
