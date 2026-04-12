// (这是一个全新的文件)
package com.zszl.zszlScriptMod.utils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.zszl.zszlScriptMod.zszlScriptMod;
import com.zszl.zszlScriptMod.system.ProfileManager;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.I18n;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;

import java.io.BufferedWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.Date;

public class TerrainScannerHandler {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    /**
     * 扫描以玩家为中心的地形并保存到文件。
     * 
     * @param radius 扫描半径 (立方体范围)
     */
    public static void scanAndSaveTerrain(int radius) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.player == null || mc.world == null) {
            if (mc.player != null) {
                mc.player.sendMessage(
                        new TextComponentString(
                                TextFormatting.RED + I18n.format("msg.terrain_scan.player_world_not_loaded")));
            }
            return;
        }

        // 避免把 Entity / EntityPlayerSP 混放到同一个局部变量里。
        // ProGuard 7.8.1 在当前构建链路下会把该局部变量的 StackMapTable
        // 错误降级成 Object，进而在 Java 8 客户端触发 VerifyError。
        final BlockPos center = mc.getRenderViewEntity() != null
                ? mc.getRenderViewEntity().getPosition()
                : mc.player.getPosition();
        final World world = mc.world;
        final int finalRadius = Math.max(1, Math.min(50, radius)); // 半径限制在1-50之间

        mc.player.sendMessage(new TextComponentString(
                TextFormatting.AQUA + I18n.format("msg.terrain_scan.start", finalRadius)));

        // 在新线程中执行扫描，防止游戏卡顿
        new Thread(() -> {
            try {
                JsonObject resultJson = new JsonObject();
                JsonArray blocksArray = new JsonArray();

                // 添加元数据
                resultJson.addProperty("scanTimestamp", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
                resultJson.addProperty("scanRadius", finalRadius);
                JsonObject centerPosJson = new JsonObject();
                centerPosJson.addProperty("x", center.getX());
                centerPosJson.addProperty("y", center.getY());
                centerPosJson.addProperty("z", center.getZ());
                resultJson.add("scanCenterAbsolute", centerPosJson);

                // 执行扫描
                for (int dx = -finalRadius; dx <= finalRadius; dx++) {
                    for (int dy = -finalRadius; dy <= finalRadius; dy++) {
                        for (int dz = -finalRadius; dz <= finalRadius; dz++) {
                            BlockPos currentPos = center.add(dx, dy, dz);
                            IBlockState state = world.getBlockState(currentPos);
                            Block block = state.getBlock();

                            // 忽略空气方块以减小文件大小
                            if (block == Blocks.AIR) {
                                continue;
                            }

                            String blockName = Block.REGISTRY.getNameForObject(block).toString();

                            JsonObject blockJson = new JsonObject();
                            blockJson.addProperty("x", dx); // 相对坐标
                            blockJson.addProperty("y", dy);
                            blockJson.addProperty("z", dz);
                            blockJson.addProperty("block", blockName);
                            // 可选：添加方块元数据
                            // blockJson.addProperty("meta", block.getMetaFromState(state));

                            blocksArray.add(blockJson);
                        }
                    }
                }
                resultJson.add("blocks", blocksArray);

                // 保存文件
                String timestamp = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss").format(new Date());
                Path terrainDir = ProfileManager.getCurrentProfileDir().resolve("terrain");
                Files.createDirectories(terrainDir);
                Path outputPath = terrainDir.resolve(timestamp + ".json");

                try (BufferedWriter writer = Files.newBufferedWriter(outputPath, StandardCharsets.UTF_8)) {
                    GSON.toJson(resultJson, writer);
                }

                // 在主线程中发送成功消息
                mc.addScheduledTask(() -> {
                    mc.player.sendMessage(new TextComponentString(
                            TextFormatting.AQUA + I18n.format("msg.terrain_scan.done", blocksArray.size())));
                    mc.player.sendMessage(new TextComponentString(TextFormatting.GRAY + outputPath.toString()));
                });

            } catch (Exception e) {
                zszlScriptMod.LOGGER.error("Error occurred during terrain scanning", e);
                mc.addScheduledTask(() -> {
                    mc.player.sendMessage(
                            new TextComponentString(TextFormatting.RED + I18n.format("msg.terrain_scan.failed")));
                });
            }
        }).start();
    }
}
