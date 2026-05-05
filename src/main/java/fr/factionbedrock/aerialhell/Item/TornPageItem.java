package fr.factionbedrock.aerialhell.Item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

import java.util.function.Consumer;

public class TornPageItem extends Item
{
    private final String pageId;

    public TornPageItem(String pageId, Properties properties)
    {
        super(properties.stacksTo(1));
        this.pageId = pageId;
    }

    public String getPageId() { return pageId; }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay tooltipDisplay,
                                 Consumer<Component> tooltipAdder, TooltipFlag flag)
    {
        tooltipAdder.accept(Component.translatable("item.aerialhell.torn_page.tooltip." + pageId));
    }
}
