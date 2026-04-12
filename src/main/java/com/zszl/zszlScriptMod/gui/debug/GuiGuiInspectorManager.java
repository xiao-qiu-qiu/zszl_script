package com.zszl.zszlScriptMod.gui.debug;

import com.zszl.zszlScriptMod.gui.components.GuiTheme;
import com.zszl.zszlScriptMod.gui.components.ThemedButton;
import com.zszl.zszlScriptMod.gui.components.ThemedGuiScreen;
import com.zszl.zszlScriptMod.utils.guiinspect.GuiElementInspector;
import com.zszl.zszlScriptMod.utils.guiinspect.GuiInspectionManager;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class GuiGuiInspectorManager extends ThemedGuiScreen {

    private static final int BTN_TOGGLE_CAPTURE = 1;
    private static final int BTN_CLEAR = 2;
    private static final int BTN_COPY_PATH = 3;
    private static final int BTN_BACK = 4;
    private static final int BTN_TOGGLE_INFO = 5;

    private static final int HISTORY_ROW_HEIGHT = 38;
    private static final int ELEMENT_ROW_HEIGHT = 34;

    private final GuiScreen parentScreen;

    private int panelX;
    private int panelY;
    private int panelW;
    private int panelH;

    private int historyX;
    private int historyY;
    private int historyW;
    private int historyH;
    private int historyContentY;
    private int historyContentH;

    private int detailX;
    private int detailY;
    private int detailW;
    private int detailH;
    private int detailInfoH;

    private int elementCardY;
    private int elementCardH;
    private int elementListY;
    private int elementListH;

    private int detailFooterY;
    private int detailFooterH;
    private int detailFooterContentY;
    private int detailFooterContentH;

    private int historyScroll = 0;
    private int historyMaxScroll = 0;
    private int elementScroll = 0;
    private int elementMaxScroll = 0;
    private int selectedSnapshotIndex = -1;
    private int selectedElementIndex = -1;
    private boolean detailInfoExpanded = true;

    private GuiButton infoToggleButton;

    private float uiScale() {
        float sx = this.width / 980.0f;
        float sy = this.height / 620.0f;
        return Math.max(0.68f, Math.min(1.0f, Math.min(sx, sy)));
    }

    private int s(int base) {
        return Math.max(1, Math.round(base * uiScale()));
    }

    public GuiGuiInspectorManager(GuiScreen parentScreen) {
        this.parentScreen = parentScreen;
    }

    @Override
    public void initGui() {
        this.buttonList.clear();
        recalcLayout();

        int buttonY = panelY + panelH - s(28);
        int gap = s(8);
        int buttonCount = 4;
        int availableWidth = panelW - s(24);
        int buttonWidth = Math.max(s(58), Math.min(s(100), (availableWidth - gap * (buttonCount - 1)) / buttonCount));
        int totalButtonWidth = buttonWidth * buttonCount + gap * (buttonCount - 1);
        int startX = panelX + (panelW - totalButtonWidth) / 2;

        this.buttonList.add(new ThemedButton(BTN_TOGGLE_CAPTURE, startX, buttonY, buttonWidth, 20, ""));
        this.buttonList.add(new ThemedButton(BTN_CLEAR, startX + (buttonWidth + gap), buttonY, buttonWidth, 20,
                "清空历史"));
        this.buttonList.add(new ThemedButton(BTN_COPY_PATH, startX + 2 * (buttonWidth + gap), buttonY, buttonWidth, 20,
                "复制路径"));
        this.buttonList.add(new ThemedButton(BTN_BACK, startX + 3 * (buttonWidth + gap), buttonY, buttonWidth, 20,
                "返回"));

        int infoToggleSize = s(18);
        infoToggleButton = new ThemedButton(BTN_TOGGLE_INFO,
                detailX + detailW - infoToggleSize - s(8),
                detailY + s(6),
                infoToggleSize,
                infoToggleSize,
                "");
        this.buttonList.add(infoToggleButton);

        syncSelectionBounds();
        refreshButtons();
    }

    private void recalcLayout() {
        int margin = s(10);
        panelW = Math.max(s(420), Math.min(s(1000), this.width - margin * 2));
        panelH = Math.max(s(320), Math.min(s(640), this.height - margin * 2));
        panelX = (this.width - panelW) / 2;
        panelY = (this.height - panelH) / 2;

        int innerPadding = s(12);
        int columnGap = s(10);
        int contentTop = panelY + s(32);
        int contentBottom = panelY + panelH - s(42);
        int contentHeight = Math.max(s(180), contentBottom - contentTop);
        int totalContentWidth = panelW - innerPadding * 2 - columnGap;

        int historyMin = s(160);
        int detailMin = s(220);
        int preferredHistory = Math.max(historyMin, Math.min(s(310), Math.round(totalContentWidth * 0.32f)));
        historyW = Math.min(preferredHistory, Math.max(historyMin, totalContentWidth - detailMin));
        if (totalContentWidth < historyMin + detailMin) {
            historyW = Math.max(s(126), Math.round(totalContentWidth * 0.31f));
        }
        detailW = Math.max(s(190), totalContentWidth - historyW);
        if (historyW + detailW > totalContentWidth) {
            detailW = totalContentWidth - historyW;
        }

        historyX = panelX + innerPadding;
        historyY = contentTop;
        historyH = contentHeight;
        historyContentY = historyY + s(30);
        historyContentH = Math.max(s(76), historyH - s(40));

        detailX = historyX + historyW + columnGap;
        detailY = historyY;
        detailH = historyH;

        int sectionGap = s(10);
        int infoCollapsedH = s(32);
        int infoExpandedMin = s(94);
        int infoExpandedPref = Math.min(s(148), Math.max(infoExpandedMin, detailH / 3));
        detailInfoH = detailInfoExpanded ? infoExpandedPref : infoCollapsedH;

        int footerMin = s(84);
        int footerPref = Math.min(s(128), Math.max(footerMin, detailH / 4));
        detailFooterH = footerPref;

        int minElementCardH = s(96);
        int overflow = detailInfoH + detailFooterH + sectionGap * 2 + minElementCardH - detailH;
        if (overflow > 0) {
            int footerReducible = Math.max(0, detailFooterH - s(68));
            int footerReduce = Math.min(overflow, footerReducible);
            detailFooterH -= footerReduce;
            overflow -= footerReduce;
        }
        if (overflow > 0 && detailInfoExpanded) {
            int infoReducible = Math.max(0, detailInfoH - s(72));
            int infoReduce = Math.min(overflow, infoReducible);
            detailInfoH -= infoReduce;
            overflow -= infoReduce;
        }
        if (overflow > 0) {
            detailFooterH = Math.max(s(56), detailFooterH - overflow);
        }

        elementCardY = detailY + detailInfoH + sectionGap;
        detailFooterY = detailY + detailH - detailFooterH;
        elementCardH = Math.max(minElementCardH, detailFooterY - sectionGap - elementCardY);
        detailFooterY = elementCardY + elementCardH + sectionGap;
        detailFooterH = Math.max(s(56), detailY + detailH - detailFooterY);

        elementListY = elementCardY + s(30);
        elementListH = Math.max(s(54), elementCardH - s(40));
        detailFooterContentY = detailFooterY + s(30);
        detailFooterContentH = Math.max(s(40), detailFooterH - s(40));
    }

    private List<GuiInspectionManager.CapturedGuiSnapshot> getSnapshots() {
        return GuiInspectionManager.getHistory();
    }

    private GuiInspectionManager.CapturedGuiSnapshot getSelectedSnapshot() {
        List<GuiInspectionManager.CapturedGuiSnapshot> snapshots = getSnapshots();
        if (selectedSnapshotIndex < 0 || selectedSnapshotIndex >= snapshots.size()) {
            return null;
        }
        return snapshots.get(selectedSnapshotIndex);
    }

    private List<GuiElementInspector.GuiElementInfo> getSelectedElements() {
        GuiInspectionManager.CapturedGuiSnapshot snapshot = getSelectedSnapshot();
        return snapshot == null ? Collections.<GuiElementInspector.GuiElementInfo>emptyList() : snapshot.getElements();
    }

    private GuiElementInspector.GuiElementInfo getSelectedElement() {
        List<GuiElementInspector.GuiElementInfo> elements = getSelectedElements();
        if (selectedElementIndex < 0 || selectedElementIndex >= elements.size()) {
            return null;
        }
        return elements.get(selectedElementIndex);
    }

    private void syncSelectionBounds() {
        List<GuiInspectionManager.CapturedGuiSnapshot> snapshots = getSnapshots();
        if (snapshots.isEmpty()) {
            selectedSnapshotIndex = -1;
            selectedElementIndex = -1;
            historyScroll = 0;
            elementScroll = 0;
            clampScrolls();
            return;
        }
        if (selectedSnapshotIndex < 0 || selectedSnapshotIndex >= snapshots.size()) {
            selectedSnapshotIndex = 0;
            selectedElementIndex = -1;
        }

        List<GuiElementInspector.GuiElementInfo> elements = getSelectedElements();
        if (elements.isEmpty()) {
            selectedElementIndex = -1;
            elementScroll = 0;
        } else if (selectedElementIndex < 0 || selectedElementIndex >= elements.size()) {
            selectedElementIndex = 0;
        }
        clampScrolls();
    }

    private void clampScrolls() {
        int historyVisibleRows = Math.max(1, historyContentH / HISTORY_ROW_HEIGHT);
        historyMaxScroll = Math.max(0, getSnapshots().size() - historyVisibleRows);
        historyScroll = Math.max(0, Math.min(historyScroll, historyMaxScroll));

        int elementVisibleRows = Math.max(1, elementListH / ELEMENT_ROW_HEIGHT);
        elementMaxScroll = Math.max(0, getSelectedElements().size() - elementVisibleRows);
        elementScroll = Math.max(0, Math.min(elementScroll, elementMaxScroll));
    }

    private void refreshButtons() {
        GuiButton captureButton = findButton(BTN_TOGGLE_CAPTURE);
        if (captureButton != null) {
            captureButton.displayString = GuiInspectionManager.isCaptureEnabled() ? "停止捕获" : "开启捕获";
        }

        GuiButton copyButton = findButton(BTN_COPY_PATH);
        if (copyButton != null) {
            copyButton.enabled = getSelectedElement() != null;
        }

        if (infoToggleButton != null) {
            infoToggleButton.displayString = detailInfoExpanded ? "▲" : "▼";
            infoToggleButton.enabled = getSelectedSnapshot() != null;
            infoToggleButton.visible = true;
        }
    }

    private GuiButton findButton(int id) {
        for (GuiButton button : this.buttonList) {
            if (button != null && button.id == id) {
                return button;
            }
        }
        return null;
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        switch (button.id) {
            case BTN_TOGGLE_CAPTURE:
                GuiInspectionManager.toggleCaptureEnabled();
                refreshButtons();
                break;
            case BTN_CLEAR:
                GuiInspectionManager.clearHistory();
                syncSelectionBounds();
                refreshButtons();
                break;
            case BTN_COPY_PATH:
                GuiElementInspector.GuiElementInfo element = getSelectedElement();
                if (element != null) {
                    setClipboardString(element.getPath());
                    if (mc.player != null) {
                        mc.player.sendMessage(
                                new TextComponentString(TextFormatting.GREEN + "已复制路径: " + element.getPath()));
                    }
                }
                break;
            case BTN_BACK:
                mc.displayGuiScreen(parentScreen);
                break;
            case BTN_TOGGLE_INFO:
                detailInfoExpanded = !detailInfoExpanded;
                initGui();
                break;
            default:
                break;
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        recalcLayout();
        syncSelectionBounds();
        refreshButtons();

        GuiTheme.drawPanel(panelX, panelY, panelW, panelH);
        GuiTheme.drawTitleBar(panelX, panelY, panelW, "GUI识别管理器", this.fontRenderer);

        String captureState = GuiInspectionManager.isCaptureEnabled() ? "§a捕获中" : "§7已暂停";
        String captureHint = GuiInspectionManager.isCaptureEnabled() ? "打开目标界面即可自动记录" : "开启捕获后会自动记录界面快照";
        int stateWidth = this.fontRenderer.getStringWidth(captureState);
        drawString(this.fontRenderer, captureState, panelX + panelW - stateWidth - s(12), panelY + s(8), 0xFFFFFFFF);
        drawString(this.fontRenderer, captureHint, panelX + s(12), panelY + s(20), 0xFF9FB8CC);

        drawHistoryPanel(mouseX, mouseY);
        drawSnapshotInfoPanel(mouseX, mouseY);
        drawElementPanel(mouseX, mouseY);
        drawSelectedElementPanel(mouseX, mouseY);

        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    private void drawHistoryPanel(int mouseX, int mouseY) {
        drawCard(historyX, historyY, historyW, historyH, "捕获历史",
                GuiInspectionManager.isCaptureEnabled() ? "正在监听新的 GUI 变化" : "当前已停止捕获");

        List<GuiInspectionManager.CapturedGuiSnapshot> snapshots = getSnapshots();
        int visibleRows = Math.max(1, historyContentH / HISTORY_ROW_HEIGHT);
        clampScrolls();

        if (snapshots.isEmpty()) {
            GuiTheme.drawEmptyState(historyX + historyW / 2, historyY + historyH / 2 - s(8), "暂无记录", this.fontRenderer);
            drawCenteredString(fontRenderer, "先开启捕获，再打开任意容器或 GUI", historyX + historyW / 2,
                    historyY + historyH / 2 + s(8), 0xFF7F8FA4);
            return;
        }

        int rowWidth = historyW - s(14);
        for (int i = 0; i < visibleRows; i++) {
            int index = i + historyScroll;
            if (index >= snapshots.size()) {
                break;
            }
            GuiInspectionManager.CapturedGuiSnapshot snapshot = snapshots.get(index);
            int rowY = historyContentY + i * HISTORY_ROW_HEIGHT;
            boolean hovered = isInside(mouseX, mouseY, historyX + s(4), rowY, rowWidth, HISTORY_ROW_HEIGHT - 2);
            boolean selected = index == selectedSnapshotIndex;
            GuiTheme.drawButtonFrameSafe(historyX + s(4), rowY, rowWidth, HISTORY_ROW_HEIGHT - 2,
                    selected ? GuiTheme.UiState.SELECTED
                            : (hovered ? GuiTheme.UiState.HOVER : GuiTheme.UiState.NORMAL));

            String title = snapshot.getTitle().trim().isEmpty() ? "(无标题)" : snapshot.getTitle().trim();
            String line1 = snapshot.getScreenSimpleName() + "  ·  " + snapshot.getTimestampText();
            String line2 = title + "  ·  元素 " + snapshot.getElements().size() + "  ·  wnd " + snapshot.getWindowId();

            drawString(fontRenderer, fontRenderer.trimStringToWidth(line1, rowWidth - s(16)), historyX + s(10), rowY + s(6),
                    0xFFFFFFFF);
            drawString(fontRenderer, fontRenderer.trimStringToWidth(line2, rowWidth - s(16)), historyX + s(10),
                    rowY + s(19), 0xFF9FB0C0);
        }

        drawScrollbarIfNeeded(historyX + historyW - s(8), historyContentY, historyContentH, historyScroll, historyMaxScroll,
                snapshots.size(), visibleRows, HISTORY_ROW_HEIGHT);
    }

    private void drawSnapshotInfoPanel(int mouseX, int mouseY) {
        GuiInspectionManager.CapturedGuiSnapshot snapshot = getSelectedSnapshot();
        String subtitle;
        if (snapshot == null) {
            subtitle = "未选择任何快照";
        } else if (detailInfoExpanded) {
            subtitle = snapshot.getScreenSimpleName() + " · " + snapshot.getTimestampText();
        } else {
            subtitle = buildCollapsedSnapshotSummary(snapshot);
        }
        drawCard(detailX, detailY, detailW, detailInfoH, "当前快照", subtitle);

        if (snapshot == null || !detailInfoExpanded) {
            return;
        }

        List<String> lines = new ArrayList<>();
        lines.add("§b屏幕: §f" + safe(snapshot.getScreenSimpleName()));
        lines.add("§7类名: §f" + safe(snapshot.getScreenClassName()));
        lines.add("§7标题: §f" + safe(snapshot.getTitle().trim().isEmpty() ? "(无)" : snapshot.getTitle()));
        lines.add("§7窗口: §fwindowId=" + snapshot.getWindowId() + "  total=" + snapshot.getTotalSlots()
                + "  container=" + snapshot.getContainerSlots() + "  player=" + snapshot.getPlayerInventorySlots());
        lines.add("§7元素总数: §f" + snapshot.getElements().size());
        drawWrappedTextLines(lines, detailX + s(8), detailY + s(30), detailW - s(40), detailInfoH - s(38), 0xFFFFFFFF);
    }

    private void drawElementPanel(int mouseX, int mouseY) {
        GuiInspectionManager.CapturedGuiSnapshot snapshot = getSelectedSnapshot();
        int selectedCount = snapshot == null ? 0 : snapshot.getElements().size();
        drawCard(detailX, elementCardY, detailW, elementCardH, "元素列表",
                selectedCount <= 0 ? "当前快照没有可展示元素" : "点击元素可在下方查看路径与文本");

        List<GuiElementInspector.GuiElementInfo> elements = snapshot == null
                ? Collections.<GuiElementInspector.GuiElementInfo>emptyList()
                : snapshot.getElements();
        int visibleRows = Math.max(1, elementListH / ELEMENT_ROW_HEIGHT);
        clampScrolls();

        if (elements.isEmpty()) {
            GuiTheme.drawEmptyState(detailX + detailW / 2, elementCardY + elementCardH / 2 - s(6), "暂无元素",
                    this.fontRenderer);
            drawCenteredString(fontRenderer, "先选择左侧快照，或打开可识别控件的界面", detailX + detailW / 2,
                    elementCardY + elementCardH / 2 + s(10), 0xFF7F8FA4);
            return;
        }

        int rowWidth = detailW - s(14);
        for (int i = 0; i < visibleRows; i++) {
            int index = i + elementScroll;
            if (index >= elements.size()) {
                break;
            }
            GuiElementInspector.GuiElementInfo element = elements.get(index);
            int rowY = elementListY + i * ELEMENT_ROW_HEIGHT;
            boolean hovered = isInside(mouseX, mouseY, detailX + s(4), rowY, rowWidth, ELEMENT_ROW_HEIGHT - 2);
            boolean selected = index == selectedElementIndex;
            GuiTheme.drawButtonFrameSafe(detailX + s(4), rowY, rowWidth, ELEMENT_ROW_HEIGHT - 2,
                    selected ? GuiTheme.UiState.SELECTED
                            : (hovered ? GuiTheme.UiState.HOVER : GuiTheme.UiState.NORMAL));

            String primary = "[" + element.getType().name() + "] " + element.getPath();
            String text = element.getText().trim().isEmpty() ? "(空文本)" : element.getText().trim();
            String secondary = text + "  ·  x=" + element.getX() + ", y=" + element.getY()
                    + "  ·  " + element.getWidth() + "x" + element.getHeight();

            drawString(fontRenderer, fontRenderer.trimStringToWidth(primary, rowWidth - s(16)), detailX + s(10),
                    rowY + s(5), 0xFFFFFFFF);
            drawString(fontRenderer, fontRenderer.trimStringToWidth(secondary, rowWidth - s(16)), detailX + s(10),
                    rowY + s(18), 0xFF9FB0C0);
        }

        drawScrollbarIfNeeded(detailX + detailW - s(8), elementListY, elementListH, elementScroll, elementMaxScroll,
                elements.size(), visibleRows, ELEMENT_ROW_HEIGHT);
    }

    private void drawSelectedElementPanel(int mouseX, int mouseY) {
        GuiElementInspector.GuiElementInfo selectedElement = getSelectedElement();
        drawCard(detailX, detailFooterY, detailW, detailFooterH, "当前选中元素",
                selectedElement == null ? "未选择元素" : "路径与文本已嵌入底部卡片");

        List<String> lines = new ArrayList<>();
        if (selectedElement == null) {
            lines.add("§7点击上方“元素列表”中的任意一项，即可在这里查看完整路径、文本和坐标信息。");
            lines.add("§7如果想把路径回填到别的规则里，可以直接使用底部“复制路径”按钮。");
        } else {
            String text = selectedElement.getText().trim().isEmpty() ? "(空)" : selectedElement.getText().trim();
            lines.add("§b当前路径: §f" + selectedElement.getPath());
            lines.add("§7文本内容: §f" + text);
            lines.add("§7类型: §f" + selectedElement.getType().name()
                    + "  §7坐标: §f(" + selectedElement.getX() + ", " + selectedElement.getY() + ")"
                    + "  §7尺寸: §f" + selectedElement.getWidth() + "x" + selectedElement.getHeight());
            if (selectedElement.getButtonId() != Integer.MIN_VALUE) {
                lines.add("§7按钮ID: §f" + selectedElement.getButtonId());
            }
            if (selectedElement.getSlotIndex() >= 0) {
                lines.add("§7槽位索引: §f" + selectedElement.getSlotIndex());
            }
        }
        drawWrappedTextLines(lines, detailX + s(8), detailFooterContentY, detailW - s(16), detailFooterContentH,
                0xFFFFFFFF);
    }

    private void drawCard(int x, int y, int width, int height, String title, String subtitle) {
        GuiTheme.drawPanelSegment(x, y, width, height, panelX, panelY, panelW, panelH);
        GuiTheme.drawSectionTitle(x + s(8), y + s(8), title, this.fontRenderer);
        if (subtitle != null && !subtitle.trim().isEmpty()) {
            drawString(fontRenderer, fontRenderer.trimStringToWidth(subtitle, width - s(44)), x + s(8), y + s(20),
                    0xFF9FB8CC);
        }
    }

    private String buildCollapsedSnapshotSummary(GuiInspectionManager.CapturedGuiSnapshot snapshot) {
        String title = snapshot.getTitle().trim().isEmpty() ? "(无标题)" : snapshot.getTitle().trim();
        return snapshot.getScreenSimpleName() + " · " + title + " · 元素 " + snapshot.getElements().size();
    }

    private void drawWrappedTextLines(List<String> lines, int x, int y, int maxWidth, int maxHeight, int color) {
        if (lines == null || lines.isEmpty() || maxWidth <= 0 || maxHeight <= 0) {
            return;
        }

        List<String> wrapped = new ArrayList<>();
        for (String line : lines) {
            List<String> split = fontRenderer.listFormattedStringToWidth(safe(line), maxWidth);
            if (split == null || split.isEmpty()) {
                wrapped.add("");
            } else {
                wrapped.addAll(split);
            }
        }

        int lineHeight = fontRenderer.FONT_HEIGHT + 2;
        int maxLines = Math.max(1, maxHeight / lineHeight);
        int drawCount = Math.min(maxLines, wrapped.size());
        for (int i = 0; i < drawCount; i++) {
            String line = wrapped.get(i);
            if (i == drawCount - 1 && wrapped.size() > maxLines) {
                line = fontRenderer.trimStringToWidth(line + " ...", maxWidth);
            }
            drawString(fontRenderer, line, x, y + i * lineHeight, color);
        }
    }

    private void drawScrollbarIfNeeded(int x, int y, int height, int scrollOffset, int maxScroll,
            int totalItems, int visibleItems, int rowHeight) {
        if (maxScroll <= 0 || totalItems <= 0 || height <= 0) {
            return;
        }
        int totalHeight = totalItems * rowHeight;
        int thumbHeight = Math.max(16, (int) ((height / (float) Math.max(height, totalHeight)) * height));
        int track = Math.max(1, height - thumbHeight);
        int thumbY = y + (int) ((scrollOffset / (float) Math.max(1, maxScroll)) * track);
        GuiTheme.drawScrollbar(x, y, 4, height, thumbY, thumbHeight);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        super.mouseClicked(mouseX, mouseY, mouseButton);

        if (mouseButton != 0) {
            return;
        }

        int historyVisibleRows = Math.max(1, historyContentH / HISTORY_ROW_HEIGHT);
        for (int i = 0; i < historyVisibleRows; i++) {
            int index = i + historyScroll;
            if (index >= getSnapshots().size()) {
                break;
            }
            int rowY = historyContentY + i * HISTORY_ROW_HEIGHT;
            if (isInside(mouseX, mouseY, historyX + s(4), rowY, historyW - s(14), HISTORY_ROW_HEIGHT - 2)) {
                selectedSnapshotIndex = index;
                selectedElementIndex = getSelectedElements().isEmpty() ? -1 : 0;
                elementScroll = 0;
                refreshButtons();
                return;
            }
        }

        int elementVisibleRows = Math.max(1, elementListH / ELEMENT_ROW_HEIGHT);
        for (int i = 0; i < elementVisibleRows; i++) {
            int index = i + elementScroll;
            if (index >= getSelectedElements().size()) {
                break;
            }
            int rowY = elementListY + i * ELEMENT_ROW_HEIGHT;
            if (isInside(mouseX, mouseY, detailX + s(4), rowY, detailW - s(14), ELEMENT_ROW_HEIGHT - 2)) {
                selectedElementIndex = index;
                refreshButtons();
                return;
            }
        }
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        int dWheel = Mouse.getEventDWheel();
        if (dWheel == 0) {
            return;
        }

        int mouseX = Mouse.getEventX() * this.width / this.mc.displayWidth;
        int mouseY = this.height - Mouse.getEventY() * this.height / this.mc.displayHeight - 1;

        if (isInside(mouseX, mouseY, historyX, historyContentY, historyW, historyContentH) && historyMaxScroll > 0) {
            historyScroll = dWheel > 0 ? Math.max(0, historyScroll - 1) : Math.min(historyMaxScroll, historyScroll + 1);
            return;
        }
        if (isInside(mouseX, mouseY, detailX, elementListY, detailW, elementListH) && elementMaxScroll > 0) {
            elementScroll = dWheel > 0 ? Math.max(0, elementScroll - 1) : Math.min(elementMaxScroll, elementScroll + 1);
        }
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (keyCode == Keyboard.KEY_ESCAPE) {
            mc.displayGuiScreen(parentScreen);
            return;
        }
        super.keyTyped(typedChar, keyCode);
    }

    private boolean isInside(int mouseX, int mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}
