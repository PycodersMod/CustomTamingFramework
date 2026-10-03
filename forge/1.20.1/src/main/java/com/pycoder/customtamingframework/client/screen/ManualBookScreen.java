package com.pycoder.customtamingframework.client.screen;

import com.pycoder.customtamingframework.manual.ManualBookConfig;
import com.pycoder.customtamingframework.manual.ManualBookConfig.ManualBookData;
import com.pycoder.customtamingframework.manual.ManualBookConfig.ManualChapter;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;

import java.util.ArrayList;
import java.util.List;

public final class ManualBookScreen extends Screen {
    private static final int BOOK_WIDTH = 256;
    private static final int BOOK_HEIGHT = 160;
    private static final int PAGE_WIDTH = 110;
    private static final int PAGE_HEIGHT = 130;
    private static final int LEFT_PAGE_X = 10;
    private static final int RIGHT_PAGE_X = 134;
    private static final int PAGE_Y = 10;
    private static final int PAGE_TEXT_X = 8;
    private static final int PAGE_TEXT_WIDTH = 92;
    private static final int TITLE_Y = 8;
    private static final int RULE_Y = 24;
    private static final int BODY_START_Y = 35;
    private static final int BODY_END_Y = 116;
    private static final int PAGE_NUMBER_Y = 121;
    private static final int LINE_STEP = 10;
    private static final int MENU_ROW_STEP = 12;
    private static final int MENU_ROWS_PER_PAGE = 7;
    private static final int BODY_LINES_PER_PAGE = ((BODY_END_Y - BODY_START_Y) / LINE_STEP) + 1;

    private final Screen parentScreen;
    private final String manualId;
    private final ManualBookData manual;
    private final List<DisplayPage> displayPages = new ArrayList<>();

    private Button backButton;
    private Button forwardButton;
    private Button closeButton;
    private Button menuButton;

    private int currentSpread;
    private int totalSpreads = 1;

    public ManualBookScreen(Screen parentScreen, String manualId) {
        super(Component.empty());
        this.parentScreen = parentScreen;
        this.manualId = manualId;
        this.manual = ManualBookConfig.manual(manualId);
    }

    @Override
    protected void init() {
        super.init();
        rebuildDisplayPages();

        int left = bookLeft();
        int top = bookTop();

        this.backButton = addRenderableWidget(Button.builder(Component.literal("<"), button -> pageBack())
                .bounds(left + 6, top + BOOK_HEIGHT - 22, 20, 20)
                .build());

        this.forwardButton = addRenderableWidget(Button.builder(Component.literal(">"), button -> pageForward())
                .bounds(left + BOOK_WIDTH - 26, top + BOOK_HEIGHT - 22, 20, 20)
                .build());

        this.menuButton = addRenderableWidget(Button.builder(Component.literal("目录"), button -> goToMenu())
                .bounds(left + BOOK_WIDTH / 2 - 20, top + 4, 40, 16)
                .build());

        this.closeButton = addRenderableWidget(Button.builder(Component.literal("关闭"), button -> onClose())
                .bounds(width / 2 - 50, top + BOOK_HEIGHT + 8, 100, 20)
                .build());

        updateWidgetVisibility();
    }

    private void rebuildDisplayPages() {
        this.displayPages.clear();

        this.displayPages.add(new DisplayPage(
                PageType.COVER,
                Component.translatable(ManualBookConfig.titleKey(manualId)),
                wrapText(normalizeContent(manual.cover())),
                List.of(),
                false,
                0
        ));

        List<MenuRow> menuRows = new ArrayList<>();
        List<DisplayPage> contentPages = new ArrayList<>();
        int pageNumber = 1;

        for (ManualChapter chapter : manual.chapters()) {
            List<List<FormattedCharSequence>> chunks = paginateBodyLines(normalizeContent(chapter.content()));
            if (chunks.isEmpty()) {
                chunks = List.of(List.of());
            }

            int chapterStartDisplayIndex = 2 + contentPages.size();
            int chapterStartPageNumber = pageNumber;

            for (int index = 0; index < chunks.size(); index++) {
                boolean firstPage = index == 0;
                Component title = firstPage
                        ? Component.literal(chapter.title()).withStyle(ChatFormatting.BOLD)
                        : Component.literal(chapter.title() + " " + (index + 1));
                contentPages.add(new DisplayPage(
                        firstPage ? PageType.CHAPTER : PageType.CONTENT,
                        title,
                        chunks.get(index),
                        List.of(),
                        true,
                        pageNumber
                ));
                pageNumber++;
            }

            menuRows.add(new MenuRow(
                    chapter.title(),
                    chapterStartPageNumber,
                    Math.max(1, chapterStartDisplayIndex / 2)
            ));
        }

        this.displayPages.add(new DisplayPage(
                PageType.MENU,
                Component.literal("目录").withStyle(ChatFormatting.BOLD),
                List.of(),
                menuRows,
                false,
                0
        ));
        this.displayPages.addAll(contentPages);
        this.totalSpreads = Math.max(1, (this.displayPages.size() + 1) / 2);
        this.currentSpread = Math.max(0, Math.min(this.currentSpread, this.totalSpreads - 1));
    }

    private void updateWidgetVisibility() {
        if (backButton != null) {
            backButton.active = currentSpread > 0;
        }
        if (forwardButton != null) {
            forwardButton.active = currentSpread < totalSpreads - 1;
        }
        if (menuButton != null) {
            menuButton.visible = currentSpread > 0;
        }
    }

    private int bookLeft() {
        return (width - BOOK_WIDTH) / 2;
    }

    private int bookTop() {
        return (height - BOOK_HEIGHT) / 2;
    }

    private void pageBack() {
        if (currentSpread <= 0) {
            return;
        }
        currentSpread--;
        playPageSound();
        updateWidgetVisibility();
    }

    private void pageForward() {
        if (currentSpread >= totalSpreads - 1) {
            return;
        }
        currentSpread++;
        playPageSound();
        updateWidgetVisibility();
    }

    private void goToMenu() {
        if (currentSpread == 0) {
            return;
        }
        currentSpread = 0;
        playPageSound();
        updateWidgetVisibility();
    }

    private void playPageSound() {
        if (minecraft != null && minecraft.player != null) {
            minecraft.player.playSound(SoundEvents.BOOK_PAGE_TURN, 1.0F, 1.0F);
        }
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);

        int left = bookLeft();
        int top = bookTop();
        renderBookFrame(graphics, left, top);

        int leftIndex = currentSpread * 2;
        renderDisplayPage(graphics, left + LEFT_PAGE_X, top + PAGE_Y, getPageAt(leftIndex));
        renderDisplayPage(graphics, left + RIGHT_PAGE_X, top + PAGE_Y, getPageAt(leftIndex + 1));

        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private void renderBookFrame(GuiGraphics graphics, int left, int top) {
        graphics.fill(left, top, left + BOOK_WIDTH, top + BOOK_HEIGHT, 0xFFF0E0BC);
        graphics.fill(left + 3, top + 3, left + BOOK_WIDTH - 3, top + BOOK_HEIGHT - 3, 0xFFF9EECF);
        graphics.renderOutline(left, top, BOOK_WIDTH, BOOK_HEIGHT, 0xFF775131);
        graphics.fill(left + BOOK_WIDTH / 2 - 1, top + 8, left + BOOK_WIDTH / 2 + 1, top + BOOK_HEIGHT - 8, 0xFFB08A5C);

        renderPagePanel(graphics, left + LEFT_PAGE_X, top + PAGE_Y, PAGE_WIDTH, PAGE_HEIGHT);
        renderPagePanel(graphics, left + RIGHT_PAGE_X, top + PAGE_Y, PAGE_WIDTH, PAGE_HEIGHT);
    }

    private void renderPagePanel(GuiGraphics graphics, int x, int y, int w, int h) {
        graphics.fill(x, y, x + w, y + h, 0xFFFFF8EA);
        graphics.renderOutline(x, y, w, h, 0xFF8B6A3D);
    }

    private void renderDisplayPage(GuiGraphics graphics, int x, int y, DisplayPage page) {
        if (page == null) {
            return;
        }

        renderTitle(graphics, x, y, page.title(), page.type() == PageType.CHAPTER || page.type() == PageType.CONTENT);
        if (page.type() == PageType.MENU) {
            renderMenuRows(graphics, x, y, page.menuRows());
        } else {
            renderBodyLines(graphics, x, y, page.lines());
        }
        if (page.numbered()) {
            renderPageNumber(graphics, x, y, page.pageNumber());
        }
    }

    private void renderTitle(GuiGraphics graphics, int x, int y, Component title, boolean bold) {
        Component renderedTitle = bold ? title.copy().withStyle(ChatFormatting.BOLD) : title;
        String titleText = fitText(renderedTitle.getString(), PAGE_TEXT_WIDTH);
        Component fittedTitle = bold ? Component.literal(titleText).withStyle(ChatFormatting.BOLD) : Component.literal(titleText);
        int titleWidth = font.width(fittedTitle);
        graphics.drawString(font, fittedTitle, x + (PAGE_WIDTH - titleWidth) / 2, y + TITLE_Y, 0x6B4A2C, false);
        graphics.fill(x + PAGE_TEXT_X, y + RULE_Y, x + PAGE_WIDTH - PAGE_TEXT_X, y + RULE_Y + 1, 0xFF8B6A3D);
    }

    private void renderBodyLines(GuiGraphics graphics, int x, int y, List<FormattedCharSequence> lines) {
        int lineY = y + BODY_START_Y;
        for (FormattedCharSequence line : lines) {
            if (lineY > y + BODY_END_Y) {
                break;
            }
            graphics.drawString(font, line, x + PAGE_TEXT_X, lineY, 0x3E2723, false);
            lineY += LINE_STEP;
        }
    }

    private void renderMenuRows(GuiGraphics graphics, int x, int y, List<MenuRow> rows) {
        if (rows.isEmpty()) {
            drawWrappedText(graphics, "暂无章节内容。", x + PAGE_TEXT_X, y + BODY_START_Y, PAGE_TEXT_WIDTH, 0x5D4037);
            return;
        }

        int lineY = y + BODY_START_Y;
        for (MenuRow row : rows) {
            Component pageName = Component.literal(fitText(row.title(), 58)).withStyle(ChatFormatting.BOLD);
            String pageNumber = String.valueOf(row.pageNumber());
            graphics.drawString(font, pageName, x + PAGE_TEXT_X, lineY, 0x5D4037, false);
            graphics.drawString(font, pageNumber, x + PAGE_WIDTH - PAGE_TEXT_X - font.width(pageNumber), lineY, 0x5D4037, false);
            lineY += MENU_ROW_STEP;
        }
    }

    private void renderPageNumber(GuiGraphics graphics, int x, int y, int pageNumber) {
        String pageNumberText = String.valueOf(pageNumber);
        graphics.drawString(font, pageNumberText,
                x + (PAGE_WIDTH - font.width(pageNumberText)) / 2,
                y + PAGE_NUMBER_Y, 0x5D4037, false);
    }

    private List<FormattedCharSequence> wrapText(String content) {
        List<FormattedCharSequence> lines = new ArrayList<>();
        String normalized = normalizeContent(content);
        String[] paragraphs = normalized.split("\n", -1);
        for (String paragraph : paragraphs) {
            if (paragraph.isEmpty()) {
                lines.add(FormattedCharSequence.EMPTY);
                continue;
            }
            lines.addAll(font.split(Component.literal(paragraph), PAGE_TEXT_WIDTH));
        }
        return lines;
    }

    private List<List<FormattedCharSequence>> paginateBodyLines(String content) {
        List<FormattedCharSequence> lines = wrapText(content);
        List<List<FormattedCharSequence>> pages = new ArrayList<>();
        for (int start = 0; start < lines.size(); start += BODY_LINES_PER_PAGE) {
            pages.add(new ArrayList<>(lines.subList(start, Math.min(lines.size(), start + BODY_LINES_PER_PAGE))));
        }
        if (pages.isEmpty()) {
            pages.add(List.of());
        }
        return pages;
    }

    private void drawWrappedText(GuiGraphics graphics, String text, int x, int y, int width, int color) {
        int curY = y;
        String normalized = normalizeContent(text);
        String[] paragraphs = normalized.split("\n", -1);
        for (String paragraph : paragraphs) {
            if (paragraph.isEmpty()) {
                curY += LINE_STEP;
                continue;
            }
            for (FormattedCharSequence line : font.split(Component.literal(paragraph), width)) {
                graphics.drawString(font, line, x, curY, color, false);
                curY += LINE_STEP;
            }
        }
    }

    private String normalizeContent(String text) {
        if (text == null || text.isBlank()) {
            return "";
        }
        return text.replace("\\n", "\n").replace("\r\n", "\n").replace("\r", "\n");
    }

    private String fitText(String text, int maxWidth) {
        if (font.width(text) <= maxWidth) {
            return text;
        }
        String ellipsis = "..";
        int length = text.length();
        while (length > 0 && font.width(text.substring(0, length) + ellipsis) > maxWidth) {
            length--;
        }
        return text.substring(0, length) + ellipsis;
    }

    private DisplayPage getPageAt(int pageIndex) {
        if (pageIndex < 0 || pageIndex >= displayPages.size()) {
            return null;
        }
        return displayPages.get(pageIndex);
    }

    private boolean currentSpreadHasMenuPage() {
        DisplayPage rightPage = getPageAt(currentSpread * 2 + 1);
        return rightPage != null && rightPage.type() == PageType.MENU;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && currentSpreadHasMenuPage()) {
            int left = bookLeft();
            int top = bookTop();
            DisplayPage menuPage = getPageAt(currentSpread * 2 + 1);
            if (menuPage != null && menuPage.type() == PageType.MENU) {
                if (clickMenuRows(mouseX, mouseY, left + RIGHT_PAGE_X, top + PAGE_Y, menuPage.menuRows())) {
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private boolean clickMenuRows(double mouseX, double mouseY, int pageX, int pageY, List<MenuRow> rows) {
        int lineY = pageY + BODY_START_Y;
        for (MenuRow row : rows) {
            if (mouseX >= pageX + PAGE_TEXT_X && mouseX <= pageX + PAGE_WIDTH - PAGE_TEXT_X
                    && mouseY >= lineY && mouseY <= lineY + 10) {
                goToSpread(row.targetSpread());
                return true;
            }
            lineY += MENU_ROW_STEP;
        }
        return false;
    }

    private void goToSpread(int spread) {
        if (spread < 0 || spread >= totalSpreads) {
            return;
        }
        currentSpread = spread;
        playPageSound();
        updateWidgetVisibility();
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 263) {
            pageBack();
            return true;
        }
        if (keyCode == 262) {
            pageForward();
            return true;
        }
        if (keyCode == 256) {
            onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void onClose() {
        if (minecraft != null) {
            minecraft.setScreen(parentScreen);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private enum PageType {
        COVER,
        MENU,
        CHAPTER,
        CONTENT
    }

    private record MenuRow(String title, int pageNumber, int targetSpread) {
    }

    private record DisplayPage(PageType type, Component title, List<FormattedCharSequence> lines,
                               List<MenuRow> menuRows, boolean numbered, int pageNumber) {
    }
}
