// 文件路径: src/main/java/com/zszl/zszlScriptMod/gui/dungeon/GuiChestViewer.java
package com.zszl.zszlScriptMod.gui.dungeon;

import com.zszl.zszlScriptMod.gui.components.GuiTheme;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.inventory.IInventory;
import net.minecraft.util.ResourceLocation;

public class GuiChestViewer extends GuiContainer {

    private static final ResourceLocation CHEST_GUI_TEXTURE = new ResourceLocation(
            "textures/gui/container/generic_54.png");
    private final IInventory playerInventory;
    private final IInventory chestInventory;
    private final int inventoryRows;

    public GuiChestViewer(IInventory playerInv, IInventory chestInv) {
        super(new ChestViewerContainer(playerInv, chestInv));
        this.playerInventory = playerInv;
        this.chestInventory = chestInv;
        this.allowUserInput = false; // 禁用键盘输入
        this.inventoryRows = chestInv.getSizeInventory() / 9;
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
        String chestName = this.chestInventory.getDisplayName().getUnformattedText();
        String playerName = this.playerInventory.getDisplayName().getUnformattedText();
        this.fontRenderer.drawString(chestName, 8, 6, GuiTheme.resolveTextColor(chestName, 4210752));
        this.fontRenderer.drawString(playerName, 8, this.ySize - 96 + 2,
                GuiTheme.resolveTextColor(playerName, 4210752));
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
}
