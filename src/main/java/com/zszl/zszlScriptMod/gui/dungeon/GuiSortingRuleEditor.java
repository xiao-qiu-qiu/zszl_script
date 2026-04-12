// 文件路径: src/main/java/com/zszl/zszlScriptMod/gui/dungeon/GuiSortingRuleEditor.java
package com.zszl.zszlScriptMod.gui.dungeon;

import com.zszl.zszlScriptMod.gui.components.ToggleGuiButton;
import com.zszl.zszlScriptMod.gui.components.GuiTheme;
import com.zszl.zszlScriptMod.system.dungeon.SortingRule;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import com.zszl.zszlScriptMod.gui.components.ThemedGuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.resources.I18n;
import org.lwjgl.input.Keyboard;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class GuiSortingRuleEditor extends ThemedGuiScreen {

    private final GuiScreen parentScreen;
    private final SortingRule rule;
    private final Consumer<SortingRule> onSave;
    private final boolean isNew;

    // !! 核心修改：UI控件重构 !!
    private GuiTextField nameField, keywordsField;
    private ToggleGuiButton matchAnyButton, shulkerOnlyButton, nonShulkerOnlyButton;
    private Set<Integer> selectedSlots = new HashSet<>();

    private boolean isDragging = false;
    private int dragStartSlot = -1;

    public GuiSortingRuleEditor(GuiScreen parent, SortingRule rule, Consumer<SortingRule> onSave) {
        this.parentScreen = parent;
        this.onSave = onSave;
        this.isNew = (rule == null);
        this.rule = isNew ? new SortingRule() : rule;
        if (!isNew) {
            if (this.rule.targetSlots != null) {
                this.selectedSlots.addAll(this.rule.targetSlots);
            }
        }
    }

    @Override
    public void initGui() {
        Keyboard.enableRepeatEvents(true);
        this.buttonList.clear();
        int centerX = this.width / 2;
        int panelWidth = 380;
        int panelX = centerX - panelWidth / 2;
        int panelY = this.height / 2 - 140;

        int currentY = panelY + 20;
        nameField = new GuiTextField(0, fontRenderer, panelX + 80, currentY, 280, 20);
        nameField.setText(rule.name);
        currentY += 25;

        keywordsField = new GuiTextField(1, fontRenderer, panelX + 80, currentY, 280, 20);
        if (rule.itemKeywords != null) {
            keywordsField.setText(String.join(", ", rule.itemKeywords));
        }
        currentY += 25;

        // !! 核心修改：使用 ToggleGuiButton 代替旧的按钮 !!
        int halfWidth = (280 - 10) / 2;
        matchAnyButton = new ToggleGuiButton(2, panelX + 80, currentY, halfWidth, 20,
                I18n.format("gui.warehouse.sort.match_any"),
                rule.matchMode == SortingRule.MatchMode.ANY);
        this.buttonList.add(matchAnyButton);

        currentY += 25;
        int thirdWidth = (280 - 10) / 2;
        shulkerOnlyButton = new ToggleGuiButton(3, panelX + 80, currentY, thirdWidth, 20,
                I18n.format("gui.warehouse.sort.shulker_only"),
                rule.itemType == SortingRule.ItemType.SHULKER_ONLY);
        nonShulkerOnlyButton = new ToggleGuiButton(4, panelX + 80 + thirdWidth + 5, currentY, thirdWidth, 20,
                I18n.format("gui.warehouse.sort.non_shulker_only"),
                rule.itemType == SortingRule.ItemType.NON_SHULKER_ONLY);
        this.buttonList.add(shulkerOnlyButton);
        this.buttonList.add(nonShulkerOnlyButton);

        this.buttonList.add(new GuiButton(100, centerX - 100, panelY + 255, 95, 20, I18n.format("gui.common.save")));
        this.buttonList.add(new GuiButton(101, centerX + 5, panelY + 255, 95, 20, I18n.format("gui.common.cancel")));
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        // !! 核心修改：处理新的 ToggleButton !!
        if (button instanceof ToggleGuiButton) {
            ((ToggleGuiButton) button).setEnabledState(!((ToggleGuiButton) button).getEnabledState());

            if (button.id == 2) { // 匹配模式
                rule.matchMode = matchAnyButton.getEnabledState() ? SortingRule.MatchMode.ANY
                        : SortingRule.MatchMode.ALL;
                matchAnyButton.displayString = matchAnyButton.getEnabledState()
                        ? I18n.format("gui.warehouse.sort.match_any")
                        : I18n.format("gui.warehouse.sort.match_all");
            } else if (button.id == 3) { // 仅潜影盒
                if (shulkerOnlyButton.getEnabledState()) {
                    nonShulkerOnlyButton.setEnabledState(false);
                    rule.itemType = SortingRule.ItemType.SHULKER_ONLY;
                } else {
                    rule.itemType = SortingRule.ItemType.ANY;
                }
            } else if (button.id == 4) { // 仅非潜影盒
                if (nonShulkerOnlyButton.getEnabledState()) {
                    shulkerOnlyButton.setEnabledState(false);
                    rule.itemType = SortingRule.ItemType.NON_SHULKER_ONLY;
                } else {
                    rule.itemType = SortingRule.ItemType.ANY;
                }
            }
        } else {
            switch (button.id) {
                case 100: // Save
                    rule.name = nameField.getText();
                    rule.itemKeywords = Arrays.stream(keywordsField.getText().split(","))
                            .map(String::trim)
                            .filter(s -> !s.isEmpty())
                            .collect(Collectors.toList());
                    rule.targetSlots = new ArrayList<>(selectedSlots);
                    onSave.accept(rule);
                    mc.displayGuiScreen(parentScreen);
                    break;
                case 101: // Cancel
                    mc.displayGuiScreen(parentScreen);
                    break;
            }
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        int centerX = this.width / 2;
        int panelWidth = 380;
        int panelX = centerX - panelWidth / 2;
        int panelY = this.height / 2 - 140;
        int panelHeight = 280;

        GuiTheme.drawPanel(panelX, panelY, panelWidth, panelHeight);
        drawCenteredString(fontRenderer, I18n.format("gui.warehouse.sort.title"), centerX, panelY + 5, 0xFFFFFF);

        drawString(fontRenderer, I18n.format("gui.warehouse.sort.rule_name"), panelX + 10, panelY + 25, 0xFFFFFF);
        drawThemedTextField(nameField);
        drawString(fontRenderer, I18n.format("gui.warehouse.sort.item_match"), panelX + 10, panelY + 50, 0xFFFFFF);
        drawThemedTextField(keywordsField);
        if (keywordsField.getText().isEmpty() && !keywordsField.isFocused()) {
            drawString(fontRenderer, I18n.format("gui.warehouse.sort.keyword_hint"), keywordsField.x + 4,
                    keywordsField.y + 6, 0xFF808080);
        }

        int gridX = centerX - (9 * 18) / 2;
        int gridY = panelY + 105;
        drawString(fontRenderer, I18n.format("gui.warehouse.sort.target_slots"), gridX, gridY - 12, 0xFFFFFF);
        drawChestGrid(gridX, gridY);

        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    private void drawChestGrid(int x, int y) {
        for (int i = 0; i < 54; i++) {
            int row = i / 9;
            int col = i % 9;
            int slotX = x + col * 18;
            int slotY = y + row * 18;

            boolean isSelected = selectedSlots.contains(i);
            drawRect(slotX, slotY, slotX + 17, slotY + 17, isSelected ? 0x8000FF00 : 0x80808080);
            drawCenteredString(fontRenderer, String.valueOf(i), slotX + 8, slotY + 4, 0xFFFFFF);
        }
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        nameField.mouseClicked(mouseX, mouseY, mouseButton);
        keywordsField.mouseClicked(mouseX, mouseY, mouseButton);

        if (mouseButton == 0) {
            int gridX = this.width / 2 - (9 * 18) / 2;
            int gridY = this.height / 2 - 140 + 105;
            int slot = getSlotAt(mouseX, mouseY, gridX, gridY);
            if (slot != -1) {
                isDragging = true;
                dragStartSlot = slot;
                toggleSlot(slot);
            }
        }
    }

    @Override
    protected void mouseClickMove(int mouseX, int mouseY, int clickedMouseButton, long timeSinceLastClick) {
        if (isDragging) {
            int gridX = this.width / 2 - (9 * 18) / 2;
            int gridY = this.height / 2 - 140 + 105;
            int currentSlot = getSlotAt(mouseX, mouseY, gridX, gridY);
            if (currentSlot != -1) {
                int minX = Math.min(dragStartSlot % 9, currentSlot % 9);
                int maxX = Math.max(dragStartSlot % 9, currentSlot % 9);
                int minY = Math.min(dragStartSlot / 9, currentSlot / 9);
                int maxY = Math.max(dragStartSlot / 9, currentSlot / 9);

                selectedSlots.clear();
                for (int y = minY; y <= maxY; y++) {
                    for (int x = minX; x <= maxX; x++) {
                        selectedSlots.add(y * 9 + x);
                    }
                }
            }
        }
    }

    @Override
    protected void mouseReleased(int mouseX, int mouseY, int state) {
        super.mouseReleased(mouseX, mouseY, state);
        if (state == 0) {
            isDragging = false;
            dragStartSlot = -1;
        }
    }

    private void toggleSlot(int slot) {
        if (selectedSlots.contains(slot)) {
            selectedSlots.remove(slot);
        } else {
            selectedSlots.add(slot);
        }
    }

    private int getSlotAt(int mouseX, int mouseY, int gridX, int gridY) {
        for (int i = 0; i < 54; i++) {
            int row = i / 9;
            int col = i % 9;
            int slotX = gridX + col * 18;
            int slotY = gridY + row * 18;
            if (mouseX >= slotX && mouseX < slotX + 18 && mouseY >= slotY && mouseY < slotY + 18) {
                return i;
            }
        }
        return -1;
    }

    // 这个方法不再需要，因为我们用了 ToggleButton
    private String getItemTypeName(SortingRule.ItemType type) {
        switch (type) {
            case ANY:
                return I18n.format("gui.warehouse.sort.item_type.any");
            case SHULKER_ONLY:
                return I18n.format("gui.warehouse.sort.item_type.shulker");
            case NON_SHULKER_ONLY:
                return I18n.format("gui.warehouse.sort.item_type.non_shulker");
            default:
                return I18n.format("gui.warehouse.sort.item_type.unknown");
        }
    }

    @Override
    public void onGuiClosed() {
        Keyboard.enableRepeatEvents(false);
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        super.keyTyped(typedChar, keyCode);
        nameField.textboxKeyTyped(typedChar, keyCode);
        keywordsField.textboxKeyTyped(typedChar, keyCode);
    }
}
