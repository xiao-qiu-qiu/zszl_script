// 文件路径: src/main/java/com/keycommand2/zszlScriptMod/gui/config/GuiQuickExchangeConfig.java
// (这是优化了UI/UX的最终版本)
package com.zszl.zszlScriptMod.gui.config;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import com.zszl.zszlScriptMod.gui.components.ThemedGuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.text.TextComponentString;

import java.io.IOException;
import java.util.Arrays;

import com.zszl.zszlScriptMod.gui.components.ToggleGuiButton;
import com.zszl.zszlScriptMod.gui.components.GuiTheme;
import com.zszl.zszlScriptMod.gui.components.ThemedButton;
import com.zszl.zszlScriptMod.handlers.QuickExchangeHandler;

public class GuiQuickExchangeConfig extends ThemedGuiScreen {
    private GuiScreen parentScreen;
    private GuiTextField clickIntervalField;
    private ToggleGuiButton shiftClickButton;
    private ToggleGuiButton ctrlClickButton;

    public GuiQuickExchangeConfig(GuiScreen parent) {
        this.parentScreen = parent;
    }

    @Override
    public void initGui() {
        this.buttonList.clear();
        int panelWidth = 300;
        int panelX = (this.width - panelWidth) / 2;
        int panelY = (this.height - 180) / 2;
        int currentY = panelY + 30;

        shiftClickButton = new ToggleGuiButton(0, panelX + 10, currentY, panelWidth - 20, 20,
                I18n.format("gui.quick_exchange.shift_click"),
                QuickExchangeHandler.settings.shiftClickEnabled);
        this.buttonList.add(shiftClickButton);
        currentY += 25;

        ctrlClickButton = new ToggleGuiButton(1, panelX + 10, currentY, panelWidth - 20, 20,
                I18n.format("gui.quick_exchange.ctrl_click"),
                QuickExchangeHandler.settings.ctrlClickEnabled);
        this.buttonList.add(ctrlClickButton);
        currentY += 40;

        clickIntervalField = new GuiTextField(2, this.fontRenderer, panelX + 150, currentY, 100, 20);
        clickIntervalField.setText(String.valueOf(QuickExchangeHandler.settings.clickIntervalMs));
        // !! 核心修改：移除验证器，允许用户清空输入框 !!
        // clickIntervalField.setValidator(s -> s.matches("\\d*") && (s.isEmpty() ||
        // Integer.parseInt(s) >= 10));
        currentY += 35;

        this.buttonList.add(new ThemedButton(100, panelX + 10, currentY, (panelWidth - 30) / 2, 20,
                "§a" + I18n.format("gui.common.save")));
        this.buttonList.add(
                new ThemedButton(101, panelX + 20 + (panelWidth - 30) / 2, currentY, (panelWidth - 30) / 2, 20,
                        I18n.format("gui.common.cancel")));
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        if (button instanceof ToggleGuiButton) {
            ((ToggleGuiButton) button).setEnabledState(!((ToggleGuiButton) button).getEnabledState());
            if (button.id == 0)
                QuickExchangeHandler.settings.shiftClickEnabled = ((ToggleGuiButton) button).getEnabledState();
            if (button.id == 1)
                QuickExchangeHandler.settings.ctrlClickEnabled = ((ToggleGuiButton) button).getEnabledState();
        } else if (button.id == 100) { // 保存
            try {
                // !! 核心修改：保存时进行验证，确保最小值是100 !!
                int userInput = 100; // 默认值
                if (!clickIntervalField.getText().trim().isEmpty()) {
                    userInput = Integer.parseInt(clickIntervalField.getText());
                }
                QuickExchangeHandler.settings.clickIntervalMs = Math.max(100, userInput); // 取用户输入和100中的最大值

                QuickExchangeHandler.saveConfig();
                if (mc.player != null)
                    mc.player.sendMessage(new TextComponentString(I18n.format("msg.quick_exchange.saved")));
                mc.displayGuiScreen(parentScreen);
            } catch (NumberFormatException e) {
                if (mc.player != null)
                    mc.player.sendMessage(new TextComponentString(I18n.format("msg.quick_exchange.invalid_number")));
            }
        } else if (button.id == 101) { // 取消
            QuickExchangeHandler.loadConfig(); // 重新加载以放弃更改
            mc.displayGuiScreen(parentScreen);
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.drawDefaultBackground();
        int panelWidth = 300;
        int panelHeight = 180;
        int panelX = (this.width - panelWidth) / 2;
        int panelY = (this.height - panelHeight) / 2;

        GuiTheme.drawPanel(panelX, panelY, panelWidth, panelHeight);
        GuiTheme.drawTitleBar(panelX, panelY, panelWidth, I18n.format("gui.quick_exchange.title"), this.fontRenderer);

        this.drawString(this.fontRenderer, I18n.format("gui.quick_exchange.click_interval"), panelX + 20,
                clickIntervalField.y + 6, GuiTheme.LABEL_TEXT);
        drawThemedTextField(clickIntervalField);

        super.drawScreen(mouseX, mouseY, partialTicks);

        if (mouseX >= clickIntervalField.x && mouseX < clickIntervalField.x + clickIntervalField.width &&
                mouseY >= clickIntervalField.y && mouseY < clickIntervalField.y + clickIntervalField.height) {
            drawHoveringText(Arrays.asList(
                    I18n.format("gui.quick_exchange.tip.title"),
                    I18n.format("gui.quick_exchange.tip.line1"),
                    I18n.format("gui.quick_exchange.tip.line2"),
                    I18n.format("gui.quick_exchange.tip.line3")), mouseX, mouseY);
        }
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        super.keyTyped(typedChar, keyCode);
        clickIntervalField.textboxKeyTyped(typedChar, keyCode);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        clickIntervalField.mouseClicked(mouseX, mouseY, mouseButton);
    }
}
