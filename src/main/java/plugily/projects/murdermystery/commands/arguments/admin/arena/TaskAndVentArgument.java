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
import plugily.projects.murdermystery.arena.cutscene.BusSchematicLoader;
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
    HalloweenChat.send(sender, text);
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

            removeBlockFromConfigTasks(config, arenaId, loc);
            List<String> list = config.getStringList("instances." + arenaId + ".tasks." + type.name());
            if(!list.contains(serialized)) {
              list.add(serialized);
            }
            config.set("instances." + arenaId + ".tasks." + type.name(), list);
            ConfigUtils.saveConfig(registry.getPlugin(), config, "arenas");

            arena.removeTaskLocationAtBlock(loc);
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
          "&7ตั้งจุดสำหรับภารกิจที่เจาะจง (เช่น diesel, wiring, panel, radio, water)\n&6Permission: &7murdermystery.admin.addtask")) {
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
          removeBlockFromConfigTasks(config, arenaId, loc);
          List<String> list = config.getStringList("instances." + arenaId + ".tasks." + type.name());
          if(!list.contains(serialized)) {
            list.add(serialized);
          }
          config.set("instances." + arenaId + ".tasks." + type.name(), list);
          ConfigUtils.saveConfig(registry.getPlugin(), config, "arenas");

          arena.removeTaskLocationAtBlock(loc);
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

          msg(player, "<shadow:#000000FF><#5A4E7C>[</shadow><#FF7518>รายการจุดภารกิจ: " + arenaId + "<shadow:#000000FF><#5A4E7C>]</shadow>");
          int idx = 1;
          for(TaskType type : TaskType.values()) {
            List<Location> locs = arena.getTaskLocations(type);
            String visualTag = type.isVisual() ? " <#72EFDD>[Visual]" : "";
            if(locs.isEmpty()) {
              String fallbackNote = arena.getTaskLocations().isEmpty()
                ? "ยังไม่ได้ตั้งค่า (จะไม่สุ่มภารกิจนี้)"
                : "ยังไม่ได้ตั้งค่า (จะสุ่มจุดทั่วไป)";
              msg(player, "<#FFB703>" + idx + ". " + type.getIcon() + " " + type.getDisplayName() + visualTag
                + " <#B8B0C8>(" + type.getShortId() + "): <#FF3B47>" + fallbackNote);
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
          msg(player, "<#FF7518>จุดแตรรถบัสฉุกเฉิน (Bus Horn): "
            + (cf == null ? "<#FF3B47>ยังไม่ได้ตั้งค่า (/mma setbushorn " + arenaId + ")" : "<#6EEB83>" + cf.getBlockX() + ", " + cf.getBlockY() + ", " + cf.getBlockZ()));
          Location mp = arena.getMeetingLocation();
          msg(player, "<#FFB703>จุดรวมตัวประชุมโหวต (Meeting Point): "
            + (mp == null
              ? "<#B8B0C8>ใช้จุดเดียวกับ Bus Horn (แยกจุดนอกรถได้ด้วย /mma setmeetingpoint " + arenaId + ")"
              : "<#6EEB83>" + mp.getBlockX() + ", " + mp.getBlockY() + ", " + mp.getBlockZ()));
          CutsceneManager cm = arena.getCutsceneManager();
          String hideBoxNote = (cm.getBusHidePos1() != null && cm.getBusHidePos2() != null)
            ? " / ซ่อนรถจริงชั่วคราว: เปิด"
            : "";
          msg(player, "<#FF7518>ฉากรถบัสวิ่งและเสีย (Moving Bus): "
            + (cm.hasBusCutscene()
              ? "<#6EEB83>ตั้งค่าแล้ว (วิ่ง " + String.format(Locale.US, "%.1fs", cm.getBusDriveDurationSeconds()) + " / ไฟล์ " + cm.getBusSchematicName() + hideBoxNote + ")"
              : "<#FF3B47>ยังไม่ได้ตั้งค่า (/mma setbusstart " + arenaId + " และ /mma setbusend " + arenaId + ")"));
          String cutsceneStatus;
          if(!cm.getFrames().isEmpty()) {
            cutsceneStatus = "<#6EEB83>มุมกล้องบันทึกเอง (" + String.format(Locale.US, "%.1fs", cm.getDurationSeconds()) + " / " + cm.getFrames().size() + " เฟรม @ 60FPS)"
              + (cm.hasBusCutscene() ? " <#B8B0C8>(สลับเป็นกล้องอัตโนมัติ: /mma autocutscene " + arenaId + ")" : "");
          } else if(cm.hasBusCutscene()) {
            cutsceneStatus = "<#6EEB83>มุมกล้องอัตโนมัติ 2 ช็อต: ด้านข้าง -> ตัดไปด้านหน้าตอนรถเสีย (" + String.format(Locale.US, "%.1fs", cm.getDurationSeconds()) + " @ 60FPS)";
          } else {
            cutsceneStatus = "<#FF3B47>ยังไม่ได้ตั้งค่า (/mma setbusstart " + arenaId + " & /mma setbusend " + arenaId + ")";
          }
          msg(player, "<#C77DFF>ฉากเปิดตัวก่อนเริ่มเกม (Cutscene 60FPS): " + cutsceneStatus);
          msg(player, "<shadow:#000000FF><#5A4E7C>=============================================================</shadow>");
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

          msg(player, "<shadow:#000000FF><#5A4E7C>[</shadow><#FF3B47>รายการบล็อกแก้ Sabotage: " + arenaId + "<shadow:#000000FF><#5A4E7C>]</shadow>");
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
          msg(player, "<shadow:#000000FF><#5A4E7C>=============================================================</shadow>");
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
        "setbushorn",
        "murdermystery.admin.addtask",
        CommandArgument.ExecutorType.PLAYER,
        new LabelData(
          "/mma setbushorn <arena>",
          "/mma setbushorn <arena>",
          "&7ตั้งจุดแตรรถบัสสำหรับเรียกประชุมฉุกเฉินและโหวตหาคนร้าย\n&6Permission: &7murdermystery.admin.addtask")) {
        @Override
        public void execute(CommandSender sender, String[] args) {
          handleSetBusHorn(registry, (Player) sender, args, "setbushorn");
        }
      }
    );

    registry.mapArgument(
      "murdermysteryadmin",
      new LabeledCommandArgument(
        "clearbushorn",
        "murdermystery.admin.cleartasks",
        CommandArgument.ExecutorType.PLAYER,
        new LabelData(
          "/mma clearbushorn <arena>",
          "/mma clearbushorn <arena>",
          "&7ล้างจุดแตรรถบัสประชุมฉุกเฉินของห้อง\n&6Permission: &7murdermystery.admin.cleartasks")) {
        @Override
        public void execute(CommandSender sender, String[] args) {
          handleClearBusHorn(registry, (Player) sender, args, "clearbushorn");
        }
      }
    );

    registry.mapArgument(
      "murdermysteryadmin",
      new CommandArgument(
        "setcampfire",
        "murdermystery.admin.addtask",
        CommandArgument.ExecutorType.PLAYER) {
        @Override
        public void execute(CommandSender sender, String[] args) {
          handleSetBusHorn(registry, (Player) sender, args, "setbushorn");
        }
      }
    );

    registry.mapArgument(
      "murdermysteryadmin",
      new CommandArgument(
        "clearcampfire",
        "murdermystery.admin.cleartasks",
        CommandArgument.ExecutorType.PLAYER) {
        @Override
        public void execute(CommandSender sender, String[] args) {
          handleClearBusHorn(registry, (Player) sender, args, "clearbushorn");
        }
      }
    );

    registry.mapArgument(
      "murdermysteryadmin",
      new LabeledCommandArgument(
        "setmeetingpoint",
        "murdermystery.admin.addtask",
        CommandArgument.ExecutorType.PLAYER,
        new LabelData(
          "/mma setmeetingpoint <arena>",
          "/mma setmeetingpoint <arena>",
          "&7ตั้งจุดยืนล้อมวงประชุมโหวตแยกจากแตรรถบัส (เช่น ลานกว้างข้างรถบัส)\n&6Permission: &7murdermystery.admin.addtask")) {
        @Override
        public void execute(CommandSender sender, String[] args) {
          handleSetMeetingPoint(registry, (Player) sender, args, "setmeetingpoint");
        }
      }
    );

    registry.mapArgument(
      "murdermysteryadmin",
      new CommandArgument(
        "setmeeting",
        "murdermystery.admin.addtask",
        CommandArgument.ExecutorType.PLAYER) {
        @Override
        public void execute(CommandSender sender, String[] args) {
          handleSetMeetingPoint(registry, (Player) sender, args, "setmeeting");
        }
      }
    );

    registry.mapArgument(
      "murdermysteryadmin",
      new LabeledCommandArgument(
        "clearmeetingpoint",
        "murdermystery.admin.cleartasks",
        CommandArgument.ExecutorType.PLAYER,
        new LabelData(
          "/mma clearmeetingpoint <arena>",
          "/mma clearmeetingpoint <arena>",
          "&7ล้างจุดยืนล้อมวงประชุมโหวตที่แยกไว้ (กลับไปใช้จุดเดียวกับแตรรถบัส)\n&6Permission: &7murdermystery.admin.cleartasks")) {
        @Override
        public void execute(CommandSender sender, String[] args) {
          handleClearMeetingPoint(registry, (Player) sender, args, "clearmeetingpoint");
        }
      }
    );

    registry.mapArgument(
      "murdermysteryadmin",
      new CommandArgument(
        "clearmeeting",
        "murdermystery.admin.cleartasks",
        CommandArgument.ExecutorType.PLAYER) {
        @Override
        public void execute(CommandSender sender, String[] args) {
          handleClearMeetingPoint(registry, (Player) sender, args, "clearmeeting");
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

          arena.clearVentDisplays();
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

    registry.mapArgument(
      "murdermysteryadmin",
      new LabeledCommandArgument(
        "setbusstart",
        "murdermystery.admin.addtask",
        CommandArgument.ExecutorType.PLAYER,
        new LabelData(
          "/mma setbusstart <arena> [snap]",
          "/mma setbusstart <arena> [snap]",
          "&7ตั้งจุดเริ่มต้นที่รถบัส (bus.schem) จะเริ่มวิ่งในฉากเปิดตัว\n&6Permission: &7murdermystery.admin.addtask")) {
        @Override
        public void execute(CommandSender sender, String[] args) {
          handleSetBusEndpoint(registry, (Player) sender, args, true);
        }
      }
    );

    registry.mapArgument(
      "murdermysteryadmin",
      new LabeledCommandArgument(
        "setbusend",
        "murdermystery.admin.addtask",
        CommandArgument.ExecutorType.PLAYER,
        new LabelData(
          "/mma setbusend <arena> [snap]",
          "/mma setbusend <arena> [snap]",
          "&7ตั้งจุดปลายทางที่รถบัส (bus.schem) วิ่งมาจอดเสียและหม้อน้ำระเบิด\n&6Permission: &7murdermystery.admin.addtask")) {
        @Override
        public void execute(CommandSender sender, String[] args) {
          handleSetBusEndpoint(registry, (Player) sender, args, false);
        }
      }
    );

    registry.mapArgument(
      "murdermysteryadmin",
      new LabeledCommandArgument(
        "setbusduration",
        "murdermystery.admin.addtask",
        CommandArgument.ExecutorType.BOTH,
        new LabelData(
          "/mma setbusduration <arena> <seconds>",
          "/mma setbusduration <arena> <seconds>",
          "&7ตั้งเวลาที่รถบัสใช้ขับจากจุดเริ่มถึงจุดรถเสีย (วินาที, ค่าเริ่มต้น 8.0)\n&6Permission: &7murdermystery.admin.addtask")) {
        @Override
        public void execute(CommandSender sender, String[] args) {
          handleSetBusDuration(registry, sender, args);
        }
      }
    );

    registry.mapArgument(
      "murdermysteryadmin",
      new LabeledCommandArgument(
        "setbusschem",
        "murdermystery.admin.addtask",
        CommandArgument.ExecutorType.BOTH,
        new LabelData(
          "/mma setbusschem <arena> <filename.schem>",
          "/mma setbusschem <arena> <filename.schem>",
          "&7เลือกไฟล์ Schematic รถบัสสำหรับฉากเปิดตัว (ค่าเริ่มต้น bus.schem)\n&6Permission: &7murdermystery.admin.addtask")) {
        @Override
        public void execute(CommandSender sender, String[] args) {
          handleSetBusSchematic(registry, sender, args);
        }
      }
    );

    registry.mapArgument(
      "murdermysteryadmin",
      new LabeledCommandArgument(
        "hidebusarea",
        "murdermystery.admin.addtask",
        CommandArgument.ExecutorType.PLAYER,
        new LabelData(
          "/mma hidebusarea <arena> <pos1|pos2|clear>",
          "/mma hidebusarea <arena> <pos1|pos2|clear>",
          "&7ซ่อนบล็อกรถบัสจริงในแมพชั่วคราวระหว่างเล่น Cutscene เพื่อไม่ให้ซ้อนทับรถบัสที่กำลังวิ่งเข้ามา\n&6Permission: &7murdermystery.admin.addtask")) {
        @Override
        public void execute(CommandSender sender, String[] args) {
          handleHideBusArea(registry, (Player) sender, args, "hidebusarea");
        }
      }
    );

    registry.mapArgument(
      "murdermysteryadmin",
      new CommandArgument(
        "setbusbox",
        "murdermystery.admin.addtask",
        CommandArgument.ExecutorType.PLAYER) {
        @Override
        public void execute(CommandSender sender, String[] args) {
          handleHideBusArea(registry, (Player) sender, args, "setbusbox");
        }
      }
    );

    registry.mapArgument(
      "murdermysteryadmin",
      new LabeledCommandArgument(
        "autocutscene",
        "murdermystery.admin.addtask",
        CommandArgument.ExecutorType.PLAYER,
        new LabelData(
          "/mma autocutscene <arena>",
          "/mma autocutscene <arena>",
          "&7เปิดใช้มุมกล้องอัตโนมัติ (ถ่ายขนาบด้านข้างรถบัส -> ตัดไปด้านหน้าตอนรถเสีย) โดยไม่ต้องบินถ่ายเอง\n&6Permission: &7murdermystery.admin.addtask")) {
        @Override
        public void execute(CommandSender sender, String[] args) {
          handleAutoCutscene(registry, (Player) sender, args);
        }
      }
    );

    registry.mapArgument(
      "murdermysteryadmin",
      new LabeledCommandArgument(
        "clearbuscutscene",
        "murdermystery.admin.cleartasks",
        CommandArgument.ExecutorType.PLAYER,
        new LabelData(
          "/mma clearbuscutscene <arena>",
          "/mma clearbuscutscene <arena>",
          "&7ล้างการตั้งค่ารถบัสเคลื่อนที่ (Moving Bus Cutscene) ของห้อง\n&6Permission: &7murdermystery.admin.cleartasks")) {
        @Override
        public void execute(CommandSender sender, String[] args) {
          handleClearBusCutscene(registry, (Player) sender, args);
        }
      }
    );
  }

  private static void handleCutsceneMasterCommand(ArgumentsRegistry registry, Player player, String[] args) {
    if(args.length < 2) {
      msg(player, "<#FF3B47>การใช้งานคำสั่ง Cutscene:");
      msg(player, "<#FFB703>/mma setbusstart <arena> [snap] <#B8B0C8>- ตั้งจุดเริ่มวิ่งของรถบัส (bus.schem)");
      msg(player, "<#FFB703>/mma setbusend <arena> [snap] <#B8B0C8>- ตั้งจุดปลายทางที่รถบัสเสียและระเบิด");
      msg(player, "<#FFB703>/mma autocutscene <arena> <#B8B0C8>- ใช้มุมกล้องอัตโนมัติ (ด้านข้าง -> ตัดไปด้านหน้าตอนรถเสีย)");
      msg(player, "<#FFB703>/mma setbusduration <arena> <วินาที> <#B8B0C8>- ตั้งเวลารถบัสวิ่ง (ค่าเริ่มต้น 8.0s)");
      msg(player, "<#FFB703>/mma hidebusarea <arena> <pos1|pos2|clear> <#B8B0C8>- ซ่อนรถบัสจริงในแมพชั่วคราวตอนเล่น Cutscene");
      msg(player, "<#FFB703>/mma previewcutscene <arena> <#B8B0C8>- ทดสอบเล่น Cutscene");
      msg(player, "<#FFB703>/mma recordcutscene <arena> <#B8B0C8>- (ทางเลือก) บินถ่ายมุมกล้องเอง / <#FFB703>stopcutscene");
      msg(player, "<#FFB703>/mma clearcutscene <arena> <#B8B0C8>- ลบมุมกล้องที่บันทึก / <#FFB703>clearbuscutscene <#B8B0C8>- ลบรถบัสวิ่ง");
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

      if(firstLower.equals("auto")) {
        handleAutoCutscene(registry, player, new String[]{args[0], secondArg});
        return;
      }
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

      if(secondLower.equals("auto")) {
        handleAutoCutscene(registry, player, new String[]{args[0], firstArg});
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
    if(registry.getPlugin().getArenaRegistry().isInArena(player)) {
      msg(player, "<shadow:#000000FF><#5A4E7C>[</shadow><#FF3B47>Cutscene<shadow:#000000FF><#5A4E7C>]</shadow> <#FF3B47>กรุณาออกจากห้องเล่นเกมก่อนบันทึก Cutscene");
      return;
    }
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
    msg(player, "<shadow:#000000FF><#5A4E7C>[</shadow><#FF3B47>REC 60FPS<shadow:#000000FF><#5A4E7C>]</shadow> <#6EEB83>เริ่มบันทึก Cutscene สำหรับห้อง <#FFB703>" + arenaId + " <#6EEB83>แล้ว");
    if(arena.getCutsceneManager().hasBusCutscene()) {
      msg(player, "<#72EFDD>[Moving Bus] <#F4F1DE>ปล่อยรถบัสจำลองวิ่งจริง ("
        + String.format(Locale.US, "%.1fs", arena.getCutsceneManager().getBusDriveDurationSeconds())
        + ") ให้คุณบินถ่ายมุมกล้องตามได้เลย!");
    }
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
      "<#6EEB83>[+] บันทึก Cutscene (60 FPS) ของห้อง <#FFB703>" + result.getArena().getId()
        + " <#6EEB83>เรียบร้อยแล้ว! <#B8B0C8>(ความยาว <#FFB703>"
        + String.format(Locale.US, "%.1f วินาที", result.getDurationSeconds())
        + " <#B8B0C8>/ <#F4F1DE>" + result.getFrameCount() + " เฟรม @ 60FPS<#B8B0C8>)"
    );
    msg(player, "<#B8B0C8>สามารถดูตัวอย่างได้ด้วยคำสั่ง: <#FFB703>/mma previewcutscene " + result.getArena().getId());
  }

  private static void handlePreviewCutscene(ArgumentsRegistry registry, Player player, String[] args) {
    if(registry.getPlugin().getArenaRegistry().isInArena(player)) {
      msg(player, "<shadow:#000000FF><#5A4E7C>[</shadow><#FF3B47>Cutscene<shadow:#000000FF><#5A4E7C>]</shadow> <#FF3B47>กรุณาออกจากห้องเล่นเกมก่อนดูตัวอย่าง Cutscene");
      return;
    }
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
      msg(player, "<#FF3B47>ห้อง <#FFB703>" + arenaId + " <#FF3B47>ยังไม่มี Cutscene! ตั้งค่ารถบัสวิ่งด้วย <#FFB703>/mma setbusstart " + arenaId + " <#FF3B47>& <#FFB703>/mma setbusend " + arenaId + " <#FF3B47>หรือบันทึกมุมกล้องด้วย <#FFB703>/mma recordcutscene " + arenaId);
      return;
    }
    if(CutsceneManager.startPreview(registry.getPlugin(), player, arena)) {
      String modeTag = arena.getCutsceneManager().isUsingAutoCamera()
        ? "มุมกล้องอัตโนมัติ: ด้านข้าง -> ตัดไปด้านหน้าตอนรถเสีย, "
        : "มุมกล้องบันทึกเอง, ";
      msg(
        player,
        "<#6EEB83>[+] กำลังเล่นตัวอย่าง Cutscene ของห้อง <#FFB703>" + arenaId
          + " <#B8B0C8>(" + modeTag + String.format(Locale.US, "%.1fs", arena.getCutsceneManager().getDurationSeconds())
          + ") - พิมพ์ <#FFB703>/mma stopcutscene <#B8B0C8>หากต้องการหยุดกลางคัน"
      );
      if(!arena.getCutsceneManager().isUsingAutoCamera() && arena.getCutsceneManager().hasBusCutscene()) {
        msg(player, "<#B8B0C8>Tip: หากต้องการสลับเป็นมุมกล้องอัตโนมัติ (ไม่ต้องบินถ่ายเอง) พิมพ์ <#FFB703>/mma autocutscene " + arenaId);
      }
    }
  }

  private static void handleAutoCutscene(ArgumentsRegistry registry, Player player, String[] args) {
    if(args.length < 2) {
      msg(player, "<#FF3B47>การใช้งาน: <#FFB703>/mma autocutscene <ชื่อห้อง/arena>");
      return;
    }
    String arenaId = args[1];
    Arena arena = (Arena) registry.getPlugin().getArenaRegistry().getArena(arenaId);
    if(arena == null) {
      msg(player, "<#FF3B47>ไม่พบห้องชื่อ " + arenaId);
      return;
    }
    CutsceneManager cm = arena.getCutsceneManager();
    FileConfiguration config = ConfigUtils.getConfig(registry.getPlugin(), "arenas");
    config.set("instances." + arenaId + ".cutscene", null);
    ConfigUtils.saveConfig(registry.getPlugin(), config, "arenas");
    cm.clearFrames();

    if(!cm.hasBusCutscene()) {
      msg(player, "<#FFB703>[Auto Cutscene] <#F4F1DE>ล้างมุมกล้องที่บันทึกเองแล้ว! ตั้งจุดเริ่มและจุดรถเสียเพื่อให้กล้องอัตโนมัติทำงาน:");
      msg(player, "<#B8B0C8>1) <#FFB703>/mma setbusstart " + arenaId + " snap <#B8B0C8>(จุดเริ่มวิ่ง)");
      msg(player, "<#B8B0C8>2) <#FFB703>/mma setbusend " + arenaId + " snap <#B8B0C8>(จุดรถเสีย)");
      return;
    }
    msg(player, "<#6EEB83>[+] เปิดใช้มุมกล้อง Cutscene อัตโนมัติ (ถ่ายขนาบด้านข้างรถ -> ตัดกล้องไปด้านหน้าตอนเครื่องเสีย) สำหรับห้อง <#FFB703>" + arenaId + " <#6EEB83>เรียบร้อยแล้ว!");
    msg(player, "<#B8B0C8>ดูตัวอย่างได้ทันทีด้วยคำสั่ง: <#FFB703>/mma previewcutscene " + arenaId);
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
    if(arena.getCutsceneManager().hasBusCutscene()) {
      msg(player, "<#6EEB83>[+] ลบมุมกล้องที่บันทึกเองของห้อง <#FFB703>" + arenaId + " <#6EEB83>แล้ว (ระบบสลับไปใช้มุมกล้องอัตโนมัติ: ด้านข้าง -> ตัดไปด้านหน้าตอนรถเสีย)");
    } else {
      msg(player, "<#FF3B47>[!] ลบ Cutscene ของห้อง <#FFB703>" + arenaId + " <#FF3B47>เรียบร้อยแล้ว");
    }
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

  private static void handleSetBusHorn(ArgumentsRegistry registry, Player player, String[] args, String subcommand) {
    if(args.length < 2) {
      msg(player, "<#FF3B47>การใช้งาน: /mma " + subcommand + " <ชื่อห้อง/arena>");
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
    config.set("instances." + arenaId + ".bushorn", serialized);
    config.set("instances." + arenaId + ".campfire", null);
    ConfigUtils.saveConfig(registry.getPlugin(), config, "arenas");

    arena.setCampfireLocation(loc);

    msg(player, "<#6EEB83>[+] ตั้งจุดแตรรถบัสฉุกเฉิน (Bus Horn) ให้ห้อง <#FFB703>" + arenaId + " <#6EEB83>เรียบร้อยแล้วที่ <#F4F1DE>"
      + loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ());
    if(arena.getMeetingLocation() == null) {
      msg(player, "<#B8B0C8>Tip: หากตั้งแตรไว้ในห้องคนขับแคบๆ แนะนำให้ตั้งจุดยืนล้อมวงโหวตไว้นอกรถด้วยคำสั่ง <#FFB703>/mma setmeetingpoint " + arenaId);
    }
  }

  private static void handleClearBusHorn(ArgumentsRegistry registry, Player player, String[] args, String subcommand) {
    if(args.length < 2) {
      msg(player, "<#FF3B47>การใช้งาน: /mma " + subcommand + " <ชื่อห้อง/arena>");
      return;
    }
    String arenaId = args[1];
    Arena arena = (Arena) registry.getPlugin().getArenaRegistry().getArena(arenaId);
    if(arena == null) {
      msg(player, "<#FF3B47>ไม่พบห้องชื่อ " + arenaId);
      return;
    }

    FileConfiguration config = ConfigUtils.getConfig(registry.getPlugin(), "arenas");
    config.set("instances." + arenaId + ".bushorn", null);
    config.set("instances." + arenaId + ".campfire", null);
    ConfigUtils.saveConfig(registry.getPlugin(), config, "arenas");

    arena.setCampfireLocation(null);

    msg(player, "<#FF3B47>[!] ล้างจุดแตรรถบัสฉุกเฉินของห้อง <#FFB703>" + arenaId + " <#FF3B47>เรียบร้อยแล้ว");
  }

  private static void handleSetMeetingPoint(ArgumentsRegistry registry, Player player, String[] args, String subcommand) {
    if(args.length < 2) {
      msg(player, "<#FF3B47>การใช้งาน: /mma " + subcommand + " <ชื่อห้อง/arena>");
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
    config.set("instances." + arenaId + ".meetingpoint", serialized);
    ConfigUtils.saveConfig(registry.getPlugin(), config, "arenas");

    arena.setMeetingLocation(loc);

    msg(player, "<#6EEB83>[+] ตั้งจุดยืนล้อมวงประชุมโหวต (Meeting Point) ให้ห้อง <#FFB703>" + arenaId + " <#6EEB83>เรียบร้อยแล้วที่ <#F4F1DE>"
      + loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ());
    msg(player, "<#B8B0C8>ผู้เล่นจะกดแตรที่ [Bus Horn] แต่จะถูกวาร์ปมายืนล้อมวงโหวตรัศมี 2.8 บล็อกรอบจุดนี้");
  }

  private static void handleClearMeetingPoint(ArgumentsRegistry registry, Player player, String[] args, String subcommand) {
    if(args.length < 2) {
      msg(player, "<#FF3B47>การใช้งาน: /mma " + subcommand + " <ชื่อห้อง/arena>");
      return;
    }
    String arenaId = args[1];
    Arena arena = (Arena) registry.getPlugin().getArenaRegistry().getArena(arenaId);
    if(arena == null) {
      msg(player, "<#FF3B47>ไม่พบห้องชื่อ " + arenaId);
      return;
    }

    FileConfiguration config = ConfigUtils.getConfig(registry.getPlugin(), "arenas");
    config.set("instances." + arenaId + ".meetingpoint", null);
    ConfigUtils.saveConfig(registry.getPlugin(), config, "arenas");

    arena.setMeetingLocation(null);

    msg(player, "<#FF3B47>[!] ล้างจุดยืนล้อมวงประชุมโหวตของห้อง <#FFB703>" + arenaId + " <#FF3B47>แล้ว (กลับไปใช้จุดเดียวกับแตรรถบัส)");
  }

  private static void handleSetBusEndpoint(ArgumentsRegistry registry, Player player, String[] args, boolean isStart) {
    String cmd = isStart ? "setbusstart" : "setbusend";
    if(args.length < 2) {
      msg(player, "<#FF3B47>การใช้งาน: <#FFB703>/mma " + cmd + " <ชื่อห้อง/arena> [snap]");
      msg(player, "<#B8B0C8>ยืนกึ่งกลางเลนถนนจุดที่ต้องการให้รถบัสอยู่ (เติม <#FFB703>snap <#B8B0C8>ท้ายคำสั่งหากต้องการล็อกพิกัดลงจำนวนเต็มให้ตรงบล็อกเป๊ะ)");
      return;
    }
    String arenaId = args[1];
    Arena arena = (Arena) registry.getPlugin().getArenaRegistry().getArena(arenaId);
    if(arena == null) {
      msg(player, "<#FF3B47>ไม่พบห้องชื่อ " + arenaId);
      return;
    }

    Location loc = player.getLocation().clone();
    boolean snap = args.length >= 3 && (args[2].equalsIgnoreCase("snap") || args[2].equalsIgnoreCase("grid") || args[2].equalsIgnoreCase("exact"));
    if(snap) {
      loc.setX(Math.round(loc.getX()));
      loc.setY(Math.floor(loc.getY()));
      loc.setZ(Math.round(loc.getZ()));
    } else {
      loc.setY(Math.round(loc.getY() * 16.0) / 16.0);
    }
    loc.setPitch(0.0f);

    CutsceneManager cm = arena.getCutsceneManager();
    FileConfiguration config = ConfigUtils.getConfig(registry.getPlugin(), "arenas");
    String path = "instances." + arenaId + ".buscutscene." + (isStart ? "start" : "end");
    config.set(path, CutsceneManager.serializePreciseLocation(loc));
    ConfigUtils.saveConfig(registry.getPlugin(), config, "arenas");

    if(isStart) {
      cm.setBusStartLocation(loc);
    } else {
      cm.setBusEndLocation(loc);
    }

    BusSchematicLoader.LoadedSchematic loaded = BusSchematicLoader.loadSchematic(registry.getPlugin(), cm.getBusSchematicName());
    String coordText = String.format(Locale.US, "%.2f, %.2f, %.2f", loc.getX(), loc.getY(), loc.getZ());
    if(isStart) {
      msg(player, "<#6EEB83>[+] ตั้งจุดเริ่มต้นรถบัสวิ่ง (Bus Start) ให้ห้อง <#FFB703>" + arenaId + " <#6EEB83>ที่ <#F4F1DE>" + coordText);
    } else {
      msg(player, "<#6EEB83>[+] ตั้งจุดปลายทางรถบัสเสีย (Bus Breakdown End) ให้ห้อง <#FFB703>" + arenaId + " <#6EEB83>ที่ <#F4F1DE>" + coordText);
    }

    if(loaded != null) {
      msg(player, "<#72EFDD>[Schematic] <#B8B0C8>โหลดไฟล์ <#F4F1DE>" + cm.getBusSchematicName() + " <#B8B0C8>พร้อมใช้งาน (<#6EEB83>" + loaded.getParts().size() + " <#B8B0C8>ชิ้นส่วน BlockDisplay)");
    } else {
      msg(player, "<#FF3B47>[!] ไม่พบไฟล์ <#FFB703>plugins/MurderMystery/schematics/" + cm.getBusSchematicName() + " <#FF3B47>กรุณาตรวจสอบไฟล์ Schematic");
    }

    if(cm.hasBusCutscene()) {
      double dist = cm.getBusStartLocation().distance(cm.getBusEndLocation());
      msg(player, "<#6EEB83>[พร้อมใช้งาน] <#F4F1DE>เส้นทางรถบัสตรงยาว <#FFB703>"
        + String.format(Locale.US, "%.1f บล็อก", dist)
        + " <#F4F1DE>ใช้เวลาวิ่ง <#FFB703>"
        + String.format(Locale.US, "%.1f วินาที", cm.getBusDriveDurationSeconds()));
      if(cm.isUsingAutoCamera()) {
        msg(player, "<#72EFDD>[Auto Cutscene] <#F4F1DE>สร้างมุมกล้องอัตโนมัติ (ถ่ายขนาบด้านข้าง -> ตัดกล้องไปด้านหน้าตอนเครื่องเสีย) เรียบร้อยแล้ว! ทดสอบด้วย <#FFB703>/mma previewcutscene " + arenaId);
      } else {
        msg(player, "<#FFB703>[หมายเหตุ] <#B8B0C8>ห้องนี้มีมุมกล้องที่บันทึกเองค้างอยู่ หากต้องการใช้มุมกล้องอัตโนมัติ (ด้านข้าง -> ตัดไปด้านหน้า) ให้พิมพ์ <#FFB703>/mma autocutscene " + arenaId);
      }
    } else if(isStart) {
      msg(player, "<#B8B0C8>ขั้นตอนต่อไป: เดินไปจุดที่รถบัสจอดเสีย แล้วพิมพ์ <#FFB703>/mma setbusend " + arenaId);
    } else {
      msg(player, "<#B8B0C8>ขั้นตอนต่อไป: เดินไปจุดเริ่มปล่อยรถบัส แล้วพิมพ์ <#FFB703>/mma setbusstart " + arenaId);
    }
  }

  private static void handleSetBusDuration(ArgumentsRegistry registry, CommandSender sender, String[] args) {
    if(args.length < 3) {
      msg(sender, "<#FF3B47>การใช้งาน: <#FFB703>/mma setbusduration <ชื่อห้อง/arena> <วินาที 1.5-60>");
      return;
    }
    String arenaId = args[1];
    Arena arena = (Arena) registry.getPlugin().getArenaRegistry().getArena(arenaId);
    if(arena == null) {
      msg(sender, "<#FF3B47>ไม่พบห้องชื่อ " + arenaId);
      return;
    }
    double seconds;
    try {
      seconds = Double.parseDouble(args[2]);
    } catch(NumberFormatException ex) {
      msg(sender, "<#FF3B47>กรุณาระบุเวลาเป็นตัวเลขวินาที เช่น <#FFB703>/mma setbusduration " + arenaId + " 8.0");
      return;
    }
    if(seconds < 1.5 || seconds > 60.0) {
      msg(sender, "<#FF3B47>เวลารถบัสวิ่งต้องอยู่ระหว่าง <#FFB703>1.5 <#FF3B47>ถึง <#FFB703>60.0 <#FF3B47>วินาที");
      return;
    }

    FileConfiguration config = ConfigUtils.getConfig(registry.getPlugin(), "arenas");
    config.set("instances." + arenaId + ".buscutscene.duration", seconds);
    ConfigUtils.saveConfig(registry.getPlugin(), config, "arenas");

    arena.getCutsceneManager().setBusDriveDurationSeconds(seconds);
    msg(sender, "<#6EEB83>[+] ตั้งเวลารถบัสวิ่งจนเสียของห้อง <#FFB703>" + arenaId + " <#6EEB83>เป็น <#FFB703>"
      + String.format(Locale.US, "%.1f วินาที", seconds) + " <#6EEB83>เรียบร้อยแล้ว");
  }

  private static void handleSetBusSchematic(ArgumentsRegistry registry, CommandSender sender, String[] args) {
    if(args.length < 3) {
      msg(sender, "<#FF3B47>การใช้งาน: <#FFB703>/mma setbusschem <ชื่อห้อง/arena> <ชื่อไฟล์.schem>");
      return;
    }
    String arenaId = args[1];
    Arena arena = (Arena) registry.getPlugin().getArenaRegistry().getArena(arenaId);
    if(arena == null) {
      msg(sender, "<#FF3B47>ไม่พบห้องชื่อ " + arenaId);
      return;
    }

    String safeName = BusSchematicLoader.sanitizeFileName(args[2]);
    BusSchematicLoader.LoadedSchematic loaded = BusSchematicLoader.loadSchematic(registry.getPlugin(), safeName);
    if(loaded == null) {
      msg(sender, "<#FF3B47>ไม่พบหรืออ่านไฟล์ Schematic ไม่ได้: <#FFB703>plugins/MurderMystery/schematics/" + safeName);
      return;
    }

    FileConfiguration config = ConfigUtils.getConfig(registry.getPlugin(), "arenas");
    config.set("instances." + arenaId + ".buscutscene.schematic", safeName);
    ConfigUtils.saveConfig(registry.getPlugin(), config, "arenas");

    arena.getCutsceneManager().setBusSchematicName(safeName);
    msg(sender, "<#6EEB83>[+] ตั้งไฟล์ Schematic รถบัสของห้อง <#FFB703>" + arenaId + " <#6EEB83>เป็น <#F4F1DE>"
      + safeName + " <#B8B0C8>(" + loaded.getParts().size() + " ชิ้นส่วน)");
  }

  private static void handleHideBusArea(ArgumentsRegistry registry, Player player, String[] args, String subcommand) {
    if(args.length < 3) {
      msg(player, "<#FF3B47>การใช้งาน: <#FFB703>/mma " + subcommand + " <ชื่อห้อง/arena> <pos1|pos2|clear>");
      msg(player, "<#B8B0C8>ใช้ครอบมุมที่ 1 และมุมที่ 2 ของรถบัสจริงที่วางไว้ในแมพ เพื่อซ่อนด้วย Packet ระหว่างเล่น Cutscene ไม่ให้ซ้อนทับรถบัสที่กำลังวิ่งเข้ามาจอด");
      return;
    }
    String arenaId = args[1];
    Arena arena = (Arena) registry.getPlugin().getArenaRegistry().getArena(arenaId);
    if(arena == null) {
      msg(player, "<#FF3B47>ไม่พบห้องชื่อ " + arenaId);
      return;
    }

    String mode = args[2].trim().toLowerCase(Locale.ROOT);
    FileConfiguration config = ConfigUtils.getConfig(registry.getPlugin(), "arenas");
    CutsceneManager cm = arena.getCutsceneManager();

    if(mode.equals("clear") || mode.equals("off") || mode.equals("none") || mode.equals("reset")) {
      config.set("instances." + arenaId + ".buscutscene.hidepos1", null);
      config.set("instances." + arenaId + ".buscutscene.hidepos2", null);
      ConfigUtils.saveConfig(registry.getPlugin(), config, "arenas");
      cm.setBusHidePos1(null);
      cm.setBusHidePos2(null);
      msg(player, "<#FF3B47>[!] ล้างกรอบซ่อนรถบัสจริงชั่วคราวของห้อง <#FFB703>" + arenaId + " <#FF3B47>เรียบร้อยแล้ว");
      return;
    }

    if(!mode.equals("pos1") && !mode.equals("1") && !mode.equals("pos2") && !mode.equals("2")) {
      msg(player, "<#FF3B47>กรุณาระบุ <#FFB703>pos1<#FF3B47>, <#FFB703>pos2 <#FF3B47>หรือ <#FFB703>clear");
      return;
    }

    Block targetBlock = player.getTargetBlock(null, 5);
    Location loc = (targetBlock != null && targetBlock.getType() != Material.AIR)
      ? targetBlock.getLocation()
      : player.getLocation().getBlock().getLocation();
    String serialized = LocationSerializer.locationToString(loc);

    boolean firstCorner = mode.equals("pos1") || mode.equals("1");
    if(firstCorner) {
      config.set("instances." + arenaId + ".buscutscene.hidepos1", serialized);
      cm.setBusHidePos1(loc);
    } else {
      config.set("instances." + arenaId + ".buscutscene.hidepos2", serialized);
      cm.setBusHidePos2(loc);
    }
    ConfigUtils.saveConfig(registry.getPlugin(), config, "arenas");

    msg(player, "<#6EEB83>[+] ตั้งมุม <#FFB703>" + (firstCorner ? "Pos1" : "Pos2") + " <#6EEB83>สำหรับซ่อนรถบัสจริงระหว่าง Cutscene ที่ <#F4F1DE>"
      + loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ());
    if(cm.getBusHidePos1() != null && cm.getBusHidePos2() != null) {
      msg(player, "<#72EFDD>[พร้อมใช้งาน] <#B8B0C8>ระบบจะซ่อนบล็อกในกรอบนี้เฉพาะฝั่งผู้เล่นระหว่าง Cutscene และคืนค่าบล็อกจริงทันทีเมื่อรถบัสจอดสนิทและเริ่มเกม");
    } else {
      msg(player, "<#B8B0C8>ขั้นตอนต่อไป: ไปที่มุมตรงข้ามของรถบัสจริง แล้วพิมพ์ <#FFB703>/mma " + subcommand + " " + arenaId + " " + (firstCorner ? "pos2" : "pos1"));
    }
  }

  private static void handleClearBusCutscene(ArgumentsRegistry registry, Player player, String[] args) {
    if(args.length < 2) {
      msg(player, "<#FF3B47>การใช้งาน: <#FFB703>/mma clearbuscutscene <ชื่อห้อง/arena>");
      return;
    }
    String arenaId = args[1];
    Arena arena = (Arena) registry.getPlugin().getArenaRegistry().getArena(arenaId);
    if(arena == null) {
      msg(player, "<#FF3B47>ไม่พบห้องชื่อ " + arenaId);
      return;
    }
    FileConfiguration config = ConfigUtils.getConfig(registry.getPlugin(), "arenas");
    config.set("instances." + arenaId + ".buscutscene", null);
    ConfigUtils.saveConfig(registry.getPlugin(), config, "arenas");
    arena.getCutsceneManager().clearBusCutscene();
    msg(player, "<#FF3B47>[!] ล้างการตั้งค่ารถบัสเคลื่อนที่ (Moving Bus Cutscene) ของห้อง <#FFB703>" + arenaId + " <#FF3B47>เรียบร้อยแล้ว");
  }

  private static void removeBlockFromConfigTasks(FileConfiguration config, String arenaId, Location targetLoc) {
    if(config == null || targetLoc == null || targetLoc.getWorld() == null) {
      return;
    }
    List<String> generic = new ArrayList<>(config.getStringList("instances." + arenaId + ".tasklocations"));
    if(generic.removeIf(entry -> isSameSerializedBlock(entry, targetLoc))) {
      config.set("instances." + arenaId + ".tasklocations", generic);
    }
    org.bukkit.configuration.ConfigurationSection tasksSec = config.getConfigurationSection("instances." + arenaId + ".tasks");
    if(tasksSec != null) {
      for(String key : new ArrayList<>(tasksSec.getKeys(false))) {
        List<String> entries = new ArrayList<>(tasksSec.getStringList(key));
        if(entries.removeIf(entry -> isSameSerializedBlock(entry, targetLoc))) {
          tasksSec.set(key, entries.isEmpty() ? null : entries);
        }
      }
    }
  }

  private static boolean isSameSerializedBlock(String serialized, Location targetLoc) {
    Location parsed = LocationSerializer.getLocation(serialized);
    if(parsed == null || parsed.getWorld() == null) {
      return false;
    }
    return parsed.getWorld().equals(targetLoc.getWorld())
      && parsed.getBlockX() == targetLoc.getBlockX()
      && parsed.getBlockY() == targetLoc.getBlockY()
      && parsed.getBlockZ() == targetLoc.getBlockZ();
  }

  private static void sendTaskTypesHelp(CommandSender sender) {
    msg(sender, "<shadow:#000000FF><#5A4E7C>[</shadow><#FF7518>ประเภทภารกิจที่ใช้ได้<shadow:#000000FF><#5A4E7C>]</shadow>");
    int i = 1;
    for(TaskType t : TaskType.values()) {
      String visualBadge = t.isVisual() ? " <#72EFDD>[Visual]" : "";
      msg(sender, "<#F4F1DE>" + i + ". <#FFB703>" + t.getShortId() + " <#B8B0C8>- " + t.getIcon() + " " + t.getDisplayName() + visualBadge);
      i++;
    }
    msg(sender, "<shadow:#000000FF><#5A4E7C>(</shadow><#B8B0C8>ระบุเป็นหมายเลข 1-" + TaskType.values().length + " หรือชื่อย่อ<shadow:#000000FF><#5A4E7C>)</shadow>");
  }

  private static void sendSabotageTypesHelp(Player player) {
    msg(player, "<shadow:#000000FF><#5A4E7C>[</shadow><#FF3B47>ประเภท Sabotage ที่ใช้ได้<shadow:#000000FF><#5A4E7C>]</shadow>");
    int i = 1;
    for(SabotageType s : SabotageType.values()) {
      msg(player, "<#F4F1DE>" + i + ". <#FF3B47>" + s.getShortId() + " <#B8B0C8>- " + s.getIcon() + " " + s.getDisplayName());
      i++;
    }
    msg(player, "<shadow:#000000FF><#5A4E7C>(</shadow><#B8B0C8>ระบุเป็นหมายเลข 1-3 หรือชื่อ lights, engine, comms<shadow:#000000FF><#5A4E7C>)</shadow>");
  }
}
