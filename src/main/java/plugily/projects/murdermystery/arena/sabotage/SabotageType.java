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

import plugily.projects.murdermystery.arena.task.TaskType;

public enum SabotageType {

  LIGHTS(
    "ตัดระบบไฟฟ้า (Lights Out)",
    "[ไฟฟ้า]",
    false,
    0,
    TaskType.CHECK_CONTROL_PANEL,
    "ระบบไฟฟ้าถูกตัด! สับคัตเอาท์เพื่อเปิดไฟ"
  ),
  ENGINE_MELTDOWN(
    "วิกฤตเครื่องยนต์ระเบิด (Engine Meltdown)",
    "[เครื่องยนต์]",
    true,
    40,
    TaskType.CHECK_RADIATOR_WATER,
    "เครื่องยนต์จะระเบิด! รีบไประบายความร้อน"
  ),
  COMMS(
    "รบกวนสัญญาณเรดาร์ (Comms Jammed)",
    "[เรดาร์]",
    false,
    0,
    TaskType.CHECK_STEERING_FLUID,
    "สัญญาณเรดาร์ถูกตัด! ปรับคลื่นวิทยุกู้ระบบ"
  );

  private final String displayName;
  private final String icon;
  private final boolean critical;
  private final int countdownSeconds;
  private final TaskType fallbackStationType;
  private final String fixInstruction;

  SabotageType(String displayName, String icon, boolean critical, int countdownSeconds, TaskType fallbackStationType, String fixInstruction) {
    this.displayName = displayName;
    this.icon = icon;
    this.critical = critical;
    this.countdownSeconds = countdownSeconds;
    this.fallbackStationType = fallbackStationType;
    this.fixInstruction = fixInstruction;
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

  public TaskType getFallbackStationType() {
    return fallbackStationType;
  }

  public String getFixInstruction() {
    return fixInstruction;
  }
}
