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

package plugily.projects.murdermystery.arena.voting;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import plugily.projects.minigamesbox.api.user.IUser;
import plugily.projects.minigamesbox.classic.handlers.language.MessageBuilder;
import plugily.projects.minigamesbox.classic.utils.hologram.ArmorStandHologram;
import plugily.projects.minigamesbox.classic.utils.version.VersionUtils;
import plugily.projects.minigamesbox.classic.utils.version.xseries.XMaterial;
import plugily.projects.minigamesbox.classic.utils.version.xseries.XSound;
import plugily.projects.minigamesbox.classic.utils.version.xseries.inventory.XInventoryView;
import plugily.projects.murdermystery.arena.Arena;
import plugily.projects.murdermystery.arena.ArenaUtils;
import plugily.projects.murdermystery.arena.role.Role;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class VotingManager {

  public static class DeadBody {
    private final String victimName;
    private final Location location;
    private final List<ArmorStand> stands;
    private final TextDisplay textDisplay;
    private final ArmorStandHologram hologram;

    public DeadBody(String victimName, Location location, List<ArmorStand> stands, TextDisplay textDisplay, ArmorStandHologram hologram) {
      this.victimName = victimName;
      this.location = location;
      this.stands = stands;
      this.textDisplay = textDisplay;
      this.hologram = hologram;
    }

    public String getVictimName() {
      return victimName;
    }

    public Location getLocation() {
      return location;
    }

    public List<ArmorStand> getStands() {
      return stands;
    }

    public void remove(Arena arena) {
      if(hologram != null) {
        try {
          hologram.delete();
        } catch(Throwable ignored) {
        }
      }
      if(textDisplay != null && textDisplay.isValid()) {
        textDisplay.remove();
      }
      if(stands != null) {
        for(ArmorStand stand : stands) {
          if(stand != null) {
            arena.getPlugin().getHologramManager().getArmorStands().remove(stand);
            if(stand.isValid()) {
              stand.remove();
            }
          }
        }
      }
    }
  }

  public static final String VOTE_GUI_TITLE = "§8[ §c โหวตหาคนร้ายรอบกองไฟ §8]";
  public static final int VOTE_MENU_ITEM_SLOT = 4;

  private static final int INFO_SLOT = 4;
  private static final int SKIP_SLOT = 49;
  private static final int CHAT_CLOSE_SLOT = 53;
  private static final int[] PLAYER_SLOTS = {
    10, 11, 12, 13, 14, 15, 16,
    19, 20, 21, 22, 23, 24, 25,
    28, 29, 30, 31, 32, 33, 34
  };

  private final Arena arena;
  private VoteState voteState = VoteState.NONE;
  private int voteTimer = 0;
  private Player reporter;
  private String reportedVictimName;
  private final Map<Player, Player> votes = new HashMap<>();
  private final Set<Player> skipVotes = new HashSet<>();
  private final Map<Player, Location> preVoteLocations = new HashMap<>();
  private final Map<UUID, Location> meetingCircleLocations = new HashMap<>();
  private final Map<UUID, ItemStack> savedSlotItems = new HashMap<>();
  private final List<DeadBody> activeBodies = new ArrayList<>();
  private long lastVoteEndTime = 0;

  public VotingManager(Arena arena) {
    this.arena = arena;
  }

  public void registerDeadBody(String victimName, Location location, List<ArmorStand> stands, TextDisplay textDisplay, ArmorStandHologram hologram) {
    activeBodies.add(new DeadBody(victimName, location, stands, textDisplay, hologram));
  }

  public List<DeadBody> getActiveBodies() {
    return activeBodies;
  }

  public DeadBody findNearbyBody(Location loc, double maxDistance) {
    if(loc == null || loc.getWorld() == null) {
      return null;
    }
    double maxDistSq = maxDistance * maxDistance;
    DeadBody closest = null;
    double bestDistSq = maxDistSq;
    for(DeadBody body : activeBodies) {
      Location bLoc = body.getLocation();
      if(bLoc != null && bLoc.getWorld() != null && bLoc.getWorld().equals(loc.getWorld())) {
        double dSq = bLoc.distanceSquared(loc);
        if(dSq <= bestDistSq) {
          bestDistSq = dSq;
          closest = body;
        }
      }
    }
    return closest;
  }

  public boolean tryReportNearbyBody(Player player, Location interactLoc) {
    if(isVotingActive()) {
      return false;
    }
    DeadBody body = findNearbyBody(player.getLocation(), 3.8);
    if(body == null && interactLoc != null) {
      body = findNearbyBody(interactLoc, 3.0);
    }
    if(body != null) {
      return startVoting(player, body.getVictimName());
    }
    return false;
  }

  public void clearDeadBodies() {
    for(DeadBody body : activeBodies) {
      body.remove(arena);
    }
    activeBodies.clear();
  }

  public ItemStack createVoteMenuItem() {
    ItemStack item = XMaterial.NETHER_STAR.parseItem();
    ItemMeta meta = item.getItemMeta();
    if(meta != null) {
      meta.setDisplayName("§e§l[ เปิดเมนูโหวตหาคนร้าย (Voting GUI) ]");
      meta.setLore(Arrays.asList(
        "§7คลิกขวาเพื่อเปิดหน้าต่างโหวตคัดคนร้ายออก",
        "§7สามารถปิดหน้าต่างเพื่อคุยแชทแล้วกดเปิดใหม่ได้ตลอด"
      ));
      item.setItemMeta(meta);
    }
    return item;
  }

  public boolean isVoteMenuItem(ItemStack item) {
    if(item == null || !item.hasItemMeta() || !item.getItemMeta().hasDisplayName()) {
      return false;
    }
    return item.getItemMeta().getDisplayName().contains("เปิดเมนูโหวตหาคนร้าย");
  }

  public boolean startVoting(Player reportingPlayer) {
    return startVoting(reportingPlayer, null);
  }

  public boolean startVoting(Player reportingPlayer, String victimName) {
    if(voteState != VoteState.NONE) {
      return false;
    }

    if(victimName == null) {
      long cooldownMs = arena.getPlugin().getConfig().getInt("Voting.Cooldown", 30) * 1000L;
      if(System.currentTimeMillis() - lastVoteEndTime < cooldownMs) {
        new MessageBuilder("IN_GAME_MESSAGES_ARENA_VOTING_COOLDOWN").asKey()
          .player(reportingPlayer).arena(arena).sendPlayer();
        return false;
      }
    }

    this.reporter = reportingPlayer;
    this.reportedVictimName = victimName;
    this.votes.clear();
    this.skipVotes.clear();
    this.preVoteLocations.clear();
    this.meetingCircleLocations.clear();
    this.savedSlotItems.clear();

    if(arena.getSabotageManager() != null) {
      arena.getSabotageManager().cancelActiveSabotage("VOTING");
    }

    clearDeadBodies();

    XSound.ENTITY_LIGHTNING_BOLT_THUNDER.play(reportingPlayer.getLocation(), 1F, 1F);
    XSound.EVENT_RAID_HORN.play(reportingPlayer.getLocation(), 1.5F, 1.0F);
    new MessageBuilder("IN_GAME_MESSAGES_ARENA_VOTING_BODY_REPORTED")
      .asKey().player(reportingPlayer).arena(arena).sendArena();

    Location center = arena.getCampfireLocation();
    if(center == null && !arena.getPlayerSpawnPoints().isEmpty()) {
      center = arena.getPlayerSpawnPoints().get(0);
    }

    List<Player> alivePlayers = arena.getPlayersLeft();
    int count = Math.max(1, alivePlayers.size());
    double radius = 2.8;
    double cx = center.getBlockX() + 0.5;
    double cz = center.getBlockZ() + 0.5;
    double cy = center.getBlockY();

    for(int i = 0; i < alivePlayers.size(); i++) {
      Player player = alivePlayers.get(i);
      if(arena.getVentManager().isInsideVent(player)) {
        arena.getVentManager().exitVent(player);
      }
      preVoteLocations.put(player, player.getLocation().clone());

      double angle = (2.0 * Math.PI * i) / count;
      double px = cx + radius * Math.cos(angle);
      double pz = cz + radius * Math.sin(angle);
      double py = findSafeStandingY(center, px, cy, pz);

      double dx = cx - px;
      double dz = cz - pz;
      float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));

      Location circleSpot = new Location(center.getWorld(), px, py, pz, yaw, 10.0f);
      meetingCircleLocations.put(player.getUniqueId(), circleSpot);

      player.closeInventory();
      VersionUtils.teleport(player, circleSpot);
      player.setWalkSpeed(0.0f);
      player.setFoodLevel(20);

      savedSlotItems.put(player.getUniqueId(), player.getInventory().getItem(VOTE_MENU_ITEM_SLOT));
      player.getInventory().setItem(VOTE_MENU_ITEM_SLOT, createVoteMenuItem());
      player.getInventory().setHeldItemSlot(VOTE_MENU_ITEM_SLOT);

      if(victimName != null) {
        VersionUtils.sendTitles(player,
          "§c§l รายงานศพผู้เสียชีวิต! ",
          "§e" + reportingPlayer.getName() + " §fพบศพของ §c" + victimName,
          10, 60, 10);
      } else {
        VersionUtils.sendTitles(player,
          "§6§l เรียกประชุมฉุกเฉินรอบกองไฟ! ",
          "§e" + reportingPlayer.getName() + " §fเรียกทุกคนมารวมตัวกันที่กองไฟ",
          10, 60, 10);
      }
    }

    for(Player player : arena.getPlayers()) {
      IUser user = arena.getPlugin().getUserManager().getUser(player);
      if(user.isSpectator() && center != null) {
        VersionUtils.teleport(player, center.clone().add(0.5, 2.5, 0.5));
      }
    }

    voteState = VoteState.DISCUSSION;
    voteTimer = arena.getPlugin().getConfig().getInt("Voting.Discussion-Time", 15);

    new MessageBuilder("IN_GAME_MESSAGES_ARENA_VOTING_DISCUSSION_START")
      .asKey().player(reportingPlayer).arena(arena)
      .integer(voteTimer)
      .sendArena();

    return true;
  }

  private double findSafeStandingY(Location center, double x, double baseY, double z) {
    if(center == null || center.getWorld() == null) {
      return baseY;
    }
    int bx = (int) Math.floor(x);
    int bz = (int) Math.floor(z);
    int by = (int) Math.floor(baseY);

    for(int offset = 0; offset <= 2; offset++) {
      Block feet = center.getWorld().getBlockAt(bx, by + offset, bz);
      Block head = center.getWorld().getBlockAt(bx, by + offset + 1, bz);
      Block ground = center.getWorld().getBlockAt(bx, by + offset - 1, bz);
      if(feet.isPassable() && head.isPassable() && !ground.isPassable()) {
        return by + offset;
      }
      if(offset > 0) {
        Block feetDown = center.getWorld().getBlockAt(bx, by - offset, bz);
        Block headDown = center.getWorld().getBlockAt(bx, by - offset + 1, bz);
        Block groundDown = center.getWorld().getBlockAt(bx, by - offset - 1, bz);
        if(feetDown.isPassable() && headDown.isPassable() && !groundDown.isPassable()) {
          return by - offset;
        }
      }
    }
    return baseY;
  }

  public Location getMeetingSpot(Player player) {
    return meetingCircleLocations.get(player.getUniqueId());
  }

  public boolean tick() {
    if(voteState == VoteState.NONE) {
      return false;
    }

    voteTimer--;

    switch(voteState) {
      case DISCUSSION:
        if(voteTimer <= 0) {
          voteState = VoteState.VOTING;
          voteTimer = arena.getPlugin().getConfig().getInt("Voting.Voting-Time", 30);
          openVotingGUIForAll();

          new MessageBuilder("IN_GAME_MESSAGES_ARENA_VOTING_VOTE_NOW")
            .asKey().arena(arena).integer(voteTimer).sendArena();
        } else {
          refreshOpenVotingGUIs();
          for(Player player : arena.getPlayersLeft()) {
            VersionUtils.sendActionBar(player,
              "§6 เวลาปรึกษารอบกองไฟ: §e" + voteTimer + " วินาที §7(คลิกขวาไอเทมในมือเพื่อดูรายชื่อ)");
          }
        }
        break;

      case VOTING:
        if(voteTimer <= 0 || hasEveryoneVoted()) {
          resolveVotes();
        } else {
          refreshOpenVotingGUIs();
          int totalVoted = votes.size() + skipVotes.size();
          int totalAlive = arena.getPlayersLeft().size();
          for(Player player : arena.getPlayersLeft()) {
            VersionUtils.sendActionBar(player,
              "§c เวลาโหวต: §e" + voteTimer + " วินาที §8| §aโหวตแล้ว " + totalVoted + "/" + totalAlive + " คน §7(คลิกขวาไอเทมในมือเพื่อเปิดเมนูโหวต)");
          }
        }
        break;

      case RESULT:
        if(voteTimer <= 0) {
          endVoting();
        }
        break;
      default:
        break;
    }

    return true;
  }

  public void openVotingGUIForAll() {
    for(Player viewer : arena.getPlayersLeft()) {
      openVotingGUI(viewer);
    }
  }

  public void openVotingGUI(Player viewer) {
    if(voteState != VoteState.DISCUSSION && voteState != VoteState.VOTING) {
      return;
    }
    Inventory gui = Bukkit.createInventory(null, 54, VOTE_GUI_TITLE);
    populateVotingGUI(gui, viewer);
    viewer.openInventory(gui);
    XSound.UI_BUTTON_CLICK.play(viewer.getLocation(), 0.7F, 1.2F);
  }

  public void refreshOpenVotingGUIs() {
    for(Player viewer : arena.getPlayersLeft()) {
      try {
        String openTitle = XInventoryView.of(viewer.getOpenInventory()).getTitle();
        if(isVotingGUI(openTitle)) {
          populateVotingGUI(viewer.getOpenInventory().getTopInventory(), viewer);
        }
      } catch(Throwable ignored) {
      }
    }
  }

  private void populateVotingGUI(Inventory gui, Player viewer) {
    ItemStack darkBg = XMaterial.GRAY_STAINED_GLASS_PANE.parseItem();
    ItemMeta dMeta = darkBg.getItemMeta();
    if(dMeta != null) {
      dMeta.setDisplayName("§7 ");
      darkBg.setItemMeta(dMeta);
    }

    ItemStack fireBorder = XMaterial.ORANGE_STAINED_GLASS_PANE.parseItem();
    ItemMeta fMeta = fireBorder.getItemMeta();
    if(fMeta != null) {
      fMeta.setDisplayName("§6");
      fireBorder.setItemMeta(fMeta);
    }

    for(int i = 0; i < 54; i++) {
      if(i < 9 || i >= 45) {
        gui.setItem(i, fireBorder);
      } else {
        gui.setItem(i, darkBg);
      }
    }

    List<Player> alivePlayers = arena.getPlayersLeft();
    int totalVoted = votes.size() + skipVotes.size();

    ItemStack campfireInfo = XMaterial.CAMPFIRE.parseItem();
    ItemMeta cMeta = campfireInfo.getItemMeta();
    if(cMeta != null) {
      cMeta.setDisplayName("§6§l การประชุมรอบกองไฟ (Campfire Meeting) ");
      List<String> infoLore = new ArrayList<>();
      if(reportedVictimName != null) {
        infoLore.add("§c รายงานศพของ: §f" + reportedVictimName);
      } else {
        infoLore.add("§e ประเภท: §fเรียกประชุมฉุกเฉินที่กองไฟ");
      }
      if(reporter != null) {
        infoLore.add("§e ผู้เรียกประชุม: §f" + reporter.getName());
      }
      infoLore.add(" ");
      if(voteState == VoteState.DISCUSSION) {
        infoLore.add("§fสถานะ: §e กำลังปรึกษาหารือ (รอเปิดโหวต)");
        infoLore.add("§fจะเปิดให้โหวตในอีก: §6" + voteTimer + " วินาที");
      } else {
        infoLore.add("§fสถานะ: §a เปิดให้ลงคะแนนโหวตแล้ว!");
        infoLore.add("§fเวลาโหวตที่เหลือ: §c" + voteTimer + " วินาที");
      }
      infoLore.add("§fลงคะแนนแล้ว: §a" + totalVoted + " §7/ §f" + alivePlayers.size() + " คน");
      cMeta.setLore(infoLore);
      campfireInfo.setItemMeta(cMeta);
    }
    gui.setItem(INFO_SLOT, campfireInfo);

    IUser viewerUser = arena.getPlugin().getUserManager().getUser(viewer);
    boolean viewerIsMurderer = Role.isRole(Role.MURDERER, viewerUser, arena);
    Player viewerVotedTarget = votes.get(viewer);
    boolean viewerSkipped = skipVotes.contains(viewer);

    for(int idx = 0; idx < alivePlayers.size() && idx < PLAYER_SLOTS.length; idx++) {
      Player target = alivePlayers.get(idx);
      int slot = PLAYER_SLOTS[idx];

      ItemStack head = XMaterial.PLAYER_HEAD.parseItem();
      SkullMeta meta = (SkullMeta) head.getItemMeta();
      if(meta != null) {
        meta = VersionUtils.setPlayerHead(target, meta);

        boolean targetHasVoted = votes.containsKey(target) || skipVotes.contains(target);
        String votedBadge = targetHasVoted ? " §a[โหวตแล้ว [/]]" : " §7[รอโหวต...]";

        IUser targetUser = arena.getPlugin().getUserManager().getUser(target);
        if(viewerIsMurderer && Role.isRole(Role.MURDERER, targetUser, arena)) {
          meta.setDisplayName("§c§l " + target.getName() + " (คนร้าย)" + votedBadge);
        } else {
          meta.setDisplayName("§e§l " + target.getName() + votedBadge);
        }

        List<String> votersForTarget = new ArrayList<>();
        for(Map.Entry<Player, Player> entry : votes.entrySet()) {
          if(entry.getValue().equals(target)) {
            votersForTarget.add(entry.getKey().getName());
          }
        }

        List<String> lore = new ArrayList<>();
        if(target.equals(reporter)) {
          lore.add("§6 [ผู้เรียกประชุมในรอบนี้]");
        }
        lore.add("§7คะแนนโหวตปัจจุบัน: §e" + votersForTarget.size() + " โหวต");
        for(String voterName : votersForTarget) {
          lore.add("  §c> §f" + voterName);
        }
        lore.add(" ");

        if(target.equals(viewerVotedTarget)) {
          lore.add("§a§l[/] คุณเลือกโหวตผู้เล่นคนนี้แล้ว!");
        } else if(voteState == VoteState.DISCUSSION) {
          lore.add("§e รอหมดเวลาปรึกษาอีก " + voteTimer + " วินาที จึงจะโหวตได้");
        } else {
          lore.add("§e> คลิกเพื่อโหวตคัด " + target.getName() + " ออก!");
        }

        meta.setLore(lore);
        head.setItemMeta(meta);
      }
      gui.setItem(slot, head);
    }

    ItemStack skipItem = viewerSkipped ? XMaterial.LIME_BANNER.parseItem() : XMaterial.FEATHER.parseItem();
    ItemMeta skipMeta = skipItem.getItemMeta();
    if(skipMeta != null) {
      skipMeta.setDisplayName(viewerSkipped
        ? "§a§l[ [/] คุณเลือกข้ามการโหวตแล้ว (Skipped) ]"
        : "§b§l[ ข้ามการโหวต (Skip Vote) ]");
      List<String> skipLore = new ArrayList<>();
      skipLore.add("§7เลือกที่จะไม่โหวตคัดใครออกในรอบนี้");
      skipLore.add("§7หากเสียง Skip มากที่สุด จะไม่มีใครถูกคัดออก");
      skipLore.add(" ");
      skipLore.add("§fจำนวนคนโหวตข้ามขณะนี้: §b" + skipVotes.size() + " คน");
      for(Player skipper : skipVotes) {
        skipLore.add("  §b> §f" + skipper.getName());
      }
      skipLore.add(" ");
      if(viewerSkipped) {
        skipLore.add("§a§l[/] คุณเลือกข้ามการโหวตแล้ว!");
      } else if(voteState == VoteState.DISCUSSION) {
        skipLore.add("§e รอหมดเวลาปรึกษาอีก " + voteTimer + " วินาที จึงจะกดข้ามได้");
      } else {
        skipLore.add("§b> คลิกที่นี่เพื่อข้ามการโหวต (Skip Vote)");
      }
      skipMeta.setLore(skipLore);
      skipItem.setItemMeta(skipMeta);
    }
    gui.setItem(SKIP_SLOT, skipItem);

    ItemStack closeChat = XMaterial.OAK_SIGN.parseItem();
    ItemMeta closeMeta = closeChat.getItemMeta();
    if(closeMeta != null) {
      closeMeta.setDisplayName("§a§l[ ปิดหน้าต่างเพื่อพิมพ์แชท ]");
      closeMeta.setLore(Arrays.asList(
        "§7ซ่อนหน้าต่างโหวตชั่วคราวเพื่อพิมพ์คุยกับผู้เล่นอื่น",
        "§e> คลิกขวาไอเทมรูปดาวในมือเพื่อกลับมาเปิดเมนูโหวต!"
      ));
      closeChat.setItemMeta(closeMeta);
    }
    gui.setItem(CHAT_CLOSE_SLOT, closeChat);
  }

  public boolean handleVoteClick(Player voter, int rawSlot) {
    if(rawSlot == CHAT_CLOSE_SLOT) {
      voter.closeInventory();
      XSound.UI_BUTTON_CLICK.play(voter.getLocation(), 0.7F, 1.2F);
      VersionUtils.sendActionBar(voter, "§a ปิดหน้าต่างโหวตชั่วคราว (คลิกขวาไอเทมในมือเพื่อเปิดเมนูโหวตอีกครั้ง)");
      return true;
    }

    if(voteState == VoteState.DISCUSSION) {
      XSound.ENTITY_VILLAGER_NO.play(voter.getLocation(), 0.8F, 1.0F);
      VersionUtils.sendActionBar(voter, "§e กรุณารอหมดเวลาปรึกษาอีก " + voteTimer + " วินาที จึงจะลงคะแนนโหวตได้!");
      return true;
    }

    if(voteState != VoteState.VOTING) {
      return false;
    }

    if(votes.containsKey(voter) || skipVotes.contains(voter)) {
      new MessageBuilder("IN_GAME_MESSAGES_ARENA_VOTING_ALREADY_VOTED")
        .asKey().player(voter).sendPlayer();
      XSound.ENTITY_VILLAGER_NO.play(voter.getLocation(), 0.8F, 1.0F);
      return true;
    }

    if(rawSlot == SKIP_SLOT) {
      skipVotes.add(voter);
      XSound.UI_BUTTON_CLICK.play(voter.getLocation(), 1F, 1.2F);
      XSound.BLOCK_NOTE_BLOCK_PLING.play(voter.getLocation(), 0.8F, 1.5F);

      new MessageBuilder("IN_GAME_MESSAGES_ARENA_VOTING_PLAYER_VOTED")
        .asKey().player(voter).arena(arena).sendArena();
      voter.sendMessage("§b[โหวต] §fคุณได้เลือก §b ข้ามการโหวต (Skip Vote) §fเรียบร้อยแล้ว!");

      refreshOpenVotingGUIs();

      if(hasEveryoneVoted()) {
        voteTimer = 0;
      }
      return true;
    }

    int targetIdx = -1;
    for(int i = 0; i < PLAYER_SLOTS.length; i++) {
      if(PLAYER_SLOTS[i] == rawSlot) {
        targetIdx = i;
        break;
      }
    }

    List<Player> alivePlayers = arena.getPlayersLeft();
    if(targetIdx >= 0 && targetIdx < alivePlayers.size()) {
      Player target = alivePlayers.get(targetIdx);
      votes.put(voter, target);
      XSound.UI_BUTTON_CLICK.play(voter.getLocation(), 1F, 1.2F);
      XSound.BLOCK_NOTE_BLOCK_PLING.play(voter.getLocation(), 0.8F, 1.5F);

      new MessageBuilder("IN_GAME_MESSAGES_ARENA_VOTING_PLAYER_VOTED")
        .asKey().player(voter).arena(arena).sendArena();
      voter.sendMessage("§a[โหวต] §fคุณได้ลงคะแนนโหวตให้ §e" + target.getName() + " §fเรียบร้อยแล้ว!");

      refreshOpenVotingGUIs();

      if(hasEveryoneVoted()) {
        voteTimer = 0;
      }
      return true;
    }

    return true;
  }

  private boolean hasEveryoneVoted() {
    int totalAlive = arena.getPlayersLeft().size();
    int totalVoted = votes.size() + skipVotes.size();
    return totalVoted >= totalAlive;
  }

  private void resolveVotes() {
    voteState = VoteState.RESULT;
    voteTimer = 5;

    for(Player player : arena.getPlayersLeft()) {
      player.closeInventory();
    }

    Map<Player, Integer> voteCounts = new HashMap<>();
    for(Player target : votes.values()) {
      voteCounts.merge(target, 1, Integer::sum);
    }

    int skipCount = skipVotes.size();

    for(Player p : arena.getPlayers()) {
      p.sendMessage("§6================= [ สรุปผลโหวตรอบกองไฟ ] =================");
      for(Map.Entry<Player, Integer> entry : voteCounts.entrySet()) {
        p.sendMessage("§e " + entry.getKey().getName() + ": §c" + entry.getValue() + " โหวต");
      }
      p.sendMessage("§b ข้ามการโหวต (Skip): §f" + skipCount + " โหวต");
      p.sendMessage("§6=========================================================");
    }

    Player ejected = null;
    int maxVotes = 0;
    boolean tie = false;

    for(Map.Entry<Player, Integer> entry : voteCounts.entrySet()) {
      if(entry.getValue() > maxVotes) {
        maxVotes = entry.getValue();
        ejected = entry.getKey();
        tie = false;
      } else if(entry.getValue() == maxVotes) {
        tie = true;
      }
    }

    if(tie || skipCount >= maxVotes || ejected == null) {
      new MessageBuilder("IN_GAME_MESSAGES_ARENA_VOTING_NO_EJECT")
        .asKey().arena(arena).sendArena();

      for(Player player : arena.getPlayers()) {
        VersionUtils.sendTitles(player,
          new MessageBuilder("IN_GAME_MESSAGES_ARENA_VOTING_NO_EJECT_TITLE").asKey().build(),
          new MessageBuilder("IN_GAME_MESSAGES_ARENA_VOTING_NO_EJECT_SUBTITLE").asKey().build(),
          10, 40, 10);
      }
    } else {
      Player ejectedPlayer = ejected;
      IUser ejectedUser = arena.getPlugin().getUserManager().getUser(ejectedPlayer);
      boolean wasMurderer = Role.isRole(Role.MURDERER, ejectedUser, arena);

      String roleReveal = wasMurderer
        ? new MessageBuilder("IN_GAME_MESSAGES_ARENA_VOTING_WAS_IMPOSTOR").asKey().build()
        : new MessageBuilder("IN_GAME_MESSAGES_ARENA_VOTING_WAS_NOT_IMPOSTOR").asKey().build();

      new MessageBuilder("IN_GAME_MESSAGES_ARENA_VOTING_EJECTED")
        .asKey().player(ejectedPlayer).arena(arena)
        .value(roleReveal)
        .sendArena();

      for(Player player : arena.getPlayers()) {
        VersionUtils.sendTitles(player,
          ChatColor.RED + ejectedPlayer.getName(),
          roleReveal,
          10, 60, 10);
      }

      restoreVoteMenuItem(ejectedPlayer);
      ejectedPlayer.damage(1000.0);

      if(wasMurderer && arena.aliveMurderer() <= 0) {
        ArenaUtils.onMurdererDeath(arena);
      }
    }
  }

  private void restoreVoteMenuItem(Player player) {
    if(player == null) {
      return;
    }
    if(savedSlotItems.containsKey(player.getUniqueId())) {
      ItemStack old = savedSlotItems.remove(player.getUniqueId());
      player.getInventory().setItem(VOTE_MENU_ITEM_SLOT, old);
    } else if(isVoteMenuItem(player.getInventory().getItem(VOTE_MENU_ITEM_SLOT))) {
      player.getInventory().setItem(VOTE_MENU_ITEM_SLOT, null);
    }
  }

  private void endVoting() {
    voteState = VoteState.NONE;
    lastVoteEndTime = System.currentTimeMillis();

    for(Player player : arena.getPlayersLeft()) {
      restoreVoteMenuItem(player);
      player.setWalkSpeed(0.2f);
      Location preLoc = preVoteLocations.get(player);
      if(preLoc != null) {
        VersionUtils.teleport(player, preLoc);
      }
    }

    for(Player player : arena.getPlayers()) {
      IUser user = arena.getPlugin().getUserManager().getUser(player);
      if(user.isSpectator()) {
        player.setWalkSpeed(0.2f);
      }
    }

    votes.clear();
    skipVotes.clear();
    preVoteLocations.clear();
    meetingCircleLocations.clear();
    savedSlotItems.clear();
    reporter = null;
    reportedVictimName = null;
  }

  public boolean isVotingActive() {
    return voteState != VoteState.NONE;
  }

  public VoteState getVoteState() {
    return voteState;
  }

  public Player getReporter() {
    return reporter;
  }

  public boolean isVotingGUI(String title) {
    return title != null && (VOTE_GUI_TITLE.equals(title) || title.contains("โหวตหาคนร้ายรอบกองไฟ"));
  }

  public void handlePlayerLeave(Player player) {
    restoreVoteMenuItem(player);
    votes.remove(player);
    skipVotes.remove(player);
    preVoteLocations.remove(player);
    meetingCircleLocations.remove(player.getUniqueId());
    votes.values().removeIf(target -> target.equals(player));
  }

  public void cleanup() {
    for(Player player : arena.getPlayers()) {
      restoreVoteMenuItem(player);
    }
    clearDeadBodies();
    voteState = VoteState.NONE;
    voteTimer = 0;
    votes.clear();
    skipVotes.clear();
    preVoteLocations.clear();
    meetingCircleLocations.clear();
    savedSlotItems.clear();
    reporter = null;
    reportedVictimName = null;
    lastVoteEndTime = 0;
  }
}
