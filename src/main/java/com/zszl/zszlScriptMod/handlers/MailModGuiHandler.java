// python auto.py -detail MailModGuiHandler.java
// --- Full Java Content (src/main/java/com/zszl/zszlScriptMod/handlers/MailModGuiHandler.java) ---
package com.zszl.zszlScriptMod.handlers;

import com.zszl.zszlScriptMod.zszlScriptMod;
import com.zszl.zszlScriptMod.config.DebugModule;
import com.zszl.zszlScriptMod.config.ModConfig;
import com.zszl.zszlScriptMod.gui.mail.GuiMailIdViewer;
import com.zszl.zszlScriptMod.gui.mail.GuiMailSettings;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiYesNo;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class MailModGuiHandler {

    private static Class<? extends GuiScreen> mainMailGuiClass = null;

    @SubscribeEvent
    public void onGuiOpen(GuiOpenEvent event) {
        if (event.getGui() != null) {
            MailConfig.load();
        }

        if (!MailHelper.INSTANCE.isMailContextActive || mainMailGuiClass == null) {
            return;
        }

        GuiScreen newGui = event.getGui();
        GuiScreen currentGui = Minecraft.getMinecraft().currentScreen;

        // 切屏过程中可能先经过 null（中间帧），此时不应立刻判定为“离开邮件上下文”。
        if (newGui == null) {
            return;
        }

        Package mainPkg = mainMailGuiClass.getPackage();
        Package newPkg = newGui.getClass().getPackage();
        boolean isSamePackageGui = mainPkg != null && newPkg != null
                && mainPkg.getName().equals(newPkg.getName());

        boolean isMailRelatedDialog = (newGui instanceof GuiYesNo) &&
                (currentGui != null) &&
                (mainMailGuiClass.isInstance(currentGui)
                        || currentGui instanceof GuiMailIdViewer
                        || currentGui instanceof GuiMailSettings);

        boolean isStayingInMailContext = (mainMailGuiClass.isInstance(newGui) ||
                newGui instanceof GuiMailIdViewer ||
                newGui instanceof GuiMailSettings ||
                isMailRelatedDialog ||
                isSamePackageGui);

        if (!isStayingInMailContext) {
            // !! 核心修改：确保所有调试输出都使用 debugPrint !!
            ModConfig.debugPrint(DebugModule.MAIL_GUI, "检测到离开邮件上下文，重置状态。");
            MailHelper.INSTANCE.deactivateMailContext("GuiOpen:leave-mail-context");
            MailHelper.INSTANCE.stopAutomation("离开邮件上下文(GuiOpenEvent)");
            mainMailGuiClass = null;
        }
    }

    @SubscribeEvent
    public void onInitGuiPost(GuiScreenEvent.InitGuiEvent.Post event) {
        if (MailHelper.INSTANCE.isFingerprintTicketValid) {
            MailHelper.INSTANCE.isMailContextActive = true;
            MailHelper.INSTANCE.isFingerprintTicketValid = false;
            mainMailGuiClass = event.getGui().getClass();
            // !! 核心修改：确保所有调试输出都使用 debugPrint !!
            ModConfig.debugPrint(DebugModule.MAIL_GUI, "门票有效，已激活邮件上下文并记录主GUI类: " + mainMailGuiClass.getSimpleName());
        }

        if (MailHelper.INSTANCE.isMailContextActive && mainMailGuiClass != null
                && mainMailGuiClass.isInstance(event.getGui())) {
            GuiScreen gui = event.getGui();
            java.util.List<GuiButton> buttonList = event.getButtonList();

            if (buttonList.stream().noneMatch(b -> b.id == MailHelper.BTN_RECEIVE_ALL_ID)) {
                // !! 核心修改：确保所有调试输出都使用 debugPrint !!
                ModConfig.debugPrint(DebugModule.MAIL_GUI, "邮件上下文激活，向主GUI注入按钮...");

                int panelX = 10;
                int panelY = gui.height / 2 - 100;
                int buttonWidth = 80;
                int buttonHeight = 20;
                int spacing = 5;

                buttonList.add(new GuiButton(MailHelper.BTN_RECEIVE_ALL_ID, panelX, panelY, buttonWidth, buttonHeight,
                        "§a一键领取"));
                panelY += buttonHeight + spacing;
                buttonList.add(new GuiButton(MailHelper.BTN_DELETE_ALL_ID, panelX, panelY, buttonWidth, buttonHeight,
                        "§e一键删除"));
                panelY += buttonHeight + spacing;
                buttonList.add(
                        new GuiButton(MailHelper.BTN_VIEW_IDS, panelX, panelY, buttonWidth, buttonHeight, "§b邮件ID查看"));
                panelY += buttonHeight + spacing;
                buttonList.add(
                        new GuiButton(MailHelper.BTN_SETTINGS_ID, panelX, panelY, buttonWidth, buttonHeight, "§6邮件设置"));
                panelY += buttonHeight + spacing + 10;
                buttonList.add(
                        new GuiButton(MailHelper.BTN_STOP_ID, panelX, panelY, buttonWidth, buttonHeight, "§c停止操作"));

                // !! 核心修改：确保所有调试输出都使用 debugPrint !!
                ModConfig.debugPrint(DebugModule.MAIL_GUI, "自定义按钮已注入。");
            }
        }
    }

    @SubscribeEvent
    public void onActionPerformedPre(GuiScreenEvent.ActionPerformedEvent.Pre event) {
        if (event == null || event.getButton() == null || event.getGui() == null) {
            return;
        }

        int id = event.getButton().id;
        boolean isMailButton = id == MailHelper.BTN_RECEIVE_ALL_ID
                || id == MailHelper.BTN_DELETE_ALL_ID
                || id == MailHelper.BTN_STOP_ID
                || id == MailHelper.BTN_VIEW_IDS
                || id == MailHelper.BTN_SETTINGS_ID;
        if (!isMailButton) {
            return;
        }

        // 收紧作用域：仅在邮件上下文相关界面中处理，避免误吞其它GUI点击事件。
        boolean inMailGuiScope = mainMailGuiClass != null
                && (mainMailGuiClass.isInstance(event.getGui())
                        || event.getGui() instanceof GuiMailIdViewer
                        || event.getGui() instanceof GuiMailSettings);
        if (!inMailGuiScope) {
            ModConfig.debugPrint(DebugModule.MAIL_GUI,
                    "忽略非邮件上下文按钮点击: gui=" + event.getGui().getClass().getSimpleName() + ", id=" + id);
            return;
        }

        // 容错：若因时序导致上下文标记被提前清空，但当前GUI仍在邮件作用域，则自动恢复上下文。
        if (!MailHelper.INSTANCE.isMailContextActive) {
            MailHelper.INSTANCE.isMailContextActive = true;
            ModConfig.debugPrint(DebugModule.MAIL_GUI,
                    "检测到邮件作用域按钮点击，已自动恢复邮件上下文标记。gui="
                            + event.getGui().getClass().getSimpleName());
        }

        try {
            switch (id) {
                case MailHelper.BTN_RECEIVE_ALL_ID:
                    MailHelper.INSTANCE.startAutoReceiveAll();
                    event.setCanceled(true);
                    break;
                case MailHelper.BTN_DELETE_ALL_ID:
                    MailHelper.INSTANCE.startAutoDeleteAll();
                    event.setCanceled(true);
                    break;
                case MailHelper.BTN_STOP_ID:
                    MailHelper.INSTANCE.stopAutomation("用户点击停止按钮");
                    event.setCanceled(true);
                    break;
                case MailHelper.BTN_VIEW_IDS:
                    MailHelper.INSTANCE.onMailSubPageOpened();
                    event.getGui().mc.displayGuiScreen(new GuiMailIdViewer(event.getGui()));
                    event.setCanceled(true);
                    break;
                case MailHelper.BTN_SETTINGS_ID:
                    MailHelper.INSTANCE.onMailSubPageOpened();
                    event.getGui().mc.displayGuiScreen(new GuiMailSettings(event.getGui()));
                    event.setCanceled(true);
                    break;
            }
        } catch (Exception e) {
            zszlScriptMod.LOGGER.error("[MailModGuiHandler] 处理自定义按钮点击时出错。", e);
        }
    }
}
