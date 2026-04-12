// 文件路径: src/main/java/com/zszl/zszlScriptMod/gui/changelog/GuiChangelog.java
package com.zszl.zszlScriptMod.gui.changelog;

import com.zszl.zszlScriptMod.gui.GuiInventory;
import com.zszl.zszlScriptMod.gui.components.ThemedButton;
import com.zszl.zszlScriptMod.gui.components.ThemedGuiScreen;
import com.zszl.zszlScriptMod.utils.UpdateChecker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiUtilRenderComponents;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.event.ClickEvent;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

public class GuiChangelog extends ThemedGuiScreen {

    private static final int TYPE_NORMAL = 0;
    private static final int TYPE_H1 = 1;
    private static final int TYPE_H2 = 2;
    private static final int TYPE_H3 = 3;
    private static final int TYPE_QUOTE = 4;
    private static final int TYPE_LIST = 5;
    private static final int TYPE_SEPARATOR = 6;
    private static final int TYPE_SPACER = 7;

    private static class RenderEntry {
        final ITextComponent text;
        final int type;

        RenderEntry(ITextComponent text, int type) {
            this.text = text;
            this.type = type;
        }
    }

    private final GuiScreen parentScreen;
    private String markdownContent;

    // --- 核心数据结构 ---
    private List<RenderEntry> renderedLines = new ArrayList<>();
    private List<TocEntry> tocEntries = new ArrayList<>();

    // --- 滚动与布局 ---
    private int scrollOffset = 0;
    private int maxScroll = 0;
    private int tocScrollOffset = 0;
    private int maxTocScroll = 0;
    private int totalContentHeight = 0;
    private int tocWidth = 140;
    private int contentX, contentWidth;
    private int panelX, panelY, panelWidth, panelHeight;
    private int tocX, tocTop, tocBottom;
    private boolean draggingContentScrollbar = false;
    private int dragContentStartMouseY = 0;
    private int dragContentStartScrollOffset = 0;
    private boolean draggingTocScrollbar = false;
    private int dragTocStartMouseY = 0;
    private int dragTocStartScrollOffset = 0;

    /**
     * 内部类，用于存储目录条目
     */
    private static class TocEntry {
        final String text;
        final int yPosition; // 在渲染列表中的Y坐标
        final int level; // 标题级别 (1 for #, 2 for ##)

        TocEntry(String text, int yPosition, int level) {
            this.text = text;
            this.yPosition = yPosition;
            this.level = level;
        }
    }

    public GuiChangelog(GuiScreen parent, String content) {
        this.parentScreen = parent;
        this.markdownContent = content == null ? I18n.format("gui.changelog.empty_content") : content;
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
        this.buttonList.clear();

        this.panelWidth = Math.min(760, this.width - 40);
        this.panelHeight = Math.min(360, this.height - 30);
        this.panelX = (this.width - this.panelWidth) / 2;
        this.panelY = (this.height - this.panelHeight) / 2;

        this.tocX = this.panelX + 12;
        this.tocTop = this.panelY + 36;
        this.tocBottom = this.panelY + this.panelHeight - 36;

        // 定义布局：左目录 + 右正文
        this.contentX = this.tocX + this.tocWidth + 12;
        this.contentWidth = (this.panelX + this.panelWidth - 12) - this.contentX;

        this.buttonList.add(new ThemedButton(0, this.width / 2 - 50, this.panelY + this.panelHeight - 26, 100, 18,
                I18n.format("gui.common.back")));

        parseContent();
    }

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

    private String stripMarkdownForToc(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }

        String cleaned = text;
        cleaned = cleaned.replaceAll("\\[([^\\]]+)\\]\\(([^)]+)\\)", "$1");
        cleaned = cleaned.replace("**", "")
                .replace("__", "")
                .replace("*", "")
                .replace("_", "")
                .replace("`", "");
        return cleaned.trim();
    }

    private boolean handleTextComponentClick(ITextComponent component) {
        if (component == null) {
            return false;
        }

        Style style = component.getStyle();
        ClickEvent clickEvent = style == null ? null : style.getClickEvent();
        if (clickEvent != null && clickEvent.getAction() == ClickEvent.Action.OPEN_URL) {
            try {
                if (Desktop.isDesktopSupported()) {
                    Desktop desktop = Desktop.getDesktop();
                    if (desktop.isSupported(Desktop.Action.BROWSE)) {
                        desktop.browse(new URI(clickEvent.getValue()));
                        return true;
                    }
                }
            } catch (Exception ignored) {
            }
        }

        return this.handleComponentClick(component);
    }

    private void updateContentDrag(int mouseY) {
        if (!draggingContentScrollbar || maxScroll <= 0) {
            return;
        }

        int scrollbarHeight = this.tocBottom - this.tocTop;
        int contentHeight = Math.max(1, totalContentHeight);
        int thumbHeight = Math.max(14, (int) ((float) scrollbarHeight / contentHeight * scrollbarHeight));
        int track = Math.max(1, scrollbarHeight - thumbHeight);
        int dy = mouseY - dragContentStartMouseY;
        int deltaScroll = (int) ((float) dy / (float) track * maxScroll);
        scrollOffset = MathHelper.clamp(dragContentStartScrollOffset + deltaScroll, 0, maxScroll);
    }

    private void updateTocDrag(int mouseY) {
        if (!draggingTocScrollbar || maxTocScroll <= 0) {
            return;
        }

        int scrollbarTop = this.tocTop + 20;
        int visibleRows = Math.max(1, (tocBottom - scrollbarTop - 4) / 12);
        int scrollbarHeight = tocBottom - scrollbarTop - 4;
        int thumbHeight = Math.max(14, (int) ((float) visibleRows / tocEntries.size() * scrollbarHeight));
        int track = Math.max(1, scrollbarHeight - thumbHeight);
        int dy = mouseY - dragTocStartMouseY;
        int deltaScroll = (int) ((float) dy / (float) track * maxTocScroll);
        tocScrollOffset = MathHelper.clamp(dragTocStartScrollOffset + deltaScroll, 0, maxTocScroll);
    }

    private int getEntryHeight(RenderEntry entry) {
        switch (entry.type) {
            case TYPE_H1:
                return fontRenderer.FONT_HEIGHT + 8;
            case TYPE_H2:
                return fontRenderer.FONT_HEIGHT + 6;
            case TYPE_H3:
                return fontRenderer.FONT_HEIGHT + 4;
            case TYPE_QUOTE:
                return fontRenderer.FONT_HEIGHT + 3;
            case TYPE_LIST:
                return fontRenderer.FONT_HEIGHT + 2;
            case TYPE_SEPARATOR:
                return fontRenderer.FONT_HEIGHT + 4;
            case TYPE_SPACER:
                return 4;
            default:
                return fontRenderer.FONT_HEIGHT + 1;
        }
    }

    private ITextComponent parseInlineFormatting(String lineText) {
        TextComponentString finalComponent = new TextComponentString("");
        String remainingLine = lineText;

        while (!remainingLine.isEmpty()) {
            int boldStart = remainingLine.indexOf("**");
            int italicStart = remainingLine.indexOf("*");
            int linkStart = remainingLine.indexOf("[");

            int nextTag = -1;
            if (boldStart != -1) {
                nextTag = boldStart;
            }
            if (italicStart != -1 && italicStart != boldStart && (nextTag == -1 || italicStart < nextTag)) {
                nextTag = italicStart;
            }
            if (linkStart != -1 && (nextTag == -1 || linkStart < nextTag)) {
                nextTag = linkStart;
            }

            if (nextTag == -1) {
                finalComponent.appendText(remainingLine);
                break;
            }

            if (nextTag > 0) {
                finalComponent.appendText(remainingLine.substring(0, nextTag));
            }

            remainingLine = remainingLine.substring(nextTag);

            if (remainingLine.startsWith("**")) {
                int boldEnd = remainingLine.indexOf("**", 2);
                if (boldEnd != -1) {
                    String boldText = remainingLine.substring(2, boldEnd);
                    finalComponent.appendSibling(new TextComponentString(boldText).setStyle(new Style().setBold(true)));
                    remainingLine = remainingLine.substring(boldEnd + 2);
                } else {
                    finalComponent.appendText("**");
                    remainingLine = remainingLine.substring(2);
                }
            } else if (remainingLine.startsWith("*")) {
                int italicEnd = remainingLine.indexOf("*", 1);
                if (italicEnd != -1) {
                    String italicText = remainingLine.substring(1, italicEnd);
                    finalComponent.appendSibling(
                            new TextComponentString(italicText).setStyle(new Style().setItalic(true)));
                    remainingLine = remainingLine.substring(italicEnd + 1);
                } else {
                    finalComponent.appendText("*");
                    remainingLine = remainingLine.substring(1);
                }
            } else if (remainingLine.startsWith("[")) {
                int linkTextEnd = remainingLine.indexOf("]");
                int linkUrlStart = remainingLine.indexOf("(", linkTextEnd);
                int linkUrlEnd = remainingLine.indexOf(")", linkUrlStart);

                if (linkTextEnd != -1 && linkUrlStart == linkTextEnd + 1 && linkUrlEnd != -1) {
                    String linkText = remainingLine.substring(1, linkTextEnd);
                    String url = remainingLine.substring(linkUrlStart + 1, linkUrlEnd);
                    finalComponent.appendSibling(
                            new TextComponentString(linkText)
                                    .setStyle(new Style().setColor(TextFormatting.BLUE).setUnderlined(true)
                                            .setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, url))));
                    remainingLine = remainingLine.substring(linkUrlEnd + 1);
                } else {
                    finalComponent.appendText("[");
                    remainingLine = remainingLine.substring(1);
                }
            }
        }

        return finalComponent;
    }

    private void parseContent() {
        this.renderedLines.clear();
        this.tocEntries.clear();
        String[] lines = markdownContent.split("\n");
        int currentY = 0;

        for (String line : lines) {
            String trimmed = line == null ? "" : line.trim();
            ITextComponent component;
            int type;
            int tocLevel = 0;
            String tocTitle = null;

            if (trimmed.isEmpty()) {
                component = new TextComponentString(" ");
                type = TYPE_SPACER;
            } else if (trimmed.startsWith("### ")) {
                String title = trimmed.substring(4);
                component = parseInlineFormatting(title)
                        .setStyle(new Style().setBold(true).setColor(TextFormatting.WHITE));
                type = TYPE_H3;
                tocLevel = 3;
                tocTitle = stripMarkdownForToc(title);
            } else if (trimmed.startsWith("## ")) {
                String title = trimmed.substring(3);
                component = parseInlineFormatting(title)
                        .setStyle(new Style().setBold(true).setColor(TextFormatting.AQUA));
                type = TYPE_H2;
                tocLevel = 2;
                tocTitle = stripMarkdownForToc(title);
            } else if (trimmed.startsWith("# ")) {
                String title = trimmed.substring(2);
                component = parseInlineFormatting(title)
                        .setStyle(new Style().setBold(true).setUnderlined(true).setColor(TextFormatting.GOLD));
                type = TYPE_H1;
                tocLevel = 1;
                tocTitle = stripMarkdownForToc(title);
            } else if (trimmed.startsWith("> ")) {
                component = new TextComponentString("    ").appendSibling(parseInlineFormatting(trimmed.substring(2)))
                        .setStyle(new Style().setColor(TextFormatting.GRAY).setItalic(true));
                type = TYPE_QUOTE;
            } else if (trimmed.startsWith("- ") || trimmed.startsWith("* ")) {
                component = new TextComponentString(" • ").appendSibling(parseInlineFormatting(trimmed.substring(2)));
                type = TYPE_LIST;
            } else if (trimmed.matches("^-{3,}.*")) {
                component = new TextComponentString(
                        "§7§m                                                                                ");
                type = TYPE_SEPARATOR;
            } else {
                component = parseInlineFormatting(trimmed);
                type = TYPE_NORMAL;
            }

            if (tocLevel > 0 && tocTitle != null && !tocTitle.trim().isEmpty()) {
                tocEntries.add(new TocEntry(tocTitle.trim(), currentY, tocLevel));
            }

            List<ITextComponent> split = GuiUtilRenderComponents.splitText(component,
                    this.contentWidth - 8, this.fontRenderer, false, true);
            if (split.isEmpty()) {
                RenderEntry entry = new RenderEntry(new TextComponentString(" "), TYPE_SPACER);
                renderedLines.add(entry);
                currentY += getEntryHeight(entry);
            } else {
                for (int i = 0; i < split.size(); i++) {
                    int t = (i == 0) ? type : TYPE_NORMAL;
                    RenderEntry entry = new RenderEntry(split.get(i), t);
                    renderedLines.add(entry);
                    currentY += getEntryHeight(entry);
                }
            }

            if (type == TYPE_H1 || type == TYPE_H2 || type == TYPE_H3 || type == TYPE_QUOTE || type == TYPE_LIST) {
                RenderEntry spacer = new RenderEntry(new TextComponentString(" "), TYPE_SPACER);
                renderedLines.add(spacer);
                currentY += getEntryHeight(spacer);
            }
        }

        totalContentHeight = 0;
        for (RenderEntry entry : renderedLines) {
            totalContentHeight += getEntryHeight(entry);
        }
        int viewHeight = Math.max(1, this.tocBottom - this.tocTop - 8);
        maxScroll = Math.max(0, totalContentHeight - viewHeight);
        scrollOffset = MathHelper.clamp(scrollOffset, 0, maxScroll);

        int tocVisibleRows = Math.max(1, (this.tocBottom - (this.tocTop + 20) - 4) / 12);
        maxTocScroll = Math.max(0, tocEntries.size() - tocVisibleRows);
        tocScrollOffset = MathHelper.clamp(tocScrollOffset, 0, maxTocScroll);
    }

    private void drawTableOfContents(int mouseX, int mouseY) {
        int tocY = tocTop;

        drawRect(tocX, tocY, tocX + tocWidth, tocBottom, 0x800B0F14);
        drawCenteredString(fontRenderer, I18n.format("gui.changelog.toc"), tocX + tocWidth / 2, tocY, 0xFFFFFF);

        int currentY = tocY + 20;

        int currentTocIndex = -1;
        for (int i = tocEntries.size() - 1; i >= 0; i--) {
            if (scrollOffset >= tocEntries.get(i).yPosition) {
                currentTocIndex = i;
                break;
            }
        }

        int visibleRows = Math.max(1, (tocBottom - currentY - 4) / 12);
        for (int i = 0; i < visibleRows; i++) {
            int actualIndex = i + tocScrollOffset;
            if (actualIndex < 0 || actualIndex >= tocEntries.size()) {
                break;
            }
            TocEntry entry = tocEntries.get(actualIndex);
            int entryX = tocX + (entry.level <= 1 ? 5 : (entry.level == 2 ? 12 : 19));
            int entryWidth = tocWidth - (entryX - tocX) - 5;

            if (currentY + 12 > tocBottom) {
                break;
            }

            boolean isHovered = mouseX >= tocX && mouseX <= tocX + tocWidth && mouseY >= currentY
                    && mouseY < currentY + 12;

            TextFormatting color = (actualIndex == currentTocIndex) ? TextFormatting.YELLOW
                    : (isHovered ? TextFormatting.WHITE : TextFormatting.GRAY);

            fontRenderer.drawString(color + fontRenderer.trimStringToWidth(entry.text, entryWidth), entryX, currentY,
                    0xFFFFFF);
            currentY += 12;
        }

        if (maxTocScroll > 0) {
            int scrollbarX = tocX + tocWidth - 6;
            int scrollbarTop = tocY + 20;
            int scrollbarHeight = tocBottom - scrollbarTop - 4;
            drawRect(scrollbarX, scrollbarTop, scrollbarX + 4, scrollbarTop + scrollbarHeight, 0xFF202732);
            int thumbHeight = Math.max(14, (int) ((float) visibleRows / tocEntries.size() * scrollbarHeight));
            int thumbY = scrollbarTop + (int) ((float) tocScrollOffset / maxTocScroll * (scrollbarHeight - thumbHeight));
            drawRect(scrollbarX, thumbY, scrollbarX + 6, thumbY + thumbHeight, 0xFF888888);
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        net.minecraft.client.gui.ScaledResolution scaledResolution = new net.minecraft.client.gui.ScaledResolution(mc);
        int scaleFactor = scaledResolution.getScaleFactor();

        // 实时刷新：若后台拉取到新日志，立即重排并显示
        String latest = UpdateChecker.changelogContent;
        if (latest != null && !latest.equals(this.markdownContent)) {
            this.markdownContent = latest;
            parseContent();
        }

        if (draggingContentScrollbar) {
            updateContentDrag(mouseY);
        }
        if (draggingTocScrollbar) {
            updateTocDrag(mouseY);
        }

        drawGradientRect(0, 0, this.width, this.height, 0xA0000000, 0xD0000000);

        // 卡片阴影
        drawRect(this.panelX - 4, this.panelY - 2, this.panelX + this.panelWidth + 6,
                this.panelY + this.panelHeight + 6,
                0x40000000);
        drawRect(this.panelX - 2, this.panelY - 1, this.panelX + this.panelWidth + 3,
                this.panelY + this.panelHeight + 3,
                0x25000000);

        // 主面板
        drawRect(this.panelX, this.panelY, this.panelX + this.panelWidth, this.panelY + this.panelHeight, 0xC0181E28);
        drawHorizontalLine(this.panelX, this.panelX + this.panelWidth - 1, this.panelY, 0xFF5A6A80);
        drawHorizontalLine(this.panelX, this.panelX + this.panelWidth - 1, this.panelY + this.panelHeight - 1,
                0xFF5A6A80);
        drawVerticalLine(this.panelX, this.panelY, this.panelY + this.panelHeight - 1, 0xFF5A6A80);
        drawVerticalLine(this.panelX + this.panelWidth - 1, this.panelY, this.panelY + this.panelHeight - 1,
                0xFF5A6A80);

        drawFancyTitle(I18n.format("gui.changelog.main_title"), this.panelX + this.panelWidth / 2, this.panelY + 10);

        drawTableOfContents(mouseX, mouseY);

        int contentTop = this.tocTop;
        int contentBottom = this.tocBottom;

        int contentRight = this.panelX + this.panelWidth - 12;
        drawRect(this.contentX, contentTop, contentRight, contentBottom, 0x800B0F14);

        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        GL11.glScissor(
                this.contentX * scaleFactor,
                (this.height - contentBottom) * scaleFactor,
                (contentRight - this.contentX) * scaleFactor,
                (contentBottom - contentTop) * scaleFactor);

        int yCursor = contentTop + 3 - scrollOffset;
        int hoveredIndex = -1;

        int hitYCursor = contentTop + 3 - scrollOffset;
        for (int i = 0; i < renderedLines.size(); i++) {
            int h = getEntryHeight(renderedLines.get(i));
            if (mouseX >= contentX + 2 && mouseX <= contentRight - 2 && mouseY >= hitYCursor
                    && mouseY < hitYCursor + h) {
                hoveredIndex = i;
                break;
            }
            hitYCursor += h;
        }

        for (int i = 0; i < renderedLines.size(); i++) {
            RenderEntry line = renderedLines.get(i);
            int entryHeight = getEntryHeight(line);
            if (yCursor + entryHeight >= contentTop && yCursor <= contentBottom - fontRenderer.FONT_HEIGHT) {
                if (i == hoveredIndex && line.type != TYPE_SPACER) {
                    drawRect(contentX + 3, yCursor - 1, contentRight - 3, yCursor + entryHeight - 1, 0x332E8BFF);
                }
                if (line.type == TYPE_QUOTE) {
                    drawRect(contentX + 4, yCursor - 1, contentX + 6, yCursor + entryHeight - 2, 0xAA6C7A8C);
                }
                fontRenderer.drawString(line.text.getFormattedText(), contentX + 8, yCursor, 0xFFFFFF);
            }
            yCursor += entryHeight;
        }

        GL11.glDisable(GL11.GL_SCISSOR_TEST);

        if (maxScroll > 0) {
            int scrollbarX = this.panelX + this.panelWidth - 10;
            int scrollbarHeight = contentBottom - contentTop;
            drawRect(scrollbarX, contentTop, scrollbarX + 4, contentBottom, 0xFF202732);
            int contentHeight = Math.max(1, totalContentHeight);
            int thumbHeight = Math.max(14,
                    (int) ((float) scrollbarHeight / contentHeight * scrollbarHeight));
            int thumbY = contentTop + (int) ((float) scrollOffset / maxScroll * (scrollbarHeight - thumbHeight));
            drawRect(scrollbarX, thumbY, scrollbarX + 6, thumbY + thumbHeight, 0xFF888888);
        }

        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        if (button.id == 0) {
            returnToMainMenuOverlay();
        }
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (keyCode == 1) { // ESC
            returnToMainMenuOverlay();
            return;
        }
        super.keyTyped(typedChar, keyCode);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        super.mouseClicked(mouseX, mouseY, mouseButton);

        if (mouseButton == 0) {
            ITextComponent component = this.getChatComponent(mouseX, mouseY);
            if (component != null && this.handleTextComponentClick(component)) {
                return;
            }
        }

        int contentScrollbarX = this.panelX + this.panelWidth - 10;
        if (maxScroll > 0 && mouseX >= contentScrollbarX && mouseX <= contentScrollbarX + 8
                && mouseY >= this.tocTop && mouseY <= this.tocBottom) {
            int scrollbarHeight = this.tocBottom - this.tocTop;
            int contentHeight = Math.max(1, totalContentHeight);
            int thumbHeight = Math.max(14, (int) ((float) scrollbarHeight / contentHeight * scrollbarHeight));
            int trackHeight = Math.max(1, scrollbarHeight - thumbHeight);
            int thumbY = this.tocTop + (int) ((float) scrollOffset / maxScroll * trackHeight);

            if (mouseY < thumbY || mouseY > thumbY + thumbHeight) {
                float progress = (float) (mouseY - this.tocTop - thumbHeight / 2) / (float) trackHeight;
                this.scrollOffset = MathHelper.clamp(Math.round(progress * maxScroll), 0, maxScroll);
                thumbY = this.tocTop + (int) ((float) scrollOffset / maxScroll * trackHeight);
            }

            draggingContentScrollbar = true;
            dragContentStartMouseY = mouseY;
            dragContentStartScrollOffset = scrollOffset;
            return;
        }

        int tocScrollbarX = tocX + tocWidth - 6;
        int tocY = this.tocTop + 20;
        int tocVisibleRows = Math.max(1, (tocBottom - tocY - 4) / 12);
        if (maxTocScroll > 0 && mouseX >= tocScrollbarX && mouseX <= tocScrollbarX + 8
                && mouseY >= tocY && mouseY <= tocBottom) {
            int scrollbarHeight = tocBottom - tocY - 4;
            int thumbHeight = Math.max(14, (int) ((float) tocVisibleRows / tocEntries.size() * scrollbarHeight));
            int trackHeight = Math.max(1, scrollbarHeight - thumbHeight);
            int thumbY = tocY + (int) ((float) tocScrollOffset / maxTocScroll * trackHeight);

            if (mouseY < thumbY || mouseY > thumbY + thumbHeight) {
                float progress = (float) (mouseY - tocY - thumbHeight / 2) / (float) trackHeight;
                this.tocScrollOffset = MathHelper.clamp(Math.round(progress * maxTocScroll), 0, maxTocScroll);
                thumbY = tocY + (int) ((float) tocScrollOffset / maxTocScroll * trackHeight);
            }

            draggingTocScrollbar = true;
            dragTocStartMouseY = mouseY;
            dragTocStartScrollOffset = tocScrollOffset;
            return;
        }

        if (mouseX >= tocX && mouseX <= tocX + tocWidth - 8 && mouseY >= tocY && mouseY <= tocBottom) {
            int clickedIndex = (mouseY - tocY) / 12 + tocScrollOffset;
            if (clickedIndex >= 0 && clickedIndex < tocEntries.size()) {
                this.scrollOffset = MathHelper.clamp(tocEntries.get(clickedIndex).yPosition, 0, maxScroll);
            }
        }
    }

    @javax.annotation.Nullable
    public ITextComponent getChatComponent(int mouseX, int mouseY) {
        if (this.renderedLines.isEmpty()) {
            return null;
        }

        int contentTop = this.tocTop;
        int contentBottom = this.tocBottom;
        int contentRight = this.panelX + this.panelWidth - 12;

        if (mouseX < this.contentX || mouseX > contentRight || mouseY < contentTop || mouseY > contentBottom) {
            return null;
        }

        int yCursor = contentTop + 3 - scrollOffset;
        for (RenderEntry line : this.renderedLines) {
            int h = getEntryHeight(line);
            if (mouseY >= yCursor && mouseY < yCursor + h) {
                int textStartX = this.contentX + 8;
                int localX = mouseX - textStartX;
                if (localX < 0) {
                    return null;
                }

                int widthSoFar = 0;
                for (ITextComponent part : line.text) {
                    String partText = part.getUnformattedComponentText();
                    if (partText == null || partText.isEmpty()) {
                        continue;
                    }
                    int partWidth = this.fontRenderer.getStringWidth(partText);
                    if (localX < widthSoFar + partWidth) {
                        return part;
                    }
                    widthSoFar += partWidth;
                }

                return null;
            }
            yCursor += h;
        }

        return null;
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        int dWheel = Mouse.getEventDWheel();
        if (dWheel != 0) {
            int mouseX = Mouse.getEventX() * this.width / this.mc.displayWidth;
            int mouseY = this.height - Mouse.getEventY() * this.height / this.mc.displayHeight - 1;
            int contentRight = this.panelX + this.panelWidth - 12;
            if (mouseX >= this.tocX && mouseX <= this.tocX + this.tocWidth && mouseY >= this.tocTop
                    && mouseY <= this.tocBottom) {
                tocScrollOffset -= dWheel / 120;
                tocScrollOffset = MathHelper.clamp(tocScrollOffset, 0, maxTocScroll);
                return;
            }
            if (mouseX >= this.contentX && mouseX <= contentRight && mouseY >= this.tocTop
                    && mouseY <= this.tocBottom) {
                scrollOffset -= dWheel / 120 * (fontRenderer.FONT_HEIGHT + 1);
                scrollOffset = MathHelper.clamp(scrollOffset, 0, maxScroll);
            }
        }

        if (draggingContentScrollbar) {
            int mouseY = this.height - Mouse.getY() * this.height / this.mc.displayHeight - 1;
            updateContentDrag(mouseY);
        }

        if (draggingTocScrollbar) {
            int mouseY = this.height - Mouse.getY() * this.height / this.mc.displayHeight - 1;
            updateTocDrag(mouseY);
        }
    }

    @Override
    protected void mouseClickMove(int mouseX, int mouseY, int clickedMouseButton, long timeSinceLastClick) {
        super.mouseClickMove(mouseX, mouseY, clickedMouseButton, timeSinceLastClick);
        if (clickedMouseButton == 0) {
            if (draggingContentScrollbar) {
                updateContentDrag(mouseY);
            }
            if (draggingTocScrollbar) {
                updateTocDrag(mouseY);
            }
        }
    }

    @Override
    protected void mouseReleased(int mouseX, int mouseY, int state) {
        super.mouseReleased(mouseX, mouseY, state);
        if (state == 0) {
            draggingContentScrollbar = false;
            draggingTocScrollbar = false;
        }
    }
}

