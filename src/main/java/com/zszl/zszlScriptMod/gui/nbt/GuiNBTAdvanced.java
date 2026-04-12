// 文件路径: src/main/java/com/keycommand2/zszlScriptMod/gui/nbt/GuiNBTAdvanced.java
package com.zszl.zszlScriptMod.gui.nbt;

import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen; // !! 核心修复：添加导入 !!
import com.zszl.zszlScriptMod.gui.components.ThemedGuiScreen;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.resources.I18n;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;

import java.io.IOException;
import java.util.Iterator;

public class GuiNBTAdvanced extends ThemedGuiScreen {

    private final ItemStack stack;
    private final GuiScreen lastScreen;
    private NBTListRoot rootElement;

    public GuiNBTAdvanced(GuiScreen lastScreen, ItemStack stack) {
        this.lastScreen = lastScreen;
        this.stack = stack;
    }

    @Override
    public void initGui() {
        this.rootElement = new NBTListRoot(stack, this);
        this.buttonList
                .add(new GuiButton(200, this.width / 2 - 100, this.height - 35, 200, 20, I18n.format("gui.done")));
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        if (button.enabled && button.id == 200) {
            this.mc.displayGuiScreen(this.lastScreen);
        }
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        rootElement.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.drawDefaultBackground();

        drawRect(20, 40, width - 20, height - 40, 0xDD222222);
        drawRect(18, 38, width - 18, 40, 0xFF007788);
        drawRect(18, 40, 20, height - 40, 0xFF007788);
        drawRect(width - 20, 40, width - 18, height - 40, 0xFF007788);
        drawRect(18, height - 40, width - 18, height - 38, 0xFF007788);

        GlStateManager.pushMatrix();
        RenderHelper.enableGUIStandardItemLighting();
        GlStateManager.disableLighting();
        GlStateManager.enableRescaleNormal();
        GlStateManager.enableColorMaterial();
        GlStateManager.enableLighting();

        rootElement.drawIcon(itemRender);

        GlStateManager.popMatrix();

        rootElement.draw(mc, mouseX, mouseY);

        this.drawCenteredString(this.fontRenderer, I18n.format("gui.nbt_advanced.title"), this.width / 2, 20,
                0xFFFFFFFF);
        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }

    public static String tagToString(NBTBase tag) {
        return nbtBaseToString(tag);
    }

    private static String nbtBaseToString(NBTBase tag) {
        if (tag instanceof NBTTagCompound) {
            NBTTagCompound compound = (NBTTagCompound) tag;
            StringBuilder sb = new StringBuilder("{");
            Iterator<String> iterator = compound.getKeySet().iterator();
            while (iterator.hasNext()) {
                String key = iterator.next();
                sb.append(key).append(":").append(nbtBaseToString(compound.getTag(key)));
                if (iterator.hasNext()) {
                    sb.append(",");
                }
            }
            sb.append("}");
            return sb.toString();
        } else if (tag instanceof NBTTagList) {
            NBTTagList list = (NBTTagList) tag;
            StringBuilder sb = new StringBuilder("[");
            for (int i = 0; i < list.tagCount(); i++) {
                if (i > 0) {
                    sb.append(",");
                }
                sb.append(nbtBaseToString(list.get(i)));
            }
            sb.append("]");
            return sb.toString();
        }
        return tag.toString();
    }
}
