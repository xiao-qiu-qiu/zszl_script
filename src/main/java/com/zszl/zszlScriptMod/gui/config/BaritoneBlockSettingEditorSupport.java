package com.zszl.zszlScriptMod.gui.config;

import com.zszl.zszlScriptMod.shadowbaritone.api.utils.BlockUtils;
import com.zszl.zszlScriptMod.shadowbaritone.api.utils.BlockDisplayLookup;
import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;

final class BaritoneBlockSettingEditorSupport {

    private BaritoneBlockSettingEditorSupport() {
    }

    static String normalizeBlockId(String raw) {
        String token = raw == null ? "" : raw.trim().toLowerCase(Locale.ROOT);
        if (token.isEmpty()) {
            return "";
        }
        Block block = BlockDisplayLookup.findBlockByUserInput(token);
        return block == null ? "" : BlockUtils.blockToString(block);
    }

    static List<String> copyNormalizedBlockIds(Collection<String> blockIds) {
        LinkedHashSet<String> deduped = new LinkedHashSet<String>();
        if (blockIds != null) {
            for (String blockId : blockIds) {
                String normalized = normalizeBlockId(blockId);
                if (!normalized.isEmpty()) {
                    deduped.add(normalized);
                }
            }
        }
        return new ArrayList<String>(deduped);
    }

    static List<String> parseBlockListValue(String raw) {
        String normalized = unwrapCollection(raw);
        if (normalized.isEmpty()) {
            return new ArrayList<String>();
        }
        String[] tokens = normalized.split(",");
        List<String> parsed = new ArrayList<String>(tokens.length);
        for (String token : tokens) {
            String blockId = normalizeBlockId(token);
            if (!blockId.isEmpty()) {
                parsed.add(blockId);
            }
        }
        return copyNormalizedBlockIds(parsed);
    }

    static String serializeBlockListValue(Collection<String> blockIds) {
        return String.join(",", copyNormalizedBlockIds(blockIds));
    }

    static LinkedHashMap<String, List<String>> parseBlockMapValue(String raw) {
        LinkedHashMap<String, List<String>> mappings = new LinkedHashMap<String, List<String>>();
        String normalized = unwrapCollection(raw);
        if (normalized.isEmpty()) {
            return mappings;
        }
        String[] entries = normalized.split(",(?=[^,]*->)");
        for (String entry : entries) {
            String[] pair = entry.split("->", 2);
            if (pair.length < 2) {
                continue;
            }
            String sourceBlockId = normalizeBlockId(pair[0]);
            List<String> targetBlockIds = parseBlockListValue(pair[1]);
            if (sourceBlockId.isEmpty() || targetBlockIds.isEmpty()) {
                continue;
            }
            mappings.put(sourceBlockId, targetBlockIds);
        }
        return mappings;
    }

    static LinkedHashMap<String, List<String>> copyNormalizedBlockMap(Map<String, List<String>> mappings) {
        LinkedHashMap<String, List<String>> normalized = new LinkedHashMap<String, List<String>>();
        if (mappings != null) {
            for (Map.Entry<String, List<String>> entry : mappings.entrySet()) {
                String sourceBlockId = normalizeBlockId(entry.getKey());
                List<String> targetBlockIds = copyNormalizedBlockIds(entry.getValue());
                if (sourceBlockId.isEmpty() || targetBlockIds.isEmpty()) {
                    continue;
                }
                normalized.put(sourceBlockId, targetBlockIds);
            }
        }
        return normalized;
    }

    static String serializeBlockMapValue(Map<String, List<String>> mappings) {
        LinkedHashMap<String, List<String>> normalized = copyNormalizedBlockMap(mappings);
        List<String> entries = new ArrayList<String>(normalized.size());
        for (Map.Entry<String, List<String>> entry : normalized.entrySet()) {
            String targetValue = serializeBlockListValue(entry.getValue());
            if (!targetValue.isEmpty()) {
                entries.add(entry.getKey() + "->" + targetValue);
            }
        }
        return String.join(",", entries);
    }

    static ItemStack getBlockStack(Block block) {
        if (block == null || block == Blocks.AIR) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = new ItemStack(block);
        return stack == null ? ItemStack.EMPTY : stack;
    }

    static String getDisplayName(String blockId) {
        Block block = resolveBlock(blockId);
        ItemStack stack = getBlockStack(block);
        if (!stack.isEmpty()) {
            return stack.getDisplayName();
        }
        return blockId == null ? "" : blockId;
    }

    static Block resolveBlock(String blockId) {
        return normalizeBlockId(blockId).isEmpty() ? null : BlockUtils.stringToBlockNullable(normalizeBlockId(blockId));
    }

    private static String unwrapCollection(String raw) {
        String normalized = raw == null ? "" : raw.trim();
        if ((normalized.startsWith("[") && normalized.endsWith("]"))
                || (normalized.startsWith("{") && normalized.endsWith("}"))) {
            normalized = normalized.substring(1, normalized.length() - 1).trim();
        }
        return normalized;
    }
}
