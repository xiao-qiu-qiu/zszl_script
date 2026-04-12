// 文件路径: src/main/java/com/keycommand2/zszlScriptMod/gui/nbt/NBTListRoot.java
package com.zszl.zszlScriptMod.gui.nbt;

import com.zszl.zszlScriptMod.gui.components.GuiTheme;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen; // !! 核心修复：添加导入 !!
import net.minecraft.client.renderer.RenderItem;
import net.minecraft.client.resources.I18n;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

public class NBTListRoot extends NBTListCompound {
    private NBTListCompound tagElement;
    private NBTListElement focus = null;
    private NBTListElement selected = null;
    private int selX = 0, selY = 0, selWidth = 0, selHeight = 0;
    private NBTOption[] options = null;
    private GuiScreen parentGui;

    public NBTListRoot(ItemStack stack, GuiScreen parentGui) {
        super(stack.getDisplayName(), null, stack, 30, 50);
        this.parentGui = parentGui;
        if (icon.hasTagCompound()) {
            tagElement = new NBTListCompound("tag", icon.getTagCompound(), false, getX() + 15, getY() + 20);
            tagElement.parent = this;
        }
    }

    public NBTListElement getSelected() {
        return selected;
    }

    public void setSelected(NBTListElement e, int x, int y) {
        selected = e;
        selX = x;
        selY = y;
        options = selected.getOptions();
        selWidth = 0;
        for (NBTOption o : options) {
            selWidth = Math.max(Minecraft.getMinecraft().fontRenderer.getStringWidth(o.getText()), selWidth);
        }
        selWidth += 10;
        selHeight = 12 * options.length + 15;
    }

    public void clearSelected() {
        selected = null;
        selX = 0;
        selY = 0;
        selWidth = 0;
        selHeight = 0;
        options = null;
    }

    public void setFocus(NBTListElement e) {
        if (e == this)
            return;
        focus = e;
    }

    public void refresh() {
        if (icon.hasTagCompound()) {
            this.tagElement = new NBTListCompound("tag", icon.getTagCompound(),
                    this.tagElement != null ? this.tagElement.closed : false, getX() + 15, getY() + 20);
            this.tagElement.parent = this;
            this.tagElement.redoPositions();
        } else {
            this.tagElement = null;
        }
    }

    @Override
    public void redoPositions() {
        if (tagElement != null) {
            tagElement.redoPositions();
        }
    }

    @Override
    public void drawIcon(RenderItem itemRender) {
        super.drawIcon(itemRender);
        if (tagElement != null)
            tagElement.drawIcon(itemRender);
    }

    @Override
    public void draw(Minecraft mc, int mouseX, int mouseY) {
        drawString(mc.fontRenderer, getText(), getX() + 15, getY() - 5, 0xffffff);
        if (tagElement != null) {
            drawVerticalStructureLine(getX(), getY(), 20);
            drawHorizontalStructureLine(getX() + 2, getY() + 20, 12);
            tagElement.draw(mc, mouseX, mouseY);
        }

        if (selected != null) {
            drawRect(selX, selY, selX + selWidth + 6, selY + 13, 0xCC007788);
            String selectedType = selected.getTypeName();
            mc.fontRenderer.drawString(selectedType, selX + 3, selY + 3,
                    GuiTheme.resolveTextColor(selectedType, 0xFFCCCCCC));
            int i = 1;
            for (NBTOption o : options) {
                boolean over = mouseX >= selX && mouseX <= selX + selWidth + 6 && mouseY >= selY + 13 * i
                        && mouseY <= selY + 13 + 13 * i;
                drawRect(selX, selY + 13 * i, selX + selWidth + 6, selY + 13 + 13 * i, 0xCC333333);
                String optionText = o.getText();
                mc.fontRenderer.drawString(optionText, selX + 3, selY + 3 + i * 13,
                        GuiTheme.resolveTextColor(optionText, over ? 0xFFe67e22 : 0xFFCCCCCC));
                i++;
            }
        }
    }

    public boolean mouseOverSelected(int mouseX, int mouseY) {
        return selected != null && mouseX >= selX && mouseX <= selX + selWidth && mouseY >= selY
                && mouseY <= selY + selHeight;
    }

    @Override
    public void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        if (selected != null) {
            if (mouseOverSelected(mouseX, mouseY)) {
                if (mouseButton == 0) {
                    int y = (mouseY - (selY + 13));
                    if (y >= 0) {
                        int optionIndex = y / 13;
                        if (optionIndex < options.length) {
                            options[optionIndex].action(this.parentGui);
                        }
                    }
                }
            } else {
                clearSelected();
            }
            return;
        }
        if (tagElement != null)
            tagElement.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    public NBTOption[] getOptions() {
        return new NBTOption[] {
                getIconStack().hasTagCompound() ? new NBTOption() {
                    @Override
                    public String getText() {
                        return I18n.format("gui.nbt.clear_all_tags");
                    }

                    // !! 核心修复：为 action 方法添加 GuiScreen 参数 !!
                    @Override
                    public void action(GuiScreen currentScreen) {
                        getIconStack().setTagCompound(null);
                        tagElement = null;
                        clearSelected();
                    }
                } : new NBTOption() {
                    @Override
                    public String getText() {
                        return I18n.format("gui.nbt.create_root_tag");
                    }

                    // !! 核心修复：为 action 方法添加 GuiScreen 参数 !!
                    @Override
                    public void action(GuiScreen currentScreen) {
                        getIconStack().setTagCompound(new NBTTagCompound());
                        tagElement = new NBTListCompound("tag", icon.getTagCompound(), false, getX() + 15, getY() + 20);
                        tagElement.parent = NBTListRoot.this;
                        clearSelected();
                    }
                }
        };
    }
}
