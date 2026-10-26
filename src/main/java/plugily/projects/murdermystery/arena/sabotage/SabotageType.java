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

package plugily.projects.murdermystery.arena.sabotage;

public enum SabotageType {

  LIGHTS(
    "lights",
    "ตัดระบบไฟฟ้า (Lights Out)",
    "[ไฟฟ้า]",
    false,
    0,
    "ระบบไฟฟ้าถูกตัด! สับคัตเอาท์เพื่อเปิดไฟ"
  ),
  ENGINE_MELTDOWN(
    "engine",
    "วิกฤตเครื่องยนต์ระเบิด (Engine Meltdown)",
    "[เครื่องยนต์]",
    true,
    40,
    "เครื่องยนต์จะระเบิด! รีบไประบายความร้อน"
  ),
  COMMS(
    "comms",
    "รบกวนสัญญาณเรดาร์ (Comms Jammed)",
    "[เรดาร์]",
    false,
    0,
    "สัญญาณเรดาร์ถูกตัด! ปรับคลื่นวิทยุกู้ระบบ"
  );

  private final String shortId;
  private final String displayName;
  private final String icon;
  private final boolean critical;
  private final int countdownSeconds;
  private final String fixInstruction;

  SabotageType(String shortId, String displayName, String icon, boolean critical, int countdownSeconds, String fixInstruction) {
    this.shortId = shortId;
    this.displayName = displayName;
    this.icon = icon;
    this.critical = critical;
    this.countdownSeconds = countdownSeconds;
    this.fixInstruction = fixInstruction;
  }

  public String getShortId() {
    return shortId;
  }

  public String getDisplayName() {
    return displayName;
  }

  public String getIcon() {
    return icon;
  }

  public boolean isCritical() {
    return critical;
  }

  public int getCountdownSeconds() {
    return countdownSeconds;
  }

  public String getFixInstruction() {
    return fixInstruction;
  }

  public static SabotageType fromInput(String input) {
    if(input == null) {
      return null;
    }
    String normalized = input.trim().toLowerCase();
    for(SabotageType type : values()) {
      if(type.name().equalsIgnoreCase(normalized) || type.shortId.equalsIgnoreCase(normalized)) {
        return type;
      }
    }
    switch(normalized) {
      case "1":
      case "light":
      case "power":
      case "electrical":
        return LIGHTS;
      case "2":
      case "meltdown":
      case "reactor":
        return ENGINE_MELTDOWN;
      case "3":
      case "radar":
      case "radio":
      case "communication":
        return COMMS;
      default:
        return null;
    }
  }
}
