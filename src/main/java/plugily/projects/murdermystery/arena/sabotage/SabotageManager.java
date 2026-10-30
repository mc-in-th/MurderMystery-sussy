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

import net.kyori.adventure.bossbar.BossBar;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Entity;
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
import plugily.projects.murdermystery.arena.ArenaUtils;
import plugily.projects.murdermystery.arena.role.Role;
import plugily.projects.murdermystery.arena.task.TaskManager.TaskStationDisplay;
import plugily.projects.murdermystery.arena.task.minigame.TaskGuiHelper;
import plugily.projects.murdermystery.utils.HalloweenChat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SabotageManager {

  public static final String SABOTAGE_GUI_TITLE = HalloweenChat.color("<shadow:#000000FF><#5A4E7C>[</shadow><#FF3B47>Sabotage<shadow:#000000FF><#5A4E7C>]</shadow> <#F4F1DE>เมนูก่อวินาศกรรม");
  public static final String FIX_LIGHTS_TITLE = HalloweenChat.color("<shadow:#000000FF><#5A4E7C>[</shadow><#FF7518>Sabotage<shadow:#000000FF><#5A4E7C>]</shadow> <#F4F1DE>กู้ระบบไฟฟ้า");
  public static final String FIX_ENGINE_TITLE = HalloweenChat.color("<shadow:#000000FF><#5A4E7C>[</shadow><#FF3B47>Sabotage<shadow:#000000FF><#5A4E7C>]</shadow> <#F4F1DE>วาล์วระบายความร้อน");
  public static final String FIX_COMMS_TITLE = HalloweenChat.color("<shadow:#000000FF><#5A4E7C>[</shadow><#FF7518>Sabotage<shadow:#000000FF><#5A4E7C>]</shadow> <#F4F1DE>กู้สัญญาณเรดาร์");
  public static final int SABOTAGE_REMOTE_SLOT = 2;

  private static final int COOLDOWN_DURATION = 30;

  private final Arena arena;

  private SabotageType activeSabotage = null;
  private int countdownSeconds = 0;
  private int cooldownRemaining = 20;
  private BukkitTask tickTask = null;
  private BossBar meltdownBossBar = null;
  private final Set<Player> meltdownBarViewers = new HashSet<>();

  private TextDisplay fixTextDisplay = null;
  private BlockDisplay fixBlockDisplay = null;
  private Location activeFixLocation = null;

  private final boolean[] breakerSwitches = new boolean[5];

  public SabotageManager(Arena arena) {
    this.arena = arena;
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

  public boolean isSabotageDisplayEntity(Entity entity) {
    return entity != null && (entity.equals(fixTextDisplay) || entity.equals(fixBlockDisplay));
  }

  private static boolean isSameBlock(Location a, Location b) {
    if(a == null || b == null || a.getWorld() == null || b.getWorld() == null) {
      return false;
    }
    return a.getWorld().equals(b.getWorld())
      && a.getBlockX() == b.getBlockX()
      && a.getBlockY() == b.getBlockY()
      && a.getBlockZ() == b.getBlockZ();
  }

  private List<Location> collectOtherStationHolograms(Location sabotageLoc) {
    List<Location> locs = new ArrayList<>();
    if(arena.getCampfireDisplay() != null && arena.getCampfireDisplay().isValid()
      && !isSameBlock(arena.getCampfireLocation(), sabotageLoc)) {
      locs.add(arena.getCampfireDisplay().getLocation());
    }
    for(TextDisplay vd : arena.getVentDisplays()) {
      if(vd != null && vd.isValid()) {
        locs.add(vd.getLocation());
      }
    }
    if(arena.getTaskManager() != null) {
      for(TaskStationDisplay st : arena.getTaskManager().getStationDisplays()) {
        if(!isSameBlock(st.getLocation(), sabotageLoc)
          && st.getActiveHologram() != null
          && st.getActiveHologram().isValid()) {
          locs.add(st.getActiveHologram().getLocation());
        }
      }
    }
    return locs;
  }

  public void showEmergencyVisualsToPlayer(Player player) {
    if(player == null || !player.isOnline()) {
      return;
    }
    if(fixTextDisplay != null && fixTextDisplay.isValid()) {
      try {
        player.showEntity(arena.getPlugin(), fixTextDisplay);
      } catch(Throwable ignored) {
      }
    }
    if(fixBlockDisplay != null && fixBlockDisplay.isValid()) {
      try {
        arena.setEntityGlowColor(fixBlockDisplay, ChatColor.RED);
        player.showEntity(arena.getPlugin(), fixBlockDisplay);
      } catch(Throwable ignored) {
      }
    }
  }

  private void hideMeltdownBossBarAll() {
    if(meltdownBossBar == null) {
      meltdownBarViewers.clear();
      return;
    }
    for(Player viewer : new ArrayList<>(meltdownBarViewers)) {
      if(viewer != null) {
        try {
          viewer.hideBossBar(meltdownBossBar);
        } catch(Throwable ignored) {
        }
      }
    }
    for(Player p : arena.getPlayers()) {
      if(p != null) {
        try {
          p.hideBossBar(meltdownBossBar);
        } catch(Throwable ignored) {
        }
      }
    }
    meltdownBarViewers.clear();
  }

  public ItemStack createSabotageRemoteItem() {
    ItemStack item = XMaterial.REDSTONE_TORCH.parseItem();
    ItemMeta meta = item.getItemMeta();
    if(meta != null) {
      HalloweenChat.setItemName(meta, "<shadow:#000000FF><#5A4E7C>[</shadow><#FF3B47>Sabotage<shadow:#000000FF><#5A4E7C>]</shadow> <#F4F1DE>เมนูก่อวินาศกรรม");
      HalloweenChat.setItemLore(meta, Arrays.asList(
        "<#F4F1DE>ตัดไฟ ระเบิดเครื่องยนต์ หรือตัดเรดาร์",
        "<shadow:#000000FF><#5A4E7C>(</shadow><#B8B0C8>กดคลิกขวาเพื่อเปิดเมนู<shadow:#000000FF><#5A4E7C>)</shadow>"
      ));
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
    if(arena.getVotingManager().isVotingActive() || arena.getCutsceneManager().isPlaying()) {
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
        meltdownBossBar.name(HalloweenChat.component("<shadow:#000000FF><#5A4E7C>[</shadow><#FF3B47>Sabotage<shadow:#000000FF><#5A4E7C>]</shadow> <#F4F1DE>เครื่องยนต์จะระเบิดใน <#FFB703>" + countdownSeconds + " <#F4F1DE>วิ <shadow:#000000FF><#5A4E7C>(</shadow><#B8B0C8>รีบไประบายความร้อน<shadow:#000000FF><#5A4E7C>)</shadow>"));
        float progress = (float) Math.max(0.0, Math.min(1.0, (double) countdownSeconds / SabotageType.ENGINE_MELTDOWN.getCountdownSeconds()));
        meltdownBossBar.progress(progress);
        meltdownBossBar.color(countdownSeconds <= 15 ? BossBar.Color.RED : BossBar.Color.YELLOW);
      }

      if(fixTextDisplay != null && fixTextDisplay.isValid()) {
        fixTextDisplay.text(HalloweenChat.component(
          "<shadow:#000000FF><#5A4E7C>[</shadow><#FF3B47>Sabotage<shadow:#000000FF><#5A4E7C>]</shadow>\n\n<#F4F1DE>ระบายความร้อนฉุกเฉิน\n<#FF3B47>เหลือเวลา <#FFB703>" + countdownSeconds + " <#FF3B47>วิ\n\n<shadow:#000000FF><#5A4E7C>(</shadow><#B8B0C8>กดคลิกขวาเพื่อระบายความร้อน<shadow:#000000FF><#5A4E7C>)</shadow>"));
      }

      String barMsg = "<shadow:#000000FF><#5A4E7C>[</shadow><#FF3B47>Sabotage<shadow:#000000FF><#5A4E7C>]</shadow> <#FF3B47>เครื่องยนต์จะระเบิดใน <#FFB703>" + countdownSeconds + " <#FF3B47>วิ";
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
    Location explodeLoc = activeFixLocation;
    if(explodeLoc == null && !arena.getPlayersLeft().isEmpty()) {
      explodeLoc = arena.getPlayersLeft().get(0).getLocation();
    }
    try {
      if(explodeLoc != null && explodeLoc.getWorld() != null) {
        explodeLoc.getWorld().createExplosion(explodeLoc, 0F, false);
      }
      String title = "<shadow:#000000FF><#5A4E7C>[</shadow><#FF3B47>Sabotage<shadow:#000000FF><#5A4E7C>]</shadow>";
      String sub = "<#FF3B47>เครื่องยนต์ระเบิด <shadow:#000000FF><#5A4E7C>(</shadow><#B8B0C8>ฆาตกรเป็นฝ่ายชนะ<shadow:#000000FF><#5A4E7C>)</shadow>";
      for(Player p : arena.getPlayers()) {
        XSound.ENTITY_GENERIC_EXPLODE.play(p.getLocation(), 1F, 0.8F);
        HalloweenChat.sendTitles(p, title, sub, 10, 80, 20);
      }
    } catch(Exception ignored) {
    }

    arena.setSabotageWin(true);
    cancelActiveSabotage("MELTDOWN_EXPLODED");
    arena.getPlugin().getArenaManager().stopGame(false, arena);
  }

  private ItemStack createBorderPane() {
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
    return bg;
  }

  public void openSabotageGUI(Player player) {
    if(arena.getArenaState() != IArenaState.IN_GAME || arena.getCutsceneManager().isPlaying() || ArenaUtils.isSpectator(player, arena)) {
      return;
    }
    IUser user = arena.getPlugin().getUserManager().getUser(player);
    if(!Role.isRole(Role.MURDERER, user, arena)) {
      return;
    }
    if(arena.getVotingManager().isVotingActive()) {
      HalloweenChat.sendActionBar(player, "<shadow:#000000FF><#5A4E7C>[</shadow><#FF3B47>Sabotage<shadow:#000000FF><#5A4E7C>]</shadow> <#FF3B47>ไม่สามารถก่อวินาศกรรมระหว่างประชุมได้");
      return;
    }

    Inventory inv = Bukkit.createInventory(null, 27, SABOTAGE_GUI_TITLE);

    ItemStack bg = createBorderPane();
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
        HalloweenChat.setItemName(sMeta, "<shadow:#000000FF><#5A4E7C>[</shadow><#FF3B47>Sabotage<shadow:#000000FF><#5A4E7C>]</shadow> <#FF3B47>กำลังทำงานอยู่");
        HalloweenChat.setItemLore(sMeta, Arrays.asList("<shadow:#000000FF><#5A4E7C>(</shadow><#B8B0C8>ต้องรอให้ผู้โดยสารแก้ไขระบบก่อน<shadow:#000000FF><#5A4E7C>)</shadow>"));
      } else if(onCooldown) {
        HalloweenChat.setItemName(sMeta, "<shadow:#000000FF><#5A4E7C>[</shadow><#FF3B47>Sabotage<shadow:#000000FF><#5A4E7C>]</shadow> <#FF3B47>คูลดาวน์ <#FFB703>" + cooldownRemaining + " <#FF3B47>วิ");
        HalloweenChat.setItemLore(sMeta, Arrays.asList("<shadow:#000000FF><#5A4E7C>(</shadow><#B8B0C8>กำลังรีชาร์จระบบก่อวินาศกรรม<shadow:#000000FF><#5A4E7C>)</shadow>"));
      } else {
        HalloweenChat.setItemName(sMeta, "<shadow:#000000FF><#5A4E7C>[</shadow><#6EEB83>Sabotage<shadow:#000000FF><#5A4E7C>]</shadow> <#6EEB83>พร้อมใช้งาน");
        HalloweenChat.setItemLore(sMeta, Arrays.asList("<shadow:#000000FF><#5A4E7C>(</shadow><#B8B0C8>คลิกเลือกการก่อวินาศกรรมด้านบน<shadow:#000000FF><#5A4E7C>)</shadow>"));
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
      HalloweenChat.setItemName(meta, "<shadow:#000000FF><#5A4E7C>[</shadow><#FF7518>" + type.getDisplayName() + "<shadow:#000000FF><#5A4E7C>]</shadow>");
      List<String> lore = new ArrayList<>();
      if(type == SabotageType.LIGHTS) {
        lore.add("<#F4F1DE>ตัดกระแสไฟฟ้าทั้งแผนที่");
        lore.add("<#B8B0C8>ผู้โดยสารจะมองเห็นได้ระยะใกล้");
      } else if(type == SabotageType.ENGINE_MELTDOWN) {
        lore.add("<#F4F1DE>สั่งให้เครื่องยนต์ระเบิดใน <#FFB703>40 <#F4F1DE>วิ");
        lore.add("<#B8B0C8>หากไม่มีคนไปซ่อม ฆาตกรชนะทันที");
      } else if(type == SabotageType.COMMS) {
        lore.add("<#F4F1DE>รบกวนคลื่นวิทยุและเรดาร์");
        lore.add("<#B8B0C8>ซ่อนจุดภารกิจและแผนที่ย่อ");
      }
      lore.add(" ");
      if(!hasLocation) {
        lore.add("<#FF3B47>ยังไม่ได้ตั้งจุดในแผนที่");
        lore.add("<shadow:#000000FF><#5A4E7C>(</shadow><#B8B0C8>/mma setsabotage " + arena.getId() + " " + type.getShortId() + "<shadow:#000000FF><#5A4E7C>)</shadow>");
      } else if(isActiveThis) {
        lore.add("<#FFB703>กำลังทำงานอยู่");
      } else if(hasActive) {
        lore.add("<#FF3B47>เกิดวินาศกรรมอื่นอยู่");
      } else if(onCooldown) {
        lore.add("<#FF3B47>คูลดาวน์ " + cooldownRemaining + " วิ");
      } else {
        lore.add("<shadow:#000000FF><#5A4E7C>(</shadow><#FFB703>กดคลิกเพื่อก่อวินาศกรรม<shadow:#000000FF><#5A4E7C>)</shadow>");
      }
      HalloweenChat.setItemLore(meta, lore);
      TaskGuiHelper.setGlint(meta, hasLocation && !isActiveThis && !hasActive && !onCooldown);
      item.setItemMeta(meta);
    }
    return item;
  }

  public void handleSabotageClick(Player player, int rawSlot) {
    IUser user = arena.getPlugin().getUserManager().getUser(player);
    if(arena.getArenaState() != IArenaState.IN_GAME
      || arena.getVotingManager().isVotingActive()
      || arena.getCutsceneManager().isPlaying()
      || ArenaUtils.isSpectator(player, arena)
      || !Role.isRole(Role.MURDERER, user, arena)) {
      player.closeInventory();
      return;
    }

    if(cooldownRemaining > 0) {
      XSound.ENTITY_VILLAGER_NO.play(player.getLocation(), 0.8F, 1F);
      HalloweenChat.sendActionBar(player, "<shadow:#000000FF><#5A4E7C>[</shadow><#FF3B47>Sabotage<shadow:#000000FF><#5A4E7C>]</shadow> <#FF3B47>คูลดาวน์ <#FFB703>" + cooldownRemaining + " <#FF3B47>วิ");
      return;
    }
    if(activeSabotage != null) {
      XSound.ENTITY_VILLAGER_NO.play(player.getLocation(), 0.8F, 1F);
      HalloweenChat.sendActionBar(player, "<shadow:#000000FF><#5A4E7C>[</shadow><#FF3B47>Sabotage<shadow:#000000FF><#5A4E7C>]</shadow> <#FF3B47>กำลังเกิดวินาศกรรมอยู่");
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
        HalloweenChat.sendActionBar(player, "<shadow:#000000FF><#5A4E7C>[</shadow><#FF3B47>Sabotage<shadow:#000000FF><#5A4E7C>]</shadow> <#FF3B47>ยังไม่ได้ตั้งจุดในแผนที่");
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
    arena.getTaskManager().updateAllPlayersStationVisibility();

    if(type == SabotageType.LIGHTS) {
      for(int i = 0; i < 5; i++) {
        breakerSwitches[i] = (Math.random() < 0.3);
      }
      breakerSwitches[0] = false;
      breakerSwitches[2] = false;
      breakerSwitches[4] = false;
    }

    if(type == SabotageType.LIGHTS) {
      String title = "<shadow:#000000FF><#5A4E7C>[</shadow><#FF3B47>Sabotage<shadow:#000000FF><#5A4E7C>]</shadow>";
      String sub = "<#F4F1DE>ไฟฟ้าดับ <shadow:#000000FF><#5A4E7C>(</shadow><#B8B0C8>รีบไปสับคัตเอาท์เพื่อกู้ระบบ<shadow:#000000FF><#5A4E7C>)</shadow>";
      for(Player p : arena.getPlayersLeft()) {
        XSound.ENTITY_LIGHTNING_BOLT_THUNDER.play(p.getLocation(), 0.7F, 0.6F);
        XSound.BLOCK_REDSTONE_TORCH_BURNOUT.play(p.getLocation(), 1F, 0.8F);
        HalloweenChat.sendTitles(p, title, sub, 10, 60, 10);
      }
    } else if(type == SabotageType.ENGINE_MELTDOWN) {
      hideMeltdownBossBarAll();
      meltdownBossBar = BossBar.bossBar(
        HalloweenChat.component("<shadow:#000000FF><#5A4E7C>[</shadow><#FF3B47>Sabotage<shadow:#000000FF><#5A4E7C>]</shadow> <#F4F1DE>เครื่องยนต์จะระเบิดใน <#FFB703>40 <#F4F1DE>วิ <shadow:#000000FF><#5A4E7C>(</shadow><#B8B0C8>รีบไประบายความร้อน<shadow:#000000FF><#5A4E7C>)</shadow>"),
        1.0f,
        BossBar.Color.RED,
        BossBar.Overlay.PROGRESS
      );
      for(Player p : arena.getPlayers()) {
        addPlayerToBossBar(p);
      }

      String title = "<shadow:#000000FF><#5A4E7C>[</shadow><#FF3B47>Sabotage<shadow:#000000FF><#5A4E7C>]</shadow>";
      String sub = "<#FF3B47>เครื่องยนต์จะระเบิดใน <#FFB703>40 <#FF3B47>วิ <shadow:#000000FF><#5A4E7C>(</shadow><#B8B0C8>รีบไประบายความร้อน<shadow:#000000FF><#5A4E7C>)</shadow>";
      for(Player p : arena.getPlayersLeft()) {
        XSound.BLOCK_BEACON_DEACTIVATE.play(p.getLocation(), 1F, 0.8F);
        HalloweenChat.sendTitles(p, title, sub, 10, 60, 10);
      }
    } else if(type == SabotageType.COMMS) {
      arena.getTaskManager().updateAllPlayersStationVisibility();

      String title = "<shadow:#000000FF><#5A4E7C>[</shadow><#FF3B47>Sabotage<shadow:#000000FF><#5A4E7C>]</shadow>";
      String sub = "<#F4F1DE>เรดาร์ถูกรบกวน <shadow:#000000FF><#5A4E7C>(</shadow><#B8B0C8>รีบไปปรับคลื่นวิทยุ<shadow:#000000FF><#5A4E7C>)</shadow>";
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

    Location textLoc = arena.resolveClearHologramLocation(loc, 1.4, collectOtherStationHolograms(loc));
    String text;
    if(type == SabotageType.LIGHTS) {
      text = "<shadow:#000000FF><#5A4E7C>[</shadow><#FF3B47>Sabotage<shadow:#000000FF><#5A4E7C>]</shadow>\n\n<#F4F1DE>กู้ระบบไฟฟ้า\n\n<shadow:#000000FF><#5A4E7C>(</shadow><#B8B0C8>กดคลิกขวาเพื่อสับคัตเอาท์<shadow:#000000FF><#5A4E7C>)</shadow>";
    } else if(type == SabotageType.ENGINE_MELTDOWN) {
      text = "<shadow:#000000FF><#5A4E7C>[</shadow><#FF3B47>Sabotage<shadow:#000000FF><#5A4E7C>]</shadow>\n\n<#F4F1DE>ระบายความร้อนฉุกเฉิน\n<#FF3B47>เหลือเวลา <#FFB703>40 <#FF3B47>วิ\n\n<shadow:#000000FF><#5A4E7C>(</shadow><#B8B0C8>กดคลิกขวาเพื่อระบายความร้อน<shadow:#000000FF><#5A4E7C>)</shadow>";
    } else {
      text = "<shadow:#000000FF><#5A4E7C>[</shadow><#FF3B47>Sabotage<shadow:#000000FF><#5A4E7C>]</shadow>\n\n<#F4F1DE>กู้สัญญาณเรดาร์\n\n<shadow:#000000FF><#5A4E7C>(</shadow><#B8B0C8>กดคลิกขวาเพื่อปรับคลื่นวิทยุ<shadow:#000000FF><#5A4E7C>)</shadow>";
    }

    fixTextDisplay = arena.spawnTextDisplay(textLoc, text, false);
    arena.applyCompactHologramScaleIfNeeded(fixTextDisplay, textLoc);
    fixBlockDisplay = arena.spawnBlockDisplay(loc, null, ChatColor.RED);

    for(Player p : arena.getPlayers()) {
      showEmergencyVisualsToPlayer(p);
    }
  }

  private void removeEmergencyVisuals() {
    if(fixTextDisplay != null && fixTextDisplay.isValid()) {
      fixTextDisplay.remove();
      fixTextDisplay = null;
    }
    if(fixBlockDisplay != null) {
      arena.removeEntityGlowColor(fixBlockDisplay);
      if(fixBlockDisplay.isValid()) {
        fixBlockDisplay.remove();
      }
      fixBlockDisplay = null;
    }
  }

  public boolean isAtFixStation(Player player, Location clickedLoc) {
    if(activeSabotage == null || activeFixLocation == null || activeFixLocation.getWorld() == null) {
      return false;
    }
    if(player == null || !player.getWorld().equals(activeFixLocation.getWorld())) {
      return false;
    }
    if(player.getLocation().distanceSquared(activeFixLocation.clone().add(0.5, 0.5, 0.5)) > 36.0) {
      return false;
    }
    if(clickedLoc != null && clickedLoc.getWorld() != null && clickedLoc.getWorld().equals(activeFixLocation.getWorld())) {
      if(isSameBlock(clickedLoc, activeFixLocation)) {
        return true;
      }
      if(isSameBlock(clickedLoc, arena.getCampfireLocation())
        || arena.isVentBlock(clickedLoc)
        || (arena.getTaskManager() != null && arena.getTaskManager().isTaskStationBlock(clickedLoc))) {
        return false;
      }
      if(clickedLoc.getBlockX() == activeFixLocation.getBlockX()
        && clickedLoc.getBlockZ() == activeFixLocation.getBlockZ()
        && clickedLoc.getBlockY() - activeFixLocation.getBlockY() == 1) {
        return true;
      }
      return clickedLoc.distanceSquared(activeFixLocation) <= 1.25;
    }
    try {
      Block target = player.getTargetBlockExact(5);
      if(target != null) {
        Location tLoc = target.getLocation();
        if(isSameBlock(tLoc, activeFixLocation)) {
          return true;
        }
        if(isSameBlock(tLoc, arena.getCampfireLocation())
          || arena.isVentBlock(tLoc)
          || (arena.getTaskManager() != null && arena.getTaskManager().isTaskStationBlock(tLoc))) {
          return false;
        }
        if(tLoc.distanceSquared(activeFixLocation) <= 1.5) {
          return true;
        }
      }
    } catch(Throwable ignored) {
    }
    return false;
  }

  public void openFixGUI(Player player) {
    if(activeSabotage == null
      || arena.getArenaState() != IArenaState.IN_GAME
      || arena.getVotingManager().isVotingActive()
      || arena.getCutsceneManager().isPlaying()
      || ArenaUtils.isSpectator(player, arena)
      || (arena.getVentManager() != null && arena.getVentManager().isInsideVent(player))) {
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
    Inventory inv = Bukkit.createInventory(null, 36, FIX_LIGHTS_TITLE);
    buildFixLightsGUI(inv);
    player.openInventory(inv);
    XSound.BLOCK_CHEST_OPEN.play(player.getLocation(), 0.8F, 1.2F);
    HalloweenChat.sendActionBar(player, "<shadow:#000000FF><#5A4E7C>[</shadow><#FF7518>Sabotage<shadow:#000000FF><#5A4E7C>]</shadow> <#F4F1DE>คลิก <#FFB703>คัตเอาท์ที่เรืองแสง <#F4F1DE>เพื่อเปิดไฟให้ครบทั้ง 5 ตัว");
  }

  private void buildFixLightsGUI(Inventory inv) {
    TaskGuiHelper.fillBackground(inv);

    int onCount = 0;
    for(boolean b : breakerSwitches) {
      if(b) {
        onCount++;
      }
    }

    inv.setItem(4, TaskGuiHelper.createItem(
      XMaterial.REDSTONE_LAMP,
      Math.max(1, 5 - onCount),
      false,
      TaskGuiHelper.tag("<#FF7518>", "กู้ระบบไฟฟ้า - เปิดแล้ว " + onCount + "/5 ตัว"),
      "",
      "<#F4F1DE>คลิก <#FFB703>คันโยกคัตเอาท์ที่เรืองแสง <#F4F1DE>ให้เปิดเป็นสีเขียวทั้งหมด",
      "<#F4F1DE>ความคืบหน้า " + TaskGuiHelper.buildPipeBar(onCount, 5, 10)
    ));

    int[] lightSlots = {11, 12, 13, 14, 15};
    int[] switchSlots = {20, 21, 22, 23, 24};
    for(int i = 0; i < 5; i++) {
      int num = i + 1;
      boolean isOn = breakerSwitches[i];

      inv.setItem(lightSlots[i], TaskGuiHelper.createItem(
        isOn ? XMaterial.LIME_STAINED_GLASS_PANE : XMaterial.RED_STAINED_GLASS_PANE,
        1,
        false,
        TaskGuiHelper.tag(isOn ? "<#6EEB83>" : "<#FF3B47>", "ไฟสถานะ #" + num + (isOn ? " - ปกติ" : " - ไฟดับ")),
        "",
        isOn ? "<#6EEB83>จ่ายกระแสไฟแล้ว" : TaskGuiHelper.hint("<#FFB703>", "คลิกคัตเอาท์ด้านล่างเพื่อเปิดไฟ")
      ));

      inv.setItem(switchSlots[i], TaskGuiHelper.createItem(
        isOn ? XMaterial.TRIPWIRE_HOOK : XMaterial.LEVER,
        num,
        !isOn,
        isOn ? TaskGuiHelper.doneName("คัตเอาท์ #" + num + " เปิดแล้ว") : TaskGuiHelper.clickName("สับคัตเอาท์ #" + num + " ขึ้น"),
        "",
        isOn ? "<#6EEB83>สวิตช์ทำงานเรียบร้อย" : TaskGuiHelper.hint("<#FFB703>", "กดคลิกเพื่อต่อกระแสไฟ")
      ));
    }
  }

  private void openFixEngineGUI(Player player) {
    Inventory inv = Bukkit.createInventory(null, 27, FIX_ENGINE_TITLE);
    TaskGuiHelper.fillBackground(inv);

    inv.setItem(4, TaskGuiHelper.createItem(
      XMaterial.BLAZE_POWDER,
      1,
      false,
      TaskGuiHelper.tag("<#FF3B47>", "เตือนภัยเครื่องยนต์ร้อนจัด - เหลือ " + countdownSeconds + " วิ"),
      "",
      "<#F4F1DE>คลิกปุ่ม <#72EFDD>วาล์วน้ำแข็งเรืองแสงตรงกลาง <#F4F1DE>เพื่อหยุดการระเบิดทันที"
    ));

    ItemStack warnGlass = TaskGuiHelper.createItem(
      XMaterial.RED_STAINED_GLASS_PANE,
      1,
      false,
      TaskGuiHelper.tag("<#FF3B47>", "คลิกปุ่มตรงกลางเพื่อระบายความร้อน")
    );
    inv.setItem(12, warnGlass);
    inv.setItem(14, warnGlass);

    inv.setItem(13, TaskGuiHelper.createItem(
      XMaterial.PACKED_ICE,
      1,
      true,
      TaskGuiHelper.clickName("เปิดวาล์วระบายความร้อนฉุกเฉิน"),
      "",
      "<#F4F1DE>ฉีดน้ำยาหล่อเย็นแรงดันสูงดับความร้อนเครื่องยนต์",
      "",
      TaskGuiHelper.hint("<#6EEB83>", "กดคลิกที่นี่เพื่อหยุดการระเบิดทันที")
    ));

    player.openInventory(inv);
    XSound.BLOCK_CHEST_OPEN.play(player.getLocation(), 0.8F, 1.2F);
    HalloweenChat.sendActionBar(player, "<shadow:#000000FF><#5A4E7C>[</shadow><#FF3B47>Sabotage<shadow:#000000FF><#5A4E7C>]</shadow> <#F4F1DE>คลิก <#72EFDD>ปุ่มน้ำแข็งเรืองแสงตรงกลาง <#F4F1DE>เพื่อระบายความร้อน");
  }

  private void openFixCommsGUI(Player player) {
    Inventory inv = Bukkit.createInventory(null, 27, FIX_COMMS_TITLE);
    TaskGuiHelper.fillBackground(inv);

    inv.setItem(4, TaskGuiHelper.createItem(
      XMaterial.LIGHTNING_ROD,
      1,
      false,
      TaskGuiHelper.tag("<#FF7518>", "สัญญาณเรดาร์และวิทยุถูกรบกวน"),
      "",
      "<#F4F1DE>คลิกปุ่ม <#FFB703>เข็มทิศวิทยุเรืองแสงตรงกลาง <#F4F1DE>เพื่อกู้สัญญาณ"
    ));

    ItemStack warnGlass = TaskGuiHelper.createItem(
      XMaterial.ORANGE_STAINED_GLASS_PANE,
      1,
      false,
      TaskGuiHelper.tag("<#FFB703>", "คลิกปุ่มตรงกลางเพื่อจูนคลื่นความถี่")
    );
    inv.setItem(12, warnGlass);
    inv.setItem(14, warnGlass);

    inv.setItem(13, TaskGuiHelper.createItem(
      XMaterial.COMPASS,
      1,
      true,
      TaskGuiHelper.clickName("ปรับคลื่นความถี่วิทยุกู้เรดาร์"),
      "",
      "<#F4F1DE>รีเซ็ตคลื่นความถี่เพื่อเปิดระบบนำทางภารกิจอีกครั้ง",
      "",
      TaskGuiHelper.hint("<#6EEB83>", "กดคลิกที่นี่เพื่อกู้สัญญาณทันที")
    ));

    player.openInventory(inv);
    XSound.BLOCK_CHEST_OPEN.play(player.getLocation(), 0.8F, 1.2F);
    HalloweenChat.sendActionBar(player, "<shadow:#000000FF><#5A4E7C>[</shadow><#FF7518>Sabotage<shadow:#000000FF><#5A4E7C>]</shadow> <#F4F1DE>คลิก <#FFB703>เข็มทิศเรืองแสงตรงกลาง <#F4F1DE>เพื่อกู้สัญญาณวิทยุ");
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
    if(activeSabotage == null
      || arena.getArenaState() != IArenaState.IN_GAME
      || arena.getVotingManager().isVotingActive()
      || arena.getCutsceneManager().isPlaying()
      || ArenaUtils.isSpectator(player, arena)
      || (arena.getVentManager() != null && arena.getVentManager().isInsideVent(player))
      || activeFixLocation == null
      || activeFixLocation.getWorld() == null
      || !player.getWorld().equals(activeFixLocation.getWorld())
      || player.getLocation().distanceSquared(activeFixLocation.clone().add(0.5, 0.5, 0.5)) > 49.0) {
      player.closeInventory();
      return;
    }

    if(activeSabotage == SabotageType.LIGHTS) {
      int[] lightSlots = {11, 12, 13, 14, 15};
      int[] switchSlots = {20, 21, 22, 23, 24};
      for(int i = 0; i < 5; i++) {
        if(switchSlots[i] == rawSlot || lightSlots[i] == rawSlot) {
          if(breakerSwitches[i]) {
            XSound.UI_BUTTON_CLICK.play(player.getLocation(), 0.5F, 1.2F);
            return;
          }
          breakerSwitches[i] = true;
          XSound.BLOCK_LEVER_CLICK.play(player.getLocation(), 1F, 1.4F);

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
            for(Player p : arena.getPlayersLeft()) {
              if(p.isOnline() && isFixGUI(p.getOpenInventory().getTitle())) {
                buildFixLightsGUI(p.getOpenInventory().getTopInventory());
              }
            }
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
    activeFixLocation = null;
    cooldownRemaining = COOLDOWN_DURATION;

    for(Player p : arena.getPlayers()) {
      if(p != null && p.isOnline() && !p.equals(fixer) && isFixGUI(p.getOpenInventory().getTitle())) {
        p.closeInventory();
      }
    }

    removeEmergencyVisuals();

    hideMeltdownBossBarAll();
    meltdownBossBar = null;

    for(Player p : arena.getPlayersLeft()) {
      if(arena.getVentManager() == null || !arena.getVentManager().isInsideVent(p)) {
        p.removePotionEffect(PotionEffectType.DARKNESS);
      }
      p.removePotionEffect(PotionEffectType.NIGHT_VISION);
    }

    arena.getTaskManager().updateAllPlayersStationVisibility();

    String title = "<shadow:#000000FF><#5A4E7C>[</shadow><#6EEB83>Sabotage<shadow:#000000FF><#5A4E7C>]</shadow>";
    String sub = "<#FFB703>" + fixer.getName() + " <#6EEB83>กู้ระบบสำเร็จแล้ว";
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
    activeFixLocation = null;
    cooldownRemaining = COOLDOWN_DURATION;

    removeEmergencyVisuals();

    hideMeltdownBossBarAll();
    meltdownBossBar = null;

    for(Player p : arena.getPlayersLeft()) {
      if(arena.getVentManager() == null || !arena.getVentManager().isInsideVent(p)) {
        p.removePotionEffect(PotionEffectType.DARKNESS);
      }
      p.removePotionEffect(PotionEffectType.NIGHT_VISION);
    }

    arena.getTaskManager().updateAllPlayersStationVisibility();
    arena.updateCampfireHologram();
  }

  public void handleClose(Player player) {
  }

  public void addPlayerToBossBar(Player player) {
    if(activeSabotage != null && player != null) {
      showEmergencyVisualsToPlayer(player);
    }
    if(meltdownBossBar != null && player != null) {
      try {
        player.showBossBar(meltdownBossBar);
        meltdownBarViewers.add(player);
      } catch(Throwable ignored) {
      }
    }
  }

  public void removePlayerFromBossBar(Player player) {
    if(meltdownBossBar != null && player != null) {
      try {
        player.hideBossBar(meltdownBossBar);
      } catch(Throwable ignored) {
      }
      meltdownBarViewers.remove(player);
    }
  }

  public void cleanup() {
    if(tickTask != null) {
      tickTask.cancel();
      tickTask = null;
    }
    hideMeltdownBossBarAll();
    meltdownBossBar = null;
    for(Player p : arena.getPlayers()) {
      p.removePotionEffect(PotionEffectType.DARKNESS);
      p.removePotionEffect(PotionEffectType.NIGHT_VISION);
    }
    removeEmergencyVisuals();
    activeSabotage = null;
    cooldownRemaining = COOLDOWN_DURATION;
    activeFixLocation = null;
  }
}
