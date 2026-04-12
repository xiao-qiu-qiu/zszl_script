// 文件路径: src/main/java/com/keycommand2/zszlScriptMod/handlers/QuickExchangeHandler.java
// (这是修复了聊天消息崩溃问题的最终版本)
package com.zszl.zszlScriptMod.handlers;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.zszl.zszlScriptMod.zszlScriptMod;
import com.zszl.zszlScriptMod.system.ProfileManager;
import com.zszl.zszlScriptMod.utils.ModUtils;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.gui.inventory.GuiChest;
import net.minecraft.inventory.ClickType;
import net.minecraft.inventory.ContainerChest;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString; // !! 核心修复：导入正确的类 !!
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class QuickExchangeHandler {

    public static final QuickExchangeHandler INSTANCE = new QuickExchangeHandler();
    private static final Minecraft mc = Minecraft.getMinecraft();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    // --- 运行时状态 ---
    public static boolean isExchangingAll = false; // Shift+Click 的状态锁
    private static Slot targetSlot = null;
    private static int exchangeTaskID = 0; // 用于取消延迟任务

    // --- 配置 ---
    public static class Settings {
        public int clickIntervalMs = 100;
        public boolean shiftClickEnabled = true;
        public boolean ctrlClickEnabled = true;
    }

    public static Settings settings = new Settings();

    private QuickExchangeHandler() {
    }

    private static Path getConfigFile() {
        return ProfileManager.getCurrentProfileDir().resolve("quick_exchange_config.json");
    }

    public static void loadConfig() {
        Path configFile = getConfigFile();
        if (Files.exists(configFile)) {
            try (BufferedReader reader = Files.newBufferedReader(configFile, StandardCharsets.UTF_8)) {
                settings = GSON.fromJson(reader, Settings.class);
                if (settings == null) {
                    settings = new Settings();
                }
            } catch (Exception e) {
                zszlScriptMod.LOGGER.error(I18n.format("log.quick_exchange.load_failed"), e);
                settings = new Settings();
            }
        }
    }

    public static void saveConfig() {
        Path configFile = getConfigFile();
        try {
            Files.createDirectories(configFile.getParent());
            try (BufferedWriter writer = Files.newBufferedWriter(configFile, StandardCharsets.UTF_8)) {
                GSON.toJson(settings, writer);
            }
        } catch (Exception e) {
            zszlScriptMod.LOGGER.error(I18n.format("log.quick_exchange.save_failed"), e);
        }
    }

    public static boolean isQuickExchangeGui(GuiChest gui) {
        if (gui.inventorySlots instanceof ContainerChest) {
            ContainerChest container = (ContainerChest) gui.inventorySlots;
            String title = container.getLowerChestInventory().getDisplayName().getUnformattedText();

            if (title.contains(I18n.format("quick_exchange.gui.keyword"))) {
                if (container.inventorySlots.size() > 1) {
                    ItemStack slot0 = container.getSlot(0).getStack();
                    ItemStack slot1 = container.getSlot(1).getStack();
                    if (!slot0.isEmpty() && !slot1.isEmpty()) {
                        String nameSlot0 = slot0.getDisplayName();
                        String nameSlot1 = slot1.getDisplayName();

                        return nameSlot0.contains(I18n.format("quick_exchange.item.ticket60"))
                                && nameSlot1.contains(I18n.format("quick_exchange.item.ticket360"));
                    }
                }
            }
        }
        return false;
    }

    public static void handleClick(Slot slot, boolean isShiftDown, boolean isCtrlDown, int ctrlAmount) {
        if (isExchangingAll) {
            stopExchangeAll();
            return;
        }
        if (isShiftDown && settings.shiftClickEnabled) {
            startExchangeAll(slot);
        } else if (isCtrlDown && settings.ctrlClickEnabled) {
            startExchangeSetAmount(slot, ctrlAmount);
        }
    }

    private static void startExchangeAll(Slot slot) {
        if (mc.player == null || mc.player.openContainer == null || slot == null || !slot.getHasStack())
            return;

        // [新增] 捕获当前容器的窗口ID
        final int windowId = mc.player.openContainer.windowId;

        isExchangingAll = true;
        targetSlot = slot;
        mc.player.sendMessage(
                new TextComponentString(I18n.format("msg.quick_exchange.start_all", slot.getStack().getDisplayName())));

        // 将 windowId 传递给调度方法
        scheduleNextClick(windowId);
    }

    private static void scheduleNextClick(final int windowId) {
        if (!isExchangingAll || targetSlot == null) {
            stopExchangeAll();
            return;
        }
        exchangeTaskID = (int) (Math.random() * 10000);
        final int currentTaskID = exchangeTaskID;

        ModUtils.DelayScheduler.instance.schedule(() -> {
            // [核心修复] 在执行点击前添加完整的安全检查
            if (!isExchangingAll || currentTaskID != exchangeTaskID || mc.player == null ||
                    !(mc.currentScreen instanceof GuiChest) || mc.player.openContainer == null ||
                    mc.player.openContainer.windowId != windowId) {

                // 如果任何一个条件不满足，说明GUI已关闭或已改变，安全地停止兑换
                stopExchangeAll();
                return;
            }

            mc.playerController.windowClick(windowId, targetSlot.slotNumber, 0, ClickType.PICKUP, mc.player);
            scheduleNextClick(windowId); // 递归调用时继续传递 windowId

        }, settings.clickIntervalMs / 50);
    }

    private static void stopExchangeAll() {
        if (isExchangingAll) {
            isExchangingAll = false;
            targetSlot = null;
            exchangeTaskID = -1;
            if (mc.player != null && mc.currentScreen == null) { // 只有在GUI已关闭时才发消息，避免干扰
                mc.player.sendMessage(new TextComponentString(I18n.format("msg.quick_exchange.stopped")));
            }
        }
    }

    private static void startExchangeSetAmount(Slot slot, int amount) {
        if (mc.player == null || mc.player.openContainer == null || slot == null || !slot.getHasStack() || amount <= 0)
            return;

        // [新增] 捕获当前容器的窗口ID
        final int windowId = mc.player.openContainer.windowId;

        mc.player.sendMessage(new TextComponentString(
                I18n.format("msg.quick_exchange.start_amount", amount, slot.getStack().getDisplayName())));
        for (int i = 0; i < amount; i++) {
            final int currentIteration = i;
            ModUtils.DelayScheduler.instance.schedule(() -> {
                // [核心修复] 在执行点击前添加完整的安全检查
                if (mc.player == null || !(mc.currentScreen instanceof GuiChest) ||
                        mc.player.openContainer == null || mc.player.openContainer.windowId != windowId) {
                    // 如果GUI关闭，静默停止后续点击即可
                    return;
                }

                mc.playerController.windowClick(windowId, slot.slotNumber, 0, ClickType.PICKUP, mc.player);
                if (currentIteration == amount - 1 && mc.player != null) {
                    mc.player.sendMessage(new TextComponentString(I18n.format("msg.quick_exchange.amount_done")));
                }
            }, i * (settings.clickIntervalMs / 50));
        }
    }

    @SubscribeEvent
    public void onChatMessage(ClientChatReceivedEvent event) {
        if (isExchangingAll) {
            ITextComponent message = event.getMessage();
            String text = message.getUnformattedText();
            if (text.contains(I18n.format("quick_exchange.chat.fail_exchange"))
                    || text.contains(I18n.format("quick_exchange.chat.missing_material"))) {
                stopExchangeAll();
            }
        }
    }
}
