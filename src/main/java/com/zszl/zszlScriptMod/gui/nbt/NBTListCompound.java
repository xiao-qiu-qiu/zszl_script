// 文件路径: src/main/java/com/keycommand2/zszlScriptMod/gui/nbt/NBTListCompound.java
// (这是增加了“添加”功能的版本)
package com.zszl.zszlScriptMod.gui.nbt;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.RenderItem;
import net.minecraft.client.resources.I18n;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagString;

import java.util.ArrayList;
import java.util.List;

import com.zszl.zszlScriptMod.gui.components.GuiTextInput;

public class NBTListCompound extends NBTListElement {
    protected List<NBTListElement> children;
    protected boolean closed;

    public static ItemStack openIcon = new ItemStack(Blocks.CHEST);
    public static ItemStack closedIcon = new ItemStack(Blocks.ENDER_CHEST);

    public NBTListCompound(String key, NBTTagCompound tag, boolean closed, int x, int y) {
        this(key, tag, closed ? closedIcon : openIcon, x, y);
        this.closed = closed;
    }

    public NBTListCompound(String key, NBTTagCompound tag, ItemStack iconStack, int x, int y) {
        super(key, tag, iconStack, x, y);
        this.closed = true;
        rebuildChildren();
    }

    // !! 新增：将子节点构建逻辑提取出来，方便刷新 !!
    public void rebuildChildren() {
        if (tag != null) {
            children = new ArrayList<>();
            int length = 20;
            for (String childKey : ((NBTTagCompound) tag).getKeySet()) {
                NBTBase child = ((NBTTagCompound) tag).getTag(childKey);
                int lengthToAdd;
                if (child instanceof NBTTagCompound)
                    lengthToAdd = addChild(
                            new NBTListCompound(childKey, (NBTTagCompound) child, true, x + 15, y + length));
                else
                    lengthToAdd = addChild(
                            new NBTListElement(childKey, child, new ItemStack(Items.PAPER), x + 15, y + length));
                length += lengthToAdd;
            }
        }
    }

    public int addChild(NBTListElement child) {
        child.parent = this;
        children.add(child);
        return (child instanceof NBTListCompound) ? ((NBTListCompound) child).getLength() + 20 : 20;
    }

    public NBTTagCompound getTagCompound() {
        return (NBTTagCompound) tag;
    }

    @Override
    public String getText() {
        return children != null ? getKey() + " (" + children.size() + ")" : getKey();
    }

    @Override
    public String getTypeName() {
        return "Compound Tag";
    }

    @Override
    public void drawIcon(RenderItem itemRender) {
        super.drawIcon(itemRender);
        if (closed || children == null)
            return;
        for (NBTListElement e : children) {
            e.drawIcon(itemRender);
        }
    }

    @Override
    public void draw(Minecraft mc, int mouseX, int mouseY) {
        super.draw(mc, mouseX, mouseY);
        if (closed || children == null)
            return;
        for (NBTListElement e : children) {
            drawHorizontalStructureLine(e.getX() - 13, e.getY(), 11);
            e.draw(mc, mouseX, mouseY);
        }
        int length = getLength();
        if (length > 0)
            drawVerticalStructureLine(getX(), getY(), getLength());
    }

    public int getLength() {
        if (!closed && getTagCompound() != null && children != null) {
            int length = children.size() * 20;
            for (NBTListElement child : children) {
                if (child instanceof NBTListCompound) {
                    length += ((NBTListCompound) child).getLength();
                }
            }
            return length;
        } else {
            return 0;
        }
    }

    @Override
    public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        if (this instanceof NBTListRoot)
            return;
        if (isMouseOver(mouseX, mouseY) && mouseButton == 0) {
            closed = !closed;
            icon = closed ? closedIcon : openIcon;
            getRootAsRoot().redoPositions();
        } else if (!closed && children != null && !children.isEmpty()) {
            for (NBTListElement e : children) {
                e.mouseClicked(mouseX, mouseY, mouseButton);
            }
        }
    }

    public void redoPositions() {
        if (children == null)
            return;
        int length = 20;
        for (NBTListElement e : children) {
            e.setY(this.getY() + length);
            length += 20;
            if (e instanceof NBTListCompound) {
                NBTListCompound tag = ((NBTListCompound) e);
                length += tag.getLength();
                tag.redoPositions();
            }
        }
    }

    // !! 核心修改：为Compound类型添加“添加标签”选项 !!
    @Override
    public NBTOption[] getOptions() {
        List<NBTOption> options = new ArrayList<>(java.util.Arrays.asList(super.getOptions()));

        options.add(0, new NBTOption() { // 添加到最前面
            @Override
            public String getText() {
                return I18n.format("gui.nbt.add_tag");
            }

            @Override
            public void action(GuiScreen currentScreen) {
                Minecraft.getMinecraft().displayGuiScreen(
                        new GuiTextInput(currentScreen, I18n.format("gui.nbt.input_new_key"), (newKey) -> {
                            if (newKey != null && !newKey.trim().isEmpty() && !getTagCompound().hasKey(newKey)) {
                                getTagCompound().setTag(newKey,
                                        new NBTTagString(I18n.format("gui.nbt.default_new_value")));
                            }
                            Minecraft.getMinecraft().displayGuiScreen(currentScreen);
                            getRootAsRoot().refresh(); // 刷新整个树
                        }));
            }
        });

        return options.toArray(new NBTOption[0]);
    }
}
