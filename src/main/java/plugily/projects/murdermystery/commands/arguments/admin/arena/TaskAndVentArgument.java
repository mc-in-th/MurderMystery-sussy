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

package plugily.projects.murdermystery.commands.arguments.admin.arena;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import plugily.projects.minigamesbox.api.arena.IPluginArena;
import plugily.projects.minigamesbox.classic.commands.arguments.data.CommandArgument;
import plugily.projects.minigamesbox.classic.commands.arguments.data.LabelData;
import plugily.projects.minigamesbox.classic.commands.arguments.data.LabeledCommandArgument;
import plugily.projects.minigamesbox.classic.utils.configuration.ConfigUtils;
import plugily.projects.minigamesbox.classic.utils.serialization.LocationSerializer;
import plugily.projects.murdermystery.arena.Arena;
import plugily.projects.murdermystery.arena.cutscene.CutsceneManager;
import plugily.projects.murdermystery.arena.sabotage.SabotageType;
import plugily.projects.murdermystery.arena.special.SpecialBlock;
import plugily.projects.murdermystery.arena.task.TaskType;
import plugily.projects.murdermystery.commands.arguments.ArgumentsRegistry;
import plugily.projects.murdermystery.utils.HalloweenChat;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class TaskAndVentArgument {

  private static void msg(CommandSender sender, String text) {
    sender.sendMessage(HalloweenChat.color(text));
  }

  public TaskAndVentArgument(ArgumentsRegistry registry) {
    registry.mapArgument(
      "murdermysteryadmin",
      new LabeledCommandArgument(
        "addtask",
        "murdermystery.admin.addtask",
        CommandArgument.ExecutorType.PLAYER,
        new LabelData(
          "/mma addtask <arena> [taskType]",
          "/mma addtask <arena> [taskType]",
          "&7เพิ่มจุดภารกิจ (Task Point) ที่บล็อกที่คุณมองอยู่หรือจุดที่คุณยืน\n&6Permission: &7murdermystery.admin.addtask")) {
        @Override
        public void execute(CommandSender sender, String[] args) {
          Player player = (Player) sender;
          if(args.length < 2) {
            msg(player, "<#FF3B47>การใช้งาน: /mma addtask <ชื่อห้อง/arena> [ประเภทภารกิจ/taskType]");
            sendTaskTypesHelp(player);
            return;
          }
          String arenaId = args[1];
          Arena arena = (Arena) registry.getPlugin().getArenaRegistry().getArena(arenaId);
          if(arena == null) {
            msg(player, "<#FF3B47>ไม่พบห้องชื่อ " + arenaId);
            return;
          }

          Block targetBlock = player.getTargetBlock(null, 5);
          Location loc = (targetBlock != null && targetBlock.getType() != Material.AIR)
            ? targetBlock.getLocation()
            : player.getLocation().getBlock().getLocation();

          FileConfiguration config = ConfigUtils.getConfig(registry.getPlugin(), "arenas");
          String serialized = LocationSerializer.locationToString(loc);

          if(args.length >= 3) {
            TaskType type = TaskType.fromInput(args[2]);
            if(type == null) {
              msg(player, "<#FF3B47>ไม่พบประเภทภารกิจ: <#FFB703>" + args[2]);
              sendTaskTypesHelp(player);
              return;
            }

            List<String> list = config.getStringList("instances." + arenaId + ".tasks." + type.name());
            if(!list.contains(serialized)) {
              list.add(serialized);
            }
            config.set("instances." + arenaId + ".tasks." + type.name(), list);
            ConfigUtils.saveConfig(registry.getPlugin(), config, "arenas");

            arena.addSpecificTaskLocation(type, loc);

            msg(player, "<#6EEB83>[+] ตั้งจุดภารกิจ <#FF7518>" + type.getIcon() + " " + type.getDisplayName()
              + " <#6EEB83>ให้ห้อง <#FFB703>" + arenaId + " <#6EEB83>เรียบร้อยแล้วที่ <#F4F1DE>"
              + loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ()
              + " <#6EEB83>(รวมเฉพาะภารกิจนี้ <#FFB703>" + list.size() + " <#6EEB83>จุด)");
          } else {
            List<String> list = config.getStringList("instances." + arenaId + ".tasklocations");
            if(!list.contains(serialized)) {
              list.add(serialized);
            }
            config.set("instances." + arenaId + ".tasklocations", list);
            ConfigUtils.saveConfig(registry.getPlugin(), config, "arenas");

            List<Location> currentLocs = new ArrayList<>(arena.getTaskLocations());
            if(!currentLocs.contains(loc)) {
              currentLocs.add(loc);
            }
            arena.setTaskLocations(currentLocs);

            msg(player, "<#6EEB83>[+] เพิ่มจุดภารกิจทั่วไป (Generic Task Point) ให้ห้อง <#FFB703>" + arenaId + " <#6EEB83>เรียบร้อยแล้วที่ <#F4F1DE>"
              + loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ()
              + " <#6EEB83>(รวมทั้งหมด <#FFB703>" + list.size() + " <#6EEB83>จุด)");
            msg(player, "<#B8B0C8>Tip: สามารถระบุภารกิจเจาะจงได้ เช่น: <#FFB703>/mma settask " + arenaId + " diesel");
          }
        }
      }
    );

    registry.mapArgument(
      "murdermysteryadmin",
      new LabeledCommandArgument(
        "settask",
        "murdermystery.admin.addtask",
        CommandArgument.ExecutorType.PLAYER,
        new LabelData(
          "/mma settask <arena> <taskType>",
          "/mma settask <arena> <taskType>",
          "&7ตั้งจุดสำหรับภารกิจที่เจาะจง (เช่น diesel, wiring, panel)\n&6Permission: &7murdermystery.admin.addtask")) {
        @Override
        public void execute(CommandSender sender, String[] args) {
          Player player = (Player) sender;
          if(args.length < 3) {
            msg(player, "<#FF3B47>การใช้งาน: /mma settask <ชื่อห้อง/arena> <ประเภทภารกิจ/taskType>");
            sendTaskTypesHelp(player);
            return;
          }
          String arenaId = args[1];
          Arena arena = (Arena) registry.getPlugin().getArenaRegistry().getArena(arenaId);
          if(arena == null) {
            msg(player, "<#FF3B47>ไม่พบห้องชื่อ " + arenaId);
            return;
          }

          TaskType type = TaskType.fromInput(args[2]);
          if(type == null) {
            msg(player, "<#FF3B47>ไม่พบประเภทภารกิจ: <#FFB703>" + args[2]);
            sendTaskTypesHelp(player);
            return;
          }

          Block targetBlock = player.getTargetBlock(null, 5);
          Location loc = (targetBlock != null && targetBlock.getType() != Material.AIR)
            ? targetBlock.getLocation()
            : player.getLocation().getBlock().getLocation();

          FileConfiguration config = ConfigUtils.getConfig(registry.getPlugin(), "arenas");
          String serialized = LocationSerializer.locationToString(loc);
          List<String> list = config.getStringList("instances." + arenaId + ".tasks." + type.name());
          if(!list.contains(serialized)) {
            list.add(serialized);
          }
          config.set("instances." + arenaId + ".tasks." + type.name(), list);
          ConfigUtils.saveConfig(registry.getPlugin(), config, "arenas");

          arena.addSpecificTaskLocation(type, loc);

          msg(player, "<#6EEB83>[+] ตั้งจุดภารกิจ <#FF7518>" + type.getIcon() + " " + type.getDisplayName()
            + " <#6EEB83>ให้ห้อง <#FFB703>" + arenaId + " <#6EEB83>เรียบร้อยแล้วที่ <#F4F1DE>"
            + loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ()
            + " <#6EEB83>(รวมเฉพาะภารกิจนี้ <#FFB703>" + list.size() + " <#6EEB83>จุด)");
        }
      }
    );

    registry.mapArgument(
      "murdermysteryadmin",
      new LabeledCommandArgument(
        "listtasks",
        "murdermystery.admin.addtask",
        CommandArgument.ExecutorType.PLAYER,
        new LabelData(
          "/mma listtasks <arena>",
          "/mma listtasks <arena>",
          "&7ดูรายการจุดภารกิจทั้งหมดที่ตั้งไว้ในห้อง\n&6Permission: &7murdermystery.admin.addtask")) {
        @Override
        public void execute(CommandSender sender, String[] args) {
          Player player = (Player) sender;
          if(args.length < 2) {
            msg(player, "<#FF3B47>การใช้งาน: /mma listtasks <ชื่อห้อง/arena>");
            return;
          }
          String arenaId = args[1];
          Arena arena = (Arena) registry.getPlugin().getArenaRegistry().getArena(arenaId);
          if(arena == null) {
            msg(player, "<#FF3B47>ไม่พบห้องชื่อ " + arenaId);
            return;
          }

          msg(player, "<#5A4E7C>================= [ <gradient:#FF5400:#FFB703><bold>รายการจุดภารกิจ: " + arenaId + "</bold></gradient> <#5A4E7C>] =================");
          int idx = 1;
          for(TaskType type : TaskType.values()) {
            List<Location> locs = arena.getTaskLocations(type);
            String visualTag = type.isVisual() ? " <#72EFDD>[Visual]" : "";
            if(locs.isEmpty()) {
              msg(player, "<#FFB703>" + idx + ". " + type.getIcon() + " " + type.getDisplayName() + visualTag
                + " <#B8B0C8>(" + type.getShortId() + "): <#FF3B47>ยังไม่ได้ตั้งค่า (จะสุ่มจุดทั่วไป)");
            } else {
              msg(player, "<#FFB703>" + idx + ". " + type.getIcon() + " " + type.getDisplayName() + visualTag
                + " <#B8B0C8>(" + type.getShortId() + "): <#6EEB83>" + locs.size() + " จุด");
              for(Location l : locs) {
                msg(player, "   <#B8B0C8>+- <#F4F1DE>" + l.getBlockX() + ", " + l.getBlockY() + ", " + l.getBlockZ());
              }
            }
            idx++;
          }
          List<Location> genericLocs = arena.getTaskLocations();
          msg(player, "<#72EFDD>จุดภารกิจทั่วไป (Generic Task Points): "
            + (genericLocs.isEmpty() ? "<#FF3B47>ไม่มี" : "<#6EEB83>" + genericLocs.size() + " จุด"));
          msg(player, "<#FFB703>จำนวนภารกิจต่อผู้เล่น (Tasks Per Player): <#6EEB83>"
            + arena.getEffectiveTasksPerPlayer() + " ภารกิจ <#B8B0C8>(ตั้งค่าด้วย /mma settaskamount " + arenaId + " <จำนวน>)");
          msg(player, "<#72EFDD>เอฟเฟกต์ยืนยันตัวตน (Visual Tasks): "
            + (arena.isVisualTasksEnabled() ? "<#6EEB83>เปิดใช้งาน (ON)" : "<#FF3B47>ปิดใช้งาน (OFF)"));
          Location cf = arena.getCampfireLocation();
          msg(player, "<#FF7518>จุดกองไฟประชุมฉุกเฉิน (Campfire): "
            + (cf == null ? "<#FF3B47>ยังไม่ได้ตั้งค่า (/mma setcampfire " + arenaId + ")" : "<#6EEB83>" + cf.getBlockX() + ", " + cf.getBlockY() + ", " + cf.getBlockZ()));
          msg(player, "<#C77DFF>ฉากเปิดตัวก่อนเริ่มเกม (Cutscene): "
            + (arena.getCutsceneManager().hasCutscene()
              ? "<#6EEB83>ตั้งค่าแล้ว (" + String.format(Locale.US, "%.1fs", arena.getCutsceneManager().getDurationSeconds()) + " / " + arena.getCutsceneManager().getFrames().size() + " เฟรม)"
              : "<#FF3B47>ยังไม่ได้บันทึก (/mma recordcutscene " + arenaId + ")"));
          msg(player, "<#5A4E7C>=============================================================");
        }
      }
    );

    registry.mapArgument(
      "murdermysteryadmin",
      new LabeledCommandArgument(
        "cleartasks",
        "murdermystery.admin.cleartasks",
        CommandArgument.ExecutorType.PLAYER,
        new LabelData(
          "/mma cleartasks <arena> [taskType]",
          "/mma cleartasks <arena> [taskType]",
          "&7ล้างจุดภารกิจของห้อง\n&6Permission: &7murdermystery.admin.cleartasks")) {
        @Override
        public void execute(CommandSender sender, String[] args) {
          Player player = (Player) sender;
          if(args.length < 2) {
            msg(player, "<#FF3B47>การใช้งาน: /mma cleartasks <ชื่อห้อง/arena> [ประเภทภารกิจ/taskType]");
            return;
          }
          String arenaId = args[1];
          Arena arena = (Arena) registry.getPlugin().getArenaRegistry().getArena(arenaId);
          if(arena == null) {
            msg(player, "<#FF3B47>ไม่พบห้องชื่อ " + arenaId);
            return;
          }

          FileConfiguration config = ConfigUtils.getConfig(registry.getPlugin(), "arenas");

          if(args.length >= 3) {
            TaskType type = TaskType.fromInput(args[2]);
            if(type == null) {
              msg(player, "<#FF3B47>ไม่พบประเภทภารกิจ: <#FFB703>" + args[2]);
              sendTaskTypesHelp(player);
              return;
            }
            config.set("instances." + arenaId + ".tasks." + type.name(), null);
            ConfigUtils.saveConfig(registry.getPlugin(), config, "arenas");
            arena.clearSpecificTaskLocations(type);
            msg(player, "<#FF3B47>[!] ล้างจุดภารกิจ <#FF7518>" + type.getIcon() + " " + type.getDisplayName() + " <#FF3B47>ของห้อง <#FFB703>" + arenaId + " <#FF3B47>เรียบร้อยแล้ว");
          } else {
            config.set("instances." + arenaId + ".tasklocations", new ArrayList<String>());
            config.set("instances." + arenaId + ".tasks", null);
            ConfigUtils.saveConfig(registry.getPlugin(), config, "arenas");
            arena.setTaskLocations(new ArrayList<>());
            arena.clearSpecificTaskLocations(null);
            msg(player, "<#FF3B47>[!] ล้างจุดภารกิจทั้งหมด (ทุกประเภท) ของห้อง <#FFB703>" + arenaId + " <#FF3B47>เรียบร้อยแล้ว");
          }
        }
      }
    );

    registry.mapArgument(
      "murdermysteryadmin",
      new LabeledCommandArgument(
        "setsabotage",
        "murdermystery.admin.addtask",
        CommandArgument.ExecutorType.PLAYER,
        new LabelData(
          "/mma setsabotage <arena> <sabotageType>",
          "/mma setsabotage <arena> <sabotageType>",
          "&7ตั้งจุดแก้ไขก่อวินาศกรรม (Sabotage Point) แยกจากภารกิจทั่วไป (lights, engine, comms)\n&6Permission: &7murdermystery.admin.addtask")) {
        @Override
        public void execute(CommandSender sender, String[] args) {
          handleSetSabotage(registry, (Player) sender, args);
        }
      }
    );

    registry.mapArgument(
      "murdermysteryadmin",
      new LabeledCommandArgument(
        "addsabotage",
        "murdermystery.admin.addtask",
        CommandArgument.ExecutorType.PLAYER,
        new LabelData(
          "/mma addsabotage <arena> <sabotageType>",
          "/mma addsabotage <arena> <sabotageType>",
          "&7ตั้งจุดแก้ไขก่อวินาศกรรม (Sabotage Point) แยกจากภารกิจทั่วไป (lights, engine, comms)\n&6Permission: &7murdermystery.admin.addtask")) {
        @Override
        public void execute(CommandSender sender, String[] args) {
          handleSetSabotage(registry, (Player) sender, args);
        }
      }
    );

    registry.mapArgument(
      "murdermysteryadmin",
      new LabeledCommandArgument(
        "listsabotages",
        "murdermystery.admin.addtask",
        CommandArgument.ExecutorType.PLAYER,
        new LabelData(
          "/mma listsabotages <arena>",
          "/mma listsabotages <arena>",
          "&7ดูรายการจุดแก้ไขก่อวินาศกรรม (Sabotage) ทั้งหมดของห้อง\n&6Permission: &7murdermystery.admin.addtask")) {
        @Override
        public void execute(CommandSender sender, String[] args) {
          Player player = (Player) sender;
          if(args.length < 2) {
            msg(player, "<#FF3B47>การใช้งาน: /mma listsabotages <ชื่อห้อง/arena>");
            return;
          }
          String arenaId = args[1];
          Arena arena = (Arena) registry.getPlugin().getArenaRegistry().getArena(arenaId);
          if(arena == null) {
            msg(player, "<#FF3B47>ไม่พบห้องชื่อ " + arenaId);
            return;
          }

          msg(player, "<#5A4E7C>================= [ <gradient:#D90429:#FF5400><bold>รายการบล็อกแก้ Sabotage: " + arenaId + "</bold></gradient> <#5A4E7C>] =================");
          int idx = 1;
          for(SabotageType type : SabotageType.values()) {
            Location l = arena.getSabotageLocation(type);
            if(l == null) {
              msg(player, "<#FFB703>" + idx + ". " + type.getIcon() + " " + type.getDisplayName()
                + " <#B8B0C8>(" + type.getShortId() + "): <#FF3B47>ยังไม่ได้ตั้งค่า");
            } else {
              msg(player, "<#FFB703>" + idx + ". " + type.getIcon() + " " + type.getDisplayName()
                + " <#B8B0C8>(" + type.getShortId() + "): <#6EEB83>"
                + l.getBlockX() + ", " + l.getBlockY() + ", " + l.getBlockZ());
            }
            idx++;
          }
          msg(player, "<#5A4E7C>=============================================================");
        }
      }
    );

    registry.mapArgument(
      "murdermysteryadmin",
      new LabeledCommandArgument(
        "clearsabotages",
        "murdermystery.admin.cleartasks",
        CommandArgument.ExecutorType.PLAYER,
        new LabelData(
          "/mma clearsabotages <arena> [sabotageType]",
          "/mma clearsabotages <arena> [sabotageType]",
          "&7ล้างจุดแก้ไขก่อวินาศกรรม (Sabotage) ของห้อง\n&6Permission: &7murdermystery.admin.cleartasks")) {
        @Override
        public void execute(CommandSender sender, String[] args) {
          Player player = (Player) sender;
          if(args.length < 2) {
            msg(player, "<#FF3B47>การใช้งาน: /mma clearsabotages <ชื่อห้อง/arena> [ประเภท/sabotageType]");
            return;
          }
          String arenaId = args[1];
          Arena arena = (Arena) registry.getPlugin().getArenaRegistry().getArena(arenaId);
          if(arena == null) {
            msg(player, "<#FF3B47>ไม่พบห้องชื่อ " + arenaId);
            return;
          }

          FileConfiguration config = ConfigUtils.getConfig(registry.getPlugin(), "arenas");

          if(args.length >= 3) {
            SabotageType type = SabotageType.fromInput(args[2]);
            if(type == null) {
              msg(player, "<#FF3B47>ไม่พบประเภท Sabotage: <#FFB703>" + args[2]);
              sendSabotageTypesHelp(player);
              return;
            }
            config.set("instances." + arenaId + ".sabotages." + type.name(), null);
            ConfigUtils.saveConfig(registry.getPlugin(), config, "arenas");
            arena.clearSabotageLocations(type);
            msg(player, "<#FF3B47>[!] ล้างจุด Sabotage <#FF7518>" + type.getIcon() + " " + type.getDisplayName() + " <#FF3B47>ของห้อง <#FFB703>" + arenaId + " <#FF3B47>เรียบร้อยแล้ว");
          } else {
            config.set("instances." + arenaId + ".sabotages", null);
            ConfigUtils.saveConfig(registry.getPlugin(), config, "arenas");
            arena.clearSabotageLocations(null);
            msg(player, "<#FF3B47>[!] ล้างจุด Sabotage ทั้งหมดของห้อง <#FFB703>" + arenaId + " <#FF3B47>เรียบร้อยแล้ว");
          }
        }
      }
    );

    registry.mapArgument(
      "murdermysteryadmin",
      new LabeledCommandArgument(
        "setcampfire",
        "murdermystery.admin.addtask",
        CommandArgument.ExecutorType.PLAYER,
        new LabelData(
          "/mma setcampfire <arena>",
          "/mma setcampfire <arena>",
          "&7ตั้งจุดกองไฟกลางแมพสำหรับรวมตัวประชุมและโหวตหาคนร้าย\n&6Permission: &7murdermystery.admin.addtask")) {
        @Override
        public void execute(CommandSender sender, String[] args) {
          Player player = (Player) sender;
          if(args.length < 2) {
            msg(player, "<#FF3B47>การใช้งาน: /mma setcampfire <ชื่อห้อง/arena>");
            return;
          }
          String arenaId = args[1];
          Arena arena = (Arena) registry.getPlugin().getArenaRegistry().getArena(arenaId);
          if(arena == null) {
            msg(player, "<#FF3B47>ไม่พบห้องชื่อ " + arenaId);
            return;
          }

          Block targetBlock = player.getTargetBlock(null, 5);
          Location loc = (targetBlock != null && targetBlock.getType() != Material.AIR)
            ? targetBlock.getLocation()
            : player.getLocation().getBlock().getLocation();

          FileConfiguration config = ConfigUtils.getConfig(registry.getPlugin(), "arenas");
          String serialized = LocationSerializer.locationToString(loc);
          config.set("instances." + arenaId + ".campfire", serialized);
          ConfigUtils.saveConfig(registry.getPlugin(), config, "arenas");

          arena.setCampfireLocation(loc);

          msg(player, "<#6EEB83>[+] ตั้งจุดกองไฟประชุมฉุกเฉิน (Campfire) ให้ห้อง <#FFB703>" + arenaId + " <#6EEB83>เรียบร้อยแล้วที่ <#F4F1DE>"
            + loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ());
        }
      }
    );

    registry.mapArgument(
      "murdermysteryadmin",
      new LabeledCommandArgument(
        "clearcampfire",
        "murdermystery.admin.cleartasks",
        CommandArgument.ExecutorType.PLAYER,
        new LabelData(
          "/mma clearcampfire <arena>",
          "/mma clearcampfire <arena>",
          "&7ล้างจุดกองไฟประชุมฉุกเฉินของห้อง\n&6Permission: &7murdermystery.admin.cleartasks")) {
        @Override
        public void execute(CommandSender sender, String[] args) {
          Player player = (Player) sender;
          if(args.length < 2) {
            msg(player, "<#FF3B47>การใช้งาน: /mma clearcampfire <ชื่อห้อง/arena>");
            return;
          }
          String arenaId = args[1];
          Arena arena = (Arena) registry.getPlugin().getArenaRegistry().getArena(arenaId);
          if(arena == null) {
            msg(player, "<#FF3B47>ไม่พบห้องชื่อ " + arenaId);
            return;
          }

          FileConfiguration config = ConfigUtils.getConfig(registry.getPlugin(), "arenas");
          config.set("instances." + arenaId + ".campfire", null);
          ConfigUtils.saveConfig(registry.getPlugin(), config, "arenas");

          arena.setCampfireLocation(null);

          msg(player, "<#FF3B47>[!] ล้างจุดกองไฟประชุมฉุกเฉินของห้อง <#FFB703>" + arenaId + " <#FF3B47>เรียบร้อยแล้ว");
        }
      }
    );

    registry.mapArgument(
      "murdermysteryadmin",
      new LabeledCommandArgument(
        "addvent",
        "murdermystery.admin.addvent",
        CommandArgument.ExecutorType.PLAYER,
        new LabelData(
          "/mma addvent <arena>",
          "/mma addvent <arena>",
          "&7เพิ่มท่อระบายน้ำ (Sewer Vent) ที่บล็อกที่คุณมองอยู่หรือจุดที่คุณยืน\n&6Permission: &7murdermystery.admin.addvent")) {
        @Override
        public void execute(CommandSender sender, String[] args) {
          Player player = (Player) sender;
          if(args.length < 2) {
            msg(player, "<#FF3B47>การใช้งาน: /mma addvent <ชื่อห้อง/arena>");
            return;
          }
          String arenaId = args[1];
          Arena arena = (Arena) registry.getPlugin().getArenaRegistry().getArena(arenaId);
          if(arena == null) {
            msg(player, "<#FF3B47>ไม่พบห้องชื่อ " + arenaId);
            return;
          }

          Block targetBlock = player.getTargetBlock(null, 5);
          Location loc = (targetBlock != null && targetBlock.getType() != Material.AIR)
            ? targetBlock.getLocation()
            : player.getLocation().getBlock().getLocation();

          FileConfiguration config = ConfigUtils.getConfig(registry.getPlugin(), "arenas");
          List<String> list = config.getStringList("instances." + arenaId + ".sewers");
          String serialized = LocationSerializer.locationToString(loc);
          if(!list.contains(serialized)) {
            list.add(serialized);
          }
          config.set("instances." + arenaId + ".sewers", list);
          ConfigUtils.saveConfig(registry.getPlugin(), config, "arenas");

          SpecialBlock sb = new SpecialBlock(loc, SpecialBlock.SpecialBlockType.RAPID_TELEPORTATION);
          arena.loadSpecialBlock(sb);

          msg(player, "<#6EEB83>[+] เพิ่มท่อระบายน้ำ (Sewer Vent) ให้ห้อง <#FFB703>" + arenaId + " <#6EEB83>เรียบร้อยแล้วที่ <#F4F1DE>"
            + loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ()
            + " <#6EEB83>(รวมทั้งหมด <#FFB703>" + list.size() + " <#6EEB83>จุด)");
        }
      }
    );

    registry.mapArgument(
      "murdermysteryadmin",
      new LabeledCommandArgument(
        "clearvents",
        "murdermystery.admin.clearvents",
        CommandArgument.ExecutorType.PLAYER,
        new LabelData(
          "/mma clearvents <arena>",
          "/mma clearvents <arena>",
          "&7ล้างท่อระบายน้ำทั้งหมดของห้อง\n&6Permission: &7murdermystery.admin.clearvents")) {
        @Override
        public void execute(CommandSender sender, String[] args) {
          Player player = (Player) sender;
          if(args.length < 2) {
            msg(player, "<#FF3B47>การใช้งาน: /mma clearvents <ชื่อห้อง/arena>");
            return;
          }
          String arenaId = args[1];
          Arena arena = (Arena) registry.getPlugin().getArenaRegistry().getArena(arenaId);
          if(arena == null) {
            msg(player, "<#FF3B47>ไม่พบห้องชื่อ " + arenaId);
            return;
          }

          FileConfiguration config = ConfigUtils.getConfig(registry.getPlugin(), "arenas");
          config.set("instances." + arenaId + ".sewers", new ArrayList<String>());
          config.set("instances." + arenaId + ".rapid-teleportation", new ArrayList<String>());
          ConfigUtils.saveConfig(registry.getPlugin(), config, "arenas");

          arena.getSpecialBlocks().removeIf(b -> b.getSpecialBlockType() == SpecialBlock.SpecialBlockType.RAPID_TELEPORTATION);

          msg(player, "<#FF3B47>[!] ล้างท่อระบายน้ำทั้งหมดของห้อง <#FFB703>" + arenaId + " <#FF3B47>เรียบร้อยแล้ว");
        }
      }
    );

    String[] taskCountAliases = {"settaskamount", "settaskcount", "tasksperplayer", "setmaxtasks"};
    for(String alias : taskCountAliases) {
      registry.mapArgument(
        "murdermysteryadmin",
        new LabeledCommandArgument(
          alias,
          "murdermystery.admin.addtask",
          CommandArgument.ExecutorType.BOTH,
          new LabelData(
            "/mma " + alias + " [arena] <amount>",
            "/mma " + alias + " [arena] <amount>",
            "&7ตั้งจำนวนภารกิจที่ผู้เล่นแต่ละคนจะได้รับ (1-" + TaskType.values().length + ")\n&6Permission: &7murdermystery.admin.addtask")) {
          @Override
          public void execute(CommandSender sender, String[] args) {
            handleSetTaskAmount(registry, sender, args, alias);
          }
        }
      );
    }

    registry.mapArgument(
      "murdermysteryadmin",
      new LabeledCommandArgument(
        "visualtasks",
        "murdermystery.admin.addtask",
        CommandArgument.ExecutorType.BOTH,
        new LabelData(
          "/mma visualtasks <arena> <on|off>",
          "/mma visualtasks <arena> <on|off>",
          "&7เปิดหรือปิดเอฟเฟกต์ยืนยันตัวตน (Visual Tasks) ของห้อง\n&6Permission: &7murdermystery.admin.addtask")) {
        @Override
        public void execute(CommandSender sender, String[] args) {
          handleVisualTasksToggle(registry, sender, args);
        }
      }
    );

    String[] recordAliases = {"recordcutscene", "startcutscene"};
    for(String alias : recordAliases) {
      registry.mapArgument(
        "murdermysteryadmin",
        new LabeledCommandArgument(
          alias,
          "murdermystery.admin.addtask",
          CommandArgument.ExecutorType.PLAYER,
          new LabelData(
            "/mma " + alias + " <arena>",
            "/mma " + alias + " <arena>",
            "&7เริ่มบันทึกมุมกล้องฉากเปิดตัว (Cutscene) ก่อนเริ่มเกมของห้อง\n&6Permission: &7murdermystery.admin.addtask")) {
          @Override
          public void execute(CommandSender sender, String[] args) {
            handleRecordCutscene(registry, (Player) sender, args, alias);
          }
        }
      );
    }

    registry.mapArgument(
      "murdermysteryadmin",
      new LabeledCommandArgument(
        "stopcutscene",
        "murdermystery.admin.addtask",
        CommandArgument.ExecutorType.PLAYER,
        new LabelData(
          "/mma stopcutscene",
          "/mma stopcutscene",
          "&7หยุดบันทึก Cutscene และบันทึกข้อมูลลงห้อง\n&6Permission: &7murdermystery.admin.addtask")) {
        @Override
        public void execute(CommandSender sender, String[] args) {
          handleStopCutscene(registry, (Player) sender);
        }
      }
    );

    registry.mapArgument(
      "murdermysteryadmin",
      new LabeledCommandArgument(
        "previewcutscene",
        "murdermystery.admin.addtask",
        CommandArgument.ExecutorType.PLAYER,
        new LabelData(
          "/mma previewcutscene <arena>",
          "/mma previewcutscene <arena>",
          "&7ดูตัวอย่างฉากเปิดตัว (Cutscene) ที่บันทึกไว้ของห้อง\n&6Permission: &7murdermystery.admin.addtask")) {
        @Override
        public void execute(CommandSender sender, String[] args) {
          handlePreviewCutscene(registry, (Player) sender, args);
        }
      }
    );

    registry.mapArgument(
      "murdermysteryadmin",
      new LabeledCommandArgument(
        "clearcutscene",
        "murdermystery.admin.cleartasks",
        CommandArgument.ExecutorType.PLAYER,
        new LabelData(
          "/mma clearcutscene <arena>",
          "/mma clearcutscene <arena>",
          "&7ลบฉากเปิดตัว (Cutscene) ของห้อง\n&6Permission: &7murdermystery.admin.cleartasks")) {
        @Override
        public void execute(CommandSender sender, String[] args) {
          handleClearCutscene(registry, (Player) sender, args);
        }
      }
    );

    registry.mapArgument(
      "murdermysteryadmin",
      new LabeledCommandArgument(
        "cutscene",
        "murdermystery.admin.addtask",
        CommandArgument.ExecutorType.PLAYER,
        new LabelData(
          "/mma cutscene <arena|stop> [record|stop|preview|clear]",
          "/mma cutscene <arena|stop> [record|stop|preview|clear]",
          "&7จัดการฉากเปิดตัว (Cutscene) ก่อนเริ่มเกม (record / stop / preview / clear)\n&6Permission: &7murdermystery.admin.addtask")) {
        @Override
        public void execute(CommandSender sender, String[] args) {
          handleCutsceneMasterCommand(registry, (Player) sender, args);
        }
      }
    );
  }

  private static void handleCutsceneMasterCommand(ArgumentsRegistry registry, Player player, String[] args) {
    if(args.length < 2) {
      msg(player, "<#FF3B47>การใช้งานคำสั่ง Cutscene:");
      msg(player, "<#FFB703>/mma recordcutscene <arena> <#B8B0C8>- เริ่มบันทึกมุมกล้อง Cutscene");
      msg(player, "<#FFB703>/mma stopcutscene <#B8B0C8>- หยุดบันทึกและเซฟ Cutscene");
      msg(player, "<#FFB703>/mma previewcutscene <arena> <#B8B0C8>- ทดสอบเล่น Cutscene");
      msg(player, "<#FFB703>/mma clearcutscene <arena> <#B8B0C8>- ลบ Cutscene ของห้อง");
      return;
    }

    String firstArg = args[1].trim();
    if(firstArg.equalsIgnoreCase("stop")) {
      handleStopCutscene(registry, player);
      return;
    }

    if(args.length >= 3) {
      String firstLower = firstArg.toLowerCase(Locale.ROOT);
      String secondArg = args[2].trim();
      String secondLower = secondArg.toLowerCase(Locale.ROOT);

      if(firstLower.equals("record") || firstLower.equals("start")) {
        handleRecordCutscene(registry, player, new String[]{args[0], secondArg}, "recordcutscene");
        return;
      }
      if(firstLower.equals("preview") || firstLower.equals("play") || firstLower.equals("test")) {
        handlePreviewCutscene(registry, player, new String[]{args[0], secondArg});
        return;
      }
      if(firstLower.equals("clear") || firstLower.equals("delete") || firstLower.equals("remove")) {
        handleClearCutscene(registry, player, new String[]{args[0], secondArg});
        return;
      }

      if(secondLower.equals("record") || secondLower.equals("start")) {
        handleRecordCutscene(registry, player, new String[]{args[0], firstArg}, "recordcutscene");
        return;
      }
      if(secondLower.equals("stop")) {
        handleStopCutscene(registry, player);
        return;
      }
      if(secondLower.equals("preview") || secondLower.equals("play") || secondLower.equals("test")) {
        handlePreviewCutscene(registry, player, new String[]{args[0], firstArg});
        return;
      }
      if(secondLower.equals("clear") || secondLower.equals("delete") || secondLower.equals("remove")) {
        handleClearCutscene(registry, player, new String[]{args[0], firstArg});
        return;
      }
    }

    handleRecordCutscene(registry, player, args, "cutscene");
  }

  private static void handleRecordCutscene(ArgumentsRegistry registry, Player player, String[] args, String label) {
    if(args.length < 2) {
      msg(player, "<#FF3B47>การใช้งาน: <#FFB703>/mma " + label + " <ชื่อห้อง/arena>");
      return;
    }
    String arenaId = args[1];
    Arena arena = (Arena) registry.getPlugin().getArenaRegistry().getArena(arenaId);
    if(arena == null) {
      msg(player, "<#FF3B47>ไม่พบห้องชื่อ " + arenaId);
      return;
    }

    CutsceneManager.startRecording(registry.getPlugin(), player, arena);
    msg(player, "<gradient:#D90429:#FF5400><bold>[REC]</bold></gradient> <#6EEB83>เริ่มบันทึก Cutscene สำหรับห้อง <#FFB703>" + arenaId + " <#6EEB83>แล้ว!");
    msg(player, "<#B8B0C8>เดินหรือบินไปตามเส้นทางมุมกล้องที่ต้องการ แล้วพิมพ์ <#FFB703>/mma stopcutscene <#B8B0C8>เพื่อหยุดและบันทึก");
  }

  private static void handleStopCutscene(ArgumentsRegistry registry, Player player) {
    if(CutsceneManager.isPreviewing(player)) {
      CutsceneManager.stopPreview(player);
      msg(player, "<#6EEB83>[+] หยุดการเล่นตัวอย่าง Cutscene แล้ว");
      return;
    }
    if(!CutsceneManager.isRecording(player)) {
      msg(player, "<#FF3B47>คุณยังไม่ได้เริ่มบันทึก Cutscene! ใช้ <#FFB703>/mma recordcutscene <ชื่อห้อง> <#FF3B47>เพื่อเริ่มบันทึก");
      return;
    }
    CutsceneManager.RecordingResult result = CutsceneManager.stopRecording(registry.getPlugin(), player, true);
    if(result == null || result.getFrameCount() <= 0) {
      msg(player, "<#FF3B47>ไม่มีข้อมูลเฟรมที่ถูกบันทึก");
      return;
    }
    msg(
      player,
      "<#6EEB83>[+] บันทึก Cutscene ของห้อง <#FFB703>" + result.getArena().getId()
        + " <#6EEB83>เรียบร้อยแล้ว! <#B8B0C8>(ความยาว <#FFB703>"
        + String.format(Locale.US, "%.1f วินาที", result.getDurationSeconds())
        + " <#B8B0C8>/ <#F4F1DE>" + result.getFrameCount() + " เฟรม<#B8B0C8>)"
    );
    msg(player, "<#B8B0C8>สามารถดูตัวอย่างได้ด้วยคำสั่ง: <#FFB703>/mma previewcutscene " + result.getArena().getId());
  }

  private static void handlePreviewCutscene(ArgumentsRegistry registry, Player player, String[] args) {
    if(args.length < 2) {
      msg(player, "<#FF3B47>การใช้งาน: <#FFB703>/mma previewcutscene <ชื่อห้อง/arena>");
      return;
    }
    String arenaId = args[1];
    Arena arena = (Arena) registry.getPlugin().getArenaRegistry().getArena(arenaId);
    if(arena == null) {
      msg(player, "<#FF3B47>ไม่พบห้องชื่อ " + arenaId);
      return;
    }
    if(!arena.getCutsceneManager().hasCutscene()) {
      msg(player, "<#FF3B47>ห้อง <#FFB703>" + arenaId + " <#FF3B47>ยังไม่มี Cutscene ที่บันทึกไว้! ใช้ <#FFB703>/mma recordcutscene " + arenaId);
      return;
    }
    if(CutsceneManager.startPreview(registry.getPlugin(), player, arena)) {
      msg(
        player,
        "<#6EEB83>[+] กำลังเล่นตัวอย่าง Cutscene ของห้อง <#FFB703>" + arenaId
          + " <#B8B0C8>(" + String.format(Locale.US, "%.1fs", arena.getCutsceneManager().getDurationSeconds())
          + ") - พิมพ์ <#FFB703>/mma stopcutscene <#B8B0C8>หากต้องการหยุดกลางคัน"
      );
    }
  }

  private static void handleClearCutscene(ArgumentsRegistry registry, Player player, String[] args) {
    if(args.length < 2) {
      msg(player, "<#FF3B47>การใช้งาน: <#FFB703>/mma clearcutscene <ชื่อห้อง/arena>");
      return;
    }
    String arenaId = args[1];
    Arena arena = (Arena) registry.getPlugin().getArenaRegistry().getArena(arenaId);
    if(arena == null) {
      msg(player, "<#FF3B47>ไม่พบห้องชื่อ " + arenaId);
      return;
    }
    FileConfiguration config = ConfigUtils.getConfig(registry.getPlugin(), "arenas");
    config.set("instances." + arenaId + ".cutscene", null);
    ConfigUtils.saveConfig(registry.getPlugin(), config, "arenas");
    arena.getCutsceneManager().clearFrames();
    msg(player, "<#FF3B47>[!] ลบ Cutscene ของห้อง <#FFB703>" + arenaId + " <#FF3B47>เรียบร้อยแล้ว");
  }

  private static void handleSetTaskAmount(ArgumentsRegistry registry, CommandSender sender, String[] args, String label) {
    int maxAllowed = TaskType.values().length;
    if(args.length < 2) {
      msg(sender, "<#FF3B47>การใช้งาน: <#FFB703>/mma " + label + " <ชื่อห้อง/arena> <จำนวน 1-" + maxAllowed + ">");
      msg(sender, "<#B8B0C8>หรือตั้งค่าเริ่มต้นทุกห้อง: <#FFB703>/mma " + label + " <จำนวน 1-" + maxAllowed + ">");
      return;
    }

    if(args.length == 2) {
      int amount;
      try {
        amount = Integer.parseInt(args[1]);
      } catch(NumberFormatException ex) {
        Arena arena = (Arena) registry.getPlugin().getArenaRegistry().getArena(args[1]);
        if(arena != null) {
          msg(sender, "<#FFB703>ห้อง <#6EEB83>" + arena.getId() + " <#FFB703>ตั้งจำนวนภารกิจต่อคนไว้ที่: <#6EEB83>"
            + arena.getEffectiveTasksPerPlayer() + " ภารกิจ");
          msg(sender, "<#B8B0C8>หากต้องการเปลี่ยน ใช้: <#FFB703>/mma " + label + " " + arena.getId() + " <จำนวน 1-" + maxAllowed + ">");
        } else {
          msg(sender, "<#FF3B47>การใช้งาน: <#FFB703>/mma " + label + " <ชื่อห้อง/arena> <จำนวน 1-" + maxAllowed + ">");
        }
        return;
      }

      if(amount < 1 || amount > maxAllowed) {
        msg(sender, "<#FF3B47>จำนวนภารกิจต่อคนต้องอยู่ระหว่าง <#FFB703>1 <#FF3B47>ถึง <#FFB703>" + maxAllowed + " <#FF3B47>ภารกิจ!");
        return;
      }

      registry.getPlugin().getConfig().set("Tasks.Per-Player", amount);
      registry.getPlugin().saveConfig();

      for(IPluginArena pluginArena : registry.getPlugin().getArenaRegistry().getArenas()) {
        if(pluginArena instanceof Arena) {
          Arena arena = (Arena) pluginArena;
          if(arena.getTasksPerPlayer() <= 0) {
            arena.setTasksPerPlayer(-1);
          }
        }
      }

      msg(sender, "<#6EEB83>[+] ตั้งค่าจำนวนภารกิจเริ่มต้น (Global Tasks Per Player) เป็น <#FFB703>" + amount + " ภารกิจต่อคน <#6EEB83>เรียบร้อยแล้ว!");
      return;
    }

    String arenaId = args[1];
    Arena arena = (Arena) registry.getPlugin().getArenaRegistry().getArena(arenaId);
    if(arena == null) {
      msg(sender, "<#FF3B47>ไม่พบห้องชื่อ " + arenaId);
      return;
    }

    int amount;
    try {
      amount = Integer.parseInt(args[2]);
    } catch(NumberFormatException ex) {
      msg(sender, "<#FF3B47>กรุณาระบุจำนวนภารกิจเป็นตัวเลข (1-" + maxAllowed + ") เช่น: <#FFB703>/mma " + label + " " + arenaId + " 4");
      return;
    }

    if(amount < 1 || amount > maxAllowed) {
      msg(sender, "<#FF3B47>จำนวนภารกิจต่อคนต้องอยู่ระหว่าง <#FFB703>1 <#FF3B47>ถึง <#FFB703>" + maxAllowed + " <#FF3B47>ภารกิจ!");
      return;
    }

    FileConfiguration config = ConfigUtils.getConfig(registry.getPlugin(), "arenas");
    config.set("instances." + arenaId + ".tasksperplayer", amount);
    ConfigUtils.saveConfig(registry.getPlugin(), config, "arenas");

    arena.setTasksPerPlayer(amount);

    msg(sender, "<#6EEB83>[+] ตั้งจำนวนภารกิจต่อผู้เล่นของห้อง <#FFB703>" + arenaId + " <#6EEB83>เป็น <#FFB703>" + amount + " ภารกิจต่อคน <#6EEB83>เรียบร้อยแล้ว!");
  }

  private static void handleVisualTasksToggle(ArgumentsRegistry registry, CommandSender sender, String[] args) {
    if(args.length < 3) {
      msg(sender, "<#FF3B47>การใช้งาน: <#FFB703>/mma visualtasks <ชื่อห้อง/arena> <on|off>");
      return;
    }
    String arenaId = args[1];
    Arena arena = (Arena) registry.getPlugin().getArenaRegistry().getArena(arenaId);
    if(arena == null) {
      msg(sender, "<#FF3B47>ไม่พบห้องชื่อ " + arenaId);
      return;
    }

    String val = args[2].trim().toLowerCase();
    boolean enabled;
    if(val.equals("on") || val.equals("true") || val.equals("enable") || val.equals("1")) {
      enabled = true;
    } else if(val.equals("off") || val.equals("false") || val.equals("disable") || val.equals("0")) {
      enabled = false;
    } else {
      msg(sender, "<#FF3B47>กรุณาระบุ <#FFB703>on <#FF3B47>หรือ <#FFB703>off <#FF3B47>เช่น: <#FFB703>/mma visualtasks " + arenaId + " on");
      return;
    }

    FileConfiguration config = ConfigUtils.getConfig(registry.getPlugin(), "arenas");
    config.set("instances." + arenaId + ".visualtasks", enabled);
    ConfigUtils.saveConfig(registry.getPlugin(), config, "arenas");

    arena.setVisualTasksEnabled(enabled);

    msg(sender, "<#6EEB83>[+] ตั้งค่า Visual Tasks ของห้อง <#FFB703>" + arenaId + " <#6EEB83>เป็น: "
      + (enabled ? "<#6EEB83>เปิดใช้งาน (ON)" : "<#FF3B47>ปิดใช้งาน (OFF)"));
  }

  private static void handleSetSabotage(ArgumentsRegistry registry, Player player, String[] args) {
    if(args.length < 3) {
      msg(player, "<#FF3B47>การใช้งาน: /mma setsabotage <ชื่อห้อง/arena> <ประเภท/sabotageType>");
      sendSabotageTypesHelp(player);
      return;
    }
    String arenaId = args[1];
    Arena arena = (Arena) registry.getPlugin().getArenaRegistry().getArena(arenaId);
    if(arena == null) {
      msg(player, "<#FF3B47>ไม่พบห้องชื่อ " + arenaId);
      return;
    }

    SabotageType type = SabotageType.fromInput(args[2]);
    if(type == null) {
      msg(player, "<#FF3B47>ไม่พบประเภท Sabotage: <#FFB703>" + args[2]);
      sendSabotageTypesHelp(player);
      return;
    }

    Block targetBlock = player.getTargetBlock(null, 5);
    Location loc = (targetBlock != null && targetBlock.getType() != Material.AIR)
      ? targetBlock.getLocation()
      : player.getLocation().getBlock().getLocation();

    FileConfiguration config = ConfigUtils.getConfig(registry.getPlugin(), "arenas");
    String serialized = LocationSerializer.locationToString(loc);
    config.set("instances." + arenaId + ".sabotages." + type.name(), serialized);
    ConfigUtils.saveConfig(registry.getPlugin(), config, "arenas");

    arena.setSabotageLocation(type, loc);

    msg(player, "<#6EEB83>[+] ตั้งบล็อกแก้ Sabotage <#FF3B47>" + type.getIcon() + " " + type.getDisplayName()
      + " <#6EEB83>ให้ห้อง <#FFB703>" + arenaId + " <#6EEB83>เรียบร้อยแล้วที่ <#F4F1DE>"
      + loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ());
  }

  private static void sendTaskTypesHelp(CommandSender sender) {
    msg(sender, "<gradient:#FF5400:#FFB703><bold>=== ประเภทภารกิจที่ใช้ได้ (Available Tasks) ===</bold></gradient>");
    int i = 1;
    for(TaskType t : TaskType.values()) {
      String visualBadge = t.isVisual() ? " <#72EFDD>[Visual]" : "";
      msg(sender, "<#F4F1DE>" + i + ". <#FFB703>" + t.getShortId() + " <#B8B0C8>- " + t.getIcon() + " " + t.getDisplayName() + visualBadge);
      i++;
    }
    msg(sender, "<#B8B0C8>(สามารถระบุเป็นหมายเลข 1-" + TaskType.values().length + " หรือชื่อ เช่น diesel, wiring, scan, trash, asteroids)");
  }

  private static void sendSabotageTypesHelp(Player player) {
    msg(player, "<gradient:#D90429:#FF5400><bold>=== ประเภท Sabotage ที่ใช้ได้ (Available Sabotages) ===</bold></gradient>");
    int i = 1;
    for(SabotageType s : SabotageType.values()) {
      msg(player, "<#F4F1DE>" + i + ". <#FF3B47>" + s.getShortId() + " <#B8B0C8>- " + s.getIcon() + " " + s.getDisplayName());
      i++;
    }
    msg(player, "<#B8B0C8>(สามารถระบุเป็นหมายเลข 1-3 หรือชื่อ lights, engine, comms)");
  }
}
