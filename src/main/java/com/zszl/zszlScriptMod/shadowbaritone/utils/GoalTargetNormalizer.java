package com.zszl.zszlScriptMod.shadowbaritone.utils;

import com.zszl.zszlScriptMod.shadowbaritone.api.IBaritone;
import com.zszl.zszlScriptMod.shadowbaritone.api.BaritoneAPI;
import com.zszl.zszlScriptMod.shadowbaritone.api.pathing.goals.Goal;
import com.zszl.zszlScriptMod.shadowbaritone.api.pathing.goals.GoalBlock;
import com.zszl.zszlScriptMod.shadowbaritone.api.pathing.goals.GoalComposite;
import com.zszl.zszlScriptMod.shadowbaritone.pathing.movement.MovementHelper;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;

public final class GoalTargetNormalizer {

    private GoalTargetNormalizer() {
    }

    public static Goal normalize(IBaritone baritone, Goal goal) {
        if (goal == null) {
            return null;
        }
        if (goal instanceof GoalComposite) {
            return normalizeGoalComposite(baritone, (GoalComposite) goal);
        }
        if (!(goal instanceof GoalBlock)) {
            return goal;
        }
        return normalizeGoalBlock(baritone, (GoalBlock) goal);
    }

    private static Goal normalizeGoalComposite(IBaritone baritone, GoalComposite goal) {
        Goal[] children = goal.goals();
        Goal[] normalized = new Goal[children.length];
        boolean changed = false;
        for (int i = 0; i < children.length; i++) {
            normalized[i] = normalize(baritone, children[i]);
            changed |= normalized[i] != children[i];
        }
        return changed ? new GoalComposite(normalized) : goal;
    }

    public static GoalBlock normalizeGoalBlock(IBaritone baritone, GoalBlock goal) {
        if (baritone == null || goal == null) {
            return goal;
        }

        BlockPos pos = goal.getGoalPos();
        if (pos.getY() < 0 || pos.getY() >= 255) {
            return goal;
        }

        BlockStateInterface bsi = new BlockStateInterface(baritone.getPlayerContext());
        if (!bsi.worldContainsLoadedChunk(pos.getX(), pos.getZ())) {
            return goal;
        }

        IBlockState targetState = bsi.get0(pos.getX(), pos.getY(), pos.getZ());
        if (!BaritoneAPI.getSettings().allowBreak.value
                && !MovementHelper.canWalkThrough(bsi, pos.getX(), pos.getY(), pos.getZ(), targetState)) {
            GoalBlock corrected = findClosestStandableGoal(bsi, pos);
            if (corrected != null) {
                return corrected;
            }
            return goal;
        }
        if (!MovementHelper.canWalkOn(bsi, pos.getX(), pos.getY(), pos.getZ(), targetState)) {
            return goal;
        }
        if (!MovementHelper.canWalkThrough(bsi, pos.getX(), pos.getY() + 1, pos.getZ())
                || !MovementHelper.canWalkThrough(bsi, pos.getX(), pos.getY() + 2, pos.getZ())) {
            return goal;
        }

        return new GoalBlock(pos.up());
    }

    private static GoalBlock findClosestStandableGoal(BlockStateInterface bsi, BlockPos pos) {
        GoalBlock upward = findStandableGoalInDirection(bsi, pos, 1);
        if (upward != null) {
            return upward;
        }
        return findStandableGoalInDirection(bsi, pos, -1);
    }

    private static GoalBlock findStandableGoalInDirection(BlockStateInterface bsi, BlockPos origin, int step) {
        int minY = 1;
        int maxY = 254;
        for (int y = origin.getY() + step; y >= minY && y <= maxY; y += step) {
            if (!MovementHelper.canWalkThrough(bsi, origin.getX(), y, origin.getZ())
                    || !MovementHelper.canWalkThrough(bsi, origin.getX(), y + 1, origin.getZ())) {
                continue;
            }
            IBlockState supportState = bsi.get0(origin.getX(), y - 1, origin.getZ());
            if (MovementHelper.canWalkOn(bsi, origin.getX(), y - 1, origin.getZ(), supportState)) {
                return new GoalBlock(origin.getX(), y, origin.getZ());
            }
        }
        return null;
    }
}
