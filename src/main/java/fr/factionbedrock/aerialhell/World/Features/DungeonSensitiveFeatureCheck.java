package fr.factionbedrock.aerialhell.World.Features;

import fr.factionbedrock.aerialhell.Registry.Misc.AerialHellTags;
import fr.factionbedrock.aerialhell.Util.FeatureHelper;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;

import java.util.List;

public interface DungeonSensitiveFeatureCheck
{
    //big features (tagged cant_place_in_dungeons) must stay away from structures, others only must not start inside a structure
    int BIG_FEATURE_STRUCTURE_DISTANCE = 16, SMALL_FEATURE_STRUCTURE_DISTANCE = 2;

    default boolean isDungeonSensitiveValid(FeaturePlaceContext<? extends FeatureConfiguration> context)
    {
        int distance = this.isBigFeature(context) ? BIG_FEATURE_STRUCTURE_DISTANCE : SMALL_FEATURE_STRUCTURE_DISTANCE;
        return !FeatureHelper.isFeatureGeneratingNextToStructure(context, distance, distance);
    }

    private boolean isBigFeature(FeaturePlaceContext<? extends FeatureConfiguration> context)
    {
        Registry<ConfiguredFeature<?, ?>> registry = context.level().registryAccess().lookupOrThrow(Registries.CONFIGURED_FEATURE);

        for (ResourceKey<ConfiguredFeature<?, ?>> key : this.getAssociatedConfiguredFeatures())
        {
            Holder<ConfiguredFeature<?, ?>> holder = registry.getOrThrow(key);
            if (holder.is(AerialHellTags.ConfiguredFeatures.CANT_PLACE_IN_DUNGEONS)) {return true;}
        }
        return false;
    }

    List<ResourceKey<ConfiguredFeature<?, ?>>> getAssociatedConfiguredFeatures();
}
