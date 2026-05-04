package fr.factionbedrock.aerialhell.Client.Packet;

import fr.factionbedrock.aerialhell.Client.Gui.Screen.Inventory.AerialHellBookScreen;
import fr.factionbedrock.aerialhell.Config.LoadedConfigParams;
import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public class ClientPayloadHandler
{
    public static void handleDataOnMain(final AerialHellData data, final IPayloadContext context)
    {
        if (data.name().equals("reloadTextures"))
        {
            if (LoadedConfigParams.ENABLE_SHADOW_BIND_RELOAD_TEXTURE && LoadedConfigParams.ENABLE_SHADOW_BIND_TEXTURE_SHIFT)
            {
                Minecraft.getInstance().reloadResourcePacks();
            }
        }
        else if (data.name().equals("aerial_hell_advancement"))
        {
            AerialHellBookScreen.aerialHellUnlocked = data.age() == 1;
        }
    }
}