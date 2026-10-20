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

import net.raphimc.noteblocklib.NoteBlockLib;
import net.raphimc.noteblocklib.format.SongFormat;
import net.raphimc.noteblocklib.format.minecraft.MinecraftDefinitions;
import net.raphimc.noteblocklib.format.minecraft.MinecraftInstrument;
import net.raphimc.noteblocklib.format.nbs.model.NbsCustomInstrument;
import net.raphimc.noteblocklib.model.note.Instrument;
import net.raphimc.noteblocklib.model.note.Note;
import net.raphimc.noteblocklib.model.song.Song;
import net.raphimc.noteblocklib.player.SongPlayer;
import org.bukkit.Bukkit;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;
import plugily.projects.murdermystery.arena.Arena;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class NoteBlockMusicManager {

  private final Arena arena;
  private final File soundsDir;
  private Song bodyReportSong;
  private Song emergencyMeetingSong;
  private final List<SongPlayer> activePlayers = new ArrayList<>();

  public NoteBlockMusicManager(Arena arena) {
    this.arena = arena;
    this.soundsDir = new File(arena.getPlugin().getDataFolder(), "sounds");
    initSongsFromResources();
  }

  private void initSongsFromResources() {
    try {
      if(!soundsDir.exists()) {
        soundsDir.mkdirs();
      }
      if(findSongFile("body_report") == null) {
        arena.getPlugin().saveResource("sounds/body_report.nbs", false);
      }
      if(findSongFile("emergency_meeting") == null) {
        arena.getPlugin().saveResource("sounds/emergency_meeting.nbs", false);
      }
    } catch(Throwable ignored) {
    }
    bodyReportSong = loadSong("body_report", "sounds/body_report.nbs");
    emergencyMeetingSong = loadSong("emergency_meeting", "sounds/emergency_meeting.nbs");
  }

  private Song loadSong(String baseName, String resourceFallback) {
    File externalFile = findSongFile(baseName);
    if(externalFile != null) {
      try {
        return NoteBlockLib.readSong(externalFile);
      } catch(Throwable ignored) {
      }
    }
    try(InputStream in = arena.getPlugin().getResource(resourceFallback)) {
      if(in != null) {
        return NoteBlockLib.readSong(in, SongFormat.NBS);
      }
    } catch(Throwable ignored) {
    }
    return null;
  }

  private File findSongFile(String baseName) {
    String[] extensions = {".nbs", ".mid", ".midi", ".txt", ".mcsp2"};
    for(String ext : extensions) {
      File candidate = new File(soundsDir, baseName + ext);
      if(candidate.exists() && candidate.isFile()) {
        return candidate;
      }
    }
    return null;
  }

  public void playBodyReportSound() {
    stopAll();
    Song loaded = loadSong("body_report", "sounds/body_report.nbs");
    if(loaded != null) {
      bodyReportSong = loaded;
    }
    if(bodyReportSong != null) {
      startSong(bodyReportSong);
    }
  }

  public void playEmergencyMeetingSound() {
    stopAll();
    Song loaded = loadSong("emergency_meeting", "sounds/emergency_meeting.nbs");
    if(loaded != null) {
      emergencyMeetingSong = loaded;
    }
    if(emergencyMeetingSong != null) {
      startSong(emergencyMeetingSong);
    }
  }

  private void startSong(Song song) {
    try {
      ArenaSongPlayer player = new ArenaSongPlayer(song.copy());
      synchronized(activePlayers) {
        activePlayers.add(player);
      }
      player.start();
    } catch(Throwable ignored) {
    }
  }

  public void stopAll() {
    synchronized(activePlayers) {
      for(SongPlayer sp : new ArrayList<>(activePlayers)) {
        try {
          if(sp.isRunning()) {
            sp.stop();
          }
        } catch(Throwable ignored) {
        }
      }
      activePlayers.clear();
    }
  }

  private class ArenaSongPlayer extends SongPlayer {

    public ArenaSongPlayer(Song song) {
      super(song);
    }

    @Override
    protected void playNotes(List<Note> notes) {
      if(notes == null || notes.isEmpty() || !arena.getPlugin().isEnabled()) {
        return;
      }
      List<Note> snapshot = new ArrayList<>(notes.size());
      for(Note n : notes) {
        if(n != null) {
          snapshot.add(n.copy());
        }
      }
      Bukkit.getScheduler().runTask(arena.getPlugin(), () -> {
        Set<Player> players = arena.getPlayers();
        if(players.isEmpty()) {
          return;
        }
        for(Note note : snapshot) {
          if(note.isOutsideMinecraftOctaveRange()) {
            MinecraftDefinitions.instrumentShiftNote(note);
            MinecraftDefinitions.clampNoteKey(note);
          }
          String soundName = resolveSoundName(note.getInstrument());
          float volume = Math.max(0.05F, Math.min(1.0F, note.getVolume()));
          float pitch = Math.max(0.5F, Math.min(2.0F, note.getPitch()));
          for(Player player : players) {
            if(player != null && player.isOnline()) {
              try {
                player.playSound(player.getLocation(), soundName, SoundCategory.RECORDS, volume, pitch);
              } catch(Throwable ignored) {
              }
            }
          }
        }
      });
    }

    @Override
    protected void onSongFinished() {
      super.onSongFinished();
      synchronized(activePlayers) {
        activePlayers.remove(this);
      }
    }

    private String resolveSoundName(Instrument instrument) {
      if(instrument instanceof MinecraftInstrument) {
        return ((MinecraftInstrument) instrument).mcSoundName();
      }
      if(instrument instanceof NbsCustomInstrument) {
        return ((NbsCustomInstrument) instrument).getSoundFilePathOr("block.note_block.harp");
      }
      return "block.note_block.harp";
    }
  }
}
