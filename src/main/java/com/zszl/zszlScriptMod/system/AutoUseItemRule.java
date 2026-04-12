package com.zszl.zszlScriptMod.system;

import net.minecraft.client.resources.I18n;

public class AutoUseItemRule {
    public enum UseMode {
        RIGHT_CLICK,
        LEFT_CLICK
    }

    public enum MatchMode {
        CONTAINS,
        EXACT
    }

    public String name;
    public String category;
    public boolean enabled;
    public UseMode useMode;
    public MatchMode matchMode;
    public int intervalMs;
    public int switchDelayTicks;
    public int restoreDelayTicks;

    public transient long lastUseAtMs;

    public AutoUseItemRule() {
        this.name = I18n.format("gui.autouseitem.rule.default_name");
        this.category = "默认";
        this.enabled = true;
        this.useMode = UseMode.RIGHT_CLICK;
        this.matchMode = MatchMode.CONTAINS;
        this.intervalMs = 250;
        this.switchDelayTicks = 0;
        this.restoreDelayTicks = 0;
        this.lastUseAtMs = 0L;
    }
}
