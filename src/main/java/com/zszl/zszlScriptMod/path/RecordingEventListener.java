package com.zszl.zszlScriptMod.path;

import com.zszl.zszlScriptMod.gui.path.GuiNextStepPrompt;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityChest;
import net.minecraft.util.EnumHand;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * 在录制模式下监听玩家与方块的交互事件
 */
public class RecordingEventListener {

    private final Minecraft mc = Minecraft.getMinecraft();

    @SideOnly(Side.CLIENT)
    @SubscribeEvent
    public void onPlayerRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        // 确保只在客户端、主手、录制模式下响应
        if (!event.getWorld().isRemote || event.getHand() != EnumHand.MAIN_HAND
                || !PathRecordingManager.isRecording()) {
            return;
        }

        EntityPlayerSP player = mc.player;
        if (player == null)
            return;

        TileEntity tileEntity = event.getWorld().getTileEntity(event.getPos());

        // 检查点击的是否是箱子
        if (tileEntity instanceof TileEntityChest) {
            // 阻止默认的开箱行为
            event.setCanceled(true);

            // 记录数据
            PathRecordingManager.RecordedStep step = new PathRecordingManager.RecordedStep(
                    player.getPositionVector(),
                    player.rotationYaw,
                    player.rotationPitch,
                    event.getPos());
            PathRecordingManager.addStep(step);

            // 提示用户
            player.sendMessage(new TextComponentString(
                    TextFormatting.GREEN + I18n.format("msg.path.recording.chest_recorded",
                            PathRecordingManager.getRecordedSteps().size())));

            // 弹出下一步提示GUI
            mc.addScheduledTask(() -> mc.displayGuiScreen(new GuiNextStepPrompt()));
        }
    }
}
