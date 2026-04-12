package com.zszl.zszlScriptMod.gui.path;

import com.zszl.zszlScriptMod.gui.components.ThemedGuiScreen;
import com.zszl.zszlScriptMod.system.KeybindManager;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.resources.I18n;
import org.lwjgl.input.Keyboard;

import java.io.IOException;

public class GuiNodeEditorHotkeyManager extends ThemedGuiScreen {

    private final GuiScreen parentScreen;

    public GuiNodeEditorHotkeyManager(GuiScreen parentScreen) {
        this.parentScreen = parentScreen;
    }

    @Override
    public void initGui() {
        this.buttonList.clear();
        int panelWidth = 420;
        int panelX = (this.width - panelWidth) / 2;
        int panelY = 40;
        int rowHeight = 24;
        int startY = panelY + 26;

        NodeEditorHotkeyManager.load();

        NodeEditorHotkeyManager.Action[] actions = NodeEditorHotkeyManager.Action.values();
        for (int i = 0; i < actions.length; i++) {
            int y = startY + i * rowHeight;
            this.buttonList.add(new GuiButton(i, panelX + panelWidth - 90, y, 80, 20,
                    I18n.format("gui.debug_keybind.change")));
        }

        int footerY = startY + actions.length * rowHeight + 8;
        this.buttonList.add(new GuiButton(1000, panelX, footerY, (panelWidth - 10) / 2, 20,
                I18n.format("gui.debug_keybind.save_close")));
        this.buttonList.add(new GuiButton(1001, panelX + (panelWidth + 10) / 2, footerY, (panelWidth - 10) / 2, 20,
                I18n.format("gui.debug_keybind.back_no_save")));
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        NodeEditorHotkeyManager.Action[] actions = NodeEditorHotkeyManager.Action.values();
        if (button.id >= 0 && button.id < actions.length) {
            final NodeEditorHotkeyManager.Action action = actions[button.id];
            mc.displayGuiScreen(new GuiKeybindRecorder(this, NodeEditorHotkeyManager.get(action), newKeybind -> {
                if (newKeybind != null && newKeybind.getKeyCode() != Keyboard.KEY_NONE) {
                    NodeEditorHotkeyManager.set(action, newKeybind);
                } else {
                    NodeEditorHotkeyManager.set(action, new KeybindManager.Keybind());
                }
                mc.displayGuiScreen(this);
            }));
            return;
        }
        if (button.id == 1000) {
            NodeEditorHotkeyManager.save();
            mc.displayGuiScreen(parentScreen);
            return;
        }
        if (button.id == 1001) {
            NodeEditorHotkeyManager.load();
            mc.displayGuiScreen(parentScreen);
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        int panelWidth = 420;
        int panelX = (this.width - panelWidth) / 2;
        int panelY = 40;
        int rowHeight = 24;
        int titleHeight = 24;
        int startY = panelY + 26;
        int panelHeight = 26 + NodeEditorHotkeyManager.Action.values().length * rowHeight + 36;

        drawRect(panelX, panelY, panelX + panelWidth, panelY + panelHeight, 0x88000000);
        drawRect(panelX, panelY, panelX + panelWidth, panelY + titleHeight, 0xAA000000);
        drawCenteredString(this.fontRenderer, "节点编辑器快捷键管理", this.width / 2, panelY + 8, 0xFFFFFFFF);

        NodeEditorHotkeyManager.Action[] actions = NodeEditorHotkeyManager.Action.values();
        for (int i = 0; i < actions.length; i++) {
            NodeEditorHotkeyManager.Action action = actions[i];
            int y = startY + i * rowHeight;
            drawString(this.fontRenderer, action.getDisplayName(), panelX + 10, y + 6, 0xFFFFFFFF);

            KeybindManager.Keybind keybind = NodeEditorHotkeyManager.get(action);
            String keyName = (keybind == null || keybind.getKeyCode() == Keyboard.KEY_NONE)
                    ? I18n.format("gui.debug_keybind.unbound")
                    : "§e" + keybind.toString();
            drawString(this.fontRenderer, keyName, panelX + 170, y + 6, 0xFFFFFFFF);
        }

        super.drawScreen(mouseX, mouseY, partialTicks);
    }
}