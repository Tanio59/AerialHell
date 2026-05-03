package fr.factionbedrock.aerialhell.Item;

import fr.factionbedrock.aerialhell.Client.Gui.Screen.Inventory.AerialHellBookScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;

public class AerialHellBookItem extends Item
{
    public AerialHellBookItem(Properties properties)
    {
        super(properties.stacksTo(1));
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand)
    {
        if (level.isClientSide())
        {
            Minecraft.getInstance().setScreen(new AerialHellBookScreen());
        }
        return InteractionResult.SUCCESS;
    }
}