package fr.factionbedrock.aerialhell.Item;

import fr.factionbedrock.aerialhell.AerialHell;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.component.CookingFuel;
import net.minecraft.world.level.storage.loot.providers.number.floats.ContextFloatProviders;
import net.minecraft.world.level.storage.loot.providers.number.ints.ContextIntProvider;

public class BurnableItem extends Item
{
	public BurnableItem(Properties properties, int burnTimeIn)
	{
		super(properties.component(DataComponents.COOKING_FUEL, smeltingOnlyFuel(burnTimeIn)));
	}

	//Since 26.3, furnace fuels are defined by the minecraft:cooking_fuel item component.
	//The burn time is given by aerialhell:cooking/smelting_only_<burnTime> context int provider, which returns 0 in smokers and blast furnaces
	//(burnable Aerial Hell items could only be used in furnaces before 26.3)
	public static CookingFuel smeltingOnlyFuel(int burnTime)
	{
		ResourceKey<ContextIntProvider> burnTimeProvider = ResourceKey.create(Registries.CONTEXT_INT_PROVIDER, Identifier.fromNamespaceAndPath(AerialHell.MODID, "cooking/smelting_only_" + burnTime));
		return new CookingFuel(burnTimeProvider, ContextFloatProviders.COOKING_DEFAULT_SPEED_MULTIPLIER);
	}
}
