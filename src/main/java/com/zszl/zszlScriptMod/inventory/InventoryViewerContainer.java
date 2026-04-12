// 文件路径: src/main/java/com/keycommand2/zszlScriptMod/inventory/InventoryViewerContainer.java
// (这是修改后的版本)
package com.zszl.zszlScriptMod.inventory;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

public class InventoryViewerContainer extends Container {

    private final IInventory viewerInventory;
    private final int numRows;

    public InventoryViewerContainer(IInventory playerInventory, IInventory viewerInventory) {
        this.viewerInventory = viewerInventory;
        this.numRows = viewerInventory.getSizeInventory() / 9;
        int yOffset = (this.numRows - 4) * 18;

        // !! 核心修改：使用标准的 Slot 而不是 ReadOnlySlot !!
        for (int row = 0; row < this.numRows; ++row) {
            for (int col = 0; col < 9; ++col) {
                // 现在这是一个可以交互的槽位
                this.addSlotToContainer(new Slot(viewerInventory, col + row * 9, 8 + col * 18, 18 + row * 18));
            }
        }

        // 玩家自己的物品栏 (保持不变)
        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 9; ++col) {
                this.addSlotToContainer(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 103 + row * 18 + yOffset));
            }
        }

        // 玩家自己的快捷栏 (保持不变)
        for (int col = 0; col < 9; ++col) {
            this.addSlotToContainer(new Slot(playerInventory, col, 8 + col * 18, 161 + yOffset));
        }
    }

    @Override
    public boolean canInteractWith(EntityPlayer playerIn) {
        return true;
    }

    // !! 核心修改：添加 transferStackInSlot 方法以支持 Shift+点击 !!
    @Override
    public ItemStack transferStackInSlot(EntityPlayer playerIn, int index) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.inventorySlots.get(index);

        if (slot != null && slot.getHasStack()) {
            ItemStack itemstack1 = slot.getStack();
            itemstack = itemstack1.copy();

            int viewerInventorySize = this.viewerInventory.getSizeInventory();

            if (index < viewerInventorySize) {
                // 从被查看的物品栏 -> 玩家物品栏
                if (!this.mergeItemStack(itemstack1, viewerInventorySize, this.inventorySlots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                // 从玩家物品栏 -> 被查看的物品栏
                if (!this.mergeItemStack(itemstack1, 0, viewerInventorySize, false)) {
                    return ItemStack.EMPTY;
                }
            }

            if (itemstack1.isEmpty()) {
                slot.putStack(ItemStack.EMPTY);
            } else {
                slot.onSlotChanged();
            }
        }
        return itemstack;
    }
    
    // ReadOnlySlot 内部类已被删除
}
