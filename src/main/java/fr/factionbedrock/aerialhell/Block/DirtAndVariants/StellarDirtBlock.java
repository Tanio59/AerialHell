package fr.factionbedrock.aerialhell.Block.DirtAndVariants;

import net.minecraft.world.level.block.*;

//since 26.3, hoe tilling and shovel flattening are data-driven block transformers :
//see data/neoforge/data_maps/block/transformables.json (stellar dirt -> stellar farmland / stellar dirt path, stellar coarse dirt -> stellar dirt)
public class StellarDirtBlock extends Block
{
	public StellarDirtBlock(Properties properties) {super(properties);}
}
