package com.zszl.zszlScriptMod.handlers;

import com.zszl.zszlScriptMod.config.DebugModule;
import com.zszl.zszlScriptMod.config.ModConfig;
import com.zszl.zszlScriptMod.gui.components.GuiTheme;
import com.zszl.zszlScriptMod.gui.components.ThemedButton;
import com.zszl.zszlScriptMod.gui.refine.GuiRefineIdViewer;
import com.zszl.zszlScriptMod.zszlScriptMod;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.input.Mouse;

import java.util.List;

@SideOnly(Side.CLIENT)
public class RefineModGuiHandler {

    private static Class<? extends GuiScreen> mainRefineGuiClass = null;
    private GuiTextField txtRefineTimes;

    @SubscribeEvent
    public void onGuiOpen(GuiOpenEvent event) {
        if (!RefineHelper.INSTANCE.isRefineContextActive || mainRefineGuiClass == null) {
            return;
        }

        GuiScreen newGui = event.getGui();
        if (newGui == null) {
            if (RefineHelper.INSTANCE.isAutoRefineResolveRunning()
                    && RefineHelper.INSTANCE.isAutoRefineRefreshInProgress()) {
                ModConfig.debugPrint(DebugModule.MAIL_GUI, "自动精炼分解刷新阶段触发 GuiOpen:null，保持上下文不重置。");
                return;
            }
            ModConfig.debugPrint(DebugModule.MAIL_GUI, "检测到精炼GUI关闭(newGui=null)，重置状态。");
            RefineHelper.INSTANCE.deactivateRefineContext("GuiOpen:null");
            mainRefineGuiClass = null;
            return;
        }

        if (!RefineHelper.INSTANCE.isRefineMainGuiOpen) {
            if (RefineHelper.INSTANCE.isAutoRefineResolveRunning()
                    && RefineHelper.INSTANCE.isAutoRefineRefreshInProgress()) {
                ModConfig.debugPrint(DebugModule.MAIL_GUI, "自动精炼分解刷新阶段主界面暂未打开，等待重开，不重置上下文。");
                return;
            }
            ModConfig.debugPrint(DebugModule.MAIL_GUI, "精炼主界面open状态已关闭，重置按钮上下文。");
            RefineHelper.INSTANCE.deactivateRefineContext("GuiOpen:refine-main-closed");
            mainRefineGuiClass = null;
            return;
        }
        boolean isStayingInRefineContext = (newGui != null) &&
                (mainRefineGuiClass.isInstance(newGui) || newGui instanceof GuiRefineIdViewer);

        if (!isStayingInRefineContext) {
            ModConfig.debugPrint(DebugModule.MAIL_GUI, "检测到离开精炼上下文，重置状态。");
            RefineHelper.INSTANCE.deactivateRefineContext("GuiOpen:leave-refine-context");
            mainRefineGuiClass = null;
        }
    }

    @SubscribeEvent
    public void onInitGuiPost(GuiScreenEvent.InitGuiEvent.Post event) {
        if (RefineHelper.INSTANCE.isRefineTicketValid) {
            RefineHelper.INSTANCE.isRefineContextActive = true;
            RefineHelper.INSTANCE.isRefineTicketValid = false;
            mainRefineGuiClass = event.getGui().getClass();
            ModConfig.debugPrint(DebugModule.MAIL_GUI,
                    "精炼门票有效，已激活精炼上下文并记录主GUI类: " + mainRefineGuiClass.getSimpleName());
        }

        if (RefineHelper.INSTANCE.isRefineContextActive && mainRefineGuiClass != null
                && mainRefineGuiClass.isInstance(event.getGui())) {
            ModConfig.debugPrint(DebugModule.MAIL_GUI, "精炼上下文激活，向主GUI注入主题按钮。");
            GuiScreen gui = event.getGui();
            List<GuiButton> buttonList = event.getButtonList();
            int panelX = 10;
            int panelY = gui.height / 2 - 108;
            int buttonWidth = 80;
            int buttonHeight = 20;
            int spacing = 4;

            if (buttonList.stream().noneMatch(b -> b.id == RefineHelper.BTN_ONE_KEY_REFINE)) {
                buttonList.add(
                        new ThemedButton(RefineHelper.BTN_ONE_KEY_REFINE, panelX, panelY, buttonWidth, buttonHeight,
                                "§a一键精炼"));
                panelY += buttonHeight + spacing;
                buttonList.add(
                        new ThemedButton(RefineHelper.BTN_ONE_KEY_RESOLVE, panelX, panelY, buttonWidth, buttonHeight,
                                "§d一键分解"));
                panelY += buttonHeight + spacing;
                buttonList.add(
                        new ThemedButton(RefineHelper.BTN_REFRESH_REFINE_GUI, panelX, panelY, buttonWidth, buttonHeight,
                                "§e刷新界面"));
                panelY += buttonHeight + spacing;
                buttonList.add(new ThemedButton(RefineHelper.BTN_ONE_KEY_REFINE_RESOLVE, panelX, panelY, buttonWidth,
                        buttonHeight, "§b一键精炼分解"));

                panelY += buttonHeight + 40;
                buttonList.add(new ThemedButton(RefineHelper.BTN_STOP_AUTO_REFINE, panelX, panelY, buttonWidth,
                        buttonHeight, "§c停止运行"));
                panelY += buttonHeight + spacing;
                buttonList.add(new ThemedButton(RefineHelper.BTN_VIEW_REFINE_IDS, panelX, panelY, buttonWidth,
                        buttonHeight, "§b查看精炼ID"));
            }

            if (txtRefineTimes == null) {
                txtRefineTimes = new GuiTextField(9301, net.minecraft.client.Minecraft.getMinecraft().fontRenderer, 0,
                        0, 80, 18);
                txtRefineTimes.setMaxStringLength(4);
                txtRefineTimes.setText("1");
                txtRefineTimes.setEnableBackgroundDrawing(false);
            }
        }
    }

    @SubscribeEvent
    public void onDrawScreenPre(GuiScreenEvent.DrawScreenEvent.Pre event) {
        if (!RefineHelper.INSTANCE.isRefineContextActive || !RefineHelper.INSTANCE.isRefineMainGuiOpen
                || mainRefineGuiClass == null
                || !mainRefineGuiClass.isInstance(event.getGui())) {
            return;
        }

        GuiScreen gui = event.getGui();
        int panelX = 10;
        int panelY = gui.height / 2 - 108;
        int buttonHeight = 20;
        int spacing = 4;

        if (txtRefineTimes != null) {
            int inputLabelY = panelY + (buttonHeight + spacing) * 4 + 6;
            int inputY = inputLabelY + 10;
            txtRefineTimes.x = panelX;
            txtRefineTimes.y = inputY;
            txtRefineTimes.width = 80;

            GuiTheme.drawInputFrame(txtRefineTimes.x - 1, txtRefineTimes.y - 1, txtRefineTimes.width,
                    txtRefineTimes.height, txtRefineTimes.isFocused(), true);
            txtRefineTimes.drawTextBox();
            gui.drawString(gui.mc.fontRenderer, "§7精炼次数", panelX, inputLabelY, 0xFFFFFF);
            String status = RefineHelper.INSTANCE.isAutoRefineResolveRunning()
                    ? ("§b剩余: " + RefineHelper.INSTANCE.getAutoRefineResolveRemainingTimes())
                    : "§7未运行";
            gui.drawString(gui.mc.fontRenderer, status, panelX, inputY + 22, 0xFFFFFF);
        }
    }

    @SubscribeEvent
    public void onActionPerformedPre(GuiScreenEvent.ActionPerformedEvent.Pre event) {
        if (!RefineHelper.INSTANCE.isRefineContextActive || !RefineHelper.INSTANCE.isRefineMainGuiOpen
                || mainRefineGuiClass == null
                || !mainRefineGuiClass.isInstance(event.getGui())) {
            return;
        }

        try {
            switch (event.getButton().id) {
                case RefineHelper.BTN_ONE_KEY_REFINE:
                    RefineHelper.INSTANCE.startOneKeyRefine();
                    event.setCanceled(true);
                    break;
                case RefineHelper.BTN_ONE_KEY_RESOLVE:
                    RefineHelper.INSTANCE.startOneKeyResolve();
                    event.setCanceled(true);
                    break;
                case RefineHelper.BTN_REFRESH_REFINE_GUI:
                    RefineHelper.INSTANCE.refreshRefineGui(event.getGui());
                    event.setCanceled(true);
                    break;
                case RefineHelper.BTN_ONE_KEY_REFINE_RESOLVE:
                    int times = 1;
                    if (txtRefineTimes != null) {
                        try {
                            times = Math.max(1, Integer.parseInt(txtRefineTimes.getText().trim()));
                        } catch (Exception ignored) {
                            times = 1;
                        }
                    }
                    RefineHelper.INSTANCE.startAutoRefineResolve(times, event.getGui());
                    event.setCanceled(true);
                    break;
                case RefineHelper.BTN_STOP_AUTO_REFINE:
                    RefineHelper.INSTANCE.stopAutoRefineResolve();
                    event.setCanceled(true);
                    break;
                case RefineHelper.BTN_VIEW_REFINE_IDS:
                    event.getGui().mc.displayGuiScreen(new GuiRefineIdViewer(event.getGui()));
                    event.setCanceled(true);
                    break;
                default:
                    break;
            }
        } catch (Exception e) {
            zszlScriptMod.LOGGER.error("[RefineModGuiHandler] 处理自定义按钮点击时出错。", e);
        }
    }

    @SubscribeEvent
    public void onMouseInputPre(GuiScreenEvent.MouseInputEvent.Pre event) {
        if (!RefineHelper.INSTANCE.isRefineContextActive || !RefineHelper.INSTANCE.isRefineMainGuiOpen
                || mainRefineGuiClass == null
                || !mainRefineGuiClass.isInstance(event.getGui())) {
            return;
        }

        if (txtRefineTimes == null || !Mouse.getEventButtonState() || Mouse.getEventButton() < 0) {
            return;
        }

        GuiScreen gui = event.getGui();
        int mouseX = Mouse.getEventX() * gui.width / gui.mc.displayWidth;
        int mouseY = gui.height - Mouse.getEventY() * gui.height / gui.mc.displayHeight - 1;
        txtRefineTimes.mouseClicked(mouseX, mouseY, Mouse.getEventButton());
    }

    @SubscribeEvent
    public void onKeyboardInputPre(GuiScreenEvent.KeyboardInputEvent.Pre event) {
        if (!RefineHelper.INSTANCE.isRefineContextActive || !RefineHelper.INSTANCE.isRefineMainGuiOpen
                || mainRefineGuiClass == null
                || !mainRefineGuiClass.isInstance(event.getGui())) {
            return;
        }
        if (txtRefineTimes == null || !txtRefineTimes.isFocused()) {
            return;
        }
        try {
            char typedChar = org.lwjgl.input.Keyboard.getEventCharacter();
            int keyCode = org.lwjgl.input.Keyboard.getEventKey();
            txtRefineTimes.textboxKeyTyped(typedChar, keyCode);
            event.setCanceled(true);
        } catch (Exception ignored) {
        }
    }
}
