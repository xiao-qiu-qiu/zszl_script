package com.zszl.zszlScriptMod.gui.config;

import com.zszl.zszlScriptMod.gui.components.GuiTheme;
import com.zszl.zszlScriptMod.gui.components.ThemedButton;
import com.zszl.zszlScriptMod.gui.path.GuiActionEditor.model.IndexedHitRegion;
import com.zszl.zszlScriptMod.gui.path.GuiSequenceSelector;
import com.zszl.zszlScriptMod.handlers.AutoPickupHandler;
import com.zszl.zszlScriptMod.handlers.KillAuraHandler;
import com.zszl.zszlScriptMod.path.PathSequenceManager;
import com.zszl.zszlScriptMod.system.AutoPickupRule;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.math.MathHelper;

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

public class GuiAutoPickupConfig extends AbstractThreePaneRuleManager<AutoPickupRule> {

    private static final int BTN_SELECT_SEQUENCE = 2001;
    private static final int BTN_GET_COORDS = 2002;
    private static final int BTN_TOGGLE_ENABLED = 2003;
    private static final int BTN_TOGGLE_STOP_ON_EXIT = 2004;
    private static final int BTN_TOGGLE_VISUALIZE_RANGE = 2005;
    private static final int BTN_TOGGLE_ITEM_WHITELIST = 2006;
    private static final int BTN_TOGGLE_ITEM_BLACKLIST = 2007;
    private static final int BTN_TOGGLE_ANTI_STUCK = 2023;
    private static final int BTN_ADD_WHITELIST_ENTRY = 2008;
    private static final int BTN_EDIT_WHITELIST_ENTRY = 2009;
    private static final int BTN_DELETE_WHITELIST_ENTRY = 2010;
    private static final int BTN_ADD_BLACKLIST_ENTRY = 2011;
    private static final int BTN_EDIT_BLACKLIST_ENTRY = 2012;
    private static final int BTN_DELETE_BLACKLIST_ENTRY = 2013;
    private static final int BTN_WHITELIST_SCROLL_UP = 2014;
    private static final int BTN_WHITELIST_SCROLL_DOWN = 2015;
    private static final int BTN_BLACKLIST_SCROLL_UP = 2016;
    private static final int BTN_BLACKLIST_SCROLL_DOWN = 2017;
    private static final int BTN_ADD_PICKUP_ACTION_ENTRY = 2018;
    private static final int BTN_EDIT_PICKUP_ACTION_ENTRY = 2019;
    private static final int BTN_DELETE_PICKUP_ACTION_ENTRY = 2020;
    private static final int BTN_PICKUP_ACTION_SCROLL_UP = 2021;
    private static final int BTN_PICKUP_ACTION_SCROLL_DOWN = 2022;
    private static final int BTN_SELECT_ANTI_STUCK_SEQUENCE = 2024;
    private static final long CARD_DOUBLE_CLICK_INTERVAL_MS = 300L;

    private static final DecimalFormat COORD_FORMAT = new DecimalFormat("#.##");
    private static final int ENTRY_LIST_BOX_ROWS = 4;
    private static final int ENTRY_CARD_HEIGHT = 20;
    private static final int ENTRY_CARD_GAP = 2;
    private static final int MOUSE_WHEEL_NOTCH = 120;
    private static final int DRAG_LIST_NONE = 0;
    private static final int DRAG_LIST_WHITELIST = 1;
    private static final int DRAG_LIST_BLACKLIST = 2;
    private static final int DRAG_LIST_PICKUP_ACTION = 3;
    private static final int LIST_AUTO_SCROLL_MARGIN = 12;
    private static final int LIST_HINT_LINE_HEIGHT = 10;
    private static final int INVENTORY_GRID_ROWS = AutoPickupRule.INVENTORY_SLOT_ROWS;
    private static final int INVENTORY_GRID_COLS = AutoPickupRule.INVENTORY_SLOT_COLUMNS;
    private static final int INVENTORY_SLOT_COUNT = AutoPickupRule.INVENTORY_SLOT_COUNT;
    private static final int ROW_INVENTORY_DETECTION_TITLE = 10;
    private static final int ROW_INVENTORY_DETECTION_GRID = 11;
    private static final int ROW_WHITELIST_TOGGLE = 15;
    private static final int ROW_WHITELIST_ACTIONS = 16;
    private static final int ROW_WHITELIST_SCROLL = 17;
    private static final int ROW_WHITELIST_BOX = 18;
    private static final int ROW_BLACKLIST_TOGGLE = 22;
    private static final int ROW_BLACKLIST_ACTIONS = 23;
    private static final int ROW_BLACKLIST_SCROLL = 24;
    private static final int ROW_BLACKLIST_BOX = 25;
    private static final int ROW_PICKUP_ACTION_ACTIONS = 29;
    private static final int ROW_PICKUP_ACTION_SCROLL = 30;
    private static final int ROW_PICKUP_ACTION_BOX = 31;
    private static final int ROW_POST_PICKUP_SEQUENCE = 35;
    private static final int ROW_POST_PICKUP_DELAY = 36;
    private static final int ROW_STOP_ON_EXIT = 37;

    private GuiTextField nameField;
    private GuiTextField categoryField;
    private GuiTextField xField;
    private GuiTextField yField;
    private GuiTextField zField;
    private GuiTextField radiusField;
    private GuiTextField reachDistanceField;
    private GuiTextField maxPickupAttemptsField;
    private GuiTextField delayField;
    private GuiTextField antiStuckTimeoutField;

    private GuiButton btnSelectSequence;
    private GuiButton btnGetCoords;
    private GuiButton btnToggleEnabled;
    private GuiButton btnToggleStopOnExit;
    private GuiButton btnToggleVisualizeRange;
    private GuiButton btnToggleAntiStuck;
    private GuiButton btnSelectAntiStuckSequence;
    private GuiButton btnToggleItemWhitelist;
    private GuiButton btnToggleItemBlacklist;
    private GuiButton btnAddWhitelistEntry;
    private GuiButton btnEditWhitelistEntry;
    private GuiButton btnDeleteWhitelistEntry;
    private GuiButton btnAddBlacklistEntry;
    private GuiButton btnEditBlacklistEntry;
    private GuiButton btnDeleteBlacklistEntry;
    private GuiButton btnWhitelistScrollUp;
    private GuiButton btnWhitelistScrollDown;
    private GuiButton btnBlacklistScrollUp;
    private GuiButton btnBlacklistScrollDown;
    private GuiButton btnAddPickupActionEntry;
    private GuiButton btnEditPickupActionEntry;
    private GuiButton btnDeletePickupActionEntry;
    private GuiButton btnPickupActionScrollUp;
    private GuiButton btnPickupActionScrollDown;

    private String editorSequence = "";
    private boolean editorEnabled = true;
    private boolean editorVisualizeRange = false;
    private boolean editorStopOnExit = true;
    private boolean editorAntiStuckEnabled = false;
    private String editorAntiStuckRestartSequence = "";
    private boolean editorEnableItemWhitelist = false;
    private boolean editorEnableItemBlacklist = false;
    private final List<AutoPickupRule.ItemMatchEntry> editorWhitelistEntries = new ArrayList<>();
    private final List<AutoPickupRule.ItemMatchEntry> editorBlacklistEntries = new ArrayList<>();
    private final List<AutoPickupRule.PickupActionEntry> editorPickupActionEntries = new ArrayList<>();
    private final LinkedHashSet<Integer> editorInventoryDetectionSlots = new LinkedHashSet<>();
    private final LinkedHashSet<Integer> inventoryDetectionDragSelectionSnapshot = new LinkedHashSet<>();
    private final List<IndexedHitRegion> inventoryDetectionSlotRegions = new ArrayList<>();
    private int selectedWhitelistEntryIndex = -1;
    private int selectedBlacklistEntryIndex = -1;
    private int selectedPickupActionEntryIndex = -1;
    private int whitelistScrollOffset = 0;
    private int blacklistScrollOffset = 0;
    private int pickupActionScrollOffset = 0;
    private boolean inventoryDetectionGridDragging = false;
    private boolean inventoryDetectionDragAddMode = true;
    private int inventoryDetectionDragAnchorIndex = -1;
    private int inventoryDetectionDragCurrentIndex = -1;
    private EditorStateSnapshot pendingRestoreState = null;
    private int lastCardClickIndex = -1;
    private long lastCardClickAtMs = 0L;
    private final String[] editorSections = new String[] { "基础", "白名单", "黑名单", "拾取执行" };
    private int activeEditorSection = 0;
    private int draggingListType = DRAG_LIST_NONE;
    private int draggingEntryIndex = -1;
    private int draggingMouseX = -1;
    private int draggingMouseY = -1;

    public GuiAutoPickupConfig(GuiScreen parent) {
        super(parent);
    }

    @Override
    public void initGui() {
        EditorStateSnapshot restoreState = pendingRestoreState;
        pendingRestoreState = null;
        super.initGui();
        if (restoreState != null) {
            applyEditorStateSnapshot(restoreState);
        }
    }

    @Override
    protected String getScreenTitle() {
        return I18n.format("gui.autopickup.manager.title");
    }

    @Override
    protected String getGuideText() {
        return "§7使用指南：左键筛选/折叠分组，右键分组或卡片打开菜单，右侧顶部标签可切换编辑分组，双击规则卡片可快速开关";
    }

    @Override
    protected String getEntityDisplayName() {
        return "规则";
    }

    @Override
    protected String getAllItemsLabel() {
        return "全部规则";
    }

    @Override
    protected String getEmptyTreeText() {
        return I18n.format("gui.autopickup.manager.empty");
    }

    @Override
    protected String getEmptyListText() {
        return "该分组下暂无自动拾取规则";
    }

    @Override
    protected List<AutoPickupRule> getSourceItems() {
        return AutoPickupHandler.rules;
    }

    @Override
    protected List<String> getSourceCategories() {
        return AutoPickupHandler.getCategoriesSnapshot();
    }

    @Override
    protected boolean addCategoryToSource(String category) {
        return AutoPickupHandler.addCategory(category);
    }

    @Override
    protected boolean renameCategoryInSource(String oldCategory, String newCategory) {
        return AutoPickupHandler.renameCategory(oldCategory, newCategory);
    }

    @Override
    protected boolean deleteCategoryInSource(String category) {
        return AutoPickupHandler.deleteCategory(category);
    }

    @Override
    protected boolean replaceCategoryOrderInSource(List<String> orderedCategories) {
        AutoPickupHandler.replaceCategoryOrder(orderedCategories);
        return true;
    }

    @Override
    protected void persistChanges() {
        AutoPickupHandler.saveConfig();
    }

    @Override
    protected void reloadSource() {
        AutoPickupHandler.loadConfig();
    }

    @Override
    protected AutoPickupRule createNewItem() {
        return new AutoPickupRule();
    }

    @Override
    protected AutoPickupRule copyItem(AutoPickupRule source) {
        AutoPickupRule copy = new AutoPickupRule();
        if (source == null) {
            return copy;
        }
        copy.name = source.name;
        copy.category = source.category;
        copy.enabled = source.enabled;
        copy.centerX = source.centerX;
        copy.centerY = source.centerY;
        copy.centerZ = source.centerZ;
        copy.radius = source.radius;
        copy.targetReachDistance = source.targetReachDistance;
        copy.maxPickupAttempts = source.maxPickupAttempts;
        copy.visualizeRange = source.visualizeRange;
        copy.enableItemWhitelist = source.enableItemWhitelist;
        copy.enableItemBlacklist = source.enableItemBlacklist;
        copy.itemWhitelistEntries = copyEntryList(source.itemWhitelistEntries, source.itemWhitelist);
        copy.itemBlacklistEntries = copyEntryList(source.itemBlacklistEntries, source.itemBlacklist);
        copy.pickupActionEntries = copyPickupActionEntryList(source.pickupActionEntries);
        copy.inventoryDetectionSlots = copyInventoryDetectionSlots(source.inventoryDetectionSlots);
        syncLegacyKeywordLists(copy);
        copy.postPickupSequence = source.postPickupSequence;
        copy.postPickupDelaySeconds = source.postPickupDelaySeconds;
        copy.stopOnExit = source.stopOnExit;
        copy.antiStuckEnabled = source.antiStuckEnabled;
        copy.antiStuckTimeoutSeconds = source.antiStuckTimeoutSeconds;
        copy.antiStuckRestartSequence = source.antiStuckRestartSequence;
        return copy;
    }

    @Override
    protected void addItemToSource(AutoPickupRule item) {
        AutoPickupHandler.rules.add(item);
    }

    @Override
    protected void removeItemFromSource(AutoPickupRule item) {
        AutoPickupHandler.rules.remove(item);
    }

    @Override
    protected String getItemName(AutoPickupRule item) {
        return item == null ? "" : item.name;
    }

    @Override
    protected void setItemName(AutoPickupRule item, String name) {
        if (item != null) {
            item.name = name;
        }
    }

    @Override
    protected String getItemCategory(AutoPickupRule item) {
        return item == null ? CATEGORY_DEFAULT : item.category;
    }

    @Override
    protected void setItemCategory(AutoPickupRule item, String category) {
        if (item != null) {
            item.category = normalizeCategory(category);
        }
    }

    @Override
    protected void loadEditor(AutoPickupRule item) {
        AutoPickupRule rule = item == null ? new AutoPickupRule() : item;

        setText(nameField, safe(rule.name));
        setText(categoryField, normalizeCategory(rule.category));
        setText(xField, formatDouble(rule.centerX));
        setText(yField, formatDouble(rule.centerY));
        setText(zField, formatDouble(rule.centerZ));
        setText(radiusField, formatDouble(rule.radius));
        setText(reachDistanceField, formatDouble(rule.targetReachDistance));
        setText(maxPickupAttemptsField, String.valueOf(Math.max(1, rule.maxPickupAttempts)));
        setText(delayField, String.valueOf(Math.max(0, rule.postPickupDelaySeconds)));
        setText(antiStuckTimeoutField, String.valueOf(Math.max(1, rule.antiStuckTimeoutSeconds)));

        editorSequence = safe(rule.postPickupSequence);
        editorEnabled = rule.enabled;
        editorVisualizeRange = rule.visualizeRange;
        editorStopOnExit = rule.stopOnExit;
        editorAntiStuckEnabled = rule.antiStuckEnabled;
        editorAntiStuckRestartSequence = safe(rule.antiStuckRestartSequence);
        editorEnableItemWhitelist = rule.enableItemWhitelist;
        editorEnableItemBlacklist = rule.enableItemBlacklist;
        editorWhitelistEntries.clear();
        editorWhitelistEntries.addAll(copyEntryList(rule.itemWhitelistEntries, rule.itemWhitelist));
        editorBlacklistEntries.clear();
        editorBlacklistEntries.addAll(copyEntryList(rule.itemBlacklistEntries, rule.itemBlacklist));
        editorPickupActionEntries.clear();
        editorPickupActionEntries.addAll(copyPickupActionEntryList(rule.pickupActionEntries));
        editorInventoryDetectionSlots.clear();
        editorInventoryDetectionSlots.addAll(copyInventoryDetectionSlots(rule.inventoryDetectionSlots));
        selectedWhitelistEntryIndex = editorWhitelistEntries.isEmpty() ? -1 : 0;
        selectedBlacklistEntryIndex = editorBlacklistEntries.isEmpty() ? -1 : 0;
        selectedPickupActionEntryIndex = editorPickupActionEntries.isEmpty() ? -1 : 0;
        whitelistScrollOffset = 0;
        blacklistScrollOffset = 0;
        pickupActionScrollOffset = 0;
        clearInventoryDetectionDragState();
        clearEntryDragState();

        layoutAllWidgets();
    }

    @Override
    protected AutoPickupRule buildItemFromEditor(boolean creatingNew, AutoPickupRule selectedItem) {
        AutoPickupRule base = creatingNew ? new AutoPickupRule() : copyItem(selectedItem);
        base.name = safe(nameField.getText()).trim();
        base.category = normalizeCategory(categoryField.getText());
        base.enabled = editorEnabled;
        base.centerX = parseDouble(xField.getText(), base.centerX);
        base.centerY = parseDouble(yField.getText(), base.centerY);
        base.centerZ = parseDouble(zField.getText(), base.centerZ);
        base.radius = Math.max(0.0D, parseDouble(radiusField.getText(), base.radius));
        base.targetReachDistance = Math.max(0.0D,
                parseDouble(reachDistanceField.getText(), base.targetReachDistance));
        base.maxPickupAttempts = Math.max(1, parseInt(maxPickupAttemptsField.getText(), base.maxPickupAttempts));
        base.visualizeRange = editorVisualizeRange;
        base.enableItemWhitelist = editorEnableItemWhitelist;
        base.enableItemBlacklist = editorEnableItemBlacklist;
        base.itemWhitelistEntries = normalizeEntryList(editorWhitelistEntries);
        base.itemBlacklistEntries = normalizeEntryList(editorBlacklistEntries);
        base.pickupActionEntries = normalizePickupActionEntryList(editorPickupActionEntries);
        base.inventoryDetectionSlots = copyInventoryDetectionSlots(editorInventoryDetectionSlots);
        syncLegacyKeywordLists(base);
        base.postPickupSequence = safe(editorSequence).trim();
        base.postPickupDelaySeconds = Math.max(0, parseInt(delayField.getText(), base.postPickupDelaySeconds));
        base.stopOnExit = editorStopOnExit;
        base.antiStuckEnabled = editorAntiStuckEnabled;
        base.antiStuckTimeoutSeconds = Math.max(1,
                parseInt(antiStuckTimeoutField.getText(), base.antiStuckTimeoutSeconds));
        base.antiStuckRestartSequence = safe(editorAntiStuckRestartSequence).trim();
        return base;
    }

    @Override
    protected void applyItemValues(AutoPickupRule target, AutoPickupRule source) {
        if (target == null || source == null) {
            return;
        }
        target.name = source.name;
        target.category = normalizeCategory(source.category);
        target.enabled = source.enabled;
        target.centerX = source.centerX;
        target.centerY = source.centerY;
        target.centerZ = source.centerZ;
        target.radius = source.radius;
        target.targetReachDistance = source.targetReachDistance;
        target.maxPickupAttempts = source.maxPickupAttempts;
        target.visualizeRange = source.visualizeRange;
        target.enableItemWhitelist = source.enableItemWhitelist;
        target.enableItemBlacklist = source.enableItemBlacklist;
        target.itemWhitelistEntries = copyEntryList(source.itemWhitelistEntries, source.itemWhitelist);
        target.itemBlacklistEntries = copyEntryList(source.itemBlacklistEntries, source.itemBlacklist);
        target.pickupActionEntries = copyPickupActionEntryList(source.pickupActionEntries);
        target.inventoryDetectionSlots = copyInventoryDetectionSlots(source.inventoryDetectionSlots);
        syncLegacyKeywordLists(target);
        target.postPickupSequence = source.postPickupSequence;
        target.postPickupDelaySeconds = source.postPickupDelaySeconds;
        target.stopOnExit = source.stopOnExit;
        target.antiStuckEnabled = source.antiStuckEnabled;
        target.antiStuckTimeoutSeconds = source.antiStuckTimeoutSeconds;
        target.antiStuckRestartSequence = source.antiStuckRestartSequence;
    }

    @Override
    protected void initEditorWidgets() {
        nameField = createField(3001);
        categoryField = createField(3002);
        xField = createField(3003);
        yField = createField(3004);
        zField = createField(3005);
        radiusField = createField(3006);
        reachDistanceField = createField(3009);
        maxPickupAttemptsField = createField(3010);
        delayField = createField(3007);
        antiStuckTimeoutField = createField(3008);
    }

    @Override
    protected void initAdditionalButtons() {
        btnSelectSequence = new ThemedButton(BTN_SELECT_SEQUENCE, 0, 0, 100, 20, "");
        btnGetCoords = new ThemedButton(BTN_GET_COORDS, 0, 0, 100, 20,
                I18n.format("gui.autopickup.btn.get_coords"));
        btnToggleEnabled = new ThemedButton(BTN_TOGGLE_ENABLED, 0, 0, 100, 20, "");
        btnToggleVisualizeRange = new ThemedButton(BTN_TOGGLE_VISUALIZE_RANGE, 0, 0, 100, 20, "");
        btnToggleStopOnExit = new ThemedButton(BTN_TOGGLE_STOP_ON_EXIT, 0, 0, 100, 20, "");
        btnToggleAntiStuck = new ThemedButton(BTN_TOGGLE_ANTI_STUCK, 0, 0, 100, 20, "");
        btnSelectAntiStuckSequence = new ThemedButton(BTN_SELECT_ANTI_STUCK_SEQUENCE, 0, 0, 100, 20, "");
        btnToggleItemWhitelist = new ThemedButton(BTN_TOGGLE_ITEM_WHITELIST, 0, 0, 100, 20, "");
        btnToggleItemBlacklist = new ThemedButton(BTN_TOGGLE_ITEM_BLACKLIST, 0, 0, 100, 20, "");
        btnAddWhitelistEntry = new ThemedButton(BTN_ADD_WHITELIST_ENTRY, 0, 0, 100, 20, "新增");
        btnEditWhitelistEntry = new ThemedButton(BTN_EDIT_WHITELIST_ENTRY, 0, 0, 100, 20, "编辑");
        btnDeleteWhitelistEntry = new ThemedButton(BTN_DELETE_WHITELIST_ENTRY, 0, 0, 100, 20, "删除");
        btnAddBlacklistEntry = new ThemedButton(BTN_ADD_BLACKLIST_ENTRY, 0, 0, 100, 20, "新增");
        btnEditBlacklistEntry = new ThemedButton(BTN_EDIT_BLACKLIST_ENTRY, 0, 0, 100, 20, "编辑");
        btnDeleteBlacklistEntry = new ThemedButton(BTN_DELETE_BLACKLIST_ENTRY, 0, 0, 100, 20, "删除");
        btnWhitelistScrollUp = new ThemedButton(BTN_WHITELIST_SCROLL_UP, 0, 0, 100, 20, "上翻");
        btnWhitelistScrollDown = new ThemedButton(BTN_WHITELIST_SCROLL_DOWN, 0, 0, 100, 20, "下翻");
        btnBlacklistScrollUp = new ThemedButton(BTN_BLACKLIST_SCROLL_UP, 0, 0, 100, 20, "上翻");
        btnBlacklistScrollDown = new ThemedButton(BTN_BLACKLIST_SCROLL_DOWN, 0, 0, 100, 20, "下翻");
        btnAddPickupActionEntry = new ThemedButton(BTN_ADD_PICKUP_ACTION_ENTRY, 0, 0, 100, 20, "新增");
        btnEditPickupActionEntry = new ThemedButton(BTN_EDIT_PICKUP_ACTION_ENTRY, 0, 0, 100, 20, "编辑");
        btnDeletePickupActionEntry = new ThemedButton(BTN_DELETE_PICKUP_ACTION_ENTRY, 0, 0, 100, 20, "删除");
        btnPickupActionScrollUp = new ThemedButton(BTN_PICKUP_ACTION_SCROLL_UP, 0, 0, 100, 20, "上翻");
        btnPickupActionScrollDown = new ThemedButton(BTN_PICKUP_ACTION_SCROLL_DOWN, 0, 0, 100, 20, "下翻");

        this.buttonList.add(btnSelectSequence);
        this.buttonList.add(btnGetCoords);
        this.buttonList.add(btnToggleEnabled);
        this.buttonList.add(btnToggleVisualizeRange);
        this.buttonList.add(btnToggleStopOnExit);
        this.buttonList.add(btnToggleAntiStuck);
        this.buttonList.add(btnSelectAntiStuckSequence);
        this.buttonList.add(btnToggleItemWhitelist);
        this.buttonList.add(btnToggleItemBlacklist);
        this.buttonList.add(btnAddWhitelistEntry);
        this.buttonList.add(btnEditWhitelistEntry);
        this.buttonList.add(btnDeleteWhitelistEntry);
        this.buttonList.add(btnAddBlacklistEntry);
        this.buttonList.add(btnEditBlacklistEntry);
        this.buttonList.add(btnDeleteBlacklistEntry);
        this.buttonList.add(btnWhitelistScrollUp);
        this.buttonList.add(btnWhitelistScrollDown);
        this.buttonList.add(btnBlacklistScrollUp);
        this.buttonList.add(btnBlacklistScrollDown);
        this.buttonList.add(btnAddPickupActionEntry);
        this.buttonList.add(btnEditPickupActionEntry);
        this.buttonList.add(btnDeletePickupActionEntry);
        this.buttonList.add(btnPickupActionScrollUp);
        this.buttonList.add(btnPickupActionScrollDown);
    }

    @Override
    protected void layoutEditorWidgets() {
        editorRowStartY = editorY + 52;
        editorVisibleRows = Math.max(1, (editorHeight - 62) / EDITOR_ROW_HEIGHT);
        int right = editorX + editorWidth - 14;
        int fullFieldWidth = Math.max(120, right - editorFieldX);
        int halfWidth = Math.max(70, (fullFieldWidth - 10) / 2);
        int actionButtonWidth = Math.max(1, (fullFieldWidth - 8) / 3);
        int smallButtonWidth = Math.max(1, (fullFieldWidth - 4) / 2);

        placeField(nameField, 0, editorFieldX, fullFieldWidth);
        placeField(categoryField, 1, editorFieldX, fullFieldWidth);

        placeButton(btnToggleEnabled, 2, editorFieldX, fullFieldWidth, 20);
        placeButton(btnToggleVisualizeRange, 3, editorFieldX, fullFieldWidth, 20);
        placeField(xField, 4, editorFieldX, halfWidth);
        placeField(yField, 4, editorFieldX + halfWidth + 10, halfWidth);
        placeField(zField, 5, editorFieldX, halfWidth);
        placeField(radiusField, 5, editorFieldX + halfWidth + 10, halfWidth);
        placeField(reachDistanceField, 6, editorFieldX, halfWidth);
        placeField(maxPickupAttemptsField, 6, editorFieldX + halfWidth + 10, halfWidth);
        placeButton(btnGetCoords, 7, editorFieldX, fullFieldWidth, 20);
        placeButton(btnToggleAntiStuck, 8, editorFieldX, fullFieldWidth, 20);
        placeField(antiStuckTimeoutField, 9, editorFieldX, halfWidth);
        placeButton(btnSelectAntiStuckSequence, 9, editorFieldX + halfWidth + 10, halfWidth, 20);

        placeButton(btnToggleItemWhitelist, ROW_WHITELIST_TOGGLE, editorFieldX, fullFieldWidth, 20);
        placeButton(btnAddWhitelistEntry, ROW_WHITELIST_ACTIONS, editorFieldX, actionButtonWidth, 20);
        placeButton(btnEditWhitelistEntry, ROW_WHITELIST_ACTIONS, editorFieldX + actionButtonWidth + 4,
                actionButtonWidth, 20);
        placeButton(btnDeleteWhitelistEntry, ROW_WHITELIST_ACTIONS,
                editorFieldX + 2 * (actionButtonWidth + 4), actionButtonWidth, 20);
        placeButton(btnWhitelistScrollUp, ROW_WHITELIST_SCROLL, editorFieldX, smallButtonWidth, 20);
        placeButton(btnWhitelistScrollDown, ROW_WHITELIST_SCROLL, editorFieldX + smallButtonWidth + 4,
                smallButtonWidth, 20);

        placeButton(btnToggleItemBlacklist, ROW_BLACKLIST_TOGGLE, editorFieldX, fullFieldWidth, 20);
        placeButton(btnAddBlacklistEntry, ROW_BLACKLIST_ACTIONS, editorFieldX, actionButtonWidth, 20);
        placeButton(btnEditBlacklistEntry, ROW_BLACKLIST_ACTIONS, editorFieldX + actionButtonWidth + 4,
                actionButtonWidth, 20);
        placeButton(btnDeleteBlacklistEntry, ROW_BLACKLIST_ACTIONS,
                editorFieldX + 2 * (actionButtonWidth + 4), actionButtonWidth, 20);
        placeButton(btnBlacklistScrollUp, ROW_BLACKLIST_SCROLL, editorFieldX, smallButtonWidth, 20);
        placeButton(btnBlacklistScrollDown, ROW_BLACKLIST_SCROLL, editorFieldX + smallButtonWidth + 4,
                smallButtonWidth, 20);

        placeButton(btnAddPickupActionEntry, ROW_PICKUP_ACTION_ACTIONS, editorFieldX, actionButtonWidth, 20);
        placeButton(btnEditPickupActionEntry, ROW_PICKUP_ACTION_ACTIONS, editorFieldX + actionButtonWidth + 4,
                actionButtonWidth, 20);
        placeButton(btnDeletePickupActionEntry, ROW_PICKUP_ACTION_ACTIONS,
                editorFieldX + 2 * (actionButtonWidth + 4), actionButtonWidth, 20);
        placeButton(btnPickupActionScrollUp, ROW_PICKUP_ACTION_SCROLL, editorFieldX, smallButtonWidth, 20);
        placeButton(btnPickupActionScrollDown, ROW_PICKUP_ACTION_SCROLL, editorFieldX + smallButtonWidth + 4,
                smallButtonWidth, 20);
        placeButton(btnSelectSequence, ROW_POST_PICKUP_SEQUENCE, editorFieldX, fullFieldWidth, 20);
        placeField(delayField, ROW_POST_PICKUP_DELAY, editorFieldX, halfWidth);
        placeButton(btnToggleStopOnExit, ROW_STOP_ON_EXIT, editorFieldX, fullFieldWidth, 20);
    }

    @Override
    protected void layoutAdditionalButtons() {
        updateEditorButtonStates();
    }

    @Override
    protected int getEditorTotalRows() {
        return getRowsForActiveEditorSection().size();
    }

    @Override
    protected String getEditorRowLabel(int row) {
        return getRowLabel(getActualRowForVisibleRow(row));
    }

    private String getRowLabel(int row) {
        switch (row) {
            case 0:
                return I18n.format("gui.autopickup.rule_name");
            case 1:
                return "所属分组";
            case 2:
                return "启用";
            case 3:
                return "显示拾取范围";
            case 4:
                return "中心 X / Y";
            case 5:
                return "中心 Z / 半径";
            case 6:
                return "到达距离 / 最大尝试次数";
            case 7:
                return "快捷定位";
            case 8:
                return "防卡重启";
            case 9:
                return "卡住停留秒数 / 重启序列";
            case ROW_INVENTORY_DETECTION_TITLE:
                return "背包检测范围";
            case 11:
            case 12:
            case 13:
            case 14:
                return "";
            case ROW_WHITELIST_TOGGLE:
                return "启用掉落物白名单";
            case ROW_WHITELIST_ACTIONS:
                return "白名单卡片操作";
            case ROW_WHITELIST_SCROLL:
                return "白名单滚动";
            case 18:
            case 19:
            case 20:
            case 21:
                return "";
            case ROW_BLACKLIST_TOGGLE:
                return "启用掉落物黑名单";
            case ROW_BLACKLIST_ACTIONS:
                return "黑名单卡片操作";
            case ROW_BLACKLIST_SCROLL:
                return "黑名单滚动";
            case 25:
            case 26:
            case 27:
            case 28:
                return "";
            case ROW_PICKUP_ACTION_ACTIONS:
                return "触发条件卡片操作";
            case ROW_PICKUP_ACTION_SCROLL:
                return "触发条件卡片滚动";
            case 31:
            case 32:
            case 33:
            case 34:
                return "";
            case ROW_POST_PICKUP_SEQUENCE:
                return "全部拾取后序列";
            case ROW_POST_PICKUP_DELAY:
                return I18n.format("gui.autopickup.delay");
            case ROW_STOP_ON_EXIT:
                return "离开区域停止后续";
            default:
                return "";
        }
    }

    @Override
    protected int getEditorRowY(int row) {
        int filteredIndex = getFilteredIndexForRow(row);
        if (filteredIndex < 0) {
            return -1;
        }
        int visibleIndex = filteredIndex - editorScrollOffset;
        if (visibleIndex < 0 || visibleIndex >= editorVisibleRows) {
            return -1;
        }
        return editorRowStartY + visibleIndex * EDITOR_ROW_HEIGHT;
    }

    @Override
    protected List<GuiTextField> getEditorFields() {
        List<GuiTextField> fields = new ArrayList<>();
        fields.add(nameField);
        fields.add(categoryField);
        fields.add(xField);
        fields.add(yField);
        fields.add(zField);
        fields.add(radiusField);
        fields.add(reachDistanceField);
        fields.add(maxPickupAttemptsField);
        fields.add(delayField);
        fields.add(antiStuckTimeoutField);
        return fields;
    }

    @Override
    protected void drawEditorContents(int mouseX, int mouseY, float partialTicks) {
        drawEditorTabs(mouseX, mouseY);
        drawEditorFields();
        if (activeEditorSection == 0) {
            drawInventoryDetectionSection(mouseX, mouseY);
        } else if (activeEditorSection == 1) {
            drawEntryListBox(mouseX, mouseY, true);
        } else if (activeEditorSection == 2) {
            drawEntryListBox(mouseX, mouseY, false);
        } else if (activeEditorSection == 3) {
            drawPickupActionEntryListBox(mouseX, mouseY);
        }
    }

    @Override
    protected void drawCard(AutoPickupRule item, int actualIndex, int x, int y, int width, int height,
            boolean selected, boolean hovered) {
        int cardBottom = y + height;
        int bg = selected ? 0xAA255D8A : (hovered ? 0xAA2E4258 : 0x99222222);
        int border = selected ? 0xFF5FB8FF : (hovered ? 0xFF7EC8FF : 0xFF4B4B4B);

        drawRect(x, y, x + width, cardBottom, bg);
        drawHorizontalLine(x, x + width, y, border);
        drawHorizontalLine(x, x + width, cardBottom, border);
        drawVerticalLine(x, y, cardBottom, border);
        drawVerticalLine(x + width, y, cardBottom, border);

        String status = item.enabled ? "§a✔" : "§c✘";
        drawString(fontRenderer, trimToWidth(status + " " + safe(item.name), width - 12),
                x + 6, y + 5, 0xFFFFFFFF);
        drawString(fontRenderer, trimToWidth("分类: " + normalizeCategory(item.category), width - 12),
                x + 6, y + 18, 0xFFDDDDDD);
        drawString(fontRenderer,
                trimToWidth("中心: " + formatDouble(item.centerX) + ", " + formatDouble(item.centerY) + ", "
                                + formatDouble(item.centerZ) + "  半径: " + formatDouble(item.radius)
                                + "  到达: " + formatDouble(item.targetReachDistance)
                                + "  尝试: " + Math.max(1, item.maxPickupAttempts)
                                + "  显示: " + (item.visualizeRange ? "开" : "关"),
                        width - 12),
                x + 6, y + 31, 0xFFBDBDBD);
        List<AutoPickupRule.ItemMatchEntry> whitelist = copyEntryList(item.itemWhitelistEntries, item.itemWhitelist);
        List<AutoPickupRule.ItemMatchEntry> blacklist = copyEntryList(item.itemBlacklistEntries, item.itemBlacklist);
        String seq = isBlank(item.postPickupSequence)
                ? I18n.format("gui.common.none")
                : item.postPickupSequence;
        int pickupActionCount = copyPickupActionEntryList(item.pickupActionEntries).size();
        String inventoryDetectionSummary = formatInventoryDetectionSummary(item.inventoryDetectionSlots);
        drawString(fontRenderer,
                trimToWidth("检测槽位: " + inventoryDetectionSummary + "  白名单: "
                                + (item.enableItemWhitelist ? "开" : "关") + "(" + whitelist.size() + ")  黑名单: "
                                + (item.enableItemBlacklist ? "开" : "关") + "(" + blacklist.size() + ")",
                        width - 12),
                x + 6, y + 44, 0xFFB8C7D9);
        if (!whitelist.isEmpty() || !blacklist.isEmpty()) {
            String preview = !whitelist.isEmpty()
                    ? "白首条: " + describeEntry(whitelist.get(0))
                    : "黑首条: " + describeEntry(blacklist.get(0));
            drawString(fontRenderer, trimToWidth(preview, width - 12), x + 6, y + 57, 0xFF9FB0C4);
        }
        drawString(fontRenderer,
                trimToWidth("拾取执行: " + pickupActionCount + " 张  全部拾完后续: " + seq + "  延迟: " + item.postPickupDelaySeconds + "s",
                        width - 12),
                x + 6, y + 70, 0xFFB8C7D9);
        drawString(fontRenderer,
                trimToWidth("防卡: " + (item.antiStuckEnabled ? "开" : "关") + "  停留: " + item.antiStuckTimeoutSeconds
                                + "s  重启: "
                                + (isBlank(item.antiStuckRestartSequence) ? "无" : item.antiStuckRestartSequence),
                        width - 12),
                x + 6, y + 83, 0xFF9FB0C4);
    }

    @Override
    protected int getCardHeight() {
        return 96;
    }

    @Override
    protected String validateItem(AutoPickupRule item) {
        if (item == null) {
            return "规则为空";
        }
        if (isBlank(item.name)) {
            return "规则名称不能为空";
        }
        if (item.radius < 0) {
            return "半径不能小于 0";
        }
        if (item.targetReachDistance <= 0) {
            return "达到目标点距离必须大于 0";
        }
        if (item.maxPickupAttempts < 1) {
            return "最大拾取尝试次数不能小于 1";
        }
        if (item.postPickupDelaySeconds < 0) {
            return "延迟秒数不能小于 0";
        }
        if (item.antiStuckTimeoutSeconds < 1) {
            return "防卡停留秒数不能小于 1";
        }
        if (item.antiStuckEnabled
                && !isBlank(item.antiStuckRestartSequence)
                && !PathSequenceManager.hasSequence(item.antiStuckRestartSequence)) {
            return "防卡重启序列不存在: " + item.antiStuckRestartSequence;
        }
        for (Integer slot : item.inventoryDetectionSlots == null ? new ArrayList<Integer>() : item.inventoryDetectionSlots) {
            if (slot == null || slot.intValue() < 0 || slot.intValue() >= INVENTORY_SLOT_COUNT) {
                return "背包检测槽位超出范围: " + slot;
            }
        }
        for (AutoPickupRule.PickupActionEntry entry : normalizePickupActionEntryList(item.pickupActionEntries)) {
            if (entry == null) {
                continue;
            }
            if (isBlank(entry.sequenceName)) {
                return "触发条件卡片存在空序列";
            }
            if (!PathSequenceManager.hasSequence(entry.sequenceName)) {
                return "触发条件卡片序列不存在: " + entry.sequenceName;
            }
            if (entry.executeDelaySeconds < 0) {
                return "触发条件卡片延迟不能小于 0";
            }
        }
        return null;
    }

    @Override
    protected void updateEditorButtonStates() {
        if (btnToggleEnabled != null) {
            btnToggleEnabled.displayString = I18n.format("gui.autopickup.btn.enabled", stateOnOff(editorEnabled));
            btnToggleEnabled.enabled = btnToggleEnabled.visible;
        }
        if (btnToggleVisualizeRange != null) {
            btnToggleVisualizeRange.displayString = "显示拾取范围: " + stateOnOff(editorVisualizeRange);
            btnToggleVisualizeRange.enabled = btnToggleVisualizeRange.visible;
        }
        if (btnToggleAntiStuck != null) {
            btnToggleAntiStuck.displayString = "防卡重启: " + yesNo(editorAntiStuckEnabled);
            btnToggleAntiStuck.enabled = btnToggleAntiStuck.visible;
        }
        if (btnSelectAntiStuckSequence != null) {
            String seqText = isBlank(editorAntiStuckRestartSequence)
                    ? "选择防卡重启序列"
                    : "§f" + editorAntiStuckRestartSequence;
            btnSelectAntiStuckSequence.displayString = trimToWidth(seqText,
                    Math.max(40, btnSelectAntiStuckSequence.width - 12));
            btnSelectAntiStuckSequence.enabled = btnSelectAntiStuckSequence.visible;
        }
        if (btnToggleStopOnExit != null) {
            btnToggleStopOnExit.displayString = I18n.format("gui.autopickup.btn.stop_on_exit", yesNo(editorStopOnExit));
            btnToggleStopOnExit.enabled = btnToggleStopOnExit.visible;
        }
        if (btnToggleItemWhitelist != null) {
            int count = normalizeEntryList(editorWhitelistEntries).size();
            btnToggleItemWhitelist.displayString = "启用掉落物白名单: " + stateOnOff(editorEnableItemWhitelist) + " §7(" + count + ")";
            btnToggleItemWhitelist.enabled = btnToggleItemWhitelist.visible;
        }
        if (btnToggleItemBlacklist != null) {
            int count = normalizeEntryList(editorBlacklistEntries).size();
            btnToggleItemBlacklist.displayString = "启用掉落物黑名单: " + stateOnOff(editorEnableItemBlacklist) + " §7(" + count + ")";
            btnToggleItemBlacklist.enabled = btnToggleItemBlacklist.visible;
        }
        if (btnSelectSequence != null) {
            String seqText = isBlank(editorSequence)
                    ? I18n.format("gui.autopickup.btn.select_seq")
                    : "§f" + editorSequence;
            btnSelectSequence.displayString = trimToWidth(seqText, Math.max(40, btnSelectSequence.width - 12));
            btnSelectSequence.enabled = btnSelectSequence.visible;
        }
        if (btnGetCoords != null) {
            btnGetCoords.enabled = btnGetCoords.visible;
        }
        boolean hasWhitelistSelection = selectedWhitelistEntryIndex >= 0 && selectedWhitelistEntryIndex < editorWhitelistEntries.size();
        boolean hasBlacklistSelection = selectedBlacklistEntryIndex >= 0 && selectedBlacklistEntryIndex < editorBlacklistEntries.size();
        boolean hasPickupActionSelection = selectedPickupActionEntryIndex >= 0
                && selectedPickupActionEntryIndex < editorPickupActionEntries.size();
        if (btnAddWhitelistEntry != null) {
            btnAddWhitelistEntry.enabled = btnAddWhitelistEntry.visible;
        }
        if (btnEditWhitelistEntry != null) {
            btnEditWhitelistEntry.enabled = btnEditWhitelistEntry.visible && hasWhitelistSelection;
        }
        if (btnDeleteWhitelistEntry != null) {
            btnDeleteWhitelistEntry.enabled = btnDeleteWhitelistEntry.visible && hasWhitelistSelection;
        }
        if (btnAddBlacklistEntry != null) {
            btnAddBlacklistEntry.enabled = btnAddBlacklistEntry.visible;
        }
        if (btnEditBlacklistEntry != null) {
            btnEditBlacklistEntry.enabled = btnEditBlacklistEntry.visible && hasBlacklistSelection;
        }
        if (btnDeleteBlacklistEntry != null) {
            btnDeleteBlacklistEntry.enabled = btnDeleteBlacklistEntry.visible && hasBlacklistSelection;
        }
        if (btnWhitelistScrollUp != null) {
            btnWhitelistScrollUp.enabled = btnWhitelistScrollUp.visible && whitelistScrollOffset > 0;
        }
        if (btnWhitelistScrollDown != null) {
            btnWhitelistScrollDown.enabled = btnWhitelistScrollDown.visible
                    && whitelistScrollOffset < getWhitelistMaxScroll();
        }
        if (btnBlacklistScrollUp != null) {
            btnBlacklistScrollUp.enabled = btnBlacklistScrollUp.visible && blacklistScrollOffset > 0;
        }
        if (btnBlacklistScrollDown != null) {
            btnBlacklistScrollDown.enabled = btnBlacklistScrollDown.visible
                    && blacklistScrollOffset < getBlacklistMaxScroll();
        }
        if (btnAddPickupActionEntry != null) {
            btnAddPickupActionEntry.enabled = btnAddPickupActionEntry.visible;
        }
        if (btnEditPickupActionEntry != null) {
            btnEditPickupActionEntry.enabled = btnEditPickupActionEntry.visible && hasPickupActionSelection;
        }
        if (btnDeletePickupActionEntry != null) {
            btnDeletePickupActionEntry.enabled = btnDeletePickupActionEntry.visible && hasPickupActionSelection;
        }
        if (btnPickupActionScrollUp != null) {
            btnPickupActionScrollUp.enabled = btnPickupActionScrollUp.visible && pickupActionScrollOffset > 0;
        }
        if (btnPickupActionScrollDown != null) {
            btnPickupActionScrollDown.enabled = btnPickupActionScrollDown.visible
                    && pickupActionScrollOffset < getPickupActionMaxScroll();
        }
    }

    @Override
    protected boolean handleAdditionalAction(GuiButton button) throws IOException {
        if (button.id == BTN_TOGGLE_ENABLED) {
            editorEnabled = !editorEnabled;
            updateEditorButtonStates();
            return true;
        }
        if (button.id == BTN_TOGGLE_VISUALIZE_RANGE) {
            editorVisualizeRange = !editorVisualizeRange;
            updateEditorButtonStates();
            return true;
        }
        if (button.id == BTN_TOGGLE_ANTI_STUCK) {
            editorAntiStuckEnabled = !editorAntiStuckEnabled;
            updateEditorButtonStates();
            return true;
        }
        if (button.id == BTN_TOGGLE_ITEM_WHITELIST) {
            editorEnableItemWhitelist = !editorEnableItemWhitelist;
            updateEditorButtonStates();
            return true;
        }
        if (button.id == BTN_TOGGLE_ITEM_BLACKLIST) {
            editorEnableItemBlacklist = !editorEnableItemBlacklist;
            updateEditorButtonStates();
            return true;
        }
        if (button.id == BTN_TOGGLE_STOP_ON_EXIT) {
            editorStopOnExit = !editorStopOnExit;
            updateEditorButtonStates();
            return true;
        }
        if (button.id == BTN_GET_COORDS) {
            if (mc.player != null) {
                setText(xField, formatDouble(mc.player.posX));
                setText(yField, formatDouble(mc.player.posY));
                setText(zField, formatDouble(mc.player.posZ));
            }
            return true;
        }
        if (button.id == BTN_SELECT_SEQUENCE) {
            EditorStateSnapshot snapshot = captureEditorStateSnapshot();
            mc.displayGuiScreen(new GuiSequenceSelector(this, seq -> {
                snapshot.sequence = safe(seq);
                pendingRestoreState = snapshot;
                mc.displayGuiScreen(this);
            }));
            return true;
        }
        if (button.id == BTN_SELECT_ANTI_STUCK_SEQUENCE) {
            EditorStateSnapshot snapshot = captureEditorStateSnapshot();
            mc.displayGuiScreen(new GuiSequenceSelector(this, seq -> {
                snapshot.antiStuckRestartSequence = safe(seq);
                pendingRestoreState = snapshot;
                mc.displayGuiScreen(this);
            }));
            return true;
        }
        if (button.id == BTN_ADD_WHITELIST_ENTRY) {
            openEntryEditor(true, -1);
            return true;
        }
        if (button.id == BTN_EDIT_WHITELIST_ENTRY) {
            openEntryEditor(true, selectedWhitelistEntryIndex);
            return true;
        }
        if (button.id == BTN_DELETE_WHITELIST_ENTRY) {
            if (selectedWhitelistEntryIndex >= 0 && selectedWhitelistEntryIndex < editorWhitelistEntries.size()) {
                editorWhitelistEntries.remove(selectedWhitelistEntryIndex);
                if (editorWhitelistEntries.isEmpty()) {
                    selectedWhitelistEntryIndex = -1;
                } else if (selectedWhitelistEntryIndex >= editorWhitelistEntries.size()) {
                    selectedWhitelistEntryIndex = editorWhitelistEntries.size() - 1;
                }
                whitelistScrollOffset = Math.min(whitelistScrollOffset, getWhitelistMaxScroll());
                updateEditorButtonStates();
            }
            return true;
        }
        if (button.id == BTN_ADD_BLACKLIST_ENTRY) {
            openEntryEditor(false, -1);
            return true;
        }
        if (button.id == BTN_EDIT_BLACKLIST_ENTRY) {
            openEntryEditor(false, selectedBlacklistEntryIndex);
            return true;
        }
        if (button.id == BTN_DELETE_BLACKLIST_ENTRY) {
            if (selectedBlacklistEntryIndex >= 0 && selectedBlacklistEntryIndex < editorBlacklistEntries.size()) {
                editorBlacklistEntries.remove(selectedBlacklistEntryIndex);
                if (editorBlacklistEntries.isEmpty()) {
                    selectedBlacklistEntryIndex = -1;
                } else if (selectedBlacklistEntryIndex >= editorBlacklistEntries.size()) {
                    selectedBlacklistEntryIndex = editorBlacklistEntries.size() - 1;
                }
                blacklistScrollOffset = Math.min(blacklistScrollOffset, getBlacklistMaxScroll());
                updateEditorButtonStates();
            }
            return true;
        }
        if (button.id == BTN_WHITELIST_SCROLL_UP) {
            whitelistScrollOffset = Math.max(0, whitelistScrollOffset - 1);
            updateEditorButtonStates();
            return true;
        }
        if (button.id == BTN_WHITELIST_SCROLL_DOWN) {
            whitelistScrollOffset = Math.min(getWhitelistMaxScroll(), whitelistScrollOffset + 1);
            updateEditorButtonStates();
            return true;
        }
        if (button.id == BTN_BLACKLIST_SCROLL_UP) {
            blacklistScrollOffset = Math.max(0, blacklistScrollOffset - 1);
            updateEditorButtonStates();
            return true;
        }
        if (button.id == BTN_BLACKLIST_SCROLL_DOWN) {
            blacklistScrollOffset = Math.min(getBlacklistMaxScroll(), blacklistScrollOffset + 1);
            updateEditorButtonStates();
            return true;
        }
        if (button.id == BTN_ADD_PICKUP_ACTION_ENTRY) {
            openPickupActionEntryEditor(-1);
            return true;
        }
        if (button.id == BTN_EDIT_PICKUP_ACTION_ENTRY) {
            openPickupActionEntryEditor(selectedPickupActionEntryIndex);
            return true;
        }
        if (button.id == BTN_DELETE_PICKUP_ACTION_ENTRY) {
            if (selectedPickupActionEntryIndex >= 0 && selectedPickupActionEntryIndex < editorPickupActionEntries.size()) {
                editorPickupActionEntries.remove(selectedPickupActionEntryIndex);
                if (editorPickupActionEntries.isEmpty()) {
                    selectedPickupActionEntryIndex = -1;
                } else if (selectedPickupActionEntryIndex >= editorPickupActionEntries.size()) {
                    selectedPickupActionEntryIndex = editorPickupActionEntries.size() - 1;
                }
                pickupActionScrollOffset = Math.min(pickupActionScrollOffset, getPickupActionMaxScroll());
                updateEditorButtonStates();
            }
            return true;
        }
        if (button.id == BTN_PICKUP_ACTION_SCROLL_UP) {
            pickupActionScrollOffset = Math.max(0, pickupActionScrollOffset - 1);
            updateEditorButtonStates();
            return true;
        }
        if (button.id == BTN_PICKUP_ACTION_SCROLL_DOWN) {
            pickupActionScrollOffset = Math.min(getPickupActionMaxScroll(), pickupActionScrollOffset + 1);
            updateEditorButtonStates();
            return true;
        }
        return false;
    }

    @Override
    protected int getEditorLabelWidth() {
        return 102;
    }

    @Override
    protected void drawEditorPanel(int mouseX, int mouseY, float partialTicks) {
        GuiTheme.drawPanelSegment(editorX, editorY, editorWidth, editorHeight, panelX, panelY, panelWidth, panelHeight);
        GuiTheme.drawSectionTitle(editorX + 8, editorY + 8,
                creatingNew ? getEditorPanelTitle() + " - 新建" : getEditorPanelTitle() + " - 编辑",
                this.fontRenderer);

        layoutAllWidgets();
        List<Integer> rows = getRowsForActiveEditorSection();
        int visibleStart = editorScrollOffset;
        int visibleEnd = Math.min(rows.size(), visibleStart + editorVisibleRows);
        for (int i = visibleStart; i < visibleEnd; i++) {
            int row = rows.get(i);
            int y = getEditorRowY(row);
            if (y < 0) {
                continue;
            }
            String label = getRowLabel(row);
            if (!isBlank(label)) {
                drawString(fontRenderer, label, editorLabelX, y + 4, GuiTheme.SUB_TEXT);
            }
        }

        drawEditorContents(mouseX, mouseY, partialTicks);

        if (getEditorTotalRows() > editorVisibleRows) {
            int thumbHeight = Math.max(18,
                    (int) ((editorVisibleRows / (float) getEditorTotalRows()) * (editorHeight - 12)));
            int track = Math.max(1, (editorHeight - 12) - thumbHeight);
            int thumbY = editorY + 6 + (int) ((editorScrollOffset / (float) Math.max(1, getMaxEditorScroll())) * track);
            GuiTheme.drawScrollbar(editorX + editorWidth - 8, editorY + 6, 4, editorHeight - 12, thumbY, thumbHeight);
        }
    }

    @Override
    protected String getTreeCollapsedTitle() {
        return "规则";
    }

    @Override
    protected String getListCollapsedTitle() {
        return "列表";
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        int clickedCardIndex = -1;
        if (mouseButton == 0 && !listCollapsed && isInCardList(mouseX, mouseY)) {
            clickedCardIndex = getCardIndexAt(mouseY);
        }

        super.mouseClicked(mouseX, mouseY, mouseButton);

        if (mouseButton != 0 || clickedCardIndex < 0 || clickedCardIndex >= visibleItems.size()) {
            return;
        }

        long now = System.currentTimeMillis();
        if (clickedCardIndex == lastCardClickIndex && (now - lastCardClickAtMs) <= CARD_DOUBLE_CLICK_INTERVAL_MS) {
            AutoPickupRule rule = visibleItems.get(clickedCardIndex);
            rule.enabled = !rule.enabled;
            persistChanges();
            refreshData(true);
            selectByItemName(rule.name);
            setStatus("§a规则已" + (rule.enabled ? "启用: " : "禁用: ") + safe(rule.name), 0xFF8CFF9E);
            lastCardClickIndex = -1;
            lastCardClickAtMs = 0L;
            return;
        }

        lastCardClickIndex = clickedCardIndex;
        lastCardClickAtMs = now;
    }

    @Override
    protected void onAfterEditorMouseClicked(int mouseX, int mouseY, int mouseButton) {
        if (mouseButton == 0 && handleEditorTabClick(mouseX, mouseY)) {
            return;
        }
        if (mouseButton != 0) {
            return;
        }
        if (activeEditorSection == 0 && handleInventoryDetectionSlotClick(mouseX, mouseY)) {
            return;
        }
        int whitelistIndex = getEntryIndexAt(mouseX, mouseY, true);
        if (whitelistIndex >= 0) {
            selectedWhitelistEntryIndex = whitelistIndex;
            beginEntryDrag(DRAG_LIST_WHITELIST, whitelistIndex, mouseX, mouseY);
            updateEditorButtonStates();
            return;
        }

        int blacklistIndex = getEntryIndexAt(mouseX, mouseY, false);
        if (blacklistIndex >= 0) {
            selectedBlacklistEntryIndex = blacklistIndex;
            beginEntryDrag(DRAG_LIST_BLACKLIST, blacklistIndex, mouseX, mouseY);
            updateEditorButtonStates();
            return;
        }

        int pickupActionIndex = getPickupActionEntryIndexAt(mouseX, mouseY);
        if (pickupActionIndex >= 0) {
            selectedPickupActionEntryIndex = pickupActionIndex;
            beginEntryDrag(DRAG_LIST_PICKUP_ACTION, pickupActionIndex, mouseX, mouseY);
            updateEditorButtonStates();
            return;
        }
        clearEntryDragState();
    }

    @Override
    protected void mouseClickMove(int mouseX, int mouseY, int clickedMouseButton, long timeSinceLastClick) {
        super.mouseClickMove(mouseX, mouseY, clickedMouseButton, timeSinceLastClick);
        if (clickedMouseButton != 0) {
            return;
        }
        if (inventoryDetectionGridDragging) {
            updateInventoryDetectionGridDrag(mouseX, mouseY);
        }
        if (draggingListType == DRAG_LIST_NONE) {
            return;
        }
        draggingMouseX = mouseX;
        draggingMouseY = mouseY;
        updateDraggedEntryPosition(mouseX, mouseY);
    }

    @Override
    protected void mouseReleased(int mouseX, int mouseY, int state) {
        super.mouseReleased(mouseX, mouseY, state);
        if (state == 0) {
            endInventoryDetectionGridDrag();
            clearEntryDragState();
        }
    }

    @Override
    protected boolean handleEditorMouseWheel(int mouseX, int mouseY, int wheel) {
        int steps = Math.max(1, Math.abs(wheel) / MOUSE_WHEEL_NOTCH);
        if (activeEditorSection == 1 && isMouseInEntryListBox(mouseX, mouseY, true)) {
            return scrollItemEntryList(true, wheel, steps);
        }
        if (activeEditorSection == 2 && isMouseInEntryListBox(mouseX, mouseY, false)) {
            return scrollItemEntryList(false, wheel, steps);
        }
        if (activeEditorSection == 3 && isMouseInPickupActionEntryListBox(mouseX, mouseY)) {
            return scrollPickupActionEntryList(wheel, steps);
        }
        return false;
    }

    private void drawInventoryDetectionSection(int mouseX, int mouseY) {
        inventoryDetectionSlotRegions.clear();
        pruneInventoryDetectionSlots();

        int infoY = getEditorRowY(ROW_INVENTORY_DETECTION_TITLE);
        if (infoY >= 0) {
            String info = editorInventoryDetectionSlots.isEmpty()
                    ? "§7未选时默认检查整个背包"
                    : "§7已指定 " + editorInventoryDetectionSlots.size() + " 个检测槽位";
            drawString(fontRenderer, info, editorFieldX, infoY + 4, 0xFFB8C7D9);
        }

        drawInventoryDetectionGrid(mouseX, mouseY, getEditorRowY(ROW_INVENTORY_DETECTION_GRID));
    }

    private void drawInventoryDetectionGrid(int mouseX, int mouseY, int baseY) {
        if (baseY < 0) {
            return;
        }

        int gap = 2;
        int cellSize = getInventoryDetectionGridCellSize();
        int gridWidth = INVENTORY_GRID_COLS * cellSize + Math.max(0, INVENTORY_GRID_COLS - 1) * gap;
        int gridX = editorFieldX + Math.max(0, (getEditorContentWidth() - gridWidth) / 2);

        for (int row = 0; row < INVENTORY_GRID_ROWS; row++) {
            for (int col = 0; col < INVENTORY_GRID_COLS; col++) {
                int slotIndex = row * INVENTORY_GRID_COLS + col;
                int cellX = gridX + col * (cellSize + gap);
                int cellY = baseY + row * (cellSize + gap);
                inventoryDetectionSlotRegions.add(new IndexedHitRegion(cellX, cellY, cellSize, cellSize, slotIndex));

                boolean selected = editorInventoryDetectionSlots.contains(slotIndex);
                boolean hovered = mouseX >= cellX && mouseX <= cellX + cellSize
                        && mouseY >= cellY && mouseY <= cellY + cellSize;
                int bg = selected ? (hovered ? 0xFF4B8BB2 : 0xFF2F6F95) : (hovered ? 0xFF31465C : 0xFF1B2D3D);
                int border = selected ? 0xFF9FDFFF : 0xFF3F6A8C;

                drawRect(cellX, cellY, cellX + cellSize, cellY + cellSize, bg);
                drawHorizontalLine(cellX, cellX + cellSize, cellY, border);
                drawHorizontalLine(cellX, cellX + cellSize, cellY + cellSize, border);
                drawVerticalLine(cellX, cellY, cellY + cellSize, border);
                drawVerticalLine(cellX + cellSize, cellY, cellY + cellSize, border);

                String label = String.valueOf(slotIndex);
                int textX = cellX + (cellSize - fontRenderer.getStringWidth(label)) / 2;
                int textY = cellY + Math.max(3, (cellSize - 8) / 2);
                drawString(fontRenderer, label, textX, textY, 0xFFEAF7FF);
            }
        }
    }

    private boolean handleInventoryDetectionSlotClick(int mouseX, int mouseY) {
        for (IndexedHitRegion region : inventoryDetectionSlotRegions) {
            if (region.contains(mouseX, mouseY)) {
                beginInventoryDetectionGridDrag(region);
                return true;
            }
        }
        return false;
    }

    private void beginInventoryDetectionGridDrag(IndexedHitRegion region) {
        if (region == null) {
            return;
        }
        pruneInventoryDetectionSlots();
        inventoryDetectionGridDragging = true;
        inventoryDetectionDragAnchorIndex = region.index;
        inventoryDetectionDragCurrentIndex = region.index;
        inventoryDetectionDragSelectionSnapshot.clear();
        inventoryDetectionDragSelectionSnapshot.addAll(editorInventoryDetectionSlots);
        inventoryDetectionDragAddMode = !editorInventoryDetectionSlots.contains(region.index);
        applyInventoryDetectionDragSelection();
    }

    private void updateInventoryDetectionGridDrag(int mouseX, int mouseY) {
        if (!inventoryDetectionGridDragging) {
            return;
        }
        IndexedHitRegion region = findInventoryDetectionGridRegion(mouseX, mouseY);
        if (region == null || region.index == inventoryDetectionDragCurrentIndex) {
            return;
        }
        inventoryDetectionDragCurrentIndex = region.index;
        applyInventoryDetectionDragSelection();
    }

    private IndexedHitRegion findInventoryDetectionGridRegion(int mouseX, int mouseY) {
        for (IndexedHitRegion region : inventoryDetectionSlotRegions) {
            if (region.contains(mouseX, mouseY)) {
                return region;
            }
        }
        return null;
    }

    private void applyInventoryDetectionDragSelection() {
        editorInventoryDetectionSlots.clear();
        editorInventoryDetectionSlots.addAll(inventoryDetectionDragSelectionSnapshot);

        int maxSlots = INVENTORY_SLOT_COUNT;
        int cols = INVENTORY_GRID_COLS;
        if (maxSlots <= 0 || cols <= 0) {
            return;
        }

        int anchor = MathHelper.clamp(inventoryDetectionDragAnchorIndex, 0, maxSlots - 1);
        int current = MathHelper.clamp(inventoryDetectionDragCurrentIndex, 0, maxSlots - 1);
        int startRow = Math.min(anchor / cols, current / cols);
        int endRow = Math.max(anchor / cols, current / cols);
        int startCol = Math.min(anchor % cols, current % cols);
        int endCol = Math.max(anchor % cols, current % cols);

        for (int row = startRow; row <= endRow; row++) {
            for (int col = startCol; col <= endCol; col++) {
                int slotIndex = row * cols + col;
                if (slotIndex < 0 || slotIndex >= maxSlots) {
                    continue;
                }
                if (inventoryDetectionDragAddMode) {
                    editorInventoryDetectionSlots.add(slotIndex);
                } else {
                    editorInventoryDetectionSlots.remove(slotIndex);
                }
            }
        }
    }

    private void endInventoryDetectionGridDrag() {
        if (!inventoryDetectionGridDragging) {
            return;
        }
        clearInventoryDetectionDragState();
        pruneInventoryDetectionSlots();
    }

    private void clearInventoryDetectionDragState() {
        inventoryDetectionGridDragging = false;
        inventoryDetectionDragAddMode = true;
        inventoryDetectionDragAnchorIndex = -1;
        inventoryDetectionDragCurrentIndex = -1;
        inventoryDetectionDragSelectionSnapshot.clear();
    }

    private void pruneInventoryDetectionSlots() {
        List<Integer> normalized = copyInventoryDetectionSlots(editorInventoryDetectionSlots);
        editorInventoryDetectionSlots.clear();
        editorInventoryDetectionSlots.addAll(normalized);
    }

    private int getEditorContentWidth() {
        return Math.max(120, editorX + editorWidth - 14 - editorFieldX);
    }

    private int getInventoryDetectionGridCellSize() {
        int availableWidth = getEditorContentWidth();
        int gap = 2;
        return Math.max(14, Math.min(22,
                (availableWidth - Math.max(0, INVENTORY_GRID_COLS - 1) * gap) / Math.max(1, INVENTORY_GRID_COLS)));
    }

    private void drawEntryListBox(int mouseX, int mouseY, boolean whitelist) {
        int boxX = editorFieldX;
        int boxY = getEditorRowY(whitelist ? ROW_WHITELIST_BOX : ROW_BLACKLIST_BOX);
        if (boxY < 0) {
            return;
        }
        int boxWidth = Math.max(120, editorX + editorWidth - 14 - editorFieldX);
        int boxHeight = EDITOR_ROW_HEIGHT * ENTRY_LIST_BOX_ROWS - 6;
        List<AutoPickupRule.ItemMatchEntry> entries = whitelist ? editorWhitelistEntries : editorBlacklistEntries;
        int selectedIndex = whitelist ? selectedWhitelistEntryIndex : selectedBlacklistEntryIndex;
        int scrollOffset = whitelist ? whitelistScrollOffset : blacklistScrollOffset;

        drawRect(boxX, boxY, boxX + boxWidth, boxY + boxHeight, 0x5520222A);
        drawHorizontalLine(boxX, boxX + boxWidth, boxY, 0xFF4B4B4B);
        drawHorizontalLine(boxX, boxX + boxWidth, boxY + boxHeight, 0xFF4B4B4B);
        drawVerticalLine(boxX, boxY, boxY + boxHeight, 0xFF4B4B4B);
        drawVerticalLine(boxX + boxWidth, boxY, boxY + boxHeight, 0xFF4B4B4B);

        if (entries.isEmpty()) {
            int textY = boxY + 8;
            textY += drawWrappedHintText(boxX + 6, textY, boxWidth - 18, 0xFF9FB0C4,
                    "§7暂无卡片，点击上方“新增”添加。");
            textY += drawWrappedHintText(boxX + 6, textY, boxWidth - 18, 0xFF8395AA,
                    "§7每张卡片可填物品关键字，并附加 NBT 标签。");
            drawWrappedHintText(boxX + 6, textY, boxWidth - 18, 0xFF8395AA,
                    "§7可拖动卡片来设置优先级。");
            return;
        }

        int visibleCount = ENTRY_LIST_BOX_ROWS;
        int visibleStart = Math.max(0, Math.min(scrollOffset, Math.max(0, entries.size() - visibleCount)));
        for (int i = 0; i < visibleCount; i++) {
            int entryIndex = visibleStart + i;
            if (entryIndex >= entries.size()) {
                break;
            }
            int rowY = boxY + 4 + i * (ENTRY_CARD_HEIGHT + ENTRY_CARD_GAP);
            boolean hovered = mouseX >= boxX + 3 && mouseX <= boxX + boxWidth - 10
                    && mouseY >= rowY && mouseY <= rowY + ENTRY_CARD_HEIGHT;
            boolean selected = entryIndex == selectedIndex;
            int bg = selected ? 0xAA255D8A : (hovered ? 0xAA2E4258 : 0x88222222);
            int border = selected ? 0xFF5FB8FF : (hovered ? 0xFF7EC8FF : 0xFF4B4B4B);
            drawRect(boxX + 3, rowY, boxX + boxWidth - 10, rowY + ENTRY_CARD_HEIGHT, bg);
            drawHorizontalLine(boxX + 3, boxX + boxWidth - 10, rowY, border);
            drawHorizontalLine(boxX + 3, boxX + boxWidth - 10, rowY + ENTRY_CARD_HEIGHT, border);
            drawString(fontRenderer,
                    trimToWidth((entryIndex + 1) + ". " + describeEntry(entries.get(entryIndex)), boxWidth - 20),
                    boxX + 7, rowY + 6, 0xFFFFFFFF);
        }

        if (entries.size() > visibleCount) {
            int thumbHeight = Math.max(16, (int) ((visibleCount / (float) entries.size()) * (boxHeight - 8)));
            int track = Math.max(1, (boxHeight - 8) - thumbHeight);
            int maxScroll = Math.max(1, entries.size() - visibleCount);
            int thumbY = boxY + 4 + (int) ((visibleStart / (float) maxScroll) * track);
            GuiTheme.drawScrollbar(boxX + boxWidth - 7, boxY + 4, 4, boxHeight - 8, thumbY, thumbHeight);
        }
    }

    private int getEntryIndexAt(int mouseX, int mouseY, boolean whitelist) {
        int boxX = editorFieldX;
        int boxY = getEditorRowY(whitelist ? ROW_WHITELIST_BOX : ROW_BLACKLIST_BOX);
        if (boxY < 0) {
            return -1;
        }
        int boxWidth = Math.max(120, editorX + editorWidth - 14 - editorFieldX);
        int boxHeight = EDITOR_ROW_HEIGHT * ENTRY_LIST_BOX_ROWS - 6;
        if (mouseX < boxX || mouseX > boxX + boxWidth || mouseY < boxY || mouseY > boxY + boxHeight) {
            return -1;
        }

        List<AutoPickupRule.ItemMatchEntry> entries = whitelist ? editorWhitelistEntries : editorBlacklistEntries;
        int scrollOffset = whitelist ? whitelistScrollOffset : blacklistScrollOffset;
        int localY = mouseY - (boxY + 4);
        if (localY < 0) {
            return -1;
        }
        int slot = localY / (ENTRY_CARD_HEIGHT + ENTRY_CARD_GAP);
        if (slot < 0 || slot >= ENTRY_LIST_BOX_ROWS) {
            return -1;
        }
        int entryIndex = scrollOffset + slot;
        return entryIndex >= 0 && entryIndex < entries.size() ? entryIndex : -1;
    }

    private void drawPickupActionEntryListBox(int mouseX, int mouseY) {
        int boxX = editorFieldX;
        int boxY = getEditorRowY(ROW_PICKUP_ACTION_BOX);
        if (boxY < 0) {
            return;
        }
        int boxWidth = Math.max(120, editorX + editorWidth - 14 - editorFieldX);
        int boxHeight = EDITOR_ROW_HEIGHT * ENTRY_LIST_BOX_ROWS - 6;

        drawRect(boxX, boxY, boxX + boxWidth, boxY + boxHeight, 0x5520222A);
        drawHorizontalLine(boxX, boxX + boxWidth, boxY, 0xFF4B4B4B);
        drawHorizontalLine(boxX, boxX + boxWidth, boxY + boxHeight, 0xFF4B4B4B);
        drawVerticalLine(boxX, boxY, boxY + boxHeight, 0xFF4B4B4B);
        drawVerticalLine(boxX + boxWidth, boxY, boxY + boxHeight, 0xFF4B4B4B);

        if (editorPickupActionEntries.isEmpty()) {
            int textY = boxY + 8;
            textY += drawWrappedHintText(boxX + 6, textY, boxWidth - 18, 0xFF9FB0C4,
                    "§7暂无拾取执行卡片，点击上方“新增”添加。");
            textY += drawWrappedHintText(boxX + 6, textY, boxWidth - 18, 0xFF8395AA,
                    "§7每张卡片可配置任意拾取 / 物品关键字 / NBT / 执行序列。");
            drawWrappedHintText(boxX + 6, textY, boxWidth - 18, 0xFF8395AA,
                    "§7可拖动卡片来设置优先级。");
            return;
        }

        int visibleCount = ENTRY_LIST_BOX_ROWS;
        int visibleStart = Math.max(0,
                Math.min(pickupActionScrollOffset, Math.max(0, editorPickupActionEntries.size() - visibleCount)));
        for (int i = 0; i < visibleCount; i++) {
            int entryIndex = visibleStart + i;
            if (entryIndex >= editorPickupActionEntries.size()) {
                break;
            }
            int rowY = boxY + 4 + i * (ENTRY_CARD_HEIGHT + ENTRY_CARD_GAP);
            boolean hovered = mouseX >= boxX + 3 && mouseX <= boxX + boxWidth - 10
                    && mouseY >= rowY && mouseY <= rowY + ENTRY_CARD_HEIGHT;
            boolean selected = entryIndex == selectedPickupActionEntryIndex;
            int bg = selected ? 0xAA255D8A : (hovered ? 0xAA2E4258 : 0x88222222);
            int border = selected ? 0xFF5FB8FF : (hovered ? 0xFF7EC8FF : 0xFF4B4B4B);
            drawRect(boxX + 3, rowY, boxX + boxWidth - 10, rowY + ENTRY_CARD_HEIGHT, bg);
            drawHorizontalLine(boxX + 3, boxX + boxWidth - 10, rowY, border);
            drawHorizontalLine(boxX + 3, boxX + boxWidth - 10, rowY + ENTRY_CARD_HEIGHT, border);
            drawString(fontRenderer,
                    trimToWidth((entryIndex + 1) + ". " + describePickupActionEntry(editorPickupActionEntries.get(entryIndex)),
                            boxWidth - 20),
                    boxX + 7, rowY + 6, 0xFFFFFFFF);
        }

        if (editorPickupActionEntries.size() > visibleCount) {
            int thumbHeight = Math.max(16,
                    (int) ((visibleCount / (float) editorPickupActionEntries.size()) * (boxHeight - 8)));
            int track = Math.max(1, (boxHeight - 8) - thumbHeight);
            int maxScroll = Math.max(1, editorPickupActionEntries.size() - visibleCount);
            int thumbY = boxY + 4 + (int) ((visibleStart / (float) maxScroll) * track);
            GuiTheme.drawScrollbar(boxX + boxWidth - 7, boxY + 4, 4, boxHeight - 8, thumbY, thumbHeight);
        }
    }

    private int getPickupActionEntryIndexAt(int mouseX, int mouseY) {
        int boxX = editorFieldX;
        int boxY = getEditorRowY(ROW_PICKUP_ACTION_BOX);
        if (boxY < 0) {
            return -1;
        }
        int boxWidth = Math.max(120, editorX + editorWidth - 14 - editorFieldX);
        int boxHeight = EDITOR_ROW_HEIGHT * ENTRY_LIST_BOX_ROWS - 6;
        if (mouseX < boxX || mouseX > boxX + boxWidth || mouseY < boxY || mouseY > boxY + boxHeight) {
            return -1;
        }

        int localY = mouseY - (boxY + 4);
        if (localY < 0) {
            return -1;
        }
        int slot = localY / (ENTRY_CARD_HEIGHT + ENTRY_CARD_GAP);
        if (slot < 0 || slot >= ENTRY_LIST_BOX_ROWS) {
            return -1;
        }
        int entryIndex = pickupActionScrollOffset + slot;
        return entryIndex >= 0 && entryIndex < editorPickupActionEntries.size() ? entryIndex : -1;
    }

    private boolean isMouseInEntryListBox(int mouseX, int mouseY, boolean whitelist) {
        return isMouseInListBox(mouseX, mouseY, getEditorRowY(whitelist ? ROW_WHITELIST_BOX : ROW_BLACKLIST_BOX));
    }

    private boolean isMouseInPickupActionEntryListBox(int mouseX, int mouseY) {
        return isMouseInListBox(mouseX, mouseY, getEditorRowY(ROW_PICKUP_ACTION_BOX));
    }

    private boolean isMouseInListBox(int mouseX, int mouseY, int boxY) {
        if (boxY < 0) {
            return false;
        }
        int boxX = editorFieldX;
        int boxWidth = Math.max(120, editorX + editorWidth - 14 - editorFieldX);
        int boxHeight = EDITOR_ROW_HEIGHT * ENTRY_LIST_BOX_ROWS - 6;
        return mouseX >= boxX && mouseX <= boxX + boxWidth && mouseY >= boxY && mouseY <= boxY + boxHeight;
    }

    private boolean scrollItemEntryList(boolean whitelist, int wheel, int steps) {
        int maxScroll = whitelist ? getWhitelistMaxScroll() : getBlacklistMaxScroll();
        if (maxScroll <= 0) {
            return false;
        }
        if (whitelist) {
            whitelistScrollOffset = computeNextEntryScroll(whitelistScrollOffset, maxScroll, wheel, steps);
        } else {
            blacklistScrollOffset = computeNextEntryScroll(blacklistScrollOffset, maxScroll, wheel, steps);
        }
        updateEditorButtonStates();
        return true;
    }

    private boolean scrollPickupActionEntryList(int wheel, int steps) {
        int maxScroll = getPickupActionMaxScroll();
        if (maxScroll <= 0) {
            return false;
        }
        pickupActionScrollOffset = computeNextEntryScroll(pickupActionScrollOffset, maxScroll, wheel, steps);
        updateEditorButtonStates();
        return true;
    }

    private int computeNextEntryScroll(int currentOffset, int maxScroll, int wheel, int steps) {
        int delta = wheel < 0 ? steps : -steps;
        return Math.max(0, Math.min(maxScroll, currentOffset + delta));
    }

    private void beginEntryDrag(int listType, int entryIndex, int mouseX, int mouseY) {
        draggingListType = listType;
        draggingEntryIndex = entryIndex;
        draggingMouseX = mouseX;
        draggingMouseY = mouseY;
    }

    private void clearEntryDragState() {
        draggingListType = DRAG_LIST_NONE;
        draggingEntryIndex = -1;
        draggingMouseX = -1;
        draggingMouseY = -1;
    }

    private void updateDraggedEntryPosition(int mouseX, int mouseY) {
        if (draggingEntryIndex < 0 || getListSizeForType(draggingListType) <= 1) {
            return;
        }
        if (!isMouseNearDragList(mouseX, draggingListType)) {
            return;
        }

        maybeAutoScrollDraggedList(mouseY);
        int targetIndex = getDragTargetIndex(mouseY, draggingListType);
        if (targetIndex < 0 || targetIndex == draggingEntryIndex) {
            return;
        }

        draggingEntryIndex = moveEntryForListType(draggingListType, draggingEntryIndex, targetIndex);
        setSelectedIndexForListType(draggingListType, draggingEntryIndex);
        ensureSelectedEntryVisible(draggingListType);
        updateEditorButtonStates();
    }

    private boolean isMouseNearDragList(int mouseX, int listType) {
        int boxX = editorFieldX;
        int boxWidth = Math.max(120, editorX + editorWidth - 14 - editorFieldX);
        return mouseX >= boxX - 12 && mouseX <= boxX + boxWidth + 12;
    }

    private void maybeAutoScrollDraggedList(int mouseY) {
        int boxY = getListBoxYForType(draggingListType);
        if (boxY < 0) {
            return;
        }
        int boxHeight = EDITOR_ROW_HEIGHT * ENTRY_LIST_BOX_ROWS - 6;
        int scrollOffset = getScrollOffsetForType(draggingListType);
        int maxScroll = getMaxScrollForType(draggingListType);
        int newScrollOffset = scrollOffset;
        if (mouseY <= boxY + LIST_AUTO_SCROLL_MARGIN) {
            newScrollOffset = Math.max(0, scrollOffset - 1);
        } else if (mouseY >= boxY + boxHeight - LIST_AUTO_SCROLL_MARGIN) {
            newScrollOffset = Math.min(maxScroll, scrollOffset + 1);
        }
        if (newScrollOffset != scrollOffset) {
            setScrollOffsetForType(draggingListType, newScrollOffset);
            updateEditorButtonStates();
        }
    }

    private int getDragTargetIndex(int mouseY, int listType) {
        int boxY = getListBoxYForType(listType);
        int size = getListSizeForType(listType);
        if (boxY < 0 || size <= 0) {
            return -1;
        }

        int localY = mouseY - (boxY + 4);
        int slot;
        if (localY <= 0) {
            slot = 0;
        } else {
            int boxHeight = EDITOR_ROW_HEIGHT * ENTRY_LIST_BOX_ROWS - 6;
            int maxLocalY = Math.max(0, boxHeight - 8);
            slot = Math.min(ENTRY_LIST_BOX_ROWS - 1,
                    Math.max(0, Math.min(localY, maxLocalY) / (ENTRY_CARD_HEIGHT + ENTRY_CARD_GAP)));
        }
        return Math.max(0, Math.min(size - 1, getScrollOffsetForType(listType) + slot));
    }

    private int getListBoxYForType(int listType) {
        switch (listType) {
            case DRAG_LIST_WHITELIST:
                return getEditorRowY(ROW_WHITELIST_BOX);
            case DRAG_LIST_BLACKLIST:
                return getEditorRowY(ROW_BLACKLIST_BOX);
            case DRAG_LIST_PICKUP_ACTION:
                return getEditorRowY(ROW_PICKUP_ACTION_BOX);
            default:
                return -1;
        }
    }

    private int getListSizeForType(int listType) {
        switch (listType) {
            case DRAG_LIST_WHITELIST:
                return editorWhitelistEntries.size();
            case DRAG_LIST_BLACKLIST:
                return editorBlacklistEntries.size();
            case DRAG_LIST_PICKUP_ACTION:
                return editorPickupActionEntries.size();
            default:
                return 0;
        }
    }

    private int getScrollOffsetForType(int listType) {
        switch (listType) {
            case DRAG_LIST_WHITELIST:
                return whitelistScrollOffset;
            case DRAG_LIST_BLACKLIST:
                return blacklistScrollOffset;
            case DRAG_LIST_PICKUP_ACTION:
                return pickupActionScrollOffset;
            default:
                return 0;
        }
    }

    private void setScrollOffsetForType(int listType, int value) {
        int clamped = Math.max(0, Math.min(value, getMaxScrollForType(listType)));
        switch (listType) {
            case DRAG_LIST_WHITELIST:
                whitelistScrollOffset = clamped;
                return;
            case DRAG_LIST_BLACKLIST:
                blacklistScrollOffset = clamped;
                return;
            case DRAG_LIST_PICKUP_ACTION:
                pickupActionScrollOffset = clamped;
                return;
            default:
                return;
        }
    }

    private int getMaxScrollForType(int listType) {
        switch (listType) {
            case DRAG_LIST_WHITELIST:
                return getWhitelistMaxScroll();
            case DRAG_LIST_BLACKLIST:
                return getBlacklistMaxScroll();
            case DRAG_LIST_PICKUP_ACTION:
                return getPickupActionMaxScroll();
            default:
                return 0;
        }
    }

    private void setSelectedIndexForListType(int listType, int selectedIndex) {
        switch (listType) {
            case DRAG_LIST_WHITELIST:
                selectedWhitelistEntryIndex = selectedIndex;
                return;
            case DRAG_LIST_BLACKLIST:
                selectedBlacklistEntryIndex = selectedIndex;
                return;
            case DRAG_LIST_PICKUP_ACTION:
                selectedPickupActionEntryIndex = selectedIndex;
                return;
            default:
                return;
        }
    }

    private int moveEntryForListType(int listType, int fromIndex, int toIndex) {
        switch (listType) {
            case DRAG_LIST_WHITELIST:
                return moveListEntry(editorWhitelistEntries, fromIndex, toIndex);
            case DRAG_LIST_BLACKLIST:
                return moveListEntry(editorBlacklistEntries, fromIndex, toIndex);
            case DRAG_LIST_PICKUP_ACTION:
                return moveListEntry(editorPickupActionEntries, fromIndex, toIndex);
            default:
                return fromIndex;
        }
    }

    private <T> int moveListEntry(List<T> entries, int fromIndex, int toIndex) {
        if (entries == null || entries.isEmpty()) {
            return -1;
        }
        if (fromIndex < 0 || fromIndex >= entries.size() || toIndex < 0 || toIndex >= entries.size()) {
            return fromIndex;
        }
        if (fromIndex == toIndex) {
            return fromIndex;
        }
        T moved = entries.remove(fromIndex);
        entries.add(toIndex, moved);
        return toIndex;
    }

    private void ensureSelectedEntryVisible(int listType) {
        int selectedIndex;
        switch (listType) {
            case DRAG_LIST_WHITELIST:
                selectedIndex = selectedWhitelistEntryIndex;
                break;
            case DRAG_LIST_BLACKLIST:
                selectedIndex = selectedBlacklistEntryIndex;
                break;
            case DRAG_LIST_PICKUP_ACTION:
                selectedIndex = selectedPickupActionEntryIndex;
                break;
            default:
                return;
        }
        if (selectedIndex < 0) {
            return;
        }

        int scrollOffset = getScrollOffsetForType(listType);
        if (selectedIndex < scrollOffset) {
            setScrollOffsetForType(listType, selectedIndex);
        } else if (selectedIndex >= scrollOffset + ENTRY_LIST_BOX_ROWS) {
            setScrollOffsetForType(listType, selectedIndex - ENTRY_LIST_BOX_ROWS + 1);
        }
    }

    private int drawWrappedHintText(int x, int y, int maxWidth, int color, String text) {
        if (fontRenderer == null || text == null || text.isEmpty() || maxWidth <= 0) {
            return 0;
        }
        List<String> lines = fontRenderer.listFormattedStringToWidth(text, maxWidth);
        for (int i = 0; i < lines.size(); i++) {
            drawString(fontRenderer, lines.get(i), x, y + i * LIST_HINT_LINE_HEIGHT, color);
        }
        return lines.size() * LIST_HINT_LINE_HEIGHT;
    }

    private void openEntryEditor(boolean whitelist, int editIndex) {
        List<AutoPickupRule.ItemMatchEntry> source = whitelist ? editorWhitelistEntries : editorBlacklistEntries;
        AutoPickupRule.ItemMatchEntry initial = (editIndex >= 0 && editIndex < source.size())
                ? new AutoPickupRule.ItemMatchEntry(source.get(editIndex))
                : new AutoPickupRule.ItemMatchEntry();

        EditorStateSnapshot snapshot = captureEditorStateSnapshot();
        snapshot.activeEditorSection = whitelist ? 1 : 2;
        mc.displayGuiScreen(new GuiEditAutoPickupFilterEntry(this, initial, edited -> {
            List<AutoPickupRule.ItemMatchEntry> target = whitelist ? snapshot.whitelistEntries : snapshot.blacklistEntries;
            AutoPickupRule.ItemMatchEntry normalized = normalizeEntry(edited);
            if (normalized == null) {
                if (editIndex >= 0 && editIndex < target.size()) {
                    target.remove(editIndex);
                    if (whitelist) {
                        snapshot.selectedWhitelistEntryIndex = target.isEmpty() ? -1 : Math.max(0, editIndex - 1);
                    } else {
                        snapshot.selectedBlacklistEntryIndex = target.isEmpty() ? -1 : Math.max(0, editIndex - 1);
                    }
                }
                pendingRestoreState = snapshot;
                mc.displayGuiScreen(this);
                return;
            }
            if (editIndex >= 0 && editIndex < target.size()) {
                target.set(editIndex, normalized);
                if (whitelist) {
                    snapshot.selectedWhitelistEntryIndex = editIndex;
                } else {
                    snapshot.selectedBlacklistEntryIndex = editIndex;
                }
            } else {
                target.add(normalized);
                if (whitelist) {
                    snapshot.selectedWhitelistEntryIndex = target.size() - 1;
                } else {
                    snapshot.selectedBlacklistEntryIndex = target.size() - 1;
                }
            }
            pendingRestoreState = snapshot;
            mc.displayGuiScreen(this);
        }));
    }

    private void openPickupActionEntryEditor(int editIndex) {
        AutoPickupRule.PickupActionEntry initial = (editIndex >= 0 && editIndex < editorPickupActionEntries.size())
                ? new AutoPickupRule.PickupActionEntry(editorPickupActionEntries.get(editIndex))
                : new AutoPickupRule.PickupActionEntry();

        EditorStateSnapshot snapshot = captureEditorStateSnapshot();
        snapshot.activeEditorSection = 3;
        mc.displayGuiScreen(new GuiEditAutoPickupActionEntry(this, initial, edited -> {
            List<AutoPickupRule.PickupActionEntry> target = snapshot.pickupActionEntries;
            AutoPickupRule.PickupActionEntry normalized = normalizePickupActionEntry(edited);
            if (normalized == null) {
                if (editIndex >= 0 && editIndex < target.size()) {
                    target.remove(editIndex);
                    snapshot.selectedPickupActionEntryIndex = target.isEmpty() ? -1 : Math.max(0, editIndex - 1);
                }
                pendingRestoreState = snapshot;
                mc.displayGuiScreen(this);
                return;
            }
            if (editIndex >= 0 && editIndex < target.size()) {
                target.set(editIndex, normalized);
                snapshot.selectedPickupActionEntryIndex = editIndex;
            } else {
                target.add(normalized);
                snapshot.selectedPickupActionEntryIndex = target.size() - 1;
            }
            pendingRestoreState = snapshot;
            mc.displayGuiScreen(this);
        }));
    }

    private List<AutoPickupRule.ItemMatchEntry> copyEntryList(List<AutoPickupRule.ItemMatchEntry> source,
            List<String> legacyKeywords) {
        List<AutoPickupRule.ItemMatchEntry> result = new ArrayList<>();
        if (source != null) {
            for (AutoPickupRule.ItemMatchEntry entry : source) {
                AutoPickupRule.ItemMatchEntry normalized = normalizeEntry(entry);
                if (normalized != null) {
                    result.add(normalized);
                }
            }
        }
        if (result.isEmpty()) {
            for (String keyword : normalizeKeywordList(legacyKeywords)) {
                AutoPickupRule.ItemMatchEntry entry = new AutoPickupRule.ItemMatchEntry();
                entry.keyword = keyword;
                result.add(entry);
            }
        }
        return result;
    }

    private List<AutoPickupRule.ItemMatchEntry> normalizeEntryList(List<AutoPickupRule.ItemMatchEntry> source) {
        List<AutoPickupRule.ItemMatchEntry> result = new ArrayList<>();
        if (source == null) {
            return result;
        }
        for (AutoPickupRule.ItemMatchEntry entry : source) {
            AutoPickupRule.ItemMatchEntry normalized = normalizeEntry(entry);
            if (normalized != null) {
                result.add(normalized);
            }
        }
        return result;
    }

    private List<AutoPickupRule.PickupActionEntry> copyPickupActionEntryList(
            List<AutoPickupRule.PickupActionEntry> source) {
        List<AutoPickupRule.PickupActionEntry> result = new ArrayList<>();
        if (source == null) {
            return result;
        }
        for (AutoPickupRule.PickupActionEntry entry : source) {
            AutoPickupRule.PickupActionEntry normalized = normalizePickupActionEntry(entry);
            if (normalized != null) {
                result.add(normalized);
            }
        }
        return result;
    }

    private List<AutoPickupRule.PickupActionEntry> normalizePickupActionEntryList(
            List<AutoPickupRule.PickupActionEntry> source) {
        return copyPickupActionEntryList(source);
    }

    private List<Integer> copyInventoryDetectionSlots(Iterable<Integer> source) {
        LinkedHashSet<Integer> normalized = new LinkedHashSet<>();
        if (source != null) {
            for (Integer slot : source) {
                if (slot == null) {
                    continue;
                }
                int slotIndex = slot.intValue();
                if (slotIndex >= 0 && slotIndex < INVENTORY_SLOT_COUNT) {
                    normalized.add(slotIndex);
                }
            }
        }
        return new ArrayList<>(normalized);
    }

    private AutoPickupRule.ItemMatchEntry normalizeEntry(AutoPickupRule.ItemMatchEntry source) {
        if (source == null) {
            return null;
        }
        String keyword = KillAuraHandler.normalizeFilterName(source.keyword);
        List<String> requiredNbtTags = normalizeKeywordList(source.requiredNbtTags);
        if (keyword.isEmpty() && requiredNbtTags.isEmpty()) {
            return null;
        }
        AutoPickupRule.ItemMatchEntry entry = new AutoPickupRule.ItemMatchEntry();
        entry.keyword = keyword;
        entry.requiredNbtTags = requiredNbtTags;
        return entry;
    }

    private AutoPickupRule.PickupActionEntry normalizePickupActionEntry(AutoPickupRule.PickupActionEntry source) {
        if (source == null) {
            return null;
        }
        String keyword = KillAuraHandler.normalizeFilterName(source.keyword);
        List<String> requiredNbtTags = normalizeKeywordList(source.requiredNbtTags);
        String sequenceName = safe(source.sequenceName).trim();
        if (sequenceName.isEmpty()) {
            return null;
        }
        AutoPickupRule.PickupActionEntry entry = new AutoPickupRule.PickupActionEntry();
        entry.keyword = keyword;
        entry.requiredNbtTags = requiredNbtTags;
        entry.sequenceName = sequenceName;
        entry.executeDelaySeconds = Math.max(0, source.executeDelaySeconds);
        return entry;
    }

    private void syncLegacyKeywordLists(AutoPickupRule rule) {
        if (rule == null) {
            return;
        }
        rule.itemWhitelistEntries = normalizeEntryList(rule.itemWhitelistEntries);
        rule.itemBlacklistEntries = normalizeEntryList(rule.itemBlacklistEntries);
        rule.itemWhitelist = flattenEntryKeywords(rule.itemWhitelistEntries);
        rule.itemBlacklist = flattenEntryKeywords(rule.itemBlacklistEntries);
    }

    private List<String> flattenEntryKeywords(List<AutoPickupRule.ItemMatchEntry> entries) {
        List<String> result = new ArrayList<>();
        if (entries == null) {
            return result;
        }
        for (AutoPickupRule.ItemMatchEntry entry : entries) {
            String keyword = entry == null ? "" : KillAuraHandler.normalizeFilterName(entry.keyword);
            if (!keyword.isEmpty()) {
                result.add(keyword);
            }
        }
        return normalizeKeywordList(result);
    }

    private String describeEntry(AutoPickupRule.ItemMatchEntry entry) {
        if (entry == null) {
            return "空卡片";
        }
        String keyword = safe(entry.keyword).trim();
        List<String> nbtTags = normalizeKeywordList(entry.requiredNbtTags);
        if (keyword.isEmpty() && nbtTags.isEmpty()) {
            return "空卡片";
        }
        if (keyword.isEmpty()) {
            return "NBT(" + nbtTags.size() + "): " + String.join("/", nbtTags);
        }
        if (nbtTags.isEmpty()) {
            return keyword;
        }
        return keyword + " | NBT(" + nbtTags.size() + ")";
    }

    private String describePickupActionEntry(AutoPickupRule.PickupActionEntry entry) {
        if (entry == null) {
            return "空卡片";
        }
        String keyword = safe(entry.keyword).trim();
        List<String> nbtTags = normalizeKeywordList(entry.requiredNbtTags);
        String sequenceName = safe(entry.sequenceName).trim();
        String condition;
        if (keyword.isEmpty() && nbtTags.isEmpty()) {
            condition = "任意拾取";
        } else if (keyword.isEmpty()) {
            condition = "NBT(" + nbtTags.size() + ")";
        } else if (nbtTags.isEmpty()) {
            condition = keyword;
        } else {
            condition = keyword + " | NBT(" + nbtTags.size() + ")";
        }
        String delayText = Math.max(0, entry.executeDelaySeconds) > 0
                ? (" (" + Math.max(0, entry.executeDelaySeconds) + "s)")
                : "";
        return condition + " -> " + (sequenceName.isEmpty() ? "未选序列" : sequenceName) + delayText;
    }

    private String formatInventoryDetectionSummary(Iterable<Integer> slots) {
        int count = copyInventoryDetectionSlots(slots).size();
        return count <= 0 ? "全背包" : (count + "格");
    }

    private int getWhitelistMaxScroll() {
        return Math.max(0, normalizeEntryList(editorWhitelistEntries).size() - ENTRY_LIST_BOX_ROWS);
    }

    private int getBlacklistMaxScroll() {
        return Math.max(0, normalizeEntryList(editorBlacklistEntries).size() - ENTRY_LIST_BOX_ROWS);
    }

    private int getPickupActionMaxScroll() {
        return Math.max(0, normalizePickupActionEntryList(editorPickupActionEntries).size() - ENTRY_LIST_BOX_ROWS);
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

    private int parseInt(String text, int fallback) {
        try {
            return Integer.parseInt(safe(text).trim());
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private String stateOnOff(boolean enabled) {
        return I18n.format(enabled ? "gui.autoeat.state.on" : "gui.autoeat.state.off");
    }

    private String yesNo(boolean yes) {
        return I18n.format(yes ? "gui.common.yes" : "gui.common.no");
    }

    private EditorStateSnapshot captureEditorStateSnapshot() {
        EditorStateSnapshot snapshot = new EditorStateSnapshot();
        snapshot.selectedItemName = getSelectedItemName();
        snapshot.creatingNew = creatingNew;
        snapshot.editorScrollOffset = editorScrollOffset;
        snapshot.name = nameField == null ? "" : safe(nameField.getText());
        snapshot.category = categoryField == null ? "" : safe(categoryField.getText());
        snapshot.centerX = xField == null ? "" : safe(xField.getText());
        snapshot.centerY = yField == null ? "" : safe(yField.getText());
        snapshot.centerZ = zField == null ? "" : safe(zField.getText());
        snapshot.radius = radiusField == null ? "" : safe(radiusField.getText());
        snapshot.reachDistance = reachDistanceField == null ? "" : safe(reachDistanceField.getText());
        snapshot.maxPickupAttempts = maxPickupAttemptsField == null ? "" : safe(maxPickupAttemptsField.getText());
        snapshot.delay = delayField == null ? "" : safe(delayField.getText());
        snapshot.antiStuckTimeout = antiStuckTimeoutField == null ? "" : safe(antiStuckTimeoutField.getText());
        snapshot.sequence = safe(editorSequence);
        snapshot.antiStuckRestartSequence = safe(editorAntiStuckRestartSequence);
        snapshot.enabled = editorEnabled;
        snapshot.visualizeRange = editorVisualizeRange;
        snapshot.stopOnExit = editorStopOnExit;
        snapshot.antiStuckEnabled = editorAntiStuckEnabled;
        snapshot.enableItemWhitelist = editorEnableItemWhitelist;
        snapshot.enableItemBlacklist = editorEnableItemBlacklist;
        snapshot.whitelistEntries = copyEntryList(editorWhitelistEntries, null);
        snapshot.blacklistEntries = copyEntryList(editorBlacklistEntries, null);
        snapshot.pickupActionEntries = copyPickupActionEntryList(editorPickupActionEntries);
        snapshot.inventoryDetectionSlots = copyInventoryDetectionSlots(editorInventoryDetectionSlots);
        snapshot.selectedWhitelistEntryIndex = selectedWhitelistEntryIndex;
        snapshot.selectedBlacklistEntryIndex = selectedBlacklistEntryIndex;
        snapshot.selectedPickupActionEntryIndex = selectedPickupActionEntryIndex;
        snapshot.whitelistScrollOffset = whitelistScrollOffset;
        snapshot.blacklistScrollOffset = blacklistScrollOffset;
        snapshot.pickupActionScrollOffset = pickupActionScrollOffset;
        snapshot.activeEditorSection = activeEditorSection;
        return snapshot;
    }

    private void applyEditorStateSnapshot(EditorStateSnapshot snapshot) {
        if (snapshot == null) {
            return;
        }

        if (snapshot.creatingNew) {
            creatingNew = true;
            selectedVisibleIndex = -1;
            clearEditorForNew();
        } else if (!isBlank(snapshot.selectedItemName)) {
            selectByItemName(snapshot.selectedItemName);
        }

        setText(nameField, snapshot.name);
        setText(categoryField, snapshot.category);
        setText(xField, snapshot.centerX);
        setText(yField, snapshot.centerY);
        setText(zField, snapshot.centerZ);
        setText(radiusField, snapshot.radius);
        setText(reachDistanceField, snapshot.reachDistance);
        setText(maxPickupAttemptsField, snapshot.maxPickupAttempts);
        setText(delayField, snapshot.delay);
        setText(antiStuckTimeoutField, snapshot.antiStuckTimeout);

        editorSequence = safe(snapshot.sequence);
        editorAntiStuckRestartSequence = safe(snapshot.antiStuckRestartSequence);
        editorEnabled = snapshot.enabled;
        editorVisualizeRange = snapshot.visualizeRange;
        editorStopOnExit = snapshot.stopOnExit;
        editorAntiStuckEnabled = snapshot.antiStuckEnabled;
        editorEnableItemWhitelist = snapshot.enableItemWhitelist;
        editorEnableItemBlacklist = snapshot.enableItemBlacklist;
        editorWhitelistEntries.clear();
        editorWhitelistEntries.addAll(copyEntryList(snapshot.whitelistEntries, null));
        editorBlacklistEntries.clear();
        editorBlacklistEntries.addAll(copyEntryList(snapshot.blacklistEntries, null));
        editorPickupActionEntries.clear();
        editorPickupActionEntries.addAll(copyPickupActionEntryList(snapshot.pickupActionEntries));
        editorInventoryDetectionSlots.clear();
        editorInventoryDetectionSlots.addAll(copyInventoryDetectionSlots(snapshot.inventoryDetectionSlots));
        selectedWhitelistEntryIndex = editorWhitelistEntries.isEmpty()
                ? -1
                : Math.max(0, Math.min(snapshot.selectedWhitelistEntryIndex, editorWhitelistEntries.size() - 1));
        selectedBlacklistEntryIndex = editorBlacklistEntries.isEmpty()
                ? -1
                : Math.max(0, Math.min(snapshot.selectedBlacklistEntryIndex, editorBlacklistEntries.size() - 1));
        selectedPickupActionEntryIndex = editorPickupActionEntries.isEmpty()
                ? -1
                : Math.max(0, Math.min(snapshot.selectedPickupActionEntryIndex, editorPickupActionEntries.size() - 1));
        whitelistScrollOffset = Math.max(0, Math.min(snapshot.whitelistScrollOffset, getWhitelistMaxScroll()));
        blacklistScrollOffset = Math.max(0, Math.min(snapshot.blacklistScrollOffset, getBlacklistMaxScroll()));
        pickupActionScrollOffset = Math.max(0,
                Math.min(snapshot.pickupActionScrollOffset, getPickupActionMaxScroll()));
        activeEditorSection = Math.max(0, Math.min(snapshot.activeEditorSection, editorSections.length - 1));
        editorScrollOffset = snapshot.editorScrollOffset;
        clearInventoryDetectionDragState();
        clearEntryDragState();
        clampEditorScroll();
        layoutAllWidgets();
    }

    @Override
    protected void onUpdateEditor() {
        updateEditorButtonStates();
    }

    private List<Integer> getRowsForActiveEditorSection() {
        List<Integer> rows = new ArrayList<>();
        int startRow;
        int endRow;
        switch (activeEditorSection) {
            case 0:
                startRow = 0;
                endRow = 14;
                break;
            case 1:
                startRow = 15;
                endRow = 21;
                break;
            case 2:
                startRow = 22;
                endRow = 28;
                break;
            case 3:
            default:
                startRow = 29;
                endRow = 37;
                break;
        }
        for (int row = startRow; row <= endRow; row++) {
            rows.add(row);
        }
        return rows;
    }

    private int getFilteredIndexForRow(int row) {
        List<Integer> rows = getRowsForActiveEditorSection();
        for (int i = 0; i < rows.size(); i++) {
            if (rows.get(i) == row) {
                return i;
            }
        }
        return -1;
    }

    private int getActualRowForVisibleRow(int visibleRow) {
        List<Integer> rows = getRowsForActiveEditorSection();
        return visibleRow >= 0 && visibleRow < rows.size() ? rows.get(visibleRow) : -1;
    }

    private int getEditorTabBarX() {
        return editorX + 8;
    }

    private int getEditorTabBarY() {
        return editorY + 24;
    }

    private int getEditorTabBarWidth() {
        return Math.max(80, editorWidth - 16);
    }

    private int getEditorTabBarHeight() {
        return 20;
    }

    private void drawEditorTabs(int mouseX, int mouseY) {
        int tabX = getEditorTabBarX();
        int tabY = getEditorTabBarY();
        int tabBarWidth = getEditorTabBarWidth();
        int tabBarHeight = getEditorTabBarHeight();
        GuiTheme.drawButtonFrameSafe(tabX, tabY, tabBarWidth, tabBarHeight, GuiTheme.UiState.NORMAL);

        int currentX = tabX + 2;
        int gap = 4;
        int eachWidth = Math.max(60, (tabBarWidth - 4 - gap * (editorSections.length - 1)) / editorSections.length);
        for (int i = 0; i < editorSections.length; i++) {
            boolean active = i == activeEditorSection;
            boolean hovered = mouseX >= currentX && mouseX <= currentX + eachWidth
                    && mouseY >= tabY && mouseY <= tabY + tabBarHeight;
            GuiTheme.drawButtonFrameSafe(currentX, tabY, eachWidth, tabBarHeight,
                    active ? GuiTheme.UiState.SELECTED
                            : (hovered ? GuiTheme.UiState.HOVER : GuiTheme.UiState.NORMAL));
            String text = editorSections[i];
            drawString(fontRenderer, text,
                    currentX + (eachWidth - this.fontRenderer.getStringWidth(text)) / 2,
                    tabY + 6, active ? 0xFFFFFFFF : 0xFFE8F1FA);
            currentX += eachWidth + gap;
        }
    }

    private boolean handleEditorTabClick(int mouseX, int mouseY) {
        int tabX = getEditorTabBarX();
        int tabY = getEditorTabBarY();
        int tabBarWidth = getEditorTabBarWidth();
        int tabBarHeight = getEditorTabBarHeight();
        if (mouseX < tabX || mouseX > tabX + tabBarWidth || mouseY < tabY || mouseY > tabY + tabBarHeight) {
            return false;
        }
        int gap = 4;
        int eachWidth = Math.max(60, (tabBarWidth - 4 - gap * (editorSections.length - 1)) / editorSections.length);
        int currentX = tabX + 2;
        for (int i = 0; i < editorSections.length; i++) {
            if (mouseX >= currentX && mouseX <= currentX + eachWidth) {
                activeEditorSection = i;
                editorScrollOffset = 0;
                clearEntryDragState();
                layoutAllWidgets();
                return true;
            }
            currentX += eachWidth + gap;
        }
        return false;
    }

    private List<String> normalizeKeywordList(List<String> source) {
        LinkedHashSet<String> unique = new LinkedHashSet<>();
        if (source != null) {
            for (String entry : source) {
                String normalized = KillAuraHandler.normalizeFilterName(entry);
                if (!normalized.isEmpty()) {
                    unique.add(normalized);
                }
            }
        }
        return new ArrayList<>(unique);
    }

    private static class EditorStateSnapshot {
        private String selectedItemName = "";
        private boolean creatingNew = false;
        private int editorScrollOffset = 0;
        private String name = "";
        private String category = "";
        private String centerX = "";
        private String centerY = "";
        private String centerZ = "";
        private String radius = "";
        private String reachDistance = "";
        private String maxPickupAttempts = "";
        private String delay = "";
        private String antiStuckTimeout = "";
        private String sequence = "";
        private String antiStuckRestartSequence = "";
        private boolean enabled = true;
        private boolean visualizeRange = false;
        private boolean stopOnExit = true;
        private boolean antiStuckEnabled = false;
        private boolean enableItemWhitelist = false;
        private boolean enableItemBlacklist = false;
        private List<AutoPickupRule.ItemMatchEntry> whitelistEntries = new ArrayList<>();
        private List<AutoPickupRule.ItemMatchEntry> blacklistEntries = new ArrayList<>();
        private List<AutoPickupRule.PickupActionEntry> pickupActionEntries = new ArrayList<>();
        private List<Integer> inventoryDetectionSlots = new ArrayList<>();
        private int selectedWhitelistEntryIndex = -1;
        private int selectedBlacklistEntryIndex = -1;
        private int selectedPickupActionEntryIndex = -1;
        private int whitelistScrollOffset = 0;
        private int blacklistScrollOffset = 0;
        private int pickupActionScrollOffset = 0;
        private int activeEditorSection = 0;
    }
}
