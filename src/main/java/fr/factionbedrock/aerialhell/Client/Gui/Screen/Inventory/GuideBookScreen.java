package fr.factionbedrock.aerialhell.Client.Gui.Screen.Inventory;

import fr.factionbedrock.aerialhell.AerialHell;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class GuideBookScreen extends Screen
{
    private static final Identifier BOOK_TEXTURE                           = Identifier.fromNamespaceAndPath(AerialHell.MODID, "textures/gui/guide_book/guide_book_page.png");
    private static final Identifier NAVIGATION_ARROW_PREVIOUS_PAGE         = Identifier.fromNamespaceAndPath(AerialHell.MODID, "textures/gui/guide_book/navigation_arrow_previous_page.png");
    private static final Identifier NAVIGATION_ARROW_PREVIOUS_PAGE_HOVERED = Identifier.fromNamespaceAndPath(AerialHell.MODID, "textures/gui/guide_book/navigation_arrow_previous_page_hovered.png");
    private static final Identifier NAVIGATION_ARROW_NEXT_PAGE             = Identifier.fromNamespaceAndPath(AerialHell.MODID, "textures/gui/guide_book/navigation_arrow_next_page.png");
    private static final Identifier NAVIGATION_ARROW_NEXT_PAGE_HOVERED     = Identifier.fromNamespaceAndPath(AerialHell.MODID, "textures/gui/guide_book/navigation_arrow_next_page_hovered.png");

    private record Page(String name, int pageIndex) {}

    private static final List<Page> ALL_PAGES = List.of(
            new Page("Welcome",        0),
            new Page("Mobs page 1",    1),
            new Page("Mobs page 2",    2),
            new Page("Mobs page 3",    3),
            new Page("Bosses page 1",  4),
            new Page("Bosses page 2",  5),
            new Page("Items page 1",   6),
            new Page("Items page 2",   7),
            new Page("Items page 3",   8),
            new Page("Items page 4",   9),
            new Page("Items page 5",  10),
            new Page("Armors page 1", 11),
            new Page("Armors page 2", 12),
            new Page("Armors page 3", 13),
            new Page("Armors page 4", 14),
            new Page("Tools page 1",  15),
            new Page("Tools page 2",  16),
            new Page("Tools page 3",  17),
            new Page("Tools page 4",  18),
            new Page("Tools page 5",  19),
            new Page("Utilities page 1", 20),
            new Page("Utilities page 2", 21),
            new Page("Utilities page 3", 22),
            new Page("Utilities page 4", 23),
            new Page("Utilities page 5", 24),
            new Page("Mini-Game",     25));

    private record Tab(String name, int color, int pageIndex) {}

    private static final List<Tab> TABS_LEFT = List.of(
            new Tab("Mobs",    0xFF4CAF50, 1),
            new Tab("Bosses",  0xFFE53935, 4),
            new Tab("Items",   0xFFFFB300, 6));

    private static final List<Tab> TABS_RIGHT = List.of(
            new Tab("Armors",    0xFF1E88E5, 11),
            new Tab("Tools",     0xFFFF6D00, 15),
            new Tab("Utilities", 0xFF8E24AA, 20));

    // ── Positions livre ───────────────────────────────────────────
    private int bookLeft, bookRight, bookTop, bookBottom, leftPageLeft;
    private int navigationArrowTop, navigationArrowBottom;
    private int leftNavigationArrowLeft, leftNavigationArrowRight;
    private int rightNavigationArrowLeft, rightNavigationArrowRight;

    // ── Dimensions livre ──────────────────────────────────────────
    private static final int BOOK_TEXTURE_WIDTH    = 384;
    private static final int BOOK_TEXTURE_HEIGHT   = 192;
    private static final int TAB_WIDTH             = 18;
    private static final int TAB_HEIGHT            = 36;
    private static final int TAB_GAP               = 10;
    private static final int NAVIGATION_ARROW_SIZE = 20;

    // ── Dimensions page ───────────────────────────────────────────
    private int firstLineY;
    private int leftPageLineX, rightPageLineX;
    private int leftPageCenterX, rightPageCenterX;
    private static final int LINE_HEIGHT                  = 10;
    private static final int MARGIN_WIDTH                 = 10;
    private static final int LINE_WIDTH                   = 178;
    private static final int LINE_WIDTH_NO_MARGIN         = LINE_WIDTH - 2 * MARGIN_WIDTH;
    private static final int MAX_LINES_PER_VISUAL_PAGE    = 17;
    private static final int MAX_LINES_PER_TECHNICAL_PAGE = MAX_LINES_PER_VISUAL_PAGE * 2;
    private static final float TEXT_SCALE                 = 0.8f;

    private record Line(int index, int startX, int centerX, int startY)
    {
        private int centerX(String textToCenter, Font font) {return this.centerX - font.width(textToCenter) / 2;}
    }
    private List<Line> Lines = new ArrayList<>();

    // ── État navigation ───────────────────────────────────────────
    private static final int PAGE_SUMMARY_INDEX = 0;
    private int currentPage = PAGE_SUMMARY_INDEX;

    // ── Tic Tac Toe ───────────────────────────────────────────────
    private static final int TTT_PAGE    = 25;
    private static final int CELL_SIZE   = 36;
    private static final int GRID_SIZE   = CELL_SIZE * 3;

    // 0 = vide, 1 = X, 2 = O
    private final int[] tttBoard      = new int[9];
    private int  tttCurrentPlayer     = 1; // 1 = X, 2 = O
    private int  tttWinner            = 0; // 0 = en cours, 1 = X gagne, 2 = O gagne, 3 = nul
    private int  tttGridLeft, tttGridTop; // position de la grille (calculée dans init)

    // Bouton reset
    private int resetBtnX, resetBtnY, resetBtnW, resetBtnH;

    public GuideBookScreen() {super(Component.empty());}

    @Override protected void init()
    {
        super.init();
        this.bookLeft   = (this.width  - BOOK_TEXTURE_WIDTH)  / 2;
        this.bookTop    = (this.height - BOOK_TEXTURE_HEIGHT) / 2;
        this.bookRight  = this.bookLeft + BOOK_TEXTURE_WIDTH;
        this.bookBottom = this.bookTop  + BOOK_TEXTURE_HEIGHT;
        this.navigationArrowBottom     = this.bookBottom - 5;
        this.navigationArrowTop        = this.navigationArrowBottom - NAVIGATION_ARROW_SIZE;
        this.leftNavigationArrowLeft   = this.bookLeft + 5;
        this.leftNavigationArrowRight  = this.leftNavigationArrowLeft + NAVIGATION_ARROW_SIZE;
        this.rightNavigationArrowRight = this.bookRight - 5;
        this.rightNavigationArrowLeft  = this.rightNavigationArrowRight - NAVIGATION_ARROW_SIZE;

        this.leftPageLeft     = this.bookLeft + 206;
        this.firstLineY       = this.bookTop  + 9;
        this.leftPageLineX    = this.bookLeft + MARGIN_WIDTH;
        this.rightPageLineX   = this.leftPageLeft + MARGIN_WIDTH;
        this.leftPageCenterX  = this.leftPageLineX  + LINE_WIDTH_NO_MARGIN / 2;
        this.rightPageCenterX = this.rightPageLineX + LINE_WIDTH_NO_MARGIN / 2;

        this.Lines.clear();
        for (int lineIndex = 0; lineIndex < MAX_LINES_PER_TECHNICAL_PAGE; lineIndex++)
        {
            boolean isLeft = lineIndex < MAX_LINES_PER_VISUAL_PAGE;
            this.Lines.add(new Line(
                    lineIndex,
                    isLeft ? this.leftPageLineX   : this.rightPageLineX,
                    isLeft ? this.leftPageCenterX : this.rightPageCenterX,
                    this.firstLineY + (lineIndex % MAX_LINES_PER_VISUAL_PAGE) * LINE_HEIGHT));
        }

        // Grille TTT centrée sur la page gauche
        int pageContentCenterX = this.leftPageLineX + LINE_WIDTH_NO_MARGIN / 2;
        int pageContentCenterY = this.bookTop + BOOK_TEXTURE_HEIGHT / 2 + 5;
        this.tttGridLeft = pageContentCenterX - GRID_SIZE / 2;
        this.tttGridTop  = pageContentCenterY - GRID_SIZE / 2;

        // Bouton reset sous la grille
        this.resetBtnW = 50;
        this.resetBtnH = 10;
        this.resetBtnX = pageContentCenterX - resetBtnW / 2;
        this.resetBtnY = tttGridTop + GRID_SIZE + 8;
    }

    // ── Input ─────────────────────────────────────────────────────

    @Override public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick)
    {
        double mx = event.x();
        double my = event.y();

        // Tic Tac Toe : clics sur la grille
        if (this.currentPage == TTT_PAGE)
        {
            // Bouton reset
            if (mx >= resetBtnX && mx <= resetBtnX + resetBtnW
                    && my >= resetBtnY && my <= resetBtnY + resetBtnH)
            {
                tttReset();
                return true;
            }

            // Clic sur une cellule
            if (tttWinner == 0 && mx >= tttGridLeft && mx <= tttGridLeft + GRID_SIZE
                    && my >= tttGridTop && my <= tttGridTop + GRID_SIZE)
            {
                int col = (int)((mx - tttGridLeft) / CELL_SIZE);
                int row = (int)((my - tttGridTop)  / CELL_SIZE);
                int idx = row * 3 + col;
                if (idx >= 0 && idx < 9 && tttBoard[idx] == 0)
                {
                    tttBoard[idx] = tttCurrentPlayer;
                    tttWinner = tttCheckWinner();
                    if (tttWinner == 0) tttCurrentPlayer = tttCurrentPlayer == 1 ? 2 : 1;
                    return true;
                }
            }
        }

        if (this.isHoveringPrevArrow(mx, my)) {this.navigateToPreviousPage(); return true;}
        if (this.isHoveringNextArrow(mx, my)) {this.navigateToNextPage();     return true;}

        for (int i = 0; i < TABS_LEFT.size();  i++)
            if (this.isHoveringTab(mx, my, i, true))  {this.navigateToTab(TABS_LEFT.get(i));  return true;}
        for (int i = 0; i < TABS_RIGHT.size(); i++)
            if (this.isHoveringTab(mx, my, i, false)) {this.navigateToTab(TABS_RIGHT.get(i)); return true;}

        return super.mouseClicked(event, doubleClick);
    }

    private boolean isHoveringPrevArrow(double mx, double my)
    {
        return mx >= leftNavigationArrowLeft && mx <= leftNavigationArrowRight
                && my >= navigationArrowTop      && my <= navigationArrowBottom;
    }

    private boolean isHoveringNextArrow(double mx, double my)
    {
        return mx >= rightNavigationArrowLeft && mx <= rightNavigationArrowRight
                && my >= navigationArrowTop       && my <= navigationArrowBottom;
    }

    private boolean isHoveringTab(double mx, double my, int index, boolean isLeft)
    {
        int[] pos = getTabPos(index, isLeft);
        return mx >= pos[0] && mx <= pos[0] + TAB_WIDTH
                && my >= pos[1] && my <= pos[1] + TAB_HEIGHT;
    }

    // ── Rendu ─────────────────────────────────────────────────────

    @Override public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick)
    {
        graphics.blit(RenderPipelines.GUI_TEXTURED, BOOK_TEXTURE,
                bookLeft, bookTop, 0f, 0f,
                BOOK_TEXTURE_WIDTH, BOOK_TEXTURE_HEIGHT,
                BOOK_TEXTURE_WIDTH, BOOK_TEXTURE_HEIGHT);

        for (int i = 0; i < TABS_LEFT.size();  i++) this.renderTab(graphics, i, true,  mouseX, mouseY);
        for (int i = 0; i < TABS_RIGHT.size(); i++) this.renderTab(graphics, i, false, mouseX, mouseY);

        this.renderPageContent(graphics, mouseX, mouseY);
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

    // ── Texte réduit ──────────────────────────────────────────────

    private void renderSmallText(GuiGraphicsExtractor graphics, String text, int x, int y, int color)
    {
        var pose = graphics.pose();
        pose.pushMatrix();
        pose.translate(x, y);
        pose.scale(TEXT_SCALE, TEXT_SCALE);
        graphics.text(this.font, Component.literal(text), 0, 0, color, false);
        pose.popMatrix();
    }

    // ── Blacklist de lignes ───────────────────────────────────────

    private Set<Integer> getBlacklistedLines()
    {
        return Set.of();
    }

    // ── Contenu des pages ─────────────────────────────────────────

    private void renderPageContent(GuiGraphicsExtractor graphics, int mouseX, int mouseY)
    {
        Page currentPageObj = null;
        for (Page page : ALL_PAGES) if (page.pageIndex() == this.currentPage) currentPageObj = page;
        if (currentPageObj == null) return;

        if (this.currentPage == TTT_PAGE)
        {
            renderTicTacToe(graphics, mouseX, mouseY);
            return;
        }

        int currentLineIndex = 0;

        // Titre centré
        String pageTitle = "- " + currentPageObj.name() + " -";
        graphics.text(this.font, Component.literal(pageTitle),
                Lines.get(currentLineIndex).centerX(pageTitle, this.font),
                Lines.get(currentLineIndex).startY(),
                0xFF5C3A1E, false);
        currentLineIndex++;

        // Contenu texte
        String pageText = this.currentPage == 0
                ? "Click on a tab to start exploring !"
                : "WIP";

        Set<Integer>  blacklist = getBlacklistedLines();
        List<String>  textLines = this.wrapText(pageText, (int)(LINE_WIDTH_NO_MARGIN / TEXT_SCALE));

        for (int i = 0; i < textLines.size() && currentLineIndex < MAX_LINES_PER_TECHNICAL_PAGE - 1; i++)
        {
            currentLineIndex++;
            while (blacklist.contains(currentLineIndex) && currentLineIndex < MAX_LINES_PER_TECHNICAL_PAGE - 1)
                currentLineIndex++;
            if (currentLineIndex >= MAX_LINES_PER_TECHNICAL_PAGE) break;

            renderSmallText(graphics, textLines.get(i),
                    Lines.get(currentLineIndex).startX,
                    Lines.get(currentLineIndex).startY,
                    0xFF7A5C3A);
        }
    }

    // ── Tic Tac Toe ───────────────────────────────────────────────

    private void renderTicTacToe(GuiGraphicsExtractor graphics, int mouseX, int mouseY)
    {
        int GX = tttGridLeft;
        int GY = tttGridTop;
        int C  = CELL_SIZE;

        // Titre
        String title = "- Mini-Game -";
        graphics.text(this.font, Component.literal(title),
                leftPageCenterX - this.font.width(title) / 2,
                bookTop + 12, 0xFF5C3A1E, false);

        // Tour du joueur / résultat
        String status;
        if      (tttWinner == 1) status = "X wins !";
        else if (tttWinner == 2) status = "O wins !";
        else if (tttWinner == 3) status = "Draw !";
        else                     status = (tttCurrentPlayer == 1 ? "X" : "O") + "'s turn";
        graphics.text(this.font, Component.literal(status),
                leftPageCenterX - this.font.width(status) / 2,
                bookTop + 25, tttWinner > 0 ? 0xFFCC0000 : 0xFF5C3A1E, false);

        // Fond de la grille
        graphics.fill(GX - 1, GY - 1, GX + GRID_SIZE + 1, GY + GRID_SIZE + 1, 0xFF5C3A1E);
        graphics.fill(GX, GY, GX + GRID_SIZE, GY + GRID_SIZE, 0xFFE8D8B0);

        // Lignes de la grille
        // Verticales
        graphics.fill(GX + C,     GY, GX + C + 2,     GY + GRID_SIZE, 0xFF5C3A1E);
        graphics.fill(GX + C*2,   GY, GX + C*2 + 2,   GY + GRID_SIZE, 0xFF5C3A1E);
        // Horizontales
        graphics.fill(GX, GY + C,   GX + GRID_SIZE, GY + C + 2,   0xFF5C3A1E);
        graphics.fill(GX, GY + C*2, GX + GRID_SIZE, GY + C*2 + 2, 0xFF5C3A1E);

        // Hover sur cellule
        if (tttWinner == 0)
        {
            int hCol = (int)((mouseX - GX) / (double)C);
            int hRow = (int)((mouseY - GY) / (double)C);
            if (hCol >= 0 && hCol < 3 && hRow >= 0 && hRow < 3 && tttBoard[hRow * 3 + hCol] == 0)
                graphics.fill(GX + hCol * C + 1, GY + hRow * C + 1,
                        GX + hCol * C + C - 1, GY + hRow * C + C - 1, 0x33000000);
        }

        // Contenu des cellules
        for (int row = 0; row < 3; row++)
        {
            for (int col = 0; col < 3; col++)
            {
                int val   = tttBoard[row * 3 + col];
                int cellX = GX + col * C;
                int cellY = GY + row * C;
                int cx    = cellX + C / 2;
                int cy    = cellY + C / 2;

                if (val == 1) // X
                {
                    int pad = 6;
                    // Diagonale 1
                    drawLine(graphics, cellX + pad, cellY + pad, cellX + C - pad, cellY + C - pad, 0xFFCC2200, 2);
                    // Diagonale 2
                    drawLine(graphics, cellX + C - pad, cellY + pad, cellX + pad, cellY + C - pad, 0xFFCC2200, 2);
                }
                else if (val == 2) // O
                {
                    int r   = C / 2 - 6;
                    int th  = 2;
                    drawCircle(graphics, cx, cy, r, th, 0xFF0044CC);
                }
            }
        }

        // Ligne gagnante
        if (tttWinner == 1 || tttWinner == 2)
            drawWinningLine(graphics);

        // Bouton Reset
        boolean resetHovered = mouseX >= resetBtnX && mouseX <= resetBtnX + resetBtnW
                && mouseY >= resetBtnY && mouseY <= resetBtnY + resetBtnH;
        graphics.fill(resetBtnX, resetBtnY, resetBtnX + resetBtnW, resetBtnY + resetBtnH,
                resetHovered ? 0xFF7A5C3A : 0xFF5C3A1E);
        String resetLabel = "Reset";
        graphics.text(this.font, Component.literal(resetLabel),
                resetBtnX + (resetBtnW - this.font.width(resetLabel)) / 2,
                resetBtnY + 1, 0xFFFFFFFF, false);
    }

    // Dessine une ligne épaisse pixel par pixel
    private void drawLine(GuiGraphicsExtractor g, int x0, int y0, int x1, int y1, int color, int thickness)
    {
        int dx = Math.abs(x1 - x0), dy = Math.abs(y1 - y0);
        int sx = x0 < x1 ? 1 : -1, sy = y0 < y1 ? 1 : -1;
        int err = dx - dy;
        int x = x0, y = y0;
        while (true)
        {
            g.fill(x - thickness/2, y - thickness/2, x + thickness/2 + 1, y + thickness/2 + 1, color);
            if (x == x1 && y == y1) break;
            int e2 = 2 * err;
            if (e2 > -dy) { err -= dy; x += sx; }
            if (e2 <  dx) { err += dx; y += sy; }
        }
    }

    // Dessine un cercle creux
    private void drawCircle(GuiGraphicsExtractor g, int cx, int cy, int r, int thickness, int color)
    {
        for (int angle = 0; angle < 360; angle++)
        {
            double rad = Math.toRadians(angle);
            for (int t = 0; t < thickness; t++)
            {
                int rx = (int)((r - t) * Math.cos(rad));
                int ry = (int)((r - t) * Math.sin(rad));
                g.fill(cx + rx, cy + ry, cx + rx + 1, cy + ry + 1, color);
            }
        }
    }

    // Dessine la ligne de victoire
    private void drawWinningLine(GuiGraphicsExtractor g)
    {
        int[][] wins = {
                {0,1,2}, {3,4,5}, {6,7,8}, // lignes
                {0,3,6}, {1,4,7}, {2,5,8}, // colonnes
                {0,4,8}, {2,4,6}            // diagonales
        };
        for (int[] combo : wins)
        {
            if (tttBoard[combo[0]] != 0
                    && tttBoard[combo[0]] == tttBoard[combo[1]]
                    && tttBoard[combo[1]] == tttBoard[combo[2]])
            {
                int c0 = combo[0], c2 = combo[2];
                int x0 = tttGridLeft + (c0 % 3) * CELL_SIZE + CELL_SIZE / 2;
                int y0 = tttGridTop  + (c0 / 3) * CELL_SIZE + CELL_SIZE / 2;
                int x1 = tttGridLeft + (c2 % 3) * CELL_SIZE + CELL_SIZE / 2;
                int y1 = tttGridTop  + (c2 / 3) * CELL_SIZE + CELL_SIZE / 2;
                drawLine(g, x0, y0, x1, y1, 0xFFFFD700, 3);
                return;
            }
        }
    }

    // ── Logique TTT ───────────────────────────────────────────────

    private void tttReset()
    {
        for (int i = 0; i < 9; i++) tttBoard[i] = 0;
        tttCurrentPlayer = 1;
        tttWinner        = 0;
    }

    private int tttCheckWinner()
    {
        int[][] wins = {
                {0,1,2}, {3,4,5}, {6,7,8},
                {0,3,6}, {1,4,7}, {2,5,8},
                {0,4,8}, {2,4,6}
        };
        for (int[] combo : wins)
            if (tttBoard[combo[0]] != 0
                    && tttBoard[combo[0]] == tttBoard[combo[1]]
                    && tttBoard[combo[1]] == tttBoard[combo[2]])
                return tttBoard[combo[0]];

        // Nul ?
        for (int cell : tttBoard) if (cell == 0) return 0;
        return 3;
    }

    // ── Navigation ────────────────────────────────────────────────

    private void renderNavigationButtons(GuiGraphicsExtractor graphics, int mouseX, int mouseY)
    {
        if (this.currentPage != 0)
        {
            Identifier tex = isHoveringPrevArrow(mouseX, mouseY)
                    ? NAVIGATION_ARROW_PREVIOUS_PAGE_HOVERED : NAVIGATION_ARROW_PREVIOUS_PAGE;
            graphics.blit(RenderPipelines.GUI_TEXTURED, tex,
                    leftNavigationArrowLeft, navigationArrowTop,
                    0f, 0f, NAVIGATION_ARROW_SIZE, NAVIGATION_ARROW_SIZE,
                    NAVIGATION_ARROW_SIZE, NAVIGATION_ARROW_SIZE);
        }
        if (this.currentPage != ALL_PAGES.size() - 1)
        {
            Identifier tex = isHoveringNextArrow(mouseX, mouseY)
                    ? NAVIGATION_ARROW_NEXT_PAGE_HOVERED : NAVIGATION_ARROW_NEXT_PAGE;
            graphics.blit(RenderPipelines.GUI_TEXTURED, tex,
                    rightNavigationArrowLeft, navigationArrowTop,
                    0f, 0f, NAVIGATION_ARROW_SIZE, NAVIGATION_ARROW_SIZE,
                    NAVIGATION_ARROW_SIZE, NAVIGATION_ARROW_SIZE);
        }
    }

    private void navigateToTab(Tab tab)    {this.currentPage = tab.pageIndex();}
    private void navigateToPage(Page page) {this.currentPage = page.pageIndex();}

    private void navigateToPreviousPage()
    {
        int idx = getCurrentIndex() - 1;
        if (idx >= 0 && idx < ALL_PAGES.size()) this.navigateToPage(ALL_PAGES.get(idx));
    }

    private void navigateToNextPage()
    {
        int idx = getCurrentIndex() + 1;
        if (idx >= 0 && idx < ALL_PAGES.size()) this.navigateToPage(ALL_PAGES.get(idx));
    }

    private int getCurrentIndex()
    {
        for (int i = 0; i < ALL_PAGES.size(); i++)
            if (ALL_PAGES.get(i).pageIndex() == this.currentPage) return i;
        return -1;
    }

    // ── Utilitaires texte ─────────────────────────────────────────

    private List<String> wrapText(String text, int maxWidth)
    {
        List<String> lines = new ArrayList<>();
        for (String paragraph : text.split("\n", -1))
        {
            String line = paragraph.replace("\t", "    ");
            if (line.isEmpty()) { lines.add(""); continue; }

            String[] words = line.split(" ");
            StringBuilder current = new StringBuilder();
            for (String word : words)
            {
                if (this.font.width(word) > maxWidth)
                {
                    if (!current.isEmpty()) { lines.add(current.toString()); current = new StringBuilder(); }
                    StringBuilder chunk = new StringBuilder();
                    for (char c : word.toCharArray())
                    {
                        if (this.font.width(chunk.toString() + c) > maxWidth)
                        {
                            lines.add(chunk.toString());
                            chunk = new StringBuilder();
                        }
                        chunk.append(c);
                    }
                    if (!chunk.isEmpty()) { lines.add(chunk.toString()); }
                    current = new StringBuilder();
                    continue;
                }

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
        }
        return lines;
    }

    @Override public boolean isPauseScreen() {return false;}
}