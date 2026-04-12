package com.zszl.zszlScriptMod.gui;

import java.io.IOException;

import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import net.minecraft.client.gui.GuiScreen;

public class GuiInventoryOverlayScreen extends GuiScreen {

    @Override
    public void initGui() {
        super.initGui();
        com.zszl.zszlScriptMod.zszlScriptMod.isGuiVisible = true;
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        if (GuiInventory.isAnyDragActive() && Mouse.isButtonDown(0)) {
            GuiInventory.handleMouseDrag(mouseX, mouseY);
        } else if (GuiInventory.isAnyDragActive()) {
            GuiInventory.handleMouseReleaseScaled(mouseX, mouseY, 0);
        }

        GuiInventory.drawOverlay(this.width, this.height);
        if (GuiInventory.isMasterStatusHudEditMode()) {
            OverlayGuiHandler.renderMasterStatusHudPreview();
        }
    }

    @Override
    public void handleMouseInput() throws IOException {
        int dWheel = Mouse.getEventDWheel();
        if (dWheel != 0) {
            GuiInventory.handleMouseWheel(dWheel, Mouse.getEventX(), Mouse.getEventY());
        }

        int button = Mouse.getEventButton();
        if (button != -1) {
            if (Mouse.getEventButtonState()) {
                GuiInventory.handleMouseClick(Mouse.getEventX(), Mouse.getEventY(), button);
            } else {
                GuiInventory.handleMouseRelease(Mouse.getEventX(), Mouse.getEventY(), button);
            }
        }
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (GuiInventory.handleKeyTyped(typedChar, keyCode)) {
            return;
        }
        if (keyCode == Keyboard.KEY_ESCAPE
                || keyCode == com.zszl.zszlScriptMod.zszlScriptMod.getGuiToggleKeyCode()) {
            com.zszl.zszlScriptMod.zszlScriptMod.isGuiVisible = false;
            this.mc.displayGuiScreen(null);
            return;
        }
        super.keyTyped(typedChar, keyCode);
    }

    @Override
    public void onGuiClosed() {
        GuiInventory.handleMouseReleaseScaled(0, 0, 0);
        com.zszl.zszlScriptMod.zszlScriptMod.isGuiVisible = false;
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }
}
