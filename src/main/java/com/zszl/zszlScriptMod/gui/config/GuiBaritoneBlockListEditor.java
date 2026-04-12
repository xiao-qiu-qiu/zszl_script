package com.zszl.zszlScriptMod.gui.config;

import com.zszl.zszlScriptMod.gui.components.GuiTheme;
import com.zszl.zszlScriptMod.gui.components.ThemedButton;
import com.zszl.zszlScriptMod.gui.components.ThemedGuiScreen;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.client.GuiScrollingList;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class GuiBaritoneBlockListEditor extends ThemedGuiScreen {

    private static final int BTN_ADD = 1;
    private static final int BTN_REMOVE = 2;
    private static final int BTN_CLEAR = 3;
    private static final int BTN_DEFAULT = 4;
    private static final int BTN_CANCEL = 5;
    private static final int BTN_DONE = 6;

    private final GuiScreen parentScreen;
    private final String settingKey;
    private final String settingLabel;
    private final List<String> defaultBlockIds;
    private final List<String> workingBlockIds;
    private final Consumer<String> onSave;

    private int panelX;
    private int panelY;
    private int panelWidth;
    private int panelHeight;
    private int listTop;
    private int listBottom;

    private BlockListView listGui;
    private GuiTextField blockIdField;
    private GuiButton addButton;
    private GuiButton removeButton;
    private GuiButton clearButton;
    private GuiButton defaultButton;
    private GuiButton cancelButton;
    private GuiButton doneButton;

    private Block blockToAdd;
    private String blockIdToAdd = "";

    public GuiBaritoneBlockListEditor(GuiScreen parentScreen, String settingKey, String settingLabel,
                                      List<String> currentBlockIds, List<String> defaultBlockIds, Consumer<String> onSave) {
        this.parentScreen = parentScreen;
        this.settingKey = settingKey == null ? "" : settingKey;
        this.settingLabel = settingLabel == null ? "" : settingLabel;
        this.defaultBlockIds = BaritoneBlockSettingEditorSupport.copyNormalizedBlockIds(defaultBlockIds);
        this.workingBlockIds = BaritoneBlockSettingEditorSupport.copyNormalizedBlockIds(currentBlockIds);
        this.onSave = onSave;
    }

    @Override
    public void initGui() {
        Keyboard.enableRepeatEvents(true);
        this.buttonList.clear();
        this.panelWidth = Math.min(580, Math.max(440, this.width - 24));
        this.panelHeight = Math.min(380, Math.max(316, this.height - 20));
        this.panelX = (this.width - this.panelWidth) / 2;
        this.panelY = (this.height - this.panelHeight) / 2;
        this.listTop = this.panelY + 48;
        this.listBottom = this.panelY + this.panelHeight - 86;

        this.listGui = new BlockListView(this.mc);
        this.listGui.reload();

        int fieldX = this.panelX + 56;
        int fieldY = this.panelY + this.panelHeight - 74;
        this.blockIdField = new GuiTextField(10, this.fontRenderer, fieldX, fieldY, this.panelWidth - 170, 18);
        this.blockIdField.setMaxStringLength(96);
        this.blockIdField.setFocused(true);

        int buttonY = this.panelY + this.panelHeight - 44;
        int gap = 6;
        int buttonWidth = (this.panelWidth - 20 - gap * 4) / 5;
        int startX = this.panelX + 10;

        this.buttonList.add(this.addButton = new ThemedButton(BTN_ADD, this.panelX + this.panelWidth - 104, fieldY - 1,
                94, 20, "添加方块"));
        this.buttonList.add(this.removeButton = new ThemedButton(BTN_REMOVE, startX, buttonY, buttonWidth, 20, "移除选中"));
        this.buttonList.add(this.clearButton = new ThemedButton(BTN_CLEAR, startX + (buttonWidth + gap), buttonY,
                buttonWidth, 20, "清空列表"));
        this.buttonList.add(this.defaultButton = new ThemedButton(BTN_DEFAULT, startX + (buttonWidth + gap) * 2, buttonY,
                buttonWidth, 20, "恢复默认"));
        this.buttonList.add(this.cancelButton = new ThemedButton(BTN_CANCEL, startX + (buttonWidth + gap) * 3, buttonY,
                buttonWidth, 20, "取消"));
        this.buttonList.add(this.doneButton = new ThemedButton(BTN_DONE, startX + (buttonWidth + gap) * 4, buttonY,
                buttonWidth, 20, "完成"));

        updateInputPreview();
    }

    @Override
    public void onGuiClosed() {
        Keyboard.enableRepeatEvents(false);
        super.onGuiClosed();
    }

    @Override
    public void updateScreen() {
        super.updateScreen();
        if (this.blockIdField != null) {
            this.blockIdField.updateCursorCounter();
        }
        updateInputPreview();
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        if (button == null || !button.enabled) {
            return;
        }

        switch (button.id) {
        case BTN_ADD:
            if (!this.blockIdToAdd.isEmpty() && !this.workingBlockIds.contains(this.blockIdToAdd)) {
                this.workingBlockIds.add(this.blockIdToAdd);
                this.blockIdField.setText("");
                this.listGui.reload();
                updateInputPreview();
            }
            return;
        case BTN_REMOVE:
            String selectedId = this.listGui.getSelectedBlockId();
            if (selectedId != null) {
                this.workingBlockIds.remove(selectedId);
                this.listGui.reload();
                updateInputPreview();
            }
            return;
        case BTN_CLEAR:
            this.workingBlockIds.clear();
            this.listGui.reload();
            updateInputPreview();
            return;
        case BTN_DEFAULT:
            this.workingBlockIds.clear();
            this.workingBlockIds.addAll(this.defaultBlockIds);
            this.listGui.reload();
            updateInputPreview();
            return;
        case BTN_CANCEL:
            this.mc.displayGuiScreen(this.parentScreen);
            return;
        case BTN_DONE:
            if (this.onSave != null) {
                this.onSave.accept(BaritoneBlockSettingEditorSupport.serializeBlockListValue(this.workingBlockIds));
            }
            this.mc.displayGuiScreen(this.parentScreen);
            return;
        default:
            return;
        }
    }

    @Override
    public void handleMouseInput() throws IOException {
        int mouseX = Mouse.getEventX() * this.width / this.mc.displayWidth;
        int mouseY = this.height - Mouse.getEventY() * this.height / this.mc.displayHeight - 1;

        if (this.listGui != null) {
            this.listGui.handleMouseInput(mouseX, mouseY);
        }

        int button = Mouse.getEventButton();
        if (button == -1) {
            return;
        }

        if (Mouse.getEventButtonState()) {
            handleMousePressed(mouseX, mouseY, button);
        } else {
            mouseReleased(mouseX, mouseY, button);
        }
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        handleMousePressed(mouseX, mouseY, mouseButton);
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (this.blockIdField != null && this.blockIdField.textboxKeyTyped(typedChar, keyCode)) {
            updateInputPreview();
            return;
        }

        if (keyCode == Keyboard.KEY_RETURN || keyCode == Keyboard.KEY_NUMPADENTER) {
            actionPerformed(this.addButton);
            return;
        }
        if (keyCode == Keyboard.KEY_DELETE) {
            actionPerformed(this.removeButton);
            return;
        }
        if (keyCode == Keyboard.KEY_ESCAPE) {
            this.mc.displayGuiScreen(this.parentScreen);
            return;
        }
        super.keyTyped(typedChar, keyCode);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        GuiTheme.drawPanel(this.panelX, this.panelY, this.panelWidth, this.panelHeight);
        GuiTheme.drawTitleBar(this.panelX, this.panelY, this.panelWidth, "Baritone方块列表编辑", this.fontRenderer);

        drawString(this.fontRenderer, "§7设置项: §f" + this.settingKey, this.panelX + 12, this.panelY + 22, GuiTheme.SUB_TEXT);
        drawString(this.fontRenderer,
                trimToWidth(this.settingLabel.isEmpty() ? "§7可视化编辑当前方块列表。" : "§7说明: §f" + this.settingLabel, this.panelWidth - 24),
                this.panelX + 12, this.panelY + 32, GuiTheme.SUB_TEXT);

        if (this.listGui != null) {
            this.listGui.drawScreen(mouseX, mouseY, partialTicks);
        }

        drawString(this.fontRenderer, "§b添加方块", this.panelX + 12, this.panelY + this.panelHeight - 84, 0xFFFFFFFF);
        drawBlockPreview(this.panelX + 14, this.panelY + this.panelHeight - 66);
        drawThemedTextField(this.blockIdField);
        if ((this.blockIdField.getText() == null || this.blockIdField.getText().isEmpty()) && !this.blockIdField.isFocused()) {
            drawString(this.fontRenderer, "输入方块名或 ID", this.blockIdField.x + 4, this.blockIdField.y + 5, 0xFF7B8A99);
        }

        String statusText;
        int statusColor;
        if (this.blockIdField.getText() == null || this.blockIdField.getText().trim().isEmpty()) {
            statusText = "§7当前已选择 §f" + this.workingBlockIds.size() + " §7个方块。";
            statusColor = GuiTheme.SUB_TEXT;
        } else if (this.blockToAdd == null) {
            statusText = "§c未找到这个方块，请检查中文名称或方块 ID。";
            statusColor = 0xFFFF8A8A;
        } else if (this.workingBlockIds.contains(this.blockIdToAdd)) {
            statusText = "§e该方块已在列表中，可直接从列表里选中后移除。";
            statusColor = 0xFFFFE08A;
        } else {
            statusText = "§a将添加: §f" + BaritoneBlockSettingEditorSupport.getDisplayName(this.blockIdToAdd);
            statusColor = 0xFF9CFFB2;
        }
        drawString(this.fontRenderer, statusText, this.panelX + 12, this.panelY + this.panelHeight - 20, statusColor);

        super.drawScreen(mouseX, mouseY, partialTicks);

        if (isMouseOverField(mouseX, mouseY, this.blockIdField)) {
            List<String> tooltip = new ArrayList<String>();
            tooltip.add("§e方块输入");
            tooltip.add("§7支持中文名称、原版或模组方块 ID。");
            tooltip.add("§7示例: §fminecraft:diamond_ore");
            drawHoveringText(tooltip, mouseX, mouseY);
        } else if (isMouseOverButton(mouseX, mouseY, this.removeButton) && this.removeButton.enabled) {
            String selectedId = this.listGui.getSelectedBlockId();
            if (selectedId != null) {
                drawHoveringText(java.util.Arrays.asList("§e移除选中", "§7当前选中: §f"
                        + BaritoneBlockSettingEditorSupport.getDisplayName(selectedId), "§8" + selectedId), mouseX, mouseY);
            }
        }
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }

    private boolean handleMousePressed(int mouseX, int mouseY, int mouseButton) throws IOException {
        if (mouseButton == 0 && handleButtonActivation(mouseX, mouseY)) {
            return true;
        }
        if (this.blockIdField != null) {
            this.blockIdField.mouseClicked(mouseX, mouseY, mouseButton);
        }
        return false;
    }

    private boolean handleButtonActivation(int mouseX, int mouseY) throws IOException {
        for (GuiButton button : this.buttonList) {
            if (button == null || !button.visible || !button.enabled) {
                continue;
            }
            if (!isMouseOverButton(mouseX, mouseY, button)) {
                continue;
            }
            button.playPressSound(this.mc.getSoundHandler());
            actionPerformed(button);
            return true;
        }
        return false;
    }

    private void updateInputPreview() {
        String typed = this.blockIdField == null ? "" : this.blockIdField.getText();
        this.blockIdToAdd = BaritoneBlockSettingEditorSupport.normalizeBlockId(typed);
        this.blockToAdd = this.blockIdToAdd.isEmpty() ? null : BaritoneBlockSettingEditorSupport.resolveBlock(this.blockIdToAdd);

        if (this.addButton != null) {
            this.addButton.enabled = this.blockToAdd != null && !this.workingBlockIds.contains(this.blockIdToAdd);
        }
        if (this.removeButton != null) {
            this.removeButton.enabled = this.listGui != null && this.listGui.getSelectedBlockId() != null;
        }
        if (this.clearButton != null) {
            this.clearButton.enabled = !this.workingBlockIds.isEmpty();
        }
        if (this.defaultButton != null) {
            this.defaultButton.enabled = !this.workingBlockIds.equals(this.defaultBlockIds);
        }
    }

    private void drawBlockPreview(int x, int y) {
        drawRect(x - 2, y - 2, x + 18, y + 18, 0xFF31475D);
        drawRect(x - 1, y - 1, x + 17, y + 17, 0xFF16212D);
        ItemStack stack = BaritoneBlockSettingEditorSupport.getBlockStack(this.blockToAdd);
        if (stack.isEmpty()) {
            return;
        }

        RenderHelper.enableGUIStandardItemLighting();
        this.itemRender.renderItemAndEffectIntoGUI(stack, x, y);
        this.itemRender.renderItemOverlayIntoGUI(this.fontRenderer, stack, x, y, null);
        RenderHelper.disableStandardItemLighting();
    }

    private String trimToWidth(String text, int maxWidth) {
        if (text == null) {
            return "";
        }
        if (this.fontRenderer.getStringWidth(text) <= maxWidth) {
            return text;
        }
        return this.fontRenderer.trimStringToWidth(text, Math.max(0, maxWidth - this.fontRenderer.getStringWidth(".."))) + "..";
    }

    private final class BlockListView extends GuiScrollingList {

        private final Minecraft mc;
        private int selectedIndex = -1;

        private BlockListView(Minecraft mc) {
            super(mc, GuiBaritoneBlockListEditor.this.panelWidth - 24, GuiBaritoneBlockListEditor.this.panelHeight,
                    GuiBaritoneBlockListEditor.this.listTop, GuiBaritoneBlockListEditor.this.listBottom,
                    GuiBaritoneBlockListEditor.this.panelX + 12, 22,
                    GuiBaritoneBlockListEditor.this.width, GuiBaritoneBlockListEditor.this.height);
            this.mc = mc;
        }

        private void reload() {
            String selectedId = getSelectedBlockId();
            if (selectedId != null) {
                this.selectedIndex = GuiBaritoneBlockListEditor.this.workingBlockIds.indexOf(selectedId);
            }
            if (this.selectedIndex < 0 || this.selectedIndex >= GuiBaritoneBlockListEditor.this.workingBlockIds.size()) {
                this.selectedIndex = GuiBaritoneBlockListEditor.this.workingBlockIds.isEmpty() ? -1 : 0;
            }
            GuiBaritoneBlockListEditor.this.updateInputPreview();
        }

        private String getSelectedBlockId() {
            return this.selectedIndex >= 0 && this.selectedIndex < GuiBaritoneBlockListEditor.this.workingBlockIds.size()
                    ? GuiBaritoneBlockListEditor.this.workingBlockIds.get(this.selectedIndex)
                    : null;
        }

        @Override
        protected int getSize() {
            return GuiBaritoneBlockListEditor.this.workingBlockIds.size();
        }

        @Override
        protected void elementClicked(int index, boolean doubleClick) {
            if (index < 0 || index >= GuiBaritoneBlockListEditor.this.workingBlockIds.size()) {
                return;
            }
            this.selectedIndex = index;
            GuiBaritoneBlockListEditor.this.updateInputPreview();
            if (doubleClick) {
                GuiBaritoneBlockListEditor.this.workingBlockIds.remove(index);
                reload();
            }
        }

        @Override
        protected boolean isSelected(int index) {
            return index == this.selectedIndex;
        }

        @Override
        protected void drawBackground() {
            drawRect(this.left, this.top - 4, this.left + this.listWidth, this.bottom + 4, 0x44101822);
            drawRect(this.left, this.top - 4, this.left + this.listWidth, this.top - 3, 0xFF5FB8FF);
            drawRect(this.left, this.bottom + 3, this.left + this.listWidth, this.bottom + 4, 0xFF35536C);
        }

        @Override
        protected void drawSlot(int slotIdx, int entryRight, int slotTop, int slotBuffer,
                                net.minecraft.client.renderer.Tessellator tess) {
            if (slotIdx < 0 || slotIdx >= GuiBaritoneBlockListEditor.this.workingBlockIds.size()) {
                return;
            }

            String blockId = GuiBaritoneBlockListEditor.this.workingBlockIds.get(slotIdx);
            Block block = BaritoneBlockSettingEditorSupport.resolveBlock(blockId);
            ItemStack stack = BaritoneBlockSettingEditorSupport.getBlockStack(block);

            drawRect(this.left + 2, slotTop - 1, entryRight - 4, slotTop + this.slotHeight - 2, 0x22131A22);

            if (!stack.isEmpty()) {
                GL11.glPushMatrix();
                GL11.glTranslated(this.left + 6, slotTop + 2, 0.0D);
                GL11.glScaled(0.9D, 0.9D, 1.0D);
                RenderHelper.enableGUIStandardItemLighting();
                this.mc.getRenderItem().renderItemAndEffectIntoGUI(stack, 0, 0);
                RenderHelper.disableStandardItemLighting();
                GL11.glPopMatrix();
            }

            FontRenderer fr = this.mc.fontRenderer;
            fr.drawString(BaritoneBlockSettingEditorSupport.getDisplayName(blockId), this.left + 28, slotTop + 2, 0xFFEAF6FF);
            fr.drawString("§8" + blockId, this.left + 28, slotTop + 12, 0xFF9FB2C8);
        }
    }
}
