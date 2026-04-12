package com.zszl.zszlScriptMod.utils;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.zszl.zszlScriptMod.config.ModConfig;
import com.zszl.zszlScriptMod.zszlScriptMod;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.JsonToNBT;
import net.minecraft.nbt.NBTException;
import net.minecraft.nbt.NBTTagCompound;

public class MerchantExchangeManager {

    private static final String RESOURCE_NAME = "merchant_exchange.json";
    @SuppressWarnings("unused")
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static volatile List<MerchantDef> merchants = new ArrayList<>();

    public static class ExchangeDef {
        public final ItemStack leftItem;
        public final ItemStack middleItem;
        public final ItemStack rightItem;
        public final ItemStack resultItem;

        public ExchangeDef(ItemStack leftItem, ItemStack middleItem, ItemStack rightItem, ItemStack resultItem) {
            this.leftItem = leftItem == null ? ItemStack.EMPTY : leftItem;
            this.middleItem = middleItem == null ? ItemStack.EMPTY : middleItem;
            this.rightItem = rightItem == null ? ItemStack.EMPTY : rightItem;
            this.resultItem = resultItem == null ? ItemStack.EMPTY : resultItem;
        }
    }

    public static class MerchantDef {
        public final String id;
        public final String name;
        public final List<ExchangeDef> exchanges;
        public final List<CategoryDef> categories;

        public MerchantDef(String id, String name, List<ExchangeDef> exchanges, List<CategoryDef> categories) {
            this.id = id == null ? "" : id;
            this.name = name == null || name.trim().isEmpty() ? "未命名商人" : name;
            this.exchanges = exchanges == null ? new ArrayList<>() : exchanges;
            this.categories = categories == null ? new ArrayList<>() : categories;
        }
    }

    public static class CategoryDef {
        public final String name;
        public final int startIndex;
        public final int endIndex;

        public CategoryDef(String name, int startIndex, int endIndex) {
            this.name = name == null ? "" : name;
            this.startIndex = Math.max(0, startIndex);
            this.endIndex = Math.max(this.startIndex, endIndex);
        }
    }

    public static void reload() {
        List<MerchantDef> loaded = new ArrayList<>();
        Path devPath = Paths.get("src", "main", "resources", RESOURCE_NAME);
        List<Path> candidates = new ArrayList<>();
        candidates.add(devPath);
        if (ModConfig.CONFIG_DIR != null && !ModConfig.CONFIG_DIR.trim().isEmpty()) {
            candidates.add(Paths.get(ModConfig.CONFIG_DIR, RESOURCE_NAME));
        }

        // 1) 先加载内置（或jar）作为基础
        InputStream in = MerchantExchangeManager.class.getClassLoader().getResourceAsStream(RESOURCE_NAME);
        if (in == null) {
            zszlScriptMod.LOGGER.warn("[MerchantExchangeManager] 未找到资源文件: {}", RESOURCE_NAME);
            merchants = loaded;
        } else {
            try (InputStream autoClose = in;
                    Reader reader = new InputStreamReader(autoClose, StandardCharsets.UTF_8)) {
                loaded = parseMerchants(reader);
            } catch (IOException | RuntimeException e) {
                zszlScriptMod.LOGGER.error("[MerchantExchangeManager] 读取兑换商配置失败", e);
            }
        }

        // 2) 再加载外部配置，作为补充并自动去重合并
        for (Path candidate : candidates) {
            try {
                if (!Files.exists(candidate) || !Files.isRegularFile(candidate)) {
                    continue;
                }
                try (Reader reader = Files.newBufferedReader(candidate, StandardCharsets.UTF_8)) {
                    List<MerchantDef> extra = parseMerchants(reader);
                    loaded = mergeMerchants(loaded, extra);
                    zszlScriptMod.LOGGER.info("[MerchantExchangeManager] 已合并外部文件: {}", candidate.toAbsolutePath());
                }
            } catch (IOException | RuntimeException e) {
                zszlScriptMod.LOGGER.warn("[MerchantExchangeManager] 合并外部文件失败: {}", candidate.toAbsolutePath(), e);
            }
        }

        merchants = loaded;
    }

    public static List<MerchantDef> getMerchants() {
        return merchants == null ? Collections.emptyList() : merchants;
    }

    private static List<MerchantDef> parseMerchants(Reader reader) {
        List<MerchantDef> loaded = new ArrayList<>();
        JsonElement rootEl = new JsonParser().parse(reader);
        if (!rootEl.isJsonObject()) {
            return loaded;
        }

        JsonObject root = rootEl.getAsJsonObject();
        JsonArray merchantArray = root.has("merchants") && root.get("merchants").isJsonArray()
                ? root.getAsJsonArray("merchants")
                : new JsonArray();

        for (JsonElement merchantEl : merchantArray) {
            if (!merchantEl.isJsonObject()) {
                continue;
            }
            JsonObject merchantObj = merchantEl.getAsJsonObject();
            String id = merchantObj.has("id") ? merchantObj.get("id").getAsString() : "";
            String name = merchantObj.has("name") ? merchantObj.get("name").getAsString() : "未命名商人";

            List<ExchangeDef> exchanges = new ArrayList<>();
            JsonArray exchangeArray = merchantObj.has("exchanges") && merchantObj.get("exchanges").isJsonArray()
                    ? merchantObj.getAsJsonArray("exchanges")
                    : new JsonArray();

            for (JsonElement exchangeEl : exchangeArray) {
                if (!exchangeEl.isJsonObject()) {
                    continue;
                }
                JsonObject exchangeObj = exchangeEl.getAsJsonObject();
                ItemStack left = parseItem(exchangeObj.has("left") && exchangeObj.get("left").isJsonObject()
                        ? exchangeObj.getAsJsonObject("left")
                        : null);
                ItemStack middle = parseItem(exchangeObj.has("middle") && exchangeObj.get("middle").isJsonObject()
                        ? exchangeObj.getAsJsonObject("middle")
                        : null);
                ItemStack right = parseItem(exchangeObj.has("right") && exchangeObj.get("right").isJsonObject()
                        ? exchangeObj.getAsJsonObject("right")
                        : null);
                // 兼容旧格式：left + right = result
                if (middle.isEmpty() && !right.isEmpty()) {
                    middle = right;
                    right = ItemStack.EMPTY;
                }
                ItemStack result = parseItem(exchangeObj.has("result") && exchangeObj.get("result").isJsonObject()
                        ? exchangeObj.getAsJsonObject("result")
                        : null);
                exchanges.add(new ExchangeDef(left, middle, right, result));
            }

            List<CategoryDef> categories = parseCategories(merchantObj, exchanges.size());
            loaded.add(new MerchantDef(id, name, exchanges, categories));
        }

        return loaded;
    }

    private static List<CategoryDef> parseCategories(JsonObject merchantObj, int exchangeSize) {
        List<CategoryDef> categories = new ArrayList<>();
        if (merchantObj == null) {
            return categories;
        }

        JsonArray categoryArray = merchantObj.has("categories") && merchantObj.get("categories").isJsonArray()
                ? merchantObj.getAsJsonArray("categories")
                : new JsonArray();

        for (JsonElement categoryEl : categoryArray) {
            if (!categoryEl.isJsonObject()) {
                continue;
            }
            JsonObject categoryObj = categoryEl.getAsJsonObject();
            String categoryName = categoryObj.has("name") && categoryObj.get("name").isJsonPrimitive()
                    ? String.valueOf(categoryObj.get("name").getAsString())
                    : "";
            if (categoryName.trim().isEmpty()) {
                continue;
            }

            int startIndex;
            int endIndex;
            if (categoryObj.has("startIndex") || categoryObj.has("endIndex")) {
                startIndex = categoryObj.has("startIndex") ? Math.max(0, categoryObj.get("startIndex").getAsInt()) : 0;
                endIndex = categoryObj.has("endIndex") ? Math.max(startIndex, categoryObj.get("endIndex").getAsInt())
                        : Math.max(startIndex, exchangeSize - 1);
            } else {
                int start = categoryObj.has("start") ? Math.max(1, categoryObj.get("start").getAsInt()) : 1;
                startIndex = start - 1;
                if (categoryObj.has("end")) {
                    int end = Math.max(start, categoryObj.get("end").getAsInt());
                    endIndex = end - 1;
                } else if (categoryObj.has("count")) {
                    int count = Math.max(1, categoryObj.get("count").getAsInt());
                    endIndex = startIndex + count - 1;
                } else {
                    endIndex = Math.max(startIndex, exchangeSize - 1);
                }
            }

            if (exchangeSize > 0) {
                startIndex = MathHelperClamp(startIndex, 0, exchangeSize - 1);
                endIndex = MathHelperClamp(endIndex, startIndex, exchangeSize - 1);
            }
            categories.add(new CategoryDef(categoryName, startIndex, endIndex));
        }

        return categories;
    }

    private static int MathHelperClamp(int value, int min, int max) {
        if (value < min) {
            return min;
        }
        if (value > max) {
            return max;
        }
        return value;
    }

    private static ItemStack parseItem(JsonObject itemObj) {
        if (itemObj == null) {
            return ItemStack.EMPTY;
        }

        String id = itemObj.has("id") ? String.valueOf(itemObj.get("id").getAsString()) : "minecraft:air";
        int count = itemObj.has("count") ? Math.max(1, itemObj.get("count").getAsInt()) : 1;
        int meta = itemObj.has("meta") ? Math.max(0, itemObj.get("meta").getAsInt()) : 0;

        Item item = Item.getByNameOrId(id == null ? "minecraft:air" : id);
        if (item == null) {
            return ItemStack.EMPTY;
        }

        ItemStack stack = new ItemStack(item, count, meta);
        if (itemObj.has("nbt") && !itemObj.get("nbt").isJsonNull()) {
            try {
                String nbtRaw;
                if (itemObj.get("nbt").isJsonObject()) {
                    nbtRaw = itemObj.getAsJsonObject("nbt").toString();
                } else {
                    nbtRaw = String.valueOf(itemObj.get("nbt").getAsString());
                }
                NBTTagCompound nbt = JsonToNBT.getTagFromJson(nbtRaw == null ? "{}" : nbtRaw);
                stack.setTagCompound(nbt);
            } catch (NBTException ignored) {
            }
        }

        if (itemObj.has("display") && itemObj.get("display").isJsonPrimitive()) {
            String displayName = String.valueOf(itemObj.get("display").getAsString());
            if (displayName != null) {
                stack.setStackDisplayName(displayName);
            }
        }

        return stack;
    }

    private static List<MerchantDef> mergeMerchants(List<MerchantDef> base, List<MerchantDef> extra) {
        if (base == null || base.isEmpty()) {
            return extra == null ? new ArrayList<>() : new ArrayList<>(extra);
        }
        if (extra == null || extra.isEmpty()) {
            return new ArrayList<>(base);
        }

        Map<String, MerchantDef> merged = new LinkedHashMap<>();
        for (MerchantDef m : base) {
            merged.put(merchantKey(m), copyMerchant(m));
        }

        for (MerchantDef m : extra) {
            String key = merchantKey(m);
            MerchantDef existing = merged.get(key);
            if (existing == null) {
                merged.put(key, copyMerchant(m));
                continue;
            }

            List<ExchangeDef> mergedExchanges = mergeExchanges(existing.exchanges, m.exchanges);
            List<CategoryDef> mergedCategories = mergeCategories(existing.categories, m.categories);
            String mergedId = (existing.id == null || existing.id.isEmpty()) ? m.id : existing.id;
            String mergedName = (existing.name == null || existing.name.trim().isEmpty()) ? m.name : existing.name;
            merged.put(key, new MerchantDef(mergedId, mergedName, mergedExchanges, mergedCategories));
        }

        return new ArrayList<>(merged.values());
    }

    private static MerchantDef copyMerchant(MerchantDef m) {
        if (m == null) {
            return new MerchantDef("", "未命名商人", new ArrayList<>(), new ArrayList<>());
        }
        List<ExchangeDef> exchanges = new ArrayList<>();
        if (m.exchanges != null) {
            for (ExchangeDef ex : m.exchanges) {
                if (ex == null) {
                    continue;
                }
                exchanges.add(new ExchangeDef(
                        copyStack(ex.leftItem),
                        copyStack(ex.middleItem),
                        copyStack(ex.rightItem),
                        copyStack(ex.resultItem)));
            }
        }
        List<CategoryDef> categories = new ArrayList<>();
        if (m.categories != null) {
            for (CategoryDef c : m.categories) {
                if (c == null) {
                    continue;
                }
                categories.add(new CategoryDef(c.name, c.startIndex, c.endIndex));
            }
        }
        return new MerchantDef(m.id, m.name, exchanges, categories);
    }

    private static List<ExchangeDef> mergeExchanges(List<ExchangeDef> a, List<ExchangeDef> b) {
        List<ExchangeDef> out = new ArrayList<>();
        Set<String> seen = new HashSet<>();

        if (a != null) {
            for (ExchangeDef ex : a) {
                if (ex == null) {
                    continue;
                }
                String sig = exchangeSignature(ex);
                if (seen.add(sig)) {
                    out.add(new ExchangeDef(copyStack(ex.leftItem), copyStack(ex.middleItem), copyStack(ex.rightItem),
                            copyStack(ex.resultItem)));
                }
            }
        }

        if (b != null) {
            for (ExchangeDef ex : b) {
                if (ex == null) {
                    continue;
                }
                String sig = exchangeSignature(ex);
                if (seen.add(sig)) {
                    out.add(new ExchangeDef(copyStack(ex.leftItem), copyStack(ex.middleItem), copyStack(ex.rightItem),
                            copyStack(ex.resultItem)));
                }
            }
        }

        return out;
    }

    private static List<CategoryDef> mergeCategories(List<CategoryDef> a, List<CategoryDef> b) {
        List<CategoryDef> out = new ArrayList<>();
        Set<String> seen = new HashSet<>();

        if (a != null) {
            for (CategoryDef c : a) {
                if (c == null) {
                    continue;
                }
                String sig = categorySignature(c);
                if (seen.add(sig)) {
                    out.add(new CategoryDef(c.name, c.startIndex, c.endIndex));
                }
            }
        }

        if (b != null) {
            for (CategoryDef c : b) {
                if (c == null) {
                    continue;
                }
                String sig = categorySignature(c);
                if (seen.add(sig)) {
                    out.add(new CategoryDef(c.name, c.startIndex, c.endIndex));
                }
            }
        }

        out.sort(Comparator.comparingInt((CategoryDef c) -> c.startIndex).thenComparingInt(c -> c.endIndex)
                .thenComparing(c -> c.name == null ? "" : c.name));
        return out;
    }

    private static String merchantKey(MerchantDef m) {
        if (m == null) {
            return "";
        }
        String id = m.id == null ? "" : m.id.trim();
        if (!id.isEmpty()) {
            return "id:" + id;
        }
        String name = m.name == null ? "" : m.name.trim();
        return "name:" + name;
    }

    private static String categorySignature(CategoryDef c) {
        String n = c.name == null ? "" : c.name.trim();
        return n + "|" + c.startIndex + "|" + c.endIndex;
    }

    private static String exchangeSignature(ExchangeDef ex) {
        return stackSignature(ex.leftItem) + "->" + stackSignature(ex.middleItem) + "->" + stackSignature(ex.rightItem)
                + "=>" + stackSignature(ex.resultItem);
    }

    private static String stackSignature(ItemStack s) {
        if (s == null || s.isEmpty()) {
            return "empty";
        }
        String id;
        if (s.getItem() == null || s.getItem().getRegistryName() == null) {
            id = "unknown";
        } else {
            id = String.valueOf(s.getItem().getRegistryName());
        }

        String nbt;
        NBTTagCompound tag = s.getTagCompound();
        if (s.hasTagCompound() && tag != null) {
            nbt = tag.toString();
        } else {
            nbt = "";
        }
        String name = s.hasDisplayName() ? s.getDisplayName() : "";
        return id + "#" + s.getMetadata() + "x" + s.getCount() + "@" + name + "$" + nbt;
    }

    private static ItemStack copyStack(ItemStack s) {
        return s == null ? ItemStack.EMPTY : s.copy();
    }
}
