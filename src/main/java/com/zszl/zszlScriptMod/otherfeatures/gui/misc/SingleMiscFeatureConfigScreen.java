package com.zszl.zszlScriptMod.otherfeatures.gui.misc;

import com.zszl.zszlScriptMod.gui.components.GuiTextInput;
import com.zszl.zszlScriptMod.otherfeatures.gui.common.SimpleFeatureConfigScreen;
import com.zszl.zszlScriptMod.otherfeatures.handler.misc.MiscFeatureManager;
import com.zszl.zszlScriptMod.otherfeatures.handler.misc.MiscFeatureManager.FeatureState;
import net.minecraft.client.gui.GuiScreen;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class SingleMiscFeatureConfigScreen extends SimpleFeatureConfigScreen {

    public SingleMiscFeatureConfigScreen(GuiScreen parentScreen, String featureId, String title) {
        super(parentScreen, featureId, title);
    }

    @Override
    protected FeatureView getFeatureView() {
        FeatureState state = MiscFeatureManager.getFeature(this.featureId);
        if (state == null) {
            return new FeatureView("", "当前功能不存在。", false, true, false, "未找到");
        }
        return new FeatureView(state.name, state.description, state.isEnabled(), state.isStatusHudEnabled(),
                state.behaviorImplemented, MiscFeatureManager.getFeatureRuntimeSummary(this.featureId));
    }

    @Override
    protected void saveDraftState(boolean enabled, boolean statusHudEnabled) {
        MiscFeatureManager.setEnabled(this.featureId, enabled);
        MiscFeatureManager.setFeatureStatusHudEnabled(this.featureId, statusHudEnabled);
    }

    @Override
    protected void resetFeatureState() {
        MiscFeatureManager.resetFeature(this.featureId);
    }

    @Override
    protected String getDefaultTitle() {
        return "杂项功能设置";
    }

    @Override
    protected boolean hasPrimaryOptionButton() {
        return true;
    }

    @Override
    protected boolean hasSecondaryOptionButton() {
        return "auto_reconnect".equals(this.featureId);
    }

    @Override
    protected boolean hasTertiaryOptionButton() {
        return "auto_reconnect".equals(this.featureId);
    }

    @Override
    protected String getPrimaryOptionButtonLabel() {
        if ("auto_reconnect".equals(this.featureId)) {
            return "重连延迟 : " + MiscFeatureManager.getAutoReconnectDelayTicks() + " tick";
        }
        if ("auto_respawn".equals(this.featureId)) {
            return "复活冷却 : " + MiscFeatureManager.getAutoRespawnDelayTicks() + " tick";
        }
        return "无额外设置";
    }

    @Override
    protected String getSecondaryOptionButtonLabel() {
        if ("auto_reconnect".equals(this.featureId)) {
            if (MiscFeatureManager.isAutoReconnectInfiniteAttempts()) {
                return "最大尝试次数 : 当前已忽略";
            }
            return "最大尝试次数 : " + MiscFeatureManager.getAutoReconnectMaxAttempts();
        }
        return "";
    }

    @Override
    protected String getTertiaryOptionButtonLabel() {
        if ("auto_reconnect".equals(this.featureId)) {
            return "无限重试 : " + (MiscFeatureManager.isAutoReconnectInfiniteAttempts() ? "开启" : "关闭");
        }
        return "";
    }

    @Override
    protected String getOptionSectionHintText() {
        return "§7这里可以单独配置当前杂项功能的附加参数。";
    }

    @Override
    protected List<String> getPrimaryOptionTooltipLines() {
        if ("auto_reconnect".equals(this.featureId)) {
            return Arrays.asList("§e自动重连延迟",
                    "§7断线后等待多少 tick 再开始尝试重连。");
        }
        if ("auto_respawn".equals(this.featureId)) {
            return Arrays.asList("§e自动复活冷却",
                    "§7死亡界面出现后，等待多少 tick 再自动复活。");
        }
        return Collections.emptyList();
    }

    @Override
    protected List<String> getSecondaryOptionTooltipLines() {
        if ("auto_reconnect".equals(this.featureId)) {
            if (MiscFeatureManager.isAutoReconnectInfiniteAttempts()) {
                return Arrays.asList("§e最大尝试次数",
                        "§7当前处于无限重试模式，这个次数限制暂时不会生效。",
                        "§7关闭无限重试后会继续使用这里保存的次数。");
            }
            return Arrays.asList("§e最大尝试次数",
                    "§7自动重连失败时，最多连续尝试多少次。");
        }
        return Collections.emptyList();
    }

    @Override
    protected List<String> getTertiaryOptionTooltipLines() {
        if ("auto_reconnect".equals(this.featureId)) {
            return Arrays.asList("§e无限重试",
                    "§7开启后，自动重连不会再受“最大尝试次数”限制。",
                    "§7每次失败后仍会等待设定的重连延迟，再继续重试。");
        }
        return Collections.emptyList();
    }

    @Override
    protected boolean isSecondaryOptionButtonEnabled() {
        return "auto_reconnect".equals(this.featureId) && !MiscFeatureManager.isAutoReconnectInfiniteAttempts();
    }

    @Override
    protected void onPrimaryOptionButtonPressed() {
        if ("auto_reconnect".equals(this.featureId)) {
            openIntegerInput("输入自动重连延迟 (5 - 200 tick)", MiscFeatureManager.getAutoReconnectDelayTicks(), 5, 200,
                    MiscFeatureManager::setAutoReconnectDelayTicks);
            return;
        }
        if ("auto_respawn".equals(this.featureId)) {
            openIntegerInput("输入自动复活冷却 (1 - 100 tick)", MiscFeatureManager.getAutoRespawnDelayTicks(), 1, 100,
                    MiscFeatureManager::setAutoRespawnDelayTicks);
        }
    }

    @Override
    protected void onSecondaryOptionButtonPressed() {
        if (!"auto_reconnect".equals(this.featureId)) {
            return;
        }
        openIntegerInput("输入最大重连次数 (1 - 10)", MiscFeatureManager.getAutoReconnectMaxAttempts(), 1, 10,
                MiscFeatureManager::setAutoReconnectMaxAttempts);
    }

    @Override
    protected void onTertiaryOptionButtonPressed() {
        if (!"auto_reconnect".equals(this.featureId)) {
            return;
        }
        MiscFeatureManager.setAutoReconnectInfiniteAttempts(!MiscFeatureManager.isAutoReconnectInfiniteAttempts());
    }

    private void openIntegerInput(String titleText, int currentValue, int min, int max,
            java.util.function.IntConsumer setter) {
        this.mc.displayGuiScreen(new GuiTextInput(this, titleText, String.valueOf(currentValue), value -> {
            int parsed = currentValue;
            try {
                parsed = Integer.parseInt(value.trim());
            } catch (Exception ignored) {
            }
            setter.accept(Math.max(min, Math.min(max, parsed)));
            this.mc.displayGuiScreen(this);
        }));
    }
}
