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

package plugily.projects.murdermystery.arena.managers;

import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.ChannelPromise;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;
import plugily.projects.murdermystery.arena.ArenaUtils;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Enforces hidden overhead player name tags while in-game across Bukkit Scoreboards,
 * third-party TAB plugins (TabAPI), and outbound Netty scoreboard team packets.
 */
public final class NameTagPacketManager {

  public static final String HIDE_TEAM_NAME = "MM_NT_HIDE";
  private static final String LEGACY_TEAM_NAME = "MMHide";
  private static final String GLOW_TEAM_PREFIX = "MM_GLOW_";
  private static final String HANDLER_NAME = "mm_nametag_guard";

  private static volatile Set<String> currentHiddenNames = Collections.emptySet();
  private static final Map<UUID, Channel> injectedChannels = new ConcurrentHashMap<>();
  private static final AtomicInteger syncTickCounter = new AtomicInteger(0);

  private NameTagPacketManager() {
  }

  public static void init(Plugin plugin) {
    NmsBridge.init();
    TabApiBridge.init();
    if(plugin == null) {
      return;
    }
    for(Player online : plugin.getServer().getOnlinePlayers()) {
      if(online != null && online.isOnline()) {
        injectPlayer(online);
      }
    }
    syncAll(plugin, true);
  }

  public static void handlePlayerJoin(Plugin plugin, Player player) {
    if(player == null) {
      return;
    }
    injectPlayer(player);
    syncAll(plugin, false);
    if(plugin != null && plugin.isEnabled()) {
      Bukkit.getScheduler().runTaskLater(plugin, () -> {
        if(player.isOnline()) {
          injectPlayer(player);
          syncAll(plugin, true);
        }
      }, 2L);
      Bukkit.getScheduler().runTaskLater(plugin, () -> {
        if(player.isOnline()) {
          injectPlayer(player);
          syncAll(plugin, true);
        }
      }, 15L);
    }
  }

  public static void handlePlayerQuit(Plugin plugin, Player player) {
    if(player == null) {
      return;
    }
    TabApiBridge.restore(player.getUniqueId());
    uninjectPlayer(player.getUniqueId(), false);
    if(plugin != null && plugin.isEnabled()) {
      Bukkit.getScheduler().runTask(plugin, () -> syncAll(plugin, false));
    }
  }

  public static void restorePlayer(Plugin plugin, Player player) {
    if(player == null) {
      return;
    }
    TabApiBridge.restore(player.getUniqueId());
    syncAll(plugin, false);
  }

  public static void syncAll(Plugin plugin) {
    int count = syncTickCounter.incrementAndGet();
    boolean periodicReassert = (count % 3) == 0;
    syncAll(plugin, periodicReassert);
  }

  public static void syncAll(Plugin plugin, boolean forceReassert) {
    if(plugin == null) {
      return;
    }
    Collection<? extends Player> onlinePlayers = plugin.getServer().getOnlinePlayers();
    Set<String> hiddenSnapshot = new LinkedHashSet<>();

    for(Player target : onlinePlayers) {
      if(target == null || !target.isOnline()) {
        continue;
      }
      boolean hide = ArenaUtils.shouldHidePlayerNameTag(target);
      if(hide) {
        hiddenSnapshot.add(target.getName());
        TabApiBridge.hide(target.getUniqueId());
      } else {
        TabApiBridge.restore(target.getUniqueId());
      }
    }

    Set<String> immutableHidden = hiddenSnapshot.isEmpty()
      ? Collections.emptySet()
      : Collections.unmodifiableSet(hiddenSnapshot);
    currentHiddenNames = immutableHidden;

    if(!NmsBridge.isSupported()) {
      return;
    }

    for(Player viewer : onlinePlayers) {
      if(viewer == null || !viewer.isOnline()) {
        continue;
      }
      Channel channel = ensureInjected(viewer);
      if(channel == null || !channel.isActive()) {
        continue;
      }
      ChannelHandler rawHandler = channel.pipeline().get(HANDLER_NAME);
      if(rawHandler instanceof ViewerChannelHandler) {
        ViewerChannelHandler handler = (ViewerChannelHandler) rawHandler;
        Runnable syncTask = () -> {
          ChannelHandlerContext ctx = channel.pipeline().context(HANDLER_NAME);
          if(ctx != null && channel.isActive()) {
            handler.syncWithTarget(ctx, immutableHidden, forceReassert);
          }
        };
        if(channel.eventLoop().inEventLoop()) {
          syncTask.run();
        } else {
          channel.eventLoop().execute(syncTask);
        }
      }
    }
  }

  public static void shutdown(Plugin plugin) {
    currentHiddenNames = Collections.emptySet();
    TabApiBridge.restoreAll();
    List<UUID> uuids = new ArrayList<>(injectedChannels.keySet());
    for(UUID uuid : uuids) {
      uninjectPlayer(uuid, true);
    }
    if(plugin != null) {
      for(Player online : plugin.getServer().getOnlinePlayers()) {
        if(online != null) {
          uninjectChannelDirect(NmsBridge.extractChannel(online), true);
        }
      }
    }
  }

  private static Channel injectPlayer(Player player) {
    return ensureInjected(player);
  }

  private static Channel ensureInjected(Player player) {
    if(player == null || !player.isOnline() || !NmsBridge.isSupported()) {
      return null;
    }
    Channel channel = NmsBridge.extractChannel(player);
    if(channel == null || !channel.isOpen()) {
      return null;
    }
    injectedChannels.put(player.getUniqueId(), channel);
    ChannelPipeline pipeline = channel.pipeline();
    if(pipeline.get(HANDLER_NAME) != null) {
      return channel;
    }

    Map<String, String> initialDesiredTeams = new HashMap<>();
    Set<String> initialKnownTeams = new HashSet<>();
    try {
      Scoreboard sb = player.getScoreboard();
      if(sb != null) {
        for(Team team : sb.getTeams()) {
          String tName = team.getName();
          if(HIDE_TEAM_NAME.equals(tName) || LEGACY_TEAM_NAME.equals(tName) || tName.startsWith(GLOW_TEAM_PREFIX)) {
            continue;
          }
          initialKnownTeams.add(tName);
          for(String entry : team.getEntries()) {
            if(entry != null && !entry.isEmpty()) {
              initialDesiredTeams.put(entry, tName);
            }
          }
        }
      }
    } catch(Throwable ignored) {
    }

    ViewerChannelHandler handler = new ViewerChannelHandler(initialKnownTeams, initialDesiredTeams);
    Runnable attachTask = () -> {
      try {
        if(!channel.isOpen() || pipeline.get(HANDLER_NAME) != null) {
          return;
        }
        if(pipeline.get("encoder") != null) {
          pipeline.addAfter("encoder", HANDLER_NAME, handler);
        } else if(pipeline.get("outbound_config") != null) {
          pipeline.addAfter("outbound_config", HANDLER_NAME, handler);
        } else if(pipeline.get("packet_handler") != null) {
          pipeline.addBefore("packet_handler", HANDLER_NAME, handler);
        } else {
          return;
        }
        ChannelHandlerContext ctx = pipeline.context(HANDLER_NAME);
        if(ctx != null && channel.isActive()) {
          handler.syncWithTarget(ctx, currentHiddenNames, true);
        }
      } catch(Throwable ignored) {
      }
    };

    if(channel.eventLoop().inEventLoop()) {
      attachTask.run();
    } else {
      channel.eventLoop().execute(attachTask);
    }
    return channel;
  }

  private static void uninjectPlayer(UUID uuid, boolean sendCleanupPackets) {
    if(uuid == null) {
      return;
    }
    Channel channel = injectedChannels.remove(uuid);
    uninjectChannelDirect(channel, sendCleanupPackets);
  }

  private static void uninjectChannelDirect(Channel channel, boolean sendCleanupPackets) {
    if(channel == null) {
      return;
    }
    Runnable detachTask = () -> {
      try {
        ChannelPipeline pipeline = channel.pipeline();
        ChannelHandler raw = pipeline.get(HANDLER_NAME);
        if(raw instanceof ViewerChannelHandler) {
          ViewerChannelHandler handler = (ViewerChannelHandler) raw;
          ChannelHandlerContext ctx = pipeline.context(HANDLER_NAME);
          if(sendCleanupPackets && ctx != null && channel.isActive()) {
            handler.syncWithTarget(ctx, Collections.emptySet(), false);
            handler.removeHideTeamFromClient(ctx);
          }
        }
        if(pipeline.get(HANDLER_NAME) != null) {
          pipeline.remove(HANDLER_NAME);
        }
      } catch(Throwable ignored) {
      }
    };
    try {
      if(channel.eventLoop().inEventLoop()) {
        detachTask.run();
      } else {
        channel.eventLoop().execute(detachTask);
      }
    } catch(Throwable ignored) {
    }
  }

  private static final class ViewerChannelHandler extends ChannelDuplexHandler {
    private boolean hideTeamCreated = false;
    private final Set<String> clientHiddenMembers = new HashSet<>();
    private final Set<String> knownClientTeams;
    private final Map<String, String> desiredTeamByEntry;

    private ViewerChannelHandler(Set<String> initialKnownTeams, Map<String, String> initialDesiredTeams) {
      this.knownClientTeams = new HashSet<>(initialKnownTeams);
      this.desiredTeamByEntry = new HashMap<>(initialDesiredTeams);
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
      if(msg == null) {
        super.write(ctx, null, promise);
        return;
      }
      try {
        if(NmsBridge.isTeamPacket(msg)) {
          handleOutgoingTeamPacket(ctx, msg, promise);
          return;
        }
        if(NmsBridge.isBundlePacket(msg)) {
          Iterable<?> subPackets = NmsBridge.extractSubPackets(msg);
          if(subPackets != null && NmsBridge.containsTeamPacket(subPackets)) {
            handleBundledPackets(ctx, subPackets, promise);
            return;
          }
        }
      } catch(Throwable ignored) {
      }
      super.write(ctx, msg, promise);
    }

    private void handleBundledPackets(ChannelHandlerContext ctx, Iterable<?> subPackets, ChannelPromise outerPromise) throws Exception {
      List<Object> pending = new ArrayList<>();
      for(Object sub : subPackets) {
        if(sub != null) {
          pending.add(sub);
        }
      }
      if(pending.isEmpty()) {
        succeedPromise(outerPromise);
        return;
      }
      for(int i = 0; i < pending.size(); i++) {
        Object sub = pending.get(i);
        ChannelPromise subPromise = (i == pending.size() - 1) ? outerPromise : ctx.newPromise();
        if(NmsBridge.isTeamPacket(sub)) {
          handleOutgoingTeamPacket(ctx, sub, subPromise);
        } else {
          super.write(ctx, sub, subPromise);
        }
      }
    }

    private void handleOutgoingTeamPacket(ChannelHandlerContext ctx, Object packet, ChannelPromise promise) throws Exception {
      String teamName = NmsBridge.getTeamName(packet);
      if(teamName == null) {
        super.write(ctx, packet, promise);
        return;
      }

      if(HIDE_TEAM_NAME.equals(teamName) || LEGACY_TEAM_NAME.equals(teamName)) {
        succeedPromise(promise);
        syncWithTarget(ctx, currentHiddenNames, false);
        return;
      }

      if(teamName.startsWith(GLOW_TEAM_PREFIX)) {
        super.write(ctx, packet, promise);
        return;
      }

      int method = NmsBridge.getMethod(packet);
      Collection<String> rawPlayers = NmsBridge.getPlayers(packet);
      Optional<?> params = NmsBridge.getParameters(packet);
      Set<String> hiddenNow = currentHiddenNames;

      switch(method) {
        case 0: { // METHOD_ADD (Create team)
          knownClientTeams.add(teamName);
          if(rawPlayers == null || rawPlayers.isEmpty()) {
            super.write(ctx, packet, promise);
            return;
          }
          List<String> allowed = new ArrayList<>(rawPlayers.size());
          List<String> blockedHidden = null;
          for(String entry : rawPlayers) {
            if(entry == null) {
              continue;
            }
            desiredTeamByEntry.put(entry, teamName);
            if(!hiddenNow.isEmpty() && hiddenNow.contains(entry)) {
              if(blockedHidden == null) {
                blockedHidden = new ArrayList<>();
              }
              blockedHidden.add(entry);
            } else {
              allowed.add(entry);
            }
          }
          if(blockedHidden == null) {
            if(!clientHiddenMembers.isEmpty()) {
              clientHiddenMembers.removeAll(allowed);
            }
            super.write(ctx, packet, promise);
            return;
          }
          if(!clientHiddenMembers.isEmpty() && !allowed.isEmpty()) {
            clientHiddenMembers.removeAll(allowed);
          }
          Object rewritten = NmsBridge.createTeamPacket(teamName, 0, params, allowed);
          super.write(ctx, rewritten != null ? rewritten : packet, promise);
          ensureEntriesHiddenOnClient(ctx, blockedHidden);
          return;
        }
        case 1: { // METHOD_REMOVE (Remove team)
          knownClientTeams.remove(teamName);
          desiredTeamByEntry.values().removeIf(teamName::equals);
          super.write(ctx, packet, promise);
          return;
        }
        case 2: { // METHOD_CHANGE (Modify team parameters)
          knownClientTeams.add(teamName);
          super.write(ctx, packet, promise);
          return;
        }
        case 3: { // METHOD_JOIN (Add players to team)
          knownClientTeams.add(teamName);
          if(rawPlayers == null || rawPlayers.isEmpty()) {
            super.write(ctx, packet, promise);
            return;
          }
          List<String> allowed = new ArrayList<>(rawPlayers.size());
          List<String> blockedHidden = null;
          for(String entry : rawPlayers) {
            if(entry == null) {
              continue;
            }
            desiredTeamByEntry.put(entry, teamName);
            if(!hiddenNow.isEmpty() && hiddenNow.contains(entry)) {
              if(blockedHidden == null) {
                blockedHidden = new ArrayList<>();
              }
              blockedHidden.add(entry);
            } else {
              allowed.add(entry);
            }
          }
          if(blockedHidden == null) {
            if(!clientHiddenMembers.isEmpty()) {
              clientHiddenMembers.removeAll(allowed);
            }
            super.write(ctx, packet, promise);
            return;
          }
          if(!allowed.isEmpty()) {
            if(!clientHiddenMembers.isEmpty()) {
              clientHiddenMembers.removeAll(allowed);
            }
            Object rewritten = NmsBridge.createTeamPacket(teamName, 3, Optional.empty(), allowed);
            if(rewritten != null) {
              super.write(ctx, rewritten, promise);
            } else {
              succeedPromise(promise);
            }
          } else {
            succeedPromise(promise);
          }
          ensureEntriesHiddenOnClient(ctx, blockedHidden);
          return;
        }
        case 4: { // METHOD_LEAVE (Remove players from team)
          if(rawPlayers == null || rawPlayers.isEmpty()) {
            super.write(ctx, packet, promise);
            return;
          }
          List<String> allowed = new ArrayList<>(rawPlayers.size());
          boolean filteredAny = false;
          for(String entry : rawPlayers) {
            if(entry == null) {
              continue;
            }
            desiredTeamByEntry.remove(entry, teamName);
            if(clientHiddenMembers.contains(entry)) {
              filteredAny = true;
            } else {
              allowed.add(entry);
            }
          }
          if(!filteredAny) {
            super.write(ctx, packet, promise);
          } else if(!allowed.isEmpty()) {
            Object rewritten = NmsBridge.createTeamPacket(teamName, 4, Optional.empty(), allowed);
            if(rewritten != null) {
              super.write(ctx, rewritten, promise);
            } else {
              succeedPromise(promise);
            }
          } else {
            succeedPromise(promise);
          }
          return;
        }
        default:
          super.write(ctx, packet, promise);
      }
    }

    private void ensureEntriesHiddenOnClient(ChannelHandlerContext ctx, Collection<String> entries) {
      if(entries == null || entries.isEmpty()) {
        return;
      }
      List<String> missing = null;
      for(String entry : entries) {
        if(entry != null && !clientHiddenMembers.contains(entry)) {
          if(missing == null) {
            missing = new ArrayList<>();
          }
          missing.add(entry);
        }
      }
      if(missing == null || missing.isEmpty()) {
        return;
      }
      Object packet;
      if(!hideTeamCreated) {
        packet = NmsBridge.createHideTeamCreatePacket(missing);
        if(packet != null) {
          hideTeamCreated = true;
        }
      } else {
        packet = NmsBridge.createTeamPacket(HIDE_TEAM_NAME, 3, Optional.empty(), missing);
      }
      if(packet != null) {
        clientHiddenMembers.addAll(missing);
        ctx.writeAndFlush(packet);
      }
    }

    private void syncWithTarget(ChannelHandlerContext ctx, Set<String> targetHidden, boolean forceReassert) {
      boolean wrote = false;
      Set<String> safeTarget = targetHidden != null ? targetHidden : Collections.emptySet();

      if(!clientHiddenMembers.isEmpty()) {
        List<String> toRestore = null;
        for(String existing : clientHiddenMembers) {
          if(!safeTarget.contains(existing)) {
            if(toRestore == null) {
              toRestore = new ArrayList<>();
            }
            toRestore.add(existing);
          }
        }
        if(toRestore != null && !toRestore.isEmpty()) {
          clientHiddenMembers.removeAll(toRestore);
          Map<String, List<String>> rejoinByTeam = new HashMap<>();
          List<String> plainLeave = new ArrayList<>();
          for(String restored : toRestore) {
            String origTeam = desiredTeamByEntry.get(restored);
            if(origTeam != null && knownClientTeams.contains(origTeam)) {
              rejoinByTeam.computeIfAbsent(origTeam, k -> new ArrayList<>()).add(restored);
            } else {
              plainLeave.add(restored);
            }
          }
          for(Map.Entry<String, List<String>> entry : rejoinByTeam.entrySet()) {
            Object rejoinPkt = NmsBridge.createTeamPacket(entry.getKey(), 3, Optional.empty(), entry.getValue());
            if(rejoinPkt != null) {
              ctx.write(rejoinPkt);
              wrote = true;
            }
          }
          if(!plainLeave.isEmpty() && hideTeamCreated) {
            Object leavePkt = NmsBridge.createTeamPacket(HIDE_TEAM_NAME, 4, Optional.empty(), plainLeave);
            if(leavePkt != null) {
              ctx.write(leavePkt);
              wrote = true;
            }
          }
        }
      }

      if(!safeTarget.isEmpty()) {
        if(forceReassert) {
          Object pkt;
          if(!hideTeamCreated) {
            pkt = NmsBridge.createHideTeamCreatePacket(safeTarget);
            if(pkt != null) {
              hideTeamCreated = true;
            }
          } else {
            pkt = NmsBridge.createTeamPacket(HIDE_TEAM_NAME, 3, Optional.empty(), safeTarget);
          }
          if(pkt != null) {
            clientHiddenMembers.addAll(safeTarget);
            ctx.write(pkt);
            wrote = true;
          }
        } else {
          List<String> toAdd = null;
          for(String entry : safeTarget) {
            if(!clientHiddenMembers.contains(entry)) {
              if(toAdd == null) {
                toAdd = new ArrayList<>();
              }
              toAdd.add(entry);
            }
          }
          if(toAdd != null && !toAdd.isEmpty()) {
            Object pkt;
            if(!hideTeamCreated) {
              pkt = NmsBridge.createHideTeamCreatePacket(toAdd);
              if(pkt != null) {
                hideTeamCreated = true;
              }
            } else {
              pkt = NmsBridge.createTeamPacket(HIDE_TEAM_NAME, 3, Optional.empty(), toAdd);
            }
            if(pkt != null) {
              clientHiddenMembers.addAll(toAdd);
              ctx.write(pkt);
              wrote = true;
            }
          }
        }
      }

      if(wrote) {
        ctx.flush();
      }
    }

    private void removeHideTeamFromClient(ChannelHandlerContext ctx) {
      if(!hideTeamCreated) {
        return;
      }
      hideTeamCreated = false;
      clientHiddenMembers.clear();
      Object removePkt = NmsBridge.createTeamPacket(HIDE_TEAM_NAME, 1, Optional.empty(), Collections.emptyList());
      if(removePkt != null) {
        ctx.writeAndFlush(removePkt);
      }
    }

    private static void succeedPromise(ChannelPromise promise) {
      if(promise != null && !promise.isVoid()) {
        promise.trySuccess();
      }
    }
  }

  private static final class TabApiBridge {
    private static volatile boolean initialized = false;
    private static volatile boolean available = false;
    private static Method getInstanceMethod;
    private static Method getPlayerByUuidMethod;
    private static Method getNameTagManagerMethod;
    private static Method hideNameTagMethod;
    private static Method showNameTagMethod;
    private static Method hasHiddenNameTagMethod;
    private static final Set<UUID> hiddenViaTab = ConcurrentHashMap.newKeySet();

    private static synchronized void init() {
      if(initialized) {
        return;
      }
      initialized = true;
      try {
        Class<?> tabApiClass = Class.forName("me.neznamy.tab.api.TabAPI");
        Class<?> tabPlayerClass = Class.forName("me.neznamy.tab.api.TabPlayer");
        Class<?> ntManagerClass = Class.forName("me.neznamy.tab.api.nametag.NameTagManager");

        getInstanceMethod = tabApiClass.getMethod("getInstance");
        getPlayerByUuidMethod = tabApiClass.getMethod("getPlayer", UUID.class);
        getNameTagManagerMethod = tabApiClass.getMethod("getNameTagManager");
        hideNameTagMethod = ntManagerClass.getMethod("hideNameTag", tabPlayerClass);
        showNameTagMethod = ntManagerClass.getMethod("showNameTag", tabPlayerClass);
        try {
          hasHiddenNameTagMethod = ntManagerClass.getMethod("hasHiddenNameTag", tabPlayerClass);
        } catch(NoSuchMethodException ignored) {
          hasHiddenNameTagMethod = null;
        }
        available = true;
      } catch(Throwable ignored) {
        available = false;
      }
    }

    private static void tryLateInit() {
      if(!available && Bukkit.getPluginManager().isPluginEnabled("TAB")) {
        initialized = false;
        init();
      }
    }

    static void hide(UUID uuid) {
      if(uuid == null) {
        return;
      }
      tryLateInit();
      if(!available) {
        return;
      }
      try {
        Object api = getInstanceMethod.invoke(null);
        if(api == null) {
          return;
        }
        Object ntManager = getNameTagManagerMethod.invoke(api);
        Object tabPlayer = getPlayerByUuidMethod.invoke(api, uuid);
        if(ntManager == null || tabPlayer == null) {
          return;
        }
        boolean alreadyHidden = false;
        if(hasHiddenNameTagMethod != null) {
          Object res = hasHiddenNameTagMethod.invoke(ntManager, tabPlayer);
          alreadyHidden = Boolean.TRUE.equals(res);
        } else {
          alreadyHidden = hiddenViaTab.contains(uuid);
        }
        hiddenViaTab.add(uuid);
        if(!alreadyHidden) {
          hideNameTagMethod.invoke(ntManager, tabPlayer);
        }
      } catch(Throwable ignored) {
      }
    }

    static void restore(UUID uuid) {
      if(uuid == null || !hiddenViaTab.remove(uuid)) {
        return;
      }
      if(!available) {
        return;
      }
      try {
        Object api = getInstanceMethod.invoke(null);
        if(api == null) {
          return;
        }
        Object ntManager = getNameTagManagerMethod.invoke(api);
        Object tabPlayer = getPlayerByUuidMethod.invoke(api, uuid);
        if(ntManager != null && tabPlayer != null) {
          showNameTagMethod.invoke(ntManager, tabPlayer);
        }
      } catch(Throwable ignored) {
      }
    }

    static void restoreAll() {
      List<UUID> snapshot = new ArrayList<>(hiddenViaTab);
      for(UUID uuid : snapshot) {
        restore(uuid);
      }
    }
  }

  private static final class NmsBridge {
    private static volatile boolean initialized = false;
    private static volatile boolean supported = false;

    private static Class<?> teamPacketClass;
    private static Constructor<?> teamPacketConstructor;
    private static Field teamNameField;
    private static Field methodField;
    private static Field playersField;
    private static Field parametersField;
    private static Optional<?> hideParametersOptional;

    private static Class<?> bundlePacketClass;
    private static Method subPacketsMethod;

    private static volatile Method craftPlayerGetHandle;
    private static volatile Field listenerField;
    private static volatile Field connectionField;
    private static volatile Field channelField;

    static synchronized void init() {
      if(initialized) {
        return;
      }
      initialized = true;
      try {
        teamPacketClass = findClass(
          "net.minecraft.network.protocol.game.ClientboundSetPlayerTeamPacket",
          "net.minecraft.network.protocol.game.PacketPlayOutScoreboardTeam"
        );
        if(teamPacketClass == null) {
          return;
        }

        Class<?> paramsClass = findClass(
          "net.minecraft.network.protocol.game.ClientboundSetPlayerTeamPacket$Parameters",
          "net.minecraft.network.protocol.game.PacketPlayOutScoreboardTeam$b"
        );
        if(paramsClass == null) {
          for(Class<?> inner : teamPacketClass.getDeclaredClasses()) {
            if(!inner.isEnum() && !Modifier.isInterface(inner.getModifiers())) {
              paramsClass = inner;
              break;
            }
          }
        }
        if(paramsClass == null) {
          return;
        }

        teamPacketConstructor = teamPacketClass.getDeclaredConstructor(
          String.class, int.class, Optional.class, Collection.class
        );
        teamPacketConstructor.setAccessible(true);

        for(Field field : teamPacketClass.getDeclaredFields()) {
          if(Modifier.isStatic(field.getModifiers())) {
            continue;
          }
          field.setAccessible(true);
          Class<?> type = field.getType();
          if(type == String.class && teamNameField == null) {
            teamNameField = field;
          } else if(type == int.class && methodField == null) {
            methodField = field;
          } else if(Collection.class.isAssignableFrom(type) && playersField == null) {
            playersField = field;
          } else if(type == Optional.class && parametersField == null) {
            parametersField = field;
          }
        }

        if(teamNameField == null || methodField == null || playersField == null || parametersField == null) {
          return;
        }

        Class<?> scoreboardClass = findClass(
          "net.minecraft.world.scores.Scoreboard",
          "net.minecraft.server.Scoreboard"
        );
        Class<?> playerTeamClass = findClass(
          "net.minecraft.world.scores.PlayerTeam",
          "net.minecraft.world.scores.ScoreboardTeam"
        );
        Class<?> visibilityClass = findClass(
          "net.minecraft.world.scores.Team$Visibility",
          "net.minecraft.world.scores.ScoreboardTeamBase$EnumNameTagVisibility"
        );
        Class<?> collisionRuleClass = findClass(
          "net.minecraft.world.scores.Team$CollisionRule",
          "net.minecraft.world.scores.ScoreboardTeamBase$EnumTeamPush"
        );
        if(scoreboardClass == null || playerTeamClass == null || visibilityClass == null || collisionRuleClass == null) {
          return;
        }

        Constructor<?> sbCtor = scoreboardClass.getDeclaredConstructor();
        sbCtor.setAccessible(true);
        Object dummyScoreboard = sbCtor.newInstance();

        Constructor<?> ptCtor = playerTeamClass.getConstructor(scoreboardClass, String.class);
        Object hideTeam = ptCtor.newInstance(dummyScoreboard, HIDE_TEAM_NAME);

        Object neverVisibility = findEnumConstant(visibilityClass, "NEVER");
        Object neverCollision = findEnumConstant(collisionRuleClass, "NEVER");
        if(neverVisibility == null || neverCollision == null) {
          return;
        }

        invokeSingleArgSetter(playerTeamClass, hideTeam, visibilityClass, neverVisibility);
        invokeSingleArgSetter(playerTeamClass, hideTeam, collisionRuleClass, neverCollision);
        try {
          Method seeFriendly = playerTeamClass.getMethod("setSeeFriendlyInvisibles", boolean.class);
          seeFriendly.invoke(hideTeam, false);
        } catch(NoSuchMethodException ignored) {
          setSecondBooleanFieldFalse(playerTeamClass, hideTeam);
        }

        Constructor<?> paramsCtor = paramsClass.getConstructor(playerTeamClass);
        paramsCtor.setAccessible(true);
        Object hideParams = paramsCtor.newInstance(hideTeam);
        hideParametersOptional = Optional.of(hideParams);

        bundlePacketClass = findClass("net.minecraft.network.protocol.BundlePacket");
        if(bundlePacketClass != null) {
          try {
            subPacketsMethod = bundlePacketClass.getMethod("subPackets");
          } catch(NoSuchMethodException ignored) {
            for(Method m : bundlePacketClass.getMethods()) {
              if(m.getParameterCount() == 0 && Iterable.class.isAssignableFrom(m.getReturnType())) {
                subPacketsMethod = m;
                break;
              }
            }
          }
        }

        supported = true;
      } catch(Throwable ignored) {
        supported = false;
      }
    }

    static boolean isSupported() {
      init();
      return supported;
    }

    static boolean isTeamPacket(Object msg) {
      return supported && teamPacketClass != null && teamPacketClass.isInstance(msg);
    }

    static boolean isBundlePacket(Object msg) {
      return supported && bundlePacketClass != null && subPacketsMethod != null && bundlePacketClass.isInstance(msg);
    }

    static Iterable<?> extractSubPackets(Object bundleMsg) {
      try {
        return (Iterable<?>) subPacketsMethod.invoke(bundleMsg);
      } catch(Throwable ignored) {
        return null;
      }
    }

    static boolean containsTeamPacket(Iterable<?> subPackets) {
      for(Object sub : subPackets) {
        if(isTeamPacket(sub)) {
          return true;
        }
      }
      return false;
    }

    static String getTeamName(Object packet) throws Exception {
      return (String) teamNameField.get(packet);
    }

    static int getMethod(Object packet) throws Exception {
      return methodField.getInt(packet);
    }

    @SuppressWarnings("unchecked")
    static Collection<String> getPlayers(Object packet) throws Exception {
      return (Collection<String>) playersField.get(packet);
    }

    static Optional<?> getParameters(Object packet) throws Exception {
      return (Optional<?>) parametersField.get(packet);
    }

    static Object createHideTeamCreatePacket(Collection<String> entries) {
      return createTeamPacket(HIDE_TEAM_NAME, 0, hideParametersOptional, entries);
    }

    static Object createTeamPacket(String teamName, int method, Optional<?> params, Collection<String> entries) {
      if(!supported || teamPacketConstructor == null) {
        return null;
      }
      try {
        Collection<String> safeEntries = entries != null ? new ArrayList<>(entries) : Collections.emptyList();
        Optional<?> safeParams = params != null ? params : Optional.empty();
        return teamPacketConstructor.newInstance(teamName, method, safeParams, safeEntries);
      } catch(Throwable ignored) {
        return null;
      }
    }

    static Channel extractChannel(Player player) {
      if(player == null) {
        return null;
      }
      try {
        Method handleGetter = craftPlayerGetHandle;
        if(handleGetter == null || !handleGetter.getDeclaringClass().isInstance(player)) {
          handleGetter = player.getClass().getMethod("getHandle");
          craftPlayerGetHandle = handleGetter;
        }
        Object serverPlayer = handleGetter.invoke(player);
        if(serverPlayer == null) {
          return null;
        }

        Field lField = listenerField;
        if(lField == null) {
          lField = findListenerField(serverPlayer.getClass());
          if(lField == null) {
            return null;
          }
          listenerField = lField;
        }
        Object listener = lField.get(serverPlayer);
        if(listener == null) {
          return null;
        }

        Field cField = connectionField;
        if(cField == null) {
          cField = findConnectionField(listener.getClass());
          if(cField == null) {
            return null;
          }
          connectionField = cField;
        }
        Object connection = cField.get(listener);
        if(connection == null) {
          return null;
        }

        Field chField = channelField;
        if(chField == null) {
          chField = findFieldByAssignableType(connection.getClass(), Channel.class);
          if(chField == null) {
            return null;
          }
          channelField = chField;
        }
        return (Channel) chField.get(connection);
      } catch(Throwable ignored) {
        return null;
      }
    }

    private static Field findListenerField(Class<?> serverPlayerClass) {
      try {
        Field direct = serverPlayerClass.getField("connection");
        direct.setAccessible(true);
        return direct;
      } catch(NoSuchFieldException ignored) {
      }
      Class<?> curr = serverPlayerClass;
      while(curr != null && curr != Object.class) {
        for(Field f : curr.getDeclaredFields()) {
          if(Modifier.isStatic(f.getModifiers())) {
            continue;
          }
          String typeName = f.getType().getSimpleName();
          if(typeName.contains("GamePacketListener") || typeName.contains("PlayerConnection")) {
            f.setAccessible(true);
            return f;
          }
        }
        curr = curr.getSuperclass();
      }
      return null;
    }

    private static Field findConnectionField(Class<?> listenerClass) {
      Class<?> connClass = findClass(
        "net.minecraft.network.Connection",
        "net.minecraft.network.NetworkManager"
      );
      if(connClass != null) {
        Field byType = findFieldByAssignableType(listenerClass, connClass);
        if(byType != null) {
          return byType;
        }
      }
      Class<?> curr = listenerClass;
      while(curr != null && curr != Object.class) {
        for(Field f : curr.getDeclaredFields()) {
          if(Modifier.isStatic(f.getModifiers())) {
            continue;
          }
          String simple = f.getType().getSimpleName();
          if("Connection".equals(simple) || "NetworkManager".equals(simple)) {
            f.setAccessible(true);
            return f;
          }
        }
        curr = curr.getSuperclass();
      }
      return null;
    }

    private static Field findFieldByAssignableType(Class<?> startClass, Class<?> targetType) {
      Class<?> curr = startClass;
      while(curr != null && curr != Object.class) {
        for(Field f : curr.getDeclaredFields()) {
          if(!Modifier.isStatic(f.getModifiers()) && targetType.isAssignableFrom(f.getType())) {
            f.setAccessible(true);
            return f;
          }
        }
        curr = curr.getSuperclass();
      }
      return null;
    }

    private static Class<?> findClass(String... names) {
      for(String name : names) {
        try {
          return Class.forName(name);
        } catch(Throwable ignored) {
        }
      }
      return null;
    }

    private static Object findEnumConstant(Class<?> enumClass, String name) {
      Object[] constants = enumClass.getEnumConstants();
      if(constants == null) {
        return null;
      }
      for(Object c : constants) {
        if(c instanceof Enum<?> && ((Enum<?>) c).name().equalsIgnoreCase(name)) {
          return c;
        }
      }
      return null;
    }

    private static void invokeSingleArgSetter(Class<?> ownerClass, Object instance, Class<?> paramType, Object value) throws Exception {
      for(Method m : ownerClass.getMethods()) {
        if(m.getParameterCount() == 1 && m.getParameterTypes()[0] == paramType) {
          m.setAccessible(true);
          m.invoke(instance, value);
          return;
        }
      }
      Class<?> curr = ownerClass;
      while(curr != null && curr != Object.class) {
        for(Field f : curr.getDeclaredFields()) {
          if(!Modifier.isStatic(f.getModifiers()) && f.getType() == paramType) {
            f.setAccessible(true);
            f.set(instance, value);
            return;
          }
        }
        curr = curr.getSuperclass();
      }
    }

    private static void setSecondBooleanFieldFalse(Class<?> playerTeamClass, Object instance) {
      int seen = 0;
      for(Field f : playerTeamClass.getDeclaredFields()) {
        if(!Modifier.isStatic(f.getModifiers()) && f.getType() == boolean.class) {
          seen++;
          if(seen == 2) {
            try {
              f.setAccessible(true);
              f.setBoolean(instance, false);
            } catch(Throwable ignored) {
            }
            return;
          }
        }
      }
    }
  }
}
