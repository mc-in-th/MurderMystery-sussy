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
  private static final Pattern AMPERSAND_HEX_PATTERN = Pattern.compile("(?i)(?<![</])&?#([0-9a-f]{6})");
  private static final Pattern LEGACY_CODE_PATTERN = Pattern.compile("(?i)[§&]([0-9a-fk-or])");
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
      return LEGACY_SERIALIZER.serialize(component);
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
      bukkitHexMatcher.appendReplacement(hexBuffer, "<reset><#" + hex.toString().toUpperCase(Locale.ROOT) + ">");
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

      if(languageConfig != null) {
        upgradeConfigPalette(languageConfig);
      }
      if(defaultConfig != null) {
        upgradeConfigPalette(defaultConfig);
      }
    } catch(Exception ignored) {
    }
  }

  private static void upgradeConfigPalette(FileConfiguration config) {
    config.set("Color.Placeholder.Value", "&#FFB703");
    config.set("Color.Placeholder.Number", "&#FF7518");
    config.set("Color.Placeholder.Player", "&#C77DFF");
    config.set("Color.Placeholder.Other", "&#FFB703");
    config.set("Color.Chat.Issue", "&#FF3B47");
    config.set("Color.Chat.Messages", "&#B8B0C8");
    config.set("Color.Chat.Special-Char.Contains", "■,▸");
    config.set("Color.Chat.Special-Char.Before", "&#5A4E7C");

    config.set("Scoreboard.Title", "<gradient:#FF5400:#FFB703><bold>MurderMystery</bold></gradient>");
    config.set("Scoreboard.Roles.Detective", "<gradient:#00B4D8:#72EFDD>นักสืบ</gradient>");
    config.set("Scoreboard.Roles.Murderer", "<gradient:#D90429:#FF5400>ฆาตกร</gradient>");
    config.set("Scoreboard.Roles.Innocent", "<gradient:#FF7518:#FFB703>ผู้บริสุทธิ์</gradient>");
    config.set("Scoreboard.Roles.Dead", "<#5A4E7C>เสียชีวิต");

    config.set("In-Game.Messages.Arena.Playing.Role.Murderer",
        "5,20,5;<gradient:#D90429:#FF5400><bold>บทบาท | ฆาตกร</bold></gradient>;<#B8B0C8>กำจัดผู้โดยสารทุกคนและใช้ท่อระบายน้ำแฝงตัว!");
    config.set("In-Game.Messages.Arena.Playing.Role.Detective",
        "5,20,5;<gradient:#00B4D8:#72EFDD><bold>บทบาท | นักสืบ</bold></gradient>;<#B8B0C8>สืบหาความจริงและเปิดโปงฆาตกร!");
    config.set("In-Game.Messages.Arena.Playing.Role.Innocent",
        "5,20,5;<gradient:#FF7518:#FFB703><bold>บทบาท | ผู้บริสุทธิ์</bold></gradient>;<#B8B0C8>ร่วมมือกันซ่อมรถบัสและเปิดโปงฆาตกร!");

    config.set("In-Game.Messages.Arena.Task.Bossbar",
        "<gradient:#FF5400:#FFB703><bold>ซ่อมแซมรถบัส:</bold></gradient> <#6EEB83>%number%<#5A4E7C>/<#6EEB83>%value% <#FFB703>(Bus Repair)");
    config.set("In-Game.Messages.Arena.Task.Completed",
        "<#6EEB83>สำเร็จภารกิจ: <#FFB703>%value%<#6EEB83>!");
    config.set("In-Game.Messages.Arena.Task.All-Completed",
        "<gradient:#38B000:#9EF01A><bold>ซ่อมแซมรถบัสสำเร็จแล้ว! ผู้โดยสารหลบหนีออกจากเมืองได้และคว้าชัยชนะ!</bold></gradient>");
    config.set("In-Game.Messages.Arena.Task.Win.Title",
        "<gradient:#38B000:#9EF01A><bold>รถบัสซ่อมแซมสำเร็จ!</bold></gradient>");
    config.set("In-Game.Messages.Arena.Task.Win.Subtitle",
        "<gradient:#FF7518:#FFB703>ผู้โดยสารหลบหนีออกจากเมืองได้สำเร็จ!</gradient>");

    config.set("In-Game.Messages.Arena.Voting.Cooldown",
        "%color_chat_issue%การเรียกประชุมฉุกเฉินยังติดคูลดาวน์อยู่ กรุณารอสักครู่!");
    config.set("In-Game.Messages.Arena.Voting.Body-Reported",
        "<gradient:#D90429:#FF5400><bold>[รายงานศพ]</bold></gradient> <#C77DFF>%player% <#FF3B47>พบศพผู้เสียชีวิตและเรียกประชุมด่วนรอบกองไฟ!");
    config.set("In-Game.Messages.Arena.Voting.Discussion-Start",
        "<gradient:#FF5400:#FFB703><bold>[ประชุมรอบกองไฟ]</bold></gradient> <#C77DFF>%player% <#FFB703>เรียกประชุมด่วน! เริ่มเวลาปรึกษาหารือ (<#FF3B47>%number% วินาที<#FFB703>)");
    config.set("In-Game.Messages.Arena.Voting.Vote-Now",
        "<gradient:#9D4EDD:#FF7518><bold>[เปิดโหวต]</bold></gradient> <#FFB703>คลิกเลือกผู้ต้องสงสัยเพื่อโหวตคัดออก (<#FF3B47>%number% วินาที<#FFB703>)");
    config.set("In-Game.Messages.Arena.Voting.Discussion-Timer",
        "<gradient:#FF5400:#FFB703>เวลาปรึกษาที่เหลือ:</gradient> <#FF3B47>%number% วินาที");
    config.set("In-Game.Messages.Arena.Voting.Vote-Timer",
        "<gradient:#D90429:#FF5400>เวลาโหวตที่เหลือ:</gradient> <#FFB703>%number% วินาที");
    config.set("In-Game.Messages.Arena.Voting.Already-Voted",
        "%color_chat_issue%คุณได้ยืนยันการลงคะแนนโหวตไปแล้ว!");
    config.set("In-Game.Messages.Arena.Voting.Player-Voted",
        "<#C77DFF>%player% <#B8B0C8>ได้ยืนยันการลงคะแนนโหวตแล้ว");
    config.set("In-Game.Messages.Arena.Voting.No-Eject",
        "<gradient:#FF5400:#FFB703>ไม่มีใครถูกโหวตคัดออกในรอบนี้</gradient> <#B8B0C8>(คะแนนโหวตเสมอ หรือเสียงส่วนใหญ่เลือกข้าม)");
    config.set("In-Game.Messages.Arena.Voting.No-Eject-Title",
        "<gradient:#FF5400:#FFB703><bold>ไม่มีใครถูกโหวตออก</bold></gradient>");
    config.set("In-Game.Messages.Arena.Voting.No-Eject-Subtitle",
        "<#B8B0C8>คะแนนโหวตเสมอ หรือเสียงส่วนใหญ่เลือกข้าม");
    config.set("In-Game.Messages.Arena.Voting.Ejected",
        "<#C77DFF>%player% <#FF3B47>ถูกโหวตคัดออก %value%");
    config.set("In-Game.Messages.Arena.Voting.Was-Impostor",
        "<gradient:#38B000:#9EF01A><bold>(เขาคือฆาตกร!)</bold></gradient>");
    config.set("In-Game.Messages.Arena.Voting.Was-Not-Impostor",
        "<gradient:#D90429:#FF5400><bold>(เขาไม่ใช่ฆาตกร...)</bold></gradient>");

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
