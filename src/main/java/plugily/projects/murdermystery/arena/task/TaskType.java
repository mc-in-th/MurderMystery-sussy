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

package plugily.projects.murdermystery.arena.task;

import plugily.projects.minigamesbox.classic.handlers.language.MessageBuilder;

public enum TaskType {

  FIND_DIESEL_FUEL("หาน้ำมันดีเซล", "IN_GAME_MESSAGES_ARENA_TASK_FIND_DIESEL_FUEL", "[น้ำมัน]", "diesel"),
  CHECK_CONTROL_PANEL("ตรวจสอบแผงควบคุมรถบัส", "IN_GAME_MESSAGES_ARENA_TASK_CHECK_CONTROL_PANEL", "[แผงไฟ]", "panel"),
  REPAIR_ENGINE_WIRING("ซ่อมสายไฟเครื่องยนต์", "IN_GAME_MESSAGES_ARENA_TASK_REPAIR_ENGINE_WIRING", "[สายไฟ]", "wiring"),
  CHECK_RADIATOR_WATER("เช็คระดับน้ำในหม้อน้ำ", "IN_GAME_MESSAGES_ARENA_TASK_CHECK_RADIATOR_WATER", "[หม้อน้ำ]", "radiator"),
  CHECK_ENGINE_OIL("เช็คน้ำมันเครื่อง", "IN_GAME_MESSAGES_ARENA_TASK_CHECK_ENGINE_OIL", "[น้ำมันเครื่อง]", "oil"),
  CHECK_TIRE_PRESSURE("เช็คลมยาง", "IN_GAME_MESSAGES_ARENA_TASK_CHECK_TIRE_PRESSURE", "[ลมยาง]", "tire"),
  CHECK_STEERING_FLUID("เช็คน้ำมันพวงมาลัย", "IN_GAME_MESSAGES_ARENA_TASK_CHECK_STEERING_FLUID", "[พวงมาลัย]", "steering");

  private final String defaultName;
  private final String messageKey;
  private final String icon;
  private final String shortId;

  TaskType(String defaultName, String messageKey, String icon, String shortId) {
    this.defaultName = defaultName;
    this.messageKey = messageKey;
    this.icon = icon;
    this.shortId = shortId;
  }

  public String getDefaultName() {
    return defaultName;
  }

  public String getMessageKey() {
    return messageKey;
  }

  public String getIcon() {
    return icon;
  }

  public String getShortId() {
    return shortId;
  }

  public String getDisplayName() {
    try {
      String msg = new MessageBuilder(messageKey).asKey().build();
      if(msg != null && !msg.isEmpty() && !msg.contains("ERR_MESSAGE_")) {
        return msg;
      }
    } catch(Exception ignored) {
    }
    return defaultName;
  }

  public String getHologramTitle() {
    return "&6&l[ " + getDisplayName() + " ]";
  }

  public static TaskType fromInput(String input) {
    if(input == null || input.trim().isEmpty()) {
      return null;
    }
    String clean = input.trim().toUpperCase().replace("-", "_").replace(" ", "_");

    if(clean.equals("1")) {
      return FIND_DIESEL_FUEL;
    }
    if(clean.equals("2")) {
      return CHECK_CONTROL_PANEL;
    }
    if(clean.equals("3")) {
      return REPAIR_ENGINE_WIRING;
    }
    if(clean.equals("4")) {
      return CHECK_RADIATOR_WATER;
    }
    if(clean.equals("5")) {
      return CHECK_ENGINE_OIL;
    }
    if(clean.equals("6")) {
      return CHECK_TIRE_PRESSURE;
    }
    if(clean.equals("7")) {
      return CHECK_STEERING_FLUID;
    }

    for(TaskType t : values()) {
      if(t.name().equalsIgnoreCase(clean) || t.getShortId().equalsIgnoreCase(clean)) {
        return t;
      }
    }

    if(clean.contains("DIESEL") || clean.contains("FUEL") || clean.contains("น้ำมันดีเซล")) {
      return FIND_DIESEL_FUEL;
    }
    if(clean.contains("CONTROL") || clean.contains("PANEL") || clean.contains("แผงควบคุม")) {
      return CHECK_CONTROL_PANEL;
    }
    if(clean.contains("WIRE") || clean.contains("WIRING") || clean.contains("สายไฟ")) {
      return REPAIR_ENGINE_WIRING;
    }
    if(clean.contains("RADIATOR") || clean.contains("WATER") || clean.contains("หม้อน้ำ")) {
      return CHECK_RADIATOR_WATER;
    }
    if(clean.contains("ENGINE_OIL") || clean.contains("OIL") || clean.contains("น้ำมันเครื่อง")) {
      return CHECK_ENGINE_OIL;
    }
    if(clean.contains("TIRE") || clean.contains("PRESSURE") || clean.contains("ลมยาง")) {
      return CHECK_TIRE_PRESSURE;
    }
    if(clean.contains("STEERING") || clean.contains("FLUID") || clean.contains("พวงมาลัย")) {
      return CHECK_STEERING_FLUID;
    }

    return null;
  }

  private static final TaskType[] VALUES = values();

  public static TaskType[] getValues() {
    return VALUES;
  }
}
