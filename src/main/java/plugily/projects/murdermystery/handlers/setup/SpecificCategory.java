/*
 *
 * MurderMystery
 * Copyright (C) 2021 Plugily Projects - maintained by Tigerpanzer_02, 2Wild4You and contributors
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
 *
 */

package plugily.projects.murdermystery.handlers.setup;

import org.bukkit.Material;
import plugily.projects.minigamesbox.classic.handlers.setup.categories.PluginSpecificCategory;
import plugily.projects.minigamesbox.classic.handlers.setup.items.category.CountItem;
import plugily.projects.minigamesbox.classic.handlers.setup.items.category.MaterialLocationItem;
import plugily.projects.minigamesbox.classic.handlers.setup.items.category.MaterialMultiLocationItem;
import plugily.projects.minigamesbox.classic.handlers.setup.items.category.MultiLocationItem;
import plugily.projects.minigamesbox.classic.utils.helper.ItemBuilder;
import plugily.projects.minigamesbox.classic.utils.helper.MaterialUtils;
import plugily.projects.minigamesbox.classic.utils.version.xseries.XMaterial;
import plugily.projects.minigamesbox.inventory.normal.NormalFastInv;

import java.util.Collections;


/**
 * @author Tigerpanzer_02
 * <p>
 * Created at 01.07.2022
 */
public class SpecificCategory extends PluginSpecificCategory {
  @Override
  public void addItems(NormalFastInv gui) {
    super.addItems(gui);

    CountItem playerMurderer = new CountItem(getSetupInventory(), new ItemBuilder(XMaterial.IRON_SWORD.parseMaterial()), "Player Per Murderer", "How many murderer should be ingame? This means \none murderer for that amount of players. Default: \n5 players are 1 murderer, that means if we have \n14 Players it will calculate 2 murderer! \nSet it to 1 if you want only one murderer ", "playerpermurderer");
    gui.setItem((getInventoryLine() * 9) + 1, playerMurderer);
    getItemList().add(playerMurderer);
  }

}