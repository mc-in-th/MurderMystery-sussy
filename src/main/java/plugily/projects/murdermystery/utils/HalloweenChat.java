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
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.ShadowColor;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.title.Title;
import net.md_5.bungee.api.ChatMessageType;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.meta.ItemMeta;
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
  public static final String DARK_SHADOW_TAG = "<shadow:#000000FF>";

  public static final String GRADIENT_PUMPKIN = "<gradient:#FF5400:#FFB703>";
  public static final String GRADIENT_BLOOD = "<gradient:#D90429:#FF5400>";
  public static final String GRADIENT_WITCH = "<gradient:#9D4EDD:#FF7518>";
  public static final String GRADIENT_GHOST = "<gradient:#7B2CBF:#C77DFF>";
  public static final String GRADIENT_SLIME = "<gradient:#38B000:#9EF01A>";
  public static final String GRADIENT_SOUL = "<gradient:#00B4D8:#72EFDD>";

  private static final ShadowColor DARK_TEXT_SHADOW = ShadowColor.shadowColor(0xFF000000);
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
      "(?i)<(?!([!/]?(gradient|color|shadow|rainbow|transition|bold|b|italic|i|underlined|u|strikethrough|st|obfuscated|obf|reset|newline|hover|click)(:[^>]*)?>|/?#[0-9a-f]{6}>))"
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
      String cleaned = THAI_COMBINING_PREFIX_PATTERN.matcher(serialized).replaceAll("");
      if(!cleaned.isEmpty() && !cleaned.startsWith("§r")) {
        return "§r" + cleaned;
      }
      return cleaned;
    } catch(Exception ex) {
      return ChatColor.translateAlternateColorCodes('&', line);
    }
  }

  public static Component component(String input) {
    if(input == null || input.isEmpty()) {
      return Component.empty();
    }
    if(input.indexOf('\n') == -1) {
      return componentLine(input, false);
    }
    String[] lines = input.split("\n", -1);
    TextComponent.Builder builder = Component.text();
    for(int i = 0; i < lines.length; i++) {
      if(i > 0) {
        builder.append(Component.newline());
      }
      builder.append(componentLine(lines[i], false));
    }
    return builder.build();
  }

  public static Component itemComponent(String input) {
    if(input == null || input.isEmpty()) {
      return Component.empty().decoration(TextDecoration.ITALIC, false);
    }
    return componentLine(input, true);
  }

  public static List<Component> itemLore(List<String> lines) {
    if(lines == null) {
      return new ArrayList<>();
    }
    List<Component> result = new ArrayList<>(lines.size());
    for(String line : lines) {
      result.add(itemComponent(line));
    }
    return result;
  }

  public static List<Component> itemLore(String... lines) {
    if(lines == null) {
      return new ArrayList<>();
    }
    List<Component> result = new ArrayList<>(lines.length);
    for(String line : lines) {
      result.add(itemComponent(line));
    }
    return result;
  }

  public static void setItemName(ItemMeta meta, String displayName) {
    if(meta == null) {
      return;
    }
    try {
      meta.displayName(itemComponent(displayName));
    } catch(Throwable t) {
      meta.setDisplayName(color(displayName));
    }
  }

  public static void setItemLore(ItemMeta meta, List<String> lines) {
    if(meta == null || lines == null) {
      return;
    }
    try {
      meta.lore(itemLore(lines));
    } catch(Throwable t) {
      meta.setLore(colorList(lines));
    }
  }

  public static void setItemLore(ItemMeta meta, String... lines) {
    if(meta == null || lines == null) {
      return;
    }
    try {
      meta.lore(itemLore(lines));
    } catch(Throwable t) {
      meta.setLore(colorLines(lines));
    }
  }

  private static Component componentLine(String line, boolean defaultNoItalic) {
    if(line == null || line.isEmpty()) {
      return defaultNoItalic ? Component.empty().decoration(TextDecoration.ITALIC, false) : Component.empty();
    }
    String converted = toMiniMessageFormat(line);
    try {
      Component parsed = MINI_MESSAGE.deserialize(converted);
      TextComponent.Builder out = Component.text();
      if(defaultNoItalic) {
        out.decoration(TextDecoration.ITALIC, false);
      }
      collectStyledSegments(parsed, Style.empty(), defaultNoItalic, out);
      return out.build();
    } catch(Exception ex) {
      Component fallback = LEGACY_SERIALIZER.deserialize(ChatColor.translateAlternateColorCodes('&', line));
      return defaultNoItalic ? fallback.decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE) : fallback;
    }
  }

  private static void collectStyledSegments(Component node, Style parentStyle, boolean defaultNoItalic, TextComponent.Builder out) {
    Style merged = node.style().merge(parentStyle, Style.Merge.Strategy.IF_ABSENT_ON_TARGET);
    Style emitStyle = merged;
    if(emitStyle.shadowColor() == null && isDarkColor(emitStyle.color())) {
      emitStyle = emitStyle.shadowColor(DARK_TEXT_SHADOW);
    }
    if(defaultNoItalic && emitStyle.decoration(TextDecoration.ITALIC) == TextDecoration.State.NOT_SET) {
      emitStyle = emitStyle.decoration(TextDecoration.ITALIC, TextDecoration.State.FALSE);
    }
    if(node instanceof TextComponent) {
      String content = ((TextComponent) node).content();
      if(!content.isEmpty()) {
        out.append(Component.text(content, emitStyle));
      }
    } else {
      out.append(node.children(Collections.emptyList()).style(emitStyle));
    }
    for(Component child : node.children()) {
      collectStyledSegments(child, merged, defaultNoItalic, out);
    }
  }

  public static boolean isDarkColor(TextColor color) {
    if(color == null) {
      return false;
    }
    int r = (color.value() >> 16) & 0xFF;
    int g = (color.value() >> 8) & 0xFF;
    int b = color.value() & 0xFF;
    return Math.max(r, Math.max(g, b)) <= 160;
  }

  public static void send(CommandSender sender, String message) {
    if(sender == null || message == null || message.isEmpty()) {
      return;
    }
    try {
      sender.sendMessage(component(message));
    } catch(Throwable t) {
      sender.sendMessage(color(message));
    }
  }

  public static void sendActionBar(Player player, String message) {
    if(player == null) {
      return;
    }
    try {
      player.sendActionBar(message == null ? Component.empty() : component(message));
    } catch(Throwable t) {
      String colored = message == null ? "" : color(message);
      player.spigot().sendMessage(ChatMessageType.ACTION_BAR, net.md_5.bungee.api.chat.TextComponent.fromLegacyText(colored));
    }
  }

  public static void sendTitles(Player player, String title, String subtitle, int fadeInTicks, int stayTicks, int fadeOutTicks) {
    if(player == null) {
      return;
    }
    try {
      Title.Times times = Title.Times.times(
          Duration.ofMillis(fadeInTicks * 50L),
          Duration.ofMillis(stayTicks * 50L),
          Duration.ofMillis(fadeOutTicks * 50L)
      );
      Title advTitle = Title.title(
          title == null ? Component.empty() : component(title),
          subtitle == null ? Component.empty() : component(subtitle),
          times
      );
      player.showTitle(advTitle);
    } catch(Throwable t) {
      String coloredTitle = title == null ? "" : color(title);
      String coloredSub = subtitle == null ? "" : color(subtitle);
      player.sendTitle(coloredTitle, coloredSub, fadeInTicks, stayTicks, fadeOutTicks);
    }
  }

  public static void sendSubTitle(Player player, String subtitle, int fadeInTicks, int stayTicks, int fadeOutTicks) {
    sendTitles(player, "", subtitle, fadeInTicks, stayTicks, fadeOutTicks);
  }

  public static void sendTitleEntry(Player player, String rawEntry) {
    if(player == null || rawEntry == null || rawEntry.isEmpty()) {
      return;
    }
    String[] parts = rawEntry.split(";", 3);
    int fadeIn = 10;
    int stay = 40;
    int fadeOut = 10;
    String title;
    String subtitle = "";
    if(parts.length >= 2 && parts[0].matches("\\s*\\d+\\s*,\\s*\\d+\\s*,\\s*\\d+\\s*")) {
      String[] nums = parts[0].split(",", 3);
      fadeIn = Integer.parseInt(nums[0].trim());
      stay = Integer.parseInt(nums[1].trim());
      fadeOut = Integer.parseInt(nums[2].trim());
      title = parts[1];
      subtitle = parts.length >= 3 ? parts[2] : "";
    } else {
      title = rawEntry;
    }
    sendTitles(player, title, subtitle, fadeIn, stay, fadeOut);
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
      int start = bukkitHexMatcher.start();
      int end = bukkitHexMatcher.end();
      if(start > 0 && input.charAt(start - 1) == ':') {
        bukkitHexMatcher.appendReplacement(hexBuffer, "#" + hexUpper);
      } else if(start > 0 && input.charAt(start - 1) == '<' && end < input.length() && input.charAt(end) == '>') {
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
        return "<reset><shadow:#000000FF><#1A1423>";
      case '1':
        return "<reset><shadow:#000000FF><#5A189A>";
      case '2':
        return "<reset><shadow:#000000FF><#2D8A4E>";
      case '3':
        return "<reset><#00B4D8>";
      case '4':
        return "<reset><shadow:#000000FF><#9E001A>";
      case '5':
        return "<reset><#9D4EDD>";
      case '6':
        return "<reset><#FF7518>";
      case '7':
        return "<reset><#B8B0C8>";
      case '8':
        return "<reset><shadow:#000000FF><#5A4E7C>";
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
          boolean isMissing = !languageConfig.isSet(key);
          Object existing = isMissing ? null : languageConfig.get(key);
          boolean hasStaleGradient = false;
          if(existing instanceof String) {
            String existingStr = (String) existing;
            hasStaleGradient = existingStr.contains("<gradient:")
                || existingStr.contains("MurderMystery")
                || existingStr.contains("%plugin_name%")
                || (existingStr.contains("<#5A4E7C>") && !existingStr.contains("<shadow:"));
          } else if(existing instanceof List<?>) {
            for(Object item : (List<?>) existing) {
              if(item instanceof String) {
                String itemStr = (String) item;
                if(itemStr.contains("<gradient:")
                    || itemStr.contains("MurderMystery")
                    || itemStr.contains("%plugin_name%")
                    || (itemStr.contains("<#5A4E7C>") && !itemStr.contains("<shadow:"))) {
                  hasStaleGradient = true;
                  break;
                }
              }
            }
          }
          if(isMissing || hasStaleGradient) {
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
