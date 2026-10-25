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

package plugily.projects.murdermystery.boot;

import plugily.projects.minigamesbox.classic.handlers.language.Message;
import plugily.projects.minigamesbox.classic.handlers.language.MessageManager;
import plugily.projects.murdermystery.Main;
import plugily.projects.murdermystery.utils.HalloweenChat;

/**
 * @author Tigerpanzer_02
 * <p>
 * Created at 15.10.2022
 */
public class MessageInitializer {
  private final Main plugin;

  public MessageInitializer(Main plugin) {
    this.plugin = plugin;
  }

  public void registerMessages() {
    getMessageManager().registerMessage("", new Message("", ""));
    getMessageManager().registerMessage("SCOREBOARD_ROLES_DETECTIVE", new Message("Scoreboard.Roles.Detective", ""));
    getMessageManager().registerMessage("SCOREBOARD_ROLES_MURDERER", new Message("Scoreboard.Roles.Murderer", ""));
    getMessageManager().registerMessage("SCOREBOARD_ROLES_INNOCENT", new Message("Scoreboard.Roles.Innocent", ""));
    getMessageManager().registerMessage("SCOREBOARD_ROLES_DEAD", new Message("Scoreboard.Roles.Dead", ""));
    getMessageManager().registerMessage("SCOREBOARD_DETECTIVE_ALIVE", new Message("Scoreboard.Detective.Alive", ""));
    getMessageManager().registerMessage("SCOREBOARD_DETECTIVE_BOW_DROPPED", new Message("Scoreboard.Detective.Bow.Dropped", ""));
    getMessageManager().registerMessage("SCOREBOARD_DETECTIVE_BOW_PICKED", new Message("Scoreboard.Detective.Bow.Picked", ""));

    getMessageManager().registerMessage("IN_GAME_MESSAGES_GAME_END_PLACEHOLDERS_MURDERER_STOPPED", new Message("In-Game.Messages.Game-End.Placeholders.Murderer.Stopped", ""));
    getMessageManager().registerMessage("IN_GAME_MESSAGES_GAME_END_PLACEHOLDERS_MURDERER_KILLED_YOU", new Message("In-Game.Messages.Game-End.Placeholders.Murderer.Killed.You", ""));
    getMessageManager().registerMessage("IN_GAME_MESSAGES_GAME_END_PLACEHOLDERS_MURDERER_KILLED_ALL", new Message("In-Game.Messages.Game-End.Placeholders.Murderer.Killed.All", ""));
    getMessageManager().registerMessage("IN_GAME_MESSAGES_GAME_END_PLACEHOLDERS_INNOCENT_KILLED_YOU", new Message("In-Game.Messages.Game-End.Placeholders.Innocent.Killed.You", ""));
    getMessageManager().registerMessage("IN_GAME_MESSAGES_GAME_END_PLACEHOLDERS_INNOCENT_KILLED_WRONGLY", new Message("In-Game.Messages.Game-End.Placeholders.Innocent.Killed.Wrongly", ""));
    getMessageManager().registerMessage("IN_GAME_MESSAGES_GAME_END_PLACEHOLDERS_NOBODY", new Message("In-Game.Messages.Game-End.Placeholders.Nobody", ""));

    getMessageManager().registerMessage("IN_GAME_MESSAGES_ARENA_ROLE_CHANCES_ACTION_BAR", new Message("In-Game.Messages.Arena.Chances.Action-Bar", ""));
    getMessageManager().registerMessage("IN_GAME_MESSAGES_ARENA_COOLDOWN", new Message("In-Game.Messages.Arena.Cooldown", ""));
    getMessageManager().registerMessage("IN_GAME_MESSAGES_ARENA_LOCATOR_BOW", new Message("In-Game.Messages.Arena.Locator.Bow", ""));
    getMessageManager().registerMessage("IN_GAME_MESSAGES_ARENA_LOCATOR_INNOCENT", new Message("In-Game.Messages.Arena.Locator.Innocent", ""));
    getMessageManager().registerMessage("IN_GAME_MESSAGES_ARENA_LOCATOR_WATCH_OUT", new Message("In-Game.Messages.Arena.Locator.Watch-Out", ""));
    getMessageManager().registerMessage("IN_GAME_MESSAGES_ARENA_PASS_NAME", new Message("In-Game.Messages.Arena.Pass.Name", ""));
    getMessageManager().registerMessage("IN_GAME_MESSAGES_ARENA_PASS_ROLE_MURDERER_NAME", new Message("In-Game.Messages.Arena.Pass.Role.Murderer.Name", ""));
    getMessageManager().registerMessage("IN_GAME_MESSAGES_ARENA_PASS_ROLE_MURDERER_LORE", new Message("In-Game.Messages.Arena.Pass.Role.Murderer.Lore", ""));
    getMessageManager().registerMessage("IN_GAME_MESSAGES_ARENA_PASS_ROLE_DETECTIVE_NAME", new Message("In-Game.Messages.Arena.Pass.Role.Detective.Name", ""));
    getMessageManager().registerMessage("IN_GAME_MESSAGES_ARENA_PASS_ROLE_DETECTIVE_LORE", new Message("In-Game.Messages.Arena.Pass.Role.Detective.Lore", ""));
    getMessageManager().registerMessage("IN_GAME_MESSAGES_ARENA_PASS_FAIL", new Message("In-Game.Messages.Arena.Pass.Fail", ""));
    getMessageManager().registerMessage("IN_GAME_MESSAGES_ARENA_PASS_SUCCESS", new Message("In-Game.Messages.Arena.Pass.Success", ""));
    getMessageManager().registerMessage("IN_GAME_MESSAGES_ARENA_PASS_CHANGE", new Message("In-Game.Messages.Arena.Pass.Change", ""));
    getMessageManager().registerMessage("IN_GAME_MESSAGES_ARENA_PLAYING_TIME_LEFT", new Message("In-Game.Messages.Arena.Playing.Time-Left", ""));
    getMessageManager().registerMessage("IN_GAME_MESSAGES_ARENA_PLAYING_ROLE_CHANGE", new Message("In-Game.Messages.Arena.Playing.Role.Change", ""));
    getMessageManager().registerMessage("IN_GAME_MESSAGES_ARENA_PLAYING_ROLE_MURDERER", new Message("In-Game.Messages.Arena.Playing.Role.Murderer", ""));
    getMessageManager().registerMessage("IN_GAME_MESSAGES_ARENA_PLAYING_ROLE_DETECTIVE", new Message("In-Game.Messages.Arena.Playing.Role.Detective", ""));
    getMessageManager().registerMessage("IN_GAME_MESSAGES_ARENA_PLAYING_ROLE_INNOCENT", new Message("In-Game.Messages.Arena.Playing.Role.Innocent", ""));
    getMessageManager().registerMessage("IN_GAME_MESSAGES_ARENA_PLAYING_SCORE_BONUS", new Message("In-Game.Messages.Arena.Playing.Score.Bonus", ""));
    getMessageManager().registerMessage("IN_GAME_MESSAGES_ARENA_PLAYING_SCORE_GOLD", new Message("In-Game.Messages.Arena.Playing.Score.Gold", ""));
    getMessageManager().registerMessage("IN_GAME_MESSAGES_ARENA_PLAYING_SCORE_ACTION_KILL_PLAYER", new Message("In-Game.Messages.Arena.Playing.Score.Action.Kill.Player", ""));
    getMessageManager().registerMessage("IN_GAME_MESSAGES_ARENA_PLAYING_SCORE_ACTION_KILL_MURDERER", new Message("In-Game.Messages.Arena.Playing.Score.Action.Kill.Murderer", ""));
    getMessageManager().registerMessage("IN_GAME_MESSAGES_ARENA_PLAYING_SCORE_ACTION_KILL_INNOCENT", new Message("In-Game.Messages.Arena.Playing.Score.Action.Kill.Innocent", ""));
    getMessageManager().registerMessage("IN_GAME_MESSAGES_ARENA_PLAYING_SCORE_ACTION_PICKUP_GOLD", new Message("In-Game.Messages.Arena.Playing.Score.Action.Pickup.Gold", ""));
    getMessageManager().registerMessage("IN_GAME_MESSAGES_ARENA_PLAYING_SCORE_ACTION_SURVIVING_TIME", new Message("In-Game.Messages.Arena.Playing.Score.Action.Surviving.Time", ""));
    getMessageManager().registerMessage("IN_GAME_MESSAGES_ARENA_PLAYING_SCORE_ACTION_SURVIVING_END", new Message("In-Game.Messages.Arena.Playing.Score.Action.Surviving.End", ""));
    getMessageManager().registerMessage("IN_GAME_MESSAGES_ARENA_PLAYING_SCORE_ACTION_WIN", new Message("In-Game.Messages.Arena.Playing.Score.Action.Win", ""));
    getMessageManager().registerMessage("IN_GAME_MESSAGES_ARENA_PLAYING_SCORE_ACTION_DETECTIVE", new Message("In-Game.Messages.Arena.Playing.Score.Action.Detective", ""));
    getMessageManager().registerMessage("IN_GAME_MESSAGES_ARENA_PLAYING_SWORD_SOON", new Message("In-Game.Messages.Arena.Playing.Sword.Soon", ""));
    getMessageManager().registerMessage("IN_GAME_MESSAGES_ARENA_PLAYING_SPECIAL_BLOCKS_CAULDRON_POTION", new Message("In-Game.Messages.Arena.Playing.Special-Blocks.Cauldron.Potion", ""));
    getMessageManager().registerMessage("IN_GAME_MESSAGES_ARENA_PLAYING_SPECIAL_BLOCKS_CAULDRON_HOLOGRAM", new Message("In-Game.Messages.Arena.Playing.Special-Blocks.Cauldron.Hologram", ""));
    getMessageManager().registerMessage("IN_GAME_MESSAGES_ARENA_PLAYING_SPECIAL_BLOCKS_NOT_ENOUGH_GOLD", new Message("In-Game.Messages.Arena.Playing.Special-Blocks.Not-Enough-Gold", ""));
    getMessageManager().registerMessage("IN_GAME_MESSAGES_ARENA_PLAYING_SPECIAL_BLOCKS_PRAY_HOLOGRAM", new Message("In-Game.Messages.Arena.Playing.Special-Blocks.Pray.Hologram", ""));
    getMessageManager().registerMessage("IN_GAME_MESSAGES_ARENA_PLAYING_SPECIAL_BLOCKS_PRAY_CHAT", new Message("In-Game.Messages.Arena.Playing.Special-Blocks.Pray.Chat", ""));
    getMessageManager().registerMessage("IN_GAME_MESSAGES_ARENA_PLAYING_SPECIAL_BLOCKS_PRAY_PAY", new Message("In-Game.Messages.Arena.Playing.Special-Blocks.Pray.Pay", ""));
    getMessageManager().registerMessage("IN_GAME_MESSAGES_ARENA_PLAYING_SPECIAL_BLOCKS_PRAY_PRAISE_HEARD", new Message("In-Game.Messages.Arena.Playing.Special-Blocks.Pray.Praise.Heard", ""));
    getMessageManager().registerMessage("IN_GAME_MESSAGES_ARENA_PLAYING_SPECIAL_BLOCKS_PRAY_PRAISE_FEELING_BLESSED", new Message("In-Game.Messages.Arena.Playing.Special-Blocks.Pray.Praise.Feeling.Blessed", ""));
    getMessageManager().registerMessage("IN_GAME_MESSAGES_ARENA_PLAYING_SPECIAL_BLOCKS_PRAY_PRAISE_FEELING_CURSED", new Message("In-Game.Messages.Arena.Playing.Special-Blocks.Pray.Praise.Feeling.Cursed", ""));
    getMessageManager().registerMessage("IN_GAME_MESSAGES_ARENA_PLAYING_SPECIAL_BLOCKS_PRAY_PRAISE_GIFTS_DETECTIVE_REVELATION", new Message("In-Game.Messages.Arena.Playing.Special-Blocks.Pray.Praise.Gifts.Detective-Revelation", ""));
    getMessageManager().registerMessage("IN_GAME_MESSAGES_ARENA_PLAYING_SPECIAL_BLOCKS_PRAY_PRAISE_GIFTS_GOLD_RUSH", new Message("In-Game.Messages.Arena.Playing.Special-Blocks.Pray.Praise.Gifts.Gold-Rush", ""));
    getMessageManager().registerMessage("IN_GAME_MESSAGES_ARENA_PLAYING_SPECIAL_BLOCKS_PRAY_PRAISE_GIFTS_SINGLE_COMPENSATION", new Message("In-Game.Messages.Arena.Playing.Special-Blocks.Pray.Praise.Gifts.Single-Compensation", ""));
    getMessageManager().registerMessage("IN_GAME_MESSAGES_ARENA_PLAYING_SPECIAL_BLOCKS_PRAY_PRAISE_GIFTS_BOW", new Message("In-Game.Messages.Arena.Playing.Special-Blocks.Pray.Praise.Gifts.Bow", ""));
    getMessageManager().registerMessage("IN_GAME_MESSAGES_ARENA_PLAYING_SPECIAL_BLOCKS_PRAY_PRAISE_CURSES_SLOWNESS", new Message("In-Game.Messages.Arena.Playing.Special-Blocks.Pray.Praise.Curses.Slowness", ""));
    getMessageManager().registerMessage("IN_GAME_MESSAGES_ARENA_PLAYING_SPECIAL_BLOCKS_PRAY_PRAISE_CURSES_BLINDNESS", new Message("In-Game.Messages.Arena.Playing.Special-Blocks.Pray.Praise.Curses.Blindness", ""));
    getMessageManager().registerMessage("IN_GAME_MESSAGES_ARENA_PLAYING_SPECIAL_BLOCKS_PRAY_PRAISE_CURSES_GOLD", new Message("In-Game.Messages.Arena.Playing.Special-Blocks.Pray.Praise.Curses.Gold", ""));
    getMessageManager().registerMessage("IN_GAME_MESSAGES_ARENA_PLAYING_SPECIAL_BLOCKS_PRAY_PRAISE_CURSES_DEATH", new Message("In-Game.Messages.Arena.Playing.Special-Blocks.Pray.Praise.Curses.Death", ""));
    getMessageManager().registerMessage("IN_GAME_MESSAGES_ARENA_PLAYING_BOW_DROPPED", new Message("In-Game.Messages.Arena.Playing.Bow.Dropped", ""));
    getMessageManager().registerMessage("IN_GAME_MESSAGES_ARENA_PLAYING_BOW_PICKUP", new Message("In-Game.Messages.Arena.Playing.Bow.Pickup", ""));
    getMessageManager().registerMessage("IN_GAME_MESSAGES_ARENA_PLAYING_BOW_SHOT_GOLD", new Message("In-Game.Messages.Arena.Playing.Bow.Shot.Gold", ""));
    getMessageManager().registerMessage("IN_GAME_MESSAGES_ARENA_PLAYING_BOW_SHOT_TITLE", new Message("In-Game.Messages.Arena.Playing.Bow.Shot.Title", ""));
    getMessageManager().registerMessage("LEADERBOARD_STATISTICS_CONTRIBUTION_DETECTIVE", new Message("Leaderboard.Statistics.Detective-Contribution", ""));
    getMessageManager().registerMessage("LEADERBOARD_STATISTICS_CONTRIBUTION_MURDERER", new Message("Leaderboard.Statistics.Murderer-Contribution", ""));

    getMessageManager().registerMessage("LEADERBOARD_STATISTICS_PASS_DETECTIVE", new Message("Leaderboard.Statistics.Detective-Pass", ""));
    getMessageManager().registerMessage("LEADERBOARD_STATISTICS_PASS_MURDERER", new Message("Leaderboard.Statistics.Murderer-Pass", ""));
    getMessageManager().registerMessage("LEADERBOARD_STATISTICS_KILLS", new Message("Leaderboard.Statistics.Kills", ""));
    getMessageManager().registerMessage("LEADERBOARD_STATISTICS_DEATHS", new Message("Leaderboard.Statistics.Deaths", ""));
    getMessageManager().registerMessage("LEADERBOARD_STATISTICS_HIGHEST_SCORE", new Message("Leaderboard.Statistics.Highest-Score", ""));

    getMessageManager().registerMessage("IN_GAME_MESSAGES_ARENA_TASK_BOSSBAR", new Message("In-Game.Messages.Arena.Task.Bossbar",
        HalloweenChat.color("<#5A4E7C>[<#FF7518>Bus Repair<#5A4E7C>] <#F4F1DE>ซ่อมแซมรถบัส <#FFB703>%number%<#5A4E7C>/<#FFB703>%value%")));
    getMessageManager().registerMessage("IN_GAME_MESSAGES_ARENA_TASK_COMPLETED", new Message("In-Game.Messages.Arena.Task.Completed",
        HalloweenChat.color("<#5A4E7C>[<#6EEB83>Task Completed<#5A4E7C>] <#F4F1DE>สำเร็จภารกิจ <#FFB703>%value%")));
    getMessageManager().registerMessage("IN_GAME_MESSAGES_ARENA_TASK_ALL_COMPLETED", new Message("In-Game.Messages.Arena.Task.All-Completed",
        HalloweenChat.color("<#5A4E7C>[<#6EEB83>Bus Repair<#5A4E7C>] <#6EEB83>ซ่อมแซมรถบัสสำเร็จแล้ว ผู้โดยสารหลบหนีและคว้าชัยชนะ")));
    getMessageManager().registerMessage("IN_GAME_MESSAGES_ARENA_TASK_WIN_TITLE", new Message("In-Game.Messages.Arena.Task.Win.Title",
        HalloweenChat.color("<#5A4E7C>[<#6EEB83>ซ่อมแซมรถบัสสำเร็จ<#5A4E7C>]")));
    getMessageManager().registerMessage("IN_GAME_MESSAGES_ARENA_TASK_WIN_SUBTITLE", new Message("In-Game.Messages.Arena.Task.Win.Subtitle",
        HalloweenChat.color("<#B8B0C8>ผู้โดยสารหลบหนีออกจากเมืองได้สำเร็จ")));
    getMessageManager().registerMessage("IN_GAME_MESSAGES_ARENA_TASK_FIND_DIESEL_FUEL", new Message("In-Game.Messages.Arena.Task.Types.Find-Diesel-Fuel", "หาน้ำมันดีเซล"));
    getMessageManager().registerMessage("IN_GAME_MESSAGES_ARENA_TASK_CHECK_CONTROL_PANEL", new Message("In-Game.Messages.Arena.Task.Types.Check-Control-Panel", "ตรวจสอบแผงควบคุมรถบัส"));
    getMessageManager().registerMessage("IN_GAME_MESSAGES_ARENA_TASK_REPAIR_ENGINE_WIRING", new Message("In-Game.Messages.Arena.Task.Types.Repair-Engine-Wiring", "ซ่อมสายไฟเครื่องยนต์"));
    getMessageManager().registerMessage("IN_GAME_MESSAGES_ARENA_TASK_CHECK_RADIATOR_WATER", new Message("In-Game.Messages.Arena.Task.Types.Check-Radiator-Water", "เช็คระดับน้ำในหม้อน้ำ"));
    getMessageManager().registerMessage("IN_GAME_MESSAGES_ARENA_TASK_CHECK_ENGINE_OIL", new Message("In-Game.Messages.Arena.Task.Types.Check-Engine-Oil", "เช็คน้ำมันเครื่อง"));
    getMessageManager().registerMessage("IN_GAME_MESSAGES_ARENA_TASK_CHECK_TIRE_PRESSURE", new Message("In-Game.Messages.Arena.Task.Types.Check-Tire-Pressure", "เช็คลมยาง"));
    getMessageManager().registerMessage("IN_GAME_MESSAGES_ARENA_TASK_CHECK_STEERING_FLUID", new Message("In-Game.Messages.Arena.Task.Types.Check-Steering-Fluid", "เช็คน้ำมันพวงมาลัย"));
    getMessageManager().registerMessage("IN_GAME_MESSAGES_ARENA_TASK_CLEAN_WINDSHIELD", new Message("In-Game.Messages.Arena.Task.Types.Clean-Windshield", "เช็ดคราบโคลนกระจกหน้ารถ"));
    getMessageManager().registerMessage("IN_GAME_MESSAGES_ARENA_TASK_UNLOCK_MANIFOLDS", new Message("In-Game.Messages.Arena.Task.Types.Unlock-Manifolds", "ปลดล็อกท่อแรงดัน"));
    getMessageManager().registerMessage("IN_GAME_MESSAGES_ARENA_TASK_CHARGE_BATTERY", new Message("In-Game.Messages.Arena.Task.Types.Charge-Battery", "พ่วงชาร์จแบตเตอรี่รถบัส"));
    getMessageManager().registerMessage("IN_GAME_MESSAGES_ARENA_TASK_CLEAN_AIR_FILTER", new Message("In-Game.Messages.Arena.Task.Types.Clean-Air-Filter", "ทำความสะอาดไส้กรองอากาศ"));

    getMessageManager().registerMessage("IN_GAME_MESSAGES_ARENA_VOTING_COOLDOWN", new Message("In-Game.Messages.Arena.Voting.Cooldown",
        HalloweenChat.color("<#5A4E7C>[<#FF7518>Emergency Campfire<#5A4E7C>] <#FF3B47>การเรียกประชุมฉุกเฉินยังติดคูลดาวน์อยู่")));
    getMessageManager().registerMessage("IN_GAME_MESSAGES_ARENA_VOTING_BODY_REPORTED", new Message("In-Game.Messages.Arena.Voting.Body-Reported",
        HalloweenChat.color("<#5A4E7C>[<#FF3B47>Dead Body<#5A4E7C>] <#FFB703>%player% <#F4F1DE>พบศพผู้เสียชีวิตและเรียกประชุมด่วน")));
    getMessageManager().registerMessage("IN_GAME_MESSAGES_ARENA_VOTING_DISCUSSION_START", new Message("In-Game.Messages.Arena.Voting.Discussion-Start",
        HalloweenChat.color("<#5A4E7C>[<#FF7518>Emergency Campfire<#5A4E7C>] <#FFB703>%player% <#F4F1DE>เรียกประชุมฉุกเฉิน เวลาปรึกษา <#FFB703>%number% วิ")));
    getMessageManager().registerMessage("IN_GAME_MESSAGES_ARENA_VOTING_VOTE_NOW", new Message("In-Game.Messages.Arena.Voting.Vote-Now",
        HalloweenChat.color("<#5A4E7C>[<#FF7518>Voting<#5A4E7C>] <#F4F1DE>คลิกเลือกผู้ต้องสงสัยเพื่อโหวตคัดออก <#5A4E7C>(<#FFB703>%number% วิ<#5A4E7C>)")));
    getMessageManager().registerMessage("IN_GAME_MESSAGES_ARENA_VOTING_DISCUSSION_TIMER", new Message("In-Game.Messages.Arena.Voting.Discussion-Timer",
        HalloweenChat.color("<#5A4E7C>[<#FF7518>Emergency Campfire<#5A4E7C>] <#F4F1DE>เวลาปรึกษา <#FFB703>%number% วิ")));
    getMessageManager().registerMessage("IN_GAME_MESSAGES_ARENA_VOTING_VOTE_TIMER", new Message("In-Game.Messages.Arena.Voting.Vote-Timer",
        HalloweenChat.color("<#5A4E7C>[<#FF7518>Voting<#5A4E7C>] <#F4F1DE>เวลาโหวต <#FFB703>%number% วิ")));
    getMessageManager().registerMessage("IN_GAME_MESSAGES_ARENA_VOTING_ALREADY_VOTED", new Message("In-Game.Messages.Arena.Voting.Already-Voted",
        HalloweenChat.color("<#5A4E7C>[<#FF7518>Voting<#5A4E7C>] <#FF3B47>คุณได้ยืนยันการลงคะแนนโหวตไปแล้ว")));
    getMessageManager().registerMessage("IN_GAME_MESSAGES_ARENA_VOTING_PLAYER_VOTED", new Message("In-Game.Messages.Arena.Voting.Player-Voted",
        HalloweenChat.color("<#5A4E7C>[<#FF7518>Voting<#5A4E7C>] <#FFB703>%player% <#F4F1DE>ได้ยืนยันการลงคะแนนโหวตแล้ว")));
    getMessageManager().registerMessage("IN_GAME_MESSAGES_ARENA_VOTING_NO_EJECT", new Message("In-Game.Messages.Arena.Voting.No-Eject",
        HalloweenChat.color("<#5A4E7C>[<#FF7518>Voting<#5A4E7C>] <#F4F1DE>ไม่มีใครถูกโหวตคัดออกในรอบนี้ <#5A4E7C>(<#B8B0C8>คะแนนโหวตเสมอ หรือเสียงส่วนใหญ่เลือกข้าม<#5A4E7C>)")));
    getMessageManager().registerMessage("IN_GAME_MESSAGES_ARENA_VOTING_NO_EJECT_TITLE", new Message("In-Game.Messages.Arena.Voting.No-Eject-Title",
        HalloweenChat.color("<#5A4E7C>[<#FF7518>ไม่มีใครถูกโหวตออก<#5A4E7C>]")));
    getMessageManager().registerMessage("IN_GAME_MESSAGES_ARENA_VOTING_NO_EJECT_SUBTITLE", new Message("In-Game.Messages.Arena.Voting.No-Eject-Subtitle",
        HalloweenChat.color("<#B8B0C8>คะแนนโหวตเสมอ หรือเสียงส่วนใหญ่เลือกข้าม")));
    getMessageManager().registerMessage("IN_GAME_MESSAGES_ARENA_VOTING_EJECTED", new Message("In-Game.Messages.Arena.Voting.Ejected",
        HalloweenChat.color("<#5A4E7C>[<#FF3B47>Voting<#5A4E7C>] <#FFB703>%player% <#F4F1DE>ถูกโหวตคัดออก %value%")));
    getMessageManager().registerMessage("IN_GAME_MESSAGES_ARENA_VOTING_WAS_IMPOSTOR", new Message("In-Game.Messages.Arena.Voting.Was-Impostor",
        HalloweenChat.color("<#5A4E7C>(<#6EEB83>เขาคือฆาตกร<#5A4E7C>)")));
    getMessageManager().registerMessage("IN_GAME_MESSAGES_ARENA_VOTING_WAS_NOT_IMPOSTOR", new Message("In-Game.Messages.Arena.Voting.Was-Not-Impostor",
        HalloweenChat.color("<#5A4E7C>(<#FF3B47>เขาไม่ใช่ฆาตกร<#5A4E7C>)")));
  }

  private MessageManager getMessageManager() {
    return plugin.getMessageManager();
  }

}
