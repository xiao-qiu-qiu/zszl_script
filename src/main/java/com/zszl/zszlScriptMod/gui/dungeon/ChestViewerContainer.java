// 文件路径: src/main/java/com/zszl/zszlScriptMod/gui/dungeon/ChestViewerContainer.java
package com.zszl.zszlScriptMod.gui.dungeon;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

public class ChestViewerContainer extends Container {

    public ChestViewerContainer(IInventory playerInventory, IInventory chestInventory) {
        int numRows = chestInventory.getSizeInventory() / 9;
        int yOffset = (numRows - 4) * 18;

        // 箱子槽位 (只读)
        for (int row = 0; row < numRows; ++row) {
            for (int col = 0; col < 9; ++col) {
                this.addSlotToContainer(new ReadOnlySlot(chestInventory, col + row * 9, 8 + col * 18, 18 + row * 18));
            }
        }

        // 玩家背包槽位 (只读)
        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 9; ++col) {
                this.addSlotToContainer(new ReadOnlySlot(playerInventory, col + row * 9 + 9, 8 + col * 18, 103 + row * 18 + yOffset));
            }
        }

        // 玩家快捷栏槽位 (只读)
        for (int col = 0; col < 9; ++col) {
            this.addSlotToContainer(new ReadOnlySlot(playerInventory, col, 8 + col * 18, 161 + yOffset));
        }
    }

    @Override
    public boolean canInteractWith(EntityPlayer playerIn) {
        return true;
    }

    // 覆盖此方法，防止Shift+点击移动物品
    @Override
    public ItemStack transferStackInSlot(EntityPlayer playerIn, int index) {
        return ItemStack.EMPTY;
    }

    // 自定义一个只读的Slot类
    private static class ReadOnlySlot extends Slot {
        public ReadOnlySlot(IInventory inventoryIn, int index, int xPosition, int yPosition) {
            super(inventoryIn, index, xPosition, yPosition);
        }

        @Override
        public boolean isItemValid(ItemStack stack) {
            return false; // 不允许放入任何物品
        }

        @Override
        public ItemStack decrStackSize(int amount) {
            return ItemStack.EMPTY; // 不允许减少物品
        }

        @Override
        public void putStack(ItemStack stack) {
            // 不允许放置物品
        }

        @Override
        public boolean canTakeStack(EntityPlayer playerIn) {
            return false; // 不允许拿出物品
        }
    }
}
