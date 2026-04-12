package com.zszl.zszlScriptMod.handlers;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.zszl.zszlScriptMod.system.ProfileManager;
import net.minecraft.block.BlockShulkerBox;
import net.minecraft.client.Minecraft;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.text.TextComponentString;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class ShulkerMiningReboundFixHandler {

    public static final ShulkerMiningReboundFixHandler INSTANCE = new ShulkerMiningReboundFixHandler();

    private static final Minecraft mc = Minecraft.getMinecraft();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static boolean enabled = false;

    private boolean miningActive = false;
    private BlockPos miningTarget = null;
    private int airborneNudgeCooldown = 0;

    private static final double AIRBORNE_NUDGE_Y = 0.0625D;

    private boolean prevAllowFlying = false;
    private boolean prevIsFlying = false;
    private boolean lastAppliedAllow = false;
    private boolean lastAppliedFlying = false;
    private boolean appliedStateInitialized = false;

    private static class ConfigData {
        boolean enabled = false;
    }

    private ShulkerMiningReboundFixHandler() {
    }

    public static void loadConfig() {
        Path file = ProfileManager.getCurrentProfileDir().resolve("shulker_mining_rebound_fix_config.json");
        if (!Files.exists(file)) {
            return;
        }
        try (BufferedReader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            ConfigData data = GSON.fromJson(reader, ConfigData.class);
            if (data == null) {
                return;
            }
            enabled = data.enabled;
        } catch (Exception ignored) {
        }
    }

    public static void saveConfig() {
        Path file = ProfileManager.getCurrentProfileDir().resolve("shulker_mining_rebound_fix_config.json");
        try {
            Files.createDirectories(file.getParent());
            try (BufferedWriter writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
                ConfigData data = new ConfigData();
                data.enabled = enabled;
                GSON.toJson(data, writer);
            }
        } catch (Exception ignored) {
        }
    }

    public static void toggleEnabled() {
        enabled = !enabled;
        saveConfig();
        if (mc.player != null) {
            mc.player.sendMessage(new TextComponentString(enabled
                    ? "§a[常用] 潜影盒回弹修复已开启"
                    : "§c[常用] 潜影盒回弹修复已关闭"));
        }
        if (!enabled) {
            INSTANCE.stopMiningSession(true);
        }
    }

    public void clearRuntimeState() {
        stopMiningSession(true);
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        if (!enabled || mc.player == null || mc.world == null) {
            if (miningActive) {
                stopMiningSession(true);
            }
            return;
        }

        BlockPos shulkerPos = getCurrentMiningShulkerPos();
        if (shulkerPos != null && !miningActive) {
            startMiningSession(shulkerPos);
        }

        if (!miningActive) {
            return;
        }

        if (!isShulkerAt(miningTarget) || shulkerPos == null) {
            stopMiningSession(true);
            return;
        }

        // 只要在挖潜影盒，持续保持浮空飞行状态
        applyFlightState(true, true);
        ensureAirborneWhileMining();
    }

    private BlockPos getCurrentMiningShulkerPos() {
        if (mc.player == null || mc.world == null || mc.objectMouseOver == null) {
            return null;
        }
        if (!mc.gameSettings.keyBindAttack.isKeyDown()) {
            return null;
        }
        if (mc.objectMouseOver.typeOfHit != RayTraceResult.Type.BLOCK) {
            return null;
        }
        BlockPos pos = mc.objectMouseOver.getBlockPos();
        if (pos == null || !isShulkerAt(pos)) {
            return null;
        }
        return pos;
    }

    private boolean isShulkerAt(BlockPos pos) {
        return pos != null && mc.world.getBlockState(pos).getBlock() instanceof BlockShulkerBox;
    }

    private void startMiningSession(BlockPos target) {
        miningActive = true;
        miningTarget = target;
        airborneNudgeCooldown = 0;

        prevAllowFlying = mc.player.capabilities.allowFlying;
        prevIsFlying = mc.player.capabilities.isFlying;
        appliedStateInitialized = false;
    }

    private void stopMiningSession(boolean restorePrevious) {
        if (restorePrevious && mc.player != null) {
            applyFlightState(prevAllowFlying, prevIsFlying);
        }
        miningActive = false;
        miningTarget = null;
        airborneNudgeCooldown = 0;
    }

    private void ensureAirborneWhileMining() {
        if (mc.player == null) {
            return;
        }

        if (airborneNudgeCooldown > 0) {
            airborneNudgeCooldown--;
        }

        // 核心：必须真的离地，才会进入空中挖掘状态（速度变化）
        if (mc.player.onGround && airborneNudgeCooldown <= 0) {
            mc.player.setPosition(mc.player.posX, mc.player.posY + AIRBORNE_NUDGE_Y, mc.player.posZ);
            mc.player.motionY = 0.0D;
            airborneNudgeCooldown = 2;
        }
    }

    private void applyFlightState(boolean allowFlying, boolean isFlying) {
        if (mc.player == null) {
            return;
        }

        if (appliedStateInitialized && lastAppliedAllow == allowFlying && lastAppliedFlying == isFlying) {
            return;
        }

        mc.player.capabilities.allowFlying = allowFlying;
        mc.player.capabilities.isFlying = isFlying;
        mc.player.sendPlayerAbilities();

        lastAppliedAllow = allowFlying;
        lastAppliedFlying = isFlying;
        appliedStateInitialized = true;
    }
}
