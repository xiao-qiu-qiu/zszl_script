// 文件路径: src/main/java/com/zszl/zszlScriptMod/handlers/ShulkerBoxStackingHandler.java
package com.zszl.zszlScriptMod.handlers;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import com.zszl.zszlScriptMod.zszlScriptMod;
import com.zszl.zszlScriptMod.config.DebugModule;
import com.zszl.zszlScriptMod.config.ModConfig;
import com.zszl.zszlScriptMod.system.ProfileManager;
import com.zszl.zszlScriptMod.utils.ModUtils;
import com.zszl.zszlScriptMod.utils.PacketCaptureHandler;
import io.netty.buffer.Unpooled;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.text.TextComponentString;
import net.minecraftforge.fml.common.network.internal.FMLProxyPacket;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ShulkerBoxStackingHandler {

    private static final Minecraft mc = Minecraft.getMinecraft();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static boolean autoStackingEnabled = false;
    public static List<String> stackableItemKeywords = new ArrayList<>(
            Arrays.asList(I18n.format("shulker_stacking.default_keyword")));

    private static final String CLICK_PACKET_TEMPLATE = "00 04 56 69 65 77 00 00 {id} 00 00 00 01 00 0C 4F 77 6C 56 69 65 77 5F 73 6C 6F 74 00 00 00 12 4F 77 6C 56 69 65 77 5F 73 6C 6F 74 5F 63 6C 69 63 6B %s 00 00 00 00";
    private static final String PACKET_CHANNEL = "OwlViewChannel";

    private static final Map<Integer, Long> sourceSlotCooldowns = new ConcurrentHashMap<>();
    private static final long STACK_COOLDOWN_MS = 200;
    private static int debugMessageCooldown = 0;

    static {
        loadConfig();
    }

    private static Path getConfigFile() {
        return ProfileManager.getCurrentProfileDir().resolve("auto_stacking_config.json");
    }

    public static void loadConfig() {
        Path configFile = getConfigFile();
        if (Files.exists(configFile)) {
            try (BufferedReader reader = Files.newBufferedReader(configFile, StandardCharsets.UTF_8)) {
                JsonObject config = new JsonParser().parse(reader).getAsJsonObject();
                if (config.has("autoStackingEnabled")) {
                    autoStackingEnabled = config.get("autoStackingEnabled").getAsBoolean();
                }
                if (config.has("stackableItemKeywords")) {
                    Type listType = new TypeToken<ArrayList<String>>() {
                    }.getType();
                    stackableItemKeywords = GSON.fromJson(config.get("stackableItemKeywords"), listType);
                } else {
                    stackableItemKeywords = new ArrayList<>(
                            Arrays.asList(I18n.format("shulker_stacking.default_keyword")));
                }
            } catch (Exception e) {
                zszlScriptMod.LOGGER.error(I18n.format("log.shulker_stacking.load_failed"), e);
            }
        }
    }

    public static void saveConfig() {
        Path configFile = getConfigFile();
        try {
            Files.createDirectories(configFile.getParent());
            JsonObject config = new JsonObject();
            config.addProperty("autoStackingEnabled", autoStackingEnabled);
            config.add("stackableItemKeywords", GSON.toJsonTree(stackableItemKeywords));
            try (BufferedWriter writer = Files.newBufferedWriter(configFile, StandardCharsets.UTF_8)) {
                GSON.toJson(config, writer);
            }
        } catch (Exception e) {
            zszlScriptMod.LOGGER.error(I18n.format("log.shulker_stacking.save_failed"), e);
        }
    }

    public static class StackingLogEntry {
        public final int sourceSlot;
        public final int destinationSlot;
        public final String pickupHexPayload;
        public final String placeHexPayload;
        public final byte[] sessionID;

        public StackingLogEntry(int sourceSlot, int destinationSlot, String pickupHex, String placeHex,
                byte[] sessionID) {
            this.sourceSlot = sourceSlot;
            this.destinationSlot = destinationSlot;
            this.pickupHexPayload = pickupHex;
            this.placeHexPayload = placeHex;
            this.sessionID = sessionID;
        }

        @Override
        public String toString() {
            return String.format("Move From Slot %d -> To Slot %d", sourceSlot, destinationSlot);
        }
    }

    private static final List<StackingLogEntry> lastStackingLog = new ArrayList<>();

    public static void executeStacking() {
        if (debugMessageCooldown > 0) {
            debugMessageCooldown--;
        }
        stackShulkerBoxesInternal(true);
    }

    public static List<StackingLogEntry> generateStackingLog() {
        lastStackingLog.clear();
        stackShulkerBoxesInternal(false);
        return new ArrayList<>(lastStackingLog);
    }

    private static void stackShulkerBoxesInternal(boolean execute) {
        if (mc.currentScreen != null) {
            return;
        }

        EntityPlayerSP player = mc.player;
        if (player == null || player.openContainer == null) {
            return;
        }

        byte[] sessionID = PacketCaptureHandler.getOwlViewSessionID();
        if (sessionID == null) {
            return;
        }

        long currentTime = System.currentTimeMillis();
        sourceSlotCooldowns.entrySet().removeIf(entry -> (currentTime - entry.getValue()) > STACK_COOLDOWN_MS);

        Map<String, List<Integer>> itemGroups = new HashMap<>();

        for (Slot slot : player.openContainer.inventorySlots) {
            if (slot.inventory == player.inventory && slot.getHasStack()) {
                if (matchesKeywords(slot.getStack())) {
                    String key = getUnstackableItemUniqueKey(slot.getStack());
                    itemGroups.computeIfAbsent(key, k -> new ArrayList<>()).add(slot.slotNumber);
                }
            }
        }

        for (List<Integer> group : itemGroups.values()) {
            if (group.size() <= 1)
                continue;

            // --- 核心逻辑修复 ---
            int destinationSlot = -1;

            // 1. 优先选择组里第一个未满64的物品作为目标
            for (int slotIndex : group) {
                Slot slot = player.openContainer.getSlot(slotIndex);
                if (slot != null && slot.getHasStack() && slot.getStack().getCount() < 64) {
                    destinationSlot = slotIndex;
                    break; // 找到第一个就用
                }
            }

            // 2. 如果没找到，说明组里所有物品都已满64，跳过这个组
            if (destinationSlot == -1) {
                if (ModConfig.isDebugFlagEnabled(DebugModule.SHULKER_STACKING) && debugMessageCooldown <= 0) {
                    if (mc.player != null) {
                        String itemName = player.openContainer.getSlot(group.get(0)).getStack().getDisplayName();
                        mc.player.sendMessage(
                                new TextComponentString(I18n.format("msg.shulker_stacking.group_full_skip", itemName)));
                    }
                    debugMessageCooldown = 60;
                }
                continue; // 处理下一个物品组
            }

            // 3. 找到了一个未满的目标，现在遍历组内所有其他物品作为来源
            for (int sourceSlot : group) {
                // 不能是目标自己，也不能在冷却中
                if (sourceSlot == destinationSlot || sourceSlotCooldowns.containsKey(sourceSlot)) {
                    continue;
                }

                // 找到了一个有效的源和一个有效的目标，执行移动
                sendOrLogStackingMove(sourceSlot, destinationSlot, sessionID, execute);

                if (execute) {
                    sourceSlotCooldowns.put(sourceSlot, currentTime);
                    return; // 每次tick只执行一次叠加，防止操作过快
                }
            }
            // --- 修复结束 ---
        }
    }

    private static boolean matchesKeywords(ItemStack stack) {
        if (stack == null || stack.isEmpty())
            return false;
        String displayName = stack.getDisplayName();
        for (String keyword : stackableItemKeywords) {
            if (displayName.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private static void sendOrLogStackingMove(int sourceSlot, int destinationSlot, byte[] sessionID, boolean execute) {
        String sessionIdHex = PacketCaptureHandler.getSessionIdAsHex();
        if (sessionIdHex == null) {
            zszlScriptMod.LOGGER.error("[Shulker Stacking] Lost session ID while sending packet!");
            return;
        }

        String formattedSourceSlot = String.format("%02X %02X %02X %02X",
                (sourceSlot >> 24) & 0xFF, (sourceSlot >> 16) & 0xFF, (sourceSlot >> 8) & 0xFF, sourceSlot & 0xFF);
        String formattedDestSlot = String.format("%02X %02X %02X %02X",
                (destinationSlot >> 24) & 0xFF, (destinationSlot >> 16) & 0xFF, (destinationSlot >> 8) & 0xFF,
                destinationSlot & 0xFF);

        String pickupHexWithSlot = String.format(CLICK_PACKET_TEMPLATE, formattedSourceSlot);
        String placeHexWithSlot = String.format(CLICK_PACKET_TEMPLATE, formattedDestSlot);

        String finalPickupHex = pickupHexWithSlot.replace("{id}", sessionIdHex);
        String finalPlaceHex = placeHexWithSlot.replace("{id}", sessionIdHex);

        lastStackingLog
                .add(new StackingLogEntry(sourceSlot, destinationSlot, finalPickupHex, finalPlaceHex, sessionID));

        if (execute) {
            if (mc.getConnection() == null)
                return;
            try {
                String cleanHex = finalPickupHex.replaceAll("\\s", "");
                byte[] data = new byte[cleanHex.length() / 2];
                for (int j = 0; j < data.length; j++) {
                    data[j] = (byte) Integer.parseInt(cleanHex.substring(j * 2, j * 2 + 2), 16);
                }
                FMLProxyPacket packet = new FMLProxyPacket(new PacketBuffer(Unpooled.wrappedBuffer(data)),
                        PACKET_CHANNEL);
                mc.getConnection().sendPacket(packet);
            } catch (Exception e) {
                zszlScriptMod.LOGGER.error("[Shulker Stacking] Error building/sending PICKUP packet", e);
            }

            ModUtils.DelayScheduler.instance.schedule(() -> {
                if (mc.getConnection() == null)
                    return;
                try {
                    String cleanHex = finalPlaceHex.replaceAll("\\s", "");
                    byte[] data = new byte[cleanHex.length() / 2];
                    for (int j = 0; j < data.length; j++) {
                        data[j] = (byte) Integer.parseInt(cleanHex.substring(j * 2, j * 2 + 2), 16);
                    }
                    FMLProxyPacket packet = new FMLProxyPacket(new PacketBuffer(Unpooled.wrappedBuffer(data)),
                            PACKET_CHANNEL);
                    mc.getConnection().sendPacket(packet);
                } catch (Exception e) {
                    zszlScriptMod.LOGGER.error("[Shulker Stacking] Error building/sending PLACE packet", e);
                }
            }, 1);
        }
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder hex = new StringBuilder();
        for (byte b : bytes) {
            hex.append(String.format("%02X ", b));
        }
        return hex.toString().trim();
    }

    private static String getUnstackableItemUniqueKey(ItemStack stack) {
        String itemID = stack.getItem().getRegistryName().toString();
        NBTTagCompound nbt = stack.getSubCompound("BlockEntityTag");
        if (nbt == null) {
            nbt = stack.getSubCompound("display");
        }
        String nbtString = (nbt != null) ? nbt.toString() : "{}";
        return itemID + ":" + nbtString;
    }
}

