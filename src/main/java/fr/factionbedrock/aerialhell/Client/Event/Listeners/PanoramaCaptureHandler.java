package fr.factionbedrock.aerialhell.Client.Event.Listeners;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;

//Dev tool (registered only in dev environment, see AerialHellClientSetup)
//"/aerialhell_panorama [delay_seconds]" captures the 6 faces of a title screen panorama (cube map) from the player eyes, using vanilla panorama screenshot
//pictures are saved in <game directory>/screenshots/panorama_0.png ... panorama_5.png
//to use them as title screen background, copy them to src/main/resources/assets/minecraft/textures/gui/title/background/
public class PanoramaCaptureHandler
{
    private static final int DEFAULT_DELAY_SECONDS = 2; //lets the chat screen close before capture
    private static int ticksBeforeCapture = -1;

    public static void onRegisterClientCommands(RegisterClientCommandsEvent event)
    {
        event.getDispatcher().register(Commands.literal("aerialhell_panorama")
                .executes(context -> scheduleCapture(context.getSource(), DEFAULT_DELAY_SECONDS))
                .then(Commands.argument("delay_seconds", IntegerArgumentType.integer(0, 60))
                        .executes(context -> scheduleCapture(context.getSource(), IntegerArgumentType.getInteger(context, "delay_seconds")))));
    }

    private static int scheduleCapture(CommandSourceStack source, int delaySeconds)
    {
        ticksBeforeCapture = Math.max(1, delaySeconds * 20);
        source.sendSystemMessage(Component.literal("Aerial Hell panorama capture in " + delaySeconds + "s, don't move !").withStyle(ChatFormatting.GOLD));
        return 1;
    }

    public static void onClientTick(ClientTickEvent.Post event)
    {
        if (ticksBeforeCapture < 0 || --ticksBeforeCapture > 0) {return;}
        ticksBeforeCapture = -1;

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null) {return;}
        minecraft.player.sendSystemMessage(minecraft.grabPanoramixScreenshot(minecraft.gameDirectory));
    }
}
