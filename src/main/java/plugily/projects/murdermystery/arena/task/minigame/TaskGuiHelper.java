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

import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import plugily.projects.minigamesbox.classic.utils.version.xseries.XMaterial;
import plugily.projects.minigamesbox.classic.utils.version.xseries.XSound;
import plugily.projects.murdermystery.utils.HalloweenChat;

public final class TaskGuiHelper {

  private TaskGuiHelper() {
  }

  public static ItemStack createBackgroundPane(XMaterial paneMaterial) {
    ItemStack pane = paneMaterial.parseItem();
    if(pane == null) {
      pane = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
    }
    ItemMeta meta = pane.getItemMeta();
    if(meta != null) {
      meta.setDisplayName(" ");
      try {
        meta.setHideTooltip(true);
      } catch(Throwable ignored) {
      }
      pane.setItemMeta(meta);
    }
    return pane;
  }

  public static void fillBackground(Inventory inv) {
    ItemStack bg = createBackgroundPane(XMaterial.BLACK_STAINED_GLASS_PANE);
    int size = inv.getSize();
    for(int i = 0; i < size; i++) {
      inv.setItem(i, bg);
    }
  }

  public static void applyCleanFlags(ItemMeta meta) {
    if(meta == null) {
      return;
    }
    try {
      meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS);
    } catch(Throwable ignored) {
    }
    try {
      meta.addItemFlags(ItemFlag.valueOf("HIDE_ADDITIONAL_TOOLTIP"));
    } catch(Throwable ignored) {
      try {
        meta.addItemFlags(ItemFlag.valueOf("HIDE_POTION_EFFECTS"));
      } catch(Throwable ignoredLegacy) {
      }
    }
  }

  public static void setGlint(ItemMeta meta, boolean glint) {
    if(meta == null) {
      return;
    }
    applyCleanFlags(meta);
    try {
      meta.setEnchantmentGlintOverride(glint ? Boolean.TRUE : null);
      return;
    } catch(Throwable ignored) {
    }
    if(glint) {
      try {
        meta.addEnchant(Enchantment.UNBREAKING, 1, true);
      } catch(Throwable ignored) {
      }
    }
  }

  public static ItemStack createItem(XMaterial material, int amount, boolean glint, String displayName, String... lore) {
    ItemStack item = material.parseItem();
    if(item == null) {
      item = new ItemStack(Material.PAPER);
    }
    item.setAmount(Math.max(1, Math.min(64, amount)));
    ItemMeta meta = item.getItemMeta();
    if(meta != null) {
      HalloweenChat.setItemName(meta, displayName);
      if(lore != null && lore.length > 0) {
        HalloweenChat.setItemLore(meta, lore);
      }
      setGlint(meta, glint);
      item.setItemMeta(meta);
    }
    return item;
  }

  public static ItemStack createItem(Material material, int amount, boolean glint, String displayName, String... lore) {
    ItemStack item = new ItemStack(material, Math.max(1, Math.min(64, amount)));
    ItemMeta meta = item.getItemMeta();
    if(meta != null) {
      HalloweenChat.setItemName(meta, displayName);
      if(lore != null && lore.length > 0) {
        HalloweenChat.setItemLore(meta, lore);
      }
      setGlint(meta, glint);
      item.setItemMeta(meta);
    }
    return item;
  }

  public static ItemStack createColoredPotion(Color color, boolean glint, String displayName, String... lore) {
    ItemStack item = new ItemStack(Material.POTION, 1);
    ItemMeta meta = item.getItemMeta();
    if(meta != null) {
      if(meta instanceof PotionMeta potionMeta && color != null) {
        try {
          potionMeta.setColor(color);
        } catch(Throwable ignored) {
        }
      }
      HalloweenChat.setItemName(meta, displayName);
      if(lore != null && lore.length > 0) {
        HalloweenChat.setItemLore(meta, lore);
      }
      setGlint(meta, glint);
      item.setItemMeta(meta);
    }
    return item;
  }

  public static String clickName(String label) {
    return "<shadow:#000000FF><#5A4E7C>[</shadow><#FFB703>คลิก<shadow:#000000FF><#5A4E7C>]</shadow> <#F4F1DE>" + label;
  }

  public static String confirmName(String label) {
    return "<shadow:#000000FF><#5A4E7C>[</shadow><#6EEB83>คลิกยืนยัน<shadow:#000000FF><#5A4E7C>]</shadow> <#6EEB83>" + label;
  }

  public static String lockedName(String label) {
    return "<shadow:#000000FF><#5A4E7C>[</shadow><#B8B0C8>ล็อกอยู่<shadow:#000000FF><#5A4E7C>]</shadow> <#B8B0C8>" + label;
  }

  public static String doneName(String label) {
    return "<shadow:#000000FF><#5A4E7C>[</shadow><#6EEB83>สำเร็จ<shadow:#000000FF><#5A4E7C>]</shadow> <#F4F1DE>" + label;
  }

  public static String tag(String colorHex, String text) {
    return "<shadow:#000000FF><#5A4E7C>[</shadow>" + colorHex + text + "<shadow:#000000FF><#5A4E7C>]</shadow>";
  }

  public static String hint(String colorHex, String text) {
    return "<shadow:#000000FF><#5A4E7C>(</shadow>" + colorHex + text + "<shadow:#000000FF><#5A4E7C>)</shadow>";
  }

  public static String buildPipeBar(int current, int max, int segments) {
    int filled = max <= 0 ? 0 : Math.max(0, Math.min(segments, (int) Math.round(((double) current / max) * segments)));
    StringBuilder sb = new StringBuilder();
    if(filled > 0) {
      sb.append("<#6EEB83>");
      for(int i = 0; i < filled; i++) {
        sb.append("|");
      }
    }
    if(filled < segments) {
      sb.append("<shadow:#000000FF><#5A4E7C>");
      for(int i = filled; i < segments; i++) {
        sb.append("|");
      }
      sb.append("</shadow>");
    }
    return sb.toString();
  }

  public static void sendTaskBar(Player player, String message) {
    HalloweenChat.sendActionBar(player, "<shadow:#000000FF><#5A4E7C>[</shadow><#FF7518>ภารกิจ<shadow:#000000FF><#5A4E7C>]</shadow> " + message);
  }

  public static void sendSuccessBar(Player player, String message) {
    HalloweenChat.sendActionBar(player, "<shadow:#000000FF><#5A4E7C>[</shadow><#6EEB83>ภารกิจ<shadow:#000000FF><#5A4E7C>]</shadow> <#6EEB83>" + message);
  }

  public static void sendErrorBar(Player player, String message) {
    HalloweenChat.sendActionBar(player, "<shadow:#000000FF><#5A4E7C>[</shadow><#FF3B47>ภารกิจ<shadow:#000000FF><#5A4E7C>]</shadow> <#FF3B47>" + message);
  }

  public static void playSound(Player player, XSound xSound, float volume, float pitch) {
    if(player == null || !player.isOnline() || xSound == null) {
      return;
    }
    try {
      Sound parsed = xSound.parseSound();
      if(parsed != null) {
        player.playSound(player.getLocation(), parsed, volume, pitch);
      } else {
        xSound.play(player, volume, pitch);
      }
    } catch(Throwable ignored) {
    }
  }
}
