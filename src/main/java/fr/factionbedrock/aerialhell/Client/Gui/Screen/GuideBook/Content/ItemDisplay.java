package fr.factionbedrock.aerialhell.Client.Gui.Screen.GuideBook.Content;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.item.Item;

import java.util.List;
import java.util.function.Supplier;

public record ItemDisplay(int lineIndex, Alignment alignment, float scale, Supplier<Item> item, boolean displayTooltip) implements PageElement
{
    @Override public void render(Font font, GuiGraphicsExtractor graphics, float scale, List<Line> lines, int bookLeft, int bookTop, int mouseX, int mouseY)
    {
        Item item = this.item.get();
        if (item == null) {return;}

        Line line = lines.get(this.lineIndex());

        int itemSize = (int)(16 * this.scale());

        int startX = switch (this.alignment())
        {
            case LEFT -> line.startX();
            case CENTER -> line.centerX() - itemSize / 2;
            case RIGHT -> line.rightX() - itemSize;
        };

        boolean hovered = mouseX >= startX && mouseX <= startX + itemSize && mouseY >= line.startY() && mouseY <= line.startY() + itemSize;

        graphics.pose().pushMatrix();

        graphics.pose().translate(startX, line.startY());
        graphics.pose().scale(this.scale(), this.scale());

        graphics.fakeItem(item.getDefaultInstance(), 0, 0);

        graphics.pose().popMatrix();

        if (hovered && this.displayTooltip()) {graphics.setTooltipForNextFrame(font, item.getDefaultInstance(), mouseX, mouseY);}
    }
}
