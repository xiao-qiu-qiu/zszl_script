// 文件路径: src/main/java/com/zszl/zszlScriptMod/gui/dungeon/GuiEditWarehouse.java
package com.zszl.zszlScriptMod.gui.dungeon;

import com.zszl.zszlScriptMod.handlers.WarehouseManager;
import com.zszl.zszlScriptMod.system.dungeon.Warehouse;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import com.zszl.zszlScriptMod.gui.components.ThemedGuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.resources.I18n;
import org.lwjgl.input.Keyboard;

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class GuiEditWarehouse extends ThemedGuiScreen {
    private final GuiScreen parentScreen;
    private final Warehouse warehouse;
    private final Consumer<Warehouse> onSave;
    private final boolean isNew;

    private GuiTextField nameField, x1Field, z1Field, x2Field, z2Field;
    private List<GuiTextField> allTextFields = new ArrayList<>();
    private static final DecimalFormat COORD_FORMAT = new DecimalFormat("#.##");

    public GuiEditWarehouse(GuiScreen parent, Warehouse warehouse, Consumer<Warehouse> onSaveCallback) {
        this.parentScreen = parent;
        this.isNew = (warehouse == null);
        this.warehouse = isNew ? new Warehouse() : warehouse;
        this.onSave = onSaveCallback;
    }

    @Override
    public void initGui() {
        Keyboard.enableRepeatEvents(true);
        this.buttonList.clear();
        this.allTextFields.clear();

        int centerX = this.width / 2;
        int panelWidth = 280;
        int panelY = this.height / 2 - 130; // 增加一点高度
        int currentY = panelY + 20;

        nameField = new GuiTextField(0, this.fontRenderer, centerX - 130, currentY, 260, 20);
        nameField.setText(isNew ? I18n.format("gui.warehouse.edit.new_name") : warehouse.name);
        allTextFields.add(nameField);
        currentY += 35;

        int fieldWidth = 125;
        x1Field = new GuiTextField(1, this.fontRenderer, centerX - 130, currentY, fieldWidth, 20);
        z1Field = new GuiTextField(2, this.fontRenderer, centerX + 5, currentY, fieldWidth, 20);
        allTextFields.add(x1Field);
        allTextFields.add(z1Field);
        currentY += 25;
        this.buttonList
                .add(new GuiButton(10, centerX - 130, currentY, 260, 20, I18n.format("gui.warehouse.edit.get_point1")));
        currentY += 35;

        x2Field = new GuiTextField(3, this.fontRenderer, centerX - 130, currentY, fieldWidth, 20);
        z2Field = new GuiTextField(4, this.fontRenderer, centerX + 5, currentY, fieldWidth, 20);
        allTextFields.add(x2Field);
        allTextFields.add(z2Field);
        currentY += 25;
        this.buttonList
                .add(new GuiButton(11, centerX - 130, currentY, 260, 20, I18n.format("gui.warehouse.edit.get_point2")));
        currentY += 30;

        // --- 核心新增：扫描按钮 ---
        this.buttonList.add(
                new GuiButton(14, centerX - 130, currentY, 260, 20, I18n.format("gui.warehouse.edit.scan_chests")));
        currentY += 30;
        // --- 新增结束 ---

        this.buttonList
                .add(new GuiButton(12, centerX - 130, currentY, 125, 20, I18n.format("gui.common.save_and_close")));
        this.buttonList.add(new GuiButton(13, centerX + 5, currentY, 125, 20, I18n.format("gui.common.cancel")));

        if (!isNew) {
            if (warehouse.x1 != 0.0)
                x1Field.setText(COORD_FORMAT.format(warehouse.x1));
            if (warehouse.z1 != 0.0)
                z1Field.setText(COORD_FORMAT.format(warehouse.z1));
            if (warehouse.x2 != 0.0)
                x2Field.setText(COORD_FORMAT.format(warehouse.x2));
            if (warehouse.z2 != 0.0)
                z2Field.setText(COORD_FORMAT.format(warehouse.z2));
        }
    }

    // 辅助方法，用于保存当前输入框中的数据到 warehouse 对象
    private boolean saveFieldsToObject() {
        try {
            warehouse.name = nameField.getText();
            warehouse.x1 = x1Field.getText().isEmpty() ? 0.0 : Double.parseDouble(x1Field.getText().replace(',', '.'));
            warehouse.z1 = z1Field.getText().isEmpty() ? 0.0 : Double.parseDouble(z1Field.getText().replace(',', '.'));
            warehouse.x2 = x2Field.getText().isEmpty() ? 0.0 : Double.parseDouble(x2Field.getText().replace(',', '.'));
            warehouse.z2 = z2Field.getText().isEmpty() ? 0.0 : Double.parseDouble(z2Field.getText().replace(',', '.'));
            warehouse.updateBounds();
            return true;
        } catch (NumberFormatException e) {
            System.err.println("Invalid coordinate format: " + e.getMessage());
            return false;
        }
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        switch (button.id) {
            case 10: // 获取点1
                if (mc.player != null) {
                    x1Field.setText(COORD_FORMAT.format(mc.player.posX));
                    z1Field.setText(COORD_FORMAT.format(mc.player.posZ));
                }
                break;
            case 11: // 获取点2
                if (mc.player != null) {
                    x2Field.setText(COORD_FORMAT.format(mc.player.posX));
                    z2Field.setText(COORD_FORMAT.format(mc.player.posZ));
                }
                break;
            case 12: // 保存并关闭
                if (saveFieldsToObject()) {
                    onSave.accept(warehouse);
                }
                break;
            case 13: // 取消
                mc.displayGuiScreen(parentScreen);
                break;
            // --- 核心新增：处理扫描按钮点击 ---
            case 14: // 扫描箱子
                if (saveFieldsToObject()) {
                    // 先保存当前坐标，然后执行扫描
                    WarehouseManager.scanForChestsInWarehouse(warehouse);
                    // 扫描后直接返回父界面，因为数据已经更新
                    onSave.accept(warehouse);
                }
                break;
            // --- 新增结束 ---
        }
    }

    // ... (drawScreen, onGuiClosed, keyTyped, mouseClicked, updateScreen 方法保持不变)
    // ...
    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        int centerX = this.width / 2;
        int panelY = this.height / 2 - 130;

        drawCenteredString(fontRenderer,
                I18n.format(isNew ? "gui.warehouse.edit.title_new" : "gui.warehouse.edit.title_edit"), centerX, panelY,
                0xFFFFFF);
        drawString(fontRenderer, I18n.format("gui.warehouse.edit.name"), nameField.x, nameField.y - 12, 0xA0A0A0);
        drawString(fontRenderer, I18n.format("gui.warehouse.edit.point1"), x1Field.x, x1Field.y - 12, 0xA0A0A0);
        drawString(fontRenderer, I18n.format("gui.warehouse.edit.point2"), x2Field.x, x2Field.y - 12, 0xA0A0A0);

        for (GuiTextField field : allTextFields) {
            drawThemedTextField(field);
        }
        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    @Override
    public void onGuiClosed() {
        Keyboard.enableRepeatEvents(false);
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        super.keyTyped(typedChar, keyCode);
        for (GuiTextField field : allTextFields) {
            field.textboxKeyTyped(typedChar, keyCode);
        }
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        for (GuiTextField field : allTextFields) {
            field.mouseClicked(mouseX, mouseY, mouseButton);
        }
    }

    @Override
    public void updateScreen() {
        super.updateScreen();
        for (GuiTextField field : allTextFields) {
            field.updateCursorCounter();
        }
    }
}
