// 文件路径: src/main/java/com/zszl/zszlScriptMod/handlers/GoToAndOpenHandler.java
package com.zszl.zszlScriptMod.handlers;

import com.zszl.zszlScriptMod.utils.ModUtils;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.gui.inventory.GuiChest;
import net.minecraft.inventory.ContainerChest;
import net.minecraft.inventory.IInventory;
import net.minecraft.tileentity.TileEntityChest;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.TextComponentString;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

public class GoToAndOpenHandler {
    private static final GoToAndOpenHandler INSTANCE = new GoToAndOpenHandler();
    private static final Minecraft mc = Minecraft.getMinecraft();

    private enum State {
        IDLE, MOVING, OPENING
    }

    private State currentState = State.IDLE;
    private BlockPos targetChestPos = null;
    private BlockPos targetStandPos = null;
    private int timeoutTicks = 0;

    private GoToAndOpenHandler() {
    }

    public static void start(BlockPos chestPos) {
        if (INSTANCE.currentState != State.IDLE) {
            mc.player.sendMessage(new TextComponentString(I18n.format("msg.goto_open.task_in_progress")));
            return;
        }
        INSTANCE.targetChestPos = chestPos;
        INSTANCE.targetStandPos = INSTANCE.findBestStandPosition(chestPos);
        INSTANCE.currentState = State.MOVING;
        INSTANCE.timeoutTicks = 600; // 30秒超时
        MinecraftForge.EVENT_BUS.register(INSTANCE);

        if (INSTANCE.targetStandPos != null) {
            mc.player.sendMessage(new TextComponentString(
                    I18n.format("msg.goto_open.start_to_interact_pos", INSTANCE.targetStandPos.toString())));
            EmbeddedNavigationHandler.INSTANCE.startGoto(INSTANCE.targetStandPos.getX(), INSTANCE.targetStandPos.getY(),
                    INSTANCE.targetStandPos.getZ());
        } else {
            mc.player.sendMessage(new TextComponentString(
                    I18n.format("msg.goto_open.fallback_to_chest_pos", chestPos.toString())));
            EmbeddedNavigationHandler.INSTANCE.startGoto(chestPos.getX(), chestPos.getY(), chestPos.getZ());
        }
    }

    private void stop() {
        this.currentState = State.IDLE;
        this.targetChestPos = null;
        this.targetStandPos = null;
        MinecraftForge.EVENT_BUS.unregister(this);
        EmbeddedNavigationHandler.INSTANCE.stop();
    }

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || mc.player == null)
            return;

        timeoutTicks--;
        if (timeoutTicks <= 0) {
            mc.player.sendMessage(new TextComponentString(I18n.format("msg.goto_open.timeout")));
            stop();
            return;
        }

        if (currentState == State.MOVING) {
            BlockPos arrivalPos = targetStandPos != null ? targetStandPos : targetChestPos;
            // Check if arrived at target position
            double distance = mc.player.getDistance(arrivalPos.getX() + 0.5, arrivalPos.getY() + 0.5,
                    arrivalPos.getZ() + 0.5);
            if (distance < 2.0) {
                EmbeddedNavigationHandler.INSTANCE.stop();
                mc.player.sendMessage(new TextComponentString(
                        I18n.format("msg.goto_open.arrived_try_open")));
                currentState = State.OPENING;
                // 延迟一小会再打开，确保导航完全停止
                ModUtils.DelayScheduler.instance.schedule(() -> {
                    if (currentState == State.OPENING) {
                        ModUtils.rightClickOnBlock(mc.player, targetChestPos);
                    }
                }, 5);
            }
        }
    }

    @SubscribeEvent
    public void onGuiOpen(GuiOpenEvent event) {
        if (currentState != State.OPENING || !(event.getGui() instanceof GuiChest)) {
            return;
        }

        GuiChest gui = (GuiChest) event.getGui();
        if (!(gui.inventorySlots instanceof ContainerChest)) {
            return;
        }

        ContainerChest container = (ContainerChest) gui.inventorySlots;
        IInventory inv = container.getLowerChestInventory();
        BlockPos openedChestPos = null;
        if (inv instanceof TileEntityChest) {
            openedChestPos = ((TileEntityChest) inv).getPos();
        }

        if (openedChestPos != null && targetChestPos != null && !openedChestPos.equals(targetChestPos)) {
            if (mc.player != null) {
                mc.player.sendMessage(new TextComponentString(I18n.format("msg.goto_open.not_target_retry")));
            }
            return;
        }

        mc.player.sendMessage(
                new TextComponentString(I18n.format("msg.goto_open.success")));
        stop();
    }

    private BlockPos findBestStandPosition(BlockPos chestPos) {
        if (mc.world == null) {
            return null;
        }

        BlockPos[] candidates = new BlockPos[] {
                chestPos.north(), chestPos.south(), chestPos.west(), chestPos.east(), chestPos
        };

        BlockPos best = null;
        double bestDistSq = Double.MAX_VALUE;
        Vec3d chestCenter = new Vec3d(chestPos).addVector(0.5, 0.5, 0.5);

        for (BlockPos candidate : candidates) {
            if (!isStandable(candidate)) {
                continue;
            }
            if (!hasLineOfSightToChest(candidate, chestCenter, chestPos)) {
                continue;
            }

            double distSq;
            if (mc.player != null) {
                distSq = mc.player.getDistanceSq(candidate.getX() + 0.5, candidate.getY() + 0.5,
                        candidate.getZ() + 0.5);
            } else {
                distSq = 0;
            }

            if (distSq < bestDistSq) {
                bestDistSq = distSq;
                best = candidate;
            }
        }

        return best;
    }

    private boolean isStandable(BlockPos standPos) {
        if (mc.world == null) {
            return false;
        }

        IBlockState feetState = mc.world.getBlockState(standPos);
        IBlockState headState = mc.world.getBlockState(standPos.up());
        IBlockState belowState = mc.world.getBlockState(standPos.down());

        boolean feetPassable = !feetState.getMaterial().blocksMovement();
        boolean headPassable = !headState.getMaterial().blocksMovement();
        boolean hasGround = belowState.getMaterial().blocksMovement();

        return feetPassable && headPassable && hasGround;
    }

    private boolean hasLineOfSightToChest(BlockPos standPos, Vec3d chestCenter, BlockPos chestPos) {
        if (mc.world == null) {
            return false;
        }

        Vec3d eyePos = new Vec3d(standPos).addVector(0.5, 1.62, 0.5);
        RayTraceResult ray = mc.world.rayTraceBlocks(eyePos, chestCenter, false, true, false);
        return ray == null || (ray.typeOfHit == RayTraceResult.Type.BLOCK && chestPos.equals(ray.getBlockPos()));
    }
}

