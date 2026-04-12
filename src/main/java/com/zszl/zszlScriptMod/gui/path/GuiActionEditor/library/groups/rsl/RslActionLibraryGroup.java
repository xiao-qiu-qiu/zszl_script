package com.zszl.zszlScriptMod.gui.path.GuiActionEditor.library.groups.rsl;

import com.zszl.zszlScriptMod.gui.path.GuiActionEditor.model.ActionLibraryNode;

import net.minecraft.client.resources.I18n;

import java.util.function.Function;

public final class RslActionLibraryGroup {
    private RslActionLibraryGroup() {
    }

    public static ActionLibraryNode buildRoot(Function<String, ActionLibraryNode> itemFactory) {
        return ActionLibraryNode.group("group_rsl",
                I18n.format("gui.path.action_editor.group.rsl"),
                ActionLibraryNode.group("group_rsl_custom",
                        I18n.format("gui.path.action_editor.group.rsl.custom"),
                        itemFactory.apply("use_skill")));
    }
}
