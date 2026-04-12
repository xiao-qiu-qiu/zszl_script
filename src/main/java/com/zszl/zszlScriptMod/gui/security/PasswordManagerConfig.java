package com.zszl.zszlScriptMod.gui.security;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.zszl.zszlScriptMod.config.DebugModule;
import com.zszl.zszlScriptMod.config.ModConfig;
import com.zszl.zszlScriptMod.system.ProfileManager;
import com.zszl.zszlScriptMod.zszlScriptMod;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class PasswordManagerConfig {

    public static class PasswordEntry {
        public String playerId;
        public String password;
        public boolean autoLogin;

        public PasswordEntry() {
        }

        public PasswordEntry(String playerId, String password) {
            this(playerId, password, false);
        }

        public PasswordEntry(String playerId, String password, boolean autoLogin) {
            this.playerId = playerId;
            this.password = password;
            this.autoLogin = autoLogin;
        }
    }

    private static class Data {
        public List<PasswordEntry> entries = new ArrayList<>();
        public String selectedPlayerId = "";
    }

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static Data DATA = new Data();

    private static Path getConfigFile() {
        // 密码为全局配置，不跟随 profile 切换
        return Paths.get(ModConfig.CONFIG_DIR).resolve("password_manager.json");
    }

    private static Path getLegacyProfileConfigFile() {
        // 兼容旧版本（曾保存在 profile 目录）
        return ProfileManager.getCurrentProfileDir().resolve("password_manager.json");
    }

    private static Data tryLoadFrom(Path file) {
        if (!Files.exists(file)) {
            return null;
        }
        try (BufferedReader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            Data loaded = GSON.fromJson(reader, Data.class);
            if (loaded != null) {
                if (loaded.entries == null) {
                    loaded.entries = new ArrayList<>();
                }
                if (loaded.selectedPlayerId == null) {
                    loaded.selectedPlayerId = "";
                }
                return loaded;
            }
        } catch (Exception e) {
            zszlScriptMod.LOGGER.error("Failed to load password manager config: " + file, e);
        }
        return null;
    }

    public static synchronized void load() {
        Path globalFile = getConfigFile();
        Data loadedGlobal = tryLoadFrom(globalFile);
        if (loadedGlobal != null) {
            DATA = loadedGlobal;
            ModConfig.debugPrint(DebugModule.PASSWORD_MANAGER,
                    "Loaded global password config: " + globalFile + ", entries=" + DATA.entries.size() + ", selected='"
                            + DATA.selectedPlayerId
                            + "'");
            return;
        }

        // 兼容迁移：如果全局文件不存在，尝试读取旧的 profile 文件并迁移
        Path legacyProfileFile = getLegacyProfileConfigFile();
        Data loadedLegacy = tryLoadFrom(legacyProfileFile);
        if (loadedLegacy != null) {
            DATA = loadedLegacy;
            save();
            zszlScriptMod.LOGGER.info("Migrated password config from profile dir to global dir: " + globalFile);
            ModConfig.debugPrint(DebugModule.PASSWORD_MANAGER,
                    "Migrated legacy password config to global dir, entries=" + DATA.entries.size() + ", selected='"
                            + DATA.selectedPlayerId + "'");
            return;
        }

        DATA = new Data();
        ModConfig.debugPrint(DebugModule.PASSWORD_MANAGER,
                "Password config not found, initialized empty data. Path=" + globalFile);
    }

    public static synchronized void save() {
        Path file = getConfigFile();
        try {
            Files.createDirectories(file.getParent());
            try (BufferedWriter writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
                GSON.toJson(DATA, writer);
            }
            ModConfig.debugPrint(DebugModule.PASSWORD_MANAGER,
                    "Saved password config: " + file + ", entries=" + DATA.entries.size() + ", selected='"
                            + DATA.selectedPlayerId + "'");
        } catch (Exception e) {
            zszlScriptMod.LOGGER.error("Failed to save password manager config", e);
        }
    }

    public static synchronized List<PasswordEntry> getEntries() {
        List<PasswordEntry> copy = new ArrayList<>();
        for (PasswordEntry e : DATA.entries) {
            if (e != null && e.playerId != null) {
                copy.add(new PasswordEntry(e.playerId, e.password == null ? "" : e.password, e.autoLogin));
            }
        }
        return copy;
    }

    public static synchronized void upsertEntry(String playerId, String password) {
        String id = playerId == null ? "" : playerId.trim();
        String pwd = password == null ? "" : password;
        if (id.isEmpty()) {
            ModConfig.debugPrint(DebugModule.PASSWORD_MANAGER, "upsertEntry ignored: playerId is empty");
            return;
        }

        for (PasswordEntry e : DATA.entries) {
            if (e != null && id.equals(e.playerId)) {
                e.password = pwd;
                DATA.selectedPlayerId = id;
                ModConfig.debugPrint(DebugModule.PASSWORD_MANAGER,
                        "Updated password entry: id='" + id + "', passwordLen=" + pwd.length() + ", autoLogin="
                                + e.autoLogin);
                save();
                return;
            }
        }

        DATA.entries.add(new PasswordEntry(id, pwd));
        DATA.selectedPlayerId = id;
        ModConfig.debugPrint(DebugModule.PASSWORD_MANAGER,
                "Added password entry: id='" + id + "', passwordLen=" + pwd.length());
        save();
    }

    public static synchronized void setAutoLogin(String playerId, boolean autoLogin) {
        String id = playerId == null ? "" : playerId.trim();
        if (id.isEmpty()) {
            ModConfig.debugPrint(DebugModule.PASSWORD_MANAGER, "setAutoLogin ignored: playerId is empty");
            return;
        }

        for (PasswordEntry e : DATA.entries) {
            if (e != null && id.equals(e.playerId)) {
                e.autoLogin = autoLogin;
                ModConfig.debugPrint(DebugModule.PASSWORD_MANAGER,
                        "Toggled autoLogin: id='" + id + "', autoLogin=" + autoLogin);
                save();
                return;
            }
        }

        ModConfig.debugPrint(DebugModule.PASSWORD_MANAGER,
                "setAutoLogin entry not found: id='" + id + "', ignored");
    }

    public static synchronized void removeEntry(String playerId) {
        String id = playerId == null ? "" : playerId.trim();
        if (id.isEmpty()) {
            ModConfig.debugPrint(DebugModule.PASSWORD_MANAGER, "removeEntry ignored: playerId is empty");
            return;
        }
        DATA.entries.removeIf(e -> e != null && id.equals(e.playerId));
        if (id.equals(DATA.selectedPlayerId)) {
            DATA.selectedPlayerId = "";
        }
        ModConfig.debugPrint(DebugModule.PASSWORD_MANAGER, "Removed password entry: id='" + id + "'");
        save();
    }

    public static synchronized void setSelectedPlayerId(String playerId) {
        DATA.selectedPlayerId = playerId == null ? "" : playerId.trim();
        ModConfig.debugPrint(DebugModule.PASSWORD_MANAGER,
                "Switched selected account: selected='" + DATA.selectedPlayerId + "'");
        save();
    }

    public static synchronized PasswordEntry getSelectedEntry() {
        String selected = DATA.selectedPlayerId == null ? "" : DATA.selectedPlayerId.trim();
        if (!selected.isEmpty()) {
            for (PasswordEntry e : DATA.entries) {
                if (e != null && selected.equals(e.playerId)) {
                    return new PasswordEntry(e.playerId, e.password == null ? "" : e.password, e.autoLogin);
                }
            }
        }
        if (!DATA.entries.isEmpty()) {
            PasswordEntry e = DATA.entries.get(0);
            if (e != null && e.playerId != null) {
                return new PasswordEntry(e.playerId, e.password == null ? "" : e.password, e.autoLogin);
            }
        }
        return null;
    }

    public static synchronized PasswordEntry findEntryByPlayerId(String playerId) {
        String target = playerId == null ? "" : playerId.trim();
        if (target.isEmpty()) {
            return null;
        }

        for (PasswordEntry e : DATA.entries) {
            if (e != null && e.playerId != null && target.equalsIgnoreCase(e.playerId.trim())) {
                ModConfig.debugPrint(DebugModule.PASSWORD_MANAGER,
                        "Matched entry by player name: target='" + target + "' -> id='" + e.playerId + "', passwordLen="
                                + (e.password == null ? 0 : e.password.length()) + "，autoLogin=" + e.autoLogin);
                return new PasswordEntry(e.playerId, e.password == null ? "" : e.password, e.autoLogin);
            }
        }
        ModConfig.debugPrint(DebugModule.PASSWORD_MANAGER, "No entry matched by player name: target='" + target + "'");
        return null;
    }
}
