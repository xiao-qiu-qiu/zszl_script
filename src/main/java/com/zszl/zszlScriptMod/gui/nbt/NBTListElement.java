// 文件路径: src/main/java/com/keycommand2/zszlScriptMod/gui/nbt/NBTListElement.java
package com.zszl.zszlScriptMod.gui.nbt;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.renderer.RenderItem;
import net.minecraft.client.resources.I18n;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.JsonToNBT; // !! 核心修复：添加导入 !!

import java.util.ArrayList;
import java.util.List;

import com.zszl.zszlScriptMod.gui.components.GuiTextInput;

public class NBTListElement extends Gui {
    protected String key;
    protected NBTBase tag;
    protected ItemStack icon;
    protected NBTListCompound parent = null;

    // !! 核心修复：将访问权限从默认改为 protected !!
    protected int x, y;

    public NBTListElement(String key, NBTBase tag, ItemStack iconStack, int x, int y) {
        this.key = key;
        this.tag = tag;
        this.icon = iconStack;
        this.x = x;
        this.y = y;
    }

    public String getKey() {
        return key;
    }

    public String getText() {
        String valueStr = tag.toString();
        if (valueStr.length() > 40) {
            valueStr = valueStr.substring(0, 37) + "...";
        }
        return getKey() + " : " + valueStr;
    }

    public String getTypeName() {
        switch (tag.getId()) {
            case 1:
                return "Byte";
            case 2:
                return "Short";
            case 3:
                return "Int";
            case 4:
                return "Long";
            case 5:
                return "Float";
            case 6:
                return "Double";
            case 7:
                return "Byte Array";
            case 8:
                return "String";
            case 9:
                return "List";
            case 10:
                return "Compound";
            case 11:
                return "Int Array";
            default:
                return "Unknown";
        }
    }

    public NBTBase getTag() {
        return tag;
    }

    public ItemStack getIconStack() {
        return icon;
    }

    public void drawIcon(RenderItem itemRender) {
        itemRender.renderItemAndEffectIntoGUI(getIconStack(), x - 8, y - 9);
    }

    public void draw(Minecraft mc, int mouseX, int mouseY) {
        boolean over = isMouseOver(mouseX, mouseY);
        drawString(mc.fontRenderer, mc.fontRenderer.trimStringToWidth(getText(), 300), x + 15, y - 5,
                over ? 0xFFe67e22 : 0xffffff);
    }

    protected static void drawVerticalStructureLine(int x, int y, int length) {
        drawRect(x - 1, y - 1, x + 1, y + length + 1, 0xFF2c3e50);
    }

    protected static void drawHorizontalStructureLine(int x, int y, int length) {
        drawRect(x - 1, y - 1, x + length + 1, y + 1, 0xFF34495e);
    }

    public boolean isMouseOver(int mouseX, int mouseY) {
        if (getRootAsRoot().getSelected() != null)
            return false;
        int left = x - 9;
        int textWidth = Minecraft.getMinecraft().fontRenderer.getStringWidth(getText());
        int right = left + 25 + textWidth;
        int top = y - 8;
        int bottom = y + 7;
        return mouseX > left && mouseX < right && mouseY > top && mouseY < bottom;
    }

    public void mouseClicked(int x, int y, int mouseButton) {
        if (isMouseOver(x, y)) {
            if (mouseButton == 1) { // Right click
                NBTListRoot root = getRootAsRoot();
                root.setSelected(this, x, y);
            } else {
                getRootAsRoot().setFocus(this);
            }
        }
    }

    public int getX() {
        return x;
    }

    public void setX(int x) {
        this.x = x;
    }

    public int getY() {
        return y;
    }

    public void setY(int y) {
        this.y = y;
    }

    public NBTListElement getRoot() {
        return parent != null ? parent.getRoot() : this;
    }

    public NBTListRoot getRootAsRoot() {
        NBTListElement root = getRoot();
        return root instanceof NBTListRoot ? (NBTListRoot) root : null;
    }

    public NBTOption[] getOptions() {
        List<NBTOption> options = new ArrayList<>();

        options.add(new NBTOption() {
            @Override
            public String getText() {
                return I18n.format("gui.nbt.change_value");
            }

            @Override
            public void action(GuiScreen currentScreen) {
                Minecraft.getMinecraft().displayGuiScreen(new GuiTextInput(currentScreen,
                        I18n.format("gui.nbt.edit_value", getKey()), GuiNBTAdvanced.tagToString(tag), (newValue) -> {
                            try {
                                NBTBase newTag = JsonToNBT.getTagFromJson(newValue);
                                if (newTag.getId() == tag.getId()) {
                                    parent.getTagCompound().setTag(getKey(), newTag);
                                }
                            } catch (Exception e) {
                                parent.getTagCompound().setString(getKey(), newValue);
                            }
                            Minecraft.getMinecraft().displayGuiScreen(currentScreen);
                            getRootAsRoot().refresh();
                        }));
            }
        });

        options.add(new NBTOption() {
            @Override
            public String getText() {
                return I18n.format("gui.auto_skill.delete");
            }

            @Override
            public void action(GuiScreen currentScreen) {
                if (parent != null && parent.children != null) {
                    parent.children.remove(NBTListElement.this);
                    parent.getTagCompound().removeTag(getKey());
                }
                NBTListRoot root = getRootAsRoot();
                root.clearSelected();
                root.redoPositions();
            }
        });

        return options.toArray(new NBTOption[0]);
    }
}

