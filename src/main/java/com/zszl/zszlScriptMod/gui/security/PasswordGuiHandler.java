package com.zszl.zszlScriptMod.gui.security;

import java.nio.charset.StandardCharsets;
import java.util.List;

import org.lwjgl.input.Mouse;

import com.zszl.zszlScriptMod.config.DebugModule;
import com.zszl.zszlScriptMod.config.ModConfig;
import com.zszl.zszlScriptMod.gui.security.PasswordManagerConfig.PasswordEntry;
import com.zszl.zszlScriptMod.utils.ModUtils;
import com.zszl.zszlScriptMod.utils.PacketCaptureHandler;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.play.client.CPacketCloseWindow;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class PasswordGuiHandler {

    private static final int BTN_FILL_PASSWORD = 42001;
    private static final int BTN_MANAGE_PASSWORD = 42002;
    private static final int BTN_CRACK_PASSWORD = 42003;

    private static final double TARGET_X = 97;
    private static final double TARGET_Y = 65;
    private static final double TARGET_Z = 184;
    private static final double RANGE = 2.0;
    private static final String FIXED_PACKET_PLAYER_ID = "quemima01";
    private static final long AUTO_LOGIN_DELAY_MS = 5000L;
    private static final long LOGIN_SUCCESS_RECENT_WINDOW_MS = 20000L;

    private static boolean injectedForCurrentGui = false;
    private static GuiScreen lastGui = null;
    private static boolean autoLoginProcessedInCurrentRange = false;
    private static boolean autoLoginSecondSendPending = false;
    private static long autoLoginSecondSendAt = 0L;
    private static boolean pendingFillUntilSessionReady = false;
    private static boolean pendingFillNotifyShown = false;
    private static long lastLoginSuccessChatAt = 0L;
    private static final GuiButton OVERLAY_BTN_FILL = new GuiButton(BTN_FILL_PASSWORD, 0, 0, 80, 20, "");
    private static final GuiButton OVERLAY_BTN_MANAGE = new GuiButton(BTN_MANAGE_PASSWORD, 0, 0, 80, 20, "");
    private static final GuiButton OVERLAY_BTN_CRACK = new GuiButton(BTN_CRACK_PASSWORD, 0, 0, 80, 20, "");
    private static final String LOGIN_SUCCESS_CHAT_KEYWORD = "已成功登录";

    @SubscribeEvent
    public void onChatMessage(ClientChatReceivedEvent event) {
        if (event == null || event.getMessage() == null) {
            return;
        }
        String text = event.getMessage().getUnformattedText();
        if (text != null && text.contains(LOGIN_SUCCESS_CHAT_KEYWORD)) {
            lastLoginSuccessChatAt = System.currentTimeMillis();
            if (autoLoginSecondSendPending) {
                autoLoginSecondSendPending = false;
                autoLoginSecondSendAt = 0L;
                ModConfig.debugPrint(DebugModule.PASSWORD_MANAGER,
                        "在第二次自动填充之前检测到登录成功聊天，取消第二次发送");
            }
        }
    }

    private static void updateOverlayButtonPosition(ScaledResolution sr) {
        refreshOverlayButtonText();
        int panelX = sr.getScaledWidth() - 90;
        int panelY = sr.getScaledHeight() / 2 - 40;
        int spacing = 5;
        OVERLAY_BTN_FILL.x = panelX;
        OVERLAY_BTN_FILL.y = panelY;
        OVERLAY_BTN_MANAGE.x = panelX;
        OVERLAY_BTN_MANAGE.y = panelY + OVERLAY_BTN_FILL.height + spacing;
        OVERLAY_BTN_CRACK.x = panelX;
        OVERLAY_BTN_CRACK.y = OVERLAY_BTN_MANAGE.y + OVERLAY_BTN_MANAGE.height + spacing;
    }

    private static void refreshOverlayButtonText() {
        OVERLAY_BTN_FILL.displayString = I18n.format("gui.password.fill");
        OVERLAY_BTN_MANAGE.displayString = I18n.format("gui.password.manage");
        OVERLAY_BTN_CRACK.displayString = I18n.format("gui.password.crack");
    }

    @SubscribeEvent
    public void onGuiOpen(GuiOpenEvent event) {
        // 关闭GUI时清理注入状态
        if (event.getGui() == null) {
            injectedForCurrentGui = false;
            lastGui = null;
        }
    }

    @SubscribeEvent
    public void onRenderOverlay(RenderGameOverlayEvent.Post event) {
        if (event.getType() != RenderGameOverlayEvent.ElementType.ALL) {
            return;
        }
        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null || mc.player == null || mc.currentScreen != null) {
            return;
        }
        if (!isInTargetRange()) {
            return;
        }

        ScaledResolution sr = new ScaledResolution(mc);
        updateOverlayButtonPosition(sr);
        int mouseX = Mouse.getX() * sr.getScaledWidth() / mc.displayWidth;
        int mouseY = sr.getScaledHeight() - Mouse.getY() * sr.getScaledHeight() / mc.displayHeight - 1;

        Gui.drawRect(OVERLAY_BTN_FILL.x - 2, OVERLAY_BTN_FILL.y - 2,
                OVERLAY_BTN_FILL.x + OVERLAY_BTN_FILL.width + 2, OVERLAY_BTN_CRACK.y + OVERLAY_BTN_CRACK.height + 2,
                0x70000000);
        OVERLAY_BTN_FILL.drawButton(mc, mouseX, mouseY, 0);
        OVERLAY_BTN_MANAGE.drawButton(mc, mouseX, mouseY, 0);
        OVERLAY_BTN_CRACK.drawButton(mc, mouseX, mouseY, 0);
    }

    @SubscribeEvent
    public void onMouseInput(InputEvent.MouseInputEvent event) {
        if (!Mouse.getEventButtonState() || Mouse.getEventButton() != 0) {
            return;
        }

        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null || mc.player == null || mc.currentScreen != null || !isInTargetRange()) {
            return;
        }

        ScaledResolution sr = new ScaledResolution(mc);
        updateOverlayButtonPosition(sr);
        int mouseX = Mouse.getEventX() * sr.getScaledWidth() / mc.displayWidth;
        int mouseY = sr.getScaledHeight() - Mouse.getEventY() * sr.getScaledHeight() / mc.displayHeight - 1;

        if (OVERLAY_BTN_FILL.mousePressed(mc, mouseX, mouseY)) {
            OVERLAY_BTN_FILL.playPressSound(mc.getSoundHandler());
            sendFillPasswordPacket();
        } else if (OVERLAY_BTN_MANAGE.mousePressed(mc, mouseX, mouseY)) {
            OVERLAY_BTN_MANAGE.playPressSound(mc.getSoundHandler());
            mc.displayGuiScreen(new GuiPasswordManager(null));
        } else if (OVERLAY_BTN_CRACK.mousePressed(mc, mouseX, mouseY)) {
            OVERLAY_BTN_CRACK.playPressSound(mc.getSoundHandler());
            mc.player.sendMessage(
                    new net.minecraft.util.text.TextComponentString(I18n.format("gui.password.crack.todo")));
        }
    }

    @SubscribeEvent
    public void onInitGuiPost(GuiScreenEvent.InitGuiEvent.Post event) {
        if (event.getGui() != lastGui) {
            injectedForCurrentGui = false;
            lastGui = event.getGui();
        }

        if (!shouldInject()) {
            injectedForCurrentGui = false;
            return;
        }

        GuiScreen gui = event.getGui();
        List<GuiButton> buttonList = event.getButtonList();

        if (injectedForCurrentGui || hasAnyPasswordButtons(buttonList)) {
            injectedForCurrentGui = true;
            return;
        }

        int panelX = gui.width - 90;
        int panelY = gui.height / 2 - 40;
        int buttonWidth = 80;
        int buttonHeight = 20;
        int spacing = 5;

        buttonList.add(new GuiButton(BTN_FILL_PASSWORD, panelX, panelY, buttonWidth, buttonHeight,
                I18n.format("gui.password.fill")));
        panelY += buttonHeight + spacing;
        buttonList.add(new GuiButton(BTN_MANAGE_PASSWORD, panelX, panelY, buttonWidth, buttonHeight,
                I18n.format("gui.password.manage")));
        panelY += buttonHeight + spacing;
        buttonList.add(new GuiButton(BTN_CRACK_PASSWORD, panelX, panelY, buttonWidth, buttonHeight,
                I18n.format("gui.password.crack")));

        injectedForCurrentGui = true;
    }

    @SubscribeEvent
    public void onActionPerformedPre(GuiScreenEvent.ActionPerformedEvent.Pre event) {
        int id = event.getButton().id;
        if (id != BTN_FILL_PASSWORD && id != BTN_MANAGE_PASSWORD && id != BTN_CRACK_PASSWORD) {
            return;
        }

        event.setCanceled(true);

        if (Minecraft.getMinecraft().player == null) {
            return;
        }

        switch (id) {
            case BTN_FILL_PASSWORD:
                sendFillPasswordPacket();
                break;
            case BTN_MANAGE_PASSWORD:
                Minecraft.getMinecraft()
                        .displayGuiScreen(new GuiPasswordManager(Minecraft.getMinecraft().currentScreen));
                break;
            case BTN_CRACK_PASSWORD:
                Minecraft.getMinecraft().player
                        .sendMessage(new net.minecraft.util.text.TextComponentString(
                                I18n.format("gui.password.crack.todo")));
                break;
            default:
                break;
        }
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null || mc.player == null) {
            autoLoginProcessedInCurrentRange = false;
            autoLoginSecondSendPending = false;
            autoLoginSecondSendAt = 0L;
            pendingFillUntilSessionReady = false;
            pendingFillNotifyShown = false;
            lastLoginSuccessChatAt = 0L;
            return;
        }

        boolean inRange = isInTargetRange();
        if (!inRange) {
            autoLoginProcessedInCurrentRange = false;
            autoLoginSecondSendPending = false;
            autoLoginSecondSendAt = 0L;
            pendingFillUntilSessionReady = false;
            pendingFillNotifyShown = false;
            lastLoginSuccessChatAt = 0L;
            return;
        }

        if (pendingFillUntilSessionReady && isSessionReadyForFill()) {
            pendingFillUntilSessionReady = false;
            pendingFillNotifyShown = false;
            ModConfig.debugPrint(DebugModule.PASSWORD_MANAGER, "会话ID已准备好，正在执行延迟密码填充");
            sendFillPasswordPacket();
        }

        if (!autoLoginProcessedInCurrentRange) {
            autoLoginProcessedInCurrentRange = true;
            PasswordManagerConfig.load();

            PasswordEntry entry = resolveEntryForFill(false, false, false);
            if (entry != null && entry.autoLogin) {
                String pwd = entry.password == null ? "" : entry.password;
                if (!pwd.trim().isEmpty()) {
                    ModConfig.debugPrint(DebugModule.PASSWORD_MANAGER,
                            "自动登录触发： sourceAccount='" + entry.playerId
                                    + "', 现在就发送，5秒后再发送一次");
                    sendFillPasswordPacket();
                    autoLoginSecondSendPending = true;
                    autoLoginSecondSendAt = System.currentTimeMillis() + AUTO_LOGIN_DELAY_MS;
                }
            }
        }

        if (autoLoginSecondSendPending && System.currentTimeMillis() >= autoLoginSecondSendAt) {
            if (hasRecentLoginSuccessChat()) {
                autoLoginSecondSendPending = false;
                autoLoginSecondSendAt = 0L;
                ModConfig.debugPrint(DebugModule.PASSWORD_MANAGER,
                        "由于最近登录成功聊天，第二次自动填充被跳过");
                return;
            }
            autoLoginSecondSendPending = false;
            autoLoginSecondSendAt = 0L;
            ModConfig.debugPrint(DebugModule.PASSWORD_MANAGER, "自动登录，第二次发送触发");
            sendFillPasswordPacket();
        }
    }

    private static boolean shouldInject() {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null || mc.player == null || mc.currentScreen == null) {
            return false;
        }

        return isInTargetRange();
    }

    private static boolean isInTargetRange() {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null || mc.player == null) {
            return false;
        }

        EntityPlayer player = mc.player;
        double dx = player.posX - TARGET_X;
        double dy = player.posY - TARGET_Y;
        double dz = player.posZ - TARGET_Z;
        double distanceSq = dx * dx + dy * dy + dz * dz;
        return distanceSq <= (RANGE * RANGE);
    }

    private static boolean hasAnyPasswordButtons(List<GuiButton> buttonList) {
        for (GuiButton button : buttonList) {
            if (button.id == BTN_FILL_PASSWORD || button.id == BTN_MANAGE_PASSWORD || button.id == BTN_CRACK_PASSWORD) {
                return true;
            }
        }
        return false;
    }

    private static void sendFillPasswordPacket() {
        sendCloseCurrentWindowPacketBeforeFill();

        if (!isSessionReadyForFill()) {
            pendingFillUntilSessionReady = true;
            if (!pendingFillNotifyShown && Minecraft.getMinecraft().player != null) {
                pendingFillNotifyShown = true;
                Minecraft.getMinecraft().player.sendMessage(new net.minecraft.util.text.TextComponentString(
                        I18n.format("gui.password.session_waiting")));
            }
            ModConfig.debugPrint(DebugModule.PASSWORD_MANAGER,
                    "填充延迟：会话ID或连接未准备好，等待捕获然后自动发送。");
            return;
        }

        PasswordManagerConfig.load();

        PasswordEntry entry = resolveEntryForFill(true, true, false);

        if (entry == null || entry.playerId == null || entry.playerId.trim().isEmpty()) {
            ModConfig.debugPrint(DebugModule.PASSWORD_MANAGER, "填写失败：账户缺失或账户ID为空");
            Minecraft.getMinecraft().player.sendMessage(
                    new net.minecraft.util.text.TextComponentString(
                            I18n.format("gui.password.error.no_password_for_current")));
            return;
        }

        String playerId = entry.playerId.trim();
        String password = entry.password == null ? "" : entry.password;
        ModConfig.debugPrint(DebugModule.PASSWORD_MANAGER,
                "准备填充有效载荷： id='" + playerId + "', rawPwdLen=" + password.length() + ", trimmedPwdLen="
                        + password.trim().length());

        if (password.trim().isEmpty()) {
            ModConfig.debugPrint(DebugModule.PASSWORD_MANAGER,
                    "填写失败：账户 '" + playerId + "'空密码");
            Minecraft.getMinecraft().player.sendMessage(
                    new net.minecraft.util.text.TextComponentString(
                            I18n.format("gui.password.error.empty_password_for", playerId)));
            return;
        }

        String packetHex = buildOwlViewFillPacket(password);
        ModConfig.debugPrint(DebugModule.PASSWORD_MANAGER,
                "发送填充密码包： channel=OwlViewChannel, packetId='" + FIXED_PACKET_PLAYER_ID
                        + "', sourceAccount='" + playerId + "', hexLen=" + packetHex.length());

        if (ModConfig.isDebugFlagEnabled(DebugModule.PASSWORD_MANAGER)) {
            GuiScreen.setClipboardString(packetHex);
            Minecraft.getMinecraft().player.sendMessage(
                    new net.minecraft.util.text.TextComponentString(I18n.format("gui.password.debug.hex_copied")));
        }

        ModUtils.sendFmlPacket("OwlViewChannel", packetHex);
    }

    private static void sendCloseCurrentWindowPacketBeforeFill() {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null || mc.player == null || mc.getConnection() == null) {
            return;
        }

        int windowId = mc.player.openContainer != null ? mc.player.openContainer.windowId : 0;
        mc.getConnection().sendPacket(new CPacketCloseWindow(windowId));
        ModConfig.debugPrint(DebugModule.PASSWORD_MANAGER,
                "填充前已发送关闭窗口包：CPacketCloseWindow, windowId=" + windowId + " (0x08)");
    }

    private static boolean hasRecentLoginSuccessChat() {
        long ts = lastLoginSuccessChatAt;
        return ts > 0L && (System.currentTimeMillis() - ts) <= LOGIN_SUCCESS_RECENT_WINDOW_MS;
    }

    private static boolean isSessionReadyForFill() {
        Minecraft mc = Minecraft.getMinecraft();
        return mc != null && mc.getConnection() != null && PacketCaptureHandler.getSessionIdAsHex() != null;
    }

    private static PasswordEntry resolveEntryForFill(boolean updateSelectedOnCurrentMatch, boolean debugLog,
            boolean allowFallbackToSelected) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null || mc.player == null) {
            return null;
        }

        String currentPlayerName = mc.player.getName();
        PasswordEntry currentPlayerEntry = PasswordManagerConfig.findEntryByPlayerId(currentPlayerName);
        PasswordEntry selectedEntry = PasswordManagerConfig.getSelectedEntry();
        PasswordEntry entry = null;

        if (debugLog) {
            ModConfig.debugPrint(DebugModule.PASSWORD_MANAGER,
                    "开始填写密码：currentPlayer='" + currentPlayerName + "'");
        }

        if (currentPlayerEntry != null) {
            String currentPwd = currentPlayerEntry.password == null ? "" : currentPlayerEntry.password;
            if (debugLog) {
                ModConfig.debugPrint(DebugModule.PASSWORD_MANAGER,
                        "匹配的同名账户： id='" + currentPlayerEntry.playerId + "', passwordLen="
                                + currentPwd.length());
            }
            if (!currentPwd.trim().isEmpty()) {
                entry = currentPlayerEntry;
                if (updateSelectedOnCurrentMatch) {
                    PasswordManagerConfig.setSelectedPlayerId(currentPlayerEntry.playerId);
                }
            } else if (debugLog) {
                ModConfig.debugPrint(DebugModule.PASSWORD_MANAGER,
                        "同名账户密码为空，尝试回退到所选账户");
            }
        }

        if (entry == null && allowFallbackToSelected) {
            entry = selectedEntry;
            if (debugLog) {
                if (selectedEntry != null) {
                    String selectedPwd = selectedEntry.password == null ? "" : selectedEntry.password;
                    ModConfig.debugPrint(DebugModule.PASSWORD_MANAGER,
                            "使用选定账户t: id='" + selectedEntry.playerId + "', passwordLen="
                                    + selectedPwd.length());
                } else {
                    ModConfig.debugPrint(DebugModule.PASSWORD_MANAGER, "没有选定账户可用");
                }
            }
        } else if (entry == null && debugLog) {
            ModConfig.debugPrint(DebugModule.PASSWORD_MANAGER,
                    "当前玩家未匹配密码输入，且已禁用备份至所选账户： player='"
                            + currentPlayerName + "'");
        }

        return entry;
    }

    private static String buildOwlViewFillPacket(String password) {
        String prefix = "00 09 43 6F 6D 70 6F 6E 65 6E 74 00 00 {id} 00 00 00 21 00 0C 42 75 74 74 6F 6E 5F 63 6C 69 63 6B 01 00 00 00 00 00 00 00 00 00 00 00 00 01";

        String playerId = FIXED_PACKET_PLAYER_ID;
        String idLenHex = shortToHex(playerId.getBytes(StandardCharsets.UTF_8).length);
        String pwdLenHex = shortToHex(password.getBytes(StandardCharsets.UTF_8).length);
        String idHex = stringToHex(playerId);
        String pwdHex = stringToHex(password);

        return prefix + " " + idLenHex + " " + idHex + " " + pwdLenHex + " " + pwdHex;
    }

    private static String shortToHex(int v) {
        int value = v & 0xFFFF;
        return String.format("%02X %02X", (value >> 8) & 0xFF, value & 0xFF);
    }

    private static String stringToHex(String s) {
        byte[] bytes = s.getBytes(StandardCharsets.UTF_8);
        if (bytes.length == 0) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < bytes.length; i++) {
            sb.append(String.format("%02X", bytes[i] & 0xFF));
            if (i < bytes.length - 1) {
                sb.append(" ");
            }
        }
        return sb.toString();
    }
}
