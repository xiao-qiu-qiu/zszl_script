// 文件路径: src/main/java/com/keycommand2/zszlScriptMod/gui/nbt/GuiNBTEditor.java
// (这是最终的、修复了长文本显示不全问题的版本)
package com.zszl.zszlScriptMod.gui.nbt;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import com.zszl.zszlScriptMod.gui.components.ThemedGuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.resources.I18n;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.*;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import com.zszl.zszlScriptMod.gui.components.GuiTextInput;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import com.zszl.zszlScriptMod.gui.components.GuiTheme;

public class GuiNBTEditor extends ThemedGuiScreen {

    private final GuiScreen parentScreen;
    private final ItemStack itemStack;
    private NBTTagCompound nbt;

    private List<NBTEntry> nbtEntries = new ArrayList<>();
    private List<GuiTextField> keyFields = new ArrayList<>();
    private List<GuiTextField> valueFields = new ArrayList<>();
    private List<GuiButton> editButtons = new ArrayList<>();

    private GuiTextField itemNameField, itemCountField;
    private int scrollOffset = 0;
    private int maxScroll = 0;

    private static class NBTEntry {
        String key;
        String value;

        NBTEntry(String key, NBTBase tag) {
            this.key = key;
            this.value = tagToString(tag); // 使用我们新的、无截断的方法
        }
    }

    public GuiNBTEditor(GuiScreen parent, ItemStack stack) {
        this.parentScreen = parent;
        this.itemStack = stack;
        this.nbt = stack.hasTagCompound() ? stack.getTagCompound().copy() : new NBTTagCompound();
        parseNbtToList();
    }

    private void parseNbtToList() {
        nbtEntries.clear();
        for (String key : nbt.getKeySet()) {
            nbtEntries.add(new NBTEntry(key, nbt.getTag(key)));
        }
    }

    @Override
    public void initGui() {
        Keyboard.enableRepeatEvents(true);
        this.buttonList.clear();
        this.keyFields.clear();
        this.valueFields.clear();
        this.editButtons.clear();

        int panelWidth = 450;
        int panelX = (this.width - panelWidth) / 2;
        int panelY = 30;
        int panelHeight = this.height - 60;

        itemNameField = new GuiTextField(0, fontRenderer, panelX + 70, panelY + 25, 180, 20);
        itemNameField.setText(itemStack.getDisplayName());
        itemCountField = new GuiTextField(1, fontRenderer, panelX + panelWidth - 80, panelY + 25, 60, 20);
        itemCountField.setText(String.valueOf(itemStack.getCount()));

        this.buttonList.add(new GuiButton(100, panelX + 10, panelY + panelHeight - 25, 120, 20,
                I18n.format("gui.common.save_and_close")));
        this.buttonList.add(new GuiButton(101, panelX + 140, panelY + panelHeight - 25, 120, 20,
                I18n.format("gui.common.cancel")));
        this.buttonList.add(new GuiButton(102, panelX + panelWidth - 90, panelY + panelHeight - 25, 80, 20,
                I18n.format("gui.nbt.editor.add_tag")));

        int listY = panelY + 60;
        int listHeight = panelHeight - 95;
        int itemHeight = 25;
        int visibleRows = listHeight / itemHeight;
        maxScroll = Math.max(0, nbtEntries.size() - visibleRows);
        scrollOffset = Math.min(scrollOffset, maxScroll);

        for (int i = 0; i < visibleRows; i++) {
            int index = i + scrollOffset;
            if (index >= nbtEntries.size())
                break;

            NBTEntry entry = nbtEntries.get(index);
            int currentY = listY + i * itemHeight;

            this.buttonList.add(new GuiButton(400 + i, panelX + 10, currentY, 20, 20, "§cX"));

            GuiTextField keyField = new GuiTextField(200 + i, fontRenderer, panelX + 35, currentY, 120, 20);
            keyField.setText(entry.key);
            keyFields.add(keyField);

            int valueFieldWidth = 210;
            GuiTextField valueField = new GuiTextField(300 + i, fontRenderer, panelX + 160, currentY, valueFieldWidth,
                    20);

            String displayValue = entry.value;
            boolean isLongText = fontRenderer.getStringWidth(displayValue) > valueFieldWidth - 10;
            if (isLongText) {
                displayValue = fontRenderer.trimStringToWidth(displayValue, valueFieldWidth - 10) + "...";
            }
            valueField.setText(displayValue);
            valueField.setEnabled(!isLongText);
            valueFields.add(valueField);

            GuiButton editButton = new GuiButton(500 + i, panelX + 160 + valueFieldWidth + 5, currentY, 20, 20, "...");
            editButton.visible = isLongText;
            this.buttonList.add(editButton);
            this.editButtons.add(editButton);
        }
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        if (button.id == 100) { // 保存
            syncChangesFromFields();
            rebuildNbtFromList();
            itemStack.setTagCompound(this.nbt);
            itemStack.setStackDisplayName(itemNameField.getText());
            try {
                itemStack.setCount(Integer.parseInt(itemCountField.getText()));
            } catch (NumberFormatException e) {
                itemStack.setCount(1);
            }
            this.mc.displayGuiScreen(parentScreen);
        } else if (button.id == 101) { // 取消
            this.mc.displayGuiScreen(parentScreen);
        } else if (button.id == 102) { // 添加标签
            syncChangesFromFields();
            nbtEntries.add(new NBTEntry("new_key", new NBTTagString("new_value")));
            initGui();
        } else if (button.id >= 400 && button.id < 500) { // 删除按钮
            syncChangesFromFields();
            int indexToRemove = button.id - 400 + scrollOffset;
            if (indexToRemove < nbtEntries.size()) {
                nbtEntries.remove(indexToRemove);
                initGui();
            }
        } else if (button.id >= 500) { // "..." 编辑按钮
            int index = button.id - 500 + scrollOffset;
            if (index < nbtEntries.size()) {
                NBTEntry entry = nbtEntries.get(index);
                this.mc.displayGuiScreen(new GuiTextInput(this,
                        I18n.format("gui.nbt.editor.edit_value", entry.key), entry.value, (newValue) -> {
                            entry.value = newValue;
                            this.mc.displayGuiScreen(this);
                        }));
            }
        }
    }

    private void syncChangesFromFields() {
        for (int i = 0; i < keyFields.size(); i++) {
            int entryIndex = i + scrollOffset;
            if (entryIndex < nbtEntries.size()) {
                nbtEntries.get(entryIndex).key = keyFields.get(i).getText();
                if (valueFields.get(i).getEnableBackgroundDrawing()) {
                    nbtEntries.get(entryIndex).value = valueFields.get(i).getText();
                }
            }
        }
    }

    private void rebuildNbtFromList() {
        this.nbt.getKeySet().clear();
        for (NBTEntry entry : nbtEntries) {
            if (entry.key.trim().isEmpty())
                continue;
            try {
                NBTBase parsedTag = JsonToNBT.getTagFromJson(entry.value);
                nbt.setTag(entry.key, parsedTag);
            } catch (NBTException e) {
                nbt.setString(entry.key, entry.value);
            }
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        int panelWidth = 450;
        int panelX = (this.width - panelWidth) / 2;
        int panelY = 30;
        int panelHeight = this.height - 60;

        GuiTheme.drawPanel(panelX, panelY, panelWidth, panelHeight);
        drawCenteredString(fontRenderer, I18n.format("gui.nbt.editor.title"), this.width / 2, panelY + 10, 0xFFFFFF);

        drawString(fontRenderer, I18n.format("gui.nbt.editor.item_name"), panelX + 10, panelY + 30, 0xFFFFFF);
        drawThemedTextField(itemNameField);
        drawString(fontRenderer, I18n.format("gui.nbt.editor.count"), panelX + panelWidth - 120, panelY + 30, 0xFFFFFF);
        drawThemedTextField(itemCountField);

        for (GuiTextField field : keyFields)
            drawThemedTextField(field);
        for (GuiTextField field : valueFields)
            drawThemedTextField(field);

        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (keyCode == Keyboard.KEY_ESCAPE) {
            this.mc.displayGuiScreen(parentScreen);
            return;
        }
        itemNameField.textboxKeyTyped(typedChar, keyCode);
        itemCountField.textboxKeyTyped(typedChar, keyCode);
        for (GuiTextField field : keyFields)
            field.textboxKeyTyped(typedChar, keyCode);
        for (GuiTextField field : valueFields)
            field.textboxKeyTyped(typedChar, keyCode);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        itemNameField.mouseClicked(mouseX, mouseY, mouseButton);
        itemCountField.mouseClicked(mouseX, mouseY, mouseButton);
        for (GuiTextField field : keyFields)
            field.mouseClicked(mouseX, mouseY, mouseButton);
        for (GuiTextField field : valueFields)
            field.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        int dWheel = Mouse.getEventDWheel();
        if (dWheel != 0 && maxScroll > 0) {
            syncChangesFromFields();
            if (dWheel > 0) {
                scrollOffset = Math.max(0, scrollOffset - 1);
            } else {
                scrollOffset = Math.min(maxScroll, scrollOffset + 1);
            }
            initGui();
        }
    }

    @Override
    public void onGuiClosed() {
        Keyboard.enableRepeatEvents(false);
    }

    private static String tagToString(NBTBase tag) {
        return nbtBaseToString(tag);
    }

    /**
     * 递归地将NBT标签转换为一个完整的、无截断的字符串。
     * 
     * @param tag 要转换的NBT标签
     * @return 完整的字符串表示
     */
    private static String nbtBaseToString(NBTBase tag) {
        if (tag instanceof NBTTagCompound) {
            NBTTagCompound compound = (NBTTagCompound) tag;
            StringBuilder sb = new StringBuilder("{");
            Iterator<String> iterator = compound.getKeySet().iterator();
            while (iterator.hasNext()) {
                String key = iterator.next();
                // 为key加上引号，使其更像JSON
                sb.append("\"").append(key).append("\":").append(nbtBaseToString(compound.getTag(key)));
                if (iterator.hasNext()) {
                    sb.append(",");
                }
            }
            sb.append("}");
            return sb.toString();
        } else if (tag instanceof NBTTagList) {
            NBTTagList list = (NBTTagList) tag;
            StringBuilder sb = new StringBuilder("[");
            for (int i = 0; i < list.tagCount(); i++) {
                if (i > 0) {
                    sb.append(",");
                }
                sb.append(nbtBaseToString(list.get(i)));
            }
            sb.append("]");
            return sb.toString();
        }
        // 对于所有其他原始类型（包括NBTTagString），原生的toString()方法是安全且完整的。
        return tag.toString();
    }
}
