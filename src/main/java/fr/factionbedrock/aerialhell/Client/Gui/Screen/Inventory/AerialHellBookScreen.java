package fr.factionbedrock.aerialhell.Client.Gui.Screen.Inventory;

import fr.factionbedrock.aerialhell.AerialHell;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.multiplayer.ClientAdvancements;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.core.component.DataComponents;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class AerialHellBookScreen extends Screen
{
    private static final Identifier BOOK_TEXTURE = Identifier.fromNamespaceAndPath(AerialHell.MODID, "textures/gui/aerial_hell_book_page.png");
    private static final Identifier LOGO_TEXTURE = Identifier.fromNamespaceAndPath(AerialHell.MODID, "textures/gui/book/aerial_hell_logo.png");

    private static final int TEX_W        = 384;
    private static final int TEX_H        = 192;
    private static final int TEX_LINE_GAP = 10;
    private static final int TEX_LINE_Y0  = 16;

    private static final int LOGO_SRC_W = 1484;
    private static final int LOGO_SRC_H = 430;

    // ── Pages ─────────────────────────────────────────────────────
    private static final int PAGE_SUMMARY = 0;

    // ── Tabs ──────────────────────────────────────────────────────
    private record Tab(String name, int color, int page, String translationKey) {}

    private static final List<Tab> TABS_LEFT = List.of(
            new Tab("Mobs",  0xFF4CAF50, 1, "book.aerialhell.tab.mobs"),
            new Tab("Boss",  0xFFE53935, 2, "book.aerialhell.tab.boss"),
            new Tab("Items", 0xFFFFB300, 3, "book.aerialhell.tab.items")
    );

    private static final List<Tab> TABS_RIGHT = List.of(
            new Tab("Armures",  0xFF1E88E5, 4, "book.aerialhell.tab.armures"),
            new Tab("Armes",    0xFFFF6D00, 5, "book.aerialhell.tab.armes"),
            new Tab("Utilités", 0xFF8E24AA, 6, "book.aerialhell.tab.utilities")
    );

    private static final int TAB_W   = 16;
    private static final int TAB_H   = 32;
    private static final int TAB_GAP = 8;

    // ── Advancement unlock ────────────────────────────────────────
    public static boolean aerialHellUnlocked = false;

    // ── Dimensions dynamiques ─────────────────────────────────────
    private int bookW, bookH;
    private int bookLeft, bookTop;
    private int lineGap;
    private int firstLineY;
    private int pxStartL, pxEndL, pxWidthL;
    private int pxStartR, pxEndR, pxWidthR;
    private int linesPerPage;

    // ── État ──────────────────────────────────────────────────────
    private int hoveredTab  = -1;
    private int currentPage = PAGE_SUMMARY;
    private Set<String> insertedPages = new HashSet<>();
    private final ItemStack bookStack;

    public AerialHellBookScreen(ItemStack bookStack)
    {
        super(Component.empty());
        this.bookStack = bookStack;
        loadInsertedPages();
    }

    // ── Lecture des pages insérées depuis le NBT du livre ─────────

    private void loadInsertedPages()
    {
        insertedPages.clear();
        CompoundTag tag = bookStack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        if (tag.contains("inserted_pages"))
        {
            ListTag list = tag.getList("inserted_pages").orElse(new ListTag());
            for (int i = 0; i < list.size(); i++)
                list.getString(i).ifPresent(insertedPages::add);
        }
    }

    private boolean hasPage(String pageId)
    {
        return insertedPages.contains(pageId);
    }

    // ── Vérification advancement ──────────────────────────────────

    private boolean hasAerialHellAdvancement()
    {
        return aerialHellUnlocked;
    }

    @Override
    protected void init()
    {
        super.init();

        int maxW = (int)(this.width  * 0.85) - TAB_W * 2;
        int maxH = (int)(this.height * 0.80);

        bookW = Math.min(maxW, TEX_W);
        bookH = bookW * TEX_H / TEX_W;
        if (bookH > maxH) { bookH = maxH; bookW = bookH * TEX_W / TEX_H; }
        bookW = (bookW / 2) * 2;
        bookH = bookW * TEX_H / TEX_W;

        bookLeft = (this.width  - bookW) / 2;
        bookTop  = (this.height - bookH) / 2;

        float scale  = (float) bookW / TEX_W;
        lineGap      = Math.round(TEX_LINE_GAP * scale);
        firstLineY   = bookTop + Math.round(TEX_LINE_Y0 * scale);

        int spineW   = Math.round(20 * scale);
        int spineX   = bookLeft + bookW / 2 - spineW / 2;
        int margin   = Math.round(8 * scale);

        pxStartL  = bookLeft + margin;
        pxEndL    = spineX   - margin;
        pxWidthL  = pxEndL - pxStartL;

        pxStartR  = spineX + spineW + margin;
        pxEndR    = bookLeft + bookW - margin;
        pxWidthR  = pxEndR - pxStartR;

        int pageHeight = bookTop + bookH - margin - firstLineY;
        linesPerPage   = pageHeight / lineGap;
    }

    // ── Input ─────────────────────────────────────────────────────

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick)
    {
        for (int i = 0; i < TABS_LEFT.size(); i++)
            if (isHoveringTab(event.x(), event.y(), i, true))
            {
                currentPage = TABS_LEFT.get(i).page();
                return true;
            }

        for (int i = 0; i < TABS_RIGHT.size(); i++)
            if (isHoveringTab(event.x(), event.y(), i, false))
            {
                if (!hasAerialHellAdvancement()) return true;
                currentPage = TABS_RIGHT.get(i).page();
                return true;
            }

        return super.mouseClicked(event, doubleClick);
    }

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
                bookLeft, bookTop, 0f, 0f, bookW, bookH, TEX_W, TEX_H);

        boolean unlocked = hasAerialHellAdvancement();

        for (int i = 0; i < TABS_LEFT.size(); i++)
            renderTab(graphics, i, true, mouseX, mouseY, true);
        for (int i = 0; i < TABS_RIGHT.size(); i++)
            renderTab(graphics, i, false, mouseX, mouseY, unlocked);

        renderPageContent(graphics);

        super.extractBackground(graphics, mouseX, mouseY, partialTick);
    }

    private void renderTab(GuiGraphicsExtractor graphics, int index, boolean isLeft,
                            int mx, int my, boolean unlocked)
    {
        int[]   pos     = getTabPos(index, isLeft);
        int     x       = pos[0];
        int     y       = pos[1];
        Tab     tab     = isLeft ? TABS_LEFT.get(index) : TABS_RIGHT.get(index);
        boolean hovered = isHoveringTab(mx, my, index, isLeft);
        boolean locked  = !unlocked;

        int w     = hovered ? TAB_W + 4 : TAB_W;
        int xDraw = isLeft ? x - (hovered ? 4 : 0) : x;

        int color = locked ? 0xFF555555 : tab.color();
        graphics.fill(xDraw, y, xDraw + w, y + TAB_H, color);
        graphics.fill(xDraw,         y,             xDraw + w,    y + 1,       0xFF1A1A1A);
        graphics.fill(xDraw,         y + TAB_H - 1, xDraw + w,    y + TAB_H,  0xFF1A1A1A);
        graphics.fill(xDraw,         y,             xDraw + 1,    y + TAB_H,  0xFF1A1A1A);
        graphics.fill(xDraw + w - 1, y,             xDraw + w,    y + TAB_H,  0xFF1A1A1A);

        if (locked)
        {
            String padlock = "🔒";
            int px = xDraw + (TAB_W - this.font.width(padlock)) / 2;
            int py = y + (TAB_H - this.font.lineHeight) / 2;
            graphics.text(this.font, Component.literal(padlock), px, py, 0xFFAAAAAA, false);
        }

        if (hovered)
        {
            int textX = isLeft ? xDraw - this.font.width(tab.name()) - 4 : xDraw + w + 4;
            int textY = y + (TAB_H - 8) / 2;

            if (locked)
            {
                Component msg = Component.translatable("book.aerialhell.locked");
                int tw = this.font.width(msg) + 6;
                graphics.fill(textX - 3, textY - 2, textX + tw, textY + 10, 0xCC000000);
                graphics.text(this.font, msg, textX, textY, 0xFFAAAAAA, false);
            }
            else
            {
                Component label = Component.translatable(tab.translationKey());
                int tw = this.font.width(label) + 6;
                graphics.fill(textX - 3, textY - 2, textX + tw, textY + 10, 0xCC000000);
                graphics.text(this.font, label, textX, textY, 0xFFFFFFFF, false);
            }
        }
    }

    private int[] getTabPos(int index, boolean isLeft)
    {
        int totalH = TABS_LEFT.size() * TAB_H + (TABS_LEFT.size() - 1) * TAB_GAP;
        int startY = bookTop + (bookH - totalH) / 2;
        int y      = startY + index * (TAB_H + TAB_GAP);
        int x      = isLeft ? bookLeft - TAB_W : bookLeft + bookW;
        return new int[]{x, y};
    }

    // ── Contenu des pages ─────────────────────────────────────────

    private void renderPageContent(GuiGraphicsExtractor graphics)
    {
        if (currentPage == PAGE_SUMMARY)
        {
            int logoH = lineGap * 3;
            int logoW = logoH * LOGO_SRC_W / LOGO_SRC_H;
            if (logoW > pxWidthL) { logoW = pxWidthL; logoH = logoW * LOGO_SRC_H / LOGO_SRC_W; }
            int logoX = pxStartL + (pxWidthL - logoW) / 2;
            int logoY = firstLineY + lineGap / 2;

            graphics.blit(RenderPipelines.GUI_TEXTURED, LOGO_TEXTURE,
                    logoX, logoY, 0f, 0f, logoW, logoH, logoW, logoH);

            int sepY = logoY + logoH + lineGap / 2;
            graphics.fill(pxStartL, sepY, pxEndL, sepY + 1, 0xFF5C3A1E);

            int startLine = (sepY - firstLineY) / lineGap + 1;
            renderTextOnLines(graphics,
                    Component.translatable("book.aerialhell.welcome.text").getString(),
                    startLine);
        }
        else
        {
            // Trouve le tab actif
            Tab active = null;
            String pageId = null;
            for (int i = 0; i < TABS_LEFT.size(); i++)
                if (TABS_LEFT.get(i).page() == currentPage) { active = TABS_LEFT.get(i); pageId = "tab_" + i; }
            for (int i = 0; i < TABS_RIGHT.size(); i++)
                if (TABS_RIGHT.get(i).page() == currentPage) { active = TABS_RIGHT.get(i); pageId = "tab_right_" + i; }

            if (active != null)
            {
                int titleY = firstLineY - this.font.lineHeight + 1;
                Component title = Component.literal("✦ ").append(Component.translatable(active.translationKey())).append(" ✦");
                renderCenteredText(graphics, title, pxStartL, pxEndL, titleY, 0xFF5C3A1E);
                graphics.fill(pxStartL, firstLineY, pxEndL, firstLineY + 1, 0xFF5C3A1E);

                if (pageId != null && hasPage(pageId))
                {
                    // Contenu déverrouillé
                    renderTextOnLines(graphics,
                            Component.translatable("book.aerialhell.content." + pageId).getString(), 1);
                }
                else
                {
                    // Page non insérée → affiche un slot vide
                    renderLockedPageSlot(graphics);
                }
            }
        }
    }

    private void renderLockedPageSlot(GuiGraphicsExtractor graphics)
    {
        // Zone centrale de la page gauche
        int slotSize = lineGap * 4;
        int slotX    = pxStartL + (pxWidthL - slotSize) / 2;
        int slotY    = firstLineY + lineGap * 2;

        // Fond du slot
        graphics.fill(slotX, slotY, slotX + slotSize, slotY + slotSize, 0x44000000);

        // Bordure pointillée (simulée)
        graphics.fill(slotX,              slotY,              slotX + slotSize, slotY + 1,              0xFF5C3A1E);
        graphics.fill(slotX,              slotY + slotSize-1, slotX + slotSize, slotY + slotSize,       0xFF5C3A1E);
        graphics.fill(slotX,              slotY,              slotX + 1,        slotY + slotSize,       0xFF5C3A1E);
        graphics.fill(slotX + slotSize-1, slotY,              slotX + slotSize, slotY + slotSize,       0xFF5C3A1E);

        // Texte "?" centré
        String q = "?";
        graphics.text(this.font, Component.literal(q),
                slotX + (slotSize - this.font.width(q)) / 2,
                slotY + (slotSize - this.font.lineHeight) / 2,
                0xFF5C3A1E, false);

        // Message sous le slot
        Component msg = Component.translatable("book.aerialhell.page_missing");
        renderTextOnLines(graphics, msg.getString(), (slotY - firstLineY) / lineGap + 5);
    }

    private void renderTextOnLines(GuiGraphicsExtractor graphics, String text, int startLine)
    {
        List<String> linesL = wrapText(text, pxWidthL);

        int lineIndex = startLine;
        int rendered  = 0;

        for (String line : linesL)
        {
            if (lineIndex >= linesPerPage) break;
            int y = firstLineY + lineIndex * lineGap - this.font.lineHeight + 1;
            graphics.text(this.font, Component.literal(line), pxStartL + 2, y, 0xFF5C3A1E, false);
            lineIndex++;
            rendered++;
        }

        if (rendered < linesL.size())
        {
            List<String> linesR = wrapText(text, pxWidthR);
            int rightLine = 0;
            for (int i = rendered; i < linesR.size(); i++)
            {
                if (rightLine >= linesPerPage) break;
                int y = firstLineY + rightLine * lineGap - this.font.lineHeight + 1;
                graphics.text(this.font, Component.literal(linesR.get(i)), pxStartR + 2, y, 0xFF5C3A1E, false);
                rightLine++;
            }
        }
    }

    private void renderCenteredText(GuiGraphicsExtractor graphics, Component text,
                                     int xStart, int xEnd, int y, int color)
    {
        int x = xStart + (xEnd - xStart - this.font.width(text)) / 2;
        graphics.text(this.font, text, x, y, color, false);
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

    @Override
    public boolean isPauseScreen() { return false; }
}
