package com.sammy.ortus.helpers;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Parrot;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.pathfinder.Node;
import net.minecraft.world.level.pathfinder.Path;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.function.Predicate;

/**
 * A collection of various helper methods related to all your blocky needs.
 */
public class BlockHelper {

    /**
     * Copies all properties from oldState to newState, given that an individual property exists on the newState.
     *
     * @param oldState - State to copy properties from.
     * @param newState - State to apply copied property values to.
     * @return - NewState with adjusted properties.
     */
    public static BlockState getBlockStateWithExistingProperties(BlockState oldState, BlockState newState) {
        BlockState finalState = newState;
        for (Property<?> property : oldState.getProperties()) {
            if (newState.hasProperty(property)) {
                finalState = newStateWithOldProperty(oldState, finalState, property);
            }
        }
        return finalState;
    }

    /**
     * Copies BlockState properties from a BlockState already in the world, and replaces it with a newState with matching property values.
     */
    public static BlockState setBlockStateWithExistingProperties(Level level, BlockPos pos, BlockState newState, int flags) {
        BlockState oldState = level.getBlockState(pos);
        BlockState finalState = getBlockStateWithExistingProperties(oldState, newState);
        level.sendBlockUpdated(pos, oldState, finalState, flags);
        level.setBlock(pos, finalState, flags);
        return finalState;
    }

    public static <T extends Comparable<T>> BlockState newStateWithOldProperty(BlockState oldState, BlockState newState, Property<T> property) {
        return newState.setValue(property, oldState.getValue(property));
    }

    /**
     * Saves a block position to nbt.
     */
    public static void saveBlockPos(CompoundTag compoundNBT, BlockPos pos) {
        compoundNBT.putInt("X", pos.getX());
        compoundNBT.putInt("Y", pos.getY());
        compoundNBT.putInt("Z", pos.getZ());
    }

    /**
     * Saves a block position to nbt with extra text to differentiate it.
     */
    public static void saveBlockPos(CompoundTag compoundNBT, BlockPos pos, String extra) {
        compoundNBT.putInt(extra + "_X", pos.getX());
        compoundNBT.putInt(extra + "_Y", pos.getY());
        compoundNBT.putInt(extra + "_Z", pos.getZ());
    }

    /**
     * Loads a block position from nbt.
     */
    public static BlockPos loadBlockPos(CompoundTag tag) {
        return tag.contains("X") ? new BlockPos(tag.getInt("X"), tag.getInt("Y"), tag.getInt("Z")) : null;
    }

    /**
     * Loads a block position from nbt with extra text as input.
     */
    public static BlockPos loadBlockPos(CompoundTag tag, String extra) {
        return tag.contains(extra + "_X") ? new BlockPos(tag.getInt(extra + "_X"), tag.getInt(extra + "_Y"), tag.getInt(extra + "_Z")) : null;
    }

    /**
     * Returns a list of block entities within a range, with a predicate for conditional checks.
     */
    public static <T> ArrayList<T> getBlockEntities(Class<T> type, Level level, BlockPos pos, int range, Predicate<T> predicate) {
        return getBlockEntities(type, level, pos, range, range, range, predicate);
    }

    /**
     * Returns a list of block entities within an XZ range, with a predicate for conditional checks.
     */
    public static <T> ArrayList<T> getBlockEntities(Class<T> type, Level level, BlockPos pos, int x, int z, Predicate<T> predicate) {
        ArrayList<T> blockEntities = getBlockEntities(type, level, pos, x, z);
        blockEntities.removeIf(b -> !predicate.test(b));
        return blockEntities;
    }

    /**
     * Returns a list of block entities within an XYZ range, with a predicate for conditional checks.
     */
    public static <T> ArrayList<T> getBlockEntities(Class<T> type, Level level, BlockPos pos, int x, int y, int z, Predicate<T> predicate) {
        ArrayList<T> blockEntities = getBlockEntities(type, level, pos, x, y, z);
        blockEntities.removeIf(b -> !predicate.test(b));
        return blockEntities;
    }

    /**
     * Returns a list of block entities within a radius around a position.
     */
    public static <T> ArrayList<T> getBlockEntities(Class<T> type, Level level, BlockPos pos, int range) {
        return getBlockEntities(type, level, pos, range, range, range);
    }

    /**
     * Returns a list of block entities within an XZ radius around a position.
     */
    public static <T> ArrayList<T> getBlockEntities(Class<T> type, Level level, BlockPos pos, int x, int z) {
        return getBlockEntities(type, level, new AABB(pos.getX() - x, pos.getY(), pos.getZ() - z, pos.getX() + x, pos.getY() + 1, pos.getZ() + z));
    }

    /**
     * Returns a list of block entities within an XYZ radius around a position.
     */
    public static <T> ArrayList<T> getBlockEntities(Class<T> type, Level level, BlockPos pos, int x, int y, int z) {
        return getBlockEntities(type, level, pos, -x, -y, -z, x, y, z);
    }

    /**
     * Returns a list of block entities within set coordinates.
     */
    public static <T> ArrayList<T> getBlockEntities(Class<T> type, Level level, BlockPos pos, int x1, int y1, int z1, int x2, int y2, int z2) {
        return getBlockEntities(type, level, new AABB(pos.getX() + x1, pos.getY() + y1, pos.getZ() + z1, pos.getX() + x2, pos.getY() + y2, pos.getZ() + z2));
    }

    /**
     * Returns a list of block entities within an AABB.
     */
    public static <T> ArrayList<T> getBlockEntities(Class<T> type, Level world, AABB bb) {
        ArrayList<T> tileList = new ArrayList<>();
        for (int i = (int) Math.floor(bb.minX); i < (int) Math.ceil(bb.maxX) + 16; i += 16) {
            for (int j = (int) Math.floor(bb.minZ); j < (int) Math.ceil(bb.maxZ) + 16; j += 16) {
                ChunkAccess c = world.getChunk(new BlockPos(i, 0, j));
                Set<BlockPos> tiles = c.getBlockEntitiesPos();
                for (BlockPos p : tiles)
                    if (bb.contains(p.getX() + 0.5, p.getY() + 0.5, p.getZ() + 0.5)) {
                        BlockEntity t = world.getBlockEntity(p);
                        if (type.isInstance(t)) {
                            tileList.add((T) t);
                        }
                    }
            }
        }
        return tileList;
    }

    /**
     * Returns a list of block positions within a radius around a position, with a predicate for conditional checks.
     */
    public static ArrayList<BlockPos> getBlocks(BlockPos pos, int range, Predicate<BlockPos> predicate) {
        return getBlocks(pos, range, range, range, predicate);
    }

    /**
     * Returns a list of block positions within a XYZ radius around a position, with a predicate for conditional checks.
     */
    public static ArrayList<BlockPos> getBlocks(BlockPos pos, int x, int y, int z, Predicate<BlockPos> predicate) {
        ArrayList<BlockPos> blocks = getBlocks(pos, x, y, z);
        blocks.removeIf(b -> !predicate.test(b));
        return blocks;
    }

    /**
     * Returns a list of block positions within a XYZ radius around a position.
     */
    public static ArrayList<BlockPos> getBlocks(BlockPos pos, int x, int y, int z) {
        return getBlocks(pos, -x, -y, -z, x, y, z);
    }

    /**
     * Returns a list of block positions within set coordinates.
     */
    public static ArrayList<BlockPos> getBlocks(BlockPos pos, int x1, int y1, int z1, int x2, int y2, int z2) {
        ArrayList<BlockPos> positions = new ArrayList<>();
        for (int x = x1; x <= x2; x++) {
            for (int y = y1; y <= y2; y++) {
                for (int z = z1; z <= z2; z++) {
                    positions.add(pos.offset(x, y, z));
                }
            }
        }
        return positions;
    }

    public static ArrayList<BlockPos> getPlaneOfBlocks(BlockPos pos, int range, Predicate<BlockPos> predicate) {
        return getPlaneOfBlocks(pos, range, range, predicate);
    }

    public static ArrayList<BlockPos> getPlaneOfBlocks(BlockPos pos, int x, int z, Predicate<BlockPos> predicate) {
        ArrayList<BlockPos> blocks = getPlaneOfBlocks(pos, x, z);
        blocks.removeIf(b -> !predicate.test(b));
        return blocks;
    }

    public static ArrayList<BlockPos> getPlaneOfBlocks(BlockPos pos, int x, int z) {
        return getPlaneOfBlocks(pos, -x, -z, x, z);
    }

    public static ArrayList<BlockPos> getPlaneOfBlocks(BlockPos pos, int x1, int z1, int x2, int z2) {
        ArrayList<BlockPos> positions = new ArrayList<>();
        for (int x = x1; x <= x2; x++) {
            for (int z = z1; z <= z2; z++) {
                positions.add(new BlockPos(pos.getX() + x, pos.getY(), pos.getZ() + z));
            }
        }
        return positions;
    }

    public static ArrayList<BlockPos> getSphereOfBlocks(BlockPos pos, float range, Predicate<BlockPos> predicate) {
        ArrayList<BlockPos> positions = getSphereOfBlocks(pos, range, range);
        positions.removeIf(b -> !predicate.test(b));
        return positions;
    }

    public static ArrayList<BlockPos> getSphereOfBlocks(BlockPos pos, float width, float height, Predicate<BlockPos> predicate) {
        ArrayList<BlockPos> positions = getSphereOfBlocks(pos, width, height);
        positions.removeIf(b -> !predicate.test(b));
        return positions;
    }

    public static ArrayList<BlockPos> getSphereOfBlocks(BlockPos pos, float range) {
        return getSphereOfBlocks(pos, range, range);
    }

    public static ArrayList<BlockPos> getSphereOfBlocks(BlockPos pos, float width, float height) {
        ArrayList<BlockPos> positions = new ArrayList<>();
        for (int x = (int) -width; x <= width; x++) {
            for (int y = (int) -height; y <= height; y++) {
                for (int z = (int) -width; z <= width; z++) {
                    if (x * x + y * y + z * z < width * width) {
                        positions.add(pos.offset(x, y, z));
                    }
                }
            }
        }
        return positions;
    }

    /**
     * Quick method to get all blocks neighboring a block.
     */
    public static ArrayList<BlockPos> getNeighboringBlocks(BlockPos current) {
        return getBlocks(current, -1, -1, -1, 1, 1, 1);
    }

    /**
     * Returns a set of block positions within a radius around a position, with a predicate for conditional checks.
     */
    public static HashSet<BlockPos> getBlockSet(BlockPos pos, int range, Predicate<BlockPos> predicate) {
        return getBlockSet(pos, range, range, range, predicate);
    }

    /**
     * Returns a set of block positions within a XYZ radius around a position, with a predicate for conditional checks.
     */
    public static HashSet<BlockPos> getBlockSet(BlockPos pos, int x, int y, int z, Predicate<BlockPos> predicate) {
        HashSet<BlockPos> blocks = getBlockSet(pos, x, y, z);
        blocks.removeIf(b -> !predicate.test(b));
        return blocks;
    }

    /**
     * Returns a set of block positions within a XYZ radius around a position.
     */
    public static HashSet<BlockPos> getBlockSet(BlockPos pos, int x, int y, int z) {
        return getBlockSet(pos, -x, -y, -z, x, y, z);
    }

    /**
     * Returns a set of block positions within set coordinates.
     */
    public static HashSet<BlockPos> getBlockSet(BlockPos pos, int x1, int y1, int z1, int x2, int y2, int z2) {
        HashSet<BlockPos> positions = new HashSet<>();
        for (int x = x1; x <= x2; x++) {
            for (int y = y1; y <= y2; y++) {
                for (int z = z1; z <= z2; z++) {
                    positions.add(pos.offset(x, y, z));
                }
            }
        }
        return positions;
    }

    public static HashSet<BlockPos> getPlaneOfBlocksAsSet(BlockPos pos, int range, Predicate<BlockPos> predicate) {
        return getPlaneOfBlocksAsSet(pos, range, range, predicate);
    }

    public static HashSet<BlockPos> getPlaneOfBlocksAsSet(BlockPos pos, int x, int z, Predicate<BlockPos> predicate) {
        HashSet<BlockPos> blocks = getPlaneOfBlocksAsSet(pos, x, z);
        blocks.removeIf(b -> !predicate.test(b));
        return blocks;
    }

    public static HashSet<BlockPos> getPlaneOfBlocksAsSet(BlockPos pos, int x, int z) {
        return getPlaneOfBlocksAsSet(pos, -x, -z, x, z);
    }

    public static HashSet<BlockPos> getPlaneOfBlocksAsSet(BlockPos pos, int x1, int z1, int x2, int z2) {
        HashSet<BlockPos> positions = new HashSet<>();
        for (int x = x1; x <= x2; x++) {
            for (int z = z1; z <= z2; z++) {
                positions.add(new BlockPos(pos.getX() + x, pos.getY(), pos.getZ() + z));
            }
        }
        return positions;
    }

    public static HashSet<BlockPos> getSphereOfBlocksAsSet(BlockPos pos, float range, Predicate<BlockPos> predicate) {
        HashSet<BlockPos> positions = getSphereOfBlocksAsSet(pos, range, range);
        positions.removeIf(b -> !predicate.test(b));
        return positions;
    }

    public static HashSet<BlockPos> getSphereOfBlocksAsSet(BlockPos pos, float width, float height, Predicate<BlockPos> predicate) {
        HashSet<BlockPos> positions = getSphereOfBlocksAsSet(pos, width, height);
        positions.removeIf(b -> !predicate.test(b));
        return positions;
    }

    public static HashSet<BlockPos> getSphereOfBlocksAsSet(BlockPos pos, float range) {
        return getSphereOfBlocksAsSet(pos, range, range);
    }

    public static HashSet<BlockPos> getSphereOfBlocksAsSet(BlockPos pos, float width, float height) {
        HashSet<BlockPos> positions = new HashSet<>();
        for (int x = (int) -width; x <= width; x++) {
            for (int y = (int) -height; y <= height; y++) {
                for (int z = (int) -width; z <= width; z++) {
                    if (x * x + y * y + z * z < width * width) {
                        positions.add(pos.offset(x, y, z));
                    }
                }
            }
        }
        return positions;
    }

    /**
     * Quick method to get all blocks neighboring a block.
     */
    public static HashSet<BlockPos> getNeighboringBlockSet(BlockPos current) {
        return getBlockSet(current, -1, -1, -1, 1, 1, 1);
    }
    /* Javadoc
    * @param inclusive Whether to include the start and the end pos itself in the list.
    * */
    public static ArrayList<BlockPos> getPath(BlockPos start, BlockPos end, int speed, boolean inclusive, Level level){
        Parrot parrot = new Parrot(EntityType.PARROT, level);
        parrot.setPos(start.getX() + 0.5, start.getY() - 0.5, start.getZ() + 0.5);
        parrot.getNavigation().moveTo(end.getX() + 0.5, end.getY() - 0.5, end.getZ() + 0.5, speed);
        Path path = parrot.getNavigation().getPath();
        parrot.discard();
        int nodes = path.getNodeCount();
        ArrayList<BlockPos> positions = new ArrayList<>();
        for (int i = 0; i < nodes; i++) {
            Node node = path.getNode(i);
            positions.add(new BlockPos(node.x, node.y-0.5, node.z));
        }
        if(!inclusive){
            positions.remove(0);
            positions.remove(positions.size() - 1);
        }
        return positions;
    }

    public static void updateState(Level level, BlockPos pos) {
        updateState(level.getBlockState(pos), level, pos);
    }

    public static void updateState(BlockState state, Level level, BlockPos pos) {
        level.sendBlockUpdated(pos, state, state, 2);
        level.blockEntityChanged(pos);
    }

    public static void updateAndNotifyState(Level level, BlockPos pos) {
        updateAndNotifyState(level.getBlockState(pos), level, pos);
    }

    public static void updateAndNotifyState(BlockState state, Level level, BlockPos pos) {
        updateState(state, level, pos);
        state.updateNeighbourShapes(level, pos, 2);
        level.updateNeighbourForOutputSignal(pos, state.getBlock());
    }


    /**
     * Converts a block position into a Vec3 entry.
     * @param pos the block position
     * @return the vec3 representation.
     */
    public static Vec3 fromBlockPos(BlockPos pos) {
        return new Vec3(pos.getX(), pos.getY(), pos.getZ());
    }

    /**
     * Generates a random block position
     * @param pos the position the block is centered around
     * @param min the minimum distance from the center
     * @param max the maximum distance from the distance
     * @return The new block position
     */
    public static Vec3 randPos(BlockPos pos, double min, double max) {
        Random rand = new Random();
        double x = Mth.nextDouble(rand, min, max) + pos.getX();
        double y = Mth.nextDouble(rand, min, max) + pos.getY();
        double z = Mth.nextDouble(rand, min, max) + pos.getZ();
        return new Vec3(x, y, z);
    }
}