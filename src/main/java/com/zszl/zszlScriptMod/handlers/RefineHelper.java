package com.zszl.zszlScriptMod.handlers;

import com.zszl.zszlScriptMod.config.DebugModule;
import com.zszl.zszlScriptMod.config.ModConfig;
import com.zszl.zszlScriptMod.utils.CapturedIdRuleManager;
import com.zszl.zszlScriptMod.utils.ModUtils;
import com.zszl.zszlScriptMod.utils.PacketCaptureHandler;
import com.zszl.zszlScriptMod.zszlScriptMod;
import io.netty.buffer.Unpooled;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketBuffer;
import net.minecraft.network.play.client.CPacketCustomPayload;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.text.TextComponentString;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RefineHelper {

    private static final String REFINE_SELECT_PAYLOAD_TEMPLATE = "00 09 43 6F 6D 70 6F 6E 65 6E 74 00 00 {session_id} {component_id} 00 0C 42 75 74 74 6F 6E 5F 63 6C 69 63 6B 01 00 00 00 00 00 00 00 00 01 00 00 00 00 00 00 00 00";
    private static final String REFINE_START_RIGHT_CLICK_PAYLOAD_TEMPLATE = "00 09 43 6F 6D 70 6F 6E 65 6E 74 00 00 {session_id} {component_id} 00 0C 42 75 74 74 6F 6E 5F 63 6C 69 63 6B 01 00 00 00 00 01 00 00 00 00 00 00 00 00";
    private static final String REFINE_MAIN_GUI_SLOT_CLICK_TEMPLATE = "00 04 56 69 65 77 00 00 {session_id} {component_id} 00 0C 4F 77 6C 56 69 65 77 5F 73 6C 6F 74 00 00 00 12 4F 77 6C 56 69 65 77 5F 73 6C 6F 74 5F 63 6C 69 63 6B 00 00 00 2F 00 00 00 00";
    private static final String VIEW_SLOT_CLICK_TEMPLATE = "00 04 56 69 65 77 00 00 {session_id} {component_id} 00 0C 4F 77 6C 56 69 65 77 5F 73 6C 6F 74 00 00 00 12 4F 77 6C 56 69 65 77 5F 73 6C 6F 74 5F 63 6C 69 63 6B 00 00 00 {slot_index_hex} 00 00 00 00";
    private static final String REFINE_CLICK_TASK_TAG = "REFINE_CLICK_TASK";
    private static final String REFINE_AUTO_TASK_TAG = "REFINE_AUTO_TASK";
    private static final String REFRESH_GUI_TASK_TAG = "REFRESH_GUI_TASK";

    public static final RefineHelper INSTANCE = new RefineHelper();

    public static final int BTN_ONE_KEY_REFINE = 9101;
    public static final int BTN_VIEW_REFINE_IDS = 9102;
    public static final int BTN_ONE_KEY_RESOLVE = 9103;
    public static final int BTN_REFRESH_REFINE_GUI = 9104;
    public static final int BTN_ONE_KEY_REFINE_RESOLVE = 9105;
    public static final int BTN_STOP_AUTO_REFINE = 9106;

    public volatile boolean isRefineTicketValid = false;
    public volatile boolean isRefineContextActive = false;
    public volatile boolean isRefineMainGuiOpen = false;
    private volatile int refineMainGuiId = -1;
    private final ConcurrentSkipListMap<Integer, Integer> refineSlotButtonIds = new ConcurrentSkipListMap<>();
    private final LinkedHashSet<Integer> resolveCandidateSlots = new LinkedHashSet<>();
    private static final Pattern REFINE_SLOT_PATTERN = Pattern.compile("blueprintButtonBarIndex(\\d+)");
    private volatile int refineSlotUpdateVersion = 0;

    private volatile boolean autoRefineResolveRunning = false;
    private volatile boolean autoRefineResolveStopRequested = false;
    private volatile int autoRefineResolveRemainingTimes = 0;
    private volatile boolean autoRefineRefreshInProgress = false;

    private void notifyRefine(String message) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.player == null) {
            return;
        }
        if (!ModConfig.isDebugFlagEnabled(DebugModule.REFINE)) {
            return;
        }
        mc.player.sendMessage(new TextComponentString(message));
    }

    private RefineHelper() {
    }

    public void deactivateRefineContext(String reason) {
        isRefineTicketValid = false;
        isRefineContextActive = false;
        isRefineMainGuiOpen = false;
        refineMainGuiId = -1;
        refineSlotButtonIds.clear();
        resolveCandidateSlots.clear();
        autoRefineResolveRunning = false;
        autoRefineResolveStopRequested = false;
        autoRefineResolveRemainingTimes = 0;
        autoRefineRefreshInProgress = false;
        cancelRefineTasks();
        ModConfig.debugPrint(DebugModule.MAIL_GUI, "精炼上下文已重置: reason=" + reason);
    }

    public void onPossibleSetSlotPacket(Object packet) {
        try {
            if (packet == null) {
                return;
            }
            Integer slotIndex = extractSetSlotIndex(packet);
            ItemStack stack = extractSetSlotItem(packet);
            if (slotIndex == null || stack == null || stack.isEmpty()) {
                return;
            }
            if (slotIndex < 0 || slotIndex > 35) {
                return;
            }
            if (!hasResolveTag(stack)) {
                return;
            }
            synchronized (resolveCandidateSlots) {
                resolveCandidateSlots.add(slotIndex);
            }
            ModConfig.debugPrint(DebugModule.MAIL_GUI,
                    "记录可分解槽位: slot=" + slotIndex + ", item=" + stack.getDisplayName());
        } catch (Exception ignored) {
        }
    }

    public void onRefineBlueprintButtonCreated(int componentId, int parentId, String name) {
        if (name == null) {
            return;
        }
        Matcher m = REFINE_SLOT_PATTERN.matcher(name);
        if (!m.find()) {
            return;
        }
        int slotIndex;
        try {
            slotIndex = Integer.parseInt(m.group(1));
        } catch (Exception e) {
            return;
        }

        // 新一轮精炼槽位刷新从 10 开始：出现新的10时，清空旧槽位并重建
        if (slotIndex == 10) {
            refineSlotButtonIds.clear();
        }

        // 旧逻辑要求必须先捕获到10，再收11/12/...，
        // 但实测某些包会先出现11，导致“查看精炼ID”一直暂无捕获。
        // 新逻辑：仅当已有旧轮数据且未见10时，才忽略 >10；首个槽位允许从11开始。
        if (slotIndex > 10 && !refineSlotButtonIds.containsKey(10) && !refineSlotButtonIds.isEmpty()) {
            return;
        }

        refineSlotButtonIds.put(slotIndex, componentId);
        refineSlotUpdateVersion++;
        ModConfig.debugPrint(DebugModule.MAIL_GUI,
                "捕获精炼槽位按钮: index=" + slotIndex + ", componentId=" + componentId + ", parentId=" + parentId);
    }

    public int getAutoRefineResolveRemainingTimes() {
        return Math.max(0, autoRefineResolveRemainingTimes);
    }

    public boolean isAutoRefineResolveRunning() {
        return autoRefineResolveRunning;
    }

    public boolean isAutoRefineRefreshInProgress() {
        return autoRefineRefreshInProgress;
    }

    public List<String> getRefineSlotIdLines() {
        List<String> lines = new ArrayList<>();
        for (Map.Entry<Integer, Integer> e : refineSlotButtonIds.entrySet()) {
            int idx = e.getKey();
            int id = e.getValue();
            String hex = String.format("%02X %02X %02X %02X",
                    (id >> 24) & 0xFF,
                    (id >> 16) & 0xFF,
                    (id >> 8) & 0xFF,
                    id & 0xFF);
            lines.add("blueprintButtonBarIndex" + idx + " = " + id + " (" + hex + ")");
        }
        return lines;
    }

    public boolean onRefineViewCreated(int componentId, String viewName) {
        if (viewName == null || !viewName.startsWith("Compound:")) {
            return false;
        }
        Integer captured = getCapturedInt("refine_main_gui_id");
        if (captured == null || captured <= 0 || componentId != captured) {
            return false;
        }
        refineMainGuiId = captured;
        return onRefineTicketReceived();
    }

    public boolean onRefineViewOpenDetected(int componentId) {
        Integer captured = getCapturedInt("refine_main_gui_id");
        if (captured == null || captured <= 0 || componentId != captured) {
            return false;
        }
        refineMainGuiId = captured;
        isRefineMainGuiOpen = true;
        autoRefineRefreshInProgress = false;
        return onRefineTicketReceived();
    }

    public boolean onRefineViewOpenStateDetected(int componentId, boolean openFlag, boolean extraFlag) {
        Integer captured = getCapturedInt("refine_main_gui_id");
        if (captured == null || captured <= 0 || componentId != captured) {
            return false;
        }

        if (openFlag && !extraFlag) {
            return onRefineViewOpenDetected(componentId);
        }

        if (!openFlag) {
            if (autoRefineResolveRunning && autoRefineRefreshInProgress) {
                // 自动精炼分解的“刷新界面”会短暂关闭 ViewGui，此时不能把整套状态清空。
                isRefineMainGuiOpen = false;
                ModConfig.debugPrint(DebugModule.MAIL_GUI, "检测到刷新过程中的 ViewGui_set_open:false，保留自动流程状态。");
                return true;
            }
            deactivateRefineContext("ViewGui_set_open:false");
            return true;
        }

        return false;
    }

    private boolean onRefineTicketReceived() {
        if (!isRefineTicketValid) {
            isRefineTicketValid = true;
            ModConfig.debugPrint(DebugModule.MAIL_GUI, "精炼GUI指纹已捕获！门票(isRefineTicketValid)已设置为 true。");
            return true;
        }
        return false;
    }

    public void startOneKeyRefine() {
        startOneKeyRefineInternal(true);
    }

    private int startOneKeyRefineInternal(boolean notify) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.player == null || mc.getConnection() == null) {
            return 0;
        }

        if (refineSlotButtonIds.isEmpty()) {
            if (notify) {
                notifyRefine("§e[精炼] 未捕获到可点击槽位ID（需要先出现 blueprintButtonBarIndex10）");
            }
            return 0;
        }

        String sessionIdHex = PacketCaptureHandler.getSessionIdAsHex();
        if (sessionIdHex == null || sessionIdHex.trim().isEmpty()) {
            if (notify) {
                notifyRefine("§c[精炼] 未捕获到会话ID，无法发送点击包");
            }
            return 0;
        }

        Integer startRefineButtonId = getCapturedInt("refine_start_button_id");
        if (startRefineButtonId == null || startRefineButtonId <= 0) {
            if (notify) {
                notifyRefine("§e[精炼] 未捕获到开始精炼ID(refine_start_button_id)");
            }
            return 0;
        }

        Integer mainGuiId = getCapturedInt("refine_main_gui_id");
        if (mainGuiId == null || mainGuiId <= 0) {
            if (notify) {
                notifyRefine("§e[精炼] 未捕获到精炼主界面ID(refine_main_gui_id)");
            }
            return 0;
        }

        ModUtils.DelayScheduler.instance.cancelTasks(task -> REFINE_CLICK_TASK_TAG.equals(task.getTag()));

        List<Integer> slotIds = new ArrayList<>(refineSlotButtonIds.values());
        int planCount = 0;
        for (int i = 0; i < slotIds.size(); i++) {
            final int slotComponentId = slotIds.get(i);
            final int delayBase = i * 3;

            ModUtils.DelayScheduler.instance.schedule(() -> sendRefineClickPacket(slotComponentId, sessionIdHex, false),
                    delayBase, REFINE_CLICK_TASK_TAG);

            ModUtils.DelayScheduler.instance.schedule(
                    () -> sendRefineClickPacket(startRefineButtonId, sessionIdHex, true), delayBase + 1,
                    REFINE_CLICK_TASK_TAG);

            ModUtils.DelayScheduler.instance.schedule(() -> sendRefineMainGuiSlotClick(mainGuiId, sessionIdHex),
                    delayBase + 2, REFINE_CLICK_TASK_TAG);

            ModUtils.DelayScheduler.instance.schedule(() -> removeRefineSlotByComponentId(slotComponentId),
                    delayBase + 2, REFINE_CLICK_TASK_TAG);

            planCount++;
        }

        if (notify) {
            notifyRefine("§a[精炼] 已计划 " + planCount + " 个槽位精炼（每步延迟 1 tick）");
        }
        ModConfig.debugPrint(DebugModule.MAIL_GUI,
                "触发一键精炼计划。mainGuiId=" + refineMainGuiId + ", slots=" + planCount + ", startButtonId="
                        + startRefineButtonId);
        return planCount;
    }

    public void startOneKeyResolve() {
        startOneKeyResolveInternal(true);
    }

    private int startOneKeyResolveInternal(boolean notify) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.player == null || mc.getConnection() == null) {
            return 0;
        }

        String sessionIdHex = PacketCaptureHandler.getSessionIdAsHex();
        if (sessionIdHex == null || sessionIdHex.trim().isEmpty()) {
            if (notify) {
                notifyRefine("§c[分解] 未捕获到会话ID，无法发送点击包");
            }
            return 0;
        }

        Integer resolveMainGuiId = getCapturedInt("resolve_main_gui_id");
        if (resolveMainGuiId == null || resolveMainGuiId <= 0) {
            if (notify) {
                notifyRefine("§e[分解] 未捕获到分解主界面ID(resolve_main_gui_id)");
            }
            return 0;
        }

        Integer resolveStartButtonId = getCapturedInt("resolve_start_button_id");
        if (resolveStartButtonId == null || resolveStartButtonId <= 0) {
            if (notify) {
                notifyRefine("§e[分解] 未捕获到开始分解ID(resolve_start_button_id)");
            }
            return 0;
        }

        List<Integer> slots;
        synchronized (resolveCandidateSlots) {
            slots = new ArrayList<>(resolveCandidateSlots);
        }
        if (slots.isEmpty()) {
            slots = collectResolveSlotsFromInventory(mc);
            if (!slots.isEmpty()) {
                synchronized (resolveCandidateSlots) {
                    resolveCandidateSlots.addAll(slots);
                }
            }
        }
        if (slots.isEmpty()) {
            if (notify) {
                notifyRefine("§e[分解] 当前没有记录到可分解物品槽位（等待精炼后SPacketSetSlot）");
            }
            return 0;
        }

        ModUtils.DelayScheduler.instance.cancelTasks(task -> REFINE_CLICK_TASK_TAG.equals(task.getTag()));

        int planCount = 0;
        for (int i = 0; i < slots.size(); i++) {
            final int slotIndex = slots.get(i);
            final int delayBase = i * 3;

            ModUtils.DelayScheduler.instance.schedule(
                    () -> sendViewSlotClick(resolveMainGuiId, sessionIdHex, slotIndex), delayBase,
                    REFINE_CLICK_TASK_TAG);

            ModUtils.DelayScheduler.instance.schedule(
                    () -> sendRefineClickPacket(resolveStartButtonId, sessionIdHex, false), delayBase + 1,
                    REFINE_CLICK_TASK_TAG);

            ModUtils.DelayScheduler.instance.schedule(() -> sendViewSlotClick(resolveMainGuiId, sessionIdHex, 47),
                    delayBase + 2, REFINE_CLICK_TASK_TAG);

            planCount++;
        }

        if (notify) {
            notifyRefine("§a[分解] 已计划 " + planCount + " 个物品分解（每步延迟 1 tick）");
        }
        return planCount;
    }

    public void refreshRefineGui(GuiScreen currentGui) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.player == null || mc.getConnection() == null) {
            return;
        }

        String sessionIdHex = PacketCaptureHandler.getSessionIdAsHex();
        Integer openRefineGuiId = getCapturedInt("open_refine_gui_id");
        if (sessionIdHex == null || sessionIdHex.trim().isEmpty() || openRefineGuiId == null || openRefineGuiId <= 0) {
            notifyRefine("§e[精炼] 刷新失败：缺少会话ID或open_refine_gui_id");
            return;
        }

        autoRefineRefreshInProgress = autoRefineResolveRunning;
        mc.displayGuiScreen(null);
        ModUtils.DelayScheduler.instance.schedule(() -> sendRefineClickPacket(openRefineGuiId, sessionIdHex, false),
                2, REFRESH_GUI_TASK_TAG);
        ModUtils.DelayScheduler.instance.schedule(() -> {
            // 防止异常情况下标志一直悬挂
            autoRefineRefreshInProgress = false;
        }, 80, REFRESH_GUI_TASK_TAG);
        notifyRefine("§b[精炼] 已请求刷新精炼界面。");
    }

    public void stopAutoRefineResolve() {
        autoRefineResolveStopRequested = true;
        autoRefineResolveRunning = false;
        autoRefineResolveRemainingTimes = 0;
        autoRefineRefreshInProgress = false;
        cancelRefineTasks();
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.player != null) {
            notifyRefine("§c[精炼] 已停止自动精炼分解。");
        }
    }

    public void startAutoRefineResolve(int totalTimes, GuiScreen currentGui) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.player == null || mc.getConnection() == null) {
            return;
        }
        if (totalTimes <= 0) {
            notifyRefine("§e[精炼] 次数需大于0。");
            return;
        }
        if (autoRefineResolveRunning) {
            notifyRefine("§e[精炼] 自动精炼分解已在运行。");
            return;
        }

        autoRefineResolveRunning = true;
        autoRefineResolveStopRequested = false;
        autoRefineResolveRemainingTimes = totalTimes;
        notifyRefine("§a[精炼] 开始自动精炼分解，总次数=" + totalTimes);
        runAutoRefineResolveCycle(currentGui);
    }

    private void runAutoRefineResolveCycle(GuiScreen currentGui) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.player == null) {
            stopAutoRefineResolve();
            return;
        }
        if (autoRefineResolveStopRequested || !autoRefineResolveRunning) {
            stopAutoRefineResolve();
            return;
        }
        if (autoRefineResolveRemainingTimes <= 0) {
            autoRefineResolveRunning = false;
            autoRefineRefreshInProgress = false;
            notifyRefine("§a[精炼] 自动精炼分解已完成。");
            return;
        }

        int beforeVersion = refineSlotUpdateVersion;
        int refineCount = startOneKeyRefineInternal(false);
        if (refineCount <= 0) {
            stopAutoRefineResolve();
            return;
        }

        int refineDelay = refineCount * 3 + 4;
        ModUtils.DelayScheduler.instance.schedule(() -> {
            if (autoRefineResolveStopRequested || !autoRefineResolveRunning) {
                stopAutoRefineResolve();
                return;
            }

            int resolveCount = startOneKeyResolveInternal(false);
            if (resolveCount <= 0) {
                stopAutoRefineResolve();
                return;
            }

            int resolveDelay = resolveCount * 3 + 4;
            ModUtils.DelayScheduler.instance.schedule(() -> {
                if (autoRefineResolveStopRequested || !autoRefineResolveRunning) {
                    stopAutoRefineResolve();
                    return;
                }

                // 刷新前先清空当前槽位缓存，避免“ID未重新Create但仍可复用”场景下被旧数据阻塞
                refineSlotButtonIds.clear();
                refreshRefineGui(currentGui);
                waitRefineSlotsUpdatedAndContinue(beforeVersion, 200);
            }, resolveDelay, REFINE_AUTO_TASK_TAG);
        }, refineDelay, REFINE_AUTO_TASK_TAG);
    }

    private void waitRefineSlotsUpdatedAndContinue(int beforeVersion, int timeoutTicks) {
        ModUtils.DelayScheduler.instance.schedule(() -> {
            Minecraft mc = Minecraft.getMinecraft();
            if (mc.player == null) {
                stopAutoRefineResolve();
                return;
            }
            if (autoRefineResolveStopRequested || !autoRefineResolveRunning) {
                stopAutoRefineResolve();
                return;
            }

            // 兼容服务端复用组件ID的场景：只要刷新后重新出现可用槽位即可继续下一轮
            boolean updated = !refineSlotButtonIds.isEmpty();
            if (updated) {
                autoRefineResolveRemainingTimes = Math.max(0, autoRefineResolveRemainingTimes - 1);
                notifyRefine("§b[精炼] 本轮完成，剩余 " + autoRefineResolveRemainingTimes + " 次。");
                runAutoRefineResolveCycle(null);
                return;
            }

            if (timeoutTicks <= 0) {
                stopAutoRefineResolve();
                notifyRefine("§e[精炼] 等待精炼ID更新超时，已停止自动流程。");
                return;
            }

            waitRefineSlotsUpdatedAndContinue(beforeVersion, timeoutTicks - 5);
        }, 5, REFINE_AUTO_TASK_TAG);
    }

    private void cancelRefineTasks() {
        ModUtils.DelayScheduler.instance.cancelTasks(task -> {
            String tag = task.getTag();
            return REFINE_CLICK_TASK_TAG.equals(tag) || REFINE_AUTO_TASK_TAG.equals(tag)
                    || REFRESH_GUI_TASK_TAG.equals(tag);
        });
    }

    private void removeRefineSlotByComponentId(int componentId) {
        Integer targetKey = null;
        for (Map.Entry<Integer, Integer> e : refineSlotButtonIds.entrySet()) {
            if (e.getValue() != null && e.getValue() == componentId) {
                targetKey = e.getKey();
                break;
            }
        }
        if (targetKey != null) {
            refineSlotButtonIds.remove(targetKey);
        }
    }

    private boolean sendRefineMainGuiSlotClick(int mainGuiId, String sessionIdHex) {
        try {
            Minecraft mc = Minecraft.getMinecraft();
            if (mc.getConnection() == null) {
                return false;
            }

            String componentHex = String.format("%02X %02X %02X %02X",
                    (mainGuiId >> 24) & 0xFF,
                    (mainGuiId >> 16) & 0xFF,
                    (mainGuiId >> 8) & 0xFF,
                    mainGuiId & 0xFF);

            String finalHexPayload = REFINE_MAIN_GUI_SLOT_CLICK_TEMPLATE
                    .replace("{session_id}", sessionIdHex)
                    .replace("{component_id}", componentHex);

            String cleanHex = finalHexPayload.replaceAll("\\s", "");
            byte[] data = new byte[cleanHex.length() / 2];
            for (int i = 0; i < data.length; i++) {
                data[i] = (byte) Integer.parseInt(cleanHex.substring(i * 2, i * 2 + 2), 16);
            }

            PacketBuffer payload = new PacketBuffer(Unpooled.wrappedBuffer(data));
            CPacketCustomPayload packet = new CPacketCustomPayload("OwlViewChannel", payload);
            mc.getConnection().sendPacket(packet);
            return true;
        } catch (Exception e) {
            zszlScriptMod.LOGGER.error("[RefineHelper] 发送精炼主界面slot_click失败, mainGuiId=" + mainGuiId, e);
            return false;
        }
    }

    private boolean sendViewSlotClick(int mainGuiId, String sessionIdHex, int slotIndex) {
        try {
            Minecraft mc = Minecraft.getMinecraft();
            if (mc.getConnection() == null) {
                return false;
            }

            String componentHex = String.format("%02X %02X %02X %02X",
                    (mainGuiId >> 24) & 0xFF,
                    (mainGuiId >> 16) & 0xFF,
                    (mainGuiId >> 8) & 0xFF,
                    mainGuiId & 0xFF);
            String slotHex = String.format("%02X", slotIndex & 0xFF);

            String finalHexPayload = VIEW_SLOT_CLICK_TEMPLATE
                    .replace("{session_id}", sessionIdHex)
                    .replace("{component_id}", componentHex)
                    .replace("{slot_index_hex}", slotHex);

            String cleanHex = finalHexPayload.replaceAll("\\s", "");
            byte[] data = new byte[cleanHex.length() / 2];
            for (int i = 0; i < data.length; i++) {
                data[i] = (byte) Integer.parseInt(cleanHex.substring(i * 2, i * 2 + 2), 16);
            }

            PacketBuffer payload = new PacketBuffer(Unpooled.wrappedBuffer(data));
            CPacketCustomPayload packet = new CPacketCustomPayload("OwlViewChannel", payload);
            mc.getConnection().sendPacket(packet);
            return true;
        } catch (Exception e) {
            zszlScriptMod.LOGGER
                    .error("[RefineHelper] 发送View slot_click失败, mainGuiId=" + mainGuiId + ", slot=" + slotIndex, e);
            return false;
        }
    }

    private Integer extractSetSlotIndex(Object packet) {
        try {
            try {
                return (Integer) packet.getClass().getMethod("getSlot").invoke(packet);
            } catch (NoSuchMethodException ignored) {
            }
            try {
                return (Integer) packet.getClass().getMethod("func_149173_d").invoke(packet);
            } catch (NoSuchMethodException ignored) {
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private ItemStack extractSetSlotItem(Object packet) {
        try {
            try {
                Object obj = packet.getClass().getMethod("getStack").invoke(packet);
                return (obj instanceof ItemStack) ? (ItemStack) obj : ItemStack.EMPTY;
            } catch (NoSuchMethodException ignored) {
            }
            try {
                Object obj = packet.getClass().getMethod("func_149174_e").invoke(packet);
                return (obj instanceof ItemStack) ? (ItemStack) obj : ItemStack.EMPTY;
            } catch (NoSuchMethodException ignored) {
            }
        } catch (Exception ignored) {
        }
        return ItemStack.EMPTY;
    }

    private boolean hasResolveTag(ItemStack stack) {
        if (stack == null || stack.isEmpty() || !stack.hasTagCompound()) {
            return false;
        }
        NBTTagCompound tag = stack.getTagCompound();
        return tag != null && tag.hasKey("haveResolve");
    }

    private List<Integer> collectResolveSlotsFromInventory(Minecraft mc) {
        List<Integer> slots = new ArrayList<>();
        if (mc == null || mc.player == null || mc.player.inventory == null
                || mc.player.inventory.mainInventory == null) {
            return slots;
        }
        int size = mc.player.inventory.mainInventory.size();
        for (int i = 0; i < size && i <= 35; i++) {
            ItemStack stack = mc.player.inventory.mainInventory.get(i);
            if (stack != null && !stack.isEmpty() && hasResolveTag(stack)) {
                slots.add(i);
            }
        }
        return slots;
    }

    private boolean sendRefineClickPacket(int componentId, String sessionIdHex, boolean rightClickStart) {
        try {
            Minecraft mc = Minecraft.getMinecraft();
            if (mc.getConnection() == null) {
                return false;
            }

            String componentHex = String.format("%02X %02X %02X %02X",
                    (componentId >> 24) & 0xFF,
                    (componentId >> 16) & 0xFF,
                    (componentId >> 8) & 0xFF,
                    componentId & 0xFF);

            String finalHexPayload = (rightClickStart ? REFINE_START_RIGHT_CLICK_PAYLOAD_TEMPLATE
                    : REFINE_SELECT_PAYLOAD_TEMPLATE)
                    .replace("{session_id}", sessionIdHex)
                    .replace("{component_id}", componentHex);

            String cleanHex = finalHexPayload.replaceAll("\\s", "");
            byte[] data = new byte[cleanHex.length() / 2];
            for (int i = 0; i < data.length; i++) {
                data[i] = (byte) Integer.parseInt(cleanHex.substring(i * 2, i * 2 + 2), 16);
            }

            PacketBuffer payload = new PacketBuffer(Unpooled.wrappedBuffer(data));
            CPacketCustomPayload packet = new CPacketCustomPayload("OwlViewChannel", payload);
            mc.getConnection().sendPacket(packet);
            return true;
        } catch (Exception e) {
            zszlScriptMod.LOGGER.error("[RefineHelper] 发送精炼点击包失败, componentId=" + componentId, e);
            return false;
        }
    }

    private Integer getCapturedInt(String key) {
        byte[] bytes = CapturedIdRuleManager.getCapturedIdBytes(key);
        if (bytes == null || bytes.length == 0) {
            return null;
        }
        int value = 0;
        for (byte b : bytes) {
            value = (value << 8) | (b & 0xFF);
        }
        return value;
    }
}
