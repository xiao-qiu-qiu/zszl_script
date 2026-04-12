// 文件路径: src/main/java/com/keycommand2/zszlScriptMod/gui/GuiInventoryViewer.java
// (这是最终的、调用新高级编辑器的版本)
package com.zszl.zszlScriptMod.gui;

import com.zszl.zszlScriptMod.gui.components.GuiTheme;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.resources.I18n;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.input.Keyboard;

import com.zszl.zszlScriptMod.gui.nbt.GuiNBTAdvanced;
import com.zszl.zszlScriptMod.inventory.InventoryViewerContainer;

import java.io.IOException;

public class GuiInventoryViewer extends GuiContainer {

    private static final ResourceLocation CHEST_GUI_TEXTURE = new ResourceLocation(
            "textures/gui/container/generic_54.png");
    private final IInventory playerInventory;
    private final IInventory viewerInventory;
    private final int inventoryRows;

    public GuiInventoryViewer(IInventory playerInv, IInventory viewerInv) {
        super(new InventoryViewerContainer(playerInv, viewerInv));
        this.playerInventory = playerInv;
        this.viewerInventory = viewerInv;
        this.inventoryRows = viewerInv.getSizeInventory() / 9;
        this.ySize = 114 + this.inventoryRows * 18;
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        this.drawDefaultBackground();
        super.drawScreen(mouseX, mouseY, partialTicks);
        this.renderHoveredToolTip(mouseX, mouseY);
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        String title = I18n.format("gui.inv_viewer.title");
        String invName = this.playerInventory.getDisplayName().getUnformattedText();
        this.fontRenderer.drawString(title, 8, 6, GuiTheme.resolveTextColor(title, 4210752));
        this.fontRenderer.drawString(invName, 8, this.ySize - 96 + 2, GuiTheme.resolveTextColor(invName, 4210752));
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        this.mc.getTextureManager().bindTexture(CHEST_GUI_TEXTURE);
        int i = (this.width - this.xSize) / 2;
        int j = (this.height - this.ySize) / 2;
        this.drawTexturedModalRect(i, j, 0, 0, this.xSize, this.inventoryRows * 18 + 17);
        this.drawTexturedModalRect(i, j + this.inventoryRows * 18 + 17, 0, 126, this.xSize, 96);
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if (keyCode == Keyboard.KEY_F) {
            Slot hoveredSlot = this.getSlotUnderMouse();
            if (hoveredSlot != null && hoveredSlot.getHasStack()) {
                // !! 核心修改：打开新的 GuiNBTAdvanced !!
                this.mc.displayGuiScreen(new GuiNBTAdvanced(this, hoveredSlot.getStack()));
                return;
            }
        }
        super.keyTyped(typedChar, keyCode);
    }
}

