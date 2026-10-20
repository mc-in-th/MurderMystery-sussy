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
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import plugily.projects.minigamesbox.api.user.IUser;
import plugily.projects.minigamesbox.classic.handlers.language.MessageBuilder;
import plugily.projects.minigamesbox.classic.utils.version.VersionUtils;
import plugily.projects.minigamesbox.classic.utils.version.xseries.XMaterial;
import plugily.projects.minigamesbox.classic.utils.version.xseries.XSound;
import plugily.projects.murdermystery.arena.Arena;
import plugily.projects.murdermystery.arena.ArenaUtils;
import plugily.projects.murdermystery.arena.role.Role;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class VotingManager {

  private final Arena arena;
  private VoteState voteState = VoteState.NONE;
  private int voteTimer = 0;
  private Player reporter;
  private final Map<Player, Player> votes = new HashMap<>();
  private final Set<Player> skipVotes = new HashSet<>();
  private final Map<Player, Location> preVoteLocations = new HashMap<>();
  private long lastVoteEndTime = 0;

  private static final String VOTE_GUI_TITLE = ChatColor.DARK_RED + "Vote to Eject";
  private static final int SKIP_SLOT = 22;

  public VotingManager(Arena arena) {
    this.arena = arena;
  }

  public boolean startVoting(Player reportingPlayer) {
    if(voteState != VoteState.NONE) {
      return false;
    }

    long cooldownMs = arena.getPlugin().getConfig().getInt("Voting.Cooldown", 30) * 1000L;
    if(System.currentTimeMillis() - lastVoteEndTime < cooldownMs) {
      new MessageBuilder("IN_GAME_MESSAGES_ARENA_VOTING_COOLDOWN").asKey()
        .player(reportingPlayer).arena(arena).sendPlayer();
      return false;
    }

    this.reporter = reportingPlayer;
    this.votes.clear();
    this.skipVotes.clear();
    this.preVoteLocations.clear();

    if(arena.getSabotageManager() != null) {
      arena.getSabotageManager().cancelActiveSabotage("VOTING");
    }

    XSound.ENTITY_LIGHTNING_BOLT_THUNDER.play(reportingPlayer.getLocation(), 1F, 1F);
    new MessageBuilder("IN_GAME_MESSAGES_ARENA_VOTING_BODY_REPORTED")
      .asKey().player(reportingPlayer).arena(arena).sendArena();

    Location meetingPoint = arena.getPlayerSpawnPoints().get(0);
    for(Player player : arena.getPlayersLeft()) {
      if(arena.getVentManager().isInsideVent(player)) {
        arena.getVentManager().exitVent(player);
      }
      preVoteLocations.put(player, player.getLocation().clone());
      VersionUtils.teleport(player, meetingPoint);
      player.setWalkSpeed(0.0f);
      player.setFoodLevel(20);
    }

    for(Player player : arena.getPlayers()) {
      IUser user = arena.getPlugin().getUserManager().getUser(player);
      if(user.isSpectator()) {
        VersionUtils.teleport(player, meetingPoint);
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
          openVotingGUI();

          new MessageBuilder("IN_GAME_MESSAGES_ARENA_VOTING_VOTE_NOW")
            .asKey().arena(arena).integer(voteTimer).sendArena();
        } else if(voteTimer % 5 == 0 || voteTimer <= 5) {
          for(Player player : arena.getPlayersLeft()) {
            VersionUtils.sendActionBar(player,
              new MessageBuilder("IN_GAME_MESSAGES_ARENA_VOTING_DISCUSSION_TIMER")
                .asKey().integer(voteTimer).build());
          }
        }
        break;

      case VOTING:
        if(voteTimer <= 0 || hasEveryoneVoted()) {
          resolveVotes();
        } else if(voteTimer % 5 == 0 || voteTimer <= 5) {
          for(Player player : arena.getPlayersLeft()) {
            VersionUtils.sendActionBar(player,
              new MessageBuilder("IN_GAME_MESSAGES_ARENA_VOTING_VOTE_TIMER")
                .asKey().integer(voteTimer).build());
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

  private void openVotingGUI() {
    List<Player> alivePlayers = arena.getPlayersLeft();
    int inventorySize = 27;

    for(Player viewer : alivePlayers) {
      Inventory gui = Bukkit.createInventory(null, inventorySize, VOTE_GUI_TITLE);
      IUser viewerUser = arena.getPlugin().getUserManager().getUser(viewer);

      int slot = 0;
      for(Player target : alivePlayers) {
        if(slot >= 18) {
          break;
        }

        ItemStack head = XMaterial.PLAYER_HEAD.parseItem();
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        meta = VersionUtils.setPlayerHead(target, meta);
        meta.setDisplayName(ChatColor.YELLOW + target.getName());

        if(Role.isRole(Role.MURDERER, viewerUser, arena)) {
          IUser targetUser = arena.getPlugin().getUserManager().getUser(target);
          if(Role.isRole(Role.MURDERER, targetUser, arena)) {
            meta.setDisplayName(ChatColor.RED + target.getName() + " (Impostor)");
          }
        }

        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "Click to vote for this player");
        meta.setLore(lore);
        head.setItemMeta(meta);
        gui.setItem(slot, head);
        slot++;
      }

      ItemStack skipItem = new ItemStack(Material.BARRIER);
      ItemMeta skipMeta = skipItem.getItemMeta();
      skipMeta.setDisplayName(ChatColor.RED + "Skip Vote");
      List<String> skipLore = new ArrayList<>();
      skipLore.add(ChatColor.GRAY + "Click to skip your vote");
      skipMeta.setLore(skipLore);
      skipItem.setItemMeta(skipMeta);
      gui.setItem(SKIP_SLOT, skipItem);

      viewer.openInventory(gui);
    }
  }

  public boolean handleVoteClick(Player voter, int slot) {
    if(voteState != VoteState.VOTING) {
      return false;
    }

    if(votes.containsKey(voter) || skipVotes.contains(voter)) {
      new MessageBuilder("IN_GAME_MESSAGES_ARENA_VOTING_ALREADY_VOTED")
        .asKey().player(voter).sendPlayer();
      return true;
    }

    if(slot == SKIP_SLOT) {
      skipVotes.add(voter);
      voter.closeInventory();
      XSound.UI_BUTTON_CLICK.play(voter.getLocation(), 1F, 1F);

      new MessageBuilder("IN_GAME_MESSAGES_ARENA_VOTING_PLAYER_VOTED")
        .asKey().player(voter).arena(arena).sendArena();

      if(hasEveryoneVoted()) {
        voteTimer = 0;
      }
      return true;
    }

    List<Player> alivePlayers = arena.getPlayersLeft();
    if(slot >= 0 && slot < alivePlayers.size()) {
      Player target = alivePlayers.get(slot);
      votes.put(voter, target);
      voter.closeInventory();
      XSound.UI_BUTTON_CLICK.play(voter.getLocation(), 1F, 1F);

      new MessageBuilder("IN_GAME_MESSAGES_ARENA_VOTING_PLAYER_VOTED")
        .asKey().player(voter).arena(arena).sendArena();

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

      ejectedPlayer.damage(1000.0);

      if(wasMurderer && arena.aliveMurderer() <= 0) {
        ArenaUtils.onMurdererDeath(arena);
      }
    }
  }

  private void endVoting() {
    voteState = VoteState.NONE;
    lastVoteEndTime = System.currentTimeMillis();

    for(Player player : arena.getPlayersLeft()) {
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
    reporter = null;
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
    return VOTE_GUI_TITLE.equals(title);
  }

  public void handlePlayerLeave(Player player) {
    votes.remove(player);
    skipVotes.remove(player);
    preVoteLocations.remove(player);
    votes.values().removeIf(target -> target.equals(player));
  }

  public void cleanup() {
    voteState = VoteState.NONE;
    voteTimer = 0;
    votes.clear();
    skipVotes.clear();
    preVoteLocations.clear();
    reporter = null;
    lastVoteEndTime = 0;
  }
}
