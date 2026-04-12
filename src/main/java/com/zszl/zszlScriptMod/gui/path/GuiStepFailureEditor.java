package com.zszl.zszlScriptMod.gui.path;

import com.zszl.zszlScriptMod.gui.components.GuiTheme;
import com.zszl.zszlScriptMod.gui.components.ThemedButton;
import com.zszl.zszlScriptMod.gui.components.ThemedGuiScreen;
import com.zszl.zszlScriptMod.path.PathSequenceManager.PathStep;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import org.lwjgl.input.Keyboard;

import java.io.IOException;

public class GuiStepFailureEditor extends ThemedGuiScreen {

    private static final String[] EXHAUSTED_POLICIES = new String[] { "END_SEQUENCE", "RESTART_SEQUENCE" };

    private final GuiScreen parent;
    private final PathStep step;

    private GuiTextField retryCountField;
    private GuiTextField retryTimeoutField;
    private GuiButton exhaustedPolicyBtn;
    private int exhaustedPolicyIndex = 0;

    public GuiStepFailureEditor(GuiScreen parent, PathStep step) {
        this.parent = parent;
        this.step = step;
    }

    @Override
    public void initGui() {
        Keyboard.enableRepeatEvents(true);
        buttonList.clear();

        int panelW = Math.min(420, this.width - 24);
        int panelH = 220;
        int panelX = (this.width - panelW) / 2;
        int panelY = (this.height - panelH) / 2;
        int fieldX = panelX + 14;
        int fieldW = panelW - 28;
        int y = panelY + 38;

        retryCountField = new GuiTextField(4001, fontRenderer, fieldX, y, fieldW, 18);
        retryCountField.setText(String.valueOf(step == null ? 3 : step.getRetryCount()));
        y += 34;

        retryTimeoutField = new GuiTextField(4002, fontRenderer, fieldX, y, fieldW, 18);
        retryTimeoutField.setText(String.valueOf(step == null ? 5 : step.getPathRetryTimeoutSeconds()));
        y += 34;

        exhaustedPolicyBtn = new ThemedButton(10, fieldX, y, fieldW, 20, "");
        buttonList.add(exhaustedPolicyBtn);
        exhaustedPolicyIndex = indexOfPolicy(step == null ? "END_SEQUENCE" : step.getRetryExhaustedPolicy());

        int btnY = panelY + panelH - 28;
        buttonList.add(new ThemedButton(1, panelX + panelW - 184, btnY, 80, 20, "保存"));
        buttonList.add(new ThemedButton(2, panelX + panelW - 94, btnY, 80, 20, "返回"));
        refreshButtons();
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        switch (button.id) {
            case 1:
                saveToStep();
                mc.displayGuiScreen(parent);
                break;
            case 2:
                mc.displayGuiScreen(parent);
                break;
            case 10:
                exhaustedPolicyIndex = (exhaustedPolicyIndex + 1) % EXHAUSTED_POLICIES.length;
                refreshButtons();
                break;
            default:
                break;
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawDefaultBackground();
        int panelW = Math.min(420, this.width - 24);
        int panelH = 220;
        int panelX = (this.width - panelW) / 2;
        int panelY = (this.height - panelH) / 2;
        GuiTheme.drawPanel(panelX, panelY, panelW, panelH);
        GuiTheme.drawTitleBar(panelX, panelY, panelW, "寻路中断自动重试", fontRenderer);

        drawString(fontRenderer, "重试次数", retryCountField.x, retryCountField.y - 10, 0xFFFFFF);
        drawString(fontRenderer, "停留尝试限时(秒)", retryTimeoutField.x, retryTimeoutField.y - 10, 0xFFFFFF);
        drawString(fontRenderer, "重试耗尽后", exhaustedPolicyBtn.x, exhaustedPolicyBtn.y - 10, 0xFFFFFF);
        fontRenderer.drawSplitString(
                "说明：当前步骤有坐标寻路时，会监测人物是否长时间原地不动。超时后会重新发送寻路命令并扣减重试次数。"
                        + "重试次数默认 3，停留尝试限时默认 5 秒；填写 0 表示该步骤不进行自动重试。",
                panelX + 14, exhaustedPolicyBtn.y + 30, panelW - 28, 0xFFB8C7D9);

        super.drawScreen(mouseX, mouseY, partialTicks);
        drawThemedTextField(retryCountField);
        drawThemedTextField(retryTimeoutField);
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (keyCode == Keyboard.KEY_ESCAPE) {
            mc.displayGuiScreen(parent);
            return;
        }
        super.keyTyped(typedChar, keyCode);
        retryCountField.textboxKeyTyped(typedChar, keyCode);
        retryTimeoutField.textboxKeyTyped(typedChar, keyCode);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        retryCountField.mouseClicked(mouseX, mouseY, mouseButton);
        retryTimeoutField.mouseClicked(mouseX, mouseY, mouseButton);
    }

    private void saveToStep() {
        if (step == null) {
            return;
        }
        step.setRetryCount(parseInt(retryCountField, step.getRetryCount()));
        step.setPathRetryTimeoutSeconds(parseInt(retryTimeoutField, step.getPathRetryTimeoutSeconds()));
        step.setRetryExhaustedPolicy(EXHAUSTED_POLICIES[exhaustedPolicyIndex]);
    }

    private int parseInt(GuiTextField field, int fallback) {
        try {
            return Integer.parseInt(field.getText().trim());
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private int indexOfPolicy(String policy) {
        String normalized = policy == null ? "" : policy.trim().toUpperCase();
        for (int i = 0; i < EXHAUSTED_POLICIES.length; i++) {
            if (EXHAUSTED_POLICIES[i].equals(normalized)) {
                return i;
            }
        }
        return 0;
    }

    private void refreshButtons() {
        if (exhaustedPolicyBtn == null) {
            return;
        }
        if ("RESTART_SEQUENCE".equals(EXHAUSTED_POLICIES[exhaustedPolicyIndex])) {
            exhaustedPolicyBtn.displayString = "重试耗尽后: 重试开始序列";
        } else {
            exhaustedPolicyBtn.displayString = "重试耗尽后: 直接结束序列";
        }
    }
}
