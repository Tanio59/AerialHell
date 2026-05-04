package fr.factionbedrock.aerialhell.Event.Listeners;

import fr.factionbedrock.aerialhell.Client.Packet.AerialHellData;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.entity.player.AdvancementEvent;
import net.neoforged.neoforge.network.PacketDistributor;

public class AdvancementEventListener
{
    public static void onAdvancementEarn(AdvancementEvent.AdvancementEarnEvent event)
    {
        if (event.getAdvancement().id().equals(
                Identifier.fromNamespaceAndPath("aerialhell", "story/enter_aerial_hell")))
        {
            if (event.getEntity() instanceof ServerPlayer player)
            {
                PacketDistributor.sendToPlayer(player,
                        new AerialHellData("aerial_hell_advancement", 1));
            }
        }
    }
}