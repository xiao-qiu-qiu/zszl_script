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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class GuiBaritoneBlockMapEditor extends ThemedGuiScreen {

    private static final int BTN_ADD = 1;
    private static final int BTN_EDIT = 2;
    private static final int BTN_REMOVE = 3;
    private static final int BTN_CLEAR = 4;
    private static final int BTN_DEFAULT = 5;
    private static final int BTN_CANCEL = 6;
    private static final int BTN_DONE = 7;

    private final GuiScreen parentScreen;
    private final String settingKey;
    private final String settingLabel;
    private final LinkedHashMap<String, List<String>> defaultMappings;
    private final LinkedHashMap<String, List<String>> workingMappings;
    private final Consumer<String> onSave;

    private int panelX;
    private int panelY;
    private int panelWidth;
    private int panelHeight;
    private int listTop;
    private int listBottom;

    private BlockMappingListView listGui;
    private GuiTextField sourceBlockField;
    private GuiButton addButton;
    private GuiButton editButton;
    private GuiButton removeButton;
    private GuiButton clearButton;
    private GuiButton defaultButton;
    private GuiButton cancelButton;
    private GuiButton doneButton;

    private Block sourceBlockToAdd;
    private String sourceBlockIdToAdd = "";

    public GuiBaritoneBlockMapEditor(GuiScreen parentScreen, String settingKey, String settingLabel,
                                     Map<String, List<String>> currentMappings,
                                     Map<String, List<String>> defaultMappings,
                                     Consumer<String> onSave) {
        this.parentScreen = parentScreen;
        this.settingKey = settingKey == null ? "" : settingKey;
        this.settingLabel = settingLabel == null ? "" : settingLabel;
        this.defaultMappings = BaritoneBlockSettingEditorSupport.copyNormalizedBlockMap(defaultMappings);
        this.workingMappings = BaritoneBlockSettingEditorSupport.copyNormalizedBlockMap(currentMappings);
        this.onSave = onSave;
    }

    @Override
    public void initGui() {
        Keyboard.enableRepeatEvents(true);
        this.buttonList.clear();
        this.panelWidth = Math.min(660, Math.max(500, this.width - 24));
        this.panelHeight = Math.min(410, Math.max(332, this.height - 20));
        this.panelX = (this.width - this.panelWidth) / 2;
        this.panelY = (this.height - this.panelHeight) / 2;
        this.listTop = this.panelY + 48;
        this.listBottom = this.panelY + this.panelHeight - 86;

        this.listGui = new BlockMappingListView(this.mc);
        this.listGui.reload();

        int fieldX = this.panelX + 56;
        int fieldY = this.panelY + this.panelHeight - 74;
        this.sourceBlockField = new GuiTextField(10, this.fontRenderer, fieldX, fieldY, this.panelWidth - 170, 18);
        this.sourceBlockField.setMaxStringLength(96);
        this.sourceBlockField.setFocused(true);

        int buttonY = this.panelY + this.panelHeight - 44;
        int gap = 6;
        int buttonWidth = (this.panelWidth - 20 - gap * 5) / 6;
        int startX = this.panelX + 10;

        this.buttonList.add(this.addButton = new ThemedButton(BTN_ADD, this.panelX + this.panelWidth - 104, fieldY - 1,
                94, 20, "新增条目"));
        this.buttonList.add(this.editButton = new ThemedButton(BTN_EDIT, startX, buttonY, buttonWidth, 20, "编辑选中"));
        this.buttonList.add(this.removeButton = new ThemedButton(BTN_REMOVE, startX + (buttonWidth + gap), buttonY,
                buttonWidth, 20, "移除选中"));
        this.buttonList.add(this.clearButton = new ThemedButton(BTN_CLEAR, startX + (buttonWidth + gap) * 2, buttonY,
                buttonWidth, 20, "清空映射"));
        this.buttonList.add(this.defaultButton = new ThemedButton(BTN_DEFAULT, startX + (buttonWidth + gap) * 3, buttonY,
                buttonWidth, 20, "恢复默认"));
        this.buttonList.add(this.cancelButton = new ThemedButton(BTN_CANCEL, startX + (buttonWidth + gap) * 4, buttonY,
                buttonWidth, 20, "取消"));
        this.buttonList.add(this.doneButton = new ThemedButton(BTN_DONE, startX + (buttonWidth + gap) * 5, buttonY,
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
        if (this.sourceBlockField != null) {
            this.sourceBlockField.updateCursorCounter();
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
            if (!this.sourceBlockIdToAdd.isEmpty()) {
                if (!this.workingMappings.containsKey(this.sourceBlockIdToAdd)) {
                    this.workingMappings.put(this.sourceBlockIdToAdd, new ArrayList<String>());
                }
                this.sourceBlockField.setText("");
                this.listGui.reload();
                this.listGui.setSelectedBlockId(this.sourceBlockIdToAdd);
                updateInputPreview();
            }
            return;
        case BTN_EDIT:
            openSelectedMappingEditor();
            return;
        case BTN_REMOVE:
            String selectedId = this.listGui.getSelectedBlockId();
            if (selectedId != null) {
                this.workingMappings.remove(selectedId);
                this.listGui.reload();
                updateInputPreview();
            }
            return;
        case BTN_CLEAR:
            this.workingMappings.clear();
            this.listGui.reload();
            updateInputPreview();
            return;
        case BTN_DEFAULT:
            this.workingMappings.clear();
            this.workingMappings.putAll(BaritoneBlockSettingEditorSupport.copyNormalizedBlockMap(this.defaultMappings));
            this.listGui.reload();
            updateInputPreview();
            return;
        case BTN_CANCEL:
            this.mc.displayGuiScreen(this.parentScreen);
            return;
        case BTN_DONE:
            if (this.onSave != null) {
                this.onSave.accept(BaritoneBlockSettingEditorSupport.serializeBlockMapValue(this.workingMappings));
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
        if (this.sourceBlockField != null && this.sourceBlockField.textboxKeyTyped(typedChar, keyCode)) {
            updateInputPreview();
            return;
        }

        if (keyCode == Keyboard.KEY_RETURN || keyCode == Keyboard.KEY_NUMPADENTER) {
            if (this.sourceBlockField != null && this.sourceBlockField.getText() != null
                    && !this.sourceBlockField.getText().trim().isEmpty()) {
                actionPerformed(this.addButton);
            } else {
                actionPerformed(this.editButton);
            }
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
        GuiTheme.drawTitleBar(this.panelX, this.panelY, this.panelWidth, "Baritone方块映射编辑", this.fontRenderer);

        drawString(this.fontRenderer, "§7设置项: §f" + this.settingKey, this.panelX + 12, this.panelY + 22, GuiTheme.SUB_TEXT);
        drawString(this.fontRenderer,
                trimToWidth(this.settingLabel.isEmpty() ? "§7可视化编辑 源方块 -> 替代方块列表。"
                        : "§7说明: §f" + this.settingLabel, this.panelWidth - 24),
                this.panelX + 12, this.panelY + 32, GuiTheme.SUB_TEXT);

        if (this.listGui != null) {
            this.listGui.drawScreen(mouseX, mouseY, partialTicks);
        }

        drawString(this.fontRenderer, "§b新增源方块", this.panelX + 12, this.panelY + this.panelHeight - 84, 0xFFFFFFFF);
        drawBlockPreview(this.panelX + 14, this.panelY + this.panelHeight - 66);
        drawThemedTextField(this.sourceBlockField);
        if ((this.sourceBlockField.getText() == null || this.sourceBlockField.getText().isEmpty()) && !this.sourceBlockField.isFocused()) {
            drawString(this.fontRenderer, "输入要配置替代方块的源方块名称或 ID", this.sourceBlockField.x + 4, this.sourceBlockField.y + 5,
                    0xFF7B8A99);
        }

        String selectedId = this.listGui == null ? null : this.listGui.getSelectedBlockId();
        List<String> selectedTargets = selectedId == null ? null : this.workingMappings.get(selectedId);
        String statusText;
        int statusColor;
        if (this.sourceBlockField.getText() != null && !this.sourceBlockField.getText().trim().isEmpty()) {
            if (this.sourceBlockToAdd == null) {
                statusText = "§c未找到这个方块，请检查中文名称或方块 ID。";
                statusColor = 0xFFFF8A8A;
            } else if (this.workingMappings.containsKey(this.sourceBlockIdToAdd)) {
                statusText = "§e该源方块已存在，可直接选中后编辑替代列表。";
                statusColor = 0xFFFFE08A;
            } else {
                statusText = "§a将新增条目: §f" + BaritoneBlockSettingEditorSupport.getDisplayName(this.sourceBlockIdToAdd);
                statusColor = 0xFF9CFFB2;
            }
        } else if (selectedId != null) {
            int targetCount = selectedTargets == null ? 0 : selectedTargets.size();
            statusText = "§7当前选中 §f" + BaritoneBlockSettingEditorSupport.getDisplayName(selectedId)
                    + " §7，替代方块数: §f" + targetCount;
            statusColor = GuiTheme.SUB_TEXT;
        } else {
            statusText = "§7当前共配置 §f" + this.workingMappings.size() + " §7组方块映射。";
            statusColor = GuiTheme.SUB_TEXT;
        }
        drawString(this.fontRenderer, statusText, this.panelX + 12, this.panelY + this.panelHeight - 20, statusColor);

        super.drawScreen(mouseX, mouseY, partialTicks);

        if (isMouseOverField(mouseX, mouseY, this.sourceBlockField)) {
            List<String> tooltip = new ArrayList<String>();
            tooltip.add("§e源方块输入");
            tooltip.add("§7支持中文名称、原版或模组方块 ID。");
            tooltip.add("§7示例: §fminecraft:stone");
            drawHoveringText(tooltip, mouseX, mouseY);
        } else if (isMouseOverButton(mouseX, mouseY, this.editButton) && this.editButton.enabled && selectedId != null) {
            drawHoveringText(java.util.Arrays.asList("§e编辑替代方块列表", "§7当前选中: §f"
                    + BaritoneBlockSettingEditorSupport.getDisplayName(selectedId), "§8" + selectedId), mouseX, mouseY);
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
        if (this.sourceBlockField != null) {
            this.sourceBlockField.mouseClicked(mouseX, mouseY, mouseButton);
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
        String typed = this.sourceBlockField == null ? "" : this.sourceBlockField.getText();
        this.sourceBlockIdToAdd = BaritoneBlockSettingEditorSupport.normalizeBlockId(typed);
        this.sourceBlockToAdd = this.sourceBlockIdToAdd.isEmpty() ? null : BaritoneBlockSettingEditorSupport.resolveBlock(this.sourceBlockIdToAdd);

        if (this.addButton != null) {
            this.addButton.enabled = this.sourceBlockToAdd != null;
        }
        if (this.editButton != null) {
            this.editButton.enabled = this.listGui != null && this.listGui.getSelectedBlockId() != null;
        }
        if (this.removeButton != null) {
            this.removeButton.enabled = this.listGui != null && this.listGui.getSelectedBlockId() != null;
        }
        if (this.clearButton != null) {
            this.clearButton.enabled = !this.workingMappings.isEmpty();
        }
        if (this.defaultButton != null) {
            this.defaultButton.enabled = !this.workingMappings.equals(this.defaultMappings);
        }
    }

    private void openSelectedMappingEditor() {
        final String selectedId = this.listGui == null ? null : this.listGui.getSelectedBlockId();
        if (selectedId == null) {
            return;
        }
        List<String> currentTargets = this.workingMappings.containsKey(selectedId)
                ? this.workingMappings.get(selectedId)
                : new ArrayList<String>();
        List<String> defaultTargets = this.defaultMappings.containsKey(selectedId)
                ? this.defaultMappings.get(selectedId)
                : new ArrayList<String>();
        this.mc.displayGuiScreen(new GuiBaritoneBlockListEditor(this, selectedId,
                "编辑 " + BaritoneBlockSettingEditorSupport.getDisplayName(selectedId) + " 的替代方块列表",
                currentTargets, defaultTargets,
                new Consumer<String>() {
                    @Override
                    public void accept(String value) {
                        List<String> parsedTargets = BaritoneBlockSettingEditorSupport.parseBlockListValue(value);
                        if (parsedTargets.isEmpty()) {
                            GuiBaritoneBlockMapEditor.this.workingMappings.remove(selectedId);
                        } else {
                            GuiBaritoneBlockMapEditor.this.workingMappings.put(selectedId, parsedTargets);
                        }
                        if (GuiBaritoneBlockMapEditor.this.listGui != null) {
                            GuiBaritoneBlockMapEditor.this.listGui.reload();
                        }
                        GuiBaritoneBlockMapEditor.this.updateInputPreview();
                    }
                }));
    }

    private void drawBlockPreview(int x, int y) {
        drawRect(x - 2, y - 2, x + 18, y + 18, 0xFF31475D);
        drawRect(x - 1, y - 1, x + 17, y + 17, 0xFF16212D);
        ItemStack stack = BaritoneBlockSettingEditorSupport.getBlockStack(this.sourceBlockToAdd);
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

    private String buildTargetsSummary(List<String> blockIds, int maxWidth) {
        if (blockIds == null || blockIds.isEmpty()) {
            return "未配置替代方块";
        }
        StringBuilder summary = new StringBuilder("共 ").append(blockIds.size()).append(" 项: ");
        int appended = 0;
        for (String blockId : blockIds) {
            if (appended > 0) {
                summary.append(", ");
            }
            summary.append(BaritoneBlockSettingEditorSupport.getDisplayName(blockId));
            appended++;
            String candidate = summary.toString();
            if (this.fontRenderer.getStringWidth(candidate) > maxWidth) {
                return trimToWidth(candidate, maxWidth);
            }
            if (appended >= 3 && blockIds.size() > appended) {
                summary.append("...");
                return trimToWidth(summary.toString(), maxWidth);
            }
        }
        return trimToWidth(summary.toString(), maxWidth);
    }

    private final class BlockMappingListView extends GuiScrollingList {

        private final Minecraft mc;
        private final List<String> sourceEntries = new ArrayList<String>();
        private int selectedIndex = -1;

        private BlockMappingListView(Minecraft mc) {
            super(mc, GuiBaritoneBlockMapEditor.this.panelWidth - 24, GuiBaritoneBlockMapEditor.this.panelHeight,
                    GuiBaritoneBlockMapEditor.this.listTop, GuiBaritoneBlockMapEditor.this.listBottom,
                    GuiBaritoneBlockMapEditor.this.panelX + 12, 24,
                    GuiBaritoneBlockMapEditor.this.width, GuiBaritoneBlockMapEditor.this.height);
            this.mc = mc;
        }

        private void reload() {
            String selectedId = getSelectedBlockId();
            this.sourceEntries.clear();
            this.sourceEntries.addAll(GuiBaritoneBlockMapEditor.this.workingMappings.keySet());
            if (selectedId != null) {
                this.selectedIndex = this.sourceEntries.indexOf(selectedId);
            }
            if (this.selectedIndex < 0 || this.selectedIndex >= this.sourceEntries.size()) {
                this.selectedIndex = this.sourceEntries.isEmpty() ? -1 : 0;
            }
            GuiBaritoneBlockMapEditor.this.updateInputPreview();
        }

        private String getSelectedBlockId() {
            return this.selectedIndex >= 0 && this.selectedIndex < this.sourceEntries.size()
                    ? this.sourceEntries.get(this.selectedIndex)
                    : null;
        }

        private void setSelectedBlockId(String blockId) {
            this.selectedIndex = this.sourceEntries.indexOf(blockId);
            if (this.selectedIndex < 0 && !this.sourceEntries.isEmpty()) {
                this.selectedIndex = 0;
            }
        }

        @Override
        protected int getSize() {
            return this.sourceEntries.size();
        }

        @Override
        protected void elementClicked(int index, boolean doubleClick) {
            if (index < 0 || index >= this.sourceEntries.size()) {
                return;
            }
            this.selectedIndex = index;
            GuiBaritoneBlockMapEditor.this.updateInputPreview();
            if (doubleClick) {
                GuiBaritoneBlockMapEditor.this.openSelectedMappingEditor();
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
            if (slotIdx < 0 || slotIdx >= this.sourceEntries.size()) {
                return;
            }

            String sourceBlockId = this.sourceEntries.get(slotIdx);
            Block block = BaritoneBlockSettingEditorSupport.resolveBlock(sourceBlockId);
            ItemStack stack = BaritoneBlockSettingEditorSupport.getBlockStack(block);
            List<String> targetBlocks = GuiBaritoneBlockMapEditor.this.workingMappings.get(sourceBlockId);

            drawRect(this.left + 2, slotTop - 1, entryRight - 4, slotTop + this.slotHeight - 2, 0x22131A22);

            if (!stack.isEmpty()) {
                GL11.glPushMatrix();
                GL11.glTranslated(this.left + 6, slotTop + 3, 0.0D);
                GL11.glScaled(0.9D, 0.9D, 1.0D);
                RenderHelper.enableGUIStandardItemLighting();
                this.mc.getRenderItem().renderItemAndEffectIntoGUI(stack, 0, 0);
                RenderHelper.disableStandardItemLighting();
                GL11.glPopMatrix();
            }

            FontRenderer fr = this.mc.fontRenderer;
            fr.drawString(BaritoneBlockSettingEditorSupport.getDisplayName(sourceBlockId), this.left + 28, slotTop + 2, 0xFFEAF6FF);
            fr.drawString("§8" + sourceBlockId, this.left + 28, slotTop + 12, 0xFF9FB2C8);
            fr.drawString(GuiBaritoneBlockMapEditor.this.buildTargetsSummary(targetBlocks, this.listWidth - 40),
                    this.left + 28, slotTop + 22, 0xFFB7CCDD);
        }
    }
}
