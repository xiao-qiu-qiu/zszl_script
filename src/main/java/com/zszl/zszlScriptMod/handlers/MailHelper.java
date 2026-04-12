package com.zszl.zszlScriptMod.handlers;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import com.zszl.zszlScriptMod.config.DebugModule;
import com.zszl.zszlScriptMod.config.ModConfig;
import com.zszl.zszlScriptMod.utils.CapturedIdRuleManager;
import com.zszl.zszlScriptMod.utils.ModUtils;
import com.zszl.zszlScriptMod.utils.PacketCaptureHandler;
import com.zszl.zszlScriptMod.zszlScriptMod;

import io.netty.buffer.Unpooled;
import net.minecraft.client.Minecraft;
import net.minecraft.network.PacketBuffer;
import net.minecraft.network.play.client.CPacketCustomPayload;

public class MailHelper {

    public static final MailHelper INSTANCE = new MailHelper();

    private static final String MAIL_CLICK_PAYLOAD_TEMPLATE = "00 09 43 6F 6D 70 6F 6E 65 6E 74 00 00 {session_id} {mail_id} 00 0C 42 75 74 74 6F 6E 5F 63 6C 69 63 6B 01 00 00 00 00 00 00 00 00 00 00 00 00 00";

    // 使用独立ID段，避免与其它GUI模块（如Overlay快捷按钮9001-9005）冲突
    public static final int BTN_RECEIVE_ALL_ID = 29001;
    public static final int BTN_DELETE_ALL_ID = 29002;
    public static final int BTN_STOP_ID = 29003;
    public static final int BTN_VIEW_IDS = 29004;
    public static final int BTN_SETTINGS_ID = 29005;

    public static class MailInfo {
        public final int mailId;
        public int removeButtonId = -1;
        public String mailName = "";
        public String mailTime = "";
        public int pendingGoldCost = -1;
        public int pendingCouponCost = -1;
        public int pendingGoldLabelComponentId = -1;
        public int pendingCouponLabelComponentId = -1;
        public final Map<Integer, Integer> pendingCostCandidates = new LinkedHashMap<>();
        public int nameLabelComponentId = -1;
        public int timeLabelComponentId = -1;
        public int attachmentId = -1;
        public int confirmationButtonId = -1;
        public int finalConfirmationId = -1;

        public MailInfo(int mailId) {
            this.mailId = mailId;
        }
    }

    public final List<MailInfo> mailInfoList = Collections.synchronizedList(new ArrayList<>());

    public final List<Integer> mailListButtonIds = new AbstractList<Integer>() {
        @Override
        public Integer get(int index) {
            return mailInfoList.get(index).mailId;
        }

        @Override
        public int size() {
            return mailInfoList.size();
        }

        @Override
        public void add(int index, Integer element) {
            MailInfo newInfo = new MailInfo(element);
            mailInfoList.add(index, newInfo);
            pendingMetadataQueue.add(newInfo);
            triggerAutoReceive(newInfo);
        }

        @Override
        public boolean add(Integer element) {
            MailInfo newInfo = new MailInfo(element);
            boolean result = mailInfoList.add(newInfo);
            if (result) {
                pendingMetadataQueue.add(newInfo);
                triggerAutoReceive(newInfo);
            }
            return result;
        }

        @Override
        public Integer remove(int index) {
            MailInfo removed = mailInfoList.remove(index);
            pendingMetadataQueue.remove(removed);
            return removed.mailId;
        }

        @Override
        public void clear() {
            mailInfoList.clear();
            pendingMetadataQueue.clear();
        }
    };

    public final Map<String, Integer> componentIdMap = new ConcurrentHashMap<>();

    public volatile boolean isFingerprintTicketValid = false;
    public volatile boolean isMailContextActive = false;
    private volatile int mailMainRollComponentId = -1;
    private volatile int ignoreNextMainGuiCloseCount = 0;

    private volatile boolean isAutomating = false;
    private final Queue<MailInfo> mailProcessingQueue = new ConcurrentLinkedQueue<>();
    private final Queue<MailInfo> pendingMetadataQueue = new ConcurrentLinkedQueue<>();
    private volatile MailInfo currentMailProcessing = null;

    private enum AutomationMode {
        NONE,
        RECEIVE,
        DELETE
    }

    private volatile AutomationMode currentMode = AutomationMode.NONE;
    private volatile boolean currentMailHideFeatureReceived = false;
    private volatile Integer lastSyncedMailListButtonId = null;
    private volatile Integer lastSyncedRemovedComponentId = null;
    private volatile Integer lastOpenedMailIdForPayment = null;
    private volatile long lastOpenedMailForPaymentAt = 0L;
    private volatile String learnedCostSessionHex = null;
    private volatile int learnedGoldCostLabelComponentId = -1;
    private volatile int learnedCouponCostLabelComponentId = -1;
    private volatile int lastDeleteConfirmClickMailId = -1;
    private volatile int lastDeleteConfirmClickButtonId = -1;
    private volatile long lastDeleteConfirmClickAt = 0L;

    private enum AutomationState {
        IDLE,
        WAITING_FOR_ATTACHMENT_ID,
        WAITING_FOR_CONFIRMATION_BUTTON_ID,
        WAITING_FOR_FINAL_CONFIRMATION_ID,
        WAITING_FOR_FINAL_GUI_CLOSE,
        WAITING_FOR_DELETE_CONFIRMATION_BUTTON_ID,
        WAITING_FOR_DELETE_FINAL_CONFIRMATION_BUTTON_ID,
        WAITING_FOR_DELETE_REMOVED_COMPONENT_ID
    }

    private volatile AutomationState currentState = AutomationState.IDLE;

    private static final Minecraft mc = Minecraft.getMinecraft();

    private MailHelper() {
    }

    private void triggerAutoReceive(MailInfo newMail) {
        if (!MailConfig.INSTANCE.autoReceiveEnabled)
            return;

        mailProcessingQueue.add(newMail);
        ModConfig.debugPrint(DebugModule.MAIL_GUI, "新邮件 (ID: " + newMail.mailId + ") 已加入自动领取队列。");

        if (!isAutomating) {
            isAutomating = true;
            // 关键修复：自动领取入口必须显式进入 RECEIVE 模式。
            // 否则会停留在 NONE，导致金额捕获与后续状态机分支被跳过，表现为“只点开邮件就卡住”。
            currentMode = AutomationMode.RECEIVE;
            currentState = AutomationState.IDLE;
            ModConfig.debugPrint(DebugModule.MAIL_GUI, "检测到新邮件，开始自动处理队列...");
            processNextMailInQueue();
            return;
        }

        // 容错：若历史异常导致处于自动化中但模式丢失，收到新邮件时自动纠正回 RECEIVE。
        if (currentMode == AutomationMode.NONE) {
            currentMode = AutomationMode.RECEIVE;
            ModConfig.debugPrint(DebugModule.MAIL_GUI,
                    "检测到自动领取模式丢失，已自动恢复为 RECEIVE。mailId=" + newMail.mailId);
        }
    }

    public void reset() {
        stopAutomation("MailHelper.reset");
        componentIdMap.clear();
        mailInfoList.clear();
        pendingMetadataQueue.clear();
        clearLearnedCostLabelWhitelist("MailHelper.reset");
        deactivateMailContext("MailHelper.reset");
        ModConfig.debugPrint(DebugModule.MAIL_GUI, "MailHelper 状态已完全重置。");
    }

    public void deactivateMailContext(String reason) {
        isFingerprintTicketValid = false;
        isMailContextActive = false;
        mailMainRollComponentId = -1;
        ignoreNextMainGuiCloseCount = 0;
        ModConfig.debugPrint(DebugModule.MAIL_GUI,
                "邮件上下文已重置: reason=" + reason);
    }

    public void onMailSubPageOpened() {
        ignoreNextMainGuiCloseCount = Math.min(2, ignoreNextMainGuiCloseCount + 1);
        lastOpenedMailIdForPayment = null;
        lastOpenedMailForPaymentAt = 0L;
        ModConfig.debugPrint(DebugModule.MAIL_GUI,
                "进入邮件子页面，下一次主界面关闭事件将被忽略。ignoreNextMainGuiCloseCount=" + ignoreNextMainGuiCloseCount);
    }

    public boolean onFingerprintPacketReceived() {
        if (!isFingerprintTicketValid) {
            isFingerprintTicketValid = true;
            ModConfig.debugPrint(DebugModule.MAIL_GUI, "邮件GUI指纹已捕获！门票(isFingerprintTicketValid)已设置为 true。");
            return true;
        }
        return false;
    }

    public boolean onMailRollOffsetDetected(int componentId) {
        Integer captured = getCapturedInt("mail_main_gui_id");
        if (captured != null && captured > 0 && componentId != captured) {
            ModConfig.debugPrint(DebugModule.MAIL_GUI,
                    "忽略非邮件Roll指纹: detected=" + componentId + ", expected=" + captured);
            return false;
        }

        if (captured != null && captured > 0) {
            mailMainRollComponentId = captured;
        } else if (componentId > 0) {
            mailMainRollComponentId = componentId;
        }

        boolean newlyActivated = onFingerprintPacketReceived();
        if (newlyActivated && mc.currentScreen != null) {
            try {
                mc.currentScreen.initGui();
            } catch (Exception ignored) {
            }
        }
        return newlyActivated;
    }

    public boolean onMailViewGuiOpenDetected(int componentId) {
        Integer captured = getCapturedInt("mail_main_gui_id");
        if (captured == null || captured <= 0) {
            return false;
        }
        if (componentId != captured) {
            return false;
        }
        mailMainRollComponentId = captured;
        return onFingerprintPacketReceived();
    }

    public void onViewGuiRemoved(int removedComponentId) {
        if (removedComponentId <= 0) {
            return;
        }

        if (ignoreNextMainGuiCloseCount > 0) {
            ignoreNextMainGuiCloseCount--;
            ModConfig.debugPrint(DebugModule.MAIL_GUI,
                    "忽略一次主界面关闭事件（通常由邮件子页面切换触发）。componentId=" + removedComponentId
                            + ", remainIgnore=" + ignoreNextMainGuiCloseCount);
            return;
        }

        int expected = mailMainRollComponentId;
        if (expected <= 0) {
            Integer captured = getCapturedInt("mail_main_gui_id");
            if (captured != null) {
                expected = captured;
            }
        }

        if (expected > 0 && removedComponentId == expected) {
            ModConfig.debugPrint(DebugModule.MAIL_GUI,
                    "检测到邮件主界面关闭(View_set_removeGui)，仅移除自定义按钮上下文，不停止自动化。componentId=" + removedComponentId);
            deactivateMailContext("View_set_removeGui:" + removedComponentId);
        }
    }

    public void syncCapturedValuesFromRules() {
        Integer listButtonId = getCapturedInt("mail_list_button_id");
        Integer listButtonNewId = getCapturedInt("mail_list_button_newid");

        Integer effectiveListButtonId = listButtonNewId != null ? listButtonNewId : listButtonId;
        if (effectiveListButtonId != null && !Objects.equals(lastSyncedMailListButtonId, effectiveListButtonId)) {
            lastSyncedMailListButtonId = effectiveListButtonId;
            if (!mailListButtonIds.contains(effectiveListButtonId)) {
                mailListButtonIds.add(effectiveListButtonId);
                ModConfig.debugPrint(DebugModule.MAIL_GUI,
                        "通过captured_ids规则同步邮件ID: " + effectiveListButtonId + "。当前总数: " + mailListButtonIds.size());
            }
        }

        Integer removedId = getCapturedInt("mail_removed_component_id");
        if (removedId != null && !Objects.equals(lastSyncedRemovedComponentId, removedId)) {
            lastSyncedRemovedComponentId = removedId;
            onRemovedComponentCaptured(removedId);
        }

        if (!isAutomating || currentMode != AutomationMode.DELETE || currentMailProcessing == null) {
            return;
        }

        if (currentState == AutomationState.WAITING_FOR_DELETE_CONFIRMATION_BUTTON_ID
                || currentState == AutomationState.WAITING_FOR_DELETE_REMOVED_COMPONENT_ID) {
            Integer deleteConfirmId = getCapturedInt("mail_delete_confirm_btn_id");
            if (deleteConfirmId != null) {
                onCreateButtonCaptured(deleteConfirmId, "OkRemoveInfoBuyOwlButton(captured_ids)");
            }
        }

        if (currentState == AutomationState.WAITING_FOR_DELETE_FINAL_CONFIRMATION_BUTTON_ID
                || currentState == AutomationState.WAITING_FOR_DELETE_REMOVED_COMPONENT_ID) {
            Integer deleteFinalConfirmId = getCapturedInt("mail_delete_final_confirm_btn_id");
            if (deleteFinalConfirmId != null) {
                onCreateButtonCaptured(deleteFinalConfirmId, "infoBuyOwlButton(captured_ids)");
            }
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

    public void stopAutomation() {
        stopAutomation("未提供停止原因");
    }

    public void stopAutomation(String reason) {
        if (isAutomating) {
            String detail = (reason == null || reason.trim().isEmpty()) ? "未提供停止原因" : reason;
            String modeText = String.valueOf(currentMode);
            String stateText = String.valueOf(currentState);
            String currentMailText = currentMailProcessing == null ? "none"
                    : String.valueOf(currentMailProcessing.mailId);

            isAutomating = false;
            currentMode = AutomationMode.NONE;
            currentState = AutomationState.IDLE;
            currentMailProcessing = null;
            currentMailHideFeatureReceived = false;
            mailProcessingQueue.clear();
            ModUtils.DelayScheduler.instance.cancelTasks(task -> "MAIL_TASK".equals(task.getTag()));
            ModConfig.debugPrint(DebugModule.MAIL_GUI,
                    "§c自动化任务已停止。reason=" + detail + ", prevMode=" + modeText
                            + ", prevState=" + stateText + ", currentMail=" + currentMailText);
        }
    }

    public void removeMailById(int mailId) {
        MailInfo toRemove = null;
        synchronized (mailInfoList) {
            for (MailInfo info : mailInfoList) {
                if (info.mailId == mailId) {
                    toRemove = info;
                    break;
                }
            }
        }
        boolean removed = toRemove != null && mailInfoList.remove(toRemove);
        if (toRemove != null) {
            pendingMetadataQueue.remove(toRemove);
        }
        mailProcessingQueue.removeIf(info -> info.mailId == mailId);
        if (currentMailProcessing != null && currentMailProcessing.mailId == mailId) {
            currentMailProcessing = null;
        }
        if (removed) {
            ModConfig.debugPrint(DebugModule.MAIL_GUI, "邮件ID " + mailId + " 已从内部列表中移除 (与GUI同步)。");
        }
    }

    public void onRemovedComponentCaptured(int removedComponentId) {
        if (removedComponentId <= 0) {
            return;
        }

        // 删除自动化优先处理：避免先 removeMailById 导致 currentMailProcessing 被清空，后续无法继续队列。
        if (isAutomating
                && currentMode == AutomationMode.DELETE
                && currentState == AutomationState.WAITING_FOR_DELETE_REMOVED_COMPONENT_ID
                && currentMailProcessing != null
                && removedComponentId == currentMailProcessing.mailId) {
            cancelTimeout();
            int finishedMailId = currentMailProcessing.mailId;
            removeMailById(removedComponentId);
            ModConfig.debugPrint(DebugModule.MAIL_GUI,
                    "§a删除结果确认: 已收到移除组件ID " + removedComponentId + "，邮件ID " + finishedMailId + " 删除完成，处理下一封...");
            currentState = AutomationState.IDLE;
            processNextMailInQueue();
            return;
        }

        boolean existed;
        synchronized (mailInfoList) {
            existed = mailInfoList.stream().anyMatch(info -> info.mailId == removedComponentId);
        }

        if (existed) {
            removeMailById(removedComponentId);
            ModConfig.debugPrint(DebugModule.MAIL_GUI,
                    "检测到移除组件事件，已同步删除邮件卡片: removedComponentId=" + removedComponentId);
        }

        if (!isAutomating || currentMode != AutomationMode.DELETE
                || currentState != AutomationState.WAITING_FOR_DELETE_REMOVED_COMPONENT_ID
                || currentMailProcessing == null) {
            return;
        }
    }

    public void clearAllMails() {
        int oldSize = mailInfoList.size();
        mailInfoList.clear();
        pendingMetadataQueue.clear();
        mailProcessingQueue.clear();
        currentMailProcessing = null;
        ModConfig.debugPrint(DebugModule.MAIL_GUI, "已一键清空捕获邮件ID列表，移除数量: " + oldSize);
    }

    public void onMailLabelTextCaptured(String text) {
        onMailLabelTextCaptured(-1, text, "unknown");
    }

    public void onCreateLabelCaptured(int componentId, String name) {
        if (name == null || componentId <= 0)
            return;

        MailInfo target = pendingMetadataQueue.peek();
        while (target != null && !mailInfoList.contains(target)) {
            pendingMetadataQueue.poll();
            target = pendingMetadataQueue.peek();
        }
        if (target == null)
            return;

        if (containsIgnoreCase(name, "getSendNameIndex")) {
            target.nameLabelComponentId = componentId;
            ModConfig.debugPrint(DebugModule.MAIL_GUI,
                    "绑定名称标签: mailId=" + target.mailId + ", labelId=" + componentId + ", name=" + name);
        } else if (containsIgnoreCase(name, "getEmailDataLbelIndex")
                || containsIgnoreCase(name, "getEmailDataLabelIndex")) {
            target.timeLabelComponentId = componentId;
            ModConfig.debugPrint(DebugModule.MAIL_GUI,
                    "绑定时间标签: mailId=" + target.mailId + ", labelId=" + componentId + ", name=" + name);
        } else if (containsIgnoreCase(name, "desNameIndex")) {
            ModConfig.debugPrint(DebugModule.MAIL_GUI,
                    "识别说明标签(将忽略内容): mailId=" + target.mailId + ", labelId=" + componentId + ", name=" + name);
        }
    }

    private boolean containsIgnoreCase(String source, String token) {
        if (source == null || token == null) {
            return false;
        }
        return source.toLowerCase(Locale.ROOT).contains(token.toLowerCase(Locale.ROOT));
    }

    private boolean isDeleteFirstConfirmButton(String buttonName) {
        return containsIgnoreCase(buttonName, "OkRemoveInfoBuyOwlButton");
    }

    private boolean isDeleteFinalConfirmButton(String buttonName) {
        if (buttonName == null) {
            return false;
        }
        // 注意：noRemoveInfoBuyOwlButton 包含 infoBuyOwlButton 子串，必须排除。
        return containsIgnoreCase(buttonName, "infoBuyOwlButton")
                && !containsIgnoreCase(buttonName, "noRemoveInfoBuyOwlButton")
                && !containsIgnoreCase(buttonName, "OkRemoveInfoBuyOwlButton");
    }

    public boolean onMailLabelTextCaptured(int componentId, String text, String operation) {
        if (text == null)
            return false;

        String normalized = normalizeMailText(text);
        if (normalized.isEmpty())
            return false;

        tryCapturePendingCosts(componentId, normalized, operation);

        ModConfig.debugPrint(DebugModule.MAIL_GUI,
                "邮件文本进入匹配队列: componentId=" + componentId + ", op=" + operation + ", raw='" + text
                        + "', normalized='" + normalized + "', pending=" + pendingMetadataQueue.size());

        MailInfo target = pendingMetadataQueue.peek();
        while (target != null && !mailInfoList.contains(target)) {
            pendingMetadataQueue.poll();
            target = pendingMetadataQueue.peek();
        }
        if (target == null)
            return false;

        if (componentId > 0) {
            boolean isNameSource = componentId == target.nameLabelComponentId;
            boolean isTimeSource = componentId == target.timeLabelComponentId;
            if (!isNameSource && !isTimeSource) {
                ModConfig.debugPrint(DebugModule.MAIL_GUI,
                        "忽略非目标标签文本: mailId=" + target.mailId + ", componentId=" + componentId
                                + ", nameLabelId=" + target.nameLabelComponentId + ", timeLabelId="
                                + target.timeLabelComponentId + ", text='" + normalized + "'");
                return false;
            }
        }

        boolean isTimeLike = normalized.matches("\\d{2}-\\d{2}\\s+\\d{2}:\\d{2}");
        boolean isMetaLabel = normalized.endsWith(":") || normalized.endsWith("：");
        if (componentId > 0 && componentId == target.timeLabelComponentId) {
            if (target.mailTime == null || target.mailTime.isEmpty()) {
                target.mailTime = normalized;
                ModConfig.debugPrint(DebugModule.MAIL_GUI,
                        "邮件元数据匹配(时间标签): mailId=" + target.mailId + " -> 时间='" + target.mailTime + "'");
            }
        } else if (componentId > 0 && componentId == target.nameLabelComponentId) {
            if (target.mailName == null || target.mailName.isEmpty()) {
                target.mailName = normalized;
                ModConfig.debugPrint(DebugModule.MAIL_GUI,
                        "邮件元数据匹配(名称标签): mailId=" + target.mailId + " -> 名称='" + target.mailName + "'");
            }
        } else if (isTimeLike) {
            if (target.mailTime == null || target.mailTime.isEmpty()) {
                target.mailTime = normalized;
                ModConfig.debugPrint(DebugModule.MAIL_GUI,
                        "邮件元数据匹配: mailId=" + target.mailId + " -> 时间='" + target.mailTime + "'");
            }
        } else if (!isMetaLabel) {
            if (target.mailName == null || target.mailName.isEmpty()) {
                target.mailName = normalized;
                ModConfig.debugPrint(DebugModule.MAIL_GUI,
                        "邮件元数据匹配: mailId=" + target.mailId + " -> 名称='" + target.mailName + "'");
            } else if (target.mailTime == null || target.mailTime.isEmpty()) {
                target.mailTime = normalized;
                ModConfig.debugPrint(DebugModule.MAIL_GUI,
                        "邮件元数据回填: mailId=" + target.mailId + " -> 时间='" + target.mailTime + "'");
            }
        } else {
            ModConfig.debugPrint(DebugModule.MAIL_GUI,
                    "跳过说明性文本: mailId=" + target.mailId + ", text='" + normalized + "'");
        }

        if (target.mailName != null && !target.mailName.isEmpty()
                && target.mailTime != null && !target.mailTime.isEmpty()) {
            pendingMetadataQueue.poll();
            ModConfig.debugPrint(DebugModule.MAIL_GUI,
                    "邮件元数据完成: mailId=" + target.mailId + ", name='" + target.mailName
                            + "', time='" + target.mailTime + "'。剩余待匹配=" + pendingMetadataQueue.size());
            return true;
        }
        return true;
    }

    private void tryCapturePendingCosts(int componentId, String normalizedText, String operation) {
        refreshLearnedCostWhitelistBySessionChange();

        // 金额仅来自 Label_set_text；明确排除 LabelArea_set_text / TextArea_set_text 等正文区域文本。
        if (!"Label_set_text".equals(operation)
                && !"Label_init(hex-fallback)".equals(operation)) {
            return;
        }

        Integer openedMailId = lastOpenedMailIdForPayment;

        // 自动领取模式下，仅在“等待附件/金额”阶段接收金额，避免列表区/正文区刷新串入。
        if (isAutomating && currentMode == AutomationMode.RECEIVE) {
            if (currentState != AutomationState.WAITING_FOR_ATTACHMENT_ID || currentMailProcessing == null) {
                return;
            }
            if (openedMailId == null || openedMailId <= 0) {
                openedMailId = currentMailProcessing.mailId;
                lastOpenedMailIdForPayment = openedMailId;
                if (lastOpenedMailForPaymentAt <= 0L) {
                    lastOpenedMailForPaymentAt = System.currentTimeMillis();
                }
            }
            if (!Objects.equals(openedMailId, currentMailProcessing.mailId)) {
                return;
            }
        }

        // 兜底：自动领取时即使未捕获到 C->S Button_click，也按当前处理邮件做金额归属
        if ((openedMailId == null || openedMailId <= 0)
                && isAutomating
                && currentMode == AutomationMode.RECEIVE
                && currentState == AutomationState.WAITING_FOR_ATTACHMENT_ID
                && currentMailProcessing != null) {
            openedMailId = currentMailProcessing.mailId;
            lastOpenedMailIdForPayment = openedMailId;
            if (lastOpenedMailForPaymentAt <= 0L) {
                lastOpenedMailForPaymentAt = System.currentTimeMillis();
            }
        }

        if (openedMailId == null || openedMailId <= 0 || componentId <= 0) {
            return;
        }

        if (isLearnedCostWhitelistActive()
                && componentId != learnedGoldCostLabelComponentId
                && componentId != learnedCouponCostLabelComponentId) {
            ModConfig.debugPrint(DebugModule.MAIL_GUI,
                    "金额白名单忽略: componentId=" + componentId + ", text=" + normalizedText
                            + ", expectedGoldId=" + learnedGoldCostLabelComponentId
                            + ", expectedCouponId=" + learnedCouponCostLabelComponentId);
            return;
        }

        long captureWindowMs = Math.max(4000L, MailConfig.INSTANCE.waitTimeoutTicks * 50L + 1000L);
        if ((System.currentTimeMillis() - lastOpenedMailForPaymentAt) > captureWindowMs) {
            return;
        }

        int parsedCost = parseNonNegativeIntStrict(normalizedText);
        if (parsedCost < 0) {
            return;
        }

        MailInfo target = findMailById(openedMailId);
        if (target == null) {
            return;
        }

        target.pendingCostCandidates.put(componentId, parsedCost);
        recalculatePendingCostsByCandidates(target, operation);

        if (isAutomating && currentMode == AutomationMode.RECEIVE
                && currentState == AutomationState.WAITING_FOR_ATTACHMENT_ID
                && currentMailProcessing != null
                && currentMailProcessing.mailId == target.mailId
                && currentMailProcessing.attachmentId > 0
                && isCostReady(currentMailProcessing)) {
            continueAutoReceiveAfterCostAndAttachmentReady();
        }
    }

    private void recalculatePendingCostsByCandidates(MailInfo target, String operation) {
        if (target == null || target.pendingCostCandidates.isEmpty()) {
            return;
        }

        if (isLearnedCostWhitelistActive()) {
            Integer goldCost = target.pendingCostCandidates.get(learnedGoldCostLabelComponentId);
            Integer couponCost = target.pendingCostCandidates.get(learnedCouponCostLabelComponentId);
            if (goldCost != null) {
                target.pendingGoldLabelComponentId = learnedGoldCostLabelComponentId;
                target.pendingGoldCost = goldCost;
            }
            if (couponCost != null) {
                target.pendingCouponLabelComponentId = learnedCouponCostLabelComponentId;
                target.pendingCouponCost = couponCost;
            }
            if (goldCost != null && couponCost != null) {
                return;
            }
        }

        List<Integer> ids = new ArrayList<>(target.pendingCostCandidates.keySet());
        Collections.sort(ids);

        if (ids.size() == 1) {
            int onlyId = ids.get(0);
            target.pendingGoldLabelComponentId = onlyId;
            target.pendingGoldCost = target.pendingCostCandidates.get(onlyId);
            return;
        }

        int couponId = ids.get(ids.size() - 1);
        int goldId = ids.get(ids.size() - 2);
        target.pendingGoldLabelComponentId = goldId;
        target.pendingCouponLabelComponentId = couponId;
        target.pendingGoldCost = target.pendingCostCandidates.get(goldId);
        target.pendingCouponCost = target.pendingCostCandidates.get(couponId);
        learnCostLabelWhitelist(goldId, couponId);

        ModConfig.debugPrint(DebugModule.MAIL_GUI,
                "重算待支付金额: mailId=" + target.mailId
                        + ", gold(component=" + goldId + ")=" + target.pendingGoldCost
                        + ", coupon(component=" + couponId + ")=" + target.pendingCouponCost
                        + ", op=" + operation);
    }

    private boolean isCostReady(MailInfo info) {
        return info != null && info.pendingGoldCost >= 0 && info.pendingCouponCost >= 0;
    }

    private void continueAutoReceiveAfterCostAndAttachmentReady() {
        if (currentState != AutomationState.WAITING_FOR_ATTACHMENT_ID || currentMailProcessing == null) {
            return;
        }

        if (shouldSkipReceiveByCostFilter(currentMailProcessing)) {
            cancelTimeout();
            ModConfig.debugPrint(DebugModule.MAIL_GUI,
                    "§e邮件ID " + currentMailProcessing.mailId + " 命中过滤条件，跳过一键领取。"
                            + "(金币=" + formatCostValue(currentMailProcessing.pendingGoldCost)
                            + ", 点券=" + formatCostValue(currentMailProcessing.pendingCouponCost)
                            + ", 阈值金币=" + MailConfig.INSTANCE.autoReceiveMaxGold
                            + ", 阈值点券=" + MailConfig.INSTANCE.autoReceiveMaxCoupon + ")");
            currentState = AutomationState.IDLE;
            processNextMailInQueue();
            return;
        }

        int attachmentId = currentMailProcessing.attachmentId;
        currentState = AutomationState.WAITING_FOR_CONFIRMATION_BUTTON_ID;
        ModConfig.debugPrint(DebugModule.MAIL_GUI,
                "§a金额已就绪，继续领取流程: mailId=" + currentMailProcessing.mailId
                        + ", attachmentId=" + attachmentId);

        sendClickPacket(attachmentId);
        ModConfig.debugPrint(DebugModule.MAIL_GUI, "步骤2/5: 已点击附件ID，正在等待'确定并删除'按钮ID...");
        startTimeout("'确定并删除'按钮");
    }

    private int parseNonNegativeIntStrict(String text) {
        if (text == null) {
            return -1;
        }
        String t = text.trim();
        if (t.isEmpty() || !t.matches("\\d+")) {
            return -1;
        }
        try {
            return Integer.parseInt(t);
        } catch (Exception ignored) {
            return -1;
        }
    }

    private MailInfo findMailById(int mailId) {
        synchronized (mailInfoList) {
            for (MailInfo info : mailInfoList) {
                if (info != null && info.mailId == mailId) {
                    return info;
                }
            }
        }
        return null;
    }

    public void onOutboundOwlViewPacket(String channel, byte[] outboundData) {
        if (!"OwlViewChannel".equals(channel) || outboundData == null || outboundData.length == 0) {
            return;
        }
        int clickedId = extractClickedComponentId(outboundData);
        if (clickedId <= 0) {
            return;
        }

        MailInfo clickedMail = findMailById(clickedId);
        if (clickedMail == null) {
            return;
        }

        markMailOpenedForPayment(clickedMail);
        ModConfig.debugPrint(DebugModule.MAIL_GUI,
                "检测到邮件点击，开始等待待支付信息: mailId=" + clickedId);
    }

    private void markMailOpenedForPayment(MailInfo mail) {
        if (mail == null) {
            return;
        }
        lastOpenedMailIdForPayment = mail.mailId;
        lastOpenedMailForPaymentAt = System.currentTimeMillis();
        mail.pendingGoldCost = -1;
        mail.pendingCouponCost = -1;
        mail.pendingGoldLabelComponentId = -1;
        mail.pendingCouponLabelComponentId = -1;
        mail.pendingCostCandidates.clear();
    }

    private int extractClickedComponentId(byte[] outboundData) {
        byte[] marker = "Button_click".getBytes(java.nio.charset.StandardCharsets.US_ASCII);
        int markerPos = indexOf(outboundData, marker);
        if (markerPos < 6) {
            return -1;
        }
        if ((outboundData[markerPos - 1] & 0xFF) != 0x0C || (outboundData[markerPos - 2] & 0xFF) != 0x00) {
            return -1;
        }
        int p = markerPos - 6;
        return ((outboundData[p] & 0xFF) << 24)
                | ((outboundData[p + 1] & 0xFF) << 16)
                | ((outboundData[p + 2] & 0xFF) << 8)
                | (outboundData[p + 3] & 0xFF);
    }

    private int indexOf(byte[] source, byte[] target) {
        if (source == null || target == null || target.length == 0 || source.length < target.length) {
            return -1;
        }
        for (int i = 0; i <= source.length - target.length; i++) {
            boolean matched = true;
            for (int j = 0; j < target.length; j++) {
                if (source[i + j] != target[j]) {
                    matched = false;
                    break;
                }
            }
            if (matched) {
                return i;
            }
        }
        return -1;
    }

    private String normalizeMailText(String text) {
        if (text == null)
            return "";
        String normalized = text
                .replaceAll("§.", "")
                .replace("\u0000", "")
                .trim();
        return normalized;
    }

    // 查询当前状态，供解码器使用
    public boolean isWaitingForAttachmentId() {
        return currentState == AutomationState.WAITING_FOR_ATTACHMENT_ID;
    }

    public boolean isWaitingForConfirmationButtonId() {
        return currentState == AutomationState.WAITING_FOR_CONFIRMATION_BUTTON_ID;
    }

    public boolean isWaitingForFinalConfirmationId() {
        return currentState == AutomationState.WAITING_FOR_FINAL_CONFIRMATION_ID;
    }

    public boolean isWaitingForFinalGuiClose() {
        return currentState == AutomationState.WAITING_FOR_FINAL_GUI_CLOSE;
    }

    public boolean isWaitingForDeleteConfirmationButtonId() {
        return currentState == AutomationState.WAITING_FOR_DELETE_CONFIRMATION_BUTTON_ID;
    }

    public boolean isWaitingForDeleteFinalConfirmationButtonId() {
        return currentState == AutomationState.WAITING_FOR_DELETE_FINAL_CONFIRMATION_BUTTON_ID;
    }

    public void startAutoReceiveAll() {
        if (isAutomating) {
            ModConfig.debugPrint(DebugModule.MAIL_GUI, "§e用户尝试启动，但自动化任务已在进行中。");
            return;
        }
        if (mailInfoList.isEmpty()) {
            ModConfig.debugPrint(DebugModule.MAIL_GUI, "§e用户尝试启动，但未找到任何邮件ID。");
            return;
        }

        isAutomating = true;
        currentMode = AutomationMode.RECEIVE;
        mailProcessingQueue.clear();
        synchronized (mailInfoList) {
            for (int i = mailInfoList.size() - 1; i >= 0; i--) {
                mailProcessingQueue.add(mailInfoList.get(i));
            }
        }

        ModConfig.debugPrint(DebugModule.MAIL_GUI, "§a开始处理 " + mailProcessingQueue.size() + " 封邮件...");

        processNextMailInQueue();
    }

    private void processNextMailInQueue() {
        if (!isAutomating)
            return;

        currentMailProcessing = mailProcessingQueue.poll();
        currentMailHideFeatureReceived = false;
        if (currentMailProcessing == null) {
            ModConfig.debugPrint(DebugModule.MAIL_GUI, "§a所有邮件处理完毕！");
            stopAutomation("队列处理完成");
            return;
        }

        ModConfig.debugPrint(DebugModule.MAIL_GUI, "  - 处理邮件 (ID: " + currentMailProcessing.mailId + ")...");

        if (currentMode == AutomationMode.DELETE) {
            int deleteButtonId = currentMailProcessing.removeButtonId > 0
                    ? currentMailProcessing.removeButtonId
                    : (currentMailProcessing.mailId + 1);
            // 先置状态再发送点击，避免服务端瞬时回包导致首个确认按钮被“过早到达”而漏接。
            currentState = AutomationState.WAITING_FOR_DELETE_CONFIRMATION_BUTTON_ID;
            sendClickPacket(deleteButtonId);
            ModConfig.debugPrint(DebugModule.MAIL_GUI,
                    "步骤1/3[删除]: 已点击删除按钮ID " + deleteButtonId
                            + (currentMailProcessing.removeButtonId > 0 ? " (绑定ID)" : " (回退 mailId+1)")
                            + "，等待第一个确认按钮...");
            startTimeout("删除第一个确认按钮");
            return;
        }

        // 步骤1: 点击邮件列表中的邮件
        // 先打开金额捕获窗口，避免个别场景下 C->S 点击包未被捕获导致金额丢失
        markMailOpenedForPayment(currentMailProcessing);
        // 先置状态再发送点击，避免附件ID瞬时返回时被错过。
        currentState = AutomationState.WAITING_FOR_ATTACHMENT_ID;
        sendClickPacket(currentMailProcessing.mailId);
        ModConfig.debugPrint(DebugModule.MAIL_GUI, "步骤1/5: 已点击邮件 " + currentMailProcessing.mailId + "，正在等待附件ID...");

        startTimeout("附件ID");
    }

    // 步骤2: 捕获到附件ID后，由解码器调用
    public void onAttachmentIdCaptured(int attachmentId) {
        if (currentState != AutomationState.WAITING_FOR_ATTACHMENT_ID || currentMailProcessing == null)
            return;

        currentMailProcessing.attachmentId = attachmentId;
        cancelTimeout();

        if (MailConfig.INSTANCE.amountFilterEnabled && !isCostReady(currentMailProcessing)) {
            ModConfig.debugPrint(DebugModule.MAIL_GUI,
                    "步骤2/5: 已捕获附件ID=" + attachmentId + "，等待待支付金币/点券数据...");
            startTimeout("待支付金币/点券");
            return;
        }

        continueAutoReceiveAfterCostAndAttachmentReady();
    }

    private boolean shouldSkipReceiveByCostFilter(MailInfo info) {
        if (info == null) {
            return false;
        }
        if (!MailConfig.INSTANCE.amountFilterEnabled) {
            return false;
        }
        int maxGold = Math.max(0, MailConfig.INSTANCE.autoReceiveMaxGold);
        int maxCoupon = Math.max(0, MailConfig.INSTANCE.autoReceiveMaxCoupon);

        boolean goldExceeded = info.pendingGoldCost >= 0 && info.pendingGoldCost > maxGold;
        boolean couponExceeded = info.pendingCouponCost >= 0 && info.pendingCouponCost > maxCoupon;
        return goldExceeded || couponExceeded;
    }

    private String formatCostValue(int value) {
        return value >= 0 ? String.valueOf(value) : "未获取";
    }

    private boolean isLearnedCostWhitelistActive() {
        return learnedGoldCostLabelComponentId > 0 && learnedCouponCostLabelComponentId > 0;
    }

    private String normalizeSessionHex(String sessionHex) {
        if (sessionHex == null) {
            return null;
        }
        String normalized = sessionHex.replaceAll("\\s+", "").toUpperCase(Locale.ROOT);
        return normalized.isEmpty() ? null : normalized;
    }

    private void clearLearnedCostLabelWhitelist(String reason) {
        learnedCostSessionHex = null;
        learnedGoldCostLabelComponentId = -1;
        learnedCouponCostLabelComponentId = -1;
        ModConfig.debugPrint(DebugModule.MAIL_GUI,
                "金额组件白名单已清空: reason=" + reason);
    }

    private void refreshLearnedCostWhitelistBySessionChange() {
        String current = normalizeSessionHex(PacketCaptureHandler.getSessionIdAsHex());
        if (current == null || learnedCostSessionHex == null) {
            return;
        }
        if (!current.equals(learnedCostSessionHex)) {
            clearLearnedCostLabelWhitelist("会话ID变更: " + learnedCostSessionHex + " -> " + current);
        }
    }

    private void learnCostLabelWhitelist(int goldId, int couponId) {
        if (goldId <= 0 || couponId <= 0) {
            return;
        }
        String currentSession = normalizeSessionHex(PacketCaptureHandler.getSessionIdAsHex());
        if (currentSession == null) {
            return;
        }

        if (!currentSession.equals(learnedCostSessionHex)
                || learnedGoldCostLabelComponentId != goldId
                || learnedCouponCostLabelComponentId != couponId) {
            learnedCostSessionHex = currentSession;
            learnedGoldCostLabelComponentId = goldId;
            learnedCouponCostLabelComponentId = couponId;
            ModConfig.debugPrint(DebugModule.MAIL_GUI,
                    "学习金额组件白名单: session=" + learnedCostSessionHex
                            + ", goldId=" + learnedGoldCostLabelComponentId
                            + ", couponId=" + learnedCouponCostLabelComponentId);
        }
    }

    // 步骤3: 捕获到“确定并删除”按钮ID后，由解码器调用
    public void onConfirmationButtonIdCaptured(int buttonId) {
        if (currentState != AutomationState.WAITING_FOR_CONFIRMATION_BUTTON_ID || currentMailProcessing == null)
            return;

        cancelTimeout();
        currentMailProcessing.confirmationButtonId = buttonId;
        ModConfig.debugPrint(DebugModule.MAIL_GUI, "§a步骤3/5: 成功捕获'确定并删除'按钮ID: " + buttonId + "，立即点击。");

        // 步骤3.1: 先置状态再点击，避免下一步确认按钮瞬时返回丢失。
        currentState = AutomationState.WAITING_FOR_FINAL_CONFIRMATION_ID;
        sendClickPacket(buttonId);
        ModConfig.debugPrint(DebugModule.MAIL_GUI, "步骤3/5: 已点击'确定并删除'，正在等待最终'确认'按钮ID...");
        startTimeout("最终'确认'按钮");
    }

    // 步骤4: 捕获到最终“确认”按钮ID后，由解码器调用
    public void onFinalConfirmationIdCaptured(int buttonId) {
        if (currentState != AutomationState.WAITING_FOR_FINAL_CONFIRMATION_ID || currentMailProcessing == null)
            return;

        cancelTimeout();
        currentMailProcessing.finalConfirmationId = buttonId;
        ModConfig.debugPrint(DebugModule.MAIL_GUI, "§a步骤4/5: 成功捕获最终'确认'按钮ID: " + buttonId + "，立即点击。");

        // 步骤4.1: 先置状态再点击，避免关闭事件先到导致状态错过。
        currentState = AutomationState.WAITING_FOR_FINAL_GUI_CLOSE;
        sendClickPacket(buttonId);
        ModConfig.debugPrint(DebugModule.MAIL_GUI, "步骤4/5: 已点击最终'确认'，正在等待确认框关闭...");
        startTimeout("确认框关闭");
    }

    // 步骤5: 捕获到最终确认框被移除的事件后，由解码器调用
    public void onFinalGuiClosed() {
        if (currentState != AutomationState.WAITING_FOR_FINAL_GUI_CLOSE)
            return;

        cancelTimeout();
        if (currentMailProcessing != null) {
            removeMailById(currentMailProcessing.mailId);
        }
        ModConfig.debugPrint(DebugModule.MAIL_GUI, "§a步骤5/5: 确认框已关闭，流程完成。处理下一封邮件。");
        currentState = AutomationState.IDLE;
        processNextMailInQueue();
    }

    public void onCreateButtonCaptured(int buttonId, String buttonName) {
        if (buttonName != null) {
            bindMailRemoveButtonId(buttonId, buttonName);
            // 关键诊断日志：若删除确认按钮到达时状态不匹配，便于定位竞态与时序问题。
            if (isDeleteFirstConfirmButton(buttonName) || isDeleteFinalConfirmButton(buttonName)) {
                ModConfig.debugPrint(DebugModule.MAIL_GUI,
                        "删除确认按钮到达: id=" + buttonId + ", name=" + buttonName
                                + ", mode=" + currentMode + ", state=" + currentState
                                + ", automating=" + isAutomating
                                + ", currentMail=" + (currentMailProcessing == null ? "none"
                                        : currentMailProcessing.mailId));
            }
        }

        if (!isAutomating || currentMailProcessing == null || buttonName == null) {
            return;
        }

        // 删除流程鲁棒兜底：无论状态轻微漂移或包到达顺序变化，都尽量执行确认点击。
        if (currentMode == AutomationMode.DELETE) {
            if (isDeleteFirstConfirmButton(buttonName)) {
                if (currentState == AutomationState.WAITING_FOR_DELETE_CONFIRMATION_BUTTON_ID
                        || currentState == AutomationState.WAITING_FOR_DELETE_REMOVED_COMPONENT_ID
                        || currentState == AutomationState.WAITING_FOR_DELETE_FINAL_CONFIRMATION_BUTTON_ID) {
                    clickDeleteConfirmButton(buttonId, buttonName, false);
                }
                return;
            }
            if (isDeleteFinalConfirmButton(buttonName)) {
                if (currentState == AutomationState.WAITING_FOR_DELETE_REMOVED_COMPONENT_ID
                        || currentState == AutomationState.WAITING_FOR_DELETE_FINAL_CONFIRMATION_BUTTON_ID
                        || currentState == AutomationState.WAITING_FOR_DELETE_CONFIRMATION_BUTTON_ID) {
                    clickDeleteConfirmButton(buttonId, buttonName, true);
                }
                return;
            }
        }

        if (currentState == AutomationState.WAITING_FOR_CONFIRMATION_BUTTON_ID) {
            onConfirmationButtonIdCaptured(buttonId);
            return;
        }

        if (currentState == AutomationState.WAITING_FOR_FINAL_CONFIRMATION_ID) {
            onFinalConfirmationIdCaptured(buttonId);
            return;
        }

        if (currentState == AutomationState.WAITING_FOR_DELETE_CONFIRMATION_BUTTON_ID) {
            if (isDeleteFirstConfirmButton(buttonName)) {
                cancelTimeout();
                ModConfig.debugPrint(DebugModule.MAIL_GUI,
                        "§a步骤2/3[删除]: 捕获第一个确认按钮ID: " + buttonId + " (" + buttonName + ")，立即点击。");
                // 兼容两种服务器流程：
                // 1) 只需一次确认（点击Ok后直接返回removeBaseComponent）
                // 2) 需要二次确认（会再弹出infoBuyOwlButton）
                // 统一先进入“等待移除组件”状态；若后续出现二次确认按钮，再自动点击。
                currentState = AutomationState.WAITING_FOR_DELETE_REMOVED_COMPONENT_ID;
                sendClickPacket(buttonId);
                ModConfig.debugPrint(DebugModule.MAIL_GUI,
                        "步骤2/3[删除]: 已点击第一个确认，等待移除组件ID（若出现二次确认将自动点击）...");
                startTimeout("删除结果移除组件ID");
            }
            return;
        }

        // 兼容“二次确认晚到”场景：在等待移除阶段如果弹出 infoBuyOwlButton，自动补点。
        if (currentState == AutomationState.WAITING_FOR_DELETE_REMOVED_COMPONENT_ID) {
            if (isDeleteFinalConfirmButton(buttonName)) {
                cancelTimeout();
                ModConfig.debugPrint(DebugModule.MAIL_GUI,
                        "§a删除补充确认: 捕获二次确认按钮ID: " + buttonId + " (" + buttonName + ")，立即点击。");
                sendClickPacket(buttonId);
                startTimeout("删除结果移除组件ID");
            }
            return;
        }

        if (currentState == AutomationState.WAITING_FOR_DELETE_FINAL_CONFIRMATION_BUTTON_ID) {
            if (isDeleteFinalConfirmButton(buttonName)) {
                cancelTimeout();
                ModConfig.debugPrint(DebugModule.MAIL_GUI,
                        "§a步骤3/3[删除]: 捕获第二个确认按钮ID: " + buttonId + " (" + buttonName + ")，立即点击。");
                sendClickPacket(buttonId);
                currentState = AutomationState.WAITING_FOR_DELETE_REMOVED_COMPONENT_ID;
                ModConfig.debugPrint(DebugModule.MAIL_GUI,
                        "步骤3/3[删除]: 已点击第二个确认，等待 Gui_set_removeBaseComponent 返回被移除的邮件ID...");
                startTimeout("删除结果移除组件ID");
            }
        }
    }

    private boolean shouldSkipDuplicateDeleteConfirmClick(int buttonId) {
        if (currentMailProcessing == null || buttonId <= 0) {
            return false;
        }
        long now = System.currentTimeMillis();
        // 同一封邮件、同一按钮在短时间内重复到达时去重，避免重复点击导致状态震荡。
        if (lastDeleteConfirmClickMailId == currentMailProcessing.mailId
                && lastDeleteConfirmClickButtonId == buttonId
                && (now - lastDeleteConfirmClickAt) <= 350L) {
            return true;
        }
        lastDeleteConfirmClickMailId = currentMailProcessing.mailId;
        lastDeleteConfirmClickButtonId = buttonId;
        lastDeleteConfirmClickAt = now;
        return false;
    }

    private void clickDeleteConfirmButton(int buttonId, String buttonName, boolean finalConfirm) {
        if (shouldSkipDuplicateDeleteConfirmClick(buttonId)) {
            ModConfig.debugPrint(DebugModule.MAIL_GUI,
                    "删除确认去重: 忽略短时重复按钮 id=" + buttonId + ", name=" + buttonName);
            return;
        }

        cancelTimeout();
        sendClickPacket(buttonId);
        currentState = AutomationState.WAITING_FOR_DELETE_REMOVED_COMPONENT_ID;
        if (finalConfirm) {
            ModConfig.debugPrint(DebugModule.MAIL_GUI,
                    "§a步骤3/3[删除|鲁棒]: 点击第二确认按钮: id=" + buttonId + ", name=" + buttonName
                            + "，等待 Gui_set_removeBaseComponent...");
        } else {
            ModConfig.debugPrint(DebugModule.MAIL_GUI,
                    "§a步骤2/3[删除|鲁棒]: 点击第一确认按钮: id=" + buttonId + ", name=" + buttonName
                            + "，等待移除组件（若弹出二次确认会继续自动点击）...");
        }
        startTimeout("删除结果移除组件ID");
    }

    private void bindMailRemoveButtonId(int buttonId, String buttonName) {
        if (!containsIgnoreCase(buttonName, "GetRemoveEmailButtonIndex")) {
            return;
        }

        MailInfo target = pendingMetadataQueue.peek();
        while (target != null && !mailInfoList.contains(target)) {
            pendingMetadataQueue.poll();
            target = pendingMetadataQueue.peek();
        }
        if (target == null) {
            synchronized (mailInfoList) {
                if (!mailInfoList.isEmpty()) {
                    target = mailInfoList.get(mailInfoList.size() - 1);
                }
            }
        }
        if (target != null) {
            target.removeButtonId = buttonId;
            ModConfig.debugPrint(DebugModule.MAIL_GUI,
                    "绑定邮件删除按钮: mailId=" + target.mailId + ", removeButtonId=" + buttonId + ", name=" + buttonName);
        }
    }

    public void onGuiSetHideCaptured(boolean p1, boolean p2, boolean p3) {
        if (!isAutomating || currentMode != AutomationMode.RECEIVE || currentMailProcessing == null) {
            return;
        }
        if (!p1 && !p2 && p3) {
            currentMailHideFeatureReceived = true;
            ModConfig.debugPrint(DebugModule.MAIL_GUI,
                    "§a已捕获 Gui_set_Hide(false,false,true) 特征，邮件ID " + currentMailProcessing.mailId + " 判定为有效。");
        }
    }

    private void startTimeout(String stepName) {
        final int timeoutTicks = Math.max(1, MailConfig.INSTANCE.waitTimeoutTicks);
        ModUtils.DelayScheduler.instance.schedule(() -> {
            if (isAutomating && currentMailProcessing != null && currentState != AutomationState.IDLE) {
                if (currentMode == AutomationMode.RECEIVE
                        && currentState == AutomationState.WAITING_FOR_ATTACHMENT_ID
                        && !currentMailHideFeatureReceived) {
                    ModConfig.debugPrint(DebugModule.MAIL_GUI,
                            "§e警告: 等待 [" + stepName + "] 超时，且未收到 Gui_set_Hide(false,false,true) 特征。"
                                    + "邮件ID " + currentMailProcessing.mailId + " 本次跳过，不从列表删除。");
                    currentState = AutomationState.IDLE;
                    processNextMailInQueue();
                    return;
                }

                if (currentMode == AutomationMode.RECEIVE
                        && currentState == AutomationState.WAITING_FOR_ATTACHMENT_ID
                        && MailConfig.INSTANCE.amountFilterEnabled
                        && currentMailProcessing.attachmentId > 0
                        && !isCostReady(currentMailProcessing)) {
                    ModConfig.debugPrint(DebugModule.MAIL_GUI,
                            "§e警告: 等待 [" + stepName + "] 超时，未获取到待支付金币/点券，跳过邮件ID "
                                    + currentMailProcessing.mailId + "。");
                    currentState = AutomationState.IDLE;
                    processNextMailInQueue();
                    return;
                }

                ModConfig.debugPrint(DebugModule.MAIL_GUI, "§e警告: 等待 [" + stepName + "] 超时。跳过此邮件。");
                currentState = AutomationState.IDLE;
                processNextMailInQueue();
            }
        }, timeoutTicks, "MAIL_TASK");
    }

    private void cancelTimeout() {
        ModUtils.DelayScheduler.instance.cancelTasks(task -> "MAIL_TASK".equals(task.getTag()));
    }

    public void startAutoDeleteAll() {
        if (isAutomating) {
            ModConfig.debugPrint(DebugModule.MAIL_GUI, "§e用户尝试启动一键删除，但自动化任务已在进行中。");
            return;
        }
        if (mailInfoList.isEmpty()) {
            ModConfig.debugPrint(DebugModule.MAIL_GUI, "§e用户尝试启动一键删除，但未找到任何邮件ID。");
            return;
        }

        isAutomating = true;
        currentMode = AutomationMode.DELETE;
        mailProcessingQueue.clear();
        synchronized (mailInfoList) {
            for (int i = mailInfoList.size() - 1; i >= 0; i--) {
                mailProcessingQueue.add(mailInfoList.get(i));
            }
        }

        ModConfig.debugPrint(DebugModule.MAIL_GUI, "§a开始一键删除 " + mailProcessingQueue.size() + " 封邮件...");
        processNextMailInQueue();
    }

    public void sendClickPacket(int componentId) {
        if (mc.player == null || mc.getConnection() == null)
            return;

        String sessionIdHex = PacketCaptureHandler.getSessionIdAsHex();
        if (sessionIdHex == null) {
            if (!(isAutomating && currentMode == AutomationMode.RECEIVE)) {
                stopAutomation("发送点击失败：未捕获会话ID(session_id)");
            }
            ModConfig.debugPrint(DebugModule.MAIL_GUI, "§c错误：无法发送点击！未捕获到会话ID。");
            return;
        }

        String mailIdHex = String.format("%02X %02X %02X %02X",
                (byte) (componentId >> 24), (byte) (componentId >> 16), (byte) (componentId >> 8), (byte) componentId);

        String finalHexPayload = MAIL_CLICK_PAYLOAD_TEMPLATE
                .replace("{session_id}", sessionIdHex)
                .replace("{mail_id}", mailIdHex);

        try {
            String cleanHex = finalHexPayload.replaceAll("\\s", "");
            byte[] data = new byte[cleanHex.length() / 2];
            for (int j = 0; j < data.length; j++) {
                data[j] = (byte) Integer.parseInt(cleanHex.substring(j * 2, j * 2 + 2), 16);
            }
            PacketBuffer payload = new PacketBuffer(Unpooled.wrappedBuffer(data));
            CPacketCustomPayload packet = new CPacketCustomPayload("OwlViewChannel", payload);
            mc.getConnection().sendPacket(packet);
        } catch (Exception e) {
            zszlScriptMod.LOGGER.error("[MailHelper] 发送点击数据包时发生严重错误, ComponentID: " + componentId, e);
            if (!(isAutomating && currentMode == AutomationMode.RECEIVE)) {
                stopAutomation("发送点击包异常，componentId=" + componentId + ", error=" + e.getClass().getSimpleName());
            }
        }
    }
}
