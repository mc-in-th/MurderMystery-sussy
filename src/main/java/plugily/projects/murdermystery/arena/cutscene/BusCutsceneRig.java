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
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.TextDisplay;
import org.bukkit.util.Transformation;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import plugily.projects.minigamesbox.classic.PluginMain;
import plugily.projects.murdermystery.arena.Arena;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class BusCutsceneRig {

  private static final int MAX_HIDE_VOLUME = 25000;
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
  private final List<BlockDisplay> partEntities = new ArrayList<>();
  private final List<Chunk> ticketedChunks = new ArrayList<>();
  private final List<BlockState> realHiddenStates = new ArrayList<>();
  private final List<BlockState> fakeAirStates = new ArrayList<>();
  private final Set<UUID> syncedViewers = ConcurrentHashMap.newKeySet();

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
    if(loaded == null || loaded.getParts().isEmpty()) {
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
    preloadRoadChunks();
    prepareHiddenArenaBlocks(hidePos1, hidePos2);

    Location spawnOrigin = new Location(world, startX, startY, startZ, 0.0f, 0.0f);
    try {
      anchor = world.spawn(spawnOrigin, TextDisplay.class, entity -> {
        entity.setText("");
        entity.setGravity(false);
        entity.setPersistent(false);
        entity.setTeleportDuration(1);
        entity.setInterpolationDuration(1);
        entity.setViewRange(0.0f);
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
          partEntities.add(bd);
          anchor.addPassenger(bd);
        }
      } catch(Throwable ignored) {
      }
    }

    if(partEntities.isEmpty()) {
      return false;
    }

    syncViewers(initialViewers);
    return true;
  }

  private void preloadRoadChunks() {
    int c1x = ((int) Math.floor(startX)) >> 4;
    int c1z = ((int) Math.floor(startZ)) >> 4;
    int c2x = ((int) Math.floor(startX + deltaX)) >> 4;
    int c2z = ((int) Math.floor(startZ + deltaZ)) >> 4;
    int minCx = Math.min(c1x, c2x) - 1;
    int maxCx = Math.max(c1x, c2x) + 1;
    int minCz = Math.min(c1z, c2z) - 1;
    int maxCz = Math.max(c1z, c2z) + 1;

    if((long) (maxCx - minCx + 1) * (long) (maxCz - minCz + 1) > 256L) {
      Chunk startChunk = world.getChunkAt(c1x, c1z);
      startChunk.load(true);
      startChunk.addPluginChunkTicket(plugin);
      ticketedChunks.add(startChunk);
      Chunk endChunk = world.getChunkAt(c2x, c2z);
      endChunk.load(true);
      endChunk.addPluginChunkTicket(plugin);
      ticketedChunks.add(endChunk);
      return;
    }

    Set<Long> visited = new HashSet<>();
    int steps = Math.max(1, Math.max(Math.abs(c2x - c1x), Math.abs(c2z - c1z)) * 2);
    for(int i = 0; i <= steps; i++) {
      double t = (double) i / (double) steps;
      int cx = ((int) Math.floor(startX + deltaX * t)) >> 4;
      int cz = ((int) Math.floor(startZ + deltaZ * t)) >> 4;
      for(int ox = -1; ox <= 1; ox++) {
        for(int oz = -1; oz <= 1; oz++) {
          int tx = cx + ox;
          int tz = cz + oz;
          long key = (((long) tx) << 32) ^ (tz & 0xffffffffL);
          if(visited.add(key)) {
            try {
              Chunk chunk = world.getChunkAt(tx, tz);
              chunk.load(true);
              chunk.addPluginChunkTicket(plugin);
              ticketedChunks.add(chunk);
            } catch(Throwable ignored) {
            }
          }
        }
      }
    }
  }

  private void prepareHiddenArenaBlocks(Location hidePos1, Location hidePos2) {
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

    BlockData airData = Material.AIR.createBlockData();
    for(int y = minY; y <= maxY; y++) {
      for(int z = minZ; z <= maxZ; z++) {
        for(int x = minX; x <= maxX; x++) {
          Block b = world.getBlockAt(x, y, z);
          if(!b.getType().isAir()) {
            BlockState real = b.getState();
            BlockState fake = b.getState();
            fake.setBlockData(airData);
            realHiddenStates.add(real);
            fakeAirStates.add(fake);
          }
        }
      }
    }
  }

  public void syncViewers(Collection<Player> viewers) {
    if(cleanedUp || viewers == null) {
      return;
    }
    TextDisplay hornDisplay = arena != null ? arena.getCampfireDisplay() : null;
    for(Player viewer : viewers) {
      if(viewer == null || !viewer.isOnline()) {
        continue;
      }
      if(syncedViewers.add(viewer.getUniqueId())) {
        try {
          if(anchor != null && anchor.isValid()) {
            viewer.showEntity(plugin, anchor);
          }
          for(BlockDisplay bd : partEntities) {
            if(bd != null && bd.isValid()) {
              viewer.showEntity(plugin, bd);
            }
          }
          if(hornDisplay != null && hornDisplay.isValid()) {
            viewer.hideEntity(plugin, hornDisplay);
          }
          sendStatesToPlayer(viewer, fakeAirStates);
        } catch(Throwable ignored) {
        }
      }
    }
  }

  public void restoreViewer(Player viewer) {
    if(viewer == null) {
      return;
    }
    syncedViewers.remove(viewer.getUniqueId());
    if(!viewer.isOnline()) {
      return;
    }
    try {
      sendStatesToPlayer(viewer, realHiddenStates);
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

    if(viewers == null || viewers.isEmpty()) {
      return;
    }

    Location hoodLoc = localToWorld(pose.x, pose.y, pose.z, schematic.getHoodLocalX(), schematic.getHoodLocalY(), schematic.getHoodLocalZ());
    Location exhaustLoc = localToWorld(pose.x, pose.y, pose.z, schematic.getExhaustLocalX(), schematic.getExhaustLocalY(), schematic.getExhaustLocalZ());

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
    syncedViewers.clear();

    for(Player viewer : toRestore) {
      restoreViewer(viewer);
    }

    for(BlockDisplay bd : partEntities) {
      if(bd != null) {
        try {
          bd.remove();
        } catch(Throwable ignored) {
        }
      }
    }
    partEntities.clear();

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
