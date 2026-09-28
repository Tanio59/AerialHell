package fr.factionbedrock.aerialhell.Util;

import fr.factionbedrock.aerialhell.World.Structure.AbstractAerialHellStructure;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.material.MapColor;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class FeatureHelper
{
    //true if a piece of an Aerial Hell structure is closer than the given distances to the feature origin
    public static boolean isFeatureGeneratingNextToStructure(FeaturePlaceContext<?> context, int horizontalDistance, int verticalDistance)
    {
        //features placed outside of world generation (saplings, bone meal) are not restricted
        if (!(context.level() instanceof WorldGenRegion region)) {return false;}

        BlockPos origin = context.origin();
        //horizontalDistance must be <= 16 : only the structure references of the chunks next to the feature chunk are available
        BoundingBox area = new BoundingBox(origin.getX() - horizontalDistance, origin.getY() - verticalDistance, origin.getZ() - horizontalDistance, origin.getX() + horizontalDistance, origin.getY() + verticalDistance, origin.getZ() + horizontalDistance);
        return !getAerialHellStructurePieceBoxes(region, area).isEmpty();
    }

    /**
     * @return the bounding boxes of the Aerial Hell structure pieces intersecting the area.
     * Only works during the features generation step, and for an area contained in the chunks next to the region center.
     */
    public static List<BoundingBox> getAerialHellStructurePieceBoxes(WorldGenRegion region, BoundingBox area)
    {
        List<BoundingBox> boxes = new ArrayList<>();
        Set<StructureStart> checkedStarts = Collections.newSetFromMap(new IdentityHashMap<>());
        ChunkPos center = region.getCenter();

        int minChunkX = Math.max(SectionPos.blockToSectionCoord(area.minX()), center.x() - 1), maxChunkX = Math.min(SectionPos.blockToSectionCoord(area.maxX()), center.x() + 1);
        int minChunkZ = Math.max(SectionPos.blockToSectionCoord(area.minZ()), center.z() - 1), maxChunkZ = Math.min(SectionPos.blockToSectionCoord(area.maxZ()), center.z() + 1);
        for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++)
        {
            for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++)
            {
                //references of a chunk : positions of the starts of structures having pieces in this chunk
                for (Map.Entry<Structure, LongSet> entry : region.getChunk(chunkX, chunkZ).getAllReferences().entrySet())
                {
                    if (!(entry.getKey() instanceof AbstractAerialHellStructure)) {continue;}
                    for (long startChunkPos : entry.getValue())
                    {
                        int startChunkX = ChunkPos.getX(startChunkPos), startChunkZ = ChunkPos.getZ(startChunkPos);
                        //structure starts are only available up to 8 chunks from the region center
                        if (center.getChessboardDistance(startChunkX, startChunkZ) > 8) {continue;}

                        StructureStart start = region.getChunk(startChunkX, startChunkZ).getStartForStructure(entry.getKey());
                        if (start == null || !start.isValid() || !checkedStarts.add(start)) {continue;}
                        for (StructurePiece piece : start.getPieces())
                        {
                            if (piece.getBoundingBox().intersects(area)) {boxes.add(piece.getBoundingBox());}
                        }
                    }
                }
            }
        }
        return boxes;
    }

    public static boolean isShadowBiome(Biome biome)
    {
        //Identifier shadowPlain = AerialHellBiomes.SHADOW_PLAIN.location();
        //Identifier shadowForest = AerialHellBiomes.SHADOW_FOREST.location();
        // Used to test getNoiseBiome() method in isFeatureChunk structure gen condition. This method doesn't return the right biome : do not use isShadowBiome(Biome biome) in isFeatureChunk context.
        // if (!(biome.getRegistryName() != null && (biome.getRegistryName().equals(shadowPlain) || biome.getRegistryName().equals(shadowForest)))) {System.out.println("not shadow biome detected : registry name =  "+biome.getRegistryName());}
        return false;//biome.getRegistryName() != null && (biome.getRegistryName().equals(shadowPlain) || biome.getRegistryName().equals(shadowForest)); //TODO
    }

    public static boolean isReplaceableByLogOrLeavesFeature(LevelAccessor level, BlockPos pos, boolean canReplacePlant)
    {
        return level.isStateAtPosition(pos, (state) ->
        {
            return state.canBeReplaced() || canReplacePlant && state.getMapColor(level, pos) == MapColor.PLANT; //TODO : it works ?
        });
    }

    //edited copy of WorldGenRegion method of same name
    public boolean isWithinWriteZone(FeaturePlaceContext<?> context, BlockPos pos)
    {
        ChunkPos featureCenter = getFeatureCenterChunk(context);
        ChunkPos chunkPos = ChunkPos.containing(pos);
        return isWithinWriteZone(featureCenter, chunkPos, 1);
    }

    //edited copy of WorldGenRegion method of same name
    private static boolean isWithinWriteZone(ChunkPos centerPos, ChunkPos readOrWritePos, int writeRadius)
    {
        return Math.abs(centerPos.x() - readOrWritePos.x()) <= writeRadius && Math.abs(centerPos.z() - readOrWritePos.z()) <= writeRadius;
    }

    public static ChunkPos getFeatureCenterChunk(FeaturePlaceContext<?> context)
    {
        return ChunkPos.containing(context.origin());
    }

    public static BlockPos getFeatureCenter(FeaturePlaceContext<?> context)
    {
        ChunkPos chunkPos = getFeatureCenterChunk(context);

        int centerOfFeatureX = chunkPos.x() * 16 + 8;
        int centerOfFeatureZ = chunkPos.z() * 16 + 8;
        return new BlockPos(centerOfFeatureX, context.origin().getY(), centerOfFeatureZ);
    }

    public static BlockPos getRandomPosInFeatureRegion(BlockPos featureCenter, RandomSource rand, int MAX_XZ_DISTANCE_FROM_CENTER, int MAX_Y_DISTANCE_FROM_CENTER)
    {
        //MAX_XZ_DISTANCE_FROM_CENTER must be <= 23
        return featureCenter.offset(rand.nextInt(- MAX_XZ_DISTANCE_FROM_CENTER, MAX_XZ_DISTANCE_FROM_CENTER), rand.nextInt(- MAX_Y_DISTANCE_FROM_CENTER, MAX_Y_DISTANCE_FROM_CENTER), rand.nextInt(- MAX_XZ_DISTANCE_FROM_CENTER, MAX_XZ_DISTANCE_FROM_CENTER));
    }

    public static boolean isBlockPosInFeatureRegion(FeaturePlaceContext<NoneFeatureConfiguration> context, BlockPos pos)
    {
        BlockPos featureCenter = getFeatureCenter(context);
        return isBlockPosInFeatureRegion(featureCenter, pos);
    }

    public static boolean isBlockPosInFeatureRegion(BlockPos featureCenter, BlockPos pos)
    {
        int MAX_FEATURE_SIZE_HORIZONTAL = 3 * 16, MAX_FEATURE_SIZE_VERTICAL = 80; //features are int 3x3 chunks
        int maxAbsHorizontalOffset = getMaxAbsoluteXZOffset(featureCenter, pos);
        int absVerticalOffset = Math.abs(featureCenter.getY() - pos.getY());
        return maxAbsHorizontalOffset < MAX_FEATURE_SIZE_HORIZONTAL/2 - 1 && absVerticalOffset < MAX_FEATURE_SIZE_VERTICAL/2;
        //MAX_FEATURE_SIZE_HORIZONTAL/2 - 1 because we check from "feature center" blockpos, which is not really the center, since a chunk is 16x16.. the center is 2x2
    }

    public static boolean isBelowMaxBuildHeight(FeaturePlaceContext<?> context, BlockPos pos)
    {
        return pos.getY() < context.level().getMaxY();
    }

    public static int getMaxAbsoluteXZOffset(BlockPos pos1, BlockPos pos2)
    {
        int xOffset = pos2.getX() - pos1.getX(); int zOffset = pos2.getZ() - pos1.getZ();
        return Math.max(Math.abs(xOffset), Math.abs(zOffset));
    }

    public static int getMaxAbsoluteXYZOffset(BlockPos pos1, BlockPos pos2)
    {
        int xOffset = pos2.getX() - pos1.getX(); int yOffset = pos2.getY() - pos1.getY(); int zOffset = pos2.getZ() - pos1.getZ();
        return Math.max(Math.max(Math.abs(xOffset), Math.abs(yOffset)), Math.abs(zOffset));
    }

    public static Vector3f getRandomOrthogonalVectorToLineDefinedWith2Points(BlockPos linePos1, BlockPos linePos2, RandomSource rand)
    {
        Vector3f vector1 = new Vector3f(linePos2.getX() - linePos1.getX(), linePos2.getY() - linePos1.getY(), linePos2.getZ() - linePos1.getZ());
        Vector3f vector2 = new Vector3f(rand.nextInt(10), rand.nextInt(10), rand.nextInt(10));
        if (vector2.x / vector1.x == vector2.y / vector1.y) {vector2.x = - vector2.x / 2;} //quickly handle the case where vector1 and vector2 may be collinear
        return new Vector3f(vector1.y * vector2.z - vector1.z * vector2.y, vector1.z * vector2.x - vector1.x * vector2.z, vector1.x * vector2.y - vector1.y * vector2.x);
    }
}
