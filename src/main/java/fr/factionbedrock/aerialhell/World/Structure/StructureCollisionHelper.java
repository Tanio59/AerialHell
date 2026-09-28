package fr.factionbedrock.aerialhell.World.Structure;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.core.Registry;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePiecesBuilder;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;
import net.minecraft.world.level.levelgen.RandomState;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.concurrent.ExecutionException;

/*
 * Prevents Aerial Hell structures from colliding with the terrain and with each other.
 *
 * Terrain : every structure layout is checked against the base terrain (noise) before being accepted.
 *   - floating structures must be surrounded by air (they are moved up or down to find free space),
 *   - grounded structures must not go through terrain above their ground level,
 *   - hanging structures (upside down pyramid) must not go through terrain below them,
 *   - underground structures are inside terrain by design (see terrain_adaptation in their json).
 *
 * Other structures : structures are ordered (priority, then structure id, then chunk position).
 *   A structure does not generate if its layout intersects the "terrain-valid" layout of a structure placed before it in this order.
 *   The check is deterministic and only depends on the seed, so the result is the same whichever chunk is generated first.
 *   Since the check is made against layouts which ignore the other structures, if two structures intersect, the last one in the order
 *   can never generate : no collision is possible (a few structures may be removed when the layout that removed them is itself removed, which is fine).
 */
public class StructureCollisionHelper
{
    //minimal space between two structures
    public static final int STRUCTURE_MARGIN = 4;
    //space which must be free around floating structures and above grounded structures
    private static final int TERRAIN_MARGIN = 3;
    //max terrain height above ground level tolerated in a grounded structure footprint (slopes)
    private static final int GROUND_TOLERANCE = 5;
    //max vertical move of floating structures to find a free space
    private static final int MAX_FLOATING_VERTICAL_OFFSET = 48, FLOATING_VERTICAL_OFFSET_STEP = 8;
    //horizontal distance between two terrain samples
    private static final int COLUMN_SAMPLING_STEP = 4;

    //layouts computed for neighbour structures, reused when neighbour chunks are generated
    private static final Cache<LayoutKey, Optional<StructureLayout>> LAYOUT_CACHE = CacheBuilder.newBuilder().maximumSize(16384).build();

    private record LayoutKey(Structure structure, RandomState randomState, long chunkPos) {}

    //the cached layouts belong to the world which was running
    public static void onServerStopped(ServerStoppedEvent event) {LAYOUT_CACHE.invalidateAll();}

    public record StructureLayout(BoundingBox box, List<BoundingBox> pieceBoxes)
    {
        public static StructureLayout of(StructurePiecesBuilder builder)
        {
            List<BoundingBox> pieceBoxes = new ArrayList<>();
            for (StructurePiece piece : builder.build().pieces()) {pieceBoxes.add(piece.getBoundingBox());}
            return new StructureLayout(builder.getBoundingBox(), pieceBoxes);
        }

        public boolean intersects(StructureLayout other, int margin)
        {
            if (!this.box.inflatedBy(margin).intersects(other.box)) {return false;}
            for (BoundingBox pieceBox : this.pieceBoxes)
            {
                BoundingBox inflatedPieceBox = pieceBox.inflatedBy(margin);
                if (!inflatedPieceBox.intersects(other.box)) {continue;}
                for (BoundingBox otherPieceBox : other.pieceBoxes)
                {
                    if (inflatedPieceBox.intersects(otherPieceBox)) {return true;}
                }
            }
            return false;
        }
    }

    /* ------------------------------------------------ Structure / structure ------------------------------------------------ */

    public static boolean collidesWithPriorStructure(AbstractAerialHellStructure structure, Structure.GenerationContext context, StructureLayout layout)
    {
        var structureRegistry = context.registryAccess().lookupOrThrow(Registries.STRUCTURE);
        Identifier structureId = structureRegistry.getKey(structure);
        Set<Holder<Biome>> possibleBiomes = context.biomeSource().possibleBiomes();
        ChunkPos chunkPos = context.chunkPos();
        BoundingBox zone = layout.box().inflatedBy(STRUCTURE_MARGIN);

        for (StructureSet set : context.registryAccess().lookupOrThrow(Registries.STRUCTURE_SET))
        {
            if (!(set.placement() instanceof RandomSpreadStructurePlacement placement)) {continue;}

            //only the structures which can be placed before this one matter (checked before any expensive computation)
            List<AbstractAerialHellStructure> priorStructures = new ArrayList<>();
            int maxReach = -1;
            for (StructureSet.StructureSelectionEntry entry : set.structures())
            {
                if (entry.structure().value() instanceof AbstractAerialHellStructure other && canGenerateWithBiomes(other, possibleBiomes) && canBePlacedBefore(other, structureRegistry.getKey(other), structure, structureId))
                {
                    priorStructures.add(other);
                    //pieces are at most maxDistanceFromCenter blocks away from the center of the start piece, which is near the start chunk
                    maxReach = Math.max(maxReach, other.maxDistanceFromCenter + 32);
                }
            }
            if (priorStructures.isEmpty()) {continue;}

            //chunks in which a structure of this set can start and still reach the zone
            int minChunkX = SectionPos.blockToSectionCoord(zone.minX() - maxReach), maxChunkX = SectionPos.blockToSectionCoord(zone.maxX() + maxReach);
            int minChunkZ = SectionPos.blockToSectionCoord(zone.minZ() - maxReach), maxChunkZ = SectionPos.blockToSectionCoord(zone.maxZ() + maxReach);
            int spacing = placement.spacing();

            for (int regionX = Math.floorDiv(minChunkX, spacing); regionX <= Math.floorDiv(maxChunkX, spacing); regionX++)
            {
                for (int regionZ = Math.floorDiv(minChunkZ, spacing); regionZ <= Math.floorDiv(maxChunkZ, spacing); regionZ++)
                {
                    ChunkPos candidate = placement.getPotentialStructureChunk(context.seed(), regionX * spacing, regionZ * spacing);
                    if (candidate.x() < minChunkX || candidate.x() > maxChunkX || candidate.z() < minChunkZ || candidate.z() > maxChunkZ) {continue;}
                    if (!placement.applyAdditionalChunkRestrictions(candidate.x(), candidate.z(), context.seed())) {continue;}
                    if (!isAnyPlacedBefore(priorStructures, structureRegistry, candidate, structure, structureId, chunkPos)) {continue;}

                    Optional<SetLayout> candidateLayout = getSetLayout(set, context, candidate);
                    if (candidateLayout.isEmpty()) {continue;}

                    AbstractAerialHellStructure other = candidateLayout.get().structure();
                    if (other == structure && candidate.equals(chunkPos)) {continue;} //this is the structure being generated
                    if (!isPlacedBefore(other, structureRegistry.getKey(other), candidate, structure, structureId, chunkPos)) {continue;}

                    if (candidateLayout.get().layout().intersects(layout, STRUCTURE_MARGIN)) {return true;}
                }
            }
        }
        return false;
    }

    private static boolean canGenerateWithBiomes(AbstractAerialHellStructure structure, Set<Holder<Biome>> possibleBiomes)
    {
        for (Holder<Biome> biome : structure.biomes()) {if (possibleBiomes.contains(biome)) {return true;}}
        return false;
    }

    //true if a structure of this type is placed before the other one, at least at some positions
    private static boolean canBePlacedBefore(AbstractAerialHellStructure structure, Identifier id, AbstractAerialHellStructure otherStructure, Identifier otherId)
    {
        int priority = structure.getGenerationPriority(), otherPriority = otherStructure.getGenerationPriority();
        if (priority != otherPriority) {return priority > otherPriority;}
        return String.valueOf(id).compareTo(String.valueOf(otherId)) <= 0;
    }

    private static boolean isAnyPlacedBefore(List<AbstractAerialHellStructure> structures, Registry<Structure> registry, ChunkPos pos, AbstractAerialHellStructure otherStructure, Identifier otherId, ChunkPos otherPos)
    {
        for (AbstractAerialHellStructure structure : structures)
        {
            if (isPlacedBefore(structure, registry.getKey(structure), pos, otherStructure, otherId, otherPos)) {return true;}
        }
        return false;
    }

    //deterministic total order of the structures
    private static boolean isPlacedBefore(AbstractAerialHellStructure structure, Identifier id, ChunkPos pos, AbstractAerialHellStructure otherStructure, Identifier otherId, ChunkPos otherPos)
    {
        int priority = structure.getGenerationPriority(), otherPriority = otherStructure.getGenerationPriority();
        if (priority != otherPriority) {return priority > otherPriority;}
        int idComparison = String.valueOf(id).compareTo(String.valueOf(otherId));
        if (idComparison != 0) {return idComparison < 0;}
        if (pos.x() != otherPos.x()) {return pos.x() < otherPos.x();}
        return pos.z() < otherPos.z();
    }

    private record SetLayout(AbstractAerialHellStructure structure, StructureLayout layout) {}

    //same structure selection as ChunkGenerator.createStructures
    private static Optional<SetLayout> getSetLayout(StructureSet set, Structure.GenerationContext context, ChunkPos chunkPos)
    {
        List<StructureSet.StructureSelectionEntry> entries = set.structures();
        if (entries.size() == 1) {return getSetLayout(entries.getFirst(), context, chunkPos);}

        List<StructureSet.StructureSelectionEntry> options = new ArrayList<>(entries);
        WorldgenRandom random = new WorldgenRandom(new LegacyRandomSource(0L));
        random.setLargeFeatureSeed(context.seed(), chunkPos.x(), chunkPos.z());
        int total = 0;
        for (StructureSet.StructureSelectionEntry option : options) {total += option.weight();}

        while (!options.isEmpty())
        {
            int choice = random.nextInt(total);
            int index = 0;
            for (StructureSet.StructureSelectionEntry option : options)
            {
                choice -= option.weight();
                if (choice < 0) {break;}
                index++;
            }

            StructureSet.StructureSelectionEntry selected = options.get(index);
            Optional<SetLayout> layout = getSetLayout(selected, context, chunkPos);
            if (layout.isPresent()) {return layout;}
            options.remove(index);
            total -= selected.weight();
        }
        return Optional.empty();
    }

    private static Optional<SetLayout> getSetLayout(StructureSet.StructureSelectionEntry entry, Structure.GenerationContext context, ChunkPos chunkPos)
    {
        //a structure from another mod can't be predicted, it is ignored
        if (!(entry.structure().value() instanceof AbstractAerialHellStructure structure)) {return Optional.empty();}
        return getTerrainValidLayout(structure, context, chunkPos).map(layout -> new SetLayout(structure, layout));
    }

    private static Optional<StructureLayout> getTerrainValidLayout(AbstractAerialHellStructure structure, Structure.GenerationContext originContext, ChunkPos chunkPos)
    {
        LayoutKey key = new LayoutKey(structure, originContext.randomState(), chunkPos.pack());
        try
        {
            return LAYOUT_CACHE.get(key, () ->
            {
                //same context as the one created by vanilla when generating the structure in this chunk
                Structure.GenerationContext context = new Structure.GenerationContext(
                        originContext.registryAccess(),
                        originContext.chunkGenerator(),
                        originContext.biomeSource(),
                        originContext.randomState(),
                        originContext.structureTemplateManager(),
                        originContext.seed(),
                        chunkPos,
                        originContext.heightAccessor(),
                        structure.biomes()::contains);
                return structure.findTerrainValidGenerationPoint(context).map(stub -> StructureLayout.of(stub.getPiecesBuilder()));
            });
        }
        catch (ExecutionException e) {throw new RuntimeException(e.getCause());}
    }

    /* ------------------------------------------------ Structure / terrain ------------------------------------------------ */

    //same test as the private Structure.isValidBiome method, which is applied by vanilla after findGenerationPoint
    public static boolean isValidBiome(Structure.GenerationContext context, BlockPos pos)
    {
        return context.validBiome().test(context.chunkGenerator().getBiomeSource().getNoiseBiome(QuartPos.fromBlock(pos.getX()), QuartPos.fromBlock(pos.getY()), QuartPos.fromBlock(pos.getZ()), context.randomState().sampler()));
    }

    /**
     * @return the vertical offset to apply to the structure pieces so that they don't collide with the terrain, or empty if the structure can't generate here
     */
    public static OptionalInt findTerrainFreeVerticalOffset(AbstractAerialHellStructure.PlacementType placementType, Structure.GenerationContext context, StructurePiecesBuilder builder, BlockPos startPos)
    {
        if (placementType == AbstractAerialHellStructure.PlacementType.UNDERGROUND) {return OptionalInt.of(0);}

        List<SampledColumn> columns = sampleColumns(StructureLayout.of(builder).pieceBoxes());

        if (placementType == AbstractAerialHellStructure.PlacementType.GROUNDED)
        {
            //terrain above ground level (+ tolerance for slopes) would go through the structure
            int minCheckedY = startPos.getY() + GROUND_TOLERANCE;
            for (SampledColumn column : columns)
            {
                if (hasTerrainBetween(column.getNoise(context), minCheckedY, column.maxY + TERRAIN_MARGIN)) {return OptionalInt.empty();}
            }
            return OptionalInt.of(0);
        }
        else if (placementType == AbstractAerialHellStructure.PlacementType.HANGING)
        {
            //terrain below the island the structure is hanging from would go through the structure
            int maxCheckedY = startPos.getY() - GROUND_TOLERANCE;
            for (SampledColumn column : columns)
            {
                if (hasTerrainBetween(column.getNoise(context), column.minY - TERRAIN_MARGIN, maxCheckedY)) {return OptionalInt.empty();}
            }
            return OptionalInt.of(0);
        }
        else //FLOATING
        {
            LevelHeightAccessor heightAccessor = context.heightAccessor();
            BoundingBox box = builder.getBoundingBox();
            for (int i = 0; i <= 2 * MAX_FLOATING_VERTICAL_OFFSET / FLOATING_VERTICAL_OFFSET_STEP; i++)
            {
                //0, +step, -step, +2*step, -2*step, ...
                int offset = ((i + 1) / 2) * FLOATING_VERTICAL_OFFSET_STEP * (i % 2 == 0 ? -1 : 1);
                if (box.minY() + offset <= heightAccessor.getMinY() || box.maxY() + offset >= heightAccessor.getMaxY()) {continue;}

                boolean free = true;
                for (SampledColumn column : columns)
                {
                    if (hasTerrainBetween(column.getNoise(context), column.minY + offset - TERRAIN_MARGIN, column.maxY + offset + TERRAIN_MARGIN)) {free = false; break;}
                }
                if (free) {return OptionalInt.of(offset);}
            }
            return OptionalInt.empty();
        }
    }

    //a terrain column covered by the structure, with the vertical range covered by the pieces. The terrain noise is computed only when needed.
    private static class SampledColumn
    {
        private final int x, z;
        private int minY = Integer.MAX_VALUE, maxY = Integer.MIN_VALUE;
        private NoiseColumn noise;

        private SampledColumn(int x, int z) {this.x = x; this.z = z;}

        private NoiseColumn getNoise(Structure.GenerationContext context)
        {
            if (this.noise == null) {this.noise = context.chunkGenerator().getBaseColumn(this.x, this.z, context.heightAccessor(), context.randomState());}
            return this.noise;
        }
    }

    //samples columns on a global grid (so that adjacent pieces share their samples), and at the center of each piece (so that small pieces are sampled too)
    private static List<SampledColumn> sampleColumns(List<BoundingBox> pieceBoxes)
    {
        Long2ObjectMap<SampledColumn> columns = new Long2ObjectLinkedOpenHashMap<>();
        for (BoundingBox box : pieceBoxes)
        {
            int minX = box.minX() - 1, maxX = box.maxX() + 1, minZ = box.minZ() - 1, maxZ = box.maxZ() + 1;
            for (int x = roundUpToStep(minX); x <= maxX; x += COLUMN_SAMPLING_STEP)
            {
                for (int z = roundUpToStep(minZ); z <= maxZ; z += COLUMN_SAMPLING_STEP) {addSample(columns, x, z, box);}
            }
            addSample(columns, (box.minX() + box.maxX()) / 2, (box.minZ() + box.maxZ()) / 2, box);
        }
        return new ArrayList<>(columns.values());
    }

    private static int roundUpToStep(int value) {return Math.floorDiv(value + COLUMN_SAMPLING_STEP - 1, COLUMN_SAMPLING_STEP) * COLUMN_SAMPLING_STEP;}

    private static void addSample(Long2ObjectMap<SampledColumn> columns, int x, int z, BoundingBox box)
    {
        SampledColumn column = columns.computeIfAbsent(BlockPos.asLong(x, 0, z), key -> new SampledColumn(x, z));
        column.minY = Math.min(column.minY, box.minY());
        column.maxY = Math.max(column.maxY, box.maxY());
    }

    private static boolean hasTerrainBetween(NoiseColumn column, int minY, int maxY)
    {
        for (int y = minY; y <= maxY; y++)
        {
            if (!column.getBlock(y).isAir()) {return true;}
        }
        return false;
    }
}
