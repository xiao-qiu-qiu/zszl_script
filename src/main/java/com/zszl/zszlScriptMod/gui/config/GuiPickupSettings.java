// 文件路径: src/main/java/com/keycommand2/zszlScriptMod/gui/config/GuiPickupSettings.java
// (这是一个全新的文件)
package com.zszl.zszlScriptMod.gui.config;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import com.zszl.zszlScriptMod.gui.components.ThemedGuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.text.TextComponentString;

import java.io.IOException;

import com.zszl.zszlScriptMod.gui.components.GuiTheme;
import com.zszl.zszlScriptMod.gui.components.ThemedButton;
import com.zszl.zszlScriptMod.handlers.ArenaItemHandler;

public class GuiPickupSettings extends ThemedGuiScreen {
    private GuiScreen parentScreen;

    // UI组件
    private GuiTextField initialDelayField;
    private GuiTextField itemsPerBatchField;
    private GuiTextField intervalField;

    public GuiPickupSettings(GuiScreen parentScreen) {
        this.parentScreen = parentScreen;
    }

    @Override
    public void initGui() {
        this.buttonList.clear();

        int centerX = width / 2;
        int startY = height / 2 - 80;
        int panelWidth = 300;
        int panelX = centerX - panelWidth / 2;

        // 创建输入框
        initialDelayField = new GuiTextField(1, fontRenderer, centerX + 20, startY + 20, 60, 20);
        initialDelayField.setMaxStringLength(Integer.MAX_VALUE);
        initialDelayField.setText(String.valueOf(ArenaItemHandler.pickupInitialDelay));

        itemsPerBatchField = new GuiTextField(2, fontRenderer, centerX + 20, startY + 50, 60, 20);
        itemsPerBatchField.setMaxStringLength(Integer.MAX_VALUE);
        itemsPerBatchField.setText(String.valueOf(ArenaItemHandler.pickupItemsPerBatch));

        intervalField = new GuiTextField(3, fontRenderer, centerX + 20, startY + 80, 60, 20);
        intervalField.setMaxStringLength(Integer.MAX_VALUE);
        intervalField.setText(String.valueOf(ArenaItemHandler.pickupOperationInterval));

        // 创建按钮
        this.buttonList
                .add(new ThemedButton(100, panelX + 10, startY + 120, (panelWidth - 40) / 3, 20,
                        I18n.format("gui.common.save")));
        this.buttonList.add(
                new ThemedButton(101, panelX + 20 + (panelWidth - 40) / 3, startY + 120, (panelWidth - 40) / 3, 20,
                        I18n.format("gui.common.cancel")));
        this.buttonList.add(new ThemedButton(102, panelX + 30 + 2 * ((panelWidth - 40) / 3), startY + 120,
                (panelWidth - 40) / 3, 20, I18n.format("gui.common.reset_default")));
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        switch (button.id) {
            case 100: // 保存
                saveSettings();
                mc.displayGuiScreen(parentScreen);
                break;
            case 101: // 取消
                mc.displayGuiScreen(parentScreen);
                break;
            case 102: // 恢复默认
                resetToDefaults();
                initGui(); // 刷新界面显示默认值
                break;
        }
    }

    private void saveSettings() {
        try {
            ArenaItemHandler.pickupInitialDelay = Math.max(0, Integer.parseInt(initialDelayField.getText()));
            ArenaItemHandler.pickupItemsPerBatch = Math.max(1, Integer.parseInt(itemsPerBatchField.getText()));
            ArenaItemHandler.pickupOperationInterval = Math.max(1, Integer.parseInt(intervalField.getText()));

            ArenaItemHandler.saveArenaConfig(); // 调用保存方法

            if (mc.player != null) {
                mc.player.sendMessage(new TextComponentString(I18n.format("msg.pickup.saved")));
            }
        } catch (NumberFormatException e) {
            if (mc.player != null) {
                mc.player.sendMessage(new TextComponentString(I18n.format("msg.pickup.invalid_number")));
            }
        }
    }

    private void resetToDefaults() {
        // 设置为推荐的默认值
        ArenaItemHandler.pickupInitialDelay = 10;
        ArenaItemHandler.pickupItemsPerBatch = 1;
        ArenaItemHandler.pickupOperationInterval = 2;
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        int centerX = width / 2;
        int startY = height / 2 - 80;
        int panelWidth = 300;
        int panelX = centerX - panelWidth / 2;
        int panelHeight = 160;

        GuiTheme.drawPanel(panelX, startY, panelWidth, panelHeight);
        GuiTheme.drawTitleBar(panelX, startY, panelWidth, I18n.format("gui.pickup.title"), this.fontRenderer);

        // 绘制标签
        drawString(fontRenderer, I18n.format("gui.pickup.initial_delay"), panelX + 20, startY + 25,
                GuiTheme.LABEL_TEXT);
        drawString(fontRenderer, I18n.format("gui.pickup.items_per_batch"), panelX + 20, startY + 55,
                GuiTheme.LABEL_TEXT);
        drawString(fontRenderer, I18n.format("gui.pickup.batch_interval"), panelX + 20, startY + 85,
                GuiTheme.LABEL_TEXT);

        // 绘制输入框
        drawThemedTextField(initialDelayField);
        drawThemedTextField(itemsPerBatchField);
        drawThemedTextField(intervalField);

        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    // (需要添加 keyTyped 和 mouseClicked 方法来让输入框工作)
    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        super.keyTyped(typedChar, keyCode);
        initialDelayField.textboxKeyTyped(typedChar, keyCode);
        itemsPerBatchField.textboxKeyTyped(typedChar, keyCode);
        intervalField.textboxKeyTyped(typedChar, keyCode);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        initialDelayField.mouseClicked(mouseX, mouseY, mouseButton);
        itemsPerBatchField.mouseClicked(mouseX, mouseY, mouseButton);
        intervalField.mouseClicked(mouseX, mouseY, mouseButton);
    }
}
