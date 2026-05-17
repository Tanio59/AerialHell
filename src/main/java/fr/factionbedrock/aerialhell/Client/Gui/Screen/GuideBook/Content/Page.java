package fr.factionbedrock.aerialhell.Client.Gui.Screen.GuideBook.Content;

import fr.factionbedrock.aerialhell.AerialHell;
import fr.factionbedrock.aerialhell.Client.Util.TextureInfo;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class Page
{
    private final String pageName;
    private final int pageIndex;
    private final TextureInfo backgroundTexture;
    List<PageElement> pageElements;

    public Page(String pageName, TextureInfo backgroundTexture, int pageIndex)
    {
        this.pageName = pageName;
        this.backgroundTexture = backgroundTexture;
        this.pageIndex = pageIndex;
        this.pageElements = new ArrayList<>();
    }

    public void render(Font font, GuiGraphicsExtractor graphics, float scale, List<Line> lines, int bookLeft, int bookTop, int mouseX, int mouseY)
    {
        graphics.blit(RenderPipelines.GUI_TEXTURED, this.backgroundTexture.texture(), bookLeft, bookTop, 0f, 0f, this.backgroundTexture.width(), this.backgroundTexture.height(), this.backgroundTexture.width(), this.backgroundTexture.height());

        for (PageElement pageElement : this.pageElements)
        {
            pageElement.render(font, graphics, scale, lines, bookLeft, bookTop, mouseX, mouseY);
        }
    }

    public int pageIndex() {return this.pageIndex;}

    public Page addParagraph(int startLineIndex, int lastLineIndex, int lineWidth, Alignment alignment, String paragraphName)
    {
        this.pageElements.add(new Paragraph(startLineIndex, lastLineIndex, lineWidth, alignment, 0xFF7A5C3A, "aerialhell.guide_book."+ this.pageName +"."+paragraphName));
        return this;
    }

    public Page addParagraph(int startLineIndex, int lastLineIndex, int lineWidth, Alignment alignment, int color, String paragraphName)
    {
        this.pageElements.add(new Paragraph(startLineIndex, lastLineIndex, lineWidth, alignment, color, "aerialhell.guide_book."+ this.pageName +"."+paragraphName));
        return this;
    }

    public Page addItemTexture(int lineIndex, Alignment alignment, float scale, Supplier<Item> item, boolean displayTooltip)
    {
        this.pageElements.add(new ItemDisplay(lineIndex, alignment, scale, item, displayTooltip));
        return this;
    }

    public Page addTextureDisplay(int lineIndex, Alignment alignment, float scale, String path, int width, int height)
    {
        this.pageElements.add(new TextureDisplay(lineIndex, alignment, scale, new TextureInfo(Identifier.fromNamespaceAndPath(AerialHell.MODID, "textures/"+path+".png"), width, height)));
        return this;
    }

    public Page addCraftingTableRecipeDisplay(int lineIndex, Alignment alignment, float scale, CraftingTableRecipeDisplay.Ingredients ingredients, Supplier<Item> result, boolean displayTooltip)
    {
        this.pageElements.add(new CraftingTableRecipeDisplay(lineIndex, alignment, scale, ingredients, result, displayTooltip));
        return this;
    }
}