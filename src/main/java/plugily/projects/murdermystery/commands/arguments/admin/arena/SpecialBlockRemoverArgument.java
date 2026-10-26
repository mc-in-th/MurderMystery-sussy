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
import plugily.projects.minigamesbox.classic.handlers.language.MessageBuilder;
import plugily.projects.minigamesbox.classic.utils.configuration.ConfigUtils;
import plugily.projects.minigamesbox.classic.utils.serialization.LocationSerializer;
import plugily.projects.murdermystery.Main;
import plugily.projects.murdermystery.arena.Arena;
import plugily.projects.murdermystery.arena.special.SpecialBlock;
import plugily.projects.murdermystery.commands.arguments.ArgumentsRegistry;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Tigerpanzer_02
 * <p>Created at 22.10.2020
 */
public class SpecialBlockRemoverArgument {
  public SpecialBlockRemoverArgument(ArgumentsRegistry registry) {
    registry.mapArgument(
      "murdermysteryadmin",
      new LabeledCommandArgument(
        "removeblock",
        "murdermystery.admin.removeblock",
        CommandArgument.ExecutorType.PLAYER,
        new LabelData(
          "/mma removeblock",
          "/mma removeblock",
          "&7Removes the special block you are looking at \n&6Permission: &7murdermystery.admin.removeblock")) {
        @Override
        public void execute(CommandSender sender, String[] args) {
          Player player = (Player) sender;
          Block targetBlock = player.getTargetBlock(null, 7);
          if(targetBlock == null || targetBlock.getType() == Material.AIR) {
            new MessageBuilder("&cPlease target a special block to continue!").player(player).sendPlayer();
            return;
          }

          for(Arena pluginArena : ((Main) registry.getPlugin()).getArenaRegistry().getPluginArenas()) {
            if(pluginArena == null || pluginArena.getSpecialBlocks().isEmpty()) {
              continue;
            }
            for(SpecialBlock specialBlock : new ArrayList<>(pluginArena.getSpecialBlocks())) {
              Location sbLoc = specialBlock.getLocation();
              if(sbLoc == null || sbLoc.getWorld() == null || !sbLoc.getWorld().equals(player.getWorld())) {
                continue;
              }
              if(sbLoc.getBlock().equals(targetBlock)) {
                FileConfiguration config = ConfigUtils.getConfig(registry.getPlugin(), "arenas");
                pluginArena.getSpecialBlocks().remove(specialBlock);
                if(specialBlock.getArmorStandHologram() != null) {
                  specialBlock.getArmorStandHologram().delete();
                }
                if(specialBlock.getSpecialBlockType() == SpecialBlock.SpecialBlockType.RAPID_TELEPORTATION) {
                  specialBlock.setTextDisplay(null);
                  pluginArena.removeNearbyDisplays(sbLoc.clone().add(0.5, 1.2, 0.5), 1.2, 1.8, 1.2, false);
                }

                String serializedLoc = LocationSerializer.locationToString(sbLoc);
                if(specialBlock.getSpecialBlockType() == SpecialBlock.SpecialBlockType.MYSTERY_CAULDRON) {
                  removeFromConfigList(config, "instances." + pluginArena.getId() + ".mystery-cauldrons", serializedLoc);
                } else if(specialBlock.getSpecialBlockType() == SpecialBlock.SpecialBlockType.PRAISE_DEVELOPER) {
                  removeFromConfigList(config, "instances." + pluginArena.getId() + ".confessionals", serializedLoc);
                } else if(specialBlock.getSpecialBlockType() == SpecialBlock.SpecialBlockType.RAPID_TELEPORTATION) {
                  removeFromConfigList(config, "instances." + pluginArena.getId() + ".sewers", serializedLoc);
                  removeFromConfigList(config, "instances." + pluginArena.getId() + ".rapid-teleportation", serializedLoc);
                }

                ConfigUtils.saveConfig(registry.getPlugin(), config, "arenas");
                new MessageBuilder("&cRemoved special block at loc "
                  + serializedLoc
                  + " from arena "
                  + pluginArena.getId()).player(player).sendPlayer();
                return;
              }
            }
          }
          new MessageBuilder("&cPlease target a special block to continue!").player(player).sendPlayer();
        }
      });
  }

  private static void removeFromConfigList(FileConfiguration config, String path, String serializedLoc) {
    List<String> list = config.getStringList(path);
    if(list.remove(serializedLoc)) {
      config.set(path, list);
    }
  }
}
