// --- Full Java Content (src/main/java/com/zszl/zszlScriptMod/config/ModConfig.java) ---
package com.zszl.zszlScriptMod.config;
import com.zszl.zszlScriptMod.zszlScriptMod;
import com.zszl.zszlScriptMod.handlers.*;
import com.zszl.zszlScriptMod.handlers.AutoSigninOnlineHandler;
import com.zszl.zszlScriptMod.handlers.BlockReplacementHandler;
import com.zszl.zszlScriptMod.path.PathSequenceManager;
import com.zszl.zszlScriptMod.utils.CapturedIdRuleManager;
import com.zszl.zszlScriptMod.system.ServerFeatureVisibilityManager;
import com.zszl.zszlScriptMod.PerformanceMonitor;

import net.minecraft.client.Minecraft;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import java.util.EnumMap;
import java.util.Map;
import java.io.*;
import com.zszl.zszlScriptMod.handlers.AutoPickupHandler; // 新增导入

public class ModConfig {

    public static boolean showMouseCoordinates = false;
    public static boolean showHoverInfo = false;
    public static final String CONFIG_DIR = "config/我的世界脚本";
    public static final String LEGACY_CONFIG_DIR = "config/再生之路脚本";
    public static boolean ahkMoveMouseMode = false;

    public static boolean enableGuiListener = false;
    public static boolean enableGhostItemCopy = false;

    // !! 核心修改：新增自动暂停开关 !!
    public static boolean autoPauseOnMenuOpen = true;

    public static boolean isDebugModeEnabled = false; // 全局调试模式开关
    public static final Map<DebugModule, Boolean> debugFlags = new EnumMap<>(DebugModule.class);
    public static boolean isMouseDetached = false;
    static {
        for (DebugModule module : DebugModule.values()) {
            debugFlags.put(module, true); // 默认开启所有模块的调试，由总开关控制
        }
    }

    /**
     * 新的调试检查方法。
     * 只有当总开关和对应模块的开关都打开时，才返回true。
     * 
     * @param module 要检查的调试模块
     * @return 是否应为该模块输出调试日志
     */
    public static boolean isDebugFlagEnabled(DebugModule module) {
        if (!isDebugModeEnabled) {
            return false;
        }
        return debugFlags.getOrDefault(module, false);
    }

    /**
     * 向游戏内聊天框打印调试信息的方法。
     * 
     * @param module  调试信息所属的模块
     * @param message 要打印的消息
     */
    public static void debugPrint(DebugModule module, String message) {
        if (isDebugFlagEnabled(module) && Minecraft.getMinecraft().player != null) {
            // 使用 addScheduledTask 确保消息在主线程发送，避免多线程问题
            Minecraft.getMinecraft().addScheduledTask(() -> {
                Minecraft.getMinecraft().player.sendMessage(new TextComponentString(TextFormatting.AQUA + "[DEBUG: "
                        + module.getDisplayName() + "] " + TextFormatting.GRAY + message));
            });
        }
    }

    /**
     * 强制向游戏内聊天框打印调试信息，无视任何开关。
     * 
     * @param module  调试信息所属的模块
     * @param message 要打印的消息
     */
    public static void debugPrintForce(DebugModule module, String message) {
        if (Minecraft.getMinecraft().player != null) {
            Minecraft.getMinecraft().addScheduledTask(() -> {
                Minecraft.getMinecraft().player.sendMessage(new TextComponentString(TextFormatting.RED
                        + "[FORCE DEBUG: " + module.getDisplayName() + "] " + TextFormatting.YELLOW + message));
            });
        }
    }

    /**
     * 向日志文件打印调试信息，不输出到聊天框。
     *
     * @param module  调试信息所属的模块
     * @param message 要打印的消息
     */
    public static void debugLog(DebugModule module, String message) {
        if (isDebugFlagEnabled(module)) {
            zszlScriptMod.LOGGER.info("[DEBUG:{}|{}] {}", module.name(), module.getDisplayName(), message);
        }
    }

    /**
     * 强制向日志文件打印调试信息，无视任何开关。
     *
     * @param module  调试信息所属的模块
     * @param message 要打印的消息
     */
    public static void debugLogForce(DebugModule module, String message) {
        zszlScriptMod.LOGGER.info("[FORCE DEBUG:{}|{}] {}", module.name(), module.getDisplayName(), message);
    }

    /**
     * 加载所有配置
     */
    public static void loadAllConfigs() {
        AutoEatHandler.loadAutoEatConfig();
        ItemFilterHandler.loadFilterConfig();
        AutoSkillHandler.loadSkillConfig();
        AutoFollowHandler.loadFollowConfig();
        KillTimerHandler.loadConfig();
        DeathAutoRejoinHandler.loadConfig();
        ConditionalExecutionHandler.loadConfig();
        AutoPickupHandler.loadConfig();
        AutoUseItemHandler.loadConfig();
        BlockReplacementHandler.loadConfig();
        AutoSigninOnlineHandler.loadConfig();
        ServerFeatureVisibilityManager.loadConfig();
        PerformanceMonitor.loadConfig();
        PathSequenceManager.initializePathSequences();
        CapturedIdRuleManager.initialize();
    }

    /**
     * 检查是否有GUI打开
     * 
     * @return true if a GUI is open, false otherwise.
     */
    public static boolean isGuiOpen() {
        return Minecraft.getMinecraft().currentScreen != null;
    }
}

