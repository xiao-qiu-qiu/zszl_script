// 文件路径: src/main/java/com/zszl/zszlScriptMod/gui/GuiHandler.java
package com.zszl.zszlScriptMod.gui;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.IGuiHandler;

import javax.annotation.Nullable;

import com.zszl.zszlScriptMod.inventory.InventoryViewerManager;

public class GuiHandler implements IGuiHandler {

    public static final int INVENTORY_VIEWER = 1;
    // WAREHOUSE_CHEST_VIEWER 已被删除

    @Nullable
    @Override
    public Object getServerGuiElement(int ID, EntityPlayer player, World world, int x, int y, int z) {
        // 服务端不需要实现这个，因为我们的GUI是纯客户端的
        return null;
    }

    @Nullable
    @Override
    public Object getClientGuiElement(int ID, EntityPlayer player, World world, int x, int y, int z) {
        if (ID == INVENTORY_VIEWER) {
            return new GuiInventoryViewer(player.inventory, InventoryViewerManager.getCopiedInventory());
        }
        
        // WAREHOUSE_CHEST_VIEWER 的逻辑块已被完全删除
        
        return null;
    }
}
