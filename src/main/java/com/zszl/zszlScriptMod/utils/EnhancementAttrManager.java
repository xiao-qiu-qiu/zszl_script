package com.zszl.zszlScriptMod.utils;

import com.zszl.zszlScriptMod.zszlScriptMod;
import net.minecraft.client.resources.I18n;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

public class EnhancementAttrManager {

    public static volatile String content = I18n.format("msg.enhancement_attr.fetching");
    private static boolean hasFetched = false;
    private static volatile boolean fetchInProgress = false;
    private static volatile long lastFetchTime = 0L;

    private static final String ENHANCEMENT_URL_KEY = "enhancement_attr.url";
    private static final String MOBILE_USER_AGENT = "Mozilla/5.0 (iPhone; CPU iPhone OS 13_5 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/13.1.1 Mobile/15E148 Safari/604.1";
    private static final String CACHE_FILE = "enhancement_attr.md";

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
                Document doc = HttpsCompat.connect(SharechainLinkConfig.getRequiredUrl(ENHANCEMENT_URL_KEY))
                        .userAgent(MOBILE_USER_AGENT)
                        .timeout(15000)
                        .get();

                Element noteContentDiv = doc.selectFirst("div.note-content");
                if (noteContentDiv != null) {
                    StringBuilder markdownBuilder = new StringBuilder();
                    for (Element p : noteContentDiv.select("p")) {
                        String lineText = p.text().replace("\u00a0", " ");
                        markdownBuilder.append(lineText).append("\n");
                    }
                    String latest = markdownBuilder.toString().trim();
                    if (!latest.isEmpty()) {
                        content = latest;
                        CloudContentCache.writeText(CACHE_FILE, latest);
                    }
                } else {
                    throw new Exception(I18n.format("msg.common.error.note_content_missing"));
                }
            } catch (Exception e) {
                if (content == null || content.trim().isEmpty()
                        || content.equals(I18n.format("msg.enhancement_attr.fetching"))) {
                    content = I18n.format("msg.enhancement_attr.fetch_failed", e.getMessage());
                }
                zszlScriptMod.LOGGER.error("Failed to fetch Enhancement Attr content", e);
            } finally {
                fetchInProgress = false;
            }
        }, "EnhancementAttr-Refresh").start();
    }
}
