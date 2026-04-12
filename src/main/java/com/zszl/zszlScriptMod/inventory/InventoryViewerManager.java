// 文件路径: src/main/java/com/keycommand2/zszlScriptMod/inventory/InventoryViewerManager.java
package com.zszl.zszlScriptMod.inventory;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.InventoryBasic;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;

public class InventoryViewerManager {

    // 使用一个标准的IInventory实现来存储物品，大小为54（大箱子）
    private static final IInventory copiedInventory = new InventoryBasic("CopiedInventory", false, 54);

    /**
     * 获取存储被复制物品的物品栏实例。
     * @return IInventory 实例
     */
    public static IInventory getCopiedInventory() {
        return copiedInventory;
    }

    /**
     * 核心逻辑：复制当前目标玩家的物品栏。
     */
    public static void copyInventoryFromTarget() {
        Minecraft mc = Minecraft.getMinecraft();
        EntityPlayerSP player = mc.player;

        if (player == null) return;

        Entity targetEntity = mc.pointedEntity;
        if (targetEntity instanceof EntityPlayer) {
            EntityPlayer targetPlayer = (EntityPlayer) targetEntity;

            // 清空上一次的物品
            copiedInventory.clear();

            // 复制主物品栏 (0-35)
            for (int i = 0; i < targetPlayer.inventory.mainInventory.size(); i++) {
                if (i < copiedInventory.getSizeInventory()) {
                    copiedInventory.setInventorySlotContents(i, targetPlayer.inventory.mainInventory.get(i).copy());
                }
            }
            
            // 复制盔甲栏 (倒序放入，符合视觉习惯)
            for (int i = 0; i < targetPlayer.inventory.armorInventory.size(); i++) {
                int targetSlot = 45 + (3 - i); // 放入 45, 46, 47, 48
                if (targetSlot < copiedInventory.getSizeInventory()) {
                    copiedInventory.setInventorySlotContents(targetSlot, targetPlayer.inventory.armorInventory.get(i).copy());
                }
            }

            // 复制副手
            if (49 < copiedInventory.getSizeInventory()) {
                 copiedInventory.setInventorySlotContents(49, targetPlayer.inventory.offHandInventory.get(0).copy());
            }

            player.sendMessage(new TextComponentString(TextFormatting.GREEN + "[物品查看器] 已成功复制玩家 " + TextFormatting.AQUA + targetPlayer.getName() + TextFormatting.GREEN + " 的物品栏。"));
        } else {
            player.sendMessage(new TextComponentString(TextFormatting.RED + "[物品查看器] 准星未对准任何玩家！"));
        }
    }
}

