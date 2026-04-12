package com.zszl.zszlScriptMod.gui.path;

import net.minecraft.client.gui.GuiButton;
import com.zszl.zszlScriptMod.gui.components.ThemedGuiScreen;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import com.zszl.zszlScriptMod.shadowbaritone.utils.GuiPathingPolicy;

import java.io.IOException;

import com.zszl.zszlScriptMod.path.PathRecordingManager;
import com.zszl.zszlScriptMod.gui.components.GuiTheme;

/**
 * 录制过程中，用于询问用户下一步操作的GUI
 */
public class GuiNextStepPrompt extends ThemedGuiScreen {

    @Override
    public void initGui() {
        super.initGui();
        this.buttonList.clear();
        int panelWidth = 200;
        int panelHeight = 100;
        int panelX = (this.width - panelWidth) / 2;
        int panelY = (this.height - panelHeight) / 2;

        this.buttonList.add(new GuiButton(0, panelX + 10, panelY + 60, (panelWidth - 30) / 2, 20,
                I18n.format("gui.path.record_next")));
        this.buttonList.add(new GuiButton(1, panelX + 20 + (panelWidth - 30) / 2, panelY + 60, (panelWidth - 30) / 2,
                20, I18n.format("gui.path.finish_record")));
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        if (button.id == 0) { // 记录下一个
            this.mc.displayGuiScreen(null);
            if (this.mc.player != null) {
                this.mc.player.sendMessage(new TextComponentString(
                        TextFormatting.YELLOW + I18n.format("msg.path.right_click_next_chest")));
            }
        } else if (button.id == 1) { // 完成录制
            PathRecordingManager.finishRecording();
            this.mc.displayGuiScreen(new GuiCustomPathCreator()); // 返回到主创建界面
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.drawDefaultBackground();
        int panelWidth = 200;
        int panelHeight = 100;
        int panelX = (this.width - panelWidth) / 2;
        int panelY = (this.height - panelHeight) / 2;

        GuiTheme.drawPanel(panelX, panelY, panelWidth, panelHeight);
        this.drawCenteredString(this.fontRenderer, I18n.format("gui.path.chest_recorded"), this.width / 2, panelY + 15,
                0xFFFFFFFF);
        this.drawCenteredString(this.fontRenderer, I18n.format("gui.path.record_continue_or_finish"), this.width / 2,
                panelY + 35, 0xFFDDDDDD);
        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    @Override
    public boolean doesGuiPauseGame() {
        return !GuiPathingPolicy.shouldKeepPathingDuringGui(this.mc);
    }
}

