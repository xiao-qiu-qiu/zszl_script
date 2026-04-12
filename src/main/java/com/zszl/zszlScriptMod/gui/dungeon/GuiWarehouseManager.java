package com.zszl.zszlScriptMod.gui.dungeon;

import com.zszl.zszlScriptMod.gui.components.ThemedButton;
import com.zszl.zszlScriptMod.gui.config.AbstractThreePaneRuleManager;
import com.zszl.zszlScriptMod.handlers.WarehouseEventHandler;
import com.zszl.zszlScriptMod.handlers.WarehouseManager;
import com.zszl.zszlScriptMod.system.dungeon.Warehouse;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.resources.I18n;

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class GuiWarehouseManager extends AbstractThreePaneRuleManager<Warehouse> {

    private static final int BTN_GET_POINT1 = 2301;
    private static final int BTN_GET_POINT2 = 2302;
    private static final int BTN_TOGGLE_ACTIVE = 2303;
    private static final int BTN_SCAN_CHESTS = 2304;
    private static final int BTN_INFO = 2305;
    private static final int BTN_DEPOSIT_MODE = 2306;
    private static final int BTN_AUTO_DEPOSIT = 2307;

    private static final DecimalFormat COORD_FORMAT = new DecimalFormat("#.##");

    private GuiTextField nameField;
    private GuiTextField categoryField;
    private GuiTextField x1Field;
    private GuiTextField z1Field;
    private GuiTextField x2Field;
    private GuiTextField z2Field;

    private GuiButton btnGetPoint1;
    private GuiButton btnGetPoint2;
    private GuiButton btnToggleActive;
    private GuiButton btnScanChests;
    private GuiButton btnInfo;
    private GuiButton btnDepositMode;
    private GuiButton btnAutoDeposit;

    private Warehouse workingWarehouse;
    private boolean editorActive = false;

    public GuiWarehouseManager(GuiScreen parent) {
        super(parent);
    }

    @Override
    protected String getScreenTitle() {
        return I18n.format("gui.warehouse.manager.title");
    }

    @Override
    protected String getGuideText() {
        return "§7使用指南：左键筛选/折叠分组，右键分组或卡片打开菜单，右侧可编辑区域并管理扫描/存入功能";
    }

    @Override
    protected String getEntityDisplayName() {
        return "仓库";
    }

    @Override
    protected String getAllItemsLabel() {
        return "全部仓库";
    }

    @Override
    protected String getEmptyTreeText() {
        return I18n.format("gui.warehouse.manager.empty");
    }

    @Override
    protected String getEmptyListText() {
        return "该分组下暂无仓库";
    }

    @Override
    protected List<Warehouse> getSourceItems() {
        return WarehouseManager.warehouses;
    }

    @Override
    protected List<String> getSourceCategories() {
        return WarehouseManager.getCategoriesSnapshot();
    }

    @Override
    protected boolean addCategoryToSource(String category) {
        return WarehouseManager.addCategory(category);
    }

    @Override
    protected boolean renameCategoryInSource(String oldCategory, String newCategory) {
        return WarehouseManager.renameCategory(oldCategory, newCategory);
    }

    @Override
    protected boolean deleteCategoryInSource(String category) {
        return WarehouseManager.deleteCategory(category);
    }

    @Override
    protected void persistChanges() {
        WarehouseManager.saveWarehouses();
    }

    @Override
    protected void reloadSource() {
        WarehouseManager.loadWarehouses();
    }

    @Override
    protected Warehouse createNewItem() {
        Warehouse warehouse = new Warehouse();
        warehouse.name = I18n.format("gui.warehouse.edit.new_name");
        warehouse.category = CATEGORY_DEFAULT;
        warehouse.chests = new CopyOnWriteArrayList<>();
        warehouse.updateBounds();
        return warehouse;
    }

    @Override
    protected Warehouse copyItem(Warehouse source) {
        Warehouse copy = new Warehouse();
        if (source == null) {
            copy.name = I18n.format("gui.warehouse.edit.new_name");
            copy.category = CATEGORY_DEFAULT;
            copy.chests = new CopyOnWriteArrayList<>();
            copy.updateBounds();
            return copy;
        }
        copy.name = source.name;
        copy.category = normalizeCategory(source.category);
        copy.isActive = source.isActive;
        copy.x1 = source.x1;
        copy.z1 = source.z1;
        copy.x2 = source.x2;
        copy.z2 = source.z2;
        copy.chests = source.chests == null ? new CopyOnWriteArrayList<>() : new CopyOnWriteArrayList<>(source.chests);
        copy.updateBounds();
        return copy;
    }

    @Override
    protected void addItemToSource(Warehouse item) {
        WarehouseManager.warehouses.add(item);
    }

    @Override
    protected void removeItemFromSource(Warehouse item) {
        WarehouseManager.warehouses.remove(item);
    }

    @Override
    protected String getItemName(Warehouse item) {
        return item == null ? "" : item.name;
    }

    @Override
    protected void setItemName(Warehouse item, String name) {
        if (item != null) {
            item.name = name;
        }
    }

    @Override
    protected String getItemCategory(Warehouse item) {
        return item == null ? CATEGORY_DEFAULT : item.category;
    }

    @Override
    protected void setItemCategory(Warehouse item, String category) {
        if (item != null) {
            item.category = normalizeCategory(category);
        }
    }

    @Override
    protected void loadEditor(Warehouse item) {
        workingWarehouse = copyItem(item == null ? createNewItem() : item);

        setText(nameField, safe(workingWarehouse.name));
        setText(categoryField, normalizeCategory(workingWarehouse.category));
        setText(x1Field, formatDouble(workingWarehouse.x1));
        setText(z1Field, formatDouble(workingWarehouse.z1));
        setText(x2Field, formatDouble(workingWarehouse.x2));
        setText(z2Field, formatDouble(workingWarehouse.z2));

        editorActive = workingWarehouse.isActive;
        layoutAllWidgets();
    }

    @Override
    protected Warehouse buildItemFromEditor(boolean creatingNew, Warehouse selectedItem) {
        syncFieldsToWorkingWarehouse();
        return copyItem(workingWarehouse);
    }

    @Override
    protected void applyItemValues(Warehouse target, Warehouse source) {
        if (target == null || source == null) {
            return;
        }
        target.name = source.name;
        target.category = normalizeCategory(source.category);
        target.isActive = source.isActive;
        target.x1 = source.x1;
        target.z1 = source.z1;
        target.x2 = source.x2;
        target.z2 = source.z2;
        target.chests = source.chests == null ? new CopyOnWriteArrayList<>() : new CopyOnWriteArrayList<>(source.chests);
        target.updateBounds();
    }

    @Override
    protected void initEditorWidgets() {
        nameField = createField(3301);
        categoryField = createField(3302);
        x1Field = createField(3303);
        z1Field = createField(3304);
        x2Field = createField(3305);
        z2Field = createField(3306);
    }

    @Override
    protected void initAdditionalButtons() {
        btnGetPoint1 = new ThemedButton(BTN_GET_POINT1, 0, 0, 100, 20, I18n.format("gui.warehouse.edit.get_point1"));
        btnGetPoint2 = new ThemedButton(BTN_GET_POINT2, 0, 0, 100, 20, I18n.format("gui.warehouse.edit.get_point2"));
        btnToggleActive = new ThemedButton(BTN_TOGGLE_ACTIVE, 0, 0, 100, 20, "");
        btnScanChests = new ThemedButton(BTN_SCAN_CHESTS, 0, 0, 100, 20, I18n.format("gui.warehouse.edit.scan_chests"));
        btnInfo = new ThemedButton(BTN_INFO, 0, 0, 100, 20, I18n.format("gui.warehouse.manager.info"));
        btnDepositMode = new ThemedButton(BTN_DEPOSIT_MODE, 0, 0, 100, 20, "");
        btnAutoDeposit = new ThemedButton(BTN_AUTO_DEPOSIT, 0, 0, 100, 20, "自动存入");

        this.buttonList.add(btnGetPoint1);
        this.buttonList.add(btnGetPoint2);
        this.buttonList.add(btnToggleActive);
        this.buttonList.add(btnScanChests);
        this.buttonList.add(btnInfo);
        this.buttonList.add(btnDepositMode);
        this.buttonList.add(btnAutoDeposit);
    }

    @Override
    protected void layoutEditorWidgets() {
        int right = editorX + editorWidth - 14;
        int fullFieldWidth = Math.max(120, right - editorFieldX);
        int halfWidth = Math.max(70, (fullFieldWidth - 10) / 2);

        placeField(nameField, 0, editorFieldX, fullFieldWidth);
        placeField(categoryField, 1, editorFieldX, fullFieldWidth);

        placeField(x1Field, 2, editorFieldX, halfWidth);
        placeField(z1Field, 2, editorFieldX + halfWidth + 10, halfWidth);
        placeButton(btnGetPoint1, 3, editorFieldX, fullFieldWidth, 20);

        placeField(x2Field, 4, editorFieldX, halfWidth);
        placeField(z2Field, 4, editorFieldX + halfWidth + 10, halfWidth);
        placeButton(btnGetPoint2, 5, editorFieldX, fullFieldWidth, 20);

        placeButton(btnToggleActive, 6, editorFieldX, fullFieldWidth, 20);

        placeButton(btnScanChests, 7, editorFieldX, halfWidth, 20);
        placeButton(btnInfo, 7, editorFieldX + halfWidth + 10, halfWidth, 20);

        placeButton(btnDepositMode, 8, editorFieldX, fullFieldWidth, 20);
        placeButton(btnAutoDeposit, 9, editorFieldX, fullFieldWidth, 20);
    }

    @Override
    protected void layoutAdditionalButtons() {
        updateEditorButtonStates();
    }

    @Override
    protected int getEditorTotalRows() {
        return 11;
    }

    @Override
    protected String getEditorRowLabel(int row) {
        switch (row) {
            case 0:
                return I18n.format("gui.warehouse.edit.name");
            case 1:
                return "所属分组";
            case 2:
                return I18n.format("gui.warehouse.edit.point1");
            case 3:
                return "获取点1";
            case 4:
                return I18n.format("gui.warehouse.edit.point2");
            case 5:
                return "获取点2";
            case 6:
                return "激活状态";
            case 7:
                return "扫描 / 信息";
            case 8:
                return I18n.format("gui.warehouse.manager.deposit_mode",
                        WarehouseEventHandler.oneClickDepositMode
                                ? I18n.format("gui.common.enabled")
                                : I18n.format("gui.common.disabled"));
            case 9:
                return "自动存入";
            case 10:
                return "概要";
            default:
                return "";
        }
    }

    @Override
    protected List<GuiTextField> getEditorFields() {
        List<GuiTextField> fields = new ArrayList<>();
        fields.add(nameField);
        fields.add(categoryField);
        fields.add(x1Field);
        fields.add(z1Field);
        fields.add(x2Field);
        fields.add(z2Field);
        return fields;
    }

    @Override
    protected void drawEditorContents(int mouseX, int mouseY, float partialTicks) {
        drawEditorFields();
        drawWarehouseSummary();
    }

    @Override
    protected void drawCard(Warehouse item, int actualIndex, int x, int y, int width, int height,
            boolean selected, boolean hovered) {
        int cardBottom = y + height;
        int bg = selected ? 0xAA255D8A : (hovered ? 0xAA2E4258 : 0x99222222);
        int border = selected ? 0xFF5FB8FF : (hovered ? 0xFF7EC8FF : 0xFF4B4B4B);

        drawRect(x, y, x + width, cardBottom, bg);
        drawHorizontalLine(x, x + width, y, border);
        drawHorizontalLine(x, x + width, cardBottom, border);
        drawVerticalLine(x, y, cardBottom, border);
        drawVerticalLine(x + width, y, cardBottom, border);

        String status = item.isActive ? "§a✔" : "§c✘";
        drawString(fontRenderer, trimToWidth(status + " " + safe(item.name), width - 12),
                x + 6, y + 5, 0xFFFFFFFF);
        drawString(fontRenderer, trimToWidth("分类: " + normalizeCategory(item.category), width - 12),
                x + 6, y + 18, 0xFFDDDDDD);
        drawString(fontRenderer,
                trimToWidth("区域: (" + formatDouble(item.x1) + ", " + formatDouble(item.z1) + ") ~ ("
                                + formatDouble(item.x2) + ", " + formatDouble(item.z2) + ")",
                        width - 12),
                x + 6, y + 31, 0xFFBDBDBD);
        int chestCount = item.chests == null ? 0 : item.chests.size();
        drawString(fontRenderer, trimToWidth("箱子记录: " + chestCount, width - 12),
                x + 6, y + 44, 0xFFB8C7D9);
    }

    @Override
    protected String validateItem(Warehouse item) {
        if (item == null) {
            return "仓库为空";
        }
        if (isBlank(item.name)) {
            return "仓库名称不能为空";
        }
        return null;
    }

    @Override
    protected void updateEditorButtonStates() {
        if (btnToggleActive != null) {
            btnToggleActive.displayString = editorActive
                    ? I18n.format("gui.warehouse.manager.deactivate")
                    : I18n.format("gui.warehouse.manager.activate");
            btnToggleActive.enabled = btnToggleActive.visible;
        }
        if (btnGetPoint1 != null) {
            btnGetPoint1.enabled = btnGetPoint1.visible;
        }
        if (btnGetPoint2 != null) {
            btnGetPoint2.enabled = btnGetPoint2.visible;
        }
        if (btnScanChests != null) {
            btnScanChests.enabled = btnScanChests.visible && !creatingNew;
        }
        if (btnInfo != null) {
            btnInfo.enabled = btnInfo.visible && getSelectedItemOrNull() != null && !creatingNew;
        }
        if (btnDepositMode != null) {
            btnDepositMode.displayString = I18n.format("gui.warehouse.manager.deposit_mode",
                    WarehouseEventHandler.oneClickDepositMode
                            ? I18n.format("gui.common.enabled")
                            : I18n.format("gui.common.disabled"));
            btnDepositMode.enabled = btnDepositMode.visible;
        }
        if (btnAutoDeposit != null) {
            btnAutoDeposit.enabled = btnAutoDeposit.visible;
        }
    }

    @Override
    protected boolean handleAdditionalAction(GuiButton button) throws IOException {
        if (button.id == BTN_GET_POINT1) {
            if (mc.player != null) {
                setText(x1Field, formatDouble(mc.player.posX));
                setText(z1Field, formatDouble(mc.player.posZ));
            }
            return true;
        }
        if (button.id == BTN_GET_POINT2) {
            if (mc.player != null) {
                setText(x2Field, formatDouble(mc.player.posX));
                setText(z2Field, formatDouble(mc.player.posZ));
            }
            return true;
        }
        if (button.id == BTN_TOGGLE_ACTIVE) {
            editorActive = !editorActive;
            updateEditorButtonStates();
            return true;
        }
        if (button.id == BTN_INFO) {
            Warehouse selected = getSelectedItemOrNull();
            if (selected != null && !creatingNew) {
                mc.displayGuiScreen(new GuiWarehouseInfo(this, selected));
            }
            return true;
        }
        if (button.id == BTN_SCAN_CHESTS) {
            if (creatingNew) {
                setStatus("§c请先保存仓库后再扫描箱子", 0xFFFF8E8E);
                return true;
            }
            Warehouse selected = getSelectedItemOrNull();
            if (selected == null) {
                setStatus("§c请先选择一个仓库", 0xFFFF8E8E);
                return true;
            }
            syncFieldsToWorkingWarehouse();
            applyItemValues(selected, workingWarehouse);
            WarehouseManager.scanForChestsInWarehouse(selected);
            persistChanges();
            refreshData(true);
            selectByItemName(selected.name);
            setStatus("§a已扫描仓库内箱子", 0xFF8CFF9E);
            return true;
        }
        if (button.id == BTN_DEPOSIT_MODE) {
            WarehouseEventHandler.oneClickDepositMode = !WarehouseEventHandler.oneClickDepositMode;
            updateEditorButtonStates();
            return true;
        }
        if (button.id == BTN_AUTO_DEPOSIT) {
            WarehouseEventHandler.startAutoDepositByHighlights();
            setStatus("§a已触发自动存入", 0xFF8CFF9E);
            return true;
        }
        return false;
    }

    @Override
    protected void afterItemSaved(Warehouse item, boolean createdNow) {
        if (item != null && item.isActive) {
            for (Warehouse warehouse : WarehouseManager.warehouses) {
                if (warehouse != null && warehouse != item) {
                    warehouse.isActive = false;
                }
            }
        }
        persistChanges();
    }

    private void syncFieldsToWorkingWarehouse() {
        if (workingWarehouse == null) {
            workingWarehouse = createNewItem();
        }
        workingWarehouse.name = safe(nameField.getText()).trim();
        workingWarehouse.category = normalizeCategory(categoryField.getText());
        workingWarehouse.isActive = editorActive;
        workingWarehouse.x1 = parseDouble(x1Field.getText(), workingWarehouse.x1);
        workingWarehouse.z1 = parseDouble(z1Field.getText(), workingWarehouse.z1);
        workingWarehouse.x2 = parseDouble(x2Field.getText(), workingWarehouse.x2);
        workingWarehouse.z2 = parseDouble(z2Field.getText(), workingWarehouse.z2);
        if (workingWarehouse.chests == null) {
            workingWarehouse.chests = new CopyOnWriteArrayList<>();
        }
        workingWarehouse.updateBounds();
    }

    private void drawWarehouseSummary() {
        int row = 10;
        int y = getEditorRowY(row);
        if (y < 0 || workingWarehouse == null) {
            return;
        }

        syncFieldsToWorkingWarehouse();
        int chestCount = workingWarehouse.chests == null ? 0 : workingWarehouse.chests.size();
        String summary = "箱子记录: " + chestCount
                + "  边界: (" + formatDouble(workingWarehouse.x1) + ", " + formatDouble(workingWarehouse.z1)
                + ") ~ (" + formatDouble(workingWarehouse.x2) + ", " + formatDouble(workingWarehouse.z2) + ")";
        drawString(fontRenderer, trimToWidth(summary, editorWidth - getEditorLabelWidth() - 24),
                editorFieldX, y + 4, 0xFFB8C7D9);
    }

    private GuiTextField createField(int id) {
        GuiTextField field = new GuiTextField(id, this.fontRenderer, 0, 0, 100, 16);
        field.setMaxStringLength(Integer.MAX_VALUE);
        field.setEnableBackgroundDrawing(false);
        return field;
    }

    private String formatDouble(double value) {
        return COORD_FORMAT.format(value);
    }

    private double parseDouble(String text, double fallback) {
        try {
            return Double.parseDouble(safe(text).trim().replace(',', '.'));
        } catch (Exception ignored) {
            return fallback;
        }
    }
}