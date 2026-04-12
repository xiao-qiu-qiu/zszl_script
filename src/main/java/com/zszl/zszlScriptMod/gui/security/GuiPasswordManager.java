package com.zszl.zszlScriptMod.gui.security;

import com.zszl.zszlScriptMod.gui.security.PasswordManagerConfig.PasswordEntry;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import com.zszl.zszlScriptMod.gui.components.ThemedGuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.text.TextComponentString;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import com.zszl.zszlScriptMod.gui.components.GuiTheme;

public class GuiPasswordManager extends ThemedGuiScreen {

    private final GuiScreen parent;
    private List<PasswordEntry> entries = new ArrayList<>();
    private int selectedIndex = -1;
    private int scrollOffset = 0;
    private int maxScroll = 0;

    private GuiTextField idField;
    private GuiTextField passwordField;
    private String editingOriginalId = "";

    private static final int BTN_SAVE = 1;
    private static final int BTN_DELETE = 2;
    private static final int BTN_DONE = 3;
    private static final int BTN_NEW = 4;
    private static final int BTN_TOGGLE_AUTO = 5;

    private boolean autoLoginEnabledInEditor = false;

    private int panelX;
    private int panelY;
    private int panelWidth;
    private int panelHeight;
    private int listX;
    private int listY;
    private int listW;
    private int listH;
    private int cardH;
    private int fieldY;
    private int bottomBtnY;
    private int doneBtnY;

    private float uiScale() {
        float sx = this.width / 420.0f;
        float sy = this.height / 300.0f;
        return Math.max(0.72f, Math.min(1.0f, Math.min(sx, sy)));
    }

    private int s(int base) {
        return Math.max(1, Math.round(base * uiScale()));
    }

    private void computeLayout() {
        panelWidth = Math.max(300, Math.min(s(390), this.width - s(20)));
        panelHeight = Math.max(210, Math.min(s(265), this.height - s(20)));
        panelX = (this.width - panelWidth) / 2;
        panelY = (this.height - panelHeight) / 2;

        listX = panelX + s(10);
        listY = panelY + s(30);
        listW = panelWidth - s(20);
        cardH = s(28);

        int reservedBottom = s(88);
        listH = Math.max(s(80), panelHeight - s(40) - reservedBottom);

        fieldY = panelY + panelHeight - s(60);
        bottomBtnY = panelY + panelHeight - s(36);
        doneBtnY = panelY + s(8);
    }

    public GuiPasswordManager(GuiScreen parent) {
        this.parent = parent;
    }

    @Override
    public void initGui() {
        Keyboard.enableRepeatEvents(true);
        PasswordManagerConfig.load();
        this.entries = PasswordManagerConfig.getEntries();
        computeLayout();

        PasswordEntry selected = PasswordManagerConfig.getSelectedEntry();
        if (selected != null) {
            for (int i = 0; i < entries.size(); i++) {
                if (entries.get(i).playerId.equals(selected.playerId)) {
                    selectedIndex = i;
                    break;
                }
            }
        }

        this.buttonList.clear();
        int fieldH = s(18);
        int leftLabelW = s(58);
        int between = s(10);
        int leftX = panelX + s(10);
        int leftFieldX = leftX + leftLabelW;
        int leftFieldW = Math.max(s(90), (panelWidth - s(30) - leftLabelW - s(36) - leftLabelW) / 2);
        int rightLabelX = leftFieldX + leftFieldW + between;
        int rightFieldX = rightLabelX + s(36);
        int rightFieldW = panelX + panelWidth - s(10) - rightFieldX;

        this.idField = new GuiTextField(100, this.fontRenderer, leftFieldX, fieldY, leftFieldW, fieldH);
        this.passwordField = new GuiTextField(101, this.fontRenderer, rightFieldX, fieldY, rightFieldW, fieldH);
        this.idField.setMaxStringLength(Integer.MAX_VALUE);
        this.passwordField.setMaxStringLength(Integer.MAX_VALUE);
        this.idField.setFocused(true);

        if (selectedIndex >= 0 && selectedIndex < entries.size()) {
            PasswordEntry e = entries.get(selectedIndex);
            this.idField.setText(e.playerId == null ? "" : e.playerId);
            this.passwordField.setText(e.password == null ? "" : e.password);
            this.editingOriginalId = e.playerId == null ? "" : e.playerId;
            this.autoLoginEnabledInEditor = e.autoLogin;
        }

        int btnH = s(20);
        int autoBtnW = Math.max(s(120), panelWidth - s(210));
        int rightButtonsStartX = panelX + panelWidth - s(190);
        int smallBtnW = s(60);
        int btnGap = s(5);

        this.buttonList
            .add(new GuiButton(BTN_TOGGLE_AUTO, panelX + s(10), bottomBtnY, autoBtnW, btnH, getAutoToggleButtonText()));
        this.buttonList.add(new GuiButton(BTN_SAVE, rightButtonsStartX, bottomBtnY, smallBtnW, btnH,
                I18n.format("gui.password.save")));
        this.buttonList.add(new GuiButton(BTN_NEW, rightButtonsStartX + smallBtnW + btnGap, bottomBtnY, smallBtnW, btnH,
                I18n.format("gui.password.new")));
        this.buttonList.add(new GuiButton(BTN_DELETE, rightButtonsStartX + (smallBtnW + btnGap) * 2, bottomBtnY, smallBtnW, btnH,
                I18n.format("gui.password.delete")));
        this.buttonList.add(new GuiButton(BTN_DONE, panelX + panelWidth - s(70), doneBtnY, s(60), btnH,
            I18n.format("gui.common.done")));

        updateButtonStates();
    }

    private void updateButtonStates() {
        GuiButton del = this.buttonList.stream().filter(b -> b.id == BTN_DELETE).findFirst().orElse(null);
        boolean hasSelection = selectedIndex >= 0 && selectedIndex < entries.size();
        if (del != null) {
            del.enabled = hasSelection;
        }
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        switch (button.id) {
            case BTN_SAVE:
                saveCurrentEntry();
                break;
            case BTN_NEW:
                clearEditor();
                break;
            case BTN_TOGGLE_AUTO:
                autoLoginEnabledInEditor = !autoLoginEnabledInEditor;
                refreshAutoToggleButtonText();
                break;
            case BTN_DELETE:
                if (selectedIndex >= 0 && selectedIndex < entries.size()) {
                    PasswordManagerConfig.removeEntry(entries.get(selectedIndex).playerId);
                    this.initGui();
                }
                break;
            case BTN_DONE:
                this.mc.displayGuiScreen(parent);
                break;
            default:
                break;
        }
    }

    private void clearEditor() {
        selectedIndex = -1;
        editingOriginalId = "";
        idField.setText("");
        passwordField.setText("");
        autoLoginEnabledInEditor = false;
        idField.setFocused(true);
        refreshAutoToggleButtonText();
        updateButtonStates();
    }

    private void saveCurrentEntry() {
        String id = idField.getText() == null ? "" : idField.getText().trim();
        String password = passwordField.getText() == null ? "" : passwordField.getText();

        if (id.isEmpty()) {
            if (mc != null && mc.player != null) {
                mc.player.sendMessage(new TextComponentString(I18n.format("gui.password.error.empty_player_id")));
            }
            return;
        }

        if (!editingOriginalId.isEmpty() && !editingOriginalId.equals(id)) {
            PasswordManagerConfig.removeEntry(editingOriginalId);
        }
        PasswordManagerConfig.upsertEntry(id, password);
        PasswordManagerConfig.setAutoLogin(id, autoLoginEnabledInEditor);

        this.entries = PasswordManagerConfig.getEntries();
        selectedIndex = findIndexById(id);
        editingOriginalId = id;
        refreshAutoToggleButtonText();
        updateButtonStates();
    }

    private String getAutoToggleButtonText() {
        return autoLoginEnabledInEditor ? I18n.format("gui.password.auto.on") : I18n.format("gui.password.auto.off");
    }

    private void refreshAutoToggleButtonText() {
        for (GuiButton b : this.buttonList) {
            if (b.id == BTN_TOGGLE_AUTO) {
                b.displayString = getAutoToggleButtonText();
                return;
            }
        }
    }

    private int findIndexById(String id) {
        for (int i = 0; i < entries.size(); i++) {
            PasswordEntry e = entries.get(i);
            if (e != null && id.equals(e.playerId)) {
                return i;
            }
        }
        return -1;
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.drawDefaultBackground();

        drawGradientRect(panelX, panelY, panelX + panelWidth, panelY + panelHeight, 0xD01E1E24, 0xD0121218);
        drawRect(panelX + 1, panelY + 1, panelX + panelWidth - 1, panelY + panelHeight - 1, 0xA0181820);
        GuiTheme.drawTitleBar(panelX, panelY, panelWidth, I18n.format("gui.password.title"), this.fontRenderer);

        drawRect(listX, listY, listX + listW, listY + listH, 0x40000000);

        int visible = listH / cardH;
        maxScroll = Math.max(0, entries.size() - visible);
        if (scrollOffset > maxScroll) {
            scrollOffset = maxScroll;
        }

        for (int i = 0; i < visible; i++) {
            int idx = i + scrollOffset;
            if (idx >= entries.size()) {
                break;
            }

            PasswordEntry entry = entries.get(idx);
            int y = listY + i * cardH;
            boolean selected = idx == selectedIndex;
            int bg = selected ? 0xA055AA55 : 0x80505050;
            drawRect(listX + 2, y + 2, listX + listW - 2, y + cardH - 2, bg);

            String idText = "ID: " + entry.playerId;
            String pwdMasked = maskPassword(entry.password);
            String autoText = entry.autoLogin ? I18n.format("gui.password.auto.on")
                    : I18n.format("gui.password.auto.off");
            this.fontRenderer.drawString(idText, listX + 8, y + 7, GuiTheme.resolveTextColor(idText, 0xFFFFFF));
                this.fontRenderer.drawString(I18n.format("gui.password.label.password_masked", pwdMasked), listX + listW / 3,
                    y + 7,
                    GuiTheme.resolveTextColor(I18n.format("gui.password.label.password_masked", pwdMasked), 0xFFEEDD));
                this.fontRenderer.drawString(autoText, listX + (listW * 3) / 4, y + 7,
                    GuiTheme.resolveTextColor(autoText, 0xFFFFFF));
        }

            this.fontRenderer.drawString(I18n.format("gui.password.label.account"), panelX + s(10), fieldY + 2,
                GuiTheme.resolveTextColor(I18n.format("gui.password.label.account"), 0xFFFFFF));
            this.fontRenderer.drawString(I18n.format("gui.password.label.password"), panelX + panelWidth / 2 + s(6), fieldY + 2,
                GuiTheme.resolveTextColor(I18n.format("gui.password.label.password"), 0xFFFFFF));
            this.fontRenderer.drawString(I18n.format("gui.password.tip.auto_twice"), panelX + s(10), fieldY - s(18),
                GuiTheme.resolveTextColor(I18n.format("gui.password.tip.auto_twice"), 0x99CCFF));
        drawThemedTextField(idField);
        drawThemedTextField(passwordField);

        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    private String maskPassword(String password) {
        if (password == null || password.isEmpty()) {
            return I18n.format("gui.password.empty");
        }
        int n = Math.max(3, Math.min(password.length(), 12));
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < n; i++) {
            sb.append('*');
        }
        return sb.toString();
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        idField.mouseClicked(mouseX, mouseY, mouseButton);
        passwordField.mouseClicked(mouseX, mouseY, mouseButton);

        if (mouseX >= listX && mouseX <= listX + listW && mouseY >= listY && mouseY <= listY + listH) {
            int row = (mouseY - listY) / cardH;
            int idx = row + scrollOffset;
            if (idx >= 0 && idx < entries.size()) {
                selectedIndex = idx;
                PasswordManagerConfig.setSelectedPlayerId(entries.get(selectedIndex).playerId);
                PasswordEntry e = entries.get(selectedIndex);
                idField.setText(e.playerId == null ? "" : e.playerId);
                passwordField.setText(e.password == null ? "" : e.password);
                editingOriginalId = e.playerId == null ? "" : e.playerId;
                autoLoginEnabledInEditor = e.autoLogin;
                refreshAutoToggleButtonText();
                updateButtonStates();
            }
        }
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (idField.textboxKeyTyped(typedChar, keyCode) || passwordField.textboxKeyTyped(typedChar, keyCode)) {
            return;
        }

        if (keyCode == Keyboard.KEY_TAB) {
            boolean focusId = idField.isFocused();
            idField.setFocused(!focusId);
            passwordField.setFocused(focusId);
            return;
        }

        if (keyCode == Keyboard.KEY_RETURN || keyCode == Keyboard.KEY_NUMPADENTER) {
            saveCurrentEntry();
            return;
        }

        super.keyTyped(typedChar, keyCode);
    }

    @Override
    public void updateScreen() {
        super.updateScreen();
        idField.updateCursorCounter();
        passwordField.updateCursorCounter();
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        int dWheel = Mouse.getEventDWheel();
        if (dWheel != 0) {
            if (dWheel < 0) {
                scrollOffset = Math.min(maxScroll, scrollOffset + 1);
            } else {
                scrollOffset = Math.max(0, scrollOffset - 1);
            }
        }
    }

    @Override
    public void onGuiClosed() {
        super.onGuiClosed();
        Keyboard.enableRepeatEvents(false);
    }
}
