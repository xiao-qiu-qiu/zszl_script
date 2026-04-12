package com.zszl.zszlScriptMod.gui.config;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.zszl.zszlScriptMod.gui.components.GuiTextInput;
import com.zszl.zszlScriptMod.gui.components.GuiTheme;
import com.zszl.zszlScriptMod.gui.components.ThemedButton;
import com.zszl.zszlScriptMod.gui.components.ThemedGuiScreen;
import com.zszl.zszlScriptMod.otherfeatures.handler.block.BlockFeatureManager;
import com.zszl.zszlScriptMod.otherfeatures.handler.item.ItemFeatureManager;
import com.zszl.zszlScriptMod.otherfeatures.handler.misc.MiscFeatureManager;
import com.zszl.zszlScriptMod.otherfeatures.handler.movement.MovementFeatureManager;
import com.zszl.zszlScriptMod.otherfeatures.handler.movement.SpeedHandler;
import com.zszl.zszlScriptMod.otherfeatures.handler.render.RenderFeatureManager;
import com.zszl.zszlScriptMod.otherfeatures.handler.world.WorldFeatureManager;
import com.zszl.zszlScriptMod.system.ProfileManager;
import com.zszl.zszlScriptMod.system.ProfileShareCodeManager;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.resources.I18n;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import java.awt.Rectangle;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class GuiProfileManager extends ThemedGuiScreen {
    private static final String PREFERENCES_FILE_NAME = "gui_profile_manager_layout.json";
    private static final int DIVIDER_WIDTH = 12;
    private static final int DIVIDER_HIT_WIDTH = 24;
    private static final int MIN_PROFILE_COLUMN_WIDTH = 150;
    private static final int MIN_FILE_COLUMN_WIDTH = 190;
    private static final int MIN_PREVIEW_COLUMN_WIDTH = 260;
    private static boolean preferencesLoaded = false;
    private static double savedProfileColumnRatio = 0.18D;
    private static double savedFileColumnRatio = 0.28D;

    private final GuiScreen parentScreen;

    private List<String> profiles = new ArrayList<>();
    private List<String> shareableFiles = new ArrayList<>();
    private List<String> filteredShareableFiles = new ArrayList<>();
    private final Set<String> checkedFiles = new LinkedHashSet<>();

    private int selectedProfileIndex = -1;
    private int selectedFileIndex = -1;
    private int lastFileAnchorIndex = -1;

    private int profileScroll = 0;
    private int profileMaxScroll = 0;
    private int fileScroll = 0;
    private int fileMaxScroll = 0;
    private int previewScroll = 0;
    private int previewMaxScroll = 0;

    private String previewContent = "";
    private String statusMessage = "§7支持查看配置、单独编辑保存、Ctrl/Shift 多选并生成分享码";
    private int statusColor = 0xFFB8C7D9;
    private String fileSearchQuery = "";

    private GuiButton btnSelect;
    private GuiButton btnDelete;
    private GuiButton btnOpenEditor;
    private GuiButton btnExportShare;
    private GuiButton btnImportShare;
    private GuiButton btnSelectAllFiles;
    private GuiButton btnClearFileSelection;
    private GuiButton btnRefresh;
    private GuiTextField fileSearchField;
    private boolean draggingProfileDivider = false;
    private boolean draggingFileDivider = false;
    private Rectangle profileDividerBounds = null;
    private Rectangle fileDividerBounds = null;

    public GuiProfileManager(GuiScreen parent) {
        ensurePreferencesLoaded();
        this.parentScreen = parent;
    }

    private static synchronized void ensurePreferencesLoaded() {
        if (preferencesLoaded) {
            return;
        }
        preferencesLoaded = true;
        savedProfileColumnRatio = 0.18D;
        savedFileColumnRatio = 0.28D;

        Path path = getPreferencesPath();
        if (path == null || !Files.exists(path)) {
            return;
        }

        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            JsonObject root = new JsonParser().parse(reader).getAsJsonObject();
            if (root == null) {
                return;
            }
            if (root.has("profileColumnRatio")) {
                savedProfileColumnRatio = root.get("profileColumnRatio").getAsDouble();
            }
            if (root.has("fileColumnRatio")) {
                savedFileColumnRatio = root.get("fileColumnRatio").getAsDouble();
            }
        } catch (Exception ignored) {
            savedProfileColumnRatio = 0.18D;
            savedFileColumnRatio = 0.28D;
        }
    }

    private static Path getPreferencesPath() {
        try {
            return ProfileManager.getCurrentProfileDir().resolve(PREFERENCES_FILE_NAME);
        } catch (Exception ignored) {
            return null;
        }
    }

    private static synchronized void saveLayoutPreferences() {
        Path path = getPreferencesPath();
        if (path == null) {
            return;
        }

        try {
            if (path.getParent() != null) {
                Files.createDirectories(path.getParent());
            }
            JsonObject root = new JsonObject();
            root.addProperty("profileColumnRatio", savedProfileColumnRatio);
            root.addProperty("fileColumnRatio", savedFileColumnRatio);
            try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
                writer.write(root.toString());
            }
        } catch (Exception ignored) {
        }
    }

    private void persistCurrentLayoutRatios() {
        savedProfileColumnRatio = Math.max(0.10D, Math.min(0.40D, savedProfileColumnRatio));
        savedFileColumnRatio = Math.max(0.16D, Math.min(0.55D, savedFileColumnRatio));
        saveLayoutPreferences();
    }

    private void updateResponsiveWidgets() {
        if (fileSearchField != null) {
            fileSearchField.x = getFileSearchX() + 4;
            fileSearchField.y = getFileSearchY() + 5;
            fileSearchField.width = getFileSearchWidth() - 8;
            fileSearchField.height = 10;
        }
        updateDividerBounds();
    }

    private void updateDividerBounds() {
        int dividerCenterOne = getProfileListX() + getProfileListWidth() + 5;
        int dividerCenterTwo = getFileListX() + getFileListWidth() + 5;
        int hitOffset = DIVIDER_HIT_WIDTH / 2;
        int y = getListY() + 4;
        int height = Math.max(36, getListHeight() - 8);
        profileDividerBounds = new Rectangle(dividerCenterOne - hitOffset, y, DIVIDER_HIT_WIDTH, height);
        fileDividerBounds = new Rectangle(dividerCenterTwo - hitOffset, y, DIVIDER_HIT_WIDTH, height);
    }

    private void drawDividerHandle(Rectangle bounds, int mouseX, int mouseY, boolean dragging) {
        if (bounds == null) {
            return;
        }
        boolean hovered = bounds.contains(mouseX, mouseY);
        int accent = dragging ? 0xFF7CD9FF : (hovered ? 0xFF63BFEF : 0xFF3E617A);
        int actualX = bounds.x + Math.max(0, (bounds.width - DIVIDER_WIDTH) / 2);
        if (hovered || dragging) {
            drawRect(bounds.x, bounds.y, bounds.x + bounds.width, bounds.y + bounds.height, 0x33111922);
        }
        drawRect(actualX, bounds.y, actualX + DIVIDER_WIDTH, bounds.y + bounds.height, 0x77111922);
        drawRect(actualX + 5, bounds.y + 18, actualX + 7, bounds.y + bounds.height - 18, accent);
        int centerY = bounds.y + bounds.height / 2 - 12;
        for (int i = 0; i < 4; i++) {
            drawRect(actualX + 3, centerY + i * 7, actualX + DIVIDER_WIDTH - 3, centerY + i * 7 + 2, accent);
        }
    }

    private void applyProfileDividerDrag(int mouseX) {
        int[] widths = getColumnWidths();
        int total = getPanelWidth() - 40;
        int[] minimums = getColumnMinimums(total);
        int desiredWidth = mouseX - getPanelX() - 15;
        int maxProfileWidth = Math.max(minimums[0], total - widths[1] - minimums[2]);
        widths[0] = Math.max(minimums[0], Math.min(desiredWidth, maxProfileWidth));
        savedProfileColumnRatio = widths[0] / (double) Math.max(1, total);
        updateResponsiveWidgets();
    }

    private void applyFileDividerDrag(int mouseX) {
        int[] widths = getColumnWidths();
        int total = getPanelWidth() - 40;
        int[] minimums = getColumnMinimums(total);
        int desiredWidth = mouseX - getFileListX() - 5;
        int remainingAfterProfile = Math.max(1, total - widths[0]);
        int maxFileWidth = Math.max(minimums[1], total - widths[0] - minimums[2]);
        widths[1] = Math.max(minimums[1], Math.min(desiredWidth, maxFileWidth));
        savedFileColumnRatio = widths[1] / (double) Math.max(1, remainingAfterProfile);
        updateResponsiveWidgets();
    }

    @Override
    public void initGui() {
        Keyboard.enableRepeatEvents(true);
        this.buttonList.clear();
        refreshProfiles();
        refreshFilesForSelectedProfile();

        int panelX = getPanelX();
        int panelY = getPanelY();
        int panelWidth = getPanelWidth();

        int actionX = panelX + 10;
        int actionWidth = panelWidth - 20;
        int gap = 8;
        int cols = 5;
        int buttonWidth = Math.max(48, (actionWidth - gap * (cols - 1)) / cols);
        int row1Y = panelY + getPanelHeight() - 52;
        int row2Y = panelY + getPanelHeight() - 28;

        btnSelect = new ThemedButton(0, actionX, row1Y, buttonWidth, 20, I18n.format("gui.profile.select"));
        this.buttonList.add(btnSelect);
        this.buttonList.add(new ThemedButton(1, actionX + (buttonWidth + gap), row1Y, buttonWidth, 20,
                I18n.format("gui.profile.create")));
        btnDelete = new ThemedButton(2, actionX + 2 * (buttonWidth + gap), row1Y, buttonWidth, 20,
                I18n.format("gui.profile.delete"));
        this.buttonList.add(btnDelete);
        btnOpenEditor = new ThemedButton(3, actionX + 3 * (buttonWidth + gap), row1Y, buttonWidth, 20, "§b编辑当前");
        this.buttonList.add(btnOpenEditor);
        btnRefresh = new ThemedButton(4, actionX + 4 * (buttonWidth + gap), row1Y, buttonWidth, 20, "§e刷新");
        this.buttonList.add(btnRefresh);

        btnExportShare = new ThemedButton(5, actionX, row2Y, buttonWidth, 20, "§a复制分享码");
        this.buttonList.add(btnExportShare);
        btnImportShare = new ThemedButton(6, actionX + (buttonWidth + gap), row2Y, buttonWidth, 20, "§d导入分享码");
        this.buttonList.add(btnImportShare);
        btnSelectAllFiles = new ThemedButton(7, actionX + 2 * (buttonWidth + gap), row2Y, buttonWidth, 20, "§a全选文件");
        this.buttonList.add(btnSelectAllFiles);
        btnClearFileSelection = new ThemedButton(8, actionX + 3 * (buttonWidth + gap), row2Y, buttonWidth, 20,
                "§7清空勾选");
        this.buttonList.add(btnClearFileSelection);
        this.buttonList.add(new ThemedButton(9, actionX + 4 * (buttonWidth + gap), row2Y, buttonWidth, 20,
                I18n.format("gui.common.done")));

        fileSearchField = new GuiTextField(100, this.fontRenderer, getFileSearchX() + 4, getFileSearchY() + 5,
                getFileSearchWidth() - 8, 10);
        fileSearchField.setMaxStringLength(128);
        fileSearchField.setCanLoseFocus(true);
        fileSearchField.setEnableBackgroundDrawing(false);
        fileSearchField.setText(fileSearchQuery == null ? "" : fileSearchQuery);

        applyFileFilter(getSelectedFilePath());
        updateResponsiveWidgets();
        updateButtonStates();
    }

    @Override
    public void onGuiClosed() {
        Keyboard.enableRepeatEvents(false);
        super.onGuiClosed();
    }

    private void refreshProfiles() {
        this.profiles = ProfileManager.getAllProfileNames();
        if (profiles.isEmpty()) {
            selectedProfileIndex = -1;
            profileScroll = 0;
            return;
        }

        if (selectedProfileIndex < 0 || selectedProfileIndex >= profiles.size()) {
            selectedProfileIndex = profiles.indexOf(ProfileManager.getActiveProfileName());
            if (selectedProfileIndex < 0) {
                selectedProfileIndex = 0;
            }
        }
        recalcProfileScroll();
    }

    private void refreshFilesForSelectedProfile() {
        String preferredPath = getSelectedFilePath();
        String profileName = getSelectedProfileName();
        if (profileName == null) {
            shareableFiles = new ArrayList<>();
            filteredShareableFiles = new ArrayList<>();
            selectedFileIndex = -1;
            previewContent = "";
            checkedFiles.clear();
            lastFileAnchorIndex = -1;
            recalcFileScroll();
            recalcPreviewScroll();
            return;
        }

        shareableFiles = new ArrayList<>(ProfileShareCodeManager.listShareableFiles(profileName));
        checkedFiles.retainAll(shareableFiles);
        applyFileFilter(preferredPath);

        recalcFileScroll();
        recalcPreviewScroll();
    }

    private void applyFileFilter(String preferredPath) {
        filteredShareableFiles = new ArrayList<>();
        for (String path : shareableFiles) {
            if (matchesFileFilter(path)) {
                filteredShareableFiles.add(path);
            }
        }

        if (filteredShareableFiles.isEmpty()) {
            selectedFileIndex = -1;
            lastFileAnchorIndex = -1;
            previewContent = "";
            previewScroll = 0;
            recalcFileScroll();
            recalcPreviewScroll();
            return;
        }

        int newIndex = preferredPath == null ? -1 : filteredShareableFiles.indexOf(preferredPath);
        if (newIndex < 0 && selectedFileIndex >= 0) {
            newIndex = Math.min(selectedFileIndex, filteredShareableFiles.size() - 1);
        }
        if (newIndex < 0) {
            newIndex = 0;
        }

        selectedFileIndex = newIndex;
        if (lastFileAnchorIndex < 0 || lastFileAnchorIndex >= filteredShareableFiles.size()) {
            lastFileAnchorIndex = selectedFileIndex;
        }
        loadPreviewForSelectedFile();
        recalcFileScroll();
        recalcPreviewScroll();
    }

    private boolean matchesFileFilter(String path) {
        String keyword = normalizeSearchText(fileSearchQuery);
        if (keyword.isEmpty()) {
            return true;
        }

        String normalizedPath = normalizeSearchText(path);
        String localizedName = normalizeSearchText(getDisplayNameForFile(path));
        String label = normalizeSearchText(getDisplayLabelForFile(path));
        return normalizedPath.contains(keyword) || localizedName.contains(keyword) || label.contains(keyword);
    }

    private String normalizeSearchText(String text) {
        if (text == null) {
            return "";
        }
        return text.replace('\\', '/').replace(" ", "").trim().toLowerCase(Locale.ROOT);
    }

    private void loadPreviewForSelectedFile() {
        String profileName = getSelectedProfileName();
        String relativePath = getSelectedFilePath();
        if (profileName == null || relativePath == null) {
            previewContent = "";
            recalcPreviewScroll();
            return;
        }

        try {
            previewContent = ProfileShareCodeManager.loadProfileFileContent(profileName, relativePath);
        } catch (Exception e) {
            previewContent = "读取失败: " + e.getMessage();
        }
        previewScroll = 0;
        recalcPreviewScroll();
    }

    private void updateButtonStates() {
        boolean hasProfile = getSelectedProfileName() != null;
        boolean isActive = hasProfile && getSelectedProfileName().equals(ProfileManager.getActiveProfileName());
        boolean isDefault = hasProfile && ProfileManager.DEFAULT_PROFILE_NAME.equals(getSelectedProfileName());
        boolean hasFile = getSelectedFilePath() != null;
        boolean hasChecked = !checkedFiles.isEmpty() || hasFile;
        boolean hasVisibleFiles = !filteredShareableFiles.isEmpty();

        btnSelect.enabled = hasProfile && !isActive;
        btnDelete.enabled = hasProfile && !isDefault;
        btnOpenEditor.enabled = hasProfile && hasFile;
        btnExportShare.enabled = hasProfile && hasChecked;
        btnImportShare.enabled = hasProfile;
        btnSelectAllFiles.enabled = hasProfile && hasVisibleFiles;
        btnClearFileSelection.enabled = !checkedFiles.isEmpty();
        btnRefresh.enabled = hasProfile;
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        switch (button.id) {
            case 0:
                if (btnSelect.enabled) {
                    ProfileManager.setActiveProfile(getSelectedProfileName());
                    reloadOtherFeatureConfigsForActiveProfile();
                    setStatus("§a已切换到配置: " + getSelectedProfileName(), 0xFF8CFF9E);
                    refreshProfiles();
                    refreshFilesForSelectedProfile();
                    updateButtonStates();
                }
                break;
            case 1:
                mc.displayGuiScreen(new GuiTextInput(this, I18n.format("gui.profile.input_new"), newName -> {
                    if (newName != null && !newName.trim().isEmpty()) {
                        if (ProfileManager.createProfile(newName.trim())) {
                            profiles = ProfileManager.getAllProfileNames();
                            selectedProfileIndex = profiles.indexOf(newName.trim());
                            checkedFiles.clear();
                            selectedFileIndex = 0;
                            lastFileAnchorIndex = 0;
                            refreshFilesForSelectedProfile();
                            setStatus("§a已创建配置: " + newName.trim(), 0xFF8CFF9E);
                        } else {
                            setStatus("§c创建失败，名称可能重复或无效", 0xFFFF8E8E);
                        }
                        updateButtonStates();
                    }
                }));
                break;
            case 2:
                if (btnDelete.enabled) {
                    String profile = getSelectedProfileName();
                    if (ProfileManager.deleteProfile(profile)) {
                        setStatus("§a已删除配置: " + profile, 0xFF8CFF9E);
                        refreshProfiles();
                        checkedFiles.clear();
                        selectedFileIndex = 0;
                        lastFileAnchorIndex = 0;
                        refreshFilesForSelectedProfile();
                        updateButtonStates();
                    } else {
                        setStatus("§c删除失败: " + profile, 0xFFFF8E8E);
                    }
                }
                break;
            case 3:
                openEditorForSelectedFile();
                break;
            case 4:
                refreshProfiles();
                refreshFilesForSelectedProfile();
                updateButtonStates();
                setStatus("§a已刷新配置视图", 0xFF8CFF9E);
                break;
            case 5:
                exportShareCode();
                break;
            case 6:
                openImportDialog();
                break;
            case 7:
                checkedFiles.clear();
                checkedFiles.addAll(filteredShareableFiles);
                updateButtonStates();
                setStatus("§a已选择当前列表中的配置文件，共 " + checkedFiles.size() + " 项", 0xFF8CFF9E);
                break;
            case 8:
                checkedFiles.clear();
                updateButtonStates();
                setStatus("§7已清空分享勾选", 0xFFB8C7D9);
                break;
            case 9:
                mc.displayGuiScreen(parentScreen);
                break;
            default:
                break;
        }
    }

    private void openEditorForSelectedFile() {
        String profileName = getSelectedProfileName();
        String relativePath = getSelectedFilePath();
        if (profileName == null || relativePath == null) {
            return;
        }
        try {
            String content = ProfileShareCodeManager.loadProfileFileContent(profileName, relativePath);
            mc.displayGuiScreen(new GuiProfileConfigEditor(this, profileName, relativePath, content));
        } catch (Exception e) {
            setStatus("§c打开编辑器失败: " + e.getMessage(), 0xFFFF8E8E);
        }
    }

    private void exportShareCode() {
        String profileName = getSelectedProfileName();
        if (profileName == null) {
            return;
        }

        List<String> exportFiles = new ArrayList<>();
        if (!checkedFiles.isEmpty()) {
            exportFiles.addAll(checkedFiles);
        } else if (getSelectedFilePath() != null) {
            exportFiles.add(getSelectedFilePath());
        }

        try {
            String shareCode = ProfileShareCodeManager.generateShareCode(profileName, exportFiles);
            setClipboardString(shareCode);
            setStatus("§a分享码已复制到剪贴板，长度: " + shareCode.length() + "，文件数: " + exportFiles.size(),
                    0xFF8CFF9E);
        } catch (Exception e) {
            setStatus("§c生成分享码失败: " + e.getMessage(), 0xFFFF8E8E);
        }
    }

    private void openImportDialog() {
        final String targetProfile = getSelectedProfileName();
        if (targetProfile == null) {
            return;
        }
        mc.displayGuiScreen(new GuiTextInput(this, "粘贴分享码到当前配置: " + targetProfile, code -> {
            if (code == null || code.trim().isEmpty()) {
                setStatus("§7已取消导入", 0xFFB8C7D9);
                return;
            }
            try {
                ProfileShareCodeManager.ImportPreview preview = ProfileShareCodeManager.previewImport(code,
                        targetProfile);
                mc.displayGuiScreen(new GuiShareImportPreview(this, targetProfile, preview));
            } catch (Exception e) {
                setStatus("§c导入失败: " + e.getMessage(), 0xFFFF8E8E);
            }
        }));
    }

    public void handleImportApplied(ProfileShareCodeManager.ImportResult result) {
        refreshFilesForSelectedProfile();
        updateButtonStates();
        if (result == null) {
            setStatus("§a导入已完成", 0xFF8CFF9E);
            return;
        }
        setStatus("§a导入完成：写入 " + result.getImportedCount()
                + " 项，替换 " + result.getReplacedFileCount()
                + " 项，合并 " + result.getMergedFileCount()
                + " 项，跳过 " + result.getUnchangedFileCount() + " 项",
                0xFF8CFF9E);
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        int dWheel = Mouse.getEventDWheel();
        if (dWheel == 0) {
            return;
        }

        int mouseX = Mouse.getEventX() * this.width / this.mc.displayWidth;
        int mouseY = this.height - Mouse.getEventY() * this.height / this.mc.displayHeight - 1;

        if (isInside(mouseX, mouseY, getProfileListX(), getListY(), getProfileListWidth(), getListHeight())) {
            if (dWheel > 0) {
                profileScroll = Math.max(0, profileScroll - 1);
            } else {
                profileScroll = Math.min(profileMaxScroll, profileScroll + 1);
            }
            return;
        }

        if (isInside(mouseX, mouseY, getFileListX(), getListY(), getFileListWidth(), getListHeight())) {
            if (dWheel > 0) {
                fileScroll = Math.max(0, fileScroll - 1);
            } else {
                fileScroll = Math.min(fileMaxScroll, fileScroll + 1);
            }
            return;
        }

        if (isInside(mouseX, mouseY, getPreviewX(), getPreviewY(), getPreviewWidth(), getPreviewHeight())) {
            if (dWheel > 0) {
                previewScroll = Math.max(0, previewScroll - 3);
            } else {
                previewScroll = Math.min(previewMaxScroll, previewScroll + 3);
            }
        }
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        if (mouseButton == 0) {
            if (profileDividerBounds != null && profileDividerBounds.contains(mouseX, mouseY)) {
                draggingProfileDivider = true;
                return;
            }
            if (fileDividerBounds != null && fileDividerBounds.contains(mouseX, mouseY)) {
                draggingFileDivider = true;
                return;
            }
        }
        super.mouseClicked(mouseX, mouseY, mouseButton);

        if (fileSearchField != null) {
            fileSearchField.mouseClicked(mouseX, mouseY, mouseButton);
            if (isInside(mouseX, mouseY, getFileSearchX(), getFileSearchY(), getFileSearchWidth(), getFileSearchHeight())) {
                return;
            }
        }

        if (mouseButton != 0) {
            return;
        }

        handleProfileListClick(mouseX, mouseY);
        handleFileListClick(mouseX, mouseY);
    }

    @Override
    protected void mouseClickMove(int mouseX, int mouseY, int clickedMouseButton, long timeSinceLastClick) {
        if (draggingProfileDivider) {
            applyProfileDividerDrag(mouseX);
            return;
        }
        if (draggingFileDivider) {
            applyFileDividerDrag(mouseX);
            return;
        }
        super.mouseClickMove(mouseX, mouseY, clickedMouseButton, timeSinceLastClick);
    }

    @Override
    protected void mouseReleased(int mouseX, int mouseY, int state) {
        if (state == 0 && (draggingProfileDivider || draggingFileDivider)) {
            draggingProfileDivider = false;
            draggingFileDivider = false;
            persistCurrentLayoutRatios();
            return;
        }
        super.mouseReleased(mouseX, mouseY, state);
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (fileSearchField != null) {
            if (keyCode == Keyboard.KEY_F && GuiScreen.isCtrlKeyDown()) {
                fileSearchField.setFocused(true);
                return;
            }

            if (fileSearchField.isFocused()) {
                if (keyCode == Keyboard.KEY_ESCAPE) {
                    fileSearchField.setFocused(false);
                    return;
                }

                String preferredPath = getSelectedFilePath();
                String before = fileSearchField.getText();
                if (fileSearchField.textboxKeyTyped(typedChar, keyCode)) {
                    fileSearchQuery = fileSearchField.getText();
                    if (!before.equals(fileSearchQuery)) {
                        applyFileFilter(preferredPath);
                        updateButtonStates();
                    }
                    return;
                }
            }
        }

        super.keyTyped(typedChar, keyCode);
    }

    private void handleProfileListClick(int mouseX, int mouseY) {
        int contentY = getListContentY();
        if (!isInside(mouseX, mouseY, getProfileListX(), contentY, getProfileListWidth(), getListHeight() - 24)) {
            return;
        }

        int localIndex = (mouseY - contentY) / getRowHeight();
        int actualIndex = profileScroll + localIndex;
        if (actualIndex >= 0 && actualIndex < profiles.size()) {
            selectedProfileIndex = actualIndex;
            checkedFiles.clear();
            selectedFileIndex = 0;
            lastFileAnchorIndex = 0;
            refreshFilesForSelectedProfile();
            updateButtonStates();
        }
    }

    private void handleFileListClick(int mouseX, int mouseY) {
        int contentY = getFileListContentY();
        if (!isInside(mouseX, mouseY, getFileListX(), contentY, getFileListWidth(), getFileListContentHeight())) {
            return;
        }

        int localIndex = (mouseY - contentY) / getRowHeight();
        int actualIndex = fileScroll + localIndex;
        if (actualIndex < 0 || actualIndex >= filteredShareableFiles.size()) {
            return;
        }

        String path = filteredShareableFiles.get(actualIndex);
        boolean shiftDown = GuiScreen.isShiftKeyDown();
        boolean ctrlDown = GuiScreen.isCtrlKeyDown();
        int anchorBeforeClick = lastFileAnchorIndex;

        int checkboxX = getFileListX() + 10;
        if (mouseX >= checkboxX && mouseX <= checkboxX + 12) {
            if (checkedFiles.contains(path)) {
                checkedFiles.remove(path);
            } else {
                checkedFiles.add(path);
            }
            selectedFileIndex = actualIndex;
            lastFileAnchorIndex = actualIndex;
            loadPreviewForSelectedFile();
            updateButtonStates();
            return;
        }

        selectedFileIndex = actualIndex;

        if (shiftDown && !filteredShareableFiles.isEmpty()) {
            int anchor = anchorBeforeClick < 0 ? actualIndex : anchorBeforeClick;
            int min = Math.min(anchor, actualIndex);
            int max = Math.max(anchor, actualIndex);
            checkedFiles.clear();
            for (int i = min; i <= max; i++) {
                checkedFiles.add(filteredShareableFiles.get(i));
            }
        } else if (ctrlDown) {
            lastFileAnchorIndex = actualIndex;
            if (checkedFiles.contains(path)) {
                checkedFiles.remove(path);
            } else {
                checkedFiles.add(path);
            }
        } else {
            checkedFiles.clear();
            checkedFiles.add(path);
            lastFileAnchorIndex = actualIndex;
        }

        if (shiftDown && anchorBeforeClick < 0) {
            lastFileAnchorIndex = actualIndex;
        }

        loadPreviewForSelectedFile();
        updateButtonStates();
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.drawDefaultBackground();

        int panelX = getPanelX();
        int panelY = getPanelY();
        int panelWidth = getPanelWidth();
        int panelHeight = getPanelHeight();

        GuiTheme.drawPanel(panelX, panelY, panelWidth, panelHeight);
        GuiTheme.drawTitleBar(panelX, panelY, panelWidth, I18n.format("gui.profile.title"), this.fontRenderer);

        GuiTheme.drawInputFrameSafe(panelX + 10, panelY + 26, panelWidth - 20, 18, false, true);
        this.drawString(this.fontRenderer, statusMessage, panelX + 14, panelY + 31, statusColor);

        drawProfileList(mouseX, mouseY);
        drawFileList(mouseX, mouseY);
        drawPreviewPanel();
        drawDividerHandle(profileDividerBounds, mouseX, mouseY, draggingProfileDivider);
        drawDividerHandle(fileDividerBounds, mouseX, mouseY, draggingFileDivider);

        String filterHint = fileSearchQuery == null || fileSearchQuery.trim().isEmpty()
                ? "§7未过滤"
                : "§b筛选: " + filteredShareableFiles.size() + "/" + shareableFiles.size();
        this.drawString(this.fontRenderer,
                "§7已勾选分享文件: " + checkedFiles.size()
                        + " | 当前配置: " + (getSelectedProfileName() == null ? "-" : getSelectedProfileName())
                        + " | " + filterHint,
                panelX + 10, panelY + panelHeight - 70, 0xFFB8C7D9);

        super.drawScreen(mouseX, mouseY, partialTicks);

        if (profileDividerBounds != null && profileDividerBounds.contains(mouseX, mouseY)) {
            drawHoveringText(java.util.Collections.singletonList("拖动调整左侧配置方案与中间文件列表宽度"), mouseX, mouseY);
            return;
        }
        if (fileDividerBounds != null && fileDividerBounds.contains(mouseX, mouseY)) {
            drawHoveringText(java.util.Collections.singletonList("拖动调整中间文件列表与右侧预览宽度"), mouseX, mouseY);
        }
    }

    private void drawProfileList(int mouseX, int mouseY) {
        int x = getProfileListX();
        int y = getListY();
        int width = getProfileListWidth();
        int height = getListHeight();

        GuiTheme.drawPanelSegment(x, y, width, height, getPanelX(), getPanelY(), getPanelWidth(), getPanelHeight());
        GuiTheme.drawSectionTitle(x + 8, y + 8, "配置方案", this.fontRenderer);

        int contentY = getListContentY();
        int visibleRows = Math.max(1, (height - 30) / getRowHeight());
        profileMaxScroll = Math.max(0, profiles.size() - visibleRows);
        profileScroll = Math.max(0, Math.min(profileScroll, profileMaxScroll));

        if (profiles.isEmpty()) {
            GuiTheme.drawEmptyState(x + width / 2, y + height / 2 - 6, I18n.format("gui.profile.empty"),
                    this.fontRenderer);
            return;
        }

        for (int i = 0; i < visibleRows; i++) {
            int index = profileScroll + i;
            if (index >= profiles.size()) {
                break;
            }

            String profile = profiles.get(index);
            int rowY = contentY + i * getRowHeight();
            boolean isActive = profile.equals(ProfileManager.getActiveProfileName());
            boolean isSelected = index == selectedProfileIndex;
            boolean hovered = isInside(mouseX, mouseY, x + 6, rowY, width - 16, getRowHeight() - 2);

            GuiTheme.drawButtonFrameSafe(x + 6, rowY, width - 18, getRowHeight() - 2,
                    isSelected ? GuiTheme.UiState.SELECTED
                            : (hovered ? GuiTheme.UiState.HOVER : GuiTheme.UiState.NORMAL));

            String displayName = profile;
            if (isActive) {
                displayName = "§a" + profile + " " + I18n.format("gui.profile.current");
            } else if (profile.equals(ProfileManager.DEFAULT_PROFILE_NAME)) {
                displayName = "§e" + profile + " " + I18n.format("gui.profile.default");
            }
            displayName = this.fontRenderer.trimStringToWidth(displayName, width - 28);
            this.drawString(this.fontRenderer, displayName, x + 12, rowY + 6, 0xFFE8F1FA);
        }

        if (profileMaxScroll > 0) {
            int thumbHeight = Math.max(18,
                    (int) ((visibleRows / (float) Math.max(visibleRows, profiles.size())) * (height - 30)));
            int track = Math.max(1, (height - 30) - thumbHeight);
            int thumbY = contentY + (int) ((profileScroll / (float) Math.max(1, profileMaxScroll)) * track);
            GuiTheme.drawScrollbar(x + width - 10, contentY, 6, height - 30, thumbY, thumbHeight);
        }
    }

    private void drawFileList(int mouseX, int mouseY) {
        int x = getFileListX();
        int y = getListY();
        int width = getFileListWidth();
        int height = getListHeight();

        GuiTheme.drawPanelSegment(x, y, width, height, getPanelX(), getPanelY(), getPanelWidth(), getPanelHeight());
        GuiTheme.drawSectionTitle(x + 8, y + 8, "配置文件（Ctrl/Shift 多选，点击复选框分享）", this.fontRenderer);

        GuiTheme.drawInputFrameSafe(getFileSearchX(), getFileSearchY(), getFileSearchWidth(), getFileSearchHeight(),
                fileSearchField != null && fileSearchField.isFocused(), true);
        if (fileSearchField != null) {
            fileSearchField.drawTextBox();
            if ((fileSearchField.getText() == null || fileSearchField.getText().isEmpty()) && !fileSearchField.isFocused()) {
                this.drawString(this.fontRenderer, "§7搜索文件名 / 中文名 / 路径，Ctrl+F 快速聚焦",
                        getFileSearchX() + 5, getFileSearchY() + 5, 0xFF7D8A9A);
            }
        }

        this.drawString(this.fontRenderer,
                "§7显示 " + filteredShareableFiles.size() + " / " + shareableFiles.size(),
                x + width - 84, y + 8, 0xFF9EB3C9);

        int contentY = getFileListContentY();
        int contentHeight = getFileListContentHeight();
        int visibleRows = Math.max(1, contentHeight / getRowHeight());
        fileMaxScroll = Math.max(0, filteredShareableFiles.size() - visibleRows);
        fileScroll = Math.max(0, Math.min(fileScroll, fileMaxScroll));

        if (filteredShareableFiles.isEmpty()) {
            String emptyText = shareableFiles.isEmpty() ? "该配置下暂无可分享文件" : "没有匹配当前搜索条件的配置文件";
            GuiTheme.drawEmptyState(x + width / 2, y + height / 2 - 6, emptyText, this.fontRenderer);
            return;
        }

        for (int i = 0; i < visibleRows; i++) {
            int index = fileScroll + i;
            if (index >= filteredShareableFiles.size()) {
                break;
            }

            String path = filteredShareableFiles.get(index);
            int rowY = contentY + i * getRowHeight();
            boolean isSelected = index == selectedFileIndex;
            boolean isChecked = checkedFiles.contains(path);
            boolean hovered = isInside(mouseX, mouseY, x + 6, rowY, width - 16, getRowHeight() - 2);

            GuiTheme.drawButtonFrameSafe(x + 6, rowY, width - 18, getRowHeight() - 2,
                    isSelected ? GuiTheme.UiState.SELECTED
                            : (hovered ? GuiTheme.UiState.HOVER
                                    : (isChecked ? GuiTheme.UiState.SUCCESS : GuiTheme.UiState.NORMAL)));

            drawRect(x + 10, rowY + 5, x + 22, rowY + 17, isChecked ? 0xFF3A9F5B : 0xFF2B3440);
            drawRect(x + 11, rowY + 6, x + 21, rowY + 16, isChecked ? 0xFF67D58B : 0xFF17202A);
            if (isChecked) {
                this.drawString(this.fontRenderer, "√", x + 13, rowY + 6, 0xFFFFFFFF);
            }

            String clipped = this.fontRenderer.trimStringToWidth(getDisplayLabelForFile(path), width - 40);
            this.drawString(this.fontRenderer, clipped, x + 28, rowY + 6, 0xFFE8F1FA);
        }

        if (fileMaxScroll > 0) {
            int thumbHeight = Math.max(18,
                    (int) ((visibleRows / (float) Math.max(visibleRows, filteredShareableFiles.size())) * contentHeight));
            int track = Math.max(1, contentHeight - thumbHeight);
            int thumbY = contentY + (int) ((fileScroll / (float) Math.max(1, fileMaxScroll)) * track);
            GuiTheme.drawScrollbar(x + width - 10, contentY, 6, contentHeight, thumbY, thumbHeight);
        }
    }

    private void drawPreviewPanel() {
        int x = getPreviewX();
        int y = getPreviewY();
        int width = getPreviewWidth();
        int height = getPreviewHeight();

        GuiTheme.drawPanelSegment(x, y, width, height, getPanelX(), getPanelY(), getPanelWidth(), getPanelHeight());
        GuiTheme.drawSectionTitle(x + 8, y + 8, "内容预览 / 单文件编辑保存", this.fontRenderer);

        if (getSelectedFilePath() != null) {
            this.drawString(this.fontRenderer,
                    "§7当前文件: " + getDisplayNameForFile(getSelectedFilePath()) + " (" + getSelectedFilePath() + ")",
                    x + 8, y + 20, 0xFFB8C7D9);
        } else {
            this.drawString(this.fontRenderer, "§7请先在中间列表选择一个配置文件", x + 8, y + 20, 0xFFB8C7D9);
        }

        int contentX = x + 8;
        int contentY = y + 36;
        int contentW = width - 18;
        int contentH = height - 44;

        GuiTheme.drawInputFrameSafe(contentX, contentY, contentW, contentH, false, true);

        List<String> lines = splitPreviewLines(previewContent);
        int visibleLines = Math.max(1, (contentH - 8) / 10);
        previewMaxScroll = Math.max(0, lines.size() - visibleLines);
        previewScroll = Math.max(0, Math.min(previewScroll, previewMaxScroll));

        for (int i = 0; i < visibleLines; i++) {
            int index = previewScroll + i;
            if (index >= lines.size()) {
                break;
            }
            String line = this.fontRenderer.trimStringToWidth(lines.get(index), contentW - 10);
            this.drawString(this.fontRenderer, line, contentX + 4, contentY + 4 + i * 10, 0xFFE8F1FA);
        }

        if (previewMaxScroll > 0) {
            int thumbHeight = Math.max(18,
                    (int) ((visibleLines / (float) Math.max(visibleLines, lines.size())) * contentH));
            int track = Math.max(1, contentH - thumbHeight);
            int thumbY = contentY + (int) ((previewScroll / (float) Math.max(1, previewMaxScroll)) * track);
            GuiTheme.drawScrollbar(contentX + contentW - 6, contentY, 4, contentH, thumbY, thumbHeight);
        }
    }

    private List<String> splitPreviewLines(String content) {
        List<String> result = new ArrayList<>();
        String normalized = content == null ? "" : content.replace("\r\n", "\n").replace('\r', '\n');
        String[] split = normalized.split("\n", -1);
        for (String line : split) {
            result.add(line);
        }
        if (result.isEmpty()) {
            result.add("");
        }
        return result;
    }

    private String getDisplayLabelForFile(String path) {
        String normalized = path == null ? "" : path.replace('\\', '/');
        String displayName = getDisplayNameForFile(path);
        if (displayName.equalsIgnoreCase(normalized)) {
            return normalized;
        }
        return displayName + " §7(" + normalized + ")";
    }

    private String getDisplayNameForFile(String path) {
        return ProfileShareCodeManager.getDisplayNameForPath(path);
    }

    private void recalcProfileScroll() {
        profileScroll = Math.max(0, profileScroll);
    }

    private void recalcFileScroll() {
        fileScroll = Math.max(0, fileScroll);
    }

    private void recalcPreviewScroll() {
        previewScroll = Math.max(0, previewScroll);
    }

    private String getSelectedProfileName() {
        if (selectedProfileIndex < 0 || selectedProfileIndex >= profiles.size()) {
            return null;
        }
        return profiles.get(selectedProfileIndex);
    }

    private String getSelectedFilePath() {
        if (selectedFileIndex < 0 || selectedFileIndex >= filteredShareableFiles.size()) {
            return null;
        }
        return filteredShareableFiles.get(selectedFileIndex);
    }

    private void setStatus(String message, int color) {
        this.statusMessage = message == null ? "" : message;
        this.statusColor = color;
    }

    private boolean isInside(int mouseX, int mouseY, int x, int y, int width, int height) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }

    private int[] getColumnWidths() {
        int total = getPanelWidth() - 40;
        int[] minimums = getColumnMinimums(total);
        int profileMin = minimums[0];
        int fileMin = minimums[1];
        int previewMin = minimums[2];

        int maxProfileWidth = Math.max(profileMin, total - fileMin - previewMin);
        int profileWidth = Math.max(profileMin,
                Math.min((int) Math.round(total * savedProfileColumnRatio), maxProfileWidth));
        int remainingAfterProfile = Math.max(1, total - profileWidth);

        int preferredFileWidth = getPreferredFileListWidth();
        int suggestedFileWidth = Math.max(fileMin, Math.min(preferredFileWidth, remainingAfterProfile - previewMin));
        int maxFileWidth = Math.max(fileMin, remainingAfterProfile - previewMin);
        int fileWidth = Math.max(fileMin,
                Math.min(Math.max((int) Math.round(remainingAfterProfile * savedFileColumnRatio), suggestedFileWidth),
                        maxFileWidth));

        int previewWidth = total - profileWidth - fileWidth;
        if (previewWidth < previewMin) {
            int need = previewMin - previewWidth;
            int reducibleFile = Math.max(0, fileWidth - fileMin);
            int reduceFile = Math.min(need, reducibleFile);
            fileWidth -= reduceFile;
            previewWidth += reduceFile;
            need -= reduceFile;
            if (need > 0) {
                int reducibleProfile = Math.max(0, profileWidth - profileMin);
                int reduceProfile = Math.min(need, reducibleProfile);
                profileWidth -= reduceProfile;
                previewWidth += reduceProfile;
            }
        }
        return new int[] { profileWidth, fileWidth, total - profileWidth - fileWidth };
    }

    private int[] getColumnMinimums(int total) {
        int profilePreferred = Math.max(120, Math.min(MIN_PROFILE_COLUMN_WIDTH, Math.max(120, total / 4)));
        int filePreferred = Math.max(170, Math.min(MIN_FILE_COLUMN_WIDTH, Math.max(170, total / 3)));
        int previewPreferred = Math.max(220, Math.min(MIN_PREVIEW_COLUMN_WIDTH, Math.max(220, total / 3)));
        return fitWidthsToTotal(total,
                new int[] { profilePreferred, filePreferred, previewPreferred },
                new int[] { Math.min(profilePreferred, 96), Math.min(filePreferred, 132), Math.min(previewPreferred, 168) });
    }

    private int[] fitWidthsToTotal(int totalWidth, int[] preferredWidths, int[] floorWidths) {
        int count = Math.min(preferredWidths.length, floorWidths.length);
        int[] result = new int[count];
        if (count == 0 || totalWidth <= 0) {
            return result;
        }

        int preferredSum = 0;
        int floorSum = 0;
        for (int i = 0; i < count; i++) {
            int floor = Math.max(1, floorWidths[i]);
            int preferred = Math.max(floor, preferredWidths[i]);
            floorWidths[i] = floor;
            result[i] = preferred;
            preferredSum += preferred;
            floorSum += floor;
        }

        if (preferredSum <= totalWidth) {
            return result;
        }

        if (floorSum >= totalWidth) {
            return scaleWidthsToTotal(totalWidth, floorWidths);
        }

        int overflow = preferredSum - totalWidth;
        while (overflow > 0) {
            int reducibleTotal = 0;
            for (int i = 0; i < count; i++) {
                reducibleTotal += Math.max(0, result[i] - floorWidths[i]);
            }
            if (reducibleTotal <= 0) {
                break;
            }

            int reducedThisPass = 0;
            for (int i = 0; i < count && overflow > 0; i++) {
                int reducible = Math.max(0, result[i] - floorWidths[i]);
                if (reducible <= 0) {
                    continue;
                }
                int reduce = Math.max(1, (int) Math.floor(overflow * (reducible / (double) reducibleTotal)));
                reduce = Math.min(reduce, reducible);
                result[i] -= reduce;
                overflow -= reduce;
                reducedThisPass += reduce;
            }

            if (reducedThisPass <= 0) {
                break;
            }
        }

        while (overflow > 0) {
            boolean reduced = false;
            for (int i = count - 1; i >= 0 && overflow > 0; i--) {
                if (result[i] > floorWidths[i]) {
                    result[i]--;
                    overflow--;
                    reduced = true;
                }
            }
            if (!reduced) {
                break;
            }
        }

        return result;
    }

    private int[] scaleWidthsToTotal(int totalWidth, int[] basisWidths) {
        int[] result = new int[basisWidths.length];
        if (basisWidths.length == 0 || totalWidth <= 0) {
            return result;
        }

        int basisSum = 0;
        for (int width : basisWidths) {
            basisSum += Math.max(1, width);
        }
        if (basisSum <= 0) {
            basisSum = basisWidths.length;
        }

        int used = 0;
        double[] fractions = new double[basisWidths.length];
        for (int i = 0; i < basisWidths.length; i++) {
            double scaled = Math.max(1, basisWidths[i]) * (double) totalWidth / (double) basisSum;
            int width = Math.max(1, (int) Math.floor(scaled));
            result[i] = width;
            fractions[i] = scaled - width;
            used += width;
        }

        while (used > totalWidth) {
            int index = indexOfLargestWidth(result);
            if (index < 0 || result[index] <= 1) {
                break;
            }
            result[index]--;
            used--;
        }

        while (used < totalWidth) {
            int index = indexOfLargestFraction(fractions);
            if (index < 0) {
                index = indexOfLargestWidth(basisWidths);
            }
            if (index < 0) {
                break;
            }
            result[index]++;
            fractions[index] = 0.0D;
            used++;
        }

        return result;
    }

    private int indexOfLargestFraction(double[] values) {
        int index = -1;
        double best = Double.NEGATIVE_INFINITY;
        for (int i = 0; i < values.length; i++) {
            if (values[i] > best) {
                best = values[i];
                index = i;
            }
        }
        return index;
    }

    private int indexOfLargestWidth(int[] values) {
        int index = -1;
        int best = Integer.MIN_VALUE;
        for (int i = 0; i < values.length; i++) {
            if (values[i] > best) {
                best = values[i];
                index = i;
            }
        }
        return index;
    }

    private int getPreferredFileListWidth() {
        int preferred = 220;
        if (this.fontRenderer != null) {
            for (String path : shareableFiles) {
                preferred = Math.max(preferred, this.fontRenderer.getStringWidth(getDisplayLabelForFile(path)) + 42);
            }
        }
        return preferred;
    }

    private int getPanelWidth() {
        return Math.min(1180, this.width - 12);
    }

    private int getPanelHeight() {
        return Math.min(680, this.height - 12);
    }

    private int getPanelX() {
        return (this.width - getPanelWidth()) / 2;
    }

    private int getPanelY() {
        return (this.height - getPanelHeight()) / 2;
    }

    private int getListY() {
        return getPanelY() + 50;
    }

    private int getListContentY() {
        return getListY() + 24;
    }

    private int getListHeight() {
        return getPanelHeight() - 132;
    }

    private int getProfileListX() {
        return getPanelX() + 10;
    }

    private int getProfileListWidth() {
        return getColumnWidths()[0];
    }

    private int getFileListX() {
        return getProfileListX() + getProfileListWidth() + 10;
    }

    private int getFileListWidth() {
        return getColumnWidths()[1];
    }

    private int getPreviewX() {
        return getFileListX() + getFileListWidth() + 10;
    }

    private int getPreviewY() {
        return getListY();
    }

    private int getPreviewWidth() {
        return getColumnWidths()[2];
    }

    private int getPreviewHeight() {
        return getListHeight();
    }

    private int getRowHeight() {
        return 20;
    }

    private int getFileSearchX() {
        return getFileListX() + 8;
    }

    private int getFileSearchY() {
        return getListY() + 24;
    }

    private int getFileSearchWidth() {
        return getFileListWidth() - 18;
    }

    private int getFileSearchHeight() {
        return 18;
    }

    private int getFileListContentY() {
        return getFileSearchY() + 22;
    }

    private int getFileListContentHeight() {
        return getListHeight() - 52;
    }

    private void reloadOtherFeatureConfigsForActiveProfile() {
        SpeedHandler.loadConfig();
        MovementFeatureManager.loadConfig();
        BlockFeatureManager.loadConfig();
        WorldFeatureManager.loadConfig();
        RenderFeatureManager.loadConfig();
        ItemFeatureManager.loadConfig();
        MiscFeatureManager.loadConfig();
    }
}
