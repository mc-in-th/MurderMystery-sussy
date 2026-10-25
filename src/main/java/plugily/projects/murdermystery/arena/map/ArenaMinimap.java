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

package plugily.projects.murdermystery.arena.map;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.MapMeta;
import org.bukkit.map.MapCanvas;
import org.bukkit.map.MapCursor;
import org.bukkit.map.MapCursorCollection;
import org.bukkit.map.MapRenderer;
import org.bukkit.map.MapView;
import plugily.projects.minigamesbox.api.arena.IArenaState;
import plugily.projects.minigamesbox.api.user.IUser;
import plugily.projects.murdermystery.arena.Arena;
import plugily.projects.murdermystery.arena.role.Role;
import plugily.projects.murdermystery.arena.special.SpecialBlock;
import plugily.projects.murdermystery.arena.task.GameTask;
import plugily.projects.murdermystery.utils.HalloweenChat;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ArenaMinimap {

  private final Arena arena;
  private MapView mapView;
  private MinimapRenderer renderer;
  private Bounds cachedBounds;

  public ArenaMinimap(Arena arena) {
    this.arena = arena;
  }

  public void initMap() {
    if(mapView != null) {
      return;
    }
    try {
      World world = null;
      if(!arena.getPlayerSpawnPoints().isEmpty()) {
        world = arena.getPlayerSpawnPoints().get(0).getWorld();
      }
      if(world == null && !arena.getAllTaskLocations().isEmpty()) {
        world = arena.getAllTaskLocations().get(0).getWorld();
      }
      if(world == null && !Bukkit.getWorlds().isEmpty()) {
        world = Bukkit.getWorlds().get(0);
      }
      if(world == null) {
        return;
      }

      mapView = Bukkit.createMap(world);
      for(MapRenderer r : new ArrayList<>(mapView.getRenderers())) {
        mapView.removeRenderer(r);
      }
      renderer = new MinimapRenderer(arena);
      mapView.addRenderer(renderer);
      mapView.setTrackingPosition(false);
      mapView.setUnlimitedTracking(false);
      cachedBounds = calculateBounds();
    } catch(Throwable ignored) {
    }
  }

  public Bounds calculateBounds() {
    List<Location> allLocs = new ArrayList<>();
    allLocs.addAll(arena.getAllTaskLocations());
    allLocs.addAll(arena.getAllSabotageLocations());
    if(arena.getCampfireLocation() != null) {
      allLocs.add(arena.getCampfireLocation());
    }
    allLocs.addAll(arena.getPlayerSpawnPoints());
    for(SpecialBlock sb : arena.getSpecialBlocks()) {
      if(sb.getSpecialBlockType() == SpecialBlock.SpecialBlockType.RAPID_TELEPORTATION) {
        allLocs.add(sb.getLocation());
      }
    }
    if(allLocs.isEmpty()) {
      return new Bounds(-50, 50, -50, 50);
    }
    double minX = Double.MAX_VALUE;
    double maxX = -Double.MAX_VALUE;
    double minZ = Double.MAX_VALUE;
    double maxZ = -Double.MAX_VALUE;
    for(Location loc : allLocs) {
      if(loc == null) {
        continue;
      }
      minX = Math.min(minX, loc.getX());
      maxX = Math.max(maxX, loc.getX());
      minZ = Math.min(minZ, loc.getZ());
      maxZ = Math.max(maxZ, loc.getZ());
    }
    return new Bounds(minX, maxX, minZ, maxZ);
  }

  public ItemStack createMapItem(Player player) {
    if(mapView == null) {
      initMap();
    }
    ItemStack item = new ItemStack(Material.FILLED_MAP);
    MapMeta meta = (MapMeta) item.getItemMeta();
    if(meta != null && mapView != null) {
      meta.setMapView(mapView);
      boolean isMurderer = false;
      if(player != null) {
        IUser user = arena.getPlugin().getUserManager().getUser(player);
        isMurderer = Role.isRole(Role.MURDERER, user, arena) || arena.getMurdererList().contains(player);
      }

      if(isMurderer) {
        HalloweenChat.setItemName(meta, "<shadow:#000000FF><#5A4E7C>[</shadow><#FF3B47>Impostor Radar<shadow:#000000FF><#5A4E7C>]</shadow> <#F4F1DE>เรดาร์ฆาตกร");
        HalloweenChat.setItemLore(meta,
          "<#B8B0C8>แสดงตำแหน่งท่อและจุดทำภารกิจทั้งหมด",
          "<#FF3B47>[V] <#F4F1DE>ท่อระบายน้ำ",
          "<#FFB703>[!] <#F4F1DE>จุดภารกิจของผู้โดยสาร",
          "<#72EFDD>[^] <#F4F1DE>ตำแหน่งและทิศทางของคุณ",
          "<shadow:#000000FF><#5A4E7C>(</shadow><#B8B0C8>ถือไว้ในมือซ้ายเพื่อดูแผนที่<shadow:#000000FF><#5A4E7C>)</shadow>"
        );
      } else {
        HalloweenChat.setItemName(meta, "<shadow:#000000FF><#5A4E7C>[</shadow><#FF7518>Task Minimap<shadow:#000000FF><#5A4E7C>]</shadow> <#F4F1DE>แผนที่ภารกิจ");
        HalloweenChat.setItemLore(meta,
          "<#B8B0C8>แสดงตำแหน่งภารกิจซ่อมรถบัสของคุณ",
          "<#FFB703>[!] <#F4F1DE>ภารกิจที่ยังไม่เสร็จ",
          "<#6EEB83>[/] <#F4F1DE>ภารกิจที่เสร็จแล้ว",
          "<#72EFDD>[^] <#F4F1DE>ตำแหน่งและทิศทางของคุณ",
          "<shadow:#000000FF><#5A4E7C>(</shadow><#B8B0C8>ถือไว้ในมือซ้ายเพื่อดูแผนที่<shadow:#000000FF><#5A4E7C>)</shadow>"
        );
      }
      item.setItemMeta(meta);
    }
    return item;
  }

  public void distributeMaps() {
    initMap();
    cachedBounds = calculateBounds();
    for(Player player : arena.getPlayersLeft()) {
      giveMap(player);
    }
  }

  public void giveMap(Player player) {
    if(player == null || !player.isOnline()) {
      return;
    }
    ItemStack mapItem = createMapItem(player);
    player.getInventory().setItemInOffHand(mapItem);
    player.getInventory().setItem(7, mapItem);
    player.updateInventory();
  }

  public void cleanup() {
    cachedBounds = null;
  }

  public Bounds getBounds() {
    if(cachedBounds == null) {
      cachedBounds = calculateBounds();
    }
    return cachedBounds;
  }

  public static class Bounds {
    public final double minX;
    public final double maxX;
    public final double minZ;
    public final double maxZ;
    public final double centerX;
    public final double centerZ;
    public final double scale;

    public Bounds(double minX, double maxX, double minZ, double maxZ) {
      this.minX = minX;
      this.maxX = maxX;
      this.minZ = minZ;
      this.maxZ = maxZ;
      this.centerX = (minX + maxX) / 2.0;
      this.centerZ = (minZ + maxZ) / 2.0;
      double spanX = Math.max(maxX - minX, 20.0);
      double spanZ = Math.max(maxZ - minZ, 20.0);
      double maxSpan = Math.max(spanX, spanZ) + 24.0;
      this.scale = 92.0 / maxSpan;
    }

    public int toPixelX(double worldX) {
      int px = 64 + (int) Math.round((worldX - centerX) * scale);
      return Math.max(8, Math.min(120, px));
    }

    public int toPixelY(double worldZ) {
      int py = 64 + (int) Math.round((worldZ - centerZ) * scale);
      return Math.max(16, Math.min(112, py));
    }

    public int toCursorX(double worldX) {
      int cx = (int) Math.round((worldX - centerX) * scale * 2.0);
      return Math.max(-120, Math.min(120, cx));
    }

    public int toCursorY(double worldZ) {
      int cy = (int) Math.round((worldZ - centerZ) * scale * 2.0);
      return Math.max(-105, Math.min(105, cy));
    }
  }

  public static class MinimapRenderer extends MapRenderer {

    private final Arena arena;

    public MinimapRenderer(Arena arena) {
      super(true);
      this.arena = arena;
    }

    @Override
    public void render(MapView map, MapCanvas canvas, Player player) {
      if(arena.getArenaState() != IArenaState.IN_GAME || !player.isOnline()) {
        return;
      }

      IUser user = arena.getPlugin().getUserManager().getUser(player);
      boolean isMurderer = Role.isRole(Role.MURDERER, user, arena) || arena.getMurdererList().contains(player);
      Bounds bounds = arena.getMinimap().getBounds();

      BufferedImage img = new BufferedImage(128, 128, BufferedImage.TYPE_INT_RGB);
      Graphics2D g = img.createGraphics();
      g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
      g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

      g.setColor(new Color(16, 22, 32));
      g.fillRect(0, 0, 128, 128);

      if(!isMurderer && arena.getSabotageManager().isCommsJammed()) {
        g.setColor(new Color(15, 15, 22));
        g.fillRect(0, 0, 128, 128);

        g.setColor(new Color(12, 18, 28));
        g.fillRect(0, 0, 128, 14);
        g.setColor(new Color(255, 60, 60));
        g.setFont(new Font("SansSerif", Font.BOLD, 9));
        g.drawString("[!] RADAR OFFLINE", 18, 10);

        g.setColor(new Color(50, 55, 65));
        for(int y = 20; y < 110; y += 6) {
          int startX = 6 + (int) ((Math.sin(y + System.currentTimeMillis() / 200.0) + 1.0) * 10);
          int endX = 122 - (int) ((Math.cos(y + System.currentTimeMillis() / 200.0) + 1.0) * 10);
          g.drawLine(startX, y, endX, y);
        }

        g.setColor(new Color(255, 70, 70));
        g.setFont(new Font("SansSerif", Font.BOLD, 10));
        g.drawString("COMMS JAMMED!", 16, 48);

        g.setColor(new Color(230, 230, 230));
        g.setFont(new Font("SansSerif", Font.PLAIN, 8));
        g.drawString("Restore radio at Comms", 16, 66);
        g.drawString("to re-enable your map!", 18, 78);

        Location fixLoc = arena.getSabotageManager().getActiveFixLocation();
        if(fixLoc != null) {
          int fx = bounds.toPixelX(fixLoc.getX());
          int fy = bounds.toPixelY(fixLoc.getZ());
          boolean flash = (System.currentTimeMillis() / 400) % 2 == 0;
          g.setColor(flash ? new Color(255, 40, 40) : new Color(220, 160, 0));
          g.fillOval(fx - 4, fy - 4, 8, 8);
          g.setColor(Color.WHITE);
          g.drawOval(fx - 4, fy - 4, 8, 8);
          g.setFont(new Font("SansSerif", Font.BOLD, 7));
          g.drawString("!", fx - 1, fy + 3);
          g.setFont(new Font("SansSerif", Font.BOLD, 7));
          g.setColor(flash ? new Color(255, 100, 100) : new Color(255, 230, 100));
          g.drawString("RADIO", fx + 6, fy + 3);
        }

        g.setColor(new Color(12, 18, 28));
        g.fillRect(0, 114, 128, 14);
        g.setColor(new Color(255, 80, 80));
        g.setFont(new Font("SansSerif", Font.BOLD, 8));
        g.drawString("[!] NO SIGNAL", 34, 124);

        g.dispose();
        canvas.drawImage(0, 0, img);
        canvas.setCursors(new MapCursorCollection());
        return;
      }

      g.setColor(new Color(10, 15, 24));
      g.fillRect(4, 14, 120, 100);

      g.setColor(new Color(22, 32, 46));
      g.drawLine(24, 14, 24, 114);
      g.drawLine(44, 14, 44, 114);
      g.drawLine(64, 14, 64, 114);
      g.drawLine(84, 14, 84, 114);
      g.drawLine(104, 14, 104, 114);

      g.drawLine(4, 34, 124, 34);
      g.drawLine(4, 54, 124, 54);
      g.drawLine(4, 74, 124, 74);
      g.drawLine(4, 94, 124, 94);

      g.setColor(new Color(28, 42, 60));
      g.drawOval(44, 44, 40, 40);
      g.drawOval(24, 24, 80, 80);

      g.setColor(new Color(40, 60, 90));
      g.drawRect(4, 14, 120, 100);

      g.setColor(new Color(12, 18, 28));
      g.fillRect(0, 0, 128, 14);
      g.setColor(new Color(45, 65, 95));
      g.drawLine(0, 13, 128, 13);

      Font fontHeader = new Font("SansSerif", Font.BOLD, 9);
      g.setFont(fontHeader);
      if(arena.getSabotageManager().hasActiveSabotage()) {
        g.setColor(new Color(255, 50, 50));
        g.drawString(" " + arena.getSabotageManager().getActiveSabotage().getDisplayName(), 8, 10);
      } else if(isMurderer) {
        g.setColor(new Color(255, 65, 65));
        g.drawString(" IMPOSTOR RADAR", 12, 10);
      } else {
        g.setColor(new Color(255, 215, 0));
        g.drawString(" PASSENGER MAP", 14, 10);
      }

      if(isMurderer) {
        List<SpecialBlock> sewerBlocks = new ArrayList<>();
        for(SpecialBlock sb : arena.getSpecialBlocks()) {
          if(sb.getSpecialBlockType() == SpecialBlock.SpecialBlockType.RAPID_TELEPORTATION) {
            sewerBlocks.add(sb);
          }
        }

        if(sewerBlocks.size() > 1) {
          g.setColor(new Color(160, 30, 30));
          for(int i = 0; i < sewerBlocks.size(); i++) {
            Location l1 = sewerBlocks.get(i).getLocation();
            Location l2 = sewerBlocks.get((i + 1) % sewerBlocks.size()).getLocation();
            int x1 = bounds.toPixelX(l1.getX());
            int y1 = bounds.toPixelY(l1.getZ());
            int x2 = bounds.toPixelX(l2.getX());
            int y2 = bounds.toPixelY(l2.getZ());
            g.drawLine(x1, y1, x2, y2);
          }
        }

        List<Location> allTasks = arena.getAllTaskLocations();
        for(Location tLoc : allTasks) {
          if(tLoc == null) {
            continue;
          }
          int tx = bounds.toPixelX(tLoc.getX());
          int ty = bounds.toPixelY(tLoc.getZ());

          g.setColor(new Color(255, 215, 0));
          g.fillRect(tx - 3, ty - 3, 6, 6);
          g.setColor(new Color(140, 100, 10));
          g.drawRect(tx - 3, ty - 3, 6, 6);
          g.setColor(new Color(30, 25, 0));
          g.setFont(new Font("SansSerif", Font.BOLD, 6));
          g.drawString("!", tx - 1, ty + 3);
        }

        for(int i = 0; i < sewerBlocks.size(); i++) {
          Location vLoc = sewerBlocks.get(i).getLocation();
          int vx = bounds.toPixelX(vLoc.getX());
          int vy = bounds.toPixelY(vLoc.getZ());

          g.setColor(new Color(255, 45, 45));
          g.fillOval(vx - 4, vy - 4, 8, 8);
          g.setColor(new Color(255, 255, 255));
          g.drawOval(vx - 4, vy - 4, 8, 8);

          g.setFont(new Font("SansSerif", Font.BOLD, 6));
          g.setColor(Color.WHITE);
          g.drawString("V", vx - 2, vy + 3);

          g.setFont(new Font("SansSerif", Font.PLAIN, 7));
          g.setColor(new Color(255, 120, 120));
          g.drawString("V" + (i + 1), vx + 5, vy + 3);
        }
      } else {
        List<GameTask> myTasks = arena.getTaskManager().getPlayerTasks(player);
        if(myTasks != null) {
          for(GameTask gt : myTasks) {
            Location tLoc = gt.getLocation();
            if(tLoc == null) {
              continue;
            }
            int tx = bounds.toPixelX(tLoc.getX());
            int ty = bounds.toPixelY(tLoc.getZ());

            if(gt.isCompleted()) {
              g.setColor(new Color(40, 200, 60));
              g.fillOval(tx - 4, ty - 4, 8, 8);
              g.setColor(new Color(255, 255, 255));
              g.drawOval(tx - 4, ty - 4, 8, 8);
              g.setColor(Color.WHITE);
              g.setFont(new Font("SansSerif", Font.BOLD, 6));
              g.drawString("[/]", tx - 2, ty + 3);

              g.setFont(new Font("SansSerif", Font.PLAIN, 7));
              g.setColor(new Color(120, 255, 140));
              g.drawString("Done", tx + 5, ty + 3);
            } else {
              g.setColor(new Color(255, 215, 0));
              g.fillOval(tx - 4, ty - 4, 8, 8);
              g.setColor(new Color(255, 255, 255));
              g.drawOval(tx - 4, ty - 4, 8, 8);
              g.setColor(new Color(40, 30, 0));
              g.setFont(new Font("SansSerif", Font.BOLD, 7));
              g.drawString("!", tx - 1, ty + 3);

              g.setFont(new Font("SansSerif", Font.PLAIN, 7));
              g.setColor(new Color(255, 230, 140));
              String name = gt.getTaskType().getDisplayName();
              if(name.length() > 6) {
                name = name.substring(0, 6);
              }
              g.drawString(name, tx + 5, ty + 3);
            }
          }
        }
      }

      if(arena.getCampfireLocation() != null) {
        Location campLoc = arena.getCampfireLocation();
        int cx = bounds.toPixelX(campLoc.getX());
        int cy = bounds.toPixelY(campLoc.getZ());
        g.setColor(new Color(255, 130, 20));
        g.fillOval(cx - 4, cy - 4, 8, 8);
        g.setColor(new Color(255, 220, 120));
        g.drawOval(cx - 4, cy - 4, 8, 8);
        g.setFont(new Font("SansSerif", Font.BOLD, 6));
        g.setColor(Color.WHITE);
        g.drawString("C", cx - 2, cy + 3);
      }

      if(arena.getSabotageManager().hasActiveSabotage()) {
        Location fixLoc = arena.getSabotageManager().getActiveFixLocation();
        if(fixLoc != null) {
          int fx = bounds.toPixelX(fixLoc.getX());
          int fy = bounds.toPixelY(fixLoc.getZ());
          boolean flash = (System.currentTimeMillis() / 400) % 2 == 0;
          g.setColor(flash ? new Color(255, 30, 30) : new Color(230, 180, 0));
          g.fillOval(fx - 5, fy - 5, 10, 10);
          g.setColor(Color.WHITE);
          g.drawOval(fx - 5, fy - 5, 10, 10);
          g.setFont(new Font("SansSerif", Font.BOLD, 7));
          g.drawString("!", fx - 1, fy + 3);

          g.setFont(new Font("SansSerif", Font.BOLD, 7));
          g.setColor(flash ? new Color(255, 80, 80) : new Color(255, 220, 80));
          g.drawString("FIX", fx + 7, fy + 3);
        }
      }

      int px = bounds.toPixelX(player.getLocation().getX());
      int py = bounds.toPixelY(player.getLocation().getZ());
      g.setColor(new Color(0, 220, 255));
      g.drawOval(px - 4, py - 4, 8, 8);

      g.setColor(new Color(12, 18, 28));
      g.fillRect(0, 114, 128, 14);
      g.setColor(new Color(45, 65, 95));
      g.drawLine(0, 114, 128, 114);

      Font fontSmall = new Font("SansSerif", Font.PLAIN, 8);
      g.setFont(fontSmall);

      if(isMurderer) {
        g.setColor(new Color(255, 50, 50));
        g.fillOval(6, 118, 5, 5);
        g.setColor(new Color(200, 200, 200));
        g.drawString("Vent", 14, 123);

        g.setColor(new Color(255, 215, 0));
        g.fillOval(48, 118, 5, 5);
        g.setColor(new Color(200, 200, 200));
        g.drawString("Task", 56, 123);

        g.setColor(new Color(0, 220, 255));
        g.fillOval(90, 118, 5, 5);
        g.setColor(new Color(200, 200, 200));
        g.drawString("You", 98, 123);
      } else {
        g.setColor(new Color(255, 215, 0));
        g.fillOval(8, 118, 5, 5);
        g.setColor(new Color(200, 200, 200));
        g.drawString("Task", 16, 123);

        g.setColor(new Color(40, 200, 60));
        g.fillOval(52, 118, 5, 5);
        g.setColor(new Color(200, 200, 200));
        g.drawString("Done", 60, 123);

        g.setColor(new Color(0, 220, 255));
        g.fillOval(94, 118, 5, 5);
        g.setColor(new Color(200, 200, 200));
        g.drawString("You", 102, 123);
      }

      g.dispose();
      canvas.drawImage(0, 0, img);

      MapCursorCollection cursors = new MapCursorCollection();
      int curX = bounds.toCursorX(player.getLocation().getX());
      int curY = bounds.toCursorY(player.getLocation().getZ());
      float yaw = (player.getLocation().getYaw() % 360.0f + 360.0f) % 360.0f;
      byte dir = (byte) Math.round(yaw * 16.0f / 360.0f);
      if(dir >= 16) {
        dir = 0;
      }
      cursors.addCursor(new MapCursor((byte) curX, (byte) curY, dir, MapCursor.Type.PLAYER, true));
      canvas.setCursors(cursors);
    }
  }
}
