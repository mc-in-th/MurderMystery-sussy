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
import org.bukkit.block.Block;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import plugily.projects.minigamesbox.api.user.IUser;
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
      if(hologram != null && !hologram.isDeleted()) {
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
            arena.getStands().removeIf(s -> s != null && stand.equals(s.getStand()));
            if(stand.isValid()) {
              stand.remove();
            }
          }
        }
      }
    }
  }

  public static final String VOTE_GUI_TITLE = "§8[ §cโหวตหาคนร้ายรอบกองไฟ §8]";
  public static final int VOTE_MENU_ITEM_SLOT = 4;

  private static final int INFO_SLOT = 4;
  private static final int CONFIRM_SLOT = 47;
  private static final int SKIP_SLOT = 49;
  private static final int CANCEL_SELECT_SLOT = 51;
  private static final int CHAT_CLOSE_SLOT = 53;
  private static final int[] PLAYER_SLOTS = {
    10, 11, 12, 13, 14, 15, 16,
    19, 20, 21, 22, 23, 24, 25,
    28, 29, 30, 31, 32, 33, 34
  };

  private final Arena arena;
  private final NoteBlockMusicManager musicManager;
  private VoteState voteState = VoteState.NONE;
  private int voteTimer = 0;
  private Player reporter;
  private String reportedVictimName;
  private final Map<Player, Player> votes = new HashMap<>();
  private final Set<Player> skipVotes = new HashSet<>();
  private final Map<Player, Player> pendingVotes = new HashMap<>();
  private final Set<Player> pendingSkips = new HashSet<>();
  private final Map<Player, Location> preVoteLocations = new HashMap<>();
  private final Map<UUID, Location> meetingCircleLocations = new HashMap<>();
  private final Map<UUID, TextDisplay> circleStatusDisplays = new HashMap<>();
  private final Map<UUID, ItemStack> savedSlotItems = new HashMap<>();
  private final List<DeadBody> activeBodies = new ArrayList<>();
  private Player pendingEjectedPlayer;
  private boolean verdictExecuted = false;
  private long lastVoteEndTime = 0;

  public VotingManager(Arena arena) {
    this.arena = arena;
    this.musicManager = new NoteBlockMusicManager(arena);
  }

  private void broadcastArenaMessage(String message) {
    for(Player p : arena.getPlayers()) {
      p.sendMessage(message);
    }
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

  private void clearCircleStatusDisplays() {
    for(TextDisplay td : circleStatusDisplays.values()) {
      if(td != null && td.isValid()) {
        td.remove();
      }
    }
    circleStatusDisplays.clear();
  }

  private void updateCircleStatusDisplays() {
    for(Player player : arena.getPlayersLeft()) {
      TextDisplay td = circleStatusDisplays.get(player.getUniqueId());
      if(td == null || !td.isValid()) {
        continue;
      }
      if(voteState == VoteState.DISCUSSION) {
        td.setText(ChatColor.translateAlternateColorCodes('&',
          "&e&l" + player.getName() + "\n&7[กำลังปรึกษาหารือ]"));
      } else if(voteState == VoteState.VOTING) {
        boolean locked = hasLockedVote(player);
        td.setText(ChatColor.translateAlternateColorCodes('&',
          "&e&l" + player.getName() + "\n" + (locked ? "&a&l[โหวตแล้ว]" : "&7[กำลังตัดสินใจ...]")));
      }
    }
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
      long elapsedMs = System.currentTimeMillis() - lastVoteEndTime;
      if(elapsedMs < cooldownMs) {
        long remainSec = Math.max(1L, (cooldownMs - elapsedMs) / 1000L);
        reportingPlayer.sendMessage("§c[ประชุมฉุกเฉิน] การเรียกประชุมฉุกเฉินยังติดคูลดาวน์อยู่ กรุณารออีก §e" + remainSec + " วินาที!");
        return false;
      }
    }

    this.reporter = reportingPlayer;
    this.reportedVictimName = victimName;
    this.votes.clear();
    this.skipVotes.clear();
    this.pendingVotes.clear();
    this.pendingSkips.clear();
    this.preVoteLocations.clear();
    this.meetingCircleLocations.clear();
    this.savedSlotItems.clear();
    this.pendingEjectedPlayer = null;
    this.verdictExecuted = false;
    clearCircleStatusDisplays();

    if(arena.getSabotageManager() != null) {
      arena.getSabotageManager().cancelActiveSabotage("VOTING");
    }

    clearDeadBodies();

    if(victimName != null) {
      musicManager.playBodyReportSound();
      broadcastArenaMessage("§c[รายงานศพ] §e" + reportingPlayer.getName() + " §fพบศพของ §c" + victimName + " §fและเรียกประชุมด่วนรอบกองไฟ!");
    } else {
      musicManager.playEmergencyMeetingSound();
      broadcastArenaMessage("§6[ประชุมฉุกเฉิน] §e" + reportingPlayer.getName() + " §fได้เรียกประชุมฉุกเฉินรอบกองไฟ!");
    }

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

      TextDisplay statusTd = arena.spawnTextDisplay(
        circleSpot.clone().add(0.0, 2.25, 0.0),
        "&e&l" + player.getName() + "\n&7[กำลังปรึกษาหารือ]"
      );
      if(statusTd != null) {
        circleStatusDisplays.put(player.getUniqueId(), statusTd);
      }

      player.closeInventory();
      VersionUtils.teleport(player, circleSpot);
      player.setWalkSpeed(0.0f);
      player.setFoodLevel(20);

      savedSlotItems.put(player.getUniqueId(), player.getInventory().getItem(VOTE_MENU_ITEM_SLOT));
      player.getInventory().setItem(VOTE_MENU_ITEM_SLOT, createVoteMenuItem());
      player.getInventory().setHeldItemSlot(VOTE_MENU_ITEM_SLOT);

      if(victimName != null) {
        VersionUtils.sendTitles(player,
          "§c§l[ รายงานศพผู้เสียชีวิต! ]",
          "§e" + reportingPlayer.getName() + " §fพบศพของ §c" + victimName,
          10, 60, 10);
      } else {
        VersionUtils.sendTitles(player,
          "§6§l[ เรียกประชุมฉุกเฉินรอบกองไฟ! ]",
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

    broadcastArenaMessage("§e[รอบกองไฟ] §fเริ่มเวลาปรึกษาหารือเป็นเวลา §c" + voteTimer + " วินาที §7(พูดคุยในแชทเพื่อหาตัวคนร้าย)");

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
          updateCircleStatusDisplays();
          openVotingGUIForAll();

          broadcastArenaMessage("§a[เปิดโหวต] §fเปิดให้ลงคะแนนโหวตแล้ว! มีเวลาตัดสินใจ §e" + voteTimer + " วินาที");
        } else {
          refreshOpenVotingGUIs();
          for(Player player : arena.getPlayersLeft()) {
            VersionUtils.sendActionBar(player,
              "§6[รอบกองไฟ] เวลาปรึกษา: §e" + voteTimer + " วินาที §7(คลิกขวาไอเทมในมือเพื่อดูรายชื่อ)");
          }
        }
        break;

      case VOTING:
        if(voteTimer <= 0 || hasEveryoneVoted()) {
          finalizePendingSelections();
          resolveVotes();
        } else {
          refreshOpenVotingGUIs();
          int totalVoted = votes.size() + skipVotes.size();
          int totalAlive = arena.getPlayersLeft().size();
          for(Player player : arena.getPlayersLeft()) {
            if(voteTimer <= 5) {
              XSound.BLOCK_NOTE_BLOCK_HAT.play(player.getLocation(), 0.6F, 1.6F);
            }
            VersionUtils.sendActionBar(player,
              "§c[เวลาโหวต] เหลือ §e" + voteTimer + " วินาที §8| §aยืนยันโหวตแล้ว " + totalVoted + "/" + totalAlive + " คน §7(คลิกขวาไอเทมในมือเพื่อเปิดเมนูโหวต)");
          }
        }
        break;

      case RESULT:
        if(voteTimer > 3) {
          for(Player p : arena.getPlayers()) {
            XSound.BLOCK_NOTE_BLOCK_SNARE.play(p.getLocation(), 0.7F, 1.2F + (7 - voteTimer) * 0.15F);
          }
        } else if(voteTimer == 3 && !verdictExecuted) {
          executeVerdict();
        } else if(voteTimer <= 0) {
          endVoting();
        }
        break;
      default:
        break;
    }

    return true;
  }

  private void finalizePendingSelections() {
    for(Player player : arena.getPlayersLeft()) {
      if(hasLockedVote(player)) {
        continue;
      }
      if(pendingSkips.contains(player)) {
        skipVotes.add(player);
        pendingSkips.remove(player);
      } else if(pendingVotes.containsKey(player)) {
        Player target = pendingVotes.remove(player);
        if(target != null) {
          votes.put(player, target);
        }
      }
    }
  }

  public void openVotingGUIForAll() {
    for(Player viewer : arena.getPlayersLeft()) {
      openVotingGUI(viewer);
    }
  }

  public void openVotingGUI(Player viewer) {
    if(voteState != VoteState.DISCUSSION && voteState != VoteState.VOTING && voteState != VoteState.RESULT) {
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

  private boolean hasLockedVote(Player player) {
    return votes.containsKey(player) || skipVotes.contains(player);
  }

  private void applyGlow(ItemStack item) {
    ItemMeta meta = item.getItemMeta();
    if(meta != null) {
      try {
        meta.addEnchant(Enchantment.UNBREAKING, 1, true);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
      } catch(Throwable ignored) {
      }
      item.setItemMeta(meta);
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
      fMeta.setDisplayName("§6 ");
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
    boolean isResultPhase = (voteState == VoteState.RESULT);

    ItemStack campfireInfo = XMaterial.CAMPFIRE.parseItem();
    ItemMeta cMeta = campfireInfo.getItemMeta();
    if(cMeta != null) {
      cMeta.setDisplayName("§6§l[ การประชุมรอบกองไฟ (Campfire Meeting) ]");
      List<String> infoLore = new ArrayList<>();
      if(reportedVictimName != null) {
        infoLore.add("§cรายงานศพของ: §f" + reportedVictimName);
      } else {
        infoLore.add("§eประเภท: §fเรียกประชุมฉุกเฉินที่กองไฟ");
      }
      if(reporter != null) {
        infoLore.add("§eผู้เรียกประชุม: §f" + reporter.getName());
      }
      infoLore.add(" ");
      if(voteState == VoteState.DISCUSSION) {
        infoLore.add("§fสถานะ: §eกำลังปรึกษาหารือ (รอเปิดโหวต)");
        infoLore.add("§fจะเปิดให้โหวตในอีก: §6" + voteTimer + " วินาที");
      } else if(voteState == VoteState.VOTING) {
        infoLore.add("§fสถานะ: §aเปิดให้ลงคะแนนโหวตแล้ว! (โหวตแบบลับ)");
        infoLore.add("§fเวลาโหวตที่เหลือ: §c" + voteTimer + " วินาที");
      } else {
        infoLore.add("§fสถานะ: §6เปิดเผยผลคะแนนโหวตทั้งหมด!");
      }
      infoLore.add("§fยืนยันโหวตแล้ว: §a" + totalVoted + " §7/ §f" + alivePlayers.size() + " คน");
      if(isResultPhase) {
        infoLore.add("§fคนโหวตข้าม (Skip): §b" + skipVotes.size() + " คน");
      } else {
        infoLore.add("§7(ผลคะแนนโหวตจะเปิดเผยพร้อมกันเมื่อทุกคนโหวตครบ)");
      }
      cMeta.setLore(infoLore);
      campfireInfo.setItemMeta(cMeta);
    }
    gui.setItem(INFO_SLOT, campfireInfo);

    IUser viewerUser = arena.getPlugin().getUserManager().getUser(viewer);
    boolean viewerIsMurderer = Role.isRole(Role.MURDERER, viewerUser, arena);
    boolean viewerLocked = hasLockedVote(viewer);
    Player viewerLockedTarget = votes.get(viewer);
    boolean viewerLockedSkip = skipVotes.contains(viewer);
    Player viewerPendingTarget = pendingVotes.get(viewer);
    boolean viewerPendingSkip = pendingSkips.contains(viewer);

    for(int idx = 0; idx < alivePlayers.size() && idx < PLAYER_SLOTS.length; idx++) {
      Player target = alivePlayers.get(idx);
      int slot = PLAYER_SLOTS[idx];

      ItemStack head = XMaterial.PLAYER_HEAD.parseItem();
      SkullMeta meta = (SkullMeta) head.getItemMeta();
      if(meta != null) {
        meta = VersionUtils.setPlayerHead(target, meta);

        boolean targetHasVoted = hasLockedVote(target);
        String votedBadge = targetHasVoted ? " §a[โหวตแล้ว]" : " §7[รอโหวต...]";

        IUser targetUser = arena.getPlugin().getUserManager().getUser(target);
        if(viewerIsMurderer && Role.isRole(Role.MURDERER, targetUser, arena)) {
          meta.setDisplayName("§c§l" + target.getName() + " (คนร้าย)" + votedBadge);
        } else {
          meta.setDisplayName("§e§l" + target.getName() + votedBadge);
        }

        List<String> lore = new ArrayList<>();
        if(target.equals(reporter)) {
          lore.add("§6[ผู้เรียกประชุมในรอบนี้]");
        }
        lore.add("§7สถานะการลงคะแนน: " + (targetHasVoted ? "§aยืนยันโหวตแล้ว" : "§eกำลังตัดสินใจ..."));

        if(isResultPhase) {
          List<String> votersForTarget = new ArrayList<>();
          for(Map.Entry<Player, Player> entry : votes.entrySet()) {
            if(entry.getValue().equals(target)) {
              votersForTarget.add(entry.getKey().getName());
            }
          }
          lore.add(" ");
          lore.add("§6ผลคะแนนโหวตที่ได้รับ: §c" + votersForTarget.size() + " โหวต");
          for(String voterName : votersForTarget) {
            lore.add("  §c- §f" + voterName);
          }
        } else {
          lore.add("§8(คะแนนโหวตถูกซ่อนไว้จนกว่าจะหมดเวลาโหวต)");
        }

        lore.add(" ");
        if(target.equals(viewerLockedTarget)) {
          lore.add("§a§lคุณได้ยืนยันโหวตผู้เล่นคนนี้แล้ว!");
        } else if(viewerLocked) {
          lore.add("§7คุณได้ยืนยันผลโหวตในรอบนี้ไปแล้ว");
        } else if(voteState == VoteState.DISCUSSION) {
          lore.add("§eรอหมดเวลาปรึกษาอีก " + voteTimer + " วินาที จึงจะโหวตได้");
        } else if(target.equals(viewerPendingTarget)) {
          lore.add("§e§l> กำลังเลือกผู้เล่นคนนี้อยู่!");
          lore.add("§aคลิกซ้ำอีกครั้ง หรือกดปุ่มสีเขียวด้านล่างเพื่อยืนยันโหวต");
        } else if(voteState == VoteState.VOTING) {
          lore.add("§eคลิกเพื่อเลือกโหวต " + target.getName());
        }

        meta.setLore(lore);
        head.setItemMeta(meta);
      }
      if(target.equals(viewerLockedTarget) || target.equals(viewerPendingTarget)) {
        applyGlow(head);
      }
      gui.setItem(slot, head);
    }

    boolean skipHighlighted = viewerLockedSkip || viewerPendingSkip;
    ItemStack skipItem = skipHighlighted ? XMaterial.LIME_BANNER.parseItem() : XMaterial.FEATHER.parseItem();
    ItemMeta skipMeta = skipItem.getItemMeta();
    if(skipMeta != null) {
      if(viewerLockedSkip) {
        skipMeta.setDisplayName("§a§l[ คุณยืนยันข้ามการโหวตแล้ว (Skipped) ]");
      } else if(viewerPendingSkip) {
        skipMeta.setDisplayName("§e§l[ กำลังเลือกข้ามการโหวต - คลิกซ้ำเพื่อยืนยัน ]");
      } else {
        skipMeta.setDisplayName("§b§l[ ข้ามการโหวต (Skip Vote) ]");
      }
      List<String> skipLore = new ArrayList<>();
      skipLore.add("§7เลือกที่จะไม่โหวตคัดใครออกในรอบนี้");
      skipLore.add("§7หากเสียง Skip มากที่สุด จะไม่มีใครถูกคัดออก");
      if(isResultPhase) {
        skipLore.add(" ");
        skipLore.add("§fจำนวนคนโหวตข้ามทั้งหมด: §b" + skipVotes.size() + " คน");
        for(Player skipper : skipVotes) {
          skipLore.add("  §b- §f" + skipper.getName());
        }
      }
      skipLore.add(" ");
      if(viewerLockedSkip) {
        skipLore.add("§a§lคุณได้ยืนยันข้ามการโหวตแล้ว!");
      } else if(viewerLocked) {
        skipLore.add("§7คุณได้ยืนยันผลโหวตในรอบนี้ไปแล้ว");
      } else if(voteState == VoteState.DISCUSSION) {
        skipLore.add("§eรอหมดเวลาปรึกษาอีก " + voteTimer + " วินาที จึงจะกดข้ามได้");
      } else if(viewerPendingSkip) {
        skipLore.add("§e§l> กำลังเลือกข้ามการโหวตอยู่!");
        skipLore.add("§aคลิกซ้ำอีกครั้ง หรือกดปุ่มสีเขียวด้านซ้ายเพื่อยืนยัน");
      } else if(voteState == VoteState.VOTING) {
        skipLore.add("§bคลิกเพื่อเลือกข้ามการโหวต (Skip Vote)");
      }
      skipMeta.setLore(skipLore);
      skipItem.setItemMeta(skipMeta);
    }
    if(skipHighlighted) {
      applyGlow(skipItem);
    }
    gui.setItem(SKIP_SLOT, skipItem);

    if(voteState == VoteState.VOTING && !viewerLocked && (viewerPendingTarget != null || viewerPendingSkip)) {
      String choiceLabel = viewerPendingSkip ? "ข้ามการโหวต (Skip)" : viewerPendingTarget.getName();

      ItemStack confirmBtn = XMaterial.EMERALD_BLOCK.parseItem();
      ItemMeta confirmMeta = confirmBtn.getItemMeta();
      if(confirmMeta != null) {
        confirmMeta.setDisplayName("§a§l[ ยืนยันผลโหวต: §e" + choiceLabel + " §a§l]");
        confirmMeta.setLore(Arrays.asList(
          "§7คุณกำลังเลือก: §e" + choiceLabel,
          "§cเมื่อกดยืนยันแล้วจะไม่สามารถเปลี่ยนใจได้!",
          " ",
          "§aคลิกที่นี่เพื่อล็อกผลโหวตทันที!"
        ));
        confirmBtn.setItemMeta(confirmMeta);
      }
      applyGlow(confirmBtn);
      gui.setItem(CONFIRM_SLOT, confirmBtn);

      ItemStack cancelBtn = XMaterial.REDSTONE_BLOCK.parseItem();
      ItemMeta cancelMeta = cancelBtn.getItemMeta();
      if(cancelMeta != null) {
        cancelMeta.setDisplayName("§c§l[ ยกเลิกตัวเลือกที่เลือกไว้ ]");
        cancelMeta.setLore(Arrays.asList(
          "§7ยกเลิกการเลือก §e" + choiceLabel,
          "§7เพื่อกลับไปพิจารณาผู้เล่นคนอื่นใหม่"
        ));
        cancelBtn.setItemMeta(cancelMeta);
      }
      gui.setItem(CANCEL_SELECT_SLOT, cancelBtn);
    }

    ItemStack closeChat = XMaterial.OAK_SIGN.parseItem();
    ItemMeta closeMeta = closeChat.getItemMeta();
    if(closeMeta != null) {
      closeMeta.setDisplayName("§a§l[ ปิดหน้าต่างเพื่อพิมพ์แชท ]");
      closeMeta.setLore(Arrays.asList(
        "§7ซ่อนหน้าต่างโหวตชั่วคราวเพื่อพิมพ์คุยกับผู้เล่นอื่น",
        "§eคลิกขวาไอเทมรูปดาวในมือเพื่อกลับมาเปิดเมนูโหวต!"
      ));
      closeChat.setItemMeta(closeMeta);
    }
    gui.setItem(CHAT_CLOSE_SLOT, closeChat);
  }

  public boolean handleVoteClick(Player voter, int rawSlot) {
    if(rawSlot == CHAT_CLOSE_SLOT) {
      voter.closeInventory();
      XSound.UI_BUTTON_CLICK.play(voter.getLocation(), 0.7F, 1.2F);
      VersionUtils.sendActionBar(voter, "§aปิดหน้าต่างโหวตชั่วคราว (คลิกขวาไอเทมในมือเพื่อเปิดเมนูโหวตอีกครั้ง)");
      return true;
    }

    if(voteState == VoteState.DISCUSSION) {
      XSound.ENTITY_VILLAGER_NO.play(voter.getLocation(), 0.8F, 1.0F);
      VersionUtils.sendActionBar(voter, "§eกรุณารอหมดเวลาปรึกษาอีก " + voteTimer + " วินาที จึงจะลงคะแนนโหวตได้!");
      return true;
    }

    if(voteState != VoteState.VOTING) {
      return true;
    }

    if(hasLockedVote(voter)) {
      voter.sendMessage("§c[โหวต] คุณได้ยืนยันการลงคะแนนโหวตไปแล้ว!");
      XSound.ENTITY_VILLAGER_NO.play(voter.getLocation(), 0.8F, 1.0F);
      return true;
    }

    if(rawSlot == CONFIRM_SLOT) {
      if(pendingSkips.contains(voter)) {
        confirmSkipVote(voter);
      } else if(pendingVotes.containsKey(voter)) {
        confirmTargetVote(voter, pendingVotes.get(voter));
      }
      return true;
    }

    if(rawSlot == CANCEL_SELECT_SLOT) {
      pendingVotes.remove(voter);
      pendingSkips.remove(voter);
      XSound.UI_BUTTON_CLICK.play(voter.getLocation(), 0.8F, 0.8F);
      VersionUtils.sendActionBar(voter, "§eยกเลิกตัวเลือกแล้ว สามารถเลือกผู้เล่นใหม่ได้");
      refreshOpenVotingGUIs();
      return true;
    }

    if(rawSlot == SKIP_SLOT) {
      if(pendingSkips.contains(voter)) {
        confirmSkipVote(voter);
      } else {
        pendingVotes.remove(voter);
        pendingSkips.add(voter);
        XSound.UI_BUTTON_CLICK.play(voter.getLocation(), 0.9F, 1.3F);
        VersionUtils.sendActionBar(voter, "§eเลือก [ข้ามการโหวต] - คลิกซ้ำอีกครั้งหรือกดปุ่มสีเขียวเพื่อยืนยัน!");
        refreshOpenVotingGUIs();
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
      if(target.equals(pendingVotes.get(voter))) {
        confirmTargetVote(voter, target);
      } else {
        pendingSkips.remove(voter);
        pendingVotes.put(voter, target);
        XSound.UI_BUTTON_CLICK.play(voter.getLocation(), 0.9F, 1.3F);
        VersionUtils.sendActionBar(voter, "§eเลือก [" + target.getName() + "] - คลิกซ้ำอีกครั้งหรือกดปุ่มสีเขียวเพื่อยืนยัน!");
        refreshOpenVotingGUIs();
      }
      return true;
    }

    return true;
  }

  private void confirmSkipVote(Player voter) {
    pendingSkips.remove(voter);
    pendingVotes.remove(voter);
    skipVotes.add(voter);
    XSound.UI_BUTTON_CLICK.play(voter.getLocation(), 1F, 1.2F);
    XSound.BLOCK_NOTE_BLOCK_PLING.play(voter.getLocation(), 0.8F, 1.5F);

    int totalVoted = votes.size() + skipVotes.size();
    int totalAlive = arena.getPlayersLeft().size();
    broadcastArenaMessage("§7[โหวต] §e" + voter.getName() + " §fได้ยืนยันการลงคะแนนโหวตแล้ว §7(" + totalVoted + "/" + totalAlive + ")");
    voter.sendMessage("§b[โหวต] §fคุณได้ยืนยัน §bข้ามการโหวต (Skip Vote) §fเรียบร้อยแล้ว!");

    updateCircleStatusDisplays();
    refreshOpenVotingGUIs();

    if(hasEveryoneVoted()) {
      voteTimer = 0;
    }
  }

  private void confirmTargetVote(Player voter, Player target) {
    if(target == null) {
      return;
    }
    pendingSkips.remove(voter);
    pendingVotes.remove(voter);
    votes.put(voter, target);
    XSound.UI_BUTTON_CLICK.play(voter.getLocation(), 1F, 1.2F);
    XSound.BLOCK_NOTE_BLOCK_PLING.play(voter.getLocation(), 0.8F, 1.5F);

    int totalVoted = votes.size() + skipVotes.size();
    int totalAlive = arena.getPlayersLeft().size();
    broadcastArenaMessage("§7[โหวต] §e" + voter.getName() + " §fได้ยืนยันการลงคะแนนโหวตแล้ว §7(" + totalVoted + "/" + totalAlive + ")");
    voter.sendMessage("§a[โหวต] §fคุณได้ยืนยันลงคะแนนโหวตให้ §e" + target.getName() + " §fเรียบร้อยแล้ว!");

    updateCircleStatusDisplays();
    refreshOpenVotingGUIs();

    if(hasEveryoneVoted()) {
      voteTimer = 0;
    }
  }

  private boolean hasEveryoneVoted() {
    int totalAlive = arena.getPlayersLeft().size();
    int totalVoted = votes.size() + skipVotes.size();
    return totalVoted >= totalAlive;
  }

  private void resolveVotes() {
    voteState = VoteState.RESULT;
    voteTimer = 7;
    verdictExecuted = false;
    pendingEjectedPlayer = null;

    Map<Player, List<String>> votersByTarget = new HashMap<>();
    for(Map.Entry<Player, Player> entry : votes.entrySet()) {
      votersByTarget.computeIfAbsent(entry.getValue(), k -> new ArrayList<>()).add(entry.getKey().getName());
    }

    int skipCount = skipVotes.size();
    List<String> skipperNames = new ArrayList<>();
    for(Player skipper : skipVotes) {
      skipperNames.add(skipper.getName());
    }

    for(Player player : arena.getPlayersLeft()) {
      TextDisplay td = circleStatusDisplays.get(player.getUniqueId());
      if(td != null && td.isValid()) {
        List<String> voters = votersByTarget.getOrDefault(player, new ArrayList<>());
        String voteText = voters.isEmpty() ? "&7[0 โหวต]" : "&c&l[" + voters.size() + " โหวต]";
        td.setText(ChatColor.translateAlternateColorCodes('&',
          "&e&l" + player.getName() + "\n" + voteText));
      }
    }

    for(Player p : arena.getPlayers()) {
      p.sendMessage("§6================= [ เปิดเผยผลโหวตรอบกองไฟ ] =================");
      for(Player alive : arena.getPlayersLeft()) {
        List<String> voters = votersByTarget.getOrDefault(alive, new ArrayList<>());
        if(!voters.isEmpty()) {
          p.sendMessage("§e- " + alive.getName() + " §fได้รับ §c§l" + voters.size() + " โหวต §7(" + String.join(", ", voters) + ")");
        }
      }
      if(skipCount > 0) {
        p.sendMessage("§b- ข้ามการโหวต (Skip): §f§l" + skipCount + " โหวต §7(" + String.join(", ", skipperNames) + ")");
      } else {
        p.sendMessage("§b- ข้ามการโหวต (Skip): §70 โหวต");
      }
      p.sendMessage("§6=============================================================");
    }

    refreshOpenVotingGUIs();

    Player topCandidate = null;
    int maxVotes = 0;
    boolean tie = false;

    for(Map.Entry<Player, List<String>> entry : votersByTarget.entrySet()) {
      int count = entry.getValue().size();
      if(count > maxVotes) {
        maxVotes = count;
        topCandidate = entry.getKey();
        tie = false;
      } else if(count == maxVotes) {
        tie = true;
      }
    }

    if(!tie && skipCount < maxVotes && topCandidate != null) {
      pendingEjectedPlayer = topCandidate;
    }

    for(Player p : arena.getPlayers()) {
      XSound.BLOCK_NOTE_BLOCK_PLING.play(p.getLocation(), 1.0F, 1.2F);
      VersionUtils.sendTitles(p,
        "§6§l[ เปิดเผยผลคะแนนโหวต! ]",
        pendingEjectedPlayer != null
          ? "§eเสียงส่วนใหญ่ชี้ไปที่ §c§l" + pendingEjectedPlayer.getName() + " §7(" + maxVotes + " โหวต)"
          : "§bคะแนนโหวตเสมอ หรือเสียงส่วนใหญ่เลือกข้าม (Skip)",
        5, 45, 10);
    }
  }

  private void executeVerdict() {
    verdictExecuted = true;

    for(Player player : arena.getPlayersLeft()) {
      player.closeInventory();
    }

    if(pendingEjectedPlayer == null || !pendingEjectedPlayer.isOnline()) {
      broadcastArenaMessage("§e[ผลการโหวต] §fไม่มีใครถูกโหวตคัดออกในรอบนี้ §7(คะแนนโหวตเสมอ หรือเสียงส่วนใหญ่เลือกข้าม)");

      for(Player player : arena.getPlayers()) {
        XSound.BLOCK_NOTE_BLOCK_BASS.play(player.getLocation(), 1.0F, 0.8F);
        VersionUtils.sendTitles(player,
          "§e§lไม่มีใครถูกโหวตออก",
          "§7คะแนนโหวตเสมอ หรือเสียงส่วนใหญ่เลือกข้าม",
          10, 45, 10);
      }
      return;
    }

    Player ejectedPlayer = pendingEjectedPlayer;
    IUser ejectedUser = arena.getPlugin().getUserManager().getUser(ejectedPlayer);
    boolean wasMurderer = Role.isRole(Role.MURDERER, ejectedUser, arena);

    Location center = arena.getCampfireLocation();
    if(center != null && center.getWorld() != null) {
      Location fireCenter = center.clone().add(0.5, 1.0, 0.5);
      meetingCircleLocations.put(ejectedPlayer.getUniqueId(), fireCenter);
      VersionUtils.teleport(ejectedPlayer, fireCenter);
      try {
        center.getWorld().strikeLightningEffect(fireCenter);
      } catch(Throwable ignored) {
      }
    }

    String roleReveal = wasMurderer
      ? "§a(เขาคือคนร้าย!)"
      : "§c(เขาไม่ใช่คนร้าย...)";

    broadcastArenaMessage("§c[ผลการโหวต] §e" + ejectedPlayer.getName() + " §fถูกโหวตคัดออกรอบกองไฟ " + roleReveal);

    for(Player player : arena.getPlayers()) {
      XSound.ENTITY_GENERIC_EXPLODE.play(player.getLocation(), 0.9F, 1.0F);
      VersionUtils.sendTitles(player,
        "§c§l" + ejectedPlayer.getName() + " ถูกโหวตคัดออก!",
        roleReveal,
        10, 60, 10);
    }

    restoreVoteMenuItem(ejectedPlayer);
    ejectedPlayer.damage(1000.0);

    if(wasMurderer && arena.aliveMurderer() <= 0) {
      ArenaUtils.onMurdererDeath(arena);
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
    clearCircleStatusDisplays();

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
    pendingVotes.clear();
    pendingSkips.clear();
    preVoteLocations.clear();
    meetingCircleLocations.clear();
    savedSlotItems.clear();
    pendingEjectedPlayer = null;
    verdictExecuted = false;
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
    pendingVotes.remove(player);
    pendingSkips.remove(player);
    preVoteLocations.remove(player);
    meetingCircleLocations.remove(player.getUniqueId());
    TextDisplay td = circleStatusDisplays.remove(player.getUniqueId());
    if(td != null && td.isValid()) {
      td.remove();
    }
    votes.values().removeIf(target -> target.equals(player));
    pendingVotes.values().removeIf(target -> target.equals(player));
  }

  public void cleanup() {
    musicManager.stopAll();
    clearCircleStatusDisplays();
    for(Player player : arena.getPlayers()) {
      restoreVoteMenuItem(player);
    }
    clearDeadBodies();
    voteState = VoteState.NONE;
    voteTimer = 0;
    votes.clear();
    skipVotes.clear();
    pendingVotes.clear();
    pendingSkips.clear();
    preVoteLocations.clear();
    meetingCircleLocations.clear();
    savedSlotItems.clear();
    pendingEjectedPlayer = null;
    verdictExecuted = false;
    reporter = null;
    reportedVictimName = null;
    lastVoteEndTime = 0;
  }
}
