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

import org.bukkit.Chunk;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Rotation;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.sign.Side;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.GlowItemFrame;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Transformation;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import plugily.projects.minigamesbox.classic.PluginMain;
import plugily.projects.murdermystery.arena.Arena;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class BusCutsceneRig {

  private static final int MAX_HIDE_VOLUME = 25000;
  private static final int MIN_LIVE_PARTS_THRESHOLD = 40;
  private static final double CRUISE_FRACTION = 0.65;
  private static final double CRUISE_SPEED_FACTOR = 2.0 / (1.0 + CRUISE_FRACTION);

  private static final Particle PARTICLE_LARGE_SMOKE = resolveParticle("LARGE_SMOKE", "SMOKE_LARGE");
  private static final Particle PARTICLE_CAMPFIRE_SMOKE = resolveParticle("CAMPFIRE_COSY_SMOKE", "SMOKE_LARGE");
  private static final Particle PARTICLE_CLOUD = resolveParticle("CLOUD", "CLOUD");
  private static final Particle PARTICLE_FLAME = resolveParticle("FLAME", "FLAME");
  private static final Particle PARTICLE_LAVA = resolveParticle("LAVA", "LAVA");

  public interface ServerPositionUpdater {
    void update(Entity anchor, double x, double y, double z);
  }

  private final PluginMain plugin;
  private final Arena arena;
  private final World world;
  private final double startX;
  private final double startY;
  private final double startZ;
  private final double deltaX;
  private final double deltaY;
  private final double deltaZ;
  private final Quaternionf busRotation;
  private final BusSchematicLoader.LoadedSchematic schematic;

  private TextDisplay anchor;
  private final List<Display> rigEntities = new ArrayList<>();
  private final List<Chunk> ticketedChunks = new ArrayList<>();
  private final List<BlockState> realHiddenStates = new ArrayList<>();
  private final List<BlockState> fakeAirStates = new ArrayList<>();
  private final List<BusSchematicLoader.SchematicPart> liveBlockParts = new ArrayList<>();
  private final List<Entity> hiddenWorldEntities = new ArrayList<>();
  private final List<Entity> mapDecorEntitiesToClone = new ArrayList<>();
  private final Map<UUID, List<Entity>> viewerHiddenEntities = new ConcurrentHashMap<>();
  private final Set<UUID> syncedViewers = ConcurrentHashMap.newKeySet();

  private boolean usingLiveWorldBlocks = false;
  private float liveHoodOffsetX = 0.0f;
  private float liveHoodOffsetY = 1.6f;
  private float liveHoodOffsetZ = 0.0f;
  private float liveExhaustOffsetX = 0.0f;
  private float liveExhaustOffsetY = 0.65f;
  private float liveExhaustOffsetZ = 0.0f;
  private boolean hasLiveHoodAnchor = false;

  private boolean misfireOnePlayed = false;
  private boolean misfireTwoPlayed = false;
  private boolean breakdownBlastPlayed = false;
  private int tickCounter = 0;
  private volatile boolean cleanedUp = false;

  private BusCutsceneRig(
    PluginMain plugin,
    Arena arena,
    World world,
    Location startLoc,
    Location endLoc,
    BusSchematicLoader.LoadedSchematic schematic
  ) {
    this.plugin = plugin;
    this.arena = arena;
    this.world = world;
    this.startX = startLoc.getX();
    this.startY = startLoc.getY();
    this.startZ = startLoc.getZ();
    this.deltaX = endLoc.getX() - startLoc.getX();
    this.deltaY = endLoc.getY() - startLoc.getY();
    this.deltaZ = endLoc.getZ() - startLoc.getZ();
    this.schematic = schematic;

    double horizSq = deltaX * deltaX + deltaZ * deltaZ;
    float roadAngleRad;
    if(horizSq > 1e-6) {
      roadAngleRad = (float) Math.atan2(-deltaZ, deltaX);
    } else {
      double yawRad = Math.toRadians(startLoc.getYaw());
      double fx = -Math.sin(yawRad);
      double fz = Math.cos(yawRad);
      roadAngleRad = (float) Math.atan2(-fz, fx);
    }
    this.busRotation = new Quaternionf().rotateY(roadAngleRad);
  }

  public static BusCutsceneRig spawn(
    PluginMain plugin,
    Arena arena,
    Location startLoc,
    Location endLoc,
    String schematicName,
    Location hidePos1,
    Location hidePos2,
    Collection<Player> initialViewers
  ) {
    if(plugin == null || startLoc == null || endLoc == null || startLoc.getWorld() == null) {
      return null;
    }
    World world = startLoc.getWorld();
    if(endLoc.getWorld() != null && !world.equals(endLoc.getWorld())) {
      return null;
    }
    BusSchematicLoader.LoadedSchematic loaded = BusSchematicLoader.loadSchematic(plugin, schematicName);
    boolean hasHideBox = hidePos1 != null && hidePos2 != null;
    if((loaded == null || loaded.getParts().isEmpty()) && !hasHideBox) {
      return null;
    }

    BusCutsceneRig rig = new BusCutsceneRig(plugin, arena, world, startLoc, endLoc, loaded);
    if(!rig.initEntities(hidePos1, hidePos2, initialViewers)) {
      rig.cleanup(initialViewers);
      return null;
    }
    return rig;
  }

  private boolean initEntities(Location hidePos1, Location hidePos2, Collection<Player> initialViewers) {
    preloadRoadAndBusChunks(hidePos1, hidePos2);
    prepareHiddenArenaBlocksAndEntities(hidePos1, hidePos2);

    Location spawnOrigin = new Location(world, startX, startY, startZ, 0.0f, 0.0f);
    try {
      anchor = world.spawn(spawnOrigin, TextDisplay.class, entity -> {
        entity.setText("");
        entity.setBillboard(Display.Billboard.FIXED);
        entity.setDefaultBackground(false);
        entity.setBackgroundColor(Color.fromARGB(0, 0, 0, 0));
        entity.setShadowed(false);
        entity.setGravity(false);
        entity.setPersistent(false);
        entity.setTeleportDuration(1);
        entity.setInterpolationDuration(1);
        entity.setViewRange(4.0f);
        entity.setVisibleByDefault(false);
      });
    } catch(Throwable t) {
      return false;
    }
    if(anchor == null || !anchor.isValid()) {
      return false;
    }

    Display.Brightness fullBright = new Display.Brightness(15, 15);
    Quaternionf identityRot = new Quaternionf();

    usingLiveWorldBlocks = liveBlockParts.size() >= MIN_LIVE_PARTS_THRESHOLD
      || schematic == null
      || schematic.getParts().isEmpty();

    if(usingLiveWorldBlocks) {
      for(BusSchematicLoader.SchematicPart part : liveBlockParts) {
        try {
          Vector3f offset = new Vector3f(part.getOffsetX(), part.getOffsetY(), part.getOffsetZ());
          Vector3f scale = new Vector3f(part.getScaleX(), part.getScaleY(), part.getScaleZ());
          Transformation transformation = new Transformation(offset, identityRot, scale, identityRot);
          BlockDisplay bd = world.spawn(spawnOrigin, BlockDisplay.class, entity -> {
            entity.setBlock(part.getBlockData());
            entity.setBillboard(Display.Billboard.FIXED);
            entity.setGravity(false);
            entity.setPersistent(false);
            entity.setViewRange(4.0f);
            entity.setVisibleByDefault(false);
            if(part.isEmissive()) {
              entity.setBrightness(fullBright);
            }
            entity.setTransformation(transformation);
          });
          if(bd != null) {
            rigEntities.add(bd);
            anchor.addPassenger(bd);
          }
        } catch(Throwable ignored) {
        }
      }
    } else if(schematic != null) {
      for(BusSchematicLoader.SchematicPart part : schematic.getParts()) {
        try {
          Vector3f rotatedOffset = busRotation.transform(
            new Vector3f(part.getOffsetX(), part.getOffsetY(), part.getOffsetZ())
          );
          Vector3f scale = new Vector3f(part.getScaleX(), part.getScaleY(), part.getScaleZ());
          Transformation transformation = new Transformation(
            rotatedOffset,
            new Quaternionf(busRotation),
            scale,
            identityRot
          );
          BlockDisplay bd = world.spawn(spawnOrigin, BlockDisplay.class, entity -> {
            entity.setBlock(part.getBlockData());
            entity.setBillboard(Display.Billboard.FIXED);
            entity.setGravity(false);
            entity.setPersistent(false);
            entity.setViewRange(4.0f);
            entity.setVisibleByDefault(false);
            if(part.isEmissive()) {
              entity.setBrightness(fullBright);
            }
            entity.setTransformation(transformation);
          });
          if(bd != null) {
            rigEntities.add(bd);
            anchor.addPassenger(bd);
          }
        } catch(Throwable ignored) {
        }
      }
    }

    double endX = startX + deltaX;
    double endY = startY + deltaY;
    double endZ = startZ + deltaZ;
    for(Entity mapEntity : mapDecorEntitiesToClone) {
      if(mapEntity == null || !mapEntity.isValid()) {
        continue;
      }
      if(mapEntity instanceof Display) {
        Display cloned = cloneWorldDisplayForRig((Display) mapEntity, endX, endY, endZ);
        if(cloned != null) {
          rigEntities.add(cloned);
          anchor.addPassenger(cloned);
        }
      } else if(mapEntity instanceof ItemFrame) {
        spawnItemFrameRigParts((ItemFrame) mapEntity, spawnOrigin, endX, endY, endZ, fullBright);
      }
    }

    if(rigEntities.isEmpty()) {
      return false;
    }

    syncViewers(initialViewers);
    return true;
  }

  private Display cloneWorldDisplayForRig(Display src, double endX, double endY, double endZ) {
    try {
      Location srcLoc = src.getLocation();
      float srcYaw = srcLoc.getYaw();
      float srcPitch = srcLoc.getPitch();
      Vector3f worldDelta = new Vector3f(
        (float) (srcLoc.getX() - endX),
        (float) (srcLoc.getY() - endY),
        (float) (srcLoc.getZ() - endZ)
      );
      Vector3f localDelta;
      if(src.getBillboard() == Display.Billboard.FIXED
        && (Math.abs(srcYaw) > 1e-4f || Math.abs(srcPitch) > 1e-4f)) {
        Quaternionf entityRot = new Quaternionf().rotationYXZ(
          (float) Math.toRadians(-srcYaw),
          (float) Math.toRadians(srcPitch),
          0.0f
        );
        localDelta = entityRot.invert().transform(new Vector3f(worldDelta));
      } else {
        localDelta = worldDelta;
      }

      Transformation t = src.getTransformation();
      Vector3f shiftedTranslation = new Vector3f(t.getTranslation()).add(localDelta);
      Transformation shiftedTransform = new Transformation(
        shiftedTranslation,
        new Quaternionf(t.getLeftRotation()),
        new Vector3f(t.getScale()),
        new Quaternionf(t.getRightRotation())
      );

      Location spawnLoc = new Location(world, startX, startY, startZ, srcYaw, srcPitch);

      try {
        Entity rawCopy = src.copy();
        if(rawCopy instanceof Display) {
          Display copyDisplay = (Display) rawCopy;
          copyDisplay.setPersistent(false);
          copyDisplay.setGravity(false);
          copyDisplay.setVisibleByDefault(false);
          copyDisplay.setViewRange(Math.max(4.0f, src.getViewRange()));
          copyDisplay.setTransformation(shiftedTransform);
          if(copyDisplay.spawnAt(spawnLoc)) {
            return copyDisplay;
          }
        }
      } catch(Throwable ignored) {
      }

      if(src instanceof BlockDisplay) {
        BlockDisplay bSrc = (BlockDisplay) src;
        return world.spawn(spawnLoc, BlockDisplay.class, entity -> {
          copyBaseDisplayProperties(bSrc, entity, shiftedTransform);
          entity.setBlock(bSrc.getBlock().clone());
        });
      }
      if(src instanceof ItemDisplay) {
        ItemDisplay iSrc = (ItemDisplay) src;
        return world.spawn(spawnLoc, ItemDisplay.class, entity -> {
          copyBaseDisplayProperties(iSrc, entity, shiftedTransform);
          ItemStack stack = iSrc.getItemStack();
          if(stack != null) {
            entity.setItemStack(stack.clone());
          }
          entity.setItemDisplayTransform(iSrc.getItemDisplayTransform());
        });
      }
      if(src instanceof TextDisplay) {
        TextDisplay tSrc = (TextDisplay) src;
        return world.spawn(spawnLoc, TextDisplay.class, entity -> {
          copyBaseDisplayProperties(tSrc, entity, shiftedTransform);
          entity.text(tSrc.text());
          entity.setAlignment(tSrc.getAlignment());
          entity.setLineWidth(tSrc.getLineWidth());
          entity.setShadowed(tSrc.isShadowed());
          entity.setSeeThrough(tSrc.isSeeThrough());
          entity.setDefaultBackground(tSrc.isDefaultBackground());
          entity.setTextOpacity(tSrc.getTextOpacity());
          Color bg = tSrc.getBackgroundColor();
          if(bg != null) {
            entity.setBackgroundColor(bg);
          }
        });
      }
    } catch(Throwable ignored) {
    }
    return null;
  }

  private static void copyBaseDisplayProperties(Display src, Display dst, Transformation shiftedTransform) {
    dst.setBillboard(src.getBillboard());
    dst.setGravity(false);
    dst.setPersistent(false);
    dst.setVisibleByDefault(false);
    dst.setViewRange(Math.max(4.0f, src.getViewRange()));
    dst.setShadowRadius(src.getShadowRadius());
    dst.setShadowStrength(src.getShadowStrength());
    Display.Brightness brightness = src.getBrightness();
    if(brightness != null) {
      dst.setBrightness(brightness);
    }
    Color glowOverride = src.getGlowColorOverride();
    if(glowOverride != null) {
      dst.setGlowColorOverride(glowOverride);
    }
    dst.setTransformation(shiftedTransform);
  }

  private void spawnItemFrameRigParts(
    ItemFrame frame,
    Location spawnOrigin,
    double endX,
    double endY,
    double endZ,
    Display.Brightness fullBright
  ) {
    try {
      Location fLoc = frame.getLocation();
      BlockFace facing = frame.getFacing();
      boolean glowing = frame instanceof GlowItemFrame;
      Vector3f baseDelta = new Vector3f(
        (float) (fLoc.getX() - endX),
        (float) (fLoc.getY() - endY),
        (float) (fLoc.getZ() - endZ)
      );
      Quaternionf identity = new Quaternionf();

      if(!frame.isInvisible()) {
        float sx = facing.getModX() != 0 ? 0.0625f : 0.75f;
        float sy = facing.getModY() != 0 ? 0.0625f : 0.75f;
        float sz = facing.getModZ() != 0 ? 0.0625f : 0.75f;
        Vector3f backOffset = new Vector3f(
          baseDelta.x - sx * 0.5f,
          baseDelta.y - sy * 0.5f,
          baseDelta.z - sz * 0.5f
        );
        Transformation backTransform = new Transformation(
          backOffset,
          identity,
          new Vector3f(sx, sy, sz),
          identity
        );
        BlockData frameBackData = Material.BIRCH_PLANKS.createBlockData();
        BlockDisplay backDisplay = world.spawn(spawnOrigin, BlockDisplay.class, entity -> {
          entity.setBlock(frameBackData);
          entity.setBillboard(Display.Billboard.FIXED);
          entity.setGravity(false);
          entity.setPersistent(false);
          entity.setVisibleByDefault(false);
          entity.setViewRange(4.0f);
          if(glowing) {
            entity.setBrightness(fullBright);
          }
          entity.setTransformation(backTransform);
        });
        if(backDisplay != null) {
          rigEntities.add(backDisplay);
          anchor.addPassenger(backDisplay);
        }
      }

      ItemStack item = frame.getItem();
      if(item != null && !item.getType().isAir()) {
        ItemStack itemCopy = item.clone();
        Quaternionf itemRot = new Quaternionf();
        switch(facing) {
          case EAST:
            itemRot.rotateY((float) Math.toRadians(90.0));
            break;
          case WEST:
            itemRot.rotateY((float) Math.toRadians(-90.0));
            break;
          case NORTH:
            itemRot.rotateY((float) Math.toRadians(180.0));
            break;
          case UP:
            itemRot.rotateX((float) Math.toRadians(-90.0));
            break;
          case DOWN:
            itemRot.rotateX((float) Math.toRadians(90.0));
            break;
          case SOUTH:
          default:
            break;
        }
        int rotSteps = rotationToSteps(frame.getRotation());
        if(rotSteps != 0) {
          itemRot.rotateZ((float) Math.toRadians(-rotSteps * 45.0));
        }
        Vector3f itemOffset = new Vector3f(
          baseDelta.x + facing.getModX() * 0.025f,
          baseDelta.y + facing.getModY() * 0.025f,
          baseDelta.z + facing.getModZ() * 0.025f
        );
        Transformation itemTransform = new Transformation(
          itemOffset,
          itemRot,
          new Vector3f(0.5f, 0.5f, 0.5f),
          identity
        );
        ItemDisplay itemDisplay = world.spawn(spawnOrigin, ItemDisplay.class, entity -> {
          entity.setItemStack(itemCopy);
          entity.setItemDisplayTransform(ItemDisplay.ItemDisplayTransform.FIXED);
          entity.setBillboard(Display.Billboard.FIXED);
          entity.setGravity(false);
          entity.setPersistent(false);
          entity.setVisibleByDefault(false);
          entity.setViewRange(4.0f);
          if(glowing) {
            entity.setBrightness(fullBright);
          }
          entity.setTransformation(itemTransform);
        });
        if(itemDisplay != null) {
          rigEntities.add(itemDisplay);
          anchor.addPassenger(itemDisplay);
        }
      }
    } catch(Throwable ignored) {
    }
  }

  private static int rotationToSteps(Rotation rotation) {
    if(rotation == null) {
      return 0;
    }
    switch(rotation) {
      case CLOCKWISE_45:
        return 1;
      case CLOCKWISE:
        return 2;
      case CLOCKWISE_135:
        return 3;
      case FLIPPED:
        return 4;
      case FLIPPED_45:
        return 5;
      case COUNTER_CLOCKWISE:
        return 6;
      case COUNTER_CLOCKWISE_45:
        return 7;
      case NONE:
      default:
        return 0;
    }
  }

  private void preloadRoadAndBusChunks(Location hidePos1, Location hidePos2) {
    int c1x = ((int) Math.floor(startX)) >> 4;
    int c1z = ((int) Math.floor(startZ)) >> 4;
    int c2x = ((int) Math.floor(startX + deltaX)) >> 4;
    int c2z = ((int) Math.floor(startZ + deltaZ)) >> 4;
    int minCx = Math.min(c1x, c2x) - 1;
    int maxCx = Math.max(c1x, c2x) + 1;
    int minCz = Math.min(c1z, c2z) - 1;
    int maxCz = Math.max(c1z, c2z) + 1;

    Set<Long> visited = new HashSet<>();
    if((long) (maxCx - minCx + 1) * (long) (maxCz - minCz + 1) > 256L) {
      ticketChunk(c1x, c1z, visited);
      ticketChunk(c2x, c2z, visited);
    } else {
      int steps = Math.max(1, Math.max(Math.abs(c2x - c1x), Math.abs(c2z - c1z)) * 2);
      for(int i = 0; i <= steps; i++) {
        double t = (double) i / (double) steps;
        int cx = ((int) Math.floor(startX + deltaX * t)) >> 4;
        int cz = ((int) Math.floor(startZ + deltaZ * t)) >> 4;
        for(int ox = -1; ox <= 1; ox++) {
          for(int oz = -1; oz <= 1; oz++) {
            ticketChunk(cx + ox, cz + oz, visited);
          }
        }
      }
    }

    if(hidePos1 != null && hidePos2 != null && world.equals(hidePos1.getWorld())) {
      int hMinCx = (Math.min(hidePos1.getBlockX(), hidePos2.getBlockX()) >> 4) - 1;
      int hMaxCx = (Math.max(hidePos1.getBlockX(), hidePos2.getBlockX()) >> 4) + 1;
      int hMinCz = (Math.min(hidePos1.getBlockZ(), hidePos2.getBlockZ()) >> 4) - 1;
      int hMaxCz = (Math.max(hidePos1.getBlockZ(), hidePos2.getBlockZ()) >> 4) + 1;
      if((long) (hMaxCx - hMinCx + 1) * (long) (hMaxCz - hMinCz + 1) <= 64L) {
        for(int cx = hMinCx; cx <= hMaxCx; cx++) {
          for(int cz = hMinCz; cz <= hMaxCz; cz++) {
            ticketChunk(cx, cz, visited);
          }
        }
      }
    }
  }

  private void ticketChunk(int cx, int cz, Set<Long> visited) {
    long key = (((long) cx) << 32) ^ (cz & 0xffffffffL);
    if(visited.add(key)) {
      try {
        Chunk chunk = world.getChunkAt(cx, cz);
        chunk.load(true);
        chunk.addPluginChunkTicket(plugin);
        ticketedChunks.add(chunk);
      } catch(Throwable ignored) {
      }
    }
  }

  private void prepareHiddenArenaBlocksAndEntities(Location hidePos1, Location hidePos2) {
    if(hidePos1 == null || hidePos2 == null || hidePos1.getWorld() == null || !world.equals(hidePos1.getWorld())) {
      return;
    }
    if(hidePos2.getWorld() != null && !world.equals(hidePos2.getWorld())) {
      return;
    }
    int minX = Math.min(hidePos1.getBlockX(), hidePos2.getBlockX());
    int maxX = Math.max(hidePos1.getBlockX(), hidePos2.getBlockX());
    int minY = Math.min(hidePos1.getBlockY(), hidePos2.getBlockY());
    int maxY = Math.max(hidePos1.getBlockY(), hidePos2.getBlockY());
    int minZ = Math.min(hidePos1.getBlockZ(), hidePos2.getBlockZ());
    int maxZ = Math.max(hidePos1.getBlockZ(), hidePos2.getBlockZ());

    long volume = (long) (maxX - minX + 1) * (long) (maxY - minY + 1) * (long) (maxZ - minZ + 1);
    if(volume <= 0 || volume > MAX_HIDE_VOLUME) {
      return;
    }

    double endX = startX + deltaX;
    double endY = startY + deltaY;
    double endZ = startZ + deltaZ;
    int busWheelBaseY = (int) Math.floor(Math.min(startY, endY) + 0.05);

    int effectiveMinY = minY;
    if(busWheelBaseY > effectiveMinY && busWheelBaseY <= maxY) {
      effectiveMinY = busWheelBaseY;
    }
    while(effectiveMinY < maxY && isContinuousGroundFloorLayer(minX, maxX, effectiveMinY, minZ, maxZ)) {
      effectiveMinY++;
    }

    float furnaceSumX = 0.0f;
    float furnaceSumY = 0.0f;
    float furnaceSumZ = 0.0f;
    int furnaceCount = 0;

    BlockData airData = Material.AIR.createBlockData();
    for(int y = effectiveMinY; y <= maxY; y++) {
      for(int z = minZ; z <= maxZ; z++) {
        for(int x = minX; x <= maxX; x++) {
          Block b = world.getBlockAt(x, y, z);
          Material mat = b.getType();
          if(!shouldHideArenaBlock(mat)) {
            continue;
          }
          BlockState real = b.getState();
          BlockState fake = b.getState();
          fake.setBlockData(airData);
          realHiddenStates.add(real);
          fakeAirStates.add(fake);

          if(mat == Material.BLAST_FURNACE) {
            furnaceSumX += (float) ((x + 0.5) - endX);
            furnaceSumY += (float) ((y + 0.5) - endY);
            furnaceSumZ += (float) ((z + 0.5) - endZ);
            furnaceCount++;
          }

          if(!isBuilderSetupNoteSign(real)) {
            BusSchematicLoader.SchematicPart livePart = BusSchematicLoader.createLiveWorldPart(
              b.getBlockData(),
              (float) (x - endX),
              (float) (y - endY),
              (float) (z - endZ)
            );
            if(livePart != null) {
              liveBlockParts.add(livePart);
            }
          }
        }
      }
    }

    if(furnaceCount > 0) {
      double dirLen = Math.hypot(deltaX, deltaZ);
      float fwdX = dirLen > 1e-4 ? (float) (deltaX / dirLen) : 1.0f;
      float fwdZ = dirLen > 1e-4 ? (float) (deltaZ / dirLen) : 0.0f;
      liveHoodOffsetX = (furnaceSumX / furnaceCount) + fwdX * 0.65f;
      liveHoodOffsetY = (furnaceSumY / furnaceCount) + 0.55f;
      liveHoodOffsetZ = (furnaceSumZ / furnaceCount) + fwdZ * 0.65f;
      double halfSpan = Math.max(Math.abs(maxX - minX + 1), Math.abs(maxZ - minZ + 1)) * 0.5;
      liveExhaustOffsetX = -fwdX * (float) (halfSpan + 0.15);
      liveExhaustOffsetY = 0.65f;
      liveExhaustOffsetZ = -fwdZ * (float) (halfSpan + 0.15);
      hasLiveHoodAnchor = true;
    }

    double entMinX = minX - 0.45;
    double entMaxX = maxX + 1.45;
    double entMinY = effectiveMinY - 0.15;
    double entMaxY = maxY + 2.25;
    double entMinZ = minZ - 0.45;
    double entMaxZ = maxZ + 1.45;
    BoundingBox searchBox = new BoundingBox(entMinX, entMinY, entMinZ, entMaxX, entMaxY, entMaxZ);

    Set<UUID> visitedIds = new HashSet<>();
    try {
      for(Entity entity : world.getNearbyEntities(searchBox)) {
        collectBusEntityHierarchy(entity, searchBox, false, visitedIds);
      }
    } catch(Throwable ignored) {
    }

    int minCx = ((int) Math.floor(entMinX)) >> 4;
    int maxCx = ((int) Math.floor(entMaxX)) >> 4;
    int minCz = ((int) Math.floor(entMinZ)) >> 4;
    int maxCz = ((int) Math.floor(entMaxZ)) >> 4;
    for(int cx = minCx; cx <= maxCx; cx++) {
      for(int cz = minCz; cz <= maxCz; cz++) {
        try {
          Chunk chunk = world.getChunkAt(cx, cz);
          for(Entity entity : chunk.getEntities()) {
            collectBusEntityHierarchy(entity, searchBox, false, visitedIds);
          }
        } catch(Throwable ignored) {
        }
      }
    }
  }

  private void collectBusEntityHierarchy(
    Entity entity,
    BoundingBox searchBox,
    boolean parentMatched,
    Set<UUID> visitedIds
  ) {
    if(entity == null || entity instanceof Player || !entity.isValid() || entity.isDead()) {
      return;
    }
    Location loc = entity.getLocation();
    boolean inside = parentMatched || searchBox.contains(loc.getX(), loc.getY(), loc.getZ());
    if(!inside) {
      return;
    }
    if(visitedIds.add(entity.getUniqueId())) {
      hiddenWorldEntities.add(entity);
      boolean pluginManaged = arena != null && arena.isPluginManagedEntity(entity);
      if(entity.isPersistent() && !pluginManaged
        && (entity instanceof Display || entity instanceof ItemFrame)) {
        mapDecorEntitiesToClone.add(entity);
      }
    }
    List<Entity> passengers = entity.getPassengers();
    if(passengers != null && !passengers.isEmpty()) {
      for(Entity passenger : passengers) {
        collectBusEntityHierarchy(passenger, searchBox, true, visitedIds);
      }
    }
  }

  private static boolean shouldHideArenaBlock(Material type) {
    return type != null
      && !type.isAir()
      && type != Material.BARRIER
      && type != Material.LIGHT
      && type != Material.STRUCTURE_VOID
      && type != Material.MOVING_PISTON;
  }

  private boolean isContinuousGroundFloorLayer(int minX, int maxX, int y, int minZ, int maxZ) {
    int totalInside = 0;
    int solidInside = 0;
    for(int z = minZ; z <= maxZ; z++) {
      for(int x = minX; x <= maxX; x++) {
        totalInside++;
        Material mat = world.getBlockAt(x, y, z).getType();
        if(mat.isSolid() && mat.isOccluding()) {
          solidInside++;
        }
      }
    }
    if(totalInside <= 0 || solidInside < totalInside * 0.72) {
      return false;
    }

    int ringTotal = 0;
    int ringGroundMatch = 0;
    for(int x = minX - 1; x <= maxX + 1; x++) {
      ringTotal += 2;
      if(isRoadSurfaceColumn(x, y, minZ - 1)) {
        ringGroundMatch++;
      }
      if(isRoadSurfaceColumn(x, y, maxZ + 1)) {
        ringGroundMatch++;
      }
    }
    for(int z = minZ; z <= maxZ; z++) {
      ringTotal += 2;
      if(isRoadSurfaceColumn(minX - 1, y, z)) {
        ringGroundMatch++;
      }
      if(isRoadSurfaceColumn(maxX + 1, y, z)) {
        ringGroundMatch++;
      }
    }
    return ringTotal > 0 && ringGroundMatch >= ringTotal * 0.55;
  }

  private boolean isRoadSurfaceColumn(int x, int y, int z) {
    Material floor = world.getBlockAt(x, y, z).getType();
    if(!floor.isSolid() || floor == Material.BARRIER) {
      return false;
    }
    Material above = world.getBlockAt(x, y + 1, z).getType();
    return above.isAir() || above == Material.LIGHT || above == Material.BARRIER || !above.isOccluding();
  }

  private static boolean isBuilderSetupNoteSign(BlockState state) {
    if(!(state instanceof Sign)) {
      return false;
    }
    Sign sign = (Sign) state;
    try {
      for(Side side : new Side[]{Side.FRONT, Side.BACK}) {
        String[] lines = sign.getSide(side).getLines();
        if(lines == null) {
          continue;
        }
        for(String line : lines) {
          if(line == null || line.isEmpty()) {
            continue;
          }
          String lower = line.toLowerCase(Locale.ROOT);
          if(lower.contains("task")
            || lower.contains("spawn")
            || lower.contains("waiting")
            || lower.contains("ซ่อม")
            || lower.contains("เช็ค")) {
            return true;
          }
        }
      }
    } catch(Throwable ignored) {
    }
    return false;
  }

  public void syncViewers(Collection<Player> viewers) {
    if(cleanedUp || viewers == null) {
      return;
    }
    TextDisplay hornDisplay = arena != null ? arena.getCampfireDisplay() : null;
    List<Player> newlySynced = new ArrayList<>();
    for(Player viewer : viewers) {
      if(viewer == null || !viewer.isOnline()) {
        continue;
      }
      if(syncedViewers.add(viewer.getUniqueId())) {
        try {
          for(Display d : rigEntities) {
            if(d != null && d.isValid()) {
              viewer.showEntity(plugin, d);
            }
          }
          if(anchor != null && anchor.isValid()) {
            viewer.showEntity(plugin, anchor);
          }
          newlySynced.add(viewer);

          if(hornDisplay != null && hornDisplay.isValid()) {
            viewer.hideEntity(plugin, hornDisplay);
          }

          if(!hiddenWorldEntities.isEmpty()) {
            List<Entity> hiddenForViewer = new ArrayList<>(hiddenWorldEntities.size());
            for(Entity hidden : hiddenWorldEntities) {
              if(hidden == null || !hidden.isValid() || hidden.equals(hornDisplay)) {
                continue;
              }
              boolean wasVisible = true;
              try {
                wasVisible = viewer.canSee(hidden);
              } catch(Throwable ignored) {
              }
              if(wasVisible) {
                hiddenForViewer.add(hidden);
                viewer.hideEntity(plugin, hidden);
              }
            }
            if(!hiddenForViewer.isEmpty()) {
              viewerHiddenEntities.put(viewer.getUniqueId(), hiddenForViewer);
            }
          }

          sendStatesToPlayer(viewer, fakeAirStates);
        } catch(Throwable ignored) {
        }
      }
    }
    if(!newlySynced.isEmpty() && anchor != null && anchor.isValid()) {
      CutsceneManager.syncVehiclePassengers(anchor, newlySynced);
    }
  }

  public void restoreViewer(Player viewer) {
    if(viewer == null) {
      return;
    }
    syncedViewers.remove(viewer.getUniqueId());
    List<Entity> hiddenForViewer = viewerHiddenEntities.remove(viewer.getUniqueId());
    if(!viewer.isOnline()) {
      return;
    }
    try {
      sendStatesToPlayer(viewer, realHiddenStates);
      if(hiddenForViewer != null) {
        for(Entity hidden : hiddenForViewer) {
          if(hidden != null && hidden.isValid()) {
            try {
              viewer.showEntity(plugin, hidden);
            } catch(Throwable ignored) {
            }
          }
        }
      }
      TextDisplay hornDisplay = arena != null ? arena.getCampfireDisplay() : null;
      if(hornDisplay != null && hornDisplay.isValid()) {
        viewer.showEntity(plugin, hornDisplay);
      }
    } catch(Throwable ignored) {
    }
  }

  private static void sendStatesToPlayer(Player player, List<BlockState> states) {
    if(player == null || !player.isOnline() || states.isEmpty()) {
      return;
    }
    try {
      player.sendBlockChanges(states);
    } catch(Throwable fallback) {
      for(BlockState state : states) {
        try {
          player.sendBlockChange(state.getLocation(), state.getBlockData());
        } catch(Throwable ignored) {
        }
      }
    }
  }

  public Entity getAnchor() {
    return anchor;
  }

  public static double computeEffectiveDriveSeconds(double configuredDriveSec, double totalCutsceneSec, boolean hasCustomCameraFrames) {
    double base = configuredDriveSec > 0.5 ? configuredDriveSec : 8.0;
    if(hasCustomCameraFrames && totalCutsceneSec > 1.0) {
      return Math.max(1.2, Math.min(base, totalCutsceneSec * 0.82));
    }
    return Math.max(1.2, base);
  }

  public BusPose computePose(double elapsedSec, double effectiveDriveSec) {
    double drive = Math.max(0.5, effectiveDriveSec);
    double u = Math.max(0.0, Math.min(1.0, elapsedSec / drive));
    double s;
    if(u <= CRUISE_FRACTION) {
      s = CRUISE_SPEED_FACTOR * u;
    } else {
      double span = 1.0 - CRUISE_FRACTION;
      double w = (u - CRUISE_FRACTION) / span;
      s = (CRUISE_SPEED_FACTOR * CRUISE_FRACTION) + CRUISE_SPEED_FACTOR * span * (w - 0.5 * w * w);
    }

    double bobY;
    if(u < CRUISE_FRACTION) {
      bobY = Math.sin(elapsedSec * 15.0) * 0.022 + Math.sin(elapsedSec * 6.7) * 0.014;
    } else if(u < 1.0) {
      double damp = 1.0 - ((u - CRUISE_FRACTION) / (1.0 - CRUISE_FRACTION));
      bobY = (Math.sin(elapsedSec * 42.0) * 0.075 + Math.cos(elapsedSec * 23.0) * 0.035) * damp;
    } else {
      bobY = 0.0;
    }

    return new BusPose(
      startX + deltaX * s,
      startY + deltaY * s + bobY,
      startZ + deltaZ * s,
      u
    );
  }

  public void tickMainThread(
    double elapsedSec,
    double effectiveDriveSec,
    List<Player> viewers,
    ServerPositionUpdater serverPosUpdater
  ) {
    if(cleanedUp || anchor == null || !anchor.isValid()) {
      return;
    }
    tickCounter++;
    syncViewers(viewers);

    BusPose pose = computePose(elapsedSec, effectiveDriveSec);
    if(serverPosUpdater != null) {
      serverPosUpdater.update(anchor, pose.x, pose.y, pose.z);
    }
    if(viewers != null && !viewers.isEmpty()
      && (tickCounter == 1 || tickCounter == 3 || tickCounter == 10)) {
      CutsceneManager.syncVehiclePassengers(anchor, viewers);
    }

    if(viewers == null || viewers.isEmpty()) {
      return;
    }

    Location hoodLoc;
    Location exhaustLoc;
    if(usingLiveWorldBlocks && hasLiveHoodAnchor) {
      hoodLoc = new Location(world, pose.x + liveHoodOffsetX, pose.y + liveHoodOffsetY, pose.z + liveHoodOffsetZ);
      exhaustLoc = new Location(world, pose.x + liveExhaustOffsetX, pose.y + liveExhaustOffsetY, pose.z + liveExhaustOffsetZ);
    } else if(schematic != null) {
      hoodLoc = localToWorld(pose.x, pose.y, pose.z, schematic.getHoodLocalX(), schematic.getHoodLocalY(), schematic.getHoodLocalZ());
      exhaustLoc = localToWorld(pose.x, pose.y, pose.z, schematic.getExhaustLocalX(), schematic.getExhaustLocalY(), schematic.getExhaustLocalZ());
    } else {
      hoodLoc = new Location(world, pose.x, pose.y + 1.6, pose.z);
      exhaustLoc = new Location(world, pose.x, pose.y + 0.65, pose.z);
    }

    double u = pose.progress;
    if(u < 0.96 && tickCounter % 8 == 1) {
      float pitch = u < CRUISE_FRACTION ? 0.58f : 0.48f;
      playSpatialSound(viewers, hoodLoc, Sound.ENTITY_MINECART_RIDING, 0.35f, pitch);
      playSpatialSound(viewers, hoodLoc, Sound.BLOCK_NOTE_BLOCK_BASEDRUM, 0.45f, 0.52f);
    }

    if(u < 0.98 && tickCounter % 2 == 0) {
      spawnViewerParticle(viewers, PARTICLE_CAMPFIRE_SMOKE, exhaustLoc, 1, 0.06, 0.04, 0.06, 0.01);
    }

    if(!misfireOnePlayed && u >= CRUISE_FRACTION) {
      misfireOnePlayed = true;
      playSpatialSound(viewers, hoodLoc, Sound.ENTITY_ZOMBIE_ATTACK_IRON_DOOR, 0.9f, 0.65f);
      playSpatialSound(viewers, hoodLoc, Sound.ENTITY_FIREWORK_ROCKET_BLAST, 0.85f, 0.55f);
      spawnViewerParticle(viewers, PARTICLE_LAVA, hoodLoc, 6, 0.22, 0.15, 0.22, 0.03);
      spawnViewerParticle(viewers, PARTICLE_FLAME, hoodLoc, 10, 0.22, 0.15, 0.22, 0.04);
      spawnViewerParticle(viewers, PARTICLE_LARGE_SMOKE, hoodLoc, 12, 0.28, 0.2, 0.28, 0.03);
    }

    if(!misfireTwoPlayed && u >= 0.80) {
      misfireTwoPlayed = true;
      playSpatialSound(viewers, hoodLoc, Sound.BLOCK_ANVIL_PLACE, 0.75f, 0.55f);
      playSpatialSound(viewers, hoodLoc, Sound.BLOCK_FIRE_EXTINGUISH, 0.85f, 0.85f);
      spawnViewerParticle(viewers, PARTICLE_LAVA, hoodLoc, 8, 0.25, 0.18, 0.25, 0.04);
      spawnViewerParticle(viewers, PARTICLE_LARGE_SMOKE, hoodLoc, 16, 0.32, 0.25, 0.32, 0.03);
      spawnViewerParticle(viewers, PARTICLE_CLOUD, hoodLoc, 8, 0.25, 0.2, 0.25, 0.02);
    }

    if(!breakdownBlastPlayed && u >= 0.96) {
      breakdownBlastPlayed = true;
      playSpatialSound(viewers, hoodLoc, Sound.ENTITY_GENERIC_EXPLODE, 0.85f, 0.82f);
      playSpatialSound(viewers, hoodLoc, Sound.BLOCK_ANVIL_LAND, 0.65f, 0.55f);
      playSpatialSound(viewers, hoodLoc, Sound.BLOCK_FIRE_EXTINGUISH, 1.0f, 0.65f);
      spawnViewerParticle(viewers, PARTICLE_CAMPFIRE_SMOKE, hoodLoc, 18, 0.35, 0.35, 0.35, 0.03);
      spawnViewerParticle(viewers, PARTICLE_LARGE_SMOKE, hoodLoc, 24, 0.4, 0.35, 0.4, 0.04);
      spawnViewerParticle(viewers, PARTICLE_CLOUD, hoodLoc, 18, 0.35, 0.28, 0.35, 0.03);
      spawnViewerParticle(viewers, PARTICLE_LAVA, hoodLoc, 12, 0.3, 0.22, 0.3, 0.05);
    }

    if(u >= 0.70 && tickCounter % 2 == 0) {
      spawnViewerParticle(viewers, PARTICLE_CAMPFIRE_SMOKE, hoodLoc, 2, 0.22, 0.18, 0.22, 0.025);
      spawnViewerParticle(viewers, PARTICLE_LARGE_SMOKE, hoodLoc, 3, 0.25, 0.2, 0.25, 0.02);
      spawnViewerParticle(viewers, PARTICLE_CLOUD, hoodLoc, 1, 0.2, 0.15, 0.2, 0.015);
      if(tickCounter % 8 == 0) {
        spawnViewerParticle(viewers, PARTICLE_LAVA, hoodLoc, 2, 0.18, 0.12, 0.18, 0.02);
      }
    }

    if(u >= 1.0 && tickCounter % 24 == 0) {
      playSpatialSound(viewers, hoodLoc, Sound.BLOCK_FIRE_EXTINGUISH, 0.35f, 0.72f);
    }
  }

  public Location localToWorld(double baseX, double baseY, double baseZ, float lx, float ly, float lz) {
    Vector3f v = busRotation.transform(new Vector3f(lx, ly, lz));
    return new Location(world, baseX + v.x, baseY + v.y, baseZ + v.z);
  }

  private static void playSpatialSound(List<Player> viewers, Location loc, Sound sound, float volume, float pitch) {
    if(loc == null || sound == null) {
      return;
    }
    for(Player viewer : viewers) {
      if(viewer != null && viewer.isOnline() && viewer.getWorld().equals(loc.getWorld())) {
        try {
          viewer.playSound(loc, sound, volume, pitch);
        } catch(Throwable ignored) {
        }
      }
    }
  }

  private static void spawnViewerParticle(
    List<Player> viewers,
    Particle particle,
    Location loc,
    int count,
    double ox,
    double oy,
    double oz,
    double extra
  ) {
    if(particle == null || loc == null) {
      return;
    }
    for(Player viewer : viewers) {
      if(viewer != null && viewer.isOnline() && viewer.getWorld().equals(loc.getWorld())) {
        try {
          viewer.spawnParticle(particle, loc, count, ox, oy, oz, extra);
        } catch(Throwable ignored) {
        }
      }
    }
  }

  public void cleanup(Collection<Player> extraViewers) {
    if(cleanedUp) {
      return;
    }
    cleanedUp = true;

    Set<Player> toRestore = new HashSet<>();
    if(extraViewers != null) {
      for(Player p : extraViewers) {
        if(p != null) {
          toRestore.add(p);
        }
      }
    }
    for(UUID uuid : syncedViewers) {
      Player p = org.bukkit.Bukkit.getPlayer(uuid);
      if(p != null) {
        toRestore.add(p);
      }
    }
    for(UUID uuid : viewerHiddenEntities.keySet()) {
      Player p = org.bukkit.Bukkit.getPlayer(uuid);
      if(p != null) {
        toRestore.add(p);
      }
    }
    syncedViewers.clear();

    for(Player viewer : toRestore) {
      restoreViewer(viewer);
    }
    viewerHiddenEntities.clear();

    for(Display d : rigEntities) {
      if(d != null) {
        try {
          d.remove();
        } catch(Throwable ignored) {
        }
      }
    }
    rigEntities.clear();

    if(anchor != null) {
      try {
        anchor.remove();
      } catch(Throwable ignored) {
      }
      anchor = null;
    }

    for(Chunk chunk : ticketedChunks) {
      if(chunk != null) {
        try {
          chunk.removePluginChunkTicket(plugin);
        } catch(Throwable ignored) {
        }
      }
    }
    ticketedChunks.clear();
    realHiddenStates.clear();
    fakeAirStates.clear();
    liveBlockParts.clear();
    hiddenWorldEntities.clear();
    mapDecorEntitiesToClone.clear();
  }

  private static Particle resolveParticle(String modern, String legacy) {
    try {
      return Particle.valueOf(modern);
    } catch(IllegalArgumentException ex) {
      try {
        return Particle.valueOf(legacy);
      } catch(IllegalArgumentException ignored) {
        return null;
      }
    }
  }

  public static final class BusPose {
    public final double x;
    public final double y;
    public final double z;
    public final double progress;

    public BusPose(double x, double y, double z, double progress) {
      this.x = x;
      this.y = y;
      this.z = z;
      this.progress = progress;
    }
  }
}
