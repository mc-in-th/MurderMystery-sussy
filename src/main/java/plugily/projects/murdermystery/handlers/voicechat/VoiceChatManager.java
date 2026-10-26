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

import plugily.projects.murdermystery.HookManager;
import plugily.projects.murdermystery.Main;

public class VoiceChatManager {

  private final Main plugin;
  private Object voiceChatPlugin;

  public VoiceChatManager(Main plugin) {
    this.plugin = plugin;
    registerVoiceChat();
  }

  private void registerVoiceChat() {
    if(plugin.getHookManager() == null || !plugin.getHookManager().isFeatureEnabled(HookManager.HookFeature.VOICE_CHAT)) {
      return;
    }
    try {
      voiceChatPlugin = MurderMysteryVoiceChatPlugin.register(plugin);
    } catch(Throwable ignored) {
    }
  }

  public void unregister() {
    if(voiceChatPlugin != null) {
      try {
        if(voiceChatPlugin instanceof MurderMysteryVoiceChatPlugin) {
          ((MurderMysteryVoiceChatPlugin) voiceChatPlugin).deactivate();
        }
        plugin.getServer().getServicesManager().unregister(voiceChatPlugin);
      } catch(Throwable ignored) {
      }
      voiceChatPlugin = null;
    }
  }
}
