package com.zszl.zszlScriptMod.gui.halloffame;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import com.zszl.zszlScriptMod.gui.components.ThemedGuiScreen;
import net.minecraft.client.gui.GuiUtilRenderComponents;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import com.zszl.zszlScriptMod.gui.GuiInventory;
import com.zszl.zszlScriptMod.utils.HallOfFameManager;
import com.zszl.zszlScriptMod.utils.TitleCompendiumManager;
import com.zszl.zszlScriptMod.utils.EnhancementAttrManager;
import com.zszl.zszlScriptMod.utils.AdExpListManager;
import org.lwjgl.input.Mouse;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import com.zszl.zszlScriptMod.gui.components.ThemedButton;

public class GuiHallOfFame extends ThemedGuiScreen {

    private static final int SOURCE_HALL_OF_FAME = 0;
    private static final int SOURCE_TITLE_COMPENDIUM = 1;
    private static final int SOURCE_ENHANCEMENT_ATTR = 2;
    private static final int SOURCE_AD_EXP_LIST = 3;

    private static final int TYPE_NORMAL = 0;
    private static final int TYPE_H1 = 1;
    private static final int TYPE_H2 = 2;
    private static final int TYPE_H3 = 7;
    private static final int TYPE_QUOTE = 3;
    private static final int TYPE_LIST = 4;
    private static final int TYPE_SEPARATOR = 5;
    private static final int TYPE_SPACER = 6;
    private static final int TYPE_TABLE = 8;
    private static final int TYPE_TABLE_HEADER = 9;

    private static final int BTN_BACK = 0;
    private static final int BTN_SEARCH_PREV = 1;
    private static final int BTN_SEARCH_NEXT = 2;

    private static final Pattern MD_LOCAL_LINK_PATTERN = Pattern.compile("\\[([^\\]]+)]\\(#([^\\)]+)\\)");
    private static final Pattern MD_BOLD_PATTERN = Pattern.compile("\\*\\*([^*]+)\\*\\*");

    private static class RenderEntry {
        final ITextComponent text;
        final int type;
        final List<String> tableCells;
        final int[] tableWidths;
        final boolean tableDrawTop;
        final boolean tableDrawBottom;
        final boolean tableHeaderDivider;

        RenderEntry(ITextComponent text, int type) {
            this.text = text;
            this.type = type;
            this.tableCells = null;
            this.tableWidths = null;
            this.tableDrawTop = false;
            this.tableDrawBottom = false;
            this.tableHeaderDivider = false;
        }

        RenderEntry(List<String> tableCells, int[] tableWidths, int type,
                boolean tableDrawTop, boolean tableDrawBottom, boolean tableHeaderDivider) {
            this.text = new TextComponentString("");
            this.type = type;
            this.tableCells = tableCells;
            this.tableWidths = tableWidths;
            this.tableDrawTop = tableDrawTop;
            this.tableDrawBottom = tableDrawBottom;
            this.tableHeaderDivider = tableHeaderDivider;
        }
    }

    private static class StyledSegment {
        final String text;
        final boolean bold;

        StyledSegment(String text, boolean bold) {
            this.text = text;
            this.bold = bold;
        }
    }

    private final GuiScreen parentScreen;
    private String markdownContent;
    private final String mainTitleKey;
    private final int contentSource;

    private static class TocEntry {
        final String text;
        final int yPosition;
        final int level;

        TocEntry(String text, int yPosition, int level) {
            this.text = text;
            this.yPosition = yPosition;
            this.level = level;
        }
    }

    private List<RenderEntry> renderedLines = new ArrayList<>();
    private List<TocEntry> tocEntries = new ArrayList<>();
    private final Map<String, Integer> anchorPositions = new HashMap<>();
    private int scrollOffset = 0;
    private int maxScroll = 0;
    private int tocScrollOffset = 0;
    private int maxTocScroll = 0;
    private int horizontalScrollOffset = 0;
    private int maxHorizontalScroll = 0;
    private int totalContentHeight = 0;
    private int contentX, contentWidth;
    private int panelX, panelY, panelWidth, panelHeight;
    private int contentTop, contentBottom;
    private int lineHeight;
    private int tocX, tocWidth, tocTop, tocBottom;

    private GuiTextField searchField;
    private String searchQuery = "";
    private final List<Integer> searchHitLineIndices = new ArrayList<>();
    private int currentSearchHit = -1;

    private boolean draggingScrollbar = false;
    private int dragStartMouseY = 0;
    private int dragStartScrollOffset = 0;
    private boolean draggingHorizontalScrollbar = false;
    private int dragStartMouseX = 0;
    private int dragStartHorizontalScrollOffset = 0;
    private boolean draggingTocScrollbar = false;
    private int dragTocStartMouseY = 0;
    private int dragTocStartScrollOffset = 0;

    public GuiHallOfFame(GuiScreen parent, String content) {
        this(parent, content, "gui.halloffame.main_title", SOURCE_HALL_OF_FAME);
    }

    public static GuiHallOfFame createTitleCompendiumView(GuiScreen parent) {
        return new GuiHallOfFame(parent,
                TitleCompendiumManager.content,
                "gui.title_compendium.main_title",
                SOURCE_TITLE_COMPENDIUM);
    }

    public static GuiHallOfFame createEnhancementAttrView(GuiScreen parent) {
        return new GuiHallOfFame(parent,
                EnhancementAttrManager.content,
                "gui.enhancement_attr.main_title",
                SOURCE_ENHANCEMENT_ATTR);
    }

    public static GuiHallOfFame createAdExpListView(GuiScreen parent) {
        return new GuiHallOfFame(parent,
                AdExpListManager.content,
                "gui.ad_exp_list.main_title",
                SOURCE_AD_EXP_LIST);
    }

    private GuiHallOfFame(GuiScreen parent, String content, String mainTitleKey, int contentSource) {
        this.parentScreen = parent;
        this.markdownContent = content == null ? I18n.format("gui.halloffame.empty_content") : content;
        this.mainTitleKey = mainTitleKey;
        this.contentSource = contentSource;
    }

    private void returnToMainMenuOverlay() {
        if (parentScreen != null) {
            mc.displayGuiScreen(parentScreen);
            return;
        }
        GuiInventory.openOverlayScreen();
    }

    @Override
    public void initGui() {
        Keyboard.enableRepeatEvents(true);
        this.buttonList.clear();
        this.panelWidth = Math.min(620, this.width - 40);
        this.panelHeight = Math.min(330, this.height - 30);
        this.panelX = (this.width - this.panelWidth) / 2;
        this.panelY = (this.height - this.panelHeight) / 2;

        this.contentTop = this.panelY + 36;
        this.contentBottom = this.panelY + this.panelHeight - 36;
        this.lineHeight = this.fontRenderer.FONT_HEIGHT + 2;
        this.tocWidth = 140;
        this.tocX = this.panelX + 10;
        this.tocTop = this.contentTop;
        this.tocBottom = this.contentBottom - 28; // 预留目录下方搜索区

        // 左侧目录 + 右侧正文
        this.contentX = this.tocX + this.tocWidth + 12;
        this.contentWidth = (this.panelX + this.panelWidth - 12) - this.contentX;

        this.buttonList.add(new ThemedButton(BTN_BACK, this.width / 2 - 50, this.panelY + this.panelHeight - 26, 100, 18,
                I18n.format("gui.common.back")));

        int searchY = this.tocBottom + 8;
        int searchX = this.tocX;
        int searchW = Math.max(70, this.tocWidth - 46);
        this.searchField = new GuiTextField(200, this.fontRenderer, searchX, searchY, searchW, 18);
        this.searchField.setMaxStringLength(128);
        this.searchField.setText(this.searchQuery == null ? "" : this.searchQuery);

        this.buttonList.add(new ThemedButton(BTN_SEARCH_PREV, searchX + searchW + 4, searchY, 20, 18, "↑"));
        this.buttonList.add(new ThemedButton(BTN_SEARCH_NEXT, searchX + searchW + 26, searchY, 20, 18, "↓"));

        parseContent();
        updateSearchResults(false);
    }

    // !! 核心修改 1: 添加与 GuiChangelog 相同的内联格式解析器 !!
    private ITextComponent parseInlineFormatting(String lineText) {
        TextComponentString finalComponent = new TextComponentString("");
        String remainingLine = stripMarkdownLinks(lineText);

        while (!remainingLine.isEmpty()) {
            int boldStart = remainingLine.indexOf("**");

            // 为了简化，我们只处理加粗，不处理链接
            if (boldStart == -1) {
                finalComponent.appendText(remainingLine);
                break;
            }

            if (boldStart > 0) {
                finalComponent.appendText(remainingLine.substring(0, boldStart));
            }

            remainingLine = remainingLine.substring(boldStart);

            if (remainingLine.startsWith("**")) {
                int boldEnd = remainingLine.indexOf("**", 2);
                if (boldEnd != -1) {
                    String boldText = remainingLine.substring(2, boldEnd);
                    finalComponent.appendSibling(new TextComponentString(boldText).setStyle(new Style().setBold(true)));
                    remainingLine = remainingLine.substring(boldEnd + 2);
                } else {
                    // 如果没有找到结束的**，则当作普通文本处理
                    finalComponent.appendText("**");
                    remainingLine = remainingLine.substring(2);
                }
            }
        }

        return finalComponent;
    }

    private int getEntryHeight(RenderEntry entry) {
        switch (entry.type) {
            case TYPE_H1:
                return lineHeight + 6;
            case TYPE_H2:
                return lineHeight + 4;
            case TYPE_H3:
                return lineHeight + 2;
            case TYPE_QUOTE:
                return lineHeight + 2;
            case TYPE_LIST:
                return lineHeight + 1;
            case TYPE_SEPARATOR:
                return lineHeight + 3;
            case TYPE_SPACER:
                return 4;
            case TYPE_TABLE_HEADER:
                return lineHeight + 1;
            case TYPE_TABLE:
                return lineHeight + 1;
            default:
                return lineHeight;
        }
    }

    private int getContentViewportBottom() {
        return this.contentBottom - 10;
    }

    // !! 核心修改 2: 重写内容解析逻辑，以支持所有格式 !!
    private void parseContent() {
        this.renderedLines.clear();
        this.tocEntries.clear();
        this.anchorPositions.clear();
        String[] lines = markdownContent.split("\n");
        int currentY = 0;

        for (int lineIndex = 0; lineIndex < lines.length; lineIndex++) {
            String line = lines[lineIndex];
            String trimmed = line == null ? "" : line.trim();
            ITextComponent lineComponent;
            int lineType;
            int tocLevel = 0;
            String tocTitle = null;

            if (trimmed.isEmpty()) {
                lineComponent = new TextComponentString(" ");
                lineType = TYPE_SPACER;
            } else if (trimmed.startsWith("|")) { // 处理Markdown表格
                List<String> tableLines = new ArrayList<>();
                while (lineIndex < lines.length) {
                    String candidate = lines[lineIndex] == null ? "" : lines[lineIndex].trim();
                    if (!candidate.startsWith("|")) {
                        break;
                    }
                    tableLines.add(candidate);
                    lineIndex++;
                }
                lineIndex--; // for循环会再++，这里回退1步

                currentY = appendTableBlock(tableLines, currentY);
                continue;
            } else if (trimmed.startsWith("> ")) { // 处理引用
                lineComponent = new TextComponentString("    " + trimmed.substring(2)) // 缩进
                        .setStyle(new Style().setColor(TextFormatting.GRAY).setItalic(true));
                lineType = TYPE_QUOTE;
            } else if (trimmed.startsWith("### ")) {
                String title = trimmed.substring(4);
                lineComponent = parseInlineFormatting(title)
                        .setStyle(new Style().setColor(TextFormatting.WHITE).setBold(true));
                lineType = TYPE_H3;
                tocLevel = 3;
                tocTitle = title;
            } else if (trimmed.startsWith("## ")) { // 处理二级标题
                String title = trimmed.substring(3);
                lineComponent = parseInlineFormatting(title)
                        .setStyle(new Style().setColor(TextFormatting.AQUA).setBold(true));
                lineType = TYPE_H2;
                tocLevel = 2;
                tocTitle = title;
            } else if (trimmed.startsWith("# ")) { // 处理一级标题
                String title = trimmed.substring(2);
                lineComponent = parseInlineFormatting(title)
                        .setStyle(new Style().setColor(TextFormatting.GOLD).setBold(true).setUnderlined(true));
                lineType = TYPE_H1;
                tocLevel = 1;
                tocTitle = title;
            } else if (trimmed.startsWith("* ") || trimmed.startsWith("- ")) { // 处理列表
                lineComponent = new TextComponentString(" • ")
                        .appendSibling(parseInlineFormatting(trimmed.substring(2)));
                lineType = TYPE_LIST;
            } else if (trimmed.matches("^-{3,}.*")) { // 处理分隔线
                lineComponent = new TextComponentString(
                        "§7§m                                                                                ");
                lineType = TYPE_SEPARATOR;
            } else { // 处理普通文本
                lineComponent = parseInlineFormatting(trimmed);
                lineType = TYPE_NORMAL;
            }

            if (tocLevel > 0 && tocTitle != null && !tocTitle.trim().isEmpty()) {
                tocEntries.add(new TocEntry(tocTitle.trim(), currentY, tocLevel));
                registerAnchor(tocTitle, currentY);
            }

            registerBoldAnchorsFromText(trimmed, currentY);

            List<ITextComponent> split = GuiUtilRenderComponents.splitText(lineComponent, this.contentWidth,
                    this.fontRenderer, false, true);
            if (split.isEmpty()) {
                RenderEntry entry = new RenderEntry(new TextComponentString(" "), TYPE_SPACER);
                renderedLines.add(entry);
                currentY += getEntryHeight(entry);
            } else {
                for (int i = 0; i < split.size(); i++) {
                    int typeForLine = (i == 0) ? lineType : TYPE_NORMAL;
                    RenderEntry entry = new RenderEntry(split.get(i), typeForLine);
                    renderedLines.add(entry);
                    currentY += getEntryHeight(entry);
                }
            }

            if (lineType == TYPE_H1 || lineType == TYPE_H2 || lineType == TYPE_H3 || lineType == TYPE_QUOTE
                    || lineType == TYPE_LIST) {
                RenderEntry spacer = new RenderEntry(new TextComponentString(" "), TYPE_SPACER);
                renderedLines.add(spacer);
                currentY += getEntryHeight(spacer);
            }
        }

        totalContentHeight = 0;
        int maxLineWidth = 0;
        for (RenderEntry entry : renderedLines) {
            totalContentHeight += getEntryHeight(entry);
            if ((entry.type == TYPE_TABLE || entry.type == TYPE_TABLE_HEADER)
                    && entry.tableWidths != null && entry.tableWidths.length > 0) {
                int w = 0;
                for (int cw : entry.tableWidths) {
                    w += cw;
                }
                maxLineWidth = Math.max(maxLineWidth, w + 8);
            } else if (entry.text != null) {
                maxLineWidth = Math.max(maxLineWidth, this.fontRenderer.getStringWidth(entry.text.getUnformattedText()));
            }
        }

        int viewHeight = Math.max(1, getContentViewportBottom() - this.contentTop - 8);
        maxScroll = Math.max(0, totalContentHeight - viewHeight);
        scrollOffset = MathHelper.clamp(scrollOffset, 0, maxScroll);

        int tocVisibleRows = Math.max(1, (this.tocBottom - (this.tocTop + 16) - 4) / 12);
        maxTocScroll = Math.max(0, tocEntries.size() - tocVisibleRows);
        tocScrollOffset = MathHelper.clamp(tocScrollOffset, 0, maxTocScroll);

        int visibleWidth = Math.max(20, this.contentWidth - 24);
        maxHorizontalScroll = Math.max(0, maxLineWidth - visibleWidth);
        horizontalScrollOffset = MathHelper.clamp(horizontalScrollOffset, 0, maxHorizontalScroll);
    }
    // !! 修改结束 !!

    private void drawFancyTitle(String title, int centerX, int y) {
        int totalWidth = fontRenderer.getStringWidth(title);
        int startX = centerX - totalWidth / 2;
        int x = startX;
        long ticker = Minecraft.getSystemTime() / 30L;

        for (int i = 0; i < title.length(); i++) {
            String ch = String.valueOf(title.charAt(i));
            int mix = (int) ((i * 18 + ticker) % 180);
            float t = mix / 180.0F;

            int r = (int) (255 * (1.0F - t) + 80 * t);
            int g = (int) (210 * (1.0F - t) + 220 * t);
            int b = (int) (40 * (1.0F - t) + 255 * t);
            int color = (r << 16) | (g << 8) | b;

            fontRenderer.drawStringWithShadow(ch, x, y, color);
            x += fontRenderer.getStringWidth(ch);
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        // 实时刷新：若后台拉取到新内容，立即重排并显示
        String latest = this.contentSource == SOURCE_HALL_OF_FAME
            ? HallOfFameManager.content
            : (this.contentSource == SOURCE_TITLE_COMPENDIUM
                ? TitleCompendiumManager.content
                : (this.contentSource == SOURCE_ENHANCEMENT_ATTR
                    ? EnhancementAttrManager.content
                    : AdExpListManager.content));
        if (latest != null && !latest.equals(this.markdownContent)) {
            this.markdownContent = latest;
            parseContent();
            updateSearchResults(false);
        }

        drawGradientRect(0, 0, this.width, this.height, 0xA0000000, 0xD0000000);

        // 卡片阴影
        drawRect(this.panelX - 4, this.panelY - 2, this.panelX + this.panelWidth + 6,
                this.panelY + this.panelHeight + 6,
                0x40000000);
        drawRect(this.panelX - 2, this.panelY - 1, this.panelX + this.panelWidth + 3,
                this.panelY + this.panelHeight + 3,
                0x25000000);

        // 主面板（半透明 + 边框）
        drawRect(this.panelX, this.panelY, this.panelX + this.panelWidth, this.panelY + this.panelHeight, 0xC0181E28);
        drawHorizontalLine(this.panelX, this.panelX + this.panelWidth - 1, this.panelY, 0xFF5A6A80);
        drawHorizontalLine(this.panelX, this.panelX + this.panelWidth - 1, this.panelY + this.panelHeight - 1,
                0xFF5A6A80);
        drawVerticalLine(this.panelX, this.panelY, this.panelY + this.panelHeight - 1, 0xFF5A6A80);
        drawVerticalLine(this.panelX + this.panelWidth - 1, this.panelY, this.panelY + this.panelHeight - 1,
                0xFF5A6A80);

        // 标题区（渐变字效）
        drawFancyTitle(I18n.format(this.mainTitleKey), this.panelX + this.panelWidth / 2, this.panelY + 10);

        // 左侧目录背景
        drawRect(this.tocX, this.tocTop, this.tocX + this.tocWidth, this.tocBottom, 0x800B0F14);

        // 内容区背景（半透明）
        int listLeft = this.contentX;
        int listRight = this.panelX + this.panelWidth - 10;
        drawRect(listLeft, this.contentTop, listRight, this.contentBottom, 0x800B0F14);

        drawTableOfContents(mouseX, mouseY);

        int textLeft = listLeft + 8;
        int textRight = listRight - 12;
        int clipWidth = textRight - textLeft;
        int viewportBottom = getContentViewportBottom();
        int clipHeight = viewportBottom - this.contentTop - 4;

        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        int scaleFactor = new net.minecraft.client.gui.ScaledResolution(mc).getScaleFactor();
        GL11.glScissor(
                textLeft * scaleFactor,
                (this.height - (this.contentTop + clipHeight)) * scaleFactor,
                clipWidth * scaleFactor,
                clipHeight * scaleFactor);

        int yCursor = this.contentTop + 3 - scrollOffset;
        int drawTextLeft = textLeft - horizontalScrollOffset;
        int drawTextRight = textRight - horizontalScrollOffset;
        int hoveredIndex = -1;
        int tableVisualRow = 0;

        int hitYCursor = this.contentTop + 3 - scrollOffset;
        for (int i = 0; i < renderedLines.size(); i++) {
            int h = getEntryHeight(renderedLines.get(i));
            if (mouseX >= textLeft - 2 && mouseX <= textRight && mouseY >= hitYCursor && mouseY < hitYCursor + h) {
                hoveredIndex = i;
                break;
            }
            hitYCursor += h;
        }

        for (int i = 0; i < renderedLines.size(); i++) {
            RenderEntry line = renderedLines.get(i);
            int entryHeight = getEntryHeight(line);
            if (yCursor + entryHeight >= this.contentTop && yCursor <= viewportBottom - fontRenderer.FONT_HEIGHT) {
                boolean isTableLine = (line.type == TYPE_TABLE || line.type == TYPE_TABLE_HEADER);
                boolean isSearchHit = searchHitLineIndices.contains(i);

                // 表格行底色：奇偶斑马纹
                if (line.type == TYPE_TABLE_HEADER) {
                    drawRect(textLeft - 3, yCursor - 1, textRight - 2, yCursor + entryHeight - 1, 0x334AA3FF);
                } else if (line.type == TYPE_TABLE) {
                    int zebra = (tableVisualRow % 2 == 0) ? 0x22293A56 : 0x1A223349;
                    drawRect(textLeft - 3, yCursor - 1, textRight - 2, yCursor + entryHeight - 1, zebra);
                }

                if (i == hoveredIndex && line.type != TYPE_SPACER) {
                    int hoverColor = isTableLine ? 0x554C90FF : 0x332E8BFF;
                    drawRect(textLeft - 3, yCursor - 1, textRight - 2, yCursor + entryHeight - 1, hoverColor);
                }
                if (isSearchHit) {
                    int hitIdx = searchHitLineIndices.indexOf(i);
                    int hitBg = (hitIdx == currentSearchHit) ? 0x66FFD54A : 0x44FFE082;
                    drawRect(textLeft - 3, yCursor - 1, textRight - 2, yCursor + entryHeight - 1, hitBg);
                }
                if (line.type == TYPE_QUOTE) {
                    drawRect(textLeft - 5, yCursor - 1, textLeft - 3, yCursor + entryHeight - 2, 0xAA6C7A8C);
                }
                if (isTableLine && line.tableCells != null && line.tableWidths != null) {
                    drawRealTableRow(drawTextLeft - 3, drawTextRight - 2, yCursor, entryHeight, line);
                } else {
                    int textColor = 0xFFFFFF;
                    if (line.type == TYPE_TABLE_HEADER) {
                        textColor = 0xFFE6C45C;
                    } else if (line.type == TYPE_TABLE) {
                        textColor = 0xFFDCE6F5;
                    }
                    drawString(fontRenderer, line.text.getFormattedText(), drawTextLeft, yCursor, textColor);
                }

                if (isTableLine) {
                    tableVisualRow++;
                }
            }
            yCursor += entryHeight;
        }

        GL11.glDisable(GL11.GL_SCISSOR_TEST);

        if (maxScroll > 0) {
            int scrollbarX = listRight - 6;
            int scrollbarHeight = viewportBottom - this.contentTop;
            drawRect(scrollbarX, this.contentTop, scrollbarX + 4, viewportBottom, 0xFF202732);
            int contentHeight = Math.max(1, totalContentHeight);
            int thumbHeight = Math.max(14, (int) ((float) scrollbarHeight / contentHeight * scrollbarHeight));
            int thumbY = this.contentTop + (int) ((float) scrollOffset / maxScroll * (scrollbarHeight - thumbHeight));
            drawRect(scrollbarX, thumbY, scrollbarX + 6, thumbY + thumbHeight, 0xFF888888);
        }

        int hTrackLeft = listLeft + 2;
        int hTrackRight = listRight - 10;
        int hTrackTop = viewportBottom + 2;
        int hTrackBottom = this.contentBottom - 2;
        drawRect(hTrackLeft, hTrackTop, hTrackRight, hTrackBottom, 0xFF202732);
        if (maxHorizontalScroll > 0) {
            int trackW = Math.max(1, hTrackRight - hTrackLeft);
            int visibleW = Math.max(20, clipWidth);
            int contentW = visibleW + maxHorizontalScroll;
            int thumbW = Math.max(18, (int) ((float) visibleW / contentW * trackW));
            int thumbX = hTrackLeft + (int) ((float) horizontalScrollOffset / maxHorizontalScroll * (trackW - thumbW));
            drawRect(thumbX, hTrackTop, thumbX + thumbW, hTrackBottom, 0xFF888888);
        }

        if (searchField != null) {
            drawThemedTextField(searchField);
            String stat = searchHitLineIndices.isEmpty()
                    ? "0/0"
                    : (currentSearchHit + 1) + "/" + searchHitLineIndices.size();
            drawString(this.fontRenderer, "§7搜索: " + stat, searchField.x, searchField.y - 10, 0xFFFFFFFF);
        }

        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    private void drawTableOfContents(int mouseX, int mouseY) {
        drawCenteredString(fontRenderer, I18n.format("gui.halloffame.toc"), this.tocX + this.tocWidth / 2,
                this.tocTop + 2, 0xFFFFFF);

        int currentY = this.tocTop + 16;
        int currentTocIndex = -1;
        for (int i = tocEntries.size() - 1; i >= 0; i--) {
            if (scrollOffset >= tocEntries.get(i).yPosition) {
                currentTocIndex = i;
                break;
            }
        }

        int visibleRows = Math.max(1, (this.tocBottom - currentY - 4) / 12);
        for (int i = 0; i < visibleRows; i++) {
            int actualIndex = i + tocScrollOffset;
            if (actualIndex < 0 || actualIndex >= tocEntries.size()) {
                break;
            }

            TocEntry entry = tocEntries.get(actualIndex);
            int indent = (entry.level <= 1) ? 5 : (entry.level == 2 ? 12 : 19);
            int entryX = this.tocX + indent;
            int maxW = this.tocWidth - indent - 10;

            if (currentY + 11 > this.tocBottom) {
                break;
            }

            boolean hovered = mouseX >= this.tocX && mouseX <= this.tocX + this.tocWidth - 8
                    && mouseY >= currentY && mouseY < currentY + 11;
            TextFormatting color = (actualIndex == currentTocIndex) ? TextFormatting.YELLOW
                    : (hovered ? TextFormatting.WHITE : TextFormatting.GRAY);

            fontRenderer.drawStringWithShadow(color + fontRenderer.trimStringToWidth(entry.text, maxW), entryX,
                    currentY,
                    0xFFFFFF);
            currentY += 12;
        }

        if (maxTocScroll > 0) {
            int scrollbarX = this.tocX + this.tocWidth - 6;
            int scrollbarTop = this.tocTop + 16;
            int scrollbarHeight = this.tocBottom - scrollbarTop - 4;
            drawRect(scrollbarX, scrollbarTop, scrollbarX + 4, scrollbarTop + scrollbarHeight, 0xFF202732);
            int thumbHeight = Math.max(14, (int) ((float) visibleRows / tocEntries.size() * scrollbarHeight));
            int thumbY = scrollbarTop + (int) ((float) tocScrollOffset / maxTocScroll * (scrollbarHeight - thumbHeight));
            drawRect(scrollbarX, thumbY, scrollbarX + 6, thumbY + thumbHeight, 0xFF888888);
        }
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        if (button.id == BTN_BACK) {
            returnToMainMenuOverlay();
        } else if (button.id == BTN_SEARCH_PREV) {
            jumpToSearchHit(-1);
        } else if (button.id == BTN_SEARCH_NEXT) {
            jumpToSearchHit(1);
        }
    }

    private int appendTableBlock(List<String> tableLines, int currentY) {
        if (tableLines == null || tableLines.isEmpty()) {
            return currentY;
        }

        List<List<String>> rows = new ArrayList<>();
        boolean hasHeaderSeparator = false;
        int columnCount = 0;

        for (String line : tableLines) {
            if (isMarkdownTableSeparator(line)) {
                hasHeaderSeparator = true;
                continue;
            }
            List<String> cells = parseTableCells(line);
            if (cells.isEmpty()) {
                continue;
            }
            columnCount = Math.max(columnCount, cells.size());
            rows.add(cells);
        }

        if (rows.isEmpty() || columnCount <= 0) {
            return currentY;
        }

        int[] widths = computeTableColumnWidths(rows, columnCount);

        for (int i = 0; i < rows.size(); i++) {
            int rowY = currentY;
            int rowType = (i == 0 && hasHeaderSeparator) ? TYPE_TABLE_HEADER : TYPE_TABLE;
            boolean drawTop = i == 0;
            boolean drawBottom = i == rows.size() - 1;
            boolean drawHeaderDivider = (i == 0 && hasHeaderSeparator);

            List<String> row = rows.get(i);
            for (String cell : row) {
                registerBoldAnchorsFromText(cell, rowY);
            }

            addTableLine(row, widths, rowType, drawTop, drawBottom, drawHeaderDivider);
            currentY += getEntryHeight(new RenderEntry(new TextComponentString(""), TYPE_TABLE));
        }

        // 表格后加一个空行
        RenderEntry spacer = new RenderEntry(new TextComponentString(" "), TYPE_SPACER);
        renderedLines.add(spacer);
        currentY += getEntryHeight(spacer);

        return currentY;
    }

    private void addTableLine(String line, int currentY) {
        addTableLine(line, currentY, TYPE_TABLE);
    }

    private void addTableLine(List<String> cells, int[] widths, int type,
            boolean drawTop, boolean drawBottom, boolean drawHeaderDivider) {
        renderedLines.add(new RenderEntry(new ArrayList<>(cells), widths.clone(), type,
                drawTop, drawBottom, drawHeaderDivider));
    }

    private void addTableLine(String line, int currentY, int type) {
        RenderEntry entry = new RenderEntry(new TextComponentString(line), type);
        renderedLines.add(entry);
    }

    private boolean isMarkdownTableSeparator(String line) {
        String t = (line == null ? "" : line).replace("|", "").trim();
        return !t.isEmpty() && t.matches("[-: ]+");
    }

    private List<String> parseTableCells(String line) {
        String[] raw = (line == null ? "" : line).split("\\|", -1);
        List<String> cells = new ArrayList<>();

        int start = 0;
        int end = raw.length;
        if (raw.length > 0 && raw[0].trim().isEmpty()) {
            start = 1;
        }
        if (raw.length > 1 && raw[raw.length - 1].trim().isEmpty()) {
            end = raw.length - 1;
        }

        for (int i = start; i < end; i++) {
            String s = raw[i] == null ? "" : raw[i].trim();
            s = s.replace("\\\\[", "[").replace("\\\\]", "]");
            cells.add(s);
        }

        return cells;
    }

    private int[] computeTableColumnWidths(List<List<String>> rows, int columnCount) {
        int[] widths = new int[columnCount];
        int minColWidth = 36;

        for (List<String> row : rows) {
            for (int i = 0; i < columnCount; i++) {
                String cell = i < row.size() ? row.get(i) : "";
                int w = this.fontRenderer.getStringWidth(stripMarkdownLinks(cell));
                widths[i] = Math.max(Math.max(widths[i], minColWidth), w + 8);
            }
        }

        return widths;
    }

    private String buildTableRowLine(List<String> row, int[] widths) {
        StringBuilder sb = new StringBuilder();
        sb.append('│');
        for (int i = 0; i < widths.length; i++) {
            String cell = i < row.size() ? row.get(i) : "";
            sb.append(fitCellToPixelWidth(cell, widths[i]));
            sb.append('│');
        }
        return sb.toString();
    }

    private void drawRealTableRow(int left, int right, int y, int h, RenderEntry rowEntry) {
        int borderColor = 0xFFB8D7FF;
        int borderColorStrong = 0xFFE8C45C;
        int textColor = rowEntry.type == TYPE_TABLE_HEADER ? 0xFFEED58A : 0xFFDCE6F5;

        int tableWidth = 1; // 左边框占1像素
        if (rowEntry.tableWidths != null) {
            for (int cw : rowEntry.tableWidths) {
                tableWidth += Math.max(0, cw);
            }
        }
        tableWidth = Math.max(20, tableWidth);
        int x = left;
        int yTop = y - 1;
        int yBottom = y + h - 1;

        // 上边框（首行）
        if (rowEntry.tableDrawTop) {
            drawRect(x, yTop, x + tableWidth, yTop + 1, borderColorStrong);
        }

        // 下边框（每行）
        int bottomColor = rowEntry.tableHeaderDivider ? borderColorStrong : borderColor;
        drawRect(x, yBottom, x + tableWidth, yBottom + 1, bottomColor);

        // 竖线 + 单元格文字
        int cursorX = x;
        drawRect(cursorX, yTop, cursorX + 1, yBottom + 1, borderColor);

        for (int i = 0; i < rowEntry.tableWidths.length; i++) {
            int cw = rowEntry.tableWidths[i];
            int cellTextWidth = Math.max(8, cw - 8);
            String raw = i < rowEntry.tableCells.size() ? rowEntry.tableCells.get(i) : "";
            drawStyledCellText(raw == null ? "" : raw, cursorX + 4, y, cellTextWidth, textColor);

            cursorX += cw;
            drawRect(cursorX, yTop, cursorX + 1, yBottom + 1, borderColor);
        }

        // 末行强化底框
        if (rowEntry.tableDrawBottom) {
            drawRect(x, yBottom, x + tableWidth, yBottom + 2, borderColorStrong);
        }
    }

    private String buildBorderLine(char left, char mid, char right, int[] widths) {
        StringBuilder sb = new StringBuilder();
        sb.append(left);
        int dashW = Math.max(1, this.fontRenderer.getStringWidth("─"));
        for (int i = 0; i < widths.length; i++) {
            int count = Math.max(2, widths[i] / dashW);
            for (int j = 0; j < count; j++) {
                sb.append('─');
            }
            sb.append(i == widths.length - 1 ? right : mid);
        }
        return sb.toString();
    }

    private String fitCellToPixelWidth(String text, int targetWidth) {
        String value = text == null ? "" : text;
        String ellipsis = "…";
        int pad = this.fontRenderer.getStringWidth(" ");
        int innerTarget = Math.max(8, targetWidth - 4);

        if (this.fontRenderer.getStringWidth(value) > innerTarget) {
            while (!value.isEmpty() && this.fontRenderer.getStringWidth(value + ellipsis) > innerTarget) {
                value = value.substring(0, value.length() - 1);
            }
            value = value + ellipsis;
        }

        StringBuilder sb = new StringBuilder();
        sb.append(' ').append(value);
        while (this.fontRenderer.getStringWidth(sb.toString()) + pad < targetWidth) {
            sb.append(' ');
        }
        sb.append(' ');
        return sb.toString();
    }

    private void drawStyledCellText(String raw, int x, int y, int maxWidth, int color) {
        List<StyledSegment> segments = parseMarkdownBoldSegments(stripMarkdownLinks(raw));
        int used = 0;

        for (StyledSegment seg : segments) {
            if (seg.text == null || seg.text.isEmpty()) {
                continue;
            }

            String prefix = seg.bold ? "§l" : "";
            String content = seg.text;
            int partWidth = this.fontRenderer.getStringWidth(prefix + content);

            if (used + partWidth <= maxWidth) {
                drawString(this.fontRenderer, prefix + content, x + used, y, color);
                used += partWidth;
                continue;
            }

            // 需要截断
            StringBuilder fit = new StringBuilder();
            for (int i = 0; i < content.length(); i++) {
                char ch = content.charAt(i);
                String candidate = fit.toString() + ch;
                int cw = this.fontRenderer.getStringWidth(prefix + candidate + "…");
                if (used + cw > maxWidth) {
                    break;
                }
                fit.append(ch);
            }

            if (fit.length() > 0) {
                drawString(this.fontRenderer, prefix + fit + "…", x + used, y, color);
            } else if (used == 0) {
                drawString(this.fontRenderer, "…", x + used, y, color);
            }
            return;
        }
    }

    private List<StyledSegment> parseMarkdownBoldSegments(String text) {
        List<StyledSegment> out = new ArrayList<>();
        if (text == null || text.isEmpty()) {
            return out;
        }

        String s = text.replace("\\\\[", "[").replace("\\\\]", "]");
        int idx = 0;
        while (idx < s.length()) {
            int start = s.indexOf("**", idx);
            if (start < 0) {
                out.add(new StyledSegment(s.substring(idx), false));
                break;
            }
            if (start > idx) {
                out.add(new StyledSegment(s.substring(idx, start), false));
            }
            int end = s.indexOf("**", start + 2);
            if (end < 0) {
                out.add(new StyledSegment(s.substring(start), false));
                break;
            }
            out.add(new StyledSegment(s.substring(start + 2, end), true));
            idx = end + 2;
        }

        return out;
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (searchField != null && searchField.textboxKeyTyped(typedChar, keyCode)) {
            String now = searchField.getText();
            if (!now.equals(searchQuery)) {
                searchQuery = now;
                updateSearchResults(true);
            }
            return;
        }

        if (keyCode == Keyboard.KEY_F3) {
            jumpToSearchHit(1);
            return;
        }
        if (keyCode == Keyboard.KEY_F && GuiScreen.isCtrlKeyDown()) {
            if (searchField != null) {
                searchField.setFocused(true);
            }
            return;
        }

        if (keyCode == 1) { // ESC
            returnToMainMenuOverlay();
            return;
        }
        super.keyTyped(typedChar, keyCode);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        super.mouseClicked(mouseX, mouseY, mouseButton);

        if (searchField != null) {
            searchField.mouseClicked(mouseX, mouseY, mouseButton);
        }

        if (mouseButton == 0) {
            int listLeft = this.contentX;
            int listRight = this.panelX + this.panelWidth - 10;
            int scrollbarX = listRight - 6;
            int viewportBottom = getContentViewportBottom();
            int contentScrollbarHeight = viewportBottom - this.contentTop;
            if (maxScroll > 0 && mouseX >= scrollbarX && mouseX <= scrollbarX + 8
                    && mouseY >= this.contentTop && mouseY <= viewportBottom) {
                int contentHeight = Math.max(1, totalContentHeight);
                int thumbHeight = Math.max(14,
                        (int) ((float) contentScrollbarHeight / contentHeight * contentScrollbarHeight));
                int thumbY = this.contentTop
                        + (int) ((float) scrollOffset / maxScroll * (contentScrollbarHeight - thumbHeight));
                if (mouseY >= thumbY && mouseY <= thumbY + thumbHeight) {
                    draggingScrollbar = true;
                    dragStartMouseY = mouseY;
                    dragStartScrollOffset = scrollOffset;
                }
            }

            int hTrackLeft = listLeft + 2;
            int hTrackRight = listRight - 10;
            int hTrackTop = viewportBottom + 2;
            int hTrackBottom = this.contentBottom - 2;
            if (maxHorizontalScroll > 0 && mouseX >= hTrackLeft && mouseX <= hTrackRight
                    && mouseY >= hTrackTop && mouseY <= hTrackBottom) {
                int trackW = Math.max(1, hTrackRight - hTrackLeft);
                int visibleW = Math.max(20, this.contentWidth - 24);
                int contentW = visibleW + maxHorizontalScroll;
                int thumbW = Math.max(18, (int) ((float) visibleW / contentW * trackW));
                int thumbX = hTrackLeft + (int) ((float) horizontalScrollOffset / maxHorizontalScroll * (trackW - thumbW));
                if (mouseX >= thumbX && mouseX <= thumbX + thumbW) {
                    draggingHorizontalScrollbar = true;
                    dragStartMouseX = mouseX;
                    dragStartHorizontalScrollOffset = horizontalScrollOffset;
                }
            }

            int tocScrollbarX = this.tocX + this.tocWidth - 6;
            int startY = this.tocTop + 16;
            int tocVisibleRows = Math.max(1, (this.tocBottom - startY - 4) / 12);
            if (maxTocScroll > 0 && mouseX >= tocScrollbarX && mouseX <= tocScrollbarX + 8
                    && mouseY >= startY && mouseY <= this.tocBottom) {
                int tocScrollbarHeight = this.tocBottom - startY - 4;
                int thumbHeight = Math.max(14,
                        (int) ((float) tocVisibleRows / tocEntries.size() * tocScrollbarHeight));
                int thumbY = startY
                        + (int) ((float) tocScrollOffset / maxTocScroll * (tocScrollbarHeight - thumbHeight));
                if (mouseY >= thumbY && mouseY <= thumbY + thumbHeight) {
                    draggingScrollbar = false;
                    draggingHorizontalScrollbar = false;
                    draggingTocScrollbar = true;
                    dragTocStartMouseY = mouseY;
                    dragTocStartScrollOffset = tocScrollOffset;
                    return;
                }
            }

            if (mouseX >= this.tocX && mouseX <= this.tocX + this.tocWidth - 8 && mouseY >= startY
                    && mouseY <= this.tocBottom) {
                int clickedIndex = (mouseY - startY) / 12 + tocScrollOffset;
                if (clickedIndex >= 0 && clickedIndex < tocEntries.size()) {
                    this.scrollOffset = MathHelper.clamp(tocEntries.get(clickedIndex).yPosition, 0, maxScroll);
                }
            }

            handleContentLinkClick(mouseX, mouseY);
        }
    }

    private void handleContentLinkClick(int mouseX, int mouseY) {
        int listLeft = this.contentX;
        int listRight = this.panelX + this.panelWidth - 10;
        int textLeft = listLeft + 8;
        int textRight = listRight - 12;

        if (mouseX < textLeft - 2 || mouseX > textRight || mouseY < this.contentTop || mouseY > this.contentBottom) {
            return;
        }

        int yCursor = this.contentTop + 3 - scrollOffset;
        for (int i = 0; i < renderedLines.size(); i++) {
            RenderEntry entry = renderedLines.get(i);
            int h = getEntryHeight(entry);

            if (mouseY >= yCursor && mouseY < yCursor + h) {
                String target = null;
                if ((entry.type == TYPE_TABLE || entry.type == TYPE_TABLE_HEADER) && entry.tableCells != null) {
                    for (String cell : entry.tableCells) {
                        target = findFirstLocalAnchorTarget(cell);
                        if (target != null) {
                            break;
                        }
                    }
                } else if (entry.text != null) {
                    target = findFirstLocalAnchorTarget(entry.text.getUnformattedText());
                }

                if (target != null) {
                    scrollToAnchor(target);
                }
                return;
            }

            yCursor += h;
        }
    }

    private String stripMarkdownLinks(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        Matcher m = MD_LOCAL_LINK_PATTERN.matcher(text);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            m.appendReplacement(sb, Matcher.quoteReplacement(m.group(1)));
        }
        m.appendTail(sb);
        return sb.toString();
    }

    private String findFirstLocalAnchorTarget(String text) {
        if (text == null || text.isEmpty()) {
            return null;
        }
        Matcher m = MD_LOCAL_LINK_PATTERN.matcher(text);
        if (m.find()) {
            return m.group(2);
        }
        return null;
    }

    private void registerBoldAnchorsFromText(String text, int yPosition) {
        if (text == null || text.isEmpty()) {
            return;
        }
        Matcher m = MD_BOLD_PATTERN.matcher(text);
        while (m.find()) {
            registerAnchor(m.group(1), yPosition);
        }
    }

    private void registerAnchor(String rawName, int yPosition) {
        String key = normalizeAnchorKey(rawName);
        if (key.isEmpty()) {
            return;
        }
        if (!anchorPositions.containsKey(key)) {
            anchorPositions.put(key, yPosition);
        }
    }

    private String normalizeAnchorKey(String raw) {
        if (raw == null) {
            return "";
        }
        String s = raw.trim();
        if (s.startsWith("#")) {
            s = s.substring(1);
        }
        s = s.toLowerCase(Locale.ROOT);
        s = s.replace(" ", "").replace("\t", "").replace("-", "");
        return s;
    }

    private void scrollToAnchor(String anchor) {
        String key = normalizeAnchorKey(anchor);
        if (key.isEmpty()) {
            return;
        }

        Integer y = anchorPositions.get(key);
        if (y != null) {
            this.scrollOffset = MathHelper.clamp(y, 0, maxScroll);
        }
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        int dWheel = Mouse.getEventDWheel();
        if (dWheel != 0) {
            int mouseX = Mouse.getEventX() * this.width / this.mc.displayWidth;
            int mouseY = this.height - Mouse.getEventY() * this.height / this.mc.displayHeight - 1;

            int listLeft = this.panelX + 10;
            int listRight = this.panelX + this.panelWidth - 10;
            int viewportBottom = getContentViewportBottom();
            if (mouseX >= this.tocX && mouseX <= this.tocX + this.tocWidth && mouseY >= this.tocTop
                    && mouseY <= this.tocBottom) {
                tocScrollOffset -= dWheel / 120;
                tocScrollOffset = MathHelper.clamp(tocScrollOffset, 0, maxTocScroll);
                return;
            }
            if (mouseX >= listLeft && mouseX <= listRight && mouseY >= this.contentTop
                    && mouseY <= this.contentBottom) {
                if (GuiScreen.isShiftKeyDown()) {
                    horizontalScrollOffset -= dWheel / 120 * 12;
                } else if (mouseY <= viewportBottom) {
                    scrollOffset -= dWheel / 120 * this.lineHeight;
                }
            }
            scrollOffset = MathHelper.clamp(scrollOffset, 0, maxScroll);
            horizontalScrollOffset = MathHelper.clamp(horizontalScrollOffset, 0, maxHorizontalScroll);
        }

        if (draggingScrollbar) {
            int mouseY = this.height - Mouse.getY() * this.height / this.mc.displayHeight - 1;
            int scrollbarHeight = getContentViewportBottom() - this.contentTop;
            int contentHeight = Math.max(1, totalContentHeight);
            int thumbHeight = Math.max(14, (int) ((float) scrollbarHeight / contentHeight * scrollbarHeight));
            int track = Math.max(1, scrollbarHeight - thumbHeight);
            int dy = mouseY - dragStartMouseY;
            int deltaScroll = (int) ((float) dy / (float) track * maxScroll);
            scrollOffset = MathHelper.clamp(dragStartScrollOffset + deltaScroll, 0, maxScroll);
        }

        if (draggingHorizontalScrollbar) {
            int mouseX = Mouse.getX() * this.width / this.mc.displayWidth;
            int listLeft = this.contentX;
            int listRight = this.panelX + this.panelWidth - 10;
            int hTrackLeft = listLeft + 2;
            int hTrackRight = listRight - 10;
            int trackW = Math.max(1, hTrackRight - hTrackLeft);
            int visibleW = Math.max(20, this.contentWidth - 24);
            int contentW = visibleW + maxHorizontalScroll;
            int thumbW = Math.max(18, (int) ((float) visibleW / contentW * trackW));
            int track = Math.max(1, trackW - thumbW);
            int dx = mouseX - dragStartMouseX;
            int deltaScroll = (int) ((float) dx / (float) track * maxHorizontalScroll);
            horizontalScrollOffset = MathHelper.clamp(dragStartHorizontalScrollOffset + deltaScroll, 0,
                    maxHorizontalScroll);
        }

        if (draggingTocScrollbar) {
            int mouseY = this.height - Mouse.getY() * this.height / this.mc.displayHeight - 1;
            int scrollbarTop = this.tocTop + 16;
            int visibleRows = Math.max(1, (this.tocBottom - scrollbarTop - 4) / 12);
            int tocScrollbarHeight = this.tocBottom - scrollbarTop - 4;
            int thumbHeight = Math.max(14, (int) ((float) visibleRows / tocEntries.size() * tocScrollbarHeight));
            int track = Math.max(1, tocScrollbarHeight - thumbHeight);
            int dy = mouseY - dragTocStartMouseY;
            int deltaScroll = (int) ((float) dy / (float) track * maxTocScroll);
            tocScrollOffset = MathHelper.clamp(dragTocStartScrollOffset + deltaScroll, 0, maxTocScroll);
        }
    }

    @Override
    protected void mouseReleased(int mouseX, int mouseY, int state) {
        super.mouseReleased(mouseX, mouseY, state);
        if (state == 0) {
            draggingScrollbar = false;
            draggingHorizontalScrollbar = false;
            draggingTocScrollbar = false;
        }
    }

    @Override
    public void updateScreen() {
        super.updateScreen();
        if (searchField != null) {
            searchField.updateCursorCounter();
        }
    }

    @Override
    public void onGuiClosed() {
        super.onGuiClosed();
        Keyboard.enableRepeatEvents(false);
    }

    private void updateSearchResults(boolean jumpToFirst) {
        searchHitLineIndices.clear();
        currentSearchHit = -1;

        String q = searchQuery == null ? "" : searchQuery.trim().toLowerCase();
        if (q.isEmpty()) {
            return;
        }

        for (int i = 0; i < renderedLines.size(); i++) {
            RenderEntry e = renderedLines.get(i);
            String text;
            if ((e.type == TYPE_TABLE || e.type == TYPE_TABLE_HEADER) && e.tableCells != null) {
                text = String.join(" ", e.tableCells);
            } else {
                text = e.text == null ? "" : e.text.getUnformattedText();
            }
            if (text != null && text.toLowerCase().contains(q)) {
                searchHitLineIndices.add(i);
            }
        }

        if (!searchHitLineIndices.isEmpty() && jumpToFirst) {
            currentSearchHit = 0;
            jumpToHitLine(searchHitLineIndices.get(0));
        }
    }

    private void jumpToSearchHit(int direction) {
        if (searchHitLineIndices.isEmpty()) {
            return;
        }
        if (currentSearchHit < 0) {
            currentSearchHit = 0;
        } else {
            int size = searchHitLineIndices.size();
            currentSearchHit = (currentSearchHit + direction + size) % size;
        }
        jumpToHitLine(searchHitLineIndices.get(currentSearchHit));
    }

    private void jumpToHitLine(int targetLineIndex) {
        int y = 0;
        for (int i = 0; i < targetLineIndex && i < renderedLines.size(); i++) {
            y += getEntryHeight(renderedLines.get(i));
        }
        int viewH = Math.max(1, this.contentBottom - this.contentTop - 8);
        int centerOffset = Math.max(0, viewH / 2);
        scrollOffset = MathHelper.clamp(y - centerOffset, 0, maxScroll);
    }
}

