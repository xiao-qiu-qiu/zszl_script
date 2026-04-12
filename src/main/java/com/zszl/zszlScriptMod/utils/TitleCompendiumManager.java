package com.zszl.zszlScriptMod.utils;

import com.zszl.zszlScriptMod.zszlScriptMod;
import net.minecraft.client.resources.I18n;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.util.ArrayList;
import java.util.List;

public class TitleCompendiumManager {

    public static volatile String content = I18n.format("msg.title_compendium.fetching");
    private static boolean hasFetched = false;
    private static volatile boolean fetchInProgress = false;
    private static volatile long lastFetchTime = 0L;

    private static final String COMPENDIUM_URL_KEY = "title_compendium.url";
    private static final String MOBILE_USER_AGENT = "Mozilla/5.0 (iPhone; CPU iPhone OS 13_5 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/13.1.1 Mobile/15E148 Safari/604.1";
    private static final String CACHE_FILE = "title_compendium.md";

    public static void fetchContent() {
        if (hasFetched) {
            return;
        }
        String cached = CloudContentCache.readText(CACHE_FILE);
        if (!cached.isEmpty()) {
            content = cached;
        }
        hasFetched = true;
        forceRefresh();
    }

    public static void requestRefreshIfDue(long intervalMs) {
        long now = System.currentTimeMillis();
        if (now - lastFetchTime >= intervalMs) {
            forceRefresh();
        }
    }

    public static void forceRefresh() {
        if (fetchInProgress) {
            return;
        }
        fetchInProgress = true;
        lastFetchTime = System.currentTimeMillis();

        new Thread(() -> {
            try {
                Document doc = HttpsCompat.connect(SharechainLinkConfig.getRequiredUrl(COMPENDIUM_URL_KEY))
                        .userAgent(MOBILE_USER_AGENT)
                        .timeout(15000)
                        .get();

                Element noteContentDiv = doc.selectFirst("div.note-content");

                String rawText;
                if (noteContentDiv != null) {
                    StringBuilder markdownBuilder = new StringBuilder();
                    for (Element p : noteContentDiv.select("p")) {
                        String lineText = p.text().replace("\u00a0", " ");
                        markdownBuilder.append(lineText).append("\n");
                    }
                    rawText = markdownBuilder.toString().trim();
                } else {
                    throw new Exception(I18n.format("msg.common.error.note_content_missing"));
                }

                String enhanced = enhanceCategoryToc(rawText);
                if (enhanced != null && !enhanced.trim().isEmpty()) {
                    content = enhanced;
                    CloudContentCache.writeText(CACHE_FILE, enhanced);
                }
            } catch (Exception e) {
                if (content == null || content.trim().isEmpty()
                        || content.equals(I18n.format("msg.title_compendium.fetching"))) {
                    content = I18n.format("msg.title_compendium.fetch_failed", e.getMessage());
                }
                zszlScriptMod.LOGGER.error("Failed to fetch Title Compendium content", e);
            } finally {
                fetchInProgress = false;
            }
        }, "TitleCompendium-Refresh").start();
    }

    private static String enhanceCategoryToc(String markdown) {
        if (markdown == null || markdown.trim().isEmpty()) {
            return markdown;
        }

        String[] lines = markdown.split("\\r?\\n");
        List<String> categories = new ArrayList<>();

        boolean inBaseTable = false;
        String lastCategory = null;

        for (String line : lines) {
            String trimmed = line == null ? "" : line.trim();

            if (trimmed.startsWith("### ") && trimmed.contains("基础称号")) {
                inBaseTable = false;
                continue;
            }
            if (trimmed.startsWith("### ") && trimmed.contains("合并称号")) {
                break;
            }

            if (!inBaseTable && trimmed.startsWith("| 类别")) {
                inBaseTable = true;
                continue;
            }

            if (!inBaseTable || !trimmed.startsWith("|")) {
                continue;
            }

            String[] cols = trimmed.split("\\|");
            if (cols.length < 3) {
                continue;
            }
            String category = cols[1].trim().replace("**", "");
            if (category.isEmpty() || "类别".equals(category) || category.equals(lastCategory)) {
                continue;
            }

            categories.add(category);
            lastCategory = category;
        }

        if (categories.isEmpty()) {
            return markdown;
        }

        StringBuilder prefix = new StringBuilder();
        prefix.append("### 📚 基础称号分类索引\n\n");
        for (String c : categories) {
            prefix.append("### ").append(c).append("\n");
        }
        prefix.append("\n");

        if (markdown.contains("### 🏆 基础称号列表")) {
            return markdown.replace("### 🏆 基础称号列表", "### 🏆 基础称号列表\n\n" + prefix.toString().trim());
        }

        return prefix + markdown;
    }
}
