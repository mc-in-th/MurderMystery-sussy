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

package plugily.projects.murdermystery.arena.cutscene;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.data.BlockData;
import plugily.projects.minigamesbox.classic.PluginMain;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.zip.GZIPInputStream;

public final class BusSchematicLoader {

  public static final String DEFAULT_SCHEMATIC_NAME = "bus.schem";
  private static final Map<String, CachedSchematic> CACHE = new ConcurrentHashMap<>();

  private BusSchematicLoader() {
  }

  public static void ensureDefaultSchematics(PluginMain plugin) {
    if(plugin == null) {
      return;
    }
    File dir = new File(plugin.getDataFolder(), "schematics");
    if(!dir.exists()) {
      dir.mkdirs();
    }
    File target = new File(dir, DEFAULT_SCHEMATIC_NAME);
    if(target.exists() && target.length() > 0) {
      return;
    }
    try(InputStream in = plugin.getResource("schematics/" + DEFAULT_SCHEMATIC_NAME)) {
      if(in == null) {
        return;
      }
      try(FileOutputStream out = new FileOutputStream(target)) {
        byte[] buf = new byte[4096];
        int read;
        while((read = in.read(buf)) != -1) {
          out.write(buf, 0, read);
        }
      }
    } catch(Throwable ignored) {
    }
  }

  public static LoadedSchematic loadSchematic(PluginMain plugin, String schematicName) {
    ensureDefaultSchematics(plugin);
    String safeName = sanitizeFileName(schematicName);
    File dir = new File(plugin.getDataFolder(), "schematics");
    File file = new File(dir, safeName);

    if(file.exists() && file.isFile()) {
      String cacheKey = file.getAbsolutePath();
      long lastMod = file.lastModified();
      long length = file.length();
      CachedSchematic cached = CACHE.get(cacheKey);
      if(cached != null && cached.lastModified == lastMod && cached.length == length) {
        return cached.schematic;
      }
      try(InputStream fis = new FileInputStream(file)) {
        LoadedSchematic parsed = parseStream(fis);
        if(parsed != null && !parsed.getParts().isEmpty()) {
          CACHE.put(cacheKey, new CachedSchematic(lastMod, length, parsed));
          return parsed;
        }
      } catch(Throwable ignored) {
      }
    }

    String resourceKey = "jar:" + safeName;
    CachedSchematic cachedJar = CACHE.get(resourceKey);
    if(cachedJar != null) {
      return cachedJar.schematic;
    }
    try(InputStream res = plugin.getResource("schematics/" + safeName)) {
      if(res != null) {
        LoadedSchematic parsed = parseStream(res);
        if(parsed != null && !parsed.getParts().isEmpty()) {
          CACHE.put(resourceKey, new CachedSchematic(0L, 0L, parsed));
          return parsed;
        }
      }
    } catch(Throwable ignored) {
    }
    return null;
  }

  public static String sanitizeFileName(String name) {
    if(name == null || name.trim().isEmpty()) {
      return DEFAULT_SCHEMATIC_NAME;
    }
    String cleaned = new File(name.trim()).getName();
    if(cleaned.isEmpty()) {
      return DEFAULT_SCHEMATIC_NAME;
    }
    if(!cleaned.toLowerCase(Locale.ROOT).endsWith(".schem") && !cleaned.toLowerCase(Locale.ROOT).endsWith(".schematic")) {
      cleaned = cleaned + ".schem";
    }
    return cleaned;
  }

  private static LoadedSchematic parseStream(InputStream rawIn) throws IOException {
    BufferedInputStream buffered = new BufferedInputStream(rawIn);
    buffered.mark(4);
    int b1 = buffered.read();
    int b2 = buffered.read();
    buffered.reset();
    InputStream effective = (b1 == 0x1F && b2 == 0x8B) ? new GZIPInputStream(buffered) : buffered;

    try(DataInputStream dis = new DataInputStream(new BufferedInputStream(effective))) {
      int rootTag = dis.readUnsignedByte();
      if(rootTag != 10) {
        return null;
      }
      dis.readUTF();
      Map<String, Object> root = readCompound(dis);
      return buildSchematicFromNbt(root);
    }
  }

  @SuppressWarnings("unchecked")
  private static LoadedSchematic buildSchematicFromNbt(Map<String, Object> root) {
    Map<String, Object> schem = root;
    if(root.get("Schematic") instanceof Map) {
      schem = (Map<String, Object>) root.get("Schematic");
    }

    int width = getInt(schem.get("Width"), 0);
    int height = getInt(schem.get("Height"), 0);
    int length = getInt(schem.get("Length"), 0);
    if(width <= 0 || height <= 0 || length <= 0) {
      return null;
    }

    Map<String, Object> paletteMap = null;
    byte[] blockDataBytes = null;

    if(schem.get("Blocks") instanceof Map) {
      Map<String, Object> blocksCompound = (Map<String, Object>) schem.get("Blocks");
      if(blocksCompound.get("Palette") instanceof Map) {
        paletteMap = (Map<String, Object>) blocksCompound.get("Palette");
      }
      if(blocksCompound.get("Data") instanceof byte[]) {
        blockDataBytes = (byte[]) blocksCompound.get("Data");
      }
    }
    if(paletteMap == null && schem.get("Palette") instanceof Map) {
      paletteMap = (Map<String, Object>) schem.get("Palette");
    }
    if(blockDataBytes == null && schem.get("BlockData") instanceof byte[]) {
      blockDataBytes = (byte[]) schem.get("BlockData");
    }
    if(paletteMap == null || blockDataBytes == null) {
      return null;
    }

    Map<Integer, String> idToState = new HashMap<>();
    for(Map.Entry<String, Object> entry : paletteMap.entrySet()) {
      int id = getInt(entry.getValue(), -1);
      if(id >= 0) {
        idToState.put(id, entry.getKey());
      }
    }

    int totalCells = width * height * length;
    int[] grid = new int[totalCells];
    int idx = 0;
    int pos = 0;
    while(pos < blockDataBytes.length && idx < totalCells) {
      int value = 0;
      int shift = 0;
      while(pos < blockDataBytes.length) {
        int b = blockDataBytes[pos++] & 0xFF;
        value |= (b & 0x7F) << shift;
        if((b & 0x80) == 0) {
          break;
        }
        shift += 7;
      }
      grid[idx++] = value;
    }

    Map<Integer, ResolvedState> resolvedPalette = new HashMap<>();
    for(Map.Entry<Integer, String> entry : idToState.entrySet()) {
      ResolvedState resolved = resolveBlockState(entry.getValue());
      if(resolved != null) {
        resolvedPalette.put(entry.getKey(), resolved);
      }
    }

    int minX = width;
    int maxX = -1;
    int minY = height;
    int maxY = -1;
    int minZ = length;
    int maxZ = -1;

    boolean[] includeCell = new boolean[totalCells];
    for(int y = 0; y < height; y++) {
      for(int z = 0; z < length; z++) {
        for(int x = 0; x < width; x++) {
          int cell = (y * length + z) * width + x;
          ResolvedState state = resolvedPalette.get(grid[cell]);
          if(state == null) {
            continue;
          }
          if(isIsolatedCornerMarker(x, y, z, width, height, length, grid, resolvedPalette, state)) {
            continue;
          }
          includeCell[cell] = true;
          if(x < minX) minX = x;
          if(x > maxX) maxX = x;
          if(y < minY) minY = y;
          if(y > maxY) maxY = y;
          if(z < minZ) minZ = z;
          if(z > maxZ) maxZ = z;
        }
      }
    }

    if(maxX < minX || maxY < minY || maxZ < minZ) {
      return null;
    }

    float pivotX = (minX + maxX + 1) * 0.5f;
    float baseY = (float) minY;
    float pivotZ = (minZ + maxZ + 1) * 0.5f;

    float furnaceSumX = 0.0f;
    float furnaceSumY = 0.0f;
    float furnaceSumZ = 0.0f;
    int furnaceCount = 0;

    List<SchematicPart> parts = new ArrayList<>();
    for(int y = 0; y < height; y++) {
      for(int z = 0; z < length; z++) {
        for(int x = 0; x < width; x++) {
          int cell = (y * length + z) * width + x;
          if(!includeCell[cell]) {
            continue;
          }
          ResolvedState state = resolvedPalette.get(grid[cell]);
          if(state == null) {
            continue;
          }
          float ox = (x - pivotX) + state.offsetX;
          float oy = (y - baseY) + state.offsetY;
          float oz = (z - pivotZ) + state.offsetZ;
          parts.add(new SchematicPart(
            state.blockData,
            ox,
            oy,
            oz,
            state.scaleX,
            state.scaleY,
            state.scaleZ,
            state.emissive
          ));
          if(state.radiatorAnchor) {
            furnaceSumX += (x + 0.5f - pivotX);
            furnaceSumY += (y + 0.5f - baseY);
            furnaceSumZ += (z + 0.5f - pivotZ);
            furnaceCount++;
          }
        }
      }
    }

    float hoodX;
    float hoodY;
    float hoodZ;
    if(furnaceCount > 0) {
      hoodX = (furnaceSumX / furnaceCount) + 0.65f;
      hoodY = (furnaceSumY / furnaceCount) + 0.55f;
      hoodZ = (furnaceSumZ / furnaceCount);
    } else {
      hoodX = (maxX + 0.5f) - pivotX;
      hoodY = Math.min(2.0f, (maxY - minY + 1) * 0.35f);
      hoodZ = 0.0f;
    }

    float exhaustX = (minX - 0.15f) - pivotX;
    float exhaustY = 0.65f;
    float exhaustZ = Math.max(0.0f, (maxZ - 1.0f) - pivotZ);

    return new LoadedSchematic(
      parts,
      maxX - minX + 1,
      maxY - minY + 1,
      maxZ - minZ + 1,
      hoodX,
      hoodY,
      hoodZ,
      exhaustX,
      exhaustY,
      exhaustZ
    );
  }

  private static boolean isIsolatedCornerMarker(
    int x,
    int y,
    int z,
    int width,
    int height,
    int length,
    int[] grid,
    Map<Integer, ResolvedState> resolvedPalette,
    ResolvedState state
  ) {
    boolean isOuterCorner = (x == 0 || x == width - 1)
      && (y == 0 || y == height - 1)
      && (z == 0 || z == length - 1);
    if(!isOuterCorner || state.blockData.getMaterial() != Material.GLASS) {
      return false;
    }
    int[][] dirs = {{1, 0, 0}, {-1, 0, 0}, {0, 1, 0}, {0, -1, 0}, {0, 0, 1}, {0, 0, -1}};
    for(int[] d : dirs) {
      int nx = x + d[0];
      int ny = y + d[1];
      int nz = z + d[2];
      if(nx >= 0 && nx < width && ny >= 0 && ny < height && nz >= 0 && nz < length) {
        int nCell = (ny * length + nz) * width + nx;
        if(resolvedPalette.containsKey(grid[nCell])) {
          return false;
        }
      }
    }
    return true;
  }

  public static SchematicPart createLiveWorldPart(
    BlockData blockData,
    float baseOffsetX,
    float baseOffsetY,
    float baseOffsetZ
  ) {
    if(blockData == null) {
      return null;
    }
    Material type = blockData.getMaterial();
    if(type.isAir()
      || type == Material.BARRIER
      || type == Material.LIGHT
      || type == Material.STRUCTURE_VOID
      || type == Material.MOVING_PISTON) {
      return null;
    }
    String rawState = blockData.getAsString();
    ResolvedState resolved = resolveBlockState(rawState);
    if(resolved == null) {
      return null;
    }
    String lowerName = type.name().toLowerCase(Locale.ROOT);
    boolean isSignFallback = lowerName.endsWith("_wall_sign")
      || (lowerName.endsWith("_sign") && !lowerName.endsWith("_hanging_sign"));
    BlockData finalData = isSignFallback ? resolved.blockData : blockData.clone();
    return new SchematicPart(
      finalData,
      baseOffsetX + resolved.offsetX,
      baseOffsetY + resolved.offsetY,
      baseOffsetZ + resolved.offsetZ,
      resolved.scaleX,
      resolved.scaleY,
      resolved.scaleZ,
      resolved.emissive
    );
  }

  private static ResolvedState resolveBlockState(String rawState) {
    if(rawState == null || rawState.isEmpty()) {
      return null;
    }
    String trimmed = rawState.trim();
    int bracketIdx = trimmed.indexOf('[');
    String baseId = bracketIdx >= 0 ? trimmed.substring(0, bracketIdx) : trimmed;
    String props = bracketIdx >= 0 ? trimmed.substring(bracketIdx) : "";
    String shortName = baseId.startsWith("minecraft:") ? baseId.substring("minecraft:".length()) : baseId;
    String lowerShort = shortName.toLowerCase(Locale.ROOT);

    if(lowerShort.equals("air") || lowerShort.equals("cave_air") || lowerShort.equals("void_air") || lowerShort.equals("structure_void")) {
      return null;
    }

    if(lowerShort.endsWith("_wall_sign")) {
      String wood = lowerShort.substring(0, lowerShort.length() - "_wall_sign".length());
      BlockData plankData = createPlankFallback(wood);
      String facing = extractProperty(props, "facing", "north");
      float ox = 0.0f;
      float oy = 0.28125f;
      float oz = 0.0f;
      float sx = 1.0f;
      float sy = 0.5f;
      float sz = 0.125f;
      switch(facing) {
        case "north":
          oz = 0.875f;
          sx = 1.0f;
          sz = 0.125f;
          break;
        case "south":
          oz = 0.0f;
          sx = 1.0f;
          sz = 0.125f;
          break;
        case "west":
          ox = 0.875f;
          sx = 0.125f;
          sz = 1.0f;
          break;
        case "east":
          ox = 0.0f;
          sx = 0.125f;
          sz = 1.0f;
          break;
        default:
          break;
      }
      return new ResolvedState(plankData, ox, oy, oz, sx, sy, sz, false, false);
    }

    if(lowerShort.endsWith("_sign") && !lowerShort.endsWith("_hanging_sign")) {
      String wood = lowerShort.substring(0, lowerShort.length() - "_sign".length());
      BlockData plankData = createPlankFallback(wood);
      return new ResolvedState(plankData, 0.0f, 0.5f, 0.4375f, 1.0f, 0.5f, 0.125f, false, false);
    }

    boolean emissive = props.contains("lit=true")
      || lowerShort.contains("glowstone")
      || lowerShort.contains("sea_lantern")
      || lowerShort.contains("shroomlight")
      || lowerShort.contains("froglight")
      || lowerShort.endsWith("lantern");
    boolean radiator = lowerShort.equals("blast_furnace");

    BlockData parsed = tryCreateBlockData(trimmed);
    if(parsed == null) {
      parsed = tryFallbackBlockData(lowerShort, props);
    }
    if(parsed == null || parsed.getMaterial().isAir()) {
      return null;
    }
    return new ResolvedState(parsed, 0.0f, 0.0f, 0.0f, 1.0f, 1.0f, 1.0f, emissive, radiator);
  }

  private static BlockData createPlankFallback(String woodPrefix) {
    BlockData direct = tryCreateBlockData("minecraft:" + woodPrefix + "_planks");
    if(direct != null) {
      return direct;
    }
    if(woodPrefix.contains("pale")) {
      BlockData birch = tryCreateBlockData("minecraft:birch_planks");
      if(birch != null) {
        return birch;
      }
    }
    return Material.OAK_PLANKS.createBlockData();
  }

  private static BlockData tryFallbackBlockData(String lowerShort, String props) {
    if(lowerShort.equals("polished_cinnabar_wall") || lowerShort.contains("cinnabar_wall")) {
      BlockData wall = tryCreateBlockData("minecraft:red_nether_brick_wall" + props);
      if(wall != null) {
        return wall;
      }
      return tryCreateBlockData("minecraft:red_nether_brick_wall");
    }
    if(lowerShort.contains("cinnabar")) {
      return Material.RED_TERRACOTTA.createBlockData();
    }
    if(lowerShort.startsWith("pale_oak_")) {
      String suffix = lowerShort.substring("pale_oak_".length());
      BlockData birchVariant = tryCreateBlockData("minecraft:birch_" + suffix + props);
      if(birchVariant != null) {
        return birchVariant;
      }
      birchVariant = tryCreateBlockData("minecraft:birch_" + suffix);
      if(birchVariant != null) {
        return birchVariant;
      }
    }
    BlockData baseOnly = tryCreateBlockData("minecraft:" + lowerShort);
    if(baseOnly != null) {
      return baseOnly;
    }
    return Material.RED_CONCRETE.createBlockData();
  }

  private static BlockData tryCreateBlockData(String dataString) {
    try {
      return Bukkit.createBlockData(dataString);
    } catch(Throwable ignored) {
      return null;
    }
  }

  private static String extractProperty(String props, String key, String defaultValue) {
    if(props == null || props.length() < 3) {
      return defaultValue;
    }
    String inner = props.substring(1, props.length() - 1);
    String prefix = key + "=";
    for(String token : inner.split(",")) {
      String t = token.trim();
      if(t.startsWith(prefix)) {
        return t.substring(prefix.length()).toLowerCase(Locale.ROOT);
      }
    }
    return defaultValue;
  }

  private static int getInt(Object num, int def) {
    if(num instanceof Number) {
      return ((Number) num).intValue();
    }
    return def;
  }

  private static Map<String, Object> readCompound(DataInputStream dis) throws IOException {
    Map<String, Object> map = new HashMap<>();
    while(true) {
      int type = dis.readUnsignedByte();
      if(type == 0) {
        break;
      }
      String name = dis.readUTF();
      Object payload = readPayload(dis, type);
      map.put(name, payload);
    }
    return map;
  }

  private static Object readPayload(DataInputStream dis, int type) throws IOException {
    switch(type) {
      case 1:
        return dis.readByte();
      case 2:
        return dis.readShort();
      case 3:
        return dis.readInt();
      case 4:
        return dis.readLong();
      case 5:
        return dis.readFloat();
      case 6:
        return dis.readDouble();
      case 7: {
        int len = dis.readInt();
        byte[] bytes = new byte[Math.max(0, len)];
        dis.readFully(bytes);
        return bytes;
      }
      case 8:
        return dis.readUTF();
      case 9: {
        int elemType = dis.readUnsignedByte();
        int count = dis.readInt();
        List<Object> list = new ArrayList<>(Math.max(0, count));
        for(int i = 0; i < count; i++) {
          list.add(readPayload(dis, elemType));
        }
        return list;
      }
      case 10:
        return readCompound(dis);
      case 11: {
        int len = dis.readInt();
        int[] ints = new int[Math.max(0, len)];
        for(int i = 0; i < len; i++) {
          ints[i] = dis.readInt();
        }
        return ints;
      }
      case 12: {
        int len = dis.readInt();
        long[] longs = new long[Math.max(0, len)];
        for(int i = 0; i < len; i++) {
          longs[i] = dis.readLong();
        }
        return longs;
      }
      default:
        throw new IOException("Unsupported NBT tag type: " + type);
    }
  }

  private static final class CachedSchematic {
    private final long lastModified;
    private final long length;
    private final LoadedSchematic schematic;

    private CachedSchematic(long lastModified, long length, LoadedSchematic schematic) {
      this.lastModified = lastModified;
      this.length = length;
      this.schematic = schematic;
    }
  }

  private static final class ResolvedState {
    private final BlockData blockData;
    private final float offsetX;
    private final float offsetY;
    private final float offsetZ;
    private final float scaleX;
    private final float scaleY;
    private final float scaleZ;
    private final boolean emissive;
    private final boolean radiatorAnchor;

    private ResolvedState(
      BlockData blockData,
      float offsetX,
      float offsetY,
      float offsetZ,
      float scaleX,
      float scaleY,
      float scaleZ,
      boolean emissive,
      boolean radiatorAnchor
    ) {
      this.blockData = blockData;
      this.offsetX = offsetX;
      this.offsetY = offsetY;
      this.offsetZ = offsetZ;
      this.scaleX = scaleX;
      this.scaleY = scaleY;
      this.scaleZ = scaleZ;
      this.emissive = emissive;
      this.radiatorAnchor = radiatorAnchor;
    }
  }

  public static final class SchematicPart {
    private final BlockData blockData;
    private final float offsetX;
    private final float offsetY;
    private final float offsetZ;
    private final float scaleX;
    private final float scaleY;
    private final float scaleZ;
    private final boolean emissive;

    public SchematicPart(
      BlockData blockData,
      float offsetX,
      float offsetY,
      float offsetZ,
      float scaleX,
      float scaleY,
      float scaleZ,
      boolean emissive
    ) {
      this.blockData = blockData;
      this.offsetX = offsetX;
      this.offsetY = offsetY;
      this.offsetZ = offsetZ;
      this.scaleX = scaleX;
      this.scaleY = scaleY;
      this.scaleZ = scaleZ;
      this.emissive = emissive;
    }

    public BlockData getBlockData() {
      return blockData;
    }

    public float getOffsetX() {
      return offsetX;
    }

    public float getOffsetY() {
      return offsetY;
    }

    public float getOffsetZ() {
      return offsetZ;
    }

    public float getScaleX() {
      return scaleX;
    }

    public float getScaleY() {
      return scaleY;
    }

    public float getScaleZ() {
      return scaleZ;
    }

    public boolean isEmissive() {
      return emissive;
    }
  }

  public static final class LoadedSchematic {
    private final List<SchematicPart> parts;
    private final int spanX;
    private final int spanY;
    private final int spanZ;
    private final float hoodLocalX;
    private final float hoodLocalY;
    private final float hoodLocalZ;
    private final float exhaustLocalX;
    private final float exhaustLocalY;
    private final float exhaustLocalZ;

    public LoadedSchematic(
      List<SchematicPart> parts,
      int spanX,
      int spanY,
      int spanZ,
      float hoodLocalX,
      float hoodLocalY,
      float hoodLocalZ,
      float exhaustLocalX,
      float exhaustLocalY,
      float exhaustLocalZ
    ) {
      this.parts = Collections.unmodifiableList(parts);
      this.spanX = spanX;
      this.spanY = spanY;
      this.spanZ = spanZ;
      this.hoodLocalX = hoodLocalX;
      this.hoodLocalY = hoodLocalY;
      this.hoodLocalZ = hoodLocalZ;
      this.exhaustLocalX = exhaustLocalX;
      this.exhaustLocalY = exhaustLocalY;
      this.exhaustLocalZ = exhaustLocalZ;
    }

    public List<SchematicPart> getParts() {
      return parts;
    }

    public int getSpanX() {
      return spanX;
    }

    public int getSpanY() {
      return spanY;
    }

    public int getSpanZ() {
      return spanZ;
    }

    public float getHoodLocalX() {
      return hoodLocalX;
    }

    public float getHoodLocalY() {
      return hoodLocalY;
    }

    public float getHoodLocalZ() {
      return hoodLocalZ;
    }

    public float getExhaustLocalX() {
      return exhaustLocalX;
    }

    public float getExhaustLocalY() {
      return exhaustLocalY;
    }

    public float getExhaustLocalZ() {
      return exhaustLocalZ;
    }
  }
}
