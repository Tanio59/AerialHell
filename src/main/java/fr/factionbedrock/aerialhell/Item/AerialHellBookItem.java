package fr.factionbedrock.aerialhell.Item;

import fr.factionbedrock.aerialhell.Client.Gui.Screen.Inventory.AerialHellBookScreen;
import fr.factionbedrock.aerialhell.Client.Packet.AerialHellData;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.PacketDistributor;

public class AerialHellBookItem extends Item
{
    public AerialHellBookItem(Properties properties)
    {
        super(properties.stacksTo(1));
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand)
    {
        if (!level.isClientSide() && player instanceof ServerPlayer sp)
        {
            // Vérifie si le joueur a déjà l'advancement et informe le client
            var advHolder = sp.level().getServer().getAdvancements().get(
                    Identifier.fromNamespaceAndPath("aerialhell", "story/enter_aerial_hell"));
            boolean hasAdv = advHolder != null &&
                    sp.getAdvancements().getOrStartProgress(advHolder).isDone();
            PacketDistributor.sendToPlayer(sp,
                    new AerialHellData("aerial_hell_advancement", hasAdv ? 1 : 0));
        }

        if (level.isClientSide())
        {
            Minecraft.getInstance().setScreen(new AerialHellBookScreen());
        }
        return InteractionResult.SUCCESS;
    }
}