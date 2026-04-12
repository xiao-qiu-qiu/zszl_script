package com.zszl.zszlScriptMod.otherfeatures.handler.misc.runtime;

import com.zszl.zszlScriptMod.zszlScriptMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiDisconnected;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.multiplayer.GuiConnecting;
import net.minecraft.client.multiplayer.ServerData;

public final class AutoReconnectRuntime {

    private ServerData pendingReconnectServer = null;
    private int reconnectDelayTicks = 0;
    private int reconnectKeepAliveTicks = 0;
    private int reconnectAttemptCount = 0;

    public void onClientDisconnected(Minecraft mc, boolean featureEnabled, int reconnectDelayTicks) {
        if (!featureEnabled || mc == null || mc.isSingleplayer()) {
            clearState();
            return;
        }

        ServerData current = mc.getCurrentServerData();
        if (current == null || current.serverIP == null || current.serverIP.trim().isEmpty()) {
            clearState();
            return;
        }

        this.pendingReconnectServer = new ServerData(current.serverName, current.serverIP, false);
        this.reconnectDelayTicks = reconnectDelayTicks;
        this.reconnectKeepAliveTicks = 200;
        this.reconnectAttemptCount = 0;
    }

    public void onClientConnected() {
        clearState();
    }

    public void tick(boolean featureEnabled, boolean infiniteAttempts, int maxAttempts, int retryDelayTicks, Minecraft mc) {
        if (!featureEnabled || this.pendingReconnectServer == null || mc == null) {
            return;
        }
        if (mc.player != null && mc.world != null) {
            clearState();
            return;
        }
        if (mc.currentScreen instanceof GuiConnecting) {
            return;
        }
        if (mc.currentScreen instanceof GuiDisconnected) {
            if (this.reconnectDelayTicks > 0) {
                this.reconnectDelayTicks--;
                return;
            }
            if (!infiniteAttempts && this.reconnectAttemptCount >= maxAttempts) {
                clearState();
                return;
            }
            try {
                mc.displayGuiScreen(new GuiConnecting(new GuiMainMenu(), mc, this.pendingReconnectServer));
                this.reconnectAttemptCount++;
                this.reconnectDelayTicks = retryDelayTicks;
            } catch (Exception e) {
                zszlScriptMod.LOGGER.debug("自动重连执行失败", e);
                clearState();
            }
            return;
        }
        if (this.reconnectKeepAliveTicks > 0) {
            this.reconnectKeepAliveTicks--;
            return;
        }
        clearState();
    }

    public void clearState() {
        this.pendingReconnectServer = null;
        this.reconnectDelayTicks = 0;
        this.reconnectKeepAliveTicks = 0;
        this.reconnectAttemptCount = 0;
    }

    public ServerData getPendingReconnectServer() {
        return this.pendingReconnectServer;
    }

    public int getReconnectDelayTicks() {
        return this.reconnectDelayTicks;
    }

    public int getReconnectAttemptCount() {
        return this.reconnectAttemptCount;
    }
}
