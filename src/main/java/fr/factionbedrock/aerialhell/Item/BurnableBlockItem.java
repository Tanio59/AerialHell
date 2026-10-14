package fr.factionbedrock.aerialhell.Item;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;

public class BurnableBlockItem extends BlockItem
{
	//since 26.3, furnace fuels are defined by the minecraft:cooking_fuel item component (see BurnableItem.smeltingOnlyFuel)
	public BurnableBlockItem(Block blockIn, Properties builder, int burnTimeIn)
	{
		super(blockIn, builder.component(DataComponents.COOKING_FUEL, BurnableItem.smeltingOnlyFuel(burnTimeIn)));
	}
}
