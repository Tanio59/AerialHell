package fr.factionbedrock.aerialhell.Client.Gui.Screen.Inventory;

import fr.factionbedrock.aerialhell.AerialHell;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.util.List;

public class AerialHellBookScreen extends Screen
{
    private static final Identifier BOOK_TEXTURE = Identifier.fromNamespaceAndPath(AerialHell.MODID, "textures/gui/book.png");

    // ── Pages ─────────────────────────────────────────────────────
    private static final int PAGE_SUMMARY = 0;

    // ── Tabs ──────────────────────────────────────────────────────
    private record Tab(String name, int color, int page) {}

    private static final List<Tab> TABS_LEFT = List.of(
            new Tab("Mobs",  0xFF4CAF50, 1),
            new Tab("Boss",  0xFFE53935, 2),
            new Tab("Items", 0xFFFFB300, 3)
    );

    private static final List<Tab> TABS_RIGHT = List.of(
            new Tab("Armures",  0xFF1E88E5, 4),
            new Tab("Armes",    0xFFFF6D00, 5),
            new Tab("Utilités", 0xFF8E24AA, 6)
    );

    // ── Dimensions ────────────────────────────────────────────────
    private static final int BOOK_W  = 400;
    private static final int BOOK_H  = 200;
    private static final int TAB_W   = 18;
    private static final int TAB_H   = 36;
    private static final int TAB_GAP = 10;

    // ── État ──────────────────────────────────────────────────────
    private int bookLeft, bookTop;
    private int hoveredTab  = -1;
    private int currentPage = PAGE_SUMMARY;

    public AerialHellBookScreen() { super(Component.empty()); }

    @Override
    protected void init()
    {
        super.init();
        bookLeft = (this.width  - BOOK_W) / 2;
        bookTop  = (this.height - BOOK_H) / 2;
    }

    // ── Input ─────────────────────────────────────────────────────

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick)
    {
        for (int i = 0; i < TABS_LEFT.size(); i++)
        {
            if (isHoveringTab(event.x(), event.y(), i, true))
            {
                currentPage = TABS_LEFT.get(i).page();
                return true;
            }
        }
        for (int i = 0; i < TABS_RIGHT.size(); i++)
        {
            if (isHoveringTab(event.x(), event.y(), i, false))
            {
                currentPage = TABS_RIGHT.get(i).page();
                return true;
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    // Remplace mouseMoved par :
    @Override
    public void afterMouseMove()
    {
        double mouseX = this.minecraft.mouseHandler.xpos();
        double mouseY = this.minecraft.mouseHandler.ypos();
        hoveredTab = -1;
        for (int i = 0; i < TABS_LEFT.size(); i++)
            if (isHoveringTab(mouseX, mouseY, i, true))  { hoveredTab = i;       return; }
        for (int i = 0; i < TABS_RIGHT.size(); i++)
            if (isHoveringTab(mouseX, mouseY, i, false)) { hoveredTab = 100 + i; return; }
    }

    private boolean isHoveringTab(double mx, double my, int index, boolean isLeft)
    {
        int[] pos = getTabPos(index, isLeft);
        return mx >= pos[0] && mx <= pos[0] + TAB_W && my >= pos[1] && my <= pos[1] + TAB_H;
    }

    // ── Rendu ─────────────────────────────────────────────────────

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick)
    {
        graphics.blit(RenderPipelines.GUI_TEXTURED, BOOK_TEXTURE,
                bookLeft, bookTop, 0f, 0f, BOOK_W, BOOK_H, 512, 256);

        for (int i = 0; i < TABS_LEFT.size(); i++)
            renderTab(graphics, i, true, mouseX, mouseY);

        for (int i = 0; i < TABS_RIGHT.size(); i++)
            renderTab(graphics, i, false, mouseX, mouseY);

        renderPageContent(graphics);

        super.extractBackground(graphics, mouseX, mouseY, partialTick);
    }

    private void renderTab(GuiGraphicsExtractor graphics, int index, boolean isLeft, int mx, int my)
    {
        int[]   pos     = getTabPos(index, isLeft);
        int     x       = pos[0];
        int     y       = pos[1];
        Tab     tab     = isLeft ? TABS_LEFT.get(index) : TABS_RIGHT.get(index);
        boolean hovered = isHoveringTab(mx, my, index, isLeft);

        int w     = hovered ? TAB_W + 4 : TAB_W;
        int xDraw = isLeft ? x - (hovered ? 4 : 0) : x;

        graphics.fill(xDraw, y, xDraw + w, y + TAB_H, tab.color());

        // Bordure
        graphics.fill(xDraw,         y,            xDraw + w,     y + 1,          0xFF1A1A1A);
        graphics.fill(xDraw,         y + TAB_H - 1, xDraw + w,   y + TAB_H,       0xFF1A1A1A);
        graphics.fill(xDraw,         y,            xDraw + 1,     y + TAB_H,       0xFF1A1A1A);
        graphics.fill(xDraw + w - 1, y,            xDraw + w,     y + TAB_H,       0xFF1A1A1A);

        // Tooltip au hover
        if (hovered)
        {
            int textX = isLeft ? xDraw - this.font.width(tab.name()) - 4 : xDraw + w + 4;
            int textY = y + (TAB_H - 8) / 2;
            int tw    = this.font.width(tab.name()) + 6;

            graphics.fill(textX - 3, textY - 2, textX + tw, textY + 10, 0xCC000000);
            graphics.text(this.font, Component.literal(tab.name()), textX, textY, 0xFFFFFFFF);
        }
    }

    private int[] getTabPos(int index, boolean isLeft)
    {
        int totalH = TABS_LEFT.size() * TAB_H + (TABS_LEFT.size() - 1) * TAB_GAP;
        int startY = bookTop + (BOOK_H - totalH) / 2;
        int y      = startY + index * (TAB_H + TAB_GAP);
        int x      = isLeft ? bookLeft - TAB_W : bookLeft + BOOK_W;
        return new int[]{x, y};
    }

    private void renderPageContent(GuiGraphicsExtractor graphics)
    {
        if (currentPage == PAGE_SUMMARY)
        {
            String title = "✦ Bienvenue ✦";
            graphics.text(this.font, Component.literal(title),
                    bookLeft + (BOOK_W / 2 - this.font.width(title)) / 2,
                    bookTop + 20, 0xFF5C3A1E);

            String sub = "Clique sur un marque-page pour explorer.";
            graphics.text(this.font, Component.literal(sub),
                    bookLeft + (BOOK_W / 2 - this.font.width(sub)) / 2,
                    bookTop + 40, 0xFF7A5C3A);
        }
        else
        {
            Tab active = null;
            for (Tab t : TABS_LEFT)  if (t.page() == currentPage) active = t;
            for (Tab t : TABS_RIGHT) if (t.page() == currentPage) active = t;

            if (active != null)
            {
                String title = "✦ " + active.name() + " ✦";
                graphics.text(this.font, Component.literal(title),
                        bookLeft + (BOOK_W / 2 - this.font.width(title)) / 2,
                        bookTop + 20, 0xFF5C3A1E);

                graphics.text(this.font, Component.literal("(contenu à venir)"),
                        bookLeft + 20, bookTop + 45, 0xFF7A5C3A);
            }
        }
    }

    @Override
    public boolean isPauseScreen() { return false; }
}