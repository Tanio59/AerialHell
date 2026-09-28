package fr.factionbedrock.aerialhell.World.Structure;

import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.heightproviders.HeightProvider;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;

import java.util.List;
import java.util.Optional;

public abstract class AbstractClassicLittleStructure extends AbstractAerialHellStructure
{
    public AbstractClassicLittleStructure(Structure.StructureSettings config, Holder<StructureTemplatePool> startPool, Optional<Identifier> startJigsawName, int size, HeightProvider startHeight, Optional<Heightmap.Types> projectStartToHeightmap, int maxDistanceFromCenter)
    {
        super(config, startPool, startJigsawName, size, startHeight, projectStartToHeightmap, maxDistanceFromCenter, List.of());
    }

    @Override protected boolean isStructureChunk(Structure.GenerationContext context)
    {
        //collisions with other structures and with the terrain are checked in AbstractAerialHellStructure
        int landHeight = getTerrainHeight(context);
        return landHeight > getMinY() && landHeight < getMaxY();
    }

    protected abstract int getMinY();
    protected abstract int getMaxY();
}