package com.zszl.zszlScriptMod.handlers;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.zszl.zszlScriptMod.config.ModConfig;
import com.zszl.zszlScriptMod.system.ProfileManager;
import com.zszl.zszlScriptMod.system.SimulatedKeyInputManager;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.monster.IMob;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.TextComponentString;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public class KillTimerHandler {

    public static final KillTimerHandler INSTANCE = new KillTimerHandler();
    private static final Minecraft mc = Minecraft.getMinecraft();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static boolean isEnabled = false;

    // 你的新默认与最小要求
    public static int panelX = 0;
    public static int panelY = 22;
    public static int panelWidth = 219;
    public static int panelHeight = 100;
    public static int panelAlpha = 100; // 30-240

    public static int combatTimeoutSeconds = 3; // 脱战判定秒数
    public static int disengageRemoveSeconds = 3; // 脱战后保留秒数（倒计时后删除）
    public static int stableDpsIntervalSeconds = 2; // 固定DPS刷新秒数
    public static final int DEATH_MODE_CHAT = 0;
    public static final int DEATH_MODE_PANEL_HOLD = 1;
    public static int deathDataMode = DEATH_MODE_CHAT;
    public static int deathPanelHoldSeconds = 3;
    public static boolean freeEditMode = false;
    private static boolean pendingMouseGrabAfterEdit = false;
    private static boolean escKeyWasDown = false;

    public static int scrollOffset = 0;

    private static final int MIN_W = 120;
    private static final int MIN_H = 60;
    private static final int HEADER_H = 18;
    private static final int FOOTER_H = 18;
    private static final int CARD_H = 28;
    private static final int CARD_GAP = 4;
    private static final int PADDING = 6;

    private static boolean dragging = false;
    private static boolean resizing = false;
    private static int dragOffsetX;
    private static int dragOffsetY;
    private static int resizeStartX;
    private static int resizeStartY;
    private static int resizeStartW;
    private static int resizeStartH;
    private static int selectedIndex = -1;

    private static final Map<UUID, TrackInfo> tracked = new HashMap<>();

    private static class TrackInfo {
        UUID id;
        String name;
        float maxHealth;
        float currentHealth;
        float minHealth;

        long combatStartMs;
        long lastDamageLikeMs;
        boolean paused;
        long pausedAccumulatedMs;
        long pausedStartMs;

        double stableDps;
        long lastStableDpsUpdateMs;
        boolean dead;
        long deathAtMs;
        long deathFreezeUntilMs;

        TrackInfo(EntityLivingBase e) {
            this.id = e.getUniqueID();
            this.name = e.getName();
            this.maxHealth = Math.max(1.0F, e.getMaxHealth());
            this.currentHealth = Math.max(0.0F, e.getHealth());
            this.minHealth = this.currentHealth;
            long now = System.currentTimeMillis();
            this.combatStartMs = now;
            this.lastDamageLikeMs = now;
            this.paused = false;
            this.pausedAccumulatedMs = 0L;
            this.pausedStartMs = 0L;
            this.stableDps = 0.0;
            this.lastStableDpsUpdateMs = now;
            this.dead = false;
            this.deathAtMs = 0L;
            this.deathFreezeUntilMs = 0L;
        }

        double getActiveSeconds(long now) {
            long pausedNow = pausedAccumulatedMs;
            if (paused) {
                pausedNow += Math.max(0L, now - pausedStartMs);
            }
            long activeMs = Math.max(1L, now - combatStartMs - pausedNow);
            return activeMs / 1000.0;
        }
    }

    private static class ConfigData {
        boolean enabled = false;
        int panelX = 0;
        int panelY = 22;
        int panelWidth = 219;
        int panelHeight = 100;
        int panelAlpha = 100;
        int combatTimeoutSeconds = 3;
        int disengageRemoveSeconds = 3;
        int stableDpsIntervalSeconds = 2;
        int deathDataMode = DEATH_MODE_CHAT;
        int deathPanelHoldSeconds = 3;
    }

    private KillTimerHandler() {
    }

    public static void loadConfig() {
        Path file = ProfileManager.getCurrentProfileDir().resolve("kill_timer_config.json");
        if (!Files.exists(file))
            return;
        try (BufferedReader r = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            ConfigData d = GSON.fromJson(r, ConfigData.class);
            if (d == null)
                return;
            isEnabled = d.enabled;
            panelX = Math.max(0, d.panelX);
            panelY = Math.max(0, d.panelY);
            panelWidth = Math.max(MIN_W, d.panelWidth);
            panelHeight = Math.max(MIN_H, d.panelHeight);
            panelAlpha = MathHelper.clamp(d.panelAlpha, 30, 240);
            combatTimeoutSeconds = Math.max(1, d.combatTimeoutSeconds);
            disengageRemoveSeconds = Math.max(1, d.disengageRemoveSeconds);
            stableDpsIntervalSeconds = Math.max(1, d.stableDpsIntervalSeconds);
            deathDataMode = (d.deathDataMode == DEATH_MODE_PANEL_HOLD) ? DEATH_MODE_PANEL_HOLD : DEATH_MODE_CHAT;
            deathPanelHoldSeconds = Math.max(1, d.deathPanelHoldSeconds);
        } catch (Exception ignore) {
        }
    }

    public static void saveConfig() {
        Path file = ProfileManager.getCurrentProfileDir().resolve("kill_timer_config.json");
        try {
            Files.createDirectories(file.getParent());
            try (BufferedWriter w = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
                ConfigData d = new ConfigData();
                d.enabled = isEnabled;
                d.panelX = panelX;
                d.panelY = panelY;
                d.panelWidth = panelWidth;
                d.panelHeight = panelHeight;
                d.panelAlpha = panelAlpha;
                d.combatTimeoutSeconds = combatTimeoutSeconds;
                d.disengageRemoveSeconds = disengageRemoveSeconds;
                d.stableDpsIntervalSeconds = stableDpsIntervalSeconds;
                d.deathDataMode = deathDataMode;
                d.deathPanelHoldSeconds = deathPanelHoldSeconds;
                GSON.toJson(d, w);
            }
        } catch (Exception ignore) {
        }
    }

    public static void toggleEnabled() {
        isEnabled = !isEnabled;
        if (!isEnabled)
            clearRuntimeState();
        if (mc.player != null) {
            mc.player.sendMessage(new TextComponentString(I18n.format("msg.kill_timer.toggle",
                    isEnabled ? I18n.format("msg.kill_timer.state_on") : I18n.format("msg.kill_timer.state_off"))));
        }
        saveConfig();
    }

    public static void clearRuntimeState() {
        tracked.clear();
        selectedIndex = -1;
        scrollOffset = 0;
        dragging = false;
        resizing = false;
        freeEditMode = false;
        pendingMouseGrabAfterEdit = false;
        escKeyWasDown = false;
    }

    public static void enterFreeEditMode() {
        freeEditMode = true;
        ModConfig.isMouseDetached = true;
        pendingMouseGrabAfterEdit = false;
        escKeyWasDown = false;
        // 预埋一次 GUI 拦截：用于拦截用户按 ESC 可能触发的菜单
        GuiBlockerHandler.blockNextGui(1);
    }

    public static void exitFreeEditMode() {
        if (!freeEditMode)
            return;
        freeEditMode = false;
        dragging = false;
        resizing = false;
        ModConfig.isMouseDetached = false;
        if (mc.currentScreen == null) {
            mc.mouseHelper.grabMouseCursor();
            pendingMouseGrabAfterEdit = false;
        } else {
            pendingMouseGrabAfterEdit = true;
        }
        saveConfig();
        if (mc.player != null) {
            mc.player.sendMessage(new TextComponentString(I18n.format("msg.kill_timer.exit_free_edit")));
        }
    }

    @SubscribeEvent
    public void onMonsterDeath(LivingDeathEvent event) {
        if (!isEnabled || mc.world == null || !mc.world.isRemote)
            return;
        EntityLivingBase dead = event.getEntityLiving();
        if (dead == null)
            return;

        TrackInfo info = tracked.get(dead.getUniqueID());
        if (info != null && mc.player != null) {
            long now = System.currentTimeMillis();
            info.currentHealth = 0.0F;
            info.dead = true;
            info.deathAtMs = now;
            info.deathFreezeUntilMs = now + Math.max(1, deathPanelHoldSeconds) * 1000L;
            // 死亡定格：冻结时长与DPS计算基准
            if (!info.paused) {
                info.paused = true;
                info.pausedStartMs = now;
            }

            double sec = info.getActiveSeconds(now);
            double totalDmg = Math.max(0.0, info.maxHealth - info.minHealth);
            double dps = totalDmg / Math.max(0.001, sec);

            if (deathDataMode == DEATH_MODE_CHAT) {
                tracked.remove(dead.getUniqueID());
                mc.player.sendMessage(new TextComponentString(I18n.format(
                        "msg.kill_timer.kill_summary",
                        info.name, sec, formatCompactNumber(totalDmg), formatCompactNumber(dps))));
            }
        }
    }

    @SubscribeEvent
    public void onRenderOverlay(RenderGameOverlayEvent.Post event) {
        if (!isEnabled || event.getType() != RenderGameOverlayEvent.ElementType.ALL || mc.player == null
                || mc.world == null) {
            return;
        }

        if (freeEditMode) {
            boolean escDown = SimulatedKeyInputManager.isKeyDown(Keyboard.KEY_ESCAPE);
            if (escDown && !escKeyWasDown) {
                // 自由编辑模式下，ESC 用于退出编辑，并回归鼠标
                exitFreeEditMode();
            }
            escKeyWasDown = escDown;
        } else {
            escKeyWasDown = false;
        }

        if (pendingMouseGrabAfterEdit && mc.currentScreen == null && !ModConfig.isMouseDetached) {
            mc.mouseHelper.grabMouseCursor();
            pendingMouseGrabAfterEdit = false;
        }

        if (freeEditMode && mc.currentScreen != null) {
            exitFreeEditMode();
        }

        updateFromWorld();
        handleMouseInteractions();
        renderPanel();
    }

    private static void updateFromWorld() {
        long now = System.currentTimeMillis();

        for (Object obj : new ArrayList<>(mc.world.loadedEntityList)) {
            if (!(obj instanceof EntityLivingBase))
                continue;
            EntityLivingBase e = (EntityLivingBase) obj;
            if (!(e instanceof IMob))
                continue;
            UUID id = e.getUniqueID();

            if (!e.isEntityAlive()) {
                TrackInfo t = tracked.get(id);
                if (t != null && t.dead && now <= t.deathFreezeUntilMs) {
                    t.currentHealth = 0.0F;
                } else {
                    tracked.remove(id);
                }
                continue;
            }

            float maxHp = Math.max(1.0F, e.getMaxHealth());
            float hp = Math.max(0.0F, e.getHealth());
            TrackInfo t = tracked.get(id);
            if (t == null) {
                // 首次进入追踪：需要已受伤，避免把满血路过怪物都加进面板
                if (hp < maxHp) {
                    t = new TrackInfo(e);
                    tracked.put(id, t);
                }
                continue;
            }

            t.dead = false;
            t.deathAtMs = 0L;
            t.deathFreezeUntilMs = 0L;
            t.name = e.getName();
            t.maxHealth = Math.max(t.maxHealth, maxHp);

            float prevHp = t.currentHealth;
            boolean tookDamageNow = hp < prevHp - 0.0001F;

            t.currentHealth = hp;
            t.minHealth = Math.min(t.minHealth, hp);

            // 只在“真实掉血”时刷新战斗时间；仅受击动画但不掉血不算战斗中
            if (tookDamageNow) {
                t.lastDamageLikeMs = now;
                if (t.paused) {
                    t.pausedAccumulatedMs += Math.max(0L, now - t.pausedStartMs);
                    t.pausedStartMs = 0L;
                    t.paused = false;
                }
            } else {
                if (!t.paused && now - t.lastDamageLikeMs >= combatTimeoutSeconds * 1000L) {
                    t.paused = true;
                    t.pausedStartMs = now;
                }
                if (now - t.lastDamageLikeMs >= (combatTimeoutSeconds + disengageRemoveSeconds) * 1000L) {
                    tracked.remove(id);
                }
            }
        }

        // 清理死亡后定格超时的数据
        tracked.entrySet().removeIf(entry -> {
            TrackInfo t = entry.getValue();
            return t != null && t.dead && now > t.deathFreezeUntilMs;
        });

        // 稳定DPS刷新
        for (TrackInfo t : tracked.values()) {
            if (t.paused || t.dead)
                continue;
            if (now - t.lastStableDpsUpdateMs >= stableDpsIntervalSeconds * 1000L) {
                double sec = t.getActiveSeconds(now);
                double totalDmg = Math.max(0.0, t.maxHealth - t.currentHealth);
                t.stableDps = totalDmg / Math.max(0.001, sec);
                t.lastStableDpsUpdateMs = now;
            }
        }
    }

    private static void handleMouseInteractions() {
        if (mc.currentScreen != null) {
            dragging = false;
            resizing = false;
            return;
        }

        ScaledResolution sr = new ScaledResolution(mc);
        int sw = sr.getScaledWidth();
        int sh = sr.getScaledHeight();
        int mouseX = Mouse.getX() * sw / mc.displayWidth;
        int mouseY = sh - Mouse.getY() * sh / mc.displayHeight - 1;
        boolean left = Mouse.isButtonDown(0);

        boolean inPanel = inside(mouseX, mouseY, panelX, panelY, panelWidth, panelHeight);
        boolean inResize = inside(mouseX, mouseY, panelX + panelWidth - 10, panelY + panelHeight - 10, 10, 10);

        boolean shift = SimulatedKeyInputManager.isEitherKeyDown(Keyboard.KEY_LSHIFT, Keyboard.KEY_RSHIFT);
        boolean ctrl = SimulatedKeyInputManager.isEitherKeyDown(Keyboard.KEY_LCONTROL, Keyboard.KEY_RCONTROL);
        boolean canDrag = freeEditMode || shift;
        boolean canResize = freeEditMode || ctrl;

        if (left) {
            if (!dragging && !resizing) {
                if (canResize && inResize) {
                    resizing = true;
                    resizeStartX = mouseX;
                    resizeStartY = mouseY;
                    resizeStartW = panelWidth;
                    resizeStartH = panelHeight;
                } else if (canDrag && inPanel) {
                    dragging = true;
                    dragOffsetX = mouseX - panelX;
                    dragOffsetY = mouseY - panelY;
                }
            }

            if (dragging) {
                panelX = Math.max(0, mouseX - dragOffsetX);
                panelY = Math.max(0, mouseY - dragOffsetY);
            }
            if (resizing) {
                panelWidth = Math.max(MIN_W, resizeStartW + (mouseX - resizeStartX));
                panelHeight = Math.max(MIN_H, resizeStartH + (mouseY - resizeStartY));
            }
        } else {
            if (dragging || resizing) {
                saveConfig();
            }
            dragging = false;
            resizing = false;
        }
    }

    private static void renderPanel() {
        int x = panelX;
        int y = panelY;
        int w = panelWidth;
        int h = panelHeight;

        int bg = ((panelAlpha & 0xFF) << 24) | 0x101820;
        Gui.drawRect(x, y, x + w, y + h, bg);
        Gui.drawRect(x, y, x + w, y + HEADER_H, 0xCC1E4A69);
        Gui.drawRect(x, y, x + w, y + 1, 0xFF80D4FF);
        Gui.drawRect(x, y + h - 1, x + w, y + h, 0xFF304050);
        Gui.drawRect(x, y, x + 1, y + h, 0xFF304050);
        Gui.drawRect(x + w - 1, y, x + w, y + h, 0xFF304050);

        mc.fontRenderer.drawStringWithShadow(I18n.format("gui.kill_timer.panel_title"), x + 6, y + 5, 0xFFFFFF);
        mc.fontRenderer.drawStringWithShadow(I18n.format("gui.kill_timer.target_count", tracked.size()), x + w - 68,
                y + 5, 0xFFFFFF);

        List<TrackInfo> rows = new ArrayList<>(tracked.values());
        rows.sort(Comparator.comparingLong(a -> a.combatStartMs));

        int listX = x + PADDING;
        int listY = y + HEADER_H + 4;
        int listW = w - PADDING * 2 - 6;
        int listH = h - HEADER_H - FOOTER_H - 6;

        int totalRowsHeight = rows.size() * (CARD_H + CARD_GAP);
        int maxScroll = Math.max(0, totalRowsHeight - listH);
        scrollOffset = MathHelper.clamp(scrollOffset, 0, maxScroll);

        int mouseX = getScaledMouseX();
        int mouseY = getScaledMouseY();

        // 选择卡片
        if (Mouse.isButtonDown(0) && mc.currentScreen == null) {
            int localY = mouseY - listY + scrollOffset;
            if (inside(mouseX, mouseY, listX, listY, listW, listH)) {
                int idx = localY / (CARD_H + CARD_GAP);
                if (idx >= 0 && idx < rows.size()) {
                    selectedIndex = idx;
                }
            }
        }

        // 滚轮
        int wheel = Mouse.getDWheel();
        if (wheel != 0 && inside(mouseX, mouseY, listX, listY, listW, listH)) {
            scrollOffset = MathHelper.clamp(scrollOffset - (wheel / 120) * (CARD_H + CARD_GAP), 0, maxScroll);
        }

        int drawY = listY - scrollOffset;
        for (int i = 0; i < rows.size(); i++) {
            TrackInfo t = rows.get(i);
            int cy = drawY + i * (CARD_H + CARD_GAP);
            if (cy + CARD_H < listY || cy > listY + listH)
                continue;

            boolean selected = (i == selectedIndex);
            int cardBg = selected ? 0xAA2E5F87 : 0x99304050;
            Gui.drawRect(listX, cy, listX + listW, cy + CARD_H, cardBg);

            long calcNow = t.dead ? t.deathAtMs : System.currentTimeMillis();
            double sec = t.getActiveSeconds(calcNow);
            double totalDmg = Math.max(0.0, t.maxHealth - t.currentHealth);
            double dpsLive = totalDmg / Math.max(0.001, sec);

            String name = fitText("§e" + t.name, listW - 10);
            String hpLine = String.format("§fHP: §c%s§7/§a%s",
                    formatCompactNumber(t.currentHealth), formatCompactNumber(t.maxHealth));
            String dpsLine = String.format("§fDPS: §d%s §7| 固定: §b%s",
                    formatCompactNumber(dpsLive), formatCompactNumber(t.stableDps));
            String statusText;
            if (t.dead) {
                long remainMs = Math.max(0L, t.deathFreezeUntilMs - System.currentTimeMillis());
                statusText = I18n.format("gui.kill_timer.status_killed", trimTrailingZeros(remainMs / 1000.0));
            } else {
                if (t.paused) {
                    long sinceLastDamageMs = Math.max(0L, System.currentTimeMillis() - t.lastDamageLikeMs);
                    long removeAtMs = Math.max(0L,
                            (combatTimeoutSeconds + disengageRemoveSeconds) * 1000L - sinceLastDamageMs);
                    statusText = I18n.format("gui.kill_timer.status_disengaged",
                            trimTrailingZeros(removeAtMs / 1000.0));
                } else {
                    statusText = I18n.format("gui.kill_timer.status_fighting");
                }
            }
            String tLine = I18n.format("gui.kill_timer.time_status", sec, statusText);

            // 文字超出时自动裁剪
            hpLine = fitText(hpLine, listW - 10);
            dpsLine = fitText(dpsLine, listW - 10);
            tLine = fitText(tLine, listW - 10);

            mc.fontRenderer.drawStringWithShadow(name, listX + 4, cy + 2, 0xFFFFFF);
            mc.fontRenderer.drawStringWithShadow(hpLine, listX + 4, cy + 11, 0xFFFFFF);
            mc.fontRenderer.drawStringWithShadow(dpsLine, listX + listW / 2, cy + 2, 0xFFFFFF);
            mc.fontRenderer.drawStringWithShadow(tLine, listX + listW / 2, cy + 11, 0xFFFFFF);
        }

        // 滚动条
        if (maxScroll > 0) {
            int sbX = x + w - 5;
            int sbY = listY;
            int sbH = listH;
            Gui.drawRect(sbX, sbY, sbX + 4, sbY + sbH, 0xFF1A1A1A);
            int thumbH = Math.max(12, (int) ((float) listH / Math.max(listH, totalRowsHeight) * sbH));
            int thumbY = sbY + (int) ((float) scrollOffset / maxScroll * (sbH - thumbH));
            Gui.drawRect(sbX, thumbY, sbX + 4, thumbY + thumbH, 0xFF8AAED0);
        }

        // 选中后底部删除按钮
        if (selectedIndex >= 0 && selectedIndex < rows.size()) {
            int bx = x + w - 64;
            int by = y + h - FOOTER_H + 1;
            Gui.drawRect(bx, by, bx + 56, by + 14, 0xAA882222);
            mc.fontRenderer.drawStringWithShadow(I18n.format("gui.kill_timer.delete"), bx + 18, by + 3, 0xFFFFFF);

            if (Mouse.isButtonDown(0) && inside(mouseX, mouseY, bx, by, 56, 14)) {
                TrackInfo target = rows.get(selectedIndex);
                tracked.remove(target.id);
                selectedIndex = -1;
            }
        }

        // resize 角标
        Gui.drawRect(x + w - 8, y + h - 8, x + w - 2, y + h - 2, 0xCC66CCFF);
    }

    private static String fitText(String text, int maxWidth) {
        if (mc.fontRenderer.getStringWidth(text) <= maxWidth) {
            return text;
        }
        String raw = text;
        while (raw.length() > 2 && mc.fontRenderer.getStringWidth(raw + "§7…") > maxWidth) {
            raw = raw.substring(0, raw.length() - 1);
        }
        return raw + "§7…";
    }

    private static int getScaledMouseX() {
        ScaledResolution sr = new ScaledResolution(mc);
        return Mouse.getX() * sr.getScaledWidth() / mc.displayWidth;
    }

    private static int getScaledMouseY() {
        ScaledResolution sr = new ScaledResolution(mc);
        return sr.getScaledHeight() - Mouse.getY() * sr.getScaledHeight() / mc.displayHeight - 1;
    }

    private static boolean inside(int mx, int my, int x, int y, int w, int h) {
        return mx >= x && mx <= x + w && my >= y && my <= y + h;
    }

    private static String formatCompactNumber(double value) {
        double abs = Math.abs(value);
        if (abs >= 1_000_000_000_000D) {
            return trimTrailingZeros(value / 1_000_000_000_000D) + "T";
        }
        if (abs >= 1_000_000_000D) {
            return trimTrailingZeros(value / 1_000_000_000D) + "B";
        }
        if (abs >= 1_000_000D) {
            return trimTrailingZeros(value / 1_000_000D) + "M";
        }
        if (abs >= 1_000D) {
            return trimTrailingZeros(value / 1_000D) + "K";
        }
        if (Math.abs(value - Math.rint(value)) < 0.0001D) {
            return String.valueOf((long) Math.rint(value));
        }
        return trimTrailingZeros(value);
    }

    private static String trimTrailingZeros(double value) {
        String s = String.format(Locale.US, "%.2f", value);
        if (s.indexOf('.') >= 0) {
            s = s.replaceAll("0+$", "").replaceAll("\\.$", "");
        }
        return s;
    }
}

