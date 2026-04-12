package com.zszl.zszlScriptMod.gui.mail;

import com.zszl.zszlScriptMod.handlers.MailHelper;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import com.zszl.zszlScriptMod.gui.components.ThemedGuiScreen;
import net.minecraft.client.gui.GuiYesNo;
import net.minecraft.client.resources.I18n;
import org.lwjgl.input.Mouse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import com.zszl.zszlScriptMod.gui.components.GuiTheme;

public class GuiMailIdViewer extends ThemedGuiScreen {

    private static final int PANEL_WIDTH = 360;
    private static final int PANEL_HEIGHT = 260;
    private static final int LIST_TOP_OFFSET = 36;
    private static final int LIST_LEFT_PADDING = 10;
    private static final int LIST_HEIGHT = 180;
    private static final int LIST_BOTTOM_GAP = 12;
    private static final int CARD_HEIGHT = 70;

    private final GuiScreen parentScreen;
    private List<MailHelper.MailInfo> mailList;

    private int scrollOffset = 0;
    private int maxScroll = 0;
    private final Set<Integer> selectedIndices = new HashSet<>();
    private int selectionAnchorIndex = -1;

    private GuiButton deleteButton;
    private GuiButton clearAllButton;

    private static final int BTN_DONE = 0;
    private static final int BTN_DELETE_SELECTED = 1;
    private static final int BTN_CLEAR_ALL = 2;

    private static final int CONFIRM_DELETE_SELECTED = 1001;
    private static final int CONFIRM_CLEAR_ALL = 1002;

    public GuiMailIdViewer(GuiScreen parent) {
        this.parentScreen = parent;
    }

    @Override
    public void initGui() {
        this.buttonList.clear();
        refreshMailList();

        int panelX = (this.width - PANEL_WIDTH) / 2;
        int panelY = (this.height - PANEL_HEIGHT) / 2;
        int buttonY = panelY + PANEL_HEIGHT - 20 - LIST_BOTTOM_GAP;

        deleteButton = new GuiButton(BTN_DELETE_SELECTED, panelX, buttonY, 112, 20,
                I18n.format("gui.mail.viewer.delete_selected"));
        clearAllButton = new GuiButton(BTN_CLEAR_ALL, panelX + 124, buttonY, 112, 20,
                I18n.format("gui.mail.viewer.clear_all"));
        this.buttonList.add(deleteButton);
        this.buttonList.add(clearAllButton);
        this.buttonList
                .add(new GuiButton(BTN_DONE, panelX + 248, buttonY, 112, 20, I18n.format("gui.common.done")));

        updateButtonStates();
    }

    private void refreshMailList() {
        synchronized (MailHelper.INSTANCE.mailInfoList) {
            this.mailList = new ArrayList<>(MailHelper.INSTANCE.mailInfoList);
        }
    }

    private void updateButtonStates() {
        deleteButton.enabled = !selectedIndices.isEmpty();
        clearAllButton.enabled = !mailList.isEmpty();
    }

    private void selectAll() {
        selectedIndices.clear();
        for (int i = 0; i < mailList.size(); i++) {
            selectedIndices.add(i);
        }
        if (!mailList.isEmpty()) {
            selectionAnchorIndex = mailList.size() - 1;
        }
        updateButtonStates();
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        if (button.id == BTN_DONE) { // 完成
            this.mc.displayGuiScreen(this.parentScreen);
        } else if (button.id == BTN_DELETE_SELECTED) { // 删除选中
            if (!selectedIndices.isEmpty()) {
                mc.displayGuiScreen(new GuiYesNo(this, I18n.format("gui.mail.viewer.confirm_delete.title"),
                        I18n.format("gui.mail.viewer.confirm_delete.message", selectedIndices.size()),
                        CONFIRM_DELETE_SELECTED));
            }
        } else if (button.id == BTN_CLEAR_ALL) { // 一键清空
            if (!mailList.isEmpty()) {
                mc.displayGuiScreen(new GuiYesNo(this, I18n.format("gui.mail.viewer.confirm_clear.title"),
                        I18n.format("gui.mail.viewer.confirm_clear.message", mailList.size()), CONFIRM_CLEAR_ALL));
            }
        }
    }

    @Override
    public void confirmClicked(boolean result, int id) {
        if (result) {
            if (id == CONFIRM_DELETE_SELECTED) {
                List<Integer> idsToRemove = new ArrayList<>();
                for (Integer index : selectedIndices) {
                    if (index != null && index >= 0 && index < mailList.size()) {
                        idsToRemove.add(mailList.get(index).mailId);
                    }
                }
                for (Integer mailId : idsToRemove) {
                    MailHelper.INSTANCE.removeMailById(mailId);
                }
            } else if (id == CONFIRM_CLEAR_ALL) {
                MailHelper.INSTANCE.clearAllMails();
            }
        }

        refreshMailList();
        selectedIndices.clear();
        selectionAnchorIndex = -1;
        scrollOffset = Math.min(scrollOffset, Math.max(0, mailList.size() - 1));
        updateButtonStates();
        this.mc.displayGuiScreen(this);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.drawDefaultBackground();

        int panelX = (this.width - PANEL_WIDTH) / 2;
        int panelY = (this.height - PANEL_HEIGHT) / 2;

        GuiTheme.drawPanel(panelX, panelY, PANEL_WIDTH, PANEL_HEIGHT);
        GuiTheme.drawTitleBar(panelX, panelY, PANEL_WIDTH, I18n.format("gui.mail.viewer.title"), this.fontRenderer);
        this.drawCenteredString(this.fontRenderer, I18n.format("gui.mail.viewer.multiselect_hint"), this.width / 2,
                panelY + 20, 0xAAAAAA);

        int listTop = panelY + LIST_TOP_OFFSET;
        int listHeight = LIST_HEIGHT;
        int listBottom = listTop + listHeight;
        int listLeft = panelX + LIST_LEFT_PADDING;
        int listWidth = PANEL_WIDTH - LIST_LEFT_PADDING * 2;

        int visibleItems = Math.max(1, listHeight / CARD_HEIGHT);
        maxScroll = Math.max(0, mailList.size() - visibleItems);
        scrollOffset = Math.max(0, Math.min(maxScroll, scrollOffset));

        if (mailList.isEmpty()) {
            drawCenteredString(fontRenderer, I18n.format("gui.mail.viewer.empty"), this.width / 2,
                    listTop + listHeight / 2 - 4, 0xAAAAAA);
        } else {
            for (int i = 0; i < visibleItems; i++) {
                int index = i + scrollOffset;
                if (index >= mailList.size())
                    break;

                int itemY = listTop + i * CARD_HEIGHT;
                MailHelper.MailInfo info = mailList.get(index);

                String titleText = I18n.format("gui.mail.viewer.card.title", index + 1, info.mailId);
                String nameText = I18n.format("gui.mail.viewer.card.name", formatOrUnknown(info.mailName,
                        I18n.format("gui.mail.viewer.unknown_name")));
                String timeText = I18n.format("gui.mail.viewer.card.time", formatOrUnknown(info.mailTime,
                        I18n.format("gui.mail.viewer.unknown_time")));
                String removeText = info.removeButtonId > 0
                        ? I18n.format("gui.mail.viewer.card.remove_id", info.removeButtonId)
                        : I18n.format("gui.mail.viewer.card.remove_id.none");
                String pendingGoldText = "待支付金币: " + formatPayment(info.pendingGoldCost);
                String pendingCouponText = "待支付点券: " + formatPayment(info.pendingCouponCost);

                int bgColor = selectedIndices.contains(index) ? 0xFF0066AA : 0x0;
                boolean isHovered = mouseX >= listLeft && mouseX <= listLeft + listWidth && mouseY >= itemY
                        && mouseY < itemY + CARD_HEIGHT;
                if (isHovered && !selectedIndices.contains(index)) {
                    bgColor = 0x44FFFFFF;
                }
                drawRect(listLeft, itemY, listLeft + listWidth, itemY + CARD_HEIGHT - 2, bgColor);
                drawRect(listLeft, itemY, listLeft + listWidth, itemY + 1, 0x33FFFFFF);

                drawString(fontRenderer, titleText, listLeft + 6, itemY + 4, 0xFFFFFF);
                drawString(fontRenderer, nameText, listLeft + 6, itemY + 18, 0xFFEEDDAA);
                drawString(fontRenderer, timeText, listLeft + 6, itemY + 30, 0xFFCCCCCC);
                drawString(fontRenderer, removeText, listLeft + 6, itemY + 42, 0xFF99CCFF);
                drawString(fontRenderer, pendingGoldText, listLeft + 6, itemY + 54, 0xFFE6D27F);
                drawString(fontRenderer, pendingCouponText, listLeft + 170, itemY + 54, 0xFF88DDFF);
            }
        }

        if (maxScroll > 0) {
            int scrollbarX = listLeft + listWidth - 6;
            drawRect(scrollbarX, listTop, scrollbarX + 5, listBottom, 0xFF202020);
            int thumbHeight = Math.max(10, (int) ((float) visibleItems / mailList.size() * listHeight));
            int thumbY = listTop + (int) ((float) scrollOffset / maxScroll * (listHeight - thumbHeight));
            drawRect(scrollbarX, thumbY, scrollbarX + 5, thumbY + thumbHeight, 0xFF888888);
        }

        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        super.mouseClicked(mouseX, mouseY, mouseButton);

        int panelX = (this.width - PANEL_WIDTH) / 2;
        int panelY = (this.height - PANEL_HEIGHT) / 2;
        int listTop = panelY + LIST_TOP_OFFSET;
        int listLeft = panelX + LIST_LEFT_PADDING;
        int listWidth = PANEL_WIDTH - LIST_LEFT_PADDING * 2;
        int listHeight = LIST_HEIGHT;

        if (mouseX >= listLeft && mouseX < listLeft + listWidth && mouseY >= listTop && mouseY < listTop + listHeight) {
            int clickedIndex = (mouseY - listTop) / CARD_HEIGHT + scrollOffset;
            if (clickedIndex >= 0 && clickedIndex < mailList.size()) {
                boolean ctrlDown = isCtrlKeyDown();
                boolean shiftDown = isShiftKeyDown();

                if (shiftDown && selectionAnchorIndex >= 0 && selectionAnchorIndex < mailList.size()) {
                    int from = Math.min(selectionAnchorIndex, clickedIndex);
                    int to = Math.max(selectionAnchorIndex, clickedIndex);
                    if (!ctrlDown) {
                        selectedIndices.clear();
                    }
                    for (int i = from; i <= to; i++) {
                        selectedIndices.add(i);
                    }
                } else if (ctrlDown) {
                    if (selectedIndices.contains(clickedIndex)) {
                        selectedIndices.remove(clickedIndex);
                    } else {
                        selectedIndices.add(clickedIndex);
                    }
                    selectionAnchorIndex = clickedIndex;
                } else {
                    selectedIndices.clear();
                    selectedIndices.add(clickedIndex);
                    selectionAnchorIndex = clickedIndex;
                }

                updateButtonStates();
            }
        }
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        int dWheel = Mouse.getEventDWheel();
        if (dWheel != 0 && maxScroll > 0) {
            if (dWheel > 0) {
                scrollOffset = Math.max(0, scrollOffset - 1);
            } else {
                scrollOffset = Math.min(maxScroll, scrollOffset + 1);
            }
        }
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (isCtrlKeyDown() && (typedChar == 1 || keyCode == 30)) { // Ctrl+A
            selectAll();
            return;
        }
        super.keyTyped(typedChar, keyCode);
    }

    private String formatOrUnknown(String value, String unknownValue) {
        if (value == null) {
            return unknownValue;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? unknownValue : trimmed;
    }

    private String formatPayment(int value) {
        return value >= 0 ? String.valueOf(value) : "未获取";
    }
}
