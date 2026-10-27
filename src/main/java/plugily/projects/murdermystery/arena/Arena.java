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

package plugily.projects.murdermystery.arena;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;
import org.bukkit.util.Transformation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;
import plugily.projects.minigamesbox.api.arena.IArenaState;
import plugily.projects.minigamesbox.api.arena.IPluginArena;
import plugily.projects.minigamesbox.api.user.IUser;
import plugily.projects.minigamesbox.classic.arena.PluginArena;
import plugily.projects.minigamesbox.classic.arena.managers.BossbarManager;
import plugily.projects.minigamesbox.classic.arena.managers.PluginMapRestorerManager;
import plugily.projects.minigamesbox.classic.handlers.language.MessageBuilder;
import plugily.projects.minigamesbox.classic.utils.hologram.ArmorStandHologram;
import plugily.projects.minigamesbox.classic.utils.version.VersionUtils;
import plugily.projects.murdermystery.HookManager;
import plugily.projects.murdermystery.Main;
import plugily.projects.murdermystery.arena.corpse.Corpse;
import plugily.projects.murdermystery.arena.corpse.Stand;
import plugily.projects.murdermystery.arena.cutscene.CutsceneManager;
import plugily.projects.murdermystery.arena.managers.MapRestorerManager;
import plugily.projects.murdermystery.arena.managers.ScoreboardManager;
import plugily.projects.murdermystery.arena.map.ArenaMinimap;
import plugily.projects.murdermystery.arena.role.Role;
import plugily.projects.murdermystery.arena.sabotage.SabotageManager;
import plugily.projects.murdermystery.arena.sabotage.SabotageType;
import plugily.projects.murdermystery.arena.special.SpecialBlock;
import plugily.projects.murdermystery.arena.special.VentManager;
import plugily.projects.murdermystery.arena.states.InGameState;
import plugily.projects.murdermystery.arena.states.RestartingState;
import plugily.projects.murdermystery.arena.states.StartingState;
import plugily.projects.murdermystery.arena.states.WaitingState;
import plugily.projects.murdermystery.arena.task.TaskManager;
import plugily.projects.murdermystery.arena.task.TaskType;
import plugily.projects.murdermystery.arena.voting.VotingManager;
import plugily.projects.murdermystery.utils.HalloweenChat;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * @author Tigerpanzer_02
 * <p>
 * Created at 17.12.2021
 */
public class Arena extends PluginArena {

  private static Main plugin;

  private final List<Player> spectators = new CopyOnWriteArrayList<>();
  private final List<Player> deaths = new CopyOnWriteArrayList<>();
  private final List<Player> detectives = new ArrayList<>();
  private final List<Player> murderers = new ArrayList<>();
  private final List<Item> goldSpawned = new ArrayList<>();
  private final List<Corpse> corpses = new ArrayList<>();
  private final List<Stand> stands = new ArrayList<>();
  private final List<SpecialBlock> specialBlocks = new ArrayList<>();
  private List<Location> goldSpawnPoints = new ArrayList<>();
  private List<Location> playerSpawnPoints = new ArrayList<>();
  private int spawnGoldTimer = 0;
  private int spawnGoldTime = 0;
  private boolean detectiveDead;
  private boolean murdererLocatorReceived;
  private boolean hideChances;
  private boolean goldVisuals = false;
  private boolean sabotageWin = false;
  private final Map<CharacterType, Player> gameCharacters = new EnumMap<>(CharacterType.class);
  private final MapRestorerManager mapRestorerManager;
  private ArmorStandHologram bowHologram;
  private final TaskManager taskManager;
  private final VotingManager votingManager;
  private final VentManager ventManager;
  private final ArenaMinimap minimap;
  private final SabotageManager sabotageManager;
  private final CutsceneManager cutsceneManager;
  private List<Location> taskLocations = new ArrayList<>();
  private final Map<TaskType, List<Location>> specificTaskLocations = new EnumMap<>(TaskType.class);
  private final Map<SabotageType, Location> sabotageLocations = new EnumMap<>(SabotageType.class);
  private Location campfireLocation;
  private Location meetingLocation;
  private TextDisplay campfireDisplay;
  private int tasksPerPlayer = -1;
  private boolean visualTasksEnabled = true;

  public Arena(String id) {
    super(id);
    setPluginValues();
    initThreadSafePlayersSet();
    initCustomBossbarManager();
    setScoreboardManager(new ScoreboardManager(this));
    mapRestorerManager = new MapRestorerManager(this);
    setMapRestorerManager(mapRestorerManager);
    taskManager = new TaskManager(this);
    votingManager = new VotingManager(this);
    ventManager = new VentManager(plugin, this);
    minimap = new ArenaMinimap(this);
    sabotageManager = new SabotageManager(this);
    cutsceneManager = new CutsceneManager(this);
    addGameStateHandler(IArenaState.IN_GAME, new InGameState());
    addGameStateHandler(IArenaState.RESTARTING, new RestartingState());
    addGameStateHandler(IArenaState.STARTING, new StartingState());
    addGameStateHandler(IArenaState.WAITING_FOR_PLAYERS, new WaitingState());
  }

  public static void init(Main plugin) {
    Arena.plugin = plugin;
  }

  @Override
  public Main getPlugin() {
    return plugin;
  }


  @Override
  public PluginMapRestorerManager getMapRestorerManager() {
    return mapRestorerManager;
  }

  @SuppressWarnings("unchecked")
  private void initThreadSafePlayersSet() {
    try {
      Field playersField = PluginArena.class.getDeclaredField("players");
      playersField.setAccessible(true);
      Set<Player> existing = (Set<Player>) playersField.get(this);
      Set<Player> threadSafeSet = ConcurrentHashMap.newKeySet();
      if(existing != null) {
        threadSafeSet.addAll(existing);
      }
      playersField.set(this, threadSafeSet);
    } catch(Exception ignored) {
    }
  }

  private void initCustomBossbarManager() {
    try {
      BossbarManager customManager = new BossbarManager(this) {
        @Override
        public void bossBarUpdate() {
          cleanUpBossBars(null);
          if(getArenaState() == IArenaState.IN_GAME) {
            clearPluginBossBars();
            return;
          }
          super.bossBarUpdate();
        }

        @Override
        public void doBarAction(IPluginArena.IBarAction action, Player player) {
          cleanUpBossBars(action == IPluginArena.IBarAction.REMOVE ? player : null);
          if(action == IPluginArena.IBarAction.ADD && getArenaState() == IArenaState.IN_GAME) {
            return;
          }
          super.doBarAction(action, player);
          cleanUpBossBars(null);
        }
      };
      Field bossbarField = PluginArena.class.getDeclaredField("bossbarManager");
      bossbarField.setAccessible(true);
      bossbarField.set(this, customManager);
    } catch(Exception ignored) {
    }
  }

  @SuppressWarnings("unchecked")
  public void cleanUpBossBars(@Nullable Player leavingPlayer) {
    BossbarManager manager = getBossbarManager();
    if(manager == null) {
      return;
    }
    try {
      Field gameBarsField = BossbarManager.class.getDeclaredField("gameBars");
      gameBarsField.setAccessible(true);
      List<BossBar> gameBars = (List<BossBar>) gameBarsField.get(manager);
      if(gameBars == null) {
        return;
      }
      Iterator<BossBar> iterator = gameBars.iterator();
      while(iterator.hasNext()) {
        BossBar bar = iterator.next();
        if(bar == null) {
          iterator.remove();
          continue;
        }
        if(leavingPlayer != null && bar.getPlayers().contains(leavingPlayer)) {
          bar.removePlayer(leavingPlayer);
        }
        if(bar.getPlayers().isEmpty()) {
          bar.setVisible(false);
          bar.removeAll();
          iterator.remove();
        }
      }
    } catch(Exception ignored) {
    }
  }

  @SuppressWarnings("unchecked")
  public void clearPluginBossBars() {
    BossbarManager manager = getBossbarManager();
    if(manager == null) {
      return;
    }
    try {
      Field gameBarsField = BossbarManager.class.getDeclaredField("gameBars");
      gameBarsField.setAccessible(true);
      List<BossBar> gameBars = (List<BossBar>) gameBarsField.get(manager);
      if(gameBars == null) {
        return;
      }
      for(BossBar bar : gameBars) {
        if(bar != null) {
          bar.setVisible(false);
          bar.removeAll();
        }
      }
      gameBars.clear();
    } catch(Exception ignored) {
    }
  }


  private void setPluginValues() {
  }

  public void addCorpse(Corpse corpse) {
    if(plugin.getHookManager().isFeatureEnabled(HookManager.HookFeature.CORPSES)) {
      corpses.add(corpse);
    }
  }

  public List<Corpse> getCorpses() {
    return corpses;
  }

  public List<Stand> getStands() {
    return stands;
  }

  public void addHead(Stand stand) {
    stands.add(stand);
  }

  public void setSpawnGoldTime(int spawnGoldTime) {
    this.spawnGoldTime = spawnGoldTime;
  }

  public void setHideChances(boolean hideChances) {
    this.hideChances = hideChances;
  }

  public boolean isDetectiveDead() {
    return detectiveDead;
  }

  public void setDetectiveDead(boolean detectiveDead) {
    this.detectiveDead = detectiveDead;
  }

  public boolean isMurdererLocatorReceived() {
    return murdererLocatorReceived;
  }

  public void setMurdererLocatorReceived(boolean murdererLocatorReceived) {
    this.murdererLocatorReceived = murdererLocatorReceived;
  }

  public Map<CharacterType, Player> getGameCharacters() {
    return gameCharacters;
  }

  public boolean isHideChances() {
    return hideChances;
  }

  @NotNull
  public List<Item> getGoldSpawned() {
    return goldSpawned;
  }

  @NotNull
  public List<Location> getGoldSpawnPoints() {
    return goldSpawnPoints;
  }

  public void setGoldSpawnPoints(@NotNull List<Location> goldSpawnPoints) {
    this.goldSpawnPoints = goldSpawnPoints;
  }

  private BukkitTask visualTask;

  public void startGoldVisuals() {
  }

  public boolean isGoldVisuals() {
    return false;
  }

  public void setGoldVisuals(boolean goldVisuals) {
    this.goldVisuals = false;
  }

  public void loadSpecialBlock(SpecialBlock block) {
    if(!specialBlocks.contains(block)) {
      specialBlocks.add(block);
    }

    switch(block.getSpecialBlockType()) {
      case RAPID_TELEPORTATION:
        if(block.getTextDisplay() != null) {
          TextDisplay oldTd = block.getTextDisplay();
          textDisplays.remove(oldTd);
          ventDisplays.remove(oldTd);
          try {
            if(oldTd.getLocation().getWorld() != null) {
              oldTd.getLocation().getChunk().load();
            }
            oldTd.remove();
          } catch(Throwable ignored) {
          }
          block.setTextDisplay(null);
        }
        Location ventLoc = block.getLocation().clone().add(0.5, 1.2, 0.5);
        removeNearbyDisplays(ventLoc, 1.2, 1.8, 1.2, false);
        TextDisplay td = spawnTextDisplay(ventLoc, "<shadow:#000000FF><#5A4E7C>[</shadow><#FF3B47>Sewer Vent<shadow:#000000FF><#5A4E7C>]</shadow>\n\n<#F4F1DE>ท่อระบายน้ำ\n\n<shadow:#000000FF><#5A4E7C>(</shadow><#B8B0C8>กดคลิกขวาหรือ <#FFB703>SHIFT <#B8B0C8>เพื่อมุดท่อ<shadow:#000000FF><#5A4E7C>)</shadow>", false);
        block.setTextDisplay(td);
        if(td != null) {
          ventDisplays.add(td);
        }
        BlockDisplay bd = spawnBlockDisplay(block.getLocation(), null, ChatColor.RED);
        if(bd != null) {
          ventBlockDisplays.add(bd);
        }
        break;
      case MYSTERY_CAULDRON:
      case PRAISE_DEVELOPER:
      case HORSE_PURCHASE:
      default:
        break;
    }
  }

  private final List<TextDisplay> textDisplays = new ArrayList<>();
  private final List<TextDisplay> ventDisplays = new ArrayList<>();
  private final List<BlockDisplay> blockDisplays = new ArrayList<>();
  private final List<BlockDisplay> ventBlockDisplays = new ArrayList<>();
  private final Set<String> glowEntries = new HashSet<>();

  private boolean taskWin = false;

  public boolean isSabotageWin() {
    return sabotageWin;
  }

  public void setSabotageWin(boolean sabotageWin) {
    this.sabotageWin = sabotageWin;
  }

  public boolean isTaskWin() {
    return taskWin;
  }

  public void setTaskWin(boolean taskWin) {
    this.taskWin = taskWin;
  }

  public boolean isMurdererVictory() {
    if(taskWin) {
      return false;
    }
    if(sabotageWin) {
      return true;
    }
    int aliveMurds = aliveMurderer();
    return aliveMurds > 0 && getPlayersLeft().size() <= aliveMurds;
  }

  public void removeNearbyDisplays(Location loc, double rx, double ry, double rz, boolean textOnly) {
    if(loc == null || loc.getWorld() == null) {
      return;
    }
    try {
      loc.getChunk().load();
      for(Entity entity : loc.getWorld().getNearbyEntities(loc, rx, ry, rz)) {
        if(entity instanceof TextDisplay) {
          textDisplays.remove(entity);
          ventDisplays.remove(entity);
          entity.remove();
        } else if(!textOnly && entity instanceof BlockDisplay) {
          blockDisplays.remove(entity);
          ventBlockDisplays.remove(entity);
          removeEntityGlowColor(entity);
          entity.remove();
        }
      }
    } catch(Throwable ignored) {
    }
  }

  public TextDisplay spawnTextDisplay(Location loc, String text) {
    return spawnTextDisplay(loc, text, true);
  }

  public TextDisplay spawnTextDisplay(Location loc, String text, boolean visibleByDefault) {
    if(loc == null || loc.getWorld() == null) {
      return null;
    }
    try {
      loc.getChunk().load();
      TextDisplay display = loc.getWorld().spawn(loc, TextDisplay.class, entity -> {
        entity.text(HalloweenChat.component(text));
        entity.setBillboard(Display.Billboard.CENTER);
        entity.setShadowed(true);
        entity.setDefaultBackground(false);
        entity.setBackgroundColor(Color.fromARGB(0, 0, 0, 0));
        entity.setSeeThrough(false);
        entity.setVisibleByDefault(visibleByDefault);
        entity.setPersistent(false);
      });
      textDisplays.add(display);
      return display;
    } catch(Exception e) {
      return null;
    }
  }

  public BlockDisplay spawnBlockDisplay(Location loc, BlockData blockData, ChatColor glowColor) {
    if(loc == null || loc.getWorld() == null) {
      return null;
    }
    try {
      loc.getChunk().load();
      Location blockLoc = loc.getBlock().getLocation();
      BlockDisplay display = loc.getWorld().spawn(blockLoc, BlockDisplay.class, entity -> {
        if(blockData != null) {
          entity.setBlock(blockData);
        } else {
          Block b = loc.getBlock();
          if(b.getType() != Material.AIR) {
            entity.setBlock(b.getBlockData());
          } else {
            entity.setBlock(Material.BARRIER.createBlockData());
          }
        }
        try {
          Transformation transformation = new Transformation(
            new Vector3f(-0.003f, -0.003f, -0.003f),
            new AxisAngle4f(),
            new Vector3f(1.006f, 1.006f, 1.006f),
            new AxisAngle4f()
          );
          entity.setTransformation(transformation);
        } catch(Throwable ignored) {
        }

        entity.setGlowing(true);
        entity.setVisibleByDefault(false);
        entity.setPersistent(false);
      });

      if(glowColor != null) {
        setEntityGlowColor(display, glowColor);
      }

      blockDisplays.add(display);
      return display;
    } catch(Exception e) {
      return null;
    }
  }

  public void setEntityGlowColor(Entity entity, ChatColor color) {
    if(entity == null) {
      return;
    }
    try {
      String teamName = "MM_GLOW_" + color.name();
      if(teamName.length() > 16) {
        teamName = teamName.substring(0, 16);
      }
      String entry = entity.getUniqueId().toString();
      glowEntries.add(entry);

      Scoreboard mainSb = Bukkit.getScoreboardManager().getMainScoreboard();
      Team mainTeam = mainSb.getTeam(teamName);
      if(mainTeam == null) {
        mainTeam = mainSb.registerNewTeam(teamName);
        mainTeam.setColor(color);
      }
      if(!mainTeam.hasEntry(entry)) {
        mainTeam.addEntry(entry);
      }

      for(Player p : getPlayers()) {
        try {
          Scoreboard sb = p.getScoreboard();
          if(sb != null && !sb.equals(mainSb)) {
            Team team = sb.getTeam(teamName);
            if(team == null) {
              team = sb.registerNewTeam(teamName);
              team.setColor(color);
            }
            if(!team.hasEntry(entry)) {
              team.addEntry(entry);
            }
          }
        } catch(Throwable ignored) {
        }
      }
    } catch(Throwable ignored) {
    }
  }

  public void removeEntityGlowColor(Entity entity) {
    if(entity == null) {
      return;
    }
    removeGlowEntry(entity.getUniqueId().toString());
  }

  private void removeGlowEntry(String entry) {
    glowEntries.remove(entry);
    try {
      Scoreboard mainSb = Bukkit.getScoreboardManager().getMainScoreboard();
      for(Team t : mainSb.getTeams()) {
        if(t.getName().startsWith("MM_GLOW_") && t.hasEntry(entry)) {
          t.removeEntry(entry);
        }
      }
      for(Player p : Bukkit.getOnlinePlayers()) {
        Scoreboard sb = p.getScoreboard();
        if(sb != null && !sb.equals(mainSb)) {
          for(Team t : sb.getTeams()) {
            if(t.getName().startsWith("MM_GLOW_") && t.hasEntry(entry)) {
              t.removeEntry(entry);
            }
          }
        }
      }
    } catch(Throwable ignored) {
    }
  }

  public void clearGlowEntries() {
    if(glowEntries.isEmpty()) {
      return;
    }
    Set<String> snapshot = new HashSet<>(glowEntries);
    glowEntries.clear();
    try {
      Scoreboard mainSb = Bukkit.getScoreboardManager().getMainScoreboard();
      for(Team t : mainSb.getTeams()) {
        if(t.getName().startsWith("MM_GLOW_")) {
          for(String entry : snapshot) {
            if(t.hasEntry(entry)) {
              t.removeEntry(entry);
            }
          }
        }
      }
      for(Player p : Bukkit.getOnlinePlayers()) {
        Scoreboard sb = p.getScoreboard();
        if(sb != null && !sb.equals(mainSb)) {
          for(Team t : sb.getTeams()) {
            if(t.getName().startsWith("MM_GLOW_")) {
              for(String entry : snapshot) {
                if(t.hasEntry(entry)) {
                  t.removeEntry(entry);
                }
              }
            }
          }
        }
      }
    } catch(Throwable ignored) {
    }
  }

  public void clearVentDisplays() {
    for(SpecialBlock sb : specialBlocks) {
      if(sb.getSpecialBlockType() == SpecialBlock.SpecialBlockType.RAPID_TELEPORTATION) {
        sb.setTextDisplay(null);
        if(sb.getLocation() != null) {
          removeNearbyDisplays(sb.getLocation().clone().add(0.5, 1.2, 0.5), 1.2, 1.8, 1.2, false);
        }
      }
    }
    for(TextDisplay td : new ArrayList<>(ventDisplays)) {
      textDisplays.remove(td);
      if(td != null) {
        try {
          td.remove();
        } catch(Throwable ignored) {
        }
      }
    }
    for(BlockDisplay bd : new ArrayList<>(ventBlockDisplays)) {
      blockDisplays.remove(bd);
      if(bd != null) {
        removeEntityGlowColor(bd);
        try {
          bd.remove();
        } catch(Throwable ignored) {
        }
      }
    }
    ventDisplays.clear();
    ventBlockDisplays.clear();
  }

  public void showVentDisplays(Player player) {
    for(TextDisplay td : ventDisplays) {
      if(td != null && td.isValid()) {
        try {
          player.showEntity(plugin, td);
        } catch(Throwable ignored) {
        }
      }
    }
    for(BlockDisplay bd : ventBlockDisplays) {
      if(bd != null && bd.isValid()) {
        try {
          setEntityGlowColor(bd, ChatColor.RED);
          player.showEntity(plugin, bd);
        } catch(Throwable ignored) {
        }
      }
    }
  }

  public void hideVentDisplays(Player player) {
    for(TextDisplay td : ventDisplays) {
      if(td != null && td.isValid()) {
        try {
          player.hideEntity(plugin, td);
        } catch(Throwable ignored) {
        }
      }
    }
    for(BlockDisplay bd : ventBlockDisplays) {
      if(bd != null && bd.isValid()) {
        try {
          player.hideEntity(plugin, bd);
        } catch(Throwable ignored) {
        }
      }
    }
  }

  public void updateVentDisplaysVisibility() {
    for(Player player : getPlayers()) {
      IUser user = plugin.getUserManager().getUser(player);
      boolean isMurderer = Role.isRole(Role.MURDERER, user, this) || getMurdererList().contains(player);
      if(isMurderer) {
        showVentDisplays(player);
      } else {
        hideVentDisplays(player);
      }
    }
  }

  public void clearTextDisplays() {
    for(SpecialBlock sb : specialBlocks) {
      if(sb.getTextDisplay() != null) {
        sb.setTextDisplay(null);
      }
      if(sb.getLocation() != null) {
        removeNearbyDisplays(sb.getLocation().clone().add(0.5, 1.2, 0.5), 1.2, 1.8, 1.2, false);
      }
    }
    if(campfireLocation != null) {
      removeStaleCampfireDisplays(getCampfireHologramLocation());
    }
    for(TextDisplay td : new ArrayList<>(textDisplays)) {
      if(td != null) {
        try {
          if(td.getLocation().getWorld() != null) {
            td.getLocation().getChunk().load();
          }
          td.remove();
        } catch(Throwable ignored) {
        }
      }
    }
    for(BlockDisplay bd : new ArrayList<>(blockDisplays)) {
      if(bd != null) {
        try {
          if(bd.getLocation().getWorld() != null) {
            bd.getLocation().getChunk().load();
          }
          bd.remove();
        } catch(Throwable ignored) {
        }
      }
    }
    textDisplays.clear();
    ventDisplays.clear();
    blockDisplays.clear();
    ventBlockDisplays.clear();
    clearGlowEntries();
    campfireDisplay = null;
  }

  public List<TextDisplay> getTextDisplays() {
    return textDisplays;
  }

  public List<TextDisplay> getVentDisplays() {
    return ventDisplays;
  }

  public List<BlockDisplay> getVentBlockDisplays() {
    return ventBlockDisplays;
  }

  public VentManager getVentManager() {
    return ventManager;
  }

  public List<SpecialBlock> getSpecialBlocks() {
    return specialBlocks;
  }

  public int getTotalRoleChances(Role role) {
    int totalRoleChances = 0;

    for(Player p : getPlayersLeft()) {
      IUser user = getPlugin().getUserManager().getUser(p);
      totalRoleChances += getContributorValue(role, user);
    }
    //avoid division / 0
    return totalRoleChances == 0 ? 1 : totalRoleChances;
  }

  public boolean isCharacterSet(Arena.CharacterType type) {
    return gameCharacters.containsKey(type);
  }

  public void setCharacter(Arena.CharacterType type, Player player) {
    gameCharacters.put(type, player);
  }

  public void setCharacter(Role role, Player player) {
    gameCharacters.put(role == Role.MURDERER ? CharacterType.MURDERER : CharacterType.DETECTIVE, player);
    if(role == Role.MURDERER) {
      showVentDisplays(player);
    }
  }

  @Nullable
  public Player getCharacter(Arena.CharacterType type) {
    return gameCharacters.get(type);
  }

  public void addToDetectiveList(Player player) {
    detectives.add(player);
  }

  public boolean lastAliveDetective() {
    return aliveDetective() <= 1;
  }

  public int aliveDetective() {
    int alive = 0;
    for(Player player : getPlayersLeft()) {
      if(Role.isRole(Role.ANY_DETECTIVE, plugin.getUserManager().getUser(player), this) && isDetectiveAlive(player)) {
        alive++;
      }
    }
    return alive;
  }

  public boolean isDetectiveAlive(Player player) {
    for(Player p : getPlayersLeft()) {
      if(p == player && detectives.contains(p)) {
        return true;
      }
    }
    return false;
  }

  public List<Player> getDetectiveList() {
    return detectives;
  }

  public void addToMurdererList(Player player) {
    murderers.add(player);
  }

  public void removeFromMurdererList(Player player) {
    murderers.remove(player);
  }


  public boolean lastAliveMurderer() {
    return aliveMurderer() == 1;
  }

  public int aliveMurderer() {
    int alive = 0;
    for(Player player : getPlayersLeft()) {
      if(Role.isRole(Role.MURDERER, plugin.getUserManager().getUser(player), this) && isMurderAlive(player)) {
        alive++;
      }
    }
    return alive;
  }

  public boolean isMurderAlive(Player player) {
    for(Player p : getPlayersLeft()) {
      if(p == player && murderers.contains(p)) {
        return true;
      }
    }
    return false;
  }

  public List<Player> getMurdererList() {
    return murderers;
  }

  public void setBowHologram(ArmorStandHologram bowHologram) {
    if(bowHologram == null) {
      this.bowHologram = null;
      return;
    }

    this.bowHologram = bowHologram;
  }

  public ArmorStandHologram getBowHologram() {
    return bowHologram;
  }

  public void addDeathPlayer(Player player) {
    if(!deaths.contains(player)) {
      deaths.add(player);
    }
  }

  public void removeDeathPlayer(Player player) {
    deaths.remove(player);
  }

  public boolean isDeathPlayer(Player player) {
    return deaths.contains(player);
  }

  public List<Player> getDeaths() {
    return deaths;
  }

  public void addSpectatorPlayer(Player player) {
    if(!spectators.contains(player)) {
      spectators.add(player);
    }
  }

  public void removeSpectatorPlayer(Player player) {
    spectators.remove(player);
  }

  public boolean isSpectatorPlayer(Player player) {
    return spectators.contains(player);
  }

  public List<Player> getSpectators() {
    return spectators;
  }

  public List<Location> getPlayerSpawnPoints() {
    return playerSpawnPoints;
  }

  public int getSpawnGoldTime() {
    return spawnGoldTime;
  }

  public int getSpawnGoldTimer() {
    return spawnGoldTimer;
  }

  public void setSpawnGoldTimer(int spawnGoldTimer) {
    this.spawnGoldTimer = spawnGoldTimer;
  }


  public void setPlayerSpawnPoints(@NotNull List<Location> playerSpawnPoints) {
    this.playerSpawnPoints = playerSpawnPoints;
  }

  public void adjustContributorValue(Role role, IUser user, int number) {
    user.adjustStatistic("CONTRIBUTION_" + role.name(), number);
  }

  private final Map<IUser, Integer> murdererContributions = new HashMap<>();
  private final Map<IUser, Integer> detectiveContributions = new HashMap<>();

  public Map<IUser, Integer> getMurdererContributions() {
    return murdererContributions;
  }

  public Map<IUser, Integer> getDetectiveContributions() {
    return detectiveContributions;
  }

  public int getContributorValue(Role role, IUser user) {
    if(role == Role.MURDERER && murdererContributions.containsKey(user)) {
      return murdererContributions.get(user);
    } else if(detectiveContributions.containsKey(user)) {
      return detectiveContributions.get(user);
    }
    Player player = user.getPlayer();
    int contributor = user.getStatistic("CONTRIBUTION_" + role.name());
    int increase = plugin.getPermissionsManager().getPermissionCategoryValue(role.name() + "_BOOSTER", player);
    int multiplicator = plugin.getPermissionsManager().getPermissionCategoryValue("CHANCES_BOOSTER", player);
    long rawContributor = ((long) contributor + (long) increase) * (multiplicator == 0 ? 1L : (long) multiplicator);
    int calculatedContributor = (int) Math.max(1L, Math.min(Integer.MAX_VALUE - 1L, rawContributor));
    if(role == Role.MURDERER) {
      murdererContributions.put(user, calculatedContributor);
    } else {
      detectiveContributions.put(user, calculatedContributor);
    }
    return calculatedContributor;
  }

  public void resetContributorValue(Role role, IUser user) {
    user.setStatistic("CONTRIBUTION_" + role.name(), 1);
  }

  public enum CharacterType {
    MURDERER, DETECTIVE, FAKE_DETECTIVE, HERO
  }

  public TaskManager getTaskManager() {
    return taskManager;
  }

  public VotingManager getVotingManager() {
    return votingManager;
  }

  public ArenaMinimap getMinimap() {
    return minimap;
  }

  public SabotageManager getSabotageManager() {
    return sabotageManager;
  }

  private static Location normalizeBlockLocation(Location loc) {
    if(loc == null || loc.getWorld() == null) {
      return loc;
    }
    return new Location(loc.getWorld(), loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
  }

  private static boolean isSameBlockLocation(Location a, Location b) {
    if(a == null || b == null || a.getWorld() == null || b.getWorld() == null) {
      return false;
    }
    return a.getWorld().equals(b.getWorld())
      && a.getBlockX() == b.getBlockX()
      && a.getBlockY() == b.getBlockY()
      && a.getBlockZ() == b.getBlockZ();
  }

  public List<Location> getTaskLocations() {
    return taskLocations;
  }

  public void setTaskLocations(List<Location> taskLocations) {
    List<Location> normalized = new ArrayList<>();
    if(taskLocations != null) {
      for(Location loc : taskLocations) {
        Location blockLoc = normalizeBlockLocation(loc);
        if(blockLoc != null && normalized.stream().noneMatch(existing -> isSameBlockLocation(existing, blockLoc))) {
          normalized.add(blockLoc);
        }
      }
    }
    this.taskLocations = normalized;
  }

  public Map<TaskType, List<Location>> getSpecificTaskLocations() {
    return specificTaskLocations;
  }

  public List<Location> getTaskLocations(TaskType type) {
    return specificTaskLocations.getOrDefault(type, Collections.emptyList());
  }

  public void addSpecificTaskLocation(TaskType type, Location loc) {
    if(type == null || loc == null) {
      return;
    }
    Location blockLoc = normalizeBlockLocation(loc);
    List<Location> list = specificTaskLocations.computeIfAbsent(type, k -> new ArrayList<>());
    if(list.stream().noneMatch(existing -> isSameBlockLocation(existing, blockLoc))) {
      list.add(blockLoc);
    }
  }

  public void removeTaskLocationAtBlock(Location loc) {
    if(loc == null || loc.getWorld() == null) {
      return;
    }
    taskLocations.removeIf(existing -> isSameBlockLocation(existing, loc));
    Iterator<Map.Entry<TaskType, List<Location>>> it = specificTaskLocations.entrySet().iterator();
    while(it.hasNext()) {
      Map.Entry<TaskType, List<Location>> entry = it.next();
      entry.getValue().removeIf(existing -> isSameBlockLocation(existing, loc));
      if(entry.getValue().isEmpty()) {
        it.remove();
      }
    }
  }

  public void clearSpecificTaskLocations(TaskType type) {
    if(type == null) {
      specificTaskLocations.clear();
    } else {
      specificTaskLocations.remove(type);
    }
  }

  public List<Location> getAllTaskLocations() {
    List<Location> all = new ArrayList<>();
    for(Location loc : taskLocations) {
      if(loc != null && all.stream().noneMatch(existing -> isSameBlockLocation(existing, loc))) {
        all.add(loc);
      }
    }
    for(List<Location> list : specificTaskLocations.values()) {
      for(Location loc : list) {
        if(loc != null && all.stream().noneMatch(existing -> isSameBlockLocation(existing, loc))) {
          all.add(loc);
        }
      }
    }
    return all;
  }

  public Map<SabotageType, Location> getSabotageLocations() {
    return sabotageLocations;
  }

  public Location getSabotageLocation(SabotageType type) {
    return sabotageLocations.get(type);
  }

  public void setSabotageLocation(SabotageType type, Location loc) {
    if(loc == null) {
      sabotageLocations.remove(type);
    } else {
      sabotageLocations.put(type, loc);
    }
  }

  public void clearSabotageLocations(SabotageType type) {
    if(type == null) {
      sabotageLocations.clear();
    } else {
      sabotageLocations.remove(type);
    }
  }

  public List<Location> getAllSabotageLocations() {
    List<Location> all = new ArrayList<>();
    for(Location loc : sabotageLocations.values()) {
      if(loc != null && !all.contains(loc)) {
        all.add(loc);
      }
    }
    return all;
  }

  public Location getCampfireLocation() {
    return campfireLocation;
  }

  public void setCampfireLocation(Location campfireLocation) {
    this.campfireLocation = campfireLocation;
    spawnCampfireHologram();
  }

  public Location getMeetingLocation() {
    return meetingLocation;
  }

  public void setMeetingLocation(Location meetingLocation) {
    this.meetingLocation = meetingLocation;
  }

  public Location getEffectiveMeetingLocation() {
    return meetingLocation != null ? meetingLocation : campfireLocation;
  }

  public int getTasksPerPlayer() {
    return tasksPerPlayer;
  }

  public void setTasksPerPlayer(int tasksPerPlayer) {
    this.tasksPerPlayer = tasksPerPlayer;
  }

  public int getEffectiveTasksPerPlayer() {
    if(tasksPerPlayer > 0) {
      return tasksPerPlayer;
    }
    return Math.max(1, plugin.getConfig().getInt("Tasks.Per-Player", 5));
  }

  public boolean isVisualTasksEnabled() {
    return visualTasksEnabled;
  }

  public void setVisualTasksEnabled(boolean visualTasksEnabled) {
    this.visualTasksEnabled = visualTasksEnabled;
  }

  public CutsceneManager getCutsceneManager() {
    return cutsceneManager;
  }

  public TextDisplay getCampfireDisplay() {
    return campfireDisplay;
  }

  public String buildCampfireHologramText() {
    int remainingUses = votingManager != null
      ? votingManager.getRemainingEmergencyMeetings()
      : plugin.getConfig().getInt("Voting.Max-Emergency-Meetings", 2);
    int cooldownSec = votingManager != null ? votingManager.getEmergencyCooldownRemaining() : 0;
    boolean sabotageActive = sabotageManager != null && sabotageManager.hasActiveSabotage();

    StringBuilder sb = new StringBuilder();
    sb.append("<shadow:#000000FF><#5A4E7C>[</shadow><#FF7518>Bus Horn<shadow:#000000FF><#5A4E7C>]</shadow>\n\n");
    if(remainingUses > 0) {
      sb.append("<#F4F1DE>กดได้อีก <#FFB703>").append(remainingUses).append(" <#F4F1DE>รอบ");
      if(sabotageActive) {
        sb.append("\n<#FF3B47>เกิดวินาศกรรม (กดไม่ได้)");
      } else if(cooldownSec > 0) {
        sb.append("\n<#FF3B47>คูลดาวน์ ").append(cooldownSec).append(" วิ");
      }
    } else {
      sb.append("<#FF3B47>กดได้อีก 0 รอบ");
    }
    sb.append("\n\n<shadow:#000000FF><#5A4E7C>(</shadow><#B8B0C8>กดคลิกขวาเพื่อบีบแตรเรียกประชุมฉุกเฉิน<shadow:#000000FF><#5A4E7C>)</shadow>");
    return sb.toString();
  }

  private boolean hasLowCeilingAboveCampfire() {
    if(campfireLocation == null || campfireLocation.getWorld() == null) {
      return false;
    }
    try {
      Block base = campfireLocation.getBlock();
      return base.getRelative(0, 2, 0).getType().isSolid()
        || base.getRelative(0, 3, 0).getType().isSolid();
    } catch(Throwable ignored) {
      return false;
    }
  }

  private Location getCampfireHologramLocation() {
    if(campfireLocation == null) {
      return null;
    }
    double yOffset = hasLowCeilingAboveCampfire() ? 0.95 : 1.8;
    return campfireLocation.clone().add(0.5, yOffset, 0.5);
  }

  private void removeStaleCampfireDisplays(Location holoLoc) {
    if(holoLoc == null || holoLoc.getWorld() == null) {
      return;
    }
    try {
      holoLoc.getChunk().load();
      for(Entity entity : holoLoc.getWorld().getNearbyEntities(holoLoc, 1.5, 2.5, 1.5)) {
        if(entity instanceof TextDisplay && !entity.equals(campfireDisplay)) {
          TextDisplay td = (TextDisplay) entity;
          if(textDisplays.contains(td)) {
            continue;
          }
          String rawText = "";
          try {
            rawText = HalloweenChat.strip(td.getText());
          } catch(Throwable ignored) {
          }
          if(rawText.contains("Bus Horn") || rawText.contains("Emergency Campfire") || rawText.contains("บีบแตร")) {
            entity.remove();
          }
        }
      }
    } catch(Throwable ignored) {
    }
  }

  public void updateCampfireHologram() {
    if(campfireLocation == null || campfireLocation.getWorld() == null) {
      return;
    }
    try {
      campfireLocation.getChunk().load();
    } catch(Throwable ignored) {
    }
    if(campfireDisplay == null || !campfireDisplay.isValid()) {
      spawnCampfireHologram();
      return;
    }
    Location holoLoc = getCampfireHologramLocation();
    removeStaleCampfireDisplays(holoLoc);
    campfireDisplay.setDefaultBackground(false);
    campfireDisplay.setBackgroundColor(Color.fromARGB(0, 0, 0, 0));
    campfireDisplay.text(HalloweenChat.component(buildCampfireHologramText()));
  }

  public void spawnCampfireHologram() {
    if(campfireDisplay != null) {
      textDisplays.remove(campfireDisplay);
      try {
        if(campfireDisplay.getLocation().getWorld() != null) {
          campfireDisplay.getLocation().getChunk().load();
        }
        campfireDisplay.remove();
      } catch(Throwable ignored) {
      }
      campfireDisplay = null;
    }
    if(campfireLocation != null && campfireLocation.getWorld() != null) {
      Location holoLoc = getCampfireHologramLocation();
      removeStaleCampfireDisplays(holoLoc);
      campfireDisplay = spawnTextDisplay(
        holoLoc,
        buildCampfireHologramText()
      );
      if(campfireDisplay != null && hasLowCeilingAboveCampfire()) {
        try {
          campfireDisplay.setTransformation(new Transformation(
            new Vector3f(0.0f, 0.0f, 0.0f),
            new AxisAngle4f(),
            new Vector3f(0.72f, 0.72f, 0.72f),
            new AxisAngle4f()
          ));
        } catch(Throwable ignored) {
        }
      }
      if(campfireDisplay != null && cutsceneManager != null && cutsceneManager.isPlaying()) {
        for(Player p : getPlayers()) {
          if(p != null && p.isOnline()) {
            try {
              p.hideEntity(plugin, campfireDisplay);
            } catch(Throwable ignored) {
            }
          }
        }
      }
    }
  }
}
