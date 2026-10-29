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
import plugily.projects.murdermystery.utils.HalloweenChat;

public enum TaskType {

  FIND_DIESEL_FUEL("หาน้ำมันดีเซล", "IN_GAME_MESSAGES_ARENA_TASK_FIND_DIESEL_FUEL", "[น้ำมัน]", "diesel", false),
  CHECK_CONTROL_PANEL("ส่งสัญญาณวิทยุขอรถช่าง", "IN_GAME_MESSAGES_ARENA_TASK_CHECK_CONTROL_PANEL", "[วิทยุ]", "radio", false),
  REPAIR_ENGINE_WIRING("ซ่อมสายไฟเครื่องยนต์", "IN_GAME_MESSAGES_ARENA_TASK_REPAIR_ENGINE_WIRING", "[สายไฟ]", "wiring", false),
  CHECK_RADIATOR_WATER("ตักน้ำหล่อเย็นหม้อน้ำ", "IN_GAME_MESSAGES_ARENA_TASK_CHECK_RADIATOR_WATER", "[น้ำหล่อเย็น]", "water", true),
  CHECK_ENGINE_OIL("เช็คน้ำมันเครื่อง", "IN_GAME_MESSAGES_ARENA_TASK_CHECK_ENGINE_OIL", "[น้ำมันเครื่อง]", "oil", false),
  CHECK_TIRE_PRESSURE("เช็คลมยาง", "IN_GAME_MESSAGES_ARENA_TASK_CHECK_TIRE_PRESSURE", "[ลมยาง]", "tire", true),
  CHECK_STEERING_FLUID("ถ่ายน้ำมันไฮดรอลิกสำรอง", "IN_GAME_MESSAGES_ARENA_TASK_CHECK_STEERING_FLUID", "[ไฮดรอลิก]", "hydraulic", false),
  CLEAN_WINDSHIELD("เช็ดคราบโคลนกระจกหน้ารถ", "IN_GAME_MESSAGES_ARENA_TASK_CLEAN_WINDSHIELD", "[กระจกหน้า]", "windshield", false),
  UNLOCK_MANIFOLDS("ตรวจสอบสมุดแผนที่เส้นทางสำรอง", "IN_GAME_MESSAGES_ARENA_TASK_UNLOCK_MANIFOLDS", "[แผนที่]", "route", false),
  CHARGE_BATTERY("พ่วงชาร์จแบตเตอรี่สำรอง", "IN_GAME_MESSAGES_ARENA_TASK_CHARGE_BATTERY", "[แบตเตอรี่]", "battery", false),
  CLEAN_AIR_FILTER("ค้นหากล่องฟิวส์และอะไหล่", "IN_GAME_MESSAGES_ARENA_TASK_CLEAN_AIR_FILTER", "[อะไหล่]", "parts", false);

  private static final TaskType[] VALUES = values();

  private final String defaultName;
  private final String messageKey;
  private final String icon;
  private final String shortId;
  private final boolean visual;

  TaskType(String defaultName, String messageKey, String icon, String shortId, boolean visual) {
    this.defaultName = defaultName;
    this.messageKey = messageKey;
    this.icon = icon;
    this.shortId = shortId;
    this.visual = visual;
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

  public boolean isVisual() {
    return visual;
  }

  public String getDisplayName() {
    try {
      String msg = new MessageBuilder(messageKey).asKey().build();
      if(msg != null && !msg.isEmpty() && !msg.contains("ERR_MESSAGE_")) {
        String plain = HalloweenChat.strip(msg).trim();
        if(!plain.isEmpty()) {
          return plain;
        }
      }
    } catch(Exception ignored) {
    }
    return defaultName;
  }

  public String getHologramTitle() {
    if(visual) {
      return "<#F4F1DE>" + getDisplayName() + "\n<#72EFDD>ยืนยันตัวตนได้";
    }
    return "<#F4F1DE>" + getDisplayName();
  }

  public static TaskType fromInput(String input) {
    if(input == null || input.trim().isEmpty()) {
      return null;
    }
    String clean = input.trim().toUpperCase().replace("-", "_").replace(" ", "_");

    try {
      int idx = Integer.parseInt(clean);
      if(idx >= 1 && idx <= VALUES.length) {
        return VALUES[idx - 1];
      }
    } catch(NumberFormatException ignored) {
    }

    for(TaskType t : VALUES) {
      if(t.name().equalsIgnoreCase(clean) || t.getShortId().equalsIgnoreCase(clean)) {
        return t;
      }
    }

    if(clean.contains("DIESEL") || clean.contains("FUEL") || clean.contains("น้ำมันดีเซล")) {
      return FIND_DIESEL_FUEL;
    }
    if(clean.contains("RADIO") || clean.contains("SIGNAL") || clean.contains("TOW")
      || clean.contains("CONTROL") || clean.contains("PANEL")
      || clean.contains("วิทยุ") || clean.contains("สัญญาณ") || clean.contains("แผงควบคุม")) {
      return CHECK_CONTROL_PANEL;
    }
    if(clean.contains("WIRE") || clean.contains("WIRING") || clean.contains("สายไฟ")) {
      return REPAIR_ENGINE_WIRING;
    }
    if(clean.contains("RADIATOR") || clean.contains("WATER") || clean.contains("COOLANT")
      || clean.contains("น้ำหล่อเย็น") || clean.contains("ตักน้ำ") || clean.contains("หม้อน้ำ")) {
      return CHECK_RADIATOR_WATER;
    }
    if(clean.contains("HYDRAULIC") || clean.contains("STEERING") || clean.contains("FLUID")
      || clean.contains("ไฮดรอลิก") || clean.contains("พวงมาลัย")) {
      return CHECK_STEERING_FLUID;
    }
    if(clean.contains("ENGINE_OIL") || clean.contains("OIL") || clean.contains("น้ำมันเครื่อง")) {
      return CHECK_ENGINE_OIL;
    }
    if(clean.contains("TIRE") || clean.contains("PRESSURE") || clean.contains("ลมยาง")) {
      return CHECK_TIRE_PRESSURE;
    }
    if(clean.contains("WINDSHIELD") || clean.contains("WIPE") || clean.contains("GLASS") || clean.contains("กระจก") || clean.contains("โคลน")) {
      return CLEAN_WINDSHIELD;
    }
    if(clean.contains("ROUTE") || clean.contains("MAP") || clean.contains("MANIFOLD") || clean.contains("UNLOCK")
      || clean.contains("แผนที่") || clean.contains("เส้นทาง") || clean.contains("ปลดล็อก")) {
      return UNLOCK_MANIFOLDS;
    }
    if(clean.contains("BATTERY") || clean.contains("CHARGE") || clean.contains("GENERATOR")
      || clean.contains("แบตเตอรี่") || clean.contains("พ่วง")) {
      return CHARGE_BATTERY;
    }
    if(clean.contains("PARTS") || clean.contains("SPARE") || clean.contains("FUSE")
      || clean.contains("FILTER") || clean.contains("AIR") || clean.contains("LEAF")
      || clean.contains("อะไหล่") || clean.contains("ฟิวส์") || clean.contains("ไส้กรอง")) {
      return CLEAN_AIR_FILTER;
    }

    return null;
  }

  public static TaskType[] getValues() {
    return VALUES;
  }
}
