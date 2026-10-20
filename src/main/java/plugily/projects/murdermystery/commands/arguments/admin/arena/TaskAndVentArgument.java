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
import plugily.projects.minigamesbox.classic.commands.arguments.data.CommandArgument;
import plugily.projects.minigamesbox.classic.commands.arguments.data.LabelData;
import plugily.projects.minigamesbox.classic.commands.arguments.data.LabeledCommandArgument;
import plugily.projects.minigamesbox.classic.utils.configuration.ConfigUtils;
import plugily.projects.minigamesbox.classic.utils.serialization.LocationSerializer;
import plugily.projects.murdermystery.arena.Arena;
import plugily.projects.murdermystery.arena.sabotage.SabotageType;
import plugily.projects.murdermystery.arena.special.SpecialBlock;
import plugily.projects.murdermystery.arena.task.TaskType;
import plugily.projects.murdermystery.commands.arguments.ArgumentsRegistry;

import java.util.ArrayList;
import java.util.List;

public class TaskAndVentArgument {

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
            player.sendMessage("§cการใช้งาน: /mma addtask <ชื่อห้อง/arena> [ประเภทภารกิจ/taskType]");
            sendTaskTypesHelp(player);
            return;
          }
          String arenaId = args[1];
          Arena arena = (Arena) registry.getPlugin().getArenaRegistry().getArena(arenaId);
          if(arena == null) {
            player.sendMessage("§cไม่พบห้องชื่อ " + arenaId);
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
              player.sendMessage("§cไม่พบประเภทภารกิจ: §e" + args[2]);
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

            player.sendMessage("§a[+] ตั้งจุดภารกิจ §6" + type.getIcon() + " " + type.getDisplayName()
              + " §aให้ห้อง §e" + arenaId + " §aเรียบร้อยแล้วที่ §f"
              + loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ()
              + " §a(รวมเฉพาะภารกิจนี้ §e" + list.size() + " §aจุด)");
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

            player.sendMessage("§a[+] เพิ่มจุดภารกิจทั่วไป (Generic Task Point) ให้ห้อง §e" + arenaId + " §aเรียบร้อยแล้วที่ §f"
              + loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ()
              + " §a(รวมทั้งหมด §e" + list.size() + " §aจุด)");
            player.sendMessage("§7Tip: สามารถระบุภารกิจเจาะจงได้ เช่น: §e/mma settask " + arenaId + " diesel");
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
            player.sendMessage("§cการใช้งาน: /mma settask <ชื่อห้อง/arena> <ประเภทภารกิจ/taskType>");
            sendTaskTypesHelp(player);
            return;
          }
          String arenaId = args[1];
          Arena arena = (Arena) registry.getPlugin().getArenaRegistry().getArena(arenaId);
          if(arena == null) {
            player.sendMessage("§cไม่พบห้องชื่อ " + arenaId);
            return;
          }

          TaskType type = TaskType.fromInput(args[2]);
          if(type == null) {
            player.sendMessage("§cไม่พบประเภทภารกิจ: §e" + args[2]);
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

          player.sendMessage("§a[+] ตั้งจุดภารกิจ §6" + type.getIcon() + " " + type.getDisplayName()
            + " §aให้ห้อง §e" + arenaId + " §aเรียบร้อยแล้วที่ §f"
            + loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ()
            + " §a(รวมเฉพาะภารกิจนี้ §e" + list.size() + " §aจุด)");
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
            player.sendMessage("§cการใช้งาน: /mma listtasks <ชื่อห้อง/arena>");
            return;
          }
          String arenaId = args[1];
          Arena arena = (Arena) registry.getPlugin().getArenaRegistry().getArena(arenaId);
          if(arena == null) {
            player.sendMessage("§cไม่พบห้องชื่อ " + arenaId);
            return;
          }

          player.sendMessage("§6================= [ รายการจุดภารกิจ: §e" + arenaId + " §6] =================");
          int idx = 1;
          for(TaskType type : TaskType.values()) {
            List<Location> locs = arena.getTaskLocations(type);
            if(locs.isEmpty()) {
              player.sendMessage("§e" + idx + ". " + type.getIcon() + " " + type.getDisplayName()
                + " §7(" + type.getShortId() + "): §cยังไม่ได้ตั้งค่า (จะสุ่มจุดทั่วไป)");
            } else {
              player.sendMessage("§e" + idx + ". " + type.getIcon() + " " + type.getDisplayName()
                + " §7(" + type.getShortId() + "): §a" + locs.size() + " จุด");
              for(Location l : locs) {
                player.sendMessage("   §7+- §f" + l.getBlockX() + ", " + l.getBlockY() + ", " + l.getBlockZ());
              }
            }
            idx++;
          }
          List<Location> genericLocs = arena.getTaskLocations();
          player.sendMessage("§bจุดภารกิจทั่วไป (Generic Task Points): "
            + (genericLocs.isEmpty() ? "§cไม่มี" : "§a" + genericLocs.size() + " จุด"));
          Location cf = arena.getCampfireLocation();
          player.sendMessage("§6 จุดกองไฟประชุมฉุกเฉิน (Campfire): "
            + (cf == null ? "§cยังไม่ได้ตั้งค่า (/mma setcampfire " + arenaId + ")" : "§a" + cf.getBlockX() + ", " + cf.getBlockY() + ", " + cf.getBlockZ()));
          player.sendMessage("§6=============================================================");
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
            player.sendMessage("§cการใช้งาน: /mma cleartasks <ชื่อห้อง/arena> [ประเภทภารกิจ/taskType]");
            return;
          }
          String arenaId = args[1];
          Arena arena = (Arena) registry.getPlugin().getArenaRegistry().getArena(arenaId);
          if(arena == null) {
            player.sendMessage("§cไม่พบห้องชื่อ " + arenaId);
            return;
          }

          FileConfiguration config = ConfigUtils.getConfig(registry.getPlugin(), "arenas");

          if(args.length >= 3) {
            TaskType type = TaskType.fromInput(args[2]);
            if(type == null) {
              player.sendMessage("§cไม่พบประเภทภารกิจ: §e" + args[2]);
              sendTaskTypesHelp(player);
              return;
            }
            config.set("instances." + arenaId + ".tasks." + type.name(), null);
            ConfigUtils.saveConfig(registry.getPlugin(), config, "arenas");
            arena.clearSpecificTaskLocations(type);
            player.sendMessage("§c[!] ล้างจุดภารกิจ §6" + type.getIcon() + " " + type.getDisplayName() + " §cของห้อง §e" + arenaId + " §cเรียบร้อยแล้ว");
          } else {
            config.set("instances." + arenaId + ".tasklocations", new ArrayList<String>());
            config.set("instances." + arenaId + ".tasks", null);
            ConfigUtils.saveConfig(registry.getPlugin(), config, "arenas");
            arena.setTaskLocations(new ArrayList<>());
            arena.clearSpecificTaskLocations(null);
            player.sendMessage("§c[!] ล้างจุดภารกิจทั้งหมด (ทุกประเภท) ของห้อง §e" + arenaId + " §cเรียบร้อยแล้ว");
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
            player.sendMessage("§cการใช้งาน: /mma listsabotages <ชื่อห้อง/arena>");
            return;
          }
          String arenaId = args[1];
          Arena arena = (Arena) registry.getPlugin().getArenaRegistry().getArena(arenaId);
          if(arena == null) {
            player.sendMessage("§cไม่พบห้องชื่อ " + arenaId);
            return;
          }

          player.sendMessage("§c================= [ รายการบล็อกแก้ Sabotage: §e" + arenaId + " §c] =================");
          int idx = 1;
          for(SabotageType type : SabotageType.values()) {
            Location l = arena.getSabotageLocation(type);
            if(l == null) {
              player.sendMessage("§e" + idx + ". " + type.getIcon() + " " + type.getDisplayName()
                + " §7(" + type.getShortId() + "): §cยังไม่ได้ตั้งค่า");
            } else {
              player.sendMessage("§e" + idx + ". " + type.getIcon() + " " + type.getDisplayName()
                + " §7(" + type.getShortId() + "): §a"
                + l.getBlockX() + ", " + l.getBlockY() + ", " + l.getBlockZ());
            }
            idx++;
          }
          player.sendMessage("§c=============================================================");
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
            player.sendMessage("§cการใช้งาน: /mma clearsabotages <ชื่อห้อง/arena> [ประเภท/sabotageType]");
            return;
          }
          String arenaId = args[1];
          Arena arena = (Arena) registry.getPlugin().getArenaRegistry().getArena(arenaId);
          if(arena == null) {
            player.sendMessage("§cไม่พบห้องชื่อ " + arenaId);
            return;
          }

          FileConfiguration config = ConfigUtils.getConfig(registry.getPlugin(), "arenas");

          if(args.length >= 3) {
            SabotageType type = SabotageType.fromInput(args[2]);
            if(type == null) {
              player.sendMessage("§cไม่พบประเภท Sabotage: §e" + args[2]);
              sendSabotageTypesHelp(player);
              return;
            }
            config.set("instances." + arenaId + ".sabotages." + type.name(), null);
            ConfigUtils.saveConfig(registry.getPlugin(), config, "arenas");
            arena.clearSabotageLocations(type);
            player.sendMessage("§c[!] ล้างจุด Sabotage §6" + type.getIcon() + " " + type.getDisplayName() + " §cของห้อง §e" + arenaId + " §cเรียบร้อยแล้ว");
          } else {
            config.set("instances." + arenaId + ".sabotages", null);
            ConfigUtils.saveConfig(registry.getPlugin(), config, "arenas");
            arena.clearSabotageLocations(null);
            player.sendMessage("§c[!] ล้างจุด Sabotage ทั้งหมดของห้อง §e" + arenaId + " §cเรียบร้อยแล้ว");
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
            player.sendMessage("§cการใช้งาน: /mma setcampfire <ชื่อห้อง/arena>");
            return;
          }
          String arenaId = args[1];
          Arena arena = (Arena) registry.getPlugin().getArenaRegistry().getArena(arenaId);
          if(arena == null) {
            player.sendMessage("§cไม่พบห้องชื่อ " + arenaId);
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

          player.sendMessage("§a[+] ตั้งจุดกองไฟประชุมฉุกเฉิน (Campfire) ให้ห้อง §e" + arenaId + " §aเรียบร้อยแล้วที่ §f"
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
            player.sendMessage("§cการใช้งาน: /mma clearcampfire <ชื่อห้อง/arena>");
            return;
          }
          String arenaId = args[1];
          Arena arena = (Arena) registry.getPlugin().getArenaRegistry().getArena(arenaId);
          if(arena == null) {
            player.sendMessage("§cไม่พบห้องชื่อ " + arenaId);
            return;
          }

          FileConfiguration config = ConfigUtils.getConfig(registry.getPlugin(), "arenas");
          config.set("instances." + arenaId + ".campfire", null);
          ConfigUtils.saveConfig(registry.getPlugin(), config, "arenas");

          arena.setCampfireLocation(null);

          player.sendMessage("§c[!] ล้างจุดกองไฟประชุมฉุกเฉินของห้อง §e" + arenaId + " §cเรียบร้อยแล้ว");
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
            player.sendMessage("§cการใช้งาน: /mma addvent <ชื่อห้อง/arena>");
            return;
          }
          String arenaId = args[1];
          Arena arena = (Arena) registry.getPlugin().getArenaRegistry().getArena(arenaId);
          if(arena == null) {
            player.sendMessage("§cไม่พบห้องชื่อ " + arenaId);
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

          player.sendMessage("§a[+] เพิ่มท่อระบายน้ำ (Sewer Vent) ให้ห้อง §e" + arenaId + " §aเรียบร้อยแล้วที่ §f"
            + loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ()
            + " §a(รวมทั้งหมด §e" + list.size() + " §aจุด)");
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
            player.sendMessage("§cการใช้งาน: /mma clearvents <ชื่อห้อง/arena>");
            return;
          }
          String arenaId = args[1];
          Arena arena = (Arena) registry.getPlugin().getArenaRegistry().getArena(arenaId);
          if(arena == null) {
            player.sendMessage("§cไม่พบห้องชื่อ " + arenaId);
            return;
          }

          FileConfiguration config = ConfigUtils.getConfig(registry.getPlugin(), "arenas");
          config.set("instances." + arenaId + ".sewers", new ArrayList<String>());
          config.set("instances." + arenaId + ".rapid-teleportation", new ArrayList<String>());
          ConfigUtils.saveConfig(registry.getPlugin(), config, "arenas");

          arena.getSpecialBlocks().removeIf(b -> b.getSpecialBlockType() == SpecialBlock.SpecialBlockType.RAPID_TELEPORTATION);

          player.sendMessage("§c[!] ล้างท่อระบายน้ำทั้งหมดของห้อง §e" + arenaId + " §cเรียบร้อยแล้ว");
        }
      }
    );
  }

  private static void handleSetSabotage(ArgumentsRegistry registry, Player player, String[] args) {
    if(args.length < 3) {
      player.sendMessage("§cการใช้งาน: /mma setsabotage <ชื่อห้อง/arena> <ประเภท/sabotageType>");
      sendSabotageTypesHelp(player);
      return;
    }
    String arenaId = args[1];
    Arena arena = (Arena) registry.getPlugin().getArenaRegistry().getArena(arenaId);
    if(arena == null) {
      player.sendMessage("§cไม่พบห้องชื่อ " + arenaId);
      return;
    }

    SabotageType type = SabotageType.fromInput(args[2]);
    if(type == null) {
      player.sendMessage("§cไม่พบประเภท Sabotage: §e" + args[2]);
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

    player.sendMessage("§a[+] ตั้งบล็อกแก้ Sabotage §c" + type.getIcon() + " " + type.getDisplayName()
      + " §aให้ห้อง §e" + arenaId + " §aเรียบร้อยแล้วที่ §f"
      + loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ());
  }

  private static void sendTaskTypesHelp(Player player) {
    player.sendMessage("§e=== ประเภทภารกิจที่ใช้ได้ (Available Tasks) ===");
    int i = 1;
    for(TaskType t : TaskType.values()) {
      player.sendMessage("§f" + i + ". §e" + t.getShortId() + " §7- " + t.getIcon() + " " + t.getDisplayName());
      i++;
    }
    player.sendMessage("§7(สามารถระบุเป็นหมายเลข 1-7 หรือชื่อ เช่น diesel, wiring, panel)");
  }

  private static void sendSabotageTypesHelp(Player player) {
    player.sendMessage("§c=== ประเภท Sabotage ที่ใช้ได้ (Available Sabotages) ===");
    int i = 1;
    for(SabotageType s : SabotageType.values()) {
      player.sendMessage("§f" + i + ". §c" + s.getShortId() + " §7- " + s.getIcon() + " " + s.getDisplayName());
      i++;
    }
    player.sendMessage("§7(สามารถระบุเป็นหมายเลข 1-3 หรือชื่อ lights, engine, comms)");
  }
}
