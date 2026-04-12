package com.zszl.zszlScriptMod.gui.config;

import com.zszl.zszlScriptMod.gui.components.ThemedGuiScreen;

import net.minecraft.client.gui.*;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.ResourceLocation;

import net.minecraftforge.fml.client.config.GuiSlider;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

import com.zszl.zszlScriptMod.GlobalEventListener;
import com.zszl.zszlScriptMod.config.ChatOptimizationConfig;
import com.zszl.zszlScriptMod.gui.packet.PacketInterceptConfig;
import com.zszl.zszlScriptMod.gui.CustomGuiNewChat;
import com.zszl.zszlScriptMod.gui.components.GuiTheme;
import com.zszl.zszlScriptMod.gui.components.ThemedButton;
import com.zszl.zszlScriptMod.gui.components.GuiTextInput;
import com.zszl.zszlScriptMod.gui.components.ToggleGuiButton;
import com.zszl.zszlScriptMod.utils.TextureManagerHelper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GuiChatOptimization extends ThemedGuiScreen {
    private static final int BTN_GROUP_PREV = 200;
    private static final int BTN_GROUP_NEXT = 201;

    private final GuiScreen parentScreen;
    private ChatOptimizationConfig settings;

    // --- 预览框相关 ---
    private List<ITextComponent> exampleChat = new ArrayList<>();
    private boolean dragging = false;
    private int chatLeft, chatRight, chatTop, chatBottom, dragStartX, dragStartY;
    private GuiButton imageQualityButton;

    // --- UI 控件 ---
    private final List<GuiButton> scrollableButtons = new ArrayList<>();
    private final List<GuiTextField> textFields = new ArrayList<>();
    private final List<GuiSlider> sliders = new ArrayList<>();
    private final Map<GuiButton, String> tooltips = new HashMap<>();
    private GuiTextField blacklistField, whitelistField, antiSpamThresholdField;
    private GuiSlider scaleSlider, widthSlider;
    private GuiSlider backgroundTransparencySlider;
    private GuiTextField backgroundImagePathField;
    private GuiTextField backgroundScaleField;
    private GuiTextField backgroundCropXField;
    private GuiTextField backgroundCropYField;
    private GuiTextField timedMessageIntervalField;
    private GuiButton timedMessageModeButton;
    private GuiButton groupPrevButton;
    private GuiButton groupNextButton;

    // --- 滚动面板相关 ---
    private int scrollOffset = 0;
    private int maxScroll = 0;
    private int contentHeight = 0;
    private int panelTopY, panelBottomY, panelX, panelWidth;
    private int viewportHeight;
    private boolean isDraggingScrollbar = false;
    private int contentViewportTop;
    private int groupBarX;
    private int groupBarY;
    private int groupBarW;
    private int groupBarH;
    private int groupTabsX;
    private int groupTabsY;
    private int groupTabsW;
    private int groupTabsH;
    private int groupTabScroll = 0;
    private int groupTabMaxScroll = 0;
    private int groupTabContentWidth = 0;
    private int timedListX = -2000;
    private int timedListY = -2000;
    private int timedListW = 0;
    private int timedListH = 0;

    // !! 新增：定时消息列表UI相关 !!
    private int selectedTimedMessageIndex = -1;
    private int timedMessageScrollOffset = 0;
    private int maxTimedMessageScroll = 0;
    private boolean isDraggingTimedMessageScrollbar = false;
    private ConfigGroup selectedGroup = ConfigGroup.BASIC;

    public GuiChatOptimization(GuiScreen parent) {
        this.parentScreen = parent;
        this.settings = ChatOptimizationConfig.INSTANCE;
        exampleChat.add(new TextComponentString(I18n.format("gui.chatopt.preview.line1")));
        exampleChat.add(new TextComponentString(I18n.format("gui.chatopt.preview.line2")));
        exampleChat.add(new TextComponentString(I18n.format("gui.chatopt.preview.line3")));
    }

    @Override
    public void initGui() {
        this.buttonList.clear();
        this.scrollableButtons.clear();
        this.textFields.clear();
        this.sliders.clear();
        this.tooltips.clear();
        Keyboard.enableRepeatEvents(true);

        if (mc.ingameGUI.getChatGUI() instanceof CustomGuiNewChat) {
            ((CustomGuiNewChat) mc.ingameGUI.getChatGUI()).configuring = true;
        }

        panelWidth = Math.min(560, Math.max(360, this.width - 24));
        panelX = (this.width - panelWidth) / 2;
        panelTopY = 32;
        panelBottomY = this.height - 55;
        if (panelBottomY - panelTopY < 180) {
            panelTopY = Math.max(16, panelBottomY - 180);
        }
        groupBarX = panelX + 10;
        groupBarY = panelTopY + 26;
        groupBarW = panelWidth - 20;
        groupBarH = 28;
        groupTabsX = groupBarX + 32;
        groupTabsY = groupBarY + 4;
        groupTabsW = groupBarW - 64;
        groupTabsH = 20;
        contentViewportTop = groupBarY + groupBarH + 8;
        viewportHeight = Math.max(80, panelBottomY - contentViewportTop - 4);

        int currentY = 32;
        int controlX = 10;
        int controlWidth = panelWidth - 20;
        int halfWidth = (controlWidth - 10) / 2;
        int labelSpacing = 12;
        int controlSpacing = 25;

        // --- 在滚动区域内布局所有控件 ---
        ToggleGuiButton btnSmartCopy = new ToggleGuiButton(0, controlX, currentY, halfWidth, 20,
                I18n.format("gui.chatopt.smart_copy"),
                settings.enableSmartCopy);
        scrollableButtons.add(btnSmartCopy);
        tooltips.put(btnSmartCopy, I18n.format("gui.chatopt.tip.smart_copy"));

        ToggleGuiButton btnCopyWithFormatting = new ToggleGuiButton(1, controlX + halfWidth + 10, currentY, halfWidth,
                20, I18n.format("gui.chatopt.copy_with_format"), settings.copyWithFormatting);
        scrollableButtons.add(btnCopyWithFormatting);
        tooltips.put(btnCopyWithFormatting, I18n.format("gui.chatopt.tip.copy_with_format"));
        currentY += controlSpacing;

        ToggleGuiButton btnAntiSpam = new ToggleGuiButton(2, controlX, currentY, halfWidth, 20,
                I18n.format("gui.chatopt.anti_spam"),
                settings.enableAntiSpam);
        scrollableButtons.add(btnAntiSpam);
        tooltips.put(btnAntiSpam, I18n.format("gui.chatopt.tip.anti_spam"));

        ToggleGuiButton btnTimestamp = new ToggleGuiButton(3, controlX + halfWidth + 10, currentY, halfWidth, 20,
                I18n.format("gui.chatopt.timestamp"), settings.enableTimestamp);
        scrollableButtons.add(btnTimestamp);
        tooltips.put(btnTimestamp, I18n.format("gui.chatopt.tip.timestamp"));
        currentY += controlSpacing;

        ToggleGuiButton btnAntiSpamScroll = new ToggleGuiButton(4, controlX, currentY, halfWidth, 20,
                I18n.format("gui.chatopt.scroll_bottom"),
                settings.antiSpamScrollToBottom);
        scrollableButtons.add(btnAntiSpamScroll);
        tooltips.put(btnAntiSpamScroll, I18n.format("gui.chatopt.tip.scroll_bottom"));

        antiSpamThresholdField = new GuiTextField(5, this.fontRenderer, controlX + controlWidth - 40, currentY, 40, 20);
        antiSpamThresholdField.setText(String.valueOf(settings.antiSpamThresholdSeconds));
        textFields.add(antiSpamThresholdField);
        currentY += controlSpacing;

        // !! 新增：定时发送消息UI !!
        ToggleGuiButton btnTimedMessage = new ToggleGuiButton(16, controlX, currentY, halfWidth, 20,
                I18n.format("gui.chatopt.timed_message"),
                settings.enableTimedMessage);
        scrollableButtons.add(btnTimedMessage);
        tooltips.put(btnTimedMessage, I18n.format("gui.chatopt.tip.timed_message"));

        timedMessageModeButton = new ThemedButton(19, controlX + halfWidth + 10, currentY, halfWidth, 20,
                I18n.format("gui.chatopt.mode", settings.timedMessageMode.getDisplayName()));
        scrollableButtons.add(timedMessageModeButton);
        tooltips.put(timedMessageModeButton, I18n.format("gui.chatopt.tip.mode"));
        currentY += controlSpacing;

        timedMessageIntervalField = new GuiTextField(17, this.fontRenderer, controlX + controlWidth - 40, currentY, 40,
                20);
        timedMessageIntervalField.setText(String.valueOf(settings.timedMessageIntervalSeconds));
        textFields.add(timedMessageIntervalField);
        currentY += controlSpacing + labelSpacing;

        // 定时消息列表和操作按钮
        int listHeight = 80;
        int listButtonsY = currentY + listHeight + 5;
        int listButtonWidth = (controlWidth - 20) / 3;
        scrollableButtons.add(new ThemedButton(20, controlX, listButtonsY, listButtonWidth, 20,
                "§a" + I18n.format("gui.auto_skill.add")));
        scrollableButtons
                .add(new ThemedButton(21, controlX + listButtonWidth + 10, listButtonsY, listButtonWidth, 20,
                        "§e" + I18n.format("gui.auto_skill.edit")));
        scrollableButtons.add(
                new ThemedButton(22, controlX + 2 * (listButtonWidth + 10), listButtonsY, listButtonWidth, 20,
                        "§c" + I18n.format("gui.auto_skill.delete")));
        currentY = listButtonsY + controlSpacing;
        // !! 新增结束 !!

        ToggleGuiButton btnBlacklist = new ToggleGuiButton(6, controlX, currentY, halfWidth, 20,
                I18n.format("gui.chatopt.enable_blacklist"),
                settings.enableBlacklist);
        scrollableButtons.add(btnBlacklist);
        tooltips.put(btnBlacklist, I18n.format("gui.chatopt.tip.enable_blacklist"));

        ToggleGuiButton btnWhitelist = new ToggleGuiButton(7, controlX + halfWidth + 10, currentY, halfWidth, 20,
                I18n.format("gui.chatopt.enable_whitelist"), settings.enableWhitelist);
        scrollableButtons.add(btnWhitelist);
        tooltips.put(btnWhitelist, I18n.format("gui.chatopt.tip.enable_whitelist"));
        currentY += controlSpacing + labelSpacing;

        blacklistField = new GuiTextField(8, this.fontRenderer, controlX, currentY, controlWidth, 20);
        blacklistField.setMaxStringLength(Integer.MAX_VALUE);
        blacklistField.setText(String.join(", ", settings.blacklist));
        textFields.add(blacklistField);
        currentY += controlSpacing + labelSpacing;

        whitelistField = new GuiTextField(9, this.fontRenderer, controlX, currentY, controlWidth, 20);
        whitelistField.setMaxStringLength(Integer.MAX_VALUE);
        whitelistField.setText(String.join(", ", settings.whitelist));
        textFields.add(whitelistField);
        currentY += controlSpacing + 5;

        ToggleGuiButton btnSmooth = new ToggleGuiButton(11, controlX, currentY, controlWidth, 20,
                I18n.format("gui.chatopt.smooth",
                        I18n.format(settings.smooth ? "gui.auto_skill.state_on" : "gui.auto_skill.state_off")),
                settings.smooth);
        scrollableButtons.add(btnSmooth);
        tooltips.put(btnSmooth, I18n.format("gui.chatopt.tip.smooth"));
        currentY += controlSpacing;

        backgroundTransparencySlider = new GuiSlider(10, controlX, currentY, controlWidth, 20,
                I18n.format("gui.chatopt.bg_alpha"), "%", 0, 100,
                settings.backgroundTransparencyPercent, false, true);
        sliders.add(backgroundTransparencySlider);
        tooltips.put(backgroundTransparencySlider, I18n.format("gui.chatopt.tip.bg_alpha"));
        currentY += controlSpacing + labelSpacing;

        int pathFieldWidth = controlWidth - 110;
        backgroundImagePathField = new GuiTextField(14, this.fontRenderer, controlX, currentY, pathFieldWidth, 20);
        backgroundImagePathField.setMaxStringLength(Integer.MAX_VALUE);
        backgroundImagePathField.setText(settings.backgroundImagePath);
        textFields.add(backgroundImagePathField);

        imageQualityButton = new ThemedButton(15, controlX + pathFieldWidth + 10, currentY, 100, 20,
                I18n.format("gui.chatopt.quality", settings.imageQuality.getDisplayName()));
        scrollableButtons.add(imageQualityButton);
        tooltips.put(imageQualityButton,
                I18n.format("gui.chatopt.tip.quality", settings.imageQuality.getDescription()));
        currentY += controlSpacing;

        int fieldW_s = (controlWidth - 8) / 3;
        backgroundScaleField = new GuiTextField(23, this.fontRenderer, controlX, currentY, fieldW_s, 20);
        backgroundScaleField.setMaxStringLength(Integer.MAX_VALUE);
        backgroundScaleField.setText(String.valueOf(settings.backgroundImageScale));
        textFields.add(backgroundScaleField);

        backgroundCropXField = new GuiTextField(24, this.fontRenderer, controlX + fieldW_s + 4, currentY, fieldW_s, 20);
        backgroundCropXField.setMaxStringLength(Integer.MAX_VALUE);
        backgroundCropXField.setText(String.valueOf(settings.backgroundCropX));
        textFields.add(backgroundCropXField);

        backgroundCropYField = new GuiTextField(25, this.fontRenderer, controlX + 2 * (fieldW_s + 4), currentY,
                fieldW_s, 20);
        backgroundCropYField.setMaxStringLength(Integer.MAX_VALUE);
        backgroundCropYField.setText(String.valueOf(settings.backgroundCropY));
        textFields.add(backgroundCropYField);
        currentY += controlSpacing;

        scaleSlider = new GuiSlider(12, controlX, currentY, controlWidth, 20, I18n.format("gui.chatopt.scale"), "%", 0,
                100,
                this.mc.gameSettings.chatScale * 100, false, true);
        sliders.add(scaleSlider);
        currentY += controlSpacing;

        widthSlider = new GuiSlider(13, controlX, currentY, controlWidth, 20, I18n.format("gui.chatopt.width"), "px",
                40, 320,
                GuiNewChat.calculateChatboxWidth(this.mc.gameSettings.chatWidth), false, true);
        sliders.add(widthSlider);
        currentY += controlSpacing;

        int bottomButtonY = panelBottomY + 10;
        this.buttonList.add(new ThemedButton(100, panelX, bottomButtonY, (panelWidth - 20) / 3, 20,
                "§a" + I18n.format("gui.common.save_and_close")));
        this.buttonList.add(new ThemedButton(101, panelX + 10 + (panelWidth - 20) / 3, bottomButtonY,
                (panelWidth - 20) / 3, 20, "§c" + I18n.format("gui.chatopt.reset_all")));
        this.buttonList.add(new ThemedButton(102, panelX + 20 + 2 * ((panelWidth - 20) / 3), bottomButtonY,
                (panelWidth - 20) / 3, 20, I18n.format("gui.common.cancel")));

        groupPrevButton = new ThemedButton(BTN_GROUP_PREV, groupBarX + 6, groupTabsY, 20, groupTabsH, "<");
        groupNextButton = new ThemedButton(BTN_GROUP_NEXT, groupBarX + groupBarW - 26, groupTabsY, 20, groupTabsH, ">");
        this.buttonList.add(groupPrevButton);
        this.buttonList.add(groupNextButton);

        recalcGroupTabs();
        relayoutGroupedContent();
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        if (button instanceof ToggleGuiButton) {
            ((ToggleGuiButton) button).setEnabledState(!((ToggleGuiButton) button).getEnabledState());
        } else {
            switch (button.id) {
                case BTN_GROUP_PREV:
                    selectGroupByOffset(-1);
                    return;
                case BTN_GROUP_NEXT:
                    selectGroupByOffset(1);
                    return;
                case 15:
                    settings.imageQuality = settings.imageQuality.next();
                    imageQualityButton.displayString = I18n.format("gui.chatopt.quality",
                            settings.imageQuality.getDisplayName());
                    tooltips.put(imageQualityButton,
                            I18n.format("gui.chatopt.tip.quality", settings.imageQuality.getDescription()));
                    TextureManagerHelper.clearCache();
                    break;
                case 100: // 保存
                    for (GuiButton btn : scrollableButtons) {
                        if (btn instanceof ToggleGuiButton) {
                            ToggleGuiButton tBtn = (ToggleGuiButton) btn;
                            switch (tBtn.id) {
                                case 0:
                                    settings.enableSmartCopy = tBtn.getEnabledState();
                                    break;
                                case 1:
                                    settings.copyWithFormatting = tBtn.getEnabledState();
                                    break;
                                case 2:
                                    settings.enableAntiSpam = tBtn.getEnabledState();
                                    break;
                                case 3:
                                    settings.enableTimestamp = tBtn.getEnabledState();
                                    break;
                                case 4:
                                    settings.antiSpamScrollToBottom = tBtn.getEnabledState();
                                    break;
                                case 6:
                                    settings.enableBlacklist = tBtn.getEnabledState();
                                    break;
                                case 7:
                                    settings.enableWhitelist = tBtn.getEnabledState();
                                    break;
                                case 11:
                                    settings.smooth = tBtn.getEnabledState();
                                    break;
                                case 16:
                                    settings.enableTimedMessage = tBtn.getEnabledState();
                                    break;
                            }
                        }
                    }

                    String oldPath = ChatOptimizationConfig.INSTANCE.backgroundImagePath;
                    String newPath = TextureManagerHelper
                            .canonicalizeImagePath(backgroundImagePathField.getText().trim());
                    if (!oldPath.equals(newPath)) {
                        TextureManagerHelper.unloadTexture(oldPath);
                    }
                    backgroundImagePathField.setText(newPath);
                    settings.backgroundImagePath = newPath;
                    settings.imageQuality = ChatOptimizationConfig.INSTANCE.imageQuality;
                    settings.backgroundImageScale = Math.max(10,
                            Math.min(300, parseIntOr(backgroundScaleField.getText(), 100)));
                    settings.backgroundCropX = Math.max(0, parseIntOr(backgroundCropXField.getText(), 0));
                    settings.backgroundCropY = Math.max(0, parseIntOr(backgroundCropYField.getText(), 0));
                    settings.backgroundTransparencyPercent = backgroundTransparencySlider.getValueInt();
                    TextureManagerHelper.clearCache();

                    settings.blacklist = new ArrayList<>(
                            Arrays.asList(blacklistField.getText().replace("，", ",").split("\\s*,\\s*")));
                    settings.whitelist = new ArrayList<>(
                            Arrays.asList(whitelistField.getText().replace("，", ",").split("\\s*,\\s*")));
                    try {
                        settings.antiSpamThresholdSeconds = Integer.parseInt(antiSpamThresholdField.getText());
                    } catch (NumberFormatException e) {
                        settings.antiSpamThresholdSeconds = 15;
                    }

                    try {
                        settings.timedMessageIntervalSeconds = Integer.parseInt(timedMessageIntervalField.getText());
                        if (settings.timedMessageIntervalSeconds < 1)
                            settings.timedMessageIntervalSeconds = 1; // 最小间隔1秒
                    } catch (NumberFormatException e) {
                        settings.timedMessageIntervalSeconds = 60;
                    }

                    if (settings.enableTimedMessage) {
                        GlobalEventListener.timedMessageTickCounter = 0;
                    }

                    ChatOptimizationConfig.save();

                    // 智能复制开关联动内置数据包拦截规则
                    PacketInterceptConfig.load();
                    PacketInterceptConfig.ensureBuiltinRules();
                    PacketInterceptConfig.save();

                    this.mc.displayGuiScreen(this.parentScreen);
                    break;
                case 101: // 重置
                    ChatOptimizationConfig.resetToDefaults();
                    this.settings = ChatOptimizationConfig.INSTANCE;
                    this.mc.gameSettings.chatScale = 1.0f;
                    this.mc.gameSettings.chatWidth = 1.0f;
                    this.initGui();
                    break;
                case 102: // 取消
                    ChatOptimizationConfig.load();
                    this.mc.gameSettings.loadOptions();
                    this.mc.displayGuiScreen(this.parentScreen);
                    break;

                case 19: // 切换定时消息模式
                    settings.timedMessageMode = settings.timedMessageMode.next();
                    timedMessageModeButton.displayString = I18n.format("gui.chatopt.mode",
                            settings.timedMessageMode.getDisplayName());
                    break;
                case 20: // 添加定时消息
                    mc.displayGuiScreen(new GuiTextInput(this, I18n.format("gui.chatopt.input_new"), (newMessage) -> {
                        if (newMessage != null && !newMessage.trim().isEmpty()) {
                            if (settings.timedMessages.size() == 1 && settings.timedMessages.get(0).isEmpty()) {
                                settings.timedMessages.clear();
                            }
                            settings.timedMessages.add(newMessage);
                        }
                        mc.displayGuiScreen(this);
                    }));
                    break;
                case 21: // 编辑定时消息
                    if (selectedTimedMessageIndex != -1) {
                        String oldMessage = settings.timedMessages.get(selectedTimedMessageIndex);
                        mc.displayGuiScreen(new GuiTextInput(this, I18n.format("gui.chatopt.input_edit"), oldMessage,
                                (editedMessage) -> {
                                    if (editedMessage != null && !editedMessage.trim().isEmpty()) {
                                        settings.timedMessages.set(selectedTimedMessageIndex, editedMessage);
                                    }
                                    mc.displayGuiScreen(this);
                                }));
                    }
                    break;
                case 22: // 移除定时消息
                    if (selectedTimedMessageIndex != -1) {
                        settings.timedMessages.remove(selectedTimedMessageIndex);
                        if (settings.timedMessages.isEmpty()) {
                            settings.timedMessages.add(""); // 确保列表不为空
                        }
                        selectedTimedMessageIndex = -1;
                    }
                    break;
            }
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.drawDefaultBackground();

        GuiTheme.drawPanel(panelX, panelTopY, panelWidth, panelBottomY - panelTopY);
        GuiTheme.drawTitleBar(panelX, panelTopY, panelWidth, I18n.format("gui.chatopt.title"), this.fontRenderer);
        drawGroupTabs(mouseX, mouseY);

        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        ScaledResolution sr = new ScaledResolution(mc);
        int scaleFactor = sr.getScaleFactor();
        GL11.glScissor(panelX * scaleFactor,
                mc.displayHeight - ((contentViewportTop + viewportHeight) * scaleFactor),
                panelWidth * scaleFactor,
                viewportHeight * scaleFactor);

        GlStateManager.pushMatrix();
        GlStateManager.translate(panelX, contentViewportTop - scrollOffset, 0);

        for (GuiButton button : scrollableButtons) {
            if (button.visible) {
                button.drawButton(mc, mouseX - panelX, mouseY - (contentViewportTop - scrollOffset), partialTicks);
            }
        }
        for (GuiSlider slider : sliders) {
            if (slider.visible) {
                slider.drawButton(mc, mouseX - panelX, mouseY - (contentViewportTop - scrollOffset), partialTicks);
            }
        }
        for (GuiTextField field : textFields) {
            if (isTextFieldVisible(field)) {
                drawThemedTextField(field);
            }
        }

        drawActiveGroupLabels();

        if (selectedGroup == ConfigGroup.TIMED) {
            drawTimedMessageList(mouseX - panelX, mouseY - (contentViewportTop - scrollOffset));
        }

        if (selectedGroup == ConfigGroup.STYLE && backgroundImagePathField.getText().isEmpty()
                && !backgroundImagePathField.isFocused()) {
            this.drawString(this.fontRenderer, I18n.format("gui.chatopt.bg_path_hint"),
                    backgroundImagePathField.x + 4,
                    backgroundImagePathField.y + (backgroundImagePathField.height - 8) / 2, 0xFF808080);
        }

        GlStateManager.popMatrix();
        GL11.glDisable(GL11.GL_SCISSOR_TEST);

        if (maxScroll > 0) {
            int mainPanelScrollbarX = panelX + panelWidth - 6;
            int thumbHeight = Math.max(10, (int) ((float) viewportHeight / Math.max(viewportHeight, contentHeight) * viewportHeight));
            int thumbY = contentViewportTop + (int) ((float) scrollOffset / maxScroll * (viewportHeight - thumbHeight));
            GuiTheme.drawScrollbar(mainPanelScrollbarX, contentViewportTop, 5, viewportHeight, thumbY, thumbHeight);
        }

        super.drawScreen(mouseX, mouseY, partialTicks);

        int bottomButtonY = panelBottomY + 10;
        drawCenteredString(mc.fontRenderer, I18n.format("gui.chatopt.drag_preview"), width / 2, bottomButtonY - 15,
                0xFFFFFF);

        if (dragging) {
            settings.xOffset += mouseX - dragStartX;
            settings.yOffset += mouseY - dragStartY;
            dragStartX = mouseX;
            dragStartY = mouseY;
        }
        if (backgroundTransparencySlider != null) {
            settings.backgroundTransparencyPercent = backgroundTransparencySlider.getValueInt();
        }

        this.mc.gameSettings.chatScale = (float) scaleSlider.getValue() / 100;
        this.mc.gameSettings.chatWidth = ((float) widthSlider.getValue() - 40) / 280;
        drawExampleChat();

        for (GuiButton button : scrollableButtons) {
            if (!button.visible) {
                continue;
            }
            int translatedY = button.y + contentViewportTop - scrollOffset;
            boolean isHovered = mouseX >= button.x + panelX && mouseY >= translatedY
                    && mouseX < button.x + panelX + button.width && mouseY < translatedY + button.height;
            if (isHovered) {
                String tooltip = tooltips.get(button);
                if (tooltip != null) {
                    drawHoveringText(splitTooltipLines(tooltip), mouseX, mouseY);
                }
            }
        }

        int scaleX = panelX + backgroundScaleField.x;
        int scaleY = contentViewportTop - scrollOffset + backgroundScaleField.y;
        int cropXX = panelX + backgroundCropXField.x;
        int cropXY = contentViewportTop - scrollOffset + backgroundCropXField.y;
        int cropYX = panelX + backgroundCropYField.x;
        int cropYY = contentViewportTop - scrollOffset + backgroundCropYField.y;

        if (selectedGroup == ConfigGroup.STYLE
                && mouseX >= scaleX && mouseX <= scaleX + backgroundScaleField.width && mouseY >= scaleY
                && mouseY <= scaleY + backgroundScaleField.height) {
            drawHoveringText(splitTooltipLines(I18n.format("gui.chatopt.tip.bg_scale")), mouseX, mouseY);
        } else if (selectedGroup == ConfigGroup.STYLE
                && mouseX >= cropXX && mouseX <= cropXX + backgroundCropXField.width && mouseY >= cropXY
                && mouseY <= cropXY + backgroundCropXField.height) {
            drawHoveringText(splitTooltipLines(I18n.format("gui.chatopt.tip.bg_crop_x")), mouseX, mouseY);
        } else if (selectedGroup == ConfigGroup.STYLE
                && mouseX >= cropYX && mouseX <= cropYX + backgroundCropYField.width && mouseY >= cropYY
                && mouseY <= cropYY + backgroundCropYField.height) {
            drawHoveringText(splitTooltipLines(I18n.format("gui.chatopt.tip.bg_crop_y")), mouseX, mouseY);
        } else if (mouseX >= groupBarX && mouseX <= groupBarX + groupBarW && mouseY >= groupBarY
                && mouseY <= groupBarY + groupBarH) {
            drawHoveringText(Arrays.asList("§e顶部功能分组", "§7点击分组切换聊天优化设置页。", "§7滚轮或左右方向键也可快速切组。"),
                    mouseX, mouseY);
        }
    }

    public void drawExampleChat() {
        List<ITextComponent> lines = new ArrayList<>();
        int i = MathHelper
                .floor((float) mc.ingameGUI.getChatGUI().getChatWidth() / mc.ingameGUI.getChatGUI().getChatScale());
        for (ITextComponent line : exampleChat) {
            lines.addAll(GuiUtilRenderComponents.splitText(line, i, this.mc.fontRenderer, false, false));
        }
        Collections.reverse(lines);
        GlStateManager.pushMatrix();
        ScaledResolution scaledresolution = new ScaledResolution(this.mc);
        GlStateManager.translate(2.0F + settings.xOffset,
                8.0F + settings.yOffset + scaledresolution.getScaledHeight() - 48, 0.0F);
        float f = this.mc.gameSettings.chatOpacity * 0.9F + 0.1F;
        float f1 = this.mc.gameSettings.chatScale;
        int k = MathHelper.ceil(mc.ingameGUI.getChatGUI().getChatWidth() / f1);
        GlStateManager.scale(f1, f1, 1.0F);
        int i1 = 0;
        double d0 = 1.0D;
        int l1 = (int) (255.0D * d0);
        l1 = (int) ((float) l1 * f);
        GlStateManager.enableAlpha();
        GlStateManager.enableBlend();
        chatLeft = settings.xOffset;
        chatRight = (int) (settings.xOffset + (k + 4) * f1);
        chatBottom = 8 + settings.yOffset + scaledresolution.getScaledHeight() - 48;
        for (ITextComponent message : lines) {
            int j2 = -i1 * 9;

            // !! 核心修改：添加图片加载和绘制逻辑 !!

            // 1. 尝试获取背景图片的 ResourceLocation，并传入质量设置
            ResourceLocation bgLocation = TextureManagerHelper.getResourceLocationForPath(settings.backgroundImagePath,
                    settings.imageQuality);

            if (bgLocation != null) {
                // 2. 如果图片加载成功，则绘制图片背景
                mc.getTextureManager().bindTexture(bgLocation);

                // 3. 计算包含自定义透明度的最终 Alpha 值
                float transparencyRatio = settings.backgroundTransparencyPercent / 100.0f;
                float opacityRatio = 1.0f - transparencyRatio;
                // 预览框的 alpha 始终为 1.0，所以我们只考虑游戏全局透明度和我们的滑块
                float finalImageAlpha = f * opacityRatio;
                GlStateManager.color(1.0F, 1.0F, 1.0F, finalImageAlpha);

                GlStateManager.enableBlend();
                int[] texSize = TextureManagerHelper.getTextureSizeForPath(
                        settings.backgroundImagePath, settings.imageQuality);
                int texW = (texSize != null) ? texSize[0] : (k + 4);
                int texH = (texSize != null) ? texSize[1] : 9;

                int safeScale = Math.max(10, Math.min(300, settings.backgroundImageScale));
                float scaleFactor = 100.0F / safeScale;
                int targetW = k + 4;
                int targetH = 9;

                int sampleW = Math.max(1, Math.min(texW, (int) (targetW * scaleFactor)));
                int sampleH = Math.max(1, Math.min(texH, (int) (targetH * scaleFactor)));

                int u = Math.max(0, settings.backgroundCropX);
                int v = Math.max(0, settings.backgroundCropY);
                if (u + sampleW > texW) {
                    u = Math.max(0, texW - sampleW);
                }
                if (v + sampleH > texH) {
                    v = Math.max(0, texH - sampleH);
                }

                Gui.drawScaledCustomSizeModalRect(-2, j2 - 9, u, v,
                        sampleW, sampleH,
                        targetW, targetH,
                        texW, texH);
                // 恢复颜色状态，防止影响后续渲染
                GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);

            } else {
                // 4. 如果没有图片，则回退到绘制颜色背景（这是您原来的代码）
                if (settings.backgroundTransparencyPercent < 100) {
                    float transparencyRatio = settings.backgroundTransparencyPercent / 100.0f;
                    float opacityRatio = 1.0f - transparencyRatio;
                    int backgroundAlpha = (int) ((l1 / 2.0f) * opacityRatio);
                    int backgroundColor = backgroundAlpha << 24;
                    drawRect(-2, j2 - 9, k + 4, j2, backgroundColor);
                }
            }
            // !! 修改结束 !!

            this.mc.fontRenderer.drawStringWithShadow(message.getFormattedText(), 0.0F, (float) (j2 - 8),
                    16777215 + (l1 << 24));
            ++i1;
        }
        chatTop = (int) (8 + settings.yOffset + scaledresolution.getScaledHeight() - 48 + (-i1 * 9) * f1);
        GlStateManager.disableAlpha();
        GlStateManager.disableBlend();
        GlStateManager.popMatrix();
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        super.mouseClicked(mouseX, mouseY, mouseButton);

        int mainPanelScrollbarX = panelX + panelWidth - 6;
        if (mouseButton == 0 && handleGroupClick(mouseX, mouseY)) {
            return;
        }

        if (maxScroll > 0 && mouseX >= mainPanelScrollbarX && mouseX <= mainPanelScrollbarX + 5 && mouseY >= contentViewportTop
                && mouseY <= contentViewportTop + viewportHeight) {
            isDraggingScrollbar = true;
            return;
        }

        // !! 核心修复：使用转换后的坐标来检测点击 !!
        if (mouseX >= panelX && mouseX < mainPanelScrollbarX && mouseY >= contentViewportTop
                && mouseY <= contentViewportTop + viewportHeight) {
            int translatedX = mouseX - panelX;
            int translatedY = mouseY - (contentViewportTop - scrollOffset);

            // 检查定时消息列表的点击
            int timedMessageScrollbarX = timedListX + timedListW + 2;

            if (selectedGroup == ConfigGroup.TIMED
                    && maxTimedMessageScroll > 0
                    && translatedX >= timedMessageScrollbarX
                    && translatedX <= timedMessageScrollbarX + 5
                    && translatedY >= timedListY && translatedY <= timedListY + timedListH) {
                isDraggingTimedMessageScrollbar = true;
                return;
            }

            if (selectedGroup == ConfigGroup.TIMED
                    && translatedX >= timedListX && translatedX < timedMessageScrollbarX && translatedY >= timedListY
                    && translatedY <= timedListY + timedListH) {
                int clickedIndex = (translatedY - timedListY) / 15 + timedMessageScrollOffset;
                if (clickedIndex >= 0 && clickedIndex < settings.timedMessages.size()) {
                    selectedTimedMessageIndex = clickedIndex;
                }
                return; // 消耗点击事件，防止触发其他控件
            }

            // 检查其他滚动控件
            for (GuiButton button : scrollableButtons) {
                if (button.visible && button.mousePressed(mc, translatedX, translatedY)) {
                    button.playPressSound(mc.getSoundHandler());
                    this.actionPerformed(button);
                }
            }
            for (GuiSlider slider : sliders) {
                if (slider.visible && slider.mousePressed(mc, translatedX, translatedY)) {
                    slider.playPressSound(mc.getSoundHandler());
                }
            }
            for (GuiTextField field : textFields) {
                if (isTextFieldVisible(field)) {
                    field.mouseClicked(translatedX, translatedY, mouseButton);
                }
            }
        }

        if (mouseButton == 0) {
            if (mouseX >= chatLeft && mouseX <= chatRight && mouseY >= chatTop && mouseY <= chatBottom) {
                dragging = true;
                dragStartX = mouseX;
                dragStartY = mouseY;
            }
        }
    }

    // 在 GuiChatOptimization.java 中
    @Override
    protected void mouseClickMove(int mouseX, int mouseY, int clickedMouseButton, long timeSinceLastClick) {
        super.mouseClickMove(mouseX, mouseY, clickedMouseButton, timeSinceLastClick);
        if (isDraggingScrollbar) {
            float percent = (float) (mouseY - contentViewportTop) / viewportHeight;
            scrollOffset = (int) (percent * maxScroll);
            scrollOffset = Math.max(0, Math.min(maxScroll, scrollOffset));
        }
        // !! 新增代码块开始 !!
        // 将拖动事件传递给滑块
        int translatedY = mouseY - (contentViewportTop - scrollOffset);
        for (GuiSlider slider : sliders) {
            // GuiSlider的mousePressed方法也负责处理拖动
            if (slider.visible) {
                slider.mousePressed(mc, mouseX - panelX, translatedY);
            }
        }
        // !! 新增代码块结束 !!
    }

    // 在 GuiChatOptimization.java 中
    @Override
    public void mouseReleased(int mouseX, int mouseY, int mouseButton) {
        super.mouseReleased(mouseX, mouseY, mouseButton);
        dragging = false;
        isDraggingScrollbar = false;
        // !! 核心修复：将 mouseReleased 事件传递给所有滑块 !!
        for (GuiSlider slider : sliders) {
            slider.mouseReleased(mouseX, mouseY);
        }
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        int dWheel = Mouse.getDWheel();
        if (dWheel != 0) {
            int mouseX = Mouse.getX() * this.width / this.mc.displayWidth;
            int mouseY = this.height - Mouse.getY() * this.height / this.mc.displayHeight - 1;

            int listX = panelX + timedListX;
            int listYAbsolute = timedListY + contentViewportTop - scrollOffset;
            if (selectedGroup == ConfigGroup.TIMED
                    && mouseX >= listX && mouseX <= listX + timedListW && mouseY >= listYAbsolute
                    && mouseY <= listYAbsolute + timedListH) {
                if (dWheel > 0)
                    timedMessageScrollOffset = Math.max(0, timedMessageScrollOffset - 1);
                else
                    timedMessageScrollOffset = Math.min(maxTimedMessageScroll, timedMessageScrollOffset + 1);
            } else if (handleGroupWheel(dWheel, mouseX, mouseY)) {
                return;
            } else {
                if (maxScroll > 0) {
                    scrollOffset += (dWheel > 0) ? -20 : 20;
                    scrollOffset = Math.max(0, Math.min(maxScroll, scrollOffset));
                }
            }
        }
    }

    @Override
    public void onGuiClosed() {
        Keyboard.enableRepeatEvents(false);
        if (mc.ingameGUI.getChatGUI() instanceof CustomGuiNewChat) {
            ((CustomGuiNewChat) mc.ingameGUI.getChatGUI()).configuring = false;
        }
        this.mc.gameSettings.saveOptions();
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (keyCode == Keyboard.KEY_LEFT) {
            selectGroupByOffset(-1);
            return;
        }
        if (keyCode == Keyboard.KEY_RIGHT) {
            selectGroupByOffset(1);
            return;
        }
        super.keyTyped(typedChar, keyCode);
        for (GuiTextField field : textFields) {
            if (isTextFieldVisible(field)) {
                field.textboxKeyTyped(typedChar, keyCode);
            }
        }
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }

    private void recalcGroupTabs() {
        this.groupTabContentWidth = 0;
        ConfigGroup[] groups = ConfigGroup.values();
        for (int i = 0; i < groups.length; i++) {
            this.groupTabContentWidth += getGroupTabWidth(groups[i]);
            if (i < groups.length - 1) {
                this.groupTabContentWidth += 6;
            }
        }
        this.groupTabMaxScroll = Math.max(0, this.groupTabContentWidth - this.groupTabsW);
        ensureSelectedGroupVisible();
    }

    private int getGroupTabWidth(ConfigGroup group) {
        int textWidth = this.fontRenderer == null ? 48 : this.fontRenderer.getStringWidth(group.tabLabel);
        return Math.max(62, textWidth + 24);
    }

    private void ensureSelectedGroupVisible() {
        int startX = 0;
        for (ConfigGroup group : ConfigGroup.values()) {
            int tabWidth = getGroupTabWidth(group);
            if (group == this.selectedGroup) {
                int endX = startX + tabWidth;
                if (startX < this.groupTabScroll) {
                    this.groupTabScroll = startX;
                } else if (endX > this.groupTabScroll + this.groupTabsW) {
                    this.groupTabScroll = endX - this.groupTabsW;
                }
                this.groupTabScroll = Math.max(0, Math.min(this.groupTabScroll, this.groupTabMaxScroll));
                return;
            }
            startX += tabWidth + 6;
        }
    }

    private void drawGroupTabs(int mouseX, int mouseY) {
        drawSectionFrame("功能分组", groupBarX, groupBarY, groupBarW, groupBarH);
        if (groupPrevButton != null) {
            groupPrevButton.enabled = selectedGroup.ordinal() > 0;
        }
        if (groupNextButton != null) {
            groupNextButton.enabled = selectedGroup.ordinal() < ConfigGroup.values().length - 1;
        }

        int currentX = groupTabsX - groupTabScroll;
        for (ConfigGroup group : ConfigGroup.values()) {
            int tabWidth = getGroupTabWidth(group);
            if (currentX + tabWidth >= groupTabsX && currentX <= groupTabsX + groupTabsW) {
                boolean hovered = mouseX >= currentX && mouseX <= currentX + tabWidth
                        && mouseY >= groupTabsY && mouseY <= groupTabsY + groupTabsH;
                boolean selected = group == selectedGroup;
                GuiTheme.UiState state = selected ? GuiTheme.UiState.SELECTED
                        : (hovered ? GuiTheme.UiState.HOVER : GuiTheme.UiState.NORMAL);
                GuiTheme.drawButtonFrameSafe(currentX, groupTabsY, tabWidth, groupTabsH, state);
                drawCenteredString(this.fontRenderer, group.tabLabel, currentX + tabWidth / 2, groupTabsY + 6,
                        selected ? 0xFFFFFFFF : GuiTheme.getStateTextColor(state));
            }
            currentX += tabWidth + 6;
        }

        if (groupTabMaxScroll > 0) {
            int trackX = groupTabsX;
            int trackY = groupBarY + groupBarH - 6;
            int trackW = groupTabsW;
            int thumbW = Math.max(18, (int) ((groupTabsW / (float) groupTabContentWidth) * trackW));
            int thumbTravel = Math.max(1, trackW - thumbW);
            int thumbX = trackX + (int) ((groupTabScroll / (float) groupTabMaxScroll) * thumbTravel);
            drawRect(trackX, trackY, trackX + trackW, trackY + 4, 0xAA1B2733);
            drawRect(thumbX, trackY, thumbX + thumbW, trackY + 4, 0xFF4FA6D9);
        }
    }

    private void drawSectionFrame(String title, int boxX, int boxY, int boxW, int boxH) {
        drawRect(boxX, boxY, boxX + boxW, boxY + boxH, 0x44202A36);
        drawHorizontalLine(boxX, boxX + boxW, boxY, 0xFF4FA6D9);
        drawHorizontalLine(boxX, boxX + boxW, boxY + boxH, 0xFF35536C);
        drawVerticalLine(boxX, boxY, boxY + boxH, 0xFF35536C);
        drawVerticalLine(boxX + boxW, boxY, boxY + boxH, 0xFF35536C);
        this.drawString(this.fontRenderer, "§b" + title, boxX + 6, boxY + 5, 0xFFE8F6FF);
    }

    private void relayoutGroupedContent() {
        hideAllGroupControls();
        int controlX = 10;
        int controlWidth = panelWidth - 20;
        int halfWidth = (controlWidth - 10) / 2;
        int smallFieldWidth = 48;
        int currentY = 4;

        switch (selectedGroup) {
            case BASIC:
                layoutButton(0, controlX, currentY, halfWidth, 20);
                layoutButton(1, controlX + halfWidth + 10, currentY, halfWidth, 20);
                currentY += 25;
                layoutButton(2, controlX, currentY, halfWidth, 20);
                layoutButton(3, controlX + halfWidth + 10, currentY, halfWidth, 20);
                currentY += 25;
                layoutButton(4, controlX, currentY, halfWidth, 20);
                layoutField(antiSpamThresholdField, controlX + controlWidth - smallFieldWidth, currentY, smallFieldWidth);
                currentY += 25;
                layoutButton(11, controlX, currentY, controlWidth, 20);
                currentY += 28;
                break;
            case TIMED:
                layoutButton(16, controlX, currentY, halfWidth, 20);
                layoutButton(19, controlX + halfWidth + 10, currentY, halfWidth, 20);
                currentY += 25;
                layoutField(timedMessageIntervalField, controlX + controlWidth - smallFieldWidth, currentY, smallFieldWidth);
                currentY += 28;
                timedListX = controlX;
                timedListY = currentY;
                timedListW = controlWidth - 10;
                timedListH = 84;
                currentY += timedListH + 6;
                int listButtonWidth = (controlWidth - 20) / 3;
                layoutButton(20, controlX, currentY, listButtonWidth, 20);
                layoutButton(21, controlX + listButtonWidth + 10, currentY, listButtonWidth, 20);
                layoutButton(22, controlX + 2 * (listButtonWidth + 10), currentY, listButtonWidth, 20);
                currentY += 28;
                break;
            case FILTER:
                layoutButton(6, controlX, currentY, halfWidth, 20);
                layoutButton(7, controlX + halfWidth + 10, currentY, halfWidth, 20);
                currentY += 32;
                layoutField(blacklistField, controlX, currentY, controlWidth);
                currentY += 34;
                layoutField(whitelistField, controlX, currentY, controlWidth);
                currentY += 30;
                break;
            case STYLE:
            default:
                layoutSlider(backgroundTransparencySlider, controlX, currentY, controlWidth, 20);
                currentY += 32;
                int pathFieldWidth = controlWidth - 110;
                layoutField(backgroundImagePathField, controlX, currentY, pathFieldWidth);
                layoutButton(15, controlX + pathFieldWidth + 10, currentY, 100, 20);
                currentY += 32;
                int fieldW = (controlWidth - 8) / 3;
                layoutField(backgroundScaleField, controlX, currentY, fieldW);
                layoutField(backgroundCropXField, controlX + fieldW + 4, currentY, fieldW);
                layoutField(backgroundCropYField, controlX + 2 * (fieldW + 4), currentY, fieldW);
                currentY += 32;
                layoutSlider(scaleSlider, controlX, currentY, controlWidth, 20);
                currentY += 25;
                layoutSlider(widthSlider, controlX, currentY, controlWidth, 20);
                currentY += 28;
                break;
        }

        contentHeight = currentY + 8;
        maxScroll = Math.max(0, contentHeight - viewportHeight);
        scrollOffset = Math.max(0, Math.min(scrollOffset, maxScroll));
    }

    private void hideAllGroupControls() {
        timedListX = -2000;
        timedListY = -2000;
        timedListW = 0;
        timedListH = 0;
        for (GuiButton button : scrollableButtons) {
            button.visible = false;
            button.x = -2000;
            button.y = -2000;
        }
        for (GuiSlider slider : sliders) {
            slider.visible = false;
            slider.x = -2000;
            slider.y = -2000;
        }
        for (GuiTextField field : textFields) {
            field.x = -2000;
            field.y = -2000;
        }
    }

    private GuiButton findScrollableButton(int id) {
        for (GuiButton button : scrollableButtons) {
            if (button.id == id) {
                return button;
            }
        }
        return null;
    }

    private void layoutButton(int id, int x, int y, int width, int height) {
        GuiButton button = findScrollableButton(id);
        if (button != null) {
            button.visible = true;
            button.x = x;
            button.y = y;
            button.width = width;
            button.height = height;
        }
    }

    private void layoutSlider(GuiSlider slider, int x, int y, int width, int height) {
        if (slider != null) {
            slider.visible = true;
            slider.x = x;
            slider.y = y;
            slider.width = width;
            slider.height = height;
        }
    }

    private void layoutField(GuiTextField field, int x, int y, int width) {
        if (field != null) {
            field.x = x;
            field.y = y;
            field.width = width;
        }
    }

    private boolean isTextFieldVisible(GuiTextField field) {
        return field != null && field.x > -1000 && field.y > -1000;
    }

    private void drawActiveGroupLabels() {
        int labelSpacing = 12;
        switch (selectedGroup) {
            case BASIC:
                drawString(this.fontRenderer, I18n.format("gui.chatopt.antispam_sec"), antiSpamThresholdField.x - 110,
                        antiSpamThresholdField.y + 6, 0xFFFFFF);
                break;
            case TIMED:
                drawString(this.fontRenderer, I18n.format("gui.chatopt.send_interval_sec"),
                        timedMessageIntervalField.x - 80, timedMessageIntervalField.y + 6, 0xFFFFFF);
                drawString(this.fontRenderer, I18n.format("gui.chatopt.timed_list"), timedListX, timedListY - 12,
                        0xFFFFFF);
                break;
            case FILTER:
                drawString(this.fontRenderer, I18n.format("gui.chatopt.blacklist_label"), blacklistField.x,
                        blacklistField.y - labelSpacing, 0xFFFFFF);
                drawString(this.fontRenderer, I18n.format("gui.chatopt.whitelist_label"), whitelistField.x,
                        whitelistField.y - labelSpacing, 0xFFFFFF);
                break;
            case STYLE:
                drawString(this.fontRenderer, I18n.format("gui.chatopt.bg_path"), backgroundImagePathField.x,
                        backgroundImagePathField.y - labelSpacing, 0xFFFFFF);
                drawString(this.fontRenderer, I18n.format("gui.chatopt.bg_transform"), backgroundScaleField.x,
                        backgroundScaleField.y - labelSpacing, 0xFFFFFF);
                drawString(this.fontRenderer, I18n.format("gui.theme.label.scale"), backgroundScaleField.x + 2,
                        backgroundScaleField.y + 6, 0xFFB0B0B0);
                drawString(this.fontRenderer, "cropX", backgroundCropXField.x + 2, backgroundCropXField.y + 6,
                        0xFFB0B0B0);
                drawString(this.fontRenderer, "cropY", backgroundCropYField.x + 2, backgroundCropYField.y + 6,
                        0xFFB0B0B0);
                break;
        }
    }

    private void drawTimedMessageList(int mouseX, int mouseY) {
        drawRect(timedListX, timedListY, timedListX + timedListW, timedListY + timedListH, 0x80000000);
        int itemHeight = 15;
        int visibleItems = Math.max(1, timedListH / itemHeight);
        maxTimedMessageScroll = Math.max(0, settings.timedMessages.size() - visibleItems);

        for (int i = 0; i < visibleItems; i++) {
            int index = i + timedMessageScrollOffset;
            if (index >= settings.timedMessages.size()) {
                break;
            }
            String msg = settings.timedMessages.get(index);
            int itemY = timedListY + i * itemHeight;
            int bgColor = (index == selectedTimedMessageIndex) ? 0xFF0066AA : 0;
            drawRect(timedListX, itemY, timedListX + timedListW, itemY + itemHeight, bgColor);
            drawString(fontRenderer, fontRenderer.trimStringToWidth(msg, timedListW - 10), timedListX + 5, itemY + 4,
                    0xFFFFFF);
        }

        if (maxTimedMessageScroll > 0) {
            int timedMessageScrollbarX = timedListX + timedListW + 2;
            int thumbHeight = Math.max(10, (int) ((float) visibleItems / settings.timedMessages.size() * timedListH));
            int thumbY = timedListY
                    + (int) ((float) timedMessageScrollOffset / maxTimedMessageScroll * (timedListH - thumbHeight));
            GuiTheme.drawScrollbar(timedMessageScrollbarX, timedListY, 5, timedListH, thumbY, thumbHeight);
        }
    }

    private boolean handleGroupClick(int mouseX, int mouseY) {
        if (mouseX < groupTabsX || mouseX > groupTabsX + groupTabsW || mouseY < groupTabsY || mouseY > groupTabsY + groupTabsH) {
            return false;
        }
        int currentX = groupTabsX - groupTabScroll;
        for (ConfigGroup group : ConfigGroup.values()) {
            int tabWidth = getGroupTabWidth(group);
            if (mouseX >= currentX && mouseX <= currentX + tabWidth) {
                setSelectedGroup(group);
                return true;
            }
            currentX += tabWidth + 6;
        }
        return false;
    }

    private boolean handleGroupWheel(int wheel, int mouseX, int mouseY) {
        if (mouseX < groupBarX || mouseX > groupBarX + groupBarW || mouseY < groupBarY || mouseY > groupBarY + groupBarH) {
            return false;
        }
        selectGroupByOffset(wheel < 0 ? 1 : -1);
        return true;
    }

    private void selectGroupByOffset(int delta) {
        ConfigGroup[] groups = ConfigGroup.values();
        int nextIndex = Math.max(0, Math.min(selectedGroup.ordinal() + delta, groups.length - 1));
        setSelectedGroup(groups[nextIndex]);
    }

    private void setSelectedGroup(ConfigGroup group) {
        if (group == null || group == selectedGroup) {
            return;
        }
        selectedGroup = group;
        scrollOffset = 0;
        isDraggingScrollbar = false;
        isDraggingTimedMessageScrollbar = false;
        for (GuiTextField field : textFields) {
            field.setFocused(false);
        }
        relayoutGroupedContent();
    }

    private int parseIntOr(String s, int def) {
        try {
            return Integer.parseInt(s.trim());
        } catch (Exception e) {
            return def;
        }
    }

    private List<String> splitTooltipLines(String text) {
        if (text == null || text.isEmpty()) {
            return Arrays.asList("");
        }
        String normalized = text.replace("\\n", "\n");
        return Arrays.asList(normalized.split("\n"));
    }

    private enum ConfigGroup {
        BASIC("基础"),
        TIMED("定时消息"),
        FILTER("黑白名单"),
        STYLE("外观预览");

        private final String tabLabel;

        ConfigGroup(String tabLabel) {
            this.tabLabel = tabLabel;
        }
    }
}
