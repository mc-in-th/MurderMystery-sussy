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

package plugily.projects.murdermystery.utils;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import plugily.projects.murdermystery.Main;

public final class HalloweenChat {

  public static final String PUMPKIN_ORANGE = "#FF7518";
  public static final String AMBER_GOLD = "#FFB703";
  public static final String FLAME_ORANGE = "#FF5400";
  public static final String BLOOD_RED = "#FF3B47";
  public static final String DARK_CRIMSON = "#9E001A";
  public static final String WITCH_PURPLE = "#9D4EDD";
  public static final String SPECTRAL_LAVENDER = "#C77DFF";
  public static final String GHOST_CYAN = "#72EFDD";
  public static final String SLIME_GREEN = "#6EEB83";
  public static final String POISON_GREEN = "#2D8A4E";
  public static final String BONE_WHITE = "#F4F1DE";
  public static final String MIST_GRAY = "#B8B0C8";
  public static final String CRYPT_DARK = "#5A4E7C";
  public static final String MIDNIGHT_BLACK = "#1A1423";

  public static final String GRADIENT_PUMPKIN = "<gradient:#FF5400:#FFB703>";
  public static final String GRADIENT_BLOOD = "<gradient:#D90429:#FF5400>";
  public static final String GRADIENT_WITCH = "<gradient:#9D4EDD:#FF7518>";
  public static final String GRADIENT_GHOST = "<gradient:#7B2CBF:#C77DFF>";
  public static final String GRADIENT_SLIME = "<gradient:#38B000:#9EF01A>";
  public static final String GRADIENT_SOUL = "<gradient:#00B4D8:#72EFDD>";

  private static final MiniMessage MINI_MESSAGE = MiniMessage.miniMessage();
  private static final LegacyComponentSerializer LEGACY_SERIALIZER = LegacyComponentSerializer.builder()
      .hexColors()
      .useUnusualXRepeatedCharacterHexFormat()
      .build();

  private static final Pattern BUKKIT_HEX_PATTERN = Pattern.compile("(?i)[§&]x([§&][0-9a-f]){6}");
  private static final Pattern AMPERSAND_HEX_PATTERN = Pattern.compile("(?i)(?<![<:/])&?#([0-9a-f]{6})");
  private static final Pattern LEGACY_CODE_PATTERN = Pattern.compile("(?i)[§&]([0-9a-fk-or])");
  private static final Pattern THAI_COMBINING_PREFIX_PATTERN = Pattern.compile(
      "(?i)(?:[§&]x(?:[§&][0-9a-f]){6}|[§&][0-9a-fk-or])+(?=[\\u0E31\\u0E34-\\u0E3A\\u0E47-\\u0E4E])"
  );
  private static final Pattern NON_TAG_LT_PATTERN = Pattern.compile(
      "(?i)<(?!(/?(gradient|color|rainbow|transition|bold|b|italic|i|underlined|u|strikethrough|st|obfuscated|obf|reset|newline|hover|click)(:[^>]*)?>|/?#[0-9a-f]{6}>))"
  );

  private HalloweenChat() {
  }

  public static String color(String input) {
    if(input == null || input.isEmpty()) {
      return "";
    }
    if(input.indexOf('\n') == -1) {
      return colorLine(input);
    }
    String[] lines = input.split("\n", -1);
    StringBuilder builder = new StringBuilder(input.length() + 32);
    for(int i = 0; i < lines.length; i++) {
      if(i > 0) {
        builder.append('\n');
      }
      builder.append(colorLine(lines[i]));
    }
    return builder.toString();
  }

  public static String colorLine(String line) {
    if(line == null || line.isEmpty()) {
      return "";
    }
    String converted = toMiniMessageFormat(line);
    try {
      Component component = MINI_MESSAGE.deserialize(converted);
      String serialized = LEGACY_SERIALIZER.serialize(component);
      return THAI_COMBINING_PREFIX_PATTERN.matcher(serialized).replaceAll("");
    } catch(Exception ex) {
      return ChatColor.translateAlternateColorCodes('&', line);
    }
  }

  public static Component component(String input) {
    if(input == null || input.isEmpty()) {
      return Component.empty();
    }
    String converted = toMiniMessageFormat(input);
    return MINI_MESSAGE.deserialize(converted);
  }

  public static String strip(String input) {
    if(input == null || input.isEmpty()) {
      return "";
    }
    String stripped = ChatColor.stripColor(input);
    return stripped == null ? "" : stripped;
  }

  public static List<String> colorList(List<String> lines) {
    if(lines == null) {
      return new ArrayList<>();
    }
    List<String> result = new ArrayList<>(lines.size());
    for(String line : lines) {
      result.add(color(line));
    }
    return result;
  }

  public static List<String> colorLines(String... lines) {
    if(lines == null) {
      return new ArrayList<>();
    }
    List<String> result = new ArrayList<>(lines.length);
    for(String line : lines) {
      result.add(color(line));
    }
    return result;
  }

  private static String toMiniMessageFormat(String input) {
    Matcher bukkitHexMatcher = BUKKIT_HEX_PATTERN.matcher(input);
    StringBuffer hexBuffer = new StringBuffer();
    while(bukkitHexMatcher.find()) {
      String match = bukkitHexMatcher.group();
      StringBuilder hex = new StringBuilder(6);
      for(int i = 2; i < match.length(); i += 2) {
        hex.append(match.charAt(i + 1));
      }
      String hexUpper = hex.toString().toUpperCase(Locale.ROOT);
      if(bukkitHexMatcher.start() > 0 && input.charAt(bukkitHexMatcher.start() - 1) == ':') {
        bukkitHexMatcher.appendReplacement(hexBuffer, "#" + hexUpper);
      } else {
        bukkitHexMatcher.appendReplacement(hexBuffer, "<reset><#" + hexUpper + ">");
      }
    }
    bukkitHexMatcher.appendTail(hexBuffer);

    Matcher ampHexMatcher = AMPERSAND_HEX_PATTERN.matcher(hexBuffer.toString());
    StringBuffer ampBuffer = new StringBuffer();
    while(ampHexMatcher.find()) {
      String hex = ampHexMatcher.group(1).toUpperCase(Locale.ROOT);
      ampHexMatcher.appendReplacement(ampBuffer, "<reset><#" + hex + ">");
    }
    ampHexMatcher.appendTail(ampBuffer);

    Matcher legacyMatcher = LEGACY_CODE_PATTERN.matcher(ampBuffer.toString());
    StringBuffer legacyBuffer = new StringBuffer();
    while(legacyMatcher.find()) {
      char code = Character.toLowerCase(legacyMatcher.group(1).charAt(0));
      String replacement = mapLegacyCode(code);
      legacyMatcher.appendReplacement(legacyBuffer, replacement);
    }
    legacyMatcher.appendTail(legacyBuffer);

    return NON_TAG_LT_PATTERN.matcher(legacyBuffer.toString()).replaceAll("\\\\<");
  }

  private static String mapLegacyCode(char code) {
    switch(code) {
      case '0':
        return "<reset><#1A1423>";
      case '1':
        return "<reset><#5A189A>";
      case '2':
        return "<reset><#2D8A4E>";
      case '3':
        return "<reset><#00B4D8>";
      case '4':
        return "<reset><#9E001A>";
      case '5':
        return "<reset><#9D4EDD>";
      case '6':
        return "<reset><#FF7518>";
      case '7':
        return "<reset><#B8B0C8>";
      case '8':
        return "<reset><#5A4E7C>";
      case '9':
        return "<reset><#C77DFF>";
      case 'a':
        return "<reset><#6EEB83>";
      case 'b':
        return "<reset><#72EFDD>";
      case 'c':
        return "<reset><#FF3B47>";
      case 'd':
        return "<reset><#E0AAFF>";
      case 'e':
        return "<reset><#FFB703>";
      case 'f':
        return "<reset><#F4F1DE>";
      case 'k':
        return "<obfuscated>";
      case 'l':
        return "<bold>";
      case 'm':
        return "<strikethrough>";
      case 'n':
        return "<underlined>";
      case 'o':
        return "<italic>";
      case 'r':
      default:
        return "<reset>";
    }
  }

  @SuppressWarnings("unchecked")
  public static void applyToLanguageConfig(Main plugin) {
    Object languageManager = plugin.getLanguageManager();
    if(languageManager == null) {
      return;
    }
    try {
      Field langField = languageManager.getClass().getDeclaredField("languageConfig");
      langField.setAccessible(true);
      FileConfiguration languageConfig = (FileConfiguration) langField.get(languageManager);

      Field defField = languageManager.getClass().getDeclaredField("defaultLanguageConfig");
      defField.setAccessible(true);
      FileConfiguration defaultConfig = (FileConfiguration) defField.get(languageManager);

      if(languageConfig != null && defaultConfig != null) {
        for(String key : defaultConfig.getKeys(true)) {
          if(defaultConfig.isConfigurationSection(key)) {
            continue;
          }
          if(key.startsWith("Color.")
              || key.startsWith("Bossbar.")
              || key.startsWith("Scoreboard.")
              || key.startsWith("In-Game.Messages.Arena.Playing.Role.")
              || key.startsWith("In-Game.Messages.Arena.Chances.")
              || key.startsWith("In-Game.Messages.Game-End.Summary")
              || key.startsWith("In-Game.Messages.Arena.Task.")
              || key.startsWith("In-Game.Messages.Arena.Voting.")) {
            languageConfig.set(key, defaultConfig.get(key));
          }
        }
      }

      if(languageConfig != null) {
        formatConfigEntries(languageConfig);
      }
      if(defaultConfig != null) {
        formatConfigEntries(defaultConfig);
      }

      Object messageManager = plugin.getMessageManager();
      if(messageManager != null) {
        Field specialCharsField = messageManager.getClass().getDeclaredField("specialChars");
        specialCharsField.setAccessible(true);
        List<String> specialChars = (List<String>) specialCharsField.get(messageManager);
        if(specialChars != null) {
          specialChars.clear();
          specialChars.add("■");
          specialChars.add("▸");
        }
      }
    } catch(Exception ignored) {
    }
  }

  private static void formatConfigEntries(FileConfiguration config) {
    for(String key : config.getKeys(true)) {
      if(config.isConfigurationSection(key) || key.startsWith("Color.") || key.startsWith("Do-Not-Edit.")) {
        continue;
      }
      Object val = config.get(key);
      if(val instanceof String) {
        String str = (String) val;
        if(containsColorOrMiniMessage(str)) {
          config.set(key, formatConfigString(str));
        }
      } else if(val instanceof List<?>) {
        List<?> list = (List<?>) val;
        List<String> updated = new ArrayList<>(list.size());
        boolean changed = false;
        for(Object item : list) {
          if(item instanceof String) {
            String str = (String) item;
            if(containsColorOrMiniMessage(str)) {
              updated.add(formatConfigString(str));
              changed = true;
            } else {
              updated.add(str);
            }
          }
        }
        if(changed) {
          config.set(key, updated);
        }
      }
    }
  }

  private static boolean containsColorOrMiniMessage(String str) {
    return str.indexOf('&') != -1 || str.indexOf('§') != -1 || str.indexOf('<') != -1;
  }

  private static String formatConfigString(String str) {
    if(str.indexOf(';') != -1) {
      String[] parts = str.split(";", -1);
      StringBuilder sb = new StringBuilder(str.length() + 16);
      for(int i = 0; i < parts.length; i++) {
        if(i > 0) {
          sb.append(';');
        }
        if(i == 0 && parts[0].matches("\\d+,\\d+,\\d+")) {
          sb.append(parts[0]);
        } else {
          sb.append(color(parts[i]));
        }
      }
      return sb.toString();
    }
    return color(str);
  }
}
