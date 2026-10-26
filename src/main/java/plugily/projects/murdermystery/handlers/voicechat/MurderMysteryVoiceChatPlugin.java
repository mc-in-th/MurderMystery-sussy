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

package plugily.projects.murdermystery.handlers.voicechat;

import de.maxhenkel.voicechat.api.BukkitVoicechatService;
import de.maxhenkel.voicechat.api.VoicechatConnection;
import de.maxhenkel.voicechat.api.VoicechatPlugin;
import de.maxhenkel.voicechat.api.VoicechatServerApi;
import de.maxhenkel.voicechat.api.events.EntitySoundPacketEvent;
import de.maxhenkel.voicechat.api.events.EventRegistration;
import de.maxhenkel.voicechat.api.events.LocationalSoundPacketEvent;
import de.maxhenkel.voicechat.api.events.MicrophonePacketEvent;
import de.maxhenkel.voicechat.api.events.SoundPacketEvent;
import de.maxhenkel.voicechat.api.events.StaticSoundPacketEvent;
import de.maxhenkel.voicechat.api.packets.StaticSoundPacket;
import org.bukkit.entity.Player;
import plugily.projects.minigamesbox.api.arena.IArenaState;
import plugily.projects.murdermystery.Main;
import plugily.projects.murdermystery.arena.Arena;

import java.util.ArrayList;
import java.util.List;

public class MurderMysteryVoiceChatPlugin implements VoicechatPlugin {

  private final Main plugin;
  private volatile boolean active = true;

  public MurderMysteryVoiceChatPlugin(Main plugin) {
    this.plugin = plugin;
  }

  public void deactivate() {
    this.active = false;
  }

  public static Object register(Main plugin) {
    BukkitVoicechatService service = plugin.getServer().getServicesManager().load(BukkitVoicechatService.class);
    if(service != null) {
      MurderMysteryVoiceChatPlugin instance = new MurderMysteryVoiceChatPlugin(plugin);
      service.registerPlugin(instance);
      plugin.getLogger().info("Successfully hooked into Simple Voice Chat for spectator voice isolation.");
      return instance;
    }
    return null;
  }

  @Override
  public String getPluginId() {
    return "murdermystery";
  }

  @Override
  public void registerEvents(EventRegistration registration) {
    registration.registerEvent(MicrophonePacketEvent.class, this::onMicrophonePacket);
    registration.registerEvent(EntitySoundPacketEvent.class, this::onSoundPacket);
    registration.registerEvent(LocationalSoundPacketEvent.class, this::onSoundPacket);
    registration.registerEvent(StaticSoundPacketEvent.class, this::onSoundPacket);
  }

  private void onMicrophonePacket(MicrophonePacketEvent event) {
    if(!active) {
      return;
    }
    try {
      VoicechatConnection senderConnection = event.getSenderConnection();
      if(senderConnection == null || senderConnection.getPlayer() == null) {
        return;
      }
      if(!(senderConnection.getPlayer().getPlayer() instanceof Player)) {
        return;
      }
      Player speaker = (Player) senderConnection.getPlayer().getPlayer();
      Arena arena = plugin.getArenaRegistry().getArena(speaker);
      if(arena == null || arena.getArenaState() != IArenaState.IN_GAME) {
        return;
      }

      if(!isDeadOrSpectator(speaker, arena)) {
        return;
      }

      event.cancel();

      VoicechatServerApi api = event.getVoicechat();
      StaticSoundPacket spectatorPacket = event.getPacket().toStaticSoundPacket();
      List<Player> arenaPlayers = new ArrayList<>(arena.getPlayers());
      for(Player target : arenaPlayers) {
        if(target == null || target.equals(speaker)) {
          continue;
        }
        if(!isDeadOrSpectator(target, arena)) {
          continue;
        }
        VoicechatConnection targetConnection = api.getConnectionOf(target.getUniqueId());
        if(targetConnection != null && targetConnection.isConnected() && !targetConnection.isDisabled()) {
          api.sendStaticSoundPacketTo(targetConnection, spectatorPacket);
        }
      }
    } catch(Throwable ignored) {
    }
  }

  private void onSoundPacket(SoundPacketEvent<?> event) {
    if(!active) {
      return;
    }
    try {
      VoicechatConnection senderConnection = event.getSenderConnection();
      VoicechatConnection receiverConnection = event.getReceiverConnection();
      if(senderConnection == null || receiverConnection == null) {
        return;
      }
      if(senderConnection.getPlayer() == null || receiverConnection.getPlayer() == null) {
        return;
      }
      if(!(senderConnection.getPlayer().getPlayer() instanceof Player)
          || !(receiverConnection.getPlayer().getPlayer() instanceof Player)) {
        return;
      }

      Player speaker = (Player) senderConnection.getPlayer().getPlayer();
      Player listener = (Player) receiverConnection.getPlayer().getPlayer();
      Arena speakerArena = plugin.getArenaRegistry().getArena(speaker);
      Arena listenerArena = plugin.getArenaRegistry().getArena(listener);

      if(speakerArena == null && listenerArena == null) {
        return;
      }

      if(speakerArena != listenerArena) {
        event.cancel();
        return;
      }

      if(speakerArena.getArenaState() != IArenaState.IN_GAME) {
        return;
      }

      if(isDeadOrSpectator(speaker, speakerArena) && !isDeadOrSpectator(listener, speakerArena)) {
        event.cancel();
      }
    } catch(Throwable ignored) {
    }
  }

  private boolean isDeadOrSpectator(Player player, Arena arena) {
    return arena.isDeathPlayer(player) || arena.isSpectatorPlayer(player);
  }
}
