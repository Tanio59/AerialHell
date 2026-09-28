package fr.factionbedrock.aerialhell.World.Structure;

import com.mojang.datafixers.util.Either;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.WorldGenerationContext;
import net.minecraft.world.level.levelgen.heightproviders.HeightProvider;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePiecesBuilder;
import net.minecraft.world.level.levelgen.structure.pools.DimensionPadding;
import net.minecraft.world.level.levelgen.structure.pools.JigsawPlacement;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import net.minecraft.world.level.levelgen.structure.pools.alias.PoolAliasBinding;
import net.minecraft.world.level.levelgen.structure.pools.alias.PoolAliasLookup;
import net.minecraft.world.level.levelgen.structure.structures.JigsawStructure;
import net.minecraft.world.level.levelgen.structure.templatesystem.LiquidSettings;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;

public abstract class AbstractAerialHellStructure extends Structure
{
    protected static final int MIN_STRUCTURE_SIZE = 0, MAX_STRUCTURE_SIZE = 50;
    protected static final int MIN_STRUCTURE_DISTANCE_FROM_CENTER = 1, MAX_STRUCTURE_DISTANCE_FROM_CENTER = 256;

    protected final Holder<StructureTemplatePool> startPool;
    protected final Optional<Identifier> startJigsawName;
    protected final int size;
    protected final HeightProvider startHeight;
    protected final Optional<Heightmap.Types> projectStartToHeightmap;
    protected final int maxDistanceFromCenter;
    protected final List<PoolAliasBinding> poolAliases;

    public AbstractAerialHellStructure(Structure.StructureSettings config, Holder<StructureTemplatePool> startPool, Optional<Identifier> startJigsawName, int size, HeightProvider startHeight, Optional<Heightmap.Types> projectStartToHeightmap, int maxDistanceFromCenter, List<PoolAliasBinding> poolAliasBindingList)
    {
        super(config);
        this.startPool = startPool;
        this.startJigsawName = startJigsawName;
        this.size = size;
        this.startHeight = startHeight;
        this.projectStartToHeightmap = projectStartToHeightmap;
        this.maxDistanceFromCenter = maxDistanceFromCenter;
        this.poolAliases = poolAliasBindingList;
    }

    protected abstract boolean isStructureChunk(Structure.GenerationContext context);

    protected enum ChunkCoordinateType{CHUNK_MIN_COORDINATE, CHUNK_MAX_COORDINATE, CHUNK_MIDDLE_COORDINATE, MEAN, MEAN_IGNORE_OUTLIER}

    protected static int getTerrainHeight(Structure.GenerationContext context)
    {
        return getTerrainHeight(context, ChunkCoordinateType.MEAN_IGNORE_OUTLIER);
    }

    protected static int getTerrainHeight(Structure.GenerationContext context, ChunkCoordinateType type)
    {
        ChunkPos chunkpos = context.chunkPos();
        if (type == ChunkCoordinateType.CHUNK_MIN_COORDINATE)
        {
            return getTerrainHeight(context, chunkpos.getMinBlockX(), chunkpos.getMinBlockZ());
        }
        else if (type == ChunkCoordinateType.CHUNK_MAX_COORDINATE)
        {
            return getTerrainHeight(context, chunkpos.getMaxBlockX(), chunkpos.getMaxBlockZ());
        }
        else if (type == ChunkCoordinateType.CHUNK_MIDDLE_COORDINATE)
        {
            return getTerrainHeight(context, chunkpos.getMiddleBlockX(), chunkpos.getMiddleBlockZ());
        }
        else if (type == ChunkCoordinateType.MEAN || type == ChunkCoordinateType.MEAN_IGNORE_OUTLIER)
        {
            int xMin = chunkpos.getMinBlockX(), xMax = chunkpos.getMaxBlockX();
            int zMin = chunkpos.getMinBlockZ(), zMax = chunkpos.getMaxBlockZ();
            int xMiddle = chunkpos.getMiddleBlockX(), zMiddle = chunkpos.getMiddleBlockZ();

            int NorthWestHeight = getTerrainHeight(context, xMin, zMin);
            int SouthEastHeight = getTerrainHeight(context, xMax, zMax);
            int NorthEastHeight = getTerrainHeight(context, xMax, zMin);
            int SouthWestHeight = getTerrainHeight(context, xMin, zMax);
            int MiddleHeight = getTerrainHeight(context, xMiddle, zMiddle);

            if (type == ChunkCoordinateType.MEAN_IGNORE_OUTLIER)
            {
                boolean ignoreNorthWest = NorthWestHeight < 10;
                boolean ignoreSouthEast = SouthEastHeight < 10;
                boolean ignoreNorthEast = NorthEastHeight < 10;
                boolean ignoreSouthWest = SouthWestHeight < 10;
                boolean ignoreMiddle = MiddleHeight < 10;

                if (ignoreNorthWest && ignoreSouthEast && ignoreNorthEast && ignoreSouthWest && ignoreMiddle) {return MiddleHeight;}

                int referenceHeight = ignoreMiddle ? ignoreNorthWest ? ignoreSouthEast ? ignoreNorthEast ? SouthWestHeight : NorthEastHeight : SouthEastHeight: NorthWestHeight : MiddleHeight;
                if (referenceHeight == NorthWestHeight) {ignoreNorthWest = true;}
                else if (referenceHeight == SouthEastHeight) {ignoreSouthEast = true;}
                else if (referenceHeight == NorthEastHeight) {ignoreNorthEast = true;}
                else if (referenceHeight == SouthWestHeight) {ignoreSouthWest = true;}
                else if (referenceHeight == MiddleHeight) {ignoreMiddle = true;}

                int heightSum = referenceHeight;
                int count = 1;

                if (!ignoreNorthWest && Math.abs(referenceHeight - NorthWestHeight) < 10) {heightSum += NorthWestHeight; count++;}
                if (!ignoreSouthEast && Math.abs(referenceHeight - SouthEastHeight) < 10) {heightSum += SouthEastHeight; count++;}
                if (!ignoreNorthEast && Math.abs(referenceHeight - NorthEastHeight) < 10) {heightSum += NorthEastHeight; count++;}
                if (!ignoreSouthWest && Math.abs(referenceHeight - SouthWestHeight) < 10) {heightSum += SouthWestHeight; count++;}
                if (!ignoreMiddle && Math.abs(referenceHeight - MiddleHeight) < 10) {heightSum += MiddleHeight; count++;}

                return heightSum / count;
            }
            else //if type == MEAN
            {
                //unreliable because there can be heights in the void (y = -1).
                return (NorthWestHeight + SouthEastHeight + NorthEastHeight + SouthWestHeight + MiddleHeight) / 5;
            }
        }

        return getTerrainHeight(context, chunkpos.getMiddleBlockX(), chunkpos.getMiddleBlockZ());
    }

    protected static int getTerrainHeight(Structure.GenerationContext context, int posX, int posZ)
    {
        return context.chunkGenerator().getFirstOccupiedHeight(
                posX,
                posZ,
                Heightmap.Types.WORLD_SURFACE_WG,
                context.heightAccessor(),
                context.randomState());
    }

    //how the structure is supposed to be placed relatively to the terrain. Used to prevent the structure from colliding with the terrain.
    public enum PlacementType {GROUNDED, FLOATING, HANGING, UNDERGROUND}

    protected PlacementType getPlacementType() {return this.projectStartToHeightmap.isPresent() ? PlacementType.GROUNDED : PlacementType.FLOATING;}

    //when two structures would collide, the one with the highest priority generates
    protected abstract int getGenerationPriority();

    @Override
    public Optional<Structure.GenerationStub> findGenerationPoint(Structure.GenerationContext context)
    {
        Optional<Structure.GenerationStub> stub = this.findTerrainValidGenerationPoint(context);
        if (stub.isEmpty()) {return Optional.empty();}

        StructureCollisionHelper.StructureLayout layout = StructureCollisionHelper.StructureLayout.of(stub.get().getPiecesBuilder());
        if (StructureCollisionHelper.collidesWithPriorStructure(this, context, layout)) {return Optional.empty();}
        return stub;
    }

    //generation point which doesn't collide with the terrain, other structures are not taken into account
    protected Optional<Structure.GenerationStub> findTerrainValidGenerationPoint(Structure.GenerationContext context)
    {
        if (!this.isStructureChunk(context)) {return Optional.empty();}

        BlockPos structureCenter = findStructureCenter(context);
        if (structureCenter == null) {return Optional.empty();}

        Optional<Structure.GenerationStub> structurePiecesGenerator =
                JigsawPlacement.addPieces(
                        context, // Used for JigsawPlacement to get all the proper behaviors done.
                        this.startPool, // The starting pool to use to create the structure layout from
                        this.startJigsawName, // Can be used to only spawn from one Jigsaw block. But we don't need to worry about this.
                        this.size, // How deep a branch of pieces can go away from center piece. (5 means branches cannot be longer than 5 pieces from center piece)
                        structureCenter, // Where to spawn the structure.
                        false, //"useExpansionHack" (set it false)
                        Optional.empty(), //this.projectStartToHeightmap is now manually override by findStructureCenter(context).
                        new JigsawStructure.MaxDistance(this.maxDistanceFromCenter), // Maximum limit for how far pieces can spawn from center. You cannot set this bigger than 128 or else pieces gets cutoff.
                        PoolAliasLookup.EMPTY, // Optional thing that allows swapping a template pool with another per structure json instance. We don't need this but see vanilla JigsawStructure class for how to wire it up if you want it.
                        DimensionPadding.ZERO, // dimensionPadding - Optional thing to prevent generating too close to the bottom or top of the dimension.
                        LiquidSettings.IGNORE_WATERLOGGING); // liquidSettings - Optional thing to control whether the structure will be waterlogged when replacing pre-existing water in the world.

        if (structurePiecesGenerator.isEmpty()) {return Optional.empty();}

        //pieces are built now (instead of later by vanilla) to check the structure layout. The random used is the same, so the layout is the same.
        StructurePiecesBuilder builder = structurePiecesGenerator.get().getPiecesBuilder();
        BlockPos position = structurePiecesGenerator.get().position();
        if (builder.isEmpty() || !StructureCollisionHelper.isValidBiome(context, position)) {return Optional.empty();}

        OptionalInt verticalOffset = StructureCollisionHelper.findTerrainFreeVerticalOffset(this.getPlacementType(), context, builder, position);
        if (verticalOffset.isEmpty()) {return Optional.empty();}
        if (verticalOffset.getAsInt() != 0)
        {
            builder.offsetPiecesVertically(verticalOffset.getAsInt());
            position = position.above(verticalOffset.getAsInt());
            if (!StructureCollisionHelper.isValidBiome(context, position)) {return Optional.empty();}
        }

        return Optional.of(new Structure.GenerationStub(position, Either.right(builder)));
    }

    @Nullable protected BlockPos findStructureCenter(Structure.GenerationContext context)
    {
        ChunkPos chunkPos = context.chunkPos();

        int sampledStartHeight = this.startHeight.sample(context.random(), new WorldGenerationContext(context.chunkGenerator(), context.heightAccessor()));
        //sampledStartHeight corresponds to :
        //effective startY if this.projectStartToHeightmap.isEmpty()
        //y offset to apply if this.projectStartToHeightmap.isPresent()

        int startY = this.projectStartToHeightmap.isPresent() ? getTerrainHeight(context) + sampledStartHeight : sampledStartHeight;

        return new BlockPos(chunkPos.getMiddleBlockX(), startY, chunkPos.getMiddleBlockZ());
    }
}