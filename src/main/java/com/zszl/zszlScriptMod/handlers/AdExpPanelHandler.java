package com.zszl.zszlScriptMod.handlers;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.zszl.zszlScriptMod.system.ProfileManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.TextComponentString;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class AdExpPanelHandler {

    public static final AdExpPanelHandler INSTANCE = new AdExpPanelHandler();

    private static final Minecraft mc = Minecraft.getMinecraft();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static boolean enabled = false;
    public static boolean showCurrentValue = true;
    // 当前数值显示模式：false=本级内经验(默认), true=总经验
    public static boolean currentValueUseTotalExp = false;
    public static boolean showCurrentTotalExp = true;
    public static boolean showCurrentLevel = true;
    public static boolean showNextLevel = true;
    public static boolean showProgress = true;
    public static int progressDecimalPlaces = 1;

    private static volatile long currentTotalExp = 0L;

    private static final List<LevelExpEntry> LEVEL_TABLE = new ArrayList<>();

    private static class LevelExpEntry {
        int level;
        long exp;
    }

    private static class ConfigData {
        boolean enabled = false;
        boolean showCurrentValue = true;
        boolean currentValueUseTotalExp = false;
        boolean showCurrentTotalExp = true;
        boolean showCurrentLevel = true;
        boolean showNextLevel = true;
        boolean showProgress = true;
        int progressDecimalPlaces = 1;
    }

    private static class LevelProgress {
        int currentLevel;
        long currentLevelExp;
        int nextLevel;
        long nextLevelExp;
        long needExp;
        double progress;
    }

    private AdExpPanelHandler() {
    }

    public static void loadConfig() {
        loadLevelTableIfNeeded();
        Path file = ProfileManager.getCurrentProfileDir().resolve("ad_exp_panel_config.json");
        if (!Files.exists(file)) {
            return;
        }
        try (BufferedReader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            ConfigData data = GSON.fromJson(reader, ConfigData.class);
            if (data == null) {
                return;
            }
            enabled = data.enabled;
            showCurrentValue = data.showCurrentValue;
            currentValueUseTotalExp = data.currentValueUseTotalExp;
            showCurrentTotalExp = data.showCurrentTotalExp;
            showCurrentLevel = data.showCurrentLevel;
            showNextLevel = data.showNextLevel;
            showProgress = data.showProgress;
            progressDecimalPlaces = MathHelper.clamp(data.progressDecimalPlaces, 0, 4);
        } catch (Exception ignored) {
        }
    }

    public static void saveConfig() {
        Path file = ProfileManager.getCurrentProfileDir().resolve("ad_exp_panel_config.json");
        try {
            Files.createDirectories(file.getParent());
            try (BufferedWriter writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
                ConfigData data = new ConfigData();
                data.enabled = enabled;
                data.showCurrentValue = showCurrentValue;
                data.currentValueUseTotalExp = currentValueUseTotalExp;
                data.showCurrentTotalExp = showCurrentTotalExp;
                data.showCurrentLevel = showCurrentLevel;
                data.showNextLevel = showNextLevel;
                data.showProgress = showProgress;
                data.progressDecimalPlaces = MathHelper.clamp(progressDecimalPlaces, 0, 4);
                GSON.toJson(data, writer);
            }
        } catch (Exception ignored) {
        }
    }

    public static void toggleEnabled() {
        enabled = !enabled;
        saveConfig();
        if (mc.player != null) {
            mc.player.sendMessage(new TextComponentString(
                    I18n.format("msg.ad_exp_panel.toggle",
                            enabled ? I18n.format("msg.kill_timer.state_on") : I18n.format("msg.kill_timer.state_off"))));
        }
    }

    public static void clearRuntimeState() {
        currentTotalExp = 0L;
    }

    public static void onAdventureExpUpdated(long exp) {
        if (exp < 0) {
            return;
        }
        currentTotalExp = exp;
    }

    @SubscribeEvent
    public void onRenderOverlay(RenderGameOverlayEvent.Post event) {
        if (!enabled || event.getType() != RenderGameOverlayEvent.ElementType.ALL || mc.player == null || mc.world == null) {
            return;
        }
        renderPanel();
    }

    private static void renderPanel() {
        loadLevelTableIfNeeded();

        List<String[]> rows = new ArrayList<>();
        LevelProgress p = calculateProgress(currentTotalExp);

        if (showCurrentValue) {
            long currentValue = currentValueUseTotalExp
                    ? currentTotalExp
                    : Math.max(0L, currentTotalExp - p.currentLevelExp);
            rows.add(new String[]{I18n.format("gui.ad_exp_panel.row.current_value"), formatCompactZh(currentValue)});
        }
        if (showCurrentTotalExp) {
            rows.add(new String[]{I18n.format("gui.ad_exp_panel.row.current_total_exp"),
                    formatCompactZh(currentTotalExp) + " (" + formatComma(currentTotalExp) + ")"});
        }
        if (showCurrentLevel) {
            rows.add(new String[]{I18n.format("gui.ad_exp_panel.row.current_level"), "Lv." + p.currentLevel});
        }
        if (showNextLevel) {
            if (p.nextLevel > 0) {
                rows.add(new String[]{I18n.format("gui.ad_exp_panel.row.next_level", p.nextLevel),
                        formatCompactZh(p.nextLevelExp) + "(" + I18n.format("gui.ad_exp_panel.row.need", formatCompactZh(p.needExp)) + ")"});
            } else {
                rows.add(new String[]{I18n.format("gui.ad_exp_panel.row.next_level_max"), I18n.format("gui.ad_exp_panel.max_level")});
            }
        }
        if (showProgress) {
            double percent = p.progress * 100.0;
            rows.add(new String[]{I18n.format("gui.ad_exp_panel.row.progress"), formatPercent(percent, progressDecimalPlaces) + "%"});
        }

        if (rows.isEmpty()) {
            rows.add(new String[]{I18n.format("gui.ad_exp_panel.empty"), "-"});
        }

        ScaledResolution sr = new ScaledResolution(mc);
        int x = 8;
        int lineHeight = 12;
        int titleHeight = 16;
        int pad = 6;
        int h = pad + titleHeight + rows.size() * lineHeight + pad;
        int y = sr.getScaledHeight() - h - 8;
        int w = 240;

        Gui.drawRect(x, y, x + w, y + h, 0xA0000000);
        Gui.drawRect(x, y, x + w, y + 1, 0xFF5A6A80);
        Gui.drawRect(x, y + h - 1, x + w, y + h, 0xFF5A6A80);

        mc.fontRenderer.drawStringWithShadow(I18n.format("gui.ad_exp_panel.title"), x + pad, y + 4, 0xFFFFFF);

        int labelX = x + pad;
        int valueX = x + 116;
        int rowY = y + pad + titleHeight;
        for (String[] row : rows) {
            mc.fontRenderer.drawStringWithShadow(row[0], labelX, rowY, 0xE0E0E0);
            mc.fontRenderer.drawStringWithShadow(row[1], valueX, rowY, 0xFFFFFF);
            rowY += lineHeight;
        }
    }

    private static LevelProgress calculateProgress(long totalExp) {
        LevelProgress result = new LevelProgress();
        if (LEVEL_TABLE.isEmpty()) {
            result.currentLevel = 0;
            result.currentLevelExp = 0;
            result.nextLevel = 0;
            result.nextLevelExp = 0;
            result.needExp = 0;
            result.progress = 0;
            return result;
        }

        LevelExpEntry first = LEVEL_TABLE.get(0);
        if (totalExp < first.exp) {
            result.currentLevel = Math.max(0, first.level - 1);
            result.currentLevelExp = 0;
            result.nextLevel = first.level;
            result.nextLevelExp = first.exp;
            result.needExp = Math.max(0L, first.exp - totalExp);
            result.progress = first.exp <= 0 ? 0.0 : MathHelper.clamp(totalExp / (double) first.exp, 0.0, 1.0);
            return result;
        }

        LevelExpEntry current = first;
        LevelExpEntry next = null;
        for (int i = 0; i < LEVEL_TABLE.size(); i++) {
            LevelExpEntry e = LEVEL_TABLE.get(i);
            if (totalExp >= e.exp) {
                current = e;
            } else {
                next = e;
                break;
            }
        }

        result.currentLevel = current.level;
        result.currentLevelExp = current.exp;

        if (next == null) {
            result.nextLevel = 0;
            result.nextLevelExp = 0;
            result.needExp = 0;
            result.progress = 1.0;
        } else {
            result.nextLevel = next.level;
            result.nextLevelExp = next.exp;
            result.needExp = Math.max(0L, next.exp - totalExp);
            long denom = Math.max(1L, next.exp - current.exp);
            result.progress = MathHelper.clamp((totalExp - current.exp) / (double) denom, 0.0, 1.0);
        }
        return result;
    }

    private static synchronized void loadLevelTableIfNeeded() {
        if (!LEVEL_TABLE.isEmpty()) {
            return;
        }
        try (InputStream stream = AdExpPanelHandler.class.getResourceAsStream("/Ad_exp_list.json")) {
            if (stream == null) {
                return;
            }
            try (InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                Type listType = new TypeToken<List<LevelExpEntry>>() {}.getType();
                List<LevelExpEntry> list = GSON.fromJson(reader, listType);
                if (list != null) {
                    list.sort(Comparator.comparingInt(a -> a.level));
                    LEVEL_TABLE.addAll(list);
                }
            }
        } catch (Exception ignored) {
        }
    }

    private static String formatComma(long value) {
        return String.format(Locale.US, "%,d", value);
    }

    private static String formatCompactZh(long value) {
        if (value >= 100000000L) {
            return trimTrailingZeros(String.format(Locale.US, "%.2f", value / 100000000.0)) + "亿";
        }
        if (value >= 10000L) {
            return trimTrailingZeros(String.format(Locale.US, "%.2f", value / 10000.0)) + "万";
        }
        return formatComma(value);
    }

    private static String formatPercent(double percent, int decimals) {
        int d = MathHelper.clamp(decimals, 0, 4);
        return String.format(Locale.US, "%." + d + "f", percent);
    }

    private static String trimTrailingZeros(String text) {
        if (text == null || text.indexOf('.') < 0) {
            return text;
        }
        int end = text.length();
        while (end > 0 && text.charAt(end - 1) == '0') {
            end--;
        }
        if (end > 0 && text.charAt(end - 1) == '.') {
            end--;
        }
        return text.substring(0, Math.max(1, end));
    }
}
