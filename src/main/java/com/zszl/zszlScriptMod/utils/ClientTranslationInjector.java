package com.zszl.zszlScriptMod.utils;

import com.zszl.zszlScriptMod.zszlScriptMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.resources.IReloadableResourceManager;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.client.resources.IResourceManagerReloadListener;
import net.minecraft.client.resources.Locale;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.regex.Pattern;

public final class ClientTranslationInjector implements IResourceManagerReloadListener {

    public static final ClientTranslationInjector INSTANCE = new ClientTranslationInjector();

    private static final Pattern NUMERIC_VARIABLE_PATTERN = Pattern.compile("%(\\d+\\$)?[\\d\\.]*[df]");
    private static final String[] DOMAINS = { "zszl_script", "shadowbaritone" };

    private boolean registered;

    private ClientTranslationInjector() {
    }

    public void install() {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null) {
            return;
        }
        if (!registered && mc.getResourceManager() instanceof IReloadableResourceManager) {
            ((IReloadableResourceManager) mc.getResourceManager()).registerReloadListener(this);
            registered = true;
        }
        injectTranslations("install");
    }

    @Override
    public void onResourceManagerReload(IResourceManager resourceManager) {
        injectTranslations("resource_reload");
    }

    @SuppressWarnings("unchecked")
    private void injectTranslations(String reason) {
        try {
            Object localeObject = ReflectionCompat.getPrivateValue(I18n.class, null, "i18nLocale", "field_135054_a");
            if (!(localeObject instanceof Locale)) {
                return;
            }

            Map<String, String> properties = ReflectionCompat.getPrivateValue(Locale.class, (Locale) localeObject,
                    "properties", "field_135032_a");
            if (properties == null) {
                return;
            }

            int before = properties.size();
            mergeLocale(properties, "en_us");

            String currentLanguage = resolveCurrentLanguageCode();
            if (!"en_us".equals(currentLanguage)) {
                mergeLocale(properties, currentLanguage);
            }

            int injected = properties.size() - before;
            if (injected > 0) {
                zszlScriptMod.LOGGER.info("Injected {} translation entries via {} for locale {}", injected, reason,
                        currentLanguage);
            }
        } catch (Throwable t) {
            zszlScriptMod.LOGGER.warn("Failed to inject bundled translations via {}", reason, t);
        }
    }

    private void mergeLocale(Map<String, String> properties, String localeCode) {
        String normalizedLocale = normalizeLocaleCode(localeCode);
        for (String domain : DOMAINS) {
            String resourcePath = "assets/" + domain + "/lang/" + normalizedLocale + ".lang";
            try (InputStream stream = ClientTranslationInjector.class.getClassLoader().getResourceAsStream(resourcePath)) {
                if (stream == null) {
                    continue;
                }
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        if (line.isEmpty() || line.charAt(0) == '#') {
                            continue;
                        }

                        int splitIndex = findSplitIndex(line);
                        if (splitIndex <= 0 || splitIndex >= line.length() - 1) {
                            continue;
                        }

                        String key = line.substring(0, splitIndex).trim();
                        String value = line.substring(splitIndex + 1).trim();
                        if (key.isEmpty() || value.isEmpty()) {
                            continue;
                        }

                        properties.put(key, NUMERIC_VARIABLE_PATTERN.matcher(value).replaceAll("%$1s"));
                    }
                }
            } catch (Throwable t) {
                zszlScriptMod.LOGGER.warn("Failed to merge translation resource {}", resourcePath, t);
            }
        }
    }

    private String resolveCurrentLanguageCode() {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null || mc.gameSettings == null || mc.gameSettings.language == null) {
            return "en_us";
        }
        return normalizeLocaleCode(mc.gameSettings.language);
    }

    private String normalizeLocaleCode(String localeCode) {
        if (localeCode == null) {
            return "en_us";
        }
        String normalized = localeCode.trim().toLowerCase(java.util.Locale.ROOT);
        return normalized.isEmpty() ? "en_us" : normalized;
    }

    private int findSplitIndex(String line) {
        int equals = line.indexOf('=');
        int colon = line.indexOf(':');
        if (equals < 0) {
            return colon;
        }
        if (colon < 0) {
            return equals;
        }
        return Math.min(equals, colon);
    }
}
