package com.zszl.zszlScriptMod.gui.config;

import com.zszl.zszlScriptMod.gui.components.GuiTextInput;
import com.zszl.zszlScriptMod.gui.components.GuiTheme;
import com.zszl.zszlScriptMod.gui.components.ThemedButton;
import com.zszl.zszlScriptMod.gui.components.ThemedGuiScreen;
import com.zszl.zszlScriptMod.gui.components.ToggleGuiButton;
import com.zszl.zszlScriptMod.handlers.AutoFishingHandler;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import org.lwjgl.input.Mouse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class GuiAutoFishingConfig extends ThemedGuiScreen {

    private static final int BTN_REQUIRE_ROD = 1;
    private static final int BTN_AUTO_SWITCH_ROD = 2;
    private static final int BTN_PREFERRED_SLOT = 3;
    private static final int BTN_DISABLE_WHEN_GUI = 4;
    private static final int BTN_ALLOW_MOVING = 5;
    private static final int BTN_STATUS_MESSAGE = 6;

    private static final int BTN_AUTO_CAST_ON_START = 20;
    private static final int BTN_INITIAL_CAST_DELAY = 21;
    private static final int BTN_AUTO_RECAST = 22;
    private static final int BTN_RECAST_DELAY_MIN = 23;
    private static final int BTN_RECAST_DELAY_MAX = 24;
    private static final int BTN_RETRY_BOBBER_MISSING = 25;
    private static final int BTN_RETRY_CAST_DELAY = 26;
    private static final int BTN_TIMEOUT_RECAST = 27;
    private static final int BTN_MAX_WAIT = 28;

    private static final int BTN_BITE_MODE = 40;
    private static final int BTN_IGNORE_SETTLE = 41;
    private static final int BTN_REEL_DELAY = 42;
    private static final int BTN_VERTICAL_THRESHOLD = 43;
    private static final int BTN_HORIZONTAL_THRESHOLD = 44;
    private static final int BTN_CONFIRM_BITE = 45;
    private static final int BTN_DEBUG_BITE = 46;

    private static final int BTN_POST_REEL_PAUSE = 60;
    private static final int BTN_PREVENT_DOUBLE_REEL = 61;
    private static final int BTN_RECAST_ONLY_SUCCESS = 62;
    private static final int BTN_RESET_HOOK_GONE = 63;
    private static final int BTN_AUTO_RECOVER = 64;

    private static final int BTN_STOP_LOW_DURA = 80;
    private static final int BTN_MIN_DURA = 81;
    private static final int BTN_STOP_NO_ROD = 82;
    private static final int BTN_PAUSE_HOOK_ENTITY = 83;
    private static final int BTN_STOP_WORLD_CHANGE = 84;

    private static final int BTN_SAVE = 100;
    private static final int BTN_DEFAULT = 101;
    private static final int BTN_CANCEL = 102;

    private final GuiScreen parentScreen;
    private final List<String> instructionLines = new ArrayList<>();

    private ToggleGuiButton requireRodButton;
    private ToggleGuiButton autoSwitchRodButton;
    private GuiButton preferredSlotButton;
    private ToggleGuiButton disableWhenGuiButton;
    private ToggleGuiButton allowMovingButton;
    private ToggleGuiButton statusMessageButton;

    private ToggleGuiButton autoCastOnStartButton;
    private GuiButton initialCastDelayButton;
    private ToggleGuiButton autoRecastButton;
    private GuiButton recastDelayMinButton;
    private GuiButton recastDelayMaxButton;
    private ToggleGuiButton retryBobberMissingButton;
    private GuiButton retryCastDelayButton;
    private ToggleGuiButton timeoutRecastButton;
    private GuiButton maxWaitButton;

    private GuiButton biteModeButton;
    private GuiButton ignoreSettleButton;
    private GuiButton reelDelayButton;
    private GuiButton verticalThresholdButton;
    private GuiButton horizontalThresholdButton;
    private GuiButton confirmBiteButton;
    private ToggleGuiButton debugBiteButton;

    private GuiButton postReelPauseButton;
    private GuiButton preventDoubleReelButton;
    private ToggleGuiButton recastOnlySuccessButton;
    private ToggleGuiButton resetHookGoneButton;
    private ToggleGuiButton autoRecoverButton;

    private ToggleGuiButton stopLowDuraButton;
    private GuiButton minDuraButton;
    private ToggleGuiButton stopNoRodButton;
    private ToggleGuiButton pauseHookEntityButton;
    private ToggleGuiButton stopWorldChangeButton;

    private GuiButton saveButton;
    private GuiButton defaultButton;
    private GuiButton cancelButton;

    private int panelX;
    private int panelY;
    private int panelWidth;
    private int panelHeight;

    private int contentTop;
    private int contentBottom;
    private int contentScroll;
    private int contentMaxScroll;

    public GuiAutoFishingConfig(GuiScreen parentScreen) {
        this.parentScreen = parentScreen;
    }

    @Override
    public void initGui() {
        this.buttonList.clear();
        recalcLayout();
        initButtons();
        relayoutButtons();
    }

    private void recalcLayout() {
        int maxWidth = Math.min(440, Math.max(260, this.width - 12));
        int maxHeight = Math.min(380, Math.max(230, this.height - 12));

        this.panelWidth = Math.min(maxWidth, Math.max(240, this.width - 8));
        this.panelHeight = Math.min(maxHeight, Math.max(230, this.height - 8));
        this.panelX = (this.width - this.panelWidth) / 2;
        this.panelY = (this.height - this.panelHeight) / 2;

        this.instructionLines.clear();
        String instruction = "左键主界面按钮为总开关；这里用于细调自动钓鱼的出杆、咬钩判定、补杆与安全限制。";
        int instructionWidth = Math.max(80, this.panelWidth - 32);
        this.instructionLines.addAll(this.fontRenderer.listFormattedStringToWidth(instruction, instructionWidth));

        int instructionHeight = this.instructionLines.size() * 10;
        this.contentTop = this.panelY + 24 + instructionHeight + 8;
        int footerTop = this.panelY + this.panelHeight - 28;
        this.contentBottom = footerTop - 8;

        if (this.contentBottom < this.contentTop + 24) {
            this.contentBottom = this.contentTop + 24;
        }
    }

    private void initButtons() {
        requireRodButton = new ToggleGuiButton(BTN_REQUIRE_ROD, 0, 0, 100, 20, "",
                AutoFishingHandler.requireFishingRod);
        autoSwitchRodButton = new ToggleGuiButton(BTN_AUTO_SWITCH_ROD, 0, 0, 100, 20, "",
                AutoFishingHandler.autoSwitchToRod);
        preferredSlotButton = new ThemedButton(BTN_PREFERRED_SLOT, 0, 0, 100, 20, "");
        disableWhenGuiButton = new ToggleGuiButton(BTN_DISABLE_WHEN_GUI, 0, 0, 100, 20, "",
                AutoFishingHandler.disableWhenGuiOpen);
        allowMovingButton = new ToggleGuiButton(BTN_ALLOW_MOVING, 0, 0, 100, 20, "",
                AutoFishingHandler.allowWhilePlayerMoving);
        statusMessageButton = new ToggleGuiButton(BTN_STATUS_MESSAGE, 0, 0, 100, 20, "",
                AutoFishingHandler.sendStatusMessage);

        autoCastOnStartButton = new ToggleGuiButton(BTN_AUTO_CAST_ON_START, 0, 0, 100, 20, "",
                AutoFishingHandler.enableAutoCastOnStart);
        initialCastDelayButton = new ThemedButton(BTN_INITIAL_CAST_DELAY, 0, 0, 100, 20, "");
        autoRecastButton = new ToggleGuiButton(BTN_AUTO_RECAST, 0, 0, 100, 20, "",
                AutoFishingHandler.autoRecastAfterCatch);
        recastDelayMinButton = new ThemedButton(BTN_RECAST_DELAY_MIN, 0, 0, 100, 20, "");
        recastDelayMaxButton = new ThemedButton(BTN_RECAST_DELAY_MAX, 0, 0, 100, 20, "");
        retryBobberMissingButton = new ToggleGuiButton(BTN_RETRY_BOBBER_MISSING, 0, 0, 100, 20, "",
                AutoFishingHandler.retryCastWhenBobberMissing);
        retryCastDelayButton = new ThemedButton(BTN_RETRY_CAST_DELAY, 0, 0, 100, 20, "");
        timeoutRecastButton = new ToggleGuiButton(BTN_TIMEOUT_RECAST, 0, 0, 100, 20, "",
                AutoFishingHandler.timeoutRecastEnabled);
        maxWaitButton = new ThemedButton(BTN_MAX_WAIT, 0, 0, 100, 20, "");

        biteModeButton = new ThemedButton(BTN_BITE_MODE, 0, 0, 100, 20, "");
        ignoreSettleButton = new ThemedButton(BTN_IGNORE_SETTLE, 0, 0, 100, 20, "");
        reelDelayButton = new ThemedButton(BTN_REEL_DELAY, 0, 0, 100, 20, "");
        verticalThresholdButton = new ThemedButton(BTN_VERTICAL_THRESHOLD, 0, 0, 100, 20, "");
        horizontalThresholdButton = new ThemedButton(BTN_HORIZONTAL_THRESHOLD, 0, 0, 100, 20, "");
        confirmBiteButton = new ThemedButton(BTN_CONFIRM_BITE, 0, 0, 100, 20, "");
        debugBiteButton = new ToggleGuiButton(BTN_DEBUG_BITE, 0, 0, 100, 20, "",
                AutoFishingHandler.debugBiteInfo);

        postReelPauseButton = new ThemedButton(BTN_POST_REEL_PAUSE, 0, 0, 100, 20, "");
        preventDoubleReelButton = new ThemedButton(BTN_PREVENT_DOUBLE_REEL, 0, 0, 100, 20, "");
        recastOnlySuccessButton = new ToggleGuiButton(BTN_RECAST_ONLY_SUCCESS, 0, 0, 100, 20, "",
                AutoFishingHandler.recastOnlyIfLootSuccess);
        resetHookGoneButton = new ToggleGuiButton(BTN_RESET_HOOK_GONE, 0, 0, 100, 20, "",
                AutoFishingHandler.resetStateWhenHookGone);
        autoRecoverButton = new ToggleGuiButton(BTN_AUTO_RECOVER, 0, 0, 100, 20, "",
                AutoFishingHandler.autoRecoverFromInterruptedCast);

        stopLowDuraButton = new ToggleGuiButton(BTN_STOP_LOW_DURA, 0, 0, 100, 20, "",
                AutoFishingHandler.stopWhenRodDurabilityLow);
        minDuraButton = new ThemedButton(BTN_MIN_DURA, 0, 0, 100, 20, "");
        stopNoRodButton = new ToggleGuiButton(BTN_STOP_NO_ROD, 0, 0, 100, 20, "",
                AutoFishingHandler.stopWhenNoRodFound);
        pauseHookEntityButton = new ToggleGuiButton(BTN_PAUSE_HOOK_ENTITY, 0, 0, 100, 20, "",
                AutoFishingHandler.pauseWhenHookedEntity);
        stopWorldChangeButton = new ToggleGuiButton(BTN_STOP_WORLD_CHANGE, 0, 0, 100, 20, "",
                AutoFishingHandler.stopOnWorldChange);

        saveButton = new ThemedButton(BTN_SAVE, 0, 0, 90, 20, "§a保存并关闭");
        defaultButton = new ThemedButton(BTN_DEFAULT, 0, 0, 90, 20, "§e恢复默认");
        cancelButton = new ThemedButton(BTN_CANCEL, 0, 0, 90, 20, "取消");

        this.buttonList.add(requireRodButton);
        this.buttonList.add(autoSwitchRodButton);
        this.buttonList.add(preferredSlotButton);
        this.buttonList.add(disableWhenGuiButton);
        this.buttonList.add(allowMovingButton);
        this.buttonList.add(statusMessageButton);

        this.buttonList.add(autoCastOnStartButton);
        this.buttonList.add(initialCastDelayButton);
        this.buttonList.add(autoRecastButton);
        this.buttonList.add(recastDelayMinButton);
        this.buttonList.add(recastDelayMaxButton);
        this.buttonList.add(retryBobberMissingButton);
        this.buttonList.add(retryCastDelayButton);
        this.buttonList.add(timeoutRecastButton);
        this.buttonList.add(maxWaitButton);

        this.buttonList.add(biteModeButton);
        this.buttonList.add(ignoreSettleButton);
        this.buttonList.add(reelDelayButton);
        this.buttonList.add(verticalThresholdButton);
        this.buttonList.add(horizontalThresholdButton);
        this.buttonList.add(confirmBiteButton);
        this.buttonList.add(debugBiteButton);

        this.buttonList.add(postReelPauseButton);
        this.buttonList.add(preventDoubleReelButton);
        this.buttonList.add(recastOnlySuccessButton);
        this.buttonList.add(resetHookGoneButton);
        this.buttonList.add(autoRecoverButton);

        this.buttonList.add(stopLowDuraButton);
        this.buttonList.add(minDuraButton);
        this.buttonList.add(stopNoRodButton);
        this.buttonList.add(pauseHookEntityButton);
        this.buttonList.add(stopWorldChangeButton);

        this.buttonList.add(saveButton);
        this.buttonList.add(defaultButton);
        this.buttonList.add(cancelButton);

        refreshButtonTexts();
    }

    private void refreshButtonTexts() {
        requireRodButton.setEnabledState(AutoFishingHandler.requireFishingRod);
        requireRodButton.displayString = "必须手持鱼竿: " + stateText(AutoFishingHandler.requireFishingRod);

        autoSwitchRodButton.setEnabledState(AutoFishingHandler.autoSwitchToRod);
        autoSwitchRodButton.displayString = "自动切换鱼竿: " + stateText(AutoFishingHandler.autoSwitchToRod);

        preferredSlotButton.displayString = "优先鱼竿槽位: "
                + (AutoFishingHandler.preferredRodSlot <= 0 ? "自动" : AutoFishingHandler.preferredRodSlot);

        disableWhenGuiButton.setEnabledState(AutoFishingHandler.disableWhenGuiOpen);
        disableWhenGuiButton.displayString = "打开本模组界面时暂停: " + stateText(AutoFishingHandler.disableWhenGuiOpen);

        allowMovingButton.setEnabledState(AutoFishingHandler.allowWhilePlayerMoving);
        allowMovingButton.displayString = "移动时继续工作: " + stateText(AutoFishingHandler.allowWhilePlayerMoving);

        statusMessageButton.setEnabledState(AutoFishingHandler.sendStatusMessage);
        statusMessageButton.displayString = "发送状态提示: " + stateText(AutoFishingHandler.sendStatusMessage);

        autoCastOnStartButton.setEnabledState(AutoFishingHandler.enableAutoCastOnStart);
        autoCastOnStartButton.displayString = "开启后自动出杆: " + stateText(AutoFishingHandler.enableAutoCastOnStart);

        initialCastDelayButton.displayString = "首次出杆延迟: " + AutoFishingHandler.initialCastDelayTicks + " Tick";

        autoRecastButton.setEnabledState(AutoFishingHandler.autoRecastAfterCatch);
        autoRecastButton.displayString = "钓完后自动出杆: " + stateText(AutoFishingHandler.autoRecastAfterCatch);

        recastDelayMinButton.displayString = "补杆最小延迟: " + AutoFishingHandler.recastDelayMinTicks + " Tick";
        recastDelayMaxButton.displayString = "补杆最大延迟: " + AutoFishingHandler.recastDelayMaxTicks + " Tick";

        retryBobberMissingButton.setEnabledState(AutoFishingHandler.retryCastWhenBobberMissing);
        retryBobberMissingButton.displayString = "鱼漂缺失时重试: "
                + stateText(AutoFishingHandler.retryCastWhenBobberMissing);

        retryCastDelayButton.displayString = "重试出杆延迟: " + AutoFishingHandler.retryCastDelayTicks + " Tick";

        timeoutRecastButton.setEnabledState(AutoFishingHandler.timeoutRecastEnabled);
        timeoutRecastButton.displayString = "超时自动补杆: " + stateText(AutoFishingHandler.timeoutRecastEnabled);

        maxWaitButton.displayString = "最长等待咬钩: " + AutoFishingHandler.maxFishingWaitTicks + " Tick";

        biteModeButton.displayString = "咬钩判定模式: " + getBiteModeText(AutoFishingHandler.biteDetectMode);
        ignoreSettleButton.displayString = "忽略入水扰动: " + AutoFishingHandler.ignoreInitialBobberSettleTicks + " Tick";
        reelDelayButton.displayString = "咬钩后收杆延迟: " + AutoFishingHandler.reelDelayTicks + " Tick";
        verticalThresholdButton.displayString = "下沉阈值: " + formatFloat(AutoFishingHandler.minVerticalDropThreshold);
        horizontalThresholdButton.displayString = "水平位移阈值: "
                + formatFloat(AutoFishingHandler.minHorizontalMoveThreshold);
        confirmBiteButton.displayString = "咬钩确认 Tick: " + AutoFishingHandler.confirmBiteTicks;

        debugBiteButton.setEnabledState(AutoFishingHandler.debugBiteInfo);
        debugBiteButton.displayString = "显示咬钩调试: " + stateText(AutoFishingHandler.debugBiteInfo);

        postReelPauseButton.displayString = "收杆后等待: " + AutoFishingHandler.postReelPauseTicks + " Tick";
        preventDoubleReelButton.displayString = "防重复收杆: " + AutoFishingHandler.preventDoubleReelTicks + " Tick";

        recastOnlySuccessButton.setEnabledState(AutoFishingHandler.recastOnlyIfLootSuccess);
        recastOnlySuccessButton.displayString = "仅成功收杆后补杆: "
                + stateText(AutoFishingHandler.recastOnlyIfLootSuccess);

        resetHookGoneButton.setEnabledState(AutoFishingHandler.resetStateWhenHookGone);
        resetHookGoneButton.displayString = "鱼漂消失时重置: "
                + stateText(AutoFishingHandler.resetStateWhenHookGone);

        autoRecoverButton.setEnabledState(AutoFishingHandler.autoRecoverFromInterruptedCast);
        autoRecoverButton.displayString = "异常断杆后恢复: "
                + stateText(AutoFishingHandler.autoRecoverFromInterruptedCast);

        stopLowDuraButton.setEnabledState(AutoFishingHandler.stopWhenRodDurabilityLow);
        stopLowDuraButton.displayString = "耐久低时停止: "
                + stateText(AutoFishingHandler.stopWhenRodDurabilityLow);

        minDuraButton.displayString = "最低鱼竿耐久: " + AutoFishingHandler.minRodDurability;

        stopNoRodButton.setEnabledState(AutoFishingHandler.stopWhenNoRodFound);
        stopNoRodButton.displayString = "未找到鱼竿时停止: " + stateText(AutoFishingHandler.stopWhenNoRodFound);

        pauseHookEntityButton.setEnabledState(AutoFishingHandler.pauseWhenHookedEntity);
        pauseHookEntityButton.displayString = "钩到实体时暂停: " + stateText(AutoFishingHandler.pauseWhenHookedEntity);

        stopWorldChangeButton.setEnabledState(AutoFishingHandler.stopOnWorldChange);
        stopWorldChangeButton.displayString = "切图/换服时停止: " + stateText(AutoFishingHandler.stopOnWorldChange);

        recastDelayMinButton.enabled = AutoFishingHandler.autoRecastAfterCatch;
        recastDelayMaxButton.enabled = AutoFishingHandler.autoRecastAfterCatch;
        retryCastDelayButton.enabled = AutoFishingHandler.retryCastWhenBobberMissing;
        maxWaitButton.enabled = AutoFishingHandler.timeoutRecastEnabled;
        minDuraButton.enabled = AutoFishingHandler.stopWhenRodDurabilityLow;
    }

    private void relayoutButtons() {
        recalcLayout();

        int innerPadding = 12;
        int columnGap = 8;
        int buttonHeight = 20;
        int rowGap = 6;
        int rowStep = buttonHeight + rowGap;
        int sectionGap = 12;
        int sectionHeaderHeight = 18;

        int leftX = this.panelX + innerPadding;
        int buttonW = Math.max(90, (this.panelWidth - innerPadding * 2 - columnGap) / 2);
        int rightX = this.panelX + this.panelWidth - innerPadding - buttonW;

        int baseY = 0;

        int basicTop = baseY + sectionHeaderHeight;
        layoutScrollableButton(requireRodButton, leftX, basicTop, buttonW, buttonHeight);
        layoutScrollableButton(autoSwitchRodButton, rightX, basicTop, buttonW, buttonHeight);
        layoutScrollableButton(preferredSlotButton, leftX, basicTop + rowStep, buttonW, buttonHeight);
        layoutScrollableButton(disableWhenGuiButton, rightX, basicTop + rowStep, buttonW, buttonHeight);
        layoutScrollableButton(allowMovingButton, leftX, basicTop + rowStep * 2, buttonW, buttonHeight);
        layoutScrollableButton(statusMessageButton, rightX, basicTop + rowStep * 2, buttonW, buttonHeight);
        baseY = basicTop + rowStep * 3 + sectionGap;

        int castTop = baseY + sectionHeaderHeight;
        layoutScrollableButton(autoCastOnStartButton, leftX, castTop, buttonW, buttonHeight);
        layoutScrollableButton(initialCastDelayButton, rightX, castTop, buttonW, buttonHeight);
        layoutScrollableButton(autoRecastButton, leftX, castTop + rowStep, buttonW, buttonHeight);
        layoutScrollableButton(recastDelayMinButton, rightX, castTop + rowStep, buttonW, buttonHeight);
        layoutScrollableButton(recastDelayMaxButton, leftX, castTop + rowStep * 2, buttonW, buttonHeight);
        layoutScrollableButton(retryBobberMissingButton, rightX, castTop + rowStep * 2, buttonW, buttonHeight);
        layoutScrollableButton(retryCastDelayButton, leftX, castTop + rowStep * 3, buttonW, buttonHeight);
        layoutScrollableButton(timeoutRecastButton, rightX, castTop + rowStep * 3, buttonW, buttonHeight);
        layoutScrollableButton(maxWaitButton, leftX, castTop + rowStep * 4, buttonW, buttonHeight);
        baseY = castTop + rowStep * 5 + sectionGap;

        int biteTop = baseY + sectionHeaderHeight;
        layoutScrollableButton(biteModeButton, leftX, biteTop, buttonW, buttonHeight);
        layoutScrollableButton(ignoreSettleButton, rightX, biteTop, buttonW, buttonHeight);
        layoutScrollableButton(reelDelayButton, leftX, biteTop + rowStep, buttonW, buttonHeight);
        layoutScrollableButton(verticalThresholdButton, rightX, biteTop + rowStep, buttonW, buttonHeight);
        layoutScrollableButton(horizontalThresholdButton, leftX, biteTop + rowStep * 2, buttonW, buttonHeight);
        layoutScrollableButton(confirmBiteButton, rightX, biteTop + rowStep * 2, buttonW, buttonHeight);
        layoutScrollableButton(debugBiteButton, leftX, biteTop + rowStep * 3, buttonW, buttonHeight);
        baseY = biteTop + rowStep * 4 + sectionGap;

        int reelTop = baseY + sectionHeaderHeight;
        layoutScrollableButton(postReelPauseButton, leftX, reelTop, buttonW, buttonHeight);
        layoutScrollableButton(preventDoubleReelButton, rightX, reelTop, buttonW, buttonHeight);
        layoutScrollableButton(recastOnlySuccessButton, leftX, reelTop + rowStep, buttonW, buttonHeight);
        layoutScrollableButton(resetHookGoneButton, rightX, reelTop + rowStep, buttonW, buttonHeight);
        layoutScrollableButton(autoRecoverButton, leftX, reelTop + rowStep * 2, buttonW, buttonHeight);
        baseY = reelTop + rowStep * 3 + sectionGap;

        int safetyTop = baseY + sectionHeaderHeight;
        layoutScrollableButton(stopLowDuraButton, leftX, safetyTop, buttonW, buttonHeight);
        layoutScrollableButton(minDuraButton, rightX, safetyTop, buttonW, buttonHeight);
        layoutScrollableButton(stopNoRodButton, leftX, safetyTop + rowStep, buttonW, buttonHeight);
        layoutScrollableButton(pauseHookEntityButton, rightX, safetyTop + rowStep, buttonW, buttonHeight);
        layoutScrollableButton(stopWorldChangeButton, leftX, safetyTop + rowStep * 2, buttonW, buttonHeight);
        baseY = safetyTop + rowStep * 3;

        int totalContentHeight = baseY + buttonHeight;
        int visibleContentHeight = Math.max(24, this.contentBottom - this.contentTop);
        this.contentMaxScroll = Math.max(0, totalContentHeight - visibleContentHeight);
        this.contentScroll = clampInt(this.contentScroll, 0, this.contentMaxScroll);

        basicTop = sectionHeaderHeight;
        layoutScrollableButton(requireRodButton, leftX, basicTop, buttonW, buttonHeight);
        layoutScrollableButton(autoSwitchRodButton, rightX, basicTop, buttonW, buttonHeight);
        layoutScrollableButton(preferredSlotButton, leftX, basicTop + rowStep, buttonW, buttonHeight);
        layoutScrollableButton(disableWhenGuiButton, rightX, basicTop + rowStep, buttonW, buttonHeight);
        layoutScrollableButton(allowMovingButton, leftX, basicTop + rowStep * 2, buttonW, buttonHeight);
        layoutScrollableButton(statusMessageButton, rightX, basicTop + rowStep * 2, buttonW, buttonHeight);

        int offsetBase = basicTop + rowStep * 3 + sectionGap;
        castTop = offsetBase + sectionHeaderHeight;
        layoutScrollableButton(autoCastOnStartButton, leftX, castTop, buttonW, buttonHeight);
        layoutScrollableButton(initialCastDelayButton, rightX, castTop, buttonW, buttonHeight);
        layoutScrollableButton(autoRecastButton, leftX, castTop + rowStep, buttonW, buttonHeight);
        layoutScrollableButton(recastDelayMinButton, rightX, castTop + rowStep, buttonW, buttonHeight);
        layoutScrollableButton(recastDelayMaxButton, leftX, castTop + rowStep * 2, buttonW, buttonHeight);
        layoutScrollableButton(retryBobberMissingButton, rightX, castTop + rowStep * 2, buttonW, buttonHeight);
        layoutScrollableButton(retryCastDelayButton, leftX, castTop + rowStep * 3, buttonW, buttonHeight);
        layoutScrollableButton(timeoutRecastButton, rightX, castTop + rowStep * 3, buttonW, buttonHeight);
        layoutScrollableButton(maxWaitButton, leftX, castTop + rowStep * 4, buttonW, buttonHeight);

        offsetBase = castTop + rowStep * 5 + sectionGap;
        biteTop = offsetBase + sectionHeaderHeight;
        layoutScrollableButton(biteModeButton, leftX, biteTop, buttonW, buttonHeight);
        layoutScrollableButton(ignoreSettleButton, rightX, biteTop, buttonW, buttonHeight);
        layoutScrollableButton(reelDelayButton, leftX, biteTop + rowStep, buttonW, buttonHeight);
        layoutScrollableButton(verticalThresholdButton, rightX, biteTop + rowStep, buttonW, buttonHeight);
        layoutScrollableButton(horizontalThresholdButton, leftX, biteTop + rowStep * 2, buttonW, buttonHeight);
        layoutScrollableButton(confirmBiteButton, rightX, biteTop + rowStep * 2, buttonW, buttonHeight);
        layoutScrollableButton(debugBiteButton, leftX, biteTop + rowStep * 3, buttonW, buttonHeight);

        offsetBase = biteTop + rowStep * 4 + sectionGap;
        reelTop = offsetBase + sectionHeaderHeight;
        layoutScrollableButton(postReelPauseButton, leftX, reelTop, buttonW, buttonHeight);
        layoutScrollableButton(preventDoubleReelButton, rightX, reelTop, buttonW, buttonHeight);
        layoutScrollableButton(recastOnlySuccessButton, leftX, reelTop + rowStep, buttonW, buttonHeight);
        layoutScrollableButton(resetHookGoneButton, rightX, reelTop + rowStep, buttonW, buttonHeight);
        layoutScrollableButton(autoRecoverButton, leftX, reelTop + rowStep * 2, buttonW, buttonHeight);

        offsetBase = reelTop + rowStep * 3 + sectionGap;
        safetyTop = offsetBase + sectionHeaderHeight;
        layoutScrollableButton(stopLowDuraButton, leftX, safetyTop, buttonW, buttonHeight);
        layoutScrollableButton(minDuraButton, rightX, safetyTop, buttonW, buttonHeight);
        layoutScrollableButton(stopNoRodButton, leftX, safetyTop + rowStep, buttonW, buttonHeight);
        layoutScrollableButton(pauseHookEntityButton, rightX, safetyTop + rowStep, buttonW, buttonHeight);
        layoutScrollableButton(stopWorldChangeButton, leftX, safetyTop + rowStep * 2, buttonW, buttonHeight);

        int footerY = this.panelY + this.panelHeight - 28;
        int footerGap = 6;
        int footerButtonW = Math.max(64, (this.panelWidth - innerPadding * 2 - footerGap * 2) / 3);
        int footerTotalW = footerButtonW * 3 + footerGap * 2;
        int footerStartX = this.panelX + (this.panelWidth - footerTotalW) / 2;

        layoutFixedButton(saveButton, footerStartX, footerY, footerButtonW, 20);
        layoutFixedButton(defaultButton, footerStartX + footerButtonW + footerGap, footerY, footerButtonW, 20);
        layoutFixedButton(cancelButton, footerStartX + (footerButtonW + footerGap) * 2, footerY, footerButtonW, 20);
    }

    private void layoutScrollableButton(GuiButton button, int x, int baseY, int width, int height) {
        int y = this.contentTop + baseY - this.contentScroll;
        button.x = x;
        button.y = y;
        button.width = width;
        button.height = height;
        button.visible = y >= this.contentTop && y + height <= this.contentBottom;
    }

    private void layoutFixedButton(GuiButton button, int x, int y, int width, int height) {
        button.x = x;
        button.y = y;
        button.width = width;
        button.height = height;
        button.visible = true;
    }

    private boolean isInScrollableContent(int mouseX, int mouseY) {
        return mouseX >= this.panelX + 8
                && mouseX <= this.panelX + this.panelWidth - 8
                && mouseY >= this.contentTop
                && mouseY <= this.contentBottom;
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        int wheel = Mouse.getEventDWheel();
        if (wheel == 0 || this.contentMaxScroll <= 0) {
            return;
        }

        int mouseX = Mouse.getEventX() * this.width / this.mc.displayWidth;
        int mouseY = this.height - Mouse.getEventY() * this.height / this.mc.displayHeight - 1;
        if (!isInScrollableContent(mouseX, mouseY)) {
            return;
        }

        if (wheel < 0) {
            this.contentScroll = clampInt(this.contentScroll + 18, 0, this.contentMaxScroll);
        } else {
            this.contentScroll = clampInt(this.contentScroll - 18, 0, this.contentMaxScroll);
        }
        relayoutButtons();
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        switch (button.id) {
            case BTN_REQUIRE_ROD:
                AutoFishingHandler.requireFishingRod = !AutoFishingHandler.requireFishingRod;
                break;
            case BTN_AUTO_SWITCH_ROD:
                AutoFishingHandler.autoSwitchToRod = !AutoFishingHandler.autoSwitchToRod;
                break;
            case BTN_PREFERRED_SLOT:
                mc.displayGuiScreen(new GuiTextInput(this, "输入优先鱼竿槽位 (0=自动, 1-9)",
                        String.valueOf(AutoFishingHandler.preferredRodSlot), value -> {
                            int parsed = AutoFishingHandler.preferredRodSlot;
                            try {
                                parsed = Integer.parseInt(value.trim());
                            } catch (Exception ignored) {
                            }
                            AutoFishingHandler.preferredRodSlot = clampInt(parsed, 0, 9);
                            refreshButtonTexts();
                            mc.displayGuiScreen(this);
                        }));
                return;
            case BTN_DISABLE_WHEN_GUI:
                AutoFishingHandler.disableWhenGuiOpen = !AutoFishingHandler.disableWhenGuiOpen;
                break;
            case BTN_ALLOW_MOVING:
                AutoFishingHandler.allowWhilePlayerMoving = !AutoFishingHandler.allowWhilePlayerMoving;
                break;
            case BTN_STATUS_MESSAGE:
                AutoFishingHandler.sendStatusMessage = !AutoFishingHandler.sendStatusMessage;
                break;

            case BTN_AUTO_CAST_ON_START:
                AutoFishingHandler.enableAutoCastOnStart = !AutoFishingHandler.enableAutoCastOnStart;
                break;
            case BTN_INITIAL_CAST_DELAY:
                openIntInput("输入首次出杆延迟 Tick (0 - 100)", AutoFishingHandler.initialCastDelayTicks,
                        0, 100, value -> {
                            AutoFishingHandler.initialCastDelayTicks = value;
                            refreshButtonTexts();
                        });
                return;
            case BTN_AUTO_RECAST:
                AutoFishingHandler.autoRecastAfterCatch = !AutoFishingHandler.autoRecastAfterCatch;
                break;
            case BTN_RECAST_DELAY_MIN:
                openIntInput("输入补杆最小延迟 Tick (0 - 100)", AutoFishingHandler.recastDelayMinTicks,
                        0, 100, value -> {
                            AutoFishingHandler.recastDelayMinTicks = value;
                            if (AutoFishingHandler.recastDelayMaxTicks < value) {
                                AutoFishingHandler.recastDelayMaxTicks = value;
                            }
                            refreshButtonTexts();
                        });
                return;
            case BTN_RECAST_DELAY_MAX:
                openIntInput("输入补杆最大延迟 Tick", AutoFishingHandler.recastDelayMaxTicks,
                        AutoFishingHandler.recastDelayMinTicks, 100, value -> {
                            AutoFishingHandler.recastDelayMaxTicks = value;
                            refreshButtonTexts();
                        });
                return;
            case BTN_RETRY_BOBBER_MISSING:
                AutoFishingHandler.retryCastWhenBobberMissing = !AutoFishingHandler.retryCastWhenBobberMissing;
                break;
            case BTN_RETRY_CAST_DELAY:
                openIntInput("输入重试出杆延迟 Tick (5 - 100)", AutoFishingHandler.retryCastDelayTicks,
                        5, 100, value -> {
                            AutoFishingHandler.retryCastDelayTicks = value;
                            refreshButtonTexts();
                        });
                return;
            case BTN_TIMEOUT_RECAST:
                AutoFishingHandler.timeoutRecastEnabled = !AutoFishingHandler.timeoutRecastEnabled;
                break;
            case BTN_MAX_WAIT:
                openIntInput("输入最长等待咬钩时间 Tick (40 - 2400)", AutoFishingHandler.maxFishingWaitTicks,
                        40, 2400, value -> {
                            AutoFishingHandler.maxFishingWaitTicks = value;
                            refreshButtonTexts();
                        });
                return;

            case BTN_BITE_MODE:
                cycleBiteMode();
                break;
            case BTN_IGNORE_SETTLE:
                openIntInput("输入忽略入水扰动 Tick (0 - 40)", AutoFishingHandler.ignoreInitialBobberSettleTicks,
                        0, 40, value -> {
                            AutoFishingHandler.ignoreInitialBobberSettleTicks = value;
                            refreshButtonTexts();
                        });
                return;
            case BTN_REEL_DELAY:
                openIntInput("输入咬钩后收杆延迟 Tick (0 - 20)", AutoFishingHandler.reelDelayTicks,
                        0, 20, value -> {
                            AutoFishingHandler.reelDelayTicks = value;
                            refreshButtonTexts();
                        });
                return;
            case BTN_VERTICAL_THRESHOLD:
                openFloatInput("输入鱼漂下沉阈值 (0.01 - 1.0)",
                        AutoFishingHandler.minVerticalDropThreshold, 0.01F, 1.0F, value -> {
                            AutoFishingHandler.minVerticalDropThreshold = value;
                            refreshButtonTexts();
                        });
                return;
            case BTN_HORIZONTAL_THRESHOLD:
                openFloatInput("输入鱼漂水平位移阈值 (0.0 - 1.0)",
                        AutoFishingHandler.minHorizontalMoveThreshold, 0.0F, 1.0F, value -> {
                            AutoFishingHandler.minHorizontalMoveThreshold = value;
                            refreshButtonTexts();
                        });
                return;
            case BTN_CONFIRM_BITE:
                openIntInput("输入咬钩确认 Tick (1 - 5)", AutoFishingHandler.confirmBiteTicks,
                        1, 5, value -> {
                            AutoFishingHandler.confirmBiteTicks = value;
                            refreshButtonTexts();
                        });
                return;
            case BTN_DEBUG_BITE:
                AutoFishingHandler.debugBiteInfo = !AutoFishingHandler.debugBiteInfo;
                break;

            case BTN_POST_REEL_PAUSE:
                openIntInput("输入收杆后等待 Tick (0 - 40)", AutoFishingHandler.postReelPauseTicks,
                        0, 40, value -> {
                            AutoFishingHandler.postReelPauseTicks = value;
                            refreshButtonTexts();
                        });
                return;
            case BTN_PREVENT_DOUBLE_REEL:
                openIntInput("输入防重复收杆间隔 Tick (0 - 20)", AutoFishingHandler.preventDoubleReelTicks,
                        0, 20, value -> {
                            AutoFishingHandler.preventDoubleReelTicks = value;
                            refreshButtonTexts();
                        });
                return;
            case BTN_RECAST_ONLY_SUCCESS:
                AutoFishingHandler.recastOnlyIfLootSuccess = !AutoFishingHandler.recastOnlyIfLootSuccess;
                break;
            case BTN_RESET_HOOK_GONE:
                AutoFishingHandler.resetStateWhenHookGone = !AutoFishingHandler.resetStateWhenHookGone;
                break;
            case BTN_AUTO_RECOVER:
                AutoFishingHandler.autoRecoverFromInterruptedCast = !AutoFishingHandler.autoRecoverFromInterruptedCast;
                break;

            case BTN_STOP_LOW_DURA:
                AutoFishingHandler.stopWhenRodDurabilityLow = !AutoFishingHandler.stopWhenRodDurabilityLow;
                break;
            case BTN_MIN_DURA:
                openIntInput("输入最低鱼竿耐久 (1 - 64)", AutoFishingHandler.minRodDurability,
                        1, 64, value -> {
                            AutoFishingHandler.minRodDurability = value;
                            refreshButtonTexts();
                        });
                return;
            case BTN_STOP_NO_ROD:
                AutoFishingHandler.stopWhenNoRodFound = !AutoFishingHandler.stopWhenNoRodFound;
                break;
            case BTN_PAUSE_HOOK_ENTITY:
                AutoFishingHandler.pauseWhenHookedEntity = !AutoFishingHandler.pauseWhenHookedEntity;
                break;
            case BTN_STOP_WORLD_CHANGE:
                AutoFishingHandler.stopOnWorldChange = !AutoFishingHandler.stopOnWorldChange;
                break;

            case BTN_SAVE:
                AutoFishingHandler.saveConfig();
                mc.displayGuiScreen(parentScreen);
                return;
            case BTN_DEFAULT:
                applyDefaultValues();
                refreshButtonTexts();
                relayoutButtons();
                return;
            case BTN_CANCEL:
                AutoFishingHandler.loadConfig();
                mc.displayGuiScreen(parentScreen);
                return;
            default:
                break;
        }

        refreshButtonTexts();
        relayoutButtons();
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.drawDefaultBackground();

        GuiTheme.drawPanel(panelX, panelY, panelWidth, panelHeight);
        GuiTheme.drawTitleBar(panelX, panelY, panelWidth, "自动钓鱼设置", this.fontRenderer);

        int textY = panelY + 22;
        for (String line : instructionLines) {
            this.drawString(this.fontRenderer, line, panelX + 16, textY, GuiTheme.SUB_TEXT);
            textY += 10;
        }

        if (contentMaxScroll > 0) {
            drawRect(panelX + 8, contentTop - 2, panelX + panelWidth - 8, contentBottom + 2, 0x221A2533);

            int trackX = panelX + panelWidth - 10;
            int trackY = contentTop;
            int trackHeight = contentBottom - contentTop;
            int thumbHeight = Math.max(18,
                    (int) ((trackHeight / (float) Math.max(trackHeight, trackHeight + contentMaxScroll)) * trackHeight));
            int thumbTrack = Math.max(1, trackHeight - thumbHeight);
            int thumbY = trackY + (int) ((contentScroll / (float) Math.max(1, contentMaxScroll)) * thumbTrack);
            GuiTheme.drawScrollbar(trackX, trackY, 4, trackHeight, thumbY, thumbHeight);
        }

        drawSectionBox("基础控制", requireRodButton, autoSwitchRodButton, preferredSlotButton,
                disableWhenGuiButton, allowMovingButton, statusMessageButton);
        drawSectionBox("出杆设置", autoCastOnStartButton, initialCastDelayButton, autoRecastButton,
                recastDelayMinButton, recastDelayMaxButton, retryBobberMissingButton, retryCastDelayButton,
                timeoutRecastButton, maxWaitButton);
        drawSectionBox("咬钩判定", biteModeButton, ignoreSettleButton, reelDelayButton,
                verticalThresholdButton, horizontalThresholdButton, confirmBiteButton, debugBiteButton);
        drawSectionBox("收杆 / 补杆", postReelPauseButton, preventDoubleReelButton, recastOnlySuccessButton,
                resetHookGoneButton, autoRecoverButton);
        drawSectionBox("安全限制", stopLowDuraButton, minDuraButton, stopNoRodButton,
                pauseHookEntityButton, stopWorldChangeButton);

        super.drawScreen(mouseX, mouseY, partialTicks);

        if (biteModeButton.visible && isMouseOver(mouseX, mouseY, biteModeButton)) {
            drawHoveringText(Arrays.asList(
                    "§e咬钩判定模式",
                    "§7SMART: 综合下沉和位移判断，适合大多数服务器。",
                    "§7MOTION: 只看鱼漂运动，简单直接。",
                    "§7STRICT: 要求更严格，误判更少但更保守。"), mouseX, mouseY);
        } else if (autoRecastButton.visible && isMouseOver(mouseX, mouseY, autoRecastButton)) {
            drawHoveringText(Arrays.asList(
                    "§e钓完后自动出杆",
                    "§7收杆完成后自动等待一段随机延迟，",
                    "§7然后重新甩杆，形成完整挂机循环。"), mouseX, mouseY);
        } else if (retryBobberMissingButton.visible && isMouseOver(mouseX, mouseY, retryBobberMissingButton)) {
            drawHoveringText(Arrays.asList(
                    "§e鱼漂缺失时重试",
                    "§7用于处理甩杆后鱼漂实体未正常出现的情况，",
                    "§7开启后会按设定延迟重复尝试出杆。"), mouseX, mouseY);
        } else if (timeoutRecastButton.visible && isMouseOver(mouseX, mouseY, timeoutRecastButton)) {
            drawHoveringText(Arrays.asList(
                    "§e超时自动补杆",
                    "§7若长时间没有检测到咬钩，",
                    "§7会自动收杆并重新进入补杆流程。"), mouseX, mouseY);
        } else if (stopLowDuraButton.visible && isMouseOver(mouseX, mouseY, stopLowDuraButton)) {
            drawHoveringText(Arrays.asList(
                    "§e耐久低时停止",
                    "§7避免挂机时把鱼竿用坏。",
                    "§7当剩余耐久小于等于下方阈值时自动关闭。"), mouseX, mouseY);
        } else if (pauseHookEntityButton.visible && isMouseOver(mouseX, mouseY, pauseHookEntityButton)) {
            drawHoveringText(Arrays.asList(
                    "§e钩到实体时暂停",
                    "§7如果鱼钩意外勾到了实体，",
                    "§7开启后会暂停自动收杆，减少误操作。"), mouseX, mouseY);
        }
    }

    private void drawSectionBox(String title, GuiButton... buttons) {
        int minX = Integer.MAX_VALUE;
        int minY = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int maxY = Integer.MIN_VALUE;

        for (GuiButton button : buttons) {
            if (button == null || !button.visible) {
                continue;
            }
            minX = Math.min(minX, button.x);
            minY = Math.min(minY, button.y);
            maxX = Math.max(maxX, button.x + button.width);
            maxY = Math.max(maxY, button.y + button.height);
        }

        if (minX == Integer.MAX_VALUE) {
            return;
        }

        int boxX = minX - 6;
        int boxY = minY - 18;
        int boxW = (maxX - minX) + 12;
        int boxH = (maxY - minY) + 24;

        drawRect(boxX, boxY, boxX + boxW, boxY + boxH, 0x44202A36);
        drawHorizontalLine(boxX, boxX + boxW, boxY, 0xFF4FA6D9);
        drawHorizontalLine(boxX, boxX + boxW, boxY + boxH, 0xFF35536C);
        drawVerticalLine(boxX, boxY, boxY + boxH, 0xFF35536C);
        drawVerticalLine(boxX + boxW, boxY, boxY + boxH, 0xFF35536C);
        this.drawString(this.fontRenderer, "§b" + title, boxX + 6, boxY + 5, 0xFFE8F6FF);
    }

    private boolean isMouseOver(int mouseX, int mouseY, GuiButton button) {
        return mouseX >= button.x && mouseX <= button.x + button.width
                && mouseY >= button.y && mouseY <= button.y + button.height;
    }

    private void cycleBiteMode() {
        if (AutoFishingHandler.BITE_MODE_SMART.equalsIgnoreCase(AutoFishingHandler.biteDetectMode)) {
            AutoFishingHandler.biteDetectMode = AutoFishingHandler.BITE_MODE_MOTION_ONLY;
        } else if (AutoFishingHandler.BITE_MODE_MOTION_ONLY.equalsIgnoreCase(AutoFishingHandler.biteDetectMode)) {
            AutoFishingHandler.biteDetectMode = AutoFishingHandler.BITE_MODE_STRICT;
        } else {
            AutoFishingHandler.biteDetectMode = AutoFishingHandler.BITE_MODE_SMART;
        }
    }

    private String getBiteModeText(String mode) {
        if (AutoFishingHandler.BITE_MODE_MOTION_ONLY.equalsIgnoreCase(mode)) {
            return "仅运动";
        }
        if (AutoFishingHandler.BITE_MODE_STRICT.equalsIgnoreCase(mode)) {
            return "严格";
        }
        return "智能";
    }

    private void applyDefaultValues() {
        AutoFishingHandler.requireFishingRod = true;
        AutoFishingHandler.autoSwitchToRod = false;
        AutoFishingHandler.preferredRodSlot = 0;
        AutoFishingHandler.disableWhenGuiOpen = true;
        AutoFishingHandler.allowWhilePlayerMoving = false;
        AutoFishingHandler.sendStatusMessage = true;

        AutoFishingHandler.enableAutoCastOnStart = true;
        AutoFishingHandler.initialCastDelayTicks = 8;
        AutoFishingHandler.autoRecastAfterCatch = true;
        AutoFishingHandler.recastDelayMinTicks = 10;
        AutoFishingHandler.recastDelayMaxTicks = 16;
        AutoFishingHandler.retryCastWhenBobberMissing = true;
        AutoFishingHandler.retryCastDelayTicks = 20;
        AutoFishingHandler.timeoutRecastEnabled = true;
        AutoFishingHandler.maxFishingWaitTicks = 600;

        AutoFishingHandler.biteDetectMode = AutoFishingHandler.BITE_MODE_SMART;
        AutoFishingHandler.ignoreInitialBobberSettleTicks = 8;
        AutoFishingHandler.reelDelayTicks = 2;
        AutoFishingHandler.minVerticalDropThreshold = 0.08F;
        AutoFishingHandler.minHorizontalMoveThreshold = 0.03F;
        AutoFishingHandler.confirmBiteTicks = 1;
        AutoFishingHandler.debugBiteInfo = false;

        AutoFishingHandler.postReelPauseTicks = 6;
        AutoFishingHandler.preventDoubleReelTicks = 6;
        AutoFishingHandler.recastOnlyIfLootSuccess = false;
        AutoFishingHandler.resetStateWhenHookGone = true;
        AutoFishingHandler.autoRecoverFromInterruptedCast = true;

        AutoFishingHandler.stopWhenRodDurabilityLow = true;
        AutoFishingHandler.minRodDurability = 5;
        AutoFishingHandler.stopWhenNoRodFound = true;
        AutoFishingHandler.pauseWhenHookedEntity = true;
        AutoFishingHandler.stopOnWorldChange = true;
    }

    private void openIntInput(String title, int current, int min, int max, IntConsumer consumer) {
        mc.displayGuiScreen(new GuiTextInput(this, title, String.valueOf(current), value -> {
            int parsed = current;
            try {
                parsed = Integer.parseInt(value.trim());
            } catch (Exception ignored) {
            }
            parsed = clampInt(parsed, min, max);
            consumer.accept(parsed);
            mc.displayGuiScreen(this);
        }));
    }

    private void openFloatInput(String title, float current, float min, float max, FloatConsumer consumer) {
        mc.displayGuiScreen(new GuiTextInput(this, title, formatFloat(current), value -> {
            float parsed = current;
            try {
                parsed = Float.parseFloat(value.trim());
            } catch (Exception ignored) {
            }
            parsed = clampFloat(parsed, min, max);
            consumer.accept(parsed);
            mc.displayGuiScreen(this);
        }));
    }

    private int clampInt(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private float clampFloat(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    private String formatFloat(float value) {
        return String.format(Locale.ROOT, "%.2f", value);
    }

    private String stateText(boolean enabled) {
        return enabled ? "§a开启" : "§c关闭";
    }

    private interface IntConsumer {
        void accept(int value);
    }

    private interface FloatConsumer {
        void accept(float value);
    }
}