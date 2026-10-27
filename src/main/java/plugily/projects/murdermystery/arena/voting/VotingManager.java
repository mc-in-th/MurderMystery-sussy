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

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
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
import plugily.projects.minigamesbox.api.arena.IArenaState;
import plugily.projects.minigamesbox.api.user.IUser;
import plugily.projects.minigamesbox.classic.utils.hologram.ArmorStandHologram;
import plugily.projects.minigamesbox.classic.utils.version.VersionUtils;
import plugily.projects.minigamesbox.classic.utils.version.xseries.XMaterial;
import plugily.projects.minigamesbox.classic.utils.version.xseries.XSound;
import plugily.projects.minigamesbox.classic.utils.version.xseries.inventory.XInventoryView;
import plugily.projects.murdermystery.arena.Arena;
import plugily.projects.murdermystery.arena.ArenaUtils;
import plugily.projects.murdermystery.arena.role.Role;
import plugily.projects.murdermystery.utils.HalloweenChat;

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
    private final Runnable cleanupAction;
    private final long spawnTimeMs;

    public DeadBody(String victimName, Location location, List<ArmorStand> stands, TextDisplay textDisplay, ArmorStandHologram hologram) {
      this(victimName, location, stands, textDisplay, hologram, null);
    }

    public DeadBody(String victimName, Location location, List<ArmorStand> stands, TextDisplay textDisplay, ArmorStandHologram hologram, Runnable cleanupAction) {
      this.victimName = victimName;
      this.location = location;
      this.stands = stands;
      this.textDisplay = textDisplay;
      this.hologram = hologram;
      this.cleanupAction = cleanupAction;
      this.spawnTimeMs = System.currentTimeMillis();
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

    public long getSpawnTimeMs() {
      return spawnTimeMs;
    }

    public void remove(Arena arena) {
      if(cleanupAction != null) {
        try {
          cleanupAction.run();
        } catch(Throwable ignored) {
        }
      }
      if(hologram != null) {
        arena.getPlugin().getHologramManager().getHolograms().remove(hologram);
        if(!hologram.isDeleted()) {
          try {
            hologram.delete();
          } catch(Throwable ignored) {
          }
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

  public static final String VOTE_GUI_RAW_TITLE = "<shadow:#000000FF><#5A4E7C>[</shadow><#FF7518>Voting<shadow:#000000FF><#5A4E7C>]</shadow>";
  public static final String VOTE_GUI_TITLE = HalloweenChat.color(VOTE_GUI_RAW_TITLE);
  public static final int VOTE_MENU_ITEM_SLOT = 4;

  private static final int GUI_SIZE = 45;
  private static final int INFO_SLOT = 4;
  private static final int CONFIRM_SLOT = 38;
  private static final int SKIP_SLOT = 40;
  private static final int CANCEL_SELECT_SLOT = 42;
  private static final int CHAT_CLOSE_SLOT = 44;
  private static final int[] BORDER_SLOTS = {
    0, 1, 2, 3, 5, 6, 7, 8,
    9, 17, 18, 26, 27, 35,
    36, 37, 38, 39, 41, 42, 43
  };

  private static final long BODY_SPAWN_GRACE_MS = 1500L;
  private static final long KILLER_REPORT_COOLDOWN_MS = 2500L;

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
  private final Map<UUID, Long> recentKills = new HashMap<>();
  private final List<DeadBody> activeBodies = new ArrayList<>();
  private Player pendingEjectedPlayer;
  private String pendingEjectedName;
  private boolean pendingEjectedWasMurderer = false;
  private boolean verdictExecuted = false;
  private long lastVoteEndTime = 0;
  private int remainingEmergencyMeetings = 2;

  public VotingManager(Arena arena) {
    this.arena = arena;
    this.musicManager = new NoteBlockMusicManager(arena);
    this.remainingEmergencyMeetings = arena.getPlugin().getConfig().getInt("Voting.Max-Emergency-Meetings", 2);
  }

  public void initForGame() {
    this.remainingEmergencyMeetings = arena.getPlugin().getConfig().getInt("Voting.Max-Emergency-Meetings", 2);
    this.lastVoteEndTime = System.currentTimeMillis();
    arena.updateCampfireHologram();
  }

  public int getRemainingEmergencyMeetings() {
    return remainingEmergencyMeetings;
  }

  public int getEmergencyCooldownRemaining() {
    if(remainingEmergencyMeetings <= 0 || lastVoteEndTime <= 0 || isVotingActive()) {
      return 0;
    }
    long cooldownMs = arena.getPlugin().getConfig().getInt("Voting.Cooldown", 20) * 1000L;
    long elapsedMs = System.currentTimeMillis() - lastVoteEndTime;
    if(elapsedMs >= cooldownMs) {
      return 0;
    }
    return Math.max(1, (int) Math.ceil((cooldownMs - elapsedMs) / 1000.0));
  }

  private void broadcastArenaMessage(String message) {
    Component comp = HalloweenChat.component(message);
    for(Player p : arena.getPlayers()) {
      p.sendMessage(comp);
    }
  }

  public void registerDeadBody(String victimName, Location location, List<ArmorStand> stands, TextDisplay textDisplay, ArmorStandHologram hologram) {
    registerDeadBody(victimName, location, stands, textDisplay, hologram, null);
  }

  public void registerDeadBody(String victimName, Location location, List<ArmorStand> stands, TextDisplay textDisplay, ArmorStandHologram hologram, Runnable cleanupAction) {
    activeBodies.add(new DeadBody(victimName, location, stands, textDisplay, hologram, cleanupAction));
  }

  public List<DeadBody> getActiveBodies() {
    return activeBodies;
  }

  public void recordKill(Player killer) {
    if(killer != null) {
      recentKills.put(killer.getUniqueId(), System.currentTimeMillis());
    }
  }

  public boolean isOnKillReportCooldown(Player player) {
    if(player == null) {
      return false;
    }
    Long lastKill = recentKills.get(player.getUniqueId());
    return lastKill != null && (System.currentTimeMillis() - lastKill) < KILLER_REPORT_COOLDOWN_MS;
  }

  public DeadBody findNearbyBody(Location loc, double maxDistance) {
    if(loc == null || loc.getWorld() == null) {
      return null;
    }
    long now = System.currentTimeMillis();
    double maxDistSq = maxDistance * maxDistance;
    DeadBody closest = null;
    double bestDistSq = maxDistSq;
    for(DeadBody body : activeBodies) {
      if(now - body.getSpawnTimeMs() < BODY_SPAWN_GRACE_MS) {
        continue;
      }
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
    if(player == null || isVotingActive() || isOnKillReportCooldown(player)) {
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
        td.text(HalloweenChat.component(
          "<#FFB703>" + player.getName() + "\n<shadow:#000000FF><#5A4E7C>(</shadow><#B8B0C8>กำลังปรึกษาหารือ<shadow:#000000FF><#5A4E7C>)</shadow>"));
      } else if(voteState == VoteState.VOTING) {
        boolean locked = hasLockedVote(player);
        td.text(HalloweenChat.component(
          "<#FFB703>" + player.getName() + "\n"
            + (locked ? "<shadow:#000000FF><#5A4E7C>[</shadow><#6EEB83>โหวตแล้ว<shadow:#000000FF><#5A4E7C>]</shadow>" : "<shadow:#000000FF><#5A4E7C>(</shadow><#B8B0C8>กำลังตัดสินใจ<shadow:#000000FF><#5A4E7C>)</shadow>")));
      }
    }
  }

  public ItemStack createVoteMenuItem() {
    ItemStack item = XMaterial.NETHER_STAR.parseItem();
    ItemMeta meta = item.getItemMeta();
    if(meta != null) {
      HalloweenChat.setItemName(meta, "<shadow:#000000FF><#5A4E7C>[</shadow><#FF7518>Voting<shadow:#000000FF><#5A4E7C>]</shadow>");
      HalloweenChat.setItemLore(meta, Arrays.asList(
        "",
        "<#F4F1DE>เมนูโหวตหาคนร้าย",
        "",
        "<shadow:#000000FF><#5A4E7C>(</shadow><#B8B0C8>กดคลิกขวาเพื่อเปิดหน้าต่างโหวต<shadow:#000000FF><#5A4E7C>)</shadow>"
      ));
      item.setItemMeta(meta);
    }
    return item;
  }

  public boolean isVoteMenuItem(ItemStack item) {
    if(item == null || !item.hasItemMeta() || !item.getItemMeta().hasDisplayName()) {
      return false;
    }
    String stripped = HalloweenChat.strip(item.getItemMeta().getDisplayName());
    return stripped.contains("Voting") || stripped.contains("Bus Horn") || stripped.contains("Emergency Campfire") || stripped.contains("เปิดเมนูโหวตหาคนร้าย");
  }

  private void playBusHornSound() {
    musicManager.stopAll();
    for(Player player : arena.getPlayers()) {
      if(player != null && player.isOnline()) {
        try {
          player.playSound(player.getLocation(), "block.bell.use", 1.0F, 1.45F);
          player.playSound(player.getLocation(), "block.note_block.bell", 0.9F, 1.335F);
          player.playSound(player.getLocation(), "item.goat_horn.sound.0", 0.8F, 1.65F);
          player.playSound(player.getLocation(), "block.note_block.didgeridoo", 1.0F, 1.335F);
          player.playSound(player.getLocation(), "block.note_block.didgeridoo", 1.0F, 1.587F);
        } catch(Throwable ignored) {
        }
      }
    }
    Bukkit.getScheduler().runTaskLater(arena.getPlugin(), () -> {
      for(Player player : arena.getPlayers()) {
        if(player != null && player.isOnline()) {
          try {
            player.stopSound("item.goat_horn.sound.0");
          } catch(Throwable ignored) {
          }
        }
      }
    }, 4L);
    Bukkit.getScheduler().runTaskLater(arena.getPlugin(), () -> {
      for(Player player : arena.getPlayers()) {
        if(player != null && player.isOnline()) {
          try {
            player.playSound(player.getLocation(), "block.bell.use", 1.0F, 1.55F);
            player.playSound(player.getLocation(), "block.note_block.bell", 0.9F, 1.498F);
            player.playSound(player.getLocation(), "item.goat_horn.sound.0", 0.85F, 1.65F);
            player.playSound(player.getLocation(), "block.note_block.didgeridoo", 1.0F, 1.335F);
            player.playSound(player.getLocation(), "block.note_block.didgeridoo", 1.0F, 1.587F);
          } catch(Throwable ignored) {
          }
        }
      }
    }, 6L);
    Bukkit.getScheduler().runTaskLater(arena.getPlugin(), () -> {
      for(Player player : arena.getPlayers()) {
        if(player != null && player.isOnline()) {
          try {
            player.playSound(player.getLocation(), "block.note_block.didgeridoo", 0.85F, 1.335F);
            player.playSound(player.getLocation(), "block.note_block.didgeridoo", 0.85F, 1.587F);
          } catch(Throwable ignored) {
          }
        }
      }
    }, 10L);
    Bukkit.getScheduler().runTaskLater(arena.getPlugin(), () -> {
      for(Player player : arena.getPlayers()) {
        if(player != null && player.isOnline()) {
          try {
            player.stopSound("item.goat_horn.sound.0");
          } catch(Throwable ignored) {
          }
        }
      }
    }, 13L);
  }

  public boolean startVoting(Player reportingPlayer) {
    return startVoting(reportingPlayer, null);
  }

  private Location resolveMeetingCenter() {
    Location center = arena.getEffectiveMeetingLocation();
    if(center == null && !arena.getPlayerSpawnPoints().isEmpty()) {
      center = arena.getPlayerSpawnPoints().get(0);
    }
    if(center == null) {
      center = arena.getStartLocation();
    }
    return center;
  }

  public boolean startVoting(Player reportingPlayer, String victimName) {
    if(voteState != VoteState.NONE || arena.getArenaState() != IArenaState.IN_GAME || arena.getCutsceneManager().isPlaying()) {
      return false;
    }

    Location center = resolveMeetingCenter();
    if(center == null || center.getWorld() == null) {
      return false;
    }

    if(victimName == null) {
      if(arena.getSabotageManager() != null && arena.getSabotageManager().hasActiveSabotage()) {
        XSound.ENTITY_VILLAGER_NO.play(reportingPlayer.getLocation(), 0.8F, 1.0F);
        HalloweenChat.send(reportingPlayer,
          "<shadow:#000000FF><#5A4E7C>[</shadow><#FF7518>Bus Horn<shadow:#000000FF><#5A4E7C>]</shadow> <#FF3B47>ไม่สามารถบีบแตรเรียกประชุมฉุกเฉินได้ขณะเกิดการก่อวินาศกรรม!");
        arena.updateCampfireHologram();
        return false;
      }
      if(remainingEmergencyMeetings <= 0) {
        XSound.ENTITY_VILLAGER_NO.play(reportingPlayer.getLocation(), 0.8F, 1.0F);
        HalloweenChat.send(reportingPlayer,
          "<shadow:#000000FF><#5A4E7C>[</shadow><#FF7518>Bus Horn<shadow:#000000FF><#5A4E7C>]</shadow> <#FF3B47>ไม่สามารถบีบแตรเรียกประชุมฉุกเฉินได้อีก (ใช้ครบจำนวนรอบแล้ว)");
        arena.updateCampfireHologram();
        return false;
      }
      int remainSec = getEmergencyCooldownRemaining();
      if(remainSec > 0) {
        XSound.ENTITY_VILLAGER_NO.play(reportingPlayer.getLocation(), 0.8F, 1.0F);
        HalloweenChat.send(reportingPlayer,
          "<shadow:#000000FF><#5A4E7C>[</shadow><#FF7518>Bus Horn<shadow:#000000FF><#5A4E7C>]</shadow> <#FF3B47>แตรรถบัสยังติดคูลดาวน์อยู่ กรุณารออีก <#FFB703>" + remainSec + " วิ");
        arena.updateCampfireHologram();
        return false;
      }
      remainingEmergencyMeetings--;
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
    this.pendingEjectedName = null;
    this.pendingEjectedWasMurderer = false;
    this.verdictExecuted = false;
    clearCircleStatusDisplays();

    if(arena.getSabotageManager() != null) {
      arena.getSabotageManager().cancelActiveSabotage("VOTING");
    }

    for(Player p : arena.getPlayers()) {
      arena.getTaskManager().getMinigameManager().handleClose(p, null);
      p.closeInventory();
    }

    clearDeadBodies();

    if(victimName != null) {
      musicManager.playBodyReportSound();
      broadcastArenaMessage("<shadow:#000000FF><#5A4E7C>[</shadow><#FF3B47>Dead Body<shadow:#000000FF><#5A4E7C>]</shadow> <#FFB703>" + reportingPlayer.getName()
        + " <#F4F1DE>พบศพของ <#FF3B47>" + victimName + " <#F4F1DE>และเรียกประชุมด่วน");
    } else {
      playBusHornSound();
      broadcastArenaMessage("<shadow:#000000FF><#5A4E7C>[</shadow><#FF7518>Bus Horn<shadow:#000000FF><#5A4E7C>]</shadow> <#FFB703>" + reportingPlayer.getName()
        + " <#F4F1DE>บีบแตรรถบัสเรียกประชุมฉุกเฉิน");
    }

    List<Player> alivePlayers = arena.getPlayersLeft();
    int count = Math.max(1, alivePlayers.size());
    double radius = 2.8;
    double cx = center.getBlockX() + 0.5;
    double cz = center.getBlockZ() + 0.5;
    double cy = center.getBlockY();

    for(int i = 0; i < alivePlayers.size(); i++) {
      Player player = alivePlayers.get(i);
      Location preLoc;
      if(arena.getVentManager() != null && arena.getVentManager().isInsideVent(player)) {
        Location safeExit = arena.getVentManager().forceExitForMeeting(player);
        preLoc = safeExit != null ? safeExit.clone() : center.clone();
      } else {
        preLoc = player.getLocation().clone();
      }
      preVoteLocations.put(player, preLoc);

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
        "<#FFB703>" + player.getName() + "\n<shadow:#000000FF><#5A4E7C>(</shadow><#B8B0C8>กำลังปรึกษาหารือ<shadow:#000000FF><#5A4E7C>)</shadow>"
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
        HalloweenChat.sendTitles(player,
          "<shadow:#000000FF><#5A4E7C>[</shadow><#FF3B47>Dead Body<shadow:#000000FF><#5A4E7C>]</shadow>",
          "<#FFB703>" + reportingPlayer.getName() + " <#F4F1DE>พบศพของ <#FF3B47>" + victimName,
          10, 60, 10);
      } else {
        HalloweenChat.sendTitles(player,
          "<shadow:#000000FF><#5A4E7C>[</shadow><#FF7518>Bus Horn<shadow:#000000FF><#5A4E7C>]</shadow>",
          "<#FFB703>" + reportingPlayer.getName() + " <#F4F1DE>บีบแตรรถบัสเรียกประชุมฉุกเฉิน",
          10, 60, 10);
      }
    }

    for(Player player : arena.getPlayers()) {
      if(ArenaUtils.isSpectator(player, arena) && center != null) {
        VersionUtils.teleport(player, center.clone().add(0.5, 2.5, 0.5)).thenAccept(ignored ->
          Bukkit.getScheduler().runTaskLater(arena.getPlugin(), () -> ArenaUtils.applySpectatorState(player, arena, true), 1L));
      }
    }

    voteState = VoteState.DISCUSSION;
    voteTimer = arena.getPlugin().getConfig().getInt("Voting.Discussion-Time", 15);
    arena.updateCampfireHologram();

    String meetingPrefix = victimName != null
      ? "<shadow:#000000FF><#5A4E7C>[</shadow><#FF3B47>Dead Body<shadow:#000000FF><#5A4E7C>]</shadow>"
      : "<shadow:#000000FF><#5A4E7C>[</shadow><#FF7518>Bus Horn<shadow:#000000FF><#5A4E7C>]</shadow>";
    broadcastArenaMessage(meetingPrefix + " <#F4F1DE>เริ่มเวลาปรึกษาหารือ <#FFB703>"
      + voteTimer + " วิ <shadow:#000000FF><#5A4E7C>(</shadow><#B8B0C8>พูดคุยในแชทเพื่อหาตัวคนร้าย<shadow:#000000FF><#5A4E7C>)</shadow>");

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

  private void triggerMurdererMeetingWin() {
    if(arena.aliveMurderer() > 0) {
      String winTitle = "<shadow:#000000FF><#5A4E7C>[</shadow><#FF3B47>ฆาตกรเป็นฝ่ายชนะ<shadow:#000000FF><#5A4E7C>]</shadow>";
      String winSub = "<#B8B0C8>ผู้โดยสารทั้งหมดถูกสังหารแล้ว";
      for(Player p : arena.getPlayers()) {
        HalloweenChat.sendTitles(p, winTitle, winSub, 10, 60, 10);
      }
    }
    Bukkit.getScheduler().runTask(arena.getPlugin(), () -> arena.getPlugin().getArenaManager().stopGame(false, arena));
  }

  public boolean tick() {
    if(voteState == VoteState.NONE || arena.getArenaState() != IArenaState.IN_GAME) {
      return false;
    }

    if(voteState == VoteState.DISCUSSION || voteState == VoteState.VOTING) {
      if(arena.aliveMurderer() <= 0) {
        ArenaUtils.onMurdererDeath(arena);
        return true;
      }
      if(arena.getPlayersLeft().size() <= arena.aliveMurderer()) {
        triggerMurdererMeetingWin();
        return true;
      }
    }

    voteTimer--;

    switch(voteState) {
      case DISCUSSION:
        if(voteTimer <= 0) {
          voteState = VoteState.VOTING;
          voteTimer = arena.getPlugin().getConfig().getInt("Voting.Voting-Time", 30);
          updateCircleStatusDisplays();
          openVotingGUIForAll();

          broadcastArenaMessage("<shadow:#000000FF><#5A4E7C>[</shadow><#FF7518>Voting<shadow:#000000FF><#5A4E7C>]</shadow> <#F4F1DE>เปิดให้ลงคะแนนโหวตแล้ว เวลาตัดสินใจ <#FFB703>"
            + voteTimer + " วิ");
        } else {
          refreshOpenVotingGUIs();
          String discPrefix = reportedVictimName != null
            ? "<shadow:#000000FF><#5A4E7C>[</shadow><#FF3B47>Dead Body<shadow:#000000FF><#5A4E7C>]</shadow>"
            : "<shadow:#000000FF><#5A4E7C>[</shadow><#FF7518>Bus Horn<shadow:#000000FF><#5A4E7C>]</shadow>";
          String barMsg = discPrefix + " <#F4F1DE>เวลาปรึกษา <#FFB703>"
            + voteTimer + " วิ <shadow:#000000FF><#5A4E7C>(</shadow><#B8B0C8>คลิกขวาไอเทมในมือเพื่อดูรายชื่อ<shadow:#000000FF><#5A4E7C>)</shadow>";
          for(Player player : arena.getPlayersLeft()) {
            HalloweenChat.sendActionBar(player, barMsg);
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
          String barMsg = "<shadow:#000000FF><#5A4E7C>[</shadow><#FF7518>Voting<shadow:#000000FF><#5A4E7C>]</shadow> <#F4F1DE>เหลือ <#FFB703>"
            + voteTimer + " วิ <shadow:#000000FF><#5A4E7C>|</shadow> <#6EEB83>โหวตแล้ว " + totalVoted + "/" + totalAlive
            + " <shadow:#000000FF><#5A4E7C>(</shadow><#B8B0C8>คลิกขวาไอเทมในมือเพื่อเปิดเมนูโหวต<shadow:#000000FF><#5A4E7C>)</shadow>";
          for(Player player : arena.getPlayersLeft()) {
            if(voteTimer <= 5) {
              XSound.BLOCK_NOTE_BLOCK_HAT.play(player.getLocation(), 0.6F, 1.6F);
            }
            HalloweenChat.sendActionBar(player, barMsg);
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
          if(arena.aliveMurderer() <= 0) {
            ArenaUtils.onMurdererDeath(arena);
            return true;
          }
          if(arena.getPlayersLeft().size() <= arena.aliveMurderer()) {
            triggerMurdererMeetingWin();
            return true;
          }
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
    if(ArenaUtils.isSpectator(viewer, arena) || !arena.getPlayersLeft().contains(viewer)) {
      return;
    }
    Inventory gui;
    try {
      gui = Bukkit.createInventory(null, GUI_SIZE, HalloweenChat.component(VOTE_GUI_RAW_TITLE));
    } catch(Throwable t) {
      gui = Bukkit.createInventory(null, GUI_SIZE, VOTE_GUI_TITLE);
    }
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

  private static int[] getCenteredRowColumns(int countInRow) {
    switch(countInRow) {
      case 1:
        return new int[]{4};
      case 2:
        return new int[]{3, 5};
      case 3:
        return new int[]{3, 4, 5};
      case 4:
        return new int[]{2, 3, 5, 6};
      case 5:
        return new int[]{2, 3, 4, 5, 6};
      case 6:
        return new int[]{1, 2, 3, 5, 6, 7};
      default:
        return new int[]{1, 2, 3, 4, 5, 6, 7};
    }
  }

  private static int[] computePlayerSlots(int totalPlayers) {
    int count = Math.max(0, Math.min(27, totalPlayers));
    if(count == 0) {
      return new int[0];
    }
    if(count > 21) {
      int[] fullGrid = new int[count];
      for(int i = 0; i < count; i++) {
        fullGrid[i] = 9 + i;
      }
      return fullGrid;
    }
    int[] result = new int[count];
    int idx = 0;
    if(count <= 7) {
      for(int col : getCenteredRowColumns(count)) {
        result[idx++] = 18 + col;
      }
    } else if(count <= 14) {
      int topCount = (count + 1) / 2;
      int midCount = count - topCount;
      for(int col : getCenteredRowColumns(topCount)) {
        result[idx++] = 9 + col;
      }
      for(int col : getCenteredRowColumns(midCount)) {
        result[idx++] = 18 + col;
      }
    } else {
      int r1 = Math.min(7, (count + 2) / 3);
      int rem = count - r1;
      int r2 = Math.min(7, (rem + 1) / 2);
      int r3 = rem - r2;
      for(int col : getCenteredRowColumns(r1)) {
        result[idx++] = 9 + col;
      }
      for(int col : getCenteredRowColumns(r2)) {
        result[idx++] = 18 + col;
      }
      for(int col : getCenteredRowColumns(r3)) {
        result[idx++] = 27 + col;
      }
    }
    return result;
  }

  private void populateVotingGUI(Inventory gui, Player viewer) {
    gui.clear();

    ItemStack borderPane = XMaterial.BLACK_STAINED_GLASS_PANE.parseItem();
    ItemMeta bMeta = borderPane.getItemMeta();
    if(bMeta != null) {
      bMeta.setDisplayName(" ");
      try {
        bMeta.setHideTooltip(true);
      } catch(Throwable ignored) {
      }
      borderPane.setItemMeta(bMeta);
    }

    for(int slot : BORDER_SLOTS) {
      if(slot < gui.getSize()) {
        gui.setItem(slot, borderPane);
      }
    }

    List<Player> alivePlayers = arena.getPlayersLeft();
    int totalVoted = votes.size() + skipVotes.size();
    boolean isResultPhase = (voteState == VoteState.RESULT);

    ItemStack meetingInfo = (reportedVictimName != null ? XMaterial.SKELETON_SKULL : XMaterial.BELL).parseItem();
    if(meetingInfo == null) {
      meetingInfo = new ItemStack(org.bukkit.Material.BELL);
    }
    ItemMeta cMeta = meetingInfo.getItemMeta();
    if(cMeta != null) {
      HalloweenChat.setItemName(cMeta,
        reportedVictimName != null
          ? "<shadow:#000000FF><#5A4E7C>[</shadow><#FF3B47>Dead Body<shadow:#000000FF><#5A4E7C>]</shadow>"
          : "<shadow:#000000FF><#5A4E7C>[</shadow><#FF7518>Bus Horn<shadow:#000000FF><#5A4E7C>]</shadow>"
      );
      List<String> infoLore = new ArrayList<>();
      infoLore.add("");
      if(reportedVictimName != null) {
        infoLore.add("<#F4F1DE>ร่างของ <#FF3B47>" + reportedVictimName);
      }
      if(reporter != null) {
        infoLore.add("<#F4F1DE>ผู้เรียกประชุม <#FFB703>" + reporter.getName());
      }
      if(voteState == VoteState.DISCUSSION) {
        infoLore.add("<#F4F1DE>เปิดโหวตใน <#FFB703>" + voteTimer + " วิ");
      } else if(voteState == VoteState.VOTING) {
        infoLore.add("<#F4F1DE>โหวตแล้ว <#6EEB83>" + totalVoted + "<shadow:#000000FF><#5A4E7C>/</shadow><#F4F1DE>" + alivePlayers.size() + " คน");
        infoLore.add("<#FF3B47>เหลือเวลา " + voteTimer + " วิ");
      } else {
        infoLore.add("<#FFB703>สรุปผลคะแนนโหวต");
        infoLore.add("<#F4F1DE>โหวตข้าม <#72EFDD>" + skipVotes.size() + " <#F4F1DE>คน");
      }
      infoLore.add("");
      if(voteState == VoteState.DISCUSSION) {
        infoLore.add("<shadow:#000000FF><#5A4E7C>(</shadow><#B8B0C8>กำลังอยู่ในช่วงปรึกษาหารือ<shadow:#000000FF><#5A4E7C>)</shadow>");
      } else if(voteState == VoteState.VOTING) {
        infoLore.add("<shadow:#000000FF><#5A4E7C>(</shadow><#B8B0C8>คลิกหัวผู้เล่นเพื่อลงคะแนนโหวต<shadow:#000000FF><#5A4E7C>)</shadow>");
      } else {
        infoLore.add("<shadow:#000000FF><#5A4E7C>(</shadow><#B8B0C8>กำลังดำเนินการตามผลโหวต<shadow:#000000FF><#5A4E7C>)</shadow>");
      }
      HalloweenChat.setItemLore(cMeta, infoLore);
      meetingInfo.setItemMeta(cMeta);
    }
    gui.setItem(INFO_SLOT, meetingInfo);

    IUser viewerUser = arena.getPlugin().getUserManager().getUser(viewer);
    boolean viewerIsMurderer = Role.isRole(Role.MURDERER, viewerUser, arena);
    boolean viewerLocked = hasLockedVote(viewer);
    Player viewerLockedTarget = votes.get(viewer);
    boolean viewerLockedSkip = skipVotes.contains(viewer);
    Player viewerPendingTarget = pendingVotes.get(viewer);
    boolean viewerPendingSkip = pendingSkips.contains(viewer);

    int[] playerSlots = computePlayerSlots(alivePlayers.size());
    for(int idx = 0; idx < alivePlayers.size() && idx < playerSlots.length; idx++) {
      Player target = alivePlayers.get(idx);
      int slot = playerSlots[idx];

      ItemStack head = XMaterial.PLAYER_HEAD.parseItem();
      SkullMeta meta = (SkullMeta) head.getItemMeta();
      if(meta != null) {
        meta = VersionUtils.setPlayerHead(target, meta);

        boolean targetHasVoted = hasLockedVote(target);
        IUser targetUser = arena.getPlugin().getUserManager().getUser(target);
        boolean isMurdererTeammate = viewerIsMurderer && Role.isRole(Role.MURDERER, targetUser, arena);

        HalloweenChat.setItemName(meta,
          isMurdererTeammate
            ? "<shadow:#000000FF><#5A4E7C>[</shadow><#FF3B47>" + target.getName() + "<shadow:#000000FF><#5A4E7C>]</shadow>"
            : "<shadow:#000000FF><#5A4E7C>[</shadow><#FFB703>" + target.getName() + "<shadow:#000000FF><#5A4E7C>]</shadow>"
        );

        List<String> lore = new ArrayList<>();
        lore.add("");
        if(isMurdererTeammate) {
          lore.add("<#FF3B47>ฆาตกร");
        }
        if(target.equals(reporter)) {
          lore.add("<#FF7518>ผู้เรียกประชุม");
        }
        lore.add(targetHasVoted ? "<#6EEB83>โหวตแล้ว" : "<#B8B0C8>ยังไม่ได้โหวต");

        if(isResultPhase) {
          List<String> votersForTarget = new ArrayList<>();
          for(Map.Entry<Player, Player> entry : votes.entrySet()) {
            if(entry.getValue().equals(target)) {
              votersForTarget.add(entry.getKey().getName());
            }
          }
          lore.add("<#F4F1DE>ได้รับ <#FFB703>" + votersForTarget.size() + " <#F4F1DE>โหวต");
          for(String voterName : votersForTarget) {
            lore.add("<shadow:#000000FF><#5A4E7C>- </shadow><#B8B0C8>" + voterName);
          }
        }

        lore.add("");
        if(target.equals(viewerLockedTarget)) {
          lore.add("<shadow:#000000FF><#5A4E7C>(</shadow><#6EEB83>คุณโหวตผู้เล่นคนนี้แล้ว<shadow:#000000FF><#5A4E7C>)</shadow>");
        } else if(target.equals(viewerPendingTarget)) {
          lore.add("<shadow:#000000FF><#5A4E7C>(</shadow><#FFB703>กดคลิกซ้ำเพื่อยืนยันโหวต<shadow:#000000FF><#5A4E7C>)</shadow>");
        } else if(viewerLocked) {
          lore.add("<shadow:#000000FF><#5A4E7C>(</shadow><#B8B0C8>คุณลงคะแนนโหวตไปแล้ว<shadow:#000000FF><#5A4E7C>)</shadow>");
        } else if(voteState == VoteState.DISCUSSION) {
          lore.add("<shadow:#000000FF><#5A4E7C>(</shadow><#B8B0C8>รอเปิดโหวตอีก " + voteTimer + " วิ<shadow:#000000FF><#5A4E7C>)</shadow>");
        } else if(voteState == VoteState.VOTING) {
          lore.add("<shadow:#000000FF><#5A4E7C>(</shadow><#B8B0C8>กดคลิกเพื่อโหวตผู้เล่นนี้<shadow:#000000FF><#5A4E7C>)</shadow>");
        } else {
          lore.add("<shadow:#000000FF><#5A4E7C>(</shadow><#B8B0C8>หมดเวลาลงคะแนนโหวต<shadow:#000000FF><#5A4E7C>)</shadow>");
        }

        HalloweenChat.setItemLore(meta, lore);
        head.setItemMeta(meta);
      }
      if(target.equals(viewerLockedTarget) || target.equals(viewerPendingTarget)) {
        applyGlow(head);
      }
      gui.setItem(slot, head);
    }

    boolean skipHighlighted = viewerLockedSkip || viewerPendingSkip;
    ItemStack skipItem = skipHighlighted ? XMaterial.LIME_DYE.parseItem() : XMaterial.FEATHER.parseItem();
    ItemMeta skipMeta = skipItem.getItemMeta();
    if(skipMeta != null) {
      HalloweenChat.setItemName(skipMeta,
        viewerLockedSkip
          ? "<shadow:#000000FF><#5A4E7C>[</shadow><#6EEB83>Skip Vote<shadow:#000000FF><#5A4E7C>]</shadow>"
          : (viewerPendingSkip ? "<shadow:#000000FF><#5A4E7C>[</shadow><#FFB703>Skip Vote<shadow:#000000FF><#5A4E7C>]</shadow>" : "<shadow:#000000FF><#5A4E7C>[</shadow><#72EFDD>Skip Vote<shadow:#000000FF><#5A4E7C>]</shadow>")
      );
      List<String> skipLore = new ArrayList<>();
      skipLore.add("");
      skipLore.add("<#F4F1DE>ข้ามการโหวตในรอบนี้");
      if(isResultPhase) {
        skipLore.add("<#F4F1DE>โหวตข้าม <#FFB703>" + skipVotes.size() + " <#F4F1DE>คน");
        for(Player skipper : skipVotes) {
          skipLore.add("<shadow:#000000FF><#5A4E7C>- </shadow><#B8B0C8>" + skipper.getName());
        }
      }
      skipLore.add("");
      if(viewerLockedSkip) {
        skipLore.add("<shadow:#000000FF><#5A4E7C>(</shadow><#6EEB83>คุณโหวตข้ามแล้ว<shadow:#000000FF><#5A4E7C>)</shadow>");
      } else if(viewerPendingSkip) {
        skipLore.add("<shadow:#000000FF><#5A4E7C>(</shadow><#FFB703>กดคลิกซ้ำเพื่อยืนยัน<shadow:#000000FF><#5A4E7C>)</shadow>");
      } else if(viewerLocked) {
        skipLore.add("<shadow:#000000FF><#5A4E7C>(</shadow><#B8B0C8>คุณลงคะแนนโหวตไปแล้ว<shadow:#000000FF><#5A4E7C>)</shadow>");
      } else if(voteState == VoteState.DISCUSSION) {
        skipLore.add("<shadow:#000000FF><#5A4E7C>(</shadow><#B8B0C8>รอเปิดโหวตอีก " + voteTimer + " วิ<shadow:#000000FF><#5A4E7C>)</shadow>");
      } else if(voteState == VoteState.VOTING) {
        skipLore.add("<shadow:#000000FF><#5A4E7C>(</shadow><#B8B0C8>กดคลิกเพื่อข้ามการโหวต<shadow:#000000FF><#5A4E7C>)</shadow>");
      } else {
        skipLore.add("<shadow:#000000FF><#5A4E7C>(</shadow><#B8B0C8>หมดเวลาลงคะแนนโหวต<shadow:#000000FF><#5A4E7C>)</shadow>");
      }
      HalloweenChat.setItemLore(skipMeta, skipLore);
      skipItem.setItemMeta(skipMeta);
    }
    if(skipHighlighted) {
      applyGlow(skipItem);
    }
    gui.setItem(SKIP_SLOT, skipItem);

    if(voteState == VoteState.VOTING && !viewerLocked && (viewerPendingTarget != null || viewerPendingSkip)) {
      String choiceLabel = viewerPendingSkip ? "ข้ามการโหวต" : viewerPendingTarget.getName();

      ItemStack confirmBtn = XMaterial.LIME_CONCRETE.parseItem();
      ItemMeta confirmMeta = confirmBtn.getItemMeta();
      if(confirmMeta != null) {
        HalloweenChat.setItemName(confirmMeta, "<shadow:#000000FF><#5A4E7C>[</shadow><#6EEB83>Confirm Vote<shadow:#000000FF><#5A4E7C>]</shadow>");
        HalloweenChat.setItemLore(confirmMeta, Arrays.asList(
          "",
          "<#F4F1DE>ยืนยันโหวต <#FFB703>" + choiceLabel,
          "",
          "<shadow:#000000FF><#5A4E7C>(</shadow><#B8B0C8>กดคลิกเพื่อล็อกผลโหวต<shadow:#000000FF><#5A4E7C>)</shadow>"
        ));
        confirmBtn.setItemMeta(confirmMeta);
      }
      applyGlow(confirmBtn);
      gui.setItem(CONFIRM_SLOT, confirmBtn);

      ItemStack cancelBtn = XMaterial.RED_CONCRETE.parseItem();
      ItemMeta cancelMeta = cancelBtn.getItemMeta();
      if(cancelMeta != null) {
        HalloweenChat.setItemName(cancelMeta, "<shadow:#000000FF><#5A4E7C>[</shadow><#FF3B47>Cancel<shadow:#000000FF><#5A4E7C>]</shadow>");
        HalloweenChat.setItemLore(cancelMeta, Arrays.asList(
          "",
          "<#F4F1DE>ยกเลิกตัวเลือก <#FFB703>" + choiceLabel,
          "",
          "<shadow:#000000FF><#5A4E7C>(</shadow><#B8B0C8>กดคลิกเพื่อเลือกใหม่<shadow:#000000FF><#5A4E7C>)</shadow>"
        ));
        cancelBtn.setItemMeta(cancelMeta);
      }
      gui.setItem(CANCEL_SELECT_SLOT, cancelBtn);
    }

    ItemStack closeChat = XMaterial.OAK_SIGN.parseItem();
    ItemMeta closeMeta = closeChat.getItemMeta();
    if(closeMeta != null) {
      HalloweenChat.setItemName(closeMeta, "<shadow:#000000FF><#5A4E7C>[</shadow><#FF7518>Chat<shadow:#000000FF><#5A4E7C>]</shadow>");
      HalloweenChat.setItemLore(closeMeta, Arrays.asList(
        "",
        "<#F4F1DE>ปิดหน้าต่างเพื่อพิมพ์แชท",
        "",
        "<shadow:#000000FF><#5A4E7C>(</shadow><#B8B0C8>กดคลิกเพื่อซ่อนหน้าต่างโหวต<shadow:#000000FF><#5A4E7C>)</shadow>"
      ));
      closeChat.setItemMeta(closeMeta);
    }
    gui.setItem(CHAT_CLOSE_SLOT, closeChat);
  }

  public boolean handleVoteClick(Player voter, int rawSlot) {
    if(ArenaUtils.isSpectator(voter, arena) || !arena.getPlayersLeft().contains(voter)) {
      voter.closeInventory();
      return true;
    }
    if(rawSlot == CHAT_CLOSE_SLOT) {
      voter.closeInventory();
      XSound.UI_BUTTON_CLICK.play(voter.getLocation(), 0.7F, 1.2F);
      HalloweenChat.sendActionBar(voter, "<shadow:#000000FF><#5A4E7C>[</shadow><#FF7518>Voting<shadow:#000000FF><#5A4E7C>]</shadow> <#6EEB83>ปิดหน้าต่างโหวตชั่วคราว <shadow:#000000FF><#5A4E7C>(</shadow><#B8B0C8>คลิกขวาไอเทมในมือเพื่อเปิดเมนูโหวตอีกครั้ง<shadow:#000000FF><#5A4E7C>)</shadow>");
      return true;
    }

    if(voteState == VoteState.DISCUSSION) {
      XSound.ENTITY_VILLAGER_NO.play(voter.getLocation(), 0.8F, 1.0F);
      HalloweenChat.sendActionBar(voter, "<shadow:#000000FF><#5A4E7C>[</shadow><#FF7518>Voting<shadow:#000000FF><#5A4E7C>]</shadow> <#FF3B47>รอหมดเวลาปรึกษาอีก <#FFB703>" + voteTimer + " วิ");
      return true;
    }

    if(voteState != VoteState.VOTING) {
      return true;
    }

    if(hasLockedVote(voter)) {
      HalloweenChat.send(voter, "<shadow:#000000FF><#5A4E7C>[</shadow><#FF7518>Voting<shadow:#000000FF><#5A4E7C>]</shadow> <#FF3B47>คุณได้ยืนยันการลงคะแนนโหวตไปแล้ว");
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
      HalloweenChat.sendActionBar(voter, "<shadow:#000000FF><#5A4E7C>[</shadow><#FF7518>Voting<shadow:#000000FF><#5A4E7C>]</shadow> <#B8B0C8>ยกเลิกตัวเลือกแล้ว สามารถเลือกผู้เล่นใหม่ได้");
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
        HalloweenChat.sendActionBar(voter, "<shadow:#000000FF><#5A4E7C>[</shadow><#FF7518>Voting<shadow:#000000FF><#5A4E7C>]</shadow> <#F4F1DE>เลือก <#72EFDD>ข้ามการโหวต <shadow:#000000FF><#5A4E7C>(</shadow><#B8B0C8>คลิกซ้ำเพื่อยืนยัน<shadow:#000000FF><#5A4E7C>)</shadow>");
        refreshOpenVotingGUIs();
      }
      return true;
    }

    List<Player> alivePlayers = arena.getPlayersLeft();
    int[] playerSlots = computePlayerSlots(alivePlayers.size());
    int targetIdx = -1;
    for(int i = 0; i < playerSlots.length; i++) {
      if(playerSlots[i] == rawSlot) {
        targetIdx = i;
        break;
      }
    }

    if(targetIdx >= 0 && targetIdx < alivePlayers.size()) {
      Player target = alivePlayers.get(targetIdx);
      if(target.equals(pendingVotes.get(voter))) {
        confirmTargetVote(voter, target);
      } else {
        pendingSkips.remove(voter);
        pendingVotes.put(voter, target);
        XSound.UI_BUTTON_CLICK.play(voter.getLocation(), 0.9F, 1.3F);
        HalloweenChat.sendActionBar(voter, "<shadow:#000000FF><#5A4E7C>[</shadow><#FF7518>Voting<shadow:#000000FF><#5A4E7C>]</shadow> <#F4F1DE>เลือก <#FFB703>" + target.getName() + " <shadow:#000000FF><#5A4E7C>(</shadow><#B8B0C8>คลิกซ้ำเพื่อยืนยัน<shadow:#000000FF><#5A4E7C>)</shadow>");
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
    broadcastArenaMessage("<shadow:#000000FF><#5A4E7C>[</shadow><#FF7518>Voting<shadow:#000000FF><#5A4E7C>]</shadow> <#FFB703>" + voter.getName()
      + " <#F4F1DE>ลงคะแนนโหวตแล้ว <shadow:#000000FF><#5A4E7C>(</shadow><#B8B0C8>" + totalVoted + "/" + totalAlive + "<shadow:#000000FF><#5A4E7C>)</shadow>");
    HalloweenChat.send(voter, "<shadow:#000000FF><#5A4E7C>[</shadow><#FF7518>Voting<shadow:#000000FF><#5A4E7C>]</shadow> <#F4F1DE>คุณยืนยัน <#72EFDD>ข้ามการโหวต <#F4F1DE>เรียบร้อยแล้ว");

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
    broadcastArenaMessage("<shadow:#000000FF><#5A4E7C>[</shadow><#FF7518>Voting<shadow:#000000FF><#5A4E7C>]</shadow> <#FFB703>" + voter.getName()
      + " <#F4F1DE>ลงคะแนนโหวตแล้ว <shadow:#000000FF><#5A4E7C>(</shadow><#B8B0C8>" + totalVoted + "/" + totalAlive + "<shadow:#000000FF><#5A4E7C>)</shadow>");
    HalloweenChat.send(voter, "<shadow:#000000FF><#5A4E7C>[</shadow><#FF7518>Voting<shadow:#000000FF><#5A4E7C>]</shadow> <#F4F1DE>คุณยืนยันโหวตให้ <#FFB703>" + target.getName() + " <#F4F1DE>เรียบร้อยแล้ว");

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
    pendingEjectedName = null;
    pendingEjectedWasMurderer = false;

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
        String voteText = voters.isEmpty()
          ? "<shadow:#000000FF><#5A4E7C>(</shadow><#B8B0C8>0 โหวต<shadow:#000000FF><#5A4E7C>)</shadow>"
          : "<shadow:#000000FF><#5A4E7C>[</shadow><#FF3B47>" + voters.size() + " โหวต<shadow:#000000FF><#5A4E7C>]</shadow>";
        td.text(HalloweenChat.component(
          "<#FFB703>" + player.getName() + "\n" + voteText));
      }
    }

    Component header = HalloweenChat.component("<shadow:#000000FF><#5A4E7C>[</shadow><#FF7518>สรุปผลคะแนนโหวต<shadow:#000000FF><#5A4E7C>]</shadow>");
    for(Player p : arena.getPlayers()) {
      p.sendMessage(header);
      for(Player alive : arena.getPlayersLeft()) {
        List<String> voters = votersByTarget.getOrDefault(alive, new ArrayList<>());
        if(!voters.isEmpty()) {
          HalloweenChat.send(p, " <shadow:#000000FF><#5A4E7C>- </shadow><#FFB703>" + alive.getName() + " <#F4F1DE>ได้รับ <#FF3B47>" + voters.size() + " โหวต <shadow:#000000FF><#5A4E7C>(</shadow><#B8B0C8>" + String.join(", ", voters) + "<shadow:#000000FF><#5A4E7C>)</shadow>");
        }
      }
      if(skipCount > 0) {
        HalloweenChat.send(p, " <shadow:#000000FF><#5A4E7C>- </shadow><#72EFDD>ข้ามการโหวต <#F4F1DE>" + skipCount + " โหวต <shadow:#000000FF><#5A4E7C>(</shadow><#B8B0C8>" + String.join(", ", skipperNames) + "<shadow:#000000FF><#5A4E7C>)</shadow>");
      } else {
        HalloweenChat.send(p, " <shadow:#000000FF><#5A4E7C>- </shadow><#72EFDD>ข้ามการโหวต <#B8B0C8>0 โหวต");
      }
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
      pendingEjectedName = topCandidate.getName();
      IUser candUser = arena.getPlugin().getUserManager().getUser(topCandidate);
      pendingEjectedWasMurderer = candUser != null && Role.isRole(Role.MURDERER, candUser, arena);
    }

    String revealTitle = "<shadow:#000000FF><#5A4E7C>[</shadow><#FF7518>สรุปผลคะแนนโหวต<shadow:#000000FF><#5A4E7C>]</shadow>";
    String revealSub = pendingEjectedPlayer != null
      ? "<#F4F1DE>เสียงส่วนใหญ่ชี้ไปที่ <#FF3B47>" + pendingEjectedPlayer.getName() + " <shadow:#000000FF><#5A4E7C>(</shadow><#FFB703>" + maxVotes + " โหวต<shadow:#000000FF><#5A4E7C>)</shadow>"
      : "<#B8B0C8>คะแนนโหวตเสมอ หรือเสียงส่วนใหญ่เลือกข้าม";

    for(Player p : arena.getPlayers()) {
      XSound.BLOCK_NOTE_BLOCK_PLING.play(p.getLocation(), 1.0F, 1.2F);
      HalloweenChat.sendTitles(p, revealTitle, revealSub, 5, 45, 10);
    }
  }

  private void executeVerdict() {
    verdictExecuted = true;

    for(Player player : arena.getPlayersLeft()) {
      player.closeInventory();
    }

    if(pendingEjectedName == null) {
      broadcastArenaMessage("<shadow:#000000FF><#5A4E7C>[</shadow><#FF7518>Voting<shadow:#000000FF><#5A4E7C>]</shadow> <#F4F1DE>ไม่มีใครถูกโหวตคัดออกในรอบนี้ <shadow:#000000FF><#5A4E7C>(</shadow><#B8B0C8>คะแนนโหวตเสมอ หรือเสียงส่วนใหญ่เลือกข้าม<shadow:#000000FF><#5A4E7C>)</shadow>");

      String noEjectTitle = "<shadow:#000000FF><#5A4E7C>[</shadow><#FF7518>ไม่มีใครถูกโหวตออก<shadow:#000000FF><#5A4E7C>]</shadow>";
      String noEjectSub = "<#B8B0C8>คะแนนโหวตเสมอ หรือเสียงส่วนใหญ่เลือกข้าม";
      for(Player player : arena.getPlayers()) {
        XSound.BLOCK_NOTE_BLOCK_BASS.play(player.getLocation(), 1.0F, 0.8F);
        HalloweenChat.sendTitles(player, noEjectTitle, noEjectSub, 10, 45, 10);
      }
      return;
    }

    Player ejectedPlayer = pendingEjectedPlayer;
    boolean ejectedOnline = ejectedPlayer != null && ejectedPlayer.isOnline() && arena.getPlayers().contains(ejectedPlayer);
    boolean wasMurderer = pendingEjectedWasMurderer;
    if(ejectedOnline) {
      IUser ejectedUser = arena.getPlugin().getUserManager().getUser(ejectedPlayer);
      wasMurderer = Role.isRole(Role.MURDERER, ejectedUser, arena);
    }

    Location center = resolveMeetingCenter();
    if(center != null && center.getWorld() != null) {
      double cx = center.getBlockX() + 0.5;
      double cz = center.getBlockZ() + 0.5;
      double safeY = findSafeStandingY(center, cx, center.getBlockY(), cz);
      Location fireCenter = new Location(center.getWorld(), cx, safeY, cz);
      if(ejectedOnline) {
        meetingCircleLocations.put(ejectedPlayer.getUniqueId(), fireCenter);
        VersionUtils.teleport(ejectedPlayer, fireCenter);
      }
      try {
        center.getWorld().strikeLightningEffect(fireCenter);
      } catch(Throwable ignored) {
      }
    }

    String roleReveal = wasMurderer
      ? "<shadow:#000000FF><#5A4E7C>(</shadow><#6EEB83>เขาคือฆาตกร<shadow:#000000FF><#5A4E7C>)</shadow>"
      : "<shadow:#000000FF><#5A4E7C>(</shadow><#FF3B47>เขาไม่ใช่ฆาตกร<shadow:#000000FF><#5A4E7C>)</shadow>";

    broadcastArenaMessage("<shadow:#000000FF><#5A4E7C>[</shadow><#FF3B47>Voting<shadow:#000000FF><#5A4E7C>]</shadow> <#FFB703>" + pendingEjectedName
      + " <#F4F1DE>ถูกโหวตคัดออก " + roleReveal);

    String ejectTitle = "<shadow:#000000FF><#5A4E7C>[</shadow><#FF3B47>" + pendingEjectedName + " ถูกโหวตคัดออก<shadow:#000000FF><#5A4E7C>]</shadow>";
    for(Player player : arena.getPlayers()) {
      XSound.ENTITY_GENERIC_EXPLODE.play(player.getLocation(), 0.9F, 1.0F);
      HalloweenChat.sendTitles(player, ejectTitle, roleReveal, 10, 60, 10);
    }

    if(ejectedOnline) {
      restoreVoteMenuItem(ejectedPlayer);
      ejectedPlayer.damage(1000.0);
      if(!arena.isDeathPlayer(ejectedPlayer)) {
        ArenaUtils.forceEliminatePlayer(ejectedPlayer, arena);
      }
    }

    if(wasMurderer && arena.aliveMurderer() <= 0) {
      Bukkit.getScheduler().runTask(arena.getPlugin(), () -> arena.getPlugin().getArenaManager().stopGame(false, arena));
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
      if(ArenaUtils.isSpectator(player, arena)) {
        player.setWalkSpeed(0.2f);
        ArenaUtils.applySpectatorState(player, arena, false);
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
    pendingEjectedName = null;
    pendingEjectedWasMurderer = false;
    verdictExecuted = false;
    reporter = null;
    reportedVictimName = null;
    arena.updateCampfireHologram();
  }

  public boolean isPendingEjected(Player player) {
    return player != null && player.equals(pendingEjectedPlayer);
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
    if(title == null) {
      return false;
    }
    String stripped = HalloweenChat.strip(title);
    return VOTE_GUI_TITLE.equals(title) || stripped.contains("Voting") || stripped.contains("Bus Horn") || stripped.contains("Emergency Campfire") || stripped.contains("โหวตหาคนร้าย");
  }

  public void handlePlayerLeave(Player player) {
    restoreVoteMenuItem(player);
    player.setWalkSpeed(0.2f);
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
    if(isVotingActive()) {
      refreshOpenVotingGUIs();
    }
  }

  public void cleanup() {
    musicManager.stopAll();
    clearCircleStatusDisplays();
    for(Player player : arena.getPlayers()) {
      restoreVoteMenuItem(player);
      player.setWalkSpeed(0.2f);
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
    pendingEjectedName = null;
    pendingEjectedWasMurderer = false;
    verdictExecuted = false;
    reporter = null;
    reportedVictimName = null;
    lastVoteEndTime = 0;
    recentKills.clear();
    remainingEmergencyMeetings = arena.getPlugin().getConfig().getInt("Voting.Max-Emergency-Meetings", 2);
    arena.updateCampfireHologram();
  }
}
