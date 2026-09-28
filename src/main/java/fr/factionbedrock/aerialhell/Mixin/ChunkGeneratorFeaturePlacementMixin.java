package fr.factionbedrock.aerialhell.Mixin;

import fr.factionbedrock.aerialhell.Util.WorldGenStructureProtection;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ChunkGenerator.class)
public class ChunkGeneratorFeaturePlacementMixin
{
    //marks the blocks placed during biome decoration as feature blocks (structures are placed in the same method, but not by this call)
    @Redirect(method = "applyBiomeDecoration", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/levelgen/placement/PlacedFeature;placeWithBiomeCheck(Lnet/minecraft/world/level/WorldGenLevel;Lnet/minecraft/world/level/chunk/ChunkGenerator;Lnet/minecraft/util/RandomSource;Lnet/minecraft/core/BlockPos;)Z"))
    private boolean placeFeatureWithStructureProtection(PlacedFeature feature, WorldGenLevel level, ChunkGenerator generator, RandomSource random, BlockPos origin)
    {
        WorldGenStructureProtection.startFeaturePlacement();
        try {return feature.placeWithBiomeCheck(level, generator, random, origin);}
        finally {WorldGenStructureProtection.endFeaturePlacement();}
    }
}
