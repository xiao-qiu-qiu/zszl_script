package com.zszl.zszlScriptMod.otherfeatures.gui.render;

import com.zszl.zszlScriptMod.gui.components.GuiTheme;
import com.zszl.zszlScriptMod.gui.components.ThemedButton;
import com.zszl.zszlScriptMod.gui.components.ThemedGuiScreen;
import com.zszl.zszlScriptMod.otherfeatures.handler.render.RenderFeatureManager;
import com.zszl.zszlScriptMod.shadowbaritone.api.utils.BlockDisplayLookup;
import com.zszl.zszlScriptMod.shadowbaritone.api.utils.BlockUtils;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.client.GuiScrollingList;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class GuiXrayBlockEditor extends ThemedGuiScreen {

    private static final int BTN_ADD = 1;
    private static final int BTN_REMOVE = 2;
    private static final int BTN_CLEAR = 3;
    private static final int BTN_DEFAULT = 4;
    private static final int BTN_DONE = 5;

    private final GuiScreen parentScreen;

    private int panelX;
    private int panelY;
    private int panelWidth;
    private int panelHeight;
    private int listTop;
    private int listBottom;

    private XrayBlockList listGui;
    private GuiTextField blockIdField;
    private GuiButton addButton;
    private GuiButton removeButton;
    private GuiButton clearButton;
    private GuiButton defaultButton;
    private GuiButton doneButton;

    private Block blockToAdd;
    private String blockIdToAdd = "";
    private String blockIdInputText = "";
    private int blockIdCursor = 0;

    public GuiXrayBlockEditor(GuiScreen parentScreen) {
        this.parentScreen = parentScreen;
    }

    @Override
    public void initGui() {
        Keyboard.enableRepeatEvents(true);
        this.buttonList.clear();
        this.panelWidth = Math.min(560, Math.max(420, this.width - 24));
        this.panelHeight = Math.min(360, Math.max(300, this.height - 20));
        this.panelX = (this.width - this.panelWidth) / 2;
        this.panelY = (this.height - this.panelHeight) / 2;
        this.listTop = this.panelY + 48;
        this.listBottom = this.panelY + this.panelHeight - 80;

        this.listGui = new XrayBlockList(this.mc, this);
        this.listGui.reload();

        int fieldX = this.panelX + 56;
        int fieldY = this.panelY + this.panelHeight - 68;
        this.blockIdField = new GuiTextField(10, this.fontRenderer, fieldX, fieldY, this.panelWidth - 170, 18);
        this.blockIdField.setMaxStringLength(Integer.MAX_VALUE);
        this.blockIdField.setCanLoseFocus(true);
        this.blockIdField.setTextColor(0xFFEAF6FF);
        this.blockIdField.setDisabledTextColour(0xFF9FB2C8);
        this.blockIdField.setFocused(true);
        this.blockIdInputText = "";
        this.blockIdCursor = 0;
        syncInternalTextFieldState();

        int buttonY = this.panelY + this.panelHeight - 42;
        int gap = 6;
        int buttonWidth = (this.panelWidth - 20 - gap * 4) / 5;
        int startX = this.panelX + 10;

        this.buttonList.add(this.addButton = new ThemedButton(BTN_ADD, this.panelX + this.panelWidth - 104, fieldY - 1,
                94, 20, "添加方块"));
        this.buttonList.add(this.removeButton = new ThemedButton(BTN_REMOVE, startX, buttonY, buttonWidth, 20, "移除选中"));
        this.buttonList.add(this.clearButton = new ThemedButton(BTN_CLEAR, startX + (buttonWidth + gap), buttonY,
                buttonWidth, 20, "清空列表"));
        this.buttonList.add(this.defaultButton = new ThemedButton(BTN_DEFAULT,
                startX + (buttonWidth + gap) * 2, buttonY, buttonWidth, 20, "恢复默认"));
        this.buttonList.add(this.doneButton = new ThemedButton(BTN_DONE, startX + (buttonWidth + gap) * 3, buttonY,
                buttonWidth + buttonWidth + gap, 20, "完成"));

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
        updateInputPreview();
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        if (button == null || !button.enabled) {
            return;
        }

        switch (button.id) {
        case BTN_ADD:
            if (!this.blockIdToAdd.isEmpty()) {
                RenderFeatureManager.setXrayBlockVisible(this.blockIdToAdd, true);
                clearBlockInput();
                this.listGui.reload();
                updateInputPreview();
            }
            return;
        case BTN_REMOVE:
            String selectedId = this.listGui.getSelectedBlockId();
            if (selectedId != null) {
                RenderFeatureManager.setXrayBlockVisible(selectedId, false);
                this.listGui.reload();
                updateInputPreview();
            }
            return;
        case BTN_CLEAR:
            RenderFeatureManager.clearXrayVisibleBlocksWithoutSave();
            this.listGui.reload();
            updateInputPreview();
            return;
        case BTN_DEFAULT:
            RenderFeatureManager.resetXrayVisibleBlocksWithoutSave();
            this.listGui.reload();
            updateInputPreview();
            return;
        case BTN_DONE:
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
        if (processKeyStroke(typedChar, keyCode)) {
            updateInputPreview();
            return;
        }
    }

    @Override
    public void handleKeyboardInput() throws IOException {
        char typedChar = Keyboard.getEventCharacter();
        int keyCode = Keyboard.getEventKey();
        boolean keyDown = Keyboard.getEventKeyState();
        boolean textCommitEvent = !keyDown && keyCode == 0 && typedChar >= 32;

        if (!keyDown && !textCommitEvent) {
            return;
        }
        if (keyCode == 0 && typedChar >= 32) {
            keyCode = typedChar + 256;
        }
        if (processKeyStroke(typedChar, keyCode)) {
            updateInputPreview();
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        GuiTheme.drawPanel(this.panelX, this.panelY, this.panelWidth, this.panelHeight);
        GuiTheme.drawTitleBar(this.panelX, this.panelY, this.panelWidth, "X光透视方块编辑", this.fontRenderer);

        drawString(this.fontRenderer, "§7这里只管理 X 光模式下允许保留渲染的方块列表。", this.panelX + 12,
                this.panelY + 22, GuiTheme.SUB_TEXT);
        drawString(this.fontRenderer, "§7可直接输入中文名称或方块 ID，例如 §fminecraft:diamond_ore§7。当前共 §f"
                + RenderFeatureManager.getXrayVisibleBlockIds().size() + " §7项。", this.panelX + 12, this.panelY + 32,
                GuiTheme.SUB_TEXT);

        if (this.listGui != null) {
            this.listGui.drawScreen(mouseX, mouseY, partialTicks);
        }

        drawString(this.fontRenderer, "§b添加方块", this.panelX + 12, this.panelY + this.panelHeight - 78, 0xFFFFFFFF);
        drawBlockPreview(this.panelX + 14, this.panelY + this.panelHeight - 60);
        drawCustomBlockInputField();

        String statusText;
        int statusColor;
        if (this.blockIdInputText == null || this.blockIdInputText.trim().isEmpty()) {
            statusText = "§7输入方块名后即可添加到透视列表。";
            statusColor = GuiTheme.SUB_TEXT;
        } else if (this.blockToAdd == null) {
            statusText = "§c未找到这个方块，请检查中文名称或方块 ID。";
            statusColor = 0xFFFF8A8A;
        } else if (RenderFeatureManager.isXrayBlockIdVisible(this.blockIdToAdd)) {
            statusText = "§e该方块已在透视列表中。";
            statusColor = 0xFFFFE08A;
        } else {
            statusText = "§a将添加: §f" + getDisplayName(this.blockIdToAdd);
            statusColor = 0xFF9CFFB2;
        }
        drawString(this.fontRenderer, statusText, this.panelX + 12, this.panelY + this.panelHeight - 20, statusColor);

        super.drawScreen(mouseX, mouseY, partialTicks);

        if (isMouseOverField(mouseX, mouseY, this.blockIdField)) {
            List<String> tooltip = new ArrayList<>();
            tooltip.add("§e方块输入");
            tooltip.add("§7支持中文名称、原版或模组方块 ID。");
            tooltip.add("§7示例: §fminecraft:diamond_ore");
            drawHoveringText(tooltip, mouseX, mouseY);
        } else if (isMouseOverButton(mouseX, mouseY, this.removeButton) && this.removeButton.enabled) {
            String selectedId = this.listGui.getSelectedBlockId();
            if (selectedId != null) {
                drawHoveringText(java.util.Arrays.asList("§e移除选中", "§7当前选中: §f" + getDisplayName(selectedId),
                        "§8" + selectedId), mouseX, mouseY);
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
            boolean focused = isMouseOverField(mouseX, mouseY, this.blockIdField);
            this.blockIdField.setFocused(focused);
            if (focused && mouseButton == 0) {
                this.blockIdCursor = resolveCursorFromMouse(mouseX);
                syncInternalTextFieldState();
            }
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
        String typed = this.blockIdInputText == null ? "" : this.blockIdInputText;
        Block resolved = BlockDisplayLookup.findBlockByUserInput(typed);
        this.blockToAdd = resolved;
        this.blockIdToAdd = resolved == null ? "" : BlockUtils.blockToString(resolved);

        if (this.addButton != null) {
            this.addButton.enabled = this.blockToAdd != null && !RenderFeatureManager.isXrayBlockIdVisible(this.blockIdToAdd);
        }
        if (this.removeButton != null) {
            this.removeButton.enabled = this.listGui != null && this.listGui.getSelectedBlockId() != null;
        }
        if (this.clearButton != null) {
            this.clearButton.enabled = !RenderFeatureManager.getXrayVisibleBlockIds().isEmpty();
        }
    }

    private void drawCustomBlockInputField() {
        if (this.blockIdField == null) {
            return;
        }
        GuiTheme.drawInputFrame(this.blockIdField.x - 1, this.blockIdField.y - 1,
                this.blockIdField.width + 2, this.blockIdField.height + 2,
                this.blockIdField.isFocused(), true);

        GlStateManager.disableLighting();
        GlStateManager.disableDepth();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);

        String text = this.blockIdInputText;
        if ((text == null || text.isEmpty()) && !this.blockIdField.isFocused()) {
            this.fontRenderer.drawString("输入方块名或 ID", this.blockIdField.x + 4, this.blockIdField.y + 5, 0xFF7B8A99);
            return;
        }

        text = text == null ? "" : text;
        int cursor = Math.max(0, Math.min(this.blockIdCursor, text.length()));
        int maxWidth = Math.max(8, this.blockIdField.width - 8);
        int start = 0;
        while (start < cursor && this.fontRenderer.getStringWidth(text.substring(start, cursor)) > maxWidth) {
            start++;
        }
        int end = text.length();
        while (end > cursor && this.fontRenderer.getStringWidth(text.substring(start, end)) > maxWidth) {
            end--;
        }
        while (start > 0 && this.fontRenderer.getStringWidth(text.substring(start - 1, end)) <= maxWidth) {
            start--;
        }

        String visibleText = text.substring(start, end);
        int textX = this.blockIdField.x + 4;
        int textY = this.blockIdField.y + 5;
        this.fontRenderer.drawString(visibleText, textX, textY, 0xFFEAF6FF);

        if (this.blockIdField.isFocused() && (Minecraft.getSystemTime() / 500L) % 2L == 0L) {
            int visibleCursor = Math.max(0, Math.min(cursor - start, visibleText.length()));
            int cursorX = textX + this.fontRenderer.getStringWidth(visibleText.substring(0, visibleCursor));
            drawRect(cursorX, this.blockIdField.y + 3, cursorX + 1, this.blockIdField.y + this.blockIdField.height - 3, 0xFFEAF6FF);
        }
    }

    private boolean processKeyStroke(char typedChar, int keyCode) throws IOException {
        if (handleBlockInputKeyTyped(typedChar, keyCode)) {
            return true;
        }

        if (keyCode == Keyboard.KEY_RETURN || keyCode == Keyboard.KEY_NUMPADENTER) {
            actionPerformed(this.addButton);
            return true;
        }
        if (keyCode == Keyboard.KEY_DELETE && !this.blockIdField.isFocused()) {
            actionPerformed(this.removeButton);
            return true;
        }
        if (keyCode == Keyboard.KEY_ESCAPE) {
            this.mc.displayGuiScreen(this.parentScreen);
            return true;
        }
        return false;
    }

    private boolean handleBlockInputKeyTyped(char typedChar, int keyCode) {
        if (this.blockIdField == null || !this.blockIdField.isFocused()) {
            return false;
        }

        syncInternalTextFieldState();
        if (this.blockIdField.textboxKeyTyped(typedChar, keyCode)) {
            syncCustomInputStateFromField();
            return true;
        }

        if (GuiScreen.isCtrlKeyDown()) {
            if (keyCode == Keyboard.KEY_V) {
                String clip = GuiScreen.getClipboardString();
                if (clip != null && !clip.isEmpty()) {
                    insertBlockInputText(clip);
                }
                syncInternalTextFieldState();
                return true;
            }
            if (keyCode == Keyboard.KEY_C) {
                GuiScreen.setClipboardString(this.blockIdInputText);
                return true;
            }
            if (keyCode == Keyboard.KEY_A) {
                this.blockIdCursor = this.blockIdInputText.length();
                syncInternalTextFieldState();
                return true;
            }
        }

        switch (keyCode) {
        case Keyboard.KEY_BACK:
            if (this.blockIdCursor > 0 && !this.blockIdInputText.isEmpty()) {
                this.blockIdInputText = this.blockIdInputText.substring(0, this.blockIdCursor - 1)
                        + this.blockIdInputText.substring(this.blockIdCursor);
                this.blockIdCursor--;
            }
            syncInternalTextFieldState();
            return true;
        case Keyboard.KEY_DELETE:
            if (this.blockIdCursor < this.blockIdInputText.length()) {
                this.blockIdInputText = this.blockIdInputText.substring(0, this.blockIdCursor)
                        + this.blockIdInputText.substring(this.blockIdCursor + 1);
            }
            syncInternalTextFieldState();
            return true;
        case Keyboard.KEY_LEFT:
            this.blockIdCursor = Math.max(0, this.blockIdCursor - 1);
            syncInternalTextFieldState();
            return true;
        case Keyboard.KEY_RIGHT:
            this.blockIdCursor = Math.min(this.blockIdInputText.length(), this.blockIdCursor + 1);
            syncInternalTextFieldState();
            return true;
        case Keyboard.KEY_HOME:
            this.blockIdCursor = 0;
            syncInternalTextFieldState();
            return true;
        case Keyboard.KEY_END:
            this.blockIdCursor = this.blockIdInputText.length();
            syncInternalTextFieldState();
            return true;
        default:
            break;
        }

        if (!GuiScreen.isCtrlKeyDown() && !GuiScreen.isAltKeyDown()
                && typedChar >= 32 && typedChar != 127) {
            insertBlockInputText(String.valueOf(typedChar));
            syncInternalTextFieldState();
            return true;
        }
        return false;
    }

    private void insertBlockInputText(String text) {
        if (text == null || text.isEmpty()) {
            return;
        }
        String safe = text.replace("\r", "").replace("\n", "");
        if (safe.isEmpty()) {
            return;
        }
        this.blockIdInputText = this.blockIdInputText.substring(0, this.blockIdCursor)
                + safe
                + this.blockIdInputText.substring(this.blockIdCursor);
        this.blockIdCursor += safe.length();
    }

    private void clearBlockInput() {
        this.blockIdInputText = "";
        this.blockIdCursor = 0;
        syncInternalTextFieldState();
    }

    private int resolveCursorFromMouse(int mouseX) {
        String text = this.blockIdInputText == null ? "" : this.blockIdInputText;
        int localX = mouseX - (this.blockIdField.x + 4);
        if (localX <= 0) {
            return 0;
        }
        for (int i = 1; i <= text.length(); i++) {
            if (this.fontRenderer.getStringWidth(text.substring(0, i)) >= localX) {
                return i - 1;
            }
        }
        return text.length();
    }

    private void syncInternalTextFieldState() {
        if (this.blockIdField == null) {
            return;
        }
        String safe = this.blockIdInputText == null ? "" : this.blockIdInputText;
        if (!safe.equals(this.blockIdField.getText())) {
            this.blockIdField.setText(safe);
        }
        this.blockIdCursor = Math.max(0, Math.min(this.blockIdCursor, safe.length()));
        this.blockIdField.setCursorPosition(this.blockIdCursor);
    }

    private void syncCustomInputStateFromField() {
        if (this.blockIdField == null) {
            return;
        }
        this.blockIdInputText = this.blockIdField.getText() == null ? "" : this.blockIdField.getText();
        this.blockIdCursor = Math.max(0, Math.min(this.blockIdField.getCursorPosition(), this.blockIdInputText.length()));
    }

    private void drawBlockPreview(int x, int y) {
        drawRect(x - 2, y - 2, x + 18, y + 18, 0xFF31475D);
        drawRect(x - 1, y - 1, x + 17, y + 17, 0xFF16212D);
        ItemStack stack = getBlockStack(this.blockToAdd);
        if (stack.isEmpty()) {
            return;
        }

        RenderHelper.enableGUIStandardItemLighting();
        this.itemRender.renderItemAndEffectIntoGUI(stack, x, y);
        this.itemRender.renderItemOverlayIntoGUI(this.fontRenderer, stack, x, y, null);
        RenderHelper.disableStandardItemLighting();
        GlStateManager.disableLighting();
        GlStateManager.disableDepth();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
    }

    private static ItemStack getBlockStack(Block block) {
        if (block == null || block == Blocks.AIR) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = new ItemStack(block);
        return stack == null ? ItemStack.EMPTY : stack;
    }

    private static String getDisplayName(String blockId) {
        Block block = RenderFeatureManager.resolveBlock(blockId);
        ItemStack stack = getBlockStack(block);
        if (!stack.isEmpty() && stack.getDisplayName() != null && !stack.getDisplayName().trim().isEmpty()) {
            return stack.getDisplayName();
        }
        if (block != null) {
            String localized = block.getLocalizedName();
            if (localized != null && !localized.trim().isEmpty()) {
                return localized;
            }
        }
        return blockId == null ? "" : blockId;
    }

    private static final class XrayBlockList extends GuiScrollingList {

        private final Minecraft mc;
        private final GuiXrayBlockEditor owner;
        private final List<String> entries = new ArrayList<>();
        private int selectedIndex = -1;

        private XrayBlockList(Minecraft mc, GuiXrayBlockEditor owner) {
            super(mc, owner.panelWidth - 24, owner.panelHeight, owner.listTop, owner.listBottom,
                    owner.panelX + 12, 22, owner.width, owner.height);
            this.mc = mc;
            this.owner = owner;
        }

        private void reload() {
            String selectedId = getSelectedBlockId();
            this.entries.clear();
            this.entries.addAll(RenderFeatureManager.getXrayVisibleBlockIds());
            if (selectedId != null) {
                this.selectedIndex = this.entries.indexOf(selectedId);
            }
            if (this.selectedIndex < 0 || this.selectedIndex >= this.entries.size()) {
                this.selectedIndex = this.entries.isEmpty() ? -1 : 0;
            }
            this.owner.updateInputPreview();
        }

        private String getSelectedBlockId() {
            return this.selectedIndex >= 0 && this.selectedIndex < this.entries.size()
                    ? this.entries.get(this.selectedIndex)
                    : null;
        }

        @Override
        protected int getSize() {
            return this.entries.size();
        }

        @Override
        protected void elementClicked(int index, boolean doubleClick) {
            if (index >= 0 && index < this.entries.size()) {
                this.selectedIndex = index;
                this.owner.updateInputPreview();
                if (doubleClick) {
                    RenderFeatureManager.setXrayBlockVisible(this.entries.get(index), false);
                    reload();
                }
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
        protected void drawSlot(int slotIdx, int entryRight, int slotTop, int slotBuffer, net.minecraft.client.renderer.Tessellator tess) {
            if (slotIdx < 0 || slotIdx >= this.entries.size()) {
                return;
            }

            String blockId = this.entries.get(slotIdx);
            Block block = RenderFeatureManager.resolveBlock(blockId);
            ItemStack stack = getBlockStack(block);
            String displayName = getDisplayName(blockId);

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
            fr.drawString(displayName, this.left + 28, slotTop + 2, 0xFFEAF6FF);
            fr.drawString("§8" + blockId, this.left + 28, slotTop + 12, 0xFF9FB2C8);
        }
    }
}

