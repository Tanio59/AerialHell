package fr.factionbedrock.aerialhell.Client.Gui.Screen.Inventory;

import fr.factionbedrock.aerialhell.AerialHell;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.List;

public class GuideBookScreen extends Screen
{
    private static final Identifier BOOK_TEXTURE = Identifier.fromNamespaceAndPath(AerialHell.MODID, "textures/gui/guide_book/guide_book_page.png");
    private static final Identifier NAVIGATION_ARROW_PREVIOUS_PAGE = Identifier.fromNamespaceAndPath(AerialHell.MODID, "textures/gui/guide_book/navigation_arrow_previous_page.png");
    private static final Identifier NAVIGATION_ARROW_PREVIOUS_PAGE_HOVERED = Identifier.fromNamespaceAndPath(AerialHell.MODID, "textures/gui/guide_book/navigation_arrow_previous_page_hovered.png");
    private static final Identifier NAVIGATION_ARROW_NEXT_PAGE = Identifier.fromNamespaceAndPath(AerialHell.MODID, "textures/gui/guide_book/navigation_arrow_next_page.png");
    private static final Identifier NAVIGATION_ARROW_NEXT_PAGE_HOVERED = Identifier.fromNamespaceAndPath(AerialHell.MODID, "textures/gui/guide_book/navigation_arrow_next_page_hovered.png");

    private record Page(String name, int pageIndex) {}

    private static final List<Page> ALL_PAGES = List.of(
            new Page("Welcome", 0),
            new Page("Mobs page 1", 1),
            new Page("Mobs page 2", 2),
            new Page("Mobs page 3", 3),
            new Page("Bosses page 1", 4),
            new Page("Bosses page 2", 5),
            new Page("Items page 1", 6),
            new Page("Items page 2", 7),
            new Page("Items page 3", 8),
            new Page("Items page 4", 9),
            new Page("Items page 5", 10),
            new Page("Armors page 1", 11),
            new Page("Armors page 2", 12),
            new Page("Armors page 3", 13),
            new Page("Armors page 4", 14),
            new Page("Tools page 1", 15),
            new Page("Tools page 2", 16),
            new Page("Tools page 3", 17),
            new Page("Tools page 4", 18),
            new Page("Tools page 5", 19),
            new Page("Utilities page 1", 20),
            new Page("Utilities page 2", 21),
            new Page("Utilities page 3", 22),
            new Page("Utilities page 4", 23),
            new Page("Utilities page 5", 24));

    private record Tab(String name, int color, int pageIndex) {}

    private static final List<Tab> TABS_LEFT = List.of(
            new Tab("Mobs",    0xFF4CAF50, 1),
            new Tab("Bosses",  0xFFE53935, 4),
            new Tab("Items",   0xFFFFB300, 6));

    private static final List<Tab> TABS_RIGHT = List.of(
            new Tab("Armors",    0xFF1E88E5, 11),
            new Tab("Tools",     0xFFFF6D00, 15),
            new Tab("Utilities", 0xFF8E24AA, 20));

    // book position
    private int bookLeft, bookRight, bookTop, bookBottom;
    // navigation arrows position
    private int navigationArrowTop;
    private int navigationArrowBottom;
    private int leftNavigationArrowLeft;
    private int leftNavigationArrowRight;
    private int rightNavigationArrowLeft;
    private int rightNavigationArrowRight;

    // book dimensions
    private static final int BOOK_TEXTURE_WIDTH  = 384;
    private static final int BOOK_TEXTURE_HEIGHT = 192;
    // tabs dimensions
    private static final int TAB_WIDTH  = 18;
    private static final int TAB_HEIGHT = 36;
    private static final int TAB_GAP    = 10;
    // navigation arrow dimension
    private static final int NAVIGATION_ARROW_SIZE = 20;

    // state
    private static final int PAGE_SUMMARY_INDEX = 0;
    private int currentPage = PAGE_SUMMARY_INDEX;

    public GuideBookScreen() {super(Component.empty());}

    @Override protected void init()
    {
        super.init();
        this.bookLeft   = (this.width  - BOOK_TEXTURE_WIDTH)  / 2;
        this.bookTop    = (this.height - BOOK_TEXTURE_HEIGHT) / 2;
        this.bookRight  = this.bookLeft + BOOK_TEXTURE_WIDTH;
        this.bookBottom = this.bookTop  + BOOK_TEXTURE_HEIGHT;
        this.navigationArrowBottom    = this.bookBottom - 5;
        this.navigationArrowTop       = this.navigationArrowBottom - NAVIGATION_ARROW_SIZE;
        this.leftNavigationArrowLeft  = this.bookLeft + 5;
        this.leftNavigationArrowRight = this.leftNavigationArrowLeft + NAVIGATION_ARROW_SIZE;
        this.rightNavigationArrowRight = this.bookRight - 5;
        this.rightNavigationArrowLeft  = this.rightNavigationArrowRight - NAVIGATION_ARROW_SIZE;
    }

    @Override public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick)
    {
        if (this.isHoveringPrevArrow(event.x(), event.y())) {this.navigateToPreviousPage(); return true;}
        if (this.isHoveringNextArrow(event.x(), event.y())) {this.navigateToNextPage();     return true;}

        for (int i = 0; i < TABS_LEFT.size(); i++)
            if (this.isHoveringTab(event.x(), event.y(), i, true))  {this.navigateToTab(TABS_LEFT.get(i));  return true;}
        for (int i = 0; i < TABS_RIGHT.size(); i++)
            if (this.isHoveringTab(event.x(), event.y(), i, false)) {this.navigateToTab(TABS_RIGHT.get(i)); return true;}

        return super.mouseClicked(event, doubleClick);
    }

    private boolean isHoveringPrevArrow(double mouseX, double mouseY)
    {
        return mouseX >= this.leftNavigationArrowLeft && mouseX <= this.leftNavigationArrowRight
                && mouseY >= this.navigationArrowTop      && mouseY <= this.navigationArrowBottom;
    }

    private boolean isHoveringNextArrow(double mouseX, double mouseY)
    {
        return mouseX >= this.rightNavigationArrowLeft && mouseX <= this.rightNavigationArrowRight
                && mouseY >= this.navigationArrowTop       && mouseY <= this.navigationArrowBottom;
    }

    private boolean isHoveringTab(double mouseX, double mouseY, int index, boolean isLeft)
    {
        int[] pos = getTabPos(index, isLeft);
        return mouseX >= pos[0] && mouseX <= pos[0] + TAB_WIDTH
                && mouseY >= pos[1] && mouseY <= pos[1] + TAB_HEIGHT;
    }

    @Override public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick)
    {
        graphics.blit(RenderPipelines.GUI_TEXTURED, BOOK_TEXTURE,
                this.bookLeft, this.bookTop, 0f, 0f,
                BOOK_TEXTURE_WIDTH, BOOK_TEXTURE_HEIGHT,
                BOOK_TEXTURE_WIDTH, BOOK_TEXTURE_HEIGHT);

        for (int i = 0; i < TABS_LEFT.size();  i++) {this.renderTab(graphics, i, true,  mouseX, mouseY);}
        for (int i = 0; i < TABS_RIGHT.size(); i++) {this.renderTab(graphics, i, false, mouseX, mouseY);}

        this.renderPageContent(graphics);
        this.renderNavigationButtons(graphics, mouseX, mouseY);

        super.extractBackground(graphics, mouseX, mouseY, partialTick);
    }

    private void renderTab(GuiGraphicsExtractor graphics, int index, boolean isLeft, int mouseX, int mouseY)
    {
        int[] pos    = getTabPos(index, isLeft);
        int   x      = pos[0];
        int   y      = pos[1];
        Tab   tab    = isLeft ? TABS_LEFT.get(index) : TABS_RIGHT.get(index);
        boolean hovered = isHoveringTab(mouseX, mouseY, index, isLeft);

        int tabWidth = TAB_WIDTH + (hovered ? 4 : 0);
        int xDraw    = isLeft ? x - (hovered ? 4 : 0) : x;

        graphics.fill(xDraw, y, xDraw + tabWidth, y + TAB_HEIGHT, tab.color());
        graphics.fill(xDraw,              y,                xDraw + tabWidth, y + 1,              0xFF1A1A1A);
        graphics.fill(xDraw,              y + TAB_HEIGHT-1, xDraw + tabWidth, y + TAB_HEIGHT,     0xFF1A1A1A);
        graphics.fill(xDraw,              y,                xDraw + 1,        y + TAB_HEIGHT,     0xFF1A1A1A);
        graphics.fill(xDraw + tabWidth-1, y,                xDraw + tabWidth, y + TAB_HEIGHT,     0xFF1A1A1A);

        if (hovered)
        {
            int textX     = isLeft ? xDraw - this.font.width(tab.name()) - 5 : xDraw + tabWidth + 3;
            int textY     = y + (TAB_HEIGHT - 8) / 2;
            int textWidth = this.font.width(tab.name()) + 6;
            graphics.fill(textX - 3, textY - 2, textX + textWidth, textY + 10, 0xCC000000);
            graphics.text(this.font, Component.literal(tab.name()), textX, textY, 0xFFFFFFFF, false);
        }
    }

    private int[] getTabPos(int tabIndex, boolean isLeft)
    {
        int totalH = TABS_LEFT.size() * TAB_HEIGHT + (TABS_LEFT.size() - 1) * TAB_GAP;
        int startY = bookTop + (BOOK_TEXTURE_HEIGHT - totalH) / 2;
        int y      = startY + tabIndex * (TAB_HEIGHT + TAB_GAP);
        int x      = isLeft ? bookLeft - TAB_WIDTH : bookRight;
        return new int[]{x, y};
    }

    private void renderPageContent(GuiGraphicsExtractor graphics)
    {
        Page currentPageObj = null;
        for (Page page : ALL_PAGES) if (page.pageIndex() == this.currentPage) currentPageObj = page;
        if (currentPageObj == null) return;

        // Titre centré sur la page gauche
        String pageTitle = "- " + currentPageObj.name() + " -";
        int titleX = bookLeft + (BOOK_TEXTURE_WIDTH / 2 - this.font.width(pageTitle)) / 2;
        graphics.text(this.font, Component.literal(pageTitle), titleX, bookTop + 20, 0xFF5C3A1E, false);

        String pageText = this.currentPage == 0 ? "Click on a tab to start exploring ! Et licet quocumque oculos flexeris feminas adfatim multas spectare cirratas, quibus, si nupsissent, per aetatem ter iam nixus poterat suppetere liberorum, ad usque taedium pedibus pavimenta tergentes iactari volucriter gyris, dum exprimunt innumera simulacra, quae finxere fabulae theatrales.\n" +
                "\n" +
                "Hanc regionem praestitutis celebritati diebus invadere parans dux ante edictus per solitudines Aboraeque amnis herbidas ripas, suorum indicio proditus, qui admissi flagitii metu exagitati ad praesidia descivere Romana. absque ullo egressus effectu deinde tabescebat immobilis." : "WIP";
        int maxWidth   = (BOOK_TEXTURE_WIDTH / 2) - 45;
        int lineHeight = this.font.lineHeight + 2;
        int maxLines   = (BOOK_TEXTURE_HEIGHT - 65) / lineHeight; // lignes disponibles par page

        List<String> lines = wrapText(pageText, maxWidth);

        // Page gauche
        int textX = bookLeft + 20;
        int textY = bookTop + 45;
        int rendered = 0;
        for (int i = 0; i < lines.size() && rendered < maxLines; i++)
        {
            graphics.text(this.font, Component.literal(lines.get(i)), textX, textY, 0xFF7A5C3A, false);
            textY += lineHeight;
            rendered++;
        }

        // Page droite si débordement
        if (rendered < lines.size())
        {
            int rightX = bookLeft + BOOK_TEXTURE_WIDTH / 2 + 25;
            int rightY = bookTop + 20;
            for (int i = rendered; i < lines.size() && i - rendered < maxLines; i++)
            {
                graphics.text(this.font, Component.literal(lines.get(i)), rightX, rightY, 0xFF7A5C3A, false);
                rightY += lineHeight;
            }
        }
    }

    private void renderNavigationButtons(GuiGraphicsExtractor graphics, int mouseX, int mouseY)
    {
        if (this.currentPage != 0)
        {
            Identifier tex = this.isHoveringPrevArrow(mouseX, mouseY)
                    ? NAVIGATION_ARROW_PREVIOUS_PAGE_HOVERED : NAVIGATION_ARROW_PREVIOUS_PAGE;
            graphics.blit(RenderPipelines.GUI_TEXTURED, tex,
                    this.leftNavigationArrowLeft, this.navigationArrowTop,
                    0f, 0f, NAVIGATION_ARROW_SIZE, NAVIGATION_ARROW_SIZE,
                    NAVIGATION_ARROW_SIZE, NAVIGATION_ARROW_SIZE);
        }

        if (this.currentPage != ALL_PAGES.size() - 1)
        {
            Identifier tex = this.isHoveringNextArrow(mouseX, mouseY)
                    ? NAVIGATION_ARROW_NEXT_PAGE_HOVERED : NAVIGATION_ARROW_NEXT_PAGE;
            graphics.blit(RenderPipelines.GUI_TEXTURED, tex,
                    this.rightNavigationArrowLeft, this.navigationArrowTop,
                    0f, 0f, NAVIGATION_ARROW_SIZE, NAVIGATION_ARROW_SIZE,
                    NAVIGATION_ARROW_SIZE, NAVIGATION_ARROW_SIZE);
        }
    }

    private void navigateToTab(Tab tab)          {this.currentPage = tab.pageIndex();}
    private void navigateToPage(Page page)        {this.currentPage = page.pageIndex();}

    private void navigateToPreviousPage()
    {
        int nextIndex = getCurrentIndex() - 1;
        if (nextIndex >= 0 && nextIndex < ALL_PAGES.size()) {this.navigateToPage(ALL_PAGES.get(nextIndex));}
    }

    private void navigateToNextPage()
    {
        int nextIndex = getCurrentIndex() + 1;
        if (nextIndex >= 0 && nextIndex < ALL_PAGES.size()) {this.navigateToPage(ALL_PAGES.get(nextIndex));}
    }

    private int getCurrentIndex()
    {
        for (int i = 0; i < ALL_PAGES.size(); i++)
            if (ALL_PAGES.get(i).pageIndex() == this.currentPage) return i;
        return -1;
    }

    private List<String> wrapText(String text, int maxWidth)
    {
        List<String> lines = new ArrayList<>();
        String[] words = text.split(" ");
        StringBuilder current = new StringBuilder();
        for (String word : words)
        {
            String test = current.isEmpty() ? word : current + " " + word;
            if (this.font.width(test) <= maxWidth)
                current = new StringBuilder(test);
            else
            {
                if (!current.isEmpty()) lines.add(current.toString());
                current = new StringBuilder(word);
            }
        }
        if (!current.isEmpty()) lines.add(current.toString());
        return lines;
    }

    @Override public boolean isPauseScreen() {return false;}
}