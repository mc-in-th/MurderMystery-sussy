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

package plugily.projects.murdermystery;

import java.io.File;
import java.lang.reflect.Field;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jetbrains.annotations.TestOnly;
import plugily.projects.minigamesbox.classic.PluginMain;
import plugily.projects.minigamesbox.classic.handlers.setup.SetupInventory;
import plugily.projects.minigamesbox.classic.handlers.setup.categories.PluginSetupCategoryManager;
import plugily.projects.minigamesbox.classic.utils.services.ServiceRegistry;
import plugily.projects.minigamesbox.classic.utils.services.metrics.Metrics;
import plugily.projects.murdermystery.arena.*;
import plugily.projects.murdermystery.arena.managers.NameTagPacketManager;
import plugily.projects.murdermystery.arena.special.SpecialBlockEvents;
import plugily.projects.murdermystery.arena.special.mysterypotion.MysteryPotionRegistry;
import plugily.projects.murdermystery.arena.special.pray.PrayerRegistry;
import plugily.projects.murdermystery.boot.AdditionalValueInitializer;
import plugily.projects.murdermystery.boot.MessageInitializer;
import plugily.projects.murdermystery.boot.PlaceholderInitializer;
import plugily.projects.murdermystery.commands.arguments.ArgumentsRegistry;
import plugily.projects.murdermystery.events.PluginEvents;
import plugily.projects.murdermystery.handlers.CorpseHandler;
import plugily.projects.murdermystery.handlers.lastwords.LastWordsManager;
import plugily.projects.murdermystery.handlers.setup.SetupCategoryManager;
import plugily.projects.murdermystery.handlers.skins.sword.SwordSkinManager;
import plugily.projects.murdermystery.handlers.trails.BowTrailsHandler;
import plugily.projects.murdermystery.handlers.trails.TrailsManager;
import plugily.projects.murdermystery.handlers.voicechat.VoiceChatManager;
import plugily.projects.murdermystery.utils.HalloweenChat;

/**
 * Created by Tigerpanzer_02 on 13.03.2022
 */
public class Main extends PluginMain {
  private ArenaRegistry arenaRegistry;
  private ArenaManager arenaManager;
  private ArgumentsRegistry argumentsRegistry;
  private LastWordsManager lastWordsManager;
  private TrailsManager trailsManager;
  private SwordSkinManager swordSkinManager;
  private HookManager hookManager;
  private CorpseHandler corpseHandler;
  private VoiceChatManager voiceChatManager;

  @TestOnly
  public Main() {
    super();
  }

  @Override
  public void onEnable() {
    long start = System.currentTimeMillis();
    disableExternalServiceRegistry();
    MessageInitializer messageInitializer = new MessageInitializer(this);
    super.onEnable();
    if(!isEnabled()) {
      return;
    }
    getDebugger().debug("[System] [Plugin] Initialization start");
    plugily.projects.murdermystery.arena.cutscene.BusSchematicLoader.ensureDefaultSchematics(this);
    arenaRegistry = new ArenaRegistry(this);
    new PlaceholderInitializer(this);
    messageInitializer.registerMessages();
    HalloweenChat.applyToLanguageConfig(this);
    new AdditionalValueInitializer(this);
    initializePluginClasses();
    NameTagPacketManager.init(this);

    getServer().getScheduler().scheduleSyncRepeatingTask(this, ArenaUtils::syncAllNameTags, 15L, 15L);

    getDebugger().debug("Full {0} plugin enabled", getName());
    getDebugger()
      .debug(
        "[System] [Plugin] Initialization finished took {0}ms",
        System.currentTimeMillis() - start);
  }

  private void disableExternalServiceRegistry() {
    try {
      File corruptLocaleData = new File(getDataFolder(), "locales/locale_data.yml");
      if(corruptLocaleData.exists()) {
        corruptLocaleData.delete();
      }
      File specialItemsFile = new File(getDataFolder(), "special_items.yml");
      if(specialItemsFile.exists()) {
        YamlConfiguration specialItemsConfig = YamlConfiguration.loadConfiguration(specialItemsFile);
        boolean modified = false;
        if(specialItemsConfig.contains("Role-Pass")) {
          specialItemsConfig.set("Role-Pass", null);
          modified = true;
        }
        String lobbyLeaveName = specialItemsConfig.getString("Lobby-Leave.displayname", "");
        if(lobbyLeaveName.contains("ออกจากห้องเกม")) {
          specialItemsConfig.set("Lobby-Leave.displayname", "&#FF3B47&lลงจากรถบัส &#5A4E7C(&#B8B0C8คลิกขวา&#5A4E7C)");
          specialItemsConfig.set("Lobby-Leave.lore", java.util.Collections.singletonList("&#B8B0C8คลิกขวาเพื่อลงจากรถบัสกลับไปยังล็อบบี้"));
          modified = true;
        }
        String spectatorLeaveName = specialItemsConfig.getString("Spectator-Leave.displayname", "");
        if(spectatorLeaveName.contains("ออกจากห้องเกม")) {
          specialItemsConfig.set("Spectator-Leave.displayname", "&#FF3B47&lลงจากรถบัส &#5A4E7C(&#B8B0C8คลิกขวา&#5A4E7C)");
          specialItemsConfig.set("Spectator-Leave.lore", java.util.Collections.singletonList("&#B8B0C8คลิกขวาเพื่อลงจากรถบัสกลับไปยังล็อบบี้"));
          modified = true;
        }
        if(modified) {
          specialItemsConfig.save(specialItemsFile);
        }
      }
      Field registeredServiceField = ServiceRegistry.class.getDeclaredField("registeredService");
      registeredServiceField.setAccessible(true);
      registeredServiceField.set(null, this);

      Field serviceEnabledField = ServiceRegistry.class.getDeclaredField("serviceEnabled");
      serviceEnabledField.setAccessible(true);
      serviceEnabledField.setBoolean(null, false);
    } catch(Exception ignored) {
    }
  }

  @Override
  public void onDisable() {
    plugily.projects.murdermystery.arena.cutscene.CutsceneManager.shutdownAll(this);
    NameTagPacketManager.shutdown(this);
    if(arenaRegistry != null) {
      for(Arena arena : arenaRegistry.getPluginArenas()) {
        if(arena.getVentManager() != null) {
          arena.getVentManager().cleanup();
        }
        arena.getCutsceneManager().cleanup();
        arena.getVotingManager().cleanup();
        arena.getSabotageManager().cleanup();
        arena.getTaskManager().cleanup();
        arena.clearTextDisplays();
        arena.clearGlowEntries();
      }
    }
    if(corpseHandler != null) {
      corpseHandler.shutdown();
    }
    if(voiceChatManager != null) {
      voiceChatManager.unregister();
    }
    super.onDisable();
  }

  public void initializePluginClasses() {
    addFileName("lastwords");
    addFileName("powerups");
    addFileName("skins");
    addFileName("special_blocks");
    addFileName("trails");
    Arena.init(this);
    ArenaUtils.init(this);
    new ArenaEvents(this);
    arenaManager = new ArenaManager(this);
    arenaRegistry.registerArenas();
    getSignManager().loadSigns();
    getSignManager().updateSigns();
    argumentsRegistry = new ArgumentsRegistry(this);

    lastWordsManager = new LastWordsManager(this);
    new BowTrailsHandler(this);
    MysteryPotionRegistry.init(this);
    PrayerRegistry.init(this);
    new SpecialBlockEvents(this);
    trailsManager = new TrailsManager(this);
    hookManager = new HookManager(this);
    corpseHandler = new CorpseHandler(this);
    voiceChatManager = new VoiceChatManager(this);
    swordSkinManager = new SwordSkinManager(this);
    new PluginEvents(this);
    addPluginMetrics();
  }

  private void addPluginMetrics() {
    getMetrics()
      .addCustomChart(
        new Metrics.SimplePie(
          "hooked_addons",
          () -> {
            if(getServer().getPluginManager().getPlugin("MurderMystery-Extension") != null) {
              return "Extension";
            }
            return "None";
          }));
  }

  @Override
  public ArenaRegistry getArenaRegistry() {
    return arenaRegistry;
  }

  @Override
  public ArgumentsRegistry getArgumentsRegistry() {
    return argumentsRegistry;
  }

  @Override
  public ArenaManager getArenaManager() {
    return arenaManager;
  }

  public LastWordsManager getLastWordsManager() {
    return lastWordsManager;
  }

  public TrailsManager getTrailsManager() {
    return trailsManager;
  }

  public SwordSkinManager getSwordSkinManager() {
    return swordSkinManager;
  }

  public HookManager getHookManager() {
    return hookManager;
  }

  public CorpseHandler getCorpseHandler() {
    return corpseHandler;
  }

  public VoiceChatManager getVoiceChatManager() {
    return voiceChatManager;
  }

  @Override
  public PluginSetupCategoryManager getSetupCategoryManager(SetupInventory setupInventory) {
    return new SetupCategoryManager(setupInventory);
  }
}
