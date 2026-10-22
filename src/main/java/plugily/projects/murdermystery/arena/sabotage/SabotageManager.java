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

package plugily.projects.murdermystery.arena.sabotage;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;
import plugily.projects.minigamesbox.api.arena.IArenaState;
import plugily.projects.minigamesbox.api.user.IUser;
import plugily.projects.minigamesbox.classic.utils.version.xseries.XMaterial;
import plugily.projects.minigamesbox.classic.utils.version.xseries.XSound;
import plugily.projects.murdermystery.arena.Arena;
import plugily.projects.murdermystery.arena.role.Role;
import plugily.projects.murdermystery.utils.HalloweenChat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SabotageManager {

  public static final String SABOTAGE_GUI_TITLE = HalloweenChat.color("<#5A4E7C>[ <gradient:#D90429:#FF5400><bold>เมนูก่อวินาศกรรม (Sabotage)</bold></gradient> <#5A4E7C>]");
  public static final String FIX_LIGHTS_TITLE = HalloweenChat.color("<#5A4E7C>[ <gradient:#FF5400:#FFB703><bold>กู้ระบบไฟฟ้า (Restore Power)</bold></gradient> <#5A4E7C>]");
  public static final String FIX_ENGINE_TITLE = HalloweenChat.color("<#5A4E7C>[ <gradient:#D90429:#FF5400><bold>วาล์วระบายความร้อนฉุกเฉิน</bold></gradient> <#5A4E7C>]");
  public static final String FIX_COMMS_TITLE = HalloweenChat.color("<#5A4E7C>[ <gradient:#9D4EDD:#FF7518><bold>ปรับคลื่นวิทยุกู้สัญญาณ (Reset Comms)</bold></gradient> <#5A4E7C>]");
  public static final int SABOTAGE_REMOTE_SLOT = 2;

  private static final int COOLDOWN_DURATION = 30;

  private final Arena arena;

  private SabotageType activeSabotage = null;
  private int countdownSeconds = 0;
  private int cooldownRemaining = 20;
  private BukkitTask tickTask = null;
  private BossBar meltdownBossBar = null;

  private TextDisplay fixTextDisplay = null;
  private BlockDisplay fixBlockDisplay = null;
  private Location activeFixLocation = null;

  private final boolean[] breakerSwitches = new boolean[5];

  public SabotageManager(Arena arena) {
    this.arena = arena;
  }

  private List<String> colorLines(List<String> lines) {
    List<String> colored = new ArrayList<>(lines.size());
    for(String line : lines) {
      colored.add(HalloweenChat.color(line));
    }
    return colored;
  }

  public boolean hasActiveSabotage() {
    return activeSabotage != null;
  }

  public SabotageType getActiveSabotage() {
    return activeSabotage;
  }

  public boolean isCommsJammed() {
    return activeSabotage == SabotageType.COMMS;
  }

  public boolean isLightsOut() {
    return activeSabotage == SabotageType.LIGHTS;
  }

  public int getCooldownRemaining() {
    return cooldownRemaining;
  }

  public Location getActiveFixLocation() {
    return activeFixLocation;
  }

  public ItemStack createSabotageRemoteItem() {
    ItemStack item = XMaterial.REDSTONE_TORCH.parseItem();
    ItemMeta meta = item.getItemMeta();
    if(meta != null) {
      meta.setDisplayName(HalloweenChat.color("<#5A4E7C>[ <gradient:#D90429:#FF5400><bold>เมนูก่อวินาศกรรม (Sabotage Remote)</bold></gradient> <#5A4E7C>]"));
      meta.setLore(colorLines(Arrays.asList(
        "<#B8B0C8>คลิกขวาเพื่อเปิดแผนที่ก่อวินาศกรรม",
        "<#B8B0C8>เลือกตัดไฟ, ทำให้เครื่องยนต์ระเบิด, หรือตัดเรดาร์",
        "<#FFB703>> คลิกขวาเพื่อใช้งาน (ช่อง 3)"
      )));
      item.setItemMeta(meta);
    }
    return item;
  }

  public boolean isSabotageRemote(ItemStack item) {
    if(item == null || !item.hasItemMeta() || !item.getItemMeta().hasDisplayName()) {
      return false;
    }
    return HalloweenChat.strip(item.getItemMeta().getDisplayName()).contains("เมนูก่อวินาศกรรม");
  }

  public void giveSabotageRemote(Player murderer) {
    if(murderer == null || !murderer.isOnline()) {
      return;
    }
    murderer.getInventory().setItem(SABOTAGE_REMOTE_SLOT, createSabotageRemoteItem());
  }

  public void start() {
    cleanup();
    cooldownRemaining = 20;

    tickTask = Bukkit.getScheduler().runTaskTimer(arena.getPlugin(), this::tick, 20L, 20L);
  }

  public void tick() {
    if(arena.getArenaState() != IArenaState.IN_GAME) {
      return;
    }

    if(activeSabotage == null) {
      if(cooldownRemaining > 0) {
        cooldownRemaining--;
      }
      return;
    }

    if(activeSabotage == SabotageType.LIGHTS) {
      for(Player p : arena.getPlayersLeft()) {
        IUser user = arena.getPlugin().getUserManager().getUser(p);
        if(Role.isRole(Role.MURDERER, user, arena)) {
          p.addPotionEffect(new PotionEffect(PotionEffectType.NIGHT_VISION, 60, 0, false, false, false));
        } else {
          p.addPotionEffect(new PotionEffect(PotionEffectType.DARKNESS, 60, 1, false, false, false));
        }
      }
    } else if(activeSabotage == SabotageType.ENGINE_MELTDOWN) {
      countdownSeconds--;

      if(meltdownBossBar != null) {
        String timeStr = String.format("%02d:%02d", countdownSeconds / 60, countdownSeconds % 60);
        meltdownBossBar.setTitle(HalloweenChat.color("<gradient:#D90429:#FF5400><bold>ฉุกเฉิน: เครื่องยนต์จะระเบิดใน " + timeStr + "! รีบไประบายความร้อน!</bold></gradient>"));
        double progress = Math.max(0.0, Math.min(1.0, (double) countdownSeconds / SabotageType.ENGINE_MELTDOWN.getCountdownSeconds()));
        meltdownBossBar.setProgress(progress);
        meltdownBossBar.setColor(countdownSeconds <= 15 ? BarColor.RED : BarColor.YELLOW);
      }

      if(fixTextDisplay != null && fixTextDisplay.isValid()) {
        String timeStr = String.format("%02d:%02d", countdownSeconds / 60, countdownSeconds % 60);
        fixTextDisplay.setText(HalloweenChat.color(
          "<gradient:#D90429:#FF5400><bold>จุดระบายความร้อนฉุกเฉิน (CRITICAL)</bold></gradient>\n<#FF3B47>เวลาที่เหลือ: <#FFB703><bold>" + timeStr + "</bold>\n<#F4F1DE>(คลิกขวาเพื่อระบายความร้อน)"));
      }

      String barMsg = HalloweenChat.color("<gradient:#D90429:#FF5400><bold>วิกฤตเครื่องยนต์ระเบิด!</bold></gradient> <#FFB703>เหลือเวลาอีก <#FF3B47><bold>" + countdownSeconds + " วินาที!</bold>");
      for(Player p : arena.getPlayersLeft()) {
        if(countdownSeconds <= 10) {
          XSound.BLOCK_NOTE_BLOCK_BELL.play(p.getLocation(), 1F, 1.8F);
        } else {
          XSound.BLOCK_NOTE_BLOCK_BELL.play(p.getLocation(), 0.7F, 1.2F);
        }
        HalloweenChat.sendActionBar(p, barMsg);
      }

      if(countdownSeconds <= 0) {
        triggerMeltdownVictory();
      }
    }
  }

  private void triggerMeltdownVictory() {
    Location explodeLoc = activeFixLocation != null ? activeFixLocation : arena.getPlayersLeft().get(0).getLocation();
    try {
      explodeLoc.getWorld().createExplosion(explodeLoc, 0F, false);
      String title = HalloweenChat.color("<gradient:#9E001A:#FF3B47><bold>เครื่องยนต์ระเบิด!</bold></gradient>");
      String sub = HalloweenChat.color("<#FF7518>คนร้ายชนะเกมด้วยการก่อวินาศกรรม! (Impostor Won by Sabotage)");
      for(Player p : arena.getPlayers()) {
        XSound.ENTITY_GENERIC_EXPLODE.play(p.getLocation(), 1F, 0.8F);
        HalloweenChat.sendTitles(p, title, sub, 10, 80, 20);
      }
    } catch(Exception ignored) {
    }

    cancelActiveSabotage("MELTDOWN_EXPLODED");
    arena.getPlugin().getArenaManager().stopGame(false, arena);
  }

  public void openSabotageGUI(Player player) {
    if(arena.getArenaState() != IArenaState.IN_GAME) {
      return;
    }
    if(arena.getVotingManager().isVotingActive()) {
      HalloweenChat.sendActionBar(player, HalloweenChat.color("<#FF3B47>ไม่สามารถก่อวินาศกรรมระหว่างการประชุมได้!"));
      return;
    }

    Inventory inv = Bukkit.createInventory(null, 27, SABOTAGE_GUI_TITLE);

    ItemStack bg = XMaterial.PURPLE_STAINED_GLASS_PANE.parseItem();
    ItemMeta bgMeta = bg.getItemMeta();
    if(bgMeta != null) {
      bgMeta.setDisplayName(HalloweenChat.color("<#5A4E7C> "));
      bg.setItemMeta(bgMeta);
    }
    for(int i = 0; i < 27; i++) {
      inv.setItem(i, bg);
    }

    boolean onCooldown = (cooldownRemaining > 0);
    boolean hasActive = (activeSabotage != null);

    inv.setItem(11, buildSabotageButton(SabotageType.LIGHTS, XMaterial.REDSTONE_LAMP, onCooldown, hasActive));
    inv.setItem(13, buildSabotageButton(SabotageType.ENGINE_MELTDOWN, XMaterial.TNT, onCooldown, hasActive));
    inv.setItem(15, buildSabotageButton(SabotageType.COMMS, XMaterial.LIGHTNING_ROD, onCooldown, hasActive));

    ItemStack status;
    if(hasActive) {
      status = XMaterial.REDSTONE_BLOCK.parseItem();
    } else if(onCooldown) {
      status = XMaterial.CLOCK.parseItem();
    } else {
      status = XMaterial.EMERALD.parseItem();
    }
    ItemMeta sMeta = status.getItemMeta();
    if(sMeta != null) {
      if(hasActive) {
        sMeta.setDisplayName(HalloweenChat.color("<#5A4E7C>[ <gradient:#D90429:#FF5400><bold>[!] มีการก่อวินาศกรรมทำงานอยู่: " + activeSabotage.getIcon() + "</bold></gradient> <#5A4E7C>]"));
        sMeta.setLore(colorLines(Arrays.asList("<#B8B0C8>ต้องรอให้ผู้โดยสารแก้ไขระบบก่อน จึงจะสั่งการใหม่ได้")));
      } else if(onCooldown) {
        sMeta.setDisplayName(HalloweenChat.color("<#5A4E7C>[ <gradient:#FF5400:#FFB703><bold>กำลังคูลดาวน์: รออีก " + cooldownRemaining + " วินาที</bold></gradient> <#5A4E7C>]"));
        sMeta.setLore(colorLines(Arrays.asList("<#B8B0C8>ระบบก่อวินาศกรรมกำลังรีชาร์จพลังงาน")));
      } else {
        sMeta.setDisplayName(HalloweenChat.color("<#5A4E7C>[ <gradient:#38B000:#9EF01A><bold>[/] ระบบพร้อมก่อวินาศกรรม!</bold></gradient> <#5A4E7C>]"));
        sMeta.setLore(colorLines(Arrays.asList("<#B8B0C8>คลิกเลือกแผนก่อกวนด้านบนเพื่อเริ่มปฏิบัติการ")));
      }
      status.setItemMeta(sMeta);
    }
    inv.setItem(22, status);

    player.openInventory(inv);
    XSound.BLOCK_CHEST_OPEN.play(player.getLocation(), 0.8F, 1.2F);
  }

  private ItemStack buildSabotageButton(SabotageType type, XMaterial readyMat, boolean onCooldown, boolean hasActive) {
    ItemStack item;
    boolean isActiveThis = (activeSabotage == type);
    boolean hasLocation = resolveStationLocation(type) != null;

    if(isActiveThis) {
      item = XMaterial.REDSTONE_BLOCK.parseItem();
    } else if(!hasLocation || hasActive || onCooldown) {
      item = XMaterial.BARRIER.parseItem();
    } else {
      item = readyMat.parseItem();
    }

    ItemMeta meta = item.getItemMeta();
    if(meta != null) {
      String prefix = isActiveThis ? "<#FFB703>* " : (!hasLocation || hasActive || onCooldown ? "<#FF3B47>[x] " : "<#6EEB83>>> ");
      meta.setDisplayName(HalloweenChat.color(prefix + "<gradient:#FF5400:#FFB703><bold>" + type.getIcon() + " " + type.getDisplayName() + "</bold></gradient>"));
      List<String> lore = new ArrayList<>();
      if(type == SabotageType.LIGHTS) {
        lore.add("<#B8B0C8>ตัดกระแสไฟฟ้าทั้งแผนที่!");
        lore.add("<#B8B0C8>- ผู้โดยสารทุกคนจะมองเห็นได้แค่ระยะ 2-3 บล็อก (Darkness)");
        lore.add("<#B8B0C8>- คนร้ายมองเห็นชัดเจนปกติ (Night Vision)");
      } else if(type == SabotageType.ENGINE_MELTDOWN) {
        lore.add("<gradient:#D90429:#FF5400><bold>[วิกฤตระเบิดฉุกเฉิน!]</bold></gradient>");
        lore.add("<#B8B0C8>- สั่งให้เครื่องยนต์นับถอยหลังระเบิดใน 40 วินาที");
        lore.add("<#FF3B47>- หากผู้โดยสารไม่มาระบายความร้อน คนร้ายชนะเกมทันที!");
      } else if(type == SabotageType.COMMS) {
        lore.add("<#B8B0C8>รบกวนคลื่นวิทยุและเรดาร์!");
        lore.add("<#B8B0C8>- แผนที่ย่อในมือซ้ายจะใช้งานไม่ได้");
        lore.add("<#B8B0C8>- จุดภารกิจและเข็มทิศจะถูกซ่อนจนกว่าจะกู้ระบบ");
      }
      lore.add(" ");
      if(!hasLocation) {
        lore.add("<#FF3B47>[ ยังไม่ได้ตั้งจุด Sabotage นี้ในแมพ ]");
        lore.add("<#B8B0C8>ตั้งค่าด้วย: <#FFB703>/mma setsabotage " + arena.getId() + " " + type.getShortId());
      } else if(isActiveThis) {
        lore.add("<gradient:#FF5400:#FFB703><bold>[ กำลังทำงานอยู่! ]</bold></gradient>");
      } else if(hasActive) {
        lore.add("<#FF3B47>[ ใช้งานไม่ได้: มี Sabotage อื่นทำงานอยู่ ]");
      } else if(onCooldown) {
        lore.add("<#FF3B47>[ คูลดาวน์: รออีก " + cooldownRemaining + " วินาที ]");
      } else {
        lore.add("<#6EEB83>> คลิกเพื่อเริ่มก่อวินาศกรรมนี้!");
      }
      meta.setLore(colorLines(lore));
      item.setItemMeta(meta);
    }
    return item;
  }

  public void handleSabotageClick(Player player, int rawSlot) {
    if(arena.getVotingManager().isVotingActive()) {
      return;
    }

    if(cooldownRemaining > 0) {
      XSound.ENTITY_VILLAGER_NO.play(player.getLocation(), 0.8F, 1F);
      HalloweenChat.sendActionBar(player, HalloweenChat.color("<#FF3B47>[คูลดาวน์] กรุณารออีก <#FFB703>" + cooldownRemaining + " วินาที!"));
      return;
    }
    if(activeSabotage != null) {
      XSound.ENTITY_VILLAGER_NO.play(player.getLocation(), 0.8F, 1F);
      HalloweenChat.sendActionBar(player, HalloweenChat.color("<#FF3B47>มีการก่อวินาศกรรมกำลังทำงานอยู่!"));
      return;
    }

    SabotageType selected = null;
    if(rawSlot == 11) {
      selected = SabotageType.LIGHTS;
    } else if(rawSlot == 13) {
      selected = SabotageType.ENGINE_MELTDOWN;
    } else if(rawSlot == 15) {
      selected = SabotageType.COMMS;
    }

    if(selected != null) {
      if(resolveStationLocation(selected) == null) {
        XSound.ENTITY_VILLAGER_NO.play(player.getLocation(), 0.8F, 1F);
        HalloweenChat.sendActionBar(player, HalloweenChat.color("<#FF3B47>ยังไม่ได้ตั้งจุด Sabotage นี้ในแมพ! <#FFB703>(/mma setsabotage " + arena.getId() + " " + selected.getShortId() + ")"));
        return;
      }
      player.closeInventory();
      triggerSabotage(player, selected);
    }
  }

  public void triggerSabotage(Player murderer, SabotageType type) {
    Location stationLoc = resolveStationLocation(type);
    if(stationLoc == null) {
      return;
    }
    this.activeSabotage = type;
    this.countdownSeconds = type.getCountdownSeconds();
    this.activeFixLocation = stationLoc;

    spawnEmergencyVisuals(type, activeFixLocation);

    if(type == SabotageType.LIGHTS) {
      for(int i = 0; i < 5; i++) {
        breakerSwitches[i] = (Math.random() < 0.3);
      }
      breakerSwitches[0] = false;
      breakerSwitches[2] = false;
      breakerSwitches[4] = false;
    }

    if(type == SabotageType.LIGHTS) {
      String title = HalloweenChat.color("<gradient:#D90429:#FF5400><bold>ไฟฟ้าดับ! (LIGHTS OUT)</bold></gradient>");
      String sub = HalloweenChat.color("<#FFB703>รีบไปสับคัตเอาท์ที่แผงไฟเพื่อกู้ระบบ!");
      for(Player p : arena.getPlayersLeft()) {
        XSound.ENTITY_LIGHTNING_BOLT_THUNDER.play(p.getLocation(), 0.7F, 0.6F);
        XSound.BLOCK_REDSTONE_TORCH_BURNOUT.play(p.getLocation(), 1F, 0.8F);
        HalloweenChat.sendTitles(p, title, sub, 10, 60, 10);
      }
    } else if(type == SabotageType.ENGINE_MELTDOWN) {
      if(meltdownBossBar != null) {
        meltdownBossBar.removeAll();
      }
      meltdownBossBar = Bukkit.createBossBar(
        HalloweenChat.color("<gradient:#D90429:#FF5400><bold>ฉุกเฉิน: เครื่องยนต์จะระเบิดใน 00:40! รีบไประบายความร้อน!</bold></gradient>"),
        BarColor.RED,
        BarStyle.SOLID
      );
      meltdownBossBar.setProgress(1.0);
      for(Player p : arena.getPlayers()) {
        meltdownBossBar.addPlayer(p);
      }

      String title = HalloweenChat.color("<gradient:#9E001A:#FF3B47><bold>วิกฤตเครื่องยนต์ระเบิด!</bold></gradient>");
      String sub = HalloweenChat.color("<#FFB703>นับถอยหลัง 40 วินาที! รีบไประบายความร้อนที่ห้องเครื่อง!");
      for(Player p : arena.getPlayersLeft()) {
        XSound.BLOCK_BEACON_DEACTIVATE.play(p.getLocation(), 1F, 0.8F);
        HalloweenChat.sendTitles(p, title, sub, 10, 60, 10);
      }
    } else if(type == SabotageType.COMMS) {
      arena.getTaskManager().getStationDisplays().forEach(s -> {
        try {
          if(s.getActiveHologram() != null) {
            for(Player p : arena.getPlayersLeft()) {
              p.hideEntity(arena.getPlugin(), s.getActiveHologram());
            }
          }
        } catch(Throwable ignored) {
        }
      });

      String title = HalloweenChat.color("<gradient:#9D4EDD:#FF7518><bold>สัญญาณเรดาร์ถูกรบกวน!</bold></gradient>");
      String sub = HalloweenChat.color("<#FFB703>แผนที่ย่อและจุดภารกิจถูกปิดกั้น! รีบไปปรับคลื่นวิทยุ!");
      for(Player p : arena.getPlayersLeft()) {
        XSound.BLOCK_FIRE_EXTINGUISH.play(p.getLocation(), 0.8F, 1.8F);
        XSound.BLOCK_NOTE_BLOCK_SNARE.play(p.getLocation(), 1F, 0.6F);
        HalloweenChat.sendTitles(p, title, sub, 10, 60, 10);
      }
    }
    arena.updateCampfireHologram();
  }

  private Location resolveStationLocation(SabotageType type) {
    return arena.getSabotageLocation(type);
  }

  public SabotageType getSabotageStationAt(Location clickedLoc) {
    if(clickedLoc == null || clickedLoc.getWorld() == null) {
      return null;
    }
    for(SabotageType type : SabotageType.values()) {
      Location station = arena.getSabotageLocation(type);
      if(station != null && station.getWorld() != null && station.getWorld().equals(clickedLoc.getWorld())) {
        if(clickedLoc.getBlockX() == station.getBlockX()
          && clickedLoc.getBlockY() == station.getBlockY()
          && clickedLoc.getBlockZ() == station.getBlockZ()) {
          return type;
        }
      }
    }
    return null;
  }

  private void spawnEmergencyVisuals(SabotageType type, Location loc) {
    removeEmergencyVisuals();
    if(loc == null || loc.getWorld() == null) {
      return;
    }

    Location textLoc = loc.clone().add(0.5, 2.0, 0.5);
    String text;
    if(type == SabotageType.LIGHTS) {
      text = "<#5A4E7C>[ <gradient:#FF5400:#FFB703><bold>จุดกู้ระบบไฟฟ้า (ELECTRICAL)</bold></gradient> <#5A4E7C>]\n<#F4F1DE>(คลิกขวาเพื่อสับคัตเอาท์เปิดไฟ)";
    } else if(type == SabotageType.ENGINE_MELTDOWN) {
      text = "<gradient:#D90429:#FF5400><bold>จุดระบายความร้อนฉุกเฉิน (CRITICAL)</bold></gradient>\n<#FF3B47>เวลาที่เหลือ: <#FFB703><bold>00:40</bold>\n<#F4F1DE>(คลิกขวาเพื่อระบายความร้อน)";
    } else {
      text = "<#5A4E7C>[ <gradient:#9D4EDD:#FF7518><bold>จุดกู้สัญญาณเรดาร์ (COMMS)</bold></gradient> <#5A4E7C>]\n<#F4F1DE>(คลิกขวาเพื่อปรับคลื่นวิทยุ)";
    }

    fixTextDisplay = arena.spawnTextDisplay(textLoc, text, false);
    fixBlockDisplay = arena.spawnBlockDisplay(loc, null, ChatColor.RED);

    if(fixBlockDisplay != null) {
      for(Player p : arena.getPlayers()) {
        try {
          arena.setEntityGlowColor(fixBlockDisplay, ChatColor.RED);
          p.showEntity(arena.getPlugin(), fixBlockDisplay);
        } catch(Throwable ignored) {
        }
      }
    }
  }

  private void removeEmergencyVisuals() {
    if(fixTextDisplay != null && fixTextDisplay.isValid()) {
      fixTextDisplay.remove();
      fixTextDisplay = null;
    }
    if(fixBlockDisplay != null && fixBlockDisplay.isValid()) {
      fixBlockDisplay.remove();
      fixBlockDisplay = null;
    }
  }

  public boolean isAtFixStation(Player player, Location clickedLoc) {
    if(activeSabotage == null || activeFixLocation == null) {
      return false;
    }
    if(clickedLoc != null && clickedLoc.getWorld() != null && clickedLoc.getWorld().equals(activeFixLocation.getWorld())) {
      if(clickedLoc.getBlockX() == activeFixLocation.getBlockX() && clickedLoc.getBlockY() == activeFixLocation.getBlockY() && clickedLoc.getBlockZ() == activeFixLocation.getBlockZ()) {
        return true;
      }
      if(clickedLoc.distanceSquared(activeFixLocation) <= 4.0) {
        return true;
      }
    }
    if(player != null && player.getWorld().equals(activeFixLocation.getWorld())) {
      try {
        Block target = player.getTargetBlockExact(5);
        if(target != null && target.getLocation().distanceSquared(activeFixLocation) <= 4.0) {
          return true;
        }
      } catch(Throwable ignored) {
      }
    }
    return false;
  }

  public void openFixGUI(Player player) {
    if(activeSabotage == null) {
      return;
    }

    if(activeSabotage == SabotageType.LIGHTS) {
      openFixLightsGUI(player);
    } else if(activeSabotage == SabotageType.ENGINE_MELTDOWN) {
      openFixEngineGUI(player);
    } else if(activeSabotage == SabotageType.COMMS) {
      openFixCommsGUI(player);
    }
  }

  private void openFixLightsGUI(Player player) {
    Inventory inv = Bukkit.createInventory(null, 27, FIX_LIGHTS_TITLE);
    buildFixLightsGUI(inv);
    player.openInventory(inv);
    XSound.BLOCK_CHEST_OPEN.play(player.getLocation(), 0.8F, 1.2F);
    HalloweenChat.sendActionBar(player, HalloweenChat.color("<gradient:#FF5400:#FFB703><bold>[กู้ระบบไฟ]</bold></gradient> <#F4F1DE>คลิกสับคัตเอาท์ทั้งหมดให้เป็นสีเขียว (ON)!"));
  }

  private void buildFixLightsGUI(Inventory inv) {
    ItemStack bg = XMaterial.PURPLE_STAINED_GLASS_PANE.parseItem();
    ItemMeta bgMeta = bg.getItemMeta();
    if(bgMeta != null) {
      bgMeta.setDisplayName(HalloweenChat.color("<#5A4E7C> "));
      bg.setItemMeta(bgMeta);
    }
    for(int i = 0; i < 27; i++) {
      inv.setItem(i, bg);
    }

    int[] switchSlots = {11, 12, 13, 14, 15};
    for(int i = 0; i < 5; i++) {
      boolean isOn = breakerSwitches[i];
      ItemStack sw = isOn ? XMaterial.LIME_STAINED_GLASS_PANE.parseItem() : XMaterial.RED_STAINED_GLASS_PANE.parseItem();
      ItemMeta sMeta = sw.getItemMeta();
      if(sMeta != null) {
        sMeta.setDisplayName(HalloweenChat.color(isOn
          ? "<gradient:#38B000:#9EF01A><bold>* คัตเอาท์ " + (i + 1) + " [ON เปิดแล้ว [/]]</bold></gradient>"
          : "<gradient:#D90429:#FF5400><bold>* คัตเอาท์ " + (i + 1) + " [OFF สับลงอยู่ [x]]</bold></gradient>"));
        sMeta.setLore(colorLines(Arrays.asList(isOn ? "<#B8B0C8>(ระบบไฟเส้นนี้ทำงานแล้ว)" : "<#FFB703>> คลิกเพื่อสับสวิตช์ขึ้นเป็น ON")));
        sw.setItemMeta(sMeta);
      }
      inv.setItem(switchSlots[i], sw);
    }
  }

  private void openFixEngineGUI(Player player) {
    Inventory inv = Bukkit.createInventory(null, 27, FIX_ENGINE_TITLE);
    ItemStack bg = XMaterial.PURPLE_STAINED_GLASS_PANE.parseItem();
    ItemMeta bgMeta = bg.getItemMeta();
    if(bgMeta != null) {
      bgMeta.setDisplayName(HalloweenChat.color("<#5A4E7C> "));
      bg.setItemMeta(bgMeta);
    }
    for(int i = 0; i < 27; i++) {
      inv.setItem(i, bg);
    }

    ItemStack coolBtn = XMaterial.ICE.parseItem();
    ItemMeta cMeta = coolBtn.getItemMeta();
    if(cMeta != null) {
      cMeta.setDisplayName(HalloweenChat.color("<#5A4E7C>[ <gradient:#00B4D8:#72EFDD><bold>กดระบายความร้อนฉุกเฉิน (Flush Coolant)</bold></gradient> <#5A4E7C>]"));
      cMeta.setLore(colorLines(Arrays.asList(
        "<#B8B0C8>ฉีดน้ำยาหล่อเย็นดับความร้อนเครื่องยนต์",
        "<#FFB703>> คลิกทันทีเพื่อหยุดการระเบิด!"
      )));
      coolBtn.setItemMeta(cMeta);
    }
    inv.setItem(13, coolBtn);

    player.openInventory(inv);
    XSound.BLOCK_CHEST_OPEN.play(player.getLocation(), 0.8F, 1.2F);
    HalloweenChat.sendActionBar(player, HalloweenChat.color("<gradient:#D90429:#FF5400><bold>[วิกฤตเครื่องยนต์]</bold></gradient> <#FFB703>คลิกปุ่มตรงกลางเพื่อฉีดน้ำยาระบายความร้อนด่วน!"));
  }

  private void openFixCommsGUI(Player player) {
    Inventory inv = Bukkit.createInventory(null, 27, FIX_COMMS_TITLE);
    ItemStack bg = XMaterial.PURPLE_STAINED_GLASS_PANE.parseItem();
    ItemMeta bgMeta = bg.getItemMeta();
    if(bgMeta != null) {
      bgMeta.setDisplayName(HalloweenChat.color("<#5A4E7C> "));
      bg.setItemMeta(bgMeta);
    }
    for(int i = 0; i < 27; i++) {
      inv.setItem(i, bg);
    }

    ItemStack tuner = XMaterial.COMPASS.parseItem();
    ItemMeta tMeta = tuner.getItemMeta();
    if(tMeta != null) {
      tMeta.setDisplayName(HalloweenChat.color("<#5A4E7C>[ <gradient:#FF5400:#FFB703><bold>คลิกจูนคลื่นความถี่วิทยุ (Lock 101.5 MHz)</bold></gradient> <#5A4E7C>]"));
      tMeta.setLore(colorLines(Arrays.asList(
        "<#B8B0C8>ปรับคลื่นความถี่เพื่อทะลวงสัญญาณรบกวน",
        "<#FFB703>> คลิกเพื่อล็อคสัญญาณวิทยุและเรดาร์"
      )));
      tuner.setItemMeta(tMeta);
    }
    inv.setItem(13, tuner);

    player.openInventory(inv);
    XSound.BLOCK_CHEST_OPEN.play(player.getLocation(), 0.8F, 1.2F);
    HalloweenChat.sendActionBar(player, HalloweenChat.color("<gradient:#9D4EDD:#FF7518><bold>[กู้เรดาร์]</bold></gradient> <#FFB703>คลิกปุ่มตรงกลางเพื่อจูนคลื่นสัญญาณวิทยุ!"));
  }

  public boolean isSabotageGUI(String title) {
    return title != null && HalloweenChat.strip(title).contains("เมนูก่อวินาศกรรม");
  }

  public boolean isFixGUI(String title) {
    if(title == null) {
      return false;
    }
    String plain = HalloweenChat.strip(title);
    return plain.contains("กู้ระบบไฟฟ้า") || plain.contains("วาล์วระบายความร้อน") || plain.contains("กู้สัญญาณ");
  }

  public void handleFixClick(Player player, int rawSlot) {
    if(activeSabotage == null) {
      return;
    }

    if(activeSabotage == SabotageType.LIGHTS) {
      int[] switchSlots = {11, 12, 13, 14, 15};
      for(int i = 0; i < 5; i++) {
        if(switchSlots[i] == rawSlot) {
          breakerSwitches[i] = !breakerSwitches[i];
          XSound.BLOCK_LEVER_CLICK.play(player.getLocation(), 1F, breakerSwitches[i] ? 1.4F : 0.8F);

          boolean allOn = true;
          for(boolean b : breakerSwitches) {
            if(!b) {
              allOn = false;
              break;
            }
          }

          if(allOn) {
            player.closeInventory();
            resolveActiveSabotage(player);
          } else {
            buildFixLightsGUI(player.getOpenInventory().getTopInventory());
          }
          return;
        }
      }
    } else if(activeSabotage == SabotageType.ENGINE_MELTDOWN) {
      if(rawSlot == 13) {
        player.closeInventory();
        resolveActiveSabotage(player);
      }
    } else if(activeSabotage == SabotageType.COMMS) {
      if(rawSlot == 13) {
        player.closeInventory();
        resolveActiveSabotage(player);
      }
    }
  }

  public void resolveActiveSabotage(Player fixer) {
    if(activeSabotage == null) {
      return;
    }

    SabotageType resolved = activeSabotage;
    activeSabotage = null;
    cooldownRemaining = COOLDOWN_DURATION;

    removeEmergencyVisuals();

    if(meltdownBossBar != null) {
      meltdownBossBar.setVisible(false);
      meltdownBossBar.removeAll();
      meltdownBossBar = null;
    }

    for(Player p : arena.getPlayersLeft()) {
      p.removePotionEffect(PotionEffectType.DARKNESS);
      p.removePotionEffect(PotionEffectType.NIGHT_VISION);
    }

    if(resolved == SabotageType.COMMS) {
      arena.getTaskManager().updateAllPlayersStationVisibility();
    }

    String title = HalloweenChat.color("<gradient:#38B000:#9EF01A><bold>[/] กู้ระบบสำเร็จ! (Sabotage Resolved)</bold></gradient>");
    String sub = HalloweenChat.color("<#FFB703>ผู้เล่น <#C77DFF>" + fixer.getName() + " <#FFB703>แก้ไขระบบเรียบร้อยแล้ว!");
    for(Player p : arena.getPlayersLeft()) {
      XSound.BLOCK_BEACON_ACTIVATE.play(p.getLocation(), 1F, 1.5F);
      XSound.ENTITY_PLAYER_LEVELUP.play(p.getLocation(), 0.8F, 1.5F);
      HalloweenChat.sendTitles(p, title, sub, 10, 60, 10);
    }
    arena.updateCampfireHologram();
  }

  public void cancelActiveSabotage(String reason) {
    if(activeSabotage == null) {
      return;
    }

    activeSabotage = null;
    cooldownRemaining = COOLDOWN_DURATION;

    removeEmergencyVisuals();

    if(meltdownBossBar != null) {
      meltdownBossBar.setVisible(false);
      meltdownBossBar.removeAll();
      meltdownBossBar = null;
    }

    for(Player p : arena.getPlayersLeft()) {
      p.removePotionEffect(PotionEffectType.DARKNESS);
      p.removePotionEffect(PotionEffectType.NIGHT_VISION);
    }

    arena.getTaskManager().updateAllPlayersStationVisibility();
    arena.updateCampfireHologram();
  }

  public void handleClose(Player player) {
  }

  public void addPlayerToBossBar(Player player) {
    if(meltdownBossBar != null && player != null) {
      meltdownBossBar.addPlayer(player);
    }
  }

  public void removePlayerFromBossBar(Player player) {
    if(meltdownBossBar != null && player != null) {
      meltdownBossBar.removePlayer(player);
    }
  }

  public void cleanup() {
    if(tickTask != null) {
      tickTask.cancel();
      tickTask = null;
    }
    if(meltdownBossBar != null) {
      meltdownBossBar.setVisible(false);
      meltdownBossBar.removeAll();
      meltdownBossBar = null;
    }
    removeEmergencyVisuals();
    activeSabotage = null;
    cooldownRemaining = COOLDOWN_DURATION;
    activeFixLocation = null;
  }
}
