// 文件路径: src/main/java/com/zszl/zszlScriptMod/gui/debug/GuiMemoryManager.java
package com.zszl.zszlScriptMod.gui.debug;

import com.zszl.zszlScriptMod.gui.components.GuiTextInput;
import com.zszl.zszlScriptMod.gui.components.ThemedButton;
import com.zszl.zszlScriptMod.system.MemoryManager;
import com.zszl.zszlScriptMod.system.MemorySnapshot;
import net.minecraft.client.gui.GuiButton;
import com.zszl.zszlScriptMod.gui.components.ThemedGuiScreen;
import net.minecraft.client.resources.I18n;
// --- 核心修复：添加缺失的导入 ---
// --- 修复结束 ---
import org.lwjgl.input.Mouse;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import com.zszl.zszlScriptMod.gui.components.GuiTheme;

public class GuiMemoryManager extends ThemedGuiScreen {

    private List<String> snapshotNames;
    private Set<Integer> selectedIndices = new HashSet<>();
    private int scrollOffset = 0;
    private int maxScroll = 0;

    private GuiButton btnCompare, btnDelete;

    public GuiMemoryManager() {
        this.snapshotNames = new ArrayList<>(MemoryManager.snapshots.keySet());
    }

    @Override
    public void initGui() {
        this.buttonList.clear();
        int panelWidth = 400;
        int panelX = (this.width - panelWidth) / 2;
        int panelY = (this.height - 240) / 2;

        int buttonWidth = (panelWidth - 50) / 4;
        int buttonY = panelY + 185;
        this.buttonList.add(new ThemedButton(0, panelX + 10, buttonY, buttonWidth, 20,
                I18n.format("gui.memory.manager.create_snapshot")));
        btnCompare = new ThemedButton(1, panelX + 20 + buttonWidth, buttonY, buttonWidth, 20,
                I18n.format("gui.memory.manager.compare_selected"));
        btnDelete = new ThemedButton(2, panelX + 30 + 2 * buttonWidth, buttonY, buttonWidth, 20,
                I18n.format("gui.memory.manager.delete_selected"));
        this.buttonList.add(new ThemedButton(3, panelX + 40 + 3 * buttonWidth, buttonY, buttonWidth, 20,
                I18n.format("gui.memory.manager.clear_all")));
        this.buttonList.add(btnCompare);
        this.buttonList.add(btnDelete);

        int bottomButtonY = panelY + 215;
        // --- 核心修改：将 GC 按钮改为打开新工具界面的按钮 ---
        this.buttonList.add(new ThemedButton(4, panelX + 10, bottomButtonY, (panelWidth - 30) / 2, 20,
                I18n.format("gui.memory.manager.advanced_tools")));
        // --- 修改结束 ---
        this.buttonList.add(
                new ThemedButton(5, panelX + 20 + (panelWidth - 30) / 2, bottomButtonY, (panelWidth - 30) / 2, 20,
                        I18n.format("gui.common.done")));

        updateButtonStates();
    }

    private void updateButtonStates() {
        btnCompare.enabled = selectedIndices.size() == 2;
        btnDelete.enabled = !selectedIndices.isEmpty();
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        switch (button.id) {
            case 0: // 创建快照
                String defaultName = I18n.format("gui.memory.manager.snapshot_prefix")
                        + new SimpleDateFormat("HH-mm-ss").format(new Date());
                mc.displayGuiScreen(new GuiTextInput(this, I18n.format("gui.memory.manager.input_snapshot_name"),
                        defaultName, name -> {
                            if (name != null && !name.trim().isEmpty()) {
                                MemoryManager.takeSnapshot(name.trim());
                                this.snapshotNames = new ArrayList<>(MemoryManager.snapshots.keySet());
                                selectedIndices.clear();
                            }
                            mc.displayGuiScreen(this);
                        }));
                break;
            case 1: // 对比
                if (selectedIndices.size() == 2) {
                    List<Integer> indices = new ArrayList<>(selectedIndices);
                    MemorySnapshot before = MemoryManager.snapshots.get(snapshotNames.get(indices.get(0)));
                    MemorySnapshot after = MemoryManager.snapshots.get(snapshotNames.get(indices.get(1)));
                    // 确保时间早的在前面
                    if (before.timestamp > after.timestamp) {
                        MemorySnapshot temp = before;
                        before = after;
                        after = temp;
                    }
                    mc.displayGuiScreen(new GuiMemoryComparison(this, MemoryManager.compare(before, after)));
                }
                break;
            case 2: // 删除
                List<String> toDelete = new ArrayList<>();
                for (int index : selectedIndices) {
                    toDelete.add(snapshotNames.get(index));
                }
                toDelete.forEach(MemoryManager::deleteSnapshot);
                this.snapshotNames = new ArrayList<>(MemoryManager.snapshots.keySet());
                selectedIndices.clear();
                updateButtonStates();
                break;
            case 3: // 清空
                MemoryManager.clearSnapshots();
                this.snapshotNames.clear();
                selectedIndices.clear();
                updateButtonStates();
                break;
            case 4: // 高级内存工具
                mc.displayGuiScreen(new GuiMemoryTools(this));
                break;
            case 5: // 完成
                mc.displayGuiScreen(null);
                break;
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.drawDefaultBackground();
        int panelWidth = 400;
        int panelHeight = 240;
        int panelX = (this.width - panelWidth) / 2;
        int panelY = (this.height - panelHeight) / 2;

        GuiTheme.drawPanel(panelX, panelY, panelWidth, panelHeight);
        GuiTheme.drawTitleBar(panelX, panelY, panelWidth, I18n.format("gui.memory.manager.title"),
                this.fontRenderer);

        int listY = panelY + 40;
        int listHeight = 140;
        int itemHeight = 20;
        int visibleItems = listHeight / itemHeight;
        maxScroll = Math.max(0, snapshotNames.size() - visibleItems);

        for (int i = 0; i < visibleItems; i++) {
            int index = i + scrollOffset;
            if (index >= snapshotNames.size())
                break;

            String name = snapshotNames.get(index);
            MemorySnapshot snapshot = MemoryManager.snapshots.get(name);
            int itemY = listY + i * itemHeight;

            int bgColor = selectedIndices.contains(index) ? 0xFF0066AA : 0xFF444444;
            boolean isHovered = mouseX >= panelX + 10 && mouseX <= panelX + panelWidth - 10 && mouseY >= itemY
                    && mouseY <= itemY + itemHeight;
            if (isHovered && !selectedIndices.contains(index))
                bgColor = 0xFF666666;

            drawRect(panelX + 10, itemY, panelX + panelWidth - 10, itemY + itemHeight, bgColor);

            String info = String.format("§f%s §7(%s, %d MB)", snapshot.name, snapshot.getFormattedTimestamp(),
                    snapshot.usedMemory / 1024 / 1024);
            this.drawString(fontRenderer, info, panelX + 15, itemY + (itemHeight - 8) / 2, 0xFFFFFF);
        }

        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        int panelWidth = 400;
        int panelX = (this.width - panelWidth) / 2;
        int listY = (this.height - 240) / 2 + 40;
        int listHeight = 140;
        int itemHeight = 20;

        if (mouseX >= panelX + 10 && mouseX <= panelX + panelWidth - 10 && mouseY >= listY
                && mouseY <= listY + listHeight) {
            int clickedIndex = (mouseY - listY) / itemHeight + scrollOffset;
            if (clickedIndex >= 0 && clickedIndex < snapshotNames.size()) {
                if (isCtrlKeyDown()) {
                    if (selectedIndices.contains(clickedIndex)) {
                        selectedIndices.remove(clickedIndex);
                    } else {
                        selectedIndices.add(clickedIndex);
                    }
                } else {
                    selectedIndices.clear();
                    selectedIndices.add(clickedIndex);
                }
                updateButtonStates();
            }
        }
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        int dWheel = Mouse.getEventDWheel();
        if (dWheel != 0) {
            if (dWheel > 0)
                scrollOffset = Math.max(0, scrollOffset - 1);
            else
                scrollOffset = Math.min(maxScroll, scrollOffset + 1);
        }
    }
}

