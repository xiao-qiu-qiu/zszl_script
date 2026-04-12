package com.zszl.zszlScriptMod.handlers;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.zszl.zszlScriptMod.zszlScriptMod;
import com.zszl.zszlScriptMod.config.DebugModule;
import com.zszl.zszlScriptMod.config.ModConfig;
import com.zszl.zszlScriptMod.system.ProfileManager;
import com.zszl.zszlScriptMod.utils.ModUtils;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.inventory.ClickType;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemFood;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.client.CPacketHeldItemChange;
import net.minecraft.util.EnumHand;
import net.minecraft.util.text.TextComponentString;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class AutoEatHandler {
    public static boolean autoEatEnabled = false;
    public static int foodLevelThreshold = 12;
    public static boolean autoMoveFoodEnabled = true;
    public static boolean eatWithLookDown = false;
    public static int targetHotbarSlot = 9;
    public static final List<String> DEFAULT_FOOD_KEYWORDS = Arrays.asList("牛排", "面包", "苹果", "曲奇饼");
    public static List<String> foodKeywords = new ArrayList<>(DEFAULT_FOOD_KEYWORDS);

    public static boolean isEating = false;
    public static int originalHotbarSlot = -1;
    public static ItemStack swappedItem = ItemStack.EMPTY;
    private static int eatTimeoutTicks = 0;
    private static int eatStartFoodLevel = 20;
    private static boolean eatStartedUsingHand = false;
    private static boolean pitchAdjusted = false;
    private static float originalPitch = 0.0F;

    static {
        loadAutoEatConfig();
    }

    private static File getConfigFile() {
        return ProfileManager.getCurrentProfileDir().resolve("keycommand_autoeat.json").toFile();
    }

    public static void loadAutoEatConfig() {
        try {
            File configFile = getConfigFile();
            if (configFile.exists()) {
                JsonObject json = new JsonParser().parse(new FileReader(configFile)).getAsJsonObject();
                autoEatEnabled = json.has("enabled") && json.get("enabled").getAsBoolean();
                foodLevelThreshold = json.has("foodLevelThreshold") ? json.get("foodLevelThreshold").getAsInt() : 12;
                autoMoveFoodEnabled = !json.has("autoMoveFoodEnabled")
                        || json.get("autoMoveFoodEnabled").getAsBoolean();
                eatWithLookDown = json.has("eatWithLookDown") && json.get("eatWithLookDown").getAsBoolean();
                targetHotbarSlot = json.has("targetHotbarSlot") ? json.get("targetHotbarSlot").getAsInt() : 9;

                foodKeywords = new ArrayList<>();
                if (json.has("foodKeywords") && json.get("foodKeywords").isJsonArray()) {
                    json.getAsJsonArray("foodKeywords").forEach(e -> {
                        if (e != null && e.isJsonPrimitive()) {
                            String keyword = e.getAsString().trim();
                            if (!keyword.isEmpty()) {
                                foodKeywords.add(keyword);
                            }
                        }
                    });
                }

                normalizeConfigValues();
            }
        } catch (Exception e) {
            zszlScriptMod.LOGGER.error("加载自动进食配置失败", e);
            normalizeConfigValues();
        }
    }

    public static void saveAutoEatConfig() {
        try {
            File configFile = getConfigFile();
            if (!configFile.getParentFile().exists()) {
                configFile.getParentFile().mkdirs();
            }
            try (FileWriter writer = new FileWriter(configFile)) {
                JsonObject json = new JsonObject();
                json.addProperty("enabled", autoEatEnabled);
                json.addProperty("foodLevelThreshold", foodLevelThreshold);
                json.addProperty("autoMoveFoodEnabled", autoMoveFoodEnabled);
                json.addProperty("eatWithLookDown", eatWithLookDown);
                json.addProperty("targetHotbarSlot", targetHotbarSlot);

                com.google.gson.JsonArray keywordArray = new com.google.gson.JsonArray();
                for (String keyword : foodKeywords) {
                    if (keyword != null && !keyword.trim().isEmpty()) {
                        keywordArray.add(keyword.trim());
                    }
                }
                json.add("foodKeywords", keywordArray);
                writer.write(json.toString());
            }
        } catch (Exception e) {
            zszlScriptMod.LOGGER.error("保存自动进食配置失败", e);
        }
    }

    /**
     * 自动进食检查方法
     * 
     * @param player 玩家实体
     */
    public static void checkAutoEat(EntityPlayerSP player) {
        if (player == null || !autoEatEnabled)
            return;

        if (isEating) {
            continueEatingProcess(player);
            return;
        }

        if (ModConfig.isGuiOpen() || player.getFoodStats().getFoodLevel() > foodLevelThreshold)
            return;

        int hotbarFoodSlot = findBestFoodInHotbar(player);
        if (hotbarFoodSlot != -1) {
            AutoEatHandler.originalHotbarSlot = player.inventory.currentItem;
            handleEatingProcess(player, hotbarFoodSlot);
            return;
        }

        if (!autoMoveFoodEnabled) {
            return;
        }

        Container container = player.openContainer;
        if (container == null)
            return;

        int sourceContainerSlot = findBestFoodInMainInventory(container, player);
        int targetContainerSlot = getContainerSlotByPlayerInventoryIndex(container, player, targetHotbarSlot - 1);
        if (sourceContainerSlot != -1 && targetContainerSlot != -1 && sourceContainerSlot != targetContainerSlot) {
            moveOrSwapItemByClick(sourceContainerSlot, targetContainerSlot);
        }
    }

    private static void normalizeConfigValues() {
        foodLevelThreshold = Math.max(0, Math.min(20, foodLevelThreshold));
        targetHotbarSlot = Math.max(1, Math.min(9, targetHotbarSlot));

        if (foodKeywords == null) {
            foodKeywords = new ArrayList<>();
        }
        List<String> normalized = new ArrayList<>();
        for (String keyword : foodKeywords) {
            if (keyword != null) {
                String trimmed = keyword.trim();
                if (!trimmed.isEmpty()) {
                    normalized.add(trimmed);
                }
            }
        }
        if (normalized.isEmpty()) {
            normalized.addAll(DEFAULT_FOOD_KEYWORDS);
        }
        foodKeywords = normalized;
    }

    private static int findBestFoodInHotbar(EntityPlayerSP player) {
        int bestSlot = -1;
        int bestPriority = Integer.MAX_VALUE;
        int bestHeal = -1;

        for (int i = 0; i < 9; i++) {
            ItemStack stack = player.inventory.getStackInSlot(i);
            if (!isPreferredFoodItem(stack)) {
                continue;
            }

            int priority = getFoodPriority(stack);
            int heal = ((ItemFood) stack.getItem()).getHealAmount(stack);
            if (priority < bestPriority || (priority == bestPriority && heal > bestHeal)) {
                bestPriority = priority;
                bestHeal = heal;
                bestSlot = i;
            }
        }

        return bestSlot;
    }

    private static int findBestFoodInMainInventory(Container container, EntityPlayerSP player) {
        int bestContainerSlot = -1;
        int bestPriority = Integer.MAX_VALUE;
        int bestHeal = -1;

        for (int i = 0; i < container.inventorySlots.size(); i++) {
            Slot slot = container.getSlot(i);
            if (slot == null || slot.inventory != player.inventory || !slot.getHasStack()) {
                continue;
            }

            int playerInvIndex = slot.getSlotIndex();
            if (playerInvIndex < 9 || playerInvIndex > 35) {
                continue;
            }

            ItemStack stack = slot.getStack();
            if (!isPreferredFoodItem(stack)) {
                continue;
            }

            int priority = getFoodPriority(stack);
            int heal = ((ItemFood) stack.getItem()).getHealAmount(stack);
            if (priority < bestPriority || (priority == bestPriority && heal > bestHeal)) {
                bestPriority = priority;
                bestHeal = heal;
                bestContainerSlot = i;
            }
        }

        return bestContainerSlot;
    }

    private static int getContainerSlotByPlayerInventoryIndex(Container container, EntityPlayerSP player, int index) {
        for (int i = 0; i < container.inventorySlots.size(); i++) {
            Slot slot = container.getSlot(i);
            if (slot != null && slot.inventory == player.inventory && slot.getSlotIndex() == index) {
                return i;
            }
        }
        return -1;
    }

    private static void moveOrSwapItemByClick(int sourceContainerSlot, int destinationContainerSlot) {
        clickContainerSlot(sourceContainerSlot);
        ModUtils.DelayScheduler.instance.schedule(() -> clickContainerSlot(destinationContainerSlot), 2);
        ModUtils.DelayScheduler.instance.schedule(() -> clickContainerSlot(sourceContainerSlot), 4);
    }

    private static void clickContainerSlot(int slot) {
        EntityPlayerSP player = Minecraft.getMinecraft().player;
        if (player == null || player.openContainer == null || Minecraft.getMinecraft().playerController == null) {
            return;
        }
        if (slot < 0 || slot >= player.openContainer.inventorySlots.size()) {
            return;
        }
        Minecraft.getMinecraft().playerController.windowClick(player.openContainer.windowId, slot, 0, ClickType.PICKUP,
                player);
    }

    private static void swapItemWithHotbar2(EntityPlayerSP player, int sourcePlayerInvSlot) {
        if (player == null || sourcePlayerInvSlot < 0 || sourcePlayerInvSlot > 35
                || Minecraft.getMinecraft().playerController == null || player.openContainer == null) {
            return;
        }

        int sourceContainerSlot = getContainerSlotByPlayerInventoryIndex(player.openContainer, player,
                sourcePlayerInvSlot);
        if (sourceContainerSlot == -1) {
            return;
        }

        int currentHotbar = player.inventory.currentItem;
        Minecraft.getMinecraft().playerController.windowClick(
                player.openContainer.windowId,
                sourceContainerSlot,
                currentHotbar,
                ClickType.SWAP,
                player);
    }

    private static int findInventorySlotByItem(ItemStack target) {
        EntityPlayerSP player = Minecraft.getMinecraft().player;
        if (player == null || target == null || target.isEmpty()) {
            return -1;
        }

        for (int i = 9; i < 36; i++) {
            if (ItemStack.areItemStacksEqual(player.inventory.getStackInSlot(i), target)) {
                return i;
            }
        }
        return -1;
    }

    // 进食处理方法
    private static void handleEatingProcess(EntityPlayerSP player, int slot) {
        if (ModConfig.isDebugFlagEnabled(DebugModule.AUTO_EAT) && Minecraft.getMinecraft().player != null) {
            ItemStack foodStack = player.inventory.getStackInSlot(slot);
            Minecraft.getMinecraft().player.sendMessage(new TextComponentString(
                    String.format("§d[调试] §7开始自动进食 [%s]。", foodStack.getDisplayName())));
        }
        AutoEatHandler.isEating = true;
        AutoEatHandler.eatTimeoutTicks = 70;
        AutoEatHandler.eatStartFoodLevel = player.getFoodStats().getFoodLevel();
        AutoEatHandler.eatStartedUsingHand = false;

        EmbeddedNavigationHandler.INSTANCE.pause();

        pitchAdjusted = false;
        if (eatWithLookDown) {
            originalPitch = player.rotationPitch;
            player.rotationPitch = 80.0F;
            pitchAdjusted = true;
        }

        player.inventory.currentItem = slot;
        player.connection.sendPacket(new CPacketHeldItemChange(slot));

        if (Minecraft.getMinecraft().playerController != null) {
            Minecraft.getMinecraft().playerController.processRightClick(player, player.world, EnumHand.MAIN_HAND);
        }
        setUseKeyState(true);

        if (eatWithLookDown) {
            player.rotationPitch = 80.0F;
        }
    }

    private static void continueEatingProcess(EntityPlayerSP player) {
        if (player == null) {
            resetEatingState();
            EmbeddedNavigationHandler.INSTANCE.resume();
            return;
        }

        if (ModConfig.isGuiOpen()) {
            finalizeEating(player, true);
            return;
        }

        if (originalHotbarSlot >= 0 && player.inventory.currentItem != originalHotbarSlot) {
            // 保持当前食物槽，不被其它逻辑抢槽
        }

        setUseKeyState(true);

        if (Minecraft.getMinecraft().playerController != null && !player.isHandActive()) {
            Minecraft.getMinecraft().playerController.processRightClick(player, player.world, EnumHand.MAIN_HAND);
        }

        if (player.isHandActive()) {
            eatStartedUsingHand = true;
        }

        boolean hungerRecovered = player.getFoodStats().getFoodLevel() > eatStartFoodLevel;
        boolean finishedUse = eatStartedUsingHand && !player.isHandActive();
        eatTimeoutTicks--;
        boolean timeout = eatTimeoutTicks <= 0;

        if (hungerRecovered || finishedUse || timeout) {
            finalizeEating(player, false);
        }
    }

    private static void finalizeEating(EntityPlayerSP player, boolean interrupted) {
        if (player != null && player.isHandActive()) {
            player.stopActiveHand();
        }

        if (player != null && pitchAdjusted) {
            player.rotationPitch = originalPitch;
        }

        setUseKeyState(false);

        if (player != null && originalHotbarSlot != -1) {
            player.inventory.currentItem = originalHotbarSlot;
            player.connection.sendPacket(new CPacketHeldItemChange(originalHotbarSlot));
        }

        EmbeddedNavigationHandler.INSTANCE.resume();
        resetEatingState();

        if (interrupted && ModConfig.isDebugFlagEnabled(DebugModule.AUTO_EAT) && player != null) {
            player.sendMessage(new TextComponentString("§d[调试] §7自动进食被中断。"));
        }
    }

    private static void setUseKeyState(boolean pressed) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null || mc.gameSettings == null || mc.gameSettings.keyBindUseItem == null) {
            return;
        }
        KeyBinding.setKeyBindState(mc.gameSettings.keyBindUseItem.getKeyCode(), pressed);
    }

    // 辅助重置方法
    private static void resetEatingState() {
        setUseKeyState(false);
        AutoEatHandler.isEating = false;
        AutoEatHandler.originalHotbarSlot = -1;
        AutoEatHandler.swappedItem = ItemStack.EMPTY;
        AutoEatHandler.eatTimeoutTicks = 0;
        AutoEatHandler.eatStartFoodLevel = 20;
        AutoEatHandler.eatStartedUsingHand = false;
        AutoEatHandler.pitchAdjusted = false;
    }

    private static boolean isPreferredFoodItem(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() instanceof ItemFood && isMatchingFoodKeyword(stack);
    }

    private static boolean isMatchingFoodKeyword(ItemStack stack) {
        if (foodKeywords == null || foodKeywords.isEmpty()) {
            return true;
        }
        String name = stack.getDisplayName().toLowerCase(Locale.ROOT);
        for (String keyword : foodKeywords) {
            if (keyword == null)
                continue;
            String trimmed = keyword.trim();
            if (!trimmed.isEmpty() && name.contains(trimmed.toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }

    private static int getFoodPriority(ItemStack stack) {
        if (foodKeywords == null || foodKeywords.isEmpty()) {
            return 0;
        }
        String name = stack.getDisplayName().toLowerCase(Locale.ROOT);
        for (int i = 0; i < foodKeywords.size(); i++) {
            String keyword = foodKeywords.get(i);
            if (keyword == null)
                continue;
            String trimmed = keyword.trim();
            if (!trimmed.isEmpty() && name.contains(trimmed.toLowerCase(Locale.ROOT))) {
                return i;
            }
        }
        return Integer.MAX_VALUE;
    }
}

