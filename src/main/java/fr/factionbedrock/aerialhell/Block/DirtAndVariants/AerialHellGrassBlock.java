package fr.factionbedrock.aerialhell.Block.DirtAndVariants;

import fr.factionbedrock.aerialhell.Registry.AerialHellBlocks;
import fr.factionbedrock.aerialhell.Registry.AerialHellStateProperties;
import fr.factionbedrock.aerialhell.World.Features.Config.RandomPatchConfiguration;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.placement.VegetationPlacements;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Util;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.GrassBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.neoforged.neoforge.common.ItemAbilities;
import net.neoforged.neoforge.common.ItemAbility;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

public abstract class AerialHellGrassBlock extends GrassBlock implements BonemealableBlock
{
	public AerialHellGrassBlock(Properties properties)
	{
		super(properties);
		this.registerDefaultState(this.defaultBlockState().setValue(SNOWY, false).setValue(AerialHellStateProperties.SHIFTED_RENDER, false));
	}

	@Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder)
	{
		super.createBlockStateDefinition(builder);
		builder.add(AerialHellStateProperties.SHIFTED_RENDER);
	}

	@Override public void performBonemeal(ServerLevel level, RandomSource random, BlockPos pos, BlockState state)
	{
		BlockPos above = pos.above();
		BlockState grass = Blocks.SHORT_GRASS.defaultBlockState();
		Optional<Holder.Reference<PlacedFeature>> grassFeature = level.registryAccess().lookupOrThrow(Registries.PLACED_FEATURE).get(this.getBonemealFeature());

		label47:
		for(int j = 0; j < 128; ++j)
		{
			BlockPos testPos = above;

			for(int i = 0; i < j / 16; ++i)
			{
				testPos = testPos.offset(random.nextInt(3) - 1, (random.nextInt(3) - 1) * random.nextInt(3) / 2, random.nextInt(3) - 1);
				if (!level.getBlockState(testPos.below()).is(this) || level.getBlockState(testPos).isCollisionShapeFullBlock(level, testPos)) {continue label47;}
			}

			BlockState testState = level.getBlockState(testPos);
			if (testState.is(grass.getBlock()) && random.nextInt(10) == 0)
			{
				BonemealableBlock bonemealableBlock = (BonemealableBlock)grass.getBlock();
				if (bonemealableBlock.isValidBonemealTarget(level, testPos, testState))
				{
					bonemealableBlock.performBonemeal(level, random, testPos, testState);
				}
			}

			if (testState.isAir() && !level.isOutsideBuildHeight(testPos))
			{
				if (random.nextInt(8) == 0)
				{
					List<ConfiguredFeature<?, ?>> features = (level.getBiome(testPos).value()).getGenerationSettings().getBoneMealFeatures();
					if (!features.isEmpty())
					{
						ConfiguredFeature<?, ?> placementFeature = Util.getRandom(features, random);
						placementFeature.place(level, level.getChunkSource().getGenerator(), random, testPos);
					}
				} else if (grassFeature.isPresent())
				{
					((PlacedFeature)((Holder.Reference)grassFeature.get()).value()).place(level, level.getChunkSource().getGenerator(), random, testPos);
				}
			}
		}

	}

	protected abstract ResourceKey<PlacedFeature> getBonemealFeature();

	@Override @Nullable
	public BlockState getToolModifiedState(BlockState state, UseOnContext context, ItemAbility itemAbility, boolean simulate)
	{
		if (!context.getItemInHand().canPerformAction(itemAbility)) {return null;}
		if (state.getBlock() == AerialHellBlocks.STELLAR_GRASS_BLOCK.get() || state.getBlock() == AerialHellBlocks.CHISELED_STELLAR_GRASS_BLOCK.get())
		{
			if (ItemAbilities.HOE_TILL == itemAbility) {return AerialHellBlocks.STELLAR_FARMLAND.get().defaultBlockState();}
			if (ItemAbilities.SHOVEL_FLATTEN == itemAbility) {return AerialHellBlocks.STELLAR_DIRT_PATH.get().defaultBlockState();}
		}
		return super.getToolModifiedState(state, context, itemAbility, simulate);
	}
}